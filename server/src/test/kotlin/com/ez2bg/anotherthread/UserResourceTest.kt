package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import org.jetbrains.exposed.sql.deleteAll
import kotlin.test.*

/**
 * Unit tests for user resource management (HP, Mana, Stamina).
 * Tests edge cases around spending, restoration, and capping at max values.
 *
 * These tests target bugs found in:
 * - Resource spending when balance equals cost exactly
 * - Resource spending when balance is insufficient
 * - Healing/restoration capped at max values
 * - Negative value prevention
 */
class UserResourceTest {

    @BeforeTest
    fun setup() {
        TestDatabaseConfig.init()
        clearAllTablesForTest()
        seedTestData()
    }

    private fun clearAllTablesForTest() {
        org.jetbrains.exposed.sql.transactions.transaction {
            UserTable.deleteAll()
            CharacterClassTable.deleteAll()
        }
    }

    private fun seedTestData() {
        // Create a warrior class for resource calculation tests
        CharacterClassRepository.create(TestFixtures.warriorClass())
        CharacterClassRepository.create(TestFixtures.mageClass())
    }

    // ========== Mana Spending Tests ==========

    @Test
    fun testSpendMana_ExactAmount_ReturnsTrue() {
        // User has exactly 10 mana, spends 10
        val user = createTestUser(currentMana = 10, maxMana = 20)

        val result = UserRepository.spendMana(user.id, 10)

        assertTrue(result, "Should be able to spend exact mana amount")
        val updated = UserRepository.findById(user.id)!!
        assertEquals(0, updated.currentMana, "Mana should be 0 after spending exact amount")
    }

    @Test
    fun testSpendMana_InsufficientAmount_ReturnsFalse() {
        // User has 5 mana, tries to spend 10
        val user = createTestUser(currentMana = 5, maxMana = 20)

        val result = UserRepository.spendMana(user.id, 10)

        assertFalse(result, "Should not be able to spend more mana than available")
        val updated = UserRepository.findById(user.id)!!
        assertEquals(5, updated.currentMana, "Mana should be unchanged after failed spend")
    }

    @Test
    fun testSpendMana_ZeroMana_ReturnsFalse() {
        // User has 0 mana, tries to spend 1
        val user = createTestUser(currentMana = 0, maxMana = 20)

        val result = UserRepository.spendMana(user.id, 1)

        assertFalse(result, "Should not be able to spend mana when at 0")
    }

    @Test
    fun testSpendMana_NonexistentUser_ReturnsFalse() {
        val result = UserRepository.spendMana("nonexistent-user-id", 5)

        assertFalse(result, "Should return false for nonexistent user")
    }

    @Test
    fun testSpendMana_ZeroAmount_ReturnsTrue() {
        // Edge case: spending 0 mana should succeed
        val user = createTestUser(currentMana = 10, maxMana = 20)

        val result = UserRepository.spendMana(user.id, 0)

        assertTrue(result, "Spending 0 mana should succeed")
        val updated = UserRepository.findById(user.id)!!
        assertEquals(10, updated.currentMana, "Mana should be unchanged when spending 0")
    }

    // ========== Stamina Spending Tests ==========

    @Test
    fun testSpendStamina_ExactAmount_ReturnsTrue() {
        val user = createTestUser(currentStamina = 15, maxStamina = 30)

        val result = UserRepository.spendStamina(user.id, 15)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(0, updated.currentStamina)
    }

