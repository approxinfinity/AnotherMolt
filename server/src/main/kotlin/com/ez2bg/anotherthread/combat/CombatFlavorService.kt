package com.ez2bg.anotherthread.combat

/**
 * Generates flavor text for combat messages based on weapon type and creature characteristics.
 * Only personalizes the generic "Attack" ability — named abilities keep their own flavor.
 */
object CombatFlavorService {

    private data class WeaponCategory(
        val patterns: List<String>,
        val verbs: List<String>
    )

    private val weaponCategories = listOf(
        WeaponCategory(
            listOf("sword", "blade", "cleaver", "scimitar", "saber", "rapier", "cutlass"),
            listOf("slashes", "cuts into", "carves into", "cleaves into", "hacks at", "slices into")
        ),
        WeaponCategory(
            listOf("dagger", "knife", "dirk", "stiletto", "shiv"),
            listOf("stabs", "jabs", "pierces", "thrusts into", "drives into")
        ),
        WeaponCategory(
            listOf("axe", "hatchet"),
            listOf("chops into", "hews into", "cleaves into", "hacks at", "splits into")
        ),
        WeaponCategory(
            listOf("mace", "club", "hammer", "maul", "flail", "morningstar"),
            listOf("bashes", "smashes", "crushes", "pounds", "hammers", "slams into")
        ),
        WeaponCategory(
            listOf("bow", "crossbow"),
            listOf("shoots", "fires an arrow at", "looses a bolt at", "pierces")
        ),
        WeaponCategory(
            listOf("staff", "rod", "wand", "scepter", "stave"),
            listOf("strikes", "whacks", "cracks", "smacks", "swings at")
        ),
        WeaponCategory(
            listOf("spear", "lance", "pike", "halberd", "trident", "javelin"),
            listOf("thrusts at", "stabs", "pierces", "jabs", "skewers")
        )
    )

    private val unarmedVerbs = listOf("punches", "strikes", "kicks", "hits", "pummels")

    private data class CreaturePattern(
        val nameContains: List<String>,
        val verbs: List<String>
    )

    private val creaturePatterns = listOf(
        CreaturePattern(
            listOf("goblin", "kobold", "hobgoblin"),
            listOf("slashes at", "stabs at", "swipes at", "cuts at", "hacks at")
        ),
        CreaturePattern(
            listOf("troll", "ogre", "giant"),
            listOf("smashes", "crushes", "pounds", "batters", "clubs")
        ),
        CreaturePattern(
            listOf("spider", "scorpion", "centipede"),
            listOf("bites", "lunges at", "snaps at", "strikes at")
        ),
        CreaturePattern(
            listOf("wolf", "dog", "bear", "beast", "cat", "lion", "tiger"),
            listOf("claws at", "bites", "mauls", "tears at", "rends")
        ),
        CreaturePattern(
            listOf("skeleton", "zombie", "mummy"),
            listOf("strikes at", "claws at", "slashes at", "swipes at")
        ),
        CreaturePattern(
            listOf("orc", "berserker"),
            listOf("hacks at", "chops at", "slashes", "cleaves at")
        ),
        CreaturePattern(
            listOf("rat", "rodent"),
            listOf("bites", "gnaws at", "scratches")
        ),
        CreaturePattern(
            listOf("dragon", "drake", "wyrm", "wyvern"),
            listOf("claws at", "bites", "rends", "tears at", "snaps at")
        ),
        CreaturePattern(
            listOf("snake", "serpent", "naga"),
            listOf("strikes at", "bites", "lunges at", "lashes at")
        ),
        CreaturePattern(
            listOf("ghoul", "wraith", "specter", "ghost", "vampire"),
            listOf("claws at", "drains", "strikes at", "lashes at")
        ),
        CreaturePattern(
            listOf("ooze", "slime", "jelly", "cube"),
            listOf("engulfs", "lashes at", "slams into", "oozes over")
        )
    )

    private val defaultCreatureVerbs = listOf("attacks", "strikes at", "lashes out at", "assails")

    /**
     * Generate a personalized player attack message.
     * Returns null for non-Attack abilities (caller falls back to default).
     */
    fun playerAttackMessage(
        actorName: String,
        targetName: String,
        abilityName: String,
        weaponName: String?,
        damage: Int,
        wasCritical: Boolean,
        wasGlancing: Boolean,
        hpRemaining: String
    ): String? {
        if (abilityName != "Attack") return null

        val weapon = weaponName ?: "fists"
        val verb = weaponVerb(weaponName)

        return when {
            wasCritical -> "CRITICAL! $actorName's $weapon $verb deep into $targetName for $damage damage!$hpRemaining"
            wasGlancing -> "$actorName's $weapon barely grazes $targetName for $damage damage.$hpRemaining"
            else -> "$actorName $verb $targetName with $weapon for $damage damage!$hpRemaining"
        }
    }

    /**
     * Generate a personalized player miss message.
     * Returns null for non-Attack abilities.
     */
    fun playerMissMessage(
        actorName: String,
        targetName: String,
        abilityName: String,
        weaponName: String?
    ): String? {
        if (abilityName != "Attack") return null

        val weapon = weaponName ?: "fists"
        val phrases = listOf(
            "$actorName swings $weapon at $targetName but misses!",
            "$actorName's $weapon fails to connect with $targetName!",
            "$targetName dodges $actorName's $weapon!"
        )
        return phrases.random()
    }

    /**
     * Generate a personalized creature attack message.
     */
    fun creatureAttackMessage(
        creatureName: String,
        targetName: String,
        damage: Int,
        wasCritical: Boolean,
        wasGlancing: Boolean
    ): String {
        val verb = creatureVerb(creatureName)

        return when {
            wasCritical -> "CRITICAL! $creatureName $verb $targetName for $damage damage!"
            wasGlancing -> "$creatureName's attack barely grazes $targetName for $damage damage."
            else -> "$creatureName $verb $targetName for $damage damage!"
        }
    }

    /**
     * Generate a creature miss message.
     */
    fun creatureMissMessage(creatureName: String, targetName: String): String {
        val phrases = listOf(
            "$creatureName's attack misses $targetName!",
            "$creatureName lunges but $targetName dodges!",
            "$targetName evades $creatureName's strike!"
        )
        return phrases.random()
    }

    private fun weaponVerb(weaponName: String?): String {
        if (weaponName == null) return unarmedVerbs.random()

        val lower = weaponName.lowercase()
        for (cat in weaponCategories) {
            if (cat.patterns.any { lower.contains(it) }) {
                return cat.verbs.random()
            }
        }
        return listOf("strikes", "attacks", "hits").random()
    }

    private fun creatureVerb(creatureName: String): String {
        val lower = creatureName.lowercase()
        for (pattern in creaturePatterns) {
            if (pattern.nameContains.any { lower.contains(it) }) {
                return pattern.verbs.random()
            }
        }
        return defaultCreatureVerbs.random()
    }
}
