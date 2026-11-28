package com.whisk.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.AiConfig
import com.whisk.domain.model.AiProviderType
import com.whisk.ui.components.WhiskTopBar

enum class SettingsSubCategory {
    NONE, GENERAL, APPEARANCE, INTERFACE, EDITOR, FILES_LINKS, KEYCHAIN, CORE_PLUGINS, AI_ENGINE, VAULT_HEALTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChange: (String) -> Unit,
    onSaveAiConfig: (AiConfig) -> Unit,
    onSaveGitHubToken: (String) -> Unit,
    onResetOnboarding: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onIndexWorkspace: ((force: Boolean) -> Unit)? = null,
    onClearRagIndex: (() -> Unit)? = null,
    onNavigateToProperties: (() -> Unit)? = null,
    onNavigateToTypes: (() -> Unit)? = null,
    onRunVaultHealthScanner: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var activeSubCategory by remember { mutableStateOf(SettingsSubCategory.NONE) }

    // Draft AI Config state
    var apiKeyText by rememberSaveable(state.aiConfig) { mutableStateOf(state.aiConfig.apiKey) }
    var githubTokenText by rememberSaveable(state.githubToken) { mutableStateOf(state.githubToken) }
    var selectedProvider by rememberSaveable(state.aiConfig) { mutableStateOf(state.aiConfig.providerType) }
    var hfModelUrlText by rememberSaveable(state.aiConfig) { mutableStateOf(state.aiConfig.hfModelUrl) }
    var hfTokenText by rememberSaveable(state.aiConfig) { mutableStateOf(state.aiConfig.hfApiToken) }
    var useGpu by rememberSaveable(state.aiConfig) { mutableStateOf(state.aiConfig.useGpuAccelerator) }
    var temperatureVal by rememberSaveable(state.aiConfig) { mutableFloatStateOf(state.aiConfig.temperature) }

    var showApiKeySecret by remember { mutableStateOf(false) }
    var showGithubTokenSecret by remember { mutableStateOf(false) }

