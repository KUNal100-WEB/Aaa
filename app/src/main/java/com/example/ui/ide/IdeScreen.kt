package com.example.ui.ide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BuildStatus
import com.example.data.model.ProjectFile
import com.example.ui.IdeUiState
import com.example.ui.MainIdeViewModel
import com.example.ui.navigation.IdeSubTab
import com.example.ui.theme.IdeCodeBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeScreen(
    uiState: IdeUiState,
    viewModel: MainIdeViewModel,
    modifier: Modifier = Modifier
) {
    val project = uiState.activeProject

    if (project == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No project selected. Please select a project from Projects tab.")
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            project.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.triggerBuild() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Build Project", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            // IDE Sub-tabs
            TabRow(
                selectedTabIndex = uiState.ideSubTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                IdeSubTab.values().forEach { subTab ->
                    Tab(
                        selected = uiState.ideSubTab == subTab,
                        onClick = { viewModel.selectIdeSubTab(subTab) },
                        text = { Text(subTab.title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (uiState.ideSubTab) {
                IdeSubTab.FILES -> FileTreeTab(uiState.projectFiles, onSelectFile = { viewModel.selectFile(it) })
                IdeSubTab.CODE -> CodeEditorTab(
                    activeFile = uiState.activeFile,
                    content = uiState.editorContent,
                    hasUnsaved = uiState.hasUnsavedChanges,
                    onContentChange = { viewModel.updateEditorContent(it) },
                    onSave = { viewModel.saveActiveFile() },
                    onInjectError = { viewModel.injectSyntaxErrorForTesting() }
                )
                IdeSubTab.AI -> AiAssistantTab(
                    uiState = uiState,
                    onSendPrompt = { viewModel.sendAiPrompt(it) }
                )
                IdeSubTab.BUILD -> BuildConsoleTab(
                    uiState = uiState,
                    onTriggerBuild = { viewModel.triggerBuild() },
                    onFixError = { errorText ->
                        viewModel.sendAiPrompt("Fix this build error: $errorText")
                    }
                )
            }
        }
    }
}

@Composable
fun FileTreeTab(files: List<ProjectFile>, onSelectFile: (ProjectFile) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeCodeBg, shape = MaterialTheme.shapes.medium)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(files, key = { it.path }) { file ->
            val icon = when {
                file.path.endsWith(".kt") -> Icons.Default.Code
                file.path.endsWith(".xml") -> Icons.Default.DataObject
                file.path.endsWith(".gradle") || file.path.endsWith(".kts") -> Icons.Default.Build
                else -> Icons.Default.Description
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFile(file) }
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun CodeEditorTab(
    activeFile: ProjectFile?,
    content: String,
    hasUnsaved: Boolean,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onInjectError: () -> Unit
) {
    if (activeFile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a file to edit")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = activeFile.path.substringAfterLast('/'),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace
                )
                if (hasUnsaved) {
                    Text(" *", color = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onInjectError, modifier = Modifier.height(32.dp)) {
                    Text("Test Error", style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = onSave, modifier = Modifier.height(32.dp)) {
                    Text("Save", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Editor Surface
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.medium,
            color = IdeCodeBg
        ) {
            TextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun AiAssistantTab(
    uiState: IdeUiState,
    onSendPrompt: (String) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Explain architecture",
                "Add Firebase auth",
                "Create settings screen",
                "Fix build error",
                "Refactor Compose"
            ).forEach { chip ->
                AssistChip(
                    onClick = { onSendPrompt(chip) },
                    label = { Text(chip, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Phone AI Android IDE Agent Ready",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "I can generate Compose components, edit files, and automatically repair compilation failures.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(uiState.aiRequests) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "User: ${req.userMessage}", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = req.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask AI for changes...") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        onSendPrompt(promptInput)
                        promptInput = ""
                    }
                },
                enabled = !uiState.isAiProcessing && promptInput.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun BuildConsoleTab(
    uiState: IdeUiState,
    onTriggerBuild: () -> Unit,
    onFixError: (String) -> Unit
) {
    val build = uiState.latestBuild

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Build Console", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Worker: Mock Isolated Cloud Builder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (build?.status) {
                    BuildStatus.SUCCEEDED -> MaterialTheme.colorScheme.tertiary
                    BuildStatus.FAILED -> MaterialTheme.colorScheme.error
                    BuildStatus.RUNNING -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = build?.status?.name ?: "IDLE",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isBuilding) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Error Banner
        if (build?.status == BuildStatus.FAILED && build.errorSummary != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Build Failed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(build.errorSummary, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = { onFixError(build.errorSummary) }) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fix with AI")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Artifact Ready Banner
        if (build?.status == BuildStatus.SUCCEEDED) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("APK Ready (14.1 MB)", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        "* Real cloud build worker connects to genuine APK outputs. Pipeline handoff ready.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Compiler Terminal Logs
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = IdeCodeBg
        ) {
            val logScroll = rememberScrollState()
            Text(
                text = build?.logs ?: "> Ready. Tap Build to start remote compilation.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .verticalScroll(logScroll),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onTriggerBuild,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBuilding
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Build Again")
        }
    }
}
