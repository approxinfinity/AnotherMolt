package com.ez2bg.anotherthread.storage

import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.currentTimeMillis
import com.ez2bg.anotherthread.isWebPlatform
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
 *
 * Session Token Handling:
 * - Web: Uses HttpOnly cookies (automatically sent with requests, not stored here)
 * - Native (iOS/Android/Desktop): Stores token here, sent via Authorization header
 */
object AuthStorage {
    private val settings: Settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }

    private const val KEY_USER = "auth_user"
    private const val KEY_SESSION_TOKEN = "session_token"
    private const val KEY_SESSION_EXPIRES_AT = "session_expires_at"

    /**
     * Save user data to persistent storage.
     */
    fun saveUser(user: UserDto) {
        val userJson = json.encodeToString(user)
        settings.putString(KEY_USER, userJson)
    }

    /**
     * Get stored user data.
     */
    fun getUser(): UserDto? {
        val userJson = settings.getStringOrNull(KEY_USER) ?: return null
        return try {
            json.decodeFromString<UserDto>(userJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save session token (for native platforms).
     * On web, this is a no-op since cookies handle the session.
     */
    fun saveSessionToken(token: String, expiresAt: Long) {
        // On web, cookies handle the session - we still save for reference
        // but the actual auth is via cookies
        settings.putString(KEY_SESSION_TOKEN, token)
        settings.putLong(KEY_SESSION_EXPIRES_AT, expiresAt)
    }

    /**
     * Get stored session token.
     * Returns null if no token or if expired.
     */
    fun getSessionToken(): String? {
        val token = settings.getStringOrNull(KEY_SESSION_TOKEN) ?: return null
        val expiresAt = settings.getLongOrNull(KEY_SESSION_EXPIRES_AT) ?: return null

        // Check if expired (with 1 minute buffer)
        if (currentTimeMillis() > (expiresAt - 60_000)) {
            // Token expired, clear it
            clearSession()
            return null
        }

        return token
    }

    /**
     * Check if session token is expired.
     */
    fun isSessionExpired(): Boolean {
        val expiresAt = settings.getLongOrNull(KEY_SESSION_EXPIRES_AT) ?: return true
        return currentTimeMillis() > expiresAt
    }

    /**
     * Get session expiry timestamp.
     */
    fun getSessionExpiresAt(): Long? {
        return settings.getLongOrNull(KEY_SESSION_EXPIRES_AT)
    }

    /**
     * Update session expiry (called when server refreshes the session).
     */
    fun updateSessionExpiry(expiresAt: Long) {
        settings.putLong(KEY_SESSION_EXPIRES_AT, expiresAt)
    }

    /**
     * Clear session token and expiry.
     */
    fun clearSession() {
        settings.remove(KEY_SESSION_TOKEN)
        settings.remove(KEY_SESSION_EXPIRES_AT)
    }

    /**
     * Clear all auth data (user + session).
     */
    fun clearUser() {
        settings.remove(KEY_USER)
        clearSession()
    }

    /**
     * Check if user is authenticated.
     * For native platforms, also checks if session is not expired.
     */
    fun isAuthenticated(): Boolean {
        val user = getUser() ?: return false

        // On web, cookies handle session - just check if we have user data
        if (isWebPlatform()) {
            return true
        }

        // On native, check if session token exists and is not expired
        return getSessionToken() != null
    }

    /**
     * Check if we should send Authorization header (native platforms only).
     */
    fun shouldSendAuthHeader(): Boolean {
        return !isWebPlatform() && getSessionToken() != null
    }
}
