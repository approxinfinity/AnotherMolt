package com.ez2bg.anotherthread.api

import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.isWebPlatform
import com.ez2bg.anotherthread.state.ConnectionStateHolder
import com.ez2bg.anotherthread.storage.AuthStorage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ExitDirection {
    NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST, ENTER, UNKNOWN
}

enum class LocationType {
    OUTDOOR_GROUND,  // Ground level outdoor - generates wilderness around it
    INDOOR,          // Indoor locations - no wilderness generation
    UNDERGROUND,     // Underground/cave locations
    UNDERWATER,      // Underwater locations
    AERIAL           // Sky/aerial locations
}

@Serializable
data class ExitDto(
    val locationId: String,
    val direction: ExitDirection = ExitDirection.UNKNOWN
)

@Serializable
enum class ShopLayoutDirection {
    VERTICAL,
    HORIZONTAL
}

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
    val lockedBy: String? = null,
    // Grid coordinates - null means not yet placed in a coordinate system
    val gridX: Int? = null,
    val gridY: Int? = null,
    // Area identifier - groups locations into distinct map regions (replaces gridZ)
    val areaId: String? = null,
    // Last edited tracking - null means never edited by a user (e.g., auto-generated)
    val lastEditedBy: String? = null,
    val lastEditedAt: String? = null, // ISO datetime string
    // Location type for determining behavior
    val locationType: LocationType? = null,
    // Biome metadata from world generation
    val biome: String? = null,
    val elevation: Float? = null,
    val moisture: Float? = null,
    val isRiver: Boolean? = null,
    val isCoast: Boolean? = null,
    val terrainFeatures: List<String>? = null,
    val isOriginalTerrain: Boolean? = null,
    // Shop layout direction (default VERTICAL)
    val shopLayoutDirection: ShopLayoutDirection? = null
)

@Serializable
data class CreatureDto(
    val id: String,
    val name: String,
    val desc: String,
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val lockedBy: String? = null,
    // Combat stats
    val maxHp: Int = 10,
    val baseDamage: Int = 5,
    val abilityIds: List<String> = emptyList(),
    val level: Int = 1,
    val experienceValue: Int = 10,
    val isAggressive: Boolean = false
)

@Serializable
data class StatBonusesDto(
    val attack: Int = 0,
    val defense: Int = 0,
    val maxHp: Int = 0
)

@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val desc: String,
    val featureIds: List<String> = emptyList(),
    val abilityIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val lockedBy: String? = null,
    // Equipment fields
    val equipmentType: String? = null,  // "weapon", "armor", "accessory", or null
    val equipmentSlot: String? = null,  // "main_hand", "off_hand", "head", etc.
    val statBonuses: StatBonusesDto? = null,
    val value: Int = 0  // Gold value
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
    val featureIds: List<String> = emptyList(),
    // Combat stats
    val maxHp: Int = 10,
    val baseDamage: Int = 5,
    val abilityIds: List<String> = emptyList(),
    val level: Int = 1,
    val experienceValue: Int = 10,
    val isAggressive: Boolean = false
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
    val characterClassId: String? = null,
    val classGenerationStartedAt: Long? = null,
    val createdAt: Long = 0,
    val lastActiveAt: Long = 0,
    // Combat stats
    val level: Int = 1,
    val experience: Int = 0,
    val maxHp: Int = 10,
    val currentHp: Int = 10,
    val maxMana: Int = 10,
    val currentMana: Int = 10,
    val maxStamina: Int = 10,
    val currentStamina: Int = 10,
    val currentCombatSessionId: String? = null,
    // D&D Attributes
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val attributeQualityBonus: Int = 0,
    val attributesGeneratedAt: Long? = null,
    // Economy and equipment
    val gold: Int = 0,
    val equippedItemIds: List<String> = emptyList(),
    // Generated appearance based on equipment
    val appearanceDescription: String = ""
)

@Serializable
data class DerivedAttributesDto(
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    val qualityBonus: Int,
    val reasoning: String,
    val missingAreas: List<String> = emptyList()
)

@Serializable
data class DeriveAttributesRequestDto(
    val description: String,
    val followUpAnswers: Map<String, String> = emptyMap()
)

@Serializable
data class CommitAttributesRequestDto(
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    val qualityBonus: Int
)

@Serializable
data class IconMappingDto(
    val abilityId: String,
    val iconName: String
)

@Serializable
data class SetIconMappingRequestDto(
    val iconName: String
)

@Serializable
data class PlayerEncounterDto(
    val encounteredUserId: String,
    val classification: String,
    val lastKnownName: String,
    val lastKnownDesc: String,
    val lastKnownImageUrl: String? = null,
    val lastLocationId: String? = null,
    val firstEncounteredAt: Long,
    val lastEncounteredAt: Long,
    val encounterCount: Int
)

@Serializable
data class ClassifyEncounterRequestDto(
    val classification: String
)

@Serializable
data class TeleportDestinationDto(
    val areaId: String,
    val locationId: String,
    val locationName: String
)

@Serializable
data class TeleportRequestDto(
    val userId: String,
    val targetAreaId: String,
    val abilityId: String = "weapon-wayfarer-teleport"
)

@Serializable
data class TeleportResponseDto(
    val success: Boolean,
    val message: String,
    val departureMessage: String? = null,
    val arrivalMessage: String? = null,
    val newLocationId: String? = null,
    val newLocationName: String? = null
)

@Serializable
data class PhasewalkDestinationDto(
    val direction: String,
    val locationId: String,
    val locationName: String,
    val gridX: Int,
    val gridY: Int
)

@Serializable
data class PhasewalkRequestDto(
    val userId: String,
    val direction: String,
    val abilityId: String = "ability-phasewalk"
)

@Serializable
data class PhasewalkResponseDto(
    val success: Boolean,
    val message: String,
    val departureMessage: String? = null,
    val newLocationId: String? = null,
    val newLocationName: String? = null
)

@Serializable
data class UnconnectedAreaDto(
    val areaId: String,
    val name: String,
    val locationCount: Int,
    val entryLocationId: String? = null,
    val entryLocationName: String? = null
)

@Serializable
data class SealableRiftDto(
    val exitLocationId: String,
    val targetAreaId: String,
    val targetAreaName: String,
    val targetLocationName: String
)

@Serializable
data class CreateRiftRequestDto(
    val targetAreaId: String
)

