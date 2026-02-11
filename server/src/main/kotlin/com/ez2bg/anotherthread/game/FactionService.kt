package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * Service for faction-related game mechanics.
 *
 * Handles:
 * - Creature faction membership
 * - Player reputation with factions
 * - Faction relationships and diplomacy
 * - Standing changes from kills and quests
 */
object FactionService {
    private val log = LoggerFactory.getLogger(FactionService::class.java)

    // Standing changes for various actions
    const val STANDING_LOSS_PER_KILL = -5
    const val STANDING_GAIN_QUEST_MINOR = 5
    const val STANDING_GAIN_QUEST_MAJOR = 15
    const val STANDING_GAIN_TRIBUTE = 10
    const val ALLIED_FACTION_LOSS_RATIO = 0.5  // Allied factions lose 50% of what the target faction loses
    const val ENEMY_FACTION_GAIN_RATIO = 0.25  // Enemy factions gain 25% of what you lose with target

    /**
     * Result of checking if a creature is hostile to the player based on faction.
     */
    @Serializable
    data class HostilityResult(
        val isHostile: Boolean,
        val factionId: String?,
        val factionName: String?,
        val playerStanding: Int,
        val standingLevel: String,
        val canNegotiate: Boolean
    )

    /**
     * Get the faction ID for a creature based on its ID prefix.
     * Creature IDs are formatted like "creature-caves-of-chaos-kobold-guard"
     * We extract faction from the creature type (kobold, goblin, etc.)
     */
    fun getCreatureFactionId(creatureId: String): String? {
        // Check for common creature types in the ID
        val factionMappings = mapOf(
            "kobold" to "faction-caves-of-chaos-kobold",
            "goblin" to "faction-caves-of-chaos-goblin",
            "ogre" to "faction-caves-of-chaos-goblin",  // Ogre allies with goblins
            "orc" to "faction-caves-of-chaos-orc",
            "hobgoblin" to "faction-caves-of-chaos-hobgoblin",
            "bugbear" to "faction-caves-of-chaos-bugbear",
            "gnoll" to "faction-caves-of-chaos-gnoll",
            "minotaur" to "faction-caves-of-chaos-minotaur",
            "owlbear" to "faction-caves-of-chaos-owlbear",
            "priest" to "faction-caves-of-chaos-evil-temple",
            "acolyte" to "faction-caves-of-chaos-evil-temple",
            "zombie" to "faction-caves-of-chaos-evil-temple",
            "skeleton" to "faction-caves-of-chaos-evil-temple",
            "medusa" to "faction-caves-of-chaos-evil-temple"
        )

        val lowerCreatureId = creatureId.lowercase()
        for ((keyword, factionId) in factionMappings) {
            if (lowerCreatureId.contains(keyword)) {
                return factionId
            }
        }
        return null
    }

    /**
     * Check if a creature should be hostile to the player based on faction standing.
     */
    fun checkHostility(userId: String, creatureId: String): HostilityResult {
        val creature = CreatureRepository.findById(creatureId)
        if (creature == null || !creature.isAggressive) {
            return HostilityResult(
                isHostile = creature?.isAggressive ?: false,
                factionId = null,
                factionName = null,
                playerStanding = 0,
                standingLevel = "Neutral",
                canNegotiate = false
            )
        }

        val factionId = getCreatureFactionId(creatureId)
        if (factionId == null) {
            // No faction, use creature's base aggression
            return HostilityResult(
                isHostile = creature.isAggressive,
                factionId = null,
                factionName = null,
                playerStanding = 0,
                standingLevel = "Neutral",
                canNegotiate = false
            )
        }

        val faction = FactionRepository.findById(factionId)
        if (faction == null) {
            return HostilityResult(
                isHostile = creature.isAggressive,
                factionId = factionId,
                factionName = null,
                playerStanding = 0,
                standingLevel = "Neutral",
                canNegotiate = false
            )
        }

        val standing = PlayerFactionStandingRepository.findByUserAndFaction(userId, factionId)
        val playerStanding = standing?.standing ?: 0
        val standingLevel = StandingLevel.fromValue(playerStanding)

        // Determine hostility based on standing
        val isHostile = when (standingLevel) {
            StandingLevel.HATED, StandingLevel.HOSTILE -> true
            StandingLevel.UNFRIENDLY -> creature.isAggressive  // Normal aggression
            StandingLevel.NEUTRAL -> creature.isAggressive && faction.hostilityLevel >= 50
            StandingLevel.FRIENDLY, StandingLevel.HONORED, StandingLevel.REVERED -> false
        }

        return HostilityResult(
            isHostile = isHostile,
            factionId = factionId,
            factionName = faction.name,
            playerStanding = playerStanding,
            standingLevel = standingLevel.name,
            canNegotiate = faction.canNegotiate && standingLevel != StandingLevel.HATED
        )
    }

