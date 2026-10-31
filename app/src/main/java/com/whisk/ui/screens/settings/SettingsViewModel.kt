package com.whisk.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.local.PreferencesManager
import com.whisk.data.repository.ExportImportRepository
import com.whisk.domain.model.AiConfig
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
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val exportImportRepository: ExportImportRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.themeMode,
        preferencesManager.aiConfig,
        preferencesManager.githubToken,
    ) { theme, aiConfig, ghToken ->
        SettingsUiState(
            themeMode = theme,
            aiConfig = aiConfig,
            githubToken = ghToken,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

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
