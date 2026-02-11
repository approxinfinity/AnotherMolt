package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.random.Random

/**
 * Service for handling trap detection, disarming, and triggering.
 */
object TrapService {
    private val log = LoggerFactory.getLogger(TrapService::class.java)

    /**
     * Result of a trap interaction.
     */
    @Serializable
    data class TrapResult(
        val success: Boolean,
        val message: String,
        val trapTriggered: Boolean = false,
        val trapDetected: Boolean = false,
        val trapDisarmed: Boolean = false,

        // Effects
        val hpChange: Int = 0,
        val conditionApplied: String? = null,
        val conditionDuration: Int = 0,
        val teleportLocationId: String? = null,
        val alertedCreatures: List<String> = emptyList()
    )

    /**
     * Check if a trap is triggered when entering a location.
     * Called when player moves to a new location.
     */
    fun checkMovementTraps(user: User, locationId: String): TrapResult? {
        val traps = TrapRepository.findByLocationId(locationId)
            .filter { it.triggerType == TrapTrigger.MOVEMENT && it.isArmed }

        for (trap in traps) {
            // Check if player has already detected this trap
            val detectedKey = "detected_trap_${trap.id}"
            val detected = FeatureStateRepository.getState(user.id, detectedKey)?.value == "true"

            if (detected) {
                // Player knows about it, can avoid
                continue
            }

            // Check if player has disarmed this trap
            val disarmedKey = "disarmed_trap_${trap.id}"
            val disarmed = FeatureStateRepository.getState(user.id, disarmedKey)?.value == "true"

            if (disarmed) {
                continue
            }

            // Trap triggers!
            return triggerTrap(user, trap)
        }

        return null
    }

    /**
     * Attempt to detect traps in a location.
     */
    fun attemptDetect(user: User, locationId: String): TrapResult {
        val traps = TrapRepository.findByLocationId(locationId)
            .filter { it.isHidden }

        if (traps.isEmpty()) {
            return TrapResult(
                success = true,
                message = "You search carefully but find no traps."
            )
        }

        val detectedTraps = mutableListOf<String>()

        for (trap in traps) {
            // Check if already detected
            val detectedKey = "detected_trap_${trap.id}"
            if (FeatureStateRepository.getState(user.id, detectedKey)?.value == "true") {
                continue
            }

            // Skill check: d20 + perception bonus vs DC
            val perceptionBonus = (user.wisdom - 10) / 2
            val roll = Random.nextInt(1, 21) + perceptionBonus
            val dc = 10 + trap.detectDifficulty * 2

            if (roll >= dc) {
                FeatureStateRepository.setState(user.id, detectedKey, "true")
                detectedTraps.add(trap.name)
            }
        }

        return if (detectedTraps.isNotEmpty()) {
            TrapResult(
                success = true,
                message = "You detected: ${detectedTraps.joinToString(", ")}!",
                trapDetected = true
            )
        } else {
            TrapResult(
                success = true,
                message = "You search carefully but don't notice anything unusual."
            )
        }
    }

    /**
     * Attempt to disarm a specific trap.
     */
    fun attemptDisarm(user: User, trapId: String): TrapResult {
        val trap = TrapRepository.findById(trapId)
            ?: return TrapResult(false, "Trap not found.")

        // Must have detected it first
        val detectedKey = "detected_trap_${trap.id}"
        if (FeatureStateRepository.getState(user.id, detectedKey)?.value != "true") {
            return TrapResult(false, "You haven't detected this trap yet.")
        }

        // Check if already disarmed
        val disarmedKey = "disarmed_trap_${trap.id}"
        if (FeatureStateRepository.getState(user.id, disarmedKey)?.value == "true") {
            return TrapResult(true, "This trap is already disarmed.")
        }

        // Skill check: d20 + dexterity bonus vs DC
        val dexBonus = (user.dexterity - 10) / 2
        val roll = Random.nextInt(1, 21) + dexBonus
        val dc = 10 + trap.disarmDifficulty * 2

        return if (roll >= dc) {
            FeatureStateRepository.setState(user.id, disarmedKey, "true")
            TrapResult(
                success = true,
                message = "You carefully disarm the ${trap.name}.",
                trapDisarmed = true
            )
        } else if (roll < dc - 5) {
            // Critical failure - trigger the trap!
            val triggerResult = triggerTrap(user, trap)
            TrapResult(
                success = false,
                message = "You fumble and trigger the trap! ${triggerResult.message}",
                trapTriggered = true,
                hpChange = triggerResult.hpChange,
                conditionApplied = triggerResult.conditionApplied,
                conditionDuration = triggerResult.conditionDuration,
                teleportLocationId = triggerResult.teleportLocationId
            )
        } else {
            TrapResult(
                success = false,
                message = "You fail to disarm the ${trap.name}. You can try again."
            )
        }
    }

