package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlin.math.roundToInt

/**
 * MajorMUD-style stat modifier system.
 *
 * This service centralizes all stat-based calculations following MajorMUD's design philosophy:
 * - Stats have breakpoints where bonuses jump rather than scale linearly
 * - Multiple stats often combine for complex calculations
 * - Equipment and buffs can temporarily modify effective stats
 *
 * STAT OVERVIEW:
 * - Strength (STR): Melee damage, carry capacity, bash/break
 * - Intelligence (INT): Mana pool, spell damage, spell learning, identification
 * - Wisdom (WIS): Mana regen, spell resistance, spell effectiveness
 * - Dexterity (DEX): Dodge, attacks per round, initiative, stealth
 * - Constitution (CON): HP pool, HP regen, poison resist, death threshold
 * - Charisma (CHA): Shop prices, critical hits, leadership, persuasion
 */
object StatModifierService {

    // ============================================================================
    // CORE MODIFIER CALCULATION
    // ============================================================================

    /**
     * Standard D&D-style attribute modifier: (stat - 10) / 2
     * This gives: 10-11=+0, 12-13=+1, 14-15=+2, 16-17=+3, 18-19=+4, 20+=+5
     */
    fun attributeModifier(stat: Int): Int = (stat - 10) / 2

    /**
     * MajorMUD-style breakpoint bonus.
     * Stats have thresholds where bonuses jump significantly.
     * Returns additional bonus on top of the linear modifier.
     */
    fun breakpointBonus(stat: Int): Int = when {
        stat >= 25 -> 5  // Legendary
        stat >= 22 -> 4  // Exceptional
        stat >= 19 -> 3  // Superior
        stat >= 16 -> 2  // Good
        stat >= 13 -> 1  // Above average
        else -> 0
    }

    // ============================================================================
    // STRENGTH (STR) - Physical Power
    // ============================================================================

    /**
     * Melee damage bonus from Strength.
     * Affects all physical attacks (weapons, unarmed).
     */
    fun meleeDamageBonus(strength: Int): Int {
        val baseMod = attributeModifier(strength)
        val breakpoint = breakpointBonus(strength)
        return baseMod + breakpoint
    }

    /**
     * Carrying capacity in stone (weight units).
     * Formula: STR * 5
     */
    fun carryingCapacity(strength: Int): Int = strength * 5

    /**
     * Bash/break door success chance (percentage).
     * Used for forcing open doors, breaking objects.
     */
    fun bashChance(strength: Int, level: Int): Int {
        val baseMod = attributeModifier(strength)
        val breakpoint = breakpointBonus(strength)
        // Base 30% + 5% per STR mod + 3% per breakpoint + 2% per level
        return (30 + (baseMod * 5) + (breakpoint * 3) + (level * 2)).coerceIn(5, 95)
    }

    /**
     * Minimum strength required for a weapon (based on weight/type).
     * Returns true if character can wield the weapon effectively.
     */
    fun canWieldWeapon(strength: Int, weaponWeight: Int, isTwoHanded: Boolean): Boolean {
        val requirement = if (isTwoHanded) weaponWeight * 2 else weaponWeight
        return strength >= requirement
    }

    // ============================================================================
    // INTELLIGENCE (INT) - Mental Acuity
    // ============================================================================

    /**
     * Bonus to maximum mana pool from Intelligence.
     */
    fun manaPoolBonus(intelligence: Int): Int {
        val baseMod = attributeModifier(intelligence)
        val breakpoint = breakpointBonus(intelligence)
        // 3 mana per INT mod, 5 mana per breakpoint tier
        return (baseMod * 3) + (breakpoint * 5)
    }

    /**
     * Spell damage multiplier (as percentage).
     * 100% = normal, 150% = 50% bonus damage.
     */
    fun spellDamageMultiplier(intelligence: Int): Int {
        val baseMod = attributeModifier(intelligence)
        val breakpoint = breakpointBonus(intelligence)
        // Base 100% + 5% per INT mod + 10% per breakpoint
        return 100 + (baseMod * 5) + (breakpoint * 10)
    }

