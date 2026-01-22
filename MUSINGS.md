# Musings - Future Ideas & Known Issues

This document tracks future feature ideas, known issues, and design decisions that need resolution.

---

## Known Issues / Bugs

### Connection Line Terrain Awareness
The dotted connection lines between location exits have terrain-aware pathfinding code, but it doesn't seem to be working correctly. The paths should:
- Curve around lakes, mountains, swamps
- Follow roads when nearby
- Hug elevation contours

**Debug logging** in `AdminScreen.kt`:
- Check browser console for "DEBUG: Found X locations with obstacle terrain"
- Check for "DEBUG: Path ... found Y obstacles"

**Code locations:**
- `locationTerrainData` - pre-computes terrain for all locations
- `nearbyObstacles` - finds obstacles near each path
- `drawTerrainAwarePath()` - applies avoidance force to path control points

**Possible issues:**
1. Obstacle detection radius may be too small
2. Avoidance force calculation may not be strong enough
3. Screen coordinate transformation may be mismatched

### Cloudflare Caching Issues
Cloudflare aggressively caches responses, causing stale data in the UI.

**Current workaround:** Cache buster query param (`?_=<refreshKey>`) on `/locations` API calls

**Problems observed:**
- After deleting a location, the LocationGraph may not refresh
- Mobile Safari + Cloudflare is particularly problematic

**Solutions to investigate:**
- Set up Cloudflare Page Rule to bypass cache
- Use "Development Mode" in Cloudflare during development
- Add cache-busting to ALL API calls affecting the graph
- Add `Cache-Control: no-store` headers to API responses

---

## Implementation Gaps

### Exploration Mode 0,0,0 Fallback Risk
When a user enters exploration mode with no currentLocationId, the system falls back to (0,0,0) or the first location with coordinates.

**Risks to investigate:**
- What if no location exists at 0,0,0?
- Should there be a designated "starting location" concept?
- Does this create issues for new users?
- Should we handle restricted area fallbacks?
- Consider server-side "default starting location" setting

### Combat UI (Frontend)
Real-time combat backend is implemented, but frontend UI is missing.

**TODO:**
- Combat UI component showing:
  - Current HP bars for all combatants
  - Ability buttons with cooldown indicators
  - Combat log/action feed
  - Target selection
  - Status effect indicators
- WebSocket connection from Compose app
- Handle all server message types
- Combat results/rewards modal

### Character Ability Activation System
Users should be able to activate a subset of their available abilities from multiple sources.

**Ability Sources:**
- Class abilities (from assigned CharacterClass)
- Item abilities (from equipped items)
- Special abilities (racial traits, quest rewards)

**UI Considerations:**
- Cohesive icon set for ability types (spell, combat, utility, passive)
- Color coding by source (class=blue, item=gold, special=purple)
- Active vs inactive visual distinction
- Maximum active abilities (combat loadout slots?)
- Quick-swap ability sets

**Technical Questions:**
- Where to store active ability selections? (see FeatureState below)
- Should passive abilities always be active?
- How do cooldowns interact with activation/deactivation?
- Should there be "action bar" slots that persist?

### FeatureState Architecture (Implemented)
The FeatureState system separates static definitions from per-user dynamic state.

**Design principle:** Features define *what* something does (spell, ability, buff), while FeatureState tracks *per-user runtime state* (cooldowns, charges, durations).

**Key schema:**
```
FeatureState:
  id: "{ownerId}-{featureId}"  // Composite key
  ownerId: user/creature/item ID
  ownerType: "user"|"creature"|"item"
  featureId: references Feature.id
  state: JSON blob (SpellState, BuffState, etc.)
```

**SpellState JSON example:**
```json
{
  "remainingCharges": 2,
  "cooldownExpiresAt": 1769110042189,
  "lastUsedAt": 1769108242189,
  "timesUsed": 5
}
```

**Usage:**
- Cooldown tracking: `cooldownExpiresAt > now()` means still cooling down
- Daily charges: `remainingCharges` decrements on use, reset via `/spells/reset-charges/{userId}`
- Round-based cooldowns: combat system manages `cooldownExpiresAt` based on round timing

