package com.whisk.ui.screens.projects

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whisk.data.local.ChatSessionEntity
import com.whisk.domain.model.Note
import com.whisk.domain.model.RelationshipDetail
import com.whisk.domain.model.RelationshipType
import com.whisk.domain.model.TaskItem
import com.whisk.ui.components.MarkdownEditor
import com.whisk.ui.components.RelationshipSection
import com.whisk.ui.components.WhiskTopBar

@Composable
fun ProjectWorkspaceScreen(
    state: ProjectWorkspaceUiState,
    onBackClick: () -> Unit,
    onUpdateContent: (title: String, description: String, body: String) -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUpdateRepoRef: (String) -> Unit,
    onCreateTask: (String) -> Unit,
    onToggleTask: (taskId: String, status: Boolean) -> Unit,
    onAddRelationship: (targetObjectId: String, type: RelationshipType) -> Unit,
    onRemoveRelationship: (relationshipId: String) -> Unit,
    onQueryObjects: suspend (String, String?) -> List<Note>,
    onOpenObject: (String) -> Unit,
    onStartAiSession: ((String) -> Unit) -> Unit,
    onOpenAiSession: (String) -> Unit,
) {
    val project = state.project ?: return
    val haptic = LocalHapticFeedback.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Tasks", "Resources", "Repository", "AI Copilot", "Relations")

    var statusMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                WhiskTopBar(
                    title = "Project • ${project.title.ifBlank { "Untitled Project" }}",
                    subtitle = "Status: ${project.status ?: "Draft"} | Progress: ${(state.progress * 100).toInt()}%",
                    onBackClick = onBackClick,
                    actions = {
                        Box {
                            OutlinedButton(
                                onClick = { statusMenuExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = project.status ?: "Draft",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false },
                            ) {
                                listOf("PLANNING", "IN_PROGRESS", "PAUSED", "COMPLETED", "CANCELLED").forEach { statusOption ->
                                    DropdownMenuItem(
                                        text = { Text(statusOption, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            statusMenuExpanded = false
                                            onUpdateStatus(statusOption)
                                        },
                                    )
                                }
                            }
                        }
                    },
                )

                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Tab Header Bar
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTabIndex = index
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Crossfade(targetState = selectedTabIndex, label = "ProjectTabAnimation") { tab ->
                when (tab) {
                    0 -> ProjectOverviewTab(
                        title = project.title,
                        description = project.description ?: "",
                        body = project.body,
                        onUpdateContent = onUpdateContent,
                        onOpenObject = onOpenObject,
                    )
                    1 -> ProjectTasksTab(
                        tasks = state.tasks,
                        onCreateTask = onCreateTask,
                        onToggleTask = onToggleTask,
                    )
                    2 -> ProjectResourcesTab(
                        linkedRelationships = state.relationships,
                        onOpenObject = onOpenObject,
                    )
                    3 -> ProjectRepoTab(
                        repoRef = state.linkedRepo?.fullName ?: "",
                        onUpdateRepoRef = onUpdateRepoRef,
                    )
                    4 -> ProjectAiCopilotTab(
                        aiSessions = state.aiSessions,
                        onStartAiSession = { onStartAiSession { sessionId -> onOpenAiSession(sessionId) } },
                        onOpenAiSession = onOpenAiSession,
                    )
                    5 -> RelationshipSection(
                        relationships = state.relationships,
                        objectTypes = state.objectTypes,
                        onQueryObjects = onQueryObjects,
                        onAddRelationship = onAddRelationship,
                        onRemoveRelationship = onRemoveRelationship,
                        onOpenObject = onOpenObject,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectOverviewTab(
    title: String,
    description: String,
    body: String,
    onUpdateContent: (String, String, String) -> Unit,
    onOpenObject: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = description,
            onValueChange = { newDesc -> onUpdateContent(title, newDesc, body) },
            placeholder = { Text("Project Summary & Objective...", style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
        )

        MarkdownEditor(
            title = title,
            onTitleChange = { newTitle -> onUpdateContent(newTitle, description, body) },
            body = body,
            onBodyChange = { newBody -> onUpdateContent(title, description, newBody) },
            onLinkClick = onOpenObject,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ProjectTasksTab(
    tasks: List<TaskItem>,
    onCreateTask: (String) -> Unit,
    onToggleTask: (String, Boolean) -> Unit,
) {
    var newTaskText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                placeholder = { Text("Add new project task...", style = MaterialTheme.typography.bodyMedium) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (newTaskText.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCreateTask(newTaskText)
                        newTaskText = ""
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tasks, key = { it.id }) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { chk -> onToggleTask(task.id, chk) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectResourcesTab(
    linkedRelationships: List<RelationshipDetail>,
    onOpenObject: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Linked Notes & Relations (${linkedRelationships.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(linkedRelationships, key = { it.relationshipId }) { rel ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenObject(rel.otherObjectId) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = rel.otherObjectTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Relation: ${rel.displayLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRepoTab(
    repoRef: String,
    onUpdateRepoRef: (String) -> Unit,
) {
    var textVal by remember(repoRef) { mutableStateOf(repoRef) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Connected GitHub Repository",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = textVal,
            onValueChange = {
                textVal = it
                onUpdateRepoRef(it)
            },
            label = { Text("Repository (e.g. owner/repo)") },
            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun ProjectAiCopilotTab(
    aiSessions: List<ChatSessionEntity>,
    onStartAiSession: () -> Unit,
    onOpenAiSession: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Project AI Copilot Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Button(
                onClick = onStartAiSession,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start AI Session", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(aiSessions, key = { it.id }) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenAiSession(session.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
