package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for X1: The Isle of Dread module.
 *
 * A wilderness adventure on a lost island filled with dinosaurs, cannibals,
 * and ancient temples. Explorers follow a tattered ship's log seeking a
 * legendary black pearl hidden in a temple on a central plateau.
 * For character levels 3-7.
 *
 * Original module by David Cook and Tom Moldvay (1981), TSR Hobbies.
 */
object X1IsleOfDreadSeed {
    private val log = LoggerFactory.getLogger(X1IsleOfDreadSeed::class.java)

    private const val ATTRIBUTION = "X1: The Isle of Dread (David Cook & Tom Moldvay, TSR 1981)"

    // ============== LOCATION IDS ==============
    // Coastal & Village Areas
    const val SPECULARUM_PORT_ID = "x1-specularum"
    const val OCEAN_VOYAGE_ID = "x1-ocean"
    const val ISLE_COAST_ID = "x1-coast"
    const val VILLAGE_TANAROA_ID = "x1-tanaroa"
    const val VILLAGE_MANTRU_ID = "x1-mantru"
    const val PIRATE_COVE_ID = "x1-pirate-cove"

    // Jungle & Wilderness
    const val JUNGLE_PATH_ID = "x1-jungle-path"
    const val SWAMP_ID = "x1-swamp"
    const val TAR_PITS_ID = "x1-tar-pits"
    const val RAKASTA_CAMP_ID = "x1-rakasta"
    const val PHANATON_VILLAGE_ID = "x1-phanaton"
    const val ARANEA_LAIR_ID = "x1-aranea"

    // Central Plateau & Temple
    const val CENTRAL_PLATEAU_ID = "x1-plateau"
    const val TABOO_ISLAND_ID = "x1-taboo-island"
    const val TEMPLE_LEVEL_ONE_ID = "x1-temple-1"
    const val TEMPLE_LEVEL_TWO_ID = "x1-temple-2"
    const val TEMPLE_LEVEL_THREE_ID = "x1-temple-3"
    const val KOPRU_LAIR_ID = "x1-kopru-lair"

    // ============== CREATURE IDS ==============
    // Dinosaurs
    const val ALLOSAURUS_ID = "creature-x1-allosaurus"
    const val TRICERATOPS_ID = "creature-x1-triceratops"
    const val TYRANNOSAURUS_ID = "creature-x1-tyrannosaurus"
    const val PTERANODON_ID = "creature-x1-pteranodon"
    const val STEGOSAURUS_ID = "creature-x1-stegosaurus"

    // Native Creatures
    const val NATIVE_WARRIOR_ID = "creature-x1-native-warrior"
    const val NATIVE_MATRIARCH_ID = "creature-x1-matriarch"
    const val ZOMBIE_NATIVE_ID = "creature-x1-zombie-native"
    const val PIRATE_ID = "creature-x1-pirate"
    const val PIRATE_CAPTAIN_ID = "creature-x1-pirate-captain"

    // Unique Island Creatures
    const val RAKASTA_WARRIOR_ID = "creature-x1-rakasta"
    const val PHANATON_ID = "creature-x1-phanaton"
    const val ARANEA_ID = "creature-x1-aranea"

    // Temple & Boss Creatures
    const val KOPRU_ID = "creature-x1-kopru"
    const val KOPRU_ELDER_ID = "creature-x1-kopru-elder"

    // ============== ITEM IDS ==============
    const val SHIPS_LOG_ID = "item-x1-ships-log"
    const val BLACK_PEARL_ID = "item-x1-black-pearl"
    const val NATIVE_SPEAR_ID = "item-x1-native-spear"
    const val DINOSAUR_TOOTH_ID = "item-x1-dinosaur-tooth"
    const val DINOSAUR_HIDE_ID = "item-x1-dinosaur-hide"
    const val PIRATE_TREASURE_ID = "item-x1-pirate-treasure"
    const val TEMPLE_GOLD_ID = "item-x1-temple-gold"
    const val KOPRU_CHARM_ID = "item-x1-kopru-charm"

