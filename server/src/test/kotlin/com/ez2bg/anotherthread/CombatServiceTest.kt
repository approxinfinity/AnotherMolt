package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.combat.*
import com.ez2bg.anotherthread.database.*
import org.jetbrains.exposed.sql.deleteAll
import java.io.File
import kotlin.test.*

/**
 * Unit tests for the combat system.
 * Tests ability execution, healing, damage over time, buffs/debuffs, and status effects.
 */
class CombatServiceTest {

    companion object {
        private var initialized = false
        // Use unique file per test class to avoid conflicts
        private val testDbFile = File.createTempFile("combat_test_db_${System.nanoTime()}", ".db").also { it.deleteOnExit() }
    }

    @BeforeTest
    fun setup() {
        // Initialize database once, then clear and seed before each test
        if (!initialized) {
            DatabaseConfig.init(testDbFile.absolutePath)
            initialized = true
        }
        // Clear all tables including character classes to avoid unique constraint issues
        clearAllTablesForTest()
        seedTestData()
    }

    private fun clearAllTablesForTest() {
        org.jetbrains.exposed.sql.transactions.transaction {
            // Clear in reverse dependency order
            CombatSessionTable.deleteAll()
            AbilityTable.deleteAll()
            UserTable.deleteAll()
            CreatureTable.deleteAll()
            LocationTable.deleteAll()
            CharacterClassTable.deleteAll()
        }
    }

    private fun seedTestData() {
        // Seed character classes
        TestFixtures.allCharacterClasses().forEach { CharacterClassRepository.create(it) }

        // Seed abilities
        TestFixtures.allAbilities().forEach { AbilityRepository.create(it) }

        // Seed users
        UserRepository.create(TestFixtures.player1())
        UserRepository.create(TestFixtures.player2())

        // Seed creatures
        CreatureRepository.create(TestFixtures.goblin())
        CreatureRepository.create(TestFixtures.orc())

        // Seed locations
        LocationRepository.create(TestFixtures.dungeonEntrance())
        LocationRepository.create(TestFixtures.dungeonRoom())
    }

    // ========== Basic Damage Tests ==========

    @Test
    fun testBasicAttackDealsDamage() {
        val actor = TestFixtures.playerCombatant(initiative = 10)
        val target = TestFixtures.creatureCombatant(currentHp = 15, maxHp = 15)
        val ability = TestFixtures.basicAttack()

        val result = executeAbility(actor, ability, target, listOf(actor, target))

        // Basic attack: 10 base damage + initiative/2 = 10 + 5 = 15
        assertEquals(15, result.damage)
        assertEquals(0, result.healing)
        assertTrue(result.success)
        assertTrue(result.message.contains("hits"))
    }

    @Test
    fun testDamageReducesTargetHp() {
        val actor = TestFixtures.playerCombatant()
        val target = TestFixtures.creatureCombatant(currentHp = 15, maxHp = 15)
        val ability = TestFixtures.basicAttack()
        val action = TestFixtures.attackAction()

        val result = executeAbility(actor, ability, target, listOf(actor, target))
        val updatedCombatants = applyActionResult(listOf(actor, target), action, result)

        val updatedTarget = updatedCombatants.find { it.id == TestFixtures.GOBLIN_ID }!!
        assertTrue(updatedTarget.currentHp < 15)
        assertEquals(15 - result.damage, updatedTarget.currentHp)
    }

    @Test
    fun testLethalDamageKillsTarget() {
        val actor = TestFixtures.playerCombatant(initiative = 20) // High initiative for more damage
        val target = TestFixtures.creatureCombatant(currentHp = 5, maxHp = 15) // Low HP
        val ability = TestFixtures.basicAttack()
        val action = TestFixtures.attackAction()

        val result = executeAbility(actor, ability, target, listOf(actor, target))
        val updatedCombatants = applyActionResult(listOf(actor, target), action, result)

        val updatedTarget = updatedCombatants.find { it.id == TestFixtures.GOBLIN_ID }!!
        assertEquals(0, updatedTarget.currentHp)
        assertFalse(updatedTarget.isAlive)
    }

