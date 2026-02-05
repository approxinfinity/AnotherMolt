package com.ez2bg.anotherthread.storage

import com.russhwolf.settings.Settings

/**
 * Persistent storage for onboarding state.
 * Uses multiplatform-settings which maps to:
 * - Web: localStorage
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 * - Desktop: java.util.prefs.Preferences
 */
object OnboardingStorage {
    private val settings: Settings = Settings()

    private const val KEY_HAS_SEEN = "has_seen_onboarding"

    /**
     * Check if the user has completed onboarding.
     */
    fun hasSeenOnboarding(): Boolean =
        settings.getBoolean(KEY_HAS_SEEN, false)

    /**
     * Mark onboarding as complete.
     */
    fun markOnboardingSeen() {
        settings.putBoolean(KEY_HAS_SEEN, true)
    }

    /**
     * Reset onboarding state (for testing/debugging).
     */
    fun reset() {
        settings.remove(KEY_HAS_SEEN)
    }
}
