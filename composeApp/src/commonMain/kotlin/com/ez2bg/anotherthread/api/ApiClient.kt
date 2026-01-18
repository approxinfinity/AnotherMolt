package com.ez2bg.anotherthread.api

import com.ez2bg.anotherthread.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ExitDirection {
    NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST, UNKNOWN
}

@Serializable
data class ExitDto(
    val locationId: String,
    val direction: ExitDirection = ExitDirection.UNKNOWN
)

@Serializable
data class LocationDto(
    val id: String,
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val creatureIds: List<String> = emptyList(),
    val exits: List<ExitDto> = emptyList(),
    val featureIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val lockedBy: String? = null
)

@Serializable
data class CreatureDto(
    val id: String,
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val lockedBy: String? = null
)

@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val desc: String,
    val featureIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val lockedBy: String? = null
)

@Serializable
data class CreateLocationRequest(
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val creatureIds: List<String> = emptyList(),
    val exits: List<ExitDto> = emptyList(),
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
data class LockRequest(
    val userId: String
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val desc: String = "",
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val currentLocationId: String? = null,
    val createdAt: Long = 0,
    val lastActiveAt: Long = 0
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
data class UpdateUserLocationRequest(
    val locationId: String?
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null
)

@Serializable
data class GenerateLocationContentRequest(
    val exitIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
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

@Serializable
data class FeatureDto(
    val id: String,
    val name: String,
    val featureCategoryId: String? = null,
    val description: String,
    val data: String = "{}"
)

@Serializable
data class GenerateImageRequest(
    val entityType: String,
    val entityId: String,
    val name: String,
    val description: String,
    val featureIds: List<String> = emptyList()
)

@Serializable
data class GenerateImageResponse(
    val imageUrl: String
)

@Serializable
data class UploadedFileDto(
    val filename: String,
    val url: String,
    val size: Long,
    val lastModified: Long
)

@Serializable
data class FileUploadResponseDto(
    val success: Boolean,
    val url: String? = null,
    val error: String? = null
)

// Admin feature ID constant
const val ADMIN_FEATURE_ID = "1"

object ApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 180_000 // 3 minutes for image generation
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 180_000
        }
    }

    private val baseUrl = AppConfig.api.baseUrl

    suspend fun getLocations(): Result<List<LocationDto>> = runCatching {
        client.get("$baseUrl/locations").body()
    }

    suspend fun createLocation(request: CreateLocationRequest): Result<LocationDto> = runCatching {
        client.post("$baseUrl/locations") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateLocation(id: String, request: CreateLocationRequest): Result<Unit> = runCatching {
        client.put("$baseUrl/locations/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    suspend fun getLocation(id: String): Result<LocationDto?> = runCatching {
        val locations: List<LocationDto> = client.get("$baseUrl/locations").body()
        locations.find { it.id == id }
    }

    suspend fun toggleLocationLock(locationId: String, userId: String): Result<LocationDto> = runCatching {
        client.put("$baseUrl/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody(LockRequest(userId))
        }.body()
    }

    suspend fun toggleCreatureLock(creatureId: String, userId: String): Result<CreatureDto> = runCatching {
        client.put("$baseUrl/creatures/$creatureId/lock") {
            contentType(ContentType.Application.Json)
            setBody(LockRequest(userId))
        }.body()
    }

    suspend fun toggleItemLock(itemId: String, userId: String): Result<ItemDto> = runCatching {
        client.put("$baseUrl/items/$itemId/lock") {
            contentType(ContentType.Application.Json)
            setBody(LockRequest(userId))
        }.body()
    }

    suspend fun deleteLocation(id: String): Result<Unit> = runCatching {
        client.delete("$baseUrl/locations/$id")
        Unit
    }

    suspend fun deleteCreature(id: String): Result<Unit> = runCatching {
        client.delete("$baseUrl/creatures/$id")
        Unit
    }

    suspend fun deleteItem(id: String): Result<Unit> = runCatching {
        client.delete("$baseUrl/items/$id")
        Unit
    }

    suspend fun createCreature(request: CreateCreatureRequest): Result<CreatureDto> = runCatching {
        client.post("$baseUrl/creatures") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateCreature(id: String, request: CreateCreatureRequest): Result<Unit> = runCatching {
        client.put("$baseUrl/creatures/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    suspend fun createItem(request: CreateItemRequest): Result<ItemDto> = runCatching {
        client.post("$baseUrl/items") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateItem(id: String, request: CreateItemRequest): Result<Unit> = runCatching {
        client.put("$baseUrl/items/$id") {
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

    // User auth methods
    suspend fun register(name: String, password: String): Result<AuthResponse> = runCatching {
        client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(name, password))
        }.body()
    }

    suspend fun login(name: String, password: String): Result<AuthResponse> = runCatching {
        client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(name, password))
        }.body()
    }

    suspend fun getUser(id: String): Result<UserDto?> = runCatching {
        client.get("$baseUrl/users/$id").body()
    }

    suspend fun updateUser(id: String, request: UpdateUserRequest): Result<UserDto> = runCatching {
        client.put("$baseUrl/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateUserLocation(id: String, locationId: String?): Result<Unit> = runCatching {
        client.put("$baseUrl/users/$id/location") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserLocationRequest(locationId))
        }
        Unit
    }

    suspend fun getActiveUsersAtLocation(locationId: String): Result<List<UserDto>> = runCatching {
        client.get("$baseUrl/users/at-location/$locationId").body()
    }

    @Serializable
    private data class ErrorResponse(val error: String)

    // Content generation methods
    suspend fun isContentGenerationAvailable(): Result<Boolean> = runCatching {
        val response: Map<String, Boolean> = client.get("$baseUrl/generate/status").body()
        response["available"] ?: false
    }

    suspend fun generateLocationContent(
        exitIds: List<String> = emptyList(),
        featureIds: List<String> = emptyList(),
        existingName: String? = null,
        existingDesc: String? = null
    ): Result<GeneratedContentResponse> = runCatching {
        val response = client.post("$baseUrl/generate/location") {
            contentType(ContentType.Application.Json)
            setBody(GenerateLocationContentRequest(exitIds, featureIds, existingName, existingDesc))
        }
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody: ErrorResponse = response.body()
            throw Exception(errorBody.error)
        }
    }

    suspend fun generateCreatureContent(
        existingName: String? = null,
        existingDesc: String? = null
    ): Result<GeneratedContentResponse> = runCatching {
        val response = client.post("$baseUrl/generate/creature") {
            contentType(ContentType.Application.Json)
            setBody(GenerateCreatureContentRequest(existingName, existingDesc))
        }
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody: ErrorResponse = response.body()
            throw Exception(errorBody.error)
        }
    }

    suspend fun generateItemContent(
        existingName: String? = null,
        existingDesc: String? = null
    ): Result<GeneratedContentResponse> = runCatching {
        val response = client.post("$baseUrl/generate/item") {
            contentType(ContentType.Application.Json)
            setBody(GenerateItemContentRequest(existingName, existingDesc))
        }
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody: ErrorResponse = response.body()
            throw Exception(errorBody.error)
        }
    }

    // Image generation methods
    suspend fun isImageGenerationAvailable(): Result<Boolean> = runCatching {
        val response: Map<String, Boolean> = client.get("$baseUrl/image-generation/status").body()
        response["available"] ?: false
    }

    suspend fun generateImage(
        entityType: String,
        entityId: String,
        name: String,
        description: String,
        featureIds: List<String> = emptyList()
    ): Result<GenerateImageResponse> = runCatching {
        val response = client.post("$baseUrl/image-generation/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateImageRequest(entityType, entityId, name, description, featureIds))
        }
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody: ErrorResponse = response.body()
            throw Exception(errorBody.error)
        }
    }

    // Feature methods
    suspend fun getFeatures(): Result<List<FeatureDto>> = runCatching {
        client.get("$baseUrl/features").body()
    }

    suspend fun getFeature(id: String): Result<FeatureDto?> = runCatching {
        val response = client.get("$baseUrl/features/$id")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            null
        }
    }

    // Admin file management methods
    suspend fun getUploadedFiles(): Result<List<UploadedFileDto>> = runCatching {
        client.get("$baseUrl/admin/files").body()
    }

    suspend fun uploadFile(filename: String, fileBytes: ByteArray): Result<FileUploadResponseDto> = runCatching {
        val response = client.post("$baseUrl/admin/files/upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                        })
                    }
                )
            )
        }
        if (response.status.isSuccess()) {
            response.body()
        } else {
            FileUploadResponseDto(success = false, error = "Upload failed with status ${response.status}")
        }
    }

    suspend fun deleteUploadedFile(filename: String): Result<Boolean> = runCatching {
        val response = client.delete("$baseUrl/admin/files/$filename")
        response.status.isSuccess()
    }

    suspend fun getAllowedFileTypes(): Result<Set<String>> = runCatching {
        val response: Map<String, Set<String>> = client.get("$baseUrl/admin/files/allowed-types").body()
        response["allowedExtensions"] ?: emptySet()
    }
}