    /**
     * Handle a creature being killed by the player.
     * Adjusts faction standing accordingly.
     */
    fun onCreatureKilled(userId: String, creatureId: String): StandingChangeResult {
        val factionId = getCreatureFactionId(creatureId) ?: return StandingChangeResult(
            success = false,
            message = "Creature has no faction."
        )

        val faction = FactionRepository.findById(factionId) ?: return StandingChangeResult(
            success = false,
            message = "Faction not found."
        )

        // Update kill count and standing with this faction
        PlayerFactionStandingRepository.incrementKillCount(userId, factionId)
        val newStanding = PlayerFactionStandingRepository.modifyStanding(userId, factionId, STANDING_LOSS_PER_KILL)

        log.info("Player $userId killed ${creatureId}, lost standing with ${faction.name}")

        // Propagate to allied factions (they also lose standing with player)
        val alliedChanges = mutableListOf<String>()
        faction.data.allyFactionIds.forEach { allyId ->
            val allyFaction = FactionRepository.findById(allyId)
            if (allyFaction != null) {
                val allyLoss = (STANDING_LOSS_PER_KILL * ALLIED_FACTION_LOSS_RATIO).toInt()
                PlayerFactionStandingRepository.modifyStanding(userId, allyId, allyLoss)
                alliedChanges.add("${allyFaction.name} ($allyLoss)")
            }
        }

        // Enemy factions gain standing with player
        val enemyChanges = mutableListOf<String>()
        faction.data.enemyFactionIds.forEach { enemyId ->
            val enemyFaction = FactionRepository.findById(enemyId)
            if (enemyFaction != null) {
                val enemyGain = (-STANDING_LOSS_PER_KILL * ENEMY_FACTION_GAIN_RATIO).toInt()
                PlayerFactionStandingRepository.modifyStanding(userId, enemyId, enemyGain)
                enemyChanges.add("${enemyFaction.name} (+$enemyGain)")
            }
        }

        val standingLevel = StandingLevel.fromValue(newStanding.standing)
        var message = "Your standing with ${faction.name} decreased to ${newStanding.standing} (${standingLevel.name})."

        if (alliedChanges.isNotEmpty()) {
            message += " Allied factions affected: ${alliedChanges.joinToString(", ")}."
        }
        if (enemyChanges.isNotEmpty()) {
            message += " Enemy factions pleased: ${enemyChanges.joinToString(", ")}."
        }

        return StandingChangeResult(
            success = true,
            message = message,
            factionId = factionId,
            newStanding = newStanding.standing,
            standingLevel = standingLevel.name
        )
    }

    /**
     * Award standing to a player for completing a quest.
     */
    fun onQuestCompleted(userId: String, factionId: String, isMajor: Boolean = false): StandingChangeResult {
        val faction = FactionRepository.findById(factionId) ?: return StandingChangeResult(
            success = false,
            message = "Faction not found."
        )

        val standingGain = if (isMajor) STANDING_GAIN_QUEST_MAJOR else STANDING_GAIN_QUEST_MINOR
        PlayerFactionStandingRepository.incrementQuestsCompleted(userId, factionId)
        val newStanding = PlayerFactionStandingRepository.modifyStanding(userId, factionId, standingGain)

        val standingLevel = StandingLevel.fromValue(newStanding.standing)
        log.info("Player $userId completed quest for ${faction.name}, gained $standingGain standing")

        return StandingChangeResult(
            success = true,
            message = "Your standing with ${faction.name} increased to ${newStanding.standing} (${standingLevel.name})!",
            factionId = factionId,
            newStanding = newStanding.standing,
            standingLevel = standingLevel.name
        )
    }