    /**
     * Chance to successfully learn a spell from a trainer (percentage).
     * Higher INT = more likely to grasp complex spells.
     */
    fun spellLearningChance(intelligence: Int, spellLevel: Int, playerLevel: Int): Int {
        val baseMod = attributeModifier(intelligence)
        val breakpoint = breakpointBonus(intelligence)
        val levelDiff = playerLevel - spellLevel
        // Base 60% + 5% per INT mod + 8% per breakpoint + 5% per level above spell
        return (60 + (baseMod * 5) + (breakpoint * 8) + (levelDiff * 5)).coerceIn(10, 99)
    }

    /**
     * Identification success chance (percentage).
     * Used for identifying items, monsters, traps.
     */
    fun identifyChance(intelligence: Int, level: Int, itemRarity: Int = 1): Int {
        val baseMod = attributeModifier(intelligence)
        val breakpoint = breakpointBonus(intelligence)
        // Base 40% + 7% per INT mod + 5% per breakpoint + 3% per level - 10% per rarity tier
        return (40 + (baseMod * 7) + (breakpoint * 5) + (level * 3) - (itemRarity * 10)).coerceIn(5, 95)
    }

    /**
     * Resistance to charm/confusion effects (percentage reduction).
     */
    fun charmResistance(intelligence: Int): Int {
        val baseMod = attributeModifier(intelligence)
        val breakpoint = breakpointBonus(intelligence)
        return (baseMod * 4 + breakpoint * 6).coerceIn(0, 50)
    }

    // ============================================================================
    // WISDOM (WIS) - Spiritual Power & Willpower
    // ============================================================================

    /**
     * Mana regeneration per tick (combat round or regen cycle).
     */
    fun manaRegenBonus(wisdom: Int): Int {
        val baseMod = attributeModifier(wisdom)
        val breakpoint = breakpointBonus(wisdom)
        // 1 mana per WIS mod, 1 extra per breakpoint tier
        return (1 + baseMod + breakpoint).coerceAtLeast(1)
    }

    /**
     * Spell resistance (percentage reduction to incoming spell damage).
     * Also affects duration of negative magical effects.
     */
    fun spellResistance(wisdom: Int): Int {
        val baseMod = attributeModifier(wisdom)
        val breakpoint = breakpointBonus(wisdom)
        // 3% per WIS mod + 5% per breakpoint
        return (baseMod * 3 + breakpoint * 5).coerceIn(0, 50)
    }

    /**
     * Mental effect resistance (stun, charm, blind, confusion).
     * Higher than general spell resistance for mental effects.
     */
    fun mentalEffectResistance(wisdom: Int): Int {
        val baseMod = attributeModifier(wisdom)
        val breakpoint = breakpointBonus(wisdom)
        // 5% per WIS mod + 8% per breakpoint
        return (baseMod * 5 + breakpoint * 8).coerceIn(0, 60)
    }

    /**
     * Healing spell effectiveness multiplier (percentage).
     * For classes that use WIS as primary (clerics, druids).
     */
    fun healingEffectiveness(wisdom: Int): Int {
        val baseMod = attributeModifier(wisdom)
        val breakpoint = breakpointBonus(wisdom)
        return 100 + (baseMod * 4) + (breakpoint * 8)
    }

    /**
     * Saving throw bonus for will-based saves.
     */
    fun willSaveBonus(wisdom: Int): Int {
        val baseMod = attributeModifier(wisdom)
        val breakpoint = breakpointBonus(wisdom)
        return baseMod + breakpoint
    }

    /**
     * Perception bonus for detecting hidden/sneaking characters.
     * Uses WIS (awareness) + INT (pattern recognition).
     * Returns a bonus to perception checks.
     */
    fun perceptionBonus(wisdom: Int, intelligence: Int, level: Int): Int {
        val wisMod = attributeModifier(wisdom)
        val intMod = attributeModifier(intelligence)
        val wisBreakpoint = breakpointBonus(wisdom)
        // WIS is primary, INT is secondary
        // 3 per WIS mod + 1 per INT mod + 2 per WIS breakpoint + level/3
        return (wisMod * 3) + intMod + (wisBreakpoint * 2) + (level / 3)
    }

