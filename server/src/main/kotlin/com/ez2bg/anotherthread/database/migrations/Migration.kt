package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Database migration tracking table.
 * Stores which migrations have been applied.
 */
object MigrationTable : Table("schema_migrations") {
    val version = integer("version").uniqueIndex()
    val name = varchar("name", 255)
    val appliedAt = long("applied_at")

    override val primaryKey = PrimaryKey(version)
}

/**
 * Base class for database migrations.
 */
abstract class Migration(
    val version: Int,
    val name: String
) {
    abstract fun up()

    // Optional: implement down() for rollback support
    open fun down() {
        throw UnsupportedOperationException("Rollback not implemented for migration $version: $name")
    }
}

/**
 * Migration runner that applies pending migrations in order.
 */
object MigrationRunner {
    private val log = LoggerFactory.getLogger(MigrationRunner::class.java)

    /**
     * All available migrations, in order.
     * Add new migrations to this list.
     */
    private val migrations: List<Migration> = listOf(
        V001_AddCombatEventLogTable(),
        V002_AddDamageDiceColumns(),
        V003_AddAbilityMinLevel(),
        V004_AddLearnedAbilityIds(),
        V005_AddItemWeight(),
        V006_AddStealthStatus(),
        V007_AddLocationItems(),
        V008_DropClassGenerationStartedAt(),
        V009_AddVisibleAbilityIds(),
        V010_AddPartyFields(),
        V011_WidenLocationIdColumn()
    )

    /**
     * Ensure the migrations table exists.
     */
    fun init() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(MigrationTable)
        }
    }

    /**
     * Get list of already applied migration versions.
     */
    fun getAppliedVersions(): Set<Int> = transaction {
        MigrationTable.selectAll()
            .map { it[MigrationTable.version] }
            .toSet()
    }

    /**
     * Get pending migrations that haven't been applied yet.
     */
    fun getPendingMigrations(): List<Migration> {
        val applied = getAppliedVersions()
        return migrations.filter { it.version !in applied }.sortedBy { it.version }
    }

    /**
     * Run all pending migrations.
     * Returns the number of migrations applied.
     */
    fun runPendingMigrations(): Int {
        init()

        val pending = getPendingMigrations()
        if (pending.isEmpty()) {
            log.info("No pending migrations")
            return 0
        }

        log.info("Found ${pending.size} pending migration(s)")

        var applied = 0
        for (migration in pending) {
            try {
                log.info("Applying migration ${migration.version}: ${migration.name}")

                transaction {
                    migration.up()

                    // Record the migration
                    MigrationTable.insert {
                        it[version] = migration.version
                        it[name] = migration.name
                        it[appliedAt] = System.currentTimeMillis()
                    }
                }

                log.info("Migration ${migration.version} applied successfully")
                applied++
            } catch (e: Exception) {
                log.error("Failed to apply migration ${migration.version}: ${e.message}", e)
                throw RuntimeException("Migration ${migration.version} failed: ${e.message}", e)
            }
        }

        log.info("Applied $applied migration(s)")
        return applied
    }

    /**
     * Get migration status report.
     */
    fun getStatus(): MigrationStatus {
        init()

        val applied = getAppliedVersions()
        val pending = getPendingMigrations()

        val appliedMigrations = transaction {
            MigrationTable.selectAll()
                .orderBy(MigrationTable.version, SortOrder.ASC)
                .map {
                    AppliedMigration(
                        version = it[MigrationTable.version],
                        name = it[MigrationTable.name],
                        appliedAt = it[MigrationTable.appliedAt]
                    )
                }
        }

        return MigrationStatus(
            appliedMigrations = appliedMigrations,
            pendingMigrations = pending.map { PendingMigration(it.version, it.name) },
            totalMigrations = migrations.size
        )
    }
}

data class AppliedMigration(
    val version: Int,
    val name: String,
    val appliedAt: Long
)

data class PendingMigration(
    val version: Int,
    val name: String
)

data class MigrationStatus(
    val appliedMigrations: List<AppliedMigration>,
    val pendingMigrations: List<PendingMigration>,
    val totalMigrations: Int
)
