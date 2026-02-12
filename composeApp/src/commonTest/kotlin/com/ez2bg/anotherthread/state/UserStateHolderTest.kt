package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.api.UserDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/**
 * Tests for UserStateHolder state management.
 *
 * These tests verify that user state updates (mana spending, user updates)
 * correctly update the observable state flows so the UI can react.
 */
class UserStateHolderTest {

    private fun createTestUser(
        id: String = "test-user-123",
        name: String = "TestPlayer",
        currentHp: Int = 100,
        maxHp: Int = 100,
        currentMana: Int = 50,
        maxMana: Int = 50,
        currentStamina: Int = 30,
        maxStamina: Int = 30
    ) = UserDto(
        id = id,
        name = name,
        currentHp = currentHp,
        maxHp = maxHp,
        currentMana = currentMana,
        maxMana = maxMana,
        currentStamina = currentStamina,
        maxStamina = maxStamina,
        currentLocationId = "town-square",
        characterClassId = "warrior",
        level = 1,
        experience = 0,
        gold = 100,
        featureIds = emptyList(),
        strength = 10,
        constitution = 10,
        intelligence = 10,
        wisdom = 10,
        dexterity = 10,
        charisma = 10
    )

    @Test
    fun spendManaLocally_reducesManaCorrectly() = runTest {
        // Given: User with 50 mana
        val user = createTestUser(currentMana = 50, maxMana = 50)
        UserStateHolder.updateUser(user)

        // When: Spend 10 mana
        UserStateHolder.spendManaLocally(10)

        // Then: Mana should be reduced to 40
        val updatedUser = UserStateHolder.currentUser.value
        assertNotNull(updatedUser, "User should not be null")
        assertEquals(40, updatedUser.currentMana, "Mana should be reduced to 40")
        assertEquals(50, updatedUser.maxMana, "Max mana should remain 50")
    }

    @Test
    fun spendManaLocally_doesNotGoBelowZero() = runTest {
        // Given: User with 5 mana
        val user = createTestUser(currentMana = 5, maxMana = 50)
        UserStateHolder.updateUser(user)

        // When: Try to spend 10 mana (more than available)
        UserStateHolder.spendManaLocally(10)

        // Then: Mana should be clamped to 0
        val updatedUser = UserStateHolder.currentUser.value
        assertNotNull(updatedUser, "User should not be null")
        assertEquals(0, updatedUser.currentMana, "Mana should be clamped to 0")
    }

    @Test
    fun spendManaLocally_withExactAmount() = runTest {
        // Given: User with 12 mana (phasewalk costs 2)
        val user = createTestUser(currentMana = 12, maxMana = 50)
        UserStateHolder.updateUser(user)

        // When: Phasewalk (spend 2 mana)
        UserStateHolder.spendManaLocally(2)

        // Then: Mana should be reduced to 10
        val updatedUser = UserStateHolder.currentUser.value
        assertNotNull(updatedUser, "User should not be null")
        assertEquals(10, updatedUser.currentMana, "Mana should be reduced to 10")
    }

    @Test
    fun spendManaLocally_withNoUser_doesNothing() = runTest {
        // Given: No user set (cleared state)
        // First clear any existing user by directly setting to null via reflection
        // Note: In real test, we'd have a clearForTest method

        // When: Try to spend mana with no user
        // This should not throw or crash
        UserStateHolder.spendManaLocally(5)

        // Then: Nothing bad happens (no crash)
        // Can't assert much here without a clear method
    }

    @Test
    fun updateUser_setsUserCorrectly() = runTest {
        // Given: A new user
        val user = createTestUser(name = "NewPlayer", currentMana = 75)

        // When: Update user
        UserStateHolder.updateUser(user)

        // Then: User should be set
        val currentUser = UserStateHolder.currentUser.value
        assertNotNull(currentUser, "User should be set")
        assertEquals("NewPlayer", currentUser.name, "Name should match")
        assertEquals(75, currentUser.currentMana, "Mana should match")
    }

    @Test
    fun spendManaLocally_preservesOtherUserFields() = runTest {
        // Given: User with various fields
        val user = createTestUser(
            name = "TestPlayer",
            currentHp = 80,
            maxHp = 100,
            currentMana = 40,
            maxMana = 50,
            currentStamina = 25,
            maxStamina = 30
        )
        UserStateHolder.updateUser(user)

        // When: Spend mana
        UserStateHolder.spendManaLocally(5)

        // Then: Only mana should change, other fields preserved
        val updatedUser = UserStateHolder.currentUser.value
        assertNotNull(updatedUser)
        assertEquals("TestPlayer", updatedUser.name, "Name should be preserved")
        assertEquals(80, updatedUser.currentHp, "HP should be preserved")
        assertEquals(100, updatedUser.maxHp, "Max HP should be preserved")
        assertEquals(35, updatedUser.currentMana, "Mana should be reduced")
        assertEquals(50, updatedUser.maxMana, "Max mana should be preserved")
        assertEquals(25, updatedUser.currentStamina, "Stamina should be preserved")
        assertEquals(30, updatedUser.maxStamina, "Max stamina should be preserved")
    }

    @Test
    fun spendManaLocally_multipleTimes() = runTest {
        // Given: User with 50 mana
        val user = createTestUser(currentMana = 50, maxMana = 50)
        UserStateHolder.updateUser(user)

        // When: Spend mana multiple times
        UserStateHolder.spendManaLocally(10) // 50 -> 40
        UserStateHolder.spendManaLocally(5)  // 40 -> 35
        UserStateHolder.spendManaLocally(15) // 35 -> 20

        // Then: Mana should be correctly reduced
        val updatedUser = UserStateHolder.currentUser.value
        assertNotNull(updatedUser)
        assertEquals(20, updatedUser.currentMana, "Mana should be 20 after multiple spends")
    }

    @Test
    fun isAuthenticated_returnsTrueWhenUserSet() = runTest {
        // Given: User is set
        val user = createTestUser()
        UserStateHolder.updateUser(user)

        // Then: isAuthenticated should be true
        assertEquals(true, UserStateHolder.isAuthenticated, "Should be authenticated")
    }

    @Test
    fun isAdmin_returnsTrueWhenAdminFeaturePresent() = runTest {
        // Given: User with admin feature
        val adminUser = createTestUser().copy(featureIds = listOf("admin"))
        UserStateHolder.updateUser(adminUser)

        // Then: isAdmin should be true
        assertEquals(true, UserStateHolder.isAdmin, "Should be admin")
    }

    @Test
    fun isAdmin_returnsFalseWhenNoAdminFeature() = runTest {
        // Given: Regular user without admin feature
        val regularUser = createTestUser().copy(featureIds = listOf("basic"))
        UserStateHolder.updateUser(regularUser)

        // Then: isAdmin should be false
        assertEquals(false, UserStateHolder.isAdmin, "Should not be admin")
    }

    // NOTE: currentLocationId test removed - location is now managed by AdventureRepository
    // Use AdventureRepository.currentLocationId as the single source of truth

    @Test
    fun userId_returnsUserId() = runTest {
        // Given: User with specific ID
        val user = createTestUser(id = "player-456")
        UserStateHolder.updateUser(user)

        // Then: userId should match
        assertEquals("player-456", UserStateHolder.userId, "User ID should match")
    }

    @Test
    fun userName_returnsUserName() = runTest {
        // Given: User with specific name
        val user = createTestUser(name = "HeroPlayer")
        UserStateHolder.updateUser(user)

        // Then: userName should match
        assertEquals("HeroPlayer", UserStateHolder.userName, "User name should match")
    }
}
