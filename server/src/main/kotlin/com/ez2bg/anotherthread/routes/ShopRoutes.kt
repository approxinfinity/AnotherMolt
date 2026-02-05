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

fun Route.shopRoutes() {
    route("/shop") {
        // Buy an item from a shop location
        post("/{locationId}/buy") {
            val locationId = call.parameters["locationId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Missing location ID"))

            val request = call.receive<BuyItemRequest>()

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

            // Spend gold
            val goldSpent = UserRepository.spendGold(request.userId, item.value)
            if (!goldSpent) {
                return@post call.respond(HttpStatusCode.BadRequest, ShopActionResponse(false, "Not enough gold (need ${item.value}g)"))
            }

            // Give item to user
            UserRepository.addItems(request.userId, listOf(request.itemId))

            // Return updated user
            val updatedUser = UserRepository.findById(request.userId)
            call.respond(ShopActionResponse(
                success = true,
                message = "Purchased ${item.name} for ${item.value}g",
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
