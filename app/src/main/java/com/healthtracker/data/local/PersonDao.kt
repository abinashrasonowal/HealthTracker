package com.healthtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query(
        """
        SELECT p.*, (SELECT MAX(r.dateTime) FROM health_records r WHERE r.personId = p.id) AS lastRecordAt
        FROM people p
        ORDER BY p.createdAt
        """,
    )
    fun observeSummaries(): Flow<List<PersonSummary>>

    @Query("SELECT * FROM people ORDER BY createdAt")
    fun observeAll(): Flow<List<Person>>

    @Query("SELECT * FROM people WHERE id = :id")
    fun observe(id: Long): Flow<Person?>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun get(id: Long): Person?

    @Insert
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Delete
    suspend fun delete(person: Person)
}
