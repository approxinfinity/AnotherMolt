package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.Creature
import com.ez2bg.anotherthread.database.CreatureRepository
import com.ez2bg.anotherthread.database.Location
import com.ez2bg.anotherthread.database.LocationRepository
import com.ez2bg.anotherthread.events.LocationEventService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Configuration for creature respawn mechanics.
 * Now delegates to GameConfig for DB-backed values.
 */
object RespawnConfig {
    /** How many ticks between respawn checks (e.g., 10 ticks = 30 seconds at 3s/tick) */
    val RESPAWN_CHECK_INTERVAL_TICKS: Int get() = GameConfig.respawn.checkIntervalTicks

    /** Minimum time (in ticks) before a creature can respawn after being killed */
    val MIN_RESPAWN_DELAY_TICKS: Int get() = GameConfig.respawn.minDelayTicks

    /** Maximum creatures to respawn per tick (prevents sudden mob swarms) */
    val MAX_RESPAWNS_PER_TICK: Int get() = GameConfig.respawn.maxPerTick
}

/**
 * Tracks creature deaths and handles respawning.
 *
 * When a creature dies, we record which creature type died and which area it was in.
 * After a delay, we respawn the creature at a random location within the same area.
 *
 * The "quota" for each area is determined by the initial creature count
 * when the server first starts (or from seed data).
 */
object CreatureRespawnService {
    private val log = LoggerFactory.getLogger(CreatureRespawnService::class.java)

    /**
     * Tracks the original creature quota per location.
     * Key: locationId, Value: map of creatureId -> count
     */
    private val locationQuotas = ConcurrentHashMap<String, Map<String, Int>>()

    /**
     * Tracks area quotas: total creatures of each type per area.
     * Key: areaId, Value: map of creatureId -> count
     */
    private val areaQuotas = ConcurrentHashMap<String, MutableMap<String, Int>>()

    /**
     * Cache of location IDs per area for fast random selection.
     * Key: areaId, Value: list of locationIds in that area
     */
    private val areaLocations = ConcurrentHashMap<String, List<String>>()

    /**
     * Tracks pending respawns: when a creature dies, we record it here.
     * Key: areaId, Value: list of (creatureId, tickWhenEligible, originalLocationId)
     */
    private val pendingRespawns = ConcurrentHashMap<String, MutableList<PendingRespawn>>()

    data class PendingRespawn(
        val creatureId: String,
        val eligibleAtTick: Long,
        val originalLocationId: String  // For logging purposes
    )

    /**
     * Initialize quotas from current world state.
     * Call this once at server startup BEFORE any creatures are killed.
     */
    fun initializeQuotas() {
        val locations = LocationRepository.findAll()
        var totalCreatures = 0

        // Build area location cache and area quotas
        val areaLocationMap = mutableMapOf<String, MutableList<String>>()

        for (location in locations) {
            val areaId = location.areaId ?: "overworld"

            // Add to area location cache
            areaLocationMap.computeIfAbsent(areaId) { mutableListOf() }.add(location.id)

            if (location.creatureIds.isNotEmpty()) {
                // Count how many of each creature type are at this location
                val creatureCounts = location.creatureIds.groupingBy { it }.eachCount()
                locationQuotas[location.id] = creatureCounts
                totalCreatures += location.creatureIds.size

                // Aggregate into area quotas
                val areaQuota = areaQuotas.computeIfAbsent(areaId) { mutableMapOf() }
                for ((creatureId, count) in creatureCounts) {
                    areaQuota[creatureId] = (areaQuota[creatureId] ?: 0) + count
                }
            }
        }

        // Store area location cache
        for ((areaId, locationIds) in areaLocationMap) {
            areaLocations[areaId] = locationIds
        }

        log.info("Initialized creature quotas for ${locationQuotas.size} locations, $totalCreatures total creatures")
        log.info("Initialized area quotas for ${areaQuotas.size} areas: ${areaQuotas.mapValues { it.value.values.sum() }}")
    }

    /**
     * Record a creature death for later respawn.
     * Called when a creature is killed in combat.
     */
    fun recordCreatureDeath(locationId: String, creatureId: String, currentTick: Long) {
        val location = LocationRepository.findById(locationId)
        val areaId = location?.areaId ?: "overworld"
        val eligibleTick = currentTick + RespawnConfig.MIN_RESPAWN_DELAY_TICKS

        val pending = pendingRespawns.computeIfAbsent(areaId) { mutableListOf() }
        synchronized(pending) {
            pending.add(PendingRespawn(creatureId, eligibleTick, locationId))
        }

        val creature = CreatureRepository.findById(creatureId)
        log.debug("Recorded death of ${creature?.name ?: creatureId} in area $areaId, eligible for respawn at tick $eligibleTick")
    }

