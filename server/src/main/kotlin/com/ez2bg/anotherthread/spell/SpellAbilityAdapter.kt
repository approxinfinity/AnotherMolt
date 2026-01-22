package com.ez2bg.anotherthread.spell

import com.ez2bg.anotherthread.database.*

/**
 * Adapter that provides a unified interface for abilities from both:
 * 1. The legacy Ability table
 * 2. Combat spells stored as Features
 *
 * This allows CombatService to work with both systems during the migration period.
 */
object SpellAbilityAdapter {

    /**
     * Represents an ability that can be used in combat, regardless of source.
     */
    data class CombatAbility(
        val id: String,
        val name: String,
        val description: String,
        val abilityType: String,      // "spell", "combat", "utility", "passive"
        val targetType: String,       // "self", "single_enemy", "single_ally", "area", "all_enemies", "all_allies"
        val range: Int,
        val cooldownType: String,     // "none", "short", "medium", "long"
        val cooldownRounds: Int,
        val baseDamage: Int,
        val baseHealing: Int,
        val effects: String,          // JSON array of effects
        val source: AbilitySource
    )

    enum class AbilitySource {
        ABILITY_TABLE,   // From the legacy Ability table
        SPELL_FEATURE    // From a Feature with combat spell data
    }

    /**
     * Find a combat ability by ID, checking both sources.
     */
    fun findById(id: String): CombatAbility? {
        // First check the Ability table
        AbilityRepository.findById(id)?.let { ability ->
            return ability.toCombatAbility()
        }

        // Then check Features with combat spell data
        FeatureRepository.findById(id)?.let { feature ->
            val parseResult = SpellDataParser.parse(feature.data)
            if (parseResult is SpellParseResult.Combat) {
                return feature.toCombatAbility(parseResult.spell)
            }
        }

        return null
    }

    /**
     * Get all combat abilities available to a user from all sources.
     */
    fun getAvailableCombatAbilities(user: User): List<CombatAbility> {
        val abilities = mutableListOf<CombatAbility>()

        // Get abilities from character class
        user.characterClassId?.let { classId ->
            AbilityRepository.findByClassId(classId).forEach { ability ->
                abilities.add(ability.toCombatAbility())
            }
        }

        // Get abilities from user's features (combat spells)
        user.featureIds.forEach { featureId ->
            FeatureRepository.findById(featureId)?.let { feature ->
                val parseResult = SpellDataParser.parse(feature.data)
                if (parseResult is SpellParseResult.Combat) {
                    abilities.add(feature.toCombatAbility(parseResult.spell))
                }
            }
        }

        // Get abilities from items
        user.itemIds.forEach { itemId ->
            ItemRepository.findById(itemId)?.let { item ->
                // Check item's ability IDs (legacy)
                item.abilityIds.forEach { abilityId ->
                    AbilityRepository.findById(abilityId)?.let { ability ->
                        abilities.add(ability.toCombatAbility())
                    }
                }

                // Check item's feature IDs (new system)
                item.featureIds.forEach { featureId ->
                    FeatureRepository.findById(featureId)?.let { feature ->
                        val parseResult = SpellDataParser.parse(feature.data)
                        if (parseResult is SpellParseResult.Combat) {
                            abilities.add(feature.toCombatAbility(parseResult.spell))
                        }
                    }
                }
            }
        }

        return abilities
    }

    /**
     * Convert an Ability to CombatAbility.
     */
    private fun Ability.toCombatAbility(): CombatAbility = CombatAbility(
        id = id,
        name = name,
        description = description,
        abilityType = abilityType,
        targetType = targetType,
        range = range,
        cooldownType = cooldownType,
        cooldownRounds = cooldownRounds,
        baseDamage = baseDamage,
        baseHealing = 0, // Ability model doesn't have baseHealing, would be in effects
        effects = effects,
        source = AbilitySource.ABILITY_TABLE
    )

    /**
     * Convert a Feature with combat spell data to CombatAbility.
     */
    private fun Feature.toCombatAbility(spell: CombatSpellData): CombatAbility {
        val combat = spell.combat

        // Convert cooldown to legacy format
        val (cooldownType, cooldownRounds) = when (spell.cooldown.type) {
            "none" -> "none" to 0
            "rounds" -> when {
                spell.cooldown.value <= 1 -> "short" to spell.cooldown.value
                spell.cooldown.value <= 3 -> "medium" to spell.cooldown.value
                else -> "long" to spell.cooldown.value
            }
            else -> "medium" to 3
        }

        // Convert effects to JSON string
        val effectsJson = if (combat.effects.isEmpty()) {
            "[]"
        } else {
            combat.effects.joinToString(",", "[", "]") { effect ->
                """{"type":"${effect.type}","stat":"${effect.stat ?: ""}","modifier":${effect.modifier},"damage":${effect.damage},"duration":${effect.duration}}"""
            }
        }

        return CombatAbility(
            id = id,
            name = name,
            description = description,
            abilityType = "spell",
            targetType = combat.target,
            range = combat.range,
            cooldownType = cooldownType,
            cooldownRounds = cooldownRounds,
            baseDamage = combat.baseDamage,
            baseHealing = combat.baseHealing,
            effects = effectsJson,
            source = AbilitySource.SPELL_FEATURE
        )
    }
}
