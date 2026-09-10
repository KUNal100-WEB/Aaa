import { v4 as uuidv4 } from 'uuid';
import { Project, ProjectFile } from '../../types.js';

const ALLOWED_EXTENSIONS = ['.kt', '.kts', '.xml', '.json', '.gradle', '.properties', '.md', '.txt', '.png', '.webp'];

export class ProjectService {
  private projects: Map<string, Project> = new Map();
  private files: Map<string, Map<string, ProjectFile>> = new Map(); // projectId -> (path -> ProjectFile)

  constructor() {
    this.seedDefaultProject();
  }

  private seedDefaultProject() {
    const defaultProject: Project = {
      id: 'proj_notes_app_demo',
      userId: 'user_dev_01',
      name: 'Modern Notes App',
      packageName: 'com.example.notesapp',
      description: 'A clean Jetpack Compose notes app with categories, search, and dark mode.',
      language: 'Kotlin',
      framework: 'Jetpack Compose',
      createdAt: Date.now() - 3600000 * 24,
      updatedAt: Date.now() - 1800000,
      theme: 'Material 3 Dark',
      appType: 'Utility',
      firebaseRequired: false
    };

    this.projects.set(defaultProject.id, defaultProject);
    const fileMap = new Map<string, ProjectFile>();

    const starterFiles: Array<{ path: string; content: string }> = [
      {
        path: 'app/src/main/AndroidManifest.xml',
        content: `<?xml version="1.0" encoding="utf-8"?>
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
            android:exported="true"
            android:theme="@style/Theme.NotesApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>`
      },
      {
        path: 'app/src/main/java/com/example/notesapp/MainActivity.kt',
        content: `package com.example.notesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
    var notes by remember {
        mutableStateOf(
            listOf(
                Note(1, "Sprint Planning meeting notes", "Work"),
                Note(2, "Grocery: Oat milk, coffee beans, avocado", "Personal"),
                Note(3, "Android Studio mobile IDE architecture specs", "Tech")
            )
        )
    }
    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Personal") }

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
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = note.title, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Category: \${note.category}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { notes = notes.filter { it.id != note.id } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}`
      },
      {
        path: 'app/build.gradle.kts',
        content: `plugins {
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
}`
      },
      {
        path: 'settings.gradle.kts',
        content: `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "NotesApp"
include(":app")`
      },
      {
        path: 'gradle.properties',
        content: `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official`
      }
    ];

    for (const file of starterFiles) {
      const fileRecord: ProjectFile = {
        id: uuidv4(),
        projectId: defaultProject.id,
        path: file.path,
        content: file.content,
        updatedAt: Date.now()
      };
      fileMap.set(file.path, fileRecord);
    }
    this.files.set(defaultProject.id, fileMap);
  }

  // Path validation and safety
  public validatePath(path: string): { valid: boolean; error?: string } {
    if (!path || typeof path !== 'string') {
      return { valid: false, error: 'Path cannot be empty.' };
    }
    if (path.includes('..') || path.startsWith('/') || path.startsWith('\\')) {
      return { valid: false, error: 'Path traversal or absolute paths are strictly forbidden.' };
    }
    const hasAllowedExt = ALLOWED_EXTENSIONS.some(ext => path.toLowerCase().endsWith(ext));
    if (!hasAllowedExt) {
      return { valid: false, error: `Unsupported file type in path: ${path}. Supported: ${ALLOWED_EXTENSIONS.join(', ')}` };
    }
    return { valid: true };
  }

  async listProjects(): Promise<Project[]> {
    return Array.from(this.projects.values()).sort((a, b) => b.updatedAt - a.updatedAt);
  }

  async getProject(id: string): Promise<Project | null> {
    return this.projects.get(id) || null;
  }

