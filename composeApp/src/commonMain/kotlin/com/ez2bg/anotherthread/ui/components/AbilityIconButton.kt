package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ez2bg.anotherthread.api.AbilityDto
import kotlinx.coroutines.delay

/**
 * A circular button representing an ability that can be cast in combat.
 *
 * Features:
 * - Icon based on ability type/effects
 * - Cooldown overlay with remaining rounds display
 * - Queued state glow ring
 * - Press animation
 * - Disabled state handling
 */
@Composable
fun AbilityIconButton(
    ability: AbilityDto,
    cooldownRounds: Int = 0,
    isQueued: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    customIconName: String? = null
) {
    val isOnCooldown = cooldownRounds > 0
    val canUse = enabled && !isOnCooldown

    // Animation states
    var isPressed by remember { mutableStateOf(false) }
    var showNameTooltip by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.85f
            isQueued -> 1.1f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isQueued) 0.6f else 0f,
        animationSpec = tween(300)
    )

    val backgroundColor = when {
        isOnCooldown -> Color(0xFF424242)  // Gray when on cooldown
        !enabled -> Color(0xFF616161)       // Darker gray when disabled
        else -> AbilityIconMapper.getAbilityTypeColor(ability.abilityType)
    }

    val icon = AbilityIconMapper.getIcon(ability, customIconName)

    Box(
        modifier = modifier.size(size + 8.dp),  // Extra space for glow
        contentAlignment = Alignment.Center
    ) {
        // Glow ring when queued
        if (isQueued) {
            Box(
                modifier = Modifier
                    .size(size + 8.dp)
                    .background(
                        Color(0xFF4CAF50).copy(alpha = glowAlpha),
                        CircleShape
                    )
            )
        }

        // Main button
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(backgroundColor, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .then(
                    if (canUse) {
                        Modifier.pointerInput(ability.id) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                },
                                onTap = { onClick() },
                                onLongPress = {
                                    // Show ability name on long press (don't trigger action)
                                    showNameTooltip = true
                                }
                            )
                        }
                    } else {
                        // Even when disabled/on cooldown, allow long-press to see name
                        Modifier.pointerInput(ability.id) {
                            detectTapGestures(
                                onLongPress = {
                                    showNameTooltip = true
                                }
                            )
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Material icon from AbilityIconMapper
            Icon(
                imageVector = icon,
                contentDescription = ability.name,
                tint = if (isOnCooldown || !enabled) Color.Gray else Color.White,
                modifier = Modifier.size(size * 0.55f)
            )

            // Cooldown overlay
            if (isOnCooldown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$cooldownRounds",
                        color = Color.White,
                        fontSize = (size.value * 0.4f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Cost label below button
        val costText = when {
            ability.manaCost > 0 && ability.staminaCost > 0 -> "${ability.manaCost}M ${ability.staminaCost}S"
            ability.manaCost > 0 -> "${ability.manaCost} MP"
            ability.staminaCost > 0 -> "${ability.staminaCost} SP"
            else -> null
        }
        if (costText != null) {
            Text(
                text = costText,
                color = when {
                    isOnCooldown || !enabled -> Color.Gray
                    ability.manaCost > 0 -> Color(0xFF64B5F6)
                    else -> Color(0xFFFFA726)
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 2.dp)
            )
        }

        // Name tooltip on long press
        if (showNameTooltip) {
            LaunchedEffect(Unit) {
                delay(2000)  // Auto-hide after 2 seconds
                showNameTooltip = false
            }

            Box(
                modifier = Modifier
                    .offset(y = -(size + 12.dp))
                    .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            showNameTooltip = false
                        }
                    }
            ) {
                Text(
                    text = ability.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A smaller variant for displaying in lists or compact UIs.
 */
@Composable
fun AbilityIconSmall(
    ability: AbilityDto,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val backgroundColor = AbilityIconMapper.getAbilityTypeColor(ability.abilityType)
    val icon = AbilityIconMapper.getIcon(ability)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = ability.name,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
