package com.healthtracker.domain

import java.time.LocalDate

enum class ExportPreset(val label: String) {
    LAST_30_DAYS("Last 30 days"),
    LAST_3_MONTHS("Last 3 months"),
    LAST_YEAR("Last year"),
    ALL_TIME("All time"),
    CUSTOM("Custom"),
}

/** An inclusive range of days; [from] null means "from the beginning". */
data class DateSpan(val from: LocalDate?, val to: LocalDate)

fun presetSpan(preset: ExportPreset, today: LocalDate, customFrom: LocalDate, customTo: LocalDate): DateSpan = when (preset) {
    ExportPreset.LAST_30_DAYS -> DateSpan(today.minusDays(29), today)
    ExportPreset.LAST_3_MONTHS -> DateSpan(today.minusMonths(3).plusDays(1), today)
    ExportPreset.LAST_YEAR -> DateSpan(today.minusYears(1).plusDays(1), today)
    ExportPreset.ALL_TIME -> DateSpan(null, today)
    ExportPreset.CUSTOM -> if (customFrom.isAfter(customTo)) DateSpan(customTo, customFrom) else DateSpan(customFrom, customTo)
}

/**
 * Whether a medication was being taken at some point during [span]. The stop date is the
 * day it was stopped (see [isMedicationActive]), so it was last taken the day before.
 */
fun medicationOverlaps(startDate: LocalDate?, stopDate: LocalDate?, span: DateSpan): Boolean {
    val startedInTime = startDate == null || !startDate.isAfter(span.to)
    val notStoppedBefore = stopDate == null || span.from == null || stopDate.isAfter(span.from)
    return startedInTime && notStoppedBefore
}
