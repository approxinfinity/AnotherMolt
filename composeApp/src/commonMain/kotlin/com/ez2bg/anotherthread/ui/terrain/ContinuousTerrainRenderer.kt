package com.ez2bg.anotherthread.ui.terrain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.TerrainOverridesDto
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Terrain info for a single location, derived from biome metadata (preferred) or description parsing (fallback).
 */
data class LocationTerrainInfo(
    val terrains: Set<TerrainType>,
    val elevation: Float,
    val moisture: Float,
    val isRiver: Boolean,
    val isCoast: Boolean,
    val biome: String?
)

/**
 * A connected river path as a series of screen-space control points.
 */
data class RiverPath(
    val points: List<Offset>,
    val width: Float
)

/**
 * A connected lake region with a smoothed boundary polygon.
 */
data class LakeRegion(
    val boundary: List<Offset>,
    val center: Offset,
    val locationIds: Set<String>
)

/**
 * A connected forest region with scattered tree positions.
 */
data class ForestRegion(
    val treePositions: List<TreePlacement>,
    val locationIds: Set<String>
)

data class TreePlacement(
    val x: Float,
    val y: Float,
    val size: Float,
    val seed: Int,
    val depth: Int
)

/**
 * A connected mountain ridge with peak positions.
 */
data class MountainRidge(
    val peaks: List<Offset>,
    val ridgePath: List<Offset>,
    val locationIds: Set<String>
)

/**
 * Pre-computed continuous terrain data for an area.
 */
data class ContinuousTerrainData(
    val terrainInfoMap: Map<String, LocationTerrainInfo>,
    val riverPaths: List<RiverPath>,
    val lakeRegions: List<LakeRegion>,
    val forestRegions: List<ForestRegion>,
    val mountainRidges: List<MountainRidge>,
    val coordToLocationId: Map<Pair<Int, Int>, String>
)

/**
 * Builds terrain info for a location, preferring biome metadata over description parsing.
 */
fun buildTerrainInfo(location: LocationDto, overrides: TerrainOverridesDto? = null): LocationTerrainInfo {
    val terrains = if (location.biome != null) {
        terrainFromBiome(location.biome, location.isRiver ?: false, location.isCoast ?: false)
    } else {
        parseTerrainFromDescription(location.desc, location.name)
    }

    val elevation = if (location.elevation != null) {
        location.elevation
    } else {
        calculateElevationFromTerrain(terrains, overrides?.elevation)
    }

    return LocationTerrainInfo(
        terrains = terrains,
        elevation = elevation,
        moisture = location.moisture ?: 0.5f,
        isRiver = location.isRiver ?: (TerrainType.RIVER in terrains || TerrainType.STREAM in terrains),
        isCoast = location.isCoast ?: (TerrainType.COAST in terrains),
        biome = location.biome
    )
}

/**
 * Convert server-side biome name to terrain types.
 */
fun terrainFromBiome(biome: String, isRiver: Boolean, isCoast: Boolean): Set<TerrainType> {
    val terrains = mutableSetOf<TerrainType>()

    when (biome.uppercase()) {
        "TROPICAL_RAIN_FOREST", "TEMPERATE_RAIN_FOREST" -> {
            terrains.add(TerrainType.FOREST)
            terrains.add(TerrainType.GRASS)
        }
        "TROPICAL_SEASONAL_FOREST", "TEMPERATE_DECIDUOUS_FOREST" -> {
            terrains.add(TerrainType.FOREST)
        }
        "GRASSLAND", "TROPICAL_GRASSLAND" -> terrains.add(TerrainType.GRASS)
        "SHRUBLAND" -> terrains.add(TerrainType.GRASS)
        "TAIGA" -> {
            terrains.add(TerrainType.FOREST)
            terrains.add(TerrainType.HILLS)
        }
        "TUNDRA" -> terrains.add(TerrainType.GRASS)
        "DESERT", "SUBTROPICAL_DESERT", "TEMPERATE_DESERT" -> terrains.add(TerrainType.DESERT)
        "MARSH" -> terrains.add(TerrainType.SWAMP)
        "LAKE" -> terrains.add(TerrainType.LAKE)
        "OCEAN", "DEEP_OCEAN" -> terrains.add(TerrainType.WATER)
        "MOUNTAIN", "SNOW" -> terrains.add(TerrainType.MOUNTAIN)
        "BARE", "SCORCHED" -> terrains.add(TerrainType.DESERT)
        else -> terrains.add(TerrainType.GRASS)
    }

    if (isRiver) terrains.add(TerrainType.RIVER)
    if (isCoast) terrains.add(TerrainType.COAST)

    return terrains
}

