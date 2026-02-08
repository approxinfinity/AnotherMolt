package com.ez2bg.anotherthread.events

import com.ez2bg.anotherthread.database.Exit
import com.ez2bg.anotherthread.database.Location
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Event types for location mutations.
 */
enum class LocationEventType {
    LOCATION_UPDATED,      // General location update (exits, items, etc.)
    EXIT_ADDED,            // New exit added to location
    EXIT_REMOVED,          // Exit removed from location
    ITEM_ADDED,            // Item added to location
    ITEM_REMOVED,          // Item removed from location
    CREATURE_REMOVED,      // Creature killed/left location
    CREATURE_ADDED,        // Creature spawned/entered location
    STEALTH_DETECTED,      // Observer detected a hidden/sneaking character
    PLAYER_ENTERED,        // Player entered the location
    PLAYER_LEFT            // Player left the location
}

/**
 * Stealth detection event sent to a specific player.
 */
@Serializable
data class StealthDetectionEvent(
    val type: String = "STEALTH_DETECTION",
    val observerId: String,
    val targetId: String,
    val targetName: String,
    val stealthType: String,  // "HIDING" or "SNEAKING"
    val message: String
)

/**
 * Item received event sent to a specific player when given an item.
 */
@Serializable
data class ItemReceivedEvent(
    val type: String = "ITEM_RECEIVED",
    val receiverId: String,
    val giverId: String,
    val giverName: String,
    val itemId: String,
    val itemName: String,
    val message: String
)

/**
 * Session invalidated event sent when user logs in on another device.
 * Notifies existing sessions to log out.
 */
@Serializable
data class SessionInvalidatedEvent(
    val type: String = "SESSION_INVALIDATED",
    val userId: String,
    val reason: String = "signed_in_elsewhere",
    val message: String
)

/**
 * Location mutation event sent to clients.
 */
@Serializable
data class LocationMutationEvent(
    val type: String,                    // "LOCATION_MUTATION"
    val eventType: LocationEventType,
    val locationId: String,
    val areaId: String?,
    val gridX: Int?,
    val gridY: Int?,
    val locationName: String,
    val exitAdded: Exit? = null,
    val exitRemoved: Exit? = null,
    val itemIdAdded: String? = null,
    val itemIdRemoved: String? = null,
    val creatureIdRemoved: String? = null,
    val creatureIdAdded: String? = null,
    val creatureName: String? = null,     // For display purposes
    val playerId: String? = null,         // Player who entered/left
    val playerName: String? = null        // Player name for display
)

/**
 * Service for broadcasting location mutation events to connected clients.
 *
 * Clients subscribe by location coordinates (areaId, gridX, gridY) and receive
 * events when the location they're observing changes.
 */
