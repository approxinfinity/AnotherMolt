package com.ez2bg.anotherthread.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable value proposition page component for onboarding.
 */
@Composable
fun OnboardingPage(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = iconTint
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Pre-defined content for Adventure Mode page.
 */
object AdventurePageContent {
    val title = "Adventure Mode"
    val description = "Explore a vast world filled with danger and mystery. Battle creatures, use powerful abilities, and level up your character as you journey through unique locations."
    val icon = Icons.Default.Explore
}

/**
 * Pre-defined content for Create Mode page.
 */
object CreatePageContent {
    val title = "Create Mode"
    val description = "Build your own worlds with powerful creation tools. Design locations, craft items, and spawn creatures. Shape the adventure for yourself and others."
    val icon = Icons.Default.Create
}
