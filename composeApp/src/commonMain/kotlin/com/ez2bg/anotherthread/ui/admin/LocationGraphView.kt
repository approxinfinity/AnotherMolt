package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.combat.CombatClient
import com.ez2bg.anotherthread.combat.CombatEvent
import com.ez2bg.anotherthread.platform.currentTimeMillis
import com.ez2bg.anotherthread.ui.GameMode
import com.ez2bg.anotherthread.ui.admin.LocationGraph
import com.ez2bg.anotherthread.ui.components.EventLogEntry
import com.ez2bg.anotherthread.ui.components.EventType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationGraphView(
    refreshKey: Long,
    onAddClick: () -> Unit,
    onLocationClick: (LocationDto) -> Unit,
    isAuthenticated: Boolean,
    isAdmin: Boolean = false,
    currentUser: UserDto? = null,
    onLoginClick: () -> Unit = {},
    gameMode: GameMode = GameMode.CREATE,
    onGameModeChange: (GameMode) -> Unit = {}
) {
    var locations by remember(refreshKey) { mutableStateOf<List<LocationDto>>(emptyList()) }
    var isLoading by remember(refreshKey) { mutableStateOf(true) }
    var error by remember(refreshKey) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Area filtering state
    var areas by remember { mutableStateOf<List<AreaInfoDto>>(emptyList()) }
    var selectedAreaId by remember { mutableStateOf("overworld") }
    var areaDropdownExpanded by remember { mutableStateOf(false) }
    // Pending location to focus on after area switch (for cross-area navigation)
    var pendingFocusLocationId by remember { mutableStateOf<String?>(null) }

    // Terrain override state
    var terrainOverridesMap by remember(refreshKey) { mutableStateOf<Map<String, TerrainOverridesDto>>(emptyMap()) }
    var selectedLocationForSettings by remember { mutableStateOf<LocationDto?>(null) }

    // Re-fetch locations when refreshKey changes (e.g., after saving a location)
    // Use refreshKey as cache buster to bypass browser/CDN caching
    LaunchedEffect(refreshKey) {
        println("DEBUG: LocationGraphView LaunchedEffect triggered with refreshKey: $refreshKey")
        isLoading = true
        val result = ApiClient.getLocations(cacheBuster = refreshKey)
        isLoading = false
        result.onSuccess {
            println("DEBUG: Loaded ${it.size} locations")
            locations = it
        }.onFailure { error = it.message }
    }

    // Fetch world areas for dropdown
    LaunchedEffect(refreshKey) {
        ApiClient.getWorldAreas().onSuccess { areaList ->
            areas = areaList
            // If selectedAreaId isn't in the area list, pick a valid one
            val areaIds = areaList.map { it.areaId } + listOf("overworld")
            if (selectedAreaId !in areaIds) {
                selectedAreaId = "overworld"
            }
        }
    }

    // Filter locations by selected area
    val filteredLocations = remember(locations, selectedAreaId) {
        locations.filter { (it.areaId ?: "overworld") == selectedAreaId }
    }

    // Combat client state - persisted across recompositions
    var activeCombatClient by remember { mutableStateOf<CombatClient?>(null) }
    var combatSession by remember { mutableStateOf<CombatSessionDto?>(null) }
    var playerCombatant by remember { mutableStateOf<CombatantDto?>(null) }
    var combatCooldowns by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var combatQueuedAbilityId by remember { mutableStateOf<String?>(null) }

    // Visual effect states from combat (blind/disorient)
    var combatIsBlinded by remember { mutableStateOf(false) }
    var combatBlindRounds by remember { mutableStateOf(0) }
    var combatIsDisoriented by remember { mutableStateOf(false) }
    var combatDisorientRounds by remember { mutableStateOf(0) }

    // Death notification state
    var deathNotification by remember { mutableStateOf<PlayerDeathResponse?>(null) }

    // Event log state
    var eventLogEntries by remember { mutableStateOf<List<EventLogEntry>>(emptyList()) }

    // Helper to add event log entry
    fun addEventLog(message: String, type: EventType) {
        val entry = EventLogEntry(
            message = message,
            type = type
        )
        eventLogEntries = (eventLogEntries + entry).takeLast(50) // Keep last 50 entries
    }

    // WebSocket connection to listen for combat events (only in Adventure mode)
    LaunchedEffect(gameMode, currentUser?.id) {
        val userId = currentUser?.id
        if (gameMode.isAdventure && userId != null) {
            println("DEBUG: Connecting to combat WebSocket for creature updates")
            try {
                val combatClient = CombatClient(userId)
                activeCombatClient = combatClient
                combatClient.connect()

                try {
                    combatClient.events.collect { event ->
                        when (event) {
                            is CombatEvent.CreatureMoved -> {
                                println("DEBUG: Creature ${event.creatureName} moved from ${event.fromLocationId} to ${event.toLocationId}")
                                addEventLog("${event.creatureName} wandered away", EventType.MOVEMENT)
                                // Refresh locations to pick up creature movement
                                val result = ApiClient.getLocations(cacheBuster = currentTimeMillis())
                                result.onSuccess {
                                    println("DEBUG: Refreshed locations after creature move, got ${it.size} locations")
                                    locations = it
                                }
                            }
                            is CombatEvent.CombatStarted -> {
                                println("DEBUG: Combat started - session ${event.session.id}")
                                combatSession = event.session
                                playerCombatant = event.yourCombatant
                                val enemyNames = event.session.combatants
                                    .filter { it.type == CombatantType.CREATURE }
                                    .joinToString(", ") { it.name }
                                addEventLog("Combat started vs $enemyNames!", EventType.COMBAT)
                            }
                            is CombatEvent.RoundStarted -> {
                                println("DEBUG: Round ${event.roundNumber} started")
                                combatQueuedAbilityId = null  // Clear queued ability for new round
                                // Update combatants list and cooldowns
                                val myCombatant = event.combatants.find { it.id == userId }
                                if (myCombatant != null) {
                                    playerCombatant = myCombatant
                                    combatCooldowns = myCombatant.cooldowns
                                }
                                addEventLog("Round ${event.roundNumber}", EventType.INFO)
                            }
                            is CombatEvent.AbilityQueued -> {
                                println("DEBUG: Ability ${event.abilityId} queued")
                                combatQueuedAbilityId = event.abilityId
                            }
                            is CombatEvent.AbilityResolved -> {
                                println("DEBUG: Ability resolved - ${event.result.abilityName}")
                                val r = event.result
                                val msg = if (r.result.damage > 0) {
                                    "${r.actorName} hits ${r.targetName ?: "target"} with ${r.abilityName} for ${r.result.damage} damage"
                                } else if (r.result.healing > 0) {
                                    "${r.actorName} heals ${r.targetName ?: "self"} for ${r.result.healing} HP"
                                } else {
                                    "${r.actorName} uses ${r.abilityName}"
                                }
                                val eventType = when {
                                    r.result.damage > 0 -> EventType.DAMAGE
                                    r.result.healing > 0 -> EventType.HEALING
                                    else -> EventType.INFO
                                }
                                addEventLog(msg, eventType)
                            }
                            is CombatEvent.StatusEffectChanged -> {
                                val response = event.effect
                                val effectDto = response.effect
                                println("DEBUG: Status effect changed - ${effectDto.effectType} on ${response.combatantId}, applied=${response.applied}, rounds=${effectDto.remainingRounds}")
                                // Check if this affects the player
                                if (response.combatantId == userId) {
                                    when (effectDto.effectType) {
                                        "blind" -> {
                                            combatIsBlinded = response.applied
                                            combatBlindRounds = effectDto.remainingRounds
                                        }
                                        "disorient" -> {
                                            combatIsDisoriented = response.applied
                                            combatDisorientRounds = effectDto.remainingRounds
                                        }
                                    }
                                }
                                val action = if (response.applied) "applied" else "expired"
                                val effectType = if (effectDto.effectType in listOf("buff", "hot", "heal")) {
                                    EventType.BUFF
                                } else {
                                    EventType.DEBUFF
                                }
                                addEventLog("${effectDto.name} $action", effectType)
                            }
                            is CombatEvent.RoundEnded -> {
                                println("DEBUG: Round ${event.roundNumber} ended")
                                val myCombatant = event.combatants.find { it.id == userId }
                                if (myCombatant != null) {
                                    playerCombatant = myCombatant
                                    // Update cooldowns from combatant data
                                    combatCooldowns = myCombatant.cooldowns
                                    println("DEBUG: Updated cooldowns: ${myCombatant.cooldowns}")
                                }
                                // Decrement visual effect rounds
                                if (combatBlindRounds > 0) {
                                    combatBlindRounds--
                                    if (combatBlindRounds <= 0) combatIsBlinded = false
                                }
                                if (combatDisorientRounds > 0) {
                                    combatDisorientRounds--
                                    if (combatDisorientRounds <= 0) combatIsDisoriented = false
                                }
                            }
                            is CombatEvent.CombatEnded -> {
                                println("DEBUG: Combat ended")
                                combatSession = null
                                playerCombatant = null
                                combatQueuedAbilityId = null
                                combatCooldowns = emptyMap()
                                combatIsBlinded = false
                                combatBlindRounds = 0
                                combatIsDisoriented = false
                                combatDisorientRounds = 0
                                val r = event.response
                                val msg = when (r.reason) {
                                    CombatEndReason.ALL_ENEMIES_DEFEATED -> "Victory! +${r.experienceGained} XP"
                                    CombatEndReason.ALL_PLAYERS_DEFEATED -> "Defeated..."
                                    CombatEndReason.ALL_PLAYERS_FLED -> "Escaped!"
                                    else -> "Combat ended"
                                }
                                addEventLog(msg, EventType.COMBAT)
                                if (r.loot.goldEarned > 0) {
                                    addEventLog("Found ${r.loot.goldEarned} gold!", EventType.LOOT)
                                }
                                if (r.loot.itemNames.isNotEmpty()) {
                                    addEventLog("Looted: ${r.loot.itemNames.joinToString()}", EventType.LOOT)
                                }
                            }
                            is CombatEvent.PlayerDied -> {
                                println("DEBUG: Player died - respawning at ${event.response.respawnLocationName}")
                                combatSession = null
                                playerCombatant = null
                                combatQueuedAbilityId = null
                                combatCooldowns = emptyMap()
                                combatIsBlinded = false
                                combatBlindRounds = 0
                                combatIsDisoriented = false
                                combatDisorientRounds = 0
                                deathNotification = event.response
                                addEventLog("You died! Dropped ${event.response.itemsDropped} items and ${event.response.goldLost} gold", EventType.DEATH)
                            }
                            is CombatEvent.Error -> {
                                println("DEBUG: Combat error - ${event.message}")
                                addEventLog(event.message, EventType.INFO)
                            }
                            is CombatEvent.ResourceUpdated -> {
                                // Resource updates are handled silently for now
                                // Could add messages like "Spent 5 mana" if desired
                            }
                            else -> { /* Ignore other events */ }
                        }
                    }
                } finally {
                    println("DEBUG: Disconnecting combat WebSocket")
                    combatClient.disconnect()
                    activeCombatClient = null
                }
            } catch (e: Exception) {
                println("DEBUG: WebSocket error: ${e.message}")
                // Don't crash the UI if WebSocket fails
            }
        }
    }

    // Fetch terrain overrides for all locations (lazy-loaded when locations change)
    LaunchedEffect(locations) {
        if (isAdmin && locations.isNotEmpty()) {
            locations.forEach { location ->
                scope.launch {
                    ApiClient.getTerrainOverrides(location.id)
                        .onSuccess { override ->
                            terrainOverridesMap = terrainOverridesMap + (location.id to override.overrides)
                        }
                }
            }
        }
    }

    // Death notification dialog
    deathNotification?.let { death ->
        AlertDialog(
            onDismissRequest = { deathNotification = null },
            title = { Text("You Died!") },
            text = {
                Column {
                    Text("You have been defeated and respawned at ${death.respawnLocationName}.")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (death.itemsDropped > 0 || death.goldLost > 0) {
                        Text("You dropped:", fontWeight = FontWeight.Bold)
                        if (death.itemsDropped > 0) {
                            Text("• ${death.itemsDropped} item(s)")
                        }
                        if (death.goldLost > 0) {
                            Text("• ${death.goldLost} gold")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        death.deathLocationName?.let { locName ->
                            Text("Your items are at: $locName", fontStyle = FontStyle.Italic)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { deathNotification = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Terrain settings dialog
    selectedLocationForSettings?.let { location ->
        TerrainSettingsDialog(
            location = location,
            currentOverrides = terrainOverridesMap[location.id],
            onDismiss = { selectedLocationForSettings = null },
            onSave = { overrides ->
                scope.launch {
                    ApiClient.updateTerrainOverrides(location.id, overrides)
                        .onSuccess {
                            terrainOverridesMap = terrainOverridesMap + (location.id to overrides)
                            selectedLocationForSettings = null
                        }
                }
            },
            onReset = {
                scope.launch {
                    ApiClient.resetTerrainOverrides(location.id)
                        .onSuccess {
                            terrainOverridesMap = terrainOverridesMap - location.id
                            selectedLocationForSettings = null
                        }
                }
            },
            currentUser = currentUser
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SelectionContainer {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            error = null
                            val result = ApiClient.getLocations()
                            isLoading = false
                            result.onSuccess { locations = it }
                                .onFailure { error = it.message }
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
            locations.isEmpty() -> {
                Text(
                    text = "No locations yet. Tap + to create one.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LocationGraph(
                    locations = filteredLocations,
                    allLocations = locations,
                    selectedAreaId = selectedAreaId,
                    onAreaNavigate = { areaId, locationId ->
                        selectedAreaId = areaId
                        pendingFocusLocationId = locationId
                    },
                    pendingFocusLocationId = pendingFocusLocationId,
                    onPendingFocusConsumed = { pendingFocusLocationId = null },
                    onLocationClick = onLocationClick,
                    modifier = Modifier.fillMaxSize(),
                    isAdmin = isAdmin,
                    terrainOverridesMap = terrainOverridesMap,
                    onSettingsClick = { location ->
                        selectedLocationForSettings = location
                    },
                    currentUser = currentUser,
                    gameMode = gameMode,
                    onGameModeChange = onGameModeChange,
                    // Pass combat state to LocationGraph
                    activeCombatClient = activeCombatClient,
                    combatCooldowns = combatCooldowns,
                    combatQueuedAbilityId = combatQueuedAbilityId,
                    combatIsBlinded = combatIsBlinded,
                    combatBlindRounds = combatBlindRounds,
                    combatIsDisoriented = combatIsDisoriented,
                    combatDisorientRounds = combatDisorientRounds,
                    eventLogEntries = eventLogEntries
                )
            }
        }

        // Area dropdown selector (CREATE mode only, when multiple areas exist)
        if (gameMode.isCreate && areas.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Surface(
                    modifier = Modifier.clickable { areaDropdownExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedAreaId,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (areaDropdownExpanded) "\u25B2" else "\u25BC",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DropdownMenu(
                    expanded = areaDropdownExpanded,
                    onDismissRequest = { areaDropdownExpanded = false }
                ) {
                    val allAreaIds = (listOf("overworld") + areas.map { it.areaId }).distinct()
                    allAreaIds.forEach { areaId ->
                        val info = areas.find { it.areaId == areaId }
                        val count = info?.locationCount ?: locations.count { (it.areaId ?: "overworld") == areaId }
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(areaId)
                                    Text(
                                        text = "$count locations",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedAreaId = areaId
                                areaDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Hide FAB and login prompt in exploration mode
        if (gameMode.isCreate) {
            if (isAuthenticated) {
                FloatingActionButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Location")
                }
            } else {
                // "Adventure awaits!" message for unauthenticated users - clickable to navigate to auth
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onLoginClick)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Adventure awaits!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
