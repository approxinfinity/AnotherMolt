package com.ez2bg.anotherthread.combat

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.experience.ExperienceService
import com.ez2bg.anotherthread.game.CreatureRespawnService
import com.ez2bg.anotherthread.game.GameTickService
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Configuration for death mechanics.
 */
object DeathConfig {
    /** If true, items are dropped at death location. If false, player keeps all items. */
    var itemsDropOnDeath: Boolean = false

    /** If true, gold is lost on death. If false, player keeps all gold. */
    var goldLostOnDeath: Boolean = false
}

/**
 * Helper object for logging combat events to the database for auditing.
 */
object CombatEventLogger {
    /**
     * Convert a Combatant to a snapshot for logging.
     */
    fun Combatant.toSnapshot(locationId: String? = null): CombatantSnapshot = CombatantSnapshot(
        id = id,
        name = name,
        type = if (type == CombatantType.PLAYER) "player" else "creature",
        level = level,
        currentHp = currentHp,
        maxHp = maxHp,
        currentMana = if (type == CombatantType.PLAYER) currentMana else null,
        maxMana = if (type == CombatantType.PLAYER) maxMana else null,
        currentStamina = if (type == CombatantType.PLAYER) currentStamina else null,
        maxStamina = if (type == CombatantType.PLAYER) maxStamina else null,
        accuracy = accuracy,
        evasion = evasion,
        baseDamage = baseDamage,
        critBonus = critBonus,
        isAlive = isAlive,
        isDowned = isDowned,
        statusEffects = statusEffects.map { "${it.name}(${it.remainingRounds})" },
        locationId = locationId
    )

