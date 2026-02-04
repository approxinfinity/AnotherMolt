package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
@Composable
fun IdPill(
    id: String,
    label: String?,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var isHovered by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .padding(2.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { 
                        isHovered = true
                        tryAwaitRelease()
                        isHovered = false
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isHovered) color.copy(alpha = 0.8f) else color,
        shadowElevation = if (isHovered) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Entity type indicator (could be enhanced with icons)
            Surface(
                shape = CircleShape,
                color = textColor.copy(alpha = 0.2f),
                modifier = Modifier.size(6.dp)
            ) {}
            
            Text(
                text = label?.ifBlank { id.take(8) } ?: id.take(8),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (onRemove != null) {
                Surface(
                    shape = CircleShape,
                    color = textColor.copy(alpha = 0.1f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onRemove() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier
                            .size(12.dp)
                            .padding(3.dp),
                        tint = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
                        .clickable(onClick = onRemove),
                    tint = textColor
                )
            }
        }
    }
}
