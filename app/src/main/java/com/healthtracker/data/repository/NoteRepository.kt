package com.healthtracker.data.repository

import com.healthtracker.data.local.Note
import com.healthtracker.data.local.NoteDao
import com.healthtracker.data.local.NoteWithPerson
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant

class NoteRepository(
    private val dao: NoteDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun observeForPerson(personId: Long): Flow<List<Note>> = dao.observeForPerson(personId)

    fun observeAllWithPerson(): Flow<List<NoteWithPerson>> = dao.observeAllWithPerson()

    fun observe(id: Long): Flow<Note?> = dao.observe(id)

    suspend fun get(id: Long): Note? = dao.get(id)

    suspend fun save(note: Note): Long {
        val now = Instant.now(clock)
        return if (note.id == 0L) {
            dao.upsert(note.copy(createdAt = now, updatedAt = now))
        } else {
            dao.upsert(note.copy(updatedAt = now))
            note.id
        }
    }

    suspend fun delete(note: Note) = dao.delete(note)
}
