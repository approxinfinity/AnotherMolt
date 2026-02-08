package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.Creature
import com.ez2bg.anotherthread.database.CreatureRepository
import com.ez2bg.anotherthread.database.Location
import com.ez2bg.anotherthread.database.LocationRepository
import com.ez2bg.anotherthread.events.LocationEventService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

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
 * When a creature dies, we record which creature type died at which location.
 * After a delay, we respawn creatures to maintain location quotas.
 *
 * The "quota" for each location is determined by the initial creature count
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
     * Tracks pending respawns: when a creature dies, we record it here.
     * Key: locationId, Value: list of (creatureId, tickWhenEligible)
     */
    private val pendingRespawns = ConcurrentHashMap<String, MutableList<PendingRespawn>>()

    data class PendingRespawn(
        val creatureId: String,
        val eligibleAtTick: Long
    )

    /**
     * Initialize quotas from current world state.
     * Call this once at server startup BEFORE any creatures are killed.
     */
    fun initializeQuotas() {
        val locations = LocationRepository.findAll()
        var totalCreatures = 0

        for (location in locations) {
            if (location.creatureIds.isNotEmpty()) {
                // Count how many of each creature type are at this location
                val creatureCounts = location.creatureIds.groupingBy { it }.eachCount()
                locationQuotas[location.id] = creatureCounts
                totalCreatures += location.creatureIds.size
            }
        }

        log.info("Initialized creature quotas for ${locationQuotas.size} locations, $totalCreatures total creatures")
    }

    /**
     * Record a creature death for later respawn.
     * Called when a creature is killed in combat.
     */
    fun recordCreatureDeath(locationId: String, creatureId: String, currentTick: Long) {
        val eligibleTick = currentTick + RespawnConfig.MIN_RESPAWN_DELAY_TICKS

        val pending = pendingRespawns.computeIfAbsent(locationId) { mutableListOf() }
        synchronized(pending) {
            pending.add(PendingRespawn(creatureId, eligibleTick))
        }

        val creature = CreatureRepository.findById(creatureId)
        log.debug("Recorded death of ${creature?.name ?: creatureId} at $locationId, eligible for respawn at tick $eligibleTick")
    }

    /**
     * Process respawns for all locations.
     * Called periodically from GameTickService.
     */
    fun processRespawns(currentTick: Long) {
        var totalRespawned = 0

        for ((locationId, pending) in pendingRespawns) {
            if (totalRespawned >= RespawnConfig.MAX_RESPAWNS_PER_TICK) break

            val location = LocationRepository.findById(locationId) ?: continue
            val quota = locationQuotas[locationId] ?: continue

            // Find creatures eligible for respawn
            val eligibleRespawns = synchronized(pending) {
                pending.filter { it.eligibleAtTick <= currentTick }.toList()
            }

            if (eligibleRespawns.isEmpty()) continue

            // Check current creature counts vs quota
            val currentCounts = location.creatureIds.groupingBy { it }.eachCount()

            for (respawn in eligibleRespawns) {
                if (totalRespawned >= RespawnConfig.MAX_RESPAWNS_PER_TICK) break

                val creatureId = respawn.creatureId
                val quotaCount = quota[creatureId] ?: 0
                val currentCount = currentCounts[creatureId] ?: 0

                if (currentCount < quotaCount) {
                    // Respawn this creature
                    val creature = CreatureRepository.findById(creatureId)
                    if (creature != null) {
                        // Add creature back to location
                        val updatedLocation = location.copy(
                            creatureIds = location.creatureIds + creatureId
                        )
                        LocationRepository.update(updatedLocation)

                        // Broadcast creature spawn to all players at this location
                        runBlocking {
                            LocationEventService.broadcastCreatureAdded(updatedLocation, creature.id, creature.name)
                        }

                        // Remove from pending
                        synchronized(pending) {
                            pending.remove(respawn)
                        }

                        totalRespawned++
                        log.info("Respawned ${creature.name} at ${location.name}")
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
     * Get pending respawn count for a location.
     */
    fun getPendingRespawnCount(locationId: String): Int {
        return pendingRespawns[locationId]?.size ?: 0
    }

    /**
     * Clear all respawn data (for testing or reset).
     */
    fun reset() {
        locationQuotas.clear()
        pendingRespawns.clear()
    }
}
