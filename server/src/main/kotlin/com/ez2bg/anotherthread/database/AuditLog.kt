package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

enum class AuditAction {
    CREATE, UPDATE, DELETE, LOCK, UNLOCK
}

@Serializable
data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val recordId: String,
    val recordType: String,
    val recordName: String,
    val action: AuditAction,
    val userId: String,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)

object AuditLogRepository {
    private fun ResultRow.toAuditLog(): AuditLog = AuditLog(
        id = this[AuditLogTable.id],
        recordId = this[AuditLogTable.recordId],
        recordType = this[AuditLogTable.recordType],
        recordName = this[AuditLogTable.recordName],
        action = AuditAction.valueOf(this[AuditLogTable.action]),
        userId = this[AuditLogTable.userId],
        userName = this[AuditLogTable.userName],
        timestamp = this[AuditLogTable.timestamp]
    )

    fun create(log: AuditLog): AuditLog = transaction {
        AuditLogTable.insert {
            it[id] = log.id
            it[recordId] = log.recordId
            it[recordType] = log.recordType
            it[recordName] = log.recordName
            it[action] = log.action.name
            it[userId] = log.userId
            it[userName] = log.userName
            it[timestamp] = log.timestamp
        }
        log
    }

    fun log(
        recordId: String,
        recordType: String,
        recordName: String,
        action: AuditAction,
        userId: String,
        userName: String
    ): AuditLog {
        return create(
            AuditLog(
                recordId = recordId,
                recordType = recordType,
                recordName = recordName,
                action = action,
                userId = userId,
                userName = userName
            )
        )
    }

    fun findAll(limit: Int = 100, offset: Long = 0): List<AuditLog> = transaction {
        AuditLogTable.selectAll()
            .orderBy(AuditLogTable.timestamp, SortOrder.DESC)
            .limit(limit, offset)
            .map { it.toAuditLog() }
    }

    fun findByRecordId(recordId: String): List<AuditLog> = transaction {
        AuditLogTable.selectAll()
            .where { AuditLogTable.recordId eq recordId }
            .orderBy(AuditLogTable.timestamp, SortOrder.DESC)
            .map { it.toAuditLog() }
    }

    fun findByRecordType(recordType: String, limit: Int = 100): List<AuditLog> = transaction {
        AuditLogTable.selectAll()
            .where { AuditLogTable.recordType eq recordType }
            .orderBy(AuditLogTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { it.toAuditLog() }
    }

    fun findByUserId(userId: String, limit: Int = 100): List<AuditLog> = transaction {
        AuditLogTable.selectAll()
            .where { AuditLogTable.userId eq userId }
            .orderBy(AuditLogTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { it.toAuditLog() }
    }
}
