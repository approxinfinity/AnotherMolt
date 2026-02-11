package com.ez2bg.anotherthread.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.ez2bg.anotherthread.api.AbilityDto

/**
 * Maps abilities to Material Design icons based on their effect types and ability types.
 */
object AbilityIconMapper {

    /**
     * Get the appropriate icon for an ability, with optional custom icon override.
     */
    fun getIcon(ability: AbilityDto, customIconName: String? = null): ImageVector {
        // Check custom mapping first
        customIconName?.let { iconNameToVector(it)?.let { icon -> return icon } }

        // Check if imageUrl contains an icon reference (format: "icon:IconName")
        ability.imageUrl?.let { url ->
            if (url.startsWith("icon:")) {
                val iconName = url.removePrefix("icon:")
                iconNameToVector(iconName)?.let { return it }
            }
        }
        // Fallback: infer icon from ability effects and type
        return inferIconFromAbility(ability)
    }

    /**
     * Get all available icons as name-vector pairs for the icon picker.
     */
    fun getAllAvailableIcons(): List<Pair<String, ImageVector>> = listOf(
        "bolt" to Icons.Filled.Bolt,
        "sword" to Icons.Filled.Gavel,
        "target" to Icons.Filled.GpsFixed,
        "fire" to Icons.Filled.Whatshot,
        "shield" to Icons.Filled.Shield,
        "security" to Icons.Filled.Security,
        "heart" to Icons.Filled.Favorite,
        "healing" to Icons.Filled.Healing,
        "first_aid" to Icons.Filled.LocalHospital,
        "spa" to Icons.Filled.Spa,
        "buff" to Icons.Filled.TrendingUp,
        "debuff" to Icons.Filled.TrendingDown,
        "star" to Icons.Filled.Star,
        "lock" to Icons.Filled.Lock,
        "anchor" to Icons.Filled.Anchor,
        "pause" to Icons.Filled.Pause,
        "run" to Icons.Filled.DirectionsRun,
        "walk" to Icons.Filled.DirectionsWalk,
        "flash" to Icons.Filled.FlashOn,
        "reflect" to Icons.Filled.Replay,
        "bloodtype" to Icons.Filled.Bloodtype,
        "science" to Icons.Filled.Science,
        "blur" to Icons.Filled.BlurCircular,
        "groups" to Icons.Filled.Groups,
        "build" to Icons.Filled.Build,
        "auto" to Icons.Filled.AutoAwesome,
        "visibility_off" to Icons.Filled.VisibilityOff,
        "screen_rotation" to Icons.Filled.ScreenRotation,
        "psychology" to Icons.Filled.Psychology,
        "dangerous" to Icons.Filled.Dangerous,
        "staff" to Icons.Filled.FlashOn,
        "drag" to Icons.Filled.PanTool,
        "open_rift" to Icons.Filled.Adjust,
        "seal_rift" to Icons.Filled.DoNotDisturb,
    )

