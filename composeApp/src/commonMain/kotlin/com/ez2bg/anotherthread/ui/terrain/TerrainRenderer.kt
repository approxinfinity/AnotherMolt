package com.ez2bg.anotherthread.ui.terrain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import com.ez2bg.anotherthread.api.DesertParamsDto
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.ForestParamsDto
import com.ez2bg.anotherthread.api.GrassParamsDto
import com.ez2bg.anotherthread.api.HillsParamsDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.MountainParamsDto
import com.ez2bg.anotherthread.api.RiverParamsDto
import com.ez2bg.anotherthread.api.StreamParamsDto
import com.ez2bg.anotherthread.api.SwampParamsDto
import com.ez2bg.anotherthread.api.TerrainOverridesDto
import com.ez2bg.anotherthread.util.SimplexNoise
import com.ez2bg.anotherthread.util.VoronoiNoise
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Parchment background colors
object ParchmentColors {
    val base = Color(0xFFF5E6C8)          // Main parchment color
    val darkSpot = Color(0xFFD4C4A8)      // Darker aging spots
    val lightSpot = Color(0xFFFFF8E7)     // Lighter worn areas
    val stain = Color(0xFFE8D5B5)         // Tea stain color
    val edge = Color(0xFFCBB896)          // Slightly darker edges
}

// Terrain drawing colors
object TerrainColors {
    val road = Color(0xFFB8976B)           // Tan/brown road
    val roadOutline = Color(0xFF8B7355)    // Darker road edge
    val tree = Color(0xFF4A6741)           // Forest green
    val treeDark = Color(0xFF3A5731)       // Darker foliage shadow
    val treeTrunk = Color(0xFF5A4030)      // Brown trunk
    val water = Color(0xFF6B8E9F)          // Muted blue-gray
    val waterHighlight = Color(0xFF8BB0C4) // Lighter water
    val mountain = Color(0xFF7D7461)       // Gray-brown
    val mountainSnow = Color(0xFFE8E4DC)   // Snow cap
    val grass = Color(0xFF7A9A6D)          // Muted green
    val building = Color(0xFF8B7355)       // Brown buildings
    val cave = Color(0xFF5A5A5A)           // Dark gray
    val sand = Color(0xFFD4C19E)           // Sandy color
    val coast = Color(0xFF8BA4B0)          // Coastal blue-gray
    val swamp = Color(0xFF5A6B52)          // Murky green
    val ruins = Color(0xFF8A7B6A)          // Aged stone
    val ink = Color(0xFF3A3022)            // Dark ink for details
}

// Terrain types for contextual drawing
enum class TerrainType {
    ROAD, FOREST, WATER, STREAM, RIVER, LAKE, MOUNTAIN, GRASS, BUILDING, CAVE, DESERT,
    COAST, HILLS, SWAMP, CHURCH, CASTLE, PORT, RUINS
}

// Data class for neighbor elevation info (used for water flow direction)
data class NeighborElevations(
    val north: Float? = null,
    val south: Float? = null,
    val east: Float? = null,
    val west: Float? = null
)

// Data class for neighbor river info (used for river connections)
data class NeighborRivers(
    val north: Boolean = false,
    val south: Boolean = false,
    val east: Boolean = false,
    val west: Boolean = false
)

// Data class for pass-through features (features that should be drawn because
// they exist on tiles further in that direction, creating visual continuity)
data class PassThroughFeatures(
    // For each terrain type, track which directions have that feature further along
    val riverDirections: Set<ExitDirection> = emptySet(),
    val forestDirections: Set<ExitDirection> = emptySet(),
    val mountainDirections: Set<ExitDirection> = emptySet(),
    val hillsDirections: Set<ExitDirection> = emptySet(),
    val lakeDirections: Set<ExitDirection> = emptySet(),
    val swampDirections: Set<ExitDirection> = emptySet()
) {
    fun hasPassThroughRiver() = riverDirections.size >= 2
    fun hasPassThroughForest() = forestDirections.size >= 2
    fun hasPassThroughMountain() = mountainDirections.size >= 2
    fun hasPassThroughHills() = hillsDirections.size >= 2
    fun hasPassThroughLake() = lakeDirections.size >= 2
    fun hasPassThroughSwamp() = swampDirections.size >= 2
}

// Voronoi-based biome blending for terrain variation
object BiomeBlending {
    data class BiomeBlendResult(
        val primaryWeight: Float,
        val secondaryWeight: Float,
        val edgeDistance: Float,
        val cellId: Int,
        val distanceToCenter: Float
    )

    fun getBlendInfo(
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int,
        cellScale: Float = 0.5f
    ): BiomeBlendResult {
        val scale = terrainSize * cellScale
        val voronoi = VoronoiNoise.cellular(localX, localY, scale, 0.8f, seed)
        val (w1, w2) = voronoi.cellWeights(0.5f)

        return BiomeBlendResult(
            primaryWeight = w1,
            secondaryWeight = w2,
            edgeDistance = voronoi.edgeDistance,
            cellId = voronoi.cellId1,
            distanceToCenter = voronoi.distance1
        )
    }

    fun getColorVariation(
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int,
        variationAmount: Float = 0.15f
    ): Float {
        val blend = getBlendInfo(localX, localY, terrainSize, seed, 0.3f)
        val cellVariation = ((blend.cellId and 0xFF) / 255f) * 2f - 1f
        val edgeFactor = (blend.edgeDistance / (terrainSize * 0.1f)).coerceIn(0f, 1f)
        return cellVariation * variationAmount * (1f - edgeFactor * 0.5f)
    }

    fun getDensityMultiplier(
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int
    ): Float {
        val blend = getBlendInfo(localX, localY, terrainSize, seed + 1000, 0.4f)
        val centerFactor = 1f - (blend.distanceToCenter / (terrainSize * 0.2f)).coerceIn(0f, 0.5f)
        val cellBonus = ((blend.cellId and 0x7F) / 127f) * 0.3f
        return (0.7f + centerFactor * 0.3f + cellBonus).coerceIn(0.5f, 1.3f)
    }

    fun blendColors(
        color1: Color,
        color2: Color,
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int
    ): Color {
        val blend = getBlendInfo(localX, localY, terrainSize, seed, 0.5f)
        val cellPreference = ((blend.cellId and 0xFF) / 255f)
        val factor = if (cellPreference > 0.5f) {
            (0.6f + blend.secondaryWeight * 0.4f).coerceIn(0f, 1f)
        } else {
            (blend.secondaryWeight * 0.4f).coerceIn(0f, 1f)
        }

        return Color(
            red = color1.red * (1f - factor) + color2.red * factor,
            green = color1.green * (1f - factor) + color2.green * factor,
            blue = color1.blue * (1f - factor) + color2.blue * factor,
            alpha = color1.alpha * (1f - factor) + color2.alpha * factor
        )
    }

    fun getEdgeProximity(
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int
    ): Float {
        val blend = getBlendInfo(localX, localY, terrainSize, seed, 0.3f)
        return (1f - blend.primaryWeight).coerceIn(0f, 1f)
    }
}

// Helper extension for keyword matching
fun String.containsAny(vararg keywords: String): Boolean {
    return keywords.any { this.contains(it) }
}

