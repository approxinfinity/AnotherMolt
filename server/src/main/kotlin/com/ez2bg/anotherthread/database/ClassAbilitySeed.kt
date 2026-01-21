package com.ez2bg.anotherthread.database

/**
 * Seed data for character classes and abilities.
 * Call seedClassesAndAbilities() on server startup to populate initial data.
 */
object ClassAbilitySeed {

    /**
     * Seeds the database with default character classes and abilities if they don't exist.
     */
    fun seedIfEmpty() {
        // Only seed if no classes exist
        if (CharacterClassRepository.findAll().isNotEmpty()) {
            return
        }

        // Create the two base classes: Spellcaster and Martial
        val spellcasterClass = CharacterClass(
            name = "Spellcaster",
            description = "A master of arcane or divine magic, capable of bending reality through spells. Spellcasters rely on intelligence or wisdom to fuel their magical abilities.",
            isSpellcaster = true,
            hitDie = 6,
            primaryAttribute = "intelligence",
            isPublic = true
        )
        val createdSpellcaster = CharacterClassRepository.create(spellcasterClass)

        val martialClass = CharacterClass(
            name = "Martial",
            description = "A trained warrior who excels in physical combat. Martial classes rely on strength, dexterity, and endurance to overcome their foes through skill and prowess.",
            isSpellcaster = false,
            hitDie = 10,
            primaryAttribute = "strength",
            isPublic = true
        )
        val createdMartial = CharacterClassRepository.create(martialClass)

        // Create the 10 unique spells for Spellcaster
        val spells = listOf(
            Ability(
                name = "Arcane Anchor",
                description = "Creates a 20ft radius zone that prevents teleportation, dimensional travel, and forced movement for 1 minute. Enemies within cannot blink, misty step, or be pushed/pulled.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "area",
                range = 60,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"control","effect":"prevent_movement","duration":10}]"""
            ),
            Ability(
                name = "Soul Tether",
                description = "Links your life force to an ally. For 3 rounds, damage taken by the target is split 50/50 with you. The tether breaks if either party moves more than 60ft apart.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "single_ally",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                effects = """[{"type":"protection","effect":"damage_split","ratio":0.5,"duration":3}]"""
            ),
            Ability(
                name = "Temporal Echo",
                description = "Record your current position, health, and status effects. Within 2 rounds, you may return to this exact state as a reaction, undoing any damage or conditions suffered.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"utility","effect":"save_state","duration":2}]"""
            ),
            Ability(
                name = "Mind Mirror",
                description = "Creates an illusion of the target's greatest fear. The target must succeed on a Wisdom save or spend their next turn attacking the illusion, ignoring all other threats.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "single_enemy",
                range = 60,
                cooldownType = "medium",
                cooldownRounds = 3,
                effects = """[{"type":"condition","condition":"fear","duration":1,"saveType":"wisdom"}]"""
            ),
            Ability(
                name = "Elemental Conversion",
                description = "Permanently transforms terrain in a 30ft line. Water becomes ice, stone becomes sand, earth becomes mud. The changed terrain persists until dispelled or manually reversed.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "area",
                range = 60,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"terrain","effect":"transform","permanent":true}]"""
            ),
            Ability(
                name = "Spirit Summon",
                description = "Summons an ancestral spirit that acts independently for 3 rounds. The spirit has its own turn and makes tactical decisions based on the battlefield, attacking enemies or protecting allies.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "area",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"summon","creatureType":"spirit","duration":3,"ai":true}]"""
            ),
            Ability(
                name = "Prescient Warning",
                description = "Divine enemy intentions for the next round. All allies within 30ft learn what enemies plan to do and gain advantage on saving throws against those enemies until your next turn.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "all_allies",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                effects = """[{"type":"buff","stat":"saves","bonus":"advantage","duration":1}]"""
            ),
            Ability(
                name = "Mana Siphon",
                description = "Drain magical energy from an enemy spellcaster. The target loses their next spell slot, and you gain a temporary spell slot or ability charge that lasts until your next rest.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "single_enemy",
                range = 60,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"drain","resource":"spell_slot","transfer":true}]"""
            ),
            Ability(
                name = "Gravity Well",
                description = "Creates a 15ft radius zone of altered gravity at a point you can see. Enemies are pulled 10ft toward the center at the start of their turn, and movement in the zone costs double.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "area",
                range = 60,
                cooldownType = "medium",
                cooldownRounds = 3,
                effects = """[{"type":"control","effect":"pull","distance":10},{"type":"terrain","effect":"difficult","multiplier":2}]"""
            ),
            Ability(
                name = "Life Bloom",
                description = "Creates a 20ft healing zone centered on you that persists for 3 rounds. Each round, allies in the zone heal 1d8 HP, while undead creatures take 1d8 radiant damage.",
                classId = createdSpellcaster.id,
                abilityType = "spell",
                targetType = "area",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"heal","diceCount":1,"diceSides":8,"duration":3},{"type":"damage","damageType":"radiant","diceCount":1,"diceSides":8,"targetType":"undead"}]"""
            )
        )

        spells.forEach { AbilityRepository.create(it) }

        // Create the 10 unique abilities for Martial
        val abilities = listOf(
            Ability(
                name = "Tactical Assessment",
                description = "Spend your action studying a single enemy. You learn their weaknesses, resistances, and intended next action. Your next attack against them has advantage and deals bonus damage.",
                classId = createdMartial.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 60,
                cooldownType = "short",
                cooldownRounds = 1,
                effects = """[{"type":"utility","effect":"reveal_info"},{"type":"buff","stat":"attack","bonus":"advantage","duration":1}]"""
            ),
            Ability(
                name = "Shield Wall",
                description = "Enter a defensive stance protecting yourself and adjacent allies. Everyone gains damage reduction, but you cannot take attack actions while maintaining the wall. Toggle on/off as a bonus action.",
                classId = createdMartial.id,
                abilityType = "combat",
                targetType = "area",
                range = 5,
                cooldownType = "none",
                cooldownRounds = 0,
                effects = """[{"type":"buff","stat":"damage_reduction","bonus":3,"aoe":true,"toggle":true}]"""
            ),
            Ability(
                name = "Precision Strike",
                description = "A calculated attack that finds gaps in armor. This attack ignores all damage reduction and armor bonuses, dealing reduced base damage but bypassing defenses entirely.",
                classId = createdMartial.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 3,
                effects = """[{"type":"damage","modifier":-2,"ignoreArmor":true,"ignoreDR":true}]"""
            ),
            Ability(
                name = "Shadowstep",
                description = "Instantly move to any shadow within 30ft. If you attack from hiding after shadowstepping, you deal an extra 2d6 damage. Requires dim light or darkness to use.",
                classId = createdMartial.id,
                abilityType = "utility",
                targetType = "self",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                effects = """[{"type":"movement","movementType":"teleport","distance":30,"requirement":"shadow"},{"type":"damage","diceCount":2,"diceSides":6,"condition":"hidden"}]"""
            ),
            Ability(
                name = "Battle Cry",
                description = "Let loose a terrifying war cry. All enemies within 30ft must make a Wisdom save or suffer disadvantage on attacks for 2 rounds. Enemies already frightened must flee.",
                classId = createdMartial.id,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"debuff","stat":"attacks","penalty":"disadvantage","duration":2,"saveType":"wisdom"}]"""
            ),
            Ability(
                name = "Vital Strike",
                description = "Target a specific body part with surgical precision. Choose to blind (eyes), disarm (hands), or slow (legs) your target on a successful hit instead of dealing normal damage.",
                classId = createdMartial.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 3,
                effects = """[{"type":"condition","options":["blind","disarm","slow"],"duration":2}]"""
            ),
            Ability(
                name = "Second Wind",
                description = "Draw upon your reserves to heal yourself. Recover 25% of your maximum HP and remove one negative condition. Cannot be used while at full health.",
                classId = createdMartial.id,
                abilityType = "utility",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"heal","percentage":25},{"type":"cleanse","count":1}]"""
            ),
            Ability(
                name = "Riposte",
                description = "When an enemy misses you with a melee attack, you may use your reaction to immediately counter-attack. This counter deals bonus damage equal to your proficiency bonus.",
                classId = createdMartial.id,
                abilityType = "passive",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "short",
                cooldownRounds = 1,
                effects = """[{"type":"damage","trigger":"enemy_miss","bonusDamage":"proficiency"}]"""
            ),
            Ability(
                name = "Hunter's Mark",
                description = "Mark a target as your quarry. All your attacks against the marked target deal an extra 1d6 damage. The mark lasts until the target dies or you mark a new target.",
                classId = createdMartial.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 90,
                cooldownType = "none",
                cooldownRounds = 0,
                effects = """[{"type":"damage","diceCount":1,"diceSides":6,"persistent":true}]"""
            ),
            Ability(
                name = "Iron Will",
                description = "Steel your mind against mental assault. For 3 rounds, you are immune to fear, charm, and mind control effects. You may activate this ability even while affected by such conditions.",
                classId = createdMartial.id,
                abilityType = "utility",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"immunity","conditions":["fear","charm","dominate"],"duration":3,"breakOnUse":true}]"""
            )
        )

        abilities.forEach { AbilityRepository.create(it) }

        println("Seeded ${spells.size} spells and ${abilities.size} abilities")
    }
}
