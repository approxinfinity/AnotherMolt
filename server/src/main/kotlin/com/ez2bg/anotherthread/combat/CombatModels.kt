package com.ez2bg.anotherthread.combat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Combat system models for MajorMUD-style real-time combat.
 *
 * Combat rounds are approximately 3 seconds long.
 * Players queue abilities during the round, which resolve at round end.
 * Cooldowns are tracked per-combatant per-ability.
 */

// ============================================================================
// Core Combat Enums
// ============================================================================

enum class CombatantType {
    PLAYER,
    CREATURE
}

enum class CombatState {
    WAITING,      // Waiting for more participants
    ACTIVE,       // Combat in progress
    ENDED         // Combat has concluded
}

enum class CombatEndReason {
    ALL_ENEMIES_DEFEATED,
    ALL_PLAYERS_DEFEATED,
    ALL_PLAYERS_FLED,
    TIMEOUT,
    CANCELLED
}

// ============================================================================
// Combatant State
// ============================================================================

/**
 * Represents a participant in combat (player or creature).
 * This is runtime state, not persisted to DB.
 */
@Serializable
data class Combatant(
    val id: String,                           // User ID or Creature ID
    val type: CombatantType,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val maxMana: Int = 10,                    // Maximum mana pool
    val currentMana: Int = 10,                // Current mana (for spells)
    val maxStamina: Int = 10,                 // Maximum stamina pool
    val currentStamina: Int = 10,             // Current stamina (for physical abilities)
    val characterClassId: String? = null,     // For players
    val abilityIds: List<String> = emptyList(),
    val initiative: Int = 0,                  // Determines action order within round
    val isAlive: Boolean = currentHp > 0,
    val statusEffects: List<StatusEffect> = emptyList(),
    val cooldowns: Map<String, Int> = emptyMap(), // abilityId -> rounds remaining
    // Combat stats for RNG mechanics
    val level: Int = 1,                       // Level affects hit chance
    val accuracy: Int = 0,                    // Bonus to hit chance
    val evasion: Int = 0,                     // Bonus to avoid attacks
    val critBonus: Int = 0,                   // Bonus to critical hit chance
    val baseDamage: Int = 5                   // Base damage for auto-attacks
)

/**
 * Status effect applied to a combatant.
 *
 * Effect types:
 * - "buff" / "debuff" - Stat modifiers (+/- value to a stat)
 * - "dot" - Damage over time (value damage per round)
 * - "hot" - Healing over time (value healing per round)
 * - "stun" - Cannot take actions
 * - "root" - Cannot move (flee)
 * - "slow" - Reduced initiative
 * - "shield" - Absorbs damage (value = total absorption remaining)
 * - "reflect" - Reflects percentage of damage back (value = % reflected, e.g. 30 = 30%)
 * - "lifesteal" - Heals attacker for percentage of damage dealt (value = %)
 */
@Serializable
data class StatusEffect(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val effectType: String,
    val value: Int = 0,               // Damage/heal per round, stat modifier, absorption remaining, or percentage
    val remainingRounds: Int,
    val sourceId: String              // Who applied this effect
)

// ============================================================================
// Combat Session
// ============================================================================

/**
 * A combat session tracks all state for an ongoing battle.
 * Sessions are location-bound - all combatants must be at the same location.
 */
@Serializable
data class CombatSession(
    val id: String = UUID.randomUUID().toString(),
    val locationId: String,
    val state: CombatState = CombatState.WAITING,
    val currentRound: Int = 0,
    val roundStartTime: Long = System.currentTimeMillis(),
    val combatants: List<Combatant> = emptyList(),
    val pendingActions: List<CombatAction> = emptyList(),
    val combatLog: List<CombatLogEntry> = emptyList(),
    val endReason: CombatEndReason? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val players: List<Combatant> get() = combatants.filter { it.type == CombatantType.PLAYER }
    val creatures: List<Combatant> get() = combatants.filter { it.type == CombatantType.CREATURE }
    val alivePlayers: List<Combatant> get() = players.filter { it.isAlive }
    val aliveCreatures: List<Combatant> get() = creatures.filter { it.isAlive }
}

