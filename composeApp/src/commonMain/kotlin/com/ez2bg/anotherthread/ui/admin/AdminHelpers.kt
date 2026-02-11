package com.ez2bg.anotherthread.ui.admin

import androidx.compose.ui.graphics.Color
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.ui.terrain.containsAny

// Data classes for location graph positioning

data class LocationPosition(val location: LocationDto, val x: Float, val y: Float)

data class GridPositionResult(
    val locationPositions: Map<String, LocationPosition>,
    val gridPositions: Map<String, Pair<Int, Int>>,
    val gridBounds: GridBounds
)

data class GridBounds(
    val minX: Int, val maxX: Int,
    val minY: Int, val maxY: Int,
    val padding: Float = 0.15f
)

data class NodeState(
    val id: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

// Force-directed layout constants
object LayoutConstants {
    const val REPULSION_STRENGTH = 0.02f
    const val ATTRACTION_STRENGTH = 0.03f  // Increased for tighter clustering
    const val CENTER_PULL = 0.005f
    const val DAMPING = 0.9f
    const val MIN_DISTANCE = 0.15f
    const val ITERATIONS = 100
}

// Dark crimson color for dot borders
val DotBorderColor = Color(0xFF8B1A1A)

// ExitDirection extension functions

fun ExitDirection.toShortLabel(): String = when (this) {
    ExitDirection.NORTH -> "N"
    ExitDirection.NORTHEAST -> "NE"
    ExitDirection.EAST -> "E"
    ExitDirection.SOUTHEAST -> "SE"
    ExitDirection.SOUTH -> "S"
    ExitDirection.SOUTHWEST -> "SW"
    ExitDirection.WEST -> "W"
    ExitDirection.NORTHWEST -> "NW"
    ExitDirection.UP -> "U"
    ExitDirection.DOWN -> "D"
    ExitDirection.ENTER -> "EN"
    ExitDirection.UNKNOWN -> "?"
}

fun ExitDirection.toDisplayLabel(): String = when (this) {
    ExitDirection.NORTH -> "North"
    ExitDirection.NORTHEAST -> "Northeast"
    ExitDirection.EAST -> "East"
    ExitDirection.SOUTHEAST -> "Southeast"
    ExitDirection.SOUTH -> "South"
    ExitDirection.SOUTHWEST -> "Southwest"
    ExitDirection.WEST -> "West"
    ExitDirection.NORTHWEST -> "Northwest"
    ExitDirection.UP -> "Up"
    ExitDirection.DOWN -> "Down"
    ExitDirection.ENTER -> "Enter"
    ExitDirection.UNKNOWN -> "Unknown"
}

fun getOppositeDirection(direction: ExitDirection): ExitDirection = when (direction) {
    ExitDirection.NORTH -> ExitDirection.SOUTH
    ExitDirection.NORTHEAST -> ExitDirection.SOUTHWEST
    ExitDirection.EAST -> ExitDirection.WEST
    ExitDirection.SOUTHEAST -> ExitDirection.NORTHWEST
    ExitDirection.SOUTH -> ExitDirection.NORTH
    ExitDirection.SOUTHWEST -> ExitDirection.NORTHEAST
    ExitDirection.WEST -> ExitDirection.EAST
    ExitDirection.NORTHWEST -> ExitDirection.SOUTHEAST
    ExitDirection.UP -> ExitDirection.DOWN
    ExitDirection.DOWN -> ExitDirection.UP
    ExitDirection.ENTER -> ExitDirection.ENTER
    ExitDirection.UNKNOWN -> ExitDirection.UNKNOWN
}

/**
 * Get the unit vector (dx, dy) for a direction.
 * North is up (negative y), East is right (positive x).
 */
fun getDirectionVector(direction: ExitDirection): Pair<Float, Float> = when (direction) {
    ExitDirection.NORTH -> Pair(0f, -1f)
    ExitDirection.NORTHEAST -> Pair(0.707f, -0.707f)
    ExitDirection.EAST -> Pair(1f, 0f)
    ExitDirection.SOUTHEAST -> Pair(0.707f, 0.707f)
    ExitDirection.SOUTH -> Pair(0f, 1f)
    ExitDirection.SOUTHWEST -> Pair(-0.707f, 0.707f)
    ExitDirection.WEST -> Pair(-1f, 0f)
    ExitDirection.NORTHWEST -> Pair(-0.707f, -0.707f)
    ExitDirection.UP -> Pair(0f, 0f) // Vertical - no x,y offset
    ExitDirection.DOWN -> Pair(0f, 0f) // Vertical - no x,y offset
    ExitDirection.ENTER -> Pair(0f, 0f) // Portal/entrance - no directional bias
    ExitDirection.UNKNOWN -> Pair(0f, 0f) // No directional bias
}

/**
 * Get grid offset for a direction.
 * Returns (dx, dy) where positive X is east, positive Y is south.
 */
fun getGridOffset(direction: ExitDirection): Pair<Int, Int> = when (direction) {
    ExitDirection.NORTH -> Pair(0, -1)
    ExitDirection.NORTHEAST -> Pair(1, -1)
    ExitDirection.EAST -> Pair(1, 0)
    ExitDirection.SOUTHEAST -> Pair(1, 1)
    ExitDirection.SOUTH -> Pair(0, 1)
    ExitDirection.SOUTHWEST -> Pair(-1, 1)
    ExitDirection.WEST -> Pair(-1, 0)
    ExitDirection.NORTHWEST -> Pair(-1, -1)
    ExitDirection.UP -> Pair(0, 0) // Vertical - no x,y offset (z changes)
    ExitDirection.DOWN -> Pair(0, 0) // Vertical - no x,y offset (z changes)
    ExitDirection.ENTER -> Pair(0, 0) // Portal - no grid offset (different z-level)
    ExitDirection.UNKNOWN -> Pair(0, 1) // Default to south for unknown
}

/**
 * Get direction from grid offset.
 */
fun getDirectionFromOffset(dx: Int, dy: Int): ExitDirection = when {
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
 * Calculate positions using stored database coordinates when available.
 * Falls back to BFS-based placement for locations without stored coordinates.
 * If location A has a SOUTH exit to location B, then B is placed directly south of A.
 */
fun calculateForceDirectedPositions(
    locations: List<LocationDto>
): GridPositionResult {
    if (locations.isEmpty()) return GridPositionResult(emptyMap(), emptyMap(), GridBounds(0, 0, 0, 0))
    if (locations.size == 1) {
        val loc = locations[0]
        val x = loc.gridX ?: 0
        val y = loc.gridY ?: 0
        return GridPositionResult(
            locationPositions = mapOf(loc.id to LocationPosition(loc, 0.5f, 0.5f)),
            gridPositions = mapOf(loc.id to Pair(x, y)),
            gridBounds = GridBounds(x, x, y, y)
        )
    }

    val locationMap = locations.associateBy { it.id }

    // Grid positions: (gridX, gridY) where positive X is east, positive Y is south
    val gridPositions = mutableMapOf<String, Pair<Int, Int>>()
    val visited = mutableSetOf<String>()

    // First pass: Use stored database coordinates for locations that have them
    locations.filter { it.gridX != null && it.gridY != null }.forEach { loc ->
        gridPositions[loc.id] = Pair(loc.gridX!!, loc.gridY!!)
        visited.add(loc.id)
    }

    // Second pass: BFS to place locations without stored coordinates
    // Start from a location with coordinates, or the first location if none have coords
    val startLocation = locations.find { it.gridX != null } ?: locations.first()
    if (startLocation.id !in gridPositions) {
        gridPositions[startLocation.id] = Pair(0, 0)
        visited.add(startLocation.id)
    }

    // BFS to place remaining connected locations
    val queue = ArrayDeque<String>()
    // Add all locations with coordinates to the queue to expand from
    gridPositions.keys.forEach { queue.add(it) }

    while (queue.isNotEmpty()) {
        val currentId = queue.removeFirst()
        val currentPos = gridPositions[currentId] ?: continue
        val currentLocation = locationMap[currentId] ?: continue

        // Place neighbors based on exit directions
        for (exit in currentLocation.exits) {
            val neighborId = exit.locationId
            val neighbor = locationMap[neighborId] ?: continue

            if (neighborId !in visited) {
                // Check if neighbor has stored coordinates
                if (neighbor.gridX != null && neighbor.gridY != null) {
                    gridPositions[neighborId] = Pair(neighbor.gridX, neighbor.gridY)
                } else {
                    // Calculate position from exit direction
                    val (dx, dy) = getGridOffset(exit.direction)
                    val newPos = Pair(currentPos.first + dx, currentPos.second + dy)

                    // Check if position is already occupied
                    val existingAtPos = gridPositions.entries.find { it.value == newPos }
                    if (existingAtPos == null) {
                        gridPositions[neighborId] = newPos
                    } else {
                        // Position occupied, find nearby free spot
                        gridPositions[neighborId] = findNearbyFreeSpot(newPos, gridPositions.values.toSet())
                    }
                }

                visited.add(neighborId)
                queue.add(neighborId)
            }
        }
    }

    // Handle disconnected locations (not reachable from any placed location)
    locations.filter { it.id !in gridPositions }.forEachIndexed { index, loc ->
        // Use stored coords if available, otherwise place below the main graph
        if (loc.gridX != null && loc.gridY != null) {
            gridPositions[loc.id] = Pair(loc.gridX, loc.gridY)
        } else {
            val maxY = gridPositions.values.maxOfOrNull { it.second } ?: 0
            gridPositions[loc.id] = findNearbyFreeSpot(Pair(index, maxY + 2), gridPositions.values.toSet())
        }
    }

    // Normalize grid positions to 0.0-1.0 range
    val allPositions = gridPositions.values.toList()
    val minX = allPositions.minOfOrNull { it.first } ?: 0
    val maxX = allPositions.maxOfOrNull { it.first } ?: 0
    val minY = allPositions.minOfOrNull { it.second } ?: 0
    val maxY = allPositions.maxOfOrNull { it.second } ?: 0

    val rangeX = (maxX - minX).coerceAtLeast(1)
    val rangeY = (maxY - minY).coerceAtLeast(1)

    // Add padding and convert to normalized coordinates
    val spacingMultiplier = 1.0f
    val padding = 0.15f
    val availableRange = 1f - 2 * padding

    val locationPositions = gridPositions.mapValues { (id, gridPos) ->
        val normalizedX = if (rangeX == 1) 0.5f else padding + availableRange * (gridPos.first - minX).toFloat() / rangeX * spacingMultiplier
        val normalizedY = if (rangeY == 1) 0.5f else padding + availableRange * (gridPos.second - minY).toFloat() / rangeY * spacingMultiplier
        LocationPosition(
            location = locationMap[id]!!,
            x = normalizedX,
            y = normalizedY
        )
    }

    return GridPositionResult(
        locationPositions = locationPositions,
        gridPositions = gridPositions,
        gridBounds = GridBounds(minX, maxX, minY, maxY, padding)
    )
}

/**
 * Find a free spot near the target position.
 */
fun findNearbyFreeSpot(target: Pair<Int, Int>, occupied: Set<Pair<Int, Int>>): Pair<Int, Int> {
    if (target !in occupied) return target

    // Spiral outward to find free spot
    for (radius in 1..10) {
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (kotlin.math.abs(dx) == radius || kotlin.math.abs(dy) == radius) {
                    val candidate = Pair(target.first + dx, target.second + dy)
                    if (candidate !in occupied) return candidate
                }
            }
        }
    }
    return Pair(target.first + 10, target.second) // Fallback
}

