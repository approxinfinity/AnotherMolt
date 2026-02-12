package com.ez2bg.anotherthread.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.ExitDto
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.PhasewalkDestinationDto
import com.ez2bg.anotherthread.api.ShopLayoutDirection
import com.ez2bg.anotherthread.api.UnconnectedAreaDto
import com.ez2bg.anotherthread.api.SealableRiftDto
import com.ez2bg.anotherthread.api.TrainerAbilityInfo
import com.ez2bg.anotherthread.api.TrainerInfoResponse
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.api.PuzzleDto
import com.ez2bg.anotherthread.api.PuzzleProgressResponse
import com.ez2bg.anotherthread.api.LeverStateDto
import com.ez2bg.anotherthread.api.PuzzleType
import com.ez2bg.anotherthread.api.ADMIN_FEATURE_ID
import com.ez2bg.anotherthread.api.FishingInfoDto
import com.ez2bg.anotherthread.api.LockpickInfoDto
import com.ez2bg.anotherthread.api.LockpickPathPointDto
import com.ez2bg.anotherthread.api.FishingMinigameStartDto
import com.ez2bg.anotherthread.api.DiplomacyResultDto
import com.ez2bg.anotherthread.ui.CreatureStateIcon
import com.ez2bg.anotherthread.ui.SwordIcon
import com.ez2bg.anotherthread.ui.getBlindItemDescription
import com.ez2bg.anotherthread.ui.getBlindPresenceDescription
import com.ez2bg.anotherthread.ui.components.AbilityIconButton
import com.ez2bg.anotherthread.ui.components.AbilityIconMapper
import com.ez2bg.anotherthread.ui.components.AbilityIconSmall
import com.ez2bg.anotherthread.ui.components.BlindOverlay
import com.ez2bg.anotherthread.ui.components.CombatTarget
import com.ez2bg.anotherthread.ui.components.ConnectionIndicator
import com.ez2bg.anotherthread.ui.components.DeathTransitionOverlay
import com.ez2bg.anotherthread.ui.components.DisorientIndicator
import com.ez2bg.anotherthread.ui.components.EventLog
import com.ez2bg.anotherthread.ui.components.MapSelectionOverlay
import com.ez2bg.anotherthread.ui.components.TargetSelectionOverlay
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.ui.admin.getTerrainColor
import com.ez2bg.anotherthread.ui.admin.UserProfileView
import com.ez2bg.anotherthread.state.UserStateHolder
import com.ez2bg.anotherthread.api.AsyncOperationRepository
import com.ez2bg.anotherthread.storage.AuthStorage
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Preview imports
import androidx.compose.ui.tooling.preview.Preview

/**
 * Adventure mode screen - completely separate from Create mode.
 * Full-screen immersive experience with solid black background.
 *
 * This composable handles all adventure mode UI including:
 * - Central location thumbnail with ability ring
 * - Navigation arrows
 * - Minimap in top-right
 * - Info panel with creatures/items
 * - Detail view for inspecting creatures/items
 * - Combat integration via CombatStateHolder
 */
