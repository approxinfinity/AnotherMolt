package com.ez2bg.anotherthread.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.storage.AuthStorage
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AdminTab(val title: String) {
    USER("User"),
    LOCATION("Location"),
    CREATURE("Creature"),
    ITEM("Item"),
    ADMIN("Admin")
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
    onGenerated: (name: String, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Button(
            onClick = {
                scope.launch {
                    isGenerating = true
                    errorMessage = null

                    val result = when (entityType) {
                        GenEntityType.LOCATION -> ApiClient.generateLocationContent(
                            exitIds = exitIds,
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
            enabled = !isGenerating
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
 * Button to generate an image for an entity using Stable Diffusion
 */
@Composable
fun GenerateImageButton(
    entityType: GenEntityType,
    entityId: String,
    name: String,
    description: String,
    featureIds: List<String> = emptyList(),
    onImageGenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isEnabled = name.isNotBlank() && description.isNotBlank() && !isGenerating

    Column(modifier = modifier) {
        Button(
            onClick = {
                scope.launch {
                    isGenerating = true
                    errorMessage = null

                    val entityTypeStr = when (entityType) {
                        GenEntityType.LOCATION -> "location"
                        GenEntityType.CREATURE -> "creature"
                        GenEntityType.ITEM -> "item"
                        GenEntityType.USER -> "user"
                    }

                    ApiClient.generateImage(
                        entityType = entityTypeStr,
                        entityId = entityId,
                        name = name,
                        description = description,
                        featureIds = featureIds
                    ).onSuccess { response ->
                        onImageGenerated(response.imageUrl)
                    }.onFailure { error ->
                        errorMessage = error.message ?: "Failed to generate image"
                    }

                    isGenerating = false
                }
            },
            enabled = isEnabled
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

sealed class ViewState {
    data object UserAuth : ViewState()
    data class UserProfile(val user: UserDto) : ViewState()
    data object LocationGraph : ViewState()
    data object LocationCreate : ViewState()
    data class LocationEdit(val location: LocationDto) : ViewState()
    data object CreatureCreate : ViewState()
    data class CreatureEdit(val creature: CreatureDto) : ViewState()
    data class CreatureDetail(val id: String) : ViewState()
    data object ItemCreate : ViewState()
    data class ItemEdit(val item: ItemDto) : ViewState()
    data class ItemDetail(val id: String) : ViewState()
    data object AdminPanel : ViewState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Admin - Manage Entities",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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
                            AdminTab.CREATURE -> ViewState.CreatureCreate
                            AdminTab.ITEM -> ViewState.ItemCreate
                            AdminTab.ADMIN -> ViewState.AdminPanel
                        }
                    },
                    text = { Text(tab.title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) }
            )
            is ViewState.LocationGraph -> LocationGraphView(
                onAddClick = { viewState = ViewState.LocationCreate },
                onLocationClick = { location -> viewState = ViewState.LocationEdit(location) }
            )
            is ViewState.LocationCreate -> LocationForm(
                editLocation = null,
                onBack = { viewState = ViewState.LocationGraph },
                onSaved = { viewState = ViewState.LocationGraph },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) },
                onNavigateToCreature = { id -> viewState = ViewState.CreatureDetail(id) },
                onNavigateToLocation = { location -> viewState = ViewState.LocationEdit(location) },
                currentUser = currentUser
            )
            is ViewState.LocationEdit -> LocationForm(
                editLocation = state.location,
                onBack = { viewState = ViewState.LocationGraph },
                onSaved = { viewState = ViewState.LocationGraph },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) },
                onNavigateToCreature = { id -> viewState = ViewState.CreatureDetail(id) },
                onNavigateToLocation = { location -> viewState = ViewState.LocationEdit(location) },
                currentUser = currentUser
            )
            is ViewState.CreatureCreate -> CreatureForm(
                editCreature = null,
                onBack = { viewState = ViewState.CreatureCreate },
                onSaved = { viewState = ViewState.CreatureCreate },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) }
            )
            is ViewState.CreatureEdit -> CreatureForm(
                editCreature = state.creature,
                onBack = { viewState = ViewState.CreatureCreate },
                onSaved = { viewState = ViewState.CreatureCreate },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) }
            )
            is ViewState.CreatureDetail -> CreatureDetailView(
                creatureId = state.id,
                onBack = { viewState = ViewState.LocationGraph },
                onEdit = { creature -> viewState = ViewState.CreatureEdit(creature) },
                onCreateNew = { viewState = ViewState.CreatureCreate },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) }
            )
            is ViewState.ItemCreate -> ItemForm(
                editItem = null,
                onBack = { viewState = ViewState.ItemCreate },
                onSaved = { viewState = ViewState.ItemCreate }
            )
            is ViewState.ItemEdit -> ItemForm(
                editItem = state.item,
                onBack = { viewState = ViewState.ItemCreate },
                onSaved = { viewState = ViewState.ItemCreate }
            )
            is ViewState.ItemDetail -> ItemDetailView(
                itemId = state.id,
                onBack = { viewState = ViewState.LocationGraph },
                onEdit = { item -> viewState = ViewState.ItemEdit(item) },
                onCreateNew = { viewState = ViewState.ItemCreate }
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
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        if (!isLoginMode) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
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
    onUserUpdated: (UserDto) -> Unit,
    onLogout: () -> Unit,
    onNavigateToItem: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf(user.desc) }
    var features by remember { mutableStateOf(user.featureIds.joinToString(", ")) }
    var itemIds by remember { mutableStateOf(user.itemIds) }
    var imageUrl by remember { mutableStateOf(user.imageUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
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

        if (isEditing) {
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
                    }
                )
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

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Logout")
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
    modifier: Modifier = Modifier
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
                    onRemove = { onRemoveId(id) }
                )
            }

            // Add button pill
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
    onLocationClick: (LocationDto) -> Unit
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
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
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