    /**
     * Process respawns for all areas.
     * Creatures respawn at a random location within their original area.
     * Called periodically from GameTickService.
     */
    fun processRespawns(currentTick: Long) {
        var totalRespawned = 0

        for ((areaId, pending) in pendingRespawns) {
            if (totalRespawned >= RespawnConfig.MAX_RESPAWNS_PER_TICK) break

            val areaQuota = areaQuotas[areaId] ?: continue
            val locationsInArea = areaLocations[areaId] ?: continue

            if (locationsInArea.isEmpty()) continue

            // Find creatures eligible for respawn
            val eligibleRespawns = synchronized(pending) {
                pending.filter { it.eligibleAtTick <= currentTick }.toList()
            }

            if (eligibleRespawns.isEmpty()) continue

            // Count current creatures of each type in the entire area
            val currentAreaCounts = mutableMapOf<String, Int>()
            for (locId in locationsInArea) {
                val loc = LocationRepository.findById(locId) ?: continue
                for (creatureId in loc.creatureIds) {
                    currentAreaCounts[creatureId] = (currentAreaCounts[creatureId] ?: 0) + 1
                }
            }

            for (respawn in eligibleRespawns) {
                if (totalRespawned >= RespawnConfig.MAX_RESPAWNS_PER_TICK) break

                val creatureId = respawn.creatureId
                val quotaCount = areaQuota[creatureId] ?: 0
                val currentCount = currentAreaCounts[creatureId] ?: 0

                if (currentCount < quotaCount) {
                    // Respawn this creature at a random location in the area
                    val creature = CreatureRepository.findById(creatureId)
                    if (creature != null) {
                        // Pick a random location in the area
                        val targetLocationId = locationsInArea[Random.nextInt(locationsInArea.size)]
                        val targetLocation = LocationRepository.findById(targetLocationId)

                        if (targetLocation != null) {
                            // Add creature to the target location
                            val updatedLocation = targetLocation.copy(
                                creatureIds = targetLocation.creatureIds + creatureId
                            )
                            LocationRepository.update(updatedLocation)

                            // Broadcast creature spawn to all players at this location
                            runBlocking {
                                LocationEventService.broadcastCreatureAdded(updatedLocation, creature.id, creature.name)
                            }

                            // Update our local count
                            currentAreaCounts[creatureId] = currentCount + 1

                            totalRespawned++
                            log.info("Respawned ${creature.name} at ${targetLocation.name} (area: $areaId)")
                        }

                        // Remove from pending
                        synchronized(pending) {
                            pending.remove(respawn)
                        }
                    } else {
                        // Creature no longer exists, remove from pending
                        synchronized(pending) {
                            pending.remove(respawn)
                        }
                    }
                } else {
                    // Quota already met (maybe another creature of same type respawned)
                    synchronized(pending) {
                        pending.remove(respawn)
                    }
                }
            }
        }

        if (totalRespawned > 0) {
            log.debug("Respawned $totalRespawned creature(s) this tick")
        }
    }

    /**
     * Get the quota for a specific location.
     * Returns a map of creatureId -> count.
     */
    fun getQuotaForLocation(locationId: String): Map<String, Int> {
        return locationQuotas[locationId] ?: emptyMap()
    }

    /**
     * Get the quota for a specific area.
     * Returns a map of creatureId -> count.
     */
    fun getQuotaForArea(areaId: String): Map<String, Int> {
        return areaQuotas[areaId] ?: emptyMap()
    }

    /**
     * Get pending respawn count for an area.
     */
    fun getPendingRespawnCount(areaId: String): Int {
        return pendingRespawns[areaId]?.size ?: 0
    }

    /**
     * Get all pending respawns for debugging/admin purposes.
     */
    fun getAllPendingRespawns(): Map<String, List<PendingRespawn>> {
        return pendingRespawns.mapValues { it.value.toList() }
    }

    /**
     * Clear all respawn data (for testing or reset).
     */
    fun reset() {
        locationQuotas.clear()
        areaQuotas.clear()
        areaLocations.clear()
        pendingRespawns.clear()
    }
}
