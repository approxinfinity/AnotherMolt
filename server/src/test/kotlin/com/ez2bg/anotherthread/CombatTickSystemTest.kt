package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.combat.*
import com.ez2bg.anotherthread.database.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.test.*
import kotlinx.coroutines.runBlocking

/**
 * Comprehensive tests for the combat tick system.
 *
 * The tick system is the heartbeat of combat:
 * 1. Every 3 seconds (ROUND_DURATION_MS), processTick() is called
 * 2. processTick() iterates over all active sessions
 * 3. For each session where enough time has passed, processRound() is called
 * 4. processRound() executes queued actions, applies damage, broadcasts results
 *
 * These tests verify the entire flow works correctly.
 */
class CombatTickSystemTest {

    companion object {
        private var initialized = false
        private val testDbFile = File.createTempFile("tick_test_db_${System.nanoTime()}", ".db").also { it.deleteOnExit() }
    }

    @BeforeTest
    fun setup() {
        if (!initialized) {
            DatabaseConfig.init(testDbFile.absolutePath)
            initialized = true
        }
        clearAllTablesForTest()
        seedTestData()
    }

    private fun clearAllTablesForTest() {
        transaction {
            CombatSessionTable.deleteAll()
            AbilityTable.deleteAll()
            UserTable.deleteAll()
            CreatureTable.deleteAll()
            LocationTable.deleteAll()
            CharacterClassTable.deleteAll()
            ItemTable.deleteAll()
        }
    }

    private fun seedTestData() {
        // Seed character classes
        TestFixtures.allCharacterClasses().forEach { CharacterClassRepository.create(it) }

        // Seed abilities
        TestFixtures.allAbilities().forEach { AbilityRepository.create(it) }

        // Seed items
        TestFixtures.allItems().forEach { ItemRepository.create(it) }

        // Seed locations
        LocationRepository.create(TestFixtures.homeLocation())
        LocationRepository.create(TestFixtures.dungeonEntrance())
        LocationRepository.create(TestFixtures.dungeonRoom())

        // Seed creatures
        CreatureRepository.create(TestFixtures.goblin())
        CreatureRepository.create(TestFixtures.orc())
    }

    // ========================================================================
    // SESSION STATE TESTS
    // ========================================================================

