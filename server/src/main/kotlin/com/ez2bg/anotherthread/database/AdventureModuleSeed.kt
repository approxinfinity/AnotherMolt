package com.ez2bg.anotherthread.database

import org.slf4j.LoggerFactory

/**
 * Base interface for self-contained adventure modules.
 *
 * An AdventureModuleSeed contains EVERYTHING needed for a complete adventure:
 * - Creatures with their abilities and loot tables
 * - Items (equipment, consumables, quest items)
 * - Locations forming the map/dungeon
 * - Chests with loot
 * - Features and puzzles
 *
 * Each module is a single class that can be independently seeded to add
 * a complete adventure to the game. Modules are designed to be:
 * - Self-contained: All content is defined within the module
 * - Portable: Uses name-based lookups, not hardcoded UUIDs
 * - Idempotent: Can be run multiple times without creating duplicates
 *
 * Usage:
 * ```
 * object MyAdventureModule : AdventureModuleSeed() {
 *     override val moduleId = "my-adventure"
 *     override val moduleName = "My Epic Adventure"
 *     override val moduleDescription = "An adventure for levels 1-3"
 *     override val attribution = "Original D&D Module B1"
 *     override val recommendedLevelMin = 1
 *     override val recommendedLevelMax = 3
 *
 *     override fun defineContent() {
 *         // Define abilities
 *         ability("goblin-stab") {
 *             name = "Stab"
 *             description = "A quick stab with a rusty dagger"
 *             // ...
 *         }
 *
 *         // Define creatures
 *         creature("goblin-scout") {
 *             name = "Goblin Scout"
 *             description = "A small, sneaky goblin"
 *             abilities("goblin-stab")
 *             loot { gold(1, 5) }
 *         }
 *
 *         // Define locations
 *         location("entrance") {
 *             name = "Cave Entrance"
 *             description = "A dark cave entrance"
 *             creatures("goblin-scout", "goblin-scout")
 *             exits { north("main-hall") }
 *         }
 *     }
 * }
 * ```
 */
abstract class AdventureModuleSeed {

    protected val log = LoggerFactory.getLogger(this::class.java)

    // ============== Module Metadata ==============

    /** Unique identifier for this module (used as prefix for all entity IDs) */
    abstract val moduleId: String

    /** Human-readable name */
    abstract val moduleName: String

    /** Description shown to players */
    abstract val moduleDescription: String

    /** Attribution source (e.g., "D&D Module B2") */
    abstract val attribution: String

    /** Minimum recommended player level */
    abstract val recommendedLevelMin: Int

    /** Maximum recommended player level */
    abstract val recommendedLevelMax: Int

    /** Area ID for grouping locations */
    val areaId: String get() = moduleId

    // ============== Content Builders ==============

    private val abilities = mutableListOf<AbilityBuilder>()
    private val items = mutableListOf<ItemBuilder>()
    private val lootTables = mutableListOf<LootTableBuilder>()
    private val creatures = mutableListOf<CreatureBuilder>()
    private val locations = mutableListOf<LocationBuilder>()
    private val chests = mutableListOf<ChestBuilder>()
    private val pools = mutableListOf<PoolBuilder>()
    private val traps = mutableListOf<TrapBuilder>()
    private val factions = mutableListOf<FactionBuilder>()
    private val factionRelations = mutableListOf<FactionRelationBuilder>()

    // ============== DSL Entry Point ==============

    /**
     * Override this to define all module content.
     */
    abstract fun defineContent()

    // ============== ID Generation ==============

    /** Generate a module-prefixed ID */
    protected fun id(suffix: String): String = "$moduleId-$suffix"

    /** Generate ability ID */
    protected fun abilityId(suffix: String): String = "ability-$moduleId-$suffix"

    /** Generate creature ID */
    protected fun creatureId(suffix: String): String = "creature-$moduleId-$suffix"

    /** Generate item ID */
    protected fun itemId(suffix: String): String = "item-$moduleId-$suffix"

    /** Generate location ID */
    protected fun locationId(suffix: String): String = "location-$moduleId-$suffix"

    /** Generate loot table ID */
    protected fun lootId(suffix: String): String = "loot-$moduleId-$suffix"

    /** Generate chest ID */
    protected fun chestId(suffix: String): String = "chest-$moduleId-$suffix"

    /** Generate pool ID */
    protected fun poolId(suffix: String): String = "pool-$moduleId-$suffix"