@Serializable
data class CreateRiftResponseDto(
    val success: Boolean,
    val message: String
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
data class UpdateUserClassRequest(
    val classId: String? = null
)

@Serializable
data class AssignClassRequest(
    val generateClass: Boolean,
    val characterDescription: String
)

@Serializable
data class AssignClassResponse(
    val success: Boolean,
    val user: UserDto,
    val assignedClass: CharacterClassDto? = null,
    val message: String? = null
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null,
    val sessionToken: String? = null,  // For native clients
    val expiresAt: Long? = null        // Session expiration timestamp
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

// Service health DTOs
@Serializable
data class ServiceStatusDto(
    val name: String,
    val displayName: String,
    val healthy: Boolean,
    val url: String? = null
)

@Serializable
data class ServiceActionRequest(
    val action: String
)

@Serializable
data class ServiceActionResponse(
    val success: Boolean,
    val message: String
)

// Audit log DTOs
@Serializable
data class AuditLogDto(
    val id: String,
    val recordId: String,
    val recordType: String,
    val recordName: String,
    val action: String,
    val userId: String,
    val userName: String,
    val timestamp: Long
)

// Data integrity DTOs
@Serializable
data class IntegrityIssueDto(
    val type: String,
    val severity: String,
    val locationId: String,
    val locationName: String,
    val message: String,
    val relatedLocationId: String? = null,
    val relatedLocationName: String? = null
)

@Serializable
data class DataIntegrityResponseDto(
    val success: Boolean,
    val totalLocations: Int,
    val issuesFound: Int,
    val issues: List<IntegrityIssueDto>
)

// Admin user DTOs
@Serializable
data class AdminUserInfoDto(
    val id: String,
    val name: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val currentLocationId: String?,
    val currentLocationName: String?,
    val imageUrl: String? = null
)

@Serializable
data class AdminUsersResponseDto(
    val success: Boolean,
    val totalUsers: Int,
    val users: List<AdminUserInfoDto>
)

// Character Class and Ability DTOs
@Serializable
data class CharacterClassDto(
    val id: String,
    val name: String,
    val description: String,
    val isSpellcaster: Boolean,
    val hitDie: Int,
    val primaryAttribute: String,
    val baseMana: Int = 10,
    val baseStamina: Int = 10,
    val imageUrl: String? = null,
    val powerBudget: Int = 100,
    val isPublic: Boolean = true,
    val createdByUserId: String? = null,
    val isLocked: Boolean = true
)

@Serializable
data class CreateCharacterClassRequest(
    val name: String,
    val description: String,
    val isSpellcaster: Boolean,
    val hitDie: Int,
    val primaryAttribute: String,
    val imageUrl: String? = null,
    val powerBudget: Int = 100,
    val isPublic: Boolean = true,
    val createdByUserId: String? = null,
    val isLocked: Boolean = true
)

@Serializable
data class AbilityDto(
    val id: String,
    val name: String,
    val description: String,
    val classId: String? = null,
    val abilityType: String,    // "spell", "combat", "utility", "passive"
    val targetType: String,     // "self", "single_enemy", "single_ally", "area", "all_enemies", "all_allies"
    val range: Int,
    val cooldownType: String,   // "none", "short", "medium", "long"
    val cooldownRounds: Int = 0,
    val effects: String = "[]",
    val imageUrl: String? = null,
    val baseDamage: Int = 0,
    val durationRounds: Int = 0,
    val powerCost: Int = 10,
    val manaCost: Int = 0,
    val staminaCost: Int = 0
)

@Serializable
data class CreateAbilityRequest(
    val name: String,
    val description: String,
    val classId: String? = null,
    val abilityType: String,
    val targetType: String,
    val range: Int,
    val cooldownType: String,
    val cooldownRounds: Int = 0,
    val effects: String = "[]",
    val imageUrl: String? = null,
    val baseDamage: Int = 0,
    val durationRounds: Int = 0
)

@Serializable
data class PdfAnalysisRequest(
    val analysisType: String  // "classes" or "abilities"
)

@Serializable
data class ExtractedClassDto(
    val name: String,
    val description: String,
    val isSpellcaster: Boolean,
    val hitDie: Int,
    val primaryAttribute: String
)

@Serializable
data class ExtractedAbilityDto(
    val name: String,
    val description: String,
    val abilityType: String,
    val targetType: String,
    val range: Int
)

@Serializable
data class PdfAnalysisResponse(
    val success: Boolean,
    val classes: List<ExtractedClassDto> = emptyList(),
    val abilities: List<ExtractedAbilityDto> = emptyList(),
    val error: String? = null
)

// Class generation and matching DTOs
@Serializable
data class MatchClassRequest(
    val characterDescription: String
)

@Serializable
data class ClassMatchResult(
    val matchedClassId: String,
    val matchedClassName: String,
    val confidence: Float,
    val reasoning: String
)

@Serializable
data class GenerateClassRequest(
    val characterDescription: String,
    val isPublic: Boolean = false
)

@Serializable
data class GeneratedClassResponse(
    val characterClass: CharacterClassDto,
    val abilities: List<AbilityDto>,
    val totalPowerCost: Int
)

@Serializable
data class LlmStatusResponse(
    val available: Boolean
)

// Nerf request DTOs
@Serializable
data class NerfRequestDto(
    val id: String,
    val abilityId: String,
    val requestedByUserId: String,
    val requestedByUserName: String,
    val reason: String,
    val status: String,
    val suggestedChanges: String? = null,
    val adminNotes: String? = null,
    val createdAt: Long,
    val resolvedAt: Long? = null,
    val resolvedByUserId: String? = null
)

@Serializable
data class CreateNerfRequestRequest(
    val abilityId: String,
    val reason: String
)

@Serializable
data class ResolveNerfRequestRequest(
    val status: String,
    val adminNotes: String? = null,
    val applyChanges: Boolean = false
)

@Serializable
data class PendingCountResponse(
    val count: Long
)

// Terrain override DTOs
@Serializable
data class ForestParamsDto(
    val treeCount: Int? = null,
    val sizeMultiplier: Float? = null
)

@Serializable
data class LakeParamsDto(
    val diameterMultiplier: Float? = null,  // Legacy - used when X and Y are linked
    val diameterMultiplierX: Float? = null,
    val diameterMultiplierY: Float? = null,
    val shapePoints: Int? = null,
    val noiseScale: Float? = null
)

@Serializable
data class RiverParamsDto(
    val widthMultiplier: Float? = null
)

@Serializable
data class MountainParamsDto(
    val peakCount: Int? = null,
    val heightMultiplier: Float? = null
)

@Serializable
data class GrassParamsDto(
    val tuftCount: Int? = null
)

@Serializable
data class HillsParamsDto(
    val heightMultiplier: Float? = null
)

@Serializable
data class StreamParamsDto(
    val widthMultiplier: Float? = null
)

@Serializable
data class DesertParamsDto(
    val duneCount: Int? = null,
    val heightMultiplier: Float? = null
)

@Serializable
data class SwampParamsDto(
    val densityMultiplier: Float? = null,
    val diameterMultiplierX: Float? = null,
    val diameterMultiplierY: Float? = null,
    val shapePoints: Int? = null,
    val noiseScale: Float? = null
)

@Serializable
data class TerrainOverridesDto(
    val forest: ForestParamsDto? = null,
    val lake: LakeParamsDto? = null,
    val river: RiverParamsDto? = null,
    val mountain: MountainParamsDto? = null,
    val grass: GrassParamsDto? = null,
    val hills: HillsParamsDto? = null,
    val stream: StreamParamsDto? = null,
    val desert: DesertParamsDto? = null,
    val swamp: SwampParamsDto? = null,
    val elevation: Float? = null  // -1.0 (deep water) to 1.0 (mountain peak), null = auto-calculate
)

@Serializable
data class TerrainOverrideDto(
    val locationId: String,
    val overrides: TerrainOverridesDto,
    val updatedBy: String? = null,
    val updatedAt: Long? = null
)

// World Generation DTOs
@Serializable
data class WorldGenParamsDto(
    val width: Int = 20,
    val height: Int = 20,
    val seed: Long? = null,
    val islandFactor: Double = 1.2,
    val noiseFactor: Double = 0.5,
    val landThreshold: Double = 0.3,
    val elevationNoiseScale: Double = 0.08,
    val elevationNoiseWeight: Double = 0.3,
    val moistureNoiseScale: Double = 0.1,
    val moistureDecay: Double = 0.85,
    val moistureNoiseWeight: Double = 0.4,
    val riverDensity: Double = 0.25,
    val riverMinLength: Int = 3,
    val areaId: String = "",
    val areaName: String = "Generated World",
    val gridOffsetX: Int = 0,
    val gridOffsetY: Int = 0,
    val generateNames: Boolean = true,
    val generateDescriptions: Boolean = true,
    val connectWaterCells: Boolean = false
)

@Serializable
data class WorldGenJobResponseDto(
    val success: Boolean,
    val jobId: String? = null,
    val message: String
)

@Serializable
data class WorldGenStatsDto(
    val totalCells: Int,
    val landCells: Int,
    val waterCells: Int,
    val coastalCells: Int,
    val riverCells: Int,
    val biomeDistribution: Map<String, Int>
)

@Serializable
data class WorldGenerationResultDto(
    val success: Boolean,
    val locationIds: List<String> = emptyList(),
    val areaId: String,
    val stats: WorldGenStatsDto? = null,
    val errorMessage: String? = null
)

@Serializable
data class WorldGenJobStatusDto(
    val jobId: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val result: WorldGenerationResultDto? = null,
    val error: String? = null,
    val progress: WorldGenProgressDto? = null
)

@Serializable
data class WorldGenProgressDto(
    val phase: String,
    val current: Int = 0,
    val total: Int = 0,
    val message: String = ""
)

@Serializable
data class AreaInfoDto(
    val areaId: String,
    val locationCount: Int,
    val hasCoordinates: Boolean
)

@Serializable
data class BiomeInfoDto(
    val name: String,
    val displayName: String,
    val terrainWords: List<String>,
    val featureWords: List<String>,
    val colorHex: String
)

@Serializable
data class AreaDeleteResponseDto(
    val success: Boolean,
    val deleted: Int,
    val areaId: String
)

object ApiClient {
    // User context for audit logging
    private var currentUserId: String? = null
    private var currentUserName: String? = null

    fun setUserContext(userId: String?, userName: String?) {
        currentUserId = userId
        currentUserName = userName
    }

    fun clearUserContext() {
        currentUserId = null
        currentUserName = null
    }

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
        // Only use Ktor's HttpCookies on native platforms
        // On web, we rely on the browser's native cookie handling via fetch credentials
        if (!isWebPlatform()) {
            install(HttpCookies)
        }
        install(DefaultRequest) {
            // Add user headers for audit logging if available
            currentUserId?.let { header("X-User-Id", it) }
            currentUserName?.let { header("X-User-Name", it) }

            // For native platforms, add Authorization header with session token
            if (!isWebPlatform()) {
                AuthStorage.getSessionToken()?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }
        }
    }

    private val baseUrl = AppConfig.api.baseUrl

    /**
     * Wrapper that tracks connection status for API calls.
     */
    private inline fun <T> apiCall(block: () -> T): Result<T> {
        return runCatching {
            block()
        }.also { result ->
            result.onSuccess {
                ConnectionStateHolder.recordSuccess()
            }.onFailure { error ->
                ConnectionStateHolder.recordFailure(error)
            }
        }
    }

    suspend fun getLocations(cacheBuster: Long? = null): Result<List<LocationDto>> = apiCall {
        val url = if (cacheBuster != null) "$baseUrl/locations?_=$cacheBuster" else "$baseUrl/locations"
        client.get(url).body()
    }

    suspend fun createLocation(request: CreateLocationRequest): Result<LocationDto> = apiCall {
        client.post("$baseUrl/locations") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateLocation(id: String, request: CreateLocationRequest): Result<Unit> = apiCall {
        client.put("$baseUrl/locations/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    suspend fun getLocation(id: String): Result<LocationDto?> = apiCall {
        val locations: List<LocationDto> = client.get("$baseUrl/locations").body()
        locations.find { it.id == id }
    }

    suspend fun toggleLocationLock(locationId: String, userId: String): Result<LocationDto> = apiCall {
        client.put("$baseUrl/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody(LockRequest(userId))
        }.body()
    }

    suspend fun toggleCreatureLock(creatureId: String, userId: String): Result<CreatureDto> = apiCall {
        client.put("$baseUrl/creatures/$creatureId/lock") {
            contentType(ContentType.Application.Json)
            setBody(LockRequest(userId))
        }.body()
    }

    suspend fun toggleItemLock(itemId: String, userId: String): Result<ItemDto> = apiCall {
        client.put("$baseUrl/items/$itemId/lock") {
            contentType(ContentType.Application.Json)
            setBody(LockRequest(userId))
        }.body()
    }

    suspend fun deleteLocation(id: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/locations/$id")
        Unit
    }

    suspend fun deleteCreature(id: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/creatures/$id")
        Unit
    }

    suspend fun deleteItem(id: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/items/$id")
        Unit
    }

    suspend fun createCreature(request: CreateCreatureRequest): Result<CreatureDto> = apiCall {
        client.post("$baseUrl/creatures") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateCreature(id: String, request: CreateCreatureRequest): Result<Unit> = apiCall {
        client.put("$baseUrl/creatures/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    suspend fun createItem(request: CreateItemRequest): Result<ItemDto> = apiCall {
        client.post("$baseUrl/items") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateItem(id: String, request: CreateItemRequest): Result<Unit> = apiCall {
        client.put("$baseUrl/items/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }

    suspend fun getCreatures(): Result<List<CreatureDto>> = apiCall {
        client.get("$baseUrl/creatures").body()
    }

    suspend fun getCreature(id: String): Result<CreatureDto?> = apiCall {
        val creatures: List<CreatureDto> = client.get("$baseUrl/creatures").body()
        creatures.find { it.id == id }
    }

    /**
     * Get activity states for all creatures (wandering, in_combat, idle).
     */
    suspend fun getCreatureStates(): Result<Map<String, String>> = apiCall {
        client.get("$baseUrl/creatures/states").body()
    }

    suspend fun getItems(): Result<List<ItemDto>> = apiCall {
        client.get("$baseUrl/items").body()
    }

    suspend fun getItem(id: String): Result<ItemDto?> = apiCall {
        val items: List<ItemDto> = client.get("$baseUrl/items").body()
        items.find { it.id == id }
    }

    // User auth methods
    suspend fun register(name: String, password: String): Result<AuthResponse> = apiCall {
        client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(name, password))
        }.body()
    }

    suspend fun login(name: String, password: String): Result<AuthResponse> = apiCall {
        client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(name, password))
        }.body()
    }

    /**
     * Validate current session and get user info.
     * This refreshes the session expiry (sliding window).
     * On web, the cookie is automatically sent and refreshed.
     * On native, the Authorization header is sent and response contains new expiry.
     */
    suspend fun validateSession(): Result<AuthResponse> = apiCall {
        client.get("$baseUrl/auth/me").body()
    }

    /**
     * Logout - invalidate current session.
     */
    suspend fun logout(): Result<Unit> = apiCall {
        client.post("$baseUrl/auth/logout")
        Unit
    }

    /**
     * Logout from all devices - invalidate all sessions for this user.
     */
    suspend fun logoutAll(): Result<Unit> = apiCall {
        client.post("$baseUrl/auth/logout-all")
        Unit
    }

    suspend fun getUser(id: String): Result<UserDto?> = apiCall {
        client.get("$baseUrl/users/$id").body()
    }

    suspend fun updateUser(id: String, request: UpdateUserRequest): Result<UserDto> = apiCall {
        client.put("$baseUrl/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateUserLocation(id: String, locationId: String?): Result<Unit> = apiCall {
        client.put("$baseUrl/users/$id/location") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserLocationRequest(locationId))
        }
        Unit
    }

    suspend fun updateUserClass(id: String, classId: String?): Result<UserDto> = apiCall {
        client.put("$baseUrl/users/$id/class") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserClassRequest(classId))
        }.body()
    }

    suspend fun deriveAttributes(userId: String, description: String, followUpAnswers: Map<String, String> = emptyMap()): Result<DerivedAttributesDto> = apiCall {
        client.post("$baseUrl/users/$userId/derive-attributes") {
            contentType(ContentType.Application.Json)
            setBody(DeriveAttributesRequestDto(description, followUpAnswers))
        }.body()
    }

    suspend fun commitAttributes(userId: String, attributes: DerivedAttributesDto): Result<UserDto> = apiCall {
        client.post("$baseUrl/users/$userId/commit-attributes") {
            contentType(ContentType.Application.Json)
            setBody(CommitAttributesRequestDto(
                strength = attributes.strength,
                dexterity = attributes.dexterity,
                constitution = attributes.constitution,
                intelligence = attributes.intelligence,
                wisdom = attributes.wisdom,
                charisma = attributes.charisma,
                qualityBonus = attributes.qualityBonus
            ))
        }.body()
    }

    suspend fun getActiveUsersAtLocation(locationId: String): Result<List<UserDto>> = apiCall {
        client.get("$baseUrl/users/at-location/$locationId").body()
    }

    // =========================================================================
    // EQUIPMENT
    // =========================================================================

    suspend fun equipItem(userId: String, itemId: String): Result<UserDto> = apiCall {
        client.post("$baseUrl/users/$userId/equip/$itemId").body()
    }

    suspend fun unequipItem(userId: String, itemId: String): Result<UserDto> = apiCall {
        client.post("$baseUrl/users/$userId/unequip/$itemId").body()
    }

    suspend fun pickupItem(userId: String, itemId: String, locationId: String): Result<UserDto> = apiCall {
        client.post("$baseUrl/users/$userId/pickup/$itemId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("locationId" to locationId))
        }.body()
    }

    // =========================================================================
    // ICON MAPPINGS
    // =========================================================================

    suspend fun getIconMappings(userId: String): Result<List<IconMappingDto>> = apiCall {
        client.get("$baseUrl/users/$userId/icon-mappings").body()
    }

    suspend fun setIconMapping(userId: String, abilityId: String, iconName: String): Result<IconMappingDto> = apiCall {
        client.put("$baseUrl/users/$userId/icon-mappings/$abilityId") {
            contentType(ContentType.Application.Json)
            setBody(SetIconMappingRequestDto(iconName = iconName))
        }.body()
    }

    suspend fun deleteIconMapping(userId: String, abilityId: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/users/$userId/icon-mappings/$abilityId")
    }

    // =========================================================================
    // ENCOUNTERS
    // =========================================================================

    suspend fun getEncounters(userId: String): Result<List<PlayerEncounterDto>> = apiCall {
        client.get("$baseUrl/users/$userId/encounters").body()
    }

    suspend fun classifyEncounter(userId: String, encounteredUserId: String, classification: String): Result<Unit> = apiCall {
        client.put("$baseUrl/users/$userId/encounters/$encounteredUserId/classify") {
            contentType(ContentType.Application.Json)
            setBody(ClassifyEncounterRequestDto(classification = classification))
        }
    }

    // =========================================================================
    // TELEPORT
    // =========================================================================

    suspend fun getTeleportDestinations(): Result<List<TeleportDestinationDto>> = apiCall {
        client.get("$baseUrl/teleport/destinations").body()
    }

    suspend fun teleport(userId: String, targetAreaId: String, abilityId: String): Result<TeleportResponseDto> = apiCall {
        client.post("$baseUrl/teleport") {
            contentType(ContentType.Application.Json)
            setBody(TeleportRequestDto(userId = userId, targetAreaId = targetAreaId, abilityId = abilityId))
        }.body()
    }

    // =========================================================================
    // PHASEWALK
    // =========================================================================

    suspend fun getPhasewalkDestinations(userId: String): Result<List<PhasewalkDestinationDto>> = apiCall {
        client.get("$baseUrl/phasewalk/destinations/$userId").body()
    }

    suspend fun phasewalk(userId: String, direction: String): Result<PhasewalkResponseDto> = apiCall {
        client.post("$baseUrl/phasewalk") {
            contentType(ContentType.Application.Json)
            setBody(PhasewalkRequestDto(userId = userId, direction = direction))
        }.body()
    }

    // =========================================================================
    // RIFT PORTAL
    // =========================================================================

    suspend fun getUnconnectedAreas(userId: String): Result<List<UnconnectedAreaDto>> = apiCall {
        client.get("$baseUrl/rift-portal/unconnected-areas") {
            header("X-User-Id", userId)
        }.body()
    }

    suspend fun getSealableRifts(userId: String): Result<List<SealableRiftDto>> = apiCall {
        client.get("$baseUrl/rift-portal/sealable-rifts") {
            header("X-User-Id", userId)
        }.body()
    }

    suspend fun openRift(userId: String, targetAreaId: String): Result<CreateRiftResponseDto> = apiCall {
        client.post("$baseUrl/rift-portal/create") {
            header("X-User-Id", userId)
            contentType(ContentType.Application.Json)
            setBody(CreateRiftRequestDto(targetAreaId = targetAreaId))
        }.body()
    }

    suspend fun sealRift(userId: String, targetAreaId: String): Result<CreateRiftResponseDto> = apiCall {
        client.post("$baseUrl/rift-portal/seal") {
            header("X-User-Id", userId)
            contentType(ContentType.Application.Json)
            setBody(CreateRiftRequestDto(targetAreaId = targetAreaId))
        }.body()
    }

    // =========================================================================
    // SHOP
    // =========================================================================

    suspend fun buyItem(locationId: String, userId: String, itemId: String): Result<ShopActionResponse> = apiCall {
        client.post("$baseUrl/shop/$locationId/buy") {
            contentType(ContentType.Application.Json)
            setBody(BuyItemRequest(userId = userId, itemId = itemId))
        }.body()
    }

    suspend fun restAtInn(locationId: String, userId: String): Result<ShopActionResponse> = apiCall {
        client.post("$baseUrl/shop/$locationId/rest") {
            contentType(ContentType.Application.Json)
            setBody(RestAtInnRequest(userId = userId))
        }.body()
    }

    // Entity identification
    @Serializable
    data class IdentifiedEntitiesResponse(
        val items: List<String> = emptyList(),
        val creatures: List<String> = emptyList()
    )

    @Serializable
    data class IdentifyRequest(val entityId: String, val entityType: String)

    @Serializable
    data class IdentifyResponse(
        val success: Boolean,
        val newlyIdentified: Boolean,
        val entityId: String,
        val entityType: String
    )

    suspend fun getIdentifiedEntities(userId: String): Result<IdentifiedEntitiesResponse> = apiCall {
        client.get("$baseUrl/users/$userId/identified").body()
    }

    suspend fun identifyEntity(userId: String, entityId: String, entityType: String): Result<IdentifyResponse> = apiCall {
        client.post("$baseUrl/users/$userId/identify") {
            contentType(ContentType.Application.Json)
            setBody(IdentifyRequest(entityId, entityType))
        }.body()
    }

    suspend fun assignClass(userId: String, request: AssignClassRequest): Result<AssignClassResponse> = apiCall {
        client.post("$baseUrl/users/$userId/assign-class") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    @Serializable
    private data class ErrorResponse(val error: String)

    // Content generation methods
    suspend fun isContentGenerationAvailable(): Result<Boolean> = apiCall {
        val response: Map<String, Boolean> = client.get("$baseUrl/generate/status").body()
        response["available"] ?: false
    }

    suspend fun generateLocationContent(
        exitIds: List<String> = emptyList(),
        featureIds: List<String> = emptyList(),
        existingName: String? = null,
        existingDesc: String? = null
    ): Result<GeneratedContentResponse> = apiCall {
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
    ): Result<GeneratedContentResponse> = apiCall {
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
    ): Result<GeneratedContentResponse> = apiCall {
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
    suspend fun isImageGenerationAvailable(): Result<Boolean> = apiCall {
        val response: Map<String, Boolean> = client.get("$baseUrl/image-generation/status").body()
        response["available"] ?: false
    }

    suspend fun generateImage(
        entityType: String,
        entityId: String,
        name: String,
        description: String,
        featureIds: List<String> = emptyList()
    ): Result<GenerateImageResponse> = apiCall {
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
    suspend fun getFeatures(): Result<List<FeatureDto>> = apiCall {
        client.get("$baseUrl/features").body()
    }

    suspend fun getFeature(id: String): Result<FeatureDto?> = apiCall {
        val response = client.get("$baseUrl/features/$id")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            null
        }
    }

    // Admin file management methods
    suspend fun getUploadedFiles(): Result<List<UploadedFileDto>> = apiCall {
        client.get("$baseUrl/admin/files").body()
    }

    suspend fun uploadFile(filename: String, fileBytes: ByteArray): Result<FileUploadResponseDto> = apiCall {
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

    suspend fun deleteUploadedFile(filename: String): Result<Boolean> = apiCall {
        val response = client.delete("$baseUrl/admin/files/$filename")
        response.status.isSuccess()
    }

    suspend fun getAllowedFileTypes(): Result<Set<String>> = apiCall {
        val response: Map<String, Set<String>> = client.get("$baseUrl/admin/files/allowed-types").body()
        response["allowedExtensions"] ?: emptySet()
    }

    // Audit log methods
    suspend fun getAuditLogs(limit: Int = 100, offset: Long = 0): Result<List<AuditLogDto>> = apiCall {
        client.get("$baseUrl/audit-logs") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
    }

    suspend fun getAuditLogsByRecord(recordId: String): Result<List<AuditLogDto>> = apiCall {
        client.get("$baseUrl/audit-logs/by-record/$recordId").body()
    }

    suspend fun getAuditLogsByType(recordType: String, limit: Int = 100): Result<List<AuditLogDto>> = apiCall {
        client.get("$baseUrl/audit-logs/by-type/$recordType") {
            parameter("limit", limit)
        }.body()
    }

    suspend fun getAuditLogsByUser(userId: String, limit: Int = 100): Result<List<AuditLogDto>> = apiCall {
        client.get("$baseUrl/audit-logs/by-user/$userId") {
            parameter("limit", limit)
        }.body()
    }

    // Data integrity check
    suspend fun getDataIntegrity(): Result<DataIntegrityResponseDto> = apiCall {
        client.get("$baseUrl/admin/database/data-integrity").body()
    }

    // Admin users
    suspend fun getAdminUsers(): Result<AdminUsersResponseDto> = apiCall {
        client.get("$baseUrl/admin/users").body()
    }

    // Terrain override methods
    suspend fun getTerrainOverrides(locationId: String): Result<TerrainOverrideDto> = apiCall {
        client.get("$baseUrl/locations/$locationId/terrain-overrides").body()
    }

    suspend fun updateTerrainOverrides(locationId: String, overrides: TerrainOverridesDto): Result<TerrainOverrideDto> = apiCall {
        client.put("$baseUrl/locations/$locationId/terrain-overrides") {
            contentType(ContentType.Application.Json)
            setBody(overrides)
        }.body()
    }

    suspend fun resetTerrainOverrides(locationId: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/locations/$locationId/terrain-overrides")
        Unit
    }

    // Exit validation
    suspend fun validateExit(fromLocationId: String, toLocationId: String): Result<ValidateExitResponse> = apiCall {
        client.post("$baseUrl/locations/validate-exit") {
            contentType(ContentType.Application.Json)
            setBody(ValidateExitRequest(fromLocationId, toLocationId))
        }.body()
    }

    // Service health and management
    // Get local service health (Ollama, Stable Diffusion) via backend API
    suspend fun getLocalServicesHealth(): Result<List<ServiceStatusDto>> = apiCall {
        client.get("$baseUrl/admin/services/health/local").body()
    }

    // Get Cloudflare tunnel health via backend API
    suspend fun getCloudflareServicesHealth(): Result<List<ServiceStatusDto>> = apiCall {
        client.get("$baseUrl/admin/services/health/cloudflare").body()
    }

    suspend fun controlService(serviceName: String, action: String): Result<ServiceActionResponse> = apiCall {
        client.post("$baseUrl/admin/services/$serviceName/control") {
            contentType(ContentType.Application.Json)
            setBody(ServiceActionRequest(action))
        }.body()
    }

    // Purge Cloudflare cache
    suspend fun purgeCloudflareCache(): Result<ServiceActionResponse> = apiCall {
        client.post("$baseUrl/admin/services/cloudflare/purge-cache").body()
    }

    // Database backup/restore
    suspend fun createDatabaseBackup(): Result<BackupResponse> = apiCall {
        client.post("$baseUrl/admin/database/backup").body()
    }

    suspend fun listDatabaseBackups(): Result<BackupListResponse> = apiCall {
        client.get("$baseUrl/admin/database/backups").body()
    }

    suspend fun restoreDatabase(filename: String): Result<RestoreResponse> = apiCall {
        client.post("$baseUrl/admin/database/restore/$filename").body()
    }

    suspend fun deleteBackup(filename: String): Result<BackupResponse> = apiCall {
        client.delete("$baseUrl/admin/database/backup/$filename").body()
    }

    // Character Class methods
    suspend fun getCharacterClasses(isAdmin: Boolean = false): Result<List<CharacterClassDto>> = apiCall {
        client.get("$baseUrl/classes") {
            header("X-Is-Admin", isAdmin.toString())
        }.body()
    }

    suspend fun getCharacterClass(id: String): Result<CharacterClassDto?> = apiCall {
        val response = client.get("$baseUrl/classes/$id")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            null
        }
    }

    suspend fun createCharacterClass(request: CreateCharacterClassRequest): Result<CharacterClassDto> = apiCall {
        client.post("$baseUrl/classes") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateCharacterClass(id: String, request: CreateCharacterClassRequest): Result<CharacterClassDto> = apiCall {
        client.put("$baseUrl/classes/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteCharacterClass(id: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/classes/$id")
        Unit
    }

    suspend fun toggleClassLock(classId: String): Result<CharacterClassDto> = apiCall {
        client.put("$baseUrl/classes/$classId/lock") {
            header("X-Is-Admin", "true")
        }.body()
    }

    // Ability methods
    suspend fun getAbilities(): Result<List<AbilityDto>> = apiCall {
        client.get("$baseUrl/abilities").body()
    }

    suspend fun getAbility(id: String): Result<AbilityDto?> = apiCall {
        val response = client.get("$baseUrl/abilities/$id")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getAbilitiesByClass(classId: String): Result<List<AbilityDto>> = apiCall {
        client.get("$baseUrl/abilities/class/$classId").body()
    }

    suspend fun createAbility(request: CreateAbilityRequest): Result<AbilityDto> = apiCall {
        client.post("$baseUrl/abilities") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateAbility(id: String, request: CreateAbilityRequest): Result<AbilityDto> = apiCall {
        client.put("$baseUrl/abilities/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteAbility(id: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/abilities/$id")
        Unit
    }

    // Class Generation and Matching methods
    suspend fun matchCharacterToClass(description: String): Result<ClassMatchResult> = apiCall {
        client.post("$baseUrl/class-generation/match") {
            contentType(ContentType.Application.Json)
            setBody(MatchClassRequest(description))
        }.body()
    }

    suspend fun generateClass(description: String, isPublic: Boolean = false): Result<GeneratedClassResponse> = apiCall {
        client.post("$baseUrl/class-generation/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateClassRequest(description, isPublic))
        }.body()
    }

    suspend fun getLlmStatus(): Result<LlmStatusResponse> = apiCall {
        client.get("$baseUrl/class-generation/status").body()
    }

    // World Generation methods
    suspend fun generateWorld(params: WorldGenParamsDto): Result<WorldGenJobResponseDto> = apiCall {
        val response = client.post("$baseUrl/world/generate") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }
        // Handle non-success responses that still have a body
        if (!response.status.isSuccess()) {
            val errorBody: WorldGenJobResponseDto = response.body()
            return@apiCall errorBody
        }
        response.body()
    }

    suspend fun generateWorldSync(params: WorldGenParamsDto): Result<WorldGenerationResultDto> = apiCall {
        client.post("$baseUrl/world/generate/sync") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()
    }

    suspend fun getWorldGenJobStatus(jobId: String): Result<WorldGenJobStatusDto> = apiCall {
        client.get("$baseUrl/world/generate/$jobId/status").body()
    }

    suspend fun getWorldAreas(): Result<List<AreaInfoDto>> = apiCall {
        client.get("$baseUrl/world/areas").body()
    }

    suspend fun deleteWorldArea(areaId: String): Result<AreaDeleteResponseDto> = apiCall {
        client.delete("$baseUrl/world/area/$areaId").body()
    }

    suspend fun getWorldBiomes(): Result<List<BiomeInfoDto>> = apiCall {
        client.get("$baseUrl/world/biomes").body()
    }

    suspend fun getWorldGenDefaults(): Result<WorldGenParamsDto> = apiCall {
        client.get("$baseUrl/world/params/defaults").body()
    }

    // Nerf Request methods
    suspend fun getNerfRequests(): Result<List<NerfRequestDto>> = apiCall {
        client.get("$baseUrl/nerf-requests").body()
    }

    suspend fun getPendingNerfRequests(): Result<List<NerfRequestDto>> = apiCall {
        client.get("$baseUrl/nerf-requests/pending").body()
    }

    suspend fun getPendingNerfCount(): Result<PendingCountResponse> = apiCall {
        client.get("$baseUrl/nerf-requests/pending/count").body()
    }

    suspend fun getNerfRequest(id: String): Result<NerfRequestDto?> = apiCall {
        val response = client.get("$baseUrl/nerf-requests/$id")
        if (response.status.isSuccess()) response.body() else null
    }

    suspend fun getNerfRequestsForAbility(abilityId: String): Result<List<NerfRequestDto>> = apiCall {
        client.get("$baseUrl/nerf-requests/ability/$abilityId").body()
    }

    suspend fun createNerfRequest(abilityId: String, reason: String): Result<NerfRequestDto> = apiCall {
        client.post("$baseUrl/nerf-requests") {
            contentType(ContentType.Application.Json)
            setBody(CreateNerfRequestRequest(abilityId, reason))
        }.body()
    }

    suspend fun resolveNerfRequest(
        id: String,
        status: String,
        adminNotes: String? = null,
        applyChanges: Boolean = false
    ): Result<NerfRequestDto> = apiCall {
        client.put("$baseUrl/nerf-requests/$id/resolve") {
            contentType(ContentType.Application.Json)
            setBody(ResolveNerfRequestRequest(status, adminNotes, applyChanges))
        }.body()
    }

    suspend fun deleteNerfRequest(id: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/nerf-requests/$id")
        Unit
    }

    // PDF Analysis methods
    suspend fun analyzePdf(filename: String, fileBytes: ByteArray, analysisType: String): Result<PdfAnalysisResponse> = apiCall {
        val response = client.post("$baseUrl/pdf/analyze") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                            append(HttpHeaders.ContentType, "application/pdf")
                        })
                        append("analysisType", analysisType)
                    }
                )
            )
        }
        if (response.status.isSuccess()) {
            response.body()
        } else {
            PdfAnalysisResponse(success = false, error = "Analysis failed with status ${response.status}")
        }
    }
}

