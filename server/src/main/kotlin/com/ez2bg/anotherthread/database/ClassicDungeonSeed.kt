package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.ImageGenerationService
import org.slf4j.LoggerFactory

/**
 * Seed data for Classic Dungeon area - iconic D&D dungeon creatures and magic items.
 * Features monsters like Owlbear, Gelatinous Cube, Rust Monster, Mind Flayer, and Beholder.
 */
object ClassicDungeonSeed {
    private val log = LoggerFactory.getLogger(ClassicDungeonSeed::class.java)

    // Attribution for tracking content source
    private const val ATTRIBUTION = "Classic D&D Monsters"

    // ============== CREATURE IDS ==============
    // Tier 1 (CR 1) - Levels 1-3
    const val GIANT_CENTIPEDE_ID = "creature-giant-centipede"
    const val STIRGE_ID = "creature-stirge"
    const val RUST_MONSTER_ID = "creature-rust-monster"

    // Tier 2 (CR 2-3) - Levels 4-6
    const val OWLBEAR_ID = "creature-owlbear"
    const val CARRION_CRAWLER_ID = "creature-carrion-crawler"
    const val GELATINOUS_CUBE_ID = "creature-gelatinous-cube"
    const val DISPLACER_BEAST_ID = "creature-displacer-beast"

    // Tier 3 (CR 4-5) - Levels 7-10
    const val ETTERCAP_ID = "creature-ettercap"
    const val UMBER_HULK_ID = "creature-umber-hulk"
    const val HOOK_HORROR_ID = "creature-hook-horror"

    // Tier 4 (CR 6+) - Bosses
    const val MIND_FLAYER_ID = "creature-mind-flayer"
    const val BEHOLDER_ID = "creature-beholder"

    // ============== ABILITY IDS ==============
    const val ABILITY_TENTACLE_PARALYSIS_ID = "ability-tentacle-paralysis"
    const val ABILITY_RUST_TOUCH_ID = "ability-rust-touch"
    const val ABILITY_BLOOD_DRAIN_STIRGE_ID = "ability-blood-drain-stirge"
    const val ABILITY_DISPLACEMENT_ID = "ability-displacement"
    const val ABILITY_CONFUSING_GAZE_ID = "ability-confusing-gaze"
    const val ABILITY_MIND_BLAST_ID = "ability-mind-blast"
    const val ABILITY_EXTRACT_BRAIN_ID = "ability-extract-brain"
    const val ABILITY_ANTIMAGIC_CONE_ID = "ability-antimagic-cone"
    const val ABILITY_EYE_RAYS_ID = "ability-eye-rays"
    const val ABILITY_DISINTEGRATION_RAY_ID = "ability-disintegration-ray"
    const val ABILITY_WEB_SPRAY_ID = "ability-web-spray"
    const val ABILITY_HOOK_ATTACK_ID = "ability-hook-attack"

    // ============== ITEM IDS ==============
    // +1/+2/+3 Weapons
    const val LONGSWORD_PLUS_1_ID = "item-longsword-plus-1"
    const val LONGSWORD_PLUS_2_ID = "item-longsword-plus-2"
    const val LONGSWORD_PLUS_3_ID = "item-longsword-plus-3"
    const val MACE_PLUS_1_ID = "item-mace-plus-1"
    const val DAGGER_PLUS_2_ID = "item-dagger-plus-2"
    const val BATTLEAXE_PLUS_2_ID = "item-battleaxe-plus-2"
    const val WAND_OF_MAGIC_MISSILES_ID = "item-wand-of-magic-missiles"

    // Armor
    const val CHAINMAIL_PLUS_1_ID = "item-chainmail-plus-1"
    const val PLATE_ARMOR_PLUS_1_ID = "item-plate-armor-plus-1"
    const val SHIELD_PLUS_2_ID = "item-shield-plus-2"
    const val BRACERS_OF_DEFENSE_ID = "item-bracers-of-defense"

    // Iconic Magic Items
    const val BAG_OF_HOLDING_ID = "item-bag-of-holding"
    const val BOOTS_OF_SPEED_ID = "item-boots-of-speed"
    const val CLOAK_OF_PROTECTION_ID = "item-cloak-of-protection"
    const val ROPE_OF_CLIMBING_ID = "item-rope-of-climbing"
    const val DECANTER_OF_ENDLESS_WATER_ID = "item-decanter-of-endless-water"
    const val IMMOVABLE_ROD_ID = "item-immovable-rod"

