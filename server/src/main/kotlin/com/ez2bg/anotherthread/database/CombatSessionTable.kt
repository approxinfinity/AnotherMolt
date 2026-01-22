package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

/**
 * Persists active combat sessions for reconnection support.
 * Combat state is primarily kept in memory, but we persist enough
 * to allow players to reconnect to ongoing battles.
 */
object CombatSessionTable : Table("combat_session") {
    val id = varchar("id", 36)
    val locationId = varchar("location_id", 36)
    val state = varchar("state", 20)  // WAITING, ACTIVE, ENDED
    val currentRound = integer("current_round")
    val roundStartTime = long("round_start_time")
    val combatantsJson = text("combatants_json")  // JSON array of Combatant objects
    val pendingActionsJson = text("pending_actions_json")  // JSON array of CombatAction objects
    val combatLogJson = text("combat_log_json")  // JSON array of CombatLogEntry objects
    val endReason = varchar("end_reason", 30).nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
