package com.ez2bg.anotherthread.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.Upload
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    USER("User", Icons.Filled.Person),
    LOCATION("Location", Icons.Filled.Place),
    CREATURE("Creature", Icons.Filled.Pets),
    ITEM("Item", SwordIcon),
    ADMIN("Admin", Icons.Filled.AdminPanelSettings)
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
    modifier: Modifier = Modifier
) {
    if (imageUrl != null) {
        val fullUrl = "${AppConfig.api.baseUrl}$imageUrl"
        var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = fullUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
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
    } else {
        // Placeholder when no image exists yet
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
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "Image generating...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
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

sealed class ViewState {
    data object UserAuth : ViewState()
    data class UserProfile(val user: UserDto) : ViewState()
    data class UserDetail(val userId: String) : ViewState()  // View other user's profile (read-only for non-admins)
    data object LocationGraph : ViewState()
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
    var selectedTab by remember { mutableStateOf(if (savedUser != null) AdminTab.LOCATION else AdminTab.USER) }
    var viewState by remember {
        mutableStateOf<ViewState>(
            if (savedUser != null) ViewState.LocationGraph else ViewState.UserAuth
        )
    }
    var currentUser by remember { mutableStateOf(savedUser) }

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

        // Filter tabs - only show Admin tab if user is admin
        val visibleTabs = if (isAdmin) {
            AdminTab.entries
        } else {
            AdminTab.entries.filter { it != AdminTab.ADMIN }
        }

        TabRow(selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)) {
            visibleTabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        viewState = when (tab) {
                            AdminTab.USER -> if (currentUser != null) ViewState.UserProfile(currentUser!!) else ViewState.UserAuth
                            AdminTab.LOCATION -> ViewState.LocationGraph
                            AdminTab.CREATURE -> ViewState.CreatureList
                            AdminTab.ITEM -> ViewState.ItemList
                            AdminTab.ADMIN -> ViewState.AdminPanel
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
                onBack = null  // No back button for own profile
            )
            is ViewState.UserDetail -> UserDetailView(
                userId = state.userId,
                currentUser = currentUser,
                isAdmin = isAdmin,
                onBack = {
                    selectedTab = AdminTab.LOCATION
                    viewState = ViewState.LocationGraph
                },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                }
            )
            is ViewState.LocationGraph -> LocationGraphView(
                onAddClick = { viewState = ViewState.LocationCreate },
                onLocationClick = { location -> viewState = ViewState.LocationEdit(location) },
                isAuthenticated = currentUser != null
            )
            is ViewState.LocationCreate -> LocationForm(
                editLocation = null,
                onBack = { viewState = ViewState.LocationGraph },
                onSaved = { viewState = ViewState.LocationGraph },
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
                isAdmin = isAdmin
            )
            is ViewState.LocationEdit -> LocationForm(
                editLocation = state.location,
                onBack = { viewState = ViewState.LocationGraph },
                onSaved = { viewState = ViewState.LocationGraph },
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
                }
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
                    viewState = ViewState.LocationGraph
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
                }
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
                    viewState = ViewState.LocationGraph
                },
                onEdit = { item ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemEdit(item)
                },
                onCreateNew = {
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemCreate
                }
            )
            is ViewState.AdminPanel -> AdminPanelView()
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
    onBack: (() -> Unit)?  // null for own profile, non-null for viewing others
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
        // Back button for viewing other users
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "User Profile",
                    style = MaterialTheme.typography.titleLarge
                )
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

