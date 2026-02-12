package com.ez2bg.anotherthread.game

import com.ez2bg.anotherthread.combat.CombatRng
import com.ez2bg.anotherthread.database.Item
import com.ez2bg.anotherthread.database.ItemRepository

/**
 * OD&D-style random gem and jewelry value tables.
 *
 * Gem table (d100):
 *   01-20: 10gp (ornamental)
 *   21-45: 50gp (semi-precious)
 *   46-75: 100gp (fancy)
 *   76-95: 500gp (precious)
 *   96-00: 1000gp (gem)
 *   Then 1-in-6 chance to bump one tier up.
 *
 * Jewelry table (d100):
 *   01-20: 3d6 x 100gp
 *   21-80: 1d6 x 1000gp
 *   81-00: 1d10 x 1000gp
 */
object GemValueService {

    // Gem tiers in ascending order
    private val GEM_TIERS = listOf(10, 50, 100, 500, 1000, 5000)

    // Gem names by tier
    private val GEM_NAMES = mapOf(
        10 to listOf("Agate", "Quartz", "Turquoise", "Lapis Lazuli", "Obsidian", "Hematite"),
        50 to listOf("Bloodstone", "Carnelian", "Moonstone", "Onyx", "Jasper", "Citrine"),
        100 to listOf("Amethyst", "Garnet", "Jade", "Pearl", "Topaz", "Coral"),
        500 to listOf("Ruby", "Emerald", "Sapphire", "Opal", "Fire Opal", "Star Sapphire"),
        1000 to listOf("Diamond", "Jacinth", "Black Opal", "Star Ruby", "Flawless Emerald"),
        5000 to listOf("Flawless Diamond", "Heart of Fire", "Star of the North", "Emperor's Eye")
    )

    // Jewelry descriptions by value range
    private val JEWELRY_NAMES = listOf(
        "Silver Ring", "Copper Bracelet", "Bronze Torc", "Pewter Brooch",
        "Gold Chain", "Jeweled Pendant", "Pearl Earring", "Gem-Set Ring",
        "Diamond Tiara", "Platinum Bracelet", "Crown of Stars", "Dragon's Hoard Necklace"
    )

    /**
     * Roll a random gem value using the OD&D d100 table.
     * Returns the value and a descriptive name.
     */
    fun rollGemValue(): Pair<Int, String> {
        val roll = CombatRng.rollD100()
        var tierIndex = when {
            roll <= 20 -> 0    // 10gp
            roll <= 45 -> 1    // 50gp
            roll <= 75 -> 2    // 100gp
            roll <= 95 -> 3    // 500gp
            else -> 4          // 1000gp
        }

        // 1-in-6 chance to bump up one tier
        if (CombatRng.rollD6() == 6 && tierIndex < GEM_TIERS.size - 1) {
            tierIndex++
        }

        val value = GEM_TIERS[tierIndex]
        val name = GEM_NAMES[value]?.random() ?: "Gem"
        return Pair(value, name)
    }

    /**
     * Roll a random jewelry value using the OD&D table.
     * Returns the value and a descriptive name.
     */
    fun rollJewelryValue(): Pair<Int, String> {
        val roll = CombatRng.rollD100()
        val value = when {
            roll <= 20 -> {
                // 3d6 x 100gp
                (CombatRng.rollD6() + CombatRng.rollD6() + CombatRng.rollD6()) * 100
            }
            roll <= 80 -> {
                // 1d6 x 1000gp
                CombatRng.rollD6() * 1000
            }
            else -> {
                // 1d10 x 1000gp
                CombatRng.rollD10() * 1000
            }
        }

        val nameIndex = when {
            value < 500 -> (0..3).random()
            value < 2000 -> (4..7).random()
            else -> (8..11).random()
        }
        val name = JEWELRY_NAMES[nameIndex]
        return Pair(value, name)
    }

    /**
     * Create a unique gem item with a randomized value.
     * The item is a new template with a unique ID so each gem is individually valued.
     */
    fun createRandomGem(): Item {
        val (value, name) = rollGemValue()
        val qualityDesc = when {
            value <= 10 -> "A rough, uncut stone with minor flaws."
            value <= 50 -> "A polished semi-precious stone of decent quality."
            value <= 100 -> "A well-cut gemstone that sparkles in the light."
            value <= 500 -> "A precious stone of remarkable clarity and color."
            value <= 1000 -> "A flawless gemstone of extraordinary beauty."
            else -> "A legendary gem that glows with inner fire."
        }

        val item = Item(
            name = name,
            desc = qualityDesc,
            featureIds = emptyList(),
            value = value,
            weight = 0,
            isStackable = false
        )
        return ItemRepository.create(item)
    }

    /**
     * Create a unique jewelry item with a randomized value.
     */
    fun createRandomJewelry(): Item {
        val (value, name) = rollJewelryValue()
        val qualityDesc = when {
            value < 500 -> "A simple piece of crafted jewelry."
            value < 2000 -> "A finely wrought piece set with small gems."
            value < 5000 -> "An exquisite piece of jewelry worthy of nobility."
            else -> "A masterwork of the jeweler's art, fit for royalty."
        }

        val item = Item(
            name = name,
            desc = qualityDesc,
            featureIds = emptyList(),
            value = value,
            weight = 0,
            isStackable = false
        )
        return ItemRepository.create(item)
    }

    /**
     * Check if an item template is a gem (by checking its ID pattern).
     * Items seeded with isGem flag will have IDs starting with "gem-".
     */
    fun isGemItem(item: Item): Boolean {
        return item.id.startsWith("gem-") || item.name.lowercase().let { n ->
            n.contains("gem") || n.contains("ruby") || n.contains("emerald") ||
            n.contains("sapphire") || n.contains("diamond") || n.contains("pearl") ||
            n.contains("opal") || n.contains("jade") || n.contains("amethyst") ||
            n.contains("garnet") || n.contains("topaz")
        }
    }

    /**
     * Check if an item template is jewelry.
     */
    fun isJewelryItem(item: Item): Boolean {
        return item.id.startsWith("jewelry-") || item.name.lowercase().let { n ->
            n.contains("necklace") || n.contains("bracelet") || n.contains("earring") ||
            n.contains("pendant") || n.contains("tiara") || n.contains("brooch") ||
            n.contains("torc")
        }
    }
}
