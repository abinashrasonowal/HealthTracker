package com.healthtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.healthtracker.domain.AppSettings
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val store = context.settingsStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            theme = p[THEME].toEnum(defaults.theme),
            weightUnit = p[WEIGHT].toEnum(defaults.weightUnit),
            temperatureUnit = p[TEMPERATURE].toEnum(defaults.temperatureUnit),
            glucoseUnit = p[GLUCOSE].toEnum(defaults.glucoseUnit),
        )
    }

    suspend fun setTheme(theme: ThemeMode) = store.edit { it[THEME] = theme.name }
    suspend fun setWeightUnit(unit: MeasureUnit) = store.edit { it[WEIGHT] = unit.name }
    suspend fun setTemperatureUnit(unit: MeasureUnit) = store.edit { it[TEMPERATURE] = unit.name }
    suspend fun setGlucoseUnit(unit: MeasureUnit) = store.edit { it[GLUCOSE] = unit.name }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val WEIGHT = stringPreferencesKey("weight_unit")
        val TEMPERATURE = stringPreferencesKey("temperature_unit")
        val GLUCOSE = stringPreferencesKey("glucose_unit")
    }
}
