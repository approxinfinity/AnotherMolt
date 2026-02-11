package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object ChestTable : Table("chest") {
    val id = text("id")  // Human-readable IDs for adventure modules
    val name = text("name")
    val desc = text("desc")
    val locationId = text("location_id")  // Human-readable location IDs can exceed 36 chars
    val guardianCreatureId = text("guardian_creature_id").nullable()  // Human-readable creature IDs
    val isLocked = bool("is_locked").default(true)
    val lockDifficulty = integer("lock_difficulty").default(1)  // 1-5 scale for pick lock
    val bashDifficulty = integer("bash_difficulty").default(1)  // 1-5 scale for bash
    val lootTableId = text("loot_table_id").nullable()  // Human-readable loot table IDs
    val goldAmount = integer("gold_amount").default(0)
    val imageUrl = text("image_url").nullable()

    override val primaryKey = PrimaryKey(id)
}
