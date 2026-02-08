package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Data class representing a game configuration entry.
 */
@Serializable
data class GameConfigEntry(
    val key: String,
    val value: String,
    val description: String?,
    val category: String,
    val valueType: String,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Repository for managing game configuration values.
 */
object GameConfigRepository {

    private fun ResultRow.toGameConfigEntry() = GameConfigEntry(
        key = this[GameConfigTable.key],
        value = this[GameConfigTable.value],
        description = this[GameConfigTable.description],
        category = this[GameConfigTable.category],
        valueType = this[GameConfigTable.valueType],
        updatedAt = this[GameConfigTable.updatedAt]
    )

    /**
     * Get a config value by key, or null if not found.
     */
    fun get(key: String): GameConfigEntry? = transaction {
        GameConfigTable.selectAll()
            .where { GameConfigTable.key eq key }
            .map { it.toGameConfigEntry() }
            .singleOrNull()
    }

    /**
     * Get a string config value, with a default fallback.
     */
    fun getString(key: String, default: String): String = transaction {
        GameConfigTable.selectAll()
            .where { GameConfigTable.key eq key }
            .map { it[GameConfigTable.value] }
            .singleOrNull() ?: default
    }

    /**
     * Get an integer config value, with a default fallback.
     */
    fun getInt(key: String, default: Int): Int = transaction {
        GameConfigTable.selectAll()
            .where { GameConfigTable.key eq key }
            .map { it[GameConfigTable.value].toIntOrNull() }
            .singleOrNull() ?: default
    }

    /**
     * Get a long config value, with a default fallback.
     */
    fun getLong(key: String, default: Long): Long = transaction {
        GameConfigTable.selectAll()
            .where { GameConfigTable.key eq key }
            .map { it[GameConfigTable.value].toLongOrNull() }
            .singleOrNull() ?: default
    }

    /**
     * Get a double config value, with a default fallback.
     */
    fun getDouble(key: String, default: Double): Double = transaction {
        GameConfigTable.selectAll()
            .where { GameConfigTable.key eq key }
            .map { it[GameConfigTable.value].toDoubleOrNull() }
            .singleOrNull() ?: default
    }

    /**
     * Get a boolean config value, with a default fallback.
     */
    fun getBoolean(key: String, default: Boolean): Boolean = transaction {
        GameConfigTable.selectAll()
            .where { GameConfigTable.key eq key }
            .map { it[GameConfigTable.value].toBooleanStrictOrNull() }
            .singleOrNull() ?: default
    }

    /**
     * Set a config value. Creates if doesn't exist, updates if it does.
     */
    fun set(
        key: String,
        value: String,
        description: String? = null,
        category: String = "general",
        valueType: String = "string"
    ): Boolean = transaction {
        val exists = GameConfigTable.selectAll()
            .where { GameConfigTable.key eq key }
            .count() > 0

        if (exists) {
            GameConfigTable.update({ GameConfigTable.key eq key }) {
                it[GameConfigTable.value] = value
                if (description != null) it[GameConfigTable.description] = description
                it[updatedAt] = System.currentTimeMillis()
            } > 0
        } else {
            GameConfigTable.insert {
                it[GameConfigTable.key] = key
                it[GameConfigTable.value] = value
                it[GameConfigTable.description] = description
                it[GameConfigTable.category] = category
                it[GameConfigTable.valueType] = valueType
                it[updatedAt] = System.currentTimeMillis()
            }
            true
        }
    }

    /**
     * Set an integer config value.
     */
    fun setInt(key: String, value: Int, description: String? = null, category: String = "general"): Boolean =
        set(key, value.toString(), description, category, "int")

    /**
     * Set a long config value.
     */
    fun setLong(key: String, value: Long, description: String? = null, category: String = "general"): Boolean =
        set(key, value.toString(), description, category, "long")

    /**
     * Set a double config value.
     */
    fun setDouble(key: String, value: Double, description: String? = null, category: String = "general"): Boolean =
        set(key, value.toString(), description, category, "double")

    /**
     * Set a boolean config value.
     */
    fun setBoolean(key: String, value: Boolean, description: String? = null, category: String = "general"): Boolean =
        set(key, value.toString(), description, category, "boolean")

    /**
     * Delete a config value.
     */
    fun delete(key: String): Boolean = transaction {
        GameConfigTable.deleteWhere { GameConfigTable.key eq key } > 0
    }

    /**
     * Get all config values.
     */
    fun findAll(): List<GameConfigEntry> = transaction {
        GameConfigTable.selectAll()
            .orderBy(GameConfigTable.category to SortOrder.ASC, GameConfigTable.key to SortOrder.ASC)
            .map { it.toGameConfigEntry() }
    }

    /**
     * Get all config values in a category.
     */
    fun findByCategory(category: String): List<GameConfigEntry> = transaction {
        GameConfigTable.selectAll()
            .where { GameConfigTable.category eq category }
            .orderBy(GameConfigTable.key to SortOrder.ASC)
            .map { it.toGameConfigEntry() }
    }

    /**
     * Initialize default config values if they don't exist.
     * This is called at server startup.
     */
    fun initializeDefaults() {
        // Combat config
        setIfNotExists("combat.roundDurationMs", "3000", "Duration of each combat round in milliseconds", "combat", "long")
        setIfNotExists("combat.maxRoundDurationMs", "5000", "Maximum wait time for slow connections", "combat", "long")
        setIfNotExists("combat.fleeSuccessChance", "0.5", "Base chance to successfully flee (0.0-1.0)", "combat", "double")
        setIfNotExists("combat.fleeCooldownRounds", "2", "Rounds before player can attempt to flee again", "combat", "int")
        setIfNotExists("combat.maxCombatRounds", "100", "Maximum rounds before combat times out", "combat", "int")
        setIfNotExists("combat.sessionTimeoutMs", "300000", "Combat session timeout in milliseconds", "combat", "long")
        setIfNotExists("combat.hpPerHitDie", "6", "Average HP per hit die", "combat", "int")
        setIfNotExists("combat.manaRegenPerRound", "1", "Mana restored per combat round", "combat", "int")
        setIfNotExists("combat.staminaRegenPerRound", "2", "Stamina restored per combat round", "combat", "int")

        // Respawn config
        setIfNotExists("respawn.checkIntervalTicks", "10", "Ticks between respawn checks", "respawn", "int")
        setIfNotExists("respawn.minDelayTicks", "20", "Minimum ticks before creature can respawn", "respawn", "int")
        setIfNotExists("respawn.maxPerTick", "3", "Maximum creatures to respawn per tick", "respawn", "int")

        // Death config
        setIfNotExists("death.respawnLocationId", "tun-du-lac-inn", "Location ID where players respawn after death", "death", "string")
        setIfNotExists("death.dropItemsOnDeath", "true", "Whether players drop items on death", "death", "boolean")
        setIfNotExists("death.dropGoldOnDeath", "true", "Whether players drop gold on death", "death", "boolean")
        setIfNotExists("death.goldDropPercent", "0.1", "Percentage of gold dropped on death (0.0-1.0)", "death", "double")

        // XP config
        setIfNotExists("xp.baseCreatureXp", "50", "Base XP for defeating a creature", "xp", "int")
        setIfNotExists("xp.levelDifferenceMultiplier", "0.1", "XP modifier per level difference", "xp", "double")
        setIfNotExists("xp.minXpPercent", "0.1", "Minimum XP percentage even for trivial kills", "xp", "double")

        // Regen config (out of combat)
        setIfNotExists("regen.baseHpPerTick", "1", "Base HP regeneration per game tick", "regen", "int")
        setIfNotExists("regen.baseManaPerTick", "1", "Base mana regeneration per game tick", "regen", "int")
        setIfNotExists("regen.baseStaminaPerTick", "2", "Base stamina regeneration per game tick", "regen", "int")

        // Spawn/starting location config
        setIfNotExists("spawn.defaultLocationId", TunDuLacSeed.TUN_DU_LAC_OVERWORLD_ID, "Default starting location for new players", "spawn", "string")
        setIfNotExists("spawn.defaultRespawnLocationId", TunDuLacSeed.TUN_DU_LAC_OVERWORLD_ID, "Default respawn location after death", "spawn", "string")
    }

    /**
     * Get the default starting location ID for new players.
     */
    fun getDefaultStartingLocationId(): String =
        getString("spawn.defaultLocationId", TunDuLacSeed.TUN_DU_LAC_OVERWORLD_ID)

    /**
     * Get the default respawn location ID after death.
     */
    fun getDefaultRespawnLocationId(): String =
        getString("spawn.defaultRespawnLocationId", TunDuLacSeed.TUN_DU_LAC_OVERWORLD_ID)

    /**
     * Set a config value only if it doesn't already exist.
     */
    private fun setIfNotExists(
        key: String,
        value: String,
        description: String,
        category: String,
        valueType: String
    ) {
        if (get(key) == null) {
            set(key, value, description, category, valueType)
        }
    }
}