/**
 * Pre-compute continuous terrain data for all locations in an area.
 * This performs flood-fill analysis to find connected features.
 */
fun computeContinuousTerrain(
    locations: List<LocationDto>,
    locationPositions: Map<String, Any>,
    getScreenPos: (Any) -> Offset,
    terrainSize: Float,
    overridesMap: Map<String, TerrainOverridesDto> = emptyMap()
): ContinuousTerrainData {
    // Build terrain info map
    val terrainInfoMap = locations.associate { loc ->
        loc.id to buildTerrainInfo(loc, overridesMap[loc.id])
    }

    // Build coord -> locationId map
    val coordToId = mutableMapOf<Pair<Int, Int>, String>()
    locations.forEach { loc ->
        if (loc.gridX != null && loc.gridY != null) {
            coordToId[Pair(loc.gridX, loc.gridY)] = loc.id
        }
    }

    // Build adjacency map from exits
    val adjacencyMap = mutableMapOf<String, MutableList<String>>()
    locations.forEach { loc ->
        loc.exits.forEach { exit ->
            if (exit.locationId in terrainInfoMap) {
                adjacencyMap.getOrPut(loc.id) { mutableListOf() }.add(exit.locationId)
            }
        }
    }

    // Flood-fill to find connected river cells
    val riverPaths = traceRiverPaths(locations, terrainInfoMap, adjacencyMap, locationPositions, getScreenPos, terrainSize)

    // Flood-fill to find connected lake regions
    val lakeRegions = findLakeRegions(locations, terrainInfoMap, adjacencyMap, locationPositions, getScreenPos, terrainSize)

    // Flood-fill to find connected forest regions
    val forestRegions = findForestRegions(locations, terrainInfoMap, adjacencyMap, locationPositions, getScreenPos, terrainSize)

    // Flood-fill to find connected mountain ridges
    val mountainRidges = findMountainRidges(locations, terrainInfoMap, adjacencyMap, locationPositions, getScreenPos, terrainSize)

    return ContinuousTerrainData(
        terrainInfoMap = terrainInfoMap,
        riverPaths = riverPaths,
        lakeRegions = lakeRegions,
        forestRegions = forestRegions,
        mountainRidges = mountainRidges,
        coordToLocationId = coordToId
    )
}

// ==================== River Tracing ====================

/**
 * Trace connected river cells into smooth paths.
 * Starts at highest-elevation river cells and follows neighbors downhill.
 */
private fun traceRiverPaths(
    locations: List<LocationDto>,
    terrainInfoMap: Map<String, LocationTerrainInfo>,
    adjacencyMap: Map<String, List<String>>,
    locationPositions: Map<String, Any>,
    getScreenPos: (Any) -> Offset,
    terrainSize: Float
): List<RiverPath> {
    val riverLocIds = terrainInfoMap.filter { it.value.isRiver }.keys
    if (riverLocIds.isEmpty()) return emptyList()

    val visited = mutableSetOf<String>()
    val paths = mutableListOf<RiverPath>()

    // Sort river cells by elevation (highest first) to start paths at sources
    val sortedRiverLocs = riverLocIds.sortedByDescending { terrainInfoMap[it]?.elevation ?: 0f }

    for (startId in sortedRiverLocs) {
        if (startId in visited) continue

        // Trace a path from this source
        val pathIds = mutableListOf<String>()
        var currentId: String? = startId

        while (currentId != null && currentId !in visited) {
            visited.add(currentId)
            pathIds.add(currentId)

            val currentElev = terrainInfoMap[currentId]?.elevation ?: 0f
            val neighbors = adjacencyMap[currentId] ?: emptyList()

            // Follow the lowest-elevation river neighbor
            currentId = neighbors
                .filter { it in riverLocIds && it !in visited }
                .minByOrNull { terrainInfoMap[it]?.elevation ?: Float.MAX_VALUE }
        }

        // Also check if we stopped at a known visited node - connect to it
        if (currentId != null && currentId in visited && pathIds.size > 1) {
            pathIds.add(currentId) // Connect to existing path
        }

        if (pathIds.size >= 2) {
            val points = pathIds.mapNotNull { id ->
                val pos = locationPositions[id] ?: return@mapNotNull null
                getScreenPos(pos)
            }
            if (points.size >= 2) {
                paths.add(RiverPath(
                    points = points,
                    width = terrainSize * 0.12f
                ))
            }
        }
    }

    return paths
}

