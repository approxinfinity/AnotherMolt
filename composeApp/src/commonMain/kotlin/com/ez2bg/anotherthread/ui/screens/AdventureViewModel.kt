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
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.data.AdventureRepository
import com.ez2bg.anotherthread.state.AdventureStateHolder
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.EventLogType
import com.ez2bg.anotherthread.state.UserStateHolder
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
    val playerCharacterClass: CharacterClassDto? = null,
    val allAbilitiesMap: Map<String, AbilityDto> = emptyMap(),
    // Shop state
    val playerGold: Int = 0,
    val shopItems: List<ItemDto> = emptyList(),
    val isShopLocation: Boolean = false,
    val isInnLocation: Boolean = false,
    // Teleport state
    val showMapSelection: Boolean = false,
    val teleportDestinations: List<TeleportDestinationDto> = emptyList(),
    val teleportAbilityId: String? = null,
    // Phasewalk destinations (for directions without exits)
    val phasewalkDestinations: List<PhasewalkDestinationDto> = emptyList(),
    // Rift state
    val showRiftSelection: Boolean = false,
    val riftMode: String? = null,  // "open" or "seal"
    val unconnectedAreas: List<UnconnectedAreaDto> = emptyList(),
    val sealableRifts: List<SealableRiftDto> = emptyList()
)

/**
 * Complete UI state for AdventureScreen.
 * Combines repository state with local UI state.
 */
data class AdventureUiState(
    // Loading state (from repository)
    val isLoading: Boolean = true,

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
    // Teleport state
    val showMapSelection: Boolean = false,
    val teleportDestinations: List<TeleportDestinationDto> = emptyList(),
    val teleportAbilityId: String? = null,
    // Phasewalk destinations (for directions without exits)
    val phasewalkDestinations: List<PhasewalkDestinationDto> = emptyList(),
    // Rift state
    val showRiftSelection: Boolean = false,
    val riftMode: String? = null,  // "open" or "seal"
    val unconnectedAreas: List<UnconnectedAreaDto> = emptyList(),
    val sealableRifts: List<SealableRiftDto> = emptyList()
) {
    // Derived properties
    val currentLocation: LocationDto?
        get() = locations.find { it.id == currentLocationId }

    val isRanger: Boolean
        get() = playerCharacterClass?.name == "Ranger"

    val isDetailViewVisible: Boolean
        get() = selectedCreature != null || selectedItem != null

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
}

/**
 * ViewModel for AdventureScreen.
 *
 * This ViewModel:
 * - Collects reactive state from AdventureRepository
 * - Repository subscribes to WebSocket events for real-time updates
 * - UI automatically reacts when creatures move via WebSocket
 */