// Parse terrain types from location description
fun parseTerrainFromDescription(desc: String, name: String): Set<TerrainType> {
    val text = (desc + " " + name).lowercase()
    val nameLower = name.lowercase()
    val terrains = mutableSetOf<TerrainType>()

    if (text.containsAny("road", "path", "trail", "highway", "street", "lane", "way")) {
        terrains.add(TerrainType.ROAD)
    }
    if (text.containsAny("forest", "tree", "wood", "grove", "copse", "timber", "oak", "pine", "jungle")) {
        terrains.add(TerrainType.FOREST)
    }
    if (text.containsAny("stream", "creek", "brook")) {
        terrains.add(TerrainType.STREAM)
    }
    if (text.containsAny("river")) {
        terrains.add(TerrainType.RIVER)
    }
    val isLakeByName = nameLower.containsAny("lake", "pond")
    val hasLakeDescription = text.contains(Regex("\\b(the|a|this|in the|on the|of the|into the|across the) (lake|pond)\\b"))
    val isOnLake = text.containsAny("on the lake", "in the lake", "across the lake", "middle of the lake")
    if (isLakeByName || hasLakeDescription || isOnLake) {
        terrains.add(TerrainType.LAKE)
    }
    if (text.containsAny("water", "shore", "bank", "spring", "pool", "falls", "waterfall", "fountain")) {
        terrains.add(TerrainType.WATER)
    }
    if (text.containsAny("mountain", "peak", "summit", "cliff", "crag", "alpine")) {
        terrains.add(TerrainType.MOUNTAIN)
    }
    if (text.containsAny("foothill", "foothills", "hill", "hills", "hilltop", "rolling", "knoll", "mound", "ridge")) {
        terrains.add(TerrainType.HILLS)
    }
    if (text.containsAny("meadow", "field", "plain", "prairie", "grass", "clearing", "pasture", "savanna")) {
        terrains.add(TerrainType.GRASS)
    }
    if (text.containsAny("building", "house", "cottage", "cabin", "shack", "hut", "home", "shop", "inn", "tavern", "store", "barn", "farm", "mill", "tower", "fort", "outpost", "camp")) {
        terrains.add(TerrainType.BUILDING)
    }
    if (text.containsAny("cave", "cavern", "grotto", "underground", "tunnel", "dungeon", "crypt", "catacomb", "mine")) {
        terrains.add(TerrainType.CAVE)
    }
    if (text.containsAny("desert", "dune", "sand", "oasis", "barren", "wasteland", "badland")) {
        terrains.add(TerrainType.DESERT)
    }
    if (text.containsAny("coast", "beach", "shore", "bay", "cove", "harbor", "dock", "pier", "wharf", "marina", "seashore", "seaside", "oceanfront", "seafront")) {
        terrains.add(TerrainType.COAST)
    }
    if (text.containsAny("swamp", "marsh", "bog", "fen", "mire", "wetland", "bayou", "moor")) {
        terrains.add(TerrainType.SWAMP)
    }
    if (text.containsAny("church", "chapel", "temple", "shrine", "monastery", "abbey", "cathedral", "sanctuary", "holy", "sacred")) {
        terrains.add(TerrainType.CHURCH)
    }
    if (text.containsAny("castle", "fortress", "citadel", "stronghold", "keep", "palace", "manor", "estate")) {
        terrains.add(TerrainType.CASTLE)
    }
    if (text.containsAny("port", "harbor", "harbour", "dock", "pier", "wharf", "marina", "quay", "jetty", "mooring", "anchorage", "shipyard", "drydock")) {
        terrains.add(TerrainType.PORT)
    }
    if (text.containsAny("ruin", "ancient", "crumbl", "decay", "abandon", "remnant", "vestige", "wreckage", "debris", "dilapidated")) {
        terrains.add(TerrainType.RUINS)
    }

    return terrains
}

fun elevationDescription(elevation: Float): String = when {
    elevation < -0.5f -> "deep underwater"
    elevation < -0.2f -> "underwater"
    elevation < 0f -> "low-lying"
    elevation < 0.3f -> "flat terrain"
    elevation < 0.5f -> "rolling hills"
    elevation < 0.7f -> "highlands"
    elevation < 0.9f -> "mountains"
    else -> "peak"
}

fun calculateElevationFromTerrain(terrains: Set<TerrainType>, overrideElevation: Float? = null): Float {
    if (overrideElevation != null) return overrideElevation

    var elevation = 0.2f

    if (TerrainType.MOUNTAIN in terrains) elevation = maxOf(elevation, 0.9f)
    if (TerrainType.HILLS in terrains) elevation = maxOf(elevation, 0.5f)
    if (TerrainType.CASTLE in terrains) elevation = maxOf(elevation, 0.4f)
    if (TerrainType.CHURCH in terrains) elevation = maxOf(elevation, 0.3f)
    if (TerrainType.BUILDING in terrains) elevation = maxOf(elevation, 0.25f)
    if (TerrainType.FOREST in terrains) elevation = maxOf(elevation, 0.15f)
    if (TerrainType.GRASS in terrains) elevation = maxOf(elevation, 0.1f)
    if (TerrainType.ROAD in terrains) elevation = maxOf(elevation, 0.05f)
    if (TerrainType.DESERT in terrains) elevation = maxOf(elevation, 0.1f)
    if (TerrainType.SWAMP in terrains) elevation = minOf(elevation, 0f)
    if (TerrainType.COAST in terrains) elevation = minOf(elevation, 0f)
    if (TerrainType.RIVER in terrains) elevation = minOf(elevation, -0.2f)
    if (TerrainType.STREAM in terrains) elevation = minOf(elevation, -0.1f)
    if (TerrainType.LAKE in terrains) elevation = minOf(elevation, -0.4f)
    if (TerrainType.PORT in terrains) elevation = minOf(elevation, -0.1f)

    if (TerrainType.WATER in terrains && TerrainType.LAKE !in terrains && TerrainType.RIVER !in terrains) {
        // Falls/fountains can be at various elevations, leave as-is
    }

    return elevation
}

// Draw parchment background with texture
fun DrawScope.drawParchmentBackground(seed: Int) {
    val random = kotlin.random.Random(seed)

    drawRect(color = ParchmentColors.base)

    val edgeWidth = size.width * 0.15f
    val edgeHeight = size.height * 0.15f

    drawRect(
        color = ParchmentColors.edge.copy(alpha = 0.3f),
        topLeft = Offset.Zero,
        size = Size(size.width, edgeHeight)
    )
    drawRect(
        color = ParchmentColors.edge.copy(alpha = 0.3f),
        topLeft = Offset(0f, size.height - edgeHeight),
        size = Size(size.width, edgeHeight)
    )
    drawRect(
        color = ParchmentColors.edge.copy(alpha = 0.2f),
        topLeft = Offset.Zero,
        size = Size(edgeWidth, size.height)
    )
    drawRect(
        color = ParchmentColors.edge.copy(alpha = 0.2f),
        topLeft = Offset(size.width - edgeWidth, 0f),
        size = Size(edgeWidth, size.height)
    )

    repeat(20) {
        val spotColor = if (random.nextBoolean()) {
            ParchmentColors.darkSpot.copy(alpha = random.nextFloat() * 0.15f)
        } else {
            ParchmentColors.lightSpot.copy(alpha = random.nextFloat() * 0.1f)
        }
        val spotX = random.nextFloat() * size.width
        val spotY = random.nextFloat() * size.height
        val spotRadius = random.nextFloat() * 30f + 10f
        drawCircle(
            color = ParchmentColors.stain.copy(alpha = random.nextFloat() * 0.08f),
            radius = spotRadius,
            center = Offset(spotX, spotY)
        )
    }
}

