package com.healthtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    /** Most recently started first; medications without a start date go last, by name. */
    @Query(
        """
        SELECT * FROM medications WHERE personId = :personId
        ORDER BY startDate IS NULL, startDate DESC, name COLLATE NOCASE
        """,
    )
    fun observeForPerson(personId: Long): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun observe(id: Long): Flow<Medication?>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun get(id: Long): Medication?

    @Query("SELECT COUNT(*) FROM medications WHERE personId = :personId")
    suspend fun countForPerson(personId: Long): Int

    @Upsert
    suspend fun upsert(medication: Medication): Long

    @Delete
    suspend fun delete(medication: Medication)
}
