# Feature 3: Wandering Monsters by Location Difficulty

**Status**: Planned

**Goal**: Periodic random creature spawns at occupied locations, appropriate to biome/difficulty.

## Files

| Action | File |
|--------|------|
| CREATE | `server/.../database/WanderingEncounter.kt` - data class + table + repository |
| CREATE | `server/.../game/WanderingMonsterService.kt` - `checkWanderingEncounter(location)` |
| CREATE | `server/.../database/WildernessEncounterSeed.kt` - biome-specific encounter tables |
| MODIFY | `server/.../game/GameTickService.kt` - add encounter check every 10 ticks |
| MODIFY | `server/.../database/AdventureModuleSeed.kt` - `wanderingEncounterTable()` DSL builder |
| MODIFY | `server/.../Application.kt` - register seed + schema |

## Key Details

- 1-in-6 chance per check (every ~30 seconds), only at locations with active players
- Location difficulty derived from max `challengeRating` of resident creatures + area metadata
- `WanderingEncounterTable` has: `biome`, `minDifficulty`, `maxDifficulty`, weighted entries
- Each entry: `creatureId`, `weight`, `minCount`, `maxCount`
- Spawned creatures are temporary - mark via FeatureState so respawn service ignores them
- Broadcast `WanderingMonsterSpawned` WebSocket message to players at location
