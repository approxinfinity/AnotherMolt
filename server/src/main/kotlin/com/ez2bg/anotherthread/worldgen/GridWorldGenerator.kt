package com.ez2bg.anotherthread.worldgen

import com.ez2bg.anotherthread.ContentGenerationService
import com.ez2bg.anotherthread.database.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Grid-based world generator using Amit Patel's terrain generation principles.
 *
 * Pipeline:
 * 1. Create grid of cells
 * 2. Island shape (radial + noise → land/water)
 * 3. Elevation from BFS distance to water + noise
 * 4. Moisture from BFS decay from water + noise
 * 5. Biomes via Whittaker diagram lookup
 * 6. Rivers trace downhill from mountains
 * 7. Convert cells to Locations with 8-directional exits
 * 8. Generate names/descriptions via Ollama
 */
class GridWorldGenerator(
    private val params: WorldGenParams,
    private val onProgress: ((phase: String, current: Int, total: Int, message: String) -> Unit)? = null
) {
    private val random = Random(params.seed)
    private lateinit var grid: Array<Array<GridCell>>

    // Direction offsets for 8-way movement
    private val directions = mapOf(
        ExitDirection.NORTH to Pair(0, -1),
        ExitDirection.NORTHEAST to Pair(1, -1),
        ExitDirection.EAST to Pair(1, 0),
        ExitDirection.SOUTHEAST to Pair(1, 1),
        ExitDirection.SOUTH to Pair(0, 1),
        ExitDirection.SOUTHWEST to Pair(-1, 1),
        ExitDirection.WEST to Pair(-1, 0),
        ExitDirection.NORTHWEST to Pair(-1, -1)
    )

    /**
     * Generate a complete world and save to database.
     */
    suspend fun generate(): WorldGenerationResult {
        return try {
            // Step 1: Initialize grid
            onProgress?.invoke("terrain", 1, 6, "Initializing grid...")
            initializeGrid()

            // Step 2: Determine land vs water
            onProgress?.invoke("terrain", 2, 6, "Calculating land and water...")
            assignLandWater()

            // Step 3: Calculate elevation
            onProgress?.invoke("terrain", 3, 6, "Calculating elevation...")
            calculateElevation()

            // Step 4: Calculate moisture
            onProgress?.invoke("terrain", 4, 6, "Calculating moisture...")
            calculateMoisture()

            // Step 5: Assign biomes
            onProgress?.invoke("terrain", 5, 6, "Assigning biomes...")
            assignBiomes()

            // Step 6: Generate rivers
            onProgress?.invoke("terrain", 6, 6, "Generating rivers...")
            generateRivers()

            // Step 7-8: Create locations and save
            createLocations()
        } catch (e: Exception) {
            WorldGenerationResult(
                success = false,
                areaId = params.areaId,
                errorMessage = e.message ?: "Unknown error during generation"
            )
        }
    }

    /**
     * Step 1: Initialize the grid with cells.
     */
    private fun initializeGrid() {
        grid = Array(params.width) { x ->
            Array(params.height) { y ->
                GridCell(x = x, y = y)
            }
        }
    }

    /**
     * Step 2: Assign land vs water using radial gradient + noise.
     */
    private fun assignLandWater() {
        val centerX = params.width / 2.0
        val centerY = params.height / 2.0
        val maxRadius = sqrt(centerX * centerX + centerY * centerY)

        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]

                // Radial distance from center (0 at center, 1 at corners)
                val dx = (x - centerX) / centerX
                val dy = (y - centerY) / centerY
                val radialDist = sqrt(dx * dx + dy * dy)

                // Noise for irregular coastline
                val noiseValue = SimplexNoise.seededFractalNoise2D(
                    x.toFloat() * 0.1f,
                    y.toFloat() * 0.1f,
                    params.seed,
                    octaves = 4,
                    persistence = 0.5f
                )

                // Combine radial gradient with noise
                val totalWeight = params.islandFactor + params.noiseFactor
                val radialWeight = params.islandFactor / totalWeight
                val noiseWeight = params.noiseFactor / totalWeight

                val combinedValue = radialWeight * (1 - radialDist) +
                        noiseWeight * ((noiseValue + 1) / 2)

                cell.isLand = combinedValue > params.landThreshold
            }
        }

        // Mark coastal cells (land cells adjacent to water)
        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]
                if (cell.isLand) {
                    cell.isCoast = getNeighbors(x, y).any { (nx, ny) ->
                        !grid[nx][ny].isLand
                    }
                }
            }
        }
    }

    /**
     * Step 3: Calculate elevation based on distance from water.
     */
    private fun calculateElevation() {
        // BFS from water cells to calculate distance
        val queue = ArrayDeque<Pair<Int, Int>>()

        // Initialize water cells
        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]
                if (!cell.isLand) {
                    cell.distanceToWater = 0
                    cell.elevation = -0.1 // Underwater
                    queue.add(Pair(x, y))
                }
            }
        }

        // BFS to propagate distance
        while (queue.isNotEmpty()) {
            val (cx, cy) = queue.removeFirst()
            val currentDist = grid[cx][cy].distanceToWater

            for ((nx, ny) in getNeighbors(cx, cy)) {
                val neighbor = grid[nx][ny]
                val newDist = currentDist + 1

                if (newDist < neighbor.distanceToWater) {
                    neighbor.distanceToWater = newDist
                    queue.add(Pair(nx, ny))
                }
            }
        }

        // Calculate elevation for land cells
        val maxDist = grid.flatMap { it.toList() }
            .filter { it.isLand }
            .maxOfOrNull { it.distanceToWater } ?: 1

        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]
                if (cell.isLand) {
                    // Base elevation from distance to water
                    val baseElevation = cell.distanceToWater.toDouble() / maxDist

                    // Add noise for variety
                    val noiseValue = SimplexNoise.seededFractalNoise2D(
                        x.toFloat() * params.elevationNoiseScale.toFloat(),
                        y.toFloat() * params.elevationNoiseScale.toFloat(),
                        params.seed + 1000,
                        octaves = 4,
                        persistence = 0.5f
                    )
                    val normalizedNoise = (noiseValue + 1) / 2

                    // Combine: mostly distance-based, some noise
                    val distWeight = 1.0 - params.elevationNoiseWeight
                    cell.elevation = (distWeight * baseElevation +
                            params.elevationNoiseWeight * normalizedNoise)
                        .coerceIn(0.0, 1.0)

                    // Coastal cells get lower elevation
                    if (cell.isCoast) {
                        cell.elevation = cell.elevation * 0.3
                    }
                }
            }
        }

        // Calculate downslope for each cell (for rivers)
        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]
                val lowestNeighbor = getNeighbors(x, y)
                    .minByOrNull { (nx, ny) -> grid[nx][ny].elevation }

                if (lowestNeighbor != null) {
                    val (nx, ny) = lowestNeighbor
                    if (grid[nx][ny].elevation < cell.elevation) {
                        cell.downslope = lowestNeighbor
                    }
                }
            }
        }
    }

    /**
     * Step 4: Calculate moisture based on distance from water + noise.
     */
    private fun calculateMoisture() {
        val moistureMap = mutableMapOf<Pair<Int, Int>, Double>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        // Water cells have maximum moisture
        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                if (!grid[x][y].isLand) {
                    moistureMap[Pair(x, y)] = 1.0
                    queue.add(Pair(x, y))
                }
            }
        }

        // Propagate moisture with decay
        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            val currentMoisture = moistureMap[pos] ?: continue

            for (neighborPos in getNeighbors(pos.first, pos.second)) {
                val newMoisture = currentMoisture * params.moistureDecay
                val existingMoisture = moistureMap[neighborPos] ?: 0.0

                if (newMoisture > existingMoisture) {
                    moistureMap[neighborPos] = newMoisture
                    queue.add(neighborPos)
                }
            }
        }

        // Apply moisture with noise variation
        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]
                val baseMoisture = moistureMap[Pair(x, y)] ?: 0.0

                // Add noise for variation (offset seed for different pattern)
                val noiseValue = SimplexNoise.seededFractalNoise2D(
                    x.toFloat() * params.moistureNoiseScale.toFloat(),
                    y.toFloat() * params.moistureNoiseScale.toFloat(),
                    params.seed + 5000,
                    octaves = 3,
                    persistence = 0.5f
                )
                val normalizedNoise = (noiseValue + 1) / 2

                // Combine base moisture with noise
                val moistWeight = 1.0 - params.moistureNoiseWeight
                cell.moisture = (moistWeight * baseMoisture +
                        params.moistureNoiseWeight * normalizedNoise)
                    .coerceIn(0.0, 1.0)
            }
        }
    }

    /**
     * Step 5: Assign biomes using Whittaker diagram.
     */
    private fun assignBiomes() {
        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]

                cell.biome = when {
                    // Water cells
                    !cell.isLand -> WhittakerDiagram.getWaterBiome(cell.distanceToWater)

                    // Check for marsh
                    WhittakerDiagram.isMarsh(cell.elevation, cell.moisture, cell.isCoast) ->
                        Biome.MARSH

                    // Coastal cells with low elevation
                    cell.isCoast && cell.elevation < 0.12 -> Biome.COAST

                    // Land cells use Whittaker diagram
                    else -> WhittakerDiagram.getBiome(cell.elevation, cell.moisture)
                }
            }
        }
    }

    /**
     * Step 6: Generate rivers flowing downslope from mountains.
     */
    private fun generateRivers() {
        // Find high-elevation cells as potential river sources
        val sources = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]
                if (cell.isLand && cell.elevation > 0.65 && random.nextDouble() < params.riverDensity) {
                    sources.add(Pair(x, y))
                }
            }
        }

        // Trace each potential river
        for (source in sources) {
            val riverPath = mutableListOf<Pair<Int, Int>>()
            var current = source

            // Follow downslope until reaching water or dead end
            while (grid[current.first][current.second].isLand && riverPath.size < 50) {
                riverPath.add(current)

                val currentCell = grid[current.first][current.second]
                val downslope = currentCell.downslope ?: break

                val downslopeCell = grid[downslope.first][downslope.second]
                if (downslopeCell.elevation >= currentCell.elevation) break

                current = downslope
            }

            // Only mark as river if it reaches water and is long enough
            if (!grid[current.first][current.second].isLand &&
                riverPath.size >= params.riverMinLength) {

                for ((rx, ry) in riverPath) {
                    val cell = grid[rx][ry]
                    cell.isRiver = true
                    // Rivers increase local moisture
                    cell.moisture = (cell.moisture + 0.2).coerceAtMost(1.0)
                }
            }
        }
    }

    /**
     * Step 7-8: Convert grid cells to Location entities and save.
     */
    private suspend fun createLocations(): WorldGenerationResult = coroutineScope {
        val createdIds = mutableListOf<String>()

        // Count land cells for progress
        val landCellCount = grid.flatMap { it.toList() }.count { it.isLand || params.connectWaterCells }
        var createdCount = 0

        // First pass: Create all locations without exits
        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]

                // Skip water cells unless connectWaterCells is enabled
                if (!cell.isLand && !params.connectWaterCells) continue

                val locationId = UUID.randomUUID().toString()
                cell.locationId = locationId

                val gridX = x + params.gridOffsetX
                val gridY = y + params.gridOffsetY

                // Build terrain features list from cell properties
                val features = buildList {
                    if (cell.isRiver) add("river")
                    if (cell.isCoast) add("coast")
                    if (cell.elevation > 0.7) add("high_elevation")
                    if (cell.elevation < 0.2) add("low_elevation")
                    if (cell.moisture > 0.7) add("wet")
                    if (cell.moisture < 0.3) add("dry")
                    if (cell.biome == Biome.MARSH) add("marsh")
                }

                val location = Location(
                    id = locationId,
                    name = generateBasicName(cell),
                    desc = generateBasicDescription(cell),
                    itemIds = emptyList(),
                    creatureIds = emptyList(),
                    exits = emptyList(), // Added in second pass
                    featureIds = emptyList(),
                    gridX = gridX,
                    gridY = gridY,
                    areaId = params.areaId,
                    biome = cell.biome.name,
                    elevation = cell.elevation.toFloat(),
                    moisture = cell.moisture.toFloat(),
                    isRiver = cell.isRiver,
                    isCoast = cell.isCoast,
                    terrainFeatures = features.ifEmpty { null },
                    isOriginalTerrain = true
                )

                LocationRepository.create(location)
                createdIds.add(locationId)
                createdCount++

                // Report progress every 10 locations
                if (createdCount % 10 == 0 || createdCount == landCellCount) {
                    onProgress?.invoke("locations", createdCount, landCellCount, "Creating locations: $createdCount / $landCellCount")
                }
            }
        }

        // Second pass: Add exits to each location
        var exitCount = 0
        val totalLocations = createdIds.size
        onProgress?.invoke("exits", 0, totalLocations, "Adding exits...")

        for (x in 0 until params.width) {
            for (y in 0 until params.height) {
                val cell = grid[x][y]
                val locationId = cell.locationId ?: continue

                val exits = mutableListOf<Exit>()

                for ((direction, offset) in directions) {
                    val nx = x + offset.first
                    val ny = y + offset.second

                    if (nx in 0 until params.width && ny in 0 until params.height) {
                        val neighborCell = grid[nx][ny]
                        val neighborLocationId = neighborCell.locationId

                        // Only create exit if neighbor has a location
                        if (neighborLocationId != null) {
                            // Don't create exits between land and water unless enabled
                            if (cell.isLand == neighborCell.isLand || params.connectWaterCells) {
                                exits.add(Exit(locationId = neighborLocationId, direction = direction))
                            }
                        }
                    }
                }

                if (exits.isNotEmpty()) {
                    val location = LocationRepository.findById(locationId)
                    if (location != null) {
                        LocationRepository.update(location.copy(exits = exits))
                    }
                }

                exitCount++
                if (exitCount % 20 == 0 || exitCount == totalLocations) {
                    onProgress?.invoke("exits", exitCount, totalLocations, "Adding exits: $exitCount / $totalLocations")
                }
            }
        }

        // Optional: Generate better names via Ollama (in batches)
        if (params.generateNames || params.generateDescriptions) {
            try {
                val batchSize = 5
                var nameCount = 0
                val totalNames = createdIds.size
                onProgress?.invoke("names", 0, totalNames, "Generating names with AI...")

                createdIds.chunked(batchSize).forEach { batch ->
                    batch.map { locationId ->
                        async {
                            generateLocationContent(locationId)
                        }
                    }.awaitAll()

                    nameCount += batch.size
                    onProgress?.invoke("names", nameCount, totalNames, "Generating names: $nameCount / $totalNames")
                }
            } catch (e: Exception) {
                // Log but don't fail - basic names are already there
                println("Warning: Ollama content generation failed: ${e.message}")
            }
        }

        // Ensure a gateway location exists at (0,0) for rift portal access
        ensureGatewayLocation(createdIds)

        // Calculate stats
        val allCells = grid.flatMap { it.toList() }
        val stats = WorldGenStats(
            totalCells = allCells.size,
            landCells = allCells.count { it.isLand },
            waterCells = allCells.count { !it.isLand },
            coastalCells = allCells.count { it.isCoast },
            riverCells = allCells.count { it.isRiver },
            biomeDistribution = allCells.groupBy { it.biome.name }
                .mapValues { it.value.size }
        )

        WorldGenerationResult(
            success = true,
            locationIds = createdIds,
            areaId = params.areaId,
            stats = stats
        )
    }

    /**
     * Get valid neighbor coordinates for a cell.
     */
    private fun getNeighbors(x: Int, y: Int): List<Pair<Int, Int>> {
        return directions.values.mapNotNull { (dx, dy) ->
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until params.width && ny in 0 until params.height) {
                Pair(nx, ny)
            } else null
        }
    }

    /**
     * Generate a basic name from biome.
     */
    private fun generateBasicName(cell: GridCell): String {
        val adjective = cell.biome.randomTerrainWord()
            .replaceFirstChar { it.uppercase() }

        val baseName = when {
            cell.isRiver -> "River ${cell.biome.displayName}"
            cell.isCoast -> "Coastal ${cell.biome.displayName}"
            else -> cell.biome.displayName
        }

        return "$adjective $baseName"
    }

    /**
     * Generate a basic description from biome properties.
     */
    private fun generateBasicDescription(cell: GridCell): String {
        val terrainWord = cell.biome.randomTerrainWord()
        val featureWord = cell.biome.randomFeatureWord()

        return buildString {
            append("You find yourself in a $terrainWord ${cell.biome.displayName.lowercase()}. ")
            append("The area features $featureWord. ")

            if (cell.isRiver) {
                append("A river flows through this area. ")
            }

            if (cell.isCoast) {
                append("The sound of waves can be heard nearby. ")
            }

            when {
                cell.elevation > 0.7 -> append("The air is thin at this altitude. ")
                cell.elevation > 0.5 -> append("You are in the highlands. ")
                cell.elevation < 0.15 -> append("The ground here is low-lying. ")
            }

            when {
                cell.moisture > 0.8 -> append("The air is thick with humidity. ")
                cell.moisture < 0.2 -> append("The air is dry and parched. ")
            }
        }
    }

    /**
     * Generate better content via Ollama for a location.
     */
    private suspend fun generateLocationContent(locationId: String) {
        val location = LocationRepository.findById(locationId) ?: return

        // Find the cell for context
        val cell = grid.flatMap { it.toList() }
            .find { it.locationId == locationId } ?: return

        // Build neighbor context
        val neighborBiomes = getNeighbors(cell.x, cell.y)
            .mapNotNull { (nx, ny) -> grid[nx][ny].biome.displayName }
            .distinct()
            .take(3)

        val contextHint = buildString {
            append("Biome: ${cell.biome.displayName}. ")
            append("Terrain: ${cell.biome.randomTerrainWord()}. ")
            if (neighborBiomes.isNotEmpty()) {
                append("Adjacent areas: ${neighborBiomes.joinToString(", ")}. ")
            }
            if (cell.isCoast) append("Coastal location. ")
            if (cell.isRiver) append("River runs through. ")
        }

        val result = ContentGenerationService.generateLocationContent(
            exitIds = location.exits.map { it.locationId },
            featureIds = emptyList(),
            existingName = cell.biome.displayName,
            existingDesc = contextHint
        )

        result.getOrNull()?.let { content ->
            LocationRepository.update(location.copy(
                name = content.name,
                desc = content.description
            ))
        }
    }

    /**
     * Ensure a gateway location exists at (0,0) for this area.
     * If terrain doesn't naturally have a location at 0,0, creates one
     * and connects it to the nearest existing location.
     */
    private fun ensureGatewayLocation(createdIds: MutableList<String>) {
        // Check if a location already exists at (0,0) for this area
        val existingOrigin = LocationRepository.findByCoordinates(0, 0, params.areaId)
        if (existingOrigin != null) {
            return // Already have a location at 0,0
        }

        // Find the nearest location to use as a connection point
        val allLocations = LocationRepository.findAll().filter { it.areaId == params.areaId }
        if (allLocations.isEmpty()) {
            return // No locations to connect to
        }

        // Find the location closest to (0,0)
        val nearest = allLocations.minByOrNull { loc ->
            val x = loc.gridX ?: 0
            val y = loc.gridY ?: 0
            x * x + y * y // Distance squared from origin
        } ?: return

        // Format area name nicely
        val areaName = params.areaId.split("-", "_")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        // Create the gateway location at (0,0)
        val gateway = Location(
            id = UUID.randomUUID().toString(),
            name = "Gateway of $areaName",
            desc = "A shimmering nexus point where travelers materialize, marked by ancient runestones. The path ahead leads into the heart of $areaName.",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = listOf(
                Exit(locationId = nearest.id, direction = ExitDirection.ENTER)
            ),
            featureIds = emptyList(),
            gridX = 0,
            gridY = 0,
            areaId = params.areaId,
            locationType = LocationType.OUTDOOR_GROUND
        )

        val createdGateway = LocationRepository.create(gateway)
        createdIds.add(createdGateway.id)

        // Add a return exit from the nearest location back to the gateway
        val updatedNearest = nearest.copy(
            exits = nearest.exits + Exit(locationId = createdGateway.id, direction = ExitDirection.ENTER)
        )
        LocationRepository.update(updatedNearest)

        println("Created gateway location at (0,0) for area ${params.areaId}, connected to ${nearest.name}")
    }
}