// Draw terrain-aware path connections with obstacle avoidance
fun DrawScope.drawTerrainAwarePath(
    from: Offset,
    to: Offset,
    color: Color,
    strokeWidth: Float,
    dashLength: Float,
    gapLength: Float,
    seed: Long,
    obstacles: List<Pair<Offset, TerrainType>> = emptyList()
) {
    val random = kotlin.random.Random(seed)
    val dx = to.x - from.x
    val dy = to.y - from.y
    val distance = sqrt(dx * dx + dy * dy)

    if (distance < 1f) return

    // Perpendicular direction for meandering offset
    val perpX = -dy / distance
    val perpY = dx / distance

    // More control points for natural-looking curves
    val numControlPoints = (distance / 30f).toInt().coerceIn(4, 12)
    val controlPoints = mutableListOf<Offset>()
    controlPoints.add(from)

    // Track cumulative offset to create flowing curves
    var cumulativeOffset = 0f
    val maxDeviation = distance * 0.12f  // 12% max deviation
    val momentum = 0.5f

    for (i in 1..numControlPoints) {
        val t = i.toFloat() / (numControlPoints + 1)
        val baseX = from.x + dx * t
        val baseY = from.y + dy * t

        // Calculate obstacle avoidance force
        var avoidanceForce = 0f
        obstacles.forEach { (obstaclePos, _) ->
            val toObstacle = Offset(obstaclePos.x - baseX, obstaclePos.y - baseY)
            val distToObstacle = sqrt(toObstacle.x * toObstacle.x + toObstacle.y * toObstacle.y)

            val influenceRadius = distance * 0.6f
            if (distToObstacle < influenceRadius && distToObstacle > 1f) {
                val side = toObstacle.x * perpX + toObstacle.y * perpY
                val strength = (1f - distToObstacle / influenceRadius) * maxDeviation * 0.8f
                avoidanceForce -= if (side > 0) strength else -strength
            }
        }

        val randomChange = (random.nextFloat() - 0.5f) * 2 * maxDeviation * 0.3f
        cumulativeOffset = cumulativeOffset * momentum + (randomChange + avoidanceForce) * (1 - momentum)
        cumulativeOffset = cumulativeOffset.coerceIn(-maxDeviation, maxDeviation)

        val taperFactor = minOf(t, 1f - t) * 4f
        val taperAmount = taperFactor.coerceIn(0f, 1f)
        val finalOffset = cumulativeOffset * taperAmount

        controlPoints.add(Offset(
            baseX + perpX * finalOffset,
            baseY + perpY * finalOffset
        ))
    }
    controlPoints.add(to)

    // Draw smooth curve through control points
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)

    val path = Path()
    path.moveTo(controlPoints[0].x, controlPoints[0].y)

    for (i in 1 until controlPoints.size - 1) {
        val prev = controlPoints[i - 1]
        val curr = controlPoints[i]
        val next = controlPoints[i + 1]

        val midX1 = (prev.x + curr.x) / 2
        val midY1 = (prev.y + curr.y) / 2
        val midX2 = (curr.x + next.x) / 2
        val midY2 = (curr.y + next.y) / 2

        if (i == 1) {
            path.lineTo(midX1, midY1)
        }
        path.quadraticTo(curr.x, curr.y, midX2, midY2)
    }
    path.lineTo(controlPoints.last().x, controlPoints.last().y)

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = pathEffect
        )
    )
}

// Draw map border decoration
fun DrawScope.drawMapBorder() {
    val borderWidth = 8f
    val innerPadding = 4f

    drawRect(
        color = TerrainColors.ink.copy(alpha = 0.6f),
        topLeft = Offset.Zero,
        size = size,
        style = Stroke(width = borderWidth)
    )
    drawRect(
        color = TerrainColors.ink.copy(alpha = 0.3f),
        topLeft = Offset(innerPadding, innerPadding),
        size = Size(size.width - innerPadding * 2, size.height - innerPadding * 2),
        style = Stroke(width = 2f)
    )

    val cornerSize = 20f
    val corners = listOf(
        Offset(0f, 0f),
        Offset(size.width, 0f),
        Offset(0f, size.height),
        Offset(size.width, size.height)
    )

    corners.forEach { corner ->
        drawCircle(
            color = TerrainColors.ink.copy(alpha = 0.5f),
            radius = cornerSize / 2,
            center = corner
        )
    }
}

// Draw compass rose
fun DrawScope.drawCompassRose(center: Offset, compassSize: Float) {
    val inkColor = TerrainColors.ink.copy(alpha = 0.5f)

    drawCircle(
        color = ParchmentColors.base,
        radius = compassSize,
        center = center
    )
    drawCircle(
        color = inkColor,
        radius = compassSize,
        center = center,
        style = Stroke(width = 2f)
    )

    val directions = listOf(0f, 90f, 180f, 270f)
    directions.forEach { angle ->
        val rad = angle * PI.toFloat() / 180f
        val tipX = center.x + cos(rad) * compassSize * 0.8f
        val tipY = center.y - sin(rad) * compassSize * 0.8f
        val baseX = center.x + cos(rad) * compassSize * 0.2f
        val baseY = center.y - sin(rad) * compassSize * 0.2f

        drawLine(
            color = inkColor,
            start = Offset(baseX, baseY),
            end = Offset(tipX, tipY),
            strokeWidth = 2f
        )
    }

    val subDirections = listOf(45f, 135f, 225f, 315f)
    subDirections.forEach { angle ->
        val rad = angle * PI.toFloat() / 180f
        val tipX = center.x + cos(rad) * compassSize * 0.5f
        val tipY = center.y - sin(rad) * compassSize * 0.5f

        drawLine(
            color = inkColor.copy(alpha = 0.3f),
            start = center,
            end = Offset(tipX, tipY),
            strokeWidth = 1f
        )
    }

    drawCircle(
        color = inkColor,
        radius = compassSize * 0.1f,
        center = center
    )
}

// Draw road terrain
fun DrawScope.drawRoadTerrain(center: Offset, terrainSize: Float, seed: Int, hasRiver: Boolean = false, hasStream: Boolean = false) {
    val random = kotlin.random.Random(seed)

    val roadWidth = terrainSize * 0.15f
    val roadColor = TerrainColors.road
    val outlineColor = TerrainColors.roadOutline

    val startX = center.x - terrainSize * 0.4f
    val endX = center.x + terrainSize * 0.4f
    val baseY = center.y

    val path = Path()
    path.moveTo(startX, baseY + random.nextFloat() * 4f - 2f)

    val segments = 5
    for (i in 1..segments) {
        val x = startX + (endX - startX) * i / segments
        val y = baseY + random.nextFloat() * 6f - 3f
        path.lineTo(x, y)
    }

    drawPath(path, color = outlineColor, style = Stroke(width = roadWidth + 4f, cap = StrokeCap.Round))
    drawPath(path, color = roadColor, style = Stroke(width = roadWidth, cap = StrokeCap.Round))

    val tileSpacing = terrainSize * 0.08f
    var tileX = startX + tileSpacing
    while (tileX < endX - tileSpacing) {
        val tileY = baseY + random.nextFloat() * 3f - 1.5f
        drawLine(
            color = outlineColor.copy(alpha = 0.3f),
            start = Offset(tileX, tileY - roadWidth * 0.3f),
            end = Offset(tileX, tileY + roadWidth * 0.3f),
            strokeWidth = 1f
        )
        tileX += tileSpacing + random.nextFloat() * 3f
    }
}

// Draw forest terrain
fun DrawScope.drawForestTerrain(center: Offset, terrainSize: Float, seed: Int, params: ForestParamsDto? = null) {
    val random = kotlin.random.Random(seed)

    val baseTreeCount = 8
    val treeCount = params?.treeCount ?: (baseTreeCount + random.nextInt(4))
    val sizeMultiplier = params?.sizeMultiplier ?: 1.0f

    val spreadX = terrainSize * 0.35f
    val spreadY = terrainSize * 0.35f

    data class TreeInfo(val x: Float, val y: Float, val size: Float, val depth: Int)

    val trees = mutableListOf<TreeInfo>()

    for (depth in 0..2) {
        val depthTreeCount = when (depth) {
            0 -> treeCount / 3
            1 -> treeCount / 2
            else -> treeCount
        }

        repeat(depthTreeCount) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val distance = random.nextFloat() * 0.9f
            val x = center.x + cos(angle) * spreadX * distance
            val y = center.y + sin(angle) * spreadY * distance
            val baseSize = terrainSize * (0.08f + random.nextFloat() * 0.06f) * sizeMultiplier
            val depthScale = 0.7f + depth * 0.15f
            trees.add(TreeInfo(x, y, baseSize * depthScale, depth))
        }
    }

    trees.sortedWith(compareBy({ it.depth }, { it.y })).forEach { tree ->
        drawTree(tree.x, tree.y, tree.size, seed + tree.x.toInt(), tree.depth)
    }
}

