package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.*
import com.ez2bg.anotherthread.combat.CombatService
import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.events.LocationEventService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class SetIconMappingRequest(val iconName: String)

@Serializable
data class SearchResponse(
    val success: Boolean,
    val message: String,
    val discoveredItems: List<DiscoveredItemInfo>,
    val totalHidden: Int,
    val hasMoreHidden: Boolean
)

@Serializable
data class DiscoveredItemInfo(
    val id: String,
    val name: String
)

@Serializable
data class UpdateLocationResponse(
    val success: Boolean,
    val combatStarted: Boolean = false,
    val combatSessionId: String? = null,
    val message: String? = null
)

@Serializable
data class EncumbranceErrorResponse(
    val error: String,
    val currentWeight: Int,
    val maxCapacity: Int
)

@Serializable
data class LogoutAllResponse(
    val success: Boolean,
    val message: String,
    val sessionsInvalidated: Int
)

@Serializable
data class SimpleSuccessResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

/**
 * Auth routes for user registration and login.
 * Base path: /auth
 *
 * Session Management:
 * - On successful login/register, creates a session with 7-day sliding expiration
 * - Sets HttpOnly cookie for web clients (automatically sent with requests)
 * - Returns token in response body for native clients (iOS/Android)
 * - Each authenticated request refreshes the session expiry (sliding window)
 */
