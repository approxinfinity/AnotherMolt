package com.ez2bg.anotherthread.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlin.math.cos
import kotlin.math.sin

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
    onViewLocationDetails: (LocationDto) -> Unit
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

    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (uiState.currentLocation != null) {
            val currentLocation = uiState.currentLocation!!

            // === TOP SECTION: Full-width black panel with info, minimap, and coordinates ===
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                // Location info on the left
                if (!uiState.isDetailViewVisible) {
                    LocationInfoPanel(
                        location = currentLocation,
                        creaturesHere = uiState.creaturesHere,
                        itemsHere = uiState.itemsHere,
                        creatureStates = uiState.creatureStates,
                        isBlinded = isBlinded,
                        onCreatureClick = { viewModel.selectCreature(it) },
                        onItemClick = { viewModel.selectItem(it) },
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .padding(16.dp)
                    )
                }

                // Minimap and coordinates on the right (floating over black background)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!uiState.isDetailViewVisible) {
                        Minimap(
                            locations = uiState.locations,
                            currentLocation = currentLocation,
                            isRanger = uiState.isRanger,
                            modifier = Modifier.size(105.dp)
                        )

                        // Coordinates below minimap
                        val gridX = currentLocation.gridX ?: 0
                        val gridY = currentLocation.gridY ?: 0
                        Text(
                            text = "($gridX, $gridY)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // === CENTER SECTION: Location thumbnail with ability ring ===
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

                // Container for thumbnail + abilities (rotates when disoriented)
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            if (isDisoriented) {
                                rotationZ = 180f
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Ability ring around thumbnail
                    if (uiState.playerAbilities.isNotEmpty()) {
                        AbilityRing(
                            abilities = uiState.playerAbilities,
                            cooldowns = cooldowns,
                            queuedAbilityId = queuedAbilityId,
                            onAbilityClick = { viewModel.handleAbilityClick(it) }
                        )
                    }

                    // Location thumbnail (with Ranger minimap overlay if applicable)
                    LocationThumbnail(
                        location = currentLocation,
                        locations = uiState.locations,
                        isBlinded = isBlinded,
                        blindRounds = blindRounds,
                        isRanger = uiState.isRanger,
                        onClick = { onViewLocationDetails(currentLocation) }
                    )
                }

                // Navigation arrows around the thumbnail
                NavigationArrows(
                    exits = currentLocation.exits,
                    locations = uiState.locations,
                    onNavigate = { viewModel.navigateToExit(it) }
                )
            }

            // === BOTTOM SECTION: Mode toggle and event log ===
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Mode toggle - bottom right of middle section
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
                    showDescriptionPopup = uiState.showDescriptionPopup,
                    offsetX = detailOffsetX,
                    snackbarHostState = snackbarHostState,
                    onBack = { viewModel.clearSelection() },
                    onShowDescription = { viewModel.showDescriptionPopup() },
                    onHideDescription = { viewModel.hideDescriptionPopup() }
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
        // Location name
        Text(
            text = location.name,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

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
// ABILITY RING
// =============================================================================

@Composable
private fun AbilityRing(
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
// NAVIGATION ARROWS
// =============================================================================

@Composable
private fun NavigationArrows(
    exits: List<ExitDto>,
    locations: List<LocationDto>,
    onNavigate: (ExitDto) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        exits.forEach { exit ->
            val targetLocation = locations.find { it.id == exit.locationId }
            if (targetLocation != null) {
                val (offsetX, offsetY, icon) = when (exit.direction) {
                    ExitDirection.NORTH -> Triple(0.dp, (-130).dp, Icons.Filled.ArrowUpward)
                    ExitDirection.SOUTH -> Triple(0.dp, 130.dp, Icons.Filled.ArrowDownward)
                    ExitDirection.EAST -> Triple(130.dp, 0.dp, Icons.AutoMirrored.Filled.ArrowForward)
                    ExitDirection.WEST -> Triple((-130).dp, 0.dp, Icons.AutoMirrored.Filled.ArrowBack)
                    ExitDirection.NORTHEAST -> Triple(92.dp, (-92).dp, Icons.Filled.NorthEast)
                    ExitDirection.NORTHWEST -> Triple((-92).dp, (-92).dp, Icons.Filled.NorthWest)
                    ExitDirection.SOUTHEAST -> Triple(92.dp, 92.dp, Icons.Filled.SouthEast)
                    ExitDirection.SOUTHWEST -> Triple((-92).dp, 92.dp, Icons.Filled.SouthWest)
                    ExitDirection.ENTER -> Triple(0.dp, 145.dp, Icons.Filled.MeetingRoom)
                    else -> Triple(0.dp, 0.dp, Icons.Filled.ArrowUpward)
                }

                if (exit.direction != ExitDirection.UNKNOWN) {
                    var isPressed by remember { mutableStateOf(false) }
                    val navScale by animateFloatAsState(
                        targetValue = if (isPressed) 1.3f else 1f,
                        animationSpec = tween(durationMillis = 150),
                        label = "navScale"
                    )
                    val glowAlpha by animateFloatAsState(
                        targetValue = if (isPressed) 0.8f else 0f,
                        animationSpec = tween(durationMillis = 150),
                        label = "navGlow"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glow ring
                        Box(
                            modifier = Modifier
                                .size((32 * navScale).dp)
                                .background(
                                    Color(0xFFFF9800).copy(alpha = glowAlpha),
                                    CircleShape
                                )
                        )
                        // Button
                        val buttonColor = if (exit.direction == ExitDirection.ENTER) {
                            Color(0xFF9C27B0) // Purple for portal
                        } else {
                            Color(0xFF1976D2) // Blue for directions
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                                contentDescription = if (exit.direction == ExitDirection.ENTER) "Enter area" else "Go ${exit.direction.name}",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
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
    showDescriptionPopup: Boolean,
    offsetX: Float,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onShowDescription: () -> Unit,
    onHideDescription: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val detailName = creature?.name ?: item?.name ?: ""
    val detailImageUrl = creature?.imageUrl ?: item?.imageUrl

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
                .offset(y = (-120).dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

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

        // Back button (WEST position)
        ActionButton(
            offset = Offset(-90f, 0f),
            color = Color(0xFFFF9800),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack
        )

        // Attack and Greet buttons for creatures
        if (creature != null) {
            // Attack button (EAST position)
            ActionButton(
                offset = Offset(90f, 0f),
                color = Color(0xFFD32F2F),
                icon = SwordIcon,
                contentDescription = "Attack",
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "lets fight!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )

            // Greet button (SOUTH position)
            ActionButton(
                offset = Offset(0f, 90f),
                color = Color(0xFF4CAF50),
                icon = Icons.Filled.Person,
                contentDescription = "Greet",
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "you say sup dude!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )

            // Creature abilities display
            val creatureAbilities = creature.abilityIds?.mapNotNull { allAbilitiesMap[it] } ?: emptyList()
            if (creatureAbilities.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .offset(y = (-160).dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Abilities:",
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
private fun NavigationArrowsPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.Black)
        ) {
            NavigationArrows(
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
