package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.random.Random

/**
 * Service for tracking player and creature movement through locations.
 *
 * Records movement "trails" that players with the Track ability can detect.
 * Trails fade over time - older trails are harder to detect.
 *
 * Inspired by classic MUD/D&D tracking mechanics.
 */
object TrackingService {
    private val log = org.slf4j.LoggerFactory.getLogger("TrackingService")

    // Trail freshness thresholds (in milliseconds)
    private const val FRESH_TRAIL_MS = 5 * 60 * 1000L      // 5 minutes - very fresh, easy to track
    private const val RECENT_TRAIL_MS = 30 * 60 * 1000L   // 30 minutes - recent, moderate difficulty
    private const val OLD_TRAIL_MS = 2 * 60 * 60 * 1000L  // 2 hours - old, hard to track
    private const val MAX_TRAIL_AGE_MS = 24 * 60 * 60 * 1000L  // 24 hours - trails expire

    // Classes that get tracking bonuses
    private val trackerClasses = setOf(
        "ranger", "hunter", "scout", "tracker", "druid", "beastmaster", "survivalist"
    )

    /**
     * Represents a detected trail at a location.
     */
    @Serializable
    data class DetectedTrail(
        val entityType: String,        // "player" or "creature"
        val entityName: String,
        val directionFrom: String?,    // Where they came from (null if spawned/logged in)
        val directionTo: String?,      // Where they went (null if despawned/logged out/still here)
        val freshness: String,         // "fresh", "recent", "old", "fading"
        val minutesAgo: Int
    )

    /**
     * Result of a tracking attempt.
     */
    @Serializable
    data class TrackResult(
        val success: Boolean,
        val message: String,
        val trails: List<DetectedTrail> = emptyList()
    )

    /**
     * Check if a character class is a tracker-type class.
     */
    fun isTrackerClass(characterClass: CharacterClass?): Boolean {
        if (characterClass == null) return false
        val className = characterClass.name.lowercase()
        return trackerClasses.any { className.contains(it) }
    }

    /**
     * Get tracking class bonus (percentage added to tracking checks).
     */
    fun getTrackerClassBonus(characterClass: CharacterClass?): Int {
        return if (isTrackerClass(characterClass)) 30 else 0
    }

    /**
     * Record a movement trail when an entity moves through a location.
     *
     * @param locationId The location where the trail is being left
     * @param entityId The entity leaving the trail
     * @param entityType "player" or "creature"
     * @param entityName Name for display
     * @param directionFrom Direction they came from (null if spawned/logged in here)
     * @param directionTo Direction they went (null if still here or despawned)
     */
    fun recordMovement(
        locationId: String,
        entityId: String,
        entityType: String,
        entityName: String,
        directionFrom: String?,
        directionTo: String?
    ) {
        transaction {
            MovementTrailTable.insert {
                it[id] = UUID.randomUUID().toString()
                it[MovementTrailTable.locationId] = locationId
                it[MovementTrailTable.entityId] = entityId
                it[MovementTrailTable.entityType] = entityType
                it[MovementTrailTable.entityName] = entityName
                it[MovementTrailTable.directionFrom] = directionFrom
                it[MovementTrailTable.directionTo] = directionTo
                it[timestamp] = System.currentTimeMillis()
            }
        }
        log.debug("Recorded trail: $entityName ($entityType) at $locationId from=$directionFrom to=$directionTo")
    }

