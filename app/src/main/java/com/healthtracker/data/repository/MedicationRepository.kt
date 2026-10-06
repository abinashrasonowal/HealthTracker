package com.healthtracker.data.repository

import com.healthtracker.data.local.Medication
import com.healthtracker.data.local.MedicationDao
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant

class MedicationRepository(
    private val dao: MedicationDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun observeForPerson(personId: Long): Flow<List<Medication>> = dao.observeForPerson(personId)

    fun observe(id: Long): Flow<Medication?> = dao.observe(id)

    suspend fun get(id: Long): Medication? = dao.get(id)

    suspend fun save(medication: Medication): Long {
        val now = Instant.now(clock)
        return if (medication.id == 0L) {
            dao.upsert(medication.copy(createdAt = now, updatedAt = now))
        } else {
            dao.upsert(medication.copy(updatedAt = now))
            medication.id
        }
    }

    suspend fun delete(medication: Medication) = dao.delete(medication)
}
