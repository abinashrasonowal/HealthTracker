package com.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class TrendsTest {

    private val now = Instant.parse("2026-10-05T09:00:00Z")
    private fun daysAgo(d: Long) = now.minus(Duration.ofDays(d))

    @Test
    fun `keeps only readings inside the range, oldest first`() {
        val readings = listOf(
            TrendInput(daysAgo(1), 70.0, null, MeasureUnit.BPM),
            TrendInput(daysAgo(10), 80.0, null, MeasureUnit.BPM),
            TrendInput(daysAgo(5), 75.0, null, MeasureUnit.BPM),
        )

        val week = Trends.build(RecordType.PULSE, readings, TrendRange.WEEK, now)!!

        assertEquals(listOf(75.0, 70.0), week.points.map { it.values.single() })
        assertEquals(72.5, week.averages.single(), 1e-9)
        assertEquals(3, Trends.build(RecordType.PULSE, readings, TrendRange.MONTH, now)!!.points.size)
    }

    @Test
    fun `no readings in range gives null`() {
        val readings = listOf(TrendInput(daysAgo(40), 70.0, null, MeasureUnit.BPM))
        assertNull(Trends.build(RecordType.PULSE, readings, TrendRange.MONTH, now))
    }

    @Test
    fun `blood pressure has systolic and diastolic series`() {
        val readings = listOf(
            TrendInput(daysAgo(2), 120.0, 80.0, MeasureUnit.MMHG),
            TrendInput(daysAgo(1), 128.0, 78.0, MeasureUnit.MMHG),
        )

        val trend = Trends.build(RecordType.BLOOD_PRESSURE, readings, TrendRange.WEEK, now)!!

        assertEquals(listOf("Systolic", "Diastolic"), trend.seriesNames)
        assertEquals(listOf(124.0, 79.0), trend.averages)
    }

    @Test
    fun `mixed units are converted to the newest reading's unit`() {
        val readings = listOf(
            TrendInput(daysAgo(2), 70.0, null, MeasureUnit.KG),
            TrendInput(daysAgo(1), 160.0, null, MeasureUnit.LB),
        )

        val trend = Trends.build(RecordType.WEIGHT, readings, TrendRange.WEEK, now)!!

        assertEquals(MeasureUnit.LB, trend.unit)
        assertEquals(154.32, trend.points.first().values.single(), 0.01)
    }

    @Test
    fun `preferred unit wins when it applies to the type`() {
        val readings = listOf(TrendInput(daysAgo(1), 70.0, null, MeasureUnit.KG))
        assertEquals(MeasureUnit.LB, Trends.build(RecordType.WEIGHT, readings, TrendRange.WEEK, now, MeasureUnit.LB)!!.unit)
        assertEquals(MeasureUnit.KG, Trends.build(RecordType.WEIGHT, readings, TrendRange.WEEK, now, MeasureUnit.CELSIUS)!!.unit)
    }

    @Test
    fun `unit conversions`() {
        assertEquals(37.0, Trends.convert(98.6, MeasureUnit.FAHRENHEIT, MeasureUnit.CELSIUS), 1e-9)
        assertEquals(5.5, Trends.convert(99.0, MeasureUnit.MG_DL, MeasureUnit.MMOL_L), 1e-9)
        assertEquals(2.20462, Trends.convert(1.0, MeasureUnit.KG, MeasureUnit.LB), 1e-5)
    }

    @Test
    fun `display rounding follows the measurement`() {
        assertEquals(124.0, Trends.roundForDisplay(123.6, RecordType.BLOOD_PRESSURE, MeasureUnit.MMHG), 0.0)
        assertEquals(71.3, Trends.roundForDisplay(71.26, RecordType.WEIGHT, MeasureUnit.KG), 1e-9)
        assertEquals(98.0, Trends.roundForDisplay(98.4, RecordType.BLOOD_SUGAR, MeasureUnit.MG_DL), 0.0)
        assertEquals(5.4, Trends.roundForDisplay(5.43, RecordType.BLOOD_SUGAR, MeasureUnit.MMOL_L), 1e-9)
    }

    @Test
    fun `nice ticks cover the data with round steps`() {
        assertEquals(listOf(60.0, 80.0, 100.0, 120.0, 140.0), Trends.niceTicks(76.0, 131.0))
        val weight = Trends.niceTicks(71.2, 71.8)
        assertTrue(weight.first() <= 71.2 && weight.last() >= 71.8)
        assertEquals(0.2, weight[1] - weight[0], 1e-9)
    }

    @Test
    fun `flat series still gets a visible band`() {
        val ticks = Trends.niceTicks(72.0, 72.0)
        assertTrue(ticks.first() < 72.0 && ticks.last() > 72.0)
    }
}