    @Test
    fun testSpendStamina_InsufficientAmount_ReturnsFalse() {
        val user = createTestUser(currentStamina = 5, maxStamina = 30)

        val result = UserRepository.spendStamina(user.id, 10)

        assertFalse(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(5, updated.currentStamina, "Stamina should be unchanged")
    }

    @Test
    fun testSpendStamina_ZeroStamina_ReturnsFalse() {
        val user = createTestUser(currentStamina = 0, maxStamina = 30)

        val result = UserRepository.spendStamina(user.id, 1)

        assertFalse(result)
    }

    // ========== HP Healing Tests ==========

    @Test
    fun testHeal_PartialHeal_IncreasesHp() {
        val user = createTestUser(currentHp = 10, maxHp = 30)

        val result = UserRepository.heal(user.id, 10)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(20, updated.currentHp)
    }

    @Test
    fun testHeal_OverHeal_CapsAtMax() {
        // Healing 50 when only 10 HP is missing should cap at max
        val user = createTestUser(currentHp = 20, maxHp = 30)

        val result = UserRepository.heal(user.id, 50)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(30, updated.currentHp, "HP should be capped at maxHp")
    }

    @Test
    fun testHeal_AtFullHp_StaysAtMax() {
        val user = createTestUser(currentHp = 30, maxHp = 30)

        val result = UserRepository.heal(user.id, 10)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(30, updated.currentHp, "HP should stay at max")
    }

    @Test
    fun testHeal_FromZeroHp_Heals() {
        // Edge case: healing when at 0 HP (downed player being revived)
        val user = createTestUser(currentHp = 0, maxHp = 30)

        val result = UserRepository.heal(user.id, 15)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(15, updated.currentHp)
    }

    @Test
    fun testHeal_ZeroAmount_ReturnsTrue() {
        val user = createTestUser(currentHp = 20, maxHp = 30)

        val result = UserRepository.heal(user.id, 0)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(20, updated.currentHp)
    }

    // ========== Mana Restoration Tests ==========

    @Test
    fun testRestoreMana_PartialRestore_IncreasesMana() {
        val user = createTestUser(currentMana = 5, maxMana = 20)

        val result = UserRepository.restoreMana(user.id, 10)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(15, updated.currentMana)
    }

    @Test
    fun testRestoreMana_OverRestore_CapsAtMax() {
        val user = createTestUser(currentMana = 15, maxMana = 20)

        val result = UserRepository.restoreMana(user.id, 100)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(20, updated.currentMana, "Mana should be capped at maxMana")
    }

    @Test
    fun testRestoreMana_FromZero_Restores() {
        val user = createTestUser(currentMana = 0, maxMana = 20)

        val result = UserRepository.restoreMana(user.id, 10)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(10, updated.currentMana)
    }

    // ========== Stamina Restoration Tests ==========

    @Test
    fun testRestoreStamina_PartialRestore_IncreasesStamina() {
        val user = createTestUser(currentStamina = 10, maxStamina = 30)

        val result = UserRepository.restoreStamina(user.id, 10)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(20, updated.currentStamina)
    }

    @Test
    fun testRestoreStamina_OverRestore_CapsAtMax() {
        val user = createTestUser(currentStamina = 25, maxStamina = 30)

        val result = UserRepository.restoreStamina(user.id, 100)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(30, updated.currentStamina, "Stamina should be capped at maxStamina")
    }

    // ========== Full Resource Restoration Tests ==========

    @Test
    fun testRestoreAllResources_RestoresToMax() {
        val user = createTestUser(
            currentHp = 10, maxHp = 30,
            currentMana = 5, maxMana = 20,
            currentStamina = 8, maxStamina = 25
        )

        val result = UserRepository.restoreAllResources(user.id)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(30, updated.currentHp, "HP should be at max")
        assertEquals(20, updated.currentMana, "Mana should be at max")
        assertEquals(25, updated.currentStamina, "Stamina should be at max")
    }

    @Test
    fun testRestoreAllResources_AlreadyFull_StaysAtMax() {
        val user = createTestUser(
            currentHp = 30, maxHp = 30,
            currentMana = 20, maxMana = 20,
            currentStamina = 25, maxStamina = 25
        )

        val result = UserRepository.restoreAllResources(user.id)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(30, updated.currentHp)
        assertEquals(20, updated.currentMana)
        assertEquals(25, updated.currentStamina)
    }

    @Test
    fun testRestoreAllResources_FromZero_RestoresToMax() {
        val user = createTestUser(
            currentHp = 0, maxHp = 30,
            currentMana = 0, maxMana = 20,
            currentStamina = 0, maxStamina = 25
        )

        val result = UserRepository.restoreAllResources(user.id)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(30, updated.currentHp)
        assertEquals(20, updated.currentMana)
        assertEquals(25, updated.currentStamina)
    }

    // ========== Gold Spending Tests ==========

    @Test
    fun testSpendGold_ExactAmount_ReturnsTrue() {
        val user = createTestUser(gold = 100)

        val result = UserRepository.spendGold(user.id, 100)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(0, updated.gold)
    }

    @Test
    fun testSpendGold_InsufficientAmount_ReturnsFalse() {
        val user = createTestUser(gold = 50)

        val result = UserRepository.spendGold(user.id, 100)

        assertFalse(result)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(50, updated.gold, "Gold should be unchanged")
    }

    @Test
    fun testSpendGold_ZeroGold_ReturnsFalse() {
        val user = createTestUser(gold = 0)

        val result = UserRepository.spendGold(user.id, 1)

        assertFalse(result)
    }

    // ========== Concurrent Resource Tests ==========

    @Test
    fun testMultipleSpendMana_SequentialCalls() {
        // Simulate multiple ability uses in sequence
        val user = createTestUser(currentMana = 20, maxMana = 20)

        assertTrue(UserRepository.spendMana(user.id, 5)) // 20 -> 15
        assertTrue(UserRepository.spendMana(user.id, 5)) // 15 -> 10
        assertTrue(UserRepository.spendMana(user.id, 5)) // 10 -> 5
        assertTrue(UserRepository.spendMana(user.id, 5)) // 5 -> 0
        assertFalse(UserRepository.spendMana(user.id, 5)) // 0 -> fail

        val updated = UserRepository.findById(user.id)!!
        assertEquals(0, updated.currentMana)
    }

    @Test
    fun testSpendThenRestore_ResourceCycle() {
        val user = createTestUser(currentMana = 20, maxMana = 20)

        // Spend all mana
        assertTrue(UserRepository.spendMana(user.id, 20))
        var updated = UserRepository.findById(user.id)!!
        assertEquals(0, updated.currentMana)

        // Restore some mana
        assertTrue(UserRepository.restoreMana(user.id, 10))
        updated = UserRepository.findById(user.id)!!
        assertEquals(10, updated.currentMana)

        // Spend again
        assertTrue(UserRepository.spendMana(user.id, 8))
        updated = UserRepository.findById(user.id)!!
        assertEquals(2, updated.currentMana)

        // Try to spend more than available
        assertFalse(UserRepository.spendMana(user.id, 5))
        updated = UserRepository.findById(user.id)!!
        assertEquals(2, updated.currentMana, "Mana should be unchanged after failed spend")
    }

    // ========== Helper Methods ==========

    private fun createTestUser(
        currentHp: Int = 30,
        maxHp: Int = 30,
        currentMana: Int = 20,
        maxMana: Int = 20,
        currentStamina: Int = 25,
        maxStamina: Int = 25,
        gold: Int = 0
    ): User {
        val user = User(
            id = "test-user-${System.nanoTime()}",
            name = "TestUser_${System.nanoTime()}",
            passwordHash = "test-hash",
            desc = "Test user for resource tests",
            currentHp = currentHp,
            maxHp = maxHp,
            currentMana = currentMana,
            maxMana = maxMana,
            currentStamina = currentStamina,
            maxStamina = maxStamina,
            gold = gold,
            characterClassId = TestFixtures.WARRIOR_CLASS_ID
        )
        UserRepository.create(user)
        return user
    }
}
