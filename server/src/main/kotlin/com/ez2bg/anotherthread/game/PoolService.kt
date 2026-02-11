package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.random.Random

/**
 * Service for handling magical pool interactions.
 */
object PoolService {
    private val log = LoggerFactory.getLogger(PoolService::class.java)

    /**
     * Result of interacting with a pool.
     */
    @Serializable
    data class PoolInteractionResult(
        val success: Boolean,
        val message: String,
        val effectApplied: String? = null,

        // Stat changes
        val hpChange: Int = 0,
        val manaChange: Int = 0,
        val goldChange: Int = 0,

        // Items received
        val itemsReceived: List<String> = emptyList(),

        // Conditions applied
        val conditionApplied: String? = null,
        val conditionDuration: Int = 0,

        // Stat modifiers (temporary buffs/debuffs)
        val statModifier: String? = null,
        val modifierAmount: Int = 0,
        val modifierDuration: Int = 0,

        // Teleport
        val teleportLocationId: String? = null,

        // Pool state changes
        val poolDepleted: Boolean = false
    )

    /**
     * Player interacts with a pool (drink, enter, touch).
     */
    fun interact(user: User, pool: Pool, interactionType: String = "drink"): PoolInteractionResult {
        log.info("User ${user.name} interacting with pool ${pool.name} via $interactionType")

        return when (pool.effectType) {
            PoolEffectType.HEALING -> handleHealing(user, pool)
            PoolEffectType.DAMAGE -> handleDamage(user, pool)
            PoolEffectType.BUFF -> handleBuff(user, pool)
            PoolEffectType.DEBUFF -> handleDebuff(user, pool)
            PoolEffectType.POISON -> handlePoison(user, pool)
            PoolEffectType.CHARM -> handleCharm(user, pool)
            PoolEffectType.SLEEP -> handleSleep(user, pool)
            PoolEffectType.STRENGTH -> handleStrength(user, pool)
            PoolEffectType.SPEED -> handleSpeed(user, pool)
            PoolEffectType.TELEPORT -> handleTeleport(user, pool)
            PoolEffectType.TREASURE -> handleTreasure(user, pool)
            PoolEffectType.TRAP -> handleTrap(user, pool)
            PoolEffectType.WINE -> handleWine(user, pool)
            PoolEffectType.TRANSFORM -> handleTransform(user, pool)
            PoolEffectType.EMPTY -> handleEmpty(pool)
            PoolEffectType.STRANGE -> handleStrange(user, pool)
        }
    }

    private fun handleHealing(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val healAmount = data.healAmount ?: rollDice(data.healDice ?: "1d6")
        val actualHeal = minOf(healAmount, user.maxHp - user.currentHp)

        val messages = mutableListOf<String>()
        messages.add("The ${pool.liquidColor} liquid soothes your wounds, healing $actualHeal HP.")

        if (data.curesDisease) {
            messages.add("You feel any disease leaving your body.")
        }
        if (data.curesPoison) {
            messages.add("Any poison in your system is neutralized.")
        }

        return PoolInteractionResult(
            success = true,
            message = messages.joinToString(" "),
            effectApplied = "healing",
            hpChange = actualHeal
        )
    }

    private fun handleDamage(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val damageAmount = data.damageAmount ?: rollDice(data.damageDice ?: "1d6")
        val damageType = data.damageType ?: "acid"

        return PoolInteractionResult(
            success = true,
            message = "The ${pool.liquidColor} liquid burns! You take $damageAmount $damageType damage!",
            effectApplied = "damage",
            hpChange = -damageAmount
        )
    }

    private fun handleBuff(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val stat = data.statModifier ?: "strength"
        val modifier = data.modifierAmount ?: 1
        val duration = data.durationRounds ?: data.durationMinutes?.times(10) ?: 60

        return PoolInteractionResult(
            success = true,
            message = "You feel a surge of power! Your $stat increases by $modifier for $duration rounds.",
            effectApplied = "buff",
            statModifier = stat,
            modifierAmount = modifier,
            modifierDuration = duration
        )
    }

