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
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val creatureIds: List<String> = emptyList(),
    val exitIds: List<String> = emptyList(),
    val features: List<String> = emptyList()
)

@Serializable
data class CreatureDto(
    val id: String,
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val features: List<String> = emptyList()
)

@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val desc: String,
    val featureIds: List<String> = emptyList()
)

@Serializable
data class CreateRoomRequest(
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val creatureIds: List<String> = emptyList(),
    val exitIds: List<String> = emptyList(),
    val features: List<String> = emptyList()
)

@Serializable
data class CreateCreatureRequest(
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val features: List<String> = emptyList()
)

@Serializable
data class CreateItemRequest(
    val name: String,
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

    suspend fun getCreatures(): Result<List<CreatureDto>> = runCatching {
        client.get("$baseUrl/creatures").body()
    }

    suspend fun getCreature(id: String): Result<CreatureDto?> = runCatching {
        val creatures: List<CreatureDto> = client.get("$baseUrl/creatures").body()
        creatures.find { it.id == id }
    }

    suspend fun getItems(): Result<List<ItemDto>> = runCatching {
        client.get("$baseUrl/items").body()
    }

    suspend fun getItem(id: String): Result<ItemDto?> = runCatching {
        val items: List<ItemDto> = client.get("$baseUrl/items").body()
        items.find { it.id == id }
    }

    suspend fun getRoom(id: String): Result<RoomDto?> = runCatching {
        val rooms: List<RoomDto> = client.get("$baseUrl/rooms").body()
        rooms.find { it.id == id }
    }
}