  async createProject(params: {
    name: string;
    packageName: string;
    description: string;
    theme?: string;
    appType?: string;
    firebaseRequired?: boolean;
  }): Promise<Project> {
    const id = 'proj_' + uuidv4().slice(0, 8);
    const newProject: Project = {
      id,
      userId: 'user_dev_01',
      name: params.name,
      packageName: params.packageName || `com.example.${params.name.toLowerCase().replace(/[^a-z0-9]/g, '')}`,
      description: params.description,
      language: 'Kotlin',
      framework: 'Jetpack Compose',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      theme: params.theme || 'Material 3 Dark',
      appType: params.appType || 'General',
      firebaseRequired: params.firebaseRequired || false
    };

    this.projects.set(id, newProject);
    const fileMap = new Map<string, ProjectFile>();
    this.files.set(id, fileMap);

    // Bootstrap fundamental Android project structure
    const pkgPath = newProject.packageName.replace(/\./g, '/');
    const filesToCreate = [
      {
        path: 'app/src/main/AndroidManifest.xml',
        content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="${newProject.name}"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.${newProject.name.replace(/\\s+/g, '')}">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>`
      },
      {
        path: `app/src/main/java/${pkgPath}/MainActivity.kt`,
        content: `package ${newProject.packageName}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScreen()
                }
            }
        }
    }
}

@Composable
fun AppScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${newProject.name}",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "${newProject.description}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}`
      },
      {
        path: 'app/build.gradle.kts',
        content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "${newProject.packageName}"
    compileSdk = 35

    defaultConfig {
        applicationId = "${newProject.packageName}"
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
}`
      },
      {
        path: 'settings.gradle.kts',
        content: `rootProject.name = "${newProject.name.replace(/\\s+/g, '')}"
include(":app")`
      }
    ];

    for (const f of filesToCreate) {
      const rec: ProjectFile = {
        id: uuidv4(),
        projectId: id,
        path: f.path,
        content: f.content,
        updatedAt: Date.now()
      };
      fileMap.set(f.path, rec);
    }

    return newProject;
  }

  async renameProject(id: string, newName: string): Promise<Project | null> {
    const proj = this.projects.get(id);
    if (!proj) return null;
    proj.name = newName;
    proj.updatedAt = Date.now();
    return proj;
  }

  async duplicateProject(id: string): Promise<Project | null> {
    const src = this.projects.get(id);
    if (!src) return null;
    const dup = await this.createProject({
      name: `${src.name} (Copy)`,
      packageName: `${src.packageName}.copy`,
      description: src.description,
      theme: src.theme,
      appType: src.appType,
      firebaseRequired: src.firebaseRequired
    });
    // Copy existing files
    const srcFiles = this.files.get(id);
    if (srcFiles) {
      const dupFiles = this.files.get(dup.id) || new Map<string, ProjectFile>();
      for (const [path, file] of srcFiles.entries()) {
        dupFiles.set(path, {
          id: uuidv4(),
          projectId: dup.id,
          path,
          content: file.content,
          updatedAt: Date.now()
        });
      }
      this.files.set(dup.id, dupFiles);
    }
    return dup;
  }

  async deleteProject(id: string): Promise<boolean> {
    this.files.delete(id);
    return this.projects.delete(id);
  }

  async getFiles(projectId: string): Promise<ProjectFile[]> {
    const fileMap = this.files.get(projectId);
    if (!fileMap) return [];
    return Array.from(fileMap.values()).sort((a, b) => a.path.localeCompare(b.path));
  }

  async getFile(projectId: string, path: string): Promise<ProjectFile | null> {
    const fileMap = this.files.get(projectId);
    if (!fileMap) return null;
    return fileMap.get(path) || null;
  }

  async saveFile(projectId: string, path: string, content: string): Promise<ProjectFile> {
    const validation = this.validatePath(path);
    if (!validation.valid) {
      throw new Error(validation.error);
    }
    let fileMap = this.files.get(projectId);
    if (!fileMap) {
      fileMap = new Map();
      this.files.set(projectId, fileMap);
    }
    const existing = fileMap.get(path);
    const updated: ProjectFile = {
      id: existing ? existing.id : uuidv4(),
      projectId,
      path,
      content,
      updatedAt: Date.now()
    };
    fileMap.set(path, updated);

    const proj = this.projects.get(projectId);
    if (proj) {
      proj.updatedAt = Date.now();
    }
    return updated;
  }

  async deleteFile(projectId: string, path: string): Promise<boolean> {
    const fileMap = this.files.get(projectId);
    if (!fileMap) return false;
    const res = fileMap.delete(path);
    const proj = this.projects.get(projectId);
    if (proj) proj.updatedAt = Date.now();
    return res;
  }
}

export const projectService = new ProjectService();