// ==================== Lake Regions ====================

/**
 * Find connected groups of lake/water cells and compute boundary polygons.
 */
private fun findLakeRegions(
    locations: List<LocationDto>,
    terrainInfoMap: Map<String, LocationTerrainInfo>,
    adjacencyMap: Map<String, List<String>>,
    locationPositions: Map<String, Any>,
    getScreenPos: (Any) -> Offset,
    terrainSize: Float
): List<LakeRegion> {
    val lakeIds = terrainInfoMap.filter { info ->
        TerrainType.LAKE in info.value.terrains || TerrainType.WATER in info.value.terrains
    }.keys

    if (lakeIds.isEmpty()) return emptyList()

    val visited = mutableSetOf<String>()
    val regions = mutableListOf<LakeRegion>()

    for (startId in lakeIds) {
        if (startId in visited) continue

        // BFS to find connected lake cells
        val group = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(startId)

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id in visited) continue
            visited.add(id)
            group.add(id)

            val neighbors = adjacencyMap[id] ?: emptyList()
            for (nId in neighbors) {
                if (nId !in visited && nId in lakeIds) {
                    queue.add(nId)
                }
            }
        }

        if (group.size >= 1) {
            val positions = group.mapNotNull { id ->
                val pos = locationPositions[id] ?: return@mapNotNull null
                getScreenPos(pos)
            }
            if (positions.isNotEmpty()) {
                val center = Offset(
                    positions.map { it.x }.average().toFloat(),
                    positions.map { it.y }.average().toFloat()
                )
                val boundary = computeLakeBoundary(positions, terrainSize)
                regions.add(LakeRegion(boundary = boundary, center = center, locationIds = group))
            }
        }
    }

    return regions
}

/**
 * Compute an organic boundary polygon around a set of lake cell positions.
 * Uses convex hull + Chaikin subdivision for smooth curves.
 */
private fun computeLakeBoundary(positions: List<Offset>, terrainSize: Float): List<Offset> {
    if (positions.size == 1) {
        // Single cell: draw a circle-like polygon
        val center = positions[0]
        val radius = terrainSize * 0.42f
        return (0 until 16).map { i ->
            val angle = (i / 16f) * 2f * PI.toFloat()
            val wobble = 0.9f + Random(center.hashCode() + i).nextFloat() * 0.2f
            Offset(
                center.x + cos(angle) * radius * wobble,
                center.y + sin(angle) * radius * wobble * 0.85f
            )
        }
    }

    // For multi-cell lakes, expand each position into circle points and find convex hull
    val expandedPoints = mutableListOf<Offset>()
    val expansionRadius = terrainSize * 0.42f

    positions.forEach { pos ->
        val seed = pos.hashCode()
        for (i in 0 until 8) {
            val angle = (i / 8f) * 2f * PI.toFloat()
            val wobble = 0.85f + Random(seed + i).nextFloat() * 0.3f
            expandedPoints.add(Offset(
                pos.x + cos(angle) * expansionRadius * wobble,
                pos.y + sin(angle) * expansionRadius * wobble * 0.9f
            ))
        }
    }

    val hull = convexHull(expandedPoints)

    // Apply Chaikin smoothing (2 iterations)
    return chaikinSmooth(chaikinSmooth(hull))
}

/**
 * Compute convex hull using Graham scan.
 */
private fun convexHull(points: List<Offset>): List<Offset> {
    if (points.size < 3) return points

    val sorted = points.sortedWith(compareBy<Offset> { it.y }.thenBy { it.x })
    val pivot = sorted.first()

    val byAngle = sorted.drop(1).sortedBy {
        kotlin.math.atan2((it.y - pivot.y).toDouble(), (it.x - pivot.x).toDouble())
    }

    val hull = mutableListOf(pivot)

    for (p in byAngle) {
        while (hull.size >= 2) {
            val a = hull[hull.size - 2]
            val b = hull[hull.size - 1]
            val cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
            if (cross <= 0) hull.removeAt(hull.size - 1)
            else break
        }
        hull.add(p)
    }

    return hull
}

/**
 * Chaikin corner-cutting subdivision for smooth curves.
 */
