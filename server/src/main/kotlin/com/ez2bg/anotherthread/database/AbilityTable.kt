package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object AbilityTable : Table("ability") {
    val id = varchar("id", 64)  // Extended for readable IDs
    val name = text("name")
    val description = text("description")
    val classId = varchar("class_id", 36).nullable()
    val abilityType = text("ability_type")  // "spell", "combat", "utility", "passive"
    val targetType = text("target_type")    // "self", "single_enemy", "single_ally", "area", "all_enemies", "all_allies"
    val range = integer("range")            // in feet, 0 for self/melee
    val cooldownType = text("cooldown_type") // "none", "short", "medium", "long"
    val cooldownRounds = integer("cooldown_rounds").default(0)
    val effects = text("effects").default("[]")  // JSON array of effect objects
    val imageUrl = text("image_url").nullable()

    // Power budget fields for balance
    val baseDamage = integer("base_damage").default(0)           // Raw damage value (fallback if damageDice is null)
    val damageDice = text("damage_dice").nullable()              // XdY+Z format (e.g., "2d6+3"), preferred over baseDamage
    val durationRounds = integer("duration_rounds").default(0)   // How long effect lasts
    val powerCost = integer("power_cost").default(10)            // Calculated total power cost
    val manaCost = integer("mana_cost").default(0)               // Mana cost for spells
    val staminaCost = integer("stamina_cost").default(0)         // Stamina cost for physical abilities
    val attribution = text("attribution").nullable()             // Content attribution source

    override val primaryKey = PrimaryKey(id)
}
