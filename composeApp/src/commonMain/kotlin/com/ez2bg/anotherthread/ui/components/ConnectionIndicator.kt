package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.state.ConnectionStateHolder

/**
 * Small blinking indicator shown when there are connection issues.
 * Appears in a corner, non-intrusive but visible.
 */
@Composable
fun ConnectionIndicator(
    modifier: Modifier = Modifier
) {
    val isConnected by ConnectionStateHolder.isConnected.collectAsState()

    if (!isConnected) {
        // Blinking animation
        val infiniteTransition = rememberInfiniteTransition()
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = modifier
                .size(32.dp)
                .alpha(alpha)
                .background(Color.Red.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SignalWifiOff,
                contentDescription = "Connection lost",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
