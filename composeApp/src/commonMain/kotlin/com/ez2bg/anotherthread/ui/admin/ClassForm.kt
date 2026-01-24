package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CharacterClassDto
import com.ez2bg.anotherthread.api.CreateCharacterClassRequest
import kotlinx.coroutines.launch

@Composable
fun ClassForm(
    editClass: CharacterClassDto?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(editClass?.name ?: "") }
    var description by remember { mutableStateOf(editClass?.description ?: "") }
    var isSpellcaster by remember { mutableStateOf(editClass?.isSpellcaster ?: true) }
    var hitDie by remember { mutableStateOf(editClass?.hitDie?.toString() ?: "6") }
    var primaryAttribute by remember { mutableStateOf(editClass?.primaryAttribute ?: "intelligence") }
    var imageUrl by remember { mutableStateOf(editClass?.imageUrl ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isEditing = editClass != null
    val hitDieOptions = listOf("6", "8", "10", "12")
    val attributeOptions = listOf("strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isEditing) "Edit Class" else "Create Class",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        error?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description field
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Spellcaster toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Class Type",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Martial",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!isSpellcaster) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = isSpellcaster,
                    onCheckedChange = { isSpellcaster = it },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text(
                    text = "Spellcaster",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSpellcaster) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hit Die selector
        Text(
            text = "Hit Die",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hitDieOptions.forEach { die ->
                FilterChip(
                    selected = hitDie == die,
                    onClick = { hitDie = die },
                    label = { Text("d$die") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Attribute selector
        Text(
            text = "Primary Attribute",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            attributeOptions.forEach { attr ->
                FilterChip(
                    selected = primaryAttribute == attr,
                    onClick = { primaryAttribute = attr },
                    label = { Text(attr.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image URL field
        OutlinedTextField(
            value = imageUrl,
            onValueChange = { imageUrl = it },
            label = { Text("Image URL (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Save button
        Button(
            onClick = {
                if (name.isBlank()) {
                    error = "Name is required"
                    return@Button
                }
                if (description.isBlank()) {
                    error = "Description is required"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    error = null

                    val request = CreateCharacterClassRequest(
                        name = name.trim(),
                        description = description.trim(),
                        isSpellcaster = isSpellcaster,
                        hitDie = hitDie.toIntOrNull() ?: 6,
                        primaryAttribute = primaryAttribute,
                        imageUrl = imageUrl.ifBlank { null }
                    )

                    val result = if (isEditing) {
                        ApiClient.updateCharacterClass(editClass!!.id, request)
                    } else {
                        ApiClient.createCharacterClass(request)
                    }

                    isLoading = false
                    result.onSuccess { onSaved() }
                        .onFailure { error = it.message ?: "Failed to save class" }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isEditing) "Save Changes" else "Create Class")
            }
        }
    }
}
