package com.example.data.model

import java.util.UUID

data class User(
    val id: String = "user_dev_01",
    val email: String = "developer@phone-ai-ide.internal",
    val createdAt: Long = System.currentTimeMillis()
)

data class Project(
    val id: String = "proj_" + UUID.randomUUID().toString().take(8),
    val userId: String = "user_dev_01",
    val name: String,
    val packageName: String,
    val description: String,
    val language: String = "Kotlin",
    val framework: String = "Jetpack Compose",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val theme: String = "Material 3 Dark",
    val appType: String = "Mobile",
    val firebaseRequired: Boolean = false
)

data class ProjectFile(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val path: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class BuildStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    TIMEOUT,
    CANCELLED
}

data class BuildRecord(
    val id: String = "build_" + UUID.randomUUID().toString().take(8),
    val projectId: String,
    val status: BuildStatus = BuildStatus.QUEUED,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val durationMs: Long? = null,
    val progress: Int = 0,
    val logs: String = "",
    val errorSummary: String? = null,
    val artifactId: String? = null,
    val workerType: String = "Mock Simulation Worker (Isolated Cloud Ready)"
)

data class Artifact(
    val id: String = "apk_" + UUID.randomUUID().toString().take(8),
    val buildId: String,
    val fileName: String,
    val size: Long = 14859200L,
    val downloadUrl: String,
    val expiresAt: Long = System.currentTimeMillis() + 86400000L * 3
)

enum class AIActionType {
    CREATE_FILE,
    UPDATE_FILE,
    DELETE_FILE,
    RENAME_FILE,
    ADD_DEPENDENCY,
    REQUEST_BUILD
}

data class AIAction(
    val type: AIActionType,
    val path: String? = null,
    val content: String? = null,
    val dependency: String? = null
)

data class AIRequest(
    val id: String = "req_" + UUID.randomUUID().toString().take(8),
    val projectId: String,
    val userMessage: String,
    val status: String = "APPLIED",
    val summary: String,
    val actions: List<AIAction> = emptyList(),
    val repairAttempt: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class BuiltApp(
    val id: String = "built_" + UUID.randomUUID().toString().take(8),
    val projectId: String,
    val projectName: String,
    val packageName: String,
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val variant: String = "debug",
    val fileName: String = "app-debug.apk",
    val fileSizeFormatted: String = "14.2 MB",
    val buildDurationSeconds: Int = 24,
    val completedAt: Long = System.currentTimeMillis(),
    val appTheme: String = "Material 3 Dark",
    val permissions: List<String> = listOf("INTERNET", "ACCESS_NETWORK_STATE"),
    val targetSdk: Int = 35,
    val minSdk: Int = 24,
    val signatureType: String = "Android Debug Keystore (SHA-256)"
)

