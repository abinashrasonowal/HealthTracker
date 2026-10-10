package com.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExportTest {

    private val today = LocalDate.of(2026, 10, 10)

    @Test
    fun `presets end today and include it`() {
        assertEquals(DateSpan(LocalDate.of(2026, 9, 11), today), presetSpan(ExportPreset.LAST_30_DAYS, today, today, today))
        assertEquals(DateSpan(LocalDate.of(2026, 7, 11), today), presetSpan(ExportPreset.LAST_3_MONTHS, today, today, today))
        assertEquals(DateSpan(null, today), presetSpan(ExportPreset.ALL_TIME, today, today, today))
    }

    @Test
    fun `custom range is put in order`() {
        val a = LocalDate.of(2026, 1, 1)
        val b = LocalDate.of(2026, 3, 1)
        assertEquals(DateSpan(a, b), presetSpan(ExportPreset.CUSTOM, today, b, a))
    }

    @Test
    fun `medications overlapping the span`() {
        val span = DateSpan(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        assertTrue("ongoing, started before", medicationOverlaps(LocalDate.of(2026, 1, 1), null, span))
        assertTrue("no dates at all", medicationOverlaps(null, null, span))
        assertTrue("stopped during", medicationOverlaps(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 10), span))
        assertFalse("started after", medicationOverlaps(LocalDate.of(2026, 10, 2), null, span))
        assertFalse("stopped before", medicationOverlaps(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 20), span))
        assertFalse("stopped on the first day, so last taken the day before", medicationOverlaps(null, LocalDate.of(2026, 9, 1), span))
        assertTrue("all time includes stopped ones", medicationOverlaps(null, LocalDate.of(2020, 1, 1), DateSpan(null, span.to)))
    }

    @Test
    fun `preferred units`() {
        val s = AppSettings(weightUnit = MeasureUnit.LB)
        assertEquals(MeasureUnit.LB, s.preferredUnit(RecordType.WEIGHT))
        assertEquals(MeasureUnit.CELSIUS, s.preferredUnit(RecordType.TEMPERATURE))
        assertEquals(MeasureUnit.MMHG, s.preferredUnit(RecordType.BLOOD_PRESSURE))
    }
}