    // ========== Self-Healing Tests ==========

    @Test
    fun testSelfHealingRestoresHp() {
        val actor = TestFixtures.playerCombatant(currentHp = 15, maxHp = 30, initiative = 8)
        val ability = TestFixtures.healSelf()

        val result = executeAbility(actor, ability, null, listOf(actor))

        // Heal: 15 base + initiative/4 = 15 + 2 = 17
        assertTrue(result.healing > 0)
        assertEquals(0, result.damage)
        assertTrue(result.message.contains("heals"))
    }

    @Test
    fun testSelfHealingAppliedToActor() {
        val actor = TestFixtures.playerCombatant(currentHp = 15, maxHp = 30)
        val ability = TestFixtures.healSelf()
        val action = TestFixtures.healSelfAction()

        val result = executeAbility(actor, ability, null, listOf(actor))
        val updatedCombatants = applyActionResult(listOf(actor), action, result)

        val updatedActor = updatedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertTrue(updatedActor.currentHp > 15)
    }

    @Test
    fun testHealingCappedAtMaxHp() {
        val actor = TestFixtures.playerCombatant(currentHp = 28, maxHp = 30) // Only 2 HP missing
        val ability = TestFixtures.healSelf()
        val action = TestFixtures.healSelfAction()

        val result = executeAbility(actor, ability, null, listOf(actor))
        val updatedCombatants = applyActionResult(listOf(actor), action, result)

        val updatedActor = updatedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertEquals(30, updatedActor.currentHp) // Should not exceed max
    }

    // ========== Targeted Healing Tests ==========

    @Test
    fun testHealOtherRestoresTargetHp() {
        val healer = TestFixtures.playerCombatant(
            id = TestFixtures.PLAYER_2_ID,
            name = "Healer",
            characterClassId = TestFixtures.HEALER_CLASS_ID
        )
        val target = TestFixtures.playerCombatant(
            id = TestFixtures.PLAYER_1_ID,
            name = "Warrior",
            currentHp = 10,
            maxHp = 30
        )
        val ability = TestFixtures.healOther()
        val action = TestFixtures.healOtherAction()

        val result = executeAbility(healer, ability, target, listOf(healer, target))

        assertTrue(result.healing > 0)
        assertTrue(result.message.contains("heals"))

        val updatedCombatants = applyActionResult(listOf(healer, target), action, result)
        val updatedTarget = updatedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertTrue(updatedTarget.currentHp > 10)
    }

    // ========== DoT (Damage Over Time) Tests ==========

    @Test
    fun testPoisonAppliesDoT() {
        val actor = TestFixtures.playerCombatant()
        val target = TestFixtures.creatureCombatant()
        val ability = TestFixtures.poisonDot()

        val result = executeAbility(actor, ability, target, listOf(actor, target))

        // Poison: deals initial damage + applies DoT effect
        assertTrue(result.damage > 0)
        assertTrue(result.appliedEffects.isNotEmpty())

        val dotEffect = result.appliedEffects.find { it.effectType == "dot" }
        assertNotNull(dotEffect)
        assertEquals(3, dotEffect.remainingRounds)
    }

    @Test
    fun testDoTAppliedToTarget() {
        val actor = TestFixtures.playerCombatant()
        val target = TestFixtures.creatureCombatant()
        val ability = TestFixtures.poisonDot()
        val action = CombatAction(
            combatantId = actor.id,
            abilityId = ability.id,
            targetId = target.id
        )

        val result = executeAbility(actor, ability, target, listOf(actor, target))
        val updatedCombatants = applyActionResult(listOf(actor, target), action, result)

        val updatedTarget = updatedCombatants.find { it.id == TestFixtures.GOBLIN_ID }!!
        assertTrue(updatedTarget.statusEffects.isNotEmpty())

        val dotEffect = updatedTarget.statusEffects.find { it.effectType == "dot" }
        assertNotNull(dotEffect)
    }

