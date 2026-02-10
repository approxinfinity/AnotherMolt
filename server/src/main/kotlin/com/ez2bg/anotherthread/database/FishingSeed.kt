package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seeds fish items, fishing loot tables, and fishing spot feature.
 */
object FishingSeed {
    private val log = LoggerFactory.getLogger("FishingSeed")

    /**
     * Seed fishing content if not already present.
     */
    fun seedIfEmpty() {
        seedFishItems()
        seedFishingLootTables()
        seedFishingFeature()
        seedFishingRod()
        seedFishingBadge()
    }

    private fun seedFishItems() {
        val fishItems = listOf(
            // Small fish (weight 1, value 5-15)
            Item(
                id = "fish-minnow",
                name = "Minnow",
                desc = "A tiny silver fish, barely a mouthful.",
                featureIds = emptyList(),
                value = 5,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-perch",
                name = "Perch",
                desc = "A small yellow perch with dark stripes.",
                featureIds = emptyList(),
                value = 8,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-bluegill",
                name = "Bluegill",
                desc = "A small bluegill sunfish with a colorful belly.",
                featureIds = emptyList(),
                value = 10,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-sardine",
                name = "Sardine",
                desc = "A small oily fish, good for bait or a snack.",
                featureIds = emptyList(),
                value = 6,
                weight = 1,
                isStackable = true
            ),

            // Medium fish (weight 2, value 20-40)
            Item(
                id = "fish-trout",
                name = "Trout",
                desc = "A fresh rainbow trout, its scales glinting with color.",
                featureIds = emptyList(),
                value = 25,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-bass",
                name = "Bass",
                desc = "A plump largemouth bass, a fisherman's prize.",
                featureIds = emptyList(),
                value = 30,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-catfish",
                name = "Catfish",
                desc = "A whiskered catfish, slippery and strong.",
                featureIds = emptyList(),
                value = 35,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-pike",
                name = "Pike",
                desc = "A northern pike with sharp teeth and a mean look.",
                featureIds = emptyList(),
                value = 40,
                weight = 2,
                isStackable = true
            ),

            // Large fish (weight 4, value 60-100)
            Item(
                id = "fish-salmon",
                name = "Salmon",
                desc = "A magnificent salmon, pink-fleshed and delicious.",
                featureIds = emptyList(),
                value = 60,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-carp",
                name = "Carp",
                desc = "A heavy golden carp, prized in eastern cuisine.",
                featureIds = emptyList(),
                value = 70,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-walleye",
                name = "Walleye",
                desc = "A walleye with distinctive glassy eyes.",
                featureIds = emptyList(),
                value = 80,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-sturgeon",
                name = "Sturgeon",
                desc = "An ancient-looking sturgeon, armored with bony plates.",
                featureIds = emptyList(),
                value = 100,
                weight = 4,
                isStackable = true
            ),

            // Trophy fish (weight 6, value 150-300)
            Item(
                id = "fish-giant-catfish",
                name = "Giant Catfish",
                desc = "An enormous catfish, the stuff of legends. Its whiskers are as long as your arm.",
                featureIds = emptyList(),
                value = 150,
                weight = 6,
                isStackable = true
            ),
            Item(
                id = "fish-golden-trout",
                name = "Golden Trout",
                desc = "A rare golden trout that shimmers with an almost magical light.",
                featureIds = emptyList(),
                value = 250,
                weight = 6,
                isStackable = true
            ),
            Item(
                id = "fish-legendary-bass",
                name = "Legendary Bass",
                desc = "A massive bass of legendary proportions. Fishermen speak of this one in hushed tones.",
                featureIds = emptyList(),
                value = 200,
                weight = 6,
                isStackable = true
            ),
            Item(
                id = "fish-ancient-sturgeon",
                name = "Ancient Sturgeon",
                desc = "A prehistoric-looking sturgeon that may have lived for centuries.",
                featureIds = emptyList(),
                value = 300,
                weight = 6,
                isStackable = true
            )
        )

        var created = 0
        fishItems.forEach { fish ->
            if (ItemRepository.findById(fish.id) == null) {
                ItemRepository.create(fish)
                created++
            }
        }
        if (created > 0) {
            log.info("Created $created fish items")
        }
    }

