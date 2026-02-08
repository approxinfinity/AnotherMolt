package com.ez2bg.anotherthread.combat

import kotlin.random.Random

/**
 * Combat RNG system for hit/miss, damage variance, and critical hits.
 *
 * MajorMUD-style combat mechanics:
 * - Hit chance based on accuracy vs evasion
 * - Damage varies ±25% from base
 * - Critical hits deal 2x damage
 * - Glancing blows deal 50% damage on near-misses
 */
object CombatRng {

    // Default combat stats
    const val BASE_HIT_CHANCE = 65          // 65% base hit chance
    const val BASE_CRIT_CHANCE = 5          // 5% base crit chance
    const val CRIT_MULTIPLIER = 2.0         // 2x damage on crit
    const val GLANCING_BLOW_MULTIPLIER = 0.5 // 50% damage on glancing blow
    const val DAMAGE_VARIANCE = 0.25        // ±25% damage variance
    const val GLANCING_THRESHOLD = 10       // Within 10% of hit chance = glancing blow

    /**
     * Result of a hit roll.
     */
    enum class HitResult {
        MISS,           // Complete miss, no damage
        GLANCING,       // Glancing blow, reduced damage
        HIT,            // Normal hit
        CRITICAL        // Critical hit, bonus damage
    }

    /**
     * Full result of an attack roll.
     */
    data class AttackResult(
        val hitResult: HitResult,
        val damage: Int,
        val wasCritical: Boolean = hitResult == HitResult.CRITICAL,
        val wasGlancing: Boolean = hitResult == HitResult.GLANCING,
        val rollDetails: RollDetails
    )

    /**
     * Details of the dice rolls for transparency/logging.
     */
    data class RollDetails(
        val hitRoll: Int,           // 1-100 roll
        val hitChance: Int,         // Chance needed to hit
        val critRoll: Int,          // 1-100 roll for crit
        val critChance: Int,        // Chance needed to crit
        val baseDamage: Int,        // Pre-variance damage
        val damageRoll: Double,     // Variance multiplier applied
        val finalDamage: Int        // After all modifiers
    )

    /**
     * Calculate hit chance based on attacker accuracy vs defender evasion.
     *
     * @param attackerAccuracy Attacker's accuracy rating (higher = more likely to hit)
     * @param defenderEvasion Defender's evasion rating (higher = more likely to dodge)
     * @param attackerLevel Attacker's level (small bonus)
     * @param defenderLevel Defender's level (small penalty)
     */
    fun calculateHitChance(
        attackerAccuracy: Int = 0,
        defenderEvasion: Int = 0,
        attackerLevel: Int = 1,
        defenderLevel: Int = 1
    ): Int {
        val levelDiff = attackerLevel - defenderLevel
        val baseChance = BASE_HIT_CHANCE + attackerAccuracy - defenderEvasion + (levelDiff * 2)
        return baseChance.coerceIn(5, 95) // Always 5-95% chance
    }

    /**
     * Calculate crit chance based on attacker stats.
     *
     * @param attackerCritBonus Bonus to crit chance from gear/abilities
     */
    fun calculateCritChance(attackerCritBonus: Int = 0): Int {
        return (BASE_CRIT_CHANCE + attackerCritBonus).coerceIn(1, 50) // Max 50% crit
    }

    /**
     * Roll to hit and determine hit result.
     *
     * @param hitChance The chance to hit (0-100)
     * @param critChance The chance to crit (0-100)
     * @return HitResult indicating miss, glancing, hit, or critical
     */
    fun rollToHit(hitChance: Int, critChance: Int): Pair<HitResult, Pair<Int, Int>> {
        val hitRoll = Random.nextInt(1, 101) // 1-100
        val critRoll = Random.nextInt(1, 101) // 1-100

        val result = when {
            hitRoll > hitChance + GLANCING_THRESHOLD -> HitResult.MISS
            hitRoll > hitChance -> HitResult.GLANCING
            critRoll <= critChance -> HitResult.CRITICAL
            else -> HitResult.HIT
        }

        return result to (hitRoll to critRoll)
    }

    /**
     * Calculate damage with variance.
     *
     * @param baseDamage The base damage before variance
     * @param hitResult The hit result (affects multiplier)
     * @return Pair of (final damage, variance multiplier)
     */
    fun calculateDamage(baseDamage: Int, hitResult: HitResult): Pair<Int, Double> {
        if (baseDamage <= 0 || hitResult == HitResult.MISS) {
            return 0 to 0.0
        }

        // Calculate variance: ±25% (0.75 to 1.25)
        val variance = 1.0 + (Random.nextDouble() * 2 * DAMAGE_VARIANCE - DAMAGE_VARIANCE)

        // Apply hit result multiplier
        val multiplier = when (hitResult) {
            HitResult.MISS -> 0.0
            HitResult.GLANCING -> GLANCING_BLOW_MULTIPLIER * variance
            HitResult.HIT -> variance
            HitResult.CRITICAL -> CRIT_MULTIPLIER * variance
        }

        val finalDamage = (baseDamage * multiplier).toInt().coerceAtLeast(1)
        return finalDamage to multiplier
    }

