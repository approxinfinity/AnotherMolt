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

    // ===== MAGIC SHOP ITEMS =====
    // Potions
    const val MINOR_HEALING_POTION_ID = "item-tdl-minor-healing-potion"
    const val HEALING_POTION_ID = "item-tdl-healing-potion"
    const val GREATER_HEALING_POTION_ID = "item-tdl-greater-healing-potion"
    const val MANA_POTION_ID = "item-tdl-mana-potion"
    const val STAMINA_POTION_ID = "item-tdl-stamina-potion"
    const val ANTIDOTE_ID = "item-tdl-antidote"
    // Accessories
    const val RING_OF_PROTECTION_ID = "item-tdl-ring-of-protection"
    const val AMULET_OF_VITALITY_ID = "item-tdl-amulet-of-vitality"
    const val RING_OF_MINOR_MAGIC_ID = "item-tdl-ring-of-minor-magic"
    const val SILVER_BRACERS_ID = "item-tdl-silver-bracers"
    // Staves
    const val STAFF_OF_SPARKS_ID = "item-tdl-staff-of-sparks"
    const val APPRENTICE_STAFF_ID = "item-tdl-apprentice-staff"

    // ===== ARMOR SHOP ITEMS =====
    // Tier 1 - Cloth/Padded (cheapest)
    const val PADDED_CAP_ID = "item-tdl-padded-cap"
    const val PADDED_TUNIC_ID = "item-tdl-padded-tunic"
    const val CLOTH_PANTS_ID = "item-tdl-cloth-pants"
    const val CLOTH_SHOES_ID = "item-tdl-cloth-shoes"
    const val CLOTH_GLOVES_ID = "item-tdl-cloth-gloves"
    // Tier 2 - Leather (moderate)
    const val LEATHER_CAP_ID = "item-tdl-leather-cap"
    const val LEATHER_ARMOR_ID = "item-tdl-leather-armor"
    const val LEATHER_PANTS_ID = "item-tdl-leather-pants"
    const val LEATHER_BOOTS_ID = "item-tdl-leather-boots"
    const val LEATHER_GLOVES_ID = "item-tdl-leather-gloves"
    // Tier 3 - Chainmail (expensive)
    const val CHAIN_COIF_ID = "item-tdl-chain-coif"
    const val CHAINMAIL_ID = "item-tdl-chainmail"
    const val CHAIN_LEGGINGS_ID = "item-tdl-chain-leggings"
    const val CHAIN_BOOTS_ID = "item-tdl-chain-boots"
    const val CHAIN_GAUNTLETS_ID = "item-tdl-chain-gauntlets"
    // Shields
    const val BUCKLER_ID = "item-tdl-buckler"
    const val ROUND_SHIELD_ID = "item-tdl-round-shield"
    const val KITE_SHIELD_ID = "item-tdl-kite-shield"

    // ===== WEAPONS SHOP ITEMS =====
    // Daggers & Knives
    const val DAGGER_ID = "item-tdl-dagger"
    const val FINE_DAGGER_ID = "item-tdl-fine-dagger"
    // Swords
    const val SHORTSWORD_ID = "item-tdl-shortsword"
    const val LONGSWORD_ID = "item-tdl-longsword"
    const val BROADSWORD_ID = "item-tdl-broadsword"
    // Axes
    const val HATCHET_ID = "item-tdl-hatchet"
    const val BATTLE_AXE_ID = "item-tdl-battle-axe"
    // Blunt
    const val CLUB_ID = "item-tdl-club"
    const val MACE_ID = "item-tdl-mace"
    const val WARHAMMER_ID = "item-tdl-warhammer"
    // Ranged
    const val SHORTBOW_ID = "item-tdl-shortbow"
    const val LONGBOW_ID = "item-tdl-longbow"
    const val CROSSBOW_ID = "item-tdl-crossbow"
    // Polearms
    const val QUARTERSTAFF_ID = "item-tdl-quarterstaff"
    const val SPEAR_ID = "item-tdl-spear"

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
        val allItems = mutableListOf<Item>()

        // ========== MAGIC SHOP ITEMS ==========
        // Potions
        allItems += Item(
            id = MINOR_HEALING_POTION_ID,
            name = "Minor Healing Potion",
            desc = "A small vial of red liquid. Restores a small amount of health.",
            featureIds = emptyList(),
            value = 8
        )
        allItems += Item(
            id = HEALING_POTION_ID,
            name = "Healing Potion",
            desc = "A shimmering red liquid in a crystal vial. Restores vitality when consumed.",
            featureIds = emptyList(),
            value = 25
        )
        allItems += Item(
            id = GREATER_HEALING_POTION_ID,
            name = "Greater Healing Potion",
            desc = "A large flask of deep crimson liquid that pulses with restorative magic.",
            featureIds = emptyList(),
            value = 75
        )
        allItems += Item(
            id = MANA_POTION_ID,
            name = "Mana Potion",
            desc = "A swirling blue elixir that restores magical energy.",
            featureIds = emptyList(),
            value = 30
        )
        allItems += Item(
            id = STAMINA_POTION_ID,
            name = "Stamina Potion",
            desc = "A fizzy green tonic that reinvigorates the body.",
            featureIds = emptyList(),
            value = 20
        )
        allItems += Item(
            id = ANTIDOTE_ID,
            name = "Antidote",
            desc = "A bitter herbal mixture that neutralizes most common poisons.",
            featureIds = emptyList(),
            value = 15
        )

        // Magic Accessories
        allItems += Item(
            id = RING_OF_PROTECTION_ID,
            name = "Ring of Protection",
            desc = "A silver band inscribed with protective runes that shimmer faintly in the light.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "finger",
            statBonuses = StatBonuses(defense = 2),
            value = 80
        )
        allItems += Item(
            id = AMULET_OF_VITALITY_ID,
            name = "Amulet of Vitality",
            desc = "A jade amulet shaped like a coiled serpent. It pulses warmly against your chest.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "amulet",
            statBonuses = StatBonuses(maxHp = 15),
            value = 60
        )
        allItems += Item(
            id = RING_OF_MINOR_MAGIC_ID,
            name = "Ring of Minor Magic",
            desc = "A copper ring set with a tiny sapphire. Enhances the wearer's magical potential.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "finger",
            statBonuses = StatBonuses(attack = 1, maxHp = 5),
            value = 45
        )
        allItems += Item(
            id = SILVER_BRACERS_ID,
            name = "Silver Bracers",
            desc = "Ornate silver bracers etched with arcane symbols. Popular among spellcasters.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "wrists",
            statBonuses = StatBonuses(defense = 1, maxHp = 5),
            value = 55
        )

        // Staves
        allItems += Item(
            id = APPRENTICE_STAFF_ID,
            name = "Apprentice Staff",
            desc = "A simple wooden staff used by magic students. Better than fighting unarmed.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 2),
            value = 15
        )
        allItems += Item(
            id = STAFF_OF_SPARKS_ID,
            name = "Staff of Sparks",
            desc = "A gnarled wooden staff topped with a crackling crystal. Lightning arcs between your fingers.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 5, maxHp = 10),
            value = 120
        )

        // ========== ARMOR SHOP ITEMS ==========
        // Tier 1 - Cloth/Padded
        allItems += Item(
            id = PADDED_CAP_ID,
            name = "Padded Cap",
            desc = "A simple cloth cap with light padding. Offers minimal protection.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "head",
            statBonuses = StatBonuses(defense = 1),
            value = 5
        )
        allItems += Item(
            id = PADDED_TUNIC_ID,
            name = "Padded Tunic",
            desc = "A quilted cloth tunic. Cheap but better than nothing.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "chest",
            statBonuses = StatBonuses(defense = 1),
            value = 10
        )
        allItems += Item(
            id = CLOTH_PANTS_ID,
            name = "Cloth Pants",
            desc = "Sturdy cloth trousers. Basic protection for the legs.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "legs",
            statBonuses = StatBonuses(defense = 1),
            value = 6
        )
        allItems += Item(
            id = CLOTH_SHOES_ID,
            name = "Cloth Shoes",
            desc = "Simple cloth footwear. Comfortable but fragile.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "feet",
            statBonuses = StatBonuses(defense = 0),
            value = 3
        )
        allItems += Item(
            id = CLOTH_GLOVES_ID,
            name = "Cloth Gloves",
            desc = "Light cloth gloves. Good for keeping hands clean.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "hands",
            statBonuses = StatBonuses(defense = 0),
            value = 2
        )

        // Tier 2 - Leather
        allItems += Item(
            id = LEATHER_CAP_ID,
            name = "Leather Cap",
            desc = "A hardened leather cap that protects the skull from glancing blows.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "head",
            statBonuses = StatBonuses(defense = 2),
            value = 20
        )
        allItems += Item(
            id = LEATHER_ARMOR_ID,
            name = "Leather Armor",
            desc = "Supple tanned leather reinforced with brass studs. Light enough for agile movement.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "chest",
            statBonuses = StatBonuses(defense = 3),
            value = 40
        )
        allItems += Item(
            id = LEATHER_PANTS_ID,
            name = "Leather Pants",
            desc = "Tough leather leggings that protect without hindering movement.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "legs",
            statBonuses = StatBonuses(defense = 2),
            value = 25
        )
        allItems += Item(
            id = LEATHER_BOOTS_ID,
            name = "Leather Boots",
            desc = "Sturdy boots with reinforced toes and thick soles.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "feet",
            statBonuses = StatBonuses(defense = 1),
            value = 18
        )
        allItems += Item(
            id = LEATHER_GLOVES_ID,
            name = "Leather Gloves",
            desc = "Well-fitted leather gloves that protect hands while maintaining dexterity.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "hands",
            statBonuses = StatBonuses(defense = 1),
            value = 12
        )

        // Tier 3 - Chainmail
        allItems += Item(
            id = CHAIN_COIF_ID,
            name = "Chain Coif",
            desc = "A hood of interlocking steel rings. Heavy but excellent head protection.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "head",
            statBonuses = StatBonuses(defense = 3),
            value = 50
        )
        allItems += Item(
            id = CHAINMAIL_ID,
            name = "Chainmail Hauberk",
            desc = "Interlocking steel rings forged by the lakeside smiths. Substantial protection.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "chest",
            statBonuses = StatBonuses(defense = 5),
            value = 90
        )
        allItems += Item(
            id = CHAIN_LEGGINGS_ID,
            name = "Chain Leggings",
            desc = "Chainmail leg armor. Heavier than leather but much more protective.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "legs",
            statBonuses = StatBonuses(defense = 3),
            value = 60
        )
        allItems += Item(
            id = CHAIN_BOOTS_ID,
            name = "Chain Boots",
            desc = "Steel-reinforced boots with chain covering. Protects feet from crushing blows.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "feet",
            statBonuses = StatBonuses(defense = 2),
            value = 45
        )
        allItems += Item(
            id = CHAIN_GAUNTLETS_ID,
            name = "Chain Gauntlets",
            desc = "Gauntlets of interlocking steel rings over leather. Excellent hand protection.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "hands",
            statBonuses = StatBonuses(defense = 2),
            value = 35
        )

        // Shields
        allItems += Item(
            id = BUCKLER_ID,
            name = "Buckler",
            desc = "A small, light shield strapped to the forearm. Quick to maneuver.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "off_hand",
            statBonuses = StatBonuses(defense = 2),
            value = 15
        )
        allItems += Item(
            id = ROUND_SHIELD_ID,
            name = "Round Shield",
            desc = "A medium wooden shield with an iron boss. Good balance of protection and weight.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "off_hand",
            statBonuses = StatBonuses(defense = 3),
            value = 30
        )
        allItems += Item(
            id = KITE_SHIELD_ID,
            name = "Kite Shield",
            desc = "A large teardrop-shaped shield. Excellent coverage but heavy.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "off_hand",
            statBonuses = StatBonuses(defense = 5),
            value = 55
        )

        // ========== WEAPONS SHOP ITEMS ==========
        // Daggers & Knives
        allItems += Item(
            id = DAGGER_ID,
            name = "Dagger",
            desc = "A simple iron dagger. Good for backup or sneaky attacks.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 2),
            value = 8
        )
        allItems += Item(
            id = FINE_DAGGER_ID,
            name = "Fine Dagger",
            desc = "A well-crafted steel dagger with excellent balance.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 3),
            value = 25
        )

        // Swords
        allItems += Item(
            id = SHORTSWORD_ID,
            name = "Shortsword",
            desc = "A short blade ideal for close-quarters combat.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 3),
            value = 18
        )
        allItems += Item(
            id = LONGSWORD_ID,
            name = "Longsword",
            desc = "A well-balanced blade. The weapon of choice for many warriors.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 4),
            value = 35
        )
        allItems += Item(
            id = BROADSWORD_ID,
            name = "Broadsword",
            desc = "A heavy blade with a wide cutting edge. Powerful but slow.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 5),
            value = 50
        )

        // Axes
        allItems += Item(
            id = HATCHET_ID,
            name = "Hatchet",
            desc = "A small axe useful for both combat and utility.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 2),
            value = 10
        )
        allItems += Item(
            id = BATTLE_AXE_ID,
            name = "Battle Axe",
            desc = "A heavy axe designed for cleaving through armor and shields.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 5),
            value = 45
        )

        // Blunt Weapons
        allItems += Item(
            id = CLUB_ID,
            name = "Club",
            desc = "A simple wooden club. Crude but effective.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 1),
            value = 2
        )
        allItems += Item(
            id = MACE_ID,
            name = "Mace",
            desc = "An iron mace with a flanged head. Effective against armored foes.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 4),
            value = 30
        )
        allItems += Item(
            id = WARHAMMER_ID,
            name = "Warhammer",
            desc = "A heavy hammer with a spiked head. What it lacks in elegance, it makes up for in crushing force.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 6),
            value = 70
        )

        // Ranged Weapons
        allItems += Item(
            id = SHORTBOW_ID,
            name = "Shortbow",
            desc = "A compact bow suitable for hunting and skirmishing.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 3),
            value = 20
        )
        allItems += Item(
            id = LONGBOW_ID,
            name = "Longbow",
            desc = "A tall yew bow strung with silk cord. Favored for its range and power.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 5),
            value = 55
        )
        allItems += Item(
            id = CROSSBOW_ID,
            name = "Crossbow",
            desc = "A mechanical bow that requires less training to use effectively.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 4),
            value = 40
        )

        // Polearms
        allItems += Item(
            id = QUARTERSTAFF_ID,
            name = "Quarterstaff",
            desc = "A sturdy wooden staff. Simple but versatile in trained hands.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 2),
            value = 5
        )
        allItems += Item(
            id = SPEAR_ID,
            name = "Spear",
            desc = "A pointed weapon with reach advantage. Good for keeping enemies at bay.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 3),
            value = 15
        )

        // Create items in database
        allItems.forEach { item ->
            if (ItemRepository.findById(item.id) == null) {
                ItemRepository.create(item)
            }
        }
    }

    private fun seedLocations() {
        // Magic Shop inventory
        val magicShopItems = listOf(
            MINOR_HEALING_POTION_ID, HEALING_POTION_ID, GREATER_HEALING_POTION_ID,
            MANA_POTION_ID, STAMINA_POTION_ID, ANTIDOTE_ID,
            RING_OF_PROTECTION_ID, AMULET_OF_VITALITY_ID, RING_OF_MINOR_MAGIC_ID, SILVER_BRACERS_ID,
            APPRENTICE_STAFF_ID, STAFF_OF_SPARKS_ID
        )

        // Armor Shop inventory - organized by tier and slot
        val armorShopItems = listOf(
            // Tier 1 - Cloth/Padded
            PADDED_CAP_ID, PADDED_TUNIC_ID, CLOTH_PANTS_ID, CLOTH_SHOES_ID, CLOTH_GLOVES_ID,
            // Tier 2 - Leather
            LEATHER_CAP_ID, LEATHER_ARMOR_ID, LEATHER_PANTS_ID, LEATHER_BOOTS_ID, LEATHER_GLOVES_ID,
            // Tier 3 - Chainmail
            CHAIN_COIF_ID, CHAINMAIL_ID, CHAIN_LEGGINGS_ID, CHAIN_BOOTS_ID, CHAIN_GAUNTLETS_ID,
            // Shields
            BUCKLER_ID, ROUND_SHIELD_ID, KITE_SHIELD_ID
        )

        // Weapons Shop inventory
        val weaponsShopItems = listOf(
            // Daggers
            DAGGER_ID, FINE_DAGGER_ID,
            // Swords
            SHORTSWORD_ID, LONGSWORD_ID, BROADSWORD_ID,
            // Axes
            HATCHET_ID, BATTLE_AXE_ID,
            // Blunt
            CLUB_ID, MACE_ID, WARHAMMER_ID,
            // Ranged
            SHORTBOW_ID, LONGBOW_ID, CROSSBOW_ID,
            // Polearms
            QUARTERSTAFF_ID, SPEAR_ID
        )

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
            desc = "Shelves of glowing potions and enchanted trinkets line the walls of this cramped shop. The air smells of lavender and ozone. An elderly woman peers at you from behind a cluttered counter, her eyes twinkling with arcane knowledge. 'Looking for something to keep you alive out there, dear?'",
            itemIds = magicShopItems,
            creatureIds = emptyList(),
            exits = listOf(
                Exit(locationId = TOWN_SQUARE_ID, direction = ExitDirection.SOUTH)
            ),
            featureIds = emptyList(),
            gridX = 0,
            gridY = -1,
            areaId = "tun-du-lac",
            locationType = LocationType.INDOOR
        )

        val armorShop = Location(
            id = ARMOR_SHOP_ID,
            name = "The Iron Scales",
            desc = "Racks of gleaming armor and shields fill this sturdy stone building. A broad-shouldered dwarf hammers at an anvil near the forge, sparks flying with each strike. The heat is almost unbearable. 'Need protection? I've got everything from simple padding to the finest chain. Take your pick!'",
            itemIds = armorShopItems,
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
            desc = "Blades and bows line the walls of this narrow shop. A scarred veteran tests the edge of a sword with practiced ease, nodding approvingly. 'Whether you favor steel or string, I've got what you need. Every weapon here has been battle-tested and proven.'",
            itemIds = weaponsShopItems,
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
            gridY = 1,
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
