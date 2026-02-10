package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Migration to widen all location_id columns from VARCHAR(36) to TEXT.
 *
 * Location IDs are human-readable strings that can exceed 36 characters.
 * This affects:
 * - combat_session.location_id
 * - chest.location_id
 * - player_encounter.last_location_id
 * - combat_event_log.location_id
 * - terrain_override.location_id
 */
class V012_WidenAllLocationIdColumns : Migration(12, "Widen all location_id columns to TEXT") {
    private val log = LoggerFactory.getLogger(V012_WidenAllLocationIdColumns::class.java)

    override fun up() {
        transaction {
            log.info("Widening all location_id columns to TEXT")

            // Helper to widen a regular column
            fun widenColumn(tableName: String, columnName: String) {
                log.info("Widening $tableName.$columnName")
                exec("ALTER TABLE $tableName ADD COLUMN ${columnName}_new TEXT DEFAULT NULL")
                exec("UPDATE $tableName SET ${columnName}_new = $columnName")
                exec("ALTER TABLE $tableName DROP COLUMN $columnName")
                exec("ALTER TABLE $tableName RENAME COLUMN ${columnName}_new TO $columnName")
            }

            // combat_session.location_id
            widenColumn("combat_session", "location_id")

            // chest.location_id
            widenColumn("chest", "location_id")

            // player_encounter.last_location_id
            widenColumn("player_encounter", "last_location_id")

            // combat_event_log.location_id - has an index that needs to be dropped first
            log.info("Widening combat_event_log.location_id (with index)")
            exec("DROP INDEX IF EXISTS combat_event_log_location_id")
            exec("ALTER TABLE combat_event_log ADD COLUMN location_id_new TEXT DEFAULT NULL")
            exec("UPDATE combat_event_log SET location_id_new = location_id")
            exec("ALTER TABLE combat_event_log DROP COLUMN location_id")
            exec("ALTER TABLE combat_event_log RENAME COLUMN location_id_new TO location_id")
            exec("CREATE INDEX combat_event_log_location_id ON combat_event_log (location_id)")

            // terrain_override.location_id (this is the primary key, need special handling)
            log.info("Widening primary key terrain_override.location_id")
            exec("""
                CREATE TABLE terrain_override_new (
                    location_id TEXT NOT NULL PRIMARY KEY,
                    overrides_json TEXT NOT NULL,
                    updated_by VARCHAR(36) NULL,
                    updated_at BIGINT NOT NULL
                )
            """.trimIndent())
            exec("INSERT INTO terrain_override_new SELECT * FROM terrain_override")
            exec("DROP TABLE terrain_override")
            exec("ALTER TABLE terrain_override_new RENAME TO terrain_override")

            log.info("Successfully widened all location_id columns to TEXT")
        }
    }
}