object LocationEventService {
    private val log = LoggerFactory.getLogger(LocationEventService::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // WebSocket connections by user ID (shared with CombatService for now)
    private val playerConnections = ConcurrentHashMap<String, WebSocketSession>()

    // Track which location each player is observing (by user ID -> location ID)
    private val playerLocations = ConcurrentHashMap<String, String>()

    /**
     * Register a player's WebSocket connection.
     */
    fun registerConnection(userId: String, session: WebSocketSession) {
        playerConnections[userId] = session
        log.debug("Player $userId registered for location events")
    }

    /**
     * Unregister a player's WebSocket connection.
     */
    fun unregisterConnection(userId: String) {
        playerConnections.remove(userId)
        playerLocations.remove(userId)
        log.debug("Player $userId unregistered from location events")
    }

    /**
     * Update which location a player is observing.
     */
    fun updatePlayerLocation(userId: String, locationId: String) {
        val oldLocation = playerLocations[userId]
        playerLocations[userId] = locationId
        log.info("Player $userId location updated: $oldLocation -> $locationId (total tracked: ${playerLocations.size})")
    }

    /**
     * Broadcast that an exit was added to a location.
     */
    suspend fun broadcastExitAdded(location: Location, exit: Exit) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.EXIT_ADDED,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name,
            exitAdded = exit
        )
        broadcastToLocationObservers(location.id, event)
    }

    /**
     * Broadcast that an exit was removed from a location.
     */
    suspend fun broadcastExitRemoved(location: Location, exit: Exit) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.EXIT_REMOVED,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name,
            exitRemoved = exit
        )
        broadcastToLocationObservers(location.id, event)
    }

    /**
     * Broadcast that an item was added to a location.
     */
    suspend fun broadcastItemAdded(location: Location, itemId: String) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.ITEM_ADDED,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name,
            itemIdAdded = itemId
        )
        broadcastToLocationObservers(location.id, event)
    }

    /**
     * Broadcast that an item was removed from a location.
     */
    suspend fun broadcastItemRemoved(location: Location, itemId: String) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.ITEM_REMOVED,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name,
            itemIdRemoved = itemId
        )
        broadcastToLocationObservers(location.id, event)
    }

    /**
     * Broadcast a general location update.
     */
    suspend fun broadcastLocationUpdated(location: Location) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.LOCATION_UPDATED,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name
        )
        broadcastToLocationObservers(location.id, event)
    }

    /**
     * Broadcast that a creature was removed from a location (killed or left).
     */
    suspend fun broadcastCreatureRemoved(location: Location, creatureId: String, creatureName: String) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.CREATURE_REMOVED,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name,
            creatureIdRemoved = creatureId,
            creatureName = creatureName
        )
        broadcastToLocationObservers(location.id, event)
    }

    /**
     * Broadcast that a creature was added to a location (spawned or entered).
     */
    suspend fun broadcastCreatureAdded(location: Location, creatureId: String, creatureName: String) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.CREATURE_ADDED,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name,
            creatureIdAdded = creatureId,
            creatureName = creatureName
        )
        broadcastToLocationObservers(location.id, event)
    }

    /**
     * Broadcast that a player entered a location.
     * Notifies all OTHER players at that location (excludes the entering player).
     */
    suspend fun broadcastPlayerEntered(location: Location, playerId: String, playerName: String) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.PLAYER_ENTERED,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name,
            playerId = playerId,
            playerName = playerName
        )
        broadcastToLocationObservers(location.id, event, excludeUserId = playerId)
    }

    /**
     * Broadcast that a player left a location.
     * Notifies all OTHER players at that location (excludes the leaving player).
     */
    suspend fun broadcastPlayerLeft(location: Location, playerId: String, playerName: String) {
        val event = LocationMutationEvent(
            type = "LOCATION_MUTATION",
            eventType = LocationEventType.PLAYER_LEFT,
            locationId = location.id,
            areaId = location.areaId,
            gridX = location.gridX,
            gridY = location.gridY,
            locationName = location.name,
            playerId = playerId,
            playerName = playerName
        )
        broadcastToLocationObservers(location.id, event, excludeUserId = playerId)
    }

    /**
     * Broadcast to all players observing a specific location.
     * Optionally excludes a specific user (e.g., the player entering/leaving).
     */
    private suspend fun broadcastToLocationObservers(locationId: String, event: LocationMutationEvent, excludeUserId: String? = null) {
        val jsonMessage = json.encodeToString(event)

        // Debug: log all tracked player locations
        log.info("All tracked player locations: ${playerLocations.entries.joinToString { "${it.key}=${it.value}" }}")

        // Find all players at this location (excluding the specified user if any)
        val observersAtLocation = playerLocations
            .filter { it.value == locationId && (excludeUserId == null || it.key != excludeUserId) }
            .keys

        log.info("Looking for observers at $locationId (excluding $excludeUserId): found ${observersAtLocation.size} - $observersAtLocation")

        for (userId in observersAtLocation) {
            val connection = playerConnections[userId]
            if (connection != null) {
                try {
                    connection.send(Frame.Text(jsonMessage))
                    log.debug("Sent location event to player $userId: ${event.eventType}")
                } catch (e: Exception) {
                    log.debug("Failed to send location event to player $userId: ${e.message}")
                }
            }
        }

        log.info("Broadcast ${event.eventType} for location $locationId to ${observersAtLocation.size} observers")
    }

    /**
     * Broadcast to ALL connected players (for global events).
     */
    suspend fun broadcastToAllPlayers(event: LocationMutationEvent) {
        val jsonMessage = json.encodeToString(event)

        for ((userId, connection) in playerConnections) {
            try {
                connection.send(Frame.Text(jsonMessage))
            } catch (e: Exception) {
                log.debug("Failed to broadcast to player $userId: ${e.message}")
            }
        }
    }

    /**
     * Send stealth detection message to a specific player (blocking version for use in routes).
     */
    fun sendStealthDetection(detection: com.ez2bg.anotherthread.game.StealthService.PerceptionResult) {
        kotlinx.coroutines.runBlocking {
            val connection = playerConnections[detection.observerId] ?: return@runBlocking

            val event = StealthDetectionEvent(
                observerId = detection.observerId,
                targetId = detection.targetId,
                targetName = detection.targetName,
                stealthType = detection.stealthType.name,
                message = detection.message
            )

            try {
                connection.send(Frame.Text(json.encodeToString(event)))
                log.debug("Sent stealth detection to ${detection.observerName}: ${detection.message}")
            } catch (e: Exception) {
                log.debug("Failed to send stealth detection to ${detection.observerName}: ${e.message}")
            }
        }
    }

    /**
     * Send item received notification to a specific player (blocking version for use in routes).
     */
    fun sendItemReceived(receiverId: String, giverId: String, giverName: String, itemId: String, itemName: String) {
        kotlinx.coroutines.runBlocking {
            val connection = playerConnections[receiverId] ?: return@runBlocking

            val event = ItemReceivedEvent(
                receiverId = receiverId,
                giverId = giverId,
                giverName = giverName,
                itemId = itemId,
                itemName = itemName,
                message = "$giverName gave you $itemName"
            )

            try {
                connection.send(Frame.Text(json.encodeToString(event)))
                log.debug("Sent item received notification to $receiverId: $giverName gave $itemName")
            } catch (e: Exception) {
                log.debug("Failed to send item received notification to $receiverId: ${e.message}")
            }
        }
    }

    /**
     * Send session invalidation to a user when they log in on another device.
     * This notifies all existing WebSocket connections for that user to log out.
     */
    fun sendSessionInvalidated(userId: String, userName: String) {
        kotlinx.coroutines.runBlocking {
            val connection = playerConnections[userId] ?: return@runBlocking

            val event = SessionInvalidatedEvent(
                userId = userId,
                reason = "signed_in_elsewhere",
                message = "$userName signed in on another device"
            )

            try {
                connection.send(Frame.Text(json.encodeToString(event)))
                log.info("Sent session invalidation to $userId: signed in elsewhere")
            } catch (e: Exception) {
                log.debug("Failed to send session invalidation to $userId: ${e.message}")
            }
        }
    }
}
