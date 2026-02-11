package com.ez2bg.anotherthread.spell

import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Service for executing spells, particularly utility spells that operate outside of combat.
 * Combat spells are handled by CombatService, but this service can provide spell data to it.
 */
object SpellService {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Result of attempting to cast a spell.
     */
    @Serializable
    sealed class CastResult {
        @Serializable
        data class Success(
            val message: String,
            val newLocationId: String? = null,  // For movement spells
            val revealedInfo: RevealedInfo? = null,  // For detection spells
            val spellState: SpellStateInfo? = null  // Current state after casting
        ) : CastResult()

        @Serializable
        data class Failure(val reason: String) : CastResult()
    }

    @Serializable
    data class SpellStateInfo(
        val remainingCharges: Int? = null,
        val cooldownExpiresAt: Long? = null,
        val cooldownSecondsRemaining: Int? = null
    )

    @Serializable
    data class RevealedInfo(
        val hiddenExits: List<HiddenExitInfo> = emptyList(),
        val traps: List<TrapInfo> = emptyList(),
        val invisibleCreatures: List<String> = emptyList()
    )

    @Serializable
    data class HiddenExitInfo(
        val direction: String,
        val targetLocationId: String,
        val targetLocationName: String
    )

    @Serializable
    data class TrapInfo(
        val name: String,
        val description: String
    )

    /**
     * Cast a utility spell.
     *
     * @param userId The user casting the spell
     * @param featureId The feature ID of the spell to cast
     * @param targetParams Optional parameters for the spell (e.g., target location for teleport)
     * @return CastResult indicating success or failure
     */
    fun castUtilitySpell(
        userId: String,
        featureId: String,
        targetParams: Map<String, String> = emptyMap()
    ): CastResult {
        // Get user
        val user = UserRepository.findById(userId)
            ?: return CastResult.Failure("User not found")

        // Get the spell feature
        val feature = FeatureRepository.findById(featureId)
            ?: return CastResult.Failure("Spell not found")

        // Parse spell data
        val parseResult = SpellDataParser.parse(feature.data)
        if (parseResult !is SpellParseResult.Utility) {
            return CastResult.Failure("Feature is not a utility spell")
        }

        val spell = parseResult.spell

        // Check requirements
        val reqCheck = checkRequirements(user, spell.requirements)
        if (reqCheck != null) {
            return CastResult.Failure(reqCheck)
        }

        // Check if user is in combat (some spells can't be used in combat)
        if (user.currentCombatSessionId != null && spell.utility.params.interruptedByCombat == true) {
            return CastResult.Failure("Cannot cast this spell while in combat")
        }

        // Check cooldown and charges
        val cooldownCheck = checkCooldownAndCharges(userId, featureId, spell.cooldown)
        if (cooldownCheck != null) {
            return CastResult.Failure(cooldownCheck)
        }

        // TODO: Check and deduct mana/stamina/health cost

        // Execute the spell action
        val result = executeUtilityAction(user, spell.utility, targetParams)

        // If successful, apply cooldown/charge consumption
        if (result is CastResult.Success) {
            val newState = applyCooldownAndCharges(userId, featureId, spell.cooldown)
            return result.copy(spellState = newState)
        }

        return result
    }

    /**
     * Check if spell is on cooldown or out of charges.
     * Returns error message if cannot cast, null if OK.
     */
    private fun checkCooldownAndCharges(userId: String, featureId: String, cooldown: Cooldown): String? {
        if (cooldown.type == "none") return null

        val currentState = FeatureStateRepository.getSpellState(userId, featureId)
        val now = System.currentTimeMillis()

        when (cooldown.type) {
            "seconds" -> {
                val expiresAt = currentState?.cooldownExpiresAt
                if (expiresAt != null && now < expiresAt) {
                    val secondsLeft = ((expiresAt - now) / 1000).toInt()
                    return "Spell on cooldown ($secondsLeft seconds remaining)"
                }
            }
            "uses_per_day" -> {
                val remaining = currentState?.remainingCharges
                // If no state exists, they have full charges
                if (remaining != null && remaining <= 0) {
                    return "No charges remaining (resets daily)"
                }
            }
            "rounds" -> {
                // Round-based cooldowns are handled by CombatService
                // For utility spells outside combat, treat as a short seconds cooldown
                val expiresAt = currentState?.cooldownExpiresAt
                if (expiresAt != null && now < expiresAt) {
                    val secondsLeft = ((expiresAt - now) / 1000).toInt()
                    return "Spell on cooldown ($secondsLeft seconds remaining)"
                }
            }
        }

        return null
    }

