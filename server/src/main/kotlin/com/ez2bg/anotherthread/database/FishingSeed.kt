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
            // ========== EASY FISH (difficulty 1-2, value 5-15) ==========
            // These barely fight - great for beginners
            Item(
                id = "fish-minnow",
                name = "Minnow",
                desc = "A tiny silver fish, barely a mouthful. Practically jumps onto the hook.",
                featureIds = emptyList(),
                value = 5,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-bluegill",
                name = "Bluegill",
                desc = "A small bluegill sunfish with a colorful belly. Easy to catch, fun for kids.",
                featureIds = emptyList(),
                value = 8,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-perch",
                name = "Perch",
                desc = "A small yellow perch with dark stripes. Puts up a token struggle.",
                featureIds = emptyList(),
                value = 10,
                weight = 1,
                isStackable = true
            ),
            Item(
                id = "fish-sardine",
                name = "Sardine",
                desc = "A small oily fish, good for bait or a snack. Schools are easy pickings.",
                featureIds = emptyList(),
                value = 6,
                weight = 1,
                isStackable = true
            ),

            // ========== MODERATE FISH (difficulty 3-4, value 20-45) ==========
            // These put up a decent fight
            Item(
                id = "fish-trout",
                name = "Trout",
                desc = "A fresh rainbow trout, its scales glinting with color. Feisty but manageable.",
                featureIds = emptyList(),
                value = 25,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-bass",
                name = "Bass",
                desc = "A plump largemouth bass, a fisherman's prize. Known for jumping when hooked.",
                featureIds = emptyList(),
                value = 32,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-catfish",
                name = "Catfish",
                desc = "A whiskered catfish, slippery and strong. Digs deep when hooked.",
                featureIds = emptyList(),
                value = 38,
                weight = 2,
                isStackable = true
            ),
            Item(
                id = "fish-crappie",
                name = "Crappie",
                desc = "A panfish that travels in schools. Quick to bite, quick to fight.",
                featureIds = emptyList(),
                value = 22,
                weight = 1,
                isStackable = true
            ),

            // ========== CHALLENGING FISH (difficulty 5-6, value 50-100) ==========
            // Real sport fish - require skill
            Item(
                id = "fish-pike",
                name = "Northern Pike",
                desc = "A northern pike with razor teeth and explosive strikes. Mean and powerful.",
                featureIds = emptyList(),
                value = 55,
                weight = 3,
                isStackable = true
            ),
            Item(
                id = "fish-salmon",
                name = "Salmon",
                desc = "A magnificent salmon, pink-fleshed and delicious. Famous for leaping upstream.",
                featureIds = emptyList(),
                value = 70,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-walleye",
                name = "Walleye",
                desc = "A walleye with distinctive glassy eyes. Subtle bites but strong runs.",
                featureIds = emptyList(),
                value = 85,
                weight = 3,
                isStackable = true
            ),
            Item(
                id = "fish-carp",
                name = "Carp",
                desc = "A heavy golden carp. Uses its bulk to resist with steady determination.",
                featureIds = emptyList(),
                value = 65,
                weight = 4,
                isStackable = true
            ),

            // ========== HARD FISH (difficulty 7-8, value 120-220) ==========
            // Serious anglers only - these fish FIGHT
            Item(
                id = "fish-muskie",
                name = "Muskellunge",
                desc = "The 'fish of ten thousand casts.' Ambush predator that tests your patience and skill.",
                featureIds = emptyList(),
                value = 140,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-sturgeon",
                name = "Sturgeon",
                desc = "An ancient-looking sturgeon, armored with bony plates. Can fight for an hour.",
                featureIds = emptyList(),
                value = 180,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-giant-catfish",
                name = "Giant Catfish",
                desc = "An enormous catfish, the stuff of legends. Its whiskers are as long as your arm.",
                featureIds = emptyList(),
                value = 160,
                weight = 6,
                isStackable = true
            ),
            Item(
                id = "fish-steelhead",
                name = "Steelhead",
                desc = "A sea-run rainbow trout. Acrobatic fighter with incredible stamina.",
                featureIds = emptyList(),
                value = 200,
                weight = 4,
                isStackable = true
            ),

            // ========== VERY HARD / RARE (difficulty 9, value 300-500) ==========
            // Trophy fish - only the best land these
            Item(
                id = "fish-legendary-bass",
                name = "Legendary Bass",
                desc = "A massive bass of legendary proportions. Fishermen speak of this one in hushed tones.",
                featureIds = emptyList(),
                value = 350,
                weight = 6,
                isStackable = true
            ),
            Item(
                id = "fish-golden-trout",
                name = "Golden Trout",
                desc = "A rare golden trout that shimmers with an almost magical light. Lives only in pristine alpine waters.",
                featureIds = emptyList(),
                value = 400,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-ancient-sturgeon",
                name = "Ancient Sturgeon",
                desc = "A prehistoric-looking sturgeon that may have lived for centuries. Fights with ancient wisdom.",
                featureIds = emptyList(),
                value = 450,
                weight = 8,
                isStackable = true
            ),
            Item(
                id = "fish-tiger-muskie",
                name = "Tiger Muskie",
                desc = "A rare hybrid predator with tiger stripes. Combines the aggression of both parent species.",
                featureIds = emptyList(),
                value = 480,
                weight = 6,
                isStackable = true
            ),

            // ========== ULTRA RARE / LEGENDARY (difficulty 10, value 600-2000) ==========
            // Mythical fish - once in a lifetime catches
            Item(
                id = "fish-albino-catfish",
                name = "Albino Catfish",
                desc = "A ghostly white catfish, almost never seen. Local legend says it grants luck to those who release it.",
                featureIds = emptyList(),
                value = 650,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-emerald-sturgeon",
                name = "Emerald Sturgeon",
                desc = "A sturgeon with scales that shimmer green like emeralds. Said to contain a precious gem in its belly.",
                featureIds = emptyList(),
                value = 800,
                weight = 7,
                isStackable = true
            ),
            Item(
                id = "fish-spectral-pike",
                name = "Spectral Pike",
                desc = "A translucent pike that seems to phase in and out of reality. Some say it swims between worlds.",
                featureIds = emptyList(),
                value = 900,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-blood-salmon",
                name = "Blood Salmon",
                desc = "A crimson salmon said to spawn once every decade. Its flesh is rumored to have magical properties.",
                featureIds = emptyList(),
                value = 1000,
                weight = 5,
                isStackable = true
            ),
            Item(
                id = "fish-deepmaw",
                name = "Deepmaw",
                desc = "A terrifying fish from lake depths that never see sunlight. All teeth and hunger.",
                featureIds = emptyList(),
                value = 1100,
                weight = 6,
                isStackable = true
            ),
            Item(
                id = "fish-prismatic-koi",
                name = "Prismatic Koi",
                desc = "A legendary koi that shimmers with every color of the rainbow. Considered sacred in many cultures.",
                featureIds = emptyList(),
                value = 1300,
                weight = 4,
                isStackable = true
            ),
            Item(
                id = "fish-lake-wyrm",
                name = "Lake Wyrm",
                desc = "A serpentine creature of impossible length. Is it a fish or something else entirely?",
                featureIds = emptyList(),
                value = 1500,
                weight = 9,
                isStackable = true
            ),
            Item(
                id = "fish-old-whiskers",
                name = "Old Whiskers",
                desc = "THE catfish. A hundred years old if it's a day. Locals have named it and tell tales of its escapes.",
                featureIds = emptyList(),
                value = 1800,
                weight = 10,
                isStackable = true
            ),
            Item(
                id = "fish-the-leviathan",
                name = "The Leviathan",
                desc = "A creature of myth made flesh. Entire boats have been pulled under trying to land this one. Why are you fishing for this?!",
                featureIds = emptyList(),
                value = 2000,
                weight = 10,
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
            // Near shore - mostly easy fish, small chance of moderate
            LootTableData(
                id = "loot-table-fishing-near",
                name = "Near Shore Fish",
                entries = listOf(
                    // Easy (85%)
                    LootEntry("fish-minnow", chance = 0.28f),
                    LootEntry("fish-bluegill", chance = 0.25f),
                    LootEntry("fish-perch", chance = 0.18f),
                    LootEntry("fish-sardine", chance = 0.14f),
                    // Moderate (15%)
                    LootEntry("fish-crappie", chance = 0.07f),
                    LootEntry("fish-trout", chance = 0.05f),
                    LootEntry("fish-bass", chance = 0.03f)
                )
            ),
            // Mid water - moderate to challenging, small chance at hard fish
            LootTableData(
                id = "loot-table-fishing-mid",
                name = "Mid Water Fish",
                entries = listOf(
                    // Moderate (40%)
                    LootEntry("fish-trout", chance = 0.15f),
                    LootEntry("fish-bass", chance = 0.13f),
                    LootEntry("fish-catfish", chance = 0.12f),
                    // Challenging (45%)
                    LootEntry("fish-pike", chance = 0.14f),
                    LootEntry("fish-salmon", chance = 0.12f),
                    LootEntry("fish-walleye", chance = 0.10f),
                    LootEntry("fish-carp", chance = 0.09f),
                    // Hard (15%)
                    LootEntry("fish-muskie", chance = 0.06f),
                    LootEntry("fish-sturgeon", chance = 0.04f),
                    LootEntry("fish-steelhead", chance = 0.03f),
                    LootEntry("fish-giant-catfish", chance = 0.02f)
                )
            ),
            // Deep water - challenging to legendary, this is where the monsters live
            LootTableData(
                id = "loot-table-fishing-far",
                name = "Deep Water Fish",
                entries = listOf(
                    // Challenging (20%)
                    LootEntry("fish-salmon", chance = 0.10f),
                    LootEntry("fish-walleye", chance = 0.10f),
                    // Hard (35%)
                    LootEntry("fish-muskie", chance = 0.12f),
                    LootEntry("fish-sturgeon", chance = 0.10f),
                    LootEntry("fish-giant-catfish", chance = 0.07f),
                    LootEntry("fish-steelhead", chance = 0.06f),
                    // Very Hard / Rare (30%)
                    LootEntry("fish-legendary-bass", chance = 0.08f),
                    LootEntry("fish-golden-trout", chance = 0.07f),
                    LootEntry("fish-ancient-sturgeon", chance = 0.06f),
                    LootEntry("fish-tiger-muskie", chance = 0.09f),
                    // Ultra Rare / Legendary (15% combined, individually rare)
                    LootEntry("fish-albino-catfish", chance = 0.04f),
                    LootEntry("fish-emerald-sturgeon", chance = 0.03f),
                    LootEntry("fish-spectral-pike", chance = 0.025f),
                    LootEntry("fish-blood-salmon", chance = 0.02f),
                    LootEntry("fish-deepmaw", chance = 0.015f),
                    LootEntry("fish-prismatic-koi", chance = 0.01f),
                    LootEntry("fish-lake-wyrm", chance = 0.007f),
                    LootEntry("fish-old-whiskers", chance = 0.005f),
                    LootEntry("fish-the-leviathan", chance = 0.003f)
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
