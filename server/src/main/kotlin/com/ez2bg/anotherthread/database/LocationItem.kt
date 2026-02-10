package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Tracks items that have been dropped/placed on the ground at a location.
 * Items become hidden after a configurable time and require searching to find.
 * Once a player discovers an item, it remains visible to them.
 */
object LocationItemTable : Table("location_item") {
    val id = varchar("id", 36)
    val locationId = varchar("location_id", 64)
    val itemId = varchar("item_id", 36)
    val droppedAt = long("dropped_at")  // Timestamp when item was placed
    val droppedByUserId = varchar("dropped_by_user_id", 36).nullable()  // Who dropped it (null = spawned)

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, locationId)
        index(false, itemId)
    }
}

/**
 * Tracks which users have discovered which hidden items.
 * Once discovered, the item remains visible to that user until picked up.
 */
object DiscoveredItemTable : Table("discovered_item") {
    val userId = varchar("user_id", 36)
    val locationItemId = varchar("location_item_id", 36)
    val discoveredAt = long("discovered_at")

    override val primaryKey = PrimaryKey(userId, locationItemId)
}

data class LocationItem(
    val id: String = UUID.randomUUID().toString(),
    val locationId: String,
    val itemId: String,
    val droppedAt: Long = System.currentTimeMillis(),
    val droppedByUserId: String? = null
) {
    companion object {
        // Items become hidden after 24 hours (in milliseconds)
        const val HIDDEN_AFTER_MS = 24 * 60 * 60 * 1000L
    }

    /**
     * Check if this item is currently hidden (dropped more than 24 hours ago).
     */
    fun isHidden(): Boolean {
        val elapsed = System.currentTimeMillis() - droppedAt
        return elapsed >= HIDDEN_AFTER_MS
    }
}

data class DiscoveredItem(
    val userId: String,
    val locationItemId: String,
    val discoveredAt: Long = System.currentTimeMillis()
)

object LocationItemRepository {
    private val log = org.slf4j.LoggerFactory.getLogger("LocationItemRepository")

    private fun ResultRow.toLocationItem() = LocationItem(
        id = this[LocationItemTable.id],
        locationId = this[LocationItemTable.locationId],
        itemId = this[LocationItemTable.itemId],
        droppedAt = this[LocationItemTable.droppedAt],
        droppedByUserId = this[LocationItemTable.droppedByUserId]
    )

    /**
     * Add an item to a location (when dropped by player or spawned).
     */
    fun addItem(locationId: String, itemId: String, droppedByUserId: String? = null): LocationItem = transaction {
        val locationItem = LocationItem(
            locationId = locationId,
            itemId = itemId,
            droppedByUserId = droppedByUserId
        )

        LocationItemTable.insert {
            it[id] = locationItem.id
            it[LocationItemTable.locationId] = locationItem.locationId
            it[LocationItemTable.itemId] = locationItem.itemId
            it[droppedAt] = locationItem.droppedAt
            it[LocationItemTable.droppedByUserId] = locationItem.droppedByUserId
        }

        locationItem
    }

    /**
     * Add a hidden item to a location (immediately hidden, requires search to find).
     * Sets droppedAt to the past so isHidden() returns true.
     */
    fun addHiddenItem(locationId: String, itemId: String, droppedByUserId: String? = null): LocationItem = transaction {
        val locationItem = LocationItem(
            locationId = locationId,
            itemId = itemId,
            droppedByUserId = droppedByUserId,
            droppedAt = System.currentTimeMillis() - LocationItem.HIDDEN_AFTER_MS - 1000  // Set in past to make immediately hidden
        )

        LocationItemTable.insert {
            it[id] = locationItem.id
            it[LocationItemTable.locationId] = locationItem.locationId
            it[LocationItemTable.itemId] = locationItem.itemId
            it[droppedAt] = locationItem.droppedAt
            it[LocationItemTable.droppedByUserId] = locationItem.droppedByUserId
        }

        locationItem
    }

    /**
     * Add multiple items at once (e.g., loot drop from combat).
     */
    fun addItems(locationId: String, itemIds: List<String>, droppedByUserId: String? = null): List<LocationItem> = transaction {
        itemIds.map { itemId ->
            val locationItem = LocationItem(
                locationId = locationId,
                itemId = itemId,
                droppedByUserId = droppedByUserId
            )

            LocationItemTable.insert {
                it[id] = locationItem.id
                it[LocationItemTable.locationId] = locationItem.locationId
                it[LocationItemTable.itemId] = locationItem.itemId
                it[droppedAt] = locationItem.droppedAt
                it[LocationItemTable.droppedByUserId] = locationItem.droppedByUserId
            }

            locationItem
        }
    }

