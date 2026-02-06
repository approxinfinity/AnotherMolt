package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for Goblin Warren dungeon locations.
 * A starter dungeon for level 1 players featuring goblins, kobolds, and classic fantasy creatures.
 * Uses creatures from ClassicFantasySeed.
 *
 * Layout:
 *   - Cave Entrance (from overworld)
 *   - Rat-infested tunnels (Giant Rats)
 *   - Goblin guard posts and barracks
 *   - Kobold trap area
 *   - Orc war chief (mini-boss)
 *   - Troll lair (boss)
 */
object GoblinWarrenLocationsSeed {
    private val log = LoggerFactory.getLogger(GoblinWarrenLocationsSeed::class.java)

    // Attribution for tracking content source
    private const val ATTRIBUTION = "Classic D&D Goblin Warren"

    // Area ID for all warren locations
    const val AREA_ID = "goblin-warren"

    // ============== LOCATION IDS ==============
    const val WARREN_CAVE_ENTRANCE_ID = "location-goblin-warren-entrance"
    const val WARREN_RAT_TUNNELS_ID = "location-goblin-warren-rat-tunnels"
    const val WARREN_RAT_NEST_ID = "location-goblin-warren-rat-nest"
    const val WARREN_GUARD_POST_ID = "location-goblin-warren-guard-post"
    const val WARREN_MAIN_CAVERN_ID = "location-goblin-warren-main-cavern"
    const val WARREN_BARRACKS_ID = "location-goblin-warren-barracks"
    const val WARREN_STOREROOM_ID = "location-goblin-warren-storeroom"
    const val WARREN_KOBOLD_TUNNELS_ID = "location-goblin-warren-kobold-tunnels"
    const val WARREN_TRAP_CORRIDOR_ID = "location-goblin-warren-trap-corridor"
    const val WARREN_ORC_QUARTERS_ID = "location-goblin-warren-orc-quarters"
    const val WARREN_TROLL_LAIR_ID = "location-goblin-warren-troll-lair"

    // Overworld connection point
    const val OVERWORLD_CAVE_ENTRANCE_ID = "location-overworld-dark-cave"

    fun seedIfEmpty() {
        val existingLocation = LocationRepository.findById(WARREN_CAVE_ENTRANCE_ID)
        if (existingLocation != null) return

        log.info("Seeding Goblin Warren locations...")
        seedOverworldEntrance()
        seedWarrenLocations()
        log.info("Seeded Goblin Warren locations")
    }

    private fun seedOverworldEntrance() {
        // Create an overworld cave entrance
        val darkCave = Location(
            id = OVERWORLD_CAVE_ENTRANCE_ID,
            name = "Dark Cave Entrance",
            desc = "A jagged cave mouth yawns in the hillside, partially obscured by thornbushes and dead vines. The stench of rotting meat and unwashed bodies wafts from within. Crude pictographs are scratched into the rocks around the entrance - warnings in a primitive language, or perhaps territorial markings. Bones of small animals litter the ground, picked clean. Something lives in these caves, and it doesn't welcome visitors.",
            itemIds = emptyList(),
            creatureIds = listOf(ClassicFantasySeed.GIANT_RAT_ID),
            exits = listOf(
                Exit(locationId = WARREN_CAVE_ENTRANCE_ID, direction = ExitDirection.ENTER)
            ),
            featureIds = emptyList(),
            gridX = -5,
            gridY = 3,
            areaId = "overworld",
            locationType = LocationType.OUTDOOR_GROUND
        )

        if (LocationRepository.findById(darkCave.id) == null) {
            LocationRepository.create(darkCave)
        }
    }

