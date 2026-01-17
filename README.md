# AnotherThread

A Kotlin Multiplatform MUD-style game engine with AI-powered content generation. Build and manage interconnected game worlds with locations, creatures, items, and features.

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
- **Entity Management** - Create, edit, and link locations, creatures, items, and features
- **Bidirectional Exits** - Adding an exit automatically creates the reverse connection; removal prompts for one-way or two-way

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
