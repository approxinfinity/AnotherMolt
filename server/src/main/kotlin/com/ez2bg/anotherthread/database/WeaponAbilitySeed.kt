package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Seed weapon-specific abilities for existing items.
 * Each weapon gets abilities that match its description and lore.
 *
 * Weapons are looked up by name (not hardcoded UUIDs) for portability.
 */
object WeaponAbilitySeed {
    private val log = LoggerFactory.getLogger(WeaponAbilitySeed::class.java)

    /**
     * Configuration for a weapon and its abilities.
     */
    data class WeaponAbilityConfig(
        val weaponName: String,
        val abilities: List<Ability>
    )

    /**
     * All weapon ability configurations.
     * Add new weapons here to grant them abilities on seed.
     */
    private val weaponConfigs = listOf(
        // Staff of Kazekage no Kokoro: healing staff with focus buff
        WeaponAbilityConfig(
            weaponName = "Staff of Kazekage no Kokoro",
            abilities = listOf(
                Ability(
                    id = "weapon-staff-heal",
                    name = "Soothing Winds",
                    description = "Channel the staff's healing energy to restore health to yourself or an ally.",
                    classId = null,
                    abilityType = "spell",
                    targetType = "single_ally",
                    range = 30,
                    cooldownType = "short",
                    cooldownRounds = 2,
                    baseDamage = 18,
                    durationRounds = 0,
                    effects = """["heal"]"""
                ),
                Ability(
                    id = "weapon-staff-focus",
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
        ),

        // Khra'gixxys: dark dagger with frenzy buff and vicious strike
        WeaponAbilityConfig(
            weaponName = "Khra'gixxys",
            abilities = listOf(
                Ability(
                    id = "weapon-khragixxys-strike",
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
                    id = "weapon-khragixxys-frenzy",
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
        ),

        // Khra'gixx: rusted sword, basic attack only
        WeaponAbilityConfig(
            weaponName = "Khra'gixx",
            abilities = listOf(
                Ability(
                    id = "weapon-khragixx-strike",
                    name = "Rusted Slash",
                    description = "A basic attack with the withered blade. Not very effective, but still dangerous.",
                    classId = null,
                    abilityType = "combat",
                    targetType = "single_enemy",
                    range = 5,
                    cooldownType = "none",
                    cooldownRounds = 0,
                    baseDamage = 8,
                    durationRounds = 0,
                    effects = "[]"
                )
            )
        ),

        // Aerthys' Whisper: blowgun with tranquilizing effects
        WeaponAbilityConfig(
            weaponName = "Aerthys' Whisper",
            abilities = listOf(
                Ability(
                    id = "weapon-whisper-dart",
                    name = "Wind Dart",
                    description = "Fire a swift dart from the silver blowgun.",
                    classId = null,
                    abilityType = "combat",
                    targetType = "single_enemy",
                    range = 40,
                    cooldownType = "none",
                    cooldownRounds = 0,
                    baseDamage = 6,
                    durationRounds = 0,
                    effects = "[]"
                ),
                Ability(
                    id = "weapon-whisper-sleep",
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
                    effects = """["stun"]"""
                )
            )
        )
    )

    fun seedWeaponAbilities() {
        log.info("Seeding weapon abilities...")

        var abilitiesCreated = 0
        var weaponsUpdated = 0

        for (config in weaponConfigs) {
            // Find the weapon by name
            val weapon = ItemRepository.findByName(config.weaponName)
            if (weapon == null) {
                log.debug("Weapon not found: ${config.weaponName} - skipping")
                continue
            }

            // Create abilities that don't exist yet
            val abilityIds = mutableListOf<String>()
            for (ability in config.abilities) {
                val created = seedAbility(ability)
                if (created) abilitiesCreated++
                abilityIds.add(ability.id)
            }

            // Update weapon with ability IDs if it doesn't have them
            if (weapon.abilityIds.isEmpty() && abilityIds.isNotEmpty()) {
                val updated = weapon.copy(abilityIds = abilityIds)
                ItemRepository.update(updated)
                weaponsUpdated++
                log.info("Updated ${weapon.name} with abilities: ${abilityIds.joinToString()}")
            }
        }

        log.info("Weapon abilities seeding complete: $abilitiesCreated abilities created, $weaponsUpdated weapons updated")
    }

    private fun seedAbility(ability: Ability): Boolean {
        return transaction {
            val exists = AbilityTable.selectAll()
                .where { AbilityTable.id eq ability.id }
                .count() > 0

            if (!exists) {
                AbilityRepository.create(ability)
                log.info("Created weapon ability: ${ability.name} (${ability.id})")
                true
            } else {
                false
            }
        }
    }
}
