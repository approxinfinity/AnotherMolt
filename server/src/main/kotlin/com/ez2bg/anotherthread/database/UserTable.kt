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
    val classGenerationStartedAt = long("class_generation_started_at").nullable()
    val createdAt = long("created_at")
    val lastActiveAt = long("last_active_at")
    // Combat stats
    val level = integer("level").default(1)
    val experience = integer("experience").default(0)
    val maxHp = integer("max_hp").default(10)
    val currentHp = integer("current_hp").default(10)
    val currentCombatSessionId = varchar("current_combat_session_id", 36).nullable()

    override val primaryKey = PrimaryKey(id)
}
