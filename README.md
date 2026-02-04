# AnotherThread

A Kotlin Multiplatform MUD-style game engine with AI-powered content generation. Build and manage interconnected game worlds with locations, creatures, items, and features.

## Documentation

| Document | Purpose |
|----------|---------|
| **README.md** (this file) | Architecture, setup, and API reference |
| [OVERVIEW.md](OVERVIEW.md) | Game mechanics, vision, and how systems work together |
| [MUSINGS.md](MUSINGS.md) | Future ideas, known issues, and design decisions pending |
| [STOCK.md](STOCK.md) | Stock character class balance analysis |
| [CLAUDE.md](CLAUDE.md) | AI assistant instructions (for Claude Code) |

## Features

### Game World Management
- **Locations** - Create interconnected areas with bidirectional exits, items, creatures, and features
- **Creatures** - Define NPCs and monsters with inventories and features
- **Items** - Create objects that can be placed in locations or held by creatures/users
- **Features** - Modular attributes organized by categories that can be attached to any entity
- **Users** - Player accounts with authentication, inventories, and location tracking

### AI Content Generation
- **Ollama Integration** - Generate names and descriptions for locations, creatures, and items using local LLMs
- **Image Generation** - Automatic image generation for entities (configurable)

### Admin UI
- **Location Graph** - Visual map showing location connections with pan and center-on-tap
  - Dotted connection lines show paths between locations
  - Two-way paths always visible; one-way paths shown only when relevant
  - Lines avoid drawing through location dots
- **Entity Management** - Create, edit, and link locations, creatures, items, and features
- **Bidirectional Exits** - Adding an exit automatically creates the reverse connection; removal prompts for one-way or two-way
- **Database Backup/Restore** - Create and restore database backups from the admin interface

### Grid Coordinate System
Locations can be placed on a 3D grid (X, Y, Z coordinates) for spatial organization:
- **Exit directions** (N, NE, E, SE, S, SW, W, NW) determine relative positioning
- **Visual map** displays locations based on their grid coordinates
- **Data integrity** - Exit directions should correspond to coordinate offsets (e.g., NORTH exit → location at Y-1)

### Multi-Platform Support
- Android, iOS, Web (Wasm/JS), Desktop (JVM)
- Shared Compose Multiplatform UI
- Ktor server backend with SQLite database

## Project Structure

```
/composeApp    - Compose Multiplatform client (Android, iOS, Desktop, Web)
/server        - Ktor server with REST API
/shared        - Shared Kotlin code
/iosApp        - iOS app entry point
```

## API Endpoints

### Locations
- `GET /locations` - List all locations
- `POST /locations` - Create location
- `PUT /locations/{id}` - Update location

### Creatures
- `GET /creatures` - List all creatures
- `POST /creatures` - Create creature
- `PUT /creatures/{id}` - Update creature

### Items
- `GET /items` - List all items
- `POST /items` - Create item
- `PUT /items/{id}` - Update item

### Features & Categories
- `GET /features` - List all features
- `GET /features/{id}` - Get feature by ID
- `GET /features/by-category/{categoryId}` - Get features by category
- `POST /features` - Create feature
- `PUT /features/{id}` - Update feature
- `GET /feature-categories` - List all categories
- `POST /feature-categories` - Create category
- `PUT /feature-categories/{id}` - Update category

### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login

### Users
- `GET /users/{id}` - Get user
- `PUT /users/{id}` - Update user
- `PUT /users/{id}/location` - Update user's current location
- `GET /users/at-location/{locationId}` - Get active users at location

### Content Generation
- `GET /generate/status` - Check if Ollama is available
- `POST /generate/location` - Generate location name/description
- `POST /generate/creature` - Generate creature name/description
- `POST /generate/item` - Generate item name/description

### Database Administration
- `POST /admin/database/backup` - Create a timestamped database backup
- `GET /admin/database/backups` - List available backup files
- `POST /admin/database/restore/{filename}` - Restore from a backup (auto-creates safety backup first)

### Combat (WebSocket)
Connect to `ws://localhost:12081/combat?userId={userId}` for real-time combat.

**Client → Server Messages:**
- `JoinCombatMessage` - Start/join combat at current location
- `UseAbilityMessage` - Queue an ability for current round
- `FleeCombatMessage` - Attempt to flee (50% base chance)
- `LeaveCombatMessage` - Exit combat gracefully

**Server → Client Messages:**
- `CombatStartedMessage` - Combat initiated with session state
- `RoundStartMessage` - New round beginning
- `AbilityResolvedMessage` - Ability executed with results (hit/miss/crit)
- `HealthUpdateMessage` - HP changed for a combatant
- `StatusEffectMessage` - Status applied/removed
- `RoundEndMessage` - Round complete with updated state
- `CombatEndedMessage` - Combat concluded with rewards

See [OVERVIEW.md](OVERVIEW.md) for combat mechanics details.

### Classes & Abilities
- `GET /classes` - List all character classes
- `GET /classes/{id}` - Get class by ID
- `POST /classes` - Create character class
- `PUT /classes/{id}` - Update character class
- `GET /abilities` - List all abilities
- `GET /abilities/by-class/{classId}` - Get abilities for a class
- `POST /abilities` - Create ability
- `PUT /abilities/{id}` - Update ability

### Spells (Feature-based)
Spells are implemented as Features with JSON data defining their behavior. Per-user state (cooldowns, charges) is tracked in FeatureState.

- `GET /spells` - List all spell features
- `GET /spells/available/{userId}` - Get spells available to a user
- `POST /spells/cast` - Cast a utility spell
- `GET /spells/state/{userId}/{featureId}` - Get spell cooldown/charges state
- `POST /spells/reset-charges/{userId}` - Reset daily charges (admin)

