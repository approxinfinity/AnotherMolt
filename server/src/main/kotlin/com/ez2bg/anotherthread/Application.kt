package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import io.ktor.server.request.header
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

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
    val z: Int
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
    val featureIds: List<String> = emptyList()
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val desc: String,
    val featureIds: List<String> = emptyList()
)

@Serializable
data class CreateFeatureCategoryRequest(
    val name: String,
    val description: String
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
    val parentZ = parentLocation.gridZ ?: 0

    for (direction in ALL_DIRECTIONS) {
        val (dx, dy) = getDirectionOffset(direction)
        val targetX = parentX + dx
        val targetY = parentY + dy

        // Check if there's already a location at this coordinate
        val existingLocation = LocationRepository.findByCoordinates(targetX, targetY, parentZ)
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
            gridZ = parentZ
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
    fromX: Int, fromY: Int, fromZ: Int,
    toX: Int, toY: Int, toZ: Int
): ExitDirection? {
    // Must be on same Z level for horizontal directions
    if (fromZ != toZ) return null

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
 * Find a random unused coordinate for a new location.
 * Searches in an expanding spiral from origin, with randomization.
 */
fun findRandomUnusedCoordinate(existingLocations: List<Location>): Triple<Int, Int, Int> {
    val usedCoords = existingLocations
        .filter { it.gridX != null && it.gridY != null }
        .map { Triple(it.gridX!!, it.gridY!!, it.gridZ ?: 0) }
        .toSet()

    // If no locations exist, return origin
    if (usedCoords.isEmpty()) return Triple(0, 0, 0)

    // Search in expanding rings around the origin, picking a random unused spot
    val random = java.util.Random()
    for (radius in 1..100) {
        val candidates = mutableListOf<Triple<Int, Int, Int>>()

        // Collect all coordinates at this radius (Manhattan distance)
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                if (kotlin.math.abs(x) == radius || kotlin.math.abs(y) == radius) {
                    val coord = Triple(x, y, 0)
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
    return Triple(random.nextInt(1000) + 100, random.nextInt(1000) + 100, 0)
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
    anchorZ: Int,
    relativePositions: Map<String, Pair<Int, Int>>,
    subgraphIds: Set<String>,
    allLocations: List<Location>
): Boolean {
    for ((locId, relPos) in relativePositions) {
        val absoluteX = anchorX + relPos.first
        val absoluteY = anchorY + relPos.second

        val existingAtCoord = LocationRepository.findByCoordinates(absoluteX, absoluteY, anchorZ)
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
    val fromZ = fromLocation.gridZ ?: 0

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
    val toZ = toLocation.gridZ ?: 0

    // If target has coordinates
    if (toX != null && toY != null) {
        // Check if they're exactly 1 cell apart
        val direction = getDirectionBetweenCoordinates(fromX, fromY, fromZ, toX, toY, toZ)
        if (direction != null) {
            return ValidateExitResponse(
                canCreateExit = true,
                validDirections = listOf(
                    ValidDirectionInfo(
                        direction = direction,
                        isFixed = true,
                        targetCoordinates = CoordinateInfo(toX, toY, toZ)
                    )
                ),
                targetHasCoordinates = true,
                targetIsConnected = true
            )
        } else {
            return ValidateExitResponse(
                canCreateExit = false,
                validDirections = emptyList(),
                errorMessage = "Target location is not adjacent (must be exactly 1 cell away)",
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
        if (!canPlaceSubgraphAt(targetX, targetY, fromZ, relativePositions, subgraphIds, allLocations)) {
            // Subgraph would conflict with existing locations
            continue
        }

        validDirections.add(
            ValidDirectionInfo(
                direction = direction,
                isFixed = false,
                targetCoordinates = CoordinateInfo(targetX, targetY, fromZ)
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
 * @param anchorZ The Z coordinate to assign to the anchor
 * @param allLocations All locations in the database
 * @param userId User ID for audit logging
 * @param userName User name for audit logging
 * @return List of location IDs that were assigned coordinates
 */
fun assignCoordinatesToSubgraph(
    anchorLocation: Location,
    anchorX: Int,
    anchorY: Int,
    anchorZ: Int,
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

        val updatedLoc = loc.copy(gridX = newX, gridY = newY, gridZ = anchorZ)
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
    val srcZ = sourceLocation.gridZ ?: 0

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
        assignCoordinatesToSubgraph(targetLoc, targetX, targetY, srcZ, allLocations, userId, userName)
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

    // Initialize database (TEST_DB_PATH takes precedence for testing)
    val dbPath = System.getProperty("TEST_DB_PATH")
        ?: appConfig.propertyOrNull("database.path")?.getString()
        ?: "data/anotherthread.db"
    DatabaseConfig.init(dbPath)
    log.info("Database initialized at $dbPath")

    // Initialize file directories
    val fileDir = File(System.getenv("FILE_DIR") ?: "data/files").also { it.mkdirs() }
    val imageGenDir = File(fileDir, "imageGen").also { it.mkdirs() }
    val uploadsDir = File(fileDir, "uploads").also { it.mkdirs() }
    log.info("Files directory: ${fileDir.absolutePath}")

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        get("/health") {
            call.respondText("OK")
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
                val (gridX, gridY, gridZ) = when {
                    // First location gets origin
                    existingLocations.isEmpty() -> Triple(0, 0, 0)

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
                                    targetLocation.gridZ ?: 0
                                )
                            } else {
                                // Direction unknown, assign random coordinate
                                findRandomUnusedCoordinate(existingLocations)
                            }
                        } else {
                            // Target has no coordinates, assign random
                            findRandomUnusedCoordinate(existingLocations)
                        }
                    }

                    // No exits - assign random unused coordinate
                    else -> findRandomUnusedCoordinate(existingLocations)
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
                    gridZ = gridZ,
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
                    gridZ = existingLocation?.gridZ,
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
                    val wz = wilderness.gridZ ?: 0

                    // Find all adjacent non-wilderness locations
                    val adjacentFeatureIds = mutableSetOf<String>()
                    for (direction in ALL_DIRECTIONS) {
                        val (dx, dy) = getDirectionOffset(direction)
                        val adjacentLoc = LocationRepository.findByCoordinates(wx + dx, wy + dy, wz)
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
            post {
                val request = call.receive<CreateCreatureRequest>()
                val userId = call.request.header("X-User-Id") ?: "unknown"
                val userName = call.request.header("X-User-Name") ?: "unknown"

                val creature = Creature(
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    featureIds = request.featureIds
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
                    lockedBy = existingCreature?.lockedBy // Preserve lock status
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
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Get active users at a location
            get("/at-location/{locationId}") {
                val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val activeUsers = UserRepository.findActiveUsersAtLocation(locationId)
                call.respond(activeUsers.map { it.toResponse() })
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
                val (newX, newY, newZ) = findRandomUnusedCoordinate(currentLocations)
                val updatedLocation = location.copy(gridX = newX, gridY = newY, gridZ = newZ)
                LocationRepository.update(updatedLocation)
                updated.add("${location.name} -> ($newX, $newY, $newZ)")

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
