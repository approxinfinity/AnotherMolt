package com.ez2bg.anotherthread.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CharacterClassDto
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.state.AdventureStateHolder
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.UserStateHolder
import com.ez2bg.anotherthread.ui.components.AbilityIconButton
import com.ez2bg.anotherthread.ui.components.BlindOverlay
import com.ez2bg.anotherthread.ui.components.DisorientIndicator
import com.ez2bg.anotherthread.ui.components.EventLog
import com.ez2bg.anotherthread.ui.components.TargetSelectionOverlay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Adventure mode screen - completely separate from Create mode.
 * Full-screen immersive experience with solid black background.
 */
@Composable
fun AdventureScreen(
    onSwitchToCreate: () -> Unit,
    onViewLocationDetails: (LocationDto) -> Unit
) {
    // Collect state from state holders
    val currentUser by UserStateHolder.currentUser.collectAsState()
    val currentLocation by AdventureStateHolder.currentLocation.collectAsState()
    val creaturesHere by AdventureStateHolder.creaturesHere.collectAsState()
    val itemsHere by AdventureStateHolder.itemsHere.collectAsState()
    val availableExits = AdventureStateHolder.availableExits
    val allLocations by AdventureStateHolder.allLocations.collectAsState()

    // Combat state
    val isInCombat = CombatStateHolder.isInCombat
    val cooldowns by CombatStateHolder.cooldowns.collectAsState()
    val queuedAbilityId by CombatStateHolder.queuedAbilityId.collectAsState()
    val isBlinded by CombatStateHolder.isBlinded.collectAsState()
    val blindRounds by CombatStateHolder.blindRounds.collectAsState()
    val isDisoriented by CombatStateHolder.isDisoriented.collectAsState()
    val disorientRounds by CombatStateHolder.disorientRounds.collectAsState()
    val eventLog by CombatStateHolder.eventLog.collectAsState()

    // Local UI state
    var selectedCreature by remember { mutableStateOf<CreatureDto?>(null) }
    var selectedItem by remember { mutableStateOf<ItemDto?>(null) }
    var pendingAbility by remember { mutableStateOf<AbilityDto?>(null) }
    var playerAbilities by remember { mutableStateOf<List<AbilityDto>>(emptyList()) }
    var playerCharacterClass by remember { mutableStateOf<CharacterClassDto?>(null) }

    val isDetailViewVisible = selectedCreature != null || selectedItem != null
    val isRanger = playerCharacterClass?.name == "Ranger"

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Animation for detail slide
    val detailOffsetX by animateFloatAsState(
        targetValue = if (isDetailViewVisible) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "detailSlide"
    )

    // Initialize adventure state when entering
    LaunchedEffect(currentUser?.id) {
        val userId = currentUser?.id
        val locationId = currentUser?.currentLocationId
        if (userId != null) {
            AdventureStateHolder.initialize(userId, locationId)
            CombatStateHolder.connect(userId)
        }
    }

    // Fetch player abilities
    LaunchedEffect(currentUser?.characterClassId) {
        val classId = currentUser?.characterClassId
        if (classId != null) {
            ApiClient.getAbilitiesByClass(classId).onSuccess { abilities ->
                playerAbilities = abilities.filter { it.abilityType != "passive" }
                    .sortedBy { it.name.lowercase() }
            }
            ApiClient.getCharacterClass(classId).onSuccess { characterClass ->
                playerCharacterClass = characterClass
            }
        }
    }

    // Main Adventure UI
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            if (currentLocation != null) {
                val location = currentLocation!!

                // Center content: Location thumbnail with ability ring
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Disorient indicator below thumbnail
                    if (isDisoriented && disorientRounds > 0) {
                        DisorientIndicator(
                            roundsRemaining = disorientRounds,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 160.dp)
                        )
                    }

                    // Container for circular thumbnail + ability icons
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                if (isDisoriented) {
                                    rotationZ = 180f
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Ability icons ring
                        if (playerAbilities.isNotEmpty()) {
                            AbilityRing(
                                abilities = playerAbilities,
                                cooldowns = cooldowns,
                                queuedAbilityId = queuedAbilityId,
                                creaturesHere = creaturesHere,
                                currentUserId = currentUser?.id,
                                onAbilityClick = { ability ->
                                    scope.launch {
                                        handleAbilityClick(
                                            ability = ability,
                                            creaturesHere = creaturesHere,
                                            currentUserId = currentUser?.id,
                                            snackbarHostState = snackbarHostState,
                                            onNeedTargetSelection = { pendingAbility = it }
                                        )
                                    }
                                }
                            )
                        }

                        // Location thumbnail
                        LocationThumbnail(
                            location = location,
                            isBlinded = isBlinded,
                            blindRounds = blindRounds,
                            isRanger = isRanger,
                            allLocations = allLocations,
                            onClick = { onViewLocationDetails(location) }
                        )
                    }
                }

                // Top section: Location info panel (full width) with controls overlaid
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                ) {
                    // Location info panel - full width background
                    LocationInfoPanel(
                        location = location,
                        creaturesHere = creaturesHere,
                        itemsHere = itemsHere,
                        isBlinded = isBlinded,
                        onCreatureClick = { selectedCreature = it },
                        onItemClick = { selectedItem = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )

                    // Controls overlay (higher z-index) - top right
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Coordinates display
                        if (location.gridX != null && location.gridY != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "(${location.gridX}, ${location.gridY})",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Refresh button
                        IconButton(
                            onClick = { AdventureStateHolder.refreshCurrentLocation() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }

                        // Mode toggle
                        ModeToggle(
                            isCreateMode = false,
                            onToggle = onSwitchToCreate
                        )
                    }
                }

                // Navigation arrows for available exits
                NavigationArrows(
                    exits = availableExits,
                    isDisoriented = isDisoriented,
                    onNavigate = { direction ->
                        AdventureStateHolder.navigateDirection(direction)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom: Event log
                EventLog(
                    entries = eventLog.map { entry ->
                        com.ez2bg.anotherthread.ui.components.EventLogEntry(
                            id = entry.id.toString(),
                            message = entry.message,
                            type = when (entry.type) {
                                com.ez2bg.anotherthread.state.EventLogType.INFO -> com.ez2bg.anotherthread.ui.components.EventType.INFO
                                com.ez2bg.anotherthread.state.EventLogType.DAMAGE_DEALT,
                                com.ez2bg.anotherthread.state.EventLogType.DAMAGE_RECEIVED -> com.ez2bg.anotherthread.ui.components.EventType.DAMAGE
                                com.ez2bg.anotherthread.state.EventLogType.HEAL -> com.ez2bg.anotherthread.ui.components.EventType.HEALING
                                com.ez2bg.anotherthread.state.EventLogType.BUFF -> com.ez2bg.anotherthread.ui.components.EventType.BUFF
                                com.ez2bg.anotherthread.state.EventLogType.DEBUFF -> com.ez2bg.anotherthread.ui.components.EventType.DEBUFF
                                com.ez2bg.anotherthread.state.EventLogType.COMBAT_START,
                                com.ez2bg.anotherthread.state.EventLogType.COMBAT_END -> com.ez2bg.anotherthread.ui.components.EventType.COMBAT
                                com.ez2bg.anotherthread.state.EventLogType.NAVIGATION -> com.ez2bg.anotherthread.ui.components.EventType.MOVEMENT
                                com.ez2bg.anotherthread.state.EventLogType.LOOT -> com.ez2bg.anotherthread.ui.components.EventType.LOOT
                                com.ez2bg.anotherthread.state.EventLogType.ERROR -> com.ez2bg.anotherthread.ui.components.EventType.INFO
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Target selection overlay
                if (pendingAbility != null) {
                    TargetSelectionOverlay(
                        ability = pendingAbility!!,
                        targets = creaturesHere.map { creature ->
                            com.ez2bg.anotherthread.ui.components.CombatTarget(
                                id = creature.id,
                                name = creature.name,
                                currentHp = 100, // Would need actual HP from combat state
                                maxHp = 100,
                                isPlayer = false
                            )
                        },
                        onTargetSelected = { targetId ->
                            scope.launch {
                                CombatStateHolder.useAbility(pendingAbility!!.id, targetId)
                                snackbarHostState.showSnackbar(
                                    "Casting ${pendingAbility!!.name}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            pendingAbility = null
                        },
                        onCancel = { pendingAbility = null }
                    )
                }

                // Blind overlay (full screen)
                if (isBlinded && blindRounds > 0) {
                    com.ez2bg.anotherthread.ui.components.BlindOverlayFullScreen(
                        roundsRemaining = blindRounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Loading or no location
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Ring of ability icons around the center thumbnail.
 */
@Composable
private fun AbilityRing(
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int>,
    queuedAbilityId: String?,
    creaturesHere: List<CreatureDto>,
    currentUserId: String?,
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

/**
 * Circular location thumbnail.
 */
@Composable
private fun LocationThumbnail(
    location: LocationDto,
    isBlinded: Boolean,
    blindRounds: Int,
    isRanger: Boolean,
    allLocations: List<LocationDto>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.8f))
            .border(2.dp, if (isRanger) Color(0xFF4CAF50) else Color(0xFF4A4A4A), CircleShape)
            .clickable(onClick = onClick)
    ) {
        if (isBlinded && blindRounds > 0) {
            BlindOverlay(
                roundsRemaining = blindRounds,
                modifier = Modifier.fillMaxSize()
            )
        } else if (location.imageUrl != null) {
            AsyncImage(
                model = "${AppConfig.api.baseUrl}${location.imageUrl}",
                contentDescription = location.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback - show location name
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = location.name.take(10),
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Location info panel showing name, creatures, and items.
 */
@Composable
private fun LocationInfoPanel(
    location: LocationDto,
    creaturesHere: List<CreatureDto>,
    itemsHere: List<ItemDto>,
    isBlinded: Boolean,
    onCreatureClick: (CreatureDto) -> Unit,
    onItemClick: (ItemDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Location name
        Text(
            text = location.name,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        // Creatures section
        if (creaturesHere.isNotEmpty()) {
            Text(
                text = "Others",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
            creaturesHere.forEach { creature ->
                Text(
                    text = if (isBlinded) "Something lurks..." else creature.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { onCreatureClick(creature) }
                )
            }
        }

        // Items section
        if (itemsHere.isNotEmpty()) {
            Text(
                text = "Items",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
            itemsHere.forEach { item ->
                Text(
                    text = if (isBlinded) "Something here..." else item.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { onItemClick(item) }
                )
            }
        }
    }
}

/**
 * Navigation arrows for moving between locations.
 */
@Composable
private fun NavigationArrows(
    exits: List<com.ez2bg.anotherthread.api.ExitDto>,
    isDisoriented: Boolean,
    onNavigate: (ExitDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        exits.forEach { exit ->
            val direction = exit.direction
            val alignment = when (direction) {
                ExitDirection.NORTH -> Alignment.TopCenter
                ExitDirection.SOUTH -> Alignment.BottomCenter
                ExitDirection.EAST -> Alignment.CenterEnd
                ExitDirection.WEST -> Alignment.CenterStart
                ExitDirection.NORTHEAST -> Alignment.TopEnd
                ExitDirection.NORTHWEST -> Alignment.TopStart
                ExitDirection.SOUTHEAST -> Alignment.BottomEnd
                ExitDirection.SOUTHWEST -> Alignment.BottomStart
                ExitDirection.ENTER -> Alignment.Center
                else -> Alignment.Center
            }

            // Arrow button
            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(24.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        if (!isDisoriented) {
                            onNavigate(direction)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val arrowChar = when (direction) {
                    ExitDirection.NORTH -> "\u2191" // ↑
                    ExitDirection.SOUTH -> "\u2193" // ↓
                    ExitDirection.EAST -> "\u2192" // →
                    ExitDirection.WEST -> "\u2190" // ←
                    ExitDirection.NORTHEAST -> "\u2197" // ↗
                    ExitDirection.NORTHWEST -> "\u2196" // ↖
                    ExitDirection.SOUTHEAST -> "\u2198" // ↘
                    ExitDirection.SOUTHWEST -> "\u2199" // ↙
                    ExitDirection.ENTER -> "\u21B5" // ↵
                    else -> "?"
                }
                Text(
                    text = arrowChar,
                    color = if (isDisoriented) Color.Gray else Color.White,
                    fontSize = 24.sp
                )
            }
        }
    }
}

/**
 * Mode toggle button.
 */
@Composable
private fun ModeToggle(
    isCreateMode: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isCreateMode) "create" else "adventure",
            color = Color.White,
            fontSize = 14.sp
        )

        // Toggle switch visual
        val trackColor = if (isCreateMode) Color(0xFF2E7D32).copy(alpha = 0.6f) else Color(0xFF9C27B0).copy(alpha = 0.6f)
        val thumbColor = if (isCreateMode) Color(0xFF4CAF50) else Color(0xFFBA68C8)

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

/**
 * Handle ability click with target selection logic.
 */
private suspend fun handleAbilityClick(
    ability: AbilityDto,
    creaturesHere: List<CreatureDto>,
    currentUserId: String?,
    snackbarHostState: SnackbarHostState,
    onNeedTargetSelection: (AbilityDto) -> Unit
) {
    if (!CombatStateHolder.isInCombat) {
        snackbarHostState.showSnackbar("Not in combat", duration = SnackbarDuration.Short)
        return
    }

    when (ability.targetType) {
        "self", "area", "all_enemies", "all_allies" -> {
            CombatStateHolder.useAbility(ability.id, null)
            snackbarHostState.showSnackbar("Casting: ${ability.name}", duration = SnackbarDuration.Short)
        }
        "single_enemy" -> {
            when {
                creaturesHere.size == 1 -> {
                    CombatStateHolder.useAbility(ability.id, creaturesHere.first().id)
                    snackbarHostState.showSnackbar(
                        "Casting ${ability.name} on ${creaturesHere.first().name}",
                        duration = SnackbarDuration.Short
                    )
                }
                creaturesHere.isEmpty() -> {
                    snackbarHostState.showSnackbar("No enemies to target", duration = SnackbarDuration.Short)
                }
                else -> {
                    onNeedTargetSelection(ability)
                }
            }
        }
        "single_ally" -> {
            CombatStateHolder.useAbility(ability.id, currentUserId)
            snackbarHostState.showSnackbar("Casting ${ability.name} on self", duration = SnackbarDuration.Short)
        }
        else -> {
            CombatStateHolder.useAbility(ability.id, null)
            snackbarHostState.showSnackbar("Casting: ${ability.name}", duration = SnackbarDuration.Short)
        }
    }
}
