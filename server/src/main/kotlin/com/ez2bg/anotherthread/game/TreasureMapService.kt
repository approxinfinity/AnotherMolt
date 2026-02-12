package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.combat.CombatRng
import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val json = Json { ignoreUnknownKeys = true }

/**
 * Data stored in a Feature's `data` field for treasure maps.
 * Each treasure map points to a destination location where treasure can be claimed.
 */
@Serializable
data class TreasureMapData(
    val destinationLocationId: String,
    val destinationHint: String,     // "Where the old oak casts its shadow at noon..."
    val readDifficulty: Int = 12,    // DC for d20 + INT modifier
    val rewardGold: Int = 0,
    val rewardItemIds: List<String> = emptyList()
)

/**
 * Per-user progress for a treasure map, stored in FeatureState's `state` field.
 */
@Serializable
data class TreasureMapProgress(
    val read: Boolean = false,
    val readAt: Long? = null,
    val claimed: Boolean = false,
    val claimedAt: Long? = null
)

@Serializable
data class ReadMapResult(
    val success: Boolean,
    val message: String,
    val hint: String? = null,
    val alreadyRead: Boolean = false,
    val roll: Int = 0,
    val modifier: Int = 0,
    val total: Int = 0,
    val difficulty: Int = 0
)

@Serializable
data class ClaimTreasureResult(
    val success: Boolean,
    val message: String,
    val goldAwarded: Int = 0,
    val itemsAwarded: List<String> = emptyList()
)

@Serializable
data class TreasureMapInfo(
    val itemId: String,
    val itemName: String,
    val featureId: String,
    val read: Boolean,
    val hint: String? = null,
    val claimed: Boolean,
    val destinationLocationId: String? = null
)

object TreasureMapService {
    private val log = LoggerFactory.getLogger("TreasureMapService")

    /**
     * Create a Feature from treasure map data.
     */
    fun createTreasureMapFeature(
        featureId: String,
        mapName: String,
        data: TreasureMapData
    ): Feature {
        val feature = Feature(
            id = featureId,
            name = "Treasure Map: $mapName",
            featureCategoryId = "treasure_map",
            description = "A weathered map hinting at buried treasure. DC ${data.readDifficulty} INT check to read.",
            data = json.encodeToString(data)
        )
        FeatureRepository.create(feature)
        return feature
    }

