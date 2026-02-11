package com.ez2bg.anotherthread.ui.terrain

import androidx.compose.ui.geometry.Offset
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
import kotlin.math.cos
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

// Draw parchment background with noise-based aging stains
fun DrawScope.drawParchmentBackground(seed: Int) {
    val random = kotlin.random.Random(seed)

    // Base parchment fill
    drawRect(color = ParchmentColors.base)

    // Edge darkening
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

    // Noise-based organic staining using SimplexNoise
    val noiseScale = 0.008f
    val stainCount = 40
    for (i in 0 until stainCount) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val noiseVal = com.ez2bg.anotherthread.util.SimplexNoise.noise2DNormalized(
            x * noiseScale + seed * 0.1f,
            y * noiseScale + seed * 0.07f
        )

        if (noiseVal > 0.55f) {
            val intensity = (noiseVal - 0.55f) * 2.2f
            val stainRadius = 12f + intensity * 35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ParchmentColors.stain.copy(alpha = intensity * 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = stainRadius
                ),
                radius = stainRadius,
                center = Offset(x, y)
            )
        }
    }

    // Faint fiber/grain lines for paper texture
    val grainCount = 8
    for (i in 0 until grainCount) {
        val y = random.nextFloat() * size.height
        val startX = random.nextFloat() * size.width * 0.3f
        val endX = startX + size.width * (0.3f + random.nextFloat() * 0.4f)
        val grainPath = wobbleLine(
            Offset(startX, y),
            Offset(endX, y + (random.nextFloat() - 0.5f) * 5f),
            WobbleConfig(roughness = 0.4f, bowing = 0.3f, strokeWidth = 0.5f, seed = seed + i * 71)
        )
        drawPath(
            grainPath,
            ParchmentColors.darkSpot.copy(alpha = 0.04f),
            style = Stroke(width = 0.5f, cap = StrokeCap.Round)
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

// Draw forest terrain - scattered ink tree icons
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

// Draw single tree as ink icon - wobbly trunk + wobbly circle canopy
private fun DrawScope.drawTree(x: Float, y: Float, treeSize: Float, seed: Int, depth: Int = 1) {
    val ink = TerrainColors.ink
    val depthAlpha = 0.25f + depth * 0.1f

    val trunkHeight = treeSize * 0.4f

    // Trunk - simple wobbly line
    drawWobblyLine(
        Offset(x, y), Offset(x, y - trunkHeight),
        ink.copy(alpha = depthAlpha),
        WobbleConfig(roughness = 0.4f, bowing = 0.3f, doubleStroke = false, strokeWidth = 1.2f, seed = seed)
    )

    // Canopy - wobbly circle outline (no fill, ink outline only)
    val canopyY = y - trunkHeight - treeSize * 0.15f
    val canopyRadius = treeSize * 0.3f
    val canopyPoints = (0..9).map { i ->
        val angle = (i / 10f) * 2f * PI.toFloat()
        Offset(
            x + cos(angle) * canopyRadius,
            canopyY + sin(angle) * canopyRadius * 0.8f
        )
    }
    drawWobblyPath(
        points = canopyPoints, closed = true,
        color = ink.copy(alpha = depthAlpha),
        config = WobbleConfig(roughness = 0.7f, bowing = 0.5f, doubleStroke = false, strokeWidth = 1f, seed = seed + 50)
    )
}

// Draw water terrain (generic water features) - wobbly circle with hachure
fun DrawScope.drawWaterTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val ink = TerrainColors.ink
    val poolRadius = terrainSize * 0.2f

    // Water pool as wobbly circle with hachure
    val poolPoints = (0..11).map { i ->
        val angle = (i / 12f) * 2f * PI.toFloat()
        Offset(
            center.x + cos(angle) * poolRadius,
            center.y + sin(angle) * poolRadius * 0.8f
        )
    }

    drawWobblyPolygon(
        points = poolPoints,
        strokeColor = ink.copy(alpha = 0.4f),
        config = WobbleConfig(roughness = 0.7f, bowing = 0.5f, doubleStroke = true, strokeWidth = 1f, seed = seed),
        hachureConfig = HachureConfig(
            angle = -45f, gap = 4f,
            wobbleConfig = WobbleConfig(roughness = 0.4f, doubleStroke = false, strokeWidth = 0.6f, seed = seed + 100)
        )
    )
}

// Draw lake terrain - hachure fill + wobbly boundary + coast hatching
fun DrawScope.drawLakeTerrain(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    neighborElevations: NeighborElevations? = null
) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink
    val baseRadius = terrainSize * 0.38f

    // Lake boundary as wobbly polygon
    val lakePoints = (0..15).map { i ->
        val angle = (i / 16f) * 2f * PI.toFloat()
        val wobble = 0.85f + random.nextFloat() * 0.3f
        Offset(
            center.x + cos(angle) * baseRadius * wobble,
            center.y + sin(angle) * baseRadius * wobble * 0.8f
        )
    }

    // Hachure fill for water
    drawHachureFill(
        boundary = lakePoints,
        color = ink.copy(alpha = 0.12f),
        config = HachureConfig(
            angle = -45f, gap = 5f,
            wobbleConfig = WobbleConfig(roughness = 0.4f, doubleStroke = false, strokeWidth = 0.7f, seed = seed + 100)
        )
    )

    // Wobbly boundary outline
    drawWobblyPath(
        points = lakePoints, closed = true,
        color = ink.copy(alpha = 0.4f),
        config = WobbleConfig(roughness = 0.7f, bowing = 0.5f, doubleStroke = true, strokeWidth = 1.2f, seed = seed)
    )

    // Coast hatching around outer edge
    drawCoastHatching(
        shoreline = lakePoints,
        layers = 3,
        maxDistance = terrainSize * 0.1f,
        color = ink.copy(alpha = 0.15f),
        inward = false,
        seed = seed + 200
    )
}

