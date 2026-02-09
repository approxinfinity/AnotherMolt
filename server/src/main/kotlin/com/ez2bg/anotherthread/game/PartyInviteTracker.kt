package com.ez2bg.anotherthread.game

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory tracker for pending party invitations.
 *
 * When user A invites user B to a party, this stores the pending invite.
 * User B can then see "Accept Party" when tapping on user A.
 * Invites expire after a timeout or when accepted/declined.
 *
 * Thread-safe via ConcurrentHashMap.
 */
object PartyInviteTracker {

    /**
     * Represents a pending party invitation.
     */
    data class PendingInvite(
        val inviterId: String,
        val inviterName: String,
        val inviteeId: String,
        val createdAt: Long
    )

    // inviteeId -> pending invite from inviter
    // Only one pending invite per invitee (latest overwrites)
    private val pendingInvites = ConcurrentHashMap<String, PendingInvite>()

    // Invite timeout (5 minutes)
    private const val INVITE_TIMEOUT_MS = 5 * 60 * 1000L

    /**
     * Create a party invitation from inviter to invitee.
     * Overwrites any existing pending invite for the invitee.
     */
    fun invite(inviterId: String, inviterName: String, inviteeId: String) {
        val invite = PendingInvite(
            inviterId = inviterId,
            inviterName = inviterName,
            inviteeId = inviteeId,
            createdAt = System.currentTimeMillis()
        )
        pendingInvites[inviteeId] = invite
    }

    /**
     * Check if the invitee has a pending invite from a specific inviter.
     * Returns true if there's a valid (non-expired) invite.
     */
    fun hasPendingInviteFrom(inviteeId: String, inviterId: String): Boolean {
        val invite = pendingInvites[inviteeId] ?: return false
        if (invite.inviterId != inviterId) return false

        // Check for timeout
        val elapsed = System.currentTimeMillis() - invite.createdAt
        if (elapsed >= INVITE_TIMEOUT_MS) {
            pendingInvites.remove(inviteeId)
            return false
        }

        return true
    }

    /**
     * Get the pending invite for an invitee, if any.
     * Returns null if no invite or if expired.
     */
    fun getPendingInvite(inviteeId: String): PendingInvite? {
        val invite = pendingInvites[inviteeId] ?: return null

        // Check for timeout
        val elapsed = System.currentTimeMillis() - invite.createdAt
        if (elapsed >= INVITE_TIMEOUT_MS) {
            pendingInvites.remove(inviteeId)
            return null
        }

        return invite
    }

    /**
     * Clear the pending invite for an invitee.
     * Called when invite is accepted, declined, or cancelled.
     */
    fun clearInvite(inviteeId: String) {
        pendingInvites.remove(inviteeId)
    }

    /**
     * Clear all invites from a specific inviter.
     * Called when inviter leaves the location or goes offline.
     */
    fun clearInvitesFrom(inviterId: String) {
        pendingInvites.entries.removeIf { it.value.inviterId == inviterId }
    }

    /**
     * Get all pending invites (for admin/debugging).
     */
    fun getAllPending(): List<PendingInvite> {
        val now = System.currentTimeMillis()
        return pendingInvites.values.filter { invite ->
            now - invite.createdAt < INVITE_TIMEOUT_MS
        }
    }

    /**
     * Clean up any expired invites.
     * Can be called periodically if needed.
     */
    fun cleanupExpired(): Int {
        val now = System.currentTimeMillis()
        var cleaned = 0
        pendingInvites.entries.removeIf { entry ->
            val expired = now - entry.value.createdAt >= INVITE_TIMEOUT_MS
            if (expired) cleaned++
            expired
        }
        return cleaned
    }
}
