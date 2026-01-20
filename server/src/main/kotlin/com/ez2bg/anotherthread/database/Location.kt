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
    NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST, UNKNOWN
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
    val gridZ: Int? = null
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
        gridZ = this[LocationTable.gridZ]
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
            it[gridZ] = location.gridZ
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
            it[gridZ] = location.gridZ
        } > 0
    }

    /**
     * Find a location at the specified grid coordinates.
     */
    fun findByCoordinates(x: Int, y: Int, z: Int): Location? = transaction {
        LocationTable.selectAll()
            .where {
                (LocationTable.gridX eq x) and
                (LocationTable.gridY eq y) and
                (LocationTable.gridZ eq z)
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
