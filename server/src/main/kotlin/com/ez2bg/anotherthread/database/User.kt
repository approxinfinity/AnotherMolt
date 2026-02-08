package com.ez2bg.anotherthread.database

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val passwordHash: String = "",
    val desc: String = "",
    val itemIds: List<String> = emptyList(),
    val featureIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val currentLocationId: String? = null,
    val characterClassId: String? = null,
    val classGenerationStartedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    // Combat stats
    val level: Int = 1,
    val experience: Int = 0,
    val maxHp: Int = 10,
    val currentHp: Int = 10,
    val maxMana: Int = 10,
    val currentMana: Int = 10,
    val maxStamina: Int = 10,
    val currentStamina: Int = 10,
    val currentCombatSessionId: String? = null,
    // D&D Attributes
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val attributeQualityBonus: Int = 0,
    val attributesGeneratedAt: Long? = null,
    // Economy and equipment
    val gold: Int = 50,  // Starting gold for new characters
    val equippedItemIds: List<String> = emptyList(),
    // Trainer system: abilities the user has learned from trainers
    val learnedAbilityIds: List<String> = emptyList()
)

/**
 * User response DTO that excludes the password hash
 */
@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val desc: String,
    val itemIds: List<String>,
    val featureIds: List<String>,
    val imageUrl: String?,
    val currentLocationId: String?,
    val characterClassId: String?,
    val classGenerationStartedAt: Long?,
    val createdAt: Long,
    val lastActiveAt: Long,
    // Combat stats
    val level: Int,
    val experience: Int,
    val maxHp: Int,
    val currentHp: Int,
    val maxMana: Int,
    val currentMana: Int,
    val maxStamina: Int,
    val currentStamina: Int,
    val currentCombatSessionId: String?,
    // D&D Attributes
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    val attributeQualityBonus: Int,
    val attributesGeneratedAt: Long?,
    // Economy and equipment
    val gold: Int,
    val equippedItemIds: List<String>,
    // Trainer system: abilities the user has learned from trainers
    val learnedAbilityIds: List<String>,
    // Generated appearance description based on equipment
    val appearanceDescription: String
)

fun User.toResponse(): UserResponse {
    // Calculate equipment bonuses from equipped items
    val equippedItems = equippedItemIds.mapNotNull { ItemRepository.findById(it) }
    val equipHpBonus = equippedItems.sumOf { it.statBonuses?.maxHp ?: 0 }

    return UserResponse(
        id = id,
        name = name,
        desc = desc,
        itemIds = itemIds,
        featureIds = featureIds,
        imageUrl = imageUrl,
        currentLocationId = currentLocationId,
        characterClassId = characterClassId,
        classGenerationStartedAt = classGenerationStartedAt,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt,
        level = level,
        experience = experience,
        maxHp = maxHp + equipHpBonus,
        currentHp = currentHp,
        maxMana = maxMana,
        currentMana = currentMana,
        maxStamina = maxStamina,
        currentStamina = currentStamina,
        currentCombatSessionId = currentCombatSessionId,
        strength = strength,
        dexterity = dexterity,
        constitution = constitution,
        intelligence = intelligence,
        wisdom = wisdom,
        charisma = charisma,
        attributeQualityBonus = attributeQualityBonus,
        attributesGeneratedAt = attributesGeneratedAt,
        gold = gold,
        equippedItemIds = equippedItemIds,
        learnedAbilityIds = learnedAbilityIds,
        appearanceDescription = UserRepository.generateAppearanceDescription(this)
    )
}

object UserRepository {
    private fun listToJson(list: List<String>): String {
        return list.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
            .let { "[$it]" }
    }

