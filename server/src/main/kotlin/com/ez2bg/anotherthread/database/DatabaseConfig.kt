package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseConfig {
    fun init(dbPath: String = "data/anotherthread.db") {
        // Ensure parent directory exists
        File(dbPath).parentFile?.mkdirs()

        Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                LocationTable,
                CreatureTable,
                ItemTable,
                UserTable,
                FeatureCategoryTable,
                FeatureTable,
                AuditLogTable
            )
        }
    }

    /**
     * Clear all data from tables. Useful for test setup.
     */
    fun clearAllTables() {
        transaction {
            UserTable.deleteAll()
            CreatureTable.deleteAll()
            ItemTable.deleteAll()
            LocationTable.deleteAll()
            FeatureTable.deleteAll()
            FeatureCategoryTable.deleteAll()
        }
    }
}
