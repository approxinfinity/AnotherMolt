package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Migration to add damageDice columns to ability and creature tables.
 *
 * The damageDice field uses XdY+Z notation (e.g., "2d6+3") for variable damage.
 * This provides more interesting combat variance compared to flat baseDamage.
 *
 * - ability.damage_dice: Dice formula for ability damage
 * - creature.damage_dice: Dice formula for creature auto-attacks
 *
 * If damageDice is null, the system falls back to baseDamage.
 */
class V002_AddDamageDiceColumns : Migration(
    version = 2,
    name = "Add damage_dice columns for XdY dice notation"
) {
    override fun up() {
        transaction {
            // Check if columns already exist (Exposed's createMissingTablesAndColumns may have added them)
            // SQLite PRAGMA table_info returns column info
            val abilityColumns = exec("PRAGMA table_info(ability)") { rs ->
                val columns = mutableListOf<String>()
                while (rs.next()) {
                    columns.add(rs.getString("name"))
                }
                columns
            } ?: emptyList()

            val creatureColumns = exec("PRAGMA table_info(creature)") { rs ->
                val columns = mutableListOf<String>()
                while (rs.next()) {
                    columns.add(rs.getString("name"))
                }
                columns
            } ?: emptyList()

            // Add damage_dice column to ability table if it doesn't exist
            if ("damage_dice" !in abilityColumns) {
                exec("""
                    ALTER TABLE ability
                    ADD COLUMN damage_dice TEXT DEFAULT NULL
                """.trimIndent())
            }

            // Add damage_dice column to creature table if it doesn't exist
            if ("damage_dice" !in creatureColumns) {
                exec("""
                    ALTER TABLE creature
                    ADD COLUMN damage_dice TEXT DEFAULT NULL
                """.trimIndent())
            }
        }
    }

    override fun down() {
        transaction {
            // SQLite doesn't support DROP COLUMN directly in older versions,
            // but newer SQLite 3.35+ does. For safety, we'll just leave the columns.
            // To fully rollback, you'd need to recreate the tables without the columns.
            exec("SELECT 1") // No-op
        }
    }
}
