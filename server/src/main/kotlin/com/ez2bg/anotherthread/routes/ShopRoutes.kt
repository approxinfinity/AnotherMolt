package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.game.FoodService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Serializable
data class BuyItemRequest(val userId: String, val itemId: String)

@Serializable
data class SellItemRequest(val userId: String, val itemId: String)

@Serializable
data class SellFoodItemRequest(val userId: String, val foodItemId: String)

@Serializable
data class RestRequest(val userId: String)

@Serializable
data class ShopBanResponse(
    val isBanned: Boolean,
    val message: String? = null,
    val banExpiresAt: Long? = null
)

// Cursed item IDs that the shopkeeper refuses to buy
private val CURSED_ITEM_IDS = setOf(
    UndeadCryptSeed.ANCIENT_TOMB_GOLD_ID
)

/**
 * Get the shop ban key for a user.
 */
private fun shopBanKey(userId: String) = "shop.ban.$userId"

/**
 * Check if a user is banned from the general store.
 * Returns the ban expiry timestamp if banned, null if not banned.
 */
private fun getShopBanExpiry(userId: String): Long? {
    val banExpiry = GameConfigRepository.getLong(shopBanKey(userId), 0L)
    return if (banExpiry > System.currentTimeMillis()) banExpiry else null
}

/**
 * Ban a user from the general store until tomorrow (midnight local time).
 */
private fun banUserFromShop(userId: String) {
    // Calculate midnight tomorrow
    val tomorrow = Instant.now()
        .atZone(ZoneId.systemDefault())
        .plusDays(1)
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant()
        .toEpochMilli()

    GameConfigRepository.setLong(
        shopBanKey(userId),
        tomorrow,
        "Shop ban for attempting to sell cursed items",
        "shop"
    )
}

/**
 * Check if user has any cursed items in inventory.
 */
private fun hasCursedItems(user: User): Boolean {
    return user.itemIds.any { it in CURSED_ITEM_IDS }
}

@Serializable
data class SellableItemDto(
    val id: String,
    val itemId: String,
    val name: String,
    val sellValue: Int,
    val isFoodItem: Boolean = false,
    val foodState: String? = null,
    val timeUntilSpoil: String? = null
)

@Serializable
data class SellableItemsResponse(
    val success: Boolean,
    val items: List<SellableItemDto>
)

@Serializable
data class ShopActionResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse? = null
)

/**
 * Calculate charisma-based discount percentage.
 * Each point of CHA modifier (above 10) gives 3% discount, max 20%.
 * Low charisma doesn't penalize (minimum 0% discount).
 */
private fun calculateCharismaDiscount(charisma: Int): Int {
    val modifier = (charisma - 10) / 2
    return (modifier * 3).coerceIn(0, 20)  // 0-20% discount
}

/**
 * Apply charisma discount to a price.
 */
private fun applyCharismaDiscount(basePrice: Int, charisma: Int): Int {
    val discountPercent = calculateCharismaDiscount(charisma)
    val discount = (basePrice * discountPercent) / 100
    return (basePrice - discount).coerceAtLeast(1)  // Minimum 1g
}