private fun DrawScope.drawTree(x: Float, y: Float, treeSize: Float, seed: Int, depth: Int = 1) {
    val random = kotlin.random.Random(seed)

    val trunkWidth = treeSize * 0.15f
    val trunkHeight = treeSize * 0.4f
    val trunkColor = TerrainColors.treeTrunk.copy(alpha = 0.7f + depth * 0.3f)

    drawLine(
        color = trunkColor,
        start = Offset(x, y),
        end = Offset(x, y - trunkHeight),
        strokeWidth = trunkWidth,
        cap = StrokeCap.Round
    )

    val foliageColor = TerrainColors.tree.copy(alpha = 0.6f + depth * 0.2f)
    val shadowColor = TerrainColors.treeDark.copy(alpha = 0.4f + depth * 0.2f)

    val layers = 3
    for (layer in 0 until layers) {
        val layerY = y - trunkHeight - (layer * treeSize * 0.2f)
        val layerSize = treeSize * (0.9f - layer * 0.2f)

        val path = Path()
        val points = 8
        for (i in 0..points) {
            val angle = (i.toFloat() / points) * 2f * PI.toFloat() - PI.toFloat() / 2
            val wobble = 1f + random.nextFloat() * 0.15f - 0.075f
            val px = x + cos(angle) * layerSize * 0.5f * wobble
            val py = layerY + sin(angle) * layerSize * 0.3f * wobble
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        drawPath(path, color = shadowColor)
        val highlightPath = Path()
        for (i in 0..points) {
            val angle = (i.toFloat() / points) * 2f * PI.toFloat() - PI.toFloat() / 2
            val wobble = 1f + random.nextFloat() * 0.1f - 0.05f
            val px = x + cos(angle) * layerSize * 0.45f * wobble - 1f
            val py = layerY + sin(angle) * layerSize * 0.27f * wobble - 1f
            if (i == 0) highlightPath.moveTo(px, py) else highlightPath.lineTo(px, py)
        }
        highlightPath.close()
        drawPath(highlightPath, color = foliageColor)
    }
}

// Draw water terrain (generic water features)
fun DrawScope.drawWaterTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val waterColor = TerrainColors.water.copy(alpha = 0.6f)
    val highlightColor = TerrainColors.waterHighlight.copy(alpha = 0.4f)

    val poolRadius = terrainSize * 0.2f

    drawCircle(
        color = waterColor,
        radius = poolRadius,
        center = center
    )

    repeat(3) {
        val waveOffset = random.nextFloat() * poolRadius * 0.5f
        drawCircle(
            color = highlightColor.copy(alpha = 0.2f),
            radius = poolRadius * 0.3f + waveOffset,
            center = Offset(
                center.x + random.nextFloat() * 4f - 2f,
                center.y + random.nextFloat() * 4f - 2f
            ),
            style = Stroke(width = 1f)
        )
    }
}

// Draw lake terrain
fun DrawScope.drawLakeTerrain(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    neighborElevations: NeighborElevations? = null
) {
    val random = kotlin.random.Random(seed)

    val waterColor = TerrainColors.water
    val deepColor = Color(0xFF4A7080)
    val shoreColor = TerrainColors.coast.copy(alpha = 0.4f)

    val baseRadius = terrainSize * 0.38f

    val path = Path()
    val points = 16
    for (i in 0..points) {
        val angle = (i.toFloat() / points) * 2f * PI.toFloat()
        val wobble = 0.85f + random.nextFloat() * 0.3f
        val x = center.x + cos(angle) * baseRadius * wobble
        val y = center.y + sin(angle) * baseRadius * wobble * 0.8f
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(path, color = shoreColor)

    val innerPath = Path()
    for (i in 0..points) {
        val angle = (i.toFloat() / points) * 2f * PI.toFloat()
        val wobble = 0.8f + random.nextFloat() * 0.25f
        val x = center.x + cos(angle) * baseRadius * 0.9f * wobble
        val y = center.y + sin(angle) * baseRadius * 0.9f * wobble * 0.8f
        if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
    }
    innerPath.close()

    drawPath(innerPath, color = waterColor)

    val deepPath = Path()
    for (i in 0..points) {
        val angle = (i.toFloat() / points) * 2f * PI.toFloat()
        val wobble = 0.7f + random.nextFloat() * 0.2f
        val x = center.x + cos(angle) * baseRadius * 0.5f * wobble
        val y = center.y + sin(angle) * baseRadius * 0.5f * wobble * 0.8f
        if (i == 0) deepPath.moveTo(x, y) else deepPath.lineTo(x, y)
    }
    deepPath.close()

    drawPath(deepPath, color = deepColor.copy(alpha = 0.6f))

    repeat(4) { i ->
        val waveRadius = baseRadius * (0.3f + i * 0.15f)
        drawCircle(
            color = TerrainColors.waterHighlight.copy(alpha = 0.15f - i * 0.03f),
            radius = waveRadius,
            center = Offset(center.x - 2f, center.y - 2f),
            style = Stroke(width = 1f)
        )
    }
}

// Draw river terrain
fun DrawScope.drawRiverTerrain(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    params: RiverParamsDto? = null,
    hasLake: Boolean = false,
    neighborElevations: NeighborElevations? = null,
    currentElevation: Float = 0f,
    neighborRivers: NeighborRivers? = null
) {
    val random = kotlin.random.Random(seed)

    val widthMultiplier = params?.widthMultiplier ?: 1.0f
    val width = 0.15f * terrainSize * widthMultiplier
    val waterColor = TerrainColors.water
    val bankColor = TerrainColors.coast.copy(alpha = 0.5f)

    val (flowDirX, flowDirY) = calculateFlowDirection(currentElevation, neighborElevations)

    val startX = center.x - flowDirX * terrainSize * 0.45f
    val startY = center.y - flowDirY * terrainSize * 0.45f
    val endX = center.x + flowDirX * terrainSize * 0.45f
    val endY = center.y + flowDirY * terrainSize * 0.45f

    val path = Path()
    path.moveTo(startX, startY)

    val midX = center.x + random.nextFloat() * 15f - 7.5f
    val midY = center.y + random.nextFloat() * 15f - 7.5f
    path.quadraticTo(midX, midY, endX, endY)

    drawPath(path, color = bankColor, style = Stroke(width = width + 6f, cap = StrokeCap.Round))
    drawPath(path, color = waterColor, style = Stroke(width = width, cap = StrokeCap.Round))

    val flowLines = 3
    repeat(flowLines) { i ->
        val offset = (i - flowLines / 2) * width * 0.2f
        val perpX = -flowDirY
        val perpY = flowDirX

        val flowPath = Path()
        flowPath.moveTo(startX + perpX * offset, startY + perpY * offset)
        flowPath.quadraticTo(
            midX + perpX * offset + random.nextFloat() * 3f,
            midY + perpY * offset + random.nextFloat() * 3f,
            endX + perpX * offset,
            endY + perpY * offset
        )

        drawPath(
            flowPath,
            color = TerrainColors.waterHighlight.copy(alpha = 0.3f),
            style = Stroke(width = 1f, cap = StrokeCap.Round)
        )
    }
}

// Draw stream terrain
fun DrawScope.drawStreamTerrain(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    params: StreamParamsDto? = null,
    hasLake: Boolean = false,
    neighborElevations: NeighborElevations? = null,
    currentElevation: Float = 0f,
    neighborRivers: NeighborRivers? = null
) {
    val random = kotlin.random.Random(seed)

    val widthMultiplier = params?.widthMultiplier ?: 1.0f
    val width = 0.06f * terrainSize * widthMultiplier
    val waterColor = TerrainColors.water.copy(alpha = 0.7f)

    val (flowDirX, flowDirY) = calculateFlowDirection(currentElevation, neighborElevations)

    val path = Path()
    val startX = center.x - flowDirX * terrainSize * 0.4f
    val startY = center.y - flowDirY * terrainSize * 0.4f
    path.moveTo(startX, startY)

    val segments = 4
    for (i in 1..segments) {
        val t = i.toFloat() / segments
        val x = center.x + flowDirX * terrainSize * (t - 0.5f) * 0.8f + random.nextFloat() * 8f - 4f
        val y = center.y + flowDirY * terrainSize * (t - 0.5f) * 0.8f + random.nextFloat() * 8f - 4f
        path.lineTo(x, y)
    }

    drawPath(path, color = waterColor, style = Stroke(width = width, cap = StrokeCap.Round))
}

// Calculate flow direction based on elevation
private fun calculateFlowDirection(elevation: Float, neighbors: NeighborElevations?): Pair<Float, Float> {
    if (neighbors == null) return Pair(1f, 0f)

    var dx = 0f
    var dy = 0f

    neighbors.east?.let { if (it < elevation) dx += (elevation - it) }
    neighbors.west?.let { if (it < elevation) dx -= (elevation - it) }
    neighbors.south?.let { if (it < elevation) dy += (elevation - it) }
    neighbors.north?.let { if (it < elevation) dy -= (elevation - it) }

    val length = sqrt(dx * dx + dy * dy)
    return if (length > 0.01f) Pair(dx / length, dy / length) else Pair(1f, 0f)
}

// Draw foothills terrain
fun DrawScope.drawFoothillsTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val hillColor = Color(0xFF8B9A6B)
    val shadowColor = Color(0xFF6B7A5B)

    repeat(5) {
        val hillX = center.x + random.nextFloat() * terrainSize * 0.6f - terrainSize * 0.3f
        val hillY = center.y + random.nextFloat() * terrainSize * 0.6f - terrainSize * 0.3f
        val hillWidth = terrainSize * (0.15f + random.nextFloat() * 0.1f)
        val hillHeight = terrainSize * (0.08f + random.nextFloat() * 0.05f)

        val path = Path()
        path.moveTo(hillX - hillWidth, hillY)
        path.quadraticTo(hillX, hillY - hillHeight, hillX + hillWidth, hillY)
        path.close()

        drawPath(path, color = shadowColor.copy(alpha = 0.4f))
        drawPath(path, color = hillColor.copy(alpha = 0.6f), style = Stroke(width = 2f))
    }
}

