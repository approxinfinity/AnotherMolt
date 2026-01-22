package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.combat.CombatRng
import kotlin.test.*

/**
 * Unit tests for the Combat RNG system.
 * Tests hit/miss mechanics, damage variance, critical hits, and glancing blows.
 */
class CombatRngTest {

    // ========== Hit Chance Calculation Tests ==========

    @Test
    fun testBaseHitChanceIs75() {
        val hitChance = CombatRng.calculateHitChance()
        assertEquals(75, hitChance)
    }

    @Test
    fun testAccuracyIncreasesHitChance() {
        val hitChance = CombatRng.calculateHitChance(attackerAccuracy = 10)
        assertEquals(85, hitChance)
    }

    @Test
    fun testEvasionDecreasesHitChance() {
        val hitChance = CombatRng.calculateHitChance(defenderEvasion = 10)
        assertEquals(65, hitChance)
    }

    @Test
    fun testLevelDifferenceAffectsHitChance() {
        // Attacker higher level
        val highLevelHitChance = CombatRng.calculateHitChance(attackerLevel = 5, defenderLevel = 1)
        assertEquals(83, highLevelHitChance) // 75 + (4 * 2) = 83

        // Defender higher level
        val lowLevelHitChance = CombatRng.calculateHitChance(attackerLevel = 1, defenderLevel = 5)
        assertEquals(67, lowLevelHitChance) // 75 + (-4 * 2) = 67
    }

    @Test
    fun testHitChanceClampedBetween5And95() {
        // Very high hit chance capped at 95
        val highChance = CombatRng.calculateHitChance(attackerAccuracy = 100)
        assertEquals(95, highChance)

        // Very low hit chance capped at 5
        val lowChance = CombatRng.calculateHitChance(defenderEvasion = 100)
        assertEquals(5, lowChance)
    }

    // ========== Crit Chance Calculation Tests ==========

    @Test
    fun testBaseCritChanceIs5() {
        val critChance = CombatRng.calculateCritChance()
        assertEquals(5, critChance)
    }

    @Test
    fun testCritBonusIncreasesCritChance() {
        val critChance = CombatRng.calculateCritChance(attackerCritBonus = 10)
        assertEquals(15, critChance)
    }

    @Test
    fun testCritChanceClampedBetween1And50() {
        // Very high crit chance capped at 50
        val highCrit = CombatRng.calculateCritChance(attackerCritBonus = 100)
        assertEquals(50, highCrit)

        // Even with negative bonus, minimum 1%
        val lowCrit = CombatRng.calculateCritChance(attackerCritBonus = -10)
        assertEquals(1, lowCrit)
    }

    // ========== Hit Roll Tests ==========

    @Test
    fun testHitRollReturnsValidResult() {
        val (result, rolls) = CombatRng.rollToHit(75, 5)
        assertTrue(result in CombatRng.HitResult.entries)
        val (hitRoll, critRoll) = rolls
        assertTrue(hitRoll in 1..100)
        assertTrue(critRoll in 1..100)
    }

    @Test
    fun testHighHitChanceAlmostAlwaysHits() {
        // With 95% hit chance, over many rolls most should hit
        var hits = 0
        repeat(100) {
            val (result, _) = CombatRng.rollToHit(95, 0)
            if (result != CombatRng.HitResult.MISS) hits++
        }
        assertTrue(hits >= 80) // Should hit at least 80% with some variance
    }

    @Test
    fun testLowHitChanceOftenMisses() {
        // With 10% hit chance, most should miss
        var misses = 0
        repeat(100) {
            val (result, _) = CombatRng.rollToHit(10, 0)
            if (result == CombatRng.HitResult.MISS) misses++
        }
        assertTrue(misses >= 60) // Should miss at least 60%
    }

    // ========== Damage Calculation Tests ==========

    @Test
    fun testMissDealsNoDamage() {
        val (damage, _) = CombatRng.calculateDamage(100, CombatRng.HitResult.MISS)
        assertEquals(0, damage)
    }

