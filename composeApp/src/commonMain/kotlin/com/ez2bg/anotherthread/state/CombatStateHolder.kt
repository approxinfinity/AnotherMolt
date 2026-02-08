package com.ez2bg.anotherthread.state

import com.ez2bg.anotherthread.api.CombatSessionDto
import com.ez2bg.anotherthread.api.CombatantDto
import com.ez2bg.anotherthread.api.StatusEffectDto
import com.ez2bg.anotherthread.api.LocationEventType
import com.ez2bg.anotherthread.combat.CombatClient
import com.ez2bg.anotherthread.combat.CombatConnectionState
import com.ez2bg.anotherthread.combat.GlobalEvent
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

    // Event log - delegates to global GameEventLogHolder
    val eventLog: StateFlow<List<EventLogEntry>> = GameEventLogHolder.eventLog

    // Combat events (for one-time event handling in UI)
    private val _globalEvents = MutableSharedFlow<GlobalEvent>(extraBufferCapacity = 64)
    val globalEvents: SharedFlow<GlobalEvent> = _globalEvents.asSharedFlow()

    // Death animation state - triggers when player dies, clears after animation completes
    private val _isPlayingDeathAnimation = MutableStateFlow(false)
    val isPlayingDeathAnimation: StateFlow<Boolean> = _isPlayingDeathAnimation.asStateFlow()

    private val _respawnLocationName = MutableStateFlow<String?>(null)
    val respawnLocationName: StateFlow<String?> = _respawnLocationName.asStateFlow()

    // Track connected user
    private var connectedUserId: String? = null

    // Pending ability to use when combat starts (for join + attack combo)
    private data class PendingAbility(val abilityId: String, val targetId: String?)
    private var pendingFirstAbility: PendingAbility? = null

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
                handleGlobalEvent(event)
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
     * If an ability is provided, it will be queued and sent after combat starts.
     */
    fun joinCombat(targetCreatureIds: List<String> = emptyList(), withAbility: String? = null, targetId: String? = null) {
        // Store pending ability to use when combat starts
        if (withAbility != null) {
            pendingFirstAbility = PendingAbility(withAbility, targetId)
        }
        scope.launch {
            combatClient?.joinCombat(targetCreatureIds)
        }
    }

    /**
     * Add an entry to the event log.
     * Delegates to global GameEventLogHolder.
     */
    fun addEventLogEntry(message: String, type: EventLogType = EventLogType.INFO) {
        GameEventLogHolder.addEntry(message, type)
    }

    /**
     * Clear the event log.
     * Delegates to global GameEventLogHolder.
     */
    fun clearEventLog() {
        GameEventLogHolder.clear()
    }

    private fun handleGlobalEvent(event: GlobalEvent) {
        // Re-emit for UI listeners
        scope.launch {
            _globalEvents.emit(event)
        }

        when (event) {
            is GlobalEvent.ConnectionStateChanged -> {
                _connectionState.value = event.state
            }

            is GlobalEvent.CombatStarted -> {
                _combatSession.value = event.session
                _playerCombatant.value = event.yourCombatant
                _combatants.value = event.session.combatants
                _cooldowns.value = event.yourCombatant.cooldowns
                _currentRound.value = event.session.currentRound
                // Show engagement messages BEFORE "Combat started!" so player knows who attacked
                event.engagementMessages.forEach { message ->
                    addEventLogEntry(message, EventLogType.DAMAGE_RECEIVED)
                }
                addEventLogEntry("Combat started!", EventLogType.COMBAT_START)

                // If we have a pending ability from joining combat, use it now
                pendingFirstAbility?.let { pending ->
                    useAbility(pending.abilityId, pending.targetId)
                    pendingFirstAbility = null
                }
            }

            is GlobalEvent.RoundStarted -> {
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

            is GlobalEvent.AbilityQueued -> {
                _queuedAbilityId.value = event.abilityId
            }

            is GlobalEvent.AbilityResolved -> {
                // Log ability usage
                val result = event.result
                val damageMsg = if (result.result.damage > 0) " for ${result.result.damage} damage" else ""
                val healMsg = if (result.result.healing > 0) " healing ${result.result.healing}" else ""
                addEventLogEntry(
                    "${result.actorName} used ${result.abilityName}$damageMsg$healMsg",
                    if (result.result.damage > 0) EventLogType.DAMAGE_DEALT else EventLogType.INFO
                )
            }

            is GlobalEvent.StatusEffectChanged -> {
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

            is GlobalEvent.RoundEnded -> {
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

            is GlobalEvent.CombatEnded -> {
                val response = event.response

                // Log defeated enemies using names from combatants list (before we clear it)
                val currentCombatants = _combatants.value
                val defeatedNames = response.defeated.mapNotNull { id ->
                    currentCombatants.find { it.id == id }?.name
                }
                defeatedNames.forEach { name ->
                    addEventLogEntry("$name has been defeated!", EventLogType.COMBAT_END)
                }

                // Log XP and loot if any
                if (response.experienceGained > 0) {
                    addEventLogEntry("Gained ${response.experienceGained} XP", EventLogType.INFO)
                }
                if (response.loot.goldEarned > 0) {
                    addEventLogEntry("Looted ${response.loot.goldEarned} gold", EventLogType.LOOT)
                }
                response.loot.itemNames.forEach { itemName ->
                    addEventLogEntry("Looted: $itemName", EventLogType.LOOT)
                }

                addEventLogEntry("Combat ended!", EventLogType.COMBAT_END)
                clearCombatState()

                // Refresh user data and location to sync HP, XP, gold, items, and remove dead creatures
                scope.launch {
                    UserStateHolder.refreshUser()
                    AdventureStateHolder.refreshCurrentLocation()
                }
            }

            is GlobalEvent.FleeResult -> {
                if (event.response.success) {
                    addEventLogEntry("You fled from combat!", EventLogType.INFO)
                    clearCombatState()
                    // Refresh user data to sync HP from server
                    scope.launch {
                        UserStateHolder.refreshUser()
                    }
                } else {
                    addEventLogEntry("Failed to flee: ${event.response.message}", EventLogType.ERROR)
                }
            }

            is GlobalEvent.HealthUpdated -> {
                // Update player combatant HP if this is about us
                val update = event.update
                if (update.combatantId == connectedUserId) {
                    _playerCombatant.value?.let { current ->
                        _playerCombatant.value = current.copy(
                            currentHp = update.currentHp,
                            maxHp = update.maxHp
                        )
                    }
                }
                // Also update in combatants list
                _combatants.value = _combatants.value.map { combatant ->
                    if (combatant.id == update.combatantId) {
                        combatant.copy(currentHp = update.currentHp, maxHp = update.maxHp)
                    } else combatant
                }
            }

            is GlobalEvent.PlayerDowned -> {
                val response = event.response
                val isMe = response.playerId == connectedUserId
                if (isMe) {
                    addEventLogEntry(
                        "You collapse to the ground, bleeding out! (${response.currentHp}/${response.deathThreshold} to death)",
                        EventLogType.ERROR
                    )
                } else {
                    addEventLogEntry(
                        "${response.playerName} collapses to the ground!",
                        EventLogType.DAMAGE_RECEIVED
                    )
                }
            }

            is GlobalEvent.PlayerStabilized -> {
                val response = event.response
                val isMe = response.playerId == connectedUserId
                if (isMe) {
                    val healerMsg = response.healerName?.let { " by $it" } ?: ""
                    addEventLogEntry(
                        "You have been stabilized$healerMsg and regain consciousness!",
                        EventLogType.HEAL
                    )
                } else {
                    val healerMsg = response.healerName?.let { " by ${response.healerName}" } ?: ""
                    addEventLogEntry(
                        "${response.playerName} has been stabilized$healerMsg!",
                        EventLogType.HEAL
                    )
                }
            }

            is GlobalEvent.PlayerDied -> {
                val response = event.response
                addEventLogEntry("You have died!", EventLogType.ERROR)
                addEventLogEntry("You respawn at ${response.respawnLocationName} with full health.", EventLogType.NAVIGATION)
                clearCombatState()
                // Trigger death animation
                _respawnLocationName.value = response.respawnLocationName
                _isPlayingDeathAnimation.value = true
                // User refresh happens after animation completes via onDeathAnimationComplete()
            }

            is GlobalEvent.PlayerDragged -> {
                val response = event.response
                val iAmDragger = response.draggerId == connectedUserId
                val iWasDragged = response.targetId == connectedUserId

                when {
                    iAmDragger -> {
                        addEventLogEntry(
                            "You drag ${response.targetName} ${response.direction} to ${response.toLocationName}!",
                            EventLogType.NAVIGATION
                        )
                        clearCombatState()
                        // Navigation update will be triggered by AdventureStateHolder when it receives location change
                    }
                    iWasDragged -> {
                        addEventLogEntry(
                            "${response.draggerName} drags you ${response.direction} to ${response.toLocationName}!",
                            EventLogType.NAVIGATION
                        )
                        clearCombatState()
                    }
                    else -> {
                        addEventLogEntry(
                            "${response.draggerName} drags ${response.targetName} ${response.direction}.",
                            EventLogType.NAVIGATION
                        )
                    }
                }
            }

            is GlobalEvent.CreatureMoved -> {
                // Only show movement if creature enters or leaves the player's current room
                val currentLocationId = AdventureStateHolder.currentLocation.value?.id
                if (currentLocationId != null) {
                    when (currentLocationId) {
                        event.fromLocationId -> {
                            val directionText = event.direction?.let { " to the $it" } ?: ""
                            addEventLogEntry("${event.creatureName} wanders off$directionText.", EventLogType.NAVIGATION)
                        }
                        event.toLocationId -> {
                            // Reverse the direction for "wanders in from" (if they went north, they came from the south)
                            val oppositeDirection = event.direction?.let { getOppositeDirection(it) }
                            val directionText = oppositeDirection?.let { " from the $it" } ?: ""
                            addEventLogEntry("${event.creatureName} wanders in$directionText.", EventLogType.NAVIGATION)
                        }
                        // Silently ignore movement between other locations
                    }
                }
            }

            is GlobalEvent.LocationMutated -> {
                // Handle location mutation events (exits added/removed, items added/removed)
                AdventureStateHolder.handleLocationMutation(event.event)
            }

            is GlobalEvent.Error -> {
                addEventLogEntry("Error: ${event.message}", EventLogType.ERROR)
            }

            is GlobalEvent.ResourceUpdated -> {
                // Update player combatant mana/stamina if this is about us
                val update = event.update
                if (update.combatantId == connectedUserId) {
                    _playerCombatant.value?.let { current ->
                        _playerCombatant.value = current.copy(
                            currentMana = update.currentMana,
                            maxMana = update.maxMana,
                            currentStamina = update.currentStamina,
                            maxStamina = update.maxStamina
                        )
                    }
                }
                // Also update in combatants list
                _combatants.value = _combatants.value.map { combatant ->
                    if (combatant.id == update.combatantId) {
                        combatant.copy(
                            currentMana = update.currentMana,
                            maxMana = update.maxMana,
                            currentStamina = update.currentStamina,
                            maxStamina = update.maxStamina
                        )
                    } else combatant
                }
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
        pendingFirstAbility = null
    }

    /**
     * Public method to clear combat state.
     * Called when player leaves combat via phasewalk or other means.
     */
    fun clearCombatStatePublic() {
        clearCombatState()
        addEventLogEntry("You left combat.", EventLogType.INFO)
    }

    /**
     * Called when death animation completes.
     * Refreshes user data to get new location and HP.
     */
    fun onDeathAnimationComplete() {
        _isPlayingDeathAnimation.value = false
        _respawnLocationName.value = null
        // Now refresh user data to update location/HP
        scope.launch {
            UserStateHolder.refreshUser()
        }
    }

    /**
     * Get the opposite direction for "wanders in from" messages.
     * If creature went north, they came from the south.
     */
    private fun getOppositeDirection(direction: String): String {
        return when (direction.lowercase()) {
            "north" -> "south"
            "south" -> "north"
            "east" -> "west"
            "west" -> "east"
            "northeast" -> "southwest"
            "northwest" -> "southeast"
            "southeast" -> "northwest"
            "southwest" -> "northeast"
            "up" -> "below"
            "down" -> "above"
            else -> direction
        }
    }

    /**
     * Check if currently in combat.
     */
    val isInCombat: Boolean
        get() = _combatSession.value != null

    // =========================================================================
    // TEST HELPERS - Only for unit testing
    // =========================================================================

    /**
     * Set the connected user ID for testing.
     */
    fun setConnectedUserId(userId: String) {
        connectedUserId = userId
    }

    /**
     * Set player combatant directly for testing.
     */
    fun setPlayerCombatantForTest(combatant: CombatantDto?) {
        _playerCombatant.value = combatant
    }

    /**
     * Set combatants list directly for testing.
     */
    fun setCombatantsForTest(combatants: List<CombatantDto>) {
        _combatants.value = combatants
    }

    /**
     * Handle a GlobalEvent directly for testing.
     * This bypasses the CombatClient and lets tests trigger state changes.
     */
    fun handleEventForTest(event: GlobalEvent) {
        handleGlobalEvent(event)
    }

    /**
     * Reset all state for testing.
     */
    fun resetForTest() {
        clearCombatState()
        connectedUserId = null
    }

    /**
     * Set blinded state for testing.
     */
    fun setBlindedForTest(blinded: Boolean, rounds: Int) {
        _isBlinded.value = blinded
        _blindRounds.value = rounds
    }

    /**
     * Set disoriented state for testing.
     */
    fun setDisorientedForTest(disoriented: Boolean, rounds: Int) {
        _isDisoriented.value = disoriented
        _disorientRounds.value = rounds
    }
}
