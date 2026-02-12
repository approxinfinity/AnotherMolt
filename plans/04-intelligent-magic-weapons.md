# Feature 4: Intelligent Magic Weapons

**Status**: Planned

**Goal**: Weapons with intelligence, ego, alignment, powers, and personality. Stored as Feature data on items.

## Intelligence Table

Roll 1d6+6 = INT 7-12. Powers scale with INT. Ego = INT + 1d4.

| INT | Communication | # Primary Powers |
|-----|--------------|-----------------|
| 7 | Empathy | 1 |
| 8 | Empathy | 2 |
| 9 | Empathy | 3 |
| 10 | Speech | 3 |
| 11 | Speech | 3 + extraordinary |
| 12 | Telepathy | 3 + extraordinary |

## Primary Powers
- Detect Traps
- See Invisible
- Detect Evil
- Detect Magic
- Detect Metal
- Detect Gems
- Detect Shifting Walls

## Extraordinary Abilities
- ESP
- Telekinesis
- Teleportation
- Healing
- Fly

## Files

| Action | File |
|--------|------|
| CREATE | `server/.../game/IntelligentWeaponService.kt` - generation, ego contests |
| MODIFY | `server/.../database/AdventureModuleSeed.kt` - `intelligent()` DSL method on ItemBuilder |
| MODIFY | `composeApp/.../api/ApiClient.kt` - `IntelligentWeaponDto` |
| MODIFY | item detail views - display intelligence, powers, alignment |

## Key Details

- `IntelligentWeaponData` stored in Feature.data JSON:
  - intelligence, ego, alignment, communicationType (empathy/speech/telepathy)
  - primaryPowers list, extraordinaryAbility
- Ego contest on equip if alignment mismatch: d20 + ego vs d20 + CHA modifier
  - Weapon wins -> refuses to be wielded, or forces action
  - Wielder wins -> weapon submits (for now)
- Communication type affects how powers are conveyed:
  - Empathy = vague feelings
  - Speech = clear words
  - Telepathy = direct thought
- DSL: `intelligent { intelligence = 10; alignment = "lawful_good"; power("detect_evil"); extraordinary("healing") }`
