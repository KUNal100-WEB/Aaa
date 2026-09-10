package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.content.Context
import android.content.Intent
import com.example.data.model.*
import com.example.data.repository.ProjectRepository
import com.example.data.service.IBuildService
import com.example.data.service.MockBuildService
import com.example.data.storage.ApkStorageManager
import com.example.data.storage.StorageResult
import com.example.domain.ai.AICodingAgent
import com.example.ui.navigation.IdeScreenTab
import com.example.ui.navigation.IdeSubTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class IdeUiState(
    val currentTab: IdeScreenTab = IdeScreenTab.PROJECTS,
    val ideSubTab: IdeSubTab = IdeSubTab.FILES,
    val projects: List<Project> = emptyList(),
    val searchQuery: String = "",
    val activeProject: Project? = null,
    val projectFiles: List<ProjectFile> = emptyList(),
    val activeFile: ProjectFile? = null,
    val editorContent: String = "",
    val hasUnsavedChanges: Boolean = false,
    val aiRequests: List<AIRequest> = emptyList(),
    val isAiProcessing: Boolean = false,
    val latestBuild: BuildRecord? = null,
    val isBuilding: Boolean = false,
    val builtApps: List<BuiltApp> = emptyList(),
    val builtAppsFilter: String = "ALL", // "ALL", "DEBUG", "RELEASE"
    val runningPreviewApp: BuiltApp? = null,
    val isGeneratingNewProject: Boolean = false,
    val generationStageIndex: Int = 0,
    val settings: AppSettings = AppSettings(),
    val aiConnectionTestStatus: String? = null,
    val isTestingAiConnection: Boolean = false,
    val errorMessage: String? = null,
    val userNotification: String? = null,
    val lastDownloadedApkUri: Uri? = null,
    val lastDownloadedApkPath: String? = null,
    val isDownloadingApk: Boolean = false
)

