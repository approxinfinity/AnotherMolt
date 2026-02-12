package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.combat.CombatRng
import com.ez2bg.anotherthread.database.Feature
import com.ez2bg.anotherthread.database.FeatureRepository
import com.ez2bg.anotherthread.database.Item
import com.ez2bg.anotherthread.database.ItemRepository
import com.ez2bg.anotherthread.database.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/**
 * Data stored in a Feature's `data` field for intelligent weapons.
 * Based on OD&D Vol II intelligent sword tables.
 */
@Serializable
data class IntelligentWeaponData(
    val intelligence: Int,           // 7-12
    val ego: Int,                    // INT + 1d4
    val alignment: String,           // "lawful_good", "neutral", "chaotic_evil", etc.
    val communicationType: String,   // "empathy", "speech", "telepathy"
    val primaryPowers: List<String>, // e.g. ["detect_evil", "detect_magic"]
    val extraordinaryAbility: String? = null, // e.g. "healing", "teleportation"
    val personalityName: String? = null,      // Optional weapon name/title
    val personalityQuirk: String? = null      // Flavor text about the weapon's personality
)

/**
 * Result of an ego contest when a player tries to equip an intelligent weapon.
 */
@Serializable
data class EgoContestResult(
    val success: Boolean,
    val playerRoll: Int,
    val playerChaModifier: Int,
    val playerTotal: Int,
    val weaponRoll: Int,
    val weaponEgo: Int,
    val weaponTotal: Int,
    val message: String
)

object IntelligentWeaponService {

    // Available primary powers (OD&D table)
    val PRIMARY_POWERS = listOf(
        "detect_traps",
        "see_invisible",
        "detect_evil",
        "detect_magic",
        "detect_metal",
        "detect_gems",
        "detect_shifting_walls"
    )

    // Extraordinary abilities (INT 11-12 only)
    val EXTRAORDINARY_ABILITIES = listOf(
        "esp",
        "telekinesis",
        "teleportation",
        "healing",
        "fly"
    )

    // Alignments
    private val ALIGNMENTS = listOf(
        "lawful_good", "lawful_neutral", "lawful_evil",
        "neutral_good", "neutral", "neutral_evil",
        "chaotic_good", "chaotic_neutral", "chaotic_evil"
    )

    // Personality names for generated weapons
    private val PERSONALITY_NAMES = listOf(
        "Ashbringer", "Doomwhisper", "Frostmourne", "Starweaver",
        "Shadowthorn", "Brightedge", "Voidcaller", "Stormbreaker",
        "Dawnshard", "Nightfang", "Ironwill", "Flameheart",
        "Crystalmind", "Thundercry", "Silentwatch", "Bonechill"
    )

    // Personality quirks
    private val PERSONALITY_QUIRKS = listOf(
        "Hums softly when enemies are near.",
        "Grows warm to the touch in darkness.",
        "Whispers forgotten battle tactics.",
        "Glows faintly when its wielder is in danger.",
        "Vibrates with anticipation before combat.",
        "Feels heavier when pointed away from treasure.",
        "Pulses with energy during storms.",
        "Becomes ice-cold near undead.",
        "Emits a faint chime when lies are spoken nearby.",
        "Tugs gently toward hidden passages."
    )

    /**
     * Number of primary powers based on intelligence (OD&D table).
     */
    private fun powerCount(intelligence: Int): Int = when (intelligence) {
        7 -> 1
        8 -> 2
        in 9..12 -> 3
        else -> 1
    }

    /**
     * Communication type based on intelligence.
     * 7-9: Empathy (vague feelings)
     * 10: Speech (clear words)
     * 11-12: Telepathy (direct thought)
     */
    private fun communicationType(intelligence: Int): String = when {
        intelligence <= 9 -> "empathy"
        intelligence == 10 -> "speech"
        else -> "telepathy"
    }

    /**
     * Generate a random intelligent weapon data block.
     * Intelligence: 1d6+6 (7-12)
     * Ego: INT + 1d4
     * Powers scale with INT.
     */
    fun generateIntelligentWeaponData(): IntelligentWeaponData {
        val intelligence = CombatRng.rollD6() + 6
        val ego = intelligence + CombatRng.rollD4()
        val alignment = ALIGNMENTS.random()
        val communication = communicationType(intelligence)

        // Select random primary powers
        val numPowers = powerCount(intelligence)
        val powers = PRIMARY_POWERS.shuffled().take(numPowers)

        // Extraordinary ability only for INT 11-12
        val extraordinary = if (intelligence >= 11) {
            EXTRAORDINARY_ABILITIES.random()
        } else null

        val personalityName = PERSONALITY_NAMES.random()
        val quirk = PERSONALITY_QUIRKS.random()

        return IntelligentWeaponData(
            intelligence = intelligence,
            ego = ego,
            alignment = alignment,
            communicationType = communication,
            primaryPowers = powers,
            extraordinaryAbility = extraordinary,
            personalityName = personalityName,
            personalityQuirk = quirk
        )
    }

    /**
     * Create a Feature from intelligent weapon data and return it.
     * The feature should be added to the item's featureIds.
     */
    fun createIntelligentWeaponFeature(
        featureId: String,
        weaponName: String,
        data: IntelligentWeaponData
    ): Feature {
        val feature = Feature(
            id = featureId,
            name = "Intelligent Weapon: ${data.personalityName ?: weaponName}",
            featureCategoryId = "intelligent_weapon",
            description = buildDescription(data),
            data = json.encodeToString(data)
        )
        FeatureRepository.create(feature)
        return feature
    }

