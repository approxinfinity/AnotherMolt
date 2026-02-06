package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.ImageGenerationService
import org.slf4j.LoggerFactory

/**
 * Seed data inspired by classic D&D creatures, items, spells, and abilities.
 * This content draws from the original Dungeons & Dragons rulebooks for inspiration,
 * providing a diverse set of fantasy encounters across multiple challenge tiers.
 */
object ClassicFantasySeed {
    private val log = LoggerFactory.getLogger(ClassicFantasySeed::class.java)

    // Attribution constant for all D&D-inspired content
    private const val ATTRIBUTION = "Classic D&D (OD&D/Basic)"

    // ============== CREATURE IDS ==============
    // Tier 1 (CR 1) - Levels 1-3
    const val GOBLIN_WARRIOR_ID = "creature-goblin-warrior"
    const val GIANT_RAT_ID = "creature-giant-rat"
    const val SKELETON_WARRIOR_ID = "creature-skeleton-warrior"
    const val KOBOLD_TRAPPER_ID = "creature-kobold-trapper"
    const val GIANT_SPIDER_ID = "creature-giant-spider"

    // Tier 2 (CR 2-3) - Levels 4-6
    const val ORC_BERSERKER_ID = "creature-orc-berserker"
    const val ZOMBIE_BRUTE_ID = "creature-zombie-brute"
    const val GHOUL_ID = "creature-ghoul"
    const val HOBGOBLIN_CAPTAIN_ID = "creature-hobgoblin-captain"
    const val OCHRE_JELLY_ID = "creature-ochre-jelly"

    // Tier 3 (CR 4-5) - Levels 7-10
    const val TROLL_ID = "creature-troll"
    const val WRAITH_ID = "creature-wraith"
    const val MINOTAUR_ID = "creature-minotaur"
    const val BASILISK_ID = "creature-basilisk"
    const val YOUNG_RED_DRAGON_ID = "creature-young-red-dragon"

    // ============== ABILITY IDS ==============
    const val ABILITY_POISON_BITE_ID = "ability-poison-bite"
    const val ABILITY_REND_ID = "ability-rend"
    const val ABILITY_PARALYZING_TOUCH_ID = "ability-paralyzing-touch"
    const val ABILITY_TROLL_REGEN_ID = "ability-troll-regeneration"
    const val ABILITY_LIFE_DRAIN_ID = "ability-life-drain"
    const val ABILITY_PETRIFYING_GAZE_ID = "ability-petrifying-gaze"
    const val ABILITY_FIRE_BREATH_ID = "ability-fire-breath"
    const val ABILITY_GORE_ID = "ability-gore"
    const val ABILITY_SPLIT_ID = "ability-split"
    const val ABILITY_TRAP_SETTER_ID = "ability-trap-setter"

    // ============== ITEM IDS ==============
    // Consumables
    const val MINOR_HEALING_POTION_ID = "item-minor-healing-potion"
    const val MAJOR_HEALING_POTION_ID = "item-major-healing-potion"
    const val ANTIDOTE_VIAL_ID = "item-antidote-vial"
    const val GIANT_STRENGTH_ELIXIR_ID = "item-giant-strength-elixir"
    const val FIREBALL_SCROLL_ID = "item-fireball-scroll"

    // Weapons
    const val ORCISH_CLEAVER_ID = "item-orcish-cleaver"
    const val SILVER_DAGGER_ID = "item-silver-dagger"
    const val FLAMING_SWORD_ID = "item-flaming-sword"
    const val STAFF_OF_FROST_ID = "item-staff-of-frost"
    const val MINOTAUR_GREATAXE_ID = "item-minotaur-greataxe"

    // Armor
    const val GOBLIN_HIDE_ARMOR_ID = "item-goblin-hide-armor"
    const val CHAINMAIL_FALLEN_ID = "item-chainmail-of-the-fallen"
    const val DRAGONSCALE_SHIELD_ID = "item-dragonscale-shield"
    const val HELM_DARKVISION_ID = "item-helm-of-darkvision"