// Draw mountain terrain
fun DrawScope.drawMountainTerrain(center: Offset, terrainSize: Float, seed: Int, params: MountainParamsDto? = null) {
    val random = kotlin.random.Random(seed)

    val heightMultiplier = params?.heightMultiplier ?: 1.0f
    val peakHeight = 0.8f * terrainSize * 0.5f * heightMultiplier
    val peakCount = params?.peakCount ?: (2 + random.nextInt(2))
    val rockColor = TerrainColors.mountain
    val snowColor = TerrainColors.mountainSnow
    val shadowColor = Color(0xFF5A5448)

    repeat(peakCount) { i ->
        val peakX = center.x + (i - peakCount / 2f) * terrainSize * 0.2f + random.nextFloat() * 10f - 5f
        val baseY = center.y + terrainSize * 0.15f
        val peakY = baseY - peakHeight * (0.8f + random.nextFloat() * 0.4f)
        val baseWidth = terrainSize * (0.2f + random.nextFloat() * 0.1f)

        val path = Path()
        path.moveTo(peakX - baseWidth, baseY)

        val leftRidgeX = peakX - baseWidth * 0.3f
        val leftRidgeY = peakY + peakHeight * 0.4f
        path.lineTo(leftRidgeX, leftRidgeY)
        path.lineTo(peakX + random.nextFloat() * 3f - 1.5f, peakY)

        val rightRidgeX = peakX + baseWidth * 0.3f
        val rightRidgeY = peakY + peakHeight * 0.3f
        path.lineTo(rightRidgeX, rightRidgeY)
        path.lineTo(peakX + baseWidth, baseY)
        path.close()

        drawPath(path, color = shadowColor.copy(alpha = 0.5f))
        drawPath(path, color = rockColor, style = Stroke(width = 2f))

        // Draw snow cap on peaks
        val snowPath = Path()
        snowPath.moveTo(peakX, peakY)
        snowPath.lineTo(leftRidgeX + baseWidth * 0.1f, leftRidgeY - peakHeight * 0.1f)
        snowPath.lineTo(peakX, peakY + peakHeight * 0.2f)
        snowPath.lineTo(rightRidgeX - baseWidth * 0.1f, rightRidgeY - peakHeight * 0.05f)
        snowPath.close()

        drawPath(snowPath, color = snowColor.copy(alpha = 0.8f))
    }
}

// Draw grass terrain
fun DrawScope.drawGrassTerrain(center: Offset, terrainSize: Float, seed: Int, params: GrassParamsDto? = null) {
    val random = kotlin.random.Random(seed)

    val tuftCount = params?.tuftCount ?: 12
    val grassColor = TerrainColors.grass
    val darkGrass = Color(0xFF5A7A4D)

    val clumpCount = tuftCount.coerceAtLeast(4)

    repeat(clumpCount) {
        val clumpX = center.x + random.nextFloat() * terrainSize * 0.7f - terrainSize * 0.35f
        val clumpY = center.y + random.nextFloat() * terrainSize * 0.7f - terrainSize * 0.35f

        val bladesInClump = 3 + random.nextInt(4)
        repeat(bladesInClump) {
            val bladeX = clumpX + random.nextFloat() * 6f - 3f
            val bladeY = clumpY
            val bladeHeight = terrainSize * (0.03f + random.nextFloat() * 0.02f)
            val bladeTilt = random.nextFloat() * 4f - 2f

            val color = if (random.nextBoolean()) grassColor else darkGrass

            drawLine(
                color = color.copy(alpha = 0.5f + random.nextFloat() * 0.3f),
                start = Offset(bladeX, bladeY),
                end = Offset(bladeX + bladeTilt, bladeY - bladeHeight),
                strokeWidth = 1f,
                cap = StrokeCap.Round
            )
        }
    }

    if (random.nextFloat() < 0.3f) {
        val flowerX = center.x + random.nextFloat() * terrainSize * 0.4f - terrainSize * 0.2f
        val flowerY = center.y + random.nextFloat() * terrainSize * 0.4f - terrainSize * 0.2f
        val flowerColor = listOf(
            Color(0xFFE8D44D),
            Color(0xFFE87D4D),
            Color(0xFF8B4DE8)
        ).random()

        drawCircle(
            color = flowerColor.copy(alpha = 0.7f),
            radius = 2f,
            center = Offset(flowerX, flowerY)
        )
    }
}

// Draw building terrain
fun DrawScope.drawBuildingTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val buildingWidth = terrainSize * 0.2f
    val buildingHeight = terrainSize * 0.15f
    val roofHeight = terrainSize * 0.1f

    val wallColor = TerrainColors.building
    val roofColor = Color(0xFF6B4A35)

    val housePath = Path()
    val left = center.x - buildingWidth / 2
    val right = center.x + buildingWidth / 2
    val bottom = center.y + buildingHeight / 2
    val top = center.y - buildingHeight / 2

    housePath.moveTo(left, bottom)
    housePath.lineTo(left, top)
    housePath.lineTo(center.x, top - roofHeight)
    housePath.lineTo(right, top)
    housePath.lineTo(right, bottom)
    housePath.close()

    drawPath(housePath, color = TerrainColors.building)
    drawPath(housePath, color = roofColor, style = Stroke(width = 2f))

    val doorWidth = buildingWidth * 0.2f
    val doorHeight = buildingHeight * 0.4f
    drawRect(
        color = roofColor,
        topLeft = Offset(center.x - doorWidth / 2, bottom - doorHeight),
        size = Size(doorWidth, doorHeight)
    )
}

// Draw cave terrain
fun DrawScope.drawCaveTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val caveColor = TerrainColors.cave
    val entranceWidth = terrainSize * 0.15f
    val entranceHeight = terrainSize * 0.12f

    val path = Path()
    path.moveTo(center.x - entranceWidth, center.y + entranceHeight / 2)
    path.quadraticTo(center.x, center.y - entranceHeight, center.x + entranceWidth, center.y + entranceHeight / 2)
    path.close()

    drawPath(path, color = caveColor)
    drawPath(path, color = Color.Black.copy(alpha = 0.6f), style = Stroke(width = 2f))
}

