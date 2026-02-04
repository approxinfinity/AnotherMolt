package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.CreatureRepository
import org.slf4j.LoggerFactory

/**
 * Simple utility to add basic gold drops to creatures that don't have them.
 * Addresses the missing economy rewards identified in combat balance analysis.
 */
object SimpleGoldBalancer {
    private val log = LoggerFactory.getLogger(SimpleGoldBalancer::class.java)
    
    /**
     * Add appropriate gold drops to all creatures that currently have none.
     * Uses Challenge Rating to determine appropriate amounts.
     */
    fun addMissingGoldDrops() {
        log.info("Adding gold drops to creatures that don't have them...")
        
        val creatures = CreatureRepository.findAll()
        var updatedCount = 0
        
        for (creature in creatures) {
            // Skip creatures that already have gold drops
            if (creature.minGoldDrop > 0 || creature.maxGoldDrop > 0) {
                continue
            }
            
            // Calculate gold drops based on Challenge Rating
            val cr = creature.challengeRating.coerceAtLeast(1)
            val minGold = cr * 2  // Minimum reliable income
            val maxGold = cr * 8  // Good reward potential
            
            val updatedCreature = creature.copy(
                minGoldDrop = minGold,
                maxGoldDrop = maxGold
            )
            
            if (CreatureRepository.update(updatedCreature)) {
                updatedCount++
                log.info("Added gold drops to ${creature.name} (CR $cr): $minGold-$maxGold gold")
            } else {
                log.error("Failed to update gold drops for ${creature.name}")
            }
        }
        
        log.info("Added gold drops to $updatedCount creatures")
    }
}
