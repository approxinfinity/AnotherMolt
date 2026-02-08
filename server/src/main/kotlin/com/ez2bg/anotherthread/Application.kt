package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.combat.*
import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.game.CreatureRespawnService
import com.ez2bg.anotherthread.game.GameConfig
import com.ez2bg.anotherthread.game.GameTickService
import com.ez2bg.anotherthread.game.RespawnConfig
import com.ez2bg.anotherthread.routes.abilityRoutes
import com.ez2bg.anotherthread.routes.adminRoutes
import com.ez2bg.anotherthread.routes.auditLogRoutes
import com.ez2bg.anotherthread.routes.authRoutes
import com.ez2bg.anotherthread.routes.chestRoutes
import com.ez2bg.anotherthread.routes.classGenerationRoutes
import com.ez2bg.anotherthread.routes.classRoutes
import com.ez2bg.anotherthread.routes.contentGenerationRoutes
import com.ez2bg.anotherthread.routes.creatureRoutes
import com.ez2bg.anotherthread.routes.featureCategoryRoutes
import com.ez2bg.anotherthread.routes.featureRoutes
import com.ez2bg.anotherthread.routes.featureStateRoutes
import com.ez2bg.anotherthread.routes.itemRoutes
import com.ez2bg.anotherthread.routes.locationRoutes
import com.ez2bg.anotherthread.routes.nerfRequestRoutes
import com.ez2bg.anotherthread.routes.pdfRoutes
import com.ez2bg.anotherthread.routes.spellRoutes
import com.ez2bg.anotherthread.routes.userRoutes
import com.ez2bg.anotherthread.routes.shopRoutes
import com.ez2bg.anotherthread.routes.trainerRoutes
import com.ez2bg.anotherthread.routes.encounterRoutes
import com.ez2bg.anotherthread.routes.teleportRoutes
import com.ez2bg.anotherthread.routes.phasewalkRoutes
import com.ez2bg.anotherthread.routes.riftPortalRoutes
import com.ez2bg.anotherthread.routes.worldGenRoutes
import com.ez2bg.anotherthread.events.LocationEventService
import com.ez2bg.anotherthread.spell.*
import com.ez2bg.anotherthread.SimpleGoldBalancer
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
data class RecalculateResourcesResponse(
    val message: String,
    val updated: List<String>,
    val failed: List<String>
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
    val featureIds: List<String> = emptyList(),
    val abilityIds: List<String> = emptyList(),
    val equipmentType: String? = null,
    val equipmentSlot: String? = null,
    val statBonuses: StatBonuses? = null,
    val value: Int = 0
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
    val user: UserResponse? = null,
    val sessionToken: String? = null,  // For native clients (iOS/Android)
    val expiresAt: Long? = null        // Session expiration timestamp
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
        // Allow credentials (cookies) - required for session auth
        allowCredentials = true

        // When allowCredentials is true, we can't use anyHost()
        // Allow common development origins
        allowHost("localhost:8080")
        allowHost("localhost:8081")
        allowHost("localhost:12080")
        allowHost("localhost:12081")
        allowHost("localhost:12090")  // QA frontend
        allowHost("localhost:12091")  // QA backend
        allowHost("127.0.0.1:8080")
        allowHost("127.0.0.1:8081")
        allowHost("127.0.0.1:12080")
        allowHost("127.0.0.1:12081")
        allowHost("127.0.0.1:12090")
        allowHost("127.0.0.1:12091")
        allowHost("192.168.1.239:8080")  // User's local network
        allowHost("192.168.1.239:8081")
        allowHost("192.168.1.239:12080")
        allowHost("192.168.1.239:12081")
        allowHost("192.168.1.239:12090")
        allowHost("192.168.1.239:12091")

        // Production domains (QA)
        allowHost("anotherthread.ez2bgood.com", schemes = listOf("https"))
        allowHost("api.ez2bgood.com", schemes = listOf("https"))

        // Dev domains
        allowHost("anotherthread2.ez2bgood.com", schemes = listOf("https"))
        allowHost("api2.ez2bgood.com", schemes = listOf("https"))

        // Headers
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)  // For native clients
        allowHeader("X-User-Id")
        allowHeader("X-User-Name")

        // Expose headers to client
        exposeHeader(HttpHeaders.SetCookie)

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
    // Use migrations for QA/production (set via config or env var)
    val useMigrations = appConfig.propertyOrNull("database.useMigrations")?.getString()?.toBoolean()
        ?: System.getenv("USE_MIGRATIONS")?.toBoolean()
        ?: false
    DatabaseConfig.init(dbPath, useMigrations)
    log.info("Database initialized at $dbPath (migrations: $useMigrations)")

    // Seed character classes and abilities if empty
    ClassAbilitySeed.seedIfEmpty()

    // Seed universal abilities (basic attack, etc.)
    UniversalAbilitySeed.seedIfEmpty()

    // Seed weapon abilities for existing items
    WeaponAbilitySeed.seedWeaponAbilities()

    // Seed spell categories and example utility spells
    SpellFeatureSeed.seedIfEmpty()

    // Ensure existing users have starting gold
    val usersGivenGold = UserRepository.ensureMinimumGold(100)
    if (usersGivenGold > 0) {
        log.info("Gave 100g to $usersGivenGold existing users who had less")
    }

    // Ensure existing users have a starting location (fallback for users with null location)
    val defaultStartingLocationId = GameConfigRepository.getDefaultStartingLocationId()
    val usersGivenLocation = UserRepository.ensureStartingLocation(defaultStartingLocationId)
    if (usersGivenLocation > 0) {
        log.info("Assigned starting location to $usersGivenLocation users who had none")
    }

    // Seed Fungus Forest content (creatures, items, loot tables, chest)
    SimpleGoldBalancer.addMissingGoldDrops()
    FungusForestSeed.seedIfEmpty()
    TunDuLacSeed.seedIfEmpty()
    TrainerSeed.seedIfEmpty()  // Trainer NPCs + ensures existing users have universal abilities
    ForestShopSeed.seedIfEmpty()
    WayfarerStaveSeed.seedIfEmpty()
    GoodmanGearSeed.seedIfEmpty()
    ClassicFantasySeed.seedIfEmpty()
    UndeadCryptSeed.seedIfEmpty()
    ElementalChaosSeed.seedIfEmpty()
    ClassicDungeonSeed.seedIfEmpty()

    // Seed dungeon location modules (adventure modules)
    UndeadCryptLocationsSeed.seedIfEmpty()
    GoblinWarrenLocationsSeed.seedIfEmpty()
    ClassicDungeonLocationsSeed.seedIfEmpty()

    // Auto-balance ability costs on startup
    val abilityCostBalancer = AbilityCostBalancer()
    abilityCostBalancer.balanceAbilityCosts()

    // Initialize file directories
    val fileDir = File(System.getenv("FILE_DIR") ?: "data/files").also { it.mkdirs() }
    val imageGenDir = File(fileDir, "imageGen").also { it.mkdirs() }
    val uploadsDir = File(fileDir, "uploads").also { it.mkdirs() }
    log.info("Files directory: ${fileDir.absolutePath}")

    // Initialize game config (loads defaults if not present, caches values)
    GameConfig.initialize()
    log.info("Game config initialized")

    // Initialize creature respawn quotas before starting the tick loop
    CreatureRespawnService.initializeQuotas()

    // Start global game tick loop (handles combat, regen, creature wandering, respawns)
    GameTickService.startTickLoop(CombatService)
    log.info("Game tick service started")

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
            LocationEventService.registerConnection(userId, this)
            // Track user's current location for location events
            user.currentLocationId?.let { locationId ->
                LocationEventService.updatePlayerLocation(userId, locationId)
                // Broadcast player presence to others at this location
                // This handles the case where a player reconnects (session restore)
                val location = LocationRepository.findById(locationId)
                if (location != null) {
                    launch {
                        LocationEventService.broadcastPlayerEntered(location, userId, user.name)
                    }
                }
            }
            // Update last active timestamp on WebSocket connect
            UserRepository.updateLastActiveAt(userId)

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
                                        // IMPORTANT: Fetch fresh user data to get current location
                                        // The user may have moved since the WebSocket connected
                                        val freshUser = UserRepository.findById(userId)
                                        val locationId = freshUser?.currentLocationId
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
                                log.error("Error processing combat message: ${e.message}", e)
                                log.error("Message content was: ${text.take(500)}")
                                send(Frame.Text(combatJson.encodeToString(
                                    CombatErrorMessage(error = "Invalid message format: ${e.message}", code = "PARSE_ERROR")
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
                // Broadcast player left to others at their location before unregistering
                val disconnectingUser = UserRepository.findById(userId)
                val locationId = disconnectingUser?.currentLocationId
                if (locationId != null && disconnectingUser != null) {
                    val location = LocationRepository.findById(locationId)
                    if (location != null) {
                        kotlinx.coroutines.runBlocking {
                            LocationEventService.broadcastPlayerLeft(location, userId, disconnectingUser.name)
                        }
                    }
                }
                CombatService.unregisterConnection(userId)
                LocationEventService.unregisterConnection(userId)
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

        // Admin routes
        adminRoutes()

        // PDF analysis routes (LLM-based)
        pdfRoutes()

        // World generation routes
        worldGenRoutes()

        // Content generation routes (LLM-based)
        contentGenerationRoutes()

        // Location routes
        locationRoutes()

        // Creature routes
        creatureRoutes()

        // User auth routes
        authRoutes()

        // User routes (authenticated)
        userRoutes()

        // Item routes
        itemRoutes()

        // Shop routes (buy items, rest at inn)
        shopRoutes()

        // Trainer routes (learn abilities from NPCs)
        trainerRoutes()

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

        // Character Class and Ability routes
        classRoutes()
        abilityRoutes()

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

        // Class generation and nerf request routes
        classGenerationRoutes()
        nerfRequestRoutes()
        encounterRoutes()
        teleportRoutes()
        phasewalkRoutes()
        riftPortalRoutes()
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