/**
 * Get a color based on the location's terrain type.
 */
fun getTerrainColor(desc: String, name: String): Color {
    val text = (desc + " " + name).lowercase()

    return when {
        text.containsAny("castle", "fortress", "citadel", "stronghold") -> Color(0xFF8B7355) // Brown
        text.containsAny("church", "temple", "cathedral", "shrine") -> Color(0xFFE8E4DC) // Light gray
        text.containsAny("forest", "tree", "wood", "grove") -> Color(0xFF4A6741) // Forest green
        text.containsAny("mountain", "peak", "summit") -> Color(0xFF7D7461) // Gray-brown
        text.containsAny("water", "river", "lake", "coast", "sea", "ocean") -> Color(0xFF6B8E9F) // Blue-gray
        text.containsAny("swamp", "marsh", "bog") -> Color(0xFF5A6B52) // Murky green
        text.containsAny("desert", "sand", "dune") -> Color(0xFFD4C19E) // Sandy
        text.containsAny("cave", "cavern", "dungeon") -> Color(0xFF5A5A5A) // Dark gray
        text.containsAny("town", "village", "inn", "tavern", "shop") -> Color(0xFFB8976B) // Tan
        text.containsAny("port", "dock", "harbor") -> Color(0xFF8BA4B0) // Coastal blue
        text.containsAny("ruin", "ancient", "abandon") -> Color(0xFF8A7B6A) // Aged stone
        text.containsAny("grass", "meadow", "field", "plain") -> Color(0xFF7A9A6D) // Muted green
        else -> Color(0xFFC4A67C) // Default parchment tan
    }
}

// String extension for splitting comma-separated lists
fun String.splitToList(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }

// Time formatting utilities

fun formatTimeAgo(millisAgo: Long): String {
    return when {
        millisAgo < 60_000 -> "Online now"
        millisAgo < 3_600_000 -> "${millisAgo / 60_000}m ago"
        millisAgo < 86_400_000 -> "${millisAgo / 3_600_000}h ago"
        millisAgo < 604_800_000 -> "${millisAgo / 86_400_000}d ago"
        else -> "${millisAgo / 604_800_000}w ago"
    }
}

fun formatTimestamp(timestamp: Long): String {
    // Simple relative time formatting
    val now = com.ez2bg.anotherthread.platform.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
