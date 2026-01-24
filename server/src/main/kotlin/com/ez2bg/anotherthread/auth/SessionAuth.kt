package com.ez2bg.anotherthread.auth

import com.ez2bg.anotherthread.database.Session
import com.ez2bg.anotherthread.database.SessionConfig
import com.ez2bg.anotherthread.database.SessionRepository
import com.ez2bg.anotherthread.database.User
import com.ez2bg.anotherthread.database.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*

/**
 * Attribute keys for storing authenticated user/session in call attributes.
 */
val AuthenticatedUserKey = AttributeKey<User>("AuthenticatedUser")
val AuthenticatedSessionKey = AttributeKey<Session>("AuthenticatedSession")

/**
 * Extension to get the authenticated user from a call.
 * Returns null if not authenticated.
 */
val ApplicationCall.authenticatedUser: User?
    get() = attributes.getOrNull(AuthenticatedUserKey)

/**
 * Extension to get the authenticated session from a call.
 * Returns null if not authenticated.
 */
val ApplicationCall.authenticatedSession: Session?
    get() = attributes.getOrNull(AuthenticatedSessionKey)

/**
 * Extract session token from request.
 * Checks cookie first (web clients), then Authorization header (native clients).
 */
fun ApplicationCall.extractSessionToken(): String? {
    // Check cookie first (web clients)
    request.cookies["session"]?.let { return it }

    // Check Authorization header (native clients)
    request.header("Authorization")?.let { header ->
        if (header.startsWith("Bearer ", ignoreCase = true)) {
            return header.removePrefix("Bearer ").removePrefix("bearer ")
        }
    }

    return null
}

/**
 * Validate and refresh session, returning user if valid.
 * Also sets the refreshed cookie for web clients.
 */
suspend fun ApplicationCall.validateAndRefreshSession(): User? {
    val token = extractSessionToken() ?: return null

    val session = SessionRepository.validateAndRefresh(token) ?: return null

    val user = UserRepository.findById(session.userId) ?: run {
        // User deleted but session exists - clean up
        SessionRepository.delete(token)
        return null
    }

    // Store in call attributes for later use
    attributes.put(AuthenticatedUserKey, user)
    attributes.put(AuthenticatedSessionKey, session)

    // Refresh cookie for web clients (sliding expiration)
    response.cookies.append(
        Cookie(
            name = "session",
            value = session.id,
            maxAge = SessionConfig.SESSION_DURATION_SECONDS,
            httpOnly = true,
            path = "/",
            extensions = mapOf("SameSite" to "Lax")
        )
    )

    // Update user's last active timestamp
    UserRepository.updateLastActiveAt(user.id)

    return user
}

/**
 * Require authentication for a route.
 * If not authenticated, responds with 401 and returns null.
 * If authenticated, returns the user and refreshes the session.
 */
suspend fun ApplicationCall.requireAuth(): User? {
    val user = validateAndRefreshSession()
    if (user == null) {
        // Clear any invalid cookie
        response.cookies.append(
            Cookie(
                name = "session",
                value = "",
                maxAge = 0,
                httpOnly = true,
                path = "/"
            )
        )
        respond(HttpStatusCode.Unauthorized, mapOf(
            "success" to false,
            "message" to "Authentication required"
        ))
        return null
    }
    return user
}

/**
 * Scheduled job to clean up expired sessions.
 * Should be called periodically (e.g., every hour).
 */
fun cleanupExpiredSessions(): Int {
    return SessionRepository.deleteExpired()
}
