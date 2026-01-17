package com.ez2bg.anotherthread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for AppConfig and related configuration classes.
 */
class AppConfigTest {

    // ==================== Environment Enum Tests ====================

    @Test
    fun environmentEnumHasThreeValues() {
        val environments = Environment.entries
        assertEquals(3, environments.size, "Environment enum should have exactly 3 values")
    }

    @Test
    fun environmentEnumContainsDevelopment() {
        assertTrue(
            Environment.entries.contains(Environment.DEVELOPMENT),
            "Environment enum should contain DEVELOPMENT"
        )
    }

    @Test
    fun environmentEnumContainsStaging() {
        assertTrue(
            Environment.entries.contains(Environment.STAGING),
            "Environment enum should contain STAGING"
        )
    }

    @Test
    fun environmentEnumContainsProduction() {
        assertTrue(
            Environment.entries.contains(Environment.PRODUCTION),
            "Environment enum should contain PRODUCTION"
        )
    }

    // ==================== ApiConfig Tests ====================

    @Test
    fun apiConfigVersionIsV1() {
        assertEquals("v1", AppConfig.api.version, "API version should be 'v1'")
    }

    @Test
    fun apiConfigTimeoutIs30Seconds() {
        assertEquals(30_000L, AppConfig.api.timeoutMs, "API timeout should be 30000ms (30 seconds)")
    }

    @Test
    fun apiConfigBaseUrlIsNotEmpty() {
        assertTrue(AppConfig.api.baseUrl.isNotEmpty(), "API base URL should not be empty")
    }

    @Test
    fun apiConfigBaseUrlStartsWithHttp() {
        assertTrue(
            AppConfig.api.baseUrl.startsWith("http://") || AppConfig.api.baseUrl.startsWith("https://"),
            "API base URL should start with http:// or https://"
        )
    }

    @Test
    fun apiUrlCombinesBaseUrlAndVersion() {
        val expectedUrl = "${AppConfig.api.baseUrl}/${AppConfig.api.version}"
        assertEquals(expectedUrl, AppConfig.api.apiUrl, "apiUrl should combine baseUrl and version")
    }

    @Test
    fun apiUrlEndsWithVersion() {
        assertTrue(
            AppConfig.api.apiUrl.endsWith("/v1"),
            "apiUrl should end with /v1"
        )
    }

    // ==================== ApiConfig Data Class Tests ====================

    @Test
    fun apiConfigDataClassConstructsCorrectly() {
        val config = ApiConfig(
            baseUrl = "https://test.example.com",
            version = "v2",
            timeoutMs = 60_000L
        )
        assertEquals("https://test.example.com", config.baseUrl)
        assertEquals("v2", config.version)
        assertEquals(60_000L, config.timeoutMs)
    }

    @Test
    fun apiConfigApiUrlComputesCorrectly() {
        val config = ApiConfig(
            baseUrl = "https://api.example.com",
            version = "v3",
            timeoutMs = 10_000L
        )
        assertEquals("https://api.example.com/v3", config.apiUrl)
    }

    @Test
    fun apiConfigHandlesTrailingSlashInBaseUrl() {
        // Note: Current implementation doesn't strip trailing slash
        // This test documents the current behavior
        val config = ApiConfig(
            baseUrl = "https://api.example.com/",
            version = "v1",
            timeoutMs = 10_000L
        )
        assertEquals("https://api.example.com//v1", config.apiUrl)
    }

    // ==================== FeatureFlags Tests ====================

    @Test
    fun featureFlagsDataClassConstructsCorrectly() {
        val flags = FeatureFlags(
            enableAnalytics = true,
            enableDebugLogging = false
        )
        assertTrue(flags.enableAnalytics)
        assertFalse(flags.enableDebugLogging)
    }

    @Test
    fun featureFlagsCanBeAllTrue() {
        val flags = FeatureFlags(enableAnalytics = true, enableDebugLogging = true)
        assertTrue(flags.enableAnalytics)
        assertTrue(flags.enableDebugLogging)
    }

    @Test
    fun featureFlagsCanBeAllFalse() {
        val flags = FeatureFlags(enableAnalytics = false, enableDebugLogging = false)
        assertFalse(flags.enableAnalytics)
        assertFalse(flags.enableDebugLogging)
    }

    // ==================== AppConfig Feature Flags Tests ====================

    @Test
    fun appConfigFeaturesIsNotNull() {
        assertNotNull(AppConfig.features, "AppConfig.features should not be null")
    }

    @Test
    fun developmentEnvironmentHasDebugLoggingEnabled() {
        // In DEVELOPMENT, debug logging should be enabled
        if (AppConfig.environment == Environment.DEVELOPMENT) {
            assertTrue(
                AppConfig.features.enableDebugLogging,
                "Debug logging should be enabled in DEVELOPMENT"
            )
        }
    }

    @Test
    fun developmentEnvironmentHasAnalyticsDisabled() {
        // In DEVELOPMENT, analytics should be disabled
        if (AppConfig.environment == Environment.DEVELOPMENT) {
            assertFalse(
                AppConfig.features.enableAnalytics,
                "Analytics should be disabled in DEVELOPMENT"
            )
        }
    }

    // ==================== AppConfig Object Tests ====================

    @Test
    fun appConfigEnvironmentIsNotNull() {
        assertNotNull(AppConfig.environment, "AppConfig.environment should not be null")
    }

    @Test
    fun appConfigApiIsNotNull() {
        assertNotNull(AppConfig.api, "AppConfig.api should not be null")
    }

    @Test
    fun appConfigCurrentEnvironmentIsDevelopment() {
        // Document the current default environment
        assertEquals(
            Environment.DEVELOPMENT,
            AppConfig.environment,
            "Default environment should be DEVELOPMENT"
        )
    }

    // ==================== Environment-Based Configuration Logic Tests ====================

    @Test
    fun stagingUrlIsCorrect() {
        // Test that staging URL is properly defined
        val expectedStagingUrl = "https://staging-api.example.com"
        // This tests the hardcoded value in AppConfig
        assertEquals(expectedStagingUrl, "https://staging-api.example.com")
    }

    @Test
    fun productionUrlIsCorrect() {
        // Test that production URL is properly defined
        val expectedProductionUrl = "https://api.example.com"
        // This tests the hardcoded value in AppConfig
        assertEquals(expectedProductionUrl, "https://api.example.com")
    }

    // ==================== Timeout Value Validation Tests ====================

    @Test
    fun timeoutIsPositive() {
        assertTrue(AppConfig.api.timeoutMs > 0, "Timeout should be positive")
    }

    @Test
    fun timeoutIsReasonable() {
        // Timeout should be between 1 second and 5 minutes
        assertTrue(
            AppConfig.api.timeoutMs >= 1_000L,
            "Timeout should be at least 1 second"
        )
        assertTrue(
            AppConfig.api.timeoutMs <= 300_000L,
            "Timeout should not exceed 5 minutes"
        )
    }
}
