package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for Tun du Lac town interior - shops (magic, armor, weapons) and inn.
 * Call seedIfEmpty() on server startup to populate initial data.
 */
object TunDuLacSeed {
    private val log = LoggerFactory.getLogger(TunDuLacSeed::class.java)

    // The existing overworld location for Tun du Lac
    const val TUN_DU_LAC_OVERWORLD_ID = "e98d7aeb-5d81-46ad-b667-8479d1673517"

    // Town interior location IDs
    const val TOWN_SQUARE_ID = "tun-du-lac-town-square"
    const val MAGIC_SHOP_ID = "tun-du-lac-magic-shop"
    const val ARMOR_SHOP_ID = "tun-du-lac-armor-shop"
    const val WEAPONS_SHOP_ID = "tun-du-lac-weapons-shop"
    const val INN_ID = "tun-du-lac-inn"

    // Item IDs - Magic Shop
    const val POTION_OF_HEALING_ID = "item-tdl-potion-of-healing"
    const val RING_OF_PROTECTION_ID = "item-tdl-ring-of-protection"
    const val STAFF_OF_SPARKS_ID = "item-tdl-staff-of-sparks"
    const val AMULET_OF_VITALITY_ID = "item-tdl-amulet-of-vitality"

    // Item IDs - Armor Shop
    const val LEATHER_ARMOR_ID = "item-tdl-leather-armor"
    const val CHAINMAIL_ID = "item-tdl-chainmail"
    const val IRON_SHIELD_ID = "item-tdl-iron-shield"
    const val STEEL_HELM_ID = "item-tdl-steel-helm"

    // Item IDs - Weapons Shop
    const val IRON_SWORD_ID = "item-tdl-iron-sword"
    const val WAR_HAMMER_ID = "item-tdl-war-hammer"
    const val LONGBOW_ID = "item-tdl-longbow"
    const val DAGGER_OF_SPEED_ID = "item-tdl-dagger-of-speed"

    // Inn rest cost
    const val INN_REST_COST = 25

    fun seedIfEmpty() {
        val existingLocation = LocationRepository.findById(TOWN_SQUARE_ID)
        if (existingLocation != null) return

        log.info("Seeding Tun du Lac town content...")
        seedItems()
        seedLocations()
        addEnterExitToOverworldLocation()
        log.info("Seeded Tun du Lac town content")
    }

    private fun seedItems() {
        // Magic Shop items
        val potionOfHealing = Item(
            id = POTION_OF_HEALING_ID,
            name = "Potion of Healing",
            desc = "A shimmering red liquid in a crystal vial. Restores vitality when consumed.",
            featureIds = emptyList(),
            value = 15
        )
        val ringOfProtection = Item(
            id = RING_OF_PROTECTION_ID,
            name = "Ring of Protection",
            desc = "A silver band inscribed with protective runes that shimmer faintly in the light.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "ring",
            statBonuses = StatBonuses(defense = 2),
            value = 80
        )
        val staffOfSparks = Item(
            id = STAFF_OF_SPARKS_ID,
            name = "Staff of Sparks",
            desc = "A gnarled wooden staff topped with a crackling crystal. Tiny arcs of lightning dance between your fingers when you hold it.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 5, maxHp = 10),
            value = 120
        )
        val amuletOfVitality = Item(
            id = AMULET_OF_VITALITY_ID,
            name = "Amulet of Vitality",
            desc = "A jade amulet shaped like a coiled serpent. It pulses warmly against your chest, fortifying your constitution.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "amulet",
            statBonuses = StatBonuses(maxHp = 20),
            value = 60
        )

        // Armor Shop items
        val leatherArmor = Item(
            id = LEATHER_ARMOR_ID,
            name = "Leather Armor",
            desc = "Supple tanned leather reinforced with brass studs. Light enough for agile movement yet sturdy against blades.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "chest",
            statBonuses = StatBonuses(defense = 3),
            value = 40
        )
        val chainmail = Item(
            id = CHAINMAIL_ID,
            name = "Chainmail",
            desc = "Interlocking steel rings forged by the lakeside smiths. The weight is substantial but the protection is unmatched.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "chest",
            statBonuses = StatBonuses(defense = 6),
            value = 90
        )
        val ironShield = Item(
            id = IRON_SHIELD_ID,
            name = "Iron Shield",
            desc = "A round shield bearing the fish-and-wave crest of Tun du Lac. Dented from many battles but still reliable.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "off_hand",
            statBonuses = StatBonuses(defense = 4),
            value = 50
        )
        val steelHelm = Item(
            id = STEEL_HELM_ID,
            name = "Steel Helm",
            desc = "A polished steel helmet with a nose guard. The interior is lined with soft leather for comfort.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "head",
            statBonuses = StatBonuses(defense = 2),
            value = 35
        )

        // Weapons Shop items
        val ironSword = Item(
            id = IRON_SWORD_ID,
            name = "Iron Sword",
            desc = "A well-balanced blade forged in the local smithy. The edge gleams with a keen sharpness.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 4),
            value = 45
        )
        val warHammer = Item(
            id = WAR_HAMMER_ID,
            name = "War Hammer",
            desc = "A heavy hammer with a spiked head. What it lacks in elegance, it makes up for in crushing force.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 6),
            value = 70
        )
        val longbow = Item(
            id = LONGBOW_ID,
            name = "Longbow",
            desc = "A tall yew bow strung with silk cord. Favored by the lakeside hunters for its range and power.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 5),
            value = 55
        )
        val daggerOfSpeed = Item(
            id = DAGGER_OF_SPEED_ID,
            name = "Dagger of Speed",
            desc = "An enchanted dagger that seems to leap from its sheath. The blade is almost too fast to see when it strikes.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 3, maxHp = 5),
            value = 100
        )

        val allItems = listOf(
            potionOfHealing, ringOfProtection, staffOfSparks, amuletOfVitality,
            leatherArmor, chainmail, ironShield, steelHelm,
            ironSword, warHammer, longbow, daggerOfSpeed
        )

        allItems.forEach { item ->
            if (ItemRepository.findById(item.id) == null) {
                ItemRepository.create(item)
            }
        }
    }

