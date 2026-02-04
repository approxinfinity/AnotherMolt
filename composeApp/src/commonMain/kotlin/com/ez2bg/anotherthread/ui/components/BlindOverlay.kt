package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Overlay that obscures the view when the player is blinded.
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