fun Route.shopRoutes() {
    route("/shop") {
        // Check if user is banned from the general store
        get("/{locationId}/ban-status/{userId}") {
            val locationId = call.parameters["locationId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ShopBanResponse(false))
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ShopBanResponse(false))

            // Only applies to general store
            if (locationId != GeneralStoreSeed.GENERAL_STORE_ID) {
                return@get call.respond(ShopBanResponse(isBanned = false))
            }

            val user = UserRepository.findById(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ShopBanResponse(false))

            val banExpiry = getShopBanExpiry(userId)
            if (banExpiry != null) {
                // Still banned - check if they still have cursed items
                val hasCursed = hasCursedItems(user)
                val message = if (hasCursed) {
                    "The shopkeeper eyes you suspiciously. \"Come back tomorrow, and don't bring that cursed gold!\""
                } else {
                    "The shopkeeper waves you away. \"I said come back tomorrow! I need time to recover from that fright.\""
                }
                return@get call.respond(ShopBanResponse(
                    isBanned = true,
                    message = message,
                    banExpiresAt = banExpiry
                ))
            }

            call.respond(ShopBanResponse(isBanned = false))
        }

        // Buy an item from a shop location
        post("/{locationId}/buy") {
            val locationId = call.parameters["locationId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Missing location ID"))

            val request = call.receive<BuyItemRequest>()

            // Get the user first (needed for charisma discount)
            val user = UserRepository.findById(request.userId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ShopActionResponse(false, "User not found"))

            // Validate location exists and has the item
            val location = LocationRepository.findById(locationId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ShopActionResponse(false, "Location not found"))

            if (request.itemId !in location.itemIds) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Item not available at this location"))
            }

            // Look up item for its price
            val item = ItemRepository.findById(request.itemId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ShopActionResponse(false, "Item not found"))

            if (item.value <= 0) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Item is not for sale"))
            }

            // Calculate price with charisma discount
            val basePrice = item.value
            val finalPrice = applyCharismaDiscount(basePrice, user.charisma)
            val savings = basePrice - finalPrice

            // Spend gold
            val goldSpent = UserRepository.spendGold(request.userId, finalPrice)
            if (!goldSpent) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Not enough gold (need ${finalPrice}g)"))
            }

            // Give item to user
            UserRepository.addItems(request.userId, listOf(request.itemId))

            // Return updated user with purchase message
            val updatedUser = UserRepository.findById(request.userId)
            val message = if (savings > 0) {
                "Purchased ${item.name} for ${finalPrice}g (saved ${savings}g with your charm!)"
            } else {
                "Purchased ${item.name} for ${finalPrice}g"
            }
            call.respond(ShopActionResponse(
                success = true,
                message = message,
                user = updatedUser?.toResponse()
            ))
        }

        // Rest at the inn
        post("/{locationId}/rest") {
            val locationId = call.parameters["locationId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Missing location ID"))

            val request = call.receive<RestRequest>()

            // Validate this is the inn
            if (locationId != TunDuLacSeed.INN_ID) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "This is not an inn"))
            }

            // Spend gold for rest
            val goldSpent = UserRepository.spendGold(request.userId, TunDuLacSeed.INN_REST_COST)
            if (!goldSpent) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Not enough gold (need ${TunDuLacSeed.INN_REST_COST}g)"))
            }

            // Heal to full and restore all resources
            UserRepository.restoreAllResources(request.userId)

            // Return updated user
            val updatedUser = UserRepository.findById(request.userId)
            call.respond(ShopActionResponse(
                success = true,
                message = "You rest at the inn and feel completely refreshed. All HP, mana, and stamina restored!",
                user = updatedUser?.toResponse()
            ))
        }

        // Get sellable items for a user at a general store
        get("/{locationId}/sellable/{userId}") {
            val locationId = call.parameters["locationId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, SellableItemsResponse(false, emptyList()))
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, SellableItemsResponse(false, emptyList()))

            // Validate this is the general store
            if (locationId != GeneralStoreSeed.GENERAL_STORE_ID) {
                return@get call.respond(HttpStatusCode.BadRequest, SellableItemsResponse(false, emptyList()))
            }

            val user = UserRepository.findById(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound, SellableItemsResponse(false, emptyList()))

            val sellableItems = mutableListOf<SellableItemDto>()

            // Add regular sellable items from inventory
            // Sellable items: rations, torches, rope, waterskin, bedroll, salt, etc.
            val sellableItemIds = setOf(
                GeneralStoreSeed.SALT_ID,
                GeneralStoreSeed.RATIONS_ID,
                GeneralStoreSeed.WATERSKIN_ID,
                GeneralStoreSeed.TORCH_ID,
                GeneralStoreSeed.ROPE_ID,
                GeneralStoreSeed.BEDROLL_ID,
                FishingSeed.FISHING_ROD_ID
            )

            user.itemIds.forEach { itemId ->
                if (itemId in sellableItemIds) {
                    val item = ItemRepository.findById(itemId)
                    if (item != null && item.value > 0) {
                        // Sell price is 50% of value
                        val sellValue = (item.value / 2).coerceAtLeast(1)
                        sellableItems.add(SellableItemDto(
                            id = itemId,  // For regular items, id = itemId
                            itemId = itemId,
                            name = item.name,
                            sellValue = sellValue,
                            isFoodItem = false
                        ))
                    }
                }
            }

            // Add food items (fish) from food inventory
            val foodItems = FoodService.getUserFoodItems(userId)
            foodItems.forEach { foodItem ->
                val item = ItemRepository.findById(foodItem.itemId)
                if (item != null && item.value > 0) {
                    // Base sell price is 50% of value
                    var sellValue = (item.value / 2).coerceAtLeast(1)
                    // Cooked fish sell for more, salted for slightly less
                    when (foodItem.state) {
                        "cooked" -> sellValue = (sellValue * 1.5).toInt()
                        "salted" -> sellValue = (sellValue * 0.8).toInt().coerceAtLeast(1)
                    }
                    sellableItems.add(SellableItemDto(
                        id = foodItem.id,  // Food items use their unique ID
                        itemId = foodItem.itemId,
                        name = item.name,
                        sellValue = sellValue,
                        isFoodItem = true,
                        foodState = foodItem.state,
                        timeUntilSpoil = foodItem.getTimeUntilSpoil()
                    ))
                }
            }

            call.respond(SellableItemsResponse(true, sellableItems))
        }

        // Sell a regular item at the general store
        post("/{locationId}/sell") {
            val locationId = call.parameters["locationId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Missing location ID"))

            val request = call.receive<SellItemRequest>()

            // Validate this is the general store
            if (locationId != GeneralStoreSeed.GENERAL_STORE_ID) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "You can only sell items at the general store"))
            }

            val user = UserRepository.findById(request.userId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ShopActionResponse(false, "User not found"))

            // Check if user is banned from the shop
            val banExpiry = getShopBanExpiry(request.userId)
            if (banExpiry != null) {
                return@post call.respond(HttpStatusCode.Forbidden, ShopActionResponse(
                    false,
                    "The shopkeeper refuses to deal with you. \"Come back tomorrow!\""
                ))
            }

            // Check user has the item
            if (request.itemId !in user.itemIds) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "You don't have that item"))
            }

            val item = ItemRepository.findById(request.itemId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ShopActionResponse(false, "Item not found"))

            // Check if trying to sell cursed items
            if (request.itemId in CURSED_ITEM_IDS) {
                // Ban the user and kick them out!
                banUserFromShop(request.userId)
                return@post call.respond(HttpStatusCode.Forbidden, ShopActionResponse(
                    false,
                    "The shopkeeper's face goes pale as he sees the ancient coins. \"That... that's tomb gold! Cursed! I want none of that here!\" He backs away, making a warding sign. \"BE GONE! Don't come back until tomorrow, and rid yourself of that cursed treasure!\""
                ))
            }

            if (item.value <= 0) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "That item has no value"))
            }

            // Sell price is 50% of value
            val sellValue = (item.value / 2).coerceAtLeast(1)

            // Remove item and add gold
            UserRepository.removeItems(request.userId, listOf(request.itemId))
            UserRepository.addGold(request.userId, sellValue)

            val updatedUser = UserRepository.findById(request.userId)
            call.respond(ShopActionResponse(
                success = true,
                message = "Sold ${item.name} for ${sellValue}g",
                user = updatedUser?.toResponse()
            ))
        }

        // Sell a food item (fish) at the general store
        post("/{locationId}/sell-food") {
            val locationId = call.parameters["locationId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Missing location ID"))

            val request = call.receive<SellFoodItemRequest>()

            // Validate this is the general store
            if (locationId != GeneralStoreSeed.GENERAL_STORE_ID) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "You can only sell items at the general store"))
            }

            val user = UserRepository.findById(request.userId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ShopActionResponse(false, "User not found"))

            val foodItem = UserFoodItemRepository.findById(request.foodItemId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ShopActionResponse(false, "Food item not found"))

            if (foodItem.userId != user.id) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "That's not your item"))
            }

            val item = ItemRepository.findById(foodItem.itemId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ShopActionResponse(false, "Item template not found"))

            if (item.value <= 0) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "That item has no value"))
            }

            // Check if spoiled
            if (foodItem.isSpoiled()) {
                UserFoodItemRepository.delete(foodItem.id)
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "That ${item.name} has spoiled! It was discarded."))
            }

            // Base sell price is 50% of value
            var sellValue = (item.value / 2).coerceAtLeast(1)
            // Cooked fish sell for more, salted for slightly less
            when (foodItem.state) {
                "cooked" -> sellValue = (sellValue * 1.5).toInt()
                "salted" -> sellValue = (sellValue * 0.8).toInt().coerceAtLeast(1)
            }

            // Remove food item and add gold
            UserFoodItemRepository.delete(foodItem.id)
            UserRepository.addGold(request.userId, sellValue)

            val statePrefix = when (foodItem.state) {
                "cooked" -> "cooked "
                "salted" -> "salted "
                else -> "raw "
            }

            val updatedUser = UserRepository.findById(request.userId)
            call.respond(ShopActionResponse(
                success = true,
                message = "Sold $statePrefix${item.name} for ${sellValue}g",
                user = updatedUser?.toResponse()
            ))
        }
    }
}
