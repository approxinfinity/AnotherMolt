package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.database.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.random.Random

/**
 * Service for handling creature charm mechanics.
 *
 * Players can attempt to charm creatures to become temporary allies.
 * Charmed creatures follow the player and assist in combat.
 *
 * Charm mechanics:
 * - Charisma-based success chance
 * - Some creature types are immune (undead, constructs, bosses)
 * - Charm has a duration based on charisma and creature level
 * - Charm breaks if: player attacks creature, creature takes too much damage, or duration expires
 * - Only one charmed creature per player at a time
 */
object CharmService {
    private val log = org.slf4j.LoggerFactory.getLogger("CharmService")

    // Charm duration constants (in milliseconds)
    private const val BASE_CHARM_DURATION_MS = 10 * 60 * 1000L  // 10 minutes base
    private const val MAX_CHARM_DURATION_MS = 60 * 60 * 1000L   // 1 hour max

    // Creature types that cannot be charmed
    private val immuneCreatureTypes = setOf(
        "undead", "skeleton", "zombie", "ghost", "wraith", "lich", "vampire",
        "construct", "golem", "automaton",
        "boss", "elite", "legendary",
        "demon", "devil", "fiend",
        "elemental"
    )

    // Classes that get charm bonuses
    private val charmerClasses = setOf(
        "bard", "enchanter", "mesmer", "illusionist", "druid", "shaman", "beastmaster"
    )

    /**
     * Represents a charmed creature instance.
     */
    @Serializable
    data class CharmedCreature(
        val id: String,
        val creatureId: String,
        val charmerUserId: String,
        val locationId: String,
        val currentHp: Int,
        val charmedAt: Long,
        val expiresAt: Long,
        val charmStrength: Int  // How strong the charm is (affects break chance)
    )

    /**
     * Result of a charm attempt.
     */
    @Serializable
    data class CharmResult(
        val success: Boolean,
        val message: String,
        val charmedCreature: CharmedCreatureDto? = null
    )

    /**
     * DTO for charmed creature info sent to client.
     */
    @Serializable
    data class CharmedCreatureDto(
        val id: String,
        val creatureId: String,
        val creatureName: String,
        val currentHp: Int,
        val maxHp: Int,
        val remainingMinutes: Int,
        val imageUrl: String?
    )

    /**
     * Check if a character class is a charmer-type class.
     */
    fun isCharmerClass(characterClass: CharacterClass?): Boolean {
        if (characterClass == null) return false
        val className = characterClass.name.lowercase()
        return charmerClasses.any { className.contains(it) }
    }

    /**
     * Get charm class bonus (percentage added to charm checks).
     */
    fun getCharmerClassBonus(characterClass: CharacterClass?): Int {
        return if (isCharmerClass(characterClass)) 25 else 0
    }

    /**
     * Check if a creature type is immune to charm.
     */
    fun isImmuneToCharm(creature: Creature): Boolean {
        val nameLower = creature.name.lowercase()
        val descLower = creature.desc.lowercase()

        return immuneCreatureTypes.any { type ->
            nameLower.contains(type) || descLower.contains(type)
        }
    }