class MainIdeViewModel(
    private val repository: ProjectRepository = ProjectRepository.instance,
    private val settingsRepository: com.example.data.repository.SettingsRepository = com.example.data.repository.SettingsRepository.instance,
    private val buildService: IBuildService = MockBuildService(),
    private val aiAgent: AICodingAgent = AICodingAgent(repository, settingsRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.projects.collect { list ->
                val active = _uiState.value.activeProject ?: list.firstOrNull()
                _uiState.update { it.copy(projects = list, activeProject = active) }
                if (active != null) {
                    loadProjectFiles(active.id)
                }
            }
        }
        viewModelScope.launch {
            repository.builtApps.collect { list ->
                _uiState.update { it.copy(builtApps = list) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _uiState.update { it.copy(settings = s) }
            }
        }
    }

    fun selectTab(tab: IdeScreenTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectIdeSubTab(subTab: IdeSubTab) {
        _uiState.update { it.copy(ideSubTab = subTab) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectProject(project: Project, switchTab: Boolean = true) {
        _uiState.update {
            it.copy(
                activeProject = project,
                currentTab = if (switchTab) IdeScreenTab.CURRENT_IDE else it.currentTab
            )
        }
        loadProjectFiles(project.id)
    }

    private fun loadProjectFiles(projectId: String) {
        val files = repository.getFiles(projectId)
        val defaultFile = files.find { it.path.contains("MainActivity.kt") } ?: files.firstOrNull()
        _uiState.update {
            it.copy(
                projectFiles = files,
                activeFile = defaultFile,
                editorContent = defaultFile?.content ?: "",
                hasUnsavedChanges = false
            )
        }
    }

    fun selectFile(file: ProjectFile) {
        _uiState.update {
            it.copy(
                activeFile = file,
                editorContent = file.content,
                hasUnsavedChanges = false,
                ideSubTab = IdeSubTab.CODE
            )
        }
    }

    fun updateEditorContent(newContent: String) {
        val changed = newContent != (_uiState.value.activeFile?.content ?: "")
        _uiState.update {
            it.copy(editorContent = newContent, hasUnsavedChanges = changed)
        }
    }

    fun saveActiveFile() {
        val active = _uiState.value.activeProject ?: return
        val file = _uiState.value.activeFile ?: return
        val content = _uiState.value.editorContent

        val result = repository.saveFile(active.id, file.path, content)
        if (result.isSuccess) {
            loadProjectFiles(active.id)
            _uiState.update {
                it.copy(
                    hasUnsavedChanges = false,
                    userNotification = "Saved ${file.path.substringAfterLast('/')}"
                )
            }
        } else {
            _uiState.update {
                it.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun injectSyntaxErrorForTesting() {
        val current = _uiState.value.editorContent
        val broken = current + "\n\n// SYNTAX_ERROR_TRIGGER\nval testError = SYNTAX_ERROR_TRIGGER {\n"
        _uiState.update { it.copy(editorContent = broken, hasUnsavedChanges = true) }
        saveActiveFile()
        _uiState.update {
            it.copy(userNotification = "Syntax error injected! Tap Build to test error handling & AI repair.")
        }
    }

    fun createProjectWithAi(
        name: String,
        packageName: String,
        description: String,
        theme: String,
        firebase: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingNewProject = true, generationStageIndex = 0) }

            // Staged simulation steps
            for (i in 1..5) {
                delay(600)
                _uiState.update { it.copy(generationStageIndex = i) }
            }

            val pkg = packageName.ifBlank { "com.example.${name.lowercase().filter { it.isLetterOrDigit() }}" }
            val newProj = repository.createProject(
                name = name,
                packageName = pkg,
                description = description,
                theme = theme,
                firebaseRequired = firebase
            )

            delay(400)
            _uiState.update {
                it.copy(
                    isGeneratingNewProject = false,
                    activeProject = newProj,
                    currentTab = IdeScreenTab.CURRENT_IDE,
                    ideSubTab = IdeSubTab.FILES,
                    userNotification = "Project \"$name\" generated!"
                )
            }
            loadProjectFiles(newProj.id)
        }
    }

    fun renameProject(id: String, newName: String) {
        repository.renameProject(id, newName)
        _uiState.update { it.copy(userNotification = "Project renamed to $newName") }
    }

    fun duplicateProject(id: String) {
        val dup = repository.duplicateProject(id)
        if (dup != null) {
            _uiState.update { it.copy(userNotification = "Project duplicated as ${dup.name}") }
        }
    }

    fun deleteProject(id: String) {
        repository.deleteProject(id)
        _uiState.update { it.copy(userNotification = "Project deleted") }
    }

    fun triggerBuild(variant: String = "debug") {
        val project = _uiState.value.activeProject ?: return
        val files = _uiState.value.projectFiles
        if (files.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBuilding = true, ideSubTab = IdeSubTab.BUILD) }
            buildService.executeBuild(project, files, variant).collect { record ->
                _uiState.update {
                    it.copy(
                        latestBuild = record,
                        isBuilding = record.status == BuildStatus.RUNNING || record.status == BuildStatus.QUEUED
                    )
                }

                if (record.status == BuildStatus.SUCCEEDED) {
                    val durationSec = ((record.durationMs ?: 24000L) / 1000).toInt().coerceAtLeast(10)
                    val newBuiltApp = BuiltApp(
                        projectId = project.id,
                        projectName = project.name,
                        packageName = project.packageName,
                        versionName = "1.0.0",
                        versionCode = 1,
                        variant = variant,
                        fileName = "${project.name.lowercase().replace(" ", "-")}-$variant.apk",
                        fileSizeFormatted = "14.2 MB",
                        buildDurationSeconds = durationSec,
                        completedAt = System.currentTimeMillis(),
                        appTheme = project.theme
                    )
                    repository.addBuiltApp(newBuiltApp)
                }
            }
        }
    }

    fun setBuiltAppsFilter(filter: String) {
        _uiState.update { it.copy(builtAppsFilter = filter) }
    }

    fun launchBuiltApp(app: BuiltApp) {
        _uiState.update { it.copy(runningPreviewApp = app) }
    }

    fun closePreviewApp() {
        _uiState.update { it.copy(runningPreviewApp = null) }
    }

    fun downloadBuiltApk(app: BuiltApp) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingApk = true) }
            val files = repository.getFiles(app.projectId)
            when (val result = ApkStorageManager.instance.saveApkToLocalStorage(app, files)) {
                is StorageResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDownloadingApk = false,
                            lastDownloadedApkUri = result.uri,
                            lastDownloadedApkPath = result.filePath,
                            userNotification = "✓ Saved to ${result.destinationFolder}: ${result.fileName} (${result.fileSizeFormatted})"
                        )
                    }
                }
                is StorageResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isDownloadingApk = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun exportProjectZip(project: Project) {
        viewModelScope.launch {
            val files = repository.getFiles(project.id)
            when (val result = ApkStorageManager.instance.exportProjectZip(project, files)) {
                is StorageResult.Success -> {
                    _uiState.update {
                        it.copy(
                            lastDownloadedApkUri = result.uri,
                            lastDownloadedApkPath = result.filePath,
                            userNotification = "✓ Exported ZIP to ${result.destinationFolder}: ${result.fileName} (${result.fileSizeFormatted})"
                        )
                    }
                }
                is StorageResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun openDownloadedApk(context: Context) {
        val uri = _uiState.value.lastDownloadedApkUri ?: return
        try {
            val intent = ApkStorageManager.instance.createInstallIntent(uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val shareIntent = ApkStorageManager.instance.createShareIntent(uri)
                context.startActivity(Intent.createChooser(shareIntent, "Open or Share APK"))
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = "Could not open file: ${ex.localizedMessage}") }
            }
        }
    }

    fun deleteBuiltApp(id: String) {
        repository.deleteBuiltApp(id)
        _uiState.update { it.copy(userNotification = "Built APK record removed.") }
    }

    fun openBuiltAppProject(app: BuiltApp) {
        val project = _uiState.value.projects.find { it.id == app.projectId }
        if (project != null) {
            selectProject(project, switchTab = true)
        } else {
            selectTab(IdeScreenTab.CURRENT_IDE)
        }
    }

    fun sendAiPrompt(prompt: String) {
        val project = _uiState.value.activeProject ?: return
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true) }
            val req = aiAgent.processPrompt(project, prompt, _uiState.value.activeFile)
            loadProjectFiles(project.id)

            val updatedRequests = listOf(req) + _uiState.value.aiRequests
            _uiState.update {
                it.copy(
                    aiRequests = updatedRequests,
                    isAiProcessing = false,
                    userNotification = req.summary
                )
            }

            // Auto rebuild if repair triggered it
            if (req.actions.any { it.type == AIActionType.REQUEST_BUILD }) {
                triggerBuild()
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.saveSettings(newSettings)
        _uiState.update { it.copy(userNotification = "Settings saved successfully.") }
    }

    fun testCustomAiConnection(endpoint: String, apiKey: String, model: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingAiConnection = true, aiConnectionTestStatus = "Testing endpoint connection...") }
            val result = settingsRepository.testCustomAiConnection(endpoint, apiKey, model)
            _uiState.update {
                it.copy(
                    isTestingAiConnection = false,
                    aiConnectionTestStatus = (if (result.first) "✅ " else "❌ ") + result.second,
                    userNotification = if (result.first) "AI link verified successfully!" else "Custom AI connection failed."
                )
            }
        }
    }

    fun clearAiConnectionTestStatus() {
        _uiState.update { it.copy(aiConnectionTestStatus = null) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null, errorMessage = null) }
    }
}

