package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.PuzzleDto
import com.ez2bg.anotherthread.api.PuzzleProgressResponse
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
 * State for puzzle-related UI.
 */
data class PuzzleState(
    val showPuzzleModal: Boolean = false,
    val currentPuzzle: PuzzleDto? = null,
    val puzzleProgress: PuzzleProgressResponse? = null,
    val isLoadingPuzzle: Boolean = false,
    val puzzlesAtLocation: List<PuzzleDto> = emptyList()
)

/**
 * One-time puzzle events for UI handling.
 */
sealed class PuzzleEvent {
    data class ShowSnackbar(val message: String) : PuzzleEvent()
}

/**
 * Singleton handler for puzzle business logic.
 * Manages puzzle loading, interaction, and solving.
 */
object PuzzleHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _puzzleState = MutableStateFlow(PuzzleState())
    val puzzleState: StateFlow<PuzzleState> = _puzzleState.asStateFlow()

    private val _events = MutableSharedFlow<PuzzleEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PuzzleEvent> = _events.asSharedFlow()

    /**
     * Load puzzles at the specified location.
     */
    fun loadPuzzlesAtLocation(locationId: String) {
        println("[PuzzleHandler] Loading puzzles for location: $locationId")
        scope.launch {
            ApiClient.getPuzzlesAtLocation(locationId)
                .onSuccess { puzzles ->
                    println("[PuzzleHandler] Loaded ${puzzles.size} puzzles: ${puzzles.map { it.name }}")
                    _puzzleState.update { it.copy(puzzlesAtLocation = puzzles) }
                }
                .onFailure { error ->
                    println("[PuzzleHandler] Failed to load puzzles: ${error.message}")
                    _puzzleState.update { it.copy(puzzlesAtLocation = emptyList()) }
                }
        }
    }

    /**
     * Open a puzzle modal.
     */
    fun openPuzzleModal(puzzle: PuzzleDto) {
        val userId = UserStateHolder.userId ?: return

        _puzzleState.update {
            it.copy(
                showPuzzleModal = true,
                currentPuzzle = puzzle,
                isLoadingPuzzle = true
            )
        }

        scope.launch {
            ApiClient.getPuzzleProgress(puzzle.id, userId)
                .onSuccess { progress ->
                    _puzzleState.update {
                        it.copy(
                            puzzleProgress = progress,
                            isLoadingPuzzle = false
                        )
                    }
                }
                .onFailure { error ->
                    _puzzleState.update {
                        it.copy(
                            showPuzzleModal = false,
                            isLoadingPuzzle = false
                        )
                    }
                    emitEvent(PuzzleEvent.ShowSnackbar("Failed to load puzzle: ${error.message}"))
                }
        }
    }

    /**
     * Pull a lever in the current puzzle.
     */
    fun pullLever(leverId: String) {
        val puzzle = _puzzleState.value.currentPuzzle ?: return
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.pullLever(puzzle.id, leverId, userId)
                .onSuccess { response ->
                    // Show the message from the server
                    emitEvent(PuzzleEvent.ShowSnackbar(response.message.replace("\n\n", " ")))

                    // Refresh puzzle progress
                    ApiClient.getPuzzleProgress(puzzle.id, userId)
                        .onSuccess { progress ->
                            _puzzleState.update { it.copy(puzzleProgress = progress) }
                        }

                    // If puzzle was solved, refresh the current location to show new exits
                    if (response.puzzleSolved) {
                        val currentLocationId = AdventureRepository.currentLocationId.value
                        if (currentLocationId != null) {
                            AdventureRepository.refreshLocationWithUserContext(currentLocationId, userId)
                        }
                    }
                }
                .onFailure { error ->
                    emitEvent(PuzzleEvent.ShowSnackbar("Failed to pull lever: ${error.message}"))
                }
        }
    }

    /**
     * Close the puzzle modal.
     */
    fun dismissPuzzleModal() {
        _puzzleState.update {
            it.copy(
                showPuzzleModal = false,
                currentPuzzle = null,
                puzzleProgress = null,
                isLoadingPuzzle = false
            )
        }
    }

    /**
     * Clear all puzzle state.
     */
    fun clearState() {
        _puzzleState.value = PuzzleState()
    }

    private fun emitEvent(event: PuzzleEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