    /** Generate trap ID */
    protected fun trapId(suffix: String): String = "trap-$moduleId-$suffix"

    /** Generate faction ID */
    protected fun factionId(suffix: String): String = "faction-$moduleId-$suffix"

    // ============== DSL Functions ==============

    /**
     * Define an ability for this module.
     */
    protected fun ability(suffix: String, block: AbilityBuilder.() -> Unit): AbilityBuilder {
        val builder = AbilityBuilder(abilityId(suffix))
        builder.block()
        abilities.add(builder)
        return builder
    }

    /**
     * Define an item for this module.
     */
    protected fun item(suffix: String, block: ItemBuilder.() -> Unit): ItemBuilder {
        val builder = ItemBuilder(itemId(suffix))
        builder.block()
        items.add(builder)
        return builder
    }

    /**
     * Define a loot table for this module.
     */
    protected fun lootTable(suffix: String, block: LootTableBuilder.() -> Unit): LootTableBuilder {
        val builder = LootTableBuilder(lootId(suffix), this)
        builder.block()
        lootTables.add(builder)
        return builder
    }

    /**
     * Define a creature for this module.
     */
    protected fun creature(suffix: String, block: CreatureBuilder.() -> Unit): CreatureBuilder {
        val builder = CreatureBuilder(creatureId(suffix), attribution, this)
        builder.block()
        creatures.add(builder)
        return builder
    }

    /**
     * Define a location for this module.
     */
    protected fun location(suffix: String, block: LocationBuilder.() -> Unit): LocationBuilder {
        val builder = LocationBuilder(locationId(suffix), areaId, this)
        builder.block()
        locations.add(builder)
        return builder
    }

    /**
     * Define a chest for this module.
     */
    protected fun chest(suffix: String, block: ChestBuilder.() -> Unit): ChestBuilder {
        val builder = ChestBuilder(chestId(suffix), this)
        builder.block()
        chests.add(builder)
        return builder
    }

    /**
     * Define a magical pool for this module.
     */
    protected fun pool(suffix: String, block: PoolBuilder.() -> Unit): PoolBuilder {
        val builder = PoolBuilder(poolId(suffix), this)
        builder.block()
        pools.add(builder)
        return builder
    }

    /**
     * Define a trap for this module.
     */
    protected fun trap(suffix: String, block: TrapBuilder.() -> Unit): TrapBuilder {
        val builder = TrapBuilder(trapId(suffix), this)
        builder.block()
        traps.add(builder)
        return builder
    }

    /**
     * Define a faction for this module.
     */
    protected fun faction(suffix: String, block: FactionBuilder.() -> Unit): FactionBuilder {
        val builder = FactionBuilder(factionId(suffix), this)
        builder.block()
        factions.add(builder)
        return builder
    }

    /**
     * Define a relationship between two factions.
     */
    protected fun factionRelation(factionSuffix: String, targetFactionSuffix: String, level: Int) {
        factionRelations.add(FactionRelationBuilder(
            factionId = factionId(factionSuffix),
            targetFactionId = factionId(targetFactionSuffix),
            relationshipLevel = level
        ))
    }

    // ============== Seeding ==============

    /**
     * Seed all module content if it doesn't exist.
     */
    fun seedIfEmpty() {
        // Check if module already exists by looking for any creature
        if (creatures.isNotEmpty()) {
            val firstCreatureId = creatures.first().id
            if (CreatureRepository.findById(firstCreatureId) != null) {
                log.debug("Module $moduleName already seeded, skipping")
                return
            }
        } else if (locations.isNotEmpty()) {
            val firstLocationId = locations.first().id
            if (LocationRepository.findById(firstLocationId) != null) {
                log.debug("Module $moduleName already seeded, skipping")
                return
            }
        }

        // Clear and redefine content
        abilities.clear()
        items.clear()
        lootTables.clear()
        creatures.clear()
        locations.clear()
        chests.clear()
        pools.clear()
        traps.clear()
        factions.clear()
        factionRelations.clear()

        defineContent()

        log.info("Seeding adventure module: $moduleName")

        // Seed in dependency order
        seedAbilities()
        seedItems()
        seedLootTables()
        seedCreatures()
        seedLocations()
        seedChests()
        seedPools()
        seedTraps()
        seedFactions()
        seedFactionRelations()

        log.info("Seeded adventure module: $moduleName (${abilities.size} abilities, ${items.size} items, ${creatures.size} creatures, ${locations.size} locations, ${chests.size} chests, ${pools.size} pools, ${traps.size} traps, ${factions.size} factions)")
    }