    /**
     * Perception check to detect a hidden/sneaking character.
     * Returns percentage chance of detection.
     * @param observerWis Observer's WIS
     * @param observerInt Observer's INT
     * @param observerLevel Observer's level
     * @param targetSneakCheck Target's stealth check result (from sneakChance or hideChance)
     */
    fun detectionChance(
        observerWis: Int,
        observerInt: Int,
        observerLevel: Int,
        targetStealth: Int
    ): Int {
        val perception = perceptionBonus(observerWis, observerInt, observerLevel)
        // Base 30% + perception bonus - target stealth/2
        // Higher perception = more likely to spot, higher stealth = less likely
        return (30 + perception - (targetStealth / 2)).coerceIn(5, 95)
    }

    // ============================================================================
    // DEXTERITY (DEX) - Agility & Speed
    // ============================================================================

    /**
     * Dodge/evasion chance bonus (percentage).
     */
    fun dodgeBonus(dexterity: Int): Int {
        val baseMod = attributeModifier(dexterity)
        val breakpoint = breakpointBonus(dexterity)
        // 2% per DEX mod + 3% per breakpoint
        return baseMod * 2 + breakpoint * 3
    }

    /**
     * Number of attack rounds per combat tick (MajorMUD-style).
     * Formula: 1 base + level/5 + DEX mod/2
     */
    fun attacksPerRound(dexterity: Int, level: Int): Int {
        val baseMod = attributeModifier(dexterity)
        val levelBonus = level / 5
        val dexBonus = baseMod / 2
        return (1 + levelBonus + dexBonus).coerceIn(1, 5)
    }

    /**
     * Initiative bonus (determines action order in combat).
     */
    fun initiativeBonus(dexterity: Int, level: Int): Int {
        val baseMod = attributeModifier(dexterity)
        val breakpoint = breakpointBonus(dexterity)
        // DEX mod + breakpoint + level/2
        return baseMod + breakpoint + (level / 2)
    }

    /**
     * Backstab accuracy bonus (for rogues/assassins).
     */
    fun backstabBonus(dexterity: Int, level: Int): Int {
        val baseMod = attributeModifier(dexterity)
        val breakpoint = breakpointBonus(dexterity)
        // 3% per DEX mod + 5% per breakpoint + 2% per level
        return baseMod * 3 + breakpoint * 5 + level * 2
    }

    /**
     * Pickpocket success chance (percentage).
     */
    fun pickpocketChance(dexterity: Int, level: Int, targetLevel: Int): Int {
        val baseMod = attributeModifier(dexterity)
        val breakpoint = breakpointBonus(dexterity)
        val levelDiff = level - targetLevel
        // Base 25% + 5% per DEX mod + 8% per breakpoint + 3% per level advantage
        return (25 + (baseMod * 5) + (breakpoint * 8) + (levelDiff * 3)).coerceIn(5, 85)
    }

    /**
     * Sneak/hide success chance (percentage).
     */
    fun sneakChance(dexterity: Int, level: Int, armorPenalty: Int = 0): Int {
        val baseMod = attributeModifier(dexterity)
        val breakpoint = breakpointBonus(dexterity)
        // Base 30% + 6% per DEX mod + 8% per breakpoint + 2% per level - armor penalty
        return (30 + (baseMod * 6) + (breakpoint * 8) + (level * 2) - armorPenalty).coerceIn(5, 95)
    }

    /**
     * Hide in place success/effectiveness value.
     * Similar to sneak but for stationary hiding.
     * Slightly higher base since not moving.
     */
    fun hideChance(dexterity: Int, level: Int, armorPenalty: Int = 0): Int {
        val baseMod = attributeModifier(dexterity)
        val breakpoint = breakpointBonus(dexterity)
        // Base 40% + 6% per DEX mod + 8% per breakpoint + 2% per level - armor penalty
        // Higher base than sneak because standing still is easier than moving silently
        return (40 + (baseMod * 6) + (breakpoint * 8) + (level * 2) - armorPenalty).coerceIn(10, 95)
    }

    /**
     * Ranged attack accuracy bonus.
     */
    fun rangedAccuracyBonus(dexterity: Int): Int {
        val baseMod = attributeModifier(dexterity)
        val breakpoint = breakpointBonus(dexterity)
        return baseMod + breakpoint
    }

    /**
     * Reflex save bonus (avoiding traps, dodging area effects).
     */
    fun reflexSaveBonus(dexterity: Int): Int {
        val baseMod = attributeModifier(dexterity)
        val breakpoint = breakpointBonus(dexterity)
        return baseMod + breakpoint
    }

