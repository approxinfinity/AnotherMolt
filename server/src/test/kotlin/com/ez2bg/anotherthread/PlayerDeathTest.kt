package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.combat.*
import com.ez2bg.anotherthread.database.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.test.*

/**
 * Unit tests for player death mechanics.
 * Tests item dropping, gold loss, respawn at Home, and HP restoration.
 */
class PlayerDeathTest {

    companion object {
        private var initialized = false
        private val testDbFile = File.createTempFile("death_test_db_${System.nanoTime()}", ".db").also { it.deleteOnExit() }
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

        // Seed locations (including Home at 0,0)
        LocationRepository.create(TestFixtures.homeLocation())
        LocationRepository.create(TestFixtures.dungeonEntrance())
        LocationRepository.create(TestFixtures.dungeonRoom())

        // Seed creatures
        CreatureRepository.create(TestFixtures.goblin())
        CreatureRepository.create(TestFixtures.orc())
    }

    // ========== UserRepository.clearInventory Tests ==========

    @Test
    fun testClearInventoryRemovesAllItems() {
        // Create a player with items
        val player = TestFixtures.player1(
            itemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.POTION_ID, TestFixtures.SHIELD_ID)
        )
        UserRepository.create(player)

        // Clear inventory
        val droppedItems = UserRepository.clearInventory(player.id)

        // Verify dropped items returned
        assertEquals(3, droppedItems.size)
        assertTrue(droppedItems.contains(TestFixtures.SWORD_ID))
        assertTrue(droppedItems.contains(TestFixtures.POTION_ID))
        assertTrue(droppedItems.contains(TestFixtures.SHIELD_ID))

