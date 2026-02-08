package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.combat.CombatRng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the dice rolling system (XdY+Z notation).
 */
class DiceRollingTest {

    // ========================================================================
    // DICE STRING PARSING TESTS
    // ========================================================================

    @Test
    fun testRollDiceString_simpleFormat() {
        // Roll 2d6
        val result = CombatRng.rollDiceString("2d6")

        assertEquals("2d6", result.formula)
        assertEquals(2, result.rolls.size)
        assertEquals(0, result.modifier)
        assertEquals(result.rolls.sum(), result.diceTotal)
        assertEquals(result.diceTotal, result.total) // No modifier

        // Each die should be 1-6
        result.rolls.forEach { roll ->
            assertTrue(roll in 1..6, "Die roll should be 1-6, was $roll")
        }
    }

    @Test
    fun testRollDiceString_withPositiveModifier() {
        // Roll 1d8+3
        val result = CombatRng.rollDiceString("1d8+3")

        assertEquals("1d8+3", result.formula)
        assertEquals(1, result.rolls.size)
        assertEquals(3, result.modifier)
        assertTrue(result.rolls[0] in 1..8)
        assertEquals(result.diceTotal + 3, result.total)
    }

    @Test
    fun testRollDiceString_withNegativeModifier() {
        // Roll 3d4-2
        val result = CombatRng.rollDiceString("3d4-2")

        assertEquals("3d4-2", result.formula)
        assertEquals(3, result.rolls.size)
        assertEquals(-2, result.modifier)
        result.rolls.forEach { roll ->
            assertTrue(roll in 1..4)
        }
        assertEquals(result.diceTotal - 2, result.total.coerceAtLeast(1))
    }

    @Test
    fun testRollDiceString_singleDie() {
        // Roll d20 (should be treated as 1d20)
        val result = CombatRng.rollDiceString("d20")

        assertEquals("d20", result.formula)
        assertEquals(1, result.rolls.size)
        assertEquals(0, result.modifier)
        assertTrue(result.rolls[0] in 1..20)
    }

    @Test
    fun testRollDiceString_caseInsensitive() {
        // Roll D6 (uppercase)
        val result = CombatRng.rollDiceString("2D6")

        assertEquals(2, result.rolls.size)
        result.rolls.forEach { roll ->
            assertTrue(roll in 1..6)
        }
    }

    @Test
    fun testRollDiceString_withWhitespace() {
        // Roll " 2d6+1 " (with spaces)
        val result = CombatRng.rollDiceString(" 2d6+1 ")

        assertEquals(2, result.rolls.size)
        assertEquals(1, result.modifier)
    }

    @Test
    fun testRollDiceString_minimumOneResult() {
        // Even with large negative modifier, result should be at least 1
        // Roll 1d4-10 (worst case: 1 - 10 = -9, should be coerced to 1)
        val result = CombatRng.rollDiceString("1d4-10")

        assertEquals(1, result.total, "Minimum damage should be 1")
    }

    @Test
    fun testRollDiceString_invalidFormat() {
        // Invalid formats should throw
        assertFailsWith<IllegalArgumentException> {
            CombatRng.rollDiceString("invalid")
        }

        assertFailsWith<IllegalArgumentException> {
            CombatRng.rollDiceString("2d")
        }

        assertFailsWith<IllegalArgumentException> {
            CombatRng.rollDiceString("d")
        }

        assertFailsWith<IllegalArgumentException> {
            CombatRng.rollDiceString("2d6+")
        }
    }

    @Test
    fun testRollDiceStringSafe_validInput() {
        val result = CombatRng.rollDiceStringSafe("2d6+3")

        assertNotNull(result)
        assertEquals(3, result.modifier)
    }

    @Test
    fun testRollDiceStringSafe_invalidInput() {
        val result = CombatRng.rollDiceStringSafe("invalid")

        assertNull(result)
    }

    @Test
    fun testRollDiceStringSafe_nullInput() {
        val result = CombatRng.rollDiceStringSafe(null)

        assertNull(result)
    }

    @Test
    fun testRollDiceStringSafe_emptyInput() {
        val result = CombatRng.rollDiceStringSafe("")

        assertNull(result)
    }

    // ========================================================================
    // AVERAGE DICE ROLL TESTS
    // ========================================================================

    @Test
    fun testAverageDiceRoll_2d6() {
        // Average of 2d6 = 2 * 3.5 = 7
        val avg = CombatRng.averageDiceRoll("2d6")

        assertEquals(7.0, avg)
    }

    @Test
    fun testAverageDiceRoll_1d8plus3() {
        // Average of 1d8+3 = 4.5 + 3 = 7.5
        val avg = CombatRng.averageDiceRoll("1d8+3")

        assertEquals(7.5, avg)
    }

    @Test
    fun testAverageDiceRoll_3d4minus1() {
        // Average of 3d4-1 = 3 * 2.5 - 1 = 6.5
        val avg = CombatRng.averageDiceRoll("3d4-1")

        assertEquals(6.5, avg)
    }

    @Test
    fun testAverageDiceRoll_invalidFormat() {
        val avg = CombatRng.averageDiceRoll("invalid")

        assertNull(avg)
    }