    /**
     * Handle tribute being given to a faction.
     */
    fun onTributeGiven(userId: String, factionId: String, itemId: String): StandingChangeResult {
        val faction = FactionRepository.findById(factionId) ?: return StandingChangeResult(
            success = false,
            message = "Faction not found."
        )

        // Check if faction accepts this tribute
        if (!faction.data.tributeItems.contains(itemId)) {
            return StandingChangeResult(
                success = false,
                message = "${faction.name} is not interested in that item."
            )
        }

        val newStanding = PlayerFactionStandingRepository.modifyStanding(userId, factionId, STANDING_GAIN_TRIBUTE)
        val standingLevel = StandingLevel.fromValue(newStanding.standing)

        log.info("Player $userId gave tribute to ${faction.name}")

        return StandingChangeResult(
            success = true,
            message = "${faction.name} accepts your tribute! Standing increased to ${newStanding.standing} (${standingLevel.name}).",
            factionId = factionId,
            newStanding = newStanding.standing,
            standingLevel = standingLevel.name
        )
    }

    /**
     * Get all factions and the player's standing with each.
     */
    fun getPlayerFactionStandings(userId: String): List<FactionStandingInfo> {
        val factions = FactionRepository.findAll()
        val standings = PlayerFactionStandingRepository.findByUserId(userId)
            .associateBy { it.factionId }

        return factions.map { faction ->
            val standing = standings[faction.id]
            val standingValue = standing?.standing ?: 0
            val standingLevel = StandingLevel.fromValue(standingValue)

            FactionStandingInfo(
                factionId = faction.id,
                factionName = faction.name,
                description = faction.description,
                standing = standingValue,
                standingLevel = standingLevel.name,
                standingDescription = standingLevel.description,
                killCount = standing?.killCount ?: 0,
                questsCompleted = standing?.questsCompleted ?: 0,
                canNegotiate = faction.canNegotiate && standingLevel != StandingLevel.HATED
            )
        }
    }

    /**
     * Get faction relationship information.
     */
    fun getFactionRelationships(factionId: String): List<FactionRelationInfo> {
        val relations = FactionRelationRepository.findByFactionId(factionId)
        return relations.mapNotNull { relation ->
            val targetFaction = FactionRepository.findById(relation.targetFactionId)
            if (targetFaction != null) {
                val level = RelationshipLevel.fromValue(relation.relationshipLevel)
                FactionRelationInfo(
                    factionId = relation.targetFactionId,
                    factionName = targetFaction.name,
                    relationshipLevel = relation.relationshipLevel,
                    relationshipType = level.name
                )
            } else null
        }
    }

    @Serializable
    data class StandingChangeResult(
        val success: Boolean,
        val message: String,
        val factionId: String? = null,
        val newStanding: Int? = null,
        val standingLevel: String? = null
    )

    @Serializable
    data class FactionStandingInfo(
        val factionId: String,
        val factionName: String,
        val description: String,
        val standing: Int,
        val standingLevel: String,
        val standingDescription: String,
        val killCount: Int,
        val questsCompleted: Int,
        val canNegotiate: Boolean
    )

    @Serializable
    data class FactionRelationInfo(
        val factionId: String,
        val factionName: String,
        val relationshipLevel: Int,
        val relationshipType: String
    )

    // ========== DIPLOMACY SYSTEM ==========

    // Gold costs for diplomacy actions
    const val BRIBE_COST_BASE = 50
    const val BRIBE_COST_PER_HOSTILE_LEVEL = 25
    const val PARLEY_STANDING_REQUIREMENT = -50  // Can't parley if below this

    /**
     * Result of a diplomacy attempt.
     */
    @Serializable
    data class DiplomacyResult(
        val success: Boolean,
        val message: String,
        val combatAvoided: Boolean = false,
        val standingChange: Int = 0,
        val goldSpent: Int = 0
    )

    /**
     * Check if the player can attempt diplomacy with a creature.
     */
    fun canAttemptDiplomacy(userId: String, creatureId: String): DiplomacyResult {
        val factionId = getCreatureFactionId(creatureId) ?: return DiplomacyResult(
            success = false,
            message = "This creature has no faction to negotiate with."
        )

        val faction = FactionRepository.findById(factionId) ?: return DiplomacyResult(
            success = false,
            message = "Faction not found."
        )

        if (!faction.canNegotiate) {
            return DiplomacyResult(
                success = false,
                message = "${faction.name} does not negotiate with outsiders."
            )
        }

        val standing = PlayerFactionStandingRepository.findByUserAndFaction(userId, factionId)
        val playerStanding = standing?.standing ?: 0

        if (playerStanding < PARLEY_STANDING_REQUIREMENT) {
            return DiplomacyResult(
                success = false,
                message = "Your reputation with ${faction.name} is too low to negotiate. They attack on sight!"
            )
        }

        return DiplomacyResult(
            success = true,
            message = "You may attempt to negotiate with ${faction.name}."
        )
    }

