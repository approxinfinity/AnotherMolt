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
data class Creature(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val desc: String,
    val itemIds: List<String>,
    val featureIds: List<String>,
    val imageUrl: String? = null,
    val lockedBy: String? = null,
    // Combat stats
    val maxHp: Int = 10,
    val baseDamage: Int = 5,
    val damageDice: String? = null,  // XdY+Z format (e.g., "1d6+2"), preferred over baseDamage
    val abilityIds: List<String> = emptyList(),
    val level: Int = 1,
    val experienceValue: Int = 10,
    val challengeRating: Int = 1,
    val isAggressive: Boolean = false,
    // Loot fields
    val lootTableId: String? = null,
    val minGoldDrop: Int = 0,
    val maxGoldDrop: Int = 0,
    val attribution: String? = null  // Content attribution source
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
        featureIds = jsonToList(this[CreatureTable.featureIds]),
        imageUrl = this[CreatureTable.imageUrl],
        lockedBy = this[CreatureTable.lockedBy],
        maxHp = this[CreatureTable.maxHp],
        baseDamage = this[CreatureTable.baseDamage],
        damageDice = this[CreatureTable.damageDice],
        abilityIds = jsonToList(this[CreatureTable.abilityIds]),
        level = this[CreatureTable.level],
        experienceValue = this[CreatureTable.experienceValue],
        challengeRating = this[CreatureTable.challengeRating],
        isAggressive = this[CreatureTable.isAggressive],
        lootTableId = this[CreatureTable.lootTableId],
        minGoldDrop = this[CreatureTable.minGoldDrop],
        maxGoldDrop = this[CreatureTable.maxGoldDrop],
        attribution = this[CreatureTable.attribution]
    )

    fun create(creature: Creature): Creature = transaction {
        CreatureTable.insert {
            it[id] = creature.id
            it[name] = creature.name
            it[desc] = creature.desc
            it[itemIds] = listToJson(creature.itemIds)
            it[featureIds] = listToJson(creature.featureIds)
            it[imageUrl] = creature.imageUrl
            it[lockedBy] = creature.lockedBy
            it[maxHp] = creature.maxHp
            it[baseDamage] = creature.baseDamage
            it[damageDice] = creature.damageDice
            it[abilityIds] = listToJson(creature.abilityIds)
            it[level] = creature.level
            it[experienceValue] = creature.experienceValue
            it[challengeRating] = creature.challengeRating
            it[isAggressive] = creature.isAggressive
            it[lootTableId] = creature.lootTableId
            it[minGoldDrop] = creature.minGoldDrop
            it[maxGoldDrop] = creature.maxGoldDrop
            it[attribution] = creature.attribution
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

    fun update(creature: Creature): Boolean = transaction {
        CreatureTable.update({ CreatureTable.id eq creature.id }) {
            it[name] = creature.name
            it[desc] = creature.desc
            it[itemIds] = listToJson(creature.itemIds)
            it[featureIds] = listToJson(creature.featureIds)
            it[imageUrl] = creature.imageUrl
            it[lockedBy] = creature.lockedBy
            it[maxHp] = creature.maxHp
            it[baseDamage] = creature.baseDamage
            it[damageDice] = creature.damageDice
            it[abilityIds] = listToJson(creature.abilityIds)
            it[level] = creature.level
            it[experienceValue] = creature.experienceValue
            it[challengeRating] = creature.challengeRating
            it[isAggressive] = creature.isAggressive
            it[lootTableId] = creature.lootTableId
            it[minGoldDrop] = creature.minGoldDrop
            it[maxGoldDrop] = creature.maxGoldDrop
            it[attribution] = creature.attribution
        } > 0
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        CreatureTable.update({ CreatureTable.id eq id }) {
            it[CreatureTable.imageUrl] = imageUrl
        } > 0
    }

    fun updateLockedBy(id: String, lockedBy: String?): Boolean = transaction {
        CreatureTable.update({ CreatureTable.id eq id }) {
            it[CreatureTable.lockedBy] = lockedBy
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        CreatureTable.deleteWhere { CreatureTable.id eq id } > 0
    }
}
