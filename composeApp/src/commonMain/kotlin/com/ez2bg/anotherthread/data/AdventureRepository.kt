package com.ez2bg.anotherthread.data

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.combat.GlobalEvent
import com.ez2bg.anotherthread.state.CombatStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * Repository for adventure world data that reacts to WebSocket events.
 *
 * This repository:
 * - Holds cached world data (creatures, items, locations)
 * - Subscribes to WebSocket events for real-time updates
 * - Exposes reactive state that the UI can collect
 *
 * When a CreatureMoved event arrives via WebSocket, this repository
 * immediately updates which creatures are at which location, and the UI
 * automatically reacts.
 */
object AdventureRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // =========================================================================
    // CREATURE DATA
    // =========================================================================

    /** All creatures in the game world */
    private val _creatures = MutableStateFlow<List<CreatureDto>>(emptyList())
    val creatures: StateFlow<List<CreatureDto>> = _creatures.asStateFlow()

    /**
     * Creature locations: creatureId -> locationId
     * This is updated in real-time by WebSocket events.
     */
    private val _creatureLocations = MutableStateFlow<Map<String, String>>(emptyMap())
    val creatureLocations: StateFlow<Map<String, String>> = _creatureLocations.asStateFlow()

    /** Creature states: creatureId -> state (wandering, in_combat, idle) */
    private val _creatureStates = MutableStateFlow<Map<String, String>>(emptyMap())
    val creatureStates: StateFlow<Map<String, String>> = _creatureStates.asStateFlow()

    // =========================================================================
    // LOCATION DATA
    // =========================================================================

    /** All locations in the game world */
    private val _locations = MutableStateFlow<List<LocationDto>>(emptyList())
    val locations: StateFlow<List<LocationDto>> = _locations.asStateFlow()

    /** Current player location ID */
    private val _currentLocationId = MutableStateFlow<String?>(null)
    val currentLocationId: StateFlow<String?> = _currentLocationId.asStateFlow()

    // =========================================================================
    // ITEM DATA
    // =========================================================================

    /** All items in the game world */
    private val _items = MutableStateFlow<List<ItemDto>>(emptyList())
    val items: StateFlow<List<ItemDto>> = _items.asStateFlow()

    // =========================================================================
    // LOADING STATE
    // =========================================================================

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // =========================================================================
    // WEBSOCKET SUBSCRIPTION
    // =========================================================================

    private var isSubscribedToWebSocket = false

    /**
     * Subscribe to WebSocket events from CombatStateHolder.
     * Call this once when the repository is initialized.
     */
    private fun subscribeToWebSocketEvents() {
        if (isSubscribedToWebSocket) return
        isSubscribedToWebSocket = true

        scope.launch {
            CombatStateHolder.globalEvents.collect { event ->
                handleGlobalEvent(event)
            }
        }
    }

    private fun handleGlobalEvent(event: GlobalEvent) {
        when (event) {
            is GlobalEvent.CreatureMoved -> {
                // Update creature location in real-time
                updateCreatureLocation(event.creatureId, event.toLocationId)
            }
            // Handle other events that affect world state if needed
            else -> { /* Other events handled by CombatStateHolder */ }
        }
    }

    /**
     * Update a creature's location. This is called when we receive
     * a CreatureMoved WebSocket event.
     */
    private fun updateCreatureLocation(creatureId: String, newLocationId: String) {
        _creatureLocations.value = _creatureLocations.value.toMutableMap().apply {
            put(creatureId, newLocationId)
        }
    }

    // =========================================================================
    // INITIALIZATION & DATA LOADING
    // =========================================================================

    /**
     * Initialize the repository with initial data from the server.
     * This should be called once when entering adventure mode.
     */
    fun initialize(initialLocationId: String? = null) {
        if (_isInitialized.value) return

        subscribeToWebSocketEvents()

        scope.launch {
            _isLoading.value = true

            // Load all data in parallel
            val locationsDeferred = scope.launch {
                ApiClient.getLocations().onSuccess { locationList ->
                    _locations.value = locationList

                    // Build initial creature locations from location.creatureIds
                    val creatureLocs = mutableMapOf<String, String>()
                    locationList.forEach { location ->
                        location.creatureIds.forEach { creatureId ->
                            creatureLocs[creatureId] = location.id
                        }
                    }
                    _creatureLocations.value = creatureLocs
                }
            }

            val creaturesDeferred = scope.launch {
                ApiClient.getCreatures().onSuccess { creatureList ->
                    _creatures.value = creatureList
                }
            }

            val itemsDeferred = scope.launch {
                ApiClient.getItems().onSuccess { itemList ->
                    _items.value = itemList
                }
            }

            val statesDeferred = scope.launch {
                ApiClient.getCreatureStates().onSuccess { states ->
                    _creatureStates.value = states
                }
            }

            // Wait for all to complete
            locationsDeferred.join()
            creaturesDeferred.join()
            itemsDeferred.join()
            statesDeferred.join()

            // Set initial location with fallback to origin
            val validLocationId = if (initialLocationId != null && _locations.value.any { it.id == initialLocationId }) {
                initialLocationId
            } else {
                // Fallback: find a (0,0) location in any area, preferring "overworld"
                val originLocation = _locations.value.find { it.gridX == 0 && it.gridY == 0 && it.areaId == "overworld" }
                    ?: _locations.value.find { it.gridX == 0 && it.gridY == 0 }
                    ?: _locations.value.firstOrNull()
                originLocation?.id
            }
            _currentLocationId.value = validLocationId

            _isLoading.value = false
            _isInitialized.value = true
        }
    }

    /**
     * Set the current player location.
     */
    fun setCurrentLocation(locationId: String) {
        _currentLocationId.value = locationId
    }

    /**
     * Refresh all data from the server.
     * Only shows loading indicator on initial load, not during background refreshes.
     */
    fun refresh() {
        scope.launch {
            // Only show loading indicator if we don't have data yet
            val isInitialLoad = !_isInitialized.value
            if (isInitialLoad) {
                _isLoading.value = true
            }

            ApiClient.getLocations().onSuccess { locationList ->
                _locations.value = locationList

                // Rebuild creature locations from server data
                val creatureLocs = mutableMapOf<String, String>()
                locationList.forEach { location ->
                    location.creatureIds.forEach { creatureId ->
                        creatureLocs[creatureId] = location.id
                    }
                }
                _creatureLocations.value = creatureLocs
            }

            ApiClient.getCreatures().onSuccess { creatureList ->
                _creatures.value = creatureList
            }

            ApiClient.getItems().onSuccess { itemList ->
                _items.value = itemList
            }

            ApiClient.getCreatureStates().onSuccess { states ->
                _creatureStates.value = states
            }

            if (isInitialLoad) {
                _isLoading.value = false
                _isInitialized.value = true
            }
        }
    }

    // =========================================================================
    // DERIVED STATE HELPERS
    // =========================================================================

    /**
     * Get creatures at a specific location.
     * This is reactive - it updates when creatureLocations changes.
     */
    fun getCreaturesAtLocation(locationId: String): List<CreatureDto> {
        val creatureIds = _creatureLocations.value
            .filter { it.value == locationId }
            .keys
        return _creatures.value.filter { it.id in creatureIds }
    }

    /**
     * Get items at a specific location.
     */
    fun getItemsAtLocation(locationId: String): List<ItemDto> {
        val location = _locations.value.find { it.id == locationId }
        return location?.itemIds?.let { itemIds ->
            _items.value.filter { it.id in itemIds }
        } ?: emptyList()
    }

    /**
     * Get a location by ID.
     */
    fun getLocation(locationId: String): LocationDto? {
        return _locations.value.find { it.id == locationId }
    }

    /**
     * Get the current location.
     */
    fun getCurrentLocation(): LocationDto? {
        val locId = _currentLocationId.value ?: return null
        return getLocation(locId)
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    /**
     * Clear all cached data. Call when leaving adventure mode.
     */
    fun clear() {
        _creatures.value = emptyList()
        _creatureLocations.value = emptyMap()
        _creatureStates.value = emptyMap()
        _locations.value = emptyList()
        _items.value = emptyList()
        _currentLocationId.value = null
        _isInitialized.value = false
        _isLoading.value = false
    }
}
