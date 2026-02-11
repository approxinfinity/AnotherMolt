package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.combat.CombatConfig
import com.ez2bg.anotherthread.combat.CombatService
import com.ez2bg.anotherthread.database.ItemRepository
import com.ez2bg.anotherthread.database.UserRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Global game tick service that processes world events on a regular interval.
 *
 * The tick is the heartbeat of the game world. Every tick (3 seconds):
 * 1. All online players regenerate HP/Mana/Stamina based on their stats
 * 2. Combat sessions process their rounds
 * 3. Creatures may wander between locations
 * 4. Status effects tick (DoTs, buffs, etc.)
 *
 * This ensures the game world is always alive and consistent,
 * regardless of whether players are in combat or not.
 */
object GameTickService {
    private val log = LoggerFactory.getLogger(GameTickService::class.java)

    private var tickJob: Job? = null
    private val tickScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Track last regen time per user to avoid double-regen during combat
    private val lastRegenTick = mutableMapOf<String, Long>()
    private var currentTickNumber = 0L

    /**
     * Start the global game tick loop. Called once when server starts.
     */
    fun startTickLoop(combatService: CombatService) {
        if (tickJob?.isActive == true) return

        tickJob = tickScope.launch {
            log.info("Global game tick started (${CombatConfig.ROUND_DURATION_MS}ms interval)")
            while (isActive) {
                val tickStart = System.currentTimeMillis()
                currentTickNumber++

                try {
                    // 1. Process resource regeneration for ALL online players
                    processGlobalRegen()

                    // 2. Process combat (for players in combat sessions)
                    combatService.processTick()

                    // 3. Process creature wandering
                    combatService.processCreatureWandering()

                    // 4. Process creature respawns (every N ticks)
                    if (currentTickNumber % RespawnConfig.RESPAWN_CHECK_INTERVAL_TICKS == 0L) {
                        CreatureRespawnService.processRespawns(currentTickNumber)
                    }

                    // 5. Process lock resets (check every tick since it's lightweight)
                    LockResetService.processLockResets(currentTickNumber)

                    // 6. Process puzzle resets (every 10 ticks to reduce DB queries)
                    if (currentTickNumber % 10 == 0L) {
                        LockResetService.processPuzzleResets()
                    }

                } catch (e: Exception) {
                    log.error("Error in game tick $currentTickNumber: ${e.message}", e)
                }

                // Maintain consistent tick timing
                val elapsed = System.currentTimeMillis() - tickStart
                val sleepTime = (CombatConfig.ROUND_DURATION_MS - elapsed).coerceAtLeast(100)
                delay(sleepTime)
            }
        }
    }

    /**
     * Stop the game tick loop. Called on server shutdown.
     */
    fun stopTickLoop() {
        tickJob?.cancel()
        tickJob = null
        log.info("Global game tick stopped")
    }

    /**
     * Process resource regeneration for all online players.
     * Players in combat get their regen handled by CombatService instead.
     *
     * MajorMUD-style regen:
     * - Base HP regen that everyone gets (1 per tick minimum)
     * - CON modifier adds to HP regen
     * - Level adds scaling regen (+1 per 3 levels)
     * - Equipment HP bonuses are considered for max HP cap
     */
    private suspend fun processGlobalRegen() {
        // Get all users who are online (have been active in last 5 minutes)
        val recentlyActiveUsers = UserRepository.findRecentlyActive(300_000) // 5 minutes

        if (currentTickNumber % 10 == 0L) {
            log.info("Regen tick $currentTickNumber: ${recentlyActiveUsers.size} active users")
        }

        for (user in recentlyActiveUsers) {
            // Skip if we already processed regen for this user this tick
            if (lastRegenTick[user.id] == currentTickNumber) {
                continue
            }
            lastRegenTick[user.id] = currentTickNumber

            // Calculate regen based on stats (MajorMUD style)
            val conMod = UserRepository.attributeModifier(user.constitution)
            val intMod = UserRepository.attributeModifier(user.intelligence)
            val wisMod = UserRepository.attributeModifier(user.wisdom)

            // HP regen: base 1 + CON modifier + level/3
            // Everyone gets at least 1 HP per tick, CON adds more, level adds scaling
            val levelBonus = user.level / 3
            val hpRegen = (1 + conMod + levelBonus).coerceAtLeast(1)

            // Mana regen: 1 + max(INT, WIS) modifier
            val spellMod = maxOf(intMod, wisMod)
            val manaRegen = (1 + spellMod).coerceAtLeast(1)

            // Stamina regen: 2 + CON modifier
            val staminaRegen = (2 + conMod).coerceAtLeast(1)

            // Calculate effective max HP including equipment bonuses
            val equippedItems = user.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
            val equipHpBonus = equippedItems.sumOf { it.statBonuses?.maxHp ?: 0 }
            val effectiveMaxHp = user.maxHp + equipHpBonus

            // Apply regeneration if below max
            if (user.currentHp < effectiveMaxHp) {
                UserRepository.healWithEquipment(user.id, hpRegen, effectiveMaxHp)
            }

            if (user.currentMana < user.maxMana) {
                val restored = UserRepository.restoreMana(user.id, manaRegen)
                if (currentTickNumber % 10 == 0L) {
                    log.info("Mana regen for ${user.name}: ${user.currentMana}/${user.maxMana} +$manaRegen (restored=$restored)")
                }
            }

            if (user.currentStamina < user.maxStamina) {
                UserRepository.restoreStamina(user.id, staminaRegen)
            }

            // TODO: Could broadcast resource update to connected WebSocket clients
        }

        // Clean up old entries from lastRegenTick (users who went offline)
        if (currentTickNumber % 100 == 0L) {
            val activeUserIds = recentlyActiveUsers.map { it.id }.toSet()
            lastRegenTick.keys.removeIf { it !in activeUserIds }
        }
    }

    /**
     * Mark that a user's regen was handled this tick (by combat).
     * This prevents double-regen.
     */
    fun markRegenHandled(userId: String) {
        lastRegenTick[userId] = currentTickNumber
    }

    /**
     * Get the current tick number (for debugging/sync purposes).
     */
    fun getCurrentTick(): Long = currentTickNumber
}