    private fun jsonToList(json: String): List<String> {
        if (json == "[]") return emptyList()
        return json
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"").replace("\\\"", "\"") }
            .filter { it.isNotEmpty() }
    }

    private fun ResultRow.toUser(): User = User(
        id = this[UserTable.id],
        name = this[UserTable.name],
        passwordHash = this[UserTable.passwordHash],
        desc = this[UserTable.desc],
        itemIds = jsonToList(this[UserTable.itemIds]),
        featureIds = jsonToList(this[UserTable.featureIds]),
        imageUrl = this[UserTable.imageUrl],
        currentLocationId = this[UserTable.currentLocationId],
        characterClassId = this[UserTable.characterClassId],
        classGenerationStartedAt = this[UserTable.classGenerationStartedAt],
        createdAt = this[UserTable.createdAt],
        lastActiveAt = this[UserTable.lastActiveAt],
        level = this[UserTable.level],
        experience = this[UserTable.experience],
        maxHp = this[UserTable.maxHp],
        currentHp = this[UserTable.currentHp],
        maxMana = this[UserTable.maxMana],
        currentMana = this[UserTable.currentMana],
        maxStamina = this[UserTable.maxStamina],
        currentStamina = this[UserTable.currentStamina],
        currentCombatSessionId = this[UserTable.currentCombatSessionId],
        strength = this[UserTable.strength],
        dexterity = this[UserTable.dexterity],
        constitution = this[UserTable.constitution],
        intelligence = this[UserTable.intelligence],
        wisdom = this[UserTable.wisdom],
        charisma = this[UserTable.charisma],
        attributeQualityBonus = this[UserTable.attributeQualityBonus],
        attributesGeneratedAt = this[UserTable.attributesGeneratedAt],
        gold = this[UserTable.gold],
        equippedItemIds = jsonToList(this[UserTable.equippedItemIds]),
        learnedAbilityIds = jsonToList(this[UserTable.learnedAbilityIds])
    )

    fun create(user: User): User = transaction {
        UserTable.insert {
            it[id] = user.id
            it[name] = user.name
            it[passwordHash] = user.passwordHash
            it[desc] = user.desc
            it[itemIds] = listToJson(user.itemIds)
            it[featureIds] = listToJson(user.featureIds)
            it[imageUrl] = user.imageUrl
            it[currentLocationId] = user.currentLocationId
            it[characterClassId] = user.characterClassId
            it[classGenerationStartedAt] = user.classGenerationStartedAt
            it[createdAt] = user.createdAt
            it[lastActiveAt] = user.lastActiveAt
            it[level] = user.level
            it[experience] = user.experience
            it[maxHp] = user.maxHp
            it[currentHp] = user.currentHp
            it[maxMana] = user.maxMana
            it[currentMana] = user.currentMana
            it[maxStamina] = user.maxStamina
            it[currentStamina] = user.currentStamina
            it[currentCombatSessionId] = user.currentCombatSessionId
            it[strength] = user.strength
            it[dexterity] = user.dexterity
            it[constitution] = user.constitution
            it[intelligence] = user.intelligence
            it[wisdom] = user.wisdom
            it[charisma] = user.charisma
            it[attributeQualityBonus] = user.attributeQualityBonus
            it[attributesGeneratedAt] = user.attributesGeneratedAt
            it[gold] = user.gold
            it[equippedItemIds] = listToJson(user.equippedItemIds)
            it[learnedAbilityIds] = listToJson(user.learnedAbilityIds)
        }
        user
    }

    fun findAll(): List<User> = transaction {
        UserTable.selectAll().map { it.toUser() }
    }

    fun findById(id: String): User? = transaction {
        UserTable.selectAll()
            .where { UserTable.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findByName(name: String): User? = transaction {
        UserTable.selectAll()
            .map { it.toUser() }
            .find { it.name.equals(name, ignoreCase = true) }
    }

    fun update(user: User): Boolean = transaction {
        UserTable.update({ UserTable.id eq user.id }) {
            it[name] = user.name
            it[desc] = user.desc
            it[itemIds] = listToJson(user.itemIds)
            it[featureIds] = listToJson(user.featureIds)
            it[imageUrl] = user.imageUrl
            it[currentLocationId] = user.currentLocationId
            it[characterClassId] = user.characterClassId
            it[lastActiveAt] = user.lastActiveAt
            it[level] = user.level
            it[experience] = user.experience
            it[maxHp] = user.maxHp
            it[currentHp] = user.currentHp
            it[maxMana] = user.maxMana
            it[currentMana] = user.currentMana
            it[maxStamina] = user.maxStamina
            it[currentStamina] = user.currentStamina
            it[currentCombatSessionId] = user.currentCombatSessionId
            it[strength] = user.strength
            it[dexterity] = user.dexterity
            it[constitution] = user.constitution
            it[intelligence] = user.intelligence
            it[wisdom] = user.wisdom
            it[charisma] = user.charisma
            it[attributeQualityBonus] = user.attributeQualityBonus
            it[attributesGeneratedAt] = user.attributesGeneratedAt
            it[gold] = user.gold
            it[equippedItemIds] = listToJson(user.equippedItemIds)
            it[learnedAbilityIds] = listToJson(user.learnedAbilityIds)
        } > 0
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[UserTable.imageUrl] = imageUrl
        } > 0
    }

    fun updateLastActiveAt(id: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    fun updateCurrentLocation(id: String, locationId: String?): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[currentLocationId] = locationId
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    fun updateCharacterClass(id: String, classId: String?): Boolean = transaction {
        // Get user and class to recalculate max resources
        val user = findById(id) ?: return@transaction false
        val characterClass = classId?.let { CharacterClassRepository.findById(it) }

        // Recalculate max resources based on new class
        val updatedUser = user.copy(characterClassId = classId)
        val newMaxHp = calculateMaxHp(updatedUser, characterClass)
        val newMaxMana = calculateMaxMana(updatedUser, characterClass)
        val newMaxStamina = calculateMaxStamina(updatedUser, characterClass)

        UserTable.update({ UserTable.id eq id }) {
            it[characterClassId] = classId
            it[classGenerationStartedAt] = null // Clear generation status when class is assigned
            it[maxHp] = newMaxHp
            it[currentHp] = newMaxHp // Full heal on class change
            it[maxMana] = newMaxMana
            it[currentMana] = newMaxMana
            it[maxStamina] = newMaxStamina
            it[currentStamina] = newMaxStamina
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    fun startClassGeneration(id: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[classGenerationStartedAt] = System.currentTimeMillis()
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    fun clearClassGeneration(id: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[classGenerationStartedAt] = null
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Find all users who are currently at a specific location and have been active recently
     */
    fun findActiveUsersAtLocation(locationId: String, activeThresholdMs: Long = 30000): List<User> = transaction {
        val cutoff = System.currentTimeMillis() - activeThresholdMs
        UserTable.selectAll()
            .where { (UserTable.currentLocationId eq locationId) }
            .map { it.toUser() }
            .filter { it.lastActiveAt >= cutoff }
    }

    /**
     * Update user's combat state
     */
    fun updateCombatState(id: String, newCurrentHp: Int, newCombatSessionId: String?): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[currentHp] = newCurrentHp
            it[currentCombatSessionId] = newCombatSessionId
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Award experience and handle level ups
     */
    fun awardExperience(id: String, expGained: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }
        val newExp = user.experience + expGained
        val newLevel = calculateLevel(newExp)

        // Recalculate all max resources with new level
        val updatedUser = user.copy(level = newLevel)
        val newMaxHp = calculateMaxHp(updatedUser, characterClass)
        val newMaxMana = calculateMaxMana(updatedUser, characterClass)
        val newMaxStamina = calculateMaxStamina(updatedUser, characterClass)

        UserTable.update({ UserTable.id eq id }) {
            it[experience] = newExp
            it[level] = newLevel
            it[maxHp] = newMaxHp
            it[maxMana] = newMaxMana
            it[maxStamina] = newMaxStamina
            // Full restore on level up
            if (newLevel > user.level) {
                it[currentHp] = newMaxHp
                it[currentMana] = newMaxMana
                it[currentStamina] = newMaxStamina
            }
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Fully heal a user (restores HP, mana, and stamina)
     * Includes equipment bonuses for effective max HP calculation
     */
    fun healToFull(id: String): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        // Calculate effective max HP including equipment bonuses
        val equippedItems = user.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
        val equipHpBonus = equippedItems.sumOf { it.statBonuses?.maxHp ?: 0 }
        val effectiveMaxHp = user.maxHp + equipHpBonus

        UserTable.update({ UserTable.id eq id }) {
            it[currentHp] = effectiveMaxHp
            it[currentMana] = user.maxMana
            it[currentStamina] = user.maxStamina
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Set a user's level directly (admin function).
     * Updates experience to match the level and recalculates all stats.
     */
    fun setLevel(id: String, newLevel: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }

        // Calculate experience needed for this level
        val expNeeded = (newLevel - 1) * 100

        // Recalculate all max resources with new level
        val updatedUser = user.copy(level = newLevel)
        val newMaxHp = calculateMaxHp(updatedUser, characterClass)
        val newMaxMana = calculateMaxMana(updatedUser, characterClass)
        val newMaxStamina = calculateMaxStamina(updatedUser, characterClass)

        UserTable.update({ UserTable.id eq id }) {
            it[experience] = expNeeded
            it[level] = newLevel
            it[maxHp] = newMaxHp
            it[maxMana] = newMaxMana
            it[maxStamina] = newMaxStamina
            // Full restore on level set
            it[currentHp] = newMaxHp
            it[currentMana] = newMaxMana
            it[currentStamina] = newMaxStamina
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Find all users currently in a combat session
     */
    fun findByCombatSession(sessionId: String): List<User> = transaction {
        UserTable.selectAll()
            .where { UserTable.currentCombatSessionId eq sessionId }
            .map { it.toUser() }
    }

    /**
     * Find all users who have been active within the specified time window.
     * @param withinMs Maximum milliseconds since last activity
     */
    fun findRecentlyActive(withinMs: Long): List<User> = transaction {
        val cutoffTime = System.currentTimeMillis() - withinMs
        UserTable.selectAll()
            .where { UserTable.lastActiveAt greater cutoffTime }
            .map { it.toUser() }
    }

    fun delete(id: String): Boolean = transaction {
        UserTable.deleteWhere { UserTable.id eq id } > 0
    }

    /**
     * Ensure all users have at least the specified minimum gold.
     * Returns the number of users who received gold.
     */
    fun ensureMinimumGold(minimumGold: Int): Int = transaction {
        val users = findAll()
        var count = 0
        users.filter { it.gold < minimumGold }.forEach { user ->
            val needed = minimumGold - user.gold
            UserTable.update({ UserTable.id eq user.id }) {
                it[gold] = minimumGold
            }
            count++
        }
        count
    }

    /**
     * Ensure all users have a starting location.
     * Users without a currentLocationId are assigned the default starting location.
     * Returns the number of users who were assigned a location.
     */
    fun ensureStartingLocation(defaultLocationId: String): Int = transaction {
        val users = findAll()
        var count = 0
        users.filter { it.currentLocationId == null }.forEach { user ->
            UserTable.update({ UserTable.id eq user.id }) {
                it[currentLocationId] = defaultLocationId
            }
            count++
        }
        count
    }

    /**
     * Add gold to a user's balance
     */
    fun addGold(id: String, amount: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        UserTable.update({ UserTable.id eq id }) {
            it[gold] = user.gold + amount
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Add items to a user's inventory
     */
    fun addItems(id: String, itemIds: List<String>): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        UserTable.update({ UserTable.id eq id }) {
            it[UserTable.itemIds] = listToJson(user.itemIds + itemIds)
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Remove specific items from a user's inventory
     */
    fun removeItems(id: String, itemIdsToRemove: List<String>): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val removeSet = itemIdsToRemove.toMutableList()
        val newItemIds = user.itemIds.filter { itemId ->
            if (itemId in removeSet) {
                removeSet.remove(itemId)
                false
            } else {
                true
            }
        }
        UserTable.update({ UserTable.id eq id }) {
            it[UserTable.itemIds] = listToJson(newItemIds)
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Clear all items from a user's inventory
     * Returns the list of item IDs that were removed
     */
    fun clearInventory(id: String): List<String> = transaction {
        val user = findById(id) ?: return@transaction emptyList()
        val itemIds = user.itemIds.toList()
        UserTable.update({ UserTable.id eq id }) {
            it[UserTable.itemIds] = listToJson(emptyList())
            it[equippedItemIds] = listToJson(emptyList())
            it[lastActiveAt] = System.currentTimeMillis()
        }
        itemIds
    }

    /**
     * Equip an item (must be in inventory)
     */
    fun equipItem(id: String, itemId: String): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        if (itemId !in user.itemIds) return@transaction false
        if (itemId in user.equippedItemIds) return@transaction true // Already equipped
        UserTable.update({ UserTable.id eq id }) {
            it[equippedItemIds] = listToJson(user.equippedItemIds + itemId)
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Unequip an item
     */
    fun unequipItem(id: String, itemId: String): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        if (itemId !in user.equippedItemIds) return@transaction true // Already unequipped
        UserTable.update({ UserTable.id eq id }) {
            it[equippedItemIds] = listToJson(user.equippedItemIds - itemId)
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Learn an ability from a trainer
     * Returns true if the ability was learned (or already known)
     */
    fun learnAbility(id: String, abilityId: String): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        if (abilityId in user.learnedAbilityIds) return@transaction true // Already learned
        UserTable.update({ UserTable.id eq id }) {
            it[learnedAbilityIds] = listToJson(user.learnedAbilityIds + abilityId)
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Check if a user has learned a specific ability
     */
    fun hasLearnedAbility(id: String, abilityId: String): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        abilityId in user.learnedAbilityIds
    }

    /**
     * Get all abilities a user has learned
     */
    fun getLearnedAbilities(id: String): List<Ability> = transaction {
        val user = findById(id) ?: return@transaction emptyList()
        user.learnedAbilityIds.mapNotNull { AbilityRepository.findById(it) }
    }

    /**
     * Get abilities a user can use in combat:
     * - Must have learned the ability from a trainer
     * - Must meet the minLevel requirement
     */
    fun getUsableAbilities(id: String): List<Ability> = transaction {
        val user = findById(id) ?: return@transaction emptyList()
        user.learnedAbilityIds
            .mapNotNull { AbilityRepository.findById(it) }
            .filter { it.minLevel <= user.level }
    }

    /**
     * Spend gold (returns false if insufficient)
     */
    fun spendGold(id: String, amount: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        if (user.gold < amount) return@transaction false
        UserTable.update({ UserTable.id eq id }) {
            it[gold] = user.gold - amount
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Spend mana (returns false if insufficient)
     */
    fun spendMana(id: String, amount: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        if (user.currentMana < amount) return@transaction false
        UserTable.update({ UserTable.id eq id }) {
            it[currentMana] = user.currentMana - amount
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Spend stamina (returns false if insufficient)
     */
    fun spendStamina(id: String, amount: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        if (user.currentStamina < amount) return@transaction false
        UserTable.update({ UserTable.id eq id }) {
            it[currentStamina] = user.currentStamina - amount
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Heal HP (capped at base max - use healWithEquipment for equipment-aware healing)
     */
    fun heal(id: String, amount: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val newHp = (user.currentHp + amount).coerceAtMost(user.maxHp)
        UserTable.update({ UserTable.id eq id }) {
            it[currentHp] = newHp
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Heal HP respecting equipment bonuses (capped at effective max HP)
     * Used by regen system to allow healing up to equipment-boosted max
     */
    fun healWithEquipment(id: String, amount: Int, effectiveMaxHp: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val newHp = (user.currentHp + amount).coerceAtMost(effectiveMaxHp)
        UserTable.update({ UserTable.id eq id }) {
            it[currentHp] = newHp
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Restore mana (capped at max)
     */
    fun restoreMana(id: String, amount: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val newMana = (user.currentMana + amount).coerceAtMost(user.maxMana)
        UserTable.update({ UserTable.id eq id }) {
            it[currentMana] = newMana
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Restore stamina (capped at max)
     */
    fun restoreStamina(id: String, amount: Int): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val newStamina = (user.currentStamina + amount).coerceAtMost(user.maxStamina)
        UserTable.update({ UserTable.id eq id }) {
            it[currentStamina] = newStamina
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Restore all resources to full (HP, Mana, Stamina)
     */
    fun restoreAllResources(id: String): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        UserTable.update({ UserTable.id eq id }) {
            it[currentHp] = user.maxHp
            it[currentMana] = user.maxMana
            it[currentStamina] = user.maxStamina
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Update max resource values (called on level up or class assignment)
     */
    fun updateMaxResources(id: String, newMaxHp: Int, newMaxMana: Int, newMaxStamina: Int): Boolean = transaction {
        UserTable.update({ UserTable.id eq id }) {
            it[maxHp] = newMaxHp
            it[maxMana] = newMaxMana
            it[maxStamina] = newMaxStamina
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Recalculate max resources based on current stats, level, and class.
     * Useful for fixing existing characters whose resources weren't calculated properly.
     * Optionally restores current resources to max.
     */
    fun recalculateMaxResources(id: String, restoreToFull: Boolean = false): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }

        val newMaxHp = calculateMaxHp(user, characterClass)
        val newMaxMana = calculateMaxMana(user, characterClass)
        val newMaxStamina = calculateMaxStamina(user, characterClass)

        UserTable.update({ UserTable.id eq id }) {
            it[maxHp] = newMaxHp
            it[maxMana] = newMaxMana
            it[maxStamina] = newMaxStamina
            if (restoreToFull) {
                it[currentHp] = newMaxHp
                it[currentMana] = newMaxMana
                it[currentStamina] = newMaxStamina
            }
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    /**
     * Update D&D attributes and recalculate max resources
     */
    fun updateAttributes(
        id: String,
        strength: Int, dexterity: Int, constitution: Int,
        intelligence: Int, wisdom: Int, charisma: Int,
        qualityBonus: Int
    ): Boolean = transaction {
        val user = findById(id) ?: return@transaction false
        val characterClass = user.characterClassId?.let { CharacterClassRepository.findById(it) }

        val updatedUser = user.copy(
            strength = strength, dexterity = dexterity, constitution = constitution,
            intelligence = intelligence, wisdom = wisdom, charisma = charisma
        )
        val newMaxHp = calculateMaxHp(updatedUser, characterClass)
        val newMaxMana = calculateMaxMana(updatedUser, characterClass)
        val newMaxStamina = calculateMaxStamina(updatedUser, characterClass)

        UserTable.update({ UserTable.id eq id }) {
            it[UserTable.strength] = strength
            it[UserTable.dexterity] = dexterity
            it[UserTable.constitution] = constitution
            it[UserTable.intelligence] = intelligence
            it[UserTable.wisdom] = wisdom
            it[UserTable.charisma] = charisma
            it[attributeQualityBonus] = qualityBonus
            it[attributesGeneratedAt] = System.currentTimeMillis()
            it[maxHp] = newMaxHp
            it[currentHp] = newMaxHp // Full heal on attribute assignment
            it[maxMana] = newMaxMana
            it[currentMana] = newMaxMana
            it[maxStamina] = newMaxStamina
            it[currentStamina] = newMaxStamina
            it[lastActiveAt] = System.currentTimeMillis()
        } > 0
    }

    // Level calculation: every 100 exp = 1 level
    private fun calculateLevel(experience: Int): Int = (experience / 100) + 1

    // D&D attribute modifier: (stat - 10) / 2
    fun attributeModifier(stat: Int): Int = (stat - 10) / 2

    // Max HP: (hitDie + CON mod) at level 1, then (hitDie/2+1 + CON mod) per level
    fun calculateMaxHp(user: User, characterClass: CharacterClass?): Int {
        val conMod = attributeModifier(user.constitution)
        val hitDie = characterClass?.hitDie ?: 8
        val baseHp = hitDie + conMod
        val perLevelHp = (hitDie / 2 + 1) + conMod
        return (baseHp + (user.level - 1) * perLevelHp).coerceAtLeast(1)
    }

    // Max Mana: baseMana + spellcastingMod*2 + level*2
    fun calculateMaxMana(user: User, characterClass: CharacterClass?): Int {
        val baseMana = characterClass?.baseMana ?: 10
        val spellMod = when (characterClass?.primaryAttribute) {
            "intelligence" -> attributeModifier(user.intelligence)
            "wisdom" -> attributeModifier(user.wisdom)
            "charisma" -> attributeModifier(user.charisma)
            else -> 0
        }
        return (baseMana + (spellMod * 2) + (user.level * 2)).coerceAtLeast(0)
    }

    // Max Stamina: baseStamina + avgPhysicalMod*2 + level*2
    fun calculateMaxStamina(user: User, characterClass: CharacterClass?): Int {
        val strMod = attributeModifier(user.strength)
        val dexMod = attributeModifier(user.dexterity)
        val conMod = attributeModifier(user.constitution)
        val avgPhysicalMod = (strMod + dexMod + conMod) / 3
        val baseStamina = characterClass?.baseStamina ?: 10
        return (baseStamina + (avgPhysicalMod * 2) + (user.level * 2)).coerceAtLeast(0)
    }

    // Combat stat calculations (used by CombatService.toCombatant)
    fun calculateAccuracy(user: User, equipmentAttackBonus: Int = 0): Int {
        val encumbranceInfo = com.ez2bg.anotherthread.game.EncumbranceService.getEncumbranceInfo(user)
        return attributeModifier(user.dexterity) + user.level / 2 + equipmentAttackBonus + encumbranceInfo.attackModifier
    }

    fun calculateEvasion(user: User, equipmentDefenseBonus: Int = 0): Int {
        // Evasion scales with DEX modifier, level, and equipment
        // Level adds +1 evasion per 2 levels to match creature accuracy scaling
        val baseEvasion = attributeModifier(user.dexterity) + (user.level / 2) + equipmentDefenseBonus
        // Apply encumbrance dodge penalty (percentage reduction)
        val encumbranceInfo = com.ez2bg.anotherthread.game.EncumbranceService.getEncumbranceInfo(user)
        val dodgePenaltyMultiplier = (100 + encumbranceInfo.dodgeModifier) / 100.0
        return (baseEvasion * dodgePenaltyMultiplier).toInt()
    }

    fun calculateCritBonus(user: User): Int {
        return attributeModifier(user.charisma) + user.level / 5
    }

    fun calculateBaseDamage(user: User, equipmentAttackBonus: Int = 0): Int {
        return (5 + user.level + attributeModifier(user.strength) + equipmentAttackBonus).coerceAtLeast(1)
    }

    /**
     * Calculate attacks per round (MajorMUD-style).
     * Formula: 1 base + (level / 5) + (DEX modifier / 2)
     * - Level 1-4: 1 attack
     * - Level 5-9: 2 attacks (with DEX 10)
     * - Level 10-14: 3 attacks (with DEX 10)
     * - High DEX adds bonus attacks
     * Maximum capped at 5 attacks per round.
     */
    fun calculateAttacksPerRound(user: User): Int {
        val levelBonus = user.level / 5
        val dexBonus = attributeModifier(user.dexterity) / 2
        return (1 + levelBonus + dexBonus).coerceIn(1, 5)
    }

    // Legacy HP calculation for backward compat
    private fun calculateMaxHp(level: Int, classId: String?): Int {
        val hitDieAverage = 5
        return 10 + (level * hitDieAverage)
    }

    // Password hashing utilities
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }

    /**
     * Generate an appearance description based on equipped items.
     * Returns a phrase like "heavily armored" or "lightly equipped" based on equipment.
     */
    fun generateAppearanceDescription(user: User): String {
        val equippedItems = user.equippedItemIds.mapNotNull { ItemRepository.findById(it) }
        if (equippedItems.isEmpty()) return "unarmed and unarmored"

        val descriptors = mutableListOf<String>()

        // Check for weapons
        val weapons = equippedItems.filter { it.equipmentType == "weapon" }
        val mainHand = weapons.find { it.equipmentSlot == "main_hand" }
        val offHand = weapons.find { it.equipmentSlot == "off_hand" }

        when {
            mainHand != null && offHand != null -> descriptors.add("dual-wielding")
            mainHand != null -> descriptors.add("armed with ${mainHand.name.lowercase()}")
            offHand != null -> descriptors.add("carrying ${offHand.name.lowercase()}")
        }

        // Check for armor pieces
        val armorPieces = equippedItems.filter { it.equipmentType == "armor" }
        val armorSlots = armorPieces.mapNotNull { it.equipmentSlot }.toSet()
        val totalDefense = armorPieces.sumOf { it.statBonuses?.defense ?: 0 }

        val armorDescription = when {
            armorSlots.containsAll(listOf("head", "chest", "legs", "feet")) -> "fully armored"
            totalDefense >= 15 -> "heavily armored"
            totalDefense >= 10 -> "well-armored"
            totalDefense >= 5 -> "lightly armored"
            armorSlots.contains("chest") -> "wearing ${armorPieces.find { it.equipmentSlot == "chest" }?.name?.lowercase() ?: "armor"}"
            armorPieces.isNotEmpty() -> "partially armored"
            else -> null
        }
        if (armorDescription != null) descriptors.add(armorDescription)

        // Check for accessories
        val accessories = equippedItems.filter { it.equipmentType == "accessory" }
        if (accessories.isNotEmpty()) {
            val accessoryNames = accessories.take(2).joinToString(" and ") { it.name.lowercase() }
            descriptors.add("adorned with $accessoryNames")
        }

        return if (descriptors.isEmpty()) {
            "minimally equipped"
        } else {
            descriptors.joinToString(", ")
        }
    }
}
