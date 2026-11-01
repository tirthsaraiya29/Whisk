package com.whisk.ui.components.views

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.domain.model.NotePropertyValue
import com.whisk.ui.theme.DotMatrixFontFamily
import com.whisk.ui.theme.VercelMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarView(
    notes: List<Note>,
    datePropertyId: String? = null,
    notePropertyValues: Map<String, List<NotePropertyValue>> = emptyMap(),
    onOpenNote: (String) -> Unit,
    onCreateOnDate: (dateKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val currentMonthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dateKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Map dateKey ("YYYY-MM-DD") -> List<Note>
    val notesByDate = remember(notes, datePropertyId, notePropertyValues) {
        val map = mutableMapOf<String, MutableList<Note>>()
        notes.forEach { note ->
            val dateKey = if (!datePropertyId.isNullOrBlank()) {
                val pVal = notePropertyValues[note.id]?.firstOrNull { it.propertyDefinitionId == datePropertyId }
                pVal?.textValue ?: dateKeyFormat.format(Date(note.createdAt))
            } else {
                dateKeyFormat.format(Date(note.createdAt))
            }
            map.getOrPut(dateKey) { mutableListOf() }.add(note)
        }
        map
    }

    // Days in current month grid
    val daysInMonth = remember(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)) {
        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<String>()
        repeat(firstDayOfWeek) { list.add("") } // Blank leading padding
        for (day in 1..maxDays) {
            tempCal.set(Calendar.DAY_OF_MONTH, day)
            list.add(dateKeyFormat.format(tempCal.time))
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val newCal = calendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        calendar = newCal
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(
                    text = currentMonthFormat.format(calendar.time).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                IconButton(
                    onClick = {
                        val newCal = calendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        calendar = newCal
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Button(
                onClick = { calendar = Calendar.getInstance() },
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "TODAY",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        // Days of Week Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

        // Month Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(daysInMonth) { dateKey ->
                if (dateKey.isEmpty()) {
                    Box(modifier = Modifier.height(75.dp))
                } else {
                    val dateNotes = notesByDate[dateKey] ?: emptyList()
                    val dayNum = dateKey.split("-").lastOrNull() ?: ""

                    Column(
                        modifier = Modifier
                            .height(75.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                            .background(if (dateNotes.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                            .clickable {
                                if (dateNotes.isNotEmpty()) {
                                    onOpenNote(dateNotes.first().id)
                                } else {
                                    onCreateOnDate(dateKey)
                                }
                            }
                            .padding(4.dp),
                    ) {
                        Text(
                            text = dayNum,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        dateNotes.take(2).forEach { n ->
                            Text(
                                text = n.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = VercelMonoFontFamily,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        if (dateNotes.size > 2) {
                            Text(
                                text = "+${dateNotes.size - 2} more",
                                fontFamily = DotMatrixFontFamily,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
