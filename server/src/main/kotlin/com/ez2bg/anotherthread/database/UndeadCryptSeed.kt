package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.ImageGenerationService
import org.slf4j.LoggerFactory

/**
 * Seed data for Undead Crypt area - a themed dungeon featuring various undead monsters.
 * Inspired by classic D&D undead creatures with appropriate abilities, items, and loot.
 */
object UndeadCryptSeed {
    private val log = LoggerFactory.getLogger(UndeadCryptSeed::class.java)

    // Attribution for tracking content source
    private const val ATTRIBUTION = "Classic D&D Undead"

    // ============== CREATURE IDS ==============
    // Tier 1 (CR 1) - Levels 1-3
    const val ZOMBIE_SHAMBLER_ID = "creature-zombie-shambler"
    const val SKELETAL_ARCHER_ID = "creature-skeletal-archer"
    const val GHAST_ID = "creature-ghast"

    // Tier 2 (CR 2-3) - Levels 4-6
    const val WIGHT_ID = "creature-wight"
    const val SPECTER_ID = "creature-specter"
    const val MUMMY_ID = "creature-mummy"
    const val SKELETON_MAGE_ID = "creature-skeleton-mage"

    // Tier 3 (CR 4-5) - Levels 7-10
    const val BONE_GOLEM_ID = "creature-bone-golem"
    const val VAMPIRE_SPAWN_ID = "creature-vampire-spawn"
    const val DEATH_KNIGHT_ID = "creature-death-knight"

    // Tier 4 (CR 6+) - Boss
    const val VAMPIRE_LORD_ID = "creature-vampire-lord"

    // ============== ABILITY IDS ==============
    const val ABILITY_FEAR_AURA_ID = "ability-fear-aura"
    const val ABILITY_NECROTIC_TOUCH_ID = "ability-necrotic-touch"
    const val ABILITY_MUMMY_ROT_ID = "ability-mummy-rot"
    const val ABILITY_RAISE_DEAD_ID = "ability-raise-dead"
    const val ABILITY_BLOOD_DRAIN_ID = "ability-blood-drain"
    const val ABILITY_UNHOLY_SMITE_ID = "ability-unholy-smite"
    const val ABILITY_SHADOWBOLT_ID = "ability-shadowbolt"
    const val ABILITY_BONE_SHATTER_ID = "ability-bone-shatter"
    const val ABILITY_DOMINATE_ID = "ability-dominate"
    const val ABILITY_CHARM_GAZE_ID = "ability-charm-gaze"

    // ============== ITEM IDS ==============
    // Consumables
    const val HOLY_WATER_VIAL_ID = "item-holy-water-vial"
    const val BLESSED_BANDAGE_ID = "item-blessed-bandage"
    const val RESTORATION_ELIXIR_ID = "item-restoration-elixir"
    const val TURN_UNDEAD_SCROLL_ID = "item-turn-undead-scroll"

    // Weapons
    const val SILVER_LONGSWORD_ID = "item-silver-longsword"
    const val MACE_OF_DISRUPTION_ID = "item-mace-of-disruption"
    const val HOLY_AVENGER_ID = "item-holy-avenger"
    const val BONE_BOW_ID = "item-bone-bow"
    const val DEATH_KNIGHTS_BLADE_ID = "item-death-knights-blade"

    // Armor
    const val BLESSED_CHAINMAIL_ID = "item-blessed-chainmail"
    const val DEATH_KNIGHTS_PLATE_ID = "item-death-knights-plate"
    const val CLOAK_OF_SHADOWS_ID = "item-cloak-of-shadows"

    // Accessories
    const val AMULET_OF_LIFE_PROTECTION_ID = "item-amulet-of-life-protection"
    const val RING_OF_POSITIVE_ENERGY_ID = "item-ring-of-positive-energy"
    const val PHYLACTERY_SHARD_ID = "item-phylactery-shard"

