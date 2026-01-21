package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.ExitDirection
import kotlin.test.*

/**
 * Unit tests for exit direction coordinate mapping logic.
 *
 * These tests verify pure functions that map directions to coordinate offsets.
 * Data integrity checks against live data should be done via the admin dashboard,
 * not in unit tests.
 */
class ExitCoordinateValidationTest {

    // ============================================
    // Direction to Offset Mapping Tests
    // ============================================

    @Test
    fun `NORTH direction maps to Y minus 1`() {
        val offset = getDirectionOffset(ExitDirection.NORTH)
        assertEquals(Pair(0, -1), offset, "NORTH should be (0, -1)")
    }

    @Test
    fun `SOUTH direction maps to Y plus 1`() {
        val offset = getDirectionOffset(ExitDirection.SOUTH)
        assertEquals(Pair(0, 1), offset, "SOUTH should be (0, 1)")
    }

    @Test
    fun `EAST direction maps to X plus 1`() {
        val offset = getDirectionOffset(ExitDirection.EAST)
        assertEquals(Pair(1, 0), offset, "EAST should be (1, 0)")
    }

    @Test
    fun `WEST direction maps to X minus 1`() {
        val offset = getDirectionOffset(ExitDirection.WEST)
        assertEquals(Pair(-1, 0), offset, "WEST should be (-1, 0)")
    }

    @Test
    fun `diagonal directions map to correct offsets`() {
        assertEquals(Pair(1, -1), getDirectionOffset(ExitDirection.NORTHEAST), "NORTHEAST should be (1, -1)")
        assertEquals(Pair(1, 1), getDirectionOffset(ExitDirection.SOUTHEAST), "SOUTHEAST should be (1, 1)")
        assertEquals(Pair(-1, 1), getDirectionOffset(ExitDirection.SOUTHWEST), "SOUTHWEST should be (-1, 1)")
        assertEquals(Pair(-1, -1), getDirectionOffset(ExitDirection.NORTHWEST), "NORTHWEST should be (-1, -1)")
    }

    @Test
    fun `UNKNOWN direction maps to zero offset`() {
        val offset = getDirectionOffset(ExitDirection.UNKNOWN)
        assertEquals(Pair(0, 0), offset, "UNKNOWN should be (0, 0)")
    }

    @Test
    fun `opposite directions have inverse offsets`() {
        val pairs = listOf(
            ExitDirection.NORTH to ExitDirection.SOUTH,
            ExitDirection.EAST to ExitDirection.WEST,
            ExitDirection.NORTHEAST to ExitDirection.SOUTHWEST,
            ExitDirection.NORTHWEST to ExitDirection.SOUTHEAST
        )

        for ((dir1, dir2) in pairs) {
            val offset1 = getDirectionOffset(dir1)
            val offset2 = getDirectionOffset(dir2)
            assertEquals(-offset1.first, offset2.first, "$dir1 and $dir2 should have inverse X offsets")
            assertEquals(-offset1.second, offset2.second, "$dir1 and $dir2 should have inverse Y offsets")
        }
    }

    @Test
    fun `getDirectionFromOffset returns correct direction for all valid offsets`() {
        // Test all 8 cardinal/ordinal directions
        assertEquals(ExitDirection.NORTH, getDirectionFromOffset(0, -1))
        assertEquals(ExitDirection.NORTHEAST, getDirectionFromOffset(1, -1))
        assertEquals(ExitDirection.EAST, getDirectionFromOffset(1, 0))
        assertEquals(ExitDirection.SOUTHEAST, getDirectionFromOffset(1, 1))
        assertEquals(ExitDirection.SOUTH, getDirectionFromOffset(0, 1))
        assertEquals(ExitDirection.SOUTHWEST, getDirectionFromOffset(-1, 1))
        assertEquals(ExitDirection.WEST, getDirectionFromOffset(-1, 0))
        assertEquals(ExitDirection.NORTHWEST, getDirectionFromOffset(-1, -1))
    }

    @Test
    fun `getDirectionFromOffset returns UNKNOWN for invalid offsets`() {
        // Invalid offsets (more than 1 tile away or zero)
        assertEquals(ExitDirection.UNKNOWN, getDirectionFromOffset(0, 0))
        assertEquals(ExitDirection.UNKNOWN, getDirectionFromOffset(2, 0))
        assertEquals(ExitDirection.UNKNOWN, getDirectionFromOffset(0, -2))
        assertEquals(ExitDirection.UNKNOWN, getDirectionFromOffset(2, 2))
        assertEquals(ExitDirection.UNKNOWN, getDirectionFromOffset(-3, 1))
    }

