package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object LocationTable : Table("location") {
    val id = varchar("id", 36)
    val name = text("name")
    val desc = text("desc")
    val itemIds = text("item_ids")
    val creatureIds = text("creature_ids")
    val exitIds = text("exit_ids")
    val featureIds = text("feature_ids")
    val imageUrl = text("image_url").nullable()
    val lockedBy = varchar("locked_by", 36).nullable()

    override val primaryKey = PrimaryKey(id)
}
