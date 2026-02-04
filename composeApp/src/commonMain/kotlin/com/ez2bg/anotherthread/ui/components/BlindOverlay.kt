package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
    if (isBlind) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.95f),
                            Color.Black
                        ),
                        radius = 500f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated blind icon
                    Text(
                        text = "🙈",
                        fontSize = 64.sp,
                        modifier = Modifier.alpha(0.9f)
                    )
                    
                    Text(
                        text = "BLINDED",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Red.copy(alpha = 0.9f),
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                    
                    Text(
                        text = "Your vision is shrouded in darkness.\nYou cannot see your surroundings.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    if (duration > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏱️",
                                    fontSize = 20.sp
                                )
                                Column {
                                    Text(
                                        text = "Duration Remaining",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "${duration} seconds",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
 *
 * Features:
 * - Dark overlay that covers the room view
 * - VisibilityOff icon indicator
 * - Countdown showing remaining rounds
 * - Fading effect based on remaining duration
 */
@Composable
fun BlindOverlay(
    roundsRemaining: Int,
    modifier: Modifier = Modifier
) {
    // Alpha fades slightly as effect wears off
    val alpha by animateFloatAsState(
        targetValue = when (roundsRemaining) {
            0 -> 0f
            1 -> 0.85f
            2 -> 0.92f
            else -> 0.95f
        },
        animationSpec = tween(500)
    )

    if (roundsRemaining <= 0) return

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = "Blinded",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$roundsRemaining",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Full-screen blind overlay for covering the entire exploration view.
 */
@Composable
fun BlindOverlayFullScreen(
    roundsRemaining: Int,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = when (roundsRemaining) {
            0 -> 0f
            1 -> 0.85f
            2 -> 0.92f
            else -> 0.95f
        },
        animationSpec = tween(500)
    )

    if (roundsRemaining <= 0) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = "Blinded",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "BLINDED",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$roundsRemaining round${if (roundsRemaining > 1) "s" else ""} remaining",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Indicator badge showing that the player is disoriented.
 */
@Composable
fun DisorientIndicator(
    roundsRemaining: Int,
    modifier: Modifier = Modifier
) {
    if (roundsRemaining <= 0) return

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ScreenRotation,
                contentDescription = "Disoriented",
                tint = Color(0xFFFF9800),  // Orange
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Disoriented ($roundsRemaining)",
                color = Color(0xFFFF9800),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Status effect badge that can display any status effect.
 */
@Composable
fun StatusEffectBadge(
    effectType: String,
    value: Int,
    roundsRemaining: Int,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (effectType) {
        "blind" -> Triple(Icons.Filled.VisibilityOff, Color(0xFF616161), "Blind")
        "disorient" -> Triple(Icons.Filled.ScreenRotation, Color(0xFFFF9800), "Disorient")
        "shield" -> Triple(Icons.Filled.Shield, Color(0xFF2196F3), "Shield: $value")
        "reflect" -> Triple(Icons.Filled.Replay, Color(0xFF9C27B0), "Reflect: $value%")
        "lifesteal" -> Triple(Icons.Filled.Bloodtype, Color(0xFFD32F2F), "Lifesteal: $value%")
        "stun" -> Triple(Icons.Filled.Lock, Color(0xFFFF5722), "Stunned")
        "root" -> Triple(Icons.Filled.Anchor, Color(0xFF795548), "Rooted")
        else -> Triple(Icons.Filled.Star, Color(0xFF4CAF50), effectType.replaceFirstChar { it.uppercase() })
    }

    if (roundsRemaining <= 0) return

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "$label ($roundsRemaining)",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
