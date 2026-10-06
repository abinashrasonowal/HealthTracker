package com.healthtracker.data.repository

import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.HealthRecordDao
import com.healthtracker.data.local.RecordWithPerson
import com.healthtracker.domain.RecordType
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant

class HealthRecordRepository(
    private val dao: HealthRecordDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun observeRecent(personId: Long, limit: Int = 20): Flow<List<HealthRecord>> = dao.observeRecent(personId, limit)

    fun observeHistory(personId: Long, types: Collection<RecordType> = RecordType.entries): Flow<List<HealthRecord>> =
        dao.observeHistory(personId, types.toList())

    fun observeAllWithPerson(): Flow<List<RecordWithPerson>> = dao.observeAllWithPerson()

    fun observeLatestPerType(personId: Long): Flow<List<HealthRecord>> = dao.observeLatestPerType(personId)

    fun observe(id: Long): Flow<HealthRecord?> = dao.observe(id)

    suspend fun get(id: Long): HealthRecord? = dao.get(id)

    suspend fun save(record: HealthRecord): Long {
        val now = Instant.now(clock)
        return if (record.id == 0L) {
            dao.upsert(record.copy(createdAt = now, updatedAt = now))
        } else {
            dao.upsert(record.copy(updatedAt = now))
            record.id
        }
    }

    suspend fun delete(record: HealthRecord) = dao.delete(record)
}
