package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

/**
 * Table for storing game configuration values.
 *
 * This provides a key-value store for game settings that can be
 * modified at runtime through the admin interface.
 *
 * Keys are organized by category (e.g., "combat.roundDurationMs", "respawn.delayTicks")
 */
object GameConfigTable : Table("game_config") {
    val key = varchar("key", 100)
    val value = text("value")  // Stored as string, parsed to appropriate type
    val description = text("description").nullable()
    val category = varchar("category", 50)  // "combat", "respawn", "death", "xp", etc.
    val valueType = varchar("value_type", 20)  // "int", "long", "double", "boolean", "string"
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(key)
}
