package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.SchemaUtils
import com.ez2bg.anotherthread.database.CombatEventLogTable

/**
 * Migration V001: Add combat_event_log table for auditing combat events.
 *
 * This table stores detailed combat event logs including:
 * - Session lifecycle (created, ended, player joined/left)
 * - Combat actions (attacks, abilities, damage, healing)
 * - State changes (player downed, died, creature removed)
 * - Full combatant stat snapshots at time of event
 */
class V001_AddCombatEventLogTable : Migration(
    version = 1,
    name = "Add combat_event_log table"
) {
    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(CombatEventLogTable)
    }

    override fun down() {
        SchemaUtils.drop(CombatEventLogTable)
    }
}