data class LocationPosition(val location: LocationDto, val x: Float, val y: Float)

@Composable
fun LocationGraph(
    locations: List<LocationDto>,
    onLocationClick: (LocationDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val locationPositions = remember(locations) {
        calculateLocationPositions(locations)
    }

    // Pan offset state with animation support
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ ->
                    scope.launch {
                        offset.snapTo(offset.value + pan)
                    }
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val boxSize = 100.dp
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.value.x
                    translationY = offset.value.y
                }
        ) {
            // Draw connection lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                locations.forEach { location ->
                    val fromPos = locationPositions[location.id] ?: return@forEach
                    location.exitIds.forEach { exitId ->
                        val toPos = locationPositions[exitId]
                        if (toPos != null) {
                            drawLine(
                                color = Color.Gray,
                                start = Offset(
                                    fromPos.x * (width - boxSizePx) + boxSizePx / 2,
                                    fromPos.y * (height - boxSizePx) + boxSizePx / 2
                                ),
                                end = Offset(
                                    toPos.x * (width - boxSizePx) + boxSizePx / 2,
                                    toPos.y * (height - boxSizePx) + boxSizePx / 2
                                ),
                                strokeWidth = 3f
                            )
                        }
                    }
                }
            }

            // Draw location boxes
            locations.forEach { location ->
                val pos = locationPositions[location.id] ?: return@forEach
                Box(
                    modifier = Modifier
                        .offset(
                            x = (pos.x * (width - boxSizePx) / 2.5f).dp,
                            y = (pos.y * (height - boxSizePx) / 2.5f).dp
                        )
                        .size(boxSize)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            centerOnLocation(location)
                            onLocationClick(location)
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = location.name.ifBlank { location.id.take(8) },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private fun calculateLocationPositions(locations: List<LocationDto>): Map<String, LocationPosition> {
    if (locations.isEmpty()) return emptyMap()

    val positions = mutableMapOf<String, LocationPosition>()
    val count = locations.size

    if (count == 1) {
        positions[locations[0].id] = LocationPosition(locations[0], 0.5f, 0.5f)
    } else {
        locations.forEachIndexed { index, location ->
            val angle = (2 * PI * index / count) - PI / 2
            val radius = 0.35f
            val x = 0.5f + (radius * cos(angle)).toFloat()
            val y = 0.5f + (radius * sin(angle)).toFloat()
            positions[location.id] = LocationPosition(location, x, y)
        }
    }

    return positions
}

@Composable
fun LocationForm(
    editLocation: LocationDto?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToItem: (String) -> Unit,
    onNavigateToCreature: (String) -> Unit,
    onNavigateToLocation: (LocationDto) -> Unit,
    currentUser: UserDto? = null
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
    val scope = rememberCoroutineScope()

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
                style = MaterialTheme.typography.titleLarge
            )
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
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            GenButton(
                entityType = GenEntityType.LOCATION,
                currentName = name,
                currentDesc = desc,
                exitIds = exitIds,
                onGenerated = { genName, genDesc ->
                    name = genName
                    desc = genDesc
                }
            )
        }

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

        IdPillSection(
            label = "Creatures",
            ids = creatureIds,
            entityType = EntityType.CREATURE,
            availableOptions = availableCreatures,
            onPillClick = onNavigateToCreature,
            onAddId = { id -> creatureIds = creatureIds + id },
            onRemoveId = { id -> creatureIds = creatureIds - id }
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
                                style = MaterialTheme.typography.bodyMedium
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
                // Show confirmation dialog for exit removal
                exitToRemove = id
                showRemoveExitDialog = true
            }
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
            singleLine = true
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
                    }
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
                enabled = !isLoading && name.isNotBlank()
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
    onNavigateToItem: (String) -> Unit
) {
    val isEditMode = editCreature != null
    var name by remember { mutableStateOf(editCreature?.name ?: "") }
    var desc by remember { mutableStateOf(editCreature?.desc ?: "") }
    var itemIds by remember { mutableStateOf(editCreature?.itemIds ?: emptyList()) }
    var features by remember { mutableStateOf(editCreature?.featureIds?.joinToString(", ") ?: "") }
    var imageUrl by remember { mutableStateOf(editCreature?.imageUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Available options for dropdown
    var availableItems by remember { mutableStateOf<List<IdOption>>(emptyList()) }

    // Fetch available options on mount
    LaunchedEffect(Unit) {
        ApiClient.getItems().onSuccess { items ->
            availableItems = items.map { IdOption(it.id, it.name) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Edit Creature",
                    style = MaterialTheme.typography.titleLarge
                )
            }

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
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            GenButton(
                entityType = GenEntityType.CREATURE,
                currentName = name,
                currentDesc = desc,
                onGenerated = { genName, genDesc ->
                    name = genName
                    desc = genDesc
                }
            )
        }

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
                    }
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
                enabled = !isLoading && name.isNotBlank()
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
    onSaved: () -> Unit
) {
    val isEditMode = editItem != null
    var name by remember { mutableStateOf(editItem?.name ?: "") }
    var desc by remember { mutableStateOf(editItem?.desc ?: "") }
    var featureIds by remember { mutableStateOf(editItem?.featureIds?.joinToString(", ") ?: "") }
    var imageUrl by remember { mutableStateOf(editItem?.imageUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Edit Item",
                    style = MaterialTheme.typography.titleLarge
                )
            }

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
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            GenButton(
                entityType = GenEntityType.ITEM,
                currentName = name,
                currentDesc = desc,
                onGenerated = { genName, genDesc ->
                    name = genName
                    desc = genDesc
                }
            )
        }

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = featureIds,
            onValueChange = { featureIds = it },
            label = { Text("Feature IDs (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
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
                    }
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        val result = ApiClient.createItem(
                            CreateItemRequest(
                                name = name,
                                desc = desc,
                                featureIds = featureIds.splitToList()
                            )
                        )
                        isLoading = false
                        message = if (result.isSuccess) {
                            name = ""; desc = ""; featureIds = ""
                            "Item created successfully!"
                        } else {
                            "Error: ${result.exceptionOrNull()?.message}"
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank()
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
                                    val file = java.io.File(selectedFilePath)
                                    if (!file.exists()) {
                                        uploadMessage = "File not found: $selectedFilePath"
                                        isUploading = false
                                        return@launch
                                    }

                                    val extension = file.extension.lowercase()
                                    if (extension !in allowedTypes) {
                                        uploadMessage = "File type .$extension not allowed"
                                        isUploading = false
                                        return@launch
                                    }

                                    val fileBytes = file.readBytes()
                                    ApiClient.uploadFile(file.name, fileBytes).onSuccess { response ->
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
