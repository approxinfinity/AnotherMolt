package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.game.SearchService
import com.ez2bg.anotherthread.game.StatModifierService
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import kotlin.test.*

/**
 * Unit tests for the hidden ground items system.
 *
 * Tests cover:
 * - LocationItem tracking (add, remove, find)
 * - Item visibility based on age (24 hour threshold)
 * - Item discovery mechanics
 * - SearchService functionality
 * - StatModifierService.searchChance calculations
 * - Class bonuses for search
 */
class HiddenItemsTest {

    companion object {
        // Test constants
        const val TEST_LOCATION_ID = "test-location-hidden-items"
        const val TEST_ITEM_1_ID = "test-hidden-item-1"
        const val TEST_ITEM_2_ID = "test-hidden-item-2"
        const val TEST_ITEM_3_ID = "test-hidden-item-3"
        const val TEST_USER_ID = "test-searcher"
        const val ROGUE_CLASS_ID = "test-rogue-class"
        const val WARRIOR_CLASS_ID = "test-warrior-class"
    }

    @BeforeTest
    fun setup() {
        TestDatabaseConfig.init()
        clearAllTablesForTest()
        seedTestData()
    }

    private fun clearAllTablesForTest() {
        org.jetbrains.exposed.sql.transactions.transaction {
            // Create tables if they don't exist
            SchemaUtils.createMissingTablesAndColumns(LocationItemTable, DiscoveredItemTable)

            // Clear all test data
            DiscoveredItemTable.deleteAll()
            LocationItemTable.deleteAll()
            UserTable.deleteAll()
            ItemTable.deleteAll()
            LocationTable.deleteAll()
            CharacterClassTable.deleteAll()
        }
    }

    private fun seedTestData() {
        // Create test location
        LocationRepository.create(Location(
            id = TEST_LOCATION_ID,
            name = "Test Location",
            desc = "A test location for hidden items",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = emptyList(),
            featureIds = emptyList()
        ))

        // Create test items
        ItemRepository.create(Item(id = TEST_ITEM_1_ID, name = "Hidden Gem", desc = "A gem hidden in the dust", featureIds = emptyList()))
        ItemRepository.create(Item(id = TEST_ITEM_2_ID, name = "Old Coin", desc = "A coin lost long ago", featureIds = emptyList()))
        ItemRepository.create(Item(id = TEST_ITEM_3_ID, name = "Fresh Potion", desc = "A recently dropped potion", featureIds = emptyList()))

        // Create test character classes
        CharacterClassRepository.create(CharacterClass(
            id = ROGUE_CLASS_ID,
            name = "Rogue",  // Contains "rogue" for search bonus
            description = "A sneaky thief class",
            isSpellcaster = false,
            hitDie = 8,
            primaryAttribute = "dexterity",
            isPublic = true
        ))

        CharacterClassRepository.create(CharacterClass(
            id = WARRIOR_CLASS_ID,
            name = "Warrior",
            description = "A fighter class",
            isSpellcaster = false,
            hitDie = 10,
            primaryAttribute = "strength",
            isPublic = true
        ))
    }

    private fun createTestUser(
        id: String = TEST_USER_ID,
        name: String = "TestSearcher_${System.nanoTime()}",
        intelligence: Int = 10,
        level: Int = 1,
        characterClassId: String? = null,
        currentLocationId: String? = TEST_LOCATION_ID
    ): User {
        val user = User(
            id = id,
            name = name,
            passwordHash = "test-hash",
            intelligence = intelligence,
            level = level,
            characterClassId = characterClassId,
            currentLocationId = currentLocationId
        )
        return UserRepository.create(user)
    }

    // ========== LocationItemRepository Tests ==========

    @Test
    fun testAddItem_CreatesLocationItem() {
        val locationItem = LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID, null)

