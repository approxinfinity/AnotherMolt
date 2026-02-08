package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LearnAbilityRequest(val userId: String, val abilityId: String)

@Serializable
data class TrainerActionResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse? = null
)

@Serializable
data class TrainerAbilityInfo(
    val ability: Ability,
    val goldCost: Int,
    val alreadyLearned: Boolean,
    val meetsLevelRequirement: Boolean
)

@Serializable
data class TrainerInfoResponse(
    val trainerId: String,
    val trainerName: String,
    val abilities: List<TrainerAbilityInfo>
)

/**
 * Trainer feature data JSON structure.
 * Stored in Feature.data field.
 */
@Serializable
data class TrainerFeatureData(
    val featureType: String = "trainer",
    val abilities: List<TrainerAbilityEntry> = emptyList()
)

@Serializable
data class TrainerAbilityEntry(
    val abilityId: String,
    val goldCost: Int = 50  // Default cost to learn
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Parse trainer data from a Feature's data field.
 */
private fun parseTrainerData(feature: Feature): TrainerFeatureData? {
    return try {
        val data = json.decodeFromString<TrainerFeatureData>(feature.data)
        if (data.featureType == "trainer") data else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Find the trainer feature for a creature.
 * Returns the first feature with featureType="trainer" in its data.
 */
private fun findTrainerFeature(creature: Creature): Pair<Feature, TrainerFeatureData>? {
    for (featureId in creature.featureIds) {
        val feature = FeatureRepository.findById(featureId) ?: continue
        val trainerData = parseTrainerData(feature) ?: continue
        return feature to trainerData
    }
    return null
}

fun Route.trainerRoutes() {
    route("/trainer") {
        // Get trainer info (abilities they can teach)
        get("/{creatureId}") {
            val creatureId = call.parameters["creatureId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, TrainerActionResponse(false, "Missing creature ID"))

            val userId = call.request.headers["X-User-Id"]

            val creature = CreatureRepository.findById(creatureId)
                ?: return@get call.respond(HttpStatusCode.NotFound, TrainerActionResponse(false, "Creature not found"))

            val (feature, trainerData) = findTrainerFeature(creature)
                ?: return@get call.respond(HttpStatusCode.BadRequest, TrainerActionResponse(false, "This creature is not a trainer"))

            // Get user info for checking learned status
            val user = userId?.let { UserRepository.findById(it) }

            // Build ability info list
            val abilities = trainerData.abilities.mapNotNull { entry ->
                val ability = AbilityRepository.findById(entry.abilityId) ?: return@mapNotNull null
                TrainerAbilityInfo(
                    ability = ability,
                    goldCost = entry.goldCost,
                    alreadyLearned = user?.learnedAbilityIds?.contains(entry.abilityId) == true,
                    meetsLevelRequirement = user?.let { it.level >= ability.minLevel } ?: false
                )
            }

            call.respond(TrainerInfoResponse(
                trainerId = creature.id,
                trainerName = creature.name,
                abilities = abilities
            ))
        }

        // Learn an ability from a trainer
        post("/{creatureId}/learn") {
            val creatureId = call.parameters["creatureId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, TrainerActionResponse(false, "Missing creature ID"))

            val request = call.receive<LearnAbilityRequest>()

            // Get user
            val user = UserRepository.findById(request.userId)
                ?: return@post call.respond(HttpStatusCode.NotFound, TrainerActionResponse(false, "User not found"))

            // Get creature
            val creature = CreatureRepository.findById(creatureId)
                ?: return@post call.respond(HttpStatusCode.NotFound, TrainerActionResponse(false, "Creature not found"))

            // Verify it's a trainer
            val (feature, trainerData) = findTrainerFeature(creature)
                ?: return@post call.respond(HttpStatusCode.BadRequest, TrainerActionResponse(false, "This creature is not a trainer"))

            // Find the ability entry
            val abilityEntry = trainerData.abilities.find { it.abilityId == request.abilityId }
                ?: return@post call.respond(HttpStatusCode.BadRequest, TrainerActionResponse(false, "This trainer doesn't teach that ability"))

            // Get ability details
            val ability = AbilityRepository.findById(request.abilityId)
                ?: return@post call.respond(HttpStatusCode.NotFound, TrainerActionResponse(false, "Ability not found"))

            // Check if already learned
            if (request.abilityId in user.learnedAbilityIds) {
                return@post call.respond(TrainerActionResponse(
                    success = false,
                    message = "You have already learned ${ability.name}",
                    user = user.toResponse()
                ))
            }

            // Check level requirement
            if (user.level < ability.minLevel) {
                return@post call.respond(TrainerActionResponse(
                    success = false,
                    message = "You must be level ${ability.minLevel} to learn ${ability.name} (current level: ${user.level})",
                    user = user.toResponse()
                ))
            }

            // Check gold
            if (user.gold < abilityEntry.goldCost) {
                return@post call.respond(TrainerActionResponse(
                    success = false,
                    message = "Not enough gold to learn ${ability.name} (need ${abilityEntry.goldCost}g, have ${user.gold}g)",
                    user = user.toResponse()
                ))
            }

            // Spend gold
            val goldSpent = UserRepository.spendGold(request.userId, abilityEntry.goldCost)
            if (!goldSpent) {
                return@post call.respond(HttpStatusCode.InternalServerError, TrainerActionResponse(false, "Failed to spend gold"))
            }

            // Learn the ability
            val learned = UserRepository.learnAbility(request.userId, request.abilityId)
            if (!learned) {
                // Refund gold if learning failed
                UserRepository.addGold(request.userId, abilityEntry.goldCost)
                return@post call.respond(HttpStatusCode.InternalServerError, TrainerActionResponse(false, "Failed to learn ability"))
            }

            // Success!
            val updatedUser = UserRepository.findById(request.userId)
            call.respond(TrainerActionResponse(
                success = true,
                message = "${creature.name} teaches you ${ability.name}! (Cost: ${abilityEntry.goldCost}g)",
                user = updatedUser?.toResponse()
            ))
        }

        // Get abilities a user can use in combat (learned + meets level requirement)
        get("/usable/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing user ID"))

            val abilities = UserRepository.getUsableAbilities(userId)
            call.respond(abilities)
        }

        // Get all abilities a user has learned (regardless of level)
        get("/learned/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing user ID"))

            val abilities = UserRepository.getLearnedAbilities(userId)
            call.respond(abilities)
        }
    }
}
