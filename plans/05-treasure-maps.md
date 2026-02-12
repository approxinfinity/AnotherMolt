# Feature 5: Treasure Maps as Loot Drops

**Status**: Planned

**Goal**: Map items that reveal treasure locations. Multi-step: find -> read -> travel -> search.

## Flow

1. **Find**: Treasure map drops from loot table or chest
2. **Read**: INT check (d20 + INT mod >= DC) reveals cryptic hint about destination
3. **Travel**: Player navigates to the destination location
4. **Search**: Search action at destination awards treasure, marks map as used

## Files

| Action | File |
|--------|------|
| CREATE | `server/.../game/TreasureMapService.kt` - readMap, searchTreasure |
| CREATE | `server/.../routes/TreasureMapRoutes.kt` - read/search/progress endpoints |
| MODIFY | `server/.../database/AdventureModuleSeed.kt` - `treasureMap()` DSL builder |
| MODIFY | `composeApp/.../api/ApiClient.kt` - DTOs + API calls |
| MODIFY | `composeApp/.../ui/screens/ModalComponents.kt` - treasure map modal |
| MODIFY | `server/.../Application.kt` - register route |

## Key Details

- `TreasureMapData` in Feature.data:
  - `destinationLocationId`, `destinationHint` (cryptic text)
  - `readDifficulty` (DC for INT check)
  - `treasureLootTableId`, `treasureGold`
- Progress tracked via FeatureState: `"treasure-map-${mapItemId}"` -> `TreasureMapProgress` JSON
- Maps can drop from loot tables, be found in chests, or placed in modules
- DSL: `treasureMap("pirate-map") { hint = "Where the old oak casts its shadow at noon..."; destinationSuffix = "oak-clearing"; gold = 500 }`
