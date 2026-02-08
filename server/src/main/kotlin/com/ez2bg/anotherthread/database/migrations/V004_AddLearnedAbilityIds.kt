package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Migration to add learned_ability_ids column to user table.
 *
 * This supports the trainer system where players must visit trainers
 * to learn abilities before they can use them in combat.
 * Stores a JSON array of ability IDs the user has learned.
 */
class V004_AddLearnedAbilityIds : Migration(
    version = 4,
    name = "Add learned_ability_ids column to user table"
) {
    override fun up() {
        transaction {
            // Check if column already exists
            val userColumns = exec("PRAGMA table_info(user)") { rs ->
                val columns = mutableListOf<String>()
                while (rs.next()) {
                    columns.add(rs.getString("name"))
                }
                columns
            } ?: emptyList()

            // Add learned_ability_ids column to user table if it doesn't exist
            if ("learned_ability_ids" !in userColumns) {
                exec("""
                    ALTER TABLE user
                    ADD COLUMN learned_ability_ids TEXT DEFAULT '[]' NOT NULL
                """.trimIndent())
            }
        }
    }

    override fun down() {
        transaction {
            // SQLite 3.35+ supports DROP COLUMN, but for safety we'll leave it
            exec("SELECT 1") // No-op
        }
    }
}
