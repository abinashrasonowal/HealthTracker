package com.healthtracker.domain

/**
 * Describes the input fields for one record type.
 *
 * Ranges exist only to catch typing mistakes (e.g. "1240" instead of "124");
 * they are deliberately wide and say nothing about what is healthy.
 */
data class FieldSpec(
    val label: String,
    val required: Boolean,
    val allowDecimals: Boolean,
    val range: (MeasureUnit) -> ClosedFloatingPointRange<Double>,
)

data class RecordSpec(
    val value1: FieldSpec,
    val value2: FieldSpec? = null,
    val pulse: FieldSpec? = null,
    val units: List<MeasureUnit>,
    val hasSugarContext: Boolean = false,
)

private val pulseField = FieldSpec("Pulse (bpm)", required = false, allowDecimals = false) { 20.0..250.0 }

val RecordType.spec: RecordSpec
    get() = when (this) {
        RecordType.BLOOD_PRESSURE -> RecordSpec(
            value1 = FieldSpec("Systolic", required = true, allowDecimals = false) { 50.0..260.0 },
            value2 = FieldSpec("Diastolic", required = true, allowDecimals = false) { 30.0..200.0 },
            pulse = pulseField,
            units = listOf(MeasureUnit.MMHG),
        )
        RecordType.BLOOD_SUGAR -> RecordSpec(
            value1 = FieldSpec("Blood sugar", required = true, allowDecimals = true) { unit ->
                if (unit == MeasureUnit.MMOL_L) 0.5..55.0 else 10.0..1000.0
            },
            units = listOf(MeasureUnit.MG_DL, MeasureUnit.MMOL_L),
            hasSugarContext = true,
        )
        RecordType.WEIGHT -> RecordSpec(
            value1 = FieldSpec("Weight", required = true, allowDecimals = true) { unit ->
                if (unit == MeasureUnit.LB) 1.0..900.0 else 0.5..400.0
            },
            units = listOf(MeasureUnit.KG, MeasureUnit.LB),
        )
        RecordType.PULSE -> RecordSpec(
            value1 = FieldSpec("Pulse", required = true, allowDecimals = false) { 20.0..250.0 },
            units = listOf(MeasureUnit.BPM),
        )
        RecordType.SPO2 -> RecordSpec(
            value1 = FieldSpec("Oxygen saturation", required = true, allowDecimals = false) { 50.0..100.0 },
            pulse = pulseField,
            units = listOf(MeasureUnit.PERCENT),
        )
        RecordType.TEMPERATURE -> RecordSpec(
            value1 = FieldSpec("Temperature", required = true, allowDecimals = true) { unit ->
                if (unit == MeasureUnit.FAHRENHEIT) 86.0..113.0 else 30.0..45.0
            },
            units = listOf(MeasureUnit.CELSIUS, MeasureUnit.FAHRENHEIT),
        )
    }
