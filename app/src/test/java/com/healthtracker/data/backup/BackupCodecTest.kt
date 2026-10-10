package com.healthtracker.data.backup

import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.Medication
import com.healthtracker.data.local.Note
import com.healthtracker.data.local.Person
import com.healthtracker.domain.Gender
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.SugarContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BackupCodecTest {

    private val t = Instant.parse("2026-10-05T08:30:00Z")

    private val contents = BackupContents(
        createdAt = t,
        people = listOf(
            Person(id = 1, name = "Dad", relationship = "Father", dateOfBirth = LocalDate.of(1964, 3, 12),
                gender = Gender.MALE, notes = "Allergic to penicillin", createdAt = t, updatedAt = t),
            Person(id = 2, name = "Mom", createdAt = t, updatedAt = t),
        ),
        records = listOf(
            HealthRecord(id = 10, personId = 1, type = RecordType.BLOOD_PRESSURE, value1 = 124.0, value2 = 82.0, pulse = 72,
                unit = MeasureUnit.MMHG, dateTime = t, note = "x", createdAt = t, updatedAt = t),
            HealthRecord(id = 11, personId = 2, type = RecordType.BLOOD_SUGAR, value1 = 98.0, unit = MeasureUnit.MG_DL,
                context = SugarContext.AFTER_MEAL, dateTime = t, createdAt = t, updatedAt = t),
        ),
        notes = listOf(Note(id = 5, personId = 1, title = "Visit", content = "ok", dateTime = t, createdAt = t, updatedAt = t)),
        medications = listOf(
            Medication(id = 7, personId = 1, name = "Metformin", dosage = "500 mg", startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 10, 5), createdAt = t, updatedAt = t),
        ),
        photos = mapOf(1L to byteArrayOf(1, 2, 3, -1)),
    )

    @Test
    fun `round trips everything`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(contents))

        assertEquals(contents.createdAt, decoded.createdAt)
        assertEquals(contents.people, decoded.people)
        assertEquals(contents.records, decoded.records)
        assertEquals(contents.notes, decoded.notes)
        assertEquals(contents.medications, decoded.medications)
        assertArrayEquals(contents.photos.getValue(1), decoded.photos.getValue(1))
        assertEquals(setOf(1L), decoded.photos.keys)
    }

    @Test
    fun `rejects files that are not backups`() {
        val e = assertThrows(BackupFormatException::class.java) { BackupCodec.decode("{\"hello\": 1}") }
        assertTrue(e.message!!.contains("isn't a Medical Records backup"))
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("not json") }
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode("""{"format":"something-else","version":1,"createdAt":"$t"}""")
        }
    }

    @Test
    fun `rejects backups from a newer version`() {
        val e = assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode("""{"format":"${BackupCodec.FORMAT}","version":99,"createdAt":"$t"}""")
        }
        assertTrue(e.message!!.contains("newer version"))
    }

    @Test
    fun `rejects damaged content`() {
        val good = BackupCodec.encode(contents)
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(good.replace("BLOOD_PRESSURE", "BLOOD_SOMETHING")) }
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(good.replace("\"personId\":2", "\"personId\":99"))
        }
    }
}
