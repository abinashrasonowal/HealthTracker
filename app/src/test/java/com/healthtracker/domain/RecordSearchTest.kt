package com.healthtracker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class RecordSearchTest {

    private val date = LocalDate.of(2026, 10, 5)

    private fun matches(
        query: String,
        name: String = "Dad",
        type: RecordType = RecordType.BLOOD_PRESSURE,
        values: List<Double> = listOf(124.0, 82.0),
        note: String? = "Before breakfast",
    ) = RecordSearch.matches(RecordSearch.queryWords(query), name, type, values, note, date, Locale.US)

    @Test
    fun `matches person name prefix case-insensitively`() {
        assertTrue(matches("da"))
        assertTrue(matches("DAD"))
        assertFalse(matches("mom"))
    }

    @Test
    fun `matches record type and aliases`() {
        assertTrue(matches("blood pressure"))
        assertTrue(matches("bp"))
        assertTrue(matches("oxygen", type = RecordType.SPO2))
        assertTrue(matches("glucose", type = RecordType.BLOOD_SUGAR))
        assertFalse(matches("weight"))
    }

    @Test
    fun `aliases match whole words only`() {
        assertFalse(matches("bp", type = RecordType.PULSE, values = listOf(70.0)))
        assertTrue(matches("bpm", type = RecordType.PULSE, values = listOf(70.0)))
    }

    @Test
    fun `all words must match`() {
        assertTrue(matches("dad bp"))
        assertFalse(matches("mom bp"))
    }

    @Test
    fun `matches dates in common formats`() {
        assertTrue(matches("5 oct 2026"))
        assertTrue(matches("october"))
        assertTrue(matches("05/10/2026"))
        assertTrue(matches("2026-10"))
        assertFalse(matches("november"))
    }

    @Test
    fun `single digit does not match inside a longer number`() {
        // "5" is the start of the day "5" here; on 15 October it would not match "2025" or "15".
        assertFalse(
            RecordSearch.matches(
                RecordSearch.queryWords("5"), "Dad", RecordType.WEIGHT, listOf(71.0), null,
                LocalDate.of(2026, 10, 15), Locale.US,
            ),
        )
    }

    @Test
    fun `matches values and note text`() {
        assertTrue(matches("124"))
        assertTrue(matches("breakfast"))
    }

    @Test
    fun `blank query matches nothing`() {
        assertFalse(matches("   "))
    }

    private fun matchesNote(query: String) = RecordSearch.matchesNote(
        RecordSearch.queryWords(query), "Mom", "Doctor Visit", "Continue current medication.", date, Locale.US,
    )

    @Test
    fun `notes match on title, content, person, date and the word note`() {
        assertTrue(matchesNote("doctor"))
        assertTrue(matchesNote("medication"))
        assertTrue(matchesNote("mom visit"))
        assertTrue(matchesNote("5 oct"))
        assertTrue(matchesNote("notes"))
        assertFalse(matchesNote("dad"))
        assertFalse(matchesNote("bp"))
    }
}
