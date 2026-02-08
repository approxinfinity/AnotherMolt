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
    CREATURE_ADDED         // Creature spawned/entered location
}

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
    val creatureName: String? = null      // For display purposes
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
        playerLocations[userId] = locationId
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
     * Broadcast to all players observing a specific location.
     */
    private suspend fun broadcastToLocationObservers(locationId: String, event: LocationMutationEvent) {
        val jsonMessage = json.encodeToString(event)

        // Find all players at this location
        val observersAtLocation = playerLocations.filter { it.value == locationId }.keys

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
}
