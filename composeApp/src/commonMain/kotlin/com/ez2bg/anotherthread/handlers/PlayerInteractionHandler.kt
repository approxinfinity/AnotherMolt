package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.EventLogType
import com.ez2bg.anotherthread.state.UserStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for player interaction UI.
 */
data class PlayerInteractionState(
    val selectedPlayer: UserDto? = null,
    val showPlayerInteractionModal: Boolean = false,
    val showGiveItemModal: Boolean = false
)

/**
 * Singleton handler for player interaction business logic.
 * Manages player selection, trading, robbing, and party management.
 */
object PlayerInteractionHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(PlayerInteractionState())
    val state: StateFlow<PlayerInteractionState> = _state.asStateFlow()

    /**
     * Select a player to interact with (shows the player interaction modal).
     */
    fun selectPlayer(player: UserDto) {
        _state.update {
            it.copy(
                selectedPlayer = player,
                showPlayerInteractionModal = true
            )
        }
    }

    /**
     * Dismiss the player interaction modal.
     */
    fun dismissPlayerInteractionModal() {
        _state.update {
            it.copy(
                showPlayerInteractionModal = false,
                showGiveItemModal = false
            )
        }
    }

    /**
     * Show the give item modal for the selected player.
     */
    fun showGiveItemModal() {
        _state.update {
            it.copy(showGiveItemModal = true)
        }
    }

    /**
     * Dismiss just the give item modal (back to player interaction).
     */
    fun dismissGiveItemModal() {
        _state.update {
            it.copy(showGiveItemModal = false)
        }
    }

    /**
     * Give an item to the selected player.
     */
    fun giveItemToPlayer(itemId: String) {
        val targetPlayer = _state.value.selectedPlayer ?: return
        val myUserId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.giveItem(myUserId, targetPlayer.id, itemId)
                .onSuccess { response ->
                    // Update our user state with new inventory
                    UserStateHolder.updateUser(response.giver)
                    CombatStateHolder.addEventLogEntry("Gave ${response.itemName} to ${targetPlayer.name}", EventLogType.INFO)
                    dismissPlayerInteractionModal()
                }
                .onFailure { error ->
                    CombatStateHolder.addEventLogEntry("Failed to give item: ${error.message}", EventLogType.ERROR)
                }
        }
    }

    /**
     * Initiate attack against another player (PvP).
     * Note: Full PvP combat implementation may need additional work.
     */
    fun attackPlayer(player: UserDto) {
        CombatStateHolder.addEventLogEntry("Attacking ${player.name}!", EventLogType.INFO)
        // TODO: Implement PvP combat when ready
        dismissPlayerInteractionModal()
    }

    /**
     * Attempt to rob the selected player.
     * Uses DEX-based pickpocket mechanics. Success steals gold, failure alerts target.
     */
    fun robPlayer(player: UserDto) {
        val userId = UserStateHolder.userId ?: return
        CombatStateHolder.addEventLogEntry("Attempting to rob ${player.name}...", EventLogType.INFO)
        dismissPlayerInteractionModal()

        scope.launch {
            ApiClient.robPlayer(userId, player.id).onSuccess { result ->
                CombatStateHolder.addEventLogEntry(result.message, EventLogType.INFO)
                if (result.success && result.goldStolen > 0) {
                    // Refresh our user data to show updated gold
                    UserStateHolder.refreshUser()
                }
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Rob attempt failed: ${error.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Invite the selected player to party.
     * Both players must be at the same location.
     */
    fun inviteToParty(player: UserDto) {
        val userId = UserStateHolder.userId ?: return
        dismissPlayerInteractionModal()

        scope.launch {
            ApiClient.inviteToParty(userId, player.id).onSuccess { response ->
                CombatStateHolder.addEventLogEntry(response.message, EventLogType.INFO)
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Failed to invite to party: ${error.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Accept a pending party invite from another player.
     * Makes the inviter the party leader.
     */
    fun acceptPartyInvite(player: UserDto) {
        val userId = UserStateHolder.userId ?: return
        dismissPlayerInteractionModal()

        scope.launch {
            ApiClient.acceptPartyInvite(userId, player.id).onSuccess { response ->
                CombatStateHolder.addEventLogEntry(response.message, EventLogType.INFO)
                // Clear the pending invite from state
                CombatStateHolder.clearPendingPartyInvite()
                // Refresh user data to get the partyLeaderId
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Failed to accept party invite: ${error.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Leave the current party.
     */
    fun leaveParty() {
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.leaveParty(userId).onSuccess { response ->
                CombatStateHolder.addEventLogEntry(response.message, EventLogType.INFO)
                // Refresh user data to clear partyLeaderId
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Failed to leave party: ${error.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Disband the party (leader only).
     */
    fun disbandParty() {
        val userId = UserStateHolder.userId ?: return

        scope.launch {
            ApiClient.disbandParty(userId).onSuccess { response ->
                CombatStateHolder.addEventLogEntry(response.message, EventLogType.INFO)
                // Refresh user data to update isPartyLeader
                UserStateHolder.refreshUser()
            }.onFailure { error ->
                CombatStateHolder.addEventLogEntry("Failed to disband party: ${error.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Clear all player interaction state.
     */
    fun clearState() {
        _state.value = PlayerInteractionState()
    }
}
