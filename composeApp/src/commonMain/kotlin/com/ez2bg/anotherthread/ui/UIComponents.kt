package com.ez2bg.anotherthread.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Extension function to check if user has a complete profile (class assigned).
 */
fun UserDto.hasCompleteProfile(): Boolean = characterClassId != null

/**
 * Generate a vague description of a creature when the player is blinded.
 * Uses creature properties to give hints without revealing identity.
 */
fun getBlindPresenceDescription(creature: CreatureDto, index: Int): String {
    val descriptions = listOf(
        "A presence nearby",
        "Something lurks here",
        "A shape in the darkness",
        "Movement to your side",
        "A figure you sense",
        "An unknown form"
    )

    // Use creature level/danger to hint at size/threat
    val sizeHint = when {
        creature.level >= 10 -> "massive "
        creature.level >= 7 -> "large "
        creature.level >= 4 -> ""
        creature.level >= 2 -> "small "
        else -> "tiny "
    }

    // Check creature type from description or name for hints
    val typeHint = when {
        creature.desc.contains("undead", ignoreCase = true) ||
        creature.name.contains("skeleton", ignoreCase = true) ||
        creature.name.contains("zombie", ignoreCase = true) -> "cold "

        creature.desc.contains("beast", ignoreCase = true) ||
        creature.desc.contains("animal", ignoreCase = true) -> "bestial "

        creature.desc.contains("humanoid", ignoreCase = true) ||
        creature.name.contains("goblin", ignoreCase = true) ||
        creature.name.contains("orc", ignoreCase = true) -> "humanoid "

        creature.desc.contains("elemental", ignoreCase = true) ||
        creature.name.contains("fire", ignoreCase = true) ||
        creature.name.contains("ice", ignoreCase = true) -> "unnatural "

        else -> ""
    }

    return when (index % 4) {
        0 -> "A ${sizeHint}${typeHint}presence"
        1 -> "Something ${sizeHint}moves nearby"
        2 -> "A ${typeHint}shape in the dark"
        else -> "You sense ${if (sizeHint.isNotEmpty()) "a ${sizeHint.trim()} form" else "something"}"
    }
}

/**
 * Generate a vague description of an item when the player is blinded.
 */
fun getBlindItemDescription(item: ItemDto, index: Int): String {
    // Check item type or name for hints
    val shapeHint = when {
        item.name.contains("sword", ignoreCase = true) ||
        item.name.contains("blade", ignoreCase = true) ||
        item.name.contains("dagger", ignoreCase = true) -> "long, thin object"

        item.name.contains("shield", ignoreCase = true) ||
        item.name.contains("armor", ignoreCase = true) -> "heavy metal object"

        item.name.contains("potion", ignoreCase = true) ||
        item.name.contains("vial", ignoreCase = true) ||
        item.name.contains("flask", ignoreCase = true) -> "small container"

        item.name.contains("scroll", ignoreCase = true) ||
        item.name.contains("book", ignoreCase = true) -> "paper-like object"

        item.name.contains("ring", ignoreCase = true) ||
        item.name.contains("amulet", ignoreCase = true) ||
        item.name.contains("necklace", ignoreCase = true) -> "small trinket"

        item.name.contains("staff", ignoreCase = true) ||
        item.name.contains("wand", ignoreCase = true) -> "wooden implement"

        item.name.contains("coin", ignoreCase = true) ||
        item.name.contains("gold", ignoreCase = true) -> "something metallic"

        item.name.contains("key", ignoreCase = true) -> "small metal object"

        else -> "an object"
    }

    return when (index % 3) {
        0 -> "You feel $shapeHint"
        1 -> "Your hand brushes $shapeHint"
        else -> "Something here - $shapeHint"
    }
}

/**
 * Icon showing creature activity state (wandering, in_combat, idle).
 * - Wandering: Three small footprints
 * - In Combat: Crossed swords
 * - Idle: No icon (transparent)
 */
