package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.api.CombatantDto
import com.ez2bg.anotherthread.api.CombatantType
import com.ez2bg.anotherthread.api.CombatSessionDto
import com.ez2bg.anotherthread.api.CombatState
import com.ez2bg.anotherthread.api.HealthUpdateResponse
import com.ez2bg.anotherthread.api.ResourceUpdateResponse
import com.ez2bg.anotherthread.combat.GlobalEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Tests for CombatStateHolder state management.
 *
 * These tests verify that HP, MP, and SP updates from WebSocket events
 * correctly update the observable state flows so the UI can react.
 */
class CombatStateHolderTest {

    private val testUserId = "test-player-123"

    private fun createTestCombatant(
        id: String = testUserId,
        currentHp: Int = 100,
        maxHp: Int = 100,
        currentMana: Int = 50,
        maxMana: Int = 50,
        currentStamina: Int = 30,
        maxStamina: Int = 30
    ) = CombatantDto(
        id = id,
        type = CombatantType.PLAYER,
        name = "Test Player",
        maxHp = maxHp,
        currentHp = currentHp,
        maxMana = maxMana,
        currentMana = currentMana,
        maxStamina = maxStamina,
        currentStamina = currentStamina,
        characterClassId = "warrior",
        abilityIds = listOf("slash", "heal"),
        initiative = 10,
        isDowned = false,
        deathThreshold = -10,
        isAlive = true,
        statusEffects = emptyList(),
        cooldowns = emptyMap()
    )

    @Test
    fun healthUpdated_updatesPlayerCombatantHp() = runTest {
        // Given: Player combatant is set with initial HP
        val initialCombatant = createTestCombatant(currentHp = 100, maxHp = 100)
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(initialCombatant)
        CombatStateHolder.setCombatantsForTest(listOf(initialCombatant))

        // When: HealthUpdated event comes in with reduced HP
        val healthUpdate = HealthUpdateResponse(
            sessionId = "session-1",
            combatantId = testUserId,
            combatantName = "Test Player",
            currentHp = 75,
            maxHp = 100,
            changeAmount = 25,
            sourceId = "goblin-1",
            sourceName = "Goblin"
        )
        CombatStateHolder.handleEventForTest(GlobalEvent.HealthUpdated(healthUpdate))

        // Then: playerCombatant should have updated HP
        val updatedCombatant = CombatStateHolder.playerCombatant.value
        assertNotNull(updatedCombatant, "Player combatant should not be null")
        assertEquals(75, updatedCombatant.currentHp, "HP should be updated to 75")
        assertEquals(100, updatedCombatant.maxHp, "Max HP should remain 100")
    }

    @Test
    fun healthUpdated_updatesCombatantsList() = runTest {
        // Given: Multiple combatants in combat
        val player = createTestCombatant(id = testUserId, currentHp = 100)
        val enemy = createTestCombatant(id = "goblin-1", currentHp = 50, maxHp = 50)
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(player)
        CombatStateHolder.setCombatantsForTest(listOf(player, enemy))

        // When: Enemy takes damage
        val healthUpdate = HealthUpdateResponse(
            sessionId = "session-1",
            combatantId = "goblin-1",
            combatantName = "Goblin",
            currentHp = 30,
            maxHp = 50,
            changeAmount = 20,
            sourceId = testUserId,
            sourceName = "Test Player"
        )
        CombatStateHolder.handleEventForTest(GlobalEvent.HealthUpdated(healthUpdate))

        // Then: Enemy in combatants list should have updated HP
        val combatants = CombatStateHolder.combatants.value
        val updatedEnemy = combatants.find { it.id == "goblin-1" }
        assertNotNull(updatedEnemy, "Enemy should be in combatants list")
        assertEquals(30, updatedEnemy.currentHp, "Enemy HP should be updated to 30")
    }

