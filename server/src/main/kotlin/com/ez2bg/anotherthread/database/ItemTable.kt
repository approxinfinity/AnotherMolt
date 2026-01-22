package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object ItemTable : Table("item") {
    val id = varchar("id", 36)
    val name = text("name")
    val desc = text("desc")
    val featureIds = text("feature_ids")
    val abilityIds = text("ability_ids").default("[]")  // JSON array of ability IDs
    val imageUrl = text("image_url").nullable()
    val lockedBy = varchar("locked_by", 36).nullable()

    override val primaryKey = PrimaryKey(id)
}