// ============================================================================
// Combat Actions
// ============================================================================

/**
 * An action queued by a combatant for the current round.
 */
@Serializable
data class CombatAction(
    val id: String = UUID.randomUUID().toString(),
    val combatantId: String,
    val abilityId: String,
    val targetId: String? = null,     // Null for self/area abilities
    val queuedAt: Long = System.currentTimeMillis()
)

/**
 * Result of executing a combat action.
 */
@Serializable
data class ActionResult(
    val actionId: String,
    val success: Boolean,
    val damage: Int = 0,
    val healing: Int = 0,
    val appliedEffects: List<StatusEffect> = emptyList(),
    val message: String,
    // RNG details for combat log
    val hitResult: String? = null,            // "hit", "miss", "critical", "glancing"
    val wasCritical: Boolean = false,
    val wasGlancing: Boolean = false
)

// ============================================================================
// Combat Log
// ============================================================================

/**
 * A single entry in the combat log for display to players.
 */
@Serializable
data class CombatLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val round: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val actorId: String,
    val actorName: String,
    val targetId: String? = null,
    val targetName: String? = null,
    val abilityName: String? = null,
    val damage: Int = 0,
    val healing: Int = 0,
    val message: String
)

// ============================================================================
// WebSocket Message Types (Client -> Server)
// ============================================================================

/**
 * Base interface for all client messages.
 */
@Serializable
sealed class ClientCombatMessage {
    abstract val userId: String
}

/**
 * Player wants to join/start combat at their current location.
 */
@Serializable
data class JoinCombatMessage(
    override val userId: String,
    val targetCreatureIds: List<String> = emptyList() // Optional: specific creatures to fight
) : ClientCombatMessage()

/**
 * Player queues an ability for the current round.
 */
@Serializable
data class UseAbilityMessage(
    override val userId: String,
    val sessionId: String,
    val abilityId: String,
    val targetId: String? = null
) : ClientCombatMessage()

/**
 * Player attempts to flee combat.
 */
@Serializable
data class FleeCombatMessage(
    override val userId: String,
    val sessionId: String
) : ClientCombatMessage()

/**
 * Player disconnecting/leaving combat (not fleeing).
 */
@Serializable
data class LeaveCombatMessage(
    override val userId: String,
    val sessionId: String
) : ClientCombatMessage()

// ============================================================================
// WebSocket Message Types (Server -> Client)
// ============================================================================

/**
 * Base interface for all server messages.
 */
@Serializable
sealed class ServerCombatMessage

/**
 * Sent when a combat session is created or player joins.
 */
@Serializable
data class CombatStartedMessage(
    val session: CombatSession,
    val yourCombatant: Combatant
) : ServerCombatMessage()

/**
 * Sent at the start of each round.
 */
@Serializable
data class RoundStartMessage(
    val sessionId: String,
    val roundNumber: Int,
    val roundDurationMs: Long,
    val combatants: List<Combatant>
) : ServerCombatMessage()

/**
 * Sent when any combatant's HP changes.
 */
@Serializable
data class HealthUpdateMessage(
    val sessionId: String,
    val combatantId: String,
    val currentHp: Int,
    val maxHp: Int,
    val changeAmount: Int,           // Positive = damage taken, negative = healing
    val sourceId: String? = null,
    val sourceName: String? = null
) : ServerCombatMessage()

/**
 * Sent when a combatant's mana or stamina changes.
 */
@Serializable
data class ResourceUpdateMessage(
    val sessionId: String,
    val combatantId: String,
    val currentMana: Int,
    val maxMana: Int,
    val currentStamina: Int,
    val maxStamina: Int,
    val manaChange: Int = 0,         // Positive = restored, negative = spent
    val staminaChange: Int = 0       // Positive = restored, negative = spent
) : ServerCombatMessage()

