package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.ImageGenerationService
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Seeds special items and abilities for user "goodman":
 * - Death Shroud: A cloak that obscures appearance but lowers AC by 10
 * - Phasewalk: An ability to walk through walls to adjacent locations
 */
object GoodmanGearSeed {
    private val log = LoggerFactory.getLogger(GoodmanGearSeed::class.java)

    private const val PHASEWALK_ABILITY_ID = "ability-phasewalk"
    private const val DEATH_SHROUD_ITEM_ID = "item-death-shroud"

    fun seedIfEmpty() {
        log.info("Checking Goodman's special gear seed...")

        // 1. Seed the Phasewalk ability (or fix existing one with wrong type)
        val abilityCreated = transaction {
            val existing = AbilityRepository.findById(PHASEWALK_ABILITY_ID)
            if (existing == null) {
                AbilityRepository.create(
                    Ability(
                        id = PHASEWALK_ABILITY_ID,
                        name = "Phasewalk",
                        description = "Phase through solid matter to an adjacent location that has no exit from the current room. Your form becomes ethereal momentarily as you pass through walls.",
                        classId = null,
                        abilityType = "navigation",  // Navigation abilities don't show in action tray (shown on direction ring)
                        targetType = "phasewalk",  // Special target type for phasing through walls
                        range = 0,
                        cooldownType = "none",
                        cooldownRounds = 0,
                        baseDamage = 0,
                        durationRounds = 0,
                        manaCost = 2,
                        staminaCost = 0,
                        effects = """["phasewalk"]""",
                        imageUrl = "icon:directions_walk"
                    )
                )
                log.info("Created Phasewalk ability")
                true
            } else if (existing.abilityType != "navigation") {
                // Fix existing ability if it has wrong type
                val fixed = existing.copy(abilityType = "navigation")
                AbilityRepository.update(fixed)
                log.info("Fixed Phasewalk abilityType from '${existing.abilityType}' to 'navigation'")
                false
            } else {
                false
            }
        }

        // 2. Seed the Death Shroud item (or fix existing one with wrong slot)
        val itemCreated = transaction {
            val existing = ItemRepository.findById(DEATH_SHROUD_ITEM_ID)
            if (existing == null) {
                ItemRepository.create(
                    Item(
                        id = DEATH_SHROUD_ITEM_ID,
                        name = "Death Shroud",
                        desc = "A tattered cloak woven from shadows and whispers of the dead. It wraps around the wearer like a living darkness, completely obscuring their identity and features. While it grants the ability to phase through solid matter, the shroud's ethereal nature provides no physical protection, leaving the wearer more vulnerable to attacks.",
                        featureIds = emptyList(),
                        abilityIds = listOf(PHASEWALK_ABILITY_ID),
                        equipmentType = "accessory",
                        equipmentSlot = "back",  // Cloak worn on back, doesn't conflict with main_hand weapons
                        statBonuses = StatBonuses(attack = 0, defense = -10, maxHp = 0),  // -10 armor
                        value = 0
                    )
                )
                log.info("Created Death Shroud item")
                true
            } else if (existing.equipmentSlot != "back") {
                // Fix existing item if it has wrong slot (was previously "chest")
                val fixed = existing.copy(equipmentType = "accessory", equipmentSlot = "back")
                ItemRepository.update(fixed)
                log.info("Fixed Death Shroud slot from ${existing.equipmentSlot} to 'back'")
                false
            } else {
                false
            }
        }

        // 3. Give to user "goodman"
        // First add item if needed (in separate transaction to ensure commit)
        val userId = transaction {
            UserRepository.findByName("goodman")?.id
        }

        if (userId != null) {
            // Check and add item
            val needsItem = transaction {
                val user = UserRepository.findById(userId)!!
                DEATH_SHROUD_ITEM_ID !in user.itemIds
            }
            if (needsItem) {
                UserRepository.addItems(userId, listOf(DEATH_SHROUD_ITEM_ID))
                log.info("Added Death Shroud to goodman's inventory")
            }

            // Check and equip item (after addItems transaction committed)
            val needsEquip = transaction {
                val user = UserRepository.findById(userId)!!
                DEATH_SHROUD_ITEM_ID !in user.equippedItemIds
            }
            if (needsEquip) {
                UserRepository.equipItem(userId, DEATH_SHROUD_ITEM_ID)
                log.info("Equipped Death Shroud on goodman")
            }
        } else {
            log.warn("User 'goodman' not found — cannot give Death Shroud")
        }

        // 4. Generate image via Stable Diffusion
        if (itemCreated) {
            runBlocking {
                try {
                    ImageGenerationService.generateImage(
                        entityType = "item",
                        entityId = DEATH_SHROUD_ITEM_ID,
                        description = "A dark ethereal hooded cloak made of living shadows and wisps of darkness, tattered edges that seem to fade into nothingness, ghostly and translucent, death shroud, fantasy RPG equipment, dark fantasy, mysterious, haunting",
                        entityName = "Death Shroud"
                    ).onSuccess { imageUrl ->
                        transaction {
                            ItemRepository.updateImageUrl(DEATH_SHROUD_ITEM_ID, imageUrl)
                        }
                        log.info("Generated image for Death Shroud: $imageUrl")
                    }.onFailure { error ->
                        log.warn("Failed to generate Death Shroud image: ${error.message}")
                    }
                } catch (e: Exception) {
                    log.warn("Stable Diffusion unavailable for Death Shroud image: ${e.message}")
                }
            }
        }

        log.info("Goodman's special gear seed complete")
    }
}
