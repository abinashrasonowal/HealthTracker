package com.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class TimelineTest {

    @Test
    fun `groups items under month and day headers`() {
        val dates = listOf(
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 3),
            LocalDate.of(2026, 9, 30),
        )

        val rows = buildTimeline(dates) { it }

        assertEquals(
            listOf(
                TimelineRow.Month(YearMonth.of(2026, 10)),
                TimelineRow.Day(dates[0]),
                TimelineRow.Item(dates[0]),
                TimelineRow.Item(dates[1]),
                TimelineRow.Day(dates[2]),
                TimelineRow.Item(dates[2]),
                TimelineRow.Month(YearMonth.of(2026, 9)),
                TimelineRow.Day(dates[3]),
                TimelineRow.Item(dates[3]),
            ),
            rows,
        )
    }

    @Test
    fun `same month in different years gets separate headers`() {
        val rows = buildTimeline(listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2025, 1, 1))) { it }
        assertEquals(2, rows.count { it is TimelineRow.Month })
    }

    @Test
    fun `empty input gives empty timeline`() {
        assertEquals(emptyList<TimelineRow<LocalDate>>(), buildTimeline(emptyList<LocalDate>()) { it })
    }

    @Test
    fun `labels`() {
        val today = LocalDate.of(2026, 10, 5)
        assertEquals("October 2026", monthLabel(YearMonth.of(2026, 10), Locale.US))
        assertEquals("Today", timelineDayLabel(today, today, Locale.US))
        assertEquals("Yesterday", timelineDayLabel(today.minusDays(1), today, Locale.US))
        assertEquals("3 October", timelineDayLabel(LocalDate.of(2026, 10, 3), today, Locale.US))
    }
}
