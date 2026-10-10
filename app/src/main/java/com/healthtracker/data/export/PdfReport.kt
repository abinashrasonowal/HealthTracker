package com.healthtracker.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.healthtracker.data.ExportData
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.Trends
import com.healthtracker.domain.ageInYears
import com.healthtracker.domain.formatMeasurement
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A printable summary for a doctor: who, which period, current medications, readings grouped
 * by type (with count and average), then notes. A4, black on white, no colours to lose in print.
 * It reports what was recorded and makes no judgement about the values.
 */
class PdfReport(private val zone: ZoneId = ZoneId.systemDefault()) {

    fun write(data: ExportData, out: OutputStream, today: LocalDate = LocalDate.now(zone)) {
        val doc = PdfDocument()
        try {
            Writer(doc).apply {
                header(data, today)
                medications(data)
                readings(data)
                notes(data)
                finish()
            }
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    private inner class Writer(private val doc: PdfDocument) {
        private val pageWidth = 595 // A4 in points
        private val pageHeight = 842
        private val margin = 48f
        private val contentWidth = pageWidth - 2 * margin
        private val bottom = pageHeight - margin - 24f // room for the footer

        private val title = paint(20f, bold = true)
        private val heading = paint(14f, bold = true)
        private val body = paint(10.5f)
        private val bodyBold = paint(10.5f, bold = true)
        private val muted = paint(9.5f).apply { color = Color.rgb(90, 90, 90) }
        private val thinRule = Paint().apply { color = Color.rgb(200, 200, 200); strokeWidth = 0.5f }
        private val headerRule = Paint().apply { color = Color.rgb(120, 120, 120); strokeWidth = 1f }

        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var y = 0f
        private var reportTitle = ""

        // Linear, unhinted text: positions aren't snapped to screen pixels, which squeezes word spacing in a PDF.
        private fun paint(size: Float, bold: Boolean = false) = TextPaint(
            Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG,
        ).apply {
            hinting = Paint.HINTING_OFF
            textSize = size
            color = Color.BLACK
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

        private fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page!!.canvas
            y = margin
            drawText("$reportTitle · Page $pageNumber", muted, margin, pageHeight - margin, contentWidth)
        }

        private fun ensure(height: Float) {
            if (page == null || y + height > bottom) newPage()
        }

        private fun layout(text: String, paint: TextPaint, width: Float): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .build()

        private fun drawText(text: String, paint: TextPaint, x: Float, top: Float, width: Float): Float {
            val l = layout(text, paint, width)
            canvas.save()
            canvas.translate(x, top)
            l.draw(canvas)
            canvas.restore()
            return l.height.toFloat()
        }

        /** A full-width paragraph, moved to the next page whole if it doesn't fit. */
        private fun paragraph(text: String, paint: TextPaint, spaceAfter: Float = 4f) {
            ensure(layout(text, paint, contentWidth).height.toFloat())
            y += drawText(text, paint, margin, y, contentWidth) + spaceAfter
        }

        /** Wrapped text starting [indent] in from the left margin, e.g. a note under a table row. */
        private fun indented(text: String, indent: Float) {
            val width = contentWidth - indent
            ensure(layout(text, muted, width).height + 8f)
            y += drawText(text, muted, margin + indent, y + 2f, width) + 6f
            canvas.drawLine(margin, y, margin + contentWidth, y, thinRule)
        }

        private fun section(name: String) {
            ensure(60f) // keep a heading with at least a couple of rows
            y += 14f
            y += drawText(name, heading, margin, y, contentWidth) + 6f
        }

        /** A table row; [widths] are fractions of the content width. */
        private fun row(cells: List<String>, widths: List<Float>, paint: TextPaint, header: Boolean = false) {
            val layouts = cells.mapIndexed { i, c -> layout(c, paint, contentWidth * widths[i] - 8f) }
            val height = layouts.maxOf { it.height } + 8f
            ensure(height)
            var x = margin
            layouts.forEachIndexed { i, l ->
                canvas.save()
                canvas.translate(x, y + 4f)
                l.draw(canvas)
                canvas.restore()
                x += contentWidth * widths[i]
            }
            y += height
            canvas.drawLine(margin, y, margin + contentWidth, y, if (header) headerRule else thinRule)
        }

        fun header(data: ExportData, today: LocalDate) {
            val p = data.person
            reportTitle = "Health records: ${p.name}"
            newPage()
            y += drawText(reportTitle, title, margin, y, contentWidth) + 6f
            val about = listOfNotNull(
                p.relationship,
                p.dateOfBirth?.let { "Born ${it.format(dateFormat)} (${ageInYears(it, today)} years)" },
                p.gender?.label,
            ).joinToString(" · ")
            if (about.isNotEmpty()) paragraph(about, body)
            val period = data.span.from?.let { "${it.format(dateFormat)} to ${data.span.to.format(dateFormat)}" }
                ?: "All records up to ${data.span.to.format(dateFormat)}"
            paragraph("Period: $period", body)
            paragraph("Created ${today.format(dateFormat)}. Values were recorded at home by the user.", muted)
            p.notes?.let { paragraph("About this person: $it", body) }
            if (data.isEmpty) paragraph("Nothing was recorded in this period.", body)
        }

        fun medications(data: ExportData) {
            if (data.medications.isEmpty()) return
            section("Medications")
            val widths = listOf(0.3f, 0.18f, 0.22f, 0.3f)
            row(listOf("Medicine", "Dose", "How often", "Taken"), widths, bodyBold, header = true)
            val today = LocalDate.now(zone)
            data.medications.forEach { m ->
                val taken = when {
                    m.endDate != null && !m.endDate.isAfter(today) -> listOfNotNull(m.startDate?.format(dateFormat), "stopped ${m.endDate.format(dateFormat)}").joinToString(", ")
                    m.startDate != null -> "Since ${m.startDate.format(dateFormat)}" + (m.endDate?.let { ", until ${it.format(dateFormat)}" } ?: "")
                    else -> "Current"
                }
                row(listOf(m.name, m.dosage.orEmpty(), m.frequency.orEmpty(), taken), widths, body)
                m.notes?.let { indented(it, indent = contentWidth * widths[0]) }
            }
        }

        fun readings(data: ExportData) {
            val byType = data.records.groupBy { it.type }
            RecordType.entries.forEach { type ->
                val records = byType[type] ?: return@forEach
                section(type.label)
                paragraph(summary(type, records), muted, spaceAfter = 6f)
                val extra = when (type) {
                    RecordType.BLOOD_PRESSURE, RecordType.SPO2 -> "Pulse"
                    RecordType.BLOOD_SUGAR -> "Measured"
                    else -> null
                }
                val widths = if (extra != null) listOf(0.2f, 0.13f, 0.22f, 0.13f, 0.32f) else listOf(0.2f, 0.13f, 0.22f, 0.45f)
                row(listOfNotNull("Date", "Time", "Reading", extra, "Note"), widths, bodyBold, header = true)
                records.forEach { r ->
                    val local = r.dateTime.atZone(zone)
                    val extraValue = when (type) {
                        RecordType.BLOOD_PRESSURE, RecordType.SPO2 -> r.pulse?.let { "$it bpm" }.orEmpty()
                        RecordType.BLOOD_SUGAR -> r.context?.label.orEmpty()
                        else -> null
                    }
                    row(
                        listOfNotNull(
                            local.toLocalDate().format(dateFormat),
                            local.toLocalTime().format(timeFormat),
                            formatMeasurement(r.type, r.value1, r.value2, r.unit),
                            extraValue,
                            r.note.orEmpty(),
                        ),
                        widths,
                        body,
                    )
                }
            }
        }

        /** "12 readings · average 127 / 82 mmHg", averaged in the most recent reading's unit. */
        private fun summary(type: RecordType, records: List<HealthRecord>): String {
            val count = if (records.size == 1) "1 reading" else "${records.size} readings"
            if (records.size < 2) return count
            val unit = records.first().unit
            fun avg(values: List<Double>) = Trends.roundForDisplay(values.average(), type, unit)
            val a1 = avg(records.map { Trends.convert(it.value1, it.unit, unit) })
            val a2 = records.mapNotNull { r -> r.value2?.let { Trends.convert(it, r.unit, unit) } }.takeIf { it.isNotEmpty() }?.let(::avg)
            return "$count · average ${formatMeasurement(type, a1, a2, unit)}"
        }

        fun notes(data: ExportData) {
            if (data.notes.isEmpty()) return
            section("Notes")
            data.notes.forEach { n ->
                val local = n.dateTime.atZone(zone)
                paragraph("${local.toLocalDate().format(dateFormat)} · ${n.title}", bodyBold, spaceAfter = 2f)
                n.content?.let { paragraph(it, body, spaceAfter = 8f) }
            }
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }
    }
}
