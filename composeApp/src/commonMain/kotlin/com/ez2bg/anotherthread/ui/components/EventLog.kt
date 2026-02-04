package com.ez2bg.anotherthread.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
    LazyColumn(
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(events.takeLast(100)) { event ->
                EventCard(event = event)
            }
        }
    }
}

@Composable
private fun EventCard(
    event: String,
    modifier: Modifier = Modifier
) {
    val eventColor = getEventColor(event)
    val eventIcon = getEventIcon(event)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = eventColor.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = eventIcon,
                fontSize = 16.sp
            )
            
            Text(
                text = event,
                color = eventColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0.5f, 0.5f),
                        blurRadius = 1f
                    )
                )
            )
        }
    }
}

private fun getEventColor(event: String): Color {
    return when {
        event.contains("damage", ignoreCase = true) || event.contains("hit", ignoreCase = true) -> Color(0xFFFF5252)
        event.contains("heal", ignoreCase = true) || event.contains("restore", ignoreCase = true) -> Color(0xFF4CAF50)
        event.contains("cast", ignoreCase = true) || event.contains("spell", ignoreCase = true) -> Color(0xFF2196F3)
        event.contains("miss", ignoreCase = true) || event.contains("dodge", ignoreCase = true) -> Color(0xFF757575)
        event.contains("critical", ignoreCase = true) || event.contains("crit", ignoreCase = true) -> Color(0xFFFFD700)
        event.contains("enter", ignoreCase = true) || event.contains("leave", ignoreCase = true) -> Color(0xFF9C27B0)
        event.contains("death", ignoreCase = true) || event.contains("die", ignoreCase = true) -> Color(0xFF8B0000)
        event.contains("level", ignoreCase = true) || event.contains("experience", ignoreCase = true) -> Color(0xFFFFD700)
        event.contains("gold", ignoreCase = true) || event.contains("coin", ignoreCase = true) -> Color(0xFFFFD700)
        else -> Color.White
    }
}

private fun getEventIcon(event: String): String {
    return when {
        event.contains("damage", ignoreCase = true) || event.contains("hit", ignoreCase = true) -> "⚔️"
        event.contains("heal", ignoreCase = true) || event.contains("restore", ignoreCase = true) -> "💚"
        event.contains("cast", ignoreCase = true) || event.contains("spell", ignoreCase = true) -> "🔮"
        event.contains("miss", ignoreCase = true) || event.contains("dodge", ignoreCase = true) -> "💨"
        event.contains("critical", ignoreCase = true) || event.contains("crit", ignoreCase = true) -> "💥"
        event.contains("enter", ignoreCase = true) || event.contains("leave", ignoreCase = true) -> "🚶"
        event.contains("death", ignoreCase = true) || event.contains("die", ignoreCase = true) -> "💀"
        event.contains("level", ignoreCase = true) || event.contains("experience", ignoreCase = true) -> "⭐"
        event.contains("gold", ignoreCase = true) || event.contains("coin", ignoreCase = true) -> "💰"
        else -> "📝"
    }
}
                            offset = Offset(0.5f, 0.5f),
                            blurRadius = 1f
                        )
                    )
                )
            }
        }
    }
}
    HEALING,        // Green - healing
    BUFF,           // Blue - buffs applied
    DEBUFF,         // Orange - debuffs applied
    COMBAT,         // Yellow - combat state changes
    MOVEMENT,       // Cyan - movement events
    DEATH,          // Dark red - death events
    LOOT            // Gold - loot/rewards
}

/**
 * Get the color for an event type.
 */
private fun EventType.toColor(): Color = when (this) {
    EventType.INFO -> Color(0xFFB0B0B0)
    EventType.DAMAGE -> Color(0xFFFF5252)
    EventType.HEALING -> Color(0xFF69F0AE)
    EventType.BUFF -> Color(0xFF448AFF)
    EventType.DEBUFF -> Color(0xFFFFAB40)
    EventType.COMBAT -> Color(0xFFFFEB3B)
    EventType.MOVEMENT -> Color(0xFF18FFFF)
    EventType.DEATH -> Color(0xFFB71C1C)
    EventType.LOOT -> Color(0xFFFFD700)
}

/**
 * A scrolling event log that displays combat and game events.
 *
 * Features:
 * - Auto-scrolls to latest entry
 * - Color-coded by event type
 * - Fades old entries
 * - Max entries limit with auto-cleanup
 * - Compact display
 */
@Composable
fun EventLog(
    entries: List<EventLogEntry>,
    modifier: Modifier = Modifier,
    maxVisibleEntries: Int = 6,
    showTimestamps: Boolean = false
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new entries added
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(8.dp)
    ) {
        if (entries.isEmpty()) {
            Text(
                text = "No events",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(entries.takeLast(maxVisibleEntries * 2), key = { it.id }) { entry ->
                    EventLogItem(
                        entry = entry,
                        showTimestamp = showTimestamps
                    )
                }
            }
        }
    }
}

/**
 * A single event log item.
 */
@Composable
private fun EventLogItem(
    entry: EventLogEntry,
    showTimestamp: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(entry.type.toColor(), RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Message
        Text(
            text = entry.message,
            color = entry.type.toColor(),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * A compact event log that shows only the most recent entries with fade effect.
 */
@Composable
fun CompactEventLog(
    entries: List<EventLogEntry>,
    modifier: Modifier = Modifier,
    maxEntries: Int = 4,
    fadeDelayMs: Long = 5000
) {
    val recentEntries = entries.takeLast(maxEntries)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        recentEntries.forEach { entry ->
            var isVisible by remember(entry.id) { mutableStateOf(true) }

            // Auto-fade after delay
            LaunchedEffect(entry.id) {
                delay(fadeDelayMs)
                isVisible = false
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = entry.message,
                    color = entry.type.toColor().copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
