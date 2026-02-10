package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for B5: Horror on the Hill module.
 *
 * From Guido's Fort, adventurers cross the River Shrill to explore a
 * fog-shrouded hill infested with monsters. A ruined monastery hides
 * a hobgoblin army, and beneath it a young red dragon guards its hoard.
 * For character levels 1-3.
 *
 * Original module by Douglas Niles (1983), TSR Hobbies.
 */
object B5HorrorOnHillSeed {
    private val log = LoggerFactory.getLogger(B5HorrorOnHillSeed::class.java)

    private const val ATTRIBUTION = "B5: Horror on the Hill (Douglas Niles, TSR 1983)"

    // ============== LOCATION IDS ==============
    const val GUIDOS_FORT_ID = "b5-guidos-fort"
    const val LIONS_DEN_INN_ID = "b5-lions-den"
    const val RIVER_CROSSING_ID = "b5-river-crossing"
    const val HILL_BASE_ID = "b5-hill-base"
    const val HILL_TRAIL_ID = "b5-hill-trail"
    const val WITCH_HUT_ID = "b5-witch-hut"
    const val HAUNTED_GRAVEYARD_ID = "b5-graveyard"
    const val RUINED_MONASTERY_ID = "b5-monastery"
    const val MONASTERY_GARDEN_ID = "b5-monastery-garden"
    const val DUNGEON_LEVEL_ONE_ID = "b5-dungeon-1"
    const val DUNGEON_LEVEL_TWO_ID = "b5-dungeon-2"
    const val DUNGEON_LEVEL_THREE_ID = "b5-dungeon-3"
    const val DRAGONS_LAIR_ID = "b5-dragon-lair"

    // ============== CREATURE IDS ==============
    // Tier 1 - Surface
    const val GOBLIN_SCOUT_ID = "creature-b5-goblin-scout"
    const val DIRE_WOLF_ID = "creature-b5-dire-wolf"
    const val GIANT_BAT_ID = "creature-b5-giant-bat"
    const val GHOUL_ID = "creature-b5-ghoul"
    const val SKELETON_ID = "creature-b5-skeleton"

    // Tier 2 - Monastery & Upper Dungeon
    const val HOBGOBLIN_SOLDIER_ID = "creature-b5-hobgoblin"
    const val HOBGOBLIN_SERGEANT_ID = "creature-b5-hobgoblin-sergeant"
    const val OGRE_ID = "creature-b5-ogre"
    const val WITCH_ID = "creature-b5-witch"
    const val PIRANHA_BIRD_ID = "creature-b5-piranha-bird"
    const val STEAM_WEEVIL_ID = "creature-b5-steam-weevil"

    // Tier 3 - Lower Dungeon & Boss
    const val HOBGOBLIN_KING_ID = "creature-b5-hobgoblin-king"
    const val LAVA_LIZARD_ID = "creature-b5-lava-lizard"
    const val YOUNG_RED_DRAGON_ID = "creature-b5-red-dragon"

    // ============== ITEM IDS ==============
    const val MAGIC_FOUNTAIN_WATER_ID = "item-b5-fountain-water"
    const val HOBGOBLIN_WEAPON_ID = "item-b5-hobgoblin-weapon"
    const val WITCH_BREW_ID = "item-b5-witch-brew"
    const val DRAGON_SCALE_ID = "item-b5-dragon-scale"
    const val DRAGON_HOARD_GOLD_ID = "item-b5-dragon-gold"
    const val RUBY_OF_THE_HILL_ID = "item-b5-ruby"
    const val MONASTERY_TREASURE_ID = "item-b5-monastery-treasure"

    // ============== ABILITY IDS ==============
    const val ABILITY_DRAGON_BREATH_ID = "ability-b5-dragon-breath"
    const val ABILITY_GHOUL_PARALYSIS_ID = "ability-b5-ghoul-paralysis"
    const val ABILITY_WITCH_CURSE_ID = "ability-b5-witch-curse"

