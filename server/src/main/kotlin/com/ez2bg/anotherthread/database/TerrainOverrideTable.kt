package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object TerrainOverrideTable : Table("terrain_override") {
    val locationId = varchar("location_id", 36)
    val overridesJson = text("overrides_json")
    val updatedBy = varchar("updated_by", 36).nullable()
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(locationId)
}
