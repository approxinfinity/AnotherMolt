package com.ez2bg.anotherthread.ui.terrain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * RoughJS-inspired hand-drawn rendering primitives for Compose Canvas.
 * Provides wobble, double-stroke, and hachure fill effects.
 */

data class WobbleConfig(
    val roughness: Float = 1.0f,
    val bowing: Float = 1.0f,
    val doubleStroke: Boolean = true,
    val strokeWidth: Float = 1.5f,
    val seed: Int = 0
)

data class HachureConfig(
    val angle: Float = 45f,
    val gap: Float = 4f,
    val density: Float = 1.0f,
    val wobbleConfig: WobbleConfig = WobbleConfig(roughness = 0.6f, doubleStroke = false, strokeWidth = 1f)
)

object HandDrawnDefaults {
    val STANDARD = WobbleConfig()
    val MOUNTAINS = WobbleConfig(roughness = 0.8f, bowing = 0.6f)
    val RIVERS = WobbleConfig(roughness = 1.2f, bowing = 1.5f, strokeWidth = 2f)
    val BUILDINGS = WobbleConfig(roughness = 0.7f, bowing = 0.5f)
    val TREES = WobbleConfig(roughness = 1.0f, bowing = 0.8f, strokeWidth = 1.5f)
}

// ============================================================
// Core Wobble Primitives
// ============================================================

/**
 * Create a wobbly path between two points (RoughJS-style).
 * Randomizes endpoints, adds 2 control points at 50%/75% with perpendicular bowing.
 */
fun wobbleLine(from: Offset, to: Offset, config: WobbleConfig): Path {
    val random = Random(config.seed)
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 0.1f) return Path().apply { moveTo(from.x, from.y); lineTo(to.x, to.y) }

    val maxOffset = config.roughness * min(length * 0.05f, 3f)
    val from2 = Offset(
        from.x + (random.nextFloat() - 0.5f) * maxOffset,
        from.y + (random.nextFloat() - 0.5f) * maxOffset
    )
    val to2 = Offset(
        to.x + (random.nextFloat() - 0.5f) * maxOffset,
        to.y + (random.nextFloat() - 0.5f) * maxOffset
    )

    // Perpendicular direction for bowing
    val perpX = -dy / length
    val perpY = dx / length
    val bowAmount = config.bowing * (random.nextFloat() - 0.5f) * 2f * min(length * 0.1f, 8f)

    val mid = Offset(
        from2.x + dx * 0.5f + perpX * bowAmount,
        from2.y + dy * 0.5f + perpY * bowAmount
    )
    val cp2 = Offset(
        from2.x + dx * 0.75f + perpX * bowAmount * 0.3f,
        from2.y + dy * 0.75f + perpY * bowAmount * 0.3f
    )

    return Path().apply {
        moveTo(from2.x, from2.y)
        cubicTo(mid.x, mid.y, cp2.x, cp2.y, to2.x, to2.y)
    }
}

/**
 * Draw a wobbly line with double-stroke effect.
 */
