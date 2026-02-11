package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.random.Random

/**
 * Service for handling secret door discovery mechanics.
 *
 * Secret doors are hidden exits that require active searching to discover.
 * Once discovered by a player, they remain visible for that player.
 *
 * Discovery is tracked via FeatureState with keys like "discovered_secret_{locationId}_{exitDirection}"
 */
object SecretDoorService {
    private val log = LoggerFactory.getLogger(SecretDoorService::class.java)

    /**
     * Result of searching for secret doors.
     */
    @Serializable
    data class SearchResult(
        val success: Boolean,
        val message: String,
        val discoveredExits: List<DiscoveredExit> = emptyList()
    )

    @Serializable
    data class DiscoveredExit(
        val direction: String,
        val destinationName: String,
        val destinationId: String
    )

    /**
     * Secret door configuration attached to an exit.
     * Store in Location.exits as a special field or check location names/descriptions.
     */
    @Serializable
    data class SecretDoorConfig(
        val isSecret: Boolean = true,
        val detectDifficulty: Int = 2,  // 1-5 scale, DC = 10 + difficulty*2
        val requiresKey: String? = null,
        val triggerMechanism: String? = null  // "lever", "pressure_plate", "keyword", etc.
    )

    // Feature ID prefix for secret door tracking
    const val SECRET_DOOR_PREFIX = "discovered_secret_"

    /**
     * Check if a secret exit has been discovered by a user.
     */
    fun isSecretExitDiscovered(userId: String, locationId: String, exitDirection: ExitDirection): Boolean {
        val key = "$SECRET_DOOR_PREFIX${locationId}_${exitDirection.name}"
        val state = FeatureStateRepository.getState(userId, key)
        return state?.value == "true"
    }

    /**
     * Mark a secret exit as discovered.
     */
    fun markSecretExitDiscovered(userId: String, locationId: String, exitDirection: ExitDirection) {
        val key = "$SECRET_DOOR_PREFIX${locationId}_${exitDirection.name}"
        FeatureStateRepository.setState(userId, key, "true")
    }

    /**
     * Get visible exits for a user at a location.
     * Filters out secret exits that haven't been discovered.
     */
    fun getVisibleExits(userId: String, location: Location): List<Exit> {
        return location.exits.filter { exit ->
            // Check if this exit leads to a "secret" location
            val destination = LocationRepository.findById(exit.locationId)
            if (destination == null) {
                true  // Show exit even if destination doesn't exist
            } else {
                // Secret exits are identified by having "secret" in the name or description
                val isSecret = destination.name.lowercase().contains("secret") ||
                        destination.desc.lowercase().contains("secret door") ||
                        destination.desc.lowercase().contains("hidden passage")

                if (isSecret) {
                    isSecretExitDiscovered(userId, location.id, exit.direction)
                } else {
                    true
                }
            }
        }
    }

    /**
     * Search for secret doors in a location.
     * Uses Wisdom/Perception check against hidden exit DCs.
     */
    fun searchForSecretDoors(userId: String, locationId: String): SearchResult {
        val user = UserRepository.findById(userId)
            ?: return SearchResult(false, "User not found.")

        val location = LocationRepository.findById(locationId)
            ?: return SearchResult(false, "Location not found.")

        val discoveredExits = mutableListOf<DiscoveredExit>()
        val wisdomBonus = (user.wisdom - 10) / 2

        for (exit in location.exits) {
            val destination = LocationRepository.findById(exit.locationId) ?: continue

            // Check if this is a secret exit
            val isSecret = destination.name.lowercase().contains("secret") ||
                    destination.desc.lowercase().contains("secret door") ||
                    destination.desc.lowercase().contains("hidden passage")

            if (!isSecret) continue

            // Already discovered?
            if (isSecretExitDiscovered(userId, locationId, exit.direction)) continue

            // Determine difficulty based on destination name/desc or default
            val difficulty = when {
                destination.name.lowercase().contains("master") -> 4
                destination.name.lowercase().contains("complex") -> 3
                destination.name.lowercase().contains("hidden") -> 2
                else -> 2
            }

            val dc = 10 + difficulty * 2
            val roll = Random.nextInt(1, 21) + wisdomBonus

            if (roll >= dc) {
                markSecretExitDiscovered(userId, locationId, exit.direction)
                discoveredExits.add(DiscoveredExit(
                    direction = exit.direction.name,
                    destinationName = destination.name,
                    destinationId = destination.id
                ))
                log.info("User ${user.name} discovered secret door to ${destination.name}")
            }
        }

        return if (discoveredExits.isNotEmpty()) {
            val names = discoveredExits.joinToString(", ") { it.destinationName }
            SearchResult(
                success = true,
                message = "You discover a hidden passage! You found: $names",
                discoveredExits = discoveredExits
            )
        } else {
            // Check if there were any secret doors at all
            val hasSecretDoors = location.exits.any { exit ->
                val dest = LocationRepository.findById(exit.locationId)
                dest != null && (dest.name.lowercase().contains("secret") ||
                        dest.desc.lowercase().contains("secret door") ||
                        dest.desc.lowercase().contains("hidden passage"))
            }

            if (hasSecretDoors) {
                SearchResult(
                    success = true,
                    message = "You search carefully but don't find anything... or maybe you missed something."
                )
            } else {
                SearchResult(
                    success = true,
                    message = "You search the area thoroughly but find no secret passages."
                )
            }
        }
    }

    /**
     * Get all secret doors in a location that the user has NOT discovered.
     * Useful for checking if there's anything left to find.
     */
    fun getUndiscoveredSecretDoors(userId: String, locationId: String): List<Exit> {
        val location = LocationRepository.findById(locationId) ?: return emptyList()

        return location.exits.filter { exit ->
            val destination = LocationRepository.findById(exit.locationId) ?: return@filter false

            val isSecret = destination.name.lowercase().contains("secret") ||
                    destination.desc.lowercase().contains("secret door") ||
                    destination.desc.lowercase().contains("hidden passage")

            isSecret && !isSecretExitDiscovered(userId, locationId, exit.direction)
        }
    }
}