private fun chaikinSmooth(polygon: List<Offset>): List<Offset> {
    if (polygon.size < 3) return polygon
    val result = mutableListOf<Offset>()
    for (i in polygon.indices) {
        val p0 = polygon[i]
        val p1 = polygon[(i + 1) % polygon.size]
        result.add(Offset(
            p0.x * 0.75f + p1.x * 0.25f,
            p0.y * 0.75f + p1.y * 0.25f
        ))
        result.add(Offset(
            p0.x * 0.25f + p1.x * 0.75f,
            p0.y * 0.25f + p1.y * 0.75f
        ))
    }
    return result
}

// ==================== Forest Regions ====================

/**
 * Find connected forest regions and scatter trees using Poisson-disk-like placement.
 */
private fun findForestRegions(
    locations: List<LocationDto>,
    terrainInfoMap: Map<String, LocationTerrainInfo>,
    adjacencyMap: Map<String, List<String>>,
    locationPositions: Map<String, Any>,
    getScreenPos: (Any) -> Offset,
    terrainSize: Float
): List<ForestRegion> {
    val forestIds = terrainInfoMap.filter { TerrainType.FOREST in it.value.terrains }.keys
    if (forestIds.isEmpty()) return emptyList()

    val visited = mutableSetOf<String>()
    val regions = mutableListOf<ForestRegion>()

    for (startId in forestIds) {
        if (startId in visited) continue

        // BFS to find connected forest cells
        val group = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(startId)

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id in visited) continue
            visited.add(id)
            group.add(id)

            val neighbors = adjacencyMap[id] ?: emptyList()
            for (nId in neighbors) {
                if (nId !in visited && nId in forestIds) {
                    queue.add(nId)
                }
            }
        }

        // Scatter trees across the entire region
        val trees = scatterTrees(group, locationPositions, getScreenPos, terrainSize)
        if (trees.isNotEmpty()) {
            regions.add(ForestRegion(treePositions = trees, locationIds = group))
        }
    }

    return regions
}

/**
 * Scatter trees across a connected forest region using pseudo-Poisson-disk sampling.
 * Trees are placed within the combined area of all cells, ignoring tile boundaries.
 */
private fun scatterTrees(
    locationIds: Set<String>,
    locationPositions: Map<String, Any>,
    getScreenPos: (Any) -> Offset,
    terrainSize: Float
): List<TreePlacement> {
    val positions = locationIds.mapNotNull { id ->
        val pos = locationPositions[id] ?: return@mapNotNull null
        id to getScreenPos(pos)
    }
    if (positions.isEmpty()) return emptyList()

    val trees = mutableListOf<TreePlacement>()
    val minDist = terrainSize * 0.15f // Minimum distance between trees

    // For each cell in the region, place trees with some overlap at edges
    for ((id, center) in positions) {
        val seed = id.hashCode()
        val random = Random(seed)
        val treeCount = 6 + random.nextInt(4) // 6-9 trees per cell

        for (i in 0 until treeCount) {
            val treeRandom = Random(seed + i * 31)
            // Place in a slightly wider area than the tile (allowing overlap)
            val spread = terrainSize * 0.45f
            val tx = center.x + (treeRandom.nextFloat() - 0.5f) * 2f * spread
            val ty = center.y + (treeRandom.nextFloat() - 0.5f) * 2f * spread

            // Check if this position is close enough to any cell center in the group
            val isNearGroupCell = positions.any { (_, cellCenter) ->
                val dx = tx - cellCenter.x
                val dy = ty - cellCenter.y
                sqrt(dx * dx + dy * dy) < terrainSize * 0.55f
            }

            if (isNearGroupCell) {
                val size = terrainSize * (0.08f + treeRandom.nextFloat() * 0.06f)
                val depth = treeRandom.nextInt(3)
                trees.add(TreePlacement(
                    x = tx, y = ty,
                    size = size * (0.7f + depth * 0.15f),
                    seed = seed + i,
                    depth = depth
                ))
            }
        }
    }

    // Sort by depth then y for painter's order
    return trees.sortedWith(compareBy<TreePlacement> { it.depth }.thenBy { it.y })
}

// ==================== Mountain Ridges ====================

/**
 * Find connected high-elevation cells and create ridgeline paths.
 */
