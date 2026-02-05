package com.ez2bg.anotherthread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.ez2bg.anotherthread.api.UserDto
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
                savedUser.characterClassId == null -> AppScreen.CharacterCreation(savedUser)
                else -> AppScreen.Main
            }
        }
    }

    var currentScreen by remember { mutableStateOf(initialScreen) }

    // Dark theme background color matching onboarding
    val darkBackground = Color(0xFF1A1A2E)

    MaterialTheme {
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
                        onComplete = { user ->
                            OnboardingStorage.markOnboardingSeen()
                            // After auth, check if user needs character creation
                            if (user.characterClassId == null) {
                                currentScreen = AppScreen.CharacterCreation(user)
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