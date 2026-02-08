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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CheckCircle
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.state.UserStateHolder
import com.ez2bg.anotherthread.ui.BackgroundImageGenerationManager
import com.ez2bg.anotherthread.ui.EntityImage
import com.ez2bg.anotherthread.ui.components.AbilityIconMapper
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
    var iconMappingsExpanded by remember { mutableStateOf(false) }
    var iconMappings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var iconPickerAbilityId by remember { mutableStateOf<String?>(null) }
    var actionBarExpanded by remember { mutableStateOf(false) }
    var visibleAbilityIds by remember(user.id) { mutableStateOf(user.visibleAbilityIds) }
    var allAvailableAbilities by remember { mutableStateOf<List<AbilityDto>>(emptyList()) }
    var encountersExpanded by remember { mutableStateOf(false) }
    var encounters by remember { mutableStateOf<List<PlayerEncounterDto>>(emptyList()) }
    var inventoryExpanded by remember { mutableStateOf(false) }
    var inventoryItems by remember { mutableStateOf<List<ItemDto>>(emptyList()) }
    var itemAbilitiesMap by remember { mutableStateOf<Map<String, AbilityDto>>(emptyMap()) }
    var equippedItemIds by remember(user.id) { mutableStateOf(user.equippedItemIds) }
    var encounterFilter by remember { mutableStateOf("all") } // "all", "friend", "enemy"
    var selectedEncounter by remember { mutableStateOf<PlayerEncounterDto?>(null) }
    var showRerollConfirmDialog by remember { mutableStateOf(false) }

    // Collect class generation status from the repository
    val classGenerationStatus by AsyncOperationRepository.classGenerationStatus.collectAsState()
    val isClassGenerating = user.id in classGenerationStatus

    // Check if class generation is already in progress (client-side tracking)
    // Note: Generation status is now tracked in-memory on both client and server.
    // If the app restarts during generation, the user will need to re-initiate.
    LaunchedEffect(user.id) {
        // No action needed - AsyncOperationRepository tracks active generations
        // and will continue polling if there's one in progress
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

    // Load icon mappings for this user
    LaunchedEffect(user.id) {
        ApiClient.getIconMappings(user.id).onSuccess { mappings ->
            iconMappings = mappings.associate { it.abilityId to it.iconName }
        }
    }

    // Load encounters for this user
    LaunchedEffect(user.id) {
        ApiClient.getEncounters(user.id).onSuccess { result ->
            encounters = result
        }
    }

    // Load inventory items for this user (need unique items for display)
    LaunchedEffect(user.id, user.itemIds) {
        if (user.itemIds.isNotEmpty()) {
            val uniqueItemIds = user.itemIds.toSet()
            ApiClient.getItems().onSuccess { allItems ->
                inventoryItems = allItems.filter { it.id in uniqueItemIds }
            }
        } else {
            inventoryItems = emptyList()
        }
    }

    // Calculate item counts for stacking display
    val itemCounts: Map<String, Int> = remember(user.itemIds) {
        user.itemIds.groupingBy { it }.eachCount()
    }

    // Calculate weight-based encumbrance
    val currentWeight = remember(inventoryItems, itemCounts) {
        inventoryItems.sumOf { item ->
            val count = itemCounts[item.id] ?: 1
            item.weight * count
        }
    }
    val maxCapacity = user.strength * 5  // STR * 5 = max weight in stone
    val encumbrancePercent = if (maxCapacity > 0) (currentWeight * 100) / maxCapacity else 100
    val encumbranceTier = getEncumbranceTier(encumbrancePercent)

    // Load abilities granted by inventory items
    LaunchedEffect(inventoryItems) {
        val abilityIds = inventoryItems.flatMap { it.abilityIds }.distinct()
        if (abilityIds.isNotEmpty()) {
            ApiClient.getAbilities().onSuccess { allAbilities ->
                itemAbilitiesMap = allAbilities.filter { it.id in abilityIds }.associateBy { it.id }
            }
        } else {
            itemAbilitiesMap = emptyMap()
        }
    }

    // Load all available abilities for action bar configuration (class + learned + item abilities)
    LaunchedEffect(characterClassId, user.learnedAbilityIds, inventoryItems) {
        val abilityList = mutableListOf<AbilityDto>()

        // Class abilities
        characterClassId?.let { classId ->
            ApiClient.getAbilitiesByClass(classId).getOrNull()?.let { abilities ->
                abilityList.addAll(abilities)
            }
        }

        // Learned abilities
        user.learnedAbilityIds.forEach { abilityId ->
            ApiClient.getAbility(abilityId).getOrNull()?.let { ability ->
                abilityList.add(ability)
            }
        }

        // Item abilities
        val itemAbilityIds = inventoryItems.flatMap { it.abilityIds }.distinct()
        itemAbilityIds.forEach { abilityId ->
            ApiClient.getAbility(abilityId).getOrNull()?.let { ability ->
                abilityList.add(ability)
            }
        }

        // Dedupe and filter to non-passive abilities only, sorted by name
        allAvailableAbilities = abilityList
            .distinctBy { it.id }
            .filter { it.abilityType != "passive" }
            .sortedBy { it.name.lowercase() }
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

        // Character Stats Section
        Spacer(modifier = Modifier.height(16.dp))
        CharacterStatsSection(
            user = user,
            currentWeight = currentWeight,
            maxCapacity = maxCapacity,
            encumbrancePercent = encumbrancePercent,
            encumbranceTier = encumbranceTier
        )

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

        // Inventory section - show for own profile when items exist
        if (isOwnProfile && inventoryItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inventoryExpanded = !inventoryExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Backpack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Inventory ($currentWeight/$maxCapacity stone)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Icon(
                            imageVector = if (inventoryExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (inventoryExpanded) "Collapse" else "Expand"
                        )
                    }

                    // Collapsed summary: comma-delimited list of items with counts
                    if (!inventoryExpanded) {
                        val summaryText = inventoryItems.joinToString(", ") { item ->
                            val count = itemCounts[item.id] ?: 1
                            val countSuffix = if (count > 1) " x$count" else ""
                            // Only show slot suffix for equipped items
                            val isEquipped = item.id in equippedItemIds
                            val slotSuffix = if (isEquipped) {
                                item.equipmentSlot?.let { slot ->
                                    " (${slot.replace("_", " ")})"
                                } ?: ""
                            } else ""
                            "${item.name}$countSuffix$slotSuffix"
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (inventoryExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Group items by slot
                        val equippedItems = inventoryItems.filter { it.id in equippedItemIds }
                        val unequippedItems = inventoryItems.filter { it.id !in equippedItemIds }

                        // Equipped items section
                        if (equippedItems.isNotEmpty()) {
                            Text(
                                text = "Equipped",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                equippedItems.forEach { item ->
                                    InventoryItemCard(
                                        item = item,
                                        count = itemCounts[item.id] ?: 1,
                                        isEquipped = true,
                                        abilities = item.abilityIds.mapNotNull { itemAbilitiesMap[it] },
                                        onEquipToggle = {
                                            scope.launch {
                                                ApiClient.unequipItem(user.id, item.id).onSuccess { updatedUser ->
                                                    equippedItemIds = updatedUser.equippedItemIds
                                                    UserStateHolder.updateUser(updatedUser)
                                                    onUserUpdated(updatedUser)
                                                }.onFailure { error ->
                                                    message = "Failed to unequip: ${error.message}"
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Unequipped items section
                        if (unequippedItems.isNotEmpty()) {
                            if (equippedItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = "Inventory",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                unequippedItems.forEach { item ->
                                    InventoryItemCard(
                                        item = item,
                                        count = itemCounts[item.id] ?: 1,
                                        isEquipped = false,
                                        abilities = item.abilityIds.mapNotNull { itemAbilitiesMap[it] },
                                        onEquipToggle = if (item.equipmentSlot != null) {
                                            {
                                                scope.launch {
                                                    ApiClient.equipItem(user.id, item.id).onSuccess { updatedUser ->
                                                        equippedItemIds = updatedUser.equippedItemIds
                                                        UserStateHolder.updateUser(updatedUser)
                                                        onUserUpdated(updatedUser)
                                                    }.onFailure { error ->
                                                        message = "Failed to equip: ${error.message}"
                                                    }
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Configure Action Mappings section - only for own profile with abilities
        if (isOwnProfile && classAbilities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { iconMappingsExpanded = !iconMappingsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configure Action Icons",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (iconMappingsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (iconMappingsExpanded) "Collapse" else "Expand"
                        )
                    }

                    if (iconMappingsExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            classAbilities.filter { it.abilityType != "passive" }.forEach { ability ->
                                val currentIconName = iconMappings[ability.id]
                                val currentIcon = AbilityIconMapper.getIcon(ability, currentIconName)
                                val typeColor = AbilityIconMapper.getAbilityTypeColor(ability.abilityType)

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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Current icon
                                        Surface(
                                            shape = CircleShape,
                                            color = typeColor.copy(alpha = 0.2f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = currentIcon,
                                                    contentDescription = ability.name,
                                                    tint = typeColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Ability name
                                        Text(
                                            text = ability.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Edit button
                                        IconButton(
                                            onClick = {
                                                iconPickerAbilityId = if (iconPickerAbilityId == ability.id) null else ability.id
                                            }
                                        ) {
                                            Icon(Icons.Filled.Edit, "Change icon")
                                        }
                                    }

                                    // Icon picker grid (expanded inline)
                                    if (iconPickerAbilityId == ability.id) {
                                        val availableIcons = AbilityIconMapper.getAllAvailableIcons()
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            // Grid of icons
                                            val chunked = availableIcons.chunked(7)
                                            chunked.forEach { row ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                ) {
                                                    row.forEach { (name, icon) ->
                                                        val isSelected = currentIconName == name
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (isSelected) typeColor.copy(alpha = 0.3f)
                                                                    else MaterialTheme.colorScheme.surface,
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .then(
                                                                    if (isSelected) Modifier.border(2.dp, typeColor, CircleShape)
                                                                    else Modifier
                                                                )
                                                                .clickable {
                                                                    scope.launch {
                                                                        ApiClient.setIconMapping(user.id, ability.id, name)
                                                                            .onSuccess {
                                                                                iconMappings = iconMappings + (ability.id to name)
                                                                                iconPickerAbilityId = null
                                                                            }
                                                                    }
                                                                }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = icon,
                                                                    contentDescription = name,
                                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Reset to default button
                                            if (currentIconName != null) {
                                                TextButton(
                                                    onClick = {
                                                        scope.launch {
                                                            ApiClient.deleteIconMapping(user.id, ability.id)
                                                                .onSuccess {
                                                                    iconMappings = iconMappings - ability.id
                                                                    iconPickerAbilityId = null
                                                                }
                                                        }
                                                    },
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Text("Reset to Default")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Bar Abilities section - only for own profile with class abilities
        if (isOwnProfile && classAbilities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ActionBarAbilitiesSection(
                user = user,
                classAbilities = classAbilities,
                learnedAbilityIds = user.learnedAbilityIds,
                visibleAbilityIds = user.visibleAbilityIds,
                onVisibleAbilitiesChanged = { newIds ->
                    scope.launch {
                        ApiClient.updateVisibleAbilities(user.id, newIds).onSuccess { updatedUser ->
                            UserStateHolder.updateUser(updatedUser)
                            onUserUpdated(updatedUser)
                            message = "Action bar updated!"
                        }.onFailure { error ->
                            message = "Error: ${error.message}"
                        }
                    }
                }
            )
        }

        // Encounters section - only for own profile with encounters
        if (isOwnProfile && encounters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { encountersExpanded = !encountersExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Encounters (${encounters.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (encountersExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (encountersExpanded) "Collapse" else "Expand"
                        )
                    }

                    if (encountersExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter tabs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("all" to "All", "friend" to "Friends", "enemy" to "Enemies").forEach { (key, label) ->
                                FilterChip(
                                    selected = encounterFilter == key,
                                    onClick = { encounterFilter = key },
                                    label = {
                                        val count = when (key) {
                                            "friend" -> encounters.count { it.classification == "friend" }
                                            "enemy" -> encounters.count { it.classification == "enemy" }
                                            else -> encounters.size
                                        }
                                        Text("$label ($count)")
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val filtered = when (encounterFilter) {
                            "friend" -> encounters.filter { it.classification == "friend" }
                            "enemy" -> encounters.filter { it.classification == "enemy" }
                            else -> encounters
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filtered.forEach { encounter ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedEncounter = if (selectedEncounter?.encounteredUserId == encounter.encounteredUserId) null
                                                                else encounter
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (encounter.classification) {
                                            "friend" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            "enemy" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Avatar
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                if (encounter.lastKnownImageUrl != null) {
                                                    EntityImage(
                                                        imageUrl = encounter.lastKnownImageUrl,
                                                        contentDescription = encounter.lastKnownName,
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                } else {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Filled.Person, "Player", modifier = Modifier.size(24.dp))
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = encounter.lastKnownName,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = "Seen ${encounter.encounterCount}x",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Classification badge
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (encounter.classification) {
                                                    "friend" -> MaterialTheme.colorScheme.primary
                                                    "enemy" -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            ) {
                                                Text(
                                                    text = encounter.classification.replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Expanded detail view
                                        if (selectedEncounter?.encounteredUserId == encounter.encounteredUserId) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (encounter.lastKnownDesc.isNotEmpty()) {
                                                Text(
                                                    text = encounter.lastKnownDesc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            // Friend/Enemy toggle buttons
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val newClass = if (encounter.classification == "friend") "neutral" else "friend"
                                                            ApiClient.classifyEncounter(user.id, encounter.encounteredUserId, newClass)
                                                                .onSuccess {
                                                                    encounters = encounters.map { e ->
                                                                        if (e.encounteredUserId == encounter.encounteredUserId)
                                                                            e.copy(classification = newClass)
                                                                        else e
                                                                    }
                                                                    selectedEncounter = selectedEncounter?.copy(classification = newClass)
                                                                }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Filled.Star, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(if (encounter.classification == "friend") "Remove Friend" else "Add Friend")
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val newClass = if (encounter.classification == "enemy") "neutral" else "enemy"
                                                            ApiClient.classifyEncounter(user.id, encounter.encounteredUserId, newClass)
                                                                .onSuccess {
                                                                    encounters = encounters.map { e ->
                                                                        if (e.encounteredUserId == encounter.encounteredUserId)
                                                                            e.copy(classification = newClass)
                                                                        else e
                                                                    }
                                                                    selectedEncounter = selectedEncounter?.copy(classification = newClass)
                                                                }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Filled.Dangerous, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(if (encounter.classification == "enemy") "Remove Enemy" else "Mark Enemy")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (filtered.isEmpty()) {
                                Text(
                                    text = "No ${if (encounterFilter != "all") encounterFilter + "s" else "encounters"} yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

@Composable
private fun InventoryItemCard(
    item: ItemDto,
    count: Int = 1,
    isEquipped: Boolean,
    abilities: List<AbilityDto> = emptyList(),
    onEquipToggle: (() -> Unit)?
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isEquipped)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Item image or placeholder
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (item.imageUrl != null) {
                            EntityImage(
                                imageUrl = item.imageUrl,
                                contentDescription = item.name,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Backpack,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (count > 1) "${item.name} x$count" else item.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (isEquipped) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Equipped",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        // Show slot info
                        if (item.equipmentSlot != null) {
                            Text(
                                text = item.equipmentSlot.replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Show stat bonuses if any
                        item.statBonuses?.let { bonuses ->
                            val bonusStrings = mutableListOf<String>()
                            if (bonuses.attack != 0) bonusStrings.add("ATK ${if (bonuses.attack > 0) "+" else ""}${bonuses.attack}")
                            if (bonuses.defense != 0) bonusStrings.add("DEF ${if (bonuses.defense > 0) "+" else ""}${bonuses.defense}")
                            if (bonuses.maxHp != 0) bonusStrings.add("HP ${if (bonuses.maxHp > 0) "+" else ""}${bonuses.maxHp}")
                            if (bonusStrings.isNotEmpty()) {
                                Text(
                                    text = bonusStrings.joinToString(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (bonusStrings.any { it.contains("-") })
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Expand/collapse indicator and equip button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Equip/Unequip button
                    if (onEquipToggle != null) {
                        OutlinedButton(
                            onClick = onEquipToggle,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(if (isEquipped) "Unequip" else "Equip")
                        }
                    }
                }
            }

            // Expanded description section
            if (isExpanded && item.desc.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = item.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = if (abilities.isNotEmpty()) 4.dp else 12.dp)
                )
            }

            // Show granted abilities (always visible when present)
            if (abilities.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Grants:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    abilities.forEach { ability ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = ability.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterStatsSection(
    user: UserDto,
    currentWeight: Int,
    maxCapacity: Int,
    encumbrancePercent: Int,
    encumbranceTier: EncumbranceTier
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Level and XP row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatBadge(label = "Level", value = user.level.toString())
            StatBadge(label = "XP", value = "${user.experience}")
            StatBadge(label = "Gold", value = user.gold.toString())
        }

        // HP/Mana/Stamina bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResourceBar(
                label = "HP",
                current = user.currentHp,
                max = user.maxHp,
                color = androidx.compose.ui.graphics.Color(0xFFE57373),
                modifier = Modifier.weight(1f)
            )
            ResourceBar(
                label = "MP",
                current = user.currentMana,
                max = user.maxMana,
                color = androidx.compose.ui.graphics.Color(0xFF64B5F6),
                modifier = Modifier.weight(1f)
            )
            ResourceBar(
                label = "SP",
                current = user.currentStamina,
                max = user.maxStamina,
                color = androidx.compose.ui.graphics.Color(0xFF81C784),
                modifier = Modifier.weight(1f)
            )
        }

        // Encumbrance bar with tier display
        EncumbranceBar(
            currentWeight = currentWeight,
            maxCapacity = maxCapacity,
            percent = encumbrancePercent,
            tier = encumbranceTier
        )

        // D&D Attributes in 2 rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttributeStat(abbrev = "STR", value = user.strength)
            AttributeStat(abbrev = "DEX", value = user.dexterity)
            AttributeStat(abbrev = "CON", value = user.constitution)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttributeStat(abbrev = "INT", value = user.intelligence)
            AttributeStat(abbrev = "WIS", value = user.wisdom)
            AttributeStat(abbrev = "CHA", value = user.charisma)
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ResourceBar(
    label: String,
    current: Int,
    max: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$current/$max",
            style = MaterialTheme.typography.labelSmall
        )
        LinearProgressIndicator(
            progress = { if (max > 0) current.toFloat() / max else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttributeStat(abbrev: String, value: Int) {
    val modifier = (value - 10) / 2
    val modifierText = if (modifier >= 0) "+$modifier" else "$modifier"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Text(
            text = abbrev,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "($modifierText)",
            style = MaterialTheme.typography.labelSmall,
            color = if (modifier >= 0)
                androidx.compose.ui.graphics.Color(0xFF81C784)
            else
                androidx.compose.ui.graphics.Color(0xFFE57373)
        )
    }
}

@Composable
private fun EncumbranceBar(
    currentWeight: Int,
    maxCapacity: Int,
    percent: Int,
    tier: EncumbranceTier
) {
    val tierColor = androidx.compose.ui.graphics.Color(tier.color)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Encumbrance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentWeight/$maxCapacity stone",
                    style = MaterialTheme.typography.labelSmall
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = tierColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = tier.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (percent.coerceAtMost(100)).toFloat() / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = tierColor,
            trackColor = tierColor.copy(alpha = 0.2f)
        )
        // Show penalties if encumbered
        if (tier != EncumbranceTier.UNENCUMBERED) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ATK ${tier.attackModifier}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tierColor
                )
                Text(
                    text = "Dodge ${tier.dodgeModifier}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = tierColor
                )
                if (!tier.canMove) {
                    Text(
                        text = "Cannot Move!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Action Bar Abilities section - allows users to select up to 10 abilities to show on action bar.
 * Empty selection = show all abilities.
 */
@Composable
private fun ActionBarAbilitiesSection(
    user: UserDto,
    classAbilities: List<AbilityDto>,
    learnedAbilityIds: List<String>,
    visibleAbilityIds: List<String>,
    onVisibleAbilitiesChanged: (List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var localVisibleIds by remember(visibleAbilityIds) { mutableStateOf(visibleAbilityIds.toMutableList()) }
    var allAbilities by remember { mutableStateOf<List<AbilityDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load all abilities (class + learned)
    LaunchedEffect(classAbilities, learnedAbilityIds) {
        isLoading = true
        val learnedAbilities = mutableListOf<AbilityDto>()
        for (abilityId in learnedAbilityIds) {
            ApiClient.getAbility(abilityId).getOrNull()?.let { learnedAbilities.add(it) }
        }
        // Combine and deduplicate, filter out passives
        allAbilities = (classAbilities + learnedAbilities)
            .distinctBy { it.id }
            .filter { it.abilityType != "passive" }
            .sortedBy { it.name.lowercase() }
        isLoading = false
    }

    val maxAbilities = 10
    val selectedCount = localVisibleIds.size

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Action Bar Abilities",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (selectedCount == 0) "Showing all abilities" else "$selectedCount/$maxAbilities selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    Text(
                        text = "Select up to 10 abilities to show on your action bar. Leave all unchecked to show all abilities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allAbilities.forEach { ability ->
                            val isSelected = ability.id in localVisibleIds
                            val canSelect = selectedCount < maxAbilities || isSelected

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = canSelect) {
                                        localVisibleIds = if (isSelected) {
                                            localVisibleIds.toMutableList().apply { remove(ability.id) }
                                        } else {
                                            localVisibleIds.toMutableList().apply { add(ability.id) }
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        localVisibleIds = if (checked) {
                                            if (selectedCount < maxAbilities) {
                                                localVisibleIds.toMutableList().apply { add(ability.id) }
                                            } else {
                                                localVisibleIds
                                            }
                                        } else {
                                            localVisibleIds.toMutableList().apply { remove(ability.id) }
                                        }
                                    },
                                    enabled = canSelect
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ability.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (canSelect) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = ability.abilityType.replaceFirstChar { it.uppercase() } +
                                               if (ability.powerCost > 0) " - Cost: ${ability.powerCost}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clear all button
                        OutlinedButton(
                            onClick = { localVisibleIds = mutableListOf() },
                            modifier = Modifier.weight(1f),
                            enabled = localVisibleIds.isNotEmpty()
                        ) {
                            Text("Clear All")
                        }

                        // Save button
                        Button(
                            onClick = { onVisibleAbilitiesChanged(localVisibleIds) },
                            modifier = Modifier.weight(1f),
                            enabled = localVisibleIds != visibleAbilityIds
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Encumbrance tiers based on MajorMUD-style system.
 */
enum class EncumbranceTier(
    val displayName: String,
    val color: Long,          // Color as ARGB long
    val attackModifier: Int,
    val dodgeModifier: Int,
    val canMove: Boolean
) {
    UNENCUMBERED("Unencumbered", 0xFF81C784, 0, 0, true),
    LIGHT("Light", 0xFFFFEB3B, -1, -5, true),
    MEDIUM("Medium", 0xFFFF9800, -2, -10, true),
    HEAVY("Heavy", 0xFFE57373, -3, -20, true),
    OVER_ENCUMBERED("Over!", 0xFFD32F2F, -5, -50, false)
}

/**
 * Get encumbrance tier from percentage of capacity used.
 */
private fun getEncumbranceTier(percent: Int): EncumbranceTier {
    return when {
        percent <= 50 -> EncumbranceTier.UNENCUMBERED
        percent <= 75 -> EncumbranceTier.LIGHT
        percent <= 90 -> EncumbranceTier.MEDIUM
        percent <= 100 -> EncumbranceTier.HEAVY
        else -> EncumbranceTier.OVER_ENCUMBERED
    }
}
