package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class TeleportRequest(
    val userId: String,
    val targetAreaId: String,
    val abilityId: String = "weapon-wayfarer-teleport"
)

@Serializable
data class TeleportResponse(
    val success: Boolean,
    val message: String,
    val departureMessage: String? = null,
    val arrivalMessage: String? = null,
    val newLocationId: String? = null,
    val newLocationName: String? = null
)

@Serializable
data class TeleportDestination(
    val areaId: String,
    val locationId: String,
    val locationName: String
)

fun Route.teleportRoutes() {
    route("/teleport") {
        // List available teleport destinations (areas with a 0,0 location)
        get("/destinations") {
            val locations = LocationRepository.findAll()
            val destinations = locations
                .filter { it.gridX == 0 && it.gridY == 0 }
                .map {
                    TeleportDestination(
                        areaId = it.areaId ?: "overworld",
                        locationId = it.id,
                        locationName = it.name
                    )
                }
            call.respond(destinations)
        }

        // Teleport a player to 0,0 of a target area
        post {
            val request = call.receive<TeleportRequest>()

            // 1. Validate user exists
            val user = UserRepository.findById(request.userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, TeleportResponse(false, "User not found"))
                return@post
            }

            // 2. Verify the user has an equipped item with this ability
            val equippedItems = user.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
            val hasAbility = equippedItems.any { request.abilityId in it.abilityIds }
            if (!hasAbility) {
                call.respond(HttpStatusCode.Forbidden, TeleportResponse(false, "No equipped item grants this ability"))
                return@post
            }

            // 3. Check mana
            if (user.currentMana < 1) {
                call.respond(HttpStatusCode.BadRequest, TeleportResponse(false, "Not enough mana"))
                return@post
            }

            // 4. Find destination location at (0,0) in target area
            val destination = LocationRepository.findByCoordinates(0, 0, request.targetAreaId)
            if (destination == null) {
                call.respond(HttpStatusCode.NotFound, TeleportResponse(false, "No gateway location found in ${request.targetAreaId}"))
                return@post
            }

            // 5. Deduct mana
            val manaSpent = UserRepository.spendMana(user.id, 1)
            if (!manaSpent) {
                call.respond(HttpStatusCode.BadRequest, TeleportResponse(false, "Failed to spend mana"))
                return@post
            }

            // 6. Update location
            UserRepository.updateCurrentLocation(user.id, destination.id)

            // 7. Build messages
            val departureMessage = "With a soft pop ${user.name} dematerializes."
            val arrivalMessage = "${user.name} materializes with a loud bang!"

            call.respond(
                TeleportResponse(
                    success = true,
                    message = "Teleported to ${destination.name}",
                    departureMessage = departureMessage,
                    arrivalMessage = arrivalMessage,
                    newLocationId = destination.id,
                    newLocationName = destination.name
                )
            )
        }
    }
}
