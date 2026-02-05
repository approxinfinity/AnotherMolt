package com.ez2bg.anotherthread.worldgen

import kotlinx.serialization.Serializable

/**
 * Parameters for grid-based world generation.
 */
@Serializable
data class WorldGenParams(
    // Grid dimensions
    val width: Int = 20,               // Grid width (number of cells)
    val height: Int = 20,              // Grid height (number of cells)

    // Random seed for reproducibility
    val seed: Long = System.currentTimeMillis(),

    // Island shaping parameters
    val islandFactor: Double = 1.2,    // Higher = more circular island shape
    val noiseFactor: Double = 0.5,     // Amount of noise in land/water boundary
    val landThreshold: Double = 0.3,   // Higher = less land, more water

    // Noise parameters for elevation
    val elevationNoiseScale: Double = 0.08,
    val elevationNoiseWeight: Double = 0.3, // How much noise affects elevation (vs distance)

    // Noise parameters for moisture
    val moistureNoiseScale: Double = 0.1,
    val moistureDecay: Double = 0.85,  // How quickly moisture decreases inland
    val moistureNoiseWeight: Double = 0.4,

    // River settings
    val riverDensity: Double = 0.25,   // Probability of river starting at mountains
    val riverMinLength: Int = 3,       // Minimum cells for a river to be created

    // Area/region settings
    val areaId: String = "generated-${System.currentTimeMillis()}",
    val areaName: String = "Generated World",

    // Offset for grid coordinates (allows placing in different parts of the world)
    val gridOffsetX: Int = 0,
    val gridOffsetY: Int = 0,

    // Content generation
    val generateNames: Boolean = true,
    val generateDescriptions: Boolean = true,

    // Whether to connect water cells (creates ocean/lake traversal)
    val connectWaterCells: Boolean = false
)

/**
 * Result of world generation.
 */
@Serializable
data class WorldGenerationResult(
    val success: Boolean,
    val locationIds: List<String> = emptyList(),
    val areaId: String,
    val stats: WorldGenStats? = null,
    val errorMessage: String? = null
)

@Serializable
data class WorldGenStats(
    val totalCells: Int,
    val landCells: Int,
    val waterCells: Int,
    val coastalCells: Int,
    val riverCells: Int,
    val biomeDistribution: Map<String, Int>
)

/**
 * Internal cell representation during generation.
 */
data class GridCell(
    val x: Int,
    val y: Int,
    var isLand: Boolean = true,
    var isCoast: Boolean = false,
    var isRiver: Boolean = false,
    var elevation: Double = 0.0,
    var moisture: Double = 0.0,
    var distanceToWater: Int = Int.MAX_VALUE,
    var biome: Biome = Biome.GRASSLAND,
    var downslope: Pair<Int, Int>? = null, // Coordinates of downhill neighbor
    var locationId: String? = null
)
