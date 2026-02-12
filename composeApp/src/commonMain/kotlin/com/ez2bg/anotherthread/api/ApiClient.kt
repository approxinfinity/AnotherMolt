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
    NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST, UP, DOWN, ENTER, UNKNOWN
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
    val gridZ: Int? = 0,  // Z coordinate for vertical stacking (UP/DOWN exits)
    // Area identifier - groups locations into distinct map regions
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
    val shopLayoutDirection: ShopLayoutDirection? = null,
    // Item IDs that the user has discovered via search (shown with * prefix)
    val discoveredItemIds: List<String> = emptyList(),
    // Lock level: null = unlocked, 1-4 = locked with increasing difficulty
    val lockLevel: Int? = null
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
    val isAggressive: Boolean = false,
    val isAlly: Boolean = false,
    // Whether this creature is a trainer (can teach abilities)
    val isTrainer: Boolean = false
)

@Serializable
data class ReactionResultDto(
    val reaction: String,        // "hostile", "uncertain", "friendly"
    val roll: Int,               // 2d6 raw roll
    val charismaModifier: Int,   // CHA modifier applied
    val totalRoll: Int,          // roll + modifier
    val message: String          // Flavor text
)

@Serializable
data class IntelligentWeaponDto(
    val intelligence: Int,
    val ego: Int,
    val alignment: String,
    val communicationType: String,   // "empathy", "speech", "telepathy"
    val primaryPowers: List<String>,
    val extraordinaryAbility: String? = null,
    val personalityName: String? = null,
    val personalityQuirk: String? = null
)

@Serializable
data class EgoContestDto(
    val success: Boolean,
    val playerRoll: Int,
    val playerChaModifier: Int,
    val playerTotal: Int,
    val weaponRoll: Int,
    val weaponEgo: Int,
    val weaponTotal: Int,
    val message: String
)

