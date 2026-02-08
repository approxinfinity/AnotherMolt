package com.ez2bg.anotherthread.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Seed data for trainer NPCs in Tun du Lac.
 * Trainers teach abilities to players for gold.
 */
object TrainerSeed {
    private val log = LoggerFactory.getLogger(TrainerSeed::class.java)
    private val json = Json { prettyPrint = false }

    // Trainer creature IDs
    const val ARCANE_TRAINER_ID = "trainer-arcane-master"
    const val MARTIAL_TRAINER_ID = "trainer-weapons-master"

    // Training feature IDs
    const val ARCANE_TRAINING_FEATURE_ID = "feature-arcane-training"
    const val MARTIAL_TRAINING_FEATURE_ID = "feature-martial-training"

    // Training guild location
    const val TRAINING_GUILD_ID = "tun-du-lac-training-guild"

    /**
     * Data class for trainer feature JSON
     */
    @kotlinx.serialization.Serializable
    data class TrainerFeatureData(
        val featureType: String = "trainer",
        val abilities: List<TrainerAbilityEntry> = emptyList()
    )

    @kotlinx.serialization.Serializable
    data class TrainerAbilityEntry(
        val abilityId: String,
        val goldCost: Int
    )

    fun seedIfEmpty() {
        // Check if trainers already exist
        if (CreatureRepository.findById(ARCANE_TRAINER_ID) != null) {
            log.info("Trainer seed already exists, skipping")
            return
        }

        log.info("Seeding trainer NPCs...")
        seedTrainingGuildLocation()
        seedTrainingFeatures()
        seedTrainerCreatures()
        ensureExistingUsersHaveUniversalAbilities()
        log.info("Seeded trainer NPCs")
    }

    private fun seedTrainingGuildLocation() {
        val existing = LocationRepository.findById(TRAINING_GUILD_ID)
        if (existing != null) return

        // Create the training guild location connected to town square
        val trainingGuild = Location(
            id = TRAINING_GUILD_ID,
            name = "Adventurer's Training Guild",
            desc = "A large stone building where experienced trainers teach combat techniques and magical arts. The main hall features training dummies, weapon racks, and arcane circles for practice. Two master trainers stand ready to teach those who can afford their services.",
            creatureIds = listOf(ARCANE_TRAINER_ID, MARTIAL_TRAINER_ID),
            itemIds = emptyList(),
            featureIds = emptyList(),
            exits = listOf(Exit(locationId = TunDuLacSeed.TOWN_SQUARE_ID, direction = ExitDirection.ENTER)),
            locationType = LocationType.INDOOR
        )
        LocationRepository.create(trainingGuild)

        // Add connection from town square to training guild
        val townSquare = LocationRepository.findById(TunDuLacSeed.TOWN_SQUARE_ID)
        if (townSquare != null) {
            val updatedExits = townSquare.exits + Exit(locationId = TRAINING_GUILD_ID, direction = ExitDirection.ENTER)
            LocationRepository.update(townSquare.copy(exits = updatedExits.distinctBy { it.locationId }))
        }
    }

