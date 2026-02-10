package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seeds the general store location and items.
 * The general store sells consumables, food, and basic supplies.
 * Players can also sell food items (including fish) here.
 */
object GeneralStoreSeed {
    private val log = LoggerFactory.getLogger("GeneralStoreSeed")

    // Location ID
    const val GENERAL_STORE_ID = "tun-du-lac-general-store"

    // Item IDs
    const val SALT_ID = "item-salt"
    const val RATIONS_ID = "item-rations"
    const val WATERSKIN_ID = "item-waterskin"
    const val TORCH_ID = "item-torch"
    const val ROPE_ID = "item-rope"
    const val BEDROLL_ID = "item-bedroll"

    // Feature for cooking fire
    const val COOKING_FIRE_FEATURE = "feature-cooking-fire"
    const val HEARTH_FEATURE = "feature-hearth"

    fun seedIfEmpty() {
        // Check if already seeded
        if (LocationRepository.findById(GENERAL_STORE_ID) != null) {
            // Check if hearth feature exists in the inn
            addHearthToInn()
            return
        }

        log.info("Seeding General Store...")
        seedItems()
        seedCookingFeatures()
        seedLocation()
        addExitToTownSquare()
        addHearthToInn()
        log.info("Seeded General Store")
    }

    private fun seedItems() {
        val items = listOf(
            Item(
                id = SALT_ID,
                name = "Salt",
                desc = "A small pouch of salt. Essential for preserving food. Salted food stays fresh for up to 3 months.",
                featureIds = emptyList(),
                value = 5,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = RATIONS_ID,
                name = "Rations",
                desc = "A day's worth of dried trail rations. Not the most appetizing, but filling.",
                featureIds = emptyList(),
                value = 10,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = WATERSKIN_ID,
                name = "Waterskin",
                desc = "A leather waterskin that can hold water for a day's travel.",
                featureIds = emptyList(),
                value = 8,
                weight = 1,
                isStackable = false
            ),
            Item(
                id = TORCH_ID,
                name = "Torch",
                desc = "A wooden torch dipped in pitch. Provides light in dark places.",
                featureIds = emptyList(),
                value = 3,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = ROPE_ID,
                name = "Rope (50 ft)",
                desc = "Fifty feet of sturdy hemp rope. Useful for climbing or binding.",
                featureIds = emptyList(),
                value = 15,
                weight = 2,
                isStackable = false
            ),
            Item(
                id = BEDROLL_ID,
                name = "Bedroll",
                desc = "A rolled-up sleeping pad and blanket. Makes camping more comfortable.",
                featureIds = emptyList(),
                value = 12,
                weight = 3,
                isStackable = false
            )
        )

        var created = 0
        items.forEach { item ->
            if (ItemRepository.findById(item.id) == null) {
                ItemRepository.create(item)
                created++
            }
        }
        if (created > 0) {
            log.info("Created $created general store items")
        }
    }

    private fun seedCookingFeatures() {
        // Create cooking fire feature
        if (FeatureRepository.findById(COOKING_FIRE_FEATURE) == null) {
            val cookingFire = Feature(
                id = COOKING_FIRE_FEATURE,
                name = "Cooking Fire",
                featureCategoryId = null,
                description = "A cooking fire suitable for preparing food. You can cook raw fish and other foods here.",
                data = """{"canCook": true}"""
            )
            FeatureRepository.create(cookingFire)
            log.info("Created cooking fire feature")
        }

        // Create hearth feature
        if (FeatureRepository.findById(HEARTH_FEATURE) == null) {
            val hearth = Feature(
                id = HEARTH_FEATURE,
                name = "Hearth",
                featureCategoryId = null,
                description = "A warm hearth with an open fire. You can cook food here.",
                data = """{"canCook": true}"""
            )
            FeatureRepository.create(hearth)
            log.info("Created hearth feature")
        }
    }

    private fun seedLocation() {
        val generalStore = Location(
            id = GENERAL_STORE_ID,
            name = "Tun du Lac General Store",
            desc = "A well-stocked general store with barrels, crates, and shelves lining the walls. The smell of dried herbs and smoked meats fills the air. A friendly merchant stands behind a wooden counter, ready to buy and sell various goods.",
            locationType = LocationType.INDOOR,
            areaId = "tun-du-lac",
            featureIds = listOf(COOKING_FIRE_FEATURE),  // Has a small cooking fire for customers
            creatureIds = emptyList(),
            itemIds = listOf(SALT_ID, RATIONS_ID, WATERSKIN_ID, TORCH_ID, ROPE_ID, BEDROLL_ID),
            gridX = 51,  // Position near town square
            gridY = 50,
            exits = listOf(
                Exit(TunDuLacSeed.TOWN_SQUARE_ID, ExitDirection.SOUTH)
            )
        )

        LocationRepository.create(generalStore)
        log.info("Created General Store location")
    }

    private fun addExitToTownSquare() {
        val townSquare = LocationRepository.findById(TunDuLacSeed.TOWN_SQUARE_ID) ?: return

        // Check if exit already exists
        if (townSquare.exits.any { it.locationId == GENERAL_STORE_ID }) return

        // Add exit to general store
        val updatedTownSquare = townSquare.copy(
            exits = townSquare.exits + Exit(GENERAL_STORE_ID, ExitDirection.NORTH)
        )
        LocationRepository.update(updatedTownSquare)
        log.info("Added exit from Town Square to General Store")
    }

    private fun addHearthToInn() {
        val inn = LocationRepository.findById(TunDuLacSeed.INN_ID) ?: return

        // Check if hearth already exists
        if (inn.featureIds.contains(HEARTH_FEATURE)) return

        // Add hearth feature to inn
        val updatedInn = inn.copy(
            featureIds = inn.featureIds + HEARTH_FEATURE
        )
        LocationRepository.update(updatedInn)
        log.info("Added hearth feature to Tun du Lac Inn")
    }
}