// Draw river terrain - ink double-line with wobble
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
    val ink = TerrainColors.ink

    val (flowDirX, flowDirY) = calculateFlowDirection(currentElevation, neighborElevations)

    val startX = center.x - flowDirX * terrainSize * 0.45f
    val startY = center.y - flowDirY * terrainSize * 0.45f
    val midX = center.x + random.nextFloat() * 15f - 7.5f
    val midY = center.y + random.nextFloat() * 15f - 7.5f
    val endX = center.x + flowDirX * terrainSize * 0.45f
    val endY = center.y + flowDirY * terrainSize * 0.45f

    val riverPoints = listOf(
        Offset(startX, startY),
        Offset(midX, midY),
        Offset(endX, endY)
    )

    // Ink double-line river
    drawWobblyPath(
        points = riverPoints, closed = false,
        color = ink.copy(alpha = 0.45f),
        config = HandDrawnDefaults.RIVERS.copy(seed = seed)
    )
}

// Draw stream terrain - thin wobbly ink double-line
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
    val ink = TerrainColors.ink

    val (flowDirX, flowDirY) = calculateFlowDirection(currentElevation, neighborElevations)

    val startX = center.x - flowDirX * terrainSize * 0.4f
    val startY = center.y - flowDirY * terrainSize * 0.4f

    val streamPoints = mutableListOf(Offset(startX, startY))
    val segments = 4
    for (i in 1..segments) {
        val t = i.toFloat() / segments
        val x = center.x + flowDirX * terrainSize * (t - 0.5f) * 0.8f + random.nextFloat() * 8f - 4f
        val y = center.y + flowDirY * terrainSize * (t - 0.5f) * 0.8f + random.nextFloat() * 8f - 4f
        streamPoints.add(Offset(x, y))
    }

    drawWobblyPath(
        points = streamPoints, closed = false,
        color = ink.copy(alpha = 0.35f),
        config = WobbleConfig(roughness = 0.8f, bowing = 0.6f, doubleStroke = true, strokeWidth = 1f, seed = seed)
    )
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

