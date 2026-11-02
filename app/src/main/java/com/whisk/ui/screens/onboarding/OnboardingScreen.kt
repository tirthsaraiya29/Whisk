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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.ui.theme.DotMatrixFontFamily
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
                title = "LOCAL-FIRST VAULT",
                subtitle = "WHISK // ARCHITECTURE",
                description = "Your personal knowledge graph stays on your device. Zero tracking, offline-first SQLite database, and full control over your notes.",
                icon = Icons.Default.Description,
                codeSnippet = "VAULT_STATUS: ENCRYPTED & LOCAL\nNOTES_LOADED: READY\nSTORAGE: ON-DEVICE ROOM DB",
            ),
            OnboardingStep(
                stepIndex = 2,
                title = "BI-DIRECTIONAL LINKS",
                subtitle = "WHISK // KNOWLEDGE GRAPH",
                description = "Connect your thoughts automatically. Type [[Note Title]] anywhere to create instant wikilinks and build your interactive knowledge network.",
                icon = Icons.Default.Hub,
                codeSnippet = "# Architecture Note\nReferencing [[Database Schema]] and\nlinking to [[RAG Embeddings]]...",
            ),
            OnboardingStep(
                stepIndex = 3,
                title = "STRUCTURED VIEWS",
                subtitle = "WHISK // OBJECT DATABASE",
                description = "Transform notes into structured database objects with custom properties. Switch seamlessly between Table, Board, Gallery, and Calendar views.",
                icon = Icons.Default.TableChart,
                codeSnippet = "OBJECT_TYPE: ProjectTask\nPROPERTIES: Priority, Status, DueDate\nVIEWS: Kanban Board, Grid, Table",
            ),
            OnboardingStep(
                stepIndex = 4,
                title = "VAULT INTELLIGENCE",
                subtitle = "WHISK // RAG & AI ASSISTANT",
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
            // Header / System Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "WHISK // INITIALIZATION",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            triggerComplete()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "SKIP [X]",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Central Interactive Carousel Card
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingStepTransition",
            ) { activeStep ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                ) {
                    // Step Counter & Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = activeStep.subtitle,
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "[0${activeStep.stepIndex} / 04]",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = activeStep.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = activeStep.title,
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = activeStep.description,
                        fontFamily = VercelMonoFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )

                    if (activeStep.codeSnippet != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                        ) {
                            Text(
                                text = activeStep.codeSnippet,
                                fontFamily = VercelMonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }
            }

            // Bottom Navigation & Progress Indicator
            Column(modifier = Modifier.fillMaxWidth()) {
                // Step Progress Dots / Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    steps.forEachIndexed { idx, _ ->
                        val isCurrent = idx == currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentStep > 0) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentStep--
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = "<- BACK",
                                fontFamily = DotMatrixFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentStep < steps.size - 1) {
                                    currentStep++
                                } else {
                                    triggerComplete()
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = if (currentStep == steps.size - 1) "LAUNCH VAULT ->" else "NEXT ->",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}