private fun findMountainRidges(
    locations: List<LocationDto>,
    terrainInfoMap: Map<String, LocationTerrainInfo>,
    adjacencyMap: Map<String, List<String>>,
    locationPositions: Map<String, Any>,
    getScreenPos: (Any) -> Offset,
    terrainSize: Float
): List<MountainRidge> {
    val mountainIds = terrainInfoMap.filter { TerrainType.MOUNTAIN in it.value.terrains }.keys
    if (mountainIds.isEmpty()) return emptyList()

    val visited = mutableSetOf<String>()
    val ridges = mutableListOf<MountainRidge>()

    for (startId in mountainIds) {
        if (startId in visited) continue

        // BFS to find connected mountain cells
        val group = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(startId)

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id in visited) continue
            visited.add(id)
            group.add(id)

            val neighbors = adjacencyMap[id] ?: emptyList()
            for (nId in neighbors) {
                if (nId !in visited && nId in mountainIds) {
                    queue.add(nId)
                }
            }
        }

        // Get positions and create ridge
        val groupPositions = group.mapNotNull { id ->
            val pos = locationPositions[id] ?: return@mapNotNull null
            id to getScreenPos(pos)
        }

        if (groupPositions.isNotEmpty()) {
            // Sort by x then y to create a ridge line
            val sortedPositions = groupPositions.sortedWith(
                compareBy<Pair<String, Offset>> { it.second.x }.thenBy { it.second.y }
            )

            val ridgePath = sortedPositions.map { it.second }
            val peaks = sortedPositions.map { (id, pos) ->
                // Add some variation to peak positions
                val random = Random(id.hashCode())
                Offset(
                    pos.x + (random.nextFloat() - 0.5f) * terrainSize * 0.2f,
                    pos.y - terrainSize * 0.15f + (random.nextFloat() - 0.5f) * terrainSize * 0.1f
                )
            }

            ridges.add(MountainRidge(
                peaks = peaks,
                ridgePath = ridgePath,
                locationIds = group
            ))
        }
    }

    return ridges
}

// ==================== Drawing Functions ====================

/**
 * Draw subtle elevation indicators (Layer 2A).
 * Faint ink contour marks at high-elevation and low-elevation cells.
 * Replaces colored radial gradients with parchment-friendly ink marks.
 */
fun DrawScope.drawElevationGradients(
    locations: List<LocationDto>,
    terrainData: ContinuousTerrainData,
    locationPositions: Map<String, Any>,
    getScreenPos: (Any) -> Offset,
    terrainSize: Float
) {
    locations.forEach { loc ->
        val pos = locationPositions[loc.id] ?: return@forEach
        val screenPos = getScreenPos(pos)
        val info = terrainData.terrainInfoMap[loc.id] ?: return@forEach
        val seed = loc.id.hashCode()
        val random = Random(seed)

        val elevation = info.elevation

        // High elevation: faint concentric contour arcs
        if (elevation > 0.55f) {
            val intensity = ((elevation - 0.55f) / 0.45f).coerceIn(0f, 1f)
            val numRings = (1 + (intensity * 2).toInt()).coerceAtMost(3)
            for (ring in 0 until numRings) {
                val radius = terrainSize * (0.25f + ring * 0.12f)
                val arcPoints = (0 until 8).map { i ->
                    val angle = (i / 8f) * 2f * PI.toFloat()
                    val wobble = 1f + (Random(seed + ring * 100 + i).nextFloat() - 0.5f) * 0.15f
                    Offset(
                        screenPos.x + cos(angle) * radius * wobble,
                        screenPos.y + sin(angle) * radius * wobble * 0.8f
                    )
                }
                drawWobblyPath(
                    points = arcPoints,
                    closed = true,
                    color = TerrainColors.ink.copy(alpha = 0.06f * intensity),
                    config = WobbleConfig(roughness = 0.5f, bowing = 0.3f, doubleStroke = false, strokeWidth = 0.7f, seed = seed + ring)
                )
            }
        }

        // Low elevation / water areas: subtle parchment darkening (ink wash)
        if (elevation < -0.1f) {
            val intensity = ((-0.1f - elevation) / 0.9f).coerceIn(0f, 1f)
            val radius = terrainSize * 0.4f
            drawCircle(
                color = TerrainColors.ink.copy(alpha = 0.03f * intensity),
                radius = radius,
                center = screenPos
            )
        }
    }
}

/**
 * Draw lake regions with ink hachure fill and wobbly boundary (Layer 2B).
 * Watabou-style: hatched water + coastline hatching marks around edge.
 */
