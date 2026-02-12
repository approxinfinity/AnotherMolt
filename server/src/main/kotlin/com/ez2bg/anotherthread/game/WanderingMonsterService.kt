package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.combat.CombatRng
import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.events.LocationEventService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * OD&D-style wandering monster system.
 *
 * Every N ticks, each location with active players gets a 1-in-6 chance
 * of spawning a wandering monster appropriate to its biome and difficulty.
 *
 * Wandering monsters are temporary — they are NOT tracked by CreatureRespawnService
 * because they won't appear in the initial quota snapshot. When killed, they simply
 * disappear without respawning.
 *
 * If not engaged within a timeout period, wandering monsters despawn automatically.
 */
object WanderingMonsterService {
    private val log = LoggerFactory.getLogger(WanderingMonsterService::class.java)

    /** How often to check for wandering encounters (in ticks). ~30s at 3s/tick. */
    const val CHECK_INTERVAL_TICKS = 10L

    /** Ticks before an unengaged wandering monster despawns. ~5 min at 3s/tick. */
    private const val DESPAWN_TIMEOUT_TICKS = 100L

    /** Encounter tables keyed by biome name (uppercase). */
    private val encounterTables = ConcurrentHashMap<String, List<WanderingEncounterEntry>>()

    /** Indoor/underground encounter table (for dungeons without a biome). */
    private val indoorEncounterTable = mutableListOf<WanderingEncounterEntry>()

    /** Track spawned wandering monsters for despawn cleanup. */
    data class WanderingSpawn(
        val locationId: String,
        val creatureId: String,
        val spawnTick: Long
    )
    private val activeWanderingMonsters = ConcurrentHashMap.newKeySet<WanderingSpawn>()

    /**
     * A single entry in a wandering encounter table.
     */
    data class WanderingEncounterEntry(
        val creatureId: String,
        val weight: Int = 1,
        val minCount: Int = 1,
        val maxCount: Int = 1,
        val minChallengeRating: Int = 1,
        val maxChallengeRating: Int = 20
    )

    /**
     * Register encounter entries for a biome.
     */
    fun registerBiomeEncounters(biome: String, entries: List<WanderingEncounterEntry>) {
        encounterTables[biome.uppercase()] = entries
        log.info("Registered ${entries.size} wandering encounter entries for biome $biome")
    }

    /**
     * Register encounter entries for indoor/underground locations.
     */
    fun registerIndoorEncounters(entries: List<WanderingEncounterEntry>) {
        indoorEncounterTable.clear()
        indoorEncounterTable.addAll(entries)
        log.info("Registered ${entries.size} indoor wandering encounter entries")
    }

    /**
     * Main tick processor. Called from GameTickService every CHECK_INTERVAL_TICKS.
     */
    fun processEncounterChecks(currentTick: Long) {
        // 1. Despawn timed-out wandering monsters
        processDespawns(currentTick)

        // 2. Find locations with active players
        val activeUsers = UserRepository.findRecentlyActive(300_000) // 5 min
        if (activeUsers.isEmpty()) return

        // Group players by location
        val playersByLocation = activeUsers.groupBy { it.currentLocationId }

        for ((locationId, _) in playersByLocation) {
            if (locationId.isNullOrBlank()) continue

            // 1-in-6 chance per location per check
            if (CombatRng.rollD6() != 1) continue

            val location = LocationRepository.findById(locationId) ?: continue

            // Don't spawn wandering monsters in safe/town areas
            if (location.areaId == "keep-on-the-borderlands") continue

            // Pick the right encounter table
            val entries = getEncounterTable(location)
            if (entries.isEmpty()) continue

            // Filter by location's effective difficulty
            val locationDifficulty = estimateLocationDifficulty(location)
            val eligible = entries.filter {
                locationDifficulty in it.minChallengeRating..it.maxChallengeRating
            }
            if (eligible.isEmpty()) continue

            // Weighted random selection
            val selected = weightedRandom(eligible) ?: continue
            val creature = CreatureRepository.findById(selected.creatureId) ?: continue

            // Determine count
            val count = if (selected.minCount == selected.maxCount) {
                selected.minCount
            } else {
                selected.minCount + Random.nextInt(selected.maxCount - selected.minCount + 1)
            }

            // Spawn the creature(s) at the location
            repeat(count) {
                LocationRepository.addCreatureToLocation(locationId, creature.id)
                activeWanderingMonsters.add(WanderingSpawn(locationId, creature.id, currentTick))
            }

            // Broadcast to players at this location
            runBlocking {
                val updatedLocation = LocationRepository.findById(locationId) ?: return@runBlocking
                val spawnMessage = if (count > 1) {
                    "${count}x ${creature.name}"
                } else {
                    creature.name
                }
                LocationEventService.broadcastCreatureAdded(updatedLocation, creature.id, spawnMessage)
            }

            log.info("Wandering monster spawned: ${count}x ${creature.name} at ${location.name} (biome=${location.biome}, difficulty=$locationDifficulty)")
        }
    }

