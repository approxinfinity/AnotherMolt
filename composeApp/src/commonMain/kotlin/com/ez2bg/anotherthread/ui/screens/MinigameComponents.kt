package com.ez2bg.anotherthread.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ez2bg.anotherthread.api.FishingInfoDto
import com.ez2bg.anotherthread.api.FishingMinigameStartDto
import com.ez2bg.anotherthread.api.LeverStateDto
import com.ez2bg.anotherthread.api.LockpickInfoDto
import com.ez2bg.anotherthread.api.LockpickPathPointDto
import com.ez2bg.anotherthread.api.PuzzleDto
import com.ez2bg.anotherthread.api.PuzzleProgressResponse

// =============================================================================
// PUZZLE MODAL
// =============================================================================

/**
 * Modal for interacting with lever puzzles.
 * Shows the puzzle description, lever states, and allows pulling levers.
 */
@Composable
fun PuzzleModal(
    puzzle: PuzzleDto?,
    puzzleProgress: PuzzleProgressResponse?,
    isLoading: Boolean,
    onPullLever: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF9C27B0), RoundedCornerShape(16.dp))
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { /* Consume taps to prevent dismissal */ }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Gavel,
                    contentDescription = "Puzzle",
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = puzzle?.name ?: "Puzzle",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Solved indicator - shown below title when solved
            if (puzzleProgress?.isSolved == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B5E20).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Solved",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Puzzle Solved!",
                        color = Color(0xFF4CAF50),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Puzzle description
            if (puzzle != null) {
                Text(
                    text = puzzle.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF9C27B0))
                }
            } else if (puzzleProgress == null || puzzle == null) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unable to load puzzle.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lever list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(puzzleProgress.levers) { lever ->
                        LeverRow(
                            lever = lever,
                            isSolved = puzzleProgress.isSolved,
                            onPull = { onPullLever(lever.id) }
                        )
                    }
                }

                // Show revealed passages if solved
                if (puzzleProgress.isSolved && puzzleProgress.revealedPassages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Secret Passages Revealed:",
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    puzzleProgress.revealedPassages.forEach { passage ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B5E20).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MeetingRoom,
                                contentDescription = "Secret Passage",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = passage.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = passage.description,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close button
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

/**
 * A single lever row in the puzzle modal.
 */
@Composable
fun LeverRow(
    lever: LeverStateDto,
    isSolved: Boolean,
    onPull: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    lever.isPulled -> Color(0xFF9C27B0).copy(alpha = 0.3f)
                    else -> Color(0xFF2A2A2A)
                },
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                when {
                    lever.isPulled -> Color(0xFF9C27B0)
                    else -> Color(0xFF444444)
                },
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lever icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (lever.isPulled) Color(0xFF9C27B0).copy(alpha = 0.5f)
                    else Color(0xFF444444).copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Gavel,
                contentDescription = lever.name,
                tint = if (lever.isPulled) Color.White else Color.Gray,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer(rotationZ = if (lever.isPulled) -45f else 0f)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Lever info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lever.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = lever.description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Pull button or status
        if (lever.isPulled) {
            Text(
                text = "Pulled",
                color = Color(0xFF9C27B0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (!isSolved) {
            Button(
                onClick = onPull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Pull",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// =============================================================================
// SEARCH OVERLAY
// =============================================================================

/**
 * Semi-transparent overlay with pizza spinner animation while searching.
 * Automatically animates based on durationMs.
 */
@Composable
fun SearchOverlay(durationMs: Long) {
    // Progress animation from 0 to 1 over the duration
    var targetProgress by remember { mutableStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing),
        label = "searchProgress"
    )

    // Start the animation when composable appears
    LaunchedEffect(Unit) {
        targetProgress = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pizza spinner (pie slice that grows)
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val sweepAngle = 360f * progress
                    val startAngle = -90f // Start from top

                    // Background circle
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        radius = size.minDimension / 2
                    )

                    // Progress arc (pizza slice)
                    drawArc(
                        color = Color(0xFFFFA000), // Amber/orange color
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = size
                    )

                    // Border
                    drawCircle(
                        color = Color(0xFFFFA000),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // Search icon in center
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Searching",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Searching...",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            // Show time remaining
            val remainingMs = ((1f - progress) * durationMs).toLong()
            val remainingSeconds = (remainingMs / 1000f).coerceAtLeast(0f)
            val formattedSeconds = "${(remainingSeconds * 10).toInt() / 10.0}s"
            Text(
                text = formattedSeconds,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

// =============================================================================
// FISHING OVERLAY
// =============================================================================

/**
 * Semi-transparent overlay with fishing rod animation while fishing.
 * Automatically animates based on durationMs.
 */
@Composable
fun FishingOverlay(durationMs: Long) {
    // Progress animation from 0 to 1 over the duration
    var targetProgress by remember { mutableStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing),
        label = "fishingProgress"
    )

    // Bobbing animation for the bobber
    val infiniteTransition = rememberInfiniteTransition(label = "bobber")
    val bobberOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobberFloat"
    )

    // Start the animation when composable appears
    LaunchedEffect(Unit) {
        targetProgress = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fishing animation area
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val sweepAngle = 360f * progress
                    val startAngle = -90f // Start from top

                    // Background circle (water)
                    drawCircle(
                        color = Color(0xFF1E88E5).copy(alpha = 0.3f),
                        radius = size.minDimension / 2
                    )

                    // Progress arc
                    drawArc(
                        color = Color(0xFF1E88E5),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = size
                    )

                    // Border
                    drawCircle(
                        color = Color(0xFF1E88E5),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Draw bobber (red and white)
                    val centerX = size.width / 2
                    val centerY = size.height / 2 + bobberOffset

                    // Bobber top (red)
                    drawCircle(
                        color = Color.Red,
                        radius = 12.dp.toPx(),
                        center = Offset(centerX, centerY - 8.dp.toPx())
                    )
                    // Bobber bottom (white)
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(centerX, centerY + 4.dp.toPx())
                    )
                }
            }

            Text(
                text = "Fishing...",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            // Show time remaining
            val remainingMs = ((1f - progress) * durationMs).toLong()
            val remainingSeconds = (remainingMs / 1000f).coerceAtLeast(0f)
            val formattedSeconds = "${(remainingSeconds * 10).toInt() / 10.0}s"
            Text(
                text = formattedSeconds,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

// =============================================================================
// FISHING MINIGAME OVERLAY (STARDEW VALLEY STYLE)
// =============================================================================

/**
 * Interactive fishing minigame where fish moves up/down and player controls catch zone.
 * - Fish moves along Y-axis based on difficulty (speed, erraticness)
 * - Player slides catch zone to keep fish inside
 * - Score starts at 50, increases when fish is in zone, decreases when outside
 * - Reach 100 = catch, hit 0 = fish escapes
 */
@Composable
fun FishingMinigameOverlay(
    minigameData: FishingMinigameStartDto,
    onComplete: (finalScore: Int) -> Unit,
    onCancel: () -> Unit
) {
    val fishBehavior = minigameData.fishBehavior
    val durationMs = minigameData.durationMs
    val startingScore = minigameData.startingScore
    val catchZoneSizePercent = minigameData.catchZoneSize  // 20-40%

    // Game state
    var score by remember { mutableStateOf(startingScore.toFloat()) }
    var fishPosition by remember { mutableStateOf(0.5f) }  // 0-1, center of bar
    var catchZonePosition by remember { mutableStateOf(0.5f) }  // 0-1, center of bar
    var fishDirection by remember { mutableStateOf(if (kotlin.random.Random.nextBoolean()) 1f else -1f) }
    var gameEnded by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf(0L) }

    // Bar dimensions - taller bar for more challenge
    val barHeight = 400.dp
    val barWidth = 60.dp
    val catchZoneSize = catchZoneSizePercent / 100f  // Convert to 0-0.4 range

    // Fish behavior parameters
    val fishSpeed = fishBehavior?.speed ?: 0.3f
    val changeDirectionChance = fishBehavior?.changeDirectionChance ?: 0.05f
    val erraticness = fishBehavior?.erraticness ?: 0.3f
    val dartChance = fishBehavior?.dartChance ?: 0f
    val edgePull = fishBehavior?.edgePull ?: 0f
    val behaviorType = fishBehavior?.behaviorType ?: "CALM"

    // DARTING behavior state - tracks if currently in a dart
    var isDarting by remember { mutableStateOf(false) }
    var dartTimer by remember { mutableStateOf(0f) }

    // Game loop
    LaunchedEffect(Unit) {
        val startTime = com.ez2bg.anotherthread.platform.currentTimeMillis()
        lastUpdateTime = startTime

        while (!gameEnded) {
            val currentTime = com.ez2bg.anotherthread.platform.currentTimeMillis()
            val deltaTime = (currentTime - lastUpdateTime) / 1000f  // Seconds
            lastUpdateTime = currentTime

            // Check if time is up
            if (currentTime - startTime >= durationMs) {
                gameEnded = true
                onComplete(score.toInt())
                break
            }

            // Handle DARTING behavior - sudden speed bursts
            if (dartTimer > 0) {
                dartTimer -= deltaTime
                if (dartTimer <= 0) {
                    isDarting = false
                }
            } else if (dartChance > 0 && kotlin.random.Random.nextFloat() < dartChance) {
                isDarting = true
                dartTimer = 0.3f + kotlin.random.Random.nextFloat() * 0.4f  // Dart for 0.3-0.7 seconds
            }

            // Calculate effective speed (3x during dart)
            val effectiveSpeed = if (isDarting) fishSpeed * 3f else fishSpeed

            // Move fish
            val baseMove = effectiveSpeed * deltaTime * fishDirection
            val randomJitter = (kotlin.random.Random.nextFloat() - 0.5f) * erraticness * deltaTime

            // Apply edge pull for STUBBORN behavior
            val edgeForce = if (edgePull > 0) {
                // Pull toward nearest edge (0 or 1)
                val nearestEdge = if (fishPosition > 0.5f) 1f else 0f
                (nearestEdge - fishPosition) * edgePull * deltaTime
            } else 0f

            fishPosition = (fishPosition + baseMove + randomJitter + edgeForce).coerceIn(0.1f, 0.9f)

            // Randomly change direction (more often during WILD/ERRATIC)
            if (kotlin.random.Random.nextFloat() < changeDirectionChance) {
                fishDirection = -fishDirection
            }

            // Bounce off edges
            if (fishPosition <= 0.1f || fishPosition >= 0.9f) {
                fishDirection = -fishDirection
            }

            // Check if fish is in catch zone
            val catchZoneTop = catchZonePosition - catchZoneSize / 2
            val catchZoneBottom = catchZonePosition + catchZoneSize / 2
            val fishInZone = fishPosition >= catchZoneTop && fishPosition <= catchZoneBottom

            // Update score - harder: slower gain, faster loss
            val scoreChange = if (fishInZone) 12f * deltaTime else -25f * deltaTime
            score = (score + scoreChange).coerceIn(0f, 100f)

            // Check win/lose conditions
            if (score >= 100f) {
                gameEnded = true
                onComplete(100)
                break
            } else if (score <= 0f) {
                gameEnded = true
                onComplete(0)
                break
            }

            kotlinx.coroutines.delay(16)  // ~60 FPS
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fish name
            Text(
                text = minigameData.fishName ?: "Fish",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Difficulty indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(10) { i ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (i < minigameData.fishDifficulty) Color(0xFFFF6B6B) else Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score meter on left
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CAUGHT", color = Color.Green, fontSize = 10.sp)
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(barHeight)
                            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    ) {
                        // Score fill (from bottom)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(score / 100f)
                                .align(Alignment.BottomCenter)
                                .background(
                                    when {
                                        score >= 75 -> Color(0xFF4CAF50)  // Green
                                        score >= 40 -> Color(0xFFFFEB3B)  // Yellow
                                        else -> Color(0xFFFF5722)  // Red/Orange
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                    Text("OFF", color = Color.Red, fontSize = 10.sp)
                }

                // Main fishing bar with catch zone and fish
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(barHeight)
                        .background(Color(0xFF1E88E5).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .border(2.dp, Color(0xFF1E88E5), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // Move catch zone based on drag
                                val dragPercent = dragAmount.y / barHeight.toPx()
                                catchZonePosition = (catchZonePosition + dragPercent).coerceIn(
                                    catchZoneSize / 2,
                                    1f - catchZoneSize / 2
                                )
                            }
                        }
                ) {
                    // Catch zone (green box that player controls)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(catchZoneSize)
                            .offset(y = (barHeight * (catchZonePosition - catchZoneSize / 2)))
                            .background(
                                Color.Green.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(2.dp, Color.Green, RoundedCornerShape(4.dp))
                    )

                    // Fish (red/orange circle)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .offset(
                                x = (barWidth - 24.dp) / 2,
                                y = barHeight * fishPosition - 12.dp
                            )
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF5722))
                                ),
                                CircleShape
                            )
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                // Instructions on right
                Column(
                    modifier = Modifier.width(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Drag to move",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Keep fish in green zone!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Score text
            Text(
                text = "Score: ${score.toInt()}/100",
                color = when {
                    score >= 75 -> Color(0xFF4CAF50)
                    score >= 40 -> Color(0xFFFFEB3B)
                    else -> Color(0xFFFF5722)
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Cancel button
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Text("Give Up")
            }
        }
    }
}

// =============================================================================
// LOCKPICKING MINIGAME OVERLAY
// =============================================================================

/**
 * Lockpicking minigame overlay - "trace the path" mechanic.
 * Player drags finger to trace a winding path without going outside the tolerance zone.
 * Higher difficulty locks have narrower paths and shakier lines.
 */
@Composable
fun LockpickingMinigameOverlay(
    lockInfo: LockpickInfoDto,
    onComplete: (accuracy: Float) -> Unit,
    onCancel: () -> Unit
) {
    val pathPoints = lockInfo.pathPoints
    val tolerance = lockInfo.tolerance
    val shakiness = lockInfo.shakiness

    // Game state
    var playerPath by remember { mutableStateOf(listOf<Offset>()) }
    var isTracing by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }
    var hasFinished by remember { mutableStateOf(false) }
    var currentAccuracy by remember { mutableStateOf(1f) }
    var totalDeviation by remember { mutableStateOf(0f) }
    var deviationSamples by remember { mutableStateOf(0) }

    // Canvas dimensions and position tracking for touch offset correction
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }

    // Convert normalized path points to canvas coordinates
    fun toCanvasOffset(point: LockpickPathPointDto): Offset {
        return Offset(
            x = point.x * canvasSize.width,
            y = point.y * canvasSize.height
        )
    }

    // Find closest point on path to a given position
    fun findClosestPointOnPath(pos: Offset): Pair<Offset, Float> {
        var minDist = Float.MAX_VALUE
        var closestPoint = pos

        for (i in 0 until pathPoints.size - 1) {
            val p1 = toCanvasOffset(pathPoints[i])
            val p2 = toCanvasOffset(pathPoints[i + 1])

            // Project pos onto line segment p1-p2
            val lineVec = p2 - p1
            val lineLen = lineVec.getDistance()
            if (lineLen == 0f) continue

            val t = ((pos - p1).x * lineVec.x + (pos - p1).y * lineVec.y) / (lineLen * lineLen)
            val tClamped = t.coerceIn(0f, 1f)
            val projected = p1 + lineVec * tClamped
            val dist = (pos - projected).getDistance()

            if (dist < minDist) {
                minDist = dist
                closestPoint = projected
            }
        }

        return Pair(closestPoint, minDist)
    }

    // Calculate normalized tolerance in pixels
    val tolerancePx = if (canvasSize.width > 0) {
        (tolerance / 100f) * canvasSize.width * 0.5f
    } else 30f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            // Consume all pointer events to prevent swipe navigation underneath
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = lockInfo.lockLevelName ?: "Pick the Lock",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Difficulty indicator
            Text(
                text = "Difficulty: ${lockInfo.difficulty ?: "Unknown"}",
                color = when (lockInfo.difficulty) {
                    "SIMPLE" -> Color(0xFF4CAF50)
                    "STANDARD" -> Color(0xFFFFEB3B)
                    "COMPLEX" -> Color(0xFFFF9800)
                    "MASTER" -> Color(0xFFFF5722)
                    else -> Color.White
                },
                fontSize = 14.sp
            )

            // Instructions
            if (!hasStarted) {
                Text(
                    text = "Trace the path from left to right",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            // Canvas for the path
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(250.dp)
                    .background(Color(0xFF1a1a2e), RoundedCornerShape(8.dp))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            // Wait for first touch
                            val down = awaitFirstDown()
                            val initialPos = down.position

                            if (!hasFinished && canvasSize.width > 0) {
                                // Check if starting near the first point
                                val startPoint = toCanvasOffset(pathPoints.first())
                                val dist = (initialPos - startPoint).getDistance()
                                if (dist < tolerancePx * 2) {
                                    isTracing = true
                                    hasStarted = true
                                    playerPath = listOf(initialPos)
                                    totalDeviation = 0f
                                    deviationSamples = 0

                                    // Track drag movement
                                    do {
                                        val event = awaitPointerEvent()
                                        val pos = event.changes.firstOrNull()?.position ?: break

                                        if (isTracing && !hasFinished) {
                                            event.changes.forEach { it.consume() }
                                            playerPath = playerPath + pos

                                            // Calculate deviation from ideal path
                                            val (_, deviation) = findClosestPointOnPath(pos)
                                            totalDeviation += deviation
                                            deviationSamples++

                                            // Update running accuracy
                                            val avgDeviation = if (deviationSamples > 0) totalDeviation / deviationSamples else 0f
                                            currentAccuracy = (1f - (avgDeviation / tolerancePx).coerceIn(0f, 1f)).coerceIn(0f, 1f)

                                            // Check if reached the end
                                            val endPoint = toCanvasOffset(pathPoints.last())
                                            val distToEnd = (pos - endPoint).getDistance()
                                            if (distToEnd < tolerancePx * 2) {
                                                isTracing = false
                                                hasFinished = true
                                                onComplete(currentAccuracy)
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    // Drag ended
                                    if (isTracing && !hasFinished) {
                                        isTracing = false
                                        hasFinished = true
                                        onComplete(currentAccuracy * 0.5f)  // Penalty for not finishing
                                    }
                                }
                            }
                        }
                    }
            ) {
                if (canvasSize.width == 0) return@Canvas

                // Draw tolerance zone (the path the player should stay within)
                val path = Path()
                pathPoints.forEachIndexed { index, point ->
                    val canvasPoint = toCanvasOffset(point)
                    if (index == 0) {
                        path.moveTo(canvasPoint.x, canvasPoint.y)
                    } else {
                        // Apply shakiness for harder locks
                        val prevPoint = toCanvasOffset(pathPoints[index - 1])
                        val midX = (prevPoint.x + canvasPoint.x) / 2
                        val midY = (prevPoint.y + canvasPoint.y) / 2
                        val offsetY = if (shakiness > 0) {
                            (kotlin.random.Random.nextFloat() - 0.5f) * shakiness * 50f
                        } else 0f
                        path.quadraticBezierTo(midX, midY + offsetY, canvasPoint.x, canvasPoint.y)
                    }
                }

                // Draw tolerance zone (wide semi-transparent area)
                drawPath(
                    path = path,
                    color = Color(0xFF3a506b),
                    style = Stroke(width = tolerancePx * 2, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw ideal path (thin center line)
                drawPath(
                    path = path,
                    color = Color(0xFF5bc0be),
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw start point (green circle)
                val startPoint = toCanvasOffset(pathPoints.first())
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 15f,
                    center = startPoint
                )

                // Draw end point (gold circle)
                val endPoint = toCanvasOffset(pathPoints.last())
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 15f,
                    center = endPoint
                )

                // Draw player's trace
                if (playerPath.size > 1) {
                    val playerPathObj = Path()
                    playerPath.forEachIndexed { index, offset ->
                        if (index == 0) {
                            playerPathObj.moveTo(offset.x, offset.y)
                        } else {
                            playerPathObj.lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(
                        path = playerPathObj,
                        color = if (currentAccuracy >= lockInfo.successThreshold) Color(0xFF4CAF50) else Color(0xFFFF5722),
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // Accuracy display - fixed height to prevent layout shift
            Column(
                modifier = Modifier.height(50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val accuracyPercent = (currentAccuracy * 100).toInt()
                Text(
                    text = if (hasStarted) "Accuracy: $accuracyPercent%" else " ",
                    color = when {
                        currentAccuracy >= lockInfo.successThreshold -> Color(0xFF4CAF50)
                        currentAccuracy >= lockInfo.successThreshold * 0.7f -> Color(0xFFFFEB3B)
                        else -> Color(0xFFFF5722)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Need ${(lockInfo.successThreshold * 100).toInt()}% to open",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            // Cancel button
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Text("Give Up")
            }
        }
    }
}

// =============================================================================
// FISHING DISTANCE MODAL
// =============================================================================

/**
 * Modal for selecting fishing cast distance.
 */
@Composable
fun FishingDistanceModal(
    fishingInfo: FishingInfoDto,
    onSelectDistance: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cast Your Line",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose how far to cast. Farther casts require more strength but yield bigger fish.",
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Near option
                FishingDistanceOption(
                    distance = "NEAR",
                    label = "Near Shore",
                    description = "Small fish (no STR requirement)",
                    enabled = fishingInfo.nearEnabled,
                    onClick = { onSelectDistance("NEAR") }
                )

                // Mid option
                FishingDistanceOption(
                    distance = "MID",
                    label = "Mid Water",
                    description = "Medium fish (STR ${fishingInfo.midStrRequired}+)",
                    enabled = fishingInfo.midEnabled,
                    strRequired = if (!fishingInfo.midEnabled) fishingInfo.midStrRequired else null,
                    currentStr = fishingInfo.currentStr,
                    onClick = { onSelectDistance("MID") }
                )

                // Far option
                FishingDistanceOption(
                    distance = "FAR",
                    label = "Deep Water",
                    description = "Large/trophy fish (STR ${fishingInfo.farStrRequired}+)",
                    enabled = fishingInfo.farEnabled,
                    strRequired = if (!fishingInfo.farEnabled) fishingInfo.farStrRequired else null,
                    currentStr = fishingInfo.currentStr,
                    onClick = { onSelectDistance("FAR") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Success: ${fishingInfo.successChance}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Cost: ${fishingInfo.staminaCost} STA, ${fishingInfo.manaCost} MP",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FishingDistanceOption(
    distance: String,
    label: String,
    description: String,
    enabled: Boolean,
    strRequired: Int? = null,
    currentStr: Int = 0,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        distance == "FAR" -> Color(0xFF1565C0).copy(alpha = 0.2f)  // Deep blue
        distance == "MID" -> Color(0xFF1E88E5).copy(alpha = 0.2f)  // Medium blue
        else -> Color(0xFF42A5F5).copy(alpha = 0.2f)  // Light blue
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = if (enabled) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (strRequired != null && !enabled) {
                Text(
                    text = "Need STR $strRequired (You: $currentStr)",
                    fontSize = 11.sp,
                    color = Color.Red.copy(alpha = 0.8f)
                )
            }
        }
    }
}
