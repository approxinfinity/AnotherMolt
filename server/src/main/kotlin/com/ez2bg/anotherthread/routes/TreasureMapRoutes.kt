package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.UserRepository
import com.ez2bg.anotherthread.game.TreasureMapService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class TreasureMapRequest(val itemId: String)

@Serializable
data class ReadMapResponse(
    val success: Boolean,
    val message: String,
    val hint: String? = null,
    val alreadyRead: Boolean = false,
    val roll: Int = 0,
    val modifier: Int = 0,
    val total: Int = 0,
    val difficulty: Int = 0
)

@Serializable
data class ClaimTreasureResponse(
    val success: Boolean,
    val message: String,
    val goldAwarded: Int = 0,
    val itemsAwarded: List<String> = emptyList()
)

@Serializable
data class TreasureMapStatusInfo(
    val itemId: String,
    val itemName: String,
    val featureId: String,
    val read: Boolean,
    val hint: String? = null,
    val claimed: Boolean,
    val destinationLocationId: String? = null
)

@Serializable
data class TreasureMapStatusResponse(
    val maps: List<TreasureMapStatusInfo>
)

fun Route.treasureMapRoutes() {
    route("/treasure-maps") {
        // Read a treasure map (INT check)
        post("/{userId}/read") {
            val userId = call.parameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")
            val user = UserRepository.findById(userId)
                ?: return@post call.respond(HttpStatusCode.NotFound, "User not found")
            val request = call.receive<TreasureMapRequest>()

            val result = TreasureMapService.readMap(user, request.itemId)
            call.respond(
                HttpStatusCode.OK,
                ReadMapResponse(
                    success = result.success,
                    message = result.message,
                    hint = result.hint,
                    alreadyRead = result.alreadyRead,
                    roll = result.roll,
                    modifier = result.modifier,
                    total = result.total,
                    difficulty = result.difficulty
                )
            )
        }

        // Claim treasure at destination
        post("/{userId}/claim") {
            val userId = call.parameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")
            val user = UserRepository.findById(userId)
                ?: return@post call.respond(HttpStatusCode.NotFound, "User not found")
            val request = call.receive<TreasureMapRequest>()

            val result = TreasureMapService.claimTreasure(user, request.itemId)
            call.respond(
                HttpStatusCode.OK,
                ClaimTreasureResponse(
                    success = result.success,
                    message = result.message,
                    goldAwarded = result.goldAwarded,
                    itemsAwarded = result.itemsAwarded
                )
            )
        }

        // Get all treasure map statuses for a user
        get("/{userId}/status") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
            UserRepository.findById(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "User not found")

            val statuses = TreasureMapService.getMapStatuses(userId)
            call.respond(
                HttpStatusCode.OK,
                TreasureMapStatusResponse(
                    maps = statuses.map { info ->
                        TreasureMapStatusInfo(
                            itemId = info.itemId,
                            itemName = info.itemName,
                            featureId = info.featureId,
                            read = info.read,
                            hint = info.hint,
                            claimed = info.claimed,
                            destinationLocationId = info.destinationLocationId
                        )
                    }
                )
            )
        }
    }
}