    // ============== ABILITY IDS ==============
    const val ABILITY_KOPRU_CHARM_ID = "ability-x1-kopru-charm"
    const val ABILITY_TYRANNO_BITE_ID = "ability-x1-tyranno-bite"
    const val ABILITY_ARANEA_WEB_ID = "ability-x1-aranea-web"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_DINOSAUR_ID = "loot-x1-dinosaur"
    const val LOOT_TABLE_NATIVE_ID = "loot-x1-native"
    const val LOOT_TABLE_PIRATE_ID = "loot-x1-pirate"
    const val LOOT_TABLE_TEMPLE_ID = "loot-x1-temple"
    const val LOOT_TABLE_KOPRU_ID = "loot-x1-kopru"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (ALLOSAURUS_ID !in existingCreatures) {
            log.info("Seeding X1: The Isle of Dread content...")
            seedAbilities()
            seedItems()
            seedLootTables()
            seedCreatures()
            seedLocations()
            log.info("Seeded X1: The Isle of Dread content")
        }
    }

    private fun seedAbilities() {
        val abilities = listOf(
            Ability(
                id = ABILITY_KOPRU_CHARM_ID,
                name = "Kopru Charm",
                description = "The kopru's deadly charm power can take control of humanoid minds, turning them against their allies. Only the strong-willed can resist.",
                classId = null,
                abilityType = "control",
                targetType = "single_enemy",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 0,
                durationRounds = 5,
                effects = """[{"type":"condition","condition":"charmed","duration":5,"saveType":"wisdom"}]""",
                attribution = ATTRIBUTION
            ),
            Ability(
                id = ABILITY_TYRANNO_BITE_ID,
                name = "Tyrannosaurus Bite",
                description = "The tyrannosaurus delivers a devastating bite that can swallow smaller creatures whole.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 40,
                effects = """[{"type":"damage","modifier":40}]""",
                attribution = ATTRIBUTION
            ),
            Ability(
                id = ABILITY_ARANEA_WEB_ID,
                name = "Web Trap",
                description = "The aranea spins a magical web that ensnares and immobilizes its prey.",
                classId = null,
                abilityType = "control",
                targetType = "single_enemy",
                range = 20,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 0,
                durationRounds = 3,
                effects = """[{"type":"condition","condition":"restrained","duration":3,"saveType":"dexterity"}]""",
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
            Item(
                id = SHIPS_LOG_ID,
                name = "Tattered Ship's Log",
                desc = "A water-damaged journal from a long-dead explorer. It describes the Isle of Dread and hints at a great treasure—a legendary black pearl hidden in an ancient temple.",
                featureIds = emptyList(),
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = BLACK_PEARL_ID,
                name = "The Black Pearl",
                desc = "A legendary pearl the size of a fist, darker than night and cold to the touch. It pulses with ancient power. This is what the natives fear and the kopru worship.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                statBonuses = StatBonuses(maxMana = 50, attack = 5, defense = 5),
                value = 50000,
                attribution = ATTRIBUTION
            ),
            Item(
                id = NATIVE_SPEAR_ID,
                name = "Native Spear",
                desc = "A hardwood spear with a sharpened obsidian tip. The natives of Tanaroa are skilled hunters.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 4),
                value = 10,
                attribution = ATTRIBUTION
            ),
            Item(
                id = DINOSAUR_TOOTH_ID,
                name = "Dinosaur Tooth",
                desc = "A massive tooth from a carnivorous dinosaur. Could be used as a dagger or sold as a curiosity.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 5),
                value = 75,
                attribution = ATTRIBUTION
            ),
            Item(
                id = DINOSAUR_HIDE_ID,
                name = "Dinosaur Hide",
                desc = "Thick, scaly hide from a large dinosaur. Can be crafted into exceptional armor by a skilled leatherworker.",
                featureIds = emptyList(),
                equipmentType = "material",
                value = 200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = PIRATE_TREASURE_ID,
                name = "Pirate Treasure",
                desc = "Gold coins, jewelry, and gems plundered by the pirates who use the island as a hideout.",
                featureIds = emptyList(),
                value = 300,
                isStackable = true,
                attribution = ATTRIBUTION
            ),
            Item(
                id = TEMPLE_GOLD_ID,
                name = "Temple Artifacts",
                desc = "Ancient golden artifacts from the temple on Taboo Island. They bear strange symbols and seem to predate the native culture.",
                featureIds = emptyList(),
                value = 500,
                isStackable = true,
                attribution = ATTRIBUTION
            ),
            Item(
                id = KOPRU_CHARM_ID,
                name = "Kopru Mind Stone",
                desc = "A strange organic gem that pulses with psychic energy. The kopru use these to enhance their charm abilities. Handle with care.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                statBonuses = StatBonuses(maxMana = 20),
                value = 800,
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
                id = LOOT_TABLE_DINOSAUR_ID,
                name = "Dinosaur Loot",
                entries = listOf(
                    LootEntry(DINOSAUR_TOOTH_ID, 0.08f),
                    LootEntry(DINOSAUR_HIDE_ID, 0.06f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_NATIVE_ID,
                name = "Native Loot",
                entries = listOf(
                    LootEntry(NATIVE_SPEAR_ID, 0.15f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_PIRATE_ID,
                name = "Pirate Loot",
                entries = listOf(
                    LootEntry(PIRATE_TREASURE_ID, 0.50f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_TEMPLE_ID,
                name = "Temple Loot",
                entries = listOf(
                    LootEntry(TEMPLE_GOLD_ID, 0.60f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_KOPRU_ID,
                name = "Kopru Loot",
                entries = listOf(
                    LootEntry(KOPRU_CHARM_ID, 0.25f),
                    LootEntry(TEMPLE_GOLD_ID, 0.70f),
                    LootEntry(BLACK_PEARL_ID, 0.10f)
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
            // Dinosaurs
            Creature(
                id = ALLOSAURUS_ID,
                name = "Allosaurus",
                description = "A large carnivorous dinosaur with powerful jaws and razor-sharp claws. Hunts in packs through the jungle.",
                level = 5,
                hitDice = 6,
                armorClass = 15,
                attackBonus = 8,
                damage = "2d8+4",
                maxHp = 45,
                currentHp = 45,
                xpValue = 350,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_DINOSAUR_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = TRICERATOPS_ID,
                name = "Triceratops",
                description = "A massive three-horned herbivore. Normally docile but extremely dangerous when threatened or protecting young.",
                level = 6,
                hitDice = 8,
                armorClass = 17,
                attackBonus = 9,
                damage = "2d10+5",
                maxHp = 60,
                currentHp = 60,
                xpValue = 500,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_DINOSAUR_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = TYRANNOSAURUS_ID,
                name = "Tyrannosaurus Rex",
                description = "The king of dinosaurs. A 40-foot terror with jaws that can crush bone. The natives worship it as a god.",
                level = 10,
                hitDice = 14,
                armorClass = 17,
                attackBonus = 13,
                damage = "3d10+8",
                maxHp = 120,
                currentHp = 120,
                xpValue = 2000,
                abilityIds = listOf(ABILITY_TYRANNO_BITE_ID),
                lootTableId = LOOT_TABLE_DINOSAUR_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = PTERANODON_ID,
                name = "Pteranodon",
                description = "A flying reptile with a 25-foot wingspan. Swoops down on prey from above.",
                level = 4,
                hitDice = 4,
                armorClass = 14,
                attackBonus = 6,
                damage = "1d8+3",
                maxHp = 28,
                currentHp = 28,
                xpValue = 175,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_DINOSAUR_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = STEGOSAURUS_ID,
                name = "Stegosaurus",
                description = "A plated herbivore with a spiked tail. The plates can flush with blood as a warning display.",
                level = 5,
                hitDice = 7,
                armorClass = 16,
                attackBonus = 7,
                damage = "2d6+4",
                maxHp = 50,
                currentHp = 50,
                xpValue = 400,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_DINOSAUR_ID,
                attribution = ATTRIBUTION
            ),

            // Natives & Pirates
            Creature(
                id = NATIVE_WARRIOR_ID,
                name = "Native Warrior",
                description = "A warrior of the native tribes. Skilled hunters and fierce defenders of their villages. Some are friendly, others hostile.",
                level = 2,
                hitDice = 2,
                armorClass = 13,
                attackBonus = 4,
                damage = "1d6+1",
                maxHp = 12,
                currentHp = 12,
                xpValue = 40,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_NATIVE_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = NATIVE_MATRIARCH_ID,
                name = "Tribal Matriarch",
                description = "The wise leader of a native village. Commands respect through age and wisdom. May be friend or foe.",
                level = 4,
                hitDice = 5,
                armorClass = 14,
                attackBonus = 6,
                damage = "1d8+2",
                maxHp = 35,
                currentHp = 35,
                xpValue = 0,  // Neutral NPC
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ZOMBIE_NATIVE_ID,
                name = "Zombie",
                description = "The animated corpse of a native, raised by the kopru's dark magic. Shambles toward the living with outstretched arms.",
                level = 2,
                hitDice = 2,
                armorClass = 12,
                attackBonus = 3,
                damage = "1d8",
                maxHp = 14,
                currentHp = 14,
                xpValue = 45,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = PIRATE_ID,
                name = "Pirate",
                description = "A cutthroat who uses the island as a hideout. These criminals prey on native villages and any explorers foolish enough to land.",
                level = 2,
                hitDice = 2,
                armorClass = 14,
                attackBonus = 4,
                damage = "1d8",
                maxHp = 12,
                currentHp = 12,
                xpValue = 50,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_PIRATE_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = PIRATE_CAPTAIN_ID,
                name = "Pirate Captain",
                description = "The ruthless leader of the pirate band. Has a price on their head in multiple kingdoms.",
                level = 5,
                hitDice = 6,
                armorClass = 16,
                attackBonus = 8,
                damage = "1d8+4",
                maxHp = 45,
                currentHp = 45,
                xpValue = 300,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_PIRATE_ID,
                attribution = ATTRIBUTION
            ),

            // Unique Island Races
            Creature(
                id = RAKASTA_WARRIOR_ID,
                name = "Rakasta",
                description = "A cat-like humanoid native to the island. The rakasta are proud warriors who hunt with metal claws. They may ally with worthy explorers.",
                level = 3,
                hitDice = 3,
                armorClass = 15,
                attackBonus = 6,
                damage = "1d6+2",
                maxHp = 20,
                currentHp = 20,
                xpValue = 0,  // Potential ally
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = PHANATON_ID,
                name = "Phanaton",
                description = "A small raccoon-like creature with gliding membranes. The phanaton live in tree villages and are enemies of the aranea.",
                level = 1,
                hitDice = 1,
                armorClass = 14,
                attackBonus = 3,
                damage = "1d4+1",
                maxHp = 6,
                currentHp = 6,
                xpValue = 0,  // Friendly
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ARANEA_ID,
                name = "Aranea",
                description = "An intelligent spider that can take humanoid form and cast spells. They spin webs of deception as easily as silk.",
                level = 4,
                hitDice = 4,
                armorClass = 14,
                attackBonus = 6,
                damage = "1d6+2",
                maxHp = 26,
                currentHp = 26,
                xpValue = 200,
                abilityIds = listOf(ABILITY_ARANEA_WEB_ID),
                lootTableId = LOOT_TABLE_TEMPLE_ID,
                attribution = ATTRIBUTION
            ),

            // Kopru - Final Boss
            Creature(
                id = KOPRU_ID,
                name = "Kopru",
                description = "An ancient aquatic creature with a humanoid torso and fish-like tail. The kopru can charm humanoids into servitude. They make their lair in boiling geysers.",
                level = 6,
                hitDice = 8,
                armorClass = 16,
                attackBonus = 9,
                damage = "1d8+4",
                maxHp = 55,
                currentHp = 55,
                xpValue = 600,
                abilityIds = listOf(ABILITY_KOPRU_CHARM_ID),
                lootTableId = LOOT_TABLE_KOPRU_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = KOPRU_ELDER_ID,
                name = "Kopru Elder",
                description = "An ancient kopru of immense power. Its charm is nearly irresistible. It guards the Black Pearl and dreams of enslaving the surface world.",
                level = 10,
                hitDice = 12,
                armorClass = 18,
                attackBonus = 12,
                damage = "2d6+6",
                maxHp = 95,
                currentHp = 95,
                xpValue = 2500,
                abilityIds = listOf(ABILITY_KOPRU_CHARM_ID),
                lootTableId = LOOT_TABLE_KOPRU_ID,
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
            // Starting Point & Ocean
            Location(
                id = SPECULARUM_PORT_ID,
                name = "Port of Specularum",
                desc = "The bustling capital of the Grand Duchy of Karameikos. In the marketplace, you found a tattered ship's log describing a mysterious island and fabulous treasure. Now your ship is provisioned and the crew eager—the Isle of Dread awaits.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = listOf(SHIPS_LOG_ID),
                exits = listOf(
                    Exit(OCEAN_VOYAGE_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = OCEAN_VOYAGE_ID,
                name = "The Open Sea",
                desc = "Weeks of ocean travel, guided only by the dead explorer's notes. Storm clouds gather on the horizon. Then, rising from the mist like the teeth of a sea serpent—the rocky peaks of the Isle of Dread!",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(SPECULARUM_PORT_ID, ExitDirection.NORTH),
                    Exit(ISLE_COAST_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),

            // Coastal Areas
            Location(
                id = ISLE_COAST_ID,
                name = "Isle of Dread Coast",
                desc = "The ship anchors in a natural harbor. Dense jungle climbs steep hills beyond the beach. Strange bird calls echo from the trees, and something large moves in the undergrowth. A native village is visible along the shore.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(PTERANODON_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(OCEAN_VOYAGE_ID, ExitDirection.NORTH),
                    Exit(VILLAGE_TANAROA_ID, ExitDirection.EAST),
                    Exit(JUNGLE_PATH_ID, ExitDirection.SOUTH),
                    Exit(PIRATE_COVE_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = VILLAGE_TANAROA_ID,
                name = "Village of Tanaroa",
                desc = "A native village protected by a great wall. The people speak of a central plateau where their ancestors built temples—now taboo. They warn of dinosaurs, pirates, and worse. A wise matriarch rules here.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(NATIVE_MATRIARCH_ID, NATIVE_WARRIOR_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(ISLE_COAST_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = PIRATE_COVE_ID,
                name = "Pirate Cove",
                desc = "A hidden cove where pirates have established a base. Their ship lies at anchor, and treasure from raids fills a sea cave. The pirates attack all strangers on sight.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(PIRATE_ID, PIRATE_ID, PIRATE_CAPTAIN_ID),
                itemIds = listOf(PIRATE_TREASURE_ID),
                exits = listOf(
                    Exit(ISLE_COAST_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),

            // Jungle & Wilderness
            Location(
                id = JUNGLE_PATH_ID,
                name = "Jungle Trail",
                desc = "A barely-visible path through dense jungle. The canopy blocks most sunlight. Dinosaur tracks cross the trail—some fresh. The air is thick with humidity and the buzz of insects.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(ALLOSAURUS_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(ISLE_COAST_ID, ExitDirection.NORTH),
                    Exit(SWAMP_ID, ExitDirection.EAST),
                    Exit(RAKASTA_CAMP_ID, ExitDirection.WEST),
                    Exit(CENTRAL_PLATEAU_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = SWAMP_ID,
                name = "Murky Swamp",
                desc = "A fetid swamp where the ground is uncertain. Tar pits bubble nearby, and the bones of trapped dinosaurs protrude from the muck. Something slithers through the water.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(STEGOSAURUS_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(JUNGLE_PATH_ID, ExitDirection.WEST),
                    Exit(TAR_PITS_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TAR_PITS_ID,
                name = "Tar Pits",
                desc = "Bubbling pools of hot tar trap unwary creatures. Dinosaur bones preserved for millennia protrude from the black surface. The natives avoid this cursed place.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(TRICERATOPS_ID),
                itemIds = listOf(DINOSAUR_HIDE_ID, DINOSAUR_TOOTH_ID),
                exits = listOf(
                    Exit(SWAMP_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = RAKASTA_CAMP_ID,
                name = "Rakasta Encampment",
                desc = "A camp of cat-like warriors called rakasta. They are proud and fierce, but may befriend those who prove their worth in combat. They know the paths to the central plateau.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(RAKASTA_WARRIOR_ID, RAKASTA_WARRIOR_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(JUNGLE_PATH_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = PHANATON_VILLAGE_ID,
                name = "Phanaton Tree Village",
                desc = "A village of small, raccoon-like creatures in the treetops. The phanaton are friendly and can glide from tree to tree. They warn of the aranea—intelligent spiders who are their enemies.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(PHANATON_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(ARANEA_LAIR_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = ARANEA_LAIR_ID,
                name = "Aranea Web",
                desc = "Massive webs span between ancient trees. The aranea—intelligent spiders who can assume human form—make their lair here. They spin webs of silk and lies with equal skill.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(ARANEA_ID, ARANEA_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(PHANATON_VILLAGE_ID, ExitDirection.NORTH),
                    Exit(CENTRAL_PLATEAU_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),

            // Central Plateau & Temple
            Location(
                id = CENTRAL_PLATEAU_ID,
                name = "Central Plateau",
                desc = "A flat-topped mountain in the island's center. Sheer cliffs protect it from all sides. The natives consider this sacred ground—taboo to enter. Ancient ruins crown the heights, and a Tyrannosaurus Rex prowls the plateau.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(TYRANNOSAURUS_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(JUNGLE_PATH_ID, ExitDirection.NORTH),
                    Exit(ARANEA_LAIR_ID, ExitDirection.EAST),
                    Exit(VILLAGE_MANTRU_ID, ExitDirection.WEST),
                    Exit(TABOO_ISLAND_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = VILLAGE_MANTRU_ID,
                name = "Village of Mantru",
                desc = "A native village on the plateau, isolated from the coastal tribes. The people here are under the influence of the kopru—their eyes empty, their movements puppet-like.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(ZOMBIE_NATIVE_ID, ZOMBIE_NATIVE_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(CENTRAL_PLATEAU_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TABOO_ISLAND_ID,
                name = "Taboo Island",
                desc = "A small island in a lake at the plateau's center. Ancient stone stairs lead up to a crumbling temple. Steam rises from volcanic vents. This is where the ancestors built their holiest shrine—now corrupted by the kopru.",
                locationType = LocationType.OUTDOOR,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(CENTRAL_PLATEAU_ID, ExitDirection.NORTH),
                    Exit(TEMPLE_LEVEL_ONE_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TEMPLE_LEVEL_ONE_ID,
                name = "Temple Level 1",
                desc = "The upper temple chambers. Ancient carvings depict the island's history—the arrival of the ancestors, the building of the temple, and then... strange fish-like creatures rising from below.",
                locationType = LocationType.DUNGEON,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(ZOMBIE_NATIVE_ID, ZOMBIE_NATIVE_ID),
                itemIds = listOf(TEMPLE_GOLD_ID),
                exits = listOf(
                    Exit(TABOO_ISLAND_ID, ExitDirection.UP),
                    Exit(TEMPLE_LEVEL_TWO_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TEMPLE_LEVEL_TWO_ID,
                name = "Temple Level 2",
                desc = "Deeper into the temple. The walls are slick with moisture, and the air smells of sulfur. Boiling mud pits bubble in side chambers. The kopru's influence is strong here.",
                locationType = LocationType.DUNGEON,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(KOPRU_ID),
                itemIds = listOf(TEMPLE_GOLD_ID),
                exits = listOf(
                    Exit(TEMPLE_LEVEL_ONE_ID, ExitDirection.UP),
                    Exit(TEMPLE_LEVEL_THREE_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TEMPLE_LEVEL_THREE_ID,
                name = "Temple Level 3",
                desc = "The deepest level, built around volcanic geysers. Steam obscures vision. The kopru swim through boiling water that would kill a human instantly.",
                locationType = LocationType.DUNGEON,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(KOPRU_ID, KOPRU_ID),
                itemIds = listOf(KOPRU_CHARM_ID),
                exits = listOf(
                    Exit(TEMPLE_LEVEL_TWO_ID, ExitDirection.UP),
                    Exit(KOPRU_LAIR_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = KOPRU_LAIR_ID,
                name = "Lair of the Kopru Elder",
                desc = "A vast underground lake of boiling water, lit by volcanic vents. In the center, upon a coral throne, sits the Kopru Elder—ancient beyond counting. Before it, in a place of honor, rests the legendary Black Pearl. 'Will you fall prey to the kopru's deadly charm?'",
                locationType = LocationType.CAVE,
                areaId = "x1-isle-of-dread",
                featureIds = emptyList(),
                creatureIds = listOf(KOPRU_ELDER_ID),
                itemIds = listOf(BLACK_PEARL_ID, TEMPLE_GOLD_ID),
                exits = listOf(
                    Exit(TEMPLE_LEVEL_THREE_ID, ExitDirection.UP)
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
