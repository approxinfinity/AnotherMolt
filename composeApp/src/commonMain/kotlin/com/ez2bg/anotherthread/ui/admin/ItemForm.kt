package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.ui.BackgroundImageGenerationManager
import com.ez2bg.anotherthread.ui.EntityImage
import com.ez2bg.anotherthread.ui.GenButton
import com.ez2bg.anotherthread.ui.GenEntityType
import kotlinx.coroutines.launch

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
