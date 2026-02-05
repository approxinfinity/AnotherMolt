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

@Serializable
data class PlayerEncounter(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val encounteredUserId: String,
    val firstEncounteredAt: Long = System.currentTimeMillis(),
    val lastEncounteredAt: Long = System.currentTimeMillis(),
    val lastLocationId: String? = null,
    val classification: String = "neutral",
    val lastKnownDesc: String = "",
    val lastKnownImageUrl: String? = null,
    val lastKnownName: String = "",
    val encounterCount: Int = 1
)

@Serializable
data class PlayerEncounterResponse(
    val encounteredUserId: String,
    val classification: String,
    val lastKnownName: String,
    val lastKnownDesc: String,
    val lastKnownImageUrl: String?,
    val lastLocationId: String?,
    val firstEncounteredAt: Long,
    val lastEncounteredAt: Long,
    val encounterCount: Int
)

object PlayerEncounterRepository {
    private fun ResultRow.toPlayerEncounter() = PlayerEncounter(
        id = this[PlayerEncounterTable.id],
        userId = this[PlayerEncounterTable.userId],
        encounteredUserId = this[PlayerEncounterTable.encounteredUserId],
        firstEncounteredAt = this[PlayerEncounterTable.firstEncounteredAt],
        lastEncounteredAt = this[PlayerEncounterTable.lastEncounteredAt],
        lastLocationId = this[PlayerEncounterTable.lastLocationId],
        classification = this[PlayerEncounterTable.classification],
        lastKnownDesc = this[PlayerEncounterTable.lastKnownDesc],
        lastKnownImageUrl = this[PlayerEncounterTable.lastKnownImageUrl],
        lastKnownName = this[PlayerEncounterTable.lastKnownName],
        encounterCount = this[PlayerEncounterTable.encounterCount]
    )

    fun findByUser(userId: String): List<PlayerEncounter> = transaction {
        PlayerEncounterTable.selectAll()
            .where { PlayerEncounterTable.userId eq userId }
            .map { it.toPlayerEncounter() }
    }

    fun findEncounter(userId: String, encounteredUserId: String): PlayerEncounter? = transaction {
        PlayerEncounterTable.selectAll()
            .where {
                (PlayerEncounterTable.userId eq userId) and
                (PlayerEncounterTable.encounteredUserId eq encounteredUserId)
            }
            .firstOrNull()?.toPlayerEncounter()
    }

    /**
     * Records an encounter between two players.
     * Creates the record if new, or updates last-seen info if existing.
     * Records encounter from BOTH perspectives (userId sees encounteredUser, and vice versa).
     */
    fun recordEncounter(userId: String, encounteredUser: User, locationId: String) {
        recordOneWay(userId, encounteredUser, locationId)
        // Also record the reverse direction
        val currentUser = UserRepository.findById(userId)
        if (currentUser != null) {
            recordOneWay(encounteredUser.id, currentUser, locationId)
        }
    }

    private fun recordOneWay(userId: String, encounteredUser: User, locationId: String) = transaction {
        val existing = PlayerEncounterTable.selectAll()
            .where {
                (PlayerEncounterTable.userId eq userId) and
                (PlayerEncounterTable.encounteredUserId eq encounteredUser.id)
            }
            .firstOrNull()

        val now = System.currentTimeMillis()

        if (existing != null) {
            PlayerEncounterTable.update({
                (PlayerEncounterTable.userId eq userId) and
                (PlayerEncounterTable.encounteredUserId eq encounteredUser.id)
            }) {
                it[lastEncounteredAt] = now
                it[lastLocationId] = locationId
                it[lastKnownDesc] = encounteredUser.desc
                it[lastKnownImageUrl] = encounteredUser.imageUrl
                it[lastKnownName] = encounteredUser.name
                it[encounterCount] = existing[PlayerEncounterTable.encounterCount] + 1
            }
        } else {
            PlayerEncounterTable.insert {
                it[id] = UUID.randomUUID().toString()
                it[PlayerEncounterTable.userId] = userId
                it[encounteredUserId] = encounteredUser.id
                it[firstEncounteredAt] = now
                it[lastEncounteredAt] = now
                it[PlayerEncounterTable.lastLocationId] = locationId
                it[classification] = "neutral"
                it[lastKnownDesc] = encounteredUser.desc
                it[lastKnownImageUrl] = encounteredUser.imageUrl
                it[lastKnownName] = encounteredUser.name
                it[encounterCount] = 1
            }
        }
    }

    fun classify(userId: String, encounteredUserId: String, newClassification: String): Boolean = transaction {
        PlayerEncounterTable.update({
            (PlayerEncounterTable.userId eq userId) and
            (PlayerEncounterTable.encounteredUserId eq encounteredUserId)
        }) {
            it[classification] = newClassification
        } > 0
    }
}