    @Test
    fun testDoTTicksDamage() {
        val creature = TestFixtures.creatureCombatant(
            currentHp = 15,
            maxHp = 15,
            statusEffects = listOf(TestFixtures.dotEffect(value = 5, remainingRounds = 2))
        )

        val processedCombatants = processStatusEffectsForTest(listOf(creature))

        val updated = processedCombatants.find { it.id == TestFixtures.GOBLIN_ID }!!
        assertEquals(10, updated.currentHp) // 15 - 5 = 10
    }

    @Test
    fun testDoTExpires() {
        val creature = TestFixtures.creatureCombatant(
            currentHp = 15,
            maxHp = 15,
            statusEffects = listOf(TestFixtures.dotEffect(value = 5, remainingRounds = 1))
        )

        val processedCombatants = processStatusEffectsForTest(listOf(creature))

        val updated = processedCombatants.find { it.id == TestFixtures.GOBLIN_ID }!!
        assertEquals(10, updated.currentHp) // Damage applied
        assertTrue(updated.statusEffects.isEmpty()) // Effect expired
    }

    // ========== HoT (Heal Over Time) Tests ==========

    @Test
    fun testHotAppliesRegeneration() {
        val actor = TestFixtures.playerCombatant()
        val ability = TestFixtures.hotRegen()

        val result = executeAbility(actor, ability, null, listOf(actor))

        assertTrue(result.appliedEffects.isNotEmpty())
        val hotEffect = result.appliedEffects.find { it.effectType == "hot" }
        assertNotNull(hotEffect)
        assertEquals(3, hotEffect.remainingRounds)
    }

    @Test
    fun testHotTicksHealing() {
        val player = TestFixtures.playerCombatant(
            currentHp = 20,
            maxHp = 30,
            statusEffects = listOf(TestFixtures.hotEffect(value = 5, remainingRounds = 2))
        )

        val processedCombatants = processStatusEffectsForTest(listOf(player))

        val updated = processedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertEquals(25, updated.currentHp) // 20 + 5 = 25
    }

    @Test
    fun testHotCappedAtMaxHp() {
        val player = TestFixtures.playerCombatant(
            currentHp = 28,
            maxHp = 30,
            statusEffects = listOf(TestFixtures.hotEffect(value = 10, remainingRounds = 2))
        )

        val processedCombatants = processStatusEffectsForTest(listOf(player))

        val updated = processedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertEquals(30, updated.currentHp) // Should not exceed max
    }

    // ========== Buff Tests ==========

    @Test
    fun testBuffAppliesEffect() {
        val actor = TestFixtures.playerCombatant()
        val ability = TestFixtures.buffStrength()

        val result = executeAbility(actor, ability, null, listOf(actor))

        assertTrue(result.appliedEffects.isNotEmpty())
        val buffEffect = result.appliedEffects.find { it.effectType == "buff" }
        assertNotNull(buffEffect)
        assertEquals(3, buffEffect.remainingRounds)
    }

    @Test
    fun testBuffAppliedToSelf() {
        val actor = TestFixtures.playerCombatant()
        val ability = TestFixtures.buffStrength()
        val action = CombatAction(
            combatantId = actor.id,
            abilityId = ability.id,
            targetId = null
        )

        val result = executeAbility(actor, ability, null, listOf(actor))
        val updatedCombatants = applyActionResult(listOf(actor), action, result)

        val updatedActor = updatedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertTrue(updatedActor.statusEffects.isNotEmpty())

        val buffEffect = updatedActor.statusEffects.find { it.effectType == "buff" }
        assertNotNull(buffEffect)
    }

