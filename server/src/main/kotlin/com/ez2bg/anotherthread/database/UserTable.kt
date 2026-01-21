package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object UserTable : Table("user") {
    val id = varchar("id", 36)
    val name = text("name").uniqueIndex()
    val passwordHash = text("password_hash")
    val desc = text("desc").default("")
    val itemIds = text("item_ids").default("[]")
    val featureIds = text("feature_ids").default("[]")
    val imageUrl = text("image_url").nullable()
    val currentLocationId = varchar("current_location_id", 36).nullable()
    val characterClassId = varchar("character_class_id", 36).nullable()
    val createdAt = long("created_at")
    val lastActiveAt = long("last_active_at")

    override val primaryKey = PrimaryKey(id)
}
