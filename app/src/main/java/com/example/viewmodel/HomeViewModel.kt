package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.repository.FlowAiRepository
import com.example.data.repository.FlowProjectRepository
import com.example.domain.model.AiConfigurationState
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AppFontFamily
import com.example.domain.model.FlowProject
import com.example.domain.model.FlowTemplate
import com.example.domain.model.GridStyle
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserProfileData
import com.example.domain.templates.FlowTemplates
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val projects: List<FlowProject> = emptyList(),
    val recentProjects: List<FlowProject> = emptyList(),
    val filteredProjects: List<FlowProject> = emptyList(),
    val filteredTemplates: List<FlowTemplate> = FlowTemplates.allTemplates,
    val searchQuery: String = "",
    val selectedTemplateCategory: String = "All",
    val settings: UserSettings = UserSettings(),
    val userProfile: UserProfileData = UserProfileData(),
    val totalNodesAcrossProjects: Int = 0,
    val isLoading: Boolean = true,
    val isGeneratingAi: Boolean = false,
    val aiErrorMessage: String? = null,
    val aiConfig: AiConfigurationState = AiConfigurationState(),
    val isConfiguringAi: Boolean = false,
    val syncErrorMessage: String? = null,
)

sealed interface HomeEvent {
    data class OpenProject(val projectId: String) : HomeEvent
    data class ShowMessage(val message: String) : HomeEvent
}

