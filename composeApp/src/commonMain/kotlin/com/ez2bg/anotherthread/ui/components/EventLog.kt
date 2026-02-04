package com.ez2bg.anotherthread.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private var eventIdCounter = 0L

/**
 * A single entry in the event log.
 */
data class EventLogEntry(
    val id: String = (++eventIdCounter).toString(),
    val message: String,
    val type: EventType = EventType.INFO
)

/**
 * Type of event for color coding.
 */
enum class EventType {
    INFO,           // Gray - general info
    DAMAGE,         // Red - damage dealt/received
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
