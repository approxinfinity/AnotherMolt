package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import kotlin.test.*

/**
 * Tests for IdentifiedEntityRepository.
 * Tests the entity identification persistence system that tracks
 * which items/creatures have been identified by each user.
 */
class IdentifiedEntityTest {

    @BeforeTest
    fun setup() {
        TestDatabaseConfig.init()
        // Clear all tables before each test for isolation
        DatabaseConfig.clearAllTables()
    }

    // ========== Basic Identification Tests ==========

    @Test
    fun testIdentifyItemFirstTime() {
        val userId = TestFixtures.PLAYER_1_ID
        val itemId = TestFixtures.SWORD_ID

        val result = IdentifiedEntityRepository.identify(userId, itemId, "item")

        assertTrue(result, "First identification should return true")
    }

    @Test
    fun testIdentifyItemSecondTimeReturnsFalse() {
        val userId = TestFixtures.PLAYER_1_ID
        val itemId = TestFixtures.SWORD_ID

        val firstResult = IdentifiedEntityRepository.identify(userId, itemId, "item")
        val secondResult = IdentifiedEntityRepository.identify(userId, itemId, "item")

        assertTrue(firstResult, "First identification should return true")
        assertFalse(secondResult, "Second identification should return false (already identified)")
    }

    @Test
    fun testIdentifyCreatureFirstTime() {
        val userId = TestFixtures.PLAYER_1_ID
        val creatureId = TestFixtures.GOBLIN_ID

        val result = IdentifiedEntityRepository.identify(userId, creatureId, "creature")

        assertTrue(result, "First identification should return true")
    }

    @Test
    fun testIdentifyCreatureSecondTimeReturnsFalse() {
        val userId = TestFixtures.PLAYER_1_ID
        val creatureId = TestFixtures.GOBLIN_ID

        val firstResult = IdentifiedEntityRepository.identify(userId, creatureId, "creature")
        val secondResult = IdentifiedEntityRepository.identify(userId, creatureId, "creature")

        assertTrue(firstResult, "First identification should return true")
        assertFalse(secondResult, "Second identification should return false (already identified)")
    }

    // ========== isIdentified Tests ==========

    @Test
    fun testIsIdentifiedReturnsTrueAfterIdentify() {
        val userId = TestFixtures.PLAYER_1_ID
        val itemId = TestFixtures.SWORD_ID

        assertFalse(IdentifiedEntityRepository.isIdentified(userId, itemId, "item"))

        IdentifiedEntityRepository.identify(userId, itemId, "item")

        assertTrue(IdentifiedEntityRepository.isIdentified(userId, itemId, "item"))
    }

    @Test
    fun testIsIdentifiedReturnsFalseForUnidentified() {
        val userId = TestFixtures.PLAYER_1_ID
        val itemId = TestFixtures.SWORD_ID

        val result = IdentifiedEntityRepository.isIdentified(userId, itemId, "item")

        assertFalse(result, "Unidentified entity should return false")
    }

    @Test
    fun testIsIdentifiedDifferentiatesBetweenEntityTypes() {
        val userId = TestFixtures.PLAYER_1_ID
        val entityId = "shared-entity-id"

        // Identify as item
        IdentifiedEntityRepository.identify(userId, entityId, "item")

        // Should be identified as item, not as creature
        assertTrue(IdentifiedEntityRepository.isIdentified(userId, entityId, "item"))
        assertFalse(IdentifiedEntityRepository.isIdentified(userId, entityId, "creature"))
    }

    // ========== User Isolation Tests ==========

    @Test
    fun testIdentificationIsUserSpecific() {
        val user1 = TestFixtures.PLAYER_1_ID
        val user2 = TestFixtures.PLAYER_2_ID
        val itemId = TestFixtures.SWORD_ID

        // User 1 identifies the item
        IdentifiedEntityRepository.identify(user1, itemId, "item")

        // User 1 should see it as identified, user 2 should not
        assertTrue(IdentifiedEntityRepository.isIdentified(user1, itemId, "item"))
        assertFalse(IdentifiedEntityRepository.isIdentified(user2, itemId, "item"))
    }

