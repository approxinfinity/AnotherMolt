package com.ez2bg.anotherthread.combat

import com.ez2bg.anotherthread.database.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * CombatService manages all active combat sessions and the combat tick loop.
 *
 * MajorMUD-style combat flow:
 * 1. Player enters combat (attacks creature or is attacked)
 * 2. Combat session created at that location
 * 3. Other players at location can join
 * 4. Every 3 seconds, a round resolves:
 *    - Pending actions execute in initiative order
 *    - Status effects tick (damage over time, buff durations)
 *    - Cooldowns decrement
 *    - Results broadcast to all participants
 * 5. Combat ends when all enemies or all players are defeated
 */
object CombatService {
    private val log = LoggerFactory.getLogger(CombatService::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Active combat sessions by session ID
    private val sessions = ConcurrentHashMap<String, CombatSession>()

    // WebSocket connections by user ID
    private val playerConnections = ConcurrentHashMap<String, WebSocketSession>()

    // Map user ID to their current session ID
    private val playerSessions = ConcurrentHashMap<String, String>()

    // Mutex for session modifications
    private val sessionMutex = Mutex()

    // Combat tick job
    private var tickJob: Job? = null
    private val tickScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Start the combat tick loop. Called once when server starts.
     */
    fun startTickLoop() {
        if (tickJob?.isActive == true) return

        tickJob = tickScope.launch {
            log.info("Combat tick loop started")
            while (isActive) {
                try {
                    processTick()
                } catch (e: Exception) {
                    log.error("Error in combat tick: ${e.message}", e)
                }
                delay(CombatConfig.ROUND_DURATION_MS)
            }
        }
    }

    /**
     * Stop the combat tick loop. Called on server shutdown.
     */
    fun stopTickLoop() {
        tickJob?.cancel()
        tickJob = null
        log.info("Combat tick loop stopped")
    }

    /**
     * Register a player's WebSocket connection.
     */
    fun registerConnection(userId: String, session: WebSocketSession) {
        playerConnections[userId] = session
        log.debug("Player $userId connected to combat service")
    }

    /**
     * Unregister a player's WebSocket connection.
     */
    fun unregisterConnection(userId: String) {
        playerConnections.remove(userId)
        val sessionId = playerSessions.remove(userId)
        if (sessionId != null) {
            log.debug("Player $userId disconnected from combat session $sessionId")
        }
    }

    /**
     * Get active session for a location, if any.
     */
    fun getSessionAtLocation(locationId: String): CombatSession? {
        return sessions.values.find {
            it.locationId == locationId && it.state != CombatState.ENDED
        }
    }

    /**
     * Start combat at a location. Creates a new session or joins existing.
     */
    suspend fun startCombat(
        userId: String,
        locationId: String,
        targetCreatureIds: List<String> = emptyList()
    ): Result<CombatSession> = sessionMutex.withLock {
        // Check if player is already in combat
        val existingSessionId = playerSessions[userId]
        if (existingSessionId != null) {
            sessions[existingSessionId]?.let {
                return Result.failure(Exception("Already in combat session ${it.id}"))
            }
        }

        // Get or create session at location
        var session = getSessionAtLocation(locationId)

        if (session == null) {
            // Create new session
            session = createSession(locationId, targetCreatureIds)
        }

        // Add player to session
        val user = UserRepository.findById(userId)
            ?: return Result.failure(Exception("User not found"))

        val playerCombatant = user.toCombatant()
        session = session.copy(
            combatants = session.combatants + playerCombatant
        )

        // If we have both players and creatures, start combat
        if (session.players.isNotEmpty() && session.creatures.isNotEmpty()) {
            session = session.copy(
                state = CombatState.ACTIVE,
                roundStartTime = System.currentTimeMillis()
            )
        }

        sessions[session.id] = session
        playerSessions[userId] = session.id

        // Update user's combat session reference
        UserRepository.updateCombatState(userId, user.currentHp, session.id)

        // Persist session
        CombatSessionRepository.create(session)

        // Notify all players
        broadcastToSession(session.id, CombatStartedMessage(session, playerCombatant))

        log.info("Player $userId joined combat session ${session.id} at location $locationId")
        return Result.success(session)
    }

    /**
     * Create a new combat session with creatures at a location.
     */
    private fun createSession(locationId: String, targetCreatureIds: List<String>): CombatSession {
        // Get creatures at location
        val location = LocationRepository.findById(locationId)
        val creatureIds = if (targetCreatureIds.isNotEmpty()) {
            targetCreatureIds
        } else {
            location?.creatureIds ?: emptyList()
        }

        val creatureCombatants = creatureIds.mapNotNull { creatureId ->
            CreatureRepository.findById(creatureId)?.toCombatant()
        }

        return CombatSession(
            locationId = locationId,
            state = if (creatureCombatants.isEmpty()) CombatState.WAITING else CombatState.WAITING,
            combatants = creatureCombatants
        )
    }

    /**
     * Queue an ability use for the current round.
     */
    suspend fun queueAbility(
        userId: String,
        sessionId: String,
        abilityId: String,
        targetId: String?
    ): Result<Unit> = sessionMutex.withLock {
        val session = sessions[sessionId]
            ?: return Result.failure(Exception("Combat session not found"))

        if (session.state != CombatState.ACTIVE) {
            return Result.failure(Exception("Combat is not active"))
        }

        val combatant = session.combatants.find { it.id == userId }
            ?: return Result.failure(Exception("Not a participant in this combat"))

        if (!combatant.isAlive) {
            return Result.failure(Exception("Cannot act while defeated"))
        }

        // Check ability exists and belongs to combatant
        val ability = AbilityRepository.findById(abilityId)
            ?: return Result.failure(Exception("Ability not found"))

        // Check cooldown
        val cooldownRemaining = combatant.cooldowns[abilityId] ?: 0
        if (cooldownRemaining > 0) {
            return Result.failure(Exception("Ability on cooldown for $cooldownRemaining more rounds"))
        }

        // Validate target if required
        if (ability.targetType != "self" && ability.targetType != "area" && targetId == null) {
            return Result.failure(Exception("Target required for this ability"))
        }

        // Remove any existing action from this combatant this round
        val filteredActions = session.pendingActions.filter { it.combatantId != userId }

        val action = CombatAction(
            combatantId = userId,
            abilityId = abilityId,
            targetId = targetId
        )

        sessions[sessionId] = session.copy(
            pendingActions = filteredActions + action
        )

        // Confirm to player
        sendToPlayer(userId, AbilityQueuedMessage(sessionId, abilityId, targetId))

        log.debug("Player $userId queued ability $abilityId in session $sessionId")
        return Result.success(Unit)
    }

    /**
     * Attempt to flee combat.
     */
    suspend fun attemptFlee(userId: String, sessionId: String): Result<Boolean> = sessionMutex.withLock {
        val session = sessions[sessionId]
            ?: return Result.failure(Exception("Combat session not found"))

        val combatant = session.combatants.find { it.id == userId }
            ?: return Result.failure(Exception("Not a participant in this combat"))

        // Check flee cooldown (can't spam flee)
        val fleeCooldown = combatant.cooldowns["__flee__"] ?: 0
        if (fleeCooldown > 0) {
            return Result.failure(Exception("Cannot flee yet, wait $fleeCooldown more rounds"))
        }

        // Roll for flee success
        val success = Math.random() < CombatConfig.FLEE_SUCCESS_CHANCE

        val message = if (success) {
            "${combatant.name} fled from combat!"
        } else {
            "${combatant.name} failed to flee!"
        }

        // Update combatant with flee cooldown on failure
        val updatedCombatants = session.combatants.map {
            if (it.id == userId && !success) {
                it.copy(cooldowns = it.cooldowns + ("__flee__" to CombatConfig.FLEE_COOLDOWN_ROUNDS))
            } else if (it.id == userId && success) {
                // Mark as "fled" by setting HP to 0 but not truly dead
                it.copy(currentHp = 0, isAlive = false)
            } else {
                it
            }
        }

        sessions[sessionId] = session.copy(combatants = updatedCombatants)

        broadcastToSession(sessionId, FleeResultMessage(sessionId, userId, success, message))

        if (success) {
            // Remove player from session tracking
            playerSessions.remove(userId)
            UserRepository.updateCombatState(userId, combatant.currentHp, null)
        }

        return Result.success(success)
    }

    /**
     * Process a combat tick - resolve all pending actions and advance the round.
     */
    private suspend fun processTick() = sessionMutex.withLock {
        val now = System.currentTimeMillis()

        for ((sessionId, session) in sessions) {
            if (session.state != CombatState.ACTIVE) continue

            // Check if round duration has passed
            if (now - session.roundStartTime < CombatConfig.ROUND_DURATION_MS) continue

            // Process the round
            val updatedSession = processRound(session)

            // Check for combat end conditions
            val finalSession = checkEndConditions(updatedSession)

            sessions[sessionId] = finalSession
            CombatSessionRepository.update(finalSession)

            if (finalSession.state == CombatState.ENDED) {
                handleCombatEnd(finalSession)
            }
        }
    }

    /**
     * Process a single round of combat.
     */
    private suspend fun processRound(session: CombatSession): CombatSession {
        val roundNumber = session.currentRound + 1
        val logEntries = mutableListOf<CombatLogEntry>()

        // Broadcast round start
        broadcastToSession(session.id, RoundStartMessage(
            sessionId = session.id,
            roundNumber = roundNumber,
            roundDurationMs = CombatConfig.ROUND_DURATION_MS,
            combatants = session.combatants
        ))

        // Sort actions by combatant initiative
        val sortedActions = session.pendingActions.sortedByDescending { action ->
            session.combatants.find { it.id == action.combatantId }?.initiative ?: 0
        }

        var currentCombatants = session.combatants.toMutableList()

        // Execute each action
        for (action in sortedActions) {
            val actor = currentCombatants.find { it.id == action.combatantId }
            if (actor == null || !actor.isAlive) continue

            val ability = AbilityRepository.findById(action.abilityId) ?: continue
            val target = action.targetId?.let { tid -> currentCombatants.find { it.id == tid } }

            // Execute the ability
            val result = executeAbility(actor, ability, target, currentCombatants)

            // Update combatants based on result
            currentCombatants = applyActionResult(currentCombatants, action, result).toMutableList()

            // Create log entry
            val logEntry = CombatLogEntry(
                round = roundNumber,
                actorId = actor.id,
                actorName = actor.name,
                targetId = target?.id,
                targetName = target?.name,
                abilityName = ability.name,
                damage = result.damage,
                healing = result.healing,
                message = result.message
            )
            logEntries.add(logEntry)

            // Broadcast ability resolution
            broadcastToSession(session.id, AbilityResolvedMessage(
                sessionId = session.id,
                result = result,
                actorName = actor.name,
                targetName = target?.name,
                abilityName = ability.name
            ))

            // Broadcast health updates for affected combatants
            if (result.damage > 0 && target != null) {
                val updatedTarget = currentCombatants.find { it.id == target.id }
                if (updatedTarget != null) {
                    broadcastToSession(session.id, HealthUpdateMessage(
                        sessionId = session.id,
                        combatantId = target.id,
                        currentHp = updatedTarget.currentHp,
                        maxHp = updatedTarget.maxHp,
                        changeAmount = result.damage,
                        sourceId = actor.id,
                        sourceName = actor.name
                    ))
                }
            }
        }

        // Process status effects (DoTs, HoTs, duration decrements)
        currentCombatants = processStatusEffects(currentCombatants, session.id).toMutableList()

        // Decrement cooldowns
        currentCombatants = decrementCooldowns(currentCombatants).toMutableList()

        // AI actions for creatures
        currentCombatants = processCreatureAI(currentCombatants, session).toMutableList()

        // Broadcast round end
        broadcastToSession(session.id, RoundEndMessage(
            sessionId = session.id,
            roundNumber = roundNumber,
            combatants = currentCombatants,
            logEntries = logEntries
        ))

        return session.copy(
            currentRound = roundNumber,
            roundStartTime = System.currentTimeMillis(),
            combatants = currentCombatants,
            pendingActions = emptyList(), // Clear actions for next round
            combatLog = session.combatLog + logEntries
        )
    }

    /**
     * Execute an ability and return the result.
     */
    private fun executeAbility(
        actor: Combatant,
        ability: Ability,
        target: Combatant?,
        allCombatants: List<Combatant>
    ): ActionResult {
        // Calculate damage/healing
        val damage = if (ability.baseDamage > 0 && target != null) {
            // Simple damage calculation, can be enhanced later
            ability.baseDamage + (actor.initiative / 2)
        } else 0

        val healing = if (ability.effects.contains("heal")) {
            ability.baseDamage // Use baseDamage as heal amount for healing abilities
        } else 0

        // Build result message
        val message = when {
            damage > 0 && target != null -> "${actor.name} hits ${target.name} with ${ability.name} for $damage damage!"
            healing > 0 -> "${actor.name} heals for $healing with ${ability.name}!"
            else -> "${actor.name} uses ${ability.name}!"
        }

        return ActionResult(
            actionId = "${actor.id}-${ability.id}",
            success = true,
            damage = damage,
            healing = healing,
            appliedEffects = emptyList(), // TODO: Parse and apply effects
            message = message
        )
    }

    /**
     * Apply action result to combatants.
     */
    private fun applyActionResult(
        combatants: List<Combatant>,
        action: CombatAction,
        result: ActionResult
    ): List<Combatant> {
        return combatants.map { combatant ->
            when {
                // Apply damage to target
                combatant.id == action.targetId && result.damage > 0 -> {
                    val newHp = (combatant.currentHp - result.damage).coerceAtLeast(0)
                    combatant.copy(
                        currentHp = newHp,
                        isAlive = newHp > 0
                    )
                }
                // Apply healing to actor (self-heal) or target
                combatant.id == action.combatantId && result.healing > 0 && action.targetId == null -> {
                    val newHp = (combatant.currentHp + result.healing).coerceAtMost(combatant.maxHp)
                    combatant.copy(currentHp = newHp)
                }
                combatant.id == action.targetId && result.healing > 0 -> {
                    val newHp = (combatant.currentHp + result.healing).coerceAtMost(combatant.maxHp)
                    combatant.copy(currentHp = newHp)
                }
                // Apply cooldown to actor
                combatant.id == action.combatantId -> {
                    val ability = AbilityRepository.findById(action.abilityId)
                    val cooldownRounds = ability?.cooldownRounds ?: 0
                    if (cooldownRounds > 0) {
                        combatant.copy(cooldowns = combatant.cooldowns + (action.abilityId to cooldownRounds))
                    } else combatant
                }
                else -> combatant
            }
        }
    }

    /**
     * Process status effects for all combatants.
     */
    private suspend fun processStatusEffects(
        combatants: List<Combatant>,
        sessionId: String
    ): List<Combatant> {
        return combatants.map { combatant ->
            var hp = combatant.currentHp
            val remainingEffects = mutableListOf<StatusEffect>()

            for (effect in combatant.statusEffects) {
                // Apply effect
                when (effect.effectType) {
                    "dot" -> {
                        hp = (hp - effect.value).coerceAtLeast(0)
                        broadcastToSession(sessionId, HealthUpdateMessage(
                            sessionId = sessionId,
                            combatantId = combatant.id,
                            currentHp = hp,
                            maxHp = combatant.maxHp,
                            changeAmount = effect.value,
                            sourceId = effect.sourceId,
                            sourceName = null
                        ))
                    }
                    "hot" -> {
                        hp = (hp + effect.value).coerceAtMost(combatant.maxHp)
                        broadcastToSession(sessionId, HealthUpdateMessage(
                            sessionId = sessionId,
                            combatantId = combatant.id,
                            currentHp = hp,
                            maxHp = combatant.maxHp,
                            changeAmount = -effect.value,
                            sourceId = effect.sourceId,
                            sourceName = null
                        ))
                    }
                }

                // Decrement duration
                val newDuration = effect.remainingRounds - 1
                if (newDuration > 0) {
                    remainingEffects.add(effect.copy(remainingRounds = newDuration))
                } else {
                    // Effect expired
                    broadcastToSession(sessionId, StatusEffectMessage(
                        sessionId = sessionId,
                        combatantId = combatant.id,
                        effect = effect,
                        applied = false
                    ))
                }
            }

            combatant.copy(
                currentHp = hp,
                isAlive = hp > 0,
                statusEffects = remainingEffects
            )
        }
    }

    /**
     * Decrement ability cooldowns.
     */
    private fun decrementCooldowns(combatants: List<Combatant>): List<Combatant> {
        return combatants.map { combatant ->
            val newCooldowns = combatant.cooldowns
                .mapValues { (_, rounds) -> rounds - 1 }
                .filter { (_, rounds) -> rounds > 0 }
            combatant.copy(cooldowns = newCooldowns)
        }
    }

    /**
     * Simple AI for creature actions.
     */
    private fun processCreatureAI(
        combatants: List<Combatant>,
        session: CombatSession
    ): List<Combatant> {
        val alivePlayers = combatants.filter { it.type == CombatantType.PLAYER && it.isAlive }
        if (alivePlayers.isEmpty()) return combatants

        return combatants.map { creature ->
            if (creature.type != CombatantType.CREATURE || !creature.isAlive) return@map creature

            // Find a target (random alive player)
            val target = alivePlayers.random()

            // Simple attack: deal base damage from creature
            val dbCreature = CreatureRepository.findById(creature.id)
            val damage = dbCreature?.baseDamage ?: 5

            // Apply damage to target in next tick (queue as pending action)
            // For now, just do basic attack damage directly
            creature
        }
    }

    /**
     * Check if combat should end.
     */
    private fun checkEndConditions(session: CombatSession): CombatSession {
        val alivePlayers = session.alivePlayers
        val aliveCreatures = session.aliveCreatures

        val (state, endReason) = when {
            alivePlayers.isEmpty() && aliveCreatures.isEmpty() ->
                CombatState.ENDED to CombatEndReason.TIMEOUT
            alivePlayers.isEmpty() ->
                CombatState.ENDED to CombatEndReason.ALL_PLAYERS_DEFEATED
            aliveCreatures.isEmpty() && session.creatures.isNotEmpty() ->
                CombatState.ENDED to CombatEndReason.ALL_ENEMIES_DEFEATED
            session.currentRound >= CombatConfig.MAX_COMBAT_ROUNDS ->
                CombatState.ENDED to CombatEndReason.TIMEOUT
            else -> session.state to session.endReason
        }

        return session.copy(state = state, endReason = endReason)
    }

    /**
     * Handle combat ending - cleanup, rewards, etc.
     */
    private suspend fun handleCombatEnd(session: CombatSession) {
        val victors = when (session.endReason) {
            CombatEndReason.ALL_ENEMIES_DEFEATED -> session.players.map { it.id }
            CombatEndReason.ALL_PLAYERS_DEFEATED -> session.creatures.map { it.id }
            else -> emptyList()
        }

        val defeated = when (session.endReason) {
            CombatEndReason.ALL_ENEMIES_DEFEATED -> session.creatures.map { it.id }
            CombatEndReason.ALL_PLAYERS_DEFEATED -> session.players.map { it.id }
            else -> emptyList()
        }

        // Calculate experience for players if they won
        val totalExp = if (session.endReason == CombatEndReason.ALL_ENEMIES_DEFEATED) {
            session.creatures.sumOf { creature ->
                CreatureRepository.findById(creature.id)?.experienceValue ?: 0
            }
        } else 0

        // Award experience to surviving players
        val expPerPlayer = if (victors.isNotEmpty() && totalExp > 0) {
            totalExp / victors.size
        } else 0

        for (playerId in session.alivePlayers.map { it.id }) {
            if (expPerPlayer > 0) {
                UserRepository.awardExperience(playerId, expPerPlayer)
            }
            // Clear combat state
            val playerCombatant = session.combatants.find { it.id == playerId }
            UserRepository.updateCombatState(playerId, playerCombatant?.currentHp ?: 1, null)
            playerSessions.remove(playerId)
        }

        // Broadcast combat end
        broadcastToSession(session.id, CombatEndedMessage(
            sessionId = session.id,
            reason = session.endReason!!,
            victors = victors,
            defeated = defeated,
            experienceGained = expPerPlayer
        ))

        log.info("Combat session ${session.id} ended: ${session.endReason}")
    }

    /**
     * Broadcast a message to all players in a session.
     */
    private suspend fun broadcastToSession(sessionId: String, message: ServerCombatMessage) {
        val session = sessions[sessionId] ?: return

        for (combatant in session.players) {
            sendToPlayer(combatant.id, message)
        }
    }

    /**
     * Send a message to a specific player.
     */
    private suspend fun sendToPlayer(userId: String, message: ServerCombatMessage) {
        val connection = playerConnections[userId] ?: return
        try {
            val jsonMessage = json.encodeToString(message)
            connection.send(Frame.Text(jsonMessage))
        } catch (e: Exception) {
            log.error("Failed to send message to player $userId: ${e.message}")
        }
    }

    /**
     * Extension: Convert User to Combatant
     */
    private fun User.toCombatant(): Combatant {
        val classAbilities = characterClassId?.let {
            AbilityRepository.findByClassId(it).map { a -> a.id }
        } ?: emptyList()

        return Combatant(
            id = id,
            type = CombatantType.PLAYER,
            name = name,
            maxHp = maxHp,
            currentHp = currentHp,
            characterClassId = characterClassId,
            abilityIds = classAbilities,
            initiative = (1..20).random() // D20 initiative roll
        )
    }

    /**
     * Extension: Convert Creature to Combatant
     */
    private fun Creature.toCombatant(): Combatant {
        return Combatant(
            id = id,
            type = CombatantType.CREATURE,
            name = name,
            maxHp = maxHp,
            currentHp = maxHp, // Creatures start at full HP
            abilityIds = abilityIds,
            initiative = (1..20).random()
        )
    }
}
