package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.ui.BackgroundImageGenerationManager
import com.ez2bg.anotherthread.ui.EntityImage
import com.ez2bg.anotherthread.ui.EntityType
import com.ez2bg.anotherthread.ui.ExplorerIcon
import com.ez2bg.anotherthread.ui.GameMode
import com.ez2bg.anotherthread.ui.GenButton
import com.ez2bg.anotherthread.ui.GenEntityType
import com.ez2bg.anotherthread.ui.IdOption
import kotlinx.coroutines.launch

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
    onDeleted: () -> Unit = {},
    gameMode: GameMode = GameMode.CREATE
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

    // Combined disabled state: not authenticated OR locked OR image generating OR Adventure mode
    val isNotAuthenticated = currentUser == null
    val isDisabled = isNotAuthenticated || isLocked || isImageGenerating || gameMode.isAdventure

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
                text = if (isEditMode) "Location Details" else "Create Location",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            // Lock status visible to all users in edit mode
            if (isEditMode && editLocation != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Show status text
                    if (gameMode.isAdventure) {
                        Text(
                            text = "Exploration Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9C27B0)
                        )
                    } else if (isLocked && lockerName != null) {
                        Text(
                            text = "Locked by $lockerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // In exploration mode, just show explorer icon (no lock toggle)
                    if (gameMode.isAdventure) {
                        Icon(
                            imageVector = ExplorerIcon,
                            contentDescription = "Exploration Mode",
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.padding(12.dp)
                        )
                    } else if (isAdmin) {
                        // Only admins can toggle the lock and delete
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
                        // In Adventure mode, show adventurer icon instead
                        Icon(
                            imageVector = when {
                                gameMode.isAdventure -> ExplorerIcon
                                isLocked -> Icons.Filled.Lock
                                else -> Icons.Filled.LockOpen
                            },
                            contentDescription = when {
                                gameMode.isAdventure -> "Adventure Mode"
                                isLocked -> "Location is locked"
                                else -> "Location is unlocked"
                            },
                            tint = when {
                                gameMode.isAdventure -> Color(0xFF9C27B0)
                                isLocked -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
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

        // Enhanced coordinate display with better visual hierarchy
        if (editLocation != null && editLocation.gridX != null && editLocation.gridY != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit, // Using a map-like icon would be better
                        contentDescription = "Location coordinates",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Map Coordinates",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "(${editLocation.gridX}, ${editLocation.gridY})",
        // Enhanced exit management section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Exits",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${exits.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                ExitPillSection(
                    label = "", // Label now handled above
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
            }
        }
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
