package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.GameConfigRepository
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Central game configuration that reads from the database.
 *
 * Values are cached for performance, with a configurable refresh interval.
 * Call refresh() to reload all values from the database.
 *
 * Usage:
 *   val roundDuration = GameConfig.combat.roundDurationMs
 *   val respawnDelay = GameConfig.respawn.minDelayTicks
 */
object GameConfig {
    private val log = LoggerFactory.getLogger(GameConfig::class.java)

    // Cache for config values with last refresh time
    private val cache = ConcurrentHashMap<String, CachedValue<*>>()
    private var lastRefresh: Long = 0
    private const val CACHE_TTL_MS = 60_000L  // Refresh cache every minute

    private data class CachedValue<T>(val value: T, val timestamp: Long)

    /**
     * Initialize the config system. Call this at server startup.
     */
    fun initialize() {
        // Ensure defaults exist in DB
        GameConfigRepository.initializeDefaults()
        // Load all values into cache
        refresh()
        log.info("GameConfig initialized with ${cache.size} values")
    }

    /**
     * Force refresh all cached values from the database.
     */
    fun refresh() {
        cache.clear()
        lastRefresh = System.currentTimeMillis()
        // Pre-load commonly used values
        combat.roundDurationMs
        combat.fleeSuccessChance
        respawn.checkIntervalTicks
        respawn.minDelayTicks
        log.debug("GameConfig cache refreshed")
    }

    private inline fun <reified T> getCached(key: String, crossinline loader: () -> T): T {
        val now = System.currentTimeMillis()

        // Check if cache needs refresh
        if (now - lastRefresh > CACHE_TTL_MS) {
            cache.clear()
            lastRefresh = now
        }

        @Suppress("UNCHECKED_CAST")
        val cached = cache[key] as? CachedValue<T>
        if (cached != null) {
            return cached.value
        }

        val value = loader()
        cache[key] = CachedValue(value, now)
        return value
    }

    // =========================================================================
    // COMBAT CONFIGURATION
    // =========================================================================

    object combat {
        /** Duration of each combat round in milliseconds */
        val roundDurationMs: Long
            get() = getCached("combat.roundDurationMs") {
                GameConfigRepository.getLong("combat.roundDurationMs", 3000L)
            }

        /** Maximum wait time for slow connections */
        val maxRoundDurationMs: Long
            get() = getCached("combat.maxRoundDurationMs") {
                GameConfigRepository.getLong("combat.maxRoundDurationMs", 5000L)
            }

        /** Base chance to successfully flee (0.0-1.0) */
        val fleeSuccessChance: Double
            get() = getCached("combat.fleeSuccessChance") {
                GameConfigRepository.getDouble("combat.fleeSuccessChance", 0.5)
            }

        /** Rounds before player can attempt to flee again */
        val fleeCooldownRounds: Int
            get() = getCached("combat.fleeCooldownRounds") {
                GameConfigRepository.getInt("combat.fleeCooldownRounds", 2)
            }

        /** Maximum rounds before combat times out */
        val maxCombatRounds: Int
            get() = getCached("combat.maxCombatRounds") {
                GameConfigRepository.getInt("combat.maxCombatRounds", 100)
            }

        /** Combat session timeout in milliseconds */
        val sessionTimeoutMs: Long
            get() = getCached("combat.sessionTimeoutMs") {
                GameConfigRepository.getLong("combat.sessionTimeoutMs", 300000L)
            }

        /** Average HP per hit die */
        val hpPerHitDie: Int
            get() = getCached("combat.hpPerHitDie") {
                GameConfigRepository.getInt("combat.hpPerHitDie", 6)
            }

        /** Mana restored per combat round */
        val manaRegenPerRound: Int
            get() = getCached("combat.manaRegenPerRound") {
                GameConfigRepository.getInt("combat.manaRegenPerRound", 1)
            }

        /** Stamina restored per combat round */
        val staminaRegenPerRound: Int
            get() = getCached("combat.staminaRegenPerRound") {
                GameConfigRepository.getInt("combat.staminaRegenPerRound", 2)
            }
    }

    // =========================================================================
    // RESPAWN CONFIGURATION
    // =========================================================================

    object respawn {
        /** Ticks between respawn checks */
        val checkIntervalTicks: Int
            get() = getCached("respawn.checkIntervalTicks") {
                GameConfigRepository.getInt("respawn.checkIntervalTicks", 10)
            }

        /** Minimum ticks before creature can respawn */
        val minDelayTicks: Int
            get() = getCached("respawn.minDelayTicks") {
                GameConfigRepository.getInt("respawn.minDelayTicks", 20)
            }

        /** Maximum creatures to respawn per tick */
        val maxPerTick: Int
            get() = getCached("respawn.maxPerTick") {
                GameConfigRepository.getInt("respawn.maxPerTick", 3)
            }
    }

    // =========================================================================
    // DEATH CONFIGURATION
    // =========================================================================

    object death {
        /** Location ID where players respawn after death */
        val respawnLocationId: String
            get() = getCached("death.respawnLocationId") {
                GameConfigRepository.getString("death.respawnLocationId", "tun-du-lac-inn")
            }

        /** Whether players drop items on death */
        val dropItemsOnDeath: Boolean
            get() = getCached("death.dropItemsOnDeath") {
                GameConfigRepository.getBoolean("death.dropItemsOnDeath", false)
            }

        /** Whether players drop gold on death */
        val dropGoldOnDeath: Boolean
            get() = getCached("death.dropGoldOnDeath") {
                GameConfigRepository.getBoolean("death.dropGoldOnDeath", false)
            }

        /** Percentage of gold dropped on death (0.0-1.0) */
        val goldDropPercent: Double
            get() = getCached("death.goldDropPercent") {
                GameConfigRepository.getDouble("death.goldDropPercent", 0.1)
            }
    }

    // =========================================================================
    // XP CONFIGURATION
    // =========================================================================

    object xp {
        /** Base XP for defeating a creature */
        val baseCreatureXp: Int
            get() = getCached("xp.baseCreatureXp") {
                GameConfigRepository.getInt("xp.baseCreatureXp", 50)
            }

        /** XP modifier per level difference */
        val levelDifferenceMultiplier: Double
            get() = getCached("xp.levelDifferenceMultiplier") {
                GameConfigRepository.getDouble("xp.levelDifferenceMultiplier", 0.1)
            }

        /** Minimum XP percentage even for trivial kills */
        val minXpPercent: Double
            get() = getCached("xp.minXpPercent") {
                GameConfigRepository.getDouble("xp.minXpPercent", 0.1)
            }
    }

    // =========================================================================
    // REGEN CONFIGURATION (out of combat)
    // =========================================================================

    object regen {
        /** Base HP regeneration per game tick */
        val baseHpPerTick: Int
            get() = getCached("regen.baseHpPerTick") {
                GameConfigRepository.getInt("regen.baseHpPerTick", 1)
            }

        /** Base mana regeneration per game tick */
        val baseManaPerTick: Int
            get() = getCached("regen.baseManaPerTick") {
                GameConfigRepository.getInt("regen.baseManaPerTick", 1)
            }

        /** Base stamina regeneration per game tick */
        val baseStaminaPerTick: Int
            get() = getCached("regen.baseStaminaPerTick") {
                GameConfigRepository.getInt("regen.baseStaminaPerTick", 2)
            }
    }
}
