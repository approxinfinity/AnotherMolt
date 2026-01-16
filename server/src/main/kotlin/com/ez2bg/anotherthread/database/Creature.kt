package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class Creature(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val desc: String,
    val itemIds: List<String>,
    val features: List<String>,
    val imageUrl: String? = null
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
        name = this[CreatureTable.name],
        desc = this[CreatureTable.desc],
        itemIds = jsonToList(this[CreatureTable.itemIds]),
        features = jsonToList(this[CreatureTable.features]),
        imageUrl = this[CreatureTable.imageUrl]
    )

    fun create(creature: Creature): Creature = transaction {
        CreatureTable.insert {
            it[id] = creature.id
            it[name] = creature.name
            it[desc] = creature.desc
            it[itemIds] = listToJson(creature.itemIds)
            it[features] = listToJson(creature.features)
            it[imageUrl] = creature.imageUrl
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

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        CreatureTable.update({ CreatureTable.id eq id }) {
            it[CreatureTable.imageUrl] = imageUrl
        } > 0
    }
}
