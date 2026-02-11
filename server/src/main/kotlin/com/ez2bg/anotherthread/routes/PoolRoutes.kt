package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.game.PoolService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.poolRoutes() {
    route("/pools") {
        // Get all pools
        get {
            call.respond(PoolRepository.findAll())
        }

        // Get pool by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val pool = PoolRepository.findById(id)
            if (pool != null) {
                call.respond(pool)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Get pools at a location
        get("/at-location/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")

            val pools = PoolRepository.findByLocationId(locationId)

            // Filter hidden pools unless player has discovered them
            val visiblePools = if (userId != null) {
                pools.filter { pool ->
                    if (!pool.isHidden) {
                        true // Not hidden, always visible
                    } else {
                        // Check if user has discovered this pool
                        val discoveredKey = "discovered_pool_${pool.id}"
                        val featureState = FeatureStateRepository.getState(userId, discoveredKey)
                        featureState?.value == "true"
                    }
                }
            } else {
                pools.filter { !it.isHidden }
            }

            // Return pool info with descriptions
            @Serializable
            data class PoolInfoDto(
                val id: String,
                val name: String,
                val description: String,
                val liquidColor: String,
                val liquidAppearance: String,
                val isIdentified: Boolean,
                val canInteract: Boolean
            )

            val poolInfos = visiblePools.map { pool ->
                // Check if user has identified this pool
                val identifiedKey = "identified_pool_${pool.id}"
                val identifiedState = userId?.let { FeatureStateRepository.getState(it, identifiedKey) }
                val isIdentified = identifiedState?.value == "true" || pool.identifyDifficulty == 0

                // Check if pool is depleted
                val depletedKey = "depleted_pool_${pool.id}"
                val depletedState = userId?.let { FeatureStateRepository.getState(it, depletedKey) }
                val isDepleted = depletedState?.value == "true" && pool.isOneTimeUse

                PoolInfoDto(
                    id = pool.id,
                    name = pool.name,
                    description = PoolService.getPoolDescription(pool, isIdentified),
                    liquidColor = pool.liquidColor,
                    liquidAppearance = pool.liquidAppearance,
                    isIdentified = isIdentified,
                    canInteract = !isDepleted
                )
            }

            call.respond(poolInfos)
        }

        // Attempt to identify a pool
        post("/{id}/identify") {
            val poolId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val pool = PoolRepository.findById(poolId)
            if (pool == null) {
                call.respond(HttpStatusCode.NotFound, IdentifyResultDto(false, "Pool not found"))
                return@post
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, IdentifyResultDto(false, "User not found"))
                return@post
            }

            // Check if already identified
            val identifiedKey = "identified_pool_${poolId}"
            val identifiedState = FeatureStateRepository.getState(userId, identifiedKey)
            if (identifiedState?.value == "true" || pool.identifyDifficulty == 0) {
                call.respond(IdentifyResultDto(
                    success = true,
                    message = "You already know what this pool does.",
                    description = PoolService.getPoolDescription(pool, true)
                ))
                return@post
            }

            // Attempt identification
            val success = PoolService.attemptIdentify(user, pool)

            if (success) {
                FeatureStateRepository.setState(userId, identifiedKey, "true")
                call.respond(IdentifyResultDto(
                    success = true,
                    message = "You discern the pool's properties!",
                    description = PoolService.getPoolDescription(pool, true)
                ))
            } else {
                call.respond(IdentifyResultDto(
                    success = false,
                    message = "You cannot determine the pool's properties."
                ))
            }
        }

        // Interact with a pool (drink, enter, touch)
        post("/{id}/interact") {
            val poolId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            @Serializable
            data class InteractRequest(val interactionType: String = "drink") // "drink", "enter", "touch"

            val request = call.receive<InteractRequest>()

            val pool = PoolRepository.findById(poolId)
            if (pool == null) {
                call.respond(HttpStatusCode.NotFound, InteractResultDto(false, "Pool not found"))
                return@post
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, InteractResultDto(false, "User not found"))
                return@post
            }

            // Check if pool is depleted (one-time use)
            if (pool.isOneTimeUse) {
                val depletedKey = "depleted_pool_${poolId}"
                val depletedState = FeatureStateRepository.getState(userId, depletedKey)
                if (depletedState?.value == "true") {
                    call.respond(InteractResultDto(
                        success = false,
                        message = "This pool has been depleted."
                    ))
                    return@post
                }
            }

            // Check daily usage limit
            if (pool.usesPerDay > 0) {
                val usageKey = "pool_uses_${poolId}_${userId}_today"
                val usageState = FeatureStateRepository.getState(userId, usageKey)
                val usesToday = usageState?.value?.toIntOrNull() ?: 0
                if (usesToday >= pool.usesPerDay) {
                    call.respond(InteractResultDto(
                        success = false,
                        message = "You have already used this pool ${pool.usesPerDay} time(s) today."
                    ))
                    return@post
                }
            }

            // Interact with the pool
            val result = PoolService.interact(user, pool, request.interactionType)

            // Apply effects to user
            var updatedUser = user

            // HP changes
            if (result.hpChange != 0) {
                val newHp = (user.currentHp + result.hpChange).coerceIn(0, user.maxHp)
                updatedUser = updatedUser.copy(currentHp = newHp)
            }

            // Gold changes
            if (result.goldChange != 0) {
                updatedUser = updatedUser.copy(gold = updatedUser.gold + result.goldChange)
            }

            // Items received
            if (result.itemsReceived.isNotEmpty()) {
                val currentItems = updatedUser.itemIds.toMutableList()
                currentItems.addAll(result.itemsReceived)
                updatedUser = updatedUser.copy(itemIds = currentItems)
            }

            // Save user updates
            if (updatedUser != user) {
                UserRepository.update(updatedUser)
            }

            // Handle teleport
            if (result.teleportLocationId != null) {
                val teleportedUser = updatedUser.copy(currentLocationId = result.teleportLocationId)
                UserRepository.update(teleportedUser)
            }

            // Mark pool as depleted if one-time use
            if (result.poolDepleted || (pool.isOneTimeUse && result.success)) {
                val depletedKey = "depleted_pool_${poolId}"
                FeatureStateRepository.setState(userId, depletedKey, "true")
            }

            // Update daily usage count
            if (pool.usesPerDay > 0 && result.success) {
                val usageKey = "pool_uses_${poolId}_${userId}_today"
                val usageState = FeatureStateRepository.getState(userId, usageKey)
                val usesToday = (usageState?.value?.toIntOrNull() ?: 0) + 1
                FeatureStateRepository.setState(userId, usageKey, usesToday.toString())
            }

            call.respond(InteractResultDto(
                success = result.success,
                message = result.message,
                effectApplied = result.effectApplied,
                hpChange = result.hpChange,
                goldChange = result.goldChange,
                itemsReceived = result.itemsReceived.mapNotNull { itemId ->
                    ItemRepository.findById(itemId)?.name
                },
                conditionApplied = result.conditionApplied,
                conditionDuration = result.conditionDuration,
                teleportLocationId = result.teleportLocationId,
                newHp = if (result.hpChange != 0) updatedUser.currentHp else null,
                newGold = if (result.goldChange != 0) updatedUser.gold else null
            ))
        }
    }
}

@Serializable
private data class IdentifyResultDto(
    val success: Boolean,
    val message: String,
    val description: String? = null
)

@Serializable
private data class InteractResultDto(
    val success: Boolean,
    val message: String,
    val effectApplied: String? = null,
    val hpChange: Int = 0,
    val goldChange: Int = 0,
    val itemsReceived: List<String> = emptyList(),
    val conditionApplied: String? = null,
    val conditionDuration: Int = 0,
    val teleportLocationId: String? = null,
    val newHp: Int? = null,
    val newGold: Int? = null
)