    private fun handleDebuff(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val stat = data.statModifier ?: "strength"
        val modifier = -(data.modifierAmount ?: 1)
        val duration = data.durationRounds ?: data.durationMinutes?.times(10) ?: 60

        return PoolInteractionResult(
            success = true,
            message = "You feel weakened! Your $stat decreases by ${-modifier} for $duration rounds.",
            effectApplied = "debuff",
            statModifier = stat,
            modifierAmount = modifier,
            modifierDuration = duration
        )
    }

    private fun handlePoison(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val chance = data.conditionChance ?: 1.0f

        return if (Random.nextFloat() <= chance) {
            val duration = data.conditionDuration ?: 10
            PoolInteractionResult(
                success = true,
                message = "The liquid was poison! You feel sick and weak...",
                effectApplied = "poison",
                conditionApplied = "poisoned",
                conditionDuration = duration
            )
        } else {
            PoolInteractionResult(
                success = true,
                message = "The liquid tastes bitter but you resist its effects.",
                effectApplied = "resisted"
            )
        }
    }

    private fun handleCharm(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val chance = data.conditionChance ?: 0.5f
        val duration = data.conditionDuration ?: 10

        return if (Random.nextFloat() <= chance) {
            PoolInteractionResult(
                success = true,
                message = "The ${pool.liquidColor} liquid makes you feel wonderfully at peace...",
                effectApplied = "charm",
                conditionApplied = "charmed",
                conditionDuration = duration
            )
        } else {
            PoolInteractionResult(
                success = true,
                message = "The liquid has a strange taste but you shake off its effects.",
                effectApplied = "resisted"
            )
        }
    }

    private fun handleSleep(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val chance = data.conditionChance ?: 0.7f
        val duration = data.conditionDuration ?: 20

        return if (Random.nextFloat() <= chance) {
            PoolInteractionResult(
                success = true,
                message = "Overwhelming drowsiness washes over you... You fall asleep!",
                effectApplied = "sleep",
                conditionApplied = "sleeping",
                conditionDuration = duration
            )
        } else {
            PoolInteractionResult(
                success = true,
                message = "The liquid makes you yawn but you manage to stay awake.",
                effectApplied = "resisted"
            )
        }
    }

    private fun handleStrength(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val modifier = data.modifierAmount ?: 2
        val duration = data.durationRounds ?: 60

        return PoolInteractionResult(
            success = true,
            message = "Mighty power courses through your veins! Strength +$modifier for $duration rounds.",
            effectApplied = "strength",
            statModifier = "strength",
            modifierAmount = modifier,
            modifierDuration = duration
        )
    }

    private fun handleSpeed(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val modifier = data.modifierAmount ?: 1
        val duration = data.durationRounds ?: 30

        return PoolInteractionResult(
            success = true,
            message = "You feel light on your feet! Speed +$modifier for $duration rounds.",
            effectApplied = "speed",
            statModifier = "speed",
            modifierAmount = modifier,
            modifierDuration = duration
        )
    }

    private fun handleTeleport(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val destination = data.teleportLocationId

        return if (destination != null) {
            PoolInteractionResult(
                success = true,
                message = "The pool swirls around you and suddenly you're elsewhere!",
                effectApplied = "teleport",
                teleportLocationId = destination
            )
        } else {
            PoolInteractionResult(
                success = true,
                message = "The pool shimmers but nothing happens.",
                effectApplied = "none"
            )
        }
    }

    private fun handleTreasure(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val items = mutableListOf<String>()
        val gold = data.goldAmount ?: 0

        data.containsItemId?.let { items.add(it) }

        val messages = mutableListOf<String>()
        if (items.isNotEmpty()) {
            messages.add("You reach into the pool and find something!")
        }
        if (gold > 0) {
            messages.add("You find $gold gold pieces at the bottom!")
        }
        if (items.isEmpty() && gold == 0) {
            messages.add("The pool is empty—someone must have taken its treasure already.")
        }

        return PoolInteractionResult(
            success = true,
            message = messages.joinToString(" "),
            effectApplied = "treasure",
            itemsReceived = items,
            goldChange = gold,
            poolDepleted = items.isNotEmpty() || gold > 0
        )
    }

