package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.TrainerInfoResponse
import com.ez2bg.anotherthread.data.AdventureRepository
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
 * State for trainer-related UI.
 */
data class TrainerState(
    val showTrainerModal: Boolean = false,
    val trainerInfo: TrainerInfoResponse? = null,
    val isLoadingTrainer: Boolean = false
)

/**
 * One-time trainer events for UI handling.
 */
sealed class TrainerEvent {
    data class ShowSnackbar(val message: String) : TrainerEvent()
    data class AbilitiesUpdated(val unit: Unit = Unit) : TrainerEvent()
}

/**
 * Singleton handler for trainer business logic.
 * Manages trainer interaction and ability learning.
 */
object TrainerHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(TrainerState())
    val state: StateFlow<TrainerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TrainerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TrainerEvent> = _events.asSharedFlow()

    /**
     * Open the trainer modal for a creature.
     * Fetches the trainer info from the server.
     */
    fun openTrainerModal(creature: CreatureDto) {
        val userId = UserStateHolder.userId ?: return

        _state.update { it.copy(isLoadingTrainer = true, showTrainerModal = true) }

        scope.launch {
            ApiClient.getTrainerInfo(creature.id, userId)
                .onSuccess { trainerInfo ->
                    _state.update {
                        it.copy(
                            trainerInfo = trainerInfo,
                            isLoadingTrainer = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            showTrainerModal = false,
                            isLoadingTrainer = false
                        )
                    }
                    emitEvent(TrainerEvent.ShowSnackbar("Failed to load trainer info: ${error.message}"))
                }
        }
    }

    /**
     * Learn an ability from the current trainer.
     */
    fun learnAbility(abilityId: String) {
        val trainerInfo = _state.value.trainerInfo ?: return
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.learnAbility(trainerInfo.trainerId, userId, abilityId)
                .onSuccess { response ->
                    if (response.success) {
                        emitEvent(TrainerEvent.ShowSnackbar(response.message))
                        // Refresh trainer info to update learned status
                        ApiClient.getTrainerInfo(trainerInfo.trainerId, userId)
                            .onSuccess { updatedInfo ->
                                _state.update { it.copy(trainerInfo = updatedInfo) }
                            }
                        // Refresh user data to update gold and learned abilities
                        AdventureRepository.refresh()
                        // Notify that abilities were updated
                        emitEvent(TrainerEvent.AbilitiesUpdated())
                    } else {
                        emitEvent(TrainerEvent.ShowSnackbar(response.message))
                    }
                }
                .onFailure { error ->
                    emitEvent(TrainerEvent.ShowSnackbar("Failed to learn ability: ${error.message}"))
                }
        }
    }

    /**
     * Close the trainer modal.
     */
    fun dismissTrainerModal() {
        _state.update {
            it.copy(
                showTrainerModal = false,
                trainerInfo = null,
                isLoadingTrainer = false
            )
        }
    }

    /**
     * Clear all trainer state.
     */
    fun clearState() {
        _state.value = TrainerState()
    }

    private fun emitEvent(event: TrainerEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
