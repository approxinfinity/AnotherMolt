package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.combat.*
import com.ez2bg.anotherthread.database.*

/**
 * Stock test fixtures for unit tests.
 * Provides consistent, reusable test data for combat, abilities, users, and creatures.
 */
object TestFixtures {

    // ========== Stock User IDs ==========
    const val PLAYER_1_ID = "test-player-1"
    const val PLAYER_2_ID = "test-player-2"
    const val ADMIN_USER_ID = "test-admin"

    // ========== Stock Creature IDs ==========
    const val GOBLIN_ID = "test-goblin"
    const val ORC_ID = "test-orc"
    const val DRAGON_ID = "test-dragon"

    // ========== Stock Location IDs ==========
    const val HOME_LOCATION_ID = "test-home"
    const val DUNGEON_ENTRANCE_ID = "test-dungeon-entrance"
    const val DUNGEON_ROOM_ID = "test-dungeon-room"

    // ========== Stock Item IDs ==========
    const val SWORD_ID = "test-sword"
    const val POTION_ID = "test-potion"
    const val SHIELD_ID = "test-shield"

    // ========== Stock Ability IDs ==========
    const val BASIC_ATTACK_ID = "test-basic-attack"
    const val UNIVERSAL_BASIC_ATTACK_ID = "universal-basic-attack"  // Has baseDamage = 0, uses actor's baseDamage
    const val FIREBALL_ID = "test-fireball"
    const val HEAL_SELF_ID = "test-heal-self"
    const val HEAL_OTHER_ID = "test-heal-other"
    const val POISON_DOT_ID = "test-poison-dot"
    const val BUFF_STRENGTH_ID = "test-buff-strength"
    const val DEBUFF_WEAKNESS_ID = "test-debuff-weakness"
    const val STUN_ID = "test-stun"
    const val HOT_REGEN_ID = "test-hot-regen"

    // ========== Stock Character Class IDs ==========
    const val WARRIOR_CLASS_ID = "test-warrior-class"
    const val HEALER_CLASS_ID = "test-healer-class"
    const val MAGE_CLASS_ID = "test-mage-class"

    // ========== Stock Abilities ==========

    /**
     * Basic attack ability - deals 10 base damage to single enemy.
     */
    fun basicAttack() = Ability(
        id = BASIC_ATTACK_ID,
        name = "Basic Attack",
        description = "A simple attack that deals damage.",
        classId = WARRIOR_CLASS_ID,
        abilityType = "combat",
        targetType = "single_enemy",
        range = 5,
        cooldownType = "none",
        cooldownRounds = 0,
        baseDamage = 10,
        durationRounds = 0,
        effects = "[]"
    )

    /**
     * Universal basic attack - has baseDamage = 0, should use combatant's baseDamage stat.
     * This mimics the real universal-basic-attack in the game.
     */
    fun universalBasicAttack() = Ability(
        id = UNIVERSAL_BASIC_ATTACK_ID,
        name = "Attack",
        description = "A basic physical attack using your weapon.",
        classId = null,  // Available to all
        abilityType = "item",  // Use "item" to preserve manual stamina cost (matches production seed)
        targetType = "single_enemy",
        range = 5,
        cooldownType = "none",
        cooldownRounds = 0,
        baseDamage = 0,  // Damage comes from combatant's baseDamage stat
        durationRounds = 0,
        effects = """[{"type":"damage","source":"baseDamage"}]""",
        staminaCost = 2
    )

    /**
     * Fireball - area fire spell with base damage.
     */
    fun fireball() = Ability(
        id = FIREBALL_ID,
        name = "Fireball",
        description = "Hurls a ball of fire at enemies.",
        classId = MAGE_CLASS_ID,
        abilityType = "spell",
        targetType = "area",
        range = 60,
        cooldownType = "medium",
        cooldownRounds = 3,
        baseDamage = 25,
        durationRounds = 0,
        manaCost = 8,
        effects = "[]"
    )

