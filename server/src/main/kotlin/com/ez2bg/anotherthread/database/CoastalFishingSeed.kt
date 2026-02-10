package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seeds coastal/saltwater fish items, loot tables, and coastal fishing spots.
 * Based on Pacific Northwest coastal species with fantasy elements.
 */
object CoastalFishingSeed {
    private val log = LoggerFactory.getLogger("CoastalFishingSeed")

    // Feature ID for coastal fishing spots
    const val COASTAL_FISHING_FEATURE = "feature-coastal-fishing"

    // Loot table IDs for coastal fishing
    const val COASTAL_NEAR_LOOT_TABLE = "loot-table-coastal-near"
    const val COASTAL_MID_LOOT_TABLE = "loot-table-coastal-mid"
    const val COASTAL_FAR_LOOT_TABLE = "loot-table-coastal-far"

    fun seedIfEmpty() {
        seedCoastalFish()
        seedCoastalLootTables()
        seedCoastalFeature()
        addCoastalFishingSpotsToWestIsland()
    }

    private fun seedCoastalFish() {
        val coastalFish = listOf(
            // Small coastal fish (weight 1, value 8-20) - Common shore catches
            Item(
                id = "fish-herring",
                name = "Herring",
                desc = "A silvery schooling fish that shimmers in the sunlight. Common but valued for bait.",
                featureIds = emptyList(),
                value = 8,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-smelt",
                name = "Smelt",
                desc = "A small, oily fish prized for its delicate flavor when fried whole.",
                featureIds = emptyList(),
                value = 10,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-anchovy",
                name = "Anchovy",
                desc = "A tiny but flavorful fish that adds punch to any dish.",
                featureIds = emptyList(),
                value = 6,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-sand-dab",
                name = "Sand Dab",
                desc = "A small flatfish that hides in the sandy shallows.",
                featureIds = emptyList(),
                value = 12,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-surf-perch",
                name = "Surf Perch",
                desc = "A colorful perch caught in the breaking waves.",
                featureIds = emptyList(),
                value = 15,
                weight = 1,
                isStackable = true
            ),

            // Medium coastal fish (weight 2-3, value 35-80) - Good sport fish
            Item(
                id = "fish-rockfish",
                name = "Rockfish",
                desc = "A spiny-finned fish with vibrant red and orange coloring. Lives among the rocky reefs.",
                featureIds = emptyList(),
                value = 45,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-cabezon",
                name = "Cabezon",
                desc = "An ugly but tasty bottom-dweller with mottled skin and a massive head.",
                featureIds = emptyList(),
                value = 55,
                weight = 3,
                isStackable = true
            ),
            Item(
                id = "fish-sea-bass",
                name = "Sea Bass",
                desc = "A prized table fish with firm, white flesh. A favorite of coastal anglers.",
                featureIds = emptyList(),
                value = 60,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-pacific-cod",
                name = "Pacific Cod",
                desc = "A cold-water fish with flaky white meat, perfect for fish and chips.",
                featureIds = emptyList(),
                value = 50,
                weight = 3,
                isStackable = true
            ),
            Item(
                id = "fish-kelp-greenling",
                name = "Kelp Greenling",
                desc = "A colorful fish that lurks in the kelp forests, with distinctive spotted patterns.",
                featureIds = emptyList(),
                value = 40,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-flounder",
                name = "Flounder",
                desc = "A flat-bodied fish with both eyes on one side. Camouflages perfectly on the ocean floor.",
                featureIds = emptyList(),
                value = 35,
                weight = 2,
                isStackable = true
            ),

            // Large coastal fish (weight 4-5, value 100-200) - Trophy catches
            Item(
                id = "fish-lingcod",
                name = "Lingcod",
                desc = "A fearsome predator with needle-sharp teeth and blue-green flesh. Highly prized by anglers.",
                featureIds = emptyList(),
                value = 120,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-chinook-salmon",
                name = "Chinook Salmon",
                desc = "The king of salmon! A powerful fighter with rich, fatty flesh.",
                featureIds = emptyList(),
                value = 150,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-coho-salmon",
                name = "Coho Salmon",
                desc = "A silver-sided salmon known for its acrobatic leaps when hooked.",
                featureIds = emptyList(),
                value = 100,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-yelloweye-rockfish",
                name = "Yelloweye Rockfish",
                desc = "A deep-dwelling rockfish with brilliant golden eyes. Can live over 100 years!",
                featureIds = emptyList(),
                value = 180,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-black-sea-bass",
                name = "Black Sea Bass",
                desc = "A large, dark-colored bass with a firm texture. Commands top prices at market.",
                featureIds = emptyList(),
                value = 140,
                weight = 4,
                isStackable = true
            ),

            // Trophy/Legendary coastal fish (weight 6-8, value 300-800) - Rare catches
            Item(
                id = "fish-pacific-halibut",
                name = "Pacific Halibut",
                desc = "A massive flatfish that can weigh hundreds of pounds. The barn door of the sea!",
                featureIds = emptyList(),
                value = 350,
                weight = 8,
                isStackable = true
            ),
            Item(
                id = "fish-giant-sea-bass",
                name = "Giant Sea Bass",
                desc = "An enormous bass that can grow larger than a person. A true monster of the deep.",
                featureIds = emptyList(),
                value = 400,
                weight = 7,
                isStackable = true
            ),
            Item(
                id = "fish-white-sturgeon",
                name = "White Sturgeon",
                desc = "An ancient fish that can live for centuries. Its roe is the most prized caviar.",
                featureIds = emptyList(),
                value = 500,
                weight = 8,
                isStackable = true
            ),
            Item(
                id = "fish-golden-lingcod",
                name = "Golden Lingcod",
                desc = "An extremely rare color variant with shimmering golden scales. Fishermen tell legends of this one.",
                featureIds = emptyList(),
                value = 600,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-ghost-halibut",
                name = "Ghost Halibut",
                desc = "A pale, almost translucent halibut said to swim between worlds. Catching one brings luck for a year.",
                featureIds = emptyList(),
                value = 800,
                weight = 6,
                isStackable = true
            )
        )

        var created = 0
        coastalFish.forEach { fish ->
            if (ItemRepository.findById(fish.id) == null) {
                ItemRepository.create(fish)
                created++
            }
        }
        if (created > 0) {
            log.info("Created $created coastal fish items")
        }
    }

