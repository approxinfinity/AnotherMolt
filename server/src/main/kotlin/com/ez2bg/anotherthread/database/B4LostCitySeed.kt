package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for B4: The Lost City module.
 *
 * A mysterious step-pyramid rises from a desert ruins, ruled by masked beings.
 * Three factions (Brotherhood of Gorm, Magi of Usamigaras, Warrior Maidens of Madarua)
 * resist the ruling Cynidiceans who have descended into drug-fueled madness.
 * For character levels 1-3.
 *
 * Original module by Tom Moldvay (1982), TSR Hobbies.
 */
object B4LostCitySeed {
    private val log = LoggerFactory.getLogger(B4LostCitySeed::class.java)

    private const val ATTRIBUTION = "B4: The Lost City (Tom Moldvay, TSR 1982)"

    // ============== LOCATION IDS ==============
    const val DESERT_RUINS_ID = "b4-desert-ruins"
    const val PYRAMID_ENTRANCE_ID = "b4-pyramid-entrance"
    const val TIER_ONE_ID = "b4-tier-one"
    const val TIER_TWO_ID = "b4-tier-two"
    const val TIER_THREE_ID = "b4-tier-three"
    const val TIER_FOUR_ID = "b4-tier-four"
    const val TIER_FIVE_ID = "b4-tier-five"
    const val BROTHERHOOD_HIDEOUT_ID = "b4-brotherhood-hideout"
    const val MAGI_QUARTERS_ID = "b4-magi-quarters"
    const val WARRIOR_MAIDENS_HALL_ID = "b4-warrior-maidens-hall"
    const val CYNIDICEAN_THRONE_ID = "b4-cynidicean-throne"
    const val ZARGON_TEMPLE_ID = "b4-zargon-temple"

    // ============== CREATURE IDS ==============
    // Tier 1 - Levels 1-2
    const val FIRE_BEETLE_ID = "creature-b4-fire-beetle"
    const val GIANT_CENTIPEDE_ID = "creature-b4-giant-centipede"
    const val GIANT_GECKO_ID = "creature-b4-giant-gecko"
    const val CYNIDICEAN_CITIZEN_ID = "creature-b4-cynidicean-citizen"

    // Tier 2 - Levels 2-3
    const val CYNIDICEAN_GUARD_ID = "creature-b4-cynidicean-guard"
    const val CYNIDICEAN_PRIEST_ID = "creature-b4-cynidicean-priest"
    const val GIANT_SPIDER_ID = "creature-b4-giant-spider"
    const val POLYMAR_ID = "creature-b4-polymar"

    // Tier 3 - Boss
    const val HIGH_PRIEST_DEMETRIUS_ID = "creature-b4-demetrius"
    const val ZARGON_ID = "creature-b4-zargon"

    // Friendly NPCs (faction leaders)
    const val KANADIUS_ID = "creature-b4-kanadius"  // Brotherhood of Gorm
    const val AURIGA_ID = "creature-b4-auriga"      // Magi of Usamigaras
    const val PANDORA_ID = "creature-b4-pandora"    // Warrior Maidens of Madarua

    // ============== ITEM IDS ==============
    const val MASK_OF_GORM_ID = "item-b4-mask-gorm"
    const val MASK_OF_USAMIGARAS_ID = "item-b4-mask-usamigaras"
    const val MASK_OF_MADARUA_ID = "item-b4-mask-madarua"
    const val CYNIDICEAN_MASK_ID = "item-b4-cynidicean-mask"
    const val DRUG_MUSHROOM_ID = "item-b4-drug-mushroom"
    const val ANCIENT_GOLD_COIN_ID = "item-b4-ancient-gold"
    const val PYRAMID_TREASURE_ID = "item-b4-pyramid-treasure"
    const val HORN_OF_ZARGON_ID = "item-b4-horn-zargon"
    const val STAFF_OF_USAMIGARAS_ID = "item-b4-staff-usamigaras"
    const val SWORD_OF_GORM_ID = "item-b4-sword-gorm"
    const val SHIELD_OF_MADARUA_ID = "item-b4-shield-madarua"