// Draw foothills terrain - small wobbly ink hill arcs
fun DrawScope.drawFoothillsTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink

    repeat(5) { i ->
        val hillX = center.x + random.nextFloat() * terrainSize * 0.6f - terrainSize * 0.3f
        val hillY = center.y + random.nextFloat() * terrainSize * 0.6f - terrainSize * 0.3f
        val hillWidth = terrainSize * (0.15f + random.nextFloat() * 0.1f)
        val hillHeight = terrainSize * (0.08f + random.nextFloat() * 0.05f)

        // Simple arc as wobbly open path
        val arcPoints = listOf(
            Offset(hillX - hillWidth, hillY),
            Offset(hillX - hillWidth * 0.3f, hillY - hillHeight * 0.7f),
            Offset(hillX, hillY - hillHeight),
            Offset(hillX + hillWidth * 0.3f, hillY - hillHeight * 0.7f),
            Offset(hillX + hillWidth, hillY)
        )
        drawWobblyPath(
            points = arcPoints, closed = false,
            color = ink.copy(alpha = 0.25f),
            config = WobbleConfig(roughness = 0.5f, bowing = 0.3f, doubleStroke = false, strokeWidth = 1f, seed = seed + i * 53)
        )
    }
}

// Draw mountain terrain - watabou-style ink peaks with shadow hatching
fun DrawScope.drawMountainTerrain(center: Offset, terrainSize: Float, seed: Int, params: MountainParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink

    val heightMultiplier = params?.heightMultiplier ?: 1.0f
    val peakHeight = 0.8f * terrainSize * 0.5f * heightMultiplier
    val peakCount = params?.peakCount ?: (2 + random.nextInt(2))

    repeat(peakCount) { i ->
        val peakX = center.x + (i - peakCount / 2f) * terrainSize * 0.2f + random.nextFloat() * 10f - 5f
        val baseY = center.y + terrainSize * 0.15f
        val thisHeight = peakHeight * (0.8f + random.nextFloat() * 0.4f)
        val peakY = baseY - thisHeight
        val baseWidth = terrainSize * (0.2f + random.nextFloat() * 0.1f)

        val baseLeft = Offset(peakX - baseWidth, baseY)
        val apex = Offset(peakX + random.nextFloat() * 3f - 1.5f, peakY)
        val baseRight = Offset(peakX + baseWidth, baseY)

        // Full triangle outline
        val triangle = listOf(baseLeft, apex, baseRight)
        drawWobblyPolygon(
            points = triangle,
            strokeColor = ink.copy(alpha = 0.5f),
            config = HandDrawnDefaults.MOUNTAINS.copy(seed = seed + i * 100)
        )

        // Left-side shadow hachure (watabou style)
        val leftShadow = listOf(baseLeft, apex, Offset(peakX, baseY))
        drawHachureFill(
            boundary = leftShadow,
            color = ink.copy(alpha = 0.15f),
            config = HachureConfig(
                angle = 60f, gap = 3f,
                wobbleConfig = WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 0.7f, seed = seed + i * 100 + 50)
            )
        )

        // Snow cap as small wobbly triangle at top 15%
        if (thisHeight > terrainSize * 0.15f) {
            val snowT = 0.15f
            val snowLeft = Offset(apex.x + (baseLeft.x - apex.x) * snowT, apex.y + (baseLeft.y - apex.y) * snowT)
            val snowRight = Offset(apex.x + (baseRight.x - apex.x) * snowT, apex.y + (baseRight.y - apex.y) * snowT)
            val snowTriangle = listOf(snowLeft, apex, snowRight)
            val snowPath = buildWobblyPath(snowTriangle, closed = true, HandDrawnDefaults.MOUNTAINS.copy(seed = seed + i * 100 + 80))
            drawPath(snowPath, ParchmentColors.lightSpot.copy(alpha = 0.6f))
        }
    }
}

// Draw grass terrain - wobbly ink blade tufts
fun DrawScope.drawGrassTerrain(center: Offset, terrainSize: Float, seed: Int, params: GrassParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink

    val tuftCount = params?.tuftCount ?: 12
    val clumpCount = tuftCount.coerceAtLeast(4)

    repeat(clumpCount) { clumpIdx ->
        val clumpX = center.x + random.nextFloat() * terrainSize * 0.7f - terrainSize * 0.35f
        val clumpY = center.y + random.nextFloat() * terrainSize * 0.7f - terrainSize * 0.35f

        val bladesInClump = 3 + random.nextInt(4)
        repeat(bladesInClump) { bladeIdx ->
            val bladeX = clumpX + random.nextFloat() * 6f - 3f
            val bladeY = clumpY
            val bladeHeight = terrainSize * (0.03f + random.nextFloat() * 0.02f)
            val bladeTilt = random.nextFloat() * 4f - 2f

            val path = wobbleLine(
                Offset(bladeX, bladeY),
                Offset(bladeX + bladeTilt, bladeY - bladeHeight),
                WobbleConfig(roughness = 0.6f, bowing = 0.4f, doubleStroke = false, strokeWidth = 0.8f, seed = seed + clumpIdx * 31 + bladeIdx)
            )
            drawPath(
                path, ink.copy(alpha = 0.2f + random.nextFloat() * 0.15f),
                style = Stroke(width = 0.8f, cap = StrokeCap.Round)
            )
        }
    }
}

