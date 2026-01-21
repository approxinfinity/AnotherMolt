package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object CharacterClassTable : Table("character_class") {
    val id = varchar("id", 36)
    val name = text("name").uniqueIndex()
    val description = text("description")
    val isSpellcaster = bool("is_spellcaster")
    val hitDie = integer("hit_die")
    val primaryAttribute = text("primary_attribute")
    val imageUrl = text("image_url").nullable()

    // Power budget for balance - total points available for abilities
    val powerBudget = integer("power_budget").default(100)
    // Whether this class is available for all users or user-generated
    val isPublic = bool("is_public").default(true)
    // Creator user ID (null for seeded/admin classes)
    val createdByUserId = varchar("created_by_user_id", 36).nullable()

    override val primaryKey = PrimaryKey(id)
}
