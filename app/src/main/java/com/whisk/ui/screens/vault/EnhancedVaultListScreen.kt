package com.whisk.ui.screens.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisk.vault.EnhancedMultiVaultManager
import com.whisk.vault.EnhancedVaultWorkspace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedVaultListScreen(
    modifier: Modifier = Modifier,
    multiVaultManager: EnhancedMultiVaultManager,
    onVaultSelected: (EnhancedVaultWorkspace) -> Unit,
) {
    val vaults by multiVaultManager.vaults.collectAsState()
    val activeVault by multiVaultManager.activeVault.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vaults") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Create new vault */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
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
                EnhancedVaultItem(
                    vault = vault,
                    isSelected = vault.id == activeVault?.id,
                    onClick = { onVaultSelected(vault) }
                )
            }
        }
    }
}

@Composable
fun EnhancedVaultItem(
    vault: EnhancedVaultWorkspace,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 8.dp) else CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface,
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = vault.name,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
