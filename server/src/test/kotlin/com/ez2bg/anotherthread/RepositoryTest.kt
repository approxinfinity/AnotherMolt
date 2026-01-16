package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import java.io.File
import kotlin.test.*

/**
 * Tests for repository JSON serialization/deserialization logic.
 * These test the critical list-to-JSON and JSON-to-list conversions.
 */
class RepositoryTest {

    companion object {
        private var initialized = false
        private val testDbFile = File.createTempFile("test_db", ".db").also { it.deleteOnExit() }
    }

    @BeforeTest
    fun setup() {
        // Only initialize once - reuse the same database for all tests
        if (!initialized) {
            DatabaseConfig.init(testDbFile.absolutePath)
            initialized = true
        }
    }

    // ========== Location Repository Tests ==========

    @Test
    fun testCreateAndFindLocation() {
        val location = Location(
            name = "Test Dungeon",
            desc = "A dark and mysterious place",
            itemIds = listOf("item-1", "item-2"),
            creatureIds = listOf("creature-1"),
            exitIds = listOf("location-north", "location-south"),
            features = listOf("torch", "cobwebs")
        )

        val created = LocationRepository.create(location)
        assertEquals(location.id, created.id)
        assertEquals("Test Dungeon", created.name)

        val found = LocationRepository.findById(location.id)
        assertNotNull(found)
        assertEquals("Test Dungeon", found.name)
        assertEquals("A dark and mysterious place", found.desc)
        assertEquals(listOf("item-1", "item-2"), found.itemIds)
        assertEquals(listOf("creature-1"), found.creatureIds)
        assertEquals(listOf("location-north", "location-south"), found.exitIds)
        assertEquals(listOf("torch", "cobwebs"), found.features)
    }

    @Test
    fun testLocationWithEmptyLists() {
        val location = Location(
            name = "Empty Location",
            desc = "Nothing here",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exitIds = emptyList(),
            features = emptyList()
        )

        LocationRepository.create(location)
        val found = LocationRepository.findById(location.id)

        assertNotNull(found)
        assertTrue(found.itemIds.isEmpty())
        assertTrue(found.creatureIds.isEmpty())
        assertTrue(found.exitIds.isEmpty())
        assertTrue(found.features.isEmpty())
    }

    @Test
    fun testLocationWithSpecialCharacters() {
        val location = Location(
            name = "Location with \"quotes\"",
            desc = "Description with, commas, and \"quotes\"",
            itemIds = listOf("item-with-dash", "item_with_underscore"),
            creatureIds = emptyList(),
            exitIds = emptyList(),
            features = listOf("feature with spaces", "another-feature")
        )

        LocationRepository.create(location)
        val found = LocationRepository.findById(location.id)

        assertNotNull(found)
        assertEquals("Location with \"quotes\"", found.name)
        assertEquals(listOf("item-with-dash", "item_with_underscore"), found.itemIds)
        assertEquals(listOf("feature with spaces", "another-feature"), found.features)
    }

    @Test
    fun testUpdateLocation() {
        val location = Location(
            name = "Original Name",
            desc = "Original description",
            itemIds = listOf("item-1"),
            creatureIds = emptyList(),
            exitIds = emptyList(),
            features = emptyList()
        )
        LocationRepository.create(location)

        val updated = location.copy(
            name = "Updated Name",
            desc = "Updated description",
            itemIds = listOf("item-1", "item-2", "item-3")
        )
        val success = LocationRepository.update(updated)
        assertTrue(success)

        val found = LocationRepository.findById(location.id)
        assertNotNull(found)
        assertEquals("Updated Name", found.name)
        assertEquals("Updated description", found.desc)
        assertEquals(listOf("item-1", "item-2", "item-3"), found.itemIds)
    }

    @Test
    fun testUpdateLocationImageUrl() {
        val location = Location(
            name = "Location",
            desc = "Description",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exitIds = emptyList(),
            features = emptyList()
        )
        LocationRepository.create(location)

        val success = LocationRepository.updateImageUrl(location.id, "/images/location_${location.id}.png")
        assertTrue(success)

        val found = LocationRepository.findById(location.id)
        assertNotNull(found)
        assertEquals("/images/location_${location.id}.png", found.imageUrl)
    }

    @Test
    fun testFindAllLocations() {
        val location1 = Location(
            name = "Location 1",
            desc = "First",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exitIds = emptyList(),
            features = emptyList()
        )
        val location2 = Location(
            name = "Location 2",
            desc = "Second",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exitIds = emptyList(),
            features = emptyList()
        )
        LocationRepository.create(location1)
        LocationRepository.create(location2)

        val all = LocationRepository.findAll()
        assertTrue(all.size >= 2)
        assertTrue(all.any { it.name == "Location 1" })
        assertTrue(all.any { it.name == "Location 2" })
    }

