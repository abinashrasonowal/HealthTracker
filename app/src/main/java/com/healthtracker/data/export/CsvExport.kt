package com.healthtracker.data.export

import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.Medication
import com.healthtracker.data.local.Note
import com.healthtracker.domain.formatNumber
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Spreadsheet export: one row per reading, note or medication. Dates are ISO (2026-10-05)
 * so spreadsheets sort them correctly; numbers are plain so they can be charted.
 */
object CsvExport {

    val header = listOf("Person", "Date", "Time", "Type", "Value", "Diastolic", "Unit", "Pulse", "Measured", "Title", "Note")

    private const val BYTE_ORDER_MARK = "\uFEFF"

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun build(
        personName: String,
        records: List<HealthRecord>,
        notes: List<Note>,
        medications: List<Medication>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val rows = mutableListOf(header)
        records.forEach { r ->
            val local = r.dateTime.atZone(zone)
            rows += listOf(
                personName,
                local.toLocalDate().toString(),
                local.format(timeFormat),
                r.type.label,
                formatNumber(r.value1),
                r.value2?.let(::formatNumber).orEmpty(),
                r.unit.symbol,
                r.pulse?.toString().orEmpty(),
                r.context?.label.orEmpty(),
                "",
                r.note.orEmpty(),
            )
        }
        notes.forEach { n ->
            val local = n.dateTime.atZone(zone)
            rows += listOf(personName, local.toLocalDate().toString(), local.format(timeFormat), "Note", "", "", "", "", "", n.title, n.content.orEmpty())
        }
        medications.forEach { m ->
            val details = listOfNotNull(
                m.dosage,
                m.frequency,
                m.endDate?.let { "Stopped $it" },
                m.notes,
            ).joinToString(" · ")
            rows += listOf(personName, m.startDate?.toString().orEmpty(), "", "Medication", "", "", "", "", "", m.name, details)
        }
        // Byte-order mark so Excel reads the file as UTF-8 (°C, ₂); CRLF line ends per RFC 4180.
        return BYTE_ORDER_MARK + rows.joinToString("\r\n", postfix = "\r\n") { row -> row.joinToString(",") { cell(it) } }
    }

    /** Quotes when needed, and defuses text a spreadsheet would run as a formula. */
    internal fun cell(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }
}
