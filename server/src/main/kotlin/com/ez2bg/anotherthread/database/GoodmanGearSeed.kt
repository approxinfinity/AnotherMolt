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

    const val RIFT_PORTAL_ABILITY_ID = "ability-rift-portal"
    const val RIFT_RING_ITEM_ID = "item-rift-ring"

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

        // 5. Seed the Rift Portal ability
        transaction {
            val existing = AbilityRepository.findById(RIFT_PORTAL_ABILITY_ID)
            if (existing == null) {
                AbilityRepository.create(
                    Ability(
                        id = RIFT_PORTAL_ABILITY_ID,
                        name = "Open Rift Portal",
                        description = "Tear open a permanent rift in space, creating a portal to another realm. The rift appears as a shimmering tear in reality and remains as a permanent exit from this location.",
                        classId = null,
                        abilityType = "rift_portal",  // Special type for opening rifts between maps
                        targetType = "self",
                        range = 0,
                        cooldownType = "none",
                        cooldownRounds = 0,
                        baseDamage = 0,
                        durationRounds = 0,
                        manaCost = 10,
                        staminaCost = 0,
                        effects = """["rift_portal"]""",
                        imageUrl = "icon:blur_on"
                    )
                )
                log.info("Created Rift Portal ability")
            }
        }

        // 6. Seed the Rift Ring item
        val riftRingCreated = transaction {
            val existing = ItemRepository.findById(RIFT_RING_ITEM_ID)
            if (existing == null) {
                ItemRepository.create(
                    Item(
                        id = RIFT_RING_ITEM_ID,
                        name = "Rift Ring",
                        desc = "A band of dark metal set with a gemstone that contains a swirling fragment of the void between worlds. When activated, the wearer can tear open permanent portals to unconnected realms, allowing travel between distant places.",
                        featureIds = emptyList(),
                        abilityIds = listOf(RIFT_PORTAL_ABILITY_ID),
                        equipmentType = "accessory",
                        equipmentSlot = "finger",
                        statBonuses = StatBonuses(attack = 0, defense = 0, maxHp = 0),
                        value = 0
                    )
                )
                log.info("Created Rift Ring item")
                true
            } else {
                false
            }
        }

        // 7. Give Rift Ring to user "goodman"
        if (userId != null) {
            val needsRiftRing = transaction {
                val user = UserRepository.findById(userId)!!
                RIFT_RING_ITEM_ID !in user.itemIds
            }
            if (needsRiftRing) {
                UserRepository.addItems(userId, listOf(RIFT_RING_ITEM_ID))
                log.info("Added Rift Ring to goodman's inventory")
            }
        }

        // 8. Generate Rift Ring image
        if (riftRingCreated) {
            runBlocking {
                try {
                    ImageGenerationService.generateImage(
                        entityType = "item",
                        entityId = RIFT_RING_ITEM_ID,
                        description = "A dark metal ring with a gemstone containing swirling purple void energy, dimensional rift ring, fantasy RPG jewelry, arcane power, portal magic, cosmic energy, dark fantasy, ornate band",
                        entityName = "Rift Ring"
                    ).onSuccess { imageUrl ->
                        transaction {
                            ItemRepository.updateImageUrl(RIFT_RING_ITEM_ID, imageUrl)
                        }
                        log.info("Generated image for Rift Ring: $imageUrl")
                    }.onFailure { error ->
                        log.warn("Failed to generate Rift Ring image: ${error.message}")
                    }
                } catch (e: Exception) {
                    log.warn("Stable Diffusion unavailable for Rift Ring image: ${e.message}")
                }
            }
        }

        log.info("Goodman's special gear seed complete")
    }
}
