package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.AbilityRepository
import org.slf4j.LoggerFactory

class AbilityCostBalancer {
    private val logger = LoggerFactory.getLogger(AbilityCostBalancer::class.java)
    
    /**
     * Balance ability costs based on tactical resource regeneration rates:
     * - Mana regen: 1 per round
     * - Stamina regen: 2 per round
     * 
     * Goals:
     * - Healing abilities should require 3+ rounds of planning (3 mana)
     * - Combat abilities should primarily use stamina (3-6 stamina)
     * - Utility spells balanced at 2-4 mana based on power
     */
    fun balanceAbilityCosts() {
        logger.info("Starting ability cost balancing...")
        
        val abilities = AbilityRepository.findAll()
        var abilitiesModified = 0
        
        for (ability in abilities) {
            val originalManaCost = ability.manaCost.toLong()
            val originalStaminaCost = ability.staminaCost.toLong()
            
            // Calculate balanced costs based on ability type and name
            val newManaCost = calculateBalancedManaCost(originalManaCost, ability.name, ability.abilityType)
            val newStaminaCost = calculateBalancedStaminaCost(originalStaminaCost, ability.name, ability.abilityType)
            
            // Only update if costs changed
            if (newManaCost != originalManaCost || newStaminaCost != originalStaminaCost) {
                val updatedAbility = ability.copy(
                    manaCost = newManaCost.toInt(),
                    staminaCost = newStaminaCost.toInt()
                )
                AbilityRepository.update(updatedAbility)
                
                logger.info(
                    "Updated ${ability.name}: mana ${originalManaCost}→${newManaCost}, " +
                    "stamina ${originalStaminaCost}→${newStaminaCost}"
                )
                abilitiesModified++
            }
        }
        
        logger.info("Ability cost balancing complete. Modified $abilitiesModified of ${abilities.size} abilities.")
    }
    
    private fun calculateBalancedManaCost(currentCost: Long, name: String, type: String): Long {
        return when {
            // Healing abilities should be expensive (3 mana = 3 rounds planning)
            name.contains("heal", ignoreCase = true) -> 3L
            
            // Focus and buff abilities are utility (2 mana = 2 rounds planning)
            name.contains("focus", ignoreCase = true) || type.contains("BUFF", ignoreCase = true) -> 2L
            
            // Spell-type abilities
            type.contains("SPELL", ignoreCase = true) -> when {
                name.contains("strike", ignoreCase = true) -> 2L // Basic spell attacks
                name.contains("frenzy", ignoreCase = true) -> 4L // High power abilities
                else -> maxOf(2L, currentCost) // Minimum 2 mana for spells
            }
            
            // Default: minimum 2 mana for magical abilities
            else -> maxOf(2L, currentCost)
        }
    }
    
    private fun calculateBalancedStaminaCost(currentCost: Long, name: String, type: String): Long {
        return when {
            // Physical attacks should primarily use stamina
            name.contains("strike", ignoreCase = true) && !type.contains("SPELL", ignoreCase = true) -> 3L
            name.contains("slash", ignoreCase = true) -> 3L
            name.contains("thrust", ignoreCase = true) -> 3L
            name.contains("bash", ignoreCase = true) -> 4L // Heavy attacks cost more
            
            // Combat maneuvers
            type.contains("PHYSICAL", ignoreCase = true) -> when {
                name.contains("frenzy", ignoreCase = true) -> 6L // High-intensity moves
                else -> maxOf(3L, currentCost) // Minimum 3 stamina for physical abilities
            }
            
            // Keep current stamina cost for non-physical abilities (usually 0)
            else -> currentCost
        }
    }
}