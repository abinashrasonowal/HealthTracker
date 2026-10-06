package com.healthtracker.ui.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.healthtracker.domain.Trend
import com.healthtracker.domain.Trends
import com.healthtracker.domain.formatNumber
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Series colors: categorical slots 1 and 2 (blue, orange), stepped separately for the
 * light and dark card surfaces. Validated for color-vision deficiency on both; the light
 * orange is just under 3:1 against the card, which the legend, end labels and the
 * readings table make up for.
 */
@Composable
fun trendSeriesColors(): List<Color> {
    val dark = MaterialTheme.colorScheme.surfaceContainerLow.luminance() < 0.5f
    return if (dark) listOf(Color(0xFF3987E5), Color(0xFFD95926)) else listOf(Color(0xFF2A78D6), Color(0xFFEB6834))
}

private val axisDateFormat = DateTimeFormatter.ofPattern("MMM d")

/**
 * A time-proportional line chart: x spans the whole selected range, so gaps between
 * readings show as gaps. [selected] is the index of the point under the crosshair.
 */
@Composable
fun TrendChart(
    trend: Trend,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val colors = trendSeriesColors()
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val grid = MaterialTheme.colorScheme.outlineVariant
    val crosshair = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val endLabelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val measurer = rememberTextMeasurer()

    val allValues = trend.points.flatMap { it.values }
    val ticks = Trends.niceTicks(allValues.min(), allValues.max())
    val zone = ZoneId.systemDefault()
    val twoSeries = trend.seriesNames.size > 1

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = summary }
            .pointerInput(trend) {
                detectTapGestures { offset -> onSelect(nearestIndex(trend, offset.x, size.width.toFloat(), layout(trend, ticks, measurer, labelStyle, endLabelStyle, twoSeries, this))) }
            }
            .pointerInput(trend) {
                detectHorizontalDragGestures { change, _ ->
                    onSelect(nearestIndex(trend, change.position.x, size.width.toFloat(), layout(trend, ticks, measurer, labelStyle, endLabelStyle, twoSeries, this)))
                }
            },
    ) {
        val l = layout(trend, ticks, measurer, labelStyle, endLabelStyle, twoSeries, this)
        val plotLeft = l.left
        val plotRight = size.width - l.right
        val plotTop = 8.dp.toPx()
        val plotBottom = size.height - l.bottom
        val yMin = ticks.first()
        val yMax = ticks.last()
        fun x(i: Int) = plotLeft + (plotRight - plotLeft) * fraction(trend, i)
        fun y(v: Double) = (plotBottom - (plotBottom - plotTop) * ((v - yMin) / (yMax - yMin))).toFloat()

        // Recessive hairline grid with round-number labels.
        ticks.forEach { t ->
            val ty = y(t)
            drawLine(grid, Offset(plotLeft, ty), Offset(plotRight, ty), strokeWidth = 1.dp.toPx())
            val text = measurer.measure(formatNumber(t), labelStyle)
            drawText(text, topLeft = Offset(plotLeft - text.size.width - 6.dp.toPx(), ty - text.size.height / 2))
        }
        // Start, middle and end dates of the range.
        listOf(0f, 0.5f, 1f).forEach { f ->
            val instant = trend.start.plusMillis(((trend.end.toEpochMilli() - trend.start.toEpochMilli()) * f).toLong())
            val text = measurer.measure(instant.atZone(zone).format(axisDateFormat), labelStyle)
            val tx = (plotLeft + (plotRight - plotLeft) * f - text.size.width * f).coerceAtLeast(plotLeft)
            drawText(text, topLeft = Offset(tx, plotBottom + 6.dp.toPx()))
        }

        selected?.let { i ->
            drawLine(crosshair, Offset(x(i), plotTop), Offset(x(i), plotBottom), strokeWidth = 1.dp.toPx())
        }

        trend.seriesNames.indices.forEach { s ->
            val color = colors[s]
            if (trend.points.size > 1) {
                val path = Path()
                trend.points.forEachIndexed { i, p ->
                    if (i == 0) path.moveTo(x(i), y(p.values[s])) else path.lineTo(x(i), y(p.values[s]))
                }
                drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            trend.points.forEachIndexed { i, p ->
                val r = if (i == selected) 6.dp.toPx() else 4.dp.toPx()
                drawMarker(Offset(x(i), y(p.values[s])), r, color, surface)
            }
        }

        if (twoSeries) drawEndLabels(trend, l, measurer, endLabelStyle, ::y, plotRight)
    }
}

private data class ChartLayout(val left: Float, val right: Float, val bottom: Float)

private fun layout(
    trend: Trend,
    ticks: List<Double>,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
    endLabelStyle: TextStyle,
    twoSeries: Boolean,
    density: androidx.compose.ui.unit.Density,
): ChartLayout = with(density) {
    val tickWidth = ticks.maxOf { measurer.measure(formatNumber(it), labelStyle).size.width }
    val endWidth = if (twoSeries) {
        trend.seriesNames.indices.maxOf { s ->
            measurer.measure(endLabel(trend, s), endLabelStyle).size.width
        } + 10.dp.toPx()
    } else {
        12.dp.toPx()
    }
    ChartLayout(left = tickWidth + 10.dp.toPx(), right = endWidth, bottom = 28.dp.toPx())
}

private fun endLabel(trend: Trend, series: Int): String {
    val short = if (series == 0) "Sys" else "Dia"
    return "$short ${formatNumber(trend.points.last().values[series])}"
}

/** Direct labels at the line ends; skipped when they would overlap (the legend still names them). */
private fun DrawScope.drawEndLabels(
    trend: Trend,
    l: ChartLayout,
    measurer: TextMeasurer,
    style: TextStyle,
    y: (Double) -> Float,
    plotRight: Float,
) {
    val last = trend.points.last().values
    val texts = trend.seriesNames.indices.map { measurer.measure(endLabel(trend, it), style) }
    if (abs(y(last[0]) - y(last[1])) < texts[0].size.height) return
    texts.forEachIndexed { s, text ->
        drawText(text, topLeft = Offset(plotRight + 8.dp.toPx(), y(last[s]) - text.size.height / 2))
    }
}

/** 8dp dot with a 2dp ring in the surface color, so it stays legible where it crosses a line. */
private fun DrawScope.drawMarker(center: Offset, radius: Float, color: Color, surface: Color) {
    drawCircle(surface, radius + 2.dp.toPx(), center)
    drawCircle(color, radius, center)
}

private fun fraction(trend: Trend, i: Int): Float {
    val span = (trend.end.toEpochMilli() - trend.start.toEpochMilli()).toFloat()
    return ((trend.points[i].dateTime.toEpochMilli() - trend.start.toEpochMilli()) / span).coerceIn(0f, 1f)
}

private fun nearestIndex(trend: Trend, px: Float, width: Float, l: ChartLayout): Int {
    val plotWidth = width - l.left - l.right
    return trend.points.indices.minBy { abs(l.left + plotWidth * fraction(trend, it) - px) }
}
