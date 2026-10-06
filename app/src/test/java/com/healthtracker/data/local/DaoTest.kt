package com.healthtracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.RecordType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DaoTest {

    private lateinit var db: AppDatabase
    private val people get() = db.personDao()
    private val records get() = db.healthRecordDao()
    private val t0 = Instant.parse("2026-10-01T08:00:00Z")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun addPerson(name: String, createdAt: Instant = t0) =
        people.insert(Person(name = name, createdAt = createdAt, updatedAt = createdAt))

    private suspend fun addRecord(personId: Long, type: RecordType, value: Double, at: Instant) =
        records.upsert(
            HealthRecord(
                personId = personId, type = type, value1 = value, unit = MeasureUnit.MMHG,
                dateTime = at, createdAt = at, updatedAt = at,
            ),
        )

    @Test
    fun latestPerTypeReturnsNewestRecordOfEachType() = runTest {
        val dad = addPerson("Dad")
        val mom = addPerson("Mom")
        addRecord(dad, RecordType.BLOOD_PRESSURE, 120.0, t0)
        addRecord(dad, RecordType.BLOOD_PRESSURE, 130.0, t0.plusSeconds(3600))
        addRecord(dad, RecordType.BLOOD_PRESSURE, 110.0, t0.minusSeconds(3600))
        addRecord(dad, RecordType.PULSE, 72.0, t0)
        addRecord(mom, RecordType.PULSE, 90.0, t0.plusSeconds(7200))

        val latest = records.observeLatestPerType(dad).first().associateBy { it.type }

        assertEquals(2, latest.size)
        assertEquals(130.0, latest.getValue(RecordType.BLOOD_PRESSURE).value1, 0.0)
        assertEquals(72.0, latest.getValue(RecordType.PULSE).value1, 0.0)
    }

    @Test
    fun latestPerTypeReturnsOneRowWhenTimestampsTie() = runTest {
        val dad = addPerson("Dad")
        addRecord(dad, RecordType.PULSE, 70.0, t0)
        addRecord(dad, RecordType.PULSE, 75.0, t0)

        val latest = records.observeLatestPerType(dad).first()

        assertEquals(1, latest.size)
    }

    @Test
    fun summariesIncludeLastRecordTime() = runTest {
        val dad = addPerson("Dad")
        addPerson("Mom", createdAt = t0.plusSeconds(1))
        addRecord(dad, RecordType.PULSE, 70.0, t0)
        addRecord(dad, RecordType.PULSE, 70.0, t0.plusSeconds(60))

        val summaries = people.observeSummaries().first()

        assertEquals(listOf("Dad", "Mom"), summaries.map { it.person.name })
        assertEquals(t0.plusSeconds(60), summaries[0].lastRecordAt)
        assertNull(summaries[1].lastRecordAt)
    }

    @Test
    fun deletingPersonCascadesToRecords() = runTest {
        val dad = addPerson("Dad")
        val mom = addPerson("Mom")
        val momRecord = addRecord(mom, RecordType.PULSE, 80.0, t0)
        addRecord(dad, RecordType.PULSE, 70.0, t0)
        addRecord(dad, RecordType.WEIGHT, 71.0, t0)

        people.delete(people.get(dad)!!)

        assertEquals(0, records.countForPerson(dad))
        assertEquals(1, records.countForPerson(mom))
        assertEquals(80.0, records.get(momRecord)!!.value1, 0.0)
    }

    @Test
    fun recentIsNewestFirstAndLimited() = runTest {
        val dad = addPerson("Dad")
        repeat(5) { addRecord(dad, RecordType.PULSE, 60.0 + it, t0.plusSeconds(it * 60L)) }

        val recent = records.observeRecent(dad, limit = 3).first()

        assertEquals(listOf(64.0, 63.0, 62.0), recent.map { it.value1 })
    }

    @Test
    fun historyFiltersByTypeNewestFirst() = runTest {
        val dad = addPerson("Dad")
        addRecord(dad, RecordType.PULSE, 70.0, t0)
        addRecord(dad, RecordType.WEIGHT, 71.0, t0.plusSeconds(60))
        addRecord(dad, RecordType.PULSE, 75.0, t0.plusSeconds(120))

        val pulses = records.observeHistory(dad, listOf(RecordType.PULSE)).first()
        val all = records.observeHistory(dad, RecordType.entries).first()

        assertEquals(listOf(75.0, 70.0), pulses.map { it.value1 })
        assertEquals(listOf(75.0, 71.0, 70.0), all.map { it.value1 })
    }

    @Test
    fun allWithPersonJoinsNames() = runTest {
        val dad = addPerson("Dad")
        val mom = addPerson("Mom")
        addRecord(dad, RecordType.PULSE, 70.0, t0)
        addRecord(mom, RecordType.PULSE, 80.0, t0.plusSeconds(60))

        val rows = records.observeAllWithPerson().first()

        assertEquals(listOf("Mom" to 80.0, "Dad" to 70.0), rows.map { it.personName to it.record.value1 })
    }

    private suspend fun addNote(personId: Long, title: String, at: Instant) =
        db.noteDao().upsert(Note(personId = personId, title = title, dateTime = at, createdAt = at, updatedAt = at))

    @Test
    fun notesAreNewestFirstAndCascadeWithPerson() = runTest {
        val dad = addPerson("Dad")
        val mom = addPerson("Mom")
        addNote(dad, "Old visit", t0)
        addNote(dad, "New visit", t0.plusSeconds(60))
        addNote(mom, "Mom visit", t0)

        assertEquals(listOf("New visit", "Old visit"), db.noteDao().observeForPerson(dad).first().map { it.title })
        assertEquals(
            // Same timestamp: the later-inserted note (higher id) comes first.
            listOf("Dad" to "New visit", "Mom" to "Mom visit", "Dad" to "Old visit"),
            db.noteDao().observeAllWithPerson().first().map { it.personName to it.note.title },
        )

        people.delete(people.get(dad)!!)

        assertEquals(0, db.noteDao().countForPerson(dad))
        assertEquals(1, db.noteDao().countForPerson(mom))
    }
}
