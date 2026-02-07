package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Types of combat events for audit logging.
 */
enum class CombatEventType {
    // Session lifecycle
    SESSION_CREATED,
    SESSION_ENDED,
    PLAYER_JOINED,
    PLAYER_LEFT,
    PLAYER_FLED,

    // Combat actions
    ROUND_START,
    ROUND_END,
    PLAYER_ATTACK,
    CREATURE_ATTACK,
    ABILITY_USED,
    ABILITY_RESOLVED,

    // Damage/healing
    DAMAGE_DEALT,
    HEALING_RECEIVED,
    STATUS_EFFECT_APPLIED,
    STATUS_EFFECT_REMOVED,

    // State changes
    PLAYER_DOWNED,
    PLAYER_STABILIZED,
    PLAYER_DIED,
    CREATURE_DIED,
    CREATURE_REMOVED,  // Wandered away from combat location

    // Loot/rewards
    LOOT_DROPPED,
    XP_AWARDED
}

/**
 * Snapshot of a combatant's stats at the time of an event.
 */
@Serializable
data class CombatantSnapshot(
    val id: String,
    val name: String,
    val type: String,  // "player" or "creature"
    val level: Int,
    val currentHp: Int,
    val maxHp: Int,
    val currentMana: Int? = null,
    val maxMana: Int? = null,
    val currentStamina: Int? = null,
    val maxStamina: Int? = null,
    val accuracy: Int,
    val evasion: Int,
    val baseDamage: Int,
    val critBonus: Int,
    val isAlive: Boolean,
    val isDowned: Boolean = false,
    val statusEffects: List<String> = emptyList(),
    val locationId: String? = null
)

/**
 * A combat event log entry with full context.
 */
@Serializable
data class CombatEventLog(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val locationId: String,
    val locationName: String,
    val eventType: CombatEventType,
    val roundNumber: Int,
    val message: String,
    val actorSnapshot: CombatantSnapshot? = null,
    val targetSnapshot: CombatantSnapshot? = null,
    val allCombatantsJson: String? = null,  // JSON array of all combatant snapshots
    val eventData: String? = null,  // Additional event-specific JSON data
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Database table for combat event logs.
 */
object CombatEventLogTable : Table("combat_event_log") {
    val id = varchar("id", 36)
    val sessionId = varchar("session_id", 36)
    val locationId = varchar("location_id", 36)
    val locationName = varchar("location_name", 255)
    val eventType = varchar("event_type", 50)
    val roundNumber = integer("round_number")
    val message = text("message")
    val actorSnapshotJson = text("actor_snapshot_json").nullable()
    val targetSnapshotJson = text("target_snapshot_json").nullable()
    val allCombatantsJson = text("all_combatants_json").nullable()
    val eventData = text("event_data").nullable()
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, sessionId)
        index(false, locationId)
        index(false, eventType)
        index(false, timestamp)
    }
}

object CombatEventLogRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun ResultRow.toCombatEventLog(): CombatEventLog = CombatEventLog(
        id = this[CombatEventLogTable.id],
        sessionId = this[CombatEventLogTable.sessionId],
        locationId = this[CombatEventLogTable.locationId],
        locationName = this[CombatEventLogTable.locationName],
        eventType = CombatEventType.valueOf(this[CombatEventLogTable.eventType]),
        roundNumber = this[CombatEventLogTable.roundNumber],
        message = this[CombatEventLogTable.message],
        actorSnapshot = this[CombatEventLogTable.actorSnapshotJson]?.let {
            json.decodeFromString<CombatantSnapshot>(it)
        },
        targetSnapshot = this[CombatEventLogTable.targetSnapshotJson]?.let {
            json.decodeFromString<CombatantSnapshot>(it)
        },
        allCombatantsJson = this[CombatEventLogTable.allCombatantsJson],
        eventData = this[CombatEventLogTable.eventData],
        timestamp = this[CombatEventLogTable.timestamp]
    )

    fun create(log: CombatEventLog): CombatEventLog = transaction {
        CombatEventLogTable.insert {
            it[id] = log.id
            it[sessionId] = log.sessionId
            it[locationId] = log.locationId
            it[locationName] = log.locationName
            it[eventType] = log.eventType.name
            it[roundNumber] = log.roundNumber
            it[message] = log.message
            it[actorSnapshotJson] = log.actorSnapshot?.let { snap -> json.encodeToString(snap) }
            it[targetSnapshotJson] = log.targetSnapshot?.let { snap -> json.encodeToString(snap) }
            it[allCombatantsJson] = log.allCombatantsJson
            it[eventData] = log.eventData
            it[timestamp] = log.timestamp
        }
        log
    }

    /**
     * Log a combat event with full context.
     */
    fun log(
        sessionId: String,
        locationId: String,
        locationName: String,
        eventType: CombatEventType,
        roundNumber: Int,
        message: String,
        actorSnapshot: CombatantSnapshot? = null,
        targetSnapshot: CombatantSnapshot? = null,
        allCombatants: List<CombatantSnapshot>? = null,
        eventData: Map<String, Any>? = null
    ): CombatEventLog {
        return create(
            CombatEventLog(
                sessionId = sessionId,
                locationId = locationId,
                locationName = locationName,
                eventType = eventType,
                roundNumber = roundNumber,
                message = message,
                actorSnapshot = actorSnapshot,
                targetSnapshot = targetSnapshot,
                allCombatantsJson = allCombatants?.let { json.encodeToString(it) },
                eventData = eventData?.let { json.encodeToString(it) }
            )
        )
    }

    fun findBySessionId(sessionId: String): List<CombatEventLog> = transaction {
        CombatEventLogTable.selectAll()
            .where { CombatEventLogTable.sessionId eq sessionId }
            .orderBy(CombatEventLogTable.timestamp, SortOrder.ASC)
            .map { it.toCombatEventLog() }
    }

    fun findByLocationId(locationId: String, limit: Int = 100): List<CombatEventLog> = transaction {
        CombatEventLogTable.selectAll()
            .where { CombatEventLogTable.locationId eq locationId }
            .orderBy(CombatEventLogTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { it.toCombatEventLog() }
    }

    fun findByEventType(eventType: CombatEventType, limit: Int = 100): List<CombatEventLog> = transaction {
        CombatEventLogTable.selectAll()
            .where { CombatEventLogTable.eventType eq eventType.name }
            .orderBy(CombatEventLogTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { it.toCombatEventLog() }
    }

    fun findRecent(limit: Int = 100, offset: Long = 0): List<CombatEventLog> = transaction {
        CombatEventLogTable.selectAll()
            .orderBy(CombatEventLogTable.timestamp, SortOrder.DESC)
            .limit(limit, offset)
            .map { it.toCombatEventLog() }
    }

    /**
     * Find events for a specific player (actor or target).
     */
    fun findByPlayerId(playerId: String, limit: Int = 100): List<CombatEventLog> = transaction {
        CombatEventLogTable.selectAll()
            .where {
                (CombatEventLogTable.actorSnapshotJson like "%\"id\":\"$playerId\"%") or
                (CombatEventLogTable.targetSnapshotJson like "%\"id\":\"$playerId\"%")
            }
            .orderBy(CombatEventLogTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { it.toCombatEventLog() }
    }

    /**
     * Delete old logs (for cleanup).
     */
    fun deleteOlderThan(timestampMs: Long): Int = transaction {
        CombatEventLogTable.deleteWhere {
            CombatEventLogTable.timestamp lessEq timestampMs
        }
    }
}