// Draw desert terrain
fun DrawScope.drawDesertTerrain(center: Offset, terrainSize: Float, seed: Int, params: DesertParamsDto? = null) {
    val random = kotlin.random.Random(seed)

    val sandColor = TerrainColors.sand
    val shadowColor = Color(0xFFB8A080)
    val duneCount = (params?.duneCount ?: 3).coerceIn(1, 6)

    repeat(duneCount) { i ->
        val duneX = center.x + (i - duneCount / 2f) * terrainSize * 0.25f + random.nextFloat() * 10f - 5f
        val duneY = center.y + random.nextFloat() * terrainSize * 0.3f - terrainSize * 0.15f
        val duneWidth = terrainSize * (0.2f + random.nextFloat() * 0.1f)
        val duneHeight = terrainSize * (0.05f + random.nextFloat() * 0.03f)

        val dunePath = Path()
        dunePath.moveTo(duneX - duneWidth, duneY)
        dunePath.quadraticTo(duneX - duneWidth * 0.3f, duneY - duneHeight, duneX, duneY - duneHeight * 0.5f)
        dunePath.quadraticTo(duneX + duneWidth * 0.3f, duneY - duneHeight * 0.8f, duneX + duneWidth, duneY)
        dunePath.close()

        drawPath(dunePath, color = shadowColor.copy(alpha = 0.3f))
        drawPath(dunePath, color = sandColor.copy(alpha = 0.6f), style = Stroke(width = 1f))
    }

    if (random.nextFloat() < 0.2f) {
        val cactusX = center.x + random.nextFloat() * terrainSize * 0.4f - terrainSize * 0.2f
        val cactusY = center.y + random.nextFloat() * terrainSize * 0.2f
        val cactusHeight = terrainSize * 0.08f

        drawLine(
            color = Color(0xFF4A7A4A),
            start = Offset(cactusX, cactusY),
            end = Offset(cactusX, cactusY - cactusHeight),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF4A7A4A),
            start = Offset(cactusX - 4f, cactusY - cactusHeight * 0.6f),
            end = Offset(cactusX - 4f, cactusY - cactusHeight * 0.8f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

// Draw boulder
fun DrawScope.drawBoulder(x: Float, y: Float, boulderSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val rockColor = TerrainColors.mountain.copy(alpha = 0.7f)
    val shadowColor = Color(0xFF4A4A3A).copy(alpha = 0.5f)

    val path = Path()
    val points = 6
    for (i in 0..points) {
        val angle = (i.toFloat() / points) * 2f * PI.toFloat()
        val wobble = 0.7f + random.nextFloat() * 0.3f
        val px = x + cos(angle) * boulderSize * wobble
        val py = y + sin(angle) * boulderSize * 0.6f * wobble
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()

    drawPath(path, color = shadowColor)
    drawPath(path, color = rockColor, style = Stroke(width = 1f))
}

// Draw hills terrain
fun DrawScope.drawHillsTerrain(center: Offset, terrainSize: Float, seed: Int, params: HillsParamsDto? = null) {
    val random = kotlin.random.Random(seed)

    val heightMultiplier = params?.heightMultiplier ?: 1.0f
    val hillCount = (4 * heightMultiplier).toInt().coerceIn(2, 8)
    val hillColor = Color(0xFF7A9A6B)
    val shadowColor = Color(0xFF5A7A4B)

    data class HillInfo(val x: Float, val y: Float, val width: Float, val height: Float)

    val hills = mutableListOf<HillInfo>()
    repeat(hillCount) {
        val hillX = center.x + random.nextFloat() * terrainSize * 0.7f - terrainSize * 0.35f
        val hillY = center.y + random.nextFloat() * terrainSize * 0.5f - terrainSize * 0.1f
        val hillWidth = terrainSize * (0.15f + random.nextFloat() * 0.15f)
        val hillHeight = terrainSize * (0.08f + random.nextFloat() * 0.06f)
        hills.add(HillInfo(hillX, hillY, hillWidth, hillHeight))
    }

    hills.sortedBy { it.y }.forEach { hill ->
        val path = Path()
        path.moveTo(hill.x - hill.width, hill.y)
        path.quadraticTo(hill.x - hill.width * 0.5f, hill.y - hill.height * 0.7f, hill.x, hill.y - hill.height)
        path.quadraticTo(hill.x + hill.width * 0.5f, hill.y - hill.height * 0.7f, hill.x + hill.width, hill.y)
        path.close()

        drawPath(path, color = shadowColor.copy(alpha = 0.4f))
        drawPath(path, color = hillColor.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
    }
}

// Draw coast terrain
fun DrawScope.drawCoastTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val sandColor = TerrainColors.sand.copy(alpha = 0.7f)
    val waterColor = TerrainColors.coast.copy(alpha = 0.5f)
    val foamColor = Color.White.copy(alpha = 0.4f)

    drawRect(
        color = sandColor,
        topLeft = Offset(center.x - terrainSize * 0.4f, center.y - terrainSize * 0.2f),
        size = Size(terrainSize * 0.8f, terrainSize * 0.4f)
    )

    val waterY = center.y + terrainSize * 0.1f
    repeat(3) { i ->
        val waveY = waterY + i * 8f
        val wavePath = Path()
        wavePath.moveTo(center.x - terrainSize * 0.4f, waveY)

        var x = center.x - terrainSize * 0.4f
        while (x < center.x + terrainSize * 0.4f) {
            val waveHeight = 3f + random.nextFloat() * 2f
            wavePath.quadraticTo(x + 10f, waveY - waveHeight, x + 20f, waveY)
            x += 20f
        }

        drawPath(wavePath, color = if (i == 0) foamColor else waterColor, style = Stroke(width = 2f))
    }
}

// Draw ship (for port terrain)
fun DrawScope.drawShip(center: Offset, shipSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val hullColor = Color(0xFF5A4030)
    val sailColor = Color(0xFFF5F0E0)

    val hullPath = Path()
    hullPath.moveTo(center.x - shipSize * 0.4f, center.y)
    hullPath.lineTo(center.x - shipSize * 0.3f, center.y + shipSize * 0.2f)
    hullPath.lineTo(center.x + shipSize * 0.3f, center.y + shipSize * 0.2f)
    hullPath.lineTo(center.x + shipSize * 0.5f, center.y)
    hullPath.close()

    drawPath(hullPath, color = hullColor)

    drawLine(
        color = hullColor,
        start = Offset(center.x, center.y),
        end = Offset(center.x, center.y - shipSize * 0.5f),
        strokeWidth = 2f
    )

    val sailPath = Path()
    sailPath.moveTo(center.x, center.y - shipSize * 0.5f)
    sailPath.lineTo(center.x + shipSize * 0.3f, center.y - shipSize * 0.3f)
    sailPath.lineTo(center.x, center.y - shipSize * 0.1f)
    sailPath.close()

    drawPath(sailPath, color = sailColor)
}

// Draw swamp terrain
fun DrawScope.drawSwampTerrain(center: Offset, terrainSize: Float, seed: Int, params: SwampParamsDto? = null) {
    val random = kotlin.random.Random(seed)

    val densityMultiplier = params?.densityMultiplier ?: 1.0f
    val swampColor = TerrainColors.swamp
    val waterColor = Color(0xFF4A5A42).copy(alpha = 0.5f * densityMultiplier)
    val reedColor = Color(0xFF6A7A52)

    drawCircle(
        color = waterColor,
        radius = terrainSize * 0.35f,
        center = center
    )

    repeat(8) {
        val mudX = center.x + random.nextFloat() * terrainSize * 0.6f - terrainSize * 0.3f
        val mudY = center.y + random.nextFloat() * terrainSize * 0.6f - terrainSize * 0.3f

        drawCircle(
            color = swampColor.copy(alpha = 0.4f),
            radius = terrainSize * (0.03f + random.nextFloat() * 0.02f),
            center = Offset(mudX, mudY)
        )
    }

    repeat(12) {
        val reedX = center.x + random.nextFloat() * terrainSize * 0.5f - terrainSize * 0.25f
        val reedY = center.y + random.nextFloat() * terrainSize * 0.5f - terrainSize * 0.25f
        val reedHeight = terrainSize * (0.04f + random.nextFloat() * 0.03f)
        val reedTilt = random.nextFloat() * 3f - 1.5f

        drawLine(
            color = reedColor.copy(alpha = 0.6f),
            start = Offset(reedX, reedY),
            end = Offset(reedX + reedTilt, reedY - reedHeight),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
    }
}

// Draw castle terrain
fun DrawScope.drawCastleTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val stoneColor = Color(0xFF7A7060)
    val shadowColor = Color(0xFF5A5040)

    val baseWidth = terrainSize * 0.3f
    val baseHeight = terrainSize * 0.2f
    val towerWidth = terrainSize * 0.08f
    val towerHeight = terrainSize * 0.15f

    drawRect(
        color = stoneColor,
        topLeft = Offset(center.x - baseWidth / 2, center.y - baseHeight / 2),
        size = Size(baseWidth, baseHeight)
    )

    listOf(-1f, 1f).forEach { side ->
        val towerX = center.x + side * (baseWidth / 2 - towerWidth / 2)
        drawRect(
            color = shadowColor,
            topLeft = Offset(towerX - towerWidth / 2, center.y - baseHeight / 2 - towerHeight),
            size = Size(towerWidth, towerHeight + baseHeight / 2)
        )

        repeat(3) { i ->
            val crenelX = towerX - towerWidth / 2 + i * towerWidth / 3
            drawRect(
                color = stoneColor,
                topLeft = Offset(crenelX, center.y - baseHeight / 2 - towerHeight - 4f),
                size = Size(towerWidth / 4, 4f)
            )
        }
    }
}

// Draw church terrain
fun DrawScope.drawChurchTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val wallColor = Color(0xFFF0E8D8)
    val roofColor = Color(0xFF6B4A35)

    val width = terrainSize * 0.15f
    val height = terrainSize * 0.12f
    val steepleHeight = terrainSize * 0.15f

    drawRect(
        color = wallColor,
        topLeft = Offset(center.x - width / 2, center.y - height / 2),
        size = Size(width, height)
    )

    val roofPath = Path()
    roofPath.moveTo(center.x - width / 2 - 2f, center.y - height / 2)
    roofPath.lineTo(center.x, center.y - height / 2 - steepleHeight)
    roofPath.lineTo(center.x + width / 2 + 2f, center.y - height / 2)
    roofPath.close()

    drawPath(roofPath, color = roofColor)

    val crossSize = terrainSize * 0.03f
    val crossY = center.y - height / 2 - steepleHeight - crossSize
    drawLine(
        color = Color(0xFF4A3A2A),
        start = Offset(center.x, crossY - crossSize),
        end = Offset(center.x, crossY + crossSize),
        strokeWidth = 2f
    )
    drawLine(
        color = Color(0xFF4A3A2A),
        start = Offset(center.x - crossSize * 0.6f, crossY),
        end = Offset(center.x + crossSize * 0.6f, crossY),
        strokeWidth = 2f
    )
}

// Draw port terrain
fun DrawScope.drawPortTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val waterColor = TerrainColors.water.copy(alpha = 0.6f)
    val woodColor = Color(0xFF6B5030)

    drawRect(
        color = waterColor,
        topLeft = Offset(center.x - terrainSize * 0.35f, center.y),
        size = Size(terrainSize * 0.7f, terrainSize * 0.25f)
    )

    val dockY = center.y + terrainSize * 0.05f
    drawRect(
        color = woodColor,
        topLeft = Offset(center.x - terrainSize * 0.2f, dockY - 3f),
        size = Size(terrainSize * 0.4f, 6f)
    )

    repeat(4) { i ->
        val postX = center.x - terrainSize * 0.15f + i * terrainSize * 0.1f
        drawLine(
            color = woodColor,
            start = Offset(postX, dockY),
            end = Offset(postX, dockY + terrainSize * 0.08f),
            strokeWidth = 3f
        )
    }

    drawShip(Offset(center.x + terrainSize * 0.15f, center.y + terrainSize * 0.12f), terrainSize * 0.15f, seed)
}

// Draw ruins terrain
fun DrawScope.drawRuinsTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    val stoneColor = TerrainColors.ruins
    val shadowColor = Color(0xFF6A5B4A)

    repeat(5) {
        val blockX = center.x + random.nextFloat() * terrainSize * 0.5f - terrainSize * 0.25f
        val blockY = center.y + random.nextFloat() * terrainSize * 0.4f - terrainSize * 0.2f
        val blockWidth = terrainSize * (0.05f + random.nextFloat() * 0.04f)
        val blockHeight = terrainSize * (0.03f + random.nextFloat() * 0.03f)

        val tilt = random.nextFloat() * 0.3f - 0.15f

        drawRect(
            color = shadowColor.copy(alpha = 0.5f),
            topLeft = Offset(blockX + 2f, blockY + 2f),
            size = Size(blockWidth, blockHeight)
        )
        drawRect(
            color = stoneColor.copy(alpha = 0.7f),
            topLeft = Offset(blockX, blockY),
            size = Size(blockWidth, blockHeight)
        )
    }

    repeat(2) {
        val pillarX = center.x + (it - 0.5f) * terrainSize * 0.3f + random.nextFloat() * 5f - 2.5f
        val pillarY = center.y + random.nextFloat() * terrainSize * 0.2f - terrainSize * 0.1f
        val pillarHeight = terrainSize * (0.08f + random.nextFloat() * 0.04f)
        val pillarWidth = terrainSize * 0.025f

        drawRect(
            color = stoneColor,
            topLeft = Offset(pillarX - pillarWidth / 2, pillarY - pillarHeight),
            size = Size(pillarWidth, pillarHeight)
        )
    }
}

// Master terrain drawing function
fun DrawScope.drawLocationTerrain(
    location: LocationDto,
    center: Offset,
    terrainSize: Float,
    overrides: TerrainOverridesDto? = null,
    neighborElevations: NeighborElevations? = null,
    neighborRivers: NeighborRivers? = null,
    passThrough: PassThroughFeatures = PassThroughFeatures()
) {
    val terrains = parseTerrainFromDescription(location.desc, location.name)
    val seed = location.id.hashCode()

    val elevation = calculateElevationFromTerrain(terrains, overrides?.elevation)

    drawElevationShading(center, terrainSize, elevation, seed)

    if (passThrough.hasPassThroughRiver() && TerrainType.RIVER !in terrains) {
        drawPassThroughRiver(center, terrainSize, seed, passThrough.riverDirections)
    }
    if (passThrough.hasPassThroughForest() && TerrainType.FOREST !in terrains) {
        drawPassThroughForest(center, terrainSize, seed, passThrough.forestDirections)
    }
    if (passThrough.hasPassThroughHills() && TerrainType.HILLS !in terrains) {
        drawPassThroughHills(center, terrainSize, seed, passThrough.hillsDirections)
    }
    if (passThrough.hasPassThroughMountain() && TerrainType.MOUNTAIN !in terrains) {
        drawPassThroughMountain(center, terrainSize, seed, passThrough.mountainDirections)
    }
    if (passThrough.hasPassThroughSwamp() && TerrainType.SWAMP !in terrains) {
        drawPassThroughSwamp(center, terrainSize, seed, passThrough.swampDirections)
    }
    if (passThrough.hasPassThroughLake() && TerrainType.LAKE !in terrains) {
        drawPassThroughLake(center, terrainSize, seed, passThrough.lakeDirections)
    }

    if (TerrainType.GRASS in terrains) {
        drawGrassTerrain(center, terrainSize, seed, overrides?.grass)
    }
    if (TerrainType.DESERT in terrains) {
        drawDesertTerrain(center, terrainSize, seed, overrides?.desert)
    }
    if (TerrainType.HILLS in terrains) {
        drawHillsTerrain(center, terrainSize, seed, overrides?.hills)
    }
    if (TerrainType.SWAMP in terrains) {
        drawSwampTerrain(center, terrainSize, seed, overrides?.swamp)
    }
    if (TerrainType.COAST in terrains) {
        drawCoastTerrain(center, terrainSize, seed)
    }
    if (TerrainType.LAKE in terrains) {
        drawLakeTerrain(center, terrainSize, seed, neighborElevations)
    }
    if (TerrainType.WATER in terrains && TerrainType.LAKE !in terrains) {
        drawWaterTerrain(center, terrainSize, seed)
    }
    if (TerrainType.RIVER in terrains) {
        drawRiverTerrain(center, terrainSize, seed, overrides?.river, TerrainType.LAKE in terrains, neighborElevations, elevation, neighborRivers)
    }
    if (TerrainType.STREAM in terrains) {
        drawStreamTerrain(center, terrainSize, seed, overrides?.stream, TerrainType.LAKE in terrains, neighborElevations, elevation, neighborRivers)
    }
    if (TerrainType.FOREST in terrains) {
        drawForestTerrain(center, terrainSize, seed, overrides?.forest)
    }
    if (TerrainType.MOUNTAIN in terrains) {
        drawMountainTerrain(center, terrainSize, seed, overrides?.mountain)
    }
    if (TerrainType.ROAD in terrains) {
        drawRoadTerrain(center, terrainSize, seed, TerrainType.RIVER in terrains, TerrainType.STREAM in terrains)
    }
    if (TerrainType.RUINS in terrains) {
        drawRuinsTerrain(center, terrainSize, seed)
    }
    if (TerrainType.BUILDING in terrains) {
        drawBuildingTerrain(center, terrainSize, seed)
    }
    if (TerrainType.CHURCH in terrains) {
        drawChurchTerrain(center, terrainSize, seed)
    }
    if (TerrainType.CASTLE in terrains) {
        drawCastleTerrain(center, terrainSize, seed)
    }
    if (TerrainType.PORT in terrains) {
        drawPortTerrain(center, terrainSize, seed)
    }
    if (TerrainType.CAVE in terrains) {
        drawCaveTerrain(center, terrainSize, seed)
    }
}

// Draw elevation shading
private fun DrawScope.drawElevationShading(
    center: Offset,
    terrainSize: Float,
    elevation: Float,
    seed: Int
) {
    val shadingAlpha = when {
        elevation > 0.7f -> 0.15f
        elevation > 0.4f -> 0.08f
        elevation < -0.2f -> 0.1f
        else -> 0f
    }

    if (shadingAlpha > 0f) {
        val shadingColor = if (elevation > 0f) {
            Color(0xFF8B7355).copy(alpha = shadingAlpha)
        } else {
            Color(0xFF4A6B7A).copy(alpha = shadingAlpha)
        }

        drawCircle(
            color = shadingColor,
            radius = terrainSize * 0.4f,
            center = center
        )
    }
}

// Pass-through terrain drawing functions
private fun DrawScope.drawPassThroughRiver(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val waterColor = TerrainColors.water.copy(alpha = 0.4f)
    val random = kotlin.random.Random(seed)

    directions.forEach { dir ->
        val (dx, dy) = getDirectionVector(dir)
        val startX = center.x + dx * terrainSize * 0.35f
        val startY = center.y + dy * terrainSize * 0.35f
        val endX = center.x + dx * terrainSize * 0.5f
        val endY = center.y + dy * terrainSize * 0.5f

        drawLine(
            color = waterColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = terrainSize * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawPassThroughForest(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val random = kotlin.random.Random(seed)

    directions.forEach { dir ->
        val (dx, dy) = getDirectionVector(dir)
        repeat(2) { i ->
            val distance = 0.35f + i * 0.1f
            val treeX = center.x + dx * terrainSize * distance + random.nextFloat() * 10f - 5f
            val treeY = center.y + dy * terrainSize * distance + random.nextFloat() * 10f - 5f
            drawTree(treeX, treeY, terrainSize * 0.06f, seed + i, 0)
        }
    }
}

private fun DrawScope.drawPassThroughHills(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val hillColor = Color(0xFF7A9A6B).copy(alpha = 0.3f)
    val random = kotlin.random.Random(seed)

    directions.forEach { dir ->
        val (dx, dy) = getDirectionVector(dir)
        val hillX = center.x + dx * terrainSize * 0.4f
        val hillY = center.y + dy * terrainSize * 0.4f

        val path = Path()
        path.moveTo(hillX - terrainSize * 0.1f, hillY)
        path.quadraticTo(hillX, hillY - terrainSize * 0.05f, hillX + terrainSize * 0.1f, hillY)
        path.close()

        drawPath(path, color = hillColor)
    }
}

private fun DrawScope.drawPassThroughMountain(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val rockColor = TerrainColors.mountain.copy(alpha = 0.4f)

    directions.forEach { dir ->
        val (dx, dy) = getDirectionVector(dir)
        val peakX = center.x + dx * terrainSize * 0.4f
        val peakY = center.y + dy * terrainSize * 0.4f

        val path = Path()
        path.moveTo(peakX - terrainSize * 0.08f, peakY)
        path.lineTo(peakX, peakY - terrainSize * 0.1f)
        path.lineTo(peakX + terrainSize * 0.08f, peakY)
        path.close()

        drawPath(path, color = rockColor)
    }
}

private fun DrawScope.drawPassThroughSwamp(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val swampColor = TerrainColors.swamp.copy(alpha = 0.3f)
    val random = kotlin.random.Random(seed)

    directions.forEach { dir ->
        val (dx, dy) = getDirectionVector(dir)
        val swampX = center.x + dx * terrainSize * 0.4f
        val swampY = center.y + dy * terrainSize * 0.4f

        drawCircle(
            color = swampColor,
            radius = terrainSize * 0.08f,
            center = Offset(swampX, swampY)
        )
    }
}

private fun DrawScope.drawPassThroughLake(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val waterColor = TerrainColors.water.copy(alpha = 0.3f)
    val shoreColor = TerrainColors.coast.copy(alpha = 0.2f)

    val channelPath = Path()
    var firstPoint = true

    directions.sortedBy { it.ordinal }.forEach { dir ->
        val (dx, dy) = getDirectionVector(dir)
        val edgeX = center.x + dx * terrainSize * 0.45f
        val edgeY = center.y + dy * terrainSize * 0.45f

        if (firstPoint) {
            channelPath.moveTo(edgeX, edgeY)
            firstPoint = false
        } else {
            channelPath.lineTo(edgeX, edgeY)
        }
    }

    drawPath(channelPath, color = shoreColor, style = Stroke(width = 18f, cap = StrokeCap.Round))
    drawPath(channelPath, color = waterColor, style = Stroke(width = 12f, cap = StrokeCap.Round))
    drawPath(channelPath, color = TerrainColors.water.copy(alpha = 0.5f), style = Stroke(width = 5f, cap = StrokeCap.Round))
}

// Get direction vector for exit direction
private fun getDirectionVector(direction: ExitDirection): Pair<Float, Float> = when (direction) {
    ExitDirection.NORTH -> Pair(0f, -1f)
    ExitDirection.NORTHEAST -> Pair(0.707f, -0.707f)
    ExitDirection.EAST -> Pair(1f, 0f)
    ExitDirection.SOUTHEAST -> Pair(0.707f, 0.707f)
    ExitDirection.SOUTH -> Pair(0f, 1f)
    ExitDirection.SOUTHWEST -> Pair(-0.707f, 0.707f)
    ExitDirection.WEST -> Pair(-1f, 0f)
    ExitDirection.NORTHWEST -> Pair(-0.707f, -0.707f)
    ExitDirection.ENTER -> Pair(0f, 0f)
    ExitDirection.UNKNOWN -> Pair(0f, 0f)
}