    private fun seedAbilities() {
        abilities.forEach { builder ->
            if (AbilityRepository.findById(builder.id) == null) {
                AbilityRepository.create(builder.build())
            }
        }
    }

    private fun seedItems() {
        items.forEach { builder ->
            if (ItemRepository.findById(builder.id) == null) {
                ItemRepository.create(builder.build())
            }
        }
    }

    private fun seedLootTables() {
        lootTables.forEach { builder ->
            if (LootTableRepository.findById(builder.id) == null) {
                LootTableRepository.create(builder.build())
            }
        }
    }

    private fun seedCreatures() {
        creatures.forEach { builder ->
            if (CreatureRepository.findById(builder.id) == null) {
                CreatureRepository.create(builder.build())
            }
        }
    }

    private fun seedLocations() {
        locations.forEach { builder ->
            if (LocationRepository.findById(builder.id) == null) {
                LocationRepository.create(builder.build())
            }
        }
    }

    private fun seedChests() {
        chests.forEach { builder ->
            if (ChestRepository.findById(builder.id) == null) {
                ChestRepository.create(builder.build())
            }
        }
    }

    private fun seedPools() {
        pools.forEach { builder ->
            if (PoolRepository.findById(builder.id) == null) {
                PoolRepository.create(builder.build())
            }
        }
    }

    private fun seedTraps() {
        traps.forEach { builder ->
            if (TrapRepository.findById(builder.id) == null) {
                TrapRepository.create(builder.build())
            }
        }
    }

    private fun seedFactions() {
        factions.forEach { builder ->
            if (FactionRepository.findById(builder.id) == null) {
                FactionRepository.create(builder.build())
            }
        }
    }

    private fun seedFactionRelations() {
        factionRelations.forEach { builder ->
            if (FactionRelationRepository.findRelation(builder.factionId, builder.targetFactionId) == null) {
                FactionRelationRepository.create(builder.build())
            }
        }
    }

    // ============== Builder Classes ==============

    class AbilityBuilder(val id: String) {
        var name: String = ""
        var description: String = ""
        var classId: String? = null
        var abilityType: String = "combat"
        var targetType: String = "single_enemy"
        var range: Int = 5
        var cooldownType: String = "none"
        var cooldownRounds: Int = 0
        var baseDamage: Int = 0
        var durationRounds: Int = 0
        var effects: String = "[]"
        var manaCost: Int = 0
        var staminaCost: Int = 0

        fun build(): Ability = Ability(
            id = id,
            name = name,
            description = description,
            classId = classId,
            abilityType = abilityType,
            targetType = targetType,
            range = range,
            cooldownType = cooldownType,
            cooldownRounds = cooldownRounds,
            baseDamage = baseDamage,
            durationRounds = durationRounds,
            effects = effects,
            manaCost = manaCost,
            staminaCost = staminaCost
        )
    }

    class ItemBuilder(val id: String) {
        var name: String = ""
        var description: String = ""
        var value: Int = 0
        var weight: Int = 1
        var equipmentType: String? = null
        var equipmentSlot: String? = null
        var statBonuses: StatBonuses? = null
        var featureIds: List<String> = emptyList()
        var abilityIds: List<String> = emptyList()

        fun stats(attack: Int = 0, defense: Int = 0, maxHp: Int = 0) {
            statBonuses = StatBonuses(attack = attack, defense = defense, maxHp = maxHp)
        }

        fun weapon(slot: String = "main_hand") {
            equipmentType = "weapon"
            equipmentSlot = slot
        }

        fun armor(slot: String) {
            equipmentType = "armor"
            equipmentSlot = slot
        }

        fun accessory(slot: String = "finger") {
            equipmentType = "accessory"
            equipmentSlot = slot
        }

        fun build(): Item = Item(
            id = id,
            name = name,
            desc = description,
            value = value,
            weight = weight,
            equipmentType = equipmentType,
            equipmentSlot = equipmentSlot,
            statBonuses = statBonuses,
            featureIds = featureIds,
            abilityIds = abilityIds
        )
    }

    class LootTableBuilder(val id: String, private val module: AdventureModuleSeed) {
        var name: String = ""
        private val entries = mutableListOf<LootEntry>()

