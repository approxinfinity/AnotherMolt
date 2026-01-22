# Feature-Based Spell System Design

## Overview

Consolidate all abilities (combat spells, utility spells, passive effects) into the existing Feature system using the `data` JSON field for type-specific configuration. This provides flexibility for new spell types without schema migrations.

## Current State

**Feature model:**
```kotlin
data class Feature(
    val id: String,
    val name: String,
    val featureCategoryId: String?,
    val description: String,
    val data: String = "{}"  // <-- Underutilized
)
```

**Ability model (to be deprecated):**
- 14+ dedicated columns
- Only supports combat context
- Schema changes needed for new ability types

## Proposed Category Structure

```
Feature Categories:
├── Terrain (existing)
│   ├── Forest, Lake, Mountain, etc.
├── Properties (existing)
│   ├── Flammable, Magical, etc.
├── Spells (NEW - parent category)
│   ├── Combat Spells
│   │   ├── Direct Damage
│   │   ├── Healing
│   │   ├── Buffs/Debuffs
│   │   └── Status Effects
│   ├── Utility Spells
│   │   ├── Movement (Phase Walk, Teleport, Levitate)
│   │   ├── Detection (Detect Secret, Sense Evil)
│   │   ├── Stealth (Invisibility, Disguise)
│   │   └── Environmental (Light, Darkness, Weather)
│   └── Passive Abilities
│       ├── Stat Modifiers
│       ├── Resistances
│       └── Special Senses
```

## Data JSON Schemas

### Base Schema (all spells)

```json
{
  "spellType": "combat|utility|passive",
  "cooldown": {
    "type": "none|rounds|seconds|uses_per_day",
    "value": 0
  },
  "cost": {
    "mana": 0,
    "stamina": 0,
    "health": 0
  },
  "requirements": {
    "level": 1,
    "classIds": [],
    "featureIds": []
  }
}
```

### Combat Spell Schema

```json
{
  "spellType": "combat",
  "combat": {
    "target": "self|single_enemy|single_ally|area|all_enemies|all_allies",
    "range": 0,
    "baseDamage": 0,
    "baseHealing": 0,
    "damageType": "physical|fire|ice|lightning|poison|holy|shadow",
    "effects": [
      {
        "type": "dot|hot|buff|debuff|stun|root|slow",
        "stat": "accuracy|evasion|critBonus|damage|armor",
        "modifier": 0,
        "duration": 3
      }
    ]
  },
  "cooldown": { "type": "rounds", "value": 3 },
  "cost": { "mana": 15 }
}
```

**Example - Fireball:**
```json
{
  "spellType": "combat",
  "combat": {
    "target": "area",
    "range": 60,
    "baseDamage": 25,
    "damageType": "fire",
    "effects": [
      { "type": "dot", "damage": 5, "duration": 2 }
    ]
  },
  "cooldown": { "type": "rounds", "value": 5 },
  "cost": { "mana": 30 }
}
```

### Utility Spell Schema

```json
{
  "spellType": "utility",
  "utility": {
    "action": "phase_walk|teleport|levitate|detect_secret|invisibility|light|unlock|...",
    "params": {}
  },
  "cooldown": { "type": "seconds", "value": 300 },
  "cost": { "mana": 20 }
}
```

**Example - Phase Walk:**
```json
{
  "spellType": "utility",
  "utility": {
    "action": "phase_walk",
    "params": {
      "range": 1,
      "ignoresExits": true,
      "ignoresTerrain": ["wall", "door"],
      "duration": 0
    }
  },
  "cooldown": { "type": "uses_per_day", "value": 3 },
  "cost": { "mana": 25 }
}
```

**Example - Teleport:**
```json
{
  "spellType": "utility",
  "utility": {
    "action": "teleport",
    "params": {
      "targetType": "known_location|party_member|home",
      "maxDistance": null,
      "castTime": 10
    }
  },
  "cooldown": { "type": "seconds", "value": 600 },
  "cost": { "mana": 50 }
}
```

