package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Validation tests for seed data integrity.
 *
 * These tests verify:
 * 1. All referenced IDs (creatures, items, abilities) exist
 * 2. Location exits are bidirectional
 * 3. All locations are reachable from entry points
 * 4. No duplicate IDs
 * 5. Creature tiers are appropriate for location depth
 */
class SeedValidationTest : BaseApplicationTest() {

    // ========================================================================
    // Content Seed Validation (creatures, items, abilities, loot)
    // ========================================================================

    @Test
    fun `ClassicFantasySeed - all creature ability references exist`() {
        ClassicFantasySeed.seedIfEmpty()

        val creatureIds = listOf(
            ClassicFantasySeed.GOBLIN_WARRIOR_ID,
            ClassicFantasySeed.GIANT_RAT_ID,
            ClassicFantasySeed.SKELETON_WARRIOR_ID,
            ClassicFantasySeed.KOBOLD_TRAPPER_ID,
            ClassicFantasySeed.GIANT_SPIDER_ID,
            ClassicFantasySeed.ORC_BERSERKER_ID,
            ClassicFantasySeed.ZOMBIE_BRUTE_ID,
            ClassicFantasySeed.GHOUL_ID,
            ClassicFantasySeed.HOBGOBLIN_CAPTAIN_ID,
            ClassicFantasySeed.OCHRE_JELLY_ID,
            ClassicFantasySeed.TROLL_ID,
            ClassicFantasySeed.WRAITH_ID,
            ClassicFantasySeed.MINOTAUR_ID,
            ClassicFantasySeed.BASILISK_ID,
            ClassicFantasySeed.YOUNG_RED_DRAGON_ID
        )

        validateCreatureAbilitiesExist(creatureIds, "ClassicFantasySeed")
    }

    @Test
    fun `UndeadCryptSeed - all creature ability references exist`() {
        UndeadCryptSeed.seedIfEmpty()

        val creatureIds = listOf(
            UndeadCryptSeed.ZOMBIE_SHAMBLER_ID,
            UndeadCryptSeed.SKELETAL_ARCHER_ID,
            UndeadCryptSeed.GHAST_ID,
            UndeadCryptSeed.WIGHT_ID,
            UndeadCryptSeed.SPECTER_ID,
            UndeadCryptSeed.MUMMY_ID,
            UndeadCryptSeed.SKELETON_MAGE_ID,
            UndeadCryptSeed.BONE_GOLEM_ID,
            UndeadCryptSeed.VAMPIRE_SPAWN_ID,
            UndeadCryptSeed.DEATH_KNIGHT_ID,
            UndeadCryptSeed.VAMPIRE_LORD_ID
        )

        validateCreatureAbilitiesExist(creatureIds, "UndeadCryptSeed")
    }

    @Test
    fun `ElementalChaosSeed - all creature ability references exist`() {
        ElementalChaosSeed.seedIfEmpty()

        val creatureIds = listOf(
            ElementalChaosSeed.FIRE_MEPHIT_ID,
            ElementalChaosSeed.ICE_MEPHIT_ID,
            ElementalChaosSeed.DUST_MEPHIT_ID,
            ElementalChaosSeed.STEAM_MEPHIT_ID,
            ElementalChaosSeed.FIRE_ELEMENTAL_ID,
            ElementalChaosSeed.WATER_ELEMENTAL_ID,
            ElementalChaosSeed.EARTH_ELEMENTAL_ID,
            ElementalChaosSeed.AIR_ELEMENTAL_ID,
            ElementalChaosSeed.MAGMA_ELEMENTAL_ID,
            ElementalChaosSeed.ICE_ELEMENTAL_ID,
            ElementalChaosSeed.MUD_ELEMENTAL_ID,
            ElementalChaosSeed.LIGHTNING_ELEMENTAL_ID,
            ElementalChaosSeed.ELEMENTAL_PRINCE_ID
        )

        validateCreatureAbilitiesExist(creatureIds, "ElementalChaosSeed")
    }