    /**
     * Trigger a trap and apply its effects.
     */
    fun triggerTrap(user: User, trap: Trap): TrapResult {
        log.info("Trap ${trap.name} triggered for user ${user.name}")

        val data = trap.effectData

        return when (trap.trapType) {
            TrapType.PIT -> handlePitTrap(user, trap, data)
            TrapType.DART -> handleDartTrap(user, trap, data)
            TrapType.POISON_NEEDLE -> handlePoisonNeedle(user, trap, data)
            TrapType.BOULDER -> handleBoulderTrap(user, trap, data)
            TrapType.ALARM -> handleAlarmTrap(user, trap, data)
            TrapType.FIRE -> handleFireTrap(user, trap, data)
            TrapType.SPEAR -> handleSpearTrap(user, trap, data)
            TrapType.CAGE -> handleCageTrap(user, trap, data)
            TrapType.TELEPORT -> handleTeleportTrap(user, trap, data)
            TrapType.MAGIC -> handleMagicTrap(user, trap, data)
        }
    }

    private fun handlePitTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val depth = data.pitDepth ?: 10
        val damageDice = data.damageDice ?: "1d6"
        val damage = rollDice(damageDice)

        // Dex save for half damage
        val dexBonus = (user.dexterity - 10) / 2
        val saveRoll = Random.nextInt(1, 21) + dexBonus
        val dc = data.savingThrowDC ?: 12
        val actualDamage = if (saveRoll >= dc) damage / 2 else damage

