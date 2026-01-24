package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object CharacterClassTable : Table("character_class") {
    val id = varchar("id", 36)
    val name = text("name").uniqueIndex()
    val description = text("description")
    val isSpellcaster = bool("is_spellcaster")
    val hitDie = integer("hit_die")
    val primaryAttribute = text("primary_attribute")
    val baseMana = integer("base_mana").default(10)      // Base mana pool (spellcasters get more)
    val baseStamina = integer("base_stamina").default(10) // Base stamina pool (physical classes get more)
    val imageUrl = text("image_url").nullable()

    // Power budget for balance - total points available for abilities
    val powerBudget = integer("power_budget").default(100)
    // Whether this class is available for all users or user-generated
    val isPublic = bool("is_public").default(true)
    // Creator user ID (null for seeded/admin classes)
    val createdByUserId = varchar("created_by_user_id", 36).nullable()
    // Whether this class is locked (cannot be edited/deleted except by admin)
    val isLocked = bool("is_locked").default(true)

    override val primaryKey = PrimaryKey(id)
}
