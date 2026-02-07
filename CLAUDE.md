# Claude Code Instructions for AnotherThread

## Critical Port Configuration

**The application uses TWO servers - do NOT confuse them:**

- **Port 12080**: Web frontend (webpack dev server serving HTML/JS)
- **Port 12081**: Backend API (Ktor server with database)

### When restarting servers:

```bash
# Backend API (port 12081)
kill $(lsof -t -i :12081) 2>/dev/null; sleep 2; ./gradlew server:run &

# Web frontend (port 12080)
kill $(lsof -t -i :12080) 2>/dev/null; sleep 2; ./gradlew jsBrowserDevelopmentRun &
```

### Common Issues

**"404 on /locations" from browser:**
- The web client is trying to call the backend on the WRONG port
- Check `shared/src/jsMain/kotlin/com/ez2bg/anotherthread/Platform.js.kt`
- The `developmentBaseUrl()` function MUST return port 12081, NOT 12080

**CORS errors:**
- Add the client's origin to `server/src/main/resources/application.conf`
- Example: `allowedHosts = ["localhost:12080", "192.168.1.239:12080"]`

### Key Files

| File | Contains |
|------|----------|
| `server/src/main/resources/application.conf` | Backend port, CORS hosts |
| `shared/src/jsMain/kotlin/.../Platform.js.kt` | JS backend URL - MUST use :12081 |
| `shared/src/wasmJsMain/kotlin/.../Platform.wasmJs.kt` | Wasm backend URL - MUST use :12081 |

### User's Network

The user accesses from `192.168.1.239`. This IP must be in CORS allowed hosts.

## Server Management

When the user asks to restart or check servers, ALWAYS:
1. Check both ports (12080 and 12081)
2. Verify the backend responds: `curl http://localhost:12081/locations`
3. Verify the frontend responds: `curl http://localhost:12080/`

## Tech Stack

- Kotlin Multiplatform (Android, iOS, Web, Desktop)
- Compose Multiplatform UI
- Ktor backend with SQLite
- Terrain rendering uses SimplexNoise and VoronoiNoise for procedural generation

## Code Hygiene Checks

**Periodically check for duplicate code patterns:**

1. **Duplicate Routes**: Search for route definitions that exist in multiple files
   ```bash
   grep -rn "post\|get\|route" server/src --include="*.kt" | grep "/admin" | sort
   ```
   Routes should only be defined ONCE - typically in their dedicated `*Routes.kt` file, NOT in `Application.kt`.

2. **Duplicate Function Names**: Look for functions defined in multiple places
   ```bash
   grep -rn "^fun \|^private fun " server/src --include="*.kt" | awk -F':' '{print $2}' | sort | uniq -d
   ```

3. **Migration leftovers**: When moving code to new files, ensure old definitions are removed. Look for comments like "(MOVED TO ...)" that indicate incomplete migrations.

## Future Musings / Known Issues

For known issues, future feature ideas, and design decisions still to be made, see [MUSINGS.md](MUSINGS.md).
