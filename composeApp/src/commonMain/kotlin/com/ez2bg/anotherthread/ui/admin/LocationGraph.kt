package com.ez2bg.anotherthread.ui.admin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CharacterClassDto
import com.ez2bg.anotherthread.api.CreatureDto
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.ItemDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.TerrainOverridesDto
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.combat.CombatClient
import com.ez2bg.anotherthread.ui.GameMode
import com.ez2bg.anotherthread.ui.SwordIcon
import com.ez2bg.anotherthread.ui.CreatureStateIcon
import com.ez2bg.anotherthread.ui.getBlindPresenceDescription
import com.ez2bg.anotherthread.ui.getBlindItemDescription
import com.ez2bg.anotherthread.ui.components.AbilityIconButton
import com.ez2bg.anotherthread.ui.components.AbilityIconSmall
import com.ez2bg.anotherthread.ui.components.BlindOverlay
import com.ez2bg.anotherthread.ui.components.CombatTarget
import com.ez2bg.anotherthread.ui.components.DisorientIndicator
import com.ez2bg.anotherthread.ui.components.EventLogEntry
import com.ez2bg.anotherthread.ui.components.TargetSelectionOverlay
import com.ez2bg.anotherthread.ui.terrain.NeighborElevations
import com.ez2bg.anotherthread.ui.terrain.NeighborRivers
import com.ez2bg.anotherthread.ui.terrain.PassThroughFeatures
import com.ez2bg.anotherthread.ui.terrain.TerrainType
import com.ez2bg.anotherthread.ui.terrain.calculateElevationFromTerrain
import com.ez2bg.anotherthread.ui.terrain.drawLocationTerrain
import com.ez2bg.anotherthread.ui.terrain.drawParchmentBackground
import com.ez2bg.anotherthread.ui.terrain.drawTerrainAwarePath
import com.ez2bg.anotherthread.ui.terrain.parseTerrainFromDescription
import com.ez2bg.anotherthread.updateUrlWithCacheBuster
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Main location graph composable that displays an interactive map of locations.
 * Supports panning, zooming, and location selection.
 * Has two modes: CREATE (admin/editing) and ADVENTURE (gameplay).
 */
