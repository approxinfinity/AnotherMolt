package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.ExitDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.ValidateExitResponse
import com.ez2bg.anotherthread.ui.IdOption

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExitPillSection(
    label: String,
    exits: List<ExitDto>,
    availableOptions: List<IdOption> = emptyList(),
    onPillClick: (String) -> Unit,
    onAddExit: (ExitDto) -> Unit,
    onUpdateExit: (oldExit: ExitDto, newExit: ExitDto) -> Unit,
    onRemoveExit: (ExitDto) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    currentLocationId: String? = null,  // The source location for coordinate validation
    allLocations: List<LocationDto> = emptyList()  // All locations for coordinate lookup
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    var selectedDirection by remember { mutableStateOf(ExitDirection.UNKNOWN) }
    var directionDropdownExpanded by remember { mutableStateOf(false) }

    // Validation state
    var validationResult by remember { mutableStateOf<ValidateExitResponse?>(null) }
    var isValidating by remember { mutableStateOf(false) }

    // State for edit dialog
    var exitToEdit by remember { mutableStateOf<ExitDto?>(null) }
    var editDirection by remember { mutableStateOf(ExitDirection.UNKNOWN) }
    var editDirectionDropdownExpanded by remember { mutableStateOf(false) }

    // Get current location's coordinates for display
    val currentLocation = allLocations.find { it.id == currentLocationId }

    val pillColor = MaterialTheme.colorScheme.primaryContainer
    val pillTextColor = MaterialTheme.colorScheme.onPrimaryContainer

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
            exits.forEach { exit ->
                ExitPill(
                    exit = exit,
                    locationName = idToNameMap[exit.locationId],
                    color = pillColor,
                    textColor = pillTextColor,
                    onClick = { onPillClick(exit.locationId) },
                    onEdit = if (enabled) {{
                        exitToEdit = exit
                        editDirection = exit.direction
                    }} else null
                )
            }

            // Add button pill (only show when enabled)
            if (enabled) {
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
    }

    if (showAddDialog) {
        // Filter out already selected locations from available options
        val existingLocationIds = exits.map { it.locationId }.toSet()

        // Filter locations: exclude non-adjacent locations that have coordinates
        // A location with coordinates is only valid if it's exactly 1 cell away from current location
        val unselectedOptions = availableOptions.filter { option ->
            if (option.id in existingLocationIds) return@filter false

            val targetLoc = allLocations.find { it.id == option.id }

            // If target has no coordinates, it's a floating location - allow it
            if (targetLoc?.gridX == null) return@filter true

            // If source has no coordinates, allow all targets
            if (currentLocation?.gridX == null) return@filter true

            // Both have coordinates - check if adjacent (exactly 1 cell away)
            val dx = kotlin.math.abs(targetLoc.gridX!! - currentLocation.gridX!!)
            val dy = kotlin.math.abs((targetLoc.gridY ?: 0) - (currentLocation.gridY ?: 0))
            val sameArea = (targetLoc.areaId ?: "overworld") == (currentLocation.areaId ?: "overworld")

            // Adjacent means max 1 step in any direction (including diagonals), same area, not same cell
            sameArea && dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0)
        }

        // Filter out already used directions (max 1 exit per direction)
        val usedDirections = exits.map { it.direction }.toSet()

        // When a location is selected, validate exit and get available directions
        LaunchedEffect(selectedLocationId, currentLocationId) {
            if (selectedLocationId != null && currentLocationId != null) {
                isValidating = true
                try {
                    ApiClient.validateExit(currentLocationId, selectedLocationId!!)
                        .onSuccess { result ->
                            validationResult = result
                            // Auto-select direction if fixed
                            if (result.validDirections.isNotEmpty()) {
                                val firstValid = result.validDirections.first()
                                selectedDirection = firstValid.direction
                            }
                        }
                        .onFailure {
                            validationResult = null
                        }
                } finally {
                    isValidating = false
                }
            } else {
                validationResult = null
            }
        }

        // Compute available directions based on validation result
        val availableDirections = if (validationResult != null && validationResult!!.canCreateExit) {
            validationResult!!.validDirections.map { it.direction }.filter { it !in usedDirections }
        } else if (currentLocationId == null || currentLocation?.gridX == null) {
            // Source doesn't have coordinates - use old behavior
            ExitDirection.entries.filter { it !in usedDirections }
        } else {
            emptyList()
        }

        // Check if selected direction is fixed (target already has coordinates)
        val isDirectionFixed = validationResult?.validDirections?.firstOrNull()?.isFixed == true

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                selectedLocationId = null
                selectedDirection = ExitDirection.UNKNOWN
                validationResult = null
            },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Add Exit")
                    if (currentLocation?.gridX != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "From: (${currentLocation.gridX}, ${currentLocation.gridY})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                    // Enhanced Step 1: Select destination with improved visual hierarchy
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text(
                                        text = "1",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                Text(
                                    text = "Select destination:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            if (unselectedOptions.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 150.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(unselectedOptions) { option ->
                                        val isSelected = selectedLocationId == option.id
                                        val targetLoc = allLocations.find { it.id == option.id }
                                        val hasCoords = targetLoc?.gridX != null

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedLocationId = option.id
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            shadowElevation = if (isSelected) 4.dp else 1.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = option.name.ifBlank { "(No name)" },
                    // Enhanced Step 2: Direction selection
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedLocationId != null) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = if (selectedLocationId != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text(
                                        text = "2",
                                        color = if (selectedLocationId != null) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                Text(
                                    text = "Choose direction:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selectedLocationId != null) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    }
                                )
                            }

                            if (selectedLocationId == null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "👆 Select a destination first",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else if (isValidating) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp), 
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Validating connection...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            } else if (validationResult != null && !validationResult!!.canCreateExit) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "❌ ${validationResult!!.errorMessage ?: "Cannot create exit to this location"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                    // Enhanced summary section
                    if (selectedLocationId != null && availableDirections.isNotEmpty() && !isValidating) {
                        val selectedName = availableOptions.find { it.id == selectedLocationId }?.name ?: selectedLocationId
                        val dirToShow = if (selectedDirection in availableDirections) selectedDirection else availableDirections.first()
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "✅ Exit Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = dirToShow.toDisplayLabel(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "→",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = selectedName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedLocationId != null && availableDirections.isNotEmpty()) {
                            val directionToUse = if (selectedDirection in availableDirections) selectedDirection else availableDirections.first()
                            onAddExit(ExitDto(locationId = selectedLocationId!!, direction = directionToUse))
                            showAddDialog = false
                            selectedLocationId = null
                            selectedDirection = ExitDirection.UNKNOWN
                            validationResult = null
                        }
                    },
                    enabled = selectedLocationId != null && availableDirections.isNotEmpty() && !isValidating,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        selectedLocationId = null
                        selectedDirection = ExitDirection.UNKNOWN
                        validationResult = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
                                }
                            } else {
                                // Direction is selectable
                                ExposedDropdownMenuBox(
                                    expanded = directionDropdownExpanded,
                                    onExpandedChange = { directionDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = if (selectedDirection in availableDirections) selectedDirection.toDisplayLabel() else availableDirections.first().toDisplayLabel(),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Direction") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = directionDropdownExpanded,
                                        onDismissRequest = { directionDropdownExpanded = false }
                                    ) {
                                        availableDirections.forEach { direction ->
                                            val coordInfo = validationResult?.validDirections?.find { it.direction == direction }?.targetCoordinates
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(direction.toDisplayLabel())
                                                        if (coordInfo != null) {
                                                            Text(
                                                                text = "→ (${coordInfo.x}, ${coordInfo.y})",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedDirection = direction
                                                    directionDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Validating...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (validationResult != null && !validationResult!!.canCreateExit) {
                        Text(
                            text = validationResult!!.errorMessage ?: "Cannot create exit to this location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (availableDirections.isEmpty()) {
                        Text(
                            text = "No valid directions available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (isDirectionFixed) {
                        // Direction is fixed - show as read-only
                        val targetCoords = validationResult?.validDirections?.firstOrNull()?.targetCoordinates
                        OutlinedTextField(
                            value = selectedDirection.toDisplayLabel(),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Direction (fixed by coordinates)") },
                            supportingText = if (targetCoords != null) {
                                { Text("Target at (${targetCoords.x}, ${targetCoords.y})") }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Direction is selectable
                        ExposedDropdownMenuBox(
                            expanded = directionDropdownExpanded,
                            onExpandedChange = { directionDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = if (selectedDirection in availableDirections) selectedDirection.toDisplayLabel() else availableDirections.first().toDisplayLabel(),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = directionDropdownExpanded,
                                onDismissRequest = { directionDropdownExpanded = false }
                            ) {
                                availableDirections.forEach { direction ->
                                    val coordInfo = validationResult?.validDirections?.find { it.direction == direction }?.targetCoordinates
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(direction.toDisplayLabel())
                                                if (coordInfo != null) {
                                                    Text(
                                                        text = "-> (${coordInfo.x}, ${coordInfo.y})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedDirection = direction
                                            directionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Show selected summary
                    if (selectedLocationId != null && availableDirections.isNotEmpty() && !isValidating) {
                        val selectedName = availableOptions.find { it.id == selectedLocationId }?.name ?: selectedLocationId
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        val dirToShow = if (selectedDirection in availableDirections) selectedDirection else availableDirections.first()
                        Text(
                            text = "Exit: ${dirToShow.toDisplayLabel()} -> $selectedName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedLocationId != null && availableDirections.isNotEmpty()) {
                            val directionToUse = if (selectedDirection in availableDirections) selectedDirection else availableDirections.first()
                            onAddExit(ExitDto(locationId = selectedLocationId!!, direction = directionToUse))
                            showAddDialog = false
                            selectedLocationId = null
                            selectedDirection = ExitDirection.UNKNOWN
                            validationResult = null
                        }
                    },
                    enabled = selectedLocationId != null && availableDirections.isNotEmpty() && !isValidating
                ) {
                    Text("Save Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    selectedLocationId = null
                    selectedDirection = ExitDirection.UNKNOWN
                    validationResult = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit exit dialog - also enforce one exit per direction
    if (exitToEdit != null) {
        val currentExit = exitToEdit!!
        val locationName = idToNameMap[currentExit.locationId] ?: currentExit.locationId.take(8)
        // Available directions for edit: unused directions + current exit's direction
        val usedDirectionsForEdit = exits.filter { it.locationId != currentExit.locationId }.map { it.direction }.toSet()
        val availableDirectionsForEdit = ExitDirection.entries.filter { it !in usedDirectionsForEdit }

        AlertDialog(
            onDismissRequest = {
                exitToEdit = null
                editDirection = ExitDirection.UNKNOWN
            },
            title = { Text("Edit Exit") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Show destination (read-only)
                    Text(
                        text = "Destination:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Direction dropdown - only show unused directions
                    Text(
                        text = "Direction:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = editDirectionDropdownExpanded,
                        onExpandedChange = { editDirectionDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = editDirection.toDisplayLabel(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editDirectionDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = editDirectionDropdownExpanded,
                            onDismissRequest = { editDirectionDropdownExpanded = false }
                        ) {
                            availableDirectionsForEdit.forEach { direction ->
                                DropdownMenuItem(
                                    text = { Text(direction.toDisplayLabel()) },
                                    onClick = {
                                        editDirection = direction
                                        editDirectionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newExit = currentExit.copy(direction = editDirection)
                        onUpdateExit(currentExit, newExit)
                        exitToEdit = null
                        editDirection = ExitDirection.UNKNOWN
                    },
                    enabled = editDirection in availableDirectionsForEdit
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onRemoveExit(currentExit)
                            exitToEdit = null
                            editDirection = ExitDirection.UNKNOWN
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                    TextButton(onClick = {
                        exitToEdit = null
                        editDirection = ExitDirection.UNKNOWN
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
