package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ez2bg.anotherthread.models.Ability
import kotlin.math
 * Arranges ability icons in a circular ring around a center point.
 *
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background ring with enhanced glow
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = 100f
                        ),
                        CircleShape
                    )
            )
            
            // Arrange abilities in a circle
            abilities.forEachIndexed { index, ability ->
                val angle = (index * 360f / abilities.size) - 90f // Start from top
                val angleRad = Math.toRadians(angle.toDouble())
                val radius = 80.dp
                
                val offsetX = (radius.value * kotlin.math.cos(angleRad)).dp
                val offsetY = (radius.value * kotlin.math.sin(angleRad)).dp
                
                Box(
                    modifier = Modifier.offset(
                        x = offsetX,
                        y = offsetY
                    )
                ) {
                    AbilityIconButton(
                        iconText = ability.icon,
                        name = ability.name,
                        onClick = { onAbilityClick(ability) },
                        isUsable = ability.isUsable,
                        cooldownRemaining = ability.cooldownRemaining,
                        modifier = Modifier.size(56.dp)
                    )
                    
                    // Connection line from center
                    if (abilities.size > 1) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (-offsetX * 0.35f),
                                    y = (-offsetY * 0.35f)
                                )
                                .width(2.dp)
                                .height((radius.value * 0.65).dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        )
                                    ),
                                    RoundedCornerShape(1.dp)
                                )
                                .rotate(angle + 90f)
                        )
                    }
                }
            }
            
            // Enhanced center hub
            Card(
                modifier = Modifier.size(40.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "⚔️",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(1f, 1f),
                                blurRadius = 2f
                            )
                        )
                    )
                }
            }
        }
    }
}
            val offsetY = (ringRadius.value * sin(angleRadians)).toFloat().dp

            AbilityIconButton(
                ability = ability,
                cooldownRounds = cooldowns[ability.id] ?: 0,
                isQueued = ability.id == queuedAbilityId,
                onClick = { onAbilityClick(ability) },
                size = iconSize,
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            )
        }
    }
}

/**
 * Variant that displays abilities in two rings for larger ability sets.
 */
@Composable
fun AbilityIconDoubleRing(
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int> = emptyMap(),
    queuedAbilityId: String? = null,
    onAbilityClick: (AbilityDto) -> Unit,
    modifier: Modifier = Modifier,
    innerRadius: Dp = 70.dp,
    outerRadius: Dp = 110.dp,
    iconSize: Dp = 32.dp,
    maxInnerIcons: Int = 6,
    maxOuterIcons: Int = 10
) {
    val usableAbilities = abilities.filter { it.abilityType != "passive" }

    // Split abilities between inner and outer ring
    val innerAbilities = usableAbilities.take(maxInnerIcons)
    val outerAbilities = usableAbilities.drop(maxInnerIcons).take(maxOuterIcons)

    Box(
        modifier = modifier.size(outerRadius * 2 + iconSize + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner ring
        if (innerAbilities.isNotEmpty()) {
            val innerCount = innerAbilities.size
            innerAbilities.forEachIndexed { index, ability ->
                val angleDegrees = (360f / innerCount) * index - 90f
                val angleRadians = angleDegrees * PI / 180.0
                val offsetX = (innerRadius.value * cos(angleRadians)).toFloat().dp
                val offsetY = (innerRadius.value * sin(angleRadians)).toFloat().dp

                AbilityIconButton(
                    ability = ability,
                    cooldownRounds = cooldowns[ability.id] ?: 0,
                    isQueued = ability.id == queuedAbilityId,
                    onClick = { onAbilityClick(ability) },
                    size = iconSize,
                    modifier = Modifier.offset(x = offsetX, y = offsetY)
                )
            }
        }

        // Outer ring
        if (outerAbilities.isNotEmpty()) {
            val outerCount = outerAbilities.size
            outerAbilities.forEachIndexed { index, ability ->
                // Offset outer ring slightly so icons don't align
                val angleDegrees = (360f / outerCount) * index - 90f + (180f / outerCount)
                val angleRadians = angleDegrees * PI / 180.0
                val offsetX = (outerRadius.value * cos(angleRadians)).toFloat().dp
                val offsetY = (outerRadius.value * sin(angleRadians)).toFloat().dp

                AbilityIconButton(
                    ability = ability,
                    cooldownRounds = cooldowns[ability.id] ?: 0,
                    isQueued = ability.id == queuedAbilityId,
                    onClick = { onAbilityClick(ability) },
                    size = iconSize - 4.dp,  // Slightly smaller for outer ring
                    modifier = Modifier.offset(x = offsetX, y = offsetY)
                )
            }
        }
    }
}
