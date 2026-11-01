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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.BuiltInObjectTypes
import com.whisk.domain.model.Note
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.ui.theme.DotMatrixFontFamily

fun getIconForObjectType(typeId: String?): ImageVector {
    return when (typeId) {
        BuiltInObjectTypes.PROJECT -> Icons.Default.FolderSpecial
        BuiltInObjectTypes.PERSON -> Icons.Default.Person
        BuiltInObjectTypes.BOOK -> Icons.Default.Book
        BuiltInObjectTypes.COURSE -> Icons.Default.School
        BuiltInObjectTypes.REPOSITORY -> Icons.Default.Code
        BuiltInObjectTypes.CONCEPT -> Icons.Default.Lightbulb
        BuiltInObjectTypes.TASK -> Icons.Default.CheckCircle
        BuiltInObjectTypes.MEETING -> Icons.Default.Event
        else -> Icons.Default.Description
    }
}

@Composable
fun ObjectPickerModalDialog(
    title: String = "SELECT TARGET OBJECT",
    objectTypes: List<ObjectTypeDefinition>,
    onQueryObjects: suspend (String, String?) -> List<Note>,
    onDismiss: () -> Unit,
    onSelectObject: (Note) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var query by remember { mutableStateOf("") }
    var selectedTypeId by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<Note>>(emptyList()) }

    LaunchedEffect(query, selectedTypeId) {
        searchResults = onQueryObjects(query, selectedTypeId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = DotMatrixFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
            ) {
                // Search Input
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            "> SEARCH OBJECTS BY TITLE...",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                // Filter by Type Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    item {
                        val isAllSelected = selectedTypeId == null
                        Box(
                            modifier = Modifier
                                .background(if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTypeId = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "ALL",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    items(objectTypes) { typeDef ->
                        val isSelected = selectedTypeId == typeDef.id
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTypeId = typeDef.id
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = typeDef.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Results list
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "NO OBJECTS FOUND",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(searchResults, key = { it.id }) { obj ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectObject(obj)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = getIconForObjectType(obj.typeId),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = obj.title.ifBlank { "UNTITLED OBJECT" },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = DotMatrixFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "TYPE: ${obj.typeId.uppercase()}",
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
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(0.dp),
            ) {
                Text("CANCEL", fontFamily = DotMatrixFontFamily)
            }
        },
        confirmButton = {},
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(0.dp),
    )
}
