package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.data.AdventureRepository
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.EventLogType
import com.ez2bg.anotherthread.state.UserStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for search-related UI.
 */
data class SearchState(
    val isSearching: Boolean = false,
    val searchDurationMs: Long = 0
)

/**
 * Singleton handler for search business logic.
 * Manages location searching for hidden items.
 */
object SearchHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    /**
     * Search the current location for hidden items.
     */
    fun searchLocation() {
        val userId = UserStateHolder.userId ?: return

        // Already searching
        if (_searchState.value.isSearching) return

        scope.launch {
            // First get the search duration
            ApiClient.getSearchInfo(userId).onSuccess { info ->
                // Show the search overlay
                _searchState.update { it.copy(isSearching = true, searchDurationMs = info.durationMs) }

                // Wait for the search duration
                delay(info.durationMs)

                // Now perform the actual search
                ApiClient.searchLocation(userId).onSuccess { result ->
                    CombatStateHolder.addEventLogEntry(result.message, EventLogType.INFO)
                    if (result.discoveredItems.isNotEmpty()) {
                        // Refresh location to show newly discovered items
                        val locId = AdventureRepository.currentLocationId.value
                        if (locId != null) {
                            AdventureRepository.refreshLocationWithUserContext(locId, userId)
                        }
                    }
                }.onFailure { error ->
                    CombatStateHolder.addEventLogEntry("Failed to search: ${error.message}", EventLogType.ERROR)
                }

                // Hide the overlay
                _searchState.update { it.copy(isSearching = false, searchDurationMs = 0) }
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Failed to start search: ${error.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Cancel an in-progress search.
     */
    fun cancelSearch() {
        _searchState.update { it.copy(isSearching = false, searchDurationMs = 0) }
    }

    /**
     * Clear all search state.
     */
    fun clearState() {
        _searchState.value = SearchState()
    }
}
