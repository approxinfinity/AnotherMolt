package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class Location(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val desc: String,
    val itemIds: List<String>,
    val creatureIds: List<String>,
    val exitIds: List<String>,
    val featureIds: List<String>,
    val imageUrl: String? = null,
    val lockedBy: String? = null
)

object LocationRepository {
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

    private fun ResultRow.toLocation(): Location = Location(
        id = this[LocationTable.id],
        name = this[LocationTable.name],
        desc = this[LocationTable.desc],
        itemIds = jsonToList(this[LocationTable.itemIds]),
        creatureIds = jsonToList(this[LocationTable.creatureIds]),
        exitIds = jsonToList(this[LocationTable.exitIds]),
        featureIds = jsonToList(this[LocationTable.featureIds]),
        imageUrl = this[LocationTable.imageUrl],
        lockedBy = this[LocationTable.lockedBy]
    )

    fun create(location: Location): Location = transaction {
        LocationTable.insert {
            it[id] = location.id
            it[name] = location.name
            it[desc] = location.desc
            it[itemIds] = listToJson(location.itemIds)
            it[creatureIds] = listToJson(location.creatureIds)
            it[exitIds] = listToJson(location.exitIds)
            it[featureIds] = listToJson(location.featureIds)
            it[imageUrl] = location.imageUrl
            it[lockedBy] = location.lockedBy
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
            it[exitIds] = listToJson(location.exitIds)
            it[featureIds] = listToJson(location.featureIds)
            it[imageUrl] = location.imageUrl
            it[lockedBy] = location.lockedBy
        } > 0
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
}
