package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * Relationship level descriptions for factions.
 */
enum class RelationshipLevel(val minValue: Int, val maxValue: Int) {
    HATED(-100, -75),       // At war, attack on sight
    HOSTILE(-74, -25),      // Very unfriendly, may attack
    UNFRIENDLY(-24, -1),    // Distrustful
    NEUTRAL(0, 0),          // No opinion
    FRIENDLY(1, 24),        // Willing to trade/talk
    ALLIED(25, 74),         // Will help in combat
    DEVOTED(75, 100);       // Fight to the death together

    companion object {
        fun fromValue(value: Int): RelationshipLevel {
            return entries.find { value in it.minValue..it.maxValue } ?: NEUTRAL
        }
    }
}

/**
 * Player standing level descriptions.
 */
enum class StandingLevel(val minValue: Int, val maxValue: Int, val description: String) {
    HATED(-100, -75, "Kill on sight"),
    HOSTILE(-74, -25, "Attacks unless outnumbered"),
    UNFRIENDLY(-24, -1, "Refuses to interact"),
    NEUTRAL(0, 0, "No opinion"),
    FRIENDLY(1, 24, "Will trade and talk"),
    HONORED(25, 74, "Offers discounts and quests"),
    REVERED(75, 100, "Offers unique rewards");

    companion object {
        fun fromValue(value: Int): StandingLevel {
            return entries.find { value in it.minValue..it.maxValue } ?: NEUTRAL
        }
    }
}

/**
 * Additional faction data stored as JSON.
 */
@Serializable
data class FactionData(
    val territoryLocationIds: List<String> = emptyList(),  // All locations this faction controls
    val enemyFactionIds: List<String> = emptyList(),  // Factions they hate
    val allyFactionIds: List<String> = emptyList(),  // Factions they're allied with
    val goals: List<String> = emptyList(),  // Faction objectives (for future quest generation)
    val tradeGoods: List<String> = emptyList(),  // Items they're willing to trade
    val tributeItems: List<String> = emptyList()  // Items they accept as tribute
)

@Serializable
data class Faction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val homeLocationId: String? = null,
    val hostilityLevel: Int = 50,  // 0=peaceful, 50=neutral, 100=hostile
    val canNegotiate: Boolean = true,
    val leaderCreatureId: String? = null,
    val data: FactionData = FactionData()
)

@Serializable
data class FactionRelation(
    val id: String = UUID.randomUUID().toString(),
    val factionId: String,
    val targetFactionId: String,
    val relationshipLevel: Int = 0  // -100 to 100
)

@Serializable
data class PlayerFactionStanding(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val factionId: String,
    val standing: Int = 0,  // -100 to 100
    val killCount: Int = 0,
    val questsCompleted: Int = 0
)

object FactionRepository {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    private fun ResultRow.toFaction(): Faction = Faction(
        id = this[FactionTable.id],
        name = this[FactionTable.name],
        description = this[FactionTable.description],
        homeLocationId = this[FactionTable.homeLocationId],
        hostilityLevel = this[FactionTable.hostilityLevel],
        canNegotiate = this[FactionTable.canNegotiate],
        leaderCreatureId = this[FactionTable.leaderCreatureId],
        data = try {
            json.decodeFromString<FactionData>(this[FactionTable.data])
        } catch (e: Exception) {
            FactionData()
        }
    )

    fun create(faction: Faction): Faction = transaction {
        FactionTable.insert {
            it[id] = faction.id
            it[name] = faction.name
            it[description] = faction.description
            it[homeLocationId] = faction.homeLocationId
            it[hostilityLevel] = faction.hostilityLevel
            it[canNegotiate] = faction.canNegotiate
            it[leaderCreatureId] = faction.leaderCreatureId
            it[data] = json.encodeToString(FactionData.serializer(), faction.data)
        }
        faction
    }

    fun findAll(): List<Faction> = transaction {
        FactionTable.selectAll().map { it.toFaction() }
    }

    fun findById(id: String): Faction? = transaction {
        FactionTable.selectAll()
            .where { FactionTable.id eq id }
            .map { it.toFaction() }
            .singleOrNull()
    }

    fun update(faction: Faction): Boolean = transaction {
        FactionTable.update({ FactionTable.id eq faction.id }) {
            it[name] = faction.name
            it[description] = faction.description
            it[homeLocationId] = faction.homeLocationId
            it[hostilityLevel] = faction.hostilityLevel
            it[canNegotiate] = faction.canNegotiate
            it[leaderCreatureId] = faction.leaderCreatureId
            it[data] = json.encodeToString(FactionData.serializer(), faction.data)
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        FactionTable.deleteWhere { FactionTable.id eq id } > 0
    }
}

object FactionRelationRepository {
    private fun ResultRow.toRelation(): FactionRelation = FactionRelation(
        id = this[FactionRelationTable.id],
        factionId = this[FactionRelationTable.factionId],
        targetFactionId = this[FactionRelationTable.targetFactionId],
        relationshipLevel = this[FactionRelationTable.relationshipLevel]
    )