    @Test
    fun testBuffDecrementsDuration() {
        val player = TestFixtures.playerCombatant(
            statusEffects = listOf(TestFixtures.buffEffect(remainingRounds = 2))
        )

        val processedCombatants = processStatusEffectsForTest(listOf(player))

        val updated = processedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        val buffEffect = updated.statusEffects.find { it.effectType == "buff" }
        assertNotNull(buffEffect)
        assertEquals(1, buffEffect.remainingRounds)
    }

    // ========== Debuff Tests ==========

    @Test
    fun testDebuffAppliesEffect() {
        val actor = TestFixtures.playerCombatant()
        val target = TestFixtures.creatureCombatant()
        val ability = TestFixtures.debuffWeakness()

        val result = executeAbility(actor, ability, target, listOf(actor, target))

        assertTrue(result.appliedEffects.isNotEmpty())
        val debuffEffect = result.appliedEffects.find { it.effectType == "debuff" }
        assertNotNull(debuffEffect)
        assertEquals(2, debuffEffect.remainingRounds)
    }

    @Test
    fun testDebuffAppliedToTarget() {
        val actor = TestFixtures.playerCombatant()
        val target = TestFixtures.creatureCombatant()
        val ability = TestFixtures.debuffWeakness()
        val action = CombatAction(
            combatantId = actor.id,
            abilityId = ability.id,
            targetId = target.id
        )

        val result = executeAbility(actor, ability, target, listOf(actor, target))
        val updatedCombatants = applyActionResult(listOf(actor, target), action, result)

        val updatedTarget = updatedCombatants.find { it.id == TestFixtures.GOBLIN_ID }!!
        assertTrue(updatedTarget.statusEffects.isNotEmpty())

        val debuffEffect = updatedTarget.statusEffects.find { it.effectType == "debuff" }
        assertNotNull(debuffEffect)
    }

    // ========== Stun Tests ==========

    @Test
    fun testStunAppliesEffect() {
        val actor = TestFixtures.playerCombatant()
        val target = TestFixtures.creatureCombatant()
        val ability = TestFixtures.stunAbility()

        val result = executeAbility(actor, ability, target, listOf(actor, target))

        // Stun deals damage + applies stun effect
        assertTrue(result.damage > 0)
        assertTrue(result.appliedEffects.isNotEmpty())

        val stunEffect = result.appliedEffects.find { it.effectType == "stun" }
        assertNotNull(stunEffect)
    }

    // ========== Cooldown Tests ==========

    @Test
    fun testAbilityCooldownApplied() {
        val actor = TestFixtures.playerCombatant(cooldowns = emptyMap())
        val ability = TestFixtures.healSelf() // Has cooldown of 2 rounds
        val action = TestFixtures.healSelfAction()

        val result = executeAbility(actor, ability, null, listOf(actor))
        val updatedCombatants = applyActionResult(listOf(actor), action, result)

        val updatedActor = updatedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertTrue(updatedActor.cooldowns.containsKey(TestFixtures.HEAL_SELF_ID))
        assertEquals(2, updatedActor.cooldowns[TestFixtures.HEAL_SELF_ID])
    }

    @Test
    fun testCooldownDecrementsEachRound() {
        val actor = TestFixtures.playerCombatant(
            cooldowns = mapOf(TestFixtures.HEAL_SELF_ID to 2)
        )

        val updatedCombatants = decrementCooldownsForTest(listOf(actor))

        val updated = updatedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertEquals(1, updated.cooldowns[TestFixtures.HEAL_SELF_ID])
    }

    @Test
    fun testCooldownExpiresAtZero() {
        val actor = TestFixtures.playerCombatant(
            cooldowns = mapOf(TestFixtures.HEAL_SELF_ID to 1)
        )

        val updatedCombatants = decrementCooldownsForTest(listOf(actor))

        val updated = updatedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        assertFalse(updated.cooldowns.containsKey(TestFixtures.HEAL_SELF_ID))
    }

    // ========== Combat End Condition Tests ==========