    private fun handleTrap(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val damage = data.damageDice?.let { rollDice(it) } ?: 0
        val message = data.trapMessage ?: "It's a trap!"

        return PoolInteractionResult(
            success = true,
            message = message + if (damage > 0) " You take $damage damage!" else "",
            effectApplied = "trap",
            hpChange = -damage
        )
    }

    private fun handleWine(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        val message = data.customMessage ?: "The wine is surprisingly good!"

        // Wine gives a small buff but also slight debuff
        return PoolInteractionResult(
            success = true,
            message = message,
            effectApplied = "wine"
        )
    }

    private fun handleTransform(user: User, pool: Pool): PoolInteractionResult {
        val data = pool.effectData
        return PoolInteractionResult(
            success = true,
            message = data.customMessage ?: "You feel... different.",
            effectApplied = "transform"
        )
    }

    private fun handleEmpty(pool: Pool): PoolInteractionResult {
        val message = pool.effectData.customMessage ?: "The pool is empty and dry."
        return PoolInteractionResult(
            success = true,
            message = message,
            effectApplied = "none"
        )
    }

    private fun handleStrange(user: User, pool: Pool): PoolInteractionResult {
        // Random effect from various possibilities
        val effects = listOf("healing", "damage", "buff", "debuff", "nothing")
        val effect = effects.random()

        return when (effect) {
            "healing" -> PoolInteractionResult(
                success = true,
                message = "The strange liquid heals some of your wounds!",
                effectApplied = "healing",
                hpChange = rollDice("1d4")
            )
            "damage" -> PoolInteractionResult(
                success = true,
                message = "The strange liquid burns!",
                effectApplied = "damage",
                hpChange = -rollDice("1d4")
            )
            "buff" -> PoolInteractionResult(
                success = true,
                message = "You feel slightly stronger!",
                effectApplied = "buff",
                statModifier = "strength",
                modifierAmount = 1,
                modifierDuration = 30
            )
            "debuff" -> PoolInteractionResult(
                success = true,
                message = "You feel slightly weaker...",
                effectApplied = "debuff",
                statModifier = "strength",
                modifierAmount = -1,
                modifierDuration = 30
            )
            else -> PoolInteractionResult(
                success = true,
                message = "The strange liquid tastes odd but has no effect.",
                effectApplied = "none"
            )
        }
    }

    /**
     * Get pool description including visual appearance and any identified properties.
     */
    fun getPoolDescription(pool: Pool, isIdentified: Boolean = false): String {
        val base = StringBuilder()
        base.append("A pool of ${pool.liquidAppearance} ${pool.liquidColor} liquid. ")
        base.append(pool.description)

        if (isIdentified && pool.effectData.secretMessage != null) {
            base.append(" ")
            base.append(pool.effectData.secretMessage)
        }

        return base.toString()
    }

    /**
     * Attempt to identify a pool's properties.
     * Returns true if successfully identified.
     */
    fun attemptIdentify(user: User, pool: Pool): Boolean {
        if (pool.identifyDifficulty == 0) return true

        // Simple skill check based on intelligence/wisdom
        val skillBonus = (user.wisdom - 10) / 2
        val roll = Random.nextInt(1, 21) + skillBonus
        return roll >= (10 + pool.identifyDifficulty * 2)
    }

    /**
     * Roll dice in format "XdY" or "XdY+Z"
     */
    private fun rollDice(dice: String): Int {
        val regex = """(\d+)d(\d+)([+-]\d+)?""".toRegex()
        val match = regex.find(dice) ?: return 0

        val (numDice, dieSize, modifier) = match.destructured
        var total = 0
        repeat(numDice.toInt()) {
            total += Random.nextInt(1, dieSize.toInt() + 1)
        }
        if (modifier.isNotEmpty()) {
            total += modifier.toInt()
        }
        return total
    }
}
