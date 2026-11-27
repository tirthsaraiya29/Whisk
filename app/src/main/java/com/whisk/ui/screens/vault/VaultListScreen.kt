package com.whisk.ui.screens.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisk.vault.VaultWorkspace
import com.whisk.ui.components.*
import com.whisk.vault.MultiVaultManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    modifier: Modifier = Modifier,
    multiVaultManager: MultiVaultManager,
    onVaultSelected: (VaultWorkspace) -> Unit,
) {
    val vaults by multiVaultManager.vaults.collectAsState()
    val activeVault by multiVaultManager.activeVault.collectAsState()

    Scaffold(
        topBar = {
            MinimalTopBar("Vaults", onBackClick = null) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Create new vault */
                }) {
                Icon(Icons.Default.Add, contentDescription = "Add Vault")
            }
        }
    ) {
        padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vaults) { vault ->
                VaultItem(
                    vault = vault,
                    isSelected = vault.id == activeVault.id,
                    onClick = { onVaultSelected(vault) }
                )
            }
        }
    }
}

@Composable
fun VaultItem(
    vault: VaultWorkspace,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = vault.icon.toIconVector(),
                contentDescription = vault.name,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = vault.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Helper to convert string icon name to IconVector
fun String.toIconVector() =
    Icons.Default.Folder