package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.worldgen.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * World generation API routes.
 */
fun Route.worldGenRoutes() {
    route("/world") {
        /**
         * POST /world/generate
         *
         * Generate a new world with the specified parameters.
         * This is an async operation - returns immediately with a job ID.
         */
        post("/generate") {
            val params = call.receive<WorldGenParams>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            // Validate parameters
            if (params.width < 5 || params.width > 100) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    WorldGenJobResponse(
                        success = false,
                        message = "width must be between 5 and 100"
                    )
                )
            }

            if (params.height < 5 || params.height > 100) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    WorldGenJobResponse(
                        success = false,
                        message = "height must be between 5 and 100"
                    )
                )
            }

            // Check if area already exists
            val existingLocations = LocationRepository.findAll()
                .filter { it.areaId == params.areaId }

            if (existingLocations.isNotEmpty()) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    WorldGenJobResponse(
                        success = false,
                        message = "Area '${params.areaId}' already exists with ${existingLocations.size} locations. Delete it first with DELETE /world/area/${params.areaId}"
                    )
                )
            }

            // Start generation in background
            val jobId = java.util.UUID.randomUUID().toString()
            WorldGenJobManager.startJob(jobId, params)

            application.launch {
                try {
                    val progressCallback: (String, Int, Int, String) -> Unit = { phase, current, total, message ->
                        WorldGenJobManager.updateProgress(jobId, phase, current, total, message)
                    }
                    val generator = GridWorldGenerator(params, progressCallback)
                    val result = generator.generate()
                    WorldGenJobManager.completeJob(jobId, result)

                    // Audit log
                    AuditLogRepository.log(
                        recordId = params.areaId,
                        recordType = "WorldGeneration",
                        recordName = "Generated world: ${params.areaId} (${result.locationIds.size} locations)",
                        action = AuditAction.CREATE,
                        userId = userId,
                        userName = userName
                    )
                } catch (e: Exception) {
                    WorldGenJobManager.failJob(jobId, e.message ?: "Unknown error")
                }
            }

            call.respond(
                HttpStatusCode.Accepted,
                WorldGenJobResponse(
                    success = true,
                    jobId = jobId,
                    message = "World generation started for area '${params.areaId}'"
                )
            )
        }

        /**
         * POST /world/generate/sync
         *
         * Generate a new world synchronously (waits for completion).
         * Use for smaller worlds or when you need immediate results.
         */
        post("/generate/sync") {
            val params = call.receive<WorldGenParams>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            // Validate and limit size for sync generation
            val limitedParams = params.copy(
                width = params.width.coerceAtMost(30),
                height = params.height.coerceAtMost(30),
                generateNames = false, // Skip Ollama for sync mode
                generateDescriptions = false
            )

            // Check if area already exists
            val existingLocations = LocationRepository.findAll()
                .filter { it.areaId == limitedParams.areaId }

            if (existingLocations.isNotEmpty()) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    WorldGenerationResult(
                        success = false,
                        areaId = limitedParams.areaId,
                        errorMessage = "Area '${limitedParams.areaId}' already exists with ${existingLocations.size} locations"
                    )
                )
            }

            val generator = GridWorldGenerator(limitedParams)
            val result = generator.generate()

            if (result.success) {
                AuditLogRepository.log(
                    recordId = limitedParams.areaId,
                    recordType = "WorldGeneration",
                    recordName = "Generated world: ${limitedParams.areaId} (${result.locationIds.size} locations)",
                    action = AuditAction.CREATE,
                    userId = userId,
                    userName = userName
                )
            }

            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.InternalServerError, result)
        }

        /**
         * GET /world/generate/{jobId}/status
         *
         * Check the status of a world generation job.
         */
        get("/generate/{jobId}/status") {
            val jobId = call.parameters["jobId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing jobId")

            val status = WorldGenJobManager.getJobStatus(jobId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Job not found: $jobId")

            call.respond(status)
        }

        /**
         * DELETE /world/area/{areaId}
         *
         * Delete all locations in an area (for regeneration).
         */
        delete("/area/{areaId}") {
            val areaId = call.parameters["areaId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing areaId")
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val locations = LocationRepository.findAll()
                .filter { it.areaId == areaId }

            if (locations.isEmpty()) {
                return@delete call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "No locations found in area '$areaId'")
                )
            }

            // Delete all locations in the area
            var deleted = 0
            locations.forEach { location ->
                // Remove exits pointing to this location from other locations
                LocationRepository.removeExitIdFromAll(location.id)
                if (LocationRepository.delete(location.id)) {
                    deleted++
                }
            }

            // Audit log
            AuditLogRepository.log(
                recordId = areaId,
                recordType = "WorldGeneration",
                recordName = "Deleted area: $areaId ($deleted locations)",
                action = AuditAction.DELETE,
                userId = userId,
                userName = userName
            )

            call.respond(mapOf(
                "success" to true,
                "deleted" to deleted,
                "areaId" to areaId
            ))
        }

        /**
         * GET /world/areas
         *
         * List all areas with location counts.
         */
        get("/areas") {
            val locations = LocationRepository.findAll()
            val areas = locations.groupBy { it.areaId ?: "default" }
                .map { (areaId, locs) ->
                    AreaInfo(
                        areaId = areaId,
                        locationCount = locs.size,
                        hasCoordinates = locs.any { it.gridX != null && it.gridY != null }
                    )
                }
                .sortedByDescending { it.locationCount }

            call.respond(areas)
        }

        /**
         * GET /world/biomes
         *
         * Get list of all available biomes.
         */
        get("/biomes") {
            val biomes = Biome.values().map { biome ->
                BiomeInfo(
                    name = biome.name,
                    displayName = biome.displayName,
                    terrainWords = biome.terrainWords,
                    featureWords = biome.featureWords,
                    colorHex = "0x${biome.colorHex.toString(16).uppercase()}"
                )
            }
            call.respond(biomes)
        }

        /**
         * GET /world/whittaker
         *
         * Get Whittaker diagram data for visualization.
         */
        get("/whittaker") {
            val gridSize = 20
            val data = (0 until gridSize).flatMap { elevationStep ->
                (0 until gridSize).map { moistureStep ->
                    val elevation = elevationStep.toDouble() / (gridSize - 1)
                    val moisture = moistureStep.toDouble() / (gridSize - 1)
                    val biome = WhittakerDiagram.getBiome(elevation, moisture)

                    WhittakerCell(
                        elevation = elevation,
                        moisture = moisture,
                        biome = biome.name,
                        biomeName = biome.displayName,
                        colorHex = "0x${biome.colorHex.toString(16).uppercase()}"
                    )
                }
            }
            call.respond(data)
        }

        /**
         * GET /world/params/defaults
         *
         * Get default generation parameters.
         */
        get("/params/defaults") {
            call.respond(WorldGenParams())
        }
    }
}

