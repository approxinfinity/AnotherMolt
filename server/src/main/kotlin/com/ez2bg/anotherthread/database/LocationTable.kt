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
    // Grid coordinates - null means not yet placed in a coordinate system
    val gridX = integer("grid_x").nullable()
    val gridY = integer("grid_y").nullable()
    val gridZ = integer("grid_z").nullable()
    // Last edited tracking - null means never edited by a user (e.g., auto-generated wilderness)
    val lastEditedBy = varchar("last_edited_by", 36).nullable()
    val lastEditedAt = text("last_edited_at").nullable() // ISO datetime string
    // Location type for determining behavior (e.g., wilderness generation)
    val locationType = varchar("location_type", 50).nullable()

    override val primaryKey = PrimaryKey(id)
}
