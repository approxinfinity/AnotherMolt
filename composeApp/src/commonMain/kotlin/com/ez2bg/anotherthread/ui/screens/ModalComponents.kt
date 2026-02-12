package com.ez2bg.anotherthread.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.DiplomacyResultDto
import com.ez2bg.anotherthread.api.ReactionResultDto
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.SealableRiftDto
import com.ez2bg.anotherthread.api.TrainerAbilityInfo
import com.ez2bg.anotherthread.api.TrainerInfoResponse
import com.ez2bg.anotherthread.api.UnconnectedAreaDto
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.ui.admin.getTerrainColor

// =============================================================================
// BASIC ATTACK TARGET MODAL
// =============================================================================

/**
 * Modal overlay for selecting a target for basic attack when multiple creatures are present.
 */
@Composable
fun BasicAttackTargetModal(
    creatures: List<CreatureDto>,
    onTargetSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clickable(enabled = false) { /* Prevent click-through */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select Target",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List of creatures to attack
                creatures.forEach { creature ->
                    Surface(
                        onClick = { onTargetSelected(creature.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF3A3A4E)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gavel,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = creature.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nevermind button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Nevermind")
                }
            }
        }
    }
}

// =============================================================================
// GIVE UP CONFIRMATION MODAL
// =============================================================================

/**
 * Confirmation modal for voluntary death.
 */
@Composable
fun GiveUpConfirmationModal(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clickable(enabled = false) { /* Prevent click-through */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Skull icon
                Text(
                    text = "\uD83D\uDC80",
                    fontSize = 48.sp
                )

                Text(
                    text = "Give Up?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "You will die and respawn at Tun du Lac. You may lose items and gold.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C) // Dark red
                        )
                    ) {
                        Text("Give Up", color = Color.White)
                    }
                }
            }
        }
    }
}

// =============================================================================
// PLAYER INTERACTION MODAL
// =============================================================================

/**
 * Modal for interacting with another player.
 * Shows action icons: attack, rob, party
 * Expandable to show player picture and description
 * If in same party, shows additional party actions: heal, give
 */
