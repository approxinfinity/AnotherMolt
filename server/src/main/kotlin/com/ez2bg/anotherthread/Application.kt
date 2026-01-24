package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.combat.*
import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.spell.*
import io.ktor.server.request.header
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

@Serializable
data class ExitRequest(
    val locationId: String,
    val direction: ExitDirection = ExitDirection.UNKNOWN
)

/**
 * Request to validate an exit between two locations.
 */
@Serializable
data class ValidateExitRequest(
    val fromLocationId: String,
    val toLocationId: String
)

/**
 * Information about a valid direction for creating an exit.
 */
@Serializable
data class ValidDirectionInfo(
    val direction: ExitDirection,
    val isFixed: Boolean,  // True if this is the only valid direction (target has coordinates)
    val targetCoordinates: CoordinateInfo?  // Where target would be placed
)

@Serializable
data class CoordinateInfo(
    val x: Int,
    val y: Int,
    val areaId: String? = null
)

/**
 * Response for exit validation endpoint.
 */
@Serializable
data class ValidateExitResponse(
    val canCreateExit: Boolean,
    val validDirections: List<ValidDirectionInfo>,
    val errorMessage: String? = null,
    val targetHasCoordinates: Boolean,
    val targetIsConnected: Boolean  // True if target has exits connecting it to a coordinate system
)

/**
 * Data integrity validation issue.
 */
@Serializable
data class IntegrityIssue(
    val type: String,           // EXIT_TOO_FAR, DIRECTION_MISMATCH, MISSING_TARGET, DUPLICATE_COORDS
    val severity: String,       // ERROR, WARNING
    val locationId: String,
    val locationName: String,
    val message: String,
    val relatedLocationId: String? = null,
    val relatedLocationName: String? = null
)

/**
 * Response for data integrity check endpoint.
 */
@Serializable
data class DataIntegrityResponse(
    val success: Boolean,
    val totalLocations: Int,
    val issuesFound: Int,
    val issues: List<IntegrityIssue>
)

/**
 * Admin user info for user management panel.
 */
@Serializable
data class AdminUserInfo(
    val id: String,
    val name: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val currentLocationId: String?,
    val currentLocationName: String?,
    val imageUrl: String?
)

/**
 * Response for admin users list endpoint.
 */
@Serializable
data class AdminUsersResponse(
    val success: Boolean,
    val totalUsers: Int,
    val users: List<AdminUserInfo>
)

@Serializable
data class CreateLocationRequest(
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val creatureIds: List<String> = emptyList(),
    val exits: List<ExitRequest> = emptyList(),
    val featureIds: List<String> = emptyList()
)

@Serializable
data class CreateCreatureRequest(
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
    // Combat stats
    val maxHp: Int = 10,
    val baseDamage: Int = 5,
    val abilityIds: List<String> = emptyList(),
    val level: Int = 1,
    val experienceValue: Int = 10,
    val challengeRating: Int = 1,
    val isAggressive: Boolean = false
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val desc: String,
    val featureIds: List<String> = emptyList()
)

@Serializable
data class CreateCharacterClassRequest(
    val name: String,
    val description: String,
    val isSpellcaster: Boolean,
    val hitDie: Int,
    val primaryAttribute: String,
    val imageUrl: String? = null,
    val powerBudget: Int = 100,
    val isPublic: Boolean = true,
    val createdByUserId: String? = null
)

@Serializable
data class CreateAbilityRequest(
    val name: String,
    val description: String,
    val classId: String? = null,
    val abilityType: String,
    val targetType: String,
    val range: Int,
    val cooldownType: String,
    val cooldownRounds: Int = 0,
    val effects: String = "[]",
    val imageUrl: String? = null,
    val baseDamage: Int = 0,
    val durationRounds: Int = 0
)

@Serializable
data class CreateFeatureCategoryRequest(
    val name: String,
    val description: String
)

// Class generation and matching
@Serializable
data class MatchClassRequest(
    val characterDescription: String
)

@Serializable
data class GenerateClassRequest(
    val characterDescription: String,
    val isPublic: Boolean = false
)

@Serializable
data class CreateNerfRequestRequest(
    val abilityId: String,
    val reason: String
)

@Serializable
data class ResolveNerfRequestRequest(
    val status: String,  // approved, rejected, applied
    val adminNotes: String? = null,
    val applyChanges: Boolean = false  // If true and status is approved, apply suggested changes
)

@Serializable
data class CreateFeatureRequest(
    val id: String? = null,  // Optional - if not provided, UUID is generated
    val name: String,
    val featureCategoryId: String? = null,
    val description: String,
    val data: String = "{}"
)

@Serializable
data class RegisterRequest(
    val name: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val name: String,
    val password: String
)

@Serializable
data class UpdateUserRequest(
    val desc: String = "",
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList()
)

@Serializable
data class UpdateLocationRequest(
    val locationId: String?
)

@Serializable
data class UpdateClassRequest(
    val classId: String? = null
)

@Serializable
data class AssignClassRequest(
    val generateClass: Boolean,
    val characterDescription: String
)

@Serializable
data class AssignClassResponse(
    val success: Boolean,
    val user: UserResponse,
    val assignedClass: CharacterClass? = null,
    val message: String? = null
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse? = null
)

@Serializable
data class GenerateLocationContentRequest(
    val exitIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
    val existingName: String? = null,
    val existingDesc: String? = null
)

@Serializable
data class GenerateCreatureContentRequest(
    val existingName: String? = null,
    val existingDesc: String? = null
)

@Serializable
data class GenerateItemContentRequest(
    val existingName: String? = null,
    val existingDesc: String? = null
)

@Serializable
data class GeneratedContentResponse(
    val name: String,
    val description: String
)

@Serializable
data class GenerateImageRequest(
    val entityType: String,
    val entityId: String,
    val name: String,
    val description: String,
    val featureIds: List<String> = emptyList()
)

@Serializable
data class LockRequest(
    val userId: String
)

@Serializable
data class GenerateImageResponse(
    val imageUrl: String
)

@Serializable
data class UploadedFileResponse(
    val filename: String,
    val url: String,
    val size: Long,
    val lastModified: Long
)

@Serializable
data class FileUploadResponse(
    val success: Boolean,
    val url: String? = null,
    val error: String? = null
)

@Serializable
data class ServiceStatus(
    val name: String,
    val displayName: String,
    val healthy: Boolean,
    val url: String? = null
)

@Serializable
data class ServiceActionRequest(
    val action: String // "start", "stop", "restart"
)

@Serializable
data class ServiceActionResponse(
    val success: Boolean,
    val message: String
)

// Spell casting DTOs
@Serializable
data class CastSpellRequest(
    val userId: String,
    val featureId: String,
    val targetParams: Map<String, String> = emptyMap()
)

@Serializable
data class CastSpellResponse(
    val success: Boolean,
    val message: String,
    val newLocationId: String? = null,
    val revealedInfo: SpellService.RevealedInfo? = null,
    val spellState: SpellService.SpellStateInfo? = null
)

// Admin feature ID constant
const val ADMIN_FEATURE_ID = "1"

/**
 * Get the opposite direction for bidirectional exit creation.
 */
fun getOppositeDirection(direction: ExitDirection): ExitDirection = when (direction) {
    ExitDirection.NORTH -> ExitDirection.SOUTH
    ExitDirection.SOUTH -> ExitDirection.NORTH
    ExitDirection.EAST -> ExitDirection.WEST
    ExitDirection.WEST -> ExitDirection.EAST
    ExitDirection.NORTHEAST -> ExitDirection.SOUTHWEST
    ExitDirection.SOUTHWEST -> ExitDirection.NORTHEAST
    ExitDirection.NORTHWEST -> ExitDirection.SOUTHEAST
    ExitDirection.SOUTHEAST -> ExitDirection.NORTHWEST
    ExitDirection.ENTER -> ExitDirection.ENTER
    ExitDirection.UNKNOWN -> ExitDirection.UNKNOWN
}

/**
 * All 8 cardinal and intercardinal directions (excluding UNKNOWN).
 */
val ALL_DIRECTIONS = listOf(
    ExitDirection.NORTH,
    ExitDirection.NORTHEAST,
    ExitDirection.EAST,
    ExitDirection.SOUTHEAST,
    ExitDirection.SOUTH,
    ExitDirection.SOUTHWEST,
    ExitDirection.WEST,
    ExitDirection.NORTHWEST
)

/**
 * Get the grid offset (dx, dy) for a direction.
 * Note: NORTH is -Y (up on screen), SOUTH is +Y (down on screen)
 */
fun getDirectionOffset(direction: ExitDirection): Pair<Int, Int> = when (direction) {
    ExitDirection.NORTH -> Pair(0, -1)
    ExitDirection.NORTHEAST -> Pair(1, -1)
    ExitDirection.EAST -> Pair(1, 0)
    ExitDirection.SOUTHEAST -> Pair(1, 1)
    ExitDirection.SOUTH -> Pair(0, 1)
    ExitDirection.SOUTHWEST -> Pair(-1, 1)
    ExitDirection.WEST -> Pair(-1, 0)
    ExitDirection.NORTHWEST -> Pair(-1, -1)
    ExitDirection.ENTER -> Pair(0, 0) // Portal - no grid offset
    ExitDirection.UNKNOWN -> Pair(0, 0)
}

/**
 * Generate a wilderness description based on the parent location's features.
 */
fun generateWildernessDescription(parentFeatureIds: List<String>, parentDescription: String): String {
    val features = parentFeatureIds.mapNotNull { FeatureRepository.findById(it) }
    val featureNames = features.map { it.name.lowercase() }

    val elements = mutableListOf<String>()

    // Check for terrain-related features
    if (featureNames.any { it.contains("forest") || it.contains("tree") }) {
        elements.add("sparse trees dot the landscape")
    }
    if (featureNames.any { it.contains("river") || it.contains("stream") || it.contains("water") }) {
        elements.add("the sound of water can be heard in the distance")
    }
    if (featureNames.any { it.contains("road") || it.contains("path") || it.contains("trail") }) {
        elements.add("a faint path leads onward")
    }
    if (featureNames.any { it.contains("mountain") || it.contains("hill") }) {
        elements.add("rocky terrain rises in the distance")
    }
    if (featureNames.any { it.contains("grass") || it.contains("meadow") || it.contains("plain") }) {
        elements.add("tall grasses sway in the breeze")
    }
    if (featureNames.any { it.contains("desert") || it.contains("sand") }) {
        elements.add("sand stretches toward the horizon")
    }
    if (featureNames.any { it.contains("swamp") || it.contains("marsh") || it.contains("bog") }) {
        elements.add("murky ground squelches underfoot")
    }
    if (featureNames.any { it.contains("lake") || it.contains("pond") }) {
        elements.add("still waters glimmer nearby")
    }

    return if (elements.isNotEmpty()) {
        "An untamed stretch of wilderness. ${elements.joinToString(". ")}."
    } else {
        "An untamed stretch of wilderness stretches before you."
    }
}

/**
 * Create wilderness locations for directions around a parent location where no location exists.
 * Only creates wilderness in directions where the grid cell is empty.
 * Returns a map of direction to created wilderness location.
 */
fun createWildernessLocationsForParent(
    parentLocation: Location,
    userId: String,
    userName: String
): Map<ExitDirection, Location> {
    val createdLocations = mutableMapOf<ExitDirection, Location>()
    val wildernessDesc = generateWildernessDescription(parentLocation.featureIds, parentLocation.desc)

    val parentX = parentLocation.gridX ?: return emptyMap()
    val parentY = parentLocation.gridY ?: return emptyMap()
    val parentAreaId = parentLocation.areaId ?: "overworld"

    for (direction in ALL_DIRECTIONS) {
        val (dx, dy) = getDirectionOffset(direction)
        val targetX = parentX + dx
        val targetY = parentY + dy

        // Check if there's already a location at this coordinate
        val existingLocation = LocationRepository.findByCoordinates(targetX, targetY, parentAreaId)
        if (existingLocation != null) {
            // Location already exists at this coordinate - don't create wilderness
            continue
        }

        val oppositeDir = getOppositeDirection(direction)

        // Create the wilderness location with an exit back to the parent
        val wilderness = Location(
            name = "Wilderness",
            desc = wildernessDesc,
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = listOf(Exit(locationId = parentLocation.id, direction = oppositeDir)),
            featureIds = parentLocation.featureIds, // Inherit parent's terrain features
            gridX = targetX,
            gridY = targetY,
            areaId = parentAreaId
        )

        val createdWilderness = LocationRepository.create(wilderness)
        createdLocations[direction] = createdWilderness

        // Audit log for wilderness creation
        AuditLogRepository.log(
            recordId = createdWilderness.id,
            recordType = "Location",
            recordName = "Wilderness (auto-created)",
            action = AuditAction.CREATE,
            userId = userId,
            userName = userName
        )
    }

    return createdLocations
}

/**
 * Update the parent location to have exits to the wilderness locations.
 * Only adds exits for directions that have wilderness created.
 */
fun addWildernessExitsToParent(
    parentLocation: Location,
    wildernessLocations: Map<ExitDirection, Location>
): Location {
    val newExits = parentLocation.exits.toMutableList()
    for ((direction, wilderness) in wildernessLocations) {
        // Only add if we don't already have an exit in this direction
        if (newExits.none { it.direction == direction }) {
            newExits.add(Exit(locationId = wilderness.id, direction = direction))
        }
    }

    val updatedParent = parentLocation.copy(exits = newExits)
    LocationRepository.update(updatedParent)
    return updatedParent
}

/**
 * Get the direction from source to target based on their coordinates.
 * Returns null if not exactly 1 cell apart in a valid direction.
 */
