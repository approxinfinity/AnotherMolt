package com.ez2bg.anotherthread.database

import com.ez2bg.anotherthread.game.IntelligentWeaponData
import com.ez2bg.anotherthread.game.IntelligentWeaponService
import org.slf4j.LoggerFactory

/**
 * Seeds intelligent magic weapons into the game world.
 * These are rare, powerful weapons with their own personalities.
 */
object IntelligentWeaponSeed {
    private val log = LoggerFactory.getLogger(IntelligentWeaponSeed::class.java)

    fun seedIfEmpty() {
        // Check if already seeded
        if (ItemRepository.findById("item-intelligent-starweaver") != null) {
            log.debug("Intelligent weapons already seeded, skipping")
            return
        }

        log.info("Seeding intelligent magic weapons...")

        // 1. Starweaver - A lawful good longsword with telepathy
        seedIntelligentWeapon(
            itemId = "item-intelligent-starweaver",
            name = "Starweaver",
            description = "A shimmering longsword whose blade appears to contain swirling galaxies. It pulses with celestial energy and seems to whisper guidance to its wielder.",
            value = 2000,
            weight = 4,
            attack = 14,
            defense = 3,
            maxHp = 10,
            data = IntelligentWeaponData(
                intelligence = 12,
                ego = 15,
                alignment = "lawful_good",
                communicationType = "telepathy",
                primaryPowers = listOf("detect_evil", "detect_magic", "see_invisible"),
                extraordinaryAbility = "healing",
                personalityName = "Starweaver",
                personalityQuirk = "Hums a celestial melody when pointed toward hidden passages."
            )
        )

        // 2. Doomwhisper - A chaotic neutral dagger with speech
        seedIntelligentWeapon(
            itemId = "item-intelligent-doomwhisper",
            name = "Doomwhisper",
            description = "A wickedly curved dagger made from obsidian. It speaks in a rasping voice that only its wielder can hear, offering darkly practical advice.",
            value = 1500,
            weight = 1,
            attack = 11,
            defense = 1,
            data = IntelligentWeaponData(
                intelligence = 10,
                ego = 12,
                alignment = "chaotic_neutral",
                communicationType = "speech",
                primaryPowers = listOf("detect_traps", "detect_gems", "detect_shifting_walls"),
                extraordinaryAbility = null,
                personalityName = "Doomwhisper",
                personalityQuirk = "Whispers sarcastic commentary about your combat technique."
            )
        )

        // 3. Ironwill - A lawful neutral war hammer with empathy
        seedIntelligentWeapon(
            itemId = "item-intelligent-ironwill",
            name = "Ironwill",
            description = "A massive dwarven war hammer covered in runes that glow with inner fire. It communicates through waves of stubborn determination.",
            value = 1800,
            weight = 7,
            attack = 13,
            defense = 4,
            data = IntelligentWeaponData(
                intelligence = 8,
                ego = 10,
                alignment = "lawful_neutral",
                communicationType = "empathy",
                primaryPowers = listOf("detect_metal", "detect_shifting_walls"),
                extraordinaryAbility = null,
                personalityName = "Ironwill",
                personalityQuirk = "Grows warm to the touch near dwarven stonework."
            )
        )

        // 4. Voidcaller - A chaotic evil greatsword with telepathy
        seedIntelligentWeapon(
            itemId = "item-intelligent-voidcaller",
            name = "Voidcaller",
            description = "A massive black blade that seems to drink in light. Its alien intelligence probes the minds of those who dare touch it.",
            value = 2500,
            weight = 6,
            attack = 16,
            defense = 0,
            data = IntelligentWeaponData(
                intelligence = 12,
                ego = 16,
                alignment = "chaotic_evil",
                communicationType = "telepathy",
                primaryPowers = listOf("detect_evil", "detect_magic", "see_invisible"),
                extraordinaryAbility = "esp",
                personalityName = "Voidcaller",
                personalityQuirk = "Fills your dreams with visions of forgotten civilizations."
            )
        )

        // 5. Flameheart - A neutral good mace with speech
        seedIntelligentWeapon(
            itemId = "item-intelligent-flameheart",
            name = "Flameheart",
            description = "A gleaming mace with a head shaped like a blazing sun. It speaks with the warm, encouraging voice of a seasoned mentor.",
            value = 1600,
            weight = 5,
            attack = 10,
            defense = 2,
            maxHp = 15,
            data = IntelligentWeaponData(
                intelligence = 11,
                ego = 13,
                alignment = "neutral_good",
                communicationType = "speech",
                primaryPowers = listOf("detect_evil", "detect_traps", "detect_magic"),
                extraordinaryAbility = "healing",
                personalityName = "Flameheart",
                personalityQuirk = "Glows brighter when its wielder performs a selfless act."
            )
        )

        // Add intelligent weapons to some loot tables
        addToLootTables()

        log.info("Seeded 5 intelligent magic weapons")
    }

    private fun seedIntelligentWeapon(
        itemId: String,
        name: String,
        description: String,
        value: Int,
        weight: Int,
        attack: Int,
        defense: Int = 0,
        maxHp: Int = 0,
        data: IntelligentWeaponData
    ) {
        // Create the feature first
        val featureId = "$itemId-intelligent"
        if (FeatureRepository.findById(featureId) == null) {
            IntelligentWeaponService.createIntelligentWeaponFeature(
                featureId = featureId,
                weaponName = name,
                data = data
            )
        }

        // Create the item
        if (ItemRepository.findById(itemId) == null) {
            ItemRepository.create(Item(
                id = itemId,
                name = name,
                desc = description,
                value = value,
                weight = weight,
                equipmentType = "weapon",
                equipmentSlot = "main_hand",
                statBonuses = StatBonuses(attack = attack, defense = defense, maxHp = maxHp),
                featureIds = listOf(featureId)
            ))
        }
    }

    private fun addToLootTables() {
        // Add intelligent weapons as rare drops to existing loot tables in Caves of Chaos
        val cavesLootTables = listOf(
            "loot-caves-of-chaos-orc-chieftain",
            "loot-caves-of-chaos-bugbear-chief",
            "loot-caves-of-chaos-evil-priest"
        )

        val intelligentWeapons = listOf(
            "item-intelligent-starweaver",
            "item-intelligent-doomwhisper",
            "item-intelligent-ironwill",
            "item-intelligent-voidcaller",
            "item-intelligent-flameheart"
        )

        for (lootTableId in cavesLootTables) {
            val lootTable = LootTableRepository.findById(lootTableId) ?: continue
            // Only add if not already present
            val existingItemIds = lootTable.entries.map { it.itemId }
            val weaponToAdd = intelligentWeapons.firstOrNull { it !in existingItemIds } ?: continue

            val updatedEntries = lootTable.entries + LootEntry(
                itemId = weaponToAdd,
                chance = 0.05f, // 5% drop rate - very rare
                minQty = 1,
                maxQty = 1
            )

            LootTableRepository.update(lootTable.copy(entries = updatedEntries))
        }
    }
}