    // Accessories
    const val RING_OF_PROTECTION_ID = "item-ring-of-protection"
    const val RING_OF_INVISIBILITY_ID = "item-ring-of-invisibility"
    const val AMULET_OF_PROOF_DETECTION_ID = "item-amulet-proof-detection"
    const val IOUN_STONE_ID = "item-ioun-stone"

    // Treasure
    const val BEHOLDER_EYE_ID = "item-beholder-eye"
    const val MIND_FLAYER_BRAIN_ID = "item-mind-flayer-brain"
    const val OWLBEAR_FEATHER_ID = "item-owlbear-feather"
    const val GELATINOUS_RESIDUE_ID = "item-gelatinous-residue"

    // ============== LOOT TABLE IDS ==============
    const val LOOT_TABLE_VERMIN_ID = "loot-dungeon-vermin"
    const val LOOT_TABLE_OWLBEAR_ID = "loot-owlbear"
    const val LOOT_TABLE_GELATINOUS_ID = "loot-gelatinous-cube"
    const val LOOT_TABLE_DISPLACER_ID = "loot-displacer-beast"
    const val LOOT_TABLE_UMBER_HULK_ID = "loot-umber-hulk"
    const val LOOT_TABLE_MIND_FLAYER_ID = "loot-mind-flayer"
    const val LOOT_TABLE_BEHOLDER_ID = "loot-beholder"

    fun seedIfEmpty() {
        val existingCreatures = CreatureRepository.findAll().map { it.id }.toSet()

        if (GIANT_CENTIPEDE_ID !in existingCreatures) {
            seedCreatureAbilities()
            seedItems()
            seedLootTables()
            seedCreatures()
            println("Seeded Classic Dungeon content (iconic D&D monsters)")
        }
    }

