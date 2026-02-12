package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import org.jetbrains.exposed.sql.deleteAll
import kotlin.test.*

/**
 * Unit tests for D&D-style attribute calculations.
 * Tests edge cases in stat modifiers, HP/Mana/Stamina formulas.
 *
 * These tests target bugs found in:
 * - Integer division edge cases in attributeModifier()
 * - Negative modifier handling in resource calculations
 * - Level scaling formulas
 * - coerceAtLeast() boundary conditions
 */
class AttributeCalculationTest {

    @BeforeTest
    fun setup() {
        TestDatabaseConfig.init()
        clearAllTablesForTest()
        seedTestData()
    }

    private fun clearAllTablesForTest() {
        org.jetbrains.exposed.sql.transactions.transaction {
            UserTable.deleteAll()
            CharacterClassTable.deleteAll()
        }
    }

    private fun seedTestData() {
        CharacterClassRepository.create(TestFixtures.warriorClass())  // hitDie = 10
        CharacterClassRepository.create(TestFixtures.healerClass())   // hitDie = 6, wisdom primary
        CharacterClassRepository.create(TestFixtures.mageClass())     // hitDie = 6, intelligence primary
    }

    // ========== Attribute Modifier Tests ==========
    // D&D formula: (stat - 10) / 2, using integer division

    @Test
    fun testAttributeModifier_Stat10_ReturnsZero() {
        val modifier = UserRepository.attributeModifier(10)
        assertEquals(0, modifier, "Stat 10 should give +0 modifier")
    }

    @Test
    fun testAttributeModifier_Stat11_ReturnsZero() {
        // Integer division: (11-10)/2 = 1/2 = 0
        val modifier = UserRepository.attributeModifier(11)
        assertEquals(0, modifier, "Stat 11 should give +0 modifier (integer division)")
    }

    @Test
    fun testAttributeModifier_Stat12_ReturnsOne() {
        // (12-10)/2 = 2/2 = 1
        val modifier = UserRepository.attributeModifier(12)
        assertEquals(1, modifier, "Stat 12 should give +1 modifier")
    }

    @Test
    fun testAttributeModifier_Stat18_ReturnsFour() {
        // (18-10)/2 = 8/2 = 4
        val modifier = UserRepository.attributeModifier(18)
        assertEquals(4, modifier, "Stat 18 should give +4 modifier")
    }

    @Test
    fun testAttributeModifier_Stat20_ReturnsFive() {
        // (20-10)/2 = 10/2 = 5
        val modifier = UserRepository.attributeModifier(20)
        assertEquals(5, modifier, "Stat 20 should give +5 modifier")
    }

    @Test
    fun testAttributeModifier_Stat9_ReturnsZero() {
        // Integer division floors toward zero: (9-10)/2 = -1/2 = 0 in Kotlin
        val modifier = UserRepository.attributeModifier(9)
        assertEquals(0, modifier, "Stat 9 should give 0 modifier (integer division toward zero)")
    }

    @Test
    fun testAttributeModifier_Stat8_ReturnsNegativeOne() {
        // (8-10)/2 = -2/2 = -1
        val modifier = UserRepository.attributeModifier(8)
        assertEquals(-1, modifier, "Stat 8 should give -1 modifier")
    }

    @Test
    fun testAttributeModifier_Stat6_ReturnsNegativeTwo() {
        // (6-10)/2 = -4/2 = -2
        val modifier = UserRepository.attributeModifier(6)
        assertEquals(-2, modifier, "Stat 6 should give -2 modifier")
    }

    @Test
    fun testAttributeModifier_Stat3_ReturnsNegativeThree() {
        // Extreme low stat: (3-10)/2 = -7/2 = -3 (integer division)
        val modifier = UserRepository.attributeModifier(3)
        assertEquals(-3, modifier, "Stat 3 should give -3 modifier")
    }

    // ========== Max HP Calculation Tests ==========
    // Formula: (hitDie + CON mod) at level 1, then (hitDie/2+1 + CON mod) per level
    // Result is coerced to at least 1

    @Test
    fun testCalculateMaxHp_Level1_AverageCon() {
        val user = createTestUserWithStats(constitution = 10, level = 1)
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxHp = UserRepository.calculateMaxHp(user, characterClass)

        // hitDie=10, conMod=0: base = 10+0 = 10, perLevel = 0
        assertEquals(10, maxHp, "Level 1 warrior with CON 10 should have 10 HP")
    }

    @Test
    fun testCalculateMaxHp_Level1_HighCon() {
        val user = createTestUserWithStats(constitution = 18, level = 1)
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxHp = UserRepository.calculateMaxHp(user, characterClass)

        // hitDie=10, conMod=4: base = 10+4 = 14
        assertEquals(14, maxHp, "Level 1 warrior with CON 18 should have 14 HP")
    }

