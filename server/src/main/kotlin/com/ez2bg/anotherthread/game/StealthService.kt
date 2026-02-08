package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlin.random.Random

/**
 * MajorMUD-style stealth system.
 *
 * Characters can:
 * - HIDE: Become hidden in place. Cannot move while hidden.
 * - SNEAK: Move stealthily between rooms. Each move has a chance to be detected.
 *
 * Detection:
 * - Other characters at the same location make perception checks
 * - If perception check succeeds, they "notice" the hidden/sneaking character
 * - Otherwise, the character is invisible in the Others list
 *
 * Breaking stealth:
 * - Attacking or using most abilities breaks stealth
 * - Taking damage breaks stealth
 * - Entering combat breaks stealth
 * - Moving while hidden breaks hiding (must sneak to move stealthily)
 *
 * Class bonuses:
 * - Rogue-type classes get bonuses to stealth checks
 * - Some classes may get perception bonuses
 */
object StealthService {
    private val log = org.slf4j.LoggerFactory.getLogger("StealthService")

    /**
     * Result of attempting to hide.
     */
    data class HideResult(
        val success: Boolean,
        val message: String,
        val stealthValue: Int = 0  // How well hidden (for detection checks)
    )

    /**
     * Result of attempting to sneak.
     */
    data class SneakResult(
        val success: Boolean,
        val message: String,
        val stealthValue: Int = 0  // How stealthy the movement is
    )

    /**
     * Result of a perception check.
     */
    data class PerceptionResult(
        val detected: Boolean,
        val observerId: String,
        val observerName: String,
        val targetId: String,
        val targetName: String,
        val stealthType: StealthType,
        val message: String
    )

    enum class StealthType {
        HIDING,
        SNEAKING
    }

    /**
     * Classes that get stealth bonuses.
     * This could be expanded to pull from character class definitions.
     */
    private val stealthClasses = setOf(
        "rogue", "thief", "assassin", "ranger", "scout", "ninja", "shadow"
    )

    /**
     * Check if a character class is a stealth-focused class.
     */
    fun isStealthClass(characterClass: CharacterClass?): Boolean {
        if (characterClass == null) return false
        val className = characterClass.name.lowercase()
        return stealthClasses.any { className.contains(it) }
    }

    /**
     * Get stealth class bonus (percentage added to stealth checks).
     */
    fun getStealthClassBonus(characterClass: CharacterClass?): Int {
        return if (isStealthClass(characterClass)) 20 else 0
    }

    /**
     * Calculate armor penalty for stealth based on equipped items.
     * Heavy armor makes sneaking harder.
     */
    fun calculateArmorPenalty(user: User): Int {
        val equippedItems = user.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
        var penalty = 0

        for (item in equippedItems) {
            if (item.equipmentType == "armor") {
                val defense = item.statBonuses?.defense ?: 0
                // Heavier armor (higher defense) = bigger penalty
                // 2 penalty per point of defense above 5
                if (defense > 5) {
                    penalty += (defense - 5) * 2
                }
            }
        }

        return penalty.coerceAtMost(50)  // Cap at 50% penalty
    }

    /**
     * Attempt to hide in the current location.
     * Returns success/failure and updates user stealth status.
     */
    fun attemptHide(user: User): HideResult {
        // Can't hide if in combat
        if (user.currentCombatSessionId != null) {
            return HideResult(
                success = false,
                message = "You cannot hide while in combat!"
            )
        }

        // Already hidden
        if (user.isHidden) {
            return HideResult(
                success = true,
                message = "You are already hidden.",
                stealthValue = calculateHideValue(user)
            )
        }

        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
        val armorPenalty = calculateArmorPenalty(user)
        val classBonus = getStealthClassBonus(characterClass)

        // Calculate hide chance
        val baseHideChance = StatModifierService.hideChance(user.dexterity, user.level, armorPenalty)
        val totalHideChance = (baseHideChance + classBonus).coerceIn(5, 95)

        val roll = Random.nextInt(100)
        val success = roll < totalHideChance

        return if (success) {
            val stealthValue = calculateHideValue(user)
            // Update user status
            UserRepository.updateStealthStatus(user.id, isHidden = true, isSneaking = false)

            HideResult(
                success = true,
                message = "You slip into the shadows and hide.",
                stealthValue = stealthValue
            )
        } else {
            HideResult(
                success = false,
                message = "You try to hide but fail to find adequate cover."
            )
        }
    }