    // ========== Creature Repository Tests ==========

    @Test
    fun testCreateAndFindCreature() {
        val creature = Creature(
            name = "Goblin",
            desc = "A sneaky green creature",
            itemIds = listOf("dagger", "gold-coin"),
            features = listOf("sneaky", "cowardly")
        )

        CreatureRepository.create(creature)
        val found = CreatureRepository.findById(creature.id)

        assertNotNull(found)
        assertEquals("Goblin", found.name)
        assertEquals("A sneaky green creature", found.desc)
        assertEquals(listOf("dagger", "gold-coin"), found.itemIds)
        assertEquals(listOf("sneaky", "cowardly"), found.features)
    }

    @Test
    fun testUpdateCreature() {
        val creature = Creature(
            name = "Orc",
            desc = "Big and mean",
            itemIds = listOf("club"),
            features = listOf("strong")
        )
        CreatureRepository.create(creature)

        val updated = creature.copy(
            name = "Orc Warrior",
            itemIds = listOf("club", "shield", "helmet")
        )
        val success = CreatureRepository.update(updated)
        assertTrue(success)

        val found = CreatureRepository.findById(creature.id)
        assertNotNull(found)
        assertEquals("Orc Warrior", found.name)
        assertEquals(listOf("club", "shield", "helmet"), found.itemIds)
    }

    @Test
    fun testUpdateCreatureImageUrl() {
        val creature = Creature(
            name = "Dragon",
            desc = "Fire breathing",
            itemIds = emptyList(),
            features = listOf("flying", "fire-breath")
        )
        CreatureRepository.create(creature)

        val success = CreatureRepository.updateImageUrl(creature.id, "/images/creature_${creature.id}.png")
        assertTrue(success)

        val found = CreatureRepository.findById(creature.id)
        assertNotNull(found)
        assertEquals("/images/creature_${creature.id}.png", found.imageUrl)
    }

    // ========== Item Repository Tests ==========

    @Test
    fun testCreateAndFindItem() {
        val item = Item(
            name = "Excalibur",
            desc = "The legendary sword of King Arthur",
            featureIds = listOf("holy", "unbreakable", "glowing")
        )

        ItemRepository.create(item)
        val found = ItemRepository.findById(item.id)

        assertNotNull(found)
        assertEquals("Excalibur", found.name)
        assertEquals("The legendary sword of King Arthur", found.desc)
        assertEquals(listOf("holy", "unbreakable", "glowing"), found.featureIds)
    }

    @Test
    fun testUpdateItem() {
        val item = Item(
            name = "Potion",
            desc = "A healing potion",
            featureIds = listOf("consumable")
        )
        ItemRepository.create(item)

        val updated = item.copy(
            name = "Greater Potion",
            desc = "A powerful healing potion",
            featureIds = listOf("consumable", "rare", "powerful")
        )
        val success = ItemRepository.update(updated)
        assertTrue(success)

        val found = ItemRepository.findById(item.id)
        assertNotNull(found)
        assertEquals("Greater Potion", found.name)
        assertEquals("A powerful healing potion", found.desc)
        assertEquals(listOf("consumable", "rare", "powerful"), found.featureIds)
    }

    @Test
    fun testUpdateItemImageUrl() {
        val item = Item(
            name = "Ring",
            desc = "A magical ring",
            featureIds = emptyList()
        )
        ItemRepository.create(item)

        val success = ItemRepository.updateImageUrl(item.id, "/images/item_${item.id}.png")
        assertTrue(success)

        val found = ItemRepository.findById(item.id)
        assertNotNull(found)
        assertEquals("/images/item_${item.id}.png", found.imageUrl)
    }

    @Test
    fun testFindByIdNotFound() {
        val location = LocationRepository.findById("nonexistent-id")
        assertNull(location)

        val creature = CreatureRepository.findById("nonexistent-id")
        assertNull(creature)

        val item = ItemRepository.findById("nonexistent-id")
        assertNull(item)
    }

    @Test
    fun testUpdateNonexistentEntity() {
        val location = Location(
            id = "nonexistent-location-id",
            name = "Ghost Location",
            desc = "Doesn't exist",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exitIds = emptyList(),
            features = emptyList()
        )
        val success = LocationRepository.update(location)
        assertFalse(success)
    }
}
