package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for B1: In Search of the Unknown module.
 *
 * The legendary stronghold of Rogahn the Fearless and Zelligar the Unknown,
 * two heroes who built the Caverns of Quasqueton as their secret base.
 * Now abandoned after their final expedition, the dungeon awaits exploration.
 * For character levels 1-3.
 *
 * Original module by Mike Carr (1979), TSR Hobbies.
 */
object B1SearchUnknownSeed {
    private val log = LoggerFactory.getLogger(B1SearchUnknownSeed::class.java)

    private const val ATTRIBUTION = "B1: In Search of the Unknown (Mike Carr, TSR 1979)"

    // ============== LOCATION IDS ==============
    const val QUASQUETON_ENTRANCE_ID = "b1-entrance"
    const val ENTRANCE_CORRIDOR_ID = "b1-entrance-corridor"
    const val ROGAHNS_CHAMBER_ID = "b1-rogahn-chamber"
    const val ZELLIGARS_CHAMBER_ID = "b1-zelligar-chamber"
    const val ROOM_OF_POOLS_ID = "b1-room-of-pools"
    const val ROOM_OF_DOORS_ID = "b1-room-of-doors"
    const val GRAND_DINING_HALL_ID = "b1-dining-hall"
    const val KITCHEN_ID = "b1-kitchen"
    const val GYMNASIUM_ID = "b1-gymnasium"
    const val MUSEUM_ID = "b1-museum"
    const val LOWER_CAVERNS_ID = "b1-lower-caverns"
    const val FUNGUS_GARDEN_ID = "b1-fungus-garden"
    const val UNDERGROUND_RIVER_ID = "b1-underground-river"
    const val TREASURE_VAULT_ID = "b1-treasure-vault"

    // ============== CREATURE IDS ==============
    // The module is designed for DMs to place monsters - these are suggested encounters
    const val GIANT_RAT_ID = "creature-b1-giant-rat"
    const val KOBOLD_ID = "creature-b1-kobold"
    const val GIANT_CENTIPEDE_ID = "creature-b1-centipede"
    const val FIRE_BEETLE_ID = "creature-b1-fire-beetle"
    const val ORC_ID = "creature-b1-orc"
    const val SKELETON_ID = "creature-b1-skeleton"
    const val ZOMBIE_ID = "creature-b1-zombie"
    const val TROGLODYTE_ID = "creature-b1-troglodyte"
    const val BERSERKER_ID = "creature-b1-berserker"
    const val GELATINOUS_CUBE_ID = "creature-b1-gelatinous-cube"

    // ============== ITEM IDS ==============
    const val ROGAHNS_SWORD_ID = "item-b1-rogahn-sword"
    const val ZELLIGARS_WAND_ID = "item-b1-zelligar-wand"
    const val POOL_POTION_ID = "item-b1-pool-potion"
    const val QUASQUETON_COIN_ID = "item-b1-quasqueton-coin"
    const val MAGIC_MOUTH_GEM_ID = "item-b1-magic-mouth-gem"
    const val CURSED_RING_ID = "item-b1-cursed-ring"
    const val TELEPORT_SCROLL_ID = "item-b1-teleport-scroll"
    const val TREASURE_MAP_ID = "item-b1-treasure-map"