@Serializable
data class BackupResponse(
    val success: Boolean,
    val message: String,
    val path: String? = null
)

@Serializable
data class BackupInfo(
    val filename: String,
    val size: Long,
    val modified: Long
)

@Serializable
data class BackupListResponse(
    val success: Boolean,
    val backups: List<BackupInfo> = emptyList(),
    val message: String? = null
)

@Serializable
data class RestoreResponse(
    val success: Boolean,
    val message: String,
    val preRestoreBackup: String? = null
)

@Serializable
data class ValidateExitRequest(
    val fromLocationId: String,
    val toLocationId: String
)

@Serializable
data class ValidDirectionInfo(
    val direction: ExitDirection,
    val isFixed: Boolean,
    val targetCoordinates: CoordinateInfo?
)

@Serializable
data class CoordinateInfo(
    val x: Int,
    val y: Int,
    val areaId: String? = null
)

@Serializable
data class ValidateExitResponse(
    val canCreateExit: Boolean,
    val validDirections: List<ValidDirectionInfo>,
    val errorMessage: String? = null,
    val targetHasCoordinates: Boolean,
    val targetIsConnected: Boolean
)

// ============================================================================
// Combat DTOs
// ============================================================================

enum class CombatantType {
    PLAYER,
    CREATURE
}

