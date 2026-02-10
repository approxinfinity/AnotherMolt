package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Migration to add the charmed_creature table for tracking charmed creature instances.
 *
 * Charmed creatures are temporary allies that follow the player and can assist in combat.
 * The charm has a duration and can break under certain conditions.
 */
class V016_AddCharmedCreatureTable : Migration(16, "Add charmed_creature table for charm system") {
    private val log = LoggerFactory.getLogger(V016_AddCharmedCreatureTable::class.java)

    override fun up() {
        transaction {
            log.info("Creating charmed_creature table")
            exec("""
                CREATE TABLE IF NOT EXISTS charmed_creature (
                    id VARCHAR(36) PRIMARY KEY,
                    creature_id VARCHAR(36) NOT NULL,
                    charmer_user_id VARCHAR(36) NOT NULL,
                    location_id VARCHAR(64) NOT NULL,
                    current_hp INTEGER NOT NULL,
                    charmed_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    charm_strength INTEGER NOT NULL DEFAULT 50
                )
            """.trimIndent())

            // Index for efficient lookups by charmer
            exec("""
                CREATE INDEX IF NOT EXISTS idx_charmed_creature_charmer
                ON charmed_creature(charmer_user_id)
            """.trimIndent())

            // Index for efficient lookups by location
            exec("""
                CREATE INDEX IF NOT EXISTS idx_charmed_creature_location
                ON charmed_creature(location_id)
            """.trimIndent())

            // Index for cleanup of expired charms
            exec("""
                CREATE INDEX IF NOT EXISTS idx_charmed_creature_expires
                ON charmed_creature(expires_at)
            """.trimIndent())

            log.info("Successfully created charmed_creature table with indexes")
        }
    }
}