    // ============== FEATURE IDS ==============
    const val MAGIC_MOUTH_FEATURE_ID = "feature-b1-magic-mouth"
    const val ONE_WAY_DOOR_FEATURE_ID = "feature-b1-one-way-door"
    const val MYSTERIOUS_POOL_FEATURE_ID = "feature-b1-mysterious-pool"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_COMMON_ID = "loot-b1-common"
    const val LOOT_TABLE_ROGAHN_ID = "loot-b1-rogahn"
    const val LOOT_TABLE_ZELLIGAR_ID = "loot-b1-zelligar"
    const val LOOT_TABLE_VAULT_ID = "loot-b1-vault"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (GIANT_RAT_ID !in existingCreatures) {
            log.info("Seeding B1: In Search of the Unknown content...")
            seedFeatures()
            seedItems()
            seedLootTables()
            seedCreatures()
            seedLocations()
            log.info("Seeded B1: In Search of the Unknown content")
        }
    }

    private fun seedFeatures() {
        val features = listOf(
            Feature(
                id = MAGIC_MOUTH_FEATURE_ID,
                name = "Magic Mouth",
                category = "magical",
                description = "A magical mouth appears on the wall, speaking cryptic messages left by Zelligar. 'Who dares enter the stronghold of the great Zelligar and mighty Rogahn?'",
                data = """{"speakTrigger":"approach","repeatable":true}"""
            ),
            Feature(
                id = ONE_WAY_DOOR_FEATURE_ID,
                name = "One-Way Secret Door",
                category = "trap",
                description = "A secret door that only opens from one side, potentially trapping unwary explorers.",
                data = """{"trapType":"one_way","difficulty":15}"""
            ),
            Feature(
                id = MYSTERIOUS_POOL_FEATURE_ID,
                name = "Mysterious Pool",
                category = "magical",
                description = "One of several pools in the Room of Pools. Each has a different magical effect—some helpful, some harmful, some just strange.",
                data = """{"effectType":"random","effects":["heal","harm","transform","teleport","nothing"]}"""
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
                id = ROGAHNS_SWORD_ID,
                name = "Rogahn's Blade",
                desc = "The legendary sword of Rogahn the Fearless. Its blade still gleams despite years of neglect, etched with battle scenes of the barbarian wars.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 10, defense = 2),
                value = 1000,
                attribution = ATTRIBUTION
            ),
            Item(
                id = ZELLIGARS_WAND_ID,
                name = "Zelligar's Wand",
                desc = "A wand of unknown power, once wielded by Zelligar the Unknown. Strange runes pulse along its length.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 6, maxMana = 30),
                value = 1200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = POOL_POTION_ID,
                name = "Pool Water Vial",
                desc = "Water collected from one of the mysterious pools. Its effects are unknown until consumed.",
                featureIds = emptyList(),
                equipmentType = "consumable",
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = QUASQUETON_COIN_ID,
                name = "Quasqueton Coin",
                desc = "An old gold coin bearing the profiles of Rogahn and Zelligar. Worth more to collectors than as currency.",
                featureIds = emptyList(),
                value = 25,
                isStackable = true,
                attribution = ATTRIBUTION
            ),
            Item(
                id = MAGIC_MOUTH_GEM_ID,
                name = "Magic Mouth Gem",
                desc = "A gem that powers one of Zelligar's magic mouth spells. Might be repurposed by a skilled wizard.",
                featureIds = emptyList(),
                value = 100,
                attribution = ATTRIBUTION
            ),
            Item(
                id = CURSED_RING_ID,
                name = "Ring of Weakness",
                desc = "A cursed ring that appears valuable but drains the wearer's strength. Once worn, it cannot be removed without magic.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "ring",
                statBonuses = StatBonuses(attack = -3, defense = -2),
                value = 0,
                attribution = ATTRIBUTION
            ),
            Item(
                id = TELEPORT_SCROLL_ID,
                name = "Scroll of Teleportation",
                desc = "A scroll containing Zelligar's teleportation spell. The destination is unclear from the faded writing.",
                featureIds = emptyList(),
                equipmentType = "consumable",
                value = 200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = TREASURE_MAP_ID,
                name = "Partial Treasure Map",
                desc = "A map showing the location of a hidden treasure room in Quasqueton. Some sections are illegible.",
                featureIds = emptyList(),
                value = 75,
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
                id = LOOT_TABLE_COMMON_ID,
                name = "Quasqueton Common Loot",
                entries = listOf(
                    LootEntry(QUASQUETON_COIN_ID, 0.35f),
                    LootEntry(POOL_POTION_ID, 0.10f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_ROGAHN_ID,
                name = "Rogahn's Chamber Loot",
                entries = listOf(
                    LootEntry(ROGAHNS_SWORD_ID, 0.30f),
                    LootEntry(QUASQUETON_COIN_ID, 0.80f),
                    LootEntry(TREASURE_MAP_ID, 0.20f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_ZELLIGAR_ID,
                name = "Zelligar's Chamber Loot",
                entries = listOf(
                    LootEntry(ZELLIGARS_WAND_ID, 0.30f),
                    LootEntry(MAGIC_MOUTH_GEM_ID, 0.40f),
                    LootEntry(TELEPORT_SCROLL_ID, 0.25f),
                    LootEntry(CURSED_RING_ID, 0.15f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_VAULT_ID,
                name = "Treasure Vault Loot",
                entries = listOf(
                    LootEntry(QUASQUETON_COIN_ID, 1.0f),
                    LootEntry(ROGAHNS_SWORD_ID, 0.20f),
                    LootEntry(ZELLIGARS_WAND_ID, 0.20f),
                    LootEntry(TREASURE_MAP_ID, 0.50f)
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
            Creature(
                id = GIANT_RAT_ID,
                name = "Giant Rat",
                description = "A dog-sized rat with matted fur and yellowed teeth. These vermin have infested the abandoned stronghold.",
                level = 1,
                hitDice = 1,
                armorClass = 12,
                attackBonus = 2,
                damage = "1d4",
                maxHp = 4,
                currentHp = 4,
                xpValue = 15,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_COMMON_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = KOBOLD_ID,
                name = "Kobold",
                description = "A small reptilian creature that has moved into the abandoned dungeon. Cowardly but cunning.",
                level = 1,
                hitDice = 1,
                armorClass = 13,
                attackBonus = 2,
                damage = "1d4",
                maxHp = 4,
                currentHp = 4,
                xpValue = 20,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_COMMON_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GIANT_CENTIPEDE_ID,
                name = "Giant Centipede",
                description = "A venomous centipede as long as a man's arm. Its bite causes painful swelling.",
                level = 1,
                hitDice = 1,
                armorClass = 11,
                attackBonus = 3,
                damage = "1d4",
                maxHp = 3,
                currentHp = 3,
                xpValue = 25,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = FIRE_BEETLE_ID,
                name = "Fire Beetle",
                description = "A large beetle with glowing glands. The glands can be harvested as light sources.",
                level = 1,
                hitDice = 1,
                armorClass = 14,
                attackBonus = 2,
                damage = "1d6",
                maxHp = 6,
                currentHp = 6,
                xpValue = 20,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ORC_ID,
                name = "Orc",
                description = "A brutish humanoid that has taken up residence in the dungeon. Likely part of a scouting party.",
                level = 1,
                hitDice = 1,
                armorClass = 14,
                attackBonus = 4,
                damage = "1d8",
                maxHp = 8,
                currentHp = 8,
                xpValue = 35,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_COMMON_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = SKELETON_ID,
                name = "Skeleton",
                description = "The animated bones of a former servant or guard. Mindlessly attacks all intruders.",
                level = 1,
                hitDice = 1,
                armorClass = 13,
                attackBonus = 2,
                damage = "1d6",
                maxHp = 6,
                currentHp = 6,
                xpValue = 25,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_COMMON_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ZOMBIE_ID,
                name = "Zombie",
                description = "A shambling corpse, perhaps once a servant of Rogahn or Zelligar. Slow but relentless.",
                level = 2,
                hitDice = 2,
                armorClass = 12,
                attackBonus = 3,
                damage = "1d8",
                maxHp = 12,
                currentHp = 12,
                xpValue = 40,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_COMMON_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = TROGLODYTE_ID,
                name = "Troglodyte",
                description = "A cave-dwelling reptilian humanoid with a nauseating stench. Has claimed the lower caverns.",
                level = 2,
                hitDice = 2,
                armorClass = 14,
                attackBonus = 4,
                damage = "1d6",
                maxHp = 11,
                currentHp = 11,
                xpValue = 50,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_COMMON_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = BERSERKER_ID,
                name = "Berserker",
                description = "A wild-eyed warrior who went mad after becoming trapped in the dungeon. Attacks without reason.",
                level = 2,
                hitDice = 2,
                armorClass = 13,
                attackBonus = 5,
                damage = "1d8+1",
                maxHp = 14,
                currentHp = 14,
                xpValue = 60,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_COMMON_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GELATINOUS_CUBE_ID,
                name = "Gelatinous Cube",
                description = "A 10-foot cube of transparent jelly that sweeps through the corridors, dissolving all in its path. Treasures float inside.",
                level = 4,
                hitDice = 4,
                armorClass = 12,
                attackBonus = 4,
                damage = "2d6",
                maxHp = 28,
                currentHp = 28,
                xpValue = 200,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_VAULT_ID,
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
                id = QUASQUETON_ENTRANCE_ID,
                name = "Caverns of Quasqueton - Entrance",
                desc = "A craggy rock outcropping high on a forested hill. Hidden among the rocks is the entrance to the legendary stronghold of Rogahn and Zelligar. A single watchtower crumbles nearby. Word has spread that the two heroes never returned from their expedition to the barbarian lands.",
                locationType = LocationType.OUTDOOR,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(ENTRANCE_CORRIDOR_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = ENTRANCE_CORRIDOR_ID,
                name = "Entrance Corridor",
                desc = "A wide corridor carved from solid rock. A magic mouth suddenly appears on the wall: 'WHO DARES ENTER THE STRONGHOLD OF THE MIGHTY ROGAHN AND THE GREAT ZELLIGAR?' The voice echoes and fades.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = listOf(MAGIC_MOUTH_FEATURE_ID),
                creatureIds = listOf(GIANT_RAT_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(QUASQUETON_ENTRANCE_ID, ExitDirection.UP),
                    Exit(ROGAHNS_CHAMBER_ID, ExitDirection.WEST),
                    Exit(ZELLIGARS_CHAMBER_ID, ExitDirection.EAST),
                    Exit(GRAND_DINING_HALL_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = ROGAHNS_CHAMBER_ID,
                name = "Rogahn's Personal Quarters",
                desc = "The private chambers of Rogahn the Fearless. Weapons and trophies decorate the walls—barbarian shields, a dire wolf head, battle standards. A large bed and writing desk remain, dusty but intact. Rogahn's personal effects may still be here.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = listOf(SKELETON_ID),
                itemIds = listOf(QUASQUETON_COIN_ID),
                exits = listOf(
                    Exit(ENTRANCE_CORRIDOR_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = ZELLIGARS_CHAMBER_ID,
                name = "Zelligar's Wizard's Workroom",
                desc = "The private laboratory of Zelligar the Unknown. Arcane symbols cover the walls, and empty potion bottles line the shelves. Strange apparatus of unknown purpose fills the room. The air still crackles with residual magic.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = listOf(MAGIC_MOUTH_FEATURE_ID),
                creatureIds = listOf(FIRE_BEETLE_ID),
                itemIds = listOf(MAGIC_MOUTH_GEM_ID),
                exits = listOf(
                    Exit(ENTRANCE_CORRIDOR_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = GRAND_DINING_HALL_ID,
                name = "Grand Dining Hall",
                desc = "A massive hall with a long table that could seat fifty. Faded tapestries depict the heroes' victories—Rogahn fighting barbarian hordes, Zelligar casting mighty spells. Cobwebs now cover the chandeliers.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = listOf(ORC_ID, KOBOLD_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(ENTRANCE_CORRIDOR_ID, ExitDirection.SOUTH),
                    Exit(KITCHEN_ID, ExitDirection.WEST),
                    Exit(ROOM_OF_POOLS_ID, ExitDirection.EAST),
                    Exit(GYMNASIUM_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = KITCHEN_ID,
                name = "Kitchen and Pantry",
                desc = "A large kitchen with cold hearths and empty pantries. Some preserved food has survived the years, though most has rotted. Pots and pans hang from hooks, clattering in unseen drafts.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = listOf(GeneralStoreSeed.COOKING_FIRE_FEATURE),
                creatureIds = listOf(GIANT_RAT_ID, GIANT_RAT_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(GRAND_DINING_HALL_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = ROOM_OF_POOLS_ID,
                name = "Room of Pools",
                desc = "A chamber containing multiple pools of water, each with different magical properties. Some heal, some harm, some transform. The pools glow with faint colored light—blue, green, red, purple, and one that seems to shift through all colors.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = listOf(MYSTERIOUS_POOL_FEATURE_ID),
                creatureIds = emptyList(),
                itemIds = listOf(POOL_POTION_ID),
                exits = listOf(
                    Exit(GRAND_DINING_HALL_ID, ExitDirection.WEST),
                    Exit(ROOM_OF_DOORS_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = ROOM_OF_DOORS_ID,
                name = "Room of Doors",
                desc = "A hexagonal room with six doors, one on each wall. Some doors are fake, some are trapped, and some lead to entirely different parts of the complex. One-way secret doors make navigation treacherous.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = listOf(ONE_WAY_DOOR_FEATURE_ID),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(ROOM_OF_POOLS_ID, ExitDirection.SOUTH),
                    Exit(MUSEUM_ID, ExitDirection.EAST),
                    Exit(LOWER_CAVERNS_ID, ExitDirection.DOWN)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = GYMNASIUM_ID,
                name = "Training Gymnasium",
                desc = "A large room where Rogahn trained his warriors. Weapon racks (mostly empty now), training dummies, and exercise equipment fill the space. The floor bears the scuff marks of countless practice bouts.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = listOf(BERSERKER_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(GRAND_DINING_HALL_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = MUSEUM_ID,
                name = "Trophy Room and Museum",
                desc = "Rogahn and Zelligar's collection of curiosities and trophies. Glass cases (many broken) contain artifacts from their adventures. A stuffed owlbear stands in one corner; a preserved mind flayer head floats in a jar.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = listOf(ZOMBIE_ID),
                itemIds = listOf(TREASURE_MAP_ID),
                exits = listOf(
                    Exit(ROOM_OF_DOORS_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = LOWER_CAVERNS_ID,
                name = "Lower Caverns",
                desc = "Natural caverns beneath the constructed stronghold. Zelligar expanded into these caves for experiments requiring more space. Strange fungi grow on the walls, and an underground river can be heard in the distance.",
                locationType = LocationType.CAVE,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = listOf(TROGLODYTE_ID, GIANT_CENTIPEDE_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(ROOM_OF_DOORS_ID, ExitDirection.UP),
                    Exit(FUNGUS_GARDEN_ID, ExitDirection.EAST),
                    Exit(UNDERGROUND_RIVER_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = FUNGUS_GARDEN_ID,
                name = "Fungus Garden",
                desc = "A cavern filled with cultivated fungi—some for food, some for spell components, some dangerous. Phosphorescent mushrooms provide an eerie glow. Some of the fungi have grown wild and deadly in the years of neglect.",
                locationType = LocationType.CAVE,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = listOf(GIANT_CENTIPEDE_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(LOWER_CAVERNS_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = UNDERGROUND_RIVER_ID,
                name = "Underground River",
                desc = "An underground river flows through a natural cavern. A boat once moored here is now rotted. The water is ice-cold and the current is strong. Something large moves in the depths.",
                locationType = LocationType.CAVE,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(LOWER_CAVERNS_ID, ExitDirection.SOUTH),
                    Exit(TREASURE_VAULT_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = TREASURE_VAULT_ID,
                name = "Hidden Treasure Vault",
                desc = "A secret vault where Rogahn and Zelligar stored their most precious treasures. The heavy door required keys from both heroes to open—a precaution against betrayal. Within lies the accumulated wealth of two legendary adventurers.",
                locationType = LocationType.DUNGEON,
                areaId = "b1-quasqueton",
                featureIds = emptyList(),
                creatureIds = listOf(GELATINOUS_CUBE_ID),
                itemIds = listOf(ROGAHNS_SWORD_ID, ZELLIGARS_WAND_ID, QUASQUETON_COIN_ID),
                exits = listOf(
                    Exit(UNDERGROUND_RIVER_ID, ExitDirection.WEST)
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
