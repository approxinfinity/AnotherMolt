package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.CreateFeatureCategoryRequest
import com.ez2bg.anotherthread.CreateFeatureRequest
import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.featureCategoryRoutes() {
    route("/feature-categories") {
        get {
            call.respond(FeatureCategoryRepository.findAll())
        }
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val category = FeatureCategoryRepository.findById(id)
            if (category != null) {
                call.respond(category)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        post {
            val request = call.receive<CreateFeatureCategoryRequest>()
            val category = FeatureCategory(
                name = request.name,
                description = request.description
            )
            val created = FeatureCategoryRepository.create(category)
            call.respond(HttpStatusCode.Created, created)
        }
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CreateFeatureCategoryRequest>()
            val category = FeatureCategory(
                id = id,
                name = request.name,
                description = request.description
            )
            if (FeatureCategoryRepository.update(category)) {
                call.respond(HttpStatusCode.OK, category)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

fun Route.featureRoutes() {
    route("/features") {
        get {
            call.respond(FeatureRepository.findAll())
        }
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val feature = FeatureRepository.findById(id)
            if (feature != null) {
                call.respond(feature)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        get("/by-category/{categoryId}") {
            val categoryId = call.parameters["categoryId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(FeatureRepository.findByCategoryId(categoryId))
        }
        post {
            val request = call.receive<CreateFeatureRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val feature = if (request.id != null) {
                Feature(
                    id = request.id,
                    name = request.name,
                    featureCategoryId = request.featureCategoryId,
                    description = request.description,
                    data = request.data
                )
            } else {
                Feature(
                    name = request.name,
                    featureCategoryId = request.featureCategoryId,
                    description = request.description,
                    data = request.data
                )
            }
            val created = FeatureRepository.create(feature)

            // Audit log
            AuditLogRepository.log(
                recordId = created.id,
                recordType = "Feature",
                recordName = created.name,
                action = AuditAction.CREATE,
                userId = userId,
                userName = userName
            )

            call.respond(HttpStatusCode.Created, created)
        }
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CreateFeatureRequest>()
            val userId = call.request.header("X-User-Id") ?: "unknown"
            val userName = call.request.header("X-User-Name") ?: "unknown"

            val feature = Feature(
                id = id,
                name = request.name,
                featureCategoryId = request.featureCategoryId,
                description = request.description,
                data = request.data
            )
            if (FeatureRepository.update(feature)) {
                // Audit log
                AuditLogRepository.log(
                    recordId = feature.id,
                    recordType = "Feature",
                    recordName = feature.name,
                    action = AuditAction.UPDATE,
                    userId = userId,
                    userName = userName
                )
                call.respond(HttpStatusCode.OK, feature)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
