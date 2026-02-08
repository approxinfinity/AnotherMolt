package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object UserTable : Table("user") {
    val id = varchar("id", 36)
    val name = text("name").uniqueIndex()
    val passwordHash = text("password_hash")
    val desc = text("desc").default("")
    val itemIds = text("item_ids").default("[]")
    val featureIds = text("feature_ids").default("[]")
    val imageUrl = text("image_url").nullable()
    val currentLocationId = varchar("current_location_id", 36).nullable()
    val characterClassId = varchar("character_class_id", 36).nullable()
    val createdAt = long("created_at")
    val lastActiveAt = long("last_active_at")
    // Combat stats
    val level = integer("level").default(1)
    val experience = integer("experience").default(0)
    val maxHp = integer("max_hp").default(10)
    val currentHp = integer("current_hp").default(10)
    val maxMana = integer("max_mana").default(10)
    val currentMana = integer("current_mana").default(10)
    val maxStamina = integer("max_stamina").default(10)
    val currentStamina = integer("current_stamina").default(10)
    val currentCombatSessionId = varchar("current_combat_session_id", 36).nullable()
    // D&D Attributes
    val strength = integer("strength").default(10)
    val dexterity = integer("dexterity").default(10)
    val constitution = integer("constitution").default(10)
    val intelligence = integer("intelligence").default(10)
    val wisdom = integer("wisdom").default(10)
    val charisma = integer("charisma").default(10)
    val attributeQualityBonus = integer("attribute_quality_bonus").default(0)
    val attributesGeneratedAt = long("attributes_generated_at").nullable()
    // Economy and equipment
    val gold = integer("gold").default(0)
    val equippedItemIds = text("equipped_item_ids").default("[]")  // JSON array
    // Trainer system: abilities the user has learned from trainers
    val learnedAbilityIds = text("learned_ability_ids").default("[]")  // JSON array
    // Action bar customization: which abilities to show (max 10, empty = show all)
    val visibleAbilityIds = text("visible_ability_ids").default("[]")  // JSON array
    // Stealth status
    val isHidden = bool("is_hidden").default(false)      // Currently hiding in place
    val isSneaking = bool("is_sneaking").default(false)  // Moving stealthily

    override val primaryKey = PrimaryKey(id)
}