@Composable
fun LocationGraphView(
    onAddClick: () -> Unit,
    onLocationClick: (LocationDto) -> Unit,
    isAuthenticated: Boolean
) {
    var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = ApiClient.getLocations()
            isLoading = false
            result.onSuccess { locations = it }
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
                    modifier = Modifier.fillMaxSize()
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

@Composable
fun LocationGraph(
    locations: List<LocationDto>,
    onLocationClick: (LocationDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val locationPositions = remember(locations) {
        calculateForceDirectedPositions(locations)
    }

    // Track which location is expanded (null = none expanded)
    var expandedLocationId by remember { mutableStateOf<String?>(null) }

    // Pan offset state with animation support
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .pointerInput(expandedLocationId) {
                detectTransformGestures { _, pan, _, _ ->
                    // Collapse any expanded thumbnail when panning
                    if (expandedLocationId != null) {
                        expandedLocationId = null
                    }
                    scope.launch {
                        offset.snapTo(offset.value + pan)
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
                    translationX = offset.value.x
                    translationY = offset.value.y
                }
        ) {
            // LAYER 2: Terrain for each location
            Canvas(modifier = Modifier.fillMaxSize()) {
                locations.forEach { location ->
                    val pos = locationPositions[location.id] ?: return@forEach
                    val screenPos = getLocationScreenPos(pos)
                    drawLocationTerrain(
                        location = location,
                        center = screenPos,
                        terrainSize = terrainSize
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
                    onClick = {
                        if (isExpanded) {
                            // Already expanded, go to detail
                            onLocationClick(location)
                        } else {
                            // Expand this thumbnail and collapse others
                            expandedLocationId = location.id
                            centerOnLocation(location)
                        }
                    }
                )
            }

            // LAYER 4: Location labels (separate from dots to not affect positioning)
            locations.forEach { location ->
                val pos = locationPositions[location.id] ?: return@forEach
                val isExpanded = expandedLocationId == location.id
                if (!isExpanded) {
                    // Position label centered below the dot
                    // Dot top-left is at dotX, dotY. Dot center is at dotX + 10dp
                    val dotX = (pos.x * (width - boxSizePx) / 2.5f).dp
                    val dotY = (pos.y * (height - boxSizePx) / 2.5f).dp

                    // Use Layout to measure text and center it on dot
                    androidx.compose.ui.layout.Layout(
                        content = {
                            Text(
                                text = location.name.ifBlank { location.id.take(6) },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                fontSize = 8.sp,
                                modifier = Modifier
                                    .background(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        },
                        modifier = Modifier.offset(x = dotX + 10.dp, y = dotY + 22.dp)
                    ) { measurables, constraints ->
                        val placeable = measurables.first().measure(constraints.copy(maxWidth = Int.MAX_VALUE))
                        layout(0, 0) {
                            // Center the label horizontally on the dot center (which is at x=0 due to offset)
                            placeable.place(-placeable.width / 2, 0)
                        }
                    }
                }
            }
        }
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
    onClick: () -> Unit
) {
    val collapsedSize = 20.dp
    val expandedSize = 100.dp

    val hasImage = location.imageUrl != null

    // Get terrain-based color for collapsed state
    val terrainColor = remember(location) {
        getTerrainColor(location.desc, location.name)
    }

    if (isExpanded) {
        // Expanded state: 100x100 thumbnail centered on where the dot center was
        // The modifier positions us at the dot's top-left corner
        // Dot center is at (10dp, 10dp) from that position
        // We want the 100x100 box center (50dp, 50dp) to align with dot center
        // So offset by: 10 - 50 = -40dp in both directions
        Box(
            modifier = modifier
                .offset(x = (-40).dp, y = (-40).dp)
                .size(expandedSize)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (hasImage) {
                val fullUrl = "${AppConfig.api.baseUrl}${location.imageUrl}"
                var imageState by remember {
                    mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
                }

                AsyncImage(
                    model = fullUrl,
                    contentDescription = location.name,
                    modifier = Modifier.fillMaxSize(),
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
                if (imageState is AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
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
    } else {
        // Collapsed state: just the dot (label is rendered separately in parent)
        Box(
            modifier = modifier
                .size(collapsedSize)
                .background(terrainColor, CircleShape)
                .border(1.5.dp, DotBorderColor.copy(alpha = 0.6f), CircleShape)
                .clickable(onClick = onClick)
        )
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = location.name.ifBlank { location.id.take(8) },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(8.dp)
        )
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
    val treeDark = Color(0xFF3A5731)       // Darker trunk/shadow
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
    ROAD, FOREST, WATER, STREAM, MOUNTAIN, GRASS, BUILDING, CAVE, DESERT,
    COAST, HILLS, SWAMP, CHURCH, CASTLE, PORT, RUINS
}

// Helper extension for keyword matching
private fun String.containsAny(vararg keywords: String): Boolean {
    return keywords.any { this.contains(it) }
}

// Parse terrain types from location description
private fun parseTerrainFromDescription(desc: String, name: String): Set<TerrainType> {
    val text = (desc + " " + name).lowercase()
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
    if (text.containsAny("river", "water", "lake", "pond", "falls", "fountain")) {
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

// Draw road/path terrain - winding path that passes through center
private fun DrawScope.drawRoadTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val pathWidth = terrainSize * 0.10f

    // Road winds through center so dot appears ON the road
    val entryAngle = random.nextFloat() * 2 * PI.toFloat()
    val exitAngle = entryAngle + PI.toFloat() + (random.nextFloat() - 0.5f) * 0.4f // Slightly off opposite

    val startX = center.x + cos(entryAngle) * terrainSize * 0.75f
    val startY = center.y + sin(entryAngle) * terrainSize * 0.75f
    val endX = center.x + cos(exitAngle) * terrainSize * 0.75f
    val endY = center.y + sin(exitAngle) * terrainSize * 0.75f

    // Create winding path with multiple curves, but ensure it passes through center
    // Control points on either side of center create the winding effect
    val perpAngle = entryAngle + PI.toFloat() / 2
    val curve1Offset = terrainSize * 0.15f * (0.5f + random.nextFloat() * 0.5f)
    val curve2Offset = terrainSize * 0.15f * (0.5f + random.nextFloat() * 0.5f)

    // First segment: start to center (curves one way)
    val ctrl1X = (startX + center.x) / 2 + cos(perpAngle) * curve1Offset
    val ctrl1Y = (startY + center.y) / 2 + sin(perpAngle) * curve1Offset

    // Second segment: center to end (curves the other way for S-shape)
    val ctrl2X = (center.x + endX) / 2 - cos(perpAngle) * curve2Offset
    val ctrl2Y = (center.y + endY) / 2 - sin(perpAngle) * curve2Offset

    val roadPath = Path().apply {
        moveTo(startX, startY)
        quadraticTo(ctrl1X, ctrl1Y, center.x, center.y)
        quadraticTo(ctrl2X, ctrl2Y, endX, endY)
    }

    drawPath(
        path = roadPath,
        color = TerrainColors.road,
        style = Stroke(width = pathWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// Draw forest terrain - mixed tree styles (conifer and deciduous)
private fun DrawScope.drawForestTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val treeCount = 5 + random.nextInt(4)

    repeat(treeCount) { i ->
        val angle = (i.toFloat() / treeCount) * 2 * PI.toFloat() + random.nextFloat() * 0.6f
        val distance = terrainSize * (0.3f + random.nextFloat() * 0.3f)
        val treeX = center.x + cos(angle) * distance
        val treeY = center.y + sin(angle) * distance
        val treeSize = terrainSize * (0.08f + random.nextFloat() * 0.06f)

        // Randomly choose tree type
        val isConifer = random.nextFloat() > 0.4f

        if (isConifer) {
            // Conifer tree (triangular with multiple tiers like vintage maps)
            val treePath = Path().apply {
                // Bottom tier
                moveTo(treeX, treeY - treeSize * 0.3f)
                lineTo(treeX - treeSize * 0.5f, treeY)
                lineTo(treeX + treeSize * 0.5f, treeY)
                close()
            }
            drawPath(treePath, color = TerrainColors.tree)

            // Middle tier
            val midPath = Path().apply {
                moveTo(treeX, treeY - treeSize * 0.7f)
                lineTo(treeX - treeSize * 0.4f, treeY - treeSize * 0.2f)
                lineTo(treeX + treeSize * 0.4f, treeY - treeSize * 0.2f)
                close()
            }
            drawPath(midPath, color = TerrainColors.tree)

            // Top tier
            val topPath = Path().apply {
                moveTo(treeX, treeY - treeSize)
                lineTo(treeX - treeSize * 0.25f, treeY - treeSize * 0.5f)
                lineTo(treeX + treeSize * 0.25f, treeY - treeSize * 0.5f)
                close()
            }
            drawPath(topPath, color = TerrainColors.tree)
        } else {
            // Deciduous tree (round canopy like vintage maps)
            // Trunk
            drawLine(
                color = TerrainColors.treeDark,
                start = Offset(treeX, treeY),
                end = Offset(treeX, treeY - treeSize * 0.4f),
                strokeWidth = 2f
            )

            // Canopy (cloud-like shape)
            val canopyY = treeY - treeSize * 0.6f
            drawCircle(
                color = TerrainColors.tree,
                radius = treeSize * 0.35f,
                center = Offset(treeX, canopyY)
            )
            drawCircle(
                color = TerrainColors.tree,
                radius = treeSize * 0.28f,
                center = Offset(treeX - treeSize * 0.2f, canopyY + treeSize * 0.1f)
            )
            drawCircle(
                color = TerrainColors.tree,
                radius = treeSize * 0.28f,
                center = Offset(treeX + treeSize * 0.2f, canopyY + treeSize * 0.1f)
            )
        }

        // Trunk for conifer
        if (isConifer) {
            drawLine(
                color = TerrainColors.treeDark,
                start = Offset(treeX, treeY),
                end = Offset(treeX, treeY + treeSize * 0.2f),
                strokeWidth = 2f
            )
        }
    }
}

// Draw water terrain - wavy lines
private fun DrawScope.drawWaterTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val waveCount = 2 + random.nextInt(2)

    repeat(waveCount) { i ->
        val offsetY = (i - waveCount / 2f) * terrainSize * 0.12f
        val startX = center.x - terrainSize * 0.4f
        val endX = center.x + terrainSize * 0.4f

        val waterPath = Path().apply {
            moveTo(startX, center.y + offsetY)
            val waveHeight = terrainSize * 0.06f
            quadraticTo(startX + terrainSize * 0.13f, center.y + offsetY - waveHeight,
                startX + terrainSize * 0.27f, center.y + offsetY)
            quadraticTo(startX + terrainSize * 0.4f, center.y + offsetY + waveHeight,
                startX + terrainSize * 0.53f, center.y + offsetY)
            quadraticTo(startX + terrainSize * 0.67f, center.y + offsetY - waveHeight,
                endX, center.y + offsetY)
        }

        drawPath(
            path = waterPath,
            color = TerrainColors.water.copy(alpha = 0.7f),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

// Draw stream terrain - a winding, dark stream passing through center
private fun DrawScope.drawStreamTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val streamColor = Color(0xFF2A4A5A) // Dark blue-gray stream color
    val highlightColor = Color(0xFF4A6A7A) // Lighter highlight

    // Stream winds through center so dot appears ON the stream (like a bridge over it)
    val entryAngle = random.nextFloat() * 2 * PI.toFloat()
    val exitAngle = entryAngle + PI.toFloat() + (random.nextFloat() - 0.5f) * 0.3f

    // Stream extends well beyond the terrain area for a longer, more natural look
    val startX = center.x + cos(entryAngle) * terrainSize * 0.95f
    val startY = center.y + sin(entryAngle) * terrainSize * 0.95f
    val endX = center.x + cos(exitAngle) * terrainSize * 0.95f
    val endY = center.y + sin(exitAngle) * terrainSize * 0.95f

    // Create winding stream with multiple curves passing through center
    val perpAngle = entryAngle + PI.toFloat() / 2
    val curve1Offset = terrainSize * 0.18f * (0.6f + random.nextFloat() * 0.4f)
    val curve2Offset = terrainSize * 0.18f * (0.6f + random.nextFloat() * 0.4f)

    // Midpoints between start/center and center/end
    val mid1X = (startX + center.x) / 2
    val mid1Y = (startY + center.y) / 2
    val mid2X = (center.x + endX) / 2
    val mid2Y = (center.y + endY) / 2

    // Control points create S-curve winding effect
    val ctrl1X = mid1X + cos(perpAngle) * curve1Offset
    val ctrl1Y = mid1Y + sin(perpAngle) * curve1Offset
    val ctrl2X = mid2X - cos(perpAngle) * curve2Offset
    val ctrl2Y = mid2Y - sin(perpAngle) * curve2Offset

    val streamPath = Path().apply {
        moveTo(startX, startY)
        quadraticTo(ctrl1X, ctrl1Y, center.x, center.y)
        quadraticTo(ctrl2X, ctrl2Y, endX, endY)
    }

    // Draw main stream
    drawPath(
        path = streamPath,
        color = streamColor,
        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Add water highlight on one edge for depth
    val highlightPath = Path().apply {
        moveTo(startX + 1.5f, startY + 1.5f)
        quadraticTo(ctrl1X + 1.5f, ctrl1Y + 1.5f, center.x + 1.5f, center.y + 1.5f)
        quadraticTo(ctrl2X + 1.5f, ctrl2Y + 1.5f, endX + 1.5f, endY + 1.5f)
    }
    drawPath(
        path = highlightPath,
        color = highlightColor.copy(alpha = 0.4f),
        style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// Draw mountain terrain - triangular peaks with vintage-style shading
private fun DrawScope.drawMountainTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val peakCount = 2 + random.nextInt(2)

    repeat(peakCount) { i ->
        val offsetX = (i - peakCount / 2f) * terrainSize * 0.22f
        val peakHeight = terrainSize * (0.28f + random.nextFloat() * 0.15f)
        val peakWidth = terrainSize * (0.16f + random.nextFloat() * 0.08f)
        val baseY = center.y + terrainSize * 0.15f
        val peakX = center.x + offsetX

        // Main mountain shape (filled)
        val mountainPath = Path().apply {
            moveTo(peakX - peakWidth, baseY)
            lineTo(peakX, baseY - peakHeight)
            lineTo(peakX + peakWidth, baseY)
            close()
        }
        drawPath(mountainPath, color = TerrainColors.mountain.copy(alpha = 0.6f))

        // Outline
        drawPath(mountainPath, color = TerrainColors.ink.copy(alpha = 0.5f), style = Stroke(width = 1.5f))

        // Vintage-style shading lines on the right side (shadow side)
        val shadingLines = 4 + random.nextInt(3)
        repeat(shadingLines) { j ->
            val lineProgress = (j + 1).toFloat() / (shadingLines + 1)
            val lineStartY = baseY - peakHeight * (1f - lineProgress * 0.7f)
            val lineEndY = baseY
            val lineX = peakX + peakWidth * (0.1f + lineProgress * 0.7f)
            val lineTopX = peakX + peakWidth * lineProgress * 0.3f

            drawLine(
                color = TerrainColors.ink.copy(alpha = 0.25f - j * 0.03f),
                start = Offset(lineTopX, lineStartY),
                end = Offset(lineX, lineEndY),
                strokeWidth = 1f
            )
        }

        // Snow cap for taller peaks
        if (peakHeight > terrainSize * 0.32f) {
            val snowPath = Path().apply {
                val snowLine = baseY - peakHeight * 0.75f
                moveTo(peakX - peakWidth * 0.25f, snowLine)
                lineTo(peakX, baseY - peakHeight)
                lineTo(peakX + peakWidth * 0.25f, snowLine)
                close()
            }
            drawPath(snowPath, color = TerrainColors.mountainSnow)
        }
    }
}

// Draw grass terrain - small tufts
private fun DrawScope.drawGrassTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val tuftCount = 6 + random.nextInt(4)

    repeat(tuftCount) {
        val angle = random.nextFloat() * 2 * PI.toFloat()
        val distance = terrainSize * (0.25f + random.nextFloat() * 0.35f)
        val tuftX = center.x + cos(angle) * distance
        val tuftY = center.y + sin(angle) * distance

        repeat(3) { j ->
            val lineAngle = -PI.toFloat() / 2 + (j - 1) * PI.toFloat() / 8
            val lineLength = terrainSize * 0.05f
            drawLine(
                color = TerrainColors.grass.copy(alpha = 0.6f),
                start = Offset(tuftX, tuftY),
                end = Offset(tuftX + cos(lineAngle) * lineLength, tuftY + sin(lineAngle) * lineLength),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
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

// Draw desert terrain - dune curves and dots
private fun DrawScope.drawDesertTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    repeat(3) { i ->
        val offsetY = (i - 1) * terrainSize * 0.15f
        val dunePath = Path().apply {
            moveTo(center.x - terrainSize * 0.35f, center.y + offsetY)
            quadraticTo(
                center.x - terrainSize * 0.08f, center.y + offsetY - terrainSize * 0.08f,
                center.x + terrainSize * 0.15f, center.y + offsetY
            )
        }
        drawPath(
            path = dunePath,
            color = TerrainColors.sand.copy(alpha = 0.5f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }

    repeat(8) {
        val dotX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.7f
        val dotY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.7f
        drawCircle(
            color = TerrainColors.sand.copy(alpha = 0.4f),
            radius = 2f,
            center = Offset(dotX, dotY)
        )
    }
}

// Draw hills terrain - rounded bumps with shading (like vintage map style)
private fun DrawScope.drawHillsTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)
    val hillCount = 2 + random.nextInt(3)

    repeat(hillCount) { i ->
        val angle = (i.toFloat() / hillCount) * 2 * PI.toFloat() + random.nextFloat() * 0.8f
        val distance = terrainSize * (0.2f + random.nextFloat() * 0.25f)
        val hillX = center.x + cos(angle) * distance
        val hillY = center.y + sin(angle) * distance
        val hillWidth = terrainSize * (0.12f + random.nextFloat() * 0.06f)
        val hillHeight = hillWidth * 0.5f

        // Draw hill as arc with shading lines
        val hillPath = Path().apply {
            moveTo(hillX - hillWidth, hillY)
            quadraticTo(hillX, hillY - hillHeight, hillX + hillWidth, hillY)
        }
        drawPath(hillPath, color = TerrainColors.mountain.copy(alpha = 0.7f), style = Stroke(width = 2f))

        // Add hatching/shading lines on right side
        repeat(3) { j ->
            val lineX = hillX + hillWidth * (0.2f + j * 0.25f)
            val lineTop = hillY - hillHeight * (0.8f - j * 0.2f)
            drawLine(
                color = TerrainColors.ink.copy(alpha = 0.3f),
                start = Offset(lineX, lineTop),
                end = Offset(lineX + hillWidth * 0.1f, hillY),
                strokeWidth = 1f
            )
        }
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
private fun DrawScope.drawSwampTerrain(center: Offset, terrainSize: Float, seed: Int) {
    val random = kotlin.random.Random(seed)

    // Murky pools
    repeat(3) {
        val poolX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.5f
        val poolY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.5f
        drawOval(
            color = TerrainColors.swamp.copy(alpha = 0.3f),
            topLeft = Offset(poolX - terrainSize * 0.08f, poolY - terrainSize * 0.05f),
            size = androidx.compose.ui.geometry.Size(terrainSize * 0.16f, terrainSize * 0.1f)
        )
    }

    // Reeds/cattails
    repeat(6 + random.nextInt(4)) {
        val reedX = center.x + (random.nextFloat() - 0.5f) * terrainSize * 0.6f
        val reedY = center.y + (random.nextFloat() - 0.5f) * terrainSize * 0.6f
        val reedHeight = terrainSize * (0.06f + random.nextFloat() * 0.04f)

        // Reed stem
        drawLine(
            color = TerrainColors.swamp,
            start = Offset(reedX, reedY),
            end = Offset(reedX, reedY - reedHeight),
            strokeWidth = 1f
        )
        // Reed head
        drawOval(
            color = TerrainColors.swamp.copy(alpha = 0.8f),
            topLeft = Offset(reedX - 2f, reedY - reedHeight - 4f),
            size = androidx.compose.ui.geometry.Size(4f, 6f)
        )
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

// Master terrain drawing function
private fun DrawScope.drawLocationTerrain(
    location: LocationDto,
    center: Offset,
    terrainSize: Float
) {
    val terrains = parseTerrainFromDescription(location.desc, location.name)
    val seed = location.id.hashCode()

    // Draw in specific order (background terrain first, then structures on top)
    // Background/base terrain
    if (TerrainType.DESERT in terrains) drawDesertTerrain(center, terrainSize, seed)
    if (TerrainType.GRASS in terrains) drawGrassTerrain(center, terrainSize, seed)
    if (TerrainType.SWAMP in terrains) drawSwampTerrain(center, terrainSize, seed + 10)

    // Water features
    if (TerrainType.COAST in terrains) drawCoastTerrain(center, terrainSize, seed + 7)
    if (TerrainType.WATER in terrains) drawWaterTerrain(center, terrainSize, seed + 1)
    if (TerrainType.STREAM in terrains) drawStreamTerrain(center, terrainSize, seed + 14)
    if (TerrainType.PORT in terrains) drawPortTerrain(center, terrainSize, seed + 11)

    // Elevated terrain
    if (TerrainType.HILLS in terrains) drawHillsTerrain(center, terrainSize, seed + 8)
    if (TerrainType.MOUNTAIN in terrains) drawMountainTerrain(center, terrainSize, seed + 2)

    // Infrastructure (roads drawn before vegetation so trees appear on top)
    if (TerrainType.ROAD in terrains) drawRoadTerrain(center, terrainSize, seed + 4)

    // Vegetation (trees on top of roads)
    if (TerrainType.FOREST in terrains) drawForestTerrain(center, terrainSize, seed + 3)

    // Structures (on top)
    if (TerrainType.RUINS in terrains) drawRuinsTerrain(center, terrainSize, seed + 12)
    if (TerrainType.CAVE in terrains) drawCaveTerrain(center, terrainSize, seed + 5)
    if (TerrainType.BUILDING in terrains) drawBuildingTerrain(center, terrainSize, seed + 6)
    if (TerrainType.CHURCH in terrains) drawChurchTerrain(center, terrainSize, seed + 9)
    if (TerrainType.CASTLE in terrains) drawCastleTerrain(center, terrainSize, seed + 13)
}

private data class NodeState(
    val id: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

/**
 * Calculate positions using force-directed layout algorithm.
 * Connected locations are attracted to each other, all locations repel.
 */
private fun calculateForceDirectedPositions(
    locations: List<LocationDto>
): Map<String, LocationPosition> {
    if (locations.isEmpty()) return emptyMap()
    if (locations.size == 1) {
        return mapOf(locations[0].id to LocationPosition(locations[0], 0.5f, 0.5f))
    }

    // Build adjacency map for quick connection lookup
    val connections = locations.associate { loc ->
        loc.id to loc.exitIds.toSet()
    }

    // Initialize nodes with positions around center
    val nodes = locations.mapIndexed { index, location ->
        val angle = (2 * PI * index / locations.size).toFloat()
        NodeState(
            id = location.id,
            x = 0.5f + 0.3f * cos(angle),
            y = 0.5f + 0.3f * sin(angle)
        )
    }
    val nodeMap = nodes.associateBy { it.id }

    // Run simulation
    repeat(LayoutConstants.ITERATIONS) {
        // Calculate forces
        nodes.forEach { node ->
            var fx = 0f
            var fy = 0f

            // Repulsion from all other nodes
            nodes.forEach { other ->
                if (other.id != node.id) {
                    val dx = node.x - other.x
                    val dy = node.y - other.y
                    val distance = sqrt(dx * dx + dy * dy)
                        .coerceAtLeast(LayoutConstants.MIN_DISTANCE)
                    val force = LayoutConstants.REPULSION_STRENGTH / (distance * distance)
                    fx += (dx / distance) * force
                    fy += (dy / distance) * force
                }
            }

            // Attraction to connected nodes
            connections[node.id]?.forEach { connectedId ->
                nodeMap[connectedId]?.let { other ->
                    val dx = other.x - node.x
                    val dy = other.y - node.y
                    val distance = sqrt(dx * dx + dy * dy)
                    if (distance > 0.01f) {
                        fx += dx * LayoutConstants.ATTRACTION_STRENGTH
                        fy += dy * LayoutConstants.ATTRACTION_STRENGTH
                    }
                }
            }

            // Pull toward center
            fx += (0.5f - node.x) * LayoutConstants.CENTER_PULL
            fy += (0.5f - node.y) * LayoutConstants.CENTER_PULL

            // Apply forces to velocity
            node.vx = (node.vx + fx) * LayoutConstants.DAMPING
            node.vy = (node.vy + fy) * LayoutConstants.DAMPING
        }

        // Update positions
        nodes.forEach { node ->
            node.x = (node.x + node.vx).coerceIn(0.1f, 0.9f)
            node.y = (node.y + node.vy).coerceIn(0.1f, 0.9f)
        }
    }

    // Convert to LocationPosition map
    val locationMap = locations.associateBy { it.id }
    return nodes.associate { node ->
        node.id to LocationPosition(
            location = locationMap[node.id]!!,
            x = node.x,
            y = node.y
        )
    }
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
    onLocationUpdated: (LocationDto) -> Unit = {}
) {
    val isEditMode = editLocation != null
    var name by remember(editLocation?.id) { mutableStateOf(editLocation?.name ?: "") }
    var desc by remember(editLocation?.id) { mutableStateOf(editLocation?.desc ?: "") }
    var itemIds by remember(editLocation?.id) { mutableStateOf(editLocation?.itemIds ?: emptyList()) }
    var creatureIds by remember(editLocation?.id) { mutableStateOf(editLocation?.creatureIds ?: emptyList()) }
    var exitIds by remember(editLocation?.id) { mutableStateOf(editLocation?.exitIds ?: emptyList()) }
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
    var exitToRemove by remember { mutableStateOf<String?>(null) }
    var showRemoveExitDialog by remember { mutableStateOf(false) }

    // Available options for dropdowns
    var availableItems by remember { mutableStateOf<List<IdOption>>(emptyList()) }
    var availableCreatures by remember { mutableStateOf<List<IdOption>>(emptyList()) }
    var availableLocations by remember { mutableStateOf<List<IdOption>>(emptyList()) }
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
                    // Only admins can toggle the lock
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
            // Display image at top when editing
            EntityImage(
                imageUrl = imageUrl,
                contentDescription = "Image of ${editLocation?.name ?: "location"}"
            )

            Text(
                text = "ID: ${editLocation?.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                exitIds = exitIds,
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

        IdPillSection(
            label = "Exits (Location IDs)",
            ids = exitIds,
            entityType = EntityType.LOCATION,
            availableOptions = availableLocations,
            onPillClick = { id ->
                scope.launch {
                    ApiClient.getLocation(id).onSuccess { loc ->
                        if (loc != null) onNavigateToLocation(loc)
                    }
                }
            },
            onAddId = { id ->
                if (isDisabled) return@IdPillSection
                // Add exit locally
                exitIds = exitIds + id
                // Add bidirectional exit on the other location (if we have an id)
                if (editLocation != null) {
                    scope.launch {
                        ApiClient.getLocation(id).onSuccess { otherLoc ->
                            if (otherLoc != null && editLocation.id !in otherLoc.exitIds) {
                                val updatedExits = otherLoc.exitIds + editLocation.id
                                val updateRequest = CreateLocationRequest(
                                    name = otherLoc.name,
                                    desc = otherLoc.desc,
                                    itemIds = otherLoc.itemIds,
                                    creatureIds = otherLoc.creatureIds,
                                    exitIds = updatedExits,
                                    featureIds = otherLoc.featureIds
                                )
                                ApiClient.updateLocation(otherLoc.id, updateRequest)
                            }
                        }
                    }
                }
            },
            onRemoveId = { id ->
                if (isDisabled) return@IdPillSection
                // Show confirmation dialog for exit removal
                exitToRemove = id
                showRemoveExitDialog = true
            },
            enabled = !isDisabled
        )

        // Exit removal confirmation dialog
        if (showRemoveExitDialog && exitToRemove != null) {
            val exitIdToRemove = exitToRemove!!
            val exitName = availableLocations.find { it.id == exitIdToRemove }?.name ?: exitIdToRemove
            AlertDialog(
                onDismissRequest = {
                    showRemoveExitDialog = false
                    exitToRemove = null
                },
                title = { Text("Remove Exit") },
                text = {
                    Text("Remove exit to \"$exitName\".\n\nShould this be a two-way removal (also remove this location from \"$exitName\"'s exits)?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Two-way removal
                            exitIds = exitIds - exitIdToRemove
                            if (editLocation != null) {
                                scope.launch {
                                    ApiClient.getLocation(exitIdToRemove).onSuccess { otherLoc ->
                                        if (otherLoc != null && editLocation.id in otherLoc.exitIds) {
                                            val updatedExits = otherLoc.exitIds - editLocation.id
                                            val updateRequest = CreateLocationRequest(
                                                name = otherLoc.name,
                                                desc = otherLoc.desc,
                                                itemIds = otherLoc.itemIds,
                                                creatureIds = otherLoc.creatureIds,
                                                exitIds = updatedExits,
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
                                exitIds = exitIds - exitIdToRemove
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
            if (isEditMode) {
                GenerateImageButton(
                    entityType = GenEntityType.LOCATION,
                    entityId = editLocation!!.id,
                    name = name,
                    description = desc,
                    featureIds = features.splitToList(),
                    onImageGenerated = { newImageUrl ->
                        imageUrl = newImageUrl
                    },
                    onError = { imageGenError = it },
                    enabled = !isDisabled
                )
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
                            exitIds = exitIds,
                            featureIds = features.splitToList()
                        )
                        val result = if (isEditMode) {
                            ApiClient.updateLocation(editLocation!!.id, request)
                        } else {
                            ApiClient.createLocation(request)
                        }
                        isLoading = false
                        if (result.isSuccess) {
                            onSaved()
                        } else {
                            message = "Error: ${result.exceptionOrNull()?.message}"
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
                    Text(if (isEditMode) "Update Location" else "Create Location")
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
                    // Only admins can toggle the lock
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
            // Display image at top when editing
            EntityImage(
                imageUrl = imageUrl,
                contentDescription = "Image of ${editCreature?.name ?: "creature"}"
            )

            Text(
                text = "ID: ${editCreature?.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            if (isEditMode) {
                GenerateImageButton(
                    entityType = GenEntityType.CREATURE,
                    entityId = editCreature!!.id,
                    name = name,
                    description = desc,
                    featureIds = features.splitToList(),
                    onImageGenerated = { newImageUrl ->
                        imageUrl = newImageUrl
                    },
                    onError = { imageGenError = it },
                    enabled = !isDisabled
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        val result = ApiClient.createCreature(
                            CreateCreatureRequest(
                                name = name,
                                desc = desc,
                                itemIds = itemIds,
                                featureIds = features.splitToList()
                            )
                        )
                        isLoading = false
                        message = if (result.isSuccess) {
                            name = ""; desc = ""; itemIds = emptyList(); features = ""
                            "Creature created successfully!"
                        } else {
                            "Error: ${result.exceptionOrNull()?.message}"
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
                    Text(if (isEditMode) "Update Creature" else "Create Creature")
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
fun CreatureDetailView(
    creatureId: String,
    onBack: () -> Unit,
    onEdit: (CreatureDto) -> Unit,
    onCreateNew: () -> Unit,
    onNavigateToItem: (String) -> Unit
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
                        Text(
                            text = "ID: ${creature!!.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    // Only admins can toggle the lock
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
            // Display image at top when editing
            EntityImage(
                imageUrl = imageUrl,
                contentDescription = "Image of ${editItem?.name ?: "item"}"
            )

            Text(
                text = "ID: ${editItem?.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            if (isEditMode) {
                GenerateImageButton(
                    entityType = GenEntityType.ITEM,
                    entityId = editItem!!.id,
                    name = name,
                    description = desc,
                    featureIds = featureIds.splitToList(),
                    onImageGenerated = { newImageUrl ->
                        imageUrl = newImageUrl
                    },
                    onError = { imageGenError = it },
                    enabled = !isDisabled
                )
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
                        val result = if (isEditMode) {
                            ApiClient.updateItem(editItem!!.id, request)
                        } else {
                            ApiClient.createItem(request)
                        }
                        isLoading = false
                        if (result.isSuccess) {
                            if (isEditMode) {
                                onSaved()
                            } else {
                                name = ""; desc = ""; featureIds = ""
                                message = "Item created successfully!"
                            }
                        } else {
                            message = "Error: ${result.exceptionOrNull()?.message}"
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
                    Text(if (isEditMode) "Update Item" else "Create Item")
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
fun ItemDetailView(
    itemId: String,
    onBack: () -> Unit,
    onEdit: (ItemDto) -> Unit,
    onCreateNew: () -> Unit
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
                        Text(
                            text = "ID: ${item!!.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
fun AdminPanelView() {
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
