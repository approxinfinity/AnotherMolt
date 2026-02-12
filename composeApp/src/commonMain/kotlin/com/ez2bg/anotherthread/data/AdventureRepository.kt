package com.ez2bg.anotherthread.data

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.LocationEventType
import com.ez2bg.anotherthread.combat.GlobalEvent
import com.ez2bg.anotherthread.state.CombatStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    // PLAYER PRESENCE EVENTS
    // =========================================================================

    /**
     * Player presence event for real-time player enter/leave notifications.
     */
    sealed class PlayerPresenceEvent {
        data class PlayerEntered(val locationId: String, val playerId: String, val playerName: String) : PlayerPresenceEvent()
        data class PlayerLeft(val locationId: String, val playerId: String, val playerName: String) : PlayerPresenceEvent()
    }

    private val _playerPresenceEvents = MutableSharedFlow<PlayerPresenceEvent>(extraBufferCapacity = 10)
    val playerPresenceEvents: SharedFlow<PlayerPresenceEvent> = _playerPresenceEvents.asSharedFlow()

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
            is GlobalEvent.LocationMutated -> {
                handleLocationMutation(event.event)
            }
            is GlobalEvent.CreatureDefeated -> {
                // When a creature is defeated in combat, remove it from location tracking
                println("[AdventureRepository] Creature defeated: ${event.response.creatureId}")
                removeCreatureFromLocation(event.response.creatureId)
            }
            // Handle other events that affect world state if needed
            else -> { /* Other events handled by CombatStateHolder */ }
        }
    }

    /**
     * Handle a location mutation event from the WebSocket.
     * Updates creature locations, items, exits, and emits player presence events.
     */
    private fun handleLocationMutation(mutation: com.ez2bg.anotherthread.api.LocationMutationEvent) {
        val currentLocId = _currentLocationId.value

        when (mutation.eventType) {
            LocationEventType.CREATURE_REMOVED -> {
                mutation.creatureIdRemoved?.let { creatureId ->
                    println("[AdventureRepository] Creature removed: $creatureId from ${mutation.locationId}")
                    removeCreatureFromLocation(creatureId)
                }
                // Also update location's creatureIds
                updateLocationCreatureIds(mutation.locationId) { ids ->
                    mutation.creatureIdRemoved?.let { ids - it } ?: ids
                }
            }
            LocationEventType.CREATURE_ADDED -> {
                mutation.creatureIdAdded?.let { creatureId ->
                    println("[AdventureRepository] Creature added: $creatureId to ${mutation.locationId}")
                    updateCreatureLocation(creatureId, mutation.locationId)
                }
                // Also update location's creatureIds
                updateLocationCreatureIds(mutation.locationId) { ids ->
                    mutation.creatureIdAdded?.let { if (it !in ids) ids + it else ids } ?: ids
                }
            }
            LocationEventType.EXIT_ADDED -> {
                mutation.exitAdded?.let { exitDto ->
                    updateLocation(mutation.locationId) { loc ->
                        loc.copy(exits = loc.exits + exitDto)
                    }
                }
            }
            LocationEventType.EXIT_REMOVED -> {
                mutation.exitRemoved?.let { exitDto ->
                    updateLocation(mutation.locationId) { loc ->
                        loc.copy(exits = loc.exits.filter { it.locationId != exitDto.locationId })
                    }
                }
            }
            LocationEventType.ITEM_ADDED -> {
                mutation.itemIdAdded?.let { itemId ->
                    updateLocation(mutation.locationId) { loc ->
                        loc.copy(itemIds = loc.itemIds + itemId)
                    }
                }
            }
            LocationEventType.ITEM_REMOVED -> {
                mutation.itemIdRemoved?.let { itemId ->
                    updateLocation(mutation.locationId) { loc ->
                        loc.copy(
                            itemIds = loc.itemIds - itemId,
                            discoveredItemIds = loc.discoveredItemIds - itemId
                        )
                    }
                }
            }
            LocationEventType.PLAYER_ENTERED -> {
                val playerId = mutation.playerId
                val playerName = mutation.playerName
                if (playerId != null && playerName != null && currentLocId == mutation.locationId) {
                    scope.launch {
                        _playerPresenceEvents.emit(
                            PlayerPresenceEvent.PlayerEntered(mutation.locationId, playerId, playerName)
                        )
                    }
                }
            }
            LocationEventType.PLAYER_LEFT -> {
                val playerId = mutation.playerId
                val playerName = mutation.playerName
                if (playerId != null && playerName != null && currentLocId == mutation.locationId) {
                    scope.launch {
                        _playerPresenceEvents.emit(
                            PlayerPresenceEvent.PlayerLeft(mutation.locationId, playerId, playerName)
                        )
                    }
                }
            }
            LocationEventType.LOCATION_UPDATED -> {
                // For general updates, refresh from server
                scope.launch { refresh() }
            }
        }
    }

    /**
     * Update a location in the cache.
     */
    private fun updateLocation(locationId: String, transform: (LocationDto) -> LocationDto) {
        _locations.value = _locations.value.map { loc ->
            if (loc.id == locationId) transform(loc) else loc
        }
    }

    /**
     * Update a location's creatureIds.
     */
    private fun updateLocationCreatureIds(locationId: String, transform: (List<String>) -> List<String>) {
        updateLocation(locationId) { loc ->
            loc.copy(creatureIds = transform(loc.creatureIds))
        }
    }

    /**
     * Update a creature's location. This is called when we receive
     * a CreatureMoved WebSocket event or CREATURE_ADDED location mutation.
     */
    private fun updateCreatureLocation(creatureId: String, newLocationId: String) {
        _creatureLocations.value = _creatureLocations.value.toMutableMap().apply {
            put(creatureId, newLocationId)
        }
    }

    /**
     * Remove a creature from location tracking. This is called when we receive
     * a CREATURE_REMOVED location mutation or CreatureDefeated event.
     */
    private fun removeCreatureFromLocation(creatureId: String) {
        _creatureLocations.value = _creatureLocations.value.toMutableMap().apply {
            remove(creatureId)
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
        println("[AdventureRepository] initialize() called with initialLocationId=$initialLocationId")
        if (_isInitialized.value) {
            println("[AdventureRepository] Already initialized, skipping")
            return
        }

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
            val locationExists = initialLocationId != null && _locations.value.any { it.id == initialLocationId }
            println("[AdventureRepository] initialize: initialLocationId=$initialLocationId, locationExists=$locationExists, totalLocations=${_locations.value.size}")
            val validLocationId = if (locationExists) {
                initialLocationId
            } else {
                // Fallback: find a (0,0) location in any area, preferring "overworld"
                val originLocation = _locations.value.find { it.gridX == 0 && it.gridY == 0 && it.areaId == "overworld" }
                    ?: _locations.value.find { it.gridX == 0 && it.gridY == 0 }
                    ?: _locations.value.firstOrNull()
                println("[AdventureRepository] Falling back to origin: ${originLocation?.id} (grid ${originLocation?.gridX},${originLocation?.gridY})")
                originLocation?.id
            }
            _currentLocationId.value = validLocationId
            println("[AdventureRepository] Set currentLocationId=$validLocationId")

            _isLoading.value = false
            _isInitialized.value = true
        }
    }

    /**
     * Set the current player location.
     * This is the SINGLE SOURCE OF TRUTH for player location.
     *
     * @param locationId The new location ID
     * @param userId Optional user ID - if provided, syncs to server
     */
    fun setCurrentLocation(locationId: String, userId: String? = null) {
        _currentLocationId.value = locationId

        // Sync to server if userId provided
        if (userId != null) {
            scope.launch {
                ApiClient.updateUserLocation(userId, locationId).onFailure { error ->
                    println("[AdventureRepository] Failed to sync location to server: ${error.message}")
                }
            }
        }
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

    /**
     * Refresh a single location with user context.
     * This fetches the location including any puzzle-revealed secret passages.
     */
    fun refreshLocationWithUserContext(locationId: String, userId: String) {
        scope.launch {
            ApiClient.getLocationWithUserContext(locationId, userId)
                .onSuccess { updatedLocation ->
                    // Update just this location in the list
                    _locations.value = _locations.value.map { location ->
                        if (location.id == locationId) updatedLocation else location
                    }
                }
        }
    }

    // =========================================================================
    // DERIVED STATE HELPERS
    // =========================================================================

    /**
     * Get creatures at a specific location.
     * This reads current state synchronously.
     */
    fun getCreaturesAtLocation(locationId: String): List<CreatureDto> {
        val creatureIds = _creatureLocations.value
            .filter { it.value == locationId }
            .keys
        return _creatures.value.filter { it.id in creatureIds }
    }

    /**
     * Get creatures at the current location.
     * Convenience method that uses currentLocationId.
     */
    fun getCreaturesHere(): List<CreatureDto> {
        val locId = _currentLocationId.value ?: return emptyList()
        return getCreaturesAtLocation(locId)
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