@Composable
fun PlayerInteractionModal(
    player: UserDto,
    isInSameParty: Boolean,
    hasPendingPartyInvite: Boolean = false,
    onAttack: () -> Unit,
    onRob: () -> Unit,
    onInviteToParty: () -> Unit,
    onAcceptParty: () -> Unit,
    onHeal: () -> Unit,
    onGive: () -> Unit,
    onDismiss: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clickable(enabled = false) { /* Prevent click-through */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Player name header
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4CAF50), // Green for players
                    fontWeight = FontWeight.Bold
                )

                // Main action icons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attack action
                    PlayerActionButton(
                        icon = Icons.Filled.SportsMartialArts,
                        label = "Attack",
                        color = Color(0xFFE53935), // Red
                        onClick = {
                            onAttack()
                            onDismiss()
                        }
                    )

                    // Rob action
                    PlayerActionButton(
                        icon = Icons.Filled.Gavel,
                        label = "Rob",
                        color = Color(0xFFFF9800), // Orange
                        onClick = {
                            onRob()
                            onDismiss()
                        }
                    )

                    // Party action - shows "Accept Party" (green) if pending invite, otherwise "Party" (blue)
                    PlayerActionButton(
                        icon = if (hasPendingPartyInvite) Icons.Filled.Check else Icons.Filled.Person,
                        label = if (hasPendingPartyInvite) "Accept" else "Party",
                        color = if (hasPendingPartyInvite) Color(0xFF4CAF50) else Color(0xFF2196F3),
                        onClick = {
                            if (hasPendingPartyInvite) {
                                onAcceptParty()
                            } else {
                                onInviteToParty()
                            }
                            onDismiss()
                        }
                    )
                }

                // Party actions row (only when in same party)
                if (isInSameParty) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "Party Actions",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Heal action
                        PlayerActionButton(
                            icon = Icons.Filled.Dangerous, // Will replace with heart/heal icon
                            label = "Heal",
                            color = Color(0xFF4CAF50), // Green
                            onClick = {
                                onHeal()
                                onDismiss()
                            }
                        )

                        // Give action
                        PlayerActionButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            label = "Give",
                            color = Color(0xFF9C27B0), // Purple
                            onClick = onGive
                        )
                    }
                }

                // Expand/collapse button
                TextButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = if (isExpanded) "Hide Details" else "Show Details",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Expanded section with player picture and description
                if (isExpanded) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )

                    // Player picture
                    if (player.imageUrl != null) {
                        AsyncImage(
                            model = "${AppConfig.api.baseUrl}${player.imageUrl}",
                            contentDescription = "Player portrait",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "\uD83D\uDC64",
                                fontSize = 48.sp
                            )
                        }
                    }

                    // Player description
                    val description = player.appearanceDescription.ifEmpty {
                        player.desc.ifEmpty { "No description available" }
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Level info
                    Text(
                        text = "Level ${player.level}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// CREATURE INTERACTION MODAL
// =============================================================================

/**
 * Modal for interacting with a creature - shows available actions.
 * Replaces the old detail view with ability ring.
 */
@Composable
fun CreatureInteractionModal(
    creature: CreatureDto,
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int>,
    currentMana: Int,
    currentStamina: Int,
    playerGold: Int = 0,
    diplomacyResult: DiplomacyResultDto? = null,
    isDiplomacyLoading: Boolean = false,
    reactionResult: ReactionResultDto? = null,
    onBasicAttack: () -> Unit,
    onAbilityClick: (AbilityDto) -> Unit,
    onTrain: (() -> Unit)?,  // Only for trainers
    onBribe: () -> Unit = {},
    onParley: () -> Unit = {},
    onLook: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Filter abilities to those that can target enemies
    val combatAbilities = abilities.filter { ability ->
        ability.abilityType != "passive" &&
        ability.abilityType != "navigation" &&
        (ability.targetType == "single_enemy" || ability.targetType == "all_enemies" || ability.targetType == "area")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .heightIn(max = 500.dp)
                .clickable(enabled = false) { /* Prevent click-through */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Creature name header with aggressive indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = creature.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (creature.isAggressive) Color(0xFFE53935) else Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                    if (creature.isAggressive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Dangerous,
                            contentDescription = "Aggressive",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Level indicator
                Text(
                    text = "Level ${creature.level}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )

                // NPC Reaction badge
                if (reactionResult != null) {
                    val reactionColor = when (reactionResult.reaction) {
                        "hostile" -> Color(0xFFE53935)    // Red
                        "uncertain" -> Color(0xFFFF9800)  // Orange
                        "friendly" -> Color(0xFF4CAF50)   // Green
                        else -> Color.White.copy(alpha = 0.6f)
                    }
                    val reactionLabel = reactionResult.reaction.replaceFirstChar { it.uppercase() }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = reactionColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, reactionColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "$reactionLabel — ${reactionResult.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = reactionColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Main action row - Basic Attack + Look
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Basic Attack
                    ActionButton(
                        icon = Icons.Filled.SportsMartialArts,
                        label = "Attack",
                        color = Color(0xFFE53935),
                        onClick = {
                            onBasicAttack()
                            onDismiss()
                        }
                    )

                    // Look/Inspect
                    ActionButton(
                        icon = Icons.Filled.Search,
                        label = "Look",
                        color = Color(0xFF2196F3),
                        onClick = onLook
                    )

                    // Train (only for trainer creatures)
                    if (onTrain != null && creature.isTrainer) {
                        ActionButton(
                            icon = Icons.Filled.School,
                            label = "Train",
                            color = Color(0xFF4CAF50),
                            onClick = {
                                onTrain()
                                onDismiss()
                            }
                        )
                    }
                }

                // Diplomacy section - show Bribe/Parley for faction creatures
                if (diplomacyResult != null && diplomacyResult.success) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "Diplomacy",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bribe - costs gold
                        ActionButton(
                            icon = Icons.Filled.Payments,
                            label = "Bribe",
                            color = Color(0xFFFFD700),  // Gold color
                            enabled = !isDiplomacyLoading,
                            onClick = onBribe
                        )

                        // Parley - skill check
                        ActionButton(
                            icon = Icons.Filled.Forum,
                            label = "Parley",
                            color = Color(0xFF9C27B0),  // Purple for wisdom
                            enabled = !isDiplomacyLoading,
                            onClick = onParley
                        )
                    }

                    // Show hint about diplomacy
                    Text(
                        text = "Avoid combat through negotiation",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                } else if (isDiplomacyLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White.copy(alpha = 0.6f),
                        strokeWidth = 2.dp
                    )
                }

                // Combat abilities section
                if (combatAbilities.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "Combat Abilities",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    // Abilities in a flow layout
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        combatAbilities.forEach { ability ->
                            val canAfford = (ability.manaCost <= currentMana) && (ability.staminaCost <= currentStamina)
                            val cooldown = cooldowns[ability.id] ?: 0

                            AbilityActionButton(
                                ability = ability,
                                cooldown = cooldown,
                                canAfford = canAfford,
                                onClick = {
                                    onAbilityClick(ability)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// INSPECTION MODAL
// =============================================================================

/**
 * Modal for inspecting/looking at things in the room.
 * Shows a scrollable list of creatures, items, players, and the location itself.
 * Also provides Search, Hide, and Fish actions.
 */
@Composable
fun InspectionModal(
    creatures: List<CreatureDto>,
    items: List<ItemDto>,
    players: List<UserDto>,
    location: LocationDto,
    isFishingLocation: Boolean,
    onInspectCreature: (CreatureDto) -> Unit,
    onInspectItem: (ItemDto) -> Unit,
    onInspectPlayer: (UserDto) -> Unit,
    onInspectLocation: () -> Unit,
    onSearch: () -> Unit,
    onHide: () -> Unit,
    onFish: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .heightIn(max = 450.dp)
                .clickable(enabled = false) { /* Prevent click-through */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Look Around",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Action buttons (Search, Hide, and optionally Fish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search button
                    Button(
                        onClick = {
                            onSearch()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF795548)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search")
                    }

                    // Hide Item button
                    Button(
                        onClick = {
                            onHide()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF607D8B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hide Item")
                    }
                }

                // Fish button (only at fishing locations)
                if (isFishingLocation) {
                    Button(
                        onClick = {
                            onFish()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E88E5)  // Blue water color
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("\uD83C\uDFA3")  // Fishing pole emoji
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fish")
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                // Location section
                InspectionItem(
                    icon = Icons.Filled.Person, // Location icon
                    name = location.name,
                    subtitle = "This place",
                    color = Color(0xFF9C27B0),
                    onClick = onInspectLocation
                )

                // Creatures section
                if (creatures.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Text(
                        text = "Creatures",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    creatures.forEach { creature ->
                        InspectionItem(
                            icon = if (creature.isAggressive) Icons.Filled.Dangerous else Icons.Filled.Person,
                            name = creature.name,
                            subtitle = "Level ${creature.level}" + if (creature.isAggressive) " - Hostile" else "",
                            color = if (creature.isAggressive) Color(0xFFE53935) else Color(0xFFFF9800),
                            onClick = { onInspectCreature(creature) }
                        )
                    }
                }

                // Items section
                if (items.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    items.forEach { item ->
                        InspectionItem(
                            icon = Icons.Filled.Star,
                            name = item.name,
                            subtitle = item.desc.take(40) + if (item.desc.length > 40) "..." else "",
                            color = Color(0xFF4CAF50),
                            onClick = { onInspectItem(item) }
                        )
                    }
                }

                // Players section
                if (players.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Text(
                        text = "Other Players",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    players.forEach { player ->
                        InspectionItem(
                            icon = Icons.Filled.Person,
                            name = player.name,
                            subtitle = "Level ${player.level}",
                            color = Color(0xFF2196F3),
                            onClick = { onInspectPlayer(player) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual item in the inspection modal.
 */
@Composable
fun InspectionItem(
    icon: ImageVector,
    name: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            Icons.Filled.Search,
            contentDescription = "Look",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// =============================================================================
// HIDE ITEM MODAL
// =============================================================================

/**
 * Modal for selecting an item to hide at the current location.
 * Hidden items require searching to find.
 */
@Composable
fun HideItemModal(
    items: List<ItemDto>,
    onSelectItem: (ItemDto) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .heightIn(max = 400.dp)
                .clickable(enabled = false) { /* Prevent click-through */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color(0xFF607D8B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hide Item",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Select an item to hide. Others will need to search to find it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                // Item list
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF607D8B).copy(alpha = 0.1f))
                            .clickable { onSelectItem(item) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            if (item.desc.isNotEmpty()) {
                                Text(
                                    text = item.desc.take(50) + if (item.desc.length > 50) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Hide",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Cancel button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}

// =============================================================================
// GIVE ITEM MODAL
// =============================================================================

/**
 * Modal for selecting an item to give to another player.
 */
@Composable
fun GiveItemModal(
    playerInventory: List<ItemDto>,
    equippedItemIds: List<String>,
    receiverName: String,
    onGiveItem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .heightIn(max = 400.dp)
                .clickable(enabled = false) { /* Prevent click-through */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Give Item to $receiverName",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filter out equipped items - can't give those
                val giveableItems = playerInventory.filter { it.id !in equippedItemIds }

                if (giveableItems.isEmpty()) {
                    Text(
                        text = "No items to give",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(giveableItems) { item ->
                            GiveItemRow(
                                item = item,
                                onClick = {
                                    onGiveItem(item.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Row displaying an item that can be given to another player.
 */
@Composable
fun GiveItemRow(
    item: ItemDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Item icon based on equipment type
        val itemEmoji = when (item.equipmentType) {
            "weapon" -> "\u2694\uFE0F"  // Crossed swords
            "armor" -> "\uD83D\uDEE1\uFE0F"  // Shield
            "accessory" -> "\uD83D\uDC8D"  // Ring
            else -> "\uD83D\uDCE6"  // Package (generic item)
        }
        Text(
            text = itemEmoji,
            fontSize = 24.sp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (item.desc.isNotEmpty()) {
                Text(
                    text = item.desc,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Arrow indicator
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Give",
            tint = Color(0xFF9C27B0), // Purple
            modifier = Modifier.size(20.dp)
        )
    }
}

// =============================================================================
// DESCRIPTION POPUP
// =============================================================================

@Composable
fun DescriptionPopup(
    creature: CreatureDto?,
    item: ItemDto?,
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    val detailName = creature?.name ?: item?.name ?: ""
    val description = creature?.desc ?: item?.desc ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFFF9800), RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Enlarged image
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = "${AppConfig.api.baseUrl}${imageUrl}",
                        contentDescription = detailName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            Text(
                text = detailName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Creature stats
            if (creature != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("Level", "${creature.level}", Color.White)
                    StatColumn("HP", "${creature.maxHp}", Color(0xFFFF5252))
                    StatColumn("Damage", "${creature.baseDamage}", Color(0xFFFFAB40))
                    StatColumn("XP", "${creature.experienceValue}", Color(0xFF69F0AE))
                }

                if (creature.isAggressive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Dangerous,
                            contentDescription = "Aggressive",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Aggressive",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Item stats
            if (item != null) {
                Spacer(modifier = Modifier.height(12.dp))

                // Equipment type badge
                if (item.equipmentType != null) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2196F3).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.equipmentType.replaceFirstChar { it.uppercase() },
                            color = Color(0xFF2196F3),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (item.equipmentSlot != null) {
                            Text(
                                text = " \u2022 ${item.equipmentSlot.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                                color = Color(0xFF2196F3).copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Stat bonuses
                val statBonuses = item.statBonuses
                if (statBonuses != null && (statBonuses.attack != 0 || statBonuses.defense != 0 || statBonuses.maxHp != 0)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (statBonuses.attack != 0) {
                            StatColumn(
                                "Attack",
                                "${if (statBonuses.attack > 0) "+" else ""}${statBonuses.attack}",
                                if (statBonuses.attack > 0) Color(0xFF69F0AE) else Color(0xFFFF5252)
                            )
                        }
                        if (statBonuses.defense != 0) {
                            StatColumn(
                                "Defense",
                                "${if (statBonuses.defense > 0) "+" else ""}${statBonuses.defense}",
                                if (statBonuses.defense > 0) Color(0xFF448AFF) else Color(0xFFFF5252)
                            )
                        }
                        if (statBonuses.maxHp != 0) {
                            StatColumn(
                                "Max HP",
                                "${if (statBonuses.maxHp > 0) "+" else ""}${statBonuses.maxHp}",
                                if (statBonuses.maxHp > 0) Color(0xFFFF5252) else Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Gold value
                if (item.value > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${item.value}",
                            color = Color(0xFFFFD700),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "gold",
                            color = Color(0xFFFFD700).copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap anywhere to close",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(
            value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// =============================================================================
// LOCATION DETAIL POPUP
// =============================================================================

/**
 * Full screen popup showing location details when tapping on the location thumbnail.
 * Similar to the creature description popup - black background with larger image and description.
 */
@Composable
fun LocationDetailPopup(
    location: LocationDto,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF4A90A4), RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Enlarged location image
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(2.dp, Color(0xFF4A90A4), CircleShape)
            ) {
                if (location.imageUrl != null) {
                    AsyncImage(
                        model = "${AppConfig.api.baseUrl}${location.imageUrl}",
                        contentDescription = location.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback - terrain color based on description
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(getTerrainColor(location.desc, location.name)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = location.name.take(2).uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location name
            Text(
                text = location.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Coordinates
            val gridX = location.gridX ?: 0
            val gridY = location.gridY ?: 0
            Text(
                text = "($gridX, $gridY)",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            if (location.desc.isNotEmpty()) {
                Text(
                    text = location.desc,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Exits info
            if (location.exits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Exits",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    location.exits.forEach { exit ->
                        val directionColor = when (exit.direction) {
                            ExitDirection.ENTER -> Color(0xFF9C27B0)  // Purple for portals
                            ExitDirection.UP, ExitDirection.DOWN -> Color(0xFF4CAF50)  // Green for vertical
                            else -> Color(0xFF1976D2)  // Blue for normal directions
                        }
                        Box(
                            modifier = Modifier
                                .background(directionColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = exit.direction.name,
                                color = directionColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap anywhere to close",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

// =============================================================================
// RIFT SELECTION OVERLAY
// =============================================================================

@Composable
fun RiftSelectionOverlay(
    mode: String,
    unconnectedAreas: List<UnconnectedAreaDto>,
    sealableRifts: List<SealableRiftDto>,
    onAreaSelected: (UnconnectedAreaDto) -> Unit,
    onRiftSelected: (SealableRiftDto) -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(16.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E2E),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = if (mode == "open") "Open Rift Portal" else "Seal Rift Portal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (mode == "open") Color(0xFF9D4EDD) else Color(0xFFE57373),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (mode == "open")
                        "Select a realm to open a permanent portal to:"
                    else
                        "Select a rift to seal:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // List of options
                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mode == "open") {
                        unconnectedAreas.forEach { area ->
                            Surface(
                                onClick = { onAreaSelected(area) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2D2D3D)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = area.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFBB86FC)
                                    )
                                    Text(
                                        text = "${area.locationCount} locations • Entry: ${area.entryLocationName ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        sealableRifts.forEach { rift ->
                            Surface(
                                onClick = { onRiftSelected(rift) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2D2D3D)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Portal to ${rift.targetAreaName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFE57373)
                                    )
                                    Text(
                                        text = "Leads to: ${rift.targetLocationName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Cancel button
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// =============================================================================
// TRAINER MODAL
// =============================================================================

/**
 * Modal for learning abilities from a trainer NPC.
 * Shows available abilities, their costs, and learn status.
 */
@Composable
fun TrainerModal(
    trainerInfo: TrainerInfoResponse?,
    isLoading: Boolean,
    playerGold: Int,
    onLearnAbility: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(16.dp))
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { /* Consume taps to prevent dismissal */ }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "Trainer",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trainerInfo?.trainerName ?: "Trainer",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Gold display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$playerGold",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "gold",
                        color = Color(0xFFFFD700).copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                }
            } else if (trainerInfo == null || trainerInfo.abilities.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This trainer has no abilities to teach.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Ability list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trainerInfo.abilities) { abilityInfo ->
                        TrainerAbilityRow(
                            abilityInfo = abilityInfo,
                            playerGold = playerGold,
                            onLearn = { onLearnAbility(abilityInfo.ability.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close button
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

/**
 * A single ability row in the trainer modal.
 */
@Composable
private fun TrainerAbilityRow(
    abilityInfo: TrainerAbilityInfo,
    playerGold: Int,
    onLearn: () -> Unit
) {
    val ability = abilityInfo.ability
    val canAfford = playerGold >= abilityInfo.goldCost
    val canLearn = !abilityInfo.alreadyLearned && abilityInfo.meetsLevelRequirement && canAfford

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    abilityInfo.alreadyLearned -> Color(0xFF2E7D32).copy(alpha = 0.3f)
                    !abilityInfo.meetsLevelRequirement -> Color.Gray.copy(alpha = 0.2f)
                    else -> Color(0xFF2A2A2A)
                },
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                when {
                    abilityInfo.alreadyLearned -> Color(0xFF4CAF50)
                    !abilityInfo.meetsLevelRequirement -> Color.Gray.copy(alpha = 0.5f)
                    else -> Color(0xFF444444)
                },
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ability icon placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    when (ability.abilityType) {
                        "spell" -> Color(0xFF2196F3).copy(alpha = 0.3f)
                        "combat" -> Color(0xFFFF5722).copy(alpha = 0.3f)
                        else -> Color.Gray.copy(alpha = 0.3f)
                    },
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ability.name.take(2).uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Ability info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ability.name,
                    color = if (abilityInfo.alreadyLearned) Color(0xFF4CAF50) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (abilityInfo.alreadyLearned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Learned",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = ability.description,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Level requirement
                Text(
                    text = "Lv ${ability.minLevel}",
                    color = if (abilityInfo.meetsLevelRequirement) Color(0xFF69F0AE) else Color(0xFFFF5252),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Type badge
                Text(
                    text = ability.abilityType.replaceFirstChar { it.uppercase() },
                    color = when (ability.abilityType) {
                        "spell" -> Color(0xFF2196F3)
                        "combat" -> Color(0xFFFF5722)
                        else -> Color.Gray
                    },
                    fontSize = 10.sp
                )
                // Resource cost
                if (ability.manaCost > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${ability.manaCost} mana",
                        color = Color(0xFF2196F3).copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
                if (ability.staminaCost > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${ability.staminaCost} stam",
                        color = Color(0xFFFFAB40).copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Learn button or status
        if (abilityInfo.alreadyLearned) {
            Text(
                text = "Learned",
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (!abilityInfo.meetsLevelRequirement) {
            Text(
                text = "Lv ${ability.minLevel}",
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Button(
                onClick = onLearn,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${abilityInfo.goldCost}g",
                    color = if (canAfford) Color.White else Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
