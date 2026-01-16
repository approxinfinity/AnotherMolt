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

    // ========== User Repository Tests ==========

    @Test
    fun testCreateAndFindUser() {
        val user = User(
            name = "testuser_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("testpass"),
            desc = "A brave adventurer",
            itemIds = listOf("sword", "shield"),
            features = listOf("strong", "brave")
        )

        val created = UserRepository.create(user)
        assertEquals(user.id, created.id)

        val found = UserRepository.findById(user.id)
        assertNotNull(found)
        assertEquals(user.name, found.name)
        assertEquals("A brave adventurer", found.desc)
        assertEquals(listOf("sword", "shield"), found.itemIds)
        assertEquals(listOf("strong", "brave"), found.features)
    }

    @Test
    fun testFindUserByName() {
        val uniqueName = "findbyname_${System.currentTimeMillis()}"
        val user = User(
            name = uniqueName,
            passwordHash = UserRepository.hashPassword("testpass")
        )
        UserRepository.create(user)

        val found = UserRepository.findByName(uniqueName)
        assertNotNull(found)
        assertEquals(user.id, found.id)
        assertEquals(uniqueName, found.name)
    }

    @Test
    fun testFindUserByNameNotFound() {
        val found = UserRepository.findByName("nonexistent_user_${System.currentTimeMillis()}")
        assertNull(found)
    }

    @Test
    fun testUpdateUser() {
        val user = User(
            name = "updateuser_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("testpass"),
            desc = "Original description"
        )
        UserRepository.create(user)

        val updated = user.copy(
            desc = "Updated description",
            itemIds = listOf("new-item-1", "new-item-2"),
            features = listOf("upgraded")
        )
        val success = UserRepository.update(updated)
        assertTrue(success)

        val found = UserRepository.findById(user.id)
        assertNotNull(found)
        assertEquals("Updated description", found.desc)
        assertEquals(listOf("new-item-1", "new-item-2"), found.itemIds)
        assertEquals(listOf("upgraded"), found.features)
    }

    @Test
    fun testUpdateUserImageUrl() {
        val user = User(
            name = "imageuser_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("testpass")
        )
        UserRepository.create(user)

        val success = UserRepository.updateImageUrl(user.id, "/images/user_${user.id}.png")
        assertTrue(success)

        val found = UserRepository.findById(user.id)
        assertNotNull(found)
        assertEquals("/images/user_${user.id}.png", found.imageUrl)
    }

    @Test
    fun testUpdateUserCurrentLocation() {
        val user = User(
            name = "locuser_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("testpass")
        )
        UserRepository.create(user)

        val success = UserRepository.updateCurrentLocation(user.id, "location-123")
        assertTrue(success)

        val found = UserRepository.findById(user.id)
        assertNotNull(found)
        assertEquals("location-123", found.currentLocationId)
    }

    @Test
    fun testClearUserCurrentLocation() {
        val user = User(
            name = "clearloc_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("testpass"),
            currentLocationId = "some-location"
        )
        UserRepository.create(user)

        val success = UserRepository.updateCurrentLocation(user.id, null)
        assertTrue(success)

        val found = UserRepository.findById(user.id)
        assertNotNull(found)
        assertNull(found.currentLocationId)
    }

    @Test
    fun testPasswordHashAndVerify() {
        val password = "mysecretpassword"
        val hash = UserRepository.hashPassword(password)

        // Hash should be different from password
        assertNotEquals(password, hash)

        // Correct password should verify
        assertTrue(UserRepository.verifyPassword(password, hash))

        // Wrong password should not verify
        assertFalse(UserRepository.verifyPassword("wrongpassword", hash))
    }

    @Test
    fun testPasswordHashUniqueness() {
        val password = "samepassword"
        val hash1 = UserRepository.hashPassword(password)
        val hash2 = UserRepository.hashPassword(password)

        // BCrypt generates different hashes for same password (due to salt)
        assertNotEquals(hash1, hash2)

        // But both should verify correctly
        assertTrue(UserRepository.verifyPassword(password, hash1))
        assertTrue(UserRepository.verifyPassword(password, hash2))
    }

    @Test
    fun testUserToResponse() {
        val user = User(
            name = "responsetest_${System.currentTimeMillis()}",
            passwordHash = "secret_hash_should_not_appear",
            desc = "Test description",
            itemIds = listOf("item1"),
            features = listOf("feature1")
        )

        val response = user.toResponse()

        assertEquals(user.id, response.id)
        assertEquals(user.name, response.name)
        assertEquals(user.desc, response.desc)
        assertEquals(user.itemIds, response.itemIds)
        assertEquals(user.features, response.features)
        // Response should NOT contain password hash (it's a UserResponse, not User)
    }

    @Test
    fun testFindActiveUsersAtLocation() {
        val locationId = "active-test-location-${System.currentTimeMillis()}"

        // Create a user at the location with recent activity
        val activeUser = User(
            name = "activeuser_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("pass"),
            currentLocationId = locationId,
            lastActiveAt = System.currentTimeMillis()
        )
        UserRepository.create(activeUser)

        val activeUsers = UserRepository.findActiveUsersAtLocation(locationId)
        assertTrue(activeUsers.any { it.id == activeUser.id })
    }

    @Test
    fun testUpdateLastActiveAt() {
        val user = User(
            name = "lastactive_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("pass"),
            lastActiveAt = 0L
        )
        UserRepository.create(user)

        val success = UserRepository.updateLastActiveAt(user.id)
        assertTrue(success)

        val found = UserRepository.findById(user.id)
        assertNotNull(found)
        assertTrue(found.lastActiveAt > 0)
    }

    @Test
    fun testUserWithEmptyLists() {
        val user = User(
            name = "emptylistuser_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("pass"),
            itemIds = emptyList(),
            features = emptyList()
        )
        UserRepository.create(user)

        val found = UserRepository.findById(user.id)
        assertNotNull(found)
        assertTrue(found.itemIds.isEmpty())
        assertTrue(found.features.isEmpty())
    }

    @Test
    fun testUserWithSpecialCharactersInName() {
        val user = User(
            name = "user\"with'special_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("pass"),
            desc = "Description with \"quotes\" and, commas"
        )
        UserRepository.create(user)

        val found = UserRepository.findByName(user.name)
        assertNotNull(found)
        assertEquals(user.name, found.name)
        assertEquals("Description with \"quotes\" and, commas", found.desc)
    }
}
