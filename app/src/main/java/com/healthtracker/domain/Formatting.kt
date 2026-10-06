package com.healthtracker.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 72.0 -> "72", 71.25 -> "71.25". */
fun formatNumber(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

/** The headline value of a record, e.g. "124 / 82 mmHg" or "98 mg/dL". */
fun formatMeasurement(type: RecordType, value1: Double, value2: Double?, unit: MeasureUnit): String {
    val main = if (type == RecordType.BLOOD_PRESSURE && value2 != null) {
        "${formatNumber(value1)} / ${formatNumber(value2)}"
    } else {
        formatNumber(value1)
    }
    val separator = if (unit == MeasureUnit.PERCENT) "" else " "
    return "$main$separator${unit.symbol}"
}

fun ageInYears(dateOfBirth: LocalDate, today: LocalDate): Int =
    Period.between(dateOfBirth, today).years.coerceAtLeast(0)

/** "Today", "Yesterday", "Sep 28", or "Sep 28, 2025" for other years. */
fun relativeDayLabel(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM d", locale))
    else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))
}
