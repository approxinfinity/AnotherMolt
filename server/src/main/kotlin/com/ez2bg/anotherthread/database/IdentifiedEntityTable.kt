package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

/**
 * Tracks which items/creatures have been identified by each user.
 * Once an entity is identified, its hidden stats become permanently visible to that user.
 */
object IdentifiedEntityTable : Table("identified_entity") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val entityId = varchar("entity_id", 36)      // Item ID or Creature ID
    val entityType = varchar("entity_type", 20)  // "item" or "creature"
    val identifiedAt = varchar("identified_at", 30)

    override val primaryKey = PrimaryKey(id)

    init {
        // Index for efficient lookups
        index(true, userId, entityId, entityType)  // Unique constraint
    }
}