    /**
     * Perform a complete attack roll.
     *
     * @param baseDamage Base damage of the attack
     * @param attackerAccuracy Attacker's accuracy stat
     * @param defenderEvasion Defender's evasion stat
     * @param attackerLevel Attacker's level
     * @param defenderLevel Defender's level
     * @param critBonus Bonus to critical hit chance
     * @return Complete AttackResult with damage and details
     */
    fun rollAttack(
        baseDamage: Int,
        attackerAccuracy: Int = 0,
        defenderEvasion: Int = 0,
        attackerLevel: Int = 1,
        defenderLevel: Int = 1,
        critBonus: Int = 0
    ): AttackResult {
        val hitChance = calculateHitChance(attackerAccuracy, defenderEvasion, attackerLevel, defenderLevel)
        val critChance = calculateCritChance(critBonus)

        val (hitResult, rolls) = rollToHit(hitChance, critChance)
        val (hitRoll, critRoll) = rolls

        val (finalDamage, damageMultiplier) = calculateDamage(baseDamage, hitResult)

        return AttackResult(
            hitResult = hitResult,
            damage = finalDamage,
            rollDetails = RollDetails(
                hitRoll = hitRoll,
                hitChance = hitChance,
                critRoll = critRoll,
                critChance = critChance,
                baseDamage = baseDamage,
                damageRoll = damageMultiplier,
                finalDamage = finalDamage
            )
        )
    }

    /**
     * Roll healing with variance (no hit check, always succeeds).
     *
     * @param baseHealing Base healing amount
     * @param critBonus Bonus to critical heal chance
     * @return Pair of (final healing, was critical)
     */
    fun rollHealing(baseHealing: Int, critBonus: Int = 0): Pair<Int, Boolean> {
        if (baseHealing <= 0) return 0 to false

        val critChance = calculateCritChance(critBonus)
        val critRoll = Random.nextInt(1, 101)
        val isCrit = critRoll <= critChance

        // Variance: ±25%
        val variance = 1.0 + (Random.nextDouble() * 2 * DAMAGE_VARIANCE - DAMAGE_VARIANCE)
        val multiplier = if (isCrit) CRIT_MULTIPLIER * variance else variance

        val finalHealing = (baseHealing * multiplier).toInt().coerceAtLeast(1)
        return finalHealing to isCrit
    }

    /**
     * Roll dice in NdS format (e.g., 2d6 = roll 2 six-sided dice).
     *
     * @param count Number of dice to roll
     * @param sides Number of sides per die
     * @return Sum of all dice
     */
    fun rollDice(count: Int, sides: Int): Int {
        if (count <= 0 || sides <= 0) return 0
        return (1..count).sumOf { Random.nextInt(1, sides + 1) }
    }

    /**
     * Parse result of a dice string roll.
     */
    data class DiceRollResult(
        val total: Int,           // Final total after all rolls and modifiers
        val diceTotal: Int,       // Sum of just the dice (before modifier)
        val modifier: Int,        // The +/- modifier
        val rolls: List<Int>,     // Individual die results
        val formula: String       // Original formula
    )

    // Regex to parse dice notation: XdY or XdY+Z or XdY-Z
    // Examples: "2d6", "1d8+3", "3d4-1", "d20" (treated as 1d20)
    private val diceRegex = Regex("""^(\d*)d(\d+)([+-]\d+)?$""", RegexOption.IGNORE_CASE)

    /**
     * Parse and roll a dice string in XdY+Z format.
     *
     * Supported formats:
     * - "2d6" - Roll 2 six-sided dice
     * - "1d8+3" - Roll 1d8 and add 3
     * - "3d4-1" - Roll 3d4 and subtract 1
     * - "d20" - Same as 1d20
     *
     * @param diceString The dice notation string (e.g., "2d6+3")
     * @return DiceRollResult with total, individual rolls, and formula details
     * @throws IllegalArgumentException if the format is invalid
     */
    fun rollDiceString(diceString: String): DiceRollResult {
        val trimmed = diceString.trim().lowercase()

        val match = diceRegex.matchEntire(trimmed)
            ?: throw IllegalArgumentException("Invalid dice format: '$diceString'. Expected format like '2d6' or '1d8+3'")

        val countStr = match.groupValues[1]
        val count = if (countStr.isEmpty()) 1 else countStr.toInt()
        val sides = match.groupValues[2].toInt()
        val modifierStr = match.groupValues[3]
        val modifier = if (modifierStr.isEmpty()) 0 else modifierStr.toInt()

        if (count <= 0 || sides <= 0) {
            throw IllegalArgumentException("Dice count and sides must be positive: '$diceString'")
        }

        // Roll each die individually
        val rolls = (1..count).map { Random.nextInt(1, sides + 1) }
        val diceTotal = rolls.sum()
        val total = (diceTotal + modifier).coerceAtLeast(1) // Minimum 1 damage

        return DiceRollResult(
            total = total,
            diceTotal = diceTotal,
            modifier = modifier,
            rolls = rolls,
            formula = diceString
        )
    }

