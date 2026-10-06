package com.healthtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.healthtracker.data.local.Medication
import com.healthtracker.domain.isMedicationActive
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun MedicationIcon(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Medication,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

@Composable
fun MedicationRow(medication: Medication, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { MedicationIcon() },
        headlineContent = { Text(medication.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            val summary = medication.doseSummary
            Text(if (summary.isNotEmpty()) "$summary\n${medication.periodLabel()}" else medication.periodLabel())
        },
    )
}

/** "5 mg · Once daily", or whichever parts are filled in. */
val Medication.doseSummary: String
    get() = listOfNotNull(dosage, frequency).joinToString(" · ")

private val mediumDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun LocalDate.mediumLabel(): String = format(mediumDate)

/** "Since Oct 1, 2026", "Stops Oct 10, 2026", "Stopped Oct 3, 2026", or "Ongoing". */
fun Medication.periodLabel(today: LocalDate = LocalDate.now()): String {
    val start = startDate
    val end = endDate
    return when {
        end != null && !isMedicationActive(end, today) -> "Stopped ${end.mediumLabel()}"
        end != null -> "Stops ${end.mediumLabel()}"
        start != null -> "Since ${start.mediumLabel()}"
        else -> "Ongoing"
    }
}
