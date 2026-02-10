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
            // ========== EASY FISH (difficulty 1-2, value 5-15) ==========
            // These barely fight - great for beginners
            Item(
                id = "fish-anchovy",
                name = "Anchovy",
                desc = "A tiny but flavorful fish that adds punch to any dish. Practically jumps into the net.",
                featureIds = emptyList(),
                value = 5,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-herring",
                name = "Herring",
                desc = "A silvery schooling fish that shimmers in the sunlight. Common and easy to catch.",
                featureIds = emptyList(),
                value = 8,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-smelt",
                name = "Smelt",
                desc = "A small, oily fish prized for its delicate flavor. Gives up without much fight.",
                featureIds = emptyList(),
                value = 10,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-sand-dab",
                name = "Sand Dab",
                desc = "A small flatfish that hides in sandy shallows. Too small to put up a struggle.",
                featureIds = emptyList(),
                value = 12,
                weight = 1,
                isStackable = true
            ),

            // ========== MODERATE FISH (difficulty 3-4, value 20-45) ==========
            // These put up a decent fight
            Item(
                id = "fish-surf-perch",
                name = "Surf Perch",
                desc = "A colorful perch caught in breaking waves. Feisty but manageable.",
                featureIds = emptyList(),
                value = 22,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-flounder",
                name = "Flounder",
                desc = "A flat-bodied fish with both eyes on one side. Uses its flat body to resist.",
                featureIds = emptyList(),
                value = 28,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-kelp-greenling",
                name = "Kelp Greenling",
                desc = "A colorful fish that lurks in kelp forests. Darts and dives unpredictably.",
                featureIds = emptyList(),
                value = 35,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-pacific-cod",
                name = "Pacific Cod",
                desc = "A cold-water fish with flaky white meat. Fights with steady determination.",
                featureIds = emptyList(),
                value = 42,
                weight = 3,
                isStackable = true
            ),

            // ========== CHALLENGING FISH (difficulty 5-6, value 50-100) ==========
            // Real sport fish - require skill
            Item(
                id = "fish-rockfish",
                name = "Rockfish",
                desc = "A spiny-finned predator. Dives for cover among rocks when hooked.",
                featureIds = emptyList(),
                value = 55,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-cabezon",
                name = "Cabezon",
                desc = "An ugly but powerful bottom-dweller. Its massive head hides surprising strength.",
                featureIds = emptyList(),
                value = 70,
                weight = 3,
                isStackable = true
            ),
            Item(
                id = "fish-sea-bass",
                name = "Sea Bass",
                desc = "A prized table fish with firm flesh. Known for sudden bursts of power.",
                featureIds = emptyList(),
                value = 85,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-coho-salmon",
                name = "Coho Salmon",
                desc = "The silver salmon! Famous for acrobatic leaps and explosive runs.",
                featureIds = emptyList(),
                value = 95,
                weight = 4,
                isStackable = true
            ),

            // ========== HARD FISH (difficulty 7-8, value 120-220) ==========
            // Serious anglers only - these fish FIGHT
            Item(
                id = "fish-lingcod",
                name = "Lingcod",
                desc = "A fearsome predator with needle teeth. Fights dirty and never gives up.",
                featureIds = emptyList(),
                value = 130,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-chinook-salmon",
                name = "Chinook Salmon",
                desc = "The KING of salmon! Legendary fighters that test even expert anglers.",
                featureIds = emptyList(),
                value = 180,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-yelloweye-rockfish",
                name = "Yelloweye Rockfish",
                desc = "A deep-dwelling ancient with golden eyes. Powerful and unpredictable.",
                featureIds = emptyList(),
                value = 200,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-black-sea-bass",
                name = "Black Sea Bass",
                desc = "A massive dark-colored bass. Bulldogs straight for structure when hooked.",
                featureIds = emptyList(),
                value = 160,
                weight = 4,
                isStackable = true
            ),

            // ========== VERY HARD / RARE (difficulty 9, value 300-500) ==========
            // Trophy fish - only the best land these
            Item(
                id = "fish-pacific-halibut",
                name = "Pacific Halibut",
                desc = "A barn door of the sea! Can drag boats and break heavy tackle. Legendary strength.",
                featureIds = emptyList(),
                value = 350,
                weight = 8,
                isStackable = true
            ),
            Item(
                id = "fish-giant-sea-bass",
                name = "Giant Sea Bass",
                desc = "An enormous bass larger than a person. Sheer mass makes it nearly impossible to land.",
                featureIds = emptyList(),
                value = 420,
                weight = 7,
                isStackable = true
            ),
            Item(
                id = "fish-white-sturgeon",
                name = "White Sturgeon",
                desc = "An ancient dinosaur fish. Can fight for HOURS. Masters spend lifetimes chasing these.",
                featureIds = emptyList(),
                value = 500,
                weight = 8,
                isStackable = true
            ),
            Item(
                id = "fish-bluefin-tuna",
                name = "Bluefin Tuna",
                desc = "The ultimate ocean athlete. Can swim 40mph and fight for hours without tiring.",
                featureIds = emptyList(),
                value = 480,
                weight = 6,
                isStackable = true
            ),

            // ========== ULTRA RARE / LEGENDARY (difficulty 10, value 600-2000) ==========
            // Mythical fish - once in a lifetime catches
            Item(
                id = "fish-golden-lingcod",
                name = "Golden Lingcod",
                desc = "An impossibly rare golden variant. Fishermen whisper of its existence. Fights like a demon.",
                featureIds = emptyList(),
                value = 650,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-ghost-halibut",
                name = "Ghost Halibut",
                desc = "A spectral, translucent halibut said to swim between worlds. Its very existence is debated.",
                featureIds = emptyList(),
                value = 800,
                weight = 6,
                isStackable = true
            ),
            Item(
                id = "fish-crimson-king-salmon",
                name = "Crimson King Salmon",
                desc = "A blood-red mutation of the Chinook. Said to appear once per decade. Fights with supernatural fury.",
                featureIds = emptyList(),
                value = 950,
                weight = 6,
                isStackable = true
            ),
            Item(
                id = "fish-abyssal-anglerfish",
                name = "Abyssal Anglerfish",
                desc = "A nightmare from the deep trenches. Its glowing lure hypnotizes prey - and fishermen. Rarely surfaces.",
                featureIds = emptyList(),
                value = 1100,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-oarfish",
                name = "Giant Oarfish",
                desc = "The serpent of the sea! Can reach 30 feet long. Sightings were once mistaken for sea monsters.",
                featureIds = emptyList(),
                value = 1250,
                weight = 9,
                isStackable = true
            ),
            Item(
                id = "fish-coelacanth",
                name = "Coelacanth",
                desc = "A living fossil thought extinct for 65 million years. Catching one rewrites history.",
                featureIds = emptyList(),
                value = 1500,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-moonfish",
                name = "Moonfish",
                desc = "A massive silvery disc that glows faintly in moonlight. Said to grant wishes to those who release it.",
                featureIds = emptyList(),
                value = 1800,
                weight = 7,
                isStackable = true
            ),
            Item(
                id = "fish-leviathan-eel",
                name = "Leviathan Eel",
                desc = "An ancient serpentine creature of impossible size. Legends say it can capsize ships. Why are you fishing for this?!",
                featureIds = emptyList(),
                value = 2000,
                weight = 10,
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
            // Near shore - mostly easy fish, small chance of moderate
            LootTableData(
                id = COASTAL_NEAR_LOOT_TABLE,
                name = "Coastal Shore Fish",
                entries = listOf(
                    // Easy (85%)
                    LootEntry("fish-anchovy", chance = 0.25f),
                    LootEntry("fish-herring", chance = 0.25f),
                    LootEntry("fish-smelt", chance = 0.20f),
                    LootEntry("fish-sand-dab", chance = 0.15f),
                    // Moderate (15%)
                    LootEntry("fish-surf-perch", chance = 0.08f),
                    LootEntry("fish-flounder", chance = 0.05f),
                    LootEntry("fish-kelp-greenling", chance = 0.02f)
                )
            ),
            // Mid water - moderate to challenging, small chance at hard fish
            LootTableData(
                id = COASTAL_MID_LOOT_TABLE,
                name = "Coastal Mid-Water Fish",
                entries = listOf(
                    // Moderate (40%)
                    LootEntry("fish-pacific-cod", chance = 0.15f),
                    LootEntry("fish-kelp-greenling", chance = 0.13f),
                    LootEntry("fish-flounder", chance = 0.12f),
                    // Challenging (45%)
                    LootEntry("fish-rockfish", chance = 0.15f),
                    LootEntry("fish-cabezon", chance = 0.12f),
                    LootEntry("fish-sea-bass", chance = 0.10f),
                    LootEntry("fish-coho-salmon", chance = 0.08f),
                    // Hard (14%)
                    LootEntry("fish-lingcod", chance = 0.06f),
                    LootEntry("fish-chinook-salmon", chance = 0.04f),
                    LootEntry("fish-yelloweye-rockfish", chance = 0.03f),
                    LootEntry("fish-black-sea-bass", chance = 0.02f)
                )
            ),
            // Deep water - challenging to legendary, this is where the monsters live
            LootTableData(
                id = COASTAL_FAR_LOOT_TABLE,
                name = "Coastal Deep Water Fish",
                entries = listOf(
                    // Challenging (20%)
                    LootEntry("fish-coho-salmon", chance = 0.10f),
                    LootEntry("fish-sea-bass", chance = 0.10f),
                    // Hard (35%)
                    LootEntry("fish-lingcod", chance = 0.12f),
                    LootEntry("fish-chinook-salmon", chance = 0.10f),
                    LootEntry("fish-yelloweye-rockfish", chance = 0.07f),
                    LootEntry("fish-black-sea-bass", chance = 0.06f),
                    // Very Hard / Rare (30%)
                    LootEntry("fish-pacific-halibut", chance = 0.10f),
                    LootEntry("fish-giant-sea-bass", chance = 0.07f),
                    LootEntry("fish-white-sturgeon", chance = 0.06f),
                    LootEntry("fish-bluefin-tuna", chance = 0.07f),
                    // Ultra Rare / Legendary (15% combined, individually rare)
                    LootEntry("fish-golden-lingcod", chance = 0.04f),
                    LootEntry("fish-ghost-halibut", chance = 0.03f),
                    LootEntry("fish-crimson-king-salmon", chance = 0.025f),
                    LootEntry("fish-abyssal-anglerfish", chance = 0.02f),
                    LootEntry("fish-oarfish", chance = 0.015f),
                    LootEntry("fish-coelacanth", chance = 0.01f),
                    LootEntry("fish-moonfish", chance = 0.007f),
                    LootEntry("fish-leviathan-eel", chance = 0.003f)
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