    fun create(relation: FactionRelation): FactionRelation = transaction {
        FactionRelationTable.insert {
            it[id] = relation.id
            it[factionId] = relation.factionId
            it[targetFactionId] = relation.targetFactionId
            it[relationshipLevel] = relation.relationshipLevel
        }
        relation
    }

    fun findAll(): List<FactionRelation> = transaction {
        FactionRelationTable.selectAll().map { it.toRelation() }
    }

    fun findByFactionId(factionId: String): List<FactionRelation> = transaction {
        FactionRelationTable.selectAll()
            .where { FactionRelationTable.factionId eq factionId }
            .map { it.toRelation() }
    }

    fun findRelation(factionId: String, targetFactionId: String): FactionRelation? = transaction {
        FactionRelationTable.selectAll()
            .where { (FactionRelationTable.factionId eq factionId) and (FactionRelationTable.targetFactionId eq targetFactionId) }
            .map { it.toRelation() }
            .singleOrNull()
    }

    fun update(relation: FactionRelation): Boolean = transaction {
        FactionRelationTable.update({ FactionRelationTable.id eq relation.id }) {
            it[relationshipLevel] = relation.relationshipLevel
        } > 0
    }

    fun setRelationship(factionId: String, targetFactionId: String, level: Int): FactionRelation {
        val existing = findRelation(factionId, targetFactionId)
        return if (existing != null) {
            val updated = existing.copy(relationshipLevel = level.coerceIn(-100, 100))
            update(updated)
            updated
        } else {
            create(FactionRelation(
                factionId = factionId,
                targetFactionId = targetFactionId,
                relationshipLevel = level.coerceIn(-100, 100)
            ))
        }
    }

    fun delete(id: String): Boolean = transaction {
        FactionRelationTable.deleteWhere { FactionRelationTable.id eq id } > 0
    }
}

object PlayerFactionStandingRepository {
    private fun ResultRow.toStanding(): PlayerFactionStanding = PlayerFactionStanding(
        id = this[PlayerFactionStandingTable.id],
        userId = this[PlayerFactionStandingTable.userId],
        factionId = this[PlayerFactionStandingTable.factionId],
        standing = this[PlayerFactionStandingTable.standing],
        killCount = this[PlayerFactionStandingTable.killCount],
        questsCompleted = this[PlayerFactionStandingTable.questsCompleted]
    )

    fun create(standing: PlayerFactionStanding): PlayerFactionStanding = transaction {
        PlayerFactionStandingTable.insert {
            it[id] = standing.id
            it[userId] = standing.userId
            it[factionId] = standing.factionId
            it[PlayerFactionStandingTable.standing] = standing.standing
            it[killCount] = standing.killCount
            it[questsCompleted] = standing.questsCompleted
        }
        standing
    }

    fun findByUserId(userId: String): List<PlayerFactionStanding> = transaction {
        PlayerFactionStandingTable.selectAll()
            .where { PlayerFactionStandingTable.userId eq userId }
            .map { it.toStanding() }
    }

    fun findByUserAndFaction(userId: String, factionId: String): PlayerFactionStanding? = transaction {
        PlayerFactionStandingTable.selectAll()
            .where { (PlayerFactionStandingTable.userId eq userId) and (PlayerFactionStandingTable.factionId eq factionId) }
            .map { it.toStanding() }
            .singleOrNull()
    }

    fun update(standing: PlayerFactionStanding): Boolean = transaction {
        PlayerFactionStandingTable.update({ PlayerFactionStandingTable.id eq standing.id }) {
            it[PlayerFactionStandingTable.standing] = standing.standing
            it[killCount] = standing.killCount
            it[questsCompleted] = standing.questsCompleted
        } > 0
    }

    fun getOrCreate(userId: String, factionId: String): PlayerFactionStanding {
        return findByUserAndFaction(userId, factionId) ?: create(PlayerFactionStanding(
            userId = userId,
            factionId = factionId
        ))
    }

    fun modifyStanding(userId: String, factionId: String, delta: Int): PlayerFactionStanding {
        val standing = getOrCreate(userId, factionId)
        val updated = standing.copy(standing = (standing.standing + delta).coerceIn(-100, 100))
        update(updated)
        return updated
    }

    fun incrementKillCount(userId: String, factionId: String): PlayerFactionStanding {
        val standing = getOrCreate(userId, factionId)
        val updated = standing.copy(killCount = standing.killCount + 1)
        update(updated)
        return updated
    }

    fun incrementQuestsCompleted(userId: String, factionId: String): PlayerFactionStanding {
        val standing = getOrCreate(userId, factionId)
        val updated = standing.copy(questsCompleted = standing.questsCompleted + 1)
        update(updated)
        return updated
    }
}
