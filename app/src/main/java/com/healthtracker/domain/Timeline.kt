package com.healthtracker.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A flattened, date-grouped list: month header, day header, then that day's items. */
sealed interface TimelineRow<out T> {
    data class Month(val month: YearMonth) : TimelineRow<Nothing>
    data class Day(val date: LocalDate) : TimelineRow<Nothing>
    data class Item<T>(val value: T) : TimelineRow<T>
}

/** Groups [items], which must already be sorted newest first, under month and day headers. */
fun <T> buildTimeline(items: List<T>, dateOf: (T) -> LocalDate): List<TimelineRow<T>> {
    val rows = mutableListOf<TimelineRow<T>>()
    var month: YearMonth? = null
    var day: LocalDate? = null
    for (item in items) {
        val date = dateOf(item)
        val itemMonth = YearMonth.from(date)
        if (itemMonth != month) {
            rows += TimelineRow.Month(itemMonth)
            month = itemMonth
        }
        if (date != day) {
            rows += TimelineRow.Day(date)
            day = date
        }
        rows += TimelineRow.Item(item)
    }
    return rows
}

/** "October 2026". */
fun monthLabel(month: YearMonth, locale: Locale = Locale.getDefault()): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

/** "Today", "Yesterday", or "3 October" — the month header already carries the year. */
fun timelineDayLabel(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("d MMMM", locale))
}
