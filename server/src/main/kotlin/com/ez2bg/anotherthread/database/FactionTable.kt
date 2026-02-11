package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

/**
 * Database table for factions (groups of creatures with shared allegiances).
 * Factions define relationships between creature groups (e.g., kobolds vs goblins).
 */
object FactionTable : Table("faction") {
    val id = text("id")
    val name = text("name")
    val description = text("description")

    // Home territory
    val homeLocationId = text("home_location_id").nullable()  // Primary lair/base

    // Faction traits
    val hostilityLevel = integer("hostility_level").default(50)  // 0=peaceful, 50=neutral, 100=hostile
    val canNegotiate = bool("can_negotiate").default(true)  // Can players attempt diplomacy?
    val leaderCreatureId = text("leader_creature_id").nullable()  // Leader of this faction

    // Extra data as JSON
    val data = text("data").default("{}")  // Additional faction data (territory, goals, etc.)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Database table for faction relationships (how factions feel about each other).
 * This is a many-to-many relationship table.
 */
object FactionRelationTable : Table("faction_relation") {
    val id = text("id")
    val factionId = text("faction_id")  // The faction whose relationship we're tracking
    val targetFactionId = text("target_faction_id")  // The faction they have an opinion about
    val relationshipLevel = integer("relationship_level").default(0)  // -100=enemies, 0=neutral, 100=allies

    override val primaryKey = PrimaryKey(id)
}

/**
 * Database table for tracking player reputation with factions.
 */
object PlayerFactionStandingTable : Table("player_faction_standing") {
    val id = text("id")
    val userId = text("user_id")
    val factionId = text("faction_id")
    val standing = integer("standing").default(0)  // -100=hated, 0=neutral, 100=revered
    val killCount = integer("kill_count").default(0)  // How many faction members player has killed
    val questsCompleted = integer("quests_completed").default(0)  // Faction quests completed

    override val primaryKey = PrimaryKey(id)
}
