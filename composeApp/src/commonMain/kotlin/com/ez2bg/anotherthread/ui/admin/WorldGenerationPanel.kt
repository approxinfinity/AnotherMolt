package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Panel for triggering world generation from the admin UI.
 */
@Composable
fun WorldGenerationPanel() {
    var isExpanded by remember { mutableStateOf(false) }
    var areas by remember { mutableStateOf<List<AreaInfoDto>>(emptyList()) }
    var isLoadingAreas by remember { mutableStateOf(false) }

    // Generation params
    var areaId by remember { mutableStateOf("") }
    var areaName by remember { mutableStateOf("Generated World") }
    var width by remember { mutableStateOf("20") }
    var height by remember { mutableStateOf("20") }
    var seed by remember { mutableStateOf("") }
    var generateNames by remember { mutableStateOf(true) }
    var generateDescriptions by remember { mutableStateOf(true) }

    // Advanced params
    var showAdvanced by remember { mutableStateOf(false) }
    var islandFactor by remember { mutableStateOf("1.2") }
    var landThreshold by remember { mutableStateOf("0.3") }
    var riverDensity by remember { mutableStateOf("0.25") }

    // Job tracking
    var currentJobId by remember { mutableStateOf<String?>(null) }
    var jobStatus by remember { mutableStateOf<WorldGenJobStatusDto?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Delete state
    var areaToDelete by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun loadAreas() {
        scope.launch {
            isLoadingAreas = true
            ApiClient.getWorldAreas().onSuccess { areaList ->
                areas = areaList
            }.onFailure { error ->
                message = "Failed to load areas: ${error.message}"
                isError = true
            }
            isLoadingAreas = false
        }
    }

    fun pollJobStatus(jobId: String) {
        var failureCount = 0
        scope.launch {
            while (currentJobId == jobId) {
                delay(2000)
                ApiClient.getWorldGenJobStatus(jobId).onSuccess { status ->
                    failureCount = 0
                    jobStatus = status
                    when (status.status) {
                        "completed" -> {
                            isGenerating = false
                            currentJobId = null
                            val result = status.result
                            if (result != null && result.success) {
                                message = "Generated ${result.locationIds.size} locations in area '${result.areaId}'"
                                isError = false
                                loadAreas()
                            } else {
                                message = result?.errorMessage ?: "Generation completed with unknown status"
                                isError = true
                            }
                        }
                        "failed" -> {
                            isGenerating = false
                            currentJobId = null
                            message = status.error ?: "Generation failed"
                            isError = true
                        }
                        "running" -> {
                            // Still running, update message
                            message = "Generating... (job: ${jobId.take(8)})"
                        }
                    }
                }.onFailure { error ->
                    failureCount++
                    if (failureCount >= 5) {
                        // Too many failures, stop polling
                        isGenerating = false
                        currentJobId = null
                        message = "Lost connection to server while checking job status: ${error.message}"
                        isError = true
                    }
                }
            }
        }
    }

    fun startGeneration() {
        val widthInt = width.toIntOrNull()
        val heightInt = height.toIntOrNull()

        if (widthInt == null || widthInt < 5 || widthInt > 100) {
            message = "Width must be between 5 and 100"
            isError = true
            return
        }
        if (heightInt == null || heightInt < 5 || heightInt > 100) {
            message = "Height must be between 5 and 100"
            isError = true
            return
        }
        if (areaId.isBlank()) {
            message = "Area ID is required"
            isError = true
            return
        }

        val params = WorldGenParamsDto(
            width = widthInt,
            height = heightInt,
            seed = seed.toLongOrNull(),
            areaId = areaId,
            areaName = areaName.ifBlank { areaId },
            generateNames = generateNames,
            generateDescriptions = generateDescriptions,
            islandFactor = islandFactor.toDoubleOrNull() ?: 1.2,
            landThreshold = landThreshold.toDoubleOrNull() ?: 0.3,
            riverDensity = riverDensity.toDoubleOrNull() ?: 0.25
        )

        scope.launch {
            isGenerating = true
            message = null
            jobStatus = null

            ApiClient.generateWorld(params).onSuccess { response ->
                if (response.success && response.jobId != null) {
                    currentJobId = response.jobId
                    message = response.message
                    isError = false
                    pollJobStatus(response.jobId)
                } else {
                    isGenerating = false
                    message = response.message
                    isError = !response.success
                }
            }.onFailure { error ->
                isGenerating = false
                message = "Failed to start generation: ${error.message}"
                isError = true
            }
        }
    }

    fun deleteArea(targetAreaId: String) {
        scope.launch {
            isDeleting = true
            ApiClient.deleteWorldArea(targetAreaId).onSuccess { response ->
                if (response.success) {
                    message = "Deleted ${response.deleted} locations from area '${response.areaId}'"
                    isError = false
                    loadAreas()
                } else {
                    message = "Failed to delete area"
                    isError = true
                }
            }.onFailure { error ->
                message = "Error deleting area: ${error.message}"
                isError = true
            }
            isDeleting = false
            areaToDelete = null
        }
    }

    // Load areas on first expand
    LaunchedEffect(isExpanded) {
        if (isExpanded && areas.isEmpty()) {
            loadAreas()
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "World Generation",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "Collapse" else "Expand")
                }
            }

            if (!isExpanded) {
                Text(
                    text = "Procedurally generate terrain with biomes, rivers, and more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                HorizontalDivider()

                // Existing Areas Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Existing Areas",
                        style = MaterialTheme.typography.titleSmall
                    )
                    IconButton(
                        onClick = { loadAreas() },
                        enabled = !isLoadingAreas
                    ) {
                        if (isLoadingAreas) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }

                if (areas.isEmpty() && !isLoadingAreas) {
                    Text(
                        text = "No generated areas found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    areas.forEach { area ->
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
                                    Text(
                                        text = area.areaId,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${area.locationCount} locations" +
                                            if (area.hasCoordinates) " (grid)" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (areaToDelete == area.areaId) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { areaToDelete = null }) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = { deleteArea(area.areaId) },
                                            enabled = !isDeleting,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            if (isDeleting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onError
                                                )
                                            } else {
                                                Text("Confirm Delete")
                                            }
                                        }
                                    }
                                } else {
                                    IconButton(
                                        onClick = { areaToDelete = area.areaId },
                                        enabled = !isDeleting
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Generation Form
                Text(
                    text = "Generate New Area",
                    style = MaterialTheme.typography.titleSmall
                )

                // Check if area ID is already taken
                val areaIdTaken = areas.any { it.areaId == areaId }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = areaId,
                            onValueChange = { areaId = it },
                            label = { Text("Area ID*") },
                            placeholder = { Text("e.g., island-south") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isGenerating,
                            isError = areaIdTaken,
                            supportingText = if (areaIdTaken) {
                                { Text("This area already exists! Delete it first or use a different ID.") }
                            } else null
                        )
                    }
                    OutlinedTextField(
                        value = areaName,
                        onValueChange = { areaName = it },
                        label = { Text("Area Name") },
                        placeholder = { Text("Southern Islands") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isGenerating
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("Width (5-100)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isGenerating
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height (5-100)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isGenerating
                    )
                    OutlinedTextField(
                        value = seed,
                        onValueChange = { seed = it },
                        label = { Text("Seed (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isGenerating
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = generateNames,
                            onCheckedChange = { generateNames = it },
                            enabled = !isGenerating
                        )
                        Text("Generate Names", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = generateDescriptions,
                            onCheckedChange = { generateDescriptions = it },
                            enabled = !isGenerating
                        )
                        Text("Generate Descriptions", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Advanced options
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Hide Advanced Options" else "Show Advanced Options")
                }

                if (showAdvanced) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = islandFactor,
                            onValueChange = { islandFactor = it },
                            label = { Text("Island Factor") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isGenerating
                        )
                        OutlinedTextField(
                            value = landThreshold,
                            onValueChange = { landThreshold = it },
                            label = { Text("Land Threshold") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isGenerating
                        )
                        OutlinedTextField(
                            value = riverDensity,
                            onValueChange = { riverDensity = it },
                            label = { Text("River Density") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isGenerating
                        )
                    }
                    Text(
                        text = "Island Factor: Higher = more circular. Land Threshold: Higher = more water. River Density: 0-1 probability.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Generate button and status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGenerating) {
                        Button(
                            onClick = { startGeneration() },
                            enabled = false
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        }

                        OutlinedButton(
                            onClick = {
                                currentJobId = null
                                isGenerating = false
                                message = "Generation cancelled (job may still be running on server)"
                                isError = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    } else {
                        Button(
                            onClick = { startGeneration() },
                            enabled = areaId.isNotBlank() && !areaIdTaken
                        ) {
                            Text("Generate World")
                        }
                        if (areaIdTaken) {
                            Text(
                                text = "Area '$areaId' already exists",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Progress display
                if (isGenerating && jobStatus?.progress != null) {
                    val progress = jobStatus!!.progress!!
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = progress.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (progress.total > 0) {
                            LinearProgressIndicator(
                                progress = { progress.current.toFloat() / progress.total.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "${progress.current} / ${progress.total}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else if (isGenerating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                message?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
