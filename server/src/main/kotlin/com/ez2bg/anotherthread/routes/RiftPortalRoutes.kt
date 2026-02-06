package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RiftPortalRoutes")

/**
 * DTO for an unconnected area/map.
 */
@Serializable
data class UnconnectedArea(
    val areaId: String,
    val name: String,  // Human-readable name derived from areaId
    val locationCount: Int,
    val entryLocationId: String?,  // Location at (0,0) or first location in area
    val entryLocationName: String?
)

/**
 * Request to create a rift portal.
 */
@Serializable
data class CreateRiftRequest(
    val targetAreaId: String
)

/**
 * Response after creating a rift.
 */
@Serializable
data class CreateRiftResponse(
    val success: Boolean,
    val message: String,
    val exitAdded: Exit? = null,
    val targetLocation: Location? = null
)

/**
 * Routes for the Rift Ring functionality.
 *
 * - GET /rift-portal/unconnected-areas: List areas not connected from current location's area
 * - POST /rift-portal/create: Create a permanent rift to another area
 */
fun Route.riftPortalRoutes() {
    route("/rift-portal") {

        /**
         * Get all areas/maps that are not connected from the user's current area.
         * An area is "unconnected" if there's no exit from any location in the current area
         * leading to any location in that target area.
         */
        get("/unconnected-areas") {
            val userId = call.request.header("X-User-Id")
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "X-User-Id header required"))
                return@get
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@get
            }

            val currentLocationId = user.currentLocationId
            if (currentLocationId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User has no current location"))
                return@get
            }

            val currentLocation = LocationRepository.findById(currentLocationId)
            if (currentLocation == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Current location not found"))
                return@get
            }

            val currentAreaId = currentLocation.areaId ?: "overworld"

            // Get all locations grouped by areaId
            val allLocations = LocationRepository.findAll()
            val locationsByArea = allLocations.groupBy { it.areaId ?: "overworld" }

            // Find all areas that current area's locations have exits to
            val currentAreaLocations = locationsByArea[currentAreaId] ?: emptyList()
            val connectedAreaIds = mutableSetOf<String>()

            // Add current area (can't create rift to own area)
            connectedAreaIds.add(currentAreaId)

            // Check all exits from current area locations
            currentAreaLocations.forEach { location ->
                location.exits.forEach { exit ->
                    val targetLocation = allLocations.find { it.id == exit.locationId }
                    if (targetLocation != null) {
                        connectedAreaIds.add(targetLocation.areaId ?: "overworld")
                    }
                }
            }

            // Find unconnected areas (areas with locations but no connection from current area)
            val unconnectedAreas = locationsByArea
                .filter { (areaId, _) -> areaId !in connectedAreaIds }
                .map { (areaId, locations) ->
                    // Find entry point: prefer (0,0), then first location
                    val entryLocation = locations.find { it.gridX == 0 && it.gridY == 0 }
                        ?: locations.minByOrNull { (it.gridX ?: 999) + (it.gridY ?: 999) }

                    UnconnectedArea(
                        areaId = areaId,
                        name = formatAreaName(areaId),
                        locationCount = locations.size,
                        entryLocationId = entryLocation?.id,
                        entryLocationName = entryLocation?.name
                    )
                }
                .sortedBy { it.name }

            call.respond(unconnectedAreas)
        }

        /**
         * Create a permanent rift portal from the user's current location to another area.
         * The rift will lead to (0,0) of the target area, or the first location if no (0,0) exists.
         */
        post("/create") {
            val userId = call.request.header("X-User-Id")
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "X-User-Id header required"))
                return@post
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

            val currentLocationId = user.currentLocationId
            if (currentLocationId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User has no current location"))
                return@post
            }

            val currentLocation = LocationRepository.findById(currentLocationId)
            if (currentLocation == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Current location not found"))
                return@post
            }

            // Check if user has the Rift Ring equipped
            val hasRiftRing = user.equippedItemIds.contains(GoodmanGearSeed.RIFT_RING_ITEM_ID)
            if (!hasRiftRing) {
                call.respond(HttpStatusCode.Forbidden, CreateRiftResponse(
                    success = false,
                    message = "You need the Rift Ring equipped to open portals"
                ))
                return@post
            }

            // Check mana cost (10 mana)
            if (user.currentMana < 10) {
                call.respond(HttpStatusCode.BadRequest, CreateRiftResponse(
                    success = false,
                    message = "Not enough mana (need 10, have ${user.currentMana})"
                ))
                return@post
            }

            val request = call.receive<CreateRiftRequest>()
            val targetAreaId = request.targetAreaId

            // Find target area locations
            val allLocations = LocationRepository.findAll()
            val targetAreaLocations = allLocations.filter { (it.areaId ?: "overworld") == targetAreaId }

            if (targetAreaLocations.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, CreateRiftResponse(
                    success = false,
                    message = "Target area '$targetAreaId' not found or has no locations"
                ))
                return@post
            }

            // Find entry point in target area: prefer (0,0), then first location
            val targetLocation = targetAreaLocations.find { it.gridX == 0 && it.gridY == 0 }
                ?: targetAreaLocations.minByOrNull { (it.gridX ?: 999) + (it.gridY ?: 999) }!!

            // Check if exit to this area already exists from current location
            val existingExit = currentLocation.exits.find { exit ->
                val exitTarget = allLocations.find { it.id == exit.locationId }
                exitTarget != null && (exitTarget.areaId ?: "overworld") == targetAreaId
            }

            if (existingExit != null) {
                call.respond(HttpStatusCode.Conflict, CreateRiftResponse(
                    success = false,
                    message = "A portal to ${formatAreaName(targetAreaId)} already exists from this location"
                ))
                return@post
            }

            // Deduct mana
            UserRepository.spendMana(userId, 10)

            // Create the new exit
            val newExit = Exit(
                locationId = targetLocation.id,
                direction = ExitDirection.ENTER  // Rift portals use ENTER direction
            )

            // Update the current location with the new exit
            val updatedLocation = currentLocation.copy(
                exits = currentLocation.exits + newExit
            )
            LocationRepository.update(updatedLocation)

            log.info("User $userId created rift portal from ${currentLocation.name} to ${targetLocation.name} in $targetAreaId")

            call.respond(CreateRiftResponse(
                success = true,
                message = "Opened a rift portal to ${targetLocation.name} in ${formatAreaName(targetAreaId)}!",
                exitAdded = newExit,
                targetLocation = targetLocation
            ))
        }

        /**
         * Get sealable rifts at the current location.
         * Returns ENTER exits that lead to different areas (created by rift ring).
         */
        get("/sealable-rifts") {
            val userId = call.request.header("X-User-Id")
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "X-User-Id header required"))
                return@get
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@get
            }

            val currentLocationId = user.currentLocationId
            if (currentLocationId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User has no current location"))
                return@get
            }

            val currentLocation = LocationRepository.findById(currentLocationId)
            if (currentLocation == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Current location not found"))
                return@get
            }

            val currentAreaId = currentLocation.areaId ?: "overworld"
            val allLocations = LocationRepository.findAll()

            // Find ENTER exits that lead to different areas
            val sealableRifts = currentLocation.exits
                .filter { it.direction == ExitDirection.ENTER }
                .mapNotNull { exit ->
                    val targetLocation = allLocations.find { it.id == exit.locationId }
                    if (targetLocation != null && (targetLocation.areaId ?: "overworld") != currentAreaId) {
                        mapOf(
                            "exitLocationId" to exit.locationId,
                            "targetAreaId" to (targetLocation.areaId ?: "overworld"),
                            "targetAreaName" to formatAreaName(targetLocation.areaId ?: "overworld"),
                            "targetLocationName" to targetLocation.name
                        )
                    } else null
                }

            call.respond(sealableRifts)
        }

        /**
         * Seal a rift portal (remove an ENTER exit to another area).
         */
        post("/seal") {
            val userId = call.request.header("X-User-Id")
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "X-User-Id header required"))
                return@post
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

            val currentLocationId = user.currentLocationId
            if (currentLocationId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User has no current location"))
                return@post
            }

            val currentLocation = LocationRepository.findById(currentLocationId)
            if (currentLocation == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Current location not found"))
                return@post
            }

            // Check if user has the Rift Ring equipped
            val hasRiftRing = user.equippedItemIds.contains(GoodmanGearSeed.RIFT_RING_ITEM_ID)
            if (!hasRiftRing) {
                call.respond(HttpStatusCode.Forbidden, CreateRiftResponse(
                    success = false,
                    message = "You need the Rift Ring equipped to seal portals"
                ))
                return@post
            }

            // Check mana cost (5 mana to seal)
            if (user.currentMana < 5) {
                call.respond(HttpStatusCode.BadRequest, CreateRiftResponse(
                    success = false,
                    message = "Not enough mana (need 5, have ${user.currentMana})"
                ))
                return@post
            }

            val request = call.receive<CreateRiftRequest>()
            val targetAreaId = request.targetAreaId

            val currentAreaId = currentLocation.areaId ?: "overworld"
            val allLocations = LocationRepository.findAll()

            // Find the ENTER exit to seal
            val exitToSeal = currentLocation.exits.find { exit ->
                exit.direction == ExitDirection.ENTER &&
                    allLocations.find { it.id == exit.locationId }?.let { target ->
                        (target.areaId ?: "overworld") == targetAreaId
                    } == true
            }

            if (exitToSeal == null) {
                call.respond(HttpStatusCode.NotFound, CreateRiftResponse(
                    success = false,
                    message = "No rift portal to ${formatAreaName(targetAreaId)} found at this location"
                ))
                return@post
            }

            // Deduct mana
            UserRepository.spendMana(userId, 5)

            // Remove the exit
            val updatedExits = currentLocation.exits.filter { it != exitToSeal }
            val updatedLocation = currentLocation.copy(exits = updatedExits)
            LocationRepository.update(updatedLocation)

            log.info("User $userId sealed rift portal from ${currentLocation.name} to $targetAreaId")

            call.respond(CreateRiftResponse(
                success = true,
                message = "Sealed the rift portal to ${formatAreaName(targetAreaId)}. The tear in reality closes with a soft whisper."
            ))
        }
    }
}

/**
 * Convert area IDs like "undead-crypt" to "Undead Crypt".
 */
private fun formatAreaName(areaId: String): String {
    return areaId.split("-")
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