    /**
     * Attempt to charm a creature.
     *
     * @param user The player attempting to charm
     * @param creature The creature template to charm
     * @param locationId The location where the charm attempt occurs
     * @return CharmResult with success/failure info
     */
    fun attemptCharm(user: User, creature: Creature, locationId: String): CharmResult {
        // Can't charm while in combat
        if (user.currentCombatSessionId != null) {
            return CharmResult(
                success = false,
                message = "You cannot charm while in combat!"
            )
        }

        // Check if creature is immune
        if (isImmuneToCharm(creature)) {
            return CharmResult(
                success = false,
                message = "${creature.name} is immune to charm!"
            )
        }

        // Check if player already has a charmed creature
        val existingCharm = getCharmedCreatureByUser(user.id)
        if (existingCharm != null) {
            val existingCreature = CreatureRepository.findById(existingCharm.creatureId)
            return CharmResult(
                success = false,
                message = "You already have a charmed companion (${existingCreature?.name ?: "unknown"}). Release it first."
            )
        }

        // Calculate charm chance
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
        val classBonus = getCharmerClassBonus(characterClass)
        val charismaMod = StatModifierService.attributeModifier(user.charisma)
        val levelDiff = user.level - creature.level

        // Base: 30% + (CHA mod * 8) + (level diff * 5) + classBonus
        // Level difference matters: higher level creatures are harder to charm
        val charmChance = (30 + (charismaMod * 8) + (levelDiff * 5) + classBonus).coerceIn(5, 85)

        log.debug("${user.name} attempting charm on ${creature.name} with ${charmChance}% chance (CHA=${user.charisma}, levelDiff=$levelDiff, classBonus=$classBonus)")

        val roll = Random.nextInt(100)
        val success = roll < charmChance

        return if (success) {
            // Calculate charm duration based on charisma
            val durationMs = calculateCharmDuration(user.charisma, creature.level)
            val expiresAt = System.currentTimeMillis() + durationMs
            val charmStrength = 50 + charismaMod * 5 + classBonus  // Higher = harder to break

            // Create charmed creature instance
            val charmedId = createCharmedCreature(
                creatureId = creature.id,
                charmerUserId = user.id,
                locationId = locationId,
                currentHp = creature.maxHp,
                expiresAt = expiresAt,
                charmStrength = charmStrength
            )

            val remainingMinutes = (durationMs / 60000).toInt()

            CharmResult(
                success = true,
                message = "You charm ${creature.name}! It will follow you for $remainingMinutes minutes.",
                charmedCreature = CharmedCreatureDto(
                    id = charmedId,
                    creatureId = creature.id,
                    creatureName = creature.name,
                    currentHp = creature.maxHp,
                    maxHp = creature.maxHp,
                    remainingMinutes = remainingMinutes,
                    imageUrl = creature.imageUrl
                )
            )
        } else {
            // Failed charm - creature might become aggressive
            val angerChance = 30
            val angered = Random.nextInt(100) < angerChance

            val message = if (angered) {
                "${creature.name} resists your charm and becomes hostile!"
            } else {
                "Your charm fails to affect ${creature.name}."
            }

            CharmResult(
                success = false,
                message = message
                // TODO: If angered, could trigger combat
            )
        }
    }

    /**
     * Calculate charm duration based on charisma and creature level.
     */
    private fun calculateCharmDuration(charisma: Int, creatureLevel: Int): Long {
        val charismaMod = StatModifierService.attributeModifier(charisma)

        // Base 10 minutes + 2 minutes per CHA modifier point - 1 minute per creature level
        val minutes = (10 + (charismaMod * 2) - (creatureLevel / 2)).coerceIn(5, 60)

        return (minutes * 60 * 1000L).coerceIn(5 * 60 * 1000L, MAX_CHARM_DURATION_MS)
    }

    /**
     * Create a charmed creature record in the database.
     */
    private fun createCharmedCreature(
        creatureId: String,
        charmerUserId: String,
        locationId: String,
        currentHp: Int,
        expiresAt: Long,
        charmStrength: Int
    ): String {
        val id = UUID.randomUUID().toString()
        transaction {
            CharmedCreatureTable.insert {
                it[CharmedCreatureTable.id] = id
                it[CharmedCreatureTable.creatureId] = creatureId
                it[CharmedCreatureTable.charmerUserId] = charmerUserId
                it[CharmedCreatureTable.locationId] = locationId
                it[CharmedCreatureTable.currentHp] = currentHp
                it[charmedAt] = System.currentTimeMillis()
                it[CharmedCreatureTable.expiresAt] = expiresAt
                it[CharmedCreatureTable.charmStrength] = charmStrength
            }
        }
        log.info("Created charmed creature: $id (creature=$creatureId, charmer=$charmerUserId)")
        return id
    }

    /**
     * Get the charmed creature for a user (if any).
     */
    fun getCharmedCreatureByUser(userId: String): CharmedCreature? {
        val now = System.currentTimeMillis()
        return transaction {
            CharmedCreatureTable.selectAll()
                .where {
                    (CharmedCreatureTable.charmerUserId eq userId) and
                    (CharmedCreatureTable.expiresAt greater now)
                }
                .map { it.toCharmedCreature() }
                .singleOrNull()
        }
    }

    /**
     * Get charmed creature DTO for client display.
     */
    fun getCharmedCreatureDto(userId: String): CharmedCreatureDto? {
        val charmed = getCharmedCreatureByUser(userId) ?: return null
        val creature = CreatureRepository.findById(charmed.creatureId) ?: return null

        val remainingMs = charmed.expiresAt - System.currentTimeMillis()
        val remainingMinutes = (remainingMs / 60000).toInt().coerceAtLeast(0)

        return CharmedCreatureDto(
            id = charmed.id,
            creatureId = charmed.creatureId,
            creatureName = creature.name,
            currentHp = charmed.currentHp,
            maxHp = creature.maxHp,
            remainingMinutes = remainingMinutes,
            imageUrl = creature.imageUrl
        )
    }

