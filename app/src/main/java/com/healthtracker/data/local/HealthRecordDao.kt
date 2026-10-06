package com.healthtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.healthtracker.domain.RecordType
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {

    @Query("SELECT * FROM health_records WHERE personId = :personId ORDER BY dateTime DESC LIMIT :limit")
    fun observeRecent(personId: Long, limit: Int): Flow<List<HealthRecord>>

    /** A person's full history, newest first, limited to [types]. */
    @Query("SELECT * FROM health_records WHERE personId = :personId AND type IN (:types) ORDER BY dateTime DESC, id DESC")
    fun observeHistory(personId: Long, types: List<RecordType>): Flow<List<HealthRecord>>

    /** Every record with its person's name, newest first, for global search. */
    @Query(
        """
        SELECT r.*, p.name AS personName FROM health_records r
        JOIN people p ON p.id = r.personId
        ORDER BY r.dateTime DESC, r.id DESC
        """,
    )
    fun observeAllWithPerson(): Flow<List<RecordWithPerson>>

    /** The most recent record of each type for a person. */
    @Query(
        """
        SELECT * FROM health_records r
        WHERE r.personId = :personId
          AND r.id = (
            SELECT r2.id FROM health_records r2
            WHERE r2.personId = r.personId AND r2.type = r.type
            ORDER BY r2.dateTime DESC, r2.id DESC
            LIMIT 1
          )
        """,
    )
    fun observeLatestPerType(personId: Long): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE id = :id")
    fun observe(id: Long): Flow<HealthRecord?>

    @Query("SELECT * FROM health_records WHERE id = :id")
    suspend fun get(id: Long): HealthRecord?

    @Query("SELECT COUNT(*) FROM health_records WHERE personId = :personId")
    suspend fun countForPerson(personId: Long): Int

    @Upsert
    suspend fun upsert(record: HealthRecord): Long

    @Delete
    suspend fun delete(record: HealthRecord)
}