// Draw building terrain - wobbly ink house with hatched roof
fun DrawScope.drawBuildingTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val buildingWidth = terrainSize * 0.2f
    val buildingHeight = terrainSize * 0.15f
    val roofHeight = terrainSize * 0.1f
    val ink = TerrainColors.ink

    val left = center.x - buildingWidth / 2
    val right = center.x + buildingWidth / 2
    val bottom = center.y + buildingHeight / 2
    val top = center.y - buildingHeight / 2

    // House shape with wobbly outline
    val houseShape = listOf(
        Offset(left, bottom), Offset(left, top),
        Offset(center.x, top - roofHeight),
        Offset(right, top), Offset(right, bottom)
    )
    drawWobblyPolygon(
        points = houseShape,
        strokeColor = ink.copy(alpha = 0.6f),
        config = HandDrawnDefaults.BUILDINGS.copy(seed = seed),
        hachureConfig = HachureConfig(
            angle = 45f, gap = 3f,
            wobbleConfig = WobbleConfig(roughness = 0.4f, doubleStroke = false, strokeWidth = 0.8f, seed = seed + 100)
        )
    )

    // Door
    val doorWidth = buildingWidth * 0.2f
    val doorHeight = buildingHeight * 0.4f
    drawWobblyRect(
        centerX = center.x, centerY = bottom - doorHeight / 2,
        width = doorWidth, height = doorHeight,
        strokeColor = ink.copy(alpha = 0.7f),
        fillColor = ink.copy(alpha = 0.4f),
        config = HandDrawnDefaults.BUILDINGS.copy(seed = seed + 50)
    )
}

// Draw cave terrain - wobbly arch with hachure darkness
fun DrawScope.drawCaveTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val ink = TerrainColors.ink
    val entranceWidth = terrainSize * 0.15f
    val entranceHeight = terrainSize * 0.12f

    // Arch shape
    val archPoints = (0..8).map { i ->
        val t = i / 8f
        val angle = PI.toFloat() * t
        Offset(
            center.x + cos(angle) * entranceWidth,
            center.y + entranceHeight / 2 - sin(angle) * entranceHeight
        )
    } + listOf(Offset(center.x + entranceWidth, center.y + entranceHeight / 2))

    // Draw arch with hachure for cave darkness
    drawWobblyPolygon(
        points = archPoints,
        strokeColor = ink.copy(alpha = 0.7f),
        config = WobbleConfig(roughness = 0.8f, bowing = 0.5f, doubleStroke = true, strokeWidth = 1.5f, seed = seed),
        hachureConfig = HachureConfig(
            angle = 80f, gap = 3f,
            wobbleConfig = WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 0.8f, seed = seed + 200)
        )
    )
}

