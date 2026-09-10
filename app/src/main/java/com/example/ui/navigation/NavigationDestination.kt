package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class IdeScreenTab(val title: String, val icon: ImageVector) {
    PROJECTS("Projects", Icons.Default.GridView),
    AI_BUILDER("AI Builder", Icons.Default.AutoAwesome),
    CURRENT_IDE("Current IDE", Icons.Default.Terminal),
    BUILT_APPS("Built Apps", Icons.Default.Android),
    SETTINGS("Settings", Icons.Default.Settings)
}

enum class IdeSubTab(val title: String) {
    FILES("Files"),
    CODE("Code"),
    AI("AI"),
    BUILD("Build")
}
