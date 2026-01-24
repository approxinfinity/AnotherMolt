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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.AbilityDto
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CreateAbilityRequest
import kotlinx.coroutines.launch

@Composable
fun AbilityForm(
    editAbility: AbilityDto?,
    classId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(editAbility?.name ?: "") }
    var description by remember { mutableStateOf(editAbility?.description ?: "") }
    var abilityType by remember { mutableStateOf(editAbility?.abilityType ?: "spell") }
    var targetType by remember { mutableStateOf(editAbility?.targetType ?: "single_enemy") }
    var range by remember { mutableStateOf(editAbility?.range?.toString() ?: "60") }
    var cooldownType by remember { mutableStateOf(editAbility?.cooldownType ?: "medium") }
    var cooldownRounds by remember { mutableStateOf(editAbility?.cooldownRounds?.toString() ?: "3") }
    var baseDamage by remember { mutableStateOf(editAbility?.baseDamage?.toString() ?: "0") }
    var durationRounds by remember { mutableStateOf(editAbility?.durationRounds?.toString() ?: "0") }
    var effects by remember { mutableStateOf(editAbility?.effects ?: "[]") }
    var imageUrl by remember { mutableStateOf(editAbility?.imageUrl ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isEditing = editAbility != null
    val abilityTypes = listOf("spell", "combat", "utility", "passive")
    val targetTypes = listOf("self", "single_enemy", "single_ally", "area", "all_enemies", "all_allies")
    val cooldownTypes = listOf("none", "short", "medium", "long")

    // Calculate estimated power cost for preview
    val estimatedPowerCost = remember(baseDamage, range, targetType, cooldownType, durationRounds, effects) {
        var cost = 0
        cost += (baseDamage.toIntOrNull() ?: 0) / 5
        val rangeVal = range.toIntOrNull() ?: 0
        cost += when {
            rangeVal <= 0 -> 0
            rangeVal <= 5 -> 1
            rangeVal <= 30 -> 2
            rangeVal <= 60 -> 3
            rangeVal <= 120 -> 4
            else -> 5
        }
        cost += when (targetType) {
            "self" -> 0
            "single_enemy", "single_ally" -> 1
            "area" -> 3
            "all_enemies", "all_allies" -> 5
            else -> 1
        }
        cost += when (cooldownType) {
            "none" -> 5
            "short" -> 2
            "medium" -> 0
            "long" -> -2
            else -> 0
        }
        val durVal = durationRounds.toIntOrNull() ?: 0
        cost += when {
            durVal <= 0 -> 0
            durVal <= 2 -> 2
            else -> 4
        }
        if (effects.contains("heal")) cost += 3
        if (effects.contains("stun")) cost += 4
        if (effects.contains("immobilize") || effects.contains("root")) cost += 5
        if (effects.contains("buff")) cost += 2
        if (effects.contains("debuff")) cost += 3
        cost.coerceAtLeast(1)
    }

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
                text = if (isEditing) "Edit Ability" else "Create Ability",
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

        // Ability Type selector
        Text(
            text = "Ability Type",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            abilityTypes.forEach { type ->
                FilterChip(
                    selected = abilityType == type,
                    onClick = { abilityType = type },
                    label = { Text(type.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Type selector
        Text(
            text = "Target Type",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            targetTypes.forEach { type ->
                FilterChip(
                    selected = targetType == type,
                    onClick = { targetType = type },
                    label = { Text(type.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Power cost preview card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    estimatedPowerCost > 15 -> MaterialTheme.colorScheme.errorContainer
                    estimatedPowerCost > 10 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Estimated Power Cost",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$estimatedPowerCost",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Damage and Duration in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = baseDamage,
                onValueChange = { baseDamage = it.filter { c -> c.isDigit() } },
                label = { Text("Base Damage") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                supportingText = { Text("+${(baseDamage.toIntOrNull() ?: 0) / 5} cost") }
            )
            OutlinedTextField(
                value = durationRounds,
                onValueChange = { durationRounds = it.filter { c -> c.isDigit() } },
                label = { Text("Duration (rounds)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                supportingText = { Text("0 = instant") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Range field
        OutlinedTextField(
            value = range,
            onValueChange = { range = it.filter { c -> c.isDigit() } },
            label = { Text("Range (feet)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("0 for self/touch, 5 for melee") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cooldown Type selector
        Text(
            text = "Cooldown Type",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cooldownTypes.forEach { type ->
                FilterChip(
                    selected = cooldownType == type,
                    onClick = { cooldownType = type },
                    label = { Text(type.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        if (cooldownType == "short" || cooldownType == "medium") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cooldownRounds,
                onValueChange = { cooldownRounds = it.filter { c -> c.isDigit() } },
                label = { Text("Cooldown Rounds") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Effects JSON field
        OutlinedTextField(
            value = effects,
            onValueChange = { effects = it },
            label = { Text("Effects (JSON)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            supportingText = { Text("JSON array of effect objects") }
        )

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

                    val request = CreateAbilityRequest(
                        name = name.trim(),
                        description = description.trim(),
                        classId = classId,
                        abilityType = abilityType,
                        targetType = targetType,
                        range = range.toIntOrNull() ?: 0,
                        cooldownType = cooldownType,
                        cooldownRounds = if (cooldownType == "short" || cooldownType == "medium") cooldownRounds.toIntOrNull() ?: 0 else 0,
                        baseDamage = baseDamage.toIntOrNull() ?: 0,
                        durationRounds = durationRounds.toIntOrNull() ?: 0,
                        effects = effects,
                        imageUrl = imageUrl.ifBlank { null }
                    )

                    val result = if (isEditing) {
                        ApiClient.updateAbility(editAbility!!.id, request)
                    } else {
                        ApiClient.createAbility(request)
                    }

                    isLoading = false
                    result.onSuccess { onSaved() }
                        .onFailure { error = it.message ?: "Failed to save ability" }
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
                Text(if (isEditing) "Save Changes" else "Create Ability")
            }
        }
    }
}