    /**
     * Apply cooldown or consume a charge after successful cast.
     * Returns the new state info for the response.
     */
    private fun applyCooldownAndCharges(userId: String, featureId: String, cooldown: Cooldown): SpellStateInfo? {
        if (cooldown.type == "none") return null

        val now = System.currentTimeMillis()
        val currentState = FeatureStateRepository.getSpellState(userId, featureId)

        val newState = when (cooldown.type) {
            "seconds" -> {
                val expiresAt = now + (cooldown.value * 1000L)
                SpellState(
                    cooldownExpiresAt = expiresAt,
                    lastUsedAt = now,
                    timesUsed = (currentState?.timesUsed ?: 0) + 1
                )
            }
            "uses_per_day" -> {
                // If no state, initialize with max charges - 1
                val maxCharges = cooldown.value
                val currentCharges = currentState?.remainingCharges ?: maxCharges
                SpellState(
                    remainingCharges = (currentCharges - 1).coerceAtLeast(0),
                    lastUsedAt = now,
                    timesUsed = (currentState?.timesUsed ?: 0) + 1
                )
            }
            "rounds" -> {
                // Convert rounds to seconds (3 seconds per round)
                val expiresAt = now + (cooldown.value * 3000L)
                SpellState(
                    cooldownExpiresAt = expiresAt,
                    lastUsedAt = now,
                    timesUsed = (currentState?.timesUsed ?: 0) + 1
                )
            }
            else -> return null
        }

        FeatureStateRepository.updateSpellState(userId, "user", featureId, newState)

        return SpellStateInfo(
            remainingCharges = newState.remainingCharges,
            cooldownExpiresAt = newState.cooldownExpiresAt,
            cooldownSecondsRemaining = newState.cooldownExpiresAt?.let {
                ((it - now) / 1000).toInt().coerceAtLeast(0)
            }
        )
    }

    /**
     * Reset daily charges for a user's spells (call at day reset).
     */
    fun resetDailyCharges(userId: String) {
        val states = FeatureStateRepository.findAllByOwner(userId)
        states.forEach { featureState ->
            val spellState = try {
                json.decodeFromString<SpellState>(featureState.state)
            } catch (e: Exception) {
                return@forEach
            }

            // Only reset if this is a uses_per_day spell with charges tracked
            if (spellState.remainingCharges != null) {
                // Get the feature to find max charges
                val feature = FeatureRepository.findById(featureState.featureId) ?: return@forEach
                val parseResult = SpellDataParser.parse(feature.data)
                if (parseResult is SpellParseResult.Utility) {
                    val maxCharges = parseResult.spell.cooldown.value
                    val resetState = spellState.copy(remainingCharges = maxCharges)
                    FeatureStateRepository.updateSpellState(
                        userId, "user", featureState.featureId, resetState
                    )
                }
            }
        }
    }

    /**
     * Get the current state of a spell for a user.
     */
    fun getSpellState(userId: String, featureId: String): SpellStateInfo? {
        val state = FeatureStateRepository.getSpellState(userId, featureId) ?: return null
        val now = System.currentTimeMillis()

        return SpellStateInfo(
            remainingCharges = state.remainingCharges,
            cooldownExpiresAt = state.cooldownExpiresAt,
            cooldownSecondsRemaining = state.cooldownExpiresAt?.let {
                ((it - now) / 1000).toInt().coerceAtLeast(0)
            }
        )
    }

    /**
     * Get all utility spells available to a user (from their features, class, and items).
     */
    fun getAvailableUtilitySpells(userId: String): List<AvailableSpell> {
        val user = UserRepository.findById(userId) ?: return emptyList()
        val availableSpells = mutableListOf<AvailableSpell>()

        // Get spells from user's features
        user.featureIds.forEach { featureId ->
            val feature = FeatureRepository.findById(featureId)
            if (feature != null && SpellDataParser.parseSpellType(feature.data) == "utility") {
                val spell = SpellDataParser.parseUtilitySpell(feature.data)
                if (spell != null) {
                    availableSpells.add(AvailableSpell(
                        featureId = feature.id,
                        name = feature.name,
                        description = feature.description,
                        source = "character",
                        action = spell.utility.action,
                        cooldown = spell.cooldown,
                        cost = spell.cost
                    ))
                }
            }
        }

        // Get spells from user's class abilities (if they exist as features)
        // TODO: Once abilities are migrated to features, include class spells here

        // Get spells from user's equipped items
        user.itemIds.forEach { itemId ->
            val item = ItemRepository.findById(itemId)
            item?.featureIds?.forEach { featureId ->
                val feature = FeatureRepository.findById(featureId)
                if (feature != null && SpellDataParser.parseSpellType(feature.data) == "utility") {
                    val spell = SpellDataParser.parseUtilitySpell(feature.data)
                    if (spell != null) {
                        availableSpells.add(AvailableSpell(
                            featureId = feature.id,
                            name = feature.name,
                            description = feature.description,
                            source = "item:${item.name}",
                            action = spell.utility.action,
                            cooldown = spell.cooldown,
                            cost = spell.cost
                        ))
                    }
                }
            }
        }

        return availableSpells
    }

