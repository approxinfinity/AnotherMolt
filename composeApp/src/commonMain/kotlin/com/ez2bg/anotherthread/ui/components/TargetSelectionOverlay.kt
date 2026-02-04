package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ez2bg.anotherthread.models.Enemy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Select Target",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    FilledIconButton(
                        onClick = onCancel,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Target list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targets) { target ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTargetSelected(target) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Target name and level
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = target.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ) {
                                        Text(
                                            text = "Lv. ${target.level}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Health information
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Health",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    
                                    val healthPercentage = target.currentHp.toFloat() / target.maxHp.toFloat()
                                    Text(
                                        text = "${target.currentHp}/${target.maxHp} (${(healthPercentage * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            healthPercentage > 0.6f -> Color(0xFF4CAF50)
                                            healthPercentage > 0.3f -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        }
                                    )
                                }
                                
                                // Health bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp)
                                        )
                                ) {
                                    val healthPercentage = target.currentHp.toFloat() / target.maxHp.toFloat()
                                    val healthColor = when {
                                        healthPercentage > 0.6f -> Color(0xFF4CAF50)
                                        healthPercentage > 0.3f -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
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
                                                    )
                                                ),
                                                RoundedCornerShape(4.dp)
                                            )
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
                Spacer(Modifier.width(40.dp))  // Balance the close button
                Text(
                    text = "Select Target for ${ability.name}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Target type indicator
            Text(
                text = when (ability.targetType) {
                    "single_enemy" -> "Choose an enemy"
                    "single_ally" -> "Choose an ally"
                    else -> "Choose a target"
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            // Target cards
            targets.filter { it.isAlive }.forEach { target ->
                TargetCard(
                    target = target,
                    isEnemy = ability.targetType == "single_enemy",
                    onClick = { onTargetSelected(target.id) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // No valid targets message
            if (targets.none { it.isAlive }) {
                Text(
                    text = "No valid targets available",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Card displaying a potential target with health information.
 */
@Composable
fun TargetCard(
    target: CombatTarget,
    isEnemy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isEnemy) Color(0xFFD32F2F) else Color(0xFF4CAF50)
    val iconColor = if (isEnemy) Color(0xFFD32F2F) else Color(0xFF64B5F6)

    Row(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon indicating player or creature
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (target.isPlayer) Icons.Filled.Person else Icons.Filled.Pets,
                    contentDescription = if (target.isPlayer) "Player" else "Creature",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = target.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Health bar
                Spacer(Modifier.height(4.dp))
                HealthBar(
                    currentHp = target.currentHp,
                    maxHp = target.maxHp,
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp)
                )
            }
        }

        // HP text
        Text(
            text = "${target.currentHp}/${target.maxHp}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

/**
 * Simple health bar visualization.
 */
@Composable
fun HealthBar(
    currentHp: Int,
    maxHp: Int,
    modifier: Modifier = Modifier
) {
    val healthPercent = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
    val healthColor = when {
        healthPercent > 0.6f -> Color(0xFF4CAF50)  // Green
        healthPercent > 0.3f -> Color(0xFFFF9800)  // Orange
        else -> Color(0xFFD32F2F)                   // Red
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF424242))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(healthPercent)
                .background(healthColor, RoundedCornerShape(4.dp))
        )
    }
}