    /**
     * Self-healing ability - heals 15 HP to self.
     */
    fun healSelf() = Ability(
        id = HEAL_SELF_ID,
        name = "Heal Self",
        description = "Restore health to yourself.",
        classId = HEALER_CLASS_ID,
        abilityType = "spell",
        targetType = "self",
        range = 0,
        cooldownType = "short",
        cooldownRounds = 2,
        baseDamage = 15, // Used as heal amount
        durationRounds = 0,
        effects = """["heal"]"""
    )

    /**
     * Targeted healing ability - heals 20 HP to single ally.
     */
    fun healOther() = Ability(
        id = HEAL_OTHER_ID,
        name = "Heal Other",
        description = "Restore health to an ally.",
        classId = HEALER_CLASS_ID,
        abilityType = "spell",
        targetType = "single_ally",
        range = 30,
        cooldownType = "medium",
        cooldownRounds = 3,
        baseDamage = 20, // Used as heal amount
        durationRounds = 0,
        effects = """["heal"]"""
    )

    /**
     * Poison DoT ability - deals 8 damage + applies 5 damage/round for 3 rounds.
     */
    fun poisonDot() = Ability(
        id = POISON_DOT_ID,
        name = "Poison Strike",
        description = "A poisonous attack that deals damage over time.",
        classId = MAGE_CLASS_ID,
        abilityType = "combat",
        targetType = "single_enemy",
        range = 5,
        cooldownType = "medium",
        cooldownRounds = 3,
        baseDamage = 8,
        durationRounds = 3,
        effects = """["dot"]"""
    )

    /**
     * Strength buff ability - applies buff for 3 rounds.
     */
    fun buffStrength() = Ability(
        id = BUFF_STRENGTH_ID,
        name = "Battle Cry",
        description = "Empower yourself with increased strength.",
        classId = WARRIOR_CLASS_ID,
        abilityType = "combat",
        targetType = "self",
        range = 0,
        cooldownType = "long",
        cooldownRounds = 5,
        baseDamage = 0,
        durationRounds = 3,
        effects = """["buff"]"""
    )

    /**
     * Weakness debuff ability - applies debuff for 2 rounds.
     */
    fun debuffWeakness() = Ability(
        id = DEBUFF_WEAKNESS_ID,
        name = "Weaken",
        description = "Reduce an enemy's combat effectiveness.",
        classId = MAGE_CLASS_ID,
        abilityType = "spell",
        targetType = "single_enemy",
        range = 30,
        cooldownType = "medium",
        cooldownRounds = 3,
        baseDamage = 0,
        durationRounds = 2,
        effects = """["debuff"]"""
    )

    /**
     * Stun ability - stuns target for 1 round.
     */
    fun stunAbility() = Ability(
        id = STUN_ID,
        name = "Stunning Blow",
        description = "A powerful strike that stuns the target.",
        classId = WARRIOR_CLASS_ID,
        abilityType = "combat",
        targetType = "single_enemy",
        range = 5,
        cooldownType = "long",
        cooldownRounds = 5,
        baseDamage = 5,
        durationRounds = 1,
        effects = """["stun"]"""
    )

    /**
     * Heal over time ability - heals 5 HP/round for 3 rounds.
     */
    fun hotRegen() = Ability(
        id = HOT_REGEN_ID,
        name = "Regeneration",
        description = "Apply a healing effect that restores health over time.",
        classId = HEALER_CLASS_ID,
        abilityType = "spell",
        targetType = "self",
        range = 0,
        cooldownType = "medium",
        cooldownRounds = 3,
        baseDamage = 0,
        durationRounds = 3,
        effects = """["hot"]"""
    )

    /**
     * Get all stock abilities as a list.
     */
    fun allAbilities(): List<Ability> = listOf(
        basicAttack(),
        universalBasicAttack(),
        fireball(),
        healSelf(),
        healOther(),
        poisonDot(),
        buffStrength(),
        debuffWeakness(),
        stunAbility(),
        hotRegen()
    )

    // ========== Stock Character Classes ==========

    fun warriorClass() = CharacterClass(
        id = WARRIOR_CLASS_ID,
        name = "Test Warrior",
        description = "A test warrior class for unit tests.",
        isSpellcaster = false,
        hitDie = 10,
        primaryAttribute = "strength",
        isPublic = true
    )

