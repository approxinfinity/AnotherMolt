package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object CreatureTable : Table("creature") {
    val id = varchar("id", 36)
    val name = text("name")
    val desc = text("desc")
    val itemIds = text("item_ids")
    val featureIds = text("feature_ids")
    val imageUrl = text("image_url").nullable()
    val lockedBy = varchar("locked_by", 36).nullable()
    // Combat stats
    val maxHp = integer("max_hp").default(10)
    val baseDamage = integer("base_damage").default(5)
    val abilityIds = text("ability_ids").default("[]")
    val level = integer("level").default(1)
    val experienceValue = integer("experience_value").default(10)
    val isAggressive = bool("is_aggressive").default(false)  // Auto-attacks players

    override val primaryKey = PrimaryKey(id)
}
