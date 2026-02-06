package com.ez2bg.anotherthread.database

/**
 * Seeds universal abilities that are available to all players regardless of class.
 */
object UniversalAbilitySeed {

    const val BASIC_ATTACK_ID = "universal-basic-attack"
    const val AID_ABILITY_ID = "universal-aid"
    const val DRAG_ABILITY_ID = "universal-drag"

    /**
     * Seeds universal abilities if they don't exist.
     */
    fun seedIfEmpty() {
        seedBasicAttack()
        seedAidAbility()
        seedDragAbility()
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
}
