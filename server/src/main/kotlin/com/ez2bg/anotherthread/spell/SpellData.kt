package com.ez2bg.anotherthread.spell

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Spell data models for Feature-based spell system.
 * These classes represent the JSON structure stored in Feature.data field.
 */

// ============================================================================
// Common Types
// ============================================================================

@Serializable
data class Cooldown(
    val type: String,  // "none", "rounds", "seconds", "uses_per_day"
    val value: Int = 0
)

@Serializable
data class SpellCost(
    val mana: Int = 0,
    val stamina: Int = 0,
    val health: Int = 0
)

@Serializable
data class SpellRequirements(
    val level: Int = 1,
    val classIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList()
)

// ============================================================================
// Combat Spell Types
// ============================================================================

@Serializable
data class CombatSpellData(
    val spellType: String = "combat",
    val combat: CombatConfig,
    val cooldown: Cooldown,
    val cost: SpellCost = SpellCost(),
    val requirements: SpellRequirements = SpellRequirements()
)

@Serializable
data class CombatConfig(
    val target: String,      // "self", "single_enemy", "single_ally", "area", "all_enemies", "all_allies"
    val range: Int = 0,
    val baseDamage: Int = 0,
    val baseHealing: Int = 0,
    val damageType: String = "physical",  // "physical", "fire", "ice", "lightning", "poison", "holy", "shadow"
    val effects: List<CombatEffect> = emptyList()
)

@Serializable
data class CombatEffect(
    val type: String,        // "dot", "hot", "buff", "debuff", "stun", "root", "slow"
    val stat: String? = null,
    val modifier: Int = 0,
    val damage: Int = 0,
    val duration: Int = 0
)

// ============================================================================
// Utility Spell Types
// ============================================================================

@Serializable
data class UtilitySpellData(
    val spellType: String = "utility",
    val utility: UtilityConfig,
    val cooldown: Cooldown,
    val cost: SpellCost = SpellCost(),
    val requirements: SpellRequirements = SpellRequirements()
)

@Serializable
data class UtilityConfig(
    val action: String,      // "phase_walk", "teleport", "levitate", "detect_secret", "invisibility", etc.
    val params: UtilityParams = UtilityParams()
)

@Serializable
data class UtilityParams(
    // Phase Walk params
    val range: Int? = null,
    val ignoresExits: Boolean? = null,
    val ignoresTerrain: List<String>? = null,

    // Teleport params
    val targetType: String? = null,  // "known_location", "party_member", "bind_point"
    val maxDistance: Int? = null,
    val castTime: Int? = null,
    val familiarityBonus: Boolean? = null,

    // Levitate params
    val verticalAccess: Boolean? = null,
    val maxAltitude: Int? = null,

    // Detect params
    val reveals: List<String>? = null,  // "hidden_exit", "trap", "invisible_creature", "illusion"

    // Invisibility params
    val target: String? = null,
    val breaksOn: List<String>? = null,  // "attack", "cast_spell", "interact_hostile"

    // Light params
    val radius: Int? = null,
    val follows: Boolean? = null,
    val brightness: String? = null,

    // Unlock params
    val maxDifficulty: String? = null,
    val breaksLock: Boolean? = null,

    // Common
    val duration: Int? = null,
    val interruptedByCombat: Boolean? = null
)

// ============================================================================
// Passive Ability Types
// ============================================================================

@Serializable
data class PassiveSpellData(
    val spellType: String = "passive",
    val passive: PassiveConfig,
    val cooldown: Cooldown = Cooldown(type = "none"),
    val cost: SpellCost = SpellCost(),
    val requirements: SpellRequirements = SpellRequirements()
)

@Serializable
data class PassiveConfig(
    val trigger: String,     // "always", "on_hit", "on_crit", "below_health", "in_terrain", "on_kill"
    val triggerParams: TriggerParams = TriggerParams(),
    val effects: List<CombatEffect> = emptyList()
)

@Serializable
data class TriggerParams(
    val threshold: Float? = null,      // For "below_health" trigger (0.0 - 1.0)
    val terrainTypes: List<String>? = null,  // For "in_terrain" trigger
    val chance: Float? = null          // Probability (0.0 - 1.0)
)

// ============================================================================
// Parser Utility
// ============================================================================

object SpellDataParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parse spell data JSON and return the appropriate type.
     * Returns null if the JSON is not a valid spell or is empty/malformed.
     */
    fun parseSpellType(data: String): String? {
        if (data.isBlank() || data == "{}") return null

        return when {
            data.contains("\"spellType\":\"combat\"") ||
            data.contains("\"spellType\": \"combat\"") -> "combat"

            data.contains("\"spellType\":\"utility\"") ||
            data.contains("\"spellType\": \"utility\"") -> "utility"

            data.contains("\"spellType\":\"passive\"") ||
            data.contains("\"spellType\": \"passive\"") -> "passive"

            else -> null
        }
    }

    fun parseCombatSpell(data: String): CombatSpellData? {
        return try {
            json.decodeFromString<CombatSpellData>(data)
        } catch (e: Exception) {
            null
        }
    }

    fun parseUtilitySpell(data: String): UtilitySpellData? {
        return try {
            json.decodeFromString<UtilitySpellData>(data)
        } catch (e: Exception) {
            null
        }
    }

    fun parsePassiveSpell(data: String): PassiveSpellData? {
        return try {
            json.decodeFromString<PassiveSpellData>(data)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse any spell type and return a sealed result.
     */
    fun parse(data: String): SpellParseResult {
        val spellType = parseSpellType(data) ?: return SpellParseResult.NotASpell

        return when (spellType) {
            "combat" -> {
                val spell = parseCombatSpell(data)
                if (spell != null) SpellParseResult.Combat(spell)
                else SpellParseResult.ParseError("Failed to parse combat spell")
            }
            "utility" -> {
                val spell = parseUtilitySpell(data)
                if (spell != null) SpellParseResult.Utility(spell)
                else SpellParseResult.ParseError("Failed to parse utility spell")
            }
            "passive" -> {
                val spell = parsePassiveSpell(data)
                if (spell != null) SpellParseResult.Passive(spell)
                else SpellParseResult.ParseError("Failed to parse passive spell")
            }
            else -> SpellParseResult.NotASpell
        }
    }

    /**
     * Check if a Feature's data represents a spell.
     */
    fun isSpell(data: String): Boolean = parseSpellType(data) != null
}

sealed class SpellParseResult {
    data class Combat(val spell: CombatSpellData) : SpellParseResult()
    data class Utility(val spell: UtilitySpellData) : SpellParseResult()
    data class Passive(val spell: PassiveSpellData) : SpellParseResult()
    data class ParseError(val message: String) : SpellParseResult()
    data object NotASpell : SpellParseResult()
}
