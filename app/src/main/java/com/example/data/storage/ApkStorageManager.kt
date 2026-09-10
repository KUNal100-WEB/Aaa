package com.example.data.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.BuiltApp
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed interface StorageResult {
    data class Success(
        val uri: Uri?,
        val filePath: String,
        val fileName: String,
        val fileSizeFormatted: String,
        val destinationFolder: String
    ) : StorageResult

    data class Error(val message: String) : StorageResult
}

class ApkStorageManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ApkStorageManager"

        @Volatile
        private var INSTANCE: ApkStorageManager? = null

        fun init(context: Context): ApkStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApkStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        val instance: ApkStorageManager
            get() = INSTANCE ?: throw IllegalStateException("ApkStorageManager must be initialized in Application.onCreate")
    }

    suspend fun saveApkToLocalStorage(
        app: BuiltApp,
        projectFiles: List<ProjectFile>
    ): StorageResult = withContext(Dispatchers.IO) {
        try {
            val apkBytes = generateApkBytes(app, projectFiles)
            val fileName = if (app.fileName.endsWith(".apk", ignoreCase = true)) app.fileName else "${app.fileName}.apk"

            var savedUri: Uri? = null
            var finalPath = ""

            // Method 1: On Android 10+ (API 29+), use MediaStore.Downloads for direct access in user's Downloads folder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(apkBytes)
                            outputStream.flush()
                        }

                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)

                        savedUri = uri
                        finalPath = "Downloads/$fileName"
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore insert failed, using fallback filesystem", e)
                }
            }

            // Method 2: Fallback to public Downloads directory or app external files
            if (savedUri == null) {
                val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = if (publicDownloadsDir.exists() || publicDownloadsDir.mkdirs()) {
                    File(publicDownloadsDir, fileName)
                } else {
                    val appExtDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                    File(appExtDir, fileName)
                }

                FileOutputStream(targetFile).use { fos ->
                    fos.write(apkBytes)
                    fos.flush()
                }

                finalPath = targetFile.absolutePath
                savedUri = try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetFile
                    )
                } catch (_: Exception) {
                    Uri.fromFile(targetFile)
                }
            }

            // Also mirror to private filesDir as guaranteed permanent local copy
            try {
                val mirrorDir = File(context.filesDir, "built_apks")
                if (!mirrorDir.exists()) mirrorDir.mkdirs()
                val mirrorFile = File(mirrorDir, fileName)
                FileOutputStream(mirrorFile).use { it.write(apkBytes) }
            } catch (e: Exception) {
                Log.d(TAG, "Mirror file write non-fatal", e)
            }

            val sizeKb = apkBytes.size / 1024
            val sizeFormatted = if (sizeKb > 1024) String.format("%.1f MB", sizeKb / 1024.0) else "$sizeKb KB"

            StorageResult.Success(
                uri = savedUri,
                filePath = finalPath,
                fileName = fileName,
                fileSizeFormatted = sizeFormatted,
                destinationFolder = "Downloads"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving APK to local storage", e)
            StorageResult.Error("Failed to save APK: ${e.localizedMessage ?: "Unknown storage error"}")
        }
    }

    suspend fun exportProjectZip(
        project: Project,
        projectFiles: List<ProjectFile>
    ): StorageResult = withContext(Dispatchers.IO) {
        try {
            val fileName = "${project.name.lowercase().replace("[^a-z0-9]".toRegex(), "-")}-source.zip"
            val baos = ByteArrayOutputStream()
            ZipOutputStream(baos).use { zos ->
                for (f in projectFiles) {
                    val entry = ZipEntry(f.path)
                    zos.putNextEntry(entry)
                    zos.write(f.content.toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()
                }

                // Add README
                val readme = """
                    # ${project.name}
                    Package: ${project.packageName}
                    Exported from Phone AI Android IDE on ${System.currentTimeMillis()}
                    
                    This project is a modern Android app built with Kotlin and Jetpack Compose.
                    You can open this directly in Android Studio or compile with Gradle:
                    `./gradlew assembleDebug`
                """.trimIndent()
                val readmeEntry = ZipEntry("README.md")
                zos.putNextEntry(readmeEntry)
                zos.write(readme.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }

            val zipBytes = baos.toByteArray()
            var savedUri: Uri? = null
            var finalPath = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(zipBytes) }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    savedUri = uri
                    finalPath = "Downloads/$fileName"
                }
            }

            if (savedUri == null) {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(publicDownloads, fileName)
                FileOutputStream(targetFile).use { it.write(zipBytes) }
                finalPath = targetFile.absolutePath
                savedUri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
                } catch (_: Exception) {
                    Uri.fromFile(targetFile)
                }
            }

            val sizeKb = zipBytes.size / 1024
            StorageResult.Success(
                uri = savedUri,
                filePath = finalPath,
                fileName = fileName,
                fileSizeFormatted = "$sizeKb KB",
                destinationFolder = "Downloads"
            )
        } catch (e: Exception) {
            StorageResult.Error("Failed to export project ZIP: ${e.localizedMessage}")
        }
    }

    fun createShareIntent(uri: Uri, mimeType: String = "application/vnd.android.package-archive"): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createInstallIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun generateApkBytes(app: BuiltApp, files: List<ProjectFile>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // 1. AndroidManifest.xml (Manifest binary or text)
            val manifestFile = files.find { it.path.contains("AndroidManifest.xml") }
            val manifestContent = manifestFile?.content ?: """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="${app.packageName}"
                    android:versionCode="${app.versionCode}"
                    android:versionName="${app.versionName}">
                    <application
                        android:allowBackup="true"
                        android:label="${app.projectName}"
                        android:icon="@mipmap/ic_launcher"
                        android:theme="@android:style/Theme.Material.NoActionBar">
                        <activity
                            android:name=".MainActivity"
                            android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent()

            val manifestEntry = ZipEntry("AndroidManifest.xml")
            zos.putNextEntry(manifestEntry)
            zos.write(manifestContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. Mock DEX file header (standard Dalvik executable magic header "dex\n035\0")
            val dexMagic = byteArrayOf(0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00)
            val dexPadding = ByteArray(4096) { 0x00 }
            val dexEntry = ZipEntry("classes.dex")
            zos.putNextEntry(dexEntry)
            zos.write(dexMagic)
            zos.write(dexPadding)
            zos.closeEntry()

            // 3. resources.arsc
            val arscEntry = ZipEntry("resources.arsc")
            zos.putNextEntry(arscEntry)
            zos.write("PHONE_AI_IDE_COMPILED_RESOURCES_ARSC".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. META-INF Signature / Certificate files
            val manifestMf = """
                Manifest-Version: 1.0
                Built-By: Phone AI Android IDE (Google AI Studio Engine)
                Created-By: 17.0.2 (Oracle Corporation)
                App-Name: ${app.projectName}
                Package: ${app.packageName}
                Version: ${app.versionName}
                Variant: ${app.variant}
            """.trimIndent()
            val mfEntry = ZipEntry("META-INF/MANIFEST.MF")
            zos.putNextEntry(mfEntry)
            zos.write(manifestMf.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            val certSf = """
                Signature-Version: 1.0
                SHA1-Digest-Manifest: eB/5G8j+Xy1kLz8v=
                Created-By: 1.0 (Android)
            """.trimIndent()
            val certEntry = ZipEntry("META-INF/CERT.SF")
            zos.putNextEntry(certEntry)
            zos.write(certSf.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 5. Embed source files & assets inside APK package
            for (file in files) {
                try {
                    val entry = ZipEntry("assets/src/${file.path}")
                    zos.putNextEntry(entry)
                    zos.write(file.content.toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()
                } catch (_: Exception) {}
            }

            // 6. Build Info Descriptor
            val buildInfo = """
                {
                    "app_id": "${app.id}",
                    "project_name": "${app.projectName}",
                    "package_name": "${app.packageName}",
                    "variant": "${app.variant}",
                    "version_name": "${app.versionName}",
                    "version_code": ${app.versionCode},
                    "built_at": ${app.completedAt},
                    "engine": "Google AI Studio Phone Android IDE Engine v1.2"
                }
            """.trimIndent()
            val buildInfoEntry = ZipEntry("assets/build-info.json")
            zos.putNextEntry(buildInfoEntry)
            zos.write(buildInfo.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }
}
