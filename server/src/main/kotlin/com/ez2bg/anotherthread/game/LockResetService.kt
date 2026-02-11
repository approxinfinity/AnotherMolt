package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.LocationRepository
import com.ez2bg.anotherthread.database.PuzzleRepository
import com.ez2bg.anotherthread.database.FeatureStateRepository
import org.slf4j.LoggerFactory

/**
 * Service to reset locks and puzzles after they've been solved.
 * Both reset after 20 ticks (1 minute) so players can retry or replay.
 */
object LockResetService {
    private val log = LoggerFactory.getLogger(LockResetService::class.java)

    // Track unlocked locations: locationId -> (originalLockLevel, tickWhenUnlocked)
    private val unlockedLocations = mutableMapOf<String, Pair<Int, Long>>()

    // How many ticks before a lock/puzzle resets
    const val RESET_TICKS = 20L  // 20 ticks * 3 seconds = 1 minute
    const val RESET_MS = RESET_TICKS * 3000L  // 60 seconds in milliseconds

    /**
     * Record that a lock was picked. Called when lockpicking succeeds.
     */
    fun recordUnlock(locationId: String, originalLockLevel: Int, currentTick: Long) {
        unlockedLocations[locationId] = Pair(originalLockLevel, currentTick)
        log.debug("Recorded unlock for $locationId (level $originalLockLevel) at tick $currentTick")
    }

    /**
     * Process lock resets. Called periodically from GameTickService.
     */
    fun processLockResets(currentTick: Long) {
        val toReset = unlockedLocations.filter { (_, value) ->
            val (_, tickWhenUnlocked) = value
            currentTick - tickWhenUnlocked >= RESET_TICKS
        }

        for ((locationId, value) in toReset) {
            val (originalLockLevel, _) = value
            LocationRepository.updateLockLevel(locationId, originalLockLevel)
            unlockedLocations.remove(locationId)
            log.info("Reset lock on location $locationId to level $originalLockLevel")
        }
    }

    /**
     * Process puzzle resets. Puzzles reset 1 minute after being solved.
     * Called periodically from GameTickService.
     */
    fun processPuzzleResets() {
        val now = System.currentTimeMillis()
        val puzzles = PuzzleRepository.findAll()

        for (puzzle in puzzles) {
            // Find all feature_state entries for this puzzle that are solved
            val stateKey = "puzzle_progress_${puzzle.id}"
            val states = FeatureStateRepository.findAllByKey(stateKey)

            for (state in states) {
                try {
                    val progress = kotlinx.serialization.json.Json.decodeFromString<com.ez2bg.anotherthread.database.PuzzleProgress>(state.value)
                    if (progress.solved && progress.solvedAt != null) {
                        val elapsed = now - progress.solvedAt
                        if (elapsed >= RESET_MS) {
                            // Reset this user's puzzle progress
                            PuzzleRepository.resetProgress(state.userId, puzzle.id)
                            log.info("Reset puzzle ${puzzle.id} progress for user ${state.userId}")
                        }
                    }
                } catch (e: Exception) {
                    // Invalid state, ignore
                }
            }
        }
    }

    /**
     * Check how many ticks until a lock resets (for display purposes).
     */
    fun getTicksUntilReset(locationId: String, currentTick: Long): Long? {
        val (_, tickWhenUnlocked) = unlockedLocations[locationId] ?: return null
        val ticksElapsed = currentTick - tickWhenUnlocked
        return (RESET_TICKS - ticksElapsed).coerceAtLeast(0)
    }
}
