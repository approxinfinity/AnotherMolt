# Musings - Future Ideas & Known Issues

This document tracks future feature ideas, known issues, and design decisions that need resolution.

---

## Known Issues / Bugs

### TODO: Test Charm System
Manually test the newly implemented charm system:
- [ ] Use Charm ability on a charmable creature (not undead/boss/demon/elemental)
- [ ] Verify charmed creature follows player to new locations
- [ ] Enter combat and confirm charmed creature fights alongside player
- [ ] Test charm duration expires correctly
- [ ] Test charm breaks when creature takes heavy damage
- [ ] Test releasing a charmed creature manually
- [ ] Test charm failure with immune creature types
- [ ] Test class bonus for bard/enchanter/mesmer classes

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

### Exploration Mode 0,0,0 Fallback Risk (PARTIALLY ADDRESSED)
When a user enters exploration mode with no currentLocationId, the system falls back to (0,0,0) or the first location with coordinates.

**Status:**
- ✅ Session restore now correctly syncs location from server after validation
- ✅ Server ensures users have starting location (Tun du Lac) on login/register
- ⏳ TODO: Admin setting for configurable default starting location

**Remaining considerations:**
- What if no location exists at 0,0,0?
- Should we handle restricted area fallbacks?

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

### Exploration Mode UI Layout
The current layout of directional arrows, action buttons, and ability icons around the central thumbnail is cluttered and needs rethinking.

**Issues:**
- Ability icons overlap with directional navigation arrows
- Action buttons (attack, greet, back) compete for space
- Layout doesn't scale well with varying numbers of abilities/exits

**Ideas to explore:**
- Separate zones: directionals on outer ring, abilities on inner ring
- Contextual action bar at bottom of screen
- Swipe gestures for directions, tap for abilities
- Collapsible/expandable ability tray
- Radial menu on long-press of center thumbnail

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

## PDF Analysis with Ollama (In Progress)

### Implementation Status
PDF analysis with Ollama is implemented with two-pass extraction:
1. **Pass 1**: Extract legend entries (symbols → names)
2. **Pass 2**: Generate locations with descriptions and connections

### Files Created/Modified
- `server/src/main/kotlin/com/ez2bg/anotherthread/PdfService.kt` - Core service
- `server/build.gradle.kts` - Added Apache PDFBox 3.0.1
- `server/src/main/kotlin/com/ez2bg/anotherthread/Application.kt` - Added `/pdf/*` endpoints

### Endpoints
- `POST /pdf/analyze` - Full analysis with Ollama (multipart: file, analysisType, areaId)
- `POST /pdf/extract-text` - Raw text extraction only
- `GET /pdf/analysis-types` - Returns supported types: MAP, CLASSES, ITEMS, CREATURES, ABILITIES

### TODO: Test Two-Pass Extraction
The two-pass approach timed out during initial testing. Need to:
1. Verify Ollama is running (`ollama serve`)
2. Test with a simpler PDF or smaller text chunk
3. Compare results against existing Fungus Forest data in DB
4. May need to increase timeout (currently 5 minutes) or use a faster model

### Comparison: Ollama vs Claude Extraction
Initial single-pass test on Fungus Forest PDF:
- **Ollama found**: Yellow House, Gaunt One Elder, Obsidian Obelisk, Poison/Heal Fungus, Waterfall, Secret Passage, DHELVANEN
- **Claude added**: Bioluminescent Pool, Giant Toadstool Grove, connecting path locations (atmospheric additions)
- **Ollama missed**: Exit connections between locations (hence the two-pass approach)

### Configuration
- `LLM_API_URL` env var (default: `http://127.0.0.1:11434`)
- `LLM_MODEL` env var (default: `llama3.2:3b`)

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

## Database Schema: JSON Blobs vs Normalized Tables

### Current State
Several fields are stored as JSON text blobs in SQLite:
- `item_ids` - JSON array of item IDs
- `creature_ids` - JSON array of creature IDs
- `exit_ids` - JSON with direction + locationId objects
- `feature_ids` - JSON array of feature IDs

### The Problem
When manipulating this data via raw SQL (e.g., sqlite3 CLI for migrations or data fixes), shell escaping mangles the JSON. The Kotlin code using `kotlinx.serialization` handles it correctly, but manual database operations are error-prone.

**Example corruption observed:**
```json
// Intended:
[{"locationId":"abc","direction":"ENTER"}]

// After shell escaping issues:
[{"locationId":"{\"locationId\":\"abc\""},{"locationId":"\"direction\":\"ENTER\"}"}]
```

