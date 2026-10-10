package com.healthtracker.data

import androidx.room.withTransaction
import com.healthtracker.data.backup.BackupCodec
import com.healthtracker.data.backup.BackupContents
import com.healthtracker.data.local.AppDatabase
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.Medication
import com.healthtracker.data.local.Note
import com.healthtracker.data.local.Person
import com.healthtracker.domain.DateSpan
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.medicationOverlaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId

/** What to put in an export. */
data class ExportSelection(
    val personId: Long,
    val span: DateSpan,
    val types: Set<RecordType>,
    val includeNotes: Boolean,
    val includeMedications: Boolean,
)

data class ExportData(
    val person: Person,
    val span: DateSpan,
    val records: List<HealthRecord>,
    val notes: List<Note>,
    val medications: List<Medication>,
) {
    val isEmpty get() = records.isEmpty() && notes.isEmpty() && medications.isEmpty()
}

/** Whole-database operations: export, backup, restore and delete everything. */
class DataManager(
    private val db: AppDatabase,
    private val photos: PhotoStorage,
) {

    suspend fun loadExport(selection: ExportSelection, zone: ZoneId = ZoneId.systemDefault()): ExportData? {
        val person = db.personDao().get(selection.personId) ?: return null
        val span = selection.span
        val from = span.from?.atStartOfDay(zone)?.toInstant() ?: Instant.EPOCH
        val to = span.to.plusDays(1).atStartOfDay(zone).toInstant()
        val records = if (selection.types.isEmpty()) {
            emptyList()
        } else {
            db.healthRecordDao().getForExport(person.id, selection.types.toList(), from, to)
        }
        val notes = if (selection.includeNotes) db.noteDao().getForExport(person.id, from, to) else emptyList()
        val medications = if (selection.includeMedications) {
            db.medicationDao().getForPerson(person.id).filter { medicationOverlaps(it.startDate, it.endDate, span) }
        } else {
            emptyList()
        }
        return ExportData(person, span, records, notes, medications)
    }

    suspend fun writeBackup(out: OutputStream) = withContext(Dispatchers.IO) {
        val people = db.personDao().getAll()
        val contents = BackupContents(
            createdAt = Instant.now(),
            people = people,
            records = db.healthRecordDao().getAll(),
            notes = db.noteDao().getAll(),
            medications = db.medicationDao().getAll(),
            photos = people.mapNotNull { p -> p.photoUri?.let { uri -> photos.read(uri)?.let { p.id to it } } }.toMap(),
        )
        out.bufferedWriter().use { it.write(BackupCodec.encode(contents)) }
    }

    /** Reads and checks a backup without changing anything. @throws BackupFormatException */
    suspend fun readBackup(input: InputStream): BackupContents = withContext(Dispatchers.IO) {
        BackupCodec.decode(input.bufferedReader().use { it.readText() })
    }

    /** Replaces everything in the app with [backup]. All or nothing. */
    suspend fun restore(backup: BackupContents) {
        val oldPhotos = db.personDao().getAll().mapNotNull { it.photoUri }
        val newPhotos = mutableMapOf<Long, String>()
        try {
            backup.photos.forEach { (personId, bytes) -> photos.write(bytes)?.let { newPhotos[personId] = it } }
            db.withTransaction {
                db.personDao().deleteAll()
                db.personDao().insertAll(backup.people.map { it.copy(photoUri = newPhotos[it.id]) })
                db.healthRecordDao().insertAll(backup.records)
                db.noteDao().insertAll(backup.notes)
                db.medicationDao().insertAll(backup.medications)
            }
        } catch (e: Exception) {
            newPhotos.values.forEach { photos.delete(it) }
            throw e
        }
        oldPhotos.forEach { photos.delete(it) }
    }

    suspend fun deleteEverything() {
        val oldPhotos = db.personDao().getAll().mapNotNull { it.photoUri }
        db.personDao().deleteAll()
        oldPhotos.forEach { photos.delete(it) }
    }
}
