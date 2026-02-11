package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.database.migrations.MigrationRunner
import com.ez2bg.anotherthread.game.CharmedCreatureTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File

object DatabaseConfig {
    private val log = LoggerFactory.getLogger(DatabaseConfig::class.java)

    /**
     * Initialize the database.
     *
     * @param dbPath Path to the SQLite database file
     * @param useMigrations If true, use migration system for schema changes.
     *                      If false (default for dev), create all tables directly.
     */
    fun init(dbPath: String = "data/anotherthread.db", useMigrations: Boolean = false) {
        // Ensure parent directory exists
        File(dbPath).parentFile?.mkdirs()

        Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )

        if (useMigrations) {
            // QA/Production mode: use migrations for schema changes
            log.info("Using migration system for database schema")
            initWithMigrations()
        } else {
            // Dev mode: create all tables directly (fast iteration)
            log.info("Using direct schema creation (dev mode)")
            initDirect()
        }
    }

    /**
     * Initialize using migrations (for QA/production).
     * Core tables are created directly, additional tables via migrations.
     */
    private fun initWithMigrations() {
        transaction {
            // Create core tables that exist since the beginning
            SchemaUtils.createMissingTablesAndColumns(
                LocationTable,
                CreatureTable,
                ItemTable,
                UserTable,
                FeatureCategoryTable,
                FeatureTable,
                FeatureStateTable,
                AuditLogTable,
                TerrainOverrideTable,
                CharacterClassTable,
                AbilityTable,
                NerfRequestTable,
                CombatSessionTable,
                LootTableTable,
                ChestTable,
                IdentifiedEntityTable,
                SessionTable,
                CustomIconMappingTable,
                PlayerEncounterTable,
                GameConfigTable
                // Note: CombatEventLogTable is added via migration V001
            )
        }

        // Run any pending migrations
        val applied = MigrationRunner.runPendingMigrations()
        log.info("Applied $applied migration(s)")
    }

    /**
     * Initialize directly (for dev - creates all tables).
     */
    private fun initDirect() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                LocationTable,
                CreatureTable,
                ItemTable,
                UserTable,
                FeatureCategoryTable,
                FeatureTable,
                FeatureStateTable,
                AuditLogTable,
                TerrainOverrideTable,
                CharacterClassTable,
                AbilityTable,
                NerfRequestTable,
                CombatSessionTable,
                LootTableTable,
                ChestTable,
                IdentifiedEntityTable,
                SessionTable,
                CustomIconMappingTable,
                PlayerEncounterTable,
                CombatEventLogTable,
                GameConfigTable,
                CharmedCreatureTable,
                UserFoodItemTable,
                PoolTable,
                TrapTable,
                FactionTable,
                FactionRelationTable,
                PlayerFactionStandingTable
            )
        }
    }

    /**
     * Clear all data from tables. Useful for test setup.
     */
    fun clearAllTables() {
        transaction {
            IdentifiedEntityTable.deleteAll()
            UserTable.deleteAll()
            CreatureTable.deleteAll()
            ItemTable.deleteAll()
            LocationTable.deleteAll()
            FeatureTable.deleteAll()
            FeatureCategoryTable.deleteAll()
        }
    }
}