        /** Add an item to the loot table by module-relative suffix */
        fun item(suffix: String, chance: Float = 1.0f, minQty: Int = 1, maxQty: Int = 1) {
            entries.add(LootEntry(
                itemId = module.itemId(suffix),
                chance = chance,
                minQty = minQty,
                maxQty = maxQty
            ))
        }

        /** Add an item by full ID (for cross-module items) */
        fun itemById(itemId: String, chance: Float = 1.0f, minQty: Int = 1, maxQty: Int = 1) {
            entries.add(LootEntry(
                itemId = itemId,
                chance = chance,
                minQty = minQty,
                maxQty = maxQty
            ))
        }

        fun build(): LootTableData = LootTableData(
            id = id,
            name = name,
            entries = entries
        )
    }

    class CreatureBuilder(
        val id: String,
        private val attribution: String,
        private val module: AdventureModuleSeed
    ) {
        var name: String = ""
        var description: String = ""
        var maxHp: Int = 10
        var baseDamage: Int = 5
        var damageDice: String? = null
        var level: Int = 1
        var experienceValue: Int = 10
        var challengeRating: Int = 1
        var isAggressive: Boolean = false
        var minGoldDrop: Int = 0
        var maxGoldDrop: Int = 0
        var lootTableId: String? = null
        private val abilityIdList = mutableListOf<String>()
        private val itemIdList = mutableListOf<String>()
        private val featureIdList = mutableListOf<String>()

        /** Add abilities by module-relative suffix */
        fun abilities(vararg suffixes: String) {
            suffixes.forEach { abilityIdList.add(module.abilityId(it)) }
        }

        /** Add abilities by full ID */
        fun abilitiesById(vararg ids: String) {
            abilityIdList.addAll(ids)
        }

        /** Set loot table by module-relative suffix */
        fun lootTable(suffix: String) {
            lootTableId = module.lootId(suffix)
        }

        /** Set gold drop range */
        fun gold(min: Int, max: Int) {
            minGoldDrop = min
            maxGoldDrop = max
        }

        fun build(): Creature = Creature(
            id = id,
            name = name,
            desc = description,
            maxHp = maxHp,
            baseDamage = baseDamage,
            damageDice = damageDice,
            level = level,
            experienceValue = experienceValue,
            challengeRating = challengeRating,
            isAggressive = isAggressive,
            minGoldDrop = minGoldDrop,
            maxGoldDrop = maxGoldDrop,
            lootTableId = lootTableId,
            abilityIds = abilityIdList,
            itemIds = itemIdList,
            featureIds = featureIdList,
            attribution = attribution
        )
    }

    class LocationBuilder(
        val id: String,
        private val areaId: String,
        private val module: AdventureModuleSeed
    ) {
        var name: String = ""
        var description: String = ""
        var gridX: Int? = null
        var gridY: Int? = null
        var gridZ: Int = 0
        var locationType: LocationType = LocationType.INDOOR
        var lockLevel: Int? = null
        var lockedBy: String? = null
        private val creatureIdList = mutableListOf<String>()
        private val itemIdList = mutableListOf<String>()
        private val featureIdList = mutableListOf<String>()
        private val exitList = mutableListOf<Exit>()

        /** Set grid position */
        fun position(x: Int, y: Int, z: Int = 0) {
            gridX = x
            gridY = y
            gridZ = z
        }

        /** Add creatures by module-relative suffix */
        fun creatures(vararg suffixes: String) {
            suffixes.forEach { creatureIdList.add(module.creatureId(it)) }
        }

        /** Add creatures by full ID */
        fun creaturesById(vararg ids: String) {
            creatureIdList.addAll(ids)
        }

        /** Add items by module-relative suffix */
        fun items(vararg suffixes: String) {
            suffixes.forEach { itemIdList.add(module.itemId(it)) }
        }

        /** Add items by full ID */
        fun itemsById(vararg ids: String) {
            itemIdList.addAll(ids)
        }

        /** Add exits using a DSL block */
        fun exits(block: ExitBuilder.() -> Unit) {
            val builder = ExitBuilder(module)
            builder.block()
            exitList.addAll(builder.exits)
        }

        fun build(): Location = Location(
            id = id,
            name = name,
            desc = description,
            gridX = gridX,
            gridY = gridY,
            gridZ = gridZ,
            areaId = areaId,
            locationType = locationType,
            lockLevel = lockLevel,
            lockedBy = lockedBy,
            creatureIds = creatureIdList,
            itemIds = itemIdList,
            featureIds = featureIdList,
            exits = exitList
        )
    }

