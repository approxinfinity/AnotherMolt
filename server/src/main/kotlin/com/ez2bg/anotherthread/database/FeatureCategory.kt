package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class FeatureCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String
)

object FeatureCategoryRepository {
    private fun ResultRow.toFeatureCategory(): FeatureCategory = FeatureCategory(
        id = this[FeatureCategoryTable.id],
        name = this[FeatureCategoryTable.name],
        description = this[FeatureCategoryTable.description]
    )

    fun create(category: FeatureCategory): FeatureCategory = transaction {
        FeatureCategoryTable.insert {
            it[id] = category.id
            it[name] = category.name
            it[description] = category.description
        }
        category
    }

    fun findAll(): List<FeatureCategory> = transaction {
        FeatureCategoryTable.selectAll().map { it.toFeatureCategory() }
    }

    fun findById(id: String): FeatureCategory? = transaction {
        FeatureCategoryTable.selectAll()
            .where { FeatureCategoryTable.id eq id }
            .map { it.toFeatureCategory() }
            .singleOrNull()
    }

    fun update(category: FeatureCategory): Boolean = transaction {
        FeatureCategoryTable.update({ FeatureCategoryTable.id eq category.id }) {
            it[name] = category.name
            it[description] = category.description
        } > 0
    }
}