    private fun seedCoastalLootTables() {
        val lootTables = listOf(
            // Near shore - small fish, some medium
            LootTableData(
                id = COASTAL_NEAR_LOOT_TABLE,
                name = "Coastal Shore Fish",
                entries = listOf(
                    LootEntry("fish-herring", chance = 0.25f),
                    LootEntry("fish-smelt", chance = 0.20f),
                    LootEntry("fish-anchovy", chance = 0.20f),
                    LootEntry("fish-sand-dab", chance = 0.15f),
                    LootEntry("fish-surf-perch", chance = 0.12f),
                    LootEntry("fish-flounder", chance = 0.05f),
                    LootEntry("fish-kelp-greenling", chance = 0.03f)
                )
            ),
            // Mid water - medium fish, some large
            LootTableData(
                id = COASTAL_MID_LOOT_TABLE,
                name = "Coastal Mid-Water Fish",
                entries = listOf(
                    LootEntry("fish-rockfish", chance = 0.20f),
                    LootEntry("fish-sea-bass", chance = 0.18f),
                    LootEntry("fish-cabezon", chance = 0.15f),
                    LootEntry("fish-pacific-cod", chance = 0.15f),
                    LootEntry("fish-kelp-greenling", chance = 0.10f),
                    LootEntry("fish-coho-salmon", chance = 0.08f),
                    LootEntry("fish-lingcod", chance = 0.06f),
                    LootEntry("fish-chinook-salmon", chance = 0.05f),
                    LootEntry("fish-yelloweye-rockfish", chance = 0.03f)
                )
            ),
            // Deep water - large fish, rare trophy fish
            LootTableData(
                id = COASTAL_FAR_LOOT_TABLE,
                name = "Coastal Deep Water Fish",
                entries = listOf(
                    LootEntry("fish-lingcod", chance = 0.18f),
                    LootEntry("fish-chinook-salmon", chance = 0.15f),
                    LootEntry("fish-yelloweye-rockfish", chance = 0.12f),
                    LootEntry("fish-black-sea-bass", chance = 0.12f),
                    LootEntry("fish-coho-salmon", chance = 0.10f),
                    LootEntry("fish-pacific-halibut", chance = 0.10f),
                    LootEntry("fish-giant-sea-bass", chance = 0.08f),
                    LootEntry("fish-white-sturgeon", chance = 0.07f),
                    LootEntry("fish-golden-lingcod", chance = 0.05f),
                    LootEntry("fish-ghost-halibut", chance = 0.03f)
                )
            )
        )

        var created = 0
        lootTables.forEach { table ->
            if (LootTableRepository.findById(table.id) == null) {
                LootTableRepository.create(table)
                created++
            }
        }
        if (created > 0) {
            log.info("Created $created coastal fishing loot tables")
        }
    }

    private fun seedCoastalFeature() {
        if (FeatureRepository.findById(COASTAL_FISHING_FEATURE) == null) {
            val coastalFeature = Feature(
                id = COASTAL_FISHING_FEATURE,
                name = "Coastal Fishing Spot",
                featureCategoryId = null,
                description = "A rocky coastline with excellent fishing. The ocean teems with saltwater fish of all sizes.",
                data = """{"waterType": "saltwater", "deepWaterAvailable": true}"""
            )
            FeatureRepository.create(coastalFeature)
            log.info("Created coastal fishing spot feature")
        }
    }

    // Words that indicate a coastal location
    private val coastalNamePatterns = listOf(
        "coast", "beach", "shore", "cove", "bay", "harbor", "harbour",
        "pier", "dock", "wharf", "ocean", "cliff", "bluff", "tide",
        "surf", "reef", "cape", "inlet", "lagoon", "port"
    )

    private fun isCoastalByName(name: String): Boolean {
        val lowerName = name.lowercase()
        return coastalNamePatterns.any { pattern -> lowerName.contains(pattern) }
    }

    private fun addCoastalFishingSpotsToWestIsland() {
        // Find all coastal locations by name pattern or isCoast flag
        val allLocations = LocationRepository.findAll()

        var added = 0
        for (location in allLocations) {
            // Check if this is a coastal location by flag OR name pattern
            val isCoastal = location.isCoast == true || isCoastalByName(location.name)

            // Add to all coastal locations that don't already have the feature
            if (isCoastal && !location.featureIds.contains(COASTAL_FISHING_FEATURE)) {
                val updatedLocation = location.copy(
                    featureIds = location.featureIds + COASTAL_FISHING_FEATURE
                )
                LocationRepository.update(updatedLocation)
                added++
                log.debug("Added coastal fishing to ${location.name} at (${location.gridX}, ${location.gridY})")
            }
        }

        if (added > 0) {
            log.info("Added coastal fishing feature to $added coastal locations")
        }
    }
}