    class ExitBuilder(private val module: AdventureModuleSeed) {
        val exits = mutableListOf<Exit>()

        /** Create exit by module-relative suffix */
        private fun exit(direction: ExitDirection, suffix: String) {
            exits.add(Exit(locationId = module.locationId(suffix), direction = direction))
        }

        /** Create exit by full ID */
        private fun exitById(direction: ExitDirection, locationId: String) {
            exits.add(Exit(locationId = locationId, direction = direction))
        }

        fun north(suffix: String) = exit(ExitDirection.NORTH, suffix)
        fun south(suffix: String) = exit(ExitDirection.SOUTH, suffix)
        fun east(suffix: String) = exit(ExitDirection.EAST, suffix)
        fun west(suffix: String) = exit(ExitDirection.WEST, suffix)
        fun up(suffix: String) = exit(ExitDirection.UP, suffix)
        fun down(suffix: String) = exit(ExitDirection.DOWN, suffix)
        fun enter(suffix: String) = exit(ExitDirection.ENTER, suffix)
        fun northeast(suffix: String) = exit(ExitDirection.NORTHEAST, suffix)
        fun northwest(suffix: String) = exit(ExitDirection.NORTHWEST, suffix)
        fun southeast(suffix: String) = exit(ExitDirection.SOUTHEAST, suffix)
        fun southwest(suffix: String) = exit(ExitDirection.SOUTHWEST, suffix)

        // Full ID versions for cross-module connections
        fun northTo(locationId: String) = exitById(ExitDirection.NORTH, locationId)
        fun southTo(locationId: String) = exitById(ExitDirection.SOUTH, locationId)
        fun eastTo(locationId: String) = exitById(ExitDirection.EAST, locationId)
        fun westTo(locationId: String) = exitById(ExitDirection.WEST, locationId)
        fun enterTo(locationId: String) = exitById(ExitDirection.ENTER, locationId)
    }

    class ChestBuilder(val id: String, private val module: AdventureModuleSeed) {
        var name: String = ""
        var description: String = ""
        var locationSuffix: String = ""
        var guardianCreatureSuffix: String? = null
        var isLocked: Boolean = false
        var lockDifficulty: Int = 1
        var bashDifficulty: Int = 2
        var lootTableSuffix: String? = null
        var goldAmount: Int = 0

        fun build(): Chest = Chest(
            id = id,
            name = name,
            desc = description,
            locationId = module.locationId(locationSuffix),
            guardianCreatureId = guardianCreatureSuffix?.let { module.creatureId(it) },
            isLocked = isLocked,
            lockDifficulty = lockDifficulty,
            bashDifficulty = bashDifficulty,
            lootTableId = lootTableSuffix?.let { module.lootId(it) },
            goldAmount = goldAmount
        )
    }

    /**
     * Builder for magical pools with various effects.
     */
    class PoolBuilder(val id: String, private val module: AdventureModuleSeed) {
        var name: String = ""
        var description: String = ""
        var locationSuffix: String = ""

        // Visual appearance
        var liquidColor: String = "clear"
        var liquidAppearance: String = "still"

        // Effect configuration
        var effectType: PoolEffectType = PoolEffectType.EMPTY
        private var effectData: PoolEffectData = PoolEffectData()

        // Usage limits
        var usesPerDay: Int = 0  // 0 = unlimited
        var isOneTimeUse: Boolean = false

        // Discovery
        var isHidden: Boolean = false
        var identifyDifficulty: Int = 0

        // === Effect Configuration Methods ===

        /** Configure healing effect */
        fun healing(amount: Int? = null, dice: String? = null, curesDisease: Boolean = false, curesPoison: Boolean = false) {
            effectType = PoolEffectType.HEALING
            effectData = effectData.copy(
                healAmount = amount,
                healDice = dice,
                curesDisease = curesDisease,
                curesPoison = curesPoison
            )
        }

        /** Configure damage effect */
        fun damage(amount: Int? = null, dice: String? = null, type: String = "acid") {
            effectType = PoolEffectType.DAMAGE
            effectData = effectData.copy(
                damageAmount = amount,
                damageDice = dice,
                damageType = type
            )
        }

