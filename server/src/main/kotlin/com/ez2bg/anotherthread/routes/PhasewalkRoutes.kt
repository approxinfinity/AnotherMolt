package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class PhasewalkRequest(
    val userId: String,
    val direction: String,  // The direction to phase (e.g., "north", "southeast")
    val abilityId: String = "ability-phasewalk"
)

@Serializable
data class PhasewalkResponse(
    val success: Boolean,
    val message: String,
    val departureMessage: String? = null,
    val newLocationId: String? = null,
    val newLocationName: String? = null
)

@Serializable
data class PhasewalkDestination(
    val direction: String,
    val locationId: String,
    val locationName: String,
    val gridX: Int,
    val gridY: Int
)

/**
 * Get the opposite direction for the arrival message.
 */
private fun getOppositeDirection(direction: String): String {
    return when (direction.lowercase()) {
        "north" -> "south"
        "south" -> "north"
        "east" -> "west"
        "west" -> "east"
        "northeast" -> "southwest"
        "northwest" -> "southeast"
        "southeast" -> "northwest"
        "southwest" -> "northeast"
        else -> direction
    }
}

/**
 * Get the coordinate offset for a direction.
 */
private fun getDirectionOffset(direction: String): Pair<Int, Int> {
    return when (direction.lowercase()) {
        "north" -> Pair(0, -1)
        "south" -> Pair(0, 1)
        "east" -> Pair(1, 0)
        "west" -> Pair(-1, 0)
        "northeast" -> Pair(1, -1)
        "northwest" -> Pair(-1, -1)
        "southeast" -> Pair(1, 1)
        "southwest" -> Pair(-1, 1)
        else -> Pair(0, 0)
    }
}

fun Route.phasewalkRoutes() {
    route("/phasewalk") {
        /**
         * List available phasewalk destinations for the current location.
         * Returns directions where:
         * 1. There is NO exit from the current location
         * 2. There IS a location at that adjacent grid position
         */
        get("/destinations/{userId}") {
            val userId = call.parameters["userId"]
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "User ID required")
                return@get
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "User not found")
                return@get
            }

            // Check if user has phasewalk ability
            val equippedItems = user.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
            println("[Phasewalk] User $userId has ${user.equippedItemIds.size} equipped items: ${user.equippedItemIds}")
            println("[Phasewalk] Loaded ${equippedItems.size} items with abilityIds: ${equippedItems.map { "${it.id} -> ${it.abilityIds}" }}")
            val hasAbility = equippedItems.any { "ability-phasewalk" in it.abilityIds }
            if (!hasAbility) {
                println("[Phasewalk] User $userId does NOT have phasewalk ability")
                call.respond(emptyList<PhasewalkDestination>())
                return@get
            }
            println("[Phasewalk] User $userId HAS phasewalk ability")

            val currentLocation = user.currentLocationId?.let { LocationRepository.findById(it) }
            if (currentLocation == null || currentLocation.gridX == null || currentLocation.gridY == null) {
                call.respond(emptyList<PhasewalkDestination>())
                return@get
            }

            val areaId = currentLocation.areaId ?: "overworld"

            // Get all exits from current location
            val existingExitDirections = currentLocation.exits
                .map { it.direction.name.lowercase() }
                .toSet()

            // All possible directions
            val allDirections = listOf("north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest")

            // Find destinations that DON'T have exits but DO have locations
            val destinations = allDirections
                .filter { it !in existingExitDirections }  // No exit in this direction
                .mapNotNull { direction ->
                    val (dx, dy) = getDirectionOffset(direction)
                    val targetX = currentLocation.gridX!! + dx
                    val targetY = currentLocation.gridY!! + dy

                    // Check if there's a location at these coordinates
                    val targetLocation = LocationRepository.findByCoordinates(targetX, targetY, areaId)
                    if (targetLocation != null) {
                        PhasewalkDestination(
                            direction = direction,
                            locationId = targetLocation.id,
                            locationName = targetLocation.name,
                            gridX = targetX,
                            gridY = targetY
                        )
                    } else {
                        null
                    }
                }

            call.respond(destinations)
        }

        /**
         * Perform a phasewalk to an adjacent location without an exit.
         */
        post {
            val request = call.receive<PhasewalkRequest>()

            // 1. Validate user exists
            val user = UserRepository.findById(request.userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, PhasewalkResponse(false, "User not found"))
                return@post
            }

            // 2. Verify the user has an equipped item with this ability
            val equippedItems = user.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
            val hasAbility = equippedItems.any { request.abilityId in it.abilityIds }
            if (!hasAbility) {
                call.respond(HttpStatusCode.Forbidden, PhasewalkResponse(false, "No equipped item grants this ability"))
                return@post
            }

            // 3. Check mana (Phasewalk costs 2 mana)
            if (user.currentMana < 2) {
                call.respond(HttpStatusCode.BadRequest, PhasewalkResponse(false, "Not enough mana (need 2)"))
                return@post
            }

            // 4. Get current location
            val currentLocation = user.currentLocationId?.let { LocationRepository.findById(it) }
            if (currentLocation == null || currentLocation.gridX == null || currentLocation.gridY == null) {
                call.respond(HttpStatusCode.BadRequest, PhasewalkResponse(false, "Cannot phasewalk from this location"))
                return@post
            }

            // 5. Check this direction doesn't have a normal exit
            val existingExitDirections = currentLocation.exits
                .map { it.direction.name.lowercase() }
                .toSet()

            val direction = request.direction.lowercase()
            if (direction in existingExitDirections) {
                call.respond(HttpStatusCode.BadRequest, PhasewalkResponse(false, "There's already an exit in that direction - just walk!"))
                return@post
            }

            // 6. Calculate target coordinates
            val (dx, dy) = getDirectionOffset(direction)
            val targetX = currentLocation.gridX!! + dx
            val targetY = currentLocation.gridY!! + dy
            val areaId = currentLocation.areaId ?: "overworld"

            // 7. Find destination location
            val destination = LocationRepository.findByCoordinates(targetX, targetY, areaId)
            if (destination == null) {
                call.respond(HttpStatusCode.NotFound, PhasewalkResponse(false, "No location exists in that direction to phase into"))
                return@post
            }

            // 8. Deduct mana
            val manaSpent = UserRepository.spendMana(user.id, 2)
            if (!manaSpent) {
                call.respond(HttpStatusCode.BadRequest, PhasewalkResponse(false, "Failed to spend mana"))
                return@post
            }

            // 9. Update location
            UserRepository.updateCurrentLocation(user.id, destination.id)

            // 10. Build departure message (visible to others in the room)
            val departureMessage = "${user.name} evaporates to the $direction."

            call.respond(
                PhasewalkResponse(
                    success = true,
                    message = "You phase through the wall to the $direction...",
                    departureMessage = departureMessage,
                    newLocationId = destination.id,
                    newLocationName = destination.name
                )
            )
        }
    }
}