    @Test
    fun `ClassicDungeonSeed - all creature ability references exist`() {
        ClassicDungeonSeed.seedIfEmpty()

        val creatureIds = listOf(
            ClassicDungeonSeed.GIANT_CENTIPEDE_ID,
            ClassicDungeonSeed.STIRGE_ID,
            ClassicDungeonSeed.RUST_MONSTER_ID,
            ClassicDungeonSeed.OWLBEAR_ID,
            ClassicDungeonSeed.CARRION_CRAWLER_ID,
            ClassicDungeonSeed.GELATINOUS_CUBE_ID,
            ClassicDungeonSeed.DISPLACER_BEAST_ID,
            ClassicDungeonSeed.ETTERCAP_ID,
            ClassicDungeonSeed.UMBER_HULK_ID,
            ClassicDungeonSeed.HOOK_HORROR_ID,
            ClassicDungeonSeed.MIND_FLAYER_ID,
            ClassicDungeonSeed.BEHOLDER_ID
        )

        validateCreatureAbilitiesExist(creatureIds, "ClassicDungeonSeed")
    }

    // ========================================================================
    // Location Seed Validation
    // ========================================================================

    @Test
    fun `UndeadCryptLocationsSeed - all creature references exist`() {
        // Ensure content seeds are loaded first
        UndeadCryptSeed.seedIfEmpty()
        UndeadCryptLocationsSeed.seedIfEmpty()

        val locationIds = listOf(
            UndeadCryptLocationsSeed.OVERWORLD_GRAVEYARD_ID,
            UndeadCryptLocationsSeed.CRYPT_ENTRANCE_ID,
            UndeadCryptLocationsSeed.CRYPT_VESTIBULE_ID,
            UndeadCryptLocationsSeed.CRYPT_WEST_PASSAGE_ID,
            UndeadCryptLocationsSeed.CRYPT_EAST_PASSAGE_ID,
            UndeadCryptLocationsSeed.TOMB_OF_THE_FALLEN_ID,
            UndeadCryptLocationsSeed.OSSUARY_ID,
            UndeadCryptLocationsSeed.MUMMY_CHAMBER_ID,
            UndeadCryptLocationsSeed.SKELETON_MAGE_STUDY_ID,
            UndeadCryptLocationsSeed.BONE_GOLEM_HALL_ID,
            UndeadCryptLocationsSeed.DEATH_KNIGHT_VIGIL_ID,
            UndeadCryptLocationsSeed.VAMPIRE_SPAWN_LAIR_ID,
            UndeadCryptLocationsSeed.VAMPIRE_LORD_SANCTUM_ID
        )

        validateLocationCreaturesExist(locationIds, "UndeadCryptLocationsSeed")
    }

    @Test
    fun `UndeadCryptLocationsSeed - exits are bidirectional`() {
        UndeadCryptSeed.seedIfEmpty()
        UndeadCryptLocationsSeed.seedIfEmpty()

        val locationIds = listOf(
            UndeadCryptLocationsSeed.CRYPT_ENTRANCE_ID,
            UndeadCryptLocationsSeed.CRYPT_VESTIBULE_ID,
            UndeadCryptLocationsSeed.CRYPT_WEST_PASSAGE_ID,
            UndeadCryptLocationsSeed.CRYPT_EAST_PASSAGE_ID,
            UndeadCryptLocationsSeed.TOMB_OF_THE_FALLEN_ID,
            UndeadCryptLocationsSeed.OSSUARY_ID,
            UndeadCryptLocationsSeed.MUMMY_CHAMBER_ID,
            UndeadCryptLocationsSeed.SKELETON_MAGE_STUDY_ID,
            UndeadCryptLocationsSeed.BONE_GOLEM_HALL_ID,
            UndeadCryptLocationsSeed.DEATH_KNIGHT_VIGIL_ID,
            UndeadCryptLocationsSeed.VAMPIRE_SPAWN_LAIR_ID,
            UndeadCryptLocationsSeed.VAMPIRE_LORD_SANCTUM_ID
        )

        validateBidirectionalExits(locationIds, "UndeadCryptLocationsSeed")
    }

