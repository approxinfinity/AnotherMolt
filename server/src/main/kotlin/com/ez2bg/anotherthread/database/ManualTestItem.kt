package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
data class ManualTestItem(
    val id: String = UUID.randomUUID().toString(),
    val featureName: String,
    val description: String,
    val category: String,
    val commitHash: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val testedAt: Long? = null,
    val testedByUserId: String? = null,
    val testedByUserName: String? = null,
    val notes: String? = null
) {
    val isTested: Boolean get() = testedAt != null
}

object ManualTestItemRepository {
    private fun ResultRow.toManualTestItem(): ManualTestItem = ManualTestItem(
        id = this[ManualTestItemTable.id],
        featureName = this[ManualTestItemTable.featureName],
        description = this[ManualTestItemTable.description],
        category = this[ManualTestItemTable.category],
        commitHash = this[ManualTestItemTable.commitHash],
        addedAt = this[ManualTestItemTable.addedAt],
        testedAt = this[ManualTestItemTable.testedAt],
        testedByUserId = this[ManualTestItemTable.testedByUserId],
        testedByUserName = this[ManualTestItemTable.testedByUserName],
        notes = this[ManualTestItemTable.notes]
    )

    fun create(item: ManualTestItem): ManualTestItem = transaction {
        ManualTestItemTable.insert {
            it[id] = item.id
            it[featureName] = item.featureName
            it[description] = item.description
            it[category] = item.category
            it[commitHash] = item.commitHash
            it[addedAt] = item.addedAt
            it[testedAt] = item.testedAt
            it[testedByUserId] = item.testedByUserId
            it[testedByUserName] = item.testedByUserName
            it[notes] = item.notes
        }
        item
    }

    /**
     * Create a test item only if it doesn't already exist (by feature name).
     * Useful for seeding to avoid duplicates.
     */
    fun createIfNotExists(item: ManualTestItem): ManualTestItem? = transaction {
        val existing = ManualTestItemTable.selectAll()
            .where { ManualTestItemTable.featureName eq item.featureName }
            .singleOrNull()

        if (existing == null) {
            ManualTestItemTable.insert {
                it[id] = item.id
                it[featureName] = item.featureName
                it[description] = item.description
                it[category] = item.category
                it[commitHash] = item.commitHash
                it[addedAt] = item.addedAt
                it[testedAt] = item.testedAt
                it[testedByUserId] = item.testedByUserId
                it[testedByUserName] = item.testedByUserName
                it[notes] = item.notes
            }
            item
        } else {
            null  // Already exists
        }
    }

    fun findById(id: String): ManualTestItem? = transaction {
        ManualTestItemTable.selectAll()
            .where { ManualTestItemTable.id eq id }
            .singleOrNull()
            ?.toManualTestItem()
    }

    fun findAll(): List<ManualTestItem> = transaction {
        ManualTestItemTable.selectAll()
            .orderBy(ManualTestItemTable.addedAt, SortOrder.DESC)
            .map { it.toManualTestItem() }
    }

    fun findUntested(): List<ManualTestItem> = transaction {
        ManualTestItemTable.selectAll()
            .where { ManualTestItemTable.testedAt.isNull() }
            .orderBy(ManualTestItemTable.addedAt, SortOrder.DESC)
            .map { it.toManualTestItem() }
    }

    fun findTested(): List<ManualTestItem> = transaction {
        ManualTestItemTable.selectAll()
            .where { ManualTestItemTable.testedAt.isNotNull() }
            .orderBy(ManualTestItemTable.testedAt, SortOrder.DESC)
            .map { it.toManualTestItem() }
    }

    fun findByCategory(category: String): List<ManualTestItem> = transaction {
        ManualTestItemTable.selectAll()
            .where { ManualTestItemTable.category eq category }
            .orderBy(ManualTestItemTable.addedAt, SortOrder.DESC)
            .map { it.toManualTestItem() }
    }

    /**
     * Mark a test item as tested.
     */
    fun markTested(id: String, userId: String, userName: String, notes: String? = null): Boolean = transaction {
        val updated = ManualTestItemTable.update({ ManualTestItemTable.id eq id }) {
            it[testedAt] = System.currentTimeMillis()
            it[testedByUserId] = userId
            it[testedByUserName] = userName
            if (notes != null) {
                it[ManualTestItemTable.notes] = notes
            }
        }
        updated > 0
    }

    /**
     * Unmark a test item (move back to untested).
     */
    fun unmarkTested(id: String): Boolean = transaction {
        val updated = ManualTestItemTable.update({ ManualTestItemTable.id eq id }) {
            it[testedAt] = null
            it[testedByUserId] = null
            it[testedByUserName] = null
        }
        updated > 0
    }

    fun delete(id: String): Boolean = transaction {
        val deleted = ManualTestItemTable.deleteWhere { ManualTestItemTable.id eq id }
        deleted > 0
    }

    fun update(item: ManualTestItem): Boolean = transaction {
        val updated = ManualTestItemTable.update({ ManualTestItemTable.id eq item.id }) {
            it[featureName] = item.featureName
            it[description] = item.description
            it[category] = item.category
            it[commitHash] = item.commitHash
            it[notes] = item.notes
        }
        updated > 0
    }

    /**
     * Get counts for dashboard display.
     */
    fun getCounts(): Pair<Int, Int> = transaction {
        val untested = ManualTestItemTable.selectAll()
            .where { ManualTestItemTable.testedAt.isNull() }
            .count()
            .toInt()
        val tested = ManualTestItemTable.selectAll()
            .where { ManualTestItemTable.testedAt.isNotNull() }
            .count()
            .toInt()
        Pair(untested, tested)
    }

    /**
     * Get all unique categories.
     */
    fun getCategories(): List<String> = transaction {
        ManualTestItemTable.select(ManualTestItemTable.category)
            .withDistinct()
            .map { it[ManualTestItemTable.category] }
            .sorted()
    }
}