enum class CombatState {
    WAITING,
    ACTIVE,
    ENDED
}

enum class CombatEndReason {
    ALL_ENEMIES_DEFEATED,
    ALL_PLAYERS_DEFEATED,
    ALL_PLAYERS_FLED,
    TIMEOUT,
    CANCELLED
}

@Serializable
data class StatusEffectDto(
    val id: String,
    val name: String,
    val effectType: String,
    val value: Int = 0,
    val remainingRounds: Int,
    val sourceId: String
)

@Serializable
data class CombatantDto(
    val id: String,
    val type: CombatantType,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val maxMana: Int = 10,
    val currentMana: Int = 10,
    val maxStamina: Int = 10,
    val currentStamina: Int = 10,
    val characterClassId: String? = null,
    val abilityIds: List<String> = emptyList(),
    val initiative: Int = 0,
    val isDowned: Boolean = false,           // Player is unconscious but not dead
    val deathThreshold: Int = -10,           // HP at which player truly dies
    val isAlive: Boolean = true,
    val statusEffects: List<StatusEffectDto> = emptyList(),
    val cooldowns: Map<String, Int> = emptyMap()
)

@Serializable
data class CombatActionDto(
    val id: String,
    val combatantId: String,
    val abilityId: String,
    val targetId: String? = null,
    val queuedAt: Long
)

