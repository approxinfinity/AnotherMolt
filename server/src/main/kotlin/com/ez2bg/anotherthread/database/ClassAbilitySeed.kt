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
        val existingClasses = CharacterClassRepository.findAll()
        val existingNames = existingClasses.map { it.name }.toSet()

        // Seed base classes if none exist
        if (existingClasses.isEmpty()) {
            seedBaseClasses()
        }

        // Always try to add new classes that don't exist yet
        seedAdditionalClasses(existingNames)
    }

    /**
     * Seeds the original Spellcaster and Martial classes
     */
    private fun seedBaseClasses() {

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

        println("Seeded ${spells.size} spells and ${abilities.size} martial abilities")
    }

    /**
     * Seeds additional classes (Scoundrel, Bard, Alchemist, Ranger) if they don't exist
     */
    private fun seedAdditionalClasses(existingNames: Set<String>) {
        var seededCount = 0

        // Create Scoundrel class - a cunning rogue-type
        if ("Scoundrel" !in existingNames) {
            val scoundrelClass = CharacterClass(
                name = "Scoundrel",
                description = "A cunning trickster who relies on deception, quick reflexes, and dirty tricks to gain the upper hand. Scoundrels excel at exploiting weaknesses and escaping dangerous situations.",
                isSpellcaster = false,
                hitDie = 8,
                primaryAttribute = "dexterity",
                isPublic = true
            )
            val createdScoundrel = CharacterClassRepository.create(scoundrelClass)

        val scoundrelAbilities = listOf(
            Ability(
                name = "Cheap Shot",
                description = "A dirty blow aimed at sensitive areas. Deal extra damage and the target must make a Constitution save or be stunned until the end of their next turn.",
                classId = createdScoundrel.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 15,
                effects = """[{"type":"condition","condition":"stunned","duration":1,"saveType":"constitution"}]"""
            ),
            Ability(
                name = "Smoke Bomb",
                description = "Throw a smoke bomb creating a 15ft radius cloud of obscuring smoke. All creatures in the area are heavily obscured. The smoke lasts for 2 rounds or until dispersed by strong wind.",
                classId = createdScoundrel.id,
                abilityType = "utility",
                targetType = "area",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                durationRounds = 2,
                effects = """[{"type":"terrain","effect":"obscured","radius":15}]"""
            ),
            Ability(
                name = "Backstab",
                description = "When attacking from hiding or with advantage, deal massive bonus damage. The target must not be aware of your presence for this ability to work.",
                classId = createdScoundrel.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 35,
                effects = """[{"type":"damage","condition":"hidden_or_advantage","multiplier":2}]"""
            ),
            Ability(
                name = "Evasive Roll",
                description = "As a reaction when targeted by an attack, roll up to 15ft away. If you end in cover, the attack automatically misses. You cannot use this ability if restrained or grappled.",
                classId = createdScoundrel.id,
                abilityType = "utility",
                targetType = "self",
                range = 15,
                cooldownType = "short",
                cooldownRounds = 1,
                effects = """[{"type":"movement","trigger":"targeted","distance":15,"negateAttack":true}]"""
            ),
            Ability(
                name = "Poison Blade",
                description = "Coat your weapon with a fast-acting poison. Your next 3 attacks apply poison that deals damage over time for 2 rounds. Targets can make a Constitution save to halve the poison damage.",
                classId = createdScoundrel.id,
                abilityType = "utility",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 0,
                baseDamage = 8,
                durationRounds = 2,
                effects = """[{"type":"buff","effect":"poison_weapon","charges":3},{"type":"dot","damageType":"poison","saveType":"constitution"}]"""
            ),
            Ability(
                name = "Pickpocket",
                description = "Attempt to steal a small item from a target within reach. On success, you may take a potion, key, or similar small object. The target is unaware unless they succeed on a Perception check.",
                classId = createdScoundrel.id,
                abilityType = "utility",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "short",
                cooldownRounds = 1,
                effects = """[{"type":"utility","effect":"steal_item","size":"small"}]"""
            ),
            Ability(
                name = "Feint",
                description = "Make a deceptive move that tricks your opponent. Your next attack against the target has advantage, and the target cannot use reactions against you until the start of your next turn.",
                classId = createdScoundrel.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "short",
                cooldownRounds = 1,
                effects = """[{"type":"buff","stat":"attack","bonus":"advantage"},{"type":"debuff","effect":"no_reactions","duration":1}]"""
            ),
            Ability(
                name = "Disappearing Act",
                description = "Vanish from sight in plain view. You become invisible for 1 round or until you attack. While invisible, you can move through enemy spaces and ignore opportunity attacks.",
                classId = createdScoundrel.id,
                abilityType = "utility",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 0,
                durationRounds = 1,
                effects = """[{"type":"condition","condition":"invisible","duration":1,"breakOnAttack":true}]"""
            ),
            Ability(
                name = "Exploit Weakness",
                description = "Study a target's fighting style and exploit their openings. For the next 3 rounds, you deal extra damage to that target and they have disadvantage on attacks against you.",
                classId = createdScoundrel.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 0,
                baseDamage = 10,
                durationRounds = 3,
                effects = """[{"type":"debuff","stat":"attacks","penalty":"disadvantage","duration":3}]"""
            ),
            Ability(
                name = "Slippery",
                description = "You're incredibly hard to pin down. You automatically succeed on checks to escape grapples and can move through spaces occupied by creatures of any size.",
                classId = createdScoundrel.id,
                abilityType = "passive",
                targetType = "self",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                effects = """[{"type":"immunity","conditions":["grappled"]},{"type":"movement","effect":"squeeze"}]"""
            )
        )

            scoundrelAbilities.forEach { AbilityRepository.create(it) }
            seededCount++
            println("Seeded Scoundrel class with ${scoundrelAbilities.size} abilities")
        }

        // Create Bard class - a charismatic support/utility caster
        if ("Bard" !in existingNames) {
            val bardClass = CharacterClass(
                name = "Bard",
                description = "A versatile performer who weaves magic through music, stories, and sheer force of personality. Bards inspire allies, confound enemies, and always seem to know the right thing to say.",
                isSpellcaster = true,
                hitDie = 8,
                primaryAttribute = "charisma",
                isPublic = true
            )
            val createdBard = CharacterClassRepository.create(bardClass)

        val bardAbilities = listOf(
            Ability(
                name = "Inspiring Melody",
                description = "Play an uplifting tune that bolsters your allies. All allies within 30ft gain a bonus to their next attack roll, ability check, or saving throw. The inspiration lasts until used or 10 minutes pass.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "all_allies",
                range = 30,
                cooldownType = "short",
                cooldownRounds = 1,
                effects = """[{"type":"buff","stat":"rolls","bonus":4,"charges":1}]"""
            ),
            Ability(
                name = "Vicious Mockery",
                description = "Unleash a string of insults laced with subtle enchantments. The target takes psychic damage and has disadvantage on their next attack roll. Creatures immune to charm are unaffected.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "single_enemy",
                range = 60,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 12,
                effects = """[{"type":"debuff","stat":"attack","penalty":"disadvantage","duration":1}]"""
            ),
            Ability(
                name = "Song of Rest",
                description = "Perform a soothing melody during a short rest. All allies who can hear you regain additional hit points when they spend hit dice to heal. Also removes one level of exhaustion.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "all_allies",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 0,
                effects = """[{"type":"heal","bonus":8,"trigger":"short_rest"},{"type":"cleanse","condition":"exhaustion","count":1}]"""
            ),
            Ability(
                name = "Cutting Words",
                description = "As a reaction, use your wit to distract an enemy making an attack roll, ability check, or damage roll. Subtract your charisma modifier from their roll.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "single_enemy",
                range = 60,
                cooldownType = "short",
                cooldownRounds = 1,
                effects = """[{"type":"debuff","stat":"rolls","penalty":"charisma","trigger":"reaction"}]"""
            ),
            Ability(
                name = "Charm Person",
                description = "Attempt to charm a humanoid you can see. If they fail a Wisdom save, they regard you as a friendly acquaintance for 1 hour. They know they were charmed when the spell ends.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "single_enemy",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                durationRounds = 10,
                effects = """[{"type":"condition","condition":"charmed","duration":10,"saveType":"wisdom"}]"""
            ),
            Ability(
                name = "Countercharm",
                description = "Begin a performance that protects against mental influence. For the next 3 rounds, you and allies within 30ft have advantage on saving throws against being frightened or charmed.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "all_allies",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                durationRounds = 3,
                effects = """[{"type":"buff","stat":"saves","conditions":["fear","charm"],"bonus":"advantage"}]"""
            ),
            Ability(
                name = "Hypnotic Pattern",
                description = "Create a twisting pattern of colors in a 30ft cube. Creatures that can see it must make a Wisdom save or become charmed and incapacitated for 2 rounds. Taking damage ends the effect.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "area",
                range = 120,
                cooldownType = "long",
                cooldownRounds = 0,
                durationRounds = 2,
                effects = """[{"type":"condition","conditions":["charmed","incapacitated"],"saveType":"wisdom","breakOnDamage":true}]"""
            ),
            Ability(
                name = "Healing Word",
                description = "Speak a word of power that restores vitality to an ally you can see. This can be cast as a bonus action, allowing you to still attack or cast another cantrip.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "single_ally",
                range = 60,
                cooldownType = "short",
                cooldownRounds = 1,
                baseDamage = 0,
                effects = """[{"type":"heal","diceCount":1,"diceSides":6,"bonus":"charisma","bonusAction":true}]"""
            ),
            Ability(
                name = "Disguise Self",
                description = "Magically alter your appearance including clothing, armor, and features. You can appear up to 1 foot shorter or taller. The illusion lasts 1 hour or until dismissed.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "self",
                range = 0,
                cooldownType = "medium",
                cooldownRounds = 3,
                durationRounds = 10,
                effects = """[{"type":"utility","effect":"disguise","duration":10}]"""
            ),
            Ability(
                name = "Thunderwave",
                description = "A wave of thunderous force sweeps out from you. Each creature in a 15ft cube must make a Constitution save or take thunder damage and be pushed 10ft away from you.",
                classId = createdBard.id,
                abilityType = "spell",
                targetType = "area",
                range = 0,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 20,
                effects = """[{"type":"movement","effect":"push","distance":10,"saveType":"constitution"}]"""
            )
        )

            bardAbilities.forEach { AbilityRepository.create(it) }
            seededCount++
            println("Seeded Bard class with ${bardAbilities.size} abilities")
        }

        // Create Alchemist class - a potion and bomb specialist
        if ("Alchemist" !in existingNames) {
            val alchemistClass = CharacterClass(
                name = "Alchemist",
                description = "A master of potions, elixirs, and volatile concoctions. Alchemists use their knowledge of chemistry and magic to create powerful effects, from healing draughts to explosive bombs.",
                isSpellcaster = true,
                hitDie = 8,
                primaryAttribute = "intelligence",
                isPublic = true
            )
            val createdAlchemist = CharacterClassRepository.create(alchemistClass)

        val alchemistAbilities = listOf(
            Ability(
                name = "Alchemist Fire",
                description = "Throw a flask of volatile liquid that ignites on impact. Deals fire damage in a 10ft radius and sets the area ablaze for 2 rounds. Creatures starting their turn in the fire take additional damage.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "area",
                range = 60,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 18,
                durationRounds = 2,
                effects = """[{"type":"terrain","effect":"fire","radius":10},{"type":"dot","damageType":"fire"}]"""
            ),
            Ability(
                name = "Healing Draught",
                description = "Quickly mix and administer a healing potion to yourself or an adjacent ally. The potion restores health immediately and provides minor regeneration for 2 rounds.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "single_ally",
                range = 5,
                cooldownType = "short",
                cooldownRounds = 1,
                durationRounds = 2,
                effects = """[{"type":"heal","diceCount":2,"diceSides":8},{"type":"regeneration","value":3,"duration":2}]"""
            ),
            Ability(
                name = "Acid Flask",
                description = "Hurl a container of concentrated acid. The target takes immediate acid damage and their armor is corroded, reducing their AC by 2 until they take a short rest to clean it.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "single_enemy",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 15,
                effects = """[{"type":"debuff","stat":"ac","penalty":2,"duration":"until_rest"}]"""
            ),
            Ability(
                name = "Smoke Concoction",
                description = "Create a billowing cloud of alchemical smoke in a 20ft radius. The area is heavily obscured and creatures inside have disadvantage on Constitution saves.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "area",
                range = 60,
                cooldownType = "medium",
                cooldownRounds = 3,
                durationRounds = 3,
                effects = """[{"type":"terrain","effect":"obscured"},{"type":"debuff","stat":"saves","saveType":"constitution","penalty":"disadvantage"}]"""
            ),
            Ability(
                name = "Mutagen",
                description = "Drink a volatile mutagen that transforms your body. Choose enhanced strength (+4), dexterity (+4), or constitution (+4) for 5 rounds. When it wears off, you take damage equal to 10% of your max HP.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 0,
                durationRounds = 5,
                effects = """[{"type":"buff","stat":"choice","bonus":4,"duration":5},{"type":"damage","trigger":"end","percentage":10}]"""
            ),
            Ability(
                name = "Flash Powder",
                description = "Throw a packet of flash powder that explodes in brilliant light. All creatures in a 15ft cone must make a Dexterity save or be blinded for 1 round.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "area",
                range = 15,
                cooldownType = "short",
                cooldownRounds = 1,
                durationRounds = 1,
                effects = """[{"type":"condition","condition":"blinded","duration":1,"saveType":"dexterity"}]"""
            ),
            Ability(
                name = "Tanglefoot Bag",
                description = "Throw a bag of sticky adhesive that bursts on impact. Creatures in a 10ft radius must make a Strength save or have their movement reduced to 0 for 2 rounds.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "area",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                durationRounds = 2,
                effects = """[{"type":"condition","condition":"restrained","duration":2,"saveType":"strength"}]"""
            ),
            Ability(
                name = "Antidote",
                description = "Administer a universal antidote that neutralizes poisons and diseases. The target is cured of one poison or disease effect and gains immunity to that specific ailment for 1 hour.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "single_ally",
                range = 5,
                cooldownType = "short",
                cooldownRounds = 1,
                effects = """[{"type":"cleanse","conditions":["poisoned","diseased"]},{"type":"immunity","duration":10}]"""
            ),
            Ability(
                name = "Thunderstone",
                description = "Throw an alchemical stone that detonates with a thunderous crack. Deals thunder damage and all creatures within 15ft must make a Constitution save or be deafened for 2 rounds.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "area",
                range = 60,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 16,
                durationRounds = 2,
                effects = """[{"type":"condition","condition":"deafened","duration":2,"saveType":"constitution"}]"""
            ),
            Ability(
                name = "Philosopher's Stone Fragment",
                description = "Use a sliver of the legendary philosopher's stone. Once per day, you may either fully heal one ally, transmute a nonmagical object into gold, or deal massive damage to an undead or construct.",
                classId = createdAlchemist.id,
                abilityType = "spell",
                targetType = "single_ally",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 0,
                baseDamage = 50,
                effects = """[{"type":"choice","options":["full_heal","transmute_gold","damage_construct_undead"]}]"""
            )
        )

            alchemistAbilities.forEach { AbilityRepository.create(it) }
            seededCount++
            println("Seeded Alchemist class with ${alchemistAbilities.size} abilities")
        }

        // Create Ranger class - a wilderness and beast master
        if ("Ranger" !in existingNames) {
            val rangerClass = CharacterClass(
                name = "Ranger",
                description = "A skilled hunter and tracker at home in the wild. Rangers combine martial prowess with nature magic, excelling at ranged combat and surviving in harsh environments.",
                isSpellcaster = false,
                hitDie = 10,
                primaryAttribute = "dexterity",
                isPublic = true
            )
            val createdRanger = CharacterClassRepository.create(rangerClass)

        val rangerAbilities = listOf(
            Ability(
                name = "Favored Enemy",
                description = "You have studied a particular type of enemy extensively. Choose a creature type; you deal extra damage to that type and have advantage on Survival checks to track them.",
                classId = createdRanger.id,
                abilityType = "passive",
                targetType = "self",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 6,
                effects = """[{"type":"buff","effect":"favored_enemy","bonus_damage":6,"tracking":"advantage"}]"""
            ),
            Ability(
                name = "Volley",
                description = "Fire a rain of arrows at a point you can see. All creatures within 10ft of that point must make a Dexterity save or take full damage. Those who save take half damage.",
                classId = createdRanger.id,
                abilityType = "combat",
                targetType = "area",
                range = 150,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 25,
                effects = """[{"type":"damage","saveType":"dexterity","halfOnSave":true}]"""
            ),
            Ability(
                name = "Natural Explorer",
                description = "Your experience in the wilderness grants benefits when traveling. Your group cannot become lost, you remain alert to danger even when doing other activities, and you move stealthily at normal pace.",
                classId = createdRanger.id,
                abilityType = "passive",
                targetType = "all_allies",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                effects = """[{"type":"utility","effect":"no_lost"},{"type":"buff","effect":"travel_stealth"}]"""
            ),
            Ability(
                name = "Ensnaring Strike",
                description = "The next creature you hit with a weapon attack must make a Strength save or become restrained by magical vines. While restrained, they take damage at the start of each turn.",
                classId = createdRanger.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 8,
                durationRounds = 3,
                effects = """[{"type":"condition","condition":"restrained","saveType":"strength"},{"type":"dot","damageType":"piercing"}]"""
            ),
            Ability(
                name = "Multiattack",
                description = "You can attack twice instead of once when you take the Attack action. If both attacks hit the same target, deal bonus damage.",
                classId = createdRanger.id,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 10,
                effects = """[{"type":"attack","extra_attacks":1,"bonus_same_target":5}]"""
            ),
            Ability(
                name = "Pass Without Trace",
                description = "A veil of shadow and silence radiates from you. For 1 hour, you and each creature within 30ft has a +10 bonus to Stealth checks and can't be tracked except by magical means.",
                classId = createdRanger.id,
                abilityType = "spell",
                targetType = "all_allies",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 0,
                durationRounds = 10,
                effects = """[{"type":"buff","stat":"stealth","bonus":10},{"type":"utility","effect":"no_tracking"}]"""
            ),
            Ability(
                name = "Colossus Slayer",
                description = "Your tenacity can wear down the most potent foes. Once per turn, when you hit a creature that is below its hit point maximum, deal extra damage.",
                classId = createdRanger.id,
                abilityType = "passive",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 8,
                effects = """[{"type":"damage","condition":"target_damaged","diceCount":1,"diceSides":8}]"""
            ),
            Ability(
                name = "Escape the Horde",
                description = "Opportunity attacks against you are made with disadvantage. You can use a bonus action to take the Disengage action.",
                classId = createdRanger.id,
                abilityType = "passive",
                targetType = "self",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                effects = """[{"type":"buff","effect":"opp_attack_disadvantage"},{"type":"utility","effect":"bonus_disengage"}]"""
            ),
            Ability(
                name = "Spike Growth",
                description = "The ground in a 20ft radius becomes twisted with spikes and thorns. The area becomes difficult terrain and creatures moving through it take piercing damage for every 5ft traveled.",
                classId = createdRanger.id,
                abilityType = "spell",
                targetType = "area",
                range = 150,
                cooldownType = "long",
                cooldownRounds = 0,
                baseDamage = 2,
                durationRounds = 10,
                effects = """[{"type":"terrain","effect":"difficult"},{"type":"damage","trigger":"movement","per_5ft":true}]"""
            ),
            Ability(
                name = "Conjure Barrage",
                description = "Throw a nonmagical weapon or fire a piece of ammunition into the air to create a cone of identical weapons. Each creature in a 60ft cone must make a Dexterity save or take the weapon's damage.",
                classId = createdRanger.id,
                abilityType = "combat",
                targetType = "area",
                range = 60,
                cooldownType = "long",
                cooldownRounds = 0,
                baseDamage = 30,
                effects = """[{"type":"damage","saveType":"dexterity","shape":"cone","halfOnSave":true}]"""
            )
        )

            rangerAbilities.forEach { AbilityRepository.create(it) }
            seededCount++
            println("Seeded Ranger class with ${rangerAbilities.size} abilities")
        }

        if (seededCount > 0) {
            println("Seeded $seededCount additional classes")
        }
    }
}