    /**
     * Get icon by explicit name mapping.
     */
    fun iconNameToVector(name: String): ImageVector? = when (name.lowercase()) {
        // Damage/Attack
        "bolt", "lightning" -> Icons.Filled.Bolt
        "sword", "attack" -> Icons.Filled.Gavel
        "target", "crosshairs" -> Icons.Filled.GpsFixed
        "fire", "flame" -> Icons.Filled.Whatshot

        // Defense
        "shield" -> Icons.Filled.Shield
        "security" -> Icons.Filled.Security

        // Healing
        "heart", "heal" -> Icons.Filled.Favorite
        "healing" -> Icons.Filled.Healing
        "first_aid", "medical", "hospital" -> Icons.Filled.LocalHospital
        "spa", "nature" -> Icons.Filled.Spa

        // Buffs/Debuffs
        "buff", "up", "trending_up" -> Icons.Filled.TrendingUp
        "debuff", "down", "trending_down" -> Icons.Filled.TrendingDown
        "star" -> Icons.Filled.Star

        // Control
        "lock", "stun" -> Icons.Filled.Lock
        "anchor", "root" -> Icons.Filled.Anchor
        "pause" -> Icons.Filled.Pause

        // Movement
        "run", "dash" -> Icons.Filled.DirectionsRun
        "directions_walk", "walk", "phasewalk" -> Icons.Filled.DirectionsWalk
        "flash", "teleport", "staff", "stave" -> Icons.Filled.FlashOn

        // Special Effects
        "reflect", "mirror", "replay" -> Icons.Filled.Replay
        "bloodtype", "lifesteal", "vampire" -> Icons.Filled.Bloodtype
        "science", "poison", "flask" -> Icons.Filled.Science
        "blur", "area", "aoe" -> Icons.Filled.BlurCircular

        // Summon/Utility
        "groups", "summon" -> Icons.Filled.Groups
        "build", "utility" -> Icons.Filled.Build
        "auto", "passive" -> Icons.Filled.AutoAwesome

        // Conditions
        "visibility_off", "blind" -> Icons.Filled.VisibilityOff
        "screen_rotation", "disorient", "rotate" -> Icons.Filled.ScreenRotation
        "psychology", "mind", "psychic" -> Icons.Filled.Psychology

        // Drag/Rift
        "drag", "pan_tool", "hand" -> Icons.Filled.PanTool
        "open_rift", "rift", "portal" -> Icons.Filled.Adjust
        "seal_rift", "close_rift", "seal" -> Icons.Filled.DoNotDisturb

        else -> null
    }

    /**
     * Infer the best icon based on ability effects JSON and type.
     */
    private fun inferIconFromAbility(ability: AbilityDto): ImageVector {
        val effects = ability.effects.lowercase()
        val name = ability.name.lowercase()

        // Check effects JSON for specific types
        return when {
            // Shield/Protection effects
            effects.contains("\"type\":\"shield\"") || name.contains("shield") ||
            name.contains("bulwark") || name.contains("barkskin") || name.contains("stoneskin") ->
                Icons.Filled.Shield

            // Reflect effects
            effects.contains("\"type\":\"reflect\"") || name.contains("reflect") ||
            name.contains("mirror") || name.contains("thorns") || name.contains("spiked") ||
            name.contains("caustic") ->
                Icons.Filled.Replay

            // Lifesteal effects
            effects.contains("\"type\":\"lifesteal\"") || name.contains("lifesteal") ||
            name.contains("vampiric") || name.contains("siphon") || name.contains("drain") ||
            name.contains("blood frenzy") || name.contains("primal savagery") ->
                Icons.Filled.Bloodtype

            // Healing effects
            effects.contains("\"type\":\"heal\"") || effects.contains("\"type\":\"hot\"") ||
            name.contains("heal") || name.contains("cure") || name.contains("restore") ||
            name.contains("life bloom") || name.contains("draught") ->
                Icons.Filled.Favorite

            // Stun/Control effects
            effects.contains("\"condition\":\"stunned\"") || effects.contains("\"condition\":\"stun\"") ||
            name.contains("stun") || name.contains("cheap shot") ->
                Icons.Filled.Lock

            // Root/Immobilize effects
            effects.contains("\"condition\":\"restrained\"") || effects.contains("\"condition\":\"root\"") ||
            name.contains("root") || name.contains("ensnare") || name.contains("tanglefoot") ->
                Icons.Filled.Anchor

            // Blind effect
            effects.contains("\"condition\":\"blind\"") || name.contains("blind") ||
            name.contains("flash powder") ->
                Icons.Filled.VisibilityOff

            // Disorient effect
            effects.contains("\"condition\":\"disorient\"") || name.contains("disorient") ||
            name.contains("vertigo") ->
                Icons.Filled.ScreenRotation

            // Charm/Mind effects
            effects.contains("\"condition\":\"charmed\"") || name.contains("charm") ||
            name.contains("hypnotic") || name.contains("mind") ->
                Icons.Filled.Psychology

            // Fear effects
            effects.contains("\"condition\":\"fear\"") || name.contains("fear") ||
            name.contains("battle cry") || name.contains("terrify") ->
                Icons.Filled.Dangerous

            // DoT/Poison effects
            effects.contains("\"type\":\"dot\"") || name.contains("poison") ||
            name.contains("acid") || name.contains("alchemist fire") ->
                Icons.Filled.Science

            // Buff effects
            effects.contains("\"type\":\"buff\"") || name.contains("inspire") ||
            name.contains("ballad") || name.contains("countercharm") ->
                Icons.Filled.TrendingUp

            // Debuff effects
            effects.contains("\"type\":\"debuff\"") || name.contains("vicious mockery") ||
            name.contains("cutting words") || name.contains("exploit") ->
                Icons.Filled.TrendingDown

            // Movement/Teleport
            effects.contains("\"movementtype\":\"teleport\"") || name.contains("shadowstep") ||
            name.contains("teleport") || name.contains("blink") ->
                Icons.Filled.FlashOn

            // Area effects
            ability.targetType == "area" || ability.targetType == "all_enemies" ||
            name.contains("volley") || name.contains("barrage") || name.contains("thunderwave") ||
            name.contains("spike growth") ->
                Icons.Filled.BlurCircular

            // Summon effects
            effects.contains("\"type\":\"summon\"") || name.contains("summon") ||
            name.contains("conjure") ->
                Icons.Filled.Groups

            // Damage spells (generic)
            ability.baseDamage > 0 && ability.abilityType == "spell" ->
                Icons.Filled.Bolt

            // Combat abilities with damage
            ability.baseDamage > 0 && ability.abilityType == "combat" ->
                Icons.Filled.Gavel

            // Fallback by ability type
            else -> when (ability.abilityType) {
                "spell" -> Icons.Filled.AutoAwesome
                "combat" -> Icons.Filled.Bolt
                "utility" -> Icons.Filled.Build
                "passive" -> Icons.Filled.Star
                "navigation" -> Icons.Filled.DirectionsWalk
                else -> Icons.Filled.HelpOutline
            }
        }
    }

