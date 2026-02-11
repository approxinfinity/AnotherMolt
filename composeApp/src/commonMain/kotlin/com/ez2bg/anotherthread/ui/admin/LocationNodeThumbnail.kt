package com.ez2bg.anotherthread.ui.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.ExitDirection
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.ui.GameMode

/**
 * Thumbnail node for a location in the graph view.
 * Collapsed: 20dp circle with color based on terrain, with name label below
 * Expanded: 100dp box with image/name, centered on where the dot was
 */
@Composable
internal fun LocationNodeThumbnail(
    location: LocationDto,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onExitClick: (LocationDto) -> Unit = {},
    onDotClick: () -> Unit = {},  // Called when the orange dot is tapped (to collapse)
    allLocations: List<LocationDto> = emptyList(),
    gameMode: GameMode = GameMode.CREATE
) {
    val collapsedSize = 20.dp
    val expandedSize = 100.dp
    val exitButtonSize = 28.dp
    val exitButtonOffset = 58.dp // Distance from center to exit button center

    val hasImage = location.imageUrl != null

    // Get terrain-based color for collapsed state
    val terrainColor = remember(location) {
        getTerrainColor(location.desc, location.name)
    }

    // Create a map of location ID to LocationDto for quick lookup
    val locationById = remember(allLocations) { allLocations.associateBy { it.id } }

    if (isExpanded) {
        // Expanded state: Orange highlighted dot on the left, 100x100 thumbnail to the right
        // with action icons overlaid on top-right and exit buttons around the thumbnail
        // The modifier positions us at the dot's top-left corner
        // Collapsed dot center is at (collapsedSize/2, collapsedSize/2) = (10dp, 10dp) from modifier origin
        val highlightedDotSize = 16.dp
        val dotToThumbnailGap = 35.dp // Gap to clear west exit button but not too far
        val exitButtonDistanceFromCenter = 72.dp // Distance from thumbnail center to cardinal exit buttons
        val diagonalExitButtonDistance = 85.dp // Larger distance for diagonal exits to clear corners

        // Calculate the total width: dot + gap + thumbnail container
        val thumbnailContainerSize = expandedSize + exitButtonSize * 2 // Extra space on all sides for exit buttons

        // The collapsed dot center is at (collapsedSize/2, collapsedSize/2) from the modifier origin
        // We want the highlighted dot to be at that exact position
        // The highlighted dot is the first item in the Row, so we offset the Row such that
        // the center of the highlighted dot aligns with the collapsed dot center

        // Collapsed dot center offset from modifier origin
        val collapsedDotCenterX = collapsedSize / 2  // 10dp
        val collapsedDotCenterY = collapsedSize / 2  // 10dp

        // Highlighted dot center will be at (highlightedDotSize/2, Row's vertical center)
        // We need to offset so highlighted dot center = collapsed dot center
        val highlightedDotCenterInRow = highlightedDotSize / 2  // 8dp from Row's left edge

        // Row should start at: collapsedDotCenterX - highlightedDotCenterInRow
        val rowOffsetX = collapsedDotCenterX - highlightedDotCenterInRow  // 10 - 8 = 2dp

        // For Y: Row is vertically centered on the thumbnail, which is expandedSize tall
        // Row's vertical center = expandedSize/2 from Row's top
        // We want the dot (which is vertically centered in the Row) to be at collapsedDotCenterY
        val rowOffsetY = collapsedDotCenterY - (expandedSize / 2 + exitButtonSize)  // Center on thumbnail container

        Row(
            modifier = modifier
                .offset(x = rowOffsetX, y = rowOffsetY)
                .wrapContentSize(),
            horizontalArrangement = Arrangement.spacedBy(dotToThumbnailGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Highlighted dot on the left - tap to collapse
            // Same translucent tan as collapsed dots, but with orange border
            Box(
                modifier = Modifier
                    .size(highlightedDotSize)
                    .clickable { onDotClick() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw translucent tan fill (same as collapsed dots)
                    val tanColor = Color(0xFFD4B896)
                    val fillAlpha = 0.15f
                    drawCircle(
                        color = tanColor.copy(alpha = fillAlpha),
                        radius = radius,
                        center = center
                    )

                    // Draw orange border to indicate selection
                    drawCircle(
                        color = Color(0xFFFF9800),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Container for thumbnail and exit buttons
            // No clickable here - let clicks pass through to adjacent dots
            // Individual elements (thumbnail, exit buttons) handle their own clicks
            Box(
                modifier = Modifier.size(thumbnailContainerSize)
            ) {
                // Exit direction buttons around the thumbnail
                location.exits.forEach { exit ->
                    val targetLocation = locationById[exit.locationId]
                    if (targetLocation != null) {
                        // Check if this is a diagonal direction
                        val isDiagonal = exit.direction in listOf(
                            ExitDirection.NORTHEAST, ExitDirection.NORTHWEST,
                            ExitDirection.SOUTHEAST, ExitDirection.SOUTHWEST
                        )

                        // Use larger distance for diagonals to clear the corners
                        val buttonDistance = if (isDiagonal) diagonalExitButtonDistance else exitButtonDistanceFromCenter

                        // Calculate position based on direction (normalized vectors)
                        val (offsetX, offsetY) = when (exit.direction) {
                            ExitDirection.NORTH -> Pair(0f, -1f)
                            ExitDirection.SOUTH -> Pair(0f, 1f)
                            ExitDirection.EAST -> Pair(1f, 0f)
                            ExitDirection.WEST -> Pair(-1f, 0f)
                            ExitDirection.NORTHEAST -> Pair(0.707f, -0.707f)
                            ExitDirection.NORTHWEST -> Pair(-0.707f, -0.707f)
                            ExitDirection.SOUTHEAST -> Pair(0.707f, 0.707f)
                            ExitDirection.SOUTHWEST -> Pair(-0.707f, 0.707f)
                            ExitDirection.UP -> Pair(0f, -1.2f) // Above, further out
                            ExitDirection.DOWN -> Pair(0f, 1.2f) // Below, further out
                            ExitDirection.ENTER -> Pair(0f, 1.4f) // Below DOWN
                            ExitDirection.UNKNOWN -> Pair(0f, 0f)
                        }

                        // Skip UNKNOWN direction
                        if (exit.direction != ExitDirection.UNKNOWN) {
                            // Center of container
                            val centerOffset = thumbnailContainerSize / 2
                            val touchTargetSize = 44.dp // Larger touch target
                            // Outer box for larger touch target
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = centerOffset - touchTargetSize / 2 + (buttonDistance * offsetX),
                                        y = centerOffset - touchTargetSize / 2 + (buttonDistance * offsetY)
                                    )
                                    .size(touchTargetSize)
                                    .clickable { onExitClick(targetLocation) },
                                contentAlignment = Alignment.Center
                            ) {
                                // Inner visible button
                                Box(
                                    modifier = Modifier
                                        .size(exitButtonSize)
                                        .background(Color(0xFF2196F3).copy(alpha = 0.9f), CircleShape)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Draw arrow icon pointing in the exit direction
                                    Icon(
                                        imageVector = when (exit.direction) {
                                            ExitDirection.NORTH -> Icons.Filled.ArrowUpward
                                            ExitDirection.SOUTH -> Icons.Filled.ArrowDownward
                                            ExitDirection.EAST -> Icons.AutoMirrored.Filled.ArrowForward
                                            ExitDirection.WEST -> Icons.AutoMirrored.Filled.ArrowBack
                                            ExitDirection.NORTHEAST -> Icons.Filled.NorthEast
                                            ExitDirection.NORTHWEST -> Icons.Filled.NorthWest
                                            ExitDirection.SOUTHEAST -> Icons.Filled.SouthEast
                                            ExitDirection.SOUTHWEST -> Icons.Filled.SouthWest
                                            ExitDirection.UP -> Icons.Filled.ArrowUpward
                                            ExitDirection.DOWN -> Icons.Filled.ArrowDownward
                                            ExitDirection.ENTER -> Icons.Filled.MeetingRoom
                                            ExitDirection.UNKNOWN -> Icons.Filled.ArrowUpward
                                        },
                                        contentDescription = exit.direction.name,
                                        tint = Color.White,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // Thumbnail box - centered in the container
                // clickable to consume clicks on the thumbnail itself
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(expandedSize)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { /* consume click on thumbnail */ },
                    contentAlignment = Alignment.Center
                ) {
                if (hasImage) {
                    val fullUrl = "${AppConfig.api.baseUrl}${location.imageUrl}"
                    var imageState by remember {
                        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
                    }

                    val isLoaded = imageState is AsyncImagePainter.State.Success
                    val imageAlpha by animateFloatAsState(
                        targetValue = if (isLoaded) 1f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label = "imageAlpha"
                    )

                    // Semi-opaque placeholder while loading
                    if (!isLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(terrainColor.copy(alpha = 0.5f))
                        )
                    }

                    AsyncImage(
                        model = fullUrl,
                        contentDescription = location.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(imageAlpha),
                        contentScale = ContentScale.Crop,
                        onState = { imageState = it }
                    )

                    // Semi-transparent overlay at bottom for name
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = location.name.ifBlank { location.id.take(8) },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Show loading indicator if still loading
                    if (imageState is AsyncImagePainter.State.Loading || imageState is AsyncImagePainter.State.Empty) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    }

                    // Fall back to colored box on error
                    if (imageState is AsyncImagePainter.State.Error) {
                        FallbackLocationBox(location)
                    }
                } else {
                    FallbackLocationBox(location)
                }

                // Action icons overlaid on top-right of thumbnail - hidden in exploration mode
                if (gameMode.isCreate) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Edit/Detail icon (pencil)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                .clickable(onClick = onClick)
                                .padding(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Location",
                                modifier = Modifier.fillMaxSize(),
                                tint = Color.White
                            )
                        }

                        // Settings/Terrain icon (gear below)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                .clickable(onClick = onSettingsClick)
                                .padding(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Terrain Settings",
                                modifier = Modifier.fillMaxSize(),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            }
        }
    } else {
        // Collapsed state: just the dot (label is rendered separately in parent)
        val isUnedited = location.lastEditedAt == null
        val dotSize = 10.dp  // Both are 10dp

        // Center dots within the space where a full-size dot would be
        val centeringOffset = (collapsedSize - dotSize) / 2

        // Light tan color - 15% alpha for both fill and border
        val tanColor = Color(0xFFD4B896)
        val fillAlpha = 0.15f

        Box(
            modifier = modifier
                .offset(x = centeringOffset, y = centeringOffset)
                .size(dotSize)
                .clickable(onClick = onClick)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Draw simple tan fill
                drawCircle(
                    color = tanColor.copy(alpha = fillAlpha),
                    radius = radius,
                    center = center
                )

                // Draw red border only for edited locations (non-wilderness), same alpha as fill
                if (!isUnedited) {
                    drawCircle(
                        color = DotBorderColor.copy(alpha = fillAlpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}
