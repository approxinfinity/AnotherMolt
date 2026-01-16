package com.ez2bg.anotherthread.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.ApiClient
import com.ez2bg.anotherthread.api.CreateCreatureRequest
import com.ez2bg.anotherthread.api.CreateItemRequest
import com.ez2bg.anotherthread.api.CreateRoomRequest
import com.ez2bg.anotherthread.api.RoomDto
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class AdminTab(val title: String) {
    ROOM("Room"),
    MONSTER("Monster"),
    ITEM("Item")
}

sealed class RoomViewState {
    data object Graph : RoomViewState()
    data object CreateForm : RoomViewState()
    data class EditForm(val room: RoomDto) : RoomViewState()
}

@Composable
fun AdminScreen() {
    var selectedTab by remember { mutableStateOf(AdminTab.ROOM) }
    var roomViewState by remember { mutableStateOf<RoomViewState>(RoomViewState.Graph) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Admin - Manage Entities",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            AdminTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        roomViewState = RoomViewState.Graph
                    },
                    text = { Text(tab.title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            AdminTab.ROOM -> RoomSection(
                viewState = roomViewState,
                onViewStateChange = { roomViewState = it }
            )
            AdminTab.MONSTER -> MonsterForm()
            AdminTab.ITEM -> ItemForm()
        }
    }
}

@Composable
fun RoomSection(
    viewState: RoomViewState,
    onViewStateChange: (RoomViewState) -> Unit
) {
    when (viewState) {
        is RoomViewState.Graph -> RoomGraphView(
            onAddClick = { onViewStateChange(RoomViewState.CreateForm) },
            onRoomClick = { room -> onViewStateChange(RoomViewState.EditForm(room)) }
        )
        is RoomViewState.CreateForm -> RoomForm(
            editRoom = null,
            onBack = { onViewStateChange(RoomViewState.Graph) },
            onSaved = { onViewStateChange(RoomViewState.Graph) }
        )
        is RoomViewState.EditForm -> RoomForm(
            editRoom = viewState.room,
            onBack = { onViewStateChange(RoomViewState.Graph) },
            onSaved = { onViewStateChange(RoomViewState.Graph) }
        )
    }
}