    /**
     * Get color for ability type.
     */
    fun getAbilityTypeColor(abilityType: String): androidx.compose.ui.graphics.Color = when (abilityType) {
        "spell" -> androidx.compose.ui.graphics.Color(0xFF7C4DFF)      // Purple for spells
        "combat" -> androidx.compose.ui.graphics.Color(0xFFD32F2F)     // Red for combat
        "utility" -> androidx.compose.ui.graphics.Color(0xFF1976D2)    // Blue for utility
        "passive" -> androidx.compose.ui.graphics.Color(0xFF388E3C)    // Green for passive
        "item" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)       // Bright green for item abilities
        "navigation" -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Purple for navigation (matches direction ring)
        else -> androidx.compose.ui.graphics.Color(0xFF616161)         // Gray default
    }

    /**
     * Generate a 4-letter abbreviation from an ability name.
     *
     * Rules:
     * 1. If name is 4 chars or less, use it as-is (uppercase)
     * 2. If name has 2+ words, take first 2 letters of each word (up to 4 chars)
     * 3. For single word, take first 4 consonants or first 4 letters
     *
     * Examples:
     * - "Fireball" -> "FRBL"
     * - "Power Strike" -> "PWST"
     * - "Heal" -> "HEAL"
     * - "Lightning Bolt" -> "LGBT"
     * - "Quick Slash" -> "QKSL"
     */
    fun getAbbreviation(abilityName: String): String {
        val name = abilityName.trim().uppercase()

        // If already 4 chars or less, use as-is
        if (name.length <= 4) return name

        // Split into words
        val words = name.split(" ", "-", "_").filter { it.isNotEmpty() }

        return when {
            // Multiple words: take first 2 letters of first 2 words
            words.size >= 2 -> {
                val first = words[0].take(2)
                val second = words[1].take(2)
                (first + second).take(4)
            }
            // Single word: take consonants first, then fill with vowels
            else -> {
                val word = words.first()
                val consonants = word.filter { it !in "AEIOU" }
                if (consonants.length >= 4) {
                    consonants.take(4)
                } else {
                    // Take first char + consonants + remaining chars
                    val firstChar = word.first()
                    val remaining = word.drop(1).filter { it !in "AEIOU" }.take(3)
                    (firstChar + remaining).padEnd(4, word.lastOrNull() ?: 'X').take(4)
                }
            }
        }
    }
}
