package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class Creature(
    val id: String,
    val desc: String,
    val itemIds: List<String>,
    val features: List<String>
)

object CreatureRepository {
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

    private fun ResultRow.toCreature(): Creature = Creature(
        id = this[CreatureTable.id],
        desc = this[CreatureTable.desc],
        itemIds = jsonToList(this[CreatureTable.itemIds]),
        features = jsonToList(this[CreatureTable.features])
    )

    fun create(creature: Creature): Creature = transaction {
        CreatureTable.insert {
            it[id] = creature.id
            it[desc] = creature.desc
            it[itemIds] = listToJson(creature.itemIds)
            it[features] = listToJson(creature.features)
        }
        creature
    }

    fun findAll(): List<Creature> = transaction {
        CreatureTable.selectAll().map { it.toCreature() }
    }

    fun findById(id: String): Creature? = transaction {
        CreatureTable.selectAll()
            .where { CreatureTable.id eq id }
            .map { it.toCreature() }
            .singleOrNull()
    }
}
