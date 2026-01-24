package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.ClassGenerationService
import com.ez2bg.anotherthread.CreateAbilityRequest
import com.ez2bg.anotherthread.CreateCharacterClassRequest
import com.ez2bg.anotherthread.CreateNerfRequestRequest
import com.ez2bg.anotherthread.GenerateClassRequest
import com.ez2bg.anotherthread.MatchClassRequest
import com.ez2bg.anotherthread.ResolveNerfRequestRequest
import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

fun Route.classRoutes() {
    route("/classes") {
        get {
            val isAdmin = call.request.header("X-Is-Admin")?.toBoolean() ?: false
            val allClasses = CharacterClassRepository.findAll()
            // Non-admins only see stock classes (createdByUserId == null)
            val visibleClasses = if (isAdmin) allClasses else allClasses.filter { it.createdByUserId == null }
            call.respond(visibleClasses)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val characterClass = CharacterClassRepository.findById(id)
            if (characterClass != null) {
                call.respond(characterClass)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post {
            val request = call.receive<CreateCharacterClassRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val characterClass = CharacterClass(
                name = request.name,
                description = request.description,
                isSpellcaster = request.isSpellcaster,
                hitDie = request.hitDie,
                primaryAttribute = request.primaryAttribute,
                imageUrl = request.imageUrl,
                powerBudget = request.powerBudget,
                isPublic = request.isPublic,
                createdByUserId = request.createdByUserId
            )
            val created = CharacterClassRepository.create(characterClass)

            AuditLogRepository.log(
                recordId = created.id,
                recordType = "CharacterClass",
                recordName = created.name,
                action = AuditAction.CREATE,
                userId = userId,
                userName = userName
            )

            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CreateCharacterClassRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"
            val isAdmin = call.request.header("X-Is-Admin")?.toBoolean() ?: false

            val existing = CharacterClassRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            // Only admin can edit locked classes
            if (existing.isLocked && !isAdmin) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Class is locked"))
                return@put
            }

            val updated = existing.copy(
                name = request.name,
                description = request.description,
                isSpellcaster = request.isSpellcaster,
                hitDie = request.hitDie,
                primaryAttribute = request.primaryAttribute,
                imageUrl = request.imageUrl,
                powerBudget = request.powerBudget,
                isPublic = request.isPublic,
                createdByUserId = request.createdByUserId
            )
            CharacterClassRepository.update(updated)

            AuditLogRepository.log(
                recordId = updated.id,
                recordType = "CharacterClass",
                recordName = updated.name,
                action = AuditAction.UPDATE,
                userId = userId,
                userName = userName
            )

            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val existing = CharacterClassRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }

            // Cannot delete locked classes
            if (existing.isLocked) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot delete locked class"))
                return@delete
            }

            CharacterClassRepository.delete(id)

            AuditLogRepository.log(
                recordId = id,
                recordType = "CharacterClass",
                recordName = existing.name,
                action = AuditAction.DELETE,
                userId = userId,
                userName = userName
            )

            call.respond(HttpStatusCode.NoContent)
        }

        put("/{id}/lock") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val isAdmin = call.request.header("X-Is-Admin")?.toBoolean() ?: false

            if (!isAdmin) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                return@put
            }

            val existing = CharacterClassRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            // Toggle lock state
            val newLockState = !existing.isLocked
            CharacterClassRepository.updateLocked(id, newLockState)

            AuditLogRepository.log(
                recordId = id,
                recordType = "CharacterClass",
                recordName = existing.name,
                action = if (newLockState) AuditAction.LOCK else AuditAction.UNLOCK,
                userId = userId,
                userName = userName
            )

            call.respond(CharacterClassRepository.findById(id)!!)
        }
    }
}

fun Route.abilityRoutes() {
    route("/abilities") {
        get {
            call.respond(AbilityRepository.findAll())
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val ability = AbilityRepository.findById(id)
            if (ability != null) {
                call.respond(ability)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/class/{classId}") {
            val classId = call.parameters["classId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(AbilityRepository.findByClassId(classId))
        }

        post {
            val request = call.receive<CreateAbilityRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val ability = Ability(
                name = request.name,
                description = request.description,
                classId = request.classId,
                abilityType = request.abilityType,
                targetType = request.targetType,
                range = request.range,
                cooldownType = request.cooldownType,
                cooldownRounds = request.cooldownRounds,
                effects = request.effects,
                imageUrl = request.imageUrl,
                baseDamage = request.baseDamage,
                durationRounds = request.durationRounds
            )
            val created = AbilityRepository.create(ability)

            AuditLogRepository.log(
                recordId = created.id,
                recordType = "Ability",
                recordName = created.name,
                action = AuditAction.CREATE,
                userId = userId,
                userName = userName
            )

            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CreateAbilityRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val existing = AbilityRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            val updated = existing.copy(
                name = request.name,
                description = request.description,
                classId = request.classId,
                abilityType = request.abilityType,
                targetType = request.targetType,
                range = request.range,
                cooldownType = request.cooldownType,
                cooldownRounds = request.cooldownRounds,
                effects = request.effects,
                imageUrl = request.imageUrl,
                baseDamage = request.baseDamage,
                durationRounds = request.durationRounds
            )
            AbilityRepository.update(updated)

            AuditLogRepository.log(
                recordId = updated.id,
                recordType = "Ability",
                recordName = updated.name,
                action = AuditAction.UPDATE,
                userId = userId,
                userName = userName
            )

            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val existing = AbilityRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }

            AbilityRepository.delete(id)

            AuditLogRepository.log(
                recordId = id,
                recordType = "Ability",
                recordName = existing.name,
                action = AuditAction.DELETE,
                userId = userId,
                userName = userName
            )

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun Route.classGenerationRoutes() {
    route("/class-generation") {
        // Match character description to existing class
        post("/match") {
            val request = call.receive<MatchClassRequest>()

            val result = ClassGenerationService.matchToExistingClass(request.characterDescription)
            result.onSuccess { matchResult ->
                call.respond(matchResult)
            }.onFailure { error ->
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (error.message ?: "Matching failed")))
            }
        }

        // Generate new class from description
        post("/generate") {
            val request = call.receive<GenerateClassRequest>()
            val userId = call.request.header("X-User-Id")
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val result = ClassGenerationService.generateNewClass(
                characterDescription = request.characterDescription,
                createdByUserId = userId,
                isPublic = request.isPublic
            )

            result.onSuccess { (characterClass, abilities) ->
                // Save to database
                val (savedClass, savedAbilities) = ClassGenerationService.saveGeneratedClass(characterClass, abilities)

                AuditLogRepository.log(
                    recordId = savedClass.id,
                    recordType = "CharacterClass",
                    recordName = savedClass.name,
                    action = AuditAction.CREATE,
                    userId = userId ?: "unknown",
                    userName = userName
                )

                call.respond(HttpStatusCode.Created, mapOf(
                    "characterClass" to savedClass,
                    "abilities" to savedAbilities,
                    "totalPowerCost" to savedAbilities.sumOf { it.powerCost }
                ))
            }.onFailure { error ->
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (error.message ?: "Generation failed")))
            }
        }

        // Get LLM availability status
        get("/status") {
            val available = ClassGenerationService.isAvailable()
            call.respond(mapOf("available" to available))
        }
    }
}