    /**
     * Find all items at a location.
     */
    fun findByLocation(locationId: String): List<LocationItem> = transaction {
        LocationItemTable.selectAll()
            .where { LocationItemTable.locationId eq locationId }
            .map { it.toLocationItem() }
    }

    /**
     * Find a specific location item by ID.
     */
    fun findById(id: String): LocationItem? = transaction {
        LocationItemTable.selectAll()
            .where { LocationItemTable.id eq id }
            .map { it.toLocationItem() }
            .singleOrNull()
    }

    /**
     * Find location item by item ID (note: items can exist at multiple locations).
     * Returns the first match if multiple exist.
     */
    fun findByItemId(itemId: String, locationId: String): LocationItem? = transaction {
        LocationItemTable.selectAll()
            .where { (LocationItemTable.itemId eq itemId) and (LocationItemTable.locationId eq locationId) }
            .map { it.toLocationItem() }
            .firstOrNull()
    }

    /**
     * Remove an item from a location (when picked up).
     */
    fun removeItem(id: String): Boolean = transaction {
        // Also remove any discovery records for this item
        DiscoveredItemTable.deleteWhere { locationItemId eq id }

        LocationItemTable.deleteWhere { LocationItemTable.id eq id } > 0
    }

    /**
     * Remove item by itemId from a specific location.
     */
    fun removeItemByItemId(itemId: String, locationId: String): Boolean = transaction {
        val locationItem = findByItemId(itemId, locationId)
        if (locationItem != null) {
            DiscoveredItemTable.deleteWhere { DiscoveredItemTable.locationItemId eq locationItem.id }
            LocationItemTable.deleteWhere { LocationItemTable.id eq locationItem.id } > 0
        } else {
            false
        }
    }

    /**
     * Get visible items for a user at a location.
     * Returns items that are either:
     * - Not hidden (dropped within 24 hours)
     * - Already discovered by this user
     */
    fun getVisibleItemsForUser(locationId: String, userId: String): List<LocationItem> = transaction {
        val allItems = findByLocation(locationId)
        val discoveredIds = getDiscoveredItemIds(userId, locationId)

        allItems.filter { item ->
            !item.isHidden() || item.id in discoveredIds
        }
    }

    /**
     * Get hidden items that a user hasn't discovered yet.
     */
    fun getHiddenItemsForUser(locationId: String, userId: String): List<LocationItem> = transaction {
        val allItems = findByLocation(locationId)
        val discoveredIds = getDiscoveredItemIds(userId, locationId)

        allItems.filter { item ->
            item.isHidden() && item.id !in discoveredIds
        }
    }

    /**
     * Mark an item as discovered by a user.
     */
    fun discoverItem(userId: String, locationItemId: String): Boolean = transaction {
        try {
            DiscoveredItemTable.insert {
                it[DiscoveredItemTable.userId] = userId
                it[DiscoveredItemTable.locationItemId] = locationItemId
                it[discoveredAt] = System.currentTimeMillis()
            }
            true
        } catch (e: Exception) {
            // Already discovered
            false
        }
    }

    /**
     * Get IDs of items this user has discovered at a location.
     */
    private fun getDiscoveredItemIds(userId: String, locationId: String): Set<String> = transaction {
        val allLocationItemIds = LocationItemTable.selectAll()
            .where { LocationItemTable.locationId eq locationId }
            .map { it[LocationItemTable.id] }
            .toSet()

        DiscoveredItemTable.selectAll()
            .where { (DiscoveredItemTable.userId eq userId) and (DiscoveredItemTable.locationItemId inList allLocationItemIds) }
            .map { it[DiscoveredItemTable.locationItemId] }
            .toSet()
    }

    /**
     * Check if a user has discovered a specific item.
     */
    fun hasDiscovered(userId: String, locationItemId: String): Boolean = transaction {
        DiscoveredItemTable.selectAll()
            .where { (DiscoveredItemTable.userId eq userId) and (DiscoveredItemTable.locationItemId eq locationItemId) }
            .count() > 0
    }
}
