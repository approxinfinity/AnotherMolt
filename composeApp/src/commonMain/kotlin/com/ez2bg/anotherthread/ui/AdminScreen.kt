package com.ez2bg.anotherthread.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

enum class AdminTab(val title: String) {
    ROOM("Room"),
    MONSTER("Monster"),
    ITEM("Item")
}

enum class EntityType {
    ROOM, CREATURE, ITEM
}

sealed class ViewState {
    data object RoomGraph : ViewState()
    data object RoomCreate : ViewState()
    data class RoomEdit(val room: RoomDto) : ViewState()
    data object CreatureCreate : ViewState()
    data class CreatureEdit(val creature: CreatureDto) : ViewState()
    data class CreatureDetail(val id: String) : ViewState()
    data object ItemCreate : ViewState()
    data class ItemEdit(val item: ItemDto) : ViewState()
    data class ItemDetail(val id: String) : ViewState()
}

@Composable
fun AdminScreen() {
    var selectedTab by remember { mutableStateOf(AdminTab.ROOM) }
    var viewState by remember { mutableStateOf<ViewState>(ViewState.RoomGraph) }

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
                        viewState = when (tab) {
                            AdminTab.ROOM -> ViewState.RoomGraph
                            AdminTab.MONSTER -> ViewState.CreatureCreate
                            AdminTab.ITEM -> ViewState.ItemCreate
                        }
                    },
                    text = { Text(tab.title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = viewState) {
            is ViewState.RoomGraph -> RoomGraphView(
                onAddClick = { viewState = ViewState.RoomCreate },
                onRoomClick = { room -> viewState = ViewState.RoomEdit(room) }
            )
            is ViewState.RoomCreate -> RoomForm(
                editRoom = null,
                onBack = { viewState = ViewState.RoomGraph },
                onSaved = { viewState = ViewState.RoomGraph },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) },
                onNavigateToCreature = { id -> viewState = ViewState.CreatureDetail(id) },
                onNavigateToRoom = { room -> viewState = ViewState.RoomEdit(room) }
            )
            is ViewState.RoomEdit -> RoomForm(
                editRoom = state.room,
                onBack = { viewState = ViewState.RoomGraph },
                onSaved = { viewState = ViewState.RoomGraph },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) },
                onNavigateToCreature = { id -> viewState = ViewState.CreatureDetail(id) },
                onNavigateToRoom = { room -> viewState = ViewState.RoomEdit(room) }
            )
            is ViewState.CreatureCreate -> CreatureForm(
                editCreature = null,
                onBack = { viewState = ViewState.CreatureCreate },
                onSaved = { viewState = ViewState.CreatureCreate },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) }
            )
            is ViewState.CreatureEdit -> CreatureForm(
                editCreature = state.creature,
                onBack = { viewState = ViewState.CreatureCreate },
                onSaved = { viewState = ViewState.CreatureCreate },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) }
            )
            is ViewState.CreatureDetail -> CreatureDetailView(
                creatureId = state.id,
                onBack = { viewState = ViewState.RoomGraph },
                onEdit = { creature -> viewState = ViewState.CreatureEdit(creature) },
                onCreateNew = { viewState = ViewState.CreatureCreate },
                onNavigateToItem = { id -> viewState = ViewState.ItemDetail(id) }
            )
            is ViewState.ItemCreate -> ItemForm(
                editItem = null,
                onBack = { viewState = ViewState.ItemCreate },
                onSaved = { viewState = ViewState.ItemCreate }
            )
            is ViewState.ItemEdit -> ItemForm(
                editItem = state.item,
                onBack = { viewState = ViewState.ItemCreate },
                onSaved = { viewState = ViewState.ItemCreate }
            )
            is ViewState.ItemDetail -> ItemDetailView(
                itemId = state.id,
                onBack = { viewState = ViewState.RoomGraph },
                onEdit = { item -> viewState = ViewState.ItemEdit(item) },
                onCreateNew = { viewState = ViewState.ItemCreate }
            )
        }
    }
}

