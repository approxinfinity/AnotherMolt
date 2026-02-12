package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.game.TreasureMapData
import com.ez2bg.anotherthread.game.TreasureMapService
import org.slf4j.LoggerFactory

/**
 * Seeds treasure map items into the game world.
 * Each map is an item that references a treasure-map feature,
 * and is added as a rare drop to existing loot tables.
 */
object TreasureMapSeed {
    private val log = LoggerFactory.getLogger(TreasureMapSeed::class.java)

    fun seedIfEmpty() {
        // Check if already seeded
        if (ItemRepository.findById("item-treasure-map-bandits-stash") != null) {
            log.debug("Treasure maps already seeded, skipping")
            return
        }

        log.info("Seeding treasure maps...")

        // 1. Bandit's Stash Map - easy, drops from goblins
        seedTreasureMap(
            itemId = "item-treasure-map-bandits-stash",
            name = "Bandit's Stash Map",
            description = "A crude map scrawled on torn parchment, marked with an 'X' in charcoal. Smells faintly of goblin.",
            data = TreasureMapData(
                destinationLocationId = "location-keep-borderlands-road-to-keep",
                destinationHint = "Where the narrow track winds upward toward civilization, search beneath the loose stones on the cliff side of the road.",
                readDifficulty = 10,
                rewardGold = 50,
                rewardItemIds = listOf(ClassicFantasySeed.MINOR_HEALING_POTION_ID)
            )
        )

        // 2. Ancient Burial Map - medium, drops from orcs
        seedTreasureMap(
            itemId = "item-treasure-map-ancient-burial",
            name = "Ancient Burial Map",
            description = "A fragment of vellum showing burial grounds marked in faded red ink. The script is ancient and nearly illegible.",
            data = TreasureMapData(
                destinationLocationId = "location-undead-crypt-vestibule",
                destinationHint = "In the vestibule of the dead, where the air hangs still and cold, the treasure lies behind the third stone from the left.",
                readDifficulty = 14,
                rewardGold = 200,
                rewardItemIds = listOf(ClassicFantasySeed.SILVER_DAGGER_ID)
            )
        )

        // 3. Dragon's Hoard Fragment - hard, drops from trolls
        seedTreasureMap(
            itemId = "item-treasure-map-dragons-hoard",
            name = "Dragon's Hoard Fragment",
            description = "A piece of scorched leather showing part of a vast cavern system. Claw marks score the edges, as if torn from a larger map.",
            data = TreasureMapData(
                destinationLocationId = "location-caves-of-chaos-cave-f-chieftain",
                destinationHint = "In the chieftain's lair deep within the ravine caves, where the strongest warriors dwell, a hidden cache lies beneath the throne of bones.",
                readDifficulty = 16,
                rewardGold = 500,
                rewardItemIds = listOf(ClassicDungeonSeed.CLOAK_OF_PROTECTION_ID)
            )
        )

        // 4. Pirate's Treasure Chart - medium, drops from ghouls
        seedTreasureMap(
            itemId = "item-treasure-map-pirates-chart",
            name = "Pirate's Treasure Chart",
            description = "A waterlogged nautical chart with strange symbols and a skull marking. The parchment is stiff with old salt.",
            data = TreasureMapData(
                destinationLocationId = "location-quasqueton-underground-lake",
                destinationHint = "Beneath the earth where still waters gather in darkness, search the rocky shore on the far side of the underground lake.",
                readDifficulty = 12,
                rewardGold = 150,
                rewardItemIds = listOf(ClassicFantasySeed.CLOAK_ELVENKIND_ID)
            )
        )

        // Add maps to loot tables
        addToLootTables()

        log.info("Seeded 4 treasure maps")
    }

    private fun seedTreasureMap(
        itemId: String,
        name: String,
        description: String,
        data: TreasureMapData
    ) {
        // Create the feature
        val featureId = itemId.replace("item-", "")  // "treasure-map-bandits-stash"
        if (FeatureRepository.findById(featureId) == null) {
            TreasureMapService.createTreasureMapFeature(
                featureId = featureId,
                mapName = name,
                data = data
            )
        }

        // Create the item
        if (ItemRepository.findById(itemId) == null) {
            ItemRepository.create(Item(
                id = itemId,
                name = name,
                desc = description,
                value = 0,  // No sell value - the real value is the treasure
                weight = 0,
                featureIds = listOf(featureId)
            ))
        }
    }

    private fun addToLootTables() {
        // Map: loot table ID -> (treasure map item ID, drop chance)
        val lootTableAssignments = listOf(
            // Bandit's Stash Map -> Goblin loot (5%)
            Triple(ClassicFantasySeed.LOOT_TABLE_GOBLIN_ID, "item-treasure-map-bandits-stash", 0.05f),
            // Ancient Burial Map -> Orc loot (3%)
            Triple(ClassicFantasySeed.LOOT_TABLE_ORC_ID, "item-treasure-map-ancient-burial", 0.03f),
            // Dragon's Hoard Fragment -> Troll loot (5%)
            Triple(ClassicFantasySeed.LOOT_TABLE_TROLL_ID, "item-treasure-map-dragons-hoard", 0.05f),
            // Pirate's Treasure Chart -> Ghoul loot (4%)
            Triple(ClassicFantasySeed.LOOT_TABLE_GHOUL_ID, "item-treasure-map-pirates-chart", 0.04f)
        )

        for ((lootTableId, mapItemId, chance) in lootTableAssignments) {
            val lootTable = LootTableRepository.findById(lootTableId) ?: continue
            // Only add if not already present
            val existingItemIds = lootTable.entries.map { it.itemId }
            if (mapItemId in existingItemIds) continue

            val updatedEntries = lootTable.entries + LootEntry(
                itemId = mapItemId,
                chance = chance,
                minQty = 1,
                maxQty = 1
            )
            LootTableRepository.update(lootTable.copy(entries = updatedEntries))
        }
    }
}
