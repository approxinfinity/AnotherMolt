package com.ez2bg.anotherthread.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.ui.CreatureStateIcon
import com.ez2bg.anotherthread.ui.SwordIcon
import com.ez2bg.anotherthread.ui.getBlindItemDescription
import com.ez2bg.anotherthread.ui.getBlindPresenceDescription
import com.ez2bg.anotherthread.ui.components.AbilityIconButton
import com.ez2bg.anotherthread.ui.components.AbilityIconSmall
import com.ez2bg.anotherthread.ui.components.BlindOverlay
import com.ez2bg.anotherthread.ui.components.CombatTarget
import com.ez2bg.anotherthread.ui.components.DisorientIndicator
import com.ez2bg.anotherthread.ui.components.EventLog
import com.ez2bg.anotherthread.ui.components.TargetSelectionOverlay
import com.ez2bg.anotherthread.ui.admin.getTerrainColor
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
    onGhostModeBack: (() -> Unit)? = null
) {
    // Create ViewModel scoped to this composable
    val viewModel = remember(currentUser?.id) { AdventureViewModel(currentUser) }

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

    // Location detail popup state
    var showLocationDetailPopup by remember { mutableStateOf(false) }

    // Convert event log entries to UI format
    val eventLogEntries = remember(eventLogState) {
        convertEventLogEntries(eventLogState)
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
                            val direction = getSwipeDirection(swipeTotalX, swipeTotalY)
                            if (direction != null) {
                                // Find exit in that direction
                                val exit = uiState.currentLocation?.exits?.find { it.direction == direction }
                                if (exit != null) {
                                    viewModel.navigateToExit(exit)
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
        if (uiState.isLoading) {
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

            // === TOP SECTION: Location info panel or Shop panel ===
            if (!uiState.isDetailViewVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    if ((uiState.isShopLocation || uiState.isInnLocation) && !ghostMode) {
                        ShopPanel(
                            location = currentLocation,
                            shopItems = uiState.shopItems,
                            playerGold = uiState.playerGold,
                            isInn = uiState.isInnLocation,
                            innCost = 25,
                            onBuyItem = { viewModel.buyItem(it.id) },
                            onRest = { viewModel.restAtInn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    } else {
                        LocationInfoPanel(
                            location = currentLocation,
                            creaturesHere = uiState.creaturesHere,
                            itemsHere = uiState.itemsHere,
                            creatureStates = uiState.creatureStates,
                            isBlinded = isBlinded,
                            onCreatureClick = { if (!ghostMode) viewModel.selectCreature(it) },
                            onItemClick = { if (!ghostMode) viewModel.selectItem(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }

            // === CENTER SECTION: Minimap with directional ring ===
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
                    DirectionalRing(
                        exits = currentLocation.exits,
                        locations = uiState.locations,
                        onNavigate = { viewModel.navigateToExit(it) }
                    )

                    // Filter to only show locations in the same area for minimap
                    val currentAreaId = currentLocation.areaId
                    val areaLocations = remember(uiState.locations, currentAreaId) {
                        uiState.locations.filter { it.areaId == currentAreaId }
                    }

                    // Centered minimap (replaces location thumbnail)
                    CenterMinimap(
                        locations = areaLocations,
                        currentLocation = currentLocation,
                        isRanger = uiState.isRanger,
                        isBlinded = isBlinded,
                        blindRounds = blindRounds,
                        onClick = { showLocationDetailPopup = true }
                    )
                }

            }

            // === BOTTOM SECTION: Abilities row, mode toggle, and event log ===
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Abilities row (non-creature-specific actions above scroll section)
                // Hide abilities in ghost mode since actions are disabled
                if (uiState.playerAbilities.isNotEmpty() && !ghostMode) {
                    AbilityRow(
                        abilities = uiState.playerAbilities,
                        cooldowns = cooldowns,
                        queuedAbilityId = queuedAbilityId,
                        onAbilityClick = { viewModel.handleAbilityClick(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Mode toggle - bottom right of middle section (hidden in ghost mode)
                if (!ghostMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ModeToggle(
                            isCreateMode = false,
                            onToggle = onSwitchToCreate,
                            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                        )
                    }
                }

                // Event log at very bottom (always visible)
                EventLog(
                    entries = eventLogEntries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    maxVisibleEntries = 4
                )
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
                    onAbilityClick = { ability -> viewModel.useAbilityOnCreature(ability, uiState.selectedCreature) }
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
                val targets = uiState.creaturesHere.map { creature ->
                    CombatTarget(
                        id = creature.id,
                        name = creature.name,
                        currentHp = creature.maxHp,
                        maxHp = creature.maxHp,
                        isPlayer = false,
                        isAlive = true
                    )
                }
                TargetSelectionOverlay(
                    ability = ability,
                    targets = targets,
                    onTargetSelected = { targetId -> viewModel.selectAbilityTarget(targetId) },
                    onCancel = { viewModel.cancelAbilityTargeting() }
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
            // No location found
            Text(
                text = "No location available",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Ghost mode back button (minimal, just navigation)
        if (ghostMode && onGhostModeBack != null) {
            IconButton(
                onClick = onGhostModeBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to character creation",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
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
    itemsHere: List<ItemDto>,
    creatureStates: Map<String, String>,
    isBlinded: Boolean,
    onCreatureClick: (CreatureDto) -> Unit,
    onItemClick: (ItemDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        // Location name with coordinates
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
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

        // Creatures section
        Text(
            text = if (isBlinded) "Presences" else "Others",
            color = Color.Gray,
            fontSize = 10.sp
        )
        if (creaturesHere.isEmpty()) {
            Text(
                text = if (isBlinded) "You sense nothing nearby" else "None",
                color = Color.White.copy(alpha = if (isBlinded) 0.6f else 1f),
                fontSize = 14.sp
            )
        } else {
            creaturesHere.forEachIndexed { index, creature ->
                val state = creatureStates[creature.id] ?: "idle"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onCreatureClick(creature) }
                        .padding(vertical = 2.dp)
                ) {
                    if (!isBlinded) {
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

        Spacer(modifier = Modifier.height(8.dp))

        // Items section
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
            itemsHere.forEachIndexed { index, item ->
                Text(
                    text = if (isBlinded) getBlindItemDescription(item, index) else item.name,
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

// =============================================================================
// SHOP PANEL
// =============================================================================

@Composable
private fun ShopPanel(
    location: LocationDto,
    shopItems: List<ItemDto>,
    playerGold: Int,
    isInn: Boolean,
    innCost: Int,
    onBuyItem: (ItemDto) -> Unit,
    onRest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Shop name and gold display
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = location.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
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
            // Shop: item list
            shopItems.forEach { item ->
                ShopItemRow(
                    item = item,
                    canAfford = playerGold >= item.value,
                    onBuy = { onBuyItem(item) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ShopItemRow(
    item: ItemDto,
    canAfford: Boolean,
    onBuy: () -> Unit
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
            Text(
                text = "${item.value}g",
                color = if (canAfford) Color(0xFFFFD700) else Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =============================================================================
// MINIMAP
// =============================================================================

@Composable
private fun Minimap(
    locations: List<LocationDto>,
    currentLocation: LocationDto,
    isRanger: Boolean,
    modifier: Modifier = Modifier
) {
    val minimapScale = 0.75f
    val currentGridX = currentLocation.gridX ?: 0
    val currentGridY = currentLocation.gridY ?: 0

    // Animated position
    val animatedGridX by animateFloatAsState(
        targetValue = currentGridX.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "minimapX"
    )
    val animatedGridY by animateFloatAsState(
        targetValue = currentGridY.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "minimapY"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val gridSpacingPx = (35 * minimapScale).dp.toPx()
            val dotRadius = (8 * minimapScale).dp.toPx()
            val highlightRadius = (12 * minimapScale).dp.toPx()
            val lineColor = if (isRanger) Color(0xFF4CAF50) else Color(0xFFFF9800)
            val lineWidth = (3 * minimapScale).dp.toPx()
            val highlightStrokeWidth = (3 * minimapScale).dp.toPx()
            val dotOutlineWidth = (1.5f * minimapScale).dp.toPx()

            // Vignette function
            val vignetteRadius = minOf(size.width, size.height) / 2
            fun vignetteAlpha(x: Float, y: Float): Float {
                val dx = x - centerX
                val dy = y - centerY
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                val fadeStart = vignetteRadius * 0.6f
                val fadeEnd = vignetteRadius * 1.0f
                return when {
                    dist <= fadeStart -> 1f
                    dist >= fadeEnd -> 0f
                    else -> 1f - ((dist - fadeStart) / (fadeEnd - fadeStart))
                }
            }

            val locationById = locations.associateBy { it.id }
            val drawnConnections = mutableSetOf<Pair<String, String>>()
            val visited = mutableSetOf<String>()
            val queue = ArrayDeque<Pair<String, Int>>()

            queue.add(currentLocation.id to 0)
            visited.add(currentLocation.id)

            // Draw connections via BFS
            while (queue.isNotEmpty()) {
                val (locId, depth) = queue.removeFirst()
                if (depth >= 3) continue

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

                    if (kotlin.math.abs(locRelX) <= 3 && kotlin.math.abs(locRelY) <= 3 &&
                        kotlin.math.abs(targetRelX) <= 3 && kotlin.math.abs(targetRelY) <= 3 &&
                        !drawnConnections.contains(connKey)) {

                        drawnConnections.add(connKey)

                        val fromX = centerX + locRelX * gridSpacingPx
                        val fromY = centerY + locRelY * gridSpacingPx
                        val toX = centerX + targetRelX * gridSpacingPx
                        val toY = centerY + targetRelY * gridSpacingPx

                        val depthAlpha = when (depth) {
                            0 -> 0.9f
                            1 -> 0.6f
                            else -> 0.4f
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

            // Draw dots
            locations.forEach { location ->
                val locGridX = location.gridX ?: return@forEach
                val locGridY = location.gridY ?: return@forEach

                val relX = locGridX - animatedGridX
                val relY = locGridY - animatedGridY

                if (kotlin.math.abs(relX) <= 3 && kotlin.math.abs(relY) <= 3) {
                    val dotX = centerX + relX * gridSpacingPx
                    val dotY = centerY + relY * gridSpacingPx
                    val isCurrentLoc = location.id == currentLocation.id

                    val dotVignetteAlpha = if (isCurrentLoc) 1f else vignetteAlpha(dotX, dotY)

                    if (dotVignetteAlpha > 0.01f) {
                        if (isCurrentLoc) {
                            drawCircle(
                                color = lineColor,
                                radius = highlightRadius,
                                center = Offset(dotX, dotY),
                                style = Stroke(width = highlightStrokeWidth)
                            )
                        }

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

            // Fog of war
            val fogRadius = size.minDimension / 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.3f),
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.9f)
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
    isRanger: Boolean,
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
            .border(2.dp, if (isRanger) Color(0xFF4CAF50) else Color(0xFF4A4A4A), CircleShape)
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
                val lineColor = if (isRanger) Color(0xFF4CAF50) else Color(0xFFFF9800)
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

                val locationById = locations.associateBy { it.id }
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

                // Draw dots
                locations.forEach { location ->
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
                            if (isCurrentLoc) {
                                drawCircle(
                                    color = lineColor,
                                    radius = highlightRadius,
                                    center = Offset(dotX, dotY),
                                    style = Stroke(width = highlightStrokeWidth)
                                )
                            }

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
 */
@Composable
private fun DirectionalRing(
    exits: List<ExitDto>,
    locations: List<LocationDto>,
    onNavigate: (ExitDto) -> Unit
) {
    val ringRadius = 70.dp  // Closer to thumbnail than ability ring was
    val buttonSize = 28.dp

    Box(contentAlignment = Alignment.Center) {
        exits.forEach { exit ->
            val targetLocation = locations.find { it.id == exit.locationId }
            if (targetLocation != null && exit.direction != ExitDirection.UNKNOWN) {
                // Calculate position based on direction
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
                    ExitDirection.ENTER -> Pair(0.dp, ringRadius + 15.dp)  // Below south
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
                    ExitDirection.ENTER -> Icons.Filled.MeetingRoom
                    else -> Icons.Filled.ArrowUpward
                }

                var isPressed by remember { mutableStateOf(false) }
                val navScale by animateFloatAsState(
                    targetValue = if (isPressed) 1.2f else 1f,
                    animationSpec = tween(durationMillis = 100),
                    label = "dirScale"
                )

                val buttonColor = if (exit.direction == ExitDirection.ENTER) {
                    Color(0xFF9C27B0)  // Purple for portal
                } else {
                    Color(0xFF1976D2)  // Blue for directions
                }

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(buttonSize)
                        .scale(navScale)
                        .clip(CircleShape)
                        .background(buttonColor, CircleShape)
                        .pointerInput(targetLocation.id) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                    onNavigate(exit)
                                }
                            )
                        },
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
        }
    }
}

// =============================================================================
// ABILITY ROW (flat row above scroll section)
// =============================================================================

/**
 * Horizontal row of ability buttons displayed above the event log.
 * Non-creature-specific actions (self buffs, area effects, etc.)
 */
@Composable
private fun AbilityRow(
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int>,
    queuedAbilityId: String?,
    onAbilityClick: (AbilityDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconSize = 32.dp
    val maxIcons = 10

    // Filter to show non-passive abilities
    val displayAbilities = abilities.filter { it.abilityType != "passive" }.take(maxIcons)

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayAbilities.forEach { ability ->
            AbilityIconButton(
                ability = ability,
                cooldownRounds = cooldowns[ability.id] ?: 0,
                isQueued = ability.id == queuedAbilityId,
                onClick = { onAbilityClick(ability) },
                size = iconSize
            )
        }
    }
}

// =============================================================================
// LOCATION THUMBNAIL
// =============================================================================

@Composable
private fun LocationThumbnail(
    location: LocationDto,
    locations: List<LocationDto>,
    isBlinded: Boolean,
    blindRounds: Int,
    isRanger: Boolean,
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
        } else if (isRanger) {
            // Ranger's Directional Attunement: minimap overlay with location image behind
            if (location.imageUrl != null) {
                AsyncImage(
                    model = "${AppConfig.api.baseUrl}${location.imageUrl}",
                    contentDescription = location.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.35f },
                    contentScale = ContentScale.Crop
                )
            }
            // Minimap overlay for ranger
            RangerThumbnailMinimap(
                locations = locations,
                currentLocation = location
            )
        } else {
            // Normal location display
            if (location.imageUrl != null) {
                AsyncImage(
                    model = "${AppConfig.api.baseUrl}${location.imageUrl}",
                    contentDescription = location.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = location.name.take(2).uppercase(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RangerThumbnailMinimap(
    locations: List<LocationDto>,
    currentLocation: LocationDto
) {
    val currentGridX = currentLocation.gridX ?: 0
    val currentGridY = currentLocation.gridY ?: 0
    val animatedGridX by animateFloatAsState(
        targetValue = currentGridX.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "rangerMinimapX"
    )
    val animatedGridY by animateFloatAsState(
        targetValue = currentGridY.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "rangerMinimapY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val gridSpacingPx = 22.dp.toPx()
        val dotRadius = 5.dp.toPx()
        val highlightRadius = 7.dp.toPx()
        val lineColor = Color(0xFF4CAF50).copy(alpha = 0.8f)
        val lineWidth = 2.dp.toPx()
        val highlightStrokeWidth = 2.dp.toPx()
        val dotOutlineWidth = 1.dp.toPx()

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

        val locationById = locations.associateBy { it.id }
        val drawnConnections = mutableSetOf<Pair<String, String>>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<String, Int>>()

        queue.add(currentLocation.id to 0)
        visited.add(currentLocation.id)

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

        // Draw dots
        locations.forEach { location ->
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
                    if (isCurrentLoc) {
                        drawCircle(
                            color = Color(0xFF4CAF50),
                            radius = highlightRadius,
                            center = Offset(dotX, dotY),
                            style = Stroke(width = highlightStrokeWidth)
                        )
                    }

                    val terrainColor = getTerrainColor(location.desc, location.name)
                    drawCircle(
                        color = terrainColor.copy(alpha = terrainColor.alpha * dotVignetteAlpha),
                        radius = dotRadius,
                        center = Offset(dotX, dotY)
                    )

                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = 0.7f * dotVignetteAlpha),
                        radius = dotRadius,
                        center = Offset(dotX, dotY),
                        style = Stroke(width = dotOutlineWidth)
                    )
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

// =============================================================================
// MODE TOGGLE
// =============================================================================

@Composable
private fun ModeToggle(
    isCreateMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
    onAbilityClick: (AbilityDto) -> Unit
) {
    val detailName = creature?.name ?: item?.name ?: ""
    val detailImageUrl = creature?.imageUrl ?: item?.imageUrl

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
        if (creature != null && combatAbilities.isNotEmpty()) {
            CreatureCombatRing(
                abilities = combatAbilities,
                cooldowns = cooldowns,
                queuedAbilityId = queuedAbilityId,
                onAbilityClick = onAbilityClick
            )
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
            ActionButton(
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

/**
 * Combat ability ring displayed around the creature thumbnail on detail view.
 * Similar to the main location ability ring but specifically for targeting a creature.
 */
@Composable
private fun CreatureCombatRing(
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

@Composable
private fun ActionButton(
    offset: Offset,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
private fun DescriptionPopup(
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
private fun LocationDetailPopup(
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
                        val directionColor = if (exit.direction == ExitDirection.ENTER) {
                            Color(0xFF9C27B0)  // Purple for portals
                        } else {
                            Color(0xFF1976D2)  // Blue for normal directions
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
                onNavigate = {}
            )
        }
    }
}
