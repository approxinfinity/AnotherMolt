package com.ez2bg.anotherthread.ui

import com.ez2bg.anotherthread.updateUrlWithCacheBuster
import com.ez2bg.anotherthread.getInitialViewParam
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.state.UserStateHolder
import com.ez2bg.anotherthread.storage.AuthStorage

// Import all admin composables from the admin package
import com.ez2bg.anotherthread.ui.admin.AbilityForm
import com.ez2bg.anotherthread.ui.admin.AdminPanelView
import com.ez2bg.anotherthread.ui.admin.AuditLogsView
import com.ez2bg.anotherthread.ui.admin.ClassDetailView
import com.ez2bg.anotherthread.ui.admin.ClassForm
import com.ez2bg.anotherthread.ui.admin.ClassListView
import com.ez2bg.anotherthread.ui.admin.CreatureDetailView
import com.ez2bg.anotherthread.ui.admin.CreatureForm
import com.ez2bg.anotherthread.ui.admin.CreatureListView
import com.ez2bg.anotherthread.ui.admin.DragonHeader
import com.ez2bg.anotherthread.ui.admin.ItemDetailView
import com.ez2bg.anotherthread.ui.admin.ItemForm
import com.ez2bg.anotherthread.ui.admin.ItemListView
import com.ez2bg.anotherthread.ui.admin.LocationForm
import com.ez2bg.anotherthread.ui.admin.LocationGraphView
import com.ez2bg.anotherthread.ui.admin.UserAuthView
import com.ez2bg.anotherthread.ui.screens.AdventureScreen
import com.ez2bg.anotherthread.ui.admin.UserDetailView
import com.ez2bg.anotherthread.ui.admin.UserProfileView

