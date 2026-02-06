package com.ez2bg.anotherthread.database

/**
 * Seed interface hierarchy for type-safe seed validation.
 *
 * Seeds are categorized by what they create:
 * - ContentSeed: Creates creatures, items, abilities, and loot tables (themed content bundles)
 * - LocationSeed: Creates locations with creature/item placements (adventure modules)
 * - AbilitySeed: Creates standalone abilities
 * - ItemSeed: Creates standalone items
 */

/**
 * Base interface for all seeds.
 */
interface Seed {
    /** Human-readable name for this seed */
    val seedName: String

    /** Attribution source for the content */
    val attribution: String

    /** Seed the database if content doesn't exist */
    fun seedIfEmpty()
}

/**
 * Seed that creates themed content bundles (creatures, items, abilities, loot tables).
 * Examples: ClassicFantasySeed, UndeadCryptSeed, ElementalChaosSeed
 */
interface ContentSeed : Seed {
    /** All creature IDs this seed creates */
    val creatureIds: List<String>

    /** All item IDs this seed creates */
    val itemIds: List<String>

    /** All ability IDs this seed creates */
    val abilityIds: List<String>

    /** All loot table IDs this seed creates */
    val lootTableIds: List<String>

    /** Tier information for creatures (creatureId -> tier 1-4) */
    val creatureTiers: Map<String, Int>
        get() = emptyMap()
}

/**
 * Seed that creates locations forming an adventure module.
 * Examples: UndeadCryptLocationsSeed, GoblinWarrenLocationsSeed
 */
interface LocationSeed : Seed {
    /** Area ID that groups these locations */
    val areaId: String

    /** All location IDs this seed creates */
    val locationIds: List<String>

    /** The entry point location ID (accessible from overworld) */
    val entryLocationId: String

    /** Boss/final location ID if applicable */
    val bossLocationId: String?
        get() = null

    /** Get all exits defined in this seed (for bidirectional validation) */
    fun getAllExits(): List<Pair<String, Exit>>

    /** Get creature IDs placed in each location */
    fun getLocationCreatures(): Map<String, List<String>>

    /** Get item IDs placed in each location */
    fun getLocationItems(): Map<String, List<String>>
}

/**
 * Seed that creates standalone abilities.
 * Examples: UniversalAbilitySeed, WeaponAbilitySeed
 */
interface AbilitySeed : Seed {
    /** All ability IDs this seed creates */
    val abilityIds: List<String>
}

/**
 * Seed that creates standalone items.
 * Examples: GoodmanGearSeed, WayfarerStaveSeed
 */
interface ItemSeed : Seed {
    /** All item IDs this seed creates */
    val itemIds: List<String>
}

/**
 * Registry of all seeds for validation purposes.
 * Add new seeds here to include them in validation tests.
 */
object SeedRegistry {
    // Content seeds (create creatures, items, abilities, loot)
    val contentSeeds: List<ContentSeed> by lazy {
        listOf(
            // Add seeds as they implement ContentSeed interface
        )
    }

    // Location seeds (create adventure locations)
    val locationSeeds: List<LocationSeed> by lazy {
        listOf(
            // Add seeds as they implement LocationSeed interface
        )
    }

    // Ability seeds
    val abilitySeeds: List<AbilitySeed> by lazy {
        listOf(
            // Add seeds as they implement AbilitySeed interface
        )
    }

    // Item seeds
    val itemSeeds: List<ItemSeed> by lazy {
        listOf(
            // Add seeds as they implement ItemSeed interface
        )
    }

    // All seeds
    val allSeeds: List<Seed>
        get() = contentSeeds + locationSeeds + abilitySeeds + itemSeeds

    // Convenience methods for validation

    /** Get all creature IDs across all content seeds */
    fun getAllCreatureIds(): Set<String> = contentSeeds.flatMap { it.creatureIds }.toSet()

    /** Get all item IDs across all seeds */
    fun getAllItemIds(): Set<String> =
        (contentSeeds.flatMap { it.itemIds } + itemSeeds.flatMap { it.itemIds }).toSet()

