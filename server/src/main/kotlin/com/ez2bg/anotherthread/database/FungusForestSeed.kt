package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.ImageGenerationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Seed data for Fungus Forest area - creatures, items, loot tables, abilities, and chest.
 * Call seedIfEmpty() on server startup to populate initial data.
 */
object FungusForestSeed {
    private val log = LoggerFactory.getLogger(FungusForestSeed::class.java)

    // IDs for referencing
    const val SPORE_SHAMBLER_ID = "creature-spore-shambler"
    const val MYCONID_SCOUT_ID = "creature-myconid-scout"
    const val GAUNT_ONE_ELDER_ID = "creature-gaunt-one-elder"

    const val FUNGAL_ESSENCE_ID = "item-fungal-essence"
    const val LUMINESCENT_CAP_ID = "item-luminescent-cap"
    const val ELDER_STAFF_ID = "item-elder-staff"
    const val SPORE_RING_ID = "item-spore-ring"

    const val LOOT_TABLE_CR1_SHAMBLER_ID = "loot-cr1-shambler"
    const val LOOT_TABLE_CR1_SCOUT_ID = "loot-cr1-scout"
    const val LOOT_TABLE_CR2_ELDER_ID = "loot-cr2-elder"
    const val LOOT_TABLE_ELDER_CHEST_ID = "loot-elder-chest"

    const val ELDER_CHEST_ID = "chest-gaunt-one-elder"

