package com.healthtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.formatMeasurement
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val RecordType.icon: ImageVector
    get() = when (this) {
        RecordType.BLOOD_PRESSURE -> Icons.Filled.Favorite
        RecordType.BLOOD_SUGAR -> Icons.Filled.WaterDrop
        RecordType.WEIGHT -> Icons.Filled.MonitorWeight
        RecordType.PULSE -> Icons.Filled.MonitorHeart
        RecordType.SPO2 -> Icons.Filled.Air
        RecordType.TEMPERATURE -> Icons.Filled.Thermostat
    }

// Muted tints so each type is recognisable without the screen getting colourful.
private val RecordType.tint: Color
    get() = when (this) {
        RecordType.BLOOD_PRESSURE -> Color(0xFFC0485A)
        RecordType.BLOOD_SUGAR -> Color(0xFFB0573A)
        RecordType.WEIGHT -> Color(0xFF4F6A8F)
        RecordType.PULSE -> Color(0xFFA2476F)
        RecordType.SPO2 -> Color(0xFF2E7D8C)
        RecordType.TEMPERATURE -> Color(0xFF9A6A1B)
    }

@Composable
fun RecordTypeIcon(type: RecordType, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(type.tint.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            // Lighten the tint on dark backgrounds so it keeps enough contrast.
            tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) lerp(type.tint, Color.White, 0.35f) else type.tint,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

val HealthRecord.displayValue: String
    get() = formatMeasurement(type, value1, value2, unit)

private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val longDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

fun HealthRecord.localDateTime() = dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime()

val HealthRecord.timeLabel: String get() = localDateTime().format(timeFormatter)

val HealthRecord.dateLabel: String get() = localDateTime().format(longDateFormatter)
