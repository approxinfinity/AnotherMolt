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
