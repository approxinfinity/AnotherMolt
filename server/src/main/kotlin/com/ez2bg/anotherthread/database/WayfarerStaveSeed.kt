package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.ImageGenerationService
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Seed the Wayfarer's Stave item and its teleport ability.
 * Gives the item to user "goodman" and generates an image via Stable Diffusion.
 */
object WayfarerStaveSeed {
    private val log = LoggerFactory.getLogger(WayfarerStaveSeed::class.java)

    private const val ABILITY_ID = "weapon-wayfarer-teleport"
    private const val ITEM_ID = "wayfarer-stave"

    fun seedIfEmpty() {
        log.info("Checking Wayfarer's Stave seed...")

        // 1. Seed the teleport ability
        val abilityCreated = transaction {
            val exists = AbilityTable.selectAll()
                .where { AbilityTable.id eq ABILITY_ID }
                .count() > 0
            if (!exists) {
                AbilityRepository.create(
                    Ability(
                        id = ABILITY_ID,
                        name = "Wayfare",
                        description = "Channel the stave's arcane energies to tear open a rift to the heart of any known land.",
                        classId = null,
                        abilityType = "item",
                        targetType = "map_select",
                        range = 0,
                        cooldownType = "none",
                        cooldownRounds = 0,
                        baseDamage = 0,
                        durationRounds = 0,
                        manaCost = 1,
                        staminaCost = 0,
                        effects = """["teleport"]""",
                        imageUrl = "icon:flash"
                    )
                )
                log.info("Created Wayfare teleport ability")
                true
            } else {
                false
            }
        }

        // 2. Seed the item
        val itemCreated = transaction {
            val exists = ItemTable.selectAll()
                .where { ItemTable.id eq ITEM_ID }
                .count() > 0
            if (!exists) {
                ItemRepository.create(
                    Item(
                        id = ITEM_ID,
                        name = "Wayfarer's Stave",
                        desc = "A gnarled staff of ancient ashwood, wound with silver thread that hums with planar energy. Those who grasp it feel the pull of distant lands.",
                        featureIds = emptyList(),
                        abilityIds = listOf(ABILITY_ID),
                        equipmentType = "weapon",
                        equipmentSlot = "main_hand",
                        statBonuses = StatBonuses(attack = 2),
                        value = 0
                    )
                )
                log.info("Created Wayfarer's Stave item")
                true
            } else {
                false
            }
        }

        // 3. Give to user "goodman"
        transaction {
            val user = UserRepository.findByName("goodman")
            if (user != null) {
                val needsItem = ITEM_ID !in user.itemIds
                val needsEquip = ITEM_ID !in user.equippedItemIds

                if (needsItem) {
                    UserRepository.addItems(user.id, listOf(ITEM_ID))
                    log.info("Added Wayfarer's Stave to goodman's inventory")
                }
                if (needsEquip) {
                    UserRepository.equipItem(user.id, ITEM_ID)
                    log.info("Equipped Wayfarer's Stave on goodman")
                }
            } else {
                log.warn("User 'goodman' not found — cannot give Wayfarer's Stave")
            }
        }

        // 4. Generate image via Stable Diffusion
        if (itemCreated) {
            runBlocking {
                try {
                    ImageGenerationService.generateImage(
                        entityType = "item",
                        entityId = ITEM_ID,
                        description = "A gnarled ancient ashwood staff wound with glowing silver thread, pulsing with arcane planar energy, fantasy RPG weapon, detailed illustration",
                        entityName = "Wayfarer's Stave"
                    ).onSuccess { imageUrl ->
                        transaction {
                            ItemRepository.updateImageUrl(ITEM_ID, imageUrl)
                        }
                        log.info("Generated image for Wayfarer's Stave: $imageUrl")
                    }.onFailure { error ->
                        log.warn("Failed to generate Wayfarer's Stave image: ${error.message}")
                    }
                } catch (e: Exception) {
                    log.warn("Stable Diffusion unavailable for Wayfarer's Stave image: ${e.message}")
                }
            }
        }

        log.info("Wayfarer's Stave seed complete")
    }
}