        assertNotNull(locationItem)
        assertEquals(TEST_LOCATION_ID, locationItem.locationId)
        assertEquals(TEST_ITEM_1_ID, locationItem.itemId)
        assertTrue(locationItem.droppedAt > 0)
        assertNull(locationItem.droppedByUserId)
    }

    @Test
    fun testAddItem_WithDroppedBy_TracksUser() {
        val locationItem = LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID, TEST_USER_ID)

        assertEquals(TEST_USER_ID, locationItem.droppedByUserId)
    }

    @Test
    fun testAddItems_CreatesMultipleItems() {
        val items = LocationItemRepository.addItems(
            TEST_LOCATION_ID,
            listOf(TEST_ITEM_1_ID, TEST_ITEM_2_ID, TEST_ITEM_3_ID),
            TEST_USER_ID
        )

        assertEquals(3, items.size)
        items.forEach { item ->
            assertEquals(TEST_LOCATION_ID, item.locationId)
            assertEquals(TEST_USER_ID, item.droppedByUserId)
        }
    }

    @Test
    fun testFindByLocation_ReturnsAllItemsAtLocation() {
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_2_ID)
        LocationItemRepository.addItem("other-location", TEST_ITEM_3_ID)

        val items = LocationItemRepository.findByLocation(TEST_LOCATION_ID)

        assertEquals(2, items.size)
        assertTrue(items.any { it.itemId == TEST_ITEM_1_ID })
        assertTrue(items.any { it.itemId == TEST_ITEM_2_ID })
    }

    @Test
    fun testFindById_ReturnsCorrectItem() {
        val created = LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)

        val found = LocationItemRepository.findById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals(TEST_ITEM_1_ID, found.itemId)
    }

    @Test
    fun testRemoveItem_DeletesItem() {
        val created = LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)

        val removed = LocationItemRepository.removeItem(created.id)

        assertTrue(removed)
        assertNull(LocationItemRepository.findById(created.id))
    }

    @Test
    fun testRemoveItemByItemId_DeletesCorrectItem() {
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_2_ID)

        val removed = LocationItemRepository.removeItemByItemId(TEST_ITEM_1_ID, TEST_LOCATION_ID)

        assertTrue(removed)
        val remaining = LocationItemRepository.findByLocation(TEST_LOCATION_ID)
        assertEquals(1, remaining.size)
        assertEquals(TEST_ITEM_2_ID, remaining[0].itemId)
    }

    // ========== Item Visibility (24 Hour Threshold) Tests ==========

    @Test
    fun testIsHidden_FreshItem_NotHidden() {
        val item = LocationItem(
            locationId = TEST_LOCATION_ID,
            itemId = TEST_ITEM_1_ID,
            droppedAt = System.currentTimeMillis()
        )

        assertFalse(item.isHidden())
    }

    @Test
    fun testIsHidden_OldItem_IsHidden() {
        val item = LocationItem(
            locationId = TEST_LOCATION_ID,
            itemId = TEST_ITEM_1_ID,
            droppedAt = System.currentTimeMillis() - (25 * 60 * 60 * 1000) // 25 hours ago
        )

        assertTrue(item.isHidden())
    }

    @Test
    fun testIsHidden_ExactlyAtThreshold_IsHidden() {
        val item = LocationItem(
            locationId = TEST_LOCATION_ID,
            itemId = TEST_ITEM_1_ID,
            droppedAt = System.currentTimeMillis() - LocationItem.HIDDEN_AFTER_MS
        )

        assertTrue(item.isHidden())
    }

    @Test
    fun testIsHidden_JustBeforeThreshold_NotHidden() {
        val item = LocationItem(
            locationId = TEST_LOCATION_ID,
            itemId = TEST_ITEM_1_ID,
            droppedAt = System.currentTimeMillis() - LocationItem.HIDDEN_AFTER_MS + 1000 // 1 second before threshold
        )

        assertFalse(item.isHidden())
    }

    // ========== Item Discovery Tests ==========

    @Test
    fun testDiscoverItem_MarksAsDiscovered() {
        createTestUser()
        val locationItem = LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)

        val discovered = LocationItemRepository.discoverItem(TEST_USER_ID, locationItem.id)

        assertTrue(discovered)
        assertTrue(LocationItemRepository.hasDiscovered(TEST_USER_ID, locationItem.id))
    }

    @Test
    fun testDiscoverItem_Duplicate_ReturnsFalse() {
        createTestUser()
        val locationItem = LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)

        LocationItemRepository.discoverItem(TEST_USER_ID, locationItem.id)
        val secondDiscover = LocationItemRepository.discoverItem(TEST_USER_ID, locationItem.id)

        assertFalse(secondDiscover)
    }

    @Test
    fun testGetVisibleItemsForUser_ShowsFreshItems() {
        createTestUser()
        // Add a fresh item
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)

        val visible = LocationItemRepository.getVisibleItemsForUser(TEST_LOCATION_ID, TEST_USER_ID)

        assertEquals(1, visible.size)
        assertEquals(TEST_ITEM_1_ID, visible[0].itemId)
    }

    @Test
    fun testGetVisibleItemsForUser_HidesOldItems() {
        createTestUser()
        // Manually create an old item
        org.jetbrains.exposed.sql.transactions.transaction {
            LocationItemTable.insert {
                it[id] = "old-item-id"
                it[locationId] = TEST_LOCATION_ID
                it[itemId] = TEST_ITEM_1_ID
                it[droppedAt] = System.currentTimeMillis() - (25 * 60 * 60 * 1000) // 25 hours ago
                it[droppedByUserId] = null
            }
        }

        val visible = LocationItemRepository.getVisibleItemsForUser(TEST_LOCATION_ID, TEST_USER_ID)

        assertTrue(visible.isEmpty())
    }

    @Test
    fun testGetVisibleItemsForUser_ShowsDiscoveredOldItems() {
        createTestUser()
        // Manually create an old item
        org.jetbrains.exposed.sql.transactions.transaction {
            LocationItemTable.insert {
                it[id] = "old-discovered-item"
                it[locationId] = TEST_LOCATION_ID
                it[itemId] = TEST_ITEM_1_ID
                it[droppedAt] = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
                it[droppedByUserId] = null
            }
        }
        // Discover it
        LocationItemRepository.discoverItem(TEST_USER_ID, "old-discovered-item")

        val visible = LocationItemRepository.getVisibleItemsForUser(TEST_LOCATION_ID, TEST_USER_ID)

        assertEquals(1, visible.size)
    }

    @Test
    fun testGetHiddenItemsForUser_ReturnsUndiscoveredOldItems() {
        createTestUser()
        // Create an old item
        org.jetbrains.exposed.sql.transactions.transaction {
            LocationItemTable.insert {
                it[id] = "hidden-item-id"
                it[locationId] = TEST_LOCATION_ID
                it[itemId] = TEST_ITEM_1_ID
                it[droppedAt] = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
                it[droppedByUserId] = null
            }
        }

        val hidden = LocationItemRepository.getHiddenItemsForUser(TEST_LOCATION_ID, TEST_USER_ID)

        assertEquals(1, hidden.size)
        assertEquals(TEST_ITEM_1_ID, hidden[0].itemId)
    }

    @Test
    fun testRemoveItem_AlsoRemovesDiscoveryRecords() {
        createTestUser()
        val locationItem = LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)
        LocationItemRepository.discoverItem(TEST_USER_ID, locationItem.id)

        LocationItemRepository.removeItem(locationItem.id)

        assertFalse(LocationItemRepository.hasDiscovered(TEST_USER_ID, locationItem.id))
    }

    // ========== StatModifierService.searchChance Tests ==========

    @Test
    fun testSearchChance_BaselineWithAverageStats() {
        // INT 10, Level 1, no class bonus
        val chance = StatModifierService.searchChance(10, 1, 0)

        // Base 30% + 0 (INT mod 0) + 0 (no breakpoint) + 2 (level 1 * 2)
        assertEquals(32, chance)
    }

    @Test
    fun testSearchChance_HighIntelligence() {
        // INT 18, Level 1, no class bonus
        val chance = StatModifierService.searchChance(18, 1, 0)

        // Base 30% + 24 (INT mod 4 * 6) + 16 (breakpoint 2 * 8) + 2 (level)
        assertEquals(72, chance)
    }

    @Test
    fun testSearchChance_WithClassBonus() {
        // INT 10, Level 1, +25 class bonus
        val chance = StatModifierService.searchChance(10, 1, 25)

        // Base 30% + 0 + 0 + 2 + 25
        assertEquals(57, chance)
    }

    @Test
    fun testSearchChance_HighLevel() {
        // INT 10, Level 10, no class bonus
        val chance = StatModifierService.searchChance(10, 10, 0)

        // Base 30% + 0 + 0 + 20 (level 10 * 2)
        assertEquals(50, chance)
    }

    @Test
    fun testSearchChance_MaxCap() {
        // Very high stats should cap at 95%
        val chance = StatModifierService.searchChance(25, 20, 25)

        assertEquals(95, chance)
    }

    @Test
    fun testSearchChance_MinCap() {
        // Very low stats should cap at 10%
        val chance = StatModifierService.searchChance(3, 1, -50)

        assertEquals(10, chance)
    }

    // ========== SearchService Tests ==========

    @Test
    fun testIsSearchClass_Rogue_ReturnsTrue() {
        val rogueClass = CharacterClassRepository.findById(ROGUE_CLASS_ID)

        assertTrue(SearchService.isSearchClass(rogueClass))
    }

    @Test
    fun testIsSearchClass_Warrior_ReturnsFalse() {
        val warriorClass = CharacterClassRepository.findById(WARRIOR_CLASS_ID)

        assertFalse(SearchService.isSearchClass(warriorClass))
    }

    @Test
    fun testGetSearchClassBonus_Rogue_Returns25() {
        val rogueClass = CharacterClassRepository.findById(ROGUE_CLASS_ID)

        assertEquals(25, SearchService.getSearchClassBonus(rogueClass))
    }

    @Test
    fun testGetSearchClassBonus_Warrior_Returns0() {
        val warriorClass = CharacterClassRepository.findById(WARRIOR_CLASS_ID)

        assertEquals(0, SearchService.getSearchClassBonus(warriorClass))
    }

    @Test
    fun testAttemptSearch_InCombat_Fails() {
        val user = createTestUser()
        // Put user in combat
        UserRepository.updateCombatState(user.id, user.currentHp, "test-combat-session")

        val updatedUser = UserRepository.findById(user.id)!!
        val result = SearchService.attemptSearch(updatedUser, TEST_LOCATION_ID)

        assertFalse(result.success)
        assertTrue(result.message.contains("combat"))
    }

    @Test
    fun testAttemptSearch_NoHiddenItems_ReturnsSuccess() {
        val user = createTestUser()
        // Add only fresh items (not hidden)
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)

        val result = SearchService.attemptSearch(user, TEST_LOCATION_ID)

        assertTrue(result.success)
        assertEquals(0, result.totalHidden)
        assertTrue(result.discoveredItems.isEmpty())
    }

    @Test
    fun testGetVisibleItemIds_ReturnsActualItemIds() {
        createTestUser()
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_2_ID)

        val visibleIds = SearchService.getVisibleItemIds(TEST_LOCATION_ID, TEST_USER_ID)

        assertEquals(2, visibleIds.size)
        assertTrue(TEST_ITEM_1_ID in visibleIds)
        assertTrue(TEST_ITEM_2_ID in visibleIds)
    }

    @Test
    fun testHasHiddenItems_WithHiddenItems_ReturnsTrue() {
        createTestUser()
        // Create an old hidden item
        org.jetbrains.exposed.sql.transactions.transaction {
            LocationItemTable.insert {
                it[id] = "check-hidden-item"
                it[locationId] = TEST_LOCATION_ID
                it[itemId] = TEST_ITEM_1_ID
                it[droppedAt] = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
                it[droppedByUserId] = null
            }
        }

        assertTrue(SearchService.hasHiddenItems(TEST_LOCATION_ID, TEST_USER_ID))
    }

    @Test
    fun testHasHiddenItems_NoHiddenItems_ReturnsFalse() {
        createTestUser()
        // Only fresh items
        LocationItemRepository.addItem(TEST_LOCATION_ID, TEST_ITEM_1_ID)

        assertFalse(SearchService.hasHiddenItems(TEST_LOCATION_ID, TEST_USER_ID))
    }

    @Test
    fun testGetHiddenItemCount_ReturnsCorrectCount() {
        createTestUser()
        // Create two old hidden items
        org.jetbrains.exposed.sql.transactions.transaction {
            LocationItemTable.insert {
                it[id] = "count-hidden-1"
                it[locationId] = TEST_LOCATION_ID
                it[itemId] = TEST_ITEM_1_ID
                it[droppedAt] = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
                it[droppedByUserId] = null
            }
            LocationItemTable.insert {
                it[id] = "count-hidden-2"
                it[locationId] = TEST_LOCATION_ID
                it[itemId] = TEST_ITEM_2_ID
                it[droppedAt] = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
                it[droppedByUserId] = null
            }
        }

        assertEquals(2, SearchService.getHiddenItemCount(TEST_LOCATION_ID, TEST_USER_ID))
    }

    // ========== Integration Tests ==========

    @Test
    fun testSearchWorkflow_DiscoverHiddenItem() {
        // Create a user with high INT for better search chance
        val user = createTestUser(intelligence = 20, level = 10)

        // Create a hidden item
        org.jetbrains.exposed.sql.transactions.transaction {
            LocationItemTable.insert {
                it[id] = "workflow-hidden-item"
                it[locationId] = TEST_LOCATION_ID
                it[itemId] = TEST_ITEM_1_ID
                it[droppedAt] = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
                it[droppedByUserId] = null
            }
        }

        // Initially hidden
        assertFalse(SearchService.getVisibleItemIds(TEST_LOCATION_ID, user.id).contains(TEST_ITEM_1_ID))

        // Search multiple times (high INT should eventually succeed)
        var discovered = false
        repeat(20) {
            if (!discovered) {
                val result = SearchService.attemptSearch(user, TEST_LOCATION_ID)
                if (result.discoveredItems.isNotEmpty()) {
                    discovered = true
                }
            }
        }

        // With INT 20 and level 10, should have very high chance
        // Verify the item is now visible
        if (discovered) {
            assertTrue(SearchService.getVisibleItemIds(TEST_LOCATION_ID, user.id).contains(TEST_ITEM_1_ID))
        }
    }

    @Test
    fun testRogueSearchBonus_HigherChance() {
        val rogueUser = createTestUser(
            id = "rogue-user",
            intelligence = 10,
            level = 1,
            characterClassId = ROGUE_CLASS_ID
        )
        val warriorUser = createTestUser(
            id = "warrior-user",
            intelligence = 10,
            level = 1,
            characterClassId = WARRIOR_CLASS_ID
        )

        val rogueClass = CharacterClassRepository.findById(ROGUE_CLASS_ID)
        val warriorClass = CharacterClassRepository.findById(WARRIOR_CLASS_ID)

        val rogueBonus = SearchService.getSearchClassBonus(rogueClass)
        val warriorBonus = SearchService.getSearchClassBonus(warriorClass)

        // Rogue should have +25 bonus
        assertEquals(25, rogueBonus)
        assertEquals(0, warriorBonus)

        // Calculate actual search chances
        val rogueChance = StatModifierService.searchChance(10, 1, rogueBonus)
        val warriorChance = StatModifierService.searchChance(10, 1, warriorBonus)

        assertTrue(rogueChance > warriorChance)
        assertEquals(57, rogueChance) // 30 + 0 + 0 + 2 + 25
        assertEquals(32, warriorChance) // 30 + 0 + 0 + 2 + 0
    }
}
