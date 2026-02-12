package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import com.ez2bg.anotherthread.game.GemValueService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.chestRoutes() {
    route("/chests") {
        // Get all chests
        get {
            call.respond(ChestRepository.findAll())
        }

        // Get chest by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val chest = ChestRepository.findById(id)
            if (chest != null) {
                call.respond(chest)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Get chests at a location (filtered by guardian defeated)
        get("/at-location/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")

            val chests = ChestRepository.findByLocationId(locationId)

            // Filter to only show chests whose guardian has been defeated (if any)
            val visibleChests = if (userId != null) {
                chests.filter { chest ->
                    if (chest.guardianCreatureId == null) {
                        true // No guardian, always visible
                    } else {
                        // Check if user has defeated the guardian via FeatureState
                        val defeatedKey = "defeated_${chest.guardianCreatureId}"
                        val featureState = FeatureStateRepository.getState(userId, defeatedKey)
                        featureState?.value == "true"
                    }
                }
            } else {
                chests.filter { it.guardianCreatureId == null }
            }

            call.respond(visibleChests)
        }

        // Open a chest (bash or pick_lock)
        post("/{id}/open") {
            val chestId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            @Serializable
            data class OpenChestRequest(val method: String) // "bash" or "pick_lock"

            val request = call.receive<OpenChestRequest>()

            val chest = ChestRepository.findById(chestId)
            if (chest == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Chest not found"))
                return@post
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

            // Check if guardian is defeated
            if (chest.guardianCreatureId != null) {
                val defeatedKey = "defeated_${chest.guardianCreatureId}"
                val featureState = FeatureStateRepository.getState(userId, defeatedKey)
                if (featureState?.value != "true") {
                    call.respond(HttpStatusCode.Forbidden, mapOf(
                        "error" to "The guardian still protects this chest"
                    ))
                    return@post
                }
            }

            // Check if already opened by this user
            val openedKey = "opened_chest_${chestId}"
            val openedState = FeatureStateRepository.getState(userId, openedKey)
            if (openedState?.value == "true") {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Chest already opened"))
                return@post
            }

            // Check class for method validity
            val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
            val archetype = characterClass?.name?.lowercase() ?: ""

            val canBash = archetype in listOf("warrior", "berserker", "paladin", "fighter", "knight")
            val canPickLock = archetype in listOf("rogue", "assassin", "bard", "thief", "scoundrel")

            when (request.method) {
                "bash" -> if (!canBash) {
                    call.respond(HttpStatusCode.Forbidden, mapOf(
                        "error" to "Only martial classes can bash chests"
                    ))
                    return@post
                }
                "pick_lock" -> if (!canPickLock) {
                    call.respond(HttpStatusCode.Forbidden, mapOf(
                        "error" to "Only scoundrel classes can pick locks"
                    ))
                    return@post
                }
                else -> {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid method"))
                    return@post
                }
            }

            // Roll for success
            val difficulty = when (request.method) {
                "bash" -> chest.bashDifficulty
                "pick_lock" -> chest.lockDifficulty
                else -> 5
            }
            val successChance = 1.0f / difficulty
            val success = kotlin.random.Random.nextFloat() < successChance

            if (!success) {
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to false,
                    "message" to "You failed to open the chest!"
                ))
                return@post
            }

            // Success! Mark chest as opened and distribute loot
            FeatureStateRepository.setState(userId, openedKey, "true")

            var goldEarned = chest.goldAmount
            val itemsEarned = mutableListOf<Item>()

            // Roll loot table
            chest.lootTableId?.let { lootTableId ->
                val lootTable = LootTableRepository.findById(lootTableId)
                lootTable?.entries?.forEach { entry ->
                    if (kotlin.random.Random.nextFloat() < entry.chance) {
                        ItemRepository.findById(entry.itemId)?.let { item ->
                            // Non-stackable items only drop once; stackable respect qty range
                            val qty = if (item.isStackable) {
                                if (entry.maxQty > entry.minQty) {
                                    kotlin.random.Random.nextInt(entry.minQty, entry.maxQty + 1)
                                } else entry.minQty
                            } else 1

                            repeat(qty) {
                                // Randomize gem/jewelry values (OD&D tables)
                                val earnedItem = when {
                                    GemValueService.isGemItem(item) -> GemValueService.createRandomGem()
                                    GemValueService.isJewelryItem(item) -> GemValueService.createRandomJewelry()
                                    else -> item
                                }
                                itemsEarned.add(earnedItem)
                            }
                        }
                    }
                }
            }

            // Award loot to user
            if (goldEarned > 0) {
                UserRepository.addGold(userId, goldEarned)
            }
            if (itemsEarned.isNotEmpty()) {
                UserRepository.addItems(userId, itemsEarned.map { it.id })
            }

            call.respond(HttpStatusCode.OK, mapOf(
                "success" to true,
                "message" to "You opened the chest!",
                "goldEarned" to goldEarned,
                "itemsEarned" to itemsEarned.map { it.name }
            ))
        }

        // Get available actions for a chest (based on user class)
        get("/{id}/actions") {
            val chestId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val chest = ChestRepository.findById(chestId)
            if (chest == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val user = UserRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            // Check if already opened
            val openedKey = "opened_chest_${chestId}"
            val openedState = FeatureStateRepository.getState(userId, openedKey)
            if (openedState?.value == "true") {
                call.respond(mapOf("actions" to emptyList<String>(), "opened" to true))
                return@get
            }

            val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
            val archetype = characterClass?.name?.lowercase() ?: ""

            val actions = mutableListOf<String>()
            if (archetype in listOf("warrior", "berserker", "paladin", "fighter", "knight")) {
                actions.add("bash")
            }
            if (archetype in listOf("rogue", "assassin", "bard", "thief", "scoundrel")) {
                actions.add("pick_lock")
            }

            call.respond(mapOf("actions" to actions, "opened" to false))
        }
    }
}
