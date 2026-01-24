package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object ItemTable : Table("item") {
    val id = varchar("id", 36)
    val name = text("name")
    val desc = text("desc")
    val featureIds = text("feature_ids")
    val abilityIds = text("ability_ids").default("[]")  // JSON array of ability IDs
    val imageUrl = text("image_url").nullable()
    val lockedBy = varchar("locked_by", 36).nullable()
    // Equipment fields
    val equipmentType = text("equipment_type").nullable()  // "weapon", "armor", "accessory", or null
    val equipmentSlot = text("equipment_slot").nullable()  // "main_hand", "off_hand", "head", "chest", "legs", "feet", "ring", "amulet"
    val statBonuses = text("stat_bonuses").nullable()  // JSON: {"attack": 5, "defense": 3, "maxHp": 10}
    val value = integer("value").default(0)  // Gold value

    override val primaryKey = PrimaryKey(id)
}
