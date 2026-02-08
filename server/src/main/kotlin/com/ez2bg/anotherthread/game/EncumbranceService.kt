package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.Item
import com.ez2bg.anotherthread.database.ItemRepository
import com.ez2bg.anotherthread.database.User

/**
 * Encumbrance tiers based on MajorMUD-style system.
 * Each tier has increasing penalties to combat effectiveness and mobility.
 */
enum class EncumbranceTier(
    val displayName: String,
    val minPercent: Int,       // Minimum percentage of capacity
    val maxPercent: Int,       // Maximum percentage of capacity
    val attackModifier: Int,   // Penalty to attack rolls
    val dodgeModifier: Int,    // Penalty to dodge (as percentage reduction)
    val canMove: Boolean,      // Whether the character can move to new locations
    val description: String    // Narrative description for AI
) {
    UNENCUMBERED(
        displayName = "Unencumbered",
        minPercent = 0,
        maxPercent = 50,
        attackModifier = 0,
        dodgeModifier = 0,
        canMove = true,
        description = "moving freely"
    ),
    LIGHT(
        displayName = "Lightly Encumbered",
        minPercent = 51,
        maxPercent = 75,
        attackModifier = -1,
        dodgeModifier = -5,
        canMove = true,
        description = "slightly weighed down by gear"
    ),
    MEDIUM(
        displayName = "Encumbered",
        minPercent = 76,
        maxPercent = 90,
        attackModifier = -2,
        dodgeModifier = -10,
        canMove = true,
        description = "noticeably burdened by heavy equipment"
    ),
    HEAVY(
        displayName = "Heavily Encumbered",
        minPercent = 91,
        maxPercent = 100,
        attackModifier = -3,
        dodgeModifier = -20,
        canMove = true,
        description = "struggling under the weight of possessions"
    ),
    OVER_ENCUMBERED(
        displayName = "Over-Encumbered",
        minPercent = 101,
        maxPercent = Int.MAX_VALUE,
        attackModifier = -5,
        dodgeModifier = -50,
        canMove = false,
        description = "completely immobilized by excessive burden"
    );

    companion object {
        fun fromPercent(percent: Int): EncumbranceTier {
            return entries.find { percent >= it.minPercent && percent <= it.maxPercent }
                ?: OVER_ENCUMBERED
        }
    }
}

/**
 * Encumbrance calculation result with all relevant data.
 */
data class EncumbranceInfo(
    val currentWeight: Int,
    val maxCapacity: Int,
    val percentUsed: Int,
    val tier: EncumbranceTier,
    val attackModifier: Int,
    val dodgeModifier: Int,
    val canMove: Boolean
) {
    val isOverEncumbered: Boolean get() = tier == EncumbranceTier.OVER_ENCUMBERED
    val displayString: String get() = "$currentWeight/$maxCapacity ($percentUsed%)"
}

/**
 * Service for calculating and managing encumbrance.
 */
object EncumbranceService {

    /**
     * Calculate carrying capacity based on Strength.
     * Formula: STR * 5 = max weight in stone
     * This means a STR 10 character can carry 50 stone.
     */
    fun calculateMaxCapacity(strength: Int): Int {
        return strength * 5
    }

    /**
     * Calculate total weight of items in inventory.
     */
    fun calculateTotalWeight(itemIds: List<String>): Int {
        if (itemIds.isEmpty()) return 0

        // Get all unique item definitions
        val uniqueIds = itemIds.toSet()
        val items = uniqueIds.mapNotNull { ItemRepository.findById(it) }
        val itemWeights = items.associate { it.id to it.weight }

        // Sum up weights (counting duplicates)
        return itemIds.sumOf { itemWeights[it] ?: 1 }
    }

    /**
     * Calculate total weight from a list of Item objects and their counts.
     */
    fun calculateTotalWeightFromItems(items: List<Item>, itemCounts: Map<String, Int>): Int {
        return items.sumOf { item ->
            val count = itemCounts[item.id] ?: 1
            item.weight * count
        }
    }

    /**
     * Get full encumbrance info for a user.
     */
    fun getEncumbranceInfo(user: User): EncumbranceInfo {
        val maxCapacity = calculateMaxCapacity(user.strength)
        val currentWeight = calculateTotalWeight(user.itemIds)
        val percentUsed = if (maxCapacity > 0) (currentWeight * 100) / maxCapacity else 100
        val tier = EncumbranceTier.fromPercent(percentUsed)

        return EncumbranceInfo(
            currentWeight = currentWeight,
            maxCapacity = maxCapacity,
            percentUsed = percentUsed,
            tier = tier,
            attackModifier = tier.attackModifier,
            dodgeModifier = tier.dodgeModifier,
            canMove = tier.canMove
        )
    }

    /**
     * Check if a user can pick up an item (would not exceed capacity by too much).
     * We allow picking up items that would put you over 100%, but warn/slow you.
     * Returns null if allowed, or an error message if not.
     */
    fun canPickupItem(user: User, item: Item): String? {
        val currentInfo = getEncumbranceInfo(user)
        val newWeight = currentInfo.currentWeight + item.weight
        val newPercent = if (currentInfo.maxCapacity > 0) {
            (newWeight * 100) / currentInfo.maxCapacity
        } else 100

        // Allow pickup but warn if it would cause over-encumbrance
        // Could add a hard cap here if desired (e.g., 150% or 200%)
        return null
    }

    /**
     * Check if a user can move to a new location.
     */
    fun canMove(user: User): Boolean {
        return getEncumbranceInfo(user).canMove
    }

    /**
     * Get a narrative description of the user's encumbrance state.
     * Useful for AI context.
     */
    fun getNarrativeDescription(user: User): String {
        val info = getEncumbranceInfo(user)
        return when (info.tier) {
            EncumbranceTier.UNENCUMBERED -> ""
            else -> "The character is ${info.tier.description}."
        }
    }
}
