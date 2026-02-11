package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.game.LockpickingService

/**
 * Seed data for the lockpicking system.
 */
object LockpickingSeed {
    private val log = org.slf4j.LoggerFactory.getLogger("LockpickingSeed")

    const val LOCKPICKING_ABILITY_ID = LockpickingService.LOCKPICKING_ABILITY_ID
    const val LOCKPICK_SET_ID = "item-lockpick-set"

    fun seed() {
        seedAbility()
        seedItems()
        log.info("Lockpicking seed complete")
    }

    private fun seedAbility() {
        // Create the lockpicking ability (learned by rogue-type classes)
        if (AbilityRepository.findById(LOCKPICKING_ABILITY_ID) == null) {
            AbilityRepository.create(
                Ability(
                    id = LOCKPICKING_ABILITY_ID,
                    name = "Lockpicking",
                    description = "The art of opening locks without a key. Allows picking locks regardless of Dexterity.",
                    abilityType = "utility",
                    targetType = "self",
                    range = 0,
                    cooldownType = "none",
                    cooldownRounds = 0,
                    manaCost = 0,
                    staminaCost = 0
                )
            )
            log.info("Created lockpicking ability: $LOCKPICKING_ABILITY_ID")
        }
    }

    private fun seedItems() {
        // Create the lockpick set item
        if (ItemRepository.findById(LOCKPICK_SET_ID) == null) {
            ItemRepository.create(
                Item(
                    id = LOCKPICK_SET_ID,
                    name = "Lockpick Set",
                    desc = "A set of fine lockpicks and tension wrenches. Essential tools for opening locks without keys.",
                    featureIds = emptyList(),
                    value = 50,
                    weight = 1
                )
            )
            log.info("Created lockpick set item: $LOCKPICK_SET_ID")
        }
    }
}
