package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.combat.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Repository for persisting combat sessions.
 * Used primarily for reconnection support and debugging.
 */
object CombatSessionRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun ResultRow.toCombatSession(): CombatSession = CombatSession(
        id = this[CombatSessionTable.id],
        locationId = this[CombatSessionTable.locationId],
        state = CombatState.valueOf(this[CombatSessionTable.state]),
        currentRound = this[CombatSessionTable.currentRound],
        roundStartTime = this[CombatSessionTable.roundStartTime],
        combatants = json.decodeFromString(this[CombatSessionTable.combatantsJson]),
        pendingActions = json.decodeFromString(this[CombatSessionTable.pendingActionsJson]),
        combatLog = json.decodeFromString(this[CombatSessionTable.combatLogJson]),
        endReason = this[CombatSessionTable.endReason]?.let { CombatEndReason.valueOf(it) },
        createdAt = this[CombatSessionTable.createdAt]
    )

    fun create(session: CombatSession): CombatSession = transaction {
        CombatSessionTable.insert {
            it[id] = session.id
            it[locationId] = session.locationId
            it[state] = session.state.name
            it[currentRound] = session.currentRound
            it[roundStartTime] = session.roundStartTime
            it[combatantsJson] = json.encodeToString(session.combatants)
            it[pendingActionsJson] = json.encodeToString(session.pendingActions)
            it[combatLogJson] = json.encodeToString(session.combatLog)
            it[endReason] = session.endReason?.name
            it[createdAt] = session.createdAt
            it[updatedAt] = System.currentTimeMillis()
        }
        session
    }

    fun findById(id: String): CombatSession? = transaction {
        CombatSessionTable.selectAll()
            .where { CombatSessionTable.id eq id }
            .map { it.toCombatSession() }
            .singleOrNull()
    }

    fun findByLocationId(locationId: String): CombatSession? = transaction {
        CombatSessionTable.selectAll()
            .where { CombatSessionTable.locationId eq locationId }
            .map { it.toCombatSession() }
            .filter { it.state != CombatState.ENDED }
            .firstOrNull()
    }

    fun findActive(): List<CombatSession> = transaction {
        CombatSessionTable.selectAll()
            .map { it.toCombatSession() }
            .filter { it.state == CombatState.ACTIVE || it.state == CombatState.WAITING }
    }

    fun update(session: CombatSession): Boolean = transaction {
        CombatSessionTable.update({ CombatSessionTable.id eq session.id }) {
            it[state] = session.state.name
            it[currentRound] = session.currentRound
            it[roundStartTime] = session.roundStartTime
            it[combatantsJson] = json.encodeToString(session.combatants)
            it[pendingActionsJson] = json.encodeToString(session.pendingActions)
            it[combatLogJson] = json.encodeToString(session.combatLog)
            it[endReason] = session.endReason?.name
            it[updatedAt] = System.currentTimeMillis()
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        CombatSessionTable.deleteWhere { CombatSessionTable.id eq id } > 0
    }

    /**
     * Clean up old ended sessions (older than 1 hour)
     */
    fun cleanupOldSessions(): Int = transaction {
        val cutoff = System.currentTimeMillis() - 3600000 // 1 hour
        CombatSessionTable.selectAll()
            .map { it.toCombatSession() }
            .filter { it.state == CombatState.ENDED && it.createdAt < cutoff }
            .count { delete(it.id) }
    }
}
