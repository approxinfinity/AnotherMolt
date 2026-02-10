package com.ez2bg.anotherthread.database

/**
 * Seeds universal abilities that are available to all players regardless of class.
 */
object UniversalAbilitySeed {

    const val BASIC_ATTACK_ID = "universal-basic-attack"
    const val AID_ABILITY_ID = "universal-aid"
    const val DRAG_ABILITY_ID = "universal-drag"
    const val TRACK_ABILITY_ID = "universal-track"
    const val HIDE_ABILITY_ID = "universal-hide"
    const val SNEAK_ABILITY_ID = "universal-sneak"
    const val CHARM_ABILITY_ID = "universal-charm"

    /**
     * Seeds universal abilities if they don't exist.
     */
    fun seedIfEmpty() {
        seedBasicAttack()
        seedAidAbility()
        seedDragAbility()
        seedTrackAbility()
        seedHideAbility()
        seedSneakAbility()
        seedCharmAbility()
    }

    private fun seedBasicAttack() {
        if (AbilityRepository.findById(BASIC_ATTACK_ID) != null) {
            return
        }

        // Basic melee attack - available to everyone
        // Using "item" abilityType to preserve manual cost (combat type auto-calculates)
        AbilityRepository.create(
            Ability(
                id = BASIC_ATTACK_ID,
                name = "Attack",
                description = "A basic melee attack using your equipped weapon or bare hands. Deals damage based on your strength and weapon.",
                classId = null,  // Universal - available to all classes
                abilityType = "item",  // Use "item" to preserve manual stamina cost
                targetType = "single_enemy",
                range = 5,  // Melee range
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 0,  // Damage comes from player's baseDamage stat (calculated from STR + weapon)
                effects = """[{"type":"damage","source":"baseDamage"}]""",
                powerCost = 0,
                staminaCost = 2  // Low stamina cost for basic attack
            )
        )
        println("Seeded basic attack ability")
    }

    private fun seedAidAbility() {
        if (AbilityRepository.findById(AID_ABILITY_ID) != null) {
            return
        }

        // Aid - stabilize a downed ally, bringing them to 1 HP
        AbilityRepository.create(
            Ability(
                id = AID_ABILITY_ID,
                name = "Aid",
                description = "Stabilize a downed ally, bringing them back to consciousness with 1 HP. Can only be used on downed players.",
                classId = null,  // Universal - available to all classes
                abilityType = "item",  // Use "item" to preserve manual cost
                targetType = "single_ally_downed",  // Special target type for downed allies only
                range = 5,  // Melee range
                cooldownType = "rounds",
                cooldownRounds = 2,  // Can't spam aid
                baseDamage = 0,
                effects = """[{"type":"aid"}]""",  // Special effect type handled by CombatService
                powerCost = 0,
                staminaCost = 3  // Costs stamina to help someone up
            )
        )
        println("Seeded aid ability")
    }

    private fun seedDragAbility() {
        if (AbilityRepository.findById(DRAG_ABILITY_ID) != null) {
            return
        }

        // Drag - move a downed ally to an adjacent location
        AbilityRepository.create(
            Ability(
                id = DRAG_ABILITY_ID,
                name = "Drag",
                description = "Drag a downed ally to safety. Both you and the ally will move to an adjacent location. Can only be used on downed players.",
                classId = null,  // Universal - available to all classes
                abilityType = "item",  // Use "item" to preserve manual cost
                targetType = "single_ally_downed",  // Special target type for downed allies only
                range = 5,  // Melee range
                cooldownType = "rounds",
                cooldownRounds = 1,
                baseDamage = 0,
                effects = """[{"type":"drag"}]""",  // Special effect type handled by CombatService
                powerCost = 0,
                staminaCost = 5  // Costs more stamina to drag someone
            )
        )
        println("Seeded drag ability")
    }

    private fun seedTrackAbility() {
        if (AbilityRepository.findById(TRACK_ABILITY_ID) != null) {
            return
        }

        // Track - detect trails of players and creatures who passed through
        AbilityRepository.create(
            Ability(
                id = TRACK_ABILITY_ID,
                name = "Track",
                description = "Search for tracks and trails left by creatures and players who passed through this area. Wisdom and tracker-type classes improve detection. Fresher trails are easier to find.",
                classId = null,  // Universal - available to all classes
                abilityType = "utility",
                targetType = "self",  // Area search centered on self
                range = 0,
                cooldownType = "short",
                cooldownRounds = 0,  // No combat cooldown, but has minor stamina cost
                baseDamage = 0,
                effects = """[{"type":"track"}]""",
                powerCost = 0,
                staminaCost = 2  // Small stamina cost to search for tracks
            )
        )
        println("Seeded track ability")
    }

    private fun seedHideAbility() {
        if (AbilityRepository.findById(HIDE_ABILITY_ID) != null) {
            return
        }

        // Hide - become invisible in current location
        AbilityRepository.create(
            Ability(
                id = HIDE_ABILITY_ID,
                name = "Hide",
                description = "Slip into the shadows and become hidden. Other characters must pass perception checks to notice you. Cannot be used in combat. Moving while hidden breaks the effect - use Sneak to move stealthily.",
                classId = null,  // Universal - available to all classes
                abilityType = "utility",
                targetType = "self",
                range = 0,
                cooldownType = "short",
                cooldownRounds = 0,
                baseDamage = 0,
                effects = """[{"type":"hide"}]""",
                powerCost = 0,
                staminaCost = 3  // Costs stamina to find a good hiding spot
            )
        )
        println("Seeded hide ability")
    }

    private fun seedSneakAbility() {
        if (AbilityRepository.findById(SNEAK_ABILITY_ID) != null) {
            return
        }

        // Sneak - move stealthily between locations
        AbilityRepository.create(
            Ability(
                id = SNEAK_ABILITY_ID,
                name = "Sneak",
                description = "Begin moving stealthily. You can move between locations while remaining hidden from others. Dexterity and rogue-type classes improve success. Heavy armor penalizes stealth. Cannot be used in combat.",
                classId = null,  // Universal - available to all classes
                abilityType = "utility",
                targetType = "self",
                range = 0,
                cooldownType = "short",
                cooldownRounds = 0,
                baseDamage = 0,
                effects = """[{"type":"sneak"}]""",
                powerCost = 0,
                staminaCost = 2  // Ongoing cost per movement handled elsewhere
            )
        )
        println("Seeded sneak ability")
    }

    private fun seedCharmAbility() {
        if (AbilityRepository.findById(CHARM_ABILITY_ID) != null) {
            return
        }

        // Charm - befriend a creature to fight alongside you
        AbilityRepository.create(
            Ability(
                id = CHARM_ABILITY_ID,
                name = "Charm",
                description = "Attempt to befriend a creature using your charisma and persuasion. Charmed creatures will follow and fight alongside you. Charisma and bard-type classes improve success. Some creatures are immune to charm. Cannot be used in combat.",
                classId = null,  // Universal - available to all classes
                abilityType = "utility",
                targetType = "single_enemy",  // Target a creature
                range = 30,  // Short range, needs to be close
                cooldownType = "long",  // Significant cooldown
                cooldownRounds = 0,
                baseDamage = 0,
                effects = """[{"type":"charm"}]""",
                powerCost = 0,
                staminaCost = 5,  // Mental effort is taxing
                manaCost = 5     // Also costs some mana
            )
        )
        println("Seeded charm ability")
    }
}
