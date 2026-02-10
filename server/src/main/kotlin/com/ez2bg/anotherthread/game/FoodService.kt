package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlin.random.Random

/**
 * Service for handling food mechanics including eating, cooking, and salting.
 *
 * Food items can be:
 * - Raw: Eating has a chance of sickness, spoils in 1 day
 * - Cooked: Safe to eat, restores more HP/mana, spoils in 7 days
 * - Salted: Safe to eat, moderate restoration, spoils in 3 months
 *
 * Cooking requires being at a location with a fire/hearth.
 * Salting requires salt in inventory.
 */
object FoodService {
    private val log = org.slf4j.LoggerFactory.getLogger("FoodService")

    // Item IDs
    const val SALT_ID = "item-salt"

    // Feature IDs for cooking
    private const val COOKING_FIRE_FEATURE = "feature-cooking-fire"
    private const val HEARTH_FEATURE = "feature-hearth"

    // Sickness chance for eating raw food
    private const val RAW_SICKNESS_CHANCE = 25  // 25% chance

    // HP restoration amounts
    private const val RAW_HP_RESTORE_MIN = 2
    private const val RAW_HP_RESTORE_MAX = 5
    private const val COOKED_HP_RESTORE_MIN = 8
    private const val COOKED_HP_RESTORE_MAX = 15
    private const val SALTED_HP_RESTORE_MIN = 5
    private const val SALTED_HP_RESTORE_MAX = 10

    // Sickness debuff duration (in game ticks, assuming 1 tick = 1 second)
    private const val SICKNESS_DURATION_TICKS = 60  // 1 minute

    // Fish item IDs (from FishingSeed and CoastalFishingSeed)
    // Rather than maintaining a hardcoded list, we check if the item ID starts with "fish-"
    private fun isFishItemId(itemId: String): Boolean = itemId.startsWith("fish-")

    /**
     * Result of eating food.
     */
    data class EatResult(
        val success: Boolean,
        val message: String,
        val hpRestored: Int = 0,
        val gotSick: Boolean = false
    )

    /**
     * Result of cooking food.
     */
    data class CookResult(
        val success: Boolean,
        val message: String,
        val newSpoilTime: String? = null
    )

    /**
     * Result of salting food.
     */
    data class SaltResult(
        val success: Boolean,
        val message: String,
        val newSpoilTime: String? = null
    )

    /**
     * Check if an item is a fish/food item.
     */
    fun isFoodItem(itemId: String): Boolean {
        return isFishItemId(itemId)
    }

    /**
     * Check if a location has cooking facilities.
     */
    fun canCookAt(locationId: String): Boolean {
        val location = LocationRepository.findById(locationId) ?: return false
        return location.featureIds.contains(COOKING_FIRE_FEATURE) ||
               location.featureIds.contains(HEARTH_FEATURE)
    }

    /**
     * Check if user has salt in their inventory.
     */
    fun hasSalt(user: User): Boolean {
        return user.itemIds.contains(SALT_ID)
    }

    /**
     * Eat a food item from inventory.
     *
     * @param user The user eating the food
     * @param foodItemId The UserFoodItem ID to eat
     * @return EatResult with details
     */
    fun eatFood(user: User, foodItemId: String): EatResult {
        val foodItem = UserFoodItemRepository.findById(foodItemId)
            ?: return EatResult(false, "Food item not found.")

        if (foodItem.userId != user.id) {
            return EatResult(false, "That's not your food!")
        }

        val item = ItemRepository.findById(foodItem.itemId)
            ?: return EatResult(false, "Unknown food type.")

        // Check if spoiled
        if (foodItem.isSpoiled()) {
            // Remove the spoiled food
            UserFoodItemRepository.delete(foodItemId)
            return EatResult(false, "The ${item.name} has spoiled and is inedible!")
        }

        // Calculate HP restoration based on state
        val (hpMin, hpMax) = when (foodItem.state) {
            "raw" -> RAW_HP_RESTORE_MIN to RAW_HP_RESTORE_MAX
            "cooked" -> COOKED_HP_RESTORE_MIN to COOKED_HP_RESTORE_MAX
            "salted" -> SALTED_HP_RESTORE_MIN to SALTED_HP_RESTORE_MAX
            else -> RAW_HP_RESTORE_MIN to RAW_HP_RESTORE_MAX
        }

        // Scale by fish weight (bigger fish = more food)
        val weightMultiplier = (item.weight.coerceIn(1, 6) / 2.0).coerceAtLeast(1.0)
        val baseHp = Random.nextInt(hpMin, hpMax + 1)
        val hpRestored = (baseHp * weightMultiplier).toInt()

        // Check for sickness from raw food
        val gotSick = if (foodItem.state == "raw") {
            Random.nextInt(100) < RAW_SICKNESS_CHANCE
        } else {
            false
        }

        // Remove the food item (consumed)
        UserFoodItemRepository.delete(foodItemId)

        // Restore HP
        UserRepository.heal(user.id, hpRestored)

        // Build message
        val stateDesc = when (foodItem.state) {
            "cooked" -> "cooked "
            "salted" -> "salted "
            else -> "raw "
        }

        return if (gotSick) {
            // TODO: Apply sickness debuff (reduce stamina regen, small HP drain)
            log.info("${user.name} ate raw ${item.name} and got sick!")
            EatResult(
                success = true,
                message = "You eat the $stateDesc${item.name} and restore $hpRestored HP, but you feel sick from eating raw food!",
                hpRestored = hpRestored,
                gotSick = true
            )
        } else {
            log.info("${user.name} ate $stateDesc${item.name}, restored $hpRestored HP")
            EatResult(
                success = true,
                message = "You eat the $stateDesc${item.name} and restore $hpRestored HP.",
                hpRestored = hpRestored,
                gotSick = false
            )
        }
    }

