package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import org.slf4j.LoggerFactory

/**
 * Utility to ensure creatures have appropriate loot tables for item drops.
 * Creates basic loot tables for creatures that don't have them.
 */
object LootTableBalancer {
    private val log = LoggerFactory.getLogger(LootTableBalancer::class.java)
    
    /**
     * Create a basic loot table for a creature based on its Challenge Rating.
     * Higher CR creatures have better loot and higher drop chances.
     */
    fun createBasicLootTable(creatureId: String, challengeRating: Int): LootTable? {
        // Only create loot tables for CR 2+ (CR 1 creatures just drop gold)
        if (challengeRating < 2) return null
        
        val lootTableId = "loot-cr-$challengeRating-basic"
        
        // Check if this loot table already exists
        if (LootTableRepository.findById(lootTableId) != null) {
            return LootTableRepository.findById(lootTableId)
        }
        
        // Create basic loot table for this CR level
        val lootTable = LootTable(
            id = lootTableId,
            name = "CR $challengeRating Basic Loot",
            description = "Basic loot for Challenge Rating $challengeRating creatures"
        )
        
        return try {
            LootTableRepository.create(lootTable)
        } catch (e: Exception) {
            log.error("Failed to create loot table $lootTableId: ${e.message}")
            null
        }
    }
    
    /**
     * Assign appropriate loot tables to creatures that don't have them.
     * Only applies to creatures with CR 2+ to avoid over-rewarding low-level content.
     */
    fun assignMissingLootTables() {
        log.info("Assigning loot tables to creatures without them...")
        
        val creatures = CreatureRepository.findAll()
        var updatedCount = 0
        
        for (creature in creatures) {
            // Skip creatures that already have loot tables
            if (creature.lootTableId != null) {
                log.debug("Skipping ${creature.name} - already has loot table ${creature.lootTableId}")
                continue
            }
            
            // Only assign loot tables to CR 2+ creatures
            if (creature.challengeRating < 2) {
                log.debug("Skipping ${creature.name} - CR ${creature.challengeRating} too low for loot table")
                continue
            }
            
            // Create or get appropriate loot table
            val lootTable = createBasicLootTable(creature.id, creature.challengeRating)
            if (lootTable == null) {
                log.warn("Could not create loot table for ${creature.name}")
                continue
            }
            
            // Assign loot table to creature
            val updatedCreature = creature.copy(lootTableId = lootTable.id)
            
            if (CreatureRepository.update(updatedCreature)) {
                updatedCount++
                log.info("Assigned loot table ${lootTable.id} to ${creature.name} (CR ${creature.challengeRating})")
            } else {
                log.error("Failed to update creature ${creature.name} with loot table")
            }
        }
        
        log.info("Assigned loot tables to $updatedCount creatures")
    }
    
    /**
     * Get the drop rate percentage for a given Challenge Rating.
     * Higher CR creatures have better drop rates.
     */
    fun getDropRateForCR(challengeRating: Int): Float {
        return when {
            challengeRating >= 10 -> 0.8f  // 80% drop rate for high CR
            challengeRating >= 5 -> 0.6f   // 60% drop rate for medium CR
            challengeRating >= 2 -> 0.4f   // 40% drop rate for low CR
            else -> 0.1f                   // 10% drop rate for CR 1
        }
    }
}
