package com.healthtracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.healthtracker.data.local.AppDatabase
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.Medication
import com.healthtracker.data.local.Note
import com.healthtracker.data.local.Person
import com.healthtracker.domain.DateSpan
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.RecordType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DataManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var photos: PhotoStorage
    private lateinit var manager: DataManager
    private val t = Instant.parse("2026-10-05T08:30:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        photos = PhotoStorage(context)
        manager = DataManager(db, photos)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seed(): Long {
        val photo = photos.write(byteArrayOf(9, 8, 7))
        val dad = db.personDao().insert(Person(name = "Dad", photoUri = photo, createdAt = t, updatedAt = t))
        db.healthRecordDao().upsert(
            HealthRecord(personId = dad, type = RecordType.PULSE, value1 = 72.0, unit = MeasureUnit.BPM, dateTime = t, createdAt = t, updatedAt = t),
        )
        db.healthRecordDao().upsert(
            HealthRecord(personId = dad, type = RecordType.WEIGHT, value1 = 71.0, unit = MeasureUnit.KG,
                dateTime = t.minusSeconds(90L * 86400), createdAt = t, updatedAt = t),
        )
        db.noteDao().upsert(Note(personId = dad, title = "Visit", dateTime = t, createdAt = t, updatedAt = t))
        db.medicationDao().upsert(Medication(personId = dad, name = "Amlodipine", createdAt = t, updatedAt = t))
        return dad
    }

    @Test
    fun backupThenRestoreBringsEverythingBack() = runTest {
        seed()
        val out = ByteArrayOutputStream()
        manager.writeBackup(out)

        // Change things after the backup.
        manager.deleteEverything()
        db.personDao().insert(Person(name = "Someone new", createdAt = t, updatedAt = t))

        val backup = manager.readBackup(ByteArrayInputStream(out.toByteArray()))
        manager.restore(backup)

        val people = db.personDao().getAll()
        assertEquals(listOf("Dad"), people.map { it.name })
        assertEquals(2, db.healthRecordDao().getAll().size)
        assertEquals(1, db.noteDao().getAll().size)
        assertEquals(1, db.medicationDao().getAll().size)
        val restoredPhoto = people.single().photoUri
        assertNotNull(restoredPhoto)
        assertArrayEquals(byteArrayOf(9, 8, 7), photos.read(restoredPhoto!!))
    }

    @Test
    fun deleteEverythingRemovesDataAndPhotos() = runTest {
        seed()
        val photo = db.personDao().getAll().single().photoUri!!

        manager.deleteEverything()

        assertTrue(db.personDao().getAll().isEmpty())
        assertTrue(db.healthRecordDao().getAll().isEmpty())
        assertEquals(null, photos.read(photo))
    }

    @Test
    fun exportHonoursDatesAndTypes() = runTest {
        val dad = seed()
        val last30 = DateSpan(LocalDate.of(2026, 9, 6), LocalDate.of(2026, 10, 5))

        val all = manager.loadExport(ExportSelection(dad, last30, RecordType.entries.toSet(), true, true), ZoneOffset.UTC)!!
        assertEquals(listOf(RecordType.PULSE), all.records.map { it.type })
        assertEquals(1, all.notes.size)
        assertEquals(1, all.medications.size)

        val weightOnly = manager.loadExport(ExportSelection(dad, DateSpan(null, last30.to), setOf(RecordType.WEIGHT), false, false), ZoneOffset.UTC)!!
        assertEquals(listOf(RecordType.WEIGHT), weightOnly.records.map { it.type })
        assertTrue(weightOnly.notes.isEmpty() && weightOnly.medications.isEmpty())
        assertNotEquals(true, weightOnly.isEmpty)
    }
}