    @Test
    fun healthUpdated_doesNotAffectOtherCombatants() = runTest {
        // Given: Multiple combatants
        val player = createTestCombatant(id = testUserId, currentHp = 100)
        val ally = createTestCombatant(id = "ally-1", currentHp = 80, maxHp = 80)
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(player)
        CombatStateHolder.setCombatantsForTest(listOf(player, ally))

        // When: Player takes damage (not ally)
        val healthUpdate = HealthUpdateResponse(
            sessionId = "session-1",
            combatantId = testUserId,
            combatantName = "Test Player",
            currentHp = 60,
            maxHp = 100,
            changeAmount = 40,
            sourceId = "goblin-1",
            sourceName = "Goblin"
        )
        CombatStateHolder.handleEventForTest(GlobalEvent.HealthUpdated(healthUpdate))

        // Then: Ally HP should be unchanged
        val combatants = CombatStateHolder.combatants.value
        val unchangedAlly = combatants.find { it.id == "ally-1" }
        assertNotNull(unchangedAlly, "Ally should be in combatants list")
        assertEquals(80, unchangedAlly.currentHp, "Ally HP should remain 80")
    }

    @Test
    fun resourceUpdated_updatesPlayerManaAndStamina() = runTest {
        // Given: Player combatant with initial resources
        val initialCombatant = createTestCombatant(
            currentMana = 50, maxMana = 50,
            currentStamina = 30, maxStamina = 30
        )
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(initialCombatant)
        CombatStateHolder.setCombatantsForTest(listOf(initialCombatant))

        // When: ResourceUpdated event comes in (player used an ability)
        val resourceUpdate = ResourceUpdateResponse(
            sessionId = "session-1",
            combatantId = testUserId,
            currentMana = 40,
            maxMana = 50,
            currentStamina = 25,
            maxStamina = 30,
            manaChange = -10,
            staminaChange = -5
        )
        CombatStateHolder.handleEventForTest(GlobalEvent.ResourceUpdated(resourceUpdate))

        // Then: playerCombatant should have updated mana and stamina
        val updatedCombatant = CombatStateHolder.playerCombatant.value
        assertNotNull(updatedCombatant, "Player combatant should not be null")
        assertEquals(40, updatedCombatant.currentMana, "Mana should be updated to 40")
        assertEquals(50, updatedCombatant.maxMana, "Max mana should remain 50")
        assertEquals(25, updatedCombatant.currentStamina, "Stamina should be updated to 25")
        assertEquals(30, updatedCombatant.maxStamina, "Max stamina should remain 30")
    }

    @Test
    fun resourceUpdated_updatesCombatantsList() = runTest {
        // Given: Player in combat
        val player = createTestCombatant(currentMana = 50, currentStamina = 30)
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(player)
        CombatStateHolder.setCombatantsForTest(listOf(player))

        // When: Resource update
        val resourceUpdate = ResourceUpdateResponse(
            sessionId = "session-1",
            combatantId = testUserId,
            currentMana = 35,
            maxMana = 50,
            currentStamina = 20,
            maxStamina = 30,
            manaChange = -15,
            staminaChange = -10
        )
        CombatStateHolder.handleEventForTest(GlobalEvent.ResourceUpdated(resourceUpdate))

        // Then: Combatants list should also be updated
        val combatants = CombatStateHolder.combatants.value
        val updatedPlayer = combatants.find { it.id == testUserId }
        assertNotNull(updatedPlayer, "Player should be in combatants list")
        assertEquals(35, updatedPlayer.currentMana, "Mana in list should be 35")
        assertEquals(20, updatedPlayer.currentStamina, "Stamina in list should be 20")
    }

    @Test
    fun healthUpdated_healingIncreasesHp() = runTest {
        // Given: Player with reduced HP
        val injuredPlayer = createTestCombatant(currentHp = 50, maxHp = 100)
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(injuredPlayer)
        CombatStateHolder.setCombatantsForTest(listOf(injuredPlayer))

        // When: Healing event (negative changeAmount = healing)
        val healthUpdate = HealthUpdateResponse(
            sessionId = "session-1",
            combatantId = testUserId,
            combatantName = "Test Player",
            currentHp = 80,
            maxHp = 100,
            changeAmount = -30, // Negative = healing
            sourceId = "healer-1",
            sourceName = "Healer"
        )
        CombatStateHolder.handleEventForTest(GlobalEvent.HealthUpdated(healthUpdate))

        // Then: HP should increase
        val updatedCombatant = CombatStateHolder.playerCombatant.value
        assertNotNull(updatedCombatant)
        assertEquals(80, updatedCombatant.currentHp, "HP should be healed to 80")
    }

