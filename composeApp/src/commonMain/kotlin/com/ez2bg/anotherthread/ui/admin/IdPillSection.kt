package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.ui.EntityType
import com.ez2bg.anotherthread.ui.IdOption

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdPillSection(
    label: String,
    ids: List<String>,
    entityType: EntityType,
    availableOptions: List<IdOption> = emptyList(),
    onPillClick: (String) -> Unit,
    onAddId: (String) -> Unit,
    onRemoveId: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newId by remember { mutableStateOf("") }

    val pillColor = when (entityType) {
        EntityType.LOCATION -> MaterialTheme.colorScheme.primaryContainer
        EntityType.CREATURE -> MaterialTheme.colorScheme.tertiaryContainer
        EntityType.ITEM -> MaterialTheme.colorScheme.secondaryContainer
    }
    val pillTextColor = when (entityType) {
        EntityType.LOCATION -> MaterialTheme.colorScheme.onPrimaryContainer
        EntityType.CREATURE -> MaterialTheme.colorScheme.onTertiaryContainer
        EntityType.ITEM -> MaterialTheme.colorScheme.onSecondaryContainer
    }

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
            ids.forEach { id ->
                IdPill(
                    id = id,
                    label = idToNameMap[id],
                    color = pillColor,
                    textColor = pillTextColor,
                    onClick = { onPillClick(id) },
                    onRemove = if (enabled) {{ onRemoveId(id) }} else null
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
        // Filter out already selected IDs from available options
        val unselectedOptions = availableOptions.filter { it.id !in ids }

        AlertDialog(
            onDismissRequest = { showAddDialog = false; newId = "" },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val entityIcon = when (entityType) {
                        EntityType.ITEM -> "🎒"
                        EntityType.CREATURE -> "🐉"
                        EntityType.LOCATION -> "📍"
                    }
                    Text("$entityIcon Add ${entityType.name.lowercase().replaceFirstChar { it.uppercase() }}")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Enhanced existing options section
                    if (unselectedOptions.isNotEmpty()) {
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
                                        text = "Select from existing:",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ) {
                                        Text(
                                            text = "${unselectedOptions.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
            confirmButton = {
                Button(
                    onClick = {
                        if (newId.isNotBlank()) {
                            onAddId(newId.trim())
                            newId = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newId.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val entityIcon = when (entityType) {
                        EntityType.ITEM -> "🎒"
                        EntityType.CREATURE -> "🐉"
                        EntityType.LOCATION -> "📍"
                    }
                    Text("$entityIcon Add Manual ID")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false; newId = "" },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = option.id,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Surface(
                                                    shape = androidx.compose.foundation.shape.CircleShape,
                                                    color = pillColor.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = "Add",
                                                        tint = pillTextColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Enhanced manual entry section
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (unselectedOptions.isNotEmpty()) 0.3f else 0.5f)
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
                                    color = if (unselectedOptions.isNotEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text(
                                        text = if (unselectedOptions.isNotEmpty()) "2" else "1",
                                        color = if (unselectedOptions.isNotEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                Text(
                                    text = if (unselectedOptions.isNotEmpty()) "Or enter ID manually:" else "Enter ID manually:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedTextField(
                                value = newId,
                                onValueChange = { newId = it },
                                label = { Text("${entityType.name} ID") },
                                placeholder = { Text("Enter unique identifier") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            },
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newId.isNotBlank()) {
                            onAddId(newId.trim())
                            newId = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newId.isNotBlank()
                ) {
                    Text("Add Manual ID")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newId = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}
