package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoomRequest(
    val id: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val creatureIds: List<String> = emptyList(),
    val exitIds: List<String> = emptyList(),
    val features: List<String> = emptyList()
)

@Serializable
data class CreateCreatureRequest(
    val id: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val features: List<String> = emptyList()
)

@Serializable
data class CreateItemRequest(
    val id: String,
    val desc: String,
    val featureIds: List<String> = emptyList()
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

    // Initialize database
    val dbPath = appConfig.propertyOrNull("database.path")?.getString() ?: "data/anotherthread.db"
    DatabaseConfig.init(dbPath)
    log.info("Database initialized at $dbPath")

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        get("/health") {
            call.respondText("OK")
        }

        // Room routes
        route("/rooms") {
            get {
                call.respond(RoomRepository.findAll())
            }
            post {
                val request = call.receive<CreateRoomRequest>()
                val room = Room(
                    id = request.id,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    creatureIds = request.creatureIds,
                    exitIds = request.exitIds,
                    features = request.features
                )
                RoomRepository.create(room)
                call.respond(HttpStatusCode.Created, room)
            }
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CreateRoomRequest>()
                val room = Room(
                    id = id,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    creatureIds = request.creatureIds,
                    exitIds = request.exitIds,
                    features = request.features
                )
                if (RoomRepository.update(room)) {
                    call.respond(HttpStatusCode.OK, room)
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
                    id = request.id,
                    desc = request.desc,
                    itemIds = request.itemIds,
                    features = request.features
                )
                CreatureRepository.create(creature)
                call.respond(HttpStatusCode.Created, creature)
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
                    id = request.id,
                    desc = request.desc,
                    featureIds = request.featureIds
                )
                ItemRepository.create(item)
                call.respond(HttpStatusCode.Created, item)
            }
        }
    }
}