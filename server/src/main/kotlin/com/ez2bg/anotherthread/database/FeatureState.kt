package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Represents the dynamic state of a feature for a specific owner (user, creature, etc.).
 *
 * The state JSON can contain different data depending on the feature type:
 * - Spells: remainingCharges, cooldownExpiresAt, timesUsed
 * - Buffs: expiresAt, stacks
 * - Abilities: lastUsedAt, cooldownRounds
 */
@Serializable
data class FeatureState(
    val id: String,  // Composite: {ownerId}-{featureId}
    val ownerId: String,
    val ownerType: String,  // "user", "creature", "item"
    val featureId: String,
    val state: String = "{}",  // JSON blob
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun createId(ownerId: String, featureId: String): String = "$ownerId-$featureId"
    }
}

/**
 * Common state structures that can be stored in the state JSON.
 */
@Serializable
data class SpellState(
    val remainingCharges: Int? = null,      // For uses_per_day spells
    val cooldownExpiresAt: Long? = null,    // Unix timestamp when cooldown ends
    val lastUsedAt: Long? = null,           // When spell was last cast
    val timesUsed: Int = 0                  // Total times used (for stats)
)

@Serializable
data class BuffState(
    val expiresAt: Long? = null,            // When buff expires
    val stacks: Int = 1,                    // Stack count for stackable buffs
    val appliedAt: Long = System.currentTimeMillis()
)

@Serializable
data class AbilityCooldownState(
    val cooldownExpiresRound: Int? = null,  // Combat round when cooldown ends
    val lastUsedRound: Int? = null          // Combat round when last used
)

object FeatureStateRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ResultRow.toFeatureState(): FeatureState = FeatureState(
        id = this[FeatureStateTable.id],
        ownerId = this[FeatureStateTable.ownerId],
        ownerType = this[FeatureStateTable.ownerType],
        featureId = this[FeatureStateTable.featureId],
        state = this[FeatureStateTable.state],
        createdAt = this[FeatureStateTable.createdAt],
        updatedAt = this[FeatureStateTable.updatedAt]
    )

    fun create(featureState: FeatureState): FeatureState = transaction {
        FeatureStateTable.insert {
            it[id] = featureState.id
            it[ownerId] = featureState.ownerId
            it[ownerType] = featureState.ownerType
            it[featureId] = featureState.featureId
            it[state] = featureState.state
            it[createdAt] = featureState.createdAt
            it[updatedAt] = featureState.updatedAt
        }
        featureState
    }

    fun findById(id: String): FeatureState? = transaction {
        FeatureStateTable.selectAll()
            .where { FeatureStateTable.id eq id }
            .map { it.toFeatureState() }
            .singleOrNull()
    }

    fun findByOwnerAndFeature(ownerId: String, featureId: String): FeatureState? {
        val id = FeatureState.createId(ownerId, featureId)
        return findById(id)
    }

    fun findAllByOwner(ownerId: String): List<FeatureState> = transaction {
        FeatureStateTable.selectAll()
            .where { FeatureStateTable.ownerId eq ownerId }
            .map { it.toFeatureState() }
    }

    fun findAllByOwnerAndType(ownerId: String, ownerType: String): List<FeatureState> = transaction {
        FeatureStateTable.selectAll()
            .where {
                (FeatureStateTable.ownerId eq ownerId) and
                (FeatureStateTable.ownerType eq ownerType)
            }
            .map { it.toFeatureState() }
    }

    fun update(featureState: FeatureState): Boolean = transaction {
        FeatureStateTable.update({ FeatureStateTable.id eq featureState.id }) {
            it[state] = featureState.state
            it[updatedAt] = System.currentTimeMillis()
        } > 0
    }

    fun updateState(id: String, newState: String): Boolean = transaction {
        FeatureStateTable.update({ FeatureStateTable.id eq id }) {
            it[state] = newState
            it[updatedAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Get or create a FeatureState for an owner/feature combination.
     * If it doesn't exist, creates one with default empty state.
     */
    fun getOrCreate(ownerId: String, ownerType: String, featureId: String): FeatureState {
        val existing = findByOwnerAndFeature(ownerId, featureId)
        if (existing != null) return existing

        val newState = FeatureState(
            id = FeatureState.createId(ownerId, featureId),
            ownerId = ownerId,
            ownerType = ownerType,
            featureId = featureId,
            state = "{}"
        )
        return create(newState)
    }

    fun delete(id: String): Boolean = transaction {
        FeatureStateTable.deleteWhere { FeatureStateTable.id eq id } > 0
    }

    fun deleteAllByOwner(ownerId: String): Int = transaction {
        FeatureStateTable.deleteWhere { FeatureStateTable.ownerId eq ownerId }
    }

    // ========================================================================
    // Typed state helpers
    // ========================================================================

    fun getSpellState(ownerId: String, featureId: String): SpellState? {
        val featureState = findByOwnerAndFeature(ownerId, featureId) ?: return null
        return try {
            json.decodeFromString<SpellState>(featureState.state)
        } catch (e: Exception) {
            null
        }
    }

    fun updateSpellState(ownerId: String, ownerType: String, featureId: String, spellState: SpellState): Boolean {
        val id = FeatureState.createId(ownerId, featureId)
        val existing = findById(id)

        return if (existing != null) {
            updateState(id, json.encodeToString(spellState))
        } else {
            create(FeatureState(
                id = id,
                ownerId = ownerId,
                ownerType = ownerType,
                featureId = featureId,
                state = json.encodeToString(spellState)
            ))
            true
        }
    }

    fun getBuffState(ownerId: String, featureId: String): BuffState? {
        val featureState = findByOwnerAndFeature(ownerId, featureId) ?: return null
        return try {
            json.decodeFromString<BuffState>(featureState.state)
        } catch (e: Exception) {
            null
        }
    }

    fun updateBuffState(ownerId: String, ownerType: String, featureId: String, buffState: BuffState): Boolean {
        val id = FeatureState.createId(ownerId, featureId)
        val existing = findById(id)

        return if (existing != null) {
            updateState(id, json.encodeToString(buffState))
        } else {
            create(FeatureState(
                id = id,
                ownerId = ownerId,
                ownerType = ownerType,
                featureId = featureId,
                state = json.encodeToString(buffState)
            ))
            true
        }
    }

    // ========================================================================
    // Simple key-value helpers for game state tracking
    // ========================================================================

    /**
     * Simple wrapper for storing a single string value.
     */
    @Serializable
    data class SimpleState(val value: String)

    /**
     * Get a simple state value by key.
     * Used for flags like "defeated_<creatureId>" or "opened_chest_<chestId>".
     */
    fun getState(ownerId: String, key: String): SimpleState? {
        val featureState = findByOwnerAndFeature(ownerId, key) ?: return null
        return try {
            json.decodeFromString<SimpleState>(featureState.state)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Set a simple state value by key.
     * Creates the state if it doesn't exist, updates if it does.
     */
    fun setState(ownerId: String, key: String, value: String): Boolean {
        val id = FeatureState.createId(ownerId, key)
        val stateJson = json.encodeToString(SimpleState(value))
        val existing = findById(id)

        return if (existing != null) {
            updateState(id, stateJson)
        } else {
            create(FeatureState(
                id = id,
                ownerId = ownerId,
                ownerType = "user",  // Default to user for simple state
                featureId = key,
                state = stateJson
            ))
            true
        }
    }
}