fun Route.authRoutes() {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()

            // Validate input
            if (request.name.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(
                    success = false,
                    message = "Name and password are required"
                ))
                return@post
            }

            if (request.password.length < 4) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(
                    success = false,
                    message = "Password must be at least 4 characters"
                ))
                return@post
            }

            // Check if username already exists
            if (UserRepository.findByName(request.name) != null) {
                call.respond(HttpStatusCode.Conflict, AuthResponse(
                    success = false,
                    message = "Username already exists"
                ))
                return@post
            }

            // Get universal abilities that all players start with
            val universalAbilityIds = AbilityRepository.findUniversal().map { it.id }

            // Get configured default starting location
            val defaultStartingLocationId = GameConfigRepository.getDefaultStartingLocationId()

            // Create user with hashed password, starting location, and universal abilities
            val user = User(
                name = request.name,
                passwordHash = UserRepository.hashPassword(request.password),
                currentLocationId = defaultStartingLocationId,  // Configurable starting location
                learnedAbilityIds = universalAbilityIds  // Start with universal abilities (Attack, Aid, Drag)
            )
            val createdUser = UserRepository.create(user)

            // Create session
            val session = SessionRepository.create(
                userId = createdUser.id,
                userAgent = call.request.header("User-Agent"),
                ipAddress = call.request.header("X-Forwarded-For")
                    ?: call.request.local.remoteHost
            )

            // Set HttpOnly cookie for web clients
            call.response.cookies.append(
                Cookie(
                    name = "session",
                    value = session.id,
                    maxAge = SessionConfig.SESSION_DURATION_SECONDS,
                    httpOnly = true,
                    path = "/",
                    extensions = mapOf("SameSite" to "Lax")
                )
            )

            call.respond(HttpStatusCode.Created, AuthResponse(
                success = true,
                message = "Registration successful",
                user = createdUser.toResponse(),
                sessionToken = session.id,
                expiresAt = session.expiresAt
            ))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = UserRepository.findByName(request.name)
            if (user == null || !UserRepository.verifyPassword(request.password, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(
                    success = false,
                    message = "Invalid username or password"
                ))
                return@post
            }

            // Update last active timestamp
            UserRepository.updateLastActiveAt(user.id)

            // Ensure user has a starting location (fallback for users created before this fix)
            if (user.currentLocationId == null) {
                val defaultStartingLocationId = GameConfigRepository.getDefaultStartingLocationId()
                UserRepository.updateCurrentLocation(user.id, defaultStartingLocationId)
            }

            // Notify any existing sessions for this user that they've been invalidated
            // This kicks them out when signing in on a new device
            LocationEventService.sendSessionInvalidated(user.id, user.name)

            // Create session
            val session = SessionRepository.create(
                userId = user.id,
                userAgent = call.request.header("User-Agent"),
                ipAddress = call.request.header("X-Forwarded-For")
                    ?: call.request.local.remoteHost
            )

            // Set HttpOnly cookie for web clients
            call.response.cookies.append(
                Cookie(
                    name = "session",
                    value = session.id,
                    maxAge = SessionConfig.SESSION_DURATION_SECONDS,
                    httpOnly = true,
                    path = "/",
                    extensions = mapOf("SameSite" to "Lax")
                )
            )

            call.respond(HttpStatusCode.OK, AuthResponse(
                success = true,
                message = "Login successful",
                user = user.toResponse(),
                sessionToken = session.id,
                expiresAt = session.expiresAt
            ))
        }

        // Logout - invalidate session
        post("/logout") {
            val token = call.request.cookies["session"]
                ?: call.request.header("Authorization")?.removePrefix("Bearer ")

            if (token != null) {
                SessionRepository.delete(token)
            }

            // Clear the cookie
            call.response.cookies.append(
                Cookie(
                    name = "session",
                    value = "",
                    maxAge = 0,
                    httpOnly = true,
                    path = "/"
                )
            )

            call.respond(HttpStatusCode.OK, SimpleSuccessResponse(success = true, message = "Logged out"))
        }

        // Logout from all devices
        post("/logout-all") {
            val token = call.request.cookies["session"]
                ?: call.request.header("Authorization")?.removePrefix("Bearer ")

            if (token != null) {
                val session = SessionRepository.findById(token)
                if (session != null) {
                    val count = SessionRepository.deleteAllForUser(session.userId)
                    call.response.cookies.append(
                        Cookie(
                            name = "session",
                            value = "",
                            maxAge = 0,
                            httpOnly = true,
                            path = "/"
                        )
                    )
                    call.respond(HttpStatusCode.OK, LogoutAllResponse(
                        success = true,
                        message = "Logged out from all devices",
                        sessionsInvalidated = count
                    ))
                    return@post
                }
            }

            call.respond(HttpStatusCode.Unauthorized, SimpleSuccessResponse(success = false, message = "Not authenticated"))
        }

        // Validate current session and get user info
        get("/me") {
            val token = call.request.cookies["session"]
                ?: call.request.header("Authorization")?.removePrefix("Bearer ")

            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(
                    success = false,
                    message = "No session token"
                ))
                return@get
            }

            val session = SessionRepository.validateAndRefresh(token)
            if (session == null) {
                // Clear expired cookie
                call.response.cookies.append(
                    Cookie(
                        name = "session",
                        value = "",
                        maxAge = 0,
                        httpOnly = true,
                        path = "/"
                    )
                )
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(
                    success = false,
                    message = "Session expired"
                ))
                return@get
            }

            val user = UserRepository.findById(session.userId)
            if (user == null) {
                SessionRepository.delete(token)
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(
                    success = false,
                    message = "User not found"
                ))
                return@get
            }

            // Update last active timestamp (session validation = activity)
            UserRepository.updateLastActiveAt(user.id)

            // Refresh the cookie expiry
            call.response.cookies.append(
                Cookie(
                    name = "session",
                    value = session.id,
                    maxAge = SessionConfig.SESSION_DURATION_SECONDS,
                    httpOnly = true,
                    path = "/",
                    extensions = mapOf("SameSite" to "Lax")
                )
            )

            call.respond(HttpStatusCode.OK, AuthResponse(
                success = true,
                message = "Session valid",
                user = user.toResponse(),
                sessionToken = session.id,
                expiresAt = session.expiresAt
            ))
        }
    }
}

/**
 * User routes for user management (authenticated).
 * Base path: /users
 */