### Proposed Migration: Normalized Join Tables

**Current:**
```sql
location (
  id, name, desc, item_ids TEXT, creature_ids TEXT, exit_ids TEXT, ...
)
```

**Proposed:**
```sql
location (id, name, desc, ...)

location_items (
  location_id REFERENCES location(id),
  item_id REFERENCES item(id),
  PRIMARY KEY (location_id, item_id)
)

location_creatures (
  location_id REFERENCES location(id),
  creature_id REFERENCES creature(id),
  PRIMARY KEY (location_id, creature_id)
)

location_exits (
  from_location_id REFERENCES location(id),
  to_location_id REFERENCES location(id),
  direction TEXT NOT NULL,
  PRIMARY KEY (from_location_id, to_location_id, direction)
)

location_features (
  location_id REFERENCES location(id),
  feature_id REFERENCES feature(id),
  PRIMARY KEY (location_id, feature_id)
)
```

### Benefits
1. **Database-enforced foreign keys** - Can't have exits to non-existent locations
2. **Queryable relationships** - "Find all locations containing creature X" is a simple JOIN
3. **No JSON parsing overhead** - Direct SQL operations
4. **Proper indexes** - Better query performance
5. **Atomic updates** - No read-modify-write cycles for adding/removing items
6. **Safe CLI operations** - No shell escaping nightmares

### Costs
1. **Migration effort** - Need to migrate existing data
2. **More tables** - 4 new join tables
3. **Repository changes** - Need to update all repository CRUD operations
4. **Slightly more complex queries** - JOINs instead of single-row reads

### Decision Factors
- How often do we need to manipulate data via raw SQL?
- Is the JSON parsing overhead noticeable?
- Are we hitting foreign key bugs (orphaned references)?
- Is developer ergonomics worth the migration cost?

### Recommendation
Migrate when:
- Adding a major new feature that touches these relationships
- Experiencing data integrity issues
- Need complex queries across relationships

Keep JSON if:
- System is working fine and rarely needs manual data fixes
- No performance issues observed
- Team is comfortable with current approach

---

## Hidden Ground Items System (Implemented)

Items dropped on the ground become hidden after 24 hours, requiring the Search action to discover them. This adds exploration depth and rewards observant players.

### How It Works
1. **Fresh items (< 24 hours)**: Visible to everyone
2. **Old items (>= 24 hours)**: Hidden unless discovered
3. **Once discovered**: Permanently visible to that player until picked up

### Search Mechanics
Base formula: `30% + (INT mod * 6) + (INT breakpoint * 8) + (level * 2) + classBonus`

**Example search chances:**
- INT 10, Level 1, Warrior: 32%
- INT 10, Level 1, Rogue: 57% (+25 class bonus)
- INT 18, Level 1, Warrior: 72%
- INT 18, Level 10, Rogue: 95% (capped)

**Thief-type classes with search bonus (+25%):**
- Rogue, Thief, Assassin, Ranger, Scout, Ninja, Shadow, Treasure Hunter

### Database Schema
```sql
location_item (
    id VARCHAR(36) PRIMARY KEY,
    location_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(36) NOT NULL,
    dropped_at INTEGER NOT NULL,  -- timestamp
    dropped_by_user_id VARCHAR(36)  -- null = spawned
)

discovered_item (
    user_id VARCHAR(36) NOT NULL,
    location_item_id VARCHAR(36) NOT NULL,
    discovered_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, location_item_id)
)
```

### API Endpoints
- `POST /users/{id}/search` - Search current location for hidden items

### Files
- `server/src/main/kotlin/com/ez2bg/anotherthread/database/LocationItem.kt` - Tables and repository
- `server/src/main/kotlin/com/ez2bg/anotherthread/game/SearchService.kt` - Search logic
- `server/src/main/kotlin/com/ez2bg/anotherthread/database/migrations/V007_AddLocationItems.kt` - Migration

### Integration Points
- **Item Pickup**: Cleans up LocationItem records when items are picked up
- **Combat Death**: Dropped items are tracked in LocationItem table
- **Stat Summary**: `searchChance` included in player stat summaries

---

## Wilderness Naming Ideas

Currently all auto-generated wilderness is named "Wilderness". Could vary:
- "Dense Forest", "Open Plains", "Rocky Wastes" based on terrain
- User editing removes wilderness status (becomes real location)
