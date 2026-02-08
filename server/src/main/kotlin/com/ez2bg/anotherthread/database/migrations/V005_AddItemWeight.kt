package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds weight column to items table for encumbrance system.
 * Default weight is 1 stone per item.
 */
class V005_AddItemWeight : Migration(
    version = 5,
    name = "AddItemWeight"
) {
    override fun up() {
        transaction {
            exec("ALTER TABLE item ADD COLUMN weight INTEGER DEFAULT 1")
        }
    }

    override fun down() {
        transaction {
            exec("ALTER TABLE item DROP COLUMN weight")
        }
    }
}
