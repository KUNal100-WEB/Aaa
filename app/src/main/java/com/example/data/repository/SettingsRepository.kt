package com.example.data.repository

import android.content.Context
import com.example.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SettingsRepository {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    init {
        loadSettings()
    }

    companion object {
        private var appContext: Context? = null

        fun init(context: Context) {
            appContext = context.applicationContext
            instance.loadSettings()
        }

        val instance: SettingsRepository by lazy { SettingsRepository() }
    }

    fun loadSettings() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "app_settings.json")
            if (file.exists()) {
                val jsonStr = file.readText()
                val json = JSONObject(jsonStr)
                _settings.value = AppSettings(
                    selectedModel = json.optString("selectedModel", "Gemini 2.5 Flash (Recommended)"),
                    useCustomAi = json.optBoolean("useCustomAi", false),
                    customAiEndpoint = json.optString("customAiEndpoint", ""),
                    customAiApiKey = json.optString("customAiApiKey", ""),
                    customAiModelName = json.optString("customAiModelName", "gemini-2.5-flash"),
                    firebaseEnabled = json.optBoolean("firebaseEnabled", true),
                    cloudBuildSimulation = json.optBoolean("cloudBuildSimulation", true),
                    darkBlueTheme = json.optBoolean("darkBlueTheme", true)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "app_settings.json")
            val json = JSONObject().apply {
                put("selectedModel", newSettings.selectedModel)
                put("useCustomAi", newSettings.useCustomAi)
                put("customAiEndpoint", newSettings.customAiEndpoint)
                put("customAiApiKey", newSettings.customAiApiKey)
                put("customAiModelName", newSettings.customAiModelName)
                put("firebaseEnabled", newSettings.firebaseEnabled)
                put("cloudBuildSimulation", newSettings.cloudBuildSimulation)
                put("darkBlueTheme", newSettings.darkBlueTheme)
            }
            file.writeText(json.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun testCustomAiConnection(
        endpoint: String,
        apiKey: String,
        modelName: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (endpoint.isBlank()) {
            return@withContext Pair(false, "API link cannot be empty.")
        }
        val startTime = System.currentTimeMillis()
        try {
            val targetUrl = endpoint.trim()
            val mediaType = "application/json; charset=utf-8".toMediaType()

            // Prepare standard OpenAI / Gemini compatible request body
            val requestJson = JSONObject().apply {
                put("model", if (modelName.isNotBlank()) modelName.trim() else "gemini-2.5-flash")
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are an Android IDE AI code assistant. Respond with 'PONG' to verify connection.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                }
                put("messages", messages)
                put("max_tokens", 50)
            }

            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .post(requestJson.toString().toRequestBody(mediaType))

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
                requestBuilder.addHeader("x-goog-api-key", apiKey.trim())
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Pair(true, "Connected successfully! (HTTP ${response.code}, ${latency}ms). Model verified.")
            } else {
                Pair(false, "API returned HTTP ${response.code}: ${body.take(120)}")
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Pair(false, "Connection error (${latency}ms): ${e.localizedMessage ?: e.message}")
        }
    }
}