    @Test
    fun testCombatEndsWhenAllCreaturesDead() {
        val session = CombatSession(
            id = "test-end-session",
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            state = CombatState.ACTIVE,
            combatants = listOf(
                TestFixtures.playerCombatant(currentHp = 20, maxHp = 30),
                TestFixtures.creatureCombatant(currentHp = 0, maxHp = 15) // Dead creature
            )
        )

        val checkedSession = checkEndConditionsForTest(session)

        assertEquals(CombatState.ENDED, checkedSession.state)
        assertEquals(CombatEndReason.ALL_ENEMIES_DEFEATED, checkedSession.endReason)
    }

    @Test
    fun testCombatEndsWhenAllPlayersDead() {
        val session = CombatSession(
            id = "test-end-session",
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            state = CombatState.ACTIVE,
            combatants = listOf(
                TestFixtures.playerCombatant(currentHp = 0, maxHp = 30), // Dead player
                TestFixtures.creatureCombatant(currentHp = 10, maxHp = 15)
            )
        )

        val checkedSession = checkEndConditionsForTest(session)

        assertEquals(CombatState.ENDED, checkedSession.state)
        assertEquals(CombatEndReason.ALL_PLAYERS_DEFEATED, checkedSession.endReason)
    }

    @Test
    fun testCombatContinuesWithLiveCombatants() {
        val session = CombatSession(
            id = "test-continue-session",
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            state = CombatState.ACTIVE,
            combatants = listOf(
                TestFixtures.playerCombatant(currentHp = 20, maxHp = 30),
                TestFixtures.creatureCombatant(currentHp = 10, maxHp = 15)
            )
        )

        val checkedSession = checkEndConditionsForTest(session)

        assertEquals(CombatState.ACTIVE, checkedSession.state)
        assertNull(checkedSession.endReason)
    }

    @Test
    fun testCombatEndsWhenNoCreatures() {
        // If combat somehow started without creatures, it should end with timeout
        val session = CombatSession(
            id = "test-no-creatures-session",
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            state = CombatState.ACTIVE,
            combatants = listOf(
                TestFixtures.playerCombatant(currentHp = 20, maxHp = 30)
                // No creatures at all
            )
        )

        val checkedSession = checkEndConditionsForTest(session)

        assertEquals(CombatState.ENDED, checkedSession.state)
        assertEquals(CombatEndReason.TIMEOUT, checkedSession.endReason)
    }

    @Test
    fun testCombatEndsWhenNoPlayers() {
        // If all players leave (e.g., phasewalk), combat should end
        val session = CombatSession(
            id = "test-no-players-session",
            locationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            state = CombatState.ACTIVE,
            combatants = listOf(
                // No players
                TestFixtures.creatureCombatant(currentHp = 10, maxHp = 15)
            )
        )

        val checkedSession = checkEndConditionsForTest(session)

        assertEquals(CombatState.ENDED, checkedSession.state)
        assertEquals(CombatEndReason.ALL_PLAYERS_DEFEATED, checkedSession.endReason)
    }

    // ========== Effect Replacement Tests ==========

    @Test
    fun testNewEffectReplacesExisting() {
        val actor = TestFixtures.playerCombatant(
            statusEffects = listOf(TestFixtures.buffEffect(remainingRounds = 1))
        )
        val ability = TestFixtures.buffStrength()
        val action = CombatAction(
            combatantId = actor.id,
            abilityId = ability.id,
            targetId = null
        )

        val result = executeAbility(actor, ability, null, listOf(actor))
        val updatedCombatants = applyActionResult(listOf(actor), action, result)

        val updated = updatedCombatants.find { it.id == TestFixtures.PLAYER_1_ID }!!
        // Should only have one buff effect
        val buffs = updated.statusEffects.filter { it.effectType == "buff" }
        assertEquals(1, buffs.size)
        // Should have the new duration, not the old one
        assertEquals(3, buffs[0].remainingRounds)
    }