    private fun seedFishingLootTables() {
        val lootTables = listOf(
            // Near shore - small fish, some medium
            LootTableData(
                id = "loot-table-fishing-near",
                name = "Near Shore Fish",
                entries = listOf(
                    LootEntry("fish-minnow", chance = 0.30f),
                    LootEntry("fish-perch", chance = 0.25f),
                    LootEntry("fish-bluegill", chance = 0.25f),
                    LootEntry("fish-sardine", chance = 0.15f),
                    LootEntry("fish-trout", chance = 0.05f)
                )
            ),
            // Mid water - medium fish, some large
            LootTableData(
                id = "loot-table-fishing-mid",
                name = "Mid Water Fish",
                entries = listOf(
                    LootEntry("fish-trout", chance = 0.25f),
                    LootEntry("fish-bass", chance = 0.25f),
                    LootEntry("fish-catfish", chance = 0.20f),
                    LootEntry("fish-pike", chance = 0.15f),
                    LootEntry("fish-salmon", chance = 0.10f),
                    LootEntry("fish-walleye", chance = 0.05f)
                )
            ),
            // Deep water - large fish, rare trophy fish
            LootTableData(
                id = "loot-table-fishing-far",
                name = "Deep Water Fish",
                entries = listOf(
                    LootEntry("fish-salmon", chance = 0.25f),
                    LootEntry("fish-carp", chance = 0.20f),
                    LootEntry("fish-walleye", chance = 0.20f),
                    LootEntry("fish-sturgeon", chance = 0.15f),
                    LootEntry("fish-giant-catfish", chance = 0.10f),
                    LootEntry("fish-golden-trout", chance = 0.05f),
                    LootEntry("fish-legendary-bass", chance = 0.03f),
                    LootEntry("fish-ancient-sturgeon", chance = 0.02f)
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
            log.info("Created $created fishing loot tables")
        }
    }

    private fun seedFishingFeature() {
        val fishingFeature = Feature(
            id = "feature-fishing-spot",
            name = "Fishing Spot",
            featureCategoryId = null,
            description = "A body of water suitable for fishing. Cast your line and see what you catch!",
            data = """{"deepWaterAvailable": true}"""
        )

        if (FeatureRepository.findById(fishingFeature.id) == null) {
            FeatureRepository.create(fishingFeature)
            log.info("Created fishing spot feature")
        }

        // Add fishing spot to Lake Rainier
        addFishingSpotToLakeRainier()
    }

    private fun addFishingSpotToLakeRainier() {
        val lakeRainierId = "54521394-6a46-4747-9782-3a05848d2166"
        val location = LocationRepository.findById(lakeRainierId) ?: return

        if (!location.featureIds.contains(FISHING_FEATURE_ID)) {
            val updatedLocation = location.copy(featureIds = location.featureIds + FISHING_FEATURE_ID)
            LocationRepository.update(updatedLocation)
            log.info("Added fishing spot feature to Lake Rainier")
        }
    }

    // Fishing rod item ID
    const val FISHING_ROD_ID = "item-fishing-rod"

    // Fishing badge ability ID
    const val FISHING_BADGE_ID = "ability-fishing-badge"

    // Feature ID
    private const val FISHING_FEATURE_ID = "feature-fishing-spot"

    /**
     * Seed the fishing rod item (sold at shops).
     */
    private fun seedFishingRod() {
        if (ItemRepository.findById(FISHING_ROD_ID) == null) {
            val fishingRod = Item(
                id = FISHING_ROD_ID,
                name = "Fishing Rod",
                desc = "A sturdy fishing rod with a reliable reel. Improves your chances of catching fish by 20%.",
                featureIds = emptyList(),
                value = 50,
                weight = 2,
                isStackable = false
            )
            ItemRepository.create(fishingRod)
            log.info("Created fishing rod item")
        }

        // Add fishing rod to the weapons shop
        addFishingRodToShop()
    }

    private fun addFishingRodToShop() {
        val weaponsShopId = "tun-du-lac-weapons-shop"
        val shop = LocationRepository.findById(weaponsShopId) ?: return

        if (!shop.itemIds.contains(FISHING_ROD_ID)) {
            val updatedShop = shop.copy(itemIds = shop.itemIds + FISHING_ROD_ID)
            LocationRepository.update(updatedShop)
            log.info("Added fishing rod to weapons shop")
        }
    }

    /**
     * Seed the fishing badge passive ability.
     */
    private fun seedFishingBadge() {
        if (AbilityRepository.findById(FISHING_BADGE_ID) != null) return

        val fishingBadge = Ability(
            id = FISHING_BADGE_ID,
            name = "Angler's Badge",
            description = "Awarded for catching 10 fish. Permanently improves fishing success by 15%.",
            classId = null,  // Universal ability
            abilityType = "passive",
            targetType = "self",
            range = 0,
            cooldownType = "none",
            cooldownRounds = 0,
            effects = """[{"type": "fishing_bonus", "value": 15}]""",
            manaCost = 0,
            staminaCost = 0,
            minLevel = 1
        )
        AbilityRepository.create(fishingBadge)
        log.info("Created fishing badge ability")
    }
}
