package com.example.ui.home

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AiConfigurationState
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AppFontFamily
import com.example.domain.model.FlowProject
import com.example.domain.model.FlowTemplate
import com.example.domain.model.GridStyle
import com.example.domain.model.ThemeMode
import com.example.domain.templates.FlowTemplates
import com.example.ui.components.FlowBuilderLogoMark
import com.example.ui.components.FlowPreviewCanvas
import com.example.ui.profile.FlowFaqDialog
import com.example.ui.profile.FlowHamburgerDrawerContent
import com.example.ui.profile.SettingsScreen
import com.example.ui.profile.UserAvatarCircle
import com.example.ui.theme.FlowTeal600
import com.example.ui.theme.SlateInk900
import com.example.viewmodel.HomeUiState
import kotlinx.coroutines.launch

enum class HomeTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    PROJECTS("My Flows", Icons.Filled.Folder, Icons.Outlined.Folder),
    TEMPLATES("Templates", Icons.Filled.AutoAwesomeMosaic, Icons.Outlined.AutoAwesomeMosaic),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    activeTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    onSearchChange: (String) -> Unit,
    onSelectTemplateCategory: (String) -> Unit,
    onCreateNewProject: (String) -> Unit,
    onCreateProjectWithAi: (title: String, prompt: String, onSuccessClose: () -> Unit) -> Unit,
    onClearAiError: () -> Unit,
    onOpenProject: (String) -> Unit,
    onUseTemplate: (FlowTemplate) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDuplicateProject: (FlowProject) -> Unit,
    onToggleFavorite: (FlowProject) -> Unit,
    onDeleteProject: (FlowProject) -> Unit,
    onExportProjectJson: (FlowProject) -> Unit,
    onImportJsonClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAiChat: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onSaveAiApiKey: (String) -> Unit,
    onTestAiConnection: (String) -> Unit,
    onRemoveAiApiKey: () -> Unit,
    onGridStyleChange: (GridStyle) -> Unit,
    onSnapToGridChange: (Boolean) -> Unit,
    onAlignmentGuidesChange: (Boolean) -> Unit,
    onValidationBadgesChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createDialogStartWithAi by remember { mutableStateOf(false) }
    var showDrawerFaqDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FlowProject?>(null) }
    var deleteTarget by remember { mutableStateOf<FlowProject?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FlowHamburgerDrawerContent(
                uiState = uiState,
                onToggleDarkLight = {
                    val next = if (uiState.settings.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                    onThemeModeChange(next)
                },
                onNavigateHome = {
                    scope.launch { drawerState.close() }
                    onSelectTab(HomeTab.HOME)
                },
                onCreateFlow = {
                    scope.launch { drawerState.close() }
                    createDialogStartWithAi = false
                    onClearAiError()
                    showCreateDialog = true
                },
                onCreateWithAi = {
                    scope.launch { drawerState.close() }
                    createDialogStartWithAi = true
                    onClearAiError()
                    showCreateDialog = true
                },
                onOpenAiChat = {
                    scope.launch { drawerState.close() }
                    onOpenAiChat()
                },
                onNavigateMyFlows = {
                    scope.launch { drawerState.close() }
                    onSelectTab(HomeTab.PROJECTS)
                },
                onNavigateTemplates = {
                    scope.launch { drawerState.close() }
                    onSelectTab(HomeTab.TEMPLATES)
                },
                onImportJson = {
                    scope.launch { drawerState.close() }
                    onImportJsonClick()
                },
                onOpenProfile = {
                    scope.launch { drawerState.close() }
                    onOpenProfile()
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onSelectTab(HomeTab.SETTINGS)
                },
                onOpenFaq = {
                    scope.launch { drawerState.close() }
                    showDrawerFaqDialog = true
                },
                onSignOut = {
                    scope.launch { drawerState.close() }
                    onSignOut()
                },
            )
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 680.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            ) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                                }
                                FlowBuilderLogoMark(size = 38.dp)
                            }
                        },
                    ) {
                        HomeTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = activeTab == tab,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!isWideScreen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                            ) {
                                HomeTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = activeTab == tab,
                                        onClick = { onSelectTab(tab) },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label,
                                            )
                                        },
                                        label = {
                                            Text(
                                                tab.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (activeTab == HomeTab.HOME || activeTab == HomeTab.PROJECTS) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    createDialogStartWithAi = false
                                    onClearAiError()
                                    showCreateDialog = true
                                },
                                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                text = { Text("New Flow") },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.testTag("fab_create_flowchart"),
                            )
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        val contentModifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1120.dp)

                        when (activeTab) {
                            HomeTab.HOME -> DashboardContent(
                                uiState = uiState,
                                modifier = contentModifier,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenProfile = onOpenProfile,
                                onOpenAiChat = onOpenAiChat,
                                onSearchChange = onSearchChange,
                                onCreateClick = {
                                    createDialogStartWithAi = false
                                    onClearAiError()
                                    showCreateDialog = true
                                },
                                onImportJsonClick = onImportJsonClick,
                                onOpenProject = onOpenProject,
                                onUseTemplate = onUseTemplate,
                                onSeeAllProjects = { onSelectTab(HomeTab.PROJECTS) },
                                onSeeAllTemplates = { onSelectTab(HomeTab.TEMPLATES) },
                                onRename = { renameTarget = it },
                                onDuplicate = onDuplicateProject,
                                onToggleFavorite = onToggleFavorite,
                                onDelete = { deleteTarget = it },
                                onExportJson = onExportProjectJson,
                            )

                            HomeTab.PROJECTS -> MyFlowchartsContent(
                                uiState = uiState,
                                isWide = isWideScreen,
                                modifier = contentModifier,
                                onSearchChange = onSearchChange,
                                onCreateClick = {
                                    createDialogStartWithAi = false
                                    onClearAiError()
                                    showCreateDialog = true
                                },
                                onImportJsonClick = onImportJsonClick,
                                onOpenProject = onOpenProject,
                                onRename = { renameTarget = it },
                                onDuplicate = onDuplicateProject,
                                onToggleFavorite = onToggleFavorite,
                                onDelete = { deleteTarget = it },
                                onExportJson = onExportProjectJson,
                            )

                            HomeTab.TEMPLATES -> TemplatesLibraryContent(
                                uiState = uiState,
                                isWide = isWideScreen,
                                modifier = contentModifier,
                                onSearchChange = onSearchChange,
                                onSelectCategory = onSelectTemplateCategory,
                                onUseTemplate = onUseTemplate,
                            )

                            HomeTab.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                showBackArrow = false,
                                onBack = { onSelectTab(HomeTab.HOME) },
                                onUpdateProfile = onUpdateProfile,
                                onThemeModeChange = onThemeModeChange,
                                onFontFamilyChange = onFontFamilyChange,
                                onAutoSaveChange = onAutoSaveChange,
                                onNotificationsChange = onNotificationsChange,
                                onSaveAiApiKey = onSaveAiApiKey,
                                onTestAiConnection = onTestAiConnection,
                                onRemoveAiApiKey = onRemoveAiApiKey,
                                onGridStyleChange = onGridStyleChange,
                                onSnapToGridChange = onSnapToGridChange,
                                onAlignmentGuidesChange = onAlignmentGuidesChange,
                                onValidationBadgesChange = onValidationBadgesChange,
                                onImportJsonClick = onImportJsonClick,
                                modifier = contentModifier,
                            )
                        }
                    }
                }
            }
        }
    }

    // Create New Flowchart Dialog (with animated "Make by AI" toggle + prompt box!)
    if (showCreateDialog) {
        CreateFlowchartDialog(
            initialMakeByAi = createDialogStartWithAi,
            aiConfig = uiState.aiConfig,
            isGeneratingAi = uiState.isGeneratingAi,
            aiErrorMessage = uiState.aiErrorMessage,
            onDismiss = {
                if (!uiState.isGeneratingAi) {
                    showCreateDialog = false
                    onClearAiError()
                }
            },
            onOpenSettingsForKey = {
                showCreateDialog = false
                onClearAiError()
                onSelectTab(HomeTab.SETTINGS)
            },
            onCreateBlank = { title ->
                showCreateDialog = false
                onCreateNewProject(title)
            },
            onCreateWithAi = { title, prompt ->
                onCreateProjectWithAi(title, prompt) {
                    showCreateDialog = false
                }
            },
        )
    }

    if (showDrawerFaqDialog) {
        FlowFaqDialog(onDismiss = { showDrawerFaqDialog = false })
    }

    // Rename Dialog
    renameTarget?.let { project ->
        var newTitle by remember(project.id) { mutableStateOf(project.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Flowchart") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Project name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameProject(project.id, newTitle)
                        renameTarget = null
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Delete \"${project.title}\"?") },
            text = {
                Text(
                    "This flowchart will be permanently removed from your account and device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(project)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CreateFlowchartDialog(
    initialMakeByAi: Boolean,
    aiConfig: AiConfigurationState,
    isGeneratingAi: Boolean,
    aiErrorMessage: String?,
    onDismiss: () -> Unit,
    onOpenSettingsForKey: () -> Unit,
    onCreateBlank: (String) -> Unit,
    onCreateWithAi: (title: String, prompt: String) -> Unit,
) {
    var titleInput by remember { mutableStateOf("") }
    var makeByAi by remember(initialMakeByAi) { mutableStateOf(initialMakeByAi) }
    var aiPrompt by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "Binary Search Algorithm",
        "Student Login & OTP Auth",
        "ATM Cash Withdrawal",
        "E-Commerce Checkout Flow",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (makeByAi) Icons.Filled.AutoAwesome else Icons.Filled.AccountTree,
                contentDescription = null,
                tint = FlowTeal600,
            )
        },
        title = {
            Text(
                text = if (makeByAi) "Make Flowchart by AI" else "Create New Flowchart",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    singleLine = true,
                    enabled = !isGeneratingAi,
                    label = { Text("Project name") },
                    placeholder = { Text("e.g., Sorting Algorithm") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input"),
                )

                // Make by AI toggle card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (makeByAi) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (makeByAi) FlowTeal600 else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isGeneratingAi) { makeByAi = !makeByAi }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = FlowTeal600,
                                modifier = Modifier.size(20.dp),
                            )
                            Column {
                                Text(
                                    text = "Make by AI",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Auto-build flowchart from algorithm prompt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = makeByAi,
                            enabled = !isGeneratingAi,
                            onCheckedChange = { makeByAi = it },
                            modifier = Modifier.testTag("toggle_make_by_ai"),
                        )
                    }
                }

                // Animated prompt area expanding smoothly when Make by AI is ON
                AnimatedVisibility(
                    visible = makeByAi,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            enabled = !isGeneratingAi,
                            minLines = 3,
                            maxLines = 5,
                            label = { Text("Describe your algorithm or process") },
                            placeholder = { Text("e.g., User registers, verifies email, logs in or resets password…") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_prompt_input"),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            quickPrompts.forEach { sample ->
                                FilterChip(
                                    selected = aiPrompt == sample,
                                    enabled = !isGeneratingAi,
                                    onClick = {
                                        aiPrompt = sample
                                        if (titleInput.isBlank()) titleInput = sample
                                    },
                                    label = { Text(sample, fontSize = 11.5.sp) },
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = when {
                                        aiConfig.status == AiConnectionStatus.CONNECTED && aiConfig.provider != null ->
                                            "Connected • Using your ${aiConfig.provider.displayName} API key."
                                        aiConfig.hasApiKey && aiConfig.status == AiConnectionStatus.INVALID_API_KEY ->
                                            "Invalid API Key — update it in AI Configuration."
                                        aiConfig.hasApiKey ->
                                            "Using your saved AI key from Settings."
                                        else ->
                                            "Built-in FlowBuilder AI ready — or connect Groq/OpenRouter."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onOpenSettingsForKey) {
                                    Text("AI Config", fontSize = 11.5.sp)
                                }
                            }
                        }

                        if (isGeneratingAi) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.6.dp,
                                        color = FlowTeal600,
                                    )
                                    Text(
                                        text = "Building & arranging your AI flowchart…",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        if (!aiErrorMessage.isNullOrBlank()) {
                            Text(
                                text = aiErrorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isGeneratingAi && (!makeByAi || aiPrompt.isNotBlank()),
                onClick = {
                    if (makeByAi) {
                        onCreateWithAi(titleInput, aiPrompt)
                    } else {
                        onCreateBlank(titleInput)
                    }
                },
                modifier = Modifier.testTag("confirm_create_project_button"),
            ) {
                if (makeByAi) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isGeneratingAi) "Generating…" else "Generate")
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isGeneratingAi,
                onClick = onDismiss,
            ) { Text("Cancel") }
        },
    )
}

