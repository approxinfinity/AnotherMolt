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
data class CustomIconMapping(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val abilityId: String,
    val iconName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CustomIconMappingResponse(
    val abilityId: String,
    val iconName: String
)

object CustomIconMappingRepository {
    private fun ResultRow.toCustomIconMapping() = CustomIconMapping(
        id = this[CustomIconMappingTable.id],
        userId = this[CustomIconMappingTable.userId],
        abilityId = this[CustomIconMappingTable.abilityId],
        iconName = this[CustomIconMappingTable.iconName],
        createdAt = this[CustomIconMappingTable.createdAt]
    )

    fun findByUser(userId: String): List<CustomIconMapping> = transaction {
        CustomIconMappingTable.selectAll()
            .where { CustomIconMappingTable.userId eq userId }
            .map { it.toCustomIconMapping() }
    }

    fun setMapping(userId: String, abilityId: String, iconName: String) = transaction {
        val existing = CustomIconMappingTable.selectAll()
            .where { (CustomIconMappingTable.userId eq userId) and (CustomIconMappingTable.abilityId eq abilityId) }
            .firstOrNull()

        if (existing != null) {
            CustomIconMappingTable.update({
                (CustomIconMappingTable.userId eq userId) and (CustomIconMappingTable.abilityId eq abilityId)
            }) {
                it[CustomIconMappingTable.iconName] = iconName
            }
        } else {
            CustomIconMappingTable.insert {
                it[CustomIconMappingTable.id] = UUID.randomUUID().toString()
                it[CustomIconMappingTable.userId] = userId
                it[CustomIconMappingTable.abilityId] = abilityId
                it[CustomIconMappingTable.iconName] = iconName
                it[CustomIconMappingTable.createdAt] = System.currentTimeMillis()
            }
        }
    }

    fun deleteMapping(userId: String, abilityId: String): Boolean = transaction {
        CustomIconMappingTable.deleteWhere {
            (CustomIconMappingTable.userId eq userId) and (CustomIconMappingTable.abilityId eq abilityId)
        } > 0
    }
}