class AdventureViewModel(
    private val currentUser: UserDto?
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Local UI state (selections, targeting, etc.)
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
            locations = locations,
            currentLocationId = currentLocationId,
            allCreatures = creatures,
            creatureLocations = creatureLocations,
            creatureStates = creatureStates,
            allItems = items,
            allAbilitiesMap = local.allAbilitiesMap,
            playerAbilities = local.playerAbilities,
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
            showMapSelection = local.showMapSelection,
            teleportDestinations = local.teleportDestinations,
            teleportAbilityId = local.teleportAbilityId,
            phasewalkDestinations = local.phasewalkDestinations,
            showRiftSelection = local.showRiftSelection,
            riftMode = local.riftMode,
            unconnectedAreas = local.unconnectedAreas,
            sealableRifts = local.sealableRifts
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdventureUiState(currentLocationId = currentUser?.currentLocationId)
    )

    // Known shop location IDs
    private val shopLocationIds = setOf(
        "tun-du-lac-magic-shop",
        "tun-du-lac-armor-shop",
        "tun-du-lac-weapons-shop"
    )
    private val innLocationId = "tun-du-lac-inn"

    init {
        initializeRepository()
        connectCombatWebSocket()
        loadPlayerAbilities()
        loadAbilitiesMap()
        loadPlayerGold()
        loadPhasewalkDestinations()
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
        AdventureRepository.initialize(currentUser?.currentLocationId)

        // Sync with AdventureStateHolder for event log filtering
        // Also update server if location was changed due to fallback
        scope.launch {
            var firstLocationUpdate = true
            AdventureRepository.currentLocationId.collect { locationId ->
                if (locationId != null) {
                    val location = AdventureRepository.getLocation(locationId)
                    location?.let { AdventureStateHolder.setCurrentLocationDirect(it) }

                    // If this is the first update and it differs from user's stored location,
                    // update the server (this handles the fallback case)
                    if (firstLocationUpdate && currentUser != null && locationId != currentUser.currentLocationId) {
                        ApiClient.updateUserLocation(currentUser.id, locationId)
                    }
                    firstLocationUpdate = false
                }
            }
        }
    }

    private fun connectCombatWebSocket() {
        val userId = currentUser?.id ?: return
        CombatStateHolder.connect(userId)
    }

    private fun loadPlayerAbilities() {
        scope.launch {
            val classAbilities = mutableListOf<AbilityDto>()
            val itemAbilities = mutableListOf<AbilityDto>()

            // 1. Load class abilities
            val classId = currentUser?.characterClassId
            if (classId != null) {
                ApiClient.getAbilitiesByClass(classId).getOrNull()?.let { abilities ->
                    classAbilities.addAll(abilities)
                }
                ApiClient.getCharacterClass(classId).getOrNull()?.let { characterClass ->
                    _localState.update { it.copy(playerCharacterClass = characterClass) }
                }
            }

            // 2. Load item abilities from equipped items
            val equippedItemIds = currentUser?.equippedItemIds ?: emptyList()
            if (equippedItemIds.isNotEmpty()) {
                val allItems = ApiClient.getItems().getOrNull() ?: emptyList()
                val equippedItems = allItems.filter { it.id in equippedItemIds }
                val abilityIdsFromItems = equippedItems.flatMap { it.abilityIds }.distinct()
                // Fetch each ability sequentially to ensure all complete
                for (abilityId in abilityIdsFromItems) {
                    ApiClient.getAbility(abilityId).getOrNull()?.let { ability ->
                        itemAbilities.add(ability)
                    }
                }
            }

            // 3. Combine class + item abilities
            val allAbilities = (classAbilities + itemAbilities)
                .filter { it.abilityType != "passive" }
                .sortedBy { it.name.lowercase() }
            _localState.update { it.copy(playerAbilities = allAbilities) }
        }
    }

    private fun loadAbilitiesMap() {
        scope.launch {
            ApiClient.getAbilities().onSuccess { abilities ->
                _localState.update { it.copy(allAbilitiesMap = abilities.associateBy { it.id }) }
            }
        }
    }

    private fun loadPlayerGold() {
        val userId = currentUser?.id ?: return
        _localState.update { it.copy(playerGold = currentUser.gold) }
        scope.launch {
            ApiClient.getUser(userId).onSuccess { user ->
                if (user != null) {
                    _localState.update { it.copy(playerGold = user.gold) }
                }
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
            showSnackbar("You cannot move while incapacitated")
            return
        }

        scope.launch {
            // Update repository's current location
            AdventureRepository.setCurrentLocation(exit.locationId)

            // Clear selection and update shop state
            val isShop = exit.locationId in shopLocationIds
            val isInn = exit.locationId == innLocationId
            _localState.update {
                it.copy(
                    selectedCreature = null,
                    selectedItem = null,
                    showDescriptionPopup = false,
                    isShopLocation = isShop,
                    isInnLocation = isInn,
                    shopItems = emptyList()
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
            }

            // Update user presence on server
            currentUser?.let { user ->
                ApiClient.updateUserLocation(user.id, exit.locationId)
            }

            // Load phasewalk destinations for the new location
            loadPhasewalkDestinations()
        }
    }

    /**
     * Reload phasewalk destinations from server.
     * Called on init, location change, and after equipment changes.
     * Public so it can be triggered when user equips/unequips items.
     */
    fun loadPhasewalkDestinations() {
        val userId = currentUser?.id ?: return
        scope.launch {
            ApiClient.getPhasewalkDestinations(userId).onSuccess { destinations ->
                println("[Phasewalk] Loaded ${destinations.size} destinations for user $userId")
                _localState.update { it.copy(phasewalkDestinations = destinations) }
            }.onFailure { error ->
                println("[Phasewalk] Failed to load destinations: ${error.message}")
                _localState.update { it.copy(phasewalkDestinations = emptyList()) }
            }
        }
    }

    fun phasewalk(direction: String) {
        // Block movement if player is downed (HP <= 0)
        // Fall back to passed-in currentUser if UserStateHolder isn't initialized yet
        val playerHp = UserStateHolder.currentUser.value?.currentHp
            ?: currentUser?.currentHp
            ?: 0
        if (playerHp <= 0) {
            showSnackbar("You cannot phasewalk while incapacitated")
            return
        }

        val userId = currentUser?.id ?: return
        scope.launch {
            ApiClient.phasewalk(userId, direction).onSuccess { response ->
                if (response.success && response.newLocationId != null) {
                    // Deduct mana locally for immediate UI feedback (2 mana cost)
                    UserStateHolder.spendManaLocally(2)

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

                    // Update user presence on server
                    ApiClient.updateUserLocation(userId, response.newLocationId)

                    // Load phasewalk destinations for the new location
                    loadPhasewalkDestinations()
                } else {
                    showSnackbar(response.message)
                }
            }.onFailure {
                showSnackbar("Failed to phasewalk: ${it.message}")
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

    fun refresh() {
        AdventureRepository.refresh()
    }

    // =========================================================================
    // DETAIL VIEW
    // =========================================================================

    fun selectCreature(creature: CreatureDto) {
        _localState.update { it.copy(selectedCreature = creature, selectedItem = null) }
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
        val userId = currentUser?.id ?: return
        val locationId = uiState.value.currentLocationId ?: return
        scope.launch {
            ApiClient.buyItem(locationId, userId, itemId).onSuccess { response ->
                if (response.success) {
                    // Update gold from response
                    response.user?.let { user ->
                        _localState.update { it.copy(playerGold = user.gold) }
                    }
                    showSnackbar(response.message)
                } else {
                    showSnackbar(response.message)
                }
            }.onFailure {
                showSnackbar("Purchase failed: ${it.message}")
            }
        }
    }

    fun restAtInn() {
        val userId = currentUser?.id ?: return
        val locationId = uiState.value.currentLocationId ?: return
        scope.launch {
            ApiClient.restAtInn(locationId, userId).onSuccess { response ->
                if (response.success) {
                    response.user?.let { user ->
                        _localState.update { it.copy(playerGold = user.gold) }
                    }
                    showSnackbar(response.message)
                } else {
                    showSnackbar(response.message)
                }
            }.onFailure {
                showSnackbar("Rest failed: ${it.message}")
            }
        }
    }

    // =========================================================================
    // ITEM PICKUP
    // =========================================================================

    fun pickupItem(item: ItemDto) {
        val userId = currentUser?.id
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
                val playerName = currentUser?.name ?: "You"
                CombatStateHolder.addEventLogEntry("$playerName picked up ${item.name}", EventLogType.LOOT)
                // Update user state with new inventory
                UserStateHolder.updateUser(updatedUser)
                // Refresh the location data to reflect item removal
                AdventureRepository.refresh()
            }.onFailure { error ->
                val message = error.message ?: "Failed to pick up item"
                // Parse common error messages for better UX
                when {
                    message.contains("Inventory full") -> showSnackbar("Inventory full!")
                    message.contains("not at this location") -> showSnackbar("Item is no longer here")
                    else -> showSnackbar(message)
                }
            }
        }
    }

    // =========================================================================
    // ABILITIES
    // =========================================================================

    fun handleAbilityClick(ability: AbilityDto) {
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

        if (!CombatStateHolder.isInCombat) {
            // AoE abilities can initiate combat with all hostiles
            if (ability.targetType in listOf("area", "all_enemies")) {
                val creatureIds = state.creaturesHere.map { it.id }
                if (creatureIds.isNotEmpty()) {
                    CombatStateHolder.joinCombat(creatureIds)
                    showSnackbar("Engaging all enemies with ${ability.name}!")
                } else {
                    showSnackbar("No enemies nearby")
                    return
                }
            } else {
                showSnackbar("Not in combat")
                return
            }
        }

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
                CombatStateHolder.useAbility(ability.id, currentUser?.id)
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
                showSnackbar("Failed to load destinations")
            }
        }
    }

    fun selectTeleportDestination(destination: TeleportDestinationDto) {
        val userId = currentUser?.id ?: return
        val abilityId = _localState.value.teleportAbilityId ?: return

        scope.launch {
            // Dismiss overlay immediately
            _localState.update { it.copy(showMapSelection = false) }

            // Departure message
            val userName = currentUser?.name ?: "Unknown"
            CombatStateHolder.addEventLogEntry(
                "With a soft pop $userName dematerializes.",
                EventLogType.NAVIGATION
            )

            // Call teleport API
            ApiClient.teleport(userId, destination.areaId, abilityId).onSuccess { response ->
                if (response.success && response.newLocationId != null) {
                    // Update location
                    AdventureRepository.setCurrentLocation(response.newLocationId)

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
                showSnackbar("Teleport failed: ${it.message}")
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

        val userId = currentUser?.id ?: return
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
                showSnackbar("Failed to find realms: ${it.message}")
            }
        }
    }

    private fun handleSealRiftAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            showSnackbar("Cannot seal rifts during combat!")
            return
        }

        val userId = currentUser?.id ?: return
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
                showSnackbar("Failed to find rifts: ${it.message}")
            }
        }
    }

    fun selectRiftToOpen(area: UnconnectedAreaDto) {
        val userId = currentUser?.id ?: return

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
                showSnackbar("Failed to open rift: ${it.message}")
            }

            _localState.update {
                it.copy(riftMode = null, unconnectedAreas = emptyList())
            }
        }
    }

    fun selectRiftToSeal(rift: SealableRiftDto) {
        val userId = currentUser?.id ?: return

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
                showSnackbar("Failed to seal rift: ${it.message}")
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
            // Start combat by joining with this creature
            CombatStateHolder.joinCombat(listOf(creature.id))
            showSnackbar("Engaging ${creature.name}!")
        }

        // Use the ability on the creature
        CombatStateHolder.useAbility(ability.id, creature.id)
        showSnackbar("${ability.name} -> ${creature.name}")
    }

    // =========================================================================
    // SNACKBAR
    // =========================================================================

    private fun showSnackbar(message: String) {
        _localState.update { it.copy(snackbarMessage = message) }
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
