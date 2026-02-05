package com.ez2bg.anotherthread.worldgen

/**
 * Whittaker diagram for biome assignment based on elevation and moisture.
 *
 * Elevation: 0.0 (sea level) to 1.0 (mountain peak)
 * Moisture:  0.0 (desert dry) to 1.0 (rainforest wet)
 *
 * Based on Amit Patel's implementation:
 * https://www.redblobgames.com/maps/terrain-from-noise/#biomes
 */
object WhittakerDiagram {

    /**
     * Get biome for land cells based on elevation and moisture.
     *
     * The diagram is divided into 4 elevation bands:
     * - 0.0-0.2: Lowlands
     * - 0.2-0.4: Midlands
     * - 0.4-0.7: Highlands
     * - 0.7-1.0: Mountains
     *
     * Each band has moisture zones from dry (0) to wet (1)
     */
    fun getBiome(elevation: Double, moisture: Double): Biome {
        val e = elevation.coerceIn(0.0, 1.0)
        val m = moisture.coerceIn(0.0, 1.0)

        return when {
            // Mountain peaks (high elevation)
            e > 0.8 -> when {
                m < 0.1 -> Biome.SCORCHED
                m < 0.2 -> Biome.BARE
                m < 0.5 -> Biome.TUNDRA
                else -> Biome.SNOW
            }

            // Highland (medium-high elevation)
            e > 0.6 -> when {
                m < 0.33 -> Biome.TEMPERATE_DESERT
                m < 0.66 -> Biome.SHRUBLAND
                else -> Biome.TAIGA
            }

            // Midland (medium elevation)
            e > 0.3 -> when {
                m < 0.16 -> Biome.TEMPERATE_DESERT
                m < 0.33 -> Biome.GRASSLAND
                m < 0.66 -> Biome.TEMPERATE_DECIDUOUS_FOREST
                else -> Biome.TEMPERATE_RAIN_FOREST
            }

            // Lowland (low elevation)
            else -> when {
                m < 0.16 -> Biome.SUBTROPICAL_DESERT
                m < 0.33 -> Biome.GRASSLAND
                m < 0.66 -> Biome.TROPICAL_SEASONAL_FOREST
                else -> Biome.TROPICAL_RAIN_FOREST
            }
        }
    }

    /**
     * Get biome for water cells.
     */
    fun getWaterBiome(distanceToLand: Int): Biome {
        return when {
            distanceToLand <= 1 -> Biome.COAST
            distanceToLand <= 3 -> Biome.LAKE
            else -> Biome.OCEAN
        }
    }

    /**
     * Check if a cell should be a marsh (water-adjacent lowland with high moisture).
     */
    fun isMarsh(elevation: Double, moisture: Double, isCoastal: Boolean): Boolean {
        return elevation < 0.15 && moisture > 0.75 && isCoastal
    }
}
