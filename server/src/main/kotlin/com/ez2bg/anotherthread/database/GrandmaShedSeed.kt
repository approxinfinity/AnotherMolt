package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.ImageGenerationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Seed data for Grandma's Shed - a locked location behind grandma's house.
 * Contains a mysterious antique suit of armor in a display case (described, not an item).
 * Also contains a Wand of Freezing - roots enemies in place for 3 ticks.
 */
object GrandmaShedSeed {
    private val log = LoggerFactory.getLogger(GrandmaShedSeed::class.java)

    // IDs
    const val GRANDMAS_HOUSE_ID = "cd931a2c-8a1e-4da7-ac61-922b5503d038"
    const val GRANDMAS_SHED_ID = "location-grandmas-shed"
    const val WAND_OF_FREEZING_ID = "item-wand-of-freezing"
    const val FREEZE_ABILITY_ID = "ability-wand-freeze"

    fun seed() {
        seedAbility()
        seedItem()
        seedLocation()
        updateGrandmasHouseExits()
        generateImages()
        log.info("Grandma's Shed seed complete")
    }

    private fun seedAbility() {
        if (AbilityRepository.findById(FREEZE_ABILITY_ID) == null) {
            AbilityRepository.create(
                Ability(
                    id = FREEZE_ABILITY_ID,
                    name = "Freeze",
                    description = "Channel the wand's icy magic to root an enemy in place, preventing them from fleeing for 3 ticks.",
                    abilityType = "combat",
                    targetType = "single_enemy",
                    range = 30,
                    cooldownType = "long",  // Long cooldown between uses
                    cooldownRounds = 10,
                    durationRounds = 3,
                    manaCost = 0,
                    staminaCost = 0,
                    effects = """[{"type":"root","duration":3}]""",
                    imageUrl = "icon:ac_unit"
                )
            )
            log.info("Created Freeze ability: $FREEZE_ABILITY_ID")
        }
    }

    private fun seedItem() {
        if (ItemRepository.findById(WAND_OF_FREEZING_ID) == null) {
            ItemRepository.create(
                Item(
                    id = WAND_OF_FREEZING_ID,
                    name = "Wand of Freezing",
                    desc = "A slender wand of pale blue crystal, cold to the touch. Frost patterns dance across its surface. When pointed at a foe and activated, it releases a blast of freezing magic that roots them in place.",
                    featureIds = emptyList(),
                    abilityIds = listOf(FREEZE_ABILITY_ID),
                    equipmentType = "weapon",
                    equipmentSlot = "off_hand",
                    value = 500,
                    weight = 1
                )
            )
            log.info("Created Wand of Freezing: $WAND_OF_FREEZING_ID")
        }
    }

    private fun seedLocation() {
        if (LocationRepository.findById(GRANDMAS_SHED_ID) == null) {
            LocationRepository.create(
                Location(
                    id = GRANDMAS_SHED_ID,
                    name = "Grandma's Shed",
                    desc = "The shed is cramped and musty, filled with the accumulated oddities of decades. Garden tools hang from rusty nails, jars of preserved herbs line dusty shelves, and moth-eaten blankets cover forgotten furniture. But dominating the small space is something unexpected: a glass display case containing a suit of ornate plate armor, clearly made for a woman. The armor is remarkably well-preserved, almost glowing in the dim light filtering through the grimy window. The breastplate bears a faded crest - perhaps of a family long forgotten. Grandma has never mentioned it.",
                    itemIds = emptyList(),
                    creatureIds = emptyList(),
                    exits = listOf(
                        Exit(locationId = GRANDMAS_HOUSE_ID, direction = ExitDirection.ENTER)
                    ),
                    featureIds = emptyList(),
                    gridX = 1,
                    gridY = -1,  // Behind (south of) grandma's house
                    areaId = "overworld",
                    locationType = LocationType.INDOOR,
                    lockLevel = 2  // Standard difficulty lock
                )
            )
            log.info("Created Grandma's Shed location: $GRANDMAS_SHED_ID")

            // Add the wand to the shed (as a ground item that can be picked up)
            LocationItemRepository.addItem(GRANDMAS_SHED_ID, WAND_OF_FREEZING_ID)
            log.info("Added Wand of Freezing to shed")
        }
    }

    private fun updateGrandmasHouseExits() {
        val grandmasHouse = LocationRepository.findById(GRANDMAS_HOUSE_ID)
        if (grandmasHouse != null) {
            // Check if exit to shed already exists
            val hasExitToShed = grandmasHouse.exits.any { it.locationId == GRANDMAS_SHED_ID }
            if (!hasExitToShed) {
                val updatedExits = grandmasHouse.exits + Exit(
                    locationId = GRANDMAS_SHED_ID,
                    direction = ExitDirection.ENTER
                )
                LocationRepository.update(grandmasHouse.copy(exits = updatedExits))
                log.info("Added exit from grandma's house to shed")
            }
        }
    }

    private fun generateImages() {
        // Launch image generation in background so seed() doesn't block
        CoroutineScope(Dispatchers.IO).launch {
            generateMissingImages()
        }
    }

    /**
     * Generate images for Grandma's Shed entities that don't have images yet.
     */
    suspend fun generateMissingImages() {
        log.info("Starting image generation for Grandma's Shed entities...")

        // Generate image for Grandma's Shed
        val location = LocationRepository.findById(GRANDMAS_SHED_ID)
        if (location != null && location.imageUrl == null) {
            log.info("Generating image for location: ${location.name}")
            ImageGenerationService.generateImage(
                entityType = "location",
                entityId = GRANDMAS_SHED_ID,
                description = "Cramped dusty wooden shed interior, garden tools on rusty nails, jars of herbs on dusty shelves, moth-eaten blankets, glass display case containing ornate feminine plate armor with faded crest, dim light through grimy window, fantasy RPG location, detailed",
                entityName = location.name
            ).onSuccess { imageUrl ->
                LocationRepository.updateImageUrl(GRANDMAS_SHED_ID, imageUrl)
                log.info("Generated image for ${location.name}: $imageUrl")
            }.onFailure { error ->
                log.warn("Failed to generate image for ${location.name}: ${error.message}")
            }
        }

        // Generate image for Wand of Freezing
        val item = ItemRepository.findById(WAND_OF_FREEZING_ID)
        if (item != null && item.imageUrl == null) {
            log.info("Generating image for item: ${item.name}")
            ImageGenerationService.generateImage(
                entityType = "item",
                entityId = WAND_OF_FREEZING_ID,
                description = "Slender pale blue crystal wand, frost patterns dancing on surface, cold magical aura, ice magic weapon, fantasy RPG item, detailed",
                entityName = item.name
            ).onSuccess { imageUrl ->
                ItemRepository.updateImageUrl(WAND_OF_FREEZING_ID, imageUrl)
                log.info("Generated image for ${item.name}: $imageUrl")
            }.onFailure { error ->
                log.warn("Failed to generate image for ${item.name}: ${error.message}")
            }
        }

        log.info("Grandma's Shed image generation complete")
    }
}