    // Treasure
    const val ANCIENT_TOMB_GOLD_ID = "item-ancient-tomb-gold"
    const val VAMPIRE_FANG_ID = "item-vampire-fang"
    const val SOUL_GEM_ID = "item-soul-gem"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_ZOMBIE_ID = "loot-crypt-zombie"
    const val LOOT_TABLE_SKELETON_UNDEAD_ID = "loot-crypt-skeleton"
    const val LOOT_TABLE_GHAST_ID = "loot-ghast"
    const val LOOT_TABLE_WIGHT_ID = "loot-wight"
    const val LOOT_TABLE_MUMMY_ID = "loot-mummy"
    const val LOOT_TABLE_VAMPIRE_SPAWN_ID = "loot-vampire-spawn"
    const val LOOT_TABLE_DEATH_KNIGHT_ID = "loot-death-knight"
    const val LOOT_TABLE_VAMPIRE_LORD_ID = "loot-vampire-lord"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (ZOMBIE_SHAMBLER_ID !in existingCreatures) {
            seedCreatureAbilities()
            seedItems()
            seedLootTables()
            seedCreatures()
            println("Seeded Undead Crypt content (D&D-inspired undead)")
        }
    }

    private fun seedCreatureAbilities() {
        val abilities = listOf(
            // Fear Aura - used by Wraith, Death Knight, Vampire Lord
            Ability(
                id = ABILITY_FEAR_AURA_ID,
                name = "Fear Aura",
                description = "An aura of dread emanates from this creature. Enemies within range must resist or become frightened, reducing their attack power.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 20,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 0,
                durationRounds = 3,
                effects = """[{"type":"condition","condition":"frightened","duration":3,"saveType":"wisdom"},{"type":"debuff","stat":"attack","penalty":4,"duration":3}]""",
                attribution = ATTRIBUTION
            ),
            // Necrotic Touch - used by Wight, Specter
            Ability(
                id = ABILITY_NECROTIC_TOUCH_ID,
                name = "Necrotic Touch",
                description = "A touch charged with negative energy that deals necrotic damage and temporarily reduces the target's maximum HP.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 10,
                durationRounds = 3,
                effects = """[{"type":"damage","damageType":"necrotic","modifier":10},{"type":"debuff","stat":"maxHp","penalty":5,"duration":3}]""",
                attribution = ATTRIBUTION
            ),
            // Mummy Rot - used by Mummy
            Ability(
                id = ABILITY_MUMMY_ROT_ID,
                name = "Mummy Rot",
                description = "Inflicts a terrible curse that prevents healing and deals ongoing necrotic damage. Only powerful magic can remove this affliction.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 8,
                durationRounds = 4,
                effects = """[{"type":"damage","damageType":"necrotic","modifier":8},{"type":"dot","damageType":"necrotic","damage":3,"duration":4},{"type":"condition","condition":"no_healing","duration":4}]""",
                attribution = ATTRIBUTION
            ),
            // Raise Dead - used by Skeleton Mage, Death Knight
            Ability(
                id = ABILITY_RAISE_DEAD_ID,
                name = "Raise Dead",
                description = "Channels dark magic to reanimate fallen corpses, summoning skeletal minions to fight alongside the caster.",
                classId = null,
                abilityType = "summon",
                targetType = "area",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 0,
                effects = """[{"type":"summon","summonId":"creature-skeleton-warrior","count":2,"duration":5}]""",
                attribution = ATTRIBUTION
            ),
            // Blood Drain - used by Vampire Spawn, Vampire Lord
            Ability(
                id = ABILITY_BLOOD_DRAIN_ID,
                name = "Blood Drain",
                description = "Sinks fangs into the victim, draining their lifeblood. Deals heavy damage and heals the vampire for the full amount drained.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 18,
                effects = """[{"type":"damage","modifier":18},{"type":"lifesteal","modifier":100}]""",
                attribution = ATTRIBUTION
            ),
            // Unholy Smite - used by Death Knight
            Ability(
                id = ABILITY_UNHOLY_SMITE_ID,
                name = "Unholy Smite",
                description = "Channels dark energy through their weapon, delivering a devastating blow infused with necrotic power.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 25,
                effects = """[{"type":"damage","modifier":15},{"type":"damage","damageType":"necrotic","modifier":10}]""",
                attribution = ATTRIBUTION
            ),
            // Shadowbolt - used by Skeleton Mage
            Ability(
                id = ABILITY_SHADOWBOLT_ID,
                name = "Shadowbolt",
                description = "Hurls a bolt of concentrated shadow energy that damages and slows the target.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 40,
                cooldownType = "short",
                cooldownRounds = 1,
                baseDamage = 12,
                durationRounds = 2,
                effects = """[{"type":"damage","damageType":"necrotic","modifier":12},{"type":"condition","condition":"slowed","duration":2}]""",
                attribution = ATTRIBUTION
            ),
            // Bone Shatter - used by Bone Golem
            Ability(
                id = ABILITY_BONE_SHATTER_ID,
                name = "Bone Shatter",
                description = "The bone golem slams the ground, sending shards of bone flying in all directions, damaging all nearby enemies.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 15,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 14,
                effects = """[{"type":"damage","modifier":14,"aoe":true}]""",
                attribution = ATTRIBUTION
            ),
            // Dominate - used by Vampire Lord
            Ability(
                id = ABILITY_DOMINATE_ID,
                name = "Dominate",
                description = "The vampire's powerful will attempts to take control of a weak-minded enemy, turning them against their allies for a short time.",
                classId = null,
                abilityType = "control",
                targetType = "single_enemy",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 0,
                durationRounds = 2,
                effects = """[{"type":"condition","condition":"charmed","duration":2,"saveType":"wisdom"}]""",
                attribution = ATTRIBUTION
            ),
            // Charm Gaze - used by Vampire Spawn
            Ability(
                id = ABILITY_CHARM_GAZE_ID,
                name = "Charm Gaze",
                description = "Locks eyes with a target, attempting to beguile them and lower their defenses.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 20,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 0,
                durationRounds = 2,
                effects = """[{"type":"debuff","stat":"defense","penalty":6,"duration":2,"saveType":"wisdom"}]""",
                attribution = ATTRIBUTION
            )
        )

        abilities.forEach { ability ->
            if (AbilityRepository.findById(ability.id) == null) {
                AbilityRepository.create(ability)
            }
        }
    }

    private fun seedItems() {
        val items = listOf(
            // ===== CONSUMABLES =====
            Item(
                id = HOLY_WATER_VIAL_ID,
                name = "Holy Water Vial",
                desc = "Water blessed by a cleric of a good deity. Deals extra damage to undead when thrown or applied.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 25,
                attribution = ATTRIBUTION
            ),
            Item(
                id = BLESSED_BANDAGE_ID,
                name = "Blessed Bandage",
                desc = "A bandage infused with divine healing energy. Restores health and removes curse effects.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 40,
                attribution = ATTRIBUTION
            ),
            Item(
                id = RESTORATION_ELIXIR_ID,
                name = "Restoration Elixir",
                desc = "A powerful elixir that removes level drain and negative energy effects, restoring the drinker to full vitality.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 100,
                attribution = ATTRIBUTION
            ),
            Item(
                id = TURN_UNDEAD_SCROLL_ID,
                name = "Scroll of Turn Undead",
                desc = "A sacred scroll that, when read aloud, channels divine energy to repel or destroy undead creatures.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "consumable",
                equipmentSlot = null,
                statBonuses = null,
                value = 150,
                attribution = ATTRIBUTION
            ),

            // ===== WEAPONS =====
            Item(
                id = SILVER_LONGSWORD_ID,
                name = "Silver Longsword",
                desc = "A longsword with a blade of pure silver. Especially effective against undead and lycanthropes.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 7, defense = 0, maxHp = 0),
                value = 150,
                attribution = ATTRIBUTION
            ),
            Item(
                id = MACE_OF_DISRUPTION_ID,
                name = "Mace of Disruption",
                desc = "A holy weapon blessed to channel positive energy. Undead struck by this weapon may be instantly destroyed.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 10, defense = 0, maxHp = 5),
                value = 450,
                attribution = ATTRIBUTION
            ),
            Item(
                id = HOLY_AVENGER_ID,
                name = "Holy Avenger",
                desc = "A legendary paladin's blade that glows with divine radiance. Devastating against evil creatures and provides protection to allies.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 14, defense = 2, maxHp = 10),
                value = 800,
                attribution = ATTRIBUTION
            ),
            Item(
                id = BONE_BOW_ID,
                name = "Bone Bow",
                desc = "A bow crafted from the bones of a giant. Its arrows are imbued with necrotic energy.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 8, defense = 0, maxHp = 0),
                value = 175,
                attribution = ATTRIBUTION
            ),
            Item(
                id = DEATH_KNIGHTS_BLADE_ID,
                name = "Death Knight's Blade",
                desc = "A cursed greatsword wreathed in dark flames. Powerful but corrupting to those not aligned with darkness.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 16, defense = -2, maxHp = 0),
                value = 600,
                attribution = ATTRIBUTION
            ),

            // ===== ARMOR =====
            Item(
                id = BLESSED_CHAINMAIL_ID,
                name = "Blessed Chainmail",
                desc = "Chainmail armor that has been consecrated by holy rituals. Provides resistance against necrotic damage.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 0, defense = 7, maxHp = 10),
                value = 250,
                attribution = ATTRIBUTION
            ),
            Item(
                id = DEATH_KNIGHTS_PLATE_ID,
                name = "Death Knight's Plate",
                desc = "Black full plate armor radiating an aura of despair. Grants tremendous protection at the cost of one's soul.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 2, defense = 12, maxHp = 20),
                value = 750,
                attribution = ATTRIBUTION
            ),
            Item(
                id = CLOAK_OF_SHADOWS_ID,
                name = "Cloak of Shadows",
                desc = "A cloak woven from pure shadow. Grants the ability to blend into darkness and resist negative energy.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "back",
                statBonuses = StatBonuses(attack = 0, defense = 4, maxHp = 5),
                value = 300,
                attribution = ATTRIBUTION
            ),

            // ===== ACCESSORIES =====
            Item(
                id = AMULET_OF_LIFE_PROTECTION_ID,
                name = "Amulet of Life Protection",
                desc = "A powerful protective amulet that guards against death effects and level drain.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "neck",
                statBonuses = StatBonuses(attack = 0, defense = 2, maxHp = 25),
                value = 550,
                attribution = ATTRIBUTION
            ),
            Item(
                id = RING_OF_POSITIVE_ENERGY_ID,
                name = "Ring of Positive Energy",
                desc = "A golden ring that channels positive energy, enhancing healing received and harming undead who touch the wearer.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "finger",
                statBonuses = StatBonuses(attack = 0, defense = 0, maxHp = 15),
                value = 350,
                attribution = ATTRIBUTION
            ),
            Item(
                id = PHYLACTERY_SHARD_ID,
                name = "Phylactery Shard",
                desc = "A fragment of a lich's phylactery. Radiates powerful dark magic and is highly valued by necromancers.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 500,
                attribution = ATTRIBUTION
            ),

            // ===== TREASURE =====
            Item(
                id = ANCIENT_TOMB_GOLD_ID,
                name = "Ancient Tomb Gold",
                desc = "Gold coins bearing the likeness of long-dead kings, taken from burial chambers deep underground.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 75,
                attribution = ATTRIBUTION
            ),
            Item(
                id = VAMPIRE_FANG_ID,
                name = "Vampire Fang",
                desc = "A fang extracted from a slain vampire. Alchemists use them to brew potions of dark power.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 150,
                attribution = ATTRIBUTION
            ),
            Item(
                id = SOUL_GEM_ID,
                name = "Soul Gem",
                desc = "A gemstone that contains a trapped soul. It pulses with eerie light and whispers can sometimes be heard from within.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 400,
                attribution = ATTRIBUTION
            )
        )

        items.forEach { item ->
            if (ItemRepository.findById(item.id) == null) {
                ItemRepository.create(item)
            }
        }
    }

    private fun seedLootTables() {
        val lootTables = listOf(
            LootTableData(
                id = LOOT_TABLE_ZOMBIE_ID,
                name = "Crypt Zombie Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_TOMB_GOLD_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_SKELETON_UNDEAD_ID,
                name = "Skeletal Archer Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_TOMB_GOLD_ID, chance = 0.15f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = BONE_BOW_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_GHAST_ID,
                name = "Ghast Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_TOMB_GOLD_ID, chance = 0.25f, minQty = 1, maxQty = 2),
                    LootEntry(itemId = BLESSED_BANDAGE_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_WIGHT_ID,
                name = "Wight Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_TOMB_GOLD_ID, chance = 0.35f, minQty = 2, maxQty = 3),
                    LootEntry(itemId = SILVER_LONGSWORD_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = RESTORATION_ELIXIR_ID, chance = 0.08f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_MUMMY_ID,
                name = "Mummy Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_TOMB_GOLD_ID, chance = 0.50f, minQty = 3, maxQty = 5),
                    LootEntry(itemId = SOUL_GEM_ID, chance = 0.10f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = BLESSED_CHAINMAIL_ID, chance = 0.08f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_VAMPIRE_SPAWN_ID,
                name = "Vampire Spawn Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_TOMB_GOLD_ID, chance = 0.45f, minQty = 2, maxQty = 4),
                    LootEntry(itemId = VAMPIRE_FANG_ID, chance = 0.40f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = CLOAK_OF_SHADOWS_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_DEATH_KNIGHT_ID,
                name = "Death Knight Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_TOMB_GOLD_ID, chance = 0.75f, minQty = 5, maxQty = 10),
                    LootEntry(itemId = DEATH_KNIGHTS_BLADE_ID, chance = 0.30f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = DEATH_KNIGHTS_PLATE_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = SOUL_GEM_ID, chance = 0.25f, minQty = 1, maxQty = 2)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_VAMPIRE_LORD_ID,
                name = "Vampire Lord Loot",
                entries = listOf(
                    LootEntry(itemId = ANCIENT_TOMB_GOLD_ID, chance = 1.0f, minQty = 8, maxQty = 15),
                    LootEntry(itemId = VAMPIRE_FANG_ID, chance = 1.0f, minQty = 2, maxQty = 3),
                    LootEntry(itemId = HOLY_AVENGER_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = AMULET_OF_LIFE_PROTECTION_ID, chance = 0.20f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = PHYLACTERY_SHARD_ID, chance = 0.25f, minQty = 1, maxQty = 1)
                )
            )
        )

        lootTables.forEach { lootTable ->
            if (LootTableRepository.findById(lootTable.id) == null) {
                LootTableRepository.create(lootTable)
            }
        }
    }

    private fun seedCreatures() {
        val creatures = listOf(
            // ===== TIER 1 (CR 1) - Levels 1-3 =====
            Creature(
                id = ZOMBIE_SHAMBLER_ID,
                name = "Zombie Shambler",
                desc = "A recently risen corpse that moves with jerky, uncoordinated motions. Its flesh is grey and rotting, and it moans constantly with mindless hunger.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 28,
                baseDamage = 7,
                abilityIds = emptyList(),
                level = 1,
                experienceValue = 20,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_ZOMBIE_ID,
                minGoldDrop = 2,
                maxGoldDrop = 8,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = SKELETAL_ARCHER_ID,
                name = "Skeletal Archer",
                desc = "A skeleton equipped with a bone bow, still possessing enough of its former skill to fire arrows with deadly accuracy. Blue flames burn in its empty eye sockets.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 18,
                baseDamage = 9,
                abilityIds = emptyList(),
                level = 1,
                experienceValue = 25,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_SKELETON_UNDEAD_ID,
                minGoldDrop = 3,
                maxGoldDrop = 10,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GHAST_ID,
                name = "Ghast",
                desc = "A more powerful variant of ghoul, this creature emanates a nauseating stench. Its paralytic touch is even more potent, and it retains some cunning from life.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 35,
                baseDamage = 10,
                abilityIds = listOf("ability-paralyzing-touch"),
                level = 2,
                experienceValue = 40,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GHAST_ID,
                minGoldDrop = 5,
                maxGoldDrop = 15,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 2 (CR 2-3) - Levels 4-6 =====
            Creature(
                id = WIGHT_ID,
                name = "Wight",
                desc = "An undead creature created from a warrior's corpse. It retains combat skill and intelligence, using weapons with deadly precision. Its touch drains life force.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 55,
                baseDamage = 12,
                abilityIds = listOf(ABILITY_NECROTIC_TOUCH_ID),
                level = 3,
                experienceValue = 65,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_WIGHT_ID,
                minGoldDrop = 12,
                maxGoldDrop = 30,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = SPECTER_ID,
                name = "Specter",
                desc = "An incorporeal undead formed from a creature that died in anguish. It exists as a wraith-like shadow, its touch sapping the life from the living.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 42,
                baseDamage = 11,
                abilityIds = listOf(ABILITY_NECROTIC_TOUCH_ID, ABILITY_FEAR_AURA_ID),
                level = 3,
                experienceValue = 70,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GHAST_ID,
                minGoldDrop = 10,
                maxGoldDrop = 25,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = MUMMY_ID,
                name = "Mummy",
                desc = "An ancient corpse preserved through dark rituals and wrapped in moldering bandages. It guards its tomb with unwavering vigilance, and its rotting touch carries a terrible curse.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 70,
                baseDamage = 14,
                abilityIds = listOf(ABILITY_MUMMY_ROT_ID, ABILITY_FEAR_AURA_ID),
                level = 4,
                experienceValue = 85,
                challengeRating = 3,
                isAggressive = true,
                lootTableId = LOOT_TABLE_MUMMY_ID,
                minGoldDrop = 20,
                maxGoldDrop = 50,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = SKELETON_MAGE_ID,
                name = "Skeleton Mage",
                desc = "A spellcaster in life, this skeleton retained its magical abilities in undeath. It wields dark magic and can animate other corpses to serve it.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 45,
                baseDamage = 8,
                abilityIds = listOf(ABILITY_SHADOWBOLT_ID, ABILITY_RAISE_DEAD_ID),
                level = 4,
                experienceValue = 80,
                challengeRating = 3,
                isAggressive = true,
                lootTableId = LOOT_TABLE_WIGHT_ID,
                minGoldDrop = 15,
                maxGoldDrop = 40,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 3 (CR 4-5) - Levels 7-10 =====
            Creature(
                id = BONE_GOLEM_ID,
                name = "Bone Golem",
                desc = "A massive construct assembled from hundreds of bones, bound together by dark magic. It towers over most creatures and attacks with devastating force.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 110,
                baseDamage = 17,
                abilityIds = listOf(ABILITY_BONE_SHATTER_ID),
                level = 6,
                experienceValue = 130,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_MUMMY_ID,
                minGoldDrop = 25,
                maxGoldDrop = 60,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = VAMPIRE_SPAWN_ID,
                name = "Vampire Spawn",
                desc = "A lesser vampire created by a vampire lord. While not as powerful as its master, it possesses supernatural speed, charm, and an insatiable thirst for blood.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 85,
                baseDamage = 15,
                abilityIds = listOf(ABILITY_BLOOD_DRAIN_ID, ABILITY_CHARM_GAZE_ID),
                level = 5,
                experienceValue = 115,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_VAMPIRE_SPAWN_ID,
                minGoldDrop = 30,
                maxGoldDrop = 70,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = DEATH_KNIGHT_ID,
                name = "Death Knight",
                desc = "Once a noble paladin, this warrior was cursed to eternal undeath for breaking their sacred oaths. Clad in blackened plate armor, they wield fell powers and formidable combat skill.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 125,
                baseDamage = 20,
                abilityIds = listOf(ABILITY_UNHOLY_SMITE_ID, ABILITY_FEAR_AURA_ID, ABILITY_RAISE_DEAD_ID),
                level = 7,
                experienceValue = 175,
                challengeRating = 5,
                isAggressive = true,
                lootTableId = LOOT_TABLE_DEATH_KNIGHT_ID,
                minGoldDrop = 50,
                maxGoldDrop = 120,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 4 (CR 6+) - Boss =====
            Creature(
                id = VAMPIRE_LORD_ID,
                name = "Vampire Lord",
                desc = "An ancient and powerful vampire who has existed for centuries. With supernatural strength, mesmerizing presence, and mastery of dark magic, it rules over its crypt with iron will.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 180,
                baseDamage = 24,
                abilityIds = listOf(ABILITY_BLOOD_DRAIN_ID, ABILITY_DOMINATE_ID, ABILITY_FEAR_AURA_ID),
                level = 9,
                experienceValue = 300,
                challengeRating = 6,
                isAggressive = true,
                lootTableId = LOOT_TABLE_VAMPIRE_LORD_ID,
                minGoldDrop = 150,
                maxGoldDrop = 350,
                attribution = ATTRIBUTION
            )
        )

        creatures.forEach { creature ->
            if (CreatureRepository.findById(creature.id) == null) {
                CreatureRepository.create(creature)
            }
        }
    }

    /**
     * Generate images for all Undead Crypt entities that don't have images yet.
     */
    suspend fun generateMissingImages() {
        log.info("Starting image generation for Undead Crypt entities...")

        val creaturePrompts = mapOf(
            ZOMBIE_SHAMBLER_ID to "Zombie shambler undead, rotting grey flesh, tattered clothes, mindless hunger, dark crypt, horror fantasy art",
            SKELETAL_ARCHER_ID to "Skeleton archer, bone bow, glowing blue eye flames, rusted armor remnants, undead, dark fantasy",
            GHAST_ID to "Ghast undead creature, pale rotting flesh, long claws, putrid stench, intelligent hunger, horror fantasy",
            WIGHT_ID to "Wight undead warrior, blackened armor, glowing red eyes, undead intelligence, wielding sword, dark fantasy",
            SPECTER_ID to "Specter incorporeal undead, ghostly shadow form, translucent, anguished face, floating, horror fantasy",
            MUMMY_ID to "Ancient mummy, rotting bandages, egyptian tomb guardian, dust and decay, glowing eyes, dark fantasy",
            SKELETON_MAGE_ID to "Skeleton mage, dark robes, magical staff, purple necrotic energy, spellcaster undead, fantasy art",
            BONE_GOLEM_ID to "Bone golem construct, massive skeleton made of many bones, towering, dark magic binding, fantasy horror",
            VAMPIRE_SPAWN_ID to "Vampire spawn, pale skin, red eyes, fangs, aristocratic but feral, blood-stained, gothic horror",
            DEATH_KNIGHT_ID to "Death knight, blackened plate armor, dark fire wreathing sword, fallen paladin, undead warrior, dark fantasy",
            VAMPIRE_LORD_ID to "Vampire lord boss, ancient aristocratic vampire, ornate dark clothing, mesmerizing presence, castle throne, gothic horror"
        )

        val itemPrompts = mapOf(
            HOLY_WATER_VIAL_ID to "Holy water vial, blessed water, glowing softly, glass flask with holy symbol, fantasy item",
            BLESSED_BANDAGE_ID to "Blessed bandage roll, divine healing energy glow, white cloth with holy symbols, fantasy healing item",
            RESTORATION_ELIXIR_ID to "Restoration elixir potion, golden glowing liquid, ornate bottle, powerful magic, fantasy alchemy",
            TURN_UNDEAD_SCROLL_ID to "Turn undead scroll, sacred parchment, holy symbols, radiating light, cleric spell scroll, fantasy",
            SILVER_LONGSWORD_ID to "Silver longsword, polished blade, ornate crossguard, anti-undead weapon, moonlight gleam, fantasy",
            MACE_OF_DISRUPTION_ID to "Mace of disruption, holy weapon, glowing golden light, anti-undead mace, paladin weapon, fantasy",
            HOLY_AVENGER_ID to "Holy avenger legendary sword, divine radiance, golden blade, paladin weapon, glorious light, fantasy",
            BONE_BOW_ID to "Bone bow weapon, crafted from giant bones, necrotic energy, dark fantasy weapon, detailed",
            DEATH_KNIGHTS_BLADE_ID to "Death knight's blade, cursed greatsword, dark flames, corrupted metal, evil aura, dark fantasy",
            BLESSED_CHAINMAIL_ID to "Blessed chainmail armor, holy symbols etched, soft divine glow, protection from evil, fantasy armor",
            DEATH_KNIGHTS_PLATE_ID to "Death knight's plate armor, black full plate, despair aura, cursed armor, dark fantasy",
            CLOAK_OF_SHADOWS_ID to "Cloak of shadows, woven from shadow, dark mystical fabric, stealth magic, fantasy accessory",
            AMULET_OF_LIFE_PROTECTION_ID to "Amulet of life protection, golden pendant, protective runes, life force shield, fantasy jewelry",
            RING_OF_POSITIVE_ENERGY_ID to "Ring of positive energy, golden ring, healing light emanating, anti-undead aura, fantasy jewelry",
            PHYLACTERY_SHARD_ID to "Phylactery shard, dark crystal fragment, soul magic, necrotic glow, lich artifact, dark fantasy",
            ANCIENT_TOMB_GOLD_ID to "Ancient tomb gold coins, weathered currency, burial treasure, skeleton king's face, fantasy loot",
            VAMPIRE_FANG_ID to "Vampire fang, large ivory tooth, blood stained, dark magic component, gothic horror trophy",
            SOUL_GEM_ID to "Soul gem, glowing ethereal crystal, trapped spirit visible, dark magic artifact, fantasy treasure"
        )

        // Generate creature images
        for ((creatureId, prompt) in creaturePrompts) {
            val creature = CreatureRepository.findById(creatureId)
            if (creature != null && creature.imageUrl == null) {
                log.info("Generating image for creature: ${creature.name}")
                ImageGenerationService.generateImage(
                    entityType = "creature",
                    entityId = creatureId,
                    description = prompt,
                    entityName = creature.name
                ).onSuccess { imageUrl ->
                    CreatureRepository.updateImageUrl(creatureId, imageUrl)
                    log.info("Generated image for ${creature.name}: $imageUrl")
                }.onFailure { error ->
                    log.warn("Failed to generate image for ${creature.name}: ${error.message}")
                }
            }
        }

        // Generate item images
        for ((itemId, prompt) in itemPrompts) {
            val item = ItemRepository.findById(itemId)
            if (item != null && item.imageUrl == null) {
                log.info("Generating image for item: ${item.name}")
                ImageGenerationService.generateImage(
                    entityType = "item",
                    entityId = itemId,
                    description = prompt,
                    entityName = item.name
                ).onSuccess { imageUrl ->
                    ItemRepository.updateImageUrl(itemId, imageUrl)
                    log.info("Generated image for ${item.name}: $imageUrl")
                }.onFailure { error ->
                    log.warn("Failed to generate image for ${item.name}: ${error.message}")
                }
            }
        }

        log.info("Undead Crypt image generation complete")
    }
}
