package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class Room(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val desc: String,
    val itemIds: List<String>,
    val creatureIds: List<String>,
    val exitIds: List<String>,
    val features: List<String>,
    val imageUrl: String? = null
)

object RoomRepository {
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

    private fun ResultRow.toRoom(): Room = Room(
        id = this[RoomTable.id],
        name = this[RoomTable.name],
        desc = this[RoomTable.desc],
        itemIds = jsonToList(this[RoomTable.itemIds]),
        creatureIds = jsonToList(this[RoomTable.creatureIds]),
        exitIds = jsonToList(this[RoomTable.exitIds]),
        features = jsonToList(this[RoomTable.features]),
        imageUrl = this[RoomTable.imageUrl]
    )

    fun create(room: Room): Room = transaction {
        RoomTable.insert {
            it[id] = room.id
            it[name] = room.name
            it[desc] = room.desc
            it[itemIds] = listToJson(room.itemIds)
            it[creatureIds] = listToJson(room.creatureIds)
            it[exitIds] = listToJson(room.exitIds)
            it[features] = listToJson(room.features)
            it[imageUrl] = room.imageUrl
        }
        room
    }

    fun findAll(): List<Room> = transaction {
        RoomTable.selectAll().map { it.toRoom() }
    }

    fun findById(id: String): Room? = transaction {
        RoomTable.selectAll()
            .where { RoomTable.id eq id }
            .map { it.toRoom() }
            .singleOrNull()
    }

    fun update(room: Room): Boolean = transaction {
        RoomTable.update({ RoomTable.id eq room.id }) {
            it[name] = room.name
            it[desc] = room.desc
            it[itemIds] = listToJson(room.itemIds)
            it[creatureIds] = listToJson(room.creatureIds)
            it[exitIds] = listToJson(room.exitIds)
            it[features] = listToJson(room.features)
            it[imageUrl] = room.imageUrl
        } > 0
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        RoomTable.update({ RoomTable.id eq id }) {
            it[RoomTable.imageUrl] = imageUrl
        } > 0
    }
}
