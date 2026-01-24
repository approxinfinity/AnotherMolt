package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.UserDto
import kotlinx.coroutines.launch

/**
 * View for displaying another user's profile (fetched by ID).
 * Read-only for non-admins, editable for admins.
 */
@Composable
fun UserDetailView(
    userId: String,
    currentUser: UserDto?,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onNavigateToItem: (String) -> Unit
) {
    var user by remember { mutableStateOf<UserDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Fetch user on mount
    LaunchedEffect(userId) {
        isLoading = true
        error = null
        ApiClient.getUser(userId).onSuccess { fetchedUser ->
            user = fetchedUser
        }.onFailure { e ->
            error = e.message ?: "Failed to load user"
        }
        isLoading = false
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
        user != null -> {
            UserProfileView(
                user = user!!,
                currentUser = currentUser,
                isAdmin = isAdmin,
                onUserUpdated = { updatedUser ->
                    user = updatedUser
                },
                onLogout = {}, // Not applicable for viewing other users
                onNavigateToItem = onNavigateToItem,
                onBack = onBack
            )
        }
    }
}