    fun healerClass() = CharacterClass(
        id = HEALER_CLASS_ID,
        name = "Test Healer",
        description = "A test healer class for unit tests.",
        isSpellcaster = true,
        hitDie = 6,
        primaryAttribute = "wisdom",
        isPublic = true
    )

    fun mageClass() = CharacterClass(
        id = MAGE_CLASS_ID,
        name = "Test Mage",
        description = "A test mage class for unit tests.",
        isSpellcaster = true,
        hitDie = 6,
        primaryAttribute = "intelligence",
        isPublic = true
    )

    fun allCharacterClasses(): List<CharacterClass> = listOf(
        warriorClass(),
        healerClass(),
        mageClass()
    )

    // ========== Stock Items ==========

    fun sword() = Item(
        id = SWORD_ID,
        name = "Test Sword",
        desc = "A simple test sword",
        featureIds = emptyList(),
        equipmentType = "weapon",
        equipmentSlot = "main_hand",
        value = 50
    )

    fun potion() = Item(
        id = POTION_ID,
        name = "Test Potion",
        desc = "A healing potion for testing",
        featureIds = emptyList(),
        value = 25,
        isStackable = true
    )

    fun shield() = Item(
        id = SHIELD_ID,
        name = "Test Shield",
        desc = "A simple test shield",
        featureIds = emptyList(),
        equipmentType = "armor",
        equipmentSlot = "off_hand",
        value = 40
    )

    fun allItems(): List<Item> = listOf(
        sword(),
        potion(),
        shield()
    )

    // ========== Stock Users ==========

    fun player1(
        currentHp: Int = 30,
        maxHp: Int = 30,
        itemIds: List<String> = emptyList(),
        gold: Int = 0,
        currentLocationId: String? = null,
        equippedItemIds: List<String> = emptyList()
    ) = User(
        id = PLAYER_1_ID,
        name = "TestPlayer1",
        passwordHash = "test-hash",
        desc = "A test player",
        currentHp = currentHp,
        maxHp = maxHp,
        characterClassId = WARRIOR_CLASS_ID,
        itemIds = itemIds,
        gold = gold,
        currentLocationId = currentLocationId,
        equippedItemIds = equippedItemIds
    )

    fun player2(
        currentHp: Int = 25,
        maxHp: Int = 25,
        itemIds: List<String> = emptyList(),
        gold: Int = 0,
        currentLocationId: String? = null
    ) = User(
        id = PLAYER_2_ID,
        name = "TestPlayer2",
        passwordHash = "test-hash",
        desc = "Another test player",
        currentHp = currentHp,
        maxHp = maxHp,
        characterClassId = HEALER_CLASS_ID,
        itemIds = itemIds,
        gold = gold,
        currentLocationId = currentLocationId
    )

    // ========== Stock Creatures ==========

    fun goblin(currentHp: Int = 15, maxHp: Int = 15) = Creature(
        id = GOBLIN_ID,
        name = "Test Goblin",
        desc = "A weak goblin for testing",
        itemIds = emptyList(),
        featureIds = emptyList(),
        maxHp = maxHp,
        baseDamage = 3,
        level = 1,
        experienceValue = 10,
        isAggressive = true
    )

    fun orc(currentHp: Int = 30, maxHp: Int = 30) = Creature(
        id = ORC_ID,
        name = "Test Orc",
        desc = "A stronger orc for testing",
        itemIds = emptyList(),
        featureIds = emptyList(),
        maxHp = maxHp,
        baseDamage = 8,
        level = 3,
        experienceValue = 30,
        isAggressive = true
    )

    fun dragon(currentHp: Int = 100, maxHp: Int = 100) = Creature(
        id = DRAGON_ID,
        name = "Test Dragon",
        desc = "A powerful dragon for testing",
        itemIds = emptyList(),
        featureIds = emptyList(),
        maxHp = maxHp,
        baseDamage = 20,
        level = 10,
        experienceValue = 200,
        isAggressive = true
    )

    // ========== Stock Locations ==========

