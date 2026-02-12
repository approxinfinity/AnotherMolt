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
import com.ez2bg.anotherthread.api.LockpickInfoDto
import com.ez2bg.anotherthread.api.DiplomacyResultDto
import com.ez2bg.anotherthread.api.HostilityResultDto
import com.ez2bg.anotherthread.api.ReactionResultDto
import com.ez2bg.anotherthread.data.AdventureRepository
import com.ez2bg.anotherthread.handlers.DiplomacyHandler
import com.ez2bg.anotherthread.handlers.DiplomacyEvent
import com.ez2bg.anotherthread.handlers.FishingHandler
import com.ez2bg.anotherthread.handlers.FishingEvent
import com.ez2bg.anotherthread.handlers.LockpickingHandler
import com.ez2bg.anotherthread.handlers.LockpickingEvent
import com.ez2bg.anotherthread.handlers.PlayerInteractionHandler
import com.ez2bg.anotherthread.handlers.PuzzleHandler
import com.ez2bg.anotherthread.handlers.PuzzleEvent
import com.ez2bg.anotherthread.handlers.RiftHandler
import com.ez2bg.anotherthread.handlers.RiftEvent
import com.ez2bg.anotherthread.handlers.SearchHandler
import com.ez2bg.anotherthread.handlers.SearchEvent
import com.ez2bg.anotherthread.handlers.ShopHandler
import com.ez2bg.anotherthread.handlers.ShopEvent
import com.ez2bg.anotherthread.handlers.TeleportHandler
import com.ez2bg.anotherthread.handlers.TeleportEvent
import com.ez2bg.anotherthread.handlers.TrainerHandler
import com.ez2bg.anotherthread.handlers.TrainerEvent
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.EventLogType
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
    val fishingMinigameData: FishingMinigameStartDto? = null,
    // Lockpicking state
    val showLockpickingMinigame: Boolean = false,
    val lockpickingInfo: LockpickInfoDto? = null,
    val lockpickingLocationId: String? = null,
    // Diplomacy state
    val diplomacyResult: DiplomacyResultDto? = null,
    val isDiplomacyLoading: Boolean = false,
    val hostilityResult: HostilityResultDto? = null,
    // NPC Reaction state
    val reactionResult: ReactionResultDto? = null
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
    val fishingMinigameData: FishingMinigameStartDto? = null,
    // Lockpicking state
    val showLockpickingMinigame: Boolean = false,
    val lockpickingInfo: LockpickInfoDto? = null,
    val lockpickingLocationId: String? = null,  // The location we're trying to enter
    // Diplomacy state
    val diplomacyResult: DiplomacyResultDto? = null,  // Result of diplomacy check
    val isDiplomacyLoading: Boolean = false,
    val hostilityResult: HostilityResultDto? = null,  // Hostility check for selected creature
    // NPC Reaction state
    val reactionResult: ReactionResultDto? = null
) {
    // Derived properties
    val currentLocation: LocationDto?
        get() = locations.find { it.id == currentLocationId }

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
    // Shop/inn detection will be set once location is synced from server
    private val _localState = MutableStateFlow(AdventureLocalState())

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
            fishingMinigameData = local.fishingMinigameData,
            showLockpickingMinigame = local.showLockpickingMinigame,
            lockpickingInfo = local.lockpickingInfo,
            lockpickingLocationId = local.lockpickingLocationId,
            diplomacyResult = local.diplomacyResult,
            isDiplomacyLoading = local.isDiplomacyLoading,
            hostilityResult = local.hostilityResult,
            reactionResult = local.reactionResult
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        // Start with empty state - will be populated once location is synced from server
        initialValue = AdventureUiState()
    )

    init {
        initializeRepository()
        connectCombatWebSocket()
        loadAbilitiesMap()
        // Shop items will be loaded once location is synced from server
        // via the combine flow and listenForUserUpdates
        listenForUserUpdates()
        // Listen for real-time player presence events (enter/leave)
        listenForPlayerPresenceEvents()
        // Listen for WebSocket reconnection to resync location
        listenForConnectionStateChanges()
        // Listen for handler events
        listenForHandlerEvents()
        // Sync handler states to local state for UI reactivity
        syncHandlerStatesToLocalState()
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
                            if (currentRepoLocationId == null || currentRepoLocationId != serverLocationId) {
                                println("[AdventureViewModel] Server location sync: setting repo to $serverLocationId (was $currentRepoLocationId)")
                                AdventureRepository.setCurrentLocation(serverLocationId)
                            } else {
                                println("[AdventureViewModel] Server location sync: no correction needed")
                            }
                            // Refresh location with user context to get discovered items
                            val userId = UserStateHolder.userId
                            if (userId != null) {
                                AdventureRepository.refreshLocationWithUserContext(serverLocationId, userId)
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
        // Initialize the repository WITHOUT a location - we'll set it after server validation
        // The repository will:
        // 1. Load all world data from the server
        // 2. Subscribe to WebSocket events for real-time updates
        // 3. NOT set any location initially (listenForUserUpdates will set it from server)
        //
        // NOTE: We intentionally DON'T use UserStateHolder.currentLocationId here because
        // that may contain stale cached data from localStorage. The correct location will
        // be set by listenForUserUpdates() after session validation with the server.
        println("[AdventureViewModel] initializeRepository() - NOT passing cached location, waiting for server validation")
        AdventureRepository.initialize(null)

        // React to location changes from AdventureRepository
        scope.launch {
            AdventureRepository.currentLocationId.collect { locationId ->
                if (locationId != null) {
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
            AdventureRepository.playerPresenceEvents.collect { event ->
                val currentLocationId = AdventureRepository.currentLocationId.value
                when (event) {
                    is AdventureRepository.PlayerPresenceEvent.PlayerEntered -> {
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
                    is AdventureRepository.PlayerPresenceEvent.PlayerLeft -> {
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
                        // AdventureRepository is the single source of truth for location
                        AdventureRepository.setCurrentLocation(serverLocationId)
                        UserStateHolder.addVisitedLocation(serverLocationId)
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

        // Check if target location is locked
        val targetLocation = AdventureRepository.getLocation(exit.locationId)
        if (targetLocation != null && targetLocation.lockLevel != null && targetLocation.lockLevel > 0) {
            // Location is locked - attempt lockpicking instead of navigating
            attemptLockedDoor(exit.locationId)
            return
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
            // AdventureRepository is the single source of truth for location
            AdventureRepository.setCurrentLocation(exit.locationId)
            // Track visited location for minimap fog-of-war
            UserStateHolder.addVisitedLocation(exit.locationId)

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
                    // Refresh location with user context to get discovered items
                    AdventureRepository.refreshLocationWithUserContext(exit.locationId, userId)
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
                    // AdventureRepository is single source of truth, update it
                    AdventureRepository.setCurrentLocation(response.newLocationId)
                    // Track visited location for minimap fog-of-war
                    UserStateHolder.addVisitedLocation(response.newLocationId)

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

    // Shop functions delegated to ShopHandler
    private fun loadShopItems(locationId: String) = ShopHandler.loadShopItems(locationId)
    private fun loadShopItemsFromApi(locationId: String) = ShopHandler.loadShopItemsFromApi(locationId)
    private fun loadSellableItems(locationId: String) = ShopHandler.loadSellableItems(locationId)
    fun openSellModal() = ShopHandler.openSellModal()
    fun closeSellModal() = ShopHandler.closeSellModal()
    fun sellItem(sellableItem: com.ez2bg.anotherthread.api.SellableItemDto) = ShopHandler.sellItem(sellableItem)

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
                showCreatureInteractionModal = true,
                diplomacyResult = null,
                isDiplomacyLoading = false,
                reactionResult = null
            )
        }
        // Check diplomacy options for this creature
        checkDiplomacy(creature.id)
        // Fetch NPC reaction for this creature
        fetchReaction(creature.id)
    }

    fun dismissCreatureInteractionModal() {
        _localState.update {
            it.copy(
                showCreatureInteractionModal = false,
                selectedCreature = null,
                diplomacyResult = null,
                isDiplomacyLoading = false,
                reactionResult = null
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
    // DIPLOMACY ACTIONS - delegated to DiplomacyHandler
    // =========================================================================

    fun checkDiplomacy(creatureId: String) = DiplomacyHandler.checkDiplomacy(creatureId)
    fun attemptBribe(creatureId: String) = DiplomacyHandler.attemptBribe(creatureId)
    fun attemptParley(creatureId: String) = DiplomacyHandler.attemptParley(creatureId)
    private fun clearDiplomacyState() = DiplomacyHandler.clearDiplomacyState()

    // =========================================================================
    // NPC REACTION
    // =========================================================================

    private fun fetchReaction(creatureId: String) {
        val userId = UserStateHolder.userId ?: return
        scope.launch {
            ApiClient.getCreatureReaction(creatureId, userId).onSuccess { result ->
                _localState.update { it.copy(reactionResult = result) }
            }
        }
    }

    // =========================================================================
    // SHOP ACTIONS (delegated to ShopHandler)
    // =========================================================================

    fun buyItem(itemId: String) {
        val locationId = uiState.value.currentLocationId ?: return
        ShopHandler.buyItem(itemId, locationId)
    }

    fun restAtInn() {
        val locationId = uiState.value.currentLocationId ?: return
        ShopHandler.restAtInn(locationId)
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
                val creatureIds = state.creaturesHere.filter { it.isAggressive }.map { it.id }
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
        val creatures = AdventureRepository.getCreaturesHere()
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
                    // Refresh location data since creature may now be charmed
                    AdventureRepository.refresh()
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
    // SEARCH - delegated to SearchHandler
    // =========================================================================

    fun searchLocation() = SearchHandler.searchLocation()
    fun cancelSearch() = SearchHandler.cancelSearch()

    // =========================================================================
    // FISHING - delegated to FishingHandler
    // =========================================================================

    fun openFishingModal() = FishingHandler.openFishingModal()
    fun closeFishingModal() = FishingHandler.closeFishingModal()
    fun startFishing(distance: String) = FishingHandler.startFishing(distance)
    fun completeFishingMinigame(finalScore: Int) = FishingHandler.completeFishingMinigame(finalScore)
    fun cancelFishing() = FishingHandler.cancelFishing()

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
    // LOCKPICKING - delegated to LockpickingHandler
    // =========================================================================

    fun attemptLockedDoor(locationId: String) = LockpickingHandler.attemptLockedDoor(locationId)
    fun completeLockpicking(accuracy: Float) = LockpickingHandler.completeLockpicking(accuracy)
    fun cancelLockpicking() = LockpickingHandler.cancelLockpicking()

    // =========================================================================
    // TELEPORT - delegated to TeleportHandler
    // =========================================================================

    private fun handleTeleportAbility(ability: AbilityDto) = TeleportHandler.handleTeleportAbility(ability)
    fun selectTeleportDestination(destination: TeleportDestinationDto) = TeleportHandler.selectTeleportDestination(destination)
    fun dismissMapSelection() = TeleportHandler.dismissMapSelection()

    // =========================================================================
    // RIFT PORTAL - delegated to RiftHandler
    // =========================================================================

    private fun handleOpenRiftAbility(ability: AbilityDto) = RiftHandler.handleOpenRiftAbility(ability)
    private fun handleSealRiftAbility(ability: AbilityDto) = RiftHandler.handleSealRiftAbility(ability)
    fun selectRiftToOpen(area: UnconnectedAreaDto) = RiftHandler.selectRiftToOpen(area)
    fun selectRiftToSeal(rift: SealableRiftDto) = RiftHandler.selectRiftToSeal(rift)
    fun dismissRiftSelection() = RiftHandler.dismissRiftSelection()

    // =========================================================================
    // Trainer Methods - delegated to TrainerHandler
    // =========================================================================

    fun openTrainerModal(creature: CreatureDto) = TrainerHandler.openTrainerModal(creature)
    fun learnAbility(abilityId: String) = TrainerHandler.learnAbility(abilityId)
    fun dismissTrainerModal() = TrainerHandler.dismissTrainerModal()

    // =========================================================================
    // PUZZLE INTERACTION - delegated to PuzzleHandler
    // =========================================================================

    fun loadPuzzlesAtLocation(locationId: String) = PuzzleHandler.loadPuzzlesAtLocation(locationId)
    fun openPuzzleModal(puzzle: PuzzleDto) = PuzzleHandler.openPuzzleModal(puzzle)
    fun pullLever(leverId: String) = PuzzleHandler.pullLever(leverId)
    fun dismissPuzzleModal() = PuzzleHandler.dismissPuzzleModal()

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
    // PLAYER INTERACTION - delegated to PlayerInteractionHandler
    // =========================================================================

    fun selectPlayer(player: UserDto) = PlayerInteractionHandler.selectPlayer(player)
    fun dismissPlayerInteractionModal() = PlayerInteractionHandler.dismissPlayerInteractionModal()
    fun showGiveItemModal() = PlayerInteractionHandler.showGiveItemModal()
    fun dismissGiveItemModal() = PlayerInteractionHandler.dismissGiveItemModal()
    fun giveItemToPlayer(itemId: String) = PlayerInteractionHandler.giveItemToPlayer(itemId)
    fun attackPlayer(player: UserDto) = PlayerInteractionHandler.attackPlayer(player)
    fun robPlayer(player: UserDto) = PlayerInteractionHandler.robPlayer(player)
    fun inviteToParty(player: UserDto) = PlayerInteractionHandler.inviteToParty(player)
    fun acceptPartyInvite(player: UserDto) = PlayerInteractionHandler.acceptPartyInvite(player)
    fun leaveParty() = PlayerInteractionHandler.leaveParty()
    fun disbandParty() = PlayerInteractionHandler.disbandParty()

    // =========================================================================
    // HANDLER EVENT LISTENERS
    // =========================================================================

    /**
     * Sync handler state changes to local state for UI reactivity.
     * This allows gradual migration - handlers own the logic, but state still
     * flows through _localState for the combine flow.
     */
    private fun syncHandlerStatesToLocalState() {
        // Sync ShopHandler state
        scope.launch {
            ShopHandler.shopState.collect { shop ->
                _localState.update {
                    it.copy(
                        shopItems = shop.shopItems,
                        sellableItems = shop.sellableItems,
                        showSellModal = shop.showSellModal,
                        isShopBanned = shop.isShopBanned,
                        shopBanMessage = shop.shopBanMessage,
                        playerGold = shop.playerGold
                    )
                }
            }
        }

        // Sync FishingHandler state
        scope.launch {
            FishingHandler.fishingState.collect { fishing ->
                _localState.update {
                    it.copy(
                        isFishing = fishing.isFishing,
                        fishingDurationMs = fishing.fishingDurationMs,
                        showFishingDistanceModal = fishing.showFishingDistanceModal,
                        fishingInfo = fishing.fishingInfo,
                        showFishingMinigame = fishing.showFishingMinigame,
                        fishingMinigameData = fishing.fishingMinigameData
                    )
                }
            }
        }

        // Sync PuzzleHandler state
        scope.launch {
            PuzzleHandler.puzzleState.collect { puzzle ->
                _localState.update {
                    it.copy(
                        showPuzzleModal = puzzle.showPuzzleModal,
                        currentPuzzle = puzzle.currentPuzzle,
                        puzzleProgress = puzzle.puzzleProgress,
                        isLoadingPuzzle = puzzle.isLoadingPuzzle,
                        puzzlesAtLocation = puzzle.puzzlesAtLocation
                    )
                }
            }
        }

        // Sync PlayerInteractionHandler state
        scope.launch {
            PlayerInteractionHandler.state.collect { interaction ->
                _localState.update {
                    it.copy(
                        selectedPlayer = interaction.selectedPlayer,
                        showPlayerInteractionModal = interaction.showPlayerInteractionModal,
                        showGiveItemModal = interaction.showGiveItemModal
                    )
                }
            }
        }

        // Sync TeleportHandler state
        scope.launch {
            TeleportHandler.state.collect { teleport ->
                _localState.update {
                    it.copy(
                        showMapSelection = teleport.showMapSelection,
                        teleportDestinations = teleport.teleportDestinations,
                        teleportAbilityId = teleport.teleportAbilityId
                    )
                }
            }
        }

        // Sync RiftHandler state
        scope.launch {
            RiftHandler.state.collect { rift ->
                _localState.update {
                    it.copy(
                        showRiftSelection = rift.showRiftSelection,
                        riftMode = rift.riftMode,
                        unconnectedAreas = rift.unconnectedAreas,
                        sealableRifts = rift.sealableRifts
                    )
                }
            }
        }

        // Sync TrainerHandler state
        scope.launch {
            TrainerHandler.state.collect { trainer ->
                _localState.update {
                    it.copy(
                        showTrainerModal = trainer.showTrainerModal,
                        trainerInfo = trainer.trainerInfo,
                        isLoadingTrainer = trainer.isLoadingTrainer
                    )
                }
            }
        }

        // Sync DiplomacyHandler state
        scope.launch {
            DiplomacyHandler.state.collect { diplomacy ->
                _localState.update {
                    it.copy(
                        diplomacyResult = diplomacy.diplomacyResult,
                        isDiplomacyLoading = diplomacy.isDiplomacyLoading,
                        hostilityResult = diplomacy.hostilityResult
                    )
                }
            }
        }

        // Sync LockpickingHandler state
        scope.launch {
            LockpickingHandler.state.collect { lockpicking ->
                _localState.update {
                    it.copy(
                        showLockpickingMinigame = lockpicking.showLockpickingMinigame,
                        lockpickingInfo = lockpicking.lockpickingInfo,
                        lockpickingLocationId = lockpicking.lockpickingLocationId
                    )
                }
            }
        }

        // Sync SearchHandler state
        scope.launch {
            SearchHandler.state.collect { search ->
                _localState.update {
                    it.copy(
                        isSearching = search.isSearching,
                        searchDurationMs = search.searchDurationMs
                    )
                }
            }
        }
    }

    /**
     * Listen for events from all handlers and dispatch to appropriate UI actions.
     * Handlers emit events for snackbars, errors, and other one-time UI actions.
     */
    private fun listenForHandlerEvents() {
        // Shop events
        scope.launch {
            ShopHandler.events.collect { event ->
                when (event) {
                    is ShopEvent.ShowSnackbar -> showSnackbar(event.message)
                }
            }
        }

        // Fishing events
        scope.launch {
            FishingHandler.events.collect { event ->
                when (event) {
                    is FishingEvent.ShowSnackbar -> showSnackbar(event.message)
                    is FishingEvent.ShowError -> logError(event.message)
                }
            }
        }

        // Puzzle events
        scope.launch {
            PuzzleHandler.events.collect { event ->
                when (event) {
                    is PuzzleEvent.ShowSnackbar -> showSnackbar(event.message)
                }
            }
        }

        // Teleport events
        scope.launch {
            TeleportHandler.events.collect { event ->
                when (event) {
                    is TeleportEvent.ShowSnackbar -> showSnackbar(event.message)
                    is TeleportEvent.ShowError -> logError(event.message)
                }
            }
        }

        // Rift events
        scope.launch {
            RiftHandler.events.collect { event ->
                when (event) {
                    is RiftEvent.ShowSnackbar -> showSnackbar(event.message)
                    is RiftEvent.ShowError -> logError(event.message)
                }
            }
        }

        // Trainer events
        scope.launch {
            TrainerHandler.events.collect { event ->
                when (event) {
                    is TrainerEvent.ShowSnackbar -> showSnackbar(event.message)
                    is TrainerEvent.AbilitiesUpdated -> loadPlayerAbilities()
                }
            }
        }

        // Diplomacy events
        scope.launch {
            DiplomacyHandler.events.collect { event ->
                when (event) {
                    is DiplomacyEvent.ShowSnackbar -> showSnackbar(event.message)
                    is DiplomacyEvent.ShowError -> logError(event.message)
                    is DiplomacyEvent.CombatAvoided -> dismissCreatureInteractionModal()
                }
            }
        }

        // Lockpicking events
        scope.launch {
            LockpickingHandler.events.collect { event ->
                when (event) {
                    is LockpickingEvent.ShowMessage -> logMessage(event.message)
                    is LockpickingEvent.ShowError -> logError(event.message)
                    is LockpickingEvent.NavigateToLocation -> navigateToExit(ExitDto(locationId = event.locationId))
                }
            }
        }

        // Search events
        scope.launch {
            SearchHandler.events.collect { event ->
                when (event) {
                    is SearchEvent.ShowMessage -> logMessage(event.message)
                    is SearchEvent.ShowError -> logError(event.message)
                }
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