        // Verify user inventory is now empty
        val updatedPlayer = UserRepository.findById(player.id)
        assertNotNull(updatedPlayer)
        assertTrue(updatedPlayer.itemIds.isEmpty())
    }

    @Test
    fun testClearInventoryAlsoClearsEquippedItems() {
        // Create a player with equipped items
        val player = TestFixtures.player1(
            itemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.SHIELD_ID),
            equippedItemIds = listOf(TestFixtures.SWORD_ID)
        )
        UserRepository.create(player)

        // Clear inventory
        UserRepository.clearInventory(player.id)

        // Verify equipped items are also cleared
        val updatedPlayer = UserRepository.findById(player.id)
        assertNotNull(updatedPlayer)
        assertTrue(updatedPlayer.itemIds.isEmpty())
        assertTrue(updatedPlayer.equippedItemIds.isEmpty())
    }

    @Test
    fun testClearInventoryReturnsEmptyListWhenNoItems() {
        // Create a player with no items
        val player = TestFixtures.player1(itemIds = emptyList())
        UserRepository.create(player)

        // Clear inventory
        val droppedItems = UserRepository.clearInventory(player.id)

        // Should return empty list
        assertTrue(droppedItems.isEmpty())
    }

    // ========== UserRepository.removeItems Tests ==========

    @Test
    fun testRemoveItemsRemovesSpecificItems() {
        // Create a player with items
        val player = TestFixtures.player1(
            itemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.POTION_ID, TestFixtures.SHIELD_ID)
        )
        UserRepository.create(player)

        // Remove only the sword and potion
        val success = UserRepository.removeItems(player.id, listOf(TestFixtures.SWORD_ID, TestFixtures.POTION_ID))
        assertTrue(success)

        // Verify only shield remains
        val updatedPlayer = UserRepository.findById(player.id)
        assertNotNull(updatedPlayer)
        assertEquals(1, updatedPlayer.itemIds.size)
        assertTrue(updatedPlayer.itemIds.contains(TestFixtures.SHIELD_ID))
        assertFalse(updatedPlayer.itemIds.contains(TestFixtures.SWORD_ID))
        assertFalse(updatedPlayer.itemIds.contains(TestFixtures.POTION_ID))
    }

    @Test
    fun testRemoveItemsHandlesDuplicates() {
        // Create a player with duplicate items (2 potions)
        val player = TestFixtures.player1(
            itemIds = listOf(TestFixtures.POTION_ID, TestFixtures.POTION_ID, TestFixtures.SWORD_ID)
        )
        UserRepository.create(player)

        // Remove only ONE potion
        val success = UserRepository.removeItems(player.id, listOf(TestFixtures.POTION_ID))
        assertTrue(success)

        // Verify one potion and sword remain
        val updatedPlayer = UserRepository.findById(player.id)
        assertNotNull(updatedPlayer)
        assertEquals(2, updatedPlayer.itemIds.size)
        assertTrue(updatedPlayer.itemIds.contains(TestFixtures.POTION_ID))
        assertTrue(updatedPlayer.itemIds.contains(TestFixtures.SWORD_ID))
    }

    // ========== LocationRepository.addItems Tests ==========

    @Test
    fun testAddItemsToLocation() {
        // Get the dungeon entrance location
        val location = LocationRepository.findById(TestFixtures.DUNGEON_ENTRANCE_ID)
        assertNotNull(location)
        assertTrue(location.itemIds.isEmpty())

        // Add items to location
        val success = LocationRepository.addItems(
            TestFixtures.DUNGEON_ENTRANCE_ID,
            listOf(TestFixtures.SWORD_ID, TestFixtures.POTION_ID)
        )
        assertTrue(success)

        // Verify items are now at location
        val updatedLocation = LocationRepository.findById(TestFixtures.DUNGEON_ENTRANCE_ID)
        assertNotNull(updatedLocation)
        assertEquals(2, updatedLocation.itemIds.size)
        assertTrue(updatedLocation.itemIds.contains(TestFixtures.SWORD_ID))
        assertTrue(updatedLocation.itemIds.contains(TestFixtures.POTION_ID))
    }

    @Test
    fun testAddItemsAppendsToExistingItems() {
        // First add some items
        LocationRepository.addItems(TestFixtures.DUNGEON_ENTRANCE_ID, listOf(TestFixtures.SWORD_ID))

        // Then add more items
        LocationRepository.addItems(TestFixtures.DUNGEON_ENTRANCE_ID, listOf(TestFixtures.POTION_ID))

        // Verify both items are present
        val location = LocationRepository.findById(TestFixtures.DUNGEON_ENTRANCE_ID)
        assertNotNull(location)
        assertEquals(2, location.itemIds.size)
        assertTrue(location.itemIds.contains(TestFixtures.SWORD_ID))
        assertTrue(location.itemIds.contains(TestFixtures.POTION_ID))
    }

    // ========== Home Location Tests ==========

    @Test
    fun testHomeLocationExistsAtCoordinates() {
        val homeLocation = LocationRepository.findByCoordinates(0, 0, "overworld")
        assertNotNull(homeLocation)
        assertEquals(TestFixtures.HOME_LOCATION_ID, homeLocation.id)
        assertEquals("Town Square", homeLocation.name)
    }

    // ========== Gold Management Tests ==========

    @Test
    fun testAddNegativeGoldReducesBalance() {
        // Create a player with gold
        val player = TestFixtures.player1(gold = 100)
        UserRepository.create(player)

        // Remove gold by adding negative amount
        UserRepository.addGold(player.id, -100)

        // Verify gold is now zero
        val updatedPlayer = UserRepository.findById(player.id)
        assertNotNull(updatedPlayer)
        assertEquals(0, updatedPlayer.gold)
    }

    // ========== Heal to Full Tests ==========

    @Test
    fun testHealToFullRestoresMaxHp() {
        // Create a player with reduced HP
        val player = TestFixtures.player1(currentHp = 5, maxHp = 30)
        UserRepository.create(player)

        // Heal to full
        UserRepository.healToFull(player.id)

        // Verify HP is restored
        val updatedPlayer = UserRepository.findById(player.id)
        assertNotNull(updatedPlayer)
        assertEquals(30, updatedPlayer.currentHp)
    }

    // ========== Location Update Tests ==========

    @Test
    fun testUpdateCurrentLocationMovesPlayer() {
        // Create a player at dungeon entrance
        val player = TestFixtures.player1(currentLocationId = TestFixtures.DUNGEON_ENTRANCE_ID)
        UserRepository.create(player)

        // Move to home
        UserRepository.updateCurrentLocation(player.id, TestFixtures.HOME_LOCATION_ID)

        // Verify location changed
        val updatedPlayer = UserRepository.findById(player.id)
        assertNotNull(updatedPlayer)
        assertEquals(TestFixtures.HOME_LOCATION_ID, updatedPlayer.currentLocationId)
    }

    // ========== Integration Test: Full Death Scenario ==========

    @Test
    fun testFullDeathScenario() {
        // Setup: Create a player with items, gold, and a location
        val player = TestFixtures.player1(
            currentHp = 0, // Dead
            maxHp = 30,
            itemIds = listOf(TestFixtures.SWORD_ID, TestFixtures.POTION_ID),
            gold = 100,
            currentLocationId = TestFixtures.DUNGEON_ENTRANCE_ID,
            equippedItemIds = listOf(TestFixtures.SWORD_ID)
        )
        UserRepository.create(player)

        // Simulate death handling (what handlePlayerDeath does):
        // 1. Get death location
        val deathLocationId = player.currentLocationId!!

        // 2. Drop items at death location
        val droppedItems = UserRepository.clearInventory(player.id)
        LocationRepository.addItems(deathLocationId, droppedItems)

        // 3. Remove gold
        val goldLost = player.gold
        UserRepository.addGold(player.id, -goldLost)

        // 4. Respawn at Home with full HP
        val homeLocation = LocationRepository.findByCoordinates(0, 0, "overworld")!!
        UserRepository.updateCurrentLocation(player.id, homeLocation.id)
        UserRepository.healToFull(player.id)

        // Verify: Player state after death
        val respawnedPlayer = UserRepository.findById(player.id)
        assertNotNull(respawnedPlayer)
        assertEquals(homeLocation.id, respawnedPlayer.currentLocationId, "Player should be at Home")
        assertEquals(30, respawnedPlayer.currentHp, "Player should have full HP")
        assertTrue(respawnedPlayer.itemIds.isEmpty(), "Player should have no items")
        assertTrue(respawnedPlayer.equippedItemIds.isEmpty(), "Player should have no equipped items")
        assertEquals(0, respawnedPlayer.gold, "Player should have no gold")

        // Verify: Items at death location
        val deathLocation = LocationRepository.findById(deathLocationId)
        assertNotNull(deathLocation)
        assertEquals(2, deathLocation.itemIds.size, "Death location should have dropped items")
        assertTrue(deathLocation.itemIds.contains(TestFixtures.SWORD_ID))
        assertTrue(deathLocation.itemIds.contains(TestFixtures.POTION_ID))
    }

    @Test
    fun testDeathWithNoItemsOrGold() {
        // Setup: Create a player with nothing
        val player = TestFixtures.player1(
            currentHp = 0,
            maxHp = 30,
            itemIds = emptyList(),
            gold = 0,
            currentLocationId = TestFixtures.DUNGEON_ENTRANCE_ID
        )
        UserRepository.create(player)

        // Simulate death handling
        val droppedItems = UserRepository.clearInventory(player.id)
        val deathLocationId = player.currentLocationId!!
        if (droppedItems.isNotEmpty()) {
            LocationRepository.addItems(deathLocationId, droppedItems)
        }
        UserRepository.addGold(player.id, -player.gold)

        val homeLocation = LocationRepository.findByCoordinates(0, 0, "overworld")!!
        UserRepository.updateCurrentLocation(player.id, homeLocation.id)
        UserRepository.healToFull(player.id)

        // Verify: Player respawned correctly even with nothing to drop
        val respawnedPlayer = UserRepository.findById(player.id)
        assertNotNull(respawnedPlayer)
        assertEquals(homeLocation.id, respawnedPlayer.currentLocationId)
        assertEquals(30, respawnedPlayer.currentHp)

        // Death location should still be empty
        val deathLocation = LocationRepository.findById(deathLocationId)
        assertNotNull(deathLocation)
        assertTrue(deathLocation.itemIds.isEmpty())
    }
}
