package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.data.AdventureRepository
import com.ez2bg.anotherthread.storage.AuthStorage
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
 * Admin feature ID constant.
 */
const val ADMIN_FEATURE_ID = "admin"

/**
 * Auth-related events.
 */
sealed class AuthEvent {
    data class LoggedIn(val user: UserDto) : AuthEvent()
    data object LoggedOut : AuthEvent()
    data class UserUpdated(val user: UserDto) : AuthEvent()
    data class AuthError(val message: String) : AuthEvent()
    data class SessionInvalidated(val message: String) : AuthEvent()
}

/**
 * Singleton state holder for user/authentication state.
 * Manages current user, admin status, and auth persistence.
 *
 * This separates user business logic from UI composables.
 */
object UserStateHolder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Current authenticated user
    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    // Derived states
    val isAuthenticated: Boolean
        get() = _currentUser.value != null

    val isAdmin: Boolean
        get() = _currentUser.value?.featureIds?.contains(ADMIN_FEATURE_ID) == true

    val hasCompleteProfile: Boolean
        get() = _currentUser.value?.characterClassId != null

    // NOTE: currentLocationId was removed - use AdventureRepository.currentLocationId instead
    // The UserDto.currentLocationId in localStorage may be stale. Server is truth for location.

    // Auth events (for one-time event handling)
    private val _authEvents = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 10)
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Initialize from persisted storage.
     * Call this at app startup.
     *
     * On web: Validates session via /auth/me (cookie is auto-sent)
     * On native: Checks if stored token is expired, then validates with server
     */
    fun initialize() {
        val savedUser = AuthStorage.getUser()
        if (savedUser != null) {
            println("[UserStateHolder] Loaded cached user: ${savedUser.name}, items: ${savedUser.itemIds.size}")
            _currentUser.value = savedUser
            ApiClient.setUserContext(savedUser.id, savedUser.name)

            // Validate session with server (this also refreshes the session)
            scope.launch {
                validateSession()
            }
        }
    }

    /**
     * Validate the current session with the server.
     * On success, refreshes user data and session expiry.
     * On explicit session invalid, logs out the user.
     * On network error, keeps the user logged in (optimistic).
     *
     * NOTE: Server is the source of truth for location. We use the server's
     * location directly - AdventureRepository is the single source of truth
     * for location state in the client.
     */
    private suspend fun validateSession() {
        val result = ApiClient.validateSession()

        result.onSuccess { response ->
            if (response.success && response.user != null) {
                // Update user data from server (fresh data with all items)
                println("[UserStateHolder] Session valid - refreshing user data. Items: ${response.user.itemIds.size}, Location: ${response.user.currentLocationId}")
                val previousUser = _currentUser.value

                // Merge visited locations: combine server + local to get complete set
                // (visited locations are additive - never lose exploration progress)
                val localVisited = previousUser?.visitedLocationIds ?: emptyList()
                val serverVisited = response.user.visitedLocationIds
                val mergedVisited = (serverVisited + localVisited).distinct()

                val mergedUser = response.user.copy(
                    visitedLocationIds = mergedVisited
                )

                _currentUser.value = mergedUser
                AuthStorage.saveUser(mergedUser)

                // Emit UserUpdated if the user data changed (e.g., characterClassId was set)
                // This allows App.kt to react and switch screens appropriately
                if (previousUser?.characterClassId != response.user.characterClassId) {
                    println("[UserStateHolder] Character class changed: ${previousUser?.characterClassId} -> ${response.user.characterClassId}")
                    _authEvents.emit(AuthEvent.UserUpdated(response.user))
                }

                // Update session expiry for native platforms
                if (response.sessionToken != null && response.expiresAt != null) {
                    AuthStorage.saveSessionToken(response.sessionToken, response.expiresAt)
                }
            } else {
                // Session explicitly invalid - logout
                println("[UserStateHolder] Session invalid: ${response.message}")
                performLocalLogout()
                _authEvents.emit(AuthEvent.AuthError("Session expired. Please login again."))
            }
        }.onFailure { error ->
            // Network error - keep user logged in (optimistic), they can retry later
            println("[UserStateHolder] Session validation failed (network): ${error.message}")
            // Don't logout on network errors - let the user continue with cached data
        }
    }

    /**
     * Login with username and password.
     */
    fun login(username: String, password: String, onResult: (Result<UserDto>) -> Unit) {
        scope.launch {
            _isLoading.value = true

            val result = ApiClient.login(username, password)

            result.onSuccess { response ->
                if (response.success && response.user != null) {
                    setUser(response.user, response.sessionToken, response.expiresAt)
                    onResult(Result.success(response.user))
                } else {
                    onResult(Result.failure(Exception(response.message ?: "Login failed")))
                }
            }.onFailure { error ->
                onResult(Result.failure(error))
            }

            _isLoading.value = false
        }
    }

    /**
     * Register a new user.
     */
    fun register(username: String, password: String, onResult: (Result<UserDto>) -> Unit) {
        scope.launch {
            _isLoading.value = true

            val result = ApiClient.register(username, password)

            result.onSuccess { response ->
                if (response.success && response.user != null) {
                    setUser(response.user, response.sessionToken, response.expiresAt)
                    onResult(Result.success(response.user))
                } else {
                    onResult(Result.failure(Exception(response.message ?: "Registration failed")))
                }
            }.onFailure { error ->
                onResult(Result.failure(error))
            }

            _isLoading.value = false
        }
    }

    /**
     * Logout the current user.
     * Calls server to invalidate session, then clears local state.
     */
    fun logout() {
        scope.launch {
            // Call server to invalidate session
            ApiClient.logout()
            // Always perform local logout even if server call fails
            performLocalLogout()
            _authEvents.emit(AuthEvent.LoggedOut)
        }
    }

    /**
     * Logout from all devices.
     * Invalidates all sessions for this user on the server.
     */
    fun logoutAll() {
        scope.launch {
            ApiClient.logoutAll()
            performLocalLogout()
            _authEvents.emit(AuthEvent.LoggedOut)
        }
    }

    /**
     * Clear local auth state without calling server.
     */
    private fun performLocalLogout() {
        AuthStorage.clearUser()
        ApiClient.clearUserContext()
        _currentUser.value = null

        // Clear related state holders
        AdventureRepository.clear()
        CombatStateHolder.disconnect()
    }

    /**
     * Handle session invalidated event (user signed in on another device).
     * Logs out locally and emits SessionInvalidated event with the message.
     */
    fun handleSessionInvalidated(message: String) {
        performLocalLogout()
        scope.launch {
            _authEvents.emit(AuthEvent.SessionInvalidated(message))
        }
    }

    /**
     * Update the current user from server data.
     * Server is authoritative - we use server's data directly.
     * Only visited locations are merged (additive, never lose progress).
     *
     * NOTE: Location is NOT managed here - AdventureRepository is the
     * single source of truth for player location.
     */
    fun updateUser(user: UserDto) {
        val localUser = _currentUser.value
        val mergedUser = if (localUser != null && localUser.id == user.id) {
            // Always merge visited locations (never lose exploration progress)
            val mergedVisited = (user.visitedLocationIds + localUser.visitedLocationIds).distinct()
            user.copy(visitedLocationIds = mergedVisited)
        } else {
            // Different user or no local user - use server data as-is
            user
        }

        _currentUser.value = mergedUser
        AuthStorage.saveUser(mergedUser)
        ApiClient.setUserContext(mergedUser.id, mergedUser.name)

        scope.launch {
            _authEvents.emit(AuthEvent.UserUpdated(mergedUser))
        }
    }

    /**
     * Set the current user (after login/register).
     * Also saves session token for native platforms.
     */
    private fun setUser(user: UserDto, sessionToken: String? = null, expiresAt: Long? = null) {
        _currentUser.value = user
        AuthStorage.saveUser(user)
        ApiClient.setUserContext(user.id, user.name)

        // Save session token for native platforms
        if (sessionToken != null && expiresAt != null) {
            AuthStorage.saveSessionToken(sessionToken, expiresAt)
        }

        scope.launch {
            _authEvents.emit(AuthEvent.LoggedIn(user))
        }
    }

    /**
     * Refresh user data from server.
     * Server is the source of truth for all user data including location.
     * AdventureRepository is the single source of truth for location in the client.
     */
    suspend fun refreshUser() {
        val userId = _currentUser.value?.id ?: return
        val localVisitedIds = _currentUser.value?.visitedLocationIds ?: emptyList()

        ApiClient.getUser(userId).onSuccess { freshUser ->
            if (freshUser != null) {
                // Merge visited locations (local + server) to never lose exploration progress
                val mergedVisitedIds = (freshUser.visitedLocationIds + localVisitedIds).distinct()
                val userToSave = freshUser.copy(visitedLocationIds = mergedVisitedIds)
                _currentUser.value = userToSave
                AuthStorage.saveUser(userToSave)
            }
        }
    }

    /**
     * @deprecated Use refreshUser() instead. Server is always truth for location.
     */
    suspend fun refreshUserWithServerLocation() {
        refreshUser()
    }

    /**
     * Spend mana locally (for immediate UI feedback).
     * Should be called when an ability is used that costs mana.
     */
    fun spendManaLocally(amount: Int) {
        val current = _currentUser.value ?: return
        val newMana = (current.currentMana - amount).coerceAtLeast(0)
        println("[UserStateHolder] spendManaLocally: ${current.currentMana} - $amount = $newMana")
        _currentUser.value = current.copy(currentMana = newMana)
        AuthStorage.saveUser(_currentUser.value!!)
    }

    /**
     * Update visited locations locally (for minimap fog-of-war).
     * Called when the user visits a new location.
     * Location itself is managed by AdventureRepository.
     */
    fun addVisitedLocation(locationId: String) {
        val current = _currentUser.value ?: return
        if (locationId !in current.visitedLocationIds) {
            _currentUser.value = current.copy(
                visitedLocationIds = current.visitedLocationIds + locationId
            )
            AuthStorage.saveUser(_currentUser.value!!)
        }
    }

    /**
     * Get the current user ID (convenience method).
     */
    val userId: String?
        get() = _currentUser.value?.id

    /**
     * Get the current user name (convenience method).
     */
    val userName: String?
        get() = _currentUser.value?.name
}
