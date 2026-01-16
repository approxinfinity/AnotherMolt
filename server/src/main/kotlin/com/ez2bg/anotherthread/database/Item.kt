package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class Item(
    val id: String,
    val desc: String,
    val featureIds: List<String>
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
        desc = this[ItemTable.desc],
        featureIds = jsonToList(this[ItemTable.featureIds])
    )

    fun create(item: Item): Item = transaction {
        ItemTable.insert {
            it[id] = item.id
            it[desc] = item.desc
            it[featureIds] = listToJson(item.featureIds)
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
}