fun DrawScope.drawLakeRegions(terrainData: ContinuousTerrainData, terrainSize: Float) {
    terrainData.lakeRegions.forEach { lake ->
        if (lake.boundary.size < 3) return@forEach

        val seed = lake.center.hashCode()

        // Hachure fill for water
        drawHachureFill(
            boundary = lake.boundary,
            color = TerrainColors.ink.copy(alpha = 0.15f),
            config = HachureConfig(
                angle = -45f,
                gap = 6f,
                wobbleConfig = WobbleConfig(roughness = 0.6f, doubleStroke = false, strokeWidth = 0.8f, seed = seed)
            )
        )

        // Wobbly boundary outline
        drawWobblyPath(
            points = lake.boundary,
            closed = true,
            color = TerrainColors.ink.copy(alpha = 0.5f),
            config = WobbleConfig(roughness = 1.0f, bowing = 0.8f, doubleStroke = true, strokeWidth = 1.5f, seed = seed + 100)
        )

        // Coast hatching around the outer edge
        drawCoastHatching(
            shoreline = lake.boundary,
            layers = 3,
            maxDistance = terrainSize * 0.12f,
            color = TerrainColors.ink.copy(alpha = 0.2f),
            inward = false,
            seed = seed + 200
        )
    }
}

/**
 * Draw river paths as ink double-lines with wobble (Layer 2C).
 * Watabou-style: two wobbly strokes for each river, tapering downstream.
 */
