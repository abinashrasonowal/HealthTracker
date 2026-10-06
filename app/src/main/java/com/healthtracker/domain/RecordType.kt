package com.healthtracker.domain

enum class RecordType(val label: String) {
    BLOOD_PRESSURE("Blood Pressure"),
    BLOOD_SUGAR("Blood Sugar"),
    WEIGHT("Weight"),
    PULSE("Pulse"),
    SPO2("SpO₂"),
    TEMPERATURE("Temperature"),
}

enum class MeasureUnit(val symbol: String) {
    MMHG("mmHg"),
    MG_DL("mg/dL"),
    MMOL_L("mmol/L"),
    KG("kg"),
    LB("lb"),
    BPM("bpm"),
    PERCENT("%"),
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
}

enum class SugarContext(val label: String) {
    FASTING("Fasting"),
    BEFORE_MEAL("Before meal"),
    AFTER_MEAL("After meal"),
    RANDOM("Random"),
}

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other"),
}
