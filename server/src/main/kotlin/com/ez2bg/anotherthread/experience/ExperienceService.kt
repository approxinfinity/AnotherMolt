package com.ez2bg.anotherthread.experience

import com.ez2bg.anotherthread.database.Creature
import com.ez2bg.anotherthread.database.User
import com.ez2bg.anotherthread.database.UserRepository
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Experience and leveling system.
 *
 * XP is scaled individually per player based on their level vs creature CR.
 * This allows party play with level disparity without breaking progression.
 */
object ExperienceService {

    // XP required to reach each level (index = level, value = total XP needed)
    // Level 1 = 0 XP, Level 2 = 100 XP, etc.
    private val xpThresholds = listOf(
        0,      // Level 1
        100,    // Level 2
        300,    // Level 3
        600,    // Level 4
        1000,   // Level 5
        1500,   // Level 6
        2100,   // Level 7
        2800,   // Level 8
        3600,   // Level 9
        4500,   // Level 10
        5500,   // Level 11
        6600,   // Level 12
        7800,   // Level 13
        9100,   // Level 14
        10500,  // Level 15
        12000,  // Level 16
        13600,  // Level 17
        15300,  // Level 18
        17100,  // Level 19
        19000   // Level 20 (cap)
    )

    const val MAX_LEVEL = 20

    /**
     * Calculate XP earned from defeating a creature.
     * Uses individual scaling based on player level vs creature CR.
     */
    fun calculateCreatureXp(playerLevel: Int, creature: Creature): Int {
        val baseXp = creature.experienceValue
        val cr = creature.challengeRating
        val scale = getScaleFactor(playerLevel, cr)
        return max(1, (baseXp * scale).roundToInt())
    }

    /**
     * Calculate XP earned from PvP victory.
     * Based on opponent's level relative to yours.
     * Reduced overall to discourage farming.
     */
    fun calculatePvpXp(winnerLevel: Int, loserLevel: Int): Int {
        val baseXp = 20 + (loserLevel * 5)  // Base PvP XP scales with opponent level
        val scale = getScaleFactor(winnerLevel, loserLevel) * 0.5  // 50% reduction for PvP
        return max(1, (baseXp * scale).roundToInt())
    }

    /**
     * Get scaling factor based on level difference.
     *
     * diff >= 4:  1.5x  (much harder)
     * diff >= 2:  1.25x (harder)
     * diff >= -1: 1.0x  (appropriate)
     * diff >= -4: 0.5x  (easy)
     * diff >= -8: 0.25x (trivial)
     * else:       0.1x  (grey - floor)
     */
    private fun getScaleFactor(playerLevel: Int, targetLevel: Int): Double {
        val diff = targetLevel - playerLevel
        return when {
            diff >= 4 -> 1.5
            diff >= 2 -> 1.25
            diff >= -1 -> 1.0
            diff >= -4 -> 0.5
            diff >= -8 -> 0.25
            else -> 0.1
        }
    }

    /**
     * Award XP to a user and handle level-ups.
     * Returns the XP result including any level changes.
     */
    fun awardXp(userId: String, xpAmount: Int): XpResult {
        val user = UserRepository.findById(userId) ?: return XpResult(
            success = false,
            message = "User not found"
        )

        if (user.level >= MAX_LEVEL) {
            return XpResult(
                success = true,
                message = "Already at max level",
                xpAwarded = 0,
                newTotalXp = user.experience,
                newLevel = user.level,
                leveledUp = false
            )
        }

        val oldLevel = user.level
        val newTotalXp = user.experience + xpAmount
        val newLevel = calculateLevel(newTotalXp)
        val leveledUp = newLevel > oldLevel

        // Update user
        val updatedUser = user.copy(
            experience = newTotalXp,
            level = newLevel,
            maxHp = if (leveledUp) calculateMaxHp(newLevel) else user.maxHp,
            currentHp = if (leveledUp) calculateMaxHp(newLevel) else user.currentHp  // Full heal on level up
        )
        UserRepository.update(updatedUser)

        val message = if (leveledUp) {
            "Gained $xpAmount XP! LEVEL UP! You are now level $newLevel!"
        } else {
            val xpToNext = getXpToNextLevel(newTotalXp, newLevel)
            "Gained $xpAmount XP ($xpToNext to next level)"
        }

        return XpResult(
            success = true,
            message = message,
            xpAwarded = xpAmount,
            newTotalXp = newTotalXp,
            newLevel = newLevel,
            leveledUp = leveledUp,
            levelsGained = newLevel - oldLevel
        )
    }

    /**
     * Calculate what level a player should be at given total XP.
     */
    fun calculateLevel(totalXp: Int): Int {
        for (level in (MAX_LEVEL - 1) downTo 0) {
            if (totalXp >= xpThresholds[level]) {
                return level + 1  // Levels are 1-indexed
            }
        }
        return 1
    }

    /**
     * Get XP required for next level.
     */
    fun getXpToNextLevel(currentXp: Int, currentLevel: Int): Int {
        if (currentLevel >= MAX_LEVEL) return 0
        return xpThresholds[currentLevel] - currentXp
    }

    /**
     * Calculate max HP for a given level.
     * Base 10 HP + 5 per level after 1.
     */
    fun calculateMaxHp(level: Int): Int {
        return 10 + ((level - 1) * 5)
    }

    /**
     * Get XP threshold for a specific level.
     */
    fun getXpForLevel(level: Int): Int {
        return if (level in 1..MAX_LEVEL) xpThresholds[level - 1] else 0
    }
}

data class XpResult(
    val success: Boolean,
    val message: String,
    val xpAwarded: Int = 0,
    val newTotalXp: Int = 0,
    val newLevel: Int = 1,
    val leveledUp: Boolean = false,
    val levelsGained: Int = 0
)
