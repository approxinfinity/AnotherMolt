package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object TerrainOverrideTable : Table("terrain_override") {
    val locationId = text("location_id")  // Human-readable location IDs can exceed 36 chars
    val overridesJson = text("overrides_json")
    val updatedBy = varchar("updated_by", 36).nullable()
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(locationId)
}
