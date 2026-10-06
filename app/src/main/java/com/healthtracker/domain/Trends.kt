package com.healthtracker.domain

import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

enum class TrendRange(val label: String, val days: Long) {
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    QUARTER("3 Months", 90),
}

/** A reading as the trend calculation needs it, independent of the database entity. */
data class TrendInput(val dateTime: Instant, val value1: Double, val value2: Double?, val unit: MeasureUnit)

/** One x position with one value per series (blood pressure has two: systolic, diastolic). */
data class TrendPoint(val dateTime: Instant, val values: List<Double>)

data class Trend(
    val type: RecordType,
    val unit: MeasureUnit,
    val seriesNames: List<String>,
    /** Oldest first. */
    val points: List<TrendPoint>,
    val averages: List<Double>,
    val start: Instant,
    val end: Instant,
)

object Trends {

    /**
     * Readings of one [type] inside the last [range], converted to a single unit (that of
     * the newest reading) so a mix of kg and lb, say, still plots on one axis.
     * Returns null when there are no readings in range.
     */
    fun build(type: RecordType, readings: List<TrendInput>, range: TrendRange, now: Instant): Trend? {
        val start = now.minus(Duration.ofDays(range.days))
        val inRange = readings.filter { !it.dateTime.isBefore(start) && !it.dateTime.isAfter(now) }
            .sortedBy { it.dateTime }
        if (inRange.isEmpty()) return null

        val unit = inRange.last().unit
        val twoSeries = type == RecordType.BLOOD_PRESSURE
        val points = inRange.mapNotNull { r ->
            val v1 = convert(r.value1, r.unit, unit)
            if (twoSeries) {
                r.value2?.let { TrendPoint(r.dateTime, listOf(v1, convert(it, r.unit, unit))) }
            } else {
                TrendPoint(r.dateTime, listOf(v1))
            }
        }
        if (points.isEmpty()) return null

        val seriesCount = points.first().values.size
        val averages = (0 until seriesCount).map { i -> points.map { it.values[i] }.average() }
        return Trend(
            type = type,
            unit = unit,
            seriesNames = if (twoSeries) listOf("Systolic", "Diastolic") else listOf(type.label),
            points = points,
            averages = averages,
            start = start,
            end = now,
        )
    }

    /** Converts between the units a record type can be stored in. */
    fun convert(value: Double, from: MeasureUnit, to: MeasureUnit): Double = when {
        from == to -> value
        from == MeasureUnit.LB && to == MeasureUnit.KG -> value * KG_PER_LB
        from == MeasureUnit.KG && to == MeasureUnit.LB -> value / KG_PER_LB
        from == MeasureUnit.FAHRENHEIT && to == MeasureUnit.CELSIUS -> (value - 32) * 5 / 9
        from == MeasureUnit.CELSIUS && to == MeasureUnit.FAHRENHEIT -> value * 9 / 5 + 32
        from == MeasureUnit.MG_DL && to == MeasureUnit.MMOL_L -> value / MGDL_PER_MMOL
        from == MeasureUnit.MMOL_L && to == MeasureUnit.MG_DL -> value * MGDL_PER_MMOL
        else -> throw IllegalArgumentException("Can't convert ${from.symbol} to ${to.symbol}")
    }

    /** Rounds an average for display: whole numbers for whole-number measurements, else one decimal. */
    fun roundForDisplay(value: Double, type: RecordType, unit: MeasureUnit): Double {
        val wholeNumbers = !type.spec.value1.allowDecimals || unit == MeasureUnit.MG_DL
        return if (wholeNumbers) value.roundToInt().toDouble() else (value * 10).roundToInt() / 10.0
    }

    /**
     * About [target] evenly spaced, "nice" axis ticks (1, 2, 2.5 or 5 times a power of ten)
     * covering [min]..[max]. A flat series gets a small band around its value.
     */
    fun niceTicks(min: Double, max: Double, target: Int = 4): List<Double> {
        var lo = min
        var hi = max
        if (hi - lo < 1e-9) {
            val pad = if (abs(lo) < 1e-9) 1.0 else abs(lo) * 0.05
            lo -= pad
            hi += pad
        }
        val rawStep = (hi - lo) / (target - 1).coerceAtLeast(1)
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= rawStep }
        val first = floor(lo / step) * step
        val last = ceil(hi / step) * step
        val count = ((last - first) / step).roundToInt()
        return (0..count).map { first + it * step }.map { (it / step).roundToInt() * step }
    }

    private const val KG_PER_LB = 0.45359237
    private const val MGDL_PER_MMOL = 18.0
}
