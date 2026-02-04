@file:OptIn(ExperimentalMaterial3Api::class)

package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class AbilityCooldown(
    val abilityId: String,
    val totalCooldown: Float, // in seconds
    val remainingCooldown: Float, // in seconds
    val isGlobalCooldown: Boolean = false
) {
    val progress: Float
        get() = if (totalCooldown > 0) {
            (totalCooldown - remainingCooldown) / totalCooldown
        } else 1f
    
    val isOnCooldown: Boolean
        get() = remainingCooldown > 0
}

@Composable
fun AbilityCooldownOverlay(
    cooldown: AbilityCooldown?,
    modifier: Modifier = Modifier
) {
    if (cooldown != null && cooldown.isOnCooldown) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // Circular progress indicator
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCooldownProgress(
                    progress = cooldown.progress,
                    isGlobalCooldown = cooldown.isGlobalCooldown
                )
            }
            
            // Cooldown text
            if (cooldown.remainingCooldown > 0.5f) {
                Card(
                    modifier = Modifier
                        .wrapContentSize(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = "${cooldown.remainingCooldown.toInt() + 1}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalCooldownIndicator(
    globalCooldown: AbilityCooldown?,
    modifier: Modifier = Modifier
) {
    if (globalCooldown != null && globalCooldown.isOnCooldown) {
        val pulseAnimation = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by pulseAnimation.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp),
            shape = RoundedCornerShape(3.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFF1e293b).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF475569).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(globalCooldown.progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFef4444).copy(alpha = pulseAlpha),
                                    Color(0xFFdc2626).copy(alpha = pulseAlpha * 0.8f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun CooldownSweepEffect(
    cooldown: AbilityCooldown?,
    modifier: Modifier = Modifier
) {
    if (cooldown != null && cooldown.isOnCooldown) {
        Canvas(
            modifier = modifier.fillMaxSize()
        ) {
            drawCooldownSweep(
                progress = cooldown.progress,
                isGlobalCooldown = cooldown.isGlobalCooldown
            )
        }
    }
}

private fun DrawScope.drawCooldownProgress(
    progress: Float,
    isGlobalCooldown: Boolean
) {
    val strokeWidth = 6.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = center
    
    // Background circle
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )
    
    // Progress arc
    val sweepAngle = 360f * progress
    val progressColor = if (isGlobalCooldown) {
        Color(0xFFef4444)
    } else {
        Color(0xFF3b82f6)
    }
    
    if (sweepAngle > 0) {
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            ),
            size = androidx.compose.ui.geometry.Size(
                width = radius * 2,
                height = radius * 2
            ),
            topLeft = androidx.compose.ui.geometry.Offset(
                x = center.x - radius,
                y = center.y - radius
            )
        )
    }
}

private fun DrawScope.drawCooldownSweep(
    progress: Float,
    isGlobalCooldown: Boolean
) {
    if (progress < 1f) {
        val sweepColor = if (isGlobalCooldown) {
            Color.Black.copy(alpha = 0.6f)
        } else {
            Color.Black.copy(alpha = 0.4f)
        }
        
        // Calculate sweep angle (remaining cooldown)
        val remainingAngle = 360f * (1f - progress)
        
        if (remainingAngle > 0) {
            drawArc(
                color = sweepColor,
                startAngle = -90f + (360f * progress),
                sweepAngle = remainingAngle,
                useCenter = true,
                size = size
            )
        }
    }
}

@Composable
fun CooldownPulseEffect(
    isOnCooldown: Boolean,
    modifier: Modifier = Modifier
) {
    if (isOnCooldown) {
        val pulseAnimation = rememberInfiniteTransition(label = "cooldownPulse")
        val pulseScale by pulseAnimation.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFef4444).copy(alpha = 0.1f * pulseScale),
                            Color.Transparent
                        ),
                        radius = 100f * pulseScale
                    ),
                    shape = CircleShape
                )
        )
    }
}