fun Route.nerfRequestRoutes() {
    route("/nerf-requests") {
        get {
            call.respond(NerfRequestRepository.findAll())
        }

        get("/pending") {
            call.respond(NerfRequestRepository.findPending())
        }

        get("/pending/count") {
            call.respond(mapOf("count" to NerfRequestRepository.countPending()))
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val nerfRequest = NerfRequestRepository.findById(id)
            if (nerfRequest != null) {
                call.respond(nerfRequest)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/ability/{abilityId}") {
            val abilityId = call.parameters["abilityId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(NerfRequestRepository.findByAbilityId(abilityId))
        }

        // Create a new nerf request
        post {
            val request = call.receive<CreateNerfRequestRequest>()
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val userName = call.request.header("X-User-Name") ?: "unknown"

            // Get the ability and generate suggested changes
            val ability = AbilityRepository.findById(request.abilityId)
            if (ability == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Ability not found"))
                return@post
            }

            // Generate suggested rebalance using LLM
            val suggestedChanges = ClassGenerationService.suggestRebalance(ability).getOrNull()

            val nerfRequest = NerfRequest(
                abilityId = request.abilityId,
                requestedByUserId = userId,
                requestedByUserName = userName,
                reason = request.reason,
                suggestedChanges = suggestedChanges?.let {
                    Json.encodeToString(
                        serializer(),
                        it
                    )
                }
            )

            val created = NerfRequestRepository.create(nerfRequest)

            AuditLogRepository.log(
                recordId = created.id,
                recordType = "NerfRequest",
                recordName = "Nerf: ${ability.name}",
                action = AuditAction.CREATE,
                userId = userId,
                userName = userName
            )

            call.respond(HttpStatusCode.Created, created)
        }

        // Resolve a nerf request (admin only)
        put("/{id}/resolve") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<ResolveNerfRequestRequest>()
            val userId = call.request.header("X-User-Id") ?: return@put call.respond(HttpStatusCode.Unauthorized)
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val nerfRequest = NerfRequestRepository.findById(id)
            if (nerfRequest == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            // If applying changes
            if (request.applyChanges && request.status == "approved" && nerfRequest.suggestedChanges != null) {
                val suggestedAbility = Json.decodeFromString<Ability>(nerfRequest.suggestedChanges)
                val existingAbility = AbilityRepository.findById(nerfRequest.abilityId)
                if (existingAbility != null) {
                    val updatedAbility = existingAbility.copy(
                        description = suggestedAbility.description,
                        targetType = suggestedAbility.targetType,
                        range = suggestedAbility.range,
                        cooldownType = suggestedAbility.cooldownType,
                        cooldownRounds = suggestedAbility.cooldownRounds,
                        baseDamage = suggestedAbility.baseDamage,
                        durationRounds = suggestedAbility.durationRounds,
                        effects = suggestedAbility.effects
                    ).withCalculatedCost()
                    AbilityRepository.update(updatedAbility)

                    AuditLogRepository.log(
                        recordId = existingAbility.id,
                        recordType = "Ability",
                        recordName = existingAbility.name,
                        action = AuditAction.UPDATE,
                        userId = userId,
                        userName = userName
                    )
                }
            }

            val finalStatus = if (request.applyChanges && request.status == "approved") "applied" else request.status

            NerfRequestRepository.resolve(
                id = id,
                status = finalStatus,
                resolvedByUserId = userId,
                adminNotes = request.adminNotes
            )

            AuditLogRepository.log(
                recordId = id,
                recordType = "NerfRequest",
                recordName = "Resolved: $finalStatus",
                action = AuditAction.UPDATE,
                userId = userId,
                userName = userName
            )

            val updated = NerfRequestRepository.findById(id)
            call.respond(updated ?: HttpStatusCode.NotFound)
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val existing = NerfRequestRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }

            NerfRequestRepository.delete(id)

            AuditLogRepository.log(
                recordId = id,
                recordType = "NerfRequest",
                recordName = "Deleted nerf request",
                action = AuditAction.DELETE,
                userId = userId,
                userName = userName
            )

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
