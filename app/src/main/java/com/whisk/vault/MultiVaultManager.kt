package com.whisk.vault

import com.whisk.data.local.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VaultWorkspace(
    val id: String,
    val name: String,
    val description: String,
    val icon: String = "📁",
)

class MultiVaultManager(
    private val preferencesManager: PreferencesManager,
) {

    private val defaultVaults = listOf(
        VaultWorkspace("vault_personal", "PERSONAL", "Main personal knowledge vault", "🏠"),
        VaultWorkspace("vault_work", "WORK", "Projects, tasks, and meeting notes", "💼"),
        VaultWorkspace("vault_research", "RESEARCH", "Papers, literature notes, and AI findings", "🔬"),
        VaultWorkspace("vault_code", "CODE", "Snippets, ADRs, and technical architectures", "💻"),
    )

    private val _vaults = MutableStateFlow(defaultVaults)
    val vaults: StateFlow<List<VaultWorkspace>> = _vaults.asStateFlow()

    private val _activeVault = MutableStateFlow(defaultVaults[0])
    val activeVault: StateFlow<VaultWorkspace> = _activeVault.asStateFlow()

    fun switchVault(vaultId: String) {
        val target = _vaults.value.find { it.id == vaultId } ?: return
        _activeVault.value = target
    }

    fun createVault(name: String, description: String, icon: String = "📁"): VaultWorkspace {
        val id = "vault_" + name.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val newVault = VaultWorkspace(id = id, name = name.uppercase(), description = description, icon = icon)
        _vaults.value = _vaults.value + newVault
        _activeVault.value = newVault
        return newVault
    }
}
