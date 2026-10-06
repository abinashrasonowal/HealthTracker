package com.healthtracker.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2KeepsDataAndAddsNotes() {
        helper.createDatabase(DB, 1).apply {
            execSQL("INSERT INTO people (id, name, createdAt, updatedAt) VALUES (1, 'Dad', 0, 0)")
            execSQL(
                "INSERT INTO health_records (personId, type, value1, unit, dateTime, createdAt, updatedAt) " +
                    "VALUES (1, 'PULSE', 72, 'BPM', 0, 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true)

        db.query("SELECT value1 FROM health_records WHERE personId = 1").use {
            it.moveToFirst()
            assertEquals(72.0, it.getDouble(0), 0.0)
        }
        db.execSQL("INSERT INTO notes (personId, title, dateTime, createdAt, updatedAt) VALUES (1, 'Doctor Visit', 0, 0, 0)")
        db.query("SELECT COUNT(*) FROM notes").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun migrate2To3AddsMedications() {
        helper.createDatabase(DB, 2).apply {
            execSQL("INSERT INTO people (id, name, createdAt, updatedAt) VALUES (1, 'Dad', 0, 0)")
            execSQL("INSERT INTO notes (personId, title, dateTime, createdAt, updatedAt) VALUES (1, 'Visit', 0, 0, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 3, true)

        db.query("SELECT COUNT(*) FROM notes").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.execSQL("INSERT INTO medications (personId, name, createdAt, updatedAt) VALUES (1, 'Amlodipine', 0, 0)")
        // The helper's raw connection doesn't enable foreign keys; Room does in the app.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM people WHERE id = 1")
        db.query("SELECT COUNT(*) FROM medications").use {
            it.moveToFirst()
            assertEquals("medications cascade with their person", 0, it.getInt(0))
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
