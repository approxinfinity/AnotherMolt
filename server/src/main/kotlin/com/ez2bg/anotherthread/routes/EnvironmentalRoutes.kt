package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.game.EnvironmentalEffectService
import com.ez2bg.anotherthread.game.PrisonerRescueService
import com.ez2bg.anotherthread.game.SecretDoorService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.environmentalRoutes() {
    route("/environmental") {
        // Check for direction confusion when moving
        post("/check-confusion/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, ConfusionCheckResult(false, null, "User not found"))
                return@post
            }

            val location = LocationRepository.findById(locationId)
            if (location == null) {
                call.respond(HttpStatusCode.NotFound, ConfusionCheckResult(false, null, "Location not found"))
                return@post
            }

            val result = EnvironmentalEffectService.checkDirectionConfusion(user, location)

            call.respond(ConfusionCheckResult(
                confused = result.redirected,
                redirectLocationId = result.newLocationId,
                message = result.message
            ))
        }
    }

    route("/prisoners") {
        // Get rescuable prisoners at a location
        get("/at-location/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val prisoners = PrisonerRescueService.getRescuablePrisoners(userId, locationId)

            call.respond(prisoners.map { prisoner ->
                PrisonerDto(
                    id = prisoner.id,
                    name = prisoner.name,
                    description = prisoner.desc,
                    canRescue = true
                )
            })
        }

        // Rescue a specific prisoner
        post("/{prisonerId}/rescue") {
            val prisonerId = call.parameters["prisonerId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val result = PrisonerRescueService.rescuePrisoner(userId, prisonerId)

            if (result.success) {
                call.respond(RescueResultDto(
                    success = true,
                    message = result.message,
                    xpReward = result.xpReward,
                    goldReward = result.goldReward,
                    itemRewards = result.itemRewards
                ))
            } else {
                call.respond(HttpStatusCode.BadRequest, RescueResultDto(
                    success = false,
                    message = result.message
                ))
            }
        }

        // Check if a creature can be rescued
        get("/{creatureId}/can-rescue") {
            val creatureId = call.parameters["creatureId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val canRescue = PrisonerRescueService.canRescue(userId, creatureId)

            call.respond(CanRescueDto(canRescue = canRescue))
        }
    }

    route("/secret-doors") {
        // Search for secret doors at current location
        post("/search/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val result = SecretDoorService.searchForSecretDoors(userId, locationId)

            call.respond(SearchResultDto(
                success = result.success,
                message = result.message,
                discoveredExits = result.discoveredExits.map { exit ->
                    DiscoveredExitDto(
                        direction = exit.direction,
                        destinationName = exit.destinationName,
                        destinationId = exit.destinationId
                    )
                }
            ))
        }

        // Get visible exits (filtering out undiscovered secrets)
        get("/visible-exits/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val location = LocationRepository.findById(locationId)
            if (location == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val visibleExits = SecretDoorService.getVisibleExits(userId, location)

            call.respond(visibleExits.map { exit ->
                val destination = LocationRepository.findById(exit.locationId)
                ExitDto(
                    direction = exit.direction.name,
                    destinationId = exit.locationId,
                    destinationName = destination?.name ?: "Unknown"
                )
            })
        }

        // Check if there are undiscovered secret doors
        get("/has-secrets/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val undiscovered = SecretDoorService.getUndiscoveredSecretDoors(userId, locationId)

            call.respond(HasSecretsDto(hasSecrets = undiscovered.isNotEmpty()))
        }
    }
}

@Serializable
private data class ConfusionCheckResult(
    val confused: Boolean,
    val redirectLocationId: String?,
    val message: String?
)

@Serializable
private data class PrisonerDto(
    val id: String,
    val name: String,
    val description: String,
    val canRescue: Boolean
)

@Serializable
private data class RescueResultDto(
    val success: Boolean,
    val message: String,
    val xpReward: Int = 0,
    val goldReward: Int = 0,
    val itemRewards: List<String> = emptyList()
)

@Serializable
private data class CanRescueDto(
    val canRescue: Boolean
)

@Serializable
private data class SearchResultDto(
    val success: Boolean,
    val message: String,
    val discoveredExits: List<DiscoveredExitDto> = emptyList()
)

@Serializable
private data class DiscoveredExitDto(
    val direction: String,
    val destinationName: String,
    val destinationId: String
)

@Serializable
private data class ExitDto(
    val direction: String,
    val destinationId: String,
    val destinationName: String
)

@Serializable
private data class HasSecretsDto(
    val hasSecrets: Boolean
)