// Draw desert terrain - wobbly ink dune curves + wobbly cactus
fun DrawScope.drawDesertTerrain(center: Offset, terrainSize: Float, seed: Int, params: DesertParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink
    val duneCount = (params?.duneCount ?: 3).coerceIn(1, 6)

    repeat(duneCount) { i ->
        val duneX = center.x + (i - duneCount / 2f) * terrainSize * 0.25f + random.nextFloat() * 10f - 5f
        val duneY = center.y + random.nextFloat() * terrainSize * 0.3f - terrainSize * 0.15f
        val duneWidth = terrainSize * (0.2f + random.nextFloat() * 0.1f)
        val duneHeight = terrainSize * (0.05f + random.nextFloat() * 0.03f)

        // Dune curve as wobbly open path
        val dunePoints = listOf(
            Offset(duneX - duneWidth, duneY),
            Offset(duneX - duneWidth * 0.3f, duneY - duneHeight),
            Offset(duneX, duneY - duneHeight * 0.5f),
            Offset(duneX + duneWidth * 0.3f, duneY - duneHeight * 0.8f),
            Offset(duneX + duneWidth, duneY)
        )
        drawWobblyPath(
            points = dunePoints, closed = false,
            color = ink.copy(alpha = 0.25f),
            config = WobbleConfig(roughness = 0.5f, bowing = 0.3f, doubleStroke = false, strokeWidth = 1f, seed = seed + i * 41)
        )
    }

    // Cactus
    if (random.nextFloat() < 0.2f) {
        val cactusX = center.x + random.nextFloat() * terrainSize * 0.4f - terrainSize * 0.2f
        val cactusY = center.y + random.nextFloat() * terrainSize * 0.2f
        val cactusHeight = terrainSize * 0.08f

        // Trunk
        drawWobblyLine(
            Offset(cactusX, cactusY), Offset(cactusX, cactusY - cactusHeight),
            ink.copy(alpha = 0.4f),
            WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 2f, seed = seed + 700)
        )
        // Arm
        drawWobblyLine(
            Offset(cactusX - 4f, cactusY - cactusHeight * 0.6f),
            Offset(cactusX - 4f, cactusY - cactusHeight * 0.8f),
            ink.copy(alpha = 0.4f),
            WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 1.5f, seed = seed + 710)
        )
    }
}

// Draw boulder - wobbly ink outline
fun DrawScope.drawBoulder(x: Float, y: Float, boulderSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink

    val points = (0..5).map { i ->
        val angle = (i.toFloat() / 6f) * 2f * PI.toFloat()
        val wobble = 0.7f + random.nextFloat() * 0.3f
        Offset(
            x + cos(angle) * boulderSize * wobble,
            y + sin(angle) * boulderSize * 0.6f * wobble
        )
    }

    drawWobblyPolygon(
        points = points,
        strokeColor = ink.copy(alpha = 0.4f),
        config = WobbleConfig(roughness = 0.8f, bowing = 0.5f, doubleStroke = false, strokeWidth = 1f, seed = seed)
    )
}

// Draw hills terrain - wobbly ink hill curves
fun DrawScope.drawHillsTerrain(center: Offset, terrainSize: Float, seed: Int, params: HillsParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink

    val heightMultiplier = params?.heightMultiplier ?: 1.0f
    val hillCount = (4 * heightMultiplier).toInt().coerceIn(2, 8)

    data class HillInfo(val x: Float, val y: Float, val width: Float, val height: Float)

    val hills = mutableListOf<HillInfo>()
    repeat(hillCount) {
        val hillX = center.x + random.nextFloat() * terrainSize * 0.7f - terrainSize * 0.35f
        val hillY = center.y + random.nextFloat() * terrainSize * 0.5f - terrainSize * 0.1f
        val hillWidth = terrainSize * (0.15f + random.nextFloat() * 0.15f)
        val hillHeight = terrainSize * (0.08f + random.nextFloat() * 0.06f)
        hills.add(HillInfo(hillX, hillY, hillWidth, hillHeight))
    }

    hills.sortedBy { it.y }.forEachIndexed { idx, hill ->
        // Hill as wobbly open arc
        val hillPoints = listOf(
            Offset(hill.x - hill.width, hill.y),
            Offset(hill.x - hill.width * 0.5f, hill.y - hill.height * 0.7f),
            Offset(hill.x, hill.y - hill.height),
            Offset(hill.x + hill.width * 0.5f, hill.y - hill.height * 0.7f),
            Offset(hill.x + hill.width, hill.y)
        )
        drawWobblyPath(
            points = hillPoints, closed = false,
            color = ink.copy(alpha = 0.3f),
            config = WobbleConfig(roughness = 0.6f, bowing = 0.4f, doubleStroke = false, strokeWidth = 1.2f, seed = seed + idx * 47)
        )
    }
}

