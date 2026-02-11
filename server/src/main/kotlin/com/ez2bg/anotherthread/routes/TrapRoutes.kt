package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.game.TrapService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.trapRoutes() {
    route("/traps") {
        // Get all traps (admin/debug)
        get {
            call.respond(TrapRepository.findAll())
        }

        // Get trap by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val trap = TrapRepository.findById(id)
            if (trap != null) {
                call.respond(trap)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Get detected traps at a location for a user
        get("/at-location/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")

            if (userId == null) {
                // No user context, return empty list (traps are hidden by default)
                call.respond(emptyList<TrapInfoDto>())
                return@get
            }

            val detectedTraps = TrapService.getDetectedTraps(userId, locationId)

            val trapInfos = detectedTraps.map { trap ->
                val disarmedKey = "disarmed_trap_${trap.id}"
                val isDisarmed = FeatureStateRepository.getState(userId, disarmedKey)?.value == "true"

                TrapInfoDto(
                    id = trap.id,
                    name = trap.name,
                    description = trap.description,
                    trapType = trap.trapType.name,
                    isDisarmed = isDisarmed,
                    canDisarm = !isDisarmed && trap.isArmed
                )
            }

            call.respond(trapInfos)
        }

        // Attempt to detect traps in a location
        post("/detect/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, DetectResultDto(false, "User not found"))
                return@post
            }

            val result = TrapService.attemptDetect(user, locationId)

            call.respond(DetectResultDto(
                success = result.success,
                message = result.message,
                trapDetected = result.trapDetected
            ))
        }

        // Attempt to disarm a specific trap
        post("/{id}/disarm") {
            val trapId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, DisarmResultDto(false, "User not found"))
                return@post
            }

            val result = TrapService.attemptDisarm(user, trapId)

            // Apply any damage from failed disarm
            if (result.hpChange != 0) {
                val newHp = (user.currentHp + result.hpChange).coerceIn(0, user.maxHp)
                UserRepository.update(user.copy(currentHp = newHp))
            }

            // Handle teleport from trap trigger
            if (result.teleportLocationId != null) {
                UserRepository.update(user.copy(currentLocationId = result.teleportLocationId))
            }

            call.respond(DisarmResultDto(
                success = result.success,
                message = result.message,
                trapDisarmed = result.trapDisarmed,
                trapTriggered = result.trapTriggered,
                hpChange = result.hpChange,
                conditionApplied = result.conditionApplied,
                conditionDuration = result.conditionDuration,
                teleportLocationId = result.teleportLocationId
            ))
        }
    }
}

@Serializable
private data class TrapInfoDto(
    val id: String,
    val name: String,
    val description: String,
    val trapType: String,
    val isDisarmed: Boolean,
    val canDisarm: Boolean
)

@Serializable
private data class DetectResultDto(
    val success: Boolean,
    val message: String,
    val trapDetected: Boolean = false
)

@Serializable
private data class DisarmResultDto(
    val success: Boolean,
    val message: String,
    val trapDisarmed: Boolean = false,
    val trapTriggered: Boolean = false,
    val hpChange: Int = 0,
    val conditionApplied: String? = null,
    val conditionDuration: Int = 0,
    val teleportLocationId: String? = null
)