    private fun seedTrainingFeatures() {
        // Find class abilities to teach
        val spellcasterClass = CharacterClassRepository.findAll().find { it.name == "Spellcaster" }
        val martialClass = CharacterClassRepository.findAll().find { it.name == "Martial" }

        // Create arcane training feature
        if (FeatureRepository.findById(ARCANE_TRAINING_FEATURE_ID) == null && spellcasterClass != null) {
            val spellcasterAbilities = AbilityRepository.findByClassId(spellcasterClass.id)
            val arcaneTrainingData = TrainerFeatureData(
                featureType = "trainer",
                abilities = spellcasterAbilities.map { ability ->
                    TrainerAbilityEntry(
                        abilityId = ability.id,
                        // Cost scales with level requirement and power
                        goldCost = calculateAbilityCost(ability)
                    )
                }
            )
            FeatureRepository.create(Feature(
                id = ARCANE_TRAINING_FEATURE_ID,
                name = "Arcane Training",
                description = "Learn powerful spells from the Arcane Master",
                data = json.encodeToString(arcaneTrainingData)
            ))
        }

        // Create martial training feature
        if (FeatureRepository.findById(MARTIAL_TRAINING_FEATURE_ID) == null && martialClass != null) {
            val martialAbilities = AbilityRepository.findByClassId(martialClass.id)
            val martialTrainingData = TrainerFeatureData(
                featureType = "trainer",
                abilities = martialAbilities.map { ability ->
                    TrainerAbilityEntry(
                        abilityId = ability.id,
                        goldCost = calculateAbilityCost(ability)
                    )
                }
            )
            FeatureRepository.create(Feature(
                id = MARTIAL_TRAINING_FEATURE_ID,
                name = "Martial Training",
                description = "Learn powerful combat techniques from the Weapons Master",
                data = json.encodeToString(martialTrainingData)
            ))
        }
    }

    /**
     * Calculate the gold cost to learn an ability.
     * Higher level requirements and more powerful abilities cost more.
     */
    private fun calculateAbilityCost(ability: Ability): Int {
        val baseCost = 25
        val levelMultiplier = ability.minLevel * 15  // +15g per level requirement
        val powerBonus = ability.powerCost * 2  // +2g per power cost
        return baseCost + levelMultiplier + powerBonus
    }

    private fun seedTrainerCreatures() {
        // Arcane Master trainer
        if (CreatureRepository.findById(ARCANE_TRAINER_ID) == null) {
            CreatureRepository.create(Creature(
                id = ARCANE_TRAINER_ID,
                name = "Arcane Master Valdris",
                desc = "An elderly elf in flowing blue robes, his silver hair tied back neatly. Arcane runes glow faintly on his hands as he studies an ancient tome. He is known throughout the realm as one of the greatest teachers of magical arts.",
                itemIds = emptyList(),
                featureIds = listOf(ARCANE_TRAINING_FEATURE_ID),
                isAggressive = false,
                level = 20,
                maxHp = 100,
                baseDamage = 25,
                experienceValue = 0  // Trainers don't give XP
            ))
        }

        // Weapons Master trainer
        if (CreatureRepository.findById(MARTIAL_TRAINER_ID) == null) {
            CreatureRepository.create(Creature(
                id = MARTIAL_TRAINER_ID,
                name = "Weapons Master Kira",
                desc = "A battle-scarred dwarf woman with powerful arms and keen eyes. Her armor bears the marks of countless battles, and various weapons hang from the walls behind her. She has trained some of the greatest warriors in the land.",
                itemIds = emptyList(),
                featureIds = listOf(MARTIAL_TRAINING_FEATURE_ID),
                isAggressive = false,
                level = 20,
                maxHp = 150,
                baseDamage = 30,
                experienceValue = 0  // Trainers don't give XP
            ))
        }
    }

    /**
     * Ensure all existing users have universal abilities learned.
     * This handles users created before the trainer system was implemented.
     */
    fun ensureExistingUsersHaveUniversalAbilities() {
        val universalAbilityIds = AbilityRepository.findUniversal().map { it.id }
        if (universalAbilityIds.isEmpty()) return

        val allUsers = UserRepository.findAll()
        var updatedCount = 0

        for (user in allUsers) {
            val missingAbilities = universalAbilityIds.filter { it !in user.learnedAbilityIds }
            if (missingAbilities.isNotEmpty()) {
                val updatedUser = user.copy(
                    learnedAbilityIds = (user.learnedAbilityIds + missingAbilities).distinct()
                )
                UserRepository.update(updatedUser)
                updatedCount++
            }
        }

        if (updatedCount > 0) {
            log.info("Updated $updatedCount users with missing universal abilities")
        }
    }
}
