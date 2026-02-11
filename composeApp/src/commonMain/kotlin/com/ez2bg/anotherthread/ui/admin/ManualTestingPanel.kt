package com.ez2bg.anotherthread.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.ManualTestItemDto
import com.ez2bg.anotherthread.api.ManualTestCountsDto
import com.ez2bg.anotherthread.state.UserStateHolder
import kotlinx.coroutines.launch

/**
 * Collapsible panel for tracking manual testing progress.
 * Shows untested features with option to mark as tested,
 * and a separate tab for viewing tested features.
 */
@Composable
fun ManualTestingPanel() {
    var isExpanded by remember { mutableStateOf(false) }
    var counts by remember { mutableStateOf(ManualTestCountsDto(0, 0)) }
    var untestedItems by remember { mutableStateOf<List<ManualTestItemDto>>(emptyList()) }
    var testedItems by remember { mutableStateOf<List<ManualTestItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }  // 0 = Untested, 1 = Tested
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load data when expanded
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            isLoading = true
            errorMessage = null

            // Load counts
            ApiClient.getManualTestCounts().onSuccess { c ->
                counts = c
            }.onFailure {
                errorMessage = "Failed to load counts: ${it.message}"
            }

            // Load untested items
            ApiClient.getUntestedItems().onSuccess { items ->
                untestedItems = items
            }.onFailure {
                errorMessage = "Failed to load untested items: ${it.message}"
            }

            // Load tested items
            ApiClient.getTestedItems().onSuccess { items ->
                testedItems = items
            }.onFailure {
                errorMessage = "Failed to load tested items: ${it.message}"
            }

            isLoading = false
        }
    }

    fun refreshData() {
        scope.launch {
            isLoading = true
            ApiClient.getManualTestCounts().onSuccess { counts = it }
            ApiClient.getUntestedItems().onSuccess { untestedItems = it }
            ApiClient.getTestedItems().onSuccess { testedItems = it }
            isLoading = false
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header - always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = if (counts.untested > 0) Color(0xFFFF9800) else Color(0xFF4CAF50)
                    )
                    Column {
                        Text(
                            text = "Manual Testing Checklist",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${counts.untested} untested, ${counts.tested} tested",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            // Expandable content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Tab row for Untested / Tested
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Untested")
                                    if (counts.untested > 0) {
                                        Badge { Text("${counts.untested}") }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tested")
                                    Badge(
                                        containerColor = Color(0xFF4CAF50)
                                    ) { Text("${counts.tested}") }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // Show list based on selected tab
                        val itemsToShow = if (selectedTab == 0) untestedItems else testedItems

                        if (itemsToShow.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedTab == 0) "All features tested!" else "No features tested yet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(itemsToShow, key = { it.id }) { item ->
                                    ManualTestItemRow(
                                        item = item,
                                        onMarkTested = {
                                            val user = UserStateHolder.currentUser.value
                                            if (user != null) {
                                                scope.launch {
                                                    ApiClient.markTestItemTested(
                                                        id = item.id,
                                                        userId = user.id,
                                                        userName = user.name
                                                    ).onSuccess {
                                                        refreshData()
                                                    }
                                                }
                                            }
                                        },
                                        onUnmarkTested = {
                                            scope.launch {
                                                ApiClient.unmarkTestItemTested(item.id).onSuccess {
                                                    refreshData()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ManualTestItemRow(
    item: ManualTestItemDto,
    onMarkTested: () -> Unit,
    onUnmarkTested: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header row with category badge and action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category badge
                    Surface(
                        color = getCategoryColor(item.category),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White
                        )
                    }

                    // Commit hash if available
                    item.commitHash?.let { hash ->
                        Text(
                            text = hash.take(7),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action button
                if (item.isTested) {
                    IconButton(onClick = onUnmarkTested, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Mark as untested",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onMarkTested, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Mark as tested",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Feature name
            Text(
                text = item.featureName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            // Description
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dates row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Added: ${formatEpochMillis(item.addedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.testedAt != null) {
                    Text(
                        text = "Tested: ${formatEpochMillis(item.testedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )

                    item.testedByUserName?.let { tester ->
                        Text(
                            text = "by $tester",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "faction" -> Color(0xFF9C27B0)  // Purple
        "dungeon" -> Color(0xFF795548)  // Brown
        "pools" -> Color(0xFF00BCD4)    // Cyan
        "shop" -> Color(0xFFFFB300)     // Amber
        "navigation" -> Color(0xFF4CAF50)  // Green
        "fishing" -> Color(0xFF2196F3)  // Blue
        "combat" -> Color(0xFFE53935)   // Red
        "items" -> Color(0xFFFF9800)    // Orange
        "locations" -> Color(0xFF607D8B)  // Blue-grey
        "puzzles" -> Color(0xFF673AB7)  // Deep purple
        else -> Color(0xFF757575)       // Grey
    }
}

/**
 * Simple multiplatform date formatter for epoch millis.
 * Formats as "Jan 15, 2025" style.
 */
private fun formatEpochMillis(millis: Long): String {
    // Calculate date components from epoch millis
    val totalDays = millis / (1000 * 60 * 60 * 24)

    // Start from Unix epoch (Jan 1, 1970)
    var year = 1970
    var remainingDays = totalDays.toInt()

    // Find the year
    while (true) {
        val daysInYear = if (isLeapYear(year)) 366 else 365
        if (remainingDays < daysInYear) break
        remainingDays -= daysInYear
        year++
    }

    // Find the month and day
    val daysInMonth = if (isLeapYear(year)) {
        intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    } else {
        intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    }

    var month = 0
    while (month < 12 && remainingDays >= daysInMonth[month]) {
        remainingDays -= daysInMonth[month]
        month++
    }
    val day = remainingDays + 1

    val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                             "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    return "${monthNames[month]} $day, $year"
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
