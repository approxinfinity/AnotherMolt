package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.*
import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val log = org.slf4j.LoggerFactory.getLogger("AdminRoutes")

/**
 * Admin routes for file management, service control, database operations, and user management.
 */
fun Route.adminRoutes() {
    // Recalculate max resources for all users based on their stats, level, and class
    post("/admin/recalculate-resources") {
        val allUsers = UserRepository.findAll()
        val updated = mutableListOf<String>()
        val failed = mutableListOf<String>()

        for (user in allUsers) {
            val success = UserRepository.recalculateMaxResources(user.id, restoreToFull = true)
            if (success) {
                updated.add(user.name)
                log.info("Recalculated resources for user: ${user.name}")
            } else {
                failed.add(user.name)
            }
        }

        call.respond(RecalculateResourcesResponse(
            message = "Recalculated resources for ${updated.size} users",
            updated = updated,
            failed = failed
        ))
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

    // Classic Fantasy admin routes (D&D-inspired content)
    route("/admin/classic-fantasy") {
        // Trigger image generation for all Classic Fantasy entities
        post("/generate-images") {
            application.launch {
                ClassicFantasySeed.generateMissingImages()
            }
            call.respond(HttpStatusCode.Accepted, mapOf(
                "message" to "Image generation started for Classic Fantasy entities",
                "note" to "Check server logs for progress"
            ))
        }
    }

    // Undead Crypt admin routes (D&D-inspired undead content)
    route("/admin/undead-crypt") {
        // Trigger image generation for all Undead Crypt entities
        post("/generate-images") {
            application.launch {
                UndeadCryptSeed.generateMissingImages()
            }
            call.respond(HttpStatusCode.Accepted, mapOf(
                "message" to "Image generation started for Undead Crypt entities",
                "note" to "Check server logs for progress"
            ))
        }
    }

    // Elemental Chaos admin routes (D&D-inspired elemental content)
    route("/admin/elemental-chaos") {
        // Trigger image generation for all Elemental Chaos entities
        post("/generate-images") {
            application.launch {
                ElementalChaosSeed.generateMissingImages()
            }
            call.respond(HttpStatusCode.Accepted, mapOf(
                "message" to "Image generation started for Elemental Chaos entities",
                "note" to "Check server logs for progress"
            ))
        }
    }

    // Classic Dungeon admin routes (iconic D&D monsters)
    route("/admin/classic-dungeon") {
        // Trigger image generation for all Classic Dungeon entities
        post("/generate-images") {
            application.launch {
                ClassicDungeonSeed.generateMissingImages()
            }
            call.respond(HttpStatusCode.Accepted, mapOf(
                "message" to "Image generation started for Classic Dungeon entities",
                "note" to "Check server logs for progress"
            ))
        }
    }

    // Generic image backfill - generate images for all entities missing them
    post("/admin/backfill-images") {
        application.launch {
            log.info("Starting image backfill for all entity types...")

            // Users
            val users = UserRepository.findAll().filter { it.imageUrl == null }
            log.info("Backfill: ${users.size} users missing images")
            for (user in users) {
                ImageGenerationService.generateImage(
                    entityType = "user",
                    entityId = user.id,
                    description = user.desc.ifBlank { "Fantasy adventurer named ${user.name}" },
                    entityName = user.name
                ).onSuccess { imageUrl ->
                    UserRepository.updateImageUrl(user.id, imageUrl)
                    log.info("Backfill: Generated image for user ${user.name}")
                }.onFailure { error ->
                    log.warn("Backfill: Failed to generate image for user ${user.name}: ${error.message}")
                }
            }

            // Creatures
            val creatures = CreatureRepository.findAll().filter { it.imageUrl == null }
            log.info("Backfill: ${creatures.size} creatures missing images")
            for (creature in creatures) {
                ImageGenerationService.generateImage(
                    entityType = "creature",
                    entityId = creature.id,
                    description = creature.desc,
                    entityName = creature.name
                ).onSuccess { imageUrl ->
                    CreatureRepository.updateImageUrl(creature.id, imageUrl)
                    log.info("Backfill: Generated image for creature ${creature.name}")
                }.onFailure { error ->
                    log.warn("Backfill: Failed to generate image for creature ${creature.name}: ${error.message}")
                }
            }

            // Items
            val items = ItemRepository.findAll().filter { it.imageUrl == null }
            log.info("Backfill: ${items.size} items missing images")
            for (item in items) {
                ImageGenerationService.generateImage(
                    entityType = "item",
                    entityId = item.id,
                    description = item.desc,
                    entityName = item.name
                ).onSuccess { imageUrl ->
                    ItemRepository.updateImageUrl(item.id, imageUrl)
                    log.info("Backfill: Generated image for item ${item.name}")
                }.onFailure { error ->
                    log.warn("Backfill: Failed to generate image for item ${item.name}: ${error.message}")
                }
            }

            // Locations - skip biome-generated wilderness (has biome data but not hand-crafted)
            val locations = LocationRepository.findAll().filter { loc ->
                loc.imageUrl == null && loc.biome == null // Only non-biome locations
            }
            log.info("Backfill: ${locations.size} non-biome locations missing images")
            for (location in locations) {
                ImageGenerationService.generateImage(
                    entityType = "location",
                    entityId = location.id,
                    description = location.desc,
                    entityName = location.name
                ).onSuccess { imageUrl ->
                    LocationRepository.updateImageUrl(location.id, imageUrl)
                    log.info("Backfill: Generated image for location ${location.name}")
                }.onFailure { error ->
                    log.warn("Backfill: Failed to generate image for location ${location.name}: ${error.message}")
                }
            }

            // Chests
            val chests = ChestRepository.findAll().filter { it.imageUrl == null }
            log.info("Backfill: ${chests.size} chests missing images")
            for (chest in chests) {
                ImageGenerationService.generateImage(
                    entityType = "chest",
                    entityId = chest.id,
                    description = chest.desc,
                    entityName = chest.name
                ).onSuccess { imageUrl ->
                    ChestRepository.updateImageUrl(chest.id, imageUrl)
                    log.info("Backfill: Generated image for chest ${chest.name}")
                }.onFailure { error ->
                    log.warn("Backfill: Failed to generate image for chest ${chest.name}: ${error.message}")
                }
            }

            log.info("Image backfill complete!")
        }

        call.respond(HttpStatusCode.Accepted, mapOf(
            "message" to "Image backfill started for all entity types (users, creatures, items, non-biome locations, chests)",
            "note" to "Check server logs for progress. Biome-generated map locations are skipped."
        ))
    }

    // Ensure every area has a gateway location at (0,0)
    post("/admin/ensure-origin-locations") {
        val allLocations = LocationRepository.findAll()
        val areaIds = allLocations.mapNotNull { it.areaId }.distinct()
        val created = mutableListOf<String>()

        for (areaId in areaIds) {
            val origin = LocationRepository.findByCoordinates(0, 0, areaId)
            if (origin == null) {
                val areaName = areaId.split("-", "_")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                LocationRepository.create(
                    Location(
                        name = "Gateway of $areaName",
                        desc = "A shimmering nexus point where travelers materialize, marked by ancient runestones.",
                        itemIds = emptyList(),
                        creatureIds = emptyList(),
                        exits = emptyList(),
                        featureIds = emptyList(),
                        gridX = 0,
                        gridY = 0,
                        areaId = areaId,
                        locationType = LocationType.OUTDOOR_GROUND
                    )
                )
                created.add(areaId)
                log.info("Created gateway location at (0,0) for area: $areaId")
            }
        }

        call.respond(mapOf(
            "message" to "Ensured origin locations for ${areaIds.size} areas",
            "areasChecked" to areaIds,
            "areasCreated" to created
        ))
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

    // Fix Tun du Lac Y-coordinate inversion (NORTH=-Y, SOUTH=+Y)
    post("/admin/fix-tun-du-lac-coords") {
        val magicShopId = "tun-du-lac-magic-shop"
        val innId = "tun-du-lac-inn"
        val fixed = mutableListOf<String>()

        val magicShop = LocationRepository.findById(magicShopId)
        if (magicShop != null && magicShop.gridY == 1) {
            LocationRepository.update(magicShop.copy(gridY = -1))
            fixed.add("Magic Shop: gridY 1 -> -1 (north of town square)")
        }

        val inn = LocationRepository.findById(innId)
        if (inn != null && inn.gridY == -1) {
            LocationRepository.update(inn.copy(gridY = 1))
            fixed.add("Inn: gridY -1 -> 1 (south of town square)")
        }

        if (fixed.isEmpty()) {
            call.respond(mapOf(
                "message" to "Tun du Lac coordinates already correct",
                "fixed" to fixed
            ))
        } else {
            log.info("Fixed Tun du Lac coordinates: $fixed")
            call.respond(mapOf(
                "message" to "Fixed ${fixed.size} Tun du Lac location coordinates",
                "fixed" to fixed
            ))
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
}

// Helper functions

/**
 * Find a random unused coordinate for a new location in the overworld.
 * Searches in an expanding spiral from origin, with randomization.
 */
private fun findRandomUnusedCoordinate(existingLocations: List<Location>, areaId: String = "overworld"): Pair<Int, Int> {
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
 * Create a backup of the database file.
 * Returns the path to the backup file.
 */
private fun createDatabaseBackup(prefix: String = "backup"): String {
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
private fun listDatabaseBackups(): List<File> {
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
private fun restoreDatabaseFromBackup(filename: String) {
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
private fun deleteDatabaseBackup(filename: String) {
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
private fun checkServiceHealth(url: String, timeoutMs: Int = 3000): Boolean {
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
private fun controlOllama(action: String): ServiceActionResponse {
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
private fun controlStableDiffusion(action: String): ServiceActionResponse {
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
private fun controlCloudflare(action: String): ServiceActionResponse {
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
