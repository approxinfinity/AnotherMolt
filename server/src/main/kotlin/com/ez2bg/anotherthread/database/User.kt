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
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
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
    val createdAt: Long,
    val lastActiveAt: Long
)

fun User.toResponse(): UserResponse = UserResponse(
    id = id,
    name = name,
    desc = desc,
    itemIds = itemIds,
    featureIds = featureIds,
    imageUrl = imageUrl,
    currentLocationId = currentLocationId,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt
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
        createdAt = this[UserTable.createdAt],
        lastActiveAt = this[UserTable.lastActiveAt]
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
            it[createdAt] = user.createdAt
            it[lastActiveAt] = user.lastActiveAt
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
            it[lastActiveAt] = user.lastActiveAt
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

    fun delete(id: String): Boolean = transaction {
        UserTable.deleteWhere { UserTable.id eq id } > 0
    }

    // Password hashing utilities
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}