    // Accessories
    const val RING_OF_REGEN_ID = "item-ring-of-regeneration"
    const val AMULET_POISON_PROOF_ID = "item-amulet-proof-against-poison"
    const val CLOAK_ELVENKIND_ID = "item-cloak-of-elvenkind"
    const val GAUNTLETS_OGRE_POWER_ID = "item-gauntlets-of-ogre-power"

    // Treasure
    const val ANCIENT_GOLD_COIN_ID = "item-ancient-gold-coin"
    const val RUBY_GEMSTONE_ID = "item-ruby-gemstone"
    const val DRAGON_TOOTH_ID = "item-dragon-tooth"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_GOBLIN_ID = "loot-goblin"
    const val LOOT_TABLE_SKELETON_ID = "loot-skeleton"
    const val LOOT_TABLE_ORC_ID = "loot-orc"
    const val LOOT_TABLE_GHOUL_ID = "loot-ghoul"
    const val LOOT_TABLE_TROLL_ID = "loot-troll"
    const val LOOT_TABLE_MINOTAUR_ID = "loot-minotaur"
    const val LOOT_TABLE_DRAGON_ID = "loot-dragon"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (GOBLIN_WARRIOR_ID !in existingCreatures) {
            seedCreatureAbilities()
            seedItems()
            seedLootTables()
            seedCreatures()
            println("Seeded Classic Fantasy content (D&D-inspired)")
        }
    }

    private fun seedCreatureAbilities() {
        val abilities = listOf(
            // Poison Bite - used by Giant Spider, Basilisk
            Ability(
                id = ABILITY_POISON_BITE_ID,
                name = "Poison Bite",
                description = "A venomous bite that injects deadly poison, dealing immediate damage and causing ongoing poison damage for 3 rounds.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 8,
                durationRounds = 3,
                effects = """[{"type":"damage","modifier":8},{"type":"dot","damageType":"poison","damage":4,"duration":3}]""",
                attribution = ATTRIBUTION
            ),
            // Rend - used by Orc Berserker, Troll
            Ability(
                id = ABILITY_REND_ID,
                name = "Rend",
                description = "A savage tearing attack with claws or weapons that causes deep wounds, dealing heavy damage plus bleeding over time.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 15,
                durationRounds = 2,
                effects = """[{"type":"damage","modifier":15},{"type":"dot","damageType":"bleed","damage":3,"duration":2}]""",
                attribution = ATTRIBUTION
            ),
            // Paralyzing Touch - used by Ghoul
            Ability(
                id = ABILITY_PARALYZING_TOUCH_ID,
                name = "Paralyzing Touch",
                description = "A chilling touch that can freeze victims in place. The target must resist or become paralyzed for 2 rounds.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 6,
                durationRounds = 2,
                effects = """[{"type":"damage","modifier":6},{"type":"condition","condition":"paralyzed","duration":2,"saveType":"constitution"}]""",
                attribution = ATTRIBUTION
            ),
            // Troll Regeneration - used by Troll
            Ability(
                id = ABILITY_TROLL_REGEN_ID,
                name = "Troll Regeneration",
                description = "The troll's unnatural vitality causes it to regenerate health each round. Fire or acid damage can temporarily suppress this ability.",
                classId = null,
                abilityType = "passive",
                targetType = "self",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 0,
                effects = """[{"type":"regeneration","value":8,"duration":0,"negatedBy":["fire","acid"]}]""",
                attribution = ATTRIBUTION
            ),
            // Life Drain - used by Wraith
            Ability(
                id = ABILITY_LIFE_DRAIN_ID,
                name = "Life Drain",
                description = "Drains the life force from a living creature, dealing necrotic damage and healing the wraith for half the damage dealt.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 12,
                effects = """[{"type":"damage","damageType":"necrotic","modifier":12},{"type":"lifesteal","modifier":50}]""",
                attribution = ATTRIBUTION
            ),
            // Petrifying Gaze - used by Basilisk
            Ability(
                id = ABILITY_PETRIFYING_GAZE_ID,
                name = "Petrifying Gaze",
                description = "The basilisk's dreadful gaze can turn flesh to stone. Those who meet its eyes are slowed and weakened as stone creeps through their limbs.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 0,
                durationRounds = 3,
                effects = """[{"type":"condition","condition":"slowed","duration":3},{"type":"debuff","stat":"defense","penalty":5,"duration":3}]""",
                attribution = ATTRIBUTION
            ),
            // Fire Breath - used by Young Red Dragon
            Ability(
                id = ABILITY_FIRE_BREATH_ID,
                name = "Fire Breath",
                description = "Exhales a devastating cone of fire that engulfs all enemies, dealing massive fire damage to everything in its path.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 25,
                effects = """[{"type":"damage","damageType":"fire","modifier":25,"aoe":true}]""",
                attribution = ATTRIBUTION
            ),
            // Gore - used by Minotaur
            Ability(
                id = ABILITY_GORE_ID,
                name = "Gore",
                description = "A powerful charging attack with massive horns. The minotaur lowers its head and charges, dealing devastating piercing damage.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 20,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 20,
                effects = """[{"type":"damage","modifier":20,"effect":"charge"}]"""
            ),
            // Split - used by Ochre Jelly
            Ability(
                id = ABILITY_SPLIT_ID,
                name = "Split",
                description = "When struck by slashing damage, the jelly splits into smaller copies. Each copy has reduced health but retains full offensive capability.",
                classId = null,
                abilityType = "passive",
                targetType = "self",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 0,
                effects = """[{"type":"trigger","trigger":"slashing_damage","effect":"spawn_copy","healthRatio":0.5}]"""
            ),
            // Trap Setter - used by Kobold Trapper
            Ability(
                id = ABILITY_TRAP_SETTER_ID,
                name = "Trap Setter",
                description = "Sets a hidden trap in an area. Enemies who trigger it take damage and are slowed for 2 rounds.",
                classId = null,
                abilityType = "utility",
                targetType = "area",
                range = 20,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 10,
                durationRounds = 2,
                effects = """[{"type":"trap","damage":10},{"type":"condition","condition":"slowed","duration":2}]"""
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
                id = MINOR_HEALING_POTION_ID,
                name = "Minor Healing Potion",
                desc = "A small vial of red liquid that restores a modest amount of health when consumed.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 25
            ),
            Item(
                id = MAJOR_HEALING_POTION_ID,
                name = "Major Healing Potion",
                desc = "A larger flask of crimson elixir that restores significant health when consumed.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 75
            ),
            Item(
                id = ANTIDOTE_VIAL_ID,
                name = "Antidote Vial",
                desc = "A clear liquid that neutralizes most common poisons and venoms.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 35
            ),
            Item(
                id = GIANT_STRENGTH_ELIXIR_ID,
                name = "Elixir of Giant Strength",
                desc = "A murky potion that temporarily grants the drinker the strength of a hill giant.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 150
            ),
            Item(
                id = FIREBALL_SCROLL_ID,
                name = "Scroll of Fireball",
                desc = "An ancient parchment inscribed with the incantation for a powerful fire spell.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 200
            ),

            // ===== WEAPONS =====
            Item(
                id = ORCISH_CLEAVER_ID,
                name = "Orcish Cleaver",
                desc = "A brutal, heavy blade favored by orc warriors. Crude but devastatingly effective.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 6, defense = 0, maxHp = 0),
                value = 65
            ),
            Item(
                id = SILVER_DAGGER_ID,
                name = "Silver Dagger",
                desc = "A finely crafted dagger made of pure silver, especially effective against supernatural creatures.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 4, defense = 0, maxHp = 0),
                value = 80
            ),
            Item(
                id = FLAMING_SWORD_ID,
                name = "Flaming Sword",
                desc = "A legendary blade wreathed in magical fire that never burns out. Deals additional fire damage with each strike.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 10, defense = 0, maxHp = 0),
                value = 350
            ),
            Item(
                id = STAFF_OF_FROST_ID,
                name = "Staff of Frost",
                desc = "A staff carved from eternal ice, cold to the touch. Empowers ice magic and provides some protection.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 8, defense = 0, maxHp = 15),
                value = 275
            ),
            Item(
                id = MINOTAUR_GREATAXE_ID,
                name = "Minotaur Greataxe",
                desc = "A massive double-headed axe wielded by minotaurs. Incredibly powerful but unwieldy.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 12, defense = -2, maxHp = 0),
                value = 200
            ),

            // ===== ARMOR =====
            Item(
                id = GOBLIN_HIDE_ARMOR_ID,
                name = "Goblin Hide Armor",
                desc = "Crude armor made from cured hides and scavenged materials. Offers basic protection.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 0, defense = 2, maxHp = 0),
                value = 25
            ),
            Item(
                id = CHAINMAIL_FALLEN_ID,
                name = "Chainmail of the Fallen",
                desc = "Armor recovered from a fallen warrior, still imbued with their fighting spirit. Provides solid protection.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 0, defense = 8, maxHp = 5),
                value = 175
            ),
            Item(
                id = DRAGONSCALE_SHIELD_ID,
                name = "Dragonscale Shield",
                desc = "A shield crafted from the scales of a slain dragon. Nearly impervious to damage and fire-resistant.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "off_hand",
                statBonuses = StatBonuses(attack = 0, defense = 6, maxHp = 10),
                value = 400
            ),
            Item(
                id = HELM_DARKVISION_ID,
                name = "Helm of Darkvision",
                desc = "An enchanted helmet that allows the wearer to see in complete darkness as if it were daylight.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "head",
                statBonuses = StatBonuses(attack = 0, defense = 2, maxHp = 5),
                value = 125
            ),

            // ===== ACCESSORIES =====
            Item(
                id = RING_OF_REGEN_ID,
                name = "Ring of Regeneration",
                desc = "A golden ring set with a pulsing emerald. The wearer slowly regenerates health over time.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "ring",
                statBonuses = StatBonuses(attack = 0, defense = 0, maxHp = 20),
                value = 500
            ),
            Item(
                id = AMULET_POISON_PROOF_ID,
                name = "Amulet of Proof Against Poison",
                desc = "A silver amulet that grants its wearer resistance to all forms of poison and venom.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "neck",
                statBonuses = StatBonuses(attack = 0, defense = 1, maxHp = 10),
                value = 200
            ),
            Item(
                id = CLOAK_ELVENKIND_ID,
                name = "Cloak of Elvenkind",
                desc = "A shimmering grey-green cloak that helps the wearer blend into natural surroundings.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "back",
                statBonuses = StatBonuses(attack = 0, defense = 3, maxHp = 5),
                value = 225
            ),
            Item(
                id = GAUNTLETS_OGRE_POWER_ID,
                name = "Gauntlets of Ogre Power",
                desc = "Heavy iron gauntlets that grant the wearer tremendous strength.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "hands",
                statBonuses = StatBonuses(attack = 6, defense = 1, maxHp = 0),
                value = 350
            ),

            // ===== TREASURE =====
            Item(
                id = ANCIENT_GOLD_COIN_ID,
                name = "Ancient Gold Coin",
                desc = "A weathered gold coin from a long-forgotten empire. Collectors pay handsomely for such relics.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 50
            ),
            Item(
                id = RUBY_GEMSTONE_ID,
                name = "Ruby Gemstone",
                desc = "A flawless ruby that catches and reflects light with an inner fire.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 200
            ),
            Item(
                id = DRAGON_TOOTH_ID,
                name = "Dragon Tooth",
                desc = "A massive fang from a true dragon. Highly valued by alchemists and collectors alike.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 300
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
                id = LOOT_TABLE_GOBLIN_ID,
                name = "Goblin Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_GOLD_COIN_ID, chance = 0.15f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = GOBLIN_HIDE_ARMOR_ID, chance = 0.10f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = MINOR_HEALING_POTION_ID, chance = 0.05f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_SKELETON_ID,
                name = "Skeleton Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_GOLD_COIN_ID, chance = 0.20f, minQty = 1, maxQty = 3),
                    LootEntry(itemId = SILVER_DAGGER_ID, chance = 0.05f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_ORC_ID,
                name = "Orc Loot",
                entries = listOf(
                    LootEntry(itemId = ORCISH_CLEAVER_ID, chance = 0.25f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = ANCIENT_GOLD_COIN_ID, chance = 0.30f, minQty = 2, maxQty = 4),
                    LootEntry(itemId = MINOR_HEALING_POTION_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_GHOUL_ID,
                name = "Ghoul Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_GOLD_COIN_ID, chance = 0.25f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = RUBY_GEMSTONE_ID, chance = 0.05f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = CLOAK_ELVENKIND_ID, chance = 0.03f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_TROLL_ID,
                name = "Troll Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_GOLD_COIN_ID, chance = 0.50f, minQty = 3, maxQty = 6),
                    LootEntry(itemId = RUBY_GEMSTONE_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = RING_OF_REGEN_ID, chance = 0.05f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = MAJOR_HEALING_POTION_ID, chance = 0.20f, minQty = 1, maxQty = 2)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_MINOTAUR_ID,
                name = "Minotaur Loot",
                entries = listOf(
                    LootEntry(itemId = MINOTAUR_GREATAXE_ID, chance = 0.50f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = ANCIENT_GOLD_COIN_ID, chance = 0.60f, minQty = 4, maxQty = 8),
                    LootEntry(itemId = GAUNTLETS_OGRE_POWER_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_DRAGON_ID,
                name = "Dragon Loot",
                entries = listOf(
                    LootEntry(itemId = DRAGON_TOOTH_ID, chance = 1.0f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = RUBY_GEMSTONE_ID, chance = 0.75f, minQty = 2, maxQty = 4),
                    LootEntry(itemId = FLAMING_SWORD_ID, chance = 0.25f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = DRAGONSCALE_SHIELD_ID, chance = 0.20f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = ANCIENT_GOLD_COIN_ID, chance = 1.0f, minQty = 10, maxQty = 20)
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
                id = GOBLIN_WARRIOR_ID,
                name = "Goblin Warrior",
                desc = "A small, green-skinned humanoid armed with a crude scimitar. Goblins are cowardly alone but dangerous in groups, relying on numbers and dirty tricks.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 18,
                baseDamage = 6,
                abilityIds = emptyList(),
                level = 1,
                experienceValue = 20,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GOBLIN_ID,
                minGoldDrop = 2,
                maxGoldDrop = 8
            ),
            Creature(
                id = GIANT_RAT_ID,
                name = "Giant Rat",
                desc = "A rat grown to monstrous proportions, with beady red eyes and yellowed fangs dripping with disease. They swarm through dungeons and sewers.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 12,
                baseDamage = 5,
                abilityIds = emptyList(),
                level = 1,
                experienceValue = 15,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = null,
                minGoldDrop = 0,
                maxGoldDrop = 3
            ),
            Creature(
                id = SKELETON_WARRIOR_ID,
                name = "Skeleton Warrior",
                desc = "The animated bones of a fallen warrior, held together by dark magic. It wields a rusted sword with eerie precision.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 20,
                baseDamage = 7,
                abilityIds = emptyList(),
                level = 1,
                experienceValue = 25,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_SKELETON_ID,
                minGoldDrop = 3,
                maxGoldDrop = 10
            ),
            Creature(
                id = KOBOLD_TRAPPER_ID,
                name = "Kobold Trapper",
                desc = "A cunning reptilian humanoid that excels at setting traps and ambushes. What kobolds lack in strength, they make up for in devious tactics.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 14,
                baseDamage = 5,
                abilityIds = listOf(ABILITY_TRAP_SETTER_ID),
                level = 1,
                experienceValue = 22,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GOBLIN_ID,
                minGoldDrop = 2,
                maxGoldDrop = 6
            ),
            Creature(
                id = GIANT_SPIDER_ID,
                name = "Giant Spider",
                desc = "A massive arachnid the size of a dog, with venomous fangs and the ability to spin sticky webs. They lurk in dark corners, waiting for prey.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 22,
                baseDamage = 8,
                abilityIds = listOf(ABILITY_POISON_BITE_ID),
                level = 1,
                experienceValue = 30,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = null,
                minGoldDrop = 0,
                maxGoldDrop = 5
            ),

            // ===== TIER 2 (CR 2-3) - Levels 4-6 =====
            Creature(
                id = ORC_BERSERKER_ID,
                name = "Orc Berserker",
                desc = "A hulking grey-green brute driven by battle-rage. When an orc berserker enters combat, it fights with reckless fury until nothing remains standing.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 45,
                baseDamage = 12,
                abilityIds = listOf(ABILITY_REND_ID),
                level = 3,
                experienceValue = 60,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_ORC_ID,
                minGoldDrop = 10,
                maxGoldDrop = 25
            ),
            Creature(
                id = ZOMBIE_BRUTE_ID,
                name = "Zombie Brute",
                desc = "A massive reanimated corpse, bloated and powerful. It shambles forward with terrible purpose, shrugging off wounds that would fell the living.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 55,
                baseDamage = 10,
                abilityIds = emptyList(),
                level = 3,
                experienceValue = 45,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_SKELETON_ID,
                minGoldDrop = 5,
                maxGoldDrop = 15
            ),
            Creature(
                id = GHOUL_ID,
                name = "Ghoul",
                desc = "An undead creature that feeds on corpses. Its touch can paralyze the living, leaving them helpless as it feeds. The stench of death clings to it.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 40,
                baseDamage = 11,
                abilityIds = listOf(ABILITY_PARALYZING_TOUCH_ID),
                level = 3,
                experienceValue = 65,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GHOUL_ID,
                minGoldDrop = 8,
                maxGoldDrop = 20
            ),
            Creature(
                id = HOBGOBLIN_CAPTAIN_ID,
                name = "Hobgoblin Captain",
                desc = "A disciplined military commander of the hobgoblin legions. Unlike their goblin cousins, hobgoblins are organized, tactical, and deadly efficient.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 50,
                baseDamage = 13,
                abilityIds = emptyList(),
                level = 4,
                experienceValue = 75,
                challengeRating = 3,
                isAggressive = true,
                lootTableId = LOOT_TABLE_ORC_ID,
                minGoldDrop = 15,
                maxGoldDrop = 35
            ),
            Creature(
                id = OCHRE_JELLY_ID,
                name = "Ochre Jelly",
                desc = "A massive blob of acidic ooze that dissolves organic matter on contact. Cutting it only creates more jellies. Fire and cold are the only reliable methods.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 60,
                baseDamage = 9,
                abilityIds = listOf(ABILITY_SPLIT_ID),
                level = 3,
                experienceValue = 55,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = null,
                minGoldDrop = 0,
                maxGoldDrop = 0
            ),

            // ===== TIER 3 (CR 4-5) - Levels 7-10 =====
            Creature(
                id = TROLL_ID,
                name = "Troll",
                desc = "A gangly giant with rubbery green skin and incredible regenerative abilities. Only fire or acid can prevent a troll from healing its wounds.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 85,
                baseDamage = 16,
                abilityIds = listOf(ABILITY_REND_ID, ABILITY_TROLL_REGEN_ID),
                level = 5,
                experienceValue = 120,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_TROLL_ID,
                minGoldDrop = 25,
                maxGoldDrop = 60
            ),
            Creature(
                id = WRAITH_ID,
                name = "Wraith",
                desc = "A malevolent spirit bound to the material world by hatred. Its incorporeal form is difficult to harm, and its touch drains the very life from living creatures.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 65,
                baseDamage = 14,
                abilityIds = listOf(ABILITY_LIFE_DRAIN_ID),
                level = 5,
                experienceValue = 110,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GHOUL_ID,
                minGoldDrop = 20,
                maxGoldDrop = 50
            ),
            Creature(
                id = MINOTAUR_ID,
                name = "Minotaur",
                desc = "A massive bull-headed humanoid that guards ancient labyrinths. Minotaurs are fearsome combatants, charging into battle with devastating force.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 95,
                baseDamage = 18,
                abilityIds = listOf(ABILITY_GORE_ID),
                level = 6,
                experienceValue = 140,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_MINOTAUR_ID,
                minGoldDrop = 30,
                maxGoldDrop = 75
            ),
            Creature(
                id = BASILISK_ID,
                name = "Basilisk",
                desc = "An eight-legged reptile with a deadly gaze that can turn creatures to stone. Its very presence brings silence, as even the smallest sound might draw its attention.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 75,
                baseDamage = 13,
                abilityIds = listOf(ABILITY_PETRIFYING_GAZE_ID, ABILITY_POISON_BITE_ID),
                level = 5,
                experienceValue = 125,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_TROLL_ID,
                minGoldDrop = 20,
                maxGoldDrop = 50
            ),
            Creature(
                id = YOUNG_RED_DRAGON_ID,
                name = "Young Red Dragon",
                desc = "A dragon in its youth, already a terrifying adversary. Its crimson scales gleam like hot coals, and its breath can reduce warriors to ash in an instant.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 150,
                baseDamage = 22,
                abilityIds = listOf(ABILITY_FIRE_BREATH_ID),
                level = 8,
                experienceValue = 250,
                challengeRating = 5,
                isAggressive = true,
                lootTableId = LOOT_TABLE_DRAGON_ID,
                minGoldDrop = 100,
                maxGoldDrop = 250
            )
        )

        creatures.forEach { creature ->
            if (CreatureRepository.findById(creature.id) == null) {
                CreatureRepository.create(creature)
            }
        }
    }

    /**
     * Generate images for all Classic Fantasy entities that don't have images yet.
     * Call from an admin endpoint or background task.
     */
    suspend fun generateMissingImages() {
        log.info("Starting image generation for Classic Fantasy entities...")

        // Creature image prompts
        val creaturePrompts = mapOf(
            GOBLIN_WARRIOR_ID to "Goblin warrior, small green-skinned humanoid, crude scimitar, leather armor, malicious grin, fantasy art, detailed",
            GIANT_RAT_ID to "Giant rat monster, enormous rodent, red beady eyes, yellowed fangs, mangy fur, dungeon creature, fantasy art",
            SKELETON_WARRIOR_ID to "Skeleton warrior, animated bones, rusted sword, tattered armor remnants, glowing eye sockets, undead, dark fantasy",
            KOBOLD_TRAPPER_ID to "Kobold trapper, small reptilian humanoid, cunning expression, holding trap mechanism, scales and leather, fantasy art",
            GIANT_SPIDER_ID to "Giant spider monster, massive arachnid, venomous fangs, multiple eyes, web strands, dark dungeon, fantasy horror",
            ORC_BERSERKER_ID to "Orc berserker, hulking grey-green brute, battle rage expression, dual axes, war paint, muscular, dark fantasy",
            ZOMBIE_BRUTE_ID to "Zombie brute, massive reanimated corpse, bloated flesh, torn clothing, mindless expression, undead horror",
            GHOUL_ID to "Ghoul undead, corpse-eater, elongated claws, hunched posture, pallid rotting flesh, glowing eyes, horror fantasy",
            HOBGOBLIN_CAPTAIN_ID to "Hobgoblin captain, military commander, orange-red skin, disciplined posture, ornate armor, longsword, fantasy soldier",
            OCHRE_JELLY_ID to "Ochre jelly ooze monster, acidic blob, yellow-brown color, translucent, dissolving bones inside, dungeon creature",
            TROLL_ID to "Troll monster, gangly giant, rubbery green skin, long arms, regenerating wounds, forest troll, dark fantasy",
            WRAITH_ID to "Wraith spirit, incorporeal undead, tattered robes, skeletal hands, ethereal glow, malevolent presence, dark fantasy",
            MINOTAUR_ID to "Minotaur, bull-headed humanoid, massive horns, muscular body, labyrinth guardian, greataxe, greek mythology fantasy",
            BASILISK_ID to "Basilisk monster, eight-legged reptile, petrifying gaze, scales, snake-like body, glowing eyes, fantasy creature",
            YOUNG_RED_DRAGON_ID to "Young red dragon, crimson scales, wings spread, fire breath, fearsome, treasure hoard, classic fantasy dragon"
        )

        // Item image prompts
        val itemPrompts = mapOf(
            MINOR_HEALING_POTION_ID to "Minor healing potion, small glass vial, glowing red liquid, cork stopper, fantasy item illustration",
            MAJOR_HEALING_POTION_ID to "Major healing potion, large flask, bright crimson elixir, ornate bottle, magical glow, fantasy item",
            ANTIDOTE_VIAL_ID to "Antidote potion, clear liquid in glass vial, green tint, antitoxin, fantasy alchemy item",
            GIANT_STRENGTH_ELIXIR_ID to "Elixir of giant strength, murky brown potion, muscle symbol on label, thick glass bottle, fantasy item",
            FIREBALL_SCROLL_ID to "Fireball spell scroll, ancient parchment, fire runes, glowing text, magical scroll, fantasy item",
            ORCISH_CLEAVER_ID to "Orcish cleaver, brutal heavy blade, crude iron, leather wrapped handle, orc weapon, fantasy",
            SILVER_DAGGER_ID to "Silver dagger, polished blade, ornate hilt, moonlight reflection, vampire hunter weapon, fantasy",
            FLAMING_SWORD_ID to "Flaming sword, magical fire wreathing blade, glowing steel, eternal flames, legendary weapon, fantasy",
            STAFF_OF_FROST_ID to "Staff of frost, carved from eternal ice, frozen crystals, cold mist, mage staff, fantasy magic item",
            MINOTAUR_GREATAXE_ID to "Minotaur greataxe, massive double-headed axe, bone decorations, brutal weapon, fantasy",
            GOBLIN_HIDE_ARMOR_ID to "Goblin hide armor, crude leather, scavenged materials, patches, primitive armor, fantasy",
            CHAINMAIL_FALLEN_ID to "Chainmail of the fallen, warrior armor, ethereal glow, battle-worn, haunted armor, fantasy",
            DRAGONSCALE_SHIELD_ID to "Dragonscale shield, red dragon scales, fireproof, ornate design, legendary defense, fantasy",
            HELM_DARKVISION_ID to "Helm of darkvision, enchanted helmet, glowing eye slots, magical runes, fantasy armor",
            RING_OF_REGEN_ID to "Ring of regeneration, golden band, pulsing emerald gem, healing magic, fantasy jewelry",
            AMULET_POISON_PROOF_ID to "Amulet of poison resistance, silver pendant, snake motif, protective charm, fantasy jewelry",
            CLOAK_ELVENKIND_ID to "Cloak of elvenkind, grey-green fabric, shimmering, forest camouflage, elven magic, fantasy",
            GAUNTLETS_OGRE_POWER_ID to "Gauntlets of ogre power, heavy iron gloves, rune inscribed, strength magic, fantasy armor",
            ANCIENT_GOLD_COIN_ID to "Ancient gold coin, weathered currency, forgotten empire symbol, treasure, fantasy loot",
            RUBY_GEMSTONE_ID to "Ruby gemstone, flawless cut, inner fire glow, precious jewel, fantasy treasure",
            DRAGON_TOOTH_ID to "Dragon tooth, massive fang, ivory color, powerful alchemical ingredient, fantasy trophy"
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

        log.info("Classic Fantasy image generation complete")
    }
}