fun getDirectionBetweenCoordinates(
    fromX: Int, fromY: Int, fromAreaId: String,
    toX: Int, toY: Int, toAreaId: String
): ExitDirection? {
    // Must be in the same area for cardinal directions
    if (fromAreaId != toAreaId) return null

    val dx = toX - fromX
    val dy = toY - fromY

    // Must be exactly 1 step away
    if (kotlin.math.abs(dx) > 1 || kotlin.math.abs(dy) > 1) return null
    if (dx == 0 && dy == 0) return null

    return when {
        dx == 0 && dy == -1 -> ExitDirection.NORTH
        dx == 1 && dy == -1 -> ExitDirection.NORTHEAST
        dx == 1 && dy == 0 -> ExitDirection.EAST
        dx == 1 && dy == 1 -> ExitDirection.SOUTHEAST
        dx == 0 && dy == 1 -> ExitDirection.SOUTH
        dx == -1 && dy == 1 -> ExitDirection.SOUTHWEST
        dx == -1 && dy == 0 -> ExitDirection.WEST
        dx == -1 && dy == -1 -> ExitDirection.NORTHWEST
        else -> null
    }
}

/**
 * Find a random unused coordinate for a new location in the overworld.
 * Searches in an expanding spiral from origin, with randomization.
 */
fun findRandomUnusedCoordinate(existingLocations: List<Location>, areaId: String = "overworld"): Pair<Int, Int> {
    val usedCoords = existingLocations
        .filter { it.gridX != null && it.gridY != null && (it.areaId ?: "overworld") == areaId }
        .map { Pair(it.gridX!!, it.gridY!!) }
        .toSet()

    // If no locations exist in this area, return origin
    if (usedCoords.isEmpty()) return Pair(0, 0)

    // Search in expanding rings around the origin, picking a random unused spot
    val random = java.util.Random()
    for (radius in 1..100) {
        val candidates = mutableListOf<Pair<Int, Int>>()

        // Collect all coordinates at this radius (Manhattan distance)
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                if (kotlin.math.abs(x) == radius || kotlin.math.abs(y) == radius) {
                    val coord = Pair(x, y)
                    if (coord !in usedCoords) {
                        candidates.add(coord)
                    }
                }
            }
        }

        // If we found candidates at this radius, pick one randomly
        if (candidates.isNotEmpty()) {
            return candidates[random.nextInt(candidates.size)]
        }
    }

    // Fallback: very far away random coordinate
    return Pair(random.nextInt(1000) + 100, random.nextInt(1000) + 100)
}

/**
 * Check if a location is connected to the coordinate system (has coordinates or is reachable from one that does).
 */
fun isLocationConnected(location: Location, allLocations: List<Location>): Boolean {
    // If it has coordinates, it's connected
    if (location.gridX != null && location.gridY != null) return true

    // If it has exits to locations with coordinates, it's connected
    val locationById = allLocations.associateBy { it.id }
    for (exit in location.exits) {
        val targetLoc = locationById[exit.locationId] ?: continue
        if (targetLoc.gridX != null && targetLoc.gridY != null) return true
    }

    // Also check if any location has an exit TO this location
    for (loc in allLocations) {
        if (loc.gridX != null && loc.gridY != null) {
            if (loc.exits.any { it.locationId == location.id }) return true
        }
    }

    return false
}

/**
 * Get all locations in the same connected subgraph as the given location.
 * A subgraph includes all locations reachable through exits (bidirectional).
 */
fun getConnectedSubgraph(startLocation: Location, allLocations: List<Location>): Set<String> {
    val locationById = allLocations.associateBy { it.id }
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.add(startLocation.id)

    while (queue.isNotEmpty()) {
        val currentId = queue.removeFirst()
        if (currentId in visited) continue
        visited.add(currentId)

        val current = locationById[currentId] ?: continue

        // Add all locations this one has exits to
        for (exit in current.exits) {
            if (exit.locationId !in visited) {
                queue.add(exit.locationId)
            }
        }

        // Add all locations that have exits to this one
        for (loc in allLocations) {
            if (loc.id !in visited && loc.exits.any { it.locationId == currentId }) {
                queue.add(loc.id)
            }
        }
    }

    return visited
}

/**
 * Calculate relative positions within a subgraph using BFS from exit directions.
 * Returns a map of location ID to relative (x, y) offset from the anchor location.
 * The anchor location is at (0, 0).
 */
fun calculateSubgraphRelativePositions(
    anchorLocation: Location,
    subgraphIds: Set<String>,
    allLocations: List<Location>
): Map<String, Pair<Int, Int>> {
    val locationById = allLocations.associateBy { it.id }
    val positions = mutableMapOf<String, Pair<Int, Int>>()
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque<String>()

    // Start with anchor at (0, 0)
    positions[anchorLocation.id] = Pair(0, 0)
    visited.add(anchorLocation.id)
    queue.add(anchorLocation.id)

    while (queue.isNotEmpty()) {
        val currentId = queue.removeFirst()
        val currentPos = positions[currentId] ?: continue
        val currentLoc = locationById[currentId] ?: continue

        // Process outgoing exits - these define where neighbors are relative to current
        for (exit in currentLoc.exits) {
            val neighborId = exit.locationId
            if (neighborId !in subgraphIds || neighborId in visited) continue

            val (dx, dy) = getDirectionOffset(exit.direction)
            val neighborPos = Pair(currentPos.first + dx, currentPos.second + dy)

            // Only set position if not already set (first path wins)
            if (neighborId !in positions) {
                positions[neighborId] = neighborPos
            }
            visited.add(neighborId)
            queue.add(neighborId)
        }

        // Process incoming exits - reverse the direction to find where the neighbor is
        for (loc in allLocations) {
            if (loc.id !in subgraphIds || loc.id in visited) continue
            val exitToHere = loc.exits.find { it.locationId == currentId }
            if (exitToHere != null) {
                // Neighbor has exit TO current, so neighbor is in the opposite direction
                val oppositeDir = getOppositeDirection(exitToHere.direction)
                val (dx, dy) = getDirectionOffset(oppositeDir)
                val neighborPos = Pair(currentPos.first + dx, currentPos.second + dy)

                if (loc.id !in positions) {
                    positions[loc.id] = neighborPos
                }
                visited.add(loc.id)
                queue.add(loc.id)
            }
        }
    }

    return positions
}

/**
 * Check if a subgraph can be placed at a given anchor position without coordinate conflicts.
 * Returns true if all positions are available (or occupied by locations within the subgraph itself).
 */
fun canPlaceSubgraphAt(
    anchorX: Int,
    anchorY: Int,
    anchorAreaId: String,
    relativePositions: Map<String, Pair<Int, Int>>,
    subgraphIds: Set<String>,
    allLocations: List<Location>
): Boolean {
    for ((locId, relPos) in relativePositions) {
        val absoluteX = anchorX + relPos.first
        val absoluteY = anchorY + relPos.second

        val existingAtCoord = LocationRepository.findByCoordinates(absoluteX, absoluteY, anchorAreaId)
        if (existingAtCoord != null && existingAtCoord.id !in subgraphIds) {
            // Coordinate occupied by a location outside the subgraph
            return false
        }
    }
    return true
}

/**
 * Calculate valid directions for creating an exit from one location to another.
 */
fun validateExitDirections(
    fromLocation: Location,
    toLocation: Location,
    allLocations: List<Location>
): ValidateExitResponse {
    val fromX = fromLocation.gridX
    val fromY = fromLocation.gridY
    val fromAreaId = fromLocation.areaId ?: "overworld"

    // Source must have coordinates
    if (fromX == null || fromY == null) {
        return ValidateExitResponse(
            canCreateExit = false,
            validDirections = emptyList(),
            errorMessage = "Source location has no coordinates",
            targetHasCoordinates = toLocation.gridX != null,
            targetIsConnected = isLocationConnected(toLocation, allLocations)
        )
    }

    val toX = toLocation.gridX
    val toY = toLocation.gridY
    val toAreaId = toLocation.areaId ?: "overworld"

    // If target has coordinates
    if (toX != null && toY != null) {
        // Check if they're exactly 1 cell apart and in the same area
        val direction = getDirectionBetweenCoordinates(fromX, fromY, fromAreaId, toX, toY, toAreaId)
        if (direction != null) {
            return ValidateExitResponse(
                canCreateExit = true,
                validDirections = listOf(
                    ValidDirectionInfo(
                        direction = direction,
                        isFixed = true,
                        targetCoordinates = CoordinateInfo(toX, toY, toAreaId)
                    )
                ),
                targetHasCoordinates = true,
                targetIsConnected = true
            )
        } else {
            return ValidateExitResponse(
                canCreateExit = false,
                validDirections = emptyList(),
                errorMessage = "Target location is not adjacent (must be exactly 1 cell away in same area)",
                targetHasCoordinates = true,
                targetIsConnected = true
            )
        }
    }

    // Target has no coordinates - check which directions are available
    val validDirections = mutableListOf<ValidDirectionInfo>()
    val targetIsConnected = isLocationConnected(toLocation, allLocations)

    // Get the target's subgraph (all locations connected to it via exits)
    val subgraphIds = getConnectedSubgraph(toLocation, allLocations)

    // Calculate relative positions within the subgraph (using exit directions)
    // This handles one-way exits by traversing both outgoing and incoming exits
    val relativePositions = calculateSubgraphRelativePositions(toLocation, subgraphIds, allLocations)

    for (direction in ALL_DIRECTIONS) {
        val (dx, dy) = getDirectionOffset(direction)
        val targetX = fromX + dx
        val targetY = fromY + dy

        // Check if the entire subgraph can be placed with target at this position
        if (!canPlaceSubgraphAt(targetX, targetY, fromAreaId, relativePositions, subgraphIds, allLocations)) {
            // Subgraph would conflict with existing locations
            continue
        }

        validDirections.add(
            ValidDirectionInfo(
                direction = direction,
                isFixed = false,
                targetCoordinates = CoordinateInfo(targetX, targetY, fromAreaId)
            )
        )
    }

    return ValidateExitResponse(
        canCreateExit = validDirections.isNotEmpty(),
        validDirections = validDirections,
        errorMessage = if (validDirections.isEmpty()) "No valid directions available (subgraph conflicts)" else null,
        targetHasCoordinates = false,
        targetIsConnected = targetIsConnected
    )
}

/**
 * Assign coordinates to a location and its entire connected subgraph.
 * This is called when an exit is created from a coordinated location to an uncoordinated one.
 *
 * @param anchorLocation The location being assigned coordinates (the target of the new exit)
 * @param anchorX The X coordinate to assign to the anchor
 * @param anchorY The Y coordinate to assign to the anchor
 * @param anchorAreaId The area ID to assign to the anchor
 * @param allLocations All locations in the database
 * @param userId User ID for audit logging
 * @param userName User name for audit logging
 * @return List of location IDs that were assigned coordinates
 */
fun assignCoordinatesToSubgraph(
    anchorLocation: Location,
    anchorX: Int,
    anchorY: Int,
    anchorAreaId: String,
    allLocations: List<Location>,
    userId: String,
    userName: String
): List<String> {
    val assignedIds = mutableListOf<String>()

    // Get the subgraph and calculate relative positions
    val subgraphIds = getConnectedSubgraph(anchorLocation, allLocations)
    val relativePositions = calculateSubgraphRelativePositions(anchorLocation, subgraphIds, allLocations)

    // Assign coordinates to each location in the subgraph
    val locationById = allLocations.associateBy { it.id }
    for ((locId, relPos) in relativePositions) {
        val loc = locationById[locId] ?: continue

        // Skip if already has coordinates
        if (loc.gridX != null && loc.gridY != null) continue

        val newX = anchorX + relPos.first
        val newY = anchorY + relPos.second

        val updatedLoc = loc.copy(gridX = newX, gridY = newY, areaId = anchorAreaId)
        LocationRepository.update(updatedLoc)
        assignedIds.add(locId)

        // Create wilderness locations around this newly coordinated location
        val wildernesses = createWildernessLocationsForParent(updatedLoc, userId, userName)
        if (wildernesses.isNotEmpty()) {
            addWildernessExitsToParent(updatedLoc, wildernesses)
        }
    }

    return assignedIds
}

/**
 * Process exit changes and assign coordinates to target locations if needed.
 * Called when a location is updated with new exits.
 */
