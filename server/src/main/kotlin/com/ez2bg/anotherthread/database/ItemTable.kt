package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object ItemTable : Table("item") {
    val id = varchar("id", 36)
    val name = text("name")
    val desc = text("desc")
    val featureIds = text("feature_ids")
    val imageUrl = text("image_url").nullable()

    override val primaryKey = PrimaryKey(id)
}
