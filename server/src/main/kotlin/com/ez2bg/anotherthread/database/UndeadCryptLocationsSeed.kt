package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for Undead Crypt dungeon locations.
 * A multi-level crypt featuring progressive undead encounters from the UndeadCryptSeed creatures.
 *
 * Layout:
 *   - Entrance (from overworld)
 *   - Entry Crypt (weak undead)
 *   - Crypt Passages (branching paths)
 *   - Tomb Chambers (mid-tier undead)
 *   - Deep Crypt (powerful undead)
 *   - Vampire Lord's Sanctum (boss)
 */
object UndeadCryptLocationsSeed {
    private val log = LoggerFactory.getLogger(UndeadCryptLocationsSeed::class.java)

    // Attribution for tracking content source
    private const val ATTRIBUTION = "Classic D&D Undead Crypt"

    // Area ID for all crypt locations
    const val AREA_ID = "undead-crypt"

    // ============== LOCATION IDS ==============
    const val CRYPT_ENTRANCE_ID = "location-undead-crypt-entrance"
    const val CRYPT_VESTIBULE_ID = "location-undead-crypt-vestibule"
    const val CRYPT_WEST_PASSAGE_ID = "location-undead-crypt-west-passage"
    const val CRYPT_EAST_PASSAGE_ID = "location-undead-crypt-east-passage"
    const val TOMB_OF_THE_FALLEN_ID = "location-undead-crypt-tomb-fallen"
    const val OSSUARY_ID = "location-undead-crypt-ossuary"
    const val MUMMY_CHAMBER_ID = "location-undead-crypt-mummy-chamber"
    const val SKELETON_MAGE_STUDY_ID = "location-undead-crypt-mage-study"
    const val BONE_GOLEM_HALL_ID = "location-undead-crypt-golem-hall"
    const val DEATH_KNIGHT_VIGIL_ID = "location-undead-crypt-death-knight-vigil"
    const val VAMPIRE_SPAWN_LAIR_ID = "location-undead-crypt-vampire-spawn-lair"
    const val VAMPIRE_LORD_SANCTUM_ID = "location-undead-crypt-vampire-sanctum"

    // Overworld connection point (a graveyard or crypt entrance)
    const val OVERWORLD_GRAVEYARD_ID = "location-overworld-abandoned-graveyard"

    fun seedIfEmpty() {
        val existingLocation = LocationRepository.findById(CRYPT_ENTRANCE_ID)
        if (existingLocation != null) return

        log.info("Seeding Undead Crypt locations...")
        seedOverworldEntrance()
        seedCryptLocations()
        log.info("Seeded Undead Crypt locations")
    }

    private fun seedOverworldEntrance() {
        // Create an overworld graveyard that leads into the crypt
        val abandonedGraveyard = Location(
            id = OVERWORLD_GRAVEYARD_ID,
            name = "Abandoned Graveyard",
            desc = "A desolate graveyard surrounded by dead trees and crumbling stone walls. Moss-covered tombstones lean at odd angles, many bearing names worn away by centuries of neglect. In the center stands an ancient mausoleum, its iron doors hanging ajar, revealing steps leading down into darkness. The air is thick with the smell of decay, and an unnatural silence hangs over everything.",
            itemIds = emptyList(),
            creatureIds = listOf(UndeadCryptSeed.ZOMBIE_SHAMBLER_ID, UndeadCryptSeed.ZOMBIE_SHAMBLER_ID),
            exits = listOf(
                Exit(locationId = CRYPT_ENTRANCE_ID, direction = ExitDirection.ENTER)
            ),
            featureIds = emptyList(),
            gridX = 5,
            gridY = 5,
            areaId = "overworld",
            locationType = LocationType.OUTDOOR_GROUND
        )

        if (LocationRepository.findById(abandonedGraveyard.id) == null) {
            LocationRepository.create(abandonedGraveyard)
        }
    }

