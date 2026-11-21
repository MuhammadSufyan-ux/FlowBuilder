package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.export.FlowExportManager
import com.example.data.local.FlowBuilderDatabase
import com.example.data.local.FlowGraphSerializer
import com.example.data.local.SecureApiKeyStore
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.remote.FirestoreFlowDataSource
import com.example.data.remote.configureAppCheck
import com.example.data.repository.FlowAiRepository
import com.example.data.repository.FlowProjectRepository
import com.example.domain.model.FlowProject
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.auth.OnboardingScreen
import com.example.ui.auth.SplashScreen
import com.example.ui.chat.AiChatScreen
import com.example.ui.editor.EditorScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeTab
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.SettingsScreen
import com.example.ui.theme.FlowBuilderTheme
import com.example.viewmodel.AiChatViewModel
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.HomeEvent
import com.example.viewmodel.HomeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private enum class AuthenticatedRoute {
    HOME,
    AI_CHAT,
    PROFILE,
    SETTINGS,
    EDITOR,
}

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var projectRepository: FlowProjectRepository
    private lateinit var aiRepository: FlowAiRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val db = FlowBuilderDatabase.getInstance(applicationContext)
        val serializer = FlowGraphSerializer()
        preferencesRepository = UserPreferencesRepository(applicationContext)
        val secureApiKeyStore = SecureApiKeyStore(applicationContext)
        aiRepository = FlowAiRepository(keyStore = secureApiKeyStore)

        // Resolve the provisioned named Firestore database via resource ID
        val firestore = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
        val remoteDataSource = FirestoreFlowDataSource(firestore)

        projectRepository = FlowProjectRepository(
            dao = db.flowProjectDao(),
            serializer = serializer,
            preferences = preferencesRepository,
            remote = remoteDataSource,
        )

        setContent {
            FlowBuilderRootApp(
                projectRepository = projectRepository,
                preferencesRepository = preferencesRepository,
                aiRepository = aiRepository,
            )
        }
    }
}

