package com.ez2bg.anotherthread.storage

import com.ez2bg.anotherthread.api.UserDto
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistent storage for authentication state.
 * Uses multiplatform-settings which maps to:
 * - Web: localStorage
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 * - Desktop: java.util.prefs.Preferences
 */
object AuthStorage {
    private val settings: Settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }

    private const val KEY_USER = "auth_user"

    fun saveUser(user: UserDto) {
        val userJson = json.encodeToString(user)
        settings.putString(KEY_USER, userJson)
    }

    fun getUser(): UserDto? {
        val userJson = settings.getStringOrNull(KEY_USER) ?: return null
        return try {
            json.decodeFromString<UserDto>(userJson)
        } catch (e: Exception) {
            null
        }
    }

    fun clearUser() {
        settings.remove(KEY_USER)
    }

    fun isAuthenticated(): Boolean {
        return getUser() != null
    }
}
