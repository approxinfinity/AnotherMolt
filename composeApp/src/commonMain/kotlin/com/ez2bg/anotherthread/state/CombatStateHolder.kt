package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.api.CombatSessionDto
import com.ez2bg.anotherthread.api.CombatantDto
import com.ez2bg.anotherthread.api.StatusEffectDto
import com.ez2bg.anotherthread.combat.CombatClient
import com.ez2bg.anotherthread.combat.CombatConnectionState
import com.ez2bg.anotherthread.combat.CombatEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Event log entry for displaying combat/adventure events to the user.
 */
data class EventLogEntry(
    val id: Long,
    val message: String,
    val timestamp: Long = com.ez2bg.anotherthread.platform.currentTimeMillis(),
    val type: EventLogType = EventLogType.INFO
)

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
 * Singleton state holder for combat-related business logic.
 * Manages CombatClient lifecycle and exposes combat state as flows.
 *
 * This separates combat business logic from UI composables.
 */
object CombatStateHolder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Combat client instance
    private var combatClient: CombatClient? = null

    // Connection state
    private val _connectionState = MutableStateFlow(CombatConnectionState.DISCONNECTED)
    val connectionState: StateFlow<CombatConnectionState> = _connectionState.asStateFlow()

    // Combat session
    private val _combatSession = MutableStateFlow<CombatSessionDto?>(null)
    val combatSession: StateFlow<CombatSessionDto?> = _combatSession.asStateFlow()

    // Player's combatant data
    private val _playerCombatant = MutableStateFlow<CombatantDto?>(null)
    val playerCombatant: StateFlow<CombatantDto?> = _playerCombatant.asStateFlow()

    // All combatants in current session
    private val _combatants = MutableStateFlow<List<CombatantDto>>(emptyList())
    val combatants: StateFlow<List<CombatantDto>> = _combatants.asStateFlow()

    // Ability cooldowns (abilityId -> rounds remaining)
    private val _cooldowns = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cooldowns: StateFlow<Map<String, Int>> = _cooldowns.asStateFlow()

    // Currently queued ability
    private val _queuedAbilityId = MutableStateFlow<String?>(null)
    val queuedAbilityId: StateFlow<String?> = _queuedAbilityId.asStateFlow()

    // Blind status effect
    private val _isBlinded = MutableStateFlow(false)
    val isBlinded: StateFlow<Boolean> = _isBlinded.asStateFlow()

    private val _blindRounds = MutableStateFlow(0)
    val blindRounds: StateFlow<Int> = _blindRounds.asStateFlow()

    // Disorient status effect
    private val _isDisoriented = MutableStateFlow(false)
    val isDisoriented: StateFlow<Boolean> = _isDisoriented.asStateFlow()

    private val _disorientRounds = MutableStateFlow(0)
    val disorientRounds: StateFlow<Int> = _disorientRounds.asStateFlow()

    // Current round number
    private val _currentRound = MutableStateFlow(0)
    val currentRound: StateFlow<Int> = _currentRound.asStateFlow()

    // Event log for UI display
    private val _eventLog = MutableStateFlow<List<EventLogEntry>>(emptyList())
    val eventLog: StateFlow<List<EventLogEntry>> = _eventLog.asStateFlow()
    private var eventIdCounter = 0L

    // Combat events (for one-time event handling in UI)
    private val _combatEvents = MutableSharedFlow<CombatEvent>(extraBufferCapacity = 64)
    val combatEvents: SharedFlow<CombatEvent> = _combatEvents.asSharedFlow()

    // Track connected user
    private var connectedUserId: String? = null

    /**
     * Connect to combat WebSocket for a user.
     */
    fun connect(userId: String) {
        // Don't reconnect if already connected for this user
        if (connectedUserId == userId && combatClient != null) {
            return
        }

        // Disconnect any existing connection
        disconnect()

        connectedUserId = userId
        val client = CombatClient(userId)
        combatClient = client

        // Observe connection state
        scope.launch {
            client.connectionState.collect { state ->
                _connectionState.value = state
            }
        }

        // Observe combat events
        scope.launch {
            client.events.collect { event ->
                handleCombatEvent(event)
            }
        }

        client.connect()
    }

    /**
     * Disconnect from combat WebSocket.
     */
    fun disconnect() {
        combatClient?.disconnect()
        combatClient = null
        connectedUserId = null
        clearCombatState()
    }

    /**
     * Use an ability targeting an optional entity.
     */
    fun useAbility(abilityId: String, targetId: String? = null) {
        scope.launch {
            combatClient?.useAbility(abilityId, targetId)
        }
    }

    /**
     * Attempt to flee from combat.
     */
    fun flee() {
        scope.launch {
            combatClient?.flee()
        }
    }

    /**
     * Join combat at current location.
     */
    fun joinCombat(targetCreatureIds: List<String> = emptyList()) {
        scope.launch {
            combatClient?.joinCombat(targetCreatureIds)
        }
    }

    /**
     * Add an entry to the event log.
     */
    fun addEventLogEntry(message: String, type: EventLogType = EventLogType.INFO) {
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
    fun clearEventLog() {
        _eventLog.value = emptyList()
    }

    private fun handleCombatEvent(event: CombatEvent) {
        // Re-emit for UI listeners
        scope.launch {
            _combatEvents.emit(event)
        }

        when (event) {
            is CombatEvent.ConnectionStateChanged -> {
                _connectionState.value = event.state
            }

            is CombatEvent.CombatStarted -> {
                _combatSession.value = event.session
                _playerCombatant.value = event.yourCombatant
                _combatants.value = event.session.combatants
                _cooldowns.value = event.yourCombatant.cooldowns
                _currentRound.value = event.session.currentRound
                addEventLogEntry("Combat started!", EventLogType.COMBAT_START)
            }

            is CombatEvent.RoundStarted -> {
                _currentRound.value = event.roundNumber
                _combatants.value = event.combatants
                _queuedAbilityId.value = null // Clear queued ability for new round

                // Update player combatant from list
                val userId = connectedUserId
                if (userId != null) {
                    val myCombatant = event.combatants.find { it.id == userId }
                    if (myCombatant != null) {
                        _playerCombatant.value = myCombatant
                        _cooldowns.value = myCombatant.cooldowns
                    }
                }
            }

            is CombatEvent.AbilityQueued -> {
                _queuedAbilityId.value = event.abilityId
            }

            is CombatEvent.AbilityResolved -> {
                // Log ability usage
                val result = event.result
                val damageMsg = if (result.result.damage > 0) " for ${result.result.damage} damage" else ""
                val healMsg = if (result.result.healing > 0) " healing ${result.result.healing}" else ""
                addEventLogEntry(
                    "${result.actorName} used ${result.abilityName}$damageMsg$healMsg",
                    if (result.result.damage > 0) EventLogType.DAMAGE_DEALT else EventLogType.INFO
                )
            }

            is CombatEvent.StatusEffectChanged -> {
                val response = event.effect
                val effect: StatusEffectDto = response.effect
                val applied = response.applied
                when (effect.effectType.lowercase()) {
                    "blind" -> {
                        _isBlinded.value = applied
                        _blindRounds.value = effect.remainingRounds
                        if (applied) {
                            addEventLogEntry("You are blinded for ${effect.remainingRounds} rounds!", EventLogType.DEBUFF)
                        }
                    }
                    "disorient" -> {
                        _isDisoriented.value = applied
                        _disorientRounds.value = effect.remainingRounds
                        if (applied) {
                            addEventLogEntry("You are disoriented for ${effect.remainingRounds} rounds!", EventLogType.DEBUFF)
                        }
                    }
                }
            }

            is CombatEvent.RoundEnded -> {
                _combatants.value = event.combatants

                // Update player combatant and decrement status effect counters
                val userId = connectedUserId
                if (userId != null) {
                    val myCombatant = event.combatants.find { it.id == userId }
                    if (myCombatant != null) {
                        _playerCombatant.value = myCombatant
                        _cooldowns.value = myCombatant.cooldowns
                    }
                }

                // Decrement blind rounds
                if (_blindRounds.value > 0) {
                    _blindRounds.value = _blindRounds.value - 1
                    if (_blindRounds.value <= 0) {
                        _isBlinded.value = false
                        addEventLogEntry("Your vision clears.", EventLogType.INFO)
                    }
                }

                // Decrement disorient rounds
                if (_disorientRounds.value > 0) {
                    _disorientRounds.value = _disorientRounds.value - 1
                    if (_disorientRounds.value <= 0) {
                        _isDisoriented.value = false
                        addEventLogEntry("Your mind clears.", EventLogType.INFO)
                    }
                }
            }

            is CombatEvent.CombatEnded -> {
                addEventLogEntry("Combat ended!", EventLogType.COMBAT_END)
                clearCombatState()
            }

            is CombatEvent.FleeResult -> {
                if (event.response.success) {
                    addEventLogEntry("You fled from combat!", EventLogType.INFO)
                    clearCombatState()
                } else {
                    addEventLogEntry("Failed to flee: ${event.response.message}", EventLogType.ERROR)
                }
            }

            is CombatEvent.HealthUpdated -> {
                val update = event.update
                val type = if (update.changeAmount < 0) EventLogType.DAMAGE_RECEIVED else EventLogType.HEAL
                addEventLogEntry(
                    "${update.combatantId}: ${update.changeAmount} HP (${update.currentHp}/${update.maxHp})",
                    type
                )
            }

            is CombatEvent.PlayerDied -> {
                addEventLogEntry("You have died!", EventLogType.ERROR)
                clearCombatState()
            }

            is CombatEvent.CreatureMoved -> {
                // Only show movement if creature enters or leaves the player's current room
                val currentLocationId = AdventureStateHolder.currentLocation.value?.id
                if (currentLocationId != null) {
                    when (currentLocationId) {
                        event.fromLocationId -> {
                            addEventLogEntry("${event.creatureName} left.", EventLogType.INFO)
                        }
                        event.toLocationId -> {
                            addEventLogEntry("${event.creatureName} arrived.", EventLogType.INFO)
                        }
                    }
                }
            }

            is CombatEvent.Error -> {
                addEventLogEntry("Error: ${event.message}", EventLogType.ERROR)
            }

            is CombatEvent.ResourceUpdated -> {
                // Could log resource changes if needed
            }
        }
    }

    private fun clearCombatState() {
        _combatSession.value = null
        _playerCombatant.value = null
        _combatants.value = emptyList()
        _cooldowns.value = emptyMap()
        _queuedAbilityId.value = null
        _isBlinded.value = false
        _blindRounds.value = 0
        _isDisoriented.value = false
        _disorientRounds.value = 0
        _currentRound.value = 0
    }

    /**
     * Check if currently in combat.
     */
    val isInCombat: Boolean
        get() = _combatSession.value != null
}
