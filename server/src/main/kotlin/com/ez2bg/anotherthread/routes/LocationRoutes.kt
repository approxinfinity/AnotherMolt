package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.*
import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Location routes for managing game locations.
 *
 * Includes:
 * - CRUD operations for locations
 * - Exit validation
 * - Lock/unlock functionality
 * - Wilderness generation
 * - Terrain overrides
 */
fun Route.locationRoutes() {
    route("/locations") {
        get {
            call.respond(LocationRepository.findAll())
        }

        // Get location by ID with puzzle-revealed secret passages
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")

            val location = LocationRepository.findById(id)
            if (location == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            // If no user, return location as-is
            if (userId == null) {
                call.respond(location)
                return@get
            }

            // Check for any puzzles at this location that reveal secret exits
            val puzzles = PuzzleRepository.findByLocationId(id)
            val additionalExits = mutableListOf<Exit>()

            for (puzzle in puzzles) {
                val progress = PuzzleRepository.getProgress(userId, puzzle.id)
                if (progress.solved) {
                    // Add revealed secret passages as exits
                    for (passage in puzzle.secretPassages) {
                        if (passage.id in progress.passagesRevealed) {
                            additionalExits.add(Exit(
                                locationId = passage.targetLocationId,
                                direction = passage.direction
                            ))
                        }
                    }
                }
            }

            // Return location with any additional revealed exits
            if (additionalExits.isNotEmpty()) {
                val locationWithSecrets = location.copy(
                    exits = location.exits + additionalExits
                )
                call.respond(locationWithSecrets)
            } else {
                call.respond(location)
            }
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
                        application.log.info("Generated image for location ${updatedLocation.id}: $imageUrl")
                    }.onFailure { error ->
                        application.log.warn("Failed to generate image for location ${updatedLocation.id}: ${error.message}")
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
                locationType = existingLocation?.locationType ?: LocationType.OUTDOOR_GROUND,
                // Preserve biome metadata
                biome = existingLocation?.biome,
                elevation = existingLocation?.elevation,
                moisture = existingLocation?.moisture,
                isRiver = existingLocation?.isRiver,
                isCoast = existingLocation?.isCoast,
                terrainFeatures = existingLocation?.terrainFeatures,
                // Mark terrain as no longer original if description was edited
                isOriginalTerrain = if (descChanged) false else existingLocation?.isOriginalTerrain
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
                            application.log.info("Regenerated image for location $id: $imageUrl")
                        }.onFailure { error ->
                            application.log.warn("Failed to regenerate image for location $id: ${error.message}")
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
}