    /**
     * Attempt to bribe a creature to avoid combat.
     * Cost depends on faction hostility and player standing.
     */
    fun attemptBribe(userId: String, creatureId: String): DiplomacyResult {
        val canResult = canAttemptDiplomacy(userId, creatureId)
        if (!canResult.success) return canResult

        val factionId = getCreatureFactionId(creatureId)!!
        val faction = FactionRepository.findById(factionId)!!
        val standing = PlayerFactionStandingRepository.findByUserAndFaction(userId, factionId)
        val playerStanding = standing?.standing ?: 0

        // Calculate bribe cost
        val hostilityFactor = (faction.hostilityLevel / 20).coerceIn(0, 5)
        val standingPenalty = if (playerStanding < 0) (-playerStanding / 10) else 0
        val bribeCost = BRIBE_COST_BASE + (hostilityFactor * BRIBE_COST_PER_HOSTILE_LEVEL) + (standingPenalty * 10)

        // Check if player has enough gold
        val user = UserRepository.findById(userId)
        if (user == null || user.gold < bribeCost) {
            return DiplomacyResult(
                success = false,
                message = "You need $bribeCost gold to bribe the ${faction.name}. You have ${user?.gold ?: 0}."
            )
        }

        // Deduct gold
        UserRepository.spendGold(userId, bribeCost)

        // Small standing improvement from successful bribe
        PlayerFactionStandingRepository.modifyStanding(userId, factionId, 1)

        log.info("Player $userId bribed ${faction.name} for $bribeCost gold")

        return DiplomacyResult(
            success = true,
            message = "You hand over $bribeCost gold. The ${faction.name} members grudgingly let you pass.",
            combatAvoided = true,
            standingChange = 1,
            goldSpent = bribeCost
        )
    }

    /**
     * Attempt to parley (talk) with a creature to avoid combat.
     * Success depends on standing and charisma (WIS for now).
     */
    fun attemptParley(userId: String, creatureId: String): DiplomacyResult {
        val canResult = canAttemptDiplomacy(userId, creatureId)
        if (!canResult.success) return canResult

        val factionId = getCreatureFactionId(creatureId)!!
        val faction = FactionRepository.findById(factionId)!!
        val standing = PlayerFactionStandingRepository.findByUserAndFaction(userId, factionId)
        val playerStanding = standing?.standing ?: 0
        val standingLevel = StandingLevel.fromValue(playerStanding)

        // If already friendly or better, auto-success
        if (standingLevel in listOf(StandingLevel.FRIENDLY, StandingLevel.HONORED, StandingLevel.REVERED)) {
            return DiplomacyResult(
                success = true,
                message = "The ${faction.name} recognize you as a friend and let you pass peacefully.",
                combatAvoided = true
            )
        }

        // For neutral/unfriendly, check player stats
        val user = UserRepository.findById(userId) ?: return DiplomacyResult(
            success = false,
            message = "User not found."
        )

        // Base chance: 30% + 5% per positive standing + 2% per WIS above 10
        val baseChance = 30
        val standingBonus = (playerStanding / 5).coerceIn(-10, 10)
        val wisBonus = ((user.wisdom - 10) * 2).coerceIn(-10, 20)
        val successChance = (baseChance + standingBonus + wisBonus).coerceIn(5, 95)

        val roll = (1..100).random()
        val success = roll <= successChance

        if (success) {
            // Small standing boost for successful parley
            PlayerFactionStandingRepository.modifyStanding(userId, factionId, 2)
            log.info("Player $userId successfully parlayed with ${faction.name} (rolled $roll vs $successChance)")
            return DiplomacyResult(
                success = true,
                message = "You convince the ${faction.name} to let you pass. They seem slightly more receptive to you.",
                combatAvoided = true,
                standingChange = 2
            )
        } else {
            log.info("Player $userId failed parley with ${faction.name} (rolled $roll vs $successChance)")
            return DiplomacyResult(
                success = false,
                message = "The ${faction.name} are not convinced by your words. They prepare to attack!"
            )
        }
    }

