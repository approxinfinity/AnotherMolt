package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.ui.BackgroundImageGenerationManager
import com.ez2bg.anotherthread.ui.EntityImage
import kotlinx.coroutines.launch

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

    // Always-editable state (no separate edit mode)
    var desc by remember(user.id) { mutableStateOf(user.desc) }
    var imageUrl by remember(user.id) { mutableStateOf(user.imageUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var imageGenError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Checkboxes for generation options
    var genClassChecked by remember { mutableStateOf(false) }
    var genImageChecked by remember { mutableStateOf(false) }

    // Class assignment state - using AsyncOperationRepository for lifecycle-independent polling
    var assignedClass by remember { mutableStateOf<CharacterClassDto?>(null) }
    var characterClassId by remember(user.id) { mutableStateOf(user.characterClassId) }
    var classAbilities by remember { mutableStateOf<List<AbilityDto>>(emptyList()) }
    var abilitiesExpanded by remember { mutableStateOf(false) }
    var showRerollConfirmDialog by remember { mutableStateOf(false) }

    // Collect class generation status from the repository
    val classGenerationStatus by AsyncOperationRepository.classGenerationStatus.collectAsState()
    val isClassGenerating = user.id in classGenerationStatus

    // Resume polling on load if class generation was in progress (survives page reload/navigation)
    LaunchedEffect(user.id) {
        val startedAt = user.classGenerationStartedAt
        if (startedAt != null && user.characterClassId == null) {
            // Resume polling via repository (handles timeout internally)
            AsyncOperationRepository.resumeClassGenerationPolling(user.id, startedAt)
        }
    }

    // Collect class generation completions and update UI
    LaunchedEffect(user.id) {
        AsyncOperationRepository.classGenerationCompletions.collect { result ->
            if (result.userId == user.id) {
                if (result.success) {
                    characterClassId = result.user?.characterClassId
                    assignedClass = result.characterClass
                    result.user?.let { onUserUpdated(it) }
                    message = "Class generated!"
                } else {
                    message = result.errorMessage ?: "Class generation failed. Please try again."
                }
            }
        }
    }

    // Track if image generation is in progress
    val generatingEntities by BackgroundImageGenerationManager.generatingEntities.collectAsState()
    val isImageGenerating = user.id in generatingEntities

    // Fetch assigned class details and abilities if user has one
    LaunchedEffect(characterClassId) {
        val classId = characterClassId
        if (classId != null) {
            ApiClient.getCharacterClass(classId).onSuccess { fetchedClass ->
                assignedClass = fetchedClass
            }
            ApiClient.getAbilitiesByClass(classId).onSuccess { abilities ->
                classAbilities = abilities.sortedBy { it.name.lowercase() }
            }
        } else {
            assignedClass = null
            classAbilities = emptyList()
        }
    }

    // Profile incomplete warning - only show for own profile when no class assigned
    val showIncompleteWarning = isOwnProfile && characterClassId == null

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lock icon when viewing someone else's profile and not admin
                    if (!isOwnProfile && !canEdit) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Read-only profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
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
        }

        // Profile incomplete warning banner
        if (showIncompleteWarning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "You must complete your character profile to create or adventure.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Display image at top
        EntityImage(
            imageUrl = imageUrl,
            contentDescription = "Image of ${user.name}",
            isGenerating = isImageGenerating
        )

        // Name card (read-only) with optional reroll button
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (isAdmin) {
                            Text(
                                text = "ID: ${user.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Reroll button - only show when class is assigned and user can edit
                    if (canEdit && characterClassId != null) {
                        IconButton(
                            onClick = { showRerollConfirmDialog = true },
                            enabled = !isLoading && !isClassGenerating
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reroll character",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Reroll confirmation dialog
        if (showRerollConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showRerollConfirmDialog = false },
                title = { Text("Reroll Character?") },
                text = {
                    Text("This will clear your current class assignment and allow you to regenerate your character. Are you sure?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRerollConfirmDialog = false
                            scope.launch {
                                isLoading = true
                                // Clear the class assignment on the server
                                ApiClient.updateUserClass(user.id, null).onSuccess { updatedUser ->
                                    characterClassId = null
                                    assignedClass = null
                                    classAbilities = emptyList()
                                    onUserUpdated(updatedUser)
                                    message = "Character reset. You can now regenerate your class."
                                }.onFailure { error ->
                                    message = "Error: ${error.message}"
                                }
                                isLoading = false
                            }
                        }
                    ) {
                        Text("Reroll")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRerollConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Description field - always editable for canEdit users
        if (canEdit) {
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isLoading && !isClassGenerating
            )
        } else {
            // Read-only view for non-editors
            if (user.desc.isNotBlank()) {
                Text(
                    text = user.desc,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Generation checkboxes and save button - only show for editable users WITHOUT a class assigned
        if (canEdit && characterClassId == null) {
            // Checkboxes row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = genClassChecked,
                        onCheckedChange = { genClassChecked = it },
                        enabled = !isLoading && !isClassGenerating
                    )
                    Text(
                        text = "Generate Class",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(enabled = !isLoading && !isClassGenerating) {
                            genClassChecked = !genClassChecked
                        }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = genImageChecked,
                        onCheckedChange = { genImageChecked = it },
                        enabled = !isLoading && !isClassGenerating && !isImageGenerating
                    )
                    Text(
                        text = "Generate Image",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(enabled = !isLoading && !isClassGenerating && !isImageGenerating) {
                            genImageChecked = !genImageChecked
                        }
                    )
                }
            }

            // Helper notes
            Text(
                text = "If you don't generate a class, a stock class will be assigned based on your description. Generating a class or image may take a few moments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null

                        // 1. Save profile description
                        val request = UpdateUserRequest(
                            desc = desc,
                            itemIds = user.itemIds,
                            featureIds = user.featureIds
                        )
                        ApiClient.updateUser(user.id, request).onSuccess { updatedUser ->
                            onUserUpdated(updatedUser)

                            // 2. Trigger image generation if checked
                            if (genImageChecked && desc.isNotBlank()) {
                                BackgroundImageGenerationManager.startGeneration(
                                    entityType = "user",
                                    entityId = user.id,
                                    name = user.name,
                                    description = desc,
                                    featureIds = user.featureIds
                                )
                                genImageChecked = false
                            }

                            // 3. Assign class if needed (checked or no class yet)
                            if (genClassChecked || characterClassId == null) {
                                if (desc.isNotBlank()) {
                                    message = "Assigning class..."
                                    // Use AsyncOperationRepository for lifecycle-independent class generation
                                    AsyncOperationRepository.startClassGeneration(
                                        userId = user.id,
                                        characterDescription = desc,
                                        generateNew = genClassChecked
                                    )
                                    genClassChecked = false
                                } else {
                                    message = "Please provide a description to assign a class"
                                }
                            } else {
                                message = "Profile saved!"
                            }
                        }.onFailure { error ->
                            message = "Error: ${error.message}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && !isClassGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Character")
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
        }

        // Message display
        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        // Class generation in progress indicator - using repository status message
        if (isClassGenerating && assignedClass == null) {
            val statusMessage = classGenerationStatus[user.id]?.message ?: "Generating class..."
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "This may take a few minutes. The page will update automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Assigned class section - show if class is assigned
        assignedClass?.let { charClass ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Character Class",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = charClass.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (charClass.isSpellcaster) "Spellcaster" else "Martial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = charClass.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Class abilities section - show when class is assigned with abilities
        if (assignedClass != null && classAbilities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with expand/collapse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { abilitiesExpanded = !abilitiesExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (assignedClass!!.isSpellcaster) "Spells (${classAbilities.size})" else "Abilities (${classAbilities.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (abilitiesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (abilitiesExpanded) "Collapse" else "Expand"
                        )
                    }

                    // Abilities list (when expanded)
                    if (abilitiesExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            classAbilities.forEach { ability ->
                                AbilityDisplayCard(ability)
                            }
                        }
                    }
                }
            }
        }

        // Logout button at the very bottom - only for own profile
        if (isOwnProfile) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
private fun AbilityDisplayCard(ability: AbilityDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ability.name,
                    style = MaterialTheme.typography.titleSmall
                )
                // Power cost badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Cost: ${ability.powerCost}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = ability.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
