package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.TeleportDestinationDto
import com.ez2bg.anotherthread.data.AdventureRepository
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.EventLogType
import com.ez2bg.anotherthread.state.UserStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for teleport-related UI.
 */
data class TeleportState(
    val showMapSelection: Boolean = false,
    val teleportDestinations: List<TeleportDestinationDto> = emptyList(),
    val teleportAbilityId: String? = null
)

/**
 * One-time teleport events for UI handling.
 */
sealed class TeleportEvent {
    data class ShowSnackbar(val message: String) : TeleportEvent()
    data class ShowError(val message: String) : TeleportEvent()
}

/**
 * Singleton handler for teleport business logic.
 * Manages teleport ability triggering, destination selection, and execution.
 */
object TeleportHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(TeleportState())
    val state: StateFlow<TeleportState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TeleportEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TeleportEvent> = _events.asSharedFlow()

    /**
     * Handle teleport ability activation.
     * Fetches available destinations and shows the map selection UI.
     */
    fun handleTeleportAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            emitEvent(TeleportEvent.ShowSnackbar("Cannot teleport during combat!"))
            return
        }

        scope.launch {
            ApiClient.getTeleportDestinations().onSuccess { destinations ->
                _state.update {
                    it.copy(
                        showMapSelection = true,
                        teleportDestinations = destinations,
                        teleportAbilityId = ability.id
                    )
                }
            }.onFailure {
                emitEvent(TeleportEvent.ShowError("Failed to load destinations"))
            }
        }
    }

    /**
     * Select a teleport destination and execute the teleport.
     */
    fun selectTeleportDestination(destination: TeleportDestinationDto) {
        val userId = UserStateHolder.userId ?: return
        val abilityId = _state.value.teleportAbilityId ?: return

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
                emitEvent(TeleportEvent.ShowError("You are over-encumbered by $overAmount stone. Drop items to teleport."))
                _state.update { it.copy(showMapSelection = false) }
                return
            }
        }

        scope.launch {
            // Dismiss overlay immediately
            _state.update { it.copy(showMapSelection = false) }

            // Departure message
            val userName = UserStateHolder.userName ?: "Unknown"
            CombatStateHolder.addEventLogEntry(
                "With a soft pop $userName dematerializes.",
                EventLogType.NAVIGATION
            )

            // Call teleport API
            ApiClient.teleport(userId, destination.areaId, abilityId).onSuccess { response ->
                if (response.success && response.newLocationId != null) {
                    // Update location - AdventureRepository is single source of truth
                    AdventureRepository.setCurrentLocation(response.newLocationId)
                    // Track visited location for minimap fog-of-war
                    UserStateHolder.addVisitedLocation(response.newLocationId)

                    // Arrival message
                    CombatStateHolder.addEventLogEntry(
                        response.arrivalMessage ?: "$userName materializes with a loud bang!",
                        EventLogType.NAVIGATION
                    )

                    emitEvent(TeleportEvent.ShowSnackbar("Teleported to ${response.newLocationName ?: destination.locationName}"))
                } else {
                    emitEvent(TeleportEvent.ShowSnackbar(response.message))
                }
            }.onFailure {
                emitEvent(TeleportEvent.ShowError("Teleport failed: ${it.message}"))
            }

            // Clear teleport state
            _state.update {
                it.copy(
                    teleportDestinations = emptyList(),
                    teleportAbilityId = null
                )
            }
        }
    }

    /**
     * Dismiss the map selection UI.
     */
    fun dismissMapSelection() {
        _state.update {
            it.copy(
                showMapSelection = false,
                teleportDestinations = emptyList(),
                teleportAbilityId = null
            )
        }
    }

    /**
     * Clear all teleport state.
     */
    fun clearState() {
        _state.value = TeleportState()
    }

    private fun emitEvent(event: TeleportEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
