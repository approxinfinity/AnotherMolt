package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Migration to add the lock_level column to the location table.
 *
 * Lock levels:
 * - null/0: Unlocked (default)
 * - 1: Simple Lock
 * - 2: Standard Lock
 * - 3: Complex Lock
 * - 4: Master Lock
 *
 * Locked locations require lockpicking to access.
 */
class V017_AddLocationLockLevel : Migration(17, "Add lock_level column to location table") {
    private val log = LoggerFactory.getLogger(V017_AddLocationLockLevel::class.java)

    override fun up() {
        transaction {
            log.info("Adding lock_level column to location table")

            // Add the lock_level column
            exec("""
                ALTER TABLE location ADD COLUMN lock_level INTEGER DEFAULT NULL
            """.trimIndent())

            log.info("Successfully added lock_level column to location table")
        }
    }
}