    private fun seedWarrenLocations() {
        val locations = listOf(
            // ============== ENTRY AREA (Very easy encounters) ==============
            Location(
                id = WARREN_CAVE_ENTRANCE_ID,
                name = "Warren Entrance",
                desc = "The cave entrance opens into a low-ceilinged passage that reeks of goblin refuse. Torches guttering in crude iron brackets cast flickering shadows on the damp stone walls. The floor is covered with a mixture of mud, bones, and things better left unidentified. Scratching sounds echo from deeper within, accompanied by occasional high-pitched chittering.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicFantasySeed.GIANT_RAT_ID, ClassicFantasySeed.GIANT_RAT_ID),
                exits = listOf(
                    Exit(locationId = OVERWORLD_CAVE_ENTRANCE_ID, direction = ExitDirection.ENTER),
                    Exit(locationId = WARREN_RAT_TUNNELS_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = 0,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = WARREN_RAT_TUNNELS_ID,
                name = "Rat-Infested Tunnels",
                desc = "A network of cramped tunnels that have been taken over by swarms of oversized rats. The walls are riddled with small holes - rat burrows that connect throughout the warren. The creatures here have grown fat on goblin scraps and anything else they can catch. Their red eyes gleam in the darkness, and they show no fear of creatures larger than themselves.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicFantasySeed.GIANT_RAT_ID, ClassicFantasySeed.GIANT_RAT_ID, ClassicFantasySeed.GIANT_RAT_ID),
                exits = listOf(
                    Exit(locationId = WARREN_CAVE_ENTRANCE_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = WARREN_RAT_NEST_ID, direction = ExitDirection.WEST),
                    Exit(locationId = WARREN_GUARD_POST_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = WARREN_RAT_NEST_ID,
                name = "Giant Rat Nest",
                desc = "A small cavern that serves as the main nest for the warren's rat population. The floor is a mass of shredded cloth, gnawed bones, and other detritus piled into crude nests. Several particularly large rats watch you with unsettling intelligence, their whiskers twitching. Among the debris, you spot a few items that must have belonged to previous visitors who didn't make it out.",
                itemIds = listOf(ClassicFantasySeed.MINOR_HEALING_POTION_ID, ClassicFantasySeed.ANCIENT_GOLD_COIN_ID),
                creatureIds = listOf(ClassicFantasySeed.GIANT_RAT_ID, ClassicFantasySeed.GIANT_RAT_ID, ClassicFantasySeed.GIANT_SPIDER_ID),
                exits = listOf(
                    Exit(locationId = WARREN_RAT_TUNNELS_ID, direction = ExitDirection.EAST)
                ),
                featureIds = emptyList(),
                gridX = -1,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== GOBLIN AREAS (Tier 1 encounters) ==============
            Location(
                id = WARREN_GUARD_POST_ID,
                name = "Goblin Guard Post",
                desc = "A widening in the tunnel where goblins have established a checkpoint. A crude barricade of broken furniture and sharpened stakes blocks the passage, with gaps left for goblin-sized creatures to slip through. Empty ale bottles and gnawed bones show the guards spend more time drinking and gambling than watching. Still, they raise the alarm when they spot intruders.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicFantasySeed.GOBLIN_WARRIOR_ID, ClassicFantasySeed.GOBLIN_WARRIOR_ID),
                exits = listOf(
                    Exit(locationId = WARREN_RAT_TUNNELS_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = WARREN_MAIN_CAVERN_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = WARREN_MAIN_CAVERN_ID,
                name = "Main Cavern",
                desc = "A large natural cavern that serves as the heart of the goblin warren. Crude shelters made of hide and sticks cluster around a central fire pit where something unidentifiable bubbles in a massive iron cauldron. Goblins of all sizes scurry about their business - sharpening weapons, fighting over scraps, or simply causing mischief. Passages lead off in several directions.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicFantasySeed.GOBLIN_WARRIOR_ID, ClassicFantasySeed.GOBLIN_WARRIOR_ID, ClassicFantasySeed.GOBLIN_WARRIOR_ID),
                exits = listOf(
                    Exit(locationId = WARREN_GUARD_POST_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = WARREN_BARRACKS_ID, direction = ExitDirection.EAST),
                    Exit(locationId = WARREN_STOREROOM_ID, direction = ExitDirection.WEST),
                    Exit(locationId = WARREN_KOBOLD_TUNNELS_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -3,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = WARREN_BARRACKS_ID,
                name = "Goblin Barracks",
                desc = "A long cave filled with crude bedding - piles of moldy straw and animal hides that serve as goblin beds. Weapons and armor (such as they are) hang from pegs driven into the walls or lie scattered carelessly about. The smell is absolutely overwhelming - a mixture of unwashed goblin, rotting food, and things you'd rather not identify. Several goblins are here, some sleeping, others sharpening their rusty blades.",
                itemIds = listOf(ClassicFantasySeed.GOBLIN_HIDE_ARMOR_ID, ClassicFantasySeed.ANTIDOTE_VIAL_ID),
                creatureIds = listOf(ClassicFantasySeed.GOBLIN_WARRIOR_ID, ClassicFantasySeed.GOBLIN_WARRIOR_ID, ClassicFantasySeed.HOBGOBLIN_CAPTAIN_ID),
                exits = listOf(
                    Exit(locationId = WARREN_MAIN_CAVERN_ID, direction = ExitDirection.WEST),
                    Exit(locationId = WARREN_ORC_QUARTERS_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -3,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = WARREN_STOREROOM_ID,
                name = "Warren Storeroom",
                desc = "A damp cave used to store the warren's supplies and loot. Sacks of stolen grain slump against the walls, many torn open by rats. Barrels of ale (some leaking) stand in a corner. Crates hold a miscellaneous collection of stolen goods - everything from farmers' tools to a few actual valuables. The goblins have no concept of organization; it's all just piled together.",
                itemIds = listOf(ClassicFantasySeed.MINOR_HEALING_POTION_ID, ClassicFantasySeed.ANCIENT_GOLD_COIN_ID, ClassicFantasySeed.ANCIENT_GOLD_COIN_ID),
                creatureIds = listOf(ClassicFantasySeed.GOBLIN_WARRIOR_ID),
                exits = listOf(
                    Exit(locationId = WARREN_MAIN_CAVERN_ID, direction = ExitDirection.EAST)
                ),
                featureIds = emptyList(),
                gridX = -1,
                gridY = -3,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== KOBOLD AREA (Tier 1 with traps) ==============
            Location(
                id = WARREN_KOBOLD_TUNNELS_ID,
                name = "Kobold Tunnels",
                desc = "These tunnels are smaller and more cramped than the goblin areas - sized for the diminutive kobolds who have allied with the warren. The walls are smoother here, properly excavated rather than natural caves. Strange symbols are painted at intervals - warnings and territorial markers in the kobold language. The kobolds are smarter than goblins and far more devious.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicFantasySeed.KOBOLD_TRAPPER_ID, ClassicFantasySeed.KOBOLD_TRAPPER_ID, ClassicFantasySeed.KOBOLD_TRAPPER_ID),
                exits = listOf(
                    Exit(locationId = WARREN_MAIN_CAVERN_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = WARREN_TRAP_CORRIDOR_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -4,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = WARREN_TRAP_CORRIDOR_ID,
                name = "Trap-Filled Corridor",
                desc = "The kobolds' pride and joy - a long corridor filled with their ingenious traps. Pressure plates trigger crossbow bolts from hidden slots. Tripwires release swinging blades. Pit traps covered with false floors wait for unwary feet. The kobolds know every safe path and delight in watching intruders stumble into their creations. They cackle from hidden alcoves, ready to finish off survivors.",
                itemIds = listOf(ClassicFantasySeed.SILVER_DAGGER_ID),
                creatureIds = listOf(ClassicFantasySeed.KOBOLD_TRAPPER_ID, ClassicFantasySeed.KOBOLD_TRAPPER_ID, ClassicFantasySeed.GIANT_SPIDER_ID),
                exits = listOf(
                    Exit(locationId = WARREN_KOBOLD_TUNNELS_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = WARREN_ORC_QUARTERS_ID, direction = ExitDirection.EAST),
                    Exit(locationId = WARREN_TROLL_LAIR_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -5,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== BOSS AREA (Tier 2-3 encounters) ==============
            Location(
                id = WARREN_ORC_QUARTERS_ID,
                name = "Orc War Chief's Quarters",
                desc = "A larger cave that has been claimed by an orc war chief who leads the goblin and kobold tribes. The orc has decorated with trophies - skulls mounted on the walls, weapons taken from defeated enemies, a crude throne made of bones. Maps and battle plans are scratched into the stone floor, showing the orc's ambitions extend beyond simple raiding. The war chief sizes you up with a brutal grin.",
                itemIds = listOf(ClassicFantasySeed.ORCISH_CLEAVER_ID, ClassicFantasySeed.MAJOR_HEALING_POTION_ID, ClassicFantasySeed.RUBY_GEMSTONE_ID),
                creatureIds = listOf(ClassicFantasySeed.ORC_BERSERKER_ID, ClassicFantasySeed.GOBLIN_WARRIOR_ID, ClassicFantasySeed.GOBLIN_WARRIOR_ID),
                exits = listOf(
                    Exit(locationId = WARREN_BARRACKS_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = WARREN_TRAP_CORRIDOR_ID, direction = ExitDirection.WEST),
                    Exit(locationId = WARREN_TROLL_LAIR_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -5,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = WARREN_TROLL_LAIR_ID,
                name = "Troll's Lair",
                desc = "The deepest chamber of the warren, home to the creature that the goblins truly fear - a massive troll. The cave reeks of rotting flesh, for the troll's meals lie scattered about in various states of decomposition. The beast itself lounges on a pile of bones and refuse, picking meat from a leg bone that might have been human. It rises with a hungry growl, gangly limbs unfolding to an terrifying height. 'More meat come to troll. Good. Troll always hungry.'",
                itemIds = listOf(
                    ClassicFantasySeed.RING_OF_REGEN_ID,
                    ClassicFantasySeed.GAUNTLETS_OGRE_POWER_ID,
                    ClassicFantasySeed.MAJOR_HEALING_POTION_ID,
                    ClassicFantasySeed.ANCIENT_GOLD_COIN_ID,
                    ClassicFantasySeed.ANCIENT_GOLD_COIN_ID,
                    ClassicFantasySeed.RUBY_GEMSTONE_ID
                ),
                creatureIds = listOf(ClassicFantasySeed.TROLL_ID),
                exits = listOf(
                    Exit(locationId = WARREN_TRAP_CORRIDOR_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = WARREN_ORC_QUARTERS_ID, direction = ExitDirection.EAST)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -6,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            )
        )

        locations.forEach { location ->
            if (LocationRepository.findById(location.id) == null) {
                LocationRepository.create(location)
            }
        }
    }
}
