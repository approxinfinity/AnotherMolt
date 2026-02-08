package com.ez2bg.anotherthread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.state.AuthEvent
import com.ez2bg.anotherthread.state.UserStateHolder
import com.ez2bg.anotherthread.storage.AuthStorage
import com.ez2bg.anotherthread.storage.OnboardingStorage
import com.ez2bg.anotherthread.ui.AdminScreen
import com.ez2bg.anotherthread.ui.onboarding.CharacterCreationScreen
import com.ez2bg.anotherthread.ui.onboarding.OnboardingScreen
import com.ez2bg.anotherthread.ui.screens.AdventureScreen

/**
 * App navigation state after onboarding.
 */
private sealed class AppScreen {
    data object Onboarding : AppScreen()
    data class CharacterCreation(val user: UserDto) : AppScreen()
    data class GhostExploration(val user: UserDto) : AppScreen()
    data object Main : AppScreen()
}

@Composable
@Preview
fun App() {
    // Determine initial screen based on stored state
    val initialScreen = remember {
        if (!OnboardingStorage.hasSeenOnboarding()) {
            AppScreen.Onboarding
        } else {
            val savedUser = AuthStorage.getUser()
            when {
                savedUser == null -> AppScreen.Onboarding // Need to auth again
                savedUser.characterClassId == null -> AppScreen.GhostExploration(savedUser) // Explore while creating character
                else -> AppScreen.Main
            }
        }
    }

    var currentScreen by remember { mutableStateOf(initialScreen) }

    // Session invalidated message to show on login screen
    var sessionInvalidatedMessage by remember { mutableStateOf<String?>(null) }

    // Initialize UserStateHolder at app startup to sync with AuthStorage
    LaunchedEffect(Unit) {
        UserStateHolder.initialize()
    }

    // Listen for auth events (logout, session expired, etc.)
    LaunchedEffect(Unit) {
        UserStateHolder.authEvents.collect { event ->
            when (event) {
                is AuthEvent.LoggedOut -> {
                    // Navigate back to onboarding/login
                    currentScreen = AppScreen.Onboarding
                }
                is AuthEvent.AuthError -> {
                    // Session expired or invalid - go to login
                    currentScreen = AppScreen.Onboarding
                }
                is AuthEvent.LoggedIn -> {
                    // User logged in - navigate appropriately
                    if (event.user.characterClassId == null) {
                        currentScreen = AppScreen.GhostExploration(event.user)
                    } else {
                        currentScreen = AppScreen.Main
                    }
                }
                is AuthEvent.UserUpdated -> {
                    // User updated - check if we need to change screens
                    if (currentScreen is AppScreen.GhostExploration && event.user.characterClassId != null) {
                        currentScreen = AppScreen.Main
                    }
                }
                is AuthEvent.SessionInvalidated -> {
                    // User signed in on another device - show message and go to login
                    sessionInvalidatedMessage = event.message
                    currentScreen = AppScreen.Onboarding
                }
            }
        }
    }

    // Dark theme background color matching onboarding
    val darkBackground = Color(0xFF1A1A2E)

    MaterialTheme(colorScheme = darkColorScheme(
        background = darkBackground,
        surface = Color(0xFF1E1E32),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFCAC4D0),
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410)
    )) {
        Surface(
            modifier = Modifier
                .background(darkBackground)
                .safeContentPadding()
                .fillMaxSize(),
            color = darkBackground
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Onboarding -> {
                    OnboardingScreen(
                        sessionInvalidatedMessage = sessionInvalidatedMessage,
                        onSessionMessageDismissed = { sessionInvalidatedMessage = null },
                        onComplete = { user ->
                            OnboardingStorage.markOnboardingSeen()
                            // After auth, check if user needs character creation
                            if (user.characterClassId == null) {
                                // Go directly to ghost exploration - user can create character when ready
                                currentScreen = AppScreen.GhostExploration(user)
                            } else {
                                currentScreen = AppScreen.Main
                            }
                        }
                    )
                }

                is AppScreen.CharacterCreation -> {
                    CharacterCreationScreen(
                        user = screen.user,
                        onComplete = { updatedUser ->
                            AuthStorage.saveUser(updatedUser)
                            UserStateHolder.updateUser(updatedUser)
                            currentScreen = AppScreen.Main
                        },
                        onExploreAsGhost = {
                            currentScreen = AppScreen.GhostExploration(screen.user)
                        }
                    )
                }

                is AppScreen.GhostExploration -> {
                    AdventureScreen(
                        currentUser = screen.user,
                        onSwitchToCreate = { },  // Disabled in ghost mode
                        onViewLocationDetails = { },  // Disabled in ghost mode
                        ghostMode = true,
                        onGhostModeBack = {
                            // Refresh user data when returning
                            val refreshedUser = AuthStorage.getUser() ?: screen.user
                            if (refreshedUser.characterClassId != null) {
                                // Class generated while exploring - go to main
                                currentScreen = AppScreen.Main
                            } else {
                                currentScreen = AppScreen.CharacterCreation(refreshedUser)
                            }
                        }
                    )
                }

                is AppScreen.Main -> {
                    AdminScreen()
                }
            }
        }
    }
}