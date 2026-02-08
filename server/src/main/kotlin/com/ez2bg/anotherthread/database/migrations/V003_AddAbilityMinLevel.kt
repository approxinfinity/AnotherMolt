package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Migration to add min_level column to ability table.
 *
 * This enables level-gated abilities - abilities that unlock as players level up.
 * Default value is 1 so all existing abilities remain available at level 1.
 */
class V003_AddAbilityMinLevel : Migration(
    version = 3,
    name = "Add min_level column to ability table"
) {
    override fun up() {
        transaction {
            // Check if column already exists
            val abilityColumns = exec("PRAGMA table_info(ability)") { rs ->
                val columns = mutableListOf<String>()
                while (rs.next()) {
                    columns.add(rs.getString("name"))
                }
                columns
            } ?: emptyList()

            // Add min_level column to ability table if it doesn't exist
            if ("min_level" !in abilityColumns) {
                exec("""
                    ALTER TABLE ability
                    ADD COLUMN min_level INTEGER DEFAULT 1 NOT NULL
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