    @Test
    fun testDifferentUsersCanIdentifySameItem() {
        val user1 = TestFixtures.PLAYER_1_ID
        val user2 = TestFixtures.PLAYER_2_ID
        val itemId = TestFixtures.SWORD_ID

        val result1 = IdentifiedEntityRepository.identify(user1, itemId, "item")
        val result2 = IdentifiedEntityRepository.identify(user2, itemId, "item")

        // Both should succeed (first time for each user)
        assertTrue(result1)
        assertTrue(result2)

        // Both should now see it as identified
        assertTrue(IdentifiedEntityRepository.isIdentified(user1, itemId, "item"))
        assertTrue(IdentifiedEntityRepository.isIdentified(user2, itemId, "item"))
    }

    // ========== findByUser Tests ==========

    @Test
    fun testFindByUserReturnsAllIdentifications() {
        val userId = TestFixtures.PLAYER_1_ID

        IdentifiedEntityRepository.identify(userId, TestFixtures.SWORD_ID, "item")
        IdentifiedEntityRepository.identify(userId, TestFixtures.POTION_ID, "item")
        IdentifiedEntityRepository.identify(userId, TestFixtures.GOBLIN_ID, "creature")

        val identifications = IdentifiedEntityRepository.findByUser(userId)

        assertEquals(3, identifications.size)
        assertTrue(identifications.any { it.entityId == TestFixtures.SWORD_ID && it.entityType == "item" })
        assertTrue(identifications.any { it.entityId == TestFixtures.POTION_ID && it.entityType == "item" })
        assertTrue(identifications.any { it.entityId == TestFixtures.GOBLIN_ID && it.entityType == "creature" })
    }

    @Test
    fun testFindByUserReturnsEmptyForNewUser() {
        val userId = "new-user-with-no-identifications"

        val identifications = IdentifiedEntityRepository.findByUser(userId)

        assertTrue(identifications.isEmpty())
    }

    @Test
    fun testFindByUserOnlyReturnsOwnIdentifications() {
        val user1 = TestFixtures.PLAYER_1_ID
        val user2 = TestFixtures.PLAYER_2_ID

        IdentifiedEntityRepository.identify(user1, TestFixtures.SWORD_ID, "item")
        IdentifiedEntityRepository.identify(user2, TestFixtures.POTION_ID, "item")

        val user1Identifications = IdentifiedEntityRepository.findByUser(user1)
        val user2Identifications = IdentifiedEntityRepository.findByUser(user2)

        assertEquals(1, user1Identifications.size)
        assertEquals(TestFixtures.SWORD_ID, user1Identifications[0].entityId)

        assertEquals(1, user2Identifications.size)
        assertEquals(TestFixtures.POTION_ID, user2Identifications[0].entityId)
    }

    // ========== getIdentifiedItemIds Tests ==========

    @Test
    fun testGetIdentifiedItemIdsReturnsOnlyItems() {
        val userId = TestFixtures.PLAYER_1_ID

        IdentifiedEntityRepository.identify(userId, TestFixtures.SWORD_ID, "item")
        IdentifiedEntityRepository.identify(userId, TestFixtures.POTION_ID, "item")
        IdentifiedEntityRepository.identify(userId, TestFixtures.GOBLIN_ID, "creature")

        val itemIds = IdentifiedEntityRepository.getIdentifiedItemIds(userId)

        assertEquals(2, itemIds.size)
        assertTrue(itemIds.contains(TestFixtures.SWORD_ID))
        assertTrue(itemIds.contains(TestFixtures.POTION_ID))
        assertFalse(itemIds.contains(TestFixtures.GOBLIN_ID))
    }

    @Test
    fun testGetIdentifiedItemIdsReturnsEmptySetForNewUser() {
        val userId = "user-with-no-items"

        val itemIds = IdentifiedEntityRepository.getIdentifiedItemIds(userId)

        assertTrue(itemIds.isEmpty())
    }

    // ========== getIdentifiedCreatureIds Tests ==========

