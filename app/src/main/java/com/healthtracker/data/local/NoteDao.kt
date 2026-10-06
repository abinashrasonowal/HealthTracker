package com.healthtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE personId = :personId ORDER BY dateTime DESC, id DESC")
    fun observeForPerson(personId: Long): Flow<List<Note>>

    @Query(
        """
        SELECT n.*, p.name AS personName FROM notes n
        JOIN people p ON p.id = n.personId
        ORDER BY n.dateTime DESC, n.id DESC
        """,
    )
    fun observeAllWithPerson(): Flow<List<NoteWithPerson>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observe(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Query("SELECT COUNT(*) FROM notes WHERE personId = :personId")
    suspend fun countForPerson(personId: Long): Int

    @Upsert
    suspend fun upsert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)
}
