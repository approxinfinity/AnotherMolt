package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun DragonHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dragon emoji with larger size
                Text(
                    text = "🐉",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Main title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Subtitle if provided
                subtitle?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                // Decorative elements
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(6.dp)
                        ) {}
                    }
                }
            }
        }
    }
}
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Wing on the left side
            val wingPath1 = Path().apply {
                moveTo(w * 0.25f, h * 0.4f)
                cubicTo(
                    w * 0.2f, h * 0.1f,
                    w * 0.3f, h * 0.05f,
                    w * 0.38f, h * 0.15f
                )
                cubicTo(
                    w * 0.42f, h * 0.25f,
                    w * 0.4f, h * 0.5f,
                    w * 0.35f, h * 0.55f
                )
            }

            drawPath(
                path = wingPath1,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth * 0.8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Wing on the right side
            val wingPath2 = Path().apply {
                moveTo(w * 0.75f, h * 0.5f)
                cubicTo(
                    w * 0.72f, h * 0.2f,
                    w * 0.8f, h * 0.1f,
                    w * 0.88f, h * 0.2f
                )
                cubicTo(
                    w * 0.92f, h * 0.28f,
                    w * 0.88f, h * 0.45f,
                    w * 0.82f, h * 0.55f
                )
            }

            drawPath(
                path = wingPath2,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth * 0.8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Spikes along the body
            val spikePositions = listOf(
                Pair(w * 0.3f, h * 0.65f),
                Pair(w * 0.4f, h * 0.75f),
                Pair(w * 0.5f, h * 0.5f),
                Pair(w * 0.6f, h * 0.25f),
                Pair(w * 0.7f, h * 0.35f)
            )

            spikePositions.forEach { (x, y) ->
                val spikePath = Path().apply {
                    moveTo(x - 5.dp.toPx(), y)
                    lineTo(x, y - 10.dp.toPx())
                    lineTo(x + 5.dp.toPx(), y)
                }
                drawPath(
                    path = spikePath,
                    color = lineColor,
                    style = Stroke(
                        width = strokeWidth * 0.6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Tail tip flourish
            val tailPath = Path().apply {
                moveTo(w * 0.05f, h * 0.7f)
                cubicTo(
                    w * 0.02f, h * 0.8f,
                    w * 0.01f, h * 0.9f,
                    w * 0.03f, h * 0.95f
                )
            }

            drawPath(
                path = tailPath,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth * 0.7f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Title text on top
        Text(
            text = "Another Thread",
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