        /** Configure stat buff */
        fun buff(stat: String, modifier: Int, durationRounds: Int? = null, durationMinutes: Int? = null) {
            effectType = PoolEffectType.BUFF
            effectData = effectData.copy(
                statModifier = stat,
                modifierAmount = modifier,
                durationRounds = durationRounds,
                durationMinutes = durationMinutes
            )
        }

        /** Configure stat debuff */
        fun debuff(stat: String, modifier: Int, durationRounds: Int? = null, durationMinutes: Int? = null) {
            effectType = PoolEffectType.DEBUFF
            effectData = effectData.copy(
                statModifier = stat,
                modifierAmount = modifier,
                durationRounds = durationRounds,
                durationMinutes = durationMinutes
            )
        }

        /** Configure poison effect */
        fun poison(damageDice: String, durationRounds: Int, chance: Float = 1.0f) {
            effectType = PoolEffectType.POISON
            effectData = effectData.copy(
                damageDice = damageDice,
                appliesCondition = "poisoned",
                conditionDuration = durationRounds,
                conditionChance = chance
            )
        }

        /** Configure condition effect (charmed, sleeping, etc.) */
        fun condition(conditionName: String, duration: Int, chance: Float = 1.0f) {
            effectType = when (conditionName) {
                "charmed" -> PoolEffectType.CHARM
                "sleeping" -> PoolEffectType.SLEEP
                else -> PoolEffectType.STRANGE
            }
            effectData = effectData.copy(
                appliesCondition = conditionName,
                conditionDuration = duration,
                conditionChance = chance
            )
        }

        /** Configure teleport effect */
        fun teleport(locationSuffix: String) {
            effectType = PoolEffectType.TELEPORT
            effectData = effectData.copy(
                teleportLocationId = module.locationId(locationSuffix)
            )
        }

        /** Configure treasure at bottom of pool */
        fun treasure(itemSuffix: String? = null, gold: Int = 0) {
            effectType = PoolEffectType.TREASURE
            effectData = effectData.copy(
                containsItemId = itemSuffix?.let { module.itemId(it) },
                goldAmount = gold
            )
        }

        /** Configure trap effect */
        fun trap(message: String, damageDice: String? = null) {
            effectType = PoolEffectType.TRAP
            effectData = effectData.copy(
                trapMessage = message,
                damageDice = damageDice
            )
        }

        /** Configure wine/drinkable effect */
        fun wine(message: String? = null) {
            effectType = PoolEffectType.WINE
            effectData = effectData.copy(customMessage = message ?: "The wine is surprisingly good!")
        }

        /** Configure empty/dried pool */
        fun empty(message: String? = null) {
            effectType = PoolEffectType.EMPTY
            effectData = effectData.copy(customMessage = message ?: "The pool is empty.")
        }

        /** Configure custom message for any effect */
        fun message(customMessage: String) {
            effectData = effectData.copy(customMessage = customMessage)
        }

        /** Configure secret message revealed after identification */
        fun secretMessage(message: String) {
            effectData = effectData.copy(secretMessage = message)
        }

        fun build(): Pool = Pool(
            id = id,
            name = name,
            description = description,
            locationId = module.locationId(locationSuffix),
            liquidColor = liquidColor,
            liquidAppearance = liquidAppearance,
            effectType = effectType,
            effectData = effectData,
            usesPerDay = usesPerDay,
            isOneTimeUse = isOneTimeUse,
            isHidden = isHidden,
            identifyDifficulty = identifyDifficulty
        )
    }

    /**
     * Builder for traps with various effects.
     */
    class TrapBuilder(val id: String, private val module: AdventureModuleSeed) {
        var name: String = ""
        var description: String = ""
        var locationSuffix: String = ""

        // Type and trigger
        var trapType: TrapType = TrapType.PIT
        var triggerType: TrapTrigger = TrapTrigger.MOVEMENT

        // Detection and disarm difficulty (1-5 scale)
        var detectDifficulty: Int = 2
        var disarmDifficulty: Int = 2

        // Effect configuration
        private var effectData: TrapEffectData = TrapEffectData()

        // State
        var isHidden: Boolean = true
        var isArmed: Boolean = true
        var resetsAfterRounds: Int = 0  // 0 = doesn't reset

        // === Trap Type Configuration Methods ===

