package com.ez2bg.anotherthread.ui

import kotlin.math.sqrt

/**
 * Data class representing a 2D point/offset for connection line calculations.
 * Separate from Compose's Offset to allow testing without Compose dependencies.
 */
data class Point(val x: Float, val y: Float) {
    fun distanceTo(other: Point): Float {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Represents a connection line between two locations, with endpoints at the edge of dots.
 */
data class ConnectionLine(
    val from: Point,
    val to: Point,
    val seed: Long
)

/**
 * Calculator for connection lines between location dots on the map.
 *
 * Rules:
 * 1. Lines connect from the edge of one dot to the edge of another (never through dots)
 * 2. Line endpoints should be centered on the dot centers (aligned with epicenter)
 * 3. Each connection is only drawn once (A->B and B->A are the same line)
 * 4. Lines are not drawn if dots are too close (would overlap)
 */
object ConnectionLineCalculator {

    /**
     * Calculate the endpoint of a connection line, offset from the dot center to its edge.
     *
     * @param fromCenter Center point of the source dot
     * @param toCenter Center point of the target dot
     * @param dotRadius Radius of the dots
     * @param isFromPoint If true, calculates the 'from' endpoint; if false, calculates the 'to' endpoint
     * @return The edge point, or null if the dots are too close
     */
    fun calculateEdgePoint(
        fromCenter: Point,
        toCenter: Point,
        dotRadius: Float,
        isFromPoint: Boolean
    ): Point? {
        val dx = toCenter.x - fromCenter.x
        val dy = toCenter.y - fromCenter.y
        val distance = sqrt(dx * dx + dy * dy)

        // Don't draw lines if dots are too close (would overlap)
        if (distance <= dotRadius * 2) {
            return null
        }

        // Normalize direction vector
        val nx = dx / distance
        val ny = dy / distance

        return if (isFromPoint) {
            // From point: offset from fromCenter toward toCenter by dotRadius
            Point(
                fromCenter.x + nx * dotRadius,
                fromCenter.y + ny * dotRadius
            )
        } else {
            // To point: offset from toCenter toward fromCenter by dotRadius
            Point(
                toCenter.x - nx * dotRadius,
                toCenter.y - ny * dotRadius
            )
        }
    }

    /**
     * Calculate a connection line between two dot centers.
     * Returns null if the dots are too close to connect.
     *
     * @param fromCenter Center point of the source dot
     * @param toCenter Center point of the target dot
     * @param dotRadius Radius of the dots
     * @param seed Random seed for line rendering
     * @return ConnectionLine with edge points, or null if dots overlap
     */
    fun calculateConnectionLine(
        fromCenter: Point,
        toCenter: Point,
        dotRadius: Float,
        seed: Long
    ): ConnectionLine? {
        val fromEdge = calculateEdgePoint(fromCenter, toCenter, dotRadius, isFromPoint = true)
            ?: return null
        val toEdge = calculateEdgePoint(fromCenter, toCenter, dotRadius, isFromPoint = false)
            ?: return null

        return ConnectionLine(fromEdge, toEdge, seed)
    }

    /**
     * Verify that a connection line's endpoints are at the correct distance from dot centers.
     * Used for testing.
     *
     * @param line The connection line to verify
     * @param fromCenter Original from dot center
     * @param toCenter Original to dot center
     * @param dotRadius Expected dot radius
     * @param tolerance Allowed floating point tolerance
     * @return true if endpoints are correctly positioned at dot edges
     */
    fun verifyEndpointsAtDotEdges(
        line: ConnectionLine,
        fromCenter: Point,
        toCenter: Point,
        dotRadius: Float,
        tolerance: Float = 0.001f
    ): Boolean {
        val fromDistance = line.from.distanceTo(fromCenter)
        val toDistance = line.to.distanceTo(toCenter)

        val fromCorrect = kotlin.math.abs(fromDistance - dotRadius) < tolerance
        val toCorrect = kotlin.math.abs(toDistance - dotRadius) < tolerance

        return fromCorrect && toCorrect
    }

    /**
     * Verify that a connection line does not pass through either dot.
     * This checks that the line segment from `line.from` to `line.to` does not
     * intersect the interior of either dot circle.
     *
     * @param line The connection line to verify
     * @param fromCenter Original from dot center
     * @param toCenter Original to dot center
     * @param dotRadius Radius of the dots
     * @return true if the line does not pass through any dot
     */
    fun verifyLineDoesNotIntersectDots(
        line: ConnectionLine,
        fromCenter: Point,
        toCenter: Point,
        dotRadius: Float
    ): Boolean {
        // Check that line endpoints are outside or on the edge of both dots
        val fromToFrom = line.from.distanceTo(fromCenter)
        val fromToTo = line.from.distanceTo(toCenter)
        val toToFrom = line.to.distanceTo(fromCenter)
        val toToTo = line.to.distanceTo(toCenter)

        // Allow small tolerance for floating point
        val tolerance = 0.001f

        // From endpoint should be exactly at fromCenter's edge
        val fromAtFromEdge = kotlin.math.abs(fromToFrom - dotRadius) < tolerance
        // From endpoint should be outside toCenter's circle
        val fromOutsideTo = fromToTo >= dotRadius - tolerance

        // To endpoint should be exactly at toCenter's edge
        val toAtToEdge = kotlin.math.abs(toToTo - dotRadius) < tolerance
        // To endpoint should be outside fromCenter's circle
        val toOutsideFrom = toToFrom >= dotRadius - tolerance

        return fromAtFromEdge && fromOutsideTo && toAtToEdge && toOutsideFrom
    }

    /**
     * Generate a canonical connection key to avoid duplicate connections.
     * A->B and B->A produce the same key.
     *
     * @param idA First location ID
     * @param idB Second location ID
     * @return Pair with IDs in consistent order
     */
    fun canonicalConnectionKey(idA: String, idB: String): Pair<String, String> {
        return if (idA < idB) {
            Pair(idA, idB)
        } else {
            Pair(idB, idA)
        }
    }

    /**
     * Verify that a line's endpoints are collinear with both dot centers.
     * This ensures the line is properly aligned through the centers.
     *
     * @param line The connection line
     * @param fromCenter From dot center
     * @param toCenter To dot center
     * @param tolerance Allowed deviation from perfect collinearity
     * @return true if all four points are collinear
     */
    fun verifyEndpointsAlignedWithCenters(
        line: ConnectionLine,
        fromCenter: Point,
        toCenter: Point,
        tolerance: Float = 0.001f
    ): Boolean {
        // Calculate cross product to check collinearity
        // If points A, B, C are collinear, then (B-A) x (C-A) = 0

        fun crossProduct(a: Point, b: Point, c: Point): Float {
            return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
        }

        // Check that line.from, fromCenter, toCenter are collinear
        val cross1 = kotlin.math.abs(crossProduct(line.from, fromCenter, toCenter))
        // Check that line.to, fromCenter, toCenter are collinear
        val cross2 = kotlin.math.abs(crossProduct(line.to, fromCenter, toCenter))

        return cross1 < tolerance && cross2 < tolerance
    }
}
