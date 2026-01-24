package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CharacterClassDto
import kotlinx.coroutines.launch

@Composable
fun ClassDetailView(
    classId: String,
    onBack: () -> Unit,
    onEdit: (CharacterClassDto) -> Unit,
    onAddAbility: (String) -> Unit,
    onEditAbility: (AbilityDto) -> Unit,
    isAdmin: Boolean
) {
    var characterClass by remember { mutableStateOf<CharacterClassDto?>(null) }
    var abilities by remember { mutableStateOf<List<AbilityDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var isTogglingLock by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isLocked = characterClass?.isLocked == true

    LaunchedEffect(classId) {
        scope.launch {
            isLoading = true
            val classResult = ApiClient.getCharacterClass(classId)
            val abilitiesResult = ApiClient.getAbilitiesByClass(classId)
            isLoading = false
            classResult.onSuccess { characterClass = it }
                .onFailure { error = it.message }
            abilitiesResult.onSuccess { abilities = it.sortedBy { a -> a.name.lowercase() } }
        }
    }

    // Unlock confirmation dialog
    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("Unlock Class?") },
            text = {
                Text("Are you sure you want to unlock this class? This will make it editable and deletable.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlockDialog = false
                        characterClass?.let { cls ->
                            scope.launch {
                                isTogglingLock = true
                                ApiClient.toggleClassLock(cls.id)
                                    .onSuccess { updated -> characterClass = updated }
                                    .onFailure { err -> error = "Failed to unlock: ${err.message}" }
                                isTogglingLock = false
                            }
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back button header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = characterClass?.name ?: "Class Details",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            // Lock status indicator and toggle
            if (characterClass != null) {
                if (isAdmin) {
                    IconButton(
                        onClick = {
                            if (isLocked) {
                                showUnlockDialog = true
                            } else {
                                // Lock the class (no confirmation needed)
                                characterClass?.let { cls ->
                                    scope.launch {
                                        isTogglingLock = true
                                        ApiClient.toggleClassLock(cls.id)
                                            .onSuccess { updated -> characterClass = updated }
                                            .onFailure { err -> error = "Failed to lock: ${err.message}" }
                                        isTogglingLock = false
                                    }
                                }
                            }
                        },
                        enabled = !isTogglingLock
                    ) {
                        if (isTogglingLock) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = if (isLocked) "Unlock class" else "Lock class",
                                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Non-admins see the lock icon but can't interact
                    Icon(
                        imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = if (isLocked) "Class is locked" else "Class is unlocked",
                        tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                // Edit button (only show when unlocked and admin)
                if (isAdmin && !isLocked) {
                    IconButton(onClick = { characterClass?.let { onEdit(it) } }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Class")
                    }
                }
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                val classResult = ApiClient.getCharacterClass(classId)
                                isLoading = false
                                classResult.onSuccess { characterClass = it }
                                    .onFailure { error = it.message }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }
            characterClass != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Class details card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Image if available
                                characterClass?.imageUrl?.let { url ->
                                    if (url.isNotBlank()) {
                                        AsyncImage(
                                            model = "${AppConfig.api.baseUrl}$url",
                                            contentDescription = characterClass?.name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }

                                Text(
                                    text = characterClass!!.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(if (characterClass!!.isSpellcaster) "Spellcaster" else "Martial") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (characterClass!!.isSpellcaster)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Hit Die: d${characterClass!!.hitDie}") }
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Primary: ${characterClass!!.primaryAttribute.replaceFirstChar { it.uppercase() }}") }
                                    )
                                }
                            }
                        }
                    }

                    // Abilities section header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (characterClass!!.isSpellcaster) "Spells" else "Abilities",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${abilities.size} total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }

                    // Abilities list
                    if (abilities.isEmpty()) {
                        item {
                            Text(
                                text = "No ${if (characterClass!!.isSpellcaster) "spells" else "abilities"} yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(abilities) { ability ->
                            AbilityListItem(
                                ability = ability,
                                onClick = if (!isLocked && isAdmin) {
                                    { onEditAbility(ability) }
                                } else null
                            )
                        }
                    }
                }

                // FAB to add ability (only show when unlocked and admin)
                if (isAdmin && !isLocked) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FloatingActionButton(
                            onClick = { onAddAbility(classId) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Ability")
                        }
                    }
                }
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
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(ability.abilityType.replaceFirstChar { it.uppercase() }) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(ability.targetType.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                )
                if (ability.range > 0) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${ability.range}ft range") }
                    )
                }
                if (ability.cooldownType != "none") {
                    AssistChip(
                        onClick = {},
                        label = { Text("${ability.cooldownType.replaceFirstChar { it.uppercase() }} cooldown") }
                    )
                }
                if (ability.baseDamage > 0) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${ability.baseDamage} damage") }
                    )
                }
                if (ability.durationRounds > 0) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${ability.durationRounds} rounds") }
                    )
                }
            }
        }
    }
}

@Composable
private fun AbilityListItem(
    ability: AbilityDto,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(ability.name)
                // Power cost badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when {
                        ability.powerCost > 15 -> MaterialTheme.colorScheme.errorContainer
                        ability.powerCost > 10 -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = "${ability.powerCost}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = when {
                            ability.powerCost > 15 -> MaterialTheme.colorScheme.onErrorContainer
                            ability.powerCost > 10 -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
        },
        supportingContent = {
            Column {
                Text(
                    text = ability.description.take(100) + if (ability.description.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(ability.abilityType.replaceFirstChar { it.uppercase() }) }
                    )
                    if (ability.range > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${ability.range}ft") }
                        )
                    }
                    if (ability.cooldownType != "none") {
                        AssistChip(
                            onClick = {},
                            label = { Text(ability.cooldownType.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                    if (ability.baseDamage > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${ability.baseDamage} dmg") }
                        )
                    }
                }
            }
        },
        leadingContent = if (!ability.imageUrl.isNullOrBlank()) {
            {
                AsyncImage(
                    model = "${AppConfig.api.baseUrl}${ability.imageUrl}",
                    contentDescription = ability.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        } else null,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    )
    HorizontalDivider()
}
