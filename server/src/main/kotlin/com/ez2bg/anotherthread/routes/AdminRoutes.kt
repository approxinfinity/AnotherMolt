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
