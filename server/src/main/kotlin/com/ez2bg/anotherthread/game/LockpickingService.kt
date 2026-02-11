package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlin.random.Random

/**
 * Service for handling lockpicking mechanics with a "trace the path" minigame.
 *
 * Locked doors and chests require either:
 * - DEX > 16, OR
 * - The lockpicking ability (learned by rogue-type classes)
 *
 * The minigame generates a winding path that the player must trace.
 * Accuracy depends on how closely they follow the path.
 */
object LockpickingService {
    private val log = org.slf4j.LoggerFactory.getLogger("LockpickingService")

    const val LOCKPICKING_ABILITY_ID = "ability-lockpicking"
    private const val DEX_THRESHOLD = 16

    /**
     * Lock difficulty levels with increasing path complexity.
     */
    enum class LockDifficulty(
        val pathPoints: Int,      // Number of control points for the path
        val tolerance: Float,     // Pixels from ideal path that counts as "on path"
        val shakiness: Float,     // Amount of visual wobble on the path (0-1)
        val successThreshold: Float  // Minimum accuracy (0-1) to succeed
    ) {
        SIMPLE(5, 25f, 0f, 0.5f),      // Wide tolerance, no shake, 50% accuracy needed
        STANDARD(7, 18f, 0.15f, 0.6f),  // Medium tolerance, slight shake
        COMPLEX(10, 12f, 0.25f, 0.7f),  // Narrow tolerance, moderate shake
        MASTER(12, 8f, 0.4f, 0.8f)     // Very narrow, shaky, 80% accuracy needed
    }

    /**
     * A point in the lockpicking path.
     */
    data class PathPoint(
        val x: Float,  // 0-1 normalized position
        val y: Float   // 0-1 normalized position
    )

    /**
     * Information about a lock for the client.
     */
    data class LockInfo(
        val canAttempt: Boolean,
        val reason: String?,
        val difficulty: LockDifficulty,
        val pathPoints: List<PathPoint>,  // Generated path for the minigame
        val tolerance: Float,
        val shakiness: Float,
        val successThreshold: Float
    )

    /**
     * Result of a lockpicking attempt.
     */
    data class LockpickResult(
        val success: Boolean,
        val message: String,
        val accuracy: Float = 0f,
        val lockOpened: Boolean = false
    )

    /**
     * Check if a user can attempt lockpicking.
     * Requires either DEX > 16 OR the lockpicking ability.
     */
    fun canAttemptLockpicking(user: User): Boolean {
        return user.dexterity > DEX_THRESHOLD ||
               user.learnedAbilityIds.contains(LOCKPICKING_ABILITY_ID)
    }

    /**
     * Get the reason why a user cannot attempt lockpicking.
     */
    fun getCannotAttemptReason(user: User): String {
        return "You need either DEX > $DEX_THRESHOLD or the Lockpicking skill to pick locks."
    }

    /**
     * Get lock difficulty based on lock level.
     */
    fun getLockDifficulty(lockLevel: Int): LockDifficulty {
        return when {
            lockLevel >= 4 -> LockDifficulty.MASTER
            lockLevel >= 3 -> LockDifficulty.COMPLEX
            lockLevel >= 2 -> LockDifficulty.STANDARD
            else -> LockDifficulty.SIMPLE
        }
    }

    /**
     * Generate a random winding path for the lockpicking minigame.
     * Path goes from left to right with random vertical movements.
     */
    fun generatePath(difficulty: LockDifficulty, seed: Long = System.currentTimeMillis()): List<PathPoint> {
        val random = Random(seed)
        val points = mutableListOf<PathPoint>()

        // Start point (left side, middle-ish)
        points.add(PathPoint(0.05f, 0.4f + random.nextFloat() * 0.2f))

        // Generate intermediate points
        val numPoints = difficulty.pathPoints - 2
        for (i in 0 until numPoints) {
            val progress = (i + 1f) / (numPoints + 1f)
            val x = 0.1f + progress * 0.8f

            // Random Y with more variation for higher difficulties
            val yRange = 0.3f + (difficulty.ordinal * 0.1f)
            val y = 0.5f + (random.nextFloat() - 0.5f) * yRange * 2f

            points.add(PathPoint(x.coerceIn(0.1f, 0.9f), y.coerceIn(0.15f, 0.85f)))
        }

        // End point (right side)
        points.add(PathPoint(0.95f, 0.4f + random.nextFloat() * 0.2f))

        return points
    }

    /**
     * Get lock info for a location.
     */
    fun getLockInfo(user: User, location: Location): LockInfo? {
        val lockLevel = location.lockLevel ?: return null

        val canAttempt = canAttemptLockpicking(user)
        val difficulty = getLockDifficulty(lockLevel)
        val path = generatePath(difficulty)

        return LockInfo(
            canAttempt = canAttempt,
            reason = if (!canAttempt) getCannotAttemptReason(user) else null,
            difficulty = difficulty,
            pathPoints = path,
            tolerance = difficulty.tolerance,
            shakiness = difficulty.shakiness,
            successThreshold = difficulty.successThreshold
        )
    }

    /**
     * Attempt to pick a lock.
     *
     * @param user The user attempting
     * @param locationId The location with the lock
     * @param accuracy The player's trace accuracy (0-1)
     * @return LockpickResult with success/failure info
     */
    fun attemptLockpick(user: User, locationId: String, accuracy: Float): LockpickResult {
        // Get the location
        val location = LocationRepository.findById(locationId)
            ?: return LockpickResult(false, "Location not found")

        val lockLevel = location.lockLevel
            ?: return LockpickResult(false, "This is not locked")

        // Check if user can attempt
        if (!canAttemptLockpicking(user)) {
            return LockpickResult(false, getCannotAttemptReason(user))
        }

        // Get difficulty threshold
        val difficulty = getLockDifficulty(lockLevel)

        // Check if accuracy meets threshold
        val success = accuracy >= difficulty.successThreshold

        if (success) {
            // Record the unlock for reset tracking
            LockResetService.recordUnlock(locationId, lockLevel, GameTickService.getCurrentTick())

            // Unlock the location by setting lockLevel to null
            LocationRepository.updateLockLevel(locationId, null)
            log.info("${user.name} successfully picked a ${difficulty.name} lock on ${location.name}")

            return LockpickResult(
                success = true,
                message = "You successfully pick the lock! (Resets in 1 minute)",
                accuracy = accuracy,
                lockOpened = true
            )
        } else {
            log.debug("${user.name} failed to pick ${difficulty.name} lock (accuracy=$accuracy, needed=${difficulty.successThreshold})")

            // Maybe trigger an alarm or trap in the future?
            return LockpickResult(
                success = false,
                message = "The lock resists your attempt. (Accuracy: ${(accuracy * 100).toInt()}%, needed ${(difficulty.successThreshold * 100).toInt()}%)",
                accuracy = accuracy,
                lockOpened = false
            )
        }
    }

    /**
     * Check if a location has a lock that can be picked.
     */
    fun hasPickableLock(location: Location): Boolean {
        return location.lockLevel != null && location.lockLevel > 0
    }

    /**
     * Get the lock level name for display.
     */
    fun getLockLevelName(lockLevel: Int?): String {
        return when (lockLevel) {
            null, 0 -> "Unlocked"
            1 -> "Simple Lock"
            2 -> "Standard Lock"
            3 -> "Complex Lock"
            4 -> "Master Lock"
            else -> "Mysterious Lock"
        }
    }
}
