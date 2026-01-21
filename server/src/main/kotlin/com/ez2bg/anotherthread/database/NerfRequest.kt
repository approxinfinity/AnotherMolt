package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class NerfRequest(
    val id: String = UUID.randomUUID().toString(),
    val abilityId: String,
    val requestedByUserId: String,
    val requestedByUserName: String,
    val reason: String,
    val status: String = "pending",  // pending, approved, rejected, applied
    val suggestedChanges: String? = null,  // JSON of suggested ability changes
    val adminNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolvedByUserId: String? = null
)

object NerfRequestRepository {
    private fun ResultRow.toNerfRequest(): NerfRequest = NerfRequest(
        id = this[NerfRequestTable.id],
        abilityId = this[NerfRequestTable.abilityId],
        requestedByUserId = this[NerfRequestTable.requestedByUserId],
        requestedByUserName = this[NerfRequestTable.requestedByUserName],
        reason = this[NerfRequestTable.reason],
        status = this[NerfRequestTable.status],
        suggestedChanges = this[NerfRequestTable.suggestedChanges],
        adminNotes = this[NerfRequestTable.adminNotes],
        createdAt = this[NerfRequestTable.createdAt],
        resolvedAt = this[NerfRequestTable.resolvedAt],
        resolvedByUserId = this[NerfRequestTable.resolvedByUserId]
    )

    fun create(nerfRequest: NerfRequest): NerfRequest = transaction {
        NerfRequestTable.insert {
            it[id] = nerfRequest.id
            it[abilityId] = nerfRequest.abilityId
            it[requestedByUserId] = nerfRequest.requestedByUserId
            it[requestedByUserName] = nerfRequest.requestedByUserName
            it[reason] = nerfRequest.reason
            it[status] = nerfRequest.status
            it[suggestedChanges] = nerfRequest.suggestedChanges
            it[adminNotes] = nerfRequest.adminNotes
            it[createdAt] = nerfRequest.createdAt
            it[resolvedAt] = nerfRequest.resolvedAt
            it[resolvedByUserId] = nerfRequest.resolvedByUserId
        }
        nerfRequest
    }

    fun findAll(): List<NerfRequest> = transaction {
        NerfRequestTable.selectAll()
            .orderBy(NerfRequestTable.createdAt)
            .map { it.toNerfRequest() }
    }

    fun findById(id: String): NerfRequest? = transaction {
        NerfRequestTable.selectAll()
            .where { NerfRequestTable.id eq id }
            .map { it.toNerfRequest() }
            .singleOrNull()
    }

    fun findPending(): List<NerfRequest> = transaction {
        NerfRequestTable.selectAll()
            .where { NerfRequestTable.status eq "pending" }
            .orderBy(NerfRequestTable.createdAt)
            .map { it.toNerfRequest() }
    }

    fun findByAbilityId(abilityId: String): List<NerfRequest> = transaction {
        NerfRequestTable.selectAll()
            .where { NerfRequestTable.abilityId eq abilityId }
            .orderBy(NerfRequestTable.createdAt)
            .map { it.toNerfRequest() }
    }

    fun findByUserId(userId: String): List<NerfRequest> = transaction {
        NerfRequestTable.selectAll()
            .where { NerfRequestTable.requestedByUserId eq userId }
            .orderBy(NerfRequestTable.createdAt)
            .map { it.toNerfRequest() }
    }

    fun update(nerfRequest: NerfRequest): Boolean = transaction {
        NerfRequestTable.update({ NerfRequestTable.id eq nerfRequest.id }) {
            it[abilityId] = nerfRequest.abilityId
            it[requestedByUserId] = nerfRequest.requestedByUserId
            it[requestedByUserName] = nerfRequest.requestedByUserName
            it[reason] = nerfRequest.reason
            it[status] = nerfRequest.status
            it[suggestedChanges] = nerfRequest.suggestedChanges
            it[adminNotes] = nerfRequest.adminNotes
            it[resolvedAt] = nerfRequest.resolvedAt
            it[resolvedByUserId] = nerfRequest.resolvedByUserId
        } > 0
    }

    fun resolve(
        id: String,
        status: String,
        resolvedByUserId: String,
        adminNotes: String? = null
    ): Boolean = transaction {
        NerfRequestTable.update({ NerfRequestTable.id eq id }) {
            it[NerfRequestTable.status] = status
            it[resolvedAt] = System.currentTimeMillis()
            it[NerfRequestTable.resolvedByUserId] = resolvedByUserId
            if (adminNotes != null) {
                it[NerfRequestTable.adminNotes] = adminNotes
            }
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        NerfRequestTable.deleteWhere { NerfRequestTable.id eq id } > 0
    }

    fun countPending(): Long = transaction {
        NerfRequestTable.selectAll()
            .where { NerfRequestTable.status eq "pending" }
            .count()
    }
}