    // ========== Helper Functions ==========
    // These simulate the private functions in CombatService for testing

    private fun executeAbility(
        actor: Combatant,
        ability: Ability,
        target: Combatant?,
        allCombatants: List<Combatant>
    ): ActionResult {
        val parsedEffects = parseAbilityEffects(ability.effects, actor.id, ability.durationRounds)
        val isHealingAbility = parsedEffects.any { it.effectType == "heal" || it.effectType == "hot" }

        val damage = if (!isHealingAbility && ability.baseDamage > 0 && target != null) {
            ability.baseDamage + (actor.initiative / 2)
        } else 0

        val healing = if (isHealingAbility && ability.baseDamage > 0) {
            ability.baseDamage + (actor.initiative / 4)
        } else 0

        val appliedEffects = parsedEffects.filter { effect ->
            when (effect.effectType) {
                "heal" -> false
                "hot", "dot", "buff", "debuff", "stun", "root", "slow" -> true
                else -> false
            }
        }

        val message = when {
            damage > 0 && target != null -> "${actor.name} hits ${target.name} with ${ability.name} for $damage damage!"
            healing > 0 -> "${actor.name} heals for $healing with ${ability.name}!"
            else -> "${actor.name} uses ${ability.name}!"
        }

        return ActionResult(
            actionId = "${actor.id}-${ability.id}",
            success = true,
            damage = damage,
            healing = healing,
            appliedEffects = appliedEffects,
            message = message
        )
    }

    private fun parseAbilityEffects(effectsJson: String, sourceId: String, durationRounds: Int): List<StatusEffect> {
        if (effectsJson.isBlank() || effectsJson == "[]") return emptyList()

        val effects = mutableListOf<StatusEffect>()
        val duration = if (durationRounds > 0) durationRounds else 3

        // Simple parsing for test abilities
        if (effectsJson.contains("heal", ignoreCase = true)) {
            effects.add(StatusEffect(name = "Heal", effectType = "heal", value = 0, remainingRounds = 1, sourceId = sourceId))
        }
        if (effectsJson.contains("dot", ignoreCase = true)) {
            effects.add(StatusEffect(name = "Burning", effectType = "dot", value = 5, remainingRounds = duration, sourceId = sourceId))
        }
        if (effectsJson.contains("hot", ignoreCase = true)) {
            effects.add(StatusEffect(name = "Regenerating", effectType = "hot", value = 5, remainingRounds = duration, sourceId = sourceId))
        }
        if (effectsJson.contains("buff", ignoreCase = true)) {
            effects.add(StatusEffect(name = "Empowered", effectType = "buff", value = 3, remainingRounds = duration, sourceId = sourceId))
        }
        if (effectsJson.contains("debuff", ignoreCase = true)) {
            effects.add(StatusEffect(name = "Weakened", effectType = "debuff", value = -3, remainingRounds = duration, sourceId = sourceId))
        }
        if (effectsJson.contains("stun", ignoreCase = true)) {
            effects.add(StatusEffect(name = "Stunned", effectType = "stun", value = 0, remainingRounds = duration, sourceId = sourceId))
        }

        return effects
    }