@Composable
fun RoomGraphView(
    onAddClick: () -> Unit,
    onRoomClick: (RoomDto) -> Unit
) {
    var rooms by remember { mutableStateOf<List<RoomDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = ApiClient.getRooms()
            isLoading = false
            result.onSuccess { rooms = it }
                .onFailure { error = it.message }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            error = null
                            val result = ApiClient.getRooms()
                            isLoading = false
                            result.onSuccess { rooms = it }
                                .onFailure { error = it.message }
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
            rooms.isEmpty() -> {
                Text(
                    text = "No rooms yet. Tap + to create one.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                RoomGraph(
                    rooms = rooms,
                    onRoomClick = onRoomClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Room")
        }
    }
}

data class RoomPosition(val room: RoomDto, val x: Float, val y: Float)

@Composable
fun RoomGraph(
    rooms: List<RoomDto>,
    onRoomClick: (RoomDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val roomPositions = remember(rooms) {
        calculateRoomPositions(rooms)
    }

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val boxSize = 100.dp
        val boxSizePx = boxSize.value * 2.5f

        // Draw connection lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            rooms.forEach { room ->
                val fromPos = roomPositions[room.id] ?: return@forEach
                room.exitIds.forEach { exitId ->
                    val toPos = roomPositions[exitId]
                    if (toPos != null) {
                        drawLine(
                            color = Color.Gray,
                            start = Offset(
                                fromPos.x * (width - boxSizePx) + boxSizePx / 2,
                                fromPos.y * (height - boxSizePx) + boxSizePx / 2
                            ),
                            end = Offset(
                                toPos.x * (width - boxSizePx) + boxSizePx / 2,
                                toPos.y * (height - boxSizePx) + boxSizePx / 2
                            ),
                            strokeWidth = 3f
                        )
                    }
                }
            }
        }

        // Draw room boxes
        rooms.forEach { room ->
            val pos = roomPositions[room.id] ?: return@forEach
            Box(
                modifier = Modifier
                    .offset(
                        x = (pos.x * (width - boxSizePx) / 2.5f).dp,
                        y = (pos.y * (height - boxSizePx) / 2.5f).dp
                    )
                    .size(boxSize)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onRoomClick(room) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = room.id,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun calculateRoomPositions(rooms: List<RoomDto>): Map<String, RoomPosition> {
    if (rooms.isEmpty()) return emptyMap()

    val positions = mutableMapOf<String, RoomPosition>()
    val count = rooms.size

    // Arrange rooms in a circle or grid pattern
    if (count == 1) {
        positions[rooms[0].id] = RoomPosition(rooms[0], 0.5f, 0.5f)
    } else {
        rooms.forEachIndexed { index, room ->
            val angle = (2 * Math.PI * index / count) - Math.PI / 2
            val radius = 0.35f
            val x = 0.5f + (radius * cos(angle)).toFloat()
            val y = 0.5f + (radius * sin(angle)).toFloat()
            positions[room.id] = RoomPosition(room, x, y)
        }
    }

    return positions
}

@Composable
fun RoomForm(
    editRoom: RoomDto?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val isEditMode = editRoom != null
    var id by remember { mutableStateOf(editRoom?.id ?: "") }
    var desc by remember { mutableStateOf(editRoom?.desc ?: "") }
    var itemIds by remember { mutableStateOf(editRoom?.itemIds?.joinToString(", ") ?: "") }
    var creatureIds by remember { mutableStateOf(editRoom?.creatureIds?.joinToString(", ") ?: "") }
    var exitIds by remember { mutableStateOf(editRoom?.exitIds?.joinToString(", ") ?: "") }
    var features by remember { mutableStateOf(editRoom?.features?.joinToString(", ") ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isEditMode) "Edit Room" else "Create Room",
                style = MaterialTheme.typography.titleLarge
            )
        }

        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isEditMode
        )

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = itemIds,
            onValueChange = { itemIds = it },
            label = { Text("Item IDs (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = creatureIds,
            onValueChange = { creatureIds = it },
            label = { Text("Creature IDs (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = exitIds,
            onValueChange = { exitIds = it },
            label = { Text("Exit IDs (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = features,
            onValueChange = { features = it },
            label = { Text("Features (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    message = null
                    val request = CreateRoomRequest(
                        id = id,
                        desc = desc,
                        itemIds = itemIds.splitToList(),
                        creatureIds = creatureIds.splitToList(),
                        exitIds = exitIds.splitToList(),
                        features = features.splitToList()
                    )
                    val result = if (isEditMode) {
                        ApiClient.updateRoom(id, request)
                    } else {
                        ApiClient.createRoom(request)
                    }
                    isLoading = false
                    if (result.isSuccess) {
                        onSaved()
                    } else {
                        message = "Error: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            enabled = !isLoading && id.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isEditMode) "Update Room" else "Create Room")
            }
        }

        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MonsterForm() {
    var id by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var itemIds by remember { mutableStateOf("") }
    var features by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = itemIds,
            onValueChange = { itemIds = it },
            label = { Text("Item IDs (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = features,
            onValueChange = { features = it },
            label = { Text("Features (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    message = null
                    val result = ApiClient.createCreature(
                        CreateCreatureRequest(
                            id = id,
                            desc = desc,
                            itemIds = itemIds.splitToList(),
                            features = features.splitToList()
                        )
                    )
                    isLoading = false
                    message = if (result.isSuccess) {
                        id = ""; desc = ""; itemIds = ""; features = ""
                        "Monster created successfully!"
                    } else {
                        "Error: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            enabled = !isLoading && id.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Create Monster")
            }
        }

        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ItemForm() {
    var id by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var featureIds by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = featureIds,
            onValueChange = { featureIds = it },
            label = { Text("Feature IDs (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    message = null
                    val result = ApiClient.createItem(
                        CreateItemRequest(
                            id = id,
                            desc = desc,
                            featureIds = featureIds.splitToList()
                        )
                    )
                    isLoading = false
                    message = if (result.isSuccess) {
                        id = ""; desc = ""; featureIds = ""
                        "Item created successfully!"
                    } else {
                        "Error: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            enabled = !isLoading && id.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Create Item")
            }
        }

        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun String.splitToList(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }
