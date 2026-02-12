package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.game.WanderingMonsterService
import com.ez2bg.anotherthread.game.WanderingMonsterService.WanderingEncounterEntry
import org.slf4j.LoggerFactory

/**
 * Seeds wandering encounter tables for each biome.
 *
 * These tables determine which creatures can appear as random encounters
 * in different terrain types. Inspired by OD&D Vol III wilderness encounter tables.
 *
 * Creature IDs reference existing creatures from adventure modules.
 * New "wandering-only" creatures are created here for biome variety.
 */
object WanderingEncounterSeed {
    private val log = LoggerFactory.getLogger(WanderingEncounterSeed::class.java)

    fun seed() {
        log.info("Seeding wandering encounter tables...")

        // Create wandering-only creatures that don't exist in modules
        seedWanderingCreatures()

        // Register biome encounter tables
        seedGrasslandEncounters()
        seedForestEncounters()
        seedMountainEncounters()
        seedSwampEncounters()
        seedDesertEncounters()
        seedTundraEncounters()
        seedIndoorEncounters()
        seedGenericEncounters()

        log.info("Wandering encounter tables seeded")
    }

    private fun seedWanderingCreatures() {
        val wanderingCreatures = listOf(
            Creature(
                id = "wandering-wolf",
                name = "Wolf",
                desc = "A lean gray wolf with hungry yellow eyes.",
                maxHp = 11,
                baseDamage = 5,
                damageDice = "2d4",
                level = 2,
                challengeRating = 2,
                experienceValue = 25,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-bandit",
                name = "Bandit",
                desc = "A rough-looking brigand with a notched sword.",
                maxHp = 16,
                baseDamage = 6,
                damageDice = "1d8+2",
                level = 3,
                challengeRating = 3,
                experienceValue = 35,
                isAggressive = true,
                minGoldDrop = 2,
                maxGoldDrop = 15,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-wild-boar",
                name = "Wild Boar",
                desc = "A massive tusked boar, snorting aggressively.",
                maxHp = 14,
                baseDamage = 6,
                damageDice = "2d6",
                level = 2,
                challengeRating = 2,
                experienceValue = 30,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-giant-spider",
                name = "Giant Spider",
                desc = "A dog-sized spider drops from the canopy, venom dripping from its fangs.",
                maxHp = 18,
                baseDamage = 7,
                damageDice = "1d8+3",
                level = 3,
                challengeRating = 3,
                experienceValue = 40,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-bear",
                name = "Bear",
                desc = "A massive brown bear rears up on its hind legs.",
                maxHp = 30,
                baseDamage = 8,
                damageDice = "2d6+2",
                level = 4,
                challengeRating = 4,
                experienceValue = 60,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-hawk",
                name = "Hawk",
                desc = "A fierce hunting hawk dives at you with razor talons.",
                maxHp = 6,
                baseDamage = 3,
                damageDice = "1d4+1",
                level = 1,
                challengeRating = 1,
                experienceValue = 10,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-mountain-goat",
                name = "Mountain Goat",
                desc = "A territorial mountain goat lowers its curved horns.",
                maxHp = 12,
                baseDamage = 4,
                damageDice = "1d6+1",
                level = 1,
                challengeRating = 1,
                experienceValue = 15,
                isAggressive = false,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-troll",
                name = "Troll",
                desc = "A gangly green-skinned troll lumbers toward you, regenerating wounds as it moves.",
                maxHp = 45,
                baseDamage = 10,
                damageDice = "2d8+2",
                level = 6,
                challengeRating = 6,
                experienceValue = 100,
                isAggressive = true,
                minGoldDrop = 5,
                maxGoldDrop = 30,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-giant-lizard",
                name = "Giant Lizard",
                desc = "A huge sun-basking lizard hisses and charges.",
                maxHp = 20,
                baseDamage = 7,
                damageDice = "1d10+2",
                level = 3,
                challengeRating = 3,
                experienceValue = 40,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-scorpion",
                name = "Giant Scorpion",
                desc = "A man-sized scorpion skitters across the sand, pincers clicking.",
                maxHp = 24,
                baseDamage = 8,
                damageDice = "2d6+1",
                level = 4,
                challengeRating = 4,
                experienceValue = 50,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-winter-wolf",
                name = "Winter Wolf",
                desc = "A white-furred wolf exhales a plume of frost. Its eyes gleam with unnatural intelligence.",
                maxHp = 28,
                baseDamage = 9,
                damageDice = "2d6+3",
                level = 5,
                challengeRating = 5,
                experienceValue = 75,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-lizardman",
                name = "Lizardman",
                desc = "A scaled humanoid wielding a crude spear rises from the murky water.",
                maxHp = 22,
                baseDamage = 7,
                damageDice = "1d8+3",
                level = 3,
                challengeRating = 3,
                experienceValue = 40,
                isAggressive = true,
                minGoldDrop = 1,
                maxGoldDrop = 8,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-giant-insect",
                name = "Giant Dragonfly",
                desc = "A dragonfly the size of a dog buzzes menacingly, compound eyes tracking your movement.",
                maxHp = 10,
                baseDamage = 4,
                damageDice = "1d6",
                level = 2,
                challengeRating = 2,
                experienceValue = 20,
                isAggressive = true,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-nomad",
                name = "Desert Nomad",
                desc = "A sun-weathered warrior in flowing robes, scimitar drawn.",
                maxHp = 20,
                baseDamage = 7,
                damageDice = "1d8+3",
                level = 3,
                challengeRating = 3,
                experienceValue = 35,
                isAggressive = false,
                minGoldDrop = 3,
                maxGoldDrop = 20,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            ),
            Creature(
                id = "wandering-eagle",
                name = "Giant Eagle",
                desc = "A massive eagle with a wingspan wider than a man is tall swoops down from the peaks.",
                maxHp = 16,
                baseDamage = 6,
                damageDice = "1d8+2",
                level = 3,
                challengeRating = 3,
                experienceValue = 35,
                isAggressive = false,
                minGoldDrop = 0,
                maxGoldDrop = 0,
                itemIds = emptyList(),
                featureIds = emptyList(),
                abilityIds = emptyList()
            )
        )

        for (creature in wanderingCreatures) {
            if (CreatureRepository.findById(creature.id) == null) {
                CreatureRepository.create(creature)
                log.info("Created wandering creature: ${creature.name} (${creature.id})")
            }
        }
    }