**Cast Request:**
```json
{
  "userId": "user-123",
  "featureId": "phase-walk-id",
  "targetParams": { "direction": "NORTH" }
}
```

**Cast Response:**
```json
{
  "success": true,
  "message": "You phase through reality...",
  "newLocationId": "new-loc-id",
  "spellState": {
    "remainingCharges": 2,
    "cooldownExpiresAt": 1769110042189,
    "cooldownSecondsRemaining": 1800
  }
}
```

### Feature State
Per-user/per-entity dynamic state for features (cooldowns, charges, buff durations).

- `GET /feature-state/user/{userId}` - Get all feature states for user
- `GET /feature-state/{ownerId}/{featureId}` - Get specific feature state
- `DELETE /feature-state/{ownerId}/{featureId}` - Delete feature state

## Ports Configuration

**IMPORTANT: The application uses two separate servers on different ports:**

| Port | Service | Description |
|------|---------|-------------|
| **12080** | Web Client | Frontend (HTML/JS/Wasm) served by webpack dev server |
| **12081** | Backend API | Ktor REST API server with database |

### Key Files for Port Configuration

| File | Purpose |
|------|---------|
| `server/src/main/resources/application.conf` | Backend port (12081) and CORS allowed hosts |
| `shared/src/jsMain/kotlin/.../Platform.js.kt` | JS client backend URL (must use port 12081) |
| `shared/src/wasmJsMain/kotlin/.../Platform.wasmJs.kt` | Wasm client backend URL (must use port 12081) |
| `composeApp/webpack.config.d/devServer.js` | Frontend dev server port (12080) |

### Local Network Access

To access from other devices on your network (e.g., `192.168.1.239`):

1. **Add your IP to CORS allowed hosts** in `server/src/main/resources/application.conf`:
   ```hocon
   cors {
       allowedHosts = ["localhost:3000", "localhost:8080", "192.168.1.239:8080"]
   }
   ```

2. **Ensure Platform files point to port 8081** (the backend), not 8080:
   ```kotlin
   // In Platform.js.kt and Platform.wasmJs.kt
   actual fun developmentBaseUrl(): String {
       val hostname = window.location.hostname
       return "http://$hostname:8081"  // <-- MUST be 8081, not 8080
   }
   ```

3. **Restart both servers** after configuration changes.

## Running the Project

### Server
```shell
./gradlew :server:run
```

### Desktop App
```shell
./gradlew :composeApp:run
```

### Android App
```shell
./gradlew :composeApp:assembleDebug
```

### Web App (Wasm)
```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### iOS App
Open `/iosApp` in Xcode and run.

## Testing

### Unit Tests
Run unit tests (fast, no external services required):
```shell
./gradlew test
```

### Integration Tests
Integration tests verify connectivity to external services (Ollama, Stable Diffusion). These are skipped by default and should only be run when:
- Setting up a new server environment
- Verifying external service connectivity after configuration changes
- Debugging content generation issues

Run integration tests:
```shell
./gradlew :server:test -PrunIntegrationTests=true
```

**Prerequisites for integration tests:**
- Ollama running at `http://localhost:11434` with a model installed
- (Optional) Stable Diffusion WebUI running at `http://localhost:7860`

## Configuration

### Ollama (Content Generation)
The server connects to Ollama at `http://localhost:11434` by default. Install Ollama and pull a model:
```shell
ollama pull llama3.2
```

**Starting Ollama with parallel request support:**

By default Ollama processes requests sequentially. To enable concurrent requests (recommended for multi-user scenarios):

```shell
# Stop any running Ollama instance
pkill ollama

# Start with parallel support (2 concurrent requests)
OLLAMA_NUM_PARALLEL=2 ollama serve
```

This allows description generation to proceed while class generation is running in the background.

### Database
SQLite database is stored at `server/data/anotherthread.db` by default.

### External Access (Cloudflare Tunnel)

To access the web app from external devices (phones, tablets, other computers):

1. **Install cloudflared:**
   ```shell
   brew install cloudflared
   ```

2. **Start tunnels for both frontend and backend:**
   ```shell
   # Terminal 1 - Frontend tunnel (port 8080)
   cloudflared tunnel --url http://localhost:8080

   # Terminal 2 - Backend tunnel (port 8081)
   cloudflared tunnel --url http://localhost:8081
   ```

   Note the tunnel URLs displayed (e.g., `https://xxx-yyy-zzz.trycloudflare.com`).

3. **Configure the frontend to use the backend tunnel:**

   Copy the config template:
   ```shell
   cp composeApp/src/wasmJsMain/resources/config.js.template \
      composeApp/src/wasmJsMain/resources/config.js
   ```

   Edit `config.js` and set your backend tunnel URL:
   ```javascript
   window.APP_CONFIG = {
       tunnelBackendUrl: "https://YOUR_BACKEND_TUNNEL_URL.trycloudflare.com",
       localBackendPort: 8081
   };
   ```

4. **Rebuild and access:**
   ```shell
   ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
   ```

   Access via the frontend tunnel URL shown in Terminal 1.

**Note:** The `config.js` file is gitignored to prevent committing tunnel URLs. The template provides the default localhost configuration.

#### Config File Reference

| Setting | Description | Default |
|---------|-------------|---------|
| `tunnelBackendUrl` | Backend API URL when accessed via tunnel | `null` (uses localhost) |
| `localBackendPort` | Backend port for local development | `8081` |

## Tech Stack

- **Kotlin Multiplatform** - Shared code across platforms
- **Compose Multiplatform** - UI framework
- **Ktor** - Server and HTTP client
- **Exposed** - SQL framework
- **SQLite** - Database
- **Ollama** - Local LLM integration
- **BCrypt** - Password hashing