    @Test
    fun `UndeadCryptLocationsSeed - all locations reachable from entrance`() {
        UndeadCryptSeed.seedIfEmpty()
        UndeadCryptLocationsSeed.seedIfEmpty()

        val allLocationIds = listOf(
            UndeadCryptLocationsSeed.CRYPT_ENTRANCE_ID,
            UndeadCryptLocationsSeed.CRYPT_VESTIBULE_ID,
            UndeadCryptLocationsSeed.CRYPT_WEST_PASSAGE_ID,
            UndeadCryptLocationsSeed.CRYPT_EAST_PASSAGE_ID,
            UndeadCryptLocationsSeed.TOMB_OF_THE_FALLEN_ID,
            UndeadCryptLocationsSeed.OSSUARY_ID,
            UndeadCryptLocationsSeed.MUMMY_CHAMBER_ID,
            UndeadCryptLocationsSeed.SKELETON_MAGE_STUDY_ID,
            UndeadCryptLocationsSeed.BONE_GOLEM_HALL_ID,
            UndeadCryptLocationsSeed.DEATH_KNIGHT_VIGIL_ID,
            UndeadCryptLocationsSeed.VAMPIRE_SPAWN_LAIR_ID,
            UndeadCryptLocationsSeed.VAMPIRE_LORD_SANCTUM_ID
        )

        validateAllLocationsReachable(
            entryLocationId = UndeadCryptLocationsSeed.CRYPT_ENTRANCE_ID,
            allLocationIds = allLocationIds,
            seedName = "UndeadCryptLocationsSeed"
        )
    }

    @Test
    fun `GoblinWarrenLocationsSeed - all creature references exist`() {
        ClassicFantasySeed.seedIfEmpty()
        GoblinWarrenLocationsSeed.seedIfEmpty()

        val locationIds = listOf(
            GoblinWarrenLocationsSeed.OVERWORLD_CAVE_ENTRANCE_ID,
            GoblinWarrenLocationsSeed.WARREN_CAVE_ENTRANCE_ID,
            GoblinWarrenLocationsSeed.WARREN_RAT_TUNNELS_ID,
            GoblinWarrenLocationsSeed.WARREN_RAT_NEST_ID,
            GoblinWarrenLocationsSeed.WARREN_GUARD_POST_ID,
            GoblinWarrenLocationsSeed.WARREN_MAIN_CAVERN_ID,
            GoblinWarrenLocationsSeed.WARREN_BARRACKS_ID,
            GoblinWarrenLocationsSeed.WARREN_STOREROOM_ID,
            GoblinWarrenLocationsSeed.WARREN_KOBOLD_TUNNELS_ID,
            GoblinWarrenLocationsSeed.WARREN_TRAP_CORRIDOR_ID,
            GoblinWarrenLocationsSeed.WARREN_ORC_QUARTERS_ID,
            GoblinWarrenLocationsSeed.WARREN_TROLL_LAIR_ID
        )

        validateLocationCreaturesExist(locationIds, "GoblinWarrenLocationsSeed")
    }

    @Test
    fun `GoblinWarrenLocationsSeed - exits are bidirectional`() {
        ClassicFantasySeed.seedIfEmpty()
        GoblinWarrenLocationsSeed.seedIfEmpty()

        val locationIds = listOf(
            GoblinWarrenLocationsSeed.WARREN_CAVE_ENTRANCE_ID,
            GoblinWarrenLocationsSeed.WARREN_RAT_TUNNELS_ID,
            GoblinWarrenLocationsSeed.WARREN_RAT_NEST_ID,
            GoblinWarrenLocationsSeed.WARREN_GUARD_POST_ID,
            GoblinWarrenLocationsSeed.WARREN_MAIN_CAVERN_ID,
            GoblinWarrenLocationsSeed.WARREN_BARRACKS_ID,
            GoblinWarrenLocationsSeed.WARREN_STOREROOM_ID,
            GoblinWarrenLocationsSeed.WARREN_KOBOLD_TUNNELS_ID,
            GoblinWarrenLocationsSeed.WARREN_TRAP_CORRIDOR_ID,
            GoblinWarrenLocationsSeed.WARREN_ORC_QUARTERS_ID,
            GoblinWarrenLocationsSeed.WARREN_TROLL_LAIR_ID
        )

        validateBidirectionalExits(locationIds, "GoblinWarrenLocationsSeed")
    }