        /** Configure a pit trap */
        fun pit(depth: Int, damageDice: String = "1d6", saveDC: Int = 12) {
            trapType = TrapType.PIT
            triggerType = TrapTrigger.MOVEMENT
            effectData = effectData.copy(
                pitDepth = depth,
                damageDice = damageDice,
                damageType = "falling",
                savingThrowType = "dexterity",
                savingThrowDC = saveDC,
                appliesCondition = "prone",
                conditionDuration = 1
            )
        }

        /** Configure a dart trap */
        fun dart(damageDice: String = "1d4", saveDC: Int = 12, poisoned: Boolean = false, poisonDuration: Int = 10) {
            trapType = TrapType.DART
            effectData = effectData.copy(
                damageDice = damageDice,
                damageType = "piercing",
                savingThrowType = "dexterity",
                savingThrowDC = saveDC,
                poisonDuration = if (poisoned) poisonDuration else null
            )
        }

        /** Configure a poison needle trap (typically on chests/doors) */
        fun poisonNeedle(damageDice: String = "1", poisonDuration: Int = 10, saveDC: Int = 13) {
            trapType = TrapType.POISON_NEEDLE
            triggerType = TrapTrigger.CHEST
            effectData = effectData.copy(
                damageDice = damageDice,
                damageType = "piercing",
                poisonDuration = poisonDuration,
                savingThrowType = "constitution",
                savingThrowDC = saveDC
            )
        }

        /** Configure a boulder trap */
        fun boulder(damageDice: String = "4d6", saveDC: Int = 15) {
            trapType = TrapType.BOULDER
            triggerType = TrapTrigger.PRESSURE_PLATE
            effectData = effectData.copy(
                damageDice = damageDice,
                damageType = "bludgeoning",
                savingThrowType = "dexterity",
                savingThrowDC = saveDC
            )
        }

        /** Configure an alarm trap that alerts creatures */
        fun alarm(creatureSuffixes: List<String>, message: String? = null) {
            trapType = TrapType.ALARM
            effectData = effectData.copy(
                alertsCreatureIds = creatureSuffixes.map { module.creatureId(it) },
                customMessage = message ?: "An alarm sounds! Nearby creatures are alerted!"
            )
        }

        /** Configure a fire trap */
        fun fire(damageDice: String = "2d6", saveDC: Int = 13) {
            trapType = TrapType.FIRE
            effectData = effectData.copy(
                damageDice = damageDice,
                damageType = "fire",
                savingThrowType = "dexterity",
                savingThrowDC = saveDC
            )
        }

        /** Configure a spear trap */
        fun spear(damageDice: String = "2d6", saveDC: Int = 14) {
            trapType = TrapType.SPEAR
            triggerType = TrapTrigger.PRESSURE_PLATE
            effectData = effectData.copy(
                damageDice = damageDice,
                damageType = "piercing",
                savingThrowType = "dexterity",
                savingThrowDC = saveDC
            )
        }

        /** Configure a cage trap */
        fun cage(duration: Int = 10, saveDC: Int = 13) {
            trapType = TrapType.CAGE
            effectData = effectData.copy(
                appliesCondition = "restrained",
                conditionDuration = duration,
                savingThrowType = "dexterity",
                savingThrowDC = saveDC
            )
        }

        /** Configure a teleport trap */
        fun teleport(destinationSuffix: String, message: String? = null) {
            trapType = TrapType.TELEPORT
            effectData = effectData.copy(
                teleportLocationId = module.locationId(destinationSuffix),
                customMessage = message ?: "Reality warps around you and suddenly you're somewhere else!"
            )
        }

        /** Configure a magic trap with custom effects */
        fun magic(damageDice: String? = null, condition: String? = null, conditionDuration: Int = 0, message: String? = null) {
            trapType = TrapType.MAGIC
            effectData = effectData.copy(
                damageDice = damageDice,
                appliesCondition = condition,
                conditionDuration = conditionDuration,
                customMessage = message ?: "Magical energy surges through you!"
            )
        }

        /** Set trigger type */
        fun trigger(type: TrapTrigger) {
            triggerType = type
        }

        /** Set custom message */
        fun message(customMessage: String) {
            effectData = effectData.copy(customMessage = customMessage)
        }

        fun build(): Trap = Trap(
            id = id,
            name = name,
            description = description,
            locationId = module.locationId(locationSuffix),
            trapType = trapType,
            triggerType = triggerType,
            detectDifficulty = detectDifficulty,
            disarmDifficulty = disarmDifficulty,
            effectData = effectData,
            isHidden = isHidden,
            isArmed = isArmed,
            resetsAfterRounds = resetsAfterRounds
        )
    }

