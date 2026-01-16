package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class Feature(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val featureCategoryId: String? = null,
    val description: String,
    val data: String = "{}"
)

object FeatureRepository {
    private fun ResultRow.toFeature(): Feature = Feature(
        id = this[FeatureTable.id],
        name = this[FeatureTable.name],
        featureCategoryId = this[FeatureTable.featureCategoryId],
        description = this[FeatureTable.description],
        data = this[FeatureTable.data]
    )

    fun create(feature: Feature): Feature = transaction {
        FeatureTable.insert {
            it[id] = feature.id
            it[name] = feature.name
            it[featureCategoryId] = feature.featureCategoryId
            it[description] = feature.description
            it[data] = feature.data
        }
        feature
    }

    fun findAll(): List<Feature> = transaction {
        FeatureTable.selectAll().map { it.toFeature() }
    }

    fun findById(id: String): Feature? = transaction {
        FeatureTable.selectAll()
            .where { FeatureTable.id eq id }
            .map { it.toFeature() }
            .singleOrNull()
    }

    fun findByCategoryId(categoryId: String): List<Feature> = transaction {
        FeatureTable.selectAll()
            .where { FeatureTable.featureCategoryId eq categoryId }
            .map { it.toFeature() }
    }

    fun update(feature: Feature): Boolean = transaction {
        FeatureTable.update({ FeatureTable.id eq feature.id }) {
            it[name] = feature.name
            it[featureCategoryId] = feature.featureCategoryId
            it[description] = feature.description
            it[data] = feature.data
        } > 0
    }
}
