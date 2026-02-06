package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.platform.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Event log entry for displaying game events to the user.
 */
data class EventLogEntry(
    val id: Long,
    val message: String,
    val timestamp: Long = currentTimeMillis(),
    val type: EventLogType = EventLogType.INFO
)

/**
 * Types of events that can appear in the game log.
 */
enum class EventLogType {
    INFO,
    DAMAGE_DEALT,
    DAMAGE_RECEIVED,
    HEAL,
    BUFF,
    DEBUFF,
    COMBAT_START,
    COMBAT_END,
    NAVIGATION,
    LOOT,
    ERROR
}

/**
 * Singleton state holder for the game event log.
 * This is a global log for all game events (combat, navigation, loot, etc.)
 *
 * Separated from CombatStateHolder since the event log is used across
 * multiple game systems, not just combat.
 */
object GameEventLogHolder {
    // Event log for UI display
    private val _eventLog = MutableStateFlow<List<EventLogEntry>>(emptyList())
    val eventLog: StateFlow<List<EventLogEntry>> = _eventLog.asStateFlow()
    private var eventIdCounter = 0L

    /**
     * Add an entry to the event log.
     */
    fun addEntry(message: String, type: EventLogType = EventLogType.INFO) {
        val entry = EventLogEntry(
            id = ++eventIdCounter,
            message = message,
            type = type
        )
        // Keep last 100 entries
        _eventLog.value = (_eventLog.value + entry).takeLast(100)
    }

    /**
     * Clear the event log.
     */
    fun clear() {
        _eventLog.value = emptyList()
    }
}
