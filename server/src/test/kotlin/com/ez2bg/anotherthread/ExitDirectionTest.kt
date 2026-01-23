package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import java.io.File
import kotlin.test.*

/**
 * Tests for ExitDirection enum handling and related functionality.
 * Ensures all directions including ENTER are properly handled throughout the codebase.
 */
class ExitDirectionTest {

    companion object {
        private var initialized = false
        private val testDbFile = File.createTempFile("exit_direction_test_db_${System.nanoTime()}", ".db").also { it.deleteOnExit() }
    }

    @BeforeTest
    fun setup() {
        if (!initialized) {
            DatabaseConfig.init(testDbFile.absolutePath)
            initialized = true
        }
        DatabaseConfig.clearAllTables()
    }

    // ========== Exit Direction Serialization Tests ==========

    @Test
    fun testAllDirectionsSerializeAndDeserialize() {
        // Test that all directions can be saved and loaded from database
        ExitDirection.entries.forEach { direction ->
            val location = Location(
                id = "test-${direction.name}",
                name = "Test ${direction.name}",
                desc = "Testing ${direction.name} direction",
                itemIds = emptyList(),
                creatureIds = emptyList(),
                exits = listOf(Exit("target-location", direction)),
                featureIds = emptyList()
            )

            LocationRepository.create(location)
            val found = LocationRepository.findById(location.id)

            assertNotNull(found, "Location with ${direction.name} exit should be found")
            assertEquals(1, found.exits.size, "Should have exactly one exit")
            assertEquals(direction, found.exits[0].direction, "Direction should be ${direction.name}")
        }
    }

    @Test
    fun testEnterDirectionInExits() {
        val location = Location(
            name = "Forest",
            desc = "A dense forest with a hidden entrance",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = listOf(
                Exit("dungeon-entrance", ExitDirection.ENTER),
                Exit("north-path", ExitDirection.NORTH),
                Exit("south-path", ExitDirection.SOUTH)
            ),
            featureIds = emptyList()
        )

        LocationRepository.create(location)
        val found = LocationRepository.findById(location.id)

        assertNotNull(found)
        assertEquals(3, found.exits.size)

        val enterExit = found.exits.find { it.direction == ExitDirection.ENTER }
        assertNotNull(enterExit, "Should have an ENTER exit")
        assertEquals("dungeon-entrance", enterExit.locationId)
    }

    @Test
    fun testMixedDirectionsInSingleLocation() {
        val location = Location(
            name = "Hub Room",
            desc = "A room with many exits",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = listOf(
                Exit("north-room", ExitDirection.NORTH),
                Exit("east-room", ExitDirection.EAST),
                Exit("portal-destination", ExitDirection.ENTER),
                Exit("unknown-path", ExitDirection.UNKNOWN)
            ),
            featureIds = emptyList()
        )

        LocationRepository.create(location)
        val found = LocationRepository.findById(location.id)

        assertNotNull(found)
        assertEquals(4, found.exits.size)
        assertTrue(found.exits.any { it.direction == ExitDirection.NORTH })
        assertTrue(found.exits.any { it.direction == ExitDirection.EAST })
        assertTrue(found.exits.any { it.direction == ExitDirection.ENTER })
        assertTrue(found.exits.any { it.direction == ExitDirection.UNKNOWN })
    }

    // ========== getOppositeDirection Tests ==========

    @Test
    fun testOppositeDirectionForCardinals() {
        assertEquals(ExitDirection.SOUTH, getOppositeDirection(ExitDirection.NORTH))
        assertEquals(ExitDirection.NORTH, getOppositeDirection(ExitDirection.SOUTH))
        assertEquals(ExitDirection.WEST, getOppositeDirection(ExitDirection.EAST))
        assertEquals(ExitDirection.EAST, getOppositeDirection(ExitDirection.WEST))
    }

    @Test
    fun testOppositeDirectionForDiagonals() {
        assertEquals(ExitDirection.SOUTHWEST, getOppositeDirection(ExitDirection.NORTHEAST))
        assertEquals(ExitDirection.NORTHEAST, getOppositeDirection(ExitDirection.SOUTHWEST))
        assertEquals(ExitDirection.NORTHWEST, getOppositeDirection(ExitDirection.SOUTHEAST))
        assertEquals(ExitDirection.SOUTHEAST, getOppositeDirection(ExitDirection.NORTHWEST))
    }

    @Test
    fun testOppositeDirectionForEnter() {
        // ENTER should map to ENTER (portal works both ways conceptually)
        assertEquals(ExitDirection.ENTER, getOppositeDirection(ExitDirection.ENTER))
    }

    @Test
    fun testOppositeDirectionForUnknown() {
        assertEquals(ExitDirection.UNKNOWN, getOppositeDirection(ExitDirection.UNKNOWN))
    }

    // ========== getDirectionOffset Tests ==========

