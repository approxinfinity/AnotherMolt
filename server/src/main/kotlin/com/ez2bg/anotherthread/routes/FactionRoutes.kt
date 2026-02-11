package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.game.FactionService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.factionRoutes() {
    route("/factions") {
        // Get all factions
        get {
            call.respond(FactionRepository.findAll())
        }

        // Get a specific faction
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val faction = FactionRepository.findById(id)
            if (faction != null) {
                call.respond(faction)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Get faction relationships
        get("/{id}/relationships") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val relationships = FactionService.getFactionRelationships(id)
            call.respond(relationships)
        }

        // Get player's standings with all factions
        get("/standings") {
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val standings = FactionService.getPlayerFactionStandings(userId)
            call.respond(standings)
        }

        // Get player's standing with a specific faction
        get("/{id}/standing") {
            val factionId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val faction = FactionRepository.findById(factionId)
            if (faction == null) {
                call.respond(HttpStatusCode.NotFound, FactionErrorResponse("Faction not found"))
                return@get
            }

            val standing = PlayerFactionStandingRepository.findByUserAndFaction(userId, factionId)
            val standingValue = standing?.standing ?: 0
            val level = StandingLevel.fromValue(standingValue)

            call.respond(PlayerStandingResponse(
                factionId = factionId,
                factionName = faction.name,
                standing = standingValue,
                standingLevel = level.name,
                standingDescription = level.description,
                killCount = standing?.killCount ?: 0,
                questsCompleted = standing?.questsCompleted ?: 0
            ))
        }

        // Check hostility of a creature toward the player
        get("/hostility/{creatureId}") {
            val creatureId = call.parameters["creatureId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val result = FactionService.checkHostility(userId, creatureId)
            call.respond(result)
        }

        // Give tribute to a faction
        post("/{id}/tribute") {
            val factionId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<TributeRequest>()

            // Check if player has the item
            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, FactionErrorResponse("User not found"))
                return@post
            }

            if (!user.itemIds.contains(request.itemId)) {
                call.respond(HttpStatusCode.BadRequest, FactionErrorResponse("You don't have that item"))
                return@post
            }

            val result = FactionService.onTributeGiven(userId, factionId, request.itemId)
            if (result.success) {
                // Remove item from player
                UserRepository.removeItems(userId, listOf(request.itemId))
            }
            call.respond(result)
        }
    }
}

@Serializable
private data class FactionErrorResponse(val error: String)

@Serializable
private data class TributeRequest(val itemId: String)

@Serializable
private data class PlayerStandingResponse(
    val factionId: String,
    val factionName: String,
    val standing: Int,
    val standingLevel: String,
    val standingDescription: String,
    val killCount: Int,
    val questsCompleted: Int
)