    /**
     * Build a human-readable description of the weapon's properties.
     */
    private fun buildDescription(data: IntelligentWeaponData): String {
        val parts = mutableListOf<String>()
        parts.add("INT ${data.intelligence}, Ego ${data.ego}")
        parts.add("Alignment: ${data.alignment.replace("_", " ")}")
        parts.add("Communication: ${data.communicationType}")
        parts.add("Powers: ${data.primaryPowers.joinToString(", ") { it.replace("_", " ") }}")
        data.extraordinaryAbility?.let {
            parts.add("Extraordinary: ${it.replace("_", " ")}")
        }
        return parts.joinToString(". ") + "."
    }

    /**
     * Check if an item has intelligent weapon data.
     * Returns the parsed data if found, null otherwise.
     */
    fun getIntelligentWeaponData(item: Item): IntelligentWeaponData? {
        for (featureId in item.featureIds) {
            val feature = FeatureRepository.findById(featureId) ?: continue
            if (feature.featureCategoryId == "intelligent_weapon") {
                return try {
                    json.decodeFromString<IntelligentWeaponData>(feature.data)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Run an ego contest when a player tries to equip an intelligent weapon.
     * Player rolls d20 + CHA modifier vs weapon d20 + ego.
     *
     * If alignments match, the weapon automatically submits.
     * If alignments differ, a contest occurs.
     *
     * Returns null if the item is not an intelligent weapon (no contest needed).
     */
    fun egoContest(user: User, item: Item): EgoContestResult? {
        val weaponData = getIntelligentWeaponData(item) ?: return null

        // Determine player alignment tendency from their stats
        // For now, if alignments differ, contest occurs
        val playerAlignment = getPlayerAlignmentTendency(user)
        val alignmentMatch = isAlignmentCompatible(playerAlignment, weaponData.alignment)

        if (alignmentMatch) {
            return EgoContestResult(
                success = true,
                playerRoll = 0,
                playerChaModifier = 0,
                playerTotal = 0,
                weaponRoll = 0,
                weaponEgo = weaponData.ego,
                weaponTotal = 0,
                message = weaponSubmitMessage(item.name, weaponData)
            )
        }

        // Roll the contest
        val chaModifier = StatModifierService.attributeModifier(user.charisma)
        val playerRoll = CombatRng.rollD20()
        val playerTotal = playerRoll + chaModifier

        val weaponRoll = CombatRng.rollD20()
        val weaponTotal = weaponRoll + weaponData.ego

        val success = playerTotal >= weaponTotal

        val message = if (success) {
            weaponSubmitMessage(item.name, weaponData)
        } else {
            weaponRefuseMessage(item.name, weaponData)
        }

        return EgoContestResult(
            success = success,
            playerRoll = playerRoll,
            playerChaModifier = chaModifier,
            playerTotal = playerTotal,
            weaponRoll = weaponRoll,
            weaponEgo = weaponData.ego,
            weaponTotal = weaponTotal,
            message = message
        )
    }

    /**
     * Derive a rough player alignment based on their stats.
     * Higher WIS = more lawful, higher CHA = more good.
     * This is a simple heuristic - could be expanded later.
     */
    private fun getPlayerAlignmentTendency(user: User): String {
        val lawChaos = when {
            user.wisdom >= 14 -> "lawful"
            user.wisdom <= 8 -> "chaotic"
            else -> "neutral"
        }
        val goodEvil = when {
            user.charisma >= 14 -> "good"
            user.charisma <= 8 -> "evil"
            else -> "neutral"
        }
        return if (lawChaos == "neutral" && goodEvil == "neutral") "neutral"
        else "${lawChaos}_$goodEvil"
    }

    /**
     * Check if two alignments are compatible (same axis or neutral).
     */
    private fun isAlignmentCompatible(playerAlignment: String, weaponAlignment: String): Boolean {
        if (playerAlignment == "neutral" || weaponAlignment == "neutral") return true
        if (playerAlignment == weaponAlignment) return true

        // Same law/chaos axis is compatible
        val playerParts = playerAlignment.split("_")
        val weaponParts = weaponAlignment.split("_")
        if (playerParts.firstOrNull() == weaponParts.firstOrNull()) return true

        return false
    }

    private fun weaponSubmitMessage(itemName: String, data: IntelligentWeaponData): String {
        val name = data.personalityName ?: itemName
        return when (data.communicationType) {
            "empathy" -> "A warm feeling of acceptance washes over you as you grip $name."
            "speech" -> "\"I accept you as my wielder,\" says $name."
            "telepathy" -> "You feel $name's consciousness merge with yours in agreement."
            else -> "$name accepts you as its wielder."
        }
    }

    private fun weaponRefuseMessage(itemName: String, data: IntelligentWeaponData): String {
        val name = data.personalityName ?: itemName
        return when (data.communicationType) {
            "empathy" -> "A violent jolt of rejection shoots up your arm as $name repels you!"
            "speech" -> "\"You are not worthy!\" snarls $name, burning your hand."
            "telepathy" -> "$name's alien will slams into your mind, forcing you to drop it!"
            else -> "$name refuses your grip and clatters to the ground."
        }
    }

    /**
     * Get display-friendly power name.
     */
    fun powerDisplayName(power: String): String = power.replace("_", " ")
        .replaceFirstChar { it.uppercase() }

    /**
     * Get display-friendly communication description.
     */
    fun communicationDescription(type: String): String = when (type) {
        "empathy" -> "Communicates through vague feelings and emotions"
        "speech" -> "Speaks aloud in a clear voice"
        "telepathy" -> "Projects thoughts directly into your mind"
        else -> "Unknown communication"
    }
}
