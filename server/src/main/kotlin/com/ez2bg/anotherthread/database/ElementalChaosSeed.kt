package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.ImageGenerationService
import org.slf4j.LoggerFactory

/**
 * Seed data for Elemental Chaos area - a themed zone featuring elemental creatures.
 * Inspired by classic D&D elemental monsters with appropriate abilities, items, and loot.
 */
object ElementalChaosSeed {
    private val log = LoggerFactory.getLogger(ElementalChaosSeed::class.java)

    // Attribution for tracking content source
    private const val ATTRIBUTION = "Classic D&D Elementals"

    // ============== CREATURE IDS ==============
    // Tier 1 (CR 1) - Levels 1-3
    const val FIRE_MEPHIT_ID = "creature-fire-mephit"
    const val ICE_MEPHIT_ID = "creature-ice-mephit"
    const val DUST_MEPHIT_ID = "creature-dust-mephit"
    const val STEAM_MEPHIT_ID = "creature-steam-mephit"

    // Tier 2 (CR 2-3) - Levels 4-6
    const val FIRE_ELEMENTAL_ID = "creature-fire-elemental"
    const val WATER_ELEMENTAL_ID = "creature-water-elemental"
    const val EARTH_ELEMENTAL_ID = "creature-earth-elemental"
    const val AIR_ELEMENTAL_ID = "creature-air-elemental"

    // Tier 3 (CR 4-5) - Levels 7-10
    const val MAGMA_ELEMENTAL_ID = "creature-magma-elemental"
    const val ICE_ELEMENTAL_ID = "creature-ice-elemental"
    const val MUD_ELEMENTAL_ID = "creature-mud-elemental"
    const val LIGHTNING_ELEMENTAL_ID = "creature-lightning-elemental"

    // Tier 4 (CR 6+) - Boss
    const val ELEMENTAL_PRINCE_ID = "creature-elemental-prince"

    // ============== ABILITY IDS ==============
    const val ABILITY_FIRE_FORM_ID = "ability-fire-form"
    const val ABILITY_WATER_FORM_ID = "ability-water-form"
    const val ABILITY_EARTH_FORM_ID = "ability-earth-form"
    const val ABILITY_AIR_FORM_ID = "ability-air-form"
    const val ABILITY_FIRE_BLAST_ID = "ability-fire-blast"
    const val ABILITY_FROST_BREATH_ELEM_ID = "ability-frost-breath-elemental"
    const val ABILITY_EARTHQUAKE_ID = "ability-earthquake"
    const val ABILITY_WHIRLWIND_ID = "ability-whirlwind"
    const val ABILITY_MAGMA_BURST_ID = "ability-magma-burst"
    const val ABILITY_FLASH_FREEZE_ID = "ability-flash-freeze"
    const val ABILITY_ENGULF_ID = "ability-engulf"
    const val ABILITY_CHAIN_LIGHTNING_ID = "ability-chain-lightning"
    const val ABILITY_ELEMENTAL_FURY_ID = "ability-elemental-fury"

    // ============== ITEM IDS ==============
    // Consumables
    const val FIRE_RESIST_POTION_ID = "item-fire-resist-potion"
    const val COLD_RESIST_POTION_ID = "item-cold-resist-potion"
    const val LIGHTNING_RESIST_POTION_ID = "item-lightning-resist-potion"
    const val ELEMENTAL_ESSENCE_ID = "item-elemental-essence"

    // Weapons
    const val BLAZING_SCIMITAR_ID = "item-blazing-scimitar"
    const val FROSTBRAND_ID = "item-frostbrand"
    const val EARTHBREAKER_HAMMER_ID = "item-earthbreaker-hammer"
    const val STORMBRINGER_SPEAR_ID = "item-stormbringer-spear"
    const val ELEMENTAL_STAFF_ID = "item-elemental-staff"

    // Armor
    const val FIRE_RESISTANT_ARMOR_ID = "item-fire-resistant-armor"
    const val COLD_RESISTANT_ARMOR_ID = "item-cold-resistant-armor"
    const val ARMOR_OF_ELEMENTAL_COMMAND_ID = "item-armor-of-elemental-command"
    const val BOOTS_OF_LEVITATION_ID = "item-boots-of-levitation"

    // Accessories
    const val RING_OF_FIRE_ELEMENTAL_COMMAND_ID = "item-ring-fire-elemental-command"
    const val RING_OF_WATER_ELEMENTAL_COMMAND_ID = "item-ring-water-elemental-command"
    const val CLOAK_OF_THE_WINDS_ID = "item-cloak-of-the-winds"
    const val AMULET_OF_ELEMENTAL_ABSORPTION_ID = "item-amulet-elemental-absorption"

