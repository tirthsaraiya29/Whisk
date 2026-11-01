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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.ui.theme.DotMatrixFontFamily

@Composable
fun LinkPreviewCard(
    note: Note,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
    isBroken: Boolean = false,
    onCreateMissing: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (isBroken) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Object Type & Title Row
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
                        tint = if (isBroken) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title.ifBlank { "UNTITLED OBJECT" },
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DotMatrixFontFamily,
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

            // Description or Body Snippet
            val snippet = (note.description ?: note.body.take(120)).replace("\n", " ").trim()
            if (snippet.isNotBlank()) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            // Action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isBroken && onCreateMissing != null) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDC2626))
                            .clickable { onCreateMissing() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "+ CREATE OBJECT",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onOpenNote(note.id) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "OPEN OBJECT",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