// Draw coast terrain - ink coastline hatching + wobbly shore
fun DrawScope.drawCoastTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val ink = TerrainColors.ink

    // Shore line as wobbly horizontal path
    val shoreY = center.y + terrainSize * 0.05f
    val shorePoints = (0..8).map { i ->
        val t = i / 8f
        Offset(
            center.x - terrainSize * 0.4f + t * terrainSize * 0.8f,
            shoreY
        )
    }

    // Draw coastline hatching below the shore (water side)
    drawCoastHatching(
        shoreline = shorePoints,
        layers = 3,
        maxDistance = terrainSize * 0.15f,
        color = ink.copy(alpha = 0.2f),
        inward = true,
        seed = seed
    )

    // Draw the shore line itself
    drawWobblyPath(
        points = shorePoints, closed = false,
        color = ink.copy(alpha = 0.4f),
        config = WobbleConfig(roughness = 0.7f, bowing = 0.5f, doubleStroke = true, strokeWidth = 1.2f, seed = seed + 100)
    )
}

// Draw ship (for port terrain) - ink outline style
fun DrawScope.drawShip(center: Offset, shipSize: Float, seed: Int) {
    val ink = TerrainColors.ink

    // Hull outline
    val hull = listOf(
        Offset(center.x - shipSize * 0.4f, center.y),
        Offset(center.x - shipSize * 0.3f, center.y + shipSize * 0.2f),
        Offset(center.x + shipSize * 0.3f, center.y + shipSize * 0.2f),
        Offset(center.x + shipSize * 0.5f, center.y)
    )
    drawWobblyPath(
        points = hull, closed = true,
        color = ink.copy(alpha = 0.5f),
        config = WobbleConfig(roughness = 0.6f, bowing = 0.4f, doubleStroke = false, strokeWidth = 1.2f, seed = seed + 400)
    )

    // Mast
    drawWobblyLine(
        Offset(center.x, center.y), Offset(center.x, center.y - shipSize * 0.5f),
        ink.copy(alpha = 0.5f),
        WobbleConfig(roughness = 0.3f, doubleStroke = false, strokeWidth = 1.5f, seed = seed + 500)
    )

    // Sail triangle
    val sail = listOf(
        Offset(center.x, center.y - shipSize * 0.5f),
        Offset(center.x + shipSize * 0.3f, center.y - shipSize * 0.3f),
        Offset(center.x, center.y - shipSize * 0.1f)
    )
    drawWobblyPath(
        points = sail, closed = true,
        color = ink.copy(alpha = 0.4f),
        config = WobbleConfig(roughness = 0.5f, bowing = 0.3f, doubleStroke = false, strokeWidth = 1f, seed = seed + 600)
    )
}

// Draw swamp terrain - hachure water pools + wobbly ink reeds
fun DrawScope.drawSwampTerrain(center: Offset, terrainSize: Float, seed: Int, params: SwampParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink

    // Water pools with hachure fill
    repeat(3) { i ->
        val poolX = center.x + random.nextFloat() * terrainSize * 0.4f - terrainSize * 0.2f
        val poolY = center.y + random.nextFloat() * terrainSize * 0.4f - terrainSize * 0.2f
        val poolRadius = terrainSize * (0.06f + random.nextFloat() * 0.04f)

        // Small irregular pool shape
        val poolPoints = (0..7).map { j ->
            val angle = (j / 8f) * 2f * PI.toFloat()
            val wobble = 0.7f + random.nextFloat() * 0.3f
            Offset(
                poolX + cos(angle) * poolRadius * wobble,
                poolY + sin(angle) * poolRadius * wobble * 0.7f
            )
        }
        drawHachureFill(
            boundary = poolPoints,
            color = ink.copy(alpha = 0.12f),
            config = HachureConfig(
                angle = -30f, gap = 3f,
                wobbleConfig = WobbleConfig(roughness = 0.4f, doubleStroke = false, strokeWidth = 0.6f, seed = seed + i * 50)
            )
        )
    }

    // Reeds as wobbly vertical lines
    repeat(12) { i ->
        val reedX = center.x + random.nextFloat() * terrainSize * 0.5f - terrainSize * 0.25f
        val reedY = center.y + random.nextFloat() * terrainSize * 0.5f - terrainSize * 0.25f
        val reedHeight = terrainSize * (0.04f + random.nextFloat() * 0.03f)
        val reedTilt = random.nextFloat() * 3f - 1.5f

        val path = wobbleLine(
            Offset(reedX, reedY),
            Offset(reedX + reedTilt, reedY - reedHeight),
            WobbleConfig(roughness = 0.5f, bowing = 0.3f, doubleStroke = false, strokeWidth = 1f, seed = seed + 300 + i)
        )
        drawPath(
            path, ink.copy(alpha = 0.3f),
            style = Stroke(width = 1f, cap = StrokeCap.Round)
        )
    }
}

