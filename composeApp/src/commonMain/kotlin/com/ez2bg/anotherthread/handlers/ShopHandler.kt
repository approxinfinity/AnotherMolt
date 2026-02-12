package com.ez2bg.anotherthread.handlers

import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.SellableItemDto
import com.ez2bg.anotherthread.data.AdventureRepository
import com.ez2bg.anotherthread.state.CombatStateHolder
import com.ez2bg.anotherthread.state.EventLogType
import com.ez2bg.anotherthread.state.UserStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for shop-related UI.
 */
data class ShopState(
    val shopItems: List<ItemDto> = emptyList(),
    val sellableItems: List<SellableItemDto> = emptyList(),
    val showSellModal: Boolean = false,
    val isShopBanned: Boolean = false,
    val shopBanMessage: String? = null,
    val playerGold: Int = 0
)

/**
 * One-time shop events for UI handling.
 */
sealed class ShopEvent {
    data class ShowSnackbar(val message: String) : ShopEvent()
}

/**
 * Singleton handler for shop business logic.
 * Manages shop items, selling, buying, and inn rest.
 */
object ShopHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _shopState = MutableStateFlow(ShopState())
    val shopState: StateFlow<ShopState> = _shopState.asStateFlow()

    private val _events = MutableSharedFlow<ShopEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ShopEvent> = _events.asSharedFlow()

    // Known shop location IDs
    private val shopLocationIds = setOf(
        "tun-du-lac-magic-shop",
        "tun-du-lac-armor-shop",
        "tun-du-lac-weapons-shop",
        "tun-du-lac-general-store",
        "location-hermits-hollow"
    )
    private val innLocationId = "tun-du-lac-inn"
    private val generalStoreLocationId = "tun-du-lac-general-store"

    /**
     * Check if a location is a shop location.
     */
    fun isShopLocation(locationId: String): Boolean = locationId in shopLocationIds

    /**
     * Check if a location is the inn.
     */
    fun isInnLocation(locationId: String): Boolean = locationId == innLocationId

    /**
     * Check if a location is the general store.
     */
    fun isGeneralStore(locationId: String): Boolean = locationId == generalStoreLocationId

    /**
     * Load shop items from the repository.
     */
    fun loadShopItems(locationId: String) {
        scope.launch {
            val location = AdventureRepository.getLocation(locationId) ?: return@launch
            val itemIds = location.itemIds
            if (itemIds.isEmpty()) return@launch

            // Load each item's details
            ApiClient.getItems().onSuccess { allItems ->
                val shopItems = allItems.filter { it.id in itemIds }
                _shopState.update { it.copy(shopItems = shopItems) }
            }
        }
    }

    /**
     * Load shop items directly from API, without relying on repository.
     * Used during initial load when repository might not be ready yet.
     */
    fun loadShopItemsFromApi(locationId: String) {
        scope.launch {
            // Fetch the location directly from API
            ApiClient.getLocation(locationId).onSuccess { location ->
                if (location == null) return@onSuccess
                val itemIds = location.itemIds
                if (itemIds.isEmpty()) return@onSuccess

                // Load items
                ApiClient.getItems().onSuccess { allItems ->
                    val shopItems = allItems.filter { it.id in itemIds }
                    _shopState.update { it.copy(shopItems = shopItems) }
                }
            }
            // Also load sellable items if at general store
            if (locationId == generalStoreLocationId) {
                loadSellableItems(locationId)
            }
        }
    }

    /**
     * Load sellable items for the general store.
     * Also checks if the user is banned from the store.
     */
    fun loadSellableItems(locationId: String) {
        val userId = UserStateHolder.userId ?: return
        scope.launch {
            // First check ban status
            ApiClient.getShopBanStatus(locationId, userId).onSuccess { banResponse ->
                if (banResponse.isBanned) {
                    _shopState.update {
                        it.copy(
                            isShopBanned = true,
                            shopBanMessage = banResponse.message,
                            sellableItems = emptyList()
                        )
                    }
                    // Show the ban message in the event log
                    banResponse.message?.let { msg ->
                        CombatStateHolder.addEventLogEntry(msg, EventLogType.ERROR)
                    }
                    return@onSuccess
                }

                // Not banned, load sellable items
                _shopState.update { it.copy(isShopBanned = false, shopBanMessage = null) }
                ApiClient.getSellableItems(locationId, userId).onSuccess { response ->
                    if (response.success) {
                        _shopState.update { it.copy(sellableItems = response.items) }
                    }
                }.onFailure {
                    println("[ShopHandler] Failed to load sellable items: ${it.message}")
                }
            }.onFailure {
                println("[ShopHandler] Failed to check shop ban status: ${it.message}")
                // Still try to load items if ban check fails
                ApiClient.getSellableItems(locationId, userId).onSuccess { response ->
                    if (response.success) {
                        _shopState.update { it.copy(sellableItems = response.items) }
                    }
                }
            }
        }
    }

    /**
     * Show the sell modal at the general store.
     */
    fun openSellModal() {
        val currentLocationId = AdventureRepository.currentLocationId.value
        if (currentLocationId != generalStoreLocationId) return

        // Check if banned - don't open modal, just show message
        if (_shopState.value.isShopBanned) {
            _shopState.value.shopBanMessage?.let { msg ->
                CombatStateHolder.addEventLogEntry(msg, EventLogType.ERROR)
            }
            return
        }

        loadSellableItems(generalStoreLocationId)
        _shopState.update { it.copy(showSellModal = true) }
    }

    /**
     * Close the sell modal.
     */
    fun closeSellModal() {
        _shopState.update { it.copy(showSellModal = false) }
    }

    /**
     * Sell an item at the general store.
     */
    fun sellItem(sellableItem: SellableItemDto) {
        val userId = UserStateHolder.userId ?: return
        scope.launch {
            val result = if (sellableItem.isFoodItem) {
                ApiClient.sellFoodItem(generalStoreLocationId, userId, sellableItem.id)
            } else {
                ApiClient.sellItem(generalStoreLocationId, userId, sellableItem.itemId)
            }

            result.onSuccess { response ->
                if (response.success) {
                    CombatStateHolder.addEventLogEntry(response.message, EventLogType.LOOT)
                    // Update user state with new gold
                    response.user?.let { user ->
                        UserStateHolder.updateUser(user)
                        _shopState.update { it.copy(playerGold = user.gold) }
                    }
                    // Refresh sellable items
                    loadSellableItems(generalStoreLocationId)
                } else {
                    // Check if this was a cursed item attempt - the message will contain the ban
                    CombatStateHolder.addEventLogEntry(response.message, EventLogType.ERROR)
                    // Reload to get updated ban status and close modal
                    loadSellableItems(generalStoreLocationId)
                    closeSellModal()
                }
            }.onFailure { error ->
                // Server returned an error - likely banned or cursed item
                val errorMessage = error.message ?: "Failed to sell item"
                CombatStateHolder.addEventLogEntry(errorMessage, EventLogType.ERROR)
                // Reload to check for ban status
                loadSellableItems(generalStoreLocationId)
                closeSellModal()
            }
        }
    }

    /**
     * Buy an item from a shop.
     */
    fun buyItem(itemId: String, locationId: String) {
        val userId = UserStateHolder.userId ?: return
        scope.launch {
            ApiClient.buyItem(locationId, userId, itemId).onSuccess { response ->
                if (response.success) {
                    // Update user state with new inventory and gold
                    response.user?.let { user ->
                        _shopState.update { it.copy(playerGold = user.gold) }
                        UserStateHolder.updateUser(user)
                    }
                    emitEvent(ShopEvent.ShowSnackbar(response.message))
                } else {
                    emitEvent(ShopEvent.ShowSnackbar(response.message))
                }
            }.onFailure {
                CombatStateHolder.addEventLogEntry("Purchase failed: ${it.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Rest at the inn to restore HP/MP/SP.
     */
    fun restAtInn(locationId: String) {
        val userId = UserStateHolder.userId ?: return
        scope.launch {
            ApiClient.restAtInn(locationId, userId).onSuccess { response ->
                if (response.success) {
                    // Update user state with restored HP/MP/SP and spent gold
                    response.user?.let { user ->
                        _shopState.update { it.copy(playerGold = user.gold) }
                        UserStateHolder.updateUser(user)
                    }
                    emitEvent(ShopEvent.ShowSnackbar(response.message))
                } else {
                    emitEvent(ShopEvent.ShowSnackbar(response.message))
                }
            }.onFailure {
                CombatStateHolder.addEventLogEntry("Rest failed: ${it.message}", EventLogType.ERROR)
            }
        }
    }

    /**
     * Update player gold when it changes from other sources.
     */
    fun updatePlayerGold(gold: Int) {
        _shopState.update { it.copy(playerGold = gold) }
    }

    /**
     * Clear shop state (e.g., when leaving shop location).
     */
    fun clearShopState() {
        _shopState.update {
            ShopState(playerGold = it.playerGold)  // Preserve gold
        }
    }

    private fun emitEvent(event: ShopEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
