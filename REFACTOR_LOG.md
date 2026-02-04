# AnotherThread Refactor Log

This document tracks all changes made during the adventure mode refactoring focused on combat balance and game feel.

## Goals
- Combat balance - Review creature stats, damage formulas, and ability costs
- Reward systems - XP scaling, loot drops, gold economy
- Game feel - Combat pacing, resource regeneration, ability cooldowns
- Missing features - Implement gaps identified in MUSINGS.md

---

## Changes Made

### [Session 1 - Initial Analysis]
**Date:** 2026-02-04  
**Focus:** Understanding current combat system and identifying balance issues

**Findings:**
- Combat system is well-architected with sophisticated RNG (CombatRng.kt)
- Resource regeneration: 2 mana/3 stamina per round in combat
- XP scaling system implemented with CR-based individual scaling
- Hit chance: 75% base + level difference bonuses, capped at 5-95%
- Damage variance: ±25% on all damage/healing
- Critical hits: 5% base chance, 2x damage
- Glancing blows: ~10% chance, 50% damage on near misses

**Issues Identified:**
1. Need to examine actual creature stats for balance
2. Resource regeneration rates may need tuning
3. Combat pacing (3s rounds) needs validation
4. Missing implementation: loot drops, gold economy
5. Need more detailed creature data for testing

**Next Steps:**
- Examine current creature stats in database
- Analyze combat balance with different level scenarios
- Implement missing reward systems
- Add more comprehensive testing

### [Session 2 - Combat Balance Analysis]
**Date:** 2026-02-04  
**Focus:** Analyzing creature stats and identifying balance issues

**Current Creature Analysis (Fungus Forest):**
- **Forest Rat**: Level 1, 8 HP, 3-4 damage, 15 XP, CR 1 (appropriate starter)
- **Gaunt One**: Level 3, 25 HP, 8-10 damage, 50 XP, CR 3 (early-mid enemy)
- **Gaunt One Elder**: Level 5, 50 HP, 12-15 damage, 100 XP, CR 5 (mid-tier boss)
- **Forest Guardian**: Level 8, 80 HP, 15-20 damage, 200 XP, CR 8 (high-tier boss)

**Critical Balance Issues Identified:**
1. **Resource Imbalance**: Combat regeneration (2 mana/3 stamina per round) may be too generous
   - A 3-second round with 2 mana means 40 mana/minute regeneration
   - Classes with 30 mana pools recover full resources in ~45 seconds
   - This could trivialize resource management

2. **Creature HP vs Player Damage Gap**: 
   - Forest Guardian (80 HP) vs new player (~10 damage) = 8+ rounds minimum
   - With 3-second rounds, this is 24+ seconds of pure combat
   - Add missed attacks/healing, and fights become very long

3. **Missing Loot Economy**: 
   - Creatures have minGoldDrop/maxGoldDrop fields but no implementation
   - No item drops or loot tables active
   - Players have no meaningful rewards beyond XP

4. **XP Scaling Edge Cases**:
   - Current scaling: 1.5x for CR+4, 0.1x for CR-8
   - Level 1 player vs Level 8 Guardian = appropriate XP scaling
   - But Level 10 player vs Level 1 Rat = only 0.1x XP (may be too punishing)

**Priority Fixes:**
1. ✅ **IMPLEMENTED**: Gold drop system for immediate progression feedback
2. Review and balance resource regeneration rates
3. Add creature loot tables for equipment progression
4. Adjust creature HP/damage ratios for better pacing

---

### [Session 3 - Gold Drop Implementation]
**Date:** 2026-02-04  
**Focus:** Implementing missing gold economy rewards

**Problem Solved:**
- Creatures in database had `minGoldDrop = 0` and `maxGoldDrop = 0`
- Players received no monetary rewards from combat victories
- Broken progression system with no economic incentive

**Implementation:**
- Created `SimpleGoldBalancer.kt` with Challenge Rating-based gold drops
- Formula: `minGold = CR * 2`, `maxGold = CR * 8`
- Integrated into application startup for automatic balancing
- CR 1 creatures: 2-8 gold, CR 5 creatures: 10-40 gold

**Impact:**
- Players now receive meaningful monetary rewards from combat
- Gold income scales with encounter difficulty
- Foundation for equipment purchasing and economic progression
- Estimated 100-1000 gold per hour based on player level

**Files Modified:**
- `server/src/main/kotlin/com/ez2bg/anotherthread/SimpleGoldBalancer.kt` (new)
- `server/src/main/kotlin/com/ez2bg/anotherthread/Application.kt` (startup integration)
