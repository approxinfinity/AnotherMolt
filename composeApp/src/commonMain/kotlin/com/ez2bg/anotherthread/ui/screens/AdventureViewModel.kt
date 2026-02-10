package com.ez2bg.anotherthread.ui.screens

import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CharacterClassDto
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.ExitDto
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.ShopActionResponse
import com.ez2bg.anotherthread.api.TeleportDestinationDto
import com.ez2bg.anotherthread.api.PhasewalkDestinationDto
import com.ez2bg.anotherthread.api.UnconnectedAreaDto
import com.ez2bg.anotherthread.api.SealableRiftDto
import com.ez2bg.anotherthread.api.TrainerInfoResponse
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.api.PuzzleDto
import com.ez2bg.anotherthread.api.PuzzleProgressResponse
import com.ez2bg.anotherthread.api.FishingInfoDto
import com.ez2bg.anotherthread.api.FishingMinigameStartDto
import com.ez2bg.anotherthread.api.FishBehaviorDto
import com.ez2bg.anotherthread.data.AdventureRepository
import com.ez2bg.anotherthread.state.AdventureStateHolder
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.EventLogType
import com.ez2bg.anotherthread.state.PlayerPresenceEvent
import com.ez2bg.anotherthread.state.UserStateHolder
import com.ez2bg.anotherthread.combat.CombatConnectionState
import com.ez2bg.anotherthread.platform.currentTimeMillis
import com.ez2bg.anotherthread.ui.components.EventLogEntry
import com.ez2bg.anotherthread.ui.components.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI-only state that doesn't come from the repository.
 * This contains selection state, targeting state, etc.
 */
data class AdventureLocalState(
    val selectedCreature: CreatureDto? = null,
    val selectedItem: ItemDto? = null,
    val showDescriptionPopup: Boolean = false,
    val pendingAbility: AbilityDto? = null,
    val snackbarMessage: String? = null,
    val playerAbilities: List<AbilityDto> = emptyList(),
    val abilitiesLoading: Boolean = true,  // Start as loading until first load completes
    val playerCharacterClass: CharacterClassDto? = null,
    val allAbilitiesMap: Map<String, AbilityDto> = emptyMap(),
    val isLocationSynced: Boolean = false,  // Whether server location has been confirmed
    // Shop state
    val playerGold: Int = 0,
    val shopItems: List<ItemDto> = emptyList(),
    val isShopLocation: Boolean = false,
    val isInnLocation: Boolean = false,
    val isGeneralStore: Boolean = false,
    val sellableItems: List<com.ez2bg.anotherthread.api.SellableItemDto> = emptyList(),
    val showSellModal: Boolean = false,
    val isShopBanned: Boolean = false,
    val shopBanMessage: String? = null,
    // Teleport state
    val showMapSelection: Boolean = false,
    val teleportDestinations: List<TeleportDestinationDto> = emptyList(),
    val teleportAbilityId: String? = null,
    // Phasewalk state (local state tracks ability + destinations)
    val hasPhasewalkAbility: Boolean = false,
    val phasewalkDestinations: List<PhasewalkDestinationDto> = emptyList(),
    // Rift state
    val showRiftSelection: Boolean = false,
    val riftMode: String? = null,  // "open" or "seal"
    val unconnectedAreas: List<UnconnectedAreaDto> = emptyList(),
    val sealableRifts: List<SealableRiftDto> = emptyList(),
    // Trainer state
    val showTrainerModal: Boolean = false,
    val trainerInfo: TrainerInfoResponse? = null,
    val isLoadingTrainer: Boolean = false,
    // Other players at this location (excludes self)
    val playersHere: List<UserDto> = emptyList(),
    // Player interaction state
    val selectedPlayer: UserDto? = null,
    val showPlayerInteractionModal: Boolean = false,
    val showGiveItemModal: Boolean = false,
    // Creature interaction state
    val showCreatureInteractionModal: Boolean = false,
    // Puzzle state
    val showPuzzleModal: Boolean = false,
    val currentPuzzle: PuzzleDto? = null,
    val puzzleProgress: PuzzleProgressResponse? = null,
    val isLoadingPuzzle: Boolean = false,
    val puzzlesAtLocation: List<PuzzleDto> = emptyList(),
    // Charm state
    val showCharmTargetSelection: Boolean = false,
    val charmableCreatures: List<CreatureDto> = emptyList(),
    // Hide item state
    val showHideItemModal: Boolean = false,
    val hideableItems: List<ItemDto> = emptyList(),
    // Search state
    val isSearching: Boolean = false,
    val searchDurationMs: Long = 0,
    // Fishing state
    val isFishing: Boolean = false,
    val fishingDurationMs: Long = 0,
    val showFishingDistanceModal: Boolean = false,
    val fishingInfo: FishingInfoDto? = null,
    // Fishing minigame state
    val showFishingMinigame: Boolean = false,
    val fishingMinigameData: FishingMinigameStartDto? = null
)

/**
 * Complete UI state for AdventureScreen.
 * Combines repository state with local UI state.
 */
data class AdventureUiState(
    // Loading state (from repository)
    val isLoading: Boolean = true,
    // Whether we've synced the player's location from the server
    val isLocationSynced: Boolean = false,

    // Location data (from repository, reactive)
    val locations: List<LocationDto> = emptyList(),
    val currentLocationId: String? = null,

    // Creature data (from repository, reactive - updated by WebSocket!)
    val allCreatures: List<CreatureDto> = emptyList(),
    val creatureLocations: Map<String, String> = emptyMap(),
    val creatureStates: Map<String, String> = emptyMap(),

    // Item data (from repository)
    val allItems: List<ItemDto> = emptyList(),

    // Player abilities (loaded separately)
    val allAbilitiesMap: Map<String, AbilityDto> = emptyMap(),
    val playerAbilities: List<AbilityDto> = emptyList(),
    val abilitiesLoading: Boolean = true,  // True until abilities are first loaded
    val playerCharacterClass: CharacterClassDto? = null,

    // Local UI state
    val selectedCreature: CreatureDto? = null,
    val selectedItem: ItemDto? = null,
    val showDescriptionPopup: Boolean = false,
    val pendingAbility: AbilityDto? = null,
    val snackbarMessage: String? = null,
    // Shop state
    val playerGold: Int = 0,
    val shopItems: List<ItemDto> = emptyList(),
    val isShopLocation: Boolean = false,
    val isInnLocation: Boolean = false,
    val isGeneralStore: Boolean = false,
    val sellableItems: List<com.ez2bg.anotherthread.api.SellableItemDto> = emptyList(),
    val showSellModal: Boolean = false,
    val isShopBanned: Boolean = false,
    val shopBanMessage: String? = null,
    // Teleport state
    val showMapSelection: Boolean = false,
    val teleportDestinations: List<TeleportDestinationDto> = emptyList(),
    val teleportAbilityId: String? = null,
    // Phasewalk state
    val hasPhasewalkAbility: Boolean = false,
    val phasewalkDestinations: List<PhasewalkDestinationDto> = emptyList(),
    // Rift state
    val showRiftSelection: Boolean = false,
    val riftMode: String? = null,  // "open" or "seal"
    val unconnectedAreas: List<UnconnectedAreaDto> = emptyList(),
    val sealableRifts: List<SealableRiftDto> = emptyList(),
    // Trainer state
    val showTrainerModal: Boolean = false,
    val trainerInfo: TrainerInfoResponse? = null,
    val isLoadingTrainer: Boolean = false,
    // Other players at this location (excludes self)
    val playersHere: List<UserDto> = emptyList(),
    // Player interaction state
    val selectedPlayer: UserDto? = null,
    val showPlayerInteractionModal: Boolean = false,
    val showGiveItemModal: Boolean = false,
    // Creature interaction state
    val showCreatureInteractionModal: Boolean = false,
    // Puzzle state
    val showPuzzleModal: Boolean = false,
    val currentPuzzle: PuzzleDto? = null,
    val puzzleProgress: PuzzleProgressResponse? = null,
    val isLoadingPuzzle: Boolean = false,
    val puzzlesAtLocation: List<PuzzleDto> = emptyList(),
    // Charm state
    val showCharmTargetSelection: Boolean = false,
    val charmableCreatures: List<CreatureDto> = emptyList(),
    // Hide item state
    val showHideItemModal: Boolean = false,
    val hideableItems: List<ItemDto> = emptyList(),
    // Search state
    val isSearching: Boolean = false,
    val searchDurationMs: Long = 0,
    // Fishing state
    val isFishing: Boolean = false,
    val fishingDurationMs: Long = 0,
    val showFishingDistanceModal: Boolean = false,
    val fishingInfo: FishingInfoDto? = null,
    // Fishing minigame state
    val showFishingMinigame: Boolean = false,
    val fishingMinigameData: FishingMinigameStartDto? = null
) {
    // Derived properties
    val currentLocation: LocationDto?
        get() = locations.find { it.id == currentLocationId }

    val isRanger: Boolean
        get() = playerCharacterClass?.name == "Ranger"

    val isDetailViewVisible: Boolean
        get() = selectedItem != null  // Only items use the detail view; creatures use the interaction modal

    /**
     * Creatures at the current location.
     * This is derived from creatureLocations which is updated in real-time
     * by WebSocket events via AdventureRepository.
     */
    val creaturesHere: List<CreatureDto>
        get() {
            val locId = currentLocationId ?: return emptyList()
            val creatureIdsHere = creatureLocations
                .filter { it.value == locId }
                .keys
            return allCreatures.filter { it.id in creatureIdsHere }
        }

    /**
     * Items at the current location.
     */
    val itemsHere: List<ItemDto>
        get() = currentLocation?.itemIds?.let { itemIds ->
            allItems.filter { it.id in itemIds }
        } ?: emptyList()

    /**
     * Whether the player is over-encumbered (>100% carry capacity).
     * When true, navigation should be blocked and exits greyed out.
     */
    val isOverEncumbered: Boolean
        get() {
            val user = UserStateHolder.currentUser.value ?: return false
            val maxCapacity = user.strength * 5  // Max capacity in stone
            val itemsMap = allItems.associateBy { it.id }
            val totalWeight = user.itemIds.sumOf { itemId -> itemsMap[itemId]?.weight ?: 0 }
            return totalWeight > maxCapacity
        }
}

/**
 * ViewModel for AdventureScreen.
 *
 * This ViewModel:
 * - Collects reactive state from AdventureRepository
 * - Repository subscribes to WebSocket events for real-time updates
 * - UI automatically reacts when creatures move via WebSocket
 * - Uses UserStateHolder for reactive user state (not a static snapshot)
 */
class AdventureViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Track last navigation time to prevent sync from overwriting recent navigations
    private var lastNavigationTime: Long = 0L
    private val NAVIGATION_SYNC_COOLDOWN_MS = 5000L  // Don't sync location within 5s of navigation

    // Known shop location IDs (defined before uiState so they can be used in initialValue)
    private val shopLocationIds = setOf(
        "tun-du-lac-magic-shop",
        "tun-du-lac-armor-shop",
        "tun-du-lac-weapons-shop",
        "tun-du-lac-general-store",
        "location-hermits-hollow"
    )
    private val innLocationId = "tun-du-lac-inn"
    private val generalStoreLocationId = "tun-du-lac-general-store"

    // Get current user reactively from UserStateHolder
    private val currentUser: UserDto?
        get() = UserStateHolder.currentUser.value

    // Local UI state (selections, targeting, etc.)
    // Initialize with shop/inn detection based on current location
    private val _localState = MutableStateFlow(
        AdventureLocalState(
            isShopLocation = UserStateHolder.currentLocationId in shopLocationIds,
            isInnLocation = UserStateHolder.currentLocationId == innLocationId,
            isGeneralStore = UserStateHolder.currentLocationId == generalStoreLocationId
        )
    )

    /**
     * Combined UI state that merges repository state with local state.
     * This flow updates automatically when:
     * - Repository state changes (including WebSocket-triggered creature movements)
     * - Local UI state changes (selections, targeting)
     */
    val uiState: StateFlow<AdventureUiState> = combine(
        AdventureRepository.locations,
        AdventureRepository.currentLocationId,
        AdventureRepository.creatures,
        AdventureRepository.creatureLocations,  // This is the key reactive flow!
        AdventureRepository.creatureStates,
        AdventureRepository.items,
        AdventureRepository.isLoading,
        _localState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val locations = values[0] as List<LocationDto>
        val currentLocationId = values[1] as String?
        val creatures = values[2] as List<CreatureDto>
        val creatureLocations = values[3] as Map<String, String>
        val creatureStates = values[4] as Map<String, String>
        val items = values[5] as List<ItemDto>
        val isLoading = values[6] as Boolean
        val local = values[7] as AdventureLocalState

        AdventureUiState(
            isLoading = isLoading,
            isLocationSynced = local.isLocationSynced,
            locations = locations,
            currentLocationId = currentLocationId,
            allCreatures = creatures,
            creatureLocations = creatureLocations,
            creatureStates = creatureStates,
            allItems = items,
            allAbilitiesMap = local.allAbilitiesMap,
            playerAbilities = local.playerAbilities,
            abilitiesLoading = local.abilitiesLoading,
            playerCharacterClass = local.playerCharacterClass,
            selectedCreature = local.selectedCreature,
            selectedItem = local.selectedItem,
            showDescriptionPopup = local.showDescriptionPopup,
            pendingAbility = local.pendingAbility,
            snackbarMessage = local.snackbarMessage,
            playerGold = local.playerGold,
            shopItems = local.shopItems,
            isShopLocation = local.isShopLocation,
            isInnLocation = local.isInnLocation,
            isGeneralStore = local.isGeneralStore,
            sellableItems = local.sellableItems,
            showSellModal = local.showSellModal,
            isShopBanned = local.isShopBanned,
            shopBanMessage = local.shopBanMessage,
            showMapSelection = local.showMapSelection,
            teleportDestinations = local.teleportDestinations,
            teleportAbilityId = local.teleportAbilityId,
            hasPhasewalkAbility = local.hasPhasewalkAbility,
            phasewalkDestinations = local.phasewalkDestinations,
            showRiftSelection = local.showRiftSelection,
            riftMode = local.riftMode,
            unconnectedAreas = local.unconnectedAreas,
            sealableRifts = local.sealableRifts,
            showTrainerModal = local.showTrainerModal,
            trainerInfo = local.trainerInfo,
            isLoadingTrainer = local.isLoadingTrainer,
            playersHere = local.playersHere,
            selectedPlayer = local.selectedPlayer,
            showPlayerInteractionModal = local.showPlayerInteractionModal,
            showGiveItemModal = local.showGiveItemModal,
            showCreatureInteractionModal = local.showCreatureInteractionModal,
            showPuzzleModal = local.showPuzzleModal,
            currentPuzzle = local.currentPuzzle,
            puzzleProgress = local.puzzleProgress,
            isLoadingPuzzle = local.isLoadingPuzzle,
            puzzlesAtLocation = local.puzzlesAtLocation,
            showCharmTargetSelection = local.showCharmTargetSelection,
            charmableCreatures = local.charmableCreatures,
            showHideItemModal = local.showHideItemModal,
            hideableItems = local.hideableItems,
            isSearching = local.isSearching,
            searchDurationMs = local.searchDurationMs,
            isFishing = local.isFishing,
            fishingDurationMs = local.fishingDurationMs,
            showFishingDistanceModal = local.showFishingDistanceModal,
            fishingInfo = local.fishingInfo,
            showFishingMinigame = local.showFishingMinigame,
            fishingMinigameData = local.fishingMinigameData
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdventureUiState(
            currentLocationId = UserStateHolder.currentLocationId,
            isShopLocation = UserStateHolder.currentLocationId in shopLocationIds,
            isInnLocation = UserStateHolder.currentLocationId == innLocationId,
            isGeneralStore = UserStateHolder.currentLocationId == generalStoreLocationId
        )
    )

    init {
        initializeRepository()
        connectCombatWebSocket()
        loadAbilitiesMap()
        // Load shop items if starting at a shop location
        val initialLocationId = UserStateHolder.currentLocationId
        if (initialLocationId in shopLocationIds) {
            loadShopItemsFromApi(initialLocationId ?: "")
        }
        // Listen for user state changes - this handles ALL user-dependent state reactively
        // including: gold, abilities (class/learned/equipped/visible), character class
        listenForUserUpdates()
        // Listen for real-time player presence events (enter/leave)
        listenForPlayerPresenceEvents()
        // Listen for WebSocket reconnection to resync location
        listenForConnectionStateChanges()
    }

    /**
     * Listen for changes to the current user and sync all relevant state.
     * Tracks: gold, learnedAbilityIds, visibleAbilityIds, equippedItemIds, characterClassId, currentLocationId
     * This ensures all user-dependent UI stays in sync after any user data changes.
     *
     * On first emission: loads initial state
     * On subsequent emissions: only reloads what changed
     *
     * IMPORTANT: Also watches for currentLocationId changes from session restore.
     * When the session is validated with the server, the user's actual location
     * may differ from what was cached locally. This ensures we navigate to the
     * correct location after server validation.
     */
    private fun listenForUserUpdates() {
        scope.launch {
            var previousLearnedAbilityIds: List<String>? = null
            var previousVisibleAbilityIds: List<String>? = null
            var previousEquippedItemIds: List<String>? = null
            var previousCharacterClassId: String? = null
            var previousGold: Int? = null
            var previousLocationId: String? = null
            var isFirstEmission = true
            var hasPerformedInitialLocationSync = false

            UserStateHolder.currentUser.collect { user ->
                if (user != null) {
                    val currentLearnedIds = user.learnedAbilityIds
                    val currentVisibleIds = user.visibleAbilityIds
                    val currentEquippedIds = user.equippedItemIds
                    val currentClassId = user.characterClassId
                    val currentGold = user.gold
                    val currentLocationId = user.currentLocationId

                    println("[AdventureViewModel] User emission: isFirst=$isFirstEmission, hasPerformedSync=$hasPerformedInitialLocationSync, locationId=$currentLocationId")

                    // Ensure WebSocket is connected (may have failed at init if user was null)
                    if (isFirstEmission) {
                        connectCombatWebSocket()
                    }

                    // On first emission, load everything
                    // On subsequent emissions, only reload if relevant properties changed
                    val learnedChanged = previousLearnedAbilityIds != currentLearnedIds
                    val visibleChanged = previousVisibleAbilityIds != currentVisibleIds
                    val equippedChanged = previousEquippedItemIds != currentEquippedIds
                    val classChanged = previousCharacterClassId != currentClassId
                    val goldChanged = previousGold != currentGold

                    // Reload abilities if first load or any ability-related property changed
                    if (isFirstEmission || learnedChanged || visibleChanged || equippedChanged || classChanged) {
                        if (!isFirstEmission) {
                            println("[AdventureViewModel] User abilities changed (learned=$learnedChanged, visible=$visibleChanged, equipped=$equippedChanged, class=$classChanged), reloading abilities")
                        }
                        loadPlayerAbilities()
                    }

                    // Sync gold to local state on first load or when it changes
                    if (isFirstEmission || goldChanged) {
                        _localState.update { it.copy(playerGold = currentGold) }
                    }

                    // ONE-TIME location sync after server validation
                    // The first emission is from cached data (AuthStorage) which may be stale.
                    // The second emission is after validateSession() updates with fresh server data.
                    // We sync the location ONLY from the second emission (after server validation).
                    // This ensures we use the server's authoritative location, not a cached one.
                    // TODO: Explore re-syncing location on WebSocket reconnection as well,
                    // in case the user's location drifts during a disconnect/reconnect cycle.
                    if (!hasPerformedInitialLocationSync && currentLocationId != null && !isFirstEmission) {
                        hasPerformedInitialLocationSync = true
                        println("[AdventureViewModel] Performing initial location sync from server: $currentLocationId")

                        // Now we have fresh server data - sync to the server's location
                        val serverLocationId = currentLocationId // capture for lambda
                        scope.launch {
                            // Wait for repository to initialize
                            AdventureRepository.isInitialized.first { it }

                            // Skip sync if we recently navigated - client location is authoritative
                            val timeSinceLastNav = currentTimeMillis() - lastNavigationTime
                            if (timeSinceLastNav < NAVIGATION_SYNC_COOLDOWN_MS) {
                                println("[AdventureViewModel] Initial location sync: skipping, navigated ${timeSinceLastNav}ms ago")
                                _localState.update { it.copy(isLocationSynced = true) }
                                return@launch
                            }

                            val currentRepoLocationId = AdventureRepository.currentLocationId.value
                            println("[AdventureViewModel] Server location sync check: server=$serverLocationId, repo=$currentRepoLocationId")
                            if (currentRepoLocationId != null && currentRepoLocationId != serverLocationId) {
                                println("[AdventureViewModel] Server location sync: correcting repo from $currentRepoLocationId to $serverLocationId")
                                AdventureRepository.setCurrentLocation(serverLocationId)
                            } else {
                                println("[AdventureViewModel] Server location sync: no correction needed")
                            }
                            // Mark location as synced - UI can now show the location
                            _localState.update { it.copy(isLocationSynced = true) }
                        }
                    }

                    // Update previous values for next comparison
                    previousLearnedAbilityIds = currentLearnedIds
                    previousVisibleAbilityIds = currentVisibleIds
                    previousEquippedItemIds = currentEquippedIds
                    previousCharacterClassId = currentClassId
                    previousGold = currentGold
                    previousLocationId = currentLocationId
                    isFirstEmission = false
                }
            }
        }
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    private fun initializeRepository() {
        // Initialize the repository with the user's current location
        // The repository will:
        // 1. Load all world data from the server
        // 2. Subscribe to WebSocket events for real-time updates
        // 3. Fall back to (0,0) origin if location is invalid
        val cachedLocationId = UserStateHolder.currentLocationId
        println("[AdventureViewModel] initializeRepository() - UserStateHolder.currentLocationId=$cachedLocationId")
        AdventureRepository.initialize(cachedLocationId)

        // Sync with AdventureStateHolder for event log filtering
        // Also update server if location was changed due to fallback
        scope.launch {
            var firstLocationUpdate = true
            var initialUserLocationId = UserStateHolder.currentLocationId
            AdventureRepository.currentLocationId.collect { locationId ->
                if (locationId != null) {
                    val location = AdventureRepository.getLocation(locationId)
                    location?.let { AdventureStateHolder.setCurrentLocationDirect(it) }

                    // Check if this is a shop or inn location
                    val isShop = locationId in shopLocationIds
                    val isInn = locationId == innLocationId
                    val isGenStore = locationId == generalStoreLocationId
                    _localState.update {
                        it.copy(
                            isShopLocation = isShop,
                            isInnLocation = isInn,
                            isGeneralStore = isGenStore,
                            sellableItems = emptyList()  // Clear sellable items when changing location
                        )
                    }
                    // Load shop items if at a shop
                    if (isShop) {
                        loadShopItems(locationId)
                    }

                    // Load other players at this location
                    loadPlayersAtLocation(locationId)

                    // Load puzzles at this location
                    loadPuzzlesAtLocation(locationId)

                    // NOTE: We no longer update the server when the repository falls back to (0,0).
                    // The server has the correct location - the listenForUserUpdates() function
                    // will correct the repository once the server data is received.
                    firstLocationUpdate = false
                }
            }
        }
    }

    /**
     * Listen for real-time player presence events (enter/leave).
     * Updates the playersHere list when other players enter or leave the current location.
     */
    private fun listenForPlayerPresenceEvents() {
        scope.launch {
            AdventureStateHolder.playerPresenceEvents.collect { event ->
                val currentLocationId = AdventureRepository.currentLocationId.value
                when (event) {
                    is PlayerPresenceEvent.PlayerEntered -> {
                        // Only update if the event is for our current location
                        if (event.locationId == currentLocationId) {
                            // Fetch the full user data and add to playersHere
                            ApiClient.getUser(event.playerId).onSuccess { user ->
                                if (user != null) {
                                    _localState.update { state ->
                                        // Don't add if already present or if it's ourselves
                                        if (state.playersHere.any { it.id == user.id } || user.id == UserStateHolder.userId) {
                                            state
                                        } else {
                                            state.copy(playersHere = state.playersHere + user)
                                        }
                                    }
                                }
                            }
                            // Add event log entry
                            CombatStateHolder.addEventLogEntry(
                                "${event.playerName} arrives.",
                                EventLogType.NAVIGATION
                            )
                        }
                    }
                    is PlayerPresenceEvent.PlayerLeft -> {
                        // Only update if the event is for our current location
                        if (event.locationId == currentLocationId) {
                            _localState.update { state ->
                                state.copy(
                                    playersHere = state.playersHere.filter { it.id != event.playerId }
                                )
                            }
                            // Add event log entry
                            CombatStateHolder.addEventLogEntry(
                                "${event.playerName} leaves.",
                                EventLogType.NAVIGATION
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Listen for WebSocket connection state changes.
     * On reconnection, re-sync location with server to ensure client/server are in sync.
     * This handles cases where the user's location drifted during a disconnect.
     */
    private fun listenForConnectionStateChanges() {
        var wasDisconnected = false
        scope.launch {
            CombatStateHolder.connectionState.collect { state ->
                when (state) {
                    CombatConnectionState.DISCONNECTED,
                    CombatConnectionState.RECONNECTING -> {
                        wasDisconnected = true
                    }
                    CombatConnectionState.CONNECTED -> {
                        if (wasDisconnected) {
                            wasDisconnected = false
                            println("[AdventureViewModel] WebSocket reconnected - syncing location with server")
                            syncLocationWithServer()
                        }
                    }
                    else -> { /* CONNECTING - do nothing */ }
                }
            }
        }
    }

    /**
     * Fetch the user's authoritative location from server and update client if different.
     * This ensures the client never drifts from server state.
     *
     * IMPORTANT: Skips sync if we recently navigated, to prevent overwriting the
     * optimistic update before the server has processed our location change.
     */
    private fun syncLocationWithServer() {
        // Skip sync if we recently navigated - server may not have processed it yet
        val timeSinceLastNav = currentTimeMillis() - lastNavigationTime
        if (timeSinceLastNav < NAVIGATION_SYNC_COOLDOWN_MS) {
            println("[AdventureViewModel] Skipping location sync - navigated ${timeSinceLastNav}ms ago (cooldown: ${NAVIGATION_SYNC_COOLDOWN_MS}ms)")
            return
        }

        val userId = UserStateHolder.userId ?: return
        val clientLocationId = AdventureRepository.currentLocationId.value
        println("[AdventureViewModel] syncLocationWithServer: fetching user $userId, client location = $clientLocationId")

        scope.launch {
            ApiClient.getUser(userId).onSuccess { user ->
                if (user != null) {
                    val serverLocationId = user.currentLocationId
                    println("[AdventureViewModel] syncLocationWithServer: server location = $serverLocationId")
                    if (serverLocationId != null && serverLocationId != clientLocationId) {
                        println("[AdventureViewModel] Location mismatch detected! Server=$serverLocationId, Client=$clientLocationId - correcting to server")
                        AdventureRepository.setCurrentLocation(serverLocationId)
                        UserStateHolder.updateLocationLocally(serverLocationId)
                        // Update shop/inn state
                        _localState.update {
                            it.copy(
                                isShopLocation = serverLocationId in shopLocationIds,
                                isInnLocation = serverLocationId == innLocationId
                            )
                        }
                    } else {
                        println("[AdventureViewModel] syncLocationWithServer: locations match, no correction needed")
                    }
                }
            }.onFailure { error ->
                println("[AdventureViewModel] syncLocationWithServer: FAILED to fetch user - ${error.message}")
            }
        }
    }

    private fun connectCombatWebSocket() {
        val userId = UserStateHolder.userId ?: return
        CombatStateHolder.connect(userId)
    }

    fun loadPlayerAbilities() {
        scope.launch {
            // Use latest user state, not stale constructor param
            val user = UserStateHolder.currentUser.value ?: currentUser ?: run {
                _localState.update { it.copy(abilitiesLoading = false) }
                return@launch
            }

            val classAbilities = mutableListOf<AbilityDto>()
            val itemAbilities = mutableListOf<AbilityDto>()
            val learnedAbilities = mutableListOf<AbilityDto>()

            // 1. Load class abilities
            val classId = user.characterClassId
            if (classId != null) {
                ApiClient.getAbilitiesByClass(classId).getOrNull()?.let { abilities ->
                    classAbilities.addAll(abilities)
                }
                ApiClient.getCharacterClass(classId).getOrNull()?.let { characterClass ->
                    _localState.update { it.copy(playerCharacterClass = characterClass) }
                }
            }

            // 2. Load item abilities from equipped items
            val equippedItemIds = user.equippedItemIds
            var hasPhasewalk = false
            if (equippedItemIds.isNotEmpty()) {
                val allItems = ApiClient.getItems().getOrNull() ?: emptyList()
                val equippedItems = allItems.filter { it.id in equippedItemIds }
                val abilityIdsFromItems = equippedItems.flatMap { it.abilityIds }.distinct()

                // Check if player has phasewalk ability from equipped items
                hasPhasewalk = "ability-phasewalk" in abilityIdsFromItems

                // Fetch each ability sequentially to ensure all complete
                for (abilityId in abilityIdsFromItems) {
                    ApiClient.getAbility(abilityId).getOrNull()?.let { ability ->
                        itemAbilities.add(ability)
                    }
                }
            }

            // 3. Load learned abilities from trainers
            val learnedAbilityIds = user.learnedAbilityIds
            if (learnedAbilityIds.isNotEmpty()) {
                // Check if player has phasewalk ability from learned abilities
                if ("ability-phasewalk" in learnedAbilityIds) {
                    hasPhasewalk = true
                }

                // Fetch each learned ability
                for (abilityId in learnedAbilityIds) {
                    ApiClient.getAbility(abilityId).getOrNull()?.let { ability ->
                        learnedAbilities.add(ability)
                    }
                }
            }

            // 4. Combine class + item + learned abilities (deduplicate by ID)
            val allAbilities = (classAbilities + itemAbilities + learnedAbilities)
                .distinctBy { it.id }
                .filter { it.abilityType != "passive" }
                .sortedBy { it.name.lowercase() }

            // 5. Filter by visible abilities if user has selected specific ones
            val visibleIds = user.visibleAbilityIds
            val displayedAbilities = if (visibleIds.isEmpty()) {
                allAbilities  // Show all when none selected
            } else {
                allAbilities.filter { it.id in visibleIds }
            }

            println("[Phasewalk] loadPlayerAbilities complete, setting hasPhasewalkAbility=$hasPhasewalk")
            _localState.update {
                it.copy(
                    playerAbilities = displayedAbilities,
                    abilitiesLoading = false,
                    hasPhasewalkAbility = hasPhasewalk
                )
            }

            // 4. If player has phasewalk, load destinations - otherwise clear them
            if (hasPhasewalk) {
                loadPhasewalkDestinations()
            } else {
                println("[Phasewalk] Player doesn't have phasewalk, clearing destinations")
                _localState.update { it.copy(phasewalkDestinations = emptyList()) }
            }
        }
    }

    private fun loadAbilitiesMap() {
        scope.launch {
            ApiClient.getAbilities().onSuccess { abilities ->
                _localState.update { it.copy(allAbilitiesMap = abilities.associateBy { it.id }) }
            }
        }
    }

    /**
     * Load other players at the given location.
     * Excludes the current user from the list.
     */
    private fun loadPlayersAtLocation(locationId: String) {
        val myUserId = UserStateHolder.userId
        scope.launch {
            ApiClient.getActiveUsersAtLocation(locationId)
                .onSuccess { users ->
                    // Exclude self from the list
                    val otherPlayers = users.filter { it.id != myUserId }
                    _localState.update { it.copy(playersHere = otherPlayers) }
                }
                .onFailure {
                    // On failure, clear the list
                    _localState.update { it.copy(playersHere = emptyList()) }
                }
        }
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    fun navigateToExit(exit: ExitDto) {
        // Block movement if player is downed (HP <= 0)
        // Fall back to passed-in currentUser if UserStateHolder isn't initialized yet
        val playerHp = UserStateHolder.currentUser.value?.currentHp
            ?: currentUser?.currentHp
            ?: 0
        if (playerHp <= 0) {
            logError("You cannot move while incapacitated")
            return
        }

        // Block movement if player is over-encumbered (>100% capacity)
        val user = UserStateHolder.currentUser.value
        if (user != null) {
            val strength = user.strength
            val maxCapacity = strength * 5  // Max capacity in stone
            val allItems = AdventureRepository.items.value
            val itemsMap = allItems.associateBy { it.id }
            val totalWeight = user.itemIds.sumOf { itemId -> itemsMap[itemId]?.weight ?: 0 }

            if (totalWeight > maxCapacity) {
                val overAmount = totalWeight - maxCapacity
                logError("You are over-encumbered by $overAmount stone. Drop items to move.")
                return
            }
        }

        scope.launch {
            val userId = UserStateHolder.userId
            if (userId == null) {
                println("[Navigation] WARNING: userId is null, cannot navigate")
                return@launch
            }

            // Capture previous location for rollback on failure
            val previousLocationId = AdventureRepository.currentLocationId.value

            // Mark navigation time to prevent sync from overwriting
            lastNavigationTime = currentTimeMillis()

            // Optimistically update client state for instant feedback
            AdventureRepository.setCurrentLocation(exit.locationId)

            // Clear selection and update shop state
            val isShop = exit.locationId in shopLocationIds
            val isInn = exit.locationId == innLocationId
            val isGenStore = exit.locationId == generalStoreLocationId
            _localState.update {
                it.copy(
                    selectedCreature = null,
                    selectedItem = null,
                    showDescriptionPopup = false,
                    isShopLocation = isShop,
                    isInnLocation = isInn,
                    isGeneralStore = isGenStore,
                    shopItems = emptyList(),
                    sellableItems = emptyList(),
                    showSellModal = false
                )
            }

            // Load shop items and show gold message if at a shop location
            if (isShop || isInn) {
                val gold = _localState.value.playerGold
                val locationName = AdventureRepository.getLocation(exit.locationId)?.name ?: "the shop"
                CombatStateHolder.addEventLogEntry(
                    "You enter $locationName. You have $gold gold.",
                    EventLogType.NAVIGATION
                )
            }
            if (isShop) {
                loadShopItems(exit.locationId)
                // Load sellable items for general store
                if (isGenStore) {
                    loadSellableItems(exit.locationId)
                }
            }

            // Update server - CRITICAL: rollback client state if this fails
            println("[Navigation] Updating server location: userId=$userId, locationId=${exit.locationId}")
            ApiClient.updateUserLocation(userId, exit.locationId)
                .onSuccess {
                    println("[Navigation] Location update succeeded")
                    // Persist location locally so it survives page refresh
                    UserStateHolder.updateLocationLocally(exit.locationId)
                }
                .onFailure { error ->
                    println("[Navigation] Location update FAILED: ${error.message} - rolling back to $previousLocationId")
                    // Rollback client state to match server
                    if (previousLocationId != null) {
                        AdventureRepository.setCurrentLocation(previousLocationId)
                        _localState.update {
                            it.copy(
                                isShopLocation = previousLocationId in shopLocationIds,
                                isInnLocation = previousLocationId == innLocationId
                            )
                        }
                    }
                    logError("Failed to move: ${error.message}")
                }

            // Load phasewalk destinations for the new location (only if player has phasewalk)
            if (_localState.value.hasPhasewalkAbility) {
                loadPhasewalkDestinations()
            }
        }
    }

    /**
     * Reload phasewalk destinations.
     * Called on init, location change, and after equipment changes.
     * Public so it can be triggered when user equips/unequips items.
     *
     * NOTE: Uses local computation only. The server endpoint is not used because
     * it reads from the database which may have stale location data during navigation.
     * Local computation uses the client's authoritative location state.
     */
    fun loadPhasewalkDestinations() {
        val hasPhasewalk = _localState.value.hasPhasewalkAbility
        println("[Phasewalk] loadPhasewalkDestinations called, hasPhasewalkAbility=$hasPhasewalk")

        // Only compute/load if player has phasewalk ability
        if (!hasPhasewalk) {
            println("[Phasewalk] Clearing destinations - player doesn't have phasewalk")
            _localState.update { it.copy(phasewalkDestinations = emptyList()) }
            return
        }

        // Compute locally - this is authoritative since client tracks location state
        computePhasewalkDestinationsLocally()
    }

    /**
     * Compute phasewalk destinations locally based on current location's grid position.
     * Shows destinations immediately without waiting for server response.
     */
    private fun computePhasewalkDestinationsLocally() {
        val currentLoc = AdventureRepository.getCurrentLocation() ?: return
        val gridX = currentLoc.gridX ?: return
        val gridY = currentLoc.gridY ?: return
        val areaId = currentLoc.areaId ?: "overworld"

        // Get existing exit directions
        val existingExitDirections = currentLoc.exits
            .map { it.direction.name.lowercase() }
            .toSet()

        // All 8 directions with their offsets
        val directions = listOf(
            "north" to Pair(0, -1),
            "northeast" to Pair(1, -1),
            "east" to Pair(1, 0),
            "southeast" to Pair(1, 1),
            "south" to Pair(0, 1),
            "southwest" to Pair(-1, 1),
            "west" to Pair(-1, 0),
            "northwest" to Pair(-1, -1)
        )

        // Get all locations in the same area
        val areaLocations = AdventureRepository.locations.value
            .filter { it.areaId == areaId && it.gridX != null && it.gridY != null }

        // Find phasewalk destinations (directions without exits that have a location)
        val destinations = directions
            .filter { (dir, _) -> dir !in existingExitDirections }
            .mapNotNull { (dir, offset) ->
                val targetX = gridX + offset.first
                val targetY = gridY + offset.second
                val targetLoc = areaLocations.find { it.gridX == targetX && it.gridY == targetY }
                if (targetLoc != null) {
                    PhasewalkDestinationDto(
                        direction = dir,
                        locationId = targetLoc.id,
                        locationName = targetLoc.name,
                        gridX = targetX,
                        gridY = targetY
                    )
                } else null
            }

        println("[Phasewalk] Computed ${destinations.size} destinations locally")
        _localState.update { it.copy(phasewalkDestinations = destinations) }
    }

    fun phasewalk(direction: String) {
        // Block movement if player is downed (HP <= 0)
        // Fall back to passed-in currentUser if UserStateHolder isn't initialized yet
        val playerHp = UserStateHolder.currentUser.value?.currentHp
            ?: currentUser?.currentHp
            ?: 0
        if (playerHp <= 0) {
            logError("You cannot phasewalk while incapacitated")
            return
        }

        // Block movement if player is over-encumbered (>100% capacity)
        val user = UserStateHolder.currentUser.value
        if (user != null) {
            val strength = user.strength
            val maxCapacity = strength * 5  // Max capacity in stone
            val allItems = AdventureRepository.items.value
            val itemsMap = allItems.associateBy { it.id }
            val totalWeight = user.itemIds.sumOf { itemId -> itemsMap[itemId]?.weight ?: 0 }

            if (totalWeight > maxCapacity) {
                val overAmount = totalWeight - maxCapacity
                logError("You are over-encumbered by $overAmount stone. Drop items to phasewalk.")
                return
            }
        }

        val userId = UserStateHolder.userId ?: return
        scope.launch {
            ApiClient.phasewalk(userId, direction).onSuccess { response ->
                if (response.success && response.newLocationId != null) {
                    // Deduct mana locally for immediate UI feedback (2 mana cost)
                    UserStateHolder.spendManaLocally(2)

                    // Clear combat state - phasewalking removes player from combat
                    CombatStateHolder.clearCombatStatePublic()

                    // Add event log entries
                    CombatStateHolder.addEventLogEntry(
                        response.message,
                        EventLogType.NAVIGATION
                    )

                    // Update repository's current location
                    AdventureRepository.setCurrentLocation(response.newLocationId)

                    // Clear selection state
                    _localState.update {
                        it.copy(
                            selectedCreature = null,
                            selectedItem = null,
                            showDescriptionPopup = false,
                            isShopLocation = false,
                            isInnLocation = false,
                            shopItems = emptyList()
                        )
                    }

                    // NOTE: Don't call updateUserLocation here - phasewalk already updated it on server
                    // Calling it again would trigger checkAggressiveCreatures and send duplicate combat messages
                    // But we do need to persist the location locally so it survives page refresh
                    UserStateHolder.updateLocationLocally(response.newLocationId)

                    // Load phasewalk destinations for the new location (phasewalk ability already confirmed)
                    if (_localState.value.hasPhasewalkAbility) {
                        loadPhasewalkDestinations()
                    }
                } else {
                    logError(response.message)
                }
            }.onFailure {
                logError("Failed to phasewalk: ${it.message}")
            }
        }
    }

    private fun loadShopItems(locationId: String) {
        scope.launch {
            val location = AdventureRepository.getLocation(locationId) ?: return@launch
            val itemIds = location.itemIds
            if (itemIds.isEmpty()) return@launch

            // Load each item's details
            ApiClient.getItems().onSuccess { allItems ->
                val shopItems = allItems.filter { it.id in itemIds }
                _localState.update { it.copy(shopItems = shopItems) }
            }
        }
    }

    /**
     * Load shop items directly from API, without relying on repository.
     * Used during initial load when repository might not be ready yet.
     */
    private fun loadShopItemsFromApi(locationId: String) {
        scope.launch {
            // Fetch the location directly from API
            ApiClient.getLocation(locationId).onSuccess { location ->
                if (location == null) return@onSuccess
                val itemIds = location.itemIds
                if (itemIds.isEmpty()) return@onSuccess

                // Load items
                ApiClient.getItems().onSuccess { allItems ->
                    val shopItems = allItems.filter { it.id in itemIds }
                    _localState.update { it.copy(shopItems = shopItems) }
                }
            }
            // Also load sellable items if at general store
            if (locationId == generalStoreLocationId) {
                loadSellableItems(locationId)
            }
        }
    }

    /**
     * Load sellable items for the general store.
     * Also checks if the user is banned from the store.
     */
    private fun loadSellableItems(locationId: String) {
        val userId = currentUser?.id ?: return
        scope.launch {
            // First check ban status
            ApiClient.getShopBanStatus(locationId, userId).onSuccess { banResponse ->
                if (banResponse.isBanned) {
                    _localState.update {
                        it.copy(
                            isShopBanned = true,
                            shopBanMessage = banResponse.message,
                            sellableItems = emptyList()
                        )
                    }
                    // Show the ban message in the event log
                    banResponse.message?.let { msg ->
                        CombatStateHolder.addEventLogEntry(msg, EventLogType.ERROR)
                    }
                    return@onSuccess
                }

                // Not banned, load sellable items
                _localState.update { it.copy(isShopBanned = false, shopBanMessage = null) }
                ApiClient.getSellableItems(locationId, userId).onSuccess { response ->
                    if (response.success) {
                        _localState.update { it.copy(sellableItems = response.items) }
                    }
                }.onFailure {
                    println("[AdventureViewModel] Failed to load sellable items: ${it.message}")
                }
            }.onFailure {
                println("[AdventureViewModel] Failed to check shop ban status: ${it.message}")
                // Still try to load items if ban check fails
                ApiClient.getSellableItems(locationId, userId).onSuccess { response ->
                    if (response.success) {
                        _localState.update { it.copy(sellableItems = response.items) }
                    }
                }
            }
        }
    }

    /**
     * Show the sell modal at the general store.
     */
    fun openSellModal() {
        val locationId = _localState.value.let {
            if (it.isGeneralStore) generalStoreLocationId else null
        } ?: return

        // Check if banned - don't open modal, just show message
        if (_localState.value.isShopBanned) {
            _localState.value.shopBanMessage?.let { msg ->
                CombatStateHolder.addEventLogEntry(msg, EventLogType.ERROR)
            }
            return
        }

        loadSellableItems(locationId)
        _localState.update { it.copy(showSellModal = true) }
    }

    /**
     * Close the sell modal.
     */
    fun closeSellModal() {
        _localState.update { it.copy(showSellModal = false) }
    }

    /**
     * Sell an item at the general store.
     */
    fun sellItem(sellableItem: com.ez2bg.anotherthread.api.SellableItemDto) {
        val userId = currentUser?.id ?: return
        scope.launch {
            val result = if (sellableItem.isFoodItem) {
                ApiClient.sellFoodItem(generalStoreLocationId, userId, sellableItem.id)
            } else {
                ApiClient.sellItem(generalStoreLocationId, userId, sellableItem.itemId)
            }

            result.onSuccess { response ->
                if (response.success) {
                    CombatStateHolder.addEventLogEntry(response.message, EventLogType.LOOT)
                    // Update user state with new gold
                    response.user?.let { UserStateHolder.updateUser(it) }
                    // Refresh sellable items
                    loadSellableItems(generalStoreLocationId)
                } else {
                    // Check if this was a cursed item attempt - the message will contain the ban
                    CombatStateHolder.addEventLogEntry(response.message, EventLogType.ERROR)
                    // Reload to get updated ban status and close modal
                    loadSellableItems(generalStoreLocationId)
                    closeSellModal()
                }
            }.onFailure { error ->
                // Server returned an error - likely banned or cursed item
                val errorMessage = error.message ?: "Failed to sell item"
                CombatStateHolder.addEventLogEntry(errorMessage, EventLogType.ERROR)
                // Reload to check for ban status
                loadSellableItems(generalStoreLocationId)
                closeSellModal()
            }
        }
    }

    fun refresh() {
        AdventureRepository.refresh()
    }

    // =========================================================================
    // DETAIL VIEW
    // =========================================================================

    fun selectCreature(creature: CreatureDto) {
        _localState.update {
            it.copy(
                selectedCreature = creature,
                selectedItem = null,
                showCreatureInteractionModal = true
            )
        }
    }

    fun dismissCreatureInteractionModal() {
        _localState.update {
            it.copy(
                showCreatureInteractionModal = false,
                selectedCreature = null
            )
        }
    }

    fun selectItem(item: ItemDto) {
        _localState.update { it.copy(selectedItem = item, selectedCreature = null) }
    }

    fun clearSelection() {
        _localState.update {
            it.copy(
                selectedCreature = null,
                selectedItem = null,
                showDescriptionPopup = false
            )
        }
    }

    fun showDescriptionPopup() {
        _localState.update { it.copy(showDescriptionPopup = true) }
    }

    fun hideDescriptionPopup() {
        _localState.update { it.copy(showDescriptionPopup = false) }
    }

    // =========================================================================
    // SHOP ACTIONS
    // =========================================================================

    fun buyItem(itemId: String) {
        val userId = UserStateHolder.userId ?: return
        val locationId = uiState.value.currentLocationId ?: return
        scope.launch {
            ApiClient.buyItem(locationId, userId, itemId).onSuccess { response ->
                if (response.success) {
                    // Update user state with new inventory and gold
                    response.user?.let { user ->
                        _localState.update { it.copy(playerGold = user.gold) }
                        UserStateHolder.updateUser(user)
                    }
                    showSnackbar(response.message)
                } else {
                    showSnackbar(response.message)
                }
            }.onFailure {
                logError("Purchase failed: ${it.message}")
            }
        }
    }

    fun restAtInn() {
        val userId = UserStateHolder.userId ?: return
        val locationId = uiState.value.currentLocationId ?: return
        scope.launch {
            ApiClient.restAtInn(locationId, userId).onSuccess { response ->
                if (response.success) {
                    // Update user state with restored HP/MP/SP and spent gold
                    response.user?.let { user ->
                        _localState.update { it.copy(playerGold = user.gold) }
                        UserStateHolder.updateUser(user)
                    }
                    showSnackbar(response.message)
                } else {
                    showSnackbar(response.message)
                }
            }.onFailure {
                logError("Rest failed: ${it.message}")
            }
        }
    }

    // =========================================================================
    // ITEM PICKUP
    // =========================================================================

    fun pickupItem(item: ItemDto) {
        // Block actions if player is downed (HP <= 0)
        val playerHp = UserStateHolder.currentUser.value?.currentHp
            ?: currentUser?.currentHp
            ?: 0
        if (playerHp <= 0) {
            showSnackbar("You cannot act while incapacitated")
            return
        }

        val userId = UserStateHolder.userId
        if (userId == null) {
            showSnackbar("Not logged in")
            return
        }
        val locationId = uiState.value.currentLocationId
        if (locationId == null) {
            showSnackbar("Location not loaded")
            return
        }

        scope.launch {
            ApiClient.pickupItem(userId, item.id, locationId).onSuccess { updatedUser ->
                // Log pickup to event log with player name
                val playerName = UserStateHolder.userName ?: "You"
                CombatStateHolder.addEventLogEntry("$playerName picked up ${item.name}", EventLogType.LOOT)
                // Update user state with new inventory
                UserStateHolder.updateUser(updatedUser)
                // Refresh the location data to reflect item removal
                AdventureRepository.refresh()
            }.onFailure { error ->
                val message = error.message ?: "Failed to pick up item"
                // Parse common error messages for better UX
                when {
                    message.contains("Inventory full") -> logError("Inventory full!")
                    message.contains("not at this location") -> logError("Item is no longer here")
                    else -> logError(message)
                }
            }
        }
    }

    // =========================================================================
    // ABILITIES
    // =========================================================================

    fun handleAbilityClick(ability: AbilityDto) {
        // Block actions if player is downed (HP <= 0)
        val playerHp = UserStateHolder.currentUser.value?.currentHp
            ?: currentUser?.currentHp
            ?: 0
        if (playerHp <= 0) {
            logError("You cannot act while incapacitated")
            return
        }

        val state = uiState.value

        // Handle map_select abilities (teleport) — works outside combat
        if (ability.targetType == "map_select") {
            handleTeleportAbility(ability)
            return
        }

        // Handle rift abilities — works outside combat
        if (ability.targetType == "rift_open") {
            handleOpenRiftAbility(ability)
            return
        }
        if (ability.targetType == "rift_seal") {
            handleSealRiftAbility(ability)
            return
        }

        // Handle track ability — works outside combat
        if (ability.effects.contains("\"type\":\"track\"")) {
            handleTrackAbility(ability)
            return
        }

        // Handle hide ability — works outside combat
        if (ability.effects.contains("\"type\":\"hide\"")) {
            handleHideAbility(ability)
            return
        }

        // Handle sneak ability — works outside combat
        if (ability.effects.contains("\"type\":\"sneak\"")) {
            handleSneakAbility(ability)
            return
        }

        // Handle charm ability — requires target selection, works outside combat
        if (ability.effects.contains("\"type\":\"charm\"")) {
            handleCharmAbility(ability)
            return
        }

        val needToJoinCombat = !CombatStateHolder.isInCombat
        if (needToJoinCombat) {
            // AoE abilities can initiate combat with all hostiles
            if (ability.targetType in listOf("area", "all_enemies")) {
                val creatureIds = state.creaturesHere.map { it.id }
                if (creatureIds.isNotEmpty()) {
                    // Join combat and queue the ability to use when combat starts
                    CombatStateHolder.joinCombat(
                        targetCreatureIds = creatureIds,
                        withAbility = ability.id,
                        targetId = null
                    )
                    showSnackbar("Engaging all enemies with ${ability.name}!")
                    return
                } else {
                    showSnackbar("No enemies nearby")
                    return
                }
            } else {
                showSnackbar("Not in combat")
                return
            }
        }

        // Already in combat, use ability directly
        when (ability.targetType) {
            "self", "area", "all_enemies", "all_allies" -> {
                CombatStateHolder.useAbility(ability.id, null)
                showSnackbar("Casting: ${ability.name}")
            }
            "single_enemy" -> {
                when {
                    state.creaturesHere.size == 1 -> {
                        val target = state.creaturesHere.first()
                        CombatStateHolder.useAbility(ability.id, target.id)
                        showSnackbar("Casting ${ability.name} on ${target.name}")
                    }
                    state.creaturesHere.isEmpty() -> {
                        showSnackbar("No enemies to target")
                    }
                    else -> {
                        _localState.update { it.copy(pendingAbility = ability) }
                    }
                }
            }
            "single_ally" -> {
                CombatStateHolder.useAbility(ability.id, UserStateHolder.userId)
                showSnackbar("Casting ${ability.name} on self")
            }
            else -> {
                CombatStateHolder.useAbility(ability.id, null)
                showSnackbar("Casting: ${ability.name}")
            }
        }
    }

    fun selectAbilityTarget(targetId: String) {
        val ability = _localState.value.pendingAbility ?: return
        val state = uiState.value
        val targetName = state.creaturesHere.find { it.id == targetId }?.name ?: "target"

        CombatStateHolder.useAbility(ability.id, targetId)
        showSnackbar("Casting ${ability.name} on $targetName")

        _localState.update { it.copy(pendingAbility = null) }
    }

    fun cancelAbilityTargeting() {
        _localState.update { it.copy(pendingAbility = null) }
    }

    // =========================================================================
    // TRACK
    // =========================================================================

    private fun handleTrackAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            showSnackbar("Cannot track while in combat!")
            return
        }

        val userId = UserStateHolder.userId ?: return

        scope.launch {
            logMessage("You kneel down and examine the ground for tracks...")

            ApiClient.trackLocation(userId).onSuccess { result ->
                // Log the main message
                logMessage(result.message)

                // Log details about each detected trail
                for (trail in result.trails) {
                    val freshnessEmoji = when (trail.freshness) {
                        "fresh" -> "[!]"
                        "recent" -> "[~]"
                        "old" -> "[.]"
                        else -> "[?]"
                    }

                    val directionInfo = buildString {
                        if (trail.directionFrom != null) {
                            append("from ${trail.directionFrom}")
                        }
                        if (trail.directionTo != null) {
                            if (trail.directionFrom != null) append(", ")
                            append("heading ${trail.directionTo}")
                        }
                    }.ifEmpty { "origin unknown" }

                    val timeAgo = when {
                        trail.minutesAgo < 1 -> "moments ago"
                        trail.minutesAgo == 1 -> "1 minute ago"
                        trail.minutesAgo < 60 -> "${trail.minutesAgo} minutes ago"
                        else -> "${trail.minutesAgo / 60} hours ago"
                    }

                    val entityDesc = if (trail.entityType == "player") {
                        trail.entityName
                    } else {
                        "A ${trail.entityName}"
                    }

                    logMessage("$freshnessEmoji $entityDesc - $directionInfo ($timeAgo)")
                }
            }.onFailure { error ->
                logError("Failed to track: ${error.message}")
            }
        }
    }

    // =========================================================================
    // HIDE / SNEAK
    // =========================================================================

    private fun handleHideAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            showSnackbar("Cannot hide while in combat!")
            return
        }

        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.attemptHide(userId).onSuccess { result ->
                logMessage(result.message)
                // Refresh user data to update isHidden status
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                logError("Failed to hide: ${error.message}")
            }
        }
    }

    private fun handleSneakAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            showSnackbar("Cannot sneak while in combat!")
            return
        }

        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.attemptSneak(userId).onSuccess { result ->
                logMessage(result.message)
                // Refresh user data to update isSneaking status
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                logError("Failed to sneak: ${error.message}")
            }
        }
    }

    // =========================================================================
    // CHARM
    // =========================================================================

    private fun handleCharmAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            showSnackbar("Cannot charm while in combat!")
            return
        }

        // Get creatures at current location for charm targeting
        val creatures = AdventureStateHolder.creaturesHere.value
        if (creatures.isEmpty()) {
            showSnackbar("No creatures here to charm")
            return
        }

        // Show creature selection for charm
        _localState.update {
            it.copy(
                showCharmTargetSelection = true,
                charmableCreatures = creatures
            )
        }
    }

    fun selectCharmTarget(creature: CreatureDto) {
        val userId = UserStateHolder.userId ?: return

        // Close the selection dialog
        _localState.update {
            it.copy(
                showCharmTargetSelection = false,
                charmableCreatures = emptyList()
            )
        }

        scope.launch {
            ApiClient.charmCreature(userId, creature.id).onSuccess { result ->
                if (result.success) {
                    logMessage(result.message)
                    // Refresh to update charmed creature state
                    UserStateHolder.refreshUser()
                    // Refresh location creatures since one may now be charmed
                    AdventureStateHolder.refreshCurrentLocation()
                } else {
                    logMessage(result.message)
                }
            }.onFailure { error ->
                logError("Failed to charm: ${error.message}")
            }
        }
    }

    fun cancelCharmSelection() {
        _localState.update {
            it.copy(
                showCharmTargetSelection = false,
                charmableCreatures = emptyList()
            )
        }
    }

    fun releaseCharmedCreature() {
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.releaseCharmedCreature(userId).onSuccess { result ->
                logMessage(result.message)
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                logError("Failed to release: ${error.message}")
            }
        }
    }

    // =========================================================================
    // SEARCH
    // =========================================================================

    fun searchLocation() {
        val userId = UserStateHolder.userId ?: return

        // Already searching
        if (_localState.value.isSearching) return

        scope.launch {
            // First get the search duration
            ApiClient.getSearchInfo(userId).onSuccess { info ->
                // Show the search overlay
                _localState.update { it.copy(isSearching = true, searchDurationMs = info.durationMs) }

                // Wait for the search duration
                kotlinx.coroutines.delay(info.durationMs)

                // Now perform the actual search
                ApiClient.searchLocation(userId).onSuccess { result ->
                    logMessage(result.message)
                    if (result.discoveredItems.isNotEmpty()) {
                        // Refresh location to show newly discovered items
                        AdventureStateHolder.refreshCurrentLocation()
                    }
                }.onFailure { error ->
                    logError("Failed to search: ${error.message}")
                }

                // Hide the overlay
                _localState.update { it.copy(isSearching = false, searchDurationMs = 0) }
            }.onFailure { error ->
                logError("Failed to start search: ${error.message}")
            }
        }
    }

    /**
     * Cancel an in-progress search.
     */
    fun cancelSearch() {
        _localState.update { it.copy(isSearching = false, searchDurationMs = 0) }
    }

    // =========================================================================
    // FISHING
    // =========================================================================

    /**
     * Open the fishing distance modal.
     * Fetches fishing info from server and shows distance options.
     */
    fun openFishingModal() {
        val userId = UserStateHolder.userId ?: return

        // Already fishing
        if (_localState.value.isFishing) return

        scope.launch {
            ApiClient.getFishingInfo(userId).onSuccess { info ->
                if (!info.canFish) {
                    logMessage(info.reason ?: "You cannot fish here.")
                    return@onSuccess
                }
                _localState.update {
                    it.copy(
                        showFishingDistanceModal = true,
                        fishingInfo = info
                    )
                }
            }.onFailure { error ->
                logError("Failed to get fishing info: ${error.message}")
            }
        }
    }

    /**
     * Close the fishing distance modal without fishing.
     */
    fun closeFishingModal() {
        _localState.update {
            it.copy(
                showFishingDistanceModal = false,
                fishingInfo = null
            )
        }
    }

    /**
     * Start fishing at the specified distance.
     * Calls the API to start a minigame session, then shows the interactive minigame UI.
     */
    fun startFishing(distance: String) {
        val userId = UserStateHolder.userId ?: return
        val info = _localState.value.fishingInfo ?: return

        // Already fishing
        if (_localState.value.isFishing || _localState.value.showFishingMinigame) return

        scope.launch {
            // Close the modal and show loading
            _localState.update {
                it.copy(
                    showFishingDistanceModal = false,
                    isFishing = true
                )
            }

            // Start the minigame session on server
            ApiClient.startFishingMinigame(userId, distance).onSuccess { minigameData ->
                if (!minigameData.success) {
                    logMessage(minigameData.message ?: "Failed to start fishing")
                    _localState.update {
                        it.copy(
                            isFishing = false,
                            fishingInfo = null
                        )
                    }
                    return@onSuccess
                }

                // Show the minigame UI
                _localState.update {
                    it.copy(
                        isFishing = false,
                        showFishingMinigame = true,
                        fishingMinigameData = minigameData
                    )
                }
            }.onFailure { error ->
                logError("Failed to start fishing: ${error.message}")
                _localState.update {
                    it.copy(
                        isFishing = false,
                        fishingInfo = null
                    )
                }
            }
        }
    }

    /**
     * Complete the fishing minigame with the final score.
     * Called by the minigame UI when the game ends.
     */
    fun completeFishingMinigame(finalScore: Int) {
        val userId = UserStateHolder.userId ?: return
        val minigameData = _localState.value.fishingMinigameData ?: return
        val sessionId = minigameData.sessionId ?: return

        scope.launch {
            ApiClient.completeFishingMinigame(userId, sessionId, finalScore).onSuccess { result ->
                if (result.caught && result.fishCaught != null) {
                    val fish = result.fishCaught
                    val sizeDesc = when {
                        fish.weight >= 6 -> "massive"
                        fish.weight >= 4 -> "large"
                        fish.weight >= 2 -> "nice"
                        else -> "small"
                    }
                    logMessage("You caught a $sizeDesc ${fish.name}! (+${result.manaRestored} mana)", EventLogType.LOOT)

                    // Show badge earned message if applicable
                    if (result.earnedBadge) {
                        logMessage("You earned the Angler's Badge! Your fishing skill has improved.", EventLogType.LOOT)
                    }

                    // Refresh user data to update inventory and abilities
                    UserStateHolder.refreshUser()
                } else {
                    logMessage(result.message)
                }
            }.onFailure { error ->
                logError("Failed to complete fishing: ${error.message}")
            }

            // Hide the minigame UI
            _localState.update {
                it.copy(
                    showFishingMinigame = false,
                    fishingMinigameData = null,
                    fishingInfo = null
                )
            }
        }
    }

    /**
     * Cancel an in-progress fishing attempt or minigame.
     */
    fun cancelFishing() {
        _localState.update {
            it.copy(
                isFishing = false,
                fishingDurationMs = 0,
                fishingInfo = null,
                showFishingMinigame = false,
                fishingMinigameData = null
            )
        }
    }

    // =========================================================================
    // HIDE ITEM
    // =========================================================================

    fun openHideItemModal() {
        val user = UserStateHolder.currentUser.value ?: return

        // Get items from user inventory that are not equipped
        val allItems = AdventureRepository.items.value
        val hideableItems = user.itemIds
            .filter { it !in user.equippedItemIds }  // Exclude equipped items
            .mapNotNull { itemId -> allItems.find { it.id == itemId } }

        if (hideableItems.isEmpty()) {
            logMessage("You have nothing to hide.")
            return
        }

        _localState.update {
            it.copy(
                showHideItemModal = true,
                hideableItems = hideableItems
            )
        }
    }

    fun hideItem(item: ItemDto) {
        val userId = UserStateHolder.userId ?: return

        // Close the modal
        _localState.update {
            it.copy(
                showHideItemModal = false,
                hideableItems = emptyList()
            )
        }

        scope.launch {
            ApiClient.hideItem(userId, item.id).onSuccess { result ->
                logMessage(result.message)
                // Refresh user to update inventory
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                logError("Failed to hide item: ${error.message}")
            }
        }
    }

    fun cancelHideItem() {
        _localState.update {
            it.copy(
                showHideItemModal = false,
                hideableItems = emptyList()
            )
        }
    }

    // =========================================================================
    // TELEPORT
    // =========================================================================

    private fun handleTeleportAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            showSnackbar("Cannot teleport during combat!")
            return
        }

        scope.launch {
            ApiClient.getTeleportDestinations().onSuccess { destinations ->
                _localState.update {
                    it.copy(
                        showMapSelection = true,
                        teleportDestinations = destinations,
                        teleportAbilityId = ability.id
                    )
                }
            }.onFailure {
                logError("Failed to load destinations")
            }
        }
    }

    fun selectTeleportDestination(destination: TeleportDestinationDto) {
        val userId = UserStateHolder.userId ?: return
        val abilityId = _localState.value.teleportAbilityId ?: return

        // Block movement if player is over-encumbered (>100% capacity)
        val user = UserStateHolder.currentUser.value
        if (user != null) {
            val strength = user.strength
            val maxCapacity = strength * 5  // Max capacity in stone
            val allItems = AdventureRepository.items.value
            val itemsMap = allItems.associateBy { it.id }
            val totalWeight = user.itemIds.sumOf { itemId -> itemsMap[itemId]?.weight ?: 0 }

            if (totalWeight > maxCapacity) {
                val overAmount = totalWeight - maxCapacity
                logError("You are over-encumbered by $overAmount stone. Drop items to teleport.")
                _localState.update { it.copy(showMapSelection = false) }
                return
            }
        }

        scope.launch {
            // Dismiss overlay immediately
            _localState.update { it.copy(showMapSelection = false) }

            // Departure message
            val userName = UserStateHolder.userName ?: "Unknown"
            CombatStateHolder.addEventLogEntry(
                "With a soft pop $userName dematerializes.",
                EventLogType.NAVIGATION
            )

            // Call teleport API
            ApiClient.teleport(userId, destination.areaId, abilityId).onSuccess { response ->
                if (response.success && response.newLocationId != null) {
                    // Update location
                    AdventureRepository.setCurrentLocation(response.newLocationId)
                    // Persist locally so it survives page refresh
                    UserStateHolder.updateLocationLocally(response.newLocationId)

                    // Arrival message
                    CombatStateHolder.addEventLogEntry(
                        response.arrivalMessage ?: "$userName materializes with a loud bang!",
                        EventLogType.NAVIGATION
                    )

                    showSnackbar("Teleported to ${response.newLocationName ?: destination.locationName}")
                } else {
                    showSnackbar(response.message)
                }
            }.onFailure {
                logError("Teleport failed: ${it.message}")
            }

            // Clear teleport state
            _localState.update {
                it.copy(
                    teleportDestinations = emptyList(),
                    teleportAbilityId = null
                )
            }
        }
    }

    fun dismissMapSelection() {
        _localState.update {
            it.copy(
                showMapSelection = false,
                teleportDestinations = emptyList(),
                teleportAbilityId = null
            )
        }
    }

    // =========================================================================
    // RIFT PORTAL
    // =========================================================================

    private fun handleOpenRiftAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            showSnackbar("Cannot open rifts during combat!")
            return
        }

        val userId = UserStateHolder.userId ?: return
        scope.launch {
            ApiClient.getUnconnectedAreas(userId).onSuccess { areas ->
                if (areas.isEmpty()) {
                    showSnackbar("No unconnected realms to open rifts to")
                } else {
                    _localState.update {
                        it.copy(
                            showRiftSelection = true,
                            riftMode = "open",
                            unconnectedAreas = areas
                        )
                    }
                }
            }.onFailure {
                logError("Failed to find realms: ${it.message}")
            }
        }
    }

    private fun handleSealRiftAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            showSnackbar("Cannot seal rifts during combat!")
            return
        }

        val userId = UserStateHolder.userId ?: return
        scope.launch {
            ApiClient.getSealableRifts(userId).onSuccess { rifts ->
                if (rifts.isEmpty()) {
                    showSnackbar("No rifts to seal at this location")
                } else {
                    _localState.update {
                        it.copy(
                            showRiftSelection = true,
                            riftMode = "seal",
                            sealableRifts = rifts
                        )
                    }
                }
            }.onFailure {
                logError("Failed to find rifts: ${it.message}")
            }
        }
    }

    fun selectRiftToOpen(area: UnconnectedAreaDto) {
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            _localState.update { it.copy(showRiftSelection = false) }

            ApiClient.openRift(userId, area.areaId).onSuccess { response ->
                if (response.success) {
                    CombatStateHolder.addEventLogEntry(response.message, EventLogType.NAVIGATION)
                    showSnackbar(response.message)
                    // Refresh to show new exit
                    AdventureRepository.refresh()
                } else {
                    showSnackbar(response.message)
                }
            }.onFailure {
                logError("Failed to open rift: ${it.message}")
            }

            _localState.update {
                it.copy(riftMode = null, unconnectedAreas = emptyList())
            }
        }
    }

    fun selectRiftToSeal(rift: SealableRiftDto) {
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            _localState.update { it.copy(showRiftSelection = false) }

            ApiClient.sealRift(userId, rift.targetAreaId).onSuccess { response ->
                if (response.success) {
                    CombatStateHolder.addEventLogEntry(response.message, EventLogType.NAVIGATION)
                    showSnackbar(response.message)
                    // Refresh to remove exit
                    AdventureRepository.refresh()
                } else {
                    showSnackbar(response.message)
                }
            }.onFailure {
                logError("Failed to seal rift: ${it.message}")
            }

            _localState.update {
                it.copy(riftMode = null, sealableRifts = emptyList())
            }
        }
    }

    fun dismissRiftSelection() {
        _localState.update {
            it.copy(
                showRiftSelection = false,
                riftMode = null,
                unconnectedAreas = emptyList(),
                sealableRifts = emptyList()
            )
        }
    }

    // =========================================================================
    // Trainer Methods
    // =========================================================================

    /**
     * Open the trainer modal for a creature.
     * Fetches the trainer info from the server.
     */
    fun openTrainerModal(creature: CreatureDto) {
        val userId = UserStateHolder.userId ?: return

        _localState.update { it.copy(isLoadingTrainer = true, showTrainerModal = true) }

        scope.launch {
            ApiClient.getTrainerInfo(creature.id, userId)
                .onSuccess { trainerInfo ->
                    _localState.update {
                        it.copy(
                            trainerInfo = trainerInfo,
                            isLoadingTrainer = false
                        )
                    }
                }
                .onFailure { error ->
                    _localState.update {
                        it.copy(
                            showTrainerModal = false,
                            isLoadingTrainer = false
                        )
                    }
                    showSnackbar("Failed to load trainer info: ${error.message}")
                }
        }
    }

    /**
     * Learn an ability from the current trainer.
     */
    fun learnAbility(abilityId: String) {
        val trainerInfo = _localState.value.trainerInfo ?: return
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.learnAbility(trainerInfo.trainerId, userId, abilityId)
                .onSuccess { response ->
                    if (response.success) {
                        showSnackbar(response.message)
                        // Refresh trainer info to update learned status
                        ApiClient.getTrainerInfo(trainerInfo.trainerId, userId)
                            .onSuccess { updatedInfo ->
                                _localState.update { it.copy(trainerInfo = updatedInfo) }
                            }
                        // Refresh user data to update gold and learned abilities
                        AdventureRepository.refresh()
                        // Refresh player abilities
                        loadPlayerAbilities()
                    } else {
                        showSnackbar(response.message)
                    }
                }
                .onFailure { error ->
                    showSnackbar("Failed to learn ability: ${error.message}")
                }
        }
    }

    /**
     * Close the trainer modal.
     */
    fun dismissTrainerModal() {
        _localState.update {
            it.copy(
                showTrainerModal = false,
                trainerInfo = null,
                isLoadingTrainer = false
            )
        }
    }

    // =========================================================================
    // PUZZLE INTERACTION
    // =========================================================================

    /**
     * Load puzzles at the current location.
     */
    fun loadPuzzlesAtLocation(locationId: String) {
        println("[AdventureViewModel] Loading puzzles for location: $locationId")
        scope.launch {
            ApiClient.getPuzzlesAtLocation(locationId)
                .onSuccess { puzzles ->
                    println("[AdventureViewModel] Loaded ${puzzles.size} puzzles: ${puzzles.map { it.name }}")
                    _localState.update { it.copy(puzzlesAtLocation = puzzles) }
                }
                .onFailure { error ->
                    println("[AdventureViewModel] Failed to load puzzles: ${error.message}")
                    _localState.update { it.copy(puzzlesAtLocation = emptyList()) }
                }
        }
    }

    /**
     * Open a puzzle modal.
     */
    fun openPuzzleModal(puzzle: PuzzleDto) {
        val userId = UserStateHolder.userId ?: return

        _localState.update {
            it.copy(
                showPuzzleModal = true,
                currentPuzzle = puzzle,
                isLoadingPuzzle = true
            )
        }

        scope.launch {
            ApiClient.getPuzzleProgress(puzzle.id, userId)
                .onSuccess { progress ->
                    _localState.update {
                        it.copy(
                            puzzleProgress = progress,
                            isLoadingPuzzle = false
                        )
                    }
                }
                .onFailure { error ->
                    _localState.update {
                        it.copy(
                            showPuzzleModal = false,
                            isLoadingPuzzle = false
                        )
                    }
                    showSnackbar("Failed to load puzzle: ${error.message}")
                }
        }
    }

    /**
     * Pull a lever in the current puzzle.
     */
    fun pullLever(leverId: String) {
        val puzzle = _localState.value.currentPuzzle ?: return
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.pullLever(puzzle.id, leverId, userId)
                .onSuccess { response ->
                    // Show the message from the server
                    showSnackbar(response.message.replace("\n\n", " "))

                    // Refresh puzzle progress
                    ApiClient.getPuzzleProgress(puzzle.id, userId)
                        .onSuccess { progress ->
                            _localState.update { it.copy(puzzleProgress = progress) }
                        }

                    // If puzzle was solved, refresh the current location to show new exits
                    if (response.puzzleSolved) {
                        val currentLocationId = AdventureRepository.currentLocationId.value
                        if (currentLocationId != null) {
                            AdventureRepository.refreshLocationWithUserContext(currentLocationId, userId)
                        }
                    }
                }
                .onFailure { error ->
                    showSnackbar("Failed to pull lever: ${error.message}")
                }
        }
    }

    /**
     * Close the puzzle modal.
     */
    fun dismissPuzzleModal() {
        _localState.update {
            it.copy(
                showPuzzleModal = false,
                currentPuzzle = null,
                puzzleProgress = null,
                isLoadingPuzzle = false
            )
        }
    }

    /**
     * Use an ability directly on a selected creature (from detail view).
     * This bypasses target selection since the target is already selected.
     */
    fun useAbilityOnCreature(ability: AbilityDto, creature: CreatureDto?) {
        if (creature == null) {
            showSnackbar("No target selected")
            return
        }

        if (!CombatStateHolder.isInCombat) {
            // Start combat by joining with this creature, and queue the ability
            CombatStateHolder.joinCombat(
                targetCreatureIds = listOf(creature.id),
                withAbility = ability.id,
                targetId = creature.id
            )
            showSnackbar("Engaging ${creature.name}!")
        } else {
            // Already in combat, use the ability directly
            CombatStateHolder.useAbility(ability.id, creature.id)
            showSnackbar("${ability.name} -> ${creature.name}")
        }
    }

    /**
     * Initiate a basic attack against a creature.
     * Uses the universal basic attack ability.
     */
    fun initiateBasicAttack(creatureId: String) {
        val creature = uiState.value.allCreatures.find { it.id == creatureId }
        val creatureName = creature?.name ?: "enemy"

        if (!CombatStateHolder.isInCombat) {
            // Start combat by joining with this creature, and queue the attack
            CombatStateHolder.joinCombat(
                targetCreatureIds = listOf(creatureId),
                withAbility = BASIC_ATTACK_ID,
                targetId = creatureId
            )
            showSnackbar("Engaging $creatureName!")
        } else {
            // Already in combat, just use the ability
            CombatStateHolder.useAbility(BASIC_ATTACK_ID, creatureId)
        }
    }

    companion object {
        const val BASIC_ATTACK_ID = "universal-basic-attack"
    }

    // =========================================================================
    // VOLUNTARY DEATH (give up when downed)
    // =========================================================================

    /**
     * Voluntary death - player gives up while downed (HP <= 0).
     * Respawns at Tun du Lac with full HP.
     *
     * @param onComplete Callback invoked when the give-up is processed
     */
    fun giveUp(onComplete: (Boolean, String) -> Unit) {
        val userId = UserStateHolder.userId
        if (userId == null) {
            onComplete(false, "Not logged in")
            return
        }

        // Check if player is downed
        val playerHp = UserStateHolder.currentUser.value?.currentHp ?: 0
        if (playerHp > 0) {
            onComplete(false, "You can only give up when downed")
            return
        }

        scope.launch {
            ApiClient.giveUp(userId).onSuccess { response ->
                if (response.success) {
                    logMessage("You have died and respawned at ${response.respawnLocationName ?: "town"}.", EventLogType.ERROR)
                    if (response.itemsDropped > 0) {
                        logMessage("You dropped ${response.itemsDropped} item(s).", EventLogType.ERROR)
                    }
                    if (response.goldLost > 0) {
                        logMessage("You lost ${response.goldLost} gold.", EventLogType.ERROR)
                    }
                    // Note: The WebSocket will receive a PlayerDeathMessage which handles
                    // the death animation and user refresh via CombatStateHolder
                    onComplete(true, response.message)
                } else {
                    onComplete(false, response.message)
                }
            }.onFailure { error ->
                logError("Failed to give up: ${error.message}")
                onComplete(false, error.message ?: "Failed to give up")
            }
        }
    }

    // =========================================================================
    // EVENT LOG MESSAGES (replaces snackbar)
    // =========================================================================

    /**
     * Log a message to the event log. This replaces snackbar notifications.
     * Messages appear in the scrollable event log at the bottom of the screen.
     */
    private fun logMessage(message: String, type: EventLogType = EventLogType.INFO) {
        CombatStateHolder.addEventLogEntry(message, type)
    }

    /**
     * Log an error message to the event log.
     */
    private fun logError(message: String) {
        CombatStateHolder.addEventLogEntry(message, EventLogType.ERROR)
    }

    // Legacy method - kept for backward compatibility with any remaining snackbar usage
    private fun showSnackbar(message: String) {
        logMessage(message)
    }

    fun consumeSnackbarMessage() {
        _localState.update { it.copy(snackbarMessage = null) }
    }

    // =========================================================================
    // COMBAT STATE (delegated to CombatStateHolder)
    // These are exposed for the UI to collect as separate flows
    // =========================================================================

    val cooldowns = CombatStateHolder.cooldowns
    val queuedAbilityId = CombatStateHolder.queuedAbilityId
    val playerCombatant = CombatStateHolder.playerCombatant
    val combatants = CombatStateHolder.combatants
    val isBlinded = CombatStateHolder.isBlinded
    val blindRounds = CombatStateHolder.blindRounds
    val isDisoriented = CombatStateHolder.isDisoriented
    val disorientRounds = CombatStateHolder.disorientRounds
    val eventLog = CombatStateHolder.eventLog

    // =========================================================================
    // PLAYER INTERACTION
    // =========================================================================

    /**
     * Select a player to interact with (shows the player interaction modal).
     */
    fun selectPlayer(player: UserDto) {
        _localState.update {
            it.copy(
                selectedPlayer = player,
                showPlayerInteractionModal = true
            )
        }
    }

    /**
     * Dismiss the player interaction modal.
     */
    fun dismissPlayerInteractionModal() {
        _localState.update {
            it.copy(
                showPlayerInteractionModal = false,
                showGiveItemModal = false
            )
        }
    }

    /**
     * Show the give item modal for the selected player.
     */
    fun showGiveItemModal() {
        _localState.update {
            it.copy(showGiveItemModal = true)
        }
    }

    /**
     * Dismiss just the give item modal (back to player interaction).
     */
    fun dismissGiveItemModal() {
        _localState.update {
            it.copy(showGiveItemModal = false)
        }
    }

    /**
     * Give an item to the selected player.
     */
    fun giveItemToPlayer(itemId: String) {
        val targetPlayer = _localState.value.selectedPlayer ?: return
        val myUserId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.giveItem(myUserId, targetPlayer.id, itemId)
                .onSuccess { response ->
                    // Update our user state with new inventory
                    UserStateHolder.updateUser(response.giver)
                    logMessage("Gave ${response.itemName} to ${targetPlayer.name}")
                    dismissPlayerInteractionModal()
                }
                .onFailure { error ->
                    logError("Failed to give item: ${error.message}")
                }
        }
    }

    /**
     * Initiate attack against another player (PvP).
     * Note: Full PvP combat implementation may need additional work.
     */
    fun attackPlayer(player: UserDto) {
        logMessage("Attacking ${player.name}!")
        // TODO: Implement PvP combat when ready
        dismissPlayerInteractionModal()
    }

    /**
     * Refresh the current user's data from the server and update UserStateHolder.
     */
    private fun refreshCurrentUser() {
        val userId = UserStateHolder.currentUser.value?.id ?: return
        scope.launch {
            ApiClient.getUser(userId).onSuccess { user ->
                user?.let { UserStateHolder.updateUser(it) }
            }
        }
    }

    /**
     * Attempt to rob the selected player.
     * Uses DEX-based pickpocket mechanics. Success steals gold, failure alerts target.
     */
    fun robPlayer(player: UserDto) {
        val userId = UserStateHolder.currentUser.value?.id ?: return
        logMessage("Attempting to rob ${player.name}...")
        dismissPlayerInteractionModal()

        scope.launch {
            ApiClient.robPlayer(userId, player.id).onSuccess { result ->
                logMessage(result.message)
                if (result.success && result.goldStolen > 0) {
                    // Refresh our user data to show updated gold
                    refreshCurrentUser()
                }
            }.onFailure { error ->
                logMessage("Rob attempt failed: ${error.message}")
            }
        }
    }

    /**
     * Invite the selected player to party.
     * Both players must be at the same location.
     */
    fun inviteToParty(player: UserDto) {
        val userId = currentUser?.id ?: return
        dismissPlayerInteractionModal()

        scope.launch {
            ApiClient.inviteToParty(userId, player.id).onSuccess { response ->
                logMessage(response.message)
            }.onFailure { error ->
                logMessage("Failed to invite to party: ${error.message}")
            }
        }
    }

    /**
     * Accept a pending party invite from another player.
     * Makes the inviter the party leader.
     */
    fun acceptPartyInvite(player: UserDto) {
        val userId = currentUser?.id ?: return
        dismissPlayerInteractionModal()

        scope.launch {
            ApiClient.acceptPartyInvite(userId, player.id).onSuccess { response ->
                logMessage(response.message)
                // Clear the pending invite from state
                CombatStateHolder.clearPendingPartyInvite()
                // Refresh user data to get the partyLeaderId
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                logMessage("Failed to accept party invite: ${error.message}")
            }
        }
    }

    /**
     * Leave the current party.
     */
    fun leaveParty() {
        val userId = currentUser?.id ?: return

        scope.launch {
            ApiClient.leaveParty(userId).onSuccess { response ->
                logMessage(response.message)
                // Refresh user data to clear partyLeaderId
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                logMessage("Failed to leave party: ${error.message}")
            }
        }
    }

    /**
     * Disband the party (leader only).
     */
    fun disbandParty() {
        val userId = currentUser?.id ?: return

        scope.launch {
            ApiClient.disbandParty(userId).onSuccess { response ->
                logMessage(response.message)
                // Refresh user data to update isPartyLeader
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                logMessage("Failed to disband party: ${error.message}")
            }
        }
    }
}

/**
 * Convert event log entries from CombatStateHolder format to UI format.
 */
fun convertEventLogEntries(entries: List<com.ez2bg.anotherthread.state.EventLogEntry>): List<EventLogEntry> {
    return entries.map { entry ->
        EventLogEntry(
            id = entry.id.toString(),
            message = entry.message,
            type = when (entry.type) {
                EventLogType.INFO -> EventType.INFO
                EventLogType.DAMAGE_DEALT, EventLogType.DAMAGE_RECEIVED -> EventType.DAMAGE
                EventLogType.HEAL -> EventType.HEALING
                EventLogType.BUFF -> EventType.BUFF
                EventLogType.DEBUFF -> EventType.DEBUFF
                EventLogType.COMBAT_START, EventLogType.COMBAT_END -> EventType.COMBAT
                EventLogType.NAVIGATION -> EventType.MOVEMENT
                EventLogType.LOOT -> EventType.LOOT
                EventLogType.ERROR -> EventType.INFO
            }
        )
    }
}
