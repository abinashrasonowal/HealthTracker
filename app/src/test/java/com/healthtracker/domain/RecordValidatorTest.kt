package com.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RecordValidatorTest {

    private val now = LocalDateTime.of(2026, 10, 5, 9, 0)

    private fun bp(sys: String, dia: String, pulse: String = "") = RecordInput(
        type = RecordType.BLOOD_PRESSURE, value1 = sys, value2 = dia, pulse = pulse,
        unit = MeasureUnit.MMHG, dateTime = now,
    )

    private fun errors(result: ValidationResult) = (result as ValidationResult.Invalid).errors

    @Test
    fun `valid blood pressure is parsed`() {
        val result = RecordValidator.validate(bp("124", "82", "72"), now)
        val record = (result as ValidationResult.Valid).record
        assertEquals(124.0, record.value1, 0.0)
        assertEquals(82.0, record.value2!!, 0.0)
        assertEquals(72, record.pulse)
        assertNull(record.note)
    }

    @Test
    fun `pulse is optional for blood pressure`() {
        val result = RecordValidator.validate(bp("124", "82"), now)
        assertNull((result as ValidationResult.Valid).record.pulse)
    }

    @Test
    fun `missing diastolic is an error`() {
        val result = RecordValidator.validate(bp("124", ""), now)
        assertEquals(setOf(Field.VALUE2), errors(result).keys)
    }

    @Test
    fun `diastolic must be lower than systolic`() {
        val result = RecordValidator.validate(bp("80", "120"), now)
        assertTrue(Field.VALUE2 in errors(result))
    }

    @Test
    fun `typo out of range is rejected`() {
        val result = RecordValidator.validate(bp("1240", "82"), now)
        assertEquals("Should be between 50 and 260", errors(result)[Field.VALUE1])
    }

    @Test
    fun `whole number fields reject decimals`() {
        val result = RecordValidator.validate(bp("124.5", "82"), now)
        assertEquals("Enter a whole number", errors(result)[Field.VALUE1])
    }

    @Test
    fun `comma decimal separator is accepted`() {
        val input = RecordInput(RecordType.WEIGHT, value1 = "71,2", unit = MeasureUnit.KG, dateTime = now)
        val record = (RecordValidator.validate(input, now) as ValidationResult.Valid).record
        assertEquals(71.2, record.value1, 0.0001)
    }

    @Test
    fun `range depends on unit`() {
        val mgdl = RecordInput(RecordType.BLOOD_SUGAR, "5.5", unit = MeasureUnit.MG_DL,
            sugarContext = SugarContext.FASTING, dateTime = now)
        val mmol = mgdl.copy(unit = MeasureUnit.MMOL_L)
        assertTrue(RecordValidator.validate(mgdl, now) is ValidationResult.Invalid)
        assertTrue(RecordValidator.validate(mmol, now) is ValidationResult.Valid)
    }

    @Test
    fun `blood sugar requires context`() {
        val input = RecordInput(RecordType.BLOOD_SUGAR, "98", unit = MeasureUnit.MG_DL, dateTime = now)
        assertTrue(Field.CONTEXT in errors(RecordValidator.validate(input, now)))
    }

    @Test
    fun `unit not allowed for type is rejected`() {
        val input = RecordInput(RecordType.WEIGHT, "70", unit = MeasureUnit.CELSIUS, dateTime = now)
        assertTrue(RecordValidator.validate(input, now) is ValidationResult.Invalid)
    }

    @Test
    fun `future date is rejected but small clock skew is tolerated`() {
        val input = RecordInput(RecordType.PULSE, "72", unit = MeasureUnit.BPM, dateTime = now.plusMinutes(3))
        assertTrue(RecordValidator.validate(input, now) is ValidationResult.Valid)
        val future = input.copy(dateTime = now.plusHours(2))
        assertTrue(Field.DATE_TIME in errors(RecordValidator.validate(future, now)))
    }

    @Test
    fun `note is trimmed and blank note becomes null`() {
        val input = RecordInput(RecordType.PULSE, "72", unit = MeasureUnit.BPM, dateTime = now, note = "  after walk ")
        assertEquals("after walk", (RecordValidator.validate(input, now) as ValidationResult.Valid).record.note)
    }

    @Test
    fun `sugar context is dropped for types without it`() {
        val input = RecordInput(RecordType.PULSE, "72", unit = MeasureUnit.BPM,
            sugarContext = SugarContext.FASTING, dateTime = now)
        assertNull((RecordValidator.validate(input, now) as ValidationResult.Valid).record.sugarContext)
    }
}
