package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ExitDto

@Composable
fun ExitPill(
    exit: ExitDto,
    locationName: String?,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.padding(2.dp),
        shape = RoundedCornerShape(20.dp), // More rounded for modern look
        color = color,
        shadowElevation = 1.dp // Subtle shadow for depth
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clickable(onClick = onClick), // Make whole pill clickable
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Direction badge with improved styling
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = textColor.copy(alpha = 0.25f), // Slightly more visible
                modifier = Modifier
            ) {
                Text(
                    text = exit.direction.toShortLabel(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    ),
                    color = textColor.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            
            // Location name with better typography
            Text(
                text = locationName ?: exit.locationId.take(8) + if (exit.locationId.length > 8) "..." else "",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Edit button with improved styling
            if (onEdit != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = textColor.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clickable(onClick = onEdit)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit exit",
                        modifier = Modifier
                            .size(14.dp)
                            .padding(1.dp),
                        tint = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
