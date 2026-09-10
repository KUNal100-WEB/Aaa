package com.example.ui.builtapps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BuiltApp
import com.example.ui.IdeUiState
import com.example.ui.MainIdeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuiltAppsScreen(
    uiState: IdeUiState,
    viewModel: MainIdeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForDetails by remember { mutableStateOf<BuiltApp?>(null) }

    val filteredApps = remember(uiState.builtApps, searchQuery, uiState.builtAppsFilter) {
        uiState.builtApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.projectName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (uiState.builtAppsFilter) {
                "DEBUG" -> app.variant.equals("debug", ignoreCase = true)
                "RELEASE" -> app.variant.equals("release", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Built Apps & APKs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${uiState.builtApps.size}",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.triggerBuild("debug") },
                        enabled = !uiState.isBuilding && uiState.activeProject != null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Build Current", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Compiled APK packages ready for installation, direct running, or distribution.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.lastDownloadedApkUri != null && uiState.userNotification != null && uiState.userNotification.contains("Saved to")) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "APK Saved to Phone Local Storage",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.userNotification ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.openDownloadedApk(context) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Install / Open APK", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Metric Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Built APKs",
                    value = "${uiState.builtApps.size}",
                    icon = Icons.Default.Android,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Storage",
                    value = "14.2 MB",
                    icon = Icons.Default.Inventory2,
                    accentColor = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Signing",
                    value = "Debug Key",
                    icon = Icons.Default.Verified,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search built apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("built_apps_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.builtAppsFilter == "ALL",
                    onClick = { viewModel.setBuiltAppsFilter("ALL") },
                    label = { Text("All Builds (${uiState.builtApps.size})") }
                )
                FilterChip(
                    selected = uiState.builtAppsFilter == "DEBUG",
                    onClick = { viewModel.setBuiltAppsFilter("DEBUG") },
                    label = { Text("Debug APKs") }
                )
                FilterChip(
                    selected = uiState.builtAppsFilter == "RELEASE",
                    onClick = { viewModel.setBuiltAppsFilter("RELEASE") },
                    label = { Text("Release") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List of Built Apps
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Built Apps Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Build your project in the IDE or use AI Builder to compile your first Android APK.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.triggerBuild("debug") },
                            enabled = uiState.activeProject != null
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Build Active Project Now")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredApps, key = { it.id }) { app ->
                        BuiltAppCard(
                            app = app,
                            onRun = { viewModel.launchBuiltApp(app) },
                            onDownload = { viewModel.downloadBuiltApk(app) },
                            onOpenIde = { viewModel.openBuiltAppProject(app) },
                            onDelete = { viewModel.deleteBuiltApp(app.id) },
                            onDetails = { selectedAppForDetails = app }
                        )
                    }
                }
            }
        }
    }

    // App Running Live Simulator Dialog
    uiState.runningPreviewApp?.let { app ->
        RunningAppSimulatorDialog(
            app = app,
            onDismiss = { viewModel.closePreviewApp() }
        )
    }

    // App Details & Manifest Dialog
    selectedAppForDetails?.let { app ->
        AppDetailsDialog(
            app = app,
            onDismiss = { selectedAppForDetails = null },
            onRun = {
                selectedAppForDetails = null
                viewModel.launchBuiltApp(app)
            },
            onDownload = {
                viewModel.downloadBuiltApk(app)
            },
            onExportZip = {
                val proj = uiState.projects.find { it.id == app.projectId } ?: uiState.activeProject
                if (proj != null) viewModel.exportProjectZip(proj)
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BuiltAppCard(
    app: BuiltApp,
    onRun: () -> Unit,
    onDownload: () -> Unit,
    onOpenIde: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(app.completedAt) { dateFormat.format(Date(app.completedAt)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDetails() }
            .testTag("built_app_card_${app.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App Icon Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF10B981))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = "App Icon",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.projectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (app.variant == "debug") Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF6366F1).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = app.variant.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (app.variant == "debug") Color(0xFF10B981) else Color(0xFF6366F1)
                            )
                        }
                    }

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Built APK",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("v${app.versionName}", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(app.fileSizeFormatted, fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("Build: ${app.buildDurationSeconds}s", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRun,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run App", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("APK")
                }

                IconButton(
                    onClick = onOpenIde,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Open in IDE",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RunningAppSimulatorDialog(
    app: BuiltApp,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Simulated Phone Container
            Card(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF374151)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Phone Status Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "9:41",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        // Notch or camera dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Simulated App Header Bar
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.projectName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Running App",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Interactive App Canvas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF090D16))
                            .padding(16.dp)
                    ) {
                        SimulatedAppInteractiveContent(app = app)
                    }

                    // Bottom Navigation / Gesture bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Gray.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedAppInteractiveContent(app: BuiltApp) {
    val name = app.projectName.lowercase()
    val isEcommerce = name.contains("shop") || name.contains("store") || name.contains("ecommerce") || name.contains("e-commerce") || name.contains("cart")
    val isFitness = name.contains("fit") || name.contains("workout") || name.contains("gym") || name.contains("step") || name.contains("run")
    val isMusic = name.contains("music") || name.contains("song") || name.contains("audio") || name.contains("player")
    val isChat = name.contains("chat") || name.contains("message") || name.contains("messenger")
    val isFinance = name.contains("finance") || name.contains("expense") || name.contains("budget") || name.contains("money")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isEcommerce -> "Shop Catalog"
                    isFitness -> "Fitness Tracker"
                    isMusic -> "Audio Player"
                    isChat -> "AI Messenger"
                    isFinance -> "Budget Wallet"
                    else -> "Task Dashboard"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "RUNNING LIVE",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            isEcommerce -> {
                var cartCount by remember { mutableStateOf(1) }
                var orderPlaced by remember { mutableStateOf(false) }
                val products = listOf(
                    "Aura Wireless ANC Headphones" to 149.99,
                    "Pulse Smartwatch Pro" to 199.50,
                    "Ergonomic Mechanical Keyboard" to 89.00,
                    "MagSafe Fast Charging Pad" to 39.99
                )

                if (orderPlaced) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🎉 Order Placed Successfully!", fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            Text("Your items are being prepared for shipping.", fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { orderPlaced = false }) { Text("Shop More") }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cart Items: $cartCount", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { orderPlaced = true; cartCount = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Checkout ($cartCount)")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(products) { (title, price) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("$$price", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { cartCount++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                ) {
                                    Text("+ Add", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            isFitness -> {
                var steps by remember { mutableStateOf(7420) }
                val workouts = remember {
                    mutableStateListOf(
                        "Morning Sunrise Run (30 mins)" to true,
                        "Core HIIT Blast (20 mins)" to true,
                        "Strength & Hypertrophy (45 mins)" to false,
                        "Evening Walk (25 mins)" to false
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily Step Counter", color = Color.Gray, fontSize = 11.sp)
                                Text("$steps / 10,000", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            }
                            Button(
                                onClick = { steps += 500 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("+500 Steps")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (steps / 10000f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF10B981)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Daily Activities", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(workouts.indices.toList()) { index ->
                        val (title, done) = workouts[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(title, color = Color.White, fontSize = 13.sp)
                                Checkbox(
                                    checked = done,
                                    onCheckedChange = { workouts[index] = title to it }
                                )
                            }
                        }
                    }
                }
            }
            isMusic -> {
                var isPlaying by remember { mutableStateOf(true) }
                var trackName by remember { mutableStateOf("Midnight LoFi Grooves") }
                val playlist = listOf("Midnight LoFi Grooves", "Electric Aurora Wave", "Sunset in Tokyo", "Starlight Ambient")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6366F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(trackName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Synthwave Chill Records", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledIconButton(
                            onClick = { isPlaying = !isPlaying },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Playlist Queue", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    items(playlist) { song ->
                        Card(
                            onClick = { trackName = song },
                            colors = CardDefaults.cardColors(
                                containerColor = if (song == trackName) Color(0xFF334155) else Color(0xFF1E293B)
                            )
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(song, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            else -> {
                // Notes / Tasks
                var notesList by remember {
                    mutableStateOf(
                        listOf(
                            "Welcome to ${app.projectName}!" to "Personal",
                            "Built using Google AI Studio on mobile" to "Tech",
                            "Saved directly to phone Downloads" to "Storage",
                            "Ready to export APK and share" to "Release"
                        )
                    )
                }
                var newNoteText by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = { newNoteText = it },
                        placeholder = { Text("Add new item...", fontSize = 12.sp, color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newNoteText.isNotBlank()) {
                                notesList = listOf(newNoteText to "General") + notesList
                                newNoteText = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notesList) { (title, category) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Category: $category",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppDetailsDialog(
    app: BuiltApp,
    onDismiss: () -> Unit,
    onRun: () -> Unit,
    onDownload: () -> Unit,
    onExportZip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(app.projectName)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Package: ${app.packageName}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Binary: ${app.fileName} (${app.fileSizeFormatted})",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Variant: ${app.variant.uppercase()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Signature: ${app.signatureType}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Permissions: ${app.permissions.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Target SDK: ${app.targetSdk} | Min SDK: ${app.minSdk}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onExportZip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Source Code ZIP", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onRun) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run App")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download APK")
            }
        }
    )
}
