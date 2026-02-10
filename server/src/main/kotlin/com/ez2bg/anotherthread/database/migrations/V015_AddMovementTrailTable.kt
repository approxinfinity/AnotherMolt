package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Migration to add the movement_trail table for tracking player/creature movements.
 *
 * This table stores recent movement events so players with the Track ability
 * can see who passed through a location recently.
 */
class V015_AddMovementTrailTable : Migration(15, "Add movement_trail table for tracking") {
    private val log = LoggerFactory.getLogger(V015_AddMovementTrailTable::class.java)

    override fun up() {
        transaction {
            log.info("Creating movement_trail table")
            exec("""
                CREATE TABLE IF NOT EXISTS movement_trail (
                    id VARCHAR(36) PRIMARY KEY,
                    location_id VARCHAR(64) NOT NULL,
                    entity_id VARCHAR(36) NOT NULL,
                    entity_type VARCHAR(16) NOT NULL,
                    entity_name TEXT NOT NULL,
                    direction_from TEXT,
                    direction_to TEXT,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())

            // Index for efficient location lookups
            exec("""
                CREATE INDEX IF NOT EXISTS idx_movement_trail_location
                ON movement_trail(location_id, timestamp DESC)
            """.trimIndent())

            // Index for cleanup of old trails
            exec("""
                CREATE INDEX IF NOT EXISTS idx_movement_trail_timestamp
                ON movement_trail(timestamp)
            """.trimIndent())

            log.info("Successfully created movement_trail table with indexes")
        }
    }
}
