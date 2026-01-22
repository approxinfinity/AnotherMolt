package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

/**
 * Table for storing per-user/per-entity state for features.
 *
 * This separates static feature definitions (what a spell does) from
 * dynamic state (how many charges does this user have left).
 *
 * The composite key is {ownerId}-{featureId} where ownerId can be a userId,
 * creatureId, itemId, or any entity that can have feature state.
 */
object FeatureStateTable : Table("feature_state") {
    val id = varchar("id", 73)  // {36-char-uuid}-{36-char-uuid} = 73 chars
    val ownerId = varchar("owner_id", 36)  // userId, creatureId, etc.
    val ownerType = varchar("owner_type", 20)  // "user", "creature", "item"
    val featureId = varchar("feature_id", 36)
    val state = text("state")  // JSON blob for dynamic state
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
