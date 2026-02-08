package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlin.random.Random

/**
 * Service for handling search mechanics for hidden ground items.
 *
 * Items that sit on the ground for more than 24 hours become hidden.
 * Players can use the Search ability to attempt to find hidden items.
 * Intelligence and thief-type classes have bonuses to searching.
 * Once an item is found, it remains visible to that player.
 */
object SearchService {
    private val log = org.slf4j.LoggerFactory.getLogger("SearchService")

    /**
     * Classes that get search bonuses (thief-types).
     */
    private val searchClasses = setOf(
        "rogue", "thief", "assassin", "ranger", "scout", "ninja", "shadow", "treasure hunter"
    )

    /**
     * Check if a character class is a search-focused class.
     */
    fun isSearchClass(characterClass: CharacterClass?): Boolean {
        if (characterClass == null) return false
        val className = characterClass.name.lowercase()
        return searchClasses.any { className.contains(it) }
    }

    /**
     * Get search class bonus (percentage added to search checks).
     */
    fun getSearchClassBonus(characterClass: CharacterClass?): Int {
        return if (isSearchClass(characterClass)) 25 else 0
    }

    /**
     * Result of a search attempt.
     */
    data class SearchResult(
        val success: Boolean,
        val message: String,
        val discoveredItems: List<LocationItem> = emptyList(),
        val totalHidden: Int = 0
    )

    /**
     * Attempt to search the current location for hidden items.
     *
     * @param user The player performing the search
     * @param locationId The location to search
     * @return SearchResult with any discovered items
     */
    fun attemptSearch(user: User, locationId: String): SearchResult {
        // Can't search while in combat
        if (user.currentCombatSessionId != null) {
            return SearchResult(
                success = false,
                message = "You cannot search while in combat!"
            )
        }

        // Get hidden items the user hasn't found yet
        val hiddenItems = LocationItemRepository.getHiddenItemsForUser(locationId, user.id)

        if (hiddenItems.isEmpty()) {
            return SearchResult(
                success = true,
                message = "You search the area carefully but find nothing hidden.",
                totalHidden = 0
            )
        }

        // Calculate search chance
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
        val classBonus = getSearchClassBonus(characterClass)
        val searchChance = StatModifierService.searchChance(user.intelligence, user.level, classBonus)

        log.debug("${user.name} searching with ${searchChance}% chance (INT=${user.intelligence}, level=${user.level}, classBonus=$classBonus)")

        // Roll for each hidden item
        val discoveredItems = mutableListOf<LocationItem>()
        for (item in hiddenItems) {
            val roll = Random.nextInt(100)
            if (roll < searchChance) {
                // Discovered!
                LocationItemRepository.discoverItem(user.id, item.id)
                discoveredItems.add(item)
                log.debug("${user.name} discovered item ${item.itemId} (roll=$roll < $searchChance)")
            } else {
                log.debug("${user.name} missed item ${item.itemId} (roll=$roll >= $searchChance)")
            }
        }

        // Build response message
        val message = when {
            discoveredItems.isEmpty() ->
                "You search carefully but don't find anything hidden."
            discoveredItems.size == 1 -> {
                val item = ItemRepository.findById(discoveredItems[0].itemId)
                "You discover ${item?.name ?: "something"} hidden in the area!"
            }
            else -> {
                val itemNames = discoveredItems.mapNotNull {
                    ItemRepository.findById(it.itemId)?.name
                }
                "You discover hidden items: ${itemNames.joinToString(", ")}!"
            }
        }

        return SearchResult(
            success = true,
            message = message,
            discoveredItems = discoveredItems,
            totalHidden = hiddenItems.size
        )
    }

    /**
     * Get visible item IDs for a user at a location.
     * This returns the actual item IDs (not location item IDs) that the user can see.
     */
    fun getVisibleItemIds(locationId: String, userId: String): List<String> {
        val visibleLocationItems = LocationItemRepository.getVisibleItemsForUser(locationId, userId)
        return visibleLocationItems.map { it.itemId }
    }

    /**
     * Check if there are any hidden items the user hasn't discovered.
     */
    fun hasHiddenItems(locationId: String, userId: String): Boolean {
        return LocationItemRepository.getHiddenItemsForUser(locationId, userId).isNotEmpty()
    }

    /**
     * Get count of hidden items the user hasn't discovered.
     */
    fun getHiddenItemCount(locationId: String, userId: String): Int {
        return LocationItemRepository.getHiddenItemsForUser(locationId, userId).size
    }
}