    // ============================================================================
    // CONSTITUTION (CON) - Vitality & Endurance
    // ============================================================================

    /**
     * Bonus HP per level from Constitution.
     */
    fun hpPerLevelBonus(constitution: Int): Int {
        val baseMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        // CON mod per level, breakpoint adds flat bonus
        return baseMod + breakpoint
    }

    /**
     * Calculate max HP for a character.
     * Uses hit die from class + CON bonuses.
     */
    fun calculateMaxHp(constitution: Int, level: Int, hitDie: Int): Int {
        val conMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        // Level 1: max hit die + CON mod
        val baseHp = hitDie + conMod
        // Subsequent levels: average hit die + CON mod + breakpoint/2
        val perLevelHp = (hitDie / 2 + 1) + conMod + (breakpoint / 2)
        return (baseHp + (level - 1) * perLevelHp).coerceAtLeast(1)
    }

    /**
     * HP regeneration per tick (out of combat).
     */
    fun hpRegenBonus(constitution: Int, level: Int): Int {
        val baseMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        // 1 base + CON mod + breakpoint/2 + level/5
        return (1 + baseMod + (breakpoint / 2) + (level / 5)).coerceAtLeast(1)
    }

    /**
     * Poison resistance (percentage reduction to poison damage/duration).
     */
    fun poisonResistance(constitution: Int): Int {
        val baseMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        // 4% per CON mod + 7% per breakpoint
        return (baseMod * 4 + breakpoint * 7).coerceIn(0, 75)
    }

    /**
     * Disease resistance (percentage reduction to disease effects).
     */
    fun diseaseResistance(constitution: Int): Int {
        val baseMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        // 5% per CON mod + 8% per breakpoint
        return (baseMod * 5 + breakpoint * 8).coerceIn(0, 80)
    }

    /**
     * Death threshold (HP at which character truly dies).
     * In MajorMUD, characters could go below 0 HP and be "downed" but not dead.
     * Death occurs at -(10 + CON * 2).
     */
    fun deathThreshold(constitution: Int): Int {
        return -(10 + constitution * 2)
    }

    /**
     * Critical hit damage reduction (percentage).
     * High CON reduces extra damage from crits.
     */
    fun critDamageReduction(constitution: Int): Int {
        val baseMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        // 3% per CON mod + 5% per breakpoint
        return (baseMod * 3 + breakpoint * 5).coerceIn(0, 40)
    }

    /**
     * Fortitude save bonus (resisting physical effects).
     */
    fun fortitudeSaveBonus(constitution: Int): Int {
        val baseMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        return baseMod + breakpoint
    }

    /**
     * Stamina pool bonus from Constitution.
     */
    fun staminaPoolBonus(constitution: Int): Int {
        val baseMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        return (baseMod * 2) + (breakpoint * 3)
    }

    /**
     * Stamina regeneration per tick.
     */
    fun staminaRegenBonus(constitution: Int): Int {
        val baseMod = attributeModifier(constitution)
        val breakpoint = breakpointBonus(constitution)
        // 2 base + CON mod + breakpoint
        return (2 + baseMod + breakpoint).coerceAtLeast(1)
    }

    // ============================================================================
    // CHARISMA (CHA) - Presence & Luck
    // ============================================================================

    /**
     * Shop price modifier (percentage).
     * Positive = better prices (lower buy, higher sell).
     * Returns modifier to apply: 100 = normal, 90 = 10% discount, 110 = 10% markup
     */
    fun shopPriceModifier(charisma: Int): Int {
        val baseMod = attributeModifier(charisma)
        val breakpoint = breakpointBonus(charisma)
        // 2% better per CHA mod + 3% per breakpoint
        // Negative CHA = worse prices
        return 100 - (baseMod * 2) - (breakpoint * 3)
    }

    /**
     * Calculate adjusted buy price.
     */
    fun adjustBuyPrice(basePrice: Int, charisma: Int): Int {
        val modifier = shopPriceModifier(charisma)
        return ((basePrice * modifier) / 100).coerceAtLeast(1)
    }