    /** Get all ability IDs across all seeds */
    fun getAllAbilityIds(): Set<String> =
        (contentSeeds.flatMap { it.abilityIds } + abilitySeeds.flatMap { it.abilityIds }).toSet()

    /** Get all location IDs across all location seeds */
    fun getAllLocationIds(): Set<String> = locationSeeds.flatMap { it.locationIds }.toSet()

    /** Get all loot table IDs across all content seeds */
    fun getAllLootTableIds(): Set<String> = contentSeeds.flatMap { it.lootTableIds }.toSet()
}

/**
 * Validation result for a single check.
 */
data class ValidationResult(
    val seedName: String,
    val checkName: String,
    val passed: Boolean,
    val message: String,
    val details: List<String> = emptyList()
) {
    override fun toString(): String {
        val status = if (passed) "PASS" else "FAIL"
        val detailStr = if (details.isNotEmpty()) "\n  - ${details.joinToString("\n  - ")}" else ""
        return "[$status] $seedName: $checkName - $message$detailStr"
    }
}

/**
 * Validation utilities for seeds.
 */
object SeedValidator {

    /**
     * Validate that all creature ability references exist.
     */
    fun validateCreatureAbilities(seed: ContentSeed): ValidationResult {
        val allAbilityIds = SeedRegistry.getAllAbilityIds()
        val missingAbilities = mutableListOf<String>()

        seed.creatureIds.forEach { creatureId ->
            val creature = CreatureRepository.findById(creatureId)
            creature?.abilityIds?.forEach { abilityId ->
                if (abilityId !in allAbilityIds && AbilityRepository.findById(abilityId) == null) {
                    missingAbilities.add("$creatureId references missing ability: $abilityId")
                }
            }
        }

        return ValidationResult(
            seedName = seed.seedName,
            checkName = "Creature Ability References",
            passed = missingAbilities.isEmpty(),
            message = if (missingAbilities.isEmpty()) "All creature abilities exist"
                      else "${missingAbilities.size} missing ability references",
            details = missingAbilities
        )
    }

    /**
     * Validate that location creature references exist.
     */
    fun validateLocationCreatures(seed: LocationSeed): ValidationResult {
        val allCreatureIds = SeedRegistry.getAllCreatureIds()
        val missingCreatures = mutableListOf<String>()

        seed.getLocationCreatures().forEach { (locationId, creatureIds) ->
            creatureIds.forEach { creatureId ->
                if (creatureId !in allCreatureIds && CreatureRepository.findById(creatureId) == null) {
                    missingCreatures.add("$locationId references missing creature: $creatureId")
                }
            }
        }

        return ValidationResult(
            seedName = seed.seedName,
            checkName = "Location Creature References",
            passed = missingCreatures.isEmpty(),
            message = if (missingCreatures.isEmpty()) "All location creatures exist"
                      else "${missingCreatures.size} missing creature references",
            details = missingCreatures
        )
    }

    /**
     * Validate that location item references exist.
     */
    fun validateLocationItems(seed: LocationSeed): ValidationResult {
        val allItemIds = SeedRegistry.getAllItemIds()
        val missingItems = mutableListOf<String>()

        seed.getLocationItems().forEach { (locationId, itemIds) ->
            itemIds.forEach { itemId ->
                if (itemId !in allItemIds && ItemRepository.findById(itemId) == null) {
                    missingItems.add("$locationId references missing item: $itemId")
                }
            }
        }

        return ValidationResult(
            seedName = seed.seedName,
            checkName = "Location Item References",
            passed = missingItems.isEmpty(),
            message = if (missingItems.isEmpty()) "All location items exist"
                      else "${missingItems.size} missing item references",
            details = missingItems
        )
    }