    private fun seedCryptLocations() {
        val locations = listOf(
            // ============== ENTRY LEVEL (Tier 1 encounters) ==============
            Location(
                id = CRYPT_ENTRANCE_ID,
                name = "Crypt Entrance",
                desc = "Stone steps descend into the darkness of the crypt. Cobwebs hang thick from ancient sconces that once held torches, now long extinguished. The walls are carved with faded reliefs depicting funeral processions and mourning figures. The air grows colder with each step downward, carrying whispers that might be the wind... or something else.",
                itemIds = emptyList(),
                creatureIds = listOf(UndeadCryptSeed.ZOMBIE_SHAMBLER_ID, UndeadCryptSeed.SKELETAL_ARCHER_ID),
                exits = listOf(
                    Exit(locationId = OVERWORLD_GRAVEYARD_ID, direction = ExitDirection.ENTER),
                    Exit(locationId = CRYPT_VESTIBULE_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = 0,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = CRYPT_VESTIBULE_ID,
                name = "Crypt Vestibule",
                desc = "A circular chamber with alcoves lining the walls, each containing a stone sarcophagus. Some of the lids have been pushed aside, revealing empty interiors - their former occupants now roam the crypt. A faded tapestry depicting a noble family crest hangs on the northern wall, partially obscuring passages leading east and west. The central floor bears a mosaic of a skull surrounded by roses.",
                itemIds = emptyList(),
                creatureIds = listOf(UndeadCryptSeed.SKELETAL_ARCHER_ID, UndeadCryptSeed.GHAST_ID),
                exits = listOf(
                    Exit(locationId = CRYPT_ENTRANCE_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = CRYPT_WEST_PASSAGE_ID, direction = ExitDirection.WEST),
                    Exit(locationId = CRYPT_EAST_PASSAGE_ID, direction = ExitDirection.EAST)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== MID LEVEL - WEST BRANCH (Tier 2 encounters) ==============
            Location(
                id = CRYPT_WEST_PASSAGE_ID,
                name = "Western Passage",
                desc = "A long corridor with burial niches carved into the walls at regular intervals. Many of the niches are empty, their occupants disturbed. The floor is littered with bone fragments and tattered burial shrouds. Halfway down the passage, you notice scratches on the walls - claw marks from something that was sealed in here and desperately wanted out.",
                itemIds = emptyList(),
                creatureIds = listOf(UndeadCryptSeed.WIGHT_ID, UndeadCryptSeed.ZOMBIE_SHAMBLER_ID),
                exits = listOf(
                    Exit(locationId = CRYPT_VESTIBULE_ID, direction = ExitDirection.EAST),
                    Exit(locationId = TOMB_OF_THE_FALLEN_ID, direction = ExitDirection.WEST),
                    Exit(locationId = OSSUARY_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = -1,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = TOMB_OF_THE_FALLEN_ID,
                name = "Tomb of the Fallen Knights",
                desc = "A grand chamber dedicated to warriors who fell in some ancient battle. Stone effigies of armored knights lie atop ornate sarcophagi, their stone swords crossed over their chests. The walls bear shields and rusted weapons - trophies from their final campaigns. In the center, an eternal flame flickers in a bronze brazier, casting dancing shadows that make the stone knights seem to move.",
                itemIds = listOf(UndeadCryptSeed.SILVER_LONGSWORD_ID),
                creatureIds = listOf(UndeadCryptSeed.WIGHT_ID, UndeadCryptSeed.WIGHT_ID, UndeadCryptSeed.SPECTER_ID),
                exits = listOf(
                    Exit(locationId = CRYPT_WEST_PASSAGE_ID, direction = ExitDirection.EAST),
                    Exit(locationId = DEATH_KNIGHT_VIGIL_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = -2,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = OSSUARY_ID,
                name = "The Ossuary",
                desc = "A chamber whose walls are entirely constructed from human bones - femurs stacked in neat rows, skulls arranged in decorative patterns, ribcages forming archways. The craftsmanship is disturbingly artistic, creating chandeliers of vertebrae and pillars of interlocking bones. The room seems to breathe with malevolent energy, and you cannot shake the feeling that every empty eye socket is watching you.",
                itemIds = listOf(UndeadCryptSeed.BLESSED_BANDAGE_ID),
                creatureIds = listOf(UndeadCryptSeed.SKELETON_MAGE_ID, UndeadCryptSeed.GHAST_ID, UndeadCryptSeed.GHAST_ID),
                exits = listOf(
                    Exit(locationId = CRYPT_WEST_PASSAGE_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = BONE_GOLEM_HALL_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = -1,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== MID LEVEL - EAST BRANCH (Tier 2-3 encounters) ==============
            Location(
                id = CRYPT_EAST_PASSAGE_ID,
                name = "Eastern Passage",
                desc = "This corridor is colder than the rest of the crypt, and frost clings to the walls in strange patterns. The burial niches here are sealed with wax stamps bearing arcane symbols - wards against the undead that have clearly failed. Shadows seem to move independently of any light source, sliding along the walls with predatory intent.",
                itemIds = emptyList(),
                creatureIds = listOf(UndeadCryptSeed.SPECTER_ID, UndeadCryptSeed.SPECTER_ID),
                exits = listOf(
                    Exit(locationId = CRYPT_VESTIBULE_ID, direction = ExitDirection.WEST),
                    Exit(locationId = MUMMY_CHAMBER_ID, direction = ExitDirection.EAST),
                    Exit(locationId = SKELETON_MAGE_STUDY_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = MUMMY_CHAMBER_ID,
                name = "Chamber of the Mummified Pharaoh",
                desc = "An incongruous chamber decorated in the style of an ancient desert kingdom. Hieroglyphs cover every surface, telling tales of a pharaoh who sought immortality through dark bargains. Canopic jars line the shelves, and a golden sarcophagus stands upright against the far wall, its painted face stern and judgmental. The air is dry and smells of ancient spices and decay.",
                itemIds = listOf(UndeadCryptSeed.ANCIENT_TOMB_GOLD_ID, UndeadCryptSeed.HOLY_WATER_VIAL_ID),
                creatureIds = listOf(UndeadCryptSeed.MUMMY_ID, UndeadCryptSeed.GHAST_ID),
                exits = listOf(
                    Exit(locationId = CRYPT_EAST_PASSAGE_ID, direction = ExitDirection.WEST),
                    Exit(locationId = VAMPIRE_SPAWN_LAIR_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 2,
                gridY = -1,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = SKELETON_MAGE_STUDY_ID,
                name = "Necromancer's Study",
                desc = "Once the laboratory of a powerful necromancer, this chamber is filled with the tools of dark research. Shelves hold jars containing preserved organs and strange specimens. A large desk is covered with yellowed parchments covered in arcane formulae. Ritual circles are etched into the floor, still glowing faintly with residual energy. Something continues the necromancer's work...",
                itemIds = listOf(UndeadCryptSeed.RESTORATION_ELIXIR_ID, UndeadCryptSeed.TURN_UNDEAD_SCROLL_ID),
                creatureIds = listOf(UndeadCryptSeed.SKELETON_MAGE_ID, UndeadCryptSeed.SKELETAL_ARCHER_ID, UndeadCryptSeed.SKELETAL_ARCHER_ID),
                exits = listOf(
                    Exit(locationId = CRYPT_EAST_PASSAGE_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = BONE_GOLEM_HALL_ID, direction = ExitDirection.WEST)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== DEEP LEVEL (Tier 3-4 encounters) ==============
            Location(
                id = BONE_GOLEM_HALL_ID,
                name = "Hall of the Bone Construct",
                desc = "A massive hall with vaulted ceilings supported by pillars of fused bone. The floor is scattered with the remnants of failed experiments - half-formed skeletal constructs and piles of rejected bones. In the center of the hall stands a towering monstrosity made of thousands of bones, its eyeless skull turning to track any movement. The walls bear instructions for its creation in cramped, mad handwriting.",
                itemIds = listOf(UndeadCryptSeed.MACE_OF_DISRUPTION_ID),
                creatureIds = listOf(UndeadCryptSeed.BONE_GOLEM_ID, UndeadCryptSeed.SKELETON_MAGE_ID),
                exits = listOf(
                    Exit(locationId = OSSUARY_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = SKELETON_MAGE_STUDY_ID, direction = ExitDirection.EAST),
                    Exit(locationId = DEATH_KNIGHT_VIGIL_ID, direction = ExitDirection.WEST),
                    Exit(locationId = VAMPIRE_SPAWN_LAIR_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = DEATH_KNIGHT_VIGIL_ID,
                name = "The Death Knight's Vigil",
                desc = "A chamber that was once a chapel, now corrupted beyond recognition. Pews of blackened wood face an altar that drips with congealed blood. Stained glass windows depicting holy scenes have been shattered and replaced with depictions of death and torment. Before the altar stands a figure in blackened plate armor, a cursed sword in its gauntleted hand. It turns slowly, flames flickering in its empty helm.",
                itemIds = listOf(UndeadCryptSeed.DEATH_KNIGHTS_BLADE_ID),
                creatureIds = listOf(UndeadCryptSeed.DEATH_KNIGHT_ID, UndeadCryptSeed.WIGHT_ID),
                exits = listOf(
                    Exit(locationId = TOMB_OF_THE_FALLEN_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = BONE_GOLEM_HALL_ID, direction = ExitDirection.EAST),
                    Exit(locationId = VAMPIRE_LORD_SANCTUM_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = -1,
                gridY = -2,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            Location(
                id = VAMPIRE_SPAWN_LAIR_ID,
                name = "Lair of the Spawn",
                desc = "A chamber filled with velvet-draped alcoves, each containing a silk-lined coffin. The spawn of the Vampire Lord rest here during the day, rising at night to hunt. The walls are decorated with portraits of beautiful nobles - perhaps the spawn's former identities, or perhaps their victims. Wine bottles line the shelves, but the dark liquid within is no vintage you would want to taste.",
                itemIds = listOf(UndeadCryptSeed.CLOAK_OF_SHADOWS_ID, UndeadCryptSeed.VAMPIRE_FANG_ID),
                creatureIds = listOf(UndeadCryptSeed.VAMPIRE_SPAWN_ID, UndeadCryptSeed.VAMPIRE_SPAWN_ID),
                exits = listOf(
                    Exit(locationId = MUMMY_CHAMBER_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = BONE_GOLEM_HALL_ID, direction = ExitDirection.WEST),
                    Exit(locationId = VAMPIRE_LORD_SANCTUM_ID, direction = ExitDirection.NORTH)
                ),
                featureIds = emptyList(),
                gridX = 1,
                gridY = -3,
                areaId = AREA_ID,
                locationType = LocationType.UNDERGROUND
            ),

            // ============== BOSS CHAMBER (Tier 4) ==============
            Location(
                id = VAMPIRE_LORD_SANCTUM_ID,
                name = "Sanctum of the Vampire Lord",
                desc = "The innermost chamber of the crypt, a grand hall befitting an ancient noble. Crimson carpets lead to a throne of black iron, upon which sits the master of this domain. Candelabras hold blood-red candles that cast dancing shadows. Portraits of the Vampire Lord through the ages line the walls, each showing the same ageless face. A massive coffin of obsidian and silver rests behind the throne. The Lord rises, ancient eyes gleaming with hunger and malice. 'You have come far, mortal. Your blood will be... exquisite.'",
                itemIds = listOf(
                    UndeadCryptSeed.HOLY_AVENGER_ID,
                    UndeadCryptSeed.AMULET_OF_LIFE_PROTECTION_ID,
                    UndeadCryptSeed.PHYLACTERY_SHARD_ID,
                    UndeadCryptSeed.SOUL_GEM_ID
                ),
                creatureIds = listOf(UndeadCryptSeed.VAMPIRE_LORD_ID),
                exits = listOf(
                    Exit(locationId = DEATH_KNIGHT_VIGIL_ID, direction = ExitDirection.SOUTH),
                    Exit(locationId = VAMPIRE_SPAWN_LAIR_ID, direction = ExitDirection.EAST)
                ),
                featureIds = emptyList(),
                gridX = 0,
                gridY = -3,
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