    /**
     * Calculate adjusted sell price.
     * NPCs typically buy at 50% of value, CHA can improve this.
     */
    fun adjustSellPrice(basePrice: Int, charisma: Int): Int {
        val baseModifier = 50  // NPCs buy at 50% normally
        val chaBonus = (100 - shopPriceModifier(charisma))  // Invert for sell
        return ((basePrice * (baseModifier + chaBonus)) / 100).coerceAtLeast(1)
    }

    /**
     * Critical hit chance bonus (percentage).
     * CHA represents luck and finding openings.
     */
    fun critChanceBonus(charisma: Int): Int {
        val baseMod = attributeModifier(charisma)
        val breakpoint = breakpointBonus(charisma)
        // 1% per CHA mod + 2% per breakpoint
        return baseMod + breakpoint * 2
    }

    /**
     * Leadership bonus for party mechanics.
     * Affects party size, morale, shared XP bonuses.
     */
    fun leadershipBonus(charisma: Int, level: Int): Int {
        val baseMod = attributeModifier(charisma)
        val breakpoint = breakpointBonus(charisma)
        return baseMod + breakpoint + (level / 5)
    }

    /**
     * Maximum party size the character can effectively lead.
     */
    fun maxPartySize(charisma: Int): Int {
        val baseMod = attributeModifier(charisma)
        val breakpoint = breakpointBonus(charisma)
        // Base 4 + CHA mod + breakpoint
        return (4 + baseMod + breakpoint).coerceIn(2, 8)
    }

    /**
     * NPC interaction modifier (affects quest rewards, information, favors).
     * Returns a modifier from -20 (hostile) to +20 (friendly).
     */
    fun npcInteractionModifier(charisma: Int): Int {
        val baseMod = attributeModifier(charisma)
        val breakpoint = breakpointBonus(charisma)
        return (baseMod * 2 + breakpoint * 3).coerceIn(-20, 20)
    }

    /**
     * Persuasion/intimidation check bonus.
     */
    fun persuasionBonus(charisma: Int, level: Int): Int {
        val baseMod = attributeModifier(charisma)
        val breakpoint = breakpointBonus(charisma)
        return baseMod + breakpoint + (level / 3)
    }

    // ============================================================================
    // COMBINED STAT CALCULATIONS
    // ============================================================================

    /**
     * To-hit calculation for melee attacks.
     * Combines DEX (accuracy) vs target DEX (evasion) + modifiers.
     */
    fun meleeHitChance(
        attackerDex: Int,
        attackerLevel: Int,
        defenderDex: Int,
        defenderLevel: Int,
        weaponBonus: Int = 0,
        armorClass: Int = 0
    ): Int {
        val attackerBonus = attributeModifier(attackerDex) + attackerLevel / 2 + weaponBonus
        val defenderBonus = attributeModifier(defenderDex) + defenderLevel / 2 + armorClass
        // Base 50% + (attacker bonuses - defender bonuses) * 5%
        return (50 + (attackerBonus - defenderBonus) * 5).coerceIn(5, 95)
    }

    /**
     * Spell hit chance (for spells that can miss).
     * Uses INT vs target's spell resistance.
     */
    fun spellHitChance(
        casterInt: Int,
        casterLevel: Int,
        targetWis: Int,
        targetLevel: Int
    ): Int {
        val casterBonus = attributeModifier(casterInt) + casterLevel / 2
        val targetBonus = attributeModifier(targetWis) + targetLevel / 2
        // Base 60% + (caster - target) * 5%
        return (60 + (casterBonus - targetBonus) * 5).coerceIn(10, 95)
    }

    /**
     * Calculate critical hit chance.
     * Combines DEX (precision) + CHA (luck) + weapon skill.
     */
    fun criticalHitChance(
        dexterity: Int,
        charisma: Int,
        level: Int,
        weaponCritBonus: Int = 0
    ): Int {
        val dexContrib = attributeModifier(dexterity)
        val chaContrib = critChanceBonus(charisma)
        val levelContrib = level / 5
        // Base 5% crit chance + modifiers
        return (5 + dexContrib + chaContrib + levelContrib + weaponCritBonus).coerceIn(1, 50)
    }

    // ============================================================================
    // REGENERATION CALCULATIONS
    // ============================================================================