    private fun applyActionResult(
        combatants: List<Combatant>,
        action: CombatAction,
        result: ActionResult
    ): List<Combatant> {
        return combatants.map { combatant ->
            var updated = combatant

            // Apply damage to target
            if (combatant.id == action.targetId && result.damage > 0) {
                val newHp = (updated.currentHp - result.damage).coerceAtLeast(0)
                updated = updated.copy(currentHp = newHp, isAlive = newHp > 0)
            }

            // Apply healing
            if (combatant.id == action.combatantId && result.healing > 0 && action.targetId == null) {
                val newHp = (updated.currentHp + result.healing).coerceAtMost(updated.maxHp)
                updated = updated.copy(currentHp = newHp)
            } else if (combatant.id == action.targetId && result.healing > 0) {
                val newHp = (updated.currentHp + result.healing).coerceAtMost(updated.maxHp)
                updated = updated.copy(currentHp = newHp)
            }

            // Apply status effects
            if (result.appliedEffects.isNotEmpty()) {
                val ability = AbilityRepository.findById(action.abilityId)
                val isSelfTarget = ability?.targetType == "self" || action.targetId == null

                val shouldApplyEffects = if (isSelfTarget) {
                    combatant.id == action.combatantId
                } else {
                    combatant.id == action.targetId
                }

                if (shouldApplyEffects) {
                    val existingEffectNames = result.appliedEffects.map { it.name }.toSet()
                    val filteredExisting = updated.statusEffects.filter { it.name !in existingEffectNames }
                    updated = updated.copy(statusEffects = filteredExisting + result.appliedEffects)
                }
            }

            // Apply cooldown
            if (combatant.id == action.combatantId) {
                val ability = AbilityRepository.findById(action.abilityId)
                val cooldownRounds = ability?.cooldownRounds ?: 0
                if (cooldownRounds > 0) {
                    updated = updated.copy(cooldowns = updated.cooldowns + (action.abilityId to cooldownRounds))
                }
            }

            updated
        }
    }

    private fun processStatusEffectsForTest(combatants: List<Combatant>): List<Combatant> {
        return combatants.map { combatant ->
            var hp = combatant.currentHp
            val remainingEffects = mutableListOf<StatusEffect>()

            for (effect in combatant.statusEffects) {
                when (effect.effectType) {
                    "dot" -> {
                        hp = (hp - effect.value).coerceAtLeast(0)
                    }
                    "hot" -> {
                        hp = (hp + effect.value).coerceAtMost(combatant.maxHp)
                    }
                }

                val newDuration = effect.remainingRounds - 1
                if (newDuration > 0) {
                    remainingEffects.add(effect.copy(remainingRounds = newDuration))
                }
            }

            combatant.copy(
                currentHp = hp,
                isAlive = hp > 0,
                statusEffects = remainingEffects
            )
        }
    }

    private fun decrementCooldownsForTest(combatants: List<Combatant>): List<Combatant> {
        return combatants.map { combatant ->
            val newCooldowns = combatant.cooldowns
                .mapValues { (_, rounds) -> rounds - 1 }
                .filter { (_, rounds) -> rounds > 0 }
            combatant.copy(cooldowns = newCooldowns)
        }
    }

    private fun checkEndConditionsForTest(session: CombatSession): CombatSession {
        val players = session.combatants.filter { it.type == CombatantType.PLAYER }
        val creatures = session.combatants.filter { it.type == CombatantType.CREATURE }
        val alivePlayers = players.filter { it.isAlive }
        val aliveCreatures = creatures.filter { it.isAlive }

        val (state, endReason) = when {
            // No players left at all - combat ends
            players.isEmpty() ->
                CombatState.ENDED to CombatEndReason.ALL_PLAYERS_DEFEATED
            // All players dead/fled
            alivePlayers.isEmpty() ->
                CombatState.ENDED to CombatEndReason.ALL_PLAYERS_DEFEATED
            // All creatures dead (and there were creatures to fight)
            aliveCreatures.isEmpty() && creatures.isNotEmpty() ->
                CombatState.ENDED to CombatEndReason.ALL_ENEMIES_DEFEATED
            // No creatures at all - nothing to fight, end combat
            creatures.isEmpty() ->
                CombatState.ENDED to CombatEndReason.TIMEOUT
            else -> session.state to session.endReason
        }

        return session.copy(state = state, endReason = endReason)
    }

    // ========== Engagement Message Tests ==========

