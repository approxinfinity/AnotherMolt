package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.CreateItemRequest
import com.ez2bg.anotherthread.ImageGenerationService
import com.ez2bg.anotherthread.LockRequest
import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch

fun Route.itemRoutes() {
    route("/items") {
        get {
            call.respond(ItemRepository.findAll())
        }
        post {
            val request = call.receive<CreateItemRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val item = Item(
                name = request.name,
                desc = request.desc,
                featureIds = request.featureIds,
                abilityIds = request.abilityIds,
                equipmentType = request.equipmentType,
                equipmentSlot = request.equipmentSlot,
                statBonuses = request.statBonuses,
                value = request.value
            )
            val createdItem = ItemRepository.create(item)

            // Audit log
            AuditLogRepository.log(
                recordId = createdItem.id,
                recordType = "Item",
                recordName = createdItem.name,
                action = AuditAction.CREATE,
                userId = userId,
                userName = userName
            )

            // Trigger image generation in background
            if (request.desc.isNotBlank()) {
                application.launch {
                    ImageGenerationService.generateImage(
                        entityType = "item",
                        entityId = createdItem.id,
                        description = request.desc,
                        entityName = request.name
                    ).onSuccess { imageUrl ->
                        ItemRepository.updateImageUrl(createdItem.id, imageUrl)
                        application.log.info("Generated image for item ${createdItem.id}: $imageUrl")
                    }.onFailure { error ->
                        application.log.warn("Failed to generate image for item ${createdItem.id}: ${error.message}")
                    }
                }
            }

            call.respond(HttpStatusCode.Created, createdItem)
        }
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CreateItemRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val existingItem = ItemRepository.findById(id)
            val descChanged = existingItem?.desc != request.desc

            val item = Item(
                id = id,
                name = request.name,
                desc = request.desc,
                featureIds = request.featureIds,
                abilityIds = request.abilityIds,
                imageUrl = existingItem?.imageUrl,
                lockedBy = existingItem?.lockedBy, // Preserve lock status
                equipmentType = request.equipmentType,
                equipmentSlot = request.equipmentSlot,
                statBonuses = request.statBonuses,
                value = request.value
            )

            if (ItemRepository.update(item)) {
                // Audit log
                AuditLogRepository.log(
                    recordId = item.id,
                    recordType = "Item",
                    recordName = item.name,
                    action = AuditAction.UPDATE,
                    userId = userId,
                    userName = userName
                )
                if (descChanged && request.desc.isNotBlank()) {
                    application.launch {
                        ImageGenerationService.generateImage(
                            entityType = "item",
                            entityId = id,
                            description = request.desc,
                            entityName = request.name
                        ).onSuccess { imageUrl ->
                            ItemRepository.updateImageUrl(id, imageUrl)
                            application.log.info("Regenerated image for item $id: $imageUrl")
                        }.onFailure { error ->
                            application.log.warn("Failed to regenerate image for item $id: ${error.message}")
                        }
                    }
                }
                call.respond(HttpStatusCode.OK, item)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        put("/{id}/lock") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<LockRequest>()

            val existingItem = ItemRepository.findById(id)
                ?: return@put call.respond(HttpStatusCode.NotFound)

            // Toggle lock: if currently locked by this user, unlock; otherwise lock
            val newLockedBy = if (existingItem.lockedBy == request.userId) {
                null // Unlock
            } else {
                request.userId // Lock
            }

            if (ItemRepository.updateLockedBy(id, newLockedBy)) {
                val updatedItem = ItemRepository.findById(id)
                call.respond(HttpStatusCode.OK, updatedItem!!)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val existingItem = ItemRepository.findById(id)
                ?: return@delete call.respond(HttpStatusCode.NotFound)

            // Remove this item from all locations' itemIds
            LocationRepository.removeItemIdFromAll(id)

            if (ItemRepository.delete(id)) {
                // Audit log
                AuditLogRepository.log(
                    recordId = id,
                    recordType = "Item",
                    recordName = existingItem.name,
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