    /**
     * Home location at (0,0) in overworld - where players respawn on death.
     */
    fun homeLocation() = Location(
        id = HOME_LOCATION_ID,
        name = "Town Square",
        desc = "The central square of town. A safe place to rest.",
        creatureIds = emptyList(),
        exits = listOf(Exit(DUNGEON_ENTRANCE_ID, ExitDirection.NORTH)),
        itemIds = emptyList(),
        featureIds = emptyList(),
        gridX = 0,
        gridY = 0,
        areaId = "overworld"
    )

    fun dungeonEntrance() = Location(
        id = DUNGEON_ENTRANCE_ID,
        name = "Dungeon Entrance",
        desc = "The entrance to a dark dungeon.",
        creatureIds = listOf(GOBLIN_ID),
        exits = listOf(Exit(DUNGEON_ROOM_ID, ExitDirection.NORTH), Exit(HOME_LOCATION_ID, ExitDirection.SOUTH)),
        itemIds = emptyList(),
        featureIds = emptyList(),
        gridX = 0,
        gridY = 1,
        areaId = "overworld"
    )

    fun dungeonRoom() = Location(
        id = DUNGEON_ROOM_ID,
        name = "Dungeon Room",
        desc = "A dark room deep in the dungeon.",
        creatureIds = listOf(ORC_ID),
        exits = listOf(Exit(DUNGEON_ENTRANCE_ID, ExitDirection.SOUTH)),
        itemIds = emptyList(),
        featureIds = emptyList(),
        gridX = 0,
        gridY = 2,
        areaId = "overworld"
    )

    // ========== Stock Combatants ==========

    /**
     * Create a player combatant with configurable stats.
     */
    fun playerCombatant(
        id: String = PLAYER_1_ID,
        name: String = "TestPlayer",
        currentHp: Int = 30,
        maxHp: Int = 30,
        currentMana: Int = 10,
        maxMana: Int = 10,
        currentStamina: Int = 10,
        maxStamina: Int = 10,
        initiative: Int = 10,
        characterClassId: String = WARRIOR_CLASS_ID,
        abilityIds: List<String> = listOf(BASIC_ATTACK_ID, HEAL_SELF_ID),
        statusEffects: List<StatusEffect> = emptyList(),
        cooldowns: Map<String, Int> = emptyMap(),
        level: Int = 1,
        accuracy: Int = 0,
        evasion: Int = 0,
        critBonus: Int = 0,
        baseDamage: Int = 5
    ) = Combatant(
        id = id,
        type = CombatantType.PLAYER,
        name = name,
        currentHp = currentHp,
        maxHp = maxHp,
        currentMana = currentMana,
        maxMana = maxMana,
        currentStamina = currentStamina,
        maxStamina = maxStamina,
        initiative = initiative,
        characterClassId = characterClassId,
        abilityIds = abilityIds,
        statusEffects = statusEffects,
        cooldowns = cooldowns,
        isAlive = currentHp > 0,
        level = level,
        accuracy = accuracy,
        evasion = evasion,
        critBonus = critBonus,
        baseDamage = baseDamage
    )

    /**
     * Create a creature combatant with configurable stats.
     */
    fun creatureCombatant(
        id: String = GOBLIN_ID,
        name: String = "TestGoblin",
        currentHp: Int = 15,
        maxHp: Int = 15,
        currentMana: Int = 10,
        maxMana: Int = 10,
        currentStamina: Int = 10,
        maxStamina: Int = 10,
        initiative: Int = 5,
        abilityIds: List<String> = emptyList(),
        statusEffects: List<StatusEffect> = emptyList(),
        level: Int = 1,
        accuracy: Int = 2,
        evasion: Int = 1,
        critBonus: Int = 0,
        baseDamage: Int = 3
    ) = Combatant(
        id = id,
        type = CombatantType.CREATURE,
        name = name,
        currentHp = currentHp,
        maxHp = maxHp,
        currentMana = currentMana,
        maxMana = maxMana,
        currentStamina = currentStamina,
        maxStamina = maxStamina,
        initiative = initiative,
        abilityIds = abilityIds,
        statusEffects = statusEffects,
        isAlive = currentHp > 0,
        level = level,
        accuracy = accuracy,
        evasion = evasion,
        critBonus = critBonus,
        baseDamage = baseDamage
    )

