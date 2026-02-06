package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for Classic Dungeon locations.
 * A classic dungeon crawl featuring iconic D&D monsters from ClassicDungeonSeed.
 *
 * Layout:
 *   - Cave entry with vermin (Giant Centipede, Stirge)
 *   - Owlbear den
 *   - Carrion Crawler tunnels
 *   - Gelatinous Cube corridor (classic trap!)
 *   - Spider caves with Ettercap
 *   - Deep areas with Umber Hulk, Hook Horror
 *   - Choice of two boss paths: Mind Flayer Sanctum OR Beholder's Eye
 */
object ClassicDungeonLocationsSeed {
    private val log = LoggerFactory.getLogger(ClassicDungeonLocationsSeed::class.java)

    // Attribution for tracking content source
    private const val ATTRIBUTION = "Classic D&D Dungeon"

    // Area ID for all dungeon locations
    const val AREA_ID = "classic-dungeon"

    // ============== LOCATION IDS ==============
    const val DUNGEON_ENTRY_CAVES_ID = "location-classic-dungeon-entry"
    const val DUNGEON_VERMIN_TUNNELS_ID = "location-classic-dungeon-vermin-tunnels"
    const val DUNGEON_STIRGE_NEST_ID = "location-classic-dungeon-stirge-nest"
    const val DUNGEON_OWLBEAR_DEN_ID = "location-classic-dungeon-owlbear-den"
    const val DUNGEON_CRAWLER_TUNNELS_ID = "location-classic-dungeon-crawler-tunnels"
    const val DUNGEON_CUBE_CORRIDOR_ID = "location-classic-dungeon-cube-corridor"
    const val DUNGEON_SPIDER_CAVES_ID = "location-classic-dungeon-spider-caves"
    const val DUNGEON_ETTERCAP_LAIR_ID = "location-classic-dungeon-ettercap-lair"
    const val DUNGEON_DEEP_TUNNELS_ID = "location-classic-dungeon-deep-tunnels"
    const val DUNGEON_HOOK_HORROR_NEST_ID = "location-classic-dungeon-hook-horror-nest"
    const val DUNGEON_UMBER_HULK_LAIR_ID = "location-classic-dungeon-umber-hulk-lair"
    const val DUNGEON_CENTRAL_HUB_ID = "location-classic-dungeon-central-hub"
    const val DUNGEON_MIND_FLAYER_SANCTUM_ID = "location-classic-dungeon-mind-flayer-sanctum"
    const val DUNGEON_BEHOLDER_EYE_ID = "location-classic-dungeon-beholder-eye"

    // Overworld connection point
    const val OVERWORLD_DUNGEON_ENTRANCE_ID = "location-overworld-ancient-ruins"

    fun seedIfEmpty() {
        val existingLocation = LocationRepository.findById(DUNGEON_ENTRY_CAVES_ID)
        if (existingLocation != null) return

        log.info("Seeding Classic Dungeon locations...")
        seedOverworldEntrance()
        seedDungeonLocations()
        log.info("Seeded Classic Dungeon locations")
    }

    private fun seedOverworldEntrance() {
        val ancientRuins = Location(
            id = OVERWORLD_DUNGEON_ENTRANCE_ID,
            name = "Ancient Ruins",
            desc = "Crumbling stone walls and toppled pillars mark what was once a great temple or fortress. Nature has reclaimed much of the structure, with vines crawling over weathered carvings of forgotten gods. In the center of the ruins, a set of stone steps descends into darkness - the entrance to dungeons that run deep beneath the earth. Scratch marks on the stairs hint at the creatures that emerge at night to hunt.",
            itemIds = emptyList(),
            creatureIds = listOf(ClassicDungeonSeed.GIANT_CENTIPEDE_ID),
            exits = listOf(
                Exit(locationId = DUNGEON_ENTRY_CAVES_ID, direction = ExitDirection.ENTER)
            ),
            featureIds = emptyList(),
            gridX = 8,
            gridY = -3,
            areaId = "overworld",
            locationType = LocationType.OUTDOOR_GROUND
        )

        if (LocationRepository.findById(ancientRuins.id) == null) {
            LocationRepository.create(ancientRuins)
        }
    }

