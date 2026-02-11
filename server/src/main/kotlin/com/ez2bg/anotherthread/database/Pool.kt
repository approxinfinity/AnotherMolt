package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * Effect types for magical pools.
 */
enum class PoolEffectType {
    HEALING,      // Restores HP, cures disease/poison
    DAMAGE,       // Deals damage (acid, fire, etc.)
    BUFF,         // Temporary stat boost
    DEBUFF,       // Temporary stat penalty or condition
    TRANSFORM,    // Changes appearance or form
    TELEPORT,     // Moves player to another location
    TREASURE,     // Contains treasure/items at bottom
    TRAP,         // Triggers a trap effect
    POISON,       // Slow-acting poison
    CHARM,        // Mind-affecting charm effect
    STRENGTH,     // Permanent or temporary strength change
    SPEED,        // Movement speed change
    WINE,         // Drinkable wine (mild effect)
    SLEEP,        // Puts player to sleep
    EMPTY,        // No effect (dried up or depleted)
    STRANGE       // Random or unpredictable effect
}

/**
 * Effect data for different pool types.
 */
@Serializable
data class PoolEffectData(
    // Healing effects
    val healAmount: Int? = null,           // Fixed healing amount
    val healDice: String? = null,          // e.g., "1d6+2"
    val curesDisease: Boolean = false,
    val curesPoison: Boolean = false,

    // Damage effects
    val damageAmount: Int? = null,
    val damageDice: String? = null,
    val damageType: String? = null,        // "acid", "fire", "cold", etc.

    // Stat effects (buff/debuff)
    val statModifier: String? = null,      // "strength", "dexterity", etc.
    val modifierAmount: Int? = null,
    val durationRounds: Int? = null,
    val durationMinutes: Int? = null,

    // Teleport
    val teleportLocationId: String? = null,

    // Treasure
    val containsItemId: String? = null,
    val goldAmount: Int? = null,

    // Trap
    val trapMessage: String? = null,

    // Condition effects
    val appliesCondition: String? = null,  // "poisoned", "charmed", "sleeping", etc.
    val conditionDuration: Int? = null,
    val conditionChance: Float? = null,    // Chance to apply (0.0 - 1.0)

    // Special
    val customMessage: String? = null,     // Message shown when using pool
    val secretMessage: String? = null      // Message shown after identification
)

@Serializable
data class Pool(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val locationId: String,

    // Visual appearance
    val liquidColor: String = "clear",
    val liquidAppearance: String = "still",

    // Pool behavior/effect
    val effectType: PoolEffectType = PoolEffectType.EMPTY,
    val effectData: PoolEffectData = PoolEffectData(),

    // Usage limits
    val usesPerDay: Int = 0,              // 0 = unlimited
    val isOneTimeUse: Boolean = false,

    // Discovery/identification
    val isHidden: Boolean = false,
    val identifyDifficulty: Int = 0       // 0 = obvious, 1-5 = requires skill check
)

object PoolRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ResultRow.toPool(): Pool = Pool(
        id = this[PoolTable.id],
        name = this[PoolTable.name],
        description = this[PoolTable.description],
        locationId = this[PoolTable.locationId],
        liquidColor = this[PoolTable.liquidColor],
        liquidAppearance = this[PoolTable.liquidAppearance],
        effectType = try {
            PoolEffectType.valueOf(this[PoolTable.effectType])
        } catch (e: Exception) {
            PoolEffectType.EMPTY
        },
        effectData = try {
            json.decodeFromString<PoolEffectData>(this[PoolTable.effectData])
        } catch (e: Exception) {
            PoolEffectData()
        },
        usesPerDay = this[PoolTable.usesPerDay],
        isOneTimeUse = this[PoolTable.isOneTimeUse],
        isHidden = this[PoolTable.isHidden],
        identifyDifficulty = this[PoolTable.identifyDifficulty]
    )

    fun create(pool: Pool): Pool = transaction {
        PoolTable.insert {
            it[id] = pool.id
            it[name] = pool.name
            it[description] = pool.description
            it[locationId] = pool.locationId
            it[liquidColor] = pool.liquidColor
            it[liquidAppearance] = pool.liquidAppearance
            it[effectType] = pool.effectType.name
            it[effectData] = json.encodeToString(PoolEffectData.serializer(), pool.effectData)
            it[usesPerDay] = pool.usesPerDay
            it[isOneTimeUse] = pool.isOneTimeUse
            it[isHidden] = pool.isHidden
            it[identifyDifficulty] = pool.identifyDifficulty
        }
        pool
    }

    fun findAll(): List<Pool> = transaction {
        PoolTable.selectAll().map { it.toPool() }
    }

    fun findById(id: String): Pool? = transaction {
        PoolTable.selectAll()
            .where { PoolTable.id eq id }
            .map { it.toPool() }
            .singleOrNull()
    }

    fun findByLocationId(locationId: String): List<Pool> = transaction {
        PoolTable.selectAll()
            .where { PoolTable.locationId eq locationId }
            .map { it.toPool() }
    }

    fun update(pool: Pool): Boolean = transaction {
        PoolTable.update({ PoolTable.id eq pool.id }) {
            it[name] = pool.name
            it[description] = pool.description
            it[locationId] = pool.locationId
            it[liquidColor] = pool.liquidColor
            it[liquidAppearance] = pool.liquidAppearance
            it[effectType] = pool.effectType.name
            it[effectData] = json.encodeToString(PoolEffectData.serializer(), pool.effectData)
            it[usesPerDay] = pool.usesPerDay
            it[isOneTimeUse] = pool.isOneTimeUse
            it[isHidden] = pool.isHidden
            it[identifyDifficulty] = pool.identifyDifficulty
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        PoolTable.deleteWhere { PoolTable.id eq id } > 0
    }
}