    fun matchesQuery(vararg terms: String): Boolean {
        if (searchQuery.isBlank()) return true
        return terms.any { it.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "Settings",
                subtitle = "Options, Plugins & AI Engine",
                onMenuClick = onMenuClick,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Search Input Bar (Obsidian Screenshot)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search settings...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                singleLine = true,
            )

            // Section 1: Options (Obsidian Screenshot)
            if (matchesQuery("Options", "General", "Appearance", "Interface", "Editor", "Files and links", "Keychain", "Security")) {
                Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        if (matchesQuery("General", "Onboarding", "Reset")) {
                            SettingsNavigationRow(
                                icon = Icons.Default.AccountCircle,
                                title = "General",
                                subtitle = "Reset onboarding & vault defaults",
                                onClick = { activeSubCategory = SettingsSubCategory.GENERAL },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        if (matchesQuery("Appearance", "Theme", "Light", "Dark", "Accent")) {
                            SettingsNavigationRow(
                                icon = Icons.Default.Palette,
                                title = "Appearance",
                                subtitle = "System, Charcoal Dark, Paper Light themes",
                                onClick = { activeSubCategory = SettingsSubCategory.APPEARANCE },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        if (matchesQuery("Interface", "Command Palette", "Navigation")) {
                            SettingsNavigationRow(
                                icon = Icons.Default.Smartphone,
                                title = "Interface",
                                subtitle = "Layout, shortcuts & command palette",
                                onClick = { activeSubCategory = SettingsSubCategory.INTERFACE },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        if (matchesQuery("Editor", "Markdown", "Formatting", "Live Preview")) {
                            SettingsNavigationRow(
                                icon = Icons.Default.Edit,
                                title = "Editor",
                                subtitle = "Live Preview, syntax formatting & brackets",
                                onClick = { activeSubCategory = SettingsSubCategory.EDITOR },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        if (matchesQuery("Files and links", "Vault", "Import", "Export", "ZIP")) {
                            SettingsNavigationRow(
                                icon = Icons.Default.FolderSpecial,
                                title = "Files and links",
                                subtitle = "Vault backup ZIP import/export",
                                onClick = { activeSubCategory = SettingsSubCategory.FILES_LINKS },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        if (matchesQuery("Keychain", "Security", "Biometrics", "GitHub Token")) {
                            SettingsNavigationRow(
                                icon = Icons.Default.Key,
                                title = "Keychain & Security",
                                subtitle = "Biometric lock & GitHub credentials",
                                onClick = { activeSubCategory = SettingsSubCategory.KEYCHAIN },
                            )
                        }
                    }
                }
            }

            // Section 2: Core Plugins & Features (Obsidian Screenshot)
            if (matchesQuery("Core plugins", "Backlinks", "Canvas", "Daily notes", "Graph view", "Properties")) {
                Text(
                    text = "Core plugins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        SettingsNavigationRow(
                            icon = Icons.Default.Hub,
                            title = "Backlinks & Mentions",
                            subtitle = "Automatic wikilink & unlinked mention detection",
                            onClick = { activeSubCategory = SettingsSubCategory.CORE_PLUGINS },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsNavigationRow(
                            icon = Icons.Default.Description,
                            title = "Spatial Canvas",
                            subtitle = "Infinite canvas cards & diagram nodes",
                            onClick = { activeSubCategory = SettingsSubCategory.CORE_PLUGINS },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsNavigationRow(
                            icon = Icons.Default.CalendarToday,
                            title = "Daily Notes",
                            subtitle = "Canonical daily logs & activity tracking",
                            onClick = { activeSubCategory = SettingsSubCategory.CORE_PLUGINS },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsNavigationRow(
                            icon = Icons.Default.TableChart,
                            title = "Properties & Object Types",
                            subtitle = "Manage custom schemas & object definitions",
                            onClick = {
                                if (onNavigateToProperties != null) onNavigateToProperties()
                                else activeSubCategory = SettingsSubCategory.CORE_PLUGINS
                            },
                        )
                    }
                }
            }

            // Section 3: AI Engine & Vault RAG Intelligence
            if (matchesQuery("AI", "Intelligence", "Gemma", "OpenAI", "RAG", "Vectors")) {
                Text(
                    text = "AI Engine & RAG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        SettingsNavigationRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "AI Model & Provider Config",
                            subtitle = "${state.aiConfig.providerType.name} • On-Device Gemma & Cloud LLMs",
                            onClick = { activeSubCategory = SettingsSubCategory.AI_ENGINE },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsNavigationRow(
                            icon = Icons.Default.Psychology,
                            title = "Vault RAG Vector Index",
                            subtitle = "Semantic search vectors & indexing status",
                            onClick = { activeSubCategory = SettingsSubCategory.AI_ENGINE },
                        )
                    }
                }
            }

            // Section 4: Vault Health & Diagnostics
            if (matchesQuery("Health", "Scanner", "Backup", "Diagnostics")) {
                Text(
                    text = "Vault Health & Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        SettingsNavigationRow(
                            icon = Icons.Default.HealthAndSafety,
                            title = "Vault Health Scanner",
                            subtitle = "Run diagnostics for orphan notes & broken links",
                            onClick = {
                                if (onRunVaultHealthScanner != null) onRunVaultHealthScanner()
                                else activeSubCategory = SettingsSubCategory.VAULT_HEALTH
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsNavigationRow(
                            icon = Icons.Default.Backup,
                            title = "Backup Vault (Export ZIP)",
                            subtitle = "Export encrypted or plain Markdown archive",
                            onClick = { onExportBackup() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsNavigationRow(
                            icon = Icons.Default.Refresh,
                            title = "Restore Vault (Import ZIP)",
                            subtitle = "Restore notes and properties from backup ZIP",
                            onClick = { onImportBackup() },
                        )
                    }
                }
            }
        }
    }

    // Modal Sub-Category Settings Detail Sheets
    if (activeSubCategory != SettingsSubCategory.NONE) {
        ModalBottomSheet(
            onDismissRequest = { activeSubCategory = SettingsSubCategory.NONE },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (activeSubCategory) {
                            SettingsSubCategory.GENERAL -> "General Options"
                            SettingsSubCategory.APPEARANCE -> "Appearance Options"
                            SettingsSubCategory.INTERFACE -> "Interface Options"
                            SettingsSubCategory.EDITOR -> "Editor Options"
                            SettingsSubCategory.FILES_LINKS -> "Files & Links Options"
                            SettingsSubCategory.KEYCHAIN -> "Keychain & Security"
                            SettingsSubCategory.CORE_PLUGINS -> "Core Plugins Config"
                            SettingsSubCategory.AI_ENGINE -> "AI Engine & RAG Config"
                            SettingsSubCategory.VAULT_HEALTH -> "Vault Health & Diagnostics"
                            else -> "Settings Detail"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    IconButton(
                        onClick = { activeSubCategory = SettingsSubCategory.NONE },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Detail")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (activeSubCategory) {
                    SettingsSubCategory.APPEARANCE -> {
                        Text("Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("SYSTEM" to "System", "DARK" to "Charcoal Dark", "LIGHT" to "Paper Light").forEach { (modeKey, modeName) ->
                                val isSelected = state.themeMode == modeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            RoundedCornerShape(10.dp),
                                        )
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onThemeChange(modeKey)
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = modeName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    SettingsSubCategory.AI_ENGINE -> {
                        Text("AI Provider Selection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AiProviderType.entries.forEach { provider ->
                                val isSelected = selectedProvider == provider
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(10.dp),
                                        )
                                        .clickable { selectedProvider = provider }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(provider.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    if (isSelected) {
                                        Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedProvider != AiProviderType.LOCAL_GEMMA) {
                            Text("API Key", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = apiKeyText,
                                onValueChange = { apiKeyText = it },
                                placeholder = { Text("sk-...") },
                                singleLine = true,
                                visualTransformation = if (showApiKeySecret) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKeySecret = !showApiKeySecret }) {
                                        Icon(if (showApiKeySecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Temperature: ${(temperatureVal * 10).toInt() / 10f}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Slider(
                            value = temperatureVal,
                            onValueChange = { temperatureVal = it },
                            valueRange = 0f..1f,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onSaveAiConfig(
                                    state.aiConfig.copy(
                                        providerType = selectedProvider,
                                        apiKey = apiKeyText,
                                        hfModelUrl = hfModelUrlText,
                                        hfApiToken = hfTokenText,
                                        useGpuAccelerator = useGpu,
                                        temperature = temperatureVal,
                                    ),
                                )
                                activeSubCategory = SettingsSubCategory.NONE
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Save AI Configuration")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (onIndexWorkspace != null) {
                            OutlinedButton(
                                onClick = { onIndexWorkspace(true) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Force Re-index RAG Vectors")
                            }
                        }
                    }

                    SettingsSubCategory.KEYCHAIN -> {
                        Text("GitHub Personal Access Token", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = githubTokenText,
                            onValueChange = { githubTokenText = it },
                            placeholder = { Text("ghp_...") },
                            singleLine = true,
                            visualTransformation = if (showGithubTokenSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGithubTokenSecret = !showGithubTokenSecret }) {
                                    Icon(if (showGithubTokenSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onSaveGitHubToken(githubTokenText)
                                activeSubCategory = SettingsSubCategory.NONE
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Save GitHub Key")
                        }
                    }

                    SettingsSubCategory.GENERAL -> {
                        Text("First Launch Setup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onResetOnboarding()
                                activeSubCategory = SettingsSubCategory.NONE
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Re-run First Launch Onboarding")
                        }
                    }

                    else -> {
                        Text("Configured according to current vault preferences.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
