package com.ez2bg.anotherthread.api

import com.ez2bg.anotherthread.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RoomDto(
    val id: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val creatureIds: List<String> = emptyList(),
    val exitIds: List<String> = emptyList(),
    val features: List<String> = emptyList()
)

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

object ApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val baseUrl = AppConfig.api.baseUrl

    suspend fun getRooms(): Result<List<RoomDto>> = runCatching {
        client.get("$baseUrl/rooms").body()
    }

    suspend fun createRoom(request: CreateRoomRequest): Result<Unit> = runCatching {
        client.post("$baseUrl/rooms") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    suspend fun updateRoom(id: String, request: CreateRoomRequest): Result<Unit> = runCatching {
        client.put("$baseUrl/rooms/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    suspend fun createCreature(request: CreateCreatureRequest): Result<Unit> = runCatching {
        client.post("$baseUrl/creatures") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    suspend fun createItem(request: CreateItemRequest): Result<Unit> = runCatching {
        client.post("$baseUrl/items") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }
}
