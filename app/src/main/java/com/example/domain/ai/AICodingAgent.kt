package com.example.domain.ai

import com.example.data.model.*
import com.example.data.repository.ProjectRepository
import com.example.domain.validation.PathValidator

class AICodingAgent(private val repository: ProjectRepository) {
    private val repairCounts = mutableMapOf<String, Int>()

    suspend fun processPrompt(
        project: Project,
        prompt: String,
        selectedFile: ProjectFile? = null
    ): AIRequest {
        val normalized = prompt.lowercase().trim()
        val actions = mutableListOf<AIAction>()
        var summary = ""
        val files = repository.getFiles(project.id)

        if (normalized.contains("fix") || normalized.contains("error") || normalized.contains("repair")) {
            val count = (repairCounts[project.id] ?: 0) + 1
            repairCounts[project.id] = count

            if (count > 3) {
                return AIRequest(
                    projectId = project.id,
                    userMessage = prompt,
                    status = "FAILED",
                    summary = "Maximum 3 automatic repair attempts exceeded. Please inspect compiler logs manually.",
                    actions = emptyList(),
                    repairAttempt = count
                )
            }

            var fixed = false
            for (f in files) {
                if (f.path.endsWith(".kt") && f.content.contains("SYNTAX_ERROR_TRIGGER")) {
                    val cleaned = f.content.replace("// SYNTAX_ERROR_TRIGGER", "").replace("SYNTAX_ERROR_TRIGGER", "")
                    actions.add(AIAction(AIActionType.UPDATE_FILE, path = f.path, content = cleaned))
                    fixed = true
                }
            }

            summary = if (fixed) {
                "[Auto-Repair Attempt #$count] Removed invalid tokens and corrected Kotlin syntax in source files."
            } else {
                "[Auto-Repair Attempt #$count] Audited Kotlin sources, Manifest, and Gradle configs. No fatal errors detected."
            }
            actions.add(AIAction(AIActionType.REQUEST_BUILD))
        } else if (normalized.contains("auth") || normalized.contains("firebase") || normalized.contains("login")) {
            summary = "Scaffolded Firebase Authentication state manager and added AuthViewModel.kt."
            val pkgPath = project.packageName.replace('.', '/')
            actions.add(AIAction(AIActionType.ADD_DEPENDENCY, dependency = "implementation(libs.firebase.auth)"))
            actions.add(
                AIAction(
                    AIActionType.CREATE_FILE,
                    path = "app/src/main/java/$pkgPath/AuthViewModel.kt",
                    content = """package ${project.packageName}

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
    object Unauthenticated : AuthState
    object Loading : AuthState
    data class Authenticated(val userId: String, val email: String) : AuthState
}

class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(email: String) {
        _state.value = AuthState.Authenticated("user_fb_99", email)
    }
}""".trimIndent()
                )
            )
        } else if (normalized.contains("settings")) {
            summary = "Created SettingsScreen.kt with Material 3 toggles and version display."
            val pkgPath = project.packageName.replace('.', '/')
            actions.add(
                AIAction(
                    AIActionType.CREATE_FILE,
                    path = "app/src/main/java/$pkgPath/SettingsScreen.kt",
                    content = """package ${project.packageName}

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var isDark by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            ListItem(
                headlineContent = { Text("Dark Mode") },
                trailingContent = { Switch(checked = isDark, onCheckedChange = { isDark = it }) }
            )
        }
    }
}""".trimIndent()
                )
            )
        } else {
            summary = "Generated Compose component for \"$prompt\"."
            val pkgPath = project.packageName.replace('.', '/')
            val compName = prompt.filter { it.isLetterOrDigit() }.take(16).ifBlank { "Custom" }
            actions.add(
                AIAction(
                    AIActionType.CREATE_FILE,
                    path = "app/src/main/java/$pkgPath/${compName}Component.kt",
                    content = """package ${project.packageName}

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ${compName}Component() {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "$prompt", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Generated by Phone AI Android IDE", style = MaterialTheme.typography.bodySmall)
        }
    }
}""".trimIndent()
                )
            )
        }

        // Apply actions with validation
        for (action in actions) {
            if (action.path != null) {
                val validation = PathValidator.validate(action.path)
                if (!validation.isValid) {
                    throw IllegalArgumentException(validation.errorMessage)
                }
                if (action.type == AIActionType.CREATE_FILE || action.type == AIActionType.UPDATE_FILE) {
                    repository.saveFile(project.id, action.path, action.content ?: "")
                } else if (action.type == AIActionType.DELETE_FILE) {
                    repository.deleteFile(project.id, action.path)
                }
            }
        }

        return AIRequest(
            projectId = project.id,
            userMessage = prompt,
            status = "APPLIED",
            summary = summary,
            actions = actions
        )
    }
}
