package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.CastSpellRequest
import com.ez2bg.anotherthread.CastSpellResponse
import com.ez2bg.anotherthread.database.FeatureRepository
import com.ez2bg.anotherthread.database.FeatureState
import com.ez2bg.anotherthread.database.FeatureStateRepository
import com.ez2bg.anotherthread.spell.SpellDataParser
import com.ez2bg.anotherthread.spell.SpellService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.spellRoutes() {
    route("/spells") {
        // Get all utility spells available to a user
        get("/available/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")

            val spells = SpellService.getAvailableUtilitySpells(userId)
            call.respond(spells)
        }

        // Get all spell features (for admin/browsing)
        get {
            val allFeatures = FeatureRepository.findAll()
            val spellFeatures = allFeatures.filter {
                SpellDataParser.isSpell(it.data)
            }
            call.respond(spellFeatures)
        }

        // Cast a utility spell
        post("/cast") {
            val request = call.receive<CastSpellRequest>()

            val result = SpellService.castUtilitySpell(
                userId = request.userId,
                featureId = request.featureId,
                targetParams = request.targetParams
            )

            when (result) {
                is SpellService.CastResult.Success -> {
                    call.respond(CastSpellResponse(
                        success = true,
                        message = result.message,
                        newLocationId = result.newLocationId,
                        revealedInfo = result.revealedInfo,
                        spellState = result.spellState
                    ))
                }
                is SpellService.CastResult.Failure -> {
                    call.respond(HttpStatusCode.BadRequest, CastSpellResponse(
                        success = false,
                        message = result.reason
                    ))
                }
            }
        }

        // Get spell state for a user
        get("/state/{userId}/{featureId}") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")
            val featureId = call.parameters["featureId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "featureId required")

            val state = SpellService.getSpellState(userId, featureId)
            if (state != null) {
                call.respond(state)
            } else {
                call.respond(mapOf("message" to "No state found (spell not yet used)"))
            }
        }

        // Reset daily charges for a user (admin/debug endpoint)
        post("/reset-charges/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "userId required")

            SpellService.resetDailyCharges(userId)
            call.respond(mapOf("success" to true, "message" to "Daily charges reset for user"))
        }
    }
}

fun Route.featureStateRoutes() {
    route("/feature-state") {
        // Get all feature states for a user
        get("/user/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")

            val states = FeatureStateRepository.findAllByOwner(userId)
            call.respond(states)
        }

        // Get specific feature state
        get("/{ownerId}/{featureId}") {
            val ownerId = call.parameters["ownerId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "ownerId required")
            val featureId = call.parameters["featureId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "featureId required")

            val state = FeatureStateRepository.findByOwnerAndFeature(ownerId, featureId)
            if (state != null) {
                call.respond(state)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "State not found"))
            }
        }

        // Delete feature state (admin/debug)
        delete("/{ownerId}/{featureId}") {
            val ownerId = call.parameters["ownerId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "ownerId required")
            val featureId = call.parameters["featureId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "featureId required")

            val id = FeatureState.createId(ownerId, featureId)
            val deleted = FeatureStateRepository.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
