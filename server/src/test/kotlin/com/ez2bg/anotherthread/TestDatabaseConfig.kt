package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.DatabaseConfig
import java.io.File

/**
 * Centralized test database configuration to ensure all tests use the same database connection.
 *
 * Problem: Exposed's Database.connect() is global - the last connection wins. When multiple test
 * classes each call DatabaseConfig.init() with different temp files, they clobber each other's
 * database connections, causing "no such table" errors.
 *
 * Solution: Use a single shared test database file that's initialized once and reused by all tests.
 */
object TestDatabaseConfig {
    @Volatile
    private var initialized = false

    private val testDbFile: File by lazy {
        File.createTempFile("shared_test_db_", ".db").also { it.deleteOnExit() }
    }

    /**
     * Initialize the shared test database. Thread-safe and idempotent.
     * Subsequent calls are no-ops to prevent database connection clobbering.
     */
    @Synchronized
    fun init() {
        if (!initialized) {
            DatabaseConfig.init(testDbFile.absolutePath)
            initialized = true
        }
    }

    /**
     * Get the path to the shared test database file.
     */
    fun getDbPath(): String = testDbFile.absolutePath
}
