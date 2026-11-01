package com.whisk.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.local.PreferencesManager
import com.whisk.data.rag.embedding.EmbeddingModelManager
import com.whisk.data.rag.embedding.EmbeddingModelStatus
import com.whisk.data.rag.indexer.DocumentIndexer
import com.whisk.data.rag.indexer.IndexingProgress
import com.whisk.data.repository.ExportImportRepository
import com.whisk.domain.model.AiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStream

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val aiConfig: AiConfig = AiConfig(),
    val githubToken: String = "",
    val isExporting: Boolean = false,
    val importResultText: String? = null,
    val indexingProgress: IndexingProgress = IndexingProgress(),
    val embeddingStatus: EmbeddingModelStatus = EmbeddingModelStatus(),
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val exportImportRepository: ExportImportRepository,
    private val documentIndexer: DocumentIndexer? = null,
    private val embeddingModelManager: EmbeddingModelManager? = null,
) : ViewModel() {

    private val dummyProgress = MutableStateFlow(IndexingProgress())
    private val dummyEmbedding = MutableStateFlow(EmbeddingModelStatus())

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(preferencesManager.themeMode, preferencesManager.aiConfig, preferencesManager.githubToken) { theme, aiConfig, ghToken ->
            Triple(theme, aiConfig, ghToken)
        },
        combine(
            documentIndexer?.progress ?: dummyProgress,
            embeddingModelManager?.status ?: dummyEmbedding,
        ) { progress, embeddingStatus ->
            Pair(progress, embeddingStatus)
        },
    ) { (theme, aiConfig, ghToken), (progress, embeddingStatus) ->
        SettingsUiState(
            themeMode = theme,
            aiConfig = aiConfig,
            githubToken = ghToken,
            indexingProgress = progress,
            embeddingStatus = embeddingStatus,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun indexWorkspace(forceReindex: Boolean = false) {
        viewModelScope.launch {
            documentIndexer?.indexWorkspace(forceReindex = forceReindex)
        }
    }

    fun clearRagIndex() {
        viewModelScope.launch {
            documentIndexer?.clearIndex()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun saveAiConfig(config: AiConfig) {
        viewModelScope.launch {
            preferencesManager.saveAiConfig(config)
        }
    }

    fun saveGitHubToken(token: String) {
        viewModelScope.launch {
            preferencesManager.saveGitHubConfig(token, "")
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesManager.resetOnboarding()
        }
    }

    fun exportVault(outputStream: OutputStream) {
        viewModelScope.launch {
            exportImportRepository.exportVaultToZip(outputStream)
        }
    }

    fun importVault(uri: Uri, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = exportImportRepository.importVaultFromZip(uri)
            onComplete(count)
        }
    }
}
