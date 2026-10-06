package com.healthtracker.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.healthtracker.domain.Gender
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.SugarContext
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "people")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: Gender? = null,
    val photoUri: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * One measurement. Meaning of the value columns depends on [type]:
 * - BLOOD_PRESSURE: value1 = systolic, value2 = diastolic, pulse = optional pulse
 * - SPO2: value1 = saturation %, pulse = optional pulse
 * - everything else: value1 only
 */
@Entity(
    tableName = "health_records",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personId", "type", "dateTime")],
)
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: RecordType,
    val value1: Double,
    val value2: Double? = null,
    val pulse: Int? = null,
    val unit: MeasureUnit,
    val context: SugarContext? = null,
    val dateTime: Instant,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PersonSummary(
    @Embedded val person: Person,
    val lastRecordAt: Instant?,
)

data class RecordWithPerson(
    @Embedded val record: HealthRecord,
    val personName: String,
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personId", "dateTime")],
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val title: String,
    val content: String? = null,
    val dateTime: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class NoteWithPerson(
    @Embedded val note: Note,
    val personName: String,
)

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personId")],
)
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val name: String,
    val dosage: String? = null,
    val frequency: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
