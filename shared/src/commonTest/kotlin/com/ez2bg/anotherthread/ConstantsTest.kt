package com.ez2bg.anotherthread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for Constants.kt values.
 */
class ConstantsTest {

    // ==================== SERVER_PORT Tests ====================

    @Test
    fun serverPortIsValidPortNumber() {
        assertTrue(
            SERVER_PORT in 1..65535,
            "SERVER_PORT should be a valid port number (1-65535)"
        )
    }

    @Test
    fun serverPortIsNotPrivileged() {
        assertTrue(
            SERVER_PORT > 1024,
            "SERVER_PORT should not be a privileged port (>1024)"
        )
    }

    @Test
    fun serverPortValue() {
        assertEquals(8080, SERVER_PORT, "SERVER_PORT should be 8080")
    }

    // ==================== API_BASE_URL Tests ====================

    @Test
    fun apiBaseUrlDelegatesToAppConfig() {
        assertEquals(
            AppConfig.api.baseUrl,
            API_BASE_URL,
            "API_BASE_URL should delegate to AppConfig.api.baseUrl"
        )
    }

    @Test
    fun apiBaseUrlIsNotEmpty() {
        assertTrue(API_BASE_URL.isNotEmpty(), "API_BASE_URL should not be empty")
    }

    @Test
    fun apiBaseUrlStartsWithHttp() {
        assertTrue(
            API_BASE_URL.startsWith("http://") || API_BASE_URL.startsWith("https://"),
            "API_BASE_URL should start with http:// or https://"
        )
    }

    // ==================== API_TIMEOUT_MS Tests ====================

    @Test
    fun apiTimeoutMsDelegatesToAppConfig() {
        assertEquals(
            AppConfig.api.timeoutMs,
            API_TIMEOUT_MS,
            "API_TIMEOUT_MS should delegate to AppConfig.api.timeoutMs"
        )
    }

    @Test
    fun apiTimeoutMsIsPositive() {
        assertTrue(API_TIMEOUT_MS > 0, "API_TIMEOUT_MS should be positive")
    }

    @Test
    fun apiTimeoutMsValue() {
        assertEquals(30_000L, API_TIMEOUT_MS, "API_TIMEOUT_MS should be 30000ms")
    }
}
