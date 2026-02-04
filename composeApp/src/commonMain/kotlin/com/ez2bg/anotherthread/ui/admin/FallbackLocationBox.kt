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
@Composable
fun FallbackLocationBox(
    location: LocationDto,
    modifier: Modifier = Modifier
) {
    // Get terrain-based color and description for enhanced fallback
    val terrainColor = remember(location) { getTerrainColor(location.desc, location.name) }
    val terrainDescription = remember(location) { getPrimaryTerrainDescription(location.desc, location.name) }
    
    Surface(
        modifier = modifier,
        color = terrainColor.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            terrainColor.copy(alpha = 0.2f),
                            terrainColor.copy(alpha = 0.6f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Terrain-based emoji icon
                Text(
                    text = terrainDescription.take(2), // Extract emoji
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Location name (abbreviated if long)
                Text(
                    text = if (location.name.length > 12) {
                        location.name.take(10) + "..."
                    } else {
                        location.name
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Terrain type label
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = terrainDescription.drop(2).trim(), // Remove emoji, keep description
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
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
