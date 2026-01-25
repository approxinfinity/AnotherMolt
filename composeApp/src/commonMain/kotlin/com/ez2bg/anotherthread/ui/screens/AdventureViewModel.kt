package com.ez2bg.anotherthread.ui.screens

import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CharacterClassDto
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.ExitDto
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.data.AdventureRepository
import com.ez2bg.anotherthread.state.AdventureStateHolder
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.EventLogType
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
    val allAbilitiesMap: Map<String, AbilityDto> = emptyMap()
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
    val snackbarMessage: String? = null
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
            snackbarMessage = local.snackbarMessage
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdventureUiState(currentLocationId = currentUser?.currentLocationId)
    )

    init {
        initializeRepository()
        connectCombatWebSocket()
        loadPlayerAbilities()
        loadAbilitiesMap()
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    private fun initializeRepository() {
        // Initialize the repository with the user's current location
        // The repository will:
        // 1. Load all world data from the server
        // 2. Subscribe to WebSocket events for real-time updates
        AdventureRepository.initialize(currentUser?.currentLocationId)

        // Sync with AdventureStateHolder for event log filtering
        scope.launch {
            AdventureRepository.currentLocationId.collect { locationId ->
                if (locationId != null) {
                    val location = AdventureRepository.getLocation(locationId)
                    location?.let { AdventureStateHolder.setCurrentLocationDirect(it) }
                }
            }
        }
    }

    private fun connectCombatWebSocket() {
        val userId = currentUser?.id ?: return
        CombatStateHolder.connect(userId)
    }

    private fun loadPlayerAbilities() {
        val classId = currentUser?.characterClassId ?: return
        scope.launch {
            ApiClient.getAbilitiesByClass(classId).onSuccess { abilities ->
                val filtered = abilities.filter { it.abilityType != "passive" }
                    .sortedBy { it.name.lowercase() }
                _localState.update { it.copy(playerAbilities = filtered) }
            }
            ApiClient.getCharacterClass(classId).onSuccess { characterClass ->
                _localState.update { it.copy(playerCharacterClass = characterClass) }
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

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    fun navigateToExit(exit: ExitDto) {
        scope.launch {
            // Update repository's current location
            AdventureRepository.setCurrentLocation(exit.locationId)

            // Clear selection
            _localState.update {
                it.copy(
                    selectedCreature = null,
                    selectedItem = null,
                    showDescriptionPopup = false
                )
            }

            // Update user presence on server
            currentUser?.let { user ->
                ApiClient.updateUserLocation(user.id, exit.locationId)
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
    // ABILITIES
    // =========================================================================

    fun handleAbilityClick(ability: AbilityDto) {
        val state = uiState.value

        if (!CombatStateHolder.isInCombat) {
            showSnackbar("Not in combat")
            return
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
