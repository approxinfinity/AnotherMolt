package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

@Serializable
data class IdentifiedEntity(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val entityId: String,
    val entityType: String,  // "item" or "creature"
    val identifiedAt: String = Instant.now().toString()
)

object IdentifiedEntityRepository {
    private fun ResultRow.toIdentifiedEntity(): IdentifiedEntity = IdentifiedEntity(
        id = this[IdentifiedEntityTable.id],
        userId = this[IdentifiedEntityTable.userId],
        entityId = this[IdentifiedEntityTable.entityId],
        entityType = this[IdentifiedEntityTable.entityType],
        identifiedAt = this[IdentifiedEntityTable.identifiedAt]
    )

    /**
     * Mark an entity as identified for a user.
     * Returns true if newly identified, false if already identified.
     */
    fun identify(userId: String, entityId: String, entityType: String): Boolean = transaction {
        // Check if already identified
        val existing = IdentifiedEntityTable.selectAll()
            .where {
                (IdentifiedEntityTable.userId eq userId) and
                (IdentifiedEntityTable.entityId eq entityId) and
                (IdentifiedEntityTable.entityType eq entityType)
            }
            .singleOrNull()

        if (existing != null) {
            return@transaction false
        }

        // Create new identification record
        val entity = IdentifiedEntity(
            userId = userId,
            entityId = entityId,
            entityType = entityType
        )

        IdentifiedEntityTable.insert {
            it[id] = entity.id
            it[IdentifiedEntityTable.userId] = entity.userId
            it[IdentifiedEntityTable.entityId] = entity.entityId
            it[IdentifiedEntityTable.entityType] = entity.entityType
            it[identifiedAt] = entity.identifiedAt
        }

        true
    }

    /**
     * Check if an entity has been identified by a user.
     */
    fun isIdentified(userId: String, entityId: String, entityType: String): Boolean = transaction {
        IdentifiedEntityTable.selectAll()
            .where {
                (IdentifiedEntityTable.userId eq userId) and
                (IdentifiedEntityTable.entityId eq entityId) and
                (IdentifiedEntityTable.entityType eq entityType)
            }
            .count() > 0
    }

    /**
     * Get all identified entities for a user.
     */
    fun findByUser(userId: String): List<IdentifiedEntity> = transaction {
        IdentifiedEntityTable.selectAll()
            .where { IdentifiedEntityTable.userId eq userId }
            .map { it.toIdentifiedEntity() }
    }

    /**
     * Get all identified item IDs for a user.
     */
    fun getIdentifiedItemIds(userId: String): Set<String> = transaction {
        IdentifiedEntityTable.selectAll()
            .where {
                (IdentifiedEntityTable.userId eq userId) and
                (IdentifiedEntityTable.entityType eq "item")
            }
            .map { it[IdentifiedEntityTable.entityId] }
            .toSet()
    }

    /**
     * Get all identified creature IDs for a user.
     */
    fun getIdentifiedCreatureIds(userId: String): Set<String> = transaction {
        IdentifiedEntityTable.selectAll()
            .where {
                (IdentifiedEntityTable.userId eq userId) and
                (IdentifiedEntityTable.entityType eq "creature")
            }
            .map { it[IdentifiedEntityTable.entityId] }
            .toSet()
    }

    /**
     * Delete all identification records for a user.
     */
    fun deleteByUser(userId: String): Int = transaction {
        IdentifiedEntityTable.deleteWhere { IdentifiedEntityTable.userId eq userId }
    }
}
