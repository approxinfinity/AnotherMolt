package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

@Serializable
data class ForestParams(
    val treeCount: Int? = null,
    val sizeMultiplier: Float? = null
)

@Serializable
data class LakeParams(
    val diameterMultiplier: Float? = null,
    val diameterMultiplierX: Float? = null,
    val diameterMultiplierY: Float? = null,
    val shapePoints: Int? = null,
    val noiseScale: Float? = null
)

@Serializable
data class RiverParams(
    val widthMultiplier: Float? = null
)

@Serializable
data class MountainParams(
    val peakCount: Int? = null,
    val heightMultiplier: Float? = null
)

@Serializable
data class GrassParams(
    val tuftCount: Int? = null
)

@Serializable
data class HillsParams(
    val heightMultiplier: Float? = null
)

@Serializable
data class StreamParams(
    val widthMultiplier: Float? = null
)

@Serializable
data class DesertParams(
    val duneCount: Int? = null,
    val heightMultiplier: Float? = null
)

@Serializable
data class SwampParams(
    val densityMultiplier: Float? = null,
    val diameterMultiplierX: Float? = null,
    val diameterMultiplierY: Float? = null,
    val shapePoints: Int? = null,
    val noiseScale: Float? = null
)

@Serializable
data class TerrainOverrides(
    val forest: ForestParams? = null,
    val lake: LakeParams? = null,
    val river: RiverParams? = null,
    val mountain: MountainParams? = null,
    val grass: GrassParams? = null,
    val hills: HillsParams? = null,
    val stream: StreamParams? = null,
    val desert: DesertParams? = null,
    val swamp: SwampParams? = null
)

@Serializable
data class TerrainOverride(
    val locationId: String,
    val overrides: TerrainOverrides,
    val updatedBy: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

private val json = Json { ignoreUnknownKeys = true }

object TerrainOverrideRepository {
    private fun ResultRow.toTerrainOverride(): TerrainOverride = TerrainOverride(
        locationId = this[TerrainOverrideTable.locationId],
        overrides = json.decodeFromString(this[TerrainOverrideTable.overridesJson]),
        updatedBy = this[TerrainOverrideTable.updatedBy],
        updatedAt = this[TerrainOverrideTable.updatedAt]
    )

    fun findByLocationId(locationId: String): TerrainOverride? = transaction {
        TerrainOverrideTable.selectAll()
            .where { TerrainOverrideTable.locationId eq locationId }
            .map { it.toTerrainOverride() }
            .singleOrNull()
    }

    fun upsert(override: TerrainOverride): TerrainOverride = transaction {
        val existing = TerrainOverrideTable.selectAll()
            .where { TerrainOverrideTable.locationId eq override.locationId }
            .singleOrNull()

        if (existing != null) {
            TerrainOverrideTable.update({ TerrainOverrideTable.locationId eq override.locationId }) {
                it[overridesJson] = json.encodeToString(override.overrides)
                it[updatedBy] = override.updatedBy
                it[updatedAt] = override.updatedAt
            }
        } else {
            TerrainOverrideTable.insert {
                it[locationId] = override.locationId
                it[overridesJson] = json.encodeToString(override.overrides)
                it[updatedBy] = override.updatedBy
                it[updatedAt] = override.updatedAt
            }
        }
        override
    }

    fun delete(locationId: String): Boolean = transaction {
        TerrainOverrideTable.deleteWhere { TerrainOverrideTable.locationId eq locationId } > 0
    }

    fun findAll(): List<TerrainOverride> = transaction {
        TerrainOverrideTable.selectAll()
            .map { it.toTerrainOverride() }
    }
}