    @Serializable
    data class AvailableSpell(
        val featureId: String,
        val name: String,
        val description: String,
        val source: String,  // "character", "class", "item:ItemName"
        val action: String,
        val cooldown: Cooldown,
        val cost: SpellCost
    )

    private fun checkRequirements(user: User, requirements: SpellRequirements): String? {
        if (user.level < requirements.level) {
            return "Requires level ${requirements.level}"
        }

        if (requirements.classIds.isNotEmpty() && user.characterClassId !in requirements.classIds) {
            return "Your class cannot use this spell"
        }

        if (requirements.featureIds.isNotEmpty()) {
            val missingFeatures = requirements.featureIds.filter { it !in user.featureIds }
            if (missingFeatures.isNotEmpty()) {
                return "Missing required features"
            }
        }

        return null
    }

    private fun executeUtilityAction(
        user: User,
        utility: UtilityConfig,
        targetParams: Map<String, String>
    ): CastResult {
        return when (utility.action) {
            "phase_walk" -> executePhaseWalk(user, utility.params, targetParams)
            "teleport" -> executeTeleport(user, utility.params, targetParams)
            "recall" -> executeRecall(user, utility.params)
            "levitate" -> executeLevitate(user, utility.params)
            "detect_secret" -> executeDetectSecret(user, utility.params)
            "invisibility" -> executeInvisibility(user, utility.params)
            "light" -> executeLight(user, utility.params)
            "unlock" -> executeUnlock(user, utility.params, targetParams)
            else -> CastResult.Failure("Unknown spell action: ${utility.action}")
        }
    }

    // ========================================================================
    // Spell Action Implementations
    // ========================================================================

    /**
     * Phase Walk: Move to an adjacent tile ignoring exits.
     * Requires targetParams["direction"] = "NORTH", "SOUTH", etc.
     */
    private fun executePhaseWalk(
        user: User,
        params: UtilityParams,
        targetParams: Map<String, String>
    ): CastResult {
        val currentLocation = user.currentLocationId?.let { LocationRepository.findById(it) }
            ?: return CastResult.Failure("You must be at a location to phase walk")

        // Get direction from target params
        val directionStr = targetParams["direction"]
            ?: return CastResult.Failure("Specify a direction to phase walk")

        val direction = try {
            ExitDirection.valueOf(directionStr.uppercase())
        } catch (e: Exception) {
            return CastResult.Failure("Invalid direction: $directionStr")
        }

        // Calculate target coordinates based on direction
        val currentX = currentLocation.gridX
        val currentY = currentLocation.gridY
        val currentAreaId = currentLocation.areaId ?: "overworld"

        if (currentX == null || currentY == null) {
            return CastResult.Failure("Current location has no coordinates")
        }

        val (targetX, targetY) = when (direction) {
            ExitDirection.NORTH -> Pair(currentX, currentY - 1)
            ExitDirection.NORTHEAST -> Pair(currentX + 1, currentY - 1)
            ExitDirection.EAST -> Pair(currentX + 1, currentY)
            ExitDirection.SOUTHEAST -> Pair(currentX + 1, currentY + 1)
            ExitDirection.SOUTH -> Pair(currentX, currentY + 1)
            ExitDirection.SOUTHWEST -> Pair(currentX - 1, currentY + 1)
            ExitDirection.WEST -> Pair(currentX - 1, currentY)
            ExitDirection.NORTHWEST -> Pair(currentX - 1, currentY - 1)
            ExitDirection.UP, ExitDirection.DOWN -> return CastResult.Failure("Cannot phase walk vertically")
            ExitDirection.ENTER -> return CastResult.Failure("Cannot phase walk through portals")
            ExitDirection.UNKNOWN -> return CastResult.Failure("Cannot phase walk in unknown direction")
        }

        // Check range (default 1 tile)
        val maxRange = params.range ?: 1
        val distance = maxOf(
            kotlin.math.abs(targetX - currentX),
            kotlin.math.abs(targetY - currentY)
        )
        if (distance > maxRange) {
            return CastResult.Failure("Target is too far away")
        }

        // Find location at target coordinates (phase walk stays in the same area)
        val targetLocation = LocationRepository.findByCoordinates(targetX, targetY, currentAreaId)
            ?: return CastResult.Failure("There is nothing in that direction - you cannot phase into the void")

        // Check if target terrain is blocked (e.g., solid rock)
        val blockedTerrain = params.ignoresTerrain ?: listOf("wall", "door", "barrier")
        // TODO: Check target location's features for terrain that can't be phased through

        // Move the user
        UserRepository.updateCurrentLocation(user.id, targetLocation.id)
        // Record visited location for minimap fog-of-war
        UserRepository.addVisitedLocation(user.id, targetLocation.id)

        return CastResult.Success(
            message = "You phase through reality and emerge at ${targetLocation.name}",
            newLocationId = targetLocation.id
        )
    }

