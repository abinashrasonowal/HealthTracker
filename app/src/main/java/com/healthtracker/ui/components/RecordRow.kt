package com.healthtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.healthtracker.data.local.HealthRecord

/**
 * One record in a list. [overline] optionally names the person (used by search);
 * [trailing] defaults to the time, since lists are usually grouped by day already.
 */
@Composable
fun RecordRow(
    record: HealthRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    trailing: String = record.timeLabel,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        overlineContent = overline?.let { { Text(it, style = MaterialTheme.typography.labelLarge) } },
        leadingContent = { RecordTypeIcon(record.type) },
        headlineContent = { Text(record.displayValue, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(record.type.label) },
        trailingContent = { Text(trailing, style = MaterialTheme.typography.bodyMedium) },
    )
}
