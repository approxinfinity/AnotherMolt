package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for B2: The Keep on the Borderlands module.
 *
 * Classic D&D adventure featuring the Keep as a home base and the infamous
 * Caves of Chaos - a network of monster-inhabited caves carved into a ravine.
 * For character levels 1-3.
 *
 * Original module by Gary Gygax (1979), TSR Hobbies.
 */
object B2KeepBorderlandsSeed {
    private val log = LoggerFactory.getLogger(B2KeepBorderlandsSeed::class.java)

    private const val ATTRIBUTION = "B2: The Keep on the Borderlands (Gary Gygax, TSR 1979)"

    // ============== LOCATION IDS ==============
    const val KEEP_ENTRANCE_ID = "b2-keep-entrance"
    const val KEEP_OUTER_BAILEY_ID = "b2-keep-outer-bailey"
    const val KEEP_INNER_BAILEY_ID = "b2-keep-inner-bailey"
    const val KEEP_TAVERN_ID = "b2-keep-tavern"
    const val KEEP_GUILD_HALL_ID = "b2-keep-guild-hall"
    const val KEEP_CHAPEL_ID = "b2-keep-chapel"
    const val CAVES_RAVINE_ID = "b2-caves-ravine"
    const val KOBOLD_CAVES_ID = "b2-kobold-caves"
    const val ORC_CAVES_ID = "b2-orc-caves"
    const val GOBLIN_CAVES_ID = "b2-goblin-caves"
    const val HOBGOBLIN_CAVES_ID = "b2-hobgoblin-caves"
    const val BUGBEAR_CAVES_ID = "b2-bugbear-caves"
    const val GNOLL_CAVES_ID = "b2-gnoll-caves"
    const val MINOTAUR_CAVES_ID = "b2-minotaur-caves"
    const val SHRINE_OF_CHAOS_ID = "b2-shrine-of-chaos"

    // ============== CREATURE IDS ==============
    // Tier 1 - Levels 1-2
    const val KOBOLD_WARRIOR_ID = "creature-b2-kobold-warrior"
    const val KOBOLD_CHIEF_ID = "creature-b2-kobold-chief"
    const val GOBLIN_WARRIOR_ID = "creature-b2-goblin-warrior"
    const val GOBLIN_CHIEF_ID = "creature-b2-goblin-chief"
    const val ORC_WARRIOR_ID = "creature-b2-orc-warrior"
    const val ORC_LEADER_ID = "creature-b2-orc-leader"

    // Tier 2 - Levels 2-3
    const val HOBGOBLIN_WARRIOR_ID = "creature-b2-hobgoblin-warrior"
    const val HOBGOBLIN_CAPTAIN_ID = "creature-b2-hobgoblin-captain"
    const val GNOLL_WARRIOR_ID = "creature-b2-gnoll-warrior"
    const val GNOLL_CHIEFTAIN_ID = "creature-b2-gnoll-chieftain"
    const val BUGBEAR_ID = "creature-b2-bugbear"
    const val BUGBEAR_CHIEFTAIN_ID = "creature-b2-bugbear-chieftain"

    // Tier 3 - Boss
    const val MINOTAUR_ID = "creature-b2-minotaur"
    const val EVIL_PRIEST_ID = "creature-b2-evil-priest"

    // Wandering monsters
    const val DIRE_WOLF_ID = "creature-b2-dire-wolf"
    const val STIRGE_ID = "creature-b2-stirge"
    const val GIANT_RAT_ID = "creature-b2-giant-rat"
    const val FIRE_BEETLE_ID = "creature-b2-fire-beetle"