@Composable
fun LocationGraph(
    locations: List<LocationDto>,
    onLocationClick: (LocationDto) -> Unit,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    terrainOverridesMap: Map<String, TerrainOverridesDto> = emptyMap(),
    onSettingsClick: (LocationDto) -> Unit = {},
    currentUser: UserDto? = null,
    gameMode: GameMode = GameMode.CREATE,
    onGameModeChange: (GameMode) -> Unit = {},
    // Combat state from WebSocket
    activeCombatClient: CombatClient? = null,
    combatCooldowns: Map<String, Int> = emptyMap(),
    combatQueuedAbilityId: String? = null,
    combatIsBlinded: Boolean = false,
    combatBlindRounds: Int = 0,
    combatIsDisoriented: Boolean = false,
    combatDisorientRounds: Int = 0,
    // Event log
    eventLogEntries: List<EventLogEntry> = emptyList()
) {
    val gridResult = remember(locations) {
        calculateForceDirectedPositions(locations)
    }
    val locationPositions = gridResult.locationPositions

    // Track which location is expanded (null = none expanded)
    var expandedLocationId by remember { mutableStateOf<String?>(null) }

    // Creatures, items, and abilities for Adventure mode display
    var allCreatures by remember { mutableStateOf<List<CreatureDto>>(emptyList()) }
    var allItems by remember { mutableStateOf<List<ItemDto>>(emptyList()) }
    var allAbilitiesMap by remember { mutableStateOf<Map<String, AbilityDto>>(emptyMap()) }
    var creatureStates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Load creatures, items, and abilities when entering Adventure mode
    LaunchedEffect(gameMode) {
        if (gameMode.isAdventure) {
            ApiClient.getCreatures().onSuccess { allCreatures = it }
            ApiClient.getItems().onSuccess { allItems = it }
            ApiClient.getAbilities().onSuccess { abilities ->
                allAbilitiesMap = abilities.associateBy { it.id }
            }
            ApiClient.getCreatureStates().onSuccess { creatureStates = it }
        }
    }

    // Auto-select location when entering Adventure mode based on user's presence
    LaunchedEffect(gameMode, currentUser?.id, locations) {
        if (gameMode.isAdventure && expandedLocationId == null && locations.isNotEmpty()) {
            // Try to use user's current location from presence data
            val userLocationId = currentUser?.currentLocationId
            val targetLocation = if (userLocationId != null) {
                locations.find { it.id == userLocationId }
            } else {
                null
            }

            if (targetLocation != null) {
                expandedLocationId = targetLocation.id
            } else {
                // Fall back to location at (0,0) in overworld or first location with coordinates
                val fallbackLocation = locations.find { it.gridX == 0 && it.gridY == 0 && (it.areaId ?: "overworld") == "overworld" }
                    ?: locations.firstOrNull { it.gridX != null && it.gridY != null }
                    ?: locations.firstOrNull()

                if (fallbackLocation != null) {
                    expandedLocationId = fallbackLocation.id
                    // Update user presence to this fallback location
                    currentUser?.let { user ->
                        ApiClient.updateUserLocation(user.id, fallbackLocation.id)
                    }
                }
            }
        }
    }

    // Update URL with cache buster when a location is selected/deselected on the graph
    LaunchedEffect(expandedLocationId) {
        val locationSuffix = expandedLocationId?.let { "&loc=$it" } ?: ""
        updateUrlWithCacheBuster("map$locationSuffix")
    }

    // Update user presence when viewing a location on the graph
    LaunchedEffect(expandedLocationId, currentUser?.id) {
        if (currentUser != null && expandedLocationId != null) {
            ApiClient.updateUserLocation(currentUser.id, expandedLocationId)
        }
    }

    // Pan offset state with animation support
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    // Zoom scale state (1.0 = 100%, min 0.2 = 20%, max 3.0 = 300%)
    // Start at 0.7 so the 100x100 thumbnail fits comfortably on screen
    var scale by remember { mutableStateOf(0.7f) }
    val minScale = 0.2f
    val maxScale = 3f
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .pointerInput(expandedLocationId, gameMode) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // Collapse any expanded thumbnail when panning/zooming (unless in Adventure mode)
                    if (expandedLocationId != null && gameMode.isCreate) {
                        expandedLocationId = null
                    }

                    // Apply zoom with limits
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    val scaleFactor = newScale / scale

                    // Adjust pan to zoom toward centroid
                    // When zooming, we want the point under the centroid to stay in place
                    val newOffsetX = centroid.x - (centroid.x - offset.value.x) * scaleFactor + pan.x
                    val newOffsetY = centroid.y - (centroid.y - offset.value.y) * scaleFactor + pan.y

                    scale = newScale
                    scope.launch {
                        offset.snapTo(Offset(newOffsetX, newOffsetY))
                    }
                }
            }
            // Removed: detectTapGestures that collapsed thumbnail on empty space tap
            // User must tap the currently selected dot to deselect it
            // Scroll wheel zoom support for desktop/web
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: continue
                            // Scroll up (negative Y) = zoom in, scroll down (positive Y) = zoom out
                            val zoomFactor = if (scrollDelta.y < 0) 1.1f else 0.9f
                            val newScale = (scale * zoomFactor).coerceIn(minScale, maxScale)

                            if (newScale != scale) {
                                // Get the pointer position for zoom centering
                                val pointerPos = event.changes.firstOrNull()?.position ?: Offset.Zero
                                val scaleFactor = newScale / scale

                                // Zoom toward pointer position
                                val newOffsetX = pointerPos.x - (pointerPos.x - offset.value.x) * scaleFactor
                                val newOffsetY = pointerPos.y - (pointerPos.y - offset.value.y) * scaleFactor

                                scale = newScale
                                scope.launch {
                                    offset.snapTo(Offset(newOffsetX, newOffsetY))
                                }
                            }
                        }
                    }
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val boxSize = 70.dp
        val boxSizePx = boxSize.value * 2.5f

        // Function to calculate screen position of a location
        fun getLocationScreenPos(pos: LocationPosition): Offset {
            return Offset(
                pos.x * (width - boxSizePx) + boxSizePx / 2,
                pos.y * (height - boxSizePx) + boxSizePx / 2
            )
        }

        // Function to center on a location's 100x100 thumbnail (not the dot)
        // The thumbnail is positioned to the right of the dot
        fun centerOnLocation(location: LocationDto) {
            val pos = locationPositions[location.id] ?: return
            val screenPos = getLocationScreenPos(pos)

            // Account for the thumbnail being to the right of the dot
            // Dot is at screenPos, thumbnail center is offset by:
            // dotToThumbnailGap (35dp) + highlightedDotSize/2 (8dp) + thumbnailContainerSize/2 (~78dp)
            // Total horizontal offset is approximately 121dp to the right
            val thumbnailOffsetX = 121f // Approximate offset in dp-ish units

            val centerX = width / 2
            val centerY = height / 2
            // Shift target to center on thumbnail instead of dot
            val targetOffset = Offset(
                centerX - screenPos.x - thumbnailOffsetX,
                centerY - screenPos.y
            )

            scope.launch {
                offset.animateTo(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 300)
                )
            }
        }

        // Terrain size
        val terrainSize = boxSizePx * 1.0f

        // LAYER 1: Parchment background (fixed, doesn't pan)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawParchmentBackground(seed = locations.size)
        }

        // LAYER 1.5: Dark overlay when in exploration mode (obscures map features)
        if (gameMode.isAdventure) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color.Black.copy(alpha = 0.85f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.value.x
                    translationY = offset.value.y
                }
        ) {
            // LAYER 2: Terrain for each location
            // Pre-compute elevations and terrain types for all locations
            val locationElevations = locations.associate { loc ->
                val terrains = parseTerrainFromDescription(loc.desc, loc.name)
                val override = terrainOverridesMap[loc.id]?.elevation
                loc.id to calculateElevationFromTerrain(terrains, override)
            }
            val locationTerrains = locations.associate { loc ->
                loc.id to parseTerrainFromDescription(loc.desc, loc.name)
            }
            val locationHasRiver = locations.associate { loc ->
                val terrains = locationTerrains[loc.id] ?: emptySet()
                loc.id to (TerrainType.RIVER in terrains || TerrainType.STREAM in terrains)
            }

            // Build a map of location ID to location for quick lookup
            val locationById = locations.associateBy { it.id }

            // Function to find all directions where a terrain type exists within maxDepth steps
            // Uses BFS to explore all paths, not just straight lines
            // Returns the general direction (N/S/E/W/NE/NW/SE/SW) from start to each found feature
            fun findFeatureDirections(startId: String, terrainType: TerrainType, maxDepth: Int = 4): Set<ExitDirection> {
                val foundDirections = mutableSetOf<ExitDirection>()
                val visited = mutableSetOf<String>()
                // Queue entries: (locationId, cumulativeX, cumulativeY, depth)
                // We track cumulative position to determine overall direction
                val queue = ArrayDeque<Triple<String, Pair<Int, Int>, Int>>()

                val startLoc = locationById[startId] ?: return emptySet()
                visited.add(startId)

                // Add all direct neighbors to queue
                for (exit in startLoc.exits) {
                    val (dx, dy) = when (exit.direction) {
                        ExitDirection.NORTH -> Pair(0, -1)
                        ExitDirection.SOUTH -> Pair(0, 1)
                        ExitDirection.EAST -> Pair(1, 0)
                        ExitDirection.WEST -> Pair(-1, 0)
                        ExitDirection.NORTHEAST -> Pair(1, -1)
                        ExitDirection.NORTHWEST -> Pair(-1, -1)
                        ExitDirection.SOUTHEAST -> Pair(1, 1)
                        ExitDirection.SOUTHWEST -> Pair(-1, 1)
                        else -> Pair(0, 0)
                    }
                    queue.addLast(Triple(exit.locationId, Pair(dx, dy), 1))
                }

                while (queue.isNotEmpty()) {
                    val (currentId, pos, depth) = queue.removeFirst()
                    if (currentId in visited) continue
                    visited.add(currentId)

                    val terrains = locationTerrains[currentId] ?: emptySet()
                    val hasFeature = terrainType in terrains ||
                        (terrainType == TerrainType.RIVER && TerrainType.STREAM in terrains)

                    if (hasFeature) {
                        // Determine direction from cumulative position
                        val (cx, cy) = pos
                        val dir = when {
                            cx > 0 && cy < 0 -> ExitDirection.NORTHEAST
                            cx < 0 && cy < 0 -> ExitDirection.NORTHWEST
                            cx > 0 && cy > 0 -> ExitDirection.SOUTHEAST
                            cx < 0 && cy > 0 -> ExitDirection.SOUTHWEST
                            cx > 0 -> ExitDirection.EAST
                            cx < 0 -> ExitDirection.WEST
                            cy < 0 -> ExitDirection.NORTH
                            cy > 0 -> ExitDirection.SOUTH
                            else -> null
                        }
                        if (dir != null) foundDirections.add(dir)
                    }

                    // Continue exploring if under max depth
                    if (depth < maxDepth) {
                        val currentLoc = locationById[currentId] ?: continue
                        for (exit in currentLoc.exits) {
                            if (exit.locationId !in visited) {
                                val (dx, dy) = when (exit.direction) {
                                    ExitDirection.NORTH -> Pair(0, -1)
                                    ExitDirection.SOUTH -> Pair(0, 1)
                                    ExitDirection.EAST -> Pair(1, 0)
                                    ExitDirection.WEST -> Pair(-1, 0)
                                    ExitDirection.NORTHEAST -> Pair(1, -1)
                                    ExitDirection.NORTHWEST -> Pair(-1, -1)
                                    ExitDirection.SOUTHEAST -> Pair(1, 1)
                                    ExitDirection.SOUTHWEST -> Pair(-1, 1)
                                    else -> Pair(0, 0)
                                }
                                queue.addLast(Triple(exit.locationId, Pair(pos.first + dx, pos.second + dy), depth + 1))
                            }
                        }
                    }
                }

                return foundDirections
            }

            // Pre-compute pass-through features for each location
            val locationPassThrough = locations.associate { loc ->
                val myTerrains = locationTerrains[loc.id] ?: emptySet()

                // Only compute pass-through if this tile doesn't already have the feature
                val riverDirs = if (TerrainType.RIVER !in myTerrains && TerrainType.STREAM !in myTerrains) {
                    findFeatureDirections(loc.id, TerrainType.RIVER)
                } else emptySet()

                val forestDirs = if (TerrainType.FOREST !in myTerrains) {
                    findFeatureDirections(loc.id, TerrainType.FOREST)
                } else emptySet()

                val mountainDirs = if (TerrainType.MOUNTAIN !in myTerrains) {
                    findFeatureDirections(loc.id, TerrainType.MOUNTAIN)
                } else emptySet()

                val hillsDirs = if (TerrainType.HILLS !in myTerrains) {
                    findFeatureDirections(loc.id, TerrainType.HILLS)
                } else emptySet()

                val lakeDirs = if (TerrainType.LAKE !in myTerrains) {
                    findFeatureDirections(loc.id, TerrainType.LAKE)
                } else emptySet()

                val swampDirs = if (TerrainType.SWAMP !in myTerrains) {
                    findFeatureDirections(loc.id, TerrainType.SWAMP)
                } else emptySet()

                loc.id to PassThroughFeatures(
                    riverDirections = riverDirs,
                    forestDirections = forestDirs,
                    mountainDirections = mountainDirs,
                    hillsDirections = hillsDirs,
                    lakeDirections = lakeDirs,
                    swampDirections = swampDirs
                )
            }

            // Only render terrain tiles when NOT in exploration mode
            if (gameMode.isCreate) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Render locations
                    locations.forEach { location ->
                        val pos = locationPositions[location.id] ?: return@forEach
                        val screenPos = getLocationScreenPos(pos)

                        // Helper to get neighbor location ID by direction
                        fun getNeighborId(vararg directions: ExitDirection): String? {
                            for (dir in directions) {
                                location.exits.find { it.direction == dir }?.let { return it.locationId }
                            }
                        return null
                    }

                    // Compute neighbor elevations from exits (including diagonals mapped to cardinals)
                    val northId = getNeighborId(ExitDirection.NORTH, ExitDirection.NORTHEAST, ExitDirection.NORTHWEST)
                    val southId = getNeighborId(ExitDirection.SOUTH, ExitDirection.SOUTHEAST, ExitDirection.SOUTHWEST)
                    val eastId = getNeighborId(ExitDirection.EAST, ExitDirection.NORTHEAST, ExitDirection.SOUTHEAST)
                    val westId = getNeighborId(ExitDirection.WEST, ExitDirection.NORTHWEST, ExitDirection.SOUTHWEST)

                    val neighborElevs = NeighborElevations(
                        north = northId?.let { locationElevations[it] },
                        south = southId?.let { locationElevations[it] },
                        east = eastId?.let { locationElevations[it] },
                        west = westId?.let { locationElevations[it] }
                    )

                    val passThrough = locationPassThrough[location.id] ?: PassThroughFeatures()

                    // Check if neighbor has river OR is a pass-through river tile
                    // This ensures rivers connect through intermediate tiles
                    // Simple river neighbor check - only direct neighbors with rivers
                    val neighborRivs = NeighborRivers(
                        north = northId?.let { locationHasRiver[it] } ?: false,
                        south = southId?.let { locationHasRiver[it] } ?: false,
                        east = eastId?.let { locationHasRiver[it] } ?: false,
                        west = westId?.let { locationHasRiver[it] } ?: false
                    )

                    drawLocationTerrain(
                        location = location,
                        center = screenPos,
                        terrainSize = terrainSize,
                        overrides = terrainOverridesMap[location.id],
                        neighborElevations = neighborElevs,
                        neighborRivers = neighborRivs,
                        passThrough = passThrough
                    )
                }
            }
            } // End of terrain rendering conditional

            // LAYER 2.5: Connection lines between exits
            // In exploration mode: lines are shown in the minimap instead
            // In normal mode: thin dotted lines
            if (gameMode.isCreate) {
            // LAYER 2.5: Connection lines between exits (thin, meandering, dotted orange lines)
            // The dots use: Modifier.offset(x = (pos.x * (width - boxSizePx) / 2.5f).dp, ...)
            // The value inside .dp is a raw number that becomes dp units.
            // The Canvas draws at (0,0) of the Box, and the dots are offset from there.
            //
            // To convert the dot's dp offset to Canvas pixels, we need to multiply by density.
            // The dot center is at: offset + collapsedSize/2 = offset + 10dp
            val densityValue = LocalDensity.current.density
            val dotRadiusPx = 5f * densityValue  // 5dp in pixels
            val dotCenterOffsetPx = 10f * densityValue  // 10dp (collapsedSize/2) in pixels

            // Data class for connection line with metadata
            data class ConnectionLineData(
                val from: Offset,
                val to: Offset,
                val seed: Long,
                val fromId: String,
                val toId: String,
                val isTwoWay: Boolean,
                val nearbyObstacles: List<Pair<Offset, TerrainType>> = emptyList()  // Positions of obstacles to avoid
            )

            // Build a lookup of which locations have exits to which other locations
            val exitLookup = remember(locations) {
                val lookup = mutableMapOf<String, Set<String>>()
                locations.forEach { loc ->
                    lookup[loc.id] = loc.exits.map { it.locationId }.toSet()
                }
                lookup
            }

            // Pre-compute terrain types and screen positions for all locations
            // Used for terrain-aware path routing
            val locationTerrainData = remember(locations, locationPositions, width, height, boxSizePx, densityValue) {
                val data = locations.mapNotNull { loc ->
                    val pos = locationPositions[loc.id] ?: return@mapNotNull null
                    val dpX = pos.x * (width - boxSizePx) / 2.5f
                    val dpY = pos.y * (height - boxSizePx) / 2.5f
                    val screenPos = Offset(
                        dpX * densityValue + dotCenterOffsetPx,
                        dpY * densityValue + dotCenterOffsetPx
                    )
                    val terrains = parseTerrainFromDescription(loc.desc, loc.name)
                    Triple(loc.id, screenPos, terrains)
                }
                // Debug: show which locations have obstacle terrain
                val obstacleTypes = setOf(TerrainType.LAKE, TerrainType.MOUNTAIN, TerrainType.SWAMP, TerrainType.WATER)
                val obstacleLocations = data.filter { (_, _, terrains) -> terrains.any { it in obstacleTypes } }
                println("DEBUG: Found ${obstacleLocations.size} locations with obstacle terrain")
                obstacleLocations.forEach { (id, _, terrains) ->
                    val loc = locations.find { it.id == id }
                    println("DEBUG:   ${loc?.name}: ${terrains.filter { it in obstacleTypes }}")
                }
                data
            }

            // Obstacle terrain types that paths should avoid
            val obstacleTerrains = setOf(TerrainType.LAKE, TerrainType.MOUNTAIN, TerrainType.SWAMP, TerrainType.WATER)

            val connectionLines = remember(locations, locationPositions, width, height, boxSizePx, densityValue, exitLookup, locationTerrainData) {
                val lines = mutableListOf<ConnectionLineData>()
                val drawnConnections = mutableSetOf<Pair<String, String>>()

                locations.forEach { location ->
                    val fromPos = locationPositions[location.id] ?: return@forEach
                    val fromDpX = fromPos.x * (width - boxSizePx) / 2.5f
                    val fromDpY = fromPos.y * (height - boxSizePx) / 2.5f
                    val fromCenter = Offset(
                        fromDpX * densityValue + dotCenterOffsetPx,
                        fromDpY * densityValue + dotCenterOffsetPx
                    )

                    location.exits.forEach exitLoop@{ exit ->
                        val toId = exit.locationId
                        val connectionKey = if (location.id < toId) {
                            Pair(location.id, toId)
                        } else {
                            Pair(toId, location.id)
                        }

                        if (connectionKey in drawnConnections) return@exitLoop
                        drawnConnections.add(connectionKey)

                        val toPos = locationPositions[toId] ?: return@exitLoop
                        val toDpX = toPos.x * (width - boxSizePx) / 2.5f
                        val toDpY = toPos.y * (height - boxSizePx) / 2.5f
                        val toCenter = Offset(
                            toDpX * densityValue + dotCenterOffsetPx,
                            toDpY * densityValue + dotCenterOffsetPx
                        )

                        val dx = toCenter.x - fromCenter.x
                        val dy = toCenter.y - fromCenter.y
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                        if (distance > dotRadiusPx * 2) {
                            val nx = dx / distance
                            val ny = dy / distance

                            val fromEdge = Offset(
                                fromCenter.x + nx * dotRadiusPx,
                                fromCenter.y + ny * dotRadiusPx
                            )
                            val toEdge = Offset(
                                toCenter.x - nx * dotRadiusPx,
                                toCenter.y - ny * dotRadiusPx
                            )

                            // Find nearby obstacles that the path should avoid
                            // Check locations within a certain distance of the path midpoint
                            val midpoint = Offset((fromCenter.x + toCenter.x) / 2, (fromCenter.y + toCenter.y) / 2)
                            val searchRadius = distance * 0.8f  // Look within 80% of the path length

                            val nearbyObstacles = locationTerrainData
                                .filter { (locId, screenPos, terrains) ->
                                    // Don't include the endpoints themselves
                                    locId != location.id && locId != toId &&
                                    // Check if location is near the path
                                    run {
                                        val distToMid = kotlin.math.sqrt(
                                            (screenPos.x - midpoint.x).let { it * it } +
                                            (screenPos.y - midpoint.y).let { it * it }
                                        )
                                        distToMid < searchRadius
                                    } &&
                                    // Check if it has obstacle terrain
                                    terrains.any { it in obstacleTerrains }
                                }
                                .map { (_, screenPos, terrains) ->
                                    // Return the position and the most significant obstacle type
                                    val obstacleType = terrains.firstOrNull { it in obstacleTerrains } ?: TerrainType.WATER
                                    Pair(screenPos, obstacleType)
                                }

                            val reverseExists = exitLookup[toId]?.contains(location.id) == true
                            val isTwoWay = reverseExists

                            // Debug: log when obstacles are found
                            if (nearbyObstacles.isNotEmpty()) {
                                println("DEBUG: Path ${location.name} -> found ${nearbyObstacles.size} obstacles: ${nearbyObstacles.map { it.second }}")
                            }

                            val seed = (location.id.hashCode() xor toId.hashCode()).toLong()
                            lines.add(ConnectionLineData(
                                from = fromEdge,
                                to = toEdge,
                                seed = seed,
                                fromId = location.id,
                                toId = toId,
                                isTwoWay = isTwoWay,
                                nearbyObstacles = nearbyObstacles
                            ))
                        }
                    }
                }
                lines
            }

            // Filter connection lines based on selected location
            // - Two-way: always show
            // - One-way: only show if involves the selected location
            val filteredConnectionLines = remember(connectionLines, expandedLocationId) {
                connectionLines.filter { line ->
                    if (line.isTwoWay) {
                        true  // Always show two-way connections
                    } else {
                        // One-way: only show if selected location is involved
                        expandedLocationId != null &&
                            (line.fromId == expandedLocationId || line.toId == expandedLocationId)
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Improved connection lines with better visibility and styling
                // In exploration mode: thicker solid lines like the highlight circle
                // In normal mode: refined dotted lines with better contrast
                val connectionColor = if (gameMode.isAdventure) {
                    Color(0xFFFF9800).copy(alpha = 0.7f)  // More visible in exploration mode
                } else {
                    Color(0xFFFF9800).copy(alpha = 0.4f) // Better visibility in create mode
                }
                val oneWayColor = if (gameMode.isAdventure) {
                    Color(0xFFFF5722).copy(alpha = 0.8f) // Distinct orange-red for one-way
                } else {
                    Color(0xFFFF5722).copy(alpha = 0.5f) // More visible one-way indicator
                }
                // Exploration mode: thick like highlight circle (about 3dp), normal: refined (1.5dp)
                val strokeWidth = if (gameMode.isAdventure) 3.5.dp.toPx() else 2.0.dp.toPx()
                val dashLength = if (gameMode.isAdventure) 10f else 6f  // More defined dashes
                val gapLength = if (gameMode.isAdventure) 3f else 4f   // Smaller gaps for continuity

                filteredConnectionLines.forEach { line ->
                    drawTerrainAwarePath(
                        from = line.from,
                        to = line.to,
                        color = if (line.isTwoWay) connectionColor else oneWayColor,
                        strokeWidth = strokeWidth,
                        dashLength = dashLength,
                        gapLength = gapLength,
                        seed = line.seed,
                        obstacles = line.nearbyObstacles
                    )
                }
            }

            // LAYER 3: Location dots/thumbnails
            // In exploration mode: only render the expanded thumbnail (no collapsed dots)
            // and the dots are not clickable
            if (gameMode.isCreate) {
                // Normal mode: render all locations with their dots
                locations.forEach { location ->
                    val pos = locationPositions[location.id] ?: return@forEach
                    val isExpanded = expandedLocationId == location.id
                    LocationNodeThumbnail(
                        location = location,
                        isExpanded = isExpanded,
                        modifier = Modifier.offset(
                            x = (pos.x * (width - boxSizePx) / 2.5f).dp,
                            y = (pos.y * (height - boxSizePx) / 2.5f).dp
                        ),
                        isAdmin = isAdmin,
                        onClick = {
                            if (isExpanded) {
                                // Already expanded, go to detail
                                onLocationClick(location)
                            } else {
                                // Expand this thumbnail and collapse others
                                expandedLocationId = location.id
                                centerOnLocation(location)
                            }
                        },
                        onSettingsClick = { onSettingsClick(location) },
                        onExitClick = { targetLocation ->
                            // Navigate to the target location: expand it and center the map
                            expandedLocationId = targetLocation.id
                            centerOnLocation(targetLocation)
                        },
                        onDotClick = {
                            // Tapping the orange dot collapses the expanded view
                            expandedLocationId = null
                        },
                        allLocations = locations,
                        gameMode = GameMode.CREATE
                    )
                }
            }

            // Labels are only shown when location is expanded (tap to reveal)
        }

        // Adventure mode view
        if (gameMode.isAdventure && expandedLocationId != null) {
            val currentLocation = locations.find { it.id == expandedLocationId }
            val currentPos = locationPositions[expandedLocationId]

            // State for detail view (creature/item inspection)
            var selectedCreature by remember { mutableStateOf<CreatureDto?>(null) }
            var selectedItem by remember { mutableStateOf<ItemDto?>(null) }
            val isDetailViewVisible = selectedCreature != null || selectedItem != null

            // State for description popup (shown when tapping the image)
            var showDescriptionPopup by remember { mutableStateOf(false) }

            // Animation for slide transition
            val detailOffsetX by animateFloatAsState(
                targetValue = if (isDetailViewVisible) 0f else 1f,
                animationSpec = tween(durationMillis = 300),
                label = "detailSlide"
            )

            // Snackbar state for action toasts
            val snackbarHostState = remember { SnackbarHostState() }

            // Combat ability state (local abilities list, use combat state for cooldowns/queued)
            var playerAbilities by remember { mutableStateOf<List<AbilityDto>>(emptyList()) }
            // Use combat state from LocationGraphView level for cooldowns, queued ability, and visual effects:
            // - combatCooldowns (Map<String, Int>)
            // - combatQueuedAbilityId (String?)
            // - combatIsBlinded, combatBlindRounds
            // - combatIsDisoriented, combatDisorientRounds

            // Alias for cleaner code in this block
            val isBlinded = combatIsBlinded
            val blindRoundsRemaining = combatBlindRounds
            val isDisoriented = combatIsDisoriented
            val disorientRoundsRemaining = combatDisorientRounds

            // Target selection state for abilities that need a target
            var pendingAbility by remember { mutableStateOf<AbilityDto?>(null) }

            // Ranger class detection for directional attunement feature
            var playerCharacterClass by remember { mutableStateOf<CharacterClassDto?>(null) }
            val isRanger = playerCharacterClass?.name == "Ranger"

            // Fetch player's class abilities and class info
            LaunchedEffect(currentUser?.characterClassId) {
                val classId = currentUser?.characterClassId
                if (classId != null) {
                    ApiClient.getAbilitiesByClass(classId).onSuccess { abilities ->
                        playerAbilities = abilities.filter { it.abilityType != "passive" }
                            .sortedBy { it.name.lowercase() }
                    }
                    // Fetch class info for Ranger detection
                    ApiClient.getCharacterClass(classId).onSuccess { characterClass ->
                        playerCharacterClass = characterClass
                    }
                }
            }

            if (currentLocation != null) {
                // 1. Centered 100x100 thumbnail - simple image at absolute center
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Location name is now in the top-left info panel

                    // Disorient indicator below thumbnail
                    if (isDisoriented && disorientRoundsRemaining > 0) {
                        DisorientIndicator(
                            roundsRemaining = disorientRoundsRemaining,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 160.dp)
                        )
                    }

                    // Container for circular thumbnail + ability icons (apply disorient rotation here)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                if (isDisoriented) {
                                    rotationZ = 180f
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Ability icons ring around the thumbnail (only show if player has abilities)
                        if (playerAbilities.isNotEmpty()) {
                            val ringRadius = 80.dp
                            val iconSize = 36.dp
                            val maxIcons = 8
                            val displayAbilities = playerAbilities.take(maxIcons)
                            val totalIcons = displayAbilities.size

                            displayAbilities.forEachIndexed { index, ability ->
                                // Calculate angle (start at top, go clockwise)
                                val angleDegrees = (360f / totalIcons) * index - 90f
                                val angleRadians = angleDegrees * PI / 180.0
                                val offsetX = (ringRadius.value * cos(angleRadians)).toFloat().dp
                                val offsetY = (ringRadius.value * sin(angleRadians)).toFloat().dp

                                AbilityIconButton(
                                    ability = ability,
                                    cooldownRounds = combatCooldowns[ability.id] ?: 0,
                                    isQueued = ability.id == combatQueuedAbilityId,
                                    onClick = {
                                        // Cast ability via combat WebSocket
                                        scope.launch {
                                            val client = activeCombatClient
                                            if (client == null) {
                                                snackbarHostState.showSnackbar(
                                                    "Not in combat",
                                                    duration = SnackbarDuration.Short
                                                )
                                                return@launch
                                            }

                                            // Handle target selection based on ability target type
                                            when (ability.targetType) {
                                                "self", "area", "all_enemies", "all_allies" -> {
                                                    // No target needed, cast immediately
                                                    client.useAbility(ability.id, null)
                                                    snackbarHostState.showSnackbar(
                                                        "Casting: ${ability.name}",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                                "single_enemy" -> {
                                                    // Need to select a target - get creatures at this location
                                                    val enemies = allCreatures.filter { it.id in currentLocation.creatureIds }
                                                    if (enemies.size == 1) {
                                                        // Auto-target single enemy
                                                        client.useAbility(ability.id, enemies.first().id)
                                                        snackbarHostState.showSnackbar(
                                                            "Casting ${ability.name} on ${enemies.first().name}",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    } else if (enemies.isEmpty()) {
                                                        snackbarHostState.showSnackbar(
                                                            "No enemies to target",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    } else {
                                                        // Show target selection overlay
                                                        pendingAbility = ability
                                                    }
                                                }
                                                "single_ally" -> {
                                                    // Self-target for now (could show party members)
                                                    client.useAbility(ability.id, currentUser?.id)
                                                    snackbarHostState.showSnackbar(
                                                        "Casting ${ability.name} on self",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                                else -> {
                                                    // Unknown target type, try casting without target
                                                    client.useAbility(ability.id, null)
                                                    snackbarHostState.showSnackbar(
                                                        "Casting: ${ability.name}",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    size = iconSize,
                                    modifier = Modifier.offset(x = offsetX, y = offsetY)
                                )
                            }
                        }

                        // Circular 100x100 thumbnail with location image or fallback
                        // For Rangers: shows minimap overlay with location image behind (semi-transparent)
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.8f))
                                .border(2.dp, if (isRanger) Color(0xFF4CAF50) else Color(0xFF4A4A4A), CircleShape)
                                .clickable { onLocationClick(currentLocation) }
                        ) {
                            // Blind overlay covers the view
                            if (isBlinded && blindRoundsRemaining > 0) {
                                BlindOverlay(
                                    roundsRemaining = blindRoundsRemaining,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (isRanger) {
                                // Ranger's Directional Attunement: minimap overlay with location image behind
                                // Layer 1: Location image with semi-opacity filter
                                if (currentLocation.imageUrl != null) {
                                    AsyncImage(
                                        model = "${AppConfig.api.baseUrl}${currentLocation.imageUrl}",
                                        contentDescription = currentLocation.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { alpha = 0.35f },
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Layer 2: Minimap overlay
                                val currentGridX = currentLocation.gridX ?: 0
                                val currentGridY = currentLocation.gridY ?: 0
                                val animatedGridX by animateFloatAsState(
                                    targetValue = currentGridX.toFloat(),
                                    animationSpec = tween(durationMillis = 300),
                                    label = "rangerMinimapX"
                                )
                                val animatedGridY by animateFloatAsState(
                                    targetValue = currentGridY.toFloat(),
                                    animationSpec = tween(durationMillis = 300),
                                    label = "rangerMinimapY"
                                )

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val centerX = size.width / 2
                                    val centerY = size.height / 2
                                    // Smaller grid spacing for 100dp size (vs 140dp minimap)
                                    val gridSpacingPx = 22.dp.toPx()
                                    val dotRadius = 5.dp.toPx()
                                    val highlightRadius = 7.dp.toPx()
                                    val lineColor = Color(0xFF4CAF50).copy(alpha = 0.8f) // Green for Ranger
                                    val lineWidth = 2.dp.toPx()
                                    val highlightStrokeWidth = 2.dp.toPx()
                                    val dotOutlineWidth = 1.dp.toPx()

                                    // Vignette effect
                                    val vignetteRadius = minOf(size.width, size.height) / 2
                                    fun vignetteAlpha(x: Float, y: Float): Float {
                                        val dx = x - centerX
                                        val dy = y - centerY
                                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                        val fadeStart = vignetteRadius * 0.5f
                                        val fadeEnd = vignetteRadius * 0.95f
                                        return when {
                                            dist <= fadeStart -> 1f
                                            dist >= fadeEnd -> 0f
                                            else -> 1f - ((dist - fadeStart) / (fadeEnd - fadeStart))
                                        }
                                    }

                                    val locationById = locations.associateBy { it.id }
                                    val drawnConnections = mutableSetOf<Pair<String, String>>()
                                    val visited = mutableSetOf<String>()
                                    val queue = ArrayDeque<Pair<String, Int>>()

                                    queue.add(currentLocation.id to 0)
                                    visited.add(currentLocation.id)

                                    // Draw connections up to 2 hops
                                    while (queue.isNotEmpty()) {
                                        val (locId, depth) = queue.removeFirst()
                                        if (depth >= 2) continue

                                        val loc = locationById[locId] ?: continue
                                        val locGridX = loc.gridX ?: continue
                                        val locGridY = loc.gridY ?: continue
                                        val locRelX = locGridX - animatedGridX
                                        val locRelY = locGridY - animatedGridY

                                        loc.exits.forEach { exit ->
                                            val targetLoc = locationById[exit.locationId] ?: return@forEach
                                            val targetGridX = targetLoc.gridX ?: return@forEach
                                            val targetGridY = targetLoc.gridY ?: return@forEach
                                            val targetRelX = targetGridX - animatedGridX
                                            val targetRelY = targetGridY - animatedGridY

                                            val connKey = if (locId < exit.locationId) locId to exit.locationId else exit.locationId to locId

                                            if (kotlin.math.abs(locRelX) <= 2 && kotlin.math.abs(locRelY) <= 2 &&
                                                kotlin.math.abs(targetRelX) <= 2 && kotlin.math.abs(targetRelY) <= 2 &&
                                                !drawnConnections.contains(connKey)) {

                                                drawnConnections.add(connKey)

                                                val fromX = centerX + locRelX * gridSpacingPx
                                                val fromY = centerY + locRelY * gridSpacingPx
                                                val toX = centerX + targetRelX * gridSpacingPx
                                                val toY = centerY + targetRelY * gridSpacingPx

                                                val depthAlpha = when (depth) {
                                                    0 -> 0.9f
                                                    else -> 0.5f
                                                }

                                                val fromVignette = vignetteAlpha(fromX, fromY)
                                                val toVignette = vignetteAlpha(toX, toY)
                                                val vignetteMultiplier = minOf(fromVignette, toVignette)
                                                val finalAlpha = depthAlpha * vignetteMultiplier

                                                if (finalAlpha > 0.01f) {
                                                    val dx = toX - fromX
                                                    val dy = toY - fromY
                                                    val length = kotlin.math.sqrt(dx * dx + dy * dy)
                                                    if (length > dotRadius * 2) {
                                                        val shortenAmount = dotRadius + 1.dp.toPx()
                                                        val ratio = shortenAmount / length
                                                        val adjustedFromX = fromX + dx * ratio
                                                        val adjustedFromY = fromY + dy * ratio
                                                        val adjustedToX = toX - dx * ratio
                                                        val adjustedToY = toY - dy * ratio

                                                        drawLine(
                                                            color = lineColor.copy(alpha = finalAlpha),
                                                            start = Offset(adjustedFromX, adjustedFromY),
                                                            end = Offset(adjustedToX, adjustedToY),
                                                            strokeWidth = lineWidth
                                                        )
                                                    }
                                                }
                                            }

                                            if (!visited.contains(exit.locationId)) {
                                                visited.add(exit.locationId)
                                                queue.add(exit.locationId to depth + 1)
                                            }
                                        }
                                    }

                                    // Draw dots
                                    locations.forEach { location ->
                                        val locGridX = location.gridX ?: return@forEach
                                        val locGridY = location.gridY ?: return@forEach

                                        val relX = locGridX - animatedGridX
                                        val relY = locGridY - animatedGridY

                                        if (kotlin.math.abs(relX) <= 2 && kotlin.math.abs(relY) <= 2) {
                                            val dotX = centerX + relX * gridSpacingPx
                                            val dotY = centerY + relY * gridSpacingPx
                                            val isCurrentLoc = location.id == expandedLocationId

                                            val dotVignetteAlpha = if (isCurrentLoc) 1f else vignetteAlpha(dotX, dotY)

                                            if (dotVignetteAlpha > 0.01f) {
                                                // Highlight for current location (green for Ranger)
                                                if (isCurrentLoc) {
                                                    drawCircle(
                                                        color = Color(0xFF4CAF50),
                                                        radius = highlightRadius,
                                                        center = Offset(dotX, dotY),
                                                        style = Stroke(width = highlightStrokeWidth)
                                                    )
                                                }

                                                // Dot fill with terrain color
                                                val terrainColor = getTerrainColor(location.desc, location.name)
                                                drawCircle(
                                                    color = terrainColor.copy(alpha = terrainColor.alpha * dotVignetteAlpha),
                                                    radius = dotRadius,
                                                    center = Offset(dotX, dotY)
                                                )

                                                // Dot outline (green for Ranger)
                                                drawCircle(
                                                    color = Color(0xFF4CAF50).copy(alpha = 0.7f * dotVignetteAlpha),
                                                    radius = dotRadius,
                                                    center = Offset(dotX, dotY),
                                                    style = Stroke(width = dotOutlineWidth)
                                                )
                                            }
                                        }
                                    }

                                    // Subtle fog of war effect
                                    val fogRadius = size.minDimension / 2
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.2f),
                                                Color.Black.copy(alpha = 0.5f),
                                                Color.Black.copy(alpha = 0.8f)
                                            ),
                                            center = Offset(centerX, centerY),
                                            radius = fogRadius
                                        ),
                                        radius = fogRadius,
                                        center = Offset(centerX, centerY)
                                    )
                                }
                            } else {
                                // Normal location display (non-Ranger)
                                if (currentLocation.imageUrl != null) {
                                    AsyncImage(
                                        model = "${AppConfig.api.baseUrl}${currentLocation.imageUrl}",
                                        contentDescription = currentLocation.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Fallback: show location initials
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentLocation.name.take(2).uppercase(),
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Navigation arrows around the thumbnail (pushed farther out to clear ability icons)
                    // Ability ring radius is 80dp, so arrows need to be beyond that
                    currentLocation.exits.forEach { exit ->
                        val targetLocation = locations.find { it.id == exit.locationId }
                        if (targetLocation != null) {
                            val (offsetX, offsetY, icon) = when (exit.direction) {
                                ExitDirection.NORTH -> Triple(0.dp, (-130).dp, Icons.Filled.ArrowUpward)
                                ExitDirection.SOUTH -> Triple(0.dp, 130.dp, Icons.Filled.ArrowDownward)
                                ExitDirection.EAST -> Triple(130.dp, 0.dp, Icons.AutoMirrored.Filled.ArrowForward)
                                ExitDirection.WEST -> Triple((-130).dp, 0.dp, Icons.AutoMirrored.Filled.ArrowBack)
                                ExitDirection.NORTHEAST -> Triple(92.dp, (-92).dp, Icons.Filled.NorthEast)
                                ExitDirection.NORTHWEST -> Triple((-92).dp, (-92).dp, Icons.Filled.NorthWest)
                                ExitDirection.SOUTHEAST -> Triple(92.dp, 92.dp, Icons.Filled.SouthEast)
                                ExitDirection.SOUTHWEST -> Triple((-92).dp, 92.dp, Icons.Filled.SouthWest)
                                ExitDirection.ENTER -> Triple(0.dp, 145.dp, Icons.Filled.MeetingRoom)
                                else -> Triple(0.dp, 0.dp, Icons.Filled.ArrowUpward)
                            }

                            if (exit.direction != ExitDirection.UNKNOWN) {
                                // Custom press animation for navigation arrows
                                var isPressed by remember { mutableStateOf(false) }
                                val navScale by animateFloatAsState(
                                    targetValue = if (isPressed) 1.3f else 1f,
                                    animationSpec = tween(durationMillis = 150),
                                    label = "navScale"
                                )
                                val glowAlpha by animateFloatAsState(
                                    targetValue = if (isPressed) 0.8f else 0f,
                                    animationSpec = tween(durationMillis = 150),
                                    label = "navGlow"
                                )

                                Box(
                                    modifier = Modifier
                                        .offset(x = offsetX, y = offsetY)
                                        .size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Orange glow ring (behind the button)
                                    Box(
                                        modifier = Modifier
                                            .size((32 * navScale).dp)
                                            .background(
                                                Color(0xFFFF9800).copy(alpha = glowAlpha),
                                                CircleShape
                                            )
                                    )
                                    // Main button - purple for ENTER (portal), blue for directions
                                    val buttonColor = if (exit.direction == ExitDirection.ENTER) {
                                        Color(0xFF9C27B0) // Purple for portal/entrance
                                    } else {
                                        Color(0xFF1976D2) // Blue for normal directions
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(buttonColor, CircleShape)
                                            .pointerInput(targetLocation.id) {
                                                detectTapGestures(
                                                    onPress = {
                                                        isPressed = true
                                                        tryAwaitRelease()
                                                        isPressed = false
                                                        expandedLocationId = targetLocation.id
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = if (exit.direction == ExitDirection.ENTER) "Enter area" else "Go ${exit.direction.name}",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                }

                // Location info at top-left (hidden when detail view is shown)
                if (!isDetailViewVisible) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth(0.6f)  // Leave room for minimap on right
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Location name at top
                        Text(
                            text = currentLocation.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Others (creatures) - show actual names, or vague descriptions when blinded
                        Text(
                            text = if (isBlinded) "Presences" else "Others",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        val creaturesHere = allCreatures.filter { it.id in currentLocation.creatureIds }
                        if (creaturesHere.isEmpty()) {
                            Text(
                                text = if (isBlinded) "You sense nothing nearby" else "None",
                                color = Color.White.copy(alpha = if (isBlinded) 0.6f else 1f),
                                fontSize = 14.sp
                            )
                        } else {
                            creaturesHere.forEachIndexed { index, creature ->
                                val state = creatureStates[creature.id] ?: "idle"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            // Can still click but won't know who it is
                                            selectedCreature = creature
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    if (!isBlinded) {
                                        // State icon - only show when not blinded
                                        CreatureStateIcon(
                                            state = state,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isBlinded) {
                                            // Generate vague description based on creature
                                            getBlindPresenceDescription(creature, index)
                                        } else {
                                            creature.name
                                        },
                                        color = if (isBlinded) Color.White.copy(alpha = 0.6f) else Color(0xFF64B5F6),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Items - show actual names, or vague descriptions when blinded
                        Text(
                            text = if (isBlinded) "Objects" else "Items",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        val itemsHere = allItems.filter { it.id in currentLocation.itemIds }
                        if (itemsHere.isEmpty()) {
                            Text(
                                text = if (isBlinded) "You feel nothing unusual" else "None",
                                color = Color.White.copy(alpha = if (isBlinded) 0.6f else 1f),
                                fontSize = 14.sp
                            )
                        } else {
                            itemsHere.forEachIndexed { index, item ->
                                Text(
                                    text = if (isBlinded) {
                                        getBlindItemDescription(item, index)
                                    } else {
                                        item.name
                                    },
                                    color = if (isBlinded) Color.White.copy(alpha = 0.6f) else Color(0xFFFFD54F),
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clickable { selectedItem = item }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Event log (fixed at bottom, full width) - max 4 lines
                if (eventLogEntries.isNotEmpty()) {
                    com.ez2bg.anotherthread.ui.components.EventLog(
                        entries = eventLogEntries,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(80.dp),
                        maxVisibleEntries = 4
                    )
                }

                // Detail view for creature/item (slides in from right)
                if (isDetailViewVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = (detailOffsetX * 400).dp)
                            .background(Color.Black.copy(alpha = 0.95f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val detailName = selectedCreature?.name ?: selectedItem?.name ?: ""
                        val detailImageUrl = selectedCreature?.imageUrl ?: selectedItem?.imageUrl

                        // Name above the 100x100
                        Text(
                            text = detailName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-120).dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        // 100x100 detail image - tappable to show description
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { showDescriptionPopup = true }
                                    )
                                }
                        ) {
                            if (detailImageUrl != null) {
                                AsyncImage(
                                    model = "${AppConfig.api.baseUrl}${detailImageUrl}",
                                    contentDescription = detailName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = detailName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            // Tap hint indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(16.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "i",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Action buttons around the 100x100 (same radius as exit buttons: 90dp)
                        // Back arrow (WEST position) - Orange
                        var isBackPressed by remember { mutableStateOf(false) }
                        val backScale by animateFloatAsState(
                            targetValue = if (isBackPressed) 1.3f else 1f,
                            animationSpec = tween(durationMillis = 150),
                            label = "backScale"
                        )
                        val backGlowAlpha by animateFloatAsState(
                            targetValue = if (isBackPressed) 0.8f else 0f,
                            animationSpec = tween(durationMillis = 150),
                            label = "backGlow"
                        )

                        Box(
                            modifier = Modifier
                                .offset(x = (-90).dp, y = 0.dp)
                                .size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((32 * backScale).dp)
                                    .background(
                                        Color(0xFFFF9800).copy(alpha = backGlowAlpha),
                                        CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9800), CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isBackPressed = true
                                                tryAwaitRelease()
                                                isBackPressed = false
                                                selectedCreature = null
                                                selectedItem = null
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Attack and Greet buttons only shown for creatures (not items)
                        if (selectedCreature != null) {
                            // Attack button (EAST position) - Sword
                            var isAttackPressed by remember { mutableStateOf(false) }
                            val attackScale by animateFloatAsState(
                                targetValue = if (isAttackPressed) 1.3f else 1f,
                                animationSpec = tween(durationMillis = 150),
                                label = "attackScale"
                            )
                            val attackGlowAlpha by animateFloatAsState(
                                targetValue = if (isAttackPressed) 0.8f else 0f,
                                animationSpec = tween(durationMillis = 150),
                                label = "attackGlow"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = 90.dp, y = 0.dp)
                                    .size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((32 * attackScale).dp)
                                        .background(
                                            Color(0xFFFF9800).copy(alpha = attackGlowAlpha),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD32F2F), CircleShape)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isAttackPressed = true
                                                    tryAwaitRelease()
                                                    isAttackPressed = false
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "lets fight!",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = SwordIcon,
                                        contentDescription = "Attack",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Greet button (SOUTH position) - Hand wave
                            var isGreetPressed by remember { mutableStateOf(false) }
                            val greetScale by animateFloatAsState(
                                targetValue = if (isGreetPressed) 1.3f else 1f,
                                animationSpec = tween(durationMillis = 150),
                                label = "greetScale"
                            )
                            val greetGlowAlpha by animateFloatAsState(
                                targetValue = if (isGreetPressed) 0.8f else 0f,
                                animationSpec = tween(durationMillis = 150),
                                label = "greetGlow"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = 0.dp, y = 90.dp)
                                    .size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((32 * greetScale).dp)
                                        .background(
                                            Color(0xFFFF9800).copy(alpha = greetGlowAlpha),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50), CircleShape)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isGreetPressed = true
                                                    tryAwaitRelease()
                                                    isGreetPressed = false
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "you say sup dude!",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Greet",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Creature abilities display (NORTH-EAST position, below name)
                            val creature = selectedCreature
                            val creatureAbilities = creature?.abilityIds?.mapNotNull { allAbilitiesMap[it] } ?: emptyList()
                            if (creatureAbilities.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .offset(y = (-160).dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.8f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Abilities:",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                    creatureAbilities.take(4).forEach { ability ->
                                        AbilityIconSmall(
                                            ability = ability,
                                            size = 24.dp
                                        )
                                    }
                                    if (creatureAbilities.size > 4) {
                                        Text(
                                            text = "+${creatureAbilities.size - 4}",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Description popup overlay (shown when tapping image)
                        if (showDescriptionPopup) {
                            val description = selectedCreature?.desc ?: selectedItem?.desc ?: ""
                            val creature = selectedCreature
                            val item = selectedItem

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.9f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { showDescriptionPopup = false }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(
                                            Color(0xFF1A1A1A),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFFFF9800),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Enlarged image (150x150)
                                    Box(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                            .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                    ) {
                                        if (detailImageUrl != null) {
                                            AsyncImage(
                                                model = "${AppConfig.api.baseUrl}${detailImageUrl}",
                                                contentDescription = detailName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Name
                                    Text(
                                        text = detailName,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Description
                                    if (description.isNotEmpty()) {
                                        Text(
                                            text = description,
                                            color = Color(0xFFCCCCCC),
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }

                                    // Creature stats
                                    if (creature != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Level", color = Color.Gray, fontSize = 10.sp)
                                                Text(
                                                    "${creature.level}",
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("HP", color = Color.Gray, fontSize = 10.sp)
                                                Text(
                                                    "${creature.maxHp}",
                                                    color = Color(0xFFFF5252),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Damage", color = Color.Gray, fontSize = 10.sp)
                                                Text(
                                                    "${creature.baseDamage}",
                                                    color = Color(0xFFFFAB40),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("XP", color = Color.Gray, fontSize = 10.sp)
                                                Text(
                                                    "${creature.experienceValue}",
                                                    color = Color(0xFF69F0AE),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Aggressive indicator
                                        if (creature.isAggressive) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Dangerous,
                                                    contentDescription = "Aggressive",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Aggressive",
                                                    color = Color(0xFFFF5252),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    // Item stats
                                    if (item != null) {
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Equipment type badge
                                        if (item.equipmentType != null) {
                                            Row(
                                                modifier = Modifier
                                                    .background(
                                                        Color(0xFF2196F3).copy(alpha = 0.2f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.equipmentType.replaceFirstChar { it.uppercase() },
                                                    color = Color(0xFF2196F3),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (item.equipmentSlot != null) {
                                                    Text(
                                                        text = " \u2022 ${item.equipmentSlot.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                                                        color = Color(0xFF2196F3).copy(alpha = 0.7f),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        // Stat bonuses
                                        val statBonuses = item.statBonuses
                                        if (statBonuses != null && (statBonuses.attack != 0 || statBonuses.defense != 0 || statBonuses.maxHp != 0)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                if (statBonuses.attack != 0) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Attack", color = Color.Gray, fontSize = 10.sp)
                                                        Text(
                                                            "${if (statBonuses.attack > 0) "+" else ""}${statBonuses.attack}",
                                                            color = if (statBonuses.attack > 0) Color(0xFF69F0AE) else Color(0xFFFF5252),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                if (statBonuses.defense != 0) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Defense", color = Color.Gray, fontSize = 10.sp)
                                                        Text(
                                                            "${if (statBonuses.defense > 0) "+" else ""}${statBonuses.defense}",
                                                            color = if (statBonuses.defense > 0) Color(0xFF448AFF) else Color(0xFFFF5252),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                if (statBonuses.maxHp != 0) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Max HP", color = Color.Gray, fontSize = 10.sp)
                                                        Text(
                                                            "${if (statBonuses.maxHp > 0) "+" else ""}${statBonuses.maxHp}",
                                                            color = if (statBonuses.maxHp > 0) Color(0xFFFF5252) else Color.Gray,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        // Gold value
                                        if (item.value > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "${item.value}",
                                                    color = Color(0xFFFFD700),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "gold",
                                                    color = Color(0xFFFFD700).copy(alpha = 0.7f),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Tap to close hint
                                    Text(
                                        text = "Tap anywhere to close",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Snackbar host for toast messages
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // 2. Minimap in top-right - just dots and connection lines, no terrain
                if (currentPos != null && !isDetailViewVisible) {
                    // Scale multiplier for minimap - adjust to change overall minimap size
                    // 1.0 = default, 0.75 = 75% of original, etc.
                    val minimapScale = 0.75f

                    val minimapSize = (140 * minimapScale).dp
                    // Grid spacing - how many dp per grid unit in the minimap
                    val gridSpacing = (35 * minimapScale).dp  // Each dot is 35dp apart in the minimap

                    // Animated offset for smooth sliding when changing locations
                    val currentGridX = currentLocation.gridX ?: 0
                    val currentGridY = currentLocation.gridY ?: 0
                    val animatedGridX by animateFloatAsState(
                        targetValue = currentGridX.toFloat(),
                        animationSpec = tween(durationMillis = 300),
                        label = "minimapX"
                    )
                    val animatedGridY by animateFloatAsState(
                        targetValue = currentGridY.toFloat(),
                        animationSpec = tween(durationMillis = 300),
                        label = "minimapY"
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    Box(
                        modifier = Modifier
                            .size(minimapSize)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                    ) {
                        // Draw on a Canvas - dots and lines only
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val gridSpacingPx = gridSpacing.toPx()
                            val dotRadius = (8 * minimapScale).dp.toPx()
                            val highlightRadius = (12 * minimapScale).dp.toPx()
                            val lineColor = Color(0xFFFF9800).copy(alpha = 0.7f)
                            val lineWidth = (3 * minimapScale).dp.toPx()
                            val highlightStrokeWidth = (3 * minimapScale).dp.toPx()
                            val dotOutlineWidth = (1.5f * minimapScale).dp.toPx()

                            // Vignette effect - fade elements near edges
                            // Returns alpha multiplier (0.0 to 1.0) based on distance from center
                            val vignetteRadius = minOf(size.width, size.height) / 2
                            fun vignetteAlpha(x: Float, y: Float): Float {
                                val dx = x - centerX
                                val dy = y - centerY
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                // Start fading at 60% of radius, fully faded at 100%
                                val fadeStart = vignetteRadius * 0.6f
                                val fadeEnd = vignetteRadius * 1.0f
                                return when {
                                    dist <= fadeStart -> 1f
                                    dist >= fadeEnd -> 0f
                                    else -> 1f - ((dist - fadeStart) / (fadeEnd - fadeStart))
                                }
                            }

                            // Build location lookup map
                            val locationById = locations.associateBy { it.id }

                            // Draw connections up to 3 hops from current location using BFS
                            // Track visited connections to avoid drawing duplicates
                            val drawnConnections = mutableSetOf<Pair<String, String>>()
                            val visited = mutableSetOf<String>()
                            val queue = ArrayDeque<Pair<String, Int>>() // locationId, depth

                            queue.add(currentLocation.id to 0)
                            visited.add(currentLocation.id)

                            while (queue.isNotEmpty()) {
                                val (locId, depth) = queue.removeFirst()
                                if (depth >= 3) continue // Stop at 3 hops

                                val loc = locationById[locId] ?: continue
                                val locGridX = loc.gridX ?: continue
                                val locGridY = loc.gridY ?: continue
                                val locRelX = locGridX - animatedGridX
                                val locRelY = locGridY - animatedGridY

                                // Draw exits from this location
                                loc.exits.forEach { exit ->
                                    val targetLoc = locationById[exit.locationId] ?: return@forEach
                                    val targetGridX = targetLoc.gridX ?: return@forEach
                                    val targetGridY = targetLoc.gridY ?: return@forEach
                                    val targetRelX = targetGridX - animatedGridX
                                    val targetRelY = targetGridY - animatedGridY

                                    // Create a canonical connection key to avoid drawing same line twice
                                    val connKey = if (locId < exit.locationId) locId to exit.locationId else exit.locationId to locId

                                    // Only draw if both are visible and not already drawn (use wider bounds during animation)
                                    if (kotlin.math.abs(locRelX) <= 3 && kotlin.math.abs(locRelY) <= 3 &&
                                        kotlin.math.abs(targetRelX) <= 3 && kotlin.math.abs(targetRelY) <= 3 &&
                                        !drawnConnections.contains(connKey)) {

                                        drawnConnections.add(connKey)

                                        val fromX = centerX + locRelX * gridSpacingPx
                                        val fromY = centerY + locRelY * gridSpacingPx
                                        val toX = centerX + targetRelX * gridSpacingPx
                                        val toY = centerY + targetRelY * gridSpacingPx

                                        // Fade color based on depth
                                        val depthAlpha = when (depth) {
                                            0 -> 0.9f
                                            1 -> 0.6f
                                            else -> 0.4f
                                        }

                                        // Apply vignette fade - use minimum of both endpoints
                                        // so line fades when either connected dot fades
                                        val fromVignette = vignetteAlpha(fromX, fromY)
                                        val toVignette = vignetteAlpha(toX, toY)
                                        val vignetteMultiplier = minOf(fromVignette, toVignette)
                                        val finalAlpha = depthAlpha * vignetteMultiplier

                                        if (finalAlpha > 0.01f) {
                                            // Shorten line to stop at dot edges (not center)
                                            val dx = toX - fromX
                                            val dy = toY - fromY
                                            val length = kotlin.math.sqrt(dx * dx + dy * dy)
                                            if (length > dotRadius * 2) {
                                                val shortenAmount = dotRadius + 1.dp.toPx()
                                                val ratio = shortenAmount / length
                                                val adjustedFromX = fromX + dx * ratio
                                                val adjustedFromY = fromY + dy * ratio
                                                val adjustedToX = toX - dx * ratio
                                                val adjustedToY = toY - dy * ratio

                                                drawLine(
                                                    color = lineColor.copy(alpha = finalAlpha),
                                                    start = Offset(adjustedFromX, adjustedFromY),
                                                    end = Offset(adjustedToX, adjustedToY),
                                                    strokeWidth = lineWidth
                                                )
                                            }
                                        }
                                    }

                                    // Add target to queue if not visited
                                    if (!visited.contains(exit.locationId)) {
                                        visited.add(exit.locationId)
                                        queue.add(exit.locationId to depth + 1)
                                    }
                                }
                            }

                            // Draw dots
                            locations.forEach { location ->
                                val locGridX = location.gridX ?: return@forEach
                                val locGridY = location.gridY ?: return@forEach

                                // Position relative to animated current location
                                val relX = locGridX - animatedGridX
                                val relY = locGridY - animatedGridY

                                // Only draw if within ~3 grid units (wider during animation)
                                if (kotlin.math.abs(relX) <= 3 && kotlin.math.abs(relY) <= 3) {
                                    val dotX = centerX + relX * gridSpacingPx
                                    val dotY = centerY + relY * gridSpacingPx
                                    val isCurrentLoc = location.id == expandedLocationId

                                    // Apply vignette fade (current location always fully visible)
                                    val dotVignetteAlpha = if (isCurrentLoc) 1f else vignetteAlpha(dotX, dotY)

                                    if (dotVignetteAlpha > 0.01f) {
                                        // Highlight for current location
                                        if (isCurrentLoc) {
                                            drawCircle(
                                                color = Color(0xFFFF9800),
                                                radius = highlightRadius,
                                                center = Offset(dotX, dotY),
                                                style = Stroke(width = highlightStrokeWidth)
                                            )
                                        }

                                        // Dot fill
                                        val terrainColor = getTerrainColor(location.desc, location.name)
                                        drawCircle(
                                            color = terrainColor.copy(alpha = terrainColor.alpha * dotVignetteAlpha),
                                            radius = dotRadius,
                                            center = Offset(dotX, dotY)
                                        )

                                        // Dot outline
                                        drawCircle(
                                            color = Color(0xFFFF9800).copy(alpha = 0.6f * dotVignetteAlpha),
                                            radius = dotRadius,
                                            center = Offset(dotX, dotY),
                                            style = Stroke(width = dotOutlineWidth)
                                        )
                                    }
                                }
                            }

                            // Fog of war - radial gradient overlay from transparent center to opaque edges
                            val fogRadius = size.minDimension / 2
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Black.copy(alpha = 0.9f)
                                    ),
                                    center = Offset(centerX, centerY),
                                    radius = fogRadius
                                ),
                                radius = fogRadius,
                                center = Offset(centerX, centerY)
                            )
                        }
                    }
                        // Coordinates text below minimap
                        Text(
                            text = "($currentGridX, $currentGridY)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Target selection overlay for single-target abilities
                if (pendingAbility != null && currentLocation != null) {
                    val ability = pendingAbility!!
                    // Get creatures at this location from the allCreatures list
                    val creaturesHere = allCreatures.filter { it.id in currentLocation.creatureIds }
                    val targets = creaturesHere.map { creature ->
                        CombatTarget(
                            id = creature.id,
                            name = creature.name,
                            currentHp = creature.maxHp, // Creatures start at full HP
                            maxHp = creature.maxHp,
                            isPlayer = false,
                            isAlive = true // Assume alive for target selection
                        )
                    }

                    TargetSelectionOverlay(
                        ability = ability,
                        targets = targets,
                        onTargetSelected = { targetId ->
                            scope.launch {
                                activeCombatClient?.useAbility(ability.id, targetId)
                                val targetName = targets.find { it.id == targetId }?.name ?: "target"
                                snackbarHostState.showSnackbar(
                                    "Casting ${ability.name} on $targetName",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            pendingAbility = null
                        },
                        onCancel = {
                            pendingAbility = null
                        }
                    )
                }
            }
        }

        // Game mode toggle (top-right corner) - only visible when signed in
        // Enhanced game mode toggle (top-right corner) - only visible when signed in
        if (currentUser != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode label
                    Text(
                        text = if (gameMode.isCreate) "CREATE" else "EXPLORE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    
                    // Enhanced toggle switch
                    val isCreateMode = gameMode.isCreate
                    val trackColor = if (isCreateMode) {
                        Color(0xFF4CAF50).copy(alpha = 0.3f)
                    } else {
                        Color(0xFF9C27B0).copy(alpha = 0.3f)
                    }
                    val thumbColor = if (isCreateMode) Color(0xFF4CAF50) else Color(0xFFBA68C8)
                    
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(24.dp)
                            .background(trackColor, RoundedCornerShape(12.dp))
                            .clickable {
                                onGameModeChange(if (gameMode.isCreate) GameMode.ADVENTURE else GameMode.CREATE)
                            },
                        contentAlignment = if (isCreateMode) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(18.dp)
                                .background(thumbColor, CircleShape)
                        )
                    }
                }
            }
        }
        // Enhanced zoom controls overlay (top-right corner, below toggle) - hidden in Adventure mode
        if (gameMode.isCreate) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zoom in button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.White.copy(alpha = 0.1f), 
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val newScale = (scale * 1.25f).coerceIn(minScale, maxScale)
                                if (newScale != scale) {
                                    // Zoom toward center
                                    val centerX = width / 2
                                    val centerY = height / 2
                                    val scaleFactor = newScale / scale
                                    val newOffsetX = centerX - (centerX - offset.value.x) * scaleFactor
                                    val newOffsetY = centerY - (centerY - offset.value.y) * scaleFactor
                                    scale = newScale
                                    scope.launch { offset.snapTo(Offset(newOffsetX, newOffsetY)) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+", 
                            color = Color.White, 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Zoom level indicator
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Zoom out button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.White.copy(alpha = 0.1f), 
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val newScale = (scale * 0.8f).coerceIn(minScale, maxScale)
                                if (newScale != scale) {
                                    // Zoom toward center
                                    val centerX = width / 2
                                    val centerY = height / 2
                                    val scaleFactor = newScale / scale
                                    val newOffsetX = centerX - (centerX - offset.value.x) * scaleFactor
                                    val newOffsetY = centerY - (centerY - offset.value.y) * scaleFactor
                                    scale = newScale
                                    scope.launch { offset.snapTo(Offset(newOffsetX, newOffsetY)) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "−", 
                            color = Color.White, 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    // Reset zoom/pan button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.White.copy(alpha = 0.1f), 
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                scale = 1f
                                scope.launch { offset.animateTo(Offset.Zero, tween(300)) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusWeak,
                            contentDescription = "Reset view",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
}
