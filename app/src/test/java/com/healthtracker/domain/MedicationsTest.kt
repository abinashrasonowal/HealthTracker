package com.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MedicationsTest {

    private val today = LocalDate.of(2026, 10, 5)

    @Test
    fun `current until the stop date arrives`() {
        assertTrue(isMedicationActive(null, today))
        assertTrue(isMedicationActive(today.plusDays(3), today))
        assertFalse("stopped today", isMedicationActive(today, today))
        assertFalse(isMedicationActive(today.minusDays(1), today))
    }

    @Test
    fun `name is required`() {
        assertEquals(setOf(MedicationField.NAME), MedicationValidator.validate("  ", null, null).keys)
        assertTrue(MedicationValidator.validate("Amlodipine", null, null).isEmpty())
    }

    @Test
    fun `end date must not precede start date`() {
        val errors = MedicationValidator.validate("Metformin", today, today.minusDays(1))
        assertEquals(setOf(MedicationField.END_DATE), errors.keys)
        assertTrue(MedicationValidator.validate("Metformin", today, today).isEmpty())
        assertTrue(MedicationValidator.validate("Metformin", null, today.minusDays(1)).isEmpty())
    }

    @Test
    fun `started and stopped the same day is valid`() {
        assertTrue(MedicationValidator.validate("Metformin", today, today).isEmpty())
    }
}