    // Fungus Forest location IDs (assume these exist or will be created)
    const val FF_SANCTUM_LOCATION_ID = "ff-sanctum-005"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (SPORE_SHAMBLER_ID !in existingCreatures) {
            seedCreatureAbilities()
            seedItems()
            seedLootTables()
            seedCreatures()
            seedChest()
            println("Seeded Fungus Forest content")
        }
    }

    private fun seedCreatureAbilities() {
        // Spore Shambler abilities
        val toxicSpores = Ability(
            id = "ability-toxic-spores",
            name = "Toxic Spores",
            description = "Releases a cloud of poisonous spores that deals damage and poisons the target for 2 damage per round for 3 rounds.",
            classId = null, // Creature ability, not class-specific
            abilityType = "combat",
            targetType = "single_enemy",
            range = 15,
            cooldownType = "medium",
            cooldownRounds = 3,
            baseDamage = 6,
            durationRounds = 3,
            effects = """[{"type":"damage","modifier":6},{"type":"dot","damageType":"poison","damage":2,"duration":3}]"""
        )

        val fungalSlam = Ability(
            id = "ability-fungal-slam",
            name = "Fungal Slam",
            description = "A powerful melee slam with fungal-encrusted limbs.",
            classId = null,
            abilityType = "combat",
            targetType = "single_enemy",
            range = 5,
            cooldownType = "none",
            cooldownRounds = 0,
            baseDamage = 10,
            effects = """[{"type":"damage","modifier":10}]"""
        )

        // Myconid Scout abilities
        val shriek = Ability(
            id = "ability-shriek",
            name = "Shriek",
            description = "A high-pitched shriek that alerts nearby creatures, potentially drawing reinforcements.",
            classId = null,
            abilityType = "utility",
            targetType = "area",
            range = 30,
            cooldownType = "long",
            cooldownRounds = 0,
            baseDamage = 0,
            effects = """[{"type":"utility","effect":"alert_nearby"}]"""
        )

        val tendrilLash = Ability(
            id = "ability-tendril-lash",
            name = "Tendril Lash",
            description = "Lashes out with vine-like tendrils, dealing damage and slowing the target's movement.",
            classId = null,
            abilityType = "combat",
            targetType = "single_enemy",
            range = 10,
            cooldownType = "short",
            cooldownRounds = 1,
            baseDamage = 8,
            durationRounds = 2,
            effects = """[{"type":"damage","modifier":8},{"type":"condition","condition":"slowed","duration":2}]"""
        )

        // Gaunt One Elder abilities
        val mindRot = Ability(
            id = "ability-mind-rot",
            name = "Mind Rot",
            description = "Assaults the target's mind with fungal spores, dealing psychic damage and disorienting them for 3 rounds.",
            classId = null,
            abilityType = "combat",
            targetType = "single_enemy",
            range = 30,
            cooldownType = "medium",
            cooldownRounds = 3,
            baseDamage = 12,
            durationRounds = 3,
            effects = """[{"type":"damage","damageType":"psychic","modifier":12},{"type":"disorient","duration":3}]"""
        )

        val sporeCloud = Ability(
            id = "ability-spore-cloud",
            name = "Spore Cloud",
            description = "Releases a massive cloud of blinding spores that damages all enemies and blinds them for 2 rounds.",
            classId = null,
            abilityType = "combat",
            targetType = "all_enemies",
            range = 20,
            cooldownType = "long",
            cooldownRounds = 0,
            baseDamage = 8,
            durationRounds = 2,
            effects = """[{"type":"damage","modifier":8},{"type":"blind","duration":2}]"""
        )

        val regenerate = Ability(
            id = "ability-regenerate",
            name = "Regenerate",
            description = "Draws upon ancient fungal magic to heal wounds.",
            classId = null,
            abilityType = "utility",
            targetType = "self",
            range = 0,
            cooldownType = "medium",
            cooldownRounds = 3,
            baseDamage = 0,
            effects = """[{"type":"heal","modifier":10}]"""
        )

        // Create abilities if they don't exist
        listOf(toxicSpores, fungalSlam, shriek, tendrilLash, mindRot, sporeCloud, regenerate).forEach { ability ->
            if (AbilityRepository.findById(ability.id) == null) {
                AbilityRepository.create(ability)
            }
        }
    }

    private fun seedItems() {
        val fungalEssence = Item(
            id = FUNGAL_ESSENCE_ID,
            name = "Fungal Essence",
            desc = "A vial containing the glowing essence of a defeated fungal creature. It pulses with a faint bioluminescent light.",
            featureIds = emptyList(),
            abilityIds = emptyList(),
            equipmentType = null,
            equipmentSlot = null,
            statBonuses = null,
            value = 15
        )

        val luminescentCap = Item(
            id = LUMINESCENT_CAP_ID,
            name = "Luminescent Cap",
            desc = "A glowing mushroom cap that emits a soft blue light. Alchemists prize these for their magical properties.",
            featureIds = emptyList(),
            abilityIds = emptyList(),
            equipmentType = null,
            equipmentSlot = null,
            statBonuses = null,
            value = 10
        )

        val elderStaff = Item(
            id = ELDER_STAFF_ID,
            name = "Elder's Staff",
            desc = "A twisted staff grown from the body of the Gaunt One Elder. Fungal growths pulse with dark power along its length.",
            featureIds = emptyList(),
            abilityIds = emptyList(),
            equipmentType = "weapon",
            equipmentSlot = "main_hand",
            statBonuses = StatBonuses(attack = 8, defense = 0, maxHp = 5),
            value = 150
        )

        val sporeRing = Item(
            id = SPORE_RING_ID,
            name = "Ring of Spores",
            desc = "A ring crafted from petrified fungal matter. Wearing it grants a strange connection to the fungal network.",
            featureIds = emptyList(),
            abilityIds = emptyList(),
            equipmentType = "accessory",
            equipmentSlot = "ring",
            statBonuses = StatBonuses(attack = 2, defense = 2, maxHp = 10),
            value = 100
        )

        listOf(fungalEssence, luminescentCap, elderStaff, sporeRing).forEach { item ->
            if (ItemRepository.findById(item.id) == null) {
                ItemRepository.create(item)
            }
        }
    }

    private fun seedLootTables() {
        val shamblerLoot = LootTableData(
            id = LOOT_TABLE_CR1_SHAMBLER_ID,
            name = "Spore Shambler Loot",
            entries = listOf(
                LootEntry(itemId = FUNGAL_ESSENCE_ID, chance = 0.25f, minQty = 1, maxQty = 1)
            )
        )

        val scoutLoot = LootTableData(
            id = LOOT_TABLE_CR1_SCOUT_ID,
            name = "Myconid Scout Loot",
            entries = listOf(
                LootEntry(itemId = LUMINESCENT_CAP_ID, chance = 0.20f, minQty = 1, maxQty = 1)
            )
        )

        val elderLoot = LootTableData(
            id = LOOT_TABLE_CR2_ELDER_ID,
            name = "Gaunt One Elder Loot",
            entries = listOf(
                LootEntry(itemId = ELDER_STAFF_ID, chance = 1.0f, minQty = 1, maxQty = 1) // Guaranteed drop
            )
        )

        val chestLoot = LootTableData(
            id = LOOT_TABLE_ELDER_CHEST_ID,
            name = "Elder's Chest Loot",
            entries = listOf(
                LootEntry(itemId = SPORE_RING_ID, chance = 0.5f, minQty = 1, maxQty = 1),
                LootEntry(itemId = FUNGAL_ESSENCE_ID, chance = 1.0f, minQty = 2, maxQty = 3),
                LootEntry(itemId = LUMINESCENT_CAP_ID, chance = 1.0f, minQty = 1, maxQty = 2)
            )
        )

        listOf(shamblerLoot, scoutLoot, elderLoot, chestLoot).forEach { lootTable ->
            if (LootTableRepository.findById(lootTable.id) == null) {
                LootTableRepository.create(lootTable)
            }
        }
    }

    private fun seedCreatures() {
        val sporeShambler = Creature(
            id = SPORE_SHAMBLER_ID,
            name = "Spore Shambler",
            desc = "A shambling humanoid composed of fungal matter. Toxic spores drift from its porous body as it moves, and its limbs end in club-like masses of compressed mycelium.",
            itemIds = emptyList(),
            featureIds = emptyList(),
            maxHp = 25,
            baseDamage = 8,
            abilityIds = listOf("ability-toxic-spores", "ability-fungal-slam"),
            level = 1,
            experienceValue = 25,
            challengeRating = 1,
            isAggressive = true,
            lootTableId = LOOT_TABLE_CR1_SHAMBLER_ID,
            minGoldDrop = 5,
            maxGoldDrop = 15
        )

        val myconidScout = Creature(
            id = MYCONID_SCOUT_ID,
            name = "Myconid Scout",
            desc = "A small, nimble mushroom creature with a glowing cap and long, vine-like tendrils. Its cap pulses with bioluminescent patterns as it communicates silently with its kind.",
            itemIds = emptyList(),
            featureIds = emptyList(),
            maxHp = 20,
            baseDamage = 10,
            abilityIds = listOf("ability-shriek", "ability-tendril-lash"),
            level = 1,
            experienceValue = 20,
            challengeRating = 1,
            isAggressive = true,
            lootTableId = LOOT_TABLE_CR1_SCOUT_ID,
            minGoldDrop = 3,
            maxGoldDrop = 10
        )

        val gauntOneElder = Creature(
            id = GAUNT_ONE_ELDER_ID,
            name = "Gaunt One Elder",
            desc = "An ancient, towering fungal entity that has existed for centuries. Its elongated form is covered in layers of shelf fungi, and its eyeless face emanates an aura of dark wisdom. It guards its sanctum with jealous vigilance.",
            itemIds = emptyList(),
            featureIds = emptyList(),
            maxHp = 80,
            baseDamage = 15,
            abilityIds = listOf("ability-mind-rot", "ability-spore-cloud", "ability-regenerate"),
            level = 3,
            experienceValue = 100,
            challengeRating = 2,
            isAggressive = true,
            lootTableId = LOOT_TABLE_CR2_ELDER_ID,
            minGoldDrop = 50,
            maxGoldDrop = 100
        )

        listOf(sporeShambler, myconidScout, gauntOneElder).forEach { creature ->
            if (CreatureRepository.findById(creature.id) == null) {
                CreatureRepository.create(creature)
            }
        }
    }

    private fun seedChest() {
        val elderChest = Chest(
            id = ELDER_CHEST_ID,
            name = "Elder's Treasure Chest",
            desc = "An ancient chest covered in fungal growths, hidden behind where the Gaunt One Elder stood guard. Strange symbols are etched into its surface.",
            locationId = FF_SANCTUM_LOCATION_ID,
            guardianCreatureId = GAUNT_ONE_ELDER_ID,
            isLocked = true,
            lockDifficulty = 2,  // Moderate difficulty for scoundrels
            bashDifficulty = 3,  // Harder to bash than pick
            lootTableId = LOOT_TABLE_ELDER_CHEST_ID,
            goldAmount = 75
        )

        if (ChestRepository.findById(elderChest.id) == null) {
            ChestRepository.create(elderChest)
        }
    }

    /**
     * Generate images for all Fungus Forest entities that don't have images yet.
     * This should be called from a coroutine context (e.g., via an admin endpoint).
     */
    suspend fun generateMissingImages() {
        log.info("Starting image generation for Fungus Forest entities...")

        // Creature image prompts
        val creaturePrompts = mapOf(
            SPORE_SHAMBLER_ID to "Fungal humanoid creature, mushroom body, spore cloud, toxic green coloring, shambling monster, dark fantasy art, highly detailed",
            MYCONID_SCOUT_ID to "Small mushroom scout creature, glowing blue cap, bioluminescent, tendril arms, nimble, fantasy art style, detailed",
            GAUNT_ONE_ELDER_ID to "Ancient towering fungal elder, tall thin elongated form, shelf fungi covering body, eyeless face, dark wisdom, boss monster, dark fantasy art"
        )

        // Item image prompts
        val itemPrompts = mapOf(
            FUNGAL_ESSENCE_ID to "Glowing fungal essence in glass vial, green bioluminescent liquid, fantasy potion, detailed illustration",
            LUMINESCENT_CAP_ID to "Glowing mushroom cap, blue bioluminescent, magical ingredient, fantasy art",
            ELDER_STAFF_ID to "Twisted ancient staff made from mushroom, fungal growths pulsing with dark power, magical weapon, dark fantasy",
            SPORE_RING_ID to "Ring crafted from petrified fungal matter, glowing green veins, magical accessory, fantasy jewelry"
        )

        // Generate creature images
        for ((creatureId, prompt) in creaturePrompts) {
            val creature = CreatureRepository.findById(creatureId)
            if (creature != null && creature.imageUrl == null) {
                log.info("Generating image for creature: ${creature.name}")
                ImageGenerationService.generateImage(
                    entityType = "creature",
                    entityId = creatureId,
                    description = prompt,
                    entityName = creature.name
                ).onSuccess { imageUrl ->
                    CreatureRepository.updateImageUrl(creatureId, imageUrl)
                    log.info("Generated image for ${creature.name}: $imageUrl")
                }.onFailure { error ->
                    log.warn("Failed to generate image for ${creature.name}: ${error.message}")
                }
            }
        }

        // Generate item images
        for ((itemId, prompt) in itemPrompts) {
            val item = ItemRepository.findById(itemId)
            if (item != null && item.imageUrl == null) {
                log.info("Generating image for item: ${item.name}")
                ImageGenerationService.generateImage(
                    entityType = "item",
                    entityId = itemId,
                    description = prompt,
                    entityName = item.name
                ).onSuccess { imageUrl ->
                    ItemRepository.updateImageUrl(itemId, imageUrl)
                    log.info("Generated image for ${item.name}: $imageUrl")
                }.onFailure { error ->
                    log.warn("Failed to generate image for ${item.name}: ${error.message}")
                }
            }
        }

        // Generate chest image
        val chest = ChestRepository.findById(ELDER_CHEST_ID)
        if (chest != null && chest.imageUrl == null) {
            log.info("Generating image for chest: ${chest.name}")
            ImageGenerationService.generateImage(
                entityType = "chest",
                entityId = ELDER_CHEST_ID,
                description = "Ancient treasure chest covered in fungal growths, glowing mushrooms, mysterious symbols, fantasy treasure, detailed",
                entityName = chest.name
            ).onSuccess { imageUrl ->
                ChestRepository.updateImageUrl(ELDER_CHEST_ID, imageUrl)
                log.info("Generated image for ${chest.name}: $imageUrl")
            }.onFailure { error ->
                log.warn("Failed to generate image for ${chest.name}: ${error.message}")
            }
        }

        log.info("Fungus Forest image generation complete")
    }

    /**
     * Fungus Forest location prompts for image generation.
     * These are the ideal prompts for locations within the Fungus Forest area.
     * Note: Locations must be created separately - these are just the visual prompts.
     */
    val locationImagePrompts = mapOf(
        "ff-entrance-001" to "Fungus forest entrance, giant towering mushrooms, bioluminescent glow, mystical atmosphere, fantasy landscape art",
        "ff-pool-002" to "Bioluminescent pool in underground mushroom cave, glowing water reflection, colorful fungi, fantasy environment",
        "ff-grove-003" to "Giant toadstool grove, colorful oversized mushrooms, mystical fog, fantasy forest clearing",
        "ff-obelisk-004" to "Obsidian obelisk in mushroom forest, ancient runes glowing, dark mysterious atmosphere, fantasy monument",
        "ff-sanctum-005" to "Underground mushroom sanctum, ancient fungal lair, boss arena, towering fungi pillars, dark fantasy cavern"
    )
}
