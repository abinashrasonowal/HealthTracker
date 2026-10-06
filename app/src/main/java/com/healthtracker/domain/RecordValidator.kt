package com.healthtracker.domain

import java.time.LocalDateTime

/** Raw text the user typed into the add/edit record form. */
data class RecordInput(
    val type: RecordType,
    val value1: String,
    val value2: String = "",
    val pulse: String = "",
    val unit: MeasureUnit,
    val sugarContext: SugarContext? = null,
    val dateTime: LocalDateTime,
    val note: String = "",
)

data class ParsedRecord(
    val value1: Double,
    val value2: Double?,
    val pulse: Int?,
    val unit: MeasureUnit,
    val sugarContext: SugarContext?,
    val dateTime: LocalDateTime,
    val note: String?,
)

enum class Field { VALUE1, VALUE2, PULSE, CONTEXT, DATE_TIME }

sealed interface ValidationResult {
    data class Valid(val record: ParsedRecord) : ValidationResult
    data class Invalid(val errors: Map<Field, String>) : ValidationResult
}

object RecordValidator {

    fun validate(input: RecordInput, now: LocalDateTime): ValidationResult {
        val spec = input.type.spec
        val errors = mutableMapOf<Field, String>()

        val value1 = parseField(input.value1, spec.value1, input.unit, Field.VALUE1, errors)
        val value2 = spec.value2?.let { parseField(input.value2, it, input.unit, Field.VALUE2, errors) }
        val pulse = spec.pulse?.let { parseField(input.pulse, it, input.unit, Field.PULSE, errors) }

        if (input.unit !in spec.units) {
            errors[Field.VALUE1] = "Unsupported unit"
        }
        if (input.type == RecordType.BLOOD_PRESSURE && value1 != null && value2 != null && value2 >= value1) {
            errors[Field.VALUE2] = "Diastolic should be lower than systolic"
        }
        if (spec.hasSugarContext && input.sugarContext == null) {
            errors[Field.CONTEXT] = "Choose when it was measured"
        }
        if (input.dateTime.isAfter(now.plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            errors[Field.DATE_TIME] = "Date and time can't be in the future"
        }

        if (errors.isNotEmpty() || value1 == null) return ValidationResult.Invalid(errors)

        return ValidationResult.Valid(
            ParsedRecord(
                value1 = value1,
                value2 = value2,
                pulse = pulse?.toInt(),
                unit = input.unit,
                sugarContext = if (spec.hasSugarContext) input.sugarContext else null,
                dateTime = input.dateTime,
                note = input.note.trim().ifEmpty { null },
            ),
        )
    }

    private fun parseField(
        text: String,
        spec: FieldSpec,
        unit: MeasureUnit,
        field: Field,
        errors: MutableMap<Field, String>,
    ): Double? {
        val trimmed = text.trim().replace(',', '.')
        if (trimmed.isEmpty()) {
            if (spec.required) errors[field] = "Enter ${spec.label.lowercase()}"
            return null
        }
        val number = trimmed.toDoubleOrNull()
        if (number == null || (!spec.allowDecimals && number % 1.0 != 0.0)) {
            errors[field] = if (spec.allowDecimals) "Enter a number" else "Enter a whole number"
            return null
        }
        val range = spec.range(unit)
        if (number !in range) {
            errors[field] = "Should be between ${formatNumber(range.start)} and ${formatNumber(range.endInclusive)}"
            return null
        }
        return number
    }

    private const val FUTURE_TOLERANCE_MINUTES = 5L
}