    @Test
    fun testNormalHitDealsDamageWithVariance() {
        val baseDamage = 100
        var minDamage = Int.MAX_VALUE
        var maxDamage = Int.MIN_VALUE

        repeat(100) {
            val (damage, _) = CombatRng.calculateDamage(baseDamage, CombatRng.HitResult.HIT)
            if (damage < minDamage) minDamage = damage
            if (damage > maxDamage) maxDamage = damage
        }

        // Variance should produce range of ~75-125 for base 100
        assertTrue(minDamage < 90) // At least some low rolls
        assertTrue(maxDamage > 110) // At least some high rolls
    }

    @Test
    fun testGlancingBlowDealsReducedDamage() {
        var totalGlancingDamage = 0
        var totalNormalDamage = 0

        repeat(100) {
            val (glancing, _) = CombatRng.calculateDamage(100, CombatRng.HitResult.GLANCING)
            val (normal, _) = CombatRng.calculateDamage(100, CombatRng.HitResult.HIT)
            totalGlancingDamage += glancing
            totalNormalDamage += normal
        }

        // Glancing average should be roughly half of normal
        val glancingAvg = totalGlancingDamage / 100.0
        val normalAvg = totalNormalDamage / 100.0
        assertTrue(glancingAvg < normalAvg * 0.7) // Glancing should be noticeably less
    }

    @Test
    fun testCriticalHitDealsIncreasedDamage() {
        var totalCritDamage = 0
        var totalNormalDamage = 0

        repeat(100) {
            val (crit, _) = CombatRng.calculateDamage(100, CombatRng.HitResult.CRITICAL)
            val (normal, _) = CombatRng.calculateDamage(100, CombatRng.HitResult.HIT)
            totalCritDamage += crit
            totalNormalDamage += normal
        }

        // Crit average should be roughly double
        val critAvg = totalCritDamage / 100.0
        val normalAvg = totalNormalDamage / 100.0
        assertTrue(critAvg > normalAvg * 1.5) // Crits should be noticeably more
    }

    @Test
    fun testDamageAlwaysAtLeastOne() {
        // Even with variance, minimum damage is 1
        repeat(100) {
            val (damage, _) = CombatRng.calculateDamage(1, CombatRng.HitResult.GLANCING)
            assertTrue(damage >= 1)
        }
    }

    // ========== Full Attack Roll Tests ==========

    @Test
    fun testRollAttackReturnsCompleteResult() {
        val result = CombatRng.rollAttack(
            baseDamage = 50,
            attackerAccuracy = 10,
            defenderEvasion = 5,
            attackerLevel = 3,
            defenderLevel = 2,
            critBonus = 3
        )

        assertNotNull(result.hitResult)
        assertNotNull(result.rollDetails)
        // Calculate: 75 + 10 - 5 + ((3-2) * 2) = 75 + 10 - 5 + 2 = 82
        assertEquals(82, result.rollDetails.hitChance)
        assertEquals(8, result.rollDetails.critChance) // 5 + 3 = 8
        assertEquals(50, result.rollDetails.baseDamage)
    }

    @Test
    fun testRollAttackDamageMatchesHitResult() {
        repeat(50) {
            val result = CombatRng.rollAttack(baseDamage = 100)

            when (result.hitResult) {
                CombatRng.HitResult.MISS -> {
                    assertEquals(0, result.damage)
                    assertFalse(result.wasCritical)
                    assertFalse(result.wasGlancing)
                }
                CombatRng.HitResult.GLANCING -> {
                    assertTrue(result.damage > 0)
                    assertTrue(result.damage < 80) // Should be roughly 50% or less with variance
                    assertFalse(result.wasCritical)
                    assertTrue(result.wasGlancing)
                }
                CombatRng.HitResult.HIT -> {
                    assertTrue(result.damage > 0)
                    assertFalse(result.wasCritical)
                    assertFalse(result.wasGlancing)
                }
                CombatRng.HitResult.CRITICAL -> {
                    assertTrue(result.damage > 100) // Should be > base due to 2x multiplier
                    assertTrue(result.wasCritical)
                    assertFalse(result.wasGlancing)
                }
            }
        }
    }

