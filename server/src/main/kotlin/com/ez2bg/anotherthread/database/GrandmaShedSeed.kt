package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Seed data for Grandma's Shed - a locked location behind grandma's house.
 * Contains a mysterious antique suit of armor in a display case (described, not an item).
 */
object GrandmaShedSeed {
    private val log = LoggerFactory.getLogger(GrandmaShedSeed::class.java)

    // IDs
    const val GRANDMAS_HOUSE_ID = "cd931a2c-8a1e-4da7-ac61-922b5503d038"
    const val GRANDMAS_SHED_ID = "location-grandmas-shed"

    fun seed() {
        seedLocation()
        updateGrandmasHouseExits()
        log.info("Grandma's Shed seed complete")
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
}
