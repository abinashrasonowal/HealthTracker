package com.healthtracker.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Matches records and notes against a free-text query. Every word of the query must be
 * the start of some word describing the item: person name, record type or note title,
 * values, note text, or the date in a few common formats. Short names like "bp" must match
 * exactly, so "bp" doesn't also find pulse readings via "bpm".
 */
object RecordSearch {

    private val separators = Regex("[\\s,;:()]+")

    private val aliases = mapOf(
        RecordType.BLOOD_PRESSURE to listOf("bp"),
        RecordType.BLOOD_SUGAR to listOf("sugar", "glucose"),
        RecordType.PULSE to listOf("heart", "bpm"),
        RecordType.SPO2 to listOf("spo2", "oxygen", "saturation"),
        RecordType.TEMPERATURE to listOf("temp"),
    )

    private val noteAliases = listOf("note", "notes")

    private val datePatterns = listOf("d MMMM yyyy", "d MMM yyyy", "dd/MM/yyyy", "yyyy-MM-dd")

    fun queryWords(query: String): List<String> =
        query.lowercase().split(separators).filter { it.isNotEmpty() }

    fun matches(
        queryWords: List<String>,
        personName: String,
        type: RecordType,
        values: List<Double>,
        note: String?,
        date: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): Boolean {
        val words = buildList {
            addAll(words(personName))
            addAll(words(type.label))
            values.forEach { add(formatNumber(it)) }
            note?.let { addAll(words(it)) }
            addAll(dateWords(date, locale))
        }
        return matchesWords(queryWords, words, aliases[type].orEmpty())
    }

    fun matchesNote(
        queryWords: List<String>,
        personName: String,
        title: String,
        content: String?,
        date: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): Boolean {
        val words = buildList {
            addAll(words(personName))
            addAll(words(title))
            content?.let { addAll(words(it)) }
            addAll(dateWords(date, locale))
        }
        return matchesWords(queryWords, words, noteAliases)
    }

    private fun matchesWords(queryWords: List<String>, words: List<String>, exact: List<String>): Boolean =
        queryWords.isNotEmpty() && queryWords.all { q -> q in exact || words.any { it.startsWith(q) } }

    private fun dateWords(date: LocalDate, locale: Locale) =
        datePatterns.flatMap { words(date.format(DateTimeFormatter.ofPattern(it, locale))) }

    private fun words(text: String) = text.lowercase().split(separators).filter { it.isNotEmpty() }
}
