# Claude Code Instructions for AnotherThread

## Critical Port Configuration

**The application uses TWO servers - do NOT confuse them:**

- **Port 8080**: Web frontend (webpack dev server serving HTML/JS)
- **Port 8081**: Backend API (Ktor server with database)

### When restarting servers:

```bash
# Backend API (port 8081)
kill $(lsof -t -i :8081) 2>/dev/null; sleep 2; ./gradlew server:run &

# Web frontend (port 8080)
kill $(lsof -t -i :8080) 2>/dev/null; sleep 2; ./gradlew jsBrowserDevelopmentRun &
```

### Common Issues

**"404 on /locations" from browser:**
- The web client is trying to call the backend on the WRONG port
- Check `shared/src/jsMain/kotlin/com/ez2bg/anotherthread/Platform.js.kt`
- The `developmentBaseUrl()` function MUST return port 8081, NOT 8080

**CORS errors:**
- Add the client's origin to `server/src/main/resources/application.conf`
- Example: `allowedHosts = ["localhost:8080", "192.168.1.239:8080"]`

### Key Files

| File | Contains |
|------|----------|
| `server/src/main/resources/application.conf` | Backend port, CORS hosts |
| `shared/src/jsMain/kotlin/.../Platform.js.kt` | JS backend URL - MUST use :8081 |
| `shared/src/wasmJsMain/kotlin/.../Platform.wasmJs.kt` | Wasm backend URL - MUST use :8081 |

### User's Network

The user accesses from `192.168.1.239`. This IP must be in CORS allowed hosts.

## Server Management

When the user asks to restart or check servers, ALWAYS:
1. Check both ports (8080 and 8081)
2. Verify the backend responds: `curl http://localhost:8081/locations`
3. Verify the frontend responds: `curl http://localhost:8080/`

## Tech Stack

- Kotlin Multiplatform (Android, iOS, Web, Desktop)
- Compose Multiplatform UI
- Ktor backend with SQLite
- Terrain rendering uses SimplexNoise and VoronoiNoise for procedural generation

## Future Musings / Known Issues

### Connection Line Terrain Awareness (TODO)
The dotted connection lines between location exits have terrain-aware pathfinding code, but it doesn't seem to be working correctly. The paths should:
- Curve around lakes, mountains, swamps
- Follow roads when nearby
- Hug elevation contours

Debug logging was added to `AdminScreen.kt` to diagnose:
- Check browser console for "DEBUG: Found X locations with obstacle terrain"
- Check for "DEBUG: Path ... found Y obstacles"

The code is in:
- `locationTerrainData` - pre-computes terrain for all locations
- `nearbyObstacles` - finds obstacles near each path
- `drawTerrainAwarePath()` - applies avoidance force to path control points

Possible issues to investigate:
1. Obstacle detection radius may be too small
2. Avoidance force calculation may not be strong enough
3. Screen coordinate transformation may be mismatched between obstacle positions and path positions

### Data Integrity Dashboard (TODO)
Add an admin dashboard section that displays data integrity warnings. This should run against live data and surface issues like:

**Exit validation:**
- Exits pointing to locations more than 1 tile away
- Exit direction doesn't match actual coordinate difference (e.g., EAST exit to a location that's actually NORTH)
- Exits pointing to non-existent locations
- One-way exits that should probably be two-way (or vice versa)

**Coordinate validation:**
- Multiple locations at the same (x, y, z) coordinates
- Locations with exits but no coordinates assigned
- Orphaned locations (no exits to or from them)

**Bidirectional consistency:**
- A→B via NORTH but B→A via EAST (directions don't match for two-way exits)

Note: One-way exits (A→B without B→A) are intentional - they support secret passages, trapdoors, slides, etc.

This would be more useful than unit tests since it validates actual game data and helps content creators spot issues.

### Exploration Mode 0,0,0 Fallback Risk (TODO - Investigate)
When a user enters exploration mode and has no prior presence data (currentLocationId is null), the system falls back to location at coordinates (0,0,0) or the first location with coordinates.

**Potential risks to investigate:**
- What if no location exists at 0,0,0? Currently falls back to first location with coordinates, then first location overall
- Should there be a designated "starting location" concept instead of hardcoded 0,0,0?
- Does this create issues for new users who haven't explored yet?
- Should we handle the case where the fallback location is in a restricted area?
- Consider adding a server-side "default starting location" setting

### Class Teaching System (TODO - Future Feature)
User-generated classes (created via LLM during character creation) are currently private to that user. In the future:
- Users should be able to "teach" their custom class to other players
- This could involve in-game interaction, mentorship mechanics, or quest completion
- Taught classes become available to the student for character creation
- Consider tracking lineage (who taught whom) for flavor/lore purposes
- Admin-created and seeded classes remain universally available

### Pro/Monetization Features (TODO - Future)
Potential premium features to consider:

**Private Worlds:**
- Users can create their own private world instances
- Seeded with user-created content (locations, creatures, items, classes)
- Isolated from the main shared world

**Image Generation Enhancements:**
- Extra image generation with random seeds as a paid feature
- "Skin swapping" - generate alternate visual versions of entities
- Store multiple image variants per entity, allow users to switch between them

### Dynamic World Features (TODO - Future)
Consider features that make the world feel more alive over time:

**Decay/Evolution System:**
- Descriptions could evolve or decay over time without player interaction
- Abandoned locations might become overgrown, ruined, or inhabited by new creatures
- Periodic LLM-driven description updates based on world events or time passage
- Image recalculation when descriptions change significantly

**Environmental Variation:**
- Seasonal images (summer, fall, winter, spring variants)
- Day/night cycle with different images and descriptions
- Weather effects (rain, snow, fog) affecting visuals and descriptions
- Time-of-day could affect which creatures appear or NPC availability

### User Profile Visibility (TODO - Future)
Currently, viewing other users' profiles is restricted to admins only. Consider:
- Allow users to view each other's profiles (read-only) with lock indicator
- Add privacy settings for what fields are visible to other users
- Profile visibility levels: public, friends-only, private

### Character Class Assignment (TODO - Implement)
Users need to be assigned a character class. Implementation needed:
- Add `characterClassId` field to User model and table
- After character creation (save or image gen), show class assignment section
- Two options: "Autoassign" (LLM matches description to existing class) or "Generate" (LLM creates custom class)
- UI shows a section that pops down saying "Now you must choose a class"
- Buttons only enabled after user saves profile or generates image

### Stock Classes Storage Location (TODO - Decide)
Currently stock classes are defined in code (`ClassAbilitySeed.kt`) and seeded to the database on startup.

**Options to consider:**
1. **Keep in code (current):** Classes are version-controlled, migrations are explicit, but requires code deployment to add/modify stock classes
2. **Move to DB only:** Admin UI manages all classes including stock ones, more flexible but loses version control benefits
3. **Hybrid approach:** Stock classes defined in code as "templates" but can be overridden/extended via DB, admin can't delete stock classes but can modify them

**Factors:**
- Do we want non-developers (game designers) to modify stock classes?
- How important is version control for class balance changes?
- Should stock classes be immutable references that user-generated classes are compared against?

See `STOCK.md` for detailed analysis of current stock classes.
