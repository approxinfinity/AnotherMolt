package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Migration to widen the current_location_id column from VARCHAR(36) to TEXT.
 *
 * The original column size was based on UUID format (36 chars), but location IDs
 * are human-readable strings like "location-classic-dungeon-vermin-tunnels" (39 chars)
 * which exceeds the limit and causes navigation to silently fail.
 *
 * SQLite doesn't strictly enforce VARCHAR lengths at runtime, but Exposed does.
 * This migration uses SQLite's ALTER TABLE to recreate the column as TEXT.
 */
class V011_WidenLocationIdColumn : Migration(11, "Widen current_location_id column to TEXT") {
    private val log = LoggerFactory.getLogger(V011_WidenLocationIdColumn::class.java)

    override fun up() {
        transaction {
            log.info("Widening user.current_location_id column from VARCHAR(36) to TEXT")

            // SQLite approach: create new column, copy data, drop old, rename new
            // Step 1: Add new TEXT column
            exec("ALTER TABLE user ADD COLUMN current_location_id_new TEXT DEFAULT NULL")

            // Step 2: Copy data from old column to new
            exec("UPDATE user SET current_location_id_new = current_location_id")

            // Step 3: Drop old column (SQLite 3.35.0+)
            exec("ALTER TABLE user DROP COLUMN current_location_id")

            // Step 4: Rename new column
            exec("ALTER TABLE user RENAME COLUMN current_location_id_new TO current_location_id")

            log.info("Successfully widened current_location_id column to TEXT")
        }
    }
}
