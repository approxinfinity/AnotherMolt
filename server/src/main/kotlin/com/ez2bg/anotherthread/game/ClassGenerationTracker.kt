package com.ez2bg.anotherthread.game

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory tracker for class generation status.
 *
 * This replaces the persistent `classGenerationStartedAt` field in the User table.
 * Generation status is transient - if the server restarts, users will need to retry
 * class generation (which is the correct behavior for a long-running operation).
 *
 * Thread-safe via ConcurrentHashMap.
 */
object ClassGenerationTracker {

    /**
     * Represents an ongoing class generation operation.
     */
    data class GenerationStatus(
        val userId: String,
        val startedAt: Long,
        val description: String
    )

    // userId -> generation status
    private val activeGenerations = ConcurrentHashMap<String, GenerationStatus>()

    // Generation timeout (10 minutes)
    private const val GENERATION_TIMEOUT_MS = 10 * 60 * 1000L

    /**
     * Mark that class generation has started for a user.
     * Returns true if marked successfully, false if already generating.
     */
    fun startGeneration(userId: String, description: String): Boolean {
        val status = GenerationStatus(
            userId = userId,
            startedAt = System.currentTimeMillis(),
            description = description
        )
        return activeGenerations.putIfAbsent(userId, status) == null
    }

    /**
     * Check if class generation is in progress for a user.
     * Also cleans up timed-out generations.
     */
    fun isGenerating(userId: String): Boolean {
        val status = activeGenerations[userId] ?: return false

        // Check for timeout
        val elapsed = System.currentTimeMillis() - status.startedAt
        if (elapsed >= GENERATION_TIMEOUT_MS) {
            // Timed out - clean up
            activeGenerations.remove(userId)
            return false
        }

        return true
    }

    /**
     * Get the generation status for a user, if active.
     */
    fun getStatus(userId: String): GenerationStatus? {
        val status = activeGenerations[userId] ?: return null

        // Check for timeout
        val elapsed = System.currentTimeMillis() - status.startedAt
        if (elapsed >= GENERATION_TIMEOUT_MS) {
            activeGenerations.remove(userId)
            return null
        }

        return status
    }

    /**
     * Get the start time for a user's class generation, if active.
     */
    fun getStartedAt(userId: String): Long? {
        return getStatus(userId)?.startedAt
    }

    /**
     * Clear the generation status for a user.
     * Called when generation completes (success or failure).
     */
    fun clearGeneration(userId: String) {
        activeGenerations.remove(userId)
    }

    /**
     * Get all active generations (for admin/debugging).
     */
    fun getAllActive(): List<GenerationStatus> {
        val now = System.currentTimeMillis()
        return activeGenerations.values.filter { status ->
            now - status.startedAt < GENERATION_TIMEOUT_MS
        }
    }

    /**
     * Clean up any timed-out generations.
     * Can be called periodically if needed.
     */
    fun cleanupTimedOut(): Int {
        val now = System.currentTimeMillis()
        var cleaned = 0
        activeGenerations.forEach { (userId, status) ->
            if (now - status.startedAt >= GENERATION_TIMEOUT_MS) {
                if (activeGenerations.remove(userId) != null) {
                    cleaned++
                }
            }
        }
        return cleaned
    }
}
