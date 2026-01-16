package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object RoomTable : Table("room") {
    val id = varchar("id", 36)
    val desc = text("desc")
    val itemIds = text("item_ids")
    val creatureIds = text("creature_ids")
    val exitIds = text("exit_ids")
    val features = text("features")

    override val primaryKey = PrimaryKey(id)
}
