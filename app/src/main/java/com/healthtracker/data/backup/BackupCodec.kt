package com.healthtracker.data.backup

import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.Medication
import com.healthtracker.data.local.Note
import com.healthtracker.data.local.Person
import com.healthtracker.domain.Gender
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.SugarContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Everything in a backup, as entities ready to insert (ids preserved, photo bytes kept aside). */
data class BackupContents(
    val createdAt: Instant,
    val people: List<Person>,
    val records: List<HealthRecord>,
    val notes: List<Note>,
    val medications: List<Medication>,
    /** Photo bytes by person id; [Person.photoUri] is null until the photo is written to disk. */
    val photos: Map<Long, ByteArray>,
)

class BackupFormatException(message: String) : Exception(message)

/**
 * The backup file format: one JSON document. Dates are ISO strings and enums are names,
 * so the file stays readable and survives changes to the database schema.
 */
@OptIn(ExperimentalEncodingApi::class)
object BackupCodec {

    const val FORMAT = "medical-records-backup"
    const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(contents: BackupContents): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            format = FORMAT,
            version = VERSION,
            createdAt = contents.createdAt.toString(),
            people = contents.people.map { it.toDto(contents.photos[it.id]) },
            records = contents.records.map { it.toDto() },
            notes = contents.notes.map { it.toDto() },
            medications = contents.medications.map { it.toDto() },
        ),
    )

    /** @throws BackupFormatException with a message fit to show the user. */
    fun decode(text: String): BackupContents {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFormatException("This isn't a Medical Records backup file, or it is damaged.")
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("This isn't a Medical Records backup file, or it is damaged.")
        }
        if (file.format != FORMAT) throw BackupFormatException("This isn't a Medical Records backup file.")
        if (file.version > VERSION) {
            throw BackupFormatException("This backup was made by a newer version of the app. Update the app, then try again.")
        }
        return try {
            val people = file.people.map { it.toEntity() }
            val personIds = people.map { it.id }.toSet()
            if (personIds.size != people.size) throw BackupFormatException("The backup file is damaged (duplicate people).")
            val records = file.records.map { it.toEntity() }
            val notes = file.notes.map { it.toEntity() }
            val medications = file.medications.map { it.toEntity() }
            val orphan = records.map { it.personId } + notes.map { it.personId } + medications.map { it.personId }
            if (orphan.any { it !in personIds }) throw BackupFormatException("The backup file is damaged (entries without a person).")
            BackupContents(
                createdAt = Instant.parse(file.createdAt),
                people = people,
                records = records,
                notes = notes,
                medications = medications,
                photos = file.people.mapNotNull { p -> p.photoBase64?.let { p.id to Base64.decode(it) } }.toMap(),
            )
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: RuntimeException) {
            // Unknown enum names, bad dates, bad base64…
            throw BackupFormatException("The backup file is damaged and can't be restored.")
        }
    }

    private fun Person.toDto(photo: ByteArray?) = PersonDto(
        id, name, relationship, dateOfBirth?.toString(), gender?.name, notes,
        createdAt.toString(), updatedAt.toString(), photo?.let { Base64.encode(it) },
    )

    private fun PersonDto.toEntity() = Person(
        id = id,
        name = name,
        relationship = relationship,
        dateOfBirth = dateOfBirth?.let(LocalDate::parse),
        gender = gender?.let(Gender::valueOf),
        photoUri = null,
        notes = notes,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

    private fun HealthRecord.toDto() = RecordDto(
        id, personId, type.name, value1, value2, pulse, unit.name, context?.name, dateTime.toString(), note,
        createdAt.toString(), updatedAt.toString(),
    )

    private fun RecordDto.toEntity() = HealthRecord(
        id = id,
        personId = personId,
        type = RecordType.valueOf(type),
        value1 = value1,
        value2 = value2,
        pulse = pulse,
        unit = MeasureUnit.valueOf(unit),
        context = context?.let(SugarContext::valueOf),
        dateTime = Instant.parse(dateTime),
        note = note,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

    private fun Note.toDto() = NoteDto(id, personId, title, content, dateTime.toString(), createdAt.toString(), updatedAt.toString())

    private fun NoteDto.toEntity() = Note(
        id = id,
        personId = personId,
        title = title,
        content = content,
        dateTime = Instant.parse(dateTime),
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

    private fun Medication.toDto() = MedicationDto(
        id, personId, name, dosage, frequency, startDate?.toString(), endDate?.toString(), notes,
        createdAt.toString(), updatedAt.toString(),
    )

    private fun MedicationDto.toEntity() = Medication(
        id = id,
        personId = personId,
        name = name,
        dosage = dosage,
        frequency = frequency,
        startDate = startDate?.let(LocalDate::parse),
        endDate = endDate?.let(LocalDate::parse),
        notes = notes,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )
}

@Serializable
private data class BackupFile(
    val format: String,
    val version: Int,
    val createdAt: String,
    val people: List<PersonDto> = emptyList(),
    val records: List<RecordDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val medications: List<MedicationDto> = emptyList(),
)

@Serializable
private data class PersonDto(
    val id: Long,
    val name: String,
    val relationship: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val photoBase64: String? = null,
)

@Serializable
private data class RecordDto(
    val id: Long,
    val personId: Long,
    val type: String,
    val value1: Double,
    val value2: Double? = null,
    val pulse: Int? = null,
    val unit: String,
    val context: String? = null,
    val dateTime: String,
    val note: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
private data class NoteDto(
    val id: Long,
    val personId: Long,
    val title: String,
    val content: String? = null,
    val dateTime: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
private data class MedicationDto(
    val id: Long,
    val personId: Long,
    val name: String,
    val dosage: String? = null,
    val frequency: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
