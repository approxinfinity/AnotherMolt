package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Migration to add location_item and discovered_item tables.
 * These track items on the ground with drop timestamps for hidden item mechanics.
 *
 * - location_item: Items placed/dropped at locations with timestamps
 * - discovered_item: Tracks which users have discovered which hidden items
 */
class V007_AddLocationItems : Migration(version = 7, name = "AddLocationItems") {
    override fun up() {
        transaction {
            // Create location_item table
            exec("""
                CREATE TABLE IF NOT EXISTS location_item (
                    id VARCHAR(36) PRIMARY KEY,
                    location_id VARCHAR(64) NOT NULL,
                    item_id VARCHAR(36) NOT NULL,
                    dropped_at INTEGER NOT NULL,
                    dropped_by_user_id VARCHAR(36)
                )
            """)

            // Create indices for location_item
            exec("CREATE INDEX IF NOT EXISTS idx_location_item_location ON location_item(location_id)")
            exec("CREATE INDEX IF NOT EXISTS idx_location_item_item ON location_item(item_id)")

            // Create discovered_item table
            exec("""
                CREATE TABLE IF NOT EXISTS discovered_item (
                    user_id VARCHAR(36) NOT NULL,
                    location_item_id VARCHAR(36) NOT NULL,
                    discovered_at INTEGER NOT NULL,
                    PRIMARY KEY (user_id, location_item_id)
                )
            """)
        }
    }
}
