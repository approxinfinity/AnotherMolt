package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Migration to add visible_ability_ids column to user table.
 * This column stores which abilities are visible on the action bar (max 10).
 * Empty array means show all abilities.
 */
class V009_AddVisibleAbilityIds : Migration(9, "AddVisibleAbilityIds") {
    override fun up() {
        transaction {
            exec("ALTER TABLE user ADD COLUMN visible_ability_ids TEXT DEFAULT '[]' NOT NULL")
        }
    }

    override fun down() {
        transaction {
            // SQLite doesn't support DROP COLUMN in older versions
            // This would require creating a new table and copying data
            throw UnsupportedOperationException("Cannot drop column in SQLite")
        }
    }
}