    /**
     * Builder for factions (creature allegiance groups).
     */
    class FactionBuilder(val id: String, private val module: AdventureModuleSeed) {
        var name: String = ""
        var description: String = ""
        var homeLocationSuffix: String? = null
        var hostilityLevel: Int = 50  // 0=peaceful, 50=neutral, 100=hostile
        var canNegotiate: Boolean = true
        var leaderCreatureSuffix: String? = null

        // Faction data
        private val territoryLocationSuffixes = mutableListOf<String>()
        private val enemyFactionSuffixes = mutableListOf<String>()
        private val allyFactionSuffixes = mutableListOf<String>()
        private val goals = mutableListOf<String>()
        private val tradeGoodSuffixes = mutableListOf<String>()
        private val tributeItemSuffixes = mutableListOf<String>()

        /** Set this faction as hostile (80+ hostility) */
        fun hostile() {
            hostilityLevel = 80
        }

        /** Set this faction as peaceful (20- hostility) */
        fun peaceful() {
            hostilityLevel = 20
        }

        /** Set this faction as neutral (50 hostility) */
        fun neutral() {
            hostilityLevel = 50
        }

        /** Mark as unable to negotiate */
        fun noNegotiation() {
            canNegotiate = false
        }

        /** Add territory locations by suffix */
        fun territory(vararg suffixes: String) {
            territoryLocationSuffixes.addAll(suffixes)
        }

        /** Add enemy factions by suffix */
        fun enemies(vararg suffixes: String) {
            enemyFactionSuffixes.addAll(suffixes)
        }

        /** Add allied factions by suffix */
        fun allies(vararg suffixes: String) {
            allyFactionSuffixes.addAll(suffixes)
        }

        /** Add faction goals */
        fun goals(vararg goalStrings: String) {
            goals.addAll(goalStrings)
        }

        /** Add trade goods by item suffix */
        fun trades(vararg itemSuffixes: String) {
            tradeGoodSuffixes.addAll(itemSuffixes)
        }

        /** Add tribute items by item suffix */
        fun acceptsTribute(vararg itemSuffixes: String) {
            tributeItemSuffixes.addAll(itemSuffixes)
        }

        fun build(): Faction = Faction(
            id = id,
            name = name,
            description = description,
            homeLocationId = homeLocationSuffix?.let { module.locationId(it) },
            hostilityLevel = hostilityLevel,
            canNegotiate = canNegotiate,
            leaderCreatureId = leaderCreatureSuffix?.let { module.creatureId(it) },
            data = FactionData(
                territoryLocationIds = territoryLocationSuffixes.map { module.locationId(it) },
                enemyFactionIds = enemyFactionSuffixes.map { module.factionId(it) },
                allyFactionIds = allyFactionSuffixes.map { module.factionId(it) },
                goals = goals,
                tradeGoods = tradeGoodSuffixes.map { module.itemId(it) },
                tributeItems = tributeItemSuffixes.map { module.itemId(it) }
            )
        )
    }

    /**
     * Builder for faction relationships.
     */
    class FactionRelationBuilder(
        val factionId: String,
        val targetFactionId: String,
        val relationshipLevel: Int
    ) {
        fun build(): FactionRelation = FactionRelation(
            factionId = factionId,
            targetFactionId = targetFactionId,
            relationshipLevel = relationshipLevel
        )
    }
}

/**
 * Registry for adventure modules.
 * Modules are registered here and can be seeded individually or all at once.
 */
object AdventureModuleRegistry {
    private val log = LoggerFactory.getLogger(AdventureModuleRegistry::class.java)

    private val modules = mutableListOf<AdventureModuleSeed>()

    /**
     * Register a module with the registry.
     */
    fun register(module: AdventureModuleSeed) {
        modules.add(module)
        log.debug("Registered adventure module: ${module.moduleName}")
    }

    /**
     * Seed all registered modules.
     */
    fun seedAll() {
        log.info("Seeding ${modules.size} adventure modules...")
        modules.forEach { it.seedIfEmpty() }
    }

    /**
     * Get all registered modules.
     */
    fun getAll(): List<AdventureModuleSeed> = modules.toList()

    /**
     * Find a module by ID.
     */
    fun findById(moduleId: String): AdventureModuleSeed? =
        modules.find { it.moduleId == moduleId }
}