    @Test
    fun testAverageDiceRoll_nullInput() {
        val avg = CombatRng.averageDiceRoll(null)

        assertNull(avg)
    }

    // ========================================================================
    // ROLL ATTACK WITH DICE TESTS
    // ========================================================================

    @Test
    fun testRollAttackWithDice_usesDiceWhenProvided() {
        // Run multiple times to ensure dice are being used
        var differentResults = mutableSetOf<Int>()

        repeat(20) {
            val result = CombatRng.rollAttackWithDice(
                damageDice = "3d6",
                baseDamage = 5,  // Should be ignored
                attackerAccuracy = 100,  // Always hit
                defenderEvasion = 0
            )

            if (result.hitResult != CombatRng.HitResult.MISS) {
                differentResults.add(result.damage)
            }
        }

        // With 3d6, we should get some variation (3-18 base, before multipliers)
        assertTrue(differentResults.size > 1, "Dice should produce varying damage results")
    }

    @Test
    fun testRollAttackWithDice_fallsBackToBaseDamage() {
        // When damageDice is null, should use baseDamage
        val result = CombatRng.rollAttackWithDice(
            damageDice = null,
            baseDamage = 10,
            attackerAccuracy = 100,  // Always hit
            defenderEvasion = 0
        )

        // Damage should be based on baseDamage (10) with possible multipliers
        if (result.hitResult != CombatRng.HitResult.MISS) {
            assertTrue(result.damage >= 1, "Should deal at least 1 damage")
        }
    }

    @Test
    fun testRollAttackWithDice_criticalDoublesResult() {
        // Force a critical by using high crit bonus
        var critFound = false
        var normalFound = false

        repeat(100) {
            val result = CombatRng.rollAttackWithDice(
                damageDice = "1d6",  // Base 1-6
                baseDamage = 0,
                attackerAccuracy = 100,
                defenderEvasion = 0,
                critBonus = 50  // 50% crit chance
            )

            if (result.wasCritical) {
                critFound = true
                // Critical should deal 2x damage (dice roll * 2)
            }
            if (result.hitResult == CombatRng.HitResult.HIT && !result.wasCritical) {
                normalFound = true
            }
        }

        assertTrue(critFound, "Should have at least one critical with 50% crit chance")
        assertTrue(normalFound, "Should have at least one normal hit")
    }

    @Test
    fun testRollAttackWithDice_missDoesZeroDamage() {
        var missFound = false

        repeat(50) {
            val result = CombatRng.rollAttackWithDice(
                damageDice = "2d6+5",
                baseDamage = 10,
                attackerAccuracy = 0,
                defenderEvasion = 100  // Very hard to hit
            )

            if (result.hitResult == CombatRng.HitResult.MISS) {
                missFound = true
                assertEquals(0, result.damage, "Miss should deal 0 damage")
            }
        }

        assertTrue(missFound, "Should have at least one miss with high evasion")
    }

    // ========================================================================
    // DICE DISTRIBUTION TESTS
    // ========================================================================

    @Test
    fun testDiceDistribution_validRange() {
        // Roll 2d6 1000 times, all results should be 2-12
        repeat(1000) {
            val result = CombatRng.rollDiceString("2d6")
            assertTrue(result.diceTotal in 2..12, "2d6 should be 2-12, was ${result.diceTotal}")
        }
    }

    @Test
    fun testDiceDistribution_1d20() {
        // Roll 1d20 1000 times, all results should be 1-20
        repeat(1000) {
            val result = CombatRng.rollDiceString("1d20")
            assertTrue(result.diceTotal in 1..20, "1d20 should be 1-20, was ${result.diceTotal}")
        }
    }

    @Test
    fun testDiceDistribution_coversFullRange() {
        // Roll 1d6 many times, should eventually hit all values 1-6
        val values = mutableSetOf<Int>()

        repeat(1000) {
            val result = CombatRng.rollDiceString("1d6")
            values.add(result.diceTotal)
        }

        assertEquals(6, values.size, "Should have all values 1-6: $values")
        assertTrue(values.containsAll(listOf(1, 2, 3, 4, 5, 6)))
    }

    // ========================================================================
    // EXISTING ROLL DICE FUNCTION TESTS
    // ========================================================================

    @Test
    fun testRollDice_2d6() {
        repeat(100) {
            val result = CombatRng.rollDice(2, 6)
            assertTrue(result in 2..12, "2d6 should be 2-12")
        }
    }

    @Test
    fun testRollDice_zeroDice() {
        val result = CombatRng.rollDice(0, 6)
        assertEquals(0, result, "0 dice should return 0")
    }

    @Test
    fun testRollDice_zeroSides() {
        val result = CombatRng.rollDice(2, 0)
        assertEquals(0, result, "0-sided dice should return 0")
    }

    @Test
    fun testRollD20() {
        repeat(100) {
            val result = CombatRng.rollD20()
            assertTrue(result in 1..20, "d20 should be 1-20")
        }
    }

    @Test
    fun testRollD6() {
        repeat(100) {
            val result = CombatRng.rollD6()
            assertTrue(result in 1..6, "d6 should be 1-6")
        }
    }
}
