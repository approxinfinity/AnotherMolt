package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val desc: String,
    val featureIds: List<String>,
    val abilityIds: List<String> = emptyList(),  // Combat abilities this item grants
    val imageUrl: String? = null,
    val lockedBy: String? = null
)

object ItemRepository {
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

    private fun ResultRow.toItem(): Item = Item(
        id = this[ItemTable.id],
        name = this[ItemTable.name],
        desc = this[ItemTable.desc],
        featureIds = jsonToList(this[ItemTable.featureIds]),
        abilityIds = jsonToList(this[ItemTable.abilityIds]),
        imageUrl = this[ItemTable.imageUrl],
        lockedBy = this[ItemTable.lockedBy]
    )

    fun create(item: Item): Item = transaction {
        ItemTable.insert {
            it[id] = item.id
            it[name] = item.name
            it[desc] = item.desc
            it[featureIds] = listToJson(item.featureIds)
            it[abilityIds] = listToJson(item.abilityIds)
            it[imageUrl] = item.imageUrl
            it[lockedBy] = item.lockedBy
        }
        item
    }

    fun findAll(): List<Item> = transaction {
        ItemTable.selectAll().map { it.toItem() }
    }

    fun findById(id: String): Item? = transaction {
        ItemTable.selectAll()
            .where { ItemTable.id eq id }
            .map { it.toItem() }
            .singleOrNull()
    }

    fun update(item: Item): Boolean = transaction {
        ItemTable.update({ ItemTable.id eq item.id }) {
            it[name] = item.name
            it[desc] = item.desc
            it[featureIds] = listToJson(item.featureIds)
            it[abilityIds] = listToJson(item.abilityIds)
            it[imageUrl] = item.imageUrl
            it[lockedBy] = item.lockedBy
        } > 0
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        ItemTable.update({ ItemTable.id eq id }) {
            it[ItemTable.imageUrl] = imageUrl
        } > 0
    }

    fun updateLockedBy(id: String, lockedBy: String?): Boolean = transaction {
        ItemTable.update({ ItemTable.id eq id }) {
            it[ItemTable.lockedBy] = lockedBy
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        ItemTable.deleteWhere { ItemTable.id eq id } > 0
    }
}