@Serializable
data class CombatLogEntryDto(
    val id: String,
    val round: Int,
    val timestamp: Long,
    val actorId: String,
    val actorName: String,
    val targetId: String? = null,
    val targetName: String? = null,
    val abilityName: String? = null,
    val damage: Int = 0,
    val healing: Int = 0,
    val message: String
)

@Serializable
data class CombatSessionDto(
    val id: String,
    val locationId: String,
    val state: CombatState,
    val currentRound: Int,
    val roundStartTime: Long,
    val combatants: List<CombatantDto> = emptyList(),
    val pendingActions: List<CombatActionDto> = emptyList(),
    val combatLog: List<CombatLogEntryDto> = emptyList(),
    val endReason: CombatEndReason? = null,
    val createdAt: Long
)

@Serializable
data class ActionResultDto(
    val actionId: String,
    val success: Boolean,
    val damage: Int = 0,
    val healing: Int = 0,
    val appliedEffects: List<StatusEffectDto> = emptyList(),
    val message: String
)

// ============================================================================
// Combat WebSocket Messages (Client -> Server)
// ============================================================================

@Serializable
data class JoinCombatRequest(
    val type: String = "join",
    val userId: String,
    val targetCreatureIds: List<String> = emptyList()
)

@Serializable
data class UseAbilityRequest(
    val type: String = "ability",
    val userId: String,
    val sessionId: String,
    val abilityId: String,
    val targetId: String? = null
)