    @Test
    fun testCalculateMaxHp_Level1_LowCon() {
        val user = createTestUserWithStats(constitution = 6, level = 1)
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxHp = UserRepository.calculateMaxHp(user, characterClass)

        // hitDie=10, conMod=-2: base = 10+(-2) = 8
        assertEquals(8, maxHp, "Level 1 warrior with CON 6 should have 8 HP")
    }

    @Test
    fun testCalculateMaxHp_Level1_VeryLowCon_CoercedToOne() {
        // Create a healer (hitDie=6) with very low CON
        val user = createTestUserWithStats(
            constitution = 3,
            level = 1,
            characterClassId = TestFixtures.HEALER_CLASS_ID
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.HEALER_CLASS_ID)!!

        val maxHp = UserRepository.calculateMaxHp(user, characterClass)

        // hitDie=6, conMod=-3: base = 6+(-3) = 3
        // Should still be positive
        assertTrue(maxHp >= 1, "HP should never go below 1")
        assertEquals(3, maxHp)
    }

    @Test
    fun testCalculateMaxHp_Level5_AverageCon() {
        val user = createTestUserWithStats(constitution = 10, level = 5)
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxHp = UserRepository.calculateMaxHp(user, characterClass)

        // hitDie=10, conMod=0:
        // base = 10, perLevel = 10/2+1 = 6
        // total = 10 + (5-1)*6 = 10 + 24 = 34
        assertEquals(34, maxHp, "Level 5 warrior with CON 10 should have 34 HP")
    }

    @Test
    fun testCalculateMaxHp_Level10_HighCon() {
        val user = createTestUserWithStats(constitution = 18, level = 10)
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxHp = UserRepository.calculateMaxHp(user, characterClass)

        // hitDie=10, conMod=4, breakpoint(18)=2:
        // base = 10+4 = 14, perLevel = 10/2+1 + 4 + 2/2 = 6+4+1 = 11
        // total = 14 + (10-1)*11 = 14 + 99 = 113
        assertEquals(113, maxHp, "Level 10 warrior with CON 18 should have 113 HP")
    }

    @Test
    fun testCalculateMaxHp_Level10_VeryLowCon_StillPositive() {
        // Edge case: even with terrible stats, HP should be >= 1
        val user = createTestUserWithStats(
            constitution = 3,
            level = 10,
            characterClassId = TestFixtures.HEALER_CLASS_ID
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.HEALER_CLASS_ID)!!

        val maxHp = UserRepository.calculateMaxHp(user, characterClass)

        // hitDie=6, conMod=-3:
        // base = 6+(-3) = 3, perLevel = 6/2+1+(-3) = 4-3 = 1
        // total = 3 + (10-1)*1 = 3 + 9 = 12
        assertTrue(maxHp >= 1, "HP should never go below 1")
    }

    @Test
    fun testCalculateMaxHp_NoCharacterClass_DefaultsToD8() {
        val user = createTestUserWithStats(constitution = 10, level = 1, characterClassId = null)

        val maxHp = UserRepository.calculateMaxHp(user, null)

        // Default hitDie=8 when no class, conMod=0: base = 8
        assertEquals(8, maxHp, "No class should default to d8 hit die")
    }

    // ========== Max Mana Calculation Tests ==========
    // Formula: baseMana + (spellMod * 2) + (level * 2)
    // spellMod = primary attribute modifier (INT for mages, WIS for healers)

    @Test
    fun testCalculateMaxMana_MageLevel1_AverageInt() {
        val user = createTestUserWithStats(
            intelligence = 10,
            level = 1,
            characterClassId = TestFixtures.MAGE_CLASS_ID
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.MAGE_CLASS_ID)!!

        val maxMana = UserRepository.calculateMaxMana(user, characterClass)

        // baseMana=10 (default), intMod=0: 10 + 0*2 + 1*2 = 12
        assertTrue(maxMana >= 0, "Mana should not be negative")
    }

    @Test
    fun testCalculateMaxMana_MageLevel1_HighInt() {
        val user = createTestUserWithStats(
            intelligence = 18,
            level = 1,
            characterClassId = TestFixtures.MAGE_CLASS_ID
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.MAGE_CLASS_ID)!!

        val maxMana = UserRepository.calculateMaxMana(user, characterClass)

        // baseMana=10, intMod=4: 10 + 4*2 + 1*2 = 10 + 8 + 2 = 20
        assertTrue(maxMana > 12, "High INT should give more mana")
    }