    // ============== ABILITY IDS ==============
    const val ABILITY_ZARGON_REGENERATE_ID = "ability-b4-zargon-regenerate"
    const val ABILITY_ZARGON_HORN_ATTACK_ID = "ability-b4-zargon-horn"
    const val ABILITY_CYNIDICEAN_HALLUCINATION_ID = "ability-b4-hallucination"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_VERMIN_ID = "loot-b4-vermin"
    const val LOOT_TABLE_CYNIDICEAN_ID = "loot-b4-cynidicean"
    const val LOOT_TABLE_PRIEST_ID = "loot-b4-priest"
    const val LOOT_TABLE_PYRAMID_ID = "loot-b4-pyramid"
    const val LOOT_TABLE_ZARGON_ID = "loot-b4-zargon"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (FIRE_BEETLE_ID !in existingCreatures) {
            log.info("Seeding B4: The Lost City content...")
            seedAbilities()
            seedItems()
            seedLootTables()
            seedCreatures()
            seedLocations()
            log.info("Seeded B4: The Lost City content")
        }
    }

    private fun seedAbilities() {
        val abilities = listOf(
            Ability(
                id = ABILITY_ZARGON_REGENERATE_ID,
                name = "Zargon's Regeneration",
                description = "Zargon regenerates health each round unless his horn is severed.",
                classId = null,
                abilityType = "passive",
                targetType = "self",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 0,
                effects = """[{"type":"regen","amount":5}]""",
                attribution = ATTRIBUTION
            ),
            Ability(
                id = ABILITY_ZARGON_HORN_ATTACK_ID,
                name = "Horn Gore",
                description = "Zargon impales victims on his terrible horn, dealing massive damage.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 25,
                effects = """[{"type":"damage","modifier":25}]""",
                attribution = ATTRIBUTION
            ),
            Ability(
                id = ABILITY_CYNIDICEAN_HALLUCINATION_ID,
                name = "Drug-Induced Hallucination",
                description = "The Cynidicean's drugged state causes unpredictable behavior.",
                classId = null,
                abilityType = "passive",
                targetType = "self",
                range = 0,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 0,
                effects = """[{"type":"condition","condition":"confused"}]""",
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
            // Faction Masks
            Item(
                id = MASK_OF_GORM_ID,
                name = "Mask of Gorm",
                desc = "A bronze mask bearing the stern visage of Gorm, god of war and storms. Worn by the Brotherhood of Gorm.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "head",
                statBonuses = StatBonuses(attack = 2, defense = 2),
                value = 100,
                attribution = ATTRIBUTION
            ),
            Item(
                id = MASK_OF_USAMIGARAS_ID,
                name = "Mask of Usamigaras",
                desc = "A silver mask with a child's innocent face. Worn by the Magi of Usamigaras, god of healing and messengers.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "head",
                statBonuses = StatBonuses(maxMana = 15, maxHp = 5),
                value = 100,
                attribution = ATTRIBUTION
            ),
            Item(
                id = MASK_OF_MADARUA_ID,
                name = "Mask of Madarua",
                desc = "A golden mask of a fierce woman warrior. Worn by the Warrior Maidens of Madarua, goddess of birth and death.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "head",
                statBonuses = StatBonuses(attack = 3, maxHp = 5),
                value = 100,
                attribution = ATTRIBUTION
            ),
            Item(
                id = CYNIDICEAN_MASK_ID,
                name = "Cynidicean Mask",
                desc = "A bizarre painted mask depicting an animal, monster, or abstract shape. The mad Cynidiceans never remove their masks.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "head",
                value = 15,
                attribution = ATTRIBUTION
            ),
            // Consumables
            Item(
                id = DRUG_MUSHROOM_ID,
                name = "Dream Mushroom",
                desc = "A glowing fungus that causes hallucinations. The Cynidiceans are addicted to these, living in a constant drug-induced stupor.",
                featureIds = emptyList(),
                equipmentType = "consumable",
                value = 5,
                attribution = ATTRIBUTION
            ),
            // Treasure
            Item(
                id = ANCIENT_GOLD_COIN_ID,
                name = "Ancient Cynidicean Coin",
                desc = "A gold coin bearing the likeness of King Alexander, last sane ruler of Cynidicea.",
                featureIds = emptyList(),
                value = 25,
                isStackable = true,
                attribution = ATTRIBUTION
            ),
            Item(
                id = PYRAMID_TREASURE_ID,
                name = "Pyramid Treasure",
                desc = "Ancient gold and jewels from the pyramid's burial chambers.",
                featureIds = emptyList(),
                value = 100,
                isStackable = true,
                attribution = ATTRIBUTION
            ),
            // Legendary Items
            Item(
                id = HORN_OF_ZARGON_ID,
                name = "Horn of Zargon",
                desc = "The severed horn of the monster-god Zargon. It pulses with dark power and prevents Zargon's regeneration while held.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                statBonuses = StatBonuses(attack = 5, defense = 5),
                value = 5000,
                attribution = ATTRIBUTION
            ),
            Item(
                id = STAFF_OF_USAMIGARAS_ID,
                name = "Staff of Usamigaras",
                desc = "The sacred staff of the Magi, topped with a crystal orb. Enhances magical ability and can cast healing spells.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 4, maxMana = 25),
                value = 800,
                attribution = ATTRIBUTION
            ),
            Item(
                id = SWORD_OF_GORM_ID,
                name = "Sword of Gorm",
                desc = "A thundering blade sacred to the Brotherhood. Lightning crackles along its edge.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 10, defense = 2),
                value = 1000,
                attribution = ATTRIBUTION
            ),
            Item(
                id = SHIELD_OF_MADARUA_ID,
                name = "Shield of Madarua",
                desc = "The battle shield of the Warrior Maidens, blessed by the goddess. Bears the symbol of the eternal cycle.",
                featureIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "off_hand",
                statBonuses = StatBonuses(defense = 8, maxHp = 10),
                value = 900,
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
                id = LOOT_TABLE_VERMIN_ID,
                name = "Pyramid Vermin Loot",
                entries = listOf(
                    LootEntry(ANCIENT_GOLD_COIN_ID, 0.15f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_CYNIDICEAN_ID,
                name = "Cynidicean Loot",
                entries = listOf(
                    LootEntry(CYNIDICEAN_MASK_ID, 0.20f),
                    LootEntry(DRUG_MUSHROOM_ID, 0.40f),
                    LootEntry(ANCIENT_GOLD_COIN_ID, 0.30f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_PRIEST_ID,
                name = "Cynidicean Priest Loot",
                entries = listOf(
                    LootEntry(CYNIDICEAN_MASK_ID, 0.40f),
                    LootEntry(DRUG_MUSHROOM_ID, 0.50f),
                    LootEntry(ANCIENT_GOLD_COIN_ID, 0.60f),
                    LootEntry(PYRAMID_TREASURE_ID, 0.20f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_PYRAMID_ID,
                name = "Pyramid Treasure",
                entries = listOf(
                    LootEntry(ANCIENT_GOLD_COIN_ID, 0.80f),
                    LootEntry(PYRAMID_TREASURE_ID, 0.50f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_ZARGON_ID,
                name = "Zargon's Hoard",
                entries = listOf(
                    LootEntry(HORN_OF_ZARGON_ID, 1.0f),
                    LootEntry(PYRAMID_TREASURE_ID, 1.0f),
                    LootEntry(ANCIENT_GOLD_COIN_ID, 1.0f)
                )
            )
        )

        lootTables.forEach { table ->
            if (LootTableRepository.findById(table.id) == null) {
                LootTableRepository.create(table)
            }
        }
    }

    private fun seedCreatures() {
        val creatures = listOf(
            // Tier 1 - Pyramid Vermin
            Creature(
                id = FIRE_BEETLE_ID,
                name = "Fire Beetle",
                description = "A large beetle with glowing glands that illuminate the dark pyramid corridors. These creatures have infested the upper tiers.",
                level = 1,
                hitDice = 1,
                armorClass = 14,
                attackBonus = 2,
                damage = "1d6",
                maxHp = 6,
                currentHp = 6,
                xpValue = 20,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_VERMIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GIANT_CENTIPEDE_ID,
                name = "Giant Centipede",
                description = "A venomous centipede the length of a man's arm. Its bite causes painful swelling.",
                level = 1,
                hitDice = 1,
                armorClass = 12,
                attackBonus = 3,
                damage = "1d4",
                maxHp = 4,
                currentHp = 4,
                xpValue = 25,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_VERMIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GIANT_GECKO_ID,
                name = "Giant Gecko",
                description = "A large desert lizard that clings to walls and ceilings. It drops on unsuspecting prey.",
                level = 1,
                hitDice = 2,
                armorClass = 14,
                attackBonus = 3,
                damage = "1d6",
                maxHp = 8,
                currentHp = 8,
                xpValue = 30,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_VERMIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = CYNIDICEAN_CITIZEN_ID,
                name = "Cynidicean Citizen",
                description = "A pale, underground-dwelling human wearing a bizarre mask. The Cynidiceans live in a drug-induced fantasy, their sanity long gone. This one believes they are an animal or mythical creature.",
                level = 1,
                hitDice = 1,
                armorClass = 11,
                attackBonus = 1,
                damage = "1d4",
                maxHp = 5,
                currentHp = 5,
                xpValue = 15,
                abilityIds = listOf(ABILITY_CYNIDICEAN_HALLUCINATION_ID),
                lootTableId = LOOT_TABLE_CYNIDICEAN_ID,
                attribution = ATTRIBUTION
            ),

            // Tier 2
            Creature(
                id = CYNIDICEAN_GUARD_ID,
                name = "Cynidicean Guard",
                description = "A warrior of the Cynidicean people, drugged but still dangerous. Wears armor over pale skin and a mask depicting a fierce monster.",
                level = 2,
                hitDice = 2,
                armorClass = 15,
                attackBonus = 4,
                damage = "1d8",
                maxHp = 12,
                currentHp = 12,
                xpValue = 50,
                abilityIds = listOf(ABILITY_CYNIDICEAN_HALLUCINATION_ID),
                lootTableId = LOOT_TABLE_CYNIDICEAN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = CYNIDICEAN_PRIEST_ID,
                name = "Priest of Zargon",
                description = "A dark priest serving the monster-god Zargon. Wears elaborate robes and a tentacled mask. Chants dark prayers in the ancient tongue.",
                level = 3,
                hitDice = 3,
                armorClass = 14,
                attackBonus = 4,
                damage = "1d6+1",
                maxHp = 18,
                currentHp = 18,
                xpValue = 100,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_PRIEST_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GIANT_SPIDER_ID,
                name = "Black Widow Spider",
                description = "A huge spider with a venomous bite. Its webs cover entire corridors in the lower tiers.",
                level = 2,
                hitDice = 3,
                armorClass = 14,
                attackBonus = 4,
                damage = "1d6",
                maxHp = 14,
                currentHp = 14,
                xpValue = 75,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_VERMIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = POLYMAR_ID,
                name = "Polymar",
                description = "A strange creature that appears as a pool of water but is actually an acidic blob. Created by ancient Cynidicean magic.",
                level = 3,
                hitDice = 4,
                armorClass = 12,
                attackBonus = 5,
                damage = "1d8",
                maxHp = 22,
                currentHp = 22,
                xpValue = 120,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_PYRAMID_ID,
                attribution = ATTRIBUTION
            ),

            // Boss Tier
            Creature(
                id = HIGH_PRIEST_DEMETRIUS_ID,
                name = "High Priest Demetrius",
                description = "The leader of Zargon's cult, dressed in rich purple robes and wearing a golden tentacled mask. He keeps the people drugged and worships the monster below.",
                level = 5,
                hitDice = 6,
                armorClass = 16,
                attackBonus = 7,
                damage = "1d8+2",
                maxHp = 40,
                currentHp = 40,
                xpValue = 500,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_PRIEST_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ZARGON_ID,
                name = "Zargon the Returner",
                description = "An ancient monster-god with a giant horn and numerous tentacles. Zargon regenerates from any wound unless his horn is severed. He has ruled the Cynidiceans for centuries.",
                level = 10,
                hitDice = 12,
                armorClass = 18,
                attackBonus = 12,
                damage = "2d10+5",
                maxHp = 100,
                currentHp = 100,
                xpValue = 3000,
                abilityIds = listOf(ABILITY_ZARGON_REGENERATE_ID, ABILITY_ZARGON_HORN_ATTACK_ID),
                lootTableId = LOOT_TABLE_ZARGON_ID,
                attribution = ATTRIBUTION
            ),

            // Friendly Faction Leaders (NPCs)
            Creature(
                id = KANADIUS_ID,
                name = "Kanadius the Warrior",
                description = "Leader of the Brotherhood of Gorm, a secret society trying to restore Cynidicea. He wears the bronze mask of Gorm and fights against Zargon's priests.",
                level = 4,
                hitDice = 4,
                armorClass = 17,
                attackBonus = 7,
                damage = "1d8+3",
                maxHp = 32,
                currentHp = 32,
                xpValue = 0, // Friendly NPC
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = AURIGA_ID,
                name = "Auriga Sirkinos",
                description = "Leader of the Magi of Usamigaras, practitioners of healing magic. Wears the silver child-mask and seeks to cure the Cynidiceans' addiction.",
                level = 4,
                hitDice = 4,
                armorClass = 14,
                attackBonus = 5,
                damage = "1d6+2",
                maxHp = 26,
                currentHp = 26,
                xpValue = 0,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = PANDORA_ID,
                name = "Pandora",
                description = "Leader of the Warrior Maidens of Madarua, an all-female fighting order. Wears the golden mask of the war goddess and trains warriors to fight Zargon.",
                level = 4,
                hitDice = 4,
                armorClass = 16,
                attackBonus = 7,
                damage = "1d8+2",
                maxHp = 30,
                currentHp = 30,
                xpValue = 0,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            )
        )

        creatures.forEach { creature ->
            if (CreatureRepository.findById(creature.id) == null) {
                CreatureRepository.create(creature)
            }
        }
    }

    private fun seedLocations() {
        val locations = listOf(
            Location(
                id = DESERT_RUINS_ID,
                name = "Desert Ruins of Cynidicea",
                desc = "Rising from the endless dunes, ancient ruins mark where the city of Cynidicea once stood. Only the great step-pyramid remains intact, its black stone absorbing the desert sun. Legend says food, water, and treasure await inside—but so does ancient evil.",
                locationType = LocationType.OUTDOOR,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(PYRAMID_ENTRANCE_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = PYRAMID_ENTRANCE_ID,
                name = "Pyramid Entrance",
                desc = "Stone steps lead up to a dark doorway in the pyramid's face. Cool air flows from within, carrying the smell of dust and decay. Ancient carvings depict robed figures worshipping a tentacled monster.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(GIANT_GECKO_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(DESERT_RUINS_ID, ExitDirection.SOUTH),
                    Exit(TIER_ONE_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TIER_ONE_ID,
                name = "Pyramid - Tier One",
                desc = "The first level of the pyramid. Corridors stretch into darkness, lit only by the glow of fire beetles. Empty chambers contain old burial goods and the detritus of past explorers.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(FIRE_BEETLE_ID, GIANT_CENTIPEDE_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(PYRAMID_ENTRANCE_ID, ExitDirection.UP),
                    Exit(TIER_TWO_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TIER_TWO_ID,
                name = "Pyramid - Tier Two",
                desc = "Deeper into the pyramid, the air grows heavier. Strange masked figures occasionally dart through the shadows—the mad Cynidiceans, living in their drug-induced dreams.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(CYNIDICEAN_CITIZEN_ID, CYNIDICEAN_CITIZEN_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(TIER_ONE_ID, ExitDirection.UP),
                    Exit(TIER_THREE_ID, ExitDirection.DOWN),
                    Exit(BROTHERHOOD_HIDEOUT_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TIER_THREE_ID,
                name = "Pyramid - Tier Three",
                desc = "The middle level of the pyramid. Elaborate murals depict the fall of Cynidicea—the coming of Zargon, the descent into madness, the three factions who resist.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(CYNIDICEAN_GUARD_ID, GIANT_SPIDER_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(TIER_TWO_ID, ExitDirection.UP),
                    Exit(TIER_FOUR_ID, ExitDirection.DOWN),
                    Exit(MAGI_QUARTERS_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TIER_FOUR_ID,
                name = "Pyramid - Tier Four",
                desc = "The lower levels are more heavily guarded. Cynidicean warriors patrol the corridors, and priests chant in hidden chambers. The smell of incense and drugs is overpowering.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(CYNIDICEAN_GUARD_ID, CYNIDICEAN_PRIEST_ID, POLYMAR_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(TIER_THREE_ID, ExitDirection.UP),
                    Exit(TIER_FIVE_ID, ExitDirection.DOWN),
                    Exit(WARRIOR_MAIDENS_HALL_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TIER_FIVE_ID,
                name = "Pyramid - Tier Five",
                desc = "The deepest level, where the priests hold court. Dark chanting echoes through the corridors, and the walls are decorated with disturbing images of Zargon.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(CYNIDICEAN_GUARD_ID, CYNIDICEAN_PRIEST_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(TIER_FOUR_ID, ExitDirection.UP),
                    Exit(CYNIDICEAN_THRONE_ID, ExitDirection.NORTH),
                    Exit(ZARGON_TEMPLE_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            // Faction Hideouts
            Location(
                id = BROTHERHOOD_HIDEOUT_ID,
                name = "Brotherhood of Gorm Hideout",
                desc = "A secret chamber decorated with lightning bolt symbols. Warriors in bronze masks train with sword and shield. Their leader Kanadius welcomes those who oppose Zargon.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(KANADIUS_ID),
                itemIds = listOf(MASK_OF_GORM_ID),
                exits = listOf(
                    Exit(TIER_TWO_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = MAGI_QUARTERS_ID,
                name = "Magi of Usamigaras Quarters",
                desc = "A hidden sanctuary where silver-masked healers tend to the sick. Auriga studies ancient texts, seeking a cure for the Cynidiceans' madness.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(AURIGA_ID),
                itemIds = listOf(MASK_OF_USAMIGARAS_ID),
                exits = listOf(
                    Exit(TIER_THREE_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = WARRIOR_MAIDENS_HALL_ID,
                name = "Hall of the Warrior Maidens",
                desc = "A training hall where female warriors practice combat. Golden masks hang on the walls, and Pandora oversees the exercises with a critical eye.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(PANDORA_ID),
                itemIds = listOf(MASK_OF_MADARUA_ID),
                exits = listOf(
                    Exit(TIER_FOUR_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            // Boss Areas
            Location(
                id = CYNIDICEAN_THRONE_ID,
                name = "Cynidicean Throne Room",
                desc = "The High Priest Demetrius holds court here, surrounded by drugged followers. Rich tapestries show Zargon devouring his enemies. The air is thick with incense.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(HIGH_PRIEST_DEMETRIUS_ID, CYNIDICEAN_GUARD_ID),
                itemIds = listOf(PYRAMID_TREASURE_ID),
                exits = listOf(
                    Exit(TIER_FIVE_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = ZARGON_TEMPLE_ID,
                name = "Temple of Zargon",
                desc = "A massive underground temple built around a boiling mud pit. Here dwells Zargon the Returner—a nightmare of tentacles, horn, and unending hunger. The monster-god has ruled here for a thousand years.",
                locationType = LocationType.DUNGEON,
                areaId = "b4-lost-city",
                featureIds = emptyList(),
                creatureIds = listOf(ZARGON_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(TIER_FIVE_ID, ExitDirection.UP)
                ),
                attribution = ATTRIBUTION
            )
        )

        locations.forEach { location ->
            if (LocationRepository.findById(location.id) == null) {
                LocationRepository.create(location)
            }
        }
    }
}
