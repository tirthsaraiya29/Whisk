package com.whisk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.whisk.domain.model.DeveloperTemplates
import com.whisk.domain.model.Note
import com.whisk.ui.theme.DotMatrixFontFamily
import com.whisk.ui.theme.VercelMonoFontFamily

data class CommandItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val shortcut: String,
    val icon: ImageVector,
    val action: () -> Unit,
)

@Composable
fun CommandPalette(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onNewNote: () -> Unit,
    onExportBackup: () -> Unit,
    onCreateTemplateNote: ((Note) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }

    val commands = remember {
        listOf(
            CommandItem("new_note", "CREATE NEW NOTE", "Initialize a blank Markdown note in vault", "⌘N", Icons.Default.Add) {
                onNewNote()
                onDismiss()
            },
            CommandItem("daily_notes_workspace", "DAILY NOTES WORKSPACE", "Canonical daily notes workspace & activity log", "⌘D", Icons.Default.CalendarToday) {
                onNavigate("daily_notes")
                onDismiss()
            },
            CommandItem("database_views", "DATABASE VIEWS", "Table, Board, Calendar, Gallery & List views", "⌘V", Icons.Default.Description) {
                onNavigate("database_views")
                onDismiss()
            },
            CommandItem("adr_note", "ARCHITECTURE DECISION RECORD", "Document an architectural decision (ADR)", "⌘A", Icons.Default.Gavel) {
                onCreateTemplateNote?.invoke(DeveloperTemplates.createADR())
                onDismiss()
            },
            CommandItem("debug_journal", "DEBUGGING JOURNAL", "Document a bug investigation and fix", "⌘B", Icons.Default.BugReport) {
                onCreateTemplateNote?.invoke(DeveloperTemplates.createDebuggingJournal())
                onDismiss()
            },
            CommandItem("search", "SEARCH VAULT", "Full-text search across all notes", "⌘F", Icons.Default.Search) {
                onNavigate("search")
                onDismiss()
            },
            CommandItem("graph", "KNOWLEDGE GRAPH", "Interactive 2D graph visualization", "⌘G", Icons.Default.Hub) {
                onNavigate("graph")
                onDismiss()
            },
            CommandItem("canvas", "SPATIAL CANVAS", "Arrange ideas & note cards", "⌘S", Icons.Default.Description) {
                onNavigate("canvas")
                onDismiss()
            },
            CommandItem("ai", "AI WORKSPACE", "On-device Gemma 2B or remote LLM", "⌘AI", Icons.Default.AutoAwesome) {
                onNavigate("ai")
                onDismiss()
            },
            CommandItem("github", "GITHUB & MCP TOOLS", "Browse repos, issues & run MCP tools", "⌘GH", Icons.Default.Code) {
                onNavigate("github")
                onDismiss()
            },
            CommandItem("export", "EXPORT VAULT BACKUP", "Backup vault to ZIP format", "⌘E", Icons.Default.Backup) {
                onExportBackup()
                onDismiss()
            },
            CommandItem("settings", "SETTINGS", "Manage themes, AI keys & preferences", "⌘,", Icons.Default.Settings) {
                onNavigate("settings")
                onDismiss()
            },
        )
    }

    val filtered = commands.filter { (_, title, subtitle, shortcut) ->
        title.contains(searchQuery, ignoreCase = true) ||
                subtitle.contains(searchQuery, ignoreCase = true) ||
                shortcut.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp)),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "WHISK // COMMAND PALETTE",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "[ESC]",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "> TYPE COMMAND OR SHORTCUT...",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    textStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = DotMatrixFontFamily),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.id }) { cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    cmd.action()
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = cmd.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = cmd.title,
                                        fontFamily = DotMatrixFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = cmd.subtitle,
                                        fontFamily = VercelMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = cmd.shortcut,
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