    /**
     * Get treasure map data from an item's feature IDs.
     */
    fun getTreasureMapData(item: Item): Pair<String, TreasureMapData>? {
        for (featureId in item.featureIds) {
            if (!featureId.startsWith("treasure-map-")) continue
            val feature = FeatureRepository.findById(featureId) ?: continue
            if (feature.featureCategoryId == "treasure_map") {
                return try {
                    featureId to json.decodeFromString<TreasureMapData>(feature.data)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Attempt to read a treasure map. Requires INT check.
     * Idempotent: if already read, returns the hint again.
     */
    fun readMap(user: User, itemId: String): ReadMapResult {
        // Verify user owns the item
        if (itemId !in user.itemIds) {
            return ReadMapResult(success = false, message = "You don't have that map.")
        }

        val item = ItemRepository.findById(itemId)
            ?: return ReadMapResult(success = false, message = "Item not found.")

        // Find treasure map feature
        val (featureId, mapData) = getTreasureMapData(item)
            ?: return ReadMapResult(success = false, message = "This item is not a treasure map.")

        // Get or create state for this user + feature
        val featureState = FeatureStateRepository.getOrCreate(user.id, "user", featureId)
        val progress = try {
            json.decodeFromString<TreasureMapProgress>(featureState.state)
        } catch (_: Exception) {
            TreasureMapProgress()
        }

        // If already read, return the hint again
        if (progress.read) {
            return ReadMapResult(
                success = true,
                message = "You study the map again. The markings are familiar now.",
                hint = mapData.destinationHint,
                alreadyRead = true
            )
        }

        // INT check: d20 + INT modifier >= DC
        val intModifier = StatModifierService.attributeModifier(user.intelligence)
        val roll = CombatRng.rollD20()
        val total = roll + intModifier

        if (total >= mapData.readDifficulty) {
            // Success - update state
            val updatedProgress = progress.copy(
                read = true,
                readAt = System.currentTimeMillis()
            )
            FeatureStateRepository.updateState(featureState.id, json.encodeToString(updatedProgress))

            log.info("User ${user.id} successfully read treasure map $itemId (rolled $roll + $intModifier = $total vs DC ${mapData.readDifficulty})")
            return ReadMapResult(
                success = true,
                message = "You carefully study the faded markings and decipher the map!",
                hint = mapData.destinationHint,
                roll = roll,
                modifier = intModifier,
                total = total,
                difficulty = mapData.readDifficulty
            )
        } else {
            log.info("User ${user.id} failed to read treasure map $itemId (rolled $roll + $intModifier = $total vs DC ${mapData.readDifficulty})")
            return ReadMapResult(
                success = false,
                message = "The markings are too cryptic to decipher. Perhaps with more study...",
                roll = roll,
                modifier = intModifier,
                total = total,
                difficulty = mapData.readDifficulty
            )
        }
    }

    /**
     * Claim treasure at the destination location.
     * Requires: map is read, user is at destination, not already claimed.
     */
    fun claimTreasure(user: User, itemId: String): ClaimTreasureResult {
        // Verify user owns the item
        if (itemId !in user.itemIds) {
            return ClaimTreasureResult(success = false, message = "You don't have that map.")
        }

        val item = ItemRepository.findById(itemId)
            ?: return ClaimTreasureResult(success = false, message = "Item not found.")

        val (featureId, mapData) = getTreasureMapData(item)
            ?: return ClaimTreasureResult(success = false, message = "This item is not a treasure map.")

        // Check state
        val featureState = FeatureStateRepository.getOrCreate(user.id, "user", featureId)
        val progress = try {
            json.decodeFromString<TreasureMapProgress>(featureState.state)
        } catch (_: Exception) {
            TreasureMapProgress()
        }

        if (!progress.read) {
            return ClaimTreasureResult(success = false, message = "You haven't deciphered this map yet.")
        }

        if (progress.claimed) {
            return ClaimTreasureResult(success = false, message = "You've already claimed this treasure.")
        }

        // Check user is at the destination
        if (user.currentLocationId != mapData.destinationLocationId) {
            return ClaimTreasureResult(
                success = false,
                message = "This doesn't seem to be the right place. The map hints at somewhere else..."
            )
        }

        // Claim the treasure!
        val awardedItemNames = mutableListOf<String>()

        // Award gold
        if (mapData.rewardGold > 0) {
            UserRepository.addGold(user.id, mapData.rewardGold)
        }

        // Award items
        if (mapData.rewardItemIds.isNotEmpty()) {
            UserRepository.addItems(user.id, mapData.rewardItemIds)
            for (rewardItemId in mapData.rewardItemIds) {
                val rewardItem = ItemRepository.findById(rewardItemId)
                awardedItemNames.add(rewardItem?.name ?: rewardItemId)
            }
        }

        // Mark as claimed
        val updatedProgress = progress.copy(
            claimed = true,
            claimedAt = System.currentTimeMillis()
        )
        FeatureStateRepository.updateState(featureState.id, json.encodeToString(updatedProgress))

        // Remove map from inventory
        UserRepository.removeItems(user.id, listOf(itemId))

        // Build reward summary
        val rewardParts = mutableListOf<String>()
        if (mapData.rewardGold > 0) rewardParts.add("${mapData.rewardGold} gold")
        if (awardedItemNames.isNotEmpty()) rewardParts.add(awardedItemNames.joinToString(", "))
        val rewardSummary = rewardParts.joinToString(" and ")

        log.info("User ${user.id} claimed treasure from map $itemId: $rewardSummary")
        return ClaimTreasureResult(
            success = true,
            message = "You dig at the spot marked on the map and find treasure! Gained: $rewardSummary",
            goldAwarded = mapData.rewardGold,
            itemsAwarded = awardedItemNames
        )
    }

    /**
     * Get status of all treasure maps the user owns.
     */
    fun getMapStatuses(userId: String): List<TreasureMapInfo> {
        val user = UserRepository.findById(userId) ?: return emptyList()
        val results = mutableListOf<TreasureMapInfo>()

        for (itemId in user.itemIds) {
            val item = ItemRepository.findById(itemId) ?: continue
            val (featureId, mapData) = getTreasureMapData(item) ?: continue

            val featureState = FeatureStateRepository.findByOwnerAndFeature(userId, featureId)
            val progress = featureState?.let {
                try {
                    json.decodeFromString<TreasureMapProgress>(it.state)
                } catch (_: Exception) {
                    TreasureMapProgress()
                }
            } ?: TreasureMapProgress()

            results.add(TreasureMapInfo(
                itemId = itemId,
                itemName = item.name,
                featureId = featureId,
                read = progress.read,
                hint = if (progress.read) mapData.destinationHint else null,
                claimed = progress.claimed,
                destinationLocationId = if (progress.read) mapData.destinationLocationId else null
            ))
        }

        return results
    }
}
