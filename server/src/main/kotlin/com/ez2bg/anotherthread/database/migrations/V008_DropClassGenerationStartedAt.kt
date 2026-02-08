package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Migration V008: Drop the class_generation_started_at column from user table.
 *
 * This column was used to track when class generation started, but that tracking
 * has been moved to in-memory (ClassGenerationTracker) since it's transient state
 * that shouldn't survive server restarts.
 */
class V008_DropClassGenerationStartedAt : Migration(
    version = 8,
    name = "Drop class_generation_started_at column"
) {
    private val log = LoggerFactory.getLogger(V008_DropClassGenerationStartedAt::class.java)

    override fun up() {
        transaction {
            // SQLite doesn't support DROP COLUMN directly in older versions,
            // but since SQLite 3.35.0 (2021-03-12) it does support ALTER TABLE DROP COLUMN.
            // We'll try the direct approach first, and if it fails, the column will
            // simply be ignored (since we removed it from the Kotlin schema).
            try {
                exec("ALTER TABLE user DROP COLUMN class_generation_started_at")
                log.info("Dropped class_generation_started_at column from user table")
            } catch (e: Exception) {
                // If DROP COLUMN isn't supported, log and continue.
                // The column will be ignored by the app since it's not in the schema.
                log.warn("Could not drop column (may require manual cleanup or newer SQLite): ${e.message}")
                log.info("Column will be ignored by the application - no action required")
            }
        }
    }

    override fun down() {
        transaction {
            // Re-add the column if needed (for rollback)
            try {
                exec("ALTER TABLE user ADD COLUMN class_generation_started_at INTEGER")
                log.info("Re-added class_generation_started_at column to user table")
            } catch (e: Exception) {
                log.warn("Could not re-add column: ${e.message}")
            }
        }
    }
}
