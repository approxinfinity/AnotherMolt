package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.ManualTestItem
import com.ez2bg.anotherthread.database.ManualTestItemRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateManualTestItemRequest(
    val featureName: String,
    val description: String,
    val category: String,
    val commitHash: String? = null
)

@Serializable
data class MarkTestedRequest(
    val userId: String,
    val userName: String,
    val notes: String? = null
)

@Serializable
data class UpdateManualTestItemRequest(
    val featureName: String,
    val description: String,
    val category: String,
    val commitHash: String? = null,
    val notes: String? = null
)

@Serializable
data class ManualTestCountsResponse(
    val untested: Int,
    val tested: Int
)

fun Route.manualTestRoutes() {
    route("/manual-tests") {
        // Get counts for dashboard
        get("/counts") {
            val (untested, tested) = ManualTestItemRepository.getCounts()
            call.respond(ManualTestCountsResponse(untested, tested))
        }

        // Get all categories
        get("/categories") {
            call.respond(ManualTestItemRepository.getCategories())
        }

        // Get all test items
        get {
            call.respond(ManualTestItemRepository.findAll())
        }

        // Get untested items only
        get("/untested") {
            call.respond(ManualTestItemRepository.findUntested())
        }

        // Get tested items only
        get("/tested") {
            call.respond(ManualTestItemRepository.findTested())
        }

        // Get items by category
        get("/category/{category}") {
            val category = call.parameters["category"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(ManualTestItemRepository.findByCategory(category))
        }

        // Get single item by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val item = ManualTestItemRepository.findById(id)
            if (item != null) {
                call.respond(item)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Create new test item
        post {
            val request = call.receive<CreateManualTestItemRequest>()
            val item = ManualTestItem(
                featureName = request.featureName,
                description = request.description,
                category = request.category,
                commitHash = request.commitHash
            )
            val created = ManualTestItemRepository.create(item)
            call.respond(HttpStatusCode.Created, created)
        }

        // Mark item as tested
        post("/{id}/mark-tested") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<MarkTestedRequest>()

            val success = ManualTestItemRepository.markTested(
                id = id,
                userId = request.userId,
                userName = request.userName,
                notes = request.notes
            )

            if (success) {
                val updated = ManualTestItemRepository.findById(id)
                call.respond(updated ?: HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Unmark item (move back to untested)
        post("/{id}/unmark-tested") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            val success = ManualTestItemRepository.unmarkTested(id)
            if (success) {
                val updated = ManualTestItemRepository.findById(id)
                call.respond(updated ?: HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Update test item
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateManualTestItemRequest>()

            val existing = ManualTestItemRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            val updated = existing.copy(
                featureName = request.featureName,
                description = request.description,
                category = request.category,
                commitHash = request.commitHash,
                notes = request.notes
            )

            val success = ManualTestItemRepository.update(updated)
            if (success) {
                call.respond(ManualTestItemRepository.findById(id) ?: updated)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        // Delete test item
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val success = ManualTestItemRepository.delete(id)
            if (success) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