    private fun seedGrasslandEncounters() {
        WanderingMonsterService.registerBiomeEncounters("GRASSLAND", listOf(
            WanderingEncounterEntry("wandering-bandit", weight = 3, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-wild-boar", weight = 3, minChallengeRating = 1, maxChallengeRating = 4),
            WanderingEncounterEntry("wandering-wolf", weight = 2, minCount = 1, maxCount = 3, minChallengeRating = 1, maxChallengeRating = 4),
            WanderingEncounterEntry("wandering-hawk", weight = 2, minChallengeRating = 1, maxChallengeRating = 2),
            WanderingEncounterEntry("wandering-bandit", weight = 1, minCount = 2, maxCount = 4, minChallengeRating = 3, maxChallengeRating = 8)
        ))

        // Also register for hills
        WanderingMonsterService.registerBiomeEncounters("HILLS", listOf(
            WanderingEncounterEntry("wandering-bandit", weight = 2, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-wild-boar", weight = 2, minChallengeRating = 1, maxChallengeRating = 4),
            WanderingEncounterEntry("wandering-wolf", weight = 3, minCount = 1, maxCount = 2, minChallengeRating = 1, maxChallengeRating = 4),
            WanderingEncounterEntry("wandering-mountain-goat", weight = 2, minChallengeRating = 1, maxChallengeRating = 3),
            WanderingEncounterEntry("wandering-troll", weight = 1, minChallengeRating = 4, maxChallengeRating = 10)
        ))

        WanderingMonsterService.registerBiomeEncounters("SHRUBLAND", listOf(
            WanderingEncounterEntry("wandering-bandit", weight = 2, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-wild-boar", weight = 3, minChallengeRating = 1, maxChallengeRating = 4),
            WanderingEncounterEntry("wandering-wolf", weight = 2, minCount = 1, maxCount = 2, minChallengeRating = 1, maxChallengeRating = 4),
            WanderingEncounterEntry("wandering-giant-lizard", weight = 2, minChallengeRating = 2, maxChallengeRating = 5)
        ))
    }

    private fun seedForestEncounters() {
        val forestEntries = listOf(
            WanderingEncounterEntry("wandering-wolf", weight = 3, minCount = 1, maxCount = 3, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-giant-spider", weight = 3, minChallengeRating = 2, maxChallengeRating = 6),
            WanderingEncounterEntry("wandering-bear", weight = 2, minChallengeRating = 3, maxChallengeRating = 8),
            WanderingEncounterEntry("wandering-bandit", weight = 2, minCount = 1, maxCount = 3, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-wild-boar", weight = 2, minChallengeRating = 1, maxChallengeRating = 4)
        )

        WanderingMonsterService.registerBiomeEncounters("TEMPERATE_DECIDUOUS_FOREST", forestEntries)
        WanderingMonsterService.registerBiomeEncounters("TEMPERATE_RAIN_FOREST", forestEntries)
        WanderingMonsterService.registerBiomeEncounters("TROPICAL_SEASONAL_FOREST", forestEntries + listOf(
            WanderingEncounterEntry("wandering-giant-insect", weight = 2, minChallengeRating = 1, maxChallengeRating = 4)
        ))
        WanderingMonsterService.registerBiomeEncounters("TROPICAL_RAIN_FOREST", forestEntries + listOf(
            WanderingEncounterEntry("wandering-giant-spider", weight = 2, minCount = 1, maxCount = 2, minChallengeRating = 2, maxChallengeRating = 6),
            WanderingEncounterEntry("wandering-giant-insect", weight = 2, minChallengeRating = 1, maxChallengeRating = 4)
        ))

        WanderingMonsterService.registerBiomeEncounters("TAIGA", listOf(
            WanderingEncounterEntry("wandering-wolf", weight = 3, minCount = 1, maxCount = 3, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-bear", weight = 3, minChallengeRating = 3, maxChallengeRating = 8),
            WanderingEncounterEntry("wandering-winter-wolf", weight = 1, minChallengeRating = 4, maxChallengeRating = 10)
        ))
    }

    private fun seedMountainEncounters() {
        WanderingMonsterService.registerBiomeEncounters("MOUNTAIN", listOf(
            WanderingEncounterEntry("wandering-mountain-goat", weight = 3, minChallengeRating = 1, maxChallengeRating = 3),
            WanderingEncounterEntry("wandering-eagle", weight = 2, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-troll", weight = 2, minChallengeRating = 4, maxChallengeRating = 10),
            WanderingEncounterEntry("wandering-wolf", weight = 2, minCount = 1, maxCount = 2, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-bear", weight = 1, minChallengeRating = 3, maxChallengeRating = 8)
        ))

        WanderingMonsterService.registerBiomeEncounters("BARE", listOf(
            WanderingEncounterEntry("wandering-mountain-goat", weight = 3, minChallengeRating = 1, maxChallengeRating = 3),
            WanderingEncounterEntry("wandering-eagle", weight = 2, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-troll", weight = 1, minChallengeRating = 4, maxChallengeRating = 10)
        ))
    }

    private fun seedSwampEncounters() {
        WanderingMonsterService.registerBiomeEncounters("MARSH", listOf(
            WanderingEncounterEntry("wandering-lizardman", weight = 3, minCount = 1, maxCount = 2, minChallengeRating = 2, maxChallengeRating = 6),
            WanderingEncounterEntry("wandering-giant-insect", weight = 3, minCount = 1, maxCount = 3, minChallengeRating = 1, maxChallengeRating = 4),
            WanderingEncounterEntry("wandering-giant-spider", weight = 2, minChallengeRating = 2, maxChallengeRating = 6),
            WanderingEncounterEntry("wandering-wolf", weight = 1, minChallengeRating = 1, maxChallengeRating = 4)
        ))
    }

    private fun seedDesertEncounters() {
        val desertEntries = listOf(
            WanderingEncounterEntry("wandering-scorpion", weight = 3, minChallengeRating = 2, maxChallengeRating = 7),
            WanderingEncounterEntry("wandering-nomad", weight = 2, minCount = 1, maxCount = 3, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-giant-lizard", weight = 3, minChallengeRating = 2, maxChallengeRating = 6),
            WanderingEncounterEntry("wandering-hawk", weight = 2, minChallengeRating = 1, maxChallengeRating = 3)
        )

        WanderingMonsterService.registerBiomeEncounters("TEMPERATE_DESERT", desertEntries)
        WanderingMonsterService.registerBiomeEncounters("SUBTROPICAL_DESERT", desertEntries)
        WanderingMonsterService.registerBiomeEncounters("SCORCHED", desertEntries + listOf(
            WanderingEncounterEntry("wandering-scorpion", weight = 2, minCount = 1, maxCount = 2, minChallengeRating = 3, maxChallengeRating = 8)
        ))
    }

    private fun seedTundraEncounters() {
        val coldEntries = listOf(
            WanderingEncounterEntry("wandering-winter-wolf", weight = 3, minChallengeRating = 3, maxChallengeRating = 10),
            WanderingEncounterEntry("wandering-wolf", weight = 3, minCount = 1, maxCount = 3, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-bear", weight = 2, minChallengeRating = 3, maxChallengeRating = 8)
        )

        WanderingMonsterService.registerBiomeEncounters("TUNDRA", coldEntries)
        WanderingMonsterService.registerBiomeEncounters("SNOW", coldEntries)
    }

    private fun seedIndoorEncounters() {
        WanderingMonsterService.registerIndoorEncounters(listOf(
            WanderingEncounterEntry("wandering-giant-spider", weight = 2, minChallengeRating = 2, maxChallengeRating = 6),
            WanderingEncounterEntry("wandering-giant-insect", weight = 2, minChallengeRating = 1, maxChallengeRating = 4)
        ))
    }

    private fun seedGenericEncounters() {
        WanderingMonsterService.registerBiomeEncounters("GENERIC", listOf(
            WanderingEncounterEntry("wandering-wolf", weight = 3, minCount = 1, maxCount = 2, minChallengeRating = 1, maxChallengeRating = 4),
            WanderingEncounterEntry("wandering-bandit", weight = 2, minChallengeRating = 1, maxChallengeRating = 5),
            WanderingEncounterEntry("wandering-hawk", weight = 2, minChallengeRating = 1, maxChallengeRating = 2)
        ))
    }
}