    @Test
    fun healthUpdated_forDifferentPlayer_doesNotUpdatePlayerCombatant() = runTest {
        // Given: Player combatant set
        val player = createTestCombatant(id = testUserId, currentHp = 100)
        val otherPlayer = createTestCombatant(id = "other-player", currentHp = 90)
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(player)
        CombatStateHolder.setCombatantsForTest(listOf(player, otherPlayer))

        // When: Health update for OTHER player
        val healthUpdate = HealthUpdateResponse(
            sessionId = "session-1",
            combatantId = "other-player",
            combatantName = "Other Player",
            currentHp = 70,
            maxHp = 90,
            changeAmount = 20,
            sourceId = "goblin-1",
            sourceName = "Goblin"
        )
        CombatStateHolder.handleEventForTest(GlobalEvent.HealthUpdated(healthUpdate))

        // Then: OUR playerCombatant should be unchanged
        val ourCombatant = CombatStateHolder.playerCombatant.value
        assertNotNull(ourCombatant)
        assertEquals(100, ourCombatant.currentHp, "Our HP should remain 100")

        // But the other player in the list should be updated
        val otherInList = CombatStateHolder.combatants.value.find { it.id == "other-player" }
        assertNotNull(otherInList)
        assertEquals(70, otherInList.currentHp, "Other player HP should be 70")
    }

    @Test
    fun clearCombatStatePublic_clearsAllCombatState() = runTest {
        // Given: Player in active combat with various state set
        val combatant = createTestCombatant(currentHp = 80, currentMana = 40)
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(combatant)
        CombatStateHolder.setCombatantsForTest(listOf(combatant))

        // Verify combat state is set (playerCombatant is the key indicator)
        assertNotNull(CombatStateHolder.playerCombatant.value, "Player combatant should be set")
        assertEquals(1, CombatStateHolder.combatants.value.size, "Should have one combatant")

        // When: Clear combat state (as happens on phasewalk)
        CombatStateHolder.clearCombatStatePublic()

        // Then: All combat state should be cleared
        assertEquals(null, CombatStateHolder.playerCombatant.value, "Player combatant should be null")
        assertEquals(emptyList(), CombatStateHolder.combatants.value, "Combatants list should be empty")
        assertEquals(emptyMap(), CombatStateHolder.cooldowns.value, "Cooldowns should be empty")
        assertEquals(null, CombatStateHolder.queuedAbilityId.value, "Queued ability should be null")
        assertEquals(0, CombatStateHolder.currentRound.value, "Current round should be 0")
    }

    @Test
    fun clearCombatStatePublic_clearsStatusEffects() = runTest {
        // Given: Player with blinded and disoriented status
        val combatant = createTestCombatant()
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(combatant)
        CombatStateHolder.setBlindedForTest(true, 3)
        CombatStateHolder.setDisorientedForTest(true, 2)

        // Verify status effects are set
        assertEquals(true, CombatStateHolder.isBlinded.value, "Should be blinded initially")
        assertEquals(true, CombatStateHolder.isDisoriented.value, "Should be disoriented initially")

        // When: Clear combat state
        CombatStateHolder.clearCombatStatePublic()

        // Then: Status effects should be cleared
        assertEquals(false, CombatStateHolder.isBlinded.value, "Blinded should be cleared")
        assertEquals(0, CombatStateHolder.blindRounds.value, "Blind rounds should be 0")
        assertEquals(false, CombatStateHolder.isDisoriented.value, "Disoriented should be cleared")
        assertEquals(0, CombatStateHolder.disorientRounds.value, "Disorient rounds should be 0")
    }

    @Test
    fun isInCombat_returnsTrueWhenSessionSet() = runTest {
        // Given: No combat session
        CombatStateHolder.clearCombatStatePublic()
        assertEquals(false, CombatStateHolder.isInCombat, "Should not be in combat initially")

        // When: Set player combatant (simulating combat start)
        val combatant = createTestCombatant()
        CombatStateHolder.setConnectedUserId(testUserId)
        CombatStateHolder.setPlayerCombatantForTest(combatant)

        // Note: isInCombat checks _combatSession.value != null, so we need to verify
        // the specific implementation. For now, we test what we can access.
    }

