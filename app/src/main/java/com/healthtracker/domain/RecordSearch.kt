package com.healthtracker.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
        }
        return matchesWords(queryWords, words, aliases[type].orEmpty(), date, locale)
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
        }
        return matchesWords(queryWords, words, noteAliases, date, locale)
    }

    /**
     * Search runs over thousands of items per keystroke, and formatting dates is the costly part,
     * so a date is only formatted when a query word could be part of one.
     */
    private fun matchesWords(
        queryWords: List<String>,
        words: List<String>,
        exact: List<String>,
        date: LocalDate,
        locale: Locale,
    ): Boolean {
        if (queryWords.isEmpty()) return false
        val dateWords by lazy { dateWords(date, locale) }
        return queryWords.all { q ->
            q in exact || words.any { it.startsWith(q) } ||
                (couldBeInDate(q, locale) && dateWords.any { it.startsWith(q) })
        }
    }

    private class LocaleDates(val formatters: List<DateTimeFormatter>, val monthNames: List<String>)

    private val localeDates = ConcurrentHashMap<Locale, LocaleDates>()

    private fun forLocale(locale: Locale) = localeDates.getOrPut(locale) {
        val months = (1..12).flatMap { m ->
            val d = LocalDate.of(2000, m, 1)
            listOf("MMMM", "MMM").map { d.format(DateTimeFormatter.ofPattern(it, locale)).lowercase() }
        }
        LocaleDates(datePatterns.map { DateTimeFormatter.ofPattern(it, locale) }, months)
    }

    /** Dates contain only numbers (with - or /) and month names. */
    private fun couldBeInDate(q: String, locale: Locale) =
        q.all { it.isDigit() || it == '-' || it == '/' } || forLocale(locale).monthNames.any { it.startsWith(q) }

    private fun dateWords(date: LocalDate, locale: Locale) =
        forLocale(locale).formatters.flatMap { words(date.format(it)) }

    private fun words(text: String) = text.lowercase().split(separators).filter { it.isNotEmpty() }
}
