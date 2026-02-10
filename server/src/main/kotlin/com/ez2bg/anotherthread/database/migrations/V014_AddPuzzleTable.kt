package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Migration to add the puzzle table for lever puzzles and secret passages.
 */
class V014_AddPuzzleTable : Migration(14, "Add puzzle table for lever puzzles and secret passages") {
    private val log = LoggerFactory.getLogger(V014_AddPuzzleTable::class.java)

    override fun up() {
        transaction {
            log.info("Creating puzzle table")
            exec("""
                CREATE TABLE IF NOT EXISTS puzzle (
                    id VARCHAR(64) PRIMARY KEY,
                    location_id VARCHAR(64) NOT NULL,
                    puzzle_data TEXT NOT NULL
                )
            """.trimIndent())
            log.info("Successfully created puzzle table")
        }
    }
}
