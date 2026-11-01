package com.whisk.ui.screens.projects

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.domain.model.RelationshipType
import com.whisk.domain.model.TaskItem
import com.whisk.ui.components.MarkdownEditor
import com.whisk.ui.components.RelationshipSection
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.theme.DotMatrixFontFamily

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
    val tabs = listOf("OVERVIEW", "TASKS", "RESOURCES", "REPOSITORY", "AI COPILOT", "RELATIONSHIPS")

    var statusMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                WhiskTopBar(
                    title = "PROJECT // ${project.title.uppercase().ifBlank { "UNTITLED PROJECT" }}",
                    subtitle = "STATUS: ${project.status?.uppercase() ?: "DRAFT"} | PROGRESS: ${(state.progress * 100).toInt()}%",
                    onBackClick = onBackClick,
                    actions = {
                        Box {
                            OutlinedButton(
                                onClick = { statusMenuExpanded = true },
                                shape = RoundedCornerShape(0.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = project.status?.uppercase() ?: "DRAFT",
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = 11.sp,
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                            ) {
                                listOf("PLANNING", "IN_PROGRESS", "PAUSED", "COMPLETED", "CANCELLED").forEach { statusOption ->
                                    DropdownMenuItem(
                                        text = { Text(statusOption, fontFamily = DotMatrixFontFamily) },
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
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
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
                                height = 3.dp,
                            )
                        }
                    },
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTabIndex = index
                            },
                            text = {
                                Text(
                                    text = tabTitle,
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Crossfade(targetState = selectedTabIndex, label = "ProjectTabFade") { tabIndex ->
                when (tabIndex) {
                    0 -> OverviewTab(project, onUpdateContent)
                    1 -> TasksTab(state.tasks, onCreateTask, onToggleTask)
                    2 -> ResourcesTab(state.relationships, onOpenObject)
                    3 -> RepositoryTab(project, state, onUpdateRepoRef)
                    4 -> AiCopilotTab(state, onStartAiSession, onOpenAiSession)
                    5 -> RelationshipsTab(
                        state = state,
                        onAddRelationship = onAddRelationship,
                        onRemoveRelationship = onRemoveRelationship,
                        onQueryObjects = onQueryObjects,
                        onOpenObject = onOpenObject,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    project: Note,
    onUpdateContent: (title: String, description: String, body: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = project.description ?: "",
            onValueChange = { newDesc -> onUpdateContent(project.title, newDesc, project.body) },
            label = { Text("PROJECT DESCRIPTION / OBJECTIVE", fontFamily = DotMatrixFontFamily) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = DotMatrixFontFamily),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )

        MarkdownEditor(
            title = project.title,
            onTitleChange = { newTitle -> onUpdateContent(newTitle, project.description ?: "", project.body) },
            body = project.body,
            onBodyChange = { newBody -> onUpdateContent(project.title, project.description ?: "", newBody) },
            onLinkClick = { /* noop */ },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TasksTab(
    tasks: List<TaskItem>,
    onCreateTask: (String) -> Unit,
    onToggleTask: (taskId: String, status: Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var newTaskText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Add Task Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                placeholder = { Text("NEW ACTION ITEM / TASK...", fontFamily = DotMatrixFontFamily, fontSize = 12.sp) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = DotMatrixFontFamily),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.weight(1f),
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
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "NO ACTION ITEMS FOR THIS PROJECT. TYPE ABOVE TO ADD A TASK.",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { checked -> onToggleTask(task.id, checked) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Bold,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourcesTab(
    relationships: List<com.whisk.domain.model.RelationshipDetail>,
    onOpenObject: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val resourceTypes = setOf("note", "concept", "course", "book", "meeting")
    val resources = relationships.filter { it.otherObjectType in resourceTypes }

    if (resources.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "NO ATTACHED RESOURCES OR ARCHITECTURE NOTES. LINK OBJECTS IN RELATIONSHIPS TAB.",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DotMatrixFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(resources, key = { it.relationshipId }) { res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenObject(res.otherObjectId)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = res.otherObjectTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "TYPE: ${res.otherObjectType.uppercase()} | ${res.displayLabel.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryTab(
    project: Note,
    state: ProjectWorkspaceUiState,
    onUpdateRepoRef: (String) -> Unit,
) {
    var repoInput by remember(project.repoRef) { mutableStateOf(project.repoRef ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = repoInput,
                onValueChange = { repoInput = it },
                label = { Text("GITHUB REPOSITORY (owner/repository)", fontFamily = DotMatrixFontFamily) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = DotMatrixFontFamily),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onUpdateRepoRef(repoInput.trim()) },
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("LINK", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
            }
        }

        if (state.isLoadingRepo) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("FETCHING REPOSITORY DATA...", fontFamily = DotMatrixFontFamily)
            }
        } else if (state.linkedRepo != null) {
            val repo = state.linkedRepo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = repo.fullName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DotMatrixFontFamily,
                        fontWeight = FontWeight.Bold,
                    )
                }
                val desc = repo.description.orEmpty()
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Text(
                    text = "STARS: ${repo.stars} | DEFAULT BRANCH: ${repo.defaultBranch.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OPEN ISSUES (${state.repoIssues.size}) & PULL REQUESTS (${state.repoPullRequests.size})",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = DotMatrixFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.repoIssues) { issue ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            text = "#${issue.number} ${issue.title}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO GITHUB REPOSITORY LINKED. ENTER 'owner/repo' ABOVE TO CONNECT.",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AiCopilotTab(
    state: ProjectWorkspaceUiState,
    onStartAiSession: ((String) -> Unit) -> Unit,
    onOpenAiSession: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStartAiSession { sessionId -> onOpenAiSession(sessionId) }
            },
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("START AI SESSION IN PROJECT CONTEXT", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
        }

        if (state.aiSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO AI CONVERSATIONS ASSOCIATED WITH THIS PROJECT YET.",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.aiSessions, key = { it.id }) { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable { onOpenAiSession(session.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipsTab(
    state: ProjectWorkspaceUiState,
    onAddRelationship: (targetObjectId: String, type: RelationshipType) -> Unit,
    onRemoveRelationship: (relationshipId: String) -> Unit,
    onQueryObjects: suspend (String, String?) -> List<Note>,
    onOpenObject: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        RelationshipSection(
            relationships = state.relationships,
            objectTypes = state.objectTypes,
            onQueryObjects = onQueryObjects,
            onAddRelationship = onAddRelationship,
            onRemoveRelationship = onRemoveRelationship,
            onOpenObject = onOpenObject,
        )
    }
}
