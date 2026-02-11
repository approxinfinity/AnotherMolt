package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

/**
 * Database table for magical pools.
 * Pools are interactive features found in dungeons that can have various effects
 * when players interact with them (drink, enter, touch, etc.)
 */
object PoolTable : Table("pool") {
    val id = text("id")
    val name = text("name")
    val description = text("description")
    val locationId = text("location_id")

    // Visual appearance
    val liquidColor = text("liquid_color")  // "clear", "green", "red", "black", etc.
    val liquidAppearance = text("liquid_appearance")  // "bubbling", "still", "glowing", etc.

    // Pool behavior/effect
    val effectType = text("effect_type")  // "healing", "damage", "buff", "debuff", "transform", "teleport", "trap", "treasure", "empty"
    val effectData = text("effect_data")  // JSON blob with effect-specific parameters

    // Usage limits
    val usesPerDay = integer("uses_per_day").default(0)  // 0 = unlimited
    val isOneTimeUse = bool("is_one_time_use").default(false)  // true = destroyed after first use

    // Discovery/identification
    val isHidden = bool("is_hidden").default(false)  // Must be discovered first
    val identifyDifficulty = integer("identify_difficulty").default(0)  // 0 = obvious, 1-5 = requires skill check

    override val primaryKey = PrimaryKey(id)
}
