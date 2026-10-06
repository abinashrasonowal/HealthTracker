package com.healthtracker.domain

import java.time.LocalDate

/**
 * A medication's [endDate] is its stop date: the day it was (or will be) stopped.
 * It counts as current until that day arrives, so "Stop taking" can simply record today.
 */
fun isMedicationActive(endDate: LocalDate?, today: LocalDate): Boolean =
    endDate == null || endDate.isAfter(today)

val frequencySuggestions = listOf("Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed")

enum class MedicationField { NAME, END_DATE }

object MedicationValidator {
    fun validate(name: String, startDate: LocalDate?, endDate: LocalDate?): Map<MedicationField, String> = buildMap {
        if (name.isBlank()) put(MedicationField.NAME, "Enter the medicine name")
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            put(MedicationField.END_DATE, "Stop date can't be before the start date")
        }
    }
}