    // ========== Healing Roll Tests ==========

    @Test
    fun testRollHealingReturnsPositiveAmount() {
        val (healing, _) = CombatRng.rollHealing(50)
        assertTrue(healing > 0)
    }

    @Test
    fun testRollHealingWithCritBonus() {
        var critCount = 0
        repeat(100) {
            val (_, isCrit) = CombatRng.rollHealing(50, critBonus = 25)
            if (isCrit) critCount++
        }
        // With 30% crit chance (5 base + 25 bonus), should crit fairly often
        assertTrue(critCount >= 15) // At least 15% should crit
    }

    @Test
    fun testRollHealingVariance() {
        var minHealing = Int.MAX_VALUE
        var maxHealing = Int.MIN_VALUE

        repeat(100) {
            val (healing, _) = CombatRng.rollHealing(100)
            if (healing < minHealing) minHealing = healing
            if (healing > maxHealing) maxHealing = healing
        }

        // Should have variance like damage
        assertTrue(minHealing < 90)
        assertTrue(maxHealing > 110)
    }

    @Test
    fun testRollHealingZeroBaseReturnsZero() {
        val (healing, isCrit) = CombatRng.rollHealing(0)
        assertEquals(0, healing)
        assertFalse(isCrit)
    }

    // ========== Dice Rolling Tests ==========

    @Test
    fun testRollDiceReturnsValidRange() {
        repeat(100) {
            val result = CombatRng.rollDice(2, 6)
            assertTrue(result in 2..12) // 2d6 range
        }
    }

    @Test
    fun testRollDiceWithZeroReturnsZero() {
        assertEquals(0, CombatRng.rollDice(0, 6))
        assertEquals(0, CombatRng.rollDice(2, 0))
    }

    @Test
    fun testSingleDieRolls() {
        repeat(100) {
            assertTrue(CombatRng.rollD20() in 1..20)
            assertTrue(CombatRng.rollD12() in 1..12)
            assertTrue(CombatRng.rollD10() in 1..10)
            assertTrue(CombatRng.rollD8() in 1..8)
            assertTrue(CombatRng.rollD6() in 1..6)
            assertTrue(CombatRng.rollD4() in 1..4)
            assertTrue(CombatRng.rollD100() in 1..100)
        }
    }

    // ========== Glancing Blow Threshold Tests ==========

    @Test
    fun testGlancingBlowWithinThreshold() {
        // Roll many times with exact hit chance boundary
        // With hit chance of 50 and glancing threshold of 10,
        // rolls 51-60 should be glancing (not miss, not hit)
        var glancingCount = 0
        var normalCount = 0

        repeat(1000) {
            val (result, rolls) = CombatRng.rollToHit(50, 0)
            if (result == CombatRng.HitResult.GLANCING) glancingCount++
            if (result == CombatRng.HitResult.HIT) normalCount++
        }

        // Should have some glancing blows
        assertTrue(glancingCount > 0)
    }

    // ========== Statistical Distribution Tests ==========

    @Test
    fun testAttackResultDistributionIsReasonable() {
        val results = mutableMapOf<CombatRng.HitResult, Int>()

        repeat(1000) {
            val result = CombatRng.rollAttack(baseDamage = 100)
            results[result.hitResult] = results.getOrDefault(result.hitResult, 0) + 1
        }

        // With 75% hit, 10% glancing threshold, 5% crit:
        // ~15% miss (rolls > 85)
        // ~10% glancing (rolls 76-85)
        // ~5% crit (of hits)
        // ~70% normal hit

        val totalRolls = results.values.sum()
        assertEquals(1000, totalRolls)

        // Rough distribution checks (with generous bounds for randomness)
        assertTrue(results.getOrDefault(CombatRng.HitResult.MISS, 0) >= 50) // At least 5% misses
        assertTrue(results.getOrDefault(CombatRng.HitResult.HIT, 0) >= 400) // Majority should hit
    }
}