@Serializable
data class FleeCombatRequest(
    val type: String = "flee",
    val userId: String,
    val sessionId: String
)

@Serializable
data class LeaveCombatRequest(
    val type: String = "leave",
    val userId: String,
    val sessionId: String
)

// ============================================================================
// Combat WebSocket Messages (Server -> Client)
// ============================================================================

@Serializable
data class CombatStartedResponse(
    val session: CombatSessionDto,
    val yourCombatant: CombatantDto,
    val engagementMessages: List<String> = emptyList()
)

@Serializable
data class RoundStartResponse(
    val sessionId: String,
    val roundNumber: Int,
    val roundDurationMs: Long,
    val combatants: List<CombatantDto>
)

@Serializable
data class HealthUpdateResponse(
    val sessionId: String,
    val combatantId: String,
    val combatantName: String,
    val currentHp: Int,
    val maxHp: Int,
    val changeAmount: Int,
    val sourceId: String? = null,
    val sourceName: String? = null
)

@Serializable
data class ResourceUpdateResponse(
    val sessionId: String,
    val combatantId: String,
    val currentMana: Int,
    val maxMana: Int,
    val currentStamina: Int,
    val maxStamina: Int,
    val manaChange: Int = 0,
    val staminaChange: Int = 0
)

@Serializable
data class AbilityResolvedResponse(
    val sessionId: String,
    val result: ActionResultDto,
    val actorName: String,
    val targetName: String?,
    val abilityName: String
)

