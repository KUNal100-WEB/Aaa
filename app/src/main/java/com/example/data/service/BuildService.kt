package com.example.data.service

import com.example.data.model.Artifact
import com.example.data.model.BuildRecord
import com.example.data.model.BuildStatus
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

interface IBuildService {
    fun executeBuild(project: Project, files: List<ProjectFile>, variant: String = "debug"): Flow<BuildRecord>
}

class MockBuildService : IBuildService {
    override fun executeBuild(
        project: Project,
        files: List<ProjectFile>,
        variant: String
    ): Flow<BuildRecord> = flow {
        val buildId = "build_" + UUID.randomUUID().toString().take(8)
        val startTime = System.currentTimeMillis()

        var currentRecord = BuildRecord(
            id = buildId,
            projectId = project.id,
            status = BuildStatus.QUEUED,
            startedAt = startTime,
            progress = 5,
            logs = """> Configure project :app
[Mock Cloud Worker] Initializing container sandbox...
[Mock Cloud Worker] JDK 17 (Eclipse Temurin) detected.
[Mock Cloud Worker] Android SDK Platform 35 initialized.
[Mock Cloud Worker] Target variant: $variant
""",
            workerType = "Mock Simulation Worker (Isolated Cloud Ready)"
        )
        emit(currentRecord)

        // Stage 1: Check required project files & syntax
        val hasManifest = files.any { it.path.contains("AndroidManifest.xml") }
        val hasGradle = files.any { it.path.contains("build.gradle") }
        val hasKotlin = files.any { it.path.endsWith(".kt") }

        var detectedError: String? = null
        for (f in files) {
            if (f.path.endsWith(".kt")) {
                val openBraces = f.content.count { it == '{' }
                val closeBraces = f.content.count { it == '}' }
                if (openBraces != closeBraces) {
                    detectedError = "e: ${f.path}: (${f.content.lines().size}, 1): Mismatched curly braces (expected $openBraces, found $closeBraces)"
                    break
                }
                if (f.content.contains("SYNTAX_ERROR_TRIGGER")) {
                    detectedError = "e: ${f.path}: (42, 12): Unresolved reference: 'SYNTAX_ERROR_TRIGGER' cannot be found in current scope."
                    break
                }
            }
        }

        if (!hasManifest) detectedError = "FATAL: AndroidManifest.xml is missing from the project structure."
        else if (!hasGradle) detectedError = "FATAL: No build.gradle or build.gradle.kts detected in app module."
        else if (!hasKotlin) detectedError = "FATAL: No Kotlin source files found in app/src/main/java/."

        delay(500)
        currentRecord = currentRecord.copy(
            status = BuildStatus.RUNNING,
            progress = 25,
            logs = currentRecord.logs + """> Task :app:preBuild UP-TO-DATE
> Task :app:generateDebugBuildConfig
> Task :app:compileDebugAidl NO-SOURCE
"""
        )
        emit(currentRecord)

        delay(700)
        val kotlinFilesCount = files.count { it.path.endsWith(".kt") }
        currentRecord = currentRecord.copy(
            progress = 55,
            logs = currentRecord.logs + """> Task :app:kspDebugKotlin UP-TO-DATE
> Task :app:compileDebugKotlin
[kotlinc] Compiling $kotlinFilesCount Kotlin source files with -jvm-target 17
"""
        )
        emit(currentRecord)

        if (detectedError != null) {
            delay(600)
            val finishTime = System.currentTimeMillis()
            currentRecord = currentRecord.copy(
                status = BuildStatus.FAILED,
                finishedAt = finishTime,
                durationMs = finishTime - startTime,
                progress = 60,
                errorSummary = detectedError,
                logs = currentRecord.logs + """
[BUILD FAILED]
$detectedError

FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> Compilation error occurred.
"""
            )
            emit(currentRecord)
            return@flow
        }

        delay(600)
        currentRecord = currentRecord.copy(
            progress = 85,
            logs = currentRecord.logs + """> Task :app:mergeDebugResources
> Task :app:processDebugManifest
> Task :app:dexBuilderDebug
> Task :app:packageDebug
"""
        )
        emit(currentRecord)

        delay(500)
        val artifactId = "apk_" + UUID.randomUUID().toString().take(8)
        val apkName = "${project.name.lowercase().replace(" ", "-")}-debug.apk"
        val finishTime = System.currentTimeMillis()

        currentRecord = currentRecord.copy(
            status = BuildStatus.SUCCEEDED,
            finishedAt = finishTime,
            durationMs = finishTime - startTime,
            progress = 100,
            artifactId = artifactId,
            logs = currentRecord.logs + """> Task :app:createDebugApkListingFileRedirect
[Mock Cloud Worker] APK signed successfully with debug keystore.
[Artifact Worker] Generated: build/outputs/apk/debug/$apkName (14.1 MB)

BUILD SUCCESSFUL in ${String.format("%.1f", (finishTime - startTime) / 1000.0)}s
42 actionable tasks: 38 executed, 4 up-to-date
"""
        )
        emit(currentRecord)
    }
}
