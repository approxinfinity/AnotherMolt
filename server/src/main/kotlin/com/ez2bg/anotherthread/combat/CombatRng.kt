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
    const val BASE_HIT_CHANCE = 75          // 75% base hit chance
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
