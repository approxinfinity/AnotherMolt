package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.CreateCreatureRequest
import com.ez2bg.anotherthread.ImageGenerationService
import com.ez2bg.anotherthread.LockRequest
import com.ez2bg.anotherthread.combat.CombatService
import com.ez2bg.anotherthread.database.AuditAction
import com.ez2bg.anotherthread.database.AuditLogRepository
import com.ez2bg.anotherthread.database.Creature
import com.ez2bg.anotherthread.database.CreatureRepository
import com.ez2bg.anotherthread.database.FeatureRepository
import com.ez2bg.anotherthread.database.LocationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CreatureResponse(
    val id: String,
    val name: String,
    val desc: String,
    val itemIds: List<String>,
    val featureIds: List<String>,
    val imageUrl: String?,
    val lockedBy: String?,
    val maxHp: Int,
    val baseDamage: Int,
    val damageDice: String?,
    val abilityIds: List<String>,
    val level: Int,
    val experienceValue: Int,
    val challengeRating: Int,
    val isAggressive: Boolean,
    val isAlly: Boolean,
    val lootTableId: String?,
    val minGoldDrop: Int,
    val maxGoldDrop: Int,
    val attribution: String?,
    val isTrainer: Boolean
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Check if a creature is a trainer by looking for a feature with featureType="trainer".
 */
private fun isTrainer(creature: Creature): Boolean {
    for (featureId in creature.featureIds) {
        val feature = FeatureRepository.findById(featureId) ?: continue
        try {
            val data = json.decodeFromString<TrainerFeatureData>(feature.data)
            if (data.featureType == "trainer") return true
        } catch (_: Exception) {
            // Not a trainer feature, continue
        }
    }
    return false
}

private fun Creature.toResponse(): CreatureResponse = CreatureResponse(
    id = id,
    name = name,
    desc = desc,
    itemIds = itemIds,
    featureIds = featureIds,
    imageUrl = imageUrl,
    lockedBy = lockedBy,
    maxHp = maxHp,
    baseDamage = baseDamage,
    damageDice = damageDice,
    abilityIds = abilityIds,
    level = level,
    experienceValue = experienceValue,
    challengeRating = challengeRating,
    isAggressive = isAggressive,
    isAlly = isAlly,
    lootTableId = lootTableId,
    minGoldDrop = minGoldDrop,
    maxGoldDrop = maxGoldDrop,
    attribution = attribution,
    isTrainer = isTrainer(this)
)

fun Route.creatureRoutes() {
    route("/creatures") {
        get {
            val creatures = CreatureRepository.findAll()
            call.respond(creatures.map { it.toResponse() })
        }

        // Get activity states for all creatures (wandering, in_combat, idle)
        get("/states") {
            val states = CombatService.getAllCreatureStates()
            call.respond(states.mapValues { it.value.name.lowercase() })
        }

        post {
            val request = call.receive<CreateCreatureRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val creature = Creature(
                name = request.name,
                desc = request.desc,
                itemIds = request.itemIds,
                featureIds = request.featureIds,
                maxHp = request.maxHp,
                baseDamage = request.baseDamage,
                abilityIds = request.abilityIds,
                level = request.level,
                experienceValue = request.experienceValue,
                challengeRating = request.challengeRating,
                isAggressive = request.isAggressive,
                isAlly = request.isAlly
            )
            val createdCreature = CreatureRepository.create(creature)

            // Audit log
            AuditLogRepository.log(
                recordId = createdCreature.id,
                recordType = "Creature",
                recordName = createdCreature.name,
                action = AuditAction.CREATE,
                userId = userId,
                userName = userName
            )

            // Trigger image generation in background
            if (request.desc.isNotBlank()) {
                application.launch {
                    ImageGenerationService.generateImage(
                        entityType = "creature",
                        entityId = createdCreature.id,
                        description = request.desc,
                        entityName = request.name
                    ).onSuccess { imageUrl ->
                        CreatureRepository.updateImageUrl(createdCreature.id, imageUrl)
                        application.log.info("Generated image for creature ${createdCreature.id}: $imageUrl")
                    }.onFailure { error ->
                        application.log.warn("Failed to generate image for creature ${createdCreature.id}: ${error.message}")
                    }
                }
            }

            call.respond(HttpStatusCode.Created, createdCreature.toResponse())
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CreateCreatureRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val existingCreature = CreatureRepository.findById(id)
            val descChanged = existingCreature?.desc != request.desc

            val creature = Creature(
                id = id,
                name = request.name,
                desc = request.desc,
                itemIds = request.itemIds,
                featureIds = request.featureIds,
                imageUrl = existingCreature?.imageUrl,
                lockedBy = existingCreature?.lockedBy, // Preserve lock status
                maxHp = request.maxHp,
                baseDamage = request.baseDamage,
                abilityIds = request.abilityIds,
                level = request.level,
                experienceValue = request.experienceValue,
                challengeRating = request.challengeRating,
                isAggressive = request.isAggressive,
                isAlly = request.isAlly
            )

            if (CreatureRepository.update(creature)) {
                // Audit log
                AuditLogRepository.log(
                    recordId = creature.id,
                    recordType = "Creature",
                    recordName = creature.name,
                    action = AuditAction.UPDATE,
                    userId = userId,
                    userName = userName
                )
                if (descChanged && request.desc.isNotBlank()) {
                    application.launch {
                        ImageGenerationService.generateImage(
                            entityType = "creature",
                            entityId = id,
                            description = request.desc,
                            entityName = request.name
                        ).onSuccess { imageUrl ->
                            CreatureRepository.updateImageUrl(id, imageUrl)
                            application.log.info("Regenerated image for creature $id: $imageUrl")
                        }.onFailure { error ->
                            application.log.warn("Failed to regenerate image for creature $id: ${error.message}")
                        }
                    }
                }
                call.respond(HttpStatusCode.OK, creature.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/{id}/lock") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<LockRequest>()

            val existingCreature = CreatureRepository.findById(id)
                ?: return@put call.respond(HttpStatusCode.NotFound)

            // Toggle lock: if currently locked by this user, unlock; otherwise lock
            val newLockedBy = if (existingCreature.lockedBy == request.userId) {
                null // Unlock
            } else {
                request.userId // Lock
            }

            if (CreatureRepository.updateLockedBy(id, newLockedBy)) {
                val updatedCreature = CreatureRepository.findById(id)!!
                call.respond(HttpStatusCode.OK, updatedCreature.toResponse())
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val existingCreature = CreatureRepository.findById(id)
                ?: return@delete call.respond(HttpStatusCode.NotFound)

            // Remove this creature from all locations' creatureIds
            LocationRepository.removeCreatureIdFromAll(id)

            if (CreatureRepository.delete(id)) {
                // Audit log
                AuditLogRepository.log(
                    recordId = id,
                    recordType = "Creature",
                    recordName = existingCreature.name,
                    action = AuditAction.DELETE,
                    userId = userId,
                    userName = userName
                )
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
