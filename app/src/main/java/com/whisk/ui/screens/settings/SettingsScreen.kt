package com.whisk.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.whisk.domain.model.AiConfig
import com.whisk.domain.model.AiProviderType
import com.whisk.ui.components.WhiskTopBar

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChange: (String) -> Unit,
    onSaveAiConfig: (AiConfig) -> Unit,
    onSaveGitHubToken: (String) -> Unit,
    onResetOnboarding: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    var apiKeyText by remember(state.aiConfig) { mutableStateOf(state.aiConfig.apiKey) }
    var githubTokenText by remember(state.githubToken) { mutableStateOf(state.githubToken) }
    var selectedProvider by remember(state.aiConfig) { mutableStateOf(state.aiConfig.providerType) }
    var hfModelUrlText by remember(state.aiConfig) { mutableStateOf(state.aiConfig.hfModelUrl) }
    var hfTokenText by remember(state.aiConfig) { mutableStateOf(state.aiConfig.hfApiToken) }
    var useGpu by remember(state.aiConfig) { mutableStateOf(state.aiConfig.useGpuAccelerator) }

    var showApiKeySecret by remember { mutableStateOf(false) }
    var showHfTokenSecret by remember { mutableStateOf(false) }
    var showGithubTokenSecret by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WhiskTopBar(title = "Settings", subtitle = "Preferences, AI keys, themes & backups")
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    listOf("SYSTEM" to "System Default", "LIGHT" to "Minimal Light", "DARK" to "Charcoal Dark").forEach { (modeKey, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeChange(modeKey) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.themeMode == modeKey,
                                onClick = { onThemeChange(modeKey) }
                            )
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            // AI Provider Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "AI Model & Provider",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AiProviderType.entries.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProvider = provider
                                    onSaveAiConfig(state.aiConfig.copy(providerType = provider))
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProvider == provider,
                                onClick = {
                                    selectedProvider = provider
                                    onSaveAiConfig(state.aiConfig.copy(providerType = provider))
                                }
                            )
                            Text(text = provider.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    if (selectedProvider == AiProviderType.LOCAL_GEMMA) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = hfModelUrlText,
                            onValueChange = {
                                hfModelUrlText = it
                                onSaveAiConfig(state.aiConfig.copy(hfModelUrl = it))
                            },
                            label = { Text("Hugging Face Model Direct URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = hfTokenText,
                            onValueChange = {
                                hfTokenText = it
                                onSaveAiConfig(state.aiConfig.copy(hfApiToken = it))
                            },
                            label = { Text("Hugging Face User Access Token (Encrypted Secret)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showHfTokenSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showHfTokenSecret = !showHfTokenSecret }) {
                                    Icon(
                                        imageVector = if (showHfTokenSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle HF Token Secret Visibility"
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GPU Accelerator Delegate", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = useGpu,
                                onCheckedChange = {
                                    useGpu = it
                                    onSaveAiConfig(state.aiConfig.copy(useGpuAccelerator = it))
                                }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = {
                                apiKeyText = it
                                onSaveAiConfig(state.aiConfig.copy(providerType = selectedProvider, apiKey = it))
                            },
                            label = { Text("API Key for ${selectedProvider.name} (Encrypted Secret)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showApiKeySecret) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showApiKeySecret = !showApiKeySecret }) {
                                    Icon(
                                        imageVector = if (showApiKeySecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle API Key Secret Visibility"
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // GitHub Access Token Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "GitHub Credentials",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = githubTokenText,
                        onValueChange = {
                            githubTokenText = it
                            onSaveGitHubToken(it)
                        },
                        label = { Text("GitHub Fine-Grained Access Token (Encrypted Secret)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showGithubTokenSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showGithubTokenSecret = !showGithubTokenSecret }) {
                                Icon(
                                    imageVector = if (showGithubTokenSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle GitHub Token Secret Visibility"
                                )
                            }
                        }
                    )
                }
            }

            // Backup, Restore & FTUE Onboarding Reset
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Data Vault & Reset",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onExportBackup,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Export ZIP Backup")
                        }
                        Button(
                            onClick = onImportBackup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import Notes")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onResetOnboarding,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Reset First-Launch Tips")
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
