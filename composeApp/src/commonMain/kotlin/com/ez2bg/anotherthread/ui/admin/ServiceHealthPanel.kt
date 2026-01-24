package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.ServiceStatusDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Displays service health status for backend services.
 * Backend API status is inferred from whether the local services check succeeds.
 */
@Composable
fun ServiceHealthPanel() {
    // Local services (checked via backend API)
    var localServices by remember { mutableStateOf<List<ServiceStatusDto>>(emptyList()) }
    // Cloudflare services (checked from browser - only on demand)
    var cloudflareServices by remember { mutableStateOf<List<ServiceStatusDto>>(emptyList()) }
    // Backend API status
    var backendHealthy by remember { mutableStateOf<Boolean?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var hasChecked by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var actionInProgress by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refreshHealth() {
        scope.launch {
            isLoading = true
            actionMessage = null

            // Check local services via backend (this also tells us if backend is up)
            ApiClient.getLocalServicesHealth().onSuccess { healthList ->
                localServices = healthList
                backendHealthy = true

                // If backend is up, also check Cloudflare services via backend
                ApiClient.getCloudflareServicesHealth().onSuccess { cfList ->
                    cloudflareServices = cfList
                }.onFailure {
                    // Keep showing unknown state for cloudflare if check fails
                    cloudflareServices = listOf(
                        ServiceStatusDto("cloudflare_frontend", "Cloudflare Frontend", false, "https://anotherthread.ez2bgood.com"),
                        ServiceStatusDto("cloudflare_backend", "Cloudflare Backend", false, "https://api.ez2bgood.com")
                    )
                }
            }.onFailure {
                backendHealthy = false
                localServices = listOf(
                    ServiceStatusDto("ollama", "Ollama LLM", false, "http://localhost:11434"),
                    ServiceStatusDto("stable_diffusion", "Stable Diffusion", false, "http://localhost:7860")
                )
                cloudflareServices = listOf(
                    ServiceStatusDto("cloudflare_frontend", "Cloudflare Frontend", false, "https://anotherthread.ez2bgood.com"),
                    ServiceStatusDto("cloudflare_backend", "Cloudflare Backend", false, "https://api.ez2bgood.com")
                )
            }

            hasChecked = true
            isLoading = false
        }
    }

    fun controlService(serviceName: String, action: String) {
        scope.launch {
            actionInProgress = serviceName
            actionMessage = null
            ApiClient.controlService(serviceName, action).onSuccess { response ->
                actionMessage = response.message
                // Wait a bit then refresh health
                delay(3000)
                refreshHealth()
            }.onFailure { error ->
                actionMessage = "Error: ${error.message}"
            }
            actionInProgress = null
        }
    }

    var isPurgingCache by remember { mutableStateOf(false) }

    fun purgeCloudflareCache() {
        scope.launch {
            isPurgingCache = true
            actionMessage = null
            ApiClient.purgeCloudflareCache().onSuccess { response ->
                actionMessage = response.message
            }.onFailure { error ->
                actionMessage = "Error purging cache: ${error.message}"
            }
            isPurgingCache = false
        }
    }

    // Build combined service list
    val allServices = buildList {
        // Backend API (always shown, health inferred from API call success)
        add(ServiceStatusDto("backend", "Backend API", backendHealthy ?: false, "http://localhost:8081"))
        // Local services
        addAll(localServices)
        // Cloudflare services
        addAll(cloudflareServices)
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
                    text = "Service Health",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = { refreshHealth() },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (hasChecked) "Refresh" else "Check Services")
                }
            }

            if (!hasChecked) {
                Text(
                    text = "Click 'Check Services' to check service health",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                allServices.forEach { service ->
                    ServiceHealthRow(
                        service = service,
                        isActionInProgress = actionInProgress == service.name,
                        onStart = { controlService(service.name, "start") },
                        onRestart = { controlService(service.name, "restart") },
                        onStop = { controlService(service.name, "stop") }
                    )
                }
            }

            actionMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("Error")) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            }

            // Cloudflare Cache Purge button
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cloudflare Cache",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { purgeCloudflareCache() },
                    enabled = !isPurgingCache,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isPurgingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Purge Cache")
                }
            }
        }
    }
}