        return TrapResult(
            success = false,
            message = "The floor gives way! You fall ${depth} feet into a pit and take $actualDamage damage!",
            trapTriggered = true,
            hpChange = -actualDamage,
            conditionApplied = "prone",
            conditionDuration = 1
        )
    }

    private fun handleDartTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val damageDice = data.damageDice ?: "1d4"
        val damage = rollDice(damageDice)

        // Dex save to avoid
        val dexBonus = (user.dexterity - 10) / 2
        val saveRoll = Random.nextInt(1, 21) + dexBonus
        val dc = data.savingThrowDC ?: 12

        return if (saveRoll >= dc) {
            TrapResult(
                success = false,
                message = "Darts shoot from the wall! You dodge them just in time!",
                trapTriggered = true
            )
        } else {
            val poisonResult = if (data.poisonDuration != null) {
                TrapResult(
                    success = false,
                    message = "Darts shoot from the wall and strike you for $damage damage! The darts are poisoned!",
                    trapTriggered = true,
                    hpChange = -damage,
                    conditionApplied = "poisoned",
                    conditionDuration = data.poisonDuration
                )
            } else {
                TrapResult(
                    success = false,
                    message = "Darts shoot from the wall and strike you for $damage damage!",
                    trapTriggered = true,
                    hpChange = -damage
                )
            }
            poisonResult
        }
    }

    private fun handlePoisonNeedle(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val damage = rollDice(data.damageDice ?: "1")
        val poisonDuration = data.poisonDuration ?: 10

        // Con save
        val conBonus = (user.constitution - 10) / 2
        val saveRoll = Random.nextInt(1, 21) + conBonus
        val dc = data.savingThrowDC ?: 13

        return if (saveRoll >= dc) {
            TrapResult(
                success = false,
                message = "A poison needle pricks your finger! You resist the poison's effects.",
                trapTriggered = true,
                hpChange = -damage
            )
        } else {
            TrapResult(
                success = false,
                message = "A poison needle pricks your finger! You feel the venom coursing through your veins!",
                trapTriggered = true,
                hpChange = -damage,
                conditionApplied = "poisoned",
                conditionDuration = poisonDuration
            )
        }
    }

    private fun handleBoulderTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val damageDice = data.damageDice ?: "4d6"
        val damage = rollDice(damageDice)

        // Dex save for half
        val dexBonus = (user.dexterity - 10) / 2
        val saveRoll = Random.nextInt(1, 21) + dexBonus
        val dc = data.savingThrowDC ?: 15
        val actualDamage = if (saveRoll >= dc) damage / 2 else damage

        return TrapResult(
            success = false,
            message = if (saveRoll >= dc) {
                "A massive boulder rolls down the corridor! You dive aside, taking $actualDamage damage!"
            } else {
                "A massive boulder rolls down the corridor and crushes you for $actualDamage damage!"
            },
            trapTriggered = true,
            hpChange = -actualDamage
        )
    }

    private fun handleAlarmTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val alertedCreatures = data.alertsCreatureIds ?: emptyList()

        return TrapResult(
            success = false,
            message = data.customMessage ?: "An alarm sounds! Nearby creatures are alerted to your presence!",
            trapTriggered = true,
            alertedCreatures = alertedCreatures
        )
    }

    private fun handleFireTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val damageDice = data.damageDice ?: "2d6"
        val damage = rollDice(damageDice)

        // Dex save for half
        val dexBonus = (user.dexterity - 10) / 2
        val saveRoll = Random.nextInt(1, 21) + dexBonus
        val dc = data.savingThrowDC ?: 13
        val actualDamage = if (saveRoll >= dc) damage / 2 else damage

        return TrapResult(
            success = false,
            message = "Flames erupt from hidden vents! You take $actualDamage fire damage!",
            trapTriggered = true,
            hpChange = -actualDamage
        )
    }

    private fun handleSpearTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val damageDice = data.damageDice ?: "2d6"
        val damage = rollDice(damageDice)

        // Dex save to avoid
        val dexBonus = (user.dexterity - 10) / 2
        val saveRoll = Random.nextInt(1, 21) + dexBonus
        val dc = data.savingThrowDC ?: 14

        return if (saveRoll >= dc) {
            TrapResult(
                success = false,
                message = "Spears thrust up from the floor! You leap aside just in time!",
                trapTriggered = true
            )
        } else {
            TrapResult(
                success = false,
                message = "Spears thrust up from the floor, impaling you for $damage damage!",
                trapTriggered = true,
                hpChange = -damage
            )
        }
    }

    private fun handleCageTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val duration = data.conditionDuration ?: 10

        // Dex save to avoid
        val dexBonus = (user.dexterity - 10) / 2
        val saveRoll = Random.nextInt(1, 21) + dexBonus
        val dc = data.savingThrowDC ?: 13

        return if (saveRoll >= dc) {
            TrapResult(
                success = false,
                message = "A cage drops from above! You roll out of the way!",
                trapTriggered = true
            )
        } else {
            TrapResult(
                success = false,
                message = "A cage drops from above, trapping you inside!",
                trapTriggered = true,
                conditionApplied = "restrained",
                conditionDuration = duration
            )
        }
    }

    private fun handleTeleportTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val destination = data.teleportLocationId

        return if (destination != null) {
            TrapResult(
                success = false,
                message = data.customMessage ?: "Reality warps around you and suddenly you're somewhere else!",
                trapTriggered = true,
                teleportLocationId = destination
            )
        } else {
            TrapResult(
                success = false,
                message = "The trap fizzles - its magic has faded.",
                trapTriggered = true
            )
        }
    }

    private fun handleMagicTrap(user: User, trap: Trap, data: TrapEffectData): TrapResult {
        val damage = data.damageDice?.let { rollDice(it) } ?: 0

        return TrapResult(
            success = false,
            message = data.customMessage ?: "Magical energy surges through you!",
            trapTriggered = true,
            hpChange = if (damage > 0) -damage else 0,
            conditionApplied = data.appliesCondition,
            conditionDuration = data.conditionDuration ?: 0
        )
    }

    /**
     * Get all detected traps in a location for a user.
     */
    fun getDetectedTraps(userId: String, locationId: String): List<Trap> {
        val traps = TrapRepository.findByLocationId(locationId)
        return traps.filter { trap ->
            val detectedKey = "detected_trap_${trap.id}"
            FeatureStateRepository.getState(userId, detectedKey)?.value == "true"
        }
    }

    /**
     * Roll dice in format "XdY" or "XdY+Z"
     */
    private fun rollDice(dice: String): Int {
        val regex = """(\d+)d(\d+)([+-]\d+)?""".toRegex()
        val match = regex.find(dice) ?: return dice.toIntOrNull() ?: 0

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