    /**
     * Teleport: Jump to a known location.
     * Requires targetParams["locationId"] for the destination.
     */
    private fun executeTeleport(
        user: User,
        params: UtilityParams,
        targetParams: Map<String, String>
    ): CastResult {
        val targetLocationId = targetParams["locationId"]
            ?: return CastResult.Failure("Specify a destination location")

        val targetLocation = LocationRepository.findById(targetLocationId)
            ?: return CastResult.Failure("Destination not found")

        // TODO: Check if user has visited this location before (familiarity)
        // TODO: Check max distance if applicable

        // Move the user
        UserRepository.updateCurrentLocation(user.id, targetLocation.id)
        // Record visited location for minimap fog-of-war
        UserRepository.addVisitedLocation(user.id, targetLocation.id)

        return CastResult.Success(
            message = "Reality folds around you as you materialize at ${targetLocation.name}",
            newLocationId = targetLocation.id
        )
    }

    /**
     * Recall: Return to bind point/home.
     */
    private fun executeRecall(user: User, params: UtilityParams): CastResult {
        // TODO: Implement bind point system
        // For now, teleport to location at (0,0) in overworld (starting area)
        val homeLocation = LocationRepository.findByCoordinates(0, 0, "overworld")
            ?: return CastResult.Failure("No home location found")

        UserRepository.updateCurrentLocation(user.id, homeLocation.id)
        // Record visited location for minimap fog-of-war
        UserRepository.addVisitedLocation(user.id, homeLocation.id)

        return CastResult.Success(
            message = "You feel the familiar tug of home as you vanish and reappear at ${homeLocation.name}",
            newLocationId = homeLocation.id
        )
    }

    /**
     * Levitate: Enable vertical movement.
     * This is more of a buff that allows UP/DOWN exits without stairs.
     */
    private fun executeLevitate(user: User, params: UtilityParams): CastResult {
        // TODO: Apply levitation buff to user (add a status feature)
        // For now, just return success message
        val duration = params.duration ?: 600

        return CastResult.Success(
            message = "You rise gently into the air, defying gravity for ${duration / 60} minutes"
        )
    }

    /**
     * Detect Secret: Reveal hidden things at current location.
     */
    private fun executeDetectSecret(user: User, params: UtilityParams): CastResult {
        val currentLocation = user.currentLocationId?.let { LocationRepository.findById(it) }
            ?: return CastResult.Failure("You must be at a location")

        val reveals = params.reveals ?: listOf("hidden_exit", "trap")
        val revealed = RevealedInfo()

        // TODO: Implement actual hidden exit detection
        // This would require a "hidden" flag on exits or a "secret_exit" feature category

        // TODO: Implement trap detection
        // This would require traps to be Features attached to locations

        // TODO: Implement invisible creature detection
        // This would require checking creatures at location for invisibility status

        return CastResult.Success(
            message = "Your senses expand, searching for hidden secrets...",
            revealedInfo = revealed
        )
    }

    /**
     * Invisibility: Make user invisible.
     */
    private fun executeInvisibility(user: User, params: UtilityParams): CastResult {
        // TODO: Apply invisibility status to user
        val duration = params.duration ?: 300

        return CastResult.Success(
            message = "Light bends around you as you fade from sight for ${duration / 60} minutes"
        )
    }

    /**
     * Light: Create illumination.
     */
    private fun executeLight(user: User, params: UtilityParams): CastResult {
        val radius = params.radius ?: 40
        val duration = params.duration ?: 3600

        // TODO: Apply light effect to user or location

        return CastResult.Success(
            message = "A soft orb of light springs into existence, illuminating ${radius} feet around you"
        )
    }

    /**
     * Unlock: Open a locked door or container.
     * Requires targetParams["targetId"] for the locked item/exit.
     */
    private fun executeUnlock(
        user: User,
        params: UtilityParams,
        targetParams: Map<String, String>
    ): CastResult {
        val targetId = targetParams["targetId"]
            ?: return CastResult.Failure("Specify what to unlock")

        // TODO: Implement lock/unlock system
        // This would require locks to be tracked on items or exits

        return CastResult.Success(
            message = "Magical energy flows into the lock mechanism... *click*"
        )
    }
}