@Composable
fun AdventureScreen(
    currentUser: UserDto?,
    onSwitchToCreate: () -> Unit,
    onViewLocationDetails: (LocationDto) -> Unit,
    ghostMode: Boolean = false,
    onGhostModeBack: (() -> Unit)? = null,
    onNavigateToAdminPanel: (() -> Unit)? = null  // Direct navigation to admin panel (skips create mode landing)
) {
    // Create ViewModel scoped to this composable
    // ViewModel now uses UserStateHolder internally for reactive user state
    val viewModel = remember(currentUser?.id) { AdventureViewModel() }

    // Collect UI state
    val uiState by viewModel.uiState.collectAsState()

    // Collect combat state (still from CombatStateHolder flows)
    val cooldowns by viewModel.cooldowns.collectAsState()
    val queuedAbilityId by viewModel.queuedAbilityId.collectAsState()
    val isBlinded by viewModel.isBlinded.collectAsState()
    val blindRounds by viewModel.blindRounds.collectAsState()
    val isDisoriented by viewModel.isDisoriented.collectAsState()
    val disorientRounds by viewModel.disorientRounds.collectAsState()
    val eventLogState by viewModel.eventLog.collectAsState()
    val playerCombatant by viewModel.playerCombatant.collectAsState()
    val combatants by viewModel.combatants.collectAsState()

    // Death animation state
    val isPlayingDeathAnimation by CombatStateHolder.isPlayingDeathAnimation.collectAsState()
    val respawnLocationName by CombatStateHolder.respawnLocationName.collectAsState()

    // Pending party invite state (inviterId, inviterName)
    val pendingPartyInvite by CombatStateHolder.pendingPartyInvite.collectAsState()

    // Reactive user state for mana/stamina display (updates on phasewalk, etc.)
    val reactiveUser by UserStateHolder.currentUser.collectAsState()
    // Use reactiveUser for live updates, fall back to passed-in currentUser
    val displayUser = reactiveUser ?: currentUser

    // Admin check for profile navigation
    val isAdmin = currentUser?.featureIds?.contains(ADMIN_FEATURE_ID) == true

    // Location detail popup state
    var showLocationDetailPopup by remember { mutableStateOf(false) }

    // Character sheet overlay state
    var showCharacterSheet by remember { mutableStateOf(false) }

    // Reload abilities when character sheet closes (in case user changed visible abilities)
    LaunchedEffect(showCharacterSheet) {
        if (!showCharacterSheet) {
            viewModel.loadPlayerAbilities()
        }
    }

    // Also reload abilities when visibleAbilityIds changes in the reactive user
    LaunchedEffect(reactiveUser?.visibleAbilityIds) {
        viewModel.loadPlayerAbilities()
    }

    // Basic attack target selection modal state
    var showBasicAttackTargetModal by remember { mutableStateOf(false) }

    // Give up confirmation modal state (voluntary death when downed)
    var showGiveUpConfirmation by remember { mutableStateOf(false) }

    // Party abilities dropdown state
    var showPartyAbilitiesDropdown by remember { mutableStateOf(false) }

    // Inspection modal state
    var showInspectionModal by remember { mutableStateOf(false) }

    // Custom icon mappings
    var iconMappings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            com.ez2bg.anotherthread.api.ApiClient.getIconMappings(userId).onSuccess { mappings ->
                iconMappings = mappings.associate { it.abilityId to it.iconName }
            }
        }
    }

    // Convert event log entries to UI format
    val eventLogEntries = remember(eventLogState) {
        convertEventLogEntries(eventLogState)
    }

    // Poll for user updates (HP/mana/stamina regen) when not in combat
    // This syncs client with server's global tick regen
    val isInCombat = combatants.isNotEmpty()
    LaunchedEffect(isInCombat, currentUser?.id) {
        if (!isInCombat && currentUser != null) {
            while (true) {
                kotlinx.coroutines.delay(3000) // Match server tick interval
                UserStateHolder.refreshUser()
            }
        }
    }

    // Check if class generation is in progress when entering ghost mode
    // Note: Generation status is now tracked in-memory on both client and server.
    // If the app restarts during generation, the user will need to re-initiate.
    LaunchedEffect(ghostMode, currentUser?.id) {
        if (ghostMode && currentUser != null) {
            if (AsyncOperationRepository.isClassGenerating(currentUser.id) && currentUser.characterClassId == null) {
                // Already tracking this generation - nothing to do, it will continue
            }
        }
    }

    // Snackbar handling
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar when message is set
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.consumeSnackbarMessage()
        }
    }

    // Animation for detail view slide
    val detailOffsetX by animateFloatAsState(
        targetValue = if (uiState.isDetailViewVisible) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "detailSlide"
    )

    // Swipe gesture state
    var swipeTotalX by remember { mutableStateOf(0f) }
    var swipeTotalY by remember { mutableStateOf(0f) }

    // Helper to determine direction from swipe angle
    fun getSwipeDirection(dx: Float, dy: Float): ExitDirection? {
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < 50f) return null  // Minimum swipe distance

        // Calculate angle in degrees (0 = right, 90 = down, -90 = up, 180/-180 = left)
        val angle = atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI

        // Map to 8 directions with 45-degree sectors
        return when {
            angle >= -22.5 && angle < 22.5 -> ExitDirection.EAST
            angle >= 22.5 && angle < 67.5 -> ExitDirection.SOUTHEAST
            angle >= 67.5 && angle < 112.5 -> ExitDirection.SOUTH
            angle >= 112.5 && angle < 157.5 -> ExitDirection.SOUTHWEST
            angle >= 157.5 || angle < -157.5 -> ExitDirection.WEST
            angle >= -157.5 && angle < -112.5 -> ExitDirection.NORTHWEST
            angle >= -112.5 && angle < -67.5 -> ExitDirection.NORTH
            angle >= -67.5 && angle < -22.5 -> ExitDirection.NORTHEAST
            else -> null
        }
    }

    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(uiState.currentLocation?.id, ghostMode) {
                if (!ghostMode && uiState.currentLocation != null) {
                    detectDragGestures(
                        onDragStart = {
                            swipeTotalX = 0f
                            swipeTotalY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            swipeTotalX += dragAmount.x
                            swipeTotalY += dragAmount.y
                        },
                        onDragEnd = {
                            // Block swipe navigation when over-encumbered
                            if (!uiState.isOverEncumbered) {
                                val direction = getSwipeDirection(swipeTotalX, swipeTotalY)
                                if (direction != null) {
                                    // Find exit in that direction
                                    val exit = uiState.currentLocation?.exits?.find { it.direction == direction }
                                    if (exit != null) {
                                        viewModel.navigateToExit(exit)
                                    }
                                }
                            }
                            swipeTotalX = 0f
                            swipeTotalY = 0f
                        },
                        onDragCancel = {
                            swipeTotalX = 0f
                            swipeTotalY = 0f
                        }
                    )
                }
            }
    ) {
        // Show loading spinner until data is loaded AND server location is synced
        // This prevents flashing the fallback location before the user's actual location is confirmed
        if (uiState.isLoading || !uiState.isLocationSynced) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (uiState.currentLocation != null) {
            val currentLocation = uiState.currentLocation!!

            // === BACKGROUND: Location image fills the entire screen ===
            if (!isBlinded && currentLocation.imageUrl != null) {
                AsyncImage(
                    model = "${AppConfig.api.baseUrl}${currentLocation.imageUrl}",
                    contentDescription = currentLocation.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.6f },  // Semi-transparent so UI is readable
                    contentScale = ContentScale.Crop
                )
                // Dark gradient overlay for better text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }

            // === MAIN CONTENT COLUMN: Proper vertical layout ===
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // === TOP SECTION: Location info panel or Shop panel ===
                // In shops/inns, this section expands to fill more space since minimap is hidden
                val isShopOrInnLocal = uiState.isShopLocation || uiState.isInnLocation
                if (!uiState.isDetailViewVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isShopOrInnLocal && !ghostMode) {
                                    // Shops take more vertical space since minimap is hidden
                                    Modifier.weight(1f)
                                } else {
                                    // Normal locations have constrained height
                                    Modifier.heightIn(max = 280.dp)
                                }
                            )
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        if (isShopOrInnLocal && !ghostMode) {
                            ShopPanel(
                                location = currentLocation,
                                shopItems = uiState.shopItems,
                                playerGold = uiState.playerGold,
                                playerCharisma = currentUser?.charisma ?: 10,
                                isInn = uiState.isInnLocation,
                                isGeneralStore = uiState.isGeneralStore,
                                sellableItems = uiState.sellableItems,
                                innCost = 25,
                                onBuyItem = { viewModel.buyItem(it.id) },
                                onRest = { viewModel.restAtInn() },
                                onSellItem = { viewModel.sellItem(it) },
                                onLocationNameClick = { showLocationDetailPopup = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        } else {
                            LocationInfoPanel(
                                location = currentLocation,
                                creaturesHere = uiState.creaturesHere,
                                playersHere = uiState.playersHere,
                                itemsHere = uiState.itemsHere,
                                creatureStates = uiState.creatureStates,
                                isBlinded = isBlinded,
                                puzzlesHere = uiState.puzzlesAtLocation,
                                onCreatureClick = { if (!ghostMode) viewModel.selectCreature(it) },
                                onItemClick = { if (!ghostMode) viewModel.pickupItem(it) },
                                onPlayerClick = { if (!ghostMode && !isBlinded) viewModel.selectPlayer(it) },
                                onLocationNameClick = { showLocationDetailPopup = true },
                                onPuzzleClick = { if (!ghostMode) viewModel.openPuzzleModal(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }

                        // Player avatar, tick indicator, and party abilities dropdown in top-right corner
                        if (currentUser != null && !ghostMode) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                // Tick indicator (pizza timer) - ghostly/subtle appearance
                                TickIndicator(
                                    tickDurationMs = 3000,
                                    size = 18.dp,
                                    fillColor = Color.White.copy(alpha = 0.25f),
                                    backgroundColor = Color.Black.copy(alpha = 0.3f),
                                    borderColor = Color.White.copy(alpha = 0.15f)
                                )

                                // Party icon - show when in a party (for party abilities and Leave Party)
                                val partyAbilities = uiState.playerAbilities.filter {
                                    it.abilityType != "passive" && it.abilityType != "navigation" &&
                                    (it.targetType == "all_allies" || it.targetType == "single_ally")
                                }
                                val isInParty = displayUser?.partyLeaderId != null || displayUser?.isPartyLeader == true
                                if (isInParty) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (showPartyAbilitiesDropdown) Color(0xFF4CAF50).copy(alpha = 0.9f)
                                                else Color(0xFF2196F3).copy(alpha = 0.7f)
                                            )
                                            .border(
                                                1.dp,
                                                if (showPartyAbilitiesDropdown) Color(0xFF81C784)
                                                else Color.White.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                            .clickable { showPartyAbilitiesDropdown = !showPartyAbilitiesDropdown },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Groups,
                                            contentDescription = "Party abilities",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Player avatar
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .clickable { showCharacterSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentUser.imageUrl != null) {
                                        AsyncImage(
                                            model = "${AppConfig.api.baseUrl}${currentUser.imageUrl}",
                                            contentDescription = "Player avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = "Player",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                }

                                // Party abilities dropdown - animated (show if in party as leader or member)
                                val partyAbilitiesForDropdown = uiState.playerAbilities.filter {
                                    it.abilityType != "passive" && it.abilityType != "navigation" &&
                                    (it.targetType == "all_allies" || it.targetType == "single_ally")
                                }
                                val isInPartyForDropdown = displayUser?.partyLeaderId != null || displayUser?.isPartyLeader == true
                                val isPartyLeaderForDropdown = displayUser?.isPartyLeader == true
                                AnimatedVisibility(
                                    visible = showPartyAbilitiesDropdown && isInPartyForDropdown,
                                    enter = expandVertically(
                                        animationSpec = tween(200),
                                        expandFrom = Alignment.Top
                                    ) + fadeIn(animationSpec = tween(200)),
                                    exit = shrinkVertically(
                                        animationSpec = tween(150),
                                        shrinkTowards = Alignment.Top
                                    ) + fadeOut(animationSpec = tween(150))
                                ) {
                                    PartyAbilitiesDropdown(
                                        abilities = partyAbilitiesForDropdown,
                                        cooldowns = cooldowns,
                                        queuedAbilityId = queuedAbilityId,
                                        currentMana = playerCombatant?.currentMana ?: displayUser?.currentMana ?: 0,
                                        currentStamina = playerCombatant?.currentStamina ?: displayUser?.currentStamina ?: 0,
                                        isPartyLeader = isPartyLeaderForDropdown,
                                        onAbilityClick = { ability ->
                                            viewModel.handleAbilityClick(ability)
                                            showPartyAbilitiesDropdown = false
                                        },
                                        onLeaveParty = {
                                            viewModel.leaveParty()
                                            showPartyAbilitiesDropdown = false
                                        },
                                        onDisbandParty = {
                                            viewModel.disbandParty()
                                            showPartyAbilitiesDropdown = false
                                        },
                                        iconMappings = iconMappings,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // === CENTER SECTION: Minimap with directional ring (takes remaining space) ===
                // In shops/inns, this section is minimal (just shows the back FAB)
                // Outside shops, this takes remaining space with weight(1f)
                val enterExits = currentLocation.exits.filter { it.direction == ExitDirection.ENTER }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isShopOrInnLocal && !ghostMode) {
                                // In shops, just enough height for the FAB
                                Modifier.height(64.dp)
                            } else {
                                // Outside shops, take remaining space
                                Modifier.weight(1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Only show minimap and direction ring outside of shops/inns
                    if (!isShopOrInnLocal) {
                        // Disorient indicator below minimap
                        if (isDisoriented && disorientRounds > 0) {
                            DisorientIndicator(
                                roundsRemaining = disorientRounds,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = 120.dp)
                            )
                        }

                        // Container for minimap + directionals (rotates when disoriented)
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    if (isDisoriented) {
                                        rotationZ = 180f
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Directional navigation ring (around minimap)
                            // DirectionalRing needs all locations to resolve cross-area exits (e.g. ENTER)
                            // Phasewalk is enabled when player has 2+ mana and is not in combat
                            val playerMana = playerCombatant?.currentMana ?: displayUser?.currentMana ?: 0
                            val isInCombat = combatants.isNotEmpty()
                            val canPhasewalk = playerMana >= 2 && !isInCombat

                            DirectionalRing(
                                exits = currentLocation.exits,
                                locations = uiState.locations,
                                phasewalkDestinations = uiState.phasewalkDestinations,
                                phasewalkEnabled = canPhasewalk && !uiState.isOverEncumbered,
                                navigationDisabled = uiState.isOverEncumbered,
                                onNavigate = { viewModel.navigateToExit(it) },
                                onPhasewalk = { direction -> viewModel.phasewalk(direction) }
                            )

                            // Filter to only show locations in the same area that the user has visited (fog-of-war)
                            val currentAreaId = currentLocation.areaId
                            val visitedLocationIds = displayUser?.visitedLocationIds ?: emptyList()
                            val areaLocations = remember(uiState.locations, currentAreaId, visitedLocationIds, currentLocation.id) {
                                uiState.locations.filter { location ->
                                    location.areaId == currentAreaId &&
                                    (location.id in visitedLocationIds || location.id == currentLocation.id)
                                }
                            }

                            // Get unvisited locations that are directly connected from current location (show as "?")
                            val unvisitedConnectedLocations = remember(uiState.locations, currentLocation.exits, visitedLocationIds) {
                                val connectedIds = currentLocation.exits.map { it.locationId }.toSet()
                                uiState.locations.filter { location ->
                                    location.id in connectedIds &&
                                    location.id !in visitedLocationIds &&
                                    location.id != currentLocation.id
                                }
                            }

                            // Centered minimap (replaces location thumbnail)
                            CenterMinimap(
                                locations = areaLocations,
                                currentLocation = currentLocation,
                                unvisitedConnectedLocations = unvisitedConnectedLocations,
                                isBlinded = isBlinded,
                                blindRounds = blindRounds,
                                onClick = { showLocationDetailPopup = true }
                            )
                        }
                    }

                    // Floating ENTER/back FAB - overlays bottom-right of this section
                    // In shops: shows green back arrow. Outside: shows green ENTER doors.
                    if (!ghostMode && (isShopOrInnLocal || enterExits.isNotEmpty())) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isShopOrInnLocal) {
                                // Shop/Inn: show green back arrow
                                FloatingActionButton(
                                    onClick = {
                                        currentLocation.exits.firstOrNull()?.let { exit ->
                                            viewModel.navigateToExit(exit)
                                        }
                                    },
                                    containerColor = Color(0xFF4CAF50), // Green
                                    contentColor = Color.White,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                                        contentDescription = "Leave shop",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                // Normal location with ENTER exits
                                enterExits.forEach { exit ->
                                    FloatingActionButton(
                                        onClick = { viewModel.navigateToExit(exit) },
                                        containerColor = Color(0xFF4CAF50), // Green
                                        contentColor = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MeetingRoom,
                                            contentDescription = "Enter",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // === BOTTOM SECTION: Resource bars, abilities, mode toggle, event log ===
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // Player resource bars (HP/MP/SP) and gold
                    if (!ghostMode) {
                        PlayerResourceBar(
                            currentHp = playerCombatant?.currentHp ?: displayUser?.currentHp ?: 0,
                            maxHp = playerCombatant?.maxHp ?: displayUser?.maxHp ?: 0,
                            currentMana = playerCombatant?.currentMana ?: displayUser?.currentMana ?: 0,
                            maxMana = playerCombatant?.maxMana ?: displayUser?.maxMana ?: 0,
                            currentStamina = playerCombatant?.currentStamina ?: displayUser?.currentStamina ?: 0,
                            maxStamina = playerCombatant?.maxStamina ?: displayUser?.maxStamina ?: 0,
                            gold = uiState.playerGold,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Abilities row - show even without class abilities (for basic attack)
                    if (!ghostMode) {
                        // Check if user has a weapon equipped
                        val hasWeaponEquipped = displayUser?.equippedItemIds?.any { equippedId ->
                            uiState.allItems.find { it.id == equippedId }?.equipmentType == "weapon"
                        } ?: false

                        // Check if player is downed (HP <= 0)
                        val isPlayerDowned = (playerCombatant?.currentHp ?: displayUser?.currentHp ?: 1) <= 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Skull button - only visible when downed
                            if (isPlayerDowned) {
                                GiveUpButton(
                                    onClick = { showGiveUpConfirmation = true },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            // Show loading indicator while abilities are being loaded
                            if (uiState.abilitiesLoading) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White.copy(alpha = 0.7f),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading abilities...",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else {
                                // Filter abilities for the action bar:
                                // - Enemy-targeting abilities (single_enemy, all_enemies, area) go to creature modal
                                // - Party abilities (all_allies, single_ally) go to party dropdown when in party
                                // - Action bar shows: self + single_ally (when not in party) + all_allies (when not in party)
                                val isInPartyForAbilityBar = displayUser?.partyLeaderId != null
                                val abilitiesForBar = uiState.playerAbilities.filter { ability ->
                                    // Always exclude enemy-targeting abilities from action bar
                                    ability.targetType != "single_enemy" &&
                                    ability.targetType != "all_enemies" &&
                                    ability.targetType != "area" &&
                                    // Exclude party abilities when in a party (they're in the dropdown)
                                    !(isInPartyForAbilityBar && (ability.targetType == "all_allies" || ability.targetType == "single_ally"))
                                }

                                AbilityRow(
                                    abilities = abilitiesForBar,
                                    cooldowns = cooldowns,
                                    queuedAbilityId = queuedAbilityId,
                                    currentMana = playerCombatant?.currentMana ?: displayUser?.currentMana ?: 0,
                                    currentStamina = playerCombatant?.currentStamina ?: displayUser?.currentStamina ?: 0,
                                    onAbilityClick = { viewModel.handleAbilityClick(it) },
                                    iconMappings = iconMappings,
                                    onInspectClick = { showInspectionModal = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Event log with floating admin controls overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        // Event log takes full space
                        EventLog(
                            entries = eventLogEntries,
                            modifier = Modifier.fillMaxSize(),
                            maxVisibleEntries = 4,
                            isAdmin = false  // Copy button moved to floating panel
                        )

                        // Mode toggle in top-right corner (admin only)
                        if (!ghostMode && isAdmin) {
                            ModeToggle(
                                isCreateMode = false,
                                onToggle = onSwitchToCreate,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 4.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            // === CHARACTER SHEET: Full-screen overlay ===
            if (showCharacterSheet && currentUser != null && !ghostMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E))
                ) {
                    UserProfileView(
                        user = displayUser ?: currentUser,  // Use reactive user for live inventory updates
                        currentUser = displayUser ?: currentUser,
                        isAdmin = isAdmin,
                        onUserUpdated = { updatedUser ->
                            // Reload abilities and phasewalk destinations when equipment changes
                            viewModel.loadPlayerAbilities()
                            viewModel.loadPhasewalkDestinations()
                        },
                        onLogout = {
                            showCharacterSheet = false
                            UserStateHolder.logout()
                        },
                        onNavigateToItem = { },
                        onBack = { showCharacterSheet = false },
                        onNavigateToAdmin = if (isAdmin && onNavigateToAdminPanel != null) {
                            { showCharacterSheet = false; onNavigateToAdminPanel() }
                        } else if (isAdmin) {
                            { showCharacterSheet = false; onSwitchToCreate() }
                        } else null,
                        onLeaveParty = { viewModel.leaveParty() }
                    )
                }
            }

            // === DETAIL VIEW: Slides in from right ===
            if (uiState.isDetailViewVisible) {
                DetailView(
                    creature = uiState.selectedCreature,
                    item = uiState.selectedItem,
                    allAbilitiesMap = uiState.allAbilitiesMap,
                    playerAbilities = uiState.playerAbilities,
                    cooldowns = cooldowns,
                    queuedAbilityId = queuedAbilityId,
                    showDescriptionPopup = uiState.showDescriptionPopup,
                    offsetX = detailOffsetX,
                    snackbarHostState = snackbarHostState,
                    onBack = { viewModel.clearSelection() },
                    onShowDescription = { viewModel.showDescriptionPopup() },
                    onHideDescription = { viewModel.hideDescriptionPopup() },
                    onAbilityClick = { ability -> viewModel.useAbilityOnCreature(ability, uiState.selectedCreature) },
                    onTrainClick = { creature -> viewModel.openTrainerModal(creature) }
                )
            }

            // === LOCATION DETAIL POPUP ===
            if (showLocationDetailPopup && uiState.currentLocation != null) {
                LocationDetailPopup(
                    location = uiState.currentLocation!!,
                    onDismiss = { showLocationDetailPopup = false }
                )
            }

            // === TARGET SELECTION OVERLAY ===
            if (uiState.pendingAbility != null) {
                val ability = uiState.pendingAbility!!

                // Build target list based on ability target type
                val targets = when (ability.targetType) {
                    "single_ally_downed" -> {
                        // For Aid/Drag: show downed player allies from combat
                        combatants.filter { combatant ->
                            combatant.id != currentUser?.id && // Not self
                            combatant.isDowned // Only downed players
                        }.map { combatant ->
                            CombatTarget(
                                id = combatant.id,
                                name = combatant.name,
                                currentHp = combatant.currentHp,
                                maxHp = combatant.maxHp,
                                isPlayer = true,
                                isAlive = true,  // Downed but not dead
                                isDowned = true
                            )
                        }
                    }
                    "single_ally" -> {
                        // For ally-targeting abilities: show player allies in combat
                        combatants.filter { combatant ->
                            combatant.id != currentUser?.id && // Not self
                            !combatant.isDowned // Not downed
                        }.map { combatant ->
                            CombatTarget(
                                id = combatant.id,
                                name = combatant.name,
                                currentHp = combatant.currentHp,
                                maxHp = combatant.maxHp,
                                isPlayer = true,
                                isAlive = combatant.isAlive
                            )
                        }
                    }
                    else -> {
                        // Default: enemies (creatures)
                        uiState.creaturesHere.map { creature ->
                            CombatTarget(
                                id = creature.id,
                                name = creature.name,
                                currentHp = creature.maxHp,
                                maxHp = creature.maxHp,
                                isPlayer = false,
                                isAlive = true
                            )
                        }
                    }
                }
                TargetSelectionOverlay(
                    ability = ability,
                    targets = targets,
                    onTargetSelected = { targetId -> viewModel.selectAbilityTarget(targetId) },
                    onCancel = { viewModel.cancelAbilityTargeting() }
                )
            }

            // === MAP SELECTION OVERLAY: For teleport abilities ===
            if (uiState.showMapSelection) {
                MapSelectionOverlay(
                    destinations = uiState.teleportDestinations,
                    currentAreaId = uiState.currentLocation?.areaId,
                    onDestinationSelected = { viewModel.selectTeleportDestination(it) },
                    onCancel = { viewModel.dismissMapSelection() }
                )
            }

            // === RIFT SELECTION OVERLAY: For rift open/seal abilities ===
            if (uiState.showRiftSelection) {
                RiftSelectionOverlay(
                    mode = uiState.riftMode ?: "open",
                    unconnectedAreas = uiState.unconnectedAreas,
                    sealableRifts = uiState.sealableRifts,
                    onAreaSelected = { viewModel.selectRiftToOpen(it) },
                    onRiftSelected = { viewModel.selectRiftToSeal(it) },
                    onCancel = { viewModel.dismissRiftSelection() }
                )
            }

            // === BASIC ATTACK TARGET SELECTION MODAL ===
            if (showBasicAttackTargetModal && uiState.creaturesHere.isNotEmpty()) {
                BasicAttackTargetModal(
                    creatures = uiState.creaturesHere,
                    onTargetSelected = { creatureId ->
                        showBasicAttackTargetModal = false
                        viewModel.initiateBasicAttack(creatureId)
                    },
                    onDismiss = { showBasicAttackTargetModal = false }
                )
            }

            // === GIVE UP CONFIRMATION MODAL ===
            if (showGiveUpConfirmation) {
                GiveUpConfirmationModal(
                    onConfirm = {
                        showGiveUpConfirmation = false
                        viewModel.giveUp { success, message ->
                            // The WebSocket handles the death animation
                        }
                    },
                    onDismiss = { showGiveUpConfirmation = false }
                )
            }

            // === TRAINER MODAL ===
            if (uiState.showTrainerModal) {
                TrainerModal(
                    trainerInfo = uiState.trainerInfo,
                    isLoading = uiState.isLoadingTrainer,
                    playerGold = currentUser?.gold ?: 0,
                    onLearnAbility = { abilityId -> viewModel.learnAbility(abilityId) },
                    onDismiss = { viewModel.dismissTrainerModal() }
                )
            }

            // === PUZZLE MODAL ===
            if (uiState.showPuzzleModal) {
                PuzzleModal(
                    puzzle = uiState.currentPuzzle,
                    puzzleProgress = uiState.puzzleProgress,
                    isLoading = uiState.isLoadingPuzzle,
                    onPullLever = { leverId -> viewModel.pullLever(leverId) },
                    onDismiss = { viewModel.dismissPuzzleModal() }
                )
            }

            // === PLAYER INTERACTION MODAL ===
            val selectedPlayer = uiState.selectedPlayer
            if (uiState.showPlayerInteractionModal && selectedPlayer != null) {
                // Check if this player has a pending party invite for us
                val hasPendingInviteFromPlayer = pendingPartyInvite?.first == selectedPlayer.id
                // Check if we're already in a party with this player
                val currentUser = reactiveUser
                val isInParty = currentUser?.partyLeaderId != null
                val isFollowingThisPlayer = currentUser?.partyLeaderId == selectedPlayer.id
                PlayerInteractionModal(
                    player = selectedPlayer,
                    isInSameParty = isFollowingThisPlayer,
                    hasPendingPartyInvite = hasPendingInviteFromPlayer,
                    onAttack = { viewModel.attackPlayer(selectedPlayer) },
                    onRob = { viewModel.robPlayer(selectedPlayer) },
                    onInviteToParty = { viewModel.inviteToParty(selectedPlayer) },
                    onAcceptParty = { viewModel.acceptPartyInvite(selectedPlayer) },
                    onHeal = { /* TODO: Implement party heal */ },
                    onGive = { viewModel.showGiveItemModal() },
                    onDismiss = { viewModel.dismissPlayerInteractionModal() }
                )
            }

            // === GIVE ITEM MODAL ===
            if (uiState.showGiveItemModal && selectedPlayer != null && displayUser != null) {
                GiveItemModal(
                    playerInventory = uiState.allItems.filter { it.id in displayUser.itemIds },
                    equippedItemIds = displayUser.equippedItemIds,
                    receiverName = selectedPlayer.name,
                    onGiveItem = { itemId -> viewModel.giveItemToPlayer(itemId) },
                    onDismiss = { viewModel.dismissGiveItemModal() }
                )
            }

            // === CREATURE INTERACTION MODAL ===
            val selectedCreature = uiState.selectedCreature
            if (uiState.showCreatureInteractionModal && selectedCreature != null) {
                CreatureInteractionModal(
                    creature = selectedCreature,
                    abilities = uiState.playerAbilities,
                    cooldowns = cooldowns,
                    currentMana = playerCombatant?.currentMana ?: displayUser?.currentMana ?: 0,
                    currentStamina = playerCombatant?.currentStamina ?: displayUser?.currentStamina ?: 0,
                    playerGold = displayUser?.gold ?: 0,
                    diplomacyResult = uiState.diplomacyResult,
                    isDiplomacyLoading = uiState.isDiplomacyLoading,
                    reactionResult = uiState.reactionResult,
                    onBasicAttack = { viewModel.initiateBasicAttack(selectedCreature.id) },
                    onAbilityClick = { ability -> viewModel.useAbilityOnCreature(ability, selectedCreature) },
                    onTrain = if (selectedCreature.isTrainer) {
                        { viewModel.openTrainerModal(selectedCreature) }
                    } else null,
                    onBribe = { viewModel.attemptBribe(selectedCreature.id) },
                    onParley = { viewModel.attemptParley(selectedCreature.id) },
                    onLook = {
                        // Show description popup
                        viewModel.showDescriptionPopup()
                    },
                    onDismiss = { viewModel.dismissCreatureInteractionModal() }
                )
            }

            // === SNACKBAR HOST ===
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )

        } else {
            // No location found - likely server connection issue
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Can't connect to server",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "Check your connection and try again",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Ghost mode floating indicator - show progress if generating, otherwise show Create Character button
        if (ghostMode && onGhostModeBack != null && currentUser != null) {
            // Check if class generation is in progress
            val classGenerationStatus by AsyncOperationRepository.classGenerationStatus.collectAsState()
            val isGenerating = classGenerationStatus.containsKey(currentUser.id)
            val generationMessage = classGenerationStatus[currentUser.id]?.message ?: "Creating your character..."

            // Listen for class generation completion
            LaunchedEffect(currentUser.id) {
                AsyncOperationRepository.classGenerationCompletions.collect { result ->
                    if (result.userId == currentUser.id && result.success) {
                        // Class generation completed - update user and trigger navigation
                        result.user?.let { updatedUser ->
                            AuthStorage.saveUser(updatedUser)
                            UserStateHolder.updateUser(updatedUser)
                            // The App.kt AuthEvent.UserUpdated listener will handle navigation to Main
                        }
                    }
                }
            }

            Surface(
                onClick = if (isGenerating) { {} } else onGhostModeBack,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),  // Above event log
                shape = RoundedCornerShape(24.dp),
                color = if (isGenerating) Color(0xFF4A4A6A) else Color(0xFF6366F1),  // Dimmer when generating
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isGenerating) {
                        // Show spinning progress indicator
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = generationMessage,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Create Character",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Connection status indicator - blinking red icon when disconnected
        ConnectionIndicator(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )

        // Inspection Modal - shows creatures, items, and players you can look at
        if (showInspectionModal && uiState.currentLocation != null) {
            // Check if this is a fishing location (freshwater or coastal)
            val isFishingLocation = uiState.currentLocation!!.featureIds.contains("feature-fishing-spot") ||
                                    uiState.currentLocation!!.featureIds.contains("feature-coastal-fishing")

            InspectionModal(
                creatures = uiState.creaturesHere,
                items = uiState.itemsHere,
                players = uiState.playersHere,
                location = uiState.currentLocation!!,
                isFishingLocation = isFishingLocation,
                onInspectCreature = { creature ->
                    showInspectionModal = false
                    viewModel.selectCreature(creature)
                    viewModel.showDescriptionPopup()
                },
                onInspectItem = { item ->
                    showInspectionModal = false
                    viewModel.selectItem(item)
                    viewModel.showDescriptionPopup()
                },
                onInspectPlayer = { player ->
                    showInspectionModal = false
                    viewModel.selectPlayer(player)
                },
                onInspectLocation = {
                    showInspectionModal = false
                    showLocationDetailPopup = true
                },
                onSearch = { viewModel.searchLocation() },
                onHide = { viewModel.openHideItemModal() },
                onFish = { viewModel.openFishingModal() },
                onDismiss = { showInspectionModal = false }
            )
        }

        // Hide Item Modal - shows items that can be hidden
        if (uiState.showHideItemModal) {
            HideItemModal(
                items = uiState.hideableItems,
                onSelectItem = { item -> viewModel.hideItem(item) },
                onDismiss = { viewModel.cancelHideItem() }
            )
        }

        // Search overlay - shows pizza spinner while searching
        if (uiState.isSearching) {
            SearchOverlay(durationMs = uiState.searchDurationMs)
        }

        // Fishing overlay - shows fishing animation while fishing
        if (uiState.isFishing) {
            FishingOverlay(durationMs = uiState.fishingDurationMs)
        }

        // Fishing distance modal - choose cast distance
        val fishingInfo = uiState.fishingInfo
        if (uiState.showFishingDistanceModal && fishingInfo != null) {
            FishingDistanceModal(
                fishingInfo = fishingInfo,
                onSelectDistance = { viewModel.startFishing(it) },
                onDismiss = { viewModel.closeFishingModal() }
            )
        }

        // Fishing minigame overlay - Stardew Valley style slider
        val minigameData = uiState.fishingMinigameData
        if (uiState.showFishingMinigame && minigameData != null) {
            FishingMinigameOverlay(
                minigameData = minigameData,
                onComplete = { finalScore -> viewModel.completeFishingMinigame(finalScore) },
                onCancel = { viewModel.cancelFishing() }
            )
        }

        // Lockpicking minigame overlay - trace the path mechanic
        val lockpickingInfo = uiState.lockpickingInfo
        if (uiState.showLockpickingMinigame && lockpickingInfo != null) {
            LockpickingMinigameOverlay(
                lockInfo = lockpickingInfo,
                onComplete = { accuracy -> viewModel.completeLockpicking(accuracy) },
                onCancel = { viewModel.cancelLockpicking() }
            )
        }

        // Death transition overlay - covers everything when player dies
        if (isPlayingDeathAnimation && respawnLocationName != null) {
            DeathTransitionOverlay(
                respawnLocationName = respawnLocationName!!,
                onAnimationComplete = { CombatStateHolder.onDeathAnimationComplete() }
            )
        }
    }
}

// =============================================================================
// LOCATION INFO PANEL
// =============================================================================

@Composable
private fun LocationInfoPanel(
    location: LocationDto,
    creaturesHere: List<CreatureDto>,
    playersHere: List<UserDto>,
    itemsHere: List<ItemDto>,
    creatureStates: Map<String, String>,
    isBlinded: Boolean,
    puzzlesHere: List<PuzzleDto> = emptyList(),
    onCreatureClick: (CreatureDto) -> Unit,
    onItemClick: (ItemDto) -> Unit,
    onPlayerClick: (UserDto) -> Unit = {},
    onLocationNameClick: () -> Unit = {},
    onPuzzleClick: (PuzzleDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        // Location name with coordinates - tappable to show details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clickable { onLocationNameClick() }
        ) {
            Text(
                text = location.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            val gridX = location.gridX ?: 0
            val gridY = location.gridY ?: 0
            Text(
                text = "($gridX, $gridY)",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        // Others section - shows both players and creatures
        Text(
            text = if (isBlinded) "Presences" else "Others",
            color = Color.Gray,
            fontSize = 10.sp
        )
        if (creaturesHere.isEmpty() && playersHere.isEmpty()) {
            Text(
                text = if (isBlinded) "You sense nothing nearby" else "None",
                color = Color.White.copy(alpha = if (isBlinded) 0.6f else 1f),
                fontSize = 14.sp
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show players first (in green) - clickable to open interaction modal
                playersHere.forEach { player ->
                    key("player-${player.id}") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onPlayerClick(player) }
                                .padding(vertical = 2.dp)
                        ) {
                            if (!isBlinded) {
                                Text(
                                    text = "\uD83D\uDC64",  // Person silhouette emoji
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = if (isBlinded) "A presence" else player.name,
                                color = if (isBlinded) Color.White.copy(alpha = 0.6f) else Color(0xFF4CAF50),  // Green for players
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                // Show creatures (in blue)
                creaturesHere.forEachIndexed { index, creature ->
                    key("creature-${creature.id}") {
                        val state = creatureStates[creature.id] ?: "idle"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onCreatureClick(creature) }
                                .padding(vertical = 2.dp)
                        ) {
                            // Only show icon and spacer for states that have visible icons
                            if (!isBlinded && state in listOf("wandering", "in_combat")) {
                                CreatureStateIcon(
                                    state = state,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isBlinded) getBlindPresenceDescription(creature, index) else creature.name,
                                color = if (isBlinded) Color.White.copy(alpha = 0.6f) else Color(0xFF64B5F6),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Items section - horizontal scrolling row
        Text(
            text = if (isBlinded) "Objects" else "Items",
            color = Color.Gray,
            fontSize = 10.sp
        )
        if (itemsHere.isEmpty()) {
            Text(
                text = if (isBlinded) "You feel nothing unusual" else "None",
                color = Color.White.copy(alpha = if (isBlinded) 0.6f else 1f),
                fontSize = 14.sp
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsHere.forEachIndexed { index, item ->
                    // Use key() to help Compose track individual items and avoid full recomposition
                    key(item.id) {
                        // Show asterisk prefix for items discovered via search
                        val isDiscovered = item.id in location.discoveredItemIds
                        val displayName = when {
                            isBlinded -> getBlindItemDescription(item, index)
                            isDiscovered -> "*${item.name}"
                            else -> item.name
                        }
                        Text(
                            text = displayName,
                            color = if (isBlinded) Color.White.copy(alpha = 0.6f) else Color(0xFFFFD54F),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onItemClick(item) }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Puzzles section - only show if puzzles exist and not blinded
        if (puzzlesHere.isNotEmpty() && !isBlinded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Puzzles",
                color = Color.Gray,
                fontSize = 10.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                puzzlesHere.forEach { puzzle ->
                    key(puzzle.id) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onPuzzleClick(puzzle) }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gavel,
                                contentDescription = "Puzzle",
                                tint = Color(0xFF9C27B0),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = puzzle.name,
                                color = Color(0xFF9C27B0),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SHOP PANEL
// =============================================================================

/**
 * Calculate charisma discount: 3% per modifier point above 10, max 20%.
 */
private fun calculateCharismaDiscount(charisma: Int): Int {
    val modifier = (charisma - 10) / 2
    return (modifier * 3).coerceIn(0, 20)
}

private fun applyCharismaDiscount(basePrice: Int, charisma: Int): Int {
    val discountPercent = calculateCharismaDiscount(charisma)
    val discount = (basePrice * discountPercent) / 100
    return (basePrice - discount).coerceAtLeast(1)
}

@Composable
private fun ShopPanel(
    location: LocationDto,
    shopItems: List<ItemDto>,
    playerGold: Int,
    playerCharisma: Int,
    isInn: Boolean,
    isGeneralStore: Boolean = false,
    sellableItems: List<com.ez2bg.anotherthread.api.SellableItemDto> = emptyList(),
    innCost: Int,
    onBuyItem: (ItemDto) -> Unit,
    onRest: () -> Unit,
    onSellItem: (com.ez2bg.anotherthread.api.SellableItemDto) -> Unit = {},
    onLocationNameClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Track whether we're in buy or sell mode for general store
    var showSellSection by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Shop name (clickable to show location details)
        Text(
            text = location.name,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .clickable { onLocationNameClick() }
        )

        // Description
        Text(
            text = location.desc,
            color = Color(0xFFCCCCCC),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isInn) {
            // Inn: rest button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (playerGold >= innCost) Color(0xFF2E7D32).copy(alpha = 0.3f)
                        else Color.Gray.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (playerGold >= innCost) Color(0xFF4CAF50) else Color.Gray,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = playerGold >= innCost) { onRest() }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rest for the Night",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fully restores HP, mana, and stamina",
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "${innCost}g",
                        color = if (playerGold >= innCost) Color(0xFFFFD700) else Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Show discount info if player has high charisma
            val discountPercent = calculateCharismaDiscount(playerCharisma)
            if (discountPercent > 0) {
                Text(
                    text = "Your charm grants ${discountPercent}% discount!",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // For general store, show Buy/Sell tabs
            if (isGeneralStore) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Buy tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (!showSellSection) Color(0xFF2E7D32).copy(alpha = 0.5f)
                                else Color.Gray.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (!showSellSection) Color(0xFF4CAF50) else Color.Gray,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { showSellSection = false }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Buy",
                            color = if (!showSellSection) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Sell tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (showSellSection) Color(0xFFB71C1C).copy(alpha = 0.5f)
                                else Color.Gray.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (showSellSection) Color(0xFFEF5350) else Color.Gray,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { showSellSection = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sell",
                            color = if (showSellSection) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showSellSection && isGeneralStore) {
                // Sell section - show sellable items
                if (sellableItems.isEmpty()) {
                    Text(
                        text = "You have nothing to sell here.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sellableItems.forEach { sellableItem ->
                            SellableItemRow(
                                item = sellableItem,
                                onSell = { onSellItem(sellableItem) }
                            )
                        }
                    }
                }
            } else {
                // Buy section - show shop items
                // Check layout direction - default to VERTICAL
                val isHorizontal = location.shopLayoutDirection == ShopLayoutDirection.HORIZONTAL

                if (isHorizontal) {
                    // Horizontal scrolling item list (compact cards)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shopItems.forEach { item ->
                            val discountedPrice = applyCharismaDiscount(item.value, playerCharisma)
                            ShopItemCard(
                                item = item,
                                basePrice = item.value,
                                discountedPrice = discountedPrice,
                                canAfford = playerGold >= discountedPrice,
                                onBuy = { onBuyItem(item) }
                            )
                        }
                    }
                } else {
                    // Vertical scrolling item list (default - full width rows)
                    // No height constraint - the shop panel expands to fill available space
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shopItems.forEach { item ->
                            val discountedPrice = applyCharismaDiscount(item.value, playerCharisma)
                            ShopItemRow(
                                item = item,
                                basePrice = item.value,
                                discountedPrice = discountedPrice,
                                canAfford = playerGold >= discountedPrice,
                                onBuy = { onBuyItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellableItemRow(
    item: com.ez2bg.anotherthread.api.SellableItemDto,
    onSell: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        // Item info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                // Show food state if applicable
                item.foodState?.let { state ->
                    val stateColor = when (state) {
                        "cooked" -> Color(0xFF81C784)
                        "salted" -> Color(0xFF90CAF9)
                        else -> Color(0xFFEF5350)
                    }
                    Text(
                        text = " ($state)",
                        color = stateColor,
                        fontSize = 12.sp
                    )
                }
            }
            // Show time until spoil for food items
            item.timeUntilSpoil?.let { time ->
                Text(
                    text = "Fresh for: $time",
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp
                )
            }
        }

        // Sell button
        Box(
            modifier = Modifier
                .background(Color(0xFFB71C1C).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable { onSell() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Sell ${item.sellValue}g",
                color = Color(0xFFFFD700),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ShopItemRow(
    item: ItemDto,
    basePrice: Int,
    discountedPrice: Int,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    val hasDiscount = discountedPrice < basePrice

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        // Item info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Color(0xFFFFD54F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            // Stats row
            val statParts = mutableListOf<String>()
            item.statBonuses?.let { stats ->
                if (stats.attack != 0) statParts.add("ATK +${stats.attack}")
                if (stats.defense != 0) statParts.add("DEF +${stats.defense}")
                if (stats.maxHp != 0) statParts.add("HP +${stats.maxHp}")
            }
            item.equipmentType?.let { type ->
                statParts.add(0, type.replaceFirstChar { it.uppercase() })
            }
            if (statParts.isNotEmpty()) {
                Text(
                    text = statParts.joinToString(" \u2022 "),
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp
                )
            }
        }

        // Price and buy button
        Box(
            modifier = Modifier
                .background(
                    if (canAfford) Color(0xFF1B5E20).copy(alpha = 0.5f)
                    else Color.Gray.copy(alpha = 0.2f),
                    RoundedCornerShape(4.dp)
                )
                .clickable(enabled = canAfford) { onBuy() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasDiscount) {
                    // Show original price struck through
                    Text(
                        text = "${basePrice}g",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    text = "${discountedPrice}g",
                    color = if (canAfford) {
                        if (hasDiscount) Color(0xFF81C784) else Color(0xFFFFD700)
                    } else Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ItemDto,
    basePrice: Int,
    discountedPrice: Int,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    val hasDiscount = discountedPrice < basePrice

    Column(
        modifier = Modifier
            .width(120.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (canAfford) Color(0xFFFFD54F).copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = canAfford) { onBuy() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Item name
        Text(
            text = item.name,
            color = Color(0xFFFFD54F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Price
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (hasDiscount) {
                Text(
                    text = "${basePrice}g",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = "${discountedPrice}g",
                color = if (canAfford) {
                    if (hasDiscount) Color(0xFF81C784) else Color(0xFFFFD700)
                } else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =============================================================================
// CENTER MINIMAP (replaces location thumbnail in center of screen)
// =============================================================================

/**
 * Centered minimap that replaces the location thumbnail.
 * 100x100 size, shows the map with current position highlighted.
 */
@Composable
private fun CenterMinimap(
    locations: List<LocationDto>,
    currentLocation: LocationDto,
    unvisitedConnectedLocations: List<LocationDto> = emptyList(),
    isBlinded: Boolean,
    blindRounds: Int,
    onClick: () -> Unit
) {
    val currentGridX = currentLocation.gridX ?: 0
    val currentGridY = currentLocation.gridY ?: 0

    // Animated position
    val animatedGridX by animateFloatAsState(
        targetValue = currentGridX.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "centerMinimapX"
    )
    val animatedGridY by animateFloatAsState(
        targetValue = currentGridY.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "centerMinimapY"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.8f))
            .border(2.dp, Color(0xFF4A4A4A), CircleShape)
            .clickable(onClick = onClick)
    ) {
        if (isBlinded && blindRounds > 0) {
            BlindOverlay(
                roundsRemaining = blindRounds,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val gridSpacingPx = 28.dp.toPx()
                val dotRadius = 6.dp.toPx()
                val highlightRadius = 9.dp.toPx()
                val lineColor = Color(0xFFFF9800) // Orange theme for all players
                val lineWidth = 2.dp.toPx()
                val highlightStrokeWidth = 2.5f.dp.toPx()
                val dotOutlineWidth = 1.dp.toPx()

                // Vignette function
                val vignetteRadius = minOf(size.width, size.height) / 2
                fun vignetteAlpha(x: Float, y: Float): Float {
                    val dx = x - centerX
                    val dy = y - centerY
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    val fadeStart = vignetteRadius * 0.5f
                    val fadeEnd = vignetteRadius * 0.95f
                    return when {
                        dist <= fadeStart -> 1f
                        dist >= fadeEnd -> 0f
                        else -> 1f - ((dist - fadeStart) / (fadeEnd - fadeStart))
                    }
                }

                // Include both visited locations and unvisited connected locations for drawing
                val allDisplayLocations = locations + unvisitedConnectedLocations
                val locationById = allDisplayLocations.associateBy { it.id }
                val unvisitedIds = unvisitedConnectedLocations.map { it.id }.toSet()
                val drawnConnections = mutableSetOf<Pair<String, String>>()
                val visited = mutableSetOf<String>()
                val queue = ArrayDeque<Pair<String, Int>>()

                queue.add(currentLocation.id to 0)
                visited.add(currentLocation.id)

                // Draw connections via BFS
                while (queue.isNotEmpty()) {
                    val (locId, depth) = queue.removeFirst()
                    if (depth >= 2) continue

                    val loc = locationById[locId] ?: continue
                    val locGridX = loc.gridX ?: continue
                    val locGridY = loc.gridY ?: continue
                    val locRelX = locGridX - animatedGridX
                    val locRelY = locGridY - animatedGridY

                    loc.exits.forEach { exit ->
                        val targetLoc = locationById[exit.locationId] ?: return@forEach
                        val targetGridX = targetLoc.gridX ?: return@forEach
                        val targetGridY = targetLoc.gridY ?: return@forEach
                        val targetRelX = targetGridX - animatedGridX
                        val targetRelY = targetGridY - animatedGridY

                        val connKey = if (locId < exit.locationId) locId to exit.locationId else exit.locationId to locId

                        if (kotlin.math.abs(locRelX) <= 2 && kotlin.math.abs(locRelY) <= 2 &&
                            kotlin.math.abs(targetRelX) <= 2 && kotlin.math.abs(targetRelY) <= 2 &&
                            !drawnConnections.contains(connKey)) {

                            drawnConnections.add(connKey)

                            val fromX = centerX + locRelX * gridSpacingPx
                            val fromY = centerY + locRelY * gridSpacingPx
                            val toX = centerX + targetRelX * gridSpacingPx
                            val toY = centerY + targetRelY * gridSpacingPx

                            val depthAlpha = when (depth) {
                                0 -> 0.9f
                                else -> 0.5f
                            }

                            val fromVignette = vignetteAlpha(fromX, fromY)
                            val toVignette = vignetteAlpha(toX, toY)
                            val vignetteMultiplier = minOf(fromVignette, toVignette)
                            val finalAlpha = depthAlpha * vignetteMultiplier

                            if (finalAlpha > 0.01f) {
                                val dx = toX - fromX
                                val dy = toY - fromY
                                val length = kotlin.math.sqrt(dx * dx + dy * dy)
                                if (length > dotRadius * 2) {
                                    val shortenAmount = dotRadius + 1.dp.toPx()
                                    val ratio = shortenAmount / length
                                    val adjustedFromX = fromX + dx * ratio
                                    val adjustedFromY = fromY + dy * ratio
                                    val adjustedToX = toX - dx * ratio
                                    val adjustedToY = toY - dy * ratio

                                    drawLine(
                                        color = lineColor.copy(alpha = finalAlpha),
                                        start = Offset(adjustedFromX, adjustedFromY),
                                        end = Offset(adjustedToX, adjustedToY),
                                        strokeWidth = lineWidth
                                    )
                                }
                            }
                        }

                        if (!visited.contains(exit.locationId)) {
                            visited.add(exit.locationId)
                            queue.add(exit.locationId to depth + 1)
                        }
                    }
                }

                // Draw dots for all display locations (visited + unvisited connected)
                allDisplayLocations.forEach { location ->
                    val locGridX = location.gridX ?: return@forEach
                    val locGridY = location.gridY ?: return@forEach

                    val relX = locGridX - animatedGridX
                    val relY = locGridY - animatedGridY

                    if (kotlin.math.abs(relX) <= 2 && kotlin.math.abs(relY) <= 2) {
                        val dotX = centerX + relX * gridSpacingPx
                        val dotY = centerY + relY * gridSpacingPx
                        val isCurrentLoc = location.id == currentLocation.id

                        val dotVignetteAlpha = if (isCurrentLoc) 1f else vignetteAlpha(dotX, dotY)

                        if (dotVignetteAlpha > 0.01f) {
                            val isUnvisited = location.id in unvisitedIds

                            if (isCurrentLoc) {
                                drawCircle(
                                    color = lineColor,
                                    radius = highlightRadius,
                                    center = Offset(dotX, dotY),
                                    style = Stroke(width = highlightStrokeWidth)
                                )
                            }

                            if (isUnvisited) {
                                // Draw "?" marker for unvisited but connected locations
                                val fogColor = Color(0xFF555555)
                                val questionColor = Color(0xFFAAAAAA)
                                drawCircle(
                                    color = fogColor.copy(alpha = 0.8f * dotVignetteAlpha),
                                    radius = dotRadius,
                                    center = Offset(dotX, dotY)
                                )
                                // Draw a dashed outline to indicate unknown
                                drawCircle(
                                    color = questionColor.copy(alpha = 0.9f * dotVignetteAlpha),
                                    radius = dotRadius,
                                    center = Offset(dotX, dotY),
                                    style = Stroke(width = dotOutlineWidth * 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
                                )
                                // Draw a small "?" shape inside using a small arc and dot
                                val qSize = dotRadius * 0.6f
                                // Draw the curved part of "?"
                                drawArc(
                                    color = questionColor.copy(alpha = dotVignetteAlpha),
                                    startAngle = 200f,
                                    sweepAngle = 250f,
                                    useCenter = false,
                                    topLeft = Offset(dotX - qSize, dotY - qSize - 1f),
                                    size = Size(qSize * 2, qSize * 1.5f),
                                    style = Stroke(width = 1.5f)
                                )
                                // Draw the dot at bottom of "?"
                                drawCircle(
                                    color = questionColor.copy(alpha = dotVignetteAlpha),
                                    radius = 1f,
                                    center = Offset(dotX, dotY + qSize * 0.6f)
                                )
                            } else {
                                // Draw normal terrain-colored dot for visited locations
                                val terrainColor = getTerrainColor(location.desc, location.name)
                                drawCircle(
                                    color = terrainColor.copy(alpha = terrainColor.alpha * dotVignetteAlpha),
                                    radius = dotRadius,
                                    center = Offset(dotX, dotY)
                                )
                                drawCircle(
                                    color = lineColor.copy(alpha = 0.6f * dotVignetteAlpha),
                                    radius = dotRadius,
                                    center = Offset(dotX, dotY),
                                    style = Stroke(width = dotOutlineWidth)
                                )
                            }
                        }
                    }
                }

                // Fog of war
                val fogRadius = size.minDimension / 2
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        center = Offset(centerX, centerY),
                        radius = fogRadius
                    ),
                    radius = fogRadius,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}

// =============================================================================
// DIRECTIONAL RING (innermost ring around minimap)
// =============================================================================

/**
 * Directional navigation buttons arranged in a ring around the center minimap.
 * ENTER exits are NOT shown here - they appear in the floating action row above status bars.
 * Also shows phasewalk destinations (purple boot icons) for directions without exits.
 */
@Composable
private fun DirectionalRing(
    exits: List<ExitDto>,
    locations: List<LocationDto>,
    phasewalkDestinations: List<PhasewalkDestinationDto>,
    phasewalkEnabled: Boolean = true,
    navigationDisabled: Boolean = false,  // When true (over-encumbered), grey out exits
    onNavigate: (ExitDto) -> Unit,
    onPhasewalk: (String) -> Unit
) {
    val ringRadius = 70.dp  // Closer to thumbnail than ability ring was
    val buttonSize = 28.dp

    // Only show directional exits (compass directions + UP/DOWN), not ENTER exits
    val directionalExits = exits.filter { it.direction != ExitDirection.ENTER && it.direction != ExitDirection.UNKNOWN }

    // Filter phasewalk destinations to exclude any direction that has a regular exit
    // This is a safety check - server should already filter these, but this prevents overlap issues
    val exitDirections = directionalExits.map { it.direction.name.lowercase() }.toSet()
    val filteredPhasewalkDestinations = phasewalkDestinations.filter {
        it.direction.lowercase() !in exitDirections
    }

    // Helper to calculate offset for a direction
    fun getDirectionOffset(direction: String): Pair<Dp, Dp> {
        val upDownOffset = ringRadius * 1.4f  // UP/DOWN are further out than compass directions
        return when (direction.lowercase()) {
            "north" -> Pair(0.dp, -ringRadius)
            "south" -> Pair(0.dp, ringRadius)
            "east" -> Pair(ringRadius, 0.dp)
            "west" -> Pair(-ringRadius, 0.dp)
            "northeast" -> {
                val diag = ringRadius.value * 0.707f
                Pair(diag.dp, -diag.dp)
            }
            "northwest" -> {
                val diag = ringRadius.value * 0.707f
                Pair(-diag.dp, -diag.dp)
            }
            "southeast" -> {
                val diag = ringRadius.value * 0.707f
                Pair(diag.dp, diag.dp)
            }
            "southwest" -> {
                val diag = ringRadius.value * 0.707f
                Pair(-diag.dp, diag.dp)
            }
            "up" -> Pair(0.dp, -upDownOffset)  // Above north
            "down" -> Pair(0.dp, upDownOffset)  // Below south
            else -> Pair(0.dp, 0.dp)
        }
    }

    Box(contentAlignment = Alignment.Center) {
        // Render directional exits in their compass positions
        directionalExits.forEach { exit ->
            val targetLocation = locations.find { it.id == exit.locationId }
            if (targetLocation != null) {
                // Calculate position based on direction
                val upDownOffset = ringRadius * 1.4f  // UP/DOWN are further out
                val (offsetX, offsetY) = when (exit.direction) {
                    ExitDirection.NORTH -> Pair(0.dp, -ringRadius)
                    ExitDirection.SOUTH -> Pair(0.dp, ringRadius)
                    ExitDirection.EAST -> Pair(ringRadius, 0.dp)
                    ExitDirection.WEST -> Pair(-ringRadius, 0.dp)
                    ExitDirection.NORTHEAST -> {
                        val diag = ringRadius.value * 0.707f
                        Pair(diag.dp, -diag.dp)
                    }
                    ExitDirection.NORTHWEST -> {
                        val diag = ringRadius.value * 0.707f
                        Pair(-diag.dp, -diag.dp)
                    }
                    ExitDirection.SOUTHEAST -> {
                        val diag = ringRadius.value * 0.707f
                        Pair(diag.dp, diag.dp)
                    }
                    ExitDirection.SOUTHWEST -> {
                        val diag = ringRadius.value * 0.707f
                        Pair(-diag.dp, diag.dp)
                    }
                    ExitDirection.UP -> Pair(0.dp, -upDownOffset)
                    ExitDirection.DOWN -> Pair(0.dp, upDownOffset)
                    else -> Pair(0.dp, 0.dp)
                }

                val icon = when (exit.direction) {
                    ExitDirection.NORTH -> Icons.Filled.ArrowUpward
                    ExitDirection.SOUTH -> Icons.Filled.ArrowDownward
                    ExitDirection.EAST -> Icons.AutoMirrored.Filled.ArrowForward
                    ExitDirection.WEST -> Icons.AutoMirrored.Filled.ArrowBack
                    ExitDirection.NORTHEAST -> Icons.Filled.NorthEast
                    ExitDirection.NORTHWEST -> Icons.Filled.NorthWest
                    ExitDirection.SOUTHEAST -> Icons.Filled.SouthEast
                    ExitDirection.SOUTHWEST -> Icons.Filled.SouthWest
                    ExitDirection.UP -> Icons.Filled.ArrowUpward
                    ExitDirection.DOWN -> Icons.Filled.ArrowDownward
                    else -> Icons.Filled.ArrowUpward
                }

                // UP/DOWN get a different color (green for vertical movement)
                val exitColor = when (exit.direction) {
                    ExitDirection.UP, ExitDirection.DOWN -> Color(0xFF4CAF50)  // Green for vertical
                    else -> Color(0xFF1976D2)  // Blue for horizontal
                }

                DirectionalButton(
                    exit = exit,
                    icon = icon,
                    color = exitColor,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    buttonSize = buttonSize,
                    enabled = !navigationDisabled,
                    onNavigate = onNavigate
                )
            }
        }

        // Render phasewalk destinations (purple boot icons with mana cost)
        // Show disabled (grey) buttons when player can't use phasewalk
        // Use filtered list to ensure no overlap with regular exits
        filteredPhasewalkDestinations.forEach { destination ->
            val (offsetX, offsetY) = getDirectionOffset(destination.direction)
            PhasewalkButton(
                direction = destination.direction,
                locationName = destination.locationName,
                offsetX = offsetX,
                offsetY = offsetY,
                buttonSize = buttonSize,
                enabled = phasewalkEnabled,
                onPhasewalk = onPhasewalk
            )
        }
    }
}

// DirectionalButton, PhasewalkButton, PurpleActionButton, PartyAbilitiesDropdown, and AbilityRow moved to CombatUIComponents.kt

// BasicAttackTargetModal, GiveUpConfirmationModal, PlayerInteractionModal, CreatureInteractionModal,
// InspectionModal, InspectionItem, HideItemModal, GiveItemModal, GiveItemRow, DescriptionPopup,
// StatColumn, LocationDetailPopup, RiftSelectionOverlay, TrainerModal, TrainerAbilityRow
// moved to ModalComponents.kt

// =============================================================================
// DETAIL VIEW
// =============================================================================

@Composable
private fun DetailView(
    creature: CreatureDto?,
    item: ItemDto?,
    allAbilitiesMap: Map<String, AbilityDto>,
    playerAbilities: List<AbilityDto>,
    cooldowns: Map<String, Int>,
    queuedAbilityId: String?,
    showDescriptionPopup: Boolean,
    offsetX: Float,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onShowDescription: () -> Unit,
    onHideDescription: () -> Unit,
    onAbilityClick: (AbilityDto) -> Unit,
    onTrainClick: (CreatureDto) -> Unit
) {
    val detailName = creature?.name ?: item?.name ?: ""
    val detailImageUrl = creature?.imageUrl ?: item?.imageUrl

    // Check if this creature is a trainer
    val isTrainerCreature = creature != null && creature.isTrainer

    // Filter combat abilities for the ring display (exclude passive, utility for non-combat scenarios)
    val combatAbilities = playerAbilities.filter { ability ->
        ability.abilityType == "combat" || ability.abilityType == "spell"
    }.take(8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = (offsetX * 400).dp)
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        // Name above the 100x100
        Text(
            text = detailName,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-140).dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // Combat ability ring around creature (similar to location ability ring)
        // Only show for non-trainer creatures - trainers show Train button instead
        if (creature != null && !isTrainerCreature && combatAbilities.isNotEmpty()) {
            CreatureCombatRing(
                abilities = combatAbilities,
                cooldowns = cooldowns,
                queuedAbilityId = queuedAbilityId,
                onAbilityClick = onAbilityClick
            )
        }

        // Train button for trainer creatures
        if (isTrainerCreature && creature != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            ) {
                Button(
                    onClick = { onTrainClick(creature) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "Train",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Train")
                }
            }
        }

        // 100x100 detail image
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onShowDescription() })
                }
        ) {
            if (detailImageUrl != null) {
                AsyncImage(
                    model = "${AppConfig.api.baseUrl}${detailImageUrl}",
                    contentDescription = detailName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = detailName,
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            // Info icon hint
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Back button - positioned outside the ability ring
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            OffsetActionButton(
                offset = Offset(0f, 0f),
                color = Color(0xFFFF9800),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
        }

        // Creature abilities display (what abilities the creature has) - bottom
        if (creature != null) {
            val creatureAbilities = creature.abilityIds?.mapNotNull { allAbilitiesMap[it] } ?: emptyList()
            if (creatureAbilities.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 140.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Knows:",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    creatureAbilities.take(4).forEach { ability ->
                        AbilityIconSmall(
                            ability = ability,
                            size = 24.dp
                        )
                    }
                    if (creatureAbilities.size > 4) {
                        Text(
                            text = "+${creatureAbilities.size - 4}",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Description popup overlay
        if (showDescriptionPopup) {
            DescriptionPopup(
                creature = creature,
                item = item,
                imageUrl = detailImageUrl,
                onDismiss = onHideDescription
            )
        }
    }
}

// PuzzleModal, LeverRow, SearchOverlay, FishingOverlay, FishingMinigameOverlay,
// LockpickingMinigameOverlay, FishingDistanceModal, FishingDistanceOption
// moved to MinigameComponents.kt

// =============================================================================
// TICK INDICATOR
// =============================================================================

/**
 * A pizza/pie indicator that shows the global game tick progress.
 * Fills up clockwise starting from 12 o'clock and resets every 3 seconds.
 */
@Composable
private fun TickIndicator(
    modifier: Modifier = Modifier,
    tickDurationMs: Int = 3000,
    size: Dp = 24.dp,
    fillColor: Color = Color(0xFF4CAF50),
    backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    borderColor: Color = Color.White.copy(alpha = 0.3f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tickTransition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = tickDurationMs, easing = LinearEasing)
        ),
        label = "tickProgress"
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        val strokeWidth = 1.5.dp.toPx()
        val padding = strokeWidth / 2

        // Background circle
        drawCircle(
            color = backgroundColor,
            radius = (this.size.minDimension - strokeWidth) / 2,
            center = center
        )

        // Progress arc (pizza slice) - starts at 12 o'clock (-90 degrees) and fills clockwise
        val sweepAngle = progress * 360f
        drawArc(
            color = fillColor,
            startAngle = -90f,  // 12 o'clock position
            sweepAngle = sweepAngle,
            useCenter = true,  // Makes it a pie slice, not just an arc
            topLeft = Offset(padding, padding),
            size = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
        )

        // Border circle
        drawCircle(
            color = borderColor,
            radius = (this.size.minDimension - strokeWidth) / 2,
            center = center,
            style = Stroke(width = strokeWidth)
        )
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

private object PreviewData {
    val sampleLocation = LocationDto(
        id = "loc-1",
        name = "Dark Forest Clearing",
        desc = "A shadowy clearing surrounded by ancient oaks.",
        gridX = 5,
        gridY = 3,
        exits = listOf(
            ExitDto("loc-2", ExitDirection.NORTH),
            ExitDto("loc-3", ExitDirection.EAST),
            ExitDto("loc-4", ExitDirection.SOUTH)
        )
    )

    val sampleCreatures = listOf(
        CreatureDto(
            id = "creature-1",
            name = "Shadow Wolf",
            desc = "A wolf made of living shadow.",
            maxHp = 50,
            baseDamage = 10,
            level = 3,
            isAggressive = true
        ),
        CreatureDto(
            id = "creature-2",
            name = "Forest Spirit",
            desc = "A benevolent spirit of the woods.",
            maxHp = 30,
            baseDamage = 5,
            level = 2,
            isAggressive = false
        )
    )

    val sampleItems = listOf(
        ItemDto(
            id = "item-1",
            name = "Healing Potion",
            desc = "Restores 50 HP.",
            value = 25
        ),
        ItemDto(
            id = "item-2",
            name = "Iron Sword",
            desc = "A sturdy blade.",
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            value = 100
        )
    )

    val sampleAbilities = listOf(
        AbilityDto(
            id = "ability-1",
            name = "Fireball",
            description = "Launches a ball of fire at the enemy.",
            abilityType = "spell",
            targetType = "single_enemy",
            range = 30,
            cooldownType = "short",
            cooldownRounds = 2,
            baseDamage = 25,
            manaCost = 15
        ),
        AbilityDto(
            id = "ability-2",
            name = "Heal",
            description = "Restores health to self.",
            abilityType = "spell",
            targetType = "self",
            range = 0,
            cooldownType = "medium",
            cooldownRounds = 3,
            baseDamage = 0,
            manaCost = 20
        )
    )

    val sampleExits = listOf(
        ExitDto("loc-2", ExitDirection.NORTH),
        ExitDto("loc-3", ExitDirection.EAST),
        ExitDto("loc-4", ExitDirection.SOUTH),
        ExitDto("loc-5", ExitDirection.WEST)
    )
}

@Preview
@Composable
private fun LocationInfoPanelPreview() {
    MaterialTheme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
            LocationInfoPanel(
                location = PreviewData.sampleLocation,
                creaturesHere = PreviewData.sampleCreatures,
                playersHere = emptyList(),
                itemsHere = PreviewData.sampleItems,
                creatureStates = emptyMap(),
                isBlinded = false,
                onCreatureClick = {},
                onItemClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
private fun ModeTogglePreview() {
    MaterialTheme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
            ModeToggle(
                isCreateMode = false,
                onToggle = {}
            )
        }
    }
}

@Preview
@Composable
private fun DirectionalRingPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            DirectionalRing(
                exits = PreviewData.sampleExits,
                locations = listOf(
                    PreviewData.sampleLocation.copy(id = "loc-2"),
                    PreviewData.sampleLocation.copy(id = "loc-3"),
                    PreviewData.sampleLocation.copy(id = "loc-4"),
                    PreviewData.sampleLocation.copy(id = "loc-5")
                ),
                phasewalkDestinations = listOf(
                    PhasewalkDestinationDto("northeast", "loc-6", "Hidden Cave", 1, -1),
                    PhasewalkDestinationDto("southwest", "loc-7", "Secret Path", -1, 1)
                ),
                onNavigate = {},
                onPhasewalk = {}
            )
        }
    }
}
