package com.healthtracker.ui.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.Trend
import com.healthtracker.domain.TrendPoint
import com.healthtracker.domain.TrendRange
import com.healthtracker.domain.Trends
import com.healthtracker.domain.formatMeasurement
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Trends tab: pick a measurement and a range, see one chart. One chart at a time keeps the
 * screen calm; it shows how values changed, never whether they are "good" or "bad".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsContent(
    availableTypes: List<RecordType>,
    type: RecordType?,
    range: TrendRange,
    trend: Trend?,
    onTypeChange: (RecordType) -> Unit,
    onRangeChange: (TrendRange) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(contentPadding = contentPadding) {
        if (availableTypes.isEmpty() || type == null) {
            item { Hint("Trends appear once there are measurements to compare.") }
            return@LazyColumn
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(availableTypes) { t ->
                    FilterChip(selected = t == type, onClick = { onTypeChange(t) }, label = { Text(t.label) })
                }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                TrendRange.entries.forEachIndexed { i, r ->
                    SegmentedButton(
                        selected = r == range,
                        onClick = { onRangeChange(r) },
                        shape = SegmentedButtonDefaults.itemShape(i, TrendRange.entries.size),
                    ) { Text(r.label) }
                }
            }
        }
        item {
            if (trend == null) {
                Hint("No ${type.label.lowercase()} readings in the last ${range.label.lowercase()}.")
            } else {
                // Reset the crosshair when the data changes.
                key(trend) { TrendCard(trend, range) }
            }
        }
    }
}

@Composable
private fun TrendCard(trend: Trend, range: TrendRange) {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var showTable by rememberSaveable { mutableStateOf(false) }
    val colors = trendSeriesColors()
    val average = averageLabel(trend)
    val count = trend.points.size
    val readings = if (count == 1) "1 reading" else "$count readings"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Average", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(average, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "$readings · last ${range.label.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (trend.seriesNames.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    trend.seriesNames.forEachIndexed { i, name -> LegendItem(name, colors[i]) }
                }
            }

            // Readout for the crosshair: the value leads, the date follows.
            val point = selected?.let { trend.points.getOrNull(it) }
            if (point != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(pointValue(trend, point), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(pointDate(point), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(
                    "Tap or drag on the chart to see a reading.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TrendChart(
                trend = trend,
                selected = selected,
                onSelect = { selected = it },
                summary = "${trend.type.label} chart, $readings in the last ${range.label.lowercase()}, average $average.",
            )

            TextButton(onClick = { showTable = !showTable }, modifier = Modifier.align(Alignment.End)) {
                Text(if (showTable) "Hide readings" else "Show readings")
            }
            if (showTable) {
                Column {
                    trend.points.asReversed().forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Text(pointDate(p), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(pointValue(trend, p), style = MaterialTheme.typography.titleMedium)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Line key, matching the line marks.
        Box(Modifier.width(16.dp).height(3.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

private val pointDateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun pointDate(p: TrendPoint) = p.dateTime.atZone(ZoneId.systemDefault()).format(pointDateFormat)

private fun round(trend: Trend, v: Double) = Trends.roundForDisplay(v, trend.type, trend.unit)

private fun pointValue(trend: Trend, p: TrendPoint) =
    formatMeasurement(trend.type, round(trend, p.values[0]), p.values.getOrNull(1)?.let { round(trend, it) }, trend.unit)

private fun averageLabel(trend: Trend) =
    formatMeasurement(trend.type, round(trend, trend.averages[0]), trend.averages.getOrNull(1)?.let { round(trend, it) }, trend.unit)