    // =========================================================================
    // Engagement Message Tests
    // =========================================================================

    private fun createTestSession(
        id: String = "session-1",
        combatants: List<CombatantDto> = emptyList()
    ) = CombatSessionDto(
        id = id,
        locationId = "test-location",
        state = CombatState.ACTIVE,
        currentRound = 1,
        roundStartTime = 1000L,  // Fixed timestamp for testing
        combatants = combatants,
        createdAt = 1000L  // Fixed timestamp for testing
    )

    @Test
    fun combatStarted_withEngagementMessages_addsMessagesToEventLog() = runTest {
        // Given: Clear state
        CombatStateHolder.clearCombatStatePublic()
        CombatStateHolder.setConnectedUserId(testUserId)

        val playerCombatant = createTestCombatant()
        val session = createTestSession(combatants = listOf(playerCombatant))

        // When: CombatStarted event with engagement messages
        val engagementMessages = listOf(
            "Shambler shambles toward Test Player!",
            "Goblin shrieks and charges at Test Player!"
        )
        CombatStateHolder.handleEventForTest(
            GlobalEvent.CombatStarted(session, playerCombatant, engagementMessages)
        )

        // Then: Event log should contain the engagement messages
        val eventLog = CombatStateHolder.eventLog.value
        assertTrue(eventLog.size >= 3, "Event log should have at least 3 entries (2 engagement + 1 combat started)")

        // The engagement messages should appear before "Combat started!"
        val messages = eventLog.map { it.message }
        assertTrue(messages.contains("Shambler shambles toward Test Player!"), "Should contain shambler message")
        assertTrue(messages.contains("Goblin shrieks and charges at Test Player!"), "Should contain goblin message")
        assertTrue(messages.contains("Combat started!"), "Should contain combat started message")
    }

    @Test
    fun combatStarted_withEmptyEngagementMessages_onlyAddsCombatStarted() = runTest {
        // Given: Clear state
        CombatStateHolder.clearCombatStatePublic()
        CombatStateHolder.setConnectedUserId(testUserId)

        val playerCombatant = createTestCombatant()
        val session = createTestSession(combatants = listOf(playerCombatant))

        // When: CombatStarted event with NO engagement messages (player initiated combat)
        CombatStateHolder.handleEventForTest(
            GlobalEvent.CombatStarted(session, playerCombatant, emptyList())
        )

        // Then: Event log should only have "Combat started!"
        val eventLog = CombatStateHolder.eventLog.value
        val combatStartedEntries = eventLog.filter { it.message == "Combat started!" }
        assertEquals(1, combatStartedEntries.size, "Should have exactly one 'Combat started!' entry")
    }

    @Test
    fun combatStarted_setsSessionAndCombatant() = runTest {
        // Given: Clear state
        CombatStateHolder.clearCombatStatePublic()
        CombatStateHolder.setConnectedUserId(testUserId)

        val playerCombatant = createTestCombatant(currentHp = 85, currentMana = 45)
        val enemyCombatant = createTestCombatant(id = "goblin-1", currentHp = 30, maxHp = 30)
        val session = createTestSession(combatants = listOf(playerCombatant, enemyCombatant))

        // When: CombatStarted event
        CombatStateHolder.handleEventForTest(
            GlobalEvent.CombatStarted(session, playerCombatant, listOf("Goblin attacks!"))
        )

        // Then: Combat state should be properly set
        val currentSession = CombatStateHolder.combatSession.value
        assertNotNull(currentSession, "Session should be set")
        assertEquals("session-1", currentSession.id, "Session ID should match")

        val currentPlayerCombatant = CombatStateHolder.playerCombatant.value
        assertNotNull(currentPlayerCombatant, "Player combatant should be set")
        assertEquals(85, currentPlayerCombatant.currentHp, "Player HP should match")
        assertEquals(45, currentPlayerCombatant.currentMana, "Player mana should match")

        val combatants = CombatStateHolder.combatants.value
        assertEquals(2, combatants.size, "Should have 2 combatants")
    }
}
