package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import org.jetbrains.exposed.sql.deleteAll
import kotlin.test.*

/**
 * Unit tests for inventory and equipment management.
 * Tests edge cases around item equipping, unequipping, removal.
 *
 * These tests target bugs found in:
 * - Equip item not in inventory (should fail)
 * - Equip already-equipped item (should be idempotent)
 * - Remove items with duplicates
 * - Clear inventory also clears equipped items
 * - Shop buy/equip workflow
 */
class InventoryEquipmentTest {

    @BeforeTest
    fun setup() {
        TestDatabaseConfig.init()
        clearAllTablesForTest()
        seedTestData()
    }

    private fun clearAllTablesForTest() {
        org.jetbrains.exposed.sql.transactions.transaction {
            UserTable.deleteAll()
            ItemTable.deleteAll()
            CharacterClassTable.deleteAll()
        }
    }

    private fun seedTestData() {
        CharacterClassRepository.create(TestFixtures.warriorClass())
        ItemRepository.create(TestFixtures.sword())
        ItemRepository.create(TestFixtures.shield())
        ItemRepository.create(TestFixtures.potion())
    }

    // ========== Equip Item Tests ==========

    @Test
    fun testEquipItem_ItemInInventory_ReturnsTrue() {
        val user = createTestUserWithInventory(itemIds = listOf(TestFixtures.SWORD_ID))

        val result = UserRepository.equipItem(user.id, TestFixtures.SWORD_ID)

        assertTrue(result, "Should be able to equip item in inventory")
        val updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds, "Item should be in equipped list")
    }

    @Test
    fun testEquipItem_ItemNotInInventory_ReturnsFalse() {
        val user = createTestUserWithInventory(itemIds = emptyList())

        val result = UserRepository.equipItem(user.id, TestFixtures.SWORD_ID)

        assertFalse(result, "Should not be able to equip item not in inventory")
        val updated = UserRepository.findById(user.id)!!
        assertTrue(updated.equippedItemIds.isEmpty(), "No items should be equipped")
    }

    @Test
    fun testEquipItem_AlreadyEquipped_Idempotent() {
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID),
            equippedItemIds = listOf(TestFixtures.SWORD_ID)
        )

        val result = UserRepository.equipItem(user.id, TestFixtures.SWORD_ID)

        assertTrue(result, "Re-equipping should succeed (idempotent)")
        val updated = UserRepository.findById(user.id)!!
        // Should still only have one instance
        assertEquals(1, updated.equippedItemIds.count { it == TestFixtures.SWORD_ID })
    }

    @Test
    fun testEquipItem_NonexistentUser_ReturnsFalse() {
        val result = UserRepository.equipItem("nonexistent-user-id", TestFixtures.SWORD_ID)

        assertFalse(result)
    }

    @Test
    fun testEquipItem_MultipleItems_AllEquipped() {
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.SHIELD_ID)
        )

        UserRepository.equipItem(user.id, TestFixtures.SWORD_ID)
        UserRepository.equipItem(user.id, TestFixtures.SHIELD_ID)

        val updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds)
        assertTrue(TestFixtures.SHIELD_ID in updated.equippedItemIds)
        assertEquals(2, updated.equippedItemIds.size)
    }

    // ========== Unequip Item Tests ==========

    @Test
    fun testUnequipItem_EquippedItem_ReturnsTrue() {
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID),
            equippedItemIds = listOf(TestFixtures.SWORD_ID)
        )

        val result = UserRepository.unequipItem(user.id, TestFixtures.SWORD_ID)

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertFalse(TestFixtures.SWORD_ID in updated.equippedItemIds, "Item should be unequipped")
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds, "Item should still be in inventory")
    }

    @Test
    fun testUnequipItem_NotEquipped_ReturnsTrue_Idempotent() {
        // Unequipping an already-unequipped item is idempotent (returns true)
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID),
            equippedItemIds = emptyList()
        )

        val result = UserRepository.unequipItem(user.id, TestFixtures.SWORD_ID)

        assertTrue(result, "Should return true (idempotent - already unequipped)")
    }

    @Test
    fun testUnequipItem_ItemNotInInventory_ReturnsTrue_Idempotent() {
        // Unequipping an item that doesn't exist is also idempotent
        val user = createTestUserWithInventory(itemIds = emptyList())

        val result = UserRepository.unequipItem(user.id, TestFixtures.SWORD_ID)

        assertTrue(result, "Should return true (idempotent)")
    }

    @Test
    fun testUnequipItem_NonexistentUser_ReturnsFalse() {
        val result = UserRepository.unequipItem("nonexistent-user-id", TestFixtures.SWORD_ID)

        assertFalse(result)
    }

    // ========== Add Items Tests ==========

    @Test
    fun testAddItems_SingleItem_AddsToInventory() {
        val user = createTestUserWithInventory(itemIds = emptyList())

        val result = UserRepository.addItems(user.id, listOf(TestFixtures.SWORD_ID))

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds)
    }

    @Test
    fun testAddItems_MultipleItems_AllAdded() {
        val user = createTestUserWithInventory(itemIds = emptyList())

        val result = UserRepository.addItems(user.id, listOf(TestFixtures.SWORD_ID, TestFixtures.SHIELD_ID, TestFixtures.POTION_ID))

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds)
        assertTrue(TestFixtures.SHIELD_ID in updated.itemIds)
        assertTrue(TestFixtures.POTION_ID in updated.itemIds)
        assertEquals(3, updated.itemIds.size)
    }

    @Test
    fun testAddItems_StackableItem_AllowsDuplicates() {
        val user = createTestUserWithInventory(itemIds = listOf(TestFixtures.POTION_ID))

        // Add another potion (potions are stackable)
        val result = UserRepository.addItems(user.id, listOf(TestFixtures.POTION_ID))

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        // Should have 2 potions now since potions are stackable
        assertEquals(2, updated.itemIds.count { it == TestFixtures.POTION_ID })
    }

    @Test
    fun testAddItems_NonStackableItem_BlocksDuplicates() {
        val user = createTestUserWithInventory(itemIds = listOf(TestFixtures.SWORD_ID))

        // Try to add a second sword (swords are not stackable)
        val result = UserRepository.addItems(user.id, listOf(TestFixtures.SWORD_ID))

        assertTrue(result) // Returns true (no error), just silently skips
        val updated = UserRepository.findById(user.id)!!
        // Should still have only 1 sword
        assertEquals(1, updated.itemIds.count { it == TestFixtures.SWORD_ID })
    }

    @Test
    fun testAddItems_ToExistingInventory_Appends() {
        val user = createTestUserWithInventory(itemIds = listOf(TestFixtures.SWORD_ID))

        UserRepository.addItems(user.id, listOf(TestFixtures.SHIELD_ID))

        val updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds, "Original item should remain")
        assertTrue(TestFixtures.SHIELD_ID in updated.itemIds, "New item should be added")
        assertEquals(2, updated.itemIds.size)
    }

    // ========== Remove Items Tests ==========

    @Test
    fun testRemoveItems_SingleItem_Removes() {
        val user = createTestUserWithInventory(itemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.SHIELD_ID))

        val result = UserRepository.removeItems(user.id, listOf(TestFixtures.SWORD_ID))

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertFalse(TestFixtures.SWORD_ID in updated.itemIds, "Removed item should be gone")
        assertTrue(TestFixtures.SHIELD_ID in updated.itemIds, "Other item should remain")
    }

    @Test
    fun testRemoveItems_ItemNotInInventory_StillReturnsTrue() {
        // Note: Current implementation returns true if update succeeds, even if item wasn't found
        val user = createTestUserWithInventory(itemIds = listOf(TestFixtures.SWORD_ID))

        val result = UserRepository.removeItems(user.id, listOf(TestFixtures.POTION_ID))

        assertTrue(result, "Update succeeds even if item wasn't in inventory")
        val updated = UserRepository.findById(user.id)!!
        assertEquals(1, updated.itemIds.size, "Inventory should be unchanged")
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds, "Original item should remain")
    }

    @Test
    fun testRemoveItems_EquippedItem_RemovedFromInventoryButNotEquipped() {
        // Note: removeItems only removes from inventory, not from equipped list
        // This is a known behavior - caller should unequip first if needed
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID),
            equippedItemIds = listOf(TestFixtures.SWORD_ID)
        )

        val result = UserRepository.removeItems(user.id, listOf(TestFixtures.SWORD_ID))

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        assertFalse(TestFixtures.SWORD_ID in updated.itemIds, "Item should be removed from inventory")
        // Note: equipped list is NOT automatically updated by removeItems
        // This could be considered a bug or a feature depending on intended behavior
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds, "Equipped list is NOT automatically updated by removeItems")
    }

    @Test
    fun testRemoveItems_WithDuplicates_RemovesOnlyOne() {
        // User has 3 potions
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.POTION_ID, TestFixtures.POTION_ID, TestFixtures.POTION_ID)
        )

        // Remove 1 potion
        val result = UserRepository.removeItems(user.id, listOf(TestFixtures.POTION_ID))

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        // Should have 2 potions left
        assertEquals(2, updated.itemIds.count { it == TestFixtures.POTION_ID }, "Should only remove one instance")
    }

    @Test
    fun testRemoveItems_MultipleOfSameItem_RemovesMultiple() {
        // User has 3 potions
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.POTION_ID, TestFixtures.POTION_ID, TestFixtures.POTION_ID)
        )

        // Remove 2 potions
        val result = UserRepository.removeItems(user.id, listOf(TestFixtures.POTION_ID, TestFixtures.POTION_ID))

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!
        // Should have 1 potion left
        assertEquals(1, updated.itemIds.count { it == TestFixtures.POTION_ID })
    }

    // ========== Clear Inventory Tests ==========

    @Test
    fun testClearInventory_RemovesAllItems() {
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.SHIELD_ID, TestFixtures.POTION_ID)
        )

        val removedItems = UserRepository.clearInventory(user.id)

        assertEquals(3, removedItems.size, "Should return all removed items")
        val updated = UserRepository.findById(user.id)!!
        assertTrue(updated.itemIds.isEmpty(), "Inventory should be empty")
    }

    @Test
    fun testClearInventory_AlsoClearsEquipped() {
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.SHIELD_ID),
            equippedItemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.SHIELD_ID)
        )

        val removedItems = UserRepository.clearInventory(user.id)

        assertEquals(2, removedItems.size)
        val updated = UserRepository.findById(user.id)!!
        assertTrue(updated.itemIds.isEmpty(), "Inventory should be empty")
        assertTrue(updated.equippedItemIds.isEmpty(), "Equipped items should also be cleared")
    }

    @Test
    fun testClearInventory_EmptyInventory_ReturnsEmptyList() {
        val user = createTestUserWithInventory(itemIds = emptyList())

        val removedItems = UserRepository.clearInventory(user.id)

        assertTrue(removedItems.isEmpty(), "Clearing empty inventory should return empty list")
    }

    // ========== Equip/Unequip Workflow Tests ==========

    @Test
    fun testEquipUnequipCycle() {
        val user = createTestUserWithInventory(itemIds = listOf(TestFixtures.SWORD_ID))

        // Equip
        assertTrue(UserRepository.equipItem(user.id, TestFixtures.SWORD_ID))
        var updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds)

        // Unequip
        assertTrue(UserRepository.unequipItem(user.id, TestFixtures.SWORD_ID))
        updated = UserRepository.findById(user.id)!!
        assertFalse(TestFixtures.SWORD_ID in updated.equippedItemIds)
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds, "Item should still be in inventory")

        // Re-equip
        assertTrue(UserRepository.equipItem(user.id, TestFixtures.SWORD_ID))
        updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds)
    }

    @Test
    fun testBuyAndEquipWorkflow() {
        val user = createTestUserWithInventory(itemIds = emptyList(), gold = 100)

        // Simulate buying an item (add to inventory, subtract gold)
        UserRepository.addItems(user.id, listOf(TestFixtures.SWORD_ID))
        UserRepository.spendGold(user.id, 50)

        var updated = UserRepository.findById(user.id)!!
        assertEquals(50, updated.gold)
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds)

        // Equip the purchased item
        assertTrue(UserRepository.equipItem(user.id, TestFixtures.SWORD_ID))
        updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds)
    }

    @Test
    fun testSellEquippedItem_RequiresExplicitUnequip() {
        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID),
            equippedItemIds = listOf(TestFixtures.SWORD_ID),
            gold = 0
        )

        // Proper sell workflow: unequip first, then remove, then add gold
        assertTrue(UserRepository.unequipItem(user.id, TestFixtures.SWORD_ID))
        assertTrue(UserRepository.removeItems(user.id, listOf(TestFixtures.SWORD_ID)))
        UserRepository.addGold(user.id, 25)  // Add sale proceeds

        val updated = UserRepository.findById(user.id)!!
        assertFalse(TestFixtures.SWORD_ID in updated.itemIds)
        assertFalse(TestFixtures.SWORD_ID in updated.equippedItemIds)
        assertEquals(25, updated.gold)
    }

    // ========== Equipment Slot Tests ==========

    @Test
    fun testEquipItem_SameSlot_ShouldNotDuplicateInRepository() {
        // Create a second sword in the same slot
        val secondSword = Item(
            id = "test-sword-2",
            name = "Second Sword",
            desc = "Another sword for the same slot",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",  // Same slot as TestFixtures.SWORD_ID
            value = 60
        )
        ItemRepository.create(secondSword)

        val user = createTestUserWithInventory(
            itemIds = listOf(TestFixtures.SWORD_ID, "test-sword-2")
        )

        // Equip first sword
        UserRepository.equipItem(user.id, TestFixtures.SWORD_ID)
        var updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds)

        // Note: Repository-level equipItem doesn't auto-unequip same slot
        // That logic is in the route handler. At repository level, both can be equipped.
        UserRepository.equipItem(user.id, "test-sword-2")
        updated = UserRepository.findById(user.id)!!
        assertTrue("test-sword-2" in updated.equippedItemIds)
        // Both are now equipped at repository level
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds)
    }

    @Test
    fun testPickupAndEquipWorkflow() {
        // Simulates the full flow: item at location -> pickup -> equip
        val user = createTestUserWithInventory(itemIds = emptyList())

        // Add item to inventory (simulating pickup)
        UserRepository.addItems(user.id, listOf(TestFixtures.SWORD_ID))
        var updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds, "Item should be in inventory after pickup")
        assertFalse(TestFixtures.SWORD_ID in updated.equippedItemIds, "Item should not be equipped after pickup")

        // Equip the item
        UserRepository.equipItem(user.id, TestFixtures.SWORD_ID)
        updated = UserRepository.findById(user.id)!!
        assertTrue(TestFixtures.SWORD_ID in updated.equippedItemIds, "Item should be equipped after equip")
        assertTrue(TestFixtures.SWORD_ID in updated.itemIds, "Item should still be in inventory when equipped")
    }

    // ========== Helper Methods ==========

    private fun createTestUserWithInventory(
        itemIds: List<String> = emptyList(),
        equippedItemIds: List<String> = emptyList(),
        gold: Int = 0
    ): User {
        val user = User(
            id = "test-user-${System.nanoTime()}",
            name = "TestUser_${System.nanoTime()}",
            passwordHash = "test-hash",
            desc = "Test user for inventory tests",
            currentHp = 30,
            maxHp = 30,
            itemIds = itemIds,
            equippedItemIds = equippedItemIds,
            gold = gold,
            characterClassId = TestFixtures.WARRIOR_CLASS_ID
        )
        UserRepository.create(user)
        return user
    }
}
