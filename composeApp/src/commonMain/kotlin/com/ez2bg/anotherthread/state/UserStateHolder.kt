package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.UserDto
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

    val currentLocationId: String?
        get() = _currentUser.value?.currentLocationId

    // Auth events (for one-time event handling)
    private val _authEvents = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 10)
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Initialize from persisted storage.
     * Call this at app startup.
     */
    fun initialize() {
        val savedUser = AuthStorage.getUser()
        if (savedUser != null) {
            _currentUser.value = savedUser
            ApiClient.setUserContext(savedUser.id, savedUser.name)

            // Refresh user data from server
            scope.launch {
                refreshUser(savedUser.id)
            }
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
                    setUser(response.user)
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
                    setUser(response.user)
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
     */
    fun logout() {
        AuthStorage.clearUser()
        ApiClient.clearUserContext()
        _currentUser.value = null

        // Clear related state holders
        AdventureStateHolder.clear()
        CombatStateHolder.disconnect()

        scope.launch {
            _authEvents.emit(AuthEvent.LoggedOut)
        }
    }

    /**
     * Update the current user.
     */
    fun updateUser(user: UserDto) {
        _currentUser.value = user
        AuthStorage.saveUser(user)
        ApiClient.setUserContext(user.id, user.name)

        scope.launch {
            _authEvents.emit(AuthEvent.UserUpdated(user))
        }
    }

    /**
     * Set the current user (after login/register).
     */
    private fun setUser(user: UserDto) {
        _currentUser.value = user
        AuthStorage.saveUser(user)
        ApiClient.setUserContext(user.id, user.name)

        scope.launch {
            _authEvents.emit(AuthEvent.LoggedIn(user))
        }
    }

    /**
     * Refresh user data from server.
     */
    private suspend fun refreshUser(userId: String) {
        ApiClient.getUser(userId).onSuccess { freshUser ->
            if (freshUser != null) {
                _currentUser.value = freshUser
                AuthStorage.saveUser(freshUser)
            }
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