    @Test
    fun `GoblinWarrenLocationsSeed - all locations reachable from entrance`() {
        ClassicFantasySeed.seedIfEmpty()
        GoblinWarrenLocationsSeed.seedIfEmpty()

        val allLocationIds = listOf(
            GoblinWarrenLocationsSeed.WARREN_CAVE_ENTRANCE_ID,
            GoblinWarrenLocationsSeed.WARREN_RAT_TUNNELS_ID,
            GoblinWarrenLocationsSeed.WARREN_RAT_NEST_ID,
            GoblinWarrenLocationsSeed.WARREN_GUARD_POST_ID,
            GoblinWarrenLocationsSeed.WARREN_MAIN_CAVERN_ID,
            GoblinWarrenLocationsSeed.WARREN_BARRACKS_ID,
            GoblinWarrenLocationsSeed.WARREN_STOREROOM_ID,
            GoblinWarrenLocationsSeed.WARREN_KOBOLD_TUNNELS_ID,
            GoblinWarrenLocationsSeed.WARREN_TRAP_CORRIDOR_ID,
            GoblinWarrenLocationsSeed.WARREN_ORC_QUARTERS_ID,
            GoblinWarrenLocationsSeed.WARREN_TROLL_LAIR_ID
        )

        validateAllLocationsReachable(
            entryLocationId = GoblinWarrenLocationsSeed.WARREN_CAVE_ENTRANCE_ID,
            allLocationIds = allLocationIds,
            seedName = "GoblinWarrenLocationsSeed"
        )
    }

    @Test
    fun `ClassicDungeonLocationsSeed - all creature references exist`() {
        ClassicDungeonSeed.seedIfEmpty()
        ClassicFantasySeed.seedIfEmpty() // For Giant Spider
        ClassicDungeonLocationsSeed.seedIfEmpty()

        val locationIds = listOf(
            ClassicDungeonLocationsSeed.OVERWORLD_DUNGEON_ENTRANCE_ID,
            ClassicDungeonLocationsSeed.DUNGEON_ENTRY_CAVES_ID,
            ClassicDungeonLocationsSeed.DUNGEON_VERMIN_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_STIRGE_NEST_ID,
            ClassicDungeonLocationsSeed.DUNGEON_OWLBEAR_DEN_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CRAWLER_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CUBE_CORRIDOR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_SPIDER_CAVES_ID,
            ClassicDungeonLocationsSeed.DUNGEON_ETTERCAP_LAIR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_DEEP_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_HOOK_HORROR_NEST_ID,
            ClassicDungeonLocationsSeed.DUNGEON_UMBER_HULK_LAIR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CENTRAL_HUB_ID,
            ClassicDungeonLocationsSeed.DUNGEON_MIND_FLAYER_SANCTUM_ID,
            ClassicDungeonLocationsSeed.DUNGEON_BEHOLDER_EYE_ID
        )

        validateLocationCreaturesExist(locationIds, "ClassicDungeonLocationsSeed")
    }

    @Test
    fun `ClassicDungeonLocationsSeed - exits are bidirectional`() {
        ClassicDungeonSeed.seedIfEmpty()
        ClassicFantasySeed.seedIfEmpty()
        ClassicDungeonLocationsSeed.seedIfEmpty()

        val locationIds = listOf(
            ClassicDungeonLocationsSeed.DUNGEON_ENTRY_CAVES_ID,
            ClassicDungeonLocationsSeed.DUNGEON_VERMIN_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_STIRGE_NEST_ID,
            ClassicDungeonLocationsSeed.DUNGEON_OWLBEAR_DEN_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CRAWLER_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CUBE_CORRIDOR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_SPIDER_CAVES_ID,
            ClassicDungeonLocationsSeed.DUNGEON_ETTERCAP_LAIR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_DEEP_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_HOOK_HORROR_NEST_ID,
            ClassicDungeonLocationsSeed.DUNGEON_UMBER_HULK_LAIR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CENTRAL_HUB_ID,
            ClassicDungeonLocationsSeed.DUNGEON_MIND_FLAYER_SANCTUM_ID,
            ClassicDungeonLocationsSeed.DUNGEON_BEHOLDER_EYE_ID
        )

        validateBidirectionalExits(locationIds, "ClassicDungeonLocationsSeed")
    }

