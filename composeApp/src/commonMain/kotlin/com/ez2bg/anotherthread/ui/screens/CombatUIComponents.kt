package com.ez2bg.anotherthread.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.ExitDto
import com.ez2bg.anotherthread.ui.components.AbilityIconButton
import com.ez2bg.anotherthread.ui.components.AbilityIconMapper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// =============================================================================
// NAVIGATION BUTTONS
// =============================================================================

/**
 * Directional navigation button for exits.
 */
@Composable
fun DirectionalButton(
    exit: ExitDto,
    icon: ImageVector,
    color: Color,
    offsetX: Dp,
    offsetY: Dp,
    buttonSize: Dp,
    enabled: Boolean = true,
    onNavigate: (ExitDto) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val navScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.2f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "dirScale"
    )

    // Grey out when disabled (over-encumbered)
    val buttonColor = if (enabled) color else Color.Gray

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(buttonSize)
            .scale(navScale)
            .clip(CircleShape)
            .background(buttonColor, CircleShape)
            .then(
                if (enabled) {
                    Modifier.pointerInput(exit.locationId) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                                onNavigate(exit)
                            }
                        )
                    }
                } else {
                    Modifier  // No tap handling when disabled
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (exit.direction == ExitDirection.ENTER) "Enter" else exit.direction.name,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Purple phasewalk button with boot icon and mana cost label inside the circle.
 * Shows for directions without exits where phasewalk is available.
 * Style matches AbilityIconButton with cost displayed at bottom of circle.
 *
 * When disabled (not enough mana or in combat), shows as grey and doesn't respond to taps.
 */
@Composable
fun PhasewalkButton(
    direction: String,
    locationName: String,
    offsetX: Dp,
    offsetY: Dp,
    buttonSize: Dp,
    enabled: Boolean = true,
    onPhasewalk: (String) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val navScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.2f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "phasewalkScale"
    )

    // Colors: purple when enabled, grey when disabled
    val buttonColor = if (enabled) Color(0xFF9C27B0) else Color(0xFF666666)
    val iconTint = if (enabled) Color.White else Color(0xFFAAAAAA)
    val costColor = if (enabled) Color(0xFF64B5F6) else Color(0xFF888888)

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(buttonSize + 8.dp),  // Extra space for cost label
        contentAlignment = Alignment.Center
    ) {
        // Main button
        Box(
            modifier = Modifier
                .size(buttonSize)
                .scale(navScale)
                .clip(CircleShape)
                .background(buttonColor, CircleShape)
                .border(1.dp, Color.White.copy(alpha = if (enabled) 0.3f else 0.1f), CircleShape)
                .then(
                    if (enabled) {
                        Modifier.pointerInput(direction) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                    onPhasewalk(direction)
                                }
                            )
                        }
                    } else {
                        Modifier  // No tap handling when disabled
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = if (enabled) "Phasewalk $direction to $locationName" else "Phasewalk unavailable (need 2 MP, no combat)",
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }

        // Mana cost label at bottom of circle (matching ability button style)
        Text(
            text = "2 MP",
            color = costColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 2.dp)
        )
    }
}

/**
 * Purple floating action button for ENTER exits and shop back buttons.
 */
@Composable
fun PurpleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "purpleButtonScale"
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xFF9C27B0), CircleShape)  // Purple
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

// =============================================================================
// ABILITY ROW (flat row above scroll section)
// =============================================================================

/**
 * Dropdown panel for party-specific abilities (all_allies, single_ally).
 * Appears when tapping the party icon next to the character button.
 */
