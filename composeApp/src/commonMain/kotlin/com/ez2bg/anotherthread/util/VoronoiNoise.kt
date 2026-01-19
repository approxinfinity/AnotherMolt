package com.ez2bg.anotherthread.util

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Voronoi noise implementation for biome blending and cellular patterns.
 * Generates organic cell-based patterns useful for terrain transitions.
 */
object VoronoiNoise {

    /**
     * Result of a Voronoi cell query, containing distance information
     * and cell identifiers for blending.
     */
    data class VoronoiResult(
        val distance1: Float,      // Distance to nearest cell center
        val distance2: Float,      // Distance to second nearest cell center
        val cellId1: Int,          // ID of nearest cell (for biome assignment)
        val cellId2: Int,          // ID of second nearest cell
        val cellX1: Float,         // X coordinate of nearest cell center
        val cellY1: Float,         // Y coordinate of nearest cell center
        val cellX2: Float,         // X coordinate of second nearest cell center
        val cellY2: Float,         // Y coordinate of second nearest cell center
        val edgeDistance: Float    // Distance to cell edge (for blending)
    ) {
        /**
         * Calculate blend factor between two cells.
         * Returns 0.0 when fully in cell1, 1.0 when on the edge.
         * @param blendWidth Width of the blend zone (0.0-1.0)
         */
        fun blendFactor(blendWidth: Float = 0.3f): Float {
            if (blendWidth <= 0f) return 0f
            val normalizedEdge = edgeDistance / (distance1 + distance2).coerceAtLeast(0.001f)
            return (normalizedEdge / blendWidth).coerceIn(0f, 1f)
        }

        /**
         * Get interpolated weights for the two nearest cells.
         * @param blendWidth Width of the blend zone
         * @return Pair of (weight1, weight2) that sum to 1.0
         */
        fun cellWeights(blendWidth: Float = 0.3f): Pair<Float, Float> {
            val blend = blendFactor(blendWidth)
            val weight1 = 1f - blend * 0.5f
            val weight2 = blend * 0.5f
            return Pair(weight1, weight2)
        }
    }

    // Hash function for cell point generation
    private fun hash2D(x: Int, y: Int, seed: Int): Int {
        var h = seed
        h = h xor (x * 374761393)
        h = h xor (y * 668265263)
        h = (h xor (h ushr 13)) * 1274126177
        return h xor (h ushr 16)
    }

    // Generate a pseudo-random float in [0, 1) from hash
    private fun hashToFloat(hash: Int): Float {
        return (hash and 0x7FFFFFFF) / 2147483648f
    }

    /**
     * Calculate Voronoi cell information for a point.
     * @param x X coordinate
     * @param y Y coordinate
     * @param scale Scale of the cells (larger = bigger cells)
     * @param jitter Amount of randomness in cell center positions (0.0-1.0)
     * @param seed Random seed for reproducible results
     * @return VoronoiResult containing cell distances and IDs
     */
    fun cellular(
        x: Float,
        y: Float,
        scale: Float = 1f,
        jitter: Float = 0.8f,
        seed: Int = 0
    ): VoronoiResult {
        val scaledX = x / scale
        val scaledY = y / scale

        val cellX = floor(scaledX).toInt()
        val cellY = floor(scaledY).toInt()

        var minDist1 = Float.MAX_VALUE
        var minDist2 = Float.MAX_VALUE
        var nearestCellId = 0
        var secondCellId = 0
        var nearestX = 0f
        var nearestY = 0f
        var secondX = 0f
        var secondY = 0f

        // Check 3x3 neighborhood
        for (dy in -1..1) {
            for (dx in -1..1) {
                val neighborX = cellX + dx
                val neighborY = cellY + dy

                // Generate cell center point with jitter
                val hash = hash2D(neighborX, neighborY, seed)
                val jitterX = hashToFloat(hash) * jitter
                val jitterY = hashToFloat(hash xor 0x5A5A5A5A) * jitter

                val pointX = neighborX + 0.5f + (jitterX - 0.5f) * jitter
                val pointY = neighborY + 0.5f + (jitterY - 0.5f) * jitter

                // Calculate distance
                val diffX = scaledX - pointX
                val diffY = scaledY - pointY
                val dist = sqrt(diffX * diffX + diffY * diffY)

                // Track two nearest cells
                if (dist < minDist1) {
                    minDist2 = minDist1
                    secondCellId = nearestCellId
                    secondX = nearestX
                    secondY = nearestY

                    minDist1 = dist
                    nearestCellId = hash
                    nearestX = pointX * scale
                    nearestY = pointY * scale
                } else if (dist < minDist2) {
                    minDist2 = dist
                    secondCellId = hash
                    secondX = pointX * scale
                    secondY = pointY * scale
                }
            }
        }

        // Calculate edge distance (distance to the boundary between cells)
        val edgeDistance = (minDist2 - minDist1) * scale

        return VoronoiResult(
            distance1 = minDist1 * scale,
            distance2 = minDist2 * scale,
            cellId1 = nearestCellId,
            cellId2 = secondCellId,
            cellX1 = nearestX,
            cellY1 = nearestY,
            cellX2 = secondX,
            cellY2 = secondY,
            edgeDistance = edgeDistance
        )
    }