**Example - Detect Secret:**
```json
{
  "spellType": "utility",
  "utility": {
    "action": "detect_secret",
    "params": {
      "range": 30,
      "reveals": ["hidden_exit", "trap", "invisible_creature"],
      "duration": 60
    }
  },
  "cooldown": { "type": "seconds", "value": 120 },
  "cost": { "mana": 10 }
}
```

**Example - Invisibility:**
```json
{
  "spellType": "utility",
  "utility": {
    "action": "invisibility",
    "params": {
      "target": "self",
      "duration": 300,
      "breaksOn": ["attack", "cast_spell", "interact"]
    }
  },
  "cooldown": { "type": "seconds", "value": 600 },
  "cost": { "mana": 35 }
}
```

### Passive Ability Schema

```json
{
  "spellType": "passive",
  "passive": {
    "trigger": "always|on_hit|on_crit|below_health|in_terrain|...",
    "triggerParams": {},
    "effects": []
  }
}
```

**Example - Rage (below 25% HP):**
```json
{
  "spellType": "passive",
  "passive": {
    "trigger": "below_health",
    "triggerParams": { "threshold": 0.25 },
    "effects": [
      { "type": "buff", "stat": "damage", "modifier": 50 },
      { "type": "debuff", "stat": "armor", "modifier": -20 }
    ]
  }
}
```

**Example - Forest Affinity:**
```json
{
  "spellType": "passive",
  "passive": {
    "trigger": "in_terrain",
    "triggerParams": { "terrainTypes": ["forest", "jungle"] },
    "effects": [
      { "type": "buff", "stat": "evasion", "modifier": 10 },
      { "type": "buff", "stat": "accuracy", "modifier": 5 }
    ]
  }
}
```

## Kotlin Data Classes

```kotlin
// In shared module for cross-platform use
package com.ez2bg.anotherthread.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class SpellData {
    abstract val cooldown: Cooldown
    abstract val cost: SpellCost
    abstract val requirements: SpellRequirements
}

@Serializable
data class Cooldown(
    val type: CooldownType,
    val value: Int = 0
)

@Serializable
enum class CooldownType {
    none, rounds, seconds, uses_per_day
}

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

// Combat Spells
@Serializable
data class CombatSpellData(
    val combat: CombatConfig,
    override val cooldown: Cooldown,
    override val cost: SpellCost = SpellCost(),
    override val requirements: SpellRequirements = SpellRequirements()
) : SpellData()

@Serializable
data class CombatConfig(
    val target: TargetType,
    val range: Int = 0,
    val baseDamage: Int = 0,
    val baseHealing: Int = 0,
    val damageType: DamageType = DamageType.physical,
    val effects: List<CombatEffect> = emptyList()
)

@Serializable
enum class TargetType {
    self, single_enemy, single_ally, area, all_enemies, all_allies
}

@Serializable
enum class DamageType {
    physical, fire, ice, lightning, poison, holy, shadow
}

@Serializable
data class CombatEffect(
    val type: EffectType,
    val stat: String? = null,
    val modifier: Int = 0,
    val damage: Int = 0,
    val duration: Int = 0
)

@Serializable
enum class EffectType {
    dot, hot, buff, debuff, stun, root, slow
}

// Utility Spells
@Serializable
data class UtilitySpellData(
    val utility: UtilityConfig,
    override val cooldown: Cooldown,
    override val cost: SpellCost = SpellCost(),
    override val requirements: SpellRequirements = SpellRequirements()
) : SpellData()

@Serializable
data class UtilityConfig(
    val action: String,
    val params: Map<String, String> = emptyMap()
)

// Passive Abilities
@Serializable
data class PassiveSpellData(
    val passive: PassiveConfig,
    override val cooldown: Cooldown = Cooldown(CooldownType.none),
    override val cost: SpellCost = SpellCost(),
    override val requirements: SpellRequirements = SpellRequirements()
) : SpellData()

@Serializable
data class PassiveConfig(
    val trigger: String,
    val triggerParams: Map<String, String> = emptyMap(),
    val effects: List<CombatEffect> = emptyList()
)

// Parser utility
object SpellDataParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(data: String): SpellData? {
        return try {
            // Try each type in order
            runCatching { json.decodeFromString<CombatSpellData>(data) }.getOrNull()
                ?: runCatching { json.decodeFromString<UtilitySpellData>(data) }.getOrNull()
                ?: runCatching { json.decodeFromString<PassiveSpellData>(data) }.getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun isCombatSpell(data: String): Boolean = data.contains("\"spellType\":\"combat\"")
    fun isUtilitySpell(data: String): Boolean = data.contains("\"spellType\":\"utility\"")
    fun isPassiveSpell(data: String): Boolean = data.contains("\"spellType\":\"passive\"")
}
```

