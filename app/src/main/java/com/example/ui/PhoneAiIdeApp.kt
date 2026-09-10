package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ai.AIBuilderScreen
import com.example.ui.builtapps.BuiltAppsScreen
import com.example.ui.ide.IdeScreen
import com.example.ui.navigation.IdeScreenTab
import com.example.ui.projects.ProjectsScreen
import com.example.ui.settings.SettingsScreen

@Composable
fun PhoneAiIdeApp(viewModel: MainIdeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar("Error: $err")
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                IdeScreenTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = uiState.currentTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                IdeScreenTab.PROJECTS -> ProjectsScreen(uiState, viewModel)
                IdeScreenTab.AI_BUILDER -> AIBuilderScreen(uiState, viewModel)
                IdeScreenTab.CURRENT_IDE -> IdeScreen(uiState, viewModel)
                IdeScreenTab.BUILT_APPS -> BuiltAppsScreen(uiState, viewModel)
                IdeScreenTab.SETTINGS -> SettingsScreen(uiState, viewModel)
            }
        }
    }
}