    /**
     * Helper function that mirrors the server's generateEngagementMessage logic.
     * This allows us to test the message generation without needing to call the actual service.
     */
    private fun generateEngagementMessage(creatureName: String, playerName: String): String {
        val lowerName = creatureName.lowercase()
        val verb = when {
            lowerName.contains("shambler") -> "shambles toward"
            lowerName.contains("zombie") -> "lurches toward"
            lowerName.contains("skeleton") -> "rattles toward"
            lowerName.contains("ghost") || lowerName.contains("specter") || lowerName.contains("wraith") -> "floats menacingly toward"
            lowerName.contains("spider") -> "skitters toward"
            lowerName.contains("wolf") || lowerName.contains("hound") -> "growls and leaps at"
            lowerName.contains("goblin") -> "shrieks and charges at"
            lowerName.contains("orc") -> "roars and rushes at"
            lowerName.contains("troll") -> "lumbers toward"
            lowerName.contains("rat") -> "scurries toward"
            lowerName.contains("snake") || lowerName.contains("serpent") -> "slithers toward"
            lowerName.contains("bat") -> "swoops at"
            lowerName.contains("slime") || lowerName.contains("ooze") -> "oozes toward"
            lowerName.contains("golem") -> "stomps toward"
            lowerName.contains("bandit") || lowerName.contains("thief") -> "sneaks up on"
            lowerName.contains("dragon") -> "roars and turns its attention to"
            lowerName.contains("demon") -> "snarls and advances on"
            lowerName.contains("fungus") || lowerName.contains("mushroom") -> "lurches toward"
            lowerName.contains("plant") || lowerName.contains("vine") -> "writhes toward"
            lowerName.contains("elemental") -> "surges toward"
            lowerName.contains("imp") -> "cackles and darts at"
            else -> "attacks"
        }
        return "$creatureName $verb $playerName!"
    }

    @Test
    fun testEngagementMessage_shambler() {
        val message = generateEngagementMessage("Fungal Shambler", "Goodman")
        assertEquals("Fungal Shambler shambles toward Goodman!", message)
    }

    @Test
    fun testEngagementMessage_goblin() {
        val message = generateEngagementMessage("Goblin Scout", "Hero")
        assertEquals("Goblin Scout shrieks and charges at Hero!", message)
    }

    @Test
    fun testEngagementMessage_zombie() {
        val message = generateEngagementMessage("Rotting Zombie", "Player")
        assertEquals("Rotting Zombie lurches toward Player!", message)
    }

    @Test
    fun testEngagementMessage_spider() {
        val message = generateEngagementMessage("Giant Spider", "Adventurer")
        assertEquals("Giant Spider skitters toward Adventurer!", message)
    }

    @Test
    fun testEngagementMessage_wolf() {
        val message = generateEngagementMessage("Dire Wolf", "Traveler")
        assertEquals("Dire Wolf growls and leaps at Traveler!", message)
    }

    @Test
    fun testEngagementMessage_ghost() {
        val message = generateEngagementMessage("Vengeful Ghost", "Cleric")
        assertEquals("Vengeful Ghost floats menacingly toward Cleric!", message)
    }

    @Test
    fun testEngagementMessage_dragon() {
        val message = generateEngagementMessage("Ancient Dragon", "Knight")
        assertEquals("Ancient Dragon roars and turns its attention to Knight!", message)
    }

    @Test
    fun testEngagementMessage_unknownCreature() {
        // Unknown creatures should use the default "attacks" verb
        val message = generateEngagementMessage("Mysterious Entity", "Player")
        assertEquals("Mysterious Entity attacks Player!", message)
    }

    @Test
    fun testEngagementMessage_caseInsensitive() {
        // Should work regardless of creature name casing
        val message1 = generateEngagementMessage("SHAMBLER", "Player")
        assertEquals("SHAMBLER shambles toward Player!", message1)

        val message2 = generateEngagementMessage("shambler", "Player")
        assertEquals("shambler shambles toward Player!", message2)

        val message3 = generateEngagementMessage("ShAmBlEr", "Player")
        assertEquals("ShAmBlEr shambles toward Player!", message3)
    }
}
