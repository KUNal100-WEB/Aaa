package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.ui.IdeUiState
import com.example.ui.MainIdeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: IdeUiState,
    viewModel: MainIdeViewModel,
    modifier: Modifier = Modifier
) {
    val currentSettings = uiState.settings

    var selectedModel by remember(currentSettings.selectedModel) { mutableStateOf(currentSettings.selectedModel) }
    var useCustomAi by remember(currentSettings.useCustomAi) { mutableStateOf(currentSettings.useCustomAi) }
    var customEndpoint by remember(currentSettings.customAiEndpoint) { mutableStateOf(currentSettings.customAiEndpoint) }
    var customApiKey by remember(currentSettings.customAiApiKey) { mutableStateOf(currentSettings.customAiApiKey) }
    var customModelName by remember(currentSettings.customAiModelName) { mutableStateOf(currentSettings.customAiModelName) }
    var showApiKey by remember { mutableStateOf(false) }
    var firebaseEnabled by remember(currentSettings.firebaseEnabled) { mutableStateOf(currentSettings.firebaseEnabled) }

    fun persistCurrentSettings() {
        viewModel.updateSettings(
            AppSettings(
                selectedModel = selectedModel,
                useCustomAi = useCustomAi,
                customAiEndpoint = customEndpoint.trim(),
                customAiApiKey = customApiKey.trim(),
                customAiModelName = customModelName.trim().ifBlank { "gemini-2.5-flash" },
                firebaseEnabled = firebaseEnabled,
                cloudBuildSimulation = currentSettings.cloudBuildSimulation,
                darkBlueTheme = true
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Settings & Custom AI")
                    }
                },
                actions = {
                    Button(
                        onClick = { persistCurrentSettings() },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save All", fontSize = 13.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Custom AI API Section ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (useCustomAi) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Custom AI Endpoint",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Drop your custom AI API link/keys for building projects & auditing functions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useCustomAi,
                                onCheckedChange = {
                                    useCustomAi = it
                                    persistCurrentSettings()
                                }
                            )
                        }

                        if (useCustomAi) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // API Link / URL
                            Text(
                                "AI API Endpoint / Link *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customEndpoint,
                                onValueChange = { customEndpoint = it },
                                placeholder = { Text("https://api.openai.com/v1/chat/completions or custom proxy") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // API Key / Token
                            Text(
                                "API Key / Bearer Token",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customApiKey,
                                onValueChange = { customApiKey = it },
                                placeholder = { Text("sk-... or API token (stored securely)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            Icons.Outlined.Lock,
                                            contentDescription = if (showApiKey) "Hide Key" else "Show Key",
                                            tint = if (showApiKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Model Name
                            Text(
                                "Model Identifier",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customModelName,
                                onValueChange = { customModelName = it },
                                placeholder = { Text("e.g. gemini-2.5-flash, gpt-4o, claude-3-5-sonnet") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Test Connection & Verification Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        persistCurrentSettings()
                                        viewModel.testCustomAiConnection(customEndpoint, customApiKey, customModelName)
                                    },
                                    enabled = !uiState.isTestingAiConnection && customEndpoint.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (uiState.isTestingAiConnection) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Testing Connection...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Test & Verify Link", fontSize = 13.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { persistCurrentSettings() }
                                ) {
                                    Text("Save Link", fontSize = 13.sp)
                                }
                            }

                            // Connection Verification Feedback Banner
                            if (uiState.aiConnectionTestStatus != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val isSuccess = uiState.aiConnectionTestStatus.startsWith("✅")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSuccess) Color(0xFF063A20) else Color(0xFF3F1116),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            uiState.aiConnectionTestStatus,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSuccess) Color(0xFF4ADE80) else Color(0xFFF87171),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.clearAiConnectionTestStatus() },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Text("✕", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Using Google AI Studio default cloud models (Gemini 2.5 Flash / 1.5 Pro). Enable above to drop custom link.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- UI Theme Status ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Dark Blue Midnight Palette (Active)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dark Blue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // --- Default AI Model Configuration ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Default Gemini Models", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select fallback model for project generation and auto-repairs when custom AI is off.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "Gemini 2.5 Flash (Recommended)",
                            "Gemini 1.5 Pro (Deep Code Reasoning)",
                            "Gemini 2.0 Flash"
                        ).forEach { modelName ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(modelName, style = MaterialTheme.typography.bodySmall)
                                RadioButton(
                                    selected = selectedModel == modelName,
                                    onClick = {
                                        selectedModel = modelName
                                        persistCurrentSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Cloud Build Worker Section ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cloud Build Worker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Status: Ready (Isolated Cloud Worker Simulation)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Compiling pipeline is powered by Java 17, Android SDK 35, and Gradle 8.9 with real-time error auditing and APK signing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- Firebase Readiness ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Firebase & Cloud Readiness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Firebase Auth & Cloud Firestore", style = MaterialTheme.typography.bodySmall)
                                Text("Auto-inject SDK stubs when project requires login or databases", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = firebaseEnabled,
                                onCheckedChange = {
                                    firebaseEnabled = it
                                    persistCurrentSettings()
                                }
                            )
                        }
                    }
                }
            }

            // --- Local Persistence Status ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Local Persistence & Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "All projects, created files, build history, APK records, and AI settings are automatically saved to device internal storage and persist across app restarts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- About ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phone AI Android IDE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Version 1.1.0 • Dark Blue Edition", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "\"Android Studio + Autonomous AI Agent, redesigned for phone-first development.\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