**API endpoints:**
- `GET /feature-state/user/{userId}` - All states for a user
- `GET /feature-state/{ownerId}/{featureId}` - Specific state
- `DELETE /feature-state/{ownerId}/{featureId}` - Clear state (admin)

See [FEATURE_SPELL_DESIGN.md](FEATURE_SPELL_DESIGN.md) for full spell system design.

### Data Integrity Dashboard
Admin dashboard section to display data integrity warnings against live data.

**Exit validation:**
- Exits pointing to locations more than 1 tile away
- Exit direction doesn't match coordinate difference
- Exits pointing to non-existent locations
- One-way exits that should probably be two-way

**Coordinate validation:**
- Multiple locations at same (x, y, z) coordinates
- Locations with exits but no coordinates
- Orphaned locations (no exits to/from)

**Bidirectional consistency:**
- A→B via NORTH but B→A via EAST

Note: One-way exits (A→B without B→A) are intentional for secret passages, trapdoors, etc.

---

## Design Decisions Pending

### Stock Classes Storage Location
Currently stock classes are defined in code (`ClassAbilitySeed.kt`) and seeded on startup.

**Options:**
1. **Keep in code (current):** Version-controlled, explicit migrations, requires deployment to modify
2. **Move to DB only:** Admin UI manages all classes, more flexible but loses version control
3. **Hybrid:** Code-defined templates that can be overridden/extended via DB

**Factors:**
- Should non-developers modify stock classes?
- How important is version control for balance changes?
- Should stock classes be immutable references?

See [STOCK.md](STOCK.md) for detailed class analysis.

### isWilderness Property
Currently "is wilderness" is derived from `name == "Wilderness"`, which has limitations.

**Consider re-adding explicit `isWilderness` boolean:**
- Set `true` when wilderness auto-created
- Set `false` when user edits (any edit "claims" the wilderness)
- Allows varying wilderness names ("Dense Forest") while retaining status
- **Important for subgraph merging**: Wilderness shouldn't block placement

---

## Future Features

### Vertical Space (Z-axis)
- Z coordinate for dungeons, towers, air fortresses, underwater
- UP/DOWN directions (won't generate wilderness)
- Wilderness generation only for outdoor locations
- Location types without wilderness: underground, indoor, underwater, aerial

### Location Type System
May need a `locationType` enum: OUTDOOR, INDOOR, UNDERGROUND, UNDERWATER, AERIAL
- Or derive from features/terrain (CAVE feature → no wilderness)
- Sea-adjacent → generate "Shallows" or "Open Water" instead

### Class Teaching System
User-generated classes are currently private. Future:
- Users can "teach" custom classes to other players
- Requires in-game interaction, mentorship, or quest completion
- Track teaching lineage for flavor/lore
- Admin/stock classes remain universally available

### User Profile Visibility
Currently restricted to admins. Consider:
- Users can view each other's profiles (read-only)
- Privacy settings for visible fields
- Visibility levels: public, friends-only, private

### Dynamic World Features

**Decay/Evolution System:**
- Descriptions evolve/decay over time without player interaction
- Abandoned locations become overgrown, ruined, inhabited
- Periodic LLM-driven description updates
- Image recalculation when descriptions change

**Environmental Variation:**
- Seasonal images (summer, fall, winter, spring)
- Day/night cycle affecting images and descriptions
- Weather effects (rain, snow, fog)
- Time-of-day affecting creature spawns, NPC availability

### Pro/Monetization Features

**Private Worlds:**
- Users create own private world instances
- Seeded with user-created content
- Isolated from main shared world

**Image Generation Enhancements:**
- Extra image generation with random seeds (paid)
- "Skin swapping" - alternate visual versions
- Store multiple image variants per entity

---

## Exit System Considerations

One-way exits create complexity for coordinate movement:
- Moving location B with incoming one-way exits → need to move entire connected subgraph
- Should one-way exits be rare/special or common?

---

## Wilderness Naming Ideas

Currently all auto-generated wilderness is named "Wilderness". Could vary:
- "Dense Forest", "Open Plains", "Rocky Wastes" based on terrain
- User editing removes wilderness status (becomes real location)
