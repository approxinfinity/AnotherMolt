package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object PuzzleTable : Table("puzzle") {
    val id = varchar("id", 64)
    val locationId = varchar("location_id", 64)
    val puzzleData = text("puzzle_data")  // JSON blob containing full Puzzle object

    override val primaryKey = PrimaryKey(id)
}