    /**
     * Cook a raw food item.
     *
     * @param user The user cooking
     * @param foodItemId The UserFoodItem ID to cook
     * @return CookResult with details
     */
    fun cookFood(user: User, foodItemId: String): CookResult {
        val foodItem = UserFoodItemRepository.findById(foodItemId)
            ?: return CookResult(false, "Food item not found.")

        if (foodItem.userId != user.id) {
            return CookResult(false, "That's not your food!")
        }

        val item = ItemRepository.findById(foodItem.itemId)
            ?: return CookResult(false, "Unknown food type.")

        // Check if at a cooking location
        val locationId = user.currentLocationId
            ?: return CookResult(false, "You need to be at a location to cook.")

        if (!canCookAt(locationId)) {
            return CookResult(false, "There's no fire or hearth here to cook with.")
        }

        // Check if already cooked or salted
        if (foodItem.state != "raw") {
            return CookResult(false, "This ${item.name} is already ${foodItem.state}.")
        }

        // Check if spoiled
        if (foodItem.isSpoiled()) {
            UserFoodItemRepository.delete(foodItemId)
            return CookResult(false, "The ${item.name} has spoiled and cannot be cooked!")
        }

        // Cook it - update state and extend spoil time
        val now = System.currentTimeMillis()
        val newSpoilAt = now + UserFoodItem.COOKED_SPOIL_MS

        val updatedFoodItem = foodItem.copy(
            state = "cooked",
            spoilAt = newSpoilAt
        )

        UserFoodItemRepository.update(updatedFoodItem)

        log.info("${user.name} cooked ${item.name}")

        return CookResult(
            success = true,
            message = "You cook the ${item.name}. It will now stay fresh for ${updatedFoodItem.getTimeUntilSpoil()}.",
            newSpoilTime = updatedFoodItem.getTimeUntilSpoil()
        )
    }

    /**
     * Salt a raw food item for preservation.
     *
     * @param user The user salting
     * @param foodItemId The UserFoodItem ID to salt
     * @return SaltResult with details
     */
    fun saltFood(user: User, foodItemId: String): SaltResult {
        val foodItem = UserFoodItemRepository.findById(foodItemId)
            ?: return SaltResult(false, "Food item not found.")

        if (foodItem.userId != user.id) {
            return SaltResult(false, "That's not your food!")
        }

        val item = ItemRepository.findById(foodItem.itemId)
            ?: return SaltResult(false, "Unknown food type.")

        // Check if user has salt
        if (!hasSalt(user)) {
            return SaltResult(false, "You need salt to preserve food.")
        }

        // Check if already salted
        if (foodItem.state == "salted") {
            return SaltResult(false, "This ${item.name} is already salted.")
        }

        // Can salt raw or cooked food
        if (foodItem.state != "raw" && foodItem.state != "cooked") {
            return SaltResult(false, "This ${item.name} cannot be salted.")
        }

        // Check if spoiled
        if (foodItem.isSpoiled()) {
            UserFoodItemRepository.delete(foodItemId)
            return SaltResult(false, "The ${item.name} has spoiled and cannot be salted!")
        }

        // Consume one salt
        UserRepository.removeItems(user.id, listOf(SALT_ID))

        // Salt it - update state and extend spoil time
        val now = System.currentTimeMillis()
        val newSpoilAt = now + UserFoodItem.SALTED_SPOIL_MS

        val updatedFoodItem = foodItem.copy(
            state = "salted",
            spoilAt = newSpoilAt
        )

        UserFoodItemRepository.update(updatedFoodItem)

        log.info("${user.name} salted ${item.name}")

        return SaltResult(
            success = true,
            message = "You salt the ${item.name}. It will now stay fresh for ${updatedFoodItem.getTimeUntilSpoil()}.",
            newSpoilTime = updatedFoodItem.getTimeUntilSpoil()
        )
    }

    /**
     * Add a newly caught fish to user's food inventory.
     */
    fun addFishToInventory(userId: String, fishItemId: String): UserFoodItem {
        val now = System.currentTimeMillis()
        val foodItem = UserFoodItem(
            userId = userId,
            itemId = fishItemId,
            acquiredAt = now,
            spoilAt = now + UserFoodItem.RAW_SPOIL_MS,
            state = "raw"
        )
        return UserFoodItemRepository.add(foodItem)
    }

    /**
     * Get all food items for a user with their current state.
     */
    fun getUserFoodItems(userId: String): List<UserFoodItem> {
        return UserFoodItemRepository.findFreshByUserId(userId)
    }

    /**
     * Check and remove any spoiled food items for a user.
     * Returns the count of removed items.
     */
    fun cleanupSpoiledFood(userId: String): Int {
        val count = UserFoodItemRepository.deleteSpoiled(userId)
        if (count > 0) {
            log.debug("Removed $count spoiled food items for user $userId")
        }
        return count
    }
}