class HomeViewModel(
    private val userId: String,
    private val initialDisplayName: String,
    private val initialEmail: String,
    private val initialPhotoUrl: String?,
    private val projectRepository: FlowProjectRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val aiRepository: FlowAiRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("All")
    private val isInitialized = MutableStateFlow(false)
    private val isGeneratingAi = MutableStateFlow(false)
    private val aiError = MutableStateFlow<String?>(null)
    private val isConfiguringAi = MutableStateFlow(false)
    private val syncError = MutableStateFlow<String?>(null)
    private val remoteProjects = MutableStateFlow<List<FlowProject>?>(null)
    private val profileFlow = MutableStateFlow(
        UserProfileData(
            userId = userId,
            displayName = initialDisplayName.ifBlank { "FlowBuilder User" },
            subtitle = "Flowchart Creator",
            email = initialEmail,
            photoUrl = initialPhotoUrl,
        )
    )

    private val _events = MutableSharedFlow<HomeEvent>()
    val events = _events.asSharedFlow()

    private val projectsStream = combine(
        remoteProjects,
        projectRepository.localProjects,
    ) { remote, local ->
        remote ?: local
    }

    private val filterAndStatusFlow = combine(
        searchQuery,
        selectedCategory,
        isInitialized,
        isGeneratingAi,
        aiError,
    ) { query, category, initialized, generating, err ->
        FilterAndStatus(query, category, initialized, generating, err)
    }

    private val aiAndSyncFlow = combine(
        aiRepository.observeConfiguration(userId),
        isConfiguringAi,
        syncError,
    ) { config, configuring, cloudErr ->
        AiAndSyncStatus(config, configuring, cloudErr)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        projectsStream,
        preferencesRepository.settingsFlow,
        profileFlow,
        filterAndStatusFlow,
        aiAndSyncFlow,
    ) { projects, settings, profile, status, aiAndSync ->
        val trimmed = status.query.trim()
        val matchingProjects = if (trimmed.isBlank()) {
            projects
        } else {
            projects.filter { p ->
                p.title.contains(trimmed, ignoreCase = true) ||
                    p.description.contains(trimmed, ignoreCase = true) ||
                    p.category.contains(trimmed, ignoreCase = true)
            }
        }
        val matchingTemplates = FlowTemplates.allTemplates.filter { tpl ->
            val catMatch = status.category == "All" || tpl.category.equals(status.category, ignoreCase = true)
            val queryMatch = trimmed.isBlank() ||
                tpl.title.contains(trimmed, ignoreCase = true) ||
                tpl.description.contains(trimmed, ignoreCase = true) ||
                tpl.category.contains(trimmed, ignoreCase = true)
            catMatch && queryMatch
        }

        HomeUiState(
            projects = projects,
            recentProjects = projects.take(6),
            filteredProjects = matchingProjects,
            filteredTemplates = matchingTemplates,
            searchQuery = status.query,
            selectedTemplateCategory = status.category,
            settings = settings,
            userProfile = profile,
            totalNodesAcrossProjects = projects.sumOf { it.graph.nodes.size },
            isLoading = !status.initialized,
            isGeneratingAi = status.isGeneratingAi,
            aiErrorMessage = status.aiError,
            aiConfig = aiAndSync.aiConfig,
            isConfiguringAi = aiAndSync.isConfiguringAi,
            syncErrorMessage = aiAndSync.syncError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            userProfile = profileFlow.value,
        ),
    )

    init {
        viewModelScope.launch {
            // Migrate any previously saved plaintext key into encrypted per-user storage
            val legacyKey = preferencesRepository.settingsFlow.first().openAiApiKey
            if (legacyKey.isNotBlank()) {
                aiRepository.migrateLegacyKeyIfNeeded(userId, legacyKey)
                preferencesRepository.setOpenAiApiKey("")
            }

            projectRepository.ensureProfileInitialized(
                userId = userId,
                displayName = initialDisplayName,
                subtitle = "Flowchart Creator",
                email = initialEmail,
                photoUrl = initialPhotoUrl,
            )
            projectRepository.ensureStarterProjectsSeeded(userId)
            isInitialized.value = true
        }

        // Observe Firestore realtime projects with two-tier cancellation unwrapping
        viewModelScope.launch {
            projectRepository.observeProjects(userId)
                .catch { e ->
                    val root = if (e is CancellationException) e.cause else e
                    if (root is FirebaseFirestoreException) {
                        syncError.value = "Cloud sync issue: ${root.message ?: root.code.name}"
                    } else if (e !is CancellationException) {
                        syncError.value = e.message
                    }
                }
                .collect { list ->
                    syncError.value = null
                    remoteProjects.value = list
                }
        }

        // Observe Firestore realtime profile
        viewModelScope.launch {
            projectRepository.observeProfile(userId)
                .catch { e ->
                    val root = if (e is CancellationException) e.cause else e
                    if (root is FirebaseFirestoreException) {
                        syncError.value = root.message
                    }
                }
                .collect { fetched ->
                    if (fetched != null) {
                        profileFlow.value = fetched.copy(
                            email = fetched.email.ifBlank { initialEmail },
                            photoUrl = fetched.photoUrl ?: initialPhotoUrl,
                        )
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectTemplateCategory(category: String) {
        selectedCategory.value = category
    }

    fun clearAiError() {
        aiError.value = null
    }

    fun createNewProject(title: String = "") {
        viewModelScope.launch {
            val settings = uiState.value.settings
            val defaultName = title.trim().ifBlank {
                "Flowchart ${uiState.value.projects.size + 1}"
            }
            val created = projectRepository.createBlankProject(
                userId = userId,
                title = defaultName,
                gridStyle = settings.defaultGridStyle,
                snapToGrid = settings.defaultSnapToGrid,
            )
            _events.emit(HomeEvent.OpenProject(created.id))
        }
    }

    fun createProjectWithAi(title: String, prompt: String, onSuccessClose: () -> Unit = {}) {
        if (isGeneratingAi.value) return
        viewModelScope.launch {
            isGeneratingAi.value = true
            aiError.value = null
            val settings = uiState.value.settings
            val projectTitle = title.trim().ifBlank {
                prompt.trim().split(Regex("\\s+")).take(4).joinToString(" ").ifBlank { "AI Flowchart" }
            }
            val result = aiRepository.generateFlowchart(
                userId = userId,
                projectTitle = projectTitle,
                prompt = prompt,
                gridStyle = settings.defaultGridStyle,
                snapToGrid = settings.defaultSnapToGrid,
                defaultFont = settings.appFontFamily,
            )
            result.onSuccess { graph ->
                val created = projectRepository.createProjectWithGraph(
                    userId = userId,
                    title = projectTitle,
                    description = "Generated by AI: ${prompt.trim()}",
                    category = "AI Generated",
                    graph = graph,
                )
                isGeneratingAi.value = false
                onSuccessClose()
                _events.emit(HomeEvent.OpenProject(created.id))
            }.onFailure { err ->
                isGeneratingAi.value = false
                aiError.value = err.message ?: "Failed to generate flowchart."
            }
        }
    }

    fun createFromTemplate(template: FlowTemplate) {
        viewModelScope.launch {
            val settings = uiState.value.settings
            val created = projectRepository.createFromTemplate(
                userId = userId,
                template = template,
                gridStyle = settings.defaultGridStyle,
                snapToGrid = settings.defaultSnapToGrid,
            )
            _events.emit(HomeEvent.OpenProject(created.id))
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        viewModelScope.launch {
            projectRepository.renameProject(userId, projectId, newTitle)
            _events.emit(HomeEvent.ShowMessage("Renamed project"))
        }
    }

    fun duplicateProject(project: FlowProject) {
        viewModelScope.launch {
            projectRepository.duplicateProject(userId, project)
            _events.emit(HomeEvent.ShowMessage("Duplicated \"${project.title}\""))
        }
    }

    fun toggleFavorite(project: FlowProject) {
        viewModelScope.launch {
            projectRepository.toggleFavorite(userId, project.id, !project.isFavorite)
        }
    }

    fun deleteProject(project: FlowProject) {
        viewModelScope.launch {
            projectRepository.deleteProject(userId, project.id)
            _events.emit(HomeEvent.ShowMessage("Deleted \"${project.title}\""))
        }
    }

    fun importJson(json: String) {
        viewModelScope.launch {
            val result = projectRepository.importFromJson(userId, json)
            result.onSuccess { project ->
                _events.emit(HomeEvent.ShowMessage("Imported \"${project.title}\""))
                _events.emit(HomeEvent.OpenProject(project.id))
            }.onFailure {
                _events.emit(HomeEvent.ShowMessage("Invalid FlowBuilder JSON file"))
            }
        }
    }

    fun updateUserProfile(displayName: String, subtitle: String) {
        viewModelScope.launch {
            val result = projectRepository.updateProfile(userId, displayName, subtitle)
            result.onSuccess {
                profileFlow.value = profileFlow.value.copy(
                    displayName = displayName.trim().ifBlank { profileFlow.value.displayName },
                    subtitle = subtitle.trim(),
                )
                _events.emit(HomeEvent.ShowMessage("Profile updated"))
            }.onFailure {
                _events.emit(HomeEvent.ShowMessage("Could not update profile right now"))
            }
        }
    }

    fun exportProjectJson(project: FlowProject): String = projectRepository.exportToJson(project)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAppFontFamily(fontFamily: AppFontFamily) {
        viewModelScope.launch { preferencesRepository.setAppFontFamily(fontFamily) }
    }

    fun setDefaultGridStyle(style: GridStyle) {
        viewModelScope.launch { preferencesRepository.setDefaultGridStyle(style) }
    }

    fun setDefaultSnapToGrid(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDefaultSnapToGrid(enabled) }
    }

    fun setShowAlignmentGuides(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowAlignmentGuides(enabled) }
    }

    fun setShowLiveValidationBadges(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowLiveValidationBadges(enabled) }
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoSaveEnabled(enabled)
            _events.emit(
                HomeEvent.ShowMessage(
                    if (enabled) "Realtime Auto-Save enabled" else "Auto-Save paused — manual save active"
                )
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun saveAiApiKey(rawKey: String) {
        if (isConfiguringAi.value) return
        viewModelScope.launch {
            isConfiguringAi.value = true
            try {
                preferencesRepository.setOpenAiApiKey("")
                val state = aiRepository.saveAndConfigureApiKey(userId, rawKey)
                val feedback = when (state.status) {
                    AiConnectionStatus.CONNECTED ->
                        "${state.provider?.displayName ?: "AI"} connected and ready"
                    AiConnectionStatus.INVALID_API_KEY ->
                        "Invalid API Key — check your key and try again"
                    AiConnectionStatus.CONNECTION_FAILED ->
                        "Connection Failed — check your network and try again"
                    AiConnectionStatus.NOT_CONFIGURED ->
                        "API key removed — built-in AI ready"
                }
                _events.emit(HomeEvent.ShowMessage(feedback))
            } finally {
                isConfiguringAi.value = false
            }
        }
    }

    fun testAiConnection(draftKey: String) {
        if (isConfiguringAi.value) return
        viewModelScope.launch {
            isConfiguringAi.value = true
            try {
                if (draftKey.isNotBlank()) {
                    preferencesRepository.setOpenAiApiKey("")
                }
                val state = aiRepository.testConnection(userId, draftKey)
                val feedback = when (state.status) {
                    AiConnectionStatus.CONNECTED ->
                        "Connected to ${state.provider?.displayName ?: "AI provider"}"
                    AiConnectionStatus.INVALID_API_KEY ->
                        "Invalid API Key"
                    AiConnectionStatus.CONNECTION_FAILED ->
                        "Connection Failed"
                    AiConnectionStatus.NOT_CONFIGURED ->
                        "Not Configured — paste an API key first"
                }
                _events.emit(HomeEvent.ShowMessage(feedback))
            } finally {
                isConfiguringAi.value = false
            }
        }
    }

    fun removeAiApiKey() {
        if (isConfiguringAi.value) return
        viewModelScope.launch {
            isConfiguringAi.value = true
            try {
                preferencesRepository.setOpenAiApiKey("")
                aiRepository.removeApiKey(userId)
                _events.emit(HomeEvent.ShowMessage("API key removed"))
            } finally {
                isConfiguringAi.value = false
            }
        }
    }

    private data class AiAndSyncStatus(
        val aiConfig: AiConfigurationState,
        val isConfiguringAi: Boolean,
        val syncError: String?,
    )

    private data class FilterAndStatus(
        val query: String,
        val category: String,
        val initialized: Boolean,
        val isGeneratingAi: Boolean,
        val aiError: String?,
    )

    class Factory(
        private val userId: String,
        private val initialDisplayName: String,
        private val initialEmail: String,
        private val initialPhotoUrl: String?,
        private val projectRepository: FlowProjectRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val aiRepository: FlowAiRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                userId = userId,
                initialDisplayName = initialDisplayName,
                initialEmail = initialEmail,
                initialPhotoUrl = initialPhotoUrl,
                projectRepository = projectRepository,
                preferencesRepository = preferencesRepository,
                aiRepository = aiRepository,
            ) as T
        }
    }
}