    /**
     * Safely roll a dice string, returning null if the format is invalid.
     */
    fun rollDiceStringSafe(diceString: String?): DiceRollResult? {
        if (diceString.isNullOrBlank()) return null
        return try {
            rollDiceString(diceString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Calculate the average roll for a dice string.
     * Useful for display and balance calculations.
     *
     * @param diceString The dice notation string (e.g., "2d6+3")
     * @return The average expected roll, or null if format is invalid
     */
    fun averageDiceRoll(diceString: String?): Double? {
        if (diceString.isNullOrBlank()) return null
        val trimmed = diceString.trim().lowercase()

        val match = diceRegex.matchEntire(trimmed) ?: return null

        val countStr = match.groupValues[1]
        val count = if (countStr.isEmpty()) 1 else countStr.toInt()
        val sides = match.groupValues[2].toInt()
        val modifierStr = match.groupValues[3]
        val modifier = if (modifierStr.isEmpty()) 0 else modifierStr.toInt()

        // Average of a die is (1 + sides) / 2
        val avgPerDie = (1 + sides) / 2.0
        return (count * avgPerDie) + modifier
    }

    /**
     * Perform a complete attack roll using dice notation.
     * If damageDice is provided, uses dice rolling. Otherwise falls back to baseDamage.
     *
     * @param damageDice Dice notation string (e.g., "2d6+3"), or null to use baseDamage
     * @param baseDamage Fallback base damage if damageDice is null
     * @param attackerAccuracy Attacker's accuracy stat
     * @param defenderEvasion Defender's evasion stat
     * @param attackerLevel Attacker's level
     * @param defenderLevel Defender's level
     * @param critBonus Bonus to critical hit chance
     * @return Complete AttackResult with damage and details
     */
    fun rollAttackWithDice(
        damageDice: String?,
        baseDamage: Int,
        attackerAccuracy: Int = 0,
        defenderEvasion: Int = 0,
        attackerLevel: Int = 1,
        defenderLevel: Int = 1,
        critBonus: Int = 0
    ): AttackResult {
        val hitChance = calculateHitChance(attackerAccuracy, defenderEvasion, attackerLevel, defenderLevel)
        val critChance = calculateCritChance(critBonus)

        val (hitResult, rolls) = rollToHit(hitChance, critChance)
        val (hitRoll, critRoll) = rolls

        // Calculate damage based on hit result
        val rawDamage = if (hitResult == HitResult.MISS) {
            0
        } else {
            // Roll dice if available, otherwise use baseDamage
            val diceResult = rollDiceStringSafe(damageDice)
            diceResult?.total ?: baseDamage
        }

        // Apply hit modifiers (glancing = 50%, crit = 200%)
        val multiplier = when (hitResult) {
            HitResult.MISS -> 0.0
            HitResult.GLANCING -> GLANCING_BLOW_MULTIPLIER
            HitResult.HIT -> 1.0
            HitResult.CRITICAL -> CRIT_MULTIPLIER
        }

        val finalDamage = (rawDamage * multiplier).toInt().coerceAtLeast(if (hitResult == HitResult.MISS) 0 else 1)

        return AttackResult(
            hitResult = hitResult,
            damage = finalDamage,
            rollDetails = RollDetails(
                hitRoll = hitRoll,
                hitChance = hitChance,
                critRoll = critRoll,
                critChance = critChance,
                baseDamage = rawDamage,  // This now reflects the dice roll result
                damageRoll = multiplier,
                finalDamage = finalDamage
            )
        )
    }

    /**
     * Roll a single die.
     */
    fun rollD20(): Int = Random.nextInt(1, 21)
    fun rollD12(): Int = Random.nextInt(1, 13)
    fun rollD10(): Int = Random.nextInt(1, 11)
    fun rollD8(): Int = Random.nextInt(1, 9)
    fun rollD6(): Int = Random.nextInt(1, 7)
    fun rollD4(): Int = Random.nextInt(1, 5)
    fun rollD100(): Int = Random.nextInt(1, 101)
}
