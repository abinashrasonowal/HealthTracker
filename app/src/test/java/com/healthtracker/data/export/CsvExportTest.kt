package com.healthtracker.data.export

import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.Medication
import com.healthtracker.data.local.Note
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.SugarContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CsvExportTest {

    private val t = Instant.parse("2026-10-05T08:30:00Z")

    private fun lines(csv: String) = csv.removePrefix("\uFEFF").split("\r\n").dropLast(1)

    @Test
    fun `writes header and one row per item`() {
        val csv = CsvExport.build(
            "Dad",
            records = listOf(
                HealthRecord(personId = 1, type = RecordType.BLOOD_PRESSURE, value1 = 124.0, value2 = 82.0, pulse = 72,
                    unit = MeasureUnit.MMHG, dateTime = t, note = "Before breakfast", createdAt = t, updatedAt = t),
                HealthRecord(personId = 1, type = RecordType.BLOOD_SUGAR, value1 = 5.4, unit = MeasureUnit.MMOL_L,
                    context = SugarContext.FASTING, dateTime = t, createdAt = t, updatedAt = t),
            ),
            notes = listOf(Note(personId = 1, title = "Doctor Visit", content = "All fine", dateTime = t, createdAt = t, updatedAt = t)),
            medications = listOf(
                Medication(personId = 1, name = "Amlodipine", dosage = "5 mg", frequency = "Once daily",
                    startDate = LocalDate.of(2026, 1, 1), createdAt = t, updatedAt = t),
            ),
            zone = ZoneOffset.UTC,
        )

        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals(
            listOf(
                "Person,Date,Time,Type,Value,Diastolic,Unit,Pulse,Measured,Title,Note",
                "Dad,2026-10-05,08:30,Blood Pressure,124,82,mmHg,72,,,Before breakfast",
                "Dad,2026-10-05,08:30,Blood Sugar,5.4,,mmol/L,,Fasting,,",
                "Dad,2026-10-05,08:30,Note,,,,,,Doctor Visit,All fine",
                "Dad,2026-01-01,,Medication,,,,,,Amlodipine,5 mg · Once daily",
            ),
            lines(csv),
        )
    }

    @Test
    fun `quotes commas, quotes and newlines`() {
        assertEquals("\"a, b\"", CsvExport.cell("a, b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExport.cell("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvExport.cell("line1\nline2"))
        assertEquals("plain", CsvExport.cell("plain"))
    }

    @Test
    fun `defuses spreadsheet formulas`() {
        assertEquals("'=HYPERLINK(1)", CsvExport.cell("=HYPERLINK(1)"))
        assertEquals("'+1", CsvExport.cell("+1"))
        assertEquals("'@cmd", CsvExport.cell("@cmd"))
    }
}
