package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.api.CombatantDto
import com.ez2bg.anotherthread.api.CombatantType
import com.ez2bg.anotherthread.api.HealthUpdateResponse
import com.ez2bg.anotherthread.api.ResourceUpdateResponse
import com.ez2bg.anotherthread.combat.GlobalEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
}