    @Test
    fun `ClassicDungeonLocationsSeed - all locations reachable from entrance`() {
        ClassicDungeonSeed.seedIfEmpty()
        ClassicFantasySeed.seedIfEmpty()
        ClassicDungeonLocationsSeed.seedIfEmpty()

        val allLocationIds = listOf(
            ClassicDungeonLocationsSeed.DUNGEON_ENTRY_CAVES_ID,
            ClassicDungeonLocationsSeed.DUNGEON_VERMIN_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_STIRGE_NEST_ID,
            ClassicDungeonLocationsSeed.DUNGEON_OWLBEAR_DEN_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CRAWLER_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CUBE_CORRIDOR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_SPIDER_CAVES_ID,
            ClassicDungeonLocationsSeed.DUNGEON_ETTERCAP_LAIR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_DEEP_TUNNELS_ID,
            ClassicDungeonLocationsSeed.DUNGEON_HOOK_HORROR_NEST_ID,
            ClassicDungeonLocationsSeed.DUNGEON_UMBER_HULK_LAIR_ID,
            ClassicDungeonLocationsSeed.DUNGEON_CENTRAL_HUB_ID,
            ClassicDungeonLocationsSeed.DUNGEON_MIND_FLAYER_SANCTUM_ID,
            ClassicDungeonLocationsSeed.DUNGEON_BEHOLDER_EYE_ID
        )

        validateAllLocationsReachable(
            entryLocationId = ClassicDungeonLocationsSeed.DUNGEON_ENTRY_CAVES_ID,
            allLocationIds = allLocationIds,
            seedName = "ClassicDungeonLocationsSeed"
        )
    }

    // ========================================================================
    // Cross-Seed Validation
    // ========================================================================

    @Test
    fun `no duplicate creature IDs across all content seeds`() {
        // Seed all content
        ClassicFantasySeed.seedIfEmpty()
        UndeadCryptSeed.seedIfEmpty()
        ElementalChaosSeed.seedIfEmpty()
        ClassicDungeonSeed.seedIfEmpty()
        FungusForestSeed.seedIfEmpty()

        val allCreatureIds = mutableListOf<String>()

        // Gather all creature IDs
        allCreatureIds.addAll(listOf(
            ClassicFantasySeed.GOBLIN_WARRIOR_ID, ClassicFantasySeed.GIANT_RAT_ID,
            ClassicFantasySeed.SKELETON_WARRIOR_ID, ClassicFantasySeed.KOBOLD_TRAPPER_ID,
            ClassicFantasySeed.GIANT_SPIDER_ID, ClassicFantasySeed.ORC_BERSERKER_ID,
            ClassicFantasySeed.ZOMBIE_BRUTE_ID, ClassicFantasySeed.GHOUL_ID,
            ClassicFantasySeed.HOBGOBLIN_CAPTAIN_ID, ClassicFantasySeed.OCHRE_JELLY_ID,
            ClassicFantasySeed.TROLL_ID, ClassicFantasySeed.WRAITH_ID,
            ClassicFantasySeed.MINOTAUR_ID, ClassicFantasySeed.BASILISK_ID,
            ClassicFantasySeed.YOUNG_RED_DRAGON_ID
        ))

        allCreatureIds.addAll(listOf(
            UndeadCryptSeed.ZOMBIE_SHAMBLER_ID, UndeadCryptSeed.SKELETAL_ARCHER_ID,
            UndeadCryptSeed.GHAST_ID, UndeadCryptSeed.WIGHT_ID, UndeadCryptSeed.SPECTER_ID,
            UndeadCryptSeed.MUMMY_ID, UndeadCryptSeed.SKELETON_MAGE_ID,
            UndeadCryptSeed.BONE_GOLEM_ID, UndeadCryptSeed.VAMPIRE_SPAWN_ID,
            UndeadCryptSeed.DEATH_KNIGHT_ID, UndeadCryptSeed.VAMPIRE_LORD_ID
        ))

        allCreatureIds.addAll(listOf(
            ElementalChaosSeed.FIRE_MEPHIT_ID, ElementalChaosSeed.ICE_MEPHIT_ID,
            ElementalChaosSeed.DUST_MEPHIT_ID, ElementalChaosSeed.STEAM_MEPHIT_ID,
            ElementalChaosSeed.FIRE_ELEMENTAL_ID, ElementalChaosSeed.WATER_ELEMENTAL_ID,
            ElementalChaosSeed.EARTH_ELEMENTAL_ID, ElementalChaosSeed.AIR_ELEMENTAL_ID,
            ElementalChaosSeed.MAGMA_ELEMENTAL_ID, ElementalChaosSeed.ICE_ELEMENTAL_ID,
            ElementalChaosSeed.MUD_ELEMENTAL_ID, ElementalChaosSeed.LIGHTNING_ELEMENTAL_ID,
            ElementalChaosSeed.ELEMENTAL_PRINCE_ID
        ))

        allCreatureIds.addAll(listOf(
            ClassicDungeonSeed.GIANT_CENTIPEDE_ID, ClassicDungeonSeed.STIRGE_ID,
            ClassicDungeonSeed.RUST_MONSTER_ID, ClassicDungeonSeed.OWLBEAR_ID,
            ClassicDungeonSeed.CARRION_CRAWLER_ID, ClassicDungeonSeed.GELATINOUS_CUBE_ID,
            ClassicDungeonSeed.DISPLACER_BEAST_ID, ClassicDungeonSeed.ETTERCAP_ID,
            ClassicDungeonSeed.UMBER_HULK_ID, ClassicDungeonSeed.HOOK_HORROR_ID,
            ClassicDungeonSeed.MIND_FLAYER_ID, ClassicDungeonSeed.BEHOLDER_ID
        ))

        allCreatureIds.addAll(listOf(
            FungusForestSeed.SPORE_SHAMBLER_ID, FungusForestSeed.MYCONID_SCOUT_ID,
            FungusForestSeed.GAUNT_ONE_ELDER_ID
        ))

        val duplicates = allCreatureIds.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "Duplicate creature IDs found: $duplicates")
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun validateCreatureAbilitiesExist(creatureIds: List<String>, seedName: String) {
        val missingAbilities = mutableListOf<String>()

        creatureIds.forEach { creatureId ->
            val creature = CreatureRepository.findById(creatureId)
            if (creature == null) {
                fail("$seedName: Creature not found: $creatureId")
            }
            creature.abilityIds.forEach { abilityId ->
                val ability = AbilityRepository.findById(abilityId)
                if (ability == null) {
                    missingAbilities.add("$creatureId -> $abilityId")
                }
            }
        }

        assertTrue(
            missingAbilities.isEmpty(),
            "$seedName: Missing ability references:\n${missingAbilities.joinToString("\n")}"
        )
    }