    /**
     * Attempt to start sneaking.
     * Returns success/failure and updates user stealth status.
     */
    fun attemptSneak(user: User): SneakResult {
        // Can't sneak if in combat
        if (user.currentCombatSessionId != null) {
            return SneakResult(
                success = false,
                message = "You cannot sneak while in combat!"
            )
        }

        // Already sneaking
        if (user.isSneaking) {
            return SneakResult(
                success = true,
                message = "You are already sneaking.",
                stealthValue = calculateSneakValue(user)
            )
        }

        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
        val armorPenalty = calculateArmorPenalty(user)
        val classBonus = getStealthClassBonus(characterClass)

        // Calculate sneak chance
        val baseSneakChance = StatModifierService.sneakChance(user.dexterity, user.level, armorPenalty)
        val totalSneakChance = (baseSneakChance + classBonus).coerceIn(5, 95)

        val roll = Random.nextInt(100)
        val success = roll < totalSneakChance

        return if (success) {
            val stealthValue = calculateSneakValue(user)
            // Update user status - sneaking replaces hiding
            UserRepository.updateStealthStatus(user.id, isHidden = false, isSneaking = true)

            SneakResult(
                success = true,
                message = "You begin moving stealthily.",
                stealthValue = stealthValue
            )
        } else {
            SneakResult(
                success = false,
                message = "You try to move quietly but make too much noise."
            )
        }
    }

    /**
     * Stop hiding/sneaking voluntarily.
     */
    fun revealSelf(user: User): String {
        val wasHidden = user.isHidden
        val wasSneaking = user.isSneaking

        UserRepository.clearStealthStatus(user.id)

        return when {
            wasHidden && wasSneaking -> "You step out of the shadows."
            wasHidden -> "You emerge from your hiding spot."
            wasSneaking -> "You stop sneaking and walk normally."
            else -> "You are already visible."
        }
    }

    /**
     * Calculate the stealth value for hiding (used in detection checks).
     * Higher = harder to detect.
     */
    fun calculateHideValue(user: User): Int {
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
        val armorPenalty = calculateArmorPenalty(user)
        val classBonus = getStealthClassBonus(characterClass)

        return StatModifierService.hideChance(user.dexterity, user.level, armorPenalty) + classBonus
    }

    /**
     * Calculate the stealth value for sneaking (used in detection checks).
     * Higher = harder to detect.
     */
    fun calculateSneakValue(user: User): Int {
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
        val armorPenalty = calculateArmorPenalty(user)
        val classBonus = getStealthClassBonus(characterClass)

        return StatModifierService.sneakChance(user.dexterity, user.level, armorPenalty) + classBonus
    }

