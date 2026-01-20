package com.ez2bg.anotherthread.ui

import com.ez2bg.anotherthread.util.SimplexNoise
import com.ez2bg.anotherthread.util.VoronoiNoise
import com.ez2bg.anotherthread.util.BiomeBlender
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Pets
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.platform.readFileBytes
import com.ez2bg.anotherthread.storage.AuthStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Data class representing a completed image generation
 */
data class ImageGenerationResult(
    val entityType: String,
    val entityId: String,
    val imageUrl: String
)

/**
 * Singleton manager for background image generation.
 * Jobs continue even if the user navigates away from the entity.
 */
object BackgroundImageGenerationManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Track which entities are currently generating images
    private val _generatingEntities = MutableStateFlow<Set<String>>(emptySet())
    val generatingEntities = _generatingEntities.asStateFlow()

    // Emit completed image generations
    private val _imageCompletions = MutableSharedFlow<ImageGenerationResult>(extraBufferCapacity = 10)
    val imageCompletions = _imageCompletions.asSharedFlow()

    // Track errors by entity ID
    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors = _errors.asStateFlow()

    fun isGenerating(entityId: String): Boolean = entityId in _generatingEntities.value

    fun getError(entityId: String): String? = _errors.value[entityId]

    fun clearError(entityId: String) {
        _errors.value = _errors.value - entityId
    }

    fun startGeneration(
        entityType: String,
        entityId: String,
        name: String,
        description: String,
        featureIds: List<String> = emptyList()
    ) {
        // Don't start if already generating
        if (isGenerating(entityId)) return

        // Mark as generating
        _generatingEntities.value = _generatingEntities.value + entityId
        _errors.value = _errors.value - entityId

        scope.launch {
            ApiClient.generateImage(
                entityType = entityType,
                entityId = entityId,
                name = name,
                description = description,
                featureIds = featureIds
            ).onSuccess { response ->
                // Emit the completion
                _imageCompletions.emit(
                    ImageGenerationResult(
                        entityType = entityType,
                        entityId = entityId,
                        imageUrl = response.imageUrl
                    )
                )
            }.onFailure { error ->
                // Store the error
                _errors.value = _errors.value + (entityId to (error.message ?: "Failed to generate image"))
            }

            // Mark as no longer generating
            _generatingEntities.value = _generatingEntities.value - entityId
        }
    }
}

// Custom sword icon for Items tab
val SwordIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Sword",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            // Blade
            moveTo(6.92f, 5f)
            lineTo(5f, 6.92f)
            lineTo(13.06f, 15f)
            lineTo(10f, 18.06f)
            lineTo(11.94f, 20f)
            lineTo(15f, 16.94f)
            lineTo(16.06f, 18f)
            lineTo(18f, 16.06f)
            lineTo(16.94f, 15f)
            lineTo(20f, 11.94f)
            lineTo(18.06f, 10f)
            lineTo(15f, 13.06f)
            lineTo(6.92f, 5f)
            close()
            // Handle
            moveTo(4.5f, 18.5f)
            lineTo(5.5f, 19.5f)
            lineTo(8.5f, 16.5f)
            lineTo(7.5f, 15.5f)
            close()
        }
    }.build()
}

enum class AdminTab(val title: String, val icon: ImageVector) {
    LOCATION("Location", Icons.Filled.Place),
    CREATURE("Creature", Icons.Filled.Pets),
    ITEM("Item", SwordIcon),
    USER("User", Icons.Filled.Person)
}

enum class EntityType {
    LOCATION, CREATURE, ITEM
}

data class IdOption(
    val id: String,
    val name: String
)

/**
 * Displays an entity's generated image with loading/error states
 */
@Composable
fun EntityImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isGenerating: Boolean = false
) {
    if (imageUrl != null) {
        val fullUrl = "${AppConfig.api.baseUrl}$imageUrl"
        var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = fullUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.FillWidth,
                onState = { imageState = it }
            )

            when (imageState) {
                is AsyncImagePainter.State.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
                is AsyncImagePainter.State.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Image unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {}
            }
        }
    } else if (isGenerating) {
        // Show generating state only when actually generating
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Image generating...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
    // If no imageUrl and not generating, show nothing (no placeholder)
}

enum class GenEntityType {
    LOCATION, CREATURE, ITEM, USER
}

/**
 * Button to generate name and description using LLM
 */