// Draw castle terrain - wobbly keep + towers with hachure
fun DrawScope.drawCastleTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val ink = TerrainColors.ink

    val baseWidth = terrainSize * 0.3f
    val baseHeight = terrainSize * 0.2f
    val towerWidth = terrainSize * 0.08f
    val towerHeight = terrainSize * 0.15f

    // Central keep
    drawWobblyRect(
        centerX = center.x, centerY = center.y,
        width = baseWidth, height = baseHeight,
        strokeColor = ink.copy(alpha = 0.6f),
        config = HandDrawnDefaults.BUILDINGS.copy(seed = seed),
        hachureConfig = HachureConfig(
            angle = 30f, gap = 4f,
            wobbleConfig = WobbleConfig(roughness = 0.4f, doubleStroke = false, strokeWidth = 0.7f, seed = seed + 100)
        )
    )

    // Towers
    listOf(-1f, 1f).forEach { side ->
        val towerX = center.x + side * (baseWidth / 2 - towerWidth / 2)
        val towerY = center.y - baseHeight / 2 - towerHeight / 2
        drawWobblyRect(
            centerX = towerX, centerY = towerY,
            width = towerWidth, height = towerHeight,
            strokeColor = ink.copy(alpha = 0.7f),
            config = HandDrawnDefaults.BUILDINGS.copy(seed = seed + (side * 200).toInt())
        )

        // Crenellations as small wobbly marks on top
        for (i in 0..2) {
            val crenelX = towerX - towerWidth / 2 + i * towerWidth / 3 + towerWidth / 6
            val crenelY = towerY - towerHeight / 2 - 3f
            drawWobblyLine(
                Offset(crenelX - 2f, crenelY), Offset(crenelX + 2f, crenelY),
                ink.copy(alpha = 0.5f),
                WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 2f, seed = seed + i + (side * 300).toInt())
            )
        }
    }
}

// Draw church terrain - wobbly building + steeple + ink cross
fun DrawScope.drawChurchTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val ink = TerrainColors.ink

    val width = terrainSize * 0.15f
    val height = terrainSize * 0.12f
    val steepleHeight = terrainSize * 0.15f

    // Building body
    drawWobblyRect(
        centerX = center.x, centerY = center.y,
        width = width, height = height,
        strokeColor = ink.copy(alpha = 0.6f),
        config = HandDrawnDefaults.BUILDINGS.copy(seed = seed)
    )

    // Steeple roof
    val roofShape = listOf(
        Offset(center.x - width / 2 - 2f, center.y - height / 2),
        Offset(center.x, center.y - height / 2 - steepleHeight),
        Offset(center.x + width / 2 + 2f, center.y - height / 2)
    )
    drawWobblyPath(
        points = roofShape, closed = true,
        color = ink.copy(alpha = 0.6f),
        config = HandDrawnDefaults.BUILDINGS.copy(seed = seed + 100)
    )

    // Cross - two wobbly lines
    val crossSize = terrainSize * 0.03f
    val crossY = center.y - height / 2 - steepleHeight - crossSize
    drawWobblyLine(
        Offset(center.x, crossY - crossSize), Offset(center.x, crossY + crossSize),
        ink.copy(alpha = 0.7f),
        WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 1.5f, seed = seed + 200)
    )
    drawWobblyLine(
        Offset(center.x - crossSize * 0.6f, crossY), Offset(center.x + crossSize * 0.6f, crossY),
        ink.copy(alpha = 0.7f),
        WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 1.5f, seed = seed + 300)
    )
}