@Composable
private fun FlowBuilderRootApp(
    projectRepository: FlowProjectRepository,
    preferencesRepository: UserPreferencesRepository,
    aiRepository: FlowAiRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by preferencesRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = UserSettings())

    val currentUser by produceState<FirebaseUser?>(initialValue = Firebase.auth.currentUser) {
        val flow = callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                trySend(auth.currentUser)
            }
            Firebase.auth.addAuthStateListener(listener)
            awaitClose { Firebase.auth.removeAuthStateListener(listener) }
        }
        flow.collect { value = it }
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var authStartOnSignUp by rememberSaveable { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var pendingProfileName by remember { mutableStateOf("") }
    var pendingProfileSubtitle by remember { mutableStateOf("") }
    var attemptedSilentSignIn by rememberSaveable { mutableStateOf(false) }

    // Attempt silent Google auto-sign-in once if returning user has finished onboarding
    LaunchedEffect(showSplash, currentUser, settings.hasCompletedOnboarding) {
        if (!showSplash && currentUser == null && settings.hasCompletedOnboarding && !attemptedSilentSignIn) {
            attemptedSilentSignIn = true
            GoogleAuthHelper.attemptAutoSignIn(context)
        }
    }

    fun triggerGoogleSignIn(customName: String = "", customSubtitle: String = "") {
        if (isSigningIn) return
        scope.launch {
            isSigningIn = true
            authError = null
            pendingProfileName = customName
            pendingProfileSubtitle = customSubtitle
            val result = GoogleAuthHelper.signInWithGoogle(context)
            result.onSuccess {
                preferencesRepository.setOnboardingCompleted(true)
            }.onFailure { err ->
                if (err !is GetCredentialCancellationException) {
                    authError = err.message ?: "Unable to sign in with Google."
                }
            }
            isSigningIn = false
        }
    }

    FlowBuilderTheme(
        themeMode = settings.themeMode,
        fontFamily = settings.appFontFamily,
    ) {
        when {
            showSplash -> {
                SplashScreen(onFinished = { showSplash = false })
            }

            currentUser == null && !settings.hasCompletedOnboarding -> {
                OnboardingScreen(
                    onCompleteToAuth = { startSignUp ->
                        authStartOnSignUp = startSignUp
                        scope.launch { preferencesRepository.setOnboardingCompleted(true) }
                    },
                    onContinueWithGoogle = { triggerGoogleSignIn() },
                    isSigningIn = isSigningIn,
                    authError = authError,
                )
            }

            currentUser == null -> {
                AuthScreen(
                    initialSignUpMode = authStartOnSignUp,
                    isSigningIn = isSigningIn,
                    errorMessage = authError,
                    onGoogleAuthClick = { name, subtitle ->
                        triggerGoogleSignIn(name, subtitle)
                    },
                )
            }

            else -> {
                val user = currentUser!!
                key(user.uid) {
                    AuthenticatedFlowApp(
                        user = user,
                        pendingProfileName = pendingProfileName,
                        pendingProfileSubtitle = pendingProfileSubtitle,
                        projectRepository = projectRepository,
                        preferencesRepository = preferencesRepository,
                        aiRepository = aiRepository,
                        onSignOut = {
                            scope.launch {
                                GoogleAuthHelper.signOutUser(context)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedFlowApp(
    user: FirebaseUser,
    pendingProfileName: String,
    pendingProfileSubtitle: String,
    projectRepository: FlowProjectRepository,
    preferencesRepository: UserPreferencesRepository,
    aiRepository: FlowAiRepository,
    onSignOut: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(
        key = "home_${user.uid}",
        factory = HomeViewModel.Factory(
            userId = user.uid,
            initialDisplayName = pendingProfileName.ifBlank { user.displayName ?: "FlowBuilder User" },
            initialEmail = user.email ?: "",
            initialPhotoUrl = user.photoUrl?.toString(),
            projectRepository = projectRepository,
            preferencesRepository = preferencesRepository,
            aiRepository = aiRepository,
        ),
    )

    // Apply custom signup name/tagline if user entered them during Create Account
    LaunchedEffect(user.uid, pendingProfileName, pendingProfileSubtitle) {
        if (pendingProfileName.isNotBlank() || pendingProfileSubtitle.isNotBlank()) {
            homeViewModel.updateUserProfile(
                displayName = pendingProfileName.ifBlank { user.displayName ?: "FlowBuilder User" },
                subtitle = pendingProfileSubtitle.ifBlank { "Flowchart Creator" },
            )
        }
    }

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }
    var activeRoute by rememberSaveable { mutableStateOf(AuthenticatedRoute.HOME) }
    var activeEditorProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingJsonExportProject by remember { mutableStateOf<FlowProject?>(null) }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                FlowExportManager.readTextFromUri(context, uri)
                    .onSuccess { json -> homeViewModel.importJson(json) }
                    .onFailure { snackbarHostState.showSnackbar("Could not read selected file") }
            }
        }
    }

    val exportHomeJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val project = pendingJsonExportProject
        if (uri != null && project != null) {
            scope.launch {
                val bytes = homeViewModel.exportProjectJson(project).toByteArray(Charsets.UTF_8)
                FlowExportManager.writeBytesToUri(context, uri, bytes)
                    .onSuccess { snackbarHostState.showSnackbar("Exported \"${project.title}.json\"") }
                    .onFailure { snackbarHostState.showSnackbar("Could not export JSON file") }
            }
        }
        pendingJsonExportProject = null
    }

    LaunchedEffect(Unit) {
        homeViewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenProject -> {
                    activeEditorProjectId = event.projectId
                    activeRoute = AuthenticatedRoute.EDITOR
                }
                is HomeEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Crossfade(
        targetState = activeRoute,
        animationSpec = tween(durationMillis = 220),
        label = "authenticated_route_transition",
    ) { route ->
        when (route) {
            AuthenticatedRoute.HOME -> {
                HomeScreen(
                    uiState = homeState,
                    activeTab = activeTab,
                    onSelectTab = { activeTab = it },
                    snackbarHostState = snackbarHostState,
                    onSearchChange = homeViewModel::updateSearchQuery,
                    onSelectTemplateCategory = homeViewModel::selectTemplateCategory,
                    onCreateNewProject = homeViewModel::createNewProject,
                    onCreateProjectWithAi = homeViewModel::createProjectWithAi,
                    onClearAiError = homeViewModel::clearAiError,
                    onOpenProject = { id ->
                        activeEditorProjectId = id
                        activeRoute = AuthenticatedRoute.EDITOR
                    },
                    onUseTemplate = homeViewModel::createFromTemplate,
                    onRenameProject = homeViewModel::renameProject,
                    onDuplicateProject = homeViewModel::duplicateProject,
                    onToggleFavorite = homeViewModel::toggleFavorite,
                    onDeleteProject = homeViewModel::deleteProject,
                    onExportProjectJson = { project ->
                        pendingJsonExportProject = project
                        val fileName = project.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                            .ifBlank { "flowchart" }
                        exportHomeJsonLauncher.launch("$fileName.json")
                    },
                    onImportJsonClick = {
                        importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    onOpenProfile = { activeRoute = AuthenticatedRoute.PROFILE },
                    onOpenAiChat = { activeRoute = AuthenticatedRoute.AI_CHAT },
                    onUpdateProfile = homeViewModel::updateUserProfile,
                    onThemeModeChange = homeViewModel::setThemeMode,
                    onFontFamilyChange = homeViewModel::setAppFontFamily,
                    onAutoSaveChange = homeViewModel::setAutoSaveEnabled,
                    onNotificationsChange = homeViewModel::setNotificationsEnabled,
                    onSaveAiApiKey = homeViewModel::saveAiApiKey,
                    onTestAiConnection = homeViewModel::testAiConnection,
                    onRemoveAiApiKey = homeViewModel::removeAiApiKey,
                    onGridStyleChange = homeViewModel::setDefaultGridStyle,
                    onSnapToGridChange = homeViewModel::setDefaultSnapToGrid,
                    onAlignmentGuidesChange = homeViewModel::setShowAlignmentGuides,
                    onValidationBadgesChange = homeViewModel::setShowLiveValidationBadges,
                    onSignOut = onSignOut,
                )
            }

            AuthenticatedRoute.AI_CHAT -> {
                BackHandler { activeRoute = AuthenticatedRoute.HOME }
                val chatViewModel: AiChatViewModel = viewModel(
                    key = "ai_chat_${user.uid}",
                    factory = AiChatViewModel.Factory(
                        userId = user.uid,
                        aiRepository = aiRepository,
                    ),
                )
                val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
                AiChatScreen(
                    uiState = chatState,
                    onBack = { activeRoute = AuthenticatedRoute.HOME },
                    onSendMessage = chatViewModel::sendMessage,
                    onClearChat = chatViewModel::clearChat,
                    onClearError = chatViewModel::clearError,
                )
            }

            AuthenticatedRoute.PROFILE -> {
                BackHandler { activeRoute = AuthenticatedRoute.HOME }
                ProfileScreen(
                    uiState = homeState,
                    onBack = { activeRoute = AuthenticatedRoute.HOME },
                    onOpenMyFlows = {
                        activeTab = HomeTab.PROJECTS
                        activeRoute = AuthenticatedRoute.HOME
                    },
                    onOpenSettings = { activeRoute = AuthenticatedRoute.SETTINGS },
                    onNotificationsChange = homeViewModel::setNotificationsEnabled,
                    onThemeModeChange = homeViewModel::setThemeMode,
                    onFontFamilyChange = homeViewModel::setAppFontFamily,
                    onSignOut = onSignOut,
                )
            }

            AuthenticatedRoute.SETTINGS -> {
                BackHandler { activeRoute = AuthenticatedRoute.PROFILE }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        SettingsScreen(
                            uiState = homeState,
                            showBackArrow = true,
                            onBack = { activeRoute = AuthenticatedRoute.PROFILE },
                            onUpdateProfile = homeViewModel::updateUserProfile,
                            onThemeModeChange = homeViewModel::setThemeMode,
                            onFontFamilyChange = homeViewModel::setAppFontFamily,
                            onAutoSaveChange = homeViewModel::setAutoSaveEnabled,
                            onNotificationsChange = homeViewModel::setNotificationsEnabled,
                            onSaveAiApiKey = homeViewModel::saveAiApiKey,
                            onTestAiConnection = homeViewModel::testAiConnection,
                            onRemoveAiApiKey = homeViewModel::removeAiApiKey,
                            onGridStyleChange = homeViewModel::setDefaultGridStyle,
                            onSnapToGridChange = homeViewModel::setDefaultSnapToGrid,
                            onAlignmentGuidesChange = homeViewModel::setShowAlignmentGuides,
                            onValidationBadgesChange = homeViewModel::setShowLiveValidationBadges,
                            onImportJsonClick = {
                                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                        )
                    }
                }
            }

            AuthenticatedRoute.EDITOR -> {
                val projectId = activeEditorProjectId
                if (projectId == null) {
                    activeRoute = AuthenticatedRoute.HOME
                } else {
                    val editorViewModel: EditorViewModel = viewModel(
                        key = "editor_${user.uid}_$projectId",
                        factory = EditorViewModel.Factory(
                            userId = user.uid,
                            projectId = projectId,
                            repository = projectRepository,
                            aiRepository = aiRepository,
                            initialShowGuides = homeState.settings.showAlignmentGuides,
                            initialShowValidation = homeState.settings.showLiveValidationBadges,
                            initialAutoSaveEnabled = homeState.settings.autoSaveEnabled,
                            defaultFontFamily = homeState.settings.appFontFamily,
                        ),
                    )
                    EditorScreen(
                        viewModel = editorViewModel,
                        autoSaveEnabled = homeState.settings.autoSaveEnabled,
                        onNavigateBack = {
                            activeEditorProjectId = null
                            activeRoute = AuthenticatedRoute.HOME
                        },
                    )
                }
            }
        }
    }
}