fun DrawScope.drawWobblyLine(
    from: Offset,
    to: Offset,
    color: Color = TerrainColors.ink,
    config: WobbleConfig = HandDrawnDefaults.STANDARD
) {
    val path1 = wobbleLine(from, to, config)
    drawPath(
        path1, color,
        style = Stroke(width = config.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    if (config.doubleStroke) {
        val path2 = wobbleLine(from, to, config.copy(seed = config.seed + 7919))
        drawPath(
            path2, color.copy(alpha = color.alpha * 0.5f),
            style = Stroke(width = config.strokeWidth * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ============================================================
// Wobbly Path (closed or open polygon)
// ============================================================

/**
 * Create a wobbly Path from a list of points. If closed, connects last→first.
 */
fun buildWobblyPath(points: List<Offset>, closed: Boolean, config: WobbleConfig): Path {
    if (points.size < 2) return Path()
    val path = Path()
    val first = points[0]
    path.moveTo(first.x, first.y)

    for (i in 0 until points.size - 1) {
        val segment = wobbleLine(points[i], points[i + 1], config.copy(seed = config.seed + i * 31))
        // Extract the cubic control points from the segment by rebuilding
        val random = Random(config.seed + i * 31)
        val from = points[i]
        val to = points[i + 1]
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = sqrt(dx * dx + dy * dy)
        if (length < 0.1f) {
            path.lineTo(to.x, to.y)
            continue
        }

        val maxOffset = config.roughness * min(length * 0.05f, 3f)
        val perpX = -dy / length
        val perpY = dx / length
        // Skip endpoint randomization for from (already at path position)
        random.nextFloat(); random.nextFloat() // consume from randomization
        val to2 = Offset(
            to.x + (random.nextFloat() - 0.5f) * maxOffset,
            to.y + (random.nextFloat() - 0.5f) * maxOffset
        )
        val bowAmount = config.bowing * (random.nextFloat() - 0.5f) * 2f * min(length * 0.1f, 8f)
        val mid = Offset(
            from.x + dx * 0.5f + perpX * bowAmount,
            from.y + dy * 0.5f + perpY * bowAmount
        )
        val cp2 = Offset(
            from.x + dx * 0.75f + perpX * bowAmount * 0.3f,
            from.y + dy * 0.75f + perpY * bowAmount * 0.3f
        )
        path.cubicTo(mid.x, mid.y, cp2.x, cp2.y, to2.x, to2.y)
    }

    if (closed && points.size >= 3) {
        val from = points.last()
        val to = points[0]
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = sqrt(dx * dx + dy * dy)
        if (length > 0.1f) {
            val random = Random(config.seed + points.size * 31)
            val maxOffset = config.roughness * min(length * 0.05f, 3f)
            val perpX = -dy / length
            val perpY = dx / length
            random.nextFloat(); random.nextFloat()
            val to2 = Offset(
                to.x + (random.nextFloat() - 0.5f) * maxOffset,
                to.y + (random.nextFloat() - 0.5f) * maxOffset
            )
            val bowAmount = config.bowing * (random.nextFloat() - 0.5f) * 2f * min(length * 0.1f, 8f)
            val mid = Offset(
                from.x + dx * 0.5f + perpX * bowAmount,
                from.y + dy * 0.5f + perpY * bowAmount
            )
            val cp2 = Offset(
                from.x + dx * 0.75f + perpX * bowAmount * 0.3f,
                from.y + dy * 0.75f + perpY * bowAmount * 0.3f
            )
            path.cubicTo(mid.x, mid.y, cp2.x, cp2.y, to2.x, to2.y)
        }
        path.close()
    }
    return path
}

/**
 * Draw a wobbly polygon outline with optional double-stroke.
 */
fun DrawScope.drawWobblyPath(
    points: List<Offset>,
    closed: Boolean = true,
    color: Color = TerrainColors.ink,
    config: WobbleConfig = HandDrawnDefaults.STANDARD
) {
    if (points.size < 2) return
    val path = buildWobblyPath(points, closed, config)
    drawPath(
        path, color,
        style = Stroke(width = config.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    if (config.doubleStroke) {
        val path2 = buildWobblyPath(points, closed, config.copy(seed = config.seed + 7919))
        drawPath(
            path2, color.copy(alpha = color.alpha * 0.5f),
            style = Stroke(width = config.strokeWidth * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Draw a wobbly polygon with optional fill (solid or hachure) + outline.
 */
fun DrawScope.drawWobblyPolygon(
    points: List<Offset>,
    fillColor: Color? = null,
    strokeColor: Color = TerrainColors.ink,
    config: WobbleConfig = HandDrawnDefaults.STANDARD,
    hachureConfig: HachureConfig? = null
) {
    if (points.size < 3) return

    // Fill
    if (hachureConfig != null) {
        drawHachureFill(points, strokeColor.copy(alpha = strokeColor.alpha * 0.3f), hachureConfig)
    } else if (fillColor != null) {
        val fillPath = buildWobblyPath(points, closed = true, config)
        drawPath(fillPath, fillColor)
    }

    // Outline
    drawWobblyPath(points, closed = true, strokeColor, config)
}

// ============================================================
// Hachure Fill (scan-line with wobbly lines)
// ============================================================

/**
 * Fill a polygon with hachure (angled parallel lines).
 * Uses scan-line technique: sweep parallel lines at given angle and draw between polygon edge intersections.
 */
fun DrawScope.drawHachureFill(
    boundary: List<Offset>,
    color: Color = TerrainColors.ink.copy(alpha = 0.2f),
    config: HachureConfig = HachureConfig()
) {
    if (boundary.size < 3) return

    val angleRad = config.angle * PI.toFloat() / 180f
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)

    // Rotate all points by -angle so scan lines become horizontal
    val rotated = boundary.map { p ->
        Offset(p.x * cosA + p.y * sinA, -p.x * sinA + p.y * cosA)
    }

    val minY = rotated.minOf { it.y }
    val maxY = rotated.maxOf { it.y }
    val adjustedGap = config.gap / config.density

    var scanY = minY + adjustedGap * 0.5f
    var lineIndex = 0
    while (scanY < maxY) {
        // Find intersections of horizontal line y=scanY with rotated polygon edges
        val intersections = mutableListOf<Float>()
        for (i in rotated.indices) {
            val j = (i + 1) % rotated.size
            val p1 = rotated[i]
            val p2 = rotated[j]
            if ((p1.y <= scanY && p2.y > scanY) || (p2.y <= scanY && p1.y > scanY)) {
                val t = (scanY - p1.y) / (p2.y - p1.y)
                intersections.add(p1.x + t * (p2.x - p1.x))
            }
        }

        intersections.sort()

        // Draw line segments between pairs, unrotated back to original space
        for (k in intersections.indices step 2) {
            if (k + 1 >= intersections.size) break
            val rx1 = intersections[k]
            val rx2 = intersections[k + 1]
            // Unrotate: x' = x*cos + y*(-sin), y' = x*sin + y*cos
            val from = Offset(
                rx1 * cosA - scanY * sinA,
                rx1 * sinA + scanY * cosA
            )
            val to = Offset(
                rx2 * cosA - scanY * sinA,
                rx2 * sinA + scanY * cosA
            )
            val lineConfig = config.wobbleConfig.copy(seed = config.wobbleConfig.seed + lineIndex)
            val path = wobbleLine(from, to, lineConfig)
            drawPath(
                path, color,
                style = Stroke(width = lineConfig.strokeWidth, cap = StrokeCap.Round)
            )
            lineIndex++
        }

        scanY += adjustedGap
    }
}

// ============================================================
// Coastline Hatching
// ============================================================

/**
 * Draw Watabou-style coastline hatching: short perpendicular marks along shore,
 * getting sparser and fainter at greater distance.
 */
fun DrawScope.drawCoastHatching(
    shoreline: List<Offset>,
    layers: Int = 4,
    maxDistance: Float = 25f,
    color: Color = TerrainColors.ink.copy(alpha = 0.25f),
    inward: Boolean = true,
    seed: Int = 0
) {
    if (shoreline.size < 2) return
    val random = Random(seed)

    for (layer in 0 until layers) {
        val distance = (layer + 1) * (maxDistance / layers)
        val alphaFactor = 1f - (layer.toFloat() / layers) * 0.6f
        // Fewer marks in outer layers
        val skipChance = layer * 0.15f

        for (i in shoreline.indices) {
            if (random.nextFloat() < skipChance) continue

            val p = shoreline[i]
            // Calculate normal (perpendicular to tangent)
            val prev = shoreline[max(0, i - 1)]
            val next = shoreline[min(shoreline.size - 1, i + 1)]
            val tangentX = next.x - prev.x
            val tangentY = next.y - prev.y
            val tangentLen = sqrt(tangentX * tangentX + tangentY * tangentY)
            if (tangentLen < 0.1f) continue

            val normalX = if (inward) tangentY / tangentLen else -tangentY / tangentLen
            val normalY = if (inward) -tangentX / tangentLen else tangentX / tangentLen

            val markLength = (6f + random.nextFloat() * 4f) * alphaFactor
            val startOffset = distance - markLength * 0.5f
            val from = Offset(p.x + normalX * startOffset, p.y + normalY * startOffset)
            val to = Offset(p.x + normalX * (startOffset + markLength), p.y + normalY * (startOffset + markLength))

            val markConfig = WobbleConfig(
                roughness = 0.5f, bowing = 0.3f, doubleStroke = false,
                strokeWidth = 1f, seed = seed + i + layer * 1000
            )
            val path = wobbleLine(from, to, markConfig)
            drawPath(
                path, color.copy(alpha = color.alpha * alphaFactor),
                style = Stroke(width = 1f, cap = StrokeCap.Round)
            )
        }
    }
}

// ============================================================
// Utility: Wobbly Rectangle
// ============================================================

/**
 * Helper to create rectangle corner points.
 */
fun rectPoints(centerX: Float, centerY: Float, width: Float, height: Float): List<Offset> = listOf(
    Offset(centerX - width / 2, centerY - height / 2),
    Offset(centerX + width / 2, centerY - height / 2),
    Offset(centerX + width / 2, centerY + height / 2),
    Offset(centerX - width / 2, centerY + height / 2)
)

/**
 * Draw a wobbly rectangle with optional hachure fill.
 */
fun DrawScope.drawWobblyRect(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    strokeColor: Color = TerrainColors.ink,
    fillColor: Color? = null,
    config: WobbleConfig = HandDrawnDefaults.BUILDINGS,
    hachureConfig: HachureConfig? = null
) {
    drawWobblyPolygon(
        points = rectPoints(centerX, centerY, width, height),
        fillColor = fillColor,
        strokeColor = strokeColor,
        config = config,
        hachureConfig = hachureConfig
    )
}
