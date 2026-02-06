package com.ez2bg.anotherthread.database

/**
 * Seeds universal abilities that are available to all players regardless of class.
 */
object UniversalAbilitySeed {

    const val BASIC_ATTACK_ID = "universal-basic-attack"

    /**
     * Seeds universal abilities if they don't exist.
     */
    fun seedIfEmpty() {
        // Check if basic attack already exists
        if (AbilityRepository.findById(BASIC_ATTACK_ID) != null) {
            return
        }

        // Basic melee attack - available to everyone
        AbilityRepository.create(
            Ability(
                id = BASIC_ATTACK_ID,
                name = "Attack",
                description = "A basic melee attack using your equipped weapon or bare hands. Deals damage based on your strength and weapon.",
                classId = null,  // Universal - available to all classes
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,  // Melee range
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 0,  // Damage comes from player's baseDamage stat (calculated from STR + weapon)
                effects = """[{"type":"damage","source":"baseDamage"}]""",
                staminaCost = 2  // Low stamina cost for basic attack
            )
        )

        println("Seeded universal abilities")
    }
}
