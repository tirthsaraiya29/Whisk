package com.whisk.vault

import com.whisk.data.local.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import java.util.UUID
import kotlin.random.Random

class EnhancedMultiVaultManager(
    private val preferencesManager: PreferencesManager,
) {

    private val _vaults = MutableStateFlow<List<EnhancedVaultWorkspace>>(emptyList())
    val vaults: StateFlow<List<EnhancedVaultWorkspace>> = _vaults.asStateFlow()
    
    private val _activeVault = MutableStateFlow<EnhancedVaultWorkspace?>(null)
    val activeVault: StateFlow<EnhancedVaultWorkspace?> = _activeVault.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    init {
        scope.launch {
            val json = preferencesManager.savedVaultsJson.first()
            if (json.isNotBlank() && json != "[]") {
                try {
                    val loadedVaults = Json.decodeFromString<List<EnhancedVaultWorkspace>>(json)
                    _vaults.value = loadedVaults
                    val activeId = preferencesManager.activeVaultId.first()
                    _activeVault.value = loadedVaults.find { it.id == activeId } ?: loadedVaults.firstOrNull()
                } catch (e: Exception) {
                    initializeDefaultVaults()
                }
            } else {
                initializeDefaultVaults()
            }
        }
    }
    
    private suspend fun saveToPrefs() {
        val json = Json.encodeToString(_vaults.value)
        preferencesManager.saveVaultsJson(json)
        _activeVault.value?.id?.let {
            preferencesManager.saveActiveVaultId(it)
        }
    }
    
    private suspend fun initializeDefaultVaults() {
        val defaultVaults = listOf(
            EnhancedVaultWorkspace(
                id = "vault_personal",
                name = "PERSONAL",
                description = "Main personal knowledge vault",
                icon = "🏠",
                isActive = true,
                isDefault = true,
                permissions = VaultPermissions(
                    read = true,
                    write = true,
                    delete = false,
                    share = true,
                    export = true,
                    import = true,
                ),
            ),
            EnhancedVaultWorkspace(
                id = "vault_work",
                name = "WORK",
                description = "Projects, tasks, and meeting notes",
                icon = "💼",
                isActive = false,
                permissions = VaultPermissions(
                    read = true,
                    write = true,
                    delete = false,
                    share = false,
                    export = true,
                    import = true,
                ),
            ),
            EnhancedVaultWorkspace(
                id = "vault_research",
                name = "RESEARCH",
                description = "Papers, literature notes, and AI findings",
                icon = "🔬",
                isActive = false,
                permissions = VaultPermissions(
                    read = true,
                    write = true,
                    delete = false,
                    share = true,
                    export = true,
                    import = true,
                ),
            ),
            EnhancedVaultWorkspace(
                id = "vault_code",
                name = "CODE",
                description = "Snippets, ADRs, and technical architectures",
                icon = "💻",
                isActive = false,
                permissions = VaultPermissions(
                    read = true,
                    write = true,
                    delete = false,
                    share = false,
                    export = true,
                    import = true,
                ),
            ),
        )
        
        _vaults.value = defaultVaults
        _activeVault.value = defaultVaults.first()
        saveToPrefs()
    }
    
    fun switchVault(vaultId: String): Boolean {
        val targetVault = _vaults.value.find { it.id == vaultId } ?: return false
        
        // Deactivate current vault
        _vaults.value = _vaults.value.map { 
            it.copy(isActive = it.id == targetVault.id) 
        }
        
        // Update active vault
        _activeVault.value = targetVault
        
        scope.launch { saveToPrefs() }
        return true
    }
    
    fun createVault(
        name: String,
        description: String,
        icon: String = "📁",
        color: String = "#FFFFFF",
        isLocked: Boolean = false,
        password: String? = null,
        permissions: VaultPermissions = VaultPermissions(),
    ): EnhancedVaultWorkspace {
        val id = "vault_${UUID.randomUUID().toString().substring(0, 8)}"
        
        val newVault = EnhancedVaultWorkspace(
            id = id,
            name = name.uppercase(),
            description = description,
            icon = icon,
            color = color,
            isLocked = isLocked,
            password = password,
            permissions = permissions,
        )
        
        // Update vaults list
        _vaults.value = _vaults.value + newVault
        
        // If it's the first vault, make it active
        if (_vaults.value.size == 1) {
            switchVault(id)
        }
        
        scope.launch { saveToPrefs() }
        return newVault
    }
    
    fun updateVault(updatedVault: EnhancedVaultWorkspace): Boolean {
        val index = _vaults.value.indexOfFirst { it.id == updatedVault.id }
        return if (index != -1) {
            val updatedList = _vaults.value.toMutableList()
            updatedList[index] = updatedVault
            _vaults.value = updatedList
            scope.launch { saveToPrefs() }
            true
        } else {
            false
        }
    }
    
    fun deleteVault(vaultId: String): Boolean {
        val vault = _vaults.value.find { it.id == vaultId } ?: return false
        
        // Check if vault is empty
        if (vault.noteCount > 0) {
            return false // Can't delete non-empty vault
        }
        
        // Remove vault
        _vaults.value = _vaults.value.filter { it.id != vaultId }
        
        // If deleting active vault, switch to another one
        if (_activeVault.value?.id == vaultId) {
            val remainingVaults = _vaults.value
            if (remainingVaults.isNotEmpty()) {
                switchVault(remainingVaults.first().id)
            } else {
                _activeVault.value = null
            }
        }
        
        scope.launch { saveToPrefs() }
        return true
    }
    
    fun renameVault(vaultId: String, newName: String): Boolean {
        val vault = _vaults.value.find { it.id == vaultId } ?: return false
        
        val updatedVault = vault.copy(name = newName.uppercase())
        return updateVault(updatedVault)
    }
    
    fun updateVaultPermissions(vaultId: String, permissions: VaultPermissions): Boolean {
        val vault = _vaults.value.find { it.id == vaultId } ?: return false
        
        val updatedVault = vault.copy(permissions = permissions)
        return updateVault(updatedVault)
    }
    
    fun lockVault(vaultId: String, password: String): Boolean {
        val vault = _vaults.value.find { it.id == vaultId } ?: return false
        
        val updatedVault = vault.copy(
            isLocked = true,
            password = password,
        )
        return updateVault(updatedVault)
    }
    
    fun unlockVault(vaultId: String, password: String): Boolean {
        val vault = _vaults.value.find { it.id == vaultId } ?: return false
        
        if (vault.password != password) {
            return false
        }
        
        val updatedVault = vault.copy(
            isLocked = false,
            password = null,
        )
        return updateVault(updatedVault)
    }
    
    fun exportVault(vaultId: String): String? {
        val vault = _vaults.value.find { it.id == vaultId } ?: return null
        
        // In a real implementation, this would create a ZIP export
        return "export_${vaultId}_${System.currentTimeMillis()}.zip"
    }
    
    fun importVault(data: ByteArray): EnhancedVaultWorkspace? {
        // In a real implementation, this would parse the ZIP and create a vault
        return null
    }
    
    fun getVaultStats(vaultId: String): VaultUsageStats? {
        val vault = _vaults.value.find { it.id == vaultId } ?: return null
        
        return VaultUsageStats(
            totalNotes = vault.noteCount,
            totalSizeBytes = vault.sizeBytes,
            lastModified = vault.updatedAt,
            lastAccessed = vault.lastAccessed
        )
    }
}