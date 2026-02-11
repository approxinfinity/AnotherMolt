package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object LocationTable : Table("location") {
    val id = varchar("id", 64)  // Extended for readable IDs like "location-classic-dungeon-mind-flayer-sanctum"
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
    val gridZ = integer("grid_z").nullable()  // Z coordinate for vertical stacking (UP/DOWN exits). Default 0 for ground level.
    // Area identifier - groups locations into distinct map regions (e.g., "overworld", "fungus-forest")
    // Locations in different areas are logically separate even if coordinates overlap
    val areaId = varchar("area_id", 100).nullable()
    // Last edited tracking - null means never edited by a user (e.g., auto-generated wilderness)
    val lastEditedBy = varchar("last_edited_by", 36).nullable()
    val lastEditedAt = text("last_edited_at").nullable() // ISO datetime string
    // Location type for determining behavior (e.g., wilderness generation)
    val locationType = varchar("location_type", 50).nullable()
    // Biome metadata from world generation (nullable for backward compatibility)
    val biome = varchar("biome", 50).nullable()
    val elevation = float("elevation").nullable()
    val moisture = float("moisture").nullable()
    val isRiver = bool("is_river").nullable()
    val isCoast = bool("is_coast").nullable()
    val terrainFeatures = text("terrain_features").nullable() // JSON string list
    val isOriginalTerrain = bool("is_original_terrain").nullable()
    // Shop layout direction (VERTICAL or HORIZONTAL) - null defaults to VERTICAL
    val shopLayoutDirection = varchar("shop_layout_direction", 20).nullable()
    // Lock level for lockpicking: null/0 = unlocked, 1-4 = Simple/Standard/Complex/Master
    val lockLevel = integer("lock_level").nullable()

    override val primaryKey = PrimaryKey(id)
}