    // ============== FEATURE IDS ==============
    const val MAGIC_FOUNTAIN_FEATURE_ID = "feature-b5-magic-fountain"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_SURFACE_ID = "loot-b5-surface"
    const val LOOT_TABLE_UNDEAD_ID = "loot-b5-undead"
    const val LOOT_TABLE_HOBGOBLIN_ID = "loot-b5-hobgoblin"
    const val LOOT_TABLE_DRAGON_ID = "loot-b5-dragon"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (GOBLIN_SCOUT_ID !in existingCreatures) {
            log.info("Seeding B5: Horror on the Hill content...")
            seedAbilities()
            seedFeatures()
            seedItems()
            seedLootTables()
            seedCreatures()
            seedLocations()
            log.info("Seeded B5: Horror on the Hill content")
        }
    }

    private fun seedAbilities() {
        val abilities = listOf(
            Ability(
                id = ABILITY_DRAGON_BREATH_ID,
                name = "Fire Breath",
                description = "The dragon exhales a cone of searing flame, burning everything in its path.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 30,
                effects = """[{"type":"damage","damageType":"fire","modifier":30,"aoe":true}]""",
                attribution = ATTRIBUTION
            ),
            Ability(
                id = ABILITY_GHOUL_PARALYSIS_ID,
                name = "Paralyzing Touch",
                description = "The ghoul's touch carries a supernatural chill that freezes victims in place.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "short",
                cooldownRounds = 1,
                baseDamage = 0,
                durationRounds = 2,
                effects = """[{"type":"condition","condition":"paralyzed","duration":2,"saveType":"constitution"}]""",
                attribution = ATTRIBUTION
            ),
            Ability(
                id = ABILITY_WITCH_CURSE_ID,
                name = "Witch's Curse",
                description = "The witch places a terrible curse upon her victim, weakening them for days.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 20,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 0,
                durationRounds = 5,
                effects = """[{"type":"debuff","stat":"all","penalty":2,"duration":5,"saveType":"wisdom"}]""",
                attribution = ATTRIBUTION
            )
        )

        abilities.forEach { ability ->
            if (AbilityRepository.findById(ability.id) == null) {
                AbilityRepository.create(ability)
            }
        }
    }

    private fun seedFeatures() {
        val features = listOf(
            Feature(
                id = MAGIC_FOUNTAIN_FEATURE_ID,
                name = "Magic Fountain",
                category = "magical",
                description = "A mysterious fountain in the monastery garden. Its waters have unpredictable magical effects—healing, poison, transformation, or nothing at all.",
                data = """{"effectType":"random","effects":["heal","poison","polymorph","nothing"]}"""
            )
        )

        features.forEach { feature ->
            if (FeatureRepository.findById(feature.id) == null) {
                FeatureRepository.create(feature)
            }
        }
    }

    private fun seedItems() {
        val items = listOf(
            Item(
                id = MAGIC_FOUNTAIN_WATER_ID,
                name = "Magic Fountain Water",
                desc = "Water collected from the monastery's magic fountain. Its effects are unpredictable—it might heal or harm the drinker.",
                featureIds = emptyList(),
                equipmentType = "consumable",
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = HOBGOBLIN_WEAPON_ID,
                name = "Hobgoblin Military Blade",
                desc = "A well-crafted weapon from the hobgoblin army. Their smiths are skilled, if brutal.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 6),
                value = 45,
                attribution = ATTRIBUTION
            ),
            Item(
                id = WITCH_BREW_ID,
                name = "Witch's Brew",
                desc = "A bubbling potion from the witch's cauldron. The effects vary based on the witch's mood when brewed.",
                featureIds = emptyList(),
                equipmentType = "consumable",
                value = 75,
                attribution = ATTRIBUTION
            ),
            Item(
                id = DRAGON_SCALE_ID,
                name = "Red Dragon Scale",
                desc = "A crimson scale from the young dragon. Hot to the touch, it could be used in crafting fire-resistant armor.",
                featureIds = emptyList(),
                equipmentType = "material",
                value = 200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = DRAGON_HOARD_GOLD_ID,
                name = "Dragon's Gold",
                desc = "Coins and treasures accumulated by the dragon over many years of terrorizing the region.",
                featureIds = emptyList(),
                value = 500,
                isStackable = true,
                attribution = ATTRIBUTION
            ),
            Item(
                id = RUBY_OF_THE_HILL_ID,
                name = "Ruby of the Hill",
                desc = "A magnificent ruby, the centerpiece of the dragon's hoard. Local legend says it was taken from an ancient king.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                statBonuses = StatBonuses(attack = 5, maxMana = 20),
                value = 2000,
                attribution = ATTRIBUTION
            ),
            Item(
                id = MONASTERY_TREASURE_ID,
                name = "Monastery Relics",
                desc = "Sacred treasures hidden by the monks before the monastery fell to monsters.",
                featureIds = emptyList(),
                value = 150,
                isStackable = true,
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
                id = LOOT_TABLE_SURFACE_ID,
                name = "Hill Surface Loot",
                entries = listOf(
                    LootEntry(MONASTERY_TREASURE_ID, 0.10f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_UNDEAD_ID,
                name = "Graveyard Undead Loot",
                entries = listOf(
                    LootEntry(MONASTERY_TREASURE_ID, 0.20f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_HOBGOBLIN_ID,
                name = "Hobgoblin Army Loot",
                entries = listOf(
                    LootEntry(HOBGOBLIN_WEAPON_ID, 0.12f),
                    LootEntry(MONASTERY_TREASURE_ID, 0.30f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_DRAGON_ID,
                name = "Dragon's Hoard",
                entries = listOf(
                    LootEntry(DRAGON_SCALE_ID, 0.12f),
                    LootEntry(DRAGON_HOARD_GOLD_ID, 1.0f),
                    LootEntry(RUBY_OF_THE_HILL_ID, 0.80f)
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
            // Tier 1 - Surface Encounters
            Creature(
                id = GOBLIN_SCOUT_ID,
                name = "Goblin Scout",
                description = "A goblin riding a dire wolf, patrolling the Hill for the hobgoblin king. Reports intruders to the monastery garrison.",
                level = 1,
                hitDice = 1,
                armorClass = 14,
                attackBonus = 3,
                damage = "1d6",
                maxHp = 6,
                currentHp = 6,
                xpValue = 30,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_SURFACE_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = DIRE_WOLF_ID,
                name = "Dire Wolf",
                description = "A massive wolf with glowing eyes, used as a mount by goblin scouts. Can track prey across the entire Hill.",
                level = 2,
                hitDice = 3,
                armorClass = 14,
                attackBonus = 5,
                damage = "1d8+2",
                maxHp = 19,
                currentHp = 19,
                xpValue = 80,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GIANT_BAT_ID,
                name = "Giant Bat",
                description = "A bat with a six-foot wingspan that hunts the Hill at night. Swoops down on unsuspecting travelers.",
                level = 1,
                hitDice = 2,
                armorClass = 13,
                attackBonus = 3,
                damage = "1d4",
                maxHp = 8,
                currentHp = 8,
                xpValue = 35,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GHOUL_ID,
                name = "Ghoul",
                description = "An undead horror that prowls the graveyard, its touch paralyzing the living. Feeds on the flesh of the dead.",
                level = 2,
                hitDice = 2,
                armorClass = 14,
                attackBonus = 4,
                damage = "1d6",
                maxHp = 12,
                currentHp = 12,
                xpValue = 75,
                abilityIds = listOf(ABILITY_GHOUL_PARALYSIS_ID),
                lootTableId = LOOT_TABLE_UNDEAD_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = SKELETON_ID,
                name = "Skeleton",
                description = "The animated bones of a long-dead monk, still wearing tattered robes. Attacks mindlessly with bony claws.",
                level = 1,
                hitDice = 1,
                armorClass = 13,
                attackBonus = 2,
                damage = "1d6",
                maxHp = 6,
                currentHp = 6,
                xpValue = 25,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_UNDEAD_ID,
                attribution = ATTRIBUTION
            ),

            // Tier 2 - Monastery & Upper Dungeon
            Creature(
                id = HOBGOBLIN_SOLDIER_ID,
                name = "Hobgoblin Soldier",
                description = "A disciplined warrior from the hobgoblin king's army. Well-armored and fighting in coordinated formations.",
                level = 2,
                hitDice = 2,
                armorClass = 16,
                attackBonus = 5,
                damage = "1d8",
                maxHp = 11,
                currentHp = 11,
                xpValue = 50,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_HOBGOBLIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = HOBGOBLIN_SERGEANT_ID,
                name = "Hobgoblin Sergeant",
                description = "A scarred veteran who commands squads of hobgoblin soldiers. Wears insignia marking kills and campaigns.",
                level = 3,
                hitDice = 3,
                armorClass = 17,
                attackBonus = 6,
                damage = "1d8+2",
                maxHp = 20,
                currentHp = 20,
                xpValue = 100,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_HOBGOBLIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = OGRE_ID,
                name = "Ogre",
                description = "A massive brute employed by the hobgoblins as shock troops. Too stupid for strategy but devastating in combat.",
                level = 3,
                hitDice = 4,
                armorClass = 14,
                attackBonus = 7,
                damage = "1d10+3",
                maxHp = 26,
                currentHp = 26,
                xpValue = 150,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_HOBGOBLIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = WITCH_ID,
                name = "The Witch",
                description = "Two 'kindly old ladies' who live in a hut on the Hill. They offer hospitality but are actually evil witches who prey on travelers.",
                level = 4,
                hitDice = 5,
                armorClass = 14,
                attackBonus = 6,
                damage = "1d6+2",
                maxHp = 30,
                currentHp = 30,
                xpValue = 250,
                abilityIds = listOf(ABILITY_WITCH_CURSE_ID),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            // New Monsters from the module
            Creature(
                id = PIRANHA_BIRD_ID,
                name = "Piranha Bird",
                description = "A flock predator with razor-sharp beaks. They swarm like fish, stripping flesh from bone in seconds.",
                level = 2,
                hitDice = 2,
                armorClass = 15,
                attackBonus = 4,
                damage = "1d6",
                maxHp = 10,
                currentHp = 10,
                xpValue = 60,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = STEAM_WEEVIL_ID,
                name = "Steam Weevil",
                description = "A giant insect that lives near volcanic vents. Sprays scalding steam at attackers.",
                level = 2,
                hitDice = 2,
                armorClass = 14,
                attackBonus = 4,
                damage = "1d4+2",
                maxHp = 12,
                currentHp = 12,
                xpValue = 50,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),

            // Tier 3 - Boss Creatures
            Creature(
                id = HOBGOBLIN_KING_ID,
                name = "The Hobgoblin King",
                description = "Ruler of the hobgoblin army occupying the monastery. Wears a crown forged from melted weapons and commands absolute loyalty.",
                level = 5,
                hitDice = 6,
                armorClass = 18,
                attackBonus = 9,
                damage = "1d10+4",
                maxHp = 45,
                currentHp = 45,
                xpValue = 600,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_HOBGOBLIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = LAVA_LIZARD_ID,
                name = "Lava Lizard",
                description = "A heat-resistant reptile that swims through molten rock. Its bite delivers burning venom.",
                level = 3,
                hitDice = 4,
                armorClass = 16,
                attackBonus = 6,
                damage = "1d8+3",
                maxHp = 28,
                currentHp = 28,
                xpValue = 175,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = YOUNG_RED_DRAGON_ID,
                name = "Young Red Dragon",
                description = "A young but deadly dragon that lairs in the deepest caverns. Its scales are bright crimson and its breath can melt stone. The true Horror on the Hill.",
                level = 8,
                hitDice = 10,
                armorClass = 18,
                attackBonus = 11,
                damage = "2d8+6",
                maxHp = 80,
                currentHp = 80,
                xpValue = 2500,
                abilityIds = listOf(ABILITY_DRAGON_BREATH_ID),
                lootTableId = LOOT_TABLE_DRAGON_ID,
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
                id = GUIDOS_FORT_ID,
                name = "Guido's Fort",
                desc = "A frontier outpost on the banks of the River Shrill. The last stop on the caravan roads, it stands as civilization's final bastion before the wilderness. Across the river looms the fog-shrouded bulk known only as 'The Hill.'",
                locationType = LocationType.OUTDOOR,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(LIONS_DEN_INN_ID, ExitDirection.NORTH),
                    Exit(RIVER_CROSSING_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = LIONS_DEN_INN_ID,
                name = "The Lion's Den Inn",
                desc = "A smoky, dimly-lit tavern where adventurers gather. Straw pallets in the back room cost a silver piece—and come with fleas. The locals share rumors of The Hill: haunted graveyards, bands of monsters, and a fire-breathing dragon.",
                locationType = LocationType.INDOOR,
                areaId = "b5-horror-hill",
                featureIds = listOf(GeneralStoreSeed.COOKING_FIRE_FEATURE),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(GUIDOS_FORT_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = RIVER_CROSSING_ID,
                name = "River Shrill",
                desc = "The mile-wide River Shrill flows between the fort and The Hill. Local fishermen will ferry you across for 20 gold pieces—but none will wait. 'You'll never come back,' they say. 'No one ever does.'",
                locationType = LocationType.OUTDOOR,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(GUIDOS_FORT_ID, ExitDirection.EAST),
                    Exit(HILL_BASE_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = HILL_BASE_ID,
                name = "Base of The Hill",
                desc = "Dense forest crowds the shore, blocking all sight of what lies ahead. The Hill rises 400 feet above, its rocky cliffs jutting from slopes shrouded in mist. Steam rises from vents on the upper slopes.",
                locationType = LocationType.OUTDOOR,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(GOBLIN_SCOUT_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(RIVER_CROSSING_ID, ExitDirection.EAST),
                    Exit(HILL_TRAIL_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = HILL_TRAIL_ID,
                name = "The Hill Trail",
                desc = "A winding path climbs through tangled undergrowth. Every bend could hide ambush. Strange bird calls echo through the trees, and the mist grows thicker as you climb.",
                locationType = LocationType.OUTDOOR,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(GIANT_BAT_ID, PIRANHA_BIRD_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(HILL_BASE_ID, ExitDirection.SOUTH),
                    Exit(WITCH_HUT_ID, ExitDirection.EAST),
                    Exit(HAUNTED_GRAVEYARD_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = WITCH_HUT_ID,
                name = "The Witch's Hut",
                desc = "A small cottage with smoke rising from the chimney. Two kindly old ladies invite you in for tea and hospitality. Their smiles are too wide, and something bubbles in the cauldron that doesn't smell like stew.",
                locationType = LocationType.INDOOR,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(WITCH_ID),
                itemIds = listOf(WITCH_BREW_ID),
                exits = listOf(
                    Exit(HILL_TRAIL_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = HAUNTED_GRAVEYARD_ID,
                name = "Haunted Graveyard",
                desc = "An old cemetery surrounding the ruined monastery. Tombstones lean at odd angles, and freshly dug earth suggests not all the dead rest easy. Mist clings to the ground even at noon.",
                locationType = LocationType.OUTDOOR,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(GHOUL_ID, GHOUL_ID, SKELETON_ID, SKELETON_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(HILL_TRAIL_ID, ExitDirection.SOUTH),
                    Exit(RUINED_MONASTERY_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = RUINED_MONASTERY_ID,
                name = "Ruined Monastery",
                desc = "Once a holy place, now headquarters for the hobgoblin army. Crumbling walls bear scorch marks and bloodstains. Hobgoblin guards patrol the corridors, and war drums echo from within.",
                locationType = LocationType.DUNGEON,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(HOBGOBLIN_SOLDIER_ID, HOBGOBLIN_SOLDIER_ID, HOBGOBLIN_SERGEANT_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(HAUNTED_GRAVEYARD_ID, ExitDirection.SOUTH),
                    Exit(MONASTERY_GARDEN_ID, ExitDirection.EAST),
                    Exit(DUNGEON_LEVEL_ONE_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = MONASTERY_GARDEN_ID,
                name = "Monastery Garden",
                desc = "An overgrown garden with a mysterious fountain at its center. The fountain still flows, its waters shimmering with magic. What effects might drinking bring—healing? Transformation? Death?",
                locationType = LocationType.OUTDOOR,
                areaId = "b5-horror-hill",
                featureIds = listOf(MAGIC_FOUNTAIN_FEATURE_ID),
                creatureIds = emptyList(),
                itemIds = listOf(MAGIC_FOUNTAIN_WATER_ID),
                exits = listOf(
                    Exit(RUINED_MONASTERY_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = DUNGEON_LEVEL_ONE_ID,
                name = "Dungeon Level I",
                desc = "The upper dungeons beneath the monastery. Hobgoblin barracks and storage rooms line the corridors. The smell of unwashed bodies and rotting food fills the air.",
                locationType = LocationType.DUNGEON,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(HOBGOBLIN_SOLDIER_ID, HOBGOBLIN_SOLDIER_ID, OGRE_ID),
                itemIds = listOf(MONASTERY_TREASURE_ID),
                exits = listOf(
                    Exit(RUINED_MONASTERY_ID, ExitDirection.UP),
                    Exit(DUNGEON_LEVEL_TWO_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = DUNGEON_LEVEL_TWO_ID,
                name = "Dungeon Level II",
                desc = "Deeper passages where the Hobgoblin King holds court. Elite guards patrol these halls, and the throne room lies ahead. Steam vents make the air hot and humid.",
                locationType = LocationType.DUNGEON,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(HOBGOBLIN_SOLDIER_ID, HOBGOBLIN_SERGEANT_ID, HOBGOBLIN_KING_ID),
                itemIds = listOf(MONASTERY_TREASURE_ID),
                exits = listOf(
                    Exit(DUNGEON_LEVEL_ONE_ID, ExitDirection.UP),
                    Exit(DUNGEON_LEVEL_THREE_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = DUNGEON_LEVEL_THREE_ID,
                name = "Dungeon Level III",
                desc = "A maze of twisting tunnels that all seem to lead back to the same place. Hidden traps and dead ends frustrate navigation. The heat grows intense as you descend.",
                locationType = LocationType.DUNGEON,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(STEAM_WEEVIL_ID, LAVA_LIZARD_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(DUNGEON_LEVEL_TWO_ID, ExitDirection.UP),
                    Exit(DRAGONS_LAIR_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = DRAGONS_LAIR_ID,
                name = "The Dragon's Lair",
                desc = "A vast cavern lit by rivers of molten rock. In the center, upon a mound of gold and bones, sleeps a young red dragon. Its crimson scales glow with inner fire, and smoke curls from its nostrils. This is the true Horror on the Hill.",
                locationType = LocationType.CAVE,
                areaId = "b5-horror-hill",
                featureIds = emptyList(),
                creatureIds = listOf(YOUNG_RED_DRAGON_ID),
                itemIds = listOf(DRAGON_HOARD_GOLD_ID, RUBY_OF_THE_HILL_ID),
                exits = listOf(
                    Exit(DUNGEON_LEVEL_THREE_ID, ExitDirection.UP)
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
