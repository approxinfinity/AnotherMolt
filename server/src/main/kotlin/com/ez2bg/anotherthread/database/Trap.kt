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
 * Types of traps.
 */
enum class TrapType {
    PIT,            // Fall into a pit, take falling damage
    DART,           // Darts shoot from wall
    POISON_NEEDLE,  // Needle on chest/door handle
    BOULDER,        // Rolling boulder
    ALARM,          // Alerts nearby creatures
    FIRE,           // Fire/flame trap
    SPEAR,          // Spears from floor/walls
    CAGE,           // Drops a cage, trapping player
    TELEPORT,       // Teleports player somewhere
    MAGIC           // Custom magical effect
}

/**
 * What triggers the trap.
 */
enum class TrapTrigger {
    MOVEMENT,       // Walking into the area
    INTERACTION,    // Interacting with something
    DOOR,           // Opening a door
    CHEST,          // Opening a chest
    PRESSURE_PLATE, // Stepping on specific spot
    TRIPWIRE        // Crossing a tripwire
}

/**
 * Effect data for different trap types.
 */
@Serializable
data class TrapEffectData(
    // Damage
    val damageDice: String? = null,        // e.g., "2d6"
    val damageType: String? = null,        // "falling", "piercing", "poison", "fire"

    // Poison
    val poisonDuration: Int? = null,       // Rounds of poison
    val poisonDamageDice: String? = null,  // Ongoing poison damage

    // Pit specific
    val pitDepth: Int? = null,             // Feet, affects damage

    // Alarm specific
    val alertsCreatureIds: List<String>? = null,  // Creature IDs to alert
    val alertRadius: Int? = null,          // How far the alarm reaches

    // Teleport
    val teleportLocationId: String? = null,

    // Status effects
    val appliesCondition: String? = null,  // "restrained", "prone", etc.
    val conditionDuration: Int? = null,

    // Custom
    val customMessage: String? = null,
    val savingThrowType: String? = null,   // "dexterity", "constitution", etc.
    val savingThrowDC: Int? = null
)

@Serializable
data class Trap(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val locationId: String,

    // Type and trigger
    val trapType: TrapType = TrapType.PIT,
    val triggerType: TrapTrigger = TrapTrigger.MOVEMENT,

    // Detection and disarm
    val detectDifficulty: Int = 2,  // 1-5 scale
    val disarmDifficulty: Int = 2,  // 1-5 scale

    // Effect data
    val effectData: TrapEffectData = TrapEffectData(),

    // State
    val isHidden: Boolean = true,
    val isArmed: Boolean = true,
    val resetsAfterRounds: Int = 0  // 0 = doesn't reset
)

object TrapRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ResultRow.toTrap(): Trap = Trap(
        id = this[TrapTable.id],
        name = this[TrapTable.name],
        description = this[TrapTable.description],
        locationId = this[TrapTable.locationId],
        trapType = try {
            TrapType.valueOf(this[TrapTable.trapType])
        } catch (e: Exception) {
            TrapType.PIT
        },
        triggerType = try {
            TrapTrigger.valueOf(this[TrapTable.triggerType])
        } catch (e: Exception) {
            TrapTrigger.MOVEMENT
        },
        detectDifficulty = this[TrapTable.detectDifficulty],
        disarmDifficulty = this[TrapTable.disarmDifficulty],
        effectData = try {
            json.decodeFromString<TrapEffectData>(this[TrapTable.effectData])
        } catch (e: Exception) {
            TrapEffectData()
        },
        isHidden = this[TrapTable.isHidden],
        isArmed = this[TrapTable.isArmed],
        resetsAfterRounds = this[TrapTable.resetsAfterRounds]
    )

    fun create(trap: Trap): Trap = transaction {
        TrapTable.insert {
            it[id] = trap.id
            it[name] = trap.name
            it[description] = trap.description
            it[locationId] = trap.locationId
            it[trapType] = trap.trapType.name
            it[triggerType] = trap.triggerType.name
            it[detectDifficulty] = trap.detectDifficulty
            it[disarmDifficulty] = trap.disarmDifficulty
            it[effectData] = json.encodeToString(TrapEffectData.serializer(), trap.effectData)
            it[isHidden] = trap.isHidden
            it[isArmed] = trap.isArmed
            it[resetsAfterRounds] = trap.resetsAfterRounds
        }
        trap
    }

    fun findAll(): List<Trap> = transaction {
        TrapTable.selectAll().map { it.toTrap() }
    }

    fun findById(id: String): Trap? = transaction {
        TrapTable.selectAll()
            .where { TrapTable.id eq id }
            .map { it.toTrap() }
            .singleOrNull()
    }

    fun findByLocationId(locationId: String): List<Trap> = transaction {
        TrapTable.selectAll()
            .where { TrapTable.locationId eq locationId }
            .map { it.toTrap() }
    }

    fun update(trap: Trap): Boolean = transaction {
        TrapTable.update({ TrapTable.id eq trap.id }) {
            it[name] = trap.name
            it[description] = trap.description
            it[locationId] = trap.locationId
            it[trapType] = trap.trapType.name
            it[triggerType] = trap.triggerType.name
            it[detectDifficulty] = trap.detectDifficulty
            it[disarmDifficulty] = trap.disarmDifficulty
            it[effectData] = json.encodeToString(TrapEffectData.serializer(), trap.effectData)
            it[isHidden] = trap.isHidden
            it[isArmed] = trap.isArmed
            it[resetsAfterRounds] = trap.resetsAfterRounds
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        TrapTable.deleteWhere { TrapTable.id eq id } > 0
    }
}