fun DrawScope.drawRiverPaths(terrainData: ContinuousTerrainData) {
    terrainData.riverPaths.forEach { river ->
        if (river.points.size < 2) return@forEach

        val seed = (river.points[0].x * 1000 + river.points[0].y).toInt()

        // Build Catmull-Rom path with wobbled control points
        for (pass in 0..1) {
            val path = Path()
            val wobbleAmount = river.width * 0.15f
            val passRandom = Random(seed + pass * 3571)

            if (river.points.size == 2) {
                val w1 = (passRandom.nextFloat() - 0.5f) * wobbleAmount
                val w2 = (passRandom.nextFloat() - 0.5f) * wobbleAmount
                path.moveTo(river.points[0].x + w1, river.points[0].y + w2)
                path.lineTo(river.points[1].x + w1, river.points[1].y + w2)
            } else {
                val w0 = (passRandom.nextFloat() - 0.5f) * wobbleAmount
                path.moveTo(river.points[0].x + w0, river.points[0].y + w0)

                for (i in 0 until river.points.size - 1) {
                    val p0 = if (i > 0) river.points[i - 1] else river.points[i]
                    val p1 = river.points[i]
                    val p2 = river.points[i + 1]
                    val p3 = if (i + 2 < river.points.size) river.points[i + 2] else river.points[i + 1]

                    val segRandom = Random(seed + pass * 3571 + i * 37)
                    val w = wobbleAmount * (segRandom.nextFloat() - 0.5f)

                    val cp1x = p1.x + (p2.x - p0.x) / 6f + w
                    val cp1y = p1.y + (p2.y - p0.y) / 6f + w * 0.7f
                    val cp2x = p2.x - (p3.x - p1.x) / 6f + w * 0.5f
                    val cp2y = p2.y - (p3.y - p1.y) / 6f + w * 0.3f

                    // Width tapers: thinner at source (start), wider downstream (end)
                    val progress = i.toFloat() / (river.points.size - 1)
                    val endW = (passRandom.nextFloat() - 0.5f) * wobbleAmount * (0.5f + progress * 0.5f)

                    path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x + endW, p2.y + endW * 0.5f)
                }
            }

            val alpha = if (pass == 0) 0.6f else 0.3f
            val widthMul = if (pass == 0) 1.0f else 0.7f

            drawPath(
                path = path,
                color = TerrainColors.ink.copy(alpha = alpha),
                style = Stroke(
                    width = river.width * widthMul,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Draw forest trees scattered across regions, ignoring tile boundaries (Layer 2D).
 */
fun DrawScope.drawForestRegions(terrainData: ContinuousTerrainData) {
    terrainData.forestRegions.forEach { forest ->
        forest.treePositions.forEach { tree ->
            drawContinuousTree(tree.x, tree.y, tree.size, tree.seed, tree.depth)
        }
    }
}

/**
 * Draw a single tree as a small ink icon (Watabou-style).
 * Simple trunk line + wobbly circular canopy outline - no solid fills.
 */
private fun DrawScope.drawContinuousTree(x: Float, y: Float, treeSize: Float, seed: Int, depth: Int) {
    val random = Random(seed)
    val alpha = 0.4f + depth * 0.15f
    val inkColor = TerrainColors.ink.copy(alpha = alpha)

    // Trunk: simple wobbly line
    val trunkHeight = treeSize * 0.35f
    val trunkPath = wobbleLine(
        Offset(x, y),
        Offset(x, y - trunkHeight),
        WobbleConfig(roughness = 0.5f, bowing = 0.3f, doubleStroke = false, strokeWidth = 1.5f, seed = seed)
    )
    drawPath(trunkPath, inkColor, style = Stroke(width = 1.5f, cap = StrokeCap.Round))

    // Canopy: wobbly circle outline (ink, no fill)
    val canopyRadius = treeSize * 0.35f
    val canopyCenterY = y - trunkHeight - canopyRadius * 0.4f
    val numPoints = 10
    val canopyPoints = (0 until numPoints).map { i ->
        val angle = (i.toFloat() / numPoints) * 2f * PI.toFloat()
        val wobble = 1f + (Random(seed + i * 17).nextFloat() - 0.5f) * 0.3f
        Offset(
            x + cos(angle) * canopyRadius * wobble,
            canopyCenterY + sin(angle) * canopyRadius * 0.7f * wobble
        )
    }
    drawWobblyPath(
        points = canopyPoints,
        closed = true,
        color = inkColor,
        config = WobbleConfig(roughness = 0.8f, bowing = 0.6f, doubleStroke = false, strokeWidth = 1.2f, seed = seed + 100)
    )
}

/**
 * Draw mountains as Watabou-style ink peaks with shadow hatching (Layer 2E).
 */
fun DrawScope.drawMountainRidges(terrainData: ContinuousTerrainData, terrainSize: Float) {
    terrainData.mountainRidges.forEach { ridge ->
        if (ridge.ridgePath.isEmpty()) return@forEach

        // Draw peaks at each cell - no connecting ridgeline (cleaner watabou look)
        ridge.peaks.forEachIndexed { index, peak ->
            val cellCenter = ridge.ridgePath.getOrNull(index) ?: return@forEachIndexed
            val seed = (cellCenter.x * 1000 + cellCenter.y).toInt()
            drawWatabouMountainPeak(cellCenter, terrainSize, seed)
        }
    }
}

/**
 * Draw a Watabou-style mountain peak: triangular ink outline with hachure shadow on left side.
 */
private fun DrawScope.drawWatabouMountainPeak(
    cellCenter: Offset,
    terrainSize: Float,
    seed: Int
) {
    val random = Random(seed)
    val peakCount = 1 + random.nextInt(2) // 1-2 peaks per cell

    for (p in 0 until peakCount) {
        val offsetX = (p - peakCount / 2f) * terrainSize * 0.22f + (random.nextFloat() - 0.5f) * terrainSize * 0.05f
        val peakHeight = terrainSize * (0.35f + random.nextFloat() * 0.15f)
        val baseHalfWidth = terrainSize * (0.12f + random.nextFloat() * 0.05f)

        val baseLeft = Offset(cellCenter.x + offsetX - baseHalfWidth, cellCenter.y + terrainSize * 0.1f)
        val baseRight = Offset(cellCenter.x + offsetX + baseHalfWidth, cellCenter.y + terrainSize * 0.1f)
        val apex = Offset(
            cellCenter.x + offsetX + (random.nextFloat() - 0.5f) * 3f,
            cellCenter.y + terrainSize * 0.1f - peakHeight
        )

        val peakSeed = seed + p * 1000
        val inkColor = TerrainColors.ink

        // Draw the triangular outline with wobble
        val triangle = listOf(baseLeft, apex, baseRight)
        drawWobblyPath(
            points = triangle,
            closed = true,
            color = inkColor.copy(alpha = 0.7f),
            config = WobbleConfig(roughness = 0.8f, bowing = 0.6f, doubleStroke = true, strokeWidth = 1.5f, seed = peakSeed)
        )

        // Shadow hatching on left side only (watabou signature look)
        val leftShadow = listOf(
            baseLeft,
            apex,
            Offset(
                (baseLeft.x + apex.x) / 2f + baseHalfWidth * 0.15f,
                (baseLeft.y + apex.y) / 2f
            )
        )
        drawHachureFill(
            boundary = leftShadow,
            color = inkColor.copy(alpha = 0.25f),
            config = HachureConfig(
                angle = 60f,
                gap = 3f,
                wobbleConfig = WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 0.8f, seed = peakSeed + 500)
            )
        )

        // Optional snow cap at very top (small wobbly triangle)
        if (random.nextFloat() > 0.4f) {
            val snowTop = 0.15f // Top 15% of peak
            val snowLeft = Offset(
                baseLeft.x + (apex.x - baseLeft.x) * (1f - snowTop),
                baseLeft.y + (apex.y - baseLeft.y) * (1f - snowTop)
            )
            val snowRight = Offset(
                baseRight.x + (apex.x - baseRight.x) * (1f - snowTop),
                baseRight.y + (apex.y - baseRight.y) * (1f - snowTop)
            )
            val snowPath = buildWobblyPath(
                listOf(snowLeft, apex, snowRight),
                closed = true,
                WobbleConfig(roughness = 0.5f, bowing = 0.3f, strokeWidth = 1f, seed = peakSeed + 900)
            )
            drawPath(snowPath, TerrainColors.mountainSnow.copy(alpha = 0.6f))
        }
    }
}

/**
 * Draw point features (buildings, castles, churches, caves, ruins, roads) per-tile.
 * These are NOT continuous features - they belong to individual tiles.
 * This delegates to the existing per-tile TerrainRenderer functions.
 */
fun DrawScope.drawPointFeatures(
    locations: List<LocationDto>,
    terrainData: ContinuousTerrainData,
    locationPositions: Map<String, Any>,
    getScreenPos: (Any) -> Offset,
    terrainSize: Float,
    overridesMap: Map<String, TerrainOverridesDto> = emptyMap()
) {
    val pointFeatureTypes = setOf(
        TerrainType.BUILDING, TerrainType.CASTLE, TerrainType.CHURCH,
        TerrainType.CAVE, TerrainType.RUINS, TerrainType.PORT, TerrainType.ROAD
    )

    locations.forEach { loc ->
        val pos = locationPositions[loc.id] ?: return@forEach
        val screenPos = getScreenPos(pos)
        val info = terrainData.terrainInfoMap[loc.id] ?: return@forEach
        val overrides = overridesMap[loc.id]
        val seed = loc.id.hashCode()

        // Only draw point features (buildings, castles, etc.) - not continuous terrain
        val pointTerrains = info.terrains.filter { it in pointFeatureTypes }

        for (terrain in pointTerrains) {
            when (terrain) {
                TerrainType.ROAD -> drawRoadTerrain(screenPos, terrainSize, seed)
                TerrainType.BUILDING -> drawBuildingTerrain(screenPos, terrainSize, seed)
                TerrainType.CAVE -> drawCaveTerrain(screenPos, terrainSize, seed)
                TerrainType.RUINS -> drawRuinsTerrain(screenPos, terrainSize, seed)
                TerrainType.CHURCH -> drawChurchTerrain(screenPos, terrainSize, seed)
                TerrainType.CASTLE -> drawCastleTerrain(screenPos, terrainSize, seed)
                TerrainType.PORT -> drawPortTerrain(screenPos, terrainSize, seed)
                else -> {}
            }
        }

        // Also draw grass/desert/swamp/hills/coast as per-tile base terrain
        // (these don't have continuous variants but provide ground texture)
        val baseTerrains = info.terrains.filter {
            it in setOf(TerrainType.GRASS, TerrainType.DESERT, TerrainType.SWAMP,
                TerrainType.HILLS, TerrainType.COAST, TerrainType.STREAM)
        }
        for (terrain in baseTerrains) {
            when (terrain) {
                TerrainType.GRASS -> drawGrassTerrain(screenPos, terrainSize, seed, overrides?.grass)
                TerrainType.DESERT -> drawDesertTerrain(screenPos, terrainSize, seed, overrides?.desert)
                TerrainType.SWAMP -> drawSwampTerrain(screenPos, terrainSize, seed, overrides?.swamp)
                TerrainType.HILLS -> drawHillsTerrain(screenPos, terrainSize, seed, overrides?.hills)
                TerrainType.COAST -> drawCoastTerrain(screenPos, terrainSize, seed)
                TerrainType.STREAM -> drawStreamTerrain(screenPos, terrainSize, seed, overrides?.stream)
                else -> {}
            }
        }
    }
}
