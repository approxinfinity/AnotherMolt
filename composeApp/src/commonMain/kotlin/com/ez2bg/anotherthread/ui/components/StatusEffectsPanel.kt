@file:OptIn(ExperimentalMaterial3Api::class)

package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StatusEffect(
    val id: String,
    val name: String,
    val description: String,
    val type: StatusEffectType,
    val duration: Int, // seconds remaining
    val stacks: Int = 1,
    val intensity: Float = 1f // 0-1 for visual intensity
)

enum class StatusEffectType {
    BUFF, DEBUFF, NEUTRAL
}

@Composable
fun StatusEffectsPanel(
    statusEffects: List<StatusEffect>,
    modifier: Modifier = Modifier
) {
    if (statusEffects.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1a1a2e).copy(alpha = 0.95f),
                                Color(0xFF16213e).copy(alpha = 0.9f),
                                Color(0xFF0f172a).copy(alpha = 0.95f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8b5cf6).copy(alpha = 0.3f),
                                Color(0xFF7c3aed).copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Status Effects",
                            tint = Color(0xFF8b5cf6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Effects (${statusEffects.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        )
                    }

                    // Effects list
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(statusEffects) { effect ->
                            StatusEffectItem(
                                effect = effect
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusEffectItem(
    effect: StatusEffect,
    modifier: Modifier = Modifier
) {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val effectColor = when (effect.type) {
        StatusEffectType.BUFF -> Color(0xFF10b981)
        StatusEffectType.DEBUFF -> Color(0xFFef4444)
        StatusEffectType.NEUTRAL -> Color(0xFF6366f1)
    }

    val effectIcon = when (effect.type) {
        StatusEffectType.BUFF -> when {
            effect.name.contains("strength", ignoreCase = true) -> Icons.Default.FitnessCenter
            effect.name.contains("speed", ignoreCase = true) -> Icons.Default.Speed
            effect.name.contains("heal", ignoreCase = true) -> Icons.Default.Healing
            effect.name.contains("shield", ignoreCase = true) -> Icons.Default.Shield
            effect.name.contains("blessing", ignoreCase = true) -> Icons.Default.AutoAwesome
            else -> Icons.Default.TrendingUp
        }
        StatusEffectType.DEBUFF -> when {
            effect.name.contains("poison", ignoreCase = true) -> Icons.Default.Dangerous
            effect.name.contains("curse", ignoreCase = true) -> Icons.Default.Warning
            effect.name.contains("slow", ignoreCase = true) -> Icons.Default.AccessTime
            effect.name.contains("weak", ignoreCase = true) -> Icons.Default.TrendingDown
            effect.name.contains("blind", ignoreCase = true) -> Icons.Default.VisibilityOff
            else -> Icons.Default.TrendingDown
        }
        StatusEffectType.NEUTRAL -> Icons.Default.Circle
    }

    Card(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            effectColor.copy(alpha = effect.intensity * 0.3f * pulseAlpha),
                            Color(0xFF1e293b).copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 2.dp,
                    color = effectColor.copy(alpha = pulseAlpha * 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = effectIcon,
                    contentDescription = effect.name,
                    tint = effectColor.copy(alpha = pulseAlpha),
                    modifier = Modifier.size(20.dp)
                )
                
                if (effect.duration > 0) {
                    Text(
                        text = "${effect.duration}s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                
                if (effect.stacks > 1) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = effectColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = effect.stacks.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        )
                    }
                }
            }
        }
    }
}