    @Test
    fun testGetIdentifiedCreatureIdsReturnsOnlyCreatures() {
        val userId = TestFixtures.PLAYER_1_ID

        IdentifiedEntityRepository.identify(userId, TestFixtures.GOBLIN_ID, "creature")
        IdentifiedEntityRepository.identify(userId, TestFixtures.ORC_ID, "creature")
        IdentifiedEntityRepository.identify(userId, TestFixtures.SWORD_ID, "item")

        val creatureIds = IdentifiedEntityRepository.getIdentifiedCreatureIds(userId)

        assertEquals(2, creatureIds.size)
        assertTrue(creatureIds.contains(TestFixtures.GOBLIN_ID))
        assertTrue(creatureIds.contains(TestFixtures.ORC_ID))
        assertFalse(creatureIds.contains(TestFixtures.SWORD_ID))
    }

    @Test
    fun testGetIdentifiedCreatureIdsReturnsEmptySetForNewUser() {
        val userId = "user-with-no-creatures"

        val creatureIds = IdentifiedEntityRepository.getIdentifiedCreatureIds(userId)

        assertTrue(creatureIds.isEmpty())
    }

    // ========== deleteByUser Tests ==========

    @Test
    fun testDeleteByUserRemovesAllIdentifications() {
        val userId = TestFixtures.PLAYER_1_ID

        IdentifiedEntityRepository.identify(userId, TestFixtures.SWORD_ID, "item")
        IdentifiedEntityRepository.identify(userId, TestFixtures.POTION_ID, "item")
        IdentifiedEntityRepository.identify(userId, TestFixtures.GOBLIN_ID, "creature")

        assertEquals(3, IdentifiedEntityRepository.findByUser(userId).size)

        val deletedCount = IdentifiedEntityRepository.deleteByUser(userId)

        assertEquals(3, deletedCount)
        assertTrue(IdentifiedEntityRepository.findByUser(userId).isEmpty())
    }

    @Test
    fun testDeleteByUserDoesNotAffectOtherUsers() {
        val user1 = TestFixtures.PLAYER_1_ID
        val user2 = TestFixtures.PLAYER_2_ID

        IdentifiedEntityRepository.identify(user1, TestFixtures.SWORD_ID, "item")
        IdentifiedEntityRepository.identify(user2, TestFixtures.POTION_ID, "item")

        IdentifiedEntityRepository.deleteByUser(user1)

        // User 1's identifications should be gone
        assertTrue(IdentifiedEntityRepository.findByUser(user1).isEmpty())

        // User 2's identifications should remain
        assertEquals(1, IdentifiedEntityRepository.findByUser(user2).size)
        assertTrue(IdentifiedEntityRepository.isIdentified(user2, TestFixtures.POTION_ID, "item"))
    }

    @Test
    fun testDeleteByUserReturnsZeroForUserWithNoIdentifications() {
        val userId = "user-with-nothing"

        val deletedCount = IdentifiedEntityRepository.deleteByUser(userId)

        assertEquals(0, deletedCount)
    }

    // ========== Data Integrity Tests ==========

    @Test
    fun testIdentifiedEntityHasCorrectTimestamp() {
        val userId = TestFixtures.PLAYER_1_ID
        val itemId = TestFixtures.SWORD_ID

        val beforeTime = System.currentTimeMillis()
        IdentifiedEntityRepository.identify(userId, itemId, "item")
        val afterTime = System.currentTimeMillis()

        val identifications = IdentifiedEntityRepository.findByUser(userId)
        assertEquals(1, identifications.size)

        val identification = identifications[0]
        assertNotNull(identification.identifiedAt)
        // The timestamp should be parseable (ISO-8601 format from Instant.now())
        assertTrue(identification.identifiedAt.isNotEmpty())
    }

    @Test
    fun testIdentifiedEntityHasUniqueId() {
        val userId = TestFixtures.PLAYER_1_ID

        IdentifiedEntityRepository.identify(userId, TestFixtures.SWORD_ID, "item")
        IdentifiedEntityRepository.identify(userId, TestFixtures.POTION_ID, "item")

        val identifications = IdentifiedEntityRepository.findByUser(userId)
        assertEquals(2, identifications.size)

        val id1 = identifications[0].id
        val id2 = identifications[1].id

        assertNotEquals(id1, id2, "Each identification record should have a unique ID")
    }
}
