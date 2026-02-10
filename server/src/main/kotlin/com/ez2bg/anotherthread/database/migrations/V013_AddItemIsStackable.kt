package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Migration to add is_stackable column to item table.
 *
 * Most items should NOT stack by default. Only consumables like potions
 * and ammunition like arrows should be marked as stackable.
 */
class V013_AddItemIsStackable : Migration(13, "Add is_stackable column to item table") {
    private val log = LoggerFactory.getLogger(V013_AddItemIsStackable::class.java)

    override fun up() {
        transaction {
            log.info("Adding is_stackable column to item table")
            exec("ALTER TABLE item ADD COLUMN is_stackable INTEGER NOT NULL DEFAULT 0")
            log.info("Successfully added is_stackable column")
        }
    }
}
