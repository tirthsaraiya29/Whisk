package com.whisk.ui.components.views

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.domain.model.NotePropertyValue
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyType
import com.whisk.ui.components.getIconForObjectType
import com.whisk.ui.theme.DotMatrixFontFamily
import com.whisk.ui.theme.VercelMonoFontFamily
import java.util.Calendar

@Composable
fun TableView(
    notes: List<Note>,
    propertyDefinitions: List<PropertyDefinition>,
    notePropertyValues: Map<String, List<NotePropertyValue>>, // noteId -> values
    visiblePropertyIds: List<String>,
    onOpenNote: (String) -> Unit,
    onUpdatePropertyValue: (noteId: String, definitionId: String, textVal: String?, numVal: Double?, boolVal: Boolean?, listVal: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    val activeDefinitions = remember(visiblePropertyIds, propertyDefinitions) {
        if (visiblePropertyIds.isEmpty()) propertyDefinitions.take(4)
        else propertyDefinitions.filter { visiblePropertyIds.contains(it.id) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(scrollState),
    ) {
        // Table Column Header Row
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Object Title Column Header
            Box(modifier = Modifier.width(180.dp)) {
                Text(
                    text = "TITLE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Object Type Column Header
            Box(modifier = Modifier.width(110.dp)) {
                Text(
                    text = "TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Status Column Header
            Box(modifier = Modifier.width(110.dp)) {
                Text(
                    text = "STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Property Columns Headers
            activeDefinitions.forEach { def ->
                Box(modifier = Modifier.width(140.dp)) {
                    Text(
                        text = def.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        // Table Rows List
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(notes, key = { it.id }) { note ->
                val pValues = notePropertyValues[note.id] ?: emptyList()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Object Title Cell
                    Row(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable { onOpenNote(note.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = getIconForObjectType(note.typeId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = VercelMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Object Type Cell
                    Box(modifier = Modifier.width(110.dp)) {
                        Text(
                            text = note.typeId.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Status Cell
                    Box(modifier = Modifier.width(110.dp)) {
                        Text(
                            text = note.status ?: "DRAFT",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Property Value Cells
                    activeDefinitions.forEach { def ->
                        val pVal = pValues.firstOrNull { it.propertyDefinitionId == def.id }
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .padding(end = 8.dp),
                        ) {
                            TableCellEditor(
                                definition = def,
                                value = pVal,
                                onUpdate = { textVal, numVal, boolVal, listVal ->
                                    onUpdatePropertyValue(note.id, def.id, textVal, numVal, boolVal, listVal)
                                },
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun TableCellEditor(
    definition: PropertyDefinition,
    value: NotePropertyValue?,
    onUpdate: (textVal: String?, numVal: Double?, boolVal: Boolean?, listVal: List<String>) -> Unit,
) {
    val context = LocalContext.current

    when (definition.type) {
        PropertyType.CHECKBOX -> {
            Checkbox(
                checked = value?.booleanValue ?: false,
                onCheckedChange = { checked ->
                    onUpdate(null, null, checked, emptyList())
                },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            )
        }
        PropertyType.SELECT -> {
            var showSelectMenu by remember { mutableStateOf(false) }
            val currentVal = value?.textValue ?: "Select..."

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                        .clickable { showSelectMenu = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = currentVal,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        maxLines = 1,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }

                DropdownMenu(
                    expanded = showSelectMenu,
                    onDismissRequest = { showSelectMenu = false },
                ) {
                    definition.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontFamily = DotMatrixFontFamily, fontSize = 11.sp) },
                            onClick = {
                                showSelectMenu = false
                                onUpdate(option, null, null, emptyList())
                            },
                        )
                    }
                }
            }
        }
        PropertyType.DATE -> {
            val dateStr = value?.textValue ?: "Pick Date"

            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                    .clickable {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formatted = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                                onUpdate(formatted, null, null, emptyList())
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                    )
                }
            }
        }
        else -> {
            var textInput by remember(value?.textValue) { mutableStateOf(value?.textValue ?: "") }
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    onUpdate(it, null, null, emptyList())
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = DotMatrixFontFamily, fontSize = 11.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
