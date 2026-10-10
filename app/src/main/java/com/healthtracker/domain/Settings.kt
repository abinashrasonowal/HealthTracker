package com.healthtracker.domain

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val weightUnit: MeasureUnit = MeasureUnit.KG,
    val temperatureUnit: MeasureUnit = MeasureUnit.CELSIUS,
    val glucoseUnit: MeasureUnit = MeasureUnit.MG_DL,
) {
    /** The unit new records and charts of [type] should use. */
    fun preferredUnit(type: RecordType): MeasureUnit = when (type) {
        RecordType.WEIGHT -> weightUnit
        RecordType.TEMPERATURE -> temperatureUnit
        RecordType.BLOOD_SUGAR -> glucoseUnit
        else -> type.spec.units.first()
    }
}
