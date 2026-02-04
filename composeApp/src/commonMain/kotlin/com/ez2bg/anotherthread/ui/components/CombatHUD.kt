package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.LocationDto

/**
 * Combat HUD displaying a circular location thumbnail surrounded by ability icons.
 *
 * Features:
 * - Circular 100dp location thumbnail in center
 * - Ring of ability icons around the thumbnail
 * - Optional blind overlay (obscures view)
 * - Optional disorient rotation (flips view 180°)
 * - Location name display
 */
@Composable
fun CombatHUD(
    location: LocationDto?,
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int> = emptyMap(),
    queuedAbilityId: String? = null,
    onAbilityClick: (AbilityDto) -> Unit,
    onLocationClick: () -> Unit,
    isBlinded: Boolean = false,
    blindRoundsRemaining: Int = 0,
    isDisoriented: Boolean = false,
    disorientRoundsRemaining: Int = 0,
    modifier: Modifier = Modifier,
    thumbnailSize: Dp = 100.dp,
    ringRadius: Dp = 80.dp,
    iconSize: Dp = 36.dp
) {
    // Calculate total size needed for the HUD
    val totalSize = ringRadius * 2 + iconSize + 32.dp

    Box(
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
