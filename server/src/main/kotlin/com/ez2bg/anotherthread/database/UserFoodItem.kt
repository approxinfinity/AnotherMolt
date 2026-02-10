package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Tracks perishable food items in user inventory with spoil dates.
 *
 * Food items have a spoilAt timestamp. When current time exceeds spoilAt,
 * the food is spoiled and should be discarded or have negative effects.
 *
 * Spoil times:
 * - Raw fish: 1 day from catch
 * - Cooked fish: 7 days from cooking
 * - Salted fish: 90 days (3 months) from salting
 */
object UserFoodItemTable : Table("user_food_item") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val itemId = varchar("item_id", 64)  // References the item template
    val acquiredAt = long("acquired_at")  // When the item was obtained
    val spoilAt = long("spoil_at")  // When the item will spoil
    val state = varchar("state", 20).default("raw")  // "raw", "cooked", "salted"

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, userId)
        index(false, itemId)
        index(false, spoilAt)
    }
}

data class UserFoodItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val itemId: String,
    val acquiredAt: Long = System.currentTimeMillis(),
    val spoilAt: Long,
    val state: String = "raw"  // "raw", "cooked", "salted"
) {
    companion object {
        // Spoil durations in milliseconds
        const val RAW_SPOIL_MS = 24 * 60 * 60 * 1000L  // 1 day
        const val COOKED_SPOIL_MS = 7 * 24 * 60 * 60 * 1000L  // 7 days
        const val SALTED_SPOIL_MS = 90L * 24 * 60 * 60 * 1000L  // 90 days (3 months)
    }

    /**
     * Check if this food item has spoiled.
     */
    fun isSpoiled(): Boolean {
        return System.currentTimeMillis() >= spoilAt
    }

    /**
     * Get time remaining before spoilage in human-readable format.
     */
    fun getTimeUntilSpoil(): String {
        val remaining = spoilAt - System.currentTimeMillis()
        if (remaining <= 0) return "Spoiled"

        val days = remaining / (24 * 60 * 60 * 1000)
        val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""}"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
            else -> "Less than 1 hour"
        }
    }
}

object UserFoodItemRepository {
    private val log = org.slf4j.LoggerFactory.getLogger("UserFoodItemRepository")

    private fun ResultRow.toUserFoodItem() = UserFoodItem(
        id = this[UserFoodItemTable.id],
        userId = this[UserFoodItemTable.userId],
        itemId = this[UserFoodItemTable.itemId],
        acquiredAt = this[UserFoodItemTable.acquiredAt],
        spoilAt = this[UserFoodItemTable.spoilAt],
        state = this[UserFoodItemTable.state]
    )

    /**
     * Add a food item to a user's inventory.
     */
    fun add(item: UserFoodItem): UserFoodItem = transaction {
        UserFoodItemTable.insert {
            it[id] = item.id
            it[userId] = item.userId
            it[itemId] = item.itemId
            it[acquiredAt] = item.acquiredAt
            it[spoilAt] = item.spoilAt
            it[state] = item.state
        }
        item
    }

    /**
     * Find all food items for a user.
     */
    fun findByUserId(userId: String): List<UserFoodItem> = transaction {
        UserFoodItemTable.selectAll()
            .where { UserFoodItemTable.userId eq userId }
            .map { it.toUserFoodItem() }
    }

    /**
     * Find a specific food item by ID.
     */
    fun findById(id: String): UserFoodItem? = transaction {
        UserFoodItemTable.selectAll()
            .where { UserFoodItemTable.id eq id }
            .map { it.toUserFoodItem() }
            .singleOrNull()
    }

    /**
     * Find food items of a specific type for a user.
     */
    fun findByUserAndItemId(userId: String, itemId: String): List<UserFoodItem> = transaction {
        UserFoodItemTable.selectAll()
            .where { (UserFoodItemTable.userId eq userId) and (UserFoodItemTable.itemId eq itemId) }
            .map { it.toUserFoodItem() }
    }

    /**
     * Update a food item (e.g., when cooking or salting).
     */
    fun update(item: UserFoodItem): Boolean = transaction {
        UserFoodItemTable.update({ UserFoodItemTable.id eq item.id }) {
            it[itemId] = item.itemId
            it[spoilAt] = item.spoilAt
            it[state] = item.state
        } > 0
    }

    /**
     * Remove a food item from inventory (when eaten or discarded).
     */
    fun delete(id: String): Boolean = transaction {
        UserFoodItemTable.deleteWhere { UserFoodItemTable.id eq id } > 0
    }

    /**
     * Remove all spoiled food items for a user.
     */
    fun deleteSpoiled(userId: String): Int = transaction {
        val now = System.currentTimeMillis()
        UserFoodItemTable.deleteWhere {
            (UserFoodItemTable.userId eq userId) and (UserFoodItemTable.spoilAt lessEq now)
        }
    }

    /**
     * Count spoiled food items for a user.
     */
    fun countSpoiled(userId: String): Int = transaction {
        val now = System.currentTimeMillis()
        UserFoodItemTable.selectAll()
            .where { (UserFoodItemTable.userId eq userId) and (UserFoodItemTable.spoilAt lessEq now) }
            .count()
            .toInt()
    }

    /**
     * Get all non-spoiled food items for a user.
     */
    fun findFreshByUserId(userId: String): List<UserFoodItem> = transaction {
        val now = System.currentTimeMillis()
        UserFoodItemTable.selectAll()
            .where { (UserFoodItemTable.userId eq userId) and (UserFoodItemTable.spoilAt greater now) }
            .map { it.toUserFoodItem() }
    }
}
