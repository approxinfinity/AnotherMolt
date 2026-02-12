package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.ExitDto
import com.ez2bg.anotherthread.api.LockpickInfoDto
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
 * State for lockpicking-related UI.
 */
data class LockpickingState(
    val showLockpickingMinigame: Boolean = false,
    val lockpickingInfo: LockpickInfoDto? = null,
    val lockpickingLocationId: String? = null  // The location we're trying to enter
)

/**
 * One-time lockpicking events for UI handling.
 */
sealed class LockpickingEvent {
    data class ShowMessage(val message: String) : LockpickingEvent()
    data class ShowError(val message: String) : LockpickingEvent()
    data class NavigateToLocation(val locationId: String) : LockpickingEvent()
}

/**
 * Singleton handler for lockpicking business logic.
 * Manages lock picking attempts and minigame state.
 */
object LockpickingHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(LockpickingState())
    val state: StateFlow<LockpickingState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LockpickingEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<LockpickingEvent> = _events.asSharedFlow()

    /**
     * Attempt to enter a locked location.
     * If the player can pick locks, shows the lockpicking minigame.
     * Otherwise shows an error message.
     */
    fun attemptLockedDoor(locationId: String) {
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.getLockpickInfo(userId, locationId).onSuccess { info ->
                if (!info.canAttempt) {
                    emitEvent(LockpickingEvent.ShowError(info.reason ?: "Cannot pick this lock"))
                    return@onSuccess
                }

                // Show the lockpicking minigame
                _state.update {
                    it.copy(
                        showLockpickingMinigame = true,
                        lockpickingInfo = info,
                        lockpickingLocationId = locationId
                    )
                }
            }.onFailure { error ->
                emitEvent(LockpickingEvent.ShowError("Failed to check lock: ${error.message}"))
            }
        }
    }

    /**
     * Complete the lockpicking minigame with the player's accuracy.
     */
    fun completeLockpicking(accuracy: Float) {
        val userId = UserStateHolder.userId ?: return
        val locationId = _state.value.lockpickingLocationId ?: return

        scope.launch {
            ApiClient.attemptLockpick(userId, locationId, accuracy).onSuccess { result ->
                if (result.success) {
                    emitEvent(LockpickingEvent.ShowMessage(result.message))
                    // Navigate to the now-unlocked location
                    emitEvent(LockpickingEvent.NavigateToLocation(locationId))
                    // Refresh location data to update lockLevel
                    AdventureRepository.refreshLocationWithUserContext(locationId, userId)
                } else {
                    emitEvent(LockpickingEvent.ShowError(result.message))
                }
            }.onFailure { error ->
                emitEvent(LockpickingEvent.ShowError("Failed to pick lock: ${error.message}"))
            }

            // Hide the minigame UI
            _state.update {
                it.copy(
                    showLockpickingMinigame = false,
                    lockpickingInfo = null,
                    lockpickingLocationId = null
                )
            }
        }
    }

    /**
     * Cancel the lockpicking attempt.
     */
    fun cancelLockpicking() {
        _state.update {
            it.copy(
                showLockpickingMinigame = false,
                lockpickingInfo = null,
                lockpickingLocationId = null
            )
        }
    }

    /**
     * Clear all lockpicking state.
     */
    fun clearState() {
        _state.value = LockpickingState()
    }

    private fun emitEvent(event: LockpickingEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
