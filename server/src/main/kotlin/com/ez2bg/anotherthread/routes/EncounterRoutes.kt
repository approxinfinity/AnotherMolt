package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ClassifyEncounterRequest(val classification: String)

fun Route.encounterRoutes() {
    route("/users/{id}/encounters") {
        // Get all encounters for a user
        get {
            val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val encounters = PlayerEncounterRepository.findByUser(userId)
            call.respond(encounters.map { enc ->
                PlayerEncounterResponse(
                    encounteredUserId = enc.encounteredUserId,
                    classification = enc.classification,
                    lastKnownName = enc.lastKnownName,
                    lastKnownDesc = enc.lastKnownDesc,
                    lastKnownImageUrl = enc.lastKnownImageUrl,
                    lastLocationId = enc.lastLocationId,
                    firstEncounteredAt = enc.firstEncounteredAt,
                    lastEncounteredAt = enc.lastEncounteredAt,
                    encounterCount = enc.encounterCount
                )
            })
        }

        // Classify an encounter as friend/enemy/neutral
        put("/{encounteredUserId}/classify") {
            val userId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val encounteredUserId = call.parameters["encounteredUserId"]
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<ClassifyEncounterRequest>()

            if (request.classification !in listOf("friend", "enemy", "neutral")) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Classification must be friend, enemy, or neutral"))
                return@put
            }

            val success = PlayerEncounterRepository.classify(userId, encounteredUserId, request.classification)
            if (success) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Encounter not found"))
            }
        }
    }
}
