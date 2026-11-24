package com.whisk.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whisk.ui.theme.VercelMonoFontFamily

data class OnboardingStep(
    val stepIndex: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val codeSnippet: String? = null,
)

@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var currentStep by remember { mutableIntStateOf(0) }
    var isCompleting by remember { mutableStateOf(false) }

    val triggerComplete = {
        if (!isCompleting) {
            isCompleting = true
            onCompleteOnboarding()
        }
    }

    val steps = remember {
        listOf(
            OnboardingStep(
                stepIndex = 1,
                title = "Local-First Vault",
                subtitle = "Architecture & Privacy",
                description = "Your personal knowledge graph stays on your device. Zero tracking, offline-first SQLite database, and full control over your notes.",
                icon = Icons.Default.Description,
                codeSnippet = "VAULT_STATUS: Encrypted & Local\nNOTES_LOADED: Ready\nSTORAGE: On-Device Room Database",
            ),
            OnboardingStep(
                stepIndex = 2,
                title = "Bi-Directional Links",
                subtitle = "Knowledge Network",
                description = "Connect your thoughts automatically. Type [[Note Title]] anywhere to create instant wikilinks and build your interactive knowledge network.",
                icon = Icons.Default.Hub,
                codeSnippet = "# Architecture Note\nReferencing [[Database Schema]] and\nlinking to [[RAG Embeddings]]...",
            ),
            OnboardingStep(
                stepIndex = 3,
                title = "Structured Views",
                subtitle = "Object Database",
                description = "Transform notes into structured database objects with custom properties. Switch seamlessly between Table, Board, Gallery, and Calendar views.",
                icon = Icons.Default.TableChart,
                codeSnippet = "OBJECT_TYPE: ProjectTask\nPROPERTIES: Priority, Status, DueDate\nVIEWS: Kanban Board, Grid, Table",
            ),
            OnboardingStep(
                stepIndex = 4,
                title = "Vault Intelligence",
                subtitle = "RAG & AI Assistant",
                description = "Ask questions directly to your vault. On-device local Gemma 2B or remote AI uses vector embeddings to answer from your notes.",
                icon = Icons.Default.AutoAwesome,
                codeSnippet = "> Ask Vault: \"Summarize my notes on RAG\"\n[RAG: Searching 12 chunks...]\nAnswer: Your local vector index holds...",
            ),
        )
    }

    val step = steps[currentStep]

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Welcome to Whisk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { triggerComplete() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Step Content
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingStepAnimation",
            ) { currentStepData ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = currentStepData.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = currentStepData.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentStepData.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentStepData.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    if (currentStepData.codeSnippet != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Text(
                                text = currentStepData.codeSnippet,
                                fontFamily = VercelMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }
            }

            // Bottom Navigation & Progress Dots
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    steps.forEachIndexed { idx, _ ->
                        val isSelected = idx == currentStep
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        )
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentStep < steps.size - 1) {
                            currentStep++
                        } else {
                            triggerComplete()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text(
                        text = if (currentStep == steps.size - 1) "Get Started" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
