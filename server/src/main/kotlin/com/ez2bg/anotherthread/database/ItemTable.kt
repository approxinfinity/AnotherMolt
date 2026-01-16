package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object ItemTable : Table("item") {
    val id = varchar("id", 36)
    val desc = text("desc")
    val featureIds = text("feature_ids")

    override val primaryKey = PrimaryKey(id)
}