## Utility Spell Actions (Registry)

The server needs handlers for each utility action:

| Action | Description | Params |
|--------|-------------|--------|
| `phase_walk` | Move ignoring exits | range, ignoresTerrain |
| `teleport` | Jump to known location | targetType, maxDistance, castTime |
| `levitate` | Access UP/DOWN without stairs | duration |
| `detect_secret` | Reveal hidden things | range, reveals, duration |
| `invisibility` | Avoid detection | duration, breaksOn |
| `light` | Illuminate dark areas | radius, duration |
| `unlock` | Open locked doors/chests | difficulty, breaksLock |
| `identify` | Reveal item properties | tier |
| `recall` | Return to home/bind point | castTime |
| `summon` | Call creature ally | creatureType, duration, count |

## Migration Path

### Phase 1: Add Spell Categories ✅ COMPLETE
1. ✅ Created "Spells" parent category
2. ✅ Created subcategories: "Combat Spells", "Utility Spells", "Passive Abilities"
3. ✅ Seeded 8 utility spells (phase_walk, teleport, recall, levitate, detect_secret, invisibility, light, unlock)

### Phase 2: Parallel Operation ✅ COMPLETE
1. ✅ AbilityTable remains functional
2. ✅ SpellData Kotlin classes in server module (moved from shared due to serialization constraints)
3. ✅ SpellService reads from Features and executes utility spells
4. ✅ SpellAbilityAdapter allows CombatService to check both Abilities and Spell-Features
5. ✅ FeatureState table tracks per-user cooldowns and charges

**Implementation notes:**
- SpellData classes are in `server/src/main/kotlin/com/ez2bg/anotherthread/spell/SpellData.kt`
- FeatureState uses composite key `{ownerId}-{featureId}` for efficient lookups
- API endpoints: `/spells/*` for spell operations, `/feature-state/*` for state management

### Phase 3: Migrate Existing Abilities (TODO)
1. Create script to convert each Ability to Feature with `data` JSON
2. Update classId references to use featureIds
3. Update Item.abilityIds to Item.featureIds (or keep separate for clarity)

### Phase 4: Deprecate AbilityTable (TODO)
1. Remove Ability CRUD endpoints
2. Remove AbilityTable from schema
3. Update all references to use Features

## Benefits

1. **Single flexible system** - No separate tables for different ability types
2. **Schema stability** - New spell types don't require migrations
3. **Cross-entity attachment** - Spells can attach to items, locations, creatures
4. **LLM compatibility** - JSON schemas are easy for LLMs to generate
5. **Admin UI reuse** - Feature editor already exists, just needs JSON editor

## Trade-offs

1. **Runtime type checking** - Must validate JSON at runtime vs compile-time columns
2. **Query complexity** - Can't easily filter by specific spell attributes without JSON functions
3. **Migration effort** - Existing abilities need conversion

## Questions to Resolve

1. Should we keep `abilityIds` on Items/Classes separate from `featureIds`, or unify?
2. How do we handle power budget calculations for balance? Move to server-side validator?
3. Should LLM-generated classes produce Feature JSON directly, or use an intermediate format?
4. Do we need a "spell book" concept where users learn spells separate from class?
