# Feature 1: NPC Reaction System

**Status**: In Progress

**Goal**: Roll 2d6 + CHA modifier on first encounter with non-aggressive creatures. Results affect interaction options.

## Reaction Table

| Roll (2d6 + CHA mod) | Reaction |
|-----------------------|----------|
| 2-5 | Hostile |
| 6-8 | Uncertain |
| 9-12+ | Friendly |

## Files

| Action | File | Status |
|--------|------|--------|
| CREATE | `server/.../game/ReactionService.kt` | Done |
| MODIFY | `server/.../routes/CreatureRoutes.kt` | Done (added GET /creatures/{id}/reaction) |
| MODIFY | `composeApp/.../api/ApiClient.kt` | Done (ReactionResultDto + API call) |
| MODIFY | `composeApp/.../ui/screens/ModalComponents.kt` | TODO - color-coded badge |
| MODIFY | `composeApp/.../ui/screens/AdventureViewModel.kt` | TODO - fetch reaction on select |
| MODIFY | `composeApp/.../ui/screens/AdventureScreen.kt` | TODO - pass reaction to modal |

## Key Details

- CHA modifier = `(charisma - 10) / 2`
- Persist reaction per user-creature pair: `FeatureStateRepository.setState(userId, "reaction-${creatureId}", json)`
- Aggressive creatures always hostile, ally creatures always friendly
- Reuses `CombatRng.rollD6()` twice for 2d6
- Color coding: Red = Hostile, Yellow/Orange = Uncertain, Green = Friendly
