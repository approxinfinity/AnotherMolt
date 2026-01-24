package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.pdfRoutes() {
    route("/pdf") {
        // Analyze a PDF file
        post("/analyze") {
            val multipart = call.receiveMultipart()
            var pdfBytes: ByteArray? = null
            var analysisType: String? = null
            var areaId: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        pdfBytes = part.streamProvider().readBytes()
                    }
                    is PartData.FormItem -> {
                        when (part.name) {
                            "analysisType" -> analysisType = part.value
                            "areaId" -> areaId = part.value
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (pdfBytes == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    PdfAnalysisResult(
                        success = false,
                        analysisType = analysisType ?: "unknown",
                        error = "No PDF file provided"
                    )
                )
                return@post
            }

            val type = try {
                PdfAnalysisType.valueOf(analysisType?.uppercase() ?: "MAP")
            } catch (e: Exception) {
                PdfAnalysisType.MAP
            }

            val result = PdfService.analyzePdf(pdfBytes!!, type, areaId)
            call.respond(result)
        }

        // Extract text only (no LLM analysis)
        post("/extract-text") {
            val multipart = call.receiveMultipart()
            var pdfBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        pdfBytes = part.streamProvider().readBytes()
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (pdfBytes == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "No PDF file provided")
                )
                return@post
            }

            try {
                val text = PdfService.extractText(pdfBytes!!)
                call.respond(mapOf("text" to text))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to extract text: ${e.message}")
                )
            }
        }

        // Get supported analysis types
        get("/analysis-types") {
            call.respond(mapOf(
                "types" to PdfAnalysisType.entries.map {
                    mapOf(
                        "value" to it.name,
                        "description" to when (it) {
                            PdfAnalysisType.MAP -> "ASCII map with locations and connections"
                            PdfAnalysisType.CLASSES -> "Character classes with abilities"
                            PdfAnalysisType.ITEMS -> "Item lists with descriptions"
                            PdfAnalysisType.CREATURES -> "Creature/monster lists"
                            PdfAnalysisType.ABILITIES -> "Standalone ability lists"
                        }
                    )
                }
            ))
        }
    }
}

fun Route.contentGenerationRoutes() {
    route("/generate") {
        get("/status") {
            val available = ContentGenerationService.isAvailable()
            call.respond(mapOf("available" to available))
        }

        post("/location") {
            val request = call.receive<GenerateLocationContentRequest>()
            ContentGenerationService.generateLocationContent(
                exitIds = request.exitIds,
                featureIds = request.featureIds,
                existingName = request.existingName,
                existingDesc = request.existingDesc
            ).onSuccess { content ->
                call.respond(GeneratedContentResponse(
                    name = content.name,
                    description = content.description
                ))
            }.onFailure { error ->
                call.application.log.error("Failed to generate location content: ${error.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to (error.message ?: "Failed to generate content")
                ))
            }
        }

        post("/creature") {
            val request = call.receive<GenerateCreatureContentRequest>()
            ContentGenerationService.generateCreatureContent(
                existingName = request.existingName,
                existingDesc = request.existingDesc
            ).onSuccess { content ->
                call.respond(GeneratedContentResponse(
                    name = content.name,
                    description = content.description
                ))
            }.onFailure { error ->
                call.application.log.error("Failed to generate creature content: ${error.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to (error.message ?: "Failed to generate content")
                ))
            }
        }

        post("/item") {
            val request = call.receive<GenerateItemContentRequest>()
            ContentGenerationService.generateItemContent(
                existingName = request.existingName,
                existingDesc = request.existingDesc
            ).onSuccess { content ->
                call.respond(GeneratedContentResponse(
                    name = content.name,
                    description = content.description
                ))
            }.onFailure { error ->
                call.application.log.error("Failed to generate item content: ${error.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to (error.message ?: "Failed to generate content")
                ))
            }
        }
    }
}
