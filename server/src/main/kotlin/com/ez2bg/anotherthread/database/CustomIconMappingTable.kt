package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object CustomIconMappingTable : Table("custom_icon_mapping") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val abilityId = varchar("ability_id", 36)
    val iconName = text("icon_name")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