// Draw port terrain - hachure water + wobbly dock + ink ship
fun DrawScope.drawPortTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val ink = TerrainColors.ink

    // Water area with hachure
    val waterRect = rectPoints(center.x, center.y + terrainSize * 0.125f, terrainSize * 0.7f, terrainSize * 0.25f)
    drawHachureFill(
        boundary = waterRect,
        color = ink.copy(alpha = 0.12f),
        config = HachureConfig(
            angle = -30f, gap = 5f,
            wobbleConfig = WobbleConfig(roughness = 0.5f, doubleStroke = false, strokeWidth = 0.7f, seed = seed)
        )
    )

    // Dock - wobbly horizontal line
    val dockY = center.y + terrainSize * 0.05f
    drawWobblyLine(
        Offset(center.x - terrainSize * 0.2f, dockY), Offset(center.x + terrainSize * 0.2f, dockY),
        ink.copy(alpha = 0.6f),
        WobbleConfig(roughness = 0.6f, bowing = 0.4f, doubleStroke = true, strokeWidth = 2f, seed = seed + 100)
    )

    // Dock posts
    for (i in 0..3) {
        val postX = center.x - terrainSize * 0.15f + i * terrainSize * 0.1f
        drawWobblyLine(
            Offset(postX, dockY), Offset(postX, dockY + terrainSize * 0.08f),
            ink.copy(alpha = 0.5f),
            WobbleConfig(roughness = 0.3f, doubleStroke = false, strokeWidth = 1.5f, seed = seed + 200 + i)
        )
    }

    // Ship - wobbly hull + mast + sail
    drawShip(Offset(center.x + terrainSize * 0.15f, center.y + terrainSize * 0.12f), terrainSize * 0.15f, seed)
}

// Draw ruins terrain - wobbly scattered blocks + broken pillars
fun DrawScope.drawRuinsTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val ink = TerrainColors.ink

    // Scattered stone blocks as wobbly rectangles
    repeat(5) { i ->
        val blockX = center.x + random.nextFloat() * terrainSize * 0.5f - terrainSize * 0.25f
        val blockY = center.y + random.nextFloat() * terrainSize * 0.4f - terrainSize * 0.2f
        val blockWidth = terrainSize * (0.05f + random.nextFloat() * 0.04f)
        val blockHeight = terrainSize * (0.03f + random.nextFloat() * 0.03f)

        drawWobblyRect(
            centerX = blockX + blockWidth / 2, centerY = blockY + blockHeight / 2,
            width = blockWidth, height = blockHeight,
            strokeColor = ink.copy(alpha = 0.4f + random.nextFloat() * 0.2f),
            config = WobbleConfig(roughness = 0.9f, bowing = 0.6f, doubleStroke = false, strokeWidth = 1f, seed = seed + i * 37)
        )
    }

    // Broken pillars as short wobbly vertical lines
    repeat(2) { i ->
        val pillarX = center.x + (i - 0.5f) * terrainSize * 0.3f + random.nextFloat() * 5f - 2.5f
        val pillarY = center.y + random.nextFloat() * terrainSize * 0.2f - terrainSize * 0.1f
        val pillarHeight = terrainSize * (0.08f + random.nextFloat() * 0.04f)

        drawWobblyLine(
            Offset(pillarX, pillarY), Offset(pillarX, pillarY - pillarHeight),
            ink.copy(alpha = 0.5f),
            WobbleConfig(roughness = 0.7f, bowing = 0.4f, doubleStroke = true, strokeWidth = 2f, seed = seed + 500 + i)
        )
        // Broken top - small horizontal wobbly mark
        drawWobblyLine(
            Offset(pillarX - 3f, pillarY - pillarHeight), Offset(pillarX + 3f, pillarY - pillarHeight),
            ink.copy(alpha = 0.4f),
            WobbleConfig(roughness = 1.0f, bowing = 0.3f, doubleStroke = false, strokeWidth = 1.5f, seed = seed + 600 + i)
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
    ExitDirection.UP -> Pair(0f, 0f)  // Vertical - no x,y direction
    ExitDirection.DOWN -> Pair(0f, 0f)  // Vertical - no x,y direction
    ExitDirection.ENTER -> Pair(0f, 0f)
    ExitDirection.UNKNOWN -> Pair(0f, 0f)
}
