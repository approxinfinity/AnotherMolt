package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seeds initial manual test items for features that need QA verification.
 * Items are identified by feature name to avoid duplicates on subsequent runs.
 */
object ManualTestItemSeed {
    private val log = LoggerFactory.getLogger(ManualTestItemSeed::class.java)

    fun seed() {
        log.info("Seeding manual test items...")
        var created = 0

        val testItems = listOf(
            // === FACTION SYSTEM ===
            ManualTestItem(
                featureName = "Diplomacy: Bribe Faction Creature",
                description = "Click on a faction creature (kobold, goblin, etc.) and use the Bribe button. Verify gold is deducted and combat is avoided.",
                category = "faction",
                commitHash = "5e650db"
            ),
            ManualTestItem(
                featureName = "Diplomacy: Parley with Faction Creature",
                description = "Click on a faction creature and use the Parley button. Success depends on WIS stat. Verify message shows success/failure.",
                category = "faction",
                commitHash = "5e650db"
            ),
            ManualTestItem(
                featureName = "Faction Standing Changes",
                description = "Kill faction creatures and verify your standing with that faction decreases. Check enemy factions gain standing.",
                category = "faction",
                commitHash = "0445f03"
            ),
            ManualTestItem(
                featureName = "Tribal Wars Events",
                description = "Wait ~5 minutes and check if tribal war events appear in logs. Enemy factions should occasionally fight each other.",
                category = "faction",
                commitHash = "43d825d"
            ),

            // === DUNGEON MECHANICS ===
            ManualTestItem(
                featureName = "Trap Detection",
                description = "Enter a trapped room and verify trap detection message appears based on your perception/class.",
                category = "dungeon",
                commitHash = "d9fcc9d"
            ),
            ManualTestItem(
                featureName = "Trap Disarm",
                description = "Find a detected trap and attempt to disarm it. Verify success/failure based on DEX and thief skills.",
                category = "dungeon",
                commitHash = "d9fcc9d"
            ),
            ManualTestItem(
                featureName = "Secret Door Discovery",
                description = "Use the Search action in rooms with secret doors. Verify hidden exits appear after successful search.",
                category = "dungeon",
                commitHash = "d9fcc9d"
            ),
            ManualTestItem(
                featureName = "Direction Confusion Effect",
                description = "Find a confusion pool/effect. Verify compass directions become scrambled for affected players.",
                category = "dungeon",
                commitHash = "d9fcc9d"
            ),

            // === POOLS ===
            ManualTestItem(
                featureName = "Room of Pools: Healing Pool",
                description = "Find and drink from a healing pool. Verify HP is restored.",
                category = "pools",
                commitHash = "59d5ce1"
            ),
            ManualTestItem(
                featureName = "Room of Pools: Stat Change Pool",
                description = "Find a stat-modifying pool. Verify stats change after drinking.",
                category = "pools",
                commitHash = "59d5ce1"
            ),
            ManualTestItem(
                featureName = "Room of Pools: Danger Pool",
                description = "Find a dangerous pool (poison, curse). Verify negative effects apply.",
                category = "pools",
                commitHash = "59d5ce1"
            ),

            // === SHOPS ===
            ManualTestItem(
                featureName = "Buy Items from Shop",
                description = "Visit a shop location and purchase an item. Verify gold deducted and item added to inventory.",
                category = "shop",
                commitHash = "5a4c7b5"
            ),
            ManualTestItem(
                featureName = "Sell Items at Shop",
                description = "Visit a shop and sell an item from inventory. Verify item removed and gold received.",
                category = "shop",
                commitHash = "5a4c7b5"
            ),
            ManualTestItem(
                featureName = "Rest at Inn",
                description = "Visit an inn and use the rest option. Verify HP/Mana/Stamina are restored and gold is charged.",
                category = "shop",
                commitHash = "5a4c7b5"
            ),

            // === NAVIGATION ===
            ManualTestItem(
                featureName = "UP/DOWN Navigation",
                description = "Find a location with UP or DOWN exits (stairs, ladder). Verify movement works and Z-coordinate changes.",
                category = "navigation",
                commitHash = "d0b3dfd"
            ),
            ManualTestItem(
                featureName = "Lockpicking Minigame",
                description = "Find a locked door and attempt to pick it. Complete the trace-the-path minigame.",
                category = "navigation",
                commitHash = "ce893f1"
            ),
            ManualTestItem(
                featureName = "Lock Reset After Time",
                description = "Pick a lock and leave. Return after 1 minute and verify the lock has reset.",
                category = "navigation",
                commitHash = "3d4fccc"
            ),

            // === FISHING ===
            ManualTestItem(
                featureName = "Fishing Minigame",
                description = "Find a fishing spot and start fishing. Complete the keep-fish-in-zone minigame.",
                category = "fishing",
                commitHash = "ce893f1"
            ),
            ManualTestItem(
                featureName = "Fishing Distance Requirements",
                description = "Try different fishing distances (near/mid/far). Verify STR requirement messages appear.",
                category = "fishing",
                commitHash = "ce893f1"
            ),

            // === COMBAT ===
            ManualTestItem(
                featureName = "Creature Respawn",
                description = "Kill all creatures in an area and wait. Verify creatures respawn after the configured interval.",
                category = "combat",
                commitHash = "67f8ffb"
            ),
            ManualTestItem(
                featureName = "Weapon Abilities",
                description = "Equip a weapon with special abilities. Verify the ability appears in action bar and can be used.",
                category = "combat",
                commitHash = "4ebe5c0"
            ),

            // === SEARCH/ITEMS ===
            ManualTestItem(
                featureName = "Search for Hidden Items",
                description = "Use the Search action in a room with hidden items. Verify items marked with * prefix appear.",
                category = "items",
                commitHash = "b9b9454"
            ),
            ManualTestItem(
                featureName = "Discovered Items Persist",
                description = "Discover an item via search, leave the room, and return. Verify the item is still visible.",
                category = "items",
                commitHash = "d813ca3"
            ),

            // === SPECIAL LOCATIONS ===
            ManualTestItem(
                featureName = "Grandma's Shed Locked Door",
                description = "Find Grandma's Shed entrance. Verify it requires lockpicking or a key to enter.",
                category = "locations",
                commitHash = "8b5c506"
            ),
            ManualTestItem(
                featureName = "Puzzle: Lever Sequences",
                description = "Find a puzzle room with levers. Pull levers in correct sequence to unlock secret passage.",
                category = "puzzles",
                commitHash = "d9fcc9d"
            ),

            // === NPC REACTIONS ===
            ManualTestItem(
                featureName = "NPC Reaction: First Encounter",
                description = "Click on a non-aggressive creature. Verify a colored reaction badge appears (red=Hostile, orange=Uncertain, green=Friendly) with a flavor message.",
                category = "combat"
            ),
            ManualTestItem(
                featureName = "NPC Reaction: Persistent Result",
                description = "Click a creature, note its reaction. Dismiss and click again. Verify the same reaction appears (persisted per user-creature pair).",
                category = "combat"
            ),
            ManualTestItem(
                featureName = "NPC Reaction: Aggressive Always Hostile",
                description = "Click on an aggressive creature (red name). Verify reaction badge shows 'Hostile' without rolling.",
                category = "combat"
            )
        )

        for (item in testItems) {
            val result = ManualTestItemRepository.createIfNotExists(item)
            if (result != null) {
                created++
            }
        }

        log.info("Manual test items seed complete: $created new items created")
    }
}