    // ========== ALERT SYSTEM ==========

    /**
     * Get creatures that could respond to an alert at a location.
     * Returns creature IDs from adjacent locations that belong to the same faction.
     */
    fun getAlertResponders(locationId: String, factionId: String, maxResponders: Int = 3): List<String> {
        val location = LocationRepository.findById(locationId) ?: return emptyList()
        val responders = mutableListOf<String>()

        // Check adjacent locations for same-faction creatures
        for (exit in location.exits) {
            val adjacentLocation = LocationRepository.findById(exit.locationId) ?: continue

            for (creatureId in adjacentLocation.creatureIds) {
                if (getCreatureFactionId(creatureId) == factionId) {
                    val creature = CreatureRepository.findById(creatureId)
                    // Only alert living, aggressive creatures
                    if (creature != null && creature.isAggressive) {
                        responders.add(creatureId)
                        if (responders.size >= maxResponders) {
                            return responders
                        }
                    }
                }
            }
        }

        return responders
    }

    /**
     * Process an alert cry - move reinforcements to the combat location.
     * Returns list of creatures that responded.
     */
    fun processAlertCry(locationId: String, creatureId: String): AlertResult {
        val factionId = getCreatureFactionId(creatureId) ?: return AlertResult(
            success = false,
            message = "Creature has no faction."
        )

        val faction = FactionRepository.findById(factionId) ?: return AlertResult(
            success = false,
            message = "Faction not found."
        )

        val responders = getAlertResponders(locationId, factionId)
        if (responders.isEmpty()) {
            return AlertResult(
                success = false,
                message = "No allies nearby to respond.",
                reinforcements = emptyList()
            )
        }

        // Move responders to the combat location
        val movedCreatures = mutableListOf<String>()
        for (responderId in responders) {
            val responder = CreatureRepository.findById(responderId) ?: continue

            // Find which location has this creature
            val fromLocation = LocationRepository.findAll().find { it.creatureIds.contains(responderId) }
            if (fromLocation != null && fromLocation.id != locationId) {
                // Move creature to alert location
                LocationRepository.removeCreatureFromLocation(fromLocation.id, responderId)
                LocationRepository.addCreatureToLocation(locationId, responderId)
                movedCreatures.add(responder.name)
                log.info("Alert: ${responder.name} moved from ${fromLocation.name} to respond to alert")
            }
        }

        if (movedCreatures.isEmpty()) {
            return AlertResult(
                success = false,
                message = "Allies could not respond.",
                reinforcements = emptyList()
            )
        }

        return AlertResult(
            success = true,
            message = "${faction.name} reinforcements arrive: ${movedCreatures.joinToString(", ")}!",
            reinforcements = movedCreatures
        )
    }

    @Serializable
    data class AlertResult(
        val success: Boolean,
        val message: String,
        val reinforcements: List<String> = emptyList()
    )

    // ========== TRIBAL WARS SYSTEM ==========

    /**
     * Result of a tribal war event.
     */
    @Serializable
    data class TribalWarEvent(
        val attackingFaction: String,
        val defendingFaction: String,
        val attackingFactionName: String,
        val defendingFactionName: String,
        val locationId: String,
        val locationName: String,
        val outcome: String,  // "attacker_won", "defender_won", "stalemate"
        val casualties: Map<String, Int>,  // factionId -> casualties count
        val message: String
    )

    /**
     * Check if two factions are enemies.
     */
    fun areFactionsEnemies(faction1Id: String, faction2Id: String): Boolean {
        val faction1 = FactionRepository.findById(faction1Id) ?: return false
        val faction2 = FactionRepository.findById(faction2Id) ?: return false

        return faction1.data.enemyFactionIds.contains(faction2Id) ||
               faction2.data.enemyFactionIds.contains(faction1Id)
    }

    /**
     * Get all enemy faction pairs.
     */
    fun getEnemyFactionPairs(): List<Pair<String, String>> {
        val factions = FactionRepository.findAll()
        val pairs = mutableSetOf<Pair<String, String>>()

        for (faction in factions) {
            for (enemyId in faction.data.enemyFactionIds) {
                // Use sorted IDs to avoid duplicates
                val pair = if (faction.id < enemyId) Pair(faction.id, enemyId) else Pair(enemyId, faction.id)
                pairs.add(pair)
            }
        }

        return pairs.toList()
    }