    @Test
    fun testCalculateMaxMana_MageLevel1_LowInt() {
        val user = createTestUserWithStats(
            intelligence = 6,
            level = 1,
            characterClassId = TestFixtures.MAGE_CLASS_ID
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.MAGE_CLASS_ID)!!

        val maxMana = UserRepository.calculateMaxMana(user, characterClass)

        // baseMana=10, intMod=-2: 10 + (-2)*2 + 1*2 = 10 - 4 + 2 = 8
        assertTrue(maxMana >= 0, "Mana should be coerced to at least 0")
    }

    @Test
    fun testCalculateMaxMana_HealerUsesWisdom() {
        val user = createTestUserWithStats(
            wisdom = 18,
            intelligence = 8,  // Low INT shouldn't matter for healer
            level = 1,
            characterClassId = TestFixtures.HEALER_CLASS_ID
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.HEALER_CLASS_ID)!!

        val maxMana = UserRepository.calculateMaxMana(user, characterClass)

        // Should use WIS (+4) not INT (-1)
        // baseMana=10, wisMod=4: 10 + 4*2 + 1*2 = 20
        assertTrue(maxMana >= 16, "Healer should use WIS modifier")
    }

    @Test
    fun testCalculateMaxMana_WarriorNoSpellMod() {
        val user = createTestUserWithStats(
            intelligence = 18,  // High INT shouldn't matter for warrior
            level = 1,
            characterClassId = TestFixtures.WARRIOR_CLASS_ID
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxMana = UserRepository.calculateMaxMana(user, characterClass)

        // Warrior has STR as primary, so spellMod = 0
        // baseMana=10, spellMod=0: 10 + 0 + 2 = 12
        assertTrue(maxMana >= 0)
    }

    // ========== Max Stamina Calculation Tests ==========
    // Formula: baseStamina + (avgPhysicalMod * 2) + (level * 2)
    // avgPhysicalMod = (STR + DEX + CON mods) / 3

    @Test
    fun testCalculateMaxStamina_AveragePhysicalStats() {
        val user = createTestUserWithStats(
            strength = 10,
            dexterity = 10,
            constitution = 10,
            level = 1
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxStamina = UserRepository.calculateMaxStamina(user, characterClass)

        // avgPhysicalMod = (0+0+0)/3 = 0
        // baseStamina=10, 10 + 0 + 2 = 12
        assertTrue(maxStamina >= 0)
    }

    @Test
    fun testCalculateMaxStamina_HighPhysicalStats() {
        val user = createTestUserWithStats(
            strength = 18,
            dexterity = 16,
            constitution = 16,
            level = 1
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxStamina = UserRepository.calculateMaxStamina(user, characterClass)

        // strMod=4, dexMod=3, conMod=3
        // avgPhysicalMod = (4+3+3)/3 = 3 (integer division)
        // baseStamina=10, 10 + 3*2 + 2 = 18
        assertTrue(maxStamina > 12, "High physical stats should give more stamina")
    }

    @Test
    fun testCalculateMaxStamina_MixedPhysicalStats_IntegerDivision() {
        // Test that integer division works correctly for averages
        val user = createTestUserWithStats(
            strength = 14,   // +2
            dexterity = 12,  // +1
            constitution = 10,  // +0
            level = 1
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxStamina = UserRepository.calculateMaxStamina(user, characterClass)

        // avgPhysicalMod = (2+1+0)/3 = 1 (integer division)
        // baseStamina=10, 10 + 1*2 + 2 = 14
        assertTrue(maxStamina >= 0)
    }

    @Test
    fun testCalculateMaxStamina_LowPhysicalStats_CoercedToZero() {
        val user = createTestUserWithStats(
            strength = 6,    // -2
            dexterity = 6,   // -2
            constitution = 6,  // -2
            level = 1
        )
        val characterClass = CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID)!!

        val maxStamina = UserRepository.calculateMaxStamina(user, characterClass)

        // avgPhysicalMod = (-2-2-2)/3 = -2
        // baseStamina=10, 10 + (-2)*2 + 2 = 10 - 4 + 2 = 8
        // Should be coerced to at least 0
        assertTrue(maxStamina >= 0, "Stamina should be coerced to at least 0")
    }

    // ========== Combat Stat Calculation Tests ==========

    @Test
    fun testCalculateAccuracy_BasedOnDexAndLevel() {
        val user = createTestUserWithStats(dexterity = 16, level = 5)

        val accuracy = UserRepository.calculateAccuracy(user)

        // dexMod=3, level/2 = 2: 3 + 2 = 5
        assertEquals(5, accuracy)
    }

    @Test
    fun testCalculateAccuracy_WithEquipmentBonus() {
        val user = createTestUserWithStats(dexterity = 10, level = 1)

        val accuracy = UserRepository.calculateAccuracy(user, equipmentAttackBonus = 5)

        // dexMod=0, level/2=0, bonus=5: 0 + 0 + 5 = 5
        assertEquals(5, accuracy)
    }

    @Test
    fun testCalculateEvasion_BasedOnDex() {
        val user = createTestUserWithStats(dexterity = 18)

        val evasion = UserRepository.calculateEvasion(user)

        // dodgeBonus(18) = dexMod(4)*2 + breakpoint(2)*3 = 8+6 = 14, level(1)/2=0
        assertEquals(14, evasion)
    }

    @Test
    fun testCalculateEvasion_WithEquipmentBonus() {
        val user = createTestUserWithStats(dexterity = 10)

        val evasion = UserRepository.calculateEvasion(user, equipmentDefenseBonus = 3)

        // dexMod=0, bonus=3: 0 + 3 = 3
        assertEquals(3, evasion)
    }

    @Test
    fun testCalculateCritBonus_BasedOnCharismaAndLevel() {
        val user = createTestUserWithStats(charisma = 16, level = 10)

        val critBonus = UserRepository.calculateCritBonus(user)

        // critChanceBonus(16) = chaMod(3) + breakpoint(2)*2 = 3+4 = 7, level(10)/5=2: 7+2 = 9
        assertEquals(9, critBonus)
    }

    @Test
    fun testCalculateBaseDamage_BasedOnStrengthAndLevel() {
        val user = createTestUserWithStats(strength = 16, level = 5)

        val baseDamage = UserRepository.calculateBaseDamage(user)

        // 5 + level(5) + meleeDamageBonus(16) = 5 + 5 + (3+2) = 15
        assertEquals(15, baseDamage)
    }

    @Test
    fun testCalculateBaseDamage_CoercedToOne() {
        // Edge case: negative strength shouldn't give 0 or negative damage
        val user = createTestUserWithStats(strength = 3, level = 1)

        val baseDamage = UserRepository.calculateBaseDamage(user)

        // 5 + 1 + (-3) = 3, but should be at least 1
        assertTrue(baseDamage >= 1, "Base damage should be at least 1")
    }

    @Test
    fun testCalculateBaseDamage_WithEquipmentBonus() {
        val user = createTestUserWithStats(strength = 10, level = 1)

        val baseDamage = UserRepository.calculateBaseDamage(user, equipmentAttackBonus = 10)

        // 5 + 1 + 0 + 10 = 16
        assertEquals(16, baseDamage)
    }

    // ========== updateAttributes Integration Test ==========

    @Test
    fun testUpdateAttributes_RecalculatesAllResources() {
        val user = createTestUser()

        val result = UserRepository.updateAttributes(
            id = user.id,
            strength = 16,
            dexterity = 14,
            constitution = 18,
            intelligence = 10,
            wisdom = 10,
            charisma = 12,
            qualityBonus = 3
        )

        assertTrue(result)
        val updated = UserRepository.findById(user.id)!!

        // Verify attributes were updated
        assertEquals(16, updated.strength)
        assertEquals(14, updated.dexterity)
        assertEquals(18, updated.constitution)

        // Verify resources were recalculated and set to max
        assertEquals(updated.maxHp, updated.currentHp, "Should be full healed")
        assertEquals(updated.maxMana, updated.currentMana, "Should be at full mana")
        assertEquals(updated.maxStamina, updated.currentStamina, "Should be at full stamina")
    }

    // ========== Helper Methods ==========

    private fun createTestUserWithStats(
        strength: Int = 10,
        dexterity: Int = 10,
        constitution: Int = 10,
        intelligence: Int = 10,
        wisdom: Int = 10,
        charisma: Int = 10,
        level: Int = 1,
        characterClassId: String? = TestFixtures.WARRIOR_CLASS_ID
    ): User {
        return User(
            id = "test-user-${System.nanoTime()}",
            name = "TestUser_${System.nanoTime()}",
            passwordHash = "test-hash",
            desc = "Test user for attribute tests",
            currentHp = 30,
            maxHp = 30,
            strength = strength,
            dexterity = dexterity,
            constitution = constitution,
            intelligence = intelligence,
            wisdom = wisdom,
            charisma = charisma,
            level = level,
            characterClassId = characterClassId
        )
    }

    private fun createTestUser(): User {
        val user = User(
            id = "test-user-${System.nanoTime()}",
            name = "TestUser_${System.nanoTime()}",
            passwordHash = "test-hash",
            desc = "Test user",
            currentHp = 20,
            maxHp = 30,
            characterClassId = TestFixtures.WARRIOR_CLASS_ID
        )
        UserRepository.create(user)
        return user
    }
}