    private fun seedCreatureAbilities() {
        val abilities = listOf(
            // Tentacle Paralysis - used by Carrion Crawler
            Ability(
                id = ABILITY_TENTACLE_PARALYSIS_ID,
                name = "Tentacle Paralysis",
                description = "The creature's tentacles secrete a paralytic poison. Victims struck must save or become paralyzed.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 10,
                cooldownType = "medium",
                cooldownRounds = 2,
                baseDamage = 8,
                durationRounds = 2,
                effects = """[{"type":"damage","modifier":8},{"type":"condition","condition":"paralyzed","duration":2,"saveType":"constitution"}]""",
                attribution = ATTRIBUTION
            ),
            // Rust Touch - used by Rust Monster
            Ability(
                id = ABILITY_RUST_TOUCH_ID,
                name = "Rust Touch",
                description = "The creature's antennae corrode metal on contact, destroying weapons and armor that strike it.",
                classId = null,
                abilityType = "passive",
                targetType = "self",
                range = 5,
                cooldownType = "none",
                cooldownRounds = 0,
                baseDamage = 0,
                effects = """[{"type":"debuff","stat":"attack","penalty":2,"duration":5,"trigger":"melee_hit"},{"type":"debuff","stat":"defense","penalty":2,"duration":5,"trigger":"melee_hit"}]""",
                attribution = ATTRIBUTION
            ),
            // Blood Drain (Stirge) - different from vampire version
            Ability(
                id = ABILITY_BLOOD_DRAIN_STIRGE_ID,
                name = "Blood Drain",
                description = "The stirge attaches to a victim and drains blood, healing itself while dealing ongoing damage.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "short",
                cooldownRounds = 1,
                baseDamage = 6,
                durationRounds = 3,
                effects = """[{"type":"damage","modifier":6},{"type":"dot","damage":4,"duration":3},{"type":"lifesteal","modifier":50}]""",
                attribution = ATTRIBUTION
            ),
            // Displacement - used by Displacer Beast
            Ability(
                id = ABILITY_DISPLACEMENT_ID,
                name = "Displacement",
                description = "The creature projects an illusion that makes it appear to be slightly offset from its true location, causing attacks to miss.",
                classId = null,
                abilityType = "buff",
                targetType = "self",
                range = 0,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 0,
                durationRounds = 3,
                effects = """[{"type":"buff","stat":"evasion","bonus":30,"duration":3}]""",
                attribution = ATTRIBUTION
            ),
            // Confusing Gaze - used by Umber Hulk
            Ability(
                id = ABILITY_CONFUSING_GAZE_ID,
                name = "Confusing Gaze",
                description = "The umber hulk's eyes cause confusion in those who meet its gaze, potentially causing them to attack randomly.",
                classId = null,
                abilityType = "control",
                targetType = "single_enemy",
                range = 30,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 0,
                durationRounds = 2,
                effects = """[{"type":"condition","condition":"confused","duration":2,"saveType":"wisdom"}]""",
                attribution = ATTRIBUTION
            ),
            // Mind Blast - used by Mind Flayer
            Ability(
                id = ABILITY_MIND_BLAST_ID,
                name = "Mind Blast",
                description = "The mind flayer unleashes a devastating cone of psychic energy that stuns and damages all creatures in the area.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 30,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 22,
                durationRounds = 2,
                effects = """[{"type":"damage","damageType":"psychic","modifier":22,"aoe":true},{"type":"condition","condition":"stunned","duration":2,"saveType":"intelligence"}]""",
                attribution = ATTRIBUTION
            ),
            // Extract Brain - used by Mind Flayer
            Ability(
                id = ABILITY_EXTRACT_BRAIN_ID,
                name = "Extract Brain",
                description = "The mind flayer attempts to extract and consume the brain of a stunned or grappled victim, dealing massive damage.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 5,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 45,
                effects = """[{"type":"damage","damageType":"psychic","modifier":45,"requireCondition":"stunned"}]""",
                attribution = ATTRIBUTION
            ),
            // Antimagic Cone - used by Beholder
            Ability(
                id = ABILITY_ANTIMAGIC_CONE_ID,
                name = "Antimagic Cone",
                description = "The beholder's central eye projects a cone of antimagic, suppressing all magical effects and preventing spellcasting.",
                classId = null,
                abilityType = "control",
                targetType = "all_enemies",
                range = 40,
                cooldownType = "long",
                cooldownRounds = 4,
                baseDamage = 0,
                durationRounds = 2,
                effects = """[{"type":"condition","condition":"silenced","duration":2},{"type":"dispel","removeBuffs":true}]""",
                attribution = ATTRIBUTION
            ),
            // Eye Rays - used by Beholder
            Ability(
                id = ABILITY_EYE_RAYS_ID,
                name = "Eye Rays",
                description = "The beholder fires multiple magical rays from its eyestalks, each with a different devastating effect.",
                classId = null,
                abilityType = "combat",
                targetType = "all_enemies",
                range = 40,
                cooldownType = "short",
                cooldownRounds = 1,
                baseDamage = 18,
                effects = """[{"type":"damage","modifier":18,"targets":3},{"type":"random_effect","effects":["fear","slow","charm","petrify"]}]""",
                attribution = ATTRIBUTION
            ),
            // Disintegration Ray - used by Beholder
            Ability(
                id = ABILITY_DISINTEGRATION_RAY_ID,
                name = "Disintegration Ray",
                description = "The beholder fires a thin green ray that disintegrates matter, dealing catastrophic damage to a single target.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 50,
                cooldownType = "long",
                cooldownRounds = 5,
                baseDamage = 55,
                effects = """[{"type":"damage","modifier":55,"saveType":"dexterity","saveHalf":true}]""",
                attribution = ATTRIBUTION
            ),
            // Web Spray - used by Ettercap
            Ability(
                id = ABILITY_WEB_SPRAY_ID,
                name = "Web Spray",
                description = "The creature sprays sticky webbing that entangles and restrains enemies, preventing movement.",
                classId = null,
                abilityType = "control",
                targetType = "all_enemies",
                range = 20,
                cooldownType = "medium",
                cooldownRounds = 3,
                baseDamage = 5,
                durationRounds = 2,
                effects = """[{"type":"damage","modifier":5,"aoe":true},{"type":"condition","condition":"restrained","duration":2,"saveType":"strength"}]""",
                attribution = ATTRIBUTION
            ),
            // Hook Attack - used by Hook Horror
            Ability(
                id = ABILITY_HOOK_ATTACK_ID,
                name = "Hook Attack",
                description = "The hook horror strikes with both of its massive hook-like claws in a devastating double attack.",
                classId = null,
                abilityType = "combat",
                targetType = "single_enemy",
                range = 10,
                cooldownType = "short",
                cooldownRounds = 1,
                baseDamage = 24,
                effects = """[{"type":"damage","modifier":12},{"type":"damage","modifier":12}]""",
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
            // ===== +1/+2/+3 WEAPONS =====
            Item(
                id = LONGSWORD_PLUS_1_ID,
                name = "Longsword +1",
                desc = "A finely crafted longsword enhanced with minor enchantments that improve its striking power.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 6, defense = 0, maxHp = 0),
                value = 150,
                attribution = ATTRIBUTION
            ),
            Item(
                id = LONGSWORD_PLUS_2_ID,
                name = "Longsword +2",
                desc = "A superior longsword bearing powerful enchantments that significantly enhance its effectiveness in combat.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 10, defense = 0, maxHp = 0),
                value = 400,
                attribution = ATTRIBUTION
            ),
            Item(
                id = LONGSWORD_PLUS_3_ID,
                name = "Longsword +3",
                desc = "A legendary longsword infused with the strongest enchantments, making it a devastating weapon in skilled hands.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 14, defense = 0, maxHp = 5),
                value = 800,
                attribution = ATTRIBUTION
            ),
            Item(
                id = MACE_PLUS_1_ID,
                name = "Mace +1",
                desc = "A sturdy mace enhanced with magical power, delivering crushing blows with increased force.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 5, defense = 0, maxHp = 5),
                value = 125,
                attribution = ATTRIBUTION
            ),
            Item(
                id = DAGGER_PLUS_2_ID,
                name = "Dagger +2",
                desc = "A wickedly sharp dagger that seems to find gaps in armor with uncanny accuracy.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 8, defense = 0, maxHp = 0),
                value = 300,
                attribution = ATTRIBUTION
            ),
            Item(
                id = BATTLEAXE_PLUS_2_ID,
                name = "Battleaxe +2",
                desc = "A heavy battleaxe enchanted to cleave through armor and bone with terrifying ease.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 12, defense = 0, maxHp = 0),
                value = 450,
                attribution = ATTRIBUTION
            ),
            Item(
                id = WAND_OF_MAGIC_MISSILES_ID,
                name = "Wand of Magic Missiles",
                desc = "A slender wand that can fire unerring bolts of magical force at enemies.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = 7, defense = 0, maxHp = 0),
                value = 350,
                attribution = ATTRIBUTION
            ),

            // ===== ARMOR =====
            Item(
                id = CHAINMAIL_PLUS_1_ID,
                name = "Chainmail +1",
                desc = "Finely crafted chainmail armor enhanced with protective enchantments.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 0, defense = 6, maxHp = 10),
                value = 200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = PLATE_ARMOR_PLUS_1_ID,
                name = "Plate Armor +1",
                desc = "Heavy plate armor reinforced with magical wards that turn aside blows.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "chest",
                statBonuses = StatBonuses(attack = 0, defense = 10, maxHp = 15),
                value = 500,
                attribution = ATTRIBUTION
            ),
            Item(
                id = SHIELD_PLUS_2_ID,
                name = "Shield +2",
                desc = "A sturdy shield bearing powerful protective enchantments that deflect attacks.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "off_hand",
                statBonuses = StatBonuses(attack = 0, defense = 8, maxHp = 5),
                value = 350,
                attribution = ATTRIBUTION
            ),
            Item(
                id = BRACERS_OF_DEFENSE_ID,
                name = "Bracers of Defense",
                desc = "Enchanted bracers that create an invisible barrier, protecting the wearer from harm.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "hands",
                statBonuses = StatBonuses(attack = 0, defense = 5, maxHp = 10),
                value = 400,
                attribution = ATTRIBUTION
            ),

            // ===== ICONIC MAGIC ITEMS =====
            Item(
                id = BAG_OF_HOLDING_ID,
                name = "Bag of Holding",
                desc = "This bag has an interior space considerably larger than its outside dimensions. It can hold far more than should be physically possible.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = null,
                statBonuses = null,
                value = 500,
                attribution = ATTRIBUTION
            ),
            Item(
                id = BOOTS_OF_SPEED_ID,
                name = "Boots of Speed",
                desc = "While wearing these boots, you can click your heels together to double your walking speed.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "armor",
                equipmentSlot = "feet",
                statBonuses = StatBonuses(attack = 2, defense = 2, maxHp = 0),
                value = 600,
                attribution = ATTRIBUTION
            ),
            Item(
                id = CLOAK_OF_PROTECTION_ID,
                name = "Cloak of Protection",
                desc = "A magical cloak that provides a bonus to armor and saving throws.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "back",
                statBonuses = StatBonuses(attack = 0, defense = 4, maxHp = 10),
                value = 350,
                attribution = ATTRIBUTION
            ),
            Item(
                id = ROPE_OF_CLIMBING_ID,
                name = "Rope of Climbing",
                desc = "This 60-foot length of silk rope can magically knot itself and move on command.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 200,
                attribution = ATTRIBUTION
            ),
            Item(
                id = DECANTER_OF_ENDLESS_WATER_ID,
                name = "Decanter of Endless Water",
                desc = "This stoppered flask produces a stream of fresh or salt water when commanded.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 300,
                attribution = ATTRIBUTION
            ),
            Item(
                id = IMMOVABLE_ROD_ID,
                name = "Immovable Rod",
                desc = "This flat iron rod has a button on one end. Pressing the button magically fixes the rod in place.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 400,
                attribution = ATTRIBUTION
            ),

            // ===== ACCESSORIES =====
            Item(
                id = RING_OF_PROTECTION_ID,
                name = "Ring of Protection",
                desc = "A magical ring that creates a protective field around the wearer.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "ring",
                statBonuses = StatBonuses(attack = 0, defense = 3, maxHp = 10),
                value = 300,
                attribution = ATTRIBUTION
            ),
            Item(
                id = RING_OF_INVISIBILITY_ID,
                name = "Ring of Invisibility",
                desc = "While wearing this ring, you can turn invisible at will. The invisibility ends if you attack or cast a spell.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "ring",
                statBonuses = StatBonuses(attack = 3, defense = 3, maxHp = 0),
                value = 800,
                attribution = ATTRIBUTION
            ),
            Item(
                id = AMULET_OF_PROOF_DETECTION_ID,
                name = "Amulet of Proof Against Detection",
                desc = "While wearing this amulet, you are hidden from divination magic and cannot be targeted by such magic or perceived through magical scrying sensors.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "neck",
                statBonuses = StatBonuses(attack = 0, defense = 2, maxHp = 15),
                value = 450,
                attribution = ATTRIBUTION
            ),
            Item(
                id = IOUN_STONE_ID,
                name = "Ioun Stone",
                desc = "A small magical stone that orbits your head, granting various benefits depending on its color and shape.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = "accessory",
                equipmentSlot = "head",
                statBonuses = StatBonuses(attack = 2, defense = 2, maxHp = 10),
                value = 500,
                attribution = ATTRIBUTION
            ),

            // ===== TREASURE =====
            Item(
                id = BEHOLDER_EYE_ID,
                name = "Beholder Eye",
                desc = "A preserved eye from a slain beholder. It still retains a faint magical glow and is highly prized by wizards.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 500,
                attribution = ATTRIBUTION
            ),
            Item(
                id = MIND_FLAYER_BRAIN_ID,
                name = "Mind Flayer Brain",
                desc = "The preserved brain of a mind flayer. It still pulses with residual psionic energy.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 350,
                attribution = ATTRIBUTION
            ),
            Item(
                id = OWLBEAR_FEATHER_ID,
                name = "Owlbear Feather",
                desc = "A large feather from an owlbear's plumage. Used in various alchemical recipes.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 50,
                attribution = ATTRIBUTION
            ),
            Item(
                id = GELATINOUS_RESIDUE_ID,
                name = "Gelatinous Residue",
                desc = "Acidic gel collected from a gelatinous cube. Handle with care.",
                featureIds = emptyList(),
                abilityIds = emptyList(),
                equipmentType = null,
                equipmentSlot = null,
                statBonuses = null,
                value = 75,
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
                id = LOOT_TABLE_VERMIN_ID,
                name = "Dungeon Vermin Loot",
                entries = listOf(
                    LootEntry(itemId = GELATINOUS_RESIDUE_ID, chance = 0.15f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_OWLBEAR_ID,
                name = "Owlbear Loot",
                entries = listOf(
                    LootEntry(itemId = OWLBEAR_FEATHER_ID, chance = 0.50f, minQty = 1, maxQty = 3),
                    LootEntry(itemId = LONGSWORD_PLUS_1_ID, chance = 0.10f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = CHAINMAIL_PLUS_1_ID, chance = 0.08f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_GELATINOUS_ID,
                name = "Gelatinous Cube Loot",
                entries = listOf(
                    LootEntry(itemId = GELATINOUS_RESIDUE_ID, chance = 1.0f, minQty = 2, maxQty = 4),
                    LootEntry(itemId = LONGSWORD_PLUS_1_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = RING_OF_PROTECTION_ID, chance = 0.10f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = BAG_OF_HOLDING_ID, chance = 0.05f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_DISPLACER_ID,
                name = "Displacer Beast Loot",
                entries = listOf(
                    LootEntry(itemId = CLOAK_OF_PROTECTION_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = DAGGER_PLUS_2_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_UMBER_HULK_ID,
                name = "Umber Hulk Loot",
                entries = listOf(
                    LootEntry(itemId = BATTLEAXE_PLUS_2_ID, chance = 0.12f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = PLATE_ARMOR_PLUS_1_ID, chance = 0.08f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = BRACERS_OF_DEFENSE_ID, chance = 0.10f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_MIND_FLAYER_ID,
                name = "Mind Flayer Loot",
                entries = listOf(
                    LootEntry(itemId = MIND_FLAYER_BRAIN_ID, chance = 0.75f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = AMULET_OF_PROOF_DETECTION_ID, chance = 0.20f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = RING_OF_INVISIBILITY_ID, chance = 0.10f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = IOUN_STONE_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = WAND_OF_MAGIC_MISSILES_ID, chance = 0.12f, minQty = 1, maxQty = 1)
                )
            ),
            LootTableData(
                id = LOOT_TABLE_BEHOLDER_ID,
                name = "Beholder Loot",
                entries = listOf(
                    LootEntry(itemId = BEHOLDER_EYE_ID, chance = 1.0f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = LONGSWORD_PLUS_3_ID, chance = 0.20f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = RING_OF_INVISIBILITY_ID, chance = 0.15f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = IOUN_STONE_ID, chance = 0.25f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = BOOTS_OF_SPEED_ID, chance = 0.12f, minQty = 1, maxQty = 1),
                    LootEntry(itemId = SHIELD_PLUS_2_ID, chance = 0.10f, minQty = 1, maxQty = 1)
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
                id = GIANT_CENTIPEDE_ID,
                name = "Giant Centipede",
                desc = "A massive centipede the size of a dog, with dozens of skittering legs and venomous mandibles. It hunts in dark places, striking from the shadows.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 14,
                baseDamage = 5,
                abilityIds = emptyList(),
                level = 1,
                experienceValue = 15,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_VERMIN_ID,
                minGoldDrop = 1,
                maxGoldDrop = 5,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = STIRGE_ID,
                name = "Stirge",
                desc = "A mosquito-like creature the size of a cat, with four wings, a long proboscis, and an insatiable thirst for blood.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 10,
                baseDamage = 4,
                abilityIds = listOf(ABILITY_BLOOD_DRAIN_STIRGE_ID),
                level = 1,
                experienceValue = 20,
                challengeRating = 1,
                isAggressive = true,
                lootTableId = LOOT_TABLE_VERMIN_ID,
                minGoldDrop = 0,
                maxGoldDrop = 3,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = RUST_MONSTER_ID,
                name = "Rust Monster",
                desc = "An insectoid creature with a propeller-like tail and feathery antennae. It feeds on metal, corroding weapons and armor on contact.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 28,
                baseDamage = 6,
                abilityIds = listOf(ABILITY_RUST_TOUCH_ID),
                level = 2,
                experienceValue = 35,
                challengeRating = 1,
                isAggressive = false,
                lootTableId = LOOT_TABLE_VERMIN_ID,
                minGoldDrop = 0,
                maxGoldDrop = 5,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 2 (CR 2-3) - Levels 4-6 =====
            Creature(
                id = OWLBEAR_ID,
                name = "Owlbear",
                desc = "A monstrous hybrid with the body of a bear and the head of an owl. It is a fearsome predator known for its terrible screech and savage claws.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 65,
                baseDamage = 13,
                abilityIds = emptyList(),
                level = 4,
                experienceValue = 75,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_OWLBEAR_ID,
                minGoldDrop = 10,
                maxGoldDrop = 25,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = CARRION_CRAWLER_ID,
                name = "Carrion Crawler",
                desc = "A large centipede-like creature with eight tentacles around its mouth that paralyze prey. It feeds on carrion but will attack living creatures.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 55,
                baseDamage = 10,
                abilityIds = listOf(ABILITY_TENTACLE_PARALYSIS_ID),
                level = 4,
                experienceValue = 70,
                challengeRating = 2,
                isAggressive = true,
                lootTableId = LOOT_TABLE_VERMIN_ID,
                minGoldDrop = 5,
                maxGoldDrop = 15,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = GELATINOUS_CUBE_ID,
                name = "Gelatinous Cube",
                desc = "A nearly transparent cube of ooze that fills dungeon corridors, slowly digesting everything in its path. Bones and treasure float within its mass.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 85,
                baseDamage = 11,
                abilityIds = emptyList(),
                level = 4,
                experienceValue = 80,
                challengeRating = 3,
                isAggressive = true,
                lootTableId = LOOT_TABLE_GELATINOUS_ID,
                minGoldDrop = 15,
                maxGoldDrop = 40,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = DISPLACER_BEAST_ID,
                name = "Displacer Beast",
                desc = "A great cat-like creature with six legs and two tentacles growing from its shoulders. It projects an illusion to make it appear slightly away from its true location.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 60,
                baseDamage = 12,
                abilityIds = listOf(ABILITY_DISPLACEMENT_ID),
                level = 4,
                experienceValue = 85,
                challengeRating = 3,
                isAggressive = true,
                lootTableId = LOOT_TABLE_DISPLACER_ID,
                minGoldDrop = 12,
                maxGoldDrop = 30,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 3 (CR 4-5) - Levels 7-10 =====
            Creature(
                id = ETTERCAP_ID,
                name = "Ettercap",
                desc = "A spider-like humanoid that lurks in webbed lairs. It breeds and commands giant spiders, spinning webs to trap unwary prey.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 70,
                baseDamage = 14,
                abilityIds = listOf(ABILITY_WEB_SPRAY_ID),
                level = 5,
                experienceValue = 100,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_DISPLACER_ID,
                minGoldDrop = 15,
                maxGoldDrop = 35,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = UMBER_HULK_ID,
                name = "Umber Hulk",
                desc = "A massive beetle-like creature with powerful claws that can tunnel through solid rock. Its multifaceted eyes cause confusion in those who meet its gaze.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 110,
                baseDamage = 17,
                abilityIds = listOf(ABILITY_CONFUSING_GAZE_ID),
                level = 6,
                experienceValue = 145,
                challengeRating = 5,
                isAggressive = true,
                lootTableId = LOOT_TABLE_UMBER_HULK_ID,
                minGoldDrop = 25,
                maxGoldDrop = 60,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = HOOK_HORROR_ID,
                name = "Hook Horror",
                desc = "A bizarre creature with a vulture-like head and arms ending in massive hooks. It communicates through clicking sounds and hunts in packs.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 90,
                baseDamage = 16,
                abilityIds = listOf(ABILITY_HOOK_ATTACK_ID),
                level = 5,
                experienceValue = 120,
                challengeRating = 4,
                isAggressive = true,
                lootTableId = LOOT_TABLE_UMBER_HULK_ID,
                minGoldDrop = 20,
                maxGoldDrop = 50,
                attribution = ATTRIBUTION
            ),

            // ===== TIER 4 (CR 6+) - Bosses =====
            Creature(
                id = MIND_FLAYER_ID,
                name = "Mind Flayer",
                desc = "A terrifying aberration with a humanoid body and an octopus-like head. It feeds on brains and possesses devastating psionic powers.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 130,
                baseDamage = 18,
                abilityIds = listOf(ABILITY_MIND_BLAST_ID, ABILITY_EXTRACT_BRAIN_ID),
                level = 8,
                experienceValue = 250,
                challengeRating = 6,
                isAggressive = true,
                lootTableId = LOOT_TABLE_MIND_FLAYER_ID,
                minGoldDrop = 75,
                maxGoldDrop = 200,
                attribution = ATTRIBUTION
            ),
            Creature(
                id = BEHOLDER_ID,
                name = "Beholder",
                desc = "A floating sphere dominated by a central eye and mouth, with smaller eyes on stalks. Each eye can fire a different magical ray. Paranoid and supremely arrogant.",
                itemIds = emptyList(),
                featureIds = emptyList(),
                maxHp = 180,
                baseDamage = 22,
                abilityIds = listOf(ABILITY_ANTIMAGIC_CONE_ID, ABILITY_EYE_RAYS_ID, ABILITY_DISINTEGRATION_RAY_ID),
                level = 10,
                experienceValue = 400,
                challengeRating = 7,
                isAggressive = true,
                lootTableId = LOOT_TABLE_BEHOLDER_ID,
                minGoldDrop = 200,
                maxGoldDrop = 500,
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
     * Generate images for all Classic Dungeon entities that don't have images yet.
     */
    suspend fun generateMissingImages() {
        log.info("Starting image generation for Classic Dungeon entities...")

        val creaturePrompts = mapOf(
            GIANT_CENTIPEDE_ID to "Giant centipede monster, massive segmented insect, dozens of legs, venomous mandibles, dark dungeon, fantasy horror",
            STIRGE_ID to "Stirge monster, mosquito-like creature, four wings, long blood-sucking proboscis, bat-like, fantasy horror",
            RUST_MONSTER_ID to "Rust monster, insectoid creature, propeller tail, feathery antennae, corroding metal, D&D monster, fantasy art",
            OWLBEAR_ID to "Owlbear hybrid monster, bear body with owl head, feathered, fierce claws, forest predator, fantasy art",
            CARRION_CRAWLER_ID to "Carrion crawler monster, large centipede with tentacles around mouth, paralyzing touch, dungeon crawler, fantasy horror",
            GELATINOUS_CUBE_ID to "Gelatinous cube ooze monster, transparent cubic blob, floating bones and treasure inside, dungeon corridor, fantasy art",
            DISPLACER_BEAST_ID to "Displacer beast monster, six-legged panther, two tentacles on shoulders, blurry displacement effect, D&D, fantasy art",
            ETTERCAP_ID to "Ettercap spider-humanoid, webbed lair, spider-like features, controlling giant spiders, dark fantasy horror",
            UMBER_HULK_ID to "Umber hulk monster, massive beetle creature, powerful digging claws, hypnotic multifaceted eyes, underground, fantasy art",
            HOOK_HORROR_ID to "Hook horror monster, vulture head, massive hook-shaped claws, chitinous armor, Underdark creature, fantasy art",
            MIND_FLAYER_ID to "Mind flayer illithid, humanoid with octopus head, four face tentacles, psionic power, robed, dark fantasy horror",
            BEHOLDER_ID to "Beholder monster boss, floating eye sphere, central eye and mouth, multiple eye stalks, paranoid expression, D&D, epic fantasy art"
        )

        val itemPrompts = mapOf(
            LONGSWORD_PLUS_1_ID to "Enchanted longsword +1, glowing blue runes, magical weapon, fantasy art",
            LONGSWORD_PLUS_2_ID to "Enchanted longsword +2, bright magical aura, superior craftsmanship, fantasy weapon",
            LONGSWORD_PLUS_3_ID to "Legendary longsword +3, brilliant magical glow, master-crafted blade, epic fantasy weapon",
            MACE_PLUS_1_ID to "Enchanted mace +1, magical crushing weapon, faint glow, fantasy art",
            DAGGER_PLUS_2_ID to "Enchanted dagger +2, wickedly sharp magical blade, glowing edge, fantasy weapon",
            BATTLEAXE_PLUS_2_ID to "Enchanted battleaxe +2, heavy magical axe, rune-inscribed head, fantasy weapon",
            WAND_OF_MAGIC_MISSILES_ID to "Wand of magic missiles, slender magical wand, arcane energy, D&D item, fantasy art",
            CHAINMAIL_PLUS_1_ID to "Enchanted chainmail +1, gleaming magical armor, protective ward glow, fantasy armor",
            PLATE_ARMOR_PLUS_1_ID to "Enchanted plate armor +1, heavy magical plate, ward runes, fantasy armor",
            SHIELD_PLUS_2_ID to "Enchanted shield +2, sturdy magical shield, protective aura, fantasy armor",
            BRACERS_OF_DEFENSE_ID to "Bracers of defense, enchanted arm guards, magical barrier effect, fantasy accessory",
            BAG_OF_HOLDING_ID to "Bag of holding, magical satchel, extra-dimensional space, D&D item, fantasy art",
            BOOTS_OF_SPEED_ID to "Boots of speed, enchanted footwear, motion blur effect, winged design, fantasy item",
            CLOAK_OF_PROTECTION_ID to "Cloak of protection, magical cape, protective ward glow, fantasy accessory",
            ROPE_OF_CLIMBING_ID to "Rope of climbing, animated magical rope, self-knotting, fantasy item",
            DECANTER_OF_ENDLESS_WATER_ID to "Decanter of endless water, magical flask, water pouring endlessly, fantasy item",
            IMMOVABLE_ROD_ID to "Immovable rod, magical iron rod with button, floating in place, D&D item, fantasy art",
            RING_OF_PROTECTION_ID to "Ring of protection, magical protective ring, ward glow, fantasy jewelry",
            RING_OF_INVISIBILITY_ID to "Ring of invisibility, magical ring, fading transparency effect, legendary item, fantasy art",
            AMULET_OF_PROOF_DETECTION_ID to "Amulet of nondetection, anti-scrying pendant, hidden magic, fantasy jewelry",
            IOUN_STONE_ID to "Ioun stone, small magical gem, orbiting head, colorful glow, D&D item, fantasy art",
            BEHOLDER_EYE_ID to "Preserved beholder eye, magical component, faint glow, alchemical ingredient, fantasy item",
            MIND_FLAYER_BRAIN_ID to "Preserved mind flayer brain, psionic residue, floating in jar, dark fantasy component",
            OWLBEAR_FEATHER_ID to "Owlbear feather, large brown-grey plume, alchemical component, fantasy item",
            GELATINOUS_RESIDUE_ID to "Gelatinous cube residue, acidic slime in vial, translucent green, alchemical item, fantasy"
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

        log.info("Classic Dungeon image generation complete")
    }
}