@Serializable
data class EquipResponseDto(
    val success: Boolean,
    val message: String? = null,
    val user: UserDto? = null,
    val egoContest: EgoContestDto? = null
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
    val value: Int = 0,  // Gold value
    val weight: Int = 1,  // Weight in stone (encumbrance unit)
    val isStackable: Boolean = false  // Whether multiple of this item stack in inventory
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
    val isAggressive: Boolean = false,
    val isAlly: Boolean = false
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
    // Trainer system: abilities the user has learned
    val learnedAbilityIds: List<String> = emptyList(),
    // Action bar customization: which abilities to show (max 10, empty = show all)
    val visibleAbilityIds: List<String> = emptyList(),
    // Stealth status
    val isHidden: Boolean = false,    // Currently hiding in place
    val isSneaking: Boolean = false,  // Moving stealthily
    // Party system: if set, user is following this leader
    val partyLeaderId: String? = null,
    // Party system: true if this user has followers (is a party leader)
    val isPartyLeader: Boolean = false,
    // Generated appearance based on equipment
    val appearanceDescription: String = "",
    // Fishing stats
    val fishCaught: Int = 0,
    // Exploration tracking: locations the user has visited (for minimap fog-of-war)
    val visitedLocationIds: List<String> = emptyList()
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

/**
 * Full stat summary with all MajorMUD-style derived values.
 * Returned by GET /users/{id}/stats
 */
@Serializable
data class StatSummaryDto(
    // Base stats
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,

    // STR-derived
    val meleeDamageBonus: Int,
    val carryCapacity: Int,
    val bashChance: Int,

    // INT-derived
    val manaPoolBonus: Int,
    val spellDamageMultiplier: Int,  // Percentage (100 = normal)
    val charmResistance: Int,        // Percentage

    // WIS-derived
    val manaRegen: Int,
    val spellResistance: Int,        // Percentage
    val mentalResistance: Int,       // Percentage
    val healingEffectiveness: Int,   // Percentage

    // DEX-derived
    val dodgeBonus: Int,
    val attacksPerRound: Int,
    val initiative: Int,
    val sneakChance: Int,            // Percentage

    // CON-derived
    val hpBonus: Int,                // Per level
    val hpRegen: Int,
    val poisonResistance: Int,       // Percentage
    val diseaseResistance: Int,      // Percentage
    val deathThreshold: Int,         // Negative HP at which death occurs
    val staminaBonus: Int,
    val staminaRegen: Int,

    // CHA-derived
    val shopPriceModifier: Int,      // Percentage (lower = better)
    val critBonus: Int,              // Percentage bonus to crit chance
    val maxPartySize: Int,
    val npcModifier: Int,            // -20 to +20

    // Combined
    val critChance: Int,             // Percentage

    // Encumbrance
    val encumbranceTier: String,
    val encumbranceAttackMod: Int,
    val encumbranceDodgeMod: Int,
    val canMove: Boolean
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
data class StealthResultDto(
    val success: Boolean,
    val message: String,
    val isHidden: Boolean = false,
    val isSneaking: Boolean = false,
    val stealthValue: Int = 0
)

@Serializable
data class SearchResultItemDto(
    val id: String,
    val name: String
)

@Serializable
data class RobResultDto(
    val success: Boolean,
    val message: String,
    val goldStolen: Int = 0,
    val caughtByTarget: Boolean = false,
    val itemsStolen: List<String> = emptyList()
)

@Serializable
data class SearchInfoDto(
    val durationMs: Long
)

@Serializable
data class SearchResultDto(
    val success: Boolean,
    val message: String,
    val discoveredItems: List<SearchResultItemDto> = emptyList(),
    val totalHidden: Int = 0,
    val hasMoreHidden: Boolean = false
)

@Serializable
data class HideItemResultDto(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null
)

@Serializable
data class TrackResultDto(
    val success: Boolean,
    val message: String,
    val trails: List<TrailInfoDto> = emptyList()
)

@Serializable
data class TrailInfoDto(
    val entityType: String,
    val entityName: String,
    val directionFrom: String?,
    val directionTo: String?,
    val freshness: String,
    val minutesAgo: Int
)

@Serializable
data class CharmResultDto(
    val success: Boolean,
    val message: String,
    val charmedCreature: CharmedCreatureDto? = null
)

@Serializable
data class CharmedCreatureDto(
    val id: String,
    val creatureId: String,
    val creatureName: String,
    val locationId: String,
    val currentHp: Int,
    val maxHp: Int,
    val charmedAt: Long,
    val expiresAt: Long,
    val charmStrength: Int,
    val minutesRemaining: Int
)

@Serializable
data class FishingInfoDto(
    val canFish: Boolean,
    val reason: String? = null,
    val nearEnabled: Boolean,
    val midEnabled: Boolean,
    val farEnabled: Boolean,
    val midStrRequired: Int,
    val farStrRequired: Int,
    val currentStr: Int,
    val successChance: Int,
    val durationMs: Long,
    val staminaCost: Int,
    val manaCost: Int,
    val waterType: String = "freshwater",  // "freshwater" or "coastal"
    val isCoastal: Boolean = false
)

@Serializable
data class FishingRequestDto(
    val distance: String
)

@Serializable
data class FishingResultDto(
    val success: Boolean,
    val message: String,
    val fishCaught: FishCaughtDto? = null,
    val manaRestored: Int = 0,
    val totalFishCaught: Int = 0,
    val earnedBadge: Boolean = false
)

@Serializable
data class FishCaughtDto(
    val id: String,
    val name: String,
    val weight: Int,
    val value: Int
)

// ===================== FISHING MINIGAME DTOs =====================

@Serializable
data class FishingMinigameStartDto(
    val success: Boolean,
    val message: String? = null,
    val sessionId: String? = null,
    val fishName: String? = null,
    val fishDifficulty: Int = 1,   // 1-10, affects fish movement speed/erraticness
    val catchZoneSize: Int = 25,   // Size of player's catch zone (20-40%)
    val durationMs: Long = 15000,  // Total time for minigame
    val startingScore: Int = 50,   // Score starts at 50
    val fishBehavior: FishBehaviorDto? = null
)

@Serializable
data class FishBehaviorDto(
    val speed: Float,              // Movement speed (0.1-1.0 of bar per second)
    val changeDirectionChance: Float,  // Chance per tick to change direction
    val erraticness: Float,        // Randomness in movement (0.0-1.0)
    val behaviorType: String = "CALM",  // CALM, ERRATIC, DARTING, STUBBORN, WILD
    val dartChance: Float = 0f,    // Chance of sudden speed burst (for DARTING)
    val edgePull: Float = 0f       // Force toward edges (for STUBBORN)
)

@Serializable
data class FishingMinigameCompleteRequestDto(
    val sessionId: String,
    val finalScore: Int  // 0-100, >= 100 means caught
)

@Serializable
data class FishingMinigameCompleteDto(
    val success: Boolean,
    val message: String,
    val caught: Boolean = false,
    val fishCaught: FishCaughtDto? = null,
    val manaRestored: Int = 0,
    val totalFishCaught: Int = 0,
    val earnedBadge: Boolean = false
)

// ===================== LOCKPICKING DTOs =====================

@Serializable
data class LockpickPathPointDto(
    val x: Float,  // 0-1 normalized position
    val y: Float   // 0-1 normalized position
)

@Serializable
data class LockpickInfoDto(
    val success: Boolean,
    val canAttempt: Boolean,
    val reason: String?,
    val difficulty: String?,    // SIMPLE, STANDARD, COMPLEX, MASTER
    val pathPoints: List<LockpickPathPointDto> = emptyList(),
    val tolerance: Float = 0f,
    val shakiness: Float = 0f,
    val successThreshold: Float = 0f,
    val lockLevelName: String? = null
)

@Serializable
data class LockpickAttemptRequestDto(
    val accuracy: Float  // 0-1 player's trace accuracy
)

@Serializable
data class LockpickResultDto(
    val success: Boolean,
    val message: String,
    val accuracy: Float = 0f,
    val lockOpened: Boolean = false
)

// ===================== FACTION/DIPLOMACY DTOs =====================

@Serializable
data class HostilityResultDto(
    val isHostile: Boolean,
    val factionId: String?,
    val factionName: String?,
    val playerStanding: Int,
    val standingLevel: String,
    val canNegotiate: Boolean
)

@Serializable
data class DiplomacyResultDto(
    val success: Boolean,
    val message: String,
    val combatAvoided: Boolean = false,
    val standingChange: Int = 0,
    val goldSpent: Int = 0
)

@Serializable
data class AlertResultDto(
    val success: Boolean,
    val message: String,
    val reinforcements: List<String> = emptyList()
)

// ===================== MANUAL TESTING DTOs =====================

@Serializable
data class ManualTestItemDto(
    val id: String,
    val featureName: String,
    val description: String,
    val category: String,
    val commitHash: String? = null,
    val addedAt: Long,
    val testedAt: Long? = null,
    val testedByUserId: String? = null,
    val testedByUserName: String? = null,
    val notes: String? = null
) {
    val isTested: Boolean get() = testedAt != null
}

@Serializable
data class ManualTestCountsDto(
    val untested: Int,
    val tested: Int
)

@Serializable
data class CreateManualTestItemRequestDto(
    val featureName: String,
    val description: String,
    val category: String,
    val commitHash: String? = null
)

@Serializable
data class MarkTestedRequestDto(
    val userId: String,
    val userName: String,
    val notes: String? = null
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

// === TREASURE MAP DTOs ===

@Serializable
data class TreasureMapRequestDto(val itemId: String)

@Serializable
data class ReadMapResponseDto(
    val success: Boolean,
    val message: String,
    val hint: String? = null,
    val alreadyRead: Boolean = false,
    val roll: Int = 0,
    val modifier: Int = 0,
    val total: Int = 0,
    val difficulty: Int = 0
)

@Serializable
data class ClaimTreasureResponseDto(
    val success: Boolean,
    val message: String,
    val goldAwarded: Int = 0,
    val itemsAwarded: List<String> = emptyList()
)

@Serializable
data class TreasureMapStatusInfoDto(
    val itemId: String,
    val itemName: String,
    val featureId: String,
    val read: Boolean,
    val hint: String? = null,
    val claimed: Boolean,
    val destinationLocationId: String? = null
)

@Serializable
data class TreasureMapStatusResponseDto(
    val maps: List<TreasureMapStatusInfoDto>
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
data class GiveUpResponse(
    val success: Boolean,
    val message: String,
    val respawnLocationId: String? = null,
    val respawnLocationName: String? = null,
    val itemsDropped: Int = 0,
    val goldLost: Int = 0
)

@Serializable
data class GiveItemResponse(
    val success: Boolean,
    val giver: UserDto,
    val receiverId: String,
    val receiverName: String,
    val itemId: String,
    val itemName: String
)

@Serializable
data class ItemReceivedEvent(
    val type: String = "ITEM_RECEIVED",
    val receiverId: String,
    val giverId: String,
    val giverName: String,
    val itemId: String,
    val itemName: String,
    val message: String
)

@Serializable
data class SessionInvalidatedEvent(
    val type: String = "SESSION_INVALIDATED",
    val userId: String,
    val reason: String,
    val message: String
)

@Serializable
data class PartyInviteEvent(
    val type: String = "PARTY_INVITE",
    val inviteeId: String,
    val inviterId: String,
    val inviterName: String,
    val message: String
)

@Serializable
data class PartyAcceptedEvent(
    val type: String = "PARTY_ACCEPTED",
    val leaderId: String,
    val followerId: String,
    val followerName: String,
    val message: String
)

@Serializable
data class PartyFollowMoveEvent(
    val type: String = "PARTY_FOLLOW_MOVE",
    val followerId: String,
    val leaderId: String,
    val leaderName: String,
    val newLocationId: String,
    val newLocationName: String,
    val message: String
)

@Serializable
data class PartyLeftEvent(
    val type: String = "PARTY_LEFT",
    val userId: String,
    val reason: String,
    val message: String
)

@Serializable
data class PartyNewLeaderEvent(
    val type: String = "PARTY_NEW_LEADER",
    val userId: String,
    val newLeaderId: String,
    val newLeaderName: String,
    val message: String
)

@Serializable
data class PlayerDeathEvent(
    val type: String = "PLAYER_DEATH",
    val playerId: String,
    val playerName: String,
    val locationId: String,
    val message: String
)

@Serializable
data class PendingPartyInviteDto(
    val hasPendingInvite: Boolean,
    val inviterId: String? = null,
    val inviterName: String? = null,
    val createdAt: Long? = null
)

@Serializable
data class PartyActionResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null
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
data class UpdateVisibleAbilitiesRequest(
    val abilityIds: List<String>
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
    val staminaCost: Int = 0,
    val minLevel: Int = 1  // Minimum player level to use this ability
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

    init {
        println("[ApiClient] Initialized with baseUrl: $baseUrl")
    }

    /**
     * Wrapper that tracks connection status for API calls.
     */
    private inline fun <T> apiCall(name: String = "unknown", block: () -> T): Result<T> {
        println("[ApiClient] $name: Starting request to $baseUrl (userId=$currentUserId)")
        return runCatching {
            block()
        }.also { result ->
            result.onSuccess {
                println("[ApiClient] $name: SUCCESS")
                ConnectionStateHolder.recordSuccess()
            }.onFailure { error ->
                println("[ApiClient] $name: FAILED - ${error::class.simpleName}: ${error.message}")
                ConnectionStateHolder.recordFailure(error)
            }
        }
    }

    suspend fun getLocations(cacheBuster: Long? = null): Result<List<LocationDto>> = apiCall("getLocations") {
        val url = if (cacheBuster != null) "$baseUrl/locations?_=$cacheBuster" else "$baseUrl/locations"
        println("[ApiClient] getLocations: Fetching from $url")
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

    /**
     * Get a single location by ID with user context.
     * Includes puzzle-revealed secret passages and discovered items (shown with * prefix).
     */
    suspend fun getLocation(id: String): Result<LocationDto?> = apiCall {
        client.get("$baseUrl/locations/$id") {
            // Explicitly add user header for user-specific data (discovered items)
            currentUserId?.let { header("X-User-Id", it) }
        }.body<LocationDto?>()
    }

    /**
     * Get a single location with explicit user context (includes puzzle-revealed secret passages
     * and discovered items).
     */
    suspend fun getLocationWithUserContext(id: String, userId: String): Result<LocationDto> = apiCall {
        client.get("$baseUrl/locations/$id") {
            header("X-User-Id", userId)
        }.body()
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

    suspend fun getCreatureReaction(creatureId: String, userId: String): Result<ReactionResultDto> = apiCall {
        client.get("$baseUrl/creatures/$creatureId/reaction") {
            header("X-User-Id", userId)
        }.body()
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

    suspend fun login(name: String, password: String): Result<AuthResponse> = apiCall("login") {
        println("[ApiClient] login: Attempting login for user '$name' to $baseUrl/auth/login")
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
    suspend fun validateSession(): Result<AuthResponse> = apiCall("validateSession") {
        println("[ApiClient] validateSession: Checking session at $baseUrl/auth/me")
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

    suspend fun getUser(id: String): Result<UserDto?> = apiCall("getUser") {
        println("[ApiClient] getUser: Fetching user $id")
        val user: UserDto? = client.get("$baseUrl/users/$id").body()
        println("[ApiClient] getUser: userId=$id, serverLocationId=${user?.currentLocationId}")
        user
    }

    /**
     * Get full stat summary for a user.
     * Returns all MajorMUD-style derived stats.
     */
    suspend fun getUserStats(id: String): Result<StatSummaryDto> = apiCall {
        client.get("$baseUrl/users/$id/stats").body()
    }

    suspend fun updateUser(id: String, request: UpdateUserRequest): Result<UserDto> = apiCall {
        client.put("$baseUrl/users/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateUserLocation(id: String, locationId: String?): Result<Unit> = apiCall {
        println("[ApiClient] updateUserLocation: userId=$id, locationId=$locationId")
        val response = client.put("$baseUrl/users/$id/location") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserLocationRequest(locationId))
        }
        println("[ApiClient] updateUserLocation response: status=${response.status}")
        if (!response.status.isSuccess()) {
            // Try to parse error message from response body
            val errorBody = try {
                response.bodyAsText()
            } catch (e: Exception) {
                null
            }
            // Try to extract "error" field from JSON response
            val errorMessage = try {
                errorBody?.let { body ->
                    val regex = """"error"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(body)?.groupValues?.get(1)
                }
            } catch (e: Exception) {
                null
            }
            throw Exception(errorMessage ?: "Server returned ${response.status}")
        }
        Unit
    }

    /**
     * Attempt to hide in the current location.
     * Cannot be used during combat.
     */
    suspend fun attemptHide(userId: String): Result<StealthResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/hide").body()
    }

    /**
     * Attempt to start sneaking.
     * Cannot be used during combat.
     */
    suspend fun attemptSneak(userId: String): Result<StealthResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/sneak").body()
    }

    /**
     * Stop hiding/sneaking and become visible.
     */
    suspend fun revealSelf(userId: String): Result<StealthResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/reveal").body()
    }

    /**
     * Attempt to rob another player.
     * Uses DEX-based pickpocket chance. On success steals gold.
     * On failure, target is alerted.
     */
    suspend fun robPlayer(userId: String, targetId: String): Result<RobResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/rob/$targetId").body()
    }

    /**
     * Get search info (duration) before starting a search.
     * Client uses this to show spinner for appropriate duration.
     */
    suspend fun getSearchInfo(userId: String): Result<SearchInfoDto> = apiCall {
        client.get("$baseUrl/users/$userId/search/info").body()
    }

    /**
     * Search the current location for hidden items.
     * Intelligence and thief-type classes have bonuses.
     */
    suspend fun searchLocation(userId: String): Result<SearchResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/search").body()
    }

    /**
     * Hide an item from inventory at the current location.
     * The item will be immediately hidden and require searching to find.
     */
    suspend fun hideItem(userId: String, itemId: String): Result<HideItemResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/hide/$itemId").body()
    }

    /**
     * Track the current location for trails of players and creatures.
     * Wisdom and tracker-type classes have bonuses.
     * Fresher trails are easier to detect.
     */
    suspend fun trackLocation(userId: String): Result<TrackResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/track").body()
    }

    /**
     * Get fishing info (duration, costs, distance requirements) for the current location.
     */
    suspend fun getFishingInfo(userId: String): Result<FishingInfoDto> = apiCall {
        client.get("$baseUrl/users/$userId/fish/info").body()
    }

    /**
     * Attempt to fish at the current location.
     * DEX + INT determine success, STR determines available distances.
     */
    suspend fun fish(userId: String, distance: String): Result<FishingResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/fish") {
            contentType(ContentType.Application.Json)
            setBody(FishingRequestDto(distance = distance))
        }.body()
    }

    /**
     * Start a fishing minigame session.
     * Spends resources upfront and returns minigame parameters.
     */
    suspend fun startFishingMinigame(userId: String, distance: String): Result<FishingMinigameStartDto> = apiCall {
        client.post("$baseUrl/users/$userId/fish/start") {
            contentType(ContentType.Application.Json)
            setBody(FishingRequestDto(distance = distance))
        }.body()
    }

    /**
     * Complete a fishing minigame session.
     * Client sends the final score; if >= 100, fish is caught.
     */
    suspend fun completeFishingMinigame(userId: String, sessionId: String, finalScore: Int): Result<FishingMinigameCompleteDto> = apiCall {
        client.post("$baseUrl/users/$userId/fish/complete") {
            contentType(ContentType.Application.Json)
            setBody(FishingMinigameCompleteRequestDto(sessionId = sessionId, finalScore = finalScore))
        }.body()
    }

    // ===================== LOCKPICKING API =====================

    /**
     * Get lockpicking info for a locked location.
     * Returns path points and difficulty settings for the minigame.
     */
    suspend fun getLockpickInfo(userId: String, locationId: String): Result<LockpickInfoDto> = apiCall {
        client.get("$baseUrl/users/$userId/lockpick/$locationId/info").body()
    }

    /**
     * Attempt to pick a lock.
     * Client sends the player's trace accuracy (0-1).
     */
    suspend fun attemptLockpick(userId: String, locationId: String, accuracy: Float): Result<LockpickResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/lockpick/$locationId") {
            contentType(ContentType.Application.Json)
            setBody(LockpickAttemptRequestDto(accuracy = accuracy))
        }.body()
    }

    /**
     * Attempt to charm a creature at the current location.
     * Charisma and bard-type classes have bonuses.
     * Cannot be used in combat. Some creatures are immune.
     */
    suspend fun charmCreature(userId: String, creatureId: String): Result<CharmResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/charm/$creatureId").body()
    }

    /**
     * Get the player's currently charmed creature, if any.
     */
    suspend fun getCharmedCreature(userId: String): Result<CharmResultDto> = apiCall {
        client.get("$baseUrl/users/$userId/charmed-creature").body()
    }

    /**
     * Release the currently charmed creature.
     */
    suspend fun releaseCharmedCreature(userId: String): Result<CharmResultDto> = apiCall {
        client.post("$baseUrl/users/$userId/release-charm").body()
    }

    // ===================== FACTION/DIPLOMACY API =====================

    /**
     * Check if a creature is hostile to the player based on faction standing.
     */
    suspend fun checkHostility(userId: String, creatureId: String): Result<HostilityResultDto> = apiCall {
        client.get("$baseUrl/factions/hostility/$creatureId") {
            header("X-User-Id", userId)
        }.body()
    }

    /**
     * Check if diplomacy is possible with a creature.
     * Returns whether bribe/parley options should be shown.
     */
    suspend fun checkDiplomacy(userId: String, creatureId: String): Result<DiplomacyResultDto> = apiCall {
        client.get("$baseUrl/factions/diplomacy/$creatureId/check") {
            header("X-User-Id", userId)
        }.body()
    }

    /**
     * Attempt to bribe a creature to avoid combat.
     * Costs gold based on faction hostility.
     */
    suspend fun attemptBribe(userId: String, creatureId: String): Result<DiplomacyResultDto> = apiCall {
        client.post("$baseUrl/factions/diplomacy/$creatureId/bribe") {
            header("X-User-Id", userId)
        }.body()
    }

    /**
     * Attempt to parley with a creature to avoid combat.
     * Uses WIS-based skill check.
     */
    suspend fun attemptParley(userId: String, creatureId: String): Result<DiplomacyResultDto> = apiCall {
        client.post("$baseUrl/factions/diplomacy/$creatureId/parley") {
            header("X-User-Id", userId)
        }.body()
    }

    suspend fun updateUserClass(id: String, classId: String?): Result<UserDto> = apiCall {
        client.put("$baseUrl/users/$id/class") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserClassRequest(classId))
        }.body()
    }

    suspend fun updateVisibleAbilities(userId: String, abilityIds: List<String>): Result<UserDto> = apiCall {
        client.put("$baseUrl/users/$userId/visible-abilities") {
            contentType(ContentType.Application.Json)
            setBody(UpdateVisibleAbilitiesRequest(abilityIds))
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

    suspend fun equipItem(userId: String, itemId: String): Result<EquipResponseDto> = apiCall {
        client.post("$baseUrl/users/$userId/equip/$itemId").body()
    }

    suspend fun unequipItem(userId: String, itemId: String): Result<UserDto> = apiCall {
        client.post("$baseUrl/users/$userId/unequip/$itemId").body()
    }

    suspend fun getIntelligentWeaponData(itemId: String): Result<IntelligentWeaponDto> = apiCall {
        client.get("$baseUrl/items/$itemId/intelligent").body()
    }

    // === Treasure Map API ===

    suspend fun readTreasureMap(userId: String, itemId: String): Result<ReadMapResponseDto> = apiCall {
        client.post("$baseUrl/treasure-maps/$userId/read") {
            contentType(ContentType.Application.Json)
            setBody(TreasureMapRequestDto(itemId = itemId))
        }.body()
    }

    suspend fun claimTreasure(userId: String, itemId: String): Result<ClaimTreasureResponseDto> = apiCall {
        client.post("$baseUrl/treasure-maps/$userId/claim") {
            contentType(ContentType.Application.Json)
            setBody(TreasureMapRequestDto(itemId = itemId))
        }.body()
    }

    suspend fun getTreasureMapStatus(userId: String): Result<TreasureMapStatusResponseDto> = apiCall {
        client.get("$baseUrl/treasure-maps/$userId/status").body()
    }

    suspend fun pickupItem(userId: String, itemId: String, locationId: String): Result<UserDto> = apiCall {
        val response = client.post("$baseUrl/users/$userId/pickup/$itemId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("locationId" to locationId))
        }
        if (response.status.isSuccess()) {
            response.body()
        } else {
            // Parse error response and throw with the error message
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["error"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Drop an item from inventory at current location.
     * Returns the updated user.
     */
    suspend fun dropItem(userId: String, itemId: String): Result<UserDto> = apiCall {
        val response = client.post("$baseUrl/users/$userId/drop/$itemId")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["error"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Drop ALL of a specific item type (for item stacks).
     * Returns updated user with reduced inventory.
     */
    suspend fun dropAllItems(userId: String, itemId: String): Result<UserDto> = apiCall {
        val response = client.post("$baseUrl/users/$userId/drop-all/$itemId")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["error"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Give an item to another player at the same location.
     * Returns updated giver user and item info.
     */
    suspend fun giveItem(giverId: String, receiverId: String, itemId: String): Result<GiveItemResponse> = apiCall {
        val response = client.post("$baseUrl/users/$giverId/give/$receiverId/$itemId")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["error"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Voluntary death - player gives up while downed (HP <= 0).
     * Respawns at Tun du Lac with full HP, optionally dropping items/gold based on server config.
     */
    suspend fun giveUp(userId: String): Result<GiveUpResponse> = apiCall {
        val response = client.post("$baseUrl/users/$userId/give-up")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["message"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    // =========================================================================
    // PARTY SYSTEM
    // =========================================================================

    /**
     * Invite another player to your party.
     * Both players must be at the same location.
     */
    suspend fun inviteToParty(userId: String, targetId: String): Result<PartyActionResponse> = apiCall {
        val response = client.post("$baseUrl/users/$userId/party/invite/$targetId")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["error"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Accept a pending party invite.
     * Makes the inviter the party leader.
     */
    suspend fun acceptPartyInvite(userId: String, inviterId: String): Result<PartyActionResponse> = apiCall {
        val response = client.post("$baseUrl/users/$userId/party/accept/$inviterId")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["error"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Leave the current party.
     */
    suspend fun leaveParty(userId: String): Result<PartyActionResponse> = apiCall {
        val response = client.post("$baseUrl/users/$userId/party/leave")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["error"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Disband the party (leader only).
     */
    suspend fun disbandParty(userId: String): Result<PartyActionResponse> = apiCall {
        val response = client.post("$baseUrl/users/$userId/party/disband")
        if (response.status.isSuccess()) {
            response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorMessage = try {
                Json.decodeFromString<Map<String, String>>(errorBody)["error"] ?: errorBody
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Get pending party invite for a user.
     */
    suspend fun getPendingPartyInvite(userId: String): Result<PendingPartyInviteDto> = apiCall {
        client.get("$baseUrl/users/$userId/party/pending-invite").body()
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

    suspend fun getSellableItems(locationId: String, userId: String): Result<SellableItemsResponse> = apiCall {
        client.get("$baseUrl/shop/$locationId/sellable/$userId").body()
    }

    suspend fun getShopBanStatus(locationId: String, userId: String): Result<ShopBanResponse> = apiCall {
        client.get("$baseUrl/shop/$locationId/ban-status/$userId").body()
    }

    suspend fun sellItem(locationId: String, userId: String, itemId: String): Result<ShopActionResponse> = apiCall {
        client.post("$baseUrl/shop/$locationId/sell") {
            contentType(ContentType.Application.Json)
            setBody(SellItemRequest(userId = userId, itemId = itemId))
        }.body()
    }

    suspend fun sellFoodItem(locationId: String, userId: String, foodItemId: String): Result<ShopActionResponse> = apiCall {
        client.post("$baseUrl/shop/$locationId/sell-food") {
            contentType(ContentType.Application.Json)
            setBody(SellFoodItemRequest(userId = userId, foodItemId = foodItemId))
        }.body()
    }

    // =========================================================================
    // Trainer API
    // =========================================================================

    suspend fun getTrainerInfo(creatureId: String, userId: String?): Result<TrainerInfoResponse> = apiCall {
        client.get("$baseUrl/trainer/$creatureId") {
            if (userId != null) {
                header("X-User-Id", userId)
            }
        }.body()
    }

    suspend fun learnAbility(creatureId: String, userId: String, abilityId: String): Result<TrainerActionResponse> = apiCall {
        client.post("$baseUrl/trainer/$creatureId/learn") {
            contentType(ContentType.Application.Json)
            setBody(LearnAbilityRequest(userId = userId, abilityId = abilityId))
        }.body()
    }

    suspend fun getUsableAbilities(userId: String): Result<List<AbilityDto>> = apiCall {
        client.get("$baseUrl/trainer/usable/$userId").body()
    }

    suspend fun getLearnedAbilities(userId: String): Result<List<AbilityDto>> = apiCall {
        client.get("$baseUrl/trainer/learned/$userId").body()
    }

    // =========================================================================
    // Puzzle API
    // =========================================================================

    suspend fun getPuzzlesAtLocation(locationId: String): Result<List<PuzzleDto>> = apiCall {
        client.get("$baseUrl/puzzles/at-location/$locationId").body()
    }

    suspend fun getPuzzleProgress(puzzleId: String, userId: String): Result<PuzzleProgressResponse> = apiCall {
        client.get("$baseUrl/puzzles/$puzzleId/progress") {
            header("X-User-Id", userId)
        }.body()
    }

    suspend fun pullLever(puzzleId: String, leverId: String, userId: String): Result<PullLeverResponse> = apiCall {
        client.post("$baseUrl/puzzles/$puzzleId/pull-lever") {
            contentType(ContentType.Application.Json)
            header("X-User-Id", userId)
            setBody(PullLeverRequest(leverId = leverId))
        }.body()
    }

    suspend fun searchPuzzle(puzzleId: String, userId: String): Result<Map<String, Any>> = apiCall {
        client.post("$baseUrl/puzzles/$puzzleId/search") {
            header("X-User-Id", userId)
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

    // ===================== MANUAL TESTING API =====================

    /**
     * Get counts of untested and tested items.
     */
    suspend fun getManualTestCounts(): Result<ManualTestCountsDto> = apiCall {
        client.get("$baseUrl/manual-tests/counts").body()
    }

    /**
     * Get all manual test items.
     */
    suspend fun getManualTestItems(): Result<List<ManualTestItemDto>> = apiCall {
        client.get("$baseUrl/manual-tests").body()
    }

    /**
     * Get only untested items.
     */
    suspend fun getUntestedItems(): Result<List<ManualTestItemDto>> = apiCall {
        client.get("$baseUrl/manual-tests/untested").body()
    }

    /**
     * Get only tested items.
     */
    suspend fun getTestedItems(): Result<List<ManualTestItemDto>> = apiCall {
        client.get("$baseUrl/manual-tests/tested").body()
    }

    /**
     * Get all unique categories.
     */
    suspend fun getManualTestCategories(): Result<List<String>> = apiCall {
        client.get("$baseUrl/manual-tests/categories").body()
    }

    /**
     * Create a new manual test item.
     */
    suspend fun createManualTestItem(request: CreateManualTestItemRequestDto): Result<ManualTestItemDto> = apiCall {
        client.post("$baseUrl/manual-tests") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Mark a test item as tested.
     */
    suspend fun markTestItemTested(id: String, userId: String, userName: String, notes: String? = null): Result<ManualTestItemDto> = apiCall {
        client.post("$baseUrl/manual-tests/$id/mark-tested") {
            contentType(ContentType.Application.Json)
            setBody(MarkTestedRequestDto(userId, userName, notes))
        }.body()
    }

    /**
     * Unmark a test item (move back to untested).
     */
    suspend fun unmarkTestItemTested(id: String): Result<ManualTestItemDto> = apiCall {
        client.post("$baseUrl/manual-tests/$id/unmark-tested").body()
    }

    /**
     * Delete a manual test item.
     */
    suspend fun deleteManualTestItem(id: String): Result<Unit> = apiCall {
        client.delete("$baseUrl/manual-tests/$id")
        Unit
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
    PLAYER_LEFT,    // Player left the location (disengaged from combat)
    TIMEOUT,
    CANCELLED,
    SERVER_RESTART  // Session orphaned due to server restart
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
    val cooldowns: Map<String, Int> = emptyMap(),
    val dexterity: Int = 10,                 // DEX stat for extra attacks calculation
    val attacksPerRound: Int = 1,            // Number of attacks per combat round (MajorMUD-style)
    val partyLeaderId: String? = null        // Party leader ID for party-aware abilities
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
    val remainingEnemies: Int,
    val leveledUp: Boolean = false,
    val newLevel: Int? = null,
    val unlockedAbilities: List<String> = emptyList()
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
data class SellItemRequest(val userId: String, val itemId: String)

@Serializable
data class SellFoodItemRequest(val userId: String, val foodItemId: String)

@Serializable
data class RestAtInnRequest(val userId: String)

@Serializable
data class ShopActionResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null
)

@Serializable
data class SellableItemDto(
    val id: String,
    val itemId: String,
    val name: String,
    val sellValue: Int,
    val isFoodItem: Boolean = false,
    val foodState: String? = null,
    val timeUntilSpoil: String? = null
)

@Serializable
data class SellableItemsResponse(
    val success: Boolean,
    val items: List<SellableItemDto>
)

@Serializable
data class ShopBanResponse(
    val isBanned: Boolean,
    val message: String? = null,
    val banExpiresAt: Long? = null
)

// ============================================================================
// Trainer DTOs
// ============================================================================

@Serializable
data class LearnAbilityRequest(val userId: String, val abilityId: String)

@Serializable
data class TrainerActionResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null
)

@Serializable
data class TrainerAbilityInfo(
    val ability: AbilityDto,
    val goldCost: Int,
    val alreadyLearned: Boolean,
    val meetsLevelRequirement: Boolean
)

@Serializable
data class TrainerInfoResponse(
    val trainerId: String,
    val trainerName: String,
    val abilities: List<TrainerAbilityInfo>
)

// ============================================================================
// Puzzle DTOs (Lever puzzles and secret passages)
// ============================================================================

@Serializable
enum class PuzzleType {
    LEVER_SEQUENCE,      // Pull levers in specific order
    LEVER_COMBINATION,   // Pull specific levers (any order)
    SEARCH_AND_ENTER,    // Search to find hidden passage
    BUTTON_PRESS         // Press buttons in sequence
}

@Serializable
data class LeverDto(
    val id: String,
    val name: String,
    val description: String,
    val pulledDescription: String = "The lever has been pulled."
)

@Serializable
data class SecretPassageDto(
    val id: String,
    val name: String,
    val description: String,
    val targetLocationId: String,
    val direction: ExitDirection = ExitDirection.ENTER,
    val searchHint: String? = null
)

@Serializable
data class PuzzleDto(
    val id: String,
    val name: String,
    val description: String,
    val locationId: String,
    val puzzleType: PuzzleType = PuzzleType.LEVER_COMBINATION,
    val levers: List<LeverDto> = emptyList(),
    val requiredSequence: List<String> = emptyList(),
    val requiredLevers: List<String> = emptyList(),
    val secretPassages: List<SecretPassageDto> = emptyList(),
    val solvedMessage: String = "You hear a click as something unlocks!",
    val failureMessage: String = "Nothing happens.",
    val resetOnFailure: Boolean = false,
    val isRepeatable: Boolean = false,
    val goldReward: Int = 0,
    val itemRewards: List<String> = emptyList()
)

@Serializable
data class PuzzleProgressDto(
    val leversPulled: List<String> = emptyList(),
    val solved: Boolean = false,
    val solvedAt: Long? = null,
    val passagesRevealed: List<String> = emptyList()
)

@Serializable
data class LeverStateDto(
    val id: String,
    val name: String,
    val description: String,
    val isPulled: Boolean
)

@Serializable
data class PuzzleProgressResponse(
    val puzzleId: String,
    val progress: PuzzleProgressDto,
    val levers: List<LeverStateDto>,
    val isSolved: Boolean,
    val revealedPassages: List<SecretPassageDto>
)

@Serializable
data class PullLeverRequest(val leverId: String)

@Serializable
data class PullLeverResponse(
    val success: Boolean,
    val message: String,
    val leverState: LeverStateDto,
    val puzzleSolved: Boolean,
    val puzzleReset: Boolean = false,
    val revealedPassages: List<SecretPassageDto>? = null
)

// ============================================================================
// Location Mutation DTOs (WebSocket events for real-time location updates)
// ============================================================================

enum class LocationEventType {
    LOCATION_UPDATED,
    EXIT_ADDED,
    EXIT_REMOVED,
    ITEM_ADDED,
    ITEM_REMOVED,
    CREATURE_REMOVED,
    CREATURE_ADDED,
    PLAYER_ENTERED,
    PLAYER_LEFT
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
    val itemIdRemoved: String? = null,
    val creatureIdRemoved: String? = null,
    val creatureIdAdded: String? = null,
    val creatureName: String? = null,
    val playerId: String? = null,        // Player who entered/left
    val playerName: String? = null       // Player name for display
)
