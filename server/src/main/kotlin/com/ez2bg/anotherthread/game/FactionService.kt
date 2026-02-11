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
}
