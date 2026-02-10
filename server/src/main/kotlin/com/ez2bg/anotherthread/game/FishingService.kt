package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlin.random.Random

/**
 * Service for handling fishing mechanics.
 *
 * Players can fish at locations with the "feature-fishing-spot" feature.
 * Fishing success is driven by DEX + INT, duration by WIS, and casting
 * distance (which determines fish size) by STR.
 *
 * Costs: 5 stamina + 2 mana per cast
 * Reward: Successful catch restores 3-5 mana
 */
object FishingService {
    private val log = org.slf4j.LoggerFactory.getLogger("FishingService")

    // Cost constants
    private const val STAMINA_COST = 5
    private const val MANA_COST = 2
    private const val MANA_RESTORE_MIN = 3
    private const val MANA_RESTORE_MAX = 5

    // STR requirements for casting distances
    private const val MID_STR_REQUIREMENT = 10
    private const val FAR_STR_REQUIREMENT = 14

    // Feature ID for fishing spots
    private const val FISHING_FEATURE_ID = "feature-fishing-spot"

    // Loot table IDs for each distance
    private const val NEAR_LOOT_TABLE = "loot-table-fishing-near"
    private const val MID_LOOT_TABLE = "loot-table-fishing-mid"
    private const val FAR_LOOT_TABLE = "loot-table-fishing-far"

    // Fishing rod bonus
    private const val FISHING_ROD_BONUS = 20  // 20% bonus with fishing rod
    private const val FISHING_BADGE_BONUS = 15  // 15% bonus with fishing badge

    // Fish needed for badge
    private const val FISH_FOR_BADGE = 10

    /**
     * Fishing distance options.
     */
    enum class FishingDistance(val lootTableId: String) {
        NEAR(NEAR_LOOT_TABLE),
        MID(MID_LOOT_TABLE),
        FAR(FAR_LOOT_TABLE)
    }

    /**
     * Result of a fishing attempt.
     */
    data class FishingResult(
        val success: Boolean,
        val message: String,
        val fishCaught: Item? = null,
        val manaRestored: Int = 0,
        val totalFishCaught: Int = 0,
        val earnedBadge: Boolean = false
    )

    /**
     * Information about fishing capabilities for a user.
     */
    data class FishingInfo(
        val canFish: Boolean,
        val reason: String?,
        val nearEnabled: Boolean,
        val midEnabled: Boolean,
        val farEnabled: Boolean,
        val midStrRequired: Int,
        val farStrRequired: Int,
        val currentStr: Int,
        val successChance: Int,
        val durationMs: Long,
        val staminaCost: Int,
        val manaCost: Int
    )

    /**
     * Check if a location is a fishing location.
     */
    fun isFishingLocation(location: Location): Boolean {
        return location.featureIds.contains(FISHING_FEATURE_ID)
    }

    /**
     * Check if a location ID is a fishing location.
     */
    fun isFishingLocation(locationId: String): Boolean {
        val location = LocationRepository.findById(locationId) ?: return false
        return isFishingLocation(location)
    }

    /**
     * Check if user has a fishing rod in inventory.
     */
    fun hasFishingRod(user: User): Boolean {
        return user.itemIds.contains(FishingSeed.FISHING_ROD_ID)
    }

    /**
     * Check if user has the fishing badge ability.
     */
    fun hasFishingBadge(user: User): Boolean {
        return user.learnedAbilityIds.contains(FishingSeed.FISHING_BADGE_ID)
    }

    /**
     * Calculate total fishing bonus from equipment and abilities.
     */
    fun getFishingBonus(user: User): Int {
        var bonus = 0
        if (hasFishingRod(user)) bonus += FISHING_ROD_BONUS
        if (hasFishingBadge(user)) bonus += FISHING_BADGE_BONUS
        return bonus
    }

    /**
     * Calculate total success chance including bonuses.
     */
    fun calculateSuccessChance(user: User): Int {
        val baseChance = StatModifierService.fishingSuccessChance(
            user.dexterity, user.intelligence, user.level
        )
        val bonus = getFishingBonus(user)
        return (baseChance + bonus).coerceIn(20, 95)
    }

    /**
     * Check if user can cast to a specific distance.
     */
    fun canCastDistance(user: User, distance: FishingDistance): Boolean {
        return when (distance) {
            FishingDistance.NEAR -> true
            FishingDistance.MID -> user.strength >= MID_STR_REQUIREMENT
            FishingDistance.FAR -> user.strength >= FAR_STR_REQUIREMENT
        }
    }

    /**
     * Get fishing information for a user.
     */
    fun getFishingInfo(user: User, locationId: String): FishingInfo {
        // Check if location is a fishing spot
        if (!isFishingLocation(locationId)) {
            return FishingInfo(
                canFish = false,
                reason = "This is not a fishing location.",
                nearEnabled = false,
                midEnabled = false,
                farEnabled = false,
                midStrRequired = MID_STR_REQUIREMENT,
                farStrRequired = FAR_STR_REQUIREMENT,
                currentStr = user.strength,
                successChance = 0,
                durationMs = 0,
                staminaCost = STAMINA_COST,
                manaCost = MANA_COST
            )
        }

        // Check if in combat
        if (user.currentCombatSessionId != null) {
            return FishingInfo(
                canFish = false,
                reason = "You cannot fish while in combat!",
                nearEnabled = false,
                midEnabled = false,
                farEnabled = false,
                midStrRequired = MID_STR_REQUIREMENT,
                farStrRequired = FAR_STR_REQUIREMENT,
                currentStr = user.strength,
                successChance = 0,
                durationMs = 0,
                staminaCost = STAMINA_COST,
                manaCost = MANA_COST
            )
        }

        // Check resources
        if (user.currentStamina < STAMINA_COST) {
            return FishingInfo(
                canFish = false,
                reason = "Not enough stamina to fish (need $STAMINA_COST).",
                nearEnabled = false,
                midEnabled = false,
                farEnabled = false,
                midStrRequired = MID_STR_REQUIREMENT,
                farStrRequired = FAR_STR_REQUIREMENT,
                currentStr = user.strength,
                successChance = 0,
                durationMs = 0,
                staminaCost = STAMINA_COST,
                manaCost = MANA_COST
            )
        }

        if (user.currentMana < MANA_COST) {
            return FishingInfo(
                canFish = false,
                reason = "Not enough mana to fish (need $MANA_COST).",
                nearEnabled = false,
                midEnabled = false,
                farEnabled = false,
                midStrRequired = MID_STR_REQUIREMENT,
                farStrRequired = FAR_STR_REQUIREMENT,
                currentStr = user.strength,
                successChance = 0,
                durationMs = 0,
                staminaCost = STAMINA_COST,
                manaCost = MANA_COST
            )
        }

        return FishingInfo(
            canFish = true,
            reason = null,
            nearEnabled = true,
            midEnabled = canCastDistance(user, FishingDistance.MID),
            farEnabled = canCastDistance(user, FishingDistance.FAR),
            midStrRequired = MID_STR_REQUIREMENT,
            farStrRequired = FAR_STR_REQUIREMENT,
            currentStr = user.strength,
            successChance = calculateSuccessChance(user),
            durationMs = StatModifierService.fishingDurationMs(user.wisdom, user.level),
            staminaCost = STAMINA_COST,
            manaCost = MANA_COST
        )
    }

    /**
     * Attempt to fish at the user's current location.
     */
    fun attemptFishing(user: User, distance: FishingDistance): FishingResult {
        // Verify location
        val locationId = user.currentLocationId ?: return FishingResult(
            success = false,
            message = "You are not at a valid location."
        )
        if (!isFishingLocation(locationId)) {
            return FishingResult(
                success = false,
                message = "This is not a fishing location."
            )
        }

        // Check combat
        if (user.currentCombatSessionId != null) {
            return FishingResult(
                success = false,
                message = "You cannot fish while in combat!"
            )
        }

        // Check if user can cast to this distance
        if (!canCastDistance(user, distance)) {
            val required = when (distance) {
                FishingDistance.MID -> MID_STR_REQUIREMENT
                FishingDistance.FAR -> FAR_STR_REQUIREMENT
                else -> 0
            }
            return FishingResult(
                success = false,
                message = "You need at least $required STR to cast that far!"
            )
        }

        // Check and spend resources
        if (user.currentStamina < STAMINA_COST) {
            return FishingResult(
                success = false,
                message = "Not enough stamina to fish (need $STAMINA_COST)."
            )
        }
        if (user.currentMana < MANA_COST) {
            return FishingResult(
                success = false,
                message = "Not enough mana to fish (need $MANA_COST)."
            )
        }

        // Spend resources
        UserRepository.spendStamina(user.id, STAMINA_COST)
        UserRepository.spendMana(user.id, MANA_COST)

        // Calculate success (includes bonuses from fishing rod and badge)
        val successChance = calculateSuccessChance(user)
        val roll = Random.nextInt(100)
        val caught = roll < successChance

        val hasRod = hasFishingRod(user)
        val hasBadge = hasFishingBadge(user)
        log.debug(
            "${user.name} fishing at $distance: rolled $roll vs $successChance% " +
            "(DEX=${user.dexterity}, INT=${user.intelligence}, level=${user.level}, " +
            "rod=$hasRod, badge=$hasBadge)"
        )

        if (!caught) {
            return FishingResult(
                success = false,
                message = "The fish got away!"
            )
        }

        // Select fish from loot table
        val lootTable = LootTableRepository.findById(distance.lootTableId)
        if (lootTable == null) {
            log.error("Loot table ${distance.lootTableId} not found!")
            return FishingResult(
                success = false,
                message = "Something went wrong with the fishing spot."
            )
        }

        val fish = selectFromLootTable(lootTable)
        if (fish == null) {
            log.error("Failed to select fish from loot table ${distance.lootTableId}")
            return FishingResult(
                success = false,
                message = "The fish slipped off the hook!"
            )
        }

        // Add fish to inventory
        UserRepository.addItems(user.id, listOf(fish.id))

        // Restore mana
        val manaRestored = Random.nextInt(MANA_RESTORE_MIN, MANA_RESTORE_MAX + 1)
        UserRepository.restoreMana(user.id, manaRestored)

        // Increment fish caught counter
        val totalFishCaught = UserRepository.incrementFishCaught(user.id)

        // Check if user earned the fishing badge (at 10 fish)
        var earnedBadge = false
        if (totalFishCaught == FISH_FOR_BADGE && !hasFishingBadge(user)) {
            // Grant the fishing badge ability
            UserRepository.learnAbility(user.id, FishingSeed.FISHING_BADGE_ID)
            earnedBadge = true
            log.info("${user.name} earned the Angler's Badge after catching $totalFishCaught fish!")
        }

        val sizeDesc = when {
            fish.weight >= 6 -> "massive"
            fish.weight >= 4 -> "large"
            fish.weight >= 2 -> "nice"
            else -> "small"
        }

        log.info("${user.name} caught a ${fish.name} (weight=${fish.weight}, value=${fish.value}, total=$totalFishCaught)")

        return FishingResult(
            success = true,
            message = "You caught a $sizeDesc ${fish.name}!",
            fishCaught = fish,
            manaRestored = manaRestored,
            totalFishCaught = totalFishCaught,
            earnedBadge = earnedBadge
        )
    }

    /**
     * Select an item from a loot table based on weighted random selection.
     */
    private fun selectFromLootTable(lootTable: LootTableData): Item? {
        if (lootTable.entries.isEmpty()) return null

        val totalWeight = lootTable.entries.sumOf { it.chance.toDouble() }
        var roll = Random.nextDouble() * totalWeight

        for (entry in lootTable.entries) {
            roll -= entry.chance
            if (roll <= 0) {
                return ItemRepository.findById(entry.itemId)
            }
        }

        // Fallback to last entry
        return ItemRepository.findById(lootTable.entries.last().itemId)
    }

    /**
     * Get fishing duration for a user.
     */
    fun getFishingDurationMs(user: User): Long {
        return StatModifierService.fishingDurationMs(user.wisdom, user.level)
    }
}
