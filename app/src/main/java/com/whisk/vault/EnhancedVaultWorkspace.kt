package com.whisk.vault

import java.util.UUID

@kotlinx.serialization.Serializable
data class VaultPermissions(
    val read: Boolean = true,
    val write: Boolean = true,
    val delete: Boolean = false,
    val share: Boolean = false,
    val export: Boolean = true,
    val import: Boolean = true,
    val manageUsers: Boolean = false,
    val manageSettings: Boolean = true,
)

sealed class PermissionType {
    object READ : PermissionType()
    object WRITE : PermissionType()
    object DELETE : PermissionType()
    object SHARE : PermissionType()
    object EXPORT : PermissionType()
    object IMPORT : PermissionType()
    object MANAGE_USERS : PermissionType()
    object MANAGE_SETTINGS : PermissionType()
}

@kotlinx.serialization.Serializable
data class EnhancedVaultWorkspace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val icon: String = "📁",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isLocked: Boolean = false,
    val password: String? = null,
    val maxNotes: Int = -1,
    val isReadOnly: Boolean = false,
    val tags: List<String> = emptyList(),
    val color: String = "#FFFFFF",
    val isDefault: Boolean = false,
    val noteCount: Int = 0,
    val sizeBytes: Long = 0,
    val lastAccessed: Long = 0,
    val syncEnabled: Boolean = false,
    val sharedWith: List<String> = emptyList(),
    val permissions: VaultPermissions = VaultPermissions(),
    val metadata: Map<String, String> = emptyMap(),
) {
    fun canCreateNote(): Boolean {
        return !isReadOnly && (maxNotes < 0 || noteCount < maxNotes)
    }
    
    fun canAccess(password: String?): Boolean {
        return !isLocked || this.password == password
    }
    
    fun canShare(): Boolean {
        return permissions.share
    }
    
    fun updateAccess() {
        // Use a mutable version for updating
    }
}
