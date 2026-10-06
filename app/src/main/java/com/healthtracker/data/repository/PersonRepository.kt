package com.healthtracker.data.repository

import com.healthtracker.data.PhotoStorage
import com.healthtracker.data.local.HealthRecordDao
import com.healthtracker.data.local.MedicationDao
import com.healthtracker.data.local.NoteDao
import com.healthtracker.data.local.Person
import com.healthtracker.data.local.PersonDao
import com.healthtracker.data.local.PersonSummary
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant

class PersonRepository(
    private val personDao: PersonDao,
    private val recordDao: HealthRecordDao,
    private val noteDao: NoteDao,
    private val medicationDao: MedicationDao,
    private val photos: PhotoStorage,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun observeSummaries(): Flow<List<PersonSummary>> = personDao.observeSummaries()

    fun observeAll(): Flow<List<Person>> = personDao.observeAll()

    fun observe(id: Long): Flow<Person?> = personDao.observe(id)

    suspend fun get(id: Long): Person? = personDao.get(id)

    suspend fun recordCount(personId: Long): Int = recordDao.countForPerson(personId)

    suspend fun noteCount(personId: Long): Int = noteDao.countForPerson(personId)

    suspend fun medicationCount(personId: Long): Int = medicationDao.countForPerson(personId)

    /** Inserts or updates [person] and returns its id. */
    suspend fun save(person: Person): Long {
        val now = Instant.now(clock)
        return if (person.id == 0L) {
            personDao.insert(person.copy(createdAt = now, updatedAt = now))
        } else {
            personDao.update(person.copy(updatedAt = now))
            person.id
        }
    }

    /** Deletes the person; their records, notes and medications are removed by the foreign-key cascade. */
    suspend fun delete(person: Person) {
        personDao.delete(person)
        photos.delete(person.photoUri)
    }
}
