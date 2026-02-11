package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.ExitDto
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.LocationEventType
import com.ez2bg.anotherthread.api.LocationMutationEvent
import com.ez2bg.anotherthread.data.AdventureRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Navigation event for Adventure mode.
 */
sealed class NavigationEvent {
    data class MovedToLocation(val locationId: String, val locationName: String) : NavigationEvent()
    data class NavigationBlocked(val reason: String) : NavigationEvent()
}

/**
 * Player presence event for real-time player enter/leave notifications.
 */
sealed class PlayerPresenceEvent {
    data class PlayerEntered(val locationId: String, val playerId: String, val playerName: String) : PlayerPresenceEvent()
    data class PlayerLeft(val locationId: String, val playerId: String, val playerName: String) : PlayerPresenceEvent()
}

/**
 * Singleton state holder for Adventure mode business logic.
 * Manages current location, navigation, and entities at location.
 *
 * This separates adventure business logic from UI composables.
 */
object AdventureStateHolder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // All locations (cached for navigation)
    private val _allLocations = MutableStateFlow<List<LocationDto>>(emptyList())
    val allLocations: StateFlow<List<LocationDto>> = _allLocations.asStateFlow()

    // Current location the player is at
    private val _currentLocation = MutableStateFlow<LocationDto?>(null)
    val currentLocation: StateFlow<LocationDto?> = _currentLocation.asStateFlow()

    // Creatures at current location
    private val _creaturesHere = MutableStateFlow<List<CreatureDto>>(emptyList())
    val creaturesHere: StateFlow<List<CreatureDto>> = _creaturesHere.asStateFlow()

    // Creature states (creatureId -> state string like "wandering", "in_combat", "idle")
    private val _creatureStates = MutableStateFlow<Map<String, String>>(emptyMap())
    val creatureStates: StateFlow<Map<String, String>> = _creatureStates.asStateFlow()

    // Items at current location
    private val _itemsHere = MutableStateFlow<List<ItemDto>>(emptyList())
    val itemsHere: StateFlow<List<ItemDto>> = _itemsHere.asStateFlow()

    // All creatures (cached for lookups)
    private val _allCreatures = MutableStateFlow<List<CreatureDto>>(emptyList())
    val allCreatures: StateFlow<List<CreatureDto>> = _allCreatures.asStateFlow()

    // All items (cached for lookups)
    private val _allItems = MutableStateFlow<List<ItemDto>>(emptyList())
    val allItems: StateFlow<List<ItemDto>> = _allItems.asStateFlow()

    // All abilities (cached for ability ring)
    private val _allAbilities = MutableStateFlow<Map<String, AbilityDto>>(emptyMap())
    val allAbilities: StateFlow<Map<String, AbilityDto>> = _allAbilities.asStateFlow()

    // Selected entity for detail view (only one at a time)
    private val _selectedCreature = MutableStateFlow<CreatureDto?>(null)
    val selectedCreature: StateFlow<CreatureDto?> = _selectedCreature.asStateFlow()

    private val _selectedItem = MutableStateFlow<ItemDto?>(null)
    val selectedItem: StateFlow<ItemDto?> = _selectedItem.asStateFlow()

    // Loading states
    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation.asStateFlow()

    // Navigation events (for one-time event handling)
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 10)
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    // Player presence events (for real-time player enter/leave notifications)
    private val _playerPresenceEvents = MutableSharedFlow<PlayerPresenceEvent>(extraBufferCapacity = 10)
    val playerPresenceEvents: SharedFlow<PlayerPresenceEvent> = _playerPresenceEvents.asSharedFlow()

    // Track current user ID for API calls
    private var currentUserId: String? = null

    /**
     * Initialize adventure state for a user.
     */
    fun initialize(userId: String, initialLocationId: String?) {
        currentUserId = userId

        scope.launch {
            // Load all locations
            ApiClient.getLocations().onSuccess { locations ->
                _allLocations.value = locations

                // Set initial location - fetch with user context for discovered items
                if (initialLocationId != null) {
                    ApiClient.getLocation(initialLocationId).onSuccess { freshLocation ->
                        if (freshLocation != null) {
                            setCurrentLocation(freshLocation)
                            // Update cache with user-specific data
                            _allLocations.value = _allLocations.value.map {
                                if (it.id == initialLocationId) freshLocation else it
                            }
                        }
                    }.onFailure {
                        // Fallback to cached location if fetch fails
                        val location = locations.find { it.id == initialLocationId }
                        if (location != null) {
                            setCurrentLocation(location)
                        }
                    }
                }
            }

            // Load all creatures
            ApiClient.getCreatures().onSuccess { creatures ->
                _allCreatures.value = creatures
            }

            // Load all items
            ApiClient.getItems().onSuccess { items ->
                _allItems.value = items
            }

            // Load all abilities
            ApiClient.getAbilities().onSuccess { abilities ->
                _allAbilities.value = abilities.associateBy { it.id }
            }

            // Load creature states
            ApiClient.getCreatureStates().onSuccess { states ->
                _creatureStates.value = states
            }
        }
    }

    /**
     * Navigate to a location by ID.
     */
    fun navigateTo(locationId: String) {
        val location = _allLocations.value.find { it.id == locationId }
        if (location != null) {
            // Store old location for rollback if needed
            val oldLocation = _currentLocation.value

            // Optimistically update the UI
            setCurrentLocation(location)

            // Update user's current location on server
            currentUserId?.let { userId ->
                scope.launch {
                    ApiClient.updateUserLocation(userId, locationId).onSuccess {
                        // Server accepted the move - fetch location with user context for discovered items
                        ApiClient.getLocation(locationId).onSuccess { freshLocation ->
                            if (freshLocation != null) {
                                setCurrentLocation(freshLocation)
                                // Update cache with user-specific data
                                _allLocations.value = _allLocations.value.map {
                                    if (it.id == locationId) freshLocation else it
                                }
                            }
                        }

                        // Emit navigation event
                        _navigationEvents.emit(NavigationEvent.MovedToLocation(locationId, location.name))

                        // Add to event log with coordinates for debugging
                        val coordStr = if (location.gridX != null && location.gridY != null) {
                            " (${location.gridX},${location.gridY})"
                        } else ""
                        CombatStateHolder.addEventLogEntry(
                            "Arrived at ${location.name}$coordStr",
                            EventLogType.NAVIGATION
                        )
                    }.onFailure { error ->
                        // Server rejected the move - rollback and show error
                        oldLocation?.let { setCurrentLocation(it) }

                        val errorMessage = error.message ?: "Cannot move to that location"
                        _navigationEvents.emit(NavigationEvent.NavigationBlocked(errorMessage))
                        CombatStateHolder.addEventLogEntry(
                            errorMessage,
                            EventLogType.ERROR
                        )
                    }
                }
            }
        }
    }

    /**
     * Navigate in a direction from current location.
     */
    fun navigateDirection(direction: ExitDirection) {
        val current = _currentLocation.value ?: return
        val exit = current.exits.find { it.direction == direction } ?: return
        navigateTo(exit.locationId)
    }

    /**
     * Set the current location and update entities here.
     */
    private fun setCurrentLocation(location: LocationDto) {
        _currentLocation.value = location
        updateEntitiesAtLocation(location)
        clearSelection()
    }

    /**
     * Set the current location directly (for external callers like AdventureScreen).
     * Only updates the location reference without clearing selection or triggering other side effects.
     */
    fun setCurrentLocationDirect(location: LocationDto) {
        _currentLocation.value = location
    }

    /**
     * Update creatures and items at the current location.
     * Uses the location's creatureIds and itemIds lists.
     */
    private fun updateEntitiesAtLocation(location: LocationDto) {
        // Filter creatures by IDs in this location
        val creaturesAtLoc = _allCreatures.value.filter { creature ->
            creature.id in location.creatureIds
        }
        _creaturesHere.value = creaturesAtLoc

        // Filter items by IDs in this location
        val itemsAtLoc = _allItems.value.filter { item ->
            item.id in location.itemIds
        }
        _itemsHere.value = itemsAtLoc
    }

    /**
     * Refresh current location data from server.
     */
    fun refreshCurrentLocation() {
        val locationId = _currentLocation.value?.id ?: return

        scope.launch {
            _isLoadingLocation.value = true

            // Refresh location
            ApiClient.getLocation(locationId).onSuccess { location ->
                if (location != null) {
                    _currentLocation.value = location

                    // Also update in allLocations cache
                    _allLocations.value = _allLocations.value.map {
                        if (it.id == locationId) location else it
                    }
                }
            }

            // Refresh creatures
            ApiClient.getCreatures().onSuccess { creatures ->
                _allCreatures.value = creatures
                _currentLocation.value?.let { loc -> updateEntitiesAtLocation(loc) }
            }

            // Refresh items
            ApiClient.getItems().onSuccess { items ->
                _allItems.value = items
                _currentLocation.value?.let { loc -> updateEntitiesAtLocation(loc) }
            }

            // Refresh creature states
            ApiClient.getCreatureStates().onSuccess { states ->
                _creatureStates.value = states
            }

            _isLoadingLocation.value = false
        }
    }

    /**
     * Select a creature for detail view.
     */
    fun selectCreature(creature: CreatureDto?) {
        _selectedCreature.value = creature
        _selectedItem.value = null // Clear item selection
    }

    /**
     * Select an item for detail view.
     */
    fun selectItem(item: ItemDto?) {
        _selectedItem.value = item
        _selectedCreature.value = null // Clear creature selection
    }

    /**
     * Clear all selections.
     */
    fun clearSelection() {
        _selectedCreature.value = null
        _selectedItem.value = null
    }

    /**
     * Check if any entity is selected.
     */
    val hasSelection: Boolean
        get() = _selectedCreature.value != null || _selectedItem.value != null

    /**
     * Get available exits from current location.
     */
    val availableExits: List<ExitDto>
        get() = _currentLocation.value?.exits ?: emptyList()

    /**
     * Get creature state for a creature ID.
     * Returns state string like "wandering", "in_combat", "idle"
     */
    fun getCreatureState(creatureId: String): String? {
        return _creatureStates.value[creatureId]
    }

    /**
     * Handle a location mutation event from the WebSocket.
     * Updates the location in cache and current location if it matches.
     */
    fun handleLocationMutation(event: LocationMutationEvent) {
        val currentLoc = _currentLocation.value
        val currentLocId = currentLoc?.id ?: AdventureRepository.currentLocationId.value

        // For player enter/leave events, we need to emit them even if _currentLocation
        // isn't set yet (could happen during initialization). We compare against
        // currentLocationId from either source.
        if (event.eventType == LocationEventType.PLAYER_ENTERED ||
            event.eventType == LocationEventType.PLAYER_LEFT) {
            val playerId = event.playerId
            val playerName = event.playerName
            if (playerId != null && playerName != null && currentLocId == event.locationId) {
                scope.launch {
                    if (event.eventType == LocationEventType.PLAYER_ENTERED) {
                        _playerPresenceEvents.emit(
                            PlayerPresenceEvent.PlayerEntered(event.locationId, playerId, playerName)
                        )
                    } else {
                        _playerPresenceEvents.emit(
                            PlayerPresenceEvent.PlayerLeft(event.locationId, playerId, playerName)
                        )
                    }
                }
            }
            // Still update cache but don't need to update currentLocation for these events
            updateLocationInCache(event)
            return
        }

        // For other event types, only process if this mutation is for our current location
        if (currentLoc == null || currentLoc.id != event.locationId) {
            // Still update allLocations cache for minimap consistency
            updateLocationInCache(event)
            return
        }

        // Update the current location based on event type
        val updatedLocation = when (event.eventType) {
            LocationEventType.EXIT_ADDED -> {
                event.exitAdded?.let { exitDto ->
                    currentLoc.copy(
                        exits = currentLoc.exits + exitDto
                    )
                } ?: currentLoc
            }
            LocationEventType.EXIT_REMOVED -> {
                event.exitRemoved?.let { exitDto ->
                    currentLoc.copy(
                        exits = currentLoc.exits.filter { it.locationId != exitDto.locationId }
                    )
                } ?: currentLoc
            }
            LocationEventType.ITEM_ADDED -> {
                event.itemIdAdded?.let { itemId ->
                    currentLoc.copy(
                        itemIds = currentLoc.itemIds + itemId
                    )
                } ?: currentLoc
            }
            LocationEventType.ITEM_REMOVED -> {
                event.itemIdRemoved?.let { itemId ->
                    currentLoc.copy(
                        itemIds = currentLoc.itemIds.filter { it != itemId }
                    )
                } ?: currentLoc
            }
            LocationEventType.CREATURE_REMOVED -> {
                event.creatureIdRemoved?.let { creatureId ->
                    currentLoc.copy(
                        creatureIds = currentLoc.creatureIds.filter { it != creatureId }
                    )
                } ?: currentLoc
            }
            LocationEventType.CREATURE_ADDED -> {
                event.creatureIdAdded?.let { creatureId ->
                    // Only add if not already present
                    if (creatureId !in currentLoc.creatureIds) {
                        currentLoc.copy(
                            creatureIds = currentLoc.creatureIds + creatureId
                        )
                    } else {
                        currentLoc
                    }
                } ?: currentLoc
            }
            LocationEventType.LOCATION_UPDATED -> {
                // For general updates, refresh from server
                scope.launch { refreshCurrentLocation() }
                currentLoc
            }
            // PLAYER_ENTERED and PLAYER_LEFT are handled at the top of this function
            LocationEventType.PLAYER_ENTERED,
            LocationEventType.PLAYER_LEFT -> currentLoc
        }

        // Update current location state
        _currentLocation.value = updatedLocation

        // Update entities at location (in case items changed)
        updateEntitiesAtLocation(updatedLocation)

        // Update in allLocations cache
        updateLocationInCache(event, updatedLocation)
    }

    /**
     * Update a location in the allLocations cache based on a mutation event.
     */
    private fun updateLocationInCache(event: LocationMutationEvent, updatedLocation: LocationDto? = null) {
        _allLocations.value = _allLocations.value.map { loc ->
            if (loc.id == event.locationId) {
                updatedLocation ?: applyMutationToLocation(loc, event)
            } else {
                loc
            }
        }
    }

    /**
     * Apply a mutation event to a location.
     */
    private fun applyMutationToLocation(location: LocationDto, event: LocationMutationEvent): LocationDto {
        return when (event.eventType) {
            LocationEventType.EXIT_ADDED -> {
                event.exitAdded?.let { exitDto ->
                    location.copy(exits = location.exits + exitDto)
                } ?: location
            }
            LocationEventType.EXIT_REMOVED -> {
                event.exitRemoved?.let { exitDto ->
                    location.copy(exits = location.exits.filter { it.locationId != exitDto.locationId })
                } ?: location
            }
            LocationEventType.ITEM_ADDED -> {
                event.itemIdAdded?.let { itemId ->
                    location.copy(itemIds = location.itemIds + itemId)
                } ?: location
            }
            LocationEventType.ITEM_REMOVED -> {
                event.itemIdRemoved?.let { itemId ->
                    location.copy(itemIds = location.itemIds.filter { it != itemId })
                } ?: location
            }
            LocationEventType.CREATURE_REMOVED -> {
                event.creatureIdRemoved?.let { creatureId ->
                    location.copy(creatureIds = location.creatureIds.filter { it != creatureId })
                } ?: location
            }
            LocationEventType.CREATURE_ADDED -> {
                event.creatureIdAdded?.let { creatureId ->
                    if (creatureId !in location.creatureIds) {
                        location.copy(creatureIds = location.creatureIds + creatureId)
                    } else {
                        location
                    }
                } ?: location
            }
            LocationEventType.LOCATION_UPDATED -> {
                // For general updates, we'd need to fetch from server
                location
            }
            LocationEventType.PLAYER_ENTERED,
            LocationEventType.PLAYER_LEFT -> {
                // Player events don't affect location data
                location
            }
        }
    }

    /**
     * Clear all adventure state (on logout or mode switch).
     */
    fun clear() {
        currentUserId = null
        _currentLocation.value = null
        _creaturesHere.value = emptyList()
        _itemsHere.value = emptyList()
        _selectedCreature.value = null
        _selectedItem.value = null
        _allLocations.value = emptyList()
        _allCreatures.value = emptyList()
        _allItems.value = emptyList()
        _allAbilities.value = emptyMap()
        _creatureStates.value = emptyMap()
    }
}