@Composable
fun CreatureStateIcon(
    state: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val iconSize = size.minDimension
        when (state) {
            "wandering" -> {
                // Draw three small footprints (boot shapes)
                val bootColor = Color(0xFF90CAF9) // Light blue
                val bootWidth = iconSize * 0.25f
                val bootHeight = iconSize * 0.35f

                // Three footprints in a walking pattern
                listOf(
                    Offset(iconSize * 0.15f, iconSize * 0.6f),
                    Offset(iconSize * 0.45f, iconSize * 0.3f),
                    Offset(iconSize * 0.75f, iconSize * 0.55f)
                ).forEach { pos ->
                    // Simple boot shape - oval
                    drawOval(
                        color = bootColor,
                        topLeft = Offset(pos.x - bootWidth / 2, pos.y - bootHeight / 2),
                        size = androidx.compose.ui.geometry.Size(bootWidth, bootHeight)
                    )
                }
            }
            "in_combat" -> {
                // Draw crossed swords
                val swordColor = Color(0xFFEF5350) // Red
                val strokeWidth = iconSize * 0.12f

                // First sword (top-left to bottom-right)
                drawLine(
                    color = swordColor,
                    start = Offset(iconSize * 0.15f, iconSize * 0.15f),
                    end = Offset(iconSize * 0.85f, iconSize * 0.85f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Hilt for first sword
                drawLine(
                    color = swordColor,
                    start = Offset(iconSize * 0.25f, iconSize * 0.35f),
                    end = Offset(iconSize * 0.35f, iconSize * 0.25f),
                    strokeWidth = strokeWidth * 0.8f,
                    cap = StrokeCap.Round
                )

                // Second sword (top-right to bottom-left)
                drawLine(
                    color = swordColor,
                    start = Offset(iconSize * 0.85f, iconSize * 0.15f),
                    end = Offset(iconSize * 0.15f, iconSize * 0.85f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Hilt for second sword
                drawLine(
                    color = swordColor,
                    start = Offset(iconSize * 0.65f, iconSize * 0.25f),
                    end = Offset(iconSize * 0.75f, iconSize * 0.35f),
                    strokeWidth = strokeWidth * 0.8f,
                    cap = StrokeCap.Round
                )
            }
            // "idle" or unknown - no icon drawn
        }
    }
}

/**
 * Component shown when user tries to access creation/adventure tabs without completing their profile.
 */
@Composable
fun IncompleteProfileBlocker(
    onNavigateToProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Profile Incomplete",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You must complete your character profile to create or adventure.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateToProfile) {
            Text("Go to Profile")
        }
    }
}

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

// Custom walking wizard icon for Exploration Mode
val ExplorerIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Explorer",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Wizard hat (pointed)
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 1f)
            lineTo(8f, 7f)
            lineTo(16f, 7f)
            close()
        }
        // Head (diamond approximating circle)
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 7f)
            lineTo(10f, 9f)
            lineTo(12f, 11f)
            lineTo(14f, 9f)
            close()
        }
        // Body/robe
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 11f)
            lineTo(8f, 20f)
            lineTo(10.5f, 20f)
            lineTo(11.5f, 16f)
            lineTo(12.5f, 16f)
            lineTo(13.5f, 20f)
            lineTo(16f, 20f)
            close()
        }
        // Staff
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(17.5f, 4f)
            lineTo(18.5f, 4f)
            lineTo(18.5f, 22f)
            lineTo(17.5f, 22f)
            close()
        }
        // Staff orb (diamond approximating circle)
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(18f, 2f)
            lineTo(16.5f, 4f)
            lineTo(18f, 6f)
            lineTo(19.5f, 4f)
            close()
        }
        // Walking leg forward
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(9.5f, 20f)
            lineTo(10.5f, 20f)
            lineTo(7.5f, 23f)
            lineTo(6.5f, 23f)
            close()
        }
        // Walking leg back
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(13.5f, 20f)
            lineTo(14.5f, 20f)
            lineTo(17.5f, 23f)
            lineTo(16.5f, 23f)
            close()
        }
    }.build()
}

enum class AdminTab(val title: String, val icon: ImageVector) {
    LOCATION("Location", Icons.Filled.Place),
    CREATURE("Creature", Icons.Filled.Pets),
    ITEM("Item", SwordIcon),
    USER("User", Icons.Filled.Person),
    CLASS("Class", Icons.Filled.Star)
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
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(12.dp)),
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
internal fun nextRefreshKey(): Long = ++refreshKeyCounter

/**
 * Game mode enum - determines whether the user is adventuring (playing) or creating (editing).
 */
enum class GameMode {
    ADVENTURE,  // Player mode: navigation, combat, interacting with the world
    CREATE;     // Editor mode: creating/editing locations, creatures, items

    val isAdventure: Boolean get() = this == ADVENTURE
    val isCreate: Boolean get() = this == CREATE
}

sealed class ViewState {
    data object UserAuth : ViewState()
    data class UserProfile(val user: UserDto) : ViewState()
    data class UserDetail(val userId: String) : ViewState()  // View other user's profile (read-only for non-admins)
    data class LocationGraph(val refreshKey: Long = nextRefreshKey()) : ViewState()
    data object LocationCreate : ViewState()
    data class LocationEdit(val location: LocationDto, val gameMode: GameMode = GameMode.CREATE) : ViewState()
    data object CreatureList : ViewState()
    data object CreatureCreate : ViewState()
    data class CreatureEdit(val creature: CreatureDto) : ViewState()
    data class CreatureDetail(val id: String, val gameMode: GameMode = GameMode.CREATE) : ViewState()
    data object ItemList : ViewState()
    data object ItemCreate : ViewState()
    data class ItemEdit(val item: ItemDto) : ViewState()
    data class ItemDetail(val id: String, val gameMode: GameMode = GameMode.CREATE) : ViewState()
    data object AdminPanel : ViewState()
    data object AuditLogs : ViewState()
    data object ClassList : ViewState()
    data object ClassCreate : ViewState()
    data class ClassEdit(val characterClass: CharacterClassDto) : ViewState()
    data class ClassDetail(val classId: String) : ViewState()
    data class AbilityCreate(val classId: String) : ViewState()
    data class AbilityEdit(val ability: AbilityDto) : ViewState()
}