@Composable
fun GenButton(
    entityType: GenEntityType,
    currentName: String,
    currentDesc: String,
    exitIds: List<String> = emptyList(),
    featureIds: List<String> = emptyList(),
    onGenerated: (name: String, description: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        // For locations, require at least 3 characters in the name
        val isNameValid = when (entityType) {
            GenEntityType.LOCATION -> currentName.length >= 3
            else -> true
        }

        Button(
            onClick = {
                scope.launch {
                    isGenerating = true
                    errorMessage = null

                    val result = when (entityType) {
                        GenEntityType.LOCATION -> ApiClient.generateLocationContent(
                            exitIds = exitIds,
                            featureIds = featureIds,
                            existingName = currentName.ifBlank { null },
                            existingDesc = currentDesc.ifBlank { null }
                        )
                        GenEntityType.CREATURE -> ApiClient.generateCreatureContent(
                            existingName = currentName.ifBlank { null },
                            existingDesc = currentDesc.ifBlank { null }
                        )
                        GenEntityType.ITEM -> ApiClient.generateItemContent(
                            existingName = currentName.ifBlank { null },
                            existingDesc = currentDesc.ifBlank { null }
                        )
                        GenEntityType.USER -> Result.failure(Exception("Content generation not supported for users"))
                    }

                    isGenerating = false

                    result.onSuccess { content ->
                        onGenerated(content.name, content.description)
                    }.onFailure { error ->
                        errorMessage = error.message ?: "Failed to generate content"
                    }
                }
            },
            enabled = enabled && !isGenerating && isNameValid
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating...")
            } else {
                Text(if (currentDesc.isBlank()) "Gen" else "Regen")
            }
        }

        errorMessage?.let {
            SelectionContainer {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Button to generate an image for an entity using Stable Diffusion.
 * Generation runs in the background - user can navigate away and the image
 * will update when complete.
 */
@Composable
fun GenerateImageButton(
    entityType: GenEntityType,
    entityId: String,
    name: String,
    description: String,
    featureIds: List<String> = emptyList(),
    onImageGenerated: (String) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Observe generation state from the background manager
    val generatingEntities by BackgroundImageGenerationManager.generatingEntities.collectAsState()
    val errors by BackgroundImageGenerationManager.errors.collectAsState()

    val isGenerating = entityId in generatingEntities
    val error = errors[entityId]

    // Listen for image completions for this entity
    LaunchedEffect(entityId) {
        BackgroundImageGenerationManager.imageCompletions.collect { result ->
            if (result.entityId == entityId) {
                onImageGenerated(result.imageUrl)
            }
        }
    }

    // Report errors when they occur
    LaunchedEffect(error) {
        if (error != null) {
            onError(error)
            BackgroundImageGenerationManager.clearError(entityId)
        }
    }

    val entityTypeStr = when (entityType) {
        GenEntityType.LOCATION -> "location"
        GenEntityType.CREATURE -> "creature"
        GenEntityType.ITEM -> "item"
        GenEntityType.USER -> "user"
    }

    val isEnabled = enabled && name.isNotBlank() && description.isNotBlank() && !isGenerating

    Button(
        onClick = {
            onError(null)
            // Start background generation - returns immediately
            BackgroundImageGenerationManager.startGeneration(
                entityType = entityTypeStr,
                entityId = entityId,
                name = name,
                description = description,
                featureIds = featureIds
            )
        },
        enabled = isEnabled,
        modifier = modifier
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generating...")
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Gen Image")
        }
    }
}

// Simple counter to generate unique refresh keys
private var refreshKeyCounter = 0L
private fun nextRefreshKey(): Long = ++refreshKeyCounter

sealed class ViewState {
    data object UserAuth : ViewState()
    data class UserProfile(val user: UserDto) : ViewState()
    data class UserDetail(val userId: String) : ViewState()  // View other user's profile (read-only for non-admins)
    data class LocationGraph(val refreshKey: Long = nextRefreshKey()) : ViewState()
    data object LocationCreate : ViewState()
    data class LocationEdit(val location: LocationDto) : ViewState()
    data object CreatureList : ViewState()
    data object CreatureCreate : ViewState()
    data class CreatureEdit(val creature: CreatureDto) : ViewState()
    data class CreatureDetail(val id: String) : ViewState()
    data object ItemList : ViewState()
    data object ItemCreate : ViewState()
    data class ItemEdit(val item: ItemDto) : ViewState()
    data class ItemDetail(val id: String) : ViewState()
    data object AdminPanel : ViewState()
    data object AuditLogs : ViewState()
}

/**
 * Header with "Another Thread" text and dragon line art behind it
 */
@Composable
fun DragonHeader(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dragon line art behind text
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 2.dp.toPx()

            // Dragon body - flowing S-curve across the header
            val dragonPath = Path().apply {
                // Start from left side - tail
                moveTo(w * 0.05f, h * 0.7f)
                // Tail curves up
                cubicTo(
                    w * 0.1f, h * 0.3f,
                    w * 0.15f, h * 0.2f,
                    w * 0.22f, h * 0.4f
                )
                // Body curves through middle
                cubicTo(
                    w * 0.3f, h * 0.7f,
                    w * 0.4f, h * 0.8f,
                    w * 0.5f, h * 0.5f
                )
                // Body continues to right
                cubicTo(
                    w * 0.6f, h * 0.2f,
                    w * 0.7f, h * 0.3f,
                    w * 0.78f, h * 0.5f
                )
                // Neck curves up
                cubicTo(
                    w * 0.85f, h * 0.7f,
                    w * 0.9f, h * 0.6f,
                    w * 0.92f, h * 0.35f
                )
                // Head
                cubicTo(
                    w * 0.93f, h * 0.25f,
                    w * 0.96f, h * 0.2f,
                    w * 0.98f, h * 0.25f
                )
            }

            drawPath(
                path = dragonPath,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Wing on the left side
            val wingPath1 = Path().apply {
                moveTo(w * 0.25f, h * 0.4f)
                cubicTo(
                    w * 0.2f, h * 0.1f,
                    w * 0.3f, h * 0.05f,
                    w * 0.38f, h * 0.15f
                )
                cubicTo(
                    w * 0.42f, h * 0.25f,
                    w * 0.4f, h * 0.5f,
                    w * 0.35f, h * 0.55f
                )
            }

            drawPath(
                path = wingPath1,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth * 0.8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Wing on the right side
            val wingPath2 = Path().apply {
                moveTo(w * 0.75f, h * 0.5f)
                cubicTo(
                    w * 0.72f, h * 0.2f,
                    w * 0.8f, h * 0.1f,
                    w * 0.88f, h * 0.2f
                )
                cubicTo(
                    w * 0.92f, h * 0.28f,
                    w * 0.88f, h * 0.45f,
                    w * 0.82f, h * 0.55f
                )
            }

            drawPath(
                path = wingPath2,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth * 0.8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Spikes along the body
            val spikePositions = listOf(
                Pair(w * 0.3f, h * 0.65f),
                Pair(w * 0.4f, h * 0.75f),
                Pair(w * 0.5f, h * 0.5f),
                Pair(w * 0.6f, h * 0.25f),
                Pair(w * 0.7f, h * 0.35f)
            )

            spikePositions.forEach { (x, y) ->
                val spikePath = Path().apply {
                    moveTo(x - 5.dp.toPx(), y)
                    lineTo(x, y - 10.dp.toPx())
                    lineTo(x + 5.dp.toPx(), y)
                }
                drawPath(
                    path = spikePath,
                    color = lineColor,
                    style = Stroke(
                        width = strokeWidth * 0.6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Tail tip flourish
            val tailPath = Path().apply {
                moveTo(w * 0.05f, h * 0.7f)
                cubicTo(
                    w * 0.02f, h * 0.8f,
                    w * 0.01f, h * 0.9f,
                    w * 0.03f, h * 0.95f
                )
            }

            drawPath(
                path = tailPath,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth * 0.7f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Title text on top
        Text(
            text = "Another Thread",
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AdminScreen() {
    // Restore persisted auth state on startup
    val savedUser = remember { AuthStorage.getUser() }
    var selectedTab by remember { mutableStateOf(AdminTab.LOCATION) }
    var viewState by remember { mutableStateOf<ViewState>(ViewState.LocationGraph()) }
    var currentUser by remember { mutableStateOf(savedUser) }

    // Separate refresh key for forcing location graph refresh after CRUD operations
    var locationGraphRefreshKey by remember { mutableStateOf(0L) }
    fun refreshLocationGraph() {
        locationGraphRefreshKey++
        println("DEBUG: refreshLocationGraph() called, new key: $locationGraphRefreshKey")
    }

    // Set user context for audit logging when user changes
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            ApiClient.setUserContext(currentUser!!.id, currentUser!!.name)
        } else {
            ApiClient.clearUserContext()
        }
    }

    // Refresh user data from server on startup to get latest featureIds, etc.
    LaunchedEffect(savedUser?.id) {
        savedUser?.let { user ->
            ApiClient.getUser(user.id).onSuccess { freshUser ->
                if (freshUser != null) {
                    AuthStorage.saveUser(freshUser)
                    currentUser = freshUser
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DragonHeader(modifier = Modifier.padding(bottom = 4.dp))

        // Check if current user has admin privilege
        val isAdmin = currentUser?.featureIds?.contains(ADMIN_FEATURE_ID) == true

        // All tabs are visible
        val visibleTabs = AdminTab.entries

        TabRow(selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)) {
            visibleTabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        viewState = when (tab) {
                            AdminTab.USER -> if (currentUser != null) ViewState.UserProfile(currentUser!!) else ViewState.UserAuth
                            AdminTab.LOCATION -> ViewState.LocationGraph()
                            AdminTab.CREATURE -> ViewState.CreatureList
                            AdminTab.ITEM -> ViewState.ItemList
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = viewState) {
            is ViewState.UserAuth -> UserAuthView(
                onAuthenticated = { user ->
                    AuthStorage.saveUser(user)
                    currentUser = user
                    viewState = ViewState.UserProfile(user)
                }
            )
            is ViewState.UserProfile -> UserProfileView(
                user = state.user,
                currentUser = currentUser,
                isAdmin = isAdmin,
                onUserUpdated = { user ->
                    AuthStorage.saveUser(user)
                    currentUser = user
                    viewState = ViewState.UserProfile(user)
                },
                onLogout = {
                    AuthStorage.clearUser()
                    currentUser = null
                    viewState = ViewState.UserAuth
                },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                onBack = null,  // No back button for own profile
                onNavigateToAdmin = if (isAdmin) {{ viewState = ViewState.AdminPanel }} else null
            )
            is ViewState.UserDetail -> UserDetailView(
                userId = state.userId,
                currentUser = currentUser,
                isAdmin = isAdmin,
                onBack = {
                    selectedTab = AdminTab.LOCATION
                    viewState = ViewState.LocationGraph()
                },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                }
            )
            is ViewState.LocationGraph -> key(locationGraphRefreshKey) {
                LocationGraphView(
                    refreshKey = locationGraphRefreshKey,
                    onAddClick = { viewState = ViewState.LocationCreate },
                    onLocationClick = { location -> viewState = ViewState.LocationEdit(location) },
                    isAuthenticated = currentUser != null,
                    isAdmin = isAdmin,
                    currentUser = currentUser,
                    onLoginClick = {
                        selectedTab = AdminTab.USER
                        viewState = ViewState.UserAuth
                    }
                )
            }
            is ViewState.LocationCreate -> LocationForm(
                editLocation = null,
                onBack = { viewState = ViewState.LocationGraph() },
                onSaved = { refreshLocationGraph(); viewState = ViewState.LocationGraph() },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                onNavigateToCreature = { id ->
                    selectedTab = AdminTab.CREATURE
                    viewState = ViewState.CreatureDetail(id)
                },
                onNavigateToLocation = { location -> viewState = ViewState.LocationEdit(location) },
                onNavigateToUser = { userId ->
                    selectedTab = AdminTab.USER
                    viewState = ViewState.UserDetail(userId)
                },
                currentUser = currentUser,
                isAdmin = isAdmin,
                onDeleted = { refreshLocationGraph(); viewState = ViewState.LocationGraph() }
            )
            is ViewState.LocationEdit -> LocationForm(
                editLocation = state.location,
                onBack = { viewState = ViewState.LocationGraph() },
                onSaved = { refreshLocationGraph(); viewState = ViewState.LocationGraph() },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                onNavigateToCreature = { id ->
                    selectedTab = AdminTab.CREATURE
                    viewState = ViewState.CreatureDetail(id)
                },
                onNavigateToLocation = { location -> viewState = ViewState.LocationEdit(location) },
                onNavigateToUser = { userId ->
                    selectedTab = AdminTab.USER
                    viewState = ViewState.UserDetail(userId)
                },
                currentUser = currentUser,
                isAdmin = isAdmin,
                onLocationUpdated = { updatedLocation ->
                    viewState = ViewState.LocationEdit(updatedLocation)
                },
                onDeleted = { refreshLocationGraph(); viewState = ViewState.LocationGraph() }
            )
            is ViewState.CreatureList -> CreatureListView(
                onCreatureClick = { creature ->
                    viewState = ViewState.CreatureEdit(creature)
                },
                onAddClick = {
                    viewState = ViewState.CreatureCreate
                },
                isAuthenticated = currentUser != null
            )
            is ViewState.CreatureCreate -> CreatureForm(
                editCreature = null,
                onBack = { viewState = ViewState.CreatureList },
                onSaved = { viewState = ViewState.CreatureList },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                currentUser = currentUser,
                isAdmin = isAdmin
            )
            is ViewState.CreatureEdit -> CreatureForm(
                editCreature = state.creature,
                onBack = { viewState = ViewState.CreatureList },
                onSaved = { viewState = ViewState.CreatureList },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                currentUser = currentUser,
                isAdmin = isAdmin,
                onCreatureUpdated = { updatedCreature ->
                    viewState = ViewState.CreatureEdit(updatedCreature)
                }
            )
            is ViewState.CreatureDetail -> CreatureDetailView(
                creatureId = state.id,
                onBack = {
                    selectedTab = AdminTab.LOCATION
                    viewState = ViewState.LocationGraph()
                },
                onEdit = { creature ->
                    selectedTab = AdminTab.CREATURE
                    viewState = ViewState.CreatureEdit(creature)
                },
                onCreateNew = {
                    selectedTab = AdminTab.CREATURE
                    viewState = ViewState.CreatureCreate
                },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                isAdmin = isAdmin
            )
            is ViewState.ItemList -> ItemListView(
                onItemClick = { item ->
                    viewState = ViewState.ItemEdit(item)
                },
                onAddClick = {
                    viewState = ViewState.ItemCreate
                },
                isAuthenticated = currentUser != null
            )
            is ViewState.ItemCreate -> ItemForm(
                editItem = null,
                onBack = { viewState = ViewState.ItemList },
                onSaved = { viewState = ViewState.ItemList },
                currentUser = currentUser,
                isAdmin = isAdmin
            )
            is ViewState.ItemEdit -> ItemForm(
                editItem = state.item,
                onBack = { viewState = ViewState.ItemList },
                onSaved = { viewState = ViewState.ItemList },
                currentUser = currentUser,
                isAdmin = isAdmin,
                onItemUpdated = { updatedItem ->
                    viewState = ViewState.ItemEdit(updatedItem)
                }
            )
            is ViewState.ItemDetail -> ItemDetailView(
                itemId = state.id,
                onBack = {
                    selectedTab = AdminTab.LOCATION
                    viewState = ViewState.LocationGraph()
                },
                onEdit = { item ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemEdit(item)
                },
                onCreateNew = {
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemCreate
                },
                isAdmin = isAdmin
            )
            is ViewState.AdminPanel -> AdminPanelView(
                onViewAuditLogs = { viewState = ViewState.AuditLogs }
            )
            is ViewState.AuditLogs -> AuditLogsView(
                onBack = { viewState = ViewState.AdminPanel }
            )
        }
    }
}

@Composable
fun UserAuthView(
    onAuthenticated: (UserDto) -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLoginMode) "Login" else "Register",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMessage = null },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (!isLoginMode) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                scope.launch {
                    if (!isLoginMode && password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@launch
                    }

                    isLoading = true
                    errorMessage = null

                    val result = if (isLoginMode) {
                        ApiClient.login(name, password)
                    } else {
                        ApiClient.register(name, password)
                    }

                    isLoading = false

                    result.onSuccess { response ->
                        if (response.success && response.user != null) {
                            onAuthenticated(response.user)
                        } else {
                            errorMessage = response.message
                        }
                    }.onFailure { error ->
                        errorMessage = error.message ?: "An error occurred"
                    }
                }
            },
            enabled = !isLoading && name.isNotBlank() && password.isNotBlank() &&
                    (isLoginMode || confirmPassword.isNotBlank()),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isLoginMode) "Login" else "Register")
            }
        }

        TextButton(
            onClick = {
                isLoginMode = !isLoginMode
                errorMessage = null
            }
        ) {
            Text(
                if (isLoginMode) "Don't have an account? Register"
                else "Already have an account? Login"
            )
        }
    }
}

@Composable
fun UserProfileView(
    user: UserDto,
    currentUser: UserDto?,
    isAdmin: Boolean,
    onUserUpdated: (UserDto) -> Unit,
    onLogout: () -> Unit,
    onNavigateToItem: (String) -> Unit,
    onBack: (() -> Unit)?,  // null for own profile, non-null for viewing others
    onNavigateToAdmin: (() -> Unit)? = null  // Only provided if isAdmin
) {
    // Determine if this is our own profile or someone else's
    val isOwnProfile = currentUser?.id == user.id
    // Can edit if: own profile, or is admin
    val canEdit = isOwnProfile || isAdmin

    var isEditing by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf(user.desc) }
    var features by remember { mutableStateOf(user.featureIds.joinToString(", ")) }
    var itemIds by remember { mutableStateOf(user.itemIds) }
    var imageUrl by remember { mutableStateOf(user.imageUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var imageGenError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Available options for dropdown
    var availableItems by remember { mutableStateOf<List<IdOption>>(emptyList()) }

    // Fetch available items on mount
    LaunchedEffect(Unit) {
        ApiClient.getItems().onSuccess { items ->
            availableItems = items.map { IdOption(it.id, it.name) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header row with back button (if viewing others) and admin icon (if admin on own profile)
        if (onBack != null || (isOwnProfile && onNavigateToAdmin != null)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "User Profile",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                // Admin panel icon for admins on their own profile
                if (isOwnProfile && onNavigateToAdmin != null) {
                    IconButton(onClick = onNavigateToAdmin) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Display image at top
        EntityImage(
            imageUrl = imageUrl,
            contentDescription = "Image of ${user.name}"
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "ID: ${user.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isEditing && canEdit) {
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            IdPillSection(
                label = "Items",
                ids = itemIds,
                entityType = EntityType.ITEM,
                availableOptions = availableItems,
                onPillClick = onNavigateToItem,
                onAddId = { id -> itemIds = itemIds + id },
                onRemoveId = { id -> itemIds = itemIds - id }
            )

            OutlinedTextField(
                value = features,
                onValueChange = { features = it },
                label = { Text("Features (comma-separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Generate Image button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                GenerateImageButton(
                    entityType = GenEntityType.USER,
                    entityId = user.id,
                    name = user.name,
                    description = desc,
                    featureIds = features.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    onImageGenerated = { newImageUrl ->
                        imageUrl = newImageUrl
                    },
                    onError = { imageGenError = it }
                )
            }

            imageGenError?.let {
                SelectionContainer {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isEditing = false
                        desc = user.desc
                        features = user.featureIds.joinToString(", ")
                        itemIds = user.itemIds
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            message = null
                            val request = UpdateUserRequest(
                                desc = desc,
                                itemIds = itemIds,
                                featureIds = features.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                            ApiClient.updateUser(user.id, request).onSuccess { updatedUser ->
                                onUserUpdated(updatedUser)
                                isEditing = false
                                message = "Profile updated successfully!"
                            }.onFailure { error ->
                                message = "Error: ${error.message}"
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        } else {
            if (user.desc.isNotBlank()) {
                Text(
                    text = user.desc,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (user.itemIds.isNotEmpty()) {
                IdPillSection(
                    label = "Items",
                    ids = user.itemIds,
                    entityType = EntityType.ITEM,
                    availableOptions = availableItems,
                    onPillClick = onNavigateToItem,
                    onAddId = {},
                    onRemoveId = {}
                )
            }

            if (user.featureIds.isNotEmpty()) {
                Text(
                    text = "Features: ${user.featureIds.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Only show Edit/Logout buttons if canEdit and it's own profile (or admin viewing)
            if (canEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit Profile")
                    }

                    // Only show logout for own profile
                    if (isOwnProfile) {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }

        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * View for displaying another user's profile (fetched by ID).
 * Read-only for non-admins, editable for admins.
 */
@Composable
fun UserDetailView(
    userId: String,
    currentUser: UserDto?,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onNavigateToItem: (String) -> Unit
) {
    var user by remember { mutableStateOf<UserDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Fetch user on mount
    LaunchedEffect(userId) {
        isLoading = true
        error = null
        ApiClient.getUser(userId).onSuccess { fetchedUser ->
            user = fetchedUser
        }.onFailure { e ->
            error = e.message ?: "Failed to load user"
        }
        isLoading = false
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
        user != null -> {
            UserProfileView(
                user = user!!,
                currentUser = currentUser,
                isAdmin = isAdmin,
                onUserUpdated = { updatedUser ->
                    user = updatedUser
                },
                onLogout = {}, // Not applicable for viewing other users
                onNavigateToItem = onNavigateToItem,
                onBack = onBack
            )
        }
    }
}

@Composable
fun IdPill(
    id: String,
    label: String? = null,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.padding(2.dp),
        shape = RoundedCornerShape(16.dp),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label ?: id.take(8) + if (id.length > 8) "..." else "",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onClick)
            )
            if (onRemove != null) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onRemove),
                    tint = textColor
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdPillSection(
    label: String,
    ids: List<String>,
    entityType: EntityType,
    availableOptions: List<IdOption> = emptyList(),
    onPillClick: (String) -> Unit,
    onAddId: (String) -> Unit,
    onRemoveId: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newId by remember { mutableStateOf("") }

    val pillColor = when (entityType) {
        EntityType.LOCATION -> MaterialTheme.colorScheme.primaryContainer
        EntityType.CREATURE -> MaterialTheme.colorScheme.tertiaryContainer
        EntityType.ITEM -> MaterialTheme.colorScheme.secondaryContainer
    }
    val pillTextColor = when (entityType) {
        EntityType.LOCATION -> MaterialTheme.colorScheme.onPrimaryContainer
        EntityType.CREATURE -> MaterialTheme.colorScheme.onTertiaryContainer
        EntityType.ITEM -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    // Create a map of id -> name for quick lookup
    val idToNameMap = remember(availableOptions) { availableOptions.associate { it.id to it.name } }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ids.forEach { id ->
                IdPill(
                    id = id,
                    label = idToNameMap[id],
                    color = pillColor,
                    textColor = pillTextColor,
                    onClick = { onPillClick(id) },
                    onRemove = if (enabled) {{ onRemoveId(id) }} else null
                )
            }

            // Add button pill (only show when enabled)
            if (enabled) {
                Surface(
                    modifier = Modifier
                        .clickable { showAddDialog = true }
                        .padding(2.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        // Filter out already selected IDs from available options
        val unselectedOptions = availableOptions.filter { it.id !in ids }

        AlertDialog(
            onDismissRequest = { showAddDialog = false; newId = "" },
            title = { Text("Add ${entityType.name.lowercase().replaceFirstChar { it.uppercase() }}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Show list of available options if there are any
                    if (unselectedOptions.isNotEmpty()) {
                        Text(
                            text = "Select from existing:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(unselectedOptions) { option ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddId(option.id)
                                            showAddDialog = false
                                            newId = ""
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = option.name.ifBlank { "(No name)" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = option.id,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Free-form text field for manual entry
                    Text(
                        text = "Or enter ID manually:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it },
                        label = { Text("ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newId.isNotBlank()) {
                            onAddId(newId.trim())
                            newId = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newId.isNotBlank()
                ) {
                    Text("Add Manual ID")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newId = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper function to get a short label for direction
fun ExitDirection.toShortLabel(): String = when (this) {
    ExitDirection.NORTH -> "N"
    ExitDirection.NORTHEAST -> "NE"
    ExitDirection.EAST -> "E"
    ExitDirection.SOUTHEAST -> "SE"
    ExitDirection.SOUTH -> "S"
    ExitDirection.SOUTHWEST -> "SW"
    ExitDirection.WEST -> "W"
    ExitDirection.NORTHWEST -> "NW"
    ExitDirection.UNKNOWN -> "?"
}

fun ExitDirection.toDisplayLabel(): String = when (this) {
    ExitDirection.NORTH -> "North"
    ExitDirection.NORTHEAST -> "Northeast"
    ExitDirection.EAST -> "East"
    ExitDirection.SOUTHEAST -> "Southeast"
    ExitDirection.SOUTH -> "South"
    ExitDirection.SOUTHWEST -> "Southwest"
    ExitDirection.WEST -> "West"
    ExitDirection.NORTHWEST -> "Northwest"
    ExitDirection.UNKNOWN -> "Unknown"
}

fun getOppositeDirection(direction: ExitDirection): ExitDirection = when (direction) {
    ExitDirection.NORTH -> ExitDirection.SOUTH
    ExitDirection.NORTHEAST -> ExitDirection.SOUTHWEST
    ExitDirection.EAST -> ExitDirection.WEST
    ExitDirection.SOUTHEAST -> ExitDirection.NORTHWEST
    ExitDirection.SOUTH -> ExitDirection.NORTH
    ExitDirection.SOUTHWEST -> ExitDirection.NORTHEAST
    ExitDirection.WEST -> ExitDirection.EAST
    ExitDirection.NORTHWEST -> ExitDirection.SOUTHEAST
    ExitDirection.UNKNOWN -> ExitDirection.UNKNOWN
}

@Composable
fun ExitPill(
    exit: ExitDto,
    locationName: String?,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.padding(2.dp),
        shape = RoundedCornerShape(16.dp),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Direction badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = textColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = exit.direction.toShortLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Text(
                text = locationName ?: exit.locationId.take(8) + if (exit.locationId.length > 8) "..." else "",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onClick)
            )
            if (onEdit != null) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onEdit),
                    tint = textColor
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExitPillSection(
    label: String,
    exits: List<ExitDto>,
    availableOptions: List<IdOption> = emptyList(),
    onPillClick: (String) -> Unit,
    onAddExit: (ExitDto) -> Unit,
    onUpdateExit: (oldExit: ExitDto, newExit: ExitDto) -> Unit,
    onRemoveExit: (ExitDto) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    currentLocationId: String? = null,  // The source location for coordinate validation
    allLocations: List<LocationDto> = emptyList()  // All locations for coordinate lookup
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    var selectedDirection by remember { mutableStateOf(ExitDirection.UNKNOWN) }
    var directionDropdownExpanded by remember { mutableStateOf(false) }

    // Validation state
    var validationResult by remember { mutableStateOf<ValidateExitResponse?>(null) }
    var isValidating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // State for edit dialog
    var exitToEdit by remember { mutableStateOf<ExitDto?>(null) }
    var editDirection by remember { mutableStateOf(ExitDirection.UNKNOWN) }
    var editDirectionDropdownExpanded by remember { mutableStateOf(false) }

    // Get current location's coordinates for display
    val currentLocation = allLocations.find { it.id == currentLocationId }

    val pillColor = MaterialTheme.colorScheme.primaryContainer
    val pillTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    // Create a map of id -> name for quick lookup
    val idToNameMap = remember(availableOptions) { availableOptions.associate { it.id to it.name } }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exits.forEach { exit ->
                ExitPill(
                    exit = exit,
                    locationName = idToNameMap[exit.locationId],
                    color = pillColor,
                    textColor = pillTextColor,
                    onClick = { onPillClick(exit.locationId) },
                    onEdit = if (enabled) {{
                        exitToEdit = exit
                        editDirection = exit.direction
                    }} else null
                )
            }

            // Add button pill (only show when enabled)
            if (enabled) {
                Surface(
                    modifier = Modifier
                        .clickable { showAddDialog = true }
                        .padding(2.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        // Filter out already selected locations from available options
        val existingLocationIds = exits.map { it.locationId }.toSet()

        // Filter locations: exclude non-adjacent locations that have coordinates
        // A location with coordinates is only valid if it's exactly 1 cell away from current location
        val unselectedOptions = availableOptions.filter { option ->
            if (option.id in existingLocationIds) return@filter false

            val targetLoc = allLocations.find { it.id == option.id }

            // If target has no coordinates, it's a floating location - allow it
            if (targetLoc?.gridX == null) return@filter true

            // If source has no coordinates, allow all targets
            if (currentLocation?.gridX == null) return@filter true

            // Both have coordinates - check if adjacent (exactly 1 cell away)
            val dx = kotlin.math.abs(targetLoc.gridX!! - currentLocation.gridX!!)
            val dy = kotlin.math.abs((targetLoc.gridY ?: 0) - (currentLocation.gridY ?: 0))
            val dz = kotlin.math.abs((targetLoc.gridZ ?: 0) - (currentLocation.gridZ ?: 0))

            // Adjacent means max 1 step in any direction (including diagonals), same Z level, not same cell
            dz == 0 && dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0)
        }

        // Filter out already used directions (max 1 exit per direction)
        val usedDirections = exits.map { it.direction }.toSet()

        // When a location is selected, validate exit and get available directions
        LaunchedEffect(selectedLocationId, currentLocationId) {
            if (selectedLocationId != null && currentLocationId != null) {
                isValidating = true
                try {
                    ApiClient.validateExit(currentLocationId, selectedLocationId!!)
                        .onSuccess { result ->
                            validationResult = result
                            // Auto-select direction if fixed
                            if (result.validDirections.isNotEmpty()) {
                                val firstValid = result.validDirections.first()
                                selectedDirection = firstValid.direction
                            }
                        }
                        .onFailure {
                            validationResult = null
                        }
                } finally {
                    isValidating = false
                }
            } else {
                validationResult = null
            }
        }

        // Compute available directions based on validation result
        val availableDirections = if (validationResult != null && validationResult!!.canCreateExit) {
            validationResult!!.validDirections.map { it.direction }.filter { it !in usedDirections }
        } else if (currentLocationId == null || currentLocation?.gridX == null) {
            // Source doesn't have coordinates - use old behavior
            ExitDirection.entries.filter { it !in usedDirections }
        } else {
            emptyList()
        }

        // Check if selected direction is fixed (target already has coordinates)
        val isDirectionFixed = validationResult?.validDirections?.firstOrNull()?.isFixed == true

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                selectedLocationId = null
                selectedDirection = ExitDirection.UNKNOWN
                validationResult = null
            },
            title = { Text("Add Exit") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Show current location coordinates if available
                    if (currentLocation?.gridX != null) {
                        Text(
                            text = "From: (${currentLocation.gridX}, ${currentLocation.gridY}, ${currentLocation.gridZ ?: 0})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Step 1: Show list of available locations FIRST
                    Text(
                        text = "1. Select destination:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (unselectedOptions.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(unselectedOptions) { option ->
                                val isSelected = selectedLocationId == option.id
                                val targetLoc = allLocations.find { it.id == option.id }
                                val hasCoords = targetLoc?.gridX != null

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLocationId = option.id
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = option.name.ifBlank { "(No name)" },
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (hasCoords && targetLoc != null) {
                                                Text(
                                                    text = "(${targetLoc.gridX}, ${targetLoc.gridY}, ${targetLoc.gridZ ?: 0})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            } else {
                                                Text(
                                                    text = "Floating (no coords)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No more locations available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Step 2: Direction selection (only after location is selected)
                    Text(
                        text = "2. Direction:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedLocationId == null) {
                        Text(
                            text = "Select a destination first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else if (isValidating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Validating...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (validationResult != null && !validationResult!!.canCreateExit) {
                        Text(
                            text = validationResult!!.errorMessage ?: "Cannot create exit to this location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (availableDirections.isEmpty()) {
                        Text(
                            text = "No valid directions available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (isDirectionFixed) {
                        // Direction is fixed - show as read-only
                        val targetCoords = validationResult?.validDirections?.firstOrNull()?.targetCoordinates
                        OutlinedTextField(
                            value = selectedDirection.toDisplayLabel(),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Direction (fixed by coordinates)") },
                            supportingText = if (targetCoords != null) {
                                { Text("Target at (${targetCoords.x}, ${targetCoords.y}, ${targetCoords.z})") }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Direction is selectable
                        ExposedDropdownMenuBox(
                            expanded = directionDropdownExpanded,
                            onExpandedChange = { directionDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = if (selectedDirection in availableDirections) selectedDirection.toDisplayLabel() else availableDirections.first().toDisplayLabel(),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = directionDropdownExpanded,
                                onDismissRequest = { directionDropdownExpanded = false }
                            ) {
                                availableDirections.forEach { direction ->
                                    val coordInfo = validationResult?.validDirections?.find { it.direction == direction }?.targetCoordinates
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(direction.toDisplayLabel())
                                                if (coordInfo != null) {
                                                    Text(
                                                        text = "-> (${coordInfo.x}, ${coordInfo.y}, ${coordInfo.z})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedDirection = direction
                                            directionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Show selected summary
                    if (selectedLocationId != null && availableDirections.isNotEmpty() && !isValidating) {
                        val selectedName = availableOptions.find { it.id == selectedLocationId }?.name ?: selectedLocationId
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        val dirToShow = if (selectedDirection in availableDirections) selectedDirection else availableDirections.first()
                        Text(
                            text = "Exit: ${dirToShow.toDisplayLabel()} -> $selectedName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedLocationId != null && availableDirections.isNotEmpty()) {
                            val directionToUse = if (selectedDirection in availableDirections) selectedDirection else availableDirections.first()
                            onAddExit(ExitDto(locationId = selectedLocationId!!, direction = directionToUse))
                            showAddDialog = false
                            selectedLocationId = null
                            selectedDirection = ExitDirection.UNKNOWN
                            validationResult = null
                        }
                    },
                    enabled = selectedLocationId != null && availableDirections.isNotEmpty() && !isValidating
                ) {
                    Text("Save Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    selectedLocationId = null
                    selectedDirection = ExitDirection.UNKNOWN
                    validationResult = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit exit dialog - also enforce one exit per direction
    if (exitToEdit != null) {
        val currentExit = exitToEdit!!
        val locationName = idToNameMap[currentExit.locationId] ?: currentExit.locationId.take(8)
        // Available directions for edit: unused directions + current exit's direction
        val usedDirectionsForEdit = exits.filter { it.locationId != currentExit.locationId }.map { it.direction }.toSet()
        val availableDirectionsForEdit = ExitDirection.entries.filter { it !in usedDirectionsForEdit }

        AlertDialog(
            onDismissRequest = {
                exitToEdit = null
                editDirection = ExitDirection.UNKNOWN
            },
            title = { Text("Edit Exit") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Show destination (read-only)
                    Text(
                        text = "Destination:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Direction dropdown - only show unused directions
                    Text(
                        text = "Direction:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = editDirectionDropdownExpanded,
                        onExpandedChange = { editDirectionDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = editDirection.toDisplayLabel(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editDirectionDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = editDirectionDropdownExpanded,
                            onDismissRequest = { editDirectionDropdownExpanded = false }
                        ) {
                            availableDirectionsForEdit.forEach { direction ->
                                DropdownMenuItem(
                                    text = { Text(direction.toDisplayLabel()) },
                                    onClick = {
                                        editDirection = direction
                                        editDirectionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newExit = currentExit.copy(direction = editDirection)
                        onUpdateExit(currentExit, newExit)
                        exitToEdit = null
                        editDirection = ExitDirection.UNKNOWN
                    },
                    enabled = editDirection in availableDirectionsForEdit
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onRemoveExit(currentExit)
                            exitToEdit = null
                            editDirection = ExitDirection.UNKNOWN
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                    TextButton(onClick = {
                        exitToEdit = null
                        editDirection = ExitDirection.UNKNOWN
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun LocationGraphView(
    refreshKey: Long,
    onAddClick: () -> Unit,
    onLocationClick: (LocationDto) -> Unit,
    isAuthenticated: Boolean,
    isAdmin: Boolean = false,
    currentUser: UserDto? = null,
    onLoginClick: () -> Unit = {}
) {
    var locations by remember(refreshKey) { mutableStateOf<List<LocationDto>>(emptyList()) }
    var isLoading by remember(refreshKey) { mutableStateOf(true) }
    var error by remember(refreshKey) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Terrain override state
    var terrainOverridesMap by remember(refreshKey) { mutableStateOf<Map<String, TerrainOverridesDto>>(emptyMap()) }
    var selectedLocationForSettings by remember { mutableStateOf<LocationDto?>(null) }

    // Re-fetch locations when refreshKey changes (e.g., after saving a location)
    // Use refreshKey as cache buster to bypass browser/CDN caching
    LaunchedEffect(refreshKey) {
        println("DEBUG: LocationGraphView LaunchedEffect triggered with refreshKey: $refreshKey")
        isLoading = true
        val result = ApiClient.getLocations(cacheBuster = refreshKey)
        isLoading = false
        result.onSuccess {
            println("DEBUG: Loaded ${it.size} locations")
            locations = it
        }.onFailure { error = it.message }
    }

    // Fetch terrain overrides for all locations (lazy-loaded when locations change)
    LaunchedEffect(locations) {
        if (isAdmin && locations.isNotEmpty()) {
            locations.forEach { location ->
                scope.launch {
                    ApiClient.getTerrainOverrides(location.id)
                        .onSuccess { override ->
                            terrainOverridesMap = terrainOverridesMap + (location.id to override.overrides)
                        }
                }
            }
        }
    }

    // Terrain settings dialog
    selectedLocationForSettings?.let { location ->
        TerrainSettingsDialog(
            location = location,
            currentOverrides = terrainOverridesMap[location.id],
            onDismiss = { selectedLocationForSettings = null },
            onSave = { overrides ->
                scope.launch {
                    ApiClient.updateTerrainOverrides(location.id, overrides)
                        .onSuccess {
                            terrainOverridesMap = terrainOverridesMap + (location.id to overrides)
                            selectedLocationForSettings = null
                        }
                }
            },
            onReset = {
                scope.launch {
                    ApiClient.resetTerrainOverrides(location.id)
                        .onSuccess {
                            terrainOverridesMap = terrainOverridesMap - location.id
                            selectedLocationForSettings = null
                        }
                }
            },
            currentUser = currentUser
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SelectionContainer {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            error = null
                            val result = ApiClient.getLocations()
                            isLoading = false
                            result.onSuccess { locations = it }
                                .onFailure { error = it.message }
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
            locations.isEmpty() -> {
                Text(
                    text = "No locations yet. Tap + to create one.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LocationGraph(
                    locations = locations,
                    onLocationClick = onLocationClick,
                    modifier = Modifier.fillMaxSize(),
                    isAdmin = isAdmin,
                    terrainOverridesMap = terrainOverridesMap,
                    onSettingsClick = { location ->
                        selectedLocationForSettings = location
                    }
                )
            }
        }

        if (isAuthenticated) {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Location")
            }
        } else {
            // "Login to Create" message for unauthenticated users - clickable to navigate to auth
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onLoginClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Login to Create",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CreatureListView(
    onCreatureClick: (CreatureDto) -> Unit,
    onAddClick: () -> Unit,
    isAuthenticated: Boolean
) {
    var creatures by remember { mutableStateOf<List<CreatureDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = ApiClient.getCreatures()
            isLoading = false
            result.onSuccess { creatures = it.sortedBy { c -> c.name.lowercase() } }
                .onFailure { error = it.message }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SelectionContainer {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            error = null
                            val result = ApiClient.getCreatures()
                            isLoading = false
                            result.onSuccess { creatures = it.sortedBy { c -> c.name.lowercase() } }
                                .onFailure { error = it.message }
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
            creatures.isEmpty() -> {
                Text(
                    text = "No creatures yet. Tap + to create one.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(creatures) { creature ->
                        ListItem(
                            headlineContent = { Text(creature.name.ifBlank { creature.id.take(8) }) },
                            supportingContent = if (creature.desc.isNotBlank()) {
                                { Text(creature.desc.take(50) + if (creature.desc.length > 50) "..." else "") }
                            } else null,
                            leadingContent = if (!creature.imageUrl.isNullOrBlank()) {
                                {
                                    AsyncImage(
                                        model = "${AppConfig.api.baseUrl}${creature.imageUrl}",
                                        contentDescription = creature.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else null,
                            modifier = Modifier.clickable { onCreatureClick(creature) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (isAuthenticated) {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Creature")
            }
        }
    }
}

@Composable
fun ItemListView(
    onItemClick: (ItemDto) -> Unit,
    onAddClick: () -> Unit,
    isAuthenticated: Boolean
) {
    var items by remember { mutableStateOf<List<ItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = ApiClient.getItems()
            isLoading = false
            result.onSuccess { items = it.sortedBy { i -> i.name.lowercase() } }
                .onFailure { error = it.message }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SelectionContainer {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            error = null
                            val result = ApiClient.getItems()
                            isLoading = false
                            result.onSuccess { items = it.sortedBy { i -> i.name.lowercase() } }
                                .onFailure { error = it.message }
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
            items.isEmpty() -> {
                Text(
                    text = "No items yet. Tap + to create one.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items) { item ->
                        ListItem(
                            headlineContent = { Text(item.name.ifBlank { item.id.take(8) }) },
                            supportingContent = if (item.desc.isNotBlank()) {
                                { Text(item.desc.take(50) + if (item.desc.length > 50) "..." else "") }
                            } else null,
                            leadingContent = if (!item.imageUrl.isNullOrBlank()) {
                                {
                                    AsyncImage(
                                        model = "${AppConfig.api.baseUrl}${item.imageUrl}",
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else null,
                            modifier = Modifier.clickable { onItemClick(item) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (isAuthenticated) {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Item")
            }
        }
    }
}

data class LocationPosition(val location: LocationDto, val x: Float, val y: Float)

// Result from grid position calculation including raw grid coordinates
data class GridPositionResult(
    val locationPositions: Map<String, LocationPosition>,
    val gridPositions: Map<String, Pair<Int, Int>>,
    val gridBounds: GridBounds
)

data class GridBounds(
    val minX: Int, val maxX: Int,
    val minY: Int, val maxY: Int,
    val padding: Float = 0.15f
)

@Composable
fun LocationGraph(
    locations: List<LocationDto>,
    onLocationClick: (LocationDto) -> Unit,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    terrainOverridesMap: Map<String, TerrainOverridesDto> = emptyMap(),
    onSettingsClick: (LocationDto) -> Unit = {}
) {
    val gridResult = remember(locations) {
        calculateForceDirectedPositions(locations)
    }
    val locationPositions = gridResult.locationPositions

    // Track which location is expanded (null = none expanded)
    var expandedLocationId by remember { mutableStateOf<String?>(null) }

    // Pan offset state with animation support
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    // Zoom scale state (1.0 = 100%, min 0.5 = 50%, max 3.0 = 300%)
    var scale by remember { mutableStateOf(1f) }
    val minScale = 0.3f
    val maxScale = 3f
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .pointerInput(expandedLocationId) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // Collapse any expanded thumbnail when panning/zooming
                    if (expandedLocationId != null) {
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
            .pointerInput(expandedLocationId) {
                detectTapGestures { _ ->
                    // Collapse expanded thumbnail when tapping on empty space
                    if (expandedLocationId != null) {
                        expandedLocationId = null
                    }
                }
            }
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

        // Function to center on a location
        fun centerOnLocation(location: LocationDto) {
            val pos = locationPositions[location.id] ?: return
            val screenPos = getLocationScreenPos(pos)
            val centerX = width / 2
            val centerY = height / 2
            val targetOffset = Offset(centerX - screenPos.x, centerY - screenPos.y)

            scope.launch {
                offset.animateTo(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 300)
                )
            }
        }

        // Terrain size extends beyond the thumbnail
        val terrainSize = boxSizePx * 2.0f

        // LAYER 1: Parchment background (fixed, doesn't pan)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawParchmentBackground(seed = locations.size)
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

            // LAYER 3: Location dots/thumbnails
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
                    onSettingsClick = { onSettingsClick(location) }
                )
            }

            // Labels are only shown when location is expanded (tap to reveal)
        }

        // Zoom controls overlay (top-right corner)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Zoom in button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
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
                Text("+", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            // Zoom out button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
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
                Text("-", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            // Reset zoom/pan button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
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

        // Zoom level indicator (bottom-right corner)
        Text(
            text = "${(scale * 100).toInt()}%",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// Dark crimson color for dot borders
private val DotBorderColor = Color(0xFF8B1A1A)

/**
 * Thumbnail node for a location in the graph view.
 * Collapsed: 20dp circle with color based on terrain, with name label below
 * Expanded: 100dp box with image/name, centered on where the dot was
 */
@Composable
private fun LocationNodeThumbnail(
    location: LocationDto,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val collapsedSize = 20.dp
    val expandedSize = 100.dp

    val hasImage = location.imageUrl != null

    // Get terrain-based color for collapsed state
    val terrainColor = remember(location) {
        getTerrainColor(location.desc, location.name)
    }

    if (isExpanded) {
        // Expanded state: 100x100 thumbnail with action icons to the right
        // The modifier positions us at the dot's top-left corner
        // Dot center is at (10dp, 10dp) from that position
        // We want the 100x100 box center (50dp, 50dp) to align with dot center
        // So offset by: 10 - 50 = -40dp in both directions
        Row(
            modifier = modifier
                .offset(x = (-40).dp, y = (-40).dp)
                .wrapContentSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail box
            Box(
                modifier = Modifier
                    .size(expandedSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasImage) {
                    val fullUrl = "${AppConfig.api.baseUrl}${location.imageUrl}"
                    var imageState by remember {
                        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
                    }

                    val isLoaded = imageState is AsyncImagePainter.State.Success
                    val imageAlpha by animateFloatAsState(
                        targetValue = if (isLoaded) 1f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label = "imageAlpha"
                    )

                    // Semi-opaque placeholder while loading
                    if (!isLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(terrainColor.copy(alpha = 0.5f))
                        )
                    }

                    AsyncImage(
                        model = fullUrl,
                        contentDescription = location.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(imageAlpha),
                        contentScale = ContentScale.Crop,
                        onState = { imageState = it }
                    )

                    // Semi-transparent overlay at bottom for name
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = location.name.ifBlank { location.id.take(8) },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Show loading indicator if still loading
                    if (imageState is AsyncImagePainter.State.Loading || imageState is AsyncImagePainter.State.Empty) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    }

                    // Fall back to colored box on error
                    if (imageState is AsyncImagePainter.State.Error) {
                        FallbackLocationBox(location)
                    }
                } else {
                    FallbackLocationBox(location)
                }
            }

            // Action icons column - always visible for now to debug
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                    // Edit/Detail icon (pencil on top)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .clickable(onClick = onClick)
                            .padding(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Location",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    }

                    // Settings/Terrain icon (gear below)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .clickable(onClick = onSettingsClick)
                            .padding(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Terrain Settings",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    }
                }
        }
    } else {
        // Collapsed state: just the dot (label is rendered separately in parent)
        // Locations never edited by a user (lastEditedAt == null) are smaller
        val isUnedited = location.lastEditedAt == null
        val editedSize = 14.dp  // Edited locations: 14dp
        val wildernessSize = 10.dp  // Wilderness: 10dp
        val dotSize = if (isUnedited) wildernessSize else editedSize

        // Center dots within the space where a full-size dot would be
        val centeringOffset = (collapsedSize - dotSize) / 2

        // Light tan color - 15% alpha for both
        val tanColor = Color(0xFFD4B896)
        val fillAlpha = 0.15f

        Box(
            modifier = modifier
                .offset(x = centeringOffset, y = centeringOffset)
                .size(dotSize)
                .clickable(onClick = onClick)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Draw simple tan fill
                drawCircle(
                    color = tanColor.copy(alpha = fillAlpha),
                    radius = radius,
                    center = center
                )

                // Draw red border only for edited locations (non-wilderness)
                if (!isUnedited) {
                    drawCircle(
                        color = DotBorderColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Get a color based on the location's terrain type.
 */
private fun getTerrainColor(desc: String, name: String): Color {
    val text = (desc + " " + name).lowercase()

    return when {
        text.containsAny("castle", "fortress", "citadel", "stronghold") -> Color(0xFF8B7355) // Brown
        text.containsAny("church", "temple", "cathedral", "shrine") -> Color(0xFFE8E4DC) // Light gray
        text.containsAny("forest", "tree", "wood", "grove") -> Color(0xFF4A6741) // Forest green
        text.containsAny("mountain", "peak", "summit") -> Color(0xFF7D7461) // Gray-brown
        text.containsAny("water", "river", "lake", "coast", "sea", "ocean") -> Color(0xFF6B8E9F) // Blue-gray
        text.containsAny("swamp", "marsh", "bog") -> Color(0xFF5A6B52) // Murky green
        text.containsAny("desert", "sand", "dune") -> Color(0xFFD4C19E) // Sandy
        text.containsAny("cave", "cavern", "dungeon") -> Color(0xFF5A5A5A) // Dark gray
        text.containsAny("town", "village", "inn", "tavern", "shop") -> Color(0xFFB8976B) // Tan
        text.containsAny("port", "dock", "harbor") -> Color(0xFF8BA4B0) // Coastal blue
        text.containsAny("ruin", "ancient", "abandon") -> Color(0xFF8A7B6A) // Aged stone
        text.containsAny("grass", "meadow", "field", "plain") -> Color(0xFF7A9A6D) // Muted green
        else -> Color(0xFFC4A67C) // Default parchment tan
    }
}

@Composable
private fun FallbackLocationBox(location: LocationDto) {
    // Mimic the look of a real location image: terrain background, mystery icon, name at bottom
    val terrainColor = remember(location) { getTerrainColor(location.desc, location.name) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(terrainColor.copy(alpha = 0.7f))
    ) {
        // Draw a simple hill silhouette with question mark
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw rolling hills silhouette at the bottom third
            val hillColor = Color(0xFF2A3A2A).copy(alpha = 0.6f)
            val hillPath = Path().apply {
                moveTo(0f, height * 0.75f)
                // First hill
                quadraticTo(width * 0.15f, height * 0.55f, width * 0.3f, height * 0.7f)
                // Second hill (taller)
                quadraticTo(width * 0.5f, height * 0.45f, width * 0.7f, height * 0.65f)
                // Third hill
                quadraticTo(width * 0.85f, height * 0.5f, width, height * 0.7f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(hillPath, color = hillColor)

            // Draw question mark silhouette in the center-upper area
            val questionMarkColor = Color.White.copy(alpha = 0.4f)
            val qmCenterX = width * 0.5f
            val qmCenterY = height * 0.35f
            val qmSize = minOf(width, height) * 0.25f

            // Question mark curve (top part)
            val qmPath = Path().apply {
                // Arc part of question mark
                moveTo(qmCenterX - qmSize * 0.3f, qmCenterY - qmSize * 0.3f)
                quadraticTo(qmCenterX - qmSize * 0.3f, qmCenterY - qmSize * 0.6f, qmCenterX, qmCenterY - qmSize * 0.6f)
                quadraticTo(qmCenterX + qmSize * 0.4f, qmCenterY - qmSize * 0.6f, qmCenterX + qmSize * 0.4f, qmCenterY - qmSize * 0.2f)
                quadraticTo(qmCenterX + qmSize * 0.4f, qmCenterY + qmSize * 0.1f, qmCenterX, qmCenterY + qmSize * 0.2f)
            }
            drawPath(qmPath, color = questionMarkColor, style = Stroke(width = qmSize * 0.15f, cap = StrokeCap.Round))

            // Question mark dot
            drawCircle(
                color = questionMarkColor,
                radius = qmSize * 0.1f,
                center = Offset(qmCenterX, qmCenterY + qmSize * 0.45f)
            )
        }

        // Name label at bottom with black background (like real location images)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(4.dp)
        ) {
            Text(
                text = location.name.ifBlank { location.id.take(8) },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Force-directed layout constants
private object LayoutConstants {
    const val REPULSION_STRENGTH = 0.02f
    const val ATTRACTION_STRENGTH = 0.03f  // Increased for tighter clustering
    const val CENTER_PULL = 0.005f
    const val DAMPING = 0.9f
    const val MIN_DISTANCE = 0.15f
    const val ITERATIONS = 100
}

// Parchment background colors
private object ParchmentColors {
    val base = Color(0xFFF5E6C8)          // Main parchment color
    val darkSpot = Color(0xFFD4C4A8)      // Darker aging spots
    val lightSpot = Color(0xFFFFF8E7)     // Lighter worn areas
    val stain = Color(0xFFE8D5B5)         // Tea stain color
    val edge = Color(0xFFCBB896)          // Slightly darker edges
}

// Terrain drawing colors
private object TerrainColors {
    val road = Color(0xFFB8976B)           // Tan/brown road
    val roadOutline = Color(0xFF8B7355)    // Darker road edge
    val tree = Color(0xFF4A6741)           // Forest green
    val treeDark = Color(0xFF3A5731)       // Darker foliage shadow
    val treeTrunk = Color(0xFF5A4030)      // Brown trunk
    val water = Color(0xFF6B8E9F)          // Muted blue-gray
    val waterHighlight = Color(0xFF8BB0C4) // Lighter water
    val mountain = Color(0xFF7D7461)       // Gray-brown
    val mountainSnow = Color(0xFFE8E4DC)   // Snow cap
    val grass = Color(0xFF7A9A6D)          // Muted green
    val building = Color(0xFF8B7355)       // Brown buildings
    val cave = Color(0xFF5A5A5A)           // Dark gray
    val sand = Color(0xFFD4C19E)           // Sandy color
    val coast = Color(0xFF8BA4B0)          // Coastal blue-gray
    val swamp = Color(0xFF5A6B52)          // Murky green
    val ruins = Color(0xFF8A7B6A)          // Aged stone
    val ink = Color(0xFF3A3022)            // Dark ink for details
}

// Terrain types for contextual drawing
private enum class TerrainType {
    ROAD, FOREST, WATER, STREAM, RIVER, LAKE, MOUNTAIN, GRASS, BUILDING, CAVE, DESERT,
    COAST, HILLS, SWAMP, CHURCH, CASTLE, PORT, RUINS
}

// Voronoi-based biome blending for terrain variation
private object BiomeBlending {
    /**
     * Get biome blend information for a position within a terrain tile.
     * Uses Voronoi cells to create natural-looking biome transitions.
     *
     * @param localX X position within tile (0 to terrainSize)
     * @param localY Y position within tile (0 to terrainSize)
     * @param terrainSize Size of the terrain tile
     * @param seed Seed for reproducible results
     * @param cellScale Scale of Voronoi cells relative to terrain
     * @return BiomeBlendResult with blend factors
     */
    data class BiomeBlendResult(
        val primaryWeight: Float,      // Weight of primary biome (0-1)
        val secondaryWeight: Float,    // Weight of secondary biome (0-1)
        val edgeDistance: Float,       // Distance to cell edge (for transition effects)
        val cellId: Int,               // ID of the current cell (for consistent variation)
        val distanceToCenter: Float    // Distance from cell center (for radial effects)
    )

    fun getBlendInfo(
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int,
        cellScale: Float = 0.5f  // Larger cells for more visible effect
    ): BiomeBlendResult {
        val scale = terrainSize * cellScale
        val voronoi = VoronoiNoise.cellular(localX, localY, scale, 0.8f, seed)
        val (w1, w2) = voronoi.cellWeights(0.5f)  // Wider blend zone

        return BiomeBlendResult(
            primaryWeight = w1,
            secondaryWeight = w2,
            edgeDistance = voronoi.edgeDistance,
            cellId = voronoi.cellId1,
            distanceToCenter = voronoi.distance1
        )
    }

    /**
     * Get color variation based on Voronoi cell position.
     * Creates natural-looking color variation within terrain.
     */
    fun getColorVariation(
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int,
        variationAmount: Float = 0.15f
    ): Float {
        val blend = getBlendInfo(localX, localY, terrainSize, seed, 0.3f)
        // Use cell ID to create consistent variation per cell
        val cellVariation = ((blend.cellId and 0xFF) / 255f) * 2f - 1f
        // Blend with edge proximity for smooth transitions
        val edgeFactor = (blend.edgeDistance / (terrainSize * 0.1f)).coerceIn(0f, 1f)
        return cellVariation * variationAmount * (1f - edgeFactor * 0.5f)
    }

    /**
     * Get density multiplier for features (trees, grass, etc.) based on Voronoi cells.
     * Creates natural clustering patterns.
     */
    fun getDensityMultiplier(
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int
    ): Float {
        val blend = getBlendInfo(localX, localY, terrainSize, seed + 1000, 0.4f)
        // Higher density near cell centers, lower near edges
        val centerFactor = 1f - (blend.distanceToCenter / (terrainSize * 0.2f)).coerceIn(0f, 0.5f)
        // Cell-based variation
        val cellBonus = ((blend.cellId and 0x7F) / 127f) * 0.3f
        return (0.7f + centerFactor * 0.3f + cellBonus).coerceIn(0.5f, 1.3f)
    }

    /**
     * Interpolate between two colors based on position using Voronoi blending.
     */
    fun blendColors(
        color1: Color,
        color2: Color,
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int
    ): Color {
        val blend = getBlendInfo(localX, localY, terrainSize, seed, 0.5f)
        // Use cell ID to determine which color dominates in each cell
        val cellPreference = ((blend.cellId and 0xFF) / 255f)
        // Stronger blending - cells clearly show different colors
        val factor = if (cellPreference > 0.5f) {
            (0.6f + blend.secondaryWeight * 0.4f).coerceIn(0f, 1f)
        } else {
            (blend.secondaryWeight * 0.4f).coerceIn(0f, 1f)
        }

        return Color(
            red = color1.red * (1f - factor) + color2.red * factor,
            green = color1.green * (1f - factor) + color2.green * factor,
            blue = color1.blue * (1f - factor) + color2.blue * factor,
            alpha = color1.alpha * (1f - factor) + color2.alpha * factor
        )
    }

    /**
     * Get biome transition effect - useful for drawing edge features.
     * Returns a value from 0 (cell center) to 1 (cell edge).
     */
    fun getEdgeProximity(
        localX: Float,
        localY: Float,
        terrainSize: Float,
        seed: Int
    ): Float {
        val blend = getBlendInfo(localX, localY, terrainSize, seed, 0.3f)
        return (1f - blend.primaryWeight).coerceIn(0f, 1f)
    }
}

// Helper extension for keyword matching
private fun String.containsAny(vararg keywords: String): Boolean {
    return keywords.any { this.contains(it) }
}

// Parse terrain types from location description
// Uses smarter detection to avoid false positives (e.g., "near Lake Rainier" shouldn't make it a lake)
private fun parseTerrainFromDescription(desc: String, name: String): Set<TerrainType> {
    val text = (desc + " " + name).lowercase()
    val nameLower = name.lowercase()
    val terrains = mutableSetOf<TerrainType>()

    if (text.containsAny("road", "path", "trail", "highway", "street", "lane", "way")) {
        terrains.add(TerrainType.ROAD)
    }
    if (text.containsAny("forest", "tree", "wood", "grove", "copse", "timber", "oak", "pine", "jungle")) {
        terrains.add(TerrainType.FOREST)
    }
    if (text.containsAny("stream", "creek", "brook")) {
        terrains.add(TerrainType.STREAM)
    }
    if (text.containsAny("river")) {
        terrains.add(TerrainType.RIVER)
    }
    // Lake detection is more careful:
    // - If name contains "lake" or "pond", it's definitely a lake
    // - If description has patterns like "the lake", "a lake", "this lake" (not "Lake SomeName"), it's a lake
    // - Avoid false positives from references like "near Lake Rainier" or "feeds into Lake X"
    val isLakeByName = nameLower.containsAny("lake", "pond")
    val hasLakeDescription = text.contains(Regex("\\b(the|a|this|in the|on the|of the|into the|across the) (lake|pond)\\b"))
    val isOnLake = text.containsAny("on the lake", "in the lake", "across the lake", "middle of the lake")
    if (isLakeByName || hasLakeDescription || isOnLake) {
        terrains.add(TerrainType.LAKE)
    }
    if (text.containsAny("water", "falls", "fountain")) {
        terrains.add(TerrainType.WATER)
    }
    if (text.containsAny("mountain", "peak", "summit", "alpine")) {
        terrains.add(TerrainType.MOUNTAIN)
    }
    if (text.containsAny("hill", "cliff", "ridge", "highland", "slope", "knoll", "mound")) {
        terrains.add(TerrainType.HILLS)
    }
    if (text.containsAny("grass", "meadow", "field", "plain", "pasture", "clearing", "prairie")) {
        terrains.add(TerrainType.GRASS)
    }
    if (text.containsAny("town", "village", "inn", "tavern", "house", "building", "shop", "market", "hamlet")) {
        terrains.add(TerrainType.BUILDING)
    }
    if (text.containsAny("castle", "fortress", "citadel", "stronghold", "keep", "palace")) {
        terrains.add(TerrainType.CASTLE)
    }
    if (text.containsAny("church", "temple", "cathedral", "shrine", "chapel", "monastery", "abbey")) {
        terrains.add(TerrainType.CHURCH)
    }
    if (text.containsAny("cave", "cavern", "underground", "tunnel", "grotto", "mine", "dungeon")) {
        terrains.add(TerrainType.CAVE)
    }
    if (text.containsAny("desert", "sand", "dune", "arid", "wasteland", "barren")) {
        terrains.add(TerrainType.DESERT)
    }
    if (text.containsAny("coast", "shore", "beach", "sea", "ocean", "bay", "harbor", "cove")) {
        terrains.add(TerrainType.COAST)
    }
    if (text.containsAny("swamp", "marsh", "bog", "wetland", "fen", "mire", "bayou")) {
        terrains.add(TerrainType.SWAMP)
    }
    if (text.containsAny("port", "dock", "pier", "wharf", "marina", "shipyard", "quay")) {
        terrains.add(TerrainType.PORT)
    }
    if (text.containsAny("ruin", "ancient", "crumbl", "decay", "abandon", "forgotten", "lost")) {
        terrains.add(TerrainType.RUINS)
    }

    return terrains
}

// Human-readable description of elevation value
private fun elevationDescription(elevation: Float): String = when {
    elevation >= 0.8f -> "Mountain Peak"
    elevation >= 0.5f -> "High Hills"
    elevation >= 0.3f -> "Hills"
    elevation >= 0.1f -> "Gentle Rise"
    elevation >= -0.1f -> "Flat/Sea Level"
    elevation >= -0.3f -> "Low Ground"
    elevation >= -0.5f -> "Lake Basin"
    else -> "Deep Water"
}

// Calculate elevation from terrain types (-1.0 = deep water, 0.0 = sea level, 1.0 = mountain peak)
private fun calculateElevationFromTerrain(terrains: Set<TerrainType>, overrideElevation: Float? = null): Float {
    // If manually overridden, use that value
    if (overrideElevation != null) return overrideElevation.coerceIn(-1f, 1f)

    // Auto-calculate based on terrain types (highest terrain wins, water lowers)
    var elevation = 0f

    // High elevation terrains
    if (TerrainType.MOUNTAIN in terrains) elevation = maxOf(elevation, 0.9f)
    if (TerrainType.HILLS in terrains) elevation = maxOf(elevation, 0.4f)
    if (TerrainType.CASTLE in terrains) elevation = maxOf(elevation, 0.3f) // Castles on high ground
    if (TerrainType.CHURCH in terrains) elevation = maxOf(elevation, 0.2f)

    // Mid elevation
    if (TerrainType.FOREST in terrains) elevation = maxOf(elevation, 0.15f)
    if (TerrainType.DESERT in terrains) elevation = maxOf(elevation, 0.1f)
    if (TerrainType.GRASS in terrains) elevation = maxOf(elevation, 0.05f)
    if (TerrainType.BUILDING in terrains) elevation = maxOf(elevation, 0.05f)
    if (TerrainType.ROAD in terrains) elevation = maxOf(elevation, 0f)

    // Low elevation / water terrains (these override high terrains if present)
    if (TerrainType.SWAMP in terrains) elevation = minOf(elevation, -0.1f)
    if (TerrainType.COAST in terrains) elevation = minOf(elevation, 0f)
    if (TerrainType.RIVER in terrains) elevation = minOf(elevation, -0.2f)
    if (TerrainType.STREAM in terrains) elevation = minOf(elevation, -0.1f)
    if (TerrainType.LAKE in terrains) elevation = minOf(elevation, -0.4f)
    if (TerrainType.PORT in terrains) elevation = minOf(elevation, -0.1f)

    // Water feature that's not a lake/river (generic water, falls, fountain)
    if (TerrainType.WATER in terrains && TerrainType.LAKE !in terrains && TerrainType.RIVER !in terrains) {
        // Falls/fountains can be at various elevations, leave as-is
    }

    return elevation
}

// Draw parchment background with texture
private fun DrawScope.drawParchmentBackground(seed: Int) {
    val random = kotlin.random.Random(seed)

    // Base fill
    drawRect(color = ParchmentColors.base)

    // Edge vignette - darker at edges
    val edgeWidth = size.width * 0.15f
    val edgeHeight = size.height * 0.15f

    // Top edge
    drawRect(
        color = ParchmentColors.edge.copy(alpha = 0.3f),
        topLeft = Offset.Zero,
        size = androidx.compose.ui.geometry.Size(size.width, edgeHeight)
    )
    // Bottom edge
    drawRect(
        color = ParchmentColors.edge.copy(alpha = 0.3f),
        topLeft = Offset(0f, size.height - edgeHeight),
        size = androidx.compose.ui.geometry.Size(size.width, edgeHeight)
    )
    // Left edge
    drawRect(
        color = ParchmentColors.edge.copy(alpha = 0.2f),
        topLeft = Offset.Zero,
        size = androidx.compose.ui.geometry.Size(edgeWidth, size.height)
    )
    // Right edge
    drawRect(
        color = ParchmentColors.edge.copy(alpha = 0.2f),
        topLeft = Offset(size.width - edgeWidth, 0f),
        size = androidx.compose.ui.geometry.Size(edgeWidth, size.height)
    )

    // Random dots/imperfections
    repeat(80) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val radius = random.nextFloat() * 3f + 1f
        val color = if (random.nextBoolean())
            ParchmentColors.darkSpot.copy(alpha = random.nextFloat() * 0.15f)
        else
            ParchmentColors.lightSpot.copy(alpha = random.nextFloat() * 0.1f)
        drawCircle(color = color, radius = radius, center = Offset(x, y))
    }

    // Occasional larger stains
    repeat(7) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val radius = random.nextFloat() * 40f + 20f
        drawCircle(
            color = ParchmentColors.stain.copy(alpha = random.nextFloat() * 0.08f),
            radius = radius,
            center = Offset(x, y)
        )
    }

    // Draw decorative border
    drawMapBorder()

    // Draw compass rose in corner
    val compassSize = minOf(size.width, size.height) * 0.12f
    drawCompassRose(
        center = Offset(size.width - compassSize - 20f, size.height - compassSize - 20f),
        size = compassSize
    )
}

// Draw decorative double-line border like vintage maps
private fun DrawScope.drawMapBorder() {
    val borderInset = 12f
    val borderWidth = 2f
    val innerInset = borderInset + 6f
    val inkColor = TerrainColors.ink.copy(alpha = 0.4f)

    // Outer border
    drawRect(
        color = inkColor,
        topLeft = Offset(borderInset, borderInset),
        size = androidx.compose.ui.geometry.Size(size.width - borderInset * 2, size.height - borderInset * 2),
        style = Stroke(width = borderWidth)
    )

    // Inner border
    drawRect(
        color = inkColor.copy(alpha = 0.3f),
        topLeft = Offset(innerInset, innerInset),
        size = androidx.compose.ui.geometry.Size(size.width - innerInset * 2, size.height - innerInset * 2),
        style = Stroke(width = 1f)
    )

    // Corner decorations (small flourishes)
    val cornerSize = 20f
    val corners = listOf(
        Offset(borderInset, borderInset),
        Offset(size.width - borderInset, borderInset),
        Offset(size.width - borderInset, size.height - borderInset),
        Offset(borderInset, size.height - borderInset)
    )

    corners.forEachIndexed { index, corner ->
        val xDir = if (index == 0 || index == 3) 1f else -1f
        val yDir = if (index < 2) 1f else -1f

        // Small decorative curl at each corner
        val curlPath = Path().apply {
            moveTo(corner.x, corner.y + cornerSize * yDir)
            quadraticTo(
                corner.x + cornerSize * 0.3f * xDir,
                corner.y + cornerSize * 0.3f * yDir,
                corner.x + cornerSize * xDir,
                corner.y
            )
        }
        drawPath(curlPath, color = inkColor, style = Stroke(width = 1.5f))
    }
}

// Draw a vintage-style compass rose
private fun DrawScope.drawCompassRose(center: Offset, size: Float) {
    val inkColor = TerrainColors.ink.copy(alpha = 0.5f)

    // Outer circle
    drawCircle(
        color = inkColor.copy(alpha = 0.3f),
        radius = size,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Inner circle
    drawCircle(
        color = inkColor.copy(alpha = 0.2f),
        radius = size * 0.7f,
        center = center,
        style = Stroke(width = 1f)
    )

    // Main compass points (N, S, E, W)
    val mainPointLength = size * 0.95f
    val mainPoints = listOf(0f, 90f, 180f, 270f)

    mainPoints.forEach { angle ->
        val radians = ((angle - 90) * PI / 180).toFloat()
        val endX = center.x + cos(radians) * mainPointLength
        val endY = center.y + sin(radians) * mainPointLength

        // Main point arrow (filled triangle)
        val pointPath = Path().apply {
            moveTo(endX, endY)
            val perpAngle = radians + PI.toFloat() / 2
            val baseX = center.x + cos(radians) * size * 0.3f
            val baseY = center.y + sin(radians) * size * 0.3f
            lineTo(baseX + cos(perpAngle) * size * 0.12f, baseY + sin(perpAngle) * size * 0.12f)
            lineTo(baseX - cos(perpAngle) * size * 0.12f, baseY - sin(perpAngle) * size * 0.12f)
            close()
        }

        // North is darker/filled, others are outlined
        if (angle == 0f) {
            drawPath(pointPath, color = inkColor)
        } else {
            drawPath(pointPath, color = inkColor.copy(alpha = 0.4f), style = Stroke(width = 1f))
        }
    }

    // Secondary compass points (NE, SE, SW, NW)
    val secondaryPointLength = size * 0.6f
    val secondaryPoints = listOf(45f, 135f, 225f, 315f)

    secondaryPoints.forEach { angle ->
        val radians = ((angle - 90) * PI / 180).toFloat()
        val endX = center.x + cos(radians) * secondaryPointLength
        val endY = center.y + sin(radians) * secondaryPointLength

        drawLine(
            color = inkColor.copy(alpha = 0.35f),
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 1f
        )
    }

    // Center dot
    drawCircle(
        color = inkColor,
        radius = size * 0.06f,
        center = center
    )
}

// Draw road/path terrain - dirt road with borders passing through center
// If hasRiver or hasStream, draws a bridge over the water
private fun DrawScope.drawRoadTerrain(center: Offset, terrainSize: Float, seed: Int, hasRiver: Boolean = false, hasStream: Boolean = false) {
    val random = kotlin.random.Random(seed)
    val pathWidth = terrainSize * 0.05f  // Narrower road
    val roadColor = Color(0xFFB8A080) // Dusty brown road
    val borderColor = Color(0xFF8A7560) // Darker border
    val edgeColor = Color(0xFF6A5A4A) // Edge texture
    val bridgeWoodColor = Color(0xFF8B7355) // Darker wood for bridge
    val bridgeRailColor = Color(0xFF5C4033) // Dark railing

    // Road winds through center so dot appears ON the road
    val entryAngle = random.nextFloat() * 2 * PI.toFloat()
    val exitAngle = entryAngle + PI.toFloat() + (random.nextFloat() - 0.5f) * 0.3f

    val startX = center.x + cos(entryAngle) * terrainSize * 0.85f
    val startY = center.y + sin(entryAngle) * terrainSize * 0.85f
    val endX = center.x + cos(exitAngle) * terrainSize * 0.85f
    val endY = center.y + sin(exitAngle) * terrainSize * 0.85f

    // Control points for gentle curve through center
    val perpAngle = entryAngle + PI.toFloat() / 2
    val curve1Offset = terrainSize * 0.12f * (0.3f + random.nextFloat() * 0.4f)
    val curve2Offset = terrainSize * 0.12f * (0.3f + random.nextFloat() * 0.4f)

    val ctrl1X = (startX + center.x) / 2 + cos(perpAngle) * curve1Offset
    val ctrl1Y = (startY + center.y) / 2 + sin(perpAngle) * curve1Offset
    val ctrl2X = (center.x + endX) / 2 - cos(perpAngle) * curve2Offset
    val ctrl2Y = (center.y + endY) / 2 - sin(perpAngle) * curve2Offset

    val roadPath = Path().apply {
        moveTo(startX, startY)
        quadraticTo(ctrl1X, ctrl1Y, center.x, center.y)
        quadraticTo(ctrl2X, ctrl2Y, endX, endY)
    }

    // Draw outer border (darkest)
    drawPath(
        path = roadPath,
        color = edgeColor,
        style = Stroke(width = pathWidth + 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Draw middle border
    drawPath(
        path = roadPath,
        color = borderColor,
        style = Stroke(width = pathWidth + 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Draw main road surface
    drawPath(
        path = roadPath,
        color = roadColor,
        style = Stroke(width = pathWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Add subtle texture marks on the road
    val markCount = 3 + random.nextInt(3)
    repeat(markCount) { i ->
        val t = (i + 1).toFloat() / (markCount + 1)
        // Approximate point along curve
        val markX = startX * (1 - t) * (1 - t) + 2 * ctrl1X * (1 - t) * t * 0.5f + center.x * t * t * 0.5f +
                center.x * (1 - t) * (1 - t) * 0.5f + 2 * ctrl2X * (1 - t) * t * 0.5f + endX * t * t
        val markY = startY * (1 - t) * (1 - t) + 2 * ctrl1Y * (1 - t) * t * 0.5f + center.y * t * t * 0.5f +
                center.y * (1 - t) * (1 - t) * 0.5f + 2 * ctrl2Y * (1 - t) * t * 0.5f + endY * t * t

        val markSize = terrainSize * 0.008f
        drawCircle(
            color = borderColor.copy(alpha = 0.3f),
            radius = markSize,
            center = Offset(markX * 0.5f + center.x * 0.5f, markY * 0.5f + center.y * 0.5f)
        )
    }

    // Draw bridge over river/stream near the center
    if (hasRiver || hasStream) {
        val bridgeLength = terrainSize * (if (hasRiver) 0.22f else 0.15f)
        val bridgeWidth = pathWidth * 1.8f

        // Bridge runs perpendicular to the road at center
        val bridgePerpAngle = entryAngle + PI.toFloat() / 2

        // Bridge planks (horizontal lines across the bridge)
        val plankCount = if (hasRiver) 8 else 5
        repeat(plankCount) { i ->
            val plankOffset = (i.toFloat() / (plankCount - 1) - 0.5f) * bridgeLength * 0.9f
            val plankX = center.x + cos(bridgePerpAngle) * plankOffset
            val plankY = center.y + sin(bridgePerpAngle) * plankOffset

            // Draw plank perpendicular to bridge direction (along road direction)
            val plankHalfLen = bridgeWidth * 0.6f
            drawLine(
                color = bridgeWoodColor,
                start = Offset(
                    plankX - cos(entryAngle) * plankHalfLen,
                    plankY - sin(entryAngle) * plankHalfLen
                ),
                end = Offset(
                    plankX + cos(entryAngle) * plankHalfLen,
                    plankY + sin(entryAngle) * plankHalfLen
                ),
                strokeWidth = terrainSize * 0.012f,
                cap = StrokeCap.Butt
            )
        }

        // Bridge railings (two lines along the sides)
        val railOffset = bridgeWidth * 0.5f
        listOf(-1f, 1f).forEach { side ->
            val railStartX = center.x + cos(bridgePerpAngle) * (-bridgeLength * 0.45f) + cos(entryAngle) * railOffset * side
            val railStartY = center.y + sin(bridgePerpAngle) * (-bridgeLength * 0.45f) + sin(entryAngle) * railOffset * side
            val railEndX = center.x + cos(bridgePerpAngle) * (bridgeLength * 0.45f) + cos(entryAngle) * railOffset * side
            val railEndY = center.y + sin(bridgePerpAngle) * (bridgeLength * 0.45f) + sin(entryAngle) * railOffset * side

            drawLine(
                color = bridgeRailColor,
                start = Offset(railStartX, railStartY),
                end = Offset(railEndX, railEndY),
                strokeWidth = terrainSize * 0.008f,
                cap = StrokeCap.Round
            )
        }

        // Bridge posts at ends
        listOf(-0.45f, 0.45f).forEach { endPos ->
            listOf(-1f, 1f).forEach { side ->
                val postX = center.x + cos(bridgePerpAngle) * (bridgeLength * endPos) + cos(entryAngle) * railOffset * side
                val postY = center.y + sin(bridgePerpAngle) * (bridgeLength * endPos) + sin(entryAngle) * railOffset * side
                drawCircle(
                    color = bridgeRailColor,
                    radius = terrainSize * 0.012f,
                    center = Offset(postX, postY)
                )
            }
        }
    }
}

// Draw forest terrain - varied tree styles (conifers, deciduous, bushes)
private fun DrawScope.drawForestTerrain(center: Offset, terrainSize: Float, seed: Int, params: ForestParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val baseTreeCount = 180 + random.nextInt(80) // 4x more trees (180-260 base)
    val treeCount = params?.treeCount ?: baseTreeCount
    val sizeMultiplier = (params?.sizeMultiplier ?: 1f) * 0.45f // Smaller trees to fit more

    // Simplex noise parameters for natural tree clustering
    val noiseScale = 0.06f
    val noiseSeed = seed * 0.1f

    // Color variations for depth and variety
    // Base forest colors (deep green, lush forest)
    val darkGreenBase = Color(0xFF0A4A2A) // Deep shadow/back trees
    val midGreenBase = Color(0xFF1A6A3A) // Rich mid-layer
    val lightGreenBase = Color(0xFF2A8A4A) // Vibrant front/lit trees
    val highlightGreenBase = Color(0xFF4ABA6A) // Bright sunlit highlights

    // Alternative biome colors (autumn forest - oranges and browns)
    val darkGreenAlt = Color(0xFF6A3A1A)  // Deep brown-orange
    val midGreenAlt = Color(0xFF8A5A2A)   // Rust orange
    val lightGreenAlt = Color(0xFFAA7A3A) // Bright orange
    val highlightGreenAlt = Color(0xFFCC9A4A) // Golden yellow

    val shadowColor = Color(0xFF0A2A1A) // Ground shadow

    // Generate trees with depth (y-position determines drawing order and size)
    // Using Simplex noise for natural clustering and Voronoi for biome variation
    data class TreeData(
        val x: Float, val y: Float, val size: Float, val type: Int, val depth: Float,
        val darkGreen: Color, val midGreen: Color, val lightGreen: Color, val highlightGreen: Color
    )
    val trees = mutableListOf<TreeData>()

    // Use cluster-based placement - trees grow in natural groves and runs
    // First, generate cluster centers using varied distribution
    val numClusters = 4 + random.nextInt(3) // 4-6 clusters per tile
    data class ClusterCenter(val x: Float, val y: Float, val size: Float, val density: Float)
    val clusters = (0 until numClusters).map { c ->
        val cRandom = kotlin.random.Random(seed + c * 17)
        // Distribute cluster centers across the tile with randomness
        val angle = (c.toFloat() / numClusters) * 2 * PI.toFloat() + cRandom.nextFloat() * 0.8f
        val dist = terrainSize * (0.1f + cRandom.nextFloat() * 0.25f)
        ClusterCenter(
            x = center.x + cos(angle) * dist + (cRandom.nextFloat() - 0.5f) * terrainSize * 0.2f,
            y = center.y + sin(angle) * dist + (cRandom.nextFloat() - 0.5f) * terrainSize * 0.2f,
            size = terrainSize * (0.12f + cRandom.nextFloat() * 0.12f), // Cluster radius
            density = 0.6f + cRandom.nextFloat() * 0.4f // How dense this cluster is
        )
    }

    // Also add a main central cluster for connected feel
    val centralCluster = ClusterCenter(
        x = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.1f,
        y = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.1f,
        size = terrainSize * 0.22f,
        density = 0.8f + random.nextFloat() * 0.2f
    )
    val allClusters = clusters + centralCluster

    repeat(treeCount) { i ->
        val tRandom = kotlin.random.Random(seed + i * 31)

        // Pick a cluster to place this tree near (weighted by cluster density)
        val clusterWeights = allClusters.map { it.density }
        val totalWeight = clusterWeights.sum()
        var pick = tRandom.nextFloat() * totalWeight
        var chosenCluster = allClusters[0]
        for (cluster in allClusters) {
            pick -= cluster.density
            if (pick <= 0) {
                chosenCluster = cluster
                break
            }
        }

        // Position tree within chosen cluster with natural falloff
        // Use noise to create "runs" - elongated groups of trees
        val runAngle = SimplexNoise.noise2D(noiseSeed + i * 0.5f, seed * 0.1f) * PI.toFloat()
        val runStretch = 1.3f + SimplexNoise.noise2DNormalized(noiseSeed + i * 0.3f, seed * 0.2f) * 0.8f

        // Distance from cluster center with exponential falloff for natural clustering
        val distFactor = tRandom.nextFloat()
        val dist = chosenCluster.size * kotlin.math.sqrt(distFactor) // sqrt gives more trees near center

        // Apply run stretching - trees form elongated groups
        val baseAngle = tRandom.nextFloat() * 2 * PI.toFloat()
        val stretchedX = cos(baseAngle) * dist
        val stretchedY = sin(baseAngle) * dist * (1f / runStretch)

        // Rotate by run angle
        val finalX = stretchedX * cos(runAngle) - stretchedY * sin(runAngle)
        val finalY = stretchedX * sin(runAngle) + stretchedY * cos(runAngle)

        val treeX = chosenCluster.x + finalX + (tRandom.nextFloat() - 0.5f) * terrainSize * 0.025f
        val treeY = chosenCluster.y + finalY + (tRandom.nextFloat() - 0.5f) * terrainSize * 0.025f

        // Skip trees too far from center (keep within circular bounds)
        val distFromCenter = kotlin.math.sqrt((treeX - center.x) * (treeX - center.x) + (treeY - center.y) * (treeY - center.y))
        if (distFromCenter > terrainSize * 0.47f) return@repeat

        val depth = (treeY - center.y + terrainSize * 0.5f) / terrainSize // 0=back, 1=front

        // Use Voronoi biome blending for color variation
        val localX = treeX - center.x + terrainSize * 0.5f
        val localY = treeY - center.y + terrainSize * 0.5f
        val densityMult = BiomeBlending.getDensityMultiplier(localX, localY, terrainSize, seed)

        // Blend tree colors based on Voronoi cells
        val darkGreen = BiomeBlending.blendColors(darkGreenBase, darkGreenAlt, localX, localY, terrainSize, seed)
        val midGreen = BiomeBlending.blendColors(midGreenBase, midGreenAlt, localX, localY, terrainSize, seed)
        val lightGreen = BiomeBlending.blendColors(lightGreenBase, lightGreenAlt, localX, localY, terrainSize, seed)
        val highlightGreen = BiomeBlending.blendColors(highlightGreenBase, highlightGreenAlt, localX, localY, terrainSize, seed)

        // Use noise for size variation - trees in cluster centers tend to be larger
        val sizeNoise = SimplexNoise.noise2DNormalized(treeX * noiseScale, treeY * noiseScale)
        val clusterCenterBonus = 1f - (distFactor * 0.25f) // Trees near cluster centers are bigger
        val baseSize = terrainSize * (0.025f + sizeNoise * 0.04f) * sizeMultiplier * clusterCenterBonus * (0.85f + densityMult * 0.3f)
        val sizeByDepth = baseSize * (0.55f + depth * 0.55f) // Back trees smaller
        // Only use conifer types (0, 1) and bushes (4), willow (5) - skip round/oval deciduous (2, 3)
        val allowedTypes = listOf(0, 1, 4, 5)
        val treeType = allowedTypes[tRandom.nextInt(allowedTypes.size)]

        trees.add(TreeData(treeX, treeY, sizeByDepth, treeType, depth, darkGreen, midGreen, lightGreen, highlightGreen))
    }

    // Sort by Y position (back to front)
    trees.sortBy { it.y }

    // Draw ground shadows first (under all trees)
    for (tree in trees) {
        val shadowOffsetX = tree.size * 0.3f
        val shadowOffsetY = tree.size * 0.15f
        drawOval(
            color = shadowColor.copy(alpha = 0.25f),
            topLeft = Offset(tree.x - tree.size * 0.4f + shadowOffsetX, tree.y + shadowOffsetY),
            size = androidx.compose.ui.geometry.Size(tree.size * 0.8f, tree.size * 0.3f)
        )
    }

    // Draw trees back to front
    for (tree in trees) {
        val treeX = tree.x
        val treeY = tree.y
        val treeSize = tree.size
        val treeType = tree.type
        val depth = tree.depth

        // Color based on depth (back trees darker) - using per-tree biome-blended colors
        val treeColor = when {
            depth < 0.33f -> tree.darkGreen
            depth < 0.66f -> tree.midGreen
            else -> tree.lightGreen
        }
        val highlightGreen = tree.highlightGreen
        val trunkColor = TerrainColors.treeTrunk.copy(alpha = 0.7f + depth * 0.3f)

        when (treeType) {
            0 -> {
                // Tall conifer (3 tiers) with shadow detail
                drawLine(
                    color = trunkColor,
                    start = Offset(treeX, treeY + treeSize * 0.15f),
                    end = Offset(treeX, treeY - treeSize * 0.1f),
                    strokeWidth = 2.5f
                )

                // Bottom tier
                val treePath = Path().apply {
                    moveTo(treeX, treeY - treeSize * 0.3f)
                    lineTo(treeX - treeSize * 0.5f, treeY)
                    lineTo(treeX + treeSize * 0.5f, treeY)
                    close()
                }
                drawPath(treePath, color = treeColor)

                // Middle tier
                val midPath = Path().apply {
                    moveTo(treeX, treeY - treeSize * 0.7f)
                    lineTo(treeX - treeSize * 0.4f, treeY - treeSize * 0.2f)
                    lineTo(treeX + treeSize * 0.4f, treeY - treeSize * 0.2f)
                    close()
                }
                drawPath(midPath, color = treeColor)

                // Top tier
                val topPath = Path().apply {
                    moveTo(treeX, treeY - treeSize)
                    lineTo(treeX - treeSize * 0.25f, treeY - treeSize * 0.5f)
                    lineTo(treeX + treeSize * 0.25f, treeY - treeSize * 0.5f)
                    close()
                }
                drawPath(topPath, color = treeColor)

                // Highlight on sunny side
                if (depth > 0.5f) {
                    val highlightPath = Path().apply {
                        moveTo(treeX - treeSize * 0.1f, treeY - treeSize * 0.8f)
                        lineTo(treeX - treeSize * 0.25f, treeY - treeSize * 0.3f)
                        lineTo(treeX - treeSize * 0.05f, treeY - treeSize * 0.35f)
                        close()
                    }
                    drawPath(highlightPath, color = highlightGreen.copy(alpha = 0.4f))
                }
            }
            1 -> {
                // Short/wide conifer
                drawLine(
                    color = trunkColor,
                    start = Offset(treeX, treeY + treeSize * 0.1f),
                    end = Offset(treeX, treeY - treeSize * 0.1f),
                    strokeWidth = 2f
                )

                val shortPath = Path().apply {
                    moveTo(treeX, treeY - treeSize * 0.7f)
                    lineTo(treeX - treeSize * 0.55f, treeY)
                    lineTo(treeX + treeSize * 0.55f, treeY)
                    close()
                }
                drawPath(shortPath, color = treeColor)
            }
            2 -> {
                // Round deciduous tree with layered canopy
                val canopyY = treeY - treeSize * 0.45f
                val canopyRadius = treeSize * 0.38f

                drawLine(
                    color = trunkColor,
                    start = Offset(treeX, treeY + treeSize * 0.1f),
                    end = Offset(treeX, canopyY + canopyRadius * 0.3f),
                    strokeWidth = 3f
                )

                // Shadow side of canopy
                drawCircle(
                    color = tree.darkGreen,
                    radius = canopyRadius,
                    center = Offset(treeX + treeSize * 0.05f, canopyY + treeSize * 0.03f)
                )
                // Main canopy
                drawCircle(
                    color = treeColor,
                    radius = canopyRadius,
                    center = Offset(treeX, canopyY)
                )
                // Highlight
                drawCircle(
                    color = highlightGreen.copy(alpha = 0.35f),
                    radius = canopyRadius * 0.5f,
                    center = Offset(treeX - canopyRadius * 0.3f, canopyY - canopyRadius * 0.25f)
                )
            }
            3 -> {
                // Oval/tall deciduous tree
                val canopyY = treeY - treeSize * 0.5f
                val ovalHeight = treeSize * 0.45f
                val ovalWidth = treeSize * 0.3f

                drawLine(
                    color = trunkColor,
                    start = Offset(treeX, treeY + treeSize * 0.1f),
                    end = Offset(treeX, canopyY + ovalHeight * 0.2f),
                    strokeWidth = 2.5f
                )

                val ovalPath = Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            treeX - ovalWidth,
                            canopyY - ovalHeight,
                            treeX + ovalWidth,
                            canopyY + ovalHeight * 0.3f
                        )
                    )
                }
                drawPath(ovalPath, color = treeColor)
            }
            4 -> {
                // Bush cluster with multiple overlapping circles
                val bushY = treeY - treeSize * 0.1f
                val bushColor = treeColor.copy(alpha = 0.85f)

                // Multiple overlapping circles for organic shape
                drawCircle(color = tree.darkGreen.copy(alpha = 0.6f), radius = treeSize * 0.28f,
                    center = Offset(treeX + treeSize * 0.08f, bushY + treeSize * 0.05f))
                drawCircle(color = bushColor, radius = treeSize * 0.25f,
                    center = Offset(treeX, bushY))
                drawCircle(color = bushColor, radius = treeSize * 0.2f,
                    center = Offset(treeX - treeSize * 0.18f, bushY + treeSize * 0.02f))
                drawCircle(color = bushColor, radius = treeSize * 0.2f,
                    center = Offset(treeX + treeSize * 0.15f, bushY - treeSize * 0.02f))
                drawCircle(color = highlightGreen.copy(alpha = 0.3f), radius = treeSize * 0.12f,
                    center = Offset(treeX - treeSize * 0.1f, bushY - treeSize * 0.08f))
            }
            else -> {
                // Willow-like tree with drooping branches
                val canopyY = treeY - treeSize * 0.4f

                drawLine(
                    color = trunkColor,
                    start = Offset(treeX, treeY + treeSize * 0.1f),
                    end = Offset(treeX, canopyY),
                    strokeWidth = 3f
                )

                // Drooping branch curves
                repeat(5) { b ->
                    val branchAngle = (b - 2) * 0.4f
                    val branchPath = Path().apply {
                        moveTo(treeX, canopyY)
                        val endX = treeX + kotlin.math.sin(branchAngle) * treeSize * 0.5f
                        val endY = canopyY + treeSize * 0.4f
                        quadraticTo(
                            treeX + kotlin.math.sin(branchAngle) * treeSize * 0.3f,
                            canopyY + treeSize * 0.1f,
                            endX, endY
                        )
                    }
                    drawPath(branchPath, color = treeColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
                }
                // Canopy crown
                drawCircle(color = treeColor, radius = treeSize * 0.25f, center = Offset(treeX, canopyY - treeSize * 0.1f))
            }
        }
    }
}

// Draw water terrain - fountain or small water feature with gentle waves
private fun DrawScope.drawWaterTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val waterColor = Color(0xFF4A7A9A)
    val highlightColor = Color(0xFF6A9ABA)

    // Small circular pool
    val poolRadius = terrainSize * 0.25f

    // Draw pool
    drawCircle(
        color = waterColor,
        radius = poolRadius,
        center = center
    )

    // Add highlight
    drawCircle(
        color = highlightColor.copy(alpha = 0.4f),
        radius = poolRadius * 0.6f,
        center = Offset(center.x - poolRadius * 0.2f, center.y - poolRadius * 0.2f)
    )

    // Add gentle ripples
    repeat(2) { i ->
        val rippleRadius = poolRadius * (0.5f + i * 0.35f)
        drawCircle(
            color = highlightColor.copy(alpha = 0.2f - i * 0.08f),
            radius = rippleRadius,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}

// Draw lake terrain - body of water with natural shoreline and depth gradient
// Lakes shrink away from elevated terrain (hills, mountains, foothills) - both local and from neighbors
private fun DrawScope.drawLakeTerrain(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    params: LakeParamsDto? = null,
    hasHills: Boolean = false,
    hasMountain: Boolean = false,
    neighborElevations: NeighborElevations? = null
) {
    val random = kotlin.random.Random(seed)
    val deepWater = Color(0xFF1A4A6A) // Deep center blue
    val midWater = Color(0xFF3A6A8A) // Mid-depth blue
    val shallowWater = Color(0xFF5A8AAA) // Shallow edge blue
    val shoreColor = Color(0xFF8A9A7A) // Sandy/muddy shore
    val highlightColor = Color(0xFF8ACAEE) // Bright water sparkle

    // Support both legacy single multiplier and new X/Y multipliers
    val sizeMultiplierX = params?.diameterMultiplierX ?: params?.diameterMultiplier ?: 1f
    val sizeMultiplierY = params?.diameterMultiplierY ?: params?.diameterMultiplier ?: 1f

    // Generate amoeba-like organic lake shape - configurable via params
    val points = params?.shapePoints ?: 20
    val noiseScale = params?.noiseScale ?: 0.35f  // How much noise affects shape (0-1)
    val baseRadiusX = terrainSize * 0.38f * sizeMultiplierX
    val baseRadiusY = terrainSize * 0.38f * sizeMultiplierY

    // Use seed to create unique offset into noise space - large offsets ensure unique shapes
    val seedOffsetX = (seed % 1000) * 7.3f
    val seedOffsetY = ((seed / 1000) % 1000) * 11.7f

    // Check for high-elevation neighbors (mountains/hills in adjacent tiles)
    // Elevation >= 0.3 indicates hills, >= 0.5 indicates mountains
    val northIsElevated = (neighborElevations?.north ?: 0f) >= 0.3f
    val southIsElevated = (neighborElevations?.south ?: 0f) >= 0.3f
    val eastIsElevated = (neighborElevations?.east ?: 0f) >= 0.3f
    val westIsElevated = (neighborElevations?.west ?: 0f) >= 0.3f

    // Generate irregular radii using multiple octaves of Simplex noise for amoeba shape
    // Now returns pairs of (radiusX, radiusY) for elliptical shapes
    val radiiPairs = (0 until points).map { i ->
        val angle = (i.toFloat() / points) * 2 * PI.toFloat()
        val nx = cos(angle)
        val ny = sin(angle)

        // Multiple octaves of noise for organic irregularity - scaled by noiseScale
        val noise1 = SimplexNoise.noise2D(nx * 1.5f + seedOffsetX, ny * 1.5f + seedOffsetY) * 0.5f
        val noise2 = SimplexNoise.noise2D(nx * 3f + seedOffsetX + 100f, ny * 3f + seedOffsetY + 100f) * 0.25f
        val noise3 = SimplexNoise.noise2D(nx * 5f + seedOffsetX + 200f, ny * 5f + seedOffsetY + 200f) * 0.12f

        // Add lobes - some points bulge out more dramatically
        val lobeNoise = SimplexNoise.noise2D(nx * 0.8f + seedOffsetX + 300f, ny * 0.8f + seedOffsetY + 300f)
        val lobe = if (lobeNoise > 0.4f) (lobeNoise - 0.4f) * 0.3f else 0f

        val totalNoise = (noise1 + noise2 + noise3 + lobe) * noiseScale
        var noiseMultiplier = 0.75f + totalNoise * 0.5f + 0.1f

        // Shrink lake away from elevated terrain (local)
        if (hasMountain || hasHills) {
            val elevationShrink = if (ny < -0.2f) {
                val shrinkFactor = if (hasMountain) 0.5f else 0.7f
                shrinkFactor + (1f - shrinkFactor) * ((ny + 1f) / 0.8f).coerceIn(0f, 1f)
            } else 1f
            noiseMultiplier *= elevationShrink
        }

        // Shrink lake away from elevated neighbors (adjacent tiles with mountains/hills)
        // North neighbor is in negative Y direction
        if (northIsElevated && ny < -0.3f) {
            val shrink = 0.5f + 0.5f * ((ny + 1f) / 0.7f).coerceIn(0f, 1f)
            noiseMultiplier *= shrink
        }
        // South neighbor is in positive Y direction
        if (southIsElevated && ny > 0.3f) {
            val shrink = 0.5f + 0.5f * ((1f - ny) / 0.7f).coerceIn(0f, 1f)
            noiseMultiplier *= shrink
        }
        // East neighbor is in positive X direction
        if (eastIsElevated && nx > 0.3f) {
            val shrink = 0.5f + 0.5f * ((1f - nx) / 0.7f).coerceIn(0f, 1f)
            noiseMultiplier *= shrink
        }
        // West neighbor is in negative X direction
        if (westIsElevated && nx < -0.3f) {
            val shrink = 0.5f + 0.5f * ((nx + 1f) / 0.7f).coerceIn(0f, 1f)
            noiseMultiplier *= shrink
        }

        Pair(baseRadiusX * noiseMultiplier, baseRadiusY * noiseMultiplier)
    }

    // Helper to create ultra-smooth lake path using cubic bezier curves
    fun createSmoothLakePath(scale: Float): Path = Path().apply {
        // Calculate points on the curve using elliptical radii
        val curvePoints = (0 until points).map { i ->
            val angle = (i.toFloat() / points) * 2 * PI.toFloat()
            val (rx, ry) = radiiPairs[i]
            Offset(
                center.x + cos(angle) * rx * scale,
                center.y + sin(angle) * ry * scale
            )
        }

        moveTo(curvePoints[0].x, curvePoints[0].y)

        // Use cubic bezier for smoother curves
        for (i in 0 until points) {
            val p0 = curvePoints[(i - 1 + points) % points]
            val p1 = curvePoints[i]
            val p2 = curvePoints[(i + 1) % points]
            val p3 = curvePoints[(i + 2) % points]

            // Catmull-Rom to Bezier conversion for smooth curves
            val tension = 0.4f
            val ctrl1X = p1.x + (p2.x - p0.x) * tension
            val ctrl1Y = p1.y + (p2.y - p0.y) * tension
            val ctrl2X = p2.x - (p3.x - p1.x) * tension
            val ctrl2Y = p2.y - (p3.y - p1.y) * tension

            cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, p2.x, p2.y)
        }
        close()
    }

    // Draw shore/beach ring (slightly larger than lake)
    val shorePath = createSmoothLakePath(1.08f)
    drawPath(shorePath, color = shoreColor)

    // Draw multiple blended water layers for smooth color transition
    // Layer 1: Outer shallow water
    val shallowPath = createSmoothLakePath(1.0f)
    drawPath(shallowPath, color = shallowWater)

    // Blend layer 1-2: Transition from shallow to mid
    val blend1Path = createSmoothLakePath(0.88f)
    val blend1Color = Color(
        red = (shallowWater.red + midWater.red) / 2f,
        green = (shallowWater.green + midWater.green) / 2f,
        blue = (shallowWater.blue + midWater.blue) / 2f,
        alpha = 1f
    )
    drawPath(blend1Path, color = blend1Color)

    // Layer 2: Mid-depth water
    val midPath = createSmoothLakePath(0.75f)
    drawPath(midPath, color = midWater)

    // Blend layer 2-3: Transition from mid to deep
    val blend2Path = createSmoothLakePath(0.62f)
    val blend2Color = Color(
        red = (midWater.red + deepWater.red) / 2f,
        green = (midWater.green + deepWater.green) / 2f,
        blue = (midWater.blue + deepWater.blue) / 2f,
        alpha = 1f
    )
    drawPath(blend2Path, color = blend2Color)

    // Layer 3: Deep center
    val deepPath = createSmoothLakePath(0.48f)
    drawPath(deepPath, color = deepWater)

    // Deepest center - slightly darker
    val deepestPath = createSmoothLakePath(0.3f)
    val deepestColor = Color(
        red = deepWater.red * 0.85f,
        green = deepWater.green * 0.85f,
        blue = deepWater.blue * 0.9f,
        alpha = 1f
    )
    drawPath(deepestPath, color = deepestColor)

    // Add organic shoreline detail (small rocks/pebbles)
    val avgBaseRadius = (baseRadiusX + baseRadiusY) / 2f
    repeat(10) {
        val angle = random.nextFloat() * 2 * PI.toFloat()
        val dist = avgBaseRadius * (1.02f + random.nextFloat() * 0.08f)
        val rockX = center.x + cos(angle) * dist
        val rockY = center.y + sin(angle) * dist
        val rockSize = terrainSize * (0.008f + random.nextFloat() * 0.012f)
        drawCircle(
            color = shoreColor.copy(alpha = 0.5f + random.nextFloat() * 0.3f),
            radius = rockSize,
            center = Offset(rockX, rockY)
        )
    }

    // Add water surface details - gentle ripples with soft edges
    repeat(4) { i ->
        val rippleY = center.y + (i - 1.5f) * terrainSize * 0.1f
        val rippleOffset = (random.nextFloat() - 0.5f) * terrainSize * 0.08f
        val ripplePath = Path().apply {
            val startX = center.x - terrainSize * 0.15f + rippleOffset
            moveTo(startX, rippleY)
            cubicTo(
                startX + terrainSize * 0.08f, rippleY - terrainSize * 0.018f,
                startX + terrainSize * 0.2f, rippleY + terrainSize * 0.012f,
                startX + terrainSize * 0.3f, rippleY
            )
        }
        drawPath(
            path = ripplePath,
            color = highlightColor.copy(alpha = 0.18f - i * 0.03f),
            style = Stroke(width = 1f, cap = StrokeCap.Round)
        )
    }

    // Add soft sparkle highlights (sun reflection) with gradient-like effect
    val highlightOffset = terrainSize * 0.1f
    // Outer glow
    drawCircle(
        color = highlightColor.copy(alpha = 0.15f),
        radius = terrainSize * 0.09f,
        center = Offset(center.x - highlightOffset, center.y - highlightOffset * 0.8f)
    )
    // Mid glow
    drawCircle(
        color = highlightColor.copy(alpha = 0.25f),
        radius = terrainSize * 0.06f,
        center = Offset(center.x - highlightOffset, center.y - highlightOffset * 0.8f)
    )
    // Bright center
    drawCircle(
        color = highlightColor.copy(alpha = 0.4f),
        radius = terrainSize * 0.035f,
        center = Offset(center.x - highlightOffset, center.y - highlightOffset * 0.8f)
    )
    // Secondary smaller highlight
    drawCircle(
        color = highlightColor.copy(alpha = 0.2f),
        radius = terrainSize * 0.025f,
        center = Offset(center.x - highlightOffset * 1.6f, center.y - highlightOffset * 0.4f)
    )

    // Maybe add a tiny island in larger lakes
    val avgSizeMultiplier = (sizeMultiplierX + sizeMultiplierY) / 2f
    if (avgSizeMultiplier > 1.2f && random.nextFloat() > 0.5f) {
        val islandX = center.x + (random.nextFloat() - 0.5f) * baseRadiusX * 0.4f
        val islandY = center.y + (random.nextFloat() - 0.5f) * baseRadiusY * 0.4f
        val islandSize = terrainSize * 0.04f
        // Island base with soft edge
        drawCircle(
            color = shoreColor.copy(alpha = 0.5f),
            radius = islandSize * 1.2f,
            center = Offset(islandX, islandY)
        )
        drawCircle(
            color = shoreColor,
            radius = islandSize,
            center = Offset(islandX, islandY)
        )
        // Tiny tree on island
        drawCircle(
            color = TerrainColors.tree,
            radius = islandSize * 0.6f,
            center = Offset(islandX, islandY - islandSize * 0.5f)
        )
    }
}

// Draw river terrain - flowing water with natural meandering and variable width
private fun DrawScope.drawRiverTerrain(center: Offset, terrainSize: Float, seed: Int, params: RiverParamsDto? = null, hasLake: Boolean = false, neighborElevations: NeighborElevations? = null, currentElevation: Float = 0f, neighborRivers: NeighborRivers? = null) {
    val random = kotlin.random.Random(seed)
    val deepWater = Color(0xFF2A5A7A) // Deep river blue
    val riverColor = Color(0xFF4A8AAA) // Main river blue
    val highlightColor = Color(0xFF7ABADD) // Bright highlight
    val bankColor = Color(0xFF3A5060) // Muddy bank
    val widthMultiplier = params?.widthMultiplier ?: 1f

    if (hasLake) {
        // When there's a lake, draw river as inlet/outlet flowing into the lake
        val inletCount = 1 + random.nextInt(2)
        repeat(inletCount) { i ->
            val inletRandom = kotlin.random.Random(seed + i * 200)
            val angle = if (i == 0) {
                -PI.toFloat() / 2 + (inletRandom.nextFloat() - 0.5f) * PI.toFloat() * 0.4f
            } else {
                PI.toFloat() / 2 + (inletRandom.nextFloat() - 0.5f) * PI.toFloat() * 0.4f
            }

            val startDist = terrainSize * 0.48f
            val startX = center.x + cos(angle) * startDist
            val startY = center.y + sin(angle) * startDist
            val endDist = terrainSize * 0.22f
            val endX = center.x + cos(angle) * endDist
            val endY = center.y + sin(angle) * endDist

            val midDist = (startDist + endDist) / 2
            val perpAngle = angle + PI.toFloat() / 2
            val curve = (inletRandom.nextFloat() - 0.5f) * terrainSize * 0.2f
            val midX = center.x + cos(angle) * midDist + cos(perpAngle) * curve
            val midY = center.y + sin(angle) * midDist + sin(perpAngle) * curve

            val inletPath = Path().apply {
                moveTo(startX, startY)
                quadraticTo(midX, midY, endX, endY)
            }

            drawPath(inletPath, color = bankColor, style = Stroke(width = 8f * widthMultiplier, cap = StrokeCap.Round))
            drawPath(inletPath, color = riverColor, style = Stroke(width = 6f * widthMultiplier, cap = StrokeCap.Round))
            drawPath(inletPath, color = deepWater, style = Stroke(width = 3f * widthMultiplier, cap = StrokeCap.Round))
        }
    } else {
        // Normal river (no lake) - connects to neighboring rivers if present
        // Check which neighbors have rivers
        val hasNorthRiver = neighborRivers?.north == true
        val hasSouthRiver = neighborRivers?.south == true
        val hasEastRiver = neighborRivers?.east == true
        val hasWestRiver = neighborRivers?.west == true

        // Count river neighbors
        val riverNeighborCount = listOf(hasNorthRiver, hasSouthRiver, hasEastRiver, hasWestRiver).count { it }

        // Edge positions - use center of each edge so adjacent tiles align perfectly
        val northEdge = Offset(center.x, center.y - terrainSize * 0.5f)
        val southEdge = Offset(center.x, center.y + terrainSize * 0.5f)
        val eastEdge = Offset(center.x + terrainSize * 0.5f, center.y)
        val westEdge = Offset(center.x - terrainSize * 0.5f, center.y)

        // Collect all edges that have river neighbors
        val riverEdges = mutableListOf<Offset>()
        if (hasNorthRiver) riverEdges.add(northEdge)
        if (hasSouthRiver) riverEdges.add(southEdge)
        if (hasEastRiver) riverEdges.add(eastEdge)
        if (hasWestRiver) riverEdges.add(westEdge)

        // If we have river neighbors, draw river segments connecting them through center
        // If only one neighbor, add a pond at the opposite end
        // If no neighbors, use flow direction

        val (flowX, flowY) = calculateFlowDirection(currentElevation, neighborElevations)

        val entryX: Float
        val entryY: Float
        val exitX: Float
        val exitY: Float
        val downstreamHasRiver: Boolean

        when {
            riverNeighborCount >= 2 -> {
                // Multiple river neighbors - pick first two (sorted by elevation for consistency)
                val sortedEdges = riverEdges.sortedByDescending { edge ->
                    when (edge) {
                        northEdge -> neighborElevations?.north ?: 0f
                        southEdge -> neighborElevations?.south ?: 0f
                        eastEdge -> neighborElevations?.east ?: 0f
                        westEdge -> neighborElevations?.west ?: 0f
                        else -> 0f
                    }
                }
                entryX = sortedEdges[0].x
                entryY = sortedEdges[0].y
                exitX = sortedEdges[1].x
                exitY = sortedEdges[1].y
                downstreamHasRiver = true
            }
            riverNeighborCount == 1 -> {
                // One river neighbor - connect to it and add pond at opposite end
                val neighborEdge = riverEdges.first()
                // Determine if neighbor is upstream or downstream based on elevation
                val neighborElevation = when (neighborEdge) {
                    northEdge -> neighborElevations?.north ?: 0f
                    southEdge -> neighborElevations?.south ?: 0f
                    eastEdge -> neighborElevations?.east ?: 0f
                    westEdge -> neighborElevations?.west ?: 0f
                    else -> 0f
                }

                if (neighborElevation > currentElevation) {
                    // Neighbor is upstream - water enters from neighbor, exits toward flow
                    entryX = neighborEdge.x
                    entryY = neighborEdge.y
                    exitX = center.x + flowX * terrainSize * 0.4f
                    exitY = center.y + flowY * terrainSize * 0.4f
                } else {
                    // Neighbor is downstream - water enters from opposite of neighbor, exits to neighbor
                    entryX = center.x - (neighborEdge.x - center.x) * 0.8f
                    entryY = center.y - (neighborEdge.y - center.y) * 0.8f
                    exitX = neighborEdge.x
                    exitY = neighborEdge.y
                }
                downstreamHasRiver = neighborElevation <= currentElevation
            }
            else -> {
                // No river neighbors - use flow direction with randomness
                entryX = center.x - flowX * terrainSize * 0.4f + (random.nextFloat() - 0.5f) * terrainSize * 0.15f
                entryY = center.y - flowY * terrainSize * 0.4f + (random.nextFloat() - 0.5f) * terrainSize * 0.15f
                exitX = center.x + flowX * terrainSize * 0.4f + (random.nextFloat() - 0.5f) * terrainSize * 0.15f
                exitY = center.y + flowY * terrainSize * 0.4f + (random.nextFloat() - 0.5f) * terrainSize * 0.15f
                downstreamHasRiver = false
            }
        }

        // Calculate actual flow direction from entry to exit
        val actualFlowX = exitX - entryX
        val actualFlowY = exitY - entryY
        val flowLen = kotlin.math.sqrt(actualFlowX * actualFlowX + actualFlowY * actualFlowY).coerceAtLeast(0.001f)
        val normFlowX = actualFlowX / flowLen
        val normFlowY = actualFlowY / flowLen

        // Perpendicular direction for meandering
        val perpX = -normFlowY
        val perpY = normFlowX

        val numSegments = 5
        val points = mutableListOf<Offset>()
        points.add(Offset(entryX, entryY))

        for (i in 1 until numSegments) {
            val t = i.toFloat() / numSegments
            val baseX = entryX + (exitX - entryX) * t
            val baseY = entryY + (exitY - entryY) * t
            // Meander perpendicular to actual flow direction
            val meander = kotlin.math.sin(t * PI.toFloat() * 1.5f + seed * 0.3f) * terrainSize * 0.15f
            val noise = (random.nextFloat() - 0.5f) * terrainSize * 0.06f
            points.add(Offset(
                baseX + perpX * meander + perpX * noise,
                baseY + perpY * meander + perpY * noise
            ))
        }
        points.add(Offset(exitX, exitY))

        val baseWidth = 8f * widthMultiplier
        fun widthAt(t: Float): Float {
            val bulge = kotlin.math.sin(t * PI.toFloat()) * 0.5f + 0.8f
            return baseWidth * bulge * (0.9f + random.nextFloat() * 0.2f)
        }

        val leftBank = mutableListOf<Offset>()
        val rightBank = mutableListOf<Offset>()

        for (i in points.indices) {
            val t = i.toFloat() / (points.size - 1)
            val width = widthAt(t)
            val current = points[i]

            val next = if (i < points.size - 1) points[i + 1] else current
            val prev = if (i > 0) points[i - 1] else current
            val dx = next.x - prev.x
            val dy = next.y - prev.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            val perpX = -dy / len
            val perpY = dx / len

            leftBank.add(Offset(current.x + perpX * width, current.y + perpY * width))
            rightBank.add(Offset(current.x - perpX * width, current.y - perpY * width))
        }

        val riverShape = Path().apply {
            moveTo(leftBank.first().x, leftBank.first().y)
            for (i in 1 until leftBank.size) {
                val prev = leftBank[i - 1]
                val curr = leftBank[i]
                quadraticTo((prev.x + curr.x) / 2, (prev.y + curr.y) / 2, curr.x, curr.y)
            }
            for (i in rightBank.indices.reversed()) {
                val curr = rightBank[i]
                if (i == rightBank.size - 1) {
                    lineTo(curr.x, curr.y)
                } else {
                    val nextPt = rightBank[i + 1]
                    quadraticTo((curr.x + nextPt.x) / 2, (curr.y + nextPt.y) / 2, curr.x, curr.y)
                }
            }
            close()
        }

        drawPath(riverShape, color = bankColor, style = Stroke(width = 3f))
        drawPath(riverShape, color = riverColor)

        val centerPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                quadraticTo(
                    (prev.x + curr.x) / 2 + (random.nextFloat() - 0.5f) * 3f,
                    (prev.y + curr.y) / 2,
                    curr.x, curr.y
                )
            }
        }
        drawPath(centerPath, color = deepWater, style = Stroke(width = 4f * widthMultiplier, cap = StrokeCap.Round))

        for (i in 1 until points.size - 1) {
            val pt = points[i]
            val flowLength = terrainSize * 0.04f
            val flowPath = Path().apply {
                moveTo(pt.x - flowLength, pt.y - flowLength * 0.3f)
                lineTo(pt.x, pt.y)
                lineTo(pt.x - flowLength, pt.y + flowLength * 0.3f)
            }
            drawPath(flowPath, color = highlightColor.copy(alpha = 0.4f), style = Stroke(width = 1f))
        }

        repeat(3) { i ->
            val t = 0.2f + i * 0.3f
            val idx = (t * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
            val pt = points[idx]
            val offsetX = (random.nextFloat() - 0.5f) * baseWidth * 0.5f
            drawCircle(
                color = highlightColor.copy(alpha = 0.6f),
                radius = 2f,
                center = Offset(pt.x + offsetX, pt.y)
            )
        }

        // If river dead-ends (no downstream neighbor with river), add a small pond
        if (!downstreamHasRiver) {
            val exitPoint = points.last()
            val pondRadius = terrainSize * 0.12f
            val pondColor = Color(0xFF3A6A8A) // Pond blue
            val pondDeepColor = Color(0xFF2A5070) // Deep pond center
            val pondHighlight = Color(0xFF6A9ABA)

            // Draw pond slightly beyond exit point (using actual flow direction)
            val pondCenter = Offset(
                exitPoint.x + normFlowX * terrainSize * 0.08f,
                exitPoint.y + normFlowY * terrainSize * 0.08f
            )

            // Bank/muddy shore
            drawCircle(
                color = bankColor,
                radius = pondRadius + 2f,
                center = pondCenter
            )
            // Main pond water
            drawCircle(
                color = pondColor,
                radius = pondRadius,
                center = pondCenter
            )
            // Deep center
            drawCircle(
                color = pondDeepColor,
                radius = pondRadius * 0.6f,
                center = pondCenter
            )
            // Highlight
            drawCircle(
                color = pondHighlight.copy(alpha = 0.3f),
                radius = pondRadius * 0.3f,
                center = Offset(pondCenter.x - pondRadius * 0.3f, pondCenter.y - pondRadius * 0.25f)
            )
        }
    }
}

// Draw stream terrain - a bubbling brook with rocks and vegetation
private fun DrawScope.drawStreamTerrain(center: Offset, terrainSize: Float, seed: Int, params: StreamParamsDto? = null, hasLake: Boolean = false, neighborElevations: NeighborElevations? = null, currentElevation: Float = 0f, neighborRivers: NeighborRivers? = null) {
    val random = kotlin.random.Random(seed)
    val streamColor = Color(0xFF4A8AAA) // Main stream
    val shallowColor = Color(0xFF6AAACC) // Shallow/foam
    val highlightColor = Color(0xFF8ACAEE) // Sparkles
    val bankColor = Color(0xFF6A7A6A) // Muddy bank
    val widthMultiplier = params?.widthMultiplier ?: 1f

    if (hasLake) {
        // When there's a lake, draw inlet streams flowing INTO the lake from edges
        val inletCount = 2 + random.nextInt(2)
        repeat(inletCount) { i ->
            val inletRandom = kotlin.random.Random(seed + i * 100)
            val angle = (i.toFloat() / inletCount) * 2 * PI.toFloat() + inletRandom.nextFloat() * 0.8f

            val startDist = terrainSize * 0.48f
            val startX = center.x + cos(angle) * startDist
            val startY = center.y + sin(angle) * startDist
            val endDist = terrainSize * 0.25f
            val endX = center.x + cos(angle) * endDist
            val endY = center.y + sin(angle) * endDist

            val midDist = (startDist + endDist) / 2
            val perpAngle = angle + PI.toFloat() / 2
            val curve = (inletRandom.nextFloat() - 0.5f) * terrainSize * 0.15f
            val midX = center.x + cos(angle) * midDist + cos(perpAngle) * curve
            val midY = center.y + sin(angle) * midDist + sin(perpAngle) * curve

            val inletPath = Path().apply {
                moveTo(startX, startY)
                quadraticTo(midX, midY, endX, endY)
            }

            drawPath(inletPath, color = bankColor.copy(alpha = 0.4f), style = Stroke(width = 6f * widthMultiplier, cap = StrokeCap.Round))
            drawPath(inletPath, color = streamColor, style = Stroke(width = 4f * widthMultiplier, cap = StrokeCap.Round))
            drawPath(inletPath, color = highlightColor.copy(alpha = 0.3f), style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        }
    } else {
        // Normal stream (no lake) - flows through the tile
        val deepWater = Color(0xFF2A5A7A)
        val rockColor = Color(0xFF5A6A6A)
        val mossColor = Color(0xFF4A7A5A)

        val entryX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.35f
        val entryY = center.y - terrainSize * 0.45f
        val exitX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.35f
        val exitY = center.y + terrainSize * 0.45f

        val numPoints = 6
        val streamPoints = mutableListOf<Offset>()
        streamPoints.add(Offset(entryX, entryY))

        for (i in 1 until numPoints - 1) {
            val t = i.toFloat() / (numPoints - 1)
            val baseX = entryX + (exitX - entryX) * t
            val baseY = entryY + (exitY - entryY) * t
            val meander = kotlin.math.sin(t * PI.toFloat() * 2f) * terrainSize * 0.15f
            val noise = (random.nextFloat() - 0.5f) * terrainSize * 0.08f
            streamPoints.add(Offset(baseX + meander + noise, baseY))
        }
        streamPoints.add(Offset(exitX, exitY))

        val streamPath = Path().apply {
            moveTo(streamPoints.first().x, streamPoints.first().y)
            for (i in 1 until streamPoints.size) {
                val prev = streamPoints[i - 1]
                val curr = streamPoints[i]
                quadraticTo(
                    (prev.x + curr.x) / 2 + (random.nextFloat() - 0.5f) * 4f,
                    (prev.y + curr.y) / 2,
                    curr.x, curr.y
                )
            }
        }

        drawPath(streamPath, color = bankColor.copy(alpha = 0.5f),
            style = Stroke(width = 7f * widthMultiplier, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(streamPath, color = streamColor,
            style = Stroke(width = 4.5f * widthMultiplier, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(streamPath, color = deepWater,
            style = Stroke(width = 2f * widthMultiplier, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Add rocks
        val rockCount = 4 + random.nextInt(4)
        repeat(rockCount) { r ->
            val t = random.nextFloat()
            val idx = (t * (streamPoints.size - 1)).toInt().coerceIn(0, streamPoints.size - 2)
            val pt1 = streamPoints[idx]
            val pt2 = streamPoints[idx + 1]
            val rockX = pt1.x + (pt2.x - pt1.x) * (t * (streamPoints.size - 1) - idx)
            val rockY = pt1.y + (pt2.y - pt1.y) * (t * (streamPoints.size - 1) - idx)
            val offset = (random.nextFloat() - 0.5f) * terrainSize * 0.08f
            val rockSize = terrainSize * (0.015f + random.nextFloat() * 0.02f)
            val finalX = rockX + offset

            drawCircle(color = rockColor, radius = rockSize, center = Offset(finalX, rockY))
            if (random.nextFloat() > 0.5f) {
                drawCircle(color = mossColor.copy(alpha = 0.6f), radius = rockSize * 0.6f,
                    center = Offset(finalX - rockSize * 0.2f, rockY - rockSize * 0.2f))
            }
            drawCircle(color = shallowColor.copy(alpha = 0.4f), radius = rockSize * 1.3f,
                center = Offset(finalX, rockY + rockSize * 0.3f))
        }

        // Add sparkle highlights
        repeat(3) {
            val t = 0.2f + random.nextFloat() * 0.6f
            val idx = (t * (streamPoints.size - 1)).toInt().coerceIn(0, streamPoints.size - 1)
            val pt = streamPoints[idx]
            drawCircle(color = highlightColor.copy(alpha = 0.7f), radius = 1.5f,
                center = Offset(pt.x + (random.nextFloat() - 0.5f) * 4f, pt.y))
        }
    }
}

// Draw small foothills leading up to mountains - gentle rolling hills with vegetation
private fun DrawScope.drawFoothillsTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val footHillCount = 6 + random.nextInt(4) // Multiple small foothills

    // Foothill colors - earthy greens and browns
    val hillGreen1 = Color(0xFF4A6A4A) // Dark green
    val hillGreen2 = Color(0xFF5A7A5A) // Medium green
    val hillGreen3 = Color(0xFF6A8A6A) // Light green
    val hillBrown = Color(0xFF7A6A5A)  // Earthy brown for dry areas
    val shadowColor = Color(0xFF3A4A3A) // Shadow

    // Draw foothills from back to front
    data class FoothillData(val x: Float, val y: Float, val width: Float, val height: Float, val color: Color)
    val foothills = mutableListOf<FoothillData>()

    repeat(footHillCount) { i ->
        val angle = (i.toFloat() / footHillCount) * 2 * PI.toFloat() + random.nextFloat() * 0.5f
        val distance = terrainSize * (0.25f + random.nextFloat() * 0.2f)
        val hillX = center.x + cos(angle) * distance
        val hillY = center.y + sin(angle) * distance
        val hillWidth = terrainSize * (0.12f + random.nextFloat() * 0.1f)
        val hillHeight = terrainSize * (0.04f + random.nextFloat() * 0.04f) // Small hills

        // Vary color based on position using noise
        val colorNoise = SimplexNoise.noise2DNormalized(hillX * 0.05f + seed, hillY * 0.05f)
        val hillColor = when {
            colorNoise < 0.25f -> hillBrown
            colorNoise < 0.5f -> hillGreen1
            colorNoise < 0.75f -> hillGreen2
            else -> hillGreen3
        }

        foothills.add(FoothillData(hillX, hillY, hillWidth, hillHeight, hillColor))
    }

    // Sort by Y (back to front)
    foothills.sortBy { it.y }

    // Draw each foothill
    for (hill in foothills) {
        // Draw shadow first
        drawOval(
            color = shadowColor.copy(alpha = 0.2f),
            topLeft = Offset(hill.x - hill.width * 0.5f + hill.height * 0.3f, hill.y - hill.height * 0.1f),
            size = androidx.compose.ui.geometry.Size(hill.width, hill.height * 0.5f)
        )

        // Draw foothill as gentle rounded shape
        val hillPath = Path().apply {
            moveTo(hill.x - hill.width * 0.5f, hill.y)
            // Gentle curve up
            quadraticTo(
                hill.x - hill.width * 0.25f, hill.y - hill.height * 0.6f,
                hill.x, hill.y - hill.height
            )
            // Gentle curve down
            quadraticTo(
                hill.x + hill.width * 0.25f, hill.y - hill.height * 0.5f,
                hill.x + hill.width * 0.5f, hill.y
            )
            close()
        }
        drawPath(hillPath, color = hill.color.copy(alpha = 0.7f))

        // Add subtle highlight on top
        val highlightPath = Path().apply {
            moveTo(hill.x - hill.width * 0.3f, hill.y - hill.height * 0.5f)
            quadraticTo(
                hill.x, hill.y - hill.height * 1.05f,
                hill.x + hill.width * 0.2f, hill.y - hill.height * 0.6f
            )
        }
        drawPath(highlightPath, color = Color.White.copy(alpha = 0.15f), style = Stroke(width = 1.5f))

        // Add tiny vegetation dots on some hills
        if (random.nextFloat() > 0.5f) {
            repeat(3 + random.nextInt(3)) {
                val dotX = hill.x + (random.nextFloat() - 0.5f) * hill.width * 0.6f
                val dotY = hill.y - hill.height * (0.3f + random.nextFloat() * 0.5f)
                val dotSize = terrainSize * (0.003f + random.nextFloat() * 0.004f)
                drawCircle(color = hillGreen1.copy(alpha = 0.5f), radius = dotSize, center = Offset(dotX, dotY))
            }
        }
    }
}

// Draw mountain terrain - layered mountain range with depth and atmospheric perspective
private fun DrawScope.drawMountainTerrain(center: Offset, terrainSize: Float, seed: Int, params: MountainParamsDto? = null) {
    // Draw foothills first (behind mountains)
    drawFoothillsTerrain(center, terrainSize, seed + 100)

    val random = kotlin.random.Random(seed)
    val basePeakCount = 2 + random.nextInt(2)
    val peakCount = params?.peakCount ?: basePeakCount
    val heightMultiplier = params?.heightMultiplier ?: 1f

    // Colors for atmospheric depth
    val farMountain = Color(0xFF7A8A9A) // Distant/hazy
    val midMountain = Color(0xFF5A6A7A) // Mid-distance
    val nearMountain = Color(0xFF4A5A6A) // Close/detailed
    val shadowColor = Color(0xFF3A4A5A) // Shadow side
    val highlightColor = Color(0xFF8A9AAA) // Sunlit side
    val snowColor = Color(0xFFE8F0F8) // Snow with slight blue tint
    val rockColor = Color(0xFF5A5A5A) // Exposed rock

    // Generate mountain data with depth layers
    data class MountainData(val x: Float, val baseY: Float, val height: Float, val width: Float, val style: Int, val layer: Int)
    val mountains = mutableListOf<MountainData>()

    // Create 2-3 layers of mountains (back to front)
    val numLayers = 2 + random.nextInt(2)
    for (layer in 0 until numLayers) {
        val layerPeaks = if (layer == numLayers - 1) peakCount else 1 + random.nextInt(2)
        val layerBaseY = center.y + terrainSize * (0.25f - layer * 0.08f)

        repeat(layerPeaks) { i ->
            val offsetX = (i - layerPeaks / 2f) * terrainSize * 0.3f + (random.nextFloat() - 0.5f) * terrainSize * 0.15f
            val layerScale = 0.6f + layer * 0.2f // Back mountains smaller
            val peakHeight = terrainSize * (0.25f + random.nextFloat() * 0.2f) * heightMultiplier * layerScale
            val peakWidth = terrainSize * (0.15f + random.nextFloat() * 0.12f) * layerScale
            val style = random.nextInt(4)

            mountains.add(MountainData(center.x + offsetX, layerBaseY, peakHeight, peakWidth, style, layer))
        }
    }

    // Sort by layer (back to front)
    mountains.sortBy { it.layer }

    // Draw each mountain
    for (mtn in mountains) {
        val peakX = mtn.x
        val baseY = mtn.baseY
        val peakHeight = mtn.height
        val peakWidth = mtn.width
        val style = mtn.style
        val layer = mtn.layer

        // Color based on layer (atmospheric perspective)
        val baseColor = when (layer) {
            0 -> farMountain
            1 -> midMountain
            else -> nearMountain
        }

        // Create mountain silhouette path
        val mountainPath = Path().apply {
            when (style) {
                0 -> {
                    // Sharp pointed peak
                    moveTo(peakX - peakWidth, baseY)
                    lineTo(peakX - peakWidth * 0.1f, baseY - peakHeight * 0.7f)
                    lineTo(peakX, baseY - peakHeight)
                    lineTo(peakX + peakWidth * 0.15f, baseY - peakHeight * 0.75f)
                    lineTo(peakX + peakWidth, baseY)
                    close()
                }
                1 -> {
                    // Rounded/dome peak with ridges
                    moveTo(peakX - peakWidth, baseY)
                    quadraticTo(peakX - peakWidth * 0.6f, baseY - peakHeight * 0.5f, peakX - peakWidth * 0.2f, baseY - peakHeight * 0.85f)
                    quadraticTo(peakX, baseY - peakHeight * 1.05f, peakX + peakWidth * 0.15f, baseY - peakHeight * 0.9f)
                    quadraticTo(peakX + peakWidth * 0.5f, baseY - peakHeight * 0.6f, peakX + peakWidth, baseY)
                    close()
                }
                2 -> {
                    // Jagged/craggy peak
                    moveTo(peakX - peakWidth, baseY)
                    lineTo(peakX - peakWidth * 0.7f, baseY - peakHeight * 0.4f)
                    lineTo(peakX - peakWidth * 0.5f, baseY - peakHeight * 0.35f)
                    lineTo(peakX - peakWidth * 0.3f, baseY - peakHeight * 0.7f)
                    lineTo(peakX - peakWidth * 0.1f, baseY - peakHeight * 0.65f)
                    lineTo(peakX, baseY - peakHeight)
                    lineTo(peakX + peakWidth * 0.2f, baseY - peakHeight * 0.8f)
                    lineTo(peakX + peakWidth * 0.4f, baseY - peakHeight * 0.6f)
                    lineTo(peakX + peakWidth * 0.6f, baseY - peakHeight * 0.5f)
                    lineTo(peakX + peakWidth, baseY)
                    close()
                }
                else -> {
                    // Asymmetric with cliff face
                    moveTo(peakX - peakWidth * 0.8f, baseY)
                    lineTo(peakX - peakWidth * 0.3f, baseY - peakHeight * 0.5f)
                    lineTo(peakX, baseY - peakHeight)
                    lineTo(peakX + peakWidth * 0.1f, baseY - peakHeight * 0.95f)
                    lineTo(peakX + peakWidth * 0.5f, baseY - peakHeight * 0.3f)
                    lineTo(peakX + peakWidth, baseY)
                    close()
                }
            }
        }

        // Draw mountain base fill
        drawPath(mountainPath, color = baseColor)

        // Add shadow side (right side darker) for front mountains
        if (layer >= numLayers - 2) {
            val shadowPath = Path().apply {
                moveTo(peakX, baseY - peakHeight)
                lineTo(peakX + peakWidth * 0.1f, baseY - peakHeight * 0.85f)
                lineTo(peakX + peakWidth * 0.6f, baseY - peakHeight * 0.3f)
                lineTo(peakX + peakWidth, baseY)
                lineTo(peakX, baseY)
                close()
            }
            drawPath(shadowPath, color = shadowColor.copy(alpha = 0.4f))

            // Add highlight on left face
            val highlightPath = Path().apply {
                moveTo(peakX - peakWidth * 0.5f, baseY)
                lineTo(peakX - peakWidth * 0.2f, baseY - peakHeight * 0.6f)
                lineTo(peakX, baseY - peakHeight)
                lineTo(peakX - peakWidth * 0.1f, baseY - peakHeight * 0.7f)
                lineTo(peakX - peakWidth * 0.3f, baseY)
                close()
            }
            drawPath(highlightPath, color = highlightColor.copy(alpha = 0.25f))
        }

        // Snow cap for taller peaks (more snow on front mountains)
        val snowThreshold = if (layer == numLayers - 1) 0.25f else 0.35f
        if (peakHeight > terrainSize * snowThreshold) {
            val snowLine = baseY - peakHeight * (0.65f + layer * 0.05f)
            val snowPath = Path().apply {
                moveTo(peakX - peakWidth * 0.35f, snowLine)
                // Irregular snow line
                quadraticTo(peakX - peakWidth * 0.2f, snowLine + peakHeight * 0.05f, peakX - peakWidth * 0.1f, snowLine - peakHeight * 0.02f)
                lineTo(peakX, baseY - peakHeight)
                lineTo(peakX + peakWidth * 0.1f, snowLine - peakHeight * 0.03f)
                quadraticTo(peakX + peakWidth * 0.2f, snowLine + peakHeight * 0.03f, peakX + peakWidth * 0.35f, snowLine)
                close()
            }
            drawPath(snowPath, color = snowColor)

            // Add some exposed rock patches in snow
            if (layer == numLayers - 1 && random.nextFloat() > 0.5f) {
                val rockX = peakX + (random.nextFloat() - 0.5f) * peakWidth * 0.3f
                val rockY = baseY - peakHeight * 0.8f
                drawCircle(color = rockColor, radius = terrainSize * 0.015f, center = Offset(rockX, rockY))
            }
        }

        // Ridge lines for detail on front mountains
        if (layer == numLayers - 1) {
            repeat(2) { r ->
                val ridgeX = peakX + (r - 0.5f) * peakWidth * 0.4f
                val ridgePath = Path().apply {
                    moveTo(ridgeX, baseY - peakHeight * (0.5f + random.nextFloat() * 0.3f))
                    lineTo(ridgeX + peakWidth * 0.1f, baseY)
                }
                drawPath(ridgePath, color = shadowColor.copy(alpha = 0.2f), style = Stroke(width = 1f))
            }
        }

        // Foothills/base rocks for front mountains
        if (layer == numLayers - 1 && random.nextFloat() > 0.4f) {
            repeat(2) {
                val cragX = peakX + (random.nextFloat() - 0.5f) * peakWidth * 1.2f
                val cragSize = terrainSize * (0.03f + random.nextFloat() * 0.02f)
                val cragPath = Path().apply {
                    moveTo(cragX - cragSize, baseY)
                    lineTo(cragX - cragSize * 0.3f, baseY - cragSize * 1.2f)
                    lineTo(cragX + cragSize * 0.2f, baseY - cragSize * 0.8f)
                    lineTo(cragX + cragSize, baseY)
                    close()
                }
                drawPath(cragPath, color = nearMountain.copy(alpha = 0.6f))
            }
        }
    }

    // Add scattered boulders at mountain base
    repeat(4 + random.nextInt(3)) { b ->
        val bRandom = kotlin.random.Random(seed + 200 + b)
        val boulderAngle = bRandom.nextFloat() * 2 * PI.toFloat()
        val boulderDist = terrainSize * (0.3f + bRandom.nextFloat() * 0.15f)
        val boulderX = center.x + cos(boulderAngle) * boulderDist
        val boulderY = center.y + sin(boulderAngle) * boulderDist * 0.5f + terrainSize * 0.15f
        drawBoulder(boulderX, boulderY, terrainSize * (0.025f + bRandom.nextFloat() * 0.03f), bRandom.nextInt())
    }
}

// Draw grass terrain - lush meadow with varied grass, dirt patches, and flowers
private fun DrawScope.drawGrassTerrain(center: Offset, terrainSize: Float, seed: Int, params: GrassParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val baseTuftCount = 12 + random.nextInt(6) // More grass tufts
    val tuftCount = params?.tuftCount ?: baseTuftCount

    // Simplex noise parameters for natural grass distribution
    val noiseScale = 0.05f
    val noiseSeed = seed * 0.1f

    // Base grass color variations (lush green)
    val grassDarkBase = Color(0xFF2A6A2A) // Dark lush grass
    val grassMidBase = Color(0xFF3A9A3A) // Medium lush grass
    val grassLightBase = Color(0xFF4ABA4A) // Light lush grass
    val grassTipBase = Color(0xFF6ADA6A) // Bright sunlit tips

    // Alternative biome colors for blending (dry/autumn grass - more yellow/brown)
    val grassDryDark = Color(0xFF7A6A2A)  // Brown-yellow dark
    val grassDryMid = Color(0xFFAA8A3A)   // Golden mid
    val grassDryLight = Color(0xFFCCAA4A) // Bright yellow-gold

    // Dirt colors for ground texture
    val dirtDark = Color(0xFF5A4A3A)   // Dark earth
    val dirtMid = Color(0xFF7A6A5A)    // Medium brown dirt
    val dirtLight = Color(0xFF9A8A7A)  // Light dusty dirt

    val flowerColors = listOf(
        Color(0xFFEAEA5A), // Yellow
        Color(0xFFFFFFFF), // White
        Color(0xFFBA7ABA), // Purple
        Color(0xFFEA7A7A)  // Pink/red
    )

    // LAYER 1: Ground texture with dirt patches and gradation
    // Draw base ground texture using noise for natural variation
    repeat(15) { i ->
        val patchAngle = random.nextFloat() * 2 * PI.toFloat()
        val patchDist = random.nextFloat() * terrainSize * 0.45f
        val patchX = center.x + cos(patchAngle) * patchDist
        val patchY = center.y + sin(patchAngle) * patchDist

        // Use noise to determine if this is a dirt patch or grass patch
        val groundNoise = SimplexNoise.noise2DNormalized(patchX * 0.08f + seed, patchY * 0.08f)
        val isDirt = groundNoise < 0.35f

        if (isDirt) {
            // Draw irregular dirt patch
            val patchSize = terrainSize * (0.03f + random.nextFloat() * 0.04f)
            val dirtColor = when {
                groundNoise < 0.15f -> dirtDark
                groundNoise < 0.25f -> dirtMid
                else -> dirtLight
            }
            // Draw elongated oval for more natural shape
            val stretchX = 0.8f + random.nextFloat() * 0.4f
            val stretchY = 0.8f + random.nextFloat() * 0.4f
            drawOval(
                color = dirtColor.copy(alpha = 0.3f + random.nextFloat() * 0.2f),
                topLeft = Offset(patchX - patchSize * stretchX, patchY - patchSize * stretchY),
                size = androidx.compose.ui.geometry.Size(patchSize * 2 * stretchX, patchSize * 2 * stretchY)
            )
        }
    }

    // LAYER 2: Small pebbles and ground detail in dirt areas
    repeat(8) {
        val pebbleX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.7f
        val pebbleY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.7f
        val groundNoise = SimplexNoise.noise2DNormalized(pebbleX * 0.08f + seed, pebbleY * 0.08f)
        if (groundNoise < 0.4f) { // Only in dirt-ish areas
            val pebbleSize = terrainSize * (0.005f + random.nextFloat() * 0.008f)
            val pebbleColor = if (random.nextBoolean()) dirtDark else dirtMid
            drawCircle(color = pebbleColor.copy(alpha = 0.4f), radius = pebbleSize, center = Offset(pebbleX, pebbleY))
        }
    }

    // LAYER 3: Scattered grass tufts with Simplex noise for natural clustering
    repeat(tuftCount) { t ->
        val baseAngle = (t.toFloat() / tuftCount) * 2 * PI.toFloat()

        // Use Simplex noise to offset positions for organic clustering
        val noiseX = cos(baseAngle) * noiseScale * 10 + noiseSeed
        val noiseY = sin(baseAngle) * noiseScale * 10 + noiseSeed
        val clusterNoise = SimplexNoise.noise2D(noiseX, noiseY)
        val distanceNoise = SimplexNoise.noise2D(noiseX + 100f, noiseY + 100f)

        val angle = baseAngle + clusterNoise * 0.8f
        val distance = terrainSize * (0.12f + (distanceNoise + 1f) * 0.22f + random.nextFloat() * 0.1f)
        val tuftX = center.x + cos(angle) * distance
        val tuftY = center.y + sin(angle) * distance

        // Use Voronoi biome blending for color variation across the terrain
        val localX = tuftX - center.x + terrainSize * 0.5f
        val localY = tuftY - center.y + terrainSize * 0.5f

        // Blend between lush and dry grass based on Voronoi cells
        val grassDark = BiomeBlending.blendColors(grassDarkBase, grassDryDark, localX, localY, terrainSize, seed)
        val grassMid = BiomeBlending.blendColors(grassMidBase, grassDryMid, localX, localY, terrainSize, seed)
        val grassLight = BiomeBlending.blendColors(grassLightBase, grassDryLight, localX, localY, terrainSize, seed)

        // Use noise for height variation - creates natural looking patches
        // Also factor in biome density - drier areas have shorter grass
        val heightNoise = SimplexNoise.noise2DNormalized(tuftX * noiseScale, tuftY * noiseScale)
        val densityMultiplier = BiomeBlending.getDensityMultiplier(localX, localY, terrainSize, seed)
        val tuftHeight = terrainSize * (0.025f + heightNoise * 0.045f) * (0.8f + densityMultiplier * 0.2f)

        // Each tuft has 4-6 blades
        val bladeCount = 4 + random.nextInt(3)
        repeat(bladeCount) { b ->
            val bladeSpread = (b - bladeCount / 2f) * 0.25f
            val bladeAngle = -PI.toFloat() / 2 + bladeSpread + (random.nextFloat() - 0.5f) * 0.3f
            val bladeHeight = tuftHeight * (0.7f + random.nextFloat() * 0.4f)
            val bladeCurve = (random.nextFloat() - 0.5f) * terrainSize * 0.012f

            // Color varies by height position
            val bladeColor = when {
                b == bladeCount / 2 -> grassLight
                random.nextFloat() > 0.7f -> grassTipBase
                else -> grassMid
            }

            // Draw curved blade
            val bladePath = Path().apply {
                moveTo(tuftX, tuftY)
                val endX = tuftX + cos(bladeAngle) * bladeHeight + bladeCurve
                val endY = tuftY + sin(bladeAngle) * bladeHeight
                quadraticTo(
                    tuftX + cos(bladeAngle) * bladeHeight * 0.5f + bladeCurve * 0.3f,
                    tuftY + sin(bladeAngle) * bladeHeight * 0.5f,
                    endX, endY
                )
            }
            drawPath(bladePath, color = bladeColor.copy(alpha = 0.7f + random.nextFloat() * 0.3f),
                style = Stroke(width = 1.2f, cap = StrokeCap.Round))
        }

        // Occasionally add small flowers
        if (random.nextFloat() > 0.88f) {
            val flowerColor = flowerColors[random.nextInt(flowerColors.size)]
            val flowerX = tuftX + (random.nextFloat() - 0.5f) * terrainSize * 0.025f
            val flowerY = tuftY - tuftHeight * 0.8f
            drawCircle(color = flowerColor, radius = 1.5f, center = Offset(flowerX, flowerY))
        }
    }

    // LAYER 4: Additional scattered short grass for density
    repeat(tuftCount / 2) {
        val shortX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.6f
        val shortY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.6f
        val localX = shortX - center.x + terrainSize * 0.5f
        val localY = shortY - center.y + terrainSize * 0.5f
        val shortGrassColor = BiomeBlending.blendColors(grassMidBase, grassDryMid, localX, localY, terrainSize, seed)

        // 2-3 very short blades
        repeat(2 + random.nextInt(2)) {
            val bladeAngle = -PI.toFloat() / 2 + (random.nextFloat() - 0.5f) * 0.6f
            val bladeHeight = terrainSize * (0.015f + random.nextFloat() * 0.015f)
            val bladePath = Path().apply {
                moveTo(shortX, shortY)
                lineTo(shortX + cos(bladeAngle) * bladeHeight, shortY + sin(bladeAngle) * bladeHeight)
            }
            drawPath(bladePath, color = shortGrassColor.copy(alpha = 0.5f + random.nextFloat() * 0.3f),
                style = Stroke(width = 1f, cap = StrokeCap.Round))
        }
    }

    // LAYER 5: Clover/moss patches with gradation
    repeat(5) {
        val patchX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.5f
        val patchY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.5f
        val localPatchX = patchX - center.x + terrainSize * 0.5f
        val localPatchY = patchY - center.y + terrainSize * 0.5f
        val patchColor = BiomeBlending.blendColors(grassDarkBase, grassDryDark, localPatchX, localPatchY, terrainSize, seed)
        val patchSize = terrainSize * (0.025f + random.nextFloat() * 0.03f)

        // Draw with radial gradient effect (darker center, lighter edges)
        drawCircle(color = patchColor.copy(alpha = 0.25f), radius = patchSize, center = Offset(patchX, patchY))
        drawCircle(color = patchColor.copy(alpha = 0.15f), radius = patchSize * 1.3f, center = Offset(patchX, patchY))
    }

    // LAYER 6: Occasional small boulders
    if (random.nextFloat() > 0.5f) { // Only 50% of grass tiles get boulders
        repeat(1 + random.nextInt(2)) { b ->
            val bRandom = kotlin.random.Random(seed + 400 + b)
            val boulderX = center.x + (bRandom.nextFloat() - 0.5f) * terrainSize * 0.6f
            val boulderY = center.y + (bRandom.nextFloat() - 0.5f) * terrainSize * 0.6f
            drawBoulder(boulderX, boulderY, terrainSize * (0.015f + bRandom.nextFloat() * 0.015f), bRandom.nextInt())
        }
    }
}

// Draw building terrain - simple house shapes
private fun DrawScope.drawBuildingTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val buildingCount = 1 + random.nextInt(3)

    repeat(buildingCount) { i ->
        val angle = (i.toFloat() / buildingCount) * 2 * PI.toFloat() + random.nextFloat()
        val distance = terrainSize * (0.3f + random.nextFloat() * 0.15f)
        val bldgX = center.x + cos(angle) * distance
        val bldgY = center.y + sin(angle) * distance
        val bldgSize = terrainSize * (0.07f + random.nextFloat() * 0.03f)

        val housePath = Path().apply {
            moveTo(bldgX - bldgSize, bldgY)
            lineTo(bldgX - bldgSize, bldgY + bldgSize)
            lineTo(bldgX + bldgSize, bldgY + bldgSize)
            lineTo(bldgX + bldgSize, bldgY)
            lineTo(bldgX, bldgY - bldgSize * 0.7f)
            close()
        }

        drawPath(housePath, color = TerrainColors.building)
    }
}

// Draw cave terrain - dark semicircle entrance
private fun DrawScope.drawCaveTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val caveX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.25f
    val caveY = center.y + terrainSize * 0.25f

    drawArc(
        color = TerrainColors.cave,
        startAngle = 0f,
        sweepAngle = -180f,
        useCenter = true,
        topLeft = Offset(caveX - terrainSize * 0.12f, caveY - terrainSize * 0.08f),
        size = androidx.compose.ui.geometry.Size(terrainSize * 0.24f, terrainSize * 0.16f)
    )
}

// Draw desert terrain - rolling sand dunes with windswept details
private fun DrawScope.drawDesertTerrain(center: Offset, terrainSize: Float, seed: Int, params: DesertParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val duneCount = params?.duneCount ?: 3
    val heightMultiplier = params?.heightMultiplier ?: 1f

    // Simplex noise parameters for natural dune variation
    val noiseScale = 0.03f
    val noiseSeed = seed * 0.1f

    // Desert colors
    val sandLight = Color(0xFFEAD8B0) // Sunlit sand
    val sandMid = Color(0xFFD0C090) // Middle tone
    val sandDark = Color(0xFFB0A070) // Shadow sand
    val sandHighlight = Color(0xFFFAE8C0) // Bright highlights
    val rockColor = Color(0xFF8A7A6A) // Desert rocks

    // Draw layered dunes from back to front with Simplex noise variation
    repeat(duneCount) { i ->
        // Use Simplex noise for natural dune positioning and size variation
        val duneNoise = SimplexNoise.noise2D(noiseSeed + i * 2f, noiseSeed + i * 3f)
        val offsetY = (duneCount - 1 - i) * terrainSize * 0.12f - terrainSize * 0.1f
        val duneHeight = terrainSize * (0.1f + i * 0.03f + duneNoise * 0.02f) * heightMultiplier
        val duneWidth = terrainSize * (0.55f + (duneNoise + 1f) * 0.15f)
        val duneX = center.x + duneNoise * terrainSize * 0.15f

        // Windward (gentle) slope
        val windwardPath = Path().apply {
            moveTo(duneX - duneWidth * 0.5f, center.y + offsetY + duneHeight * 0.3f)
            quadraticTo(
                duneX - duneWidth * 0.1f, center.y + offsetY - duneHeight,
                duneX + duneWidth * 0.15f, center.y + offsetY - duneHeight * 0.9f
            )
            lineTo(duneX + duneWidth * 0.15f, center.y + offsetY + duneHeight * 0.3f)
            close()
        }
        drawPath(windwardPath, color = if (i == duneCount - 1) sandLight else sandMid.copy(alpha = 0.7f + i * 0.1f))

        // Slip face (steep shadow side)
        val slipPath = Path().apply {
            moveTo(duneX + duneWidth * 0.15f, center.y + offsetY - duneHeight * 0.9f)
            lineTo(duneX + duneWidth * 0.45f, center.y + offsetY + duneHeight * 0.2f)
            lineTo(duneX + duneWidth * 0.15f, center.y + offsetY + duneHeight * 0.3f)
            close()
        }
        drawPath(slipPath, color = sandDark.copy(alpha = 0.5f + i * 0.1f))

        // Ridge line (sharp crest)
        val ridgePath = Path().apply {
            moveTo(duneX - duneWidth * 0.3f, center.y + offsetY - duneHeight * 0.5f)
            quadraticTo(
                duneX, center.y + offsetY - duneHeight * 1.05f,
                duneX + duneWidth * 0.15f, center.y + offsetY - duneHeight * 0.9f
            )
        }
        drawPath(ridgePath, color = sandHighlight.copy(alpha = 0.6f),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round))

        // Wind ripples on front dunes with Simplex noise for natural variation
        if (i == duneCount - 1) {
            repeat(5) { r ->
                val rippleNoise = SimplexNoise.noise2D(noiseSeed + r * 5f, noiseSeed + r * 7f)
                val rippleY = center.y + offsetY - duneHeight * (0.25f + r * 0.14f + rippleNoise * 0.03f)
                val rippleOffset = r * terrainSize * 0.018f + rippleNoise * terrainSize * 0.01f
                val rippleCurve = SimplexNoise.noise2D(noiseSeed + r * 10f, noiseSeed) * terrainSize * 0.012f
                val ripplePath = Path().apply {
                    moveTo(duneX - duneWidth * 0.38f + rippleOffset, rippleY)
                    quadraticTo(
                        duneX - duneWidth * 0.1f + rippleOffset, rippleY - terrainSize * 0.012f + rippleCurve,
                        duneX + duneWidth * 0.12f + rippleOffset, rippleY + rippleCurve * 0.5f
                    )
                }
                drawPath(ripplePath, color = sandDark.copy(alpha = 0.18f - r * 0.025f),
                    style = Stroke(width = 0.7f + rippleNoise * 0.2f))
            }
        }
    }

    // Scattered rocks
    repeat(3) {
        val rockX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.5f
        val rockY = center.y + random.nextFloat() * terrainSize * 0.3f
        val rockSize = terrainSize * (0.02f + random.nextFloat() * 0.02f)

        val rockPath = Path().apply {
            moveTo(rockX - rockSize, rockY)
            lineTo(rockX - rockSize * 0.3f, rockY - rockSize * 0.8f)
            lineTo(rockX + rockSize * 0.5f, rockY - rockSize * 0.5f)
            lineTo(rockX + rockSize, rockY)
            close()
        }
        drawPath(rockPath, color = rockColor)
    }

    // Sand scatter/texture
    repeat(12) {
        val dotX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.7f
        val dotY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.5f
        val dotSize = 1f + random.nextFloat() * 1.5f
        drawCircle(color = sandMid.copy(alpha = 0.3f), radius = dotSize, center = Offset(dotX, dotY))
    }

    // Maybe add a small cactus or dead shrub
    if (random.nextFloat() > 0.6f) {
        val cactusX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.4f
        val cactusY = center.y + terrainSize * 0.15f
        val cactusHeight = terrainSize * 0.08f

        // Main stem
        drawLine(color = Color(0xFF4A6A4A), start = Offset(cactusX, cactusY),
            end = Offset(cactusX, cactusY - cactusHeight), strokeWidth = 3f, cap = StrokeCap.Round)
        // Arms
        drawLine(color = Color(0xFF4A6A4A), start = Offset(cactusX, cactusY - cactusHeight * 0.6f),
            end = Offset(cactusX - cactusHeight * 0.3f, cactusY - cactusHeight * 0.8f), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFF4A6A4A), start = Offset(cactusX, cactusY - cactusHeight * 0.4f),
            end = Offset(cactusX + cactusHeight * 0.25f, cactusY - cactusHeight * 0.55f), strokeWidth = 2f, cap = StrokeCap.Round)
    }
}

// Draw a single boulder with natural irregular shape and shading
private fun DrawScope.drawBoulder(x: Float, y: Float, size: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    // Boulder colors
    val boulderDark = Color(0xFF5A5A5A)   // Dark gray
    val boulderMid = Color(0xFF7A7A7A)    // Medium gray
    val boulderLight = Color(0xFF9A9A9A)  // Light gray
    val boulderHighlight = Color(0xFFB0B0B0) // Highlight

    // Create irregular boulder shape using noise-based control points
    val numPoints = 6 + random.nextInt(3)
    val points = (0 until numPoints).map { i ->
        val angle = (i.toFloat() / numPoints) * 2 * PI.toFloat()
        val radiusVariation = 0.7f + random.nextFloat() * 0.5f
        val px = x + cos(angle) * size * radiusVariation
        val py = y + sin(angle) * size * radiusVariation * 0.7f // Flatten vertically
        Offset(px, py)
    }

    // Draw shadow
    val shadowPath = Path().apply {
        moveTo(points[0].x + size * 0.15f, points[0].y + size * 0.1f)
        for (i in points.indices) {
            val curr = points[i]
            val next = points[(i + 1) % points.size]
            quadraticTo(
                curr.x + size * 0.15f, curr.y + size * 0.1f,
                (curr.x + next.x) / 2 + size * 0.15f, (curr.y + next.y) / 2 + size * 0.1f
            )
        }
        close()
    }
    drawPath(shadowPath, color = Color(0xFF3A3A3A).copy(alpha = 0.3f))

    // Draw main boulder body
    val boulderPath = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in points.indices) {
            val curr = points[i]
            val next = points[(i + 1) % points.size]
            quadraticTo(curr.x, curr.y, (curr.x + next.x) / 2, (curr.y + next.y) / 2)
        }
        close()
    }
    drawPath(boulderPath, color = boulderMid)

    // Add darker lower portion for grounding
    val lowerPath = Path().apply {
        moveTo(x - size * 0.8f, y)
        quadraticTo(x - size * 0.4f, y + size * 0.3f, x, y + size * 0.2f)
        quadraticTo(x + size * 0.4f, y + size * 0.3f, x + size * 0.8f, y)
        lineTo(x + size * 0.6f, y - size * 0.1f)
        lineTo(x - size * 0.6f, y - size * 0.1f)
        close()
    }
    drawPath(lowerPath, color = boulderDark.copy(alpha = 0.5f))

    // Add highlight on upper left
    val highlightPath = Path().apply {
        moveTo(x - size * 0.3f, y - size * 0.4f)
        quadraticTo(x - size * 0.5f, y - size * 0.3f, x - size * 0.4f, y - size * 0.1f)
        quadraticTo(x - size * 0.2f, y - size * 0.35f, x, y - size * 0.45f)
        close()
    }
    drawPath(highlightPath, color = boulderHighlight.copy(alpha = 0.4f))

    // Add small surface details (cracks/texture)
    if (size > 3f) {
        repeat(2 + random.nextInt(2)) {
            val crackX = x + (random.nextFloat() - 0.5f) * size * 0.8f
            val crackY = y + (random.nextFloat() - 0.5f) * size * 0.5f
            val crackLen = size * (0.15f + random.nextFloat() * 0.2f)
            val crackAngle = random.nextFloat() * PI.toFloat()
            drawLine(
                color = boulderDark.copy(alpha = 0.3f),
                start = Offset(crackX, crackY),
                end = Offset(crackX + cos(crackAngle) * crackLen, crackY + sin(crackAngle) * crackLen * 0.5f),
                strokeWidth = 0.5f
            )
        }
    }
}

// Draw hills terrain - rolling hills with depth, grass, and atmospheric perspective
private fun DrawScope.drawHillsTerrain(center: Offset, terrainSize: Float, seed: Int, params: HillsParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val heightMultiplier = params?.heightMultiplier ?: 1f

    // Simplex noise parameters for natural hill variation
    val noiseScale = 0.04f
    val noiseSeed = seed * 0.1f

    // Colors for depth layers
    val farHillColor = Color(0xFF8BA888)   // Misty green
    val midHillColor = Color(0xFF7A9E76)   // Medium green
    val nearHillColor = Color(0xFF6B8F65)  // Rich green
    val shadowColor = Color(0xFF4A6A48)    // Dark shadow
    val highlightColor = Color(0xFFAAD4A5) // Light highlight

    // Draw ground base with subtle texture using Simplex noise
    drawCircle(
        color = Color(0xFFB8D4A8).copy(alpha = 0.3f),
        radius = terrainSize * 0.45f,
        center = center
    )

    // Create layered rolling hills (back to front for proper overlap)
    data class Hill(val x: Float, val y: Float, val width: Float, val height: Float, val layer: Int)
    val hills = mutableListOf<Hill>()

    // Far hills (smaller, lighter) with Simplex noise variation
    repeat(2 + random.nextInt(2)) { i ->
        val hillNoise = SimplexNoise.noise2D(noiseSeed + i * 3f, noiseSeed + i * 5f)
        val angle = (hillNoise + 1f) * PI.toFloat() / 2 - PI.toFloat() / 2 // Top half with noise
        val dist = terrainSize * (0.25f + (hillNoise + 1f) * 0.08f)
        hills.add(Hill(
            x = center.x + cos(angle) * dist * 0.8f,
            y = center.y - terrainSize * 0.15f + sin(angle) * dist * 0.3f,
            width = terrainSize * (0.24f + (hillNoise + 1f) * 0.06f),
            height = terrainSize * (0.07f + (hillNoise + 1f) * 0.025f) * heightMultiplier,
            layer = 0
        ))
    }

    // Mid hills with Simplex noise
    repeat(2 + random.nextInt(2)) { i ->
        val hillNoise = SimplexNoise.noise2D(noiseSeed + i * 7f + 50f, noiseSeed + i * 11f)
        val angle = (hillNoise + 1f) * PI.toFloat()
        val dist = terrainSize * (0.15f + (hillNoise + 1f) * 0.1f)
        hills.add(Hill(
            x = center.x + cos(angle) * dist,
            y = center.y + sin(angle) * dist * 0.5f,
            width = terrainSize * (0.18f + (hillNoise + 1f) * 0.07f),
            height = terrainSize * (0.09f + (hillNoise + 1f) * 0.035f) * heightMultiplier,
            layer = 1
        ))
    }

    // Near hills (larger, more detailed) with Simplex noise
    repeat(1 + random.nextInt(2)) { i ->
        val hillNoise = SimplexNoise.noise2D(noiseSeed + i * 13f + 100f, noiseSeed + i * 17f)
        val angle = (hillNoise + 1f) * PI.toFloat() / 2 + PI.toFloat() / 2 // Bottom half bias
        val dist = terrainSize * (0.1f + (hillNoise + 1f) * 0.08f)
        hills.add(Hill(
            x = center.x + cos(angle) * dist,
            y = center.y + terrainSize * 0.1f + (hillNoise + 1f) * terrainSize * 0.08f,
            width = terrainSize * (0.2f + (hillNoise + 1f) * 0.09f),
            height = terrainSize * (0.11f + (hillNoise + 1f) * 0.045f) * heightMultiplier,
            layer = 2
        ))
    }

    // Sort by layer then by Y position
    hills.sortWith(compareBy({ it.layer }, { it.y }))

    // Draw each hill
    hills.forEachIndexed { index, hill ->
        val hillColor = when (hill.layer) {
            0 -> farHillColor
            1 -> midHillColor
            else -> nearHillColor
        }
        val detailLevel = hill.layer

        // Draw hill body with organic shape
        val hillPath = Path().apply {
            moveTo(hill.x - hill.width, hill.y)
            // Left slope with slight irregularity
            val midLeftX = hill.x - hill.width * 0.5f
            val midLeftY = hill.y - hill.height * 0.7f
            quadraticTo(midLeftX - hill.width * 0.1f, midLeftY + hill.height * 0.2f,
                       hill.x - hill.width * 0.1f, hill.y - hill.height)
            // Peak area with gentle curve
            quadraticTo(hill.x + hill.width * 0.1f, hill.y - hill.height * 1.05f,
                       hill.x + hill.width * 0.3f, hill.y - hill.height * 0.8f)
            // Right slope
            quadraticTo(hill.x + hill.width * 0.7f, hill.y - hill.height * 0.3f,
                       hill.x + hill.width, hill.y)
            close()
        }

        // Fill hill body
        drawPath(hillPath, color = hillColor)

        // Add shadow on right side
        val shadowPath = Path().apply {
            moveTo(hill.x + hill.width * 0.2f, hill.y - hill.height * 0.85f)
            quadraticTo(hill.x + hill.width * 0.6f, hill.y - hill.height * 0.4f,
                       hill.x + hill.width, hill.y)
            lineTo(hill.x + hill.width * 0.3f, hill.y)
            close()
        }
        drawPath(shadowPath, color = shadowColor.copy(alpha = 0.25f))

        // Add highlight on left side
        val highlightPath = Path().apply {
            moveTo(hill.x - hill.width * 0.8f, hill.y - hill.height * 0.2f)
            quadraticTo(hill.x - hill.width * 0.3f, hill.y - hill.height * 0.9f,
                       hill.x, hill.y - hill.height)
            lineTo(hill.x - hill.width * 0.2f, hill.y - hill.height * 0.7f)
            close()
        }
        drawPath(highlightPath, color = highlightColor.copy(alpha = 0.3f))

        // Add grass tufts on near hills
        if (detailLevel >= 1) {
            val grassCount = if (detailLevel == 2) 6 else 3
            repeat(grassCount) { g ->
                val gRandom = kotlin.random.Random(seed + index * 100 + g)
                val grassX = hill.x - hill.width * 0.6f + gRandom.nextFloat() * hill.width * 1.2f
                // Position grass along the hill curve
                val normalizedX = (grassX - (hill.x - hill.width)) / (hill.width * 2)
                val curveHeight = sin(normalizedX * PI.toFloat()) * hill.height * 0.9f
                val grassY = hill.y - curveHeight + hill.height * 0.1f

                // Draw grass tuft
                val grassHeight = terrainSize * (0.015f + gRandom.nextFloat() * 0.01f)
                repeat(3) { blade ->
                    val bladeAngle = -PI.toFloat() / 2 + (blade - 1) * 0.3f + gRandom.nextFloat() * 0.2f
                    val bladeLen = grassHeight * (0.8f + gRandom.nextFloat() * 0.4f)
                    drawLine(
                        color = Color(0xFF5A7A55).copy(alpha = 0.6f),
                        start = Offset(grassX, grassY),
                        end = Offset(grassX + cos(bladeAngle) * bladeLen * 0.3f,
                                    grassY + sin(bladeAngle) * bladeLen),
                        strokeWidth = 0.8f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Add contour lines for depth on detailed hills
        if (detailLevel == 2 && hill.height > terrainSize * 0.1f) {
            repeat(2) { c ->
                val contourY = hill.y - hill.height * (0.3f + c * 0.25f)
                val contourWidth = hill.width * (0.7f - c * 0.2f)
                val contourPath = Path().apply {
                    moveTo(hill.x - contourWidth, contourY + hill.height * 0.1f)
                    quadraticTo(hill.x, contourY - hill.height * 0.05f,
                               hill.x + contourWidth * 0.8f, contourY + hill.height * 0.15f)
                }
                drawPath(contourPath, color = shadowColor.copy(alpha = 0.15f),
                        style = Stroke(width = 0.5f))
            }
        }
    }

    // Add scattered boulders
    repeat(3 + random.nextInt(3)) { b ->
        val bRandom = kotlin.random.Random(seed + 300 + b)
        val boulderX = center.x + (bRandom.nextFloat() - 0.5f) * terrainSize * 0.7f
        val boulderY = center.y + (bRandom.nextFloat() - 0.3f) * terrainSize * 0.5f
        drawBoulder(boulderX, boulderY, terrainSize * (0.02f + bRandom.nextFloat() * 0.025f), bRandom.nextInt())
    }

    // Add a few scattered wildflowers in foreground
    repeat(3 + random.nextInt(3)) { f ->
        val fRandom = kotlin.random.Random(seed + 500 + f)
        val flowerX = center.x - terrainSize * 0.35f + fRandom.nextFloat() * terrainSize * 0.7f
        val flowerY = center.y + terrainSize * 0.2f + fRandom.nextFloat() * terrainSize * 0.2f
        val flowerColor = listOf(
            Color(0xFFFFE066), // Yellow
            Color(0xFFFF9999), // Pink
            Color(0xFFFFFFFF), // White
            Color(0xFFB399FF)  // Purple
        )[fRandom.nextInt(4)]

        // Tiny flower dot
        drawCircle(
            color = flowerColor.copy(alpha = 0.7f),
            radius = terrainSize * 0.008f,
            center = Offset(flowerX, flowerY)
        )
    }
}

// Draw coast terrain - wavy shoreline with ships and sea creatures
private fun DrawScope.drawCoastTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    // Draw wave patterns in a semicircle
    repeat(4) { i ->
        val waveOffset = (i - 1.5f) * terrainSize * 0.1f
        val waveY = center.y + terrainSize * 0.2f + waveOffset
        val wavePath = Path().apply {
            moveTo(center.x - terrainSize * 0.4f, waveY)
            var x = center.x - terrainSize * 0.4f
            while (x < center.x + terrainSize * 0.4f) {
                val waveHeight = terrainSize * 0.03f
                quadraticTo(x + terrainSize * 0.05f, waveY - waveHeight, x + terrainSize * 0.1f, waveY)
                x += terrainSize * 0.1f
            }
        }
        drawPath(wavePath, color = TerrainColors.coast.copy(alpha = 0.5f - i * 0.1f), style = Stroke(width = 1.5f))
    }

    // Maybe add a small ship
    if (random.nextFloat() > 0.5f) {
        drawShip(
            center = Offset(center.x + terrainSize * 0.2f, center.y + terrainSize * 0.3f),
            size = terrainSize * 0.12f,
            seed = seed
        )
    }
}

// Draw a small vintage-style ship
private fun DrawScope.drawShip(center: Offset, size: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val facing = if (random.nextBoolean()) 1f else -1f

    // Hull
    val hullPath = Path().apply {
        moveTo(center.x - size * 0.5f * facing, center.y)
        lineTo(center.x + size * 0.5f * facing, center.y)
        lineTo(center.x + size * 0.3f * facing, center.y + size * 0.2f)
        lineTo(center.x - size * 0.4f * facing, center.y + size * 0.2f)
        close()
    }
    drawPath(hullPath, color = TerrainColors.ink.copy(alpha = 0.6f))

    // Mast
    drawLine(
        color = TerrainColors.ink.copy(alpha = 0.7f),
        start = Offset(center.x, center.y),
        end = Offset(center.x, center.y - size * 0.6f),
        strokeWidth = 1.5f
    )

    // Sail
    val sailPath = Path().apply {
        moveTo(center.x, center.y - size * 0.1f)
        quadraticTo(center.x + size * 0.3f * facing, center.y - size * 0.35f, center.x, center.y - size * 0.55f)
    }
    drawPath(sailPath, color = TerrainColors.ink.copy(alpha = 0.5f), style = Stroke(width = 1f))
}

// Draw swamp terrain - murky water with reeds
private fun DrawScope.drawSwampTerrain(center: Offset, terrainSize: Float, seed: Int, params: SwampParamsDto? = null) {
    val random = kotlin.random.Random(seed)
    val densityMultiplier = params?.densityMultiplier ?: 1f

    // Atmospheric colors - smooth gradient from dark to light
    val murkyWater = Color(0xFF2A4A3A) // Dark murky green-brown
    val murkyWaterMid = Color(0xFF324E3E) // Slightly lighter murky
    val stagnantWater = Color(0xFF3A5A4A) // Lighter stagnant
    val algaeColor = Color(0xFF4A6A4A) // Green algae
    val mudColor = Color(0xFF4A3A2A) // Brown mud
    val shallowMud = Color(0xFF5A4A3A) // Shallower mud areas (where trees can go)
    val reedDark = Color(0xFF3A4A2A) // Dark reed
    val reedLight = Color(0xFF5A6A3A) // Light reed
    val mistColor = Color(0xFF8A9A8A) // Foggy mist
    val deadTreeColor = Color(0xFF4A4A3A) // Dead wood

    // Generate amoeba-like swamp shape - configurable via params
    val points = params?.shapePoints ?: 20
    val noiseScale = params?.noiseScale ?: 0.35f  // How much noise affects shape (0-1)
    val sizeMultiplierX = params?.diameterMultiplierX ?: 1f
    val sizeMultiplierY = params?.diameterMultiplierY ?: 1f
    val baseRadiusX = terrainSize * 0.42f * sizeMultiplierX
    val baseRadiusY = terrainSize * 0.42f * sizeMultiplierY

    // Use seed to create unique offset into noise space - large offsets ensure unique shapes
    val seedOffsetX = (seed % 1000) * 7.3f
    val seedOffsetY = ((seed / 1000) % 1000) * 11.7f

    // Generate irregular radii using multiple octaves of Simplex noise for amoeba shape
    // Returns pairs of (radiusX, radiusY) for elliptical shapes
    val radiiPairs = (0 until points).map { i ->
        val angle = (i.toFloat() / points) * 2 * PI.toFloat()
        val nx = cos(angle)
        val ny = sin(angle)

        // Multiple octaves of noise for organic irregularity - scaled by noiseScale
        val noise1 = SimplexNoise.noise2D(nx * 1.5f + seedOffsetX, ny * 1.5f + seedOffsetY) * 0.5f
        val noise2 = SimplexNoise.noise2D(nx * 3f + seedOffsetX + 100f, ny * 3f + seedOffsetY + 100f) * 0.25f
        val noise3 = SimplexNoise.noise2D(nx * 5f + seedOffsetX + 200f, ny * 5f + seedOffsetY + 200f) * 0.12f

        // Add lobes - some points bulge out more dramatically
        val lobeNoise = SimplexNoise.noise2D(nx * 0.8f + seedOffsetX + 300f, ny * 0.8f + seedOffsetY + 300f)
        val lobe = if (lobeNoise > 0.4f) (lobeNoise - 0.4f) * 0.3f else 0f

        val totalNoise = (noise1 + noise2 + noise3 + lobe) * noiseScale
        val noiseMultiplier = 0.75f + totalNoise * 0.5f + 0.1f
        Pair(baseRadiusX * noiseMultiplier, baseRadiusY * noiseMultiplier)
    }

    // Helper to create smooth swamp path using cubic bezier (like lake)
    fun createSmoothSwampPath(scale: Float): Path = Path().apply {
        val curvePoints = (0 until points).map { i ->
            val angle = (i.toFloat() / points) * 2 * PI.toFloat()
            val (rx, ry) = radiiPairs[i]
            Offset(
                center.x + cos(angle) * rx * scale,
                center.y + sin(angle) * ry * scale
            )
        }

        moveTo(curvePoints[0].x, curvePoints[0].y)

        // Use cubic bezier for ultra-smooth curves
        for (i in 0 until points) {
            val p0 = curvePoints[(i - 1 + points) % points]
            val p1 = curvePoints[i]
            val p2 = curvePoints[(i + 1) % points]
            val p3 = curvePoints[(i + 2) % points]

            val tension = 0.4f
            val ctrl1X = p1.x + (p2.x - p0.x) * tension
            val ctrl1Y = p1.y + (p2.y - p0.y) * tension
            val ctrl2X = p2.x - (p3.x - p1.x) * tension
            val ctrl2Y = p2.y - (p3.y - p1.y) * tension

            cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, p2.x, p2.y)
        }
        close()
    }

    // Draw layered swamp base with smooth color blending
    val outerPath = createSmoothSwampPath(1.0f)
    drawPath(outerPath, color = murkyWater)

    // Blend layer - transition color
    val midPath = createSmoothSwampPath(0.85f)
    drawPath(midPath, color = murkyWaterMid)

    // Inner slightly lighter area
    val innerPath = createSmoothSwampPath(0.65f)
    drawPath(innerPath, color = stagnantWater.copy(alpha = 0.6f))

    // Track shallow areas (edges) for tree placement
    data class ShallowArea(val x: Float, val y: Float)
    val shallowAreas = mutableListOf<ShallowArea>()

    // Draw irregular stagnant pools with organic shapes and blending
    val poolCount = (3 + random.nextInt(2) * densityMultiplier).toInt().coerceAtLeast(2)
    repeat(poolCount) { p ->
        val poolRandom = kotlin.random.Random(seed + p * 13)
        val poolX = center.x + (poolRandom.nextFloat() - 0.5f) * terrainSize * 0.4f
        val poolY = center.y + (poolRandom.nextFloat() - 0.5f) * terrainSize * 0.4f
        val poolSize = terrainSize * (0.06f + poolRandom.nextFloat() * 0.05f)

        // Create organic pool shape using noise-based points
        val poolPoints = 8
        val poolPath = Path().apply {
            val poolRadii = (0 until poolPoints).map { i ->
                val angle = (i.toFloat() / poolPoints) * 2 * PI.toFloat()
                val noise = SimplexNoise.noise2D(
                    poolX * 0.1f + cos(angle) * 2f,
                    poolY * 0.1f + sin(angle) * 2f
                )
                poolSize * (0.7f + noise * 0.4f)
            }

            val firstAngle = 0f
            moveTo(poolX + poolRadii[0], poolY)

            for (i in 0 until poolPoints) {
                val nextI = (i + 1) % poolPoints
                val angle1 = (i.toFloat() / poolPoints) * 2 * PI.toFloat()
                val angle2 = (nextI.toFloat() / poolPoints) * 2 * PI.toFloat()
                val midAngle = (angle1 + angle2) / 2
                val midR = (poolRadii[i] + poolRadii[nextI]) / 2 * 0.9f

                quadraticTo(
                    poolX + cos(midAngle) * midR, poolY + sin(midAngle) * midR,
                    poolX + cos(angle2) * poolRadii[nextI], poolY + sin(angle2) * poolRadii[nextI]
                )
            }
            close()
        }

        // Draw pool with soft outer glow for blending
        drawPath(poolPath, color = stagnantWater.copy(alpha = 0.4f))

        // Inner darker pool
        val innerPoolPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(
                poolX - poolSize * 0.5f, poolY - poolSize * 0.4f,
                poolX + poolSize * 0.5f, poolY + poolSize * 0.4f
            ))
        }
        drawPath(innerPoolPath, color = murkyWaterMid.copy(alpha = 0.5f))

        // Algae patches on pool - also organic shapes
        if (poolRandom.nextFloat() > 0.4f) {
            val algaeSize = poolSize * 0.35f
            drawCircle(color = algaeColor.copy(alpha = 0.35f), radius = algaeSize * 1.2f,
                center = Offset(poolX + poolSize * 0.15f, poolY - poolSize * 0.2f))
            drawCircle(color = algaeColor.copy(alpha = 0.5f), radius = algaeSize,
                center = Offset(poolX + poolSize * 0.15f, poolY - poolSize * 0.2f))
        }
    }

    // Draw mud patches at edges (shallow areas) with soft blending
    repeat(5) { m ->
        val mudRandom = kotlin.random.Random(seed + 100 + m)
        // Place mud patches towards edges (shallow areas)
        val angle = mudRandom.nextFloat() * 2 * PI.toFloat()
        val dist = terrainSize * (0.28f + mudRandom.nextFloat() * 0.12f)
        val mudX = center.x + cos(angle) * dist
        val mudY = center.y + sin(angle) * dist
        val mudSize = terrainSize * (0.035f + mudRandom.nextFloat() * 0.025f)

        // Soft outer ring
        drawCircle(color = mudColor.copy(alpha = 0.2f), radius = mudSize * 1.4f, center = Offset(mudX, mudY))
        // Main mud patch
        drawCircle(color = mudColor.copy(alpha = 0.35f), radius = mudSize, center = Offset(mudX, mudY))
        // Inner darker spot
        drawCircle(color = shallowMud.copy(alpha = 0.25f), radius = mudSize * 0.6f, center = Offset(mudX, mudY))

        // These are good spots for trees (shallow mud areas)
        shallowAreas.add(ShallowArea(mudX, mudY))
    }

    // Draw dead/twisted tree stumps - only in shallow areas (edges)
    val stumpCount = (1 + random.nextInt(2) * densityMultiplier).toInt()
    repeat(stumpCount) { s ->
        // Place trees in shallow areas (near edges or on mud patches)
        val stumpRandom = kotlin.random.Random(seed + 200 + s)

        val (stumpX, stumpY) = if (shallowAreas.isNotEmpty() && stumpRandom.nextFloat() > 0.3f) {
            // Use a shallow area
            val area = shallowAreas[stumpRandom.nextInt(shallowAreas.size)]
            Pair(area.x + (stumpRandom.nextFloat() - 0.5f) * terrainSize * 0.08f,
                 area.y + (stumpRandom.nextFloat() - 0.5f) * terrainSize * 0.08f)
        } else {
            // Place near edge (high radius)
            val angle = stumpRandom.nextFloat() * 2 * PI.toFloat()
            val dist = terrainSize * (0.3f + stumpRandom.nextFloat() * 0.1f)
            Pair(center.x + cos(angle) * dist, center.y + sin(angle) * dist)
        }

        val stumpHeight = terrainSize * (0.07f + stumpRandom.nextFloat() * 0.05f)

        // Twisted trunk
        val trunkPath = Path().apply {
            moveTo(stumpX, stumpY)
            val twist = (stumpRandom.nextFloat() - 0.5f) * terrainSize * 0.025f
            quadraticTo(stumpX + twist, stumpY - stumpHeight * 0.5f, stumpX + twist * 0.5f, stumpY - stumpHeight)
        }
        drawPath(trunkPath, color = deadTreeColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        // Bare branches
        repeat(2) { b ->
            val branchAngle = (stumpRandom.nextFloat() - 0.5f) * PI.toFloat() * 0.5f
            val branchLen = stumpHeight * 0.35f
            val branchStartY = stumpY - stumpHeight * (0.5f + b * 0.25f)
            drawLine(
                color = deadTreeColor.copy(alpha = 0.6f),
                start = Offset(stumpX, branchStartY),
                end = Offset(stumpX + kotlin.math.sin(branchAngle) * branchLen, branchStartY - kotlin.math.cos(branchAngle) * branchLen * 0.4f),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round
            )
        }
    }

    // Draw reeds and cattails - prefer shallow edges
    val reedCount = ((8 + random.nextInt(5)) * densityMultiplier).toInt().coerceAtLeast(3)
    repeat(reedCount) { r ->
        val reedRandom = kotlin.random.Random(seed + 300 + r)

        // Bias towards edges (shallow water)
        val angle = reedRandom.nextFloat() * 2 * PI.toFloat()
        val dist = terrainSize * (0.25f + reedRandom.nextFloat() * 0.18f)
        val reedX = center.x + cos(angle) * dist + (reedRandom.nextFloat() - 0.5f) * terrainSize * 0.1f
        val reedY = center.y + sin(angle) * dist + (reedRandom.nextFloat() - 0.5f) * terrainSize * 0.1f

        val reedHeight = terrainSize * (0.04f + reedRandom.nextFloat() * 0.04f)
        val isCattail = reedRandom.nextFloat() > 0.6f

        // Reed stem with slight curve
        val stemCurve = (reedRandom.nextFloat() - 0.5f) * terrainSize * 0.015f
        val stemPath = Path().apply {
            moveTo(reedX, reedY)
            quadraticTo(reedX + stemCurve, reedY - reedHeight * 0.5f, reedX + stemCurve * 0.7f, reedY - reedHeight)
        }
        drawPath(stemPath, color = if (r % 2 == 0) reedDark else reedLight, style = Stroke(width = 1f, cap = StrokeCap.Round))

        if (isCattail) {
            val headY = reedY - reedHeight
            drawOval(
                color = Color(0xFF5A4A3A),
                topLeft = Offset(reedX + stemCurve * 0.7f - 1.5f, headY - 4f),
                size = androidx.compose.ui.geometry.Size(3f, 6f)
            )
        } else {
            val tipAngle = -PI.toFloat() / 2 + stemCurve * 0.08f
            val tipLen = reedHeight * 0.12f
            drawLine(
                color = reedLight,
                start = Offset(reedX + stemCurve * 0.7f, reedY - reedHeight),
                end = Offset(reedX + stemCurve * 0.7f + kotlin.math.cos(tipAngle) * tipLen * 0.25f, reedY - reedHeight - tipLen),
                strokeWidth = 0.6f,
                cap = StrokeCap.Round
            )
        }
    }

    // Add atmospheric mist/fog patches with soft edges
    repeat(3) { m ->
        val mistRandom = kotlin.random.Random(seed + 400 + m)
        val mistX = center.x + (mistRandom.nextFloat() - 0.5f) * terrainSize * 0.35f
        val mistY = center.y + (mistRandom.nextFloat() - 0.5f) * terrainSize * 0.35f
        val mistSize = terrainSize * (0.08f + mistRandom.nextFloat() * 0.06f)
        // Layered mist for soft edges
        drawCircle(color = mistColor.copy(alpha = 0.08f), radius = mistSize * 1.5f, center = Offset(mistX, mistY))
        drawCircle(color = mistColor.copy(alpha = 0.12f), radius = mistSize, center = Offset(mistX, mistY))
    }

    // Bubbles rising from murky water
    repeat(4) { b ->
        val bubbleRandom = kotlin.random.Random(seed + 500 + b)
        val bubbleX = center.x + (bubbleRandom.nextFloat() - 0.5f) * terrainSize * 0.35f
        val bubbleY = center.y + (bubbleRandom.nextFloat() - 0.5f) * terrainSize * 0.35f
        drawCircle(color = stagnantWater.copy(alpha = 0.4f), radius = 1.5f, center = Offset(bubbleX, bubbleY))
        drawCircle(color = algaeColor.copy(alpha = 0.2f), radius = 0.8f, center = Offset(bubbleX - 0.3f, bubbleY - 0.3f))
    }
}

// Draw castle terrain - detailed castle with towers
private fun DrawScope.drawCastleTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val castleX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.2f
    val castleY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.2f
    val castleSize = terrainSize * 0.15f

    // Main keep
    val keepPath = Path().apply {
        moveTo(castleX - castleSize * 0.4f, castleY + castleSize * 0.3f)
        lineTo(castleX - castleSize * 0.4f, castleY - castleSize * 0.2f)
        // Crenellations
        lineTo(castleX - castleSize * 0.3f, castleY - castleSize * 0.2f)
        lineTo(castleX - castleSize * 0.3f, castleY - castleSize * 0.35f)
        lineTo(castleX - castleSize * 0.1f, castleY - castleSize * 0.35f)
        lineTo(castleX - castleSize * 0.1f, castleY - castleSize * 0.2f)
        lineTo(castleX + castleSize * 0.1f, castleY - castleSize * 0.2f)
        lineTo(castleX + castleSize * 0.1f, castleY - castleSize * 0.35f)
        lineTo(castleX + castleSize * 0.3f, castleY - castleSize * 0.35f)
        lineTo(castleX + castleSize * 0.3f, castleY - castleSize * 0.2f)
        lineTo(castleX + castleSize * 0.4f, castleY - castleSize * 0.2f)
        lineTo(castleX + castleSize * 0.4f, castleY + castleSize * 0.3f)
        close()
    }
    drawPath(keepPath, color = TerrainColors.building)
    drawPath(keepPath, color = TerrainColors.ink.copy(alpha = 0.5f), style = Stroke(width = 1.5f))

    // Tower
    val towerPath = Path().apply {
        moveTo(castleX, castleY - castleSize * 0.35f)
        lineTo(castleX - castleSize * 0.15f, castleY - castleSize * 0.7f)
        lineTo(castleX + castleSize * 0.15f, castleY - castleSize * 0.7f)
        close()
    }
    drawPath(towerPath, color = TerrainColors.building)
    drawPath(towerPath, color = TerrainColors.ink.copy(alpha = 0.5f), style = Stroke(width = 1f))

    // Flag
    drawLine(
        color = TerrainColors.ink,
        start = Offset(castleX, castleY - castleSize * 0.7f),
        end = Offset(castleX, castleY - castleSize),
        strokeWidth = 1f
    )
    val flagPath = Path().apply {
        moveTo(castleX, castleY - castleSize)
        lineTo(castleX + castleSize * 0.15f, castleY - castleSize * 0.9f)
        lineTo(castleX, castleY - castleSize * 0.8f)
    }
    drawPath(flagPath, color = TerrainColors.ink.copy(alpha = 0.6f))
}

// Draw church/temple terrain - building with spire
private fun DrawScope.drawChurchTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val churchX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.2f
    val churchY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.2f
    val churchSize = terrainSize * 0.1f

    // Main building
    val buildingPath = Path().apply {
        moveTo(churchX - churchSize, churchY + churchSize * 0.5f)
        lineTo(churchX - churchSize, churchY - churchSize * 0.3f)
        lineTo(churchX, churchY - churchSize)
        lineTo(churchX + churchSize, churchY - churchSize * 0.3f)
        lineTo(churchX + churchSize, churchY + churchSize * 0.5f)
        close()
    }
    drawPath(buildingPath, color = TerrainColors.building)
    drawPath(buildingPath, color = TerrainColors.ink.copy(alpha = 0.5f), style = Stroke(width = 1f))

    // Spire/steeple
    drawLine(
        color = TerrainColors.ink.copy(alpha = 0.7f),
        start = Offset(churchX, churchY - churchSize),
        end = Offset(churchX, churchY - churchSize * 2f),
        strokeWidth = 2f
    )

    // Cross at top
    val crossSize = churchSize * 0.25f
    drawLine(
        color = TerrainColors.ink,
        start = Offset(churchX, churchY - churchSize * 2f - crossSize),
        end = Offset(churchX, churchY - churchSize * 2f + crossSize * 0.3f),
        strokeWidth = 1.5f
    )
    drawLine(
        color = TerrainColors.ink,
        start = Offset(churchX - crossSize * 0.5f, churchY - churchSize * 2f),
        end = Offset(churchX + crossSize * 0.5f, churchY - churchSize * 2f),
        strokeWidth = 1.5f
    )
}

// Draw port terrain - dock with ships
private fun DrawScope.drawPortTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    // Dock/pier
    val dockPath = Path().apply {
        moveTo(center.x - terrainSize * 0.15f, center.y)
        lineTo(center.x - terrainSize * 0.15f, center.y + terrainSize * 0.35f)
        lineTo(center.x + terrainSize * 0.15f, center.y + terrainSize * 0.35f)
        lineTo(center.x + terrainSize * 0.15f, center.y)
    }
    drawPath(dockPath, color = TerrainColors.road, style = Stroke(width = 3f))

    // Dock posts
    repeat(3) { i ->
        val postX = center.x - terrainSize * 0.1f + i * terrainSize * 0.1f
        drawCircle(
            color = TerrainColors.ink.copy(alpha = 0.6f),
            radius = 3f,
            center = Offset(postX, center.y + terrainSize * 0.35f)
        )
    }

    // Ships at dock
    drawShip(
        center = Offset(center.x + terrainSize * 0.3f, center.y + terrainSize * 0.25f),
        size = terrainSize * 0.15f,
        seed = seed
    )
    if (random.nextFloat() > 0.4f) {
        drawShip(
            center = Offset(center.x - terrainSize * 0.25f, center.y + terrainSize * 0.3f),
            size = terrainSize * 0.1f,
            seed = seed + 1
        )
    }
}

// Draw ruins terrain - crumbling stone structures
private fun DrawScope.drawRuinsTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    // Broken columns/walls
    repeat(3 + random.nextInt(3)) { i ->
        val angle = (i.toFloat() / 4) * 2 * PI.toFloat() + random.nextFloat()
        val distance = terrainSize * (0.2f + random.nextFloat() * 0.2f)
        val ruinX = center.x + cos(angle) * distance
        val ruinY = center.y + sin(angle) * distance
        val ruinHeight = terrainSize * (0.08f + random.nextFloat() * 0.06f)
        val ruinWidth = terrainSize * 0.03f

        // Column
        val columnPath = Path().apply {
            moveTo(ruinX - ruinWidth, ruinY)
            lineTo(ruinX - ruinWidth, ruinY - ruinHeight)
            // Broken top (irregular)
            lineTo(ruinX - ruinWidth * 0.5f, ruinY - ruinHeight - random.nextFloat() * ruinWidth)
            lineTo(ruinX + ruinWidth * 0.3f, ruinY - ruinHeight + random.nextFloat() * ruinWidth)
            lineTo(ruinX + ruinWidth, ruinY - ruinHeight)
            lineTo(ruinX + ruinWidth, ruinY)
            close()
        }
        drawPath(columnPath, color = TerrainColors.ruins.copy(alpha = 0.7f))
        drawPath(columnPath, color = TerrainColors.ink.copy(alpha = 0.3f), style = Stroke(width = 1f))
    }

    // Scattered stones
    repeat(5) {
        val stoneX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.5f
        val stoneY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.5f
        drawCircle(
            color = TerrainColors.ruins.copy(alpha = 0.4f),
            radius = terrainSize * 0.015f,
            center = Offset(stoneX, stoneY)
        )
    }
}

// Data class for neighbor elevation info (used for water flow direction)
private data class NeighborElevations(
    val north: Float? = null,
    val south: Float? = null,
    val east: Float? = null,
    val west: Float? = null
)

// Data class for neighbor river info (used for river connections)
private data class NeighborRivers(
    val north: Boolean = false,
    val south: Boolean = false,
    val east: Boolean = false,
    val west: Boolean = false
)

// Data class for pass-through features (features that should be drawn because
// they exist on tiles further in that direction, creating visual continuity)
private data class PassThroughFeatures(
    // For each terrain type, track which directions have that feature further along
    val riverDirections: Set<ExitDirection> = emptySet(),
    val forestDirections: Set<ExitDirection> = emptySet(),
    val mountainDirections: Set<ExitDirection> = emptySet(),
    val hillsDirections: Set<ExitDirection> = emptySet(),
    val lakeDirections: Set<ExitDirection> = emptySet(),
    val swampDirections: Set<ExitDirection> = emptySet()
) {
    fun hasPassThroughRiver() = riverDirections.size >= 2
    fun hasPassThroughForest() = forestDirections.size >= 2
    fun hasPassThroughMountain() = mountainDirections.size >= 2
    fun hasPassThroughHills() = hillsDirections.size >= 2
    fun hasPassThroughLake() = lakeDirections.size >= 2
    fun hasPassThroughSwamp() = swampDirections.size >= 2
}

// Master terrain drawing function
private fun DrawScope.drawLocationTerrain(
    location: LocationDto,
    center: Offset,
    terrainSize: Float,
    overrides: TerrainOverridesDto? = null,
    neighborElevations: NeighborElevations? = null,
    neighborRivers: NeighborRivers? = null,
    passThrough: PassThroughFeatures = PassThroughFeatures()
) {
    val terrains = parseTerrainFromDescription(location.desc, location.name)
    val seed = location.id.hashCode()
    val elevation = calculateElevationFromTerrain(terrains, overrides?.elevation)

    // Draw in specific order (bottom layer first, then features on top)
    val hasLake = TerrainType.LAKE in terrains || passThrough.hasPassThroughLake()

    // 1. Rivers/streams at the very bottom (like carved into terrain)
    // Skip rivers/streams if:
    // - swamp is present (swamp has its own murky water)
    // - lake is present (rivers terminate AT lakes, not drawn over them)
    val hasSwamp = TerrainType.SWAMP in terrains
    val hasRiver = TerrainType.RIVER in terrains
    val hasStream = TerrainType.STREAM in terrains

    // TODO: Pass-through features disabled for now - need to fix river alignment first
    // Draw pass-through rivers (connecting rivers that exist further in each direction)
    // if (!hasRiver && !hasStream && !hasSwamp && passThrough.hasPassThroughRiver()) {
    //     drawPassThroughRiver(center, terrainSize, seed + 16, passThrough.riverDirections, neighborElevations, elevation)
    // }
    if (hasRiver && !hasSwamp && !hasLake) drawRiverTerrain(center, terrainSize, seed + 16, overrides?.river, false, neighborElevations, elevation, neighborRivers)
    if (hasStream && !hasSwamp && !hasLake) drawStreamTerrain(center, terrainSize, seed + 14, overrides?.stream, false, neighborElevations, elevation, neighborRivers)

    // 2. Base terrain (ground cover)
    if (TerrainType.DESERT in terrains) drawDesertTerrain(center, terrainSize, seed, overrides?.desert)
    if (TerrainType.GRASS in terrains) drawGrassTerrain(center, terrainSize, seed, overrides?.grass)
    if (TerrainType.SWAMP in terrains) drawSwampTerrain(center, terrainSize, seed + 10, overrides?.swamp)
    // Pass-through swamp disabled
    // if (TerrainType.SWAMP !in terrains && passThrough.hasPassThroughSwamp()) {
    //     drawPassThroughSwamp(center, terrainSize, seed + 10, passThrough.swampDirections)
    // }

    // 3. Water bodies (lakes, coast, pools - on top of base but below terrain features)
    // Pre-check elevated terrain for lake shore positioning
    val hasHills = TerrainType.HILLS in terrains
    val hasMountain = TerrainType.MOUNTAIN in terrains

    if (TerrainType.COAST in terrains) drawCoastTerrain(center, terrainSize, seed + 7)
    if (TerrainType.LAKE in terrains) drawLakeTerrain(center, terrainSize, seed + 15, overrides?.lake, hasHills, hasMountain, neighborElevations)
    // Pass-through lake disabled
    // if (TerrainType.LAKE !in terrains && passThrough.hasPassThroughLake()) {
    //     drawPassThroughLake(center, terrainSize, seed + 15, passThrough.lakeDirections)
    // }
    // Don't draw generic WATER if swamp is present (swamp has its own murky water)
    if (TerrainType.WATER in terrains && TerrainType.SWAMP !in terrains) drawWaterTerrain(center, terrainSize, seed + 1)
    if (TerrainType.PORT in terrains) drawPortTerrain(center, terrainSize, seed + 11)

    // 4. Elevated terrain (hasHills and hasMountain already defined above for lake positioning)
    // Pass-through hills disabled
    // if (!hasHills && passThrough.hasPassThroughHills()) {
    //     drawPassThroughHills(center, terrainSize, seed + 8, passThrough.hillsDirections)
    // }
    if (hasHills) drawHillsTerrain(center, terrainSize, seed + 8, overrides?.hills)
    // Pass-through mountains disabled
    // if (!hasMountain && passThrough.hasPassThroughMountain()) {
    //     drawPassThroughMountain(center, terrainSize, seed + 2, passThrough.mountainDirections)
    // }
    if (hasMountain) drawMountainTerrain(center, terrainSize, seed + 2, overrides?.mountain)

    // 5. Infrastructure - skip roads if there's a lake (roads don't go through lakes)
    val hasRoad = TerrainType.ROAD in terrains
    if (hasRoad && !hasLake) drawRoadTerrain(center, terrainSize, seed + 4, hasRiver, hasStream)

    // 6. Vegetation (trees on top of roads)
    val hasForest = TerrainType.FOREST in terrains
    // Pass-through forest disabled
    // if (!hasForest && passThrough.hasPassThroughForest()) {
    //     drawPassThroughForest(center, terrainSize, seed + 3, passThrough.forestDirections)
    // }
    if (hasForest) drawForestTerrain(center, terrainSize, seed + 3, overrides?.forest)

    // 7. Structures (on top of everything, but not on swamps)
    // Buildings, ruins, and castles disabled - shapes too simplistic
    // if (TerrainType.RUINS in terrains) drawRuinsTerrain(center, terrainSize, seed + 12)
    if (TerrainType.CAVE in terrains) drawCaveTerrain(center, terrainSize, seed + 5)
    // if (TerrainType.BUILDING in terrains && !hasSwamp) drawBuildingTerrain(center, terrainSize, seed + 6)
    if (TerrainType.CHURCH in terrains && !hasSwamp) drawChurchTerrain(center, terrainSize, seed + 9)
    // if (TerrainType.CASTLE in terrains && !hasSwamp) drawCastleTerrain(center, terrainSize, seed + 13)

    // 8. Elevation shading overlay - disabled for now
    // drawElevationShading(center, terrainSize, elevation, neighborElevations, seed)
}

// Calculate flow direction from neighbor elevations (returns normalized direction vector)
// Water flows downhill - from high to low elevation
private fun calculateFlowDirection(elevation: Float, neighbors: NeighborElevations?): Pair<Float, Float> {
    if (neighbors == null) {
        return Pair(0f, 1f) // Default: flow south if no neighbors
    }

    // Calculate gradient as difference in elevation
    // Positive gradient means neighbor is higher, negative means lower
    // Flow goes toward LOWER elevation (negative gradient direction)
    var gradientX = 0f
    var gradientY = 0f
    var count = 0

    // East-West gradient: positive = east is higher, flow goes west (negative X)
    neighbors.east?.let { eastElev ->
        neighbors.west?.let { westElev ->
            gradientX = (eastElev - westElev) / 2f
            count++
        } ?: run {
            gradientX = eastElev - elevation
            count++
        }
    } ?: neighbors.west?.let { westElev ->
        gradientX = elevation - westElev
        count++
    }

    // North-South gradient: positive = south is higher, flow goes north (negative Y)
    neighbors.south?.let { southElev ->
        neighbors.north?.let { northElev ->
            gradientY = (southElev - northElev) / 2f
            count++
        } ?: run {
            gradientY = southElev - elevation
            count++
        }
    } ?: neighbors.north?.let { northElev ->
        gradientY = elevation - northElev
        count++
    }

    // Flow direction is OPPOSITE of gradient (downhill)
    val flowX = -gradientX
    val flowY = -gradientY

    val magnitude = kotlin.math.sqrt(flowX * flowX + flowY * flowY)
    return if (magnitude > 0.01f) {
        Pair(flowX / magnitude, flowY / magnitude)
    } else {
        Pair(0f, 1f) // Default: flow south if flat
    }
}

// Draw elevation shading with organic shape and directional gradient
// Extended to tile edges for seamless blending with neighbors
private fun DrawScope.drawElevationShading(
    center: Offset,
    terrainSize: Float,
    elevation: Float,
    neighbors: NeighborElevations?,
    seed: Int = center.hashCode()
) {
    val random = kotlin.random.Random(seed)
    // Extend radius to fully cover tile for seamless blending
    val baseRadius = terrainSize * 0.52f

    // Calculate flow direction
    val (flowX, flowY) = calculateFlowDirection(elevation, neighbors)
    val hasFlow = neighbors != null && (neighbors.north != null || neighbors.south != null ||
                                         neighbors.east != null || neighbors.west != null)

    // Generate organic shape with more points for smoother edges
    // Use world-space continuous noise so adjacent tiles blend at edges
    val numPoints = 16
    val noiseScale = 0.6f
    // Use center position for noise seed to ensure continuity across tiles
    val worldX = center.x * 0.01f
    val worldY = center.y * 0.01f

    val points = (0 until numPoints).map { i ->
        val angle = (i.toFloat() / numPoints) * 2 * PI.toFloat()
        val nx = kotlin.math.cos(angle)
        val ny = kotlin.math.sin(angle)

        // Use world-space noise for continuous blending at tile edges
        val edgeX = worldX + nx * noiseScale
        val edgeY = worldY + ny * noiseScale
        val simplexNoise = SimplexNoise.noise2D(edgeX, edgeY) * 0.08f

        // Smaller local variation for organic feel
        val localNoise = SimplexNoise.noise2D(nx * 2f + seed * 0.01f, ny * 2f + seed * 0.01f) * 0.06f

        val noise = simplexNoise + localNoise
        val r = baseRadius * (0.94f + noise)
        Offset(center.x + nx * r, center.y + ny * r)
    }

    // Create smooth organic path
    val organicPath = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in points.indices) {
            val p0 = points[(i - 1 + numPoints) % numPoints]
            val p1 = points[i]
            val p2 = points[(i + 1) % numPoints]
            val p3 = points[(i + 2) % numPoints]

            // Catmull-Rom to Bezier for smooth curves
            val tension = 0.35f
            val ctrl1X = p1.x + (p2.x - p0.x) * tension
            val ctrl1Y = p1.y + (p2.y - p0.y) * tension
            val ctrl2X = p2.x - (p3.x - p1.x) * tension
            val ctrl2Y = p2.y - (p3.y - p1.y) * tension

            cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, p2.x, p2.y)
        }
        close()
    }

    if (hasFlow) {
        // Directional gradient in flow direction (high to low)
        val gradientExtent = baseRadius * 1.3f
        val highSide = Offset(center.x - flowX * gradientExtent, center.y - flowY * gradientExtent)
        val lowSide = Offset(center.x + flowX * gradientExtent, center.y + flowY * gradientExtent)

        // Multi-stop gradient for smoother transition
        val gradientBrush = androidx.compose.ui.graphics.Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFFFFFAE8).copy(alpha = 0.18f),   // Warm light at high
                0.3f to Color(0xFFFFF8E0).copy(alpha = 0.08f),
                0.5f to Color.Transparent,
                0.7f to Color(0xFF2A3A4A).copy(alpha = 0.08f),
                1.0f to Color(0xFF1A2A3A).copy(alpha = 0.22f)    // Cool dark at low
            ),
            start = highSide,
            end = lowSide
        )

        drawPath(organicPath, brush = gradientBrush)
    } else {
        // No flow direction - use elevation-based flat shading with organic shape
        val shadingAlpha = (-elevation * 0.15f).coerceIn(-0.10f, 0.18f)

        if (shadingAlpha > 0.02f) {
            drawPath(organicPath, color = Color(0xFF1A2A3A).copy(alpha = shadingAlpha))
        } else if (shadingAlpha < -0.02f) {
            drawPath(organicPath, color = Color(0xFFFFFAE8).copy(alpha = -shadingAlpha))
        }
    }
}

// Pass-through terrain drawing functions
// These draw lighter/transitional versions of terrain features for tiles
// that don't have the feature but connect areas that do

// Draw a river passing through to connect river areas on opposite sides
private fun DrawScope.drawPassThroughRiver(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>,
    neighborElevations: NeighborElevations?,
    elevation: Float
) {
    val random = kotlin.random.Random(seed)
    val riverColor = Color(0xFF4A8AAA).copy(alpha = 0.7f)
    val deepWater = Color(0xFF2A5A7A).copy(alpha = 0.6f)
    val bankColor = Color(0xFF3A5060).copy(alpha = 0.5f)

    // Get direction vectors for the two endpoints
    val dirList = directions.toList()
    if (dirList.size < 2) return

    // Sort by elevation to determine flow direction (high to low)
    val sortedDirs = dirList.sortedByDescending { dir ->
        when (dir) {
            ExitDirection.NORTH, ExitDirection.NORTHEAST, ExitDirection.NORTHWEST -> neighborElevations?.north ?: 0f
            ExitDirection.SOUTH, ExitDirection.SOUTHEAST, ExitDirection.SOUTHWEST -> neighborElevations?.south ?: 0f
            ExitDirection.EAST, ExitDirection.NORTHEAST, ExitDirection.SOUTHEAST -> neighborElevations?.east ?: 0f
            ExitDirection.WEST, ExitDirection.NORTHWEST, ExitDirection.SOUTHWEST -> neighborElevations?.west ?: 0f
            else -> 0f
        }
    }

    val (entryDirX, entryDirY) = getDirectionVector(sortedDirs[0])
    val (exitDirX, exitDirY) = getDirectionVector(sortedDirs[1])

    val entryX = center.x + entryDirX * terrainSize * 0.5f
    val entryY = center.y + entryDirY * terrainSize * 0.5f
    val exitX = center.x + exitDirX * terrainSize * 0.5f
    val exitY = center.y + exitDirY * terrainSize * 0.5f

    // Calculate perpendicular for meandering
    val flowX = exitX - entryX
    val flowY = exitY - entryY
    val flowLen = kotlin.math.sqrt(flowX * flowX + flowY * flowY).coerceAtLeast(0.001f)
    val perpX = -flowY / flowLen
    val perpY = flowX / flowLen

    // Create meandering river path
    val numSegments = 5
    val points = mutableListOf<Offset>()
    points.add(Offset(entryX, entryY))

    for (i in 1 until numSegments) {
        val t = i.toFloat() / numSegments
        val baseX = entryX + (exitX - entryX) * t
        val baseY = entryY + (exitY - entryY) * t
        val meander = kotlin.math.sin(t * PI.toFloat() * 1.5f + seed * 0.3f) * terrainSize * 0.12f
        val noise = (random.nextFloat() - 0.5f) * terrainSize * 0.04f
        points.add(Offset(baseX + perpX * meander + perpX * noise, baseY + perpY * meander + perpY * noise))
    }
    points.add(Offset(exitX, exitY))

    // Draw river path
    val riverPath = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            quadraticTo((prev.x + curr.x) / 2, (prev.y + curr.y) / 2, curr.x, curr.y)
        }
    }

    drawPath(riverPath, color = bankColor, style = Stroke(width = 10f, cap = StrokeCap.Round))
    drawPath(riverPath, color = riverColor, style = Stroke(width = 7f, cap = StrokeCap.Round))
    drawPath(riverPath, color = deepWater, style = Stroke(width = 3f, cap = StrokeCap.Round))
}

// Draw scattered trees connecting forest areas
private fun DrawScope.drawPassThroughForest(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val random = kotlin.random.Random(seed)
    val darkGreen = Color(0xFF1A4A2A).copy(alpha = 0.6f)
    val midGreen = Color(0xFF2A6A3A).copy(alpha = 0.7f)

    // Draw fewer, scattered trees (about half of normal density)
    val treeCount = 3 + random.nextInt(3)

    // Bias tree placement toward the directions that have forests
    repeat(treeCount) { i ->
        val dirList = directions.toList()
        val biasDir = if (dirList.isNotEmpty()) dirList[i % dirList.size] else ExitDirection.UNKNOWN
        val (biasX, biasY) = getDirectionVector(biasDir)

        val angle = (i.toFloat() / treeCount) * 2 * PI.toFloat() + random.nextFloat() * 0.8f
        val distance = terrainSize * (0.15f + random.nextFloat() * 0.25f)
        // Bias toward forest directions
        val treeX = center.x + cos(angle) * distance + biasX * terrainSize * 0.1f
        val treeY = center.y + sin(angle) * distance + biasY * terrainSize * 0.1f
        val treeSize = terrainSize * (0.04f + random.nextFloat() * 0.04f)

        // Simple conifer shape
        val treePath = Path().apply {
            moveTo(treeX, treeY - treeSize * 0.6f)
            lineTo(treeX - treeSize * 0.35f, treeY)
            lineTo(treeX + treeSize * 0.35f, treeY)
            close()
        }
        drawPath(treePath, color = if (i % 2 == 0) darkGreen else midGreen)
    }
}

// Draw transitional hill terrain between hill areas
private fun DrawScope.drawPassThroughHills(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val random = kotlin.random.Random(seed)
    val hillBaseColor = Color(0xFF6A8A5A).copy(alpha = 0.4f)
    val hillHighlight = Color(0xFF8AAA6A).copy(alpha = 0.3f)

    // Draw gentle rolling terrain (smaller hills)
    val hillCount = 2 + random.nextInt(2)
    repeat(hillCount) { i ->
        val angle = (i.toFloat() / hillCount) * 2 * PI.toFloat() + random.nextFloat() * 0.6f
        val distance = terrainSize * (0.1f + random.nextFloat() * 0.2f)
        val hillX = center.x + cos(angle) * distance
        val hillY = center.y + sin(angle) * distance
        val hillWidth = terrainSize * (0.15f + random.nextFloat() * 0.1f)
        val hillHeight = terrainSize * (0.06f + random.nextFloat() * 0.04f)

        // Draw small mound
        drawOval(
            color = hillBaseColor,
            topLeft = Offset(hillX - hillWidth / 2, hillY - hillHeight / 2),
            size = androidx.compose.ui.geometry.Size(hillWidth, hillHeight)
        )
        drawOval(
            color = hillHighlight,
            topLeft = Offset(hillX - hillWidth * 0.35f, hillY - hillHeight * 0.4f),
            size = androidx.compose.ui.geometry.Size(hillWidth * 0.5f, hillHeight * 0.5f)
        )
    }
}

// Draw transitional mountain terrain (foothills/ridges)
private fun DrawScope.drawPassThroughMountain(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val random = kotlin.random.Random(seed)
    val rockColor = Color(0xFF6A6A7A).copy(alpha = 0.5f)
    val shadowColor = Color(0xFF4A4A5A).copy(alpha = 0.4f)

    // Draw rocky outcrops / small peaks (foothills)
    val peakCount = 1 + random.nextInt(2)
    repeat(peakCount) { i ->
        val angle = (i.toFloat() / peakCount) * 2 * PI.toFloat() + random.nextFloat() * 0.8f
        val distance = terrainSize * (0.08f + random.nextFloat() * 0.15f)
        val peakX = center.x + cos(angle) * distance
        val peakY = center.y + sin(angle) * distance
        val peakWidth = terrainSize * (0.1f + random.nextFloat() * 0.08f)
        val peakHeight = terrainSize * (0.08f + random.nextFloat() * 0.06f)

        // Small triangular peak
        val peakPath = Path().apply {
            moveTo(peakX, peakY - peakHeight)
            lineTo(peakX - peakWidth / 2, peakY)
            lineTo(peakX + peakWidth / 2, peakY)
            close()
        }
        drawPath(peakPath, color = shadowColor)

        // Highlight side
        val highlightPath = Path().apply {
            moveTo(peakX, peakY - peakHeight)
            lineTo(peakX - peakWidth * 0.3f, peakY - peakHeight * 0.3f)
            lineTo(peakX - peakWidth / 2, peakY)
            close()
        }
        drawPath(highlightPath, color = rockColor)
    }
}

// Draw transitional swamp (muddy/marshy areas)
private fun DrawScope.drawPassThroughSwamp(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val random = kotlin.random.Random(seed)
    val muddyColor = Color(0xFF5A6A4A).copy(alpha = 0.3f)
    val waterColor = Color(0xFF4A5A4A).copy(alpha = 0.25f)

    // Draw scattered muddy patches
    val patchCount = 2 + random.nextInt(2)
    repeat(patchCount) { i ->
        val angle = random.nextFloat() * 2 * PI.toFloat()
        val distance = terrainSize * (0.1f + random.nextFloat() * 0.2f)
        val patchX = center.x + cos(angle) * distance
        val patchY = center.y + sin(angle) * distance
        val patchRadius = terrainSize * (0.06f + random.nextFloat() * 0.05f)

        drawCircle(color = muddyColor, radius = patchRadius, center = Offset(patchX, patchY))
        // Small water puddle
        drawCircle(color = waterColor, radius = patchRadius * 0.5f, center = Offset(patchX + patchRadius * 0.2f, patchY))
    }
}

// Draw water channel connecting lake areas
private fun DrawScope.drawPassThroughLake(
    center: Offset,
    terrainSize: Float,
    seed: Int,
    directions: Set<ExitDirection>
) {
    val random = kotlin.random.Random(seed)
    val waterColor = Color(0xFF4A7A9A).copy(alpha = 0.5f)
    val deepColor = Color(0xFF2A5A7A).copy(alpha = 0.4f)
    val shoreColor = Color(0xFF8A9A7A).copy(alpha = 0.3f)

    val dirList = directions.toList()
    if (dirList.size < 2) return

    // Draw a water channel connecting the lake directions
    val (dir1X, dir1Y) = getDirectionVector(dirList[0])
    val (dir2X, dir2Y) = getDirectionVector(dirList[1])

    val start = Offset(center.x + dir1X * terrainSize * 0.45f, center.y + dir1Y * terrainSize * 0.45f)
    val end = Offset(center.x + dir2X * terrainSize * 0.45f, center.y + dir2Y * terrainSize * 0.45f)

    // Curved channel through center
    val channelPath = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.15f,
                   center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.15f,
                   end.x, end.y)
    }

    drawPath(channelPath, color = shoreColor, style = Stroke(width = 18f, cap = StrokeCap.Round))
    drawPath(channelPath, color = waterColor, style = Stroke(width = 12f, cap = StrokeCap.Round))
    drawPath(channelPath, color = deepColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
}

private data class NodeState(
    val id: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

/**
 * Get the unit vector (dx, dy) for a direction.
 * North is up (negative y), East is right (positive x).
 */
private fun getDirectionVector(direction: ExitDirection): Pair<Float, Float> = when (direction) {
    ExitDirection.NORTH -> Pair(0f, -1f)
    ExitDirection.NORTHEAST -> Pair(0.707f, -0.707f)
    ExitDirection.EAST -> Pair(1f, 0f)
    ExitDirection.SOUTHEAST -> Pair(0.707f, 0.707f)
    ExitDirection.SOUTH -> Pair(0f, 1f)
    ExitDirection.SOUTHWEST -> Pair(-0.707f, 0.707f)
    ExitDirection.WEST -> Pair(-1f, 0f)
    ExitDirection.NORTHWEST -> Pair(-0.707f, -0.707f)
    ExitDirection.UNKNOWN -> Pair(0f, 0f) // No directional bias
}

/**
 * Calculate positions using stored database coordinates when available.
 * Falls back to BFS-based placement for locations without stored coordinates.
 * If location A has a SOUTH exit to location B, then B is placed directly south of A.
 */
private fun calculateForceDirectedPositions(
    locations: List<LocationDto>
): GridPositionResult {
    if (locations.isEmpty()) return GridPositionResult(emptyMap(), emptyMap(), GridBounds(0, 0, 0, 0))
    if (locations.size == 1) {
        val loc = locations[0]
        val x = loc.gridX ?: 0
        val y = loc.gridY ?: 0
        return GridPositionResult(
            locationPositions = mapOf(loc.id to LocationPosition(loc, 0.5f, 0.5f)),
            gridPositions = mapOf(loc.id to Pair(x, y)),
            gridBounds = GridBounds(x, x, y, y)
        )
    }

    val locationMap = locations.associateBy { it.id }

    // Grid positions: (gridX, gridY) where positive X is east, positive Y is south
    val gridPositions = mutableMapOf<String, Pair<Int, Int>>()
    val visited = mutableSetOf<String>()

    // First pass: Use stored database coordinates for locations that have them
    locations.filter { it.gridX != null && it.gridY != null }.forEach { loc ->
        gridPositions[loc.id] = Pair(loc.gridX!!, loc.gridY!!)
        visited.add(loc.id)
    }

    // Second pass: BFS to place locations without stored coordinates
    // Start from a location with coordinates, or the first location if none have coords
    val startLocation = locations.find { it.gridX != null } ?: locations.first()
    if (startLocation.id !in gridPositions) {
        gridPositions[startLocation.id] = Pair(0, 0)
        visited.add(startLocation.id)
    }

    // BFS to place remaining connected locations
    val queue = ArrayDeque<String>()
    // Add all locations with coordinates to the queue to expand from
    gridPositions.keys.forEach { queue.add(it) }

    while (queue.isNotEmpty()) {
        val currentId = queue.removeFirst()
        val currentPos = gridPositions[currentId] ?: continue
        val currentLocation = locationMap[currentId] ?: continue

        // Place neighbors based on exit directions
        for (exit in currentLocation.exits) {
            val neighborId = exit.locationId
            val neighbor = locationMap[neighborId] ?: continue

            if (neighborId !in visited) {
                // Check if neighbor has stored coordinates
                if (neighbor.gridX != null && neighbor.gridY != null) {
                    gridPositions[neighborId] = Pair(neighbor.gridX, neighbor.gridY)
                } else {
                    // Calculate position from exit direction
                    val (dx, dy) = getGridOffset(exit.direction)
                    val newPos = Pair(currentPos.first + dx, currentPos.second + dy)

                    // Check if position is already occupied
                    val existingAtPos = gridPositions.entries.find { it.value == newPos }
                    if (existingAtPos == null) {
                        gridPositions[neighborId] = newPos
                    } else {
                        // Position occupied, find nearby free spot
                        gridPositions[neighborId] = findNearbyFreeSpot(newPos, gridPositions.values.toSet())
                    }
                }

                visited.add(neighborId)
                queue.add(neighborId)
            }
        }
    }

    // Handle disconnected locations (not reachable from any placed location)
    locations.filter { it.id !in gridPositions }.forEachIndexed { index, loc ->
        // Use stored coords if available, otherwise place below the main graph
        if (loc.gridX != null && loc.gridY != null) {
            gridPositions[loc.id] = Pair(loc.gridX, loc.gridY)
        } else {
            val maxY = gridPositions.values.maxOfOrNull { it.second } ?: 0
            gridPositions[loc.id] = findNearbyFreeSpot(Pair(index, maxY + 2), gridPositions.values.toSet())
        }
    }

    // Normalize grid positions to 0.0-1.0 range
    val allPositions = gridPositions.values.toList()
    val minX = allPositions.minOfOrNull { it.first } ?: 0
    val maxX = allPositions.maxOfOrNull { it.first } ?: 0
    val minY = allPositions.minOfOrNull { it.second } ?: 0
    val maxY = allPositions.maxOfOrNull { it.second } ?: 0

    val rangeX = (maxX - minX).coerceAtLeast(1)
    val rangeY = (maxY - minY).coerceAtLeast(1)

    // Add padding and convert to normalized coordinates
    val spacingMultiplier = 1.0f
    val padding = 0.15f
    val availableRange = 1f - 2 * padding

    val locationPositions = gridPositions.mapValues { (id, gridPos) ->
        val normalizedX = if (rangeX == 1) 0.5f else padding + availableRange * (gridPos.first - minX).toFloat() / rangeX * spacingMultiplier
        val normalizedY = if (rangeY == 1) 0.5f else padding + availableRange * (gridPos.second - minY).toFloat() / rangeY * spacingMultiplier
        LocationPosition(
            location = locationMap[id]!!,
            x = normalizedX,
            y = normalizedY
        )
    }

    return GridPositionResult(
        locationPositions = locationPositions,
        gridPositions = gridPositions,
        gridBounds = GridBounds(minX, maxX, minY, maxY, padding)
    )
}

/**
 * Get grid offset for a direction.
 * Returns (dx, dy) where positive X is east, positive Y is south.
 */
private fun getGridOffset(direction: ExitDirection): Pair<Int, Int> = when (direction) {
    ExitDirection.NORTH -> Pair(0, -1)
    ExitDirection.NORTHEAST -> Pair(1, -1)
    ExitDirection.EAST -> Pair(1, 0)
    ExitDirection.SOUTHEAST -> Pair(1, 1)
    ExitDirection.SOUTH -> Pair(0, 1)
    ExitDirection.SOUTHWEST -> Pair(-1, 1)
    ExitDirection.WEST -> Pair(-1, 0)
    ExitDirection.NORTHWEST -> Pair(-1, -1)
    ExitDirection.UNKNOWN -> Pair(0, 1) // Default to south for unknown
}

/**
 * Find a free spot near the target position.
 */
private fun findNearbyFreeSpot(target: Pair<Int, Int>, occupied: Set<Pair<Int, Int>>): Pair<Int, Int> {
    if (target !in occupied) return target

    // Spiral outward to find free spot
    for (radius in 1..10) {
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (kotlin.math.abs(dx) == radius || kotlin.math.abs(dy) == radius) {
                    val candidate = Pair(target.first + dx, target.second + dy)
                    if (candidate !in occupied) return candidate
                }
            }
        }
    }
    return Pair(target.first + 10, target.second) // Fallback
}

/**
 * Get direction from grid offset.
 */
private fun getDirectionFromOffset(dx: Int, dy: Int): ExitDirection = when {
    dx == 0 && dy == -1 -> ExitDirection.NORTH
    dx == 1 && dy == -1 -> ExitDirection.NORTHEAST
    dx == 1 && dy == 0 -> ExitDirection.EAST
    dx == 1 && dy == 1 -> ExitDirection.SOUTHEAST
    dx == 0 && dy == 1 -> ExitDirection.SOUTH
    dx == -1 && dy == 1 -> ExitDirection.SOUTHWEST
    dx == -1 && dy == 0 -> ExitDirection.WEST
    dx == -1 && dy == -1 -> ExitDirection.NORTHWEST
    else -> ExitDirection.UNKNOWN
}

@Composable
fun LocationForm(
    editLocation: LocationDto?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToItem: (String) -> Unit,
    onNavigateToCreature: (String) -> Unit,
    onNavigateToLocation: (LocationDto) -> Unit,
    onNavigateToUser: (String) -> Unit,
    currentUser: UserDto? = null,
    isAdmin: Boolean = false,
    onLocationUpdated: (LocationDto) -> Unit = {},
    onDeleted: () -> Unit = {}
) {
    val isEditMode = editLocation != null
    var name by remember(editLocation?.id) { mutableStateOf(editLocation?.name ?: "") }
    var desc by remember(editLocation?.id) { mutableStateOf(editLocation?.desc ?: "") }
    var itemIds by remember(editLocation?.id) { mutableStateOf(editLocation?.itemIds ?: emptyList()) }
    var creatureIds by remember(editLocation?.id) { mutableStateOf(editLocation?.creatureIds ?: emptyList()) }
    var exits by remember(editLocation?.id) { mutableStateOf(editLocation?.exits ?: emptyList()) }
    var features by remember(editLocation?.id) { mutableStateOf(editLocation?.featureIds?.joinToString(", ") ?: "") }
    var imageUrl by remember(editLocation?.id) { mutableStateOf(editLocation?.imageUrl) }
    var isLoading by remember(editLocation?.id) { mutableStateOf(false) }
    var message by remember(editLocation?.id) { mutableStateOf<String?>(null) }
    var imageGenError by remember(editLocation?.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Track if image generation is in progress for this location
    val generatingEntities by BackgroundImageGenerationManager.generatingEntities.collectAsState()
    val isImageGenerating = editLocation?.id?.let { it in generatingEntities } ?: false

    // Lock state
    var lockedBy by remember(editLocation?.id) { mutableStateOf(editLocation?.lockedBy) }
    var lockerName by remember(editLocation?.id) { mutableStateOf<String?>(null) }
    val isLocked = lockedBy != null

    // Combined disabled state: not authenticated OR locked OR image generating
    val isNotAuthenticated = currentUser == null
    val isDisabled = isNotAuthenticated || isLocked || isImageGenerating

    // State for exit removal confirmation dialog
    var exitToRemove by remember { mutableStateOf<ExitDto?>(null) }
    var showRemoveExitDialog by remember { mutableStateOf(false) }

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Available options for dropdowns
    var availableItems by remember { mutableStateOf<List<IdOption>>(emptyList()) }
    var availableCreatures by remember { mutableStateOf<List<IdOption>>(emptyList()) }
    var availableLocations by remember { mutableStateOf<List<IdOption>>(emptyList()) }
    var allLocationsForCoords by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var activeUsersAtLocation by remember(editLocation?.id) { mutableStateOf<List<UserDto>>(emptyList()) }

    // Fetch available options on mount and when location changes
    LaunchedEffect(editLocation?.id) {
        ApiClient.getItems().onSuccess { items ->
            availableItems = items.map { IdOption(it.id, it.name) }
        }
        ApiClient.getCreatures().onSuccess { creatures ->
            availableCreatures = creatures.map { IdOption(it.id, it.name) }
        }
        ApiClient.getLocations().onSuccess { locs ->
            allLocationsForCoords = locs  // Store full location data for coordinate validation
            availableLocations = locs
                .filter { it.id != editLocation?.id } // Don't show current location as exit option
                .map { IdOption(it.id, it.name) }
        }
    }

    // Fetch locker's name when location is locked
    LaunchedEffect(lockedBy) {
        val lockerId = lockedBy
        if (lockerId != null) {
            ApiClient.getUser(lockerId).onSuccess { user ->
                lockerName = user?.name
            }
        } else {
            lockerName = null
        }
    }

    // Update user's current location and fetch active users when authenticated and viewing a location
    LaunchedEffect(editLocation?.id, currentUser?.id) {
        if (currentUser != null && editLocation != null) {
            // Update user's current location
            ApiClient.updateUserLocation(currentUser.id, editLocation.id)
            // Fetch active users at this location
            ApiClient.getActiveUsersAtLocation(editLocation.id).onSuccess { users ->
                // Filter out the current user from the list
                activeUsersAtLocation = users.filter { it.id != currentUser.id }
            }
        }
    }

    // Clear user's location when leaving this view
    DisposableEffect(editLocation?.id, currentUser?.id) {
        onDispose {
            if (currentUser != null) {
                scope.launch {
                    ApiClient.updateUserLocation(currentUser.id, null)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isEditMode) "Edit Location" else "Create Location",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            // Lock status visible to all users in edit mode
            if (isEditMode && editLocation != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLocked && lockerName != null) {
                        Text(
                            text = "Locked by $lockerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Only admins can toggle the lock and delete
                    if (isAdmin) {
                        IconButton(
                            onClick = {
                                currentUser?.let { user ->
                                    scope.launch {
                                        ApiClient.toggleLocationLock(editLocation.id, user.id)
                                            .onSuccess { updatedLocation ->
                                                lockedBy = updatedLocation.lockedBy
                                                onLocationUpdated(updatedLocation)
                                            }
                                            .onFailure { error ->
                                                message = "Failed to toggle lock: ${error.message}"
                                            }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = if (isLocked) "Unlock location" else "Lock location",
                                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Show delete button only when unlocked
                        if (!isLocked) {
                            IconButton(
                                onClick = { showDeleteDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete location",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        // Non-admins see the lock icon but can't click it
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (isLocked) "Location is locked" else "Location is unlocked",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        if (isEditMode) {
            // Display image at top when editing (only if image exists or is generating)
            EntityImage(
                imageUrl = imageUrl,
                contentDescription = "Image of ${editLocation?.name ?: "location"}",
                isGenerating = isImageGenerating
            )

            if (isAdmin) {
                Text(
                    text = "ID: ${editLocation?.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Location Name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isDisabled
            )
            GenButton(
                entityType = GenEntityType.LOCATION,
                currentName = name,
                currentDesc = desc,
                exitIds = exits.map { it.locationId },
                featureIds = features.splitToList(),
                onGenerated = { _, genDesc ->
                    // Only update description, keep the existing name
                    desc = genDesc
                },
                enabled = !isDisabled
            )
        }

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            enabled = !isDisabled
        )

        IdPillSection(
            label = "Items",
            ids = itemIds,
            entityType = EntityType.ITEM,
            availableOptions = availableItems,
            onPillClick = onNavigateToItem,
            onAddId = { id -> if (!isDisabled) itemIds = itemIds + id },
            onRemoveId = { id -> if (!isDisabled) itemIds = itemIds - id },
            enabled = !isDisabled
        )

        IdPillSection(
            label = "Creatures",
            ids = creatureIds,
            entityType = EntityType.CREATURE,
            availableOptions = availableCreatures,
            onPillClick = onNavigateToCreature,
            onAddId = { id -> if (!isDisabled) creatureIds = creatureIds + id },
            onRemoveId = { id -> if (!isDisabled) creatureIds = creatureIds - id },
            enabled = !isDisabled
        )

        // Show active users at this location (if authenticated and editing)
        if (currentUser != null && isEditMode && activeUsersAtLocation.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Other Players Here",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    activeUsersAtLocation.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToUser(user.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        ExitPillSection(
            label = "Exits",
            exits = exits,
            availableOptions = availableLocations,
            onPillClick = { locationId ->
                scope.launch {
                    ApiClient.getLocation(locationId).onSuccess { loc ->
                        if (loc != null) onNavigateToLocation(loc)
                    }
                }
            },
            onAddExit = { newExit ->
                if (isDisabled) return@ExitPillSection
                // Add exit locally
                exits = exits + newExit
                // Add bidirectional exit on the other location (if we have an id)
                // Use opposite direction for the reverse exit
                if (editLocation != null) {
                    scope.launch {
                        ApiClient.getLocation(newExit.locationId).onSuccess { otherLoc ->
                            if (otherLoc != null && otherLoc.exits.none { it.locationId == editLocation.id }) {
                                val oppositeDirection = getOppositeDirection(newExit.direction)
                                val updatedExits = otherLoc.exits + ExitDto(editLocation.id, oppositeDirection)
                                val updateRequest = CreateLocationRequest(
                                    name = otherLoc.name,
                                    desc = otherLoc.desc,
                                    itemIds = otherLoc.itemIds,
                                    creatureIds = otherLoc.creatureIds,
                                    exits = updatedExits,
                                    featureIds = otherLoc.featureIds
                                )
                                ApiClient.updateLocation(otherLoc.id, updateRequest)
                            }
                        }
                    }
                }
            },
            onUpdateExit = { oldExit, newExit ->
                if (isDisabled) return@ExitPillSection
                // Update exit direction locally
                exits = exits.map { if (it == oldExit) newExit else it }
                // Update the bidirectional exit on the other location with opposite direction
                if (editLocation != null) {
                    scope.launch {
                        ApiClient.getLocation(newExit.locationId).onSuccess { otherLoc ->
                            if (otherLoc != null) {
                                val oppositeDirection = getOppositeDirection(newExit.direction)
                                val updatedExits = otherLoc.exits.map { exit ->
                                    if (exit.locationId == editLocation.id) {
                                        exit.copy(direction = oppositeDirection)
                                    } else {
                                        exit
                                    }
                                }
                                val updateRequest = CreateLocationRequest(
                                    name = otherLoc.name,
                                    desc = otherLoc.desc,
                                    itemIds = otherLoc.itemIds,
                                    creatureIds = otherLoc.creatureIds,
                                    exits = updatedExits,
                                    featureIds = otherLoc.featureIds
                                )
                                ApiClient.updateLocation(otherLoc.id, updateRequest)
                            }
                        }
                    }
                }
            },
            onRemoveExit = { exit ->
                if (isDisabled) return@ExitPillSection
                // Show confirmation dialog for exit removal
                exitToRemove = exit
                showRemoveExitDialog = true
            },
            enabled = !isDisabled,
            currentLocationId = editLocation?.id,
            allLocations = allLocationsForCoords
        )

        // Exit removal confirmation dialog
        if (showRemoveExitDialog && exitToRemove != null) {
            val exitToRemoveVal = exitToRemove!!
            val exitName = availableLocations.find { it.id == exitToRemoveVal.locationId }?.name ?: exitToRemoveVal.locationId
            AlertDialog(
                onDismissRequest = {
                    showRemoveExitDialog = false
                    exitToRemove = null
                },
                title = { Text("Remove Exit") },
                text = {
                    Text("Remove exit to \"$exitName\" (${exitToRemoveVal.direction.toDisplayLabel()}).\n\nShould this be a two-way removal (also remove this location from \"$exitName\"'s exits)?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Two-way removal
                            exits = exits.filter { it.locationId != exitToRemoveVal.locationId }
                            if (editLocation != null) {
                                scope.launch {
                                    ApiClient.getLocation(exitToRemoveVal.locationId).onSuccess { otherLoc ->
                                        if (otherLoc != null && otherLoc.exits.any { it.locationId == editLocation.id }) {
                                            val updatedExits = otherLoc.exits.filter { it.locationId != editLocation.id }
                                            val updateRequest = CreateLocationRequest(
                                                name = otherLoc.name,
                                                desc = otherLoc.desc,
                                                itemIds = otherLoc.itemIds,
                                                creatureIds = otherLoc.creatureIds,
                                                exits = updatedExits,
                                                featureIds = otherLoc.featureIds
                                            )
                                            ApiClient.updateLocation(otherLoc.id, updateRequest)
                                        }
                                    }
                                }
                            }
                            showRemoveExitDialog = false
                            exitToRemove = null
                        }
                    ) {
                        Text("Remove Both Ways")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showRemoveExitDialog = false
                                exitToRemove = null
                            }
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                // One-way removal (only from this location)
                                exits = exits.filter { it.locationId != exitToRemoveVal.locationId }
                                showRemoveExitDialog = false
                                exitToRemove = null
                            }
                        ) {
                            Text("Remove One Way")
                        }
                    }
                }
            )
        }

        // Delete confirmation dialog
        if (showDeleteDialog && editLocation != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Location") },
                text = {
                    Text("Are you sure you want to delete \"${editLocation.name}\"?\n\nThis will also remove this location from any exit lists in other locations.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                isDeleting = true
                                println("DEBUG: Starting delete for ${editLocation.id}")
                                ApiClient.deleteLocation(editLocation.id)
                                    .onSuccess {
                                        println("DEBUG: Delete successful, calling onDeleted()")
                                        showDeleteDialog = false
                                        onDeleted()
                                        println("DEBUG: onDeleted() called")
                                    }
                                    .onFailure { error ->
                                        println("DEBUG: Delete failed: ${error.message}")
                                        message = "Failed to delete: ${error.message}"
                                        showDeleteDialog = false
                                    }
                                isDeleting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Delete")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        OutlinedTextField(
            value = features,
            onValueChange = { features = it },
            label = { Text("Features (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isDisabled
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save & Gen Image / Create & Gen Image button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        imageGenError = null
                        val request = CreateLocationRequest(
                            name = name,
                            desc = desc,
                            itemIds = itemIds,
                            creatureIds = creatureIds,
                            exits = exits,
                            featureIds = features.splitToList()
                        )
                        if (isEditMode) {
                            val result = ApiClient.updateLocation(editLocation!!.id, request)
                            isLoading = false
                            if (result.isSuccess) {
                                // Start background image generation after successful save
                                BackgroundImageGenerationManager.startGeneration(
                                    entityType = "location",
                                    entityId = editLocation.id,
                                    name = name,
                                    description = desc,
                                    featureIds = features.splitToList()
                                )
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        } else {
                            val result = ApiClient.createLocation(request)
                            isLoading = false
                            result.onSuccess { createdLocation ->
                                // Start background image generation after successful create
                                BackgroundImageGenerationManager.startGeneration(
                                    entityType = "location",
                                    entityId = createdLocation.id,
                                    name = name,
                                    description = desc,
                                    featureIds = features.splitToList()
                                )
                                onSaved()
                            }.onFailure { error ->
                                message = "Error: ${error.message}"
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && desc.isNotBlank() && !isDisabled && !isImageGenerating
            ) {
                if (isImageGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditMode) "Save & Gen Image" else "Create & Gen Image")
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        val request = CreateLocationRequest(
                            name = name,
                            desc = desc,
                            itemIds = itemIds,
                            creatureIds = creatureIds,
                            exits = exits,
                            featureIds = features.splitToList()
                        )
                        if (isEditMode) {
                            val result = ApiClient.updateLocation(editLocation!!.id, request)
                            isLoading = false
                            if (result.isSuccess) {
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        } else {
                            val result = ApiClient.createLocation(request)
                            isLoading = false
                            if (result.isSuccess) {
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && !isDisabled
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isEditMode) "Save Changes" else "Create")
                }
            }
        }

        imageGenError?.let {
            SelectionContainer {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }

        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CreatureForm(
    editCreature: CreatureDto?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToItem: (String) -> Unit,
    currentUser: UserDto? = null,
    isAdmin: Boolean = false,
    onCreatureUpdated: (CreatureDto) -> Unit = {}
) {
    val isEditMode = editCreature != null
    var name by remember(editCreature?.id) { mutableStateOf(editCreature?.name ?: "") }
    var desc by remember(editCreature?.id) { mutableStateOf(editCreature?.desc ?: "") }
    var itemIds by remember(editCreature?.id) { mutableStateOf(editCreature?.itemIds ?: emptyList()) }
    var features by remember(editCreature?.id) { mutableStateOf(editCreature?.featureIds?.joinToString(", ") ?: "") }
    var imageUrl by remember(editCreature?.id) { mutableStateOf(editCreature?.imageUrl) }
    var isLoading by remember(editCreature?.id) { mutableStateOf(false) }
    var message by remember(editCreature?.id) { mutableStateOf<String?>(null) }
    var imageGenError by remember(editCreature?.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Track if image generation is in progress for this creature
    val generatingEntities by BackgroundImageGenerationManager.generatingEntities.collectAsState()
    val isImageGenerating = editCreature?.id?.let { it in generatingEntities } ?: false

    // Lock state
    var lockedBy by remember(editCreature?.id) { mutableStateOf(editCreature?.lockedBy) }
    var lockerName by remember(editCreature?.id) { mutableStateOf<String?>(null) }
    val isLocked = lockedBy != null

    // Combined disabled state: not authenticated OR locked OR image generating
    val isNotAuthenticated = currentUser == null
    val isDisabled = isNotAuthenticated || isLocked || isImageGenerating

    // Available options for dropdown
    var availableItems by remember { mutableStateOf<List<IdOption>>(emptyList()) }

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Fetch available options on mount
    LaunchedEffect(Unit) {
        ApiClient.getItems().onSuccess { items ->
            availableItems = items.map { IdOption(it.id, it.name) }
        }
    }

    // Fetch locker's name when creature is locked
    LaunchedEffect(lockedBy) {
        val lockerId = lockedBy
        if (lockerId != null) {
            ApiClient.getUser(lockerId).onSuccess { user ->
                lockerName = user?.name
            }
        } else {
            lockerName = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isEditMode) "Edit Creature" else "Create Creature",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            // Lock status visible to all users in edit mode
            if (isEditMode && editCreature != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLocked && lockerName != null) {
                        Text(
                            text = "Locked by $lockerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Only admins can toggle the lock and delete
                    if (isAdmin) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    currentUser?.let { user ->
                                        ApiClient.toggleCreatureLock(editCreature.id, user.id)
                                            .onSuccess { updatedCreature ->
                                                lockedBy = updatedCreature.lockedBy
                                                onCreatureUpdated(updatedCreature)
                                            }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = if (isLocked) "Unlock creature" else "Lock creature",
                                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Show delete button only when unlocked
                        if (!isLocked) {
                            IconButton(
                                onClick = { showDeleteDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete creature",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (isLocked) "Creature is locked" else "Creature is unlocked",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        if (isEditMode) {
            // Display image at top when editing (only if image exists or is generating)
            EntityImage(
                imageUrl = imageUrl,
                contentDescription = "Image of ${editCreature?.name ?: "creature"}",
                isGenerating = isImageGenerating
            )

            if (isAdmin) {
                Text(
                    text = "ID: ${editCreature?.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Creature Name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isDisabled
            )
            GenButton(
                entityType = GenEntityType.CREATURE,
                currentName = name,
                currentDesc = desc,
                onGenerated = { genName, genDesc ->
                    name = genName
                    desc = genDesc
                },
                enabled = !isDisabled
            )
        }

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            enabled = !isDisabled
        )

        IdPillSection(
            label = "Items",
            ids = itemIds,
            entityType = EntityType.ITEM,
            availableOptions = availableItems,
            onPillClick = onNavigateToItem,
            onAddId = { id -> if (!isDisabled) itemIds = itemIds + id },
            onRemoveId = { id -> if (!isDisabled) itemIds = itemIds - id },
            enabled = !isDisabled
        )

        OutlinedTextField(
            value = features,
            onValueChange = { features = it },
            label = { Text("Features (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isDisabled
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save & Gen Image / Create & Gen Image button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        imageGenError = null
                        val request = CreateCreatureRequest(
                            name = name,
                            desc = desc,
                            itemIds = itemIds,
                            featureIds = features.splitToList()
                        )
                        if (isEditMode) {
                            val result = ApiClient.updateCreature(editCreature!!.id, request)
                            isLoading = false
                            if (result.isSuccess) {
                                // Start background image generation after successful save
                                BackgroundImageGenerationManager.startGeneration(
                                    entityType = "creature",
                                    entityId = editCreature.id,
                                    name = name,
                                    description = desc,
                                    featureIds = features.splitToList()
                                )
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        } else {
                            val result = ApiClient.createCreature(request)
                            isLoading = false
                            result.onSuccess { createdCreature ->
                                // Start background image generation after successful create
                                BackgroundImageGenerationManager.startGeneration(
                                    entityType = "creature",
                                    entityId = createdCreature.id,
                                    name = name,
                                    description = desc,
                                    featureIds = features.splitToList()
                                )
                                onSaved()
                            }.onFailure { error ->
                                message = "Error: ${error.message}"
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && desc.isNotBlank() && !isDisabled && !isImageGenerating
            ) {
                if (isImageGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditMode) "Save & Gen Image" else "Create & Gen Image")
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        val request = CreateCreatureRequest(
                            name = name,
                            desc = desc,
                            itemIds = itemIds,
                            featureIds = features.splitToList()
                        )
                        if (isEditMode) {
                            val result = ApiClient.updateCreature(editCreature!!.id, request)
                            isLoading = false
                            if (result.isSuccess) {
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        } else {
                            val result = ApiClient.createCreature(request)
                            isLoading = false
                            if (result.isSuccess) {
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && !isDisabled
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isEditMode) "Save Changes" else "Create")
                }
            }
        }

        imageGenError?.let {
            SelectionContainer {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }

        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        // Delete confirmation dialog
        if (showDeleteDialog && editCreature != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Creature") },
                text = {
                    Text("Are you sure you want to delete \"${editCreature.name}\"?\n\nThis will also remove this creature from any locations.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                isDeleting = true
                                ApiClient.deleteCreature(editCreature.id)
                                    .onSuccess {
                                        showDeleteDialog = false
                                        onBack()
                                    }
                                    .onFailure { error ->
                                        message = "Failed to delete: ${error.message}"
                                        showDeleteDialog = false
                                    }
                                isDeleting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Delete")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CreatureDetailView(
    creatureId: String,
    onBack: () -> Unit,
    onEdit: (CreatureDto) -> Unit,
    onCreateNew: () -> Unit,
    onNavigateToItem: (String) -> Unit,
    isAdmin: Boolean = false
) {
    var creature by remember { mutableStateOf<CreatureDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }

    LaunchedEffect(creatureId) {
        isLoading = true
        ApiClient.getCreature(creatureId).onSuccess {
            creature = it
            notFound = it == null
        }.onFailure {
            notFound = true
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Creature Details",
                style = MaterialTheme.typography.titleLarge
            )
        }

        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            notFound -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Creature not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "ID: $creatureId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCreateNew) {
                            Text("Create New Creature")
                        }
                    }
                }
            }
            creature != null -> {
                // Display image at top
                EntityImage(
                    imageUrl = creature!!.imageUrl,
                    contentDescription = "Image of ${creature!!.name}"
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = creature!!.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (isAdmin) {
                            Text(
                                text = "ID: ${creature!!.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = creature!!.desc,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (creature!!.itemIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            IdPillSection(
                                label = "Items",
                                ids = creature!!.itemIds,
                                entityType = EntityType.ITEM,
                                onPillClick = onNavigateToItem,
                                onAddId = {},
                                onRemoveId = {}
                            )
                        }

                        if (creature!!.featureIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Features: ${creature!!.featureIds.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Button(
                    onClick = { onEdit(creature!!) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Edit Creature")
                }
            }
        }
    }
}

@Composable
fun ItemForm(
    editItem: ItemDto?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    currentUser: UserDto? = null,
    isAdmin: Boolean = false,
    onItemUpdated: (ItemDto) -> Unit = {}
) {
    val isEditMode = editItem != null
    var name by remember(editItem?.id) { mutableStateOf(editItem?.name ?: "") }
    var desc by remember(editItem?.id) { mutableStateOf(editItem?.desc ?: "") }
    var featureIds by remember(editItem?.id) { mutableStateOf(editItem?.featureIds?.joinToString(", ") ?: "") }
    var imageUrl by remember(editItem?.id) { mutableStateOf(editItem?.imageUrl) }
    var isLoading by remember(editItem?.id) { mutableStateOf(false) }
    var message by remember(editItem?.id) { mutableStateOf<String?>(null) }
    var imageGenError by remember(editItem?.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Track if image generation is in progress for this item
    val generatingEntities by BackgroundImageGenerationManager.generatingEntities.collectAsState()
    val isImageGenerating = editItem?.id?.let { it in generatingEntities } ?: false

    // Lock state
    var lockedBy by remember(editItem?.id) { mutableStateOf(editItem?.lockedBy) }
    var lockerName by remember(editItem?.id) { mutableStateOf<String?>(null) }
    val isLocked = lockedBy != null

    // Combined disabled state: not authenticated OR locked OR image generating
    val isNotAuthenticated = currentUser == null
    val isDisabled = isNotAuthenticated || isLocked || isImageGenerating

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Fetch locker's name when item is locked
    LaunchedEffect(lockedBy) {
        val lockerId = lockedBy
        if (lockerId != null) {
            ApiClient.getUser(lockerId).onSuccess { user ->
                lockerName = user?.name
            }
        } else {
            lockerName = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isEditMode) "Edit Item" else "Create Item",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            // Lock status visible to all users in edit mode
            if (isEditMode && editItem != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLocked && lockerName != null) {
                        Text(
                            text = "Locked by $lockerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Only admins can toggle the lock and delete
                    if (isAdmin) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    currentUser?.let { user ->
                                        ApiClient.toggleItemLock(editItem.id, user.id)
                                            .onSuccess { updatedItem ->
                                                lockedBy = updatedItem.lockedBy
                                                onItemUpdated(updatedItem)
                                            }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = if (isLocked) "Unlock item" else "Lock item",
                                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Show delete button only when unlocked
                        if (!isLocked) {
                            IconButton(
                                onClick = { showDeleteDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete item",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (isLocked) "Item is locked" else "Item is unlocked",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        if (isEditMode) {
            // Display image at top when editing (only if image exists or is generating)
            EntityImage(
                imageUrl = imageUrl,
                contentDescription = "Image of ${editItem?.name ?: "item"}",
                isGenerating = isImageGenerating
            )

            if (isAdmin) {
                Text(
                    text = "ID: ${editItem?.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Item Name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isDisabled
            )
            GenButton(
                entityType = GenEntityType.ITEM,
                currentName = name,
                currentDesc = desc,
                onGenerated = { genName, genDesc ->
                    name = genName
                    desc = genDesc
                },
                enabled = !isDisabled
            )
        }

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            enabled = !isDisabled
        )

        OutlinedTextField(
            value = featureIds,
            onValueChange = { featureIds = it },
            label = { Text("Feature IDs (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isDisabled
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save & Gen Image / Create & Gen Image button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        imageGenError = null
                        val request = CreateItemRequest(
                            name = name,
                            desc = desc,
                            featureIds = featureIds.splitToList()
                        )
                        if (isEditMode) {
                            val result = ApiClient.updateItem(editItem!!.id, request)
                            isLoading = false
                            if (result.isSuccess) {
                                // Start background image generation after successful save
                                BackgroundImageGenerationManager.startGeneration(
                                    entityType = "item",
                                    entityId = editItem.id,
                                    name = name,
                                    description = desc,
                                    featureIds = featureIds.splitToList()
                                )
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        } else {
                            val result = ApiClient.createItem(request)
                            isLoading = false
                            result.onSuccess { createdItem ->
                                // Start background image generation after successful create
                                BackgroundImageGenerationManager.startGeneration(
                                    entityType = "item",
                                    entityId = createdItem.id,
                                    name = name,
                                    description = desc,
                                    featureIds = featureIds.splitToList()
                                )
                                onSaved()
                            }.onFailure { error ->
                                message = "Error: ${error.message}"
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && desc.isNotBlank() && !isDisabled && !isImageGenerating
            ) {
                if (isImageGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditMode) "Save & Gen Image" else "Create & Gen Image")
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        val request = CreateItemRequest(
                            name = name,
                            desc = desc,
                            featureIds = featureIds.splitToList()
                        )
                        if (isEditMode) {
                            val result = ApiClient.updateItem(editItem!!.id, request)
                            isLoading = false
                            if (result.isSuccess) {
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        } else {
                            val result = ApiClient.createItem(request)
                            isLoading = false
                            if (result.isSuccess) {
                                onSaved()
                            } else {
                                message = "Error: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && !isDisabled
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isEditMode) "Save Changes" else "Create")
                }
            }
        }

        imageGenError?.let {
            SelectionContainer {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }

        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        // Delete confirmation dialog
        if (showDeleteDialog && editItem != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Item") },
                text = {
                    Text("Are you sure you want to delete \"${editItem.name}\"?\n\nThis will also remove this item from any locations.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                isDeleting = true
                                ApiClient.deleteItem(editItem.id)
                                    .onSuccess {
                                        showDeleteDialog = false
                                        onBack()
                                    }
                                    .onFailure { error ->
                                        message = "Failed to delete: ${error.message}"
                                        showDeleteDialog = false
                                    }
                                isDeleting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Delete")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ItemDetailView(
    itemId: String,
    onBack: () -> Unit,
    onEdit: (ItemDto) -> Unit,
    onCreateNew: () -> Unit,
    isAdmin: Boolean = false
) {
    var item by remember { mutableStateOf<ItemDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        isLoading = true
        ApiClient.getItem(itemId).onSuccess {
            item = it
            notFound = it == null
        }.onFailure {
            notFound = true
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Item Details",
                style = MaterialTheme.typography.titleLarge
            )
        }

        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            notFound -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Item not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "ID: $itemId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCreateNew) {
                            Text("Create New Item")
                        }
                    }
                }
            }
            item != null -> {
                // Display image at top
                EntityImage(
                    imageUrl = item!!.imageUrl,
                    contentDescription = "Image of ${item!!.name}"
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = item!!.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (isAdmin) {
                            Text(
                                text = "ID: ${item!!.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item!!.desc,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (item!!.featureIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Features: ${item!!.featureIds.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Button(
                    onClick = { onEdit(item!!) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Edit Item")
                }
            }
        }
    }
}

private fun String.splitToList(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Admin panel view with file upload functionality
 */
@Composable
fun AdminPanelView(
    onViewAuditLogs: () -> Unit
) {
    var uploadedFiles by remember { mutableStateOf<List<UploadedFileDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilePath by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var allowedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    // Load uploaded files and allowed types on mount
    LaunchedEffect(Unit) {
        isLoading = true
        ApiClient.getUploadedFiles().onSuccess { files ->
            uploadedFiles = files
        }
        ApiClient.getAllowedFileTypes().onSuccess { types ->
            allowedTypes = types
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Admin Panel",
            style = MaterialTheme.typography.titleLarge
        )

        // Audit Logs Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewAuditLogs() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Audit Logs",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "View history of all data changes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // File Upload Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "File Upload",
                    style = MaterialTheme.typography.titleMedium
                )

                if (allowedTypes.isNotEmpty()) {
                    Text(
                        text = "Allowed file types: ${allowedTypes.sorted().joinToString(", ") { ".$it" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = selectedFilePath,
                    onValueChange = { selectedFilePath = it },
                    label = { Text("File path on disk") },
                    placeholder = { Text("/path/to/file.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (selectedFilePath.isBlank()) {
                                    uploadMessage = "Please enter a file path"
                                    return@launch
                                }

                                isUploading = true
                                uploadMessage = null

                                try {
                                    val fileResult = readFileBytes(selectedFilePath)
                                    if (fileResult == null) {
                                        uploadMessage = "File not found or cannot be read: $selectedFilePath"
                                        isUploading = false
                                        return@launch
                                    }

                                    if (fileResult.extension !in allowedTypes) {
                                        uploadMessage = "File type .${fileResult.extension} not allowed"
                                        isUploading = false
                                        return@launch
                                    }

                                    ApiClient.uploadFile(fileResult.filename, fileResult.bytes).onSuccess { response ->
                                        if (response.success) {
                                            uploadMessage = "File uploaded successfully: ${response.url}"
                                            selectedFilePath = ""
                                            // Refresh the file list
                                            ApiClient.getUploadedFiles().onSuccess { files ->
                                                uploadedFiles = files
                                            }
                                        } else {
                                            uploadMessage = "Upload failed: ${response.error}"
                                        }
                                    }.onFailure { error ->
                                        uploadMessage = "Upload error: ${error.message}"
                                    }
                                } catch (e: Exception) {
                                    uploadMessage = "Error: ${e.message}"
                                }

                                isUploading = false
                            }
                        },
                        enabled = !isUploading && selectedFilePath.isNotBlank()
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload")
                        }
                    }
                }

                uploadMessage?.let {
                    SelectionContainer {
                        Text(
                            text = it,
                            color = if (it.startsWith("Error") || it.startsWith("Upload failed") || it.startsWith("File not") || it.startsWith("File type"))
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Uploaded Files List
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Uploaded Files",
                    style = MaterialTheme.typography.titleMedium
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (uploadedFiles.isEmpty()) {
                    Text(
                        text = "No files uploaded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uploadedFiles.forEach { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    SelectionContainer {
                                        Text(
                                            text = file.filename,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    SelectionContainer {
                                        Text(
                                            text = file.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = formatFileSize(file.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            ApiClient.deleteUploadedFile(file.filename).onSuccess { deleted ->
                                                if (deleted) {
                                                    uploadedFiles = uploadedFiles.filter { it.filename != file.filename }
                                                    uploadMessage = "File deleted"
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Audit logs view showing history of all data changes
 */
@Composable
fun AuditLogsView(
    onBack: () -> Unit
) {
    var auditLogs by remember { mutableStateOf<List<AuditLogDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Location", "Creature", "Item", "Feature")

    // Load audit logs
    LaunchedEffect(selectedFilter) {
        isLoading = true
        val result = if (selectedFilter == "All") {
            ApiClient.getAuditLogs(limit = 200)
        } else {
            ApiClient.getAuditLogsByType(selectedFilter, limit = 200)
        }
        result.onSuccess { logs ->
            auditLogs = logs
        }
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Audit Logs",
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        // Logs list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (auditLogs.isEmpty()) {
            Text(
                text = "No audit logs found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(auditLogs) { log ->
                    AuditLogItem(log)
                }
            }
        }
    }
}

@Composable
private fun AuditLogItem(log: AuditLogDto) {
    val actionColor = when (log.action) {
        "CREATE" -> Color(0xFF4CAF50) // Green
        "UPDATE" -> Color(0xFF2196F3) // Blue
        "DELETE" -> Color(0xFFF44336) // Red
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Action badge
                    Surface(
                        color = actionColor.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = log.action,
                            style = MaterialTheme.typography.labelSmall,
                            color = actionColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    // Record type badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = log.recordType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                // Timestamp
                Text(
                    text = formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Record name
            Text(
                text = log.recordName,
                style = MaterialTheme.typography.bodyMedium
            )

            // User info
            Text(
                text = "by ${log.userName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dialog for adjusting terrain rendering parameters for a location
 */
@Composable
private fun TerrainSettingsDialog(
    location: LocationDto,
    currentOverrides: TerrainOverridesDto?,
    onDismiss: () -> Unit,
    onSave: (TerrainOverridesDto) -> Unit,
    onReset: () -> Unit,
    currentUser: UserDto? = null
) {
    val isNotAuthenticated = currentUser == null
    val isLocked = location.lockedBy != null
    val isDisabled = isNotAuthenticated || isLocked
    // Parse what terrains this location has
    val detectedTerrains = remember(location) {
        parseTerrainFromDescription(location.desc, location.name)
    }

    // Local state for editing
    var overrides by remember(currentOverrides) {
        mutableStateOf(currentOverrides ?: TerrainOverridesDto())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Terrain Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Location name
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = if (isDisabled) 8.dp else 16.dp)
                )

                // Disabled indicator (not authenticated or locked)
                if (isDisabled) {
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = if (isNotAuthenticated) "Not authenticated" else "Locked",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isNotAuthenticated) "Login required to edit settings"
                                   else "Locked by ${location.lockedBy} - settings are read-only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Elevation setting (always shown)
                    val autoElevation = calculateElevationFromTerrain(detectedTerrains)
                    TerrainSection(title = "Elevation") {
                        Text(
                            text = "Auto-detected: ${((autoElevation * 100).toInt() / 100f)} (${elevationDescription(autoElevation)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        SliderWithLabel(
                            label = "Override Elevation",
                            value = overrides.elevation ?: autoElevation,
                            valueRange = -1f..1f,
                            onValueChange = { value ->
                                overrides = overrides.copy(elevation = value)
                            },
                            enabled = !isDisabled
                        )
                        Text(
                            text = elevationDescription(overrides.elevation ?: autoElevation),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (detectedTerrains.isEmpty()) {
                        Text(
                            text = "No adjustable terrain detected for this location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                    // Forest settings
                    if (TerrainType.FOREST in detectedTerrains) {
                        TerrainSection(title = "Forest") {
                            SliderWithLabel(
                                label = "Tree Count",
                                value = (overrides.forest?.treeCount ?: 7).toFloat(),
                                valueRange = 3f..15f,
                                steps = 12,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        forest = (overrides.forest ?: ForestParamsDto()).copy(
                                            treeCount = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Tree Size",
                                value = overrides.forest?.sizeMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        forest = (overrides.forest ?: ForestParamsDto()).copy(
                                            sizeMultiplier = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Lake settings
                    if (TerrainType.LAKE in detectedTerrains) {
                        // Determine if X/Y are linked (both null, both equal, or legacy diameterMultiplier is set)
                        val lakeXYLinked = overrides.lake?.let { lake ->
                            val hasLegacy = lake.diameterMultiplier != null
                            val xEqualsY = lake.diameterMultiplierX == lake.diameterMultiplierY
                            hasLegacy || (lake.diameterMultiplierX == null && lake.diameterMultiplierY == null) || xEqualsY
                        } ?: true

                        TerrainSection(title = "Lake") {
                            // Link X/Y checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Checkbox(
                                    checked = lakeXYLinked,
                                    onCheckedChange = { linked ->
                                        val currentX = overrides.lake?.diameterMultiplierX ?: overrides.lake?.diameterMultiplier ?: 1f
                                        val currentY = overrides.lake?.diameterMultiplierY ?: overrides.lake?.diameterMultiplier ?: 1f
                                        overrides = overrides.copy(
                                            lake = (overrides.lake ?: LakeParamsDto()).copy(
                                                diameterMultiplier = if (linked) currentX else null,
                                                diameterMultiplierX = if (linked) null else currentX,
                                                diameterMultiplierY = if (linked) null else currentY
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                                Text("Link X/Y Size", style = MaterialTheme.typography.bodySmall)
                            }

                            if (lakeXYLinked) {
                                // Single size slider when linked
                                SliderWithLabel(
                                    label = "Lake Size",
                                    value = overrides.lake?.diameterMultiplier ?: overrides.lake?.diameterMultiplierX ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            lake = (overrides.lake ?: LakeParamsDto()).copy(
                                                diameterMultiplier = value,
                                                diameterMultiplierX = null,
                                                diameterMultiplierY = null
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                            } else {
                                // Separate X and Y sliders when unlinked
                                SliderWithLabel(
                                    label = "Width (X)",
                                    value = overrides.lake?.diameterMultiplierX ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            lake = (overrides.lake ?: LakeParamsDto()).copy(
                                                diameterMultiplier = null,
                                                diameterMultiplierX = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                                SliderWithLabel(
                                    label = "Height (Y)",
                                    value = overrides.lake?.diameterMultiplierY ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            lake = (overrides.lake ?: LakeParamsDto()).copy(
                                                diameterMultiplier = null,
                                                diameterMultiplierY = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                            }

                            SliderWithLabel(
                                label = "Shape Points",
                                value = (overrides.lake?.shapePoints ?: 20).toFloat(),
                                valueRange = 8f..32f,
                                steps = 24,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        lake = (overrides.lake ?: LakeParamsDto()).copy(
                                            shapePoints = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Shape Roughness",
                                value = overrides.lake?.noiseScale ?: 0.35f,
                                valueRange = 0f..1f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        lake = (overrides.lake ?: LakeParamsDto()).copy(
                                            noiseScale = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // River settings
                    if (TerrainType.RIVER in detectedTerrains) {
                        TerrainSection(title = "River") {
                            SliderWithLabel(
                                label = "River Width",
                                value = overrides.river?.widthMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        river = RiverParamsDto(widthMultiplier = value)
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Stream settings
                    if (TerrainType.STREAM in detectedTerrains) {
                        TerrainSection(title = "Stream") {
                            SliderWithLabel(
                                label = "Stream Width",
                                value = overrides.stream?.widthMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        stream = StreamParamsDto(widthMultiplier = value)
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Mountain settings
                    if (TerrainType.MOUNTAIN in detectedTerrains) {
                        TerrainSection(title = "Mountains") {
                            SliderWithLabel(
                                label = "Peak Count",
                                value = (overrides.mountain?.peakCount ?: 3).toFloat(),
                                valueRange = 1f..6f,
                                steps = 5,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        mountain = (overrides.mountain ?: MountainParamsDto()).copy(
                                            peakCount = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Mountain Height",
                                value = overrides.mountain?.heightMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        mountain = (overrides.mountain ?: MountainParamsDto()).copy(
                                            heightMultiplier = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Hills settings
                    if (TerrainType.HILLS in detectedTerrains) {
                        TerrainSection(title = "Hills") {
                            SliderWithLabel(
                                label = "Hill Height",
                                value = overrides.hills?.heightMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        hills = HillsParamsDto(heightMultiplier = value)
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Grass settings
                    if (TerrainType.GRASS in detectedTerrains) {
                        TerrainSection(title = "Grass") {
                            SliderWithLabel(
                                label = "Tuft Count",
                                value = (overrides.grass?.tuftCount ?: 8).toFloat(),
                                valueRange = 3f..15f,
                                steps = 12,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        grass = GrassParamsDto(tuftCount = value.toInt())
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Desert settings
                    if (TerrainType.DESERT in detectedTerrains) {
                        TerrainSection(title = "Desert") {
                            SliderWithLabel(
                                label = "Dune Count",
                                value = (overrides.desert?.duneCount ?: 3).toFloat(),
                                valueRange = 1f..6f,
                                steps = 5,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        desert = (overrides.desert ?: DesertParamsDto()).copy(
                                            duneCount = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Dune Height",
                                value = overrides.desert?.heightMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        desert = (overrides.desert ?: DesertParamsDto()).copy(
                                            heightMultiplier = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Swamp settings
                    if (TerrainType.SWAMP in detectedTerrains) {
                        // Determine if X/Y are linked
                        val swampXYLinked = overrides.swamp?.let { swamp ->
                            val xEqualsY = swamp.diameterMultiplierX == swamp.diameterMultiplierY
                            (swamp.diameterMultiplierX == null && swamp.diameterMultiplierY == null) || xEqualsY
                        } ?: true

                        TerrainSection(title = "Swamp") {
                            SliderWithLabel(
                                label = "Density",
                                value = overrides.swamp?.densityMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                            densityMultiplier = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )

                            // Link X/Y checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = swampXYLinked,
                                    onCheckedChange = { linked ->
                                        val currentX = overrides.swamp?.diameterMultiplierX ?: 1f
                                        val currentY = overrides.swamp?.diameterMultiplierY ?: 1f
                                        overrides = overrides.copy(
                                            swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                                diameterMultiplierX = if (linked) null else currentX,
                                                diameterMultiplierY = if (linked) null else currentY
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                                Text("Link X/Y Size", style = MaterialTheme.typography.bodySmall)
                            }

                            if (swampXYLinked) {
                                // Single size slider when linked
                                SliderWithLabel(
                                    label = "Swamp Size",
                                    value = overrides.swamp?.diameterMultiplierX ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                                diameterMultiplierX = value,
                                                diameterMultiplierY = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                            } else {
                                // Separate X and Y sliders when unlinked
                                SliderWithLabel(
                                    label = "Width (X)",
                                    value = overrides.swamp?.diameterMultiplierX ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                                diameterMultiplierX = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                                SliderWithLabel(
                                    label = "Height (Y)",
                                    value = overrides.swamp?.diameterMultiplierY ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                                diameterMultiplierY = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                            }

                            SliderWithLabel(
                                label = "Shape Points",
                                value = (overrides.swamp?.shapePoints ?: 20).toFloat(),
                                valueRange = 8f..32f,
                                steps = 24,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                            shapePoints = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Shape Roughness",
                                value = overrides.swamp?.noiseScale ?: 0.35f,
                                valueRange = 0f..1f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                            noiseScale = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReset, enabled = !isDisabled) {
                        Text("Reset")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(overrides) }, enabled = !isDisabled) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun TerrainSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SliderWithLabel(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.padding(vertical = 4.dp).alpha(if (enabled) 1f else 0.5f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (steps > 0) value.toInt().toString() else "${(value * 10).toInt() / 10.0}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    // Simple relative time formatting
    val now = com.ez2bg.anotherthread.platform.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
