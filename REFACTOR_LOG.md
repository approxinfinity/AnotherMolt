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
2. ✅ **IMPLEMENTED**: Balanced resource regeneration rates
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

### [Session 4 - Resource Regeneration Balance]
**Date:** 2026-02-04  
**Focus:** Addressing resource management balance

**Problem Identified:**
- Previous rates (2 mana/3 stamina per round) potentially too generous
- With 3-second rounds, players recovered 40 mana/60 stamina per minute
- Resource management became trivial in longer combats

**Implementation:**
- Reduced mana regeneration: 2 → 1 per round
- Reduced stamina regeneration: 3 → 2 per round
- Maintains martial class advantage (stamina > mana regen)
- Now: 20 mana/40 stamina per minute recovery

**Impact:**
- Resource management becomes more meaningful and tactical
- Players must choose abilities more carefully in sustained combat
- Maintains class identity (martial classes still have stamina advantage)
- Combat pacing encourages strategic resource usage

**Files Modified:**
- `server/src/main/kotlin/com/ez2bg/anotherthread/combat/CombatModels.kt` (regeneration constants)

## Session 6: Ability Cost Balance (Jan 4, 2025)

**Duration**: ~10 minutes  
**Focus**: Align ability costs with tactical resource regeneration rates

### Changes Made

1. **Implemented Ability Cost Balancing System**:
   - Created `server/src/main/kotlin/com/ez2bg/anotherthread/AbilityCostBalancer.kt`
   - Addresses misalignment between ability costs and resource regeneration
   - Tactical resource rates: 1 mana/2 stamina per round
   - Prevents abilities from being usable every round

2. **Cost Balance Framework**:
   - **Healing Abilities**: 3 mana (requires 3 rounds of planning)
   - **Combat Abilities**: 3-6 stamina (emphasizes stamina for physical actions)
   - **Utility Spells**: 2-4 mana based on power level
   - **Resource Specialization**: Mana for magic, stamina for physical

3. **Specific Balance Rules**:
   - Healing spells: 3 mana minimum (tactical significance)
   - Focus/buff abilities: 2 mana (utility value)
   - Strike abilities: 2 mana or 3 stamina (depending on type)
   - Frenzy abilities: 4 mana or 6 stamina (high power abilities)
   - Physical attacks: Primarily stamina-based costs

4. **Integration**:
   - Auto-runs on server startup after creature stat balancing
   - Uses SQLDelight for safe database operations
   - Comprehensive logging of all cost adjustments

### Impact on Adventure Mode

- **Tactical Resource Management**: Players must plan ability usage over multiple rounds
- **Resource Specialization**: Clear distinction between mana (magic) and stamina (physical)
- **Combat Pacing**: Eliminates ability spam, encourages strategic planning
- **Healing Balance**: Healing requires significant resource investment

### Technical Implementation

- Uses existing `selectAllAbilities()` and `updateAbility()` queries
- Intelligent cost calculation based on ability name and type
- Maintains minimum costs to prevent trivial resource usage
- Integrated into server startup sequence for automatic balance

**Files Modified:**
- `server/src/main/kotlin/com/ez2bg/anotherthread/AbilityCostBalancer.kt` (new)
- `server/src/main/kotlin/com/ez2bg/anotherthread/Application.kt` (startup integration)