    @Test
    fun `direction offset roundtrip is consistent`() {
        // For all valid directions, getting offset then direction should return original
        val directions = listOf(
            ExitDirection.NORTH,
            ExitDirection.NORTHEAST,
            ExitDirection.EAST,
            ExitDirection.SOUTHEAST,
            ExitDirection.SOUTH,
            ExitDirection.SOUTHWEST,
            ExitDirection.WEST,
            ExitDirection.NORTHWEST
        )

        for (dir in directions) {
            val (dx, dy) = getDirectionOffset(dir)
            val roundtrip = getDirectionFromOffset(dx, dy)
            assertEquals(dir, roundtrip, "Roundtrip should preserve direction for $dir")
        }
    }

    @Test
    fun `getOppositeDirection returns correct opposites`() {
        assertEquals(ExitDirection.SOUTH, getOppositeDirection(ExitDirection.NORTH))
        assertEquals(ExitDirection.NORTH, getOppositeDirection(ExitDirection.SOUTH))
        assertEquals(ExitDirection.WEST, getOppositeDirection(ExitDirection.EAST))
        assertEquals(ExitDirection.EAST, getOppositeDirection(ExitDirection.WEST))
        assertEquals(ExitDirection.SOUTHWEST, getOppositeDirection(ExitDirection.NORTHEAST))
        assertEquals(ExitDirection.NORTHEAST, getOppositeDirection(ExitDirection.SOUTHWEST))
        assertEquals(ExitDirection.SOUTHEAST, getOppositeDirection(ExitDirection.NORTHWEST))
        assertEquals(ExitDirection.NORTHWEST, getOppositeDirection(ExitDirection.SOUTHEAST))
        assertEquals(ExitDirection.UNKNOWN, getOppositeDirection(ExitDirection.UNKNOWN))
    }

    // ============================================
    // Helper Functions (these mirror the server's logic)
    // ============================================

    /**
     * Get the coordinate offset for a direction.
     * This should match the server's getDirectionOffset function.
     */
    private fun getDirectionOffset(direction: ExitDirection): Pair<Int, Int> = when (direction) {
        ExitDirection.NORTH -> Pair(0, -1)
        ExitDirection.NORTHEAST -> Pair(1, -1)
        ExitDirection.EAST -> Pair(1, 0)
        ExitDirection.SOUTHEAST -> Pair(1, 1)
        ExitDirection.SOUTH -> Pair(0, 1)
        ExitDirection.SOUTHWEST -> Pair(-1, 1)
        ExitDirection.WEST -> Pair(-1, 0)
        ExitDirection.NORTHWEST -> Pair(-1, -1)
        ExitDirection.UNKNOWN -> Pair(0, 0)
    }

    /**
     * Get direction from coordinate offset.
     */
    private fun getDirectionFromOffset(dx: Int, dy: Int): ExitDirection = when {
        dx == 0 && dy == -1 -> ExitDirection.NORTH
        dx == 1 && dy == -1 -> ExitDirection.NORTHEAST
        dx == 1 && dy == 0 -> ExitDirection.EAST
        dx == 1 && dy == 1 -> ExitDirection.SOUTHEAST
        dx == 0 && dy == 1 -> ExitDirection.SOUTH
        dx == -1 && dy == 1 -> ExitDirection.SOUTHWEST
        dx == -1 && dy == 0 -> ExitDirection.WEST
        dx == -1 && dy == -1 -> ExitDirection.NORTHWEST
        else -> ExitDirection.UNKNOWN
    }

    /**
     * Get the opposite direction.
     */
    private fun getOppositeDirection(direction: ExitDirection): ExitDirection = when (direction) {
        ExitDirection.NORTH -> ExitDirection.SOUTH
        ExitDirection.SOUTH -> ExitDirection.NORTH
        ExitDirection.EAST -> ExitDirection.WEST
        ExitDirection.WEST -> ExitDirection.EAST
        ExitDirection.NORTHEAST -> ExitDirection.SOUTHWEST
        ExitDirection.SOUTHWEST -> ExitDirection.NORTHEAST
        ExitDirection.NORTHWEST -> ExitDirection.SOUTHEAST
        ExitDirection.SOUTHEAST -> ExitDirection.NORTHWEST
        ExitDirection.UNKNOWN -> ExitDirection.UNKNOWN
    }
}
