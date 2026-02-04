package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
    val isOnCooldown = cooldownRemaining > 0
    val alpha = if (isOnCooldown || !isUsable) 0.5f else 1.0f
    val backgroundColor = when {
        isOnCooldown -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        !isUsable -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    }
    
    Box {
        Button(
            onClick = { if (isUsable && !isOnCooldown) onClick() },
            modifier = modifier
                .size(64.dp)
                .alpha(alpha),
            enabled = isUsable && !isOnCooldown,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = iconText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOnCooldown || !isUsable) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                    else 
                        MaterialTheme.colorScheme.onPrimary
                )
                
                if (name.isNotEmpty()) {
                    Text(
                        text = name,
                        fontSize = 8.sp,
                        color = if (isOnCooldown || !isUsable) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                        else 
                            MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Enhanced cooldown overlay
        if (isOnCooldown) {
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .alpha(0.95f),
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⏱️",
                            fontSize = 16.sp
                        )
                        Text(
                            text = cooldownRemaining.toString(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(
                                    color = Color.Black,
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
}
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
    size: Dp = 36.dp
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

    val icon = AbilityIconMapper.getIcon(ability)

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
            // 4-letter abbreviation text (replaces icon)
            val abbreviation = AbilityIconMapper.getAbbreviation(ability.name)
            Text(
                text = abbreviation,
                color = if (isOnCooldown || !enabled) Color.Gray else Color.White,
                fontSize = (size.value * 0.3f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
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
    val abbreviation = AbilityIconMapper.getAbbreviation(ability.name)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = abbreviation,
            color = Color.White,
            fontSize = (size.value * 0.3f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )
    }
}
