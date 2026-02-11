package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.experience.ExperienceService
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * Service for handling prisoner rescue mechanics.
 *
 * Prisoners are special NPCs that can be rescued for rewards.
 * They are tracked via FeatureState with keys like "rescued_prisoner_{prisonerId}".
 *
 * Quest flow:
 * 1. Player finds prisoner in a cell/cage
 * 2. Player defeats guards or picks lock
 * 3. Player interacts with prisoner to "rescue" them
 * 4. Prisoner disappears, player gets XP/gold reward
 * 5. If prisoner has a home location, they may appear there later with additional thanks
 */
object PrisonerRescueService {
    private val log = LoggerFactory.getLogger(PrisonerRescueService::class.java)

    /**
     * Result of attempting to rescue a prisoner.
     */
    @Serializable
    data class RescueResult(
        val success: Boolean,
        val message: String,
        val xpReward: Int = 0,
        val goldReward: Int = 0,
        val itemRewards: List<String> = emptyList()
    )

    /**
     * Attempt to rescue a prisoner.
     * The prisoner must be a creature with isPrisoner=true in their features.
     */
    fun rescuePrisoner(userId: String, prisonerId: String): RescueResult {
        val user = UserRepository.findById(userId)
            ?: return RescueResult(false, "User not found.")

        val prisoner = CreatureRepository.findById(prisonerId)
            ?: return RescueResult(false, "Prisoner not found.")

        // Check if this creature is actually a prisoner (non-aggressive, rescuable)
        if (prisoner.isAggressive) {
            return RescueResult(false, "This creature doesn't seem to want your help.")
        }

        // Check if already rescued
        val rescueKey = "rescued_prisoner_$prisonerId"
        val existingState = FeatureStateRepository.getState(userId, rescueKey)
        if (existingState?.value == "true") {
            return RescueResult(false, "You've already rescued this prisoner.")
        }

        // Check if player is at the prisoner's location
        if (user.currentLocationId != null) {
            val location = LocationRepository.findById(user.currentLocationId)
            if (location != null && !location.creatureIds.contains(prisonerId)) {
                return RescueResult(false, "The prisoner is not here.")
            }
        }

        // Calculate rewards based on prisoner's experience value
        val xpReward = prisoner.experienceValue
        val goldReward = prisoner.maxGoldDrop

        // Mark as rescued
        FeatureStateRepository.setState(userId, rescueKey, "true")

        // Award XP
        if (xpReward > 0) {
            ExperienceService.awardXp(userId, xpReward)
        }

        // Award gold
        if (goldReward > 0) {
            UserRepository.addGold(userId, goldReward)
        }

        // Get item rewards from prisoner's loot table
        val itemRewards = mutableListOf<String>()
        prisoner.lootTableId?.let { lootTableId ->
            LootTableRepository.findById(lootTableId)?.entries?.forEach { entry ->
                if (kotlin.random.Random.nextFloat() < entry.chance) {
                    val item = ItemRepository.findById(entry.itemId)
                    if (item != null) {
                        UserRepository.addItems(userId, listOf(item.id))
                        itemRewards.add(item.name)
                    }
                }
            }
        }

        log.info("User ${user.name} rescued prisoner ${prisoner.name}, earned $xpReward XP and $goldReward gold")

        // Build message
        val rewardParts = mutableListOf<String>()
        if (xpReward > 0) rewardParts.add("$xpReward XP")
        if (goldReward > 0) rewardParts.add("$goldReward gold")
        if (itemRewards.isNotEmpty()) rewardParts.add(itemRewards.joinToString(", "))

        val rewardMessage = if (rewardParts.isNotEmpty()) {
            " They reward you with ${rewardParts.joinToString(" and ")}!"
        } else {
            ""
        }

        return RescueResult(
            success = true,
            message = "You rescue ${prisoner.name}! They thank you profusely and hurry to safety.$rewardMessage",
            xpReward = xpReward,
            goldReward = goldReward,
            itemRewards = itemRewards
        )
    }

    /**
     * Check if a creature can be rescued (is a prisoner that hasn't been rescued yet).
     */
    fun canRescue(userId: String, creatureId: String): Boolean {
        val creature = CreatureRepository.findById(creatureId) ?: return false

        // Must be non-aggressive (friendly NPC)
        if (creature.isAggressive) return false

        // Check if name suggests prisoner (simple heuristic)
        val prisonerKeywords = listOf("prisoner", "captive", "slave", "hostage", "merchant", "traveler")
        val isPrisoner = prisonerKeywords.any { creature.name.lowercase().contains(it) }

        if (!isPrisoner) return false

        // Check if already rescued
        val rescueKey = "rescued_prisoner_$creatureId"
        val existingState = FeatureStateRepository.getState(userId, rescueKey)
        return existingState?.value != "true"
    }

    /**
     * Get all prisoners at a location that can be rescued by the user.
     */
    fun getRescuablePrisoners(userId: String, locationId: String): List<Creature> {
        val location = LocationRepository.findById(locationId) ?: return emptyList()

        return location.creatureIds.mapNotNull { creatureId ->
            val creature = CreatureRepository.findById(creatureId)
            if (creature != null && canRescue(userId, creatureId)) {
                creature
            } else {
                null
            }
        }
    }
}
