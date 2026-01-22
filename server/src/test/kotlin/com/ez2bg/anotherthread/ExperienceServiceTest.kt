package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.Creature
import com.ez2bg.anotherthread.experience.ExperienceService
import kotlin.test.*

/**
 * Unit tests for the experience and leveling system.
 */
class ExperienceServiceTest {

    // ========== XP Scaling Tests ==========

    @Test
    fun testEqualLevelGivesFullXp() {
        val creature = testCreature(challengeRating = 5, experienceValue = 50)
        val xp = ExperienceService.calculateCreatureXp(playerLevel = 5, creature)

        // Equal level = 1.0x scale
        assertEquals(50, xp)
    }

    @Test
    fun testHigherCrGivesBonusXp() {
        val creature = testCreature(challengeRating = 10, experienceValue = 100)
        val xp = ExperienceService.calculateCreatureXp(playerLevel = 5, creature)

        // CR 10 vs level 5 = diff +5, so 1.5x scale
        assertEquals(150, xp)
    }

    @Test
    fun testSlightlyHigherCrGivesModerateBonus() {
        val creature = testCreature(challengeRating = 7, experienceValue = 100)
        val xp = ExperienceService.calculateCreatureXp(playerLevel = 5, creature)

        // CR 7 vs level 5 = diff +2, so 1.25x scale
        assertEquals(125, xp)
    }

    @Test
    fun testLowerCrGivesReducedXp() {
        val creature = testCreature(challengeRating = 2, experienceValue = 100)
        val xp = ExperienceService.calculateCreatureXp(playerLevel = 5, creature)

        // CR 2 vs level 5 = diff -3, so 0.5x scale
        assertEquals(50, xp)
    }

    @Test
    fun testMuchLowerCrGivesTrivialXp() {
        val creature = testCreature(challengeRating = 1, experienceValue = 100)
        val xp = ExperienceService.calculateCreatureXp(playerLevel = 10, creature)

        // CR 1 vs level 10 = diff -9, so 0.1x scale (floor)
        assertEquals(10, xp)
    }

    @Test
    fun testXpNeverBelowOne() {
        val creature = testCreature(challengeRating = 1, experienceValue = 5)
        val xp = ExperienceService.calculateCreatureXp(playerLevel = 20, creature)

        // Even with 0.1x scale on 5 XP = 0.5, should round to at least 1
        assertTrue(xp >= 1)
    }

    // ========== Level Calculation Tests ==========

    @Test
    fun testLevel1At0Xp() {
        assertEquals(1, ExperienceService.calculateLevel(0))
    }

    @Test
    fun testLevel2At100Xp() {
        assertEquals(2, ExperienceService.calculateLevel(100))
    }

    @Test
    fun testLevel5At1000Xp() {
        assertEquals(5, ExperienceService.calculateLevel(1000))
    }

    @Test
    fun testLevel20At19000Xp() {
        assertEquals(20, ExperienceService.calculateLevel(19000))
    }

    @Test
    fun testLevelCapsAt20() {
        assertEquals(20, ExperienceService.calculateLevel(100000))
    }

    // ========== HP Calculation Tests ==========

    @Test
    fun testLevel1Has10Hp() {
        assertEquals(10, ExperienceService.calculateMaxHp(1))
    }

    @Test
    fun testLevel5Has30Hp() {
        // 10 base + (4 levels * 5 HP) = 30
        assertEquals(30, ExperienceService.calculateMaxHp(5))
    }

    @Test
    fun testLevel20Has105Hp() {
        // 10 base + (19 levels * 5 HP) = 105
        assertEquals(105, ExperienceService.calculateMaxHp(20))
    }

    // ========== PvP XP Tests ==========

    @Test
    fun testPvpXpReducedBy50Percent() {
        // Equal level PvP
        val xp = ExperienceService.calculatePvpXp(winnerLevel = 10, loserLevel = 10)

        // Base: 20 + (10 * 5) = 70
        // Scale: 1.0x (equal level)
        // PvP reduction: 0.5x
        // Result: 70 * 1.0 * 0.5 = 35
        assertEquals(35, xp)
    }

    @Test
    fun testPvpXpHigherLevelOpponent() {
        val xp = ExperienceService.calculatePvpXp(winnerLevel = 5, loserLevel = 10)

        // Base: 20 + (10 * 5) = 70
        // Scale: 1.5x (diff +5)
        // PvP reduction: 0.5x
        // Result: 70 * 1.5 * 0.5 = 52.5 -> 53 (rounded)
        assertEquals(53, xp)
    }

    @Test
    fun testPvpXpLowerLevelOpponent() {
        val xp = ExperienceService.calculatePvpXp(winnerLevel = 10, loserLevel = 2)

        // Base: 20 + (2 * 5) = 30
        // Scale: 0.1x (diff -8)
        // PvP reduction: 0.5x
        // Result: 30 * 0.1 * 0.5 = 1.5 -> 2
        assertTrue(xp >= 1)
    }

    // ========== Individual Party Scaling Tests ==========

    @Test
    fun testLowLevelPlayerGetsFullXpFromHighCr() {
        // Level 1 player fights CR 5 creature with level 10 friend
        // The level 1 player should still get bonus XP
        val creature = testCreature(challengeRating = 5, experienceValue = 50)

        val level1Xp = ExperienceService.calculateCreatureXp(playerLevel = 1, creature)
        val level10Xp = ExperienceService.calculateCreatureXp(playerLevel = 10, creature)

        // Level 1 vs CR 5 = diff +4, 1.5x = 75
        assertEquals(75, level1Xp)

        // Level 10 vs CR 5 = diff -5, 0.25x = 12.5 -> 13 (rounded)
        assertEquals(13, level10Xp)

        // Low level player gets MORE XP from same kill
        assertTrue(level1Xp > level10Xp)
    }

    // ========== Helper Functions ==========

    private fun testCreature(
        challengeRating: Int,
        experienceValue: Int,
        level: Int = challengeRating
    ) = Creature(
        id = "test-creature",
        name = "Test Creature",
        desc = "A test creature",
        itemIds = emptyList(),
        featureIds = emptyList(),
        maxHp = 10 + (level * 5),
        baseDamage = 5 + level,
        level = level,
        experienceValue = experienceValue,
        challengeRating = challengeRating
    )
}
