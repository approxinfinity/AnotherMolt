package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.AbilityDto
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Arranges ability icons in a circular ring around a center point.
 *
 * @param abilities List of abilities to display
 * @param cooldowns Map of ability ID to remaining cooldown rounds
 * @param queuedAbilityId ID of the currently queued ability (if any)
 * @param onAbilityClick Callback when an ability is clicked
 * @param ringRadius Distance from center to icon centers
 * @param iconSize Size of each ability icon
 * @param maxIcons Maximum number of icons to display in the ring
 */
@Composable
fun AbilityIconRing(
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int> = emptyMap(),
    queuedAbilityId: String? = null,
    onAbilityClick: (AbilityDto) -> Unit,
    modifier: Modifier = Modifier,
    ringRadius: Dp = 80.dp,
    iconSize: Dp = 36.dp,
    maxIcons: Int = 8
) {
    // Filter to combat-usable abilities (exclude passive)
    val usableAbilities = abilities.filter { it.abilityType != "passive" }
    val displayAbilities = usableAbilities.take(maxIcons)
    val totalIcons = displayAbilities.size

    if (totalIcons == 0) return

    Box(
        modifier = modifier.size(ringRadius * 2 + iconSize + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        displayAbilities.forEachIndexed { index, ability ->
            // Calculate angle (start at top, go clockwise)
            val angleDegrees = (360f / totalIcons) * index - 90f
            val angleRadians = angleDegrees * PI / 180.0

            // Calculate offset from center
            val offsetX = (ringRadius.value * cos(angleRadians)).toFloat().dp
            val offsetY = (ringRadius.value * sin(angleRadians)).toFloat().dp

            AbilityIconButton(
                ability = ability,
                cooldownRounds = cooldowns[ability.id] ?: 0,
                isQueued = ability.id == queuedAbilityId,
                onClick = { onAbilityClick(ability) },
                size = iconSize,
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            )
        }
    }
}

/**
 * Variant that displays abilities in two rings for larger ability sets.
 */
@Composable
fun AbilityIconDoubleRing(
    abilities: List<AbilityDto>,
    cooldowns: Map<String, Int> = emptyMap(),
    queuedAbilityId: String? = null,
    onAbilityClick: (AbilityDto) -> Unit,
    modifier: Modifier = Modifier,
    innerRadius: Dp = 70.dp,
    outerRadius: Dp = 110.dp,
    iconSize: Dp = 32.dp,
    maxInnerIcons: Int = 6,
    maxOuterIcons: Int = 10
) {
    val usableAbilities = abilities.filter { it.abilityType != "passive" }

    // Split abilities between inner and outer ring
    val innerAbilities = usableAbilities.take(maxInnerIcons)
    val outerAbilities = usableAbilities.drop(maxInnerIcons).take(maxOuterIcons)

    Box(
        modifier = modifier.size(outerRadius * 2 + iconSize + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner ring
        if (innerAbilities.isNotEmpty()) {
            val innerCount = innerAbilities.size
            innerAbilities.forEachIndexed { index, ability ->
                val angleDegrees = (360f / innerCount) * index - 90f
                val angleRadians = angleDegrees * PI / 180.0
                val offsetX = (innerRadius.value * cos(angleRadians)).toFloat().dp
                val offsetY = (innerRadius.value * sin(angleRadians)).toFloat().dp

                AbilityIconButton(
                    ability = ability,
                    cooldownRounds = cooldowns[ability.id] ?: 0,
                    isQueued = ability.id == queuedAbilityId,
                    onClick = { onAbilityClick(ability) },
                    size = iconSize,
                    modifier = Modifier.offset(x = offsetX, y = offsetY)
                )
            }
        }

        // Outer ring
        if (outerAbilities.isNotEmpty()) {
            val outerCount = outerAbilities.size
            outerAbilities.forEachIndexed { index, ability ->
                // Offset outer ring slightly so icons don't align
                val angleDegrees = (360f / outerCount) * index - 90f + (180f / outerCount)
                val angleRadians = angleDegrees * PI / 180.0
                val offsetX = (outerRadius.value * cos(angleRadians)).toFloat().dp
                val offsetY = (outerRadius.value * sin(angleRadians)).toFloat().dp

                AbilityIconButton(
                    ability = ability,
                    cooldownRounds = cooldowns[ability.id] ?: 0,
                    isQueued = ability.id == queuedAbilityId,
                    onClick = { onAbilityClick(ability) },
                    size = iconSize - 4.dp,  // Slightly smaller for outer ring
                    modifier = Modifier.offset(x = offsetX, y = offsetY)
                )
            }
        }
    }
}