@Composable
fun PartyAbilitiesDropdown(
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int>,
    queuedAbilityId: String?,
    currentMana: Int = 0,
    currentStamina: Int = 0,
    isPartyLeader: Boolean = false,
    onAbilityClick: (AbilityDto) -> Unit,
    onLeaveParty: () -> Unit,
    onDisbandParty: () -> Unit = {},
    iconMappings: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val iconSize = 36.dp

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Text(
            text = "Party Abilities",
            color = Color(0xFF81C784),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Abilities in a flow layout
        if (abilities.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                abilities.forEach { ability ->
                    val canAfford = (ability.manaCost <= currentMana) && (ability.staminaCost <= currentStamina)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(50.dp)
                    ) {
                        AbilityIconButton(
                            ability = ability,
                            cooldownRounds = cooldowns[ability.id] ?: 0,
                            isQueued = ability.id == queuedAbilityId,
                            enabled = canAfford,
                            onClick = { onAbilityClick(ability) },
                            size = iconSize,
                            customIconName = iconMappings[ability.id]
                        )
                        Text(
                            text = ability.name,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Divider before Leave/Disband Party
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Leave Party (for members) or Disband Party (for leaders)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFD32F2F).copy(alpha = 0.8f))
                .clickable { if (isPartyLeader) onDisbandParty() else onLeaveParty() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isPartyLeader) "Disband Party" else "Leave Party",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Horizontal row of ability buttons displayed above the event log.
 * Non-creature-specific actions (self buffs, area effects, etc.)
 */
@Composable
fun AbilityRow(
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int>,
    queuedAbilityId: String?,
    currentMana: Int = 0,
    currentStamina: Int = 0,
    onAbilityClick: (AbilityDto) -> Unit,
    iconMappings: Map<String, String> = emptyMap(),
    onInspectClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val iconSize = 32.dp

    // Filter to show non-passive and non-navigation abilities
    // Navigation abilities (like Phasewalk) are shown on the direction ring instead
    // No limit - FlowRow handles wrapping if there are many abilities
    val displayAbilities = abilities.filter {
        it.abilityType != "passive" && it.abilityType != "navigation"
    }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // FlowRow wraps ability buttons to multiple lines if needed
        // Note: Basic attack is now only in creature action modal, not here
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            displayAbilities.forEach { ability ->
                val canAfford = (ability.manaCost <= currentMana) && (ability.staminaCost <= currentStamina)
                AbilityIconButton(
                    ability = ability,
                    cooldownRounds = cooldowns[ability.id] ?: 0,
                    isQueued = ability.id == queuedAbilityId,
                    enabled = canAfford,
                    onClick = { onAbilityClick(ability) },
                    size = iconSize,
                    customIconName = iconMappings[ability.id]
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Inspect/Look button - always visible
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2196F3).copy(alpha = 0.8f))
                .clickable { onInspectClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = "Look around",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// =============================================================================
// ACTION BUTTONS
// =============================================================================

/**
 * Ability action button with cooldown overlay and cost indicator.
 */
@Composable
fun AbilityActionButton(
    ability: AbilityDto,
    cooldown: Int,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    val isDisabled = cooldown > 0 || !canAfford
    val alpha = if (isDisabled) 0.5f else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable(enabled = !isDisabled, onClick = onClick)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AbilityIconMapper.getAbilityTypeColor(ability.abilityType).copy(alpha = 0.3f))
                .border(2.dp, AbilityIconMapper.getAbilityTypeColor(ability.abilityType), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AbilityIconMapper.getIcon(ability),
                contentDescription = ability.name,
                tint = AbilityIconMapper.getAbilityTypeColor(ability.abilityType),
                modifier = Modifier.size(24.dp)
            )

            // Cooldown overlay
            if (cooldown > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$cooldown",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = ability.name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Cost indicator
        val costText = when {
            ability.manaCost > 0 && ability.staminaCost > 0 -> "${ability.manaCost}M ${ability.staminaCost}S"
            ability.manaCost > 0 -> "${ability.manaCost}M"
            ability.staminaCost > 0 -> "${ability.staminaCost}S"
            else -> ""
        }
        if (costText.isNotEmpty()) {
            Text(
                text = costText,
                style = MaterialTheme.typography.labelSmall,
                color = if (canAfford) Color.White.copy(alpha = 0.5f) else Color(0xFFE53935),
                fontSize = 9.sp
            )
        }
    }
}

/**
 * Generic action button used in interaction modals.
 */
@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Individual action button for player interaction modal.
 */
@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Action button positioned using Offset (used in detail view).
 */
@Composable
fun OffsetActionButton(
    offset: Offset,
    color: Color,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.3f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "actionScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "actionGlow"
    )

    Box(
        modifier = Modifier
            .offset(x = offset.x.dp, y = offset.y.dp)
            .size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((32 * scale).dp)
                .background(Color(0xFFFF9800).copy(alpha = glowAlpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                            onClick()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =============================================================================
// RESOURCE BARS
// =============================================================================

/**
 * Player resource bar showing HP, Mana, Stamina, and Gold.
 */
@Composable
fun PlayerResourceBar(
    currentHp: Int, maxHp: Int,
    currentMana: Int, maxMana: Int,
    currentStamina: Int, maxStamina: Int,
    gold: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ResourceMiniBar("HP", currentHp, maxHp, Color(0xFFE53935), Modifier.weight(1f))
        ResourceMiniBar("MP", currentMana, maxMana, Color(0xFF42A5F5), Modifier.weight(1f))
        ResourceMiniBar("SP", currentStamina, maxStamina, Color(0xFFFFA726), Modifier.weight(1f))
        // Gold display
        Box(
            modifier = Modifier
                .height(20.dp)
                .background(Color(0xFF333333), RoundedCornerShape(3.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$gold g",
                color = Color(0xFFFFD700), // Gold color
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Mini resource bar for HP/Mana/Stamina display.
 */
@Composable
fun ResourceMiniBar(label: String, current: Int, max: Int, color: Color, modifier: Modifier) {
    val fraction = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier
            .height(20.dp)
            .background(Color(0xFF333333), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color)
            )
        }
        Text(
            text = "$label $current/$max",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// =============================================================================
// MODE TOGGLE
// =============================================================================

/**
 * Toggle switch for switching between Create and Adventure mode.
 */
@Composable
fun ModeToggle(
    isCreateMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = if (isCreateMode) Color(0xFF2E7D32).copy(alpha = 0.6f) else Color(0xFF9C27B0).copy(alpha = 0.6f)
    val thumbColor = if (isCreateMode) Color(0xFF4CAF50) else Color(0xFFBA68C8)

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(onClick = onToggle)
            .width(40.dp)
            .height(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(14.dp)
                .background(trackColor, RoundedCornerShape(7.dp)),
            contentAlignment = if (isCreateMode) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(10.dp)
                    .background(thumbColor, CircleShape)
            )
        }
    }
}

// =============================================================================
// COMBAT RING
// =============================================================================

/**
 * Circular ring of ability buttons around a creature.
 */
@Composable
fun CreatureCombatRing(
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int>,
    queuedAbilityId: String?,
    onAbilityClick: (AbilityDto) -> Unit
) {
    val ringRadius = 80.dp
    val iconSize = 36.dp
    val maxIcons = 8
    val displayAbilities = abilities.take(maxIcons)
    val totalIcons = displayAbilities.size

    displayAbilities.forEachIndexed { index, ability ->
        val angleDegrees = (360f / totalIcons) * index - 90f
        val angleRadians = angleDegrees * PI / 180.0
        val offsetX = (ringRadius.value * cos(angleRadians)).toFloat().dp
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

// =============================================================================
// GIVE UP BUTTON
// =============================================================================

/**
 * Black skull button shown when player is downed.
 * Tap to open confirmation dialog for voluntary death.
 */
@Composable
fun GiveUpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.9f))
            .border(1.dp, Color(0xFF666666), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Skull emoji as text since Material Icons doesn't have a skull
        Text(
            text = "\uD83D\uDC80", // Skull emoji
            fontSize = 16.sp
        )
    }
}
