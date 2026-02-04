@file:OptIn(ExperimentalMaterial3Api::class)

package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class CombatFeedbackEvent(
    val id: String,
    val type: CombatEventType,
    val value: Int,
    val target: String,
    val source: String? = null,
    val isCritical: Boolean = false,
    val timestamp: Long = com.ez2bg.anotherthread.platform.currentTimeMillis()
)

enum class CombatEventType {
    DAMAGE, HEALING, MISS, BLOCK, CRITICAL, SPELL_CAST, ABILITY_USE, DEATH, RESURRECTION
}

@Composable
fun CombatFeedbackOverlay(
    events: List<CombatFeedbackEvent>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        events.takeLast(3).forEachIndexed { index, event ->
            key(event.id) {
                CombatFeedbackItem(
                    event = event,
                    index = index
                )
            }
        }
    }
}

@Composable
private fun CombatFeedbackItem(
    event: CombatFeedbackEvent,
    index: Int,
    modifier: Modifier = Modifier
) {
    val slideAnimation = rememberInfiniteTransition(label = "slide")
    val offsetY by slideAnimation.animateFloat(
        initialValue = 0f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    val fadeAnimation = rememberInfiniteTransition(label = "fade")
    val alpha by fadeAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    val scaleAnimation = rememberInfiniteTransition(label = "scale")
    val scale by scaleAnimation.animateFloat(
        initialValue = if (event.isCritical) 1.5f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseOutBounce),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    val eventColor = when (event.type) {
        CombatEventType.DAMAGE -> if (event.isCritical) Color(0xFFfbbf24) else Color(0xFFef4444)
        CombatEventType.HEALING -> Color(0xFF10b981)
        CombatEventType.MISS -> Color(0xFF6b7280)
        CombatEventType.BLOCK -> Color(0xFF3b82f6)
        CombatEventType.CRITICAL -> Color(0xFFfbbf24)
        CombatEventType.SPELL_CAST -> Color(0xFF8b5cf6)
        CombatEventType.ABILITY_USE -> Color(0xFF06b6d4)
        CombatEventType.DEATH -> Color(0xFF7f1d1d)
        CombatEventType.RESURRECTION -> Color(0xFFfbbf24)
    }

    val eventIcon = when (event.type) {
        CombatEventType.DAMAGE -> Icons.Default.Whatshot
        CombatEventType.HEALING -> Icons.Default.Healing
        CombatEventType.MISS -> Icons.Default.Close
        CombatEventType.BLOCK -> Icons.Default.Shield
        CombatEventType.CRITICAL -> Icons.Default.Star
        CombatEventType.SPELL_CAST -> Icons.Default.AutoAwesome
        CombatEventType.ABILITY_USE -> Icons.Default.FlashOn
        CombatEventType.DEATH -> Icons.Default.Dangerous
        CombatEventType.RESURRECTION -> Icons.Default.Refresh
    }

    val displayText = when (event.type) {
        CombatEventType.DAMAGE -> if (event.isCritical) "CRIT ${event.value}!" else "${event.value}"
        CombatEventType.HEALING -> "+${event.value}"
        CombatEventType.MISS -> "MISS"
        CombatEventType.BLOCK -> "BLOCKED"
        CombatEventType.CRITICAL -> "CRITICAL!"
        CombatEventType.SPELL_CAST -> "SPELL"
        CombatEventType.ABILITY_USE -> "ABILITY"
        CombatEventType.DEATH -> "DEFEATED"
        CombatEventType.RESURRECTION -> "REVIVED"
    }

    Card(
        modifier = modifier
            .offset(y = (offsetY + (index * 20)).dp)
            .wrapContentSize()
            .shadow(
                elevation = if (event.isCritical) 12.dp else 6.dp,
                shape = RoundedCornerShape(if (event.isCritical) 16.dp else 12.dp)
            ),
        shape = RoundedCornerShape(if (event.isCritical) 16.dp else 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    brush = if (event.isCritical) {
                        Brush.radialGradient(
                            colors = listOf(
                                eventColor.copy(alpha = alpha * 0.9f),
                                eventColor.copy(alpha = alpha * 0.6f),
                                Color.Transparent
                            ),
                            radius = 150f
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                eventColor.copy(alpha = alpha * 0.8f),
                                eventColor.copy(alpha = alpha * 0.4f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(if (event.isCritical) 16.dp else 12.dp)
                )
                .border(
                    width = if (event.isCritical) 3.dp else 2.dp,
                    color = eventColor.copy(alpha = alpha),
                    shape = RoundedCornerShape(if (event.isCritical) 16.dp else 12.dp)
                )
                .padding(
                    horizontal = if (event.isCritical) 20.dp else 16.dp,
                    vertical = if (event.isCritical) 12.dp else 8.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = eventIcon,
                    contentDescription = event.type.name,
                    tint = Color.White.copy(alpha = alpha),
                    modifier = Modifier.size(
                        if (event.isCritical) (24 * scale).dp else (18 * scale).dp
                    )
                )
                
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (event.isCritical) FontWeight.Black else FontWeight.Bold,
                        color = Color.White.copy(alpha = alpha),
                        fontSize = if (event.isCritical) (20 * scale).sp else (16 * scale).sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FloatingCombatText(
    events: List<CombatFeedbackEvent>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        events.takeLast(5).forEach { event ->
            key(event.id) {
                FloatingTextItem(
                    event = event
                )
            }
        }
    }
}

@Composable
private fun FloatingTextItem(
    event: CombatFeedbackEvent,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(event.id) {
        delay(3000)
        isVisible = false
    }
    
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        val eventColor = when (event.type) {
            CombatEventType.DAMAGE -> if (event.isCritical) Color(0xFFfbbf24) else Color(0xFFef4444)
            CombatEventType.HEALING -> Color(0xFF10b981)
            CombatEventType.MISS -> Color(0xFF6b7280)
            CombatEventType.BLOCK -> Color(0xFF3b82f6)
            else -> Color(0xFF8b5cf6)
        }
        
        Text(
            text = when (event.type) {
                CombatEventType.DAMAGE -> "${if (event.isCritical) "CRIT " else ""}${event.value} damage to ${event.target}"
                CombatEventType.HEALING -> "+${event.value} health to ${event.target}"
                CombatEventType.MISS -> "${event.source ?: "Attack"} missed ${event.target}"
                CombatEventType.BLOCK -> "${event.target} blocked the attack"
                else -> "${event.type.name.lowercase().replace('_', ' ')} on ${event.target}"
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = eventColor,
                fontWeight = if (event.isCritical) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}