    /**
     * Release a charmed creature (dismiss the charm).
     */
    fun releaseCharmedCreature(userId: String): String {
        val charmed = getCharmedCreatureByUser(userId)
        if (charmed == null) {
            return "You don't have a charmed companion."
        }

        val creature = CreatureRepository.findById(charmed.creatureId)
        val creatureName = creature?.name ?: "your companion"

        transaction {
            CharmedCreatureTable.deleteWhere { CharmedCreatureTable.id eq charmed.id }
        }

        log.info("Released charmed creature: ${charmed.id} (charmer=$userId)")
        return "You release $creatureName from your charm."
    }

    /**
     * Update charmed creature's location when player moves.
     */
    fun updateCharmedCreatureLocation(userId: String, newLocationId: String) {
        transaction {
            CharmedCreatureTable.update(
                { CharmedCreatureTable.charmerUserId eq userId }
            ) {
                it[locationId] = newLocationId
            }
        }
    }

    /**
     * Damage a charmed creature. If it takes too much damage, the charm breaks.
     *
     * @return true if charm broke, false if still active
     */
    fun damageCharmedCreature(charmedId: String, damage: Int): Boolean {
        val charmed = transaction {
            CharmedCreatureTable.selectAll()
                .where { CharmedCreatureTable.id eq charmedId }
                .map { it.toCharmedCreature() }
                .singleOrNull()
        } ?: return false

        val newHp = charmed.currentHp - damage

        if (newHp <= 0) {
            // Creature died, remove charm
            transaction {
                CharmedCreatureTable.deleteWhere { CharmedCreatureTable.id eq charmedId }
            }
            log.info("Charmed creature $charmedId died from damage")
            return true
        }

        // Check if charm breaks from damage (based on charm strength)
        // Lower HP = higher break chance
        val creature = CreatureRepository.findById(charmed.creatureId)
        val maxHp = creature?.maxHp ?: charmed.currentHp
        val hpPercent = (newHp * 100) / maxHp
        val breakChance = when {
            hpPercent <= 25 -> 40 - (charmed.charmStrength / 4)  // High break chance at low HP
            hpPercent <= 50 -> 20 - (charmed.charmStrength / 5)
            else -> 5
        }.coerceIn(0, 50)

        val roll = Random.nextInt(100)
        if (roll < breakChance) {
            transaction {
                CharmedCreatureTable.deleteWhere { CharmedCreatureTable.id eq charmedId }
            }
            log.info("Charm broke on creature $charmedId due to damage (roll=$roll < $breakChance)")
            return true
        }

        // Update HP
        transaction {
            CharmedCreatureTable.update({ CharmedCreatureTable.id eq charmedId }) {
                it[currentHp] = newHp
            }
        }
        return false
    }

    /**
     * Clean up expired charms.
     */
    fun cleanupExpiredCharms(): Int {
        val now = System.currentTimeMillis()
        val deleted = transaction {
            CharmedCreatureTable.deleteWhere { expiresAt less now }
        }
        if (deleted > 0) {
            log.info("Cleaned up $deleted expired charms")
        }
        return deleted
    }

    /**
     * Convert database row to CharmedCreature.
     */
    private fun ResultRow.toCharmedCreature(): CharmedCreature = CharmedCreature(
        id = this[CharmedCreatureTable.id],
        creatureId = this[CharmedCreatureTable.creatureId],
        charmerUserId = this[CharmedCreatureTable.charmerUserId],
        locationId = this[CharmedCreatureTable.locationId],
        currentHp = this[CharmedCreatureTable.currentHp],
        charmedAt = this[CharmedCreatureTable.charmedAt],
        expiresAt = this[CharmedCreatureTable.expiresAt],
        charmStrength = this[CharmedCreatureTable.charmStrength]
    )
}

/**
 * Database table for charmed creatures.
 */
object CharmedCreatureTable : Table("charmed_creature") {
    val id = varchar("id", 36)
    val creatureId = varchar("creature_id", 36)
    val charmerUserId = varchar("charmer_user_id", 36)
    val locationId = varchar("location_id", 64)
    val currentHp = integer("current_hp")
    val charmedAt = long("charmed_at")
    val expiresAt = long("expires_at")
    val charmStrength = integer("charm_strength").default(50)

    override val primaryKey = PrimaryKey(id)
}
