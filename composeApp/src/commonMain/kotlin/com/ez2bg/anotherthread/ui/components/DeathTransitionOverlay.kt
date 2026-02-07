package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/**
 * Full-screen death transition overlay with disintegration effect.
 *
 * Animation phases:
 * 1. "You have died" text fades in with red vignette
 * 2. Particle disintegration effect (particles scatter upward)
 * 3. Screen fades to black
 * 4. Respawn location text fades in
 * 5. Everything fades out to reveal new location
 */
@Composable
fun DeathTransitionOverlay(
    respawnLocationName: String,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Overall animation progress (0 to 1 over ~4 seconds)
    val infiniteTransition = rememberInfiniteTransition(label = "death")

    // Use a single animated progress value
    var animationStarted by remember { mutableStateOf(false) }
    val animationProgress = remember { Animatable(0f) }

    // Particles for disintegration effect
    val particles = remember {
        List(150) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 6f + 2f,
                speedX = (Random.nextFloat() - 0.5f) * 0.3f,
                speedY = -Random.nextFloat() * 0.4f - 0.1f, // Upward
                alpha = Random.nextFloat() * 0.5f + 0.5f,
                delay = Random.nextFloat() * 0.3f
            )
        }
    }

    LaunchedEffect(Unit) {
        animationStarted = true
        // Full animation: 4 seconds
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 4000,
                easing = LinearEasing
            )
        )
        // Animation complete, trigger callback
        onAnimationComplete()
    }

    val progress = animationProgress.value

    // Phase timings (as fractions of total animation)
    // 0.0 - 0.15: "You have died" fades in
    // 0.15 - 0.45: Disintegration particles scatter
    // 0.45 - 0.55: Fade to black
    // 0.55 - 0.75: Respawn text fades in
    // 0.75 - 1.0: Everything fades out

    val deathTextAlpha = when {
        progress < 0.15f -> progress / 0.15f
        progress < 0.45f -> 1f
        progress < 0.55f -> 1f - ((progress - 0.45f) / 0.1f)
        else -> 0f
    }

    val respawnTextAlpha = when {
        progress < 0.55f -> 0f
        progress < 0.75f -> (progress - 0.55f) / 0.2f
        progress < 0.85f -> 1f
        else -> 1f - ((progress - 0.85f) / 0.15f)
    }

    val overlayAlpha = when {
        progress < 0.1f -> progress / 0.1f * 0.9f
        progress < 0.85f -> 0.9f
        else -> 0.9f * (1f - ((progress - 0.85f) / 0.15f))
    }

    val particleProgress = when {
        progress < 0.15f -> 0f
        progress < 0.55f -> (progress - 0.15f) / 0.4f
        else -> 1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = overlayAlpha)),
        contentAlignment = Alignment.Center
    ) {
        // Red vignette effect during death phase
        if (progress < 0.55f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val vignetteAlpha = (deathTextAlpha * 0.4f).coerceIn(0f, 0.4f)
                // Draw red edges
                drawRect(
                    color = Color.Red.copy(alpha = vignetteAlpha * 0.3f)
                )
            }
        }

        // Particle disintegration effect
        if (particleProgress > 0f && particleProgress < 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { particle ->
                    val adjustedProgress = ((particleProgress - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
                    if (adjustedProgress > 0f) {
                        val x = (particle.x + particle.speedX * adjustedProgress) * size.width
                        val y = (particle.y + particle.speedY * adjustedProgress * 2f) * size.height
                        val particleAlpha = particle.alpha * (1f - adjustedProgress)

                        if (x >= 0 && x <= size.width && y >= 0 && y <= size.height && particleAlpha > 0.05f) {
                            drawCircle(
                                color = Color(0xFFB71C1C).copy(alpha = particleAlpha), // Dark red particles
                                radius = particle.size * (1f - adjustedProgress * 0.5f),
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
        }

        // "You have died" text
        if (deathTextAlpha > 0f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = deathTextAlpha }
            ) {
                Text(
                    text = "YOU HAVE DIED",
                    color = Color(0xFFB71C1C),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Respawn location text
        if (respawnTextAlpha > 0f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = respawnTextAlpha }
            ) {
                Text(
                    text = "Respawning at",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = respawnLocationName,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speedX: Float,
    val speedY: Float,
    val alpha: Float,
    val delay: Float
)
