package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class CreateLocationRequest(
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val creatureIds: List<String> = emptyList(),
    val exitIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList()
)

@Serializable
data class CreateCreatureRequest(
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList()
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val desc: String,
    val featureIds: List<String> = emptyList()
)

@Serializable
data class RegisterRequest(
    val name: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val name: String,
    val password: String
)

@Serializable
data class UpdateUserRequest(
    val desc: String = "",
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList()
)

@Serializable
data class UpdateLocationRequest(
    val locationId: String?
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse? = null
)

@Serializable
data class GenerateLocationContentRequest(
    val exitIds: List<String> = emptyList(),
    val existingName: String? = null,
    val existingDesc: String? = null
)

@Serializable
data class GenerateCreatureContentRequest(
    val existingName: String? = null,
    val existingDesc: String? = null
)

@Serializable
data class GenerateItemContentRequest(
    val existingName: String? = null,
    val existingDesc: String? = null
)

@Serializable
data class GeneratedContentResponse(
    val name: String,
    val description: String
)

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val appConfig = environment.config.config("app")
    val appEnv = appConfig.propertyOrNull("environment")?.getString() ?: "development"

    log.info("Starting server in $appEnv environment")

    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
    }

    // Initialize database (TEST_DB_PATH takes precedence for testing)
    val dbPath = System.getProperty("TEST_DB_PATH")
        ?: appConfig.propertyOrNull("database.path")?.getString()
        ?: "data/anotherthread.db"
    DatabaseConfig.init(dbPath)
    log.info("Database initialized at $dbPath")

    // Initialize image directory
    val imageDir = File(System.getenv("IMAGE_DIR") ?: "data/images").also { it.mkdirs() }
    log.info("Image directory: ${imageDir.absolutePath}")

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        get("/health") {
            call.respondText("OK")
        }

        // Serve static images
        staticFiles("/images", imageDir)

        // Image generation status endpoint
        get("/image-generation/status") {
            val available = ImageGenerationService.isAvailable()
            call.respond(mapOf("available" to available))
        }

        // Content generation routes (LLM-based)
        route("/generate") {
            get("/status") {
                val available = ContentGenerationService.isAvailable()
                call.respond(mapOf("available" to available))
            }

            post("/location") {
                val request = call.receive<GenerateLocationContentRequest>()
                ContentGenerationService.generateLocationContent(
                    exitIds = request.exitIds,
                    existingName = request.existingName,
                    existingDesc = request.existingDesc
                ).onSuccess { content ->
                    call.respond(GeneratedContentResponse(
                        name = content.name,
                        description = content.description
                    ))
                }.onFailure { error ->
                    log.error("Failed to generate location content: ${error.message}")
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
                    log.error("Failed to generate creature content: ${error.message}")
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
                    log.error("Failed to generate item content: ${error.message}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "error" to (error.message ?: "Failed to generate content")
                    ))
                }
            }
        }

        // Location routes
        route("/locations") {
            get {
                call.respond(LocationRepository.findAll())
            }
            post {
                val request = call.receive<CreateLocationRequest>()
                val location = Location(
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    creatureIds = request.creatureIds,
                    exitIds = request.exitIds,
                    featureIds = request.featureIds
                )
                val createdLocation = LocationRepository.create(location)

                // Trigger image generation in background if description is provided
                if (request.desc.isNotBlank()) {
                    application.launch {
                        ImageGenerationService.generateImage(
                            entityType = "location",
                            entityId = createdLocation.id,
                            description = request.desc,
                            entityName = request.name
                        ).onSuccess { imageUrl ->
                            LocationRepository.updateImageUrl(createdLocation.id, imageUrl)
                            log.info("Generated image for location ${createdLocation.id}: $imageUrl")
                        }.onFailure { error ->
                            log.warn("Failed to generate image for location ${createdLocation.id}: ${error.message}")
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, createdLocation)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateLocationRequest>()

                // Get existing location to check if description changed
                val existingLocation = LocationRepository.findById(id)
                val descChanged = existingLocation?.desc != request.desc

                val location = Location(
                    id = id,
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    creatureIds = request.creatureIds,
                    exitIds = request.exitIds,
                    featureIds = request.featureIds,
                    imageUrl = existingLocation?.imageUrl // Preserve existing image
                )

                if (LocationRepository.update(location)) {
                    // Regenerate image if description changed
                    if (descChanged && request.desc.isNotBlank()) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "location",
                                entityId = id,
                                description = request.desc,
                                entityName = request.name
                            ).onSuccess { imageUrl ->
                                LocationRepository.updateImageUrl(id, imageUrl)
                                log.info("Regenerated image for location $id: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to regenerate image for location $id: ${error.message}")
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, location)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // Creature routes
        route("/creatures") {
            get {
                call.respond(CreatureRepository.findAll())
            }
            post {
                val request = call.receive<CreateCreatureRequest>()
                val creature = Creature(
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    featureIds = request.featureIds
                )
                val createdCreature = CreatureRepository.create(creature)

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
                            log.info("Generated image for creature ${createdCreature.id}: $imageUrl")
                        }.onFailure { error ->
                            log.warn("Failed to generate image for creature ${createdCreature.id}: ${error.message}")
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, createdCreature)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateCreatureRequest>()

                val existingCreature = CreatureRepository.findById(id)
                val descChanged = existingCreature?.desc != request.desc

                val creature = Creature(
                    id = id,
                    name = request.name,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    featureIds = request.featureIds,
                    imageUrl = existingCreature?.imageUrl
                )

                if (CreatureRepository.update(creature)) {
                    if (descChanged && request.desc.isNotBlank()) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "creature",
                                entityId = id,
                                description = request.desc,
                                entityName = request.name
                            ).onSuccess { imageUrl ->
                                CreatureRepository.updateImageUrl(id, imageUrl)
                                log.info("Regenerated image for creature $id: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to regenerate image for creature $id: ${error.message}")
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, creature)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // User auth routes
        route("/auth") {
            post("/register") {
                val request = call.receive<RegisterRequest>()

                // Validate input
                if (request.name.isBlank() || request.password.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(
                        success = false,
                        message = "Name and password are required"
                    ))
                    return@post
                }

                if (request.password.length < 4) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(
                        success = false,
                        message = "Password must be at least 4 characters"
                    ))
                    return@post
                }

                // Check if username already exists
                if (UserRepository.findByName(request.name) != null) {
                    call.respond(HttpStatusCode.Conflict, AuthResponse(
                        success = false,
                        message = "Username already exists"
                    ))
                    return@post
                }

                // Create user with hashed password
                val user = User(
                    name = request.name,
                    passwordHash = UserRepository.hashPassword(request.password)
                )
                val createdUser = UserRepository.create(user)

                call.respond(HttpStatusCode.Created, AuthResponse(
                    success = true,
                    message = "Registration successful",
                    user = createdUser.toResponse()
                ))
            }

            post("/login") {
                val request = call.receive<LoginRequest>()

                val user = UserRepository.findByName(request.name)
                if (user == null || !UserRepository.verifyPassword(request.password, user.passwordHash)) {
                    call.respond(HttpStatusCode.Unauthorized, AuthResponse(
                        success = false,
                        message = "Invalid username or password"
                    ))
                    return@post
                }

                // Update last active timestamp
                UserRepository.updateLastActiveAt(user.id)

                call.respond(HttpStatusCode.OK, AuthResponse(
                    success = true,
                    message = "Login successful",
                    user = user.toResponse()
                ))
            }
        }

        // User routes (authenticated)
        route("/users") {
            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = UserRepository.findById(id)
                if (user != null) {
                    call.respond(user.toResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateUserRequest>()

                val existingUser = UserRepository.findById(id)
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                val descChanged = existingUser.desc != request.desc

                val updatedUser = existingUser.copy(
                    desc = request.desc,
                    itemIds = request.itemIds,
                    featureIds = request.featureIds,
                    lastActiveAt = System.currentTimeMillis()
                )

                if (UserRepository.update(updatedUser)) {
                    // Trigger image generation if description changed
                    if (descChanged && request.desc.isNotBlank()) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "user",
                                entityId = id,
                                description = request.desc,
                                entityName = existingUser.name
                            ).onSuccess { imageUrl ->
                                UserRepository.updateImageUrl(id, imageUrl)
                                log.info("Generated image for user $id: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to generate image for user $id: ${error.message}")
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, updatedUser.toResponse())
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            put("/{id}/location") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateLocationRequest>()

                if (UserRepository.updateCurrentLocation(id, request.locationId)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Get active users at a location
            get("/at-location/{locationId}") {
                val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val activeUsers = UserRepository.findActiveUsersAtLocation(locationId)
                call.respond(activeUsers.map { it.toResponse() })
            }
        }

        // Item routes
        route("/items") {
            get {
                call.respond(ItemRepository.findAll())
            }
            post {
                val request = call.receive<CreateItemRequest>()
                val item = Item(
                    name = request.name,
                    desc = request.desc,
                    featureIds = request.featureIds
                )
                val createdItem = ItemRepository.create(item)

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
                            log.info("Generated image for item ${createdItem.id}: $imageUrl")
                        }.onFailure { error ->
                            log.warn("Failed to generate image for item ${createdItem.id}: ${error.message}")
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, createdItem)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateItemRequest>()

                val existingItem = ItemRepository.findById(id)
                val descChanged = existingItem?.desc != request.desc

                val item = Item(
                    id = id,
                    name = request.name,
                    desc = request.desc,
                    featureIds = request.featureIds,
                    imageUrl = existingItem?.imageUrl
                )

                if (ItemRepository.update(item)) {
                    if (descChanged && request.desc.isNotBlank()) {
                        application.launch {
                            ImageGenerationService.generateImage(
                                entityType = "item",
                                entityId = id,
                                description = request.desc,
                                entityName = request.name
                            ).onSuccess { imageUrl ->
                                ItemRepository.updateImageUrl(id, imageUrl)
                                log.info("Regenerated image for item $id: $imageUrl")
                            }.onFailure { error ->
                                log.warn("Failed to regenerate image for item $id: ${error.message}")
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, item)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