    /**
     * Log a combat event with full context.
     */
    fun logEvent(
        session: CombatSession,
        eventType: CombatEventType,
        message: String,
        actor: Combatant? = null,
        target: Combatant? = null,
        includeAllCombatants: Boolean = false,
        eventData: Map<String, Any>? = null
    ) {
        try {
            val locationName = LocationRepository.findById(session.locationId)?.name ?: "Unknown"
            CombatEventLogRepository.log(
                sessionId = session.id,
                locationId = session.locationId,
                locationName = locationName,
                eventType = eventType,
                roundNumber = session.currentRound,
                message = message,
                actorSnapshot = actor?.toSnapshot(session.locationId),
                targetSnapshot = target?.toSnapshot(session.locationId),
                allCombatants = if (includeAllCombatants) {
                    session.combatants.map { it.toSnapshot(session.locationId) }
                } else null,
                eventData = eventData
            )
        } catch (e: Exception) {
            // Don't let logging failures affect combat
            LoggerFactory.getLogger(CombatEventLogger::class.java)
                .warn("Failed to log combat event: ${e.message}")
        }
    }
}

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

    // Track which creatures are wandering (non-aggressive, not in combat)
    private val wanderingCreatures = mutableSetOf<String>()
    private var lastWanderTick = 0L

    /**
     * Generate a descriptive engagement message for a creature attacking a player.
     * Uses creature name to generate flavorful attack verbs.
     */
    private fun generateEngagementMessage(creatureName: String, playerName: String): String {
        val lowerName = creatureName.lowercase()
        val verb = when {
            lowerName.contains("shambler") -> "shambles toward"
            lowerName.contains("zombie") -> "lurches toward"
            lowerName.contains("skeleton") -> "rattles toward"
            lowerName.contains("ghost") || lowerName.contains("specter") || lowerName.contains("wraith") -> "floats menacingly toward"
            lowerName.contains("spider") -> "skitters toward"
            lowerName.contains("wolf") || lowerName.contains("hound") -> "growls and leaps at"
            lowerName.contains("goblin") -> "shrieks and charges at"
            lowerName.contains("orc") -> "roars and rushes at"
            lowerName.contains("troll") -> "lumbers toward"
            lowerName.contains("rat") -> "scurries toward"
            lowerName.contains("snake") || lowerName.contains("serpent") -> "slithers toward"
            lowerName.contains("bat") -> "swoops at"
            lowerName.contains("slime") || lowerName.contains("ooze") -> "oozes toward"
            lowerName.contains("golem") -> "stomps toward"
            lowerName.contains("bandit") || lowerName.contains("thief") -> "sneaks up on"
            lowerName.contains("dragon") -> "roars and turns its attention to"
            lowerName.contains("demon") -> "snarls and advances on"
            lowerName.contains("fungus") || lowerName.contains("mushroom") -> "lurches toward"
            lowerName.contains("plant") || lowerName.contains("vine") -> "writhes toward"
            lowerName.contains("elemental") -> "surges toward"
            lowerName.contains("imp") -> "cackles and darts at"
            else -> "attacks"
        }
        return "$creatureName $verb $playerName!"
    }

    // Note: Tick loop is now managed by GameTickService
    // CombatService.processTick() and processCreatureWandering() are called from there

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
     * Remove a player from combat when they leave a location.
     * Called when player navigates to a different room.
     */
    suspend fun removePlayerFromCombat(userId: String) = sessionMutex.withLock {
        val sessionId = playerSessions[userId] ?: return@withLock

        val session = sessions[sessionId] ?: return@withLock

        val combatant = session.combatants.find { it.id == userId } ?: return@withLock

        log.info("Removing $userId from combat session $sessionId (left location)")

        // Remove player from session tracking
        playerSessions.remove(userId)

        // Update user's combat state in DB
        UserRepository.updateCombatState(userId, combatant.currentHp, null)

        // Mark player as no longer in combat (set isAlive false but preserve HP)
        val updatedCombatants = session.combatants.map {
            if (it.id == userId) {
                it.copy(isAlive = false)
            } else {
                it
            }
        }

        val updatedSession = session.copy(combatants = updatedCombatants)
        sessions[sessionId] = updatedSession

        // Notify the player they've disengaged - send directly to them
        val endMessage = CombatEndedMessage(
            sessionId = sessionId,
            reason = CombatEndReason.PLAYER_LEFT,
            victors = emptyList(),
            defeated = listOf(userId)
        )
        log.info("Sending CombatEndedMessage (PLAYER_LEFT) directly to player $userId for session $sessionId")
        sendToPlayer(userId, endMessage)

        // Check if combat should end (no players left)
        if (updatedSession.alivePlayers.isEmpty()) {
            log.info("All players left combat session $sessionId, ending session")
            sessions.remove(sessionId)
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
     * Creature activity state - used for UI display.
     */
    enum class CreatureActivityState {
        IDLE,       // Standing still
        WANDERING,  // Moving between locations
        IN_COMBAT   // Engaged in combat
    }

    /**
     * Get the current activity state of a creature.
     */
    fun getCreatureState(creatureId: String): CreatureActivityState {
        // Check if creature is in active combat
        val inCombat = sessions.values.any { session ->
            session.state != CombatState.ENDED &&
            session.creatures.any { it.id == creatureId }
        }
        if (inCombat) return CreatureActivityState.IN_COMBAT

        // Check if creature is non-aggressive (wanderers)
        val creature = CreatureRepository.findById(creatureId)
        if (creature != null && !creature.isAggressive) {
            return CreatureActivityState.WANDERING
        }

        return CreatureActivityState.IDLE
    }

    /**
     * Get activity states for all creatures (efficient batch query).
     */
    fun getAllCreatureStates(): Map<String, CreatureActivityState> {
        val result = mutableMapOf<String, CreatureActivityState>()

        // Get all creatures in combat
        val creaturesInCombat = sessions.values
            .filter { it.state != CombatState.ENDED }
            .flatMap { it.creatures.map { c -> c.id } }
            .toSet()

        // Get all non-aggressive creatures (wanderers)
        val allCreatures = CreatureRepository.findAll()

        for (creature in allCreatures) {
            result[creature.id] = when {
                creature.id in creaturesInCombat -> CreatureActivityState.IN_COMBAT
                !creature.isAggressive -> CreatureActivityState.WANDERING
                else -> CreatureActivityState.IDLE
            }
        }

        return result
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
        val isNewSession = session == null

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
            log.info("Starting combat: session ${session.id} now ACTIVE with ${session.players.size} players and ${session.creatures.size} creatures")
            session = session.copy(
                state = CombatState.ACTIVE,
                roundStartTime = System.currentTimeMillis()
            )
        } else {
            log.warn("Combat session ${session.id} staying in ${session.state} state: players=${session.players.size}, creatures=${session.creatures.size}")
        }

        sessions[session.id] = session
        playerSessions[userId] = session.id

        // Update user's combat session reference
        UserRepository.updateCombatState(userId, user.currentHp, session.id)

        // Persist session - create if new, update if existing
        if (isNewSession) {
            CombatSessionRepository.create(session)
        } else {
            CombatSessionRepository.update(session)
        }

        // Generate engagement messages for each creature attacking this player
        val engagementMessages = session.creatures.map { creature ->
            generateEngagementMessage(creature.name, user.name)
        }
        log.info("Combat session ${session.id}: state=${session.state}, ${session.creatures.size} creatures, ${session.players.size} players, engagement messages: $engagementMessages")

        // Log combat event: session created or player joined
        if (isNewSession) {
            CombatEventLogger.logEvent(
                session = session,
                eventType = CombatEventType.SESSION_CREATED,
                message = "Combat started at ${LocationRepository.findById(locationId)?.name ?: locationId}",
                actor = playerCombatant,
                includeAllCombatants = true
            )
        }
        CombatEventLogger.logEvent(
            session = session,
            eventType = CombatEventType.PLAYER_JOINED,
            message = "${user.name} joined combat",
            actor = playerCombatant,
            includeAllCombatants = true,
            eventData = mapOf("engagementMessages" to engagementMessages)
        )

        // Send CombatStartedMessage ONLY to the joining player (not all players)
        // This prevents duplicate "Combat started!" logs for existing players
        // Existing players will see the new combatant in the next RoundStartMessage
        sendToPlayer(userId, CombatStartedMessage(session, playerCombatant, engagementMessages))

        log.info("Player $userId joined combat session ${session.id} at location $locationId")
        return Result.success(session)
    }

    /**
     * Check for aggressive creatures when a player enters a location.
     * Returns the session if combat was started, null otherwise.
     */
    suspend fun checkAggressiveCreatures(userId: String, locationId: String): CombatSession? {
        val location = LocationRepository.findById(locationId) ?: return null

        // Find aggressive creatures at this location
        val aggressiveCreatureIds = location.creatureIds.mapNotNull { creatureId ->
            CreatureRepository.findById(creatureId)?.takeIf { it.isAggressive }?.id
        }

        if (aggressiveCreatureIds.isEmpty()) return null

        // Check if player is already in combat
        if (playerSessions[userId] != null) return null

        // Start combat with all aggressive creatures
        log.info("Player $userId encountered ${aggressiveCreatureIds.size} aggressive creature(s) at $locationId")
        val result = startCombat(userId, locationId, aggressiveCreatureIds)

        return result.getOrNull()
    }

    /**
     * Create a new combat session with creatures at a location.
     * Only includes creatures that are actually at this location (prevents stale data bugs).
     */
    private fun createSession(locationId: String, targetCreatureIds: List<String>): CombatSession {
        // Get fresh location data to verify which creatures are actually here
        val location = LocationRepository.findById(locationId)
        val creaturesActuallyAtLocation = location?.creatureIds?.toSet() ?: emptySet()

        // If specific targets requested, verify they're actually at this location
        val creatureIds = if (targetCreatureIds.isNotEmpty()) {
            val verified = targetCreatureIds.filter { it in creaturesActuallyAtLocation }
            if (verified.size != targetCreatureIds.size) {
                val missing = targetCreatureIds.filter { it !in creaturesActuallyAtLocation }
                log.warn("Combat session creation: ${missing.size} creature(s) not at location $locationId: $missing")
            }
            verified
        } else {
            creaturesActuallyAtLocation.toList()
        }

        val creatureCombatants = creatureIds.mapNotNull { creatureId ->
            CreatureRepository.findById(creatureId)?.toCombatant()
        }

        log.info("Creating combat session at $locationId with ${creatureCombatants.size} creature(s): ${creatureCombatants.map { it.name }}")

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
            log.warn("queueAbility rejected: session $sessionId state is ${session.state}, not ACTIVE. " +
                "Players: ${session.players.size}, Creatures: ${session.creatures.size}")
            return Result.failure(Exception("Combat is not active (session state: ${session.state})"))
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

        // Check mana cost (for players only)
        if (combatant.type == CombatantType.PLAYER && ability.manaCost > 0) {
            if (combatant.currentMana < ability.manaCost) {
                return Result.failure(Exception("Not enough mana (need ${ability.manaCost}, have ${combatant.currentMana})"))
            }
        }

        // Check stamina cost (for players only)
        if (combatant.type == CombatantType.PLAYER && ability.staminaCost > 0) {
            if (combatant.currentStamina < ability.staminaCost) {
                return Result.failure(Exception("Not enough stamina (need ${ability.staminaCost}, have ${combatant.currentStamina})"))
            }
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
    /**
     * Process combat tick for all active sessions.
     * Called by GameTickService on the global tick.
     */
    suspend fun processTick() = sessionMutex.withLock {
        val now = System.currentTimeMillis()

        if (sessions.isNotEmpty()) {
            log.debug("processTick: ${sessions.size} sessions")
        }

        for ((sessionId, session) in sessions) {
            log.debug("Session $sessionId: state=${session.state}, players=${session.players.size}, creatures=${session.creatures.size}")
            if (session.state != CombatState.ACTIVE) continue

            // Check if round duration has passed
            val timeSinceRoundStart = now - session.roundStartTime
            if (timeSinceRoundStart < CombatConfig.ROUND_DURATION_MS) {
                log.debug("Session $sessionId: waiting for round (${timeSinceRoundStart}ms / ${CombatConfig.ROUND_DURATION_MS}ms)")
                continue
            }

            log.info("Processing round for session $sessionId")
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

        log.info("=== ROUND $roundNumber START === Session ${session.id}")
        log.info("  Players: ${session.players.map { "${it.name}(HP:${it.currentHp}/${it.maxHp})" }}")
        log.info("  Creatures: ${session.creatures.map { "${it.name}(HP:${it.currentHp}/${it.maxHp}, alive:${it.isAlive})" }}")
        log.info("  Pending actions: ${session.pendingActions.size}")

        // Broadcast round start
        broadcastToSession(session.id, RoundStartMessage(
            sessionId = session.id,
            roundNumber = roundNumber,
            roundDurationMs = CombatConfig.ROUND_DURATION_MS,
            combatants = session.combatants
        ))

        // Log round start with all combatant stats
        CombatEventLogger.logEvent(
            session = session.copy(currentRound = roundNumber),
            eventType = CombatEventType.ROUND_START,
            message = "Round $roundNumber started",
            includeAllCombatants = true
        )

        // Add auto-attacks for players who didn't queue an action
        // MajorMUD-style: if a player is in combat and doesn't specify an action, they auto-attack
        val playersWithActions = session.pendingActions.map { it.combatantId }.toSet()
        val autoAttackActions = session.alivePlayers
            .filter { player -> player.id !in playersWithActions }
            .mapNotNull { player ->
                // Find the first alive enemy to target
                val target = session.aliveCreatures.firstOrNull()
                if (target != null) {
                    log.debug("Auto-attack: ${player.name} attacks ${target.name}")
                    CombatAction(
                        combatantId = player.id,
                        abilityId = "universal-basic-attack",  // Use the universal basic attack
                        targetId = target.id
                    )
                } else null
            }

        // Combine manual actions with auto-attacks
        val allActions = session.pendingActions + autoAttackActions
        log.info("  Total actions (manual + auto): ${allActions.size} (${session.pendingActions.size} manual, ${autoAttackActions.size} auto)")

        // Sort actions by combatant initiative
        val sortedActions = allActions.sortedByDescending { action ->
            session.combatants.find { it.id == action.combatantId }?.initiative ?: 0
        }

        var currentCombatants = session.combatants.toMutableList()

        // Execute each action
        for (action in sortedActions) {
            var actor = currentCombatants.find { it.id == action.combatantId }
            if (actor == null || !actor.isAlive) continue

            val ability = AbilityRepository.findById(action.abilityId) ?: continue
            val target = action.targetId?.let { tid -> currentCombatants.find { it.id == tid } }

            // Check and spend resources for players
            if (actor.type == CombatantType.PLAYER) {
                // Check mana
                if (ability.manaCost > 0 && actor.currentMana < ability.manaCost) {
                    log.debug("${actor.name} doesn't have enough mana for ${ability.name}")
                    continue
                }
                // Check stamina
                if (ability.staminaCost > 0 && actor.currentStamina < ability.staminaCost) {
                    log.debug("${actor.name} doesn't have enough stamina for ${ability.name}")
                    continue
                }

                // Spend resources
                val newMana = actor.currentMana - ability.manaCost
                val newStamina = actor.currentStamina - ability.staminaCost

                if (ability.manaCost > 0 || ability.staminaCost > 0) {
                    // Update combatant state
                    actor = actor.copy(currentMana = newMana, currentStamina = newStamina)
                    currentCombatants = currentCombatants.map {
                        if (it.id == actor.id) actor else it
                    }.toMutableList()

                    // Sync to database
                    if (ability.manaCost > 0) UserRepository.spendMana(actor.id, ability.manaCost)
                    if (ability.staminaCost > 0) UserRepository.spendStamina(actor.id, ability.staminaCost)

                    // Notify clients
                    broadcastToSession(session.id, ResourceUpdateMessage(
                        sessionId = session.id,
                        combatantId = actor.id,
                        currentMana = newMana,
                        maxMana = actor.maxMana,
                        currentStamina = newStamina,
                        maxStamina = actor.maxStamina,
                        manaChange = -ability.manaCost,
                        staminaChange = -ability.staminaCost
                    ))
                }
            }

            // Determine targets based on ability targetType
            val targets: List<Combatant> = when (ability.targetType) {
                "area", "all_enemies" -> {
                    if (actor.type == CombatantType.PLAYER) {
                        currentCombatants.filter { it.type == CombatantType.CREATURE && it.isAlive }
                    } else {
                        currentCombatants.filter { it.type == CombatantType.PLAYER && it.isAlive }
                    }
                }
                "all_allies" -> {
                    currentCombatants.filter { it.type == actor.type && it.isAlive }
                }
                "single_ally_downed" -> {
                    // Aid/Drag: target must be a downed player ally
                    val downedTarget = target?.takeIf {
                        it.type == CombatantType.PLAYER && it.isDowned && it.id != actor.id
                    }
                    if (downedTarget == null) {
                        log.debug("${actor.name} tried to use ${ability.name} but target is not a downed ally")
                        continue  // Skip this action if no valid downed target
                    }
                    listOf(downedTarget)
                }
                else -> {
                    listOfNotNull(target)
                }
            }

            // Execute ability for each target
            for (currentTarget in targets.ifEmpty { listOf(null) }) {
                val result = executeAbility(actor, ability, currentTarget, currentCombatants)

                // Update combatants based on result
                currentCombatants = applyActionResult(
                    currentCombatants,
                    action.copy(targetId = currentTarget?.id),
                    result
                ).toMutableList()

                // Create log entry
                val logEntry = CombatLogEntry(
                    round = roundNumber,
                    actorId = actor.id,
                    actorName = actor.name,
                    targetId = currentTarget?.id,
                    targetName = currentTarget?.name,
                    abilityName = ability.name,
                    damage = result.damage,
                    healing = result.healing,
                    message = result.message
                )
                logEntries.add(logEntry)

                // Log ability use to combat event log
                val eventType = if (actor.type == CombatantType.PLAYER) {
                    CombatEventType.PLAYER_ATTACK
                } else {
                    CombatEventType.ABILITY_USED
                }
                CombatEventLogger.logEvent(
                    session = session.copy(currentRound = roundNumber),
                    eventType = eventType,
                    message = result.message,
                    actor = actor,
                    target = currentTarget,
                    eventData = mapOf(
                        "abilityId" to ability.id,
                        "abilityName" to ability.name,
                        "damage" to result.damage,
                        "healing" to result.healing,
                        "hitResult" to (result.hitResult ?: "N/A"),
                        "wasCritical" to result.wasCritical,
                        "wasGlancing" to result.wasGlancing
                    )
                )

                // Broadcast ability resolution
                broadcastToSession(session.id, AbilityResolvedMessage(
                    sessionId = session.id,
                    result = result,
                    actorName = actor.name,
                    targetName = currentTarget?.name,
                    abilityName = ability.name
                ))

                // Broadcast health updates for affected combatants
                if (result.damage > 0 && currentTarget != null) {
                    val updatedTarget = currentCombatants.find { it.id == currentTarget.id }
                    if (updatedTarget != null) {
                        broadcastToSession(session.id, HealthUpdateMessage(
                            sessionId = session.id,
                            combatantId = currentTarget.id,
                            combatantName = currentTarget.name,
                            currentHp = updatedTarget.currentHp,
                            maxHp = updatedTarget.maxHp,
                            changeAmount = result.damage,
                            sourceId = actor.id,
                            sourceName = actor.name
                        ))
                    }
                }

                // Broadcast health updates for healing
                if (result.healing > 0) {
                    val healTargetId = if (ability.targetType == "self" || currentTarget == null) {
                        actor.id
                    } else {
                        currentTarget.id
                    }
                    val updatedHealTarget = currentCombatants.find { it.id == healTargetId }
                    if (updatedHealTarget != null) {
                        broadcastToSession(session.id, HealthUpdateMessage(
                            sessionId = session.id,
                            combatantId = healTargetId,
                            combatantName = updatedHealTarget.name,
                            currentHp = updatedHealTarget.currentHp,
                            maxHp = updatedHealTarget.maxHp,
                            changeAmount = -result.healing,
                            sourceId = actor.id,
                            sourceName = actor.name
                        ))

                        // Check if this was an Aid ability that stabilized a downed player
                        val wasDownedTarget = currentTarget?.isDowned == true
                        val isNowStabilized = updatedHealTarget.currentHp > 0 && !updatedHealTarget.isDowned
                        if (wasDownedTarget && isNowStabilized && updatedHealTarget.type == CombatantType.PLAYER) {
                            broadcastToSession(session.id, PlayerStabilizedMessage(
                                sessionId = session.id,
                                playerId = updatedHealTarget.id,
                                playerName = updatedHealTarget.name,
                                currentHp = updatedHealTarget.currentHp,
                                healerId = actor.id,
                                healerName = actor.name
                            ))
                        }
                    }
                }

                // Handle Drag ability: move both players to adjacent location
                if (ability.effects.contains("drag") && currentTarget != null && currentTarget.isDowned) {
                    val location = LocationRepository.findById(session.locationId)
                    if (location != null) {
                        // Find a random adjacent exit (excluding ENTER portals)
                        val exits = location.exits.filter { it.direction != ExitDirection.ENTER }
                        if (exits.isNotEmpty()) {
                            val randomExit = exits.random()
                            val targetLocation = LocationRepository.findById(randomExit.locationId)
                            if (targetLocation != null) {
                                // Remove both players from combat
                                currentCombatants = currentCombatants.filter {
                                    it.id != actor.id && it.id != currentTarget.id
                                }.toMutableList()

                                // Update their locations in the database
                                UserRepository.updateCurrentLocation(actor.id, targetLocation.id)
                                UserRepository.updateCurrentLocation(currentTarget.id, targetLocation.id)

                                // Clear their combat state
                                UserRepository.updateCombatState(actor.id, actor.currentHp, null)
                                UserRepository.updateCombatState(currentTarget.id, currentTarget.currentHp, null)
                                playerSessions.remove(actor.id)
                                playerSessions.remove(currentTarget.id)

                                // Broadcast the drag message
                                val directionName = randomExit.direction.name.lowercase().replace("_", "")
                                broadcastToSession(session.id, PlayerDraggedMessage(
                                    sessionId = session.id,
                                    draggerId = actor.id,
                                    draggerName = actor.name,
                                    targetId = currentTarget.id,
                                    targetName = currentTarget.name,
                                    fromLocationId = session.locationId,
                                    toLocationId = targetLocation.id,
                                    toLocationName = targetLocation.name,
                                    direction = directionName
                                ))

                                // Also send to the dragger and target directly (they may have left the session)
                                sendToPlayer(actor.id, PlayerDraggedMessage(
                                    sessionId = session.id,
                                    draggerId = actor.id,
                                    draggerName = actor.name,
                                    targetId = currentTarget.id,
                                    targetName = currentTarget.name,
                                    fromLocationId = session.locationId,
                                    toLocationId = targetLocation.id,
                                    toLocationName = targetLocation.name,
                                    direction = directionName
                                ))

                                log.info("${actor.name} dragged ${currentTarget.name} ${directionName} to ${targetLocation.name}")
                            }
                        }
                    }
                }

                // Broadcast status effect applications
                if (result.appliedEffects.isNotEmpty()) {
                    val effectTargetId = if (ability.targetType == "self" || currentTarget == null) {
                        actor.id
                    } else {
                        currentTarget.id
                    }
                    for (effect in result.appliedEffects) {
                        broadcastToSession(session.id, StatusEffectMessage(
                            sessionId = session.id,
                            combatantId = effectTargetId,
                            effect = effect,
                            applied = true
                        ))
                    }
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
     * Uses CombatRng for hit/miss, damage variance, and critical hits.
     */
    private fun executeAbility(
        actor: Combatant,
        ability: Ability,
        target: Combatant?,
        allCombatants: List<Combatant>
    ): ActionResult {
        // Parse effects from JSON to determine ability behavior
        val parsedEffects = parseAbilityEffects(ability.effects, actor.id, ability.durationRounds)

        // Determine if this is a healing ability
        val isHealingAbility = parsedEffects.any { it.effectType == "heal" || it.effectType == "hot" }

        var damage = 0
        var healing = 0
        var hitResult: CombatRng.HitResult = CombatRng.HitResult.HIT
        var wasCritical = false
        var wasGlancing = false

        // Calculate damage using RNG system with dice notation
        // Priority: ability.damageDice > actor.damageDice > ability.baseDamage > actor.baseDamage
        val effectiveDamageDice = ability.damageDice ?: actor.damageDice
        val effectiveBaseDamage = if (ability.baseDamage > 0) ability.baseDamage else actor.baseDamage
        val hasDamage = effectiveDamageDice != null || effectiveBaseDamage > 0

        if (!isHealingAbility && hasDamage && target != null) {
            val attackResult = CombatRng.rollAttackWithDice(
                damageDice = effectiveDamageDice,
                baseDamage = effectiveBaseDamage,
                attackerAccuracy = actor.accuracy,
                defenderEvasion = target.evasion,
                attackerLevel = actor.level,
                defenderLevel = target.level,
                critBonus = actor.critBonus
            )
            damage = attackResult.damage
            hitResult = attackResult.hitResult
            wasCritical = attackResult.wasCritical
            wasGlancing = attackResult.wasGlancing

            val diceInfo = if (effectiveDamageDice != null) " dice=$effectiveDamageDice," else ""
            log.info(">>> ATTACK: ${actor.name} vs ${target.name} (HP: ${target.currentHp}/${target.maxHp}) -$diceInfo " +
                "baseDamage=$effectiveBaseDamage, " +
                "hitRoll=${attackResult.rollDetails.hitRoll}/${attackResult.rollDetails.hitChance}, " +
                "result=$hitResult, damage=$damage" +
                (if (wasCritical) " CRITICAL!" else "") +
                (if (wasGlancing) " (glancing)" else ""))
        }

        // Calculate healing using RNG system
        if (isHealingAbility && ability.baseDamage > 0) {
            val (healAmount, isCrit) = CombatRng.rollHealing(
                baseHealing = ability.baseDamage,
                critBonus = actor.critBonus
            )
            healing = healAmount
            wasCritical = isCrit

            log.debug("Healing roll: ${actor.name} heals for $healing" +
                (if (wasCritical) " CRITICAL!" else ""))
        }

        // Handle special effect types that bypass normal damage/healing
        val isAidAbility = parsedEffects.any { it.effectType == "aid" }
        val isDragAbility = parsedEffects.any { it.effectType == "drag" }

        // Aid: bring downed ally to 1 HP
        if (isAidAbility && target != null && target.isDowned) {
            // Calculate healing needed to bring from current HP to 1
            healing = 1 - target.currentHp  // e.g., if at -5 HP, heal 6 to reach 1
            log.debug("${actor.name} uses Aid on ${target.name}, healing $healing to bring to 1 HP")
        }

        // Filter effects to return (exclude instant heal since we handle it via healing amount)
        // Only apply effects if the attack hit (not a miss)
        val appliedEffects = if (hitResult != CombatRng.HitResult.MISS) {
            parsedEffects.filter { effect ->
                when (effect.effectType) {
                    "heal" -> false // Instant heal is handled by healing amount
                    "aid" -> false  // Aid is handled specially above
                    "drag" -> false // Drag is handled in applyActionResult
                    "hot", "dot", "buff", "debuff", "stun", "root", "slow" -> true
                    else -> false
                }
            }
        } else {
            emptyList() // No effects on miss
        }

        // Build result message with RNG details
        val message = buildResultMessage(actor, target, ability, damage, healing, appliedEffects, hitResult, wasCritical, wasGlancing)

        return ActionResult(
            actionId = "${actor.id}-${ability.id}",
            success = hitResult != CombatRng.HitResult.MISS,
            damage = damage,
            healing = healing,
            appliedEffects = appliedEffects,
            message = message,
            hitResult = hitResult.name.lowercase(),
            wasCritical = wasCritical,
            wasGlancing = wasGlancing
        )
    }

    /**
     * Parse ability effects JSON into StatusEffect objects.
     *
     * Effects JSON format examples:
     * - Simple: "[\"heal\"]" or "[\"stun\"]"
     * - Complex: "[{\"type\":\"dot\",\"value\":5},{\"type\":\"debuff\",\"stat\":\"damage\",\"value\":-3}]"
     */
    private fun parseAbilityEffects(effectsJson: String, sourceId: String, durationRounds: Int): List<StatusEffect> {
        if (effectsJson.isBlank() || effectsJson == "[]") return emptyList()

        val effects = mutableListOf<StatusEffect>()
        val duration = if (durationRounds > 0) durationRounds else 3 // Default 3 rounds

        try {
            val jsonArray = json.decodeFromString<JsonArray>(effectsJson)

            for (element in jsonArray) {
                // Handle string elements like "heal", "stun"
                if (element is kotlinx.serialization.json.JsonPrimitive && element.isString) {
                    val effectType = element.content.lowercase()
                    effects.add(StatusEffect(
                        name = effectType.replaceFirstChar { it.uppercase() },
                        effectType = effectType,
                        value = getDefaultEffectValue(effectType),
                        remainingRounds = duration,
                        sourceId = sourceId
                    ))
                }
                // Handle object elements with type, value, etc.
                else if (element is kotlinx.serialization.json.JsonObject) {
                    val obj = element.jsonObject
                    val effectType = obj["type"]?.jsonPrimitive?.content?.lowercase() ?: continue
                    val value = obj["value"]?.jsonPrimitive?.intOrNull ?: getDefaultEffectValue(effectType)
                    val stat = obj["stat"]?.jsonPrimitive?.content

                    effects.add(StatusEffect(
                        name = buildEffectName(effectType, stat),
                        effectType = effectType,
                        value = value,
                        remainingRounds = duration,
                        sourceId = sourceId
                    ))
                }
            }
        } catch (e: Exception) {
            // Fallback: check for simple string contains
            log.debug("Failed to parse effects JSON, using fallback: ${e.message}")

            if (effectsJson.contains("heal", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Heal",
                    effectType = "heal",
                    value = 0,
                    remainingRounds = 1,
                    sourceId = sourceId
                ))
            }
            if (effectsJson.contains("stun", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Stunned",
                    effectType = "stun",
                    value = 0,
                    remainingRounds = duration,
                    sourceId = sourceId
                ))
            }
            if (effectsJson.contains("buff", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Empowered",
                    effectType = "buff",
                    value = 3,
                    remainingRounds = duration,
                    sourceId = sourceId
                ))
            }
            if (effectsJson.contains("debuff", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Weakened",
                    effectType = "debuff",
                    value = -3,
                    remainingRounds = duration,
                    sourceId = sourceId
                ))
            }
            if (effectsJson.contains("dot", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Burning",
                    effectType = "dot",
                    value = 5,
                    remainingRounds = duration,
                    sourceId = sourceId
                ))
            }
            if (effectsJson.contains("hot", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Regenerating",
                    effectType = "hot",
                    value = 5,
                    remainingRounds = duration,
                    sourceId = sourceId
                ))
            }
            if (effectsJson.contains("shield", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Arcane Shield",
                    effectType = "shield",
                    value = 20,  // 20 points of damage absorption
                    remainingRounds = duration,
                    sourceId = sourceId
                ))
            }
            if (effectsJson.contains("reflect", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Thorns",
                    effectType = "reflect",
                    value = 30,  // 30% damage reflected
                    remainingRounds = duration,
                    sourceId = sourceId
                ))
            }
            if (effectsJson.contains("lifesteal", ignoreCase = true)) {
                effects.add(StatusEffect(
                    name = "Vampiric Touch",
                    effectType = "lifesteal",
                    value = 25,  // 25% lifesteal
                    remainingRounds = duration,
                    sourceId = sourceId
                ))
            }
        }

        return effects
    }

    /**
     * Get default value for effect types.
     */
    private fun getDefaultEffectValue(effectType: String): Int {
        return when (effectType) {
            "dot" -> 5       // 5 damage per round
            "hot" -> 5       // 5 healing per round
            "buff" -> 3      // +3 to affected stat
            "debuff" -> -3   // -3 to affected stat
            "slow" -> -2     // -2 initiative
            "shield" -> 20   // 20 damage absorption
            "reflect" -> 30  // 30% damage reflected
            "lifesteal" -> 25 // 25% lifesteal
            else -> 0
        }
    }

    /**
     * Build a descriptive name for an effect.
     */
    private fun buildEffectName(effectType: String, stat: String?): String {
        return when (effectType) {
            "dot" -> "Burning"
            "hot" -> "Regenerating"
            "buff" -> if (stat != null) "${stat.replaceFirstChar { it.uppercase() }} Boost" else "Empowered"
            "debuff" -> if (stat != null) "${stat.replaceFirstChar { it.uppercase() }} Weakness" else "Weakened"
            "stun" -> "Stunned"
            "root" -> "Rooted"
            "slow" -> "Slowed"
            "shield" -> "Arcane Shield"
            "reflect" -> "Thorns"
            "lifesteal" -> "Vampiric Touch"
            else -> effectType.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Build a descriptive message for the action result, including RNG outcomes.
     */
    private fun buildResultMessage(
        actor: Combatant,
        target: Combatant?,
        ability: Ability,
        damage: Int,
        healing: Int,
        effects: List<StatusEffect>,
        hitResult: CombatRng.HitResult = CombatRng.HitResult.HIT,
        wasCritical: Boolean = false,
        wasGlancing: Boolean = false
    ): String {
        val parts = mutableListOf<String>()

        when {
            hitResult == CombatRng.HitResult.MISS && target != null -> {
                parts.add("${actor.name}'s ${ability.name} misses ${target.name}!")
            }
            wasCritical && damage > 0 && target != null -> {
                parts.add("CRITICAL! ${actor.name} devastates ${target.name} with ${ability.name} for $damage damage!")
            }
            wasGlancing && damage > 0 && target != null -> {
                parts.add("${actor.name}'s ${ability.name} grazes ${target.name} for $damage damage.")
            }
            damage > 0 && target != null -> {
                parts.add("${actor.name} hits ${target.name} with ${ability.name} for $damage damage!")
            }
            wasCritical && healing > 0 -> {
                val healTarget = if (target != null && ability.targetType != "self") target.name else actor.name
                parts.add("CRITICAL! ${actor.name} heals $healTarget for $healing with ${ability.name}!")
            }
            healing > 0 -> {
                val healTarget = if (target != null && ability.targetType != "self") target.name else actor.name
                parts.add("${actor.name} heals $healTarget for $healing with ${ability.name}!")
            }
            effects.isEmpty() -> {
                parts.add("${actor.name} uses ${ability.name}!")
            }
        }

        // Add effect application messages
        for (effect in effects) {
            val targetName = target?.name ?: actor.name
            when (effect.effectType) {
                "stun" -> parts.add("$targetName is stunned!")
                "root" -> parts.add("$targetName is rooted in place!")
                "slow" -> parts.add("$targetName is slowed!")
                "dot" -> parts.add("$targetName is afflicted with ${effect.name}!")
                "hot" -> parts.add("$targetName gains ${effect.name}!")
                "buff" -> parts.add("$targetName gains ${effect.name}!")
                "debuff" -> parts.add("$targetName is afflicted with ${effect.name}!")
                "shield" -> parts.add("$targetName is protected by a ${effect.value}-point shield!")
                "reflect" -> parts.add("$targetName is surrounded by a reflective barrier (${effect.value}%)!")
                "lifesteal" -> parts.add("$targetName gains lifesteal (${effect.value}%)!")
            }
        }

        return parts.joinToString(" ")
    }

    /**
     * Apply action result to combatants, handling shield absorption, reflect, and lifesteal.
     */
    private fun applyActionResult(
        combatants: List<Combatant>,
        action: CombatAction,
        result: ActionResult
    ): List<Combatant> {
        // First pass: calculate damage after shield absorption and track reflect/lifesteal
        var actualDamage = result.damage
        var reflectDamage = 0
        var lifestealHealing = 0
        var updatedTargetEffects: List<StatusEffect>? = null

        // Find target to check for defensive effects
        val target = combatants.find { it.id == action.targetId }
        val attacker = combatants.find { it.id == action.combatantId }

        if (target != null && actualDamage > 0) {
            // Process shield absorption
            val shieldEffect = target.statusEffects.find { it.effectType == "shield" }
            if (shieldEffect != null) {
                val absorbed = minOf(shieldEffect.value, actualDamage)
                actualDamage -= absorbed
                val remainingShield = shieldEffect.value - absorbed

                // Update or remove shield effect
                updatedTargetEffects = if (remainingShield > 0) {
                    target.statusEffects.map {
                        if (it.id == shieldEffect.id) it.copy(value = remainingShield)
                        else it
                    }
                } else {
                    target.statusEffects.filter { it.id != shieldEffect.id }
                }

                log.debug("Shield absorbed $absorbed damage, $remainingShield shield remaining")
            }

            // Apply armor damage reduction (flat reduction, after shield)
            if (actualDamage > 0 && target.armor > 0) {
                val armorReduction = minOf(target.armor, actualDamage - 1) // Always deal at least 1 damage
                actualDamage -= armorReduction
                log.debug("Armor reduced damage by $armorReduction (${target.armor} armor)")
            }

            // Process reflect (applies to original damage, not post-shield)
            val reflectEffect = target.statusEffects.find { it.effectType == "reflect" }
            if (reflectEffect != null && result.damage > 0) {
                reflectDamage = (result.damage * reflectEffect.value / 100)
                log.debug("Reflect: ${reflectEffect.value}% of ${result.damage} = $reflectDamage reflected")
            }

            // Process attacker's lifesteal
            if (attacker != null && actualDamage > 0) {
                val lifestealEffect = attacker.statusEffects.find { it.effectType == "lifesteal" }
                if (lifestealEffect != null) {
                    lifestealHealing = (actualDamage * lifestealEffect.value / 100)
                    log.debug("Lifesteal: ${lifestealEffect.value}% of $actualDamage = $lifestealHealing healed")
                }
            }
        }

        // Second pass: apply all changes
        return combatants.map { combatant ->
            var updated = combatant

            // Apply damage to target (after shield absorption)
            if (combatant.id == action.targetId && actualDamage > 0) {
                val (damaged, becameDowned, _) = applyDamageWithDownedState(updated, actualDamage)
                updated = damaged
                // Apply updated shield effects if shield was used
                if (updatedTargetEffects != null) {
                    updated = updated.copy(statusEffects = updatedTargetEffects)
                }
                // Note: PlayerDowned message will be broadcast after round processing
            }

            // Apply reflect damage back to attacker
            if (combatant.id == action.combatantId && reflectDamage > 0) {
                val (damaged, _, _) = applyDamageWithDownedState(updated, reflectDamage)
                updated = damaged
                log.debug("${combatant.name} takes $reflectDamage reflected damage")
            }

            // Apply lifesteal healing to attacker
            if (combatant.id == action.combatantId && lifestealHealing > 0) {
                val (healed, _) = applyHealingWithDownedState(updated, lifestealHealing)
                updated = healed
                log.debug("${combatant.name} heals $lifestealHealing from lifesteal")
            }

            // Apply healing to actor (self-heal) or target
            if (combatant.id == action.combatantId && result.healing > 0 && action.targetId == null) {
                val (healed, _) = applyHealingWithDownedState(updated, result.healing)
                updated = healed
            } else if (combatant.id == action.targetId && result.healing > 0) {
                val (healed, _) = applyHealingWithDownedState(updated, result.healing)
                updated = healed
            }

            // Apply status effects to target (or self for buffs)
            if (result.appliedEffects.isNotEmpty()) {
                val ability = AbilityRepository.findById(action.abilityId)
                val isSelfTarget = ability?.targetType == "self" || action.targetId == null

                // For self-targeting abilities, apply to actor; otherwise apply to target
                val shouldApplyEffects = if (isSelfTarget) {
                    combatant.id == action.combatantId
                } else {
                    combatant.id == action.targetId
                }

                if (shouldApplyEffects) {
                    // Add new effects, replacing any existing effects with same name
                    val existingEffectNames = result.appliedEffects.map { it.name }.toSet()
                    val filteredExisting = updated.statusEffects.filter { it.name !in existingEffectNames }
                    updated = updated.copy(
                        statusEffects = filteredExisting + result.appliedEffects
                    )
                }
            }

            // Apply cooldown to actor
            if (combatant.id == action.combatantId) {
                val ability = AbilityRepository.findById(action.abilityId)
                val cooldownRounds = ability?.cooldownRounds ?: 0
                if (cooldownRounds > 0) {
                    updated = updated.copy(cooldowns = updated.cooldowns + (action.abilityId to cooldownRounds))
                }
            }

            updated
        }
    }

    /**
     * Process status effects, bleeding damage, and resource regeneration for all combatants.
     */
    private suspend fun processStatusEffects(
        combatants: List<Combatant>,
        sessionId: String
    ): List<Combatant> {
        return combatants.map { combatant ->
            var hp = combatant.currentHp
            var mana = combatant.currentMana
            var stamina = combatant.currentStamina
            val remainingEffects = mutableListOf<StatusEffect>()
            var wasDowned = combatant.isDowned

            // Apply bleeding damage for downed players (-2 HP per round)
            if (combatant.type == CombatantType.PLAYER && combatant.isDowned) {
                val bleedingDamage = 2
                hp -= bleedingDamage
                log.debug("${combatant.name} loses $bleedingDamage HP from bleeding (now at $hp, death at ${combatant.deathThreshold})")
                broadcastToSession(sessionId, HealthUpdateMessage(
                    sessionId = sessionId,
                    combatantId = combatant.id,
                    combatantName = combatant.name,
                    currentHp = hp,
                    maxHp = combatant.maxHp,
                    changeAmount = bleedingDamage,
                    sourceId = combatant.id,
                    sourceName = "Bleeding"
                ))
            }

            for (effect in combatant.statusEffects) {
                // Apply effect
                when (effect.effectType) {
                    "dot" -> {
                        // For players, allow HP to go negative (to death threshold)
                        hp = if (combatant.type == CombatantType.PLAYER) {
                            hp - effect.value
                        } else {
                            (hp - effect.value).coerceAtLeast(0)
                        }
                        broadcastToSession(sessionId, HealthUpdateMessage(
                            sessionId = sessionId,
                            combatantId = combatant.id,
                            combatantName = combatant.name,
                            currentHp = hp,
                            maxHp = combatant.maxHp,
                            changeAmount = effect.value,
                            sourceId = effect.sourceId,
                            sourceName = "Damage over time"
                        ))
                    }
                    "hot" -> {
                        val oldHp = hp
                        hp = (hp + effect.value).coerceAtMost(combatant.maxHp)
                        broadcastToSession(sessionId, HealthUpdateMessage(
                            sessionId = sessionId,
                            combatantId = combatant.id,
                            combatantName = combatant.name,
                            currentHp = hp,
                            maxHp = combatant.maxHp,
                            changeAmount = -(hp - oldHp),  // Negative = healing
                            sourceId = effect.sourceId,
                            sourceName = "Heal over time"
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

            // Resource regeneration for players (stat-based)
            if (combatant.type == CombatantType.PLAYER && combatant.isAlive) {
                val oldHp = hp
                val oldMana = mana
                val oldStamina = stamina

                // HP regen based on CON (only if positive CON modifier)
                if (combatant.hpRegen > 0 && hp < combatant.maxHp) {
                    hp = (hp + combatant.hpRegen).coerceAtMost(combatant.maxHp)
                }

                // Mana regen based on INT/WIS
                mana = (mana + combatant.manaRegen).coerceAtMost(combatant.maxMana)

                // Stamina regen based on CON
                stamina = (stamina + combatant.staminaRegen).coerceAtMost(combatant.maxStamina)

                val hpChange = hp - oldHp
                val manaChange = mana - oldMana
                val staminaChange = stamina - oldStamina

                // Sync to User record immediately
                if (hpChange > 0 || manaChange > 0 || staminaChange > 0) {
                    if (hpChange > 0) UserRepository.heal(combatant.id, hpChange)
                    if (manaChange > 0) UserRepository.restoreMana(combatant.id, manaChange)
                    if (staminaChange > 0) UserRepository.restoreStamina(combatant.id, staminaChange)

                    // Notify clients of resource change
                    broadcastToSession(sessionId, ResourceUpdateMessage(
                        sessionId = sessionId,
                        combatantId = combatant.id,
                        currentHp = hp,
                        maxHp = combatant.maxHp,
                        currentMana = mana,
                        maxMana = combatant.maxMana,
                        currentStamina = stamina,
                        maxStamina = combatant.maxStamina,
                        hpChange = hpChange,
                        manaChange = manaChange,
                        staminaChange = staminaChange
                    ))
                }
            }

            // Calculate alive/downed state based on final HP
            val (isAlive, isDowned) = calculateAliveState(combatant, hp)

            // Check for state transitions and broadcast appropriate messages
            if (combatant.type == CombatantType.PLAYER) {
                // Player just became downed (wasn't downed before, now is)
                if (!wasDowned && isDowned) {
                    broadcastToSession(sessionId, PlayerDownedMessage(
                        sessionId = sessionId,
                        playerId = combatant.id,
                        playerName = combatant.name,
                        currentHp = hp,
                        deathThreshold = combatant.deathThreshold,
                        locationId = "" // Will be filled by caller if needed
                    ))
                }

                // Player stabilized (was downed, now HP > 0)
                if (wasDowned && !isDowned && hp > 0) {
                    broadcastToSession(sessionId, PlayerStabilizedMessage(
                        sessionId = sessionId,
                        playerId = combatant.id,
                        playerName = combatant.name,
                        currentHp = hp,
                        healerId = null,
                        healerName = null
                    ))
                }
            }

            combatant.copy(
                currentHp = hp,
                currentMana = mana,
                currentStamina = stamina,
                isAlive = isAlive,
                isDowned = isDowned,
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
     * Simple AI for creature actions - creatures attack players using RNG system.
     * Returns updated combatants list with damage applied.
     */
    private suspend fun processCreatureAI(
        combatants: List<Combatant>,
        session: CombatSession
    ): List<Combatant> {
        val alivePlayers = combatants.filter { it.type == CombatantType.PLAYER && it.isAlive }
        if (alivePlayers.isEmpty()) return combatants

        var updatedCombatants = combatants.toMutableList()

        // Get the current location to verify creatures are still there
        val currentLocation = LocationRepository.findById(session.locationId)
        val creaturesAtLocation = currentLocation?.creatureIds?.toSet() ?: emptySet()

        for (creature in combatants.filter { it.type == CombatantType.CREATURE && it.isAlive }) {
            // Skip creatures that have wandered away from this location
            if (creature.id !in creaturesAtLocation) {
                log.debug("${creature.name} is no longer at location ${session.locationId}, removing from combat")
                // Log creature removal
                CombatEventLogger.logEvent(
                    session = session,
                    eventType = CombatEventType.CREATURE_REMOVED,
                    message = "${creature.name} wandered away from combat",
                    actor = creature
                )
                // Mark creature as not alive so it's removed from combat
                val creatureIndex = updatedCombatants.indexOfFirst { it.id == creature.id }
                if (creatureIndex >= 0) {
                    updatedCombatants[creatureIndex] = creature.copy(isAlive = false)
                }
                continue
            }

            // Check if creature is stunned
            if (creature.statusEffects.any { it.effectType == "stun" }) {
                log.debug("${creature.name} is stunned and cannot act")
                continue
            }

            // Find a target (prefer standing players over downed ones)
            val currentAlivePlayers = updatedCombatants.filter { it.type == CombatantType.PLAYER && it.isAlive }
            if (currentAlivePlayers.isEmpty()) break

            // Prefer standing players, but attack downed players if no one else
            val standingPlayers = currentAlivePlayers.filter { !it.isDowned }
            val target = if (standingPlayers.isNotEmpty()) {
                standingPlayers.random()
            } else {
                currentAlivePlayers.random()  // All players are downed, finish them off
            }
            val currentTarget = updatedCombatants.find { it.id == target.id } ?: continue

            // Use RNG system for creature attacks (with dice if available)
            val attackResult = CombatRng.rollAttackWithDice(
                damageDice = creature.damageDice,
                baseDamage = creature.baseDamage,
                attackerAccuracy = creature.accuracy,
                defenderEvasion = currentTarget.evasion,
                attackerLevel = creature.level,
                defenderLevel = currentTarget.level,
                critBonus = creature.critBonus
            )

            // Build attack message
            val message = when {
                attackResult.hitResult == CombatRng.HitResult.MISS -> {
                    "${creature.name}'s attack misses ${currentTarget.name}!"
                }
                attackResult.wasCritical -> {
                    "CRITICAL! ${creature.name} devastates ${currentTarget.name} for ${attackResult.damage} damage!"
                }
                attackResult.wasGlancing -> {
                    "${creature.name}'s attack grazes ${currentTarget.name} for ${attackResult.damage} damage."
                }
                else -> {
                    "${creature.name} attacks ${currentTarget.name} for ${attackResult.damage} damage!"
                }
            }

            log.debug("Creature attack: ${creature.name} vs ${currentTarget.name} - " +
                "hitRoll=${attackResult.rollDetails.hitRoll}/${attackResult.rollDetails.hitChance}, " +
                "result=${attackResult.hitResult}, damage=${attackResult.damage}" +
                (if (attackResult.wasCritical) " CRITICAL!" else "") +
                (if (attackResult.wasGlancing) " (glancing)" else ""))

            // Log the creature attack with full stats
            CombatEventLogger.logEvent(
                session = session,
                eventType = CombatEventType.CREATURE_ATTACK,
                message = message,
                actor = creature,
                target = currentTarget,
                eventData = mapOf(
                    "hitRoll" to attackResult.rollDetails.hitRoll,
                    "hitChance" to attackResult.rollDetails.hitChance,
                    "critRoll" to attackResult.rollDetails.critRoll,
                    "critChance" to attackResult.rollDetails.critChance,
                    "hitResult" to attackResult.hitResult.name,
                    "baseDamage" to attackResult.rollDetails.baseDamage,
                    "finalDamage" to attackResult.damage,
                    "wasCritical" to attackResult.wasCritical,
                    "wasGlancing" to attackResult.wasGlancing
                )
            )

            // Apply damage if hit, handling shield/reflect/lifesteal
            if (attackResult.damage > 0) {
                val targetIndex = updatedCombatants.indexOfFirst { it.id == target.id }
                val creatureIndex = updatedCombatants.indexOfFirst { it.id == creature.id }
                if (targetIndex >= 0) {
                    var actualDamage = attackResult.damage
                    var reflectDamage = 0
                    var lifestealHealing = 0
                    var targetEffects = currentTarget.statusEffects

                    // Process shield absorption
                    val shieldEffect = currentTarget.statusEffects.find { it.effectType == "shield" }
                    if (shieldEffect != null) {
                        val absorbed = minOf(shieldEffect.value, actualDamage)
                        actualDamage -= absorbed
                        val remainingShield = shieldEffect.value - absorbed
                        targetEffects = if (remainingShield > 0) {
                            targetEffects.map { if (it.id == shieldEffect.id) it.copy(value = remainingShield) else it }
                        } else {
                            targetEffects.filter { it.id != shieldEffect.id }
                        }
                        log.debug("Shield absorbed $absorbed damage from ${creature.name}'s attack")
                    }

                    // Process reflect
                    val reflectEffect = currentTarget.statusEffects.find { it.effectType == "reflect" }
                    if (reflectEffect != null) {
                        reflectDamage = (attackResult.damage * reflectEffect.value / 100)
                        log.debug("Reflect: ${reflectEffect.value}% of ${attackResult.damage} = $reflectDamage reflected to ${creature.name}")
                    }

                    // Process creature's lifesteal (if any)
                    val currentCreature = updatedCombatants.find { it.id == creature.id }
                    val lifestealEffect = currentCreature?.statusEffects?.find { it.effectType == "lifesteal" }
                    if (lifestealEffect != null && actualDamage > 0) {
                        lifestealHealing = (actualDamage * lifestealEffect.value / 100)
                    }

                    // Apply damage to target (handles player downed state)
                    val (damagedTarget, becameDowned, becameDead) = applyDamageWithDownedState(currentTarget, actualDamage)
                    updatedCombatants[targetIndex] = damagedTarget.copy(statusEffects = targetEffects)

                    // Broadcast PlayerDowned if player just went down
                    if (becameDowned && damagedTarget.type == CombatantType.PLAYER) {
                        broadcastToSession(session.id, PlayerDownedMessage(
                            sessionId = session.id,
                            playerId = damagedTarget.id,
                            playerName = damagedTarget.name,
                            currentHp = damagedTarget.currentHp,
                            deathThreshold = damagedTarget.deathThreshold,
                            locationId = session.locationId
                        ))
                    }

                    // Log if player died (for debugging - actual death handling happens at combat end)
                    if (becameDead && damagedTarget.type == CombatantType.PLAYER) {
                        log.info("DEATH: Player ${damagedTarget.name} died! HP=${damagedTarget.currentHp}, threshold=${damagedTarget.deathThreshold}")
                    }

                    // Apply reflect damage to creature (creatures don't have downed state)
                    if (reflectDamage > 0 && creatureIndex >= 0) {
                        val creatureToUpdate = updatedCombatants[creatureIndex]
                        val (damagedCreature, _, _) = applyDamageWithDownedState(creatureToUpdate, reflectDamage)
                        updatedCombatants[creatureIndex] = damagedCreature
                        log.debug("${creature.name} takes $reflectDamage reflected damage")
                    }

                    // Apply lifesteal healing to creature
                    if (lifestealHealing > 0 && creatureIndex >= 0) {
                        val creatureToUpdate = updatedCombatants[creatureIndex]
                        val (healedCreature, _) = applyHealingWithDownedState(creatureToUpdate, lifestealHealing)
                        updatedCombatants[creatureIndex] = healedCreature
                    }

                    // Broadcast the attack
                    broadcastToSession(session.id, HealthUpdateMessage(
                        sessionId = session.id,
                        combatantId = target.id,
                        combatantName = target.name,
                        currentHp = damagedTarget.currentHp,
                        maxHp = currentTarget.maxHp,
                        changeAmount = actualDamage,
                        sourceId = creature.id,
                        sourceName = creature.name
                    ))
                }
            }

            // Broadcast ability result for the creature attack
            broadcastToSession(session.id, AbilityResolvedMessage(
                sessionId = session.id,
                result = ActionResult(
                    actionId = "${creature.id}-basic-attack",
                    success = attackResult.hitResult != CombatRng.HitResult.MISS,
                    damage = attackResult.damage,
                    message = message,
                    hitResult = attackResult.hitResult.name.lowercase(),
                    wasCritical = attackResult.wasCritical,
                    wasGlancing = attackResult.wasGlancing
                ),
                actorName = creature.name,
                targetName = currentTarget.name,
                abilityName = "Attack"
            ))
        }

        return updatedCombatants
    }

    /**
     * Check if combat should end.
     */
    private fun checkEndConditions(session: CombatSession): CombatSession {
        val alivePlayers = session.alivePlayers
        val aliveCreatures = session.aliveCreatures

        // Debug: log all player states
        for (player in session.players) {
            log.info("CHECK_END: Player ${player.name}: HP=${player.currentHp}, isAlive=${player.isAlive}, isDowned=${player.isDowned}, deathThreshold=${player.deathThreshold}")
        }
        log.info("CHECK_END: alivePlayers.size=${alivePlayers.size}, aliveCreatures.size=${aliveCreatures.size}, session.creatures.size=${session.creatures.size}")

        val (state, endReason) = when {
            // No players left at all - combat ends
            session.players.isEmpty() ->
                CombatState.ENDED to CombatEndReason.ALL_PLAYERS_DEFEATED
            // All players dead/fled
            alivePlayers.isEmpty() ->
                CombatState.ENDED to CombatEndReason.ALL_PLAYERS_DEFEATED
            // All creatures dead (and there were creatures to fight)
            aliveCreatures.isEmpty() && session.creatures.isNotEmpty() ->
                CombatState.ENDED to CombatEndReason.ALL_ENEMIES_DEFEATED
            // No creatures at all - nothing to fight, end combat
            session.creatures.isEmpty() ->
                CombatState.ENDED to CombatEndReason.TIMEOUT
            // Max rounds reached
            session.currentRound >= CombatConfig.MAX_COMBAT_ROUNDS ->
                CombatState.ENDED to CombatEndReason.TIMEOUT
            else -> session.state to session.endReason
        }

        if (state == CombatState.ENDED) {
            log.info("CHECK_END: Combat ending with reason=$endReason")
        }

        return session.copy(state = state, endReason = endReason)
    }

    /**
     * Handle combat ending - cleanup, rewards, loot distribution.
     * XP is calculated individually per player based on their level vs creature CR.
     * Loot is distributed to all surviving players.
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

        // Handle player death: drop items and respawn at Home
        if (session.endReason == CombatEndReason.ALL_PLAYERS_DEFEATED) {
            handlePlayerDeath(session)
        }

        // Get defeated creatures for XP and loot calculation
        val defeatedCreatures = if (session.endReason == CombatEndReason.ALL_ENEMIES_DEFEATED) {
            session.creatures.mapNotNull { CreatureRepository.findById(it.id) }
        } else emptyList()

        // Remove dead creatures from the location and schedule respawns
        if (defeatedCreatures.isNotEmpty()) {
            val location = LocationRepository.findById(session.locationId)
            if (location != null) {
                val deadCreatureIds = defeatedCreatures.map { it.id }.toSet()
                val updatedCreatureIds = location.creatureIds.filter { it !in deadCreatureIds }
                val updatedLocation = location.copy(creatureIds = updatedCreatureIds)
                LocationRepository.update(updatedLocation)
                log.info("Removed ${deadCreatureIds.size} dead creature(s) from ${location.name}: ${defeatedCreatures.map { it.name }}")

                // Record deaths for respawn system
                val currentTick = GameTickService.getCurrentTick()
                for (creature in defeatedCreatures) {
                    CreatureRespawnService.recordCreatureDeath(session.locationId, creature.id, currentTick)
                }
            }
        }

        // Calculate total loot from all defeated creatures
        var totalGold = 0
        val droppedItems = mutableListOf<Item>()

        for (creature in defeatedCreatures) {
            // Roll gold drop
            if (creature.maxGoldDrop > creature.minGoldDrop) {
                totalGold += kotlin.random.Random.nextInt(creature.minGoldDrop, creature.maxGoldDrop + 1)
            } else {
                totalGold += creature.minGoldDrop
            }

            // Roll loot table
            creature.lootTableId?.let { lootTableId ->
                val lootTable = LootTableRepository.findById(lootTableId)
                lootTable?.entries?.forEach { entry ->
                    if (kotlin.random.Random.nextFloat() < entry.chance) {
                        val qty = if (entry.maxQty > entry.minQty) {
                            kotlin.random.Random.nextInt(entry.minQty, entry.maxQty + 1)
                        } else entry.minQty

                        repeat(qty) {
                            ItemRepository.findById(entry.itemId)?.let { item ->
                                droppedItems.add(item)
                            }
                        }
                    }
                }
            }
        }

        val lootResult = LootResult(
            goldEarned = totalGold,
            itemIds = droppedItems.map { it.id },
            itemNames = droppedItems.map { it.name }
        )

        // Award experience and loot to surviving players
        val xpResults = mutableMapOf<String, Int>()
        val survivingPlayers = session.alivePlayers

        // Split gold among surviving players (rounded up)
        val goldPerPlayer = if (survivingPlayers.isNotEmpty()) {
            (totalGold + survivingPlayers.size - 1) / survivingPlayers.size
        } else 0

        for (playerCombatant in survivingPlayers) {
            val user = UserRepository.findById(playerCombatant.id) ?: continue

            // Calculate XP from each creature individually based on player level
            val totalXp = defeatedCreatures.sumOf { creature ->
                ExperienceService.calculateCreatureXp(user.level, creature)
            }

            if (totalXp > 0) {
                val result = ExperienceService.awardXp(playerCombatant.id, totalXp)
                xpResults[playerCombatant.id] = result.xpAwarded

                if (result.leveledUp) {
                    log.info("Player ${user.name} leveled up to ${result.newLevel}!")
                }
            }

            // Award gold
            if (goldPerPlayer > 0) {
                UserRepository.addGold(playerCombatant.id, goldPerPlayer)
                log.info("Player ${user.name} received $goldPerPlayer gold")
            }

            // Award items (all players get copies of dropped items for now)
            if (droppedItems.isNotEmpty()) {
                UserRepository.addItems(playerCombatant.id, droppedItems.map { it.id })
                log.info("Player ${user.name} received ${droppedItems.size} item(s)")
            }

            // Clear combat state
            UserRepository.updateCombatState(playerCombatant.id, playerCombatant.currentHp, null)
            playerSessions.remove(playerCombatant.id)
        }

        // For broadcast, use average XP (or first player's XP if solo)
        val avgXp = if (xpResults.isNotEmpty()) xpResults.values.average().toInt() else 0

        // Mark defeated creatures in FeatureState for each surviving player
        // This is used for chest visibility (guardian must be defeated)
        if (session.endReason == CombatEndReason.ALL_ENEMIES_DEFEATED) {
            for (playerCombatant in survivingPlayers) {
                for (creature in defeatedCreatures) {
                    val defeatedKey = "defeated_${creature.id}"
                    FeatureStateRepository.setState(playerCombatant.id, defeatedKey, "true")
                    log.debug("Marked ${creature.name} as defeated for player ${playerCombatant.id}")
                }
            }
        }

        // Broadcast combat end with loot
        broadcastToSession(session.id, CombatEndedMessage(
            sessionId = session.id,
            reason = session.endReason!!,
            victors = victors,
            defeated = defeated,
            loot = lootResult,
            experienceGained = avgXp
        ))

        // Log combat end event
        CombatEventLogger.logEvent(
            session = session,
            eventType = CombatEventType.SESSION_ENDED,
            message = "Combat ended: ${session.endReason}",
            includeAllCombatants = true,
            eventData = mapOf(
                "endReason" to session.endReason!!.name,
                "victors" to victors,
                "defeated" to defeated,
                "goldDropped" to totalGold,
                "itemsDropped" to droppedItems.map { it.name },
                "xpAwarded" to avgXp
            )
        )

        log.info("Combat session ${session.id} ended: ${session.endReason}. " +
            "Loot distributed: ${totalGold}g, ${droppedItems.size} items")
    }

    /**
     * Handle player death: optionally drop items at death location and respawn at Tun du Lac.
     * Item/gold dropping is controlled by DeathConfig flags.
     */
    private suspend fun handlePlayerDeath(session: CombatSession) {
        // Respawn at Tun du Lac (the starting town)
        val respawnLocation = LocationRepository.findById(TunDuLacSeed.TUN_DU_LAC_OVERWORLD_ID)
            ?: LocationRepository.findByCoordinates(0, 0, "overworld")
        if (respawnLocation == null) {
            log.error("Cannot respawn players - Tun du Lac location not found!")
            return
        }

        for (playerCombatant in session.players) {
            val user = UserRepository.findById(playerCombatant.id) ?: continue

            // Get the player's death location
            val deathLocationId = user.currentLocationId
            val deathLocation = deathLocationId?.let { LocationRepository.findById(it) }

            var itemsDropped = 0
            var goldLost = 0

            // Only drop items if config flag is enabled
            if (DeathConfig.itemsDropOnDeath) {
                val droppedItemIds = user.itemIds.toList()
                if (droppedItemIds.isNotEmpty() && deathLocationId != null) {
                    LocationRepository.addItems(deathLocationId, droppedItemIds)
                    log.info("Player ${user.name} dropped ${droppedItemIds.size} items at location $deathLocationId")
                }
                // Clear inventory (also clears equipped items)
                UserRepository.clearInventory(playerCombatant.id)
                itemsDropped = droppedItemIds.size
            }

            // Only lose gold if config flag is enabled
            if (DeathConfig.goldLostOnDeath) {
                goldLost = user.gold
                if (goldLost > 0) {
                    UserRepository.addGold(playerCombatant.id, -goldLost)
                    log.info("Player ${user.name} lost $goldLost gold")
                }
            }

            // Respawn at Tun du Lac with full HP
            UserRepository.updateCurrentLocation(playerCombatant.id, respawnLocation.id)
            UserRepository.healToFull(playerCombatant.id)
            UserRepository.updateCombatState(playerCombatant.id, user.maxHp, null)
            playerSessions.remove(playerCombatant.id)

            // Notify the player about their death
            sendToPlayer(playerCombatant.id, PlayerDeathMessage(
                playerId = playerCombatant.id,
                playerName = user.name,
                deathLocationId = deathLocationId,
                deathLocationName = deathLocation?.name,
                respawnLocationId = respawnLocation.id,
                respawnLocationName = respawnLocation.name,
                itemsDropped = itemsDropped,
                goldLost = goldLost
            ))

            log.info("Player ${user.name} died and respawned at ${respawnLocation.name}")
        }
    }

    /**
     * Process creature wandering - move creatures to random adjacent locations.
     * Non-aggressive creatures always wander.
     * Aggressive CR1 creatures also wander (roaming mobs).
     * Aggressive CR2+ creatures (bosses) stay put.
     * Creatures wander every 3-5 ticks (~9-15 seconds) to make the world feel alive.
     */
    /**
     * Process creature wandering between locations.
     * Called by GameTickService on the global tick.
     */
    suspend fun processCreatureWandering() {
        val now = System.currentTimeMillis()

        // Only wander every 3 ticks (roughly every 9 seconds)
        if (now - lastWanderTick < CombatConfig.ROUND_DURATION_MS * 3) return
        lastWanderTick = now

        // Get all creatures that can wander:
        // - Non-aggressive creatures (always wander)
        // - Aggressive CR1 creatures (roaming mobs)
        // - NOT aggressive CR2+ creatures (bosses stay put)
        val creatures = CreatureRepository.findAll().filter { creature ->
            !creature.isAggressive || (creature.isAggressive && creature.challengeRating <= 1)
        }

        // Track which creatures have already moved this tick
        val movedCreatures = mutableSetOf<String>()

        for (creature in creatures) {
            // Skip if already moved this tick
            if (creature.id in movedCreatures) continue

            // Skip if creature is in active combat
            // Check both ACTIVE and WAITING states - creatures should never wander during combat
            val inCombat = sessions.values.any { session ->
                (session.state == CombatState.ACTIVE || session.state == CombatState.WAITING) &&
                session.combatants.any { it.type == CombatantType.CREATURE && it.id == creature.id }
            }
            if (inCombat) {
                log.debug("Skipping wander for ${creature.name} - in active combat")
                continue
            }

            // 50% chance to wander each cycle
            if (kotlin.random.Random.nextFloat() > 0.5f) continue

            // Find which location this creature is in (fresh from DB)
            val currentLocation = LocationRepository.findAll()
                .find { creature.id in it.creatureIds } ?: continue

            // Pick a random exit (excluding ENTER portals - creatures can't use those)
            val exits = currentLocation.exits.filter { it.direction != ExitDirection.ENTER }
            if (exits.isEmpty()) continue

            val randomExit = exits.random()
            val targetLocation = LocationRepository.findById(randomExit.locationId) ?: continue

            // Move creature: remove from current location, add to target (avoid duplicates)
            val updatedCurrentLocation = currentLocation.copy(
                creatureIds = currentLocation.creatureIds.filter { it != creature.id }
            )
            val updatedTargetLocation = targetLocation.copy(
                creatureIds = (targetLocation.creatureIds.filter { it != creature.id } + creature.id)
            )

            LocationRepository.update(updatedCurrentLocation)
            LocationRepository.update(updatedTargetLocation)
            movedCreatures.add(creature.id)

            // Broadcast movement only to players at source or destination locations
            val moveMessage = CreatureMovedMessage(
                creatureId = creature.id,
                creatureName = creature.name,
                fromLocationId = currentLocation.id,
                toLocationId = targetLocation.id,
                direction = randomExit.direction.name.lowercase().replace("_", "")  // e.g., "north", "southeast"
            )
            broadcastToPlayersAtLocations(moveMessage, setOf(currentLocation.id, targetLocation.id))

            log.info("${creature.name} wandered from ${currentLocation.name} to ${targetLocation.name}")
        }
    }

    /**
     * Broadcast a message to all connected players (not just those in combat).
     */
    private suspend fun broadcastToAllPlayers(message: ServerCombatMessage) {
        val jsonMessage = json.encodeToString(message)
        for ((userId, connection) in playerConnections) {
            try {
                connection.send(Frame.Text(jsonMessage))
            } catch (e: Exception) {
                log.debug("Failed to send broadcast to player $userId: ${e.message}")
            }
        }
    }

    /**
     * Broadcast a message to players at specific locations.
     * Used for location-specific events like creature movement.
     */
    private suspend fun broadcastToPlayersAtLocations(message: ServerCombatMessage, locationIds: Set<String>) {
        // Get all players at these locations
        val playersAtLocations = locationIds.flatMap { locationId ->
            UserRepository.findActiveUsersAtLocation(locationId)
        }.map { it.id }.toSet()

        val jsonMessage = json.encodeToString(message)
        for (userId in playersAtLocations) {
            val connection = playerConnections[userId] ?: continue
            try {
                connection.send(Frame.Text(jsonMessage))
            } catch (e: Exception) {
                log.debug("Failed to send location broadcast to player $userId: ${e.message}")
            }
        }
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
        val connection = playerConnections[userId]
        if (connection == null) {
            log.warn("Cannot send message to player $userId - no connection found")
            return
        }
        try {
            val jsonMessage = json.encodeToString(message)
            // Log CombatEndedMessage at INFO level for debugging
            if (message is CombatEndedMessage) {
                log.info(">>> SENDING CombatEndedMessage to $userId: $jsonMessage")
            } else {
                log.debug("Sending ${message::class.simpleName} to player $userId: ${jsonMessage.take(200)}")
            }
            connection.send(Frame.Text(jsonMessage))
        } catch (e: Exception) {
            log.error("Failed to send message to player $userId: ${e.message}")
        }
    }

    /**
     * Extension: Convert User to Combatant.
     * Derives combat stats from D&D attributes, level, and equipment bonuses.
     */
    private fun User.toCombatant(): Combatant {
        val classAbilities = characterClassId?.let {
            AbilityRepository.findByClassId(it).map { a -> a.id }
        } ?: emptyList()

        // Sum equipment bonuses from equipped items
        val equippedItems = equippedItemIds.mapNotNull { ItemRepository.findById(it) }
        val equipAttack = equippedItems.sumOf { it.statBonuses?.attack ?: 0 }
        val equipDefense = equippedItems.sumOf { it.statBonuses?.defense ?: 0 }
        val equipHpBonus = equippedItems.sumOf { it.statBonuses?.maxHp ?: 0 }

        // Calculate combat stats from D&D attributes + level + equipment
        val playerAccuracy = UserRepository.calculateAccuracy(this, equipAttack)
        val playerEvasion = UserRepository.calculateEvasion(this, equipDefense)
        val playerCritBonus = UserRepository.calculateCritBonus(this)
        val playerBaseDamage = UserRepository.calculateBaseDamage(this, equipAttack)

        // Calculate resource regeneration based on stats (MajorMUD style)
        val conMod = UserRepository.attributeModifier(constitution)
        val intMod = UserRepository.attributeModifier(intelligence)
        val wisMod = UserRepository.attributeModifier(wisdom)

        // HP regen: base 1 + CON modifier + level/3
        // Everyone gets at least 1 HP per round, CON and level add more
        val levelBonus = level / 3
        val hpRegenRate = (1 + conMod + levelBonus).coerceAtLeast(1)

        // Mana regen: base 1 + higher of INT or WIS modifier (for both arcane and divine casters)
        val spellMod = maxOf(intMod, wisMod)
        val manaRegenRate = (1 + spellMod).coerceAtLeast(1)

        // Stamina regen: base 2 + CON modifier (martial classes benefit from constitution)
        val staminaRegenRate = (2 + conMod).coerceAtLeast(1)

        // Get universal abilities (classId is null) that all players have
        val universalAbilities = AbilityRepository.findUniversal().map { it.id }

        // Death threshold: -(10 + CON * 2)
        // Higher CON = more negative threshold = harder to kill
        val playerDeathThreshold = -(10 + constitution * 2)

        // Apply equipment HP bonus to max HP (current HP stays as-is from DB)
        val effectiveMaxHp = maxHp + equipHpBonus

        return Combatant(
            id = id,
            type = CombatantType.PLAYER,
            name = name,
            maxHp = effectiveMaxHp,
            currentHp = currentHp,
            maxMana = maxMana,
            currentMana = currentMana,
            maxStamina = maxStamina,
            currentStamina = currentStamina,
            characterClassId = characterClassId,
            abilityIds = classAbilities + universalAbilities,
            initiative = CombatRng.rollD20() + UserRepository.attributeModifier(dexterity),
            level = level,
            accuracy = playerAccuracy,
            evasion = playerEvasion,
            critBonus = playerCritBonus,
            baseDamage = playerBaseDamage,
            armor = equipDefense,  // Equipment defense now provides damage reduction
            hpRegen = hpRegenRate,
            manaRegen = manaRegenRate,
            staminaRegen = staminaRegenRate,
            deathThreshold = playerDeathThreshold,
            constitution = constitution
        )
    }

    /**
     * Extension: Convert Creature to Combatant.
     * Includes combat stats for RNG calculations.
     */
    private fun Creature.toCombatant(): Combatant {
        // Creatures get stats based on level
        // Higher level creatures are harder to hit and hit more often
        val creatureAccuracy = level * 2 // +2 accuracy per level
        val creatureEvasion = level // +1 evasion per level
        val creatureCritBonus = level / 3 // +1 crit per 3 levels

        return Combatant(
            id = id,
            type = CombatantType.CREATURE,
            name = name,
            maxHp = maxHp,
            currentHp = maxHp, // Creatures start at full HP
            abilityIds = abilityIds,
            initiative = CombatRng.rollD20(),
            level = level,
            accuracy = creatureAccuracy,
            evasion = creatureEvasion,
            critBonus = creatureCritBonus,
            baseDamage = baseDamage,
            damageDice = damageDice
        )
    }

    /**
     * Calculate the alive/downed state for a combatant based on their HP.
     *
     * For players:
     * - HP > 0: alive, not downed
     * - HP <= 0 but > deathThreshold: alive (can be revived), downed
     * - HP <= deathThreshold: dead
     *
     * For creatures:
     * - HP > 0: alive
     * - HP <= 0: dead (creatures don't have a downed state)
     *
     * @return Pair(isAlive, isDowned)
     */
    private fun calculateAliveState(combatant: Combatant, newHp: Int): Pair<Boolean, Boolean> {
        return if (combatant.type == CombatantType.PLAYER) {
            val result = when {
                newHp > 0 -> Pair(true, false)  // Healthy
                newHp > combatant.deathThreshold -> Pair(true, true)  // Downed but not dead
                else -> Pair(false, false)  // Dead
            }
            // Debug: log state calculation for players with low HP
            if (newHp <= 0) {
                log.info("ALIVE_STATE: ${combatant.name} HP=$newHp, deathThreshold=${combatant.deathThreshold}, " +
                    "newHp>threshold=${newHp > combatant.deathThreshold}, result=(isAlive=${result.first}, isDowned=${result.second})")
            }
            result
        } else {
            // Creatures die at 0 HP, no downed state
            Pair(newHp > 0, false)
        }
    }

    /**
     * Apply damage to a combatant, handling the downed state for players.
     * Returns the updated combatant and whether the state changed to downed or dead.
     *
     * @return Triple(updatedCombatant, becameDowned, becameDead)
     */
    private fun applyDamageWithDownedState(
        combatant: Combatant,
        damage: Int
    ): Triple<Combatant, Boolean, Boolean> {
        val wasDowned = combatant.isDowned
        val wasAlive = combatant.isAlive

        // For players, HP can go negative (to deathThreshold)
        // For creatures, HP floors at 0
        val newHp = if (combatant.type == CombatantType.PLAYER) {
            combatant.currentHp - damage
        } else {
            (combatant.currentHp - damage).coerceAtLeast(0)
        }

        val (isAlive, isDowned) = calculateAliveState(combatant, newHp)

        val updated = combatant.copy(
            currentHp = newHp,
            isAlive = isAlive,
            isDowned = isDowned
        )

        val becameDowned = !wasDowned && isDowned
        val becameDead = wasAlive && !isAlive

        // Debug logging for damage tracking
        log.info(">>> DAMAGE APPLIED: ${combatant.name} took $damage damage: HP ${combatant.currentHp} -> $newHp, " +
            "isAlive=$isAlive, isDowned=$isDowned")
        if (combatant.type == CombatantType.PLAYER) {
            log.info("PLAYER DAMAGE: ${combatant.name} took $damage damage: HP ${combatant.currentHp} -> $newHp, " +
                "deathThreshold=${combatant.deathThreshold}, isAlive=$wasAlive->$isAlive, isDowned=$wasDowned->$isDowned" +
                (if (becameDowned) " [BECAME DOWNED]" else "") +
                (if (becameDead) " [DIED]" else ""))
        }

        return Triple(updated, becameDowned, becameDead)
    }

    /**
     * Apply healing to a combatant, handling recovery from downed state.
     * Returns the updated combatant and whether they recovered from downed.
     *
     * @return Pair(updatedCombatant, recoveredFromDowned)
     */
    private fun applyHealingWithDownedState(
        combatant: Combatant,
        healing: Int
    ): Pair<Combatant, Boolean> {
        val wasDowned = combatant.isDowned

        val newHp = (combatant.currentHp + healing).coerceAtMost(combatant.maxHp)
        val (isAlive, isDowned) = calculateAliveState(combatant, newHp)

        val updated = combatant.copy(
            currentHp = newHp,
            isAlive = isAlive,
            isDowned = isDowned
        )

        val recoveredFromDowned = wasDowned && !isDowned && newHp > 0

        return Pair(updated, recoveredFromDowned)
    }
}
