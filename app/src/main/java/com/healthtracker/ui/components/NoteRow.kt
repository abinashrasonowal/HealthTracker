package com.healthtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.healthtracker.data.local.Note
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun NoteIcon(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier.size(size).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** One note in a list, mirroring [RecordRow]. */
@Composable
fun NoteRow(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    trailing: String = note.timeLabel,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        overlineContent = overline?.let { { Text(it, style = MaterialTheme.typography.labelLarge) } },
        leadingContent = { NoteIcon() },
        headlineContent = { Text(note.title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = note.content?.let {
            { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = { Text(trailing, style = MaterialTheme.typography.bodyMedium) },
    )
}

fun Note.localDateTime() = dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime()

val Note.timeLabel: String get() = localDateTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

val Note.dateLabel: String get() = localDateTime().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