@Serializable
data class StatusEffectResponse(
    val sessionId: String,
    val combatantId: String,
    val effect: StatusEffectDto,
    val applied: Boolean
)

@Serializable
data class RoundEndResponse(
    val sessionId: String,
    val roundNumber: Int,
    val combatants: List<CombatantDto>,
    val logEntries: List<CombatLogEntryDto>
)

@Serializable
data class LootResultDto(
    val goldEarned: Int = 0,
    val itemIds: List<String> = emptyList(),
    val itemNames: List<String> = emptyList()
)

@Serializable
data class CombatEndedResponse(
    val sessionId: String,
    val reason: CombatEndReason,
    val victors: List<String>,
    val defeated: List<String>,
    val loot: LootResultDto = LootResultDto(),
    val experienceGained: Int = 0
)

@Serializable
data class CreatureDefeatedResponse(
    val sessionId: String,
    val creatureId: String,
    val creatureName: String,
    val killerPlayerId: String,
    val killerPlayerName: String,
    val experienceGained: Int,
    val loot: LootResultDto = LootResultDto(),
    val remainingEnemies: Int
)

@Serializable
data class FleeResultResponse(
    val sessionId: String,
    val combatantId: String,
    val success: Boolean,
    val message: String
)

@Serializable
data class CombatErrorResponse(
    val sessionId: String? = null,
    val error: String,
    val code: String
)

