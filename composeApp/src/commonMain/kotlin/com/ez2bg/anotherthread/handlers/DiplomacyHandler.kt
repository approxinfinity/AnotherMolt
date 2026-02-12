package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.DiplomacyResultDto
import com.ez2bg.anotherthread.api.HostilityResultDto
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
 * State for diplomacy-related UI.
 */
data class DiplomacyState(
    val diplomacyResult: DiplomacyResultDto? = null,
    val isDiplomacyLoading: Boolean = false,
    val hostilityResult: HostilityResultDto? = null
)

/**
 * One-time diplomacy events for UI handling.
 */
sealed class DiplomacyEvent {
    data class ShowSnackbar(val message: String) : DiplomacyEvent()
    data class ShowError(val message: String) : DiplomacyEvent()
    data class CombatAvoided(val creatureId: String) : DiplomacyEvent()
}

/**
 * Singleton handler for diplomacy business logic.
 * Manages bribe and parley interactions with creatures.
 */
object DiplomacyHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(DiplomacyState())
    val state: StateFlow<DiplomacyState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DiplomacyEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<DiplomacyEvent> = _events.asSharedFlow()

    /**
     * Check diplomacy options for a creature.
     * Called when a creature is selected to determine if bribe/parley options should show.
     */
    fun checkDiplomacy(creatureId: String) {
        val userId = UserStateHolder.userId ?: return
        scope.launch {
            _state.update { it.copy(isDiplomacyLoading = true, diplomacyResult = null) }
            ApiClient.checkDiplomacy(userId, creatureId).onSuccess { result ->
                _state.update { it.copy(diplomacyResult = result, isDiplomacyLoading = false) }
            }.onFailure {
                _state.update { it.copy(isDiplomacyLoading = false) }
                emitEvent(DiplomacyEvent.ShowError("Diplomacy check failed: ${it.message}"))
            }
        }
    }

    /**
     * Attempt to bribe a creature to avoid combat.
     * On success, combat is avoided and the creature won't be aggressive.
     */
    fun attemptBribe(creatureId: String) {
        val userId = UserStateHolder.userId ?: return
        scope.launch {
            _state.update { it.copy(isDiplomacyLoading = true) }
            ApiClient.attemptBribe(userId, creatureId).onSuccess { result ->
                _state.update { it.copy(diplomacyResult = result, isDiplomacyLoading = false) }
                emitEvent(DiplomacyEvent.ShowSnackbar(result.message))
                if (result.combatAvoided) {
                    // Notify that combat was avoided so modal can close
                    emitEvent(DiplomacyEvent.CombatAvoided(creatureId))
                    // Refresh user gold
                    UserStateHolder.refreshUser()
                }
            }.onFailure {
                _state.update { it.copy(isDiplomacyLoading = false) }
                emitEvent(DiplomacyEvent.ShowError("Bribe failed: ${it.message}"))
            }
        }
    }

    /**
     * Attempt to parley (talk) with a creature to avoid combat.
     * Uses WIS-based skill check.
     */
    fun attemptParley(creatureId: String) {
        val userId = UserStateHolder.userId ?: return
        scope.launch {
            _state.update { it.copy(isDiplomacyLoading = true) }
            ApiClient.attemptParley(userId, creatureId).onSuccess { result ->
                _state.update { it.copy(diplomacyResult = result, isDiplomacyLoading = false) }
                emitEvent(DiplomacyEvent.ShowSnackbar(result.message))
                if (result.combatAvoided) {
                    // Notify that combat was avoided so modal can close
                    emitEvent(DiplomacyEvent.CombatAvoided(creatureId))
                }
            }.onFailure {
                _state.update { it.copy(isDiplomacyLoading = false) }
                emitEvent(DiplomacyEvent.ShowError("Parley failed: ${it.message}"))
            }
        }
    }

    /**
     * Clear diplomacy state when creature selection changes or modal closes.
     */
    fun clearDiplomacyState() {
        _state.update { it.copy(diplomacyResult = null, isDiplomacyLoading = false, hostilityResult = null) }
    }

    /**
     * Clear all diplomacy state.
     */
    fun clearState() {
        _state.value = DiplomacyState()
    }

    private fun emitEvent(event: DiplomacyEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
