package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.SealableRiftDto
import com.ez2bg.anotherthread.api.UnconnectedAreaDto
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
 * State for rift-related UI.
 */
data class RiftState(
    val showRiftSelection: Boolean = false,
    val riftMode: String? = null,  // "open" or "seal"
    val unconnectedAreas: List<UnconnectedAreaDto> = emptyList(),
    val sealableRifts: List<SealableRiftDto> = emptyList()
)

/**
 * One-time rift events for UI handling.
 */
sealed class RiftEvent {
    data class ShowSnackbar(val message: String) : RiftEvent()
    data class ShowError(val message: String) : RiftEvent()
}

/**
 * Singleton handler for rift portal business logic.
 * Manages opening and sealing rifts to other areas.
 */
object RiftHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(RiftState())
    val state: StateFlow<RiftState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RiftEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RiftEvent> = _events.asSharedFlow()

    /**
     * Handle open rift ability activation.
     * Fetches unconnected areas and shows the selection UI.
     */
    fun handleOpenRiftAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            emitEvent(RiftEvent.ShowSnackbar("Cannot open rifts during combat!"))
            return
        }

        val userId = UserStateHolder.userId ?: return
        scope.launch {
            ApiClient.getUnconnectedAreas(userId).onSuccess { areas ->
                if (areas.isEmpty()) {
                    emitEvent(RiftEvent.ShowSnackbar("No unconnected realms to open rifts to"))
                } else {
                    _state.update {
                        it.copy(
                            showRiftSelection = true,
                            riftMode = "open",
                            unconnectedAreas = areas
                        )
                    }
                }
            }.onFailure {
                emitEvent(RiftEvent.ShowError("Failed to find realms: ${it.message}"))
            }
        }
    }

    /**
     * Handle seal rift ability activation.
     * Fetches sealable rifts and shows the selection UI.
     */
    fun handleSealRiftAbility(ability: AbilityDto) {
        if (CombatStateHolder.isInCombat) {
            emitEvent(RiftEvent.ShowSnackbar("Cannot seal rifts during combat!"))
            return
        }

        val userId = UserStateHolder.userId ?: return
        scope.launch {
            ApiClient.getSealableRifts(userId).onSuccess { rifts ->
                if (rifts.isEmpty()) {
                    emitEvent(RiftEvent.ShowSnackbar("No rifts to seal at this location"))
                } else {
                    _state.update {
                        it.copy(
                            showRiftSelection = true,
                            riftMode = "seal",
                            sealableRifts = rifts
                        )
                    }
                }
            }.onFailure {
                emitEvent(RiftEvent.ShowError("Failed to find rifts: ${it.message}"))
            }
        }
    }

    /**
     * Select an area to open a rift to.
     */
    fun selectRiftToOpen(area: UnconnectedAreaDto) {
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            _state.update { it.copy(showRiftSelection = false) }

            ApiClient.openRift(userId, area.areaId).onSuccess { response ->
                if (response.success) {
                    CombatStateHolder.addEventLogEntry(response.message, EventLogType.NAVIGATION)
                    emitEvent(RiftEvent.ShowSnackbar(response.message))
                    // Refresh to show new exit
                    AdventureRepository.refresh()
                } else {
                    emitEvent(RiftEvent.ShowSnackbar(response.message))
                }
            }.onFailure {
                emitEvent(RiftEvent.ShowError("Failed to open rift: ${it.message}"))
            }

            _state.update {
                it.copy(riftMode = null, unconnectedAreas = emptyList())
            }
        }
    }

    /**
     * Select a rift to seal.
     */
    fun selectRiftToSeal(rift: SealableRiftDto) {
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            _state.update { it.copy(showRiftSelection = false) }

            ApiClient.sealRift(userId, rift.targetAreaId).onSuccess { response ->
                if (response.success) {
                    CombatStateHolder.addEventLogEntry(response.message, EventLogType.NAVIGATION)
                    emitEvent(RiftEvent.ShowSnackbar(response.message))
                    // Refresh to remove exit
                    AdventureRepository.refresh()
                } else {
                    emitEvent(RiftEvent.ShowSnackbar(response.message))
                }
            }.onFailure {
                emitEvent(RiftEvent.ShowError("Failed to seal rift: ${it.message}"))
            }

            _state.update {
                it.copy(riftMode = null, sealableRifts = emptyList())
            }
        }
    }

    /**
     * Dismiss the rift selection UI.
     */
    fun dismissRiftSelection() {
        _state.update {
            it.copy(
                showRiftSelection = false,
                riftMode = null,
                unconnectedAreas = emptyList(),
                sealableRifts = emptyList()
            )
        }
    }

    /**
     * Clear all rift state.
     */
    fun clearState() {
        _state.value = RiftState()
    }

    private fun emitEvent(event: RiftEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
