package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.UploadedFileDto
import com.ez2bg.anotherthread.platform.readFileBytes
import kotlinx.coroutines.launch

/**
 * Admin panel view with file upload functionality
 */
@Composable
fun AdminPanelView(
    onViewAuditLogs: () -> Unit,
    onUserClick: (String) -> Unit,
    onBack: (() -> Unit)? = null  // Back to adventure mode when accessed from there
) {
    var uploadedFiles by remember { mutableStateOf<List<UploadedFileDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilePath by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var allowedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    // Load uploaded files and allowed types on mount
    LaunchedEffect(Unit) {
        isLoading = true
        ApiClient.getUploadedFiles().onSuccess { files ->
            uploadedFiles = files
        }
        ApiClient.getAllowedFileTypes().onSuccess { types ->
            allowedTypes = types
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (onBack != null) 16.dp else 0.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button when accessed from adventure mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Adventure",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "Admin Panel",
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Manual Testing Checklist (collapsible)
        ManualTestingPanel()

        // Audit Logs Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewAuditLogs() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Audit Logs",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "View history of all data changes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // World Generation Panel
        WorldGenerationPanel()

        // Service Health Panel
        ServiceHealthPanel()

        // Database Backup Panel
        DatabaseBackupPanel()

        // Data Integrity Panel
        DataIntegrityPanel()

        // Users Panel
        UsersPanel(onUserClick = onUserClick)

        // File Upload Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "File Upload",
                    style = MaterialTheme.typography.titleMedium
                )

                if (allowedTypes.isNotEmpty()) {
                    Text(
                        text = "Allowed file types: ${allowedTypes.sorted().joinToString(", ") { ".$it" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = selectedFilePath,
                    onValueChange = { selectedFilePath = it },
                    label = { Text("File path on disk") },
                    placeholder = { Text("/path/to/file.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (selectedFilePath.isBlank()) {
                                    uploadMessage = "Please enter a file path"
                                    return@launch
                                }

                                isUploading = true
                                uploadMessage = null

                                try {
                                    val fileResult = readFileBytes(selectedFilePath)
                                    if (fileResult == null) {
                                        uploadMessage = "File not found or cannot be read: $selectedFilePath"
                                        isUploading = false
                                        return@launch
                                    }

                                    if (fileResult.extension !in allowedTypes) {
                                        uploadMessage = "File type .${fileResult.extension} not allowed"
                                        isUploading = false
                                        return@launch
                                    }

                                    ApiClient.uploadFile(fileResult.filename, fileResult.bytes).onSuccess { response ->
                                        if (response.success) {
                                            uploadMessage = "File uploaded successfully: ${response.url}"
                                            selectedFilePath = ""
                                            // Refresh the file list
                                            ApiClient.getUploadedFiles().onSuccess { files ->
                                                uploadedFiles = files
                                            }
                                        } else {
                                            uploadMessage = "Upload failed: ${response.error}"
                                        }
                                    }.onFailure { error ->
                                        uploadMessage = "Upload error: ${error.message}"
                                    }
                                } catch (e: Exception) {
                                    uploadMessage = "Error: ${e.message}"
                                }

                                isUploading = false
                            }
                        },
                        enabled = !isUploading && selectedFilePath.isNotBlank()
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload")
                        }
                    }
                }

                uploadMessage?.let {
                    SelectionContainer {
                        Text(
                            text = it,
                            color = if (it.startsWith("Error") || it.startsWith("Upload failed") || it.startsWith("File not") || it.startsWith("File type"))
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Uploaded Files List
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Uploaded Files",
                    style = MaterialTheme.typography.titleMedium
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (uploadedFiles.isEmpty()) {
                    Text(
                        text = "No files uploaded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uploadedFiles.forEach { file ->
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
                                    SelectionContainer {
                                        Text(
                                            text = file.filename,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    SelectionContainer {
                                        Text(
                                            text = file.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = formatFileSize(file.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            ApiClient.deleteUploadedFile(file.filename).onSuccess { deleted ->
                                                if (deleted) {
                                                    uploadedFiles = uploadedFiles.filter { it.filename != file.filename }
                                                    uploadMessage = "File deleted"
                                                }
                                            }
                                        }
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
}
