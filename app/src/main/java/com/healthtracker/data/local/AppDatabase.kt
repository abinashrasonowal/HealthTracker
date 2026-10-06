package com.healthtracker.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Person::class, HealthRecord::class, Note::class, Medication::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // adds notes
        AutoMigration(from = 2, to = 3), // adds medications
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun healthRecordDao(): HealthRecordDao
    abstract fun noteDao(): NoteDao
    abstract fun medicationDao(): MedicationDao

    companion object {
        const val NAME = "health_tracker.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
