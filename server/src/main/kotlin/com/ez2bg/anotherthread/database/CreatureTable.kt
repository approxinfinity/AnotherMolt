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
    val experienceValue = integer("experience_value").default(10)  // Base XP, scaled by level difference
    val challengeRating = integer("challenge_rating").default(1)  // 1-20 scale, determines difficulty tier
    val isAggressive = bool("is_aggressive").default(false)  // Auto-attacks players
    // Loot fields
    val lootTableId = varchar("loot_table_id", 36).nullable()
    val minGoldDrop = integer("min_gold_drop").default(0)
    val maxGoldDrop = integer("max_gold_drop").default(0)

    override val primaryKey = PrimaryKey(id)
}