    /**
     * Validate that exits are bidirectional (if A->B exists, B->A should exist).
     */
    fun validateBidirectionalExits(seed: LocationSeed): ValidationResult {
        val allExits = seed.getAllExits()
        val exitMap = mutableMapOf<String, MutableSet<String>>()

        // Build a map of location -> destinations
        allExits.forEach { (fromLocation, exit) ->
            exitMap.getOrPut(fromLocation) { mutableSetOf() }.add(exit.locationId)
        }

        val missingReturns = mutableListOf<String>()

        allExits.forEach { (fromLocation, exit) ->
            val toLocation = exit.locationId
            // Check if there's a return path (skip ENTER exits to overworld)
            if (exit.direction != ExitDirection.ENTER) {
                val returnExists = exitMap[toLocation]?.contains(fromLocation) == true
                if (!returnExists) {
                    missingReturns.add("$fromLocation -> $toLocation has no return exit")
                }
            }
        }

        return ValidationResult(
            seedName = seed.seedName,
            checkName = "Bidirectional Exits",
            passed = missingReturns.isEmpty(),
            message = if (missingReturns.isEmpty()) "All exits are bidirectional"
                      else "${missingReturns.size} one-way exits found",
            details = missingReturns
        )
    }

    /**
     * Validate that all locations are reachable from the entry point.
     */
    fun validateLocationReachability(seed: LocationSeed): ValidationResult {
        val allExits = seed.getAllExits()
        val exitMap = mutableMapOf<String, MutableSet<String>>()

        // Build adjacency map
        allExits.forEach { (fromLocation, exit) ->
            exitMap.getOrPut(fromLocation) { mutableSetOf() }.add(exit.locationId)
        }

        // BFS from entry point
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(seed.entryLocationId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in visited) continue
            visited.add(current)

            exitMap[current]?.forEach { neighbor ->
                if (neighbor !in visited && neighbor in seed.locationIds.toSet()) {
                    queue.add(neighbor)
                }
            }
        }

        val unreachable = seed.locationIds.filter { it !in visited }

        return ValidationResult(
            seedName = seed.seedName,
            checkName = "Location Reachability",
            passed = unreachable.isEmpty(),
            message = if (unreachable.isEmpty()) "All ${seed.locationIds.size} locations reachable from entry"
                      else "${unreachable.size} unreachable locations",
            details = unreachable.map { "Unreachable: $it" }
        )
    }

    /**
     * Validate no duplicate IDs within a seed.
     */
    fun validateNoDuplicateIds(seed: Seed): ValidationResult {
        val allIds = mutableListOf<String>()

        when (seed) {
            is ContentSeed -> {
                allIds.addAll(seed.creatureIds)
                allIds.addAll(seed.itemIds)
                allIds.addAll(seed.abilityIds)
                allIds.addAll(seed.lootTableIds)
            }
            is LocationSeed -> {
                allIds.addAll(seed.locationIds)
            }
            is AbilitySeed -> {
                allIds.addAll(seed.abilityIds)
            }
            is ItemSeed -> {
                allIds.addAll(seed.itemIds)
            }
        }

        val duplicates = allIds.groupBy { it }.filter { it.value.size > 1 }.keys

        return ValidationResult(
            seedName = seed.seedName,
            checkName = "No Duplicate IDs",
            passed = duplicates.isEmpty(),
            message = if (duplicates.isEmpty()) "No duplicate IDs"
                      else "${duplicates.size} duplicate IDs found",
            details = duplicates.map { "Duplicate: $it" }
        )
    }

    /**
     * Run all validations for a seed.
     */
    fun validateSeed(seed: Seed): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        results.add(validateNoDuplicateIds(seed))

        when (seed) {
            is ContentSeed -> {
                results.add(validateCreatureAbilities(seed))
            }
            is LocationSeed -> {
                results.add(validateLocationCreatures(seed))
                results.add(validateLocationItems(seed))
                results.add(validateBidirectionalExits(seed))
                results.add(validateLocationReachability(seed))
            }
        }

        return results
    }

    /**
     * Run all validations for all registered seeds.
     */
    fun validateAllSeeds(): List<ValidationResult> {
        return SeedRegistry.allSeeds.flatMap { validateSeed(it) }
    }
}
