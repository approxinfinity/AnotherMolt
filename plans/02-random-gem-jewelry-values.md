# Feature 2: Random Gem/Jewelry Values

**Status**: Planned

**Goal**: Gems/jewelry get randomized values on drop instead of fixed template values.

## Gem Table (d100)

| Roll | Value | Bump (1-in-6) |
|------|-------|---------------|
| 01-20 | 10gp | -> 50gp |
| 21-45 | 50gp | -> 100gp |
| 46-75 | 100gp | -> 500gp |
| 76-95 | 500gp | -> 1000gp |
| 96-00 | 1000gp | (max) |

## Jewelry Table (d%)

| Roll | Value |
|------|-------|
| 01-20 | 3d6 x 100gp |
| 21-80 | 1d6 x 1000gp |
| 81-00 | 1d10 x 1000gp |

## Files

| Action | File |
|--------|------|
| CREATE | `server/.../game/GemValueService.kt` - `rollGemValue()`, `rollJewelryValue()` |
| MODIFY | `server/.../database/AdventureModuleSeed.kt` - add `isGem`/`isJewelry` flags to ItemBuilder |
| MODIFY | `server/.../combat/CombatService.kt` - call GemValueService on gem/jewelry loot drops |
| MODIFY | `server/.../routes/ChestRoutes.kt` - same for chest loot |
| MODIFY | `composeApp/.../api/ApiClient.kt` - parse instance value from feature data |

## Key Details

- When a gem/jewelry drops, create a Feature with `GemInstanceData` JSON storing the rolled value
- Attach feature to the dropped item instance's featureIds
- Client checks for `gem_instance` / `jewelry_instance` feature type to display actual value
- Template items keep their base value as default/display value
- Uses `CombatRng.rollD100()` for gem tier, `CombatRng.rollD6()` for bump check