    // Treasure
    const val FIRE_RUBY_ID = "item-fire-ruby"
    const val FROZEN_TEAR_ID = "item-frozen-tear"
    const val ELEMENTAL_GEM_ID = "item-elemental-gem"
    const val PRIMORDIAL_SHARD_ID = "item-primordial-shard"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_MEPHIT_ID = "loot-mephit"
    const val LOOT_TABLE_FIRE_ELEMENTAL_ID = "loot-fire-elemental"
    const val LOOT_TABLE_WATER_ELEMENTAL_ID = "loot-water-elemental"
    const val LOOT_TABLE_EARTH_ELEMENTAL_ID = "loot-earth-elemental"
    const val LOOT_TABLE_AIR_ELEMENTAL_ID = "loot-air-elemental"
    const val LOOT_TABLE_GREATER_ELEMENTAL_ID = "loot-greater-elemental"
    const val LOOT_TABLE_ELEMENTAL_PRINCE_ID = "loot-elemental-prince"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (FIRE_MEPHIT_ID !in existingCreatures) {
            seedCreatureAbilities()
            seedItems()
            seedLootTables()
            seedCreatures()
            println("Seeded Elemental Chaos content (D&D-inspired elementals)")
        }
    }

    private fun seedCreatureAbilities() {
        val abilities = listOf(
            // Fire Form - passive damage to attackers
            Ability(
                id = ABILITY_FIRE_FORM_ID,
                name = "Fire Form",
                description = "The elemental's body is living flame. Creatures that touch it or strike it with melee attacks take fire damage.",
                classId = null,
                abilityType = "passive",
                targetType = "self",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 5,
                effects = """[{"type":"reflect","damageType":"fire","damage":5,"trigger":"melee_hit"}]""",
                attribution = ATTRIBUTION
            ),
            // Water Form - fluid evasion
            Ability(
                id = ABILITY_WATER_FORM_ID,
                name = "Water Form",
                description = "The elemental's fluid body flows around attacks, granting increased evasion and resistance to physical damage.",
                classId = null,
                abilityType = "buff",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 0,
                durationRounds = 3,
                effects = """[{"type":"buff","stat":"defense","bonus":6,"duration":3},{"type":"resistance","damageType":"physical","modifier":0.5,"duration":3}]""",
                attribution = ATTRIBUTION
            ),
            // Earth Form - damage reduction
            Ability(
                id = ABILITY_EARTH_FORM_ID,
                name = "Earth Form",
                description = "The elemental hardens its rocky body, becoming nearly impervious to harm but slowing its movement.",
                classId = null,
                abilityType = "buff",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 0,
                durationRounds = 3,
                effects = """[{"type":"buff","stat":"defense","bonus":10,"duration":3},{"type":"debuff","stat":"speed","penalty":2,"duration":3}]""",
                attribution = ATTRIBUTION
            ),
            // Air Form - speed and evasion
            Ability(
                id = ABILITY_AIR_FORM_ID,
                name = "Air Form",
                description = "The elemental becomes a swirling vortex, dramatically increasing its speed and making it difficult to hit.",
                classId = null,
                abilityType = "buff",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 0,
                durationRounds = 3,
                effects = """[{"type":"buff","stat":"speed","bonus":4,"duration":3},{"type":"buff","stat":"evasion","bonus":20,"duration":3}]""",
                attribution = ATTRIBUTION
            ),
            // Fire Blast - ranged fire attack
            Ability(
                id = ABILITY_FIRE_BLAST_ID,
                name = "Fire Blast",
                description = "Launches a searing bolt of fire at a target, dealing heavy fire damage and potentially igniting them.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 30,
                cooldownType = "short",
                cooldownRounds = 1,
                baseDamage = 14,
                effects = """[{"type":"damage","damageType":"fire","modifier":14},{"type":"dot","damageType":"fire","damage":3,"duration":2,"chance":0.5}]""",
                attribution = ATTRIBUTION
            ),
            // Frost Breath - cone cold attack
            Ability(
                id = ABILITY_FROST_BREATH_ELEM_ID,
                name = "Frost Breath",
                description = "Exhales a cone of freezing air that damages and slows all enemies caught in its path.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 20,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 12,
                durationRounds = 2,
                effects = """[{"type":"damage","damageType":"cold","modifier":12,"aoe":true},{"type":"condition","condition":"slowed","duration":2}]""",
                attribution = ATTRIBUTION
            ),
            // Earthquake - ground-shaking AOE
            Ability(
                id = ABILITY_EARTHQUAKE_ID,
                name = "Earthquake",
                description = "Slams the ground with tremendous force, causing the earth to shake violently and damage all nearby enemies.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 15,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 16,
                effects = """[{"type":"damage","damageType":"physical","modifier":16,"aoe":true},{"type":"condition","condition":"prone","duration":1,"saveType":"dexterity"}]""",
                attribution = ATTRIBUTION
            ),
            // Whirlwind - lifting and throwing
            Ability(
                id = ABILITY_WHIRLWIND_ID,
                name = "Whirlwind",
                description = "Creates a violent cyclone that lifts enemies into the air and hurls them, dealing damage and displacing them.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 20,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 10,
                effects = """[{"type":"damage","damageType":"physical","modifier":10,"aoe":true},{"type":"displacement","distance":15}]""",
                attribution = ATTRIBUTION
            ),
            // Magma Burst - fire and physical combo
            Ability(
                id = ABILITY_MAGMA_BURST_ID,
                name = "Magma Burst",
                description = "Erupts with molten rock, spraying magma in all directions that burns and bludgeons nearby enemies.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 15,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 18,
                effects = """[{"type":"damage","damageType":"fire","modifier":10,"aoe":true},{"type":"damage","damageType":"physical","modifier":8,"aoe":true}]""",
                attribution = ATTRIBUTION
            ),
            // Flash Freeze - instant freeze effect
            Ability(
                id = ABILITY_FLASH_FREEZE_ID,
                name = "Flash Freeze",
                description = "Drops the temperature around a target to absolute zero, encasing them in ice and dealing massive cold damage.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 25,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 22,
                durationRounds = 2,
                effects = """[{"type":"damage","damageType":"cold","modifier":22},{"type":"condition","condition":"frozen","duration":2,"saveType":"constitution"}]""",
                attribution = ATTRIBUTION
            ),
            // Engulf - absorb and damage
            Ability(
                id = ABILITY_ENGULF_ID,
                name = "Engulf",
                description = "The elemental surrounds and absorbs a target into its body, dealing continuous damage while they struggle to escape.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 15,
                durationRounds = 3,
                effects = """[{"type":"damage","modifier":15},{"type":"dot","damage":8,"duration":3},{"type":"condition","condition":"restrained","duration":3,"saveType":"strength"}]""",
                attribution = ATTRIBUTION
            ),
            // Chain Lightning - bouncing lightning
            Ability(
                id = ABILITY_CHAIN_LIGHTNING_ID,
                name = "Chain Lightning",
                description = "Releases a bolt of lightning that arcs between multiple targets, dealing electrical damage to each.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 40,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 20,
                effects = """[{"type":"damage","damageType":"lightning","modifier":20,"chain":true,"chainTargets":4,"chainReduction":0.25}]""",
                attribution = ATTRIBUTION
            ),
            // Elemental Fury - boss ultimate
            Ability(
                id = ABILITY_ELEMENTAL_FURY_ID,
                name = "Elemental Fury",
                description = "The Elemental Prince unleashes the combined power of all elements, devastating everything nearby with fire, ice, lightning, and stone.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 25,
                cooldownType = "long",
                cooldownRounds = 6,
                baseDamage = 35,
                effects = """[{"type":"damage","damageType":"fire","modifier":10,"aoe":true},{"type":"damage","damageType":"cold","modifier":10,"aoe":true},{"type":"damage","damageType":"lightning","modifier":10,"aoe":true},{"type":"damage","damageType":"physical","modifier":5,"aoe":true}]""",
                attribution = ATTRIBUTION
            )
        )

        abilities.forEach { ability ->
            if (AbilityRepository.findById(ability.id) == null) {
                AbilityRepository.create(ability)
            }
        }
    }

    private fun seedItems() {
        val items = listOf(
            // ===== CONSUMABLES =====
            Item(
                id = FIRE_RESIST_POTION_ID,
                name = "Potion of Fire Resistance",
                desc = "A shimmering red potion that grants temporary immunity to fire damage when consumed.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = COLD_RESIST_POTION_ID,
                name = "Potion of Cold Resistance",
                desc = "A pale blue potion that grants temporary immunity to cold damage when consumed.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = LIGHTNING_RESIST_POTION_ID,
                name = "Potion of Lightning Resistance",
                desc = "A crackling yellow potion that grants temporary immunity to lightning damage when consumed.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = ELEMENTAL_ESSENCE_ID,
                name = "Elemental Essence",
                desc = "A swirling orb of pure elemental energy, used for crafting powerful magical items or restoring mana.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 75,
                attribution = ATTRIBUTION
            ),

            // ===== WEAPONS =====
            Item(
                id = BLAZING_SCIMITAR_ID,
                name = "Blazing Scimitar",
                desc = "A curved blade wreathed in eternal flames that sear enemies with each strike.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 9, defense = 0, maxHp = 0),
                value = 225,
                attribution = ATTRIBUTION
            ),
            Item(
                id = FROSTBRAND_ID,
                name = "Frostbrand",
                desc = "A legendary blade of enchanted ice that never melts, dealing frost damage and protecting the wielder from fire.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 12, defense = 2, maxHp = 0),
                value = 550,
                attribution = ATTRIBUTION
            ),
            Item(
                id = EARTHBREAKER_HAMMER_ID,
                name = "Earthbreaker Hammer",
                desc = "A massive hammer hewn from primordial stone, capable of shattering rock and bone with equal ease.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 14, defense = 0, maxHp = 10),
                value = 475,
                attribution = ATTRIBUTION
            ),
            Item(
                id = STORMBRINGER_SPEAR_ID,
                name = "Stormbringer Spear",
                desc = "A javelin crackling with lightning that can be thrown to strike distant foes with thunderous force.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 11, defense = 0, maxHp = 0),
                value = 400,
                attribution = ATTRIBUTION
            ),
            Item(
                id = ELEMENTAL_STAFF_ID,
                name = "Staff of Elemental Power",
                desc = "A staff containing the essence of all four elements, granting its wielder mastery over fire, water, earth, and air.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 15, defense = 3, maxHp = 15),
                value = 900,
                attribution = ATTRIBUTION
            ),

            // ===== ARMOR =====
            Item(
                id = FIRE_RESISTANT_ARMOR_ID,
                name = "Fire Resistant Armor",
                desc = "Armor treated with alchemical compounds that protect the wearer from flames and heat.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 0, defense = 6, maxHp = 10),
                value = 200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = COLD_RESISTANT_ARMOR_ID,
                name = "Cold Resistant Armor",
                desc = "Armor lined with enchanted furs that keep the wearer warm even in the most frigid environments.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 0, defense = 6, maxHp = 10),
                value = 200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = ARMOR_OF_ELEMENTAL_COMMAND_ID,
                name = "Armor of Elemental Command",
                desc = "Legendary plate armor that grants dominion over elemental beings and protection from all elemental damage types.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 2, defense = 14, maxHp = 25),
                value = 1200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = BOOTS_OF_LEVITATION_ID,
                name = "Boots of Levitation",
                desc = "Enchanted boots that allow the wearer to float above the ground, avoiding terrain hazards.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "feet",
                statBonuses = StatBonuses(attack = 0, defense = 2, maxHp = 5),
                value = 350,
                attribution = ATTRIBUTION
            ),

            // ===== ACCESSORIES =====
            Item(
                id = RING_OF_FIRE_ELEMENTAL_COMMAND_ID,
                name = "Ring of Fire Elemental Command",
                desc = "A ruby ring that grants control over fire elementals and immunity to fire damage.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "finger",
                statBonuses = StatBonuses(attack = 3, defense = 0, maxHp = 10),
                value = 600,
                attribution = ATTRIBUTION
            ),
            Item(
                id = RING_OF_WATER_ELEMENTAL_COMMAND_ID,
                name = "Ring of Water Elemental Command",
                desc = "A sapphire ring that grants control over water elementals and the ability to breathe underwater.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "finger",
                statBonuses = StatBonuses(attack = 0, defense = 3, maxHp = 15),
                value = 600,
                attribution = ATTRIBUTION
            ),
            Item(
                id = CLOAK_OF_THE_WINDS_ID,
                name = "Cloak of the Winds",
                desc = "A flowing cloak that billows even in still air, granting the wearer enhanced speed and agility.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "back",
                statBonuses = StatBonuses(attack = 2, defense = 4, maxHp = 5),
                value = 425,
                attribution = ATTRIBUTION
            ),
            Item(
                id = AMULET_OF_ELEMENTAL_ABSORPTION_ID,
                name = "Amulet of Elemental Absorption",
                desc = "An amulet that absorbs elemental damage and converts it into healing energy for the wearer.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "neck",
                statBonuses = StatBonuses(attack = 0, defense = 4, maxHp = 20),
                value = 700,
                attribution = ATTRIBUTION
            ),

            // ===== TREASURE =====
            Item(
                id = FIRE_RUBY_ID,
                name = "Fire Ruby",
                desc = "A brilliant red gem that glows with inner fire, prized by jewelers and enchanters alike.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 125,
                attribution = ATTRIBUTION
            ),
            Item(
                id = FROZEN_TEAR_ID,
                name = "Frozen Tear",
                desc = "A crystallized tear from an ice elemental, perpetually cold to the touch and valuable for enchanting.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 125,
                attribution = ATTRIBUTION
            ),
            Item(
                id = ELEMENTAL_GEM_ID,
                name = "Elemental Gem",
                desc = "A multifaceted gem containing bound elemental energy, useful for summoning or powering magical devices.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 250,
                attribution = ATTRIBUTION
            ),
            Item(
                id = PRIMORDIAL_SHARD_ID,
                name = "Primordial Shard",
                desc = "A fragment from the dawn of creation, containing pure elemental chaos. Extremely rare and powerful.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 500,
                attribution = ATTRIBUTION
            )
        )

        items.forEach { item ->
            if (ItemRepository.findById(item.id) == null) {
                ItemRepository.create(item)
            }
        }
    }

    private fun seedLootTables() {
        val lootTables = listOf(
            LootTableData(
                id = LOOT_TABLE_MEPHIT_ID,
                name = "Mephit Loot",
                entries = listOf(
                    LootEntry(itemId = ELEMENTAL_ESSENCE_ID, chance = 0.20f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = FIRE_RUBY_ID, chance = 0.10f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = FROZEN_TEAR_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_FIRE_ELEMENTAL_ID,
                name = "Fire Elemental Loot",
                entries = listOf(
                    LootEntry(itemId = ELEMENTAL_ESSENCE_ID, chance = 0.35f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = FIRE_RUBY_ID, chance = 0.25f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = FIRE_RESIST_POTION_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = BLAZING_SCIMITAR_ID, chance = 0.08f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_WATER_ELEMENTAL_ID,
                name = "Water Elemental Loot",
                entries = listOf(
                    LootEntry(itemId = ELEMENTAL_ESSENCE_ID, chance = 0.35f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = FROZEN_TEAR_ID, chance = 0.25f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = COLD_RESIST_POTION_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = FROSTBRAND_ID, chance = 0.05f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_EARTH_ELEMENTAL_ID,
                name = "Earth Elemental Loot",
                entries = listOf(
                    LootEntry(itemId = ELEMENTAL_ESSENCE_ID, chance = 0.35f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = ELEMENTAL_GEM_ID, chance = 0.20f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = EARTHBREAKER_HAMMER_ID, chance = 0.08f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = FIRE_RESISTANT_ARMOR_ID, chance = 0.06f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_AIR_ELEMENTAL_ID,
                name = "Air Elemental Loot",
                entries = listOf(
                    LootEntry(itemId = ELEMENTAL_ESSENCE_ID, chance = 0.35f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = ELEMENTAL_GEM_ID, chance = 0.20f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = CLOAK_OF_THE_WINDS_ID, chance = 0.08f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = BOOTS_OF_LEVITATION_ID, chance = 0.06f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_GREATER_ELEMENTAL_ID,
                name = "Greater Elemental Loot",
                entries = listOf(
                    LootEntry(itemId = ELEMENTAL_ESSENCE_ID, chance = 0.60f, minQty = 2, maxQty = 4),
                    LootEntry(itemId = ELEMENTAL_GEM_ID, chance = 0.40f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = PRIMORDIAL_SHARD_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = RING_OF_FIRE_ELEMENTAL_COMMAND_ID, chance = 0.05f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = RING_OF_WATER_ELEMENTAL_COMMAND_ID, chance = 0.05f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_ELEMENTAL_PRINCE_ID,
                name = "Elemental Prince Loot",
                entries = listOf(
                    LootEntry(itemId = ELEMENTAL_ESSENCE_ID, chance = 1.0f, minQty = 5, maxQty = 10),
                    LootEntry(itemId = PRIMORDIAL_SHARD_ID, chance = 0.75f, minQty = 2, maxQty = 4),
                    LootEntry(itemId = ELEMENTAL_STAFF_ID, chance = 0.25f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = ARMOR_OF_ELEMENTAL_COMMAND_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = AMULET_OF_ELEMENTAL_ABSORPTION_ID, chance = 0.20f, minQty = 1, maxQty = 1)
                )
            )
        )

        lootTables.forEach { lootTable ->
            if (LootTableRepository.findById(lootTable.id) == null) {
                LootTableRepository.create(lootTable)
            }
        }
    }

    private fun seedCreatures() {
        val creatures = listOf(
            // ===== TIER 1 (CR 1) - Levels 1-3 =====
            Creature(
                id = FIRE_MEPHIT_ID,
                name = "Fire Mephit",
                desc = "A small elemental creature composed of fire and smoke. It cackles with glee as it hurls small fireballs at its enemies.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 22,
                baseDamage = 6,
                abilityIds = listOf(ABILITY_FIRE_BLAST_ID),
                level = 1,
                experienceValue = 25,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_MEPHIT_ID,
                minGoldDrop = 3,
                maxGoldDrop = 10,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ICE_MEPHIT_ID,
                name = "Ice Mephit",
                desc = "A diminutive elemental of ice and frost. Its touch chills to the bone, and it exhales blasts of freezing air.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 20,
                baseDamage = 5,
                abilityIds = listOf(ABILITY_FROST_BREATH_ELEM_ID),
                level = 1,
                experienceValue = 25,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_MEPHIT_ID,
                minGoldDrop = 3,
                maxGoldDrop = 10,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = DUST_MEPHIT_ID,
                name = "Dust Mephit",
                desc = "A swirling mass of dust and grit in vaguely humanoid form. It blinds enemies with clouds of choking particles.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 18,
                baseDamage = 4,
                abilityIds = listOf(ABILITY_AIR_FORM_ID),
                level = 1,
                experienceValue = 20,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_MEPHIT_ID,
                minGoldDrop = 2,
                maxGoldDrop = 8,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = STEAM_MEPHIT_ID,
                name = "Steam Mephit",
                desc = "A hissing elemental formed from superheated water vapor. It scalds enemies with jets of boiling steam.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 24,
                baseDamage = 7,
                abilityIds = listOf(ABILITY_FIRE_FORM_ID),
                level = 2,
                experienceValue = 30,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_MEPHIT_ID,
                minGoldDrop = 4,
                maxGoldDrop = 12,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 2 (CR 2-3) - Levels 4-6 =====
            Creature(
                id = FIRE_ELEMENTAL_ID,
                name = "Fire Elemental",
                desc = "A towering pillar of living flame that incinerates everything in its path. It moves with frightening speed and leaves scorched earth in its wake.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 65,
                baseDamage = 12,
                abilityIds = listOf(ABILITY_FIRE_FORM_ID, ABILITY_FIRE_BLAST_ID),
                level = 4,
                experienceValue = 75,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_FIRE_ELEMENTAL_ID,
                minGoldDrop = 15,
                maxGoldDrop = 35,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = WATER_ELEMENTAL_ID,
                name = "Water Elemental",
                desc = "A churning mass of water that crashes through foes like a living tidal wave. It can engulf and drown those who underestimate it.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 75,
                baseDamage = 11,
                abilityIds = listOf(ABILITY_WATER_FORM_ID, ABILITY_ENGULF_ID),
                level = 4,
                experienceValue = 75,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_WATER_ELEMENTAL_ID,
                minGoldDrop = 15,
                maxGoldDrop = 35,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = EARTH_ELEMENTAL_ID,
                name = "Earth Elemental",
                desc = "A hulking mass of rock and stone that moves through earth as easily as swimming through water. Its fists can shatter walls.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 85,
                baseDamage = 13,
                abilityIds = listOf(ABILITY_EARTH_FORM_ID, ABILITY_EARTHQUAKE_ID),
                level = 4,
                experienceValue = 80,
                challengeRating = 3,
                isAggressive = true,
                lootTableId = LOOT_TABLE_EARTH_ELEMENTAL_ID,
                minGoldDrop = 18,
                maxGoldDrop = 40,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = AIR_ELEMENTAL_ID,
                name = "Air Elemental",
                desc = "An invisible force that manifests as a howling whirlwind. It lifts enemies into the air and hurls them with devastating force.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 55,
                baseDamage = 10,
                abilityIds = listOf(ABILITY_AIR_FORM_ID, ABILITY_WHIRLWIND_ID),
                level = 4,
                experienceValue = 70,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_AIR_ELEMENTAL_ID,
                minGoldDrop = 15,
                maxGoldDrop = 35,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 3 (CR 4-5) - Levels 7-10 =====
            Creature(
                id = MAGMA_ELEMENTAL_ID,
                name = "Magma Elemental",
                desc = "A fusion of fire and earth, this elemental is composed of molten rock that burns and crushes simultaneously. Lava drips from its form.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 100,
                baseDamage = 16,
                abilityIds = listOf(ABILITY_FIRE_FORM_ID, ABILITY_MAGMA_BURST_ID, ABILITY_EARTHQUAKE_ID),
                level = 6,
                experienceValue = 140,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GREATER_ELEMENTAL_ID,
                minGoldDrop = 30,
                maxGoldDrop = 70,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ICE_ELEMENTAL_ID,
                name = "Ice Elemental",
                desc = "A fusion of water and air, this elemental is a towering form of living ice. Its presence drops the temperature dramatically.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 90,
                baseDamage = 14,
                abilityIds = listOf(ABILITY_WATER_FORM_ID, ABILITY_FLASH_FREEZE_ID, ABILITY_FROST_BREATH_ELEM_ID),
                level = 6,
                experienceValue = 135,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GREATER_ELEMENTAL_ID,
                minGoldDrop = 30,
                maxGoldDrop = 70,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = MUD_ELEMENTAL_ID,
                name = "Mud Elemental",
                desc = "A fusion of earth and water, this elemental is a churning mass of muck that can engulf and suffocate foes.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 110,
                baseDamage = 15,
                abilityIds = listOf(ABILITY_EARTH_FORM_ID, ABILITY_ENGULF_ID),
                level = 6,
                experienceValue = 130,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GREATER_ELEMENTAL_ID,
                minGoldDrop = 28,
                maxGoldDrop = 65,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = LIGHTNING_ELEMENTAL_ID,
                name = "Lightning Elemental",
                desc = "A fusion of fire and air, this elemental crackles with electrical energy. It moves at blinding speed and strikes with devastating bolts.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 75,
                baseDamage = 18,
                abilityIds = listOf(ABILITY_AIR_FORM_ID, ABILITY_CHAIN_LIGHTNING_ID),
                level = 7,
                experienceValue = 145,
                challengeRating = 5,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GREATER_ELEMENTAL_ID,
                minGoldDrop = 35,
                maxGoldDrop = 80,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 4 (CR 6+) - Boss =====
            Creature(
                id = ELEMENTAL_PRINCE_ID,
                name = "Elemental Prince",
                desc = "A being of immense power that commands all four elements. It shifts between forms of fire, water, earth, and air, unleashing devastating attacks.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 200,
                baseDamage = 26,
                abilityIds = listOf(ABILITY_ELEMENTAL_FURY_ID, ABILITY_FIRE_BLAST_ID, ABILITY_FLASH_FREEZE_ID, ABILITY_EARTHQUAKE_ID, ABILITY_WHIRLWIND_ID),
                level = 10,
                experienceValue = 350,
                challengeRating = 7,
                isAggressive = true,
                lootTableId = LOOT_TABLE_ELEMENTAL_PRINCE_ID,
                minGoldDrop = 200,
                maxGoldDrop = 500,
                attribution = ATTRIBUTION
            )
        )

        creatures.forEach { creature ->
            if (CreatureRepository.findById(creature.id) == null) {
                CreatureRepository.create(creature)
            }
        }
    }

    /**
     * Generate images for all Elemental Chaos entities that don't have images yet.
     */
    suspend fun generateMissingImages() {
        log.info("Starting image generation for Elemental Chaos entities...")

        val creaturePrompts = mapOf(
            FIRE_MEPHIT_ID to "Fire mephit, small fire elemental creature, cackling imp made of flames and smoke, fantasy art",
            ICE_MEPHIT_ID to "Ice mephit, small ice elemental creature, frosty imp with icicle claws, blue frost aura, fantasy art",
            DUST_MEPHIT_ID to "Dust mephit, small air elemental creature, swirling dust and grit humanoid, desert spirit, fantasy art",
            STEAM_MEPHIT_ID to "Steam mephit, small water and fire elemental, hissing vapor creature, scalding mist form, fantasy art",
            FIRE_ELEMENTAL_ID to "Fire elemental, towering pillar of living flame, humanoid fire being, orange and red flames, fantasy art",
            WATER_ELEMENTAL_ID to "Water elemental, churning mass of living water, humanoid wave form, blue translucent, fantasy art",
            EARTH_ELEMENTAL_ID to "Earth elemental, massive rock and stone golem, humanoid boulder, ancient stone guardian, fantasy art",
            AIR_ELEMENTAL_ID to "Air elemental, swirling whirlwind with face, invisible wind made visible, ethereal vortex, fantasy art",
            MAGMA_ELEMENTAL_ID to "Magma elemental, molten rock humanoid, dripping lava, glowing orange cracks, volcanic being, fantasy art",
            ICE_ELEMENTAL_ID to "Ice elemental, towering frozen humanoid, crystalline ice form, cold blue glow, frost giant, fantasy art",
            MUD_ELEMENTAL_ID to "Mud elemental, churning mass of muck and earth, oozing humanoid, swamp creature, fantasy art",
            LIGHTNING_ELEMENTAL_ID to "Lightning elemental, crackling electrical humanoid, plasma being, blue-white energy, storm creature, fantasy art",
            ELEMENTAL_PRINCE_ID to "Elemental prince boss, massive being of all four elements, fire water earth air combined, cosmic elemental lord, epic fantasy art"
        )

        val itemPrompts = mapOf(
            FIRE_RESIST_POTION_ID to "Fire resistance potion, red shimmering liquid, glass flask with flame stopper, fantasy alchemy item",
            COLD_RESIST_POTION_ID to "Cold resistance potion, pale blue glowing liquid, frosted glass flask, fantasy alchemy item",
            LIGHTNING_RESIST_POTION_ID to "Lightning resistance potion, crackling yellow liquid, glass flask with lightning bolt, fantasy alchemy item",
            ELEMENTAL_ESSENCE_ID to "Elemental essence orb, swirling multicolored energy, magical crafting material, fantasy item",
            BLAZING_SCIMITAR_ID to "Blazing scimitar, curved sword wreathed in eternal flames, fire weapon, fantasy art",
            FROSTBRAND_ID to "Frostbrand legendary sword, blade of enchanted ice, frost weapon glowing blue, fantasy art",
            EARTHBREAKER_HAMMER_ID to "Earthbreaker hammer, massive stone warhammer, primordial rock weapon, fantasy art",
            STORMBRINGER_SPEAR_ID to "Stormbringer spear, lightning crackling javelin, thunder weapon, fantasy art",
            ELEMENTAL_STAFF_ID to "Staff of elemental power, four element gems in staff head, fire water earth air, legendary weapon, fantasy art",
            FIRE_RESISTANT_ARMOR_ID to "Fire resistant armor, heat-treated plate armor, red-tinted metal, salamander scales, fantasy armor",
            COLD_RESISTANT_ARMOR_ID to "Cold resistant armor, fur-lined plate armor, frost protection enchantment, fantasy armor",
            ARMOR_OF_ELEMENTAL_COMMAND_ID to "Armor of elemental command, legendary plate with four element symbols, glowing runes, fantasy armor",
            BOOTS_OF_LEVITATION_ID to "Boots of levitation, enchanted footwear with floating runes, magical boots, fantasy item",
            RING_OF_FIRE_ELEMENTAL_COMMAND_ID to "Ring of fire elemental command, ruby ring with flame symbol, magical jewelry, fantasy art",
            RING_OF_WATER_ELEMENTAL_COMMAND_ID to "Ring of water elemental command, sapphire ring with wave symbol, magical jewelry, fantasy art",
            CLOAK_OF_THE_WINDS_ID to "Cloak of the winds, flowing cape billowing in still air, wind magic, fantasy accessory",
            AMULET_OF_ELEMENTAL_ABSORPTION_ID to "Amulet of elemental absorption, four-element pendant, protective magic jewelry, fantasy art",
            FIRE_RUBY_ID to "Fire ruby gem, brilliant red jewel with inner flame glow, precious treasure, fantasy item",
            FROZEN_TEAR_ID to "Frozen tear gem, crystallized ice elemental tear, perpetually cold, precious treasure, fantasy item",
            ELEMENTAL_GEM_ID to "Elemental gem, multifaceted jewel with swirling energy, bound elemental power, fantasy item",
            PRIMORDIAL_SHARD_ID to "Primordial shard, fragment of creation, pure elemental chaos crystal, legendary treasure, fantasy art"
        )

        // Generate creature images
        for ((creatureId, prompt) in creaturePrompts) {
            val creature = CreatureRepository.findById(creatureId)
            if (creature != null && creature.imageUrl == null) {
                log.info("Generating image for creature: ${creature.name}")
                ImageGenerationService.generateImage(
                    entityType = "creature",
                    entityId = creatureId,
                    description = prompt,
                    entityName = creature.name
                ).onSuccess { imageUrl ->
                    CreatureRepository.updateImageUrl(creatureId, imageUrl)
                    log.info("Generated image for ${creature.name}: $imageUrl")
                }.onFailure { error ->
                    log.warn("Failed to generate image for ${creature.name}: ${error.message}")
                }
            }
        }

        // Generate item images
        for ((itemId, prompt) in itemPrompts) {
            val item = ItemRepository.findById(itemId)
            if (item != null && item.imageUrl == null) {
                log.info("Generating image for item: ${item.name}")
                ImageGenerationService.generateImage(
                    entityType = "item",
                    entityId = itemId,
                    description = prompt,
                    entityName = item.name
                ).onSuccess { imageUrl ->
                    ItemRepository.updateImageUrl(itemId, imageUrl)
                    log.info("Generated image for ${item.name}: $imageUrl")
                }.onFailure { error ->
                    log.warn("Failed to generate image for ${item.name}: ${error.message}")
                }
            }
        }

        log.info("Elemental Chaos image generation complete")
    }
}
