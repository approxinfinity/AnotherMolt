package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.LocationDto

/**
 * Fallback location box displayed when no image is available.
 * Shows terrain background, mystery icon (question mark), and name at bottom.
 */
@Composable
internal fun FallbackLocationBox(location: LocationDto) {
    // Mimic the look of a real location image: terrain background, mystery icon, name at bottom
    val terrainColor = remember(location) { getTerrainColor(location.desc, location.name) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(terrainColor.copy(alpha = 0.7f))
    ) {
        // Draw a simple hill silhouette with question mark
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw rolling hills silhouette at the bottom third
            val hillColor = Color(0xFF2A3A2A).copy(alpha = 0.6f)
            val hillPath = Path().apply {
                moveTo(0f, height * 0.75f)
                // First hill
                quadraticTo(width * 0.15f, height * 0.55f, width * 0.3f, height * 0.7f)
                // Second hill (taller)
                quadraticTo(width * 0.5f, height * 0.45f, width * 0.7f, height * 0.65f)
                // Third hill
                quadraticTo(width * 0.85f, height * 0.5f, width, height * 0.7f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(hillPath, color = hillColor)

            // Draw question mark silhouette in the center-upper area
            val questionMarkColor = Color.White.copy(alpha = 0.4f)
            val qmCenterX = width * 0.5f
            val qmCenterY = height * 0.35f
            val qmSize = minOf(width, height) * 0.25f

            // Question mark curve (top part)
            val qmPath = Path().apply {
                // Arc part of question mark
                moveTo(qmCenterX - qmSize * 0.3f, qmCenterY - qmSize * 0.3f)
                quadraticTo(qmCenterX - qmSize * 0.3f, qmCenterY - qmSize * 0.6f, qmCenterX, qmCenterY - qmSize * 0.6f)
                quadraticTo(qmCenterX + qmSize * 0.4f, qmCenterY - qmSize * 0.6f, qmCenterX + qmSize * 0.4f, qmCenterY - qmSize * 0.2f)
                quadraticTo(qmCenterX + qmSize * 0.4f, qmCenterY + qmSize * 0.1f, qmCenterX, qmCenterY + qmSize * 0.2f)
            }
            drawPath(qmPath, color = questionMarkColor, style = Stroke(width = qmSize * 0.15f, cap = StrokeCap.Round))

            // Question mark dot
            drawCircle(
                color = questionMarkColor,
                radius = qmSize * 0.1f,
                center = Offset(qmCenterX, qmCenterY + qmSize * 0.45f)
            )
        }

        // Name label at bottom with black background (like real location images)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(4.dp)
        ) {
            Text(
                text = location.name.ifBlank { location.id.take(8) },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
