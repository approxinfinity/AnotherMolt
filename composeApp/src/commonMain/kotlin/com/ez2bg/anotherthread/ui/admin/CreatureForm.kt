package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.ui.BackgroundImageGenerationManager
import com.ez2bg.anotherthread.ui.EntityImage
import com.ez2bg.anotherthread.ui.EntityType
import com.ez2bg.anotherthread.ui.GenButton
import com.ez2bg.anotherthread.ui.GenEntityType
import com.ez2bg.anotherthread.ui.IdOption
import kotlinx.coroutines.launch

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

    // Combat stats state
    var maxHp by remember(editCreature?.id) { mutableStateOf(editCreature?.maxHp?.toString() ?: "10") }
    var baseDamage by remember(editCreature?.id) { mutableStateOf(editCreature?.baseDamage?.toString() ?: "5") }
    var level by remember(editCreature?.id) { mutableStateOf(editCreature?.level?.toString() ?: "1") }
    var experienceValue by remember(editCreature?.id) { mutableStateOf(editCreature?.experienceValue?.toString() ?: "10") }
    var isAggressive by remember(editCreature?.id) { mutableStateOf(editCreature?.isAggressive ?: false) }
    var abilityIds by remember(editCreature?.id) { mutableStateOf(editCreature?.abilityIds ?: emptyList()) }
    var showCombatStats by remember { mutableStateOf(false) }

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

        // Combat Stats Section (collapsible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCombatStats = !showCombatStats },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Combat Stats",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = if (showCombatStats) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showCombatStats) "Collapse" else "Expand"
                    )
                }

                if (showCombatStats) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Level and Experience Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = level,
                            onValueChange = { if (it.all { c -> c.isDigit() }) level = it },
                            label = { Text("Level") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isDisabled
                        )
                        OutlinedTextField(
                            value = experienceValue,
                            onValueChange = { if (it.all { c -> c.isDigit() }) experienceValue = it },
                            label = { Text("XP Value") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isDisabled
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // HP and Damage Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = maxHp,
                            onValueChange = { if (it.all { c -> c.isDigit() }) maxHp = it },
                            label = { Text("Max HP") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isDisabled
                        )
                        OutlinedTextField(
                            value = baseDamage,
                            onValueChange = { if (it.all { c -> c.isDigit() }) baseDamage = it },
                            label = { Text("Base Damage") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isDisabled
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Aggressive toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Dangerous,
                                contentDescription = null,
                                tint = if (isAggressive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Aggressive",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "(auto-attacks players)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAggressive,
                            onCheckedChange = { isAggressive = it },
                            enabled = !isDisabled
                        )
                    }

                    // Summary text
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "L${level.ifEmpty { "1" }} * ${maxHp.ifEmpty { "10" }} HP * ${baseDamage.ifEmpty { "5" }} DMG * ${experienceValue.ifEmpty { "10" }} XP${if (isAggressive) " * Aggressive" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

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
                            featureIds = features.splitToList(),
                            maxHp = maxHp.toIntOrNull() ?: 10,
                            baseDamage = baseDamage.toIntOrNull() ?: 5,
                            abilityIds = abilityIds,
                            level = level.toIntOrNull() ?: 1,
                            experienceValue = experienceValue.toIntOrNull() ?: 10,
                            isAggressive = isAggressive
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
                            featureIds = features.splitToList(),
                            maxHp = maxHp.toIntOrNull() ?: 10,
                            baseDamage = baseDamage.toIntOrNull() ?: 5,
                            abilityIds = abilityIds,
                            level = level.toIntOrNull() ?: 1,
                            experienceValue = experienceValue.toIntOrNull() ?: 10,
                            isAggressive = isAggressive
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
