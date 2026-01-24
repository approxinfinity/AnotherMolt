package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.AuditLogRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.auditLogRoutes() {
    route("/audit-logs") {
        get {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
            call.respond(AuditLogRepository.findAll(limit, offset))
        }
        get("/by-record/{recordId}") {
            val recordId = call.parameters["recordId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(AuditLogRepository.findByRecordId(recordId))
        }
        get("/by-type/{recordType}") {
            val recordType = call.parameters["recordType"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            call.respond(AuditLogRepository.findByRecordType(recordType, limit))
        }
        get("/by-user/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            call.respond(AuditLogRepository.findByUserId(userId, limit))
        }
    }
}
