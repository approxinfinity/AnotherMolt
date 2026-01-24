package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.util.*

/**
 * Session duration constants
 */
object SessionConfig {
    const val SESSION_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
    const val SESSION_DURATION_SECONDS = 7 * 24 * 60 * 60 // 7 days in seconds (for cookie maxAge)
}

/**
 * Database table for user sessions.
 * Supports sliding expiration - each authenticated request refreshes the expiry.
 */
object SessionTable : Table("session") {
    val id = varchar("id", 64) // Session token (secure random)
    val userId = varchar("user_id", 36).references(UserTable.id)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
    val lastActiveAt = long("last_active_at")
    val userAgent = text("user_agent").nullable() // For session management UI
    val ipAddress = varchar("ip_address", 45).nullable() // IPv6 max length

    override val primaryKey = PrimaryKey(id)
}

/**
 * Session data model
 */
@Serializable
data class Session(
    val id: String,
    val userId: String,
    val createdAt: Long,
    val expiresAt: Long,
    val lastActiveAt: Long,
    val userAgent: String? = null,
    val ipAddress: String? = null
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
}

/**
 * Repository for session CRUD operations
 */
object SessionRepository {
    private val secureRandom = SecureRandom()

    /**
     * Generate a cryptographically secure session token
     */
    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Create a new session for a user
     */
    fun create(
        userId: String,
        userAgent: String? = null,
        ipAddress: String? = null
    ): Session = transaction {
        val now = System.currentTimeMillis()
        val token = generateToken()
        val session = Session(
            id = token,
            userId = userId,
            createdAt = now,
            expiresAt = now + SessionConfig.SESSION_DURATION_MS,
            lastActiveAt = now,
            userAgent = userAgent,
            ipAddress = ipAddress
        )

        SessionTable.insert {
            it[id] = session.id
            it[SessionTable.userId] = session.userId
            it[createdAt] = session.createdAt
            it[expiresAt] = session.expiresAt
            it[lastActiveAt] = session.lastActiveAt
            it[SessionTable.userAgent] = session.userAgent
            it[SessionTable.ipAddress] = session.ipAddress
        }

        session
    }

    /**
     * Find a session by token
     */
    fun findById(token: String): Session? = transaction {
        SessionTable.selectAll()
            .where { SessionTable.id eq token }
            .map { it.toSession() }
            .singleOrNull()
    }

    /**
     * Find all sessions for a user
     */
    fun findByUserId(userId: String): List<Session> = transaction {
        SessionTable.selectAll()
            .where { SessionTable.userId eq userId }
            .map { it.toSession() }
    }

    /**
     * Refresh session expiry (sliding window).
     * Called on each authenticated request.
     * Returns the updated session or null if not found.
     */
    fun refresh(token: String): Session? = transaction {
        val now = System.currentTimeMillis()
        val newExpiresAt = now + SessionConfig.SESSION_DURATION_MS

        val updated = SessionTable.update({ SessionTable.id eq token }) {
            it[expiresAt] = newExpiresAt
            it[lastActiveAt] = now
        }

        if (updated > 0) {
            findById(token)
        } else {
            null
        }
    }

    /**
     * Validate a session token.
     * Returns the session if valid and not expired, null otherwise.
     * Does NOT refresh - call refresh() separately if you want sliding expiry.
     */
    fun validate(token: String): Session? = transaction {
        val session = findById(token) ?: return@transaction null
        if (session.isExpired()) {
            // Clean up expired session
            delete(token)
            return@transaction null
        }
        session
    }

    /**
     * Validate and refresh in one operation.
     * Returns the refreshed session if valid, null otherwise.
     */
    fun validateAndRefresh(token: String): Session? = transaction {
        val session = findById(token) ?: return@transaction null
        if (session.isExpired()) {
            delete(token)
            return@transaction null
        }
        refresh(token)
    }

    /**
     * Delete a session (logout)
     */
    fun delete(token: String): Boolean = transaction {
        SessionTable.deleteWhere { id eq token } > 0
    }

    /**
     * Delete all sessions for a user (logout everywhere)
     */
    fun deleteAllForUser(userId: String): Int = transaction {
        SessionTable.deleteWhere { SessionTable.userId eq userId }
    }

    /**
     * Delete all expired sessions (cleanup job)
     */
    fun deleteExpired(): Int = transaction {
        val now = System.currentTimeMillis()
        SessionTable.deleteWhere { expiresAt less now }
    }

    private fun ResultRow.toSession(): Session = Session(
        id = this[SessionTable.id],
        userId = this[SessionTable.userId],
        createdAt = this[SessionTable.createdAt],
        expiresAt = this[SessionTable.expiresAt],
        lastActiveAt = this[SessionTable.lastActiveAt],
        userAgent = this[SessionTable.userAgent],
        ipAddress = this[SessionTable.ipAddress]
    )
}