// Response DTOs

@Serializable
data class WorldGenJobResponse(
    val success: Boolean,
    val jobId: String? = null,
    val message: String
)

@Serializable
data class AreaInfo(
    val areaId: String,
    val locationCount: Int,
    val hasCoordinates: Boolean
)

@Serializable
data class BiomeInfo(
    val name: String,
    val displayName: String,
    val terrainWords: List<String>,
    val featureWords: List<String>,
    val colorHex: String
)

@Serializable
data class WhittakerCell(
    val elevation: Double,
    val moisture: Double,
    val biome: String,
    val biomeName: String,
    val colorHex: String
)

/**
 * Simple in-memory job manager for tracking generation jobs.
 */
object WorldGenJobManager {
    private val jobs = ConcurrentHashMap<String, WorldGenJobStatus>()

    fun startJob(jobId: String, params: WorldGenParams) {
        jobs[jobId] = WorldGenJobStatus(
            jobId = jobId,
            status = "running",
            startedAt = System.currentTimeMillis(),
            progress = WorldGenProgress(
                phase = "initializing",
                message = "Starting world generation..."
            )
        )
    }

    fun updateProgress(jobId: String, phase: String, current: Int, total: Int, message: String) {
        jobs[jobId]?.let {
            jobs[jobId] = it.copy(
                progress = WorldGenProgress(
                    phase = phase,
                    current = current,
                    total = total,
                    message = message
                )
            )
        }
    }

    fun completeJob(jobId: String, result: WorldGenerationResult) {
        jobs[jobId]?.let {
            jobs[jobId] = it.copy(
                status = if (result.success) "completed" else "failed",
                completedAt = System.currentTimeMillis(),
                result = result,
                error = result.errorMessage,
                progress = null
            )
        }
    }

    fun failJob(jobId: String, error: String) {
        jobs[jobId]?.let {
            jobs[jobId] = it.copy(
                status = "failed",
                completedAt = System.currentTimeMillis(),
                error = error,
                progress = null
            )
        }
    }

    fun getJobStatus(jobId: String): WorldGenJobStatus? = jobs[jobId]
}

@Serializable
data class WorldGenJobStatus(
    val jobId: String,
    val status: String, // "running", "completed", "failed"
    val startedAt: Long,
    val completedAt: Long? = null,
    val result: WorldGenerationResult? = null,
    val error: String? = null,
    val progress: WorldGenProgress? = null
)

@Serializable
data class WorldGenProgress(
    val phase: String,           // "grid", "terrain", "locations", "exits", "names"
    val current: Int = 0,
    val total: Int = 0,
    val message: String = ""
)
