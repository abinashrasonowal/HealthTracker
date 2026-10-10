package com.healthtracker

import android.content.Context
import com.healthtracker.data.DataManager
import com.healthtracker.data.PhotoStorage
import com.healthtracker.data.SettingsRepository
import com.healthtracker.data.local.AppDatabase
import com.healthtracker.data.repository.HealthRecordRepository
import com.healthtracker.data.repository.MedicationRepository
import com.healthtracker.data.repository.NoteRepository
import com.healthtracker.data.repository.PersonRepository

/** Manual dependency container; one instance lives in [HealthTrackerApp]. */
class AppContainer(context: Context) {
    /** For reading and writing files the user picks (export, backup, restore). */
    val contentResolver = context.applicationContext.contentResolver

    private val database = AppDatabase.create(context)

    val photoStorage = PhotoStorage(context)
    val personRepository = PersonRepository(database.personDao(), database.healthRecordDao(), database.noteDao(), database.medicationDao(), photoStorage)
    val recordRepository = HealthRecordRepository(database.healthRecordDao())
    val noteRepository = NoteRepository(database.noteDao())
    val medicationRepository = MedicationRepository(database.medicationDao())
    val settingsRepository = SettingsRepository(context)
    val dataManager = DataManager(database, photoStorage)
}
