package com.whisk.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whisk.domain.model.AiConfig
import com.whisk.domain.model.AiProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "whisk_preferences")

class PreferencesManager(private val context: Context) {

    private val secureStorage = SecureStorageManager(context)

    companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val KEY_HINT_LINKS_DISMISSED = booleanPreferencesKey("hint_links_dismissed")
        val KEY_HINT_GRAPH_DISMISSED = booleanPreferencesKey("hint_graph_dismissed")
        val KEY_HINT_AI_DISMISSED = booleanPreferencesKey("hint_ai_dismissed")
        val KEY_HINT_GITHUB_DISMISSED = booleanPreferencesKey("hint_github_dismissed")
        val KEY_HINT_CANVAS_DISMISSED = booleanPreferencesKey("hint_canvas_dismissed")
        val KEY_FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")

        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_AI_CUSTOM_ENDPOINT = stringPreferencesKey("ai_custom_endpoint")
        val KEY_AI_MODEL = stringPreferencesKey("ai_model")
        val KEY_AI_HF_MODEL_URL = stringPreferencesKey("ai_hf_model_url")
        val KEY_AI_USE_GPU = booleanPreferencesKey("ai_use_gpu")

        val KEY_GITHUB_DEFAULT_REPO = stringPreferencesKey("github_default_repo")
        val KEY_MCP_ENDPOINT = stringPreferencesKey("mcp_endpoint")

        private const val SECRET_AI_API_KEY = "secret_ai_api_key"
        private const val SECRET_HF_API_TOKEN = "secret_hf_api_token"
        private const val SECRET_GITHUB_TOKEN = "secret_github_token"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "SYSTEM"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = mode
        }
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !(prefs[KEY_FIRST_LAUNCH_DONE] ?: false)
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH_DONE] = true
        }
    }

    val hintsDismissed: Flow<Map<String, Boolean>> = context.dataStore.data.map { prefs ->
        mapOf(
            "links" to (prefs[KEY_HINT_LINKS_DISMISSED] ?: false),
            "graph" to (prefs[KEY_HINT_GRAPH_DISMISSED] ?: false),
            "ai" to (prefs[KEY_HINT_AI_DISMISSED] ?: false),
            "github" to (prefs[KEY_HINT_GITHUB_DISMISSED] ?: false),
            "canvas" to (prefs[KEY_HINT_CANVAS_DISMISSED] ?: false)
        )
    }

    suspend fun dismissHint(hintKey: String) {
        context.dataStore.edit { prefs ->
            when (hintKey) {
                "links" -> prefs[KEY_HINT_LINKS_DISMISSED] = true
                "graph" -> prefs[KEY_HINT_GRAPH_DISMISSED] = true
                "ai" -> prefs[KEY_HINT_AI_DISMISSED] = true
                "github" -> prefs[KEY_HINT_GITHUB_DISMISSED] = true
                "canvas" -> prefs[KEY_HINT_CANVAS_DISMISSED] = true
            }
        }
    }

    suspend fun resetOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH_DONE] = false
            prefs[KEY_HINT_LINKS_DISMISSED] = false
            prefs[KEY_HINT_GRAPH_DISMISSED] = false
            prefs[KEY_HINT_AI_DISMISSED] = false
            prefs[KEY_HINT_GITHUB_DISMISSED] = false
            prefs[KEY_HINT_CANVAS_DISMISSED] = false
        }
    }

    val aiConfig: Flow<AiConfig> = context.dataStore.data.map { prefs ->
        val providerStr = prefs[KEY_AI_PROVIDER] ?: AiProviderType.LOCAL_GEMMA.name
        val providerType = try {
            AiProviderType.valueOf(providerStr)
        } catch (e: Exception) {
            AiProviderType.LOCAL_GEMMA
        }
        val secretApiKey = secureStorage.getSecret(SECRET_AI_API_KEY)
        val secretHfToken = secureStorage.getSecret(SECRET_HF_API_TOKEN)

        AiConfig(
            providerType = providerType,
            apiKey = secretApiKey,
            customEndpoint = prefs[KEY_AI_CUSTOM_ENDPOINT] ?: "",
            selectedModel = prefs[KEY_AI_MODEL] ?: "gemma-4-E2B-it-gpu.litertlm",
            hfModelUrl = prefs[KEY_AI_HF_MODEL_URL] ?: "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-gpu.litertlm",
            hfApiToken = secretHfToken,
            useGpuAccelerator = prefs[KEY_AI_USE_GPU] ?: true
        )
    }

    suspend fun saveAiConfig(config: AiConfig) {
        secureStorage.saveSecret(SECRET_AI_API_KEY, config.apiKey)
        secureStorage.saveSecret(SECRET_HF_API_TOKEN, config.hfApiToken)

        context.dataStore.edit { prefs ->
            prefs[KEY_AI_PROVIDER] = config.providerType.name
            prefs[KEY_AI_CUSTOM_ENDPOINT] = config.customEndpoint
            prefs[KEY_AI_MODEL] = config.selectedModel
            prefs[KEY_AI_HF_MODEL_URL] = config.hfModelUrl
            prefs[KEY_AI_USE_GPU] = config.useGpuAccelerator
        }
    }

    val githubToken: Flow<String> = context.dataStore.data.map { _ ->
        secureStorage.getSecret(SECRET_GITHUB_TOKEN)
    }

    val githubDefaultRepo: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GITHUB_DEFAULT_REPO] ?: ""
    }

    suspend fun saveGitHubConfig(token: String, defaultRepo: String) {
        secureStorage.saveSecret(SECRET_GITHUB_TOKEN, token)
        context.dataStore.edit { prefs ->
            prefs[KEY_GITHUB_DEFAULT_REPO] = defaultRepo
        }
    }

    val mcpEndpoint: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MCP_ENDPOINT] ?: "http://localhost:8080/mcp"
    }

    suspend fun saveMcpEndpoint(endpoint: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MCP_ENDPOINT] = endpoint
        }
    }
}