    // ============== ITEM IDS ==============
    const val KOBOLD_SPEAR_ID = "item-b2-kobold-spear"
    const val ORC_AXE_ID = "item-b2-orc-axe"
    const val HOBGOBLIN_SWORD_ID = "item-b2-hobgoblin-sword"
    const val GNOLL_POLEARM_ID = "item-b2-gnoll-polearm"
    const val BUGBEAR_MORNINGSTAR_ID = "item-b2-bugbear-morningstar"
    const val MINOTAUR_GREAT_AXE_ID = "item-b2-minotaur-great-axe"
    const val CHAOS_MEDALLION_ID = "item-b2-chaos-medallion"
    const val POTION_OF_HEALING_ID = "item-b2-potion-healing"
    const val SCROLL_OF_PROTECTION_ID = "item-b2-scroll-protection"
    const val MONSTER_HOARD_GOLD_ID = "item-b2-monster-hoard-gold"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_KOBOLD_ID = "loot-b2-kobold"
    const val LOOT_TABLE_GOBLIN_ID = "loot-b2-goblin"
    const val LOOT_TABLE_ORC_ID = "loot-b2-orc"
    const val LOOT_TABLE_HOBGOBLIN_ID = "loot-b2-hobgoblin"
    const val LOOT_TABLE_GNOLL_ID = "loot-b2-gnoll"
    const val LOOT_TABLE_BUGBEAR_ID = "loot-b2-bugbear"
    const val LOOT_TABLE_MINOTAUR_ID = "loot-b2-minotaur"
    const val LOOT_TABLE_SHRINE_ID = "loot-b2-shrine"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (KOBOLD_WARRIOR_ID !in existingCreatures) {
            log.info("Seeding B2: Keep on the Borderlands content...")
            seedItems()
            seedLootTables()
            seedCreatures()
            seedLocations()
            log.info("Seeded B2: Keep on the Borderlands content")
        }
    }

    private fun seedItems() {
        val items = listOf(
            // Weapons
            Item(
                id = KOBOLD_SPEAR_ID,
                name = "Kobold Spear",
                desc = "A crude spear with a fire-hardened point, favored by kobold warriors.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 2),
                value = 5,
                attribution = ATTRIBUTION
            ),
            Item(
                id = ORC_AXE_ID,
                name = "Orc Battle Axe",
                desc = "A heavy, brutish axe with crude but effective craftsmanship.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 5),
                value = 25,
                attribution = ATTRIBUTION
            ),
            Item(
                id = HOBGOBLIN_SWORD_ID,
                name = "Hobgoblin Longsword",
                desc = "A well-maintained military sword, showing the hobgoblins' martial discipline.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 6),
                value = 40,
                attribution = ATTRIBUTION
            ),
            Item(
                id = GNOLL_POLEARM_ID,
                name = "Gnoll Glaive",
                desc = "A vicious polearm decorated with trophy skulls and scraps of hide.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 7),
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = BUGBEAR_MORNINGSTAR_ID,
                name = "Bugbear Morningstar",
                desc = "A heavy spiked mace, bloodstained from many victims.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 8),
                value = 75,
                attribution = ATTRIBUTION
            ),
            Item(
                id = MINOTAUR_GREAT_AXE_ID,
                name = "Minotaur's Great Axe",
                desc = "A massive double-headed axe that requires tremendous strength to wield. The blade is notched from countless battles.",
                featureIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 12, defense = -2),
                value = 200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = CHAOS_MEDALLION_ID,
                name = "Medallion of Chaos",
                desc = "A dark medallion bearing the symbol of chaos, worn by priests of evil. Radiates a faint malevolent aura.",
                featureIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "neck",
                statBonuses = StatBonuses(attack = 3, maxMana = 15),
                value = 300,
                attribution = ATTRIBUTION
            ),
            // Consumables
            Item(
                id = POTION_OF_HEALING_ID,
                name = "Potion of Healing",
                desc = "A small vial of red liquid that restores vitality when consumed.",
                featureIds = emptyList(),
                equipmentType = "consumable",
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = SCROLL_OF_PROTECTION_ID,
                name = "Scroll of Protection from Evil",
                desc = "A blessed scroll that creates a protective ward against evil creatures.",
                featureIds = emptyList(),
                equipmentType = "consumable",
                value = 100,
                attribution = ATTRIBUTION
            ),
            // Treasure
            Item(
                id = MONSTER_HOARD_GOLD_ID,
                name = "Monster Hoard Coins",
                desc = "A collection of coins stolen from hapless travelers and caravans.",
                featureIds = emptyList(),
                value = 50,
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
                id = LOOT_TABLE_KOBOLD_ID,
                name = "Kobold Loot",
                entries = listOf(
                    LootEntry(KOBOLD_SPEAR_ID, 0.10f),
                    LootEntry(MONSTER_HOARD_GOLD_ID, 0.40f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_GOBLIN_ID,
                name = "Goblin Loot",
                entries = listOf(
                    LootEntry(KOBOLD_SPEAR_ID, 0.15f),
                    LootEntry(MONSTER_HOARD_GOLD_ID, 0.45f),
                    LootEntry(POTION_OF_HEALING_ID, 0.05f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_ORC_ID,
                name = "Orc Loot",
                entries = listOf(
                    LootEntry(ORC_AXE_ID, 0.10f),
                    LootEntry(MONSTER_HOARD_GOLD_ID, 0.50f),
                    LootEntry(POTION_OF_HEALING_ID, 0.08f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_HOBGOBLIN_ID,
                name = "Hobgoblin Loot",
                entries = listOf(
                    LootEntry(HOBGOBLIN_SWORD_ID, 0.12f),
                    LootEntry(MONSTER_HOARD_GOLD_ID, 0.55f),
                    LootEntry(POTION_OF_HEALING_ID, 0.10f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_GNOLL_ID,
                name = "Gnoll Loot",
                entries = listOf(
                    LootEntry(GNOLL_POLEARM_ID, 0.12f),
                    LootEntry(MONSTER_HOARD_GOLD_ID, 0.55f),
                    LootEntry(POTION_OF_HEALING_ID, 0.10f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_BUGBEAR_ID,
                name = "Bugbear Loot",
                entries = listOf(
                    LootEntry(BUGBEAR_MORNINGSTAR_ID, 0.15f),
                    LootEntry(MONSTER_HOARD_GOLD_ID, 0.60f),
                    LootEntry(POTION_OF_HEALING_ID, 0.12f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_MINOTAUR_ID,
                name = "Minotaur Loot",
                entries = listOf(
                    LootEntry(MINOTAUR_GREAT_AXE_ID, 0.30f),
                    LootEntry(MONSTER_HOARD_GOLD_ID, 0.80f),
                    LootEntry(POTION_OF_HEALING_ID, 0.25f)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_SHRINE_ID,
                name = "Shrine of Chaos Loot",
                entries = listOf(
                    LootEntry(CHAOS_MEDALLION_ID, 0.50f),
                    LootEntry(SCROLL_OF_PROTECTION_ID, 0.40f),
                    LootEntry(MONSTER_HOARD_GOLD_ID, 1.0f),
                    LootEntry(POTION_OF_HEALING_ID, 0.60f)
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
            // Tier 1 Creatures
            Creature(
                id = KOBOLD_WARRIOR_ID,
                name = "Kobold Warrior",
                description = "A small, reptilian humanoid with reddish-brown scales. Cowardly alone but dangerous in numbers, kobolds rely on traps and ambushes.",
                level = 1,
                hitDice = 1,
                armorClass = 13,
                attackBonus = 2,
                damage = "1d4",
                maxHp = 5,
                currentHp = 5,
                xpValue = 25,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_KOBOLD_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = KOBOLD_CHIEF_ID,
                name = "Kobold Chief",
                description = "A larger, meaner kobold wearing crude armor made from stolen equipment. Commands the tribe through cunning and cruelty.",
                level = 2,
                hitDice = 2,
                armorClass = 14,
                attackBonus = 3,
                damage = "1d6",
                maxHp = 12,
                currentHp = 12,
                xpValue = 50,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_KOBOLD_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GOBLIN_WARRIOR_ID,
                name = "Goblin Warrior",
                description = "A small, ugly humanoid with yellowed fangs and beady red eyes. Goblins are cruel and cowardly, preferring to attack from hiding.",
                level = 1,
                hitDice = 1,
                armorClass = 14,
                attackBonus = 3,
                damage = "1d6",
                maxHp = 7,
                currentHp = 7,
                xpValue = 30,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_GOBLIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GOBLIN_CHIEF_ID,
                name = "Goblin Chief",
                description = "The largest and meanest goblin, wearing a crown made of teeth and bones. Rules through fear and violence.",
                level = 2,
                hitDice = 2,
                armorClass = 15,
                attackBonus = 4,
                damage = "1d6+1",
                maxHp = 14,
                currentHp = 14,
                xpValue = 60,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_GOBLIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ORC_WARRIOR_ID,
                name = "Orc Warrior",
                description = "A brutish humanoid with grayish skin, pig-like face, and prominent tusks. Orcs are fierce fighters who respect only strength.",
                level = 1,
                hitDice = 1,
                armorClass = 13,
                attackBonus = 4,
                damage = "1d8",
                maxHp = 8,
                currentHp = 8,
                xpValue = 35,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_ORC_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = ORC_LEADER_ID,
                name = "Orc Leader",
                description = "A scarred, battle-hardened orc wearing a necklace of enemy skulls. Commands through brutal displays of strength.",
                level = 2,
                hitDice = 2,
                armorClass = 14,
                attackBonus = 5,
                damage = "1d8+2",
                maxHp = 16,
                currentHp = 16,
                xpValue = 75,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_ORC_ID,
                attribution = ATTRIBUTION
            ),

            // Tier 2 Creatures
            Creature(
                id = HOBGOBLIN_WARRIOR_ID,
                name = "Hobgoblin Warrior",
                description = "A tall, muscular goblinoid with orange-red skin. Hobgoblins are disciplined soldiers, fighting in organized formations.",
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
                id = HOBGOBLIN_CAPTAIN_ID,
                name = "Hobgoblin Captain",
                description = "A hobgoblin officer in well-maintained armor, bearing the insignia of rank. Leads with tactical precision.",
                level = 3,
                hitDice = 3,
                armorClass = 17,
                attackBonus = 6,
                damage = "1d8+2",
                maxHp = 22,
                currentHp = 22,
                xpValue = 100,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_HOBGOBLIN_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GNOLL_WARRIOR_ID,
                name = "Gnoll",
                description = "A large, hyena-headed humanoid covered in spotted fur. Gnolls are savage raiders who take pleasure in cruelty.",
                level = 2,
                hitDice = 2,
                armorClass = 15,
                attackBonus = 5,
                damage = "1d8+1",
                maxHp = 14,
                currentHp = 14,
                xpValue = 55,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_GNOLL_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GNOLL_CHIEFTAIN_ID,
                name = "Gnoll Chieftain",
                description = "A massive gnoll wearing a cape of human scalps. Its hyena-like laughter echoes through the caves as it hunts.",
                level = 3,
                hitDice = 3,
                armorClass = 16,
                attackBonus = 6,
                damage = "1d10+2",
                maxHp = 26,
                currentHp = 26,
                xpValue = 120,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_GNOLL_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = BUGBEAR_ID,
                name = "Bugbear",
                description = "A huge, hairy goblinoid that moves with surprising stealth for its size. Bugbears are ambush predators who strike from shadows.",
                level = 3,
                hitDice = 3,
                armorClass = 15,
                attackBonus = 6,
                damage = "1d8+2",
                maxHp = 22,
                currentHp = 22,
                xpValue = 100,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_BUGBEAR_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = BUGBEAR_CHIEFTAIN_ID,
                name = "Bugbear Chieftain",
                description = "The largest and most cunning of the bugbears, wearing a gruesome helmet made from an ogre skull.",
                level = 4,
                hitDice = 4,
                armorClass = 16,
                attackBonus = 7,
                damage = "1d10+3",
                maxHp = 32,
                currentHp = 32,
                xpValue = 175,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_BUGBEAR_ID,
                attribution = ATTRIBUTION
            ),

            // Tier 3 - Boss Creatures
            Creature(
                id = MINOTAUR_ID,
                name = "Minotaur",
                description = "A massive creature with the body of a man and the head of a bull. It guards the deepest caves, wearing a labyrinthine maze into the stone floor.",
                level = 5,
                hitDice = 6,
                armorClass = 16,
                attackBonus = 9,
                damage = "2d6+4",
                maxHp = 45,
                currentHp = 45,
                xpValue = 500,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_MINOTAUR_ID,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = EVIL_PRIEST_ID,
                name = "Evil Priest of Chaos",
                description = "A robed figure serving the dark powers, conducting unholy rituals in the Shrine of Chaos. His eyes glow with malevolent power.",
                level = 5,
                hitDice = 5,
                armorClass = 15,
                attackBonus = 7,
                damage = "1d6+3",
                maxHp = 38,
                currentHp = 38,
                xpValue = 600,
                abilityIds = emptyList(),
                lootTableId = LOOT_TABLE_SHRINE_ID,
                attribution = ATTRIBUTION
            ),

            // Wandering Monsters
            Creature(
                id = DIRE_WOLF_ID,
                name = "Dire Wolf",
                description = "A massive wolf with glowing eyes and slavering jaws. Often found as a mount for goblin scouts.",
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
                id = STIRGE_ID,
                name = "Stirge",
                description = "A bat-like creature with a long, blood-draining proboscis. Attacks in swarms, latching onto victims to drain their blood.",
                level = 1,
                hitDice = 1,
                armorClass = 13,
                attackBonus = 4,
                damage = "1d4",
                maxHp = 4,
                currentHp = 4,
                xpValue = 25,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GIANT_RAT_ID,
                name = "Giant Rat",
                description = "A dog-sized rat with matted fur and yellowed teeth. Disease carriers that swarm through the caves.",
                level = 1,
                hitDice = 1,
                armorClass = 12,
                attackBonus = 2,
                damage = "1d4",
                maxHp = 4,
                currentHp = 4,
                xpValue = 15,
                abilityIds = emptyList(),
                lootTableId = null,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = FIRE_BEETLE_ID,
                name = "Fire Beetle",
                description = "A large beetle with glowing glands that produce a reddish light. The glands can be harvested and used as torches.",
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
            )
        )

        creatures.forEach { creature ->
            if (CreatureRepository.findById(creature.id) == null) {
                CreatureRepository.create(creature)
            }
        }
    }

    private fun seedLocations() {
        // The Keep serves as a safe haven/home base
        val keepLocations = listOf(
            Location(
                id = KEEP_ENTRANCE_ID,
                name = "The Keep - Gate",
                desc = "The sturdy gates of the Keep loom before you. Two towers flank the entrance, with guards watching from the battlements. A drawbridge spans a dry moat, and murder holes are visible in the gatehouse ceiling. The guards eye you suspiciously but wave you through after inspection.",
                locationType = LocationType.OUTDOOR,
                areaId = "b2-keep",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(KEEP_OUTER_BAILEY_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = KEEP_OUTER_BAILEY_ID,
                name = "The Keep - Outer Bailey",
                desc = "The outer courtyard of the Keep bustles with activity. A smithy rings with hammer blows, merchants hawk wares from stalls, and peasants go about their daily business. Stone walls rise on all sides, offering protection from the dangerous borderlands beyond.",
                locationType = LocationType.OUTDOOR,
                areaId = "b2-keep",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(KEEP_ENTRANCE_ID, ExitDirection.SOUTH),
                    Exit(KEEP_TAVERN_ID, ExitDirection.EAST),
                    Exit(KEEP_GUILD_HALL_ID, ExitDirection.WEST),
                    Exit(KEEP_INNER_BAILEY_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = KEEP_TAVERN_ID,
                name = "One-Eyed Cat Tavern",
                desc = "A smoky, low-ceilinged common room filled with rough wooden tables. A one-eyed taxidermied cat watches from above the bar. Travelers, mercenaries, and local peasants share rumors over watered ale. The taverner keeps a cudgel behind the bar for rowdy customers.",
                locationType = LocationType.INDOOR,
                areaId = "b2-keep",
                featureIds = listOf(GeneralStoreSeed.COOKING_FIRE_FEATURE),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(KEEP_OUTER_BAILEY_ID, ExitDirection.WEST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = KEEP_GUILD_HALL_ID,
                name = "Travelers' Guild Hall",
                desc = "A large hall where adventurers can post notices, seek employment, and find fellow travelers. A bulletin board displays wanted posters, requests for escorts, and warnings about the Caves of Chaos.",
                locationType = LocationType.INDOOR,
                areaId = "b2-keep",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(KEEP_OUTER_BAILEY_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = KEEP_CHAPEL_ID,
                name = "Chapel of the Lawful",
                desc = "A small but well-maintained chapel dedicated to the forces of Law. Stained glass windows depict the eternal battle between good and evil. The curate offers healing and blessings to those who fight against chaos.",
                locationType = LocationType.INDOOR,
                areaId = "b2-keep",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(KEEP_INNER_BAILEY_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = KEEP_INNER_BAILEY_ID,
                name = "The Keep - Inner Bailey",
                desc = "The inner courtyard contains the castellan's tower and the barracks. Well-armed soldiers drill in formation while a sergeant barks orders. This is the heart of the Keep's defenses against the borderland threats.",
                locationType = LocationType.OUTDOOR,
                areaId = "b2-keep",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(KEEP_OUTER_BAILEY_ID, ExitDirection.SOUTH),
                    Exit(KEEP_CHAPEL_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            )
        )

        // Caves of Chaos
        val caveLocations = listOf(
            Location(
                id = CAVES_RAVINE_ID,
                name = "Ravine of the Caves",
                desc = "A steep-sided ravine cuts into the wilderness, its walls pocked with dark cave mouths. Carrion birds circle overhead and a foul smell wafts from the shadows. This is the Caves of Chaos - lair of the monsters threatening the borderlands.",
                locationType = LocationType.OUTDOOR,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = emptyList(),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(KOBOLD_CAVES_ID, ExitDirection.WEST),
                    Exit(ORC_CAVES_ID, ExitDirection.NORTH),
                    Exit(GOBLIN_CAVES_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = KOBOLD_CAVES_ID,
                name = "Kobold Lair",
                desc = "A low-ceilinged cave system stinking of reptiles. Crude pit traps and warning chimes of bone mark the kobolds' territory. Torchlight flickers from deeper within, and you hear chittering voices arguing in their guttural tongue.",
                locationType = LocationType.CAVE,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = listOf(KOBOLD_WARRIOR_ID, KOBOLD_WARRIOR_ID, KOBOLD_CHIEF_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(CAVES_RAVINE_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = GOBLIN_CAVES_ID,
                name = "Goblin Lair",
                desc = "A maze of twisting passages marked by goblin totems and crude graffiti. The stench of unwashed bodies and rotting food fills the air. Goblins scurry through the shadows, setting ambushes for the unwary.",
                locationType = LocationType.CAVE,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = listOf(GOBLIN_WARRIOR_ID, GOBLIN_WARRIOR_ID, GOBLIN_CHIEF_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(CAVES_RAVINE_ID, ExitDirection.WEST),
                    Exit(HOBGOBLIN_CAVES_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = ORC_CAVES_ID,
                name = "Orc Lair",
                desc = "A brutal cave decorated with skulls and weapons. Orc guards challenge all who approach with guttural threats. The tribe is split into two feuding factions, though they unite against common enemies.",
                locationType = LocationType.CAVE,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = listOf(ORC_WARRIOR_ID, ORC_WARRIOR_ID, ORC_LEADER_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(CAVES_RAVINE_ID, ExitDirection.SOUTH),
                    Exit(GNOLL_CAVES_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = HOBGOBLIN_CAVES_ID,
                name = "Hobgoblin Lair",
                desc = "An organized military installation carved into the rock. Hobgoblin soldiers patrol in shifts, and a password is required for entry. Their discipline makes them the most dangerous of the humanoid tribes.",
                locationType = LocationType.CAVE,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = listOf(HOBGOBLIN_WARRIOR_ID, HOBGOBLIN_WARRIOR_ID, HOBGOBLIN_CAPTAIN_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(GOBLIN_CAVES_ID, ExitDirection.SOUTH),
                    Exit(BUGBEAR_CAVES_ID, ExitDirection.EAST)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = GNOLL_CAVES_ID,
                name = "Gnoll Lair",
                desc = "A foul den littered with gnawed bones and scraps of flesh. The hyena-headed gnolls are savage and cruel, keeping prisoners for food and sport. Their cackling laughter echoes through the passages.",
                locationType = LocationType.CAVE,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = listOf(GNOLL_WARRIOR_ID, GNOLL_WARRIOR_ID, GNOLL_CHIEFTAIN_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(ORC_CAVES_ID, ExitDirection.SOUTH),
                    Exit(MINOTAUR_CAVES_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = BUGBEAR_CAVES_ID,
                name = "Bugbear Lair",
                desc = "Deceptively quiet passages that seem empty. The bugbears are masters of stealth, hiding in shadows and striking from ambush. Their victims' belongings hang as trophies on the walls.",
                locationType = LocationType.CAVE,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = listOf(BUGBEAR_ID, BUGBEAR_ID, BUGBEAR_CHIEFTAIN_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(HOBGOBLIN_CAVES_ID, ExitDirection.WEST),
                    Exit(SHRINE_OF_CHAOS_ID, ExitDirection.NORTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = MINOTAUR_CAVES_ID,
                name = "Minotaur's Labyrinth",
                desc = "A twisting maze of tunnels that the minotaur has worn into a labyrinth over decades. Bones of lost adventurers litter the passages. The beast's bellowing roar echoes from somewhere within.",
                locationType = LocationType.CAVE,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = listOf(MINOTAUR_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(GNOLL_CAVES_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            ),
            Location(
                id = SHRINE_OF_CHAOS_ID,
                name = "Shrine of Chaos",
                desc = "A hidden temple dedicated to dark powers. Black candles burn with unnatural flames, and an altar stained with old blood dominates the chamber. The evil priest conducts unholy rituals here, coordinating the monsters' attacks on civilization.",
                locationType = LocationType.DUNGEON,
                areaId = "b2-caves",
                featureIds = emptyList(),
                creatureIds = listOf(EVIL_PRIEST_ID),
                itemIds = emptyList(),
                exits = listOf(
                    Exit(BUGBEAR_CAVES_ID, ExitDirection.SOUTH)
                ),
                attribution = ATTRIBUTION
            )
        )

        (keepLocations + caveLocations).forEach { location ->
            if (LocationRepository.findById(location.id) == null) {
                LocationRepository.create(location)
            }
        }
    }
}