    private fun seedLocations() {
        val townSquare = Location(
            id = TOWN_SQUARE_ID,
            name = "Town Square",
            desc = "The central square of Tun du Lac. A stone fountain carved with leaping fish stands at its center, water trickling gently. Cobblestone paths lead to the surrounding shops and the welcoming glow of the inn to the south.",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = listOf(
                Exit(locationId = TUN_DU_LAC_OVERWORLD_ID, direction = ExitDirection.ENTER),
                Exit(locationId = MAGIC_SHOP_ID, direction = ExitDirection.NORTH),
                Exit(locationId = ARMOR_SHOP_ID, direction = ExitDirection.EAST),
                Exit(locationId = WEAPONS_SHOP_ID, direction = ExitDirection.WEST),
                Exit(locationId = INN_ID, direction = ExitDirection.SOUTH)
            ),
            featureIds = emptyList(),
            gridX = 0,
            gridY = 0,
            areaId = "tun-du-lac",
            locationType = LocationType.INDOOR
        )

        val magicShop = Location(
            id = MAGIC_SHOP_ID,
            name = "Mirela's Curios",
            desc = "Shelves of glowing potions and enchanted trinkets line the walls of this cramped shop. The air smells of lavender and ozone. An elderly woman peers at you from behind a cluttered counter, her eyes twinkling with arcane knowledge.",
            itemIds = listOf(POTION_OF_HEALING_ID, RING_OF_PROTECTION_ID, STAFF_OF_SPARKS_ID, AMULET_OF_VITALITY_ID),
            creatureIds = emptyList(),
            exits = listOf(
                Exit(locationId = TOWN_SQUARE_ID, direction = ExitDirection.SOUTH)
            ),
            featureIds = emptyList(),
            gridX = 0,
            gridY = 1,
            areaId = "tun-du-lac",
            locationType = LocationType.INDOOR
        )

        val armorShop = Location(
            id = ARMOR_SHOP_ID,
            name = "The Iron Scales",
            desc = "Racks of gleaming armor and shields fill this sturdy stone building. A broad-shouldered dwarf hammers at an anvil near the forge, sparks flying with each strike. The heat is almost unbearable.",
            itemIds = listOf(LEATHER_ARMOR_ID, CHAINMAIL_ID, IRON_SHIELD_ID, STEEL_HELM_ID),
            creatureIds = emptyList(),
            exits = listOf(
                Exit(locationId = TOWN_SQUARE_ID, direction = ExitDirection.WEST)
            ),
            featureIds = emptyList(),
            gridX = 1,
            gridY = 0,
            areaId = "tun-du-lac",
            locationType = LocationType.INDOOR
        )

        val weaponsShop = Location(
            id = WEAPONS_SHOP_ID,
            name = "Blade & Bow",
            desc = "Blades and bows line the walls of this narrow shop. A scarred veteran tests the edge of a sword with practiced ease, nodding approvingly. Every weapon here has been battle-tested and proven.",
            itemIds = listOf(IRON_SWORD_ID, WAR_HAMMER_ID, LONGBOW_ID, DAGGER_OF_SPEED_ID),
            creatureIds = emptyList(),
            exits = listOf(
                Exit(locationId = TOWN_SQUARE_ID, direction = ExitDirection.EAST)
            ),
            featureIds = emptyList(),
            gridX = -1,
            gridY = 0,
            areaId = "tun-du-lac",
            locationType = LocationType.INDOOR
        )

        val inn = Location(
            id = INN_ID,
            name = "The Lakeside Inn",
            desc = "A warm hearth crackles in the corner of this cozy inn. The smell of fresh bread and roasted fish fills the air. Weary travelers rest at wooden tables, nursing mugs of ale. A friendly innkeeper offers rooms for the night.",
            itemIds = emptyList(),
            creatureIds = emptyList(),
            exits = listOf(
                Exit(locationId = TOWN_SQUARE_ID, direction = ExitDirection.NORTH)
            ),
            featureIds = emptyList(),
            gridX = 0,
            gridY = -1,
            areaId = "tun-du-lac",
            locationType = LocationType.INDOOR
        )

        listOf(townSquare, magicShop, armorShop, weaponsShop, inn).forEach { location ->
            if (LocationRepository.findById(location.id) == null) {
                LocationRepository.create(location)
            }
        }
    }

    private fun addEnterExitToOverworldLocation() {
        val overworldLocation = LocationRepository.findById(TUN_DU_LAC_OVERWORLD_ID) ?: return

        // Check if ENTER exit already exists
        if (overworldLocation.exits.any { it.direction == ExitDirection.ENTER }) return

        val updatedExits = overworldLocation.exits + Exit(
            locationId = TOWN_SQUARE_ID,
            direction = ExitDirection.ENTER
        )
        LocationRepository.update(overworldLocation.copy(exits = updatedExits))
        log.info("Added ENTER exit to Tun du Lac overworld location")
    }
}
