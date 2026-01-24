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
import java.util.UUID

@Serializable
data class LootEntry(
    val itemId: String,
    val chance: Float,  // 0.0 to 1.0
    val minQty: Int = 1,
    val maxQty: Int = 1
)

@Serializable
data class LootTableData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val entries: List<LootEntry>
)

object LootTableRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ResultRow.toLootTable(): LootTableData = LootTableData(
        id = this[LootTableTable.id],
        name = this[LootTableTable.name],
        entries = json.decodeFromString(this[LootTableTable.entries])
    )

    fun create(lootTable: LootTableData): LootTableData = transaction {
        LootTableTable.insert {
            it[id] = lootTable.id
            it[name] = lootTable.name
            it[entries] = json.encodeToString(lootTable.entries)
        }
        lootTable
    }

    fun findAll(): List<LootTableData> = transaction {
        LootTableTable.selectAll().map { it.toLootTable() }
    }

    fun findById(id: String): LootTableData? = transaction {
        LootTableTable.selectAll()
            .where { LootTableTable.id eq id }
            .map { it.toLootTable() }
            .singleOrNull()
    }

    fun update(lootTable: LootTableData): Boolean = transaction {
        LootTableTable.update({ LootTableTable.id eq lootTable.id }) {
            it[name] = lootTable.name
            it[entries] = json.encodeToString(lootTable.entries)
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        LootTableTable.deleteWhere { LootTableTable.id eq id } > 0
    }
}
