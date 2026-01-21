package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.ui.ConnectionLineCalculator
import com.ez2bg.anotherthread.ui.Point
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionLineCalculatorTest {

    private val dotRadius = 5f  // Standard dot radius (5dp)

    // ============================================
    // Rule 1: Lines never draw within dots
    // ============================================

    @Test
    fun `line endpoints are at dot edges not centers`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(100f, 0f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)

        // From endpoint should be at edge of from dot (5 units from center)
        val fromDistance = line.from.distanceTo(fromCenter)
        assertEquals(dotRadius, fromDistance, 0.001f, "From endpoint should be at dot edge")

        // To endpoint should be at edge of to dot (5 units from center)
        val toDistance = line.to.distanceTo(toCenter)
        assertEquals(dotRadius, toDistance, 0.001f, "To endpoint should be at dot edge")
    }

    @Test
    fun `line does not intersect source dot`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(100f, 0f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)

        // Line from point should be exactly at the edge, not inside
        val fromDistance = line.from.distanceTo(fromCenter)
        assertTrue(fromDistance >= dotRadius - 0.001f, "Line should not start inside from dot")
    }

    @Test
    fun `line does not intersect target dot`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(100f, 0f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)

        // Line to point should be exactly at the edge, not inside
        val toDistance = line.to.distanceTo(toCenter)
        assertTrue(toDistance >= dotRadius - 0.001f, "Line should not end inside to dot")
    }

    @Test
    fun `line does not pass through dots - verification helper`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(100f, 0f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)
        assertTrue(
            ConnectionLineCalculator.verifyLineDoesNotIntersectDots(
                line, fromCenter, toCenter, dotRadius
            ),
            "Line should not intersect any dot"
        )
    }

    // ============================================
    // Rule 2: Lines align with dot centers
    // ============================================

    @Test
    fun `horizontal line endpoints align with centers`() {
        val fromCenter = Point(0f, 50f)
        val toCenter = Point(100f, 50f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)

        // Both endpoints should have same Y as centers (horizontal line)
        assertEquals(50f, line.from.y, 0.001f, "From Y should match center Y")
        assertEquals(50f, line.to.y, 0.001f, "To Y should match center Y")

        // Endpoints should be offset by exactly dotRadius in X direction
        assertEquals(dotRadius, line.from.x, 0.001f, "From X should be at edge")
        assertEquals(100f - dotRadius, line.to.x, 0.001f, "To X should be at edge")
    }

    @Test
    fun `vertical line endpoints align with centers`() {
        val fromCenter = Point(50f, 0f)
        val toCenter = Point(50f, 100f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)

        // Both endpoints should have same X as centers (vertical line)
        assertEquals(50f, line.from.x, 0.001f, "From X should match center X")
        assertEquals(50f, line.to.x, 0.001f, "To X should match center X")

        // Endpoints should be offset by exactly dotRadius in Y direction
        assertEquals(dotRadius, line.from.y, 0.001f, "From Y should be at edge")
        assertEquals(100f - dotRadius, line.to.y, 0.001f, "To Y should be at edge")
    }

    @Test
    fun `diagonal line endpoints align with centers`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(100f, 100f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)
        assertTrue(
            ConnectionLineCalculator.verifyEndpointsAlignedWithCenters(
                line, fromCenter, toCenter
            ),
            "Diagonal line endpoints should be collinear with centers"
        )
    }

    @Test
    fun `endpoints are collinear with both centers`() {
        // Test multiple angles
        val angles = listOf(0f, 30f, 45f, 60f, 90f, 135f, 180f, 225f, 270f, 315f)

        for (angleDeg in angles) {
            val angleRad = angleDeg * kotlin.math.PI.toFloat() / 180f
            val distance = 100f
            val fromCenter = Point(0f, 0f)
            val toCenter = Point(
                distance * kotlin.math.cos(angleRad),
                distance * kotlin.math.sin(angleRad)
            )

            val line = ConnectionLineCalculator.calculateConnectionLine(
                fromCenter, toCenter, dotRadius, seed = 123L
            )

            assertNotNull(line, "Line should be created for angle $angleDeg")
            assertTrue(
                ConnectionLineCalculator.verifyEndpointsAlignedWithCenters(
                    line, fromCenter, toCenter, tolerance = 0.01f
                ),
                "Endpoints should be collinear with centers at angle $angleDeg"
            )
        }
    }

    // ============================================
    // Rule 3: No duplicate connections
    // ============================================

    @Test
    fun `canonical key is same regardless of order`() {
        val keyAB = ConnectionLineCalculator.canonicalConnectionKey("location-a", "location-b")
        val keyBA = ConnectionLineCalculator.canonicalConnectionKey("location-b", "location-a")

        assertEquals(keyAB, keyBA, "A->B and B->A should produce same canonical key")
    }

    @Test
    fun `canonical key orders IDs consistently`() {
        val key = ConnectionLineCalculator.canonicalConnectionKey("zebra", "apple")

        assertEquals("apple", key.first, "Smaller ID should be first")
        assertEquals("zebra", key.second, "Larger ID should be second")
    }

    @Test
    fun `canonical key handles UUIDs`() {
        val id1 = "9fa3e34d-5eec-46ed-a479-b9ffd6c34163"
        val id2 = "a1b2c3d4-5678-90ab-cdef-1234567890ab"

        val key1 = ConnectionLineCalculator.canonicalConnectionKey(id1, id2)
        val key2 = ConnectionLineCalculator.canonicalConnectionKey(id2, id1)

        assertEquals(key1, key2, "UUID keys should be consistent regardless of order")
    }

    // ============================================
    // Rule 4: No lines for overlapping dots
    // ============================================

    @Test
    fun `no line when dots overlap completely`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(0f, 0f)  // Same position

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNull(line, "Should not create line when dots are at same position")
    }

    @Test
    fun `no line when dots touch exactly`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(dotRadius * 2, 0f)  // Dots touch exactly

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNull(line, "Should not create line when dots touch exactly")
    }

    @Test
    fun `no line when dots overlap partially`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(dotRadius, 0f)  // Centers closer than 2*radius

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNull(line, "Should not create line when dots overlap")
    }

    @Test
    fun `line created when dots are just apart`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(dotRadius * 2 + 1f, 0f)  // Just barely apart

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line, "Should create line when dots are just apart")
    }

    // ============================================
    // Edge cases and additional validation
    // ============================================

    @Test
    fun `line length is distance minus both radii`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(100f, 0f)
        val expectedLineLength = 100f - (2 * dotRadius)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)
        val actualLineLength = line.from.distanceTo(line.to)
        assertEquals(expectedLineLength, actualLineLength, 0.001f, "Line length should be center distance minus both radii")
    }

    @Test
    fun `line preserves seed value`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(100f, 0f)
        val expectedSeed = 42L

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = expectedSeed
        )

        assertNotNull(line)
        assertEquals(expectedSeed, line.seed, "Seed should be preserved in connection line")
    }

    @Test
    fun `endpoints verified at dot edges helper works`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(100f, 0f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)
        assertTrue(
            ConnectionLineCalculator.verifyEndpointsAtDotEdges(
                line, fromCenter, toCenter, dotRadius
            ),
            "Verification helper should confirm endpoints at edges"
        )
    }

    @Test
    fun `very long distance still works correctly`() {
        val fromCenter = Point(0f, 0f)
        val toCenter = Point(10000f, 10000f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)
        assertTrue(
            ConnectionLineCalculator.verifyEndpointsAtDotEdges(
                line, fromCenter, toCenter, dotRadius
            ),
            "Long distance lines should have correct endpoints"
        )
        assertTrue(
            ConnectionLineCalculator.verifyEndpointsAlignedWithCenters(
                line, fromCenter, toCenter
            ),
            "Long distance lines should be aligned"
        )
    }

    @Test
    fun `negative coordinates work correctly`() {
        val fromCenter = Point(-100f, -50f)
        val toCenter = Point(100f, 50f)

        val line = ConnectionLineCalculator.calculateConnectionLine(
            fromCenter, toCenter, dotRadius, seed = 123L
        )

        assertNotNull(line)
        assertTrue(
            ConnectionLineCalculator.verifyEndpointsAtDotEdges(
                line, fromCenter, toCenter, dotRadius
            ),
            "Negative coordinates should work correctly"
        )
    }
}
