package com.whisk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.domain.model.RelationshipDetail
import com.whisk.domain.model.RelationshipType
import com.whisk.ui.theme.DotMatrixFontFamily

@Composable
fun RelationshipSection(
    relationships: List<RelationshipDetail>,
    objectTypes: List<ObjectTypeDefinition>,
    onQueryObjects: suspend (String, String?) -> List<Note>,
    onAddRelationship: (targetObjectId: String, type: RelationshipType) -> Unit,
    onRemoveRelationship: (relationshipId: String) -> Unit,
    onOpenObject: (objectId: String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expandedSection by remember { mutableStateOf(true) }
    var showPickerModal by remember { mutableStateOf(false) }
    var selectedTargetObject by remember { mutableStateOf<Note?>(null) }
    var showTypeSelectModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expandedSection = !expandedSection },
            ) {
                Icon(
                    imageVector = if (expandedSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RELATIONSHIPS",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (relationships.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${relationships.size}]",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showPickerModal = true
                },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Relationship",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Relationship Items
        AnimatedVisibility(visible = expandedSection) {
            if (relationships.isEmpty()) {
                Text(
                    text = "> NO EXPLICIT OBJECT RELATIONSHIPS ESTABLISHED. CLICK '+' TO LINK.",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    relationships.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenObject(item.otherObjectId)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = getIconForObjectType(item.otherObjectType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.displayLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = DotMatrixFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "-> ${item.otherObjectTitle.ifBlank { "UNTITLED" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = DotMatrixFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRemoveRelationship(item.relationshipId)
                                },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Relationship",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Step 1: Object Picker Modal
    if (showPickerModal) {
        ObjectPickerModalDialog(
            title = "LINK TO OBJECT",
            objectTypes = objectTypes,
            onQueryObjects = onQueryObjects,
            onDismiss = { showPickerModal = false },
            onSelectObject = { target ->
                showPickerModal = false
                selectedTargetObject = target
                showTypeSelectModal = true
            },
        )
    }

    // Step 2: Relationship Type Selection Modal
    val targetObj = selectedTargetObject
    if (showTypeSelectModal && targetObj != null) {
        RelationshipTypeSelectionDialog(
            targetTitle = targetObj.title,
            onDismiss = {
                showTypeSelectModal = false
                selectedTargetObject = null
            },
            onConfirm = { relType ->
                showTypeSelectModal = false
                onAddRelationship(targetObj.id, relType)
                selectedTargetObject = null
            },
        )
    }
}

@Composable
private fun RelationshipTypeSelectionDialog(
    targetTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (RelationshipType) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var selectedType by remember { mutableStateOf(RelationshipType.RELATED_TO) }
    var expandedMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "CHOOSE RELATIONSHIP TYPE",
                fontFamily = DotMatrixFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "TARGET OBJECT: ${targetTitle.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Relationship", fontFamily = DotMatrixFontFamily) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedMenu = true },
                    )

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        RelationshipType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${type.displayName} (reciprocal: ${type.reciprocalName})",
                                        fontFamily = DotMatrixFontFamily,
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    expandedMenu = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm(selectedType)
                },
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("ESTABLISH LINK", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
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
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(0.dp),
    )
}
