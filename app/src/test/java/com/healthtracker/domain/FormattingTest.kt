package com.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class FormattingTest {

    @Test
    fun `numbers drop trailing zeros`() {
        assertEquals("72", formatNumber(72.0))
        assertEquals("71.2", formatNumber(71.2))
        assertEquals("36.85", formatNumber(36.85))
    }

    @Test
    fun `measurements are formatted per type`() {
        assertEquals("124 / 82 mmHg", formatMeasurement(RecordType.BLOOD_PRESSURE, 124.0, 82.0, MeasureUnit.MMHG))
        assertEquals("98 mg/dL", formatMeasurement(RecordType.BLOOD_SUGAR, 98.0, null, MeasureUnit.MG_DL))
        assertEquals("71.2 kg", formatMeasurement(RecordType.WEIGHT, 71.2, null, MeasureUnit.KG))
        assertEquals("97%", formatMeasurement(RecordType.SPO2, 97.0, null, MeasureUnit.PERCENT))
        assertEquals("98.6 °F", formatMeasurement(RecordType.TEMPERATURE, 98.6, null, MeasureUnit.FAHRENHEIT))
    }

    @Test
    fun `age counts completed years`() {
        val today = LocalDate.of(2026, 10, 5)
        assertEquals(62, ageInYears(LocalDate.of(1964, 3, 12), today))
        assertEquals(61, ageInYears(LocalDate.of(1964, 10, 6), today))
        assertEquals(62, ageInYears(LocalDate.of(1964, 10, 5), today))
        assertEquals(0, ageInYears(LocalDate.of(2027, 1, 1), today))
    }

    @Test
    fun `relative day labels`() {
        val today = LocalDate.of(2026, 10, 5)
        assertEquals("Today", relativeDayLabel(today, today, Locale.US))
        assertEquals("Yesterday", relativeDayLabel(today.minusDays(1), today, Locale.US))
        assertEquals("Sep 28", relativeDayLabel(LocalDate.of(2026, 9, 28), today, Locale.US))
        assertEquals("Dec 31, 2025", relativeDayLabel(LocalDate.of(2025, 12, 31), today, Locale.US))
    }
}
