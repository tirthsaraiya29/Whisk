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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.ui.components.getIconForObjectType
import com.whisk.ui.theme.DotMatrixFontFamily
import com.whisk.ui.theme.VercelMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListView(
    notes: List<Note>,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    if (notes.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "> NO OBJECTS FOUND IN LIST VIEW.",
                fontFamily = DotMatrixFontFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(notes, key = { it.id }) { note ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onOpenNote(note.id) }
                    .padding(12.dp),
            ) {
                // Header row: Icon, Title, Type Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = getIconForObjectType(note.typeId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = VercelMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = note.typeId.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }

                // Description or Snippet if present
                if (!note.description.isNullOrBlank() || note.body.isNotBlank()) {
                    val snippet = (note.description ?: note.body.take(100)).replace("\n", " ").trim()
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

                // Footer row: Status, Tags, Updated Date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STATUS: ${note.status ?: "DRAFT"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        if (note.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = note.tags.joinToString(" ") { "#$it" },
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        text = dateFormat.format(Date(note.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