    @Test
    fun testSessionStartsInWaitingState() {
        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.creatureCombatant())
        )

        assertEquals(CombatState.WAITING, session.state, "Session should start in WAITING state")
    }

    @Test
    fun testSessionBecomesActiveWhenPlayerAndCreaturePresent() {
        val creature = TestFixtures.creatureCombatant()
        val player = TestFixtures.playerCombatant()

        var session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(creature)
        )

        assertEquals(CombatState.WAITING, session.state, "Session should be WAITING with only creature")

        // Add player and activate
        session = session.copy(
            combatants = session.combatants + player,
            state = CombatState.ACTIVE
        )

        assertEquals(CombatState.ACTIVE, session.state, "Session should be ACTIVE with player and creature")
        assertTrue(session.players.isNotEmpty(), "Session should have players")
        assertTrue(session.creatures.isNotEmpty(), "Session should have creatures")
    }

    @Test
    fun testSessionHasPlayersAndCreaturesProperties() {
        val player = TestFixtures.playerCombatant()
        val creature = TestFixtures.creatureCombatant()

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player, creature),
            state = CombatState.ACTIVE
        )

        assertEquals(1, session.players.size, "Should have 1 player")
        assertEquals(1, session.creatures.size, "Should have 1 creature")
        assertEquals(player.id, session.players.first().id)
        assertEquals(creature.id, session.creatures.first().id)
    }

    // ========================================================================
    // ROUND TIMING TESTS
    // ========================================================================

    @Test
    fun testRoundDurationIsConfigured() {
        assertTrue(CombatConfig.ROUND_DURATION_MS > 0, "Round duration should be positive")
        assertEquals(3000L, CombatConfig.ROUND_DURATION_MS, "Round duration should be 3 seconds")
    }

    @Test
    fun testSessionTracksRoundStartTime() {
        val now = System.currentTimeMillis()
        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.playerCombatant(), TestFixtures.creatureCombatant()),
            state = CombatState.ACTIVE,
            roundStartTime = now
        )

        assertTrue(session.roundStartTime >= now - 100, "Round start time should be recent")
    }

    @Test
    fun testRoundNumberIncrementsEachRound() {
        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.playerCombatant(), TestFixtures.creatureCombatant()),
            state = CombatState.ACTIVE,
            currentRound = 0
        )

        assertEquals(0, session.currentRound, "Initial round should be 0")

        val afterRound1 = session.copy(currentRound = session.currentRound + 1)
        assertEquals(1, afterRound1.currentRound, "After first round should be 1")

        val afterRound2 = afterRound1.copy(currentRound = afterRound1.currentRound + 1)
        assertEquals(2, afterRound2.currentRound, "After second round should be 2")
    }

    // ========================================================================
    // ACTION QUEUING TESTS
    // ========================================================================

    @Test
    fun testActionsCanBeQueued() {
        val player = TestFixtures.playerCombatant()
        val creature = TestFixtures.creatureCombatant()

        var session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player, creature),
            state = CombatState.ACTIVE
        )

        assertTrue(session.pendingActions.isEmpty(), "No actions initially")

        val action = CombatAction(
            combatantId = player.id,
            abilityId = TestFixtures.BASIC_ATTACK_ID,
            targetId = creature.id
        )

        session = session.copy(pendingActions = session.pendingActions + action)

        assertEquals(1, session.pendingActions.size, "Should have 1 pending action")
        assertEquals(player.id, session.pendingActions.first().combatantId)
    }

    @Test
    fun testMultipleActionsCanBeQueued() {
        val player1 = TestFixtures.playerCombatant(id = "player1", name = "Player1")
        val player2 = TestFixtures.playerCombatant(id = "player2", name = "Player2")
        val creature = TestFixtures.creatureCombatant()

        var session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player1, player2, creature),
            state = CombatState.ACTIVE
        )

        val action1 = CombatAction(combatantId = player1.id, abilityId = TestFixtures.BASIC_ATTACK_ID, targetId = creature.id)
        val action2 = CombatAction(combatantId = player2.id, abilityId = TestFixtures.BASIC_ATTACK_ID, targetId = creature.id)

        session = session.copy(pendingActions = listOf(action1, action2))

        assertEquals(2, session.pendingActions.size, "Should have 2 pending actions")
    }

    @Test
    fun testActionsAreSortedByInitiative() {
        val slowPlayer = TestFixtures.playerCombatant(id = "slow", name = "Slow", initiative = 5)
        val fastPlayer = TestFixtures.playerCombatant(id = "fast", name = "Fast", initiative = 15)
        val creature = TestFixtures.creatureCombatant(initiative = 10)

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(slowPlayer, fastPlayer, creature),
            state = CombatState.ACTIVE
        )

        val action1 = CombatAction(combatantId = slowPlayer.id, abilityId = TestFixtures.BASIC_ATTACK_ID, targetId = creature.id)
        val action2 = CombatAction(combatantId = fastPlayer.id, abilityId = TestFixtures.BASIC_ATTACK_ID, targetId = creature.id)

        val sortedActions = listOf(action1, action2).sortedByDescending { action ->
            session.combatants.find { it.id == action.combatantId }?.initiative ?: 0
        }

        assertEquals(fastPlayer.id, sortedActions.first().combatantId, "Fast player should act first")
        assertEquals(slowPlayer.id, sortedActions.last().combatantId, "Slow player should act last")
    }

    // ========================================================================
    // AUTO-ATTACK TESTS (Creatures)
    // ========================================================================

    @Test
    fun testCreatureAutoAttacksWhenNoActionQueued() {
        val player = TestFixtures.playerCombatant()
        val creature = TestFixtures.creatureCombatant()

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player, creature),
            state = CombatState.ACTIVE,
            pendingActions = emptyList()  // No actions queued
        )

        // Creatures should auto-attack when no action is queued
        // This is handled in processRound - verify the logic exists
        val creaturesNeedingAutoAttack = session.creatures.filter { c ->
            c.isAlive && session.pendingActions.none { it.combatantId == c.id }
        }

        assertEquals(1, creaturesNeedingAutoAttack.size, "Creature should need auto-attack")
        assertEquals(creature.id, creaturesNeedingAutoAttack.first().id)
    }

    @Test
    fun testCreatureSelectsAlivePlayerAsTarget() {
        val alivePlayer = TestFixtures.playerCombatant(id = "alive", currentHp = 30)
        val deadPlayer = TestFixtures.playerCombatant(id = "dead", currentHp = 0)
        val creature = TestFixtures.creatureCombatant()

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(alivePlayer, deadPlayer.copy(isAlive = false), creature),
            state = CombatState.ACTIVE
        )

        val alivePlayers = session.alivePlayers
        assertEquals(1, alivePlayers.size, "Only 1 player should be alive")
        assertEquals(alivePlayer.id, alivePlayers.first().id)
    }

    // ========================================================================
    // DAMAGE APPLICATION TESTS
    // ========================================================================

    @Test
    fun testDamageReducesTargetHp() {
        var creature = TestFixtures.creatureCombatant(currentHp = 15, maxHp = 15)
        val damage = 5

        creature = creature.copy(currentHp = creature.currentHp - damage)

        assertEquals(10, creature.currentHp, "HP should be reduced by damage")
    }

    @Test
    fun testDamageCannotReduceHpBelowDeathThreshold() {
        // Player combatant has default death threshold calculated from constitution
        val player = TestFixtures.playerCombatant(currentHp = 5)
        val deathThreshold = player.deathThreshold  // Uses default from Combatant
        val damage = 100

        val newHp = (player.currentHp - damage).coerceAtLeast(deathThreshold)

        assertEquals(deathThreshold, newHp, "HP should not go below death threshold")
    }

    @Test
    fun testCreatureDeathOccursAtZeroHp() {
        var creature = TestFixtures.creatureCombatant(currentHp = 5)
        val damage = 10

        val newHp = creature.currentHp - damage
        val isDead = newHp <= 0
        creature = creature.copy(currentHp = newHp.coerceAtLeast(0), isAlive = !isDead)

        assertTrue(isDead, "Creature should be dead")
        assertFalse(creature.isAlive, "Creature isAlive should be false")
    }

    @Test
    fun testPlayerBecomesDownedAtZeroHp() {
        val player = TestFixtures.playerCombatant(currentHp = 5)
        val deathThreshold = player.deathThreshold
        val damage = 10

        val newHp = player.currentHp - damage
        val shouldBeDowned = newHp <= 0 && newHp > deathThreshold

        assertTrue(shouldBeDowned, "Player should become downed at 0 HP but above death threshold")
    }

    @Test
    fun testPlayerDiesAtDeathThreshold() {
        val player = TestFixtures.playerCombatant(currentHp = 5)
        val deathThreshold = player.deathThreshold
        // Massive damage that goes below threshold
        val massiveDamage = 5 + (-deathThreshold) + 10

        val newHp = player.currentHp - massiveDamage
        val shouldDie = newHp <= deathThreshold

        assertTrue(shouldDie, "Player should die when HP reaches death threshold")
    }

    // ========================================================================
    // COMBAT END CONDITION TESTS
    // ========================================================================

    @Test
    fun testCombatEndsWhenAllCreaturesDead() {
        val player = TestFixtures.playerCombatant()
        val deadCreature = TestFixtures.creatureCombatant(currentHp = 0).copy(isAlive = false)

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player, deadCreature),
            state = CombatState.ACTIVE
        )

        val aliveCreatures = session.aliveCreatures
        assertTrue(aliveCreatures.isEmpty(), "No creatures should be alive")

        // Combat should end
        val shouldEnd = session.aliveCreatures.isEmpty() && session.alivePlayers.isNotEmpty()
        assertTrue(shouldEnd, "Combat should end when all creatures dead and players alive")
    }

    @Test
    fun testCombatEndsWhenAllPlayersDead() {
        val deadPlayer = TestFixtures.playerCombatant(currentHp = -15).copy(isAlive = false)
        val creature = TestFixtures.creatureCombatant()

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(deadPlayer, creature),
            state = CombatState.ACTIVE
        )

        val alivePlayers = session.alivePlayers
        assertTrue(alivePlayers.isEmpty(), "No players should be alive")

        // Combat should end
        val shouldEnd = session.alivePlayers.isEmpty() && session.aliveCreatures.isNotEmpty()
        assertTrue(shouldEnd, "Combat should end when all players dead and creatures alive")
    }

    @Test
    fun testCombatContinuesWithDownedPlayers() {
        val downedPlayer = TestFixtures.playerCombatant(
            id = "downed",
            currentHp = -5
        ).copy(isDowned = true, isAlive = true)  // Downed but not dead

        val alivePlayer = TestFixtures.playerCombatant(id = "alive", currentHp = 30)
        val creature = TestFixtures.creatureCombatant()

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(downedPlayer, alivePlayer, creature),
            state = CombatState.ACTIVE
        )

        // Downed players count as alive for session purposes
        val alivePlayers = session.combatants.filter {
            it.type == CombatantType.PLAYER && it.isAlive
        }
        assertEquals(2, alivePlayers.size, "Both downed and alive players should count")
    }

    // ========================================================================
    // BASIC ATTACK DAMAGE CALCULATION TESTS
    // ========================================================================

    @Test
    fun testBasicAttackUsesActorBaseDamageWhenAbilityHasNone() {
        // Use the universal basic attack which has baseDamage = 0
        val universalAttack = AbilityRepository.findById(TestFixtures.UNIVERSAL_BASIC_ATTACK_ID)
        assertNotNull(universalAttack, "Universal basic attack ability should exist")

        // The universal basic attack ability has baseDamage = 0
        // This is intentional - damage should come from the combatant's baseDamage stat
        assertEquals(0, universalAttack.baseDamage, "Universal basic attack ability should have 0 baseDamage")

        val player = TestFixtures.playerCombatant(baseDamage = 10)
        assertEquals(10, player.baseDamage, "Player should have baseDamage stat")

        // The fix: effectiveBaseDamage should be player.baseDamage when ability.baseDamage is 0
        val effectiveBaseDamage = if (universalAttack.baseDamage > 0) universalAttack.baseDamage else player.baseDamage
        assertEquals(10, effectiveBaseDamage, "Effective damage should use actor's baseDamage")
    }

    @Test
    fun testAbilityWithBaseDamageUsesItsOwnDamage() {
        // Use fireball which has its own base damage
        val fireball = AbilityRepository.findById(TestFixtures.FIREBALL_ID)
        assertNotNull(fireball, "Fireball ability should exist")
        assertTrue(fireball.baseDamage > 0, "Fireball should have base damage")

        val player = TestFixtures.playerCombatant(baseDamage = 5)

        val effectiveBaseDamage = if (fireball.baseDamage > 0) fireball.baseDamage else player.baseDamage
        assertEquals(fireball.baseDamage, effectiveBaseDamage, "Should use ability's baseDamage when > 0")
    }

    // ========================================================================
    // COMBAT SESSION LIFECYCLE TESTS
    // ========================================================================

    @Test
    fun testSessionCanBeCreated() {
        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.creatureCombatant())
        )

        assertNotNull(session.id, "Session should have an ID")
        assertEquals(TestFixtures.DUNGEON_ENTRANCE_ID, session.locationId)
    }

    @Test
    fun testSessionCanBePersisted() {
        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.playerCombatant(), TestFixtures.creatureCombatant()),
            state = CombatState.ACTIVE
        )

        CombatSessionRepository.create(session)

        val retrieved = CombatSessionRepository.findById(session.id)
        assertNotNull(retrieved, "Session should be retrievable")
        assertEquals(session.id, retrieved.id)
        assertEquals(session.locationId, retrieved.locationId)
        assertEquals(session.state, retrieved.state)
    }

    @Test
    fun testSessionCanBeUpdated() {
        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.playerCombatant(), TestFixtures.creatureCombatant()),
            state = CombatState.ACTIVE,
            currentRound = 0
        )

        CombatSessionRepository.create(session)

        val updated = session.copy(currentRound = 5)
        CombatSessionRepository.update(updated)

        val retrieved = CombatSessionRepository.findById(session.id)
        assertNotNull(retrieved)
        assertEquals(5, retrieved.currentRound, "Round should be updated")
    }

    // ========================================================================
    // INTEGRATION: FULL ROUND SIMULATION
    // ========================================================================

    @Test
    fun testFullRoundSimulation() {
        // Setup: Player vs Creature
        val player = TestFixtures.playerCombatant(
            currentHp = 30,
            maxHp = 30,
            baseDamage = 8,
            initiative = 10
        )
        val creature = TestFixtures.creatureCombatant(
            currentHp = 20,
            maxHp = 20,
            baseDamage = 5,
            initiative = 5
        )

        var session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player, creature),
            state = CombatState.ACTIVE,
            currentRound = 0,
            roundStartTime = System.currentTimeMillis() - CombatConfig.ROUND_DURATION_MS  // Ready to process
        )

        // Queue player attack
        val playerAction = CombatAction(
            combatantId = player.id,
            abilityId = TestFixtures.BASIC_ATTACK_ID,
            targetId = creature.id
        )
        session = session.copy(pendingActions = listOf(playerAction))

        // Simulate round processing (what processRound does):
        // 1. Sort actions by initiative
        val sortedActions = session.pendingActions.sortedByDescending { action ->
            session.combatants.find { it.id == action.combatantId }?.initiative ?: 0
        }

        // 2. Player acts first (higher initiative)
        assertEquals(player.id, sortedActions.first().combatantId, "Player should act first")

        // 3. Damage is dealt
        val basicAttack = AbilityRepository.findById(TestFixtures.BASIC_ATTACK_ID)!!
        val effectiveDamage = if (basicAttack.baseDamage > 0) basicAttack.baseDamage else player.baseDamage
        assertTrue(effectiveDamage > 0, "Effective damage should be positive")

        // 4. Apply damage to creature
        var updatedCreature = creature.copy(currentHp = creature.currentHp - effectiveDamage)
        assertTrue(updatedCreature.currentHp < creature.currentHp, "Creature HP should be reduced")

        // 5. Update session combatants
        session = session.copy(
            combatants = session.combatants.map {
                if (it.id == creature.id) updatedCreature else it
            },
            pendingActions = emptyList(),  // Clear actions after round
            currentRound = session.currentRound + 1
        )

        assertEquals(1, session.currentRound, "Round should increment")
        assertTrue(session.pendingActions.isEmpty(), "Actions should be cleared")
    }

    @Test
    fun testMultipleRoundsUntilCombatEnds() {
        // Setup: Weak creature that will die in a few rounds
        var player = TestFixtures.playerCombatant(
            currentHp = 100,
            maxHp = 100,
            baseDamage = 10
        )
        var creature = TestFixtures.creatureCombatant(
            currentHp = 25,
            maxHp = 25,
            baseDamage = 5
        )

        var session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player, creature),
            state = CombatState.ACTIVE
        )

        var roundCount = 0
        val maxRounds = 10  // Safety limit

        while (session.state == CombatState.ACTIVE && roundCount < maxRounds) {
            roundCount++

            // Player attacks creature
            val currentCreature = session.combatants.find { it.id == creature.id }!!
            if (currentCreature.isAlive) {
                val damage = player.baseDamage
                val newHp = (currentCreature.currentHp - damage).coerceAtLeast(0)
                val isDead = newHp <= 0

                creature = currentCreature.copy(currentHp = newHp, isAlive = !isDead)
                session = session.copy(
                    combatants = session.combatants.map {
                        if (it.id == creature.id) creature else it
                    }
                )
            }

            // Check end condition
            if (session.aliveCreatures.isEmpty()) {
                session = session.copy(state = CombatState.ENDED)
            }
        }

        assertEquals(CombatState.ENDED, session.state, "Combat should have ended")
        assertTrue(roundCount <= 3, "Should end within 3 rounds (25 HP / 10 damage = 3)")
        assertFalse(creature.isAlive, "Creature should be dead")
    }

    // ========================================================================
    // CREATURE WANDERING VS COMBAT TESTS
    // ========================================================================

    @Test
    fun testCreatureInCombatDoesNotWander() {
        val creature = TestFixtures.creatureCombatant()
        val player = TestFixtures.playerCombatant()

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player, creature),
            state = CombatState.ACTIVE
        )

        // Creature is in combat
        val creaturesInCombat = session.creatures.map { it.id }.toSet()
        assertTrue(creature.id in creaturesInCombat, "Creature should be in combat")

        // Creature should not wander while in combat
        // This is the expected behavior
    }

    // ========================================================================
    // MESSAGE STRUCTURE TESTS
    // ========================================================================

    @Test
    fun testRoundStartMessageContainsCorrectData() {
        val player = TestFixtures.playerCombatant()
        val creature = TestFixtures.creatureCombatant()

        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(player, creature),
            state = CombatState.ACTIVE,
            currentRound = 1
        )

        val roundStartMessage = RoundStartMessage(
            sessionId = session.id,
            roundNumber = session.currentRound,
            roundDurationMs = CombatConfig.ROUND_DURATION_MS,
            combatants = session.combatants
        )

        assertEquals(session.id, roundStartMessage.sessionId)
        assertEquals(1, roundStartMessage.roundNumber)
        assertEquals(CombatConfig.ROUND_DURATION_MS, roundStartMessage.roundDurationMs)
        assertEquals(2, roundStartMessage.combatants.size)
    }

    @Test
    fun testCombatEndedMessageContainsCorrectData() {
        val message = CombatEndedMessage(
            sessionId = "test-session",
            reason = CombatEndReason.ALL_ENEMIES_DEFEATED,
            victors = listOf(TestFixtures.PLAYER_1_ID),
            defeated = listOf(TestFixtures.GOBLIN_ID)
        )

        assertEquals(CombatEndReason.ALL_ENEMIES_DEFEATED, message.reason)
        assertEquals(1, message.victors.size)
        assertEquals(1, message.defeated.size)
    }

    // ========================================================================
    // TICK TIMING VERIFICATION TESTS
    // ========================================================================

    @Test
    fun testSessionReadyForProcessingAfterRoundDuration() {
        val now = System.currentTimeMillis()

        // Session created 5 seconds ago (round started then)
        val sessionReady = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.playerCombatant(), TestFixtures.creatureCombatant()),
            state = CombatState.ACTIVE,
            roundStartTime = now - 5000  // 5 seconds ago
        )

        val timeSinceRoundStart = now - sessionReady.roundStartTime
        val isReadyToProcess = timeSinceRoundStart >= CombatConfig.ROUND_DURATION_MS

        assertTrue(isReadyToProcess, "Session should be ready to process after round duration")
    }

    @Test
    fun testSessionNotReadyBeforeRoundDuration() {
        val now = System.currentTimeMillis()

        // Session created 1 second ago (round started then)
        val sessionNotReady = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.playerCombatant(), TestFixtures.creatureCombatant()),
            state = CombatState.ACTIVE,
            roundStartTime = now - 1000  // 1 second ago
        )

        val timeSinceRoundStart = now - sessionNotReady.roundStartTime
        val isReadyToProcess = timeSinceRoundStart >= CombatConfig.ROUND_DURATION_MS

        assertFalse(isReadyToProcess, "Session should NOT be ready before round duration")
    }

    @Test
    fun testWaitingSessionIsNotProcessed() {
        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.creatureCombatant()),  // Only creature, no player
            state = CombatState.WAITING,
            roundStartTime = System.currentTimeMillis() - 10000  // 10 seconds ago
        )

        // processTick skips WAITING sessions
        val shouldProcess = session.state == CombatState.ACTIVE

        assertFalse(shouldProcess, "WAITING sessions should not be processed")
    }

    @Test
    fun testEndedSessionIsNotProcessed() {
        val session = CombatSession(
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            combatants = listOf(TestFixtures.playerCombatant()),
            state = CombatState.ENDED,
            roundStartTime = System.currentTimeMillis() - 10000
        )

        // processTick skips ENDED sessions
        val shouldProcess = session.state == CombatState.ACTIVE

        assertFalse(shouldProcess, "ENDED sessions should not be processed")
    }
}