@Composable
fun AdminScreen() {
    // Restore persisted auth state on startup
    val savedUser = remember { AuthStorage.getUser() }

    // Parse initial view from URL parameter with validation
    val rawViewParam = remember { getInitialViewParam() }

    // List of valid view param prefixes - if URL param doesn't match any, reset to default
    val validViewPrefixes = listOf(
        "profile", "auth", "user", "creatures", "creature-new", "creature-",
        "items", "item-new", "item-", "admin", "logs", "location-new", "location-",
        "map", "classes", "class-"
    )

    // Validate the param - if invalid, treat as null (will default to map)
    val initialViewParam = remember {
        rawViewParam?.let { param ->
            if (validViewPrefixes.any { param.startsWith(it) } || param.isEmpty()) {
                param
            } else {
                // Invalid param - reset URL and return null to default to map
                println("DEBUG: Invalid view param '$param', resetting to default")
                updateUrlWithCacheBuster("map")
                null
            }
        }
    }

    val initialTab = remember {
        when {
            initialViewParam?.startsWith("profile") == true -> AdminTab.USER
            initialViewParam?.startsWith("auth") == true -> AdminTab.USER
            initialViewParam?.startsWith("user") == true -> AdminTab.USER
            initialViewParam?.startsWith("creature") == true -> AdminTab.CREATURE
            initialViewParam?.startsWith("item") == true -> AdminTab.ITEM
            initialViewParam?.startsWith("class") == true -> AdminTab.CLASS
            initialViewParam?.startsWith("admin") == true -> AdminTab.LOCATION // Admin panel is under location tab
            initialViewParam?.startsWith("logs") == true -> AdminTab.LOCATION // Audit logs under location tab
            else -> AdminTab.LOCATION
        }
    }
    val initialViewState = remember {
        when {
            initialViewParam?.startsWith("profile") == true ->
                if (savedUser != null) ViewState.UserProfile(savedUser) else ViewState.UserAuth
            initialViewParam?.startsWith("auth") == true -> ViewState.UserAuth
            initialViewParam?.startsWith("creatures") == true -> ViewState.CreatureList
            initialViewParam?.startsWith("creature-new") == true -> ViewState.CreatureCreate
            initialViewParam?.startsWith("items") == true -> ViewState.ItemList
            initialViewParam?.startsWith("item-new") == true -> ViewState.ItemCreate
            initialViewParam?.startsWith("classes") == true -> ViewState.ClassList
            initialViewParam?.startsWith("admin") == true -> ViewState.AdminPanel
            initialViewParam?.startsWith("logs") == true -> ViewState.AuditLogs
            initialViewParam?.startsWith("location-new") == true -> ViewState.LocationCreate
            else -> ViewState.LocationGraph() // Default to map
        }
    }

    var selectedTab by remember { mutableStateOf(initialTab) }
    var viewState by remember { mutableStateOf<ViewState>(initialViewState) }
    var currentUser by remember { mutableStateOf(savedUser) }

    // Separate refresh key for forcing location graph refresh after CRUD operations
    var locationGraphRefreshKey by remember { mutableStateOf(0L) }
    fun refreshLocationGraph() {
        locationGraphRefreshKey++
        println("DEBUG: refreshLocationGraph() called, new key: $locationGraphRefreshKey")
    }

    // Game mode state - ADVENTURE for playing, CREATE for editing
    // Auto-start in Adventure mode if user has complete profile (class assigned) and last known location
    val startInAdventureMode = remember {
        savedUser != null &&
        savedUser.characterClassId != null &&  // Profile complete (has class)
        savedUser.currentLocationId != null    // Has last known location
    }
    var gameMode by remember { mutableStateOf(if (startInAdventureMode) GameMode.ADVENTURE else GameMode.CREATE) }

    // Set user context for audit logging when user changes
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            ApiClient.setUserContext(currentUser!!.id, currentUser!!.name)
        } else {
            ApiClient.clearUserContext()
        }
    }

    // Refresh user data from server on startup to get latest featureIds, etc.
    LaunchedEffect(savedUser?.id) {
        savedUser?.let { user ->
            ApiClient.getUser(user.id).onSuccess { freshUser ->
                if (freshUser != null) {
                    AuthStorage.saveUser(freshUser)
                    currentUser = freshUser
                }
            }
        }
    }

    // Update browser URL with cache buster on every view change (web only)
    LaunchedEffect(viewState) {
        val viewName = when (viewState) {
            is ViewState.UserAuth -> "auth"
            is ViewState.UserProfile -> "profile"
            is ViewState.UserDetail -> "user"
            is ViewState.LocationGraph -> "map"
            is ViewState.LocationCreate -> "location-new"
            is ViewState.LocationEdit -> "location"
            is ViewState.CreatureList -> "creatures"
            is ViewState.CreatureCreate -> "creature-new"
            is ViewState.CreatureEdit -> "creature"
            is ViewState.CreatureDetail -> "creature"
            is ViewState.ItemList -> "items"
            is ViewState.ItemCreate -> "item-new"
            is ViewState.ItemEdit -> "item"
            is ViewState.ItemDetail -> "item"
            is ViewState.AdminPanel -> "admin"
            is ViewState.AuditLogs -> "logs"
            is ViewState.ClassList -> "classes"
            is ViewState.ClassCreate -> "class-create"
            is ViewState.ClassEdit -> "class-edit"
            is ViewState.ClassDetail -> "class-${(viewState as ViewState.ClassDetail).classId}"
            is ViewState.AbilityCreate -> "ability-create"
            is ViewState.AbilityEdit -> "ability-edit"
        }
        updateUrlWithCacheBuster(viewName)
    }

    // Check if current user has admin privilege
    val isAdmin = currentUser?.featureIds?.contains(ADMIN_FEATURE_ID) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (gameMode.isAdventure) 0.dp else 16.dp)
    ) {
        // Hide header and tabs in adventure mode (full screen)
        if (gameMode.isCreate) {
            DragonHeader(modifier = Modifier.padding(bottom = 4.dp))

            // All tabs are visible
            val visibleTabs = AdminTab.entries

            TabRow(selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)) {
                visibleTabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            viewState = when (tab) {
                                AdminTab.USER -> if (currentUser != null) ViewState.UserProfile(currentUser!!) else ViewState.UserAuth
                                AdminTab.LOCATION -> ViewState.LocationGraph()
                                AdminTab.CREATURE -> ViewState.CreatureList
                                AdminTab.ITEM -> ViewState.ItemList
                                AdminTab.CLASS -> ViewState.ClassList
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        when (val state = viewState) {
            is ViewState.UserAuth -> UserAuthView(
                onAuthenticated = { user ->
                    AuthStorage.saveUser(user)
                    currentUser = user
                    viewState = ViewState.UserProfile(user)
                }
            )
            is ViewState.UserProfile -> UserProfileView(
                user = state.user,
                currentUser = currentUser,
                isAdmin = isAdmin,
                onUserUpdated = { user ->
                    AuthStorage.saveUser(user)
                    currentUser = user
                    viewState = ViewState.UserProfile(user)
                },
                onLogout = {
                    UserStateHolder.logout()
                    // Navigation to login will be handled by App listening to authEvents
                },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                onBack = null,  // No back button for own profile
                onNavigateToAdmin = if (isAdmin) {{ viewState = ViewState.AdminPanel }} else null
            )
            is ViewState.UserDetail -> UserDetailView(
                userId = state.userId,
                currentUser = currentUser,
                isAdmin = isAdmin,
                onBack = {
                    selectedTab = AdminTab.LOCATION
                    viewState = ViewState.LocationGraph()
                },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                }
            )
            is ViewState.LocationGraph -> key(locationGraphRefreshKey) {
                // Block if user is authenticated but profile incomplete
                val user = currentUser
                if (user != null && !user.hasCompleteProfile()) {
                    IncompleteProfileBlocker(
                        onNavigateToProfile = {
                            selectedTab = AdminTab.USER
                            viewState = ViewState.UserProfile(user)
                        }
                    )
                } else if (gameMode.isAdventure && currentUser != null) {
                    // Adventure mode uses dedicated AdventureScreen
                    AdventureScreen(
                        currentUser = currentUser,
                        onSwitchToCreate = { gameMode = GameMode.CREATE },
                        onViewLocationDetails = { location -> viewState = ViewState.LocationEdit(location, gameMode) }
                    )
                } else {
                    // Create mode uses LocationGraphView
                    LocationGraphView(
                        refreshKey = locationGraphRefreshKey,
                        onAddClick = { viewState = ViewState.LocationCreate },
                        onLocationClick = { location -> viewState = ViewState.LocationEdit(location, gameMode) },
                        isAuthenticated = currentUser != null,
                        isAdmin = isAdmin,
                        currentUser = currentUser,
                        onLoginClick = {
                            selectedTab = AdminTab.USER
                            viewState = ViewState.UserAuth
                        },
                        gameMode = gameMode,
                        onGameModeChange = { gameMode = it }
                    )
                }
            }
            is ViewState.LocationCreate -> LocationForm(
                editLocation = null,
                onBack = { viewState = ViewState.LocationGraph() },
                onSaved = { refreshLocationGraph(); viewState = ViewState.LocationGraph() },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                onNavigateToCreature = { id ->
                    selectedTab = AdminTab.CREATURE
                    viewState = ViewState.CreatureDetail(id)
                },
                onNavigateToLocation = { location -> viewState = ViewState.LocationEdit(location) },
                onNavigateToUser = { userId ->
                    selectedTab = AdminTab.USER
                    viewState = ViewState.UserDetail(userId)
                },
                currentUser = currentUser,
                isAdmin = isAdmin,
                onDeleted = { refreshLocationGraph(); viewState = ViewState.LocationGraph() }
            )
            is ViewState.LocationEdit -> LocationForm(
                editLocation = state.location,
                onBack = { viewState = ViewState.LocationGraph() },
                onSaved = { refreshLocationGraph(); viewState = ViewState.LocationGraph() },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id, state.gameMode)
                },
                onNavigateToCreature = { id ->
                    selectedTab = AdminTab.CREATURE
                    viewState = ViewState.CreatureDetail(id, state.gameMode)
                },
                onNavigateToLocation = { location -> viewState = ViewState.LocationEdit(location, state.gameMode) },
                onNavigateToUser = { userId ->
                    selectedTab = AdminTab.USER
                    viewState = ViewState.UserDetail(userId)
                },
                currentUser = currentUser,
                isAdmin = isAdmin,
                onLocationUpdated = { updatedLocation ->
                    viewState = ViewState.LocationEdit(updatedLocation, state.gameMode)
                },
                onDeleted = { refreshLocationGraph(); viewState = ViewState.LocationGraph() },
                gameMode = state.gameMode
            )
            is ViewState.CreatureList -> {
                // Block if user is authenticated but profile incomplete
                val user = currentUser
                if (user != null && !user.hasCompleteProfile()) {
                    IncompleteProfileBlocker(
                        onNavigateToProfile = {
                            selectedTab = AdminTab.USER
                            viewState = ViewState.UserProfile(user)
                        }
                    )
                } else {
                    CreatureListView(
                        onCreatureClick = { creature ->
                            viewState = ViewState.CreatureEdit(creature)
                        },
                        onAddClick = {
                            viewState = ViewState.CreatureCreate
                        },
                        isAuthenticated = currentUser != null
                    )
                }
            }
            is ViewState.CreatureCreate -> CreatureForm(
                editCreature = null,
                onBack = { viewState = ViewState.CreatureList },
                onSaved = { viewState = ViewState.CreatureList },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                currentUser = currentUser,
                isAdmin = isAdmin
            )
            is ViewState.CreatureEdit -> CreatureForm(
                editCreature = state.creature,
                onBack = { viewState = ViewState.CreatureList },
                onSaved = { viewState = ViewState.CreatureList },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id)
                },
                currentUser = currentUser,
                isAdmin = isAdmin,
                onCreatureUpdated = { updatedCreature ->
                    viewState = ViewState.CreatureEdit(updatedCreature)
                }
            )
            is ViewState.CreatureDetail -> CreatureDetailView(
                creatureId = state.id,
                onBack = {
                    selectedTab = AdminTab.LOCATION
                    viewState = ViewState.LocationGraph()
                },
                onEdit = { creature ->
                    selectedTab = AdminTab.CREATURE
                    viewState = ViewState.CreatureEdit(creature)
                },
                onCreateNew = {
                    selectedTab = AdminTab.CREATURE
                    viewState = ViewState.CreatureCreate
                },
                onNavigateToItem = { id ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemDetail(id, state.gameMode)
                },
                isAdmin = isAdmin,
                gameMode = state.gameMode
            )
            is ViewState.ItemList -> {
                // Block if user is authenticated but profile incomplete
                val user = currentUser
                if (user != null && !user.hasCompleteProfile()) {
                    IncompleteProfileBlocker(
                        onNavigateToProfile = {
                            selectedTab = AdminTab.USER
                            viewState = ViewState.UserProfile(user)
                        }
                    )
                } else {
                    ItemListView(
                        onItemClick = { item ->
                            viewState = ViewState.ItemEdit(item)
                        },
                        onAddClick = {
                            viewState = ViewState.ItemCreate
                        },
                        isAuthenticated = currentUser != null
                    )
                }
            }
            is ViewState.ItemCreate -> ItemForm(
                editItem = null,
                onBack = { viewState = ViewState.ItemList },
                onSaved = { viewState = ViewState.ItemList },
                currentUser = currentUser,
                isAdmin = isAdmin
            )
            is ViewState.ItemEdit -> ItemForm(
                editItem = state.item,
                onBack = { viewState = ViewState.ItemList },
                onSaved = { viewState = ViewState.ItemList },
                currentUser = currentUser,
                isAdmin = isAdmin,
                onItemUpdated = { updatedItem ->
                    viewState = ViewState.ItemEdit(updatedItem)
                }
            )
            is ViewState.ItemDetail -> ItemDetailView(
                itemId = state.id,
                onBack = {
                    selectedTab = AdminTab.LOCATION
                    viewState = ViewState.LocationGraph()
                },
                onEdit = { item ->
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemEdit(item)
                },
                onCreateNew = {
                    selectedTab = AdminTab.ITEM
                    viewState = ViewState.ItemCreate
                },
                isAdmin = isAdmin,
                gameMode = state.gameMode
            )
            is ViewState.AdminPanel -> AdminPanelView(
                onViewAuditLogs = { viewState = ViewState.AuditLogs },
                onUserClick = { userId -> viewState = ViewState.UserDetail(userId) }
            )
            is ViewState.AuditLogs -> AuditLogsView(
                onBack = { viewState = ViewState.AdminPanel }
            )
            is ViewState.ClassList -> ClassListView(
                onClassClick = { characterClass -> viewState = ViewState.ClassDetail(characterClass.id) },
                onAddClick = { viewState = ViewState.ClassCreate },
                isAdmin = isAdmin
            )
            is ViewState.ClassCreate -> ClassForm(
                editClass = null,
                onBack = { viewState = ViewState.ClassList },
                onSaved = { viewState = ViewState.ClassList }
            )
            is ViewState.ClassEdit -> ClassForm(
                editClass = state.characterClass,
                onBack = { viewState = ViewState.ClassDetail(state.characterClass.id) },
                onSaved = { viewState = ViewState.ClassList }
            )
            is ViewState.ClassDetail -> ClassDetailView(
                classId = state.classId,
                onBack = { viewState = ViewState.ClassList },
                onEdit = { characterClass -> viewState = ViewState.ClassEdit(characterClass) },
                onAddAbility = { classId -> viewState = ViewState.AbilityCreate(classId) },
                onEditAbility = { ability -> viewState = ViewState.AbilityEdit(ability) },
                isAdmin = isAdmin
            )
            is ViewState.AbilityCreate -> AbilityForm(
                editAbility = null,
                classId = state.classId,
                onBack = { viewState = ViewState.ClassDetail(state.classId) },
                onSaved = { viewState = ViewState.ClassDetail(state.classId) }
            )
            is ViewState.AbilityEdit -> AbilityForm(
                editAbility = state.ability,
                classId = state.ability.classId,
                onBack = {
                    val classId = state.ability.classId
                    if (classId != null) {
                        viewState = ViewState.ClassDetail(classId)
                    } else {
                        viewState = ViewState.ClassList
                    }
                },
                onSaved = {
                    val classId = state.ability.classId
                    if (classId != null) {
                        viewState = ViewState.ClassDetail(classId)
                    } else {
                        viewState = ViewState.ClassList
                    }
                }
            )
        }
    }
}
