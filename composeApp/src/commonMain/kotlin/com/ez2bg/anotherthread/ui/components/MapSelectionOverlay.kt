package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ez2bg.anotherthread.api.TeleportDestinationDto

private val ItemGreen = Color(0xFF4CAF50)

@Composable
fun MapSelectionOverlay(
    destinations: List<TeleportDestinationDto>,
    currentAreaId: String?,
    onDestinationSelected: (TeleportDestinationDto) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onCancel() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .clickable(enabled = false) {}, // Prevent click-through
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(40.dp))
                Text(
                    text = "Select Destination",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Choose a map to teleport to",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            // Destination cards
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                destinations.forEach { destination ->
                    val isCurrentArea = destination.areaId == currentAreaId
                    TeleportDestinationCard(
                        destination = destination,
                        isCurrentArea = isCurrentArea,
                        onClick = { if (!isCurrentArea) onDestinationSelected(destination) }
                    )
                }
            }

            if (destinations.isEmpty()) {
                Text(
                    text = "No teleport destinations available",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TeleportDestinationCard(
    destination: TeleportDestinationDto,
    isCurrentArea: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isCurrentArea) Color.Gray else ItemGreen
    val contentAlpha = if (isCurrentArea) 0.5f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .then(if (!isCurrentArea) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Teleport icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    (if (isCurrentArea) Color.Gray else ItemGreen).copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FlashOn,
                contentDescription = "Teleport",
                tint = if (isCurrentArea) Color.Gray else ItemGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = formatAreaName(destination.areaId),
                color = Color.White.copy(alpha = contentAlpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = destination.locationName,
                color = Color.White.copy(alpha = 0.6f * contentAlpha),
                fontSize = 12.sp
            )
            if (isCurrentArea) {
                Text(
                    text = "(You are here)",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

private fun formatAreaName(areaId: String): String {
    return areaId.split("-", "_")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