@Composable
private fun DashboardContent(
    uiState: HomeUiState,
    modifier: Modifier,
    onOpenDrawer: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAiChat: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    onImportJsonClick: () -> Unit,
    onOpenProject: (String) -> Unit,
    onUseTemplate: (FlowTemplate) -> Unit,
    onSeeAllProjects: () -> Unit,
    onSeeAllTemplates: () -> Unit,
    onRename: (FlowProject) -> Unit,
    onDuplicate: (FlowProject) -> Unit,
    onToggleFavorite: (FlowProject) -> Unit,
    onDelete: (FlowProject) -> Unit,
    onExportJson: (FlowProject) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Branded Header: Left Hamburger + FlowBuilder (NO offline badge!) + Right Profile Avatar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("home_hamburger_button"),
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
                    }
                    FlowBuilderLogoMark(size = 38.dp)
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    IconButton(
                        onClick = onOpenAiChat,
                        modifier = Modifier.testTag("home_ai_chat_button"),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SmartToy,
                                contentDescription = "Open AI Chat Assistant",
                                tint = FlowTeal600,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("open_profile_avatar_button"),
                    ) {
                        UserAvatarCircle(profile = uiState.userProfile, size = 38.dp)
                    }
                }
            }
        }

        // Search Bar
        item {
            FlowSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchChange,
            )
        }

        // Hero Create Card (hidden when actively searching)
        if (uiState.searchQuery.isBlank()) {
            item {
                HeroCreateCard(
                    projectCount = uiState.projects.size,
                    nodeCount = uiState.totalNodesAcrossProjects,
                    onCreateClick = onCreateClick,
                    onImportJsonClick = onImportJsonClick,
                )
            }
        }

        // Recent Projects Section
        item {
            SectionHeader(
                title = if (uiState.searchQuery.isBlank()) {
                    stringResource(R.string.recent_projects)
                } else {
                    "Matching Projects (${uiState.filteredProjects.size})"
                },
                actionLabel = if (uiState.projects.isNotEmpty()) "See All" else null,
                onAction = onSeeAllProjects,
            )
        }

        val displayedProjects = if (uiState.searchQuery.isBlank()) {
            uiState.recentProjects
        } else {
            uiState.filteredProjects
        }

        if (displayedProjects.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (uiState.searchQuery.isBlank()) {
                        stringResource(R.string.empty_projects_title)
                    } else {
                        stringResource(R.string.empty_search_title)
                    },
                    subtitle = if (uiState.searchQuery.isBlank()) {
                        stringResource(R.string.empty_projects_subtitle)
                    } else {
                        stringResource(R.string.empty_search_subtitle)
                    },
                    actionLabel = stringResource(R.string.create_new_flowchart),
                    onAction = onCreateClick,
                )
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(displayedProjects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            modifier = Modifier.width(248.dp),
                            onOpen = { onOpenProject(project.id) },
                            onRename = { onRename(project) },
                            onDuplicate = { onDuplicate(project) },
                            onToggleFavorite = { onToggleFavorite(project) },
                            onDelete = { onDelete(project) },
                            onExportJson = { onExportJson(project) },
                        )
                    }
                }
            }
        }

        // Templates Library Strip
        item {
            SectionHeader(
                title = stringResource(R.string.templates_library),
                actionLabel = "All ${FlowTemplates.allTemplates.size}",
                onAction = onSeeAllTemplates,
            )
        }

        if (uiState.filteredTemplates.isEmpty()) {
            item {
                Text(
                    "No templates match \"${uiState.searchQuery}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(uiState.filteredTemplates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            modifier = Modifier.width(236.dp),
                            onUse = { onUseTemplate(template) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCreateCard(
    projectCount: Int,
    nodeCount: Int,
    onCreateClick: () -> Unit,
    onImportJsonClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SlateInk900,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF064E3B),
                            Color(0xFF0D6960),
                            Color(0xFF0D9488),
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text(
                        text = "Design Clear Algorithms",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "24+ flowchart symbols, AI flow builder, live Pseudocode, and HTML/PDF/PNG export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD1FAE5),
                    )
                }

                // Compact, single-line non-breaking buttons (never wraps text!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onCreateClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF86EFAC),
                            contentColor = Color(0xFF062C1B),
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_create_flowchart_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "New Flow",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    OutlinedButton(
                        onClick = onImportJsonClick,
                        border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_import_json_button"),
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Import JSON",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill("$projectCount Flows")
                    StatPill("$nodeCount Shapes")
                    StatPill("${FlowTemplates.allTemplates.size} Templates")
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0x24FFFFFF),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE2E8F0),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun MyFlowchartsContent(
    uiState: HomeUiState,
    isWide: Boolean,
    modifier: Modifier,
    onSearchChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    onImportJsonClick: () -> Unit,
    onOpenProject: (String) -> Unit,
    onRename: (FlowProject) -> Unit,
    onDuplicate: (FlowProject) -> Unit,
    onToggleFavorite: (FlowProject) -> Unit,
    onDelete: (FlowProject) -> Unit,
    onExportJson: (FlowProject) -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.my_flowcharts),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${uiState.filteredProjects.size} synced flowcharts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onImportJsonClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Import", maxLines = 1, softWrap = false, fontSize = 12.5.sp)
            }
        }

        FlowSearchBar(query = uiState.searchQuery, onQueryChange = onSearchChange)

        if (uiState.filteredProjects.isEmpty()) {
            EmptyStateCard(
                title = if (uiState.searchQuery.isBlank()) {
                    stringResource(R.string.empty_projects_title)
                } else {
                    stringResource(R.string.empty_search_title)
                },
                subtitle = if (uiState.searchQuery.isBlank()) {
                    stringResource(R.string.empty_projects_subtitle)
                } else {
                    stringResource(R.string.empty_search_subtitle)
                },
                actionLabel = stringResource(R.string.create_new_flowchart),
                onAction = onCreateClick,
            )
        } else {
            LazyVerticalGrid(
                columns = if (isWide) GridCells.Adaptive(250.dp) else GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(uiState.filteredProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        modifier = Modifier.fillMaxWidth(),
                        onOpen = { onOpenProject(project.id) },
                        onRename = { onRename(project) },
                        onDuplicate = { onDuplicate(project) },
                        onToggleFavorite = { onToggleFavorite(project) },
                        onDelete = { onDelete(project) },
                        onExportJson = { onExportJson(project) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplatesLibraryContent(
    uiState: HomeUiState,
    isWide: Boolean,
    modifier: Modifier,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onUseTemplate: (FlowTemplate) -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.templates_library),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Tap any blueprint to launch an editable copy in the studio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FlowSearchBar(query = uiState.searchQuery, onQueryChange = onSearchChange)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowTemplates.categories.forEach { cat ->
                FilterChip(
                    selected = uiState.selectedTemplateCategory == cat,
                    onClick = { onSelectCategory(cat) },
                    label = { Text(cat) },
                    modifier = Modifier.testTag("template_cat_${cat.lowercase()}"),
                )
            }
        }

        if (uiState.filteredTemplates.isEmpty()) {
            EmptyStateCard(
                title = "No matching templates",
                subtitle = "Try clearing your search or choosing the All category.",
                actionLabel = "Show All Templates",
                onAction = {
                    onSearchChange("")
                    onSelectCategory("All")
                },
            )
        } else {
            LazyVerticalGrid(
                columns = if (isWide) GridCells.Adaptive(260.dp) else GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
            ) {
                items(uiState.filteredTemplates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        modifier = Modifier.fillMaxWidth(),
                        onUse = { onUseTemplate(template) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_search_input"),
    )
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: FlowProject,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onExportJson: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .testTag("project_card_${project.id}"),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                FlowPreviewCanvas(
                    graph = project.graph,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    ) {
                        Icon(
                            imageVector = if (project.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (project.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(6.dp).size(18.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    val relTime = DateUtils.getRelativeTimeSpanString(
                        project.updatedAt,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                    )
                    Text(
                        text = "${project.graph.nodes.size} shapes • ${project.graph.edges.size} arrows • $relTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Project options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export JSON") },
                            leadingIcon = { Icon(Icons.Filled.Download, null) },
                            onClick = {
                                menuExpanded = false
                                onExportJson()
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: FlowTemplate,
    modifier: Modifier = Modifier,
    onUse: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onUse)
            .testTag("template_card_${template.id}"),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
            ) {
                FlowPreviewCanvas(
                    graph = template.graph,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                ) {
                    Text(
                        text = template.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = template.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Use template",
                        style = MaterialTheme.typography.labelLarge,
                        color = FlowTeal600,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = FlowTeal600,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.GridOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
    }
}
