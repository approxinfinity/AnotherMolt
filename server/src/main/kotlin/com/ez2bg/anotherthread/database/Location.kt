package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

enum class ExitDirection {
    NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST, ENTER, UNKNOWN
}

// Location types for determining behavior
enum class LocationType {
    OUTDOOR_GROUND,  // Ground level outdoor - generates wilderness around it
    INDOOR,          // Indoor locations - no wilderness generation
    UNDERGROUND,     // Underground/cave locations
    UNDERWATER,      // Underwater locations
    AERIAL           // Sky/aerial locations
}

@Serializable
data class Exit(
    val locationId: String,
    val direction: ExitDirection = ExitDirection.UNKNOWN
)

@Serializable
data class Location(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val desc: String,
    val itemIds: List<String>,
    val creatureIds: List<String>,
    val exits: List<Exit>,
    val featureIds: List<String>,
    val imageUrl: String? = null,
    val lockedBy: String? = null,
    // Grid coordinates - null means not yet placed in a coordinate system
    val gridX: Int? = null,
    val gridY: Int? = null,
    // Area identifier - groups locations into distinct map regions (e.g., "overworld", "fungus-forest")
    // Note: areaId replaces gridZ - different areas can have overlapping x,y coordinates
    val areaId: String? = null,
    // Last edited tracking - null means never edited by a user (e.g., auto-generated wilderness)
    val lastEditedBy: String? = null,
    // Stored as LocalDateTime internally but serialized as ISO string
    val lastEditedAt: String? = null,
    // Location type for determining behavior
    val locationType: LocationType? = null,
    // Biome metadata from world generation
    val biome: String? = null,
    val elevation: Float? = null,
    val moisture: Float? = null,
    val isRiver: Boolean? = null,
    val isCoast: Boolean? = null,
    val terrainFeatures: List<String>? = null,
    val isOriginalTerrain: Boolean? = null
)

object LocationRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun listToJson(list: List<String>): String {
        return list.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
            .let { "[$it]" }
    }

    private fun jsonToList(json: String): List<String> {
        if (json == "[]") return emptyList()
        return json
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"").replace("\\\"", "\"") }
            .filter { it.isNotEmpty() }
    }

    private fun exitsToJson(exits: List<Exit>): String {
        return json.encodeToString(exits)
    }

    private fun jsonToExits(jsonStr: String): List<Exit> {
        if (jsonStr == "[]" || jsonStr.isBlank()) return emptyList()
        return try {
            // Try parsing as new Exit format
            json.decodeFromString<List<Exit>>(jsonStr)
        } catch (e: Exception) {
            // Fall back to legacy format (simple string list of location IDs)
            try {
                val legacyIds = jsonToList(jsonStr)
                legacyIds.map { Exit(locationId = it, direction = ExitDirection.UNKNOWN) }
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    private fun ResultRow.toLocation(): Location = Location(
        id = this[LocationTable.id],
        name = this[LocationTable.name],
        desc = this[LocationTable.desc],
        itemIds = jsonToList(this[LocationTable.itemIds]),
        creatureIds = jsonToList(this[LocationTable.creatureIds]),
        exits = jsonToExits(this[LocationTable.exitIds]),
        featureIds = jsonToList(this[LocationTable.featureIds]),
        imageUrl = this[LocationTable.imageUrl],
        lockedBy = this[LocationTable.lockedBy],
        gridX = this[LocationTable.gridX],
        gridY = this[LocationTable.gridY],
        areaId = this[LocationTable.areaId],
        lastEditedBy = this[LocationTable.lastEditedBy],
        lastEditedAt = this[LocationTable.lastEditedAt],
        locationType = this[LocationTable.locationType]?.let {
            try { LocationType.valueOf(it) } catch (e: Exception) { null }
        },
        biome = this[LocationTable.biome],
        elevation = this[LocationTable.elevation],
        moisture = this[LocationTable.moisture],
        isRiver = this[LocationTable.isRiver],
        isCoast = this[LocationTable.isCoast],
        terrainFeatures = this[LocationTable.terrainFeatures]?.let { jsonToList(it) }?.ifEmpty { null },
        isOriginalTerrain = this[LocationTable.isOriginalTerrain]
    )

    fun create(location: Location): Location = transaction {
        LocationTable.insert {
            it[id] = location.id
            it[name] = location.name
            it[desc] = location.desc
            it[itemIds] = listToJson(location.itemIds)
            it[creatureIds] = listToJson(location.creatureIds)
            it[exitIds] = exitsToJson(location.exits)
            it[featureIds] = listToJson(location.featureIds)
            it[imageUrl] = location.imageUrl
            it[lockedBy] = location.lockedBy
            it[gridX] = location.gridX
            it[gridY] = location.gridY
            it[areaId] = location.areaId
            it[lastEditedBy] = location.lastEditedBy
            it[lastEditedAt] = location.lastEditedAt
            it[locationType] = location.locationType?.name
            it[biome] = location.biome
            it[elevation] = location.elevation
            it[moisture] = location.moisture
            it[isRiver] = location.isRiver
            it[isCoast] = location.isCoast
            it[terrainFeatures] = location.terrainFeatures?.let { listToJson(it) }
            it[isOriginalTerrain] = location.isOriginalTerrain
        }
        location
    }

    fun findAll(): List<Location> = transaction {
        LocationTable.selectAll().map { it.toLocation() }
    }

    fun findById(id: String): Location? = transaction {
        LocationTable.selectAll()
            .where { LocationTable.id eq id }
            .map { it.toLocation() }
            .singleOrNull()
    }

    fun update(location: Location): Boolean = transaction {
        LocationTable.update({ LocationTable.id eq location.id }) {
            it[name] = location.name
            it[desc] = location.desc
            it[itemIds] = listToJson(location.itemIds)
            it[creatureIds] = listToJson(location.creatureIds)
            it[exitIds] = exitsToJson(location.exits)
            it[featureIds] = listToJson(location.featureIds)
            it[imageUrl] = location.imageUrl
            it[lockedBy] = location.lockedBy
            it[gridX] = location.gridX
            it[gridY] = location.gridY
            it[areaId] = location.areaId
            it[lastEditedBy] = location.lastEditedBy
            it[lastEditedAt] = location.lastEditedAt
            it[locationType] = location.locationType?.name
            it[biome] = location.biome
            it[elevation] = location.elevation
            it[moisture] = location.moisture
            it[isRiver] = location.isRiver
            it[isCoast] = location.isCoast
            it[terrainFeatures] = location.terrainFeatures?.let { listToJson(it) }
            it[isOriginalTerrain] = location.isOriginalTerrain
        } > 0
    }

    /**
     * Find a location at the specified grid coordinates within an area.
     */
    fun findByCoordinates(x: Int, y: Int, areaId: String? = "overworld"): Location? = transaction {
        LocationTable.selectAll()
            .where {
                (LocationTable.gridX eq x) and
                (LocationTable.gridY eq y) and
                (LocationTable.areaId eq areaId)
            }
            .map { it.toLocation() }
            .singleOrNull()
    }

    /**
     * Find all locations that have an exit pointing to the given location ID.
     */
    fun findLocationsWithExitTo(locationId: String): List<Location> = transaction {
        LocationTable.selectAll()
            .map { it.toLocation() }
            .filter { loc -> loc.exits.any { it.locationId == locationId } }
    }

    fun updateLockedBy(id: String, lockedBy: String?): Boolean = transaction {
        LocationTable.update({ LocationTable.id eq id }) {
            it[LocationTable.lockedBy] = lockedBy
        } > 0
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        LocationTable.update({ LocationTable.id eq id }) {
            it[LocationTable.imageUrl] = imageUrl
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        LocationTable.deleteWhere { LocationTable.id eq id } > 0
    }

    fun removeCreatureIdFromAll(creatureId: String) = transaction {
        val locations = LocationTable.selectAll().map { it.toLocation() }
        locations.filter { creatureId in it.creatureIds }.forEach { location ->
            val updatedCreatureIds = location.creatureIds.filter { it != creatureId }
            LocationTable.update({ LocationTable.id eq location.id }) {
                it[creatureIds] = listToJson(updatedCreatureIds)
            }
        }
    }

    fun removeItemIdFromAll(itemId: String) = transaction {
        val locations = LocationTable.selectAll().map { it.toLocation() }
        locations.filter { itemId in it.itemIds }.forEach { location ->
            val updatedItemIds = location.itemIds.filter { it != itemId }
            LocationTable.update({ LocationTable.id eq location.id }) {
                it[itemIds] = listToJson(updatedItemIds)
            }
        }
    }

    /**
     * Add items to a location's item list
     */
    fun addItems(id: String, newItemIds: List<String>): Boolean = transaction {
        val location = findById(id) ?: return@transaction false
        LocationTable.update({ LocationTable.id eq id }) {
            it[itemIds] = listToJson(location.itemIds + newItemIds)
        } > 0
    }

    fun removeExitIdFromAll(locationId: String) = transaction {
        val locations = LocationTable.selectAll().map { it.toLocation() }
        locations.filter { loc -> loc.exits.any { it.locationId == locationId } }.forEach { location ->
            val updatedExits = location.exits.filter { it.locationId != locationId }
            LocationTable.update({ LocationTable.id eq location.id }) {
                it[exitIds] = exitsToJson(updatedExits)
            }
        }
    }
}
