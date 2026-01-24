package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ez2bg.anotherthread.api.AbilityDto

/**
 * Data class representing a potential target for an ability.
 */
data class CombatTarget(
    val id: String,
    val name: String,
    val currentHp: Int,
    val maxHp: Int,
    val isPlayer: Boolean,
    val isAlive: Boolean = true
)

/**
 * Overlay for selecting a target when casting a single-target ability.
 *
 * Displays available targets as clickable cards with health information.
 */
@Composable
fun TargetSelectionOverlay(
    ability: AbilityDto,
    targets: List<CombatTarget>,
    onTargetSelected: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onCancel() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with ability name and cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