    /**
     * Get simple Voronoi noise value (distance to nearest cell center).
     * Useful for creating cellular patterns.
     * @return Value in approximately [0, scale] range
     */
    fun noise2D(
        x: Float,
        y: Float,
        scale: Float = 1f,
        jitter: Float = 0.8f,
        seed: Int = 0
    ): Float {
        return cellular(x, y, scale, jitter, seed).distance1
    }

    /**
     * Get edge-highlighted Voronoi noise (F2 - F1).
     * Creates patterns with highlighted cell boundaries.
     * @return Value in approximately [0, scale] range, higher at edges
     */
    fun edgeNoise2D(
        x: Float,
        y: Float,
        scale: Float = 1f,
        jitter: Float = 0.8f,
        seed: Int = 0
    ): Float {
        val result = cellular(x, y, scale, jitter, seed)
        return result.distance2 - result.distance1
    }

    /**
     * Get cell ID for a point - useful for discrete biome assignment.
     * @return Integer cell ID (hash-based, unique per cell)
     */
    fun getCellId(
        x: Float,
        y: Float,
        scale: Float = 1f,
        jitter: Float = 0.8f,
        seed: Int = 0
    ): Int {
        return cellular(x, y, scale, jitter, seed).cellId1
    }

    /**
     * Worley noise variant - returns normalized distance.
     * @return Value in [0, 1] range
     */
    fun worleyNoise2D(
        x: Float,
        y: Float,
        scale: Float = 1f,
        jitter: Float = 0.8f,
        seed: Int = 0
    ): Float {
        val dist = noise2D(x, y, scale, jitter, seed)
        // Normalize to approximately [0, 1]
        return (dist / scale).coerceIn(0f, 1f)
    }

    /**
     * Multi-octave Voronoi noise for more complex patterns.
     */
    fun fractalVoronoi2D(
        x: Float,
        y: Float,
        scale: Float = 1f,
        octaves: Int = 3,
        persistence: Float = 0.5f,
        jitter: Float = 0.8f,
        seed: Int = 0
    ): Float {
        var total = 0f
        var frequency = 1f
        var amplitude = 1f
        var maxValue = 0f

        for (i in 0 until octaves) {
            total += worleyNoise2D(x * frequency, y * frequency, scale, jitter, seed + i) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= 2f
        }

        return total / maxValue
    }
}

/**
 * Biome blending system using Voronoi cells for smooth terrain transitions.
 */
object BiomeBlender {

    /**
     * Represents a biome with its properties.
     */
    data class Biome(
        val id: Int,
        val name: String,
        val baseColor: Long,      // Color as ARGB long
        val secondaryColor: Long,
        val humidity: Float,      // 0.0 = dry, 1.0 = wet
        val temperature: Float,   // 0.0 = cold, 1.0 = hot
        val elevation: Float      // Preferred elevation
    )

