package com.ez2bg.anotherthread.database

import at.favre.lib.crypto.bcrypt.BCrypt
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
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val passwordHash: String = "",
    val desc: String = "",
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val currentLocationId: String? = null,
    val characterClassId: String? = null,
    val classGenerationStartedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    // Combat stats
    val level: Int = 1,
    val experience: Int = 0,
    val maxHp: Int = 10,
    val currentHp: Int = 10,
    val currentCombatSessionId: String? = null
)

/**
 * User response DTO that excludes the password hash
 */
@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val desc: String,
    val itemIds: List<String>,
    val featureIds: List<String>,
    val imageUrl: String?,
    val currentLocationId: String?,
    val characterClassId: String?,
    val classGenerationStartedAt: Long?,
    val createdAt: Long,
    val lastActiveAt: Long,
    // Combat stats
    val level: Int,
    val experience: Int,
    val maxHp: Int,
    val currentHp: Int,
    val currentCombatSessionId: String?
)

fun User.toResponse(): UserResponse = UserResponse(
    id = id,
    name = name,
    desc = desc,
    itemIds = itemIds,
    featureIds = featureIds,
    imageUrl = imageUrl,
    currentLocationId = currentLocationId,
    characterClassId = characterClassId,
    classGenerationStartedAt = classGenerationStartedAt,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    level = level,
    experience = experience,
    maxHp = maxHp,
    currentHp = currentHp,
    currentCombatSessionId = currentCombatSessionId
)

object UserRepository {
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

    private fun ResultRow.toUser(): User = User(
        id = this[UserTable.id],
        name = this[UserTable.name],
        passwordHash = this[UserTable.passwordHash],
        desc = this[UserTable.desc],
        itemIds = jsonToList(this[UserTable.itemIds]),
        featureIds = jsonToList(this[UserTable.featureIds]),
        imageUrl = this[UserTable.imageUrl],
        currentLocationId = this[UserTable.currentLocationId],
        characterClassId = this[UserTable.characterClassId],
        classGenerationStartedAt = this[UserTable.classGenerationStartedAt],
        createdAt = this[UserTable.createdAt],
        lastActiveAt = this[UserTable.lastActiveAt],
        level = this[UserTable.level],
        experience = this[UserTable.experience],
        maxHp = this[UserTable.maxHp],
        currentHp = this[UserTable.currentHp],
        currentCombatSessionId = this[UserTable.currentCombatSessionId]
    )

    fun create(user: User): User = transaction {
        UserTable.insert {
            it[id] = user.id
            it[name] = user.name
            it[passwordHash] = user.passwordHash
            it[desc] = user.desc
            it[itemIds] = listToJson(user.itemIds)
            it[featureIds] = listToJson(user.featureIds)
            it[imageUrl] = user.imageUrl
            it[currentLocationId] = user.currentLocationId
            it[characterClassId] = user.characterClassId
            it[classGenerationStartedAt] = user.classGenerationStartedAt
            it[createdAt] = user.createdAt
            it[lastActiveAt] = user.lastActiveAt
            it[level] = user.level
            it[experience] = user.experience
            it[maxHp] = user.maxHp
            it[currentHp] = user.currentHp
            it[currentCombatSessionId] = user.currentCombatSessionId
        }
        user
    }

    fun findAll(): List<User> = transaction {
        UserTable.selectAll().map { it.toUser() }
    }

    fun findById(id: String): User? = transaction {
        UserTable.selectAll()
            .where { UserTable.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findByName(name: String): User? = transaction {
        UserTable.selectAll()
            .map { it.toUser() }
            .find { it.name.equals(name, ignoreCase = true) }
    }

    fun update(user: User): Boolean = transaction {
        UserTable.update({ UserTable.id eq user.id }) {
            it[name] = user.name
            it[desc] = user.desc
            it[itemIds] = listToJson(user.itemIds)
            it[featureIds] = listToJson(user.featureIds)
            it[imageUrl] = user.imageUrl
            it[currentLocationId] = user.currentLocationId
            it[characterClassId] = user.characterClassId
            it[lastActiveAt] = user.lastActiveAt
            it[level] = user.level
            it[experience] = user.experience
            it[maxHp] = user.maxHp
            it[currentHp] = user.currentHp
            it[currentCombatSessionId] = user.currentCombatSessionId
        } > 0
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[UserTable.imageUrl] = imageUrl
        } > 0
    }

    fun updateLastActiveAt(id: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    fun updateCurrentLocation(id: String, locationId: String?): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[currentLocationId] = locationId
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    fun updateCharacterClass(id: String, classId: String?): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[characterClassId] = classId
            it[classGenerationStartedAt] = null // Clear generation status when class is assigned
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    fun startClassGeneration(id: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[classGenerationStartedAt] = System.currentTimeMillis()
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    fun clearClassGeneration(id: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[classGenerationStartedAt] = null
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Find all users who are currently at a specific location and have been active recently
     */
    fun findActiveUsersAtLocation(locationId: String, activeThresholdMs: Long = 30000): List<User> = transaction {
        val cutoff = System.currentTimeMillis() - activeThresholdMs
        UserTable.selectAll()
            .where { (UserTable.currentLocationId eq locationId) }
            .map { it.toUser() }
            .filter { it.lastActiveAt >= cutoff }
    }

    /**
     * Update user's combat state
     */
    fun updateCombatState(id: String, newCurrentHp: Int, newCombatSessionId: String?): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[currentHp] = newCurrentHp
            it[currentCombatSessionId] = newCombatSessionId
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Award experience and handle level ups
     */
    fun awardExperience(id: String, expGained: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val newExp = user.experience + expGained
        val newLevel = calculateLevel(newExp)
        val newMaxHp = calculateMaxHp(newLevel, user.characterClassId)

        UserTable.update({ UserTable.id eq id }) {
            it[experience] = newExp
            it[level] = newLevel
            it[maxHp] = newMaxHp
            // Restore HP on level up
            if (newLevel > user.level) {
                it[currentHp] = newMaxHp
            }
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Fully heal a user
     */
    fun healToFull(id: String): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        UserTable.update({ UserTable.id eq id }) {
            it[currentHp] = user.maxHp
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Find all users currently in a combat session
     */
    fun findByCombatSession(sessionId: String): List<User> = transaction {
        UserTable.selectAll()
            .where { UserTable.currentCombatSessionId eq sessionId }
            .map { it.toUser() }
    }

    fun delete(id: String): Boolean = transaction {
        UserTable.deleteWhere { UserTable.id eq id } > 0
    }

    // Level calculation: every 100 exp = 1 level
    private fun calculateLevel(experience: Int): Int = (experience / 100) + 1

    // HP calculation: base 10 + (level * hitDie average)
    private fun calculateMaxHp(level: Int, classId: String?): Int {
        // Default to d8 (4.5 average) if no class
        val hitDieAverage = 5 // Simplified for now, should look up class hitDie
        return 10 + (level * hitDieAverage)
    }

    // Password hashing utilities
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}
