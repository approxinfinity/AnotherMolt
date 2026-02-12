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
            ),
            // === INTELLIGENT WEAPONS ===
            ManualTestItem(
                featureName = "Intelligent Weapon: View Properties",
                description = "Find an intelligent weapon (e.g. Starweaver, Doomwhisper). Expand it in inventory. Verify INT, Ego, alignment, powers, and personality quirk display correctly.",
                category = "items"
            ),
            ManualTestItem(
                featureName = "Intelligent Weapon: Ego Contest on Equip",
                description = "Equip an intelligent weapon with mismatched alignment. Verify ego contest message appears. If contest fails, weapon should not equip.",
                category = "items"
            ),
            ManualTestItem(
                featureName = "Intelligent Weapon: Compatible Alignment Equip",
                description = "Equip an intelligent weapon with compatible alignment. Verify it equips successfully with acceptance message.",
                category = "items"
            ),

            // === RANDOM GEM/JEWELRY VALUES ===
            ManualTestItem(
                featureName = "Random Gem Values: Loot Drop",
                description = "Kill a creature with gems in its loot table. Verify the dropped gem has a randomized name (e.g. Ruby, Amethyst) and value instead of the template's fixed value.",
                category = "items"
            ),
            ManualTestItem(
                featureName = "Random Jewelry Values: Loot Drop",
                description = "Kill a creature with jewelry in its loot table. Verify dropped jewelry has a randomized name and value. Values should range from 300-10000gp.",
                category = "items"
            ),

            // === WANDERING MONSTERS ===
            ManualTestItem(
                featureName = "Wandering Monsters: Biome Spawn",
                description = "Idle at an outdoor wilderness location for a few minutes. Verify a wandering monster appropriate to the biome occasionally spawns (1-in-6 chance every ~30s).",
                category = "combat"
            ),
            ManualTestItem(
                featureName = "Wandering Monsters: Despawn Timer",
                description = "Wait for a wandering monster to spawn, then do NOT attack it. After ~5 minutes it should despawn automatically and disappear from the location.",
                category = "combat"
            ),
            ManualTestItem(
                featureName = "Wandering Monsters: No Town Spawns",
                description = "Idle at the Keep on the Borderlands (town area) for several minutes. Verify that no wandering monsters spawn in the safe town area.",
                category = "combat"
            ),

            // === WILDERNESS MOVEMENT ENCOUNTERS ===
            ManualTestItem(
                featureName = "Wilderness Encounters: Movement Spawn",
                description = "Move between outdoor wilderness locations repeatedly (not in town). Verify encounters spawn roughly 1-in-6 moves. A flavor message should appear and creature shows at the destination.",
                category = "combat"
            ),
            ManualTestItem(
                featureName = "Wilderness Encounters: Indoor Exempt",
                description = "Move from an indoor/underground location to an outdoor location (or vice versa). Verify no movement encounter triggers — only outdoor-to-outdoor movement should spawn encounters.",
                category = "combat"
            ),
            ManualTestItem(
                featureName = "Wilderness Encounters: Auto-Combat",
                description = "When an aggressive creature spawns from a movement encounter, verify combat auto-starts immediately via the existing aggressive creature check.",
                category = "combat"
            ),

            // === TREASURE MAPS ===
            ManualTestItem(
                featureName = "Treasure Maps: Read with INT Check",
                description = "Find a treasure map in your inventory (drop from goblins, orcs, trolls, or ghouls). Expand it and click 'Read Map'. Verify INT check occurs and either reveals a cryptic hint or fails with a message showing the roll.",
                category = "items"
            ),
            ManualTestItem(
                featureName = "Treasure Maps: Claim at Destination",
                description = "After reading a treasure map, travel to the hinted destination location. Click 'Search for Treasure'. Verify gold and items are awarded, and the map is removed from inventory.",
                category = "items"
            ),
            ManualTestItem(
                featureName = "Treasure Maps: Wrong Location",
                description = "After reading a treasure map, try clicking 'Search for Treasure' while NOT at the destination. Verify it shows a rejection message like 'This doesn't seem to be the right place.'",
                category = "items"
            ),
            ManualTestItem(
                featureName = "Treasure Maps: Loot Drop",
                description = "Kill goblins, orcs, trolls, or ghouls repeatedly. Verify treasure maps occasionally drop (3-5% chance). Check that the map appears in inventory with a treasure map icon section.",
                category = "items"
            ),

            // === PERSONALIZED COMBAT MESSAGES ===
            ManualTestItem(
                featureName = "Combat Flavor: Weapon-Specific Verbs",
                description = "Equip different weapon types (sword, dagger, mace, bow, etc.) and use the basic Attack ability. Verify combat messages use weapon-appropriate verbs (slashes, stabs, bashes, shoots) instead of generic 'hits with Attack'.",
                category = "combat"
            ),
            ManualTestItem(
                featureName = "Combat Flavor: Creature Attack Verbs",
                description = "Enter combat with various creature types (goblins, wolves, trolls, spiders). Verify creature attack messages use creature-appropriate verbs (goblins slash, wolves claw/bite, trolls smash) instead of generic 'attacks'.",
                category = "combat"
            ),
            ManualTestItem(
                featureName = "Combat Flavor: Named Abilities Unchanged",
                description = "Use a named ability (e.g. Vicious Strike, Power Attack) in combat. Verify the message still uses the original format ('hits with Vicious Strike') and is NOT replaced by weapon flavor text.",
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