    private fun validateLocationCreaturesExist(locationIds: List<String>, seedName: String) {
        val missingCreatures = mutableListOf<String>()

        locationIds.forEach { locationId ->
            val location = LocationRepository.findById(locationId)
            if (location == null) {
                fail("$seedName: Location not found: $locationId")
            }
            location.creatureIds.forEach { creatureId ->
                val creature = CreatureRepository.findById(creatureId)
                if (creature == null) {
                    missingCreatures.add("$locationId -> $creatureId")
                }
            }
        }

        assertTrue(
            missingCreatures.isEmpty(),
            "$seedName: Missing creature references:\n${missingCreatures.joinToString("\n")}"
        )
    }

    private fun validateBidirectionalExits(locationIds: List<String>, seedName: String) {
        val oneWayExits = mutableListOf<String>()
        val locationSet = locationIds.toSet()

        locationIds.forEach { locationId ->
            val location = LocationRepository.findById(locationId)
            if (location == null) {
                fail("$seedName: Location not found: $locationId")
            }

            location.exits.forEach { exit ->
                // Skip exits to locations outside this seed (e.g., overworld connections)
                if (exit.locationId !in locationSet) return@forEach

                // Skip ENTER exits (one-way by design)
                if (exit.direction == ExitDirection.ENTER) return@forEach

                // Check for return exit
                val targetLocation = LocationRepository.findById(exit.locationId)
                if (targetLocation == null) {
                    oneWayExits.add("$locationId -> ${exit.locationId} (target not found)")
                    return@forEach
                }

                val hasReturn = targetLocation.exits.any { it.locationId == locationId }
                if (!hasReturn) {
                    oneWayExits.add("$locationId -> ${exit.locationId} (no return)")
                }
            }
        }

        assertTrue(
            oneWayExits.isEmpty(),
            "$seedName: One-way exits found (should be bidirectional):\n${oneWayExits.joinToString("\n")}"
        )
    }

    private fun validateAllLocationsReachable(
        entryLocationId: String,
        allLocationIds: List<String>,
        seedName: String
    ) {
        val locationSet = allLocationIds.toSet()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()

        queue.add(entryLocationId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            if (currentId in visited) continue
            visited.add(currentId)

            val location = LocationRepository.findById(currentId) ?: continue

            location.exits.forEach { exit ->
                if (exit.locationId in locationSet && exit.locationId !in visited) {
                    queue.add(exit.locationId)
                }
            }
        }

        val unreachable = allLocationIds.filter { it !in visited }
        assertTrue(
            unreachable.isEmpty(),
            "$seedName: Unreachable locations from $entryLocationId:\n${unreachable.joinToString("\n")}"
        )
    }
}
