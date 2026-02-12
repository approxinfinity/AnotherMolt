# Feature 6: Wilderness Encounters by Terrain

**Status**: Planned

**Goal**: Random encounters triggered when moving between outdoor locations, with biome-specific creature tables.

## Biome Encounter Tables

| Biome | Creatures |
|-------|-----------|
| Grassland | Bandits, wild horses, boars, hawks |
| Forest | Wolves, spiders, bears, elves, dryads |
| Mountain | Giants, trolls, eagles, mountain goats, cave bears |
| Swamp | Lizardmen, undead, giant insects, crocodiles |
| Desert | Scorpions, nomads, giant lizards, sand worms |
| Tundra/Taiga | Winter wolves, frost giants, woolly rhinos |

## Files

| Action | File |
|--------|------|
| MODIFY | `server/.../game/WanderingMonsterService.kt` - add `checkMovementEncounter()` |
| MODIFY | `server/.../routes/UserRoutes.kt` - call encounter check on movement |
| CREATE | `server/.../database/WildernessEncounterSeed.kt` - terrain-specific tables |
| MODIFY | `composeApp/.../api/ApiClient.kt` - extend movement response with encounter data |

## Key Details

- Only triggers on movement between `OUTDOOR_GROUND` locations
- Uses same `WanderingEncounterTable` from Feature 3 with biome filtering
- 1-in-6 chance per outdoor move
- Encounter appears at destination location as a spawned creature
