package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.FishingInfoDto
import com.ez2bg.anotherthread.api.FishingMinigameStartDto
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
 * State for fishing-related UI.
 */
data class FishingState(
    val isFishing: Boolean = false,
    val fishingDurationMs: Long = 0,
    val showFishingDistanceModal: Boolean = false,
    val fishingInfo: FishingInfoDto? = null,
    val showFishingMinigame: Boolean = false,
    val fishingMinigameData: FishingMinigameStartDto? = null
)

/**
 * One-time fishing events for UI handling.
 */
sealed class FishingEvent {
    data class ShowSnackbar(val message: String) : FishingEvent()
    data class ShowError(val message: String) : FishingEvent()
}

/**
 * Singleton handler for fishing business logic.
 * Manages fishing modal, minigame, and catch results.
 */
object FishingHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _fishingState = MutableStateFlow(FishingState())
    val fishingState: StateFlow<FishingState> = _fishingState.asStateFlow()

    private val _events = MutableSharedFlow<FishingEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<FishingEvent> = _events.asSharedFlow()

    /**
     * Open the fishing distance modal.
     * Fetches fishing info from server and shows distance options.
     */
    fun openFishingModal() {
        val userId = UserStateHolder.userId ?: return

        // Already fishing
        if (_fishingState.value.isFishing) return

        scope.launch {
            ApiClient.getFishingInfo(userId).onSuccess { info ->
                if (!info.canFish) {
                    CombatStateHolder.addEventLogEntry(info.reason ?: "You cannot fish here.", EventLogType.INFO)
                    return@onSuccess
                }
                _fishingState.update {
                    it.copy(
                        showFishingDistanceModal = true,
                        fishingInfo = info
                    )
                }
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Failed to get fishing info: ${error.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Close the fishing distance modal without fishing.
     */
    fun closeFishingModal() {
        _fishingState.update {
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
        val info = _fishingState.value.fishingInfo ?: return

        // Already fishing
        if (_fishingState.value.isFishing || _fishingState.value.showFishingMinigame) return

        scope.launch {
            // Close the modal and show loading
            _fishingState.update {
                it.copy(
                    showFishingDistanceModal = false,
                    isFishing = true
                )
            }

            // Start the minigame session on server
            ApiClient.startFishingMinigame(userId, distance).onSuccess { minigameData ->
                if (!minigameData.success) {
                    CombatStateHolder.addEventLogEntry(minigameData.message ?: "Failed to start fishing", EventLogType.INFO)
                    _fishingState.update {
                        it.copy(
                            isFishing = false,
                            fishingInfo = null
                        )
                    }
                    return@onSuccess
                }

                // Show the minigame UI
                _fishingState.update {
                    it.copy(
                        isFishing = false,
                        showFishingMinigame = true,
                        fishingMinigameData = minigameData
                    )
                }
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Failed to start fishing: ${error.message}", EventLogType.ERROR)
                _fishingState.update {
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
        val minigameData = _fishingState.value.fishingMinigameData ?: return
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
                    CombatStateHolder.addEventLogEntry("You caught a $sizeDesc ${fish.name}! (+${result.manaRestored} mana)", EventLogType.LOOT)

                    // Show badge earned message if applicable
                    if (result.earnedBadge) {
                        CombatStateHolder.addEventLogEntry("You earned the Angler's Badge! Your fishing skill has improved.", EventLogType.LOOT)
                    }

                    // Refresh user data to update inventory and abilities
                    UserStateHolder.refreshUser()
                } else {
                    CombatStateHolder.addEventLogEntry(result.message, EventLogType.INFO)
                }
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Failed to complete fishing: ${error.message}", EventLogType.ERROR)
            }

            // Hide the minigame UI
            _fishingState.update {
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
        _fishingState.update {
            it.copy(
                isFishing = false,
                fishingDurationMs = 0,
                fishingInfo = null,
                showFishingMinigame = false,
                fishingMinigameData = null
            )
        }
    }

    /**
     * Clear all fishing state.
     */
    fun clearState() {
        _fishingState.value = FishingState()
    }

    private fun emitEvent(event: FishingEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