    private fun seedDungeonLocations() {
        val locations = listOf(
            // ============== ENTRY LEVEL (Tier 1 encounters) ==============
            Location(
                id = DUNGEON_ENTRY_CAVES_ID,
                name = "Entry Caves",
                desc = "The dungeon begins with natural caves that have been expanded over the ages. Ancient chisel marks are visible where passages were widened. Moisture drips from the ceiling, creating slick patches on the uneven floor. Small creatures scuttle away from your light - centipedes the size of your arm, and things with too many legs to count. The air is thick with the smell of earth and decay.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicDungeonSeed.GIANT_CENTIPEDE_ID, ClassicDungeonSeed.GIANT_CENTIPEDE_ID),
                exits = listOf(
                    Exit(locationId = OVERWORLD_DUNGEON_ENTRANCE_ID, direction = ExitDirection.ENTER),
                    Exit(locationId = DUNGEON_VERMIN_TUNNELS_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = 0,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DUNGEON_VERMIN_TUNNELS_ID,
                name = "Vermin Tunnels",
                desc = "A maze of narrow tunnels infested with dungeon vermin. Giant centipedes have burrowed through the walls, creating a network of passages that connects throughout the upper dungeon. The floor is covered with shed exoskeletons and the desiccated husks of their prey. Something larger has been hunting here too - a rust monster's distinctive tracks are visible in the dust.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicDungeonSeed.GIANT_CENTIPEDE_ID, ClassicDungeonSeed.RUST_MONSTER_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_ENTRY_CAVES_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = DUNGEON_STIRGE_NEST_ID, direction = ExitDirection.WEST),
                    Exit(locationId = DUNGEON_OWLBEAR_DEN_ID, direction = ExitDirection.EAST),
                    Exit(locationId = DUNGEON_CRAWLER_TUNNELS_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DUNGEON_STIRGE_NEST_ID,
                name = "Stirge Nesting Cave",
                desc = "The ceiling of this cave is covered with leathery shapes - stirges hanging upside down like oversized bats. Their long proboscises twitch as they sense your body heat. The walls are stained with old blood from previous victims, and drained corpses lie scattered on the floor. The stirges stir, unfold their wings, and begin to descend with an eager, high-pitched whine.",
                itemIds = listOf(ClassicDungeonSeed.MACE_PLUS_1_ID),
                creatureIds = listOf(ClassicDungeonSeed.STIRGE_ID, ClassicDungeonSeed.STIRGE_ID, ClassicDungeonSeed.STIRGE_ID, ClassicDungeonSeed.STIRGE_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_VERMIN_TUNNELS_ID, direction = ExitDirection.EAST)
                ),
                featureIds = emptyList(),
                gridX = -1,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== MID LEVEL (Tier 2 encounters) ==============
            Location(
                id = DUNGEON_OWLBEAR_DEN_ID,
                name = "Owlbear Den",
                desc = "A large cave that has been claimed as a lair by an owlbear. The creature has decorated its home with the skulls and bones of its prey, arranged in patterns that suggest a disturbing intelligence. A nest of feathers, fur, and cloth scraps fills one corner. The owlbear itself watches you with its great owl eyes, hooting softly before letting loose its terrible screech and charging.",
                itemIds = listOf(ClassicDungeonSeed.OWLBEAR_FEATHER_ID, ClassicDungeonSeed.LONGSWORD_PLUS_1_ID, ClassicDungeonSeed.CHAINMAIL_PLUS_1_ID),
                creatureIds = listOf(ClassicDungeonSeed.OWLBEAR_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_VERMIN_TUNNELS_ID, direction = ExitDirection.WEST),
                    Exit(locationId = DUNGEON_DEEP_TUNNELS_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DUNGEON_CRAWLER_TUNNELS_ID,
                name = "Carrion Crawler Tunnels",
                desc = "These tunnels are coated with a slick, foul-smelling slime - the mucus trail of carrion crawlers. The creatures have claimed this section of the dungeon, feeding on anything that dies here and sometimes making kills of their own. Paralyzed victims lie along the walls, still alive but unable to move as the crawlers slowly devour them. Their eyes plead for mercy... or death.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicDungeonSeed.CARRION_CRAWLER_ID, ClassicDungeonSeed.CARRION_CRAWLER_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_VERMIN_TUNNELS_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = DUNGEON_CUBE_CORRIDOR_ID, direction = ExitDirection.WEST),
                    Exit(locationId = DUNGEON_SPIDER_CAVES_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DUNGEON_CUBE_CORRIDOR_ID,
                name = "Gelatinous Corridor",
                desc = "A suspiciously clean corridor - no dust, no debris, walls polished smooth. Too clean. Then you notice the skeletal hand floating in midair, and the rusty sword beside it, and the coins suspended at varying heights. The nearly-invisible gelatinous cube fills the entire corridor, slowly oozing toward you. Within its transparent mass, you can see the dissolving remains of previous adventurers and their equipment.",
                itemIds = listOf(ClassicDungeonSeed.GELATINOUS_RESIDUE_ID, ClassicDungeonSeed.RING_OF_PROTECTION_ID, ClassicDungeonSeed.BAG_OF_HOLDING_ID),
                creatureIds = listOf(ClassicDungeonSeed.GELATINOUS_CUBE_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_CRAWLER_TUNNELS_ID, direction = ExitDirection.EAST)
                ),
                featureIds = emptyList(),
                gridX = -1,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== SPIDER AREA (Tier 2-3 encounters) ==============
            Location(
                id = DUNGEON_SPIDER_CAVES_ID,
                name = "Spider-Infested Caves",
                desc = "Thick webs fill these caves, creating curtains and canopies that obscure your vision. Cocooned shapes hang from the ceiling - some humanoid, some not - slowly being drained by the spiders that rule here. The webs vibrate at your passage, sending signals to hunters hidden in the shadows. Multiple eyes reflect your light, drawing closer from every direction.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicFantasySeed.GIANT_SPIDER_ID, ClassicFantasySeed.GIANT_SPIDER_ID, ClassicFantasySeed.GIANT_SPIDER_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_CRAWLER_TUNNELS_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = DUNGEON_ETTERCAP_LAIR_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -3,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DUNGEON_ETTERCAP_LAIR_ID,
                name = "Ettercap's Lair",
                desc = "The heart of the spider infestation, where an ettercap makes its home. This spider-like humanoid has woven an elaborate web structure - a palace of silk with multiple levels and hidden chambers. Egg sacs hang from the ceiling, ready to hatch more spiders. The ettercap itself lurks at the center of its web, surrounded by its eight-legged children, waiting for prey to blunder into its trap.",
                itemIds = listOf(ClassicDungeonSeed.DAGGER_PLUS_2_ID, ClassicDungeonSeed.CLOAK_OF_PROTECTION_ID),
                creatureIds = listOf(ClassicDungeonSeed.ETTERCAP_ID, ClassicFantasySeed.GIANT_SPIDER_ID, ClassicFantasySeed.GIANT_SPIDER_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_SPIDER_CAVES_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = DUNGEON_CENTRAL_HUB_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -4,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== DEEP LEVEL (Tier 3 encounters) ==============
            Location(
                id = DUNGEON_DEEP_TUNNELS_ID,
                name = "Deep Tunnels",
                desc = "The passages here were not carved by tools - they were torn through solid rock by something with massive claws. The tunnel walls bear the distinctive marks of an umber hulk's excavation. The creature has created a maze of its own design, with unexpected turns and dead ends. Getting lost here is easy. Getting out... less so. You hear clicking sounds echoing from multiple directions.",
                itemIds = emptyList(),
                creatureIds = listOf(ClassicDungeonSeed.DISPLACER_BEAST_ID, ClassicDungeonSeed.HOOK_HORROR_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_OWLBEAR_DEN_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = DUNGEON_HOOK_HORROR_NEST_ID, direction = ExitDirection.EAST),
                    Exit(locationId = DUNGEON_UMBER_HULK_LAIR_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DUNGEON_HOOK_HORROR_NEST_ID,
                name = "Hook Horror Nest",
                desc = "A vast cavern with natural stone pillars rising to a ceiling lost in darkness. Hook horrors have made this place their home, using the pillars to climb and the echoing acoustics to communicate. Their clicking calls bounce off the walls in a confusing cacophony. The creatures move through the darkness with terrifying speed, their hook-like claws gleaming when they pass through what little light exists.",
                itemIds = listOf(ClassicDungeonSeed.BATTLEAXE_PLUS_2_ID, ClassicDungeonSeed.BRACERS_OF_DEFENSE_ID),
                creatureIds = listOf(ClassicDungeonSeed.HOOK_HORROR_ID, ClassicDungeonSeed.HOOK_HORROR_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_DEEP_TUNNELS_ID, direction = ExitDirection.WEST),
                    Exit(locationId = DUNGEON_CENTRAL_HUB_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 2,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DUNGEON_UMBER_HULK_LAIR_ID,
                name = "Umber Hulk Lair",
                desc = "The umber hulk's personal chamber - a wide cavern it has carved to its own specifications. The walls are smooth where the creature has polished them with its bulk over the years. Broken weapons and armor from previous challengers litter the floor. The umber hulk itself stands in the center, its multifaceted eyes catching your gaze. You feel your thoughts begin to scatter as its confusing gaze takes hold...",
                itemIds = listOf(ClassicDungeonSeed.PLATE_ARMOR_PLUS_1_ID, ClassicDungeonSeed.SHIELD_PLUS_2_ID),
                creatureIds = listOf(ClassicDungeonSeed.UMBER_HULK_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_DEEP_TUNNELS_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = DUNGEON_CENTRAL_HUB_ID, direction = ExitDirection.WEST)
                ),
                featureIds = emptyList(),
                gridX = 2,
                gridY = -3,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== CENTRAL HUB (Choice of boss paths) ==============
            Location(
                id = DUNGEON_CENTRAL_HUB_ID,
                name = "Central Hub",
                desc = "A crossroads in the deep dungeon where multiple paths converge. Ancient pillars carved with warnings in a dozen languages support the vaulted ceiling. Two passages lead to the deepest areas of the dungeon - one to the west, where an unnatural silence hangs heavy, and one to the east, where strange lights flicker. Both radiate an aura of powerful evil. Choose your doom carefully.",
                itemIds = emptyList(),
                creatureIds = emptyList(),
                exits = listOf(
                    Exit(locationId = DUNGEON_ETTERCAP_LAIR_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = DUNGEON_HOOK_HORROR_NEST_ID, direction = ExitDirection.EAST),
                    Exit(locationId = DUNGEON_UMBER_HULK_LAIR_ID, direction = ExitDirection.SOUTHEAST),
                    Exit(locationId = DUNGEON_MIND_FLAYER_SANCTUM_ID, direction = ExitDirection.WEST),
                    Exit(locationId = DUNGEON_BEHOLDER_EYE_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -4,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== BOSS CHAMBERS (Tier 4 encounters) ==============
            Location(
                id = DUNGEON_MIND_FLAYER_SANCTUM_ID,
                name = "Mind Flayer Sanctum",
                desc = "An alien chamber that defies conventional geometry. The walls curve in ways that hurt to look at, covered with carvings that seem to move when viewed from the corner of your eye. A pool of silvery liquid bubbles in the center, reflecting images of other worlds. The mind flayer floats before you, its tentacles writhing with anticipation. 'Your thoughts are... delicious,' it whispers directly into your mind. 'Your brain will make a fine meal.'",
                itemIds = listOf(
                    ClassicDungeonSeed.MIND_FLAYER_BRAIN_ID,
                    ClassicDungeonSeed.WAND_OF_MAGIC_MISSILES_ID,
                    ClassicDungeonSeed.AMULET_OF_PROOF_DETECTION_ID,
                    ClassicDungeonSeed.RING_OF_INVISIBILITY_ID,
                    ClassicDungeonSeed.IOUN_STONE_ID
                ),
                creatureIds = listOf(ClassicDungeonSeed.MIND_FLAYER_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_CENTRAL_HUB_ID, direction = ExitDirection.EAST)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -5,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DUNGEON_BEHOLDER_EYE_ID,
                name = "The Beholder's Eye",
                desc = "A perfectly spherical chamber carved by the beholder's disintegration ray. Every surface is polished to mirror smoothness, reflecting your image a thousand times. Treasure lies scattered across the floor - gold, gems, magic items - the beholder's hoard, arranged according to some system only it understands. The eye tyrant floats at the center, its central eye fixed on you while its eyestalks weave hypnotic patterns. 'You dare enter MY domain? I shall enjoy watching you die in creative ways.'",
                itemIds = listOf(
                    ClassicDungeonSeed.BEHOLDER_EYE_ID,
                    ClassicDungeonSeed.LONGSWORD_PLUS_3_ID,
                    ClassicDungeonSeed.BOOTS_OF_SPEED_ID,
                    ClassicDungeonSeed.RING_OF_INVISIBILITY_ID,
                    ClassicDungeonSeed.IOUN_STONE_ID,
                    ClassicDungeonSeed.IMMOVABLE_ROD_ID
                ),
                creatureIds = listOf(ClassicDungeonSeed.BEHOLDER_ID),
                exits = listOf(
                    Exit(locationId = DUNGEON_CENTRAL_HUB_ID, direction = ExitDirection.SOUTH)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -5,
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