fun Route.userRoutes() {
    val log = org.slf4j.LoggerFactory.getLogger("UserRoutes")

    route("/users") {
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val user = UserRepository.findById(id)
            if (user != null) {
                call.respond(user.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        /**
         * Get full stat summary for a user.
         * Returns all MajorMUD-style derived stats for character sheet display.
         */
        get("/{id}/stats") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val user = UserRepository.findById(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
            val statSummary = UserRepository.getStatSummary(user, characterClass)
            call.respond(statSummary)
        }

        /**
         * Attempt to hide in the current location.
         * Cannot be used during combat.
         */
        post("/{id}/hide") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val user = UserRepository.findById(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val result = com.ez2bg.anotherthread.game.StealthService.attemptHide(user)

            // If successful, notify other players who detect the hiding attempt
            if (result.success && user.currentLocationId != null) {
                val detections = com.ez2bg.anotherthread.game.StealthService.checkLocationDetection(
                    user,
                    user.currentLocationId!!,
                    com.ez2bg.anotherthread.game.StealthService.StealthType.HIDING
                )
                // Send detection messages to observers (via WebSocket)
                for (detection in detections) {
                    LocationEventService.sendStealthDetection(detection)
                }
            }

            call.respond(mapOf(
                "success" to result.success,
                "message" to result.message,
                "isHidden" to result.success,
                "stealthValue" to result.stealthValue
            ))
        }

        /**
         * Attempt to start sneaking.
         * Cannot be used during combat.
         */
        post("/{id}/sneak") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val user = UserRepository.findById(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val result = com.ez2bg.anotherthread.game.StealthService.attemptSneak(user)

            call.respond(mapOf(
                "success" to result.success,
                "message" to result.message,
                "isSneaking" to result.success,
                "stealthValue" to result.stealthValue
            ))
        }

        /**
         * Search the current location for hidden items.
         * Intelligence and thief-type classes have bonuses.
         */
        post("/{id}/search") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val user = UserRepository.findById(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val locationId = user.currentLocationId
            if (locationId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to "You are not at a valid location."
                ))
                return@post
            }

            val result = com.ez2bg.anotherthread.game.SearchService.attemptSearch(user, locationId)

            // Get item details for discovered items
            val discoveredItemDetails = result.discoveredItems.mapNotNull { locationItem ->
                ItemRepository.findById(locationItem.itemId)?.let { item ->
                    DiscoveredItemInfo(id = item.id, name = item.name)
                }
            }

            call.respond(SearchResponse(
                success = result.success,
                message = result.message,
                discoveredItems = discoveredItemDetails,
                totalHidden = result.totalHidden,
                hasMoreHidden = result.totalHidden > result.discoveredItems.size
            ))
        }

        /**
         * Stop hiding/sneaking and become visible.
         */
        post("/{id}/reveal") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val user = UserRepository.findById(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val message = com.ez2bg.anotherthread.game.StealthService.revealSelf(user)
            val updatedUser = UserRepository.findById(id)

            call.respond(mapOf(
                "success" to true,
                "message" to message,
                "isHidden" to (updatedUser?.isHidden ?: false),
                "isSneaking" to (updatedUser?.isSneaking ?: false)
            ))
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateUserRequest>()

            val existingUser = UserRepository.findById(id)
            if (existingUser == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            val descChanged = existingUser.desc != request.desc

            val updatedUser = existingUser.copy(
                desc = request.desc,
                itemIds = request.itemIds,
                featureIds = request.featureIds,
                lastActiveAt = System.currentTimeMillis()
            )

            if (UserRepository.update(updatedUser)) {
                // Trigger image generation if description changed
                if (descChanged && request.desc.isNotBlank()) {
                    application.launch {
                        ImageGenerationService.generateImage(
                            entityType = "user",
                            entityId = id,
                            description = request.desc,
                            entityName = existingUser.name
                        ).onSuccess { imageUrl ->
                            UserRepository.updateImageUrl(id, imageUrl)
                            log.info("Generated image for user $id: $imageUrl")
                        }.onFailure { error ->
                            log.warn("Failed to generate image for user $id: ${error.message}")
                        }
                    }
                }
                call.respond(HttpStatusCode.OK, updatedUser.toResponse())
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        put("/{id}/location") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateLocationRequest>()

            // Check encumbrance - block movement if over-encumbered
            val user = UserRepository.findById(id)
            if (user != null) {
                val encumbranceInfo = com.ez2bg.anotherthread.game.EncumbranceService.getEncumbranceInfo(user)
                if (!encumbranceInfo.canMove) {
                    return@put call.respond(HttpStatusCode.BadRequest, EncumbranceErrorResponse(
                        error = "You are over-encumbered and cannot move. Drop some items first.",
                        currentWeight = encumbranceInfo.currentWeight,
                        maxCapacity = encumbranceInfo.maxCapacity
                    ))
                }
            }

            // Capture old location before updating (for player left notification)
            val oldLocationId = user?.currentLocationId
            val oldLocation = oldLocationId?.let { LocationRepository.findById(it) }
            val userName = user?.name ?: "Unknown"

            if (UserRepository.updateCurrentLocation(id, request.locationId)) {
                // Update last active timestamp so player shows in active users list
                UserRepository.updateLastActiveAt(id)

                // Remove player from any active combat when they change location
                CombatService.removePlayerFromCombat(id)

                // Broadcast player left old location (before updating location tracking)
                if (oldLocation != null && oldLocationId != request.locationId) {
                    LocationEventService.broadcastPlayerLeft(oldLocation, id, userName)
                }

                // Update location tracking for WebSocket events
                request.locationId?.let { locationId ->
                    LocationEventService.updatePlayerLocation(id, locationId)
                }

                // Broadcast player entered new location
                val newLocation = request.locationId?.let { LocationRepository.findById(it) }
                if (newLocation != null && request.locationId != oldLocationId) {
                    LocationEventService.broadcastPlayerEntered(newLocation, id, userName)
                }

                // Record encounters with other players at this location
                request.locationId?.let { locationId ->
                    val otherUsers = UserRepository.findActiveUsersAtLocation(locationId)
                    otherUsers.filter { it.id != id }.forEach { otherUser ->
                        val currentUser = UserRepository.findById(id)
                        if (currentUser != null) {
                            PlayerEncounterRepository.recordEncounter(id, otherUser, locationId)
                        }
                    }
                }

                // Check for aggressive creatures at the new location
                val combatSession = request.locationId?.let { locationId ->
                    CombatService.checkAggressiveCreatures(id, locationId)
                }

                if (combatSession != null) {
                    // Return info about the auto-started combat
                    call.respond(HttpStatusCode.OK, UpdateLocationResponse(
                        success = true,
                        combatStarted = true,
                        combatSessionId = combatSession.id,
                        message = "Aggressive creatures attack!"
                    ))
                } else {
                    call.respond(HttpStatusCode.OK, UpdateLocationResponse(
                        success = true,
                        combatStarted = false
                    ))
                }
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Clear or set user's character class directly
        put("/{id}/class") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateClassRequest>()

            val existingUser = UserRepository.findById(id)
            if (existingUser == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            if (UserRepository.updateCharacterClass(id, request.classId)) {
                val updatedUser = UserRepository.findById(id)!!
                call.respond(HttpStatusCode.OK, updatedUser.toResponse())
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        // Equip an item
        post("/{id}/equip/{itemId}") {
            val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val itemId = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

            val item = ItemRepository.findById(itemId)
            if (item == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Item not found"))
                return@post
            }

            if (item.equipmentSlot == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Item is not equippable"))
                return@post
            }

            if (itemId !in user.itemIds) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Item not in inventory"))
                return@post
            }

            // Unequip any existing item in the same slot
            // Special case: allow 2 rings (finger slot)
            val maxItemsInSlot = if (item.equipmentSlot == "finger") 2 else 1
            val existingInSlot = user.equippedItemIds.filter { equippedId ->
                ItemRepository.findById(equippedId)?.equipmentSlot == item.equipmentSlot
            }

            // If we're at capacity for this slot, unequip the oldest one
            if (existingInSlot.size >= maxItemsInSlot) {
                UserRepository.unequipItem(userId, existingInSlot.first())
            }

            if (UserRepository.equipItem(userId, itemId)) {
                val updatedUser = UserRepository.findById(userId)!!
                call.respond(HttpStatusCode.OK, updatedUser.toResponse())
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to equip item"))
            }
        }

        // Unequip an item
        post("/{id}/unequip/{itemId}") {
            val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val itemId = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

            if (itemId !in user.equippedItemIds) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Item not equipped"))
                return@post
            }

            if (UserRepository.unequipItem(userId, itemId)) {
                val updatedUser = UserRepository.findById(userId)!!
                call.respond(HttpStatusCode.OK, updatedUser.toResponse())
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to unequip item"))
            }
        }

        // Pickup an item from a location
        post("/{id}/pickup/{itemId}") {
            val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val itemId = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            @Serializable
            data class PickupRequest(val locationId: String)
            val request = call.receive<PickupRequest>()

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

            val item = ItemRepository.findById(itemId)
            if (item == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Item not found"))
                return@post
            }

            val location = LocationRepository.findById(request.locationId)
            if (location == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Location not found"))
                return@post
            }

            // Check item is at the location
            if (itemId !in location.itemIds) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Item is not at this location"))
                return@post
            }

            // Check encumbrance (based on Constitution)
            // Each point of CON allows 2 items, base of 5 items
            val maxItems = 5 + (user.constitution / 2)
            if (user.itemIds.size >= maxItems) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Inventory full (max $maxItems items based on Constitution)"
                ))
                return@post
            }

            // Add to user inventory
            UserRepository.addItems(userId, listOf(itemId))

            // Remove from location (both legacy itemIds and new LocationItem table)
            val updatedItemIds = location.itemIds.filter { it != itemId }
            val updatedLocation = location.copy(itemIds = updatedItemIds)
            LocationRepository.update(updatedLocation)
            LocationItemRepository.removeItemByItemId(itemId, request.locationId)

            val updatedUser = UserRepository.findById(userId)!!
            call.respond(HttpStatusCode.OK, updatedUser.toResponse())
        }

        // Drop an item from inventory to current location
        post("/{id}/drop/{itemId}") {
            val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val itemId = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

            // Check user has the item
            if (itemId !in user.itemIds) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "You don't have that item"))
                return@post
            }

            // Get user's current location
            val locationId = user.currentLocationId
            if (locationId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "You must be at a location to drop items"))
                return@post
            }

            val location = LocationRepository.findById(locationId)
            if (location == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Location not found"))
                return@post
            }

            // Check if item is equipped - must unequip first
            if (itemId in user.equippedItemIds) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "You must unequip this item before dropping it"))
                return@post
            }

            // Remove ONE instance of the item from user inventory
            val updatedItemIds = user.itemIds.toMutableList()
            updatedItemIds.remove(itemId)
            UserRepository.update(user.copy(itemIds = updatedItemIds))

            // Add item to location (both legacy itemIds and new LocationItem table)
            val updatedLocationItemIds = location.itemIds + itemId
            val updatedLocation = location.copy(itemIds = updatedLocationItemIds)
            LocationRepository.update(updatedLocation)
            LocationItemRepository.addItem(locationId, itemId, userId)

            // Broadcast item added to location observers
            LocationEventService.broadcastItemAdded(location, itemId)

            val updatedUser = UserRepository.findById(userId)!!
            call.respond(HttpStatusCode.OK, updatedUser.toResponse())
        }

        // Give an item to another player at the same location
        post("/{id}/give/{receiverId}/{itemId}") {
            val giverId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val receiverId = call.parameters["receiverId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val itemId = call.parameters["itemId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            val giver = UserRepository.findById(giverId)
            if (giver == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Giver not found"))
                return@post
            }

            val receiver = UserRepository.findById(receiverId)
            if (receiver == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Receiver not found"))
                return@post
            }

            // Check giver has the item
            if (itemId !in giver.itemIds) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "You don't have that item"))
                return@post
            }

            // Check if item is equipped - must unequip first
            if (itemId in giver.equippedItemIds) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "You must unequip this item before giving it"))
                return@post
            }

            // Verify both players are at the same location
            if (giver.currentLocationId == null || giver.currentLocationId != receiver.currentLocationId) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "You must be at the same location to give items"))
                return@post
            }

            // Get item info for response
            val item = ItemRepository.findById(itemId)
            val itemName = item?.name ?: "Unknown Item"

            // Transfer item: remove from giver, add to receiver
            val giverUpdatedItems = giver.itemIds.toMutableList()
            giverUpdatedItems.remove(itemId)
            UserRepository.update(giver.copy(itemIds = giverUpdatedItems))

            val receiverUpdatedItems = receiver.itemIds + itemId
            UserRepository.update(receiver.copy(itemIds = receiverUpdatedItems))

            // Notify receiver via WebSocket
            LocationEventService.sendItemReceived(receiverId, giverId, giver.name, itemId, itemName)

            val updatedGiver = UserRepository.findById(giverId)!!
            call.respond(HttpStatusCode.OK, mapOf(
                "success" to true,
                "giver" to updatedGiver.toResponse(),
                "receiverId" to receiverId,
                "receiverName" to receiver.name,
                "itemId" to itemId,
                "itemName" to itemName
            ))
        }

        // Get identified entities for a user
        get("/{id}/identified") {
            val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val identified = IdentifiedEntityRepository.findByUser(userId)
            call.respond(mapOf(
                "items" to identified.filter { it.entityType == "item" }.map { it.entityId },
                "creatures" to identified.filter { it.entityType == "creature" }.map { it.entityId }
            ))
        }

        // Identify an entity (item or creature) for a user
        post("/{id}/identify") {
            val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            @Serializable
            data class IdentifyRequest(val entityId: String, val entityType: String)

            val request = call.receive<IdentifyRequest>()

            if (request.entityType !in listOf("item", "creature")) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid entity type"))
                return@post
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

            val isNew = IdentifiedEntityRepository.identify(userId, request.entityId, request.entityType)
            call.respond(HttpStatusCode.OK, mapOf(
                "success" to true,
                "newlyIdentified" to isNew,
                "entityId" to request.entityId,
                "entityType" to request.entityType
            ))
        }

        // Get active users at a location
        get("/at-location/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val activeUsers = UserRepository.findActiveUsersAtLocation(locationId)
            call.respond(activeUsers.map { it.toResponse() })
        }

        // Derive D&D attributes from character description using LLM
        post("/{id}/derive-attributes") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            log.info("derive-attributes: Received request for user $id")

            val request = call.receive<DeriveAttributesRequest>()
            log.info("derive-attributes: Description length=${request.description.length}, followUpAnswers=${request.followUpAnswers.size}")

            val existingUser = UserRepository.findById(id)
            if (existingUser == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val result = StatDerivationService.deriveAttributes(
                description = request.description,
                followUpAnswers = request.followUpAnswers
            )

            if (result.isSuccess) {
                val attributes = result.getOrThrow()
                log.info("derive-attributes: Success - STR=${attributes.strength} DEX=${attributes.dexterity} CON=${attributes.constitution} INT=${attributes.intelligence} WIS=${attributes.wisdom} CHA=${attributes.charisma} quality=${attributes.qualityBonus}")
                call.respond(HttpStatusCode.OK, attributes)
            } else {
                val error = result.exceptionOrNull()
                log.error("derive-attributes: Failed - ${error?.message}", error)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to derive attributes: ${error?.message}"))
            }
        }

        // Commit derived attributes to user (saves to DB, recalculates resources)
        post("/{id}/commit-attributes") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            log.info("commit-attributes: Received request for user $id")

            val request = call.receive<CommitAttributesRequest>()

            val existingUser = UserRepository.findById(id)
            if (existingUser == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val success = UserRepository.updateAttributes(
                id = id,
                strength = request.strength,
                dexterity = request.dexterity,
                constitution = request.constitution,
                intelligence = request.intelligence,
                wisdom = request.wisdom,
                charisma = request.charisma,
                qualityBonus = request.qualityBonus
            )

            if (success) {
                val updatedUser = UserRepository.findById(id)!!
                log.info("commit-attributes: Success - attributes saved for user $id")
                call.respond(HttpStatusCode.OK, updatedUser.toResponse())
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to commit attributes"))
            }
        }

        // Get custom icon mappings for a user
        get("/{id}/icon-mappings") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val mappings = CustomIconMappingRepository.findByUser(id)
            call.respond(mappings.map { CustomIconMappingResponse(abilityId = it.abilityId, iconName = it.iconName) })
        }

        // Set custom icon mapping for an ability
        put("/{id}/icon-mappings/{abilityId}") {
            val userId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val abilityId = call.parameters["abilityId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<SetIconMappingRequest>()
            CustomIconMappingRepository.setMapping(userId, abilityId, request.iconName)
            call.respond(HttpStatusCode.OK, CustomIconMappingResponse(abilityId = abilityId, iconName = request.iconName))
        }

        // Delete custom icon mapping (reset to default)
        delete("/{id}/icon-mappings/{abilityId}") {
            val userId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val abilityId = call.parameters["abilityId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            CustomIconMappingRepository.deleteMapping(userId, abilityId)
            call.respond(HttpStatusCode.OK)
        }

        // Update visible abilities for action bar (max 10, empty = show all)
        put("/{id}/visible-abilities") {
            val userId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)

            @Serializable
            data class VisibleAbilitiesRequest(val abilityIds: List<String>)

            val request = call.receive<VisibleAbilitiesRequest>()

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@put
            }

            // Limit to 10 abilities (enforced in repository too, but check here for clear error)
            if (request.abilityIds.size > 10) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Maximum 10 visible abilities allowed"))
                return@put
            }

            if (UserRepository.updateVisibleAbilities(userId, request.abilityIds)) {
                val updatedUser = UserRepository.findById(userId)!!
                call.respond(HttpStatusCode.OK, updatedUser.toResponse())
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to update visible abilities"))
            }
        }

        // Voluntary death - player gives up while downed
        post("/{id}/give-up") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            log.info("give-up: Received request for user $id")

            val user = UserRepository.findById(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "User not found"))
                return@post
            }

            // Only allow if player is downed (HP <= 0)
            if (user.currentHp > 0) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to "You can only give up when downed (HP <= 0)"
                ))
                return@post
            }

            val deathMessage = CombatService.voluntaryDeath(id)
            if (deathMessage != null) {
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "message" to "You have died and respawned at ${deathMessage.respawnLocationName}",
                    "respawnLocationId" to deathMessage.respawnLocationId,
                    "respawnLocationName" to deathMessage.respawnLocationName,
                    "itemsDropped" to deathMessage.itemsDropped,
                    "goldLost" to deathMessage.goldLost
                ))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Failed to process death"
                ))
            }
        }

        // Assign class to user (either autoassign from existing or generate new)
        post("/{id}/assign-class") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            log.info("assign-class: Received request for user $id")

            val request = call.receive<AssignClassRequest>()
            log.info("assign-class: Request - generateClass=${request.generateClass}, descriptionLength=${request.characterDescription.length}")
            log.debug("assign-class: Character description: '${request.characterDescription.take(200)}...'")

            val existingUser = UserRepository.findById(id)
            if (existingUser == null) {
                log.warn("assign-class: User $id not found")
                call.respond(HttpStatusCode.NotFound)
                return@post
            }
            log.info("assign-class: Found user '${existingUser.name}', currentClassId=${existingUser.characterClassId}")

            if (request.generateClass) {
                log.info("assign-class: Starting class GENERATION mode for user $id")
                // Mark that class generation has started (in-memory tracking)
                val genStarted = UserRepository.startClassGeneration(id, request.characterDescription)
                if (!genStarted) {
                    log.warn("assign-class: Class generation already in progress for user $id")
                    call.respond(AssignClassResponse(
                        success = false,
                        user = existingUser.toResponse(),
                        message = "Class generation already in progress. Please wait."
                    ))
                    return@post
                }
                log.info("assign-class: Class generation start marked: $genStarted")

                val userForResponse = UserRepository.findById(id)!!
                log.info("assign-class: User isGenerating=${UserRepository.isClassGenerating(id)}")

                // For class generation, run async and return immediately
                // The client will poll for the user's characterClassId
                application.launch {
                    try {
                        log.info("assign-class: [ASYNC] Starting class generation for user $id")
                        log.info("assign-class: [ASYNC] Description: '${request.characterDescription.take(100)}...'")

                        val generateResult = ClassGenerationService.generateNewClass(
                            characterDescription = request.characterDescription,
                            createdByUserId = id,
                            isPublic = false
                        )

                        if (generateResult.isFailure) {
                            val ex = generateResult.exceptionOrNull()
                            log.error("assign-class: [ASYNC] Class generation failed for user $id - ${ex?.javaClass?.simpleName}: ${ex?.message}", ex)
                            UserRepository.clearClassGeneration(id)
                            return@launch
                        }

                        val (newClass, abilities) = generateResult.getOrThrow()
                        log.info("assign-class: [ASYNC] Generated class '${newClass.name}' with ${abilities.size} abilities for user $id")

                        val (savedClass, savedAbilities) = ClassGenerationService.saveGeneratedClass(newClass, abilities)
                        log.info("assign-class: [ASYNC] Saved class to database with id=${savedClass.id}")

                        // updateCharacterClass also clears in-memory generation status
                        val updateSuccess = UserRepository.updateCharacterClass(id, savedClass.id)
                        log.info("assign-class: [ASYNC] Updated user with class assignment: $updateSuccess")
                        log.info("assign-class: [ASYNC] Class generation COMPLETE for user $id: ${savedClass.name}")
                    } catch (e: Exception) {
                        log.error("assign-class: [ASYNC] Failed to generate class for user $id - ${e::class.simpleName}: ${e.message}", e)
                        // Clear generation status on failure
                        UserRepository.clearClassGeneration(id)
                        log.info("assign-class: [ASYNC] Cleared generation status after failure")
                    }
                }

                log.info("assign-class: Returning immediately with generation-in-progress response")
                // Return immediately - class is being generated
                call.respond(AssignClassResponse(
                    success = true,
                    user = userForResponse.toResponse(),
                    assignedClass = null,
                    message = "Generating your custom class... This may take a few minutes. The page will update when complete."
                ))
            } else {
                log.info("assign-class: Starting class MATCHING mode for user $id")
                // For matching, do it synchronously (it's fast)
                try {
                    val assignedClass: CharacterClass

                    // First try LLM matching
                    log.info("assign-class: Attempting LLM class matching...")
                    val matchResult = ClassGenerationService.matchToExistingClass(request.characterDescription)
                    if (matchResult.isSuccess) {
                        val match = matchResult.getOrThrow()
                        log.info("assign-class: LLM matched to class '${match.matchedClassName}' (id=${match.matchedClassId}) with confidence ${match.confidence}")
                        log.info("assign-class: Match reasoning: ${match.reasoning}")

                        val matchedClass = CharacterClassRepository.findById(match.matchedClassId)
                        assignedClass = matchedClass ?: run {
                            log.warn("assign-class: Matched class id '${match.matchedClassId}' not found in database, using fallback")
                            // Fallback: get any public class
                            val publicClasses = CharacterClassRepository.findAll().filter { it.isPublic }
                            log.info("assign-class: Found ${publicClasses.size} public classes for fallback")
                            publicClasses.firstOrNull()
                                ?: throw Exception("No classes available for assignment")
                        }
                        log.info("assign-class: Using class '${assignedClass.name}' (id=${assignedClass.id})")
                    } else {
                        // LLM not available - just pick a class based on description keywords
                        val ex = matchResult.exceptionOrNull()
                        log.warn("assign-class: LLM matching failed - ${ex?.javaClass?.simpleName}: ${ex?.message}")
                        log.info("assign-class: Falling back to keyword-based matching")

                        val publicClasses = CharacterClassRepository.findAll().filter { it.isPublic }
                        log.info("assign-class: Found ${publicClasses.size} public classes for keyword matching")

                        val descLower = request.characterDescription.lowercase()
                        val hasSpellKeywords = descLower.contains("magic") || descLower.contains("spell") ||
                            descLower.contains("wizard") || descLower.contains("mage") ||
                            descLower.contains("sorcerer") || descLower.contains("witch")
                        log.info("assign-class: Description has spell keywords: $hasSpellKeywords")

                        assignedClass = if (hasSpellKeywords) {
                            publicClasses.find { it.isSpellcaster } ?: publicClasses.firstOrNull()
                        } else {
                            publicClasses.find { !it.isSpellcaster } ?: publicClasses.firstOrNull()
                        } ?: throw Exception("No classes available for assignment")

                        log.info("assign-class: Keyword matching selected class '${assignedClass.name}'")
                    }

                    val updateSuccess = UserRepository.updateCharacterClass(id, assignedClass.id)
                    log.info("assign-class: Updated user class assignment: $updateSuccess")

                    val updatedUser = UserRepository.findById(id)!!
                    log.info("assign-class: Class matching COMPLETE for user $id: assigned '${assignedClass.name}'")

                    call.respond(AssignClassResponse(
                        success = true,
                        user = updatedUser.toResponse(),
                        assignedClass = assignedClass,
                        message = "Class '${assignedClass.name}' assigned based on your description"
                    ))
                } catch (e: Exception) {
                    log.error("assign-class: Failed to assign class for user $id - ${e::class.simpleName}: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, AssignClassResponse(
                        success = false,
                        user = existingUser.toResponse(),
                        message = "Failed to assign class: ${e.message}"
                    ))
                }
            }
        }
    }
}