@Serializable
data class AbilityQueuedResponse(
    val sessionId: String,
    val abilityId: String,
    val targetId: String?
)

@Serializable
data class CreatureMovedResponse(
    val creatureId: String,
    val creatureName: String,
    val fromLocationId: String,
    val toLocationId: String,
    val direction: String? = null  // The direction the creature moved (e.g., "north", "southeast")
)

@Serializable
data class PlayerDownedResponse(
    val sessionId: String,
    val playerId: String,
    val playerName: String,
    val currentHp: Int,
    val deathThreshold: Int,
    val locationId: String
)

@Serializable
data class PlayerStabilizedResponse(
    val sessionId: String,
    val playerId: String,
    val playerName: String,
    val currentHp: Int,
    val healerId: String? = null,
    val healerName: String? = null
)

@Serializable
data class PlayerDraggedResponse(
    val sessionId: String,
    val draggerId: String,
    val draggerName: String,
    val targetId: String,
    val targetName: String,
    val fromLocationId: String,
    val toLocationId: String,
    val toLocationName: String,
    val direction: String
)

@Serializable
data class PlayerDeathResponse(
    val playerId: String,
    val playerName: String,
    val deathLocationId: String? = null,
    val deathLocationName: String? = null,
    val respawnLocationId: String,
    val respawnLocationName: String,
    val itemsDropped: Int,
    val goldLost: Int
)

// ============================================================================
// Shop DTOs
// ============================================================================

@Serializable
data class BuyItemRequest(val userId: String, val itemId: String)

@Serializable
data class RestAtInnRequest(val userId: String)

@Serializable
data class ShopActionResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null
)

// ============================================================================
// Location Mutation DTOs (WebSocket events for real-time location updates)
// ============================================================================

enum class LocationEventType {
    LOCATION_UPDATED,
    EXIT_ADDED,
    EXIT_REMOVED,
    ITEM_ADDED,
    ITEM_REMOVED
}

@Serializable
data class LocationMutationEvent(
    val type: String,                    // "LOCATION_MUTATION"
    val eventType: LocationEventType,
    val locationId: String,
    val areaId: String? = null,
    val gridX: Int? = null,
    val gridY: Int? = null,
    val locationName: String,
    val exitAdded: ExitDto? = null,
    val exitRemoved: ExitDto? = null,
    val itemIdAdded: String? = null,
    val itemIdRemoved: String? = null
)
