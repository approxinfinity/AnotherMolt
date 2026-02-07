package com.ez2bg.anotherthread.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.state.UserStateHolder
import com.ez2bg.anotherthread.storage.AuthStorage
import com.ez2bg.anotherthread.storage.OnboardingStorage
import kotlinx.coroutines.launch

private val DarkBackground = Color(0xFF1A1A2E)
private val AccentColor = Color(0xFF6366F1)
private val LightBlue = Color(0xFFBFDBFE)

/**
 * Main onboarding screen with 3 pages:
 * 1. Adventure Mode value proposition
 * 2. Create Mode value proposition
 * 3. Authentication
 *
 * If the user has previously seen onboarding, skip directly to auth page.
 */
@Composable
fun OnboardingScreen(
    onComplete: (UserDto) -> Unit
) {
    // If user has seen onboarding before, skip to auth page (page 2)
    val hasSeenOnboarding = remember { OnboardingStorage.hasSeenOnboarding() }
    val initialPage = if (hasSeenOnboarding) 2 else 0

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 3 }
    )
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Disable swiping if we started on auth page
            userScrollEnabled = !hasSeenOnboarding
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = AdventurePageContent.title,
                    description = AdventurePageContent.description,
                    icon = AdventurePageContent.icon,
                    iconTint = LightBlue
                )
                1 -> OnboardingPage(
                    title = CreatePageContent.title,
                    description = CreatePageContent.description,
                    icon = CreatePageContent.icon,
                    iconTint = LightBlue
                )
                2 -> AuthPage(
                    onAuthenticated = { user ->
                        AuthStorage.saveUser(user)
                        UserStateHolder.updateUser(user)
                        onComplete(user)
                    },
                    defaultToLogin = hasSeenOnboarding,
                    isReturningUser = hasSeenOnboarding
                )
            }
        }

        // Bottom navigation area - only show if user hasn't seen onboarding before
        if (!hasSeenOnboarding) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress indicator
                OnboardingProgressIndicator(
                    pageCount = 3,
                    currentPage = pagerState.currentPage
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Navigation buttons (only show on value prop pages)
                AnimatedVisibility(
                    visible = pagerState.currentPage < 2,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Skip button
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                        ) {
                            Text(
                                "Skip",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // Next button
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentColor
                            )
                        ) {
                            Text(
                                if (pagerState.currentPage == 1) "Get Started" else "Next"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Authentication page with dark theme styling.
 */
@Composable
private fun AuthPage(
    onAuthenticated: (UserDto) -> Unit,
    defaultToLogin: Boolean = false,
    isReturningUser: Boolean = false
) {
    var isLoginMode by remember { mutableStateOf(defaultToLogin) }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Focus requesters for keyboard navigation
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Password visibility toggles
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Dark theme text field colors
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedTextColor = Color.White,
        focusedTextColor = Color.White,
        unfocusedBorderColor = LightBlue.copy(alpha = 0.5f),
        focusedBorderColor = LightBlue,
        unfocusedLabelColor = LightBlue.copy(alpha = 0.7f),
        focusedLabelColor = LightBlue,
        cursorColor = LightBlue,
        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
        focusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isReturningUser) "Adventure Awaits" else "Join the Adventure",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isReturningUser) "Sign in or create an account to begin" else "Create an account or sign in to begin",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Auth form with dark theme
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            color = Color.White.copy(alpha = 0.05f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLoginMode) "Login" else "Register",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LightBlue
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentType = if (isLoginMode) {
                                ContentType.Username
                            } else {
                                ContentType.NewUsername
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .semantics {
                            contentType = if (isLoginMode) {
                                ContentType.Password
                            } else {
                                ContentType.NewPassword
                            }
                        },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = LightBlue
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (!isLoginMode) confirmPasswordFocusRequester.requestFocus() },
                        onDone = { /* Submit will be handled by button */ }
                    ),
                    colors = textFieldColors
                )

                if (!isLoginMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = { Text("Confirm Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(confirmPasswordFocusRequester)
                            .semantics {
                                contentType = ContentType.NewPassword
                            },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    tint = LightBlue
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = textFieldColors
                    )
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (!isLoginMode && password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@launch
                            }

                            isLoading = true
                            errorMessage = null

                            val result = if (isLoginMode) {
                                ApiClient.login(name, password)
                            } else {
                                ApiClient.register(name, password)
                            }

                            isLoading = false

                            result.onSuccess { response ->
                                if (response.success && response.user != null) {
                                    onAuthenticated(response.user)
                                } else {
                                    errorMessage = response.message
                                }
                            }.onFailure { error ->
                                errorMessage = error.message ?: "An error occurred"
                            }
                        }
                    },
                    enabled = !isLoading && name.isNotBlank() && password.isNotBlank() &&
                            (isLoginMode || confirmPassword.isNotBlank()),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(if (isLoginMode) "Login" else "Register")
                    }
                }

                TextButton(
                    onClick = {
                        isLoginMode = !isLoginMode
                        errorMessage = null
                    }
                ) {
                    Text(
                        if (isLoginMode) "Don't have an account? Register"
                        else "Already have an account? Login",
                        color = LightBlue.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
