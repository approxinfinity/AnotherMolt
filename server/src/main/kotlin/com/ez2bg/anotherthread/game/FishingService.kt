package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlin.random.Random

/**
 * Service for handling fishing mechanics with Stardew Valley-style minigame.
 *
 * Players can fish at:
 * - Freshwater spots (lakes, rivers) with "feature-fishing-spot"
 * - Coastal spots (ocean) with "feature-coastal-fishing"
 *
 * The fishing minigame works like Stardew Valley:
 * - Fish moves up and down on a vertical bar
 * - Player controls a "catch zone" that can slide up and down
 * - Score starts at 50, increases when fish is in zone, decreases when outside
 * - Reach 100 to catch, hit 0 to lose the fish
 *
 * Fish difficulty is based on value/rarity - expensive fish move faster/more erratically.
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

    // Feature IDs for fishing spots
    private const val FRESHWATER_FISHING_FEATURE = "feature-fishing-spot"
    private const val COASTAL_FISHING_FEATURE = "feature-coastal-fishing"

    // Freshwater loot table IDs
    private const val NEAR_LOOT_TABLE = "loot-table-fishing-near"
    private const val MID_LOOT_TABLE = "loot-table-fishing-mid"
    private const val FAR_LOOT_TABLE = "loot-table-fishing-far"

    // Coastal loot table IDs
    private const val COASTAL_NEAR_LOOT_TABLE = "loot-table-coastal-near"
    private const val COASTAL_MID_LOOT_TABLE = "loot-table-coastal-mid"
    private const val COASTAL_FAR_LOOT_TABLE = "loot-table-coastal-far"

    // Fishing rod bonus
    private const val FISHING_ROD_BONUS = 20  // 20% bonus with fishing rod
    private const val FISHING_BADGE_BONUS = 15  // 15% bonus with fishing badge

    // Fish needed for badge
    private const val FISH_FOR_BADGE = 10

    // Minigame constants
    private const val MINIGAME_STARTING_SCORE = 50
    private const val MINIGAME_CATCH_THRESHOLD = 100
    private const val MINIGAME_ESCAPE_THRESHOLD = 0
    private const val MINIGAME_DURATION_MS = 15000L  // 15 seconds max

    /**
     * Type of water for fishing.
     */
    enum class WaterType {
        FRESHWATER,
        COASTAL
    }

    /**
     * Fishing distance options.
     */
    enum class FishingDistance {
        NEAR, MID, FAR;

        fun getLootTableId(waterType: WaterType): String = when (waterType) {
            WaterType.FRESHWATER -> when (this) {
                NEAR -> NEAR_LOOT_TABLE
                MID -> MID_LOOT_TABLE
                FAR -> FAR_LOOT_TABLE
            }
            WaterType.COASTAL -> when (this) {
                NEAR -> COASTAL_NEAR_LOOT_TABLE
                MID -> COASTAL_MID_LOOT_TABLE
                FAR -> COASTAL_FAR_LOOT_TABLE
            }
        }
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
        val manaCost: Int,
        val waterType: String = "freshwater",  // "freshwater" or "coastal"
        val isCoastal: Boolean = false
    )

    /**
     * Data for starting a fishing minigame session.
     * Client uses this to set up the minigame UI.
     */
    data class FishingMinigameStart(
        val sessionId: String,
        val fishName: String,
        val fishDifficulty: Int,  // 1-10, affects fish movement speed/erraticness
        val catchZoneSize: Int,   // Size of player's catch zone (affected by skill/rod)
        val durationMs: Long,     // Total time for minigame
        val startingScore: Int = MINIGAME_STARTING_SCORE
    )

    /**
     * Fish behavior parameters for the minigame.
     * Higher difficulty = faster movement, more direction changes.
     */
    data class FishBehavior(
        val speed: Float,           // Movement speed (0.0-1.0 of bar per second)
        val changeDirectionChance: Float,  // Chance per tick to change direction
        val erraticness: Float      // Randomness in movement (0.0-1.0)
    )

    /**
     * Calculate fish difficulty based on value (rarer = harder).
     */
    private fun calculateFishDifficulty(fish: Item): Int {
        return when {
            fish.value >= 500 -> 10  // Legendary
            fish.value >= 300 -> 9
            fish.value >= 200 -> 8
            fish.value >= 150 -> 7
            fish.value >= 100 -> 6
            fish.value >= 60 -> 5
            fish.value >= 40 -> 4
            fish.value >= 25 -> 3
            fish.value >= 15 -> 2
            else -> 1
        }
    }

    /**
     * Get fish behavior based on difficulty.
     */
    fun getFishBehavior(difficulty: Int): FishBehavior {
        val baseSpeed = 0.1f + (difficulty * 0.08f)  // 0.18 to 0.9 of bar per second
        val changeChance = 0.02f + (difficulty * 0.01f)  // 3% to 12% per tick
        val erraticness = 0.1f + (difficulty * 0.08f)  // 0.18 to 0.9

        return FishBehavior(
            speed = baseSpeed.coerceIn(0.1f, 1.0f),
            changeDirectionChance = changeChance.coerceIn(0.01f, 0.15f),
            erraticness = erraticness.coerceIn(0.1f, 0.9f)
        )
    }

    /**
     * Calculate catch zone size based on user skill and equipment.
     * Base is 25% of bar, can go up to 40% with bonuses.
     */
    fun calculateCatchZoneSize(user: User): Int {
        var baseSize = 25  // Base 25% of bar

        // DEX bonus (up to +5%)
        val dexMod = StatModifierService.attributeModifier(user.dexterity)
        baseSize += (dexMod * 2).coerceIn(0, 5)

        // Fishing rod bonus (+5%)
        if (hasFishingRod(user)) baseSize += 5

        // Badge bonus (+3%)
        if (hasFishingBadge(user)) baseSize += 3

        // Level bonus (+1% per 5 levels, max +4%)
        baseSize += (user.level / 5).coerceIn(0, 4)

        return baseSize.coerceIn(20, 40)
    }

    /**
     * Check if a location is a fishing location (freshwater or coastal).
     */
    fun isFishingLocation(location: Location): Boolean {
        return location.featureIds.contains(FRESHWATER_FISHING_FEATURE) ||
               location.featureIds.contains(COASTAL_FISHING_FEATURE)
    }

    /**
     * Check if a location ID is a fishing location.
     */
    fun isFishingLocation(locationId: String): Boolean {
        val location = LocationRepository.findById(locationId) ?: return false
        return isFishingLocation(location)
    }

    /**
     * Check if a location has coastal (saltwater) fishing.
     */
    fun isCoastalFishing(location: Location): Boolean {
        return location.featureIds.contains(COASTAL_FISHING_FEATURE)
    }

    /**
     * Get the water type for a location.
     */
    fun getWaterType(location: Location): WaterType {
        return if (isCoastalFishing(location)) WaterType.COASTAL else WaterType.FRESHWATER
    }

    /**
     * Get the water type for a location ID.
     */
    fun getWaterType(locationId: String): WaterType {
        val location = LocationRepository.findById(locationId) ?: return WaterType.FRESHWATER
        return getWaterType(location)
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

        // Determine water type
        val waterType = getWaterType(locationId)
        val isCoastal = waterType == WaterType.COASTAL

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
            durationMs = MINIGAME_DURATION_MS,  // Now uses minigame duration
            staminaCost = STAMINA_COST,
            manaCost = MANA_COST,
            waterType = if (isCoastal) "coastal" else "freshwater",
            isCoastal = isCoastal
        )
    }

    /**
     * Start a fishing minigame session.
     * Returns the fish that will be caught if successful, along with minigame parameters.
     */
    fun startFishingMinigame(user: User, distance: FishingDistance): Result<FishingMinigameStart> {
        // Verify location and resources
        val locationId = user.currentLocationId
            ?: return Result.failure(IllegalStateException("Not at a valid location"))

        if (!isFishingLocation(locationId)) {
            return Result.failure(IllegalStateException("Not a fishing location"))
        }

        if (user.currentCombatSessionId != null) {
            return Result.failure(IllegalStateException("Cannot fish in combat"))
        }

        if (!canCastDistance(user, distance)) {
            return Result.failure(IllegalStateException("Insufficient STR for this distance"))
        }

        if (user.currentStamina < STAMINA_COST) {
            return Result.failure(IllegalStateException("Not enough stamina"))
        }

        if (user.currentMana < MANA_COST) {
            return Result.failure(IllegalStateException("Not enough mana"))
        }

        // Spend resources upfront
        UserRepository.spendStamina(user.id, STAMINA_COST)
        UserRepository.spendMana(user.id, MANA_COST)

        // Determine water type and select fish
        val waterType = getWaterType(locationId)
        val lootTableId = distance.getLootTableId(waterType)
        val lootTable = LootTableRepository.findById(lootTableId)
            ?: return Result.failure(IllegalStateException("Loot table not found"))

        val fish = selectFromLootTable(lootTable)
            ?: return Result.failure(IllegalStateException("Failed to select fish"))

        // Calculate fish difficulty and catch zone size
        val difficulty = calculateFishDifficulty(fish)
        val catchZoneSize = calculateCatchZoneSize(user)

        // Generate session ID for tracking
        val sessionId = java.util.UUID.randomUUID().toString()

        // Store pending session (fish will be awarded after minigame completes)
        pendingFishingSessions[sessionId] = PendingFishingSession(
            userId = user.id,
            fishId = fish.id,
            fishName = fish.name,
            fishValue = fish.value,
            fishWeight = fish.weight,
            startTime = System.currentTimeMillis()
        )

        log.debug("${user.name} started fishing minigame for ${fish.name} (difficulty=$difficulty, catchZone=$catchZoneSize)")

        return Result.success(FishingMinigameStart(
            sessionId = sessionId,
            fishName = fish.name,
            fishDifficulty = difficulty,
            catchZoneSize = catchZoneSize,
            durationMs = MINIGAME_DURATION_MS,
            startingScore = MINIGAME_STARTING_SCORE
        ))
    }

    /**
     * Pending fishing session data.
     */
    data class PendingFishingSession(
        val userId: String,
        val fishId: String,
        val fishName: String,
        val fishValue: Int,
        val fishWeight: Int,
        val startTime: Long
    )

    // Store for pending fishing sessions (sessionId -> session)
    private val pendingFishingSessions = mutableMapOf<String, PendingFishingSession>()

    /**
     * Complete a fishing minigame session.
     * Called when client reports the minigame result.
     *
     * @param sessionId The session ID from startFishingMinigame
     * @param finalScore The final score (0-100)
     * @return FishingResult with catch details
     */
    fun completeFishingMinigame(sessionId: String, finalScore: Int): FishingResult {
        val session = pendingFishingSessions.remove(sessionId)
            ?: return FishingResult(false, "Invalid or expired fishing session")

        // Check if session expired (30 seconds max)
        val elapsed = System.currentTimeMillis() - session.startTime
        if (elapsed > 30000) {
            return FishingResult(false, "Fishing session expired")
        }

        val user = UserRepository.findById(session.userId)
            ?: return FishingResult(false, "User not found")

        // Check if caught (score >= 100)
        val caught = finalScore >= MINIGAME_CATCH_THRESHOLD

        if (!caught) {
            log.debug("${user.name} failed to catch ${session.fishName} (score=$finalScore)")
            return FishingResult(
                success = false,
                message = "The ${session.fishName} got away! (Score: $finalScore/100)"
            )
        }

        // Fish was caught!
        val fish = ItemRepository.findById(session.fishId)
            ?: return FishingResult(false, "Fish item not found")

        // Add fish to food inventory
        FoodService.addFishToInventory(session.userId, fish.id)

        // Restore mana
        val manaRestored = Random.nextInt(MANA_RESTORE_MIN, MANA_RESTORE_MAX + 1)
        UserRepository.restoreMana(session.userId, manaRestored)

        // Increment fish caught counter
        val totalFishCaught = UserRepository.incrementFishCaught(session.userId)

        // Check for badge
        var earnedBadge = false
        if (totalFishCaught == FISH_FOR_BADGE && !hasFishingBadge(user)) {
            UserRepository.learnAbility(session.userId, FishingSeed.FISHING_BADGE_ID)
            earnedBadge = true
            log.info("${user.name} earned the Angler's Badge after catching $totalFishCaught fish!")
        }

        val sizeDesc = when {
            fish.weight >= 6 -> "massive"
            fish.weight >= 4 -> "large"
            fish.weight >= 2 -> "nice"
            else -> "small"
        }

        log.info("${user.name} caught a ${fish.name} via minigame (score=$finalScore, weight=${fish.weight}, value=${fish.value})")

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

        // Select fish from loot table (based on water type)
        val waterType = getWaterType(locationId)
        val lootTableId = distance.getLootTableId(waterType)
        val lootTable = LootTableRepository.findById(lootTableId)
        if (lootTable == null) {
            log.error("Loot table $lootTableId not found!")
            return FishingResult(
                success = false,
                message = "Something went wrong with the fishing spot."
            )
        }

        val fish = selectFromLootTable(lootTable)
        if (fish == null) {
            log.error("Failed to select fish from loot table $lootTableId")
            return FishingResult(
                success = false,
                message = "The fish slipped off the hook!"
            )
        }

        // Add fish to food inventory with spoil tracking
        FoodService.addFishToInventory(user.id, fish.id)

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