    /**
     * Attempt to track at the current location.
     *
     * @param user The player attempting to track
     * @param locationId The location to track at
     * @return TrackResult with detected trails
     */
    fun attemptTrack(user: User, locationId: String): TrackResult {
        // Can't track while in combat
        if (user.currentCombatSessionId != null) {
            return TrackResult(
                success = false,
                message = "You cannot focus on tracking while in combat!"
            )
        }

        // Calculate tracking chance
        // Base: 20% + (WIS mod * 5) + (level * 2) + classBonus
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
        val classBonus = getTrackerClassBonus(characterClass)
        val wisdomMod = StatModifierService.attributeModifier(user.wisdom)
        val trackingChance = (20 + (wisdomMod * 5) + (user.level * 2) + classBonus).coerceIn(5, 95)

        log.debug("${user.name} tracking with ${trackingChance}% chance (WIS=${user.wisdom}, level=${user.level}, classBonus=$classBonus)")

        // Get recent trails at this location
        val now = System.currentTimeMillis()
        val cutoffTime = now - MAX_TRAIL_AGE_MS

        val rawTrails = transaction {
            MovementTrailTable.selectAll()
                .where {
                    (MovementTrailTable.locationId eq locationId) and
                    (MovementTrailTable.timestamp greater cutoffTime) and
                    (MovementTrailTable.entityId neq user.id)  // Don't track your own trails
                }
                .orderBy(MovementTrailTable.timestamp, SortOrder.DESC)
                .limit(20)  // Limit to prevent information overload
                .map { row ->
                    RawTrail(
                        entityType = row[MovementTrailTable.entityType],
                        entityName = row[MovementTrailTable.entityName],
                        directionFrom = row[MovementTrailTable.directionFrom],
                        directionTo = row[MovementTrailTable.directionTo],
                        timestamp = row[MovementTrailTable.timestamp]
                    )
                }
        }

        if (rawTrails.isEmpty()) {
            return TrackResult(
                success = true,
                message = "You search the ground carefully but find no tracks or signs of passage.",
                trails = emptyList()
            )
        }

        // Roll for each trail - harder to detect older trails
        val detectedTrails = mutableListOf<DetectedTrail>()
        for (trail in rawTrails) {
            val age = now - trail.timestamp
            val ageModifier = when {
                age < FRESH_TRAIL_MS -> 20      // Fresh trails are easier
                age < RECENT_TRAIL_MS -> 0     // Normal difficulty
                age < OLD_TRAIL_MS -> -15      // Harder to detect
                else -> -30                    // Very hard (fading)
            }

            val adjustedChance = (trackingChance + ageModifier).coerceIn(5, 95)
            val roll = Random.nextInt(100)

            if (roll < adjustedChance) {
                val freshness = when {
                    age < FRESH_TRAIL_MS -> "fresh"
                    age < RECENT_TRAIL_MS -> "recent"
                    age < OLD_TRAIL_MS -> "old"
                    else -> "fading"
                }

                detectedTrails.add(
                    DetectedTrail(
                        entityType = trail.entityType,
                        entityName = trail.entityName,
                        directionFrom = trail.directionFrom,
                        directionTo = trail.directionTo,
                        freshness = freshness,
                        minutesAgo = (age / 60000).toInt()
                    )
                )
                log.debug("${user.name} detected trail of ${trail.entityName} (roll=$roll < $adjustedChance)")
            } else {
                log.debug("${user.name} missed trail of ${trail.entityName} (roll=$roll >= $adjustedChance)")
            }
        }

        // Build response message
        val message = when {
            detectedTrails.isEmpty() ->
                "You search for tracks but the signs are too faint to read."
            detectedTrails.size == 1 -> {
                val trail = detectedTrails[0]
                buildTrailDescription(trail)
            }
            else -> {
                "You detect multiple trails in the area."
            }
        }

        return TrackResult(
            success = true,
            message = message,
            trails = detectedTrails
        )
    }

    /**
     * Build a natural language description of a single trail.
     */
    private fun buildTrailDescription(trail: DetectedTrail): String {
        val freshnessDesc = when (trail.freshness) {
            "fresh" -> "very fresh"
            "recent" -> "recent"
            "old" -> "old"
            else -> "barely visible"
        }

        val entityDesc = if (trail.entityType == "player") {
            "A ${trail.entityType}'s"
        } else {
            "A ${trail.entityName}'s"
        }

        val directionDesc = when {
            trail.directionFrom != null && trail.directionTo != null ->
                "leading from the ${trail.directionFrom.lowercase()} toward the ${trail.directionTo.lowercase()}"
            trail.directionTo != null ->
                "leading ${trail.directionTo.lowercase()}"
            trail.directionFrom != null ->
                "coming from the ${trail.directionFrom.lowercase()}"
            else ->
                "appearing to start here"
        }

        return "You find $freshnessDesc tracks - $entityDesc trail $directionDesc."
    }

    /**
     * Clean up old trails to prevent database bloat.
     * Should be called periodically (e.g., hourly).
     */
    fun cleanupOldTrails(): Int {
        val cutoffTime = System.currentTimeMillis() - MAX_TRAIL_AGE_MS
        val deleted = transaction {
            MovementTrailTable.deleteWhere { timestamp less cutoffTime }
        }
        if (deleted > 0) {
            log.info("Cleaned up $deleted expired movement trails")
        }
        return deleted
    }

    /**
     * Raw trail data from database.
     */
    private data class RawTrail(
        val entityType: String,
        val entityName: String,
        val directionFrom: String?,
        val directionTo: String?,
        val timestamp: Long
    )
}

/**
 * Database table for movement trails.
 */
object MovementTrailTable : Table("movement_trail") {
    val id = varchar("id", 36)
    val locationId = varchar("location_id", 64)
    val entityId = varchar("entity_id", 36)
    val entityType = varchar("entity_type", 16)  // "player" or "creature"
    val entityName = text("entity_name")
    val directionFrom = text("direction_from").nullable()
    val directionTo = text("direction_to").nullable()
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(id)
}
