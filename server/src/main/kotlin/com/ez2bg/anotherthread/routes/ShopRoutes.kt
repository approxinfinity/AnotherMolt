package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class BuyItemRequest(val userId: String, val itemId: String)

@Serializable
data class RestRequest(val userId: String)

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
    }
}