/**
 * Sent when an ability resolves.
 */
@Serializable
data class AbilityResolvedMessage(
    val sessionId: String,
    val result: ActionResult,
    val actorName: String,
    val targetName: String?,
    val abilityName: String
) : ServerCombatMessage()

/**
 * Sent when a status effect is applied or removed.
 */
@Serializable
data class StatusEffectMessage(
    val sessionId: String,
    val combatantId: String,
    val effect: StatusEffect,
    val applied: Boolean             // true = applied, false = removed/expired
) : ServerCombatMessage()

/**
 * Sent at the end of each round with full state.
 */
@Serializable
data class RoundEndMessage(
    val sessionId: String,
    val roundNumber: Int,
    val combatants: List<Combatant>,
    val logEntries: List<CombatLogEntry>
) : ServerCombatMessage()

/**
 * Loot result from combat victory.
 */
@Serializable
data class LootResult(
    val goldEarned: Int = 0,
    val itemIds: List<String> = emptyList(),
    val itemNames: List<String> = emptyList()  // For display without lookup
)

/**
 * Sent when combat ends.
 */
@Serializable
data class CombatEndedMessage(
    val sessionId: String,
    val reason: CombatEndReason,
    val victors: List<String>,       // IDs of winning combatants
    val defeated: List<String>,      // IDs of defeated combatants
    val loot: LootResult = LootResult(), // Gold and items dropped
    val experienceGained: Int = 0
) : ServerCombatMessage()

/**
 * Sent when a player successfully flees.
 */
@Serializable
data class FleeResultMessage(
    val sessionId: String,
    val combatantId: String,
    val success: Boolean,
    val message: String
) : ServerCombatMessage()

/**
 * Error message for invalid actions.
 */
@Serializable
data class CombatErrorMessage(
    val sessionId: String? = null,
    val error: String,
    val code: String                 // Error code for client handling
) : ServerCombatMessage()

/**
 * Confirmation that an ability has been queued.
 */
@Serializable
data class AbilityQueuedMessage(
    val sessionId: String,
    val abilityId: String,
    val targetId: String?
) : ServerCombatMessage()

/**
 * Sent when a creature moves between locations (wandering NPCs).
 * Allows clients to update their view without polling.
 */
@Serializable
data class CreatureMovedMessage(
    val creatureId: String,
    val creatureName: String,
    val fromLocationId: String,
    val toLocationId: String
) : ServerCombatMessage()

/**
 * Sent when a player dies and respawns.
 * Notifies the player about what was lost and where they respawned.
 */
@Serializable
data class PlayerDeathMessage(
    val playerId: String,
    val playerName: String,
    val deathLocationId: String?,
    val deathLocationName: String?,
    val respawnLocationId: String,
    val respawnLocationName: String,
    val itemsDropped: Int,
    val goldLost: Int
) : ServerCombatMessage()

// ============================================================================
// Combat Configuration
// ============================================================================

object CombatConfig {
    const val ROUND_DURATION_MS = 3000L           // 3 seconds per round
    const val MAX_ROUND_DURATION_MS = 5000L       // Max wait for slow connections
    const val FLEE_SUCCESS_CHANCE = 0.5           // 50% base flee chance
    const val FLEE_COOLDOWN_ROUNDS = 2            // Can't flee again for 2 rounds after failure
    const val MAX_COMBAT_ROUNDS = 100             // Timeout after 100 rounds (~5 minutes)
    const val SESSION_TIMEOUT_MS = 300000L        // 5 minute session timeout
    const val HP_PER_HIT_DIE = 6                  // Average HP per hit die
    // Resource regeneration (per combat round) - balanced for meaningful resource management
    const val MANA_REGEN_PER_ROUND = 1            // Mana restored per round (was 2 - reduced to increase tactical value)
    const val STAMINA_REGEN_PER_ROUND = 2         // Stamina restored per round (was 3 - maintains martial advantage)
}