    /**
     * Result of biome sampling at a point.
     */
    data class BiomeSample(
        val primaryBiome: Biome,
        val secondaryBiome: Biome?,
        val blendFactor: Float,   // 0.0 = fully primary, 1.0 = edge (blend needed)
        val distanceToCenter: Float,
        val distanceToEdge: Float
    ) {
        /**
         * Interpolate a float value between biomes.
         */
        fun interpolate(getValue: (Biome) -> Float): Float {
            val secondary = secondaryBiome ?: return getValue(primaryBiome)
            val weight = blendFactor * 0.5f // Max 50% blend at edge
            return getValue(primaryBiome) * (1f - weight) + getValue(secondary) * weight
        }

        /**
         * Interpolate color between biomes (returns ARGB long).
         */
        fun interpolateColor(getColor: (Biome) -> Long): Long {
            val secondary = secondaryBiome ?: return getColor(primaryBiome)
            val weight = blendFactor * 0.5f

            val c1 = getColor(primaryBiome)
            val c2 = getColor(secondary)

            val a1 = (c1 shr 24) and 0xFF
            val r1 = (c1 shr 16) and 0xFF
            val g1 = (c1 shr 8) and 0xFF
            val b1 = c1 and 0xFF

            val a2 = (c2 shr 24) and 0xFF
            val r2 = (c2 shr 16) and 0xFF
            val g2 = (c2 shr 8) and 0xFF
            val b2 = c2 and 0xFF

            val a = (a1 * (1f - weight) + a2 * weight).toLong()
            val r = (r1 * (1f - weight) + r2 * weight).toLong()
            val g = (g1 * (1f - weight) + g2 * weight).toLong()
            val b = (b1 * (1f - weight) + b2 * weight).toLong()

            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    // Default biome definitions
    val FOREST = Biome(0, "Forest", 0xFF2A6A3A, 0xFF1A4A2A, 0.6f, 0.5f, 0.3f)
    val DESERT = Biome(1, "Desert", 0xFFEAD8B0, 0xFFD0C090, 0.1f, 0.9f, 0.2f)
    val GRASSLAND = Biome(2, "Grassland", 0xFF4A8A4A, 0xFF3A6A3A, 0.4f, 0.5f, 0.2f)
    val MOUNTAIN = Biome(3, "Mountain", 0xFF8A8A9A, 0xFF6A6A7A, 0.3f, 0.3f, 0.9f)
    val SWAMP = Biome(4, "Swamp", 0xFF4A6A5A, 0xFF3A5A4A, 0.9f, 0.6f, 0.1f)
    val HILLS = Biome(5, "Hills", 0xFF6B8F65, 0xFF5A7E55, 0.5f, 0.5f, 0.5f)
    val WATER = Biome(6, "Water", 0xFF4A8AAA, 0xFF3A6A8A, 1.0f, 0.5f, -0.2f)
    val COAST = Biome(7, "Coast", 0xFFD0C8A0, 0xFFC0B890, 0.7f, 0.6f, 0.0f)

    private val defaultBiomes = listOf(FOREST, DESERT, GRASSLAND, MOUNTAIN, SWAMP, HILLS, WATER, COAST)

    /**
     * Sample biome information at a point using Voronoi cells.
     *
     * @param x X coordinate in world space
     * @param y Y coordinate in world space
     * @param cellScale Size of biome cells
     * @param blendWidth Width of transition zones (0.0-1.0)
     * @param biomes List of available biomes
     * @param seed Random seed
     * @return BiomeSample with blending information
     */
    fun sampleBiome(
        x: Float,
        y: Float,
        cellScale: Float = 100f,
        blendWidth: Float = 0.3f,
        biomes: List<Biome> = defaultBiomes,
        seed: Int = 0
    ): BiomeSample {
        val voronoi = VoronoiNoise.cellular(x, y, cellScale, 0.7f, seed)

        // Map cell IDs to biomes
        val primaryIndex = (voronoi.cellId1 and 0x7FFFFFFF) % biomes.size
        val secondaryIndex = (voronoi.cellId2 and 0x7FFFFFFF) % biomes.size

        val primary = biomes[primaryIndex]
        val secondary = if (primaryIndex != secondaryIndex) biomes[secondaryIndex] else null

        val blend = voronoi.blendFactor(blendWidth)

        return BiomeSample(
            primaryBiome = primary,
            secondaryBiome = secondary,
            blendFactor = blend,
            distanceToCenter = voronoi.distance1,
            distanceToEdge = voronoi.edgeDistance
        )
    }

    /**
     * Get blended terrain parameters for a point.
     * Useful for modifying terrain rendering based on biome.
     */
    data class BlendedTerrainParams(
        val humidity: Float,
        val temperature: Float,
        val elevation: Float,
        val primaryBiomeId: Int,
        val secondaryBiomeId: Int?,
        val blendFactor: Float
    )

    fun getBlendedParams(
        x: Float,
        y: Float,
        cellScale: Float = 100f,
        blendWidth: Float = 0.3f,
        biomes: List<Biome> = defaultBiomes,
        seed: Int = 0
    ): BlendedTerrainParams {
        val sample = sampleBiome(x, y, cellScale, blendWidth, biomes, seed)

        return BlendedTerrainParams(
            humidity = sample.interpolate { it.humidity },
            temperature = sample.interpolate { it.temperature },
            elevation = sample.interpolate { it.elevation },
            primaryBiomeId = sample.primaryBiome.id,
            secondaryBiomeId = sample.secondaryBiome?.id,
            blendFactor = sample.blendFactor
        )
    }

    /**
     * Calculate how much of each terrain type should be present at a point.
     * Returns a map of biome ID to weight (0.0-1.0).
     */
    fun getTerrainWeights(
        x: Float,
        y: Float,
        cellScale: Float = 100f,
        blendWidth: Float = 0.3f,
        biomes: List<Biome> = defaultBiomes,
        seed: Int = 0
    ): Map<Int, Float> {
        val sample = sampleBiome(x, y, cellScale, blendWidth, biomes, seed)
        val weights = mutableMapOf<Int, Float>()

        val (w1, w2) = VoronoiNoise.cellular(x, y, cellScale, 0.7f, seed).cellWeights(blendWidth)

        weights[sample.primaryBiome.id] = w1
        sample.secondaryBiome?.let { weights[it.id] = w2 }

        return weights
    }
}
