# D&D Random Tables Mega-Feature Plans

Feature plans inspired by OD&D Vol II (Monsters & Treasure), Vol III (Underworld & Wilderness Adventures), and Basic Rules (Mentzer 1983).

## Features

| # | Feature | Status | Plan |
|---|---------|--------|------|
| 1 | [NPC Reaction System](01-npc-reaction-system.md) | Done (server) / UI pending | 2d6 + CHA modifier reaction rolls |
| 2 | [Random Gem/Jewelry Values](02-random-gem-jewelry-values.md) | Done | Randomized gem/jewelry loot values |
| 3 | [Wandering Monsters by Location Difficulty](03-wandering-monsters.md) | Planned | Periodic difficulty-appropriate spawns |
| 4 | [Intelligent Magic Weapons](04-intelligent-magic-weapons.md) | Done | Weapons with INT, ego, alignment, powers |
| 5 | [Treasure Maps as Loot Drops](05-treasure-maps.md) | Planned | Find -> read -> travel -> search |
| 6 | [Wilderness Encounters by Terrain](06-wilderness-encounters.md) | Planned | Biome-specific movement encounters |

## Implementation Order

```
Phase 1 (independent, do first):
  1. NPC Reaction System - gives CHA meaning, simple
  2. Random Gem/Jewelry Values - enriches loot drops

Phase 2 (builds foundation):
  3. Wandering Monsters by Location Difficulty - new encounter tables
  4. Intelligent Magic Weapons - rich item sub-system

Phase 3 (builds on Phase 2):
  5. Treasure Maps as Loot Drops - uses Feature + FeatureState patterns
  6. Wilderness Encounters by Terrain - extends Feature 3
```

## Key Existing Utilities to Reuse
- `CombatRng` - all dice: `rollD6()`, `rollD20()`, `rollD100()`, `rollDiceStringSafe("2d6+3")`
- `FeatureStateRepository` - per-user state: `setState()`, `getState()`
- `FeatureRepository` - create features with JSON data blobs
- `AdventureModuleSeed` - DSL pattern for new builders
- `StatModifierService` - stat modifier calculations
- `GameTickService` - periodic processing loop
