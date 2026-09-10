package com.example.data.repository

import com.example.data.model.BuiltApp
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import com.example.domain.validation.PathValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProjectRepository {
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _builtApps = MutableStateFlow<List<BuiltApp>>(emptyList())
    val builtApps: StateFlow<List<BuiltApp>> = _builtApps.asStateFlow()

    private val filesMap = mutableMapOf<String, MutableMap<String, ProjectFile>>() // projectId -> path -> file

    init {
        seedInitialProject()
    }

    private fun seedInitialProject() {
        val defaultProject = Project(
            id = "proj_notes_app_demo",
            name = "Modern Notes App",
            packageName = "com.example.notesapp",
            description = "A clean Jetpack Compose notes app with categories, search, and dark mode."
        )
        val fileMap = mutableMapOf<String, ProjectFile>()

        val defaultFiles = listOf(
            ProjectFile(
                projectId = defaultProject.id,
                path = "app/src/main/AndroidManifest.xml",
                content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Notes App"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.NotesApp">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>""".trimIndent()
            ),
            ProjectFile(
                projectId = defaultProject.id,
                path = "app/src/main/java/com/example/notesapp/MainActivity.kt",
                content = """package com.example.notesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Note(val id: Long, val title: String, val category: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotesAppScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesAppScreen() {
    val notes = remember {
        mutableStateListOf(
            Note(1, "Sprint Planning meeting notes", "Work"),
            Note(2, "Grocery: Oat milk, coffee beans", "Personal"),
            Note(3, "Phone AI IDE Mobile Architecture", "Tech")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modern Notes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Add note */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = note.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Category: " + note.category, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}""".trimIndent()
            ),
            ProjectFile(
                projectId = defaultProject.id,
                path = "app/build.gradle.kts",
                content = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.notesapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.notesapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
}""".trimIndent()
            ),
            ProjectFile(
                projectId = defaultProject.id,
                path = "settings.gradle.kts",
                content = """rootProject.name = "NotesApp"
include(":app")""".trimIndent()
            )
        )

        for (f in defaultFiles) {
            fileMap[f.path] = f
        }
        filesMap[defaultProject.id] = fileMap
        _projects.value = listOf(defaultProject)

        _builtApps.value = listOf(
            BuiltApp(
                id = "built_notes_demo",
                projectId = defaultProject.id,
                projectName = defaultProject.name,
                packageName = defaultProject.packageName,
                versionName = "1.0.0",
                versionCode = 1,
                variant = "debug",
                fileName = "modern-notes-app-debug.apk",
                fileSizeFormatted = "14.2 MB",
                buildDurationSeconds = 24,
                completedAt = System.currentTimeMillis() - 7200000L,
                appTheme = defaultProject.theme,
                permissions = listOf("INTERNET", "ACCESS_NETWORK_STATE")
            )
        )
    }

    fun addBuiltApp(builtApp: BuiltApp) {
        _builtApps.value = listOf(builtApp) + _builtApps.value.filter { it.id != builtApp.id }
    }

    fun deleteBuiltApp(id: String) {
        _builtApps.value = _builtApps.value.filter { it.id != id }
    }

    fun getProject(id: String): Project? = _projects.value.find { it.id == id }

    fun getFiles(projectId: String): List<ProjectFile> {
        return filesMap[projectId]?.values?.sortedBy { it.path } ?: emptyList()
    }

    fun getFile(projectId: String, path: String): ProjectFile? {
        return filesMap[projectId]?.get(path)
    }

    fun saveFile(projectId: String, path: String, content: String): Result<ProjectFile> {
        val validation = PathValidator.validate(path)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid path"))
        }
        val fileMap = filesMap.getOrPut(projectId) { mutableMapOf() }
        val updated = ProjectFile(
            id = fileMap[path]?.id ?: UUID.randomUUID().toString(),
            projectId = projectId,
            path = path,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
        fileMap[path] = updated

        // Update project timestamp
        _projects.value = _projects.value.map {
            if (it.id == projectId) it.copy(updatedAt = System.currentTimeMillis()) else it
        }
        return Result.success(updated)
    }

    fun deleteFile(projectId: String, path: String): Boolean {
        return filesMap[projectId]?.remove(path) != null
    }

    fun createProject(
        name: String,
        packageName: String,
        description: String,
        theme: String = "Material 3 Dark",
        appType: String = "Mobile",
        firebaseRequired: Boolean = false
    ): Project {
        val proj = Project(
            name = name,
            packageName = packageName,
            description = description,
            theme = theme,
            appType = appType,
            firebaseRequired = firebaseRequired
        )
        val fileMap = mutableMapOf<String, ProjectFile>()

        val pkgPath = packageName.replace('.', '/')
        val starterFiles = listOf(
            ProjectFile(
                projectId = proj.id,
                path = "app/src/main/AndroidManifest.xml",
                content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="${proj.name}"
        android:theme="@style/Theme.Material3">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>""".trimIndent()
            ),
            ProjectFile(
                projectId = proj.id,
                path = "app/src/main/java/$pkgPath/MainActivity.kt",
                content = """package ${proj.packageName}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "${proj.name}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${proj.description}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}""".trimIndent()
            ),
            ProjectFile(
                projectId = proj.id,
                path = "app/build.gradle.kts",
                content = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "${proj.packageName}"
    compileSdk = 35

    defaultConfig {
        applicationId = "${proj.packageName}"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
}""".trimIndent()
            )
        )

        for (f in starterFiles) {
            fileMap[f.path] = f
        }
        filesMap[proj.id] = fileMap

        _projects.value = listOf(proj) + _projects.value
        return proj
    }

    fun renameProject(id: String, newName: String) {
        _projects.value = _projects.value.map {
            if (it.id == id) it.copy(name = newName, updatedAt = System.currentTimeMillis()) else it
        }
    }

    fun duplicateProject(id: String): Project? {
        val src = getProject(id) ?: return null
        val dup = createProject(
            name = "${src.name} (Copy)",
            packageName = "${src.packageName}.copy",
            description = src.description,
            theme = src.theme,
            appType = src.appType,
            firebaseRequired = src.firebaseRequired
        )
        val srcFiles = filesMap[id]
        if (srcFiles != null) {
            val dupMap = filesMap[dup.id] ?: mutableMapOf()
            srcFiles.forEach { (path, f) ->
                dupMap[path] = f.copy(id = UUID.randomUUID().toString(), projectId = dup.id)
            }
            filesMap[dup.id] = dupMap
        }
        return dup
    }

    fun deleteProject(id: String) {
        filesMap.remove(id)
        _projects.value = _projects.value.filter { it.id != id }
    }
}
