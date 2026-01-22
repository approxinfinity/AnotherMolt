package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Seed weapon-specific abilities for existing items.
 * Each weapon gets abilities that match its description and lore.
 */
object WeaponAbilitySeed {
    private val log = LoggerFactory.getLogger(WeaponAbilitySeed::class.java)

    // Item IDs from the database
    private const val STAFF_OF_KAZEKAGE = "574e89df-7aea-4baf-b4af-240627d32aea"
    private const val KHRAGIXXYS = "c1a5d150-65db-4407-9085-3b20a198e55f"
    private const val KHRAGIXX = "4aff8bc1-e213-4447-8cb4-731a2d64df4f"
    private const val AERTHYS_WHISPER = "0cc7b403-18b9-4010-a984-ffbcf2c660b2"

    // Ability IDs (using deterministic IDs for seeding)
    private const val STAFF_HEAL_ID = "weapon-staff-heal"
    private const val STAFF_FOCUS_ID = "weapon-staff-focus"
    private const val KHRAGIXXYS_STRIKE_ID = "weapon-khragixxys-strike"
    private const val KHRAGIXXYS_FRENZY_ID = "weapon-khragixxys-frenzy"
    private const val KHRAGIXX_STRIKE_ID = "weapon-khragixx-strike"
    private const val WHISPER_DART_ID = "weapon-whisper-dart"
    private const val WHISPER_SLEEP_ID = "weapon-whisper-sleep"

    fun seedWeaponAbilities() {
        log.info("Seeding weapon abilities...")

        seedAbilitiesForStaffOfKazekage()
        seedAbilitiesForKhragixxys()
        seedAbilitiesForKhragixx()
        seedAbilitiesForAerthysWhisper()

        // Update items with their ability IDs
        updateItemAbilities()

        log.info("Weapon abilities seeding complete")
    }

    private fun seedAbilitiesForStaffOfKazekage() {
        // Staff of Kazekage no Kokoro: healing staff with focus buff
        val abilities = listOf(
            Ability(
                id = STAFF_HEAL_ID,
                name = "Soothing Winds",
                description = "Channel the staff's healing energy to restore health to yourself or an ally.",
                classId = null, // Item ability, not class-specific
                abilityType = "spell",
                targetType = "single_ally",
                range = 30,
                cooldownType = "short",
                cooldownRounds = 2,
                baseDamage = 18, // Heal amount
                durationRounds = 0,
                effects = """["heal"]"""
            ),
            Ability(
                id = STAFF_FOCUS_ID,
                name = "Inner Balance",
                description = "The staff amplifies your inner balance, granting increased focus and agility.",
                classId = null,
                abilityType = "spell",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 0,
                durationRounds = 3,
                effects = """["buff"]"""
            )
        )

        abilities.forEach { seedAbility(it) }
    }

    private fun seedAbilitiesForKhragixxys() {
        // Khra'gixxys: dark dagger with frenzy buff and vicious strike
        val abilities = listOf(
            Ability(
                id = KHRAGIXXYS_STRIKE_ID,
                name = "Vicious Strike",
                description = "A savage attack with the obsidian blade, dealing heavy damage.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 15,
                durationRounds = 0,
                effects = "[]"
            ),
            Ability(
                id = KHRAGIXXYS_FRENZY_ID,
                name = "Bestial Frenzy",
                description = "The dagger imbues you with the ferocity of a ravenous beast.",
                classId = null,
                abilityType = "combat",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 6,
                baseDamage = 0,
                durationRounds = 3,
                effects = """["buff"]"""
            )
        )

        abilities.forEach { seedAbility(it) }
    }

    private fun seedAbilitiesForKhragixx() {
        // Khra'gixx: rusted sword, basic attack only
        val abilities = listOf(
            Ability(
                id = KHRAGIXX_STRIKE_ID,
                name = "Rusted Slash",
                description = "A basic attack with the withered blade. Not very effective, but still dangerous.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 8, // Lower damage due to rust
                durationRounds = 0,
                effects = "[]"
            )
        )

        abilities.forEach { seedAbility(it) }
    }

    private fun seedAbilitiesForAerthysWhisper() {
        // Aerthys' Whisper: blowgun with tranquilizing effects
        val abilities = listOf(
            Ability(
                id = WHISPER_DART_ID,
                name = "Wind Dart",
                description = "Fire a swift dart from the silver blowgun.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 40, // Long range
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 6,
                durationRounds = 0,
                effects = "[]"
            ),
            Ability(
                id = WHISPER_SLEEP_ID,
                name = "Tranquil Slumber",
                description = "Fire a tranquilizing dart that puts the target into a deep slumber.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 40,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 3,
                durationRounds = 2,
                effects = """["stun"]""" // Sleep effect represented as stun
            )
        )

        abilities.forEach { seedAbility(it) }
    }

    private fun seedAbility(ability: Ability) {
        transaction {
            val exists = AbilityTable.selectAll()
                .where { AbilityTable.id eq ability.id }
                .count() > 0

            if (!exists) {
                AbilityRepository.create(ability)
                log.info("Created weapon ability: ${ability.name} (${ability.id})")
            }
        }
    }

    private fun updateItemAbilities() {
        // Map item IDs to their ability IDs
        val itemAbilities = mapOf(
            STAFF_OF_KAZEKAGE to listOf(STAFF_HEAL_ID, STAFF_FOCUS_ID),
            KHRAGIXXYS to listOf(KHRAGIXXYS_STRIKE_ID, KHRAGIXXYS_FRENZY_ID),
            KHRAGIXX to listOf(KHRAGIXX_STRIKE_ID),
            AERTHYS_WHISPER to listOf(WHISPER_DART_ID, WHISPER_SLEEP_ID)
        )

        transaction {
            for ((itemId, abilityIds) in itemAbilities) {
                val item = ItemRepository.findById(itemId)
                if (item != null && item.abilityIds.isEmpty()) {
                    val updated = item.copy(abilityIds = abilityIds)
                    ItemRepository.update(updated)
                    log.info("Updated item ${item.name} with abilities: ${abilityIds.joinToString()}")
                }
            }
        }
    }
}