    // ========== Stock Combat Sessions ==========

    /**
     * Create a combat session with one player vs one goblin.
     */
    fun simpleCombatSession(
        sessionId: String = "test-session-1",
        locationId: String = DUNGEON_ENTRANCE_ID,
        playerHp: Int = 30,
        goblinHp: Int = 15
    ) = CombatSession(
        id = sessionId,
        locationId = locationId,
        state = CombatState.ACTIVE,
        currentRound = 0,
        combatants = listOf(
            playerCombatant(currentHp = playerHp, maxHp = 30),
            creatureCombatant(currentHp = goblinHp, maxHp = 15)
        )
    )

    /**
     * Create a combat session with two players vs one orc.
     */
    fun groupCombatSession(
        sessionId: String = "test-session-2",
        locationId: String = DUNGEON_ROOM_ID
    ) = CombatSession(
        id = sessionId,
        locationId = locationId,
        state = CombatState.ACTIVE,
        currentRound = 0,
        combatants = listOf(
            playerCombatant(id = PLAYER_1_ID, name = "Warrior", currentHp = 30, maxHp = 30),
            playerCombatant(id = PLAYER_2_ID, name = "Healer", currentHp = 25, maxHp = 25, characterClassId = HEALER_CLASS_ID),
            creatureCombatant(id = ORC_ID, name = "Orc", currentHp = 30, maxHp = 30, initiative = 8)
        )
    )

    // ========== Stock Status Effects ==========

    fun dotEffect(
        name: String = "Poison",
        value: Int = 5,
        remainingRounds: Int = 3,
        sourceId: String = PLAYER_1_ID
    ) = StatusEffect(
        name = name,
        effectType = "dot",
        value = value,
        remainingRounds = remainingRounds,
        sourceId = sourceId
    )

    fun hotEffect(
        name: String = "Regeneration",
        value: Int = 5,
        remainingRounds: Int = 3,
        sourceId: String = PLAYER_1_ID
    ) = StatusEffect(
        name = name,
        effectType = "hot",
        value = value,
        remainingRounds = remainingRounds,
        sourceId = sourceId
    )

    fun buffEffect(
        name: String = "Empowered",
        value: Int = 3,
        remainingRounds: Int = 3,
        sourceId: String = PLAYER_1_ID
    ) = StatusEffect(
        name = name,
        effectType = "buff",
        value = value,
        remainingRounds = remainingRounds,
        sourceId = sourceId
    )

    fun debuffEffect(
        name: String = "Weakened",
        value: Int = -3,
        remainingRounds: Int = 2,
        sourceId: String = PLAYER_1_ID
    ) = StatusEffect(
        name = name,
        effectType = "debuff",
        value = value,
        remainingRounds = remainingRounds,
        sourceId = sourceId
    )

    fun stunEffect(
        remainingRounds: Int = 1,
        sourceId: String = PLAYER_1_ID
    ) = StatusEffect(
        name = "Stunned",
        effectType = "stun",
        value = 0,
        remainingRounds = remainingRounds,
        sourceId = sourceId
    )

    // ========== Stock Combat Actions ==========

    fun attackAction(
        combatantId: String = PLAYER_1_ID,
        targetId: String = GOBLIN_ID,
        abilityId: String = BASIC_ATTACK_ID
    ) = CombatAction(
        combatantId = combatantId,
        abilityId = abilityId,
        targetId = targetId
    )

    fun healSelfAction(
        combatantId: String = PLAYER_1_ID
    ) = CombatAction(
        combatantId = combatantId,
        abilityId = HEAL_SELF_ID,
        targetId = null
    )

    fun healOtherAction(
        combatantId: String = PLAYER_2_ID,
        targetId: String = PLAYER_1_ID
    ) = CombatAction(
        combatantId = combatantId,
        abilityId = HEAL_OTHER_ID,
        targetId = targetId
    )
}
