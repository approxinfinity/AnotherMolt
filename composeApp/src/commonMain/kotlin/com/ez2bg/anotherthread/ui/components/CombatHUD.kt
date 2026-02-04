package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.models.Player
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.LocationDto
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enhanced Player Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Level ${player.level} ${player.characterClass ?: "Adventurer"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                
                Badge(
                    containerColor = if (player.isInCombat == true) Color.Red else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (player.isInCombat == true) "⚔️ Combat" else "🕊️ Peace",
                        fontWeight = FontWeight.Bold,
                        color = if (player.isInCombat == true) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Enhanced Health Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "❤️ Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${player.currentHp} / ${player.maxHp}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getHealthColor(player.currentHp, player.maxHp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    val healthPercentage = (player.currentHp.toFloat() / player.maxHp.toFloat()).coerceIn(0f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(healthPercentage)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        getHealthColor(player.currentHp, player.maxHp).copy(alpha = 0.8f),
                                        getHealthColor(player.currentHp, player.maxHp)
                                    )
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .animateContentSize()
                    )
                    
                    // Health percentage text overlay
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(healthPercentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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
            
            // Enhanced Mana Bar
            if (player.maxMp > 0) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💙 Mana",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${player.currentMp} / ${player.maxMp}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                Color.Black.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        val manaPercentage = (player.currentMp.toFloat() / player.maxMp.toFloat()).coerceIn(0f, 1f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(manaPercentage)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF1976D2),
                                            Color(0xFF2196F3)
                                        )
                                    ),
                                    RoundedCornerShape(6.dp)
                                )
                                .animateContentSize()
                        )
                    }
                }
            }
        }
    }
}

private fun getHealthColor(currentHp: Int, maxHp: Int): Color {
    val percentage = currentHp.toFloat() / maxHp.toFloat()
    return when {
        percentage > 0.6f -> Color(0xFF4CAF50) // Green
        percentage > 0.3f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}
                    val healthPercentage = (player.currentHp.toFloat() / player.maxHp.toFloat()).coerceIn(0f, 1f)
                    val healthColor = when {
                        healthPercentage > 0.6f -> Color(0xFF4CAF50) // Green
                        healthPercentage > 0.3f -> Color(0xFFFF9800) // Orange  
                        else -> Color(0xFFF44336) // Red
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(healthPercentage)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        healthColor.copy(alpha = 0.8f),
                                        healthColor
            // Enhanced Mana Bar
            if (player.maxMp > 0) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mana",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${player.currentMp} / ${player.maxMp}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                Color.Black.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        val manaPercentage = (player.currentMp.toFloat() / player.maxMp.toFloat()).coerceIn(0f, 1f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(manaPercentage)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF2196F3).copy(alpha = 0.8f),
                                            Color(0xFF2196F3)
                                        )
                                    ),
                                    RoundedCornerShape(6.dp)
                                )
                                .animateContentSize()
                        )
                    }
                }
            }
        }
    }
}
        modifier = modifier.size(totalSize),
        contentAlignment = Alignment.Center
    ) {
        // Apply disorient rotation to the entire content if needed
        Box(
            modifier = Modifier
                .graphicsLayer {
                    if (isDisoriented) {
                        rotationZ = 180f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Ability ring (behind the thumbnail)
            AbilityIconRing(
                abilities = abilities,
                cooldowns = cooldowns,
                queuedAbilityId = queuedAbilityId,
                onAbilityClick = onAbilityClick,
                ringRadius = ringRadius,
                iconSize = iconSize
            )

            // Central circular thumbnail
            Box(
                modifier = Modifier
                    .size(thumbnailSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(2.dp, Color(0xFF4A4A4A), CircleShape)
                    .clickable { onLocationClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isBlinded) {
                    // Blind overlay - hide room contents
                    BlindOverlay(
                        roundsRemaining = blindRoundsRemaining,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Normal location display
                    location?.let { loc ->
                        if (!loc.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = loc.imageUrl,
                                contentDescription = loc.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback text
                            Text(
                                text = loc.name.take(2).uppercase(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } ?: Text(
                        text = "?",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Location name (always upright, not affected by disorient)
        location?.let { loc ->
            Text(
                text = loc.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-20).dp)
                    .widthIn(max = 150.dp)
            )
        }

        // Status indicators (disorient indicator)
        if (isDisoriented && disorientRoundsRemaining > 0) {
            DisorientIndicator(
                roundsRemaining = disorientRoundsRemaining,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 20.dp)
            )
        }
    }
}

/**
 * Compact variant of CombatHUD for smaller screens or when less space is available.
 */
@Composable
fun CombatHUDCompact(
    location: LocationDto?,
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int> = emptyMap(),
    queuedAbilityId: String? = null,
    onAbilityClick: (AbilityDto) -> Unit,
    onLocationClick: () -> Unit,
    isBlinded: Boolean = false,
    blindRoundsRemaining: Int = 0,
    isDisoriented: Boolean = false,
    modifier: Modifier = Modifier
) {
    CombatHUD(
        location = location,
        abilities = abilities,
        cooldowns = cooldowns,
        queuedAbilityId = queuedAbilityId,
        onAbilityClick = onAbilityClick,
        onLocationClick = onLocationClick,
        isBlinded = isBlinded,
        blindRoundsRemaining = blindRoundsRemaining,
        isDisoriented = isDisoriented,
        modifier = modifier,
        thumbnailSize = 80.dp,
        ringRadius = 65.dp,
        iconSize = 28.dp
    )
}
