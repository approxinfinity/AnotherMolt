package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class CharacterClass(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val isSpellcaster: Boolean,
    val hitDie: Int,
    val primaryAttribute: String,
    val imageUrl: String? = null,
    val powerBudget: Int = 100,           // Total power points for abilities
    val isPublic: Boolean = true,          // Available to all users
    val createdByUserId: String? = null    // Null for seeded/admin classes
)

object CharacterClassRepository {
    private fun ResultRow.toCharacterClass(): CharacterClass = CharacterClass(
        id = this[CharacterClassTable.id],
        name = this[CharacterClassTable.name],
        description = this[CharacterClassTable.description],
        isSpellcaster = this[CharacterClassTable.isSpellcaster],
        hitDie = this[CharacterClassTable.hitDie],
        primaryAttribute = this[CharacterClassTable.primaryAttribute],
        imageUrl = this[CharacterClassTable.imageUrl],
        powerBudget = this[CharacterClassTable.powerBudget],
        isPublic = this[CharacterClassTable.isPublic],
        createdByUserId = this[CharacterClassTable.createdByUserId]
    )

    fun create(characterClass: CharacterClass): CharacterClass = transaction {
        CharacterClassTable.insert {
            it[id] = characterClass.id
            it[name] = characterClass.name
            it[description] = characterClass.description
            it[isSpellcaster] = characterClass.isSpellcaster
            it[hitDie] = characterClass.hitDie
            it[primaryAttribute] = characterClass.primaryAttribute
            it[imageUrl] = characterClass.imageUrl
            it[powerBudget] = characterClass.powerBudget
            it[isPublic] = characterClass.isPublic
            it[createdByUserId] = characterClass.createdByUserId
        }
        characterClass
    }

    fun findAll(): List<CharacterClass> = transaction {
        CharacterClassTable.selectAll().map { it.toCharacterClass() }
    }

    fun findById(id: String): CharacterClass? = transaction {
        CharacterClassTable.selectAll()
            .where { CharacterClassTable.id eq id }
            .map { it.toCharacterClass() }
            .singleOrNull()
    }

    fun findByName(name: String): CharacterClass? = transaction {
        CharacterClassTable.selectAll()
            .where { CharacterClassTable.name eq name }
            .map { it.toCharacterClass() }
            .singleOrNull()
    }

    fun update(characterClass: CharacterClass): Boolean = transaction {
        CharacterClassTable.update({ CharacterClassTable.id eq characterClass.id }) {
            it[name] = characterClass.name
            it[description] = characterClass.description
            it[isSpellcaster] = characterClass.isSpellcaster
            it[hitDie] = characterClass.hitDie
            it[primaryAttribute] = characterClass.primaryAttribute
            it[imageUrl] = characterClass.imageUrl
            it[powerBudget] = characterClass.powerBudget
            it[isPublic] = characterClass.isPublic
            it[createdByUserId] = characterClass.createdByUserId
        } > 0
    }

    /** Find all public classes (available to everyone) */
    fun findPublic(): List<CharacterClass> = transaction {
        CharacterClassTable.selectAll()
            .where { CharacterClassTable.isPublic eq true }
            .map { it.toCharacterClass() }
    }

    /** Find classes created by a specific user */
    fun findByCreator(userId: String): List<CharacterClass> = transaction {
        CharacterClassTable.selectAll()
            .where { CharacterClassTable.createdByUserId eq userId }
            .map { it.toCharacterClass() }
    }

    /** Find classes available to a user (public + their own) */
    fun findAvailableForUser(userId: String?): List<CharacterClass> = transaction {
        if (userId == null) {
            findPublic()
        } else {
            CharacterClassTable.selectAll()
                .where {
                    (CharacterClassTable.isPublic eq true) or
                    (CharacterClassTable.createdByUserId eq userId)
                }
                .map { it.toCharacterClass() }
        }
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        CharacterClassTable.update({ CharacterClassTable.id eq id }) {
            it[CharacterClassTable.imageUrl] = imageUrl
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        CharacterClassTable.deleteWhere { CharacterClassTable.id eq id } > 0
    }
}