    /**
     * Calculate HP regeneration per cycle.
     * @param isResting True if in a safe room/resting state (2x regen)
     * @param isInCombat True if in active combat (no regen)
     */
    fun calculateHpRegen(
        constitution: Int,
        level: Int,
        isResting: Boolean = false,
        isInCombat: Boolean = false
    ): Int {
        if (isInCombat) return 0
        val baseRegen = hpRegenBonus(constitution, level)
        return if (isResting) baseRegen * 2 else baseRegen
    }

    /**
     * Calculate mana regeneration per cycle.
     * @param isResting True if in a safe room/resting state (2x regen)
     * @param isInCombat True if in active combat (0.5x regen)
     */
    fun calculateManaRegen(
        wisdom: Int,
        level: Int,
        isResting: Boolean = false,
        isInCombat: Boolean = false
    ): Int {
        val baseRegen = manaRegenBonus(wisdom) + (level / 4)
        return when {
            isResting -> baseRegen * 2
            isInCombat -> (baseRegen * 0.5).roundToInt().coerceAtLeast(1)
            else -> baseRegen
        }
    }

    /**
     * Calculate stamina regeneration per cycle.
     */
    fun calculateStaminaRegen(
        constitution: Int,
        isResting: Boolean = false,
        isInCombat: Boolean = false
    ): Int {
        val baseRegen = staminaRegenBonus(constitution)
        return when {
            isResting -> baseRegen * 2
            isInCombat -> (baseRegen * 0.75).roundToInt().coerceAtLeast(1)
            else -> baseRegen
        }
    }

    // ============================================================================
    // CHARACTER STAT SUMMARY
    // ============================================================================

    /**
     * Get a complete stat summary for a user.
     * Useful for character sheets and AI context.
     */
    fun getStatSummary(user: User, characterClass: CharacterClass? = null): StatSummary {
        val hitDie = characterClass?.hitDie ?: 8
        val encumbrance = EncumbranceService.getEncumbranceInfo(user)

        return StatSummary(
            // Base stats
            strength = user.strength,
            dexterity = user.dexterity,
            constitution = user.constitution,
            intelligence = user.intelligence,
            wisdom = user.wisdom,
            charisma = user.charisma,

            // STR-derived
            meleeDamageBonus = meleeDamageBonus(user.strength),
            carryCapacity = carryingCapacity(user.strength),
            bashChance = bashChance(user.strength, user.level),

            // INT-derived
            manaPoolBonus = manaPoolBonus(user.intelligence),
            spellDamageMultiplier = spellDamageMultiplier(user.intelligence),
            charmResistance = charmResistance(user.intelligence),

            // WIS-derived
            manaRegen = manaRegenBonus(user.wisdom),
            spellResistance = spellResistance(user.wisdom),
            mentalResistance = mentalEffectResistance(user.wisdom),
            healingEffectiveness = healingEffectiveness(user.wisdom),

            // DEX-derived
            dodgeBonus = dodgeBonus(user.dexterity),
            attacksPerRound = attacksPerRound(user.dexterity, user.level),
            initiative = initiativeBonus(user.dexterity, user.level),
            sneakChance = sneakChance(user.dexterity, user.level),

            // CON-derived
            hpBonus = hpPerLevelBonus(user.constitution),
            hpRegen = hpRegenBonus(user.constitution, user.level),
            poisonResistance = poisonResistance(user.constitution),
            diseaseResistance = diseaseResistance(user.constitution),
            deathThreshold = deathThreshold(user.constitution),
            staminaBonus = staminaPoolBonus(user.constitution),
            staminaRegen = staminaRegenBonus(user.constitution),

            // CHA-derived
            shopPriceModifier = shopPriceModifier(user.charisma),
            critBonus = critChanceBonus(user.charisma),
            maxPartySize = maxPartySize(user.charisma),
            npcModifier = npcInteractionModifier(user.charisma),

            // Combined
            critChance = criticalHitChance(user.dexterity, user.charisma, user.level),

            // Encumbrance info
            encumbranceTier = encumbrance.tier.name,
            encumbranceAttackMod = encumbrance.attackModifier,
            encumbranceDodgeMod = encumbrance.dodgeModifier,
            canMove = encumbrance.canMove
        )
    }
}

/**
 * Complete stat summary for a character.
 * Contains all derived values from base stats.
 */
data class StatSummary(
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
    val spellDamageMultiplier: Int,  // As percentage (100 = normal)
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
