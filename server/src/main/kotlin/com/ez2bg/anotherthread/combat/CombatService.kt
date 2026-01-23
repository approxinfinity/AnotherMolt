package com.ez2bg.anotherthread.combat

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.experience.ExperienceService
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
                    processCreatureWandering()
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

            // Broadcast health updates for healing
            if (result.healing > 0) {
                // Determine who was healed - self or target
                val healTargetId = if (ability.targetType == "self" || action.targetId == null) {
                    actor.id
                } else {
                    action.targetId
                }
                val updatedHealTarget = currentCombatants.find { it.id == healTargetId }
                if (updatedHealTarget != null) {
                    broadcastToSession(session.id, HealthUpdateMessage(
                        sessionId = session.id,
                        combatantId = healTargetId,
                        currentHp = updatedHealTarget.currentHp,
                        maxHp = updatedHealTarget.maxHp,
                        changeAmount = -result.healing, // Negative = healing
                        sourceId = actor.id,
                        sourceName = actor.name
                    ))
                }
            }

            // Broadcast status effect applications
            if (result.appliedEffects.isNotEmpty()) {
                val effectTargetId = if (ability.targetType == "self" || action.targetId == null) {
                    actor.id
                } else {
                    action.targetId
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

        // Calculate damage using RNG system
        if (!isHealingAbility && ability.baseDamage > 0 && target != null) {
            val attackResult = CombatRng.rollAttack(
                baseDamage = ability.baseDamage,
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

            log.debug("Attack roll: ${actor.name} vs ${target.name} - " +
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

        // Filter effects to return (exclude instant heal since we handle it via healing amount)
        // Only apply effects if the attack hit (not a miss)
        val appliedEffects = if (hitResult != CombatRng.HitResult.MISS) {
            parsedEffects.filter { effect ->
                when (effect.effectType) {
                    "heal" -> false // Instant heal is handled by healing amount
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
        }

        return effects
    }

    /**
     * Get default value for effect types.
     */
    private fun getDefaultEffectValue(effectType: String): Int {
        return when (effectType) {
            "dot" -> 5      // 5 damage per round
            "hot" -> 5      // 5 healing per round
            "buff" -> 3     // +3 to affected stat
            "debuff" -> -3  // -3 to affected stat
            "slow" -> -2    // -2 initiative
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
            }
        }

        return parts.joinToString(" ")
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
            var updated = combatant

            // Apply damage to target
            if (combatant.id == action.targetId && result.damage > 0) {
                val newHp = (updated.currentHp - result.damage).coerceAtLeast(0)
                updated = updated.copy(
                    currentHp = newHp,
                    isAlive = newHp > 0
                )
            }

            // Apply healing to actor (self-heal) or target
            if (combatant.id == action.combatantId && result.healing > 0 && action.targetId == null) {
                val newHp = (updated.currentHp + result.healing).coerceAtMost(updated.maxHp)
                updated = updated.copy(currentHp = newHp)
            } else if (combatant.id == action.targetId && result.healing > 0) {
                val newHp = (updated.currentHp + result.healing).coerceAtMost(updated.maxHp)
                updated = updated.copy(currentHp = newHp)
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

        for (creature in combatants.filter { it.type == CombatantType.CREATURE && it.isAlive }) {
            // Check if creature is stunned
            if (creature.statusEffects.any { it.effectType == "stun" }) {
                log.debug("${creature.name} is stunned and cannot act")
                continue
            }

            // Find a target (random alive player from current state)
            val currentAlivePlayers = updatedCombatants.filter { it.type == CombatantType.PLAYER && it.isAlive }
            if (currentAlivePlayers.isEmpty()) break

            val target = currentAlivePlayers.random()
            val currentTarget = updatedCombatants.find { it.id == target.id } ?: continue

            // Use RNG system for creature attacks
            val attackResult = CombatRng.rollAttack(
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

            // Apply damage if hit
            if (attackResult.damage > 0) {
                val targetIndex = updatedCombatants.indexOfFirst { it.id == target.id }
                if (targetIndex >= 0) {
                    val newHp = (currentTarget.currentHp - attackResult.damage).coerceAtLeast(0)
                    updatedCombatants[targetIndex] = currentTarget.copy(
                        currentHp = newHp,
                        isAlive = newHp > 0
                    )

                    // Broadcast the attack
                    broadcastToSession(session.id, HealthUpdateMessage(
                        sessionId = session.id,
                        combatantId = target.id,
                        currentHp = newHp,
                        maxHp = currentTarget.maxHp,
                        changeAmount = attackResult.damage,
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
     * XP is calculated individually per player based on their level vs creature CR.
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

        // Get defeated creatures for XP calculation
        val defeatedCreatures = if (session.endReason == CombatEndReason.ALL_ENEMIES_DEFEATED) {
            session.creatures.mapNotNull { CreatureRepository.findById(it.id) }
        } else emptyList()

        // Award experience individually to surviving players based on their level vs creature CR
        val xpResults = mutableMapOf<String, Int>()
        for (playerCombatant in session.alivePlayers) {
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

            // Clear combat state
            UserRepository.updateCombatState(playerCombatant.id, playerCombatant.currentHp, null)
            playerSessions.remove(playerCombatant.id)
        }

        // For broadcast, use average XP (or first player's XP if solo)
        val avgXp = if (xpResults.isNotEmpty()) xpResults.values.average().toInt() else 0

        // Broadcast combat end
        broadcastToSession(session.id, CombatEndedMessage(
            sessionId = session.id,
            reason = session.endReason!!,
            victors = victors,
            defeated = defeated,
            experienceGained = avgXp
        ))

        log.info("Combat session ${session.id} ended: ${session.endReason}")
    }

    /**
     * Process creature wandering - move non-aggressive creatures to random adjacent locations.
     * Creatures wander every 3-5 ticks (~9-15 seconds) to make the world feel alive.
     */
    private suspend fun processCreatureWandering() {
        val now = System.currentTimeMillis()

        // Only wander every 3 ticks (roughly every 9 seconds)
        if (now - lastWanderTick < CombatConfig.ROUND_DURATION_MS * 3) return
        lastWanderTick = now

        // Get all non-aggressive creatures that could wander
        val creatures = CreatureRepository.findAll().filter { !it.isAggressive }

        // Track which creatures have already moved this tick
        val movedCreatures = mutableSetOf<String>()

        for (creature in creatures) {
            // Skip if already moved this tick
            if (creature.id in movedCreatures) continue

            // Skip if creature is in active combat
            val inCombat = sessions.values.any { session ->
                session.state != CombatState.ENDED &&
                session.creatures.any { it.id == creature.id }
            }
            if (inCombat) continue

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

            // Broadcast movement to all connected players
            val moveMessage = CreatureMovedMessage(
                creatureId = creature.id,
                creatureName = creature.name,
                fromLocationId = currentLocation.id,
                toLocationId = targetLocation.id
            )
            broadcastToAllPlayers(moveMessage)

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
     * Extension: Convert User to Combatant.
     * Includes combat stats from class and level for RNG calculations.
     */
    private fun User.toCombatant(): Combatant {
        val classAbilities = characterClassId?.let {
            AbilityRepository.findByClassId(it).map { a -> a.id }
        } ?: emptyList()

        // Get class data for base stats
        val characterClass = characterClassId?.let { CharacterClassRepository.findById(it) }

        // Calculate combat stats based on level and class
        // Players get +1 accuracy per 2 levels, +1 crit per 5 levels
        val playerAccuracy = level / 2
        val playerCritBonus = level / 5
        val playerBaseDamage = 5 + level // Base unarmed damage

        return Combatant(
            id = id,
            type = CombatantType.PLAYER,
            name = name,
            maxHp = maxHp,
            currentHp = currentHp,
            characterClassId = characterClassId,
            abilityIds = classAbilities,
            initiative = CombatRng.rollD20(), // D20 initiative roll
            level = level,
            accuracy = playerAccuracy,
            evasion = 0, // TODO: Could come from equipment/buffs
            critBonus = playerCritBonus,
            baseDamage = playerBaseDamage
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
            baseDamage = baseDamage
        )
    }
}