@Composable
fun IdPill(
    id: String,
    label: String? = null,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp),
        shape = RoundedCornerShape(16.dp),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label ?: id.take(8) + if (id.length > 8) "..." else "",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onRemove != null) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onRemove),
                    tint = textColor
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdPillSection(
    label: String,
    ids: List<String>,
    entityType: EntityType,
    onPillClick: (String) -> Unit,
    onAddId: (String) -> Unit,
    onRemoveId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newId by remember { mutableStateOf("") }

    val pillColor = when (entityType) {
        EntityType.ROOM -> MaterialTheme.colorScheme.primaryContainer
        EntityType.CREATURE -> MaterialTheme.colorScheme.tertiaryContainer
        EntityType.ITEM -> MaterialTheme.colorScheme.secondaryContainer
    }
    val pillTextColor = when (entityType) {
        EntityType.ROOM -> MaterialTheme.colorScheme.onPrimaryContainer
        EntityType.CREATURE -> MaterialTheme.colorScheme.onTertiaryContainer
        EntityType.ITEM -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ids.forEach { id ->
                IdPill(
                    id = id,
                    color = pillColor,
                    textColor = pillTextColor,
                    onClick = { onPillClick(id) },
                    onRemove = { onRemoveId(id) }
                )
            }

            // Add button pill
            Surface(
                modifier = Modifier
                    .clickable { showAddDialog = true }
                    .padding(2.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newId = "" },
            title = { Text("Add ${entityType.name.lowercase().replaceFirstChar { it.uppercase() }} ID") },
            text = {
                OutlinedTextField(
                    value = newId,
                    onValueChange = { newId = it },
                    label = { Text("ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newId.isNotBlank()) {
                            onAddId(newId.trim())
                            newId = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newId = "" }) {
                    Text("Cancel")
                }
            }
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
                    text = room.name.ifBlank { room.id.take(8) },
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
    onSaved: () -> Unit,
    onNavigateToItem: (String) -> Unit,
    onNavigateToCreature: (String) -> Unit,
    onNavigateToRoom: (RoomDto) -> Unit
) {
    val isEditMode = editRoom != null
    var name by remember { mutableStateOf(editRoom?.name ?: "") }
    var desc by remember { mutableStateOf(editRoom?.desc ?: "") }
    var itemIds by remember { mutableStateOf(editRoom?.itemIds ?: emptyList()) }
    var creatureIds by remember { mutableStateOf(editRoom?.creatureIds ?: emptyList()) }
    var exitIds by remember { mutableStateOf(editRoom?.exitIds ?: emptyList()) }
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

        if (isEditMode) {
            Text(
                text = "ID: ${editRoom?.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
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

        IdPillSection(
            label = "Items",
            ids = itemIds,
            entityType = EntityType.ITEM,
            onPillClick = onNavigateToItem,
            onAddId = { id -> itemIds = itemIds + id },
            onRemoveId = { id -> itemIds = itemIds - id }
        )

        IdPillSection(
            label = "Creatures",
            ids = creatureIds,
            entityType = EntityType.CREATURE,
            onPillClick = onNavigateToCreature,
            onAddId = { id -> creatureIds = creatureIds + id },
            onRemoveId = { id -> creatureIds = creatureIds - id }
        )

        IdPillSection(
            label = "Exits (Room IDs)",
            ids = exitIds,
            entityType = EntityType.ROOM,
            onPillClick = { id ->
                scope.launch {
                    ApiClient.getRoom(id).onSuccess { room ->
                        if (room != null) onNavigateToRoom(room)
                    }
                }
            },
            onAddId = { id -> exitIds = exitIds + id },
            onRemoveId = { id -> exitIds = exitIds - id }
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
                        name = name,
                        desc = desc,
                        itemIds = itemIds,
                        creatureIds = creatureIds,
                        exitIds = exitIds,
                        features = features.splitToList()
                    )
                    val result = if (isEditMode) {
                        ApiClient.updateRoom(editRoom!!.id, request)
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
            enabled = !isLoading && name.isNotBlank(),
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
fun CreatureForm(
    editCreature: CreatureDto?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToItem: (String) -> Unit
) {
    val isEditMode = editCreature != null
    var name by remember { mutableStateOf(editCreature?.name ?: "") }
    var desc by remember { mutableStateOf(editCreature?.desc ?: "") }
    var itemIds by remember { mutableStateOf(editCreature?.itemIds ?: emptyList()) }
    var features by remember { mutableStateOf(editCreature?.features?.joinToString(", ") ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Edit Creature",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = "ID: ${editCreature?.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
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

        IdPillSection(
            label = "Items",
            ids = itemIds,
            entityType = EntityType.ITEM,
            onPillClick = onNavigateToItem,
            onAddId = { id -> itemIds = itemIds + id },
            onRemoveId = { id -> itemIds = itemIds - id }
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
                            name = name,
                            desc = desc,
                            itemIds = itemIds,
                            features = features.splitToList()
                        )
                    )
                    isLoading = false
                    message = if (result.isSuccess) {
                        name = ""; desc = ""; itemIds = emptyList(); features = ""
                        "Creature created successfully!"
                    } else {
                        "Error: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            enabled = !isLoading && name.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isEditMode) "Update Creature" else "Create Creature")
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
fun CreatureDetailView(
    creatureId: String,
    onBack: () -> Unit,
    onEdit: (CreatureDto) -> Unit,
    onCreateNew: () -> Unit,
    onNavigateToItem: (String) -> Unit
) {
    var creature by remember { mutableStateOf<CreatureDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }

    LaunchedEffect(creatureId) {
        isLoading = true
        ApiClient.getCreature(creatureId).onSuccess {
            creature = it
            notFound = it == null
        }.onFailure {
            notFound = true
        }
        isLoading = false
    }

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
                text = "Creature Details",
                style = MaterialTheme.typography.titleLarge
            )
        }

        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            notFound -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Creature not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "ID: $creatureId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCreateNew) {
                            Text("Create New Creature")
                        }
                    }
                }
            }
            creature != null -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = creature!!.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "ID: ${creature!!.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = creature!!.desc,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (creature!!.itemIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            IdPillSection(
                                label = "Items",
                                ids = creature!!.itemIds,
                                entityType = EntityType.ITEM,
                                onPillClick = onNavigateToItem,
                                onAddId = {},
                                onRemoveId = {}
                            )
                        }

                        if (creature!!.features.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Features: ${creature!!.features.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Button(
                    onClick = { onEdit(creature!!) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Edit Creature")
                }
            }
        }
    }
}

@Composable
fun ItemForm(
    editItem: ItemDto?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val isEditMode = editItem != null
    var name by remember { mutableStateOf(editItem?.name ?: "") }
    var desc by remember { mutableStateOf(editItem?.desc ?: "") }
    var featureIds by remember { mutableStateOf(editItem?.featureIds?.joinToString(", ") ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Edit Item",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = "ID: ${editItem?.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
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
                            name = name,
                            desc = desc,
                            featureIds = featureIds.splitToList()
                        )
                    )
                    isLoading = false
                    message = if (result.isSuccess) {
                        name = ""; desc = ""; featureIds = ""
                        "Item created successfully!"
                    } else {
                        "Error: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            enabled = !isLoading && name.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isEditMode) "Update Item" else "Create Item")
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
fun ItemDetailView(
    itemId: String,
    onBack: () -> Unit,
    onEdit: (ItemDto) -> Unit,
    onCreateNew: () -> Unit
) {
    var item by remember { mutableStateOf<ItemDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        isLoading = true
        ApiClient.getItem(itemId).onSuccess {
            item = it
            notFound = it == null
        }.onFailure {
            notFound = true
        }
        isLoading = false
    }

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
                text = "Item Details",
                style = MaterialTheme.typography.titleLarge
            )
        }

        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            notFound -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Item not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "ID: $itemId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCreateNew) {
                            Text("Create New Item")
                        }
                    }
                }
            }
            item != null -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = item!!.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "ID: ${item!!.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item!!.desc,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (item!!.featureIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Features: ${item!!.featureIds.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Button(
                    onClick = { onEdit(item!!) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Edit Item")
                }
            }
        }
    }
}

private fun String.splitToList(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }
