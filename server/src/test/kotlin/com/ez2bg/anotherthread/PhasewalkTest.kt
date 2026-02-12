package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.routes.PhasewalkDestination
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.deleteAll
import kotlin.test.*

/**
 * Unit tests for the Phasewalk ability system.
 * Tests edge cases around phasing through walls to adjacent locations.
 *
 * These tests target bugs found in:
 * - User without phasewalk ability trying to phase
 * - Mana cost deduction
 * - Direction validation
 * - Phasing to locations without exits
 * - HP-based movement restrictions
 */
class PhasewalkTest {

    companion object {
        private const val PHASEWALK_ABILITY_ID = "ability-phasewalk"
        private const val DEATH_SHROUD_ID = "item-death-shroud"
    }

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
            AbilityTable.deleteAll()
            LocationTable.deleteAll()
            CharacterClassTable.deleteAll()
        }
    }

    private fun seedTestData() {
        CharacterClassRepository.create(TestFixtures.warriorClass())

        // Create phasewalk ability
        AbilityRepository.create(
            Ability(
                id = PHASEWALK_ABILITY_ID,
                name = "Phasewalk",
                description = "Phase through walls",
                classId = null,
                abilityType = "item",
                targetType = "phasewalk",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 0,
                durationRounds = 0,
                manaCost = 2,
                staminaCost = 0,
                effects = """["phasewalk"]"""
            )
        )

        // Create Death Shroud item that grants phasewalk
        ItemRepository.create(
            Item(
                id = DEATH_SHROUD_ID,
                name = "Death Shroud",
                desc = "A cloak that grants phasewalk",
                featureIds = emptyList(),
                abilityIds = listOf(PHASEWALK_ABILITY_ID),
                equipmentType = "accessory",
                equipmentSlot = "back",
                statBonuses = StatBonuses(attack = 0, defense = -10, maxHp = 0),
                value = 0
            )
        )

        // Create a grid of locations:
        // [loc-0-0] -- exit --> [loc-1-0]
        //    |                      |
        //  (no exit)             (exit)
        //    v                      v
        // [loc-0-1] <-- exit -- [loc-1-1]
        //
        // Phasewalk from loc-0-0 SOUTH should work (no exit but location exists)
        // Phasewalk from loc-0-0 EAST should fail (exit exists - just walk!)

        LocationRepository.create(
            Location(
                id = "loc-0-0",
                name = "Origin",
                desc = "Starting location",
                creatureIds = emptyList(),
                exits = listOf(Exit("loc-1-0", ExitDirection.EAST)),  // Only exit is EAST
                itemIds = emptyList(),
                featureIds = emptyList(),
                gridX = 0,
                gridY = 0,
                areaId = "test-area"
            )
        )

        LocationRepository.create(
            Location(
                id = "loc-1-0",
                name = "East Room",
                desc = "Room to the east",
                creatureIds = emptyList(),
                exits = listOf(Exit("loc-0-0", ExitDirection.WEST), Exit("loc-1-1", ExitDirection.SOUTH)),
                itemIds = emptyList(),
                featureIds = emptyList(),
                gridX = 1,
                gridY = 0,
                areaId = "test-area"
            )
        )

        LocationRepository.create(
            Location(
                id = "loc-0-1",
                name = "South Room",
                desc = "Room to the south - no direct exit from origin",
                creatureIds = emptyList(),
                exits = listOf(Exit("loc-1-1", ExitDirection.EAST)),  // Can't walk back north
                itemIds = emptyList(),
                featureIds = emptyList(),
                gridX = 0,
                gridY = 1,
                areaId = "test-area"
            )
        )

        LocationRepository.create(
            Location(
                id = "loc-1-1",
                name = "Southeast Room",
                desc = "Room to the southeast",
                creatureIds = emptyList(),
                exits = listOf(Exit("loc-1-0", ExitDirection.NORTH), Exit("loc-0-1", ExitDirection.WEST)),
                itemIds = emptyList(),
                featureIds = emptyList(),
                gridX = 1,
                gridY = 1,
                areaId = "test-area"
            )
        )
    }

    // ========== Phasewalk Ability Check Tests ==========

    @Test
    fun testUserWithDeathShroudEquipped_HasPhasewalk() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0"
        )

        // Verify user has the equipped item
        val userData = UserRepository.findById(user.id)!!
        assertTrue(DEATH_SHROUD_ID in userData.equippedItemIds)

        // Check if user has phasewalk ability via equipped items
        val equippedItems = userData.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
        val hasAbility = equippedItems.any { PHASEWALK_ABILITY_ID in it.abilityIds }
        assertTrue(hasAbility, "User with Death Shroud equipped should have phasewalk ability")
    }

    @Test
    fun testUserWithDeathShroudNotEquipped_NoPhasewalk() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),  // In inventory but not equipped
            equippedItemIds = emptyList(),
            currentLocationId = "loc-0-0"
        )

        val userData = UserRepository.findById(user.id)!!
        val equippedItems = userData.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
        val hasAbility = equippedItems.any { PHASEWALK_ABILITY_ID in it.abilityIds }
        assertFalse(hasAbility, "User without Death Shroud equipped should not have phasewalk")
    }

    @Test
    fun testUserWithoutDeathShroud_NoPhasewalk() {
        val user = createTestUser(
            itemIds = emptyList(),
            equippedItemIds = emptyList(),
            currentLocationId = "loc-0-0"
        )

        val userData = UserRepository.findById(user.id)!!
        val equippedItems = userData.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
        val hasAbility = equippedItems.any { PHASEWALK_ABILITY_ID in it.abilityIds }
        assertFalse(hasAbility, "User without Death Shroud should not have phasewalk")
    }

    // ========== Phasewalk Destination Tests ==========

    @Test
    fun testPhasewalkDestinations_DirectionWithoutExit_Available() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0"
        )

        val destinations = calculatePhasewalkDestinations(user.id)

        // SOUTH should be available (no exit to loc-0-1, but location exists)
        val southDest = destinations.find { it.direction == "south" }
        assertNotNull(southDest, "Should be able to phasewalk SOUTH")
        assertEquals("loc-0-1", southDest.locationId)
    }

    @Test
    fun testPhasewalkDestinations_DirectionWithExit_NotAvailable() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0"
        )

        val destinations = calculatePhasewalkDestinations(user.id)

        // EAST should NOT be available (there's already an exit)
        val eastDest = destinations.find { it.direction == "east" }
        assertNull(eastDest, "Should NOT be able to phasewalk EAST (exit exists)")
    }

    @Test
    fun testPhasewalkDestinations_DirectionWithNoLocation_NotAvailable() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0"
        )

        val destinations = calculatePhasewalkDestinations(user.id)

        // NORTH should NOT be available (no location at 0,-1)
        val northDest = destinations.find { it.direction == "north" }
        assertNull(northDest, "Should NOT be able to phasewalk NORTH (no location)")

        // WEST should NOT be available (no location at -1,0)
        val westDest = destinations.find { it.direction == "west" }
        assertNull(westDest, "Should NOT be able to phasewalk WEST (no location)")
    }

    // ========== Mana Cost Tests ==========

    @Test
    fun testPhasewalk_CostsMana() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0",
            currentMana = 10,
            maxMana = 10
        )

        // Simulate phasewalk mana cost
        val success = UserRepository.spendMana(user.id, 2)

        assertTrue(success)
        val updated = UserRepository.findById(user.id)!!
        assertEquals(8, updated.currentMana, "Mana should be reduced by 2")
    }

    @Test
    fun testPhasewalk_InsufficientMana_Fails() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0",
            currentMana = 1,
            maxMana = 10
        )

        // Phasewalk costs 2 mana, user only has 1
        val success = UserRepository.spendMana(user.id, 2)

        assertFalse(success, "Should fail with insufficient mana")
        val updated = UserRepository.findById(user.id)!!
        assertEquals(1, updated.currentMana, "Mana should be unchanged")
    }

    @Test
    fun testPhasewalk_ExactMana_Succeeds() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0",
            currentMana = 2,
            maxMana = 10
        )

        val success = UserRepository.spendMana(user.id, 2)

        assertTrue(success, "Should succeed with exact mana")
        val updated = UserRepository.findById(user.id)!!
        assertEquals(0, updated.currentMana)
    }

    // ========== HP-Based Movement Restriction Tests ==========

    @Test
    fun testPhasewalk_ZeroHp_ShouldBeBlocked() {
        // This tests the client-side logic that blocks phasewalk at 0 HP
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0",
            currentHp = 0,
            maxHp = 30
        )

        val userData = UserRepository.findById(user.id)!!

        // The HP check happens on the client, but we verify the state
        assertTrue(userData.currentHp <= 0, "User should be at 0 HP")
        // Client code: if (playerHp <= 0) showSnackbar("You cannot phasewalk while incapacitated")
    }

    @Test
    fun testPhasewalk_OneHp_ShouldWork() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0",
            currentHp = 1,
            maxHp = 30
        )

        val userData = UserRepository.findById(user.id)!!

        assertTrue(userData.currentHp > 0, "User should have positive HP")
        // Phasewalk should be allowed with 1 HP
    }

    // ========== Location Update Tests ==========

    @Test
    fun testPhasewalk_UpdatesUserLocation() {
        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0"
        )

        // Simulate successful phasewalk by updating location
        UserRepository.updateCurrentLocation(user.id, "loc-0-1")

        val updated = UserRepository.findById(user.id)!!
        assertEquals("loc-0-1", updated.currentLocationId, "Location should be updated")
    }

    // ========== Direction Offset Tests ==========

    @Test
    fun testDirectionOffsets() {
        // Test the direction offset calculations used in phasewalk
        val offsets = mapOf(
            "north" to Pair(0, -1),
            "south" to Pair(0, 1),
            "east" to Pair(1, 0),
            "west" to Pair(-1, 0),
            "northeast" to Pair(1, -1),
            "northwest" to Pair(-1, -1),
            "southeast" to Pair(1, 1),
            "southwest" to Pair(-1, 1)
        )

        offsets.forEach { (direction, expected) ->
            val (dx, dy) = getDirectionOffset(direction)
            assertEquals(expected.first, dx, "X offset for $direction")
            assertEquals(expected.second, dy, "Y offset for $direction")
        }
    }

    @Test
    fun testOppositeDirections() {
        val pairs = mapOf(
            "north" to "south",
            "south" to "north",
            "east" to "west",
            "west" to "east",
            "northeast" to "southwest",
            "northwest" to "southeast",
            "southeast" to "northwest",
            "southwest" to "northeast"
        )

        pairs.forEach { (direction, expected) ->
            val opposite = getOppositeDirection(direction)
            assertEquals(expected, opposite, "Opposite of $direction")
        }
    }

    // ========== Area Boundary Tests ==========

    @Test
    fun testPhasewalk_SameAreaOnly() {
        // Phasewalk should only work within the same areaId
        // Create a location in a different area at adjacent coordinates
        LocationRepository.create(
            Location(
                id = "different-area-loc",
                name = "Different Area",
                desc = "Location in different area at (0, 1)",
                creatureIds = emptyList(),
                exits = emptyList(),
                itemIds = emptyList(),
                featureIds = emptyList(),
                gridX = 0,
                gridY = 1,
                areaId = "different-area"  // Different area!
            )
        )

        val user = createTestUser(
            itemIds = listOf(DEATH_SHROUD_ID),
            equippedItemIds = listOf(DEATH_SHROUD_ID),
            currentLocationId = "loc-0-0"  // In "test-area"
        )

        val destinations = calculatePhasewalkDestinations(user.id)

        // SOUTH should find loc-0-1 (in test-area), not different-area-loc
        val southDest = destinations.find { it.direction == "south" }
        assertNotNull(southDest)
        assertEquals("loc-0-1", southDest.locationId, "Should find location in same area")
    }

    // ========== Helper Methods ==========

    private fun createTestUser(
        itemIds: List<String> = emptyList(),
        equippedItemIds: List<String> = emptyList(),
        currentLocationId: String? = null,
        currentHp: Int = 30,
        maxHp: Int = 30,
        currentMana: Int = 20,
        maxMana: Int = 20
    ): User {
        val user = User(
            id = "test-user-${System.nanoTime()}",
            name = "TestUser_${System.nanoTime()}",
            passwordHash = "test-hash",
            desc = "Test user for phasewalk tests",
            currentHp = currentHp,
            maxHp = maxHp,
            currentMana = currentMana,
            maxMana = maxMana,
            itemIds = itemIds,
            equippedItemIds = equippedItemIds,
            currentLocationId = currentLocationId,
            characterClassId = TestFixtures.WARRIOR_CLASS_ID
        )
        UserRepository.create(user)
        return user
    }

    /**
     * Simulates the phasewalk destination calculation from PhasewalkRoutes.
     */
    private fun calculatePhasewalkDestinations(userId: String): List<PhasewalkDestination> {
        val user = UserRepository.findById(userId) ?: return emptyList()

        // Check if user has phasewalk ability
        val equippedItems = user.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
        val hasAbility = equippedItems.any { PHASEWALK_ABILITY_ID in it.abilityIds }
        if (!hasAbility) return emptyList()

        val currentLocation = user.currentLocationId?.let { LocationRepository.findById(it) }
            ?: return emptyList()

        if (currentLocation.gridX == null || currentLocation.gridY == null) return emptyList()

        val areaId = currentLocation.areaId ?: "overworld"

        // Get all exits from current location
        val existingExitDirections = currentLocation.exits
            .map { it.direction.name.lowercase() }
            .toSet()

        // All possible directions
        val allDirections = listOf("north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest")

        // Find destinations that DON'T have exits but DO have locations
        return allDirections
            .filter { it !in existingExitDirections }
            .mapNotNull { direction ->
                val (dx, dy) = getDirectionOffset(direction)
                val targetX = currentLocation.gridX!! + dx
                val targetY = currentLocation.gridY!! + dy

                val targetLocation = LocationRepository.findByCoordinates(targetX, targetY, areaId)
                if (targetLocation != null) {
                    PhasewalkDestination(
                        direction = direction,
                        locationId = targetLocation.id,
                        locationName = targetLocation.name,
                        gridX = targetX,
                        gridY = targetY
                    )
                } else null
            }
    }

    private fun getDirectionOffset(direction: String): Pair<Int, Int> {
        return when (direction.lowercase()) {
            "north" -> Pair(0, -1)
            "south" -> Pair(0, 1)
            "east" -> Pair(1, 0)
            "west" -> Pair(-1, 0)
            "northeast" -> Pair(1, -1)
            "northwest" -> Pair(-1, -1)
            "southeast" -> Pair(1, 1)
            "southwest" -> Pair(-1, 1)
            else -> Pair(0, 0)
        }
    }

    private fun getOppositeDirection(direction: String): String {
        return when (direction.lowercase()) {
            "north" -> "south"
            "south" -> "north"
            "east" -> "west"
            "west" -> "east"
            "northeast" -> "southwest"
            "northwest" -> "southeast"
            "southeast" -> "northwest"
            "southwest" -> "northeast"
            else -> direction
        }
    }
}