    /**
     * Process a tribal war tick - potentially trigger conflicts between enemy factions.
     * Call this periodically (e.g., every 5-10 minutes game time).
     *
     * @param warChance Probability (0-100) that a war event occurs this tick
     */
    fun processTribalWarTick(warChance: Int = 5): TribalWarEvent? {
        // Random chance check
        if ((1..100).random() > warChance) {
            return null
        }

        val enemyPairs = getEnemyFactionPairs()
        if (enemyPairs.isEmpty()) {
            return null
        }

        // Pick a random enemy pair
        val (attackerId, defenderId) = enemyPairs.random()
        val attacker = FactionRepository.findById(attackerId) ?: return null
        val defender = FactionRepository.findById(defenderId) ?: return null

        // Find a border location (in attacker's territory adjacent to defender's)
        val attackerTerritory = attacker.data.territoryLocationIds.toSet()
        val defenderTerritory = defender.data.territoryLocationIds.toSet()

        var battleLocation: Location? = null
        for (locId in attackerTerritory) {
            val loc = LocationRepository.findById(locId) ?: continue
            for (exit in loc.exits) {
                if (exit.locationId in defenderTerritory) {
                    // Found a border location
                    battleLocation = LocationRepository.findById(exit.locationId)
                    break
                }
            }
            if (battleLocation != null) break
        }

        if (battleLocation == null) {
            // No adjacent territories, pick random defender location
            val defenderLocId = defenderTerritory.randomOrNull() ?: return null
            battleLocation = LocationRepository.findById(defenderLocId) ?: return null
        }

        // Count forces
        val attackerCreatures = attackerTerritory.flatMap { locId ->
            LocationRepository.findById(locId)?.creatureIds?.filter {
                getCreatureFactionId(it) == attackerId
            } ?: emptyList()
        }
        val defenderCreatures = battleLocation.creatureIds.filter {
            getCreatureFactionId(it) == defenderId
        }

        // Simple battle resolution based on numbers
        val attackerStrength = attackerCreatures.size + (1..3).random()  // Add some randomness
        val defenderStrength = defenderCreatures.size + (1..3).random()

        val outcome: String
        val casualties = mutableMapOf<String, Int>()
        val message: String

        when {
            attackerStrength > defenderStrength * 1.5 -> {
                // Decisive attacker victory
                outcome = "attacker_won"
                val defenderLosses = (defenderCreatures.size * 0.5).toInt().coerceAtLeast(1)
                casualties[defenderId] = defenderLosses
                casualties[attackerId] = (attackerCreatures.size * 0.1).toInt()
                message = "The ${attacker.name} launched a raid on ${battleLocation.name}! " +
                          "The ${defender.name} suffered heavy losses in the assault."

                // Actually remove some defender creatures
                defenderCreatures.take(defenderLosses).forEach { creatureId ->
                    LocationRepository.removeCreatureFromLocation(battleLocation.id, creatureId)
                }
            }
            defenderStrength > attackerStrength * 1.5 -> {
                // Decisive defender victory
                outcome = "defender_won"
                casualties[attackerId] = (attackerCreatures.size * 0.3).toInt().coerceAtLeast(1)
                casualties[defenderId] = (defenderCreatures.size * 0.1).toInt()
                message = "The ${attacker.name} attempted to raid ${battleLocation.name} " +
                          "but were driven back by the ${defender.name}!"
            }
            else -> {
                // Stalemate
                outcome = "stalemate"
                casualties[attackerId] = (attackerCreatures.size * 0.15).toInt()
                casualties[defenderId] = (defenderCreatures.size * 0.15).toInt()
                message = "Skirmishes between the ${attacker.name} and ${defender.name} " +
                          "at ${battleLocation.name} ended inconclusively."
            }
        }

        log.info("Tribal war: ${attacker.name} vs ${defender.name} at ${battleLocation.name} - $outcome")

        return TribalWarEvent(
            attackingFaction = attackerId,
            defendingFaction = defenderId,
            attackingFactionName = attacker.name,
            defendingFactionName = defender.name,
            locationId = battleLocation.id,
            locationName = battleLocation.name,
            outcome = outcome,
            casualties = casualties,
            message = message
        )
    }
}
