package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.combat.CombatRng
import com.ez2bg.anotherthread.database.Creature
import com.ez2bg.anotherthread.database.FeatureStateRepository
import com.ez2bg.anotherthread.database.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

enum class ReactionType {
    HOSTILE,      // 2-5: attacks or refuses interaction
    UNCERTAIN,    // 6-8: wary, might trade/talk with persuasion
    FRIENDLY      // 9-12+: offers help, information, or trade
}

@Serializable
data class ReactionResult(
    val reaction: String,        // ReactionType name lowercase
    val roll: Int,               // 2d6 raw roll
    val charismaModifier: Int,   // CHA modifier applied
    val totalRoll: Int,          // roll + modifier
    val message: String          // Flavor text
)

object ReactionService {

    private fun charismaModifier(charisma: Int): Int = (charisma - 10) / 2

    private fun reactionType(total: Int): ReactionType = when {
        total <= 5 -> ReactionType.HOSTILE
        total <= 8 -> ReactionType.UNCERTAIN
        else -> ReactionType.FRIENDLY
    }

    private fun flavorMessage(type: ReactionType, creatureName: String): String = when (type) {
        ReactionType.HOSTILE -> listOf(
            "$creatureName eyes you with open hostility.",
            "$creatureName snarls and reaches for a weapon.",
            "$creatureName makes a threatening gesture.",
            "$creatureName clearly wants nothing to do with you."
        ).random()
        ReactionType.UNCERTAIN -> listOf(
            "$creatureName regards you warily.",
            "$creatureName seems uncertain of your intentions.",
            "$creatureName watches you with guarded interest.",
            "$creatureName neither welcomes nor threatens you."
        ).random()
        ReactionType.FRIENDLY -> listOf(
            "$creatureName greets you warmly.",
            "$creatureName seems pleased to see a traveler.",
            "$creatureName nods in a friendly manner.",
            "$creatureName offers a welcoming gesture."
        ).random()
    }

    /**
     * Roll a 2d6 + CHA modifier reaction check for a player meeting a creature.
     * Results: 2-5 HOSTILE, 6-8 UNCERTAIN, 9-12+ FRIENDLY.
     */
    fun rollReaction(user: User, creature: Creature): ReactionResult {
        val roll = CombatRng.rollD6() + CombatRng.rollD6()
        val modifier = charismaModifier(user.charisma)
        val total = roll + modifier
        val type = reactionType(total)
        val message = flavorMessage(type, creature.name)

        return ReactionResult(
            reaction = type.name.lowercase(),
            roll = roll,
            charismaModifier = modifier,
            totalRoll = total,
            message = message
        )
    }

    /**
     * Get stored reaction for a user-creature pair, or roll a new one.
     * Reactions persist so re-visiting the same creature gives consistent behavior.
     */
    fun getOrRollReaction(user: User, creature: Creature): ReactionResult {
        // Aggressive creatures are always hostile - no roll needed
        if (creature.isAggressive) {
            return ReactionResult(
                reaction = "hostile",
                roll = 2,
                charismaModifier = 0,
                totalRoll = 2,
                message = "${creature.name} is hostile!"
            )
        }

        // Ally creatures are always friendly - no roll needed
        if (creature.isAlly) {
            return ReactionResult(
                reaction = "friendly",
                roll = 12,
                charismaModifier = 0,
                totalRoll = 12,
                message = "${creature.name} is a trusted ally."
            )
        }

        // Check for existing reaction
        val key = "reaction-${creature.id}"
        val existing = FeatureStateRepository.getState(user.id, key)
        if (existing != null) {
            return try {
                json.decodeFromString<ReactionResult>(existing.value)
            } catch (_: Exception) {
                // Corrupt state, re-roll
                rollAndStore(user, creature, key)
            }
        }

        return rollAndStore(user, creature, key)
    }

    private fun rollAndStore(user: User, creature: Creature, key: String): ReactionResult {
        val result = rollReaction(user, creature)
        FeatureStateRepository.setState(user.id, key, json.encodeToString(result))
        return result
    }
}