    /**
     * Remove wandering monsters that have been alive too long without engagement.
     */
    private fun processDespawns(currentTick: Long) {
        val expired = activeWanderingMonsters.filter { spawn ->
            currentTick - spawn.spawnTick > DESPAWN_TIMEOUT_TICKS
        }

        for (spawn in expired) {
            // Check if the creature is still at the location (might have been killed)
            val location = LocationRepository.findById(spawn.locationId) ?: continue
            if (spawn.creatureId in location.creatureIds) {
                LocationRepository.removeCreatureFromLocation(spawn.locationId, spawn.creatureId)
                runBlocking {
                    val creature = CreatureRepository.findById(spawn.creatureId)
                    val updatedLocation = LocationRepository.findById(spawn.locationId) ?: return@runBlocking
                    LocationEventService.broadcastCreatureRemoved(
                        updatedLocation,
                        spawn.creatureId,
                        creature?.name ?: "creature"
                    )
                }
                log.info("Despawned wandering monster ${spawn.creatureId} from ${spawn.locationId}")
            }
            activeWanderingMonsters.remove(spawn)
        }
    }

    /**
     * Notify that a creature was killed at a location.
     * If it was a wandering monster, clean up our tracking.
     */
    fun onCreatureKilled(locationId: String, creatureId: String) {
        activeWanderingMonsters.removeIf {
            it.locationId == locationId && it.creatureId == creatureId
        }
    }

    /**
     * Check if a creature at a location is a wandering monster.
     */
    fun isWanderingMonster(locationId: String, creatureId: String): Boolean {
        return activeWanderingMonsters.any {
            it.locationId == locationId && it.creatureId == creatureId
        }
    }

    /**
     * Movement-triggered encounter check. When a player moves between outdoor
     * wilderness locations, there's a 1-in-6 chance of spawning a biome-appropriate
     * creature at the destination.
     *
     * @return A flavor message if an encounter was spawned, or null if no encounter.
     */
    fun checkMovementEncounter(userId: String, fromLocation: Location?, toLocation: Location): String? {
        // Only trigger for outdoor-to-outdoor wilderness movement
        if (toLocation.locationType != LocationType.OUTDOOR_GROUND) return null
        if (fromLocation?.locationType != LocationType.OUTDOOR_GROUND) return null

        // No encounters in safe zones
        if (toLocation.areaId == "keep-on-the-borderlands") return null

        // 1-in-6 chance
        if (CombatRng.rollD6() != 1) return null

        // Find appropriate encounter table
        val entries = getEncounterTable(toLocation)
        if (entries.isEmpty()) return null

        // Filter by destination difficulty
        val locationDifficulty = estimateLocationDifficulty(toLocation)
        val eligible = entries.filter {
            locationDifficulty in it.minChallengeRating..it.maxChallengeRating
        }
        if (eligible.isEmpty()) return null

        // Weighted random selection
        val selected = weightedRandom(eligible) ?: return null
        val creature = CreatureRepository.findById(selected.creatureId) ?: return null

        // Determine count
        val count = if (selected.minCount == selected.maxCount) {
            selected.minCount
        } else {
            selected.minCount + Random.nextInt(selected.maxCount - selected.minCount + 1)
        }

        // Spawn the creature(s) at the destination
        val currentTick = GameTickService.getCurrentTick()
        repeat(count) {
            LocationRepository.addCreatureToLocation(toLocation.id, creature.id)
            activeWanderingMonsters.add(WanderingSpawn(toLocation.id, creature.id, currentTick))
        }

        // Broadcast to players at this location
        runBlocking {
            val updatedLocation = LocationRepository.findById(toLocation.id) ?: return@runBlocking
            val spawnMessage = if (count > 1) "${count}x ${creature.name}" else creature.name
            LocationEventService.broadcastCreatureAdded(updatedLocation, creature.id, spawnMessage)
        }

        val flavorMessage = if (count > 1) {
            "A pack of ${creature.name}s emerges from the wilderness!"
        } else {
            "A ${creature.name} emerges from the undergrowth!"
        }

        log.info("Movement encounter spawned: ${count}x ${creature.name} at ${toLocation.name} for user $userId (biome=${toLocation.biome}, difficulty=$locationDifficulty)")

        return flavorMessage
    }

    /**
     * Get the appropriate encounter table for a location.
     */
    private fun getEncounterTable(location: Location): List<WanderingEncounterEntry> {
        // Try biome-specific table first
        val biome = location.biome?.uppercase()
        if (biome != null && encounterTables.containsKey(biome)) {
            return encounterTables[biome]!!
        }

        // For indoor/underground locations, use the indoor table
        if (location.locationType == LocationType.UNDERGROUND || location.locationType == LocationType.INDOOR) {
            return indoorEncounterTable
        }

        // Fallback: try generic outdoor table
        return encounterTables["GENERIC"] ?: emptyList()
    }

    /**
     * Estimate a location's difficulty based on its resident creatures.
     */
    private fun estimateLocationDifficulty(location: Location): Int {
        if (location.creatureIds.isEmpty()) return 1

        val maxCR = location.creatureIds.mapNotNull { creatureId ->
            CreatureRepository.findById(creatureId)?.challengeRating
        }.maxOrNull() ?: 1

        return maxCR
    }

    /**
     * Weighted random selection from entries.
     */
    private fun weightedRandom(entries: List<WanderingEncounterEntry>): WanderingEncounterEntry? {
        if (entries.isEmpty()) return null
        val totalWeight = entries.sumOf { it.weight }
        if (totalWeight <= 0) return null

        var roll = Random.nextInt(totalWeight)
        for (entry in entries) {
            roll -= entry.weight
            if (roll < 0) return entry
        }
        return entries.last()
    }
}
