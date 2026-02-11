package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.random.Random

/**
 * Service for handling environmental effects on locations.
 *
 * Supports:
 * - Direction Confusion: Maze areas where non-minotaurs can get lost
 * - Environmental Hazards: Areas with ongoing effects
 * - Special Zone Effects: Silence, anti-magic, etc.
 */
object EnvironmentalEffectService {
    private val log = LoggerFactory.getLogger(EnvironmentalEffectService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    // Feature IDs for environmental effects
    const val DIRECTION_CONFUSION_FEATURE = "feature-direction-confusion"
    const val MINOTAUR_IMMUNITY_ABILITY = "ability-caves-of-chaos-labyrinth-sense"

    /**
     * Environmental effect data stored in Feature.data
     */
    @Serializable
    data class EnvironmentalEffectData(
        val effectType: String,  // "direction_confusion", "hazard", "zone"
        val confusionChance: Float = 0.5f,  // Chance to go to wrong exit
        val hazardDamageDice: String? = null,
        val hazardDamageType: String? = null,
        val zoneEffect: String? = null,
        val immuneAbilityIds: List<String> = emptyList(),
        val immuneCreatureTypes: List<String> = emptyList()
    )

    /**
     * Result of checking environmental effects when moving.
     */
    @Serializable
    data class MovementEffectResult(
        val redirected: Boolean = false,
        val newLocationId: String? = null,
        val message: String? = null,
        val hazardDamage: Int = 0
    )

    /**
     * Check if a user moving to a location is affected by direction confusion.
     * Returns a different exit location if confused, or null if movement proceeds normally.
     */
    fun checkDirectionConfusion(user: User, targetLocation: Location): MovementEffectResult {
        // Check if target location has direction confusion feature
        val hasConfusion = targetLocation.featureIds.any { featureId ->
            val feature = FeatureRepository.findById(featureId)
            if (feature != null) {
                try {
                    val data = json.decodeFromString<EnvironmentalEffectData>(feature.data)
                    data.effectType == "direction_confusion"
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        }

        if (!hasConfusion) {
            return MovementEffectResult()
        }

        // Check if user is immune (e.g., has Labyrinth Sense)
        if (isImmuneToConfusion(user, targetLocation)) {
            return MovementEffectResult()
        }

        // Get the confusion effect data
        val confusionFeature = targetLocation.featureIds.mapNotNull { FeatureRepository.findById(it) }
            .find {
                try {
                    val data = json.decodeFromString<EnvironmentalEffectData>(it.data)
                    data.effectType == "direction_confusion"
                } catch (e: Exception) {
                    false
                }
            }

        if (confusionFeature == null) {
            return MovementEffectResult()
        }

        val effectData = try {
            json.decodeFromString<EnvironmentalEffectData>(confusionFeature.data)
        } catch (e: Exception) {
            return MovementEffectResult()
        }

        // Roll for confusion
        if (Random.nextFloat() > effectData.confusionChance) {
            return MovementEffectResult()  // Not confused this time
        }

        // Player is confused! Pick a random exit from the target location
        val exits = targetLocation.exits
        if (exits.isEmpty()) {
            return MovementEffectResult()  // No exits to redirect to
        }

        // Pick a random different exit (could be the same room, leading to wandering)
        val randomExit = exits.random()

        log.info("User ${user.name} is confused in the maze and wandered to ${randomExit.locationId}")

        return MovementEffectResult(
            redirected = true,
            newLocationId = randomExit.locationId,
            message = "The twisting passages confuse you. You're not sure where you ended up..."
        )
    }

    /**
     * Check if user is immune to direction confusion.
     */
    private fun isImmuneToConfusion(user: User, location: Location): Boolean {
        // Check for Labyrinth Sense ability
        if (user.learnedAbilityIds.contains(MINOTAUR_IMMUNITY_ABILITY)) {
            return true
        }

        // Check location-specific immunities
        val confusionFeature = location.featureIds.mapNotNull { FeatureRepository.findById(it) }
            .find {
                try {
                    val data = json.decodeFromString<EnvironmentalEffectData>(it.data)
                    data.effectType == "direction_confusion"
                } catch (e: Exception) {
                    false
                }
            }

        if (confusionFeature != null) {
            try {
                val data = json.decodeFromString<EnvironmentalEffectData>(confusionFeature.data)
                // Check if any of user's abilities grant immunity
                for (abilityId in user.learnedAbilityIds) {
                    if (data.immuneAbilityIds.contains(abilityId)) {
                        return true
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        return false
    }

    /**
     * Create the Direction Confusion feature if it doesn't exist.
     */
    fun ensureDirectionConfusionFeature(confusionChance: Float = 0.5f): Feature {
        val existing = FeatureRepository.findById(DIRECTION_CONFUSION_FEATURE)
        if (existing != null) {
            return existing
        }

        val data = EnvironmentalEffectData(
            effectType = "direction_confusion",
            confusionChance = confusionChance,
            immuneAbilityIds = listOf(MINOTAUR_IMMUNITY_ABILITY)
        )

        val feature = Feature(
            id = DIRECTION_CONFUSION_FEATURE,
            name = "Confusing Maze",
            description = "The twisting passages here are disorienting. Those without special guidance may become lost.",
            data = json.encodeToString(EnvironmentalEffectData.serializer(), data)
        )

        return FeatureRepository.create(feature)
    }

    /**
     * Apply direction confusion to the minotaur maze locations in Caves of Chaos.
     * This adds the confusion feature to all Cave I locations.
     */
    fun applyDirectionConfusionToMinotaurMaze() {
        // Ensure the feature exists
        val feature = ensureDirectionConfusionFeature(confusionChance = 0.4f)

        // Location IDs in the minotaur's labyrinth
        val mazeLocationIds = listOf(
            "location-caves-of-chaos-cave-i-entrance",
            "location-caves-of-chaos-cave-i-stirge",
            "location-caves-of-chaos-cave-i-beetles-1",
            "location-caves-of-chaos-cave-i-beetles-2",
            "location-caves-of-chaos-cave-i-minotaur"
        )

        for (locationId in mazeLocationIds) {
            val location = LocationRepository.findById(locationId) ?: continue

            // Check if feature already added
            if (location.featureIds.contains(feature.id)) {
                continue
            }

            // Add the feature
            val updatedFeatureIds = location.featureIds + feature.id
            val updatedLocation = location.copy(featureIds = updatedFeatureIds)
            LocationRepository.update(updatedLocation)
            log.info("Added direction confusion to ${location.name}")
        }
    }
}
