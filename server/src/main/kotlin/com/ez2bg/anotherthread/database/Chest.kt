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
data class Chest(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val desc: String,
    val locationId: String,
    val guardianCreatureId: String? = null,  // Must defeat this creature first
    val isLocked: Boolean = true,
    val lockDifficulty: Int = 1,  // 1-5 scale for pick lock
    val bashDifficulty: Int = 1,  // 1-5 scale for bash
    val lootTableId: String? = null,
    val goldAmount: Int = 0,
    val imageUrl: String? = null
)

object ChestRepository {
    private fun ResultRow.toChest(): Chest = Chest(
        id = this[ChestTable.id],
        name = this[ChestTable.name],
        desc = this[ChestTable.desc],
        locationId = this[ChestTable.locationId],
        guardianCreatureId = this[ChestTable.guardianCreatureId],
        isLocked = this[ChestTable.isLocked],
        lockDifficulty = this[ChestTable.lockDifficulty],
        bashDifficulty = this[ChestTable.bashDifficulty],
        lootTableId = this[ChestTable.lootTableId],
        goldAmount = this[ChestTable.goldAmount],
        imageUrl = this[ChestTable.imageUrl]
    )

    fun create(chest: Chest): Chest = transaction {
        ChestTable.insert {
            it[id] = chest.id
            it[name] = chest.name
            it[desc] = chest.desc
            it[locationId] = chest.locationId
            it[guardianCreatureId] = chest.guardianCreatureId
            it[isLocked] = chest.isLocked
            it[lockDifficulty] = chest.lockDifficulty
            it[bashDifficulty] = chest.bashDifficulty
            it[lootTableId] = chest.lootTableId
            it[goldAmount] = chest.goldAmount
            it[imageUrl] = chest.imageUrl
        }
        chest
    }

    fun findAll(): List<Chest> = transaction {
        ChestTable.selectAll().map { it.toChest() }
    }

    fun findById(id: String): Chest? = transaction {
        ChestTable.selectAll()
            .where { ChestTable.id eq id }
            .map { it.toChest() }
            .singleOrNull()
    }

    fun findByLocationId(locationId: String): List<Chest> = transaction {
        ChestTable.selectAll()
            .where { ChestTable.locationId eq locationId }
            .map { it.toChest() }
    }

    fun update(chest: Chest): Boolean = transaction {
        ChestTable.update({ ChestTable.id eq chest.id }) {
            it[name] = chest.name
            it[desc] = chest.desc
            it[locationId] = chest.locationId
            it[guardianCreatureId] = chest.guardianCreatureId
            it[isLocked] = chest.isLocked
            it[lockDifficulty] = chest.lockDifficulty
            it[bashDifficulty] = chest.bashDifficulty
            it[lootTableId] = chest.lootTableId
            it[goldAmount] = chest.goldAmount
            it[imageUrl] = chest.imageUrl
        } > 0
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        ChestTable.update({ ChestTable.id eq id }) {
            it[ChestTable.imageUrl] = imageUrl
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        ChestTable.deleteWhere { ChestTable.id eq id } > 0
    }
}