fun processExitCoordinates(
    sourceLocation: Location,
    newExits: List<Exit>,
    oldExits: List<Exit>,
    allLocations: List<Location>,
    userId: String,
    userName: String
) {
    // Find exits that were added (not in old exits)
    val oldExitIds = oldExits.map { it.locationId }.toSet()
    val addedExits = newExits.filter { it.locationId !in oldExitIds }

    // Source must have coordinates to assign them to targets
    val srcX = sourceLocation.gridX ?: return
    val srcY = sourceLocation.gridY ?: return
    val srcAreaId = sourceLocation.areaId ?: "overworld"

    val locationById = allLocations.associateBy { it.id }

    for (exit in addedExits) {
        val targetLoc = locationById[exit.locationId] ?: continue

        // Skip if target already has coordinates
        if (targetLoc.gridX != null && targetLoc.gridY != null) continue

        // Calculate target coordinates from exit direction
        val (dx, dy) = getDirectionOffset(exit.direction)
        val targetX = srcX + dx
        val targetY = srcY + dy

        // Assign coordinates to the target and its subgraph
        assignCoordinatesToSubgraph(targetLoc, targetX, targetY, srcAreaId, allLocations, userId, userName)
    }
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val appConfig = environment.config.config("app")
    val appEnv = appConfig.propertyOrNull("environment")?.getString() ?: "development"

    log.info("Starting server in $appEnv environment")

    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-User-Id")
        allowHeader("X-User-Name")
        allowNonSimpleContentTypes = true
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // Initialize database (TEST_DB_PATH takes precedence for testing)
    val dbPath = System.getProperty("TEST_DB_PATH")
        ?: appConfig.propertyOrNull("database.path")?.getString()
        ?: "data/anotherthread.db"
    DatabaseConfig.init(dbPath)
    log.info("Database initialized at $dbPath")

    // Seed character classes and abilities if empty
    ClassAbilitySeed.seedIfEmpty()

    // Seed weapon abilities for existing items
    WeaponAbilitySeed.seedWeaponAbilities()

    // Seed spell categories and example utility spells
    SpellFeatureSeed.seedIfEmpty()

    // Seed Fungus Forest content (creatures, items, loot tables, chest)
    FungusForestSeed.seedIfEmpty()

    // Initialize file directories
    val fileDir = File(System.getenv("FILE_DIR") ?: "data/files").also { it.mkdirs() }
    val imageGenDir = File(fileDir, "imageGen").also { it.mkdirs() }
    val uploadsDir = File(fileDir, "uploads").also { it.mkdirs() }
    log.info("Files directory: ${fileDir.absolutePath}")

    // Start combat tick loop
    CombatService.startTickLoop()
    log.info("Combat service started")

    // JSON for WebSocket message parsing
    val combatJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        get("/health") {
            call.respondText("OK")
        }

        // WebSocket endpoint for real-time combat
        webSocket("/combat") {
            // Get user ID from query parameter
            val userId = call.request.queryParameters["userId"]
            if (userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing userId"))
                return@webSocket
            }

            // Verify user exists
            val user = UserRepository.findById(userId)
            if (user == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User not found"))
                return@webSocket
            }

            log.info("WebSocket connection established for user $userId")
            CombatService.registerConnection(userId, this)

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            try {
                                // Parse the message type from JSON
                                when {
                                    text.contains("\"type\":\"join\"") || text.contains("JoinCombat") -> {
                                        val msg = combatJson.decodeFromString<JoinCombatMessage>(text)
                                        val locationId = user.currentLocationId
                                        if (locationId == null) {
                                            send(Frame.Text(combatJson.encodeToString(
                                                CombatErrorMessage(error = "No current location", code = "NO_LOCATION")
                                            )))
                                        } else {
                                            CombatService.startCombat(userId, locationId, msg.targetCreatureIds)
                                                .onFailure { e ->
                                                    send(Frame.Text(combatJson.encodeToString(
                                                        CombatErrorMessage(error = e.message ?: "Unknown error", code = "JOIN_FAILED")
                                                    )))
                                                }
                                        }
                                    }
                                    text.contains("\"type\":\"ability\"") || text.contains("UseAbility") -> {
                                        val msg = combatJson.decodeFromString<UseAbilityMessage>(text)
                                        CombatService.queueAbility(userId, msg.sessionId, msg.abilityId, msg.targetId)
                                            .onFailure { e ->
                                                send(Frame.Text(combatJson.encodeToString(
                                                    CombatErrorMessage(
                                                        sessionId = msg.sessionId,
                                                        error = e.message ?: "Unknown error",
                                                        code = "ABILITY_FAILED"
                                                    )
                                                )))
                                            }
                                    }
                                    text.contains("\"type\":\"flee\"") || text.contains("FleeCombat") -> {
                                        val msg = combatJson.decodeFromString<FleeCombatMessage>(text)
                                        CombatService.attemptFlee(userId, msg.sessionId)
                                    }
                                    text.contains("\"type\":\"leave\"") || text.contains("LeaveCombat") -> {
                                        val msg = combatJson.decodeFromString<LeaveCombatMessage>(text)
                                        // Handle leave
                                        log.info("User $userId left combat session ${msg.sessionId}")
                                    }
                                    else -> {
                                        log.warn("Unknown combat message type: $text")
                                    }
                                }
                            } catch (e: Exception) {
                                log.error("Error processing combat message: ${e.message}")
                                send(Frame.Text(combatJson.encodeToString(
                                    CombatErrorMessage(error = "Invalid message format", code = "PARSE_ERROR")
                                )))
                            }
                        }
                        is Frame.Close -> {
                            log.info("WebSocket closed for user $userId")
                        }
                        else -> {}
                    }
                }
            } finally {
                CombatService.unregisterConnection(userId)
                log.info("WebSocket connection closed for user $userId")
            }
        }

        // Apple App Site Association for iOS password autofill
        get("/.well-known/apple-app-site-association") {
            val aasaContent = this::class.java.classLoader
                .getResourceAsStream(".well-known/apple-app-site-association")
                ?.bufferedReader()?.readText()
            if (aasaContent != null) {
                call.respondText(aasaContent, ContentType.Application.Json)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Serve static files from subfolders
        staticFiles("/files/imageGen", imageGenDir)
        staticFiles("/files/uploads", uploadsDir)
        // Keep legacy /images path for backwards compatibility
        staticFiles("/images", imageGenDir)

        // Image generation status endpoint
        get("/image-generation/status") {
            val available = ImageGenerationService.isAvailable()
            call.respond(mapOf("available" to available))
        }

        // On-demand image generation endpoint
        post("/image-generation/generate") {
            val request = call.receive<GenerateImageRequest>()

            // Build enhanced description with feature context
            val featureContext = if (request.featureIds.isNotEmpty()) {
                val features = request.featureIds.mapNotNull { FeatureRepository.findById(it) }
                if (features.isNotEmpty()) {
                    val featureDescriptions = features.joinToString("; ") { "${it.name}: ${it.description}" }
                    "${request.description}. Features: $featureDescriptions"
                } else {
                    request.description
                }
            } else {
                request.description
            }

            ImageGenerationService.generateImage(
                entityType = request.entityType,
                entityId = request.entityId,
                description = featureContext,
                entityName = request.name
            ).onSuccess { imageUrl ->
                // Update the entity's imageUrl based on type
                when (request.entityType.lowercase()) {
                    "location" -> LocationRepository.updateImageUrl(request.entityId, imageUrl)
                    "creature" -> CreatureRepository.updateImageUrl(request.entityId, imageUrl)
                    "item" -> ItemRepository.updateImageUrl(request.entityId, imageUrl)
                    "user" -> UserRepository.updateImageUrl(request.entityId, imageUrl)
                }
                call.respond(GenerateImageResponse(imageUrl = imageUrl))
            }.onFailure { error ->
                log.error("Failed to generate image: ${error.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to (error.message ?: "Failed to generate image")
                ))
            }
        }

        // File upload routes (admin only)
        route("/admin/files") {
            // List all uploaded files
            get {
                val files = FileUploadService.listUploadedFiles()
                call.respond(files.map {
                    UploadedFileResponse(
                        filename = it.filename,
                        url = it.url,
                        size = it.size,
                        lastModified = it.lastModified
                    )
                })
            }

            // Upload a file
            post("/upload") {
                val multipart = call.receiveMultipart()
                var fileUrl: String? = null
                var error: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val filename = part.originalFileName ?: "unknown"
                            val fileBytes = part.streamProvider().readBytes()

                            FileUploadService.saveUploadedFile(filename, fileBytes)
                                .onSuccess { url -> fileUrl = url }
                                .onFailure { e -> error = e.message }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (fileUrl != null) {
                    call.respond(FileUploadResponse(success = true, url = fileUrl))
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        FileUploadResponse(success = false, error = error ?: "No file provided")
                    )
                }
            }

            // Delete a file
            delete("/{filename}") {
                val filename = call.parameters["filename"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                if (FileUploadService.deleteUploadedFile(filename)) {
                    call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("deleted" to false))
                }
            }

            // Get allowed file types
            get("/allowed-types") {
                call.respond(mapOf("allowedExtensions" to FileUploadService.getAllowedExtensions()))
            }
        }

        // PDF analysis routes (LLM-based)
        route("/pdf") {
            // Analyze a PDF file
            post("/analyze") {
                val multipart = call.receiveMultipart()
                var pdfBytes: ByteArray? = null
                var analysisType: String? = null
                var areaId: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            pdfBytes = part.streamProvider().readBytes()
                        }
                        is PartData.FormItem -> {
                            when (part.name) {
                                "analysisType" -> analysisType = part.value
                                "areaId" -> areaId = part.value
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (pdfBytes == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        PdfAnalysisResult(
                            success = false,
                            analysisType = analysisType ?: "unknown",
                            error = "No PDF file provided"
                        )
                    )
                    return@post
                }

                val type = try {
                    PdfAnalysisType.valueOf(analysisType?.uppercase() ?: "MAP")
                } catch (e: Exception) {
                    PdfAnalysisType.MAP
                }

                val result = PdfService.analyzePdf(pdfBytes!!, type, areaId)
                call.respond(result)
            }

            // Extract text only (no LLM analysis)
            post("/extract-text") {
                val multipart = call.receiveMultipart()
                var pdfBytes: ByteArray? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            pdfBytes = part.streamProvider().readBytes()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (pdfBytes == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "No PDF file provided")
                    )
                    return@post
                }

                try {
                    val text = PdfService.extractText(pdfBytes!!)
                    call.respond(mapOf("text" to text))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to extract text: ${e.message}")
                    )
                }
            }

            // Get supported analysis types
            get("/analysis-types") {
                call.respond(mapOf(
                    "types" to PdfAnalysisType.entries.map {
                        mapOf(
                            "value" to it.name,
                            "description" to when (it) {
                                PdfAnalysisType.MAP -> "ASCII map with locations and connections"
                                PdfAnalysisType.CLASSES -> "Character classes with abilities"
                                PdfAnalysisType.ITEMS -> "Item lists with descriptions"
                                PdfAnalysisType.CREATURES -> "Creature/monster lists"
                                PdfAnalysisType.ABILITIES -> "Standalone ability lists"
                            }
                        )
                    }
                ))
            }
        }

        // Content generation routes (LLM-based)
        route("/generate") {
            get("/status") {
                val available = ContentGenerationService.isAvailable()
                call.respond(mapOf("available" to available))
            }

            post("/location") {
                val request = call.receive<GenerateLocationContentRequest>()
                ContentGenerationService.generateLocationContent(
                    exitIds = request.exitIds,
                    featureIds = request.featureIds,
                    existingName = request.existingName,
                    existingDesc = request.existingDesc
                ).onSuccess { content ->
                    call.respond(GeneratedContentResponse(
                        name = content.name,
                        description = content.description
                    ))
                }.onFailure { error ->
                    log.error("Failed to generate location content: ${error.message}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "error" to (error.message ?: "Failed to generate content")
                    ))
                }
            }

            post("/creature") {
                val request = call.receive<GenerateCreatureContentRequest>()
                ContentGenerationService.generateCreatureContent(
                    existingName = request.existingName,
                    existingDesc = request.existingDesc
                ).onSuccess { content ->
                    call.respond(GeneratedContentResponse(
                        name = content.name,
                        description = content.description
                    ))
                }.onFailure { error ->
                    log.error("Failed to generate creature content: ${error.message}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "error" to (error.message ?: "Failed to generate content")
                    ))
                }
            }

            post("/item") {
                val request = call.receive<GenerateItemContentRequest>()
                ContentGenerationService.generateItemContent(
                    existingName = request.existingName,
                    existingDesc = request.existingDesc
                ).onSuccess { content ->
                    call.respond(GeneratedContentResponse(
                        name = content.name,
                        description = content.description
                    ))
                }.onFailure { error ->
                    log.error("Failed to generate item content: ${error.message}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "error" to (error.message ?: "Failed to generate content")
                    ))
                }
            }
        }

        // Location routes
        route("/locations") {
            get {
                call.respond(LocationRepository.findAll())
            }
            post {
                val request = call.receive<CreateLocationRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existingLocations = LocationRepository.findAll()
                val exits = request.exits.map { Exit(it.locationId, it.direction) }

                // Determine coordinates for the new location
                val (gridX, gridY, areaId) = when {
                    // First location gets origin in overworld
                    existingLocations.isEmpty() -> Triple(0, 0, "overworld")

                    // If has exits to existing locations, calculate coordinates from exit direction
                    exits.isNotEmpty() -> {
                        val firstExit = exits.first()
                        val targetLocation = existingLocations.find { it.id == firstExit.locationId }
                        if (targetLocation?.gridX != null && targetLocation.gridY != null) {
                            // Calculate coordinate based on the OPPOSITE direction (new location is in that direction from target)
                            val oppositeDir = getOppositeDirection(firstExit.direction)
                            val offset = getDirectionOffset(oppositeDir)
                            if (offset != null) {
                                Triple(
                                    targetLocation.gridX + offset.first,
                                    targetLocation.gridY + offset.second,
                                    targetLocation.areaId ?: "overworld"
                                )
                            } else {
                                // Direction unknown, assign random coordinate in overworld
                                val (x, y) = findRandomUnusedCoordinate(existingLocations)
                                Triple(x, y, "overworld")
                            }
                        } else {
                            // Target has no coordinates, assign random in overworld
                            val (x, y) = findRandomUnusedCoordinate(existingLocations)
                            Triple(x, y, "overworld")
                        }
                    }

                    // No exits - assign random unused coordinate in overworld
                    else -> {
                        val (x, y) = findRandomUnusedCoordinate(existingLocations)
                        Triple(x, y, "overworld")
                    }
                }

                val location = Location(
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    creatureIds = request.creatureIds,
                    exits = exits,
                    featureIds = request.featureIds,
                    gridX = gridX,
                    gridY = gridY,
                    areaId = areaId,
                    // Set lastEditedBy/At for user-created locations
                    lastEditedBy = userId,
                    lastEditedAt = LocalDateTime.now().toString(),
                    // Default to OUTDOOR_GROUND for user-created locations
                    locationType = LocationType.OUTDOOR_GROUND
                )
                val createdLocation = LocationRepository.create(location)

                // Audit log
                AuditLogRepository.log(
                    recordId = createdLocation.id,
                    recordType = "Location",
                    recordName = createdLocation.name,
                    action = AuditAction.CREATE,
                    userId = userId,
                    userName = userName
                )

                // Auto-create wilderness locations for all 8 directions (only if this location has coordinates)
                val wildernessLocations = createWildernessLocationsForParent(createdLocation, userId, userName)

                // Update parent location with exits to the wilderness locations
                val updatedLocation = if (wildernessLocations.isNotEmpty()) {
                    addWildernessExitsToParent(createdLocation, wildernessLocations)
                } else {
                    createdLocation
                }

                // Trigger image generation in background if description is provided
                if (request.desc.isNotBlank()) {
                    application.launch {
                        ImageGenerationService.generateImage(
                            entityType = "location",
                            entityId = updatedLocation.id,
                            description = request.desc,
                            entityName = request.name
                        ).onSuccess { imageUrl ->
                            LocationRepository.updateImageUrl(updatedLocation.id, imageUrl)
                            log.info("Generated image for location ${updatedLocation.id}: $imageUrl")
                        }.onFailure { error ->
                            log.warn("Failed to generate image for location ${updatedLocation.id}: ${error.message}")
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, updatedLocation)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateLocationRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                // Get existing location to check if description changed
                val existingLocation = LocationRepository.findById(id)
                val descChanged = existingLocation?.desc != request.desc

                val location = Location(
                    id = id,
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    creatureIds = request.creatureIds,
                    exits = request.exits.map { Exit(it.locationId, it.direction) },
                    featureIds = request.featureIds,
                    imageUrl = existingLocation?.imageUrl, // Preserve existing image
                    lockedBy = existingLocation?.lockedBy, // Preserve lock status
                    gridX = existingLocation?.gridX, // Preserve coordinates
                    gridY = existingLocation?.gridY,
                    areaId = existingLocation?.areaId, // Preserve area
                    // Update lastEditedBy/At for user edits
                    lastEditedBy = userId,
                    lastEditedAt = LocalDateTime.now().toString(),
                    // Preserve location type (or default to OUTDOOR_GROUND if not set)
                    locationType = existingLocation?.locationType ?: LocationType.OUTDOOR_GROUND
                )

                if (LocationRepository.update(location)) {
                    // Audit log
                    AuditLogRepository.log(
                        recordId = location.id,
                        recordType = "Location",
                        recordName = location.name,
                        action = AuditAction.UPDATE,
                        userId = userId,
                        userName = userName
                    )

                    // Process exit changes - assign coordinates to target locations if needed
                    val oldExits = existingLocation?.exits ?: emptyList()
                    val newExits = location.exits
                    val allLocations = LocationRepository.findAll()
                    processExitCoordinates(location, newExits, oldExits, allLocations, userId, userName)

                    // Regenerate image if description changed
                    if (descChanged && request.desc.isNotBlank()) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "location",
                                entityId = id,
                                description = request.desc,
                                entityName = request.name
                            ).onSuccess { imageUrl ->
                                LocationRepository.updateImageUrl(id, imageUrl)
                                log.info("Regenerated image for location $id: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to regenerate image for location $id: ${error.message}")
                            }
                        }
                    }

                    // Return updated location with any coordinate changes
                    val updatedLocation = LocationRepository.findById(id) ?: location
                    call.respond(HttpStatusCode.OK, updatedLocation)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Validate exit creation between two locations
            post("/validate-exit") {
                val request = call.receive<ValidateExitRequest>()

                val fromLocation = LocationRepository.findById(request.fromLocationId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, "Source location not found")

                val toLocation = LocationRepository.findById(request.toLocationId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, "Target location not found")

                val allLocations = LocationRepository.findAll()
                val response = validateExitDirections(fromLocation, toLocation, allLocations)

                call.respond(HttpStatusCode.OK, response)
            }

            put("/{id}/lock") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<LockRequest>()

                val existingLocation = LocationRepository.findById(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound)

                // Toggle lock: if currently locked by this user, unlock; otherwise lock
                val newLockedBy = if (existingLocation.lockedBy == request.userId) {
                    null // Unlock
                } else {
                    request.userId // Lock
                }

                if (LocationRepository.updateLockedBy(id, newLockedBy)) {
                    val updatedLocation = LocationRepository.findById(id)
                    call.respond(HttpStatusCode.OK, updatedLocation!!)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            // Generate wilderness around a specific location
            post("/{id}/generate-wilderness") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val location = LocationRepository.findById(id)
                    ?: return@post call.respond(HttpStatusCode.NotFound)

                if (location.gridX == null || location.gridY == null) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Location has no coordinates")
                }

                val wildernessLocations = createWildernessLocationsForParent(location, userId, userName)
                if (wildernessLocations.isNotEmpty()) {
                    addWildernessExitsToParent(location, wildernessLocations)
                }

                call.respond(HttpStatusCode.OK, mapOf(
                    "created" to wildernessLocations.size,
                    "directions" to wildernessLocations.keys.map { it.name }
                ))
            }

            // Generate wilderness around all locations with coordinates
            post("/generate-all-wilderness") {
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val allLocations = LocationRepository.findAll()
                var totalCreated = 0

                for (location in allLocations) {
                    if (location.gridX == null || location.gridY == null) continue
                    if (location.name == "Wilderness") continue // Don't generate wilderness around wilderness

                    val wildernessLocations = createWildernessLocationsForParent(location, userId, userName)
                    if (wildernessLocations.isNotEmpty()) {
                        addWildernessExitsToParent(location, wildernessLocations)
                        totalCreated += wildernessLocations.size
                    }
                }

                call.respond(HttpStatusCode.OK, mapOf("totalCreated" to totalCreated))
            }

            // Backfill existing wilderness locations with features from adjacent non-wilderness locations
            post("/backfill-wilderness-features") {
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val allLocations = LocationRepository.findAll()
                var totalUpdated = 0

                // Find all wilderness locations
                val wildernessLocations = allLocations.filter { it.name == "Wilderness" }

                for (wilderness in wildernessLocations) {
                    val wx = wilderness.gridX ?: continue
                    val wy = wilderness.gridY ?: continue
                    val wAreaId = wilderness.areaId ?: "overworld"

                    // Find all adjacent non-wilderness locations
                    val adjacentFeatureIds = mutableSetOf<String>()
                    for (direction in ALL_DIRECTIONS) {
                        val (dx, dy) = getDirectionOffset(direction)
                        val adjacentLoc = LocationRepository.findByCoordinates(wx + dx, wy + dy, wAreaId)
                        if (adjacentLoc != null && adjacentLoc.name != "Wilderness") {
                            adjacentFeatureIds.addAll(adjacentLoc.featureIds)
                        }
                    }

                    // Update wilderness with combined features from all adjacent locations
                    if (adjacentFeatureIds.isNotEmpty() && adjacentFeatureIds != wilderness.featureIds.toSet()) {
                        val updatedWilderness = wilderness.copy(
                            featureIds = adjacentFeatureIds.toList(),
                            desc = generateWildernessDescription(adjacentFeatureIds.toList(), "")
                        )
                        LocationRepository.update(updatedWilderness)
                        totalUpdated++

                        // Audit log
                        AuditLogRepository.log(
                            recordId = wilderness.id,
                            recordType = "Location",
                            recordName = "Wilderness (backfill features)",
                            action = AuditAction.UPDATE,
                            userId = userId,
                            userName = userName
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, mapOf("totalUpdated" to totalUpdated))
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existingLocation = LocationRepository.findById(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

                // Remove this location from other locations' exitIds
                LocationRepository.removeExitIdFromAll(id)

                if (LocationRepository.delete(id)) {
                    // Audit log
                    AuditLogRepository.log(
                        recordId = id,
                        recordType = "Location",
                        recordName = existingLocation.name,
                        action = AuditAction.DELETE,
                        userId = userId,
                        userName = userName
                    )
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            // Terrain overrides for a location (admin only)
            route("/{locationId}/terrain-overrides") {
                get {
                    val locationId = call.parameters["locationId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val override = TerrainOverrideRepository.findByLocationId(locationId)
                    if (override != null) {
                        call.respond(override)
                    } else {
                        // Return empty defaults
                        call.respond(TerrainOverride(locationId = locationId, overrides = TerrainOverrides()))
                    }
                }

                put {
                    val locationId = call.parameters["locationId"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val userId = call.request.header("X-User-Id") ?: "unknown"
                    val userName = call.request.header("X-User-Name") ?: "unknown"

                    val location = LocationRepository.findById(locationId)
                        ?: return@put call.respond(HttpStatusCode.NotFound, "Location not found")

                    // Check if location is locked by someone else
                    if (location.lockedBy != null && location.lockedBy != userId) {
                        return@put call.respond(HttpStatusCode.Forbidden, "Location is locked by another user")
                    }

                    val request = call.receive<TerrainOverrides>()
                    val override = TerrainOverride(
                        locationId = locationId,
                        overrides = request,
                        updatedBy = userId,
                        updatedAt = System.currentTimeMillis()
                    )

                    TerrainOverrideRepository.upsert(override)

                    // Audit log
                    AuditLogRepository.log(
                        recordId = locationId,
                        recordType = "TerrainOverride",
                        recordName = "Terrain settings for ${location.name}",
                        action = AuditAction.UPDATE,
                        userId = userId,
                        userName = userName
                    )

                    call.respond(HttpStatusCode.OK, override)
                }

                delete {
                    val locationId = call.parameters["locationId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val userId = call.request.header("X-User-Id") ?: "unknown"
                    val userName = call.request.header("X-User-Name") ?: "unknown"

                    val location = LocationRepository.findById(locationId)
                        ?: return@delete call.respond(HttpStatusCode.NotFound, "Location not found")

                    // Check if location is locked by someone else
                    if (location.lockedBy != null && location.lockedBy != userId) {
                        return@delete call.respond(HttpStatusCode.Forbidden, "Location is locked by another user")
                    }

                    TerrainOverrideRepository.delete(locationId)

                    // Audit log
                    AuditLogRepository.log(
                        recordId = locationId,
                        recordType = "TerrainOverride",
                        recordName = "Reset terrain settings for ${location.name}",
                        action = AuditAction.DELETE,
                        userId = userId,
                        userName = userName
                    )

                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        // Creature routes
        route("/creatures") {
            get {
                call.respond(CreatureRepository.findAll())
            }

            // Get activity states for all creatures (wandering, in_combat, idle)
            get("/states") {
                val states = CombatService.getAllCreatureStates()
                call.respond(states.mapValues { it.value.name.lowercase() })
            }
            post {
                val request = call.receive<CreateCreatureRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val creature = Creature(
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    featureIds = request.featureIds,
                    maxHp = request.maxHp,
                    baseDamage = request.baseDamage,
                    abilityIds = request.abilityIds,
                    level = request.level,
                    experienceValue = request.experienceValue,
                    challengeRating = request.challengeRating,
                    isAggressive = request.isAggressive
                )
                val createdCreature = CreatureRepository.create(creature)

                // Audit log
                AuditLogRepository.log(
                    recordId = createdCreature.id,
                    recordType = "Creature",
                    recordName = createdCreature.name,
                    action = AuditAction.CREATE,
                    userId = userId,
                    userName = userName
                )

                // Trigger image generation in background
                if (request.desc.isNotBlank()) {
                    application.launch {
                        ImageGenerationService.generateImage(
                            entityType = "creature",
                            entityId = createdCreature.id,
                            description = request.desc,
                            entityName = request.name
                        ).onSuccess { imageUrl ->
                            CreatureRepository.updateImageUrl(createdCreature.id, imageUrl)
                            log.info("Generated image for creature ${createdCreature.id}: $imageUrl")
                        }.onFailure { error ->
                            log.warn("Failed to generate image for creature ${createdCreature.id}: ${error.message}")
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, createdCreature)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateCreatureRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existingCreature = CreatureRepository.findById(id)
                val descChanged = existingCreature?.desc != request.desc

                val creature = Creature(
                    id = id,
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    featureIds = request.featureIds,
                    imageUrl = existingCreature?.imageUrl,
                    lockedBy = existingCreature?.lockedBy, // Preserve lock status
                    maxHp = request.maxHp,
                    baseDamage = request.baseDamage,
                    abilityIds = request.abilityIds,
                    level = request.level,
                    experienceValue = request.experienceValue,
                    challengeRating = request.challengeRating,
                    isAggressive = request.isAggressive
                )

                if (CreatureRepository.update(creature)) {
                    // Audit log
                    AuditLogRepository.log(
                        recordId = creature.id,
                        recordType = "Creature",
                        recordName = creature.name,
                        action = AuditAction.UPDATE,
                        userId = userId,
                        userName = userName
                    )
                    if (descChanged && request.desc.isNotBlank()) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "creature",
                                entityId = id,
                                description = request.desc,
                                entityName = request.name
                            ).onSuccess { imageUrl ->
                                CreatureRepository.updateImageUrl(id, imageUrl)
                                log.info("Regenerated image for creature $id: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to regenerate image for creature $id: ${error.message}")
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, creature)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            put("/{id}/lock") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<LockRequest>()

                val existingCreature = CreatureRepository.findById(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound)

                // Toggle lock: if currently locked by this user, unlock; otherwise lock
                val newLockedBy = if (existingCreature.lockedBy == request.userId) {
                    null // Unlock
                } else {
                    request.userId // Lock
                }

                if (CreatureRepository.updateLockedBy(id, newLockedBy)) {
                    val updatedCreature = CreatureRepository.findById(id)
                    call.respond(HttpStatusCode.OK, updatedCreature!!)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existingCreature = CreatureRepository.findById(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

                // Remove this creature from all locations' creatureIds
                LocationRepository.removeCreatureIdFromAll(id)

                if (CreatureRepository.delete(id)) {
                    // Audit log
                    AuditLogRepository.log(
                        recordId = id,
                        recordType = "Creature",
                        recordName = existingCreature.name,
                        action = AuditAction.DELETE,
                        userId = userId,
                        userName = userName
                    )
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }

        // User auth routes
        route("/auth") {
            post("/register") {
                val request = call.receive<RegisterRequest>()

                // Validate input
                if (request.name.isBlank() || request.password.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(
                        success = false,
                        message = "Name and password are required"
                    ))
                    return@post
                }

                if (request.password.length < 4) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(
                        success = false,
                        message = "Password must be at least 4 characters"
                    ))
                    return@post
                }

                // Check if username already exists
                if (UserRepository.findByName(request.name) != null) {
                    call.respond(HttpStatusCode.Conflict, AuthResponse(
                        success = false,
                        message = "Username already exists"
                    ))
                    return@post
                }

                // Create user with hashed password
                val user = User(
                    name = request.name,
                    passwordHash = UserRepository.hashPassword(request.password)
                )
                val createdUser = UserRepository.create(user)

                call.respond(HttpStatusCode.Created, AuthResponse(
                    success = true,
                    message = "Registration successful",
                    user = createdUser.toResponse()
                ))
            }

            post("/login") {
                val request = call.receive<LoginRequest>()

                val user = UserRepository.findByName(request.name)
                if (user == null || !UserRepository.verifyPassword(request.password, user.passwordHash)) {
                    call.respond(HttpStatusCode.Unauthorized, AuthResponse(
                        success = false,
                        message = "Invalid username or password"
                    ))
                    return@post
                }

                // Update last active timestamp
                UserRepository.updateLastActiveAt(user.id)

                call.respond(HttpStatusCode.OK, AuthResponse(
                    success = true,
                    message = "Login successful",
                    user = user.toResponse()
                ))
            }
        }

        // User routes (authenticated)
        route("/users") {
            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = UserRepository.findById(id)
                if (user != null) {
                    call.respond(user.toResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateUserRequest>()

                val existingUser = UserRepository.findById(id)
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                val descChanged = existingUser.desc != request.desc

                val updatedUser = existingUser.copy(
                    desc = request.desc,
                    itemIds = request.itemIds,
                    featureIds = request.featureIds,
                    lastActiveAt = System.currentTimeMillis()
                )

                if (UserRepository.update(updatedUser)) {
                    // Trigger image generation if description changed
                    if (descChanged && request.desc.isNotBlank()) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "user",
                                entityId = id,
                                description = request.desc,
                                entityName = existingUser.name
                            ).onSuccess { imageUrl ->
                                UserRepository.updateImageUrl(id, imageUrl)
                                log.info("Generated image for user $id: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to generate image for user $id: ${error.message}")
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, updatedUser.toResponse())
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            put("/{id}/location") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateLocationRequest>()

                if (UserRepository.updateCurrentLocation(id, request.locationId)) {
                    // Check for aggressive creatures at the new location
                    val combatSession = request.locationId?.let { locationId ->
                        CombatService.checkAggressiveCreatures(id, locationId)
                    }

                    if (combatSession != null) {
                        // Return info about the auto-started combat
                        call.respond(HttpStatusCode.OK, mapOf(
                            "success" to true,
                            "combatStarted" to true,
                            "combatSessionId" to combatSession.id,
                            "message" to "Aggressive creatures attack!"
                        ))
                    } else {
                        call.respond(HttpStatusCode.OK, mapOf(
                            "success" to true,
                            "combatStarted" to false
                        ))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Clear or set user's character class directly
            put("/{id}/class") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateClassRequest>()

                val existingUser = UserRepository.findById(id)
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                if (UserRepository.updateCharacterClass(id, request.classId)) {
                    val updatedUser = UserRepository.findById(id)!!
                    call.respond(HttpStatusCode.OK, updatedUser.toResponse())
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            // Equip an item
            post("/{id}/equip/{itemId}") {
                val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val itemId = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                val user = UserRepository.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@post
                }

                val item = ItemRepository.findById(itemId)
                if (item == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Item not found"))
                    return@post
                }

                if (item.equipmentSlot == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Item is not equippable"))
                    return@post
                }

                if (itemId !in user.itemIds) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Item not in inventory"))
                    return@post
                }

                // Unequip any existing item in the same slot
                val existingEquipped = user.equippedItemIds.find { equippedId ->
                    ItemRepository.findById(equippedId)?.equipmentSlot == item.equipmentSlot
                }

                if (existingEquipped != null) {
                    UserRepository.unequipItem(userId, existingEquipped)
                }

                if (UserRepository.equipItem(userId, itemId)) {
                    val updatedUser = UserRepository.findById(userId)!!
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "equippedItemIds" to updatedUser.equippedItemIds,
                        "unequipped" to existingEquipped
                    ))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to equip item"))
                }
            }

            // Unequip an item
            post("/{id}/unequip/{itemId}") {
                val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val itemId = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                val user = UserRepository.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@post
                }

                if (itemId !in user.equippedItemIds) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Item not equipped"))
                    return@post
                }

                if (UserRepository.unequipItem(userId, itemId)) {
                    val updatedUser = UserRepository.findById(userId)!!
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "equippedItemIds" to updatedUser.equippedItemIds
                    ))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to unequip item"))
                }
            }

            // Get identified entities for a user
            get("/{id}/identified") {
                val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val identified = IdentifiedEntityRepository.findByUser(userId)
                call.respond(mapOf(
                    "items" to identified.filter { it.entityType == "item" }.map { it.entityId },
                    "creatures" to identified.filter { it.entityType == "creature" }.map { it.entityId }
                ))
            }

            // Identify an entity (item or creature) for a user
            post("/{id}/identify") {
                val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                @Serializable
                data class IdentifyRequest(val entityId: String, val entityType: String)

                val request = call.receive<IdentifyRequest>()

                if (request.entityType !in listOf("item", "creature")) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid entity type"))
                    return@post
                }

                val user = UserRepository.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@post
                }

                val isNew = IdentifiedEntityRepository.identify(userId, request.entityId, request.entityType)
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "newlyIdentified" to isNew,
                    "entityId" to request.entityId,
                    "entityType" to request.entityType
                ))
            }

            // Get active users at a location
            get("/at-location/{locationId}") {
                val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val activeUsers = UserRepository.findActiveUsersAtLocation(locationId)
                call.respond(activeUsers.map { it.toResponse() })
            }

            // Assign class to user (either autoassign from existing or generate new)
            post("/{id}/assign-class") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                log.info("assign-class: Received request for user $id")

                val request = call.receive<AssignClassRequest>()
                log.info("assign-class: Request - generateClass=${request.generateClass}, descriptionLength=${request.characterDescription.length}")
                log.debug("assign-class: Character description: '${request.characterDescription.take(200)}...'")

                val existingUser = UserRepository.findById(id)
                if (existingUser == null) {
                    log.warn("assign-class: User $id not found")
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }
                log.info("assign-class: Found user '${existingUser.name}', currentClassId=${existingUser.characterClassId}")

                if (request.generateClass) {
                    log.info("assign-class: Starting class GENERATION mode for user $id")
                    // Mark that class generation has started
                    val genStarted = UserRepository.startClassGeneration(id)
                    log.info("assign-class: Class generation start marked: $genStarted")

                    val userWithGenStatus = UserRepository.findById(id)!!
                    log.info("assign-class: User classGenerationStartedAt=${userWithGenStatus.classGenerationStartedAt}")

                    // For class generation, run async and return immediately
                    // The client will poll for the user's characterClassId
                    application.launch {
                        try {
                            log.info("assign-class: [ASYNC] Starting class generation for user $id")
                            log.info("assign-class: [ASYNC] Description: '${request.characterDescription.take(100)}...'")

                            val generateResult = ClassGenerationService.generateNewClass(
                                characterDescription = request.characterDescription,
                                createdByUserId = id,
                                isPublic = false
                            )

                            if (generateResult.isFailure) {
                                val ex = generateResult.exceptionOrNull()
                                log.error("assign-class: [ASYNC] Class generation failed for user $id - ${ex?.javaClass?.simpleName}: ${ex?.message}", ex)
                                UserRepository.clearClassGeneration(id)
                                return@launch
                            }

                            val (newClass, abilities) = generateResult.getOrThrow()
                            log.info("assign-class: [ASYNC] Generated class '${newClass.name}' with ${abilities.size} abilities for user $id")

                            val (savedClass, savedAbilities) = ClassGenerationService.saveGeneratedClass(newClass, abilities)
                            log.info("assign-class: [ASYNC] Saved class to database with id=${savedClass.id}")

                            // updateCharacterClass also clears classGenerationStartedAt
                            val updateSuccess = UserRepository.updateCharacterClass(id, savedClass.id)
                            log.info("assign-class: [ASYNC] Updated user with class assignment: $updateSuccess")
                            log.info("assign-class: [ASYNC] Class generation COMPLETE for user $id: ${savedClass.name}")
                        } catch (e: Exception) {
                            log.error("assign-class: [ASYNC] Failed to generate class for user $id - ${e::class.simpleName}: ${e.message}", e)
                            // Clear generation status on failure
                            UserRepository.clearClassGeneration(id)
                            log.info("assign-class: [ASYNC] Cleared generation status after failure")
                        }
                    }

                    log.info("assign-class: Returning immediately with generation-in-progress response")
                    // Return immediately - class is being generated
                    call.respond(AssignClassResponse(
                        success = true,
                        user = userWithGenStatus.toResponse(),
                        assignedClass = null,
                        message = "Generating your custom class... This may take a few minutes. The page will update when complete."
                    ))
                } else {
                    log.info("assign-class: Starting class MATCHING mode for user $id")
                    // For matching, do it synchronously (it's fast)
                    try {
                        val assignedClass: CharacterClass

                        // First try LLM matching
                        log.info("assign-class: Attempting LLM class matching...")
                        val matchResult = ClassGenerationService.matchToExistingClass(request.characterDescription)
                        if (matchResult.isSuccess) {
                            val match = matchResult.getOrThrow()
                            log.info("assign-class: LLM matched to class '${match.matchedClassName}' (id=${match.matchedClassId}) with confidence ${match.confidence}")
                            log.info("assign-class: Match reasoning: ${match.reasoning}")

                            val matchedClass = CharacterClassRepository.findById(match.matchedClassId)
                            assignedClass = matchedClass ?: run {
                                log.warn("assign-class: Matched class id '${match.matchedClassId}' not found in database, using fallback")
                                // Fallback: get any public class
                                val publicClasses = CharacterClassRepository.findAll().filter { it.isPublic }
                                log.info("assign-class: Found ${publicClasses.size} public classes for fallback")
                                publicClasses.firstOrNull()
                                    ?: throw Exception("No classes available for assignment")
                            }
                            log.info("assign-class: Using class '${assignedClass.name}' (id=${assignedClass.id})")
                        } else {
                            // LLM not available - just pick a class based on description keywords
                            val ex = matchResult.exceptionOrNull()
                            log.warn("assign-class: LLM matching failed - ${ex?.javaClass?.simpleName}: ${ex?.message}")
                            log.info("assign-class: Falling back to keyword-based matching")

                            val publicClasses = CharacterClassRepository.findAll().filter { it.isPublic }
                            log.info("assign-class: Found ${publicClasses.size} public classes for keyword matching")

                            val descLower = request.characterDescription.lowercase()
                            val hasSpellKeywords = descLower.contains("magic") || descLower.contains("spell") ||
                                descLower.contains("wizard") || descLower.contains("mage") ||
                                descLower.contains("sorcerer") || descLower.contains("witch")
                            log.info("assign-class: Description has spell keywords: $hasSpellKeywords")

                            assignedClass = if (hasSpellKeywords) {
                                publicClasses.find { it.isSpellcaster } ?: publicClasses.firstOrNull()
                            } else {
                                publicClasses.find { !it.isSpellcaster } ?: publicClasses.firstOrNull()
                            } ?: throw Exception("No classes available for assignment")

                            log.info("assign-class: Keyword matching selected class '${assignedClass.name}'")
                        }

                        val updateSuccess = UserRepository.updateCharacterClass(id, assignedClass.id)
                        log.info("assign-class: Updated user class assignment: $updateSuccess")

                        val updatedUser = UserRepository.findById(id)!!
                        log.info("assign-class: Class matching COMPLETE for user $id: assigned '${assignedClass.name}'")

                        call.respond(AssignClassResponse(
                            success = true,
                            user = updatedUser.toResponse(),
                            assignedClass = assignedClass,
                            message = "Class '${assignedClass.name}' assigned based on your description"
                        ))
                    } catch (e: Exception) {
                        log.error("assign-class: Failed to assign class for user $id - ${e::class.simpleName}: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, AssignClassResponse(
                            success = false,
                            user = existingUser.toResponse(),
                            message = "Failed to assign class: ${e.message}"
                        ))
                    }
                }
            }
        }

        // Item routes
        route("/items") {
            get {
                call.respond(ItemRepository.findAll())
            }
            post {
                val request = call.receive<CreateItemRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val item = Item(
                    name = request.name,
                    desc = request.desc,
                    featureIds = request.featureIds
                )
                val createdItem = ItemRepository.create(item)

                // Audit log
                AuditLogRepository.log(
                    recordId = createdItem.id,
                    recordType = "Item",
                    recordName = createdItem.name,
                    action = AuditAction.CREATE,
                    userId = userId,
                    userName = userName
                )

                // Trigger image generation in background
                if (request.desc.isNotBlank()) {
                    application.launch {
                        ImageGenerationService.generateImage(
                            entityType = "item",
                            entityId = createdItem.id,
                            description = request.desc,
                            entityName = request.name
                        ).onSuccess { imageUrl ->
                            ItemRepository.updateImageUrl(createdItem.id, imageUrl)
                            log.info("Generated image for item ${createdItem.id}: $imageUrl")
                        }.onFailure { error ->
                            log.warn("Failed to generate image for item ${createdItem.id}: ${error.message}")
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, createdItem)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateItemRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existingItem = ItemRepository.findById(id)
                val descChanged = existingItem?.desc != request.desc

                val item = Item(
                    id = id,
                    name = request.name,
                    desc = request.desc,
                    featureIds = request.featureIds,
                    imageUrl = existingItem?.imageUrl,
                    lockedBy = existingItem?.lockedBy // Preserve lock status
                )

                if (ItemRepository.update(item)) {
                    // Audit log
                    AuditLogRepository.log(
                        recordId = item.id,
                        recordType = "Item",
                        recordName = item.name,
                        action = AuditAction.UPDATE,
                        userId = userId,
                        userName = userName
                    )
                    if (descChanged && request.desc.isNotBlank()) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "item",
                                entityId = id,
                                description = request.desc,
                                entityName = request.name
                            ).onSuccess { imageUrl ->
                                ItemRepository.updateImageUrl(id, imageUrl)
                                log.info("Regenerated image for item $id: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to regenerate image for item $id: ${error.message}")
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, item)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            put("/{id}/lock") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<LockRequest>()

                val existingItem = ItemRepository.findById(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound)

                // Toggle lock: if currently locked by this user, unlock; otherwise lock
                val newLockedBy = if (existingItem.lockedBy == request.userId) {
                    null // Unlock
                } else {
                    request.userId // Lock
                }

                if (ItemRepository.updateLockedBy(id, newLockedBy)) {
                    val updatedItem = ItemRepository.findById(id)
                    call.respond(HttpStatusCode.OK, updatedItem!!)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existingItem = ItemRepository.findById(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

                // Remove this item from all locations' itemIds
                LocationRepository.removeItemIdFromAll(id)

                if (ItemRepository.delete(id)) {
                    // Audit log
                    AuditLogRepository.log(
                        recordId = id,
                        recordType = "Item",
                        recordName = existingItem.name,
                        action = AuditAction.DELETE,
                        userId = userId,
                        userName = userName
                    )
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }

        // Chest routes
        route("/chests") {
            // Get all chests
            get {
                call.respond(ChestRepository.findAll())
            }

            // Get chest by ID
            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val chest = ChestRepository.findById(id)
                if (chest != null) {
                    call.respond(chest)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Get chests at a location (filtered by guardian defeated)
            get("/at-location/{locationId}") {
                val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id")

                val chests = ChestRepository.findByLocationId(locationId)

                // Filter to only show chests whose guardian has been defeated (if any)
                val visibleChests = if (userId != null) {
                    chests.filter { chest ->
                        if (chest.guardianCreatureId == null) {
                            true // No guardian, always visible
                        } else {
                            // Check if user has defeated the guardian via FeatureState
                            val defeatedKey = "defeated_${chest.guardianCreatureId}"
                            val featureState = FeatureStateRepository.getState(userId, defeatedKey)
                            featureState?.value == "true"
                        }
                    }
                } else {
                    chests.filter { it.guardianCreatureId == null }
                }

                call.respond(visibleChests)
            }

            // Open a chest (bash or pick_lock)
            post("/{id}/open") {
                val chestId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

                @Serializable
                data class OpenChestRequest(val method: String) // "bash" or "pick_lock"

                val request = call.receive<OpenChestRequest>()

                val chest = ChestRepository.findById(chestId)
                if (chest == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Chest not found"))
                    return@post
                }

                val user = UserRepository.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@post
                }

                // Check if guardian is defeated
                if (chest.guardianCreatureId != null) {
                    val defeatedKey = "defeated_${chest.guardianCreatureId}"
                    val featureState = FeatureStateRepository.getState(userId, defeatedKey)
                    if (featureState?.value != "true") {
                        call.respond(HttpStatusCode.Forbidden, mapOf(
                            "error" to "The guardian still protects this chest"
                        ))
                        return@post
                    }
                }

                // Check if already opened by this user
                val openedKey = "opened_chest_${chestId}"
                val openedState = FeatureStateRepository.getState(userId, openedKey)
                if (openedState?.value == "true") {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Chest already opened"))
                    return@post
                }

                // Check class for method validity
                val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
                val archetype = characterClass?.name?.lowercase() ?: ""

                val canBash = archetype in listOf("warrior", "berserker", "paladin", "fighter", "knight")
                val canPickLock = archetype in listOf("rogue", "assassin", "bard", "thief", "scoundrel")

                when (request.method) {
                    "bash" -> if (!canBash) {
                        call.respond(HttpStatusCode.Forbidden, mapOf(
                            "error" to "Only martial classes can bash chests"
                        ))
                        return@post
                    }
                    "pick_lock" -> if (!canPickLock) {
                        call.respond(HttpStatusCode.Forbidden, mapOf(
                            "error" to "Only scoundrel classes can pick locks"
                        ))
                        return@post
                    }
                    else -> {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid method"))
                        return@post
                    }
                }

                // Roll for success
                val difficulty = when (request.method) {
                    "bash" -> chest.bashDifficulty
                    "pick_lock" -> chest.lockDifficulty
                    else -> 5
                }
                val successChance = 1.0f / difficulty
                val success = kotlin.random.Random.nextFloat() < successChance

                if (!success) {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to false,
                        "message" to "You failed to open the chest!"
                    ))
                    return@post
                }

                // Success! Mark chest as opened and distribute loot
                FeatureStateRepository.setState(userId, openedKey, "true")

                var goldEarned = chest.goldAmount
                val itemsEarned = mutableListOf<Item>()

                // Roll loot table
                chest.lootTableId?.let { lootTableId ->
                    val lootTable = LootTableRepository.findById(lootTableId)
                    lootTable?.entries?.forEach { entry ->
                        if (kotlin.random.Random.nextFloat() < entry.chance) {
                            val qty = if (entry.maxQty > entry.minQty) {
                                kotlin.random.Random.nextInt(entry.minQty, entry.maxQty + 1)
                            } else entry.minQty

                            repeat(qty) {
                                ItemRepository.findById(entry.itemId)?.let { item ->
                                    itemsEarned.add(item)
                                }
                            }
                        }
                    }
                }

                // Award loot to user
                if (goldEarned > 0) {
                    UserRepository.addGold(userId, goldEarned)
                }
                if (itemsEarned.isNotEmpty()) {
                    UserRepository.addItems(userId, itemsEarned.map { it.id })
                }

                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "message" to "You opened the chest!",
                    "goldEarned" to goldEarned,
                    "itemsEarned" to itemsEarned.map { it.name }
                ))
            }

            // Get available actions for a chest (based on user class)
            get("/{id}/actions") {
                val chestId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val chest = ChestRepository.findById(chestId)
                if (chest == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                val user = UserRepository.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                // Check if already opened
                val openedKey = "opened_chest_${chestId}"
                val openedState = FeatureStateRepository.getState(userId, openedKey)
                if (openedState?.value == "true") {
                    call.respond(mapOf("actions" to emptyList<String>(), "opened" to true))
                    return@get
                }

                val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
                val archetype = characterClass?.name?.lowercase() ?: ""

                val actions = mutableListOf<String>()
                if (archetype in listOf("warrior", "berserker", "paladin", "fighter", "knight")) {
                    actions.add("bash")
                }
                if (archetype in listOf("rogue", "assassin", "bard", "thief", "scoundrel")) {
                    actions.add("pick_lock")
                }

                call.respond(mapOf("actions" to actions, "opened" to false))
            }
        }

        // Feature Category routes
        route("/feature-categories") {
            get {
                call.respond(FeatureCategoryRepository.findAll())
            }
            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val category = FeatureCategoryRepository.findById(id)
                if (category != null) {
                    call.respond(category)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            post {
                val request = call.receive<CreateFeatureCategoryRequest>()
                val category = FeatureCategory(
                    name = request.name,
                    description = request.description
                )
                val created = FeatureCategoryRepository.create(category)
                call.respond(HttpStatusCode.Created, created)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateFeatureCategoryRequest>()
                val category = FeatureCategory(
                    id = id,
                    name = request.name,
                    description = request.description
                )
                if (FeatureCategoryRepository.update(category)) {
                    call.respond(HttpStatusCode.OK, category)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // Feature routes
        route("/features") {
            get {
                call.respond(FeatureRepository.findAll())
            }
            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val feature = FeatureRepository.findById(id)
                if (feature != null) {
                    call.respond(feature)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            get("/by-category/{categoryId}") {
                val categoryId = call.parameters["categoryId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(FeatureRepository.findByCategoryId(categoryId))
            }
            post {
                val request = call.receive<CreateFeatureRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val feature = if (request.id != null) {
                    Feature(
                        id = request.id,
                        name = request.name,
                        featureCategoryId = request.featureCategoryId,
                        description = request.description,
                        data = request.data
                    )
                } else {
                    Feature(
                        name = request.name,
                        featureCategoryId = request.featureCategoryId,
                        description = request.description,
                        data = request.data
                    )
                }
                val created = FeatureRepository.create(feature)

                // Audit log
                AuditLogRepository.log(
                    recordId = created.id,
                    recordType = "Feature",
                    recordName = created.name,
                    action = AuditAction.CREATE,
                    userId = userId,
                    userName = userName
                )

                call.respond(HttpStatusCode.Created, created)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateFeatureRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val feature = Feature(
                    id = id,
                    name = request.name,
                    featureCategoryId = request.featureCategoryId,
                    description = request.description,
                    data = request.data
                )
                if (FeatureRepository.update(feature)) {
                    // Audit log
                    AuditLogRepository.log(
                        recordId = feature.id,
                        recordType = "Feature",
                        recordName = feature.name,
                        action = AuditAction.UPDATE,
                        userId = userId,
                        userName = userName
                    )
                    call.respond(HttpStatusCode.OK, feature)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // Audit log routes
        route("/audit-logs") {
            get {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
                call.respond(AuditLogRepository.findAll(limit, offset))
            }
            get("/by-record/{recordId}") {
                val recordId = call.parameters["recordId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(AuditLogRepository.findByRecordId(recordId))
            }
            get("/by-type/{recordType}") {
                val recordType = call.parameters["recordType"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                call.respond(AuditLogRepository.findByRecordType(recordType, limit))
            }
            get("/by-user/{userId}") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                call.respond(AuditLogRepository.findByUserId(userId, limit))
            }
        }

        // Fungus Forest admin routes
        route("/admin/fungus-forest") {
            // Trigger image generation for all Fungus Forest entities
            post("/generate-images") {
                application.launch {
                    FungusForestSeed.generateMissingImages()
                }
                call.respond(HttpStatusCode.Accepted, mapOf(
                    "message" to "Image generation started for Fungus Forest entities",
                    "note" to "Check server logs for progress"
                ))
            }

            // Generate images for Fungus Forest locations
            post("/generate-location-images") {
                val prompts = FungusForestSeed.locationImagePrompts
                var count = 0

                for ((locationId, prompt) in prompts) {
                    val location = LocationRepository.findById(locationId)
                    if (location != null && location.imageUrl == null) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "location",
                                entityId = locationId,
                                description = prompt,
                                entityName = location.name
                            ).onSuccess { imageUrl ->
                                LocationRepository.updateImageUrl(locationId, imageUrl)
                                log.info("Generated image for location ${location.name}: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to generate image for location ${location.name}: ${error.message}")
                            }
                        }
                        count++
                    }
                }

                call.respond(HttpStatusCode.Accepted, mapOf(
                    "message" to "Started generating images for $count locations",
                    "note" to "Check server logs for progress"
                ))
            }
        }

        // Service health and management routes (admin only)
        route("/admin/services") {
            // Get health status of local services (Ollama, Stable Diffusion)
            get("/health/local") {
                val services = mutableListOf<ServiceStatus>()

                // Check Ollama (localhost:11434)
                services.add(ServiceStatus(
                    name = "ollama",
                    displayName = "Ollama LLM",
                    healthy = checkServiceHealth("http://localhost:11434/api/tags"),
                    url = "http://localhost:11434"
                ))

                // Check Stable Diffusion (localhost:7860)
                services.add(ServiceStatus(
                    name = "stable_diffusion",
                    displayName = "Stable Diffusion",
                    healthy = checkServiceHealth("http://localhost:7860/"),
                    url = "http://localhost:7860"
                ))

                call.respond(services)
            }

            // Get health status of Cloudflare tunnel endpoints
            get("/health/cloudflare") {
                val services = mutableListOf<ServiceStatus>()

                // Check Cloudflare Frontend (anotherthread.ez2bgood.com)
                services.add(ServiceStatus(
                    name = "cloudflare_frontend",
                    displayName = "Cloudflare Frontend",
                    healthy = checkServiceHealth("https://anotherthread.ez2bgood.com/", 5000),
                    url = "https://anotherthread.ez2bgood.com"
                ))

                // Check Cloudflare Backend (api.ez2bgood.com)
                services.add(ServiceStatus(
                    name = "cloudflare_backend",
                    displayName = "Cloudflare Backend",
                    healthy = checkServiceHealth("https://api.ez2bgood.com/health", 5000),
                    url = "https://api.ez2bgood.com"
                ))

                call.respond(services)
            }

            // Control a service (start/stop/restart)
            post("/{serviceName}/control") {
                val serviceName = call.parameters["serviceName"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<ServiceActionRequest>()

                val result = when (serviceName) {
                    "ollama" -> controlOllama(request.action)
                    "stable_diffusion" -> controlStableDiffusion(request.action)
                    "cloudflare" -> controlCloudflare(request.action)
                    else -> ServiceActionResponse(false, "Unknown service: $serviceName")
                }

                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, result)
            }

            // Purge Cloudflare cache
            post("/cloudflare/purge-cache") {
                val zoneId = environment.config.propertyOrNull("app.cloudflare.zoneId")?.getString()
                val apiToken = environment.config.propertyOrNull("app.cloudflare.apiToken")?.getString()

                if (zoneId.isNullOrBlank() || apiToken.isNullOrBlank()) {
                    call.respond(HttpStatusCode.InternalServerError, ServiceActionResponse(false, "Cloudflare credentials not configured"))
                    return@post
                }

                try {
                    val url = URL("https://api.cloudflare.com/client/v4/zones/$zoneId/purge_cache")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Authorization", "Bearer $apiToken")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    connection.outputStream.use { os ->
                        os.write("""{"purge_everything":true}""".toByteArray())
                    }

                    val responseCode = connection.responseCode
                    val responseBody = if (responseCode in 200..299) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    }
                    connection.disconnect()

                    if (responseCode in 200..299) {
                        call.respond(HttpStatusCode.OK, ServiceActionResponse(true, "Cache purged successfully"))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, ServiceActionResponse(false, "Cloudflare API error: $responseBody"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ServiceActionResponse(false, "Failed to purge cache: ${e.message}"))
                }
            }
        }

        // Admin migration endpoint - assign coordinates to locations without them
        post("/admin/migrate/assign-coordinates") {
            val allLocations = LocationRepository.findAll()
            val locationsWithoutCoords = allLocations.filter { it.gridX == null || it.gridY == null }

            if (locationsWithoutCoords.isEmpty()) {
                call.respondText(
                    """{"message":"All locations already have coordinates","updated":0,"details":[]}""",
                    ContentType.Application.Json
                )
                return@post
            }

            val updated = mutableListOf<String>()
            var currentLocations = allLocations

            for (location in locationsWithoutCoords) {
                val (newX, newY) = findRandomUnusedCoordinate(currentLocations)
                val updatedLocation = location.copy(gridX = newX, gridY = newY, areaId = "overworld")
                LocationRepository.update(updatedLocation)
                updated.add("${location.name} -> ($newX, $newY)")

                // Update our local list so next iteration sees the new coordinate as used
                currentLocations = currentLocations.map {
                    if (it.id == location.id) updatedLocation else it
                }
            }

            val detailsJson = updated.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
            call.respondText(
                """{"message":"Assigned coordinates to ${updated.size} locations","updated":${updated.size},"details":[$detailsJson]}""",
                ContentType.Application.Json
            )
        }

        // Database backup routes
        route("/admin/database") {
            // Create a backup of the database
            post("/backup") {
                try {
                    val backupPath = createDatabaseBackup()
                    call.respondText(
                        """{"success":true,"message":"Backup created successfully","path":"$backupPath"}""",
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"success":false,"message":"Backup failed: ${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // List available backups
            get("/backups") {
                try {
                    val backups = listDatabaseBackups()
                    val backupsJson = backups.joinToString(",") {
                        """{"filename":"${it.name}","size":${it.length()},"modified":${it.lastModified()}}"""
                    }
                    call.respondText(
                        """{"success":true,"backups":[$backupsJson]}""",
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"success":false,"message":"Failed to list backups: ${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Restore from a backup (creates a backup first)
            post("/restore/{filename}") {
                val filename = call.parameters["filename"]
                    ?: return@post call.respondText(
                        """{"success":false,"message":"Filename required"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )

                try {
                    // First create a backup of the current state
                    val preRestoreBackup = createDatabaseBackup("pre-restore")

                    // Then restore from the specified backup
                    restoreDatabaseFromBackup(filename)

                    call.respondText(
                        """{"success":true,"message":"Database restored successfully","preRestoreBackup":"$preRestoreBackup"}""",
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"success":false,"message":"Restore failed: ${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Delete a backup
            delete("/backup/{filename}") {
                val filename = call.parameters["filename"]
                    ?: return@delete call.respondText(
                        """{"success":false,"message":"Filename required"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )

                try {
                    deleteDatabaseBackup(filename)
                    call.respondText(
                        """{"success":true,"message":"Backup deleted successfully"}""",
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"success":false,"message":"Delete failed: ${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Data integrity check
            get("/data-integrity") {
                try {
                    val allLocations = LocationRepository.findAll()
                    val issues = mutableListOf<IntegrityIssue>()
                    val locationMap = allLocations.associateBy { it.id }

                    // Check for duplicate coordinates (within the same area)
                    val coordMap = mutableMapOf<Triple<Int, Int, String>, MutableList<Location>>()
                    allLocations.forEach { loc ->
                        if (loc.gridX != null && loc.gridY != null) {
                            val coord = Triple(loc.gridX, loc.gridY, loc.areaId ?: "overworld")
                            coordMap.getOrPut(coord) { mutableListOf() }.add(loc)
                        }
                    }
                    coordMap.filter { it.value.size > 1 }.forEach { (coord, locs) ->
                        locs.forEach { loc ->
                            val otherNames = locs.filter { it.id != loc.id }.map { it.name }
                            issues.add(IntegrityIssue(
                                type = "DUPLICATE_COORDS",
                                severity = "ERROR",
                                locationId = loc.id,
                                locationName = loc.name,
                                message = "Shares coordinates (${coord.first}, ${coord.second}) in area '${coord.third}' with: ${otherNames.joinToString(", ")}"
                            ))
                        }
                    }

                    // Check each location's exits
                    for (location in allLocations) {
                        for (exit in location.exits) {
                            val target = locationMap[exit.locationId]

                            // Missing target
                            if (target == null) {
                                issues.add(IntegrityIssue(
                                    type = "MISSING_TARGET",
                                    severity = "ERROR",
                                    locationId = location.id,
                                    locationName = location.name,
                                    message = "Exit ${exit.direction} points to non-existent location ID: ${exit.locationId.take(8)}..."
                                ))
                                continue
                            }

                            // Both have coordinates - validate distance and direction
                            if (location.gridX != null && location.gridY != null &&
                                target.gridX != null && target.gridY != null) {

                                val dx = target.gridX - location.gridX
                                val dy = target.gridY - location.gridY
                                val (expectedDx, expectedDy) = getDirectionOffset(exit.direction)

                                if (dx != expectedDx || dy != expectedDy) {
                                    val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                                    if (distance > 1) {
                                        issues.add(IntegrityIssue(
                                            type = "EXIT_TOO_FAR",
                                            severity = "ERROR",
                                            locationId = location.id,
                                            locationName = location.name,
                                            message = "Exit ${exit.direction} to '${target.name}' is $distance tiles away (should be 1)",
                                            relatedLocationId = target.id,
                                            relatedLocationName = target.name
                                        ))
                                    } else if (exit.direction != ExitDirection.UNKNOWN) {
                                        issues.add(IntegrityIssue(
                                            type = "DIRECTION_MISMATCH",
                                            severity = "WARNING",
                                            locationId = location.id,
                                            locationName = location.name,
                                            message = "Exit marked ${exit.direction} but target '${target.name}' is at offset ($dx, $dy)",
                                            relatedLocationId = target.id,
                                            relatedLocationName = target.name
                                        ))
                                    }
                                }

                                // Check bidirectional direction consistency (only for two-way exits)
                                val reverseExit = target.exits.find { it.locationId == location.id }
                                if (reverseExit != null &&
                                    exit.direction != ExitDirection.UNKNOWN &&
                                    reverseExit.direction != ExitDirection.UNKNOWN) {
                                    val expectedOpposite = getOppositeDirection(exit.direction)
                                    if (reverseExit.direction != expectedOpposite) {
                                        // Only report once (from the alphabetically first location)
                                        if (location.name < target.name) {
                                            issues.add(IntegrityIssue(
                                                type = "BIDIRECTIONAL_MISMATCH",
                                                severity = "WARNING",
                                                locationId = location.id,
                                                locationName = location.name,
                                                message = "Exit ${exit.direction} to '${target.name}', but return is ${reverseExit.direction} (expected $expectedOpposite)",
                                                relatedLocationId = target.id,
                                                relatedLocationName = target.name
                                            ))
                                        }
                                    }
                                }
                            }
                        }

                        // Note: Orphaned locations (no exits to/from) are allowed - content creators
                        // may create locations before connecting them with exits
                    }

                    call.respond(DataIntegrityResponse(
                        success = true,
                        totalLocations = allLocations.size,
                        issuesFound = issues.size,
                        issues = issues.sortedWith(compareBy({ it.severity }, { it.type }, { it.locationName }))
                    ))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        DataIntegrityResponse(
                            success = false,
                            totalLocations = 0,
                            issuesFound = 0,
                            issues = emptyList()
                        )
                    )
                }
            }
        }

        // Admin users routes
        route("/admin/users") {
            // Get all users with activity info
            get {
                try {
                    val allUsers = UserRepository.findAll()
                    val allLocations = LocationRepository.findAll()
                    val locationMap = allLocations.associateBy { it.id }

                    val userInfos = allUsers.map { user ->
                        AdminUserInfo(
                            id = user.id,
                            name = user.name,
                            createdAt = user.createdAt,
                            lastActiveAt = user.lastActiveAt,
                            currentLocationId = user.currentLocationId,
                            currentLocationName = user.currentLocationId?.let { locationMap[it]?.name },
                            imageUrl = user.imageUrl
                        )
                    }.sortedByDescending { it.lastActiveAt }

                    call.respond(AdminUsersResponse(
                        success = true,
                        totalUsers = userInfos.size,
                        users = userInfos
                    ))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        AdminUsersResponse(
                            success = false,
                            totalUsers = 0,
                            users = emptyList()
                        )
                    )
                }
            }

            // Delete a user by ID
            delete("/{id}") {
                val userId = call.parameters["id"]
                    ?: return@delete call.respondText(
                        """{"success":false,"message":"Missing user ID"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )

                try {
                    val deleted = UserRepository.delete(userId)
                    if (deleted) {
                        call.respondText(
                            """{"success":true,"message":"User deleted"}""",
                            ContentType.Application.Json
                        )
                    } else {
                        call.respondText(
                            """{"success":false,"message":"User not found"}""",
                            ContentType.Application.Json,
                            HttpStatusCode.NotFound
                        )
                    }
                } catch (e: Exception) {
                    call.respondText(
                        """{"success":false,"message":"Delete failed: ${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }
        }

        // Character Class routes
        route("/classes") {
            get {
                val isAdmin = call.request.header("X-Is-Admin")?.toBoolean() ?: false
                val allClasses = CharacterClassRepository.findAll()
                // Non-admins only see stock classes (createdByUserId == null)
                val visibleClasses = if (isAdmin) allClasses else allClasses.filter { it.createdByUserId == null }
                call.respond(visibleClasses)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val characterClass = CharacterClassRepository.findById(id)
                if (characterClass != null) {
                    call.respond(characterClass)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            post {
                val request = call.receive<CreateCharacterClassRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val characterClass = CharacterClass(
                    name = request.name,
                    description = request.description,
                    isSpellcaster = request.isSpellcaster,
                    hitDie = request.hitDie,
                    primaryAttribute = request.primaryAttribute,
                    imageUrl = request.imageUrl,
                    powerBudget = request.powerBudget,
                    isPublic = request.isPublic,
                    createdByUserId = request.createdByUserId
                )
                val created = CharacterClassRepository.create(characterClass)

                AuditLogRepository.log(
                    recordId = created.id,
                    recordType = "CharacterClass",
                    recordName = created.name,
                    action = AuditAction.CREATE,
                    userId = userId,
                    userName = userName
                )

                call.respond(HttpStatusCode.Created, created)
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateCharacterClassRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"
                val isAdmin = call.request.header("X-Is-Admin")?.toBoolean() ?: false

                val existing = CharacterClassRepository.findById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                // Only admin can edit locked classes
                if (existing.isLocked && !isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Class is locked"))
                    return@put
                }

                val updated = existing.copy(
                    name = request.name,
                    description = request.description,
                    isSpellcaster = request.isSpellcaster,
                    hitDie = request.hitDie,
                    primaryAttribute = request.primaryAttribute,
                    imageUrl = request.imageUrl,
                    powerBudget = request.powerBudget,
                    isPublic = request.isPublic,
                    createdByUserId = request.createdByUserId
                )
                CharacterClassRepository.update(updated)

                AuditLogRepository.log(
                    recordId = updated.id,
                    recordType = "CharacterClass",
                    recordName = updated.name,
                    action = AuditAction.UPDATE,
                    userId = userId,
                    userName = userName
                )

                call.respond(updated)
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existing = CharacterClassRepository.findById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                // Cannot delete locked classes
                if (existing.isLocked) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot delete locked class"))
                    return@delete
                }

                CharacterClassRepository.delete(id)

                AuditLogRepository.log(
                    recordId = id,
                    recordType = "CharacterClass",
                    recordName = existing.name,
                    action = AuditAction.DELETE,
                    userId = userId,
                    userName = userName
                )

                call.respond(HttpStatusCode.NoContent)
            }

            put("/{id}/lock") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val isAdmin = call.request.header("X-Is-Admin")?.toBoolean() ?: false

                if (!isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@put
                }

                val existing = CharacterClassRepository.findById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                // Toggle lock state
                val newLockState = !existing.isLocked
                CharacterClassRepository.updateLocked(id, newLockState)

                AuditLogRepository.log(
                    recordId = id,
                    recordType = "CharacterClass",
                    recordName = existing.name,
                    action = if (newLockState) AuditAction.LOCK else AuditAction.UNLOCK,
                    userId = userId,
                    userName = userName
                )

                call.respond(CharacterClassRepository.findById(id)!!)
            }
        }

        // Ability routes
        route("/abilities") {
            get {
                call.respond(AbilityRepository.findAll())
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val ability = AbilityRepository.findById(id)
                if (ability != null) {
                    call.respond(ability)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/class/{classId}") {
                val classId = call.parameters["classId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(AbilityRepository.findByClassId(classId))
            }

            post {
                val request = call.receive<CreateAbilityRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val ability = Ability(
                    name = request.name,
                    description = request.description,
                    classId = request.classId,
                    abilityType = request.abilityType,
                    targetType = request.targetType,
                    range = request.range,
                    cooldownType = request.cooldownType,
                    cooldownRounds = request.cooldownRounds,
                    effects = request.effects,
                    imageUrl = request.imageUrl,
                    baseDamage = request.baseDamage,
                    durationRounds = request.durationRounds
                )
                val created = AbilityRepository.create(ability)

                AuditLogRepository.log(
                    recordId = created.id,
                    recordType = "Ability",
                    recordName = created.name,
                    action = AuditAction.CREATE,
                    userId = userId,
                    userName = userName
                )

                call.respond(HttpStatusCode.Created, created)
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateAbilityRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existing = AbilityRepository.findById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                val updated = existing.copy(
                    name = request.name,
                    description = request.description,
                    classId = request.classId,
                    abilityType = request.abilityType,
                    targetType = request.targetType,
                    range = request.range,
                    cooldownType = request.cooldownType,
                    cooldownRounds = request.cooldownRounds,
                    effects = request.effects,
                    imageUrl = request.imageUrl,
                    baseDamage = request.baseDamage,
                    durationRounds = request.durationRounds
                )
                AbilityRepository.update(updated)

                AuditLogRepository.log(
                    recordId = updated.id,
                    recordType = "Ability",
                    recordName = updated.name,
                    action = AuditAction.UPDATE,
                    userId = userId,
                    userName = userName
                )

                call.respond(updated)
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existing = AbilityRepository.findById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                AbilityRepository.delete(id)

                AuditLogRepository.log(
                    recordId = id,
                    recordType = "Ability",
                    recordName = existing.name,
                    action = AuditAction.DELETE,
                    userId = userId,
                    userName = userName
                )

                call.respond(HttpStatusCode.NoContent)
            }
        }

        // Spell routes (Feature-based spell system)
        route("/spells") {
            // Get all utility spells available to a user
            get("/available/{userId}") {
                val userId = call.parameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")

                val spells = SpellService.getAvailableUtilitySpells(userId)
                call.respond(spells)
            }

            // Get all spell features (for admin/browsing)
            get {
                val allFeatures = FeatureRepository.findAll()
                val spellFeatures = allFeatures.filter {
                    SpellDataParser.isSpell(it.data)
                }
                call.respond(spellFeatures)
            }

            // Cast a utility spell
            post("/cast") {
                val request = call.receive<CastSpellRequest>()

                val result = SpellService.castUtilitySpell(
                    userId = request.userId,
                    featureId = request.featureId,
                    targetParams = request.targetParams
                )

                when (result) {
                    is SpellService.CastResult.Success -> {
                        call.respond(CastSpellResponse(
                            success = true,
                            message = result.message,
                            newLocationId = result.newLocationId,
                            revealedInfo = result.revealedInfo,
                            spellState = result.spellState
                        ))
                    }
                    is SpellService.CastResult.Failure -> {
                        call.respond(HttpStatusCode.BadRequest, CastSpellResponse(
                            success = false,
                            message = result.reason
                        ))
                    }
                }
            }

            // Get spell state for a user
            get("/state/{userId}/{featureId}") {
                val userId = call.parameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")
                val featureId = call.parameters["featureId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "featureId required")

                val state = SpellService.getSpellState(userId, featureId)
                if (state != null) {
                    call.respond(state)
                } else {
                    call.respond(mapOf("message" to "No state found (spell not yet used)"))
                }
            }

            // Reset daily charges for a user (admin/debug endpoint)
            post("/reset-charges/{userId}") {
                val userId = call.parameters["userId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "userId required")

                SpellService.resetDailyCharges(userId)
                call.respond(mapOf("success" to true, "message" to "Daily charges reset for user"))
            }
        }

        // Feature state routes (for direct state management)
        route("/feature-state") {
            // Get all feature states for a user
            get("/user/{userId}") {
                val userId = call.parameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")

                val states = FeatureStateRepository.findAllByOwner(userId)
                call.respond(states)
            }

            // Get specific feature state
            get("/{ownerId}/{featureId}") {
                val ownerId = call.parameters["ownerId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "ownerId required")
                val featureId = call.parameters["featureId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "featureId required")

                val state = FeatureStateRepository.findByOwnerAndFeature(ownerId, featureId)
                if (state != null) {
                    call.respond(state)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "State not found"))
                }
            }

            // Delete feature state (admin/debug)
            delete("/{ownerId}/{featureId}") {
                val ownerId = call.parameters["ownerId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "ownerId required")
                val featureId = call.parameters["featureId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "featureId required")

                val id = FeatureState.createId(ownerId, featureId)
                val deleted = FeatureStateRepository.delete(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // Class generation and matching endpoints
        route("/class-generation") {
            // Match character description to existing class
            post("/match") {
                val request = call.receive<MatchClassRequest>()

                val result = ClassGenerationService.matchToExistingClass(request.characterDescription)
                result.onSuccess { matchResult ->
                    call.respond(matchResult)
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (error.message ?: "Matching failed")))
                }
            }

            // Generate new class from description
            post("/generate") {
                val request = call.receive<GenerateClassRequest>()
                val userId = call.request.header("X-User-Id")
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val result = ClassGenerationService.generateNewClass(
                    characterDescription = request.characterDescription,
                    createdByUserId = userId,
                    isPublic = request.isPublic
                )

                result.onSuccess { (characterClass, abilities) ->
                    // Save to database
                    val (savedClass, savedAbilities) = ClassGenerationService.saveGeneratedClass(characterClass, abilities)

                    AuditLogRepository.log(
                        recordId = savedClass.id,
                        recordType = "CharacterClass",
                        recordName = savedClass.name,
                        action = AuditAction.CREATE,
                        userId = userId ?: "unknown",
                        userName = userName
                    )

                    call.respond(HttpStatusCode.Created, mapOf(
                        "characterClass" to savedClass,
                        "abilities" to savedAbilities,
                        "totalPowerCost" to savedAbilities.sumOf { it.powerCost }
                    ))
                }.onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (error.message ?: "Generation failed")))
                }
            }

            // Get LLM availability status
            get("/status") {
                val available = ClassGenerationService.isAvailable()
                call.respond(mapOf("available" to available))
            }
        }

        // Nerf request endpoints
        route("/nerf-requests") {
            get {
                call.respond(NerfRequestRepository.findAll())
            }

            get("/pending") {
                call.respond(NerfRequestRepository.findPending())
            }

            get("/pending/count") {
                call.respond(mapOf("count" to NerfRequestRepository.countPending()))
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val nerfRequest = NerfRequestRepository.findById(id)
                if (nerfRequest != null) {
                    call.respond(nerfRequest)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/ability/{abilityId}") {
                val abilityId = call.parameters["abilityId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(NerfRequestRepository.findByAbilityId(abilityId))
            }

            // Create a new nerf request
            post {
                val request = call.receive<CreateNerfRequestRequest>()
                val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val userName = call.request.header("X-User-Name") ?: "unknown"

                // Get the ability and generate suggested changes
                val ability = AbilityRepository.findById(request.abilityId)
                if (ability == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Ability not found"))
                    return@post
                }

                // Generate suggested rebalance using LLM
                val suggestedChanges = ClassGenerationService.suggestRebalance(ability).getOrNull()

                val nerfRequest = NerfRequest(
                    abilityId = request.abilityId,
                    requestedByUserId = userId,
                    requestedByUserName = userName,
                    reason = request.reason,
                    suggestedChanges = suggestedChanges?.let {
                        kotlinx.serialization.json.Json.encodeToString(
                            kotlinx.serialization.serializer(),
                            it
                        )
                    }
                )

                val created = NerfRequestRepository.create(nerfRequest)

                AuditLogRepository.log(
                    recordId = created.id,
                    recordType = "NerfRequest",
                    recordName = "Nerf: ${ability.name}",
                    action = AuditAction.CREATE,
                    userId = userId,
                    userName = userName
                )

                call.respond(HttpStatusCode.Created, created)
            }

            // Resolve a nerf request (admin only)
            put("/{id}/resolve") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<ResolveNerfRequestRequest>()
                val userId = call.request.header("X-User-Id") ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val nerfRequest = NerfRequestRepository.findById(id)
                if (nerfRequest == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                // If applying changes
                if (request.applyChanges && request.status == "approved" && nerfRequest.suggestedChanges != null) {
                    val suggestedAbility = kotlinx.serialization.json.Json.decodeFromString<Ability>(nerfRequest.suggestedChanges)
                    val existingAbility = AbilityRepository.findById(nerfRequest.abilityId)
                    if (existingAbility != null) {
                        val updatedAbility = existingAbility.copy(
                            description = suggestedAbility.description,
                            targetType = suggestedAbility.targetType,
                            range = suggestedAbility.range,
                            cooldownType = suggestedAbility.cooldownType,
                            cooldownRounds = suggestedAbility.cooldownRounds,
                            baseDamage = suggestedAbility.baseDamage,
                            durationRounds = suggestedAbility.durationRounds,
                            effects = suggestedAbility.effects
                        ).withCalculatedCost()
                        AbilityRepository.update(updatedAbility)

                        AuditLogRepository.log(
                            recordId = existingAbility.id,
                            recordType = "Ability",
                            recordName = existingAbility.name,
                            action = AuditAction.UPDATE,
                            userId = userId,
                            userName = userName
                        )
                    }
                }

                val finalStatus = if (request.applyChanges && request.status == "approved") "applied" else request.status

                NerfRequestRepository.resolve(
                    id = id,
                    status = finalStatus,
                    resolvedByUserId = userId,
                    adminNotes = request.adminNotes
                )

                AuditLogRepository.log(
                    recordId = id,
                    recordType = "NerfRequest",
                    recordName = "Resolved: $finalStatus",
                    action = AuditAction.UPDATE,
                    userId = userId,
                    userName = userName
                )

                val updated = NerfRequestRepository.findById(id)
                call.respond(updated ?: HttpStatusCode.NotFound)
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val existing = NerfRequestRepository.findById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                NerfRequestRepository.delete(id)

                AuditLogRepository.log(
                    recordId = id,
                    recordType = "NerfRequest",
                    recordName = "Deleted nerf request",
                    action = AuditAction.DELETE,
                    userId = userId,
                    userName = userName
                )

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/**
 * Create a backup of the database file.
 * Returns the path to the backup file.
 */
fun createDatabaseBackup(prefix: String = "backup"): String {
    val dbPath = File("data/anotherthread.db")
    if (!dbPath.exists()) {
        throw IllegalStateException("Database file not found")
    }

    val backupsDir = File("data/backups")
    if (!backupsDir.exists()) {
        backupsDir.mkdirs()
    }

    val timestamp = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val backupFilename = "${prefix}_${timestamp}.db"
    val backupPath = File(backupsDir, backupFilename)

    dbPath.copyTo(backupPath, overwrite = false)

    return backupFilename
}

/**
 * List all available database backups.
 */
fun listDatabaseBackups(): List<File> {
    val backupsDir = File("data/backups")
    if (!backupsDir.exists()) {
        return emptyList()
    }

    return backupsDir.listFiles { file -> file.extension == "db" }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}

/**
 * Restore the database from a backup file.
 */
fun restoreDatabaseFromBackup(filename: String) {
    val backupFile = File("data/backups/$filename")
    if (!backupFile.exists()) {
        throw IllegalArgumentException("Backup file not found: $filename")
    }

    val dbPath = File("data/anotherthread.db")

    // Copy backup over the current database
    backupFile.copyTo(dbPath, overwrite = true)
}

/**
 * Delete a database backup file.
 */
fun deleteDatabaseBackup(filename: String) {
    val backupFile = File("data/backups/$filename")
    if (!backupFile.exists()) {
        throw IllegalArgumentException("Backup file not found: $filename")
    }

    if (!backupFile.delete()) {
        throw IllegalStateException("Failed to delete backup file")
    }
}

/**
 * Check if a service is healthy by making a simple HTTP request.
 */
fun checkServiceHealth(url: String, timeoutMs: Int = 3000): Boolean {
    return try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.connect()
        val responseCode = connection.responseCode
        connection.disconnect()
        responseCode in 200..399
    } catch (e: Exception) {
        false
    }
}

/**
 * Control Ollama service.
 */
fun controlOllama(action: String): ServiceActionResponse {
    return try {
        when (action) {
            "start" -> {
                Runtime.getRuntime().exec(arrayOf("ollama", "serve"))
                ServiceActionResponse(true, "Ollama start command sent")
            }
            "stop" -> {
                Runtime.getRuntime().exec(arrayOf("pkill", "-f", "ollama"))
                ServiceActionResponse(true, "Ollama stop command sent")
            }
            "restart" -> {
                Runtime.getRuntime().exec(arrayOf("pkill", "-f", "ollama"))
                Thread.sleep(2000)
                Runtime.getRuntime().exec(arrayOf("ollama", "serve"))
                ServiceActionResponse(true, "Ollama restart command sent")
            }
            else -> ServiceActionResponse(false, "Unknown action: $action")
        }
    } catch (e: Exception) {
        ServiceActionResponse(false, "Error controlling Ollama: ${e.message}")
    }
}

/**
 * Control Stable Diffusion service.
 */
fun controlStableDiffusion(action: String): ServiceActionResponse {
    val sdPath = System.getProperty("user.home") + "/stable-diffusion-webui-forge"
    return try {
        when (action) {
            "start" -> {
                ProcessBuilder("bash", "-c", "cd $sdPath && ./webui.sh &")
                    .redirectErrorStream(true)
                    .start()
                ServiceActionResponse(true, "Stable Diffusion start command sent")
            }
            "stop" -> {
                Runtime.getRuntime().exec(arrayOf("pkill", "-f", "webui.sh"))
                Runtime.getRuntime().exec(arrayOf("pkill", "-f", "python.*launch.py"))
                ServiceActionResponse(true, "Stable Diffusion stop command sent")
            }
            "restart" -> {
                Runtime.getRuntime().exec(arrayOf("pkill", "-f", "webui.sh"))
                Runtime.getRuntime().exec(arrayOf("pkill", "-f", "python.*launch.py"))
                Thread.sleep(3000)
                ProcessBuilder("bash", "-c", "cd $sdPath && ./webui.sh &")
                    .redirectErrorStream(true)
                    .start()
                ServiceActionResponse(true, "Stable Diffusion restart command sent")
            }
            else -> ServiceActionResponse(false, "Unknown action: $action")
        }
    } catch (e: Exception) {
        ServiceActionResponse(false, "Error controlling Stable Diffusion: ${e.message}")
    }
}

/**
 * Control Cloudflare tunnel service.
 */
fun controlCloudflare(action: String): ServiceActionResponse {
    return try {
        when (action) {
            "start" -> {
                ProcessBuilder("bash", "-c", "cloudflared tunnel run anotherthread &")
                    .redirectErrorStream(true)
                    .start()
                ServiceActionResponse(true, "Cloudflare tunnel start command sent")
            }
            "stop" -> {
                Runtime.getRuntime().exec(arrayOf("pkill", "-f", "cloudflared"))
                ServiceActionResponse(true, "Cloudflare tunnel stop command sent")
            }
            "restart" -> {
                Runtime.getRuntime().exec(arrayOf("pkill", "-f", "cloudflared"))
                Thread.sleep(2000)
                ProcessBuilder("bash", "-c", "cloudflared tunnel run anotherthread &")
                    .redirectErrorStream(true)
                    .start()
                ServiceActionResponse(true, "Cloudflare tunnel restart command sent")
            }
            else -> ServiceActionResponse(false, "Unknown action: $action")
        }
    } catch (e: Exception) {
        ServiceActionResponse(false, "Error controlling Cloudflare: ${e.message}")
    }
}
