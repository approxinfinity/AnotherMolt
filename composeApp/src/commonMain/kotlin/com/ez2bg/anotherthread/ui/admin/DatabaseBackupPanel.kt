package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.BackupInfo
import kotlinx.coroutines.launch

/**
 * Database backup and restore panel
 */
@Composable
fun DatabaseBackupPanel() {
    var backups by remember { mutableStateOf<List<BackupInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isCreatingBackup by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedBackupForDelete by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadBackups() {
        scope.launch {
            isLoading = true
            ApiClient.listDatabaseBackups().onSuccess { response ->
                backups = response.backups
            }.onFailure { error ->
                message = "Failed to load backups: ${error.message}"
            }
            isLoading = false
        }
    }

    fun createBackup() {
        scope.launch {
            isCreatingBackup = true
            message = null
            ApiClient.createDatabaseBackup().onSuccess { response ->
                message = response.message
                loadBackups()
            }.onFailure { error ->
                message = "Backup failed: ${error.message}"
            }
            isCreatingBackup = false
        }
    }

    fun restoreBackup(filename: String) {
        scope.launch {
            isRestoring = true
            message = null
            ApiClient.restoreDatabase(filename).onSuccess { response ->
                message = "${response.message}. Pre-restore backup: ${response.preRestoreBackup}"
                loadBackups()
            }.onFailure { error ->
                message = "Restore failed: ${error.message}"
            }
            isRestoring = false
            showRestoreDialog = false
            selectedBackupForRestore = null
        }
    }

    fun deleteBackup(filename: String) {
        scope.launch {
            message = null
            ApiClient.deleteBackup(filename).onSuccess { response ->
                message = response.message
                loadBackups()
            }.onFailure { error ->
                message = "Delete failed: ${error.message}"
            }
            showDeleteDialog = false
            selectedBackupForDelete = null
        }
    }

    // Load backups on mount
    LaunchedEffect(Unit) {
        loadBackups()
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
                    text = "Database Backup",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { loadBackups() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Refresh")
                    }
                    Button(
                        onClick = { createBackup() },
                        enabled = !isCreatingBackup
                    ) {
                        if (isCreatingBackup) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Create Backup")
                    }
                }
            }

            message?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (msg.contains("failed", ignoreCase = true)) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            }

            if (backups.isEmpty() && !isLoading) {
                Text(
                    text = "No backups found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                backups.forEach { backup ->
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
                                    text = backup.filename,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${backup.size / 1024} KB • ${formatTimestamp(backup.modified)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        selectedBackupForRestore = backup.filename
                                        showRestoreDialog = true
                                    },
                                    enabled = !isRestoring
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = "Restore",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedBackupForDelete = backup.filename
                                        showDeleteDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore confirmation dialog
    if (showRestoreDialog && selectedBackupForRestore != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreDialog = false
                selectedBackupForRestore = null
            },
            title = { Text("Restore Database") },
            text = {
                Text("Are you sure you want to restore from \"${selectedBackupForRestore}\"?\n\nA backup of the current database will be created first.")
            },
            confirmButton = {
                Button(
                    onClick = { restoreBackup(selectedBackupForRestore!!) },
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        selectedBackupForRestore = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedBackupForDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedBackupForDelete = null
            },
            title = { Text("Delete Backup") },
            text = {
                Text("Are you sure you want to delete \"${selectedBackupForDelete}\"?\n\nThis action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { deleteBackup(selectedBackupForDelete!!) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedBackupForDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