    @Test
    fun testDirectionOffsetForCardinals() {
        assertEquals(Pair(0, -1), getDirectionOffset(ExitDirection.NORTH))
        assertEquals(Pair(0, 1), getDirectionOffset(ExitDirection.SOUTH))
        assertEquals(Pair(1, 0), getDirectionOffset(ExitDirection.EAST))
        assertEquals(Pair(-1, 0), getDirectionOffset(ExitDirection.WEST))
    }

    @Test
    fun testDirectionOffsetForDiagonals() {
        assertEquals(Pair(1, -1), getDirectionOffset(ExitDirection.NORTHEAST))
        assertEquals(Pair(-1, -1), getDirectionOffset(ExitDirection.NORTHWEST))
        assertEquals(Pair(1, 1), getDirectionOffset(ExitDirection.SOUTHEAST))
        assertEquals(Pair(-1, 1), getDirectionOffset(ExitDirection.SOUTHWEST))
    }

    @Test
    fun testDirectionOffsetForEnter() {
        // ENTER has no grid offset (it's a portal, not a directional move)
        assertEquals(Pair(0, 0), getDirectionOffset(ExitDirection.ENTER))
    }

    @Test
    fun testDirectionOffsetForUnknown() {
        assertEquals(Pair(0, 0), getDirectionOffset(ExitDirection.UNKNOWN))
    }

    // ========== Creature Movement Filtering Tests ==========

    @Test
    fun testFilteringEnterExitsForCreatureMovement() {
        // Simulate the filtering done in CombatService for creature wandering
        val exits = listOf(
            Exit("north-room", ExitDirection.NORTH),
            Exit("dungeon", ExitDirection.ENTER),
            Exit("south-room", ExitDirection.SOUTH),
            Exit("another-dungeon", ExitDirection.ENTER)
        )

        val filteredExits = exits.filter { it.direction != ExitDirection.ENTER }

        assertEquals(2, filteredExits.size)
        assertTrue(filteredExits.all { it.direction != ExitDirection.ENTER })
        assertTrue(filteredExits.any { it.direction == ExitDirection.NORTH })
        assertTrue(filteredExits.any { it.direction == ExitDirection.SOUTH })
    }

    @Test
    fun testFilteringLeavesEmptyWhenOnlyEnterExits() {
        val exits = listOf(
            Exit("dungeon-1", ExitDirection.ENTER),
            Exit("dungeon-2", ExitDirection.ENTER)
        )

        val filteredExits = exits.filter { it.direction != ExitDirection.ENTER }

        assertTrue(filteredExits.isEmpty())
    }

    @Test
    fun testFilteringPreservesAllNonEnterExits() {
        val allNonEnterDirections = ExitDirection.entries.filter {
            it != ExitDirection.ENTER && it != ExitDirection.UNKNOWN
        }

        val exits = allNonEnterDirections.mapIndexed { index, direction ->
            Exit("room-$index", direction)
        }

        val filteredExits = exits.filter { it.direction != ExitDirection.ENTER }

        assertEquals(exits.size, filteredExits.size)
    }

    // ========== Location Update Tests ==========

    @Test
    fun testUpdateLocationPreservesEnterExits() {
        val location = Location(
            name = "Forest",
            desc = "Original description",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = listOf(
                Exit("dungeon", ExitDirection.ENTER),
                Exit("north-path", ExitDirection.NORTH)
            ),
            featureIds = emptyList()
        )

        LocationRepository.create(location)

        // Update the location
        val updated = location.copy(
            desc = "Updated description",
            exits = listOf(
                Exit("dungeon", ExitDirection.ENTER),
                Exit("north-path", ExitDirection.NORTH),
                Exit("east-path", ExitDirection.EAST)
            )
        )
        LocationRepository.update(updated)

        val found = LocationRepository.findById(location.id)
        assertNotNull(found)
        assertEquals("Updated description", found.desc)
        assertEquals(3, found.exits.size)

        val enterExit = found.exits.find { it.direction == ExitDirection.ENTER }
        assertNotNull(enterExit, "ENTER exit should be preserved after update")
    }

    // ========== Edge Cases ==========

    @Test
    fun testEmptyExitsList() {
        val location = Location(
            name = "Dead End",
            desc = "No way out",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = emptyList(),
            featureIds = emptyList()
        )

        LocationRepository.create(location)
        val found = LocationRepository.findById(location.id)

        assertNotNull(found)
        assertTrue(found.exits.isEmpty())
    }

    @Test
    fun testMultipleEnterExitsToSameDestination() {
        // Edge case: multiple ENTER exits (shouldn't normally happen but should be handled)
        val location = Location(
            name = "Multi-portal Room",
            desc = "A room with multiple portals",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = listOf(
                Exit("dungeon-1", ExitDirection.ENTER),
                Exit("dungeon-1", ExitDirection.ENTER) // Duplicate
            ),
            featureIds = emptyList()
        )

        LocationRepository.create(location)
        val found = LocationRepository.findById(location.id)

        assertNotNull(found)
        assertEquals(2, found.exits.size) // Both should be preserved
    }
}
