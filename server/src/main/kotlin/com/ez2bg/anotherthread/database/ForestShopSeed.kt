package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for the Hermit's Hollow - a hidden shop in the forest
 * with special, unique items not found in town.
 */
object ForestShopSeed {
    private val log = LoggerFactory.getLogger(ForestShopSeed::class.java)

    const val HERMITS_HOLLOW_ID = "location-hermits-hollow"

    // Special Forest Shop Items
    const val MOONPETAL_SALVE_ID = "item-forest-moonpetal-salve"
    const val IRONBARK_TONIC_ID = "item-forest-ironbark-tonic"
    const val WOLFSBANE_POISON_ID = "item-forest-wolfsbane-poison"
    const val ELVEN_CLOAK_ID = "item-forest-elven-cloak"
    const val HUNTERS_CHARM_ID = "item-forest-hunters-charm"
    const val WILDWOOD_BOW_ID = "item-forest-wildwood-bow"
    const val THORNBLADE_ID = "item-forest-thornblade"
    const val DRUIDS_STAFF_ID = "item-forest-druids-staff"
    const val BEAST_HIDE_VEST_ID = "item-forest-beast-hide-vest"
    const val BOOTS_OF_THE_FOREST_ID = "item-forest-boots-of-the-forest"
    const val AMULET_OF_NATURE_ID = "item-forest-amulet-of-nature"
    const val RING_OF_THE_WILD_ID = "item-forest-ring-of-the-wild"

    fun seedIfEmpty() {
        val existingLocation = LocationRepository.findById(HERMITS_HOLLOW_ID)
        if (existingLocation != null) return

        log.info("Seeding Forest Shop (Hermit's Hollow) content...")
        seedItems()
        seedLocation()
        log.info("Seeded Forest Shop content")
    }

    private fun seedItems() {
        val allItems = mutableListOf<Item>()

        // ========== SPECIAL CONSUMABLES ==========
        allItems += Item(
            id = MOONPETAL_SALVE_ID,
            name = "Moonpetal Salve",
            desc = "A luminescent paste made from flowers that bloom only under moonlight. Heals wounds and cures most ailments.",
            featureIds = emptyList(),
            value = 50
        )
        allItems += Item(
            id = IRONBARK_TONIC_ID,
            name = "Ironbark Tonic",
            desc = "A thick brown liquid brewed from ironwood bark. Temporarily hardens the skin against attacks.",
            featureIds = emptyList(),
            value = 65
        )
        allItems += Item(
            id = WOLFSBANE_POISON_ID,
            name = "Wolfsbane Poison",
            desc = "A deadly extract from the wolfsbane flower. Apply to weapons for extra damage against beasts.",
            featureIds = emptyList(),
            value = 40
        )

        // ========== SPECIAL EQUIPMENT ==========
        allItems += Item(
            id = ELVEN_CLOAK_ID,
            name = "Elven Cloak",
            desc = "A shimmering grey-green cloak that seems to shift colors with the light. Helps the wearer blend into natural surroundings.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "back",
            statBonuses = StatBonuses(defense = 2, maxHp = 10),
            value = 150
        )
        allItems += Item(
            id = HUNTERS_CHARM_ID,
            name = "Hunter's Charm",
            desc = "A bone talisman carved with animal runes. Sharpens the senses and quickens reflexes.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "amulet",
            statBonuses = StatBonuses(attack = 2, maxHp = 5),
            value = 85
        )

        // ========== SPECIAL WEAPONS ==========
        allItems += Item(
            id = WILDWOOD_BOW_ID,
            name = "Wildwood Bow",
            desc = "A recurve bow carved from a still-living branch of an ancient oak. The wood seems to guide arrows toward their targets.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 7),
            value = 180
        )
        allItems += Item(
            id = THORNBLADE_ID,
            name = "Thornblade",
            desc = "A wicked sword crafted from petrified thorns and dark iron. Cuts leave painful wounds that fester.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 6, maxHp = 5),
            value = 160
        )
        allItems += Item(
            id = DRUIDS_STAFF_ID,
            name = "Druid's Staff",
            desc = "A gnarled oak staff crowned with a living cluster of mistletoe. Pulses with nature's power.",
            featureIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 4, maxHp = 20),
            value = 200
        )

        // ========== SPECIAL ARMOR ==========
        allItems += Item(
            id = BEAST_HIDE_VEST_ID,
            name = "Beast Hide Vest",
            desc = "A vest sewn from the hides of forest predators. Tough as steel but light as leather.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "chest",
            statBonuses = StatBonuses(defense = 5, maxHp = 10),
            value = 140
        )
        allItems += Item(
            id = BOOTS_OF_THE_FOREST_ID,
            name = "Boots of the Forest",
            desc = "Soft-soled boots that leave no tracks. The wearer moves silently through underbrush.",
            featureIds = emptyList(),
            equipmentType = "armor",
            equipmentSlot = "feet",
            statBonuses = StatBonuses(defense = 2, maxHp = 5),
            value = 95
        )

        // ========== SPECIAL ACCESSORIES ==========
        allItems += Item(
            id = AMULET_OF_NATURE_ID,
            name = "Amulet of Nature",
            desc = "An amber amulet containing a preserved dragonfly. Channels the vitality of the forest.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "amulet",
            statBonuses = StatBonuses(defense = 1, maxHp = 25),
            value = 120
        )
        allItems += Item(
            id = RING_OF_THE_WILD_ID,
            name = "Ring of the Wild",
            desc = "A wooden ring carved from a single piece of heartwood. Strengthens the bond between wearer and nature.",
            featureIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "finger",
            statBonuses = StatBonuses(attack = 1, defense = 1, maxHp = 10),
            value = 110
        )

        // Create items in database
        allItems.forEach { item ->
            if (ItemRepository.findById(item.id) == null) {
                ItemRepository.create(item)
            }
        }
    }

    private fun seedLocation() {
        val shopItems = listOf(
            MOONPETAL_SALVE_ID, IRONBARK_TONIC_ID, WOLFSBANE_POISON_ID,
            ELVEN_CLOAK_ID, HUNTERS_CHARM_ID,
            WILDWOOD_BOW_ID, THORNBLADE_ID, DRUIDS_STAFF_ID,
            BEAST_HIDE_VEST_ID, BOOTS_OF_THE_FOREST_ID,
            AMULET_OF_NATURE_ID, RING_OF_THE_WILD_ID
        )

        val hermitsHollow = Location(
            id = HERMITS_HOLLOW_ID,
            name = "Hermit's Hollow",
            desc = "Tucked away beneath the roots of an enormous ancient oak, a small cave opening leads to a cozy hollow. Inside, an old hermit tends to a collection of rare herbs, strange weapons, and enchanted trinkets. Mushrooms provide a soft glow. 'Ah, a traveler! Come, come... I have wares you won't find in any town.'",
            itemIds = shopItems,
            creatureIds = emptyList(),
            exits = emptyList(),  // Will be connected via rift portal or manually
            featureIds = emptyList(),
            gridX = 0,
            gridY = 0,
            areaId = "hermits-hollow",
            locationType = LocationType.UNDERGROUND
        )

        if (LocationRepository.findById(hermitsHollow.id) == null) {
            LocationRepository.create(hermitsHollow)
        }
    }
}