    /**
     * Check if an observer can detect a stealthy character.
     * Returns a PerceptionResult if detected, null if not detected.
     */
    fun checkDetection(
        observer: User,
        target: User,
        stealthType: StealthType
    ): PerceptionResult? {
        // Can't detect yourself
        if (observer.id == target.id) return null

        // Target must actually be stealthy
        val isStealthy = when (stealthType) {
            StealthType.HIDING -> target.isHidden
            StealthType.SNEAKING -> target.isSneaking
        }
        if (!isStealthy) return null

        // Get target's stealth value
        val targetStealth = when (stealthType) {
            StealthType.HIDING -> calculateHideValue(target)
            StealthType.SNEAKING -> calculateSneakValue(target)
        }

        // Calculate observer's detection chance
        val detectionChance = StatModifierService.detectionChance(
            observerWis = observer.wisdom,
            observerInt = observer.intelligence,
            observerLevel = observer.level,
            targetStealth = targetStealth
        )

        val roll = Random.nextInt(100)
        val detected = roll < detectionChance

        val message = when (stealthType) {
            StealthType.HIDING -> "You notice ${target.name} hiding in the shadows."
            StealthType.SNEAKING -> "You notice ${target.name} sneaking ${if (target.currentLocationId == observer.currentLocationId) "nearby" else "into the room"}."
        }

        return if (detected) {
            log.debug("${observer.name} detected ${target.name} (roll=$roll < chance=$detectionChance)")
            PerceptionResult(
                detected = true,
                observerId = observer.id,
                observerName = observer.name,
                targetId = target.id,
                targetName = target.name,
                stealthType = stealthType,
                message = message
            )
        } else {
            log.debug("${observer.name} did NOT detect ${target.name} (roll=$roll >= chance=$detectionChance)")
            null
        }
    }

    /**
     * Check all observers at a location for detection of a stealthy character.
     * Returns list of observers who detected the target.
     */
    fun checkLocationDetection(
        target: User,
        locationId: String,
        stealthType: StealthType
    ): List<PerceptionResult> {
        val results = mutableListOf<PerceptionResult>()

        // Get all other users at this location
        val usersAtLocation = UserRepository.findActiveUsersAtLocation(locationId)
            .filter { it.id != target.id }

        for (observer in usersAtLocation) {
            val result = checkDetection(observer, target, stealthType)
            if (result != null) {
                results.add(result)
            }
        }

        return results
    }

    /**
     * Get list of users that a specific observer can see at a location.
     * Hidden/sneaking users are filtered based on detection.
     *
     * @param observer The user doing the observing
     * @param allUsersAtLocation All users at the location
     * @param alreadyDetected Map of targetId -> true if observer has already detected them
     */
    fun getVisibleUsers(
        observer: User,
        allUsersAtLocation: List<User>,
        alreadyDetected: Set<String> = emptySet()
    ): Pair<List<User>, List<PerceptionResult>> {
        val visibleUsers = mutableListOf<User>()
        val newDetections = mutableListOf<PerceptionResult>()

        for (user in allUsersAtLocation) {
            // Always see yourself
            if (user.id == observer.id) {
                visibleUsers.add(user)
                continue
            }

            // If user is not stealthy, always visible
            if (!user.isHidden && !user.isSneaking) {
                visibleUsers.add(user)
                continue
            }

            // Already detected this user
            if (user.id in alreadyDetected) {
                visibleUsers.add(user)
                continue
            }

            // Check detection
            val stealthType = if (user.isHidden) StealthType.HIDING else StealthType.SNEAKING
            val detection = checkDetection(observer, user, stealthType)

            if (detection != null && detection.detected) {
                visibleUsers.add(user)
                newDetections.add(detection)
            }
            // If not detected, user is not added to visible list
        }

        return Pair(visibleUsers, newDetections)
    }

    /**
     * Break stealth when taking a visible action (attack, spell, etc).
     */
    fun breakStealth(userId: String, reason: String = "action"): Boolean {
        val user = UserRepository.findById(userId) ?: return false

        if (user.isHidden || user.isSneaking) {
            UserRepository.clearStealthStatus(userId)
            log.debug("${user.name} stealth broken by $reason")
            return true
        }
        return false
    }

    /**
     * Check if a user can use stealth abilities (not in combat).
     */
    fun canUseStealthAbility(user: User): Pair<Boolean, String?> {
        if (user.currentCombatSessionId != null) {
            return Pair(false, "Cannot use stealth abilities while in combat!")
        }
        return Pair(true, null)
    }
}
