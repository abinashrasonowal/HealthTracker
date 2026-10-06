package com.healthtracker.ui.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.repository.HealthRecordRepository
import com.healthtracker.domain.Field
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.RecordInput
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.RecordValidator
import com.healthtracker.domain.SugarContext
import com.healthtracker.domain.ValidationResult
import com.healthtracker.domain.formatNumber
import com.healthtracker.domain.spec
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.navigation.RecordEditRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class RecordEditState(
    val type: RecordType,
    val isNew: Boolean,
    val loading: Boolean,
    val value1: String = "",
    val value2: String = "",
    val pulse: String = "",
    val unit: MeasureUnit = type.spec.units.first(),
    val sugarContext: SugarContext? = if (type.spec.hasSugarContext) SugarContext.RANDOM else null,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES),
    val note: String = "",
    val errors: Map<Field, String> = emptyMap(),
    val done: Boolean = false,
)

class RecordEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HealthRecordRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<RecordEditRoute>()
    private var original: HealthRecord? = null

    private val _state = MutableStateFlow(
        RecordEditState(
            // When editing, the real type arrives with the record; BLOOD_PRESSURE is only a placeholder.
            type = route.type?.let(RecordType::valueOf) ?: RecordType.BLOOD_PRESSURE,
            isNew = route.recordId == 0L,
            loading = route.recordId != 0L,
        ),
    )
    val state: StateFlow<RecordEditState> = _state.asStateFlow()

    init {
        if (route.recordId != 0L) {
            viewModelScope.launch {
                val record = repository.get(route.recordId)
                if (record == null) {
                    _state.update { it.copy(loading = false, done = true) }
                    return@launch
                }
                original = record
                val local = record.dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
                _state.value = RecordEditState(
                    type = record.type,
                    isNew = false,
                    loading = false,
                    value1 = formatNumber(record.value1),
                    value2 = record.value2?.let(::formatNumber).orEmpty(),
                    pulse = record.pulse?.toString().orEmpty(),
                    unit = record.unit,
                    sugarContext = record.context,
                    date = local.toLocalDate(),
                    time = local.toLocalTime(),
                    note = record.note.orEmpty(),
                )
            }
        }
    }

    fun onValue1Change(v: String) = _state.update { it.copy(value1 = v, errors = it.errors - Field.VALUE1) }
    fun onValue2Change(v: String) = _state.update { it.copy(value2 = v, errors = it.errors - Field.VALUE2) }
    fun onPulseChange(v: String) = _state.update { it.copy(pulse = v, errors = it.errors - Field.PULSE) }
    fun onUnitChange(v: MeasureUnit) = _state.update { it.copy(unit = v, errors = it.errors - Field.VALUE1) }
    fun onContextChange(v: SugarContext) = _state.update { it.copy(sugarContext = v, errors = it.errors - Field.CONTEXT) }
    fun onDateChange(v: LocalDate) = _state.update { it.copy(date = v, errors = it.errors - Field.DATE_TIME) }
    fun onTimeChange(v: LocalTime) = _state.update { it.copy(time = v, errors = it.errors - Field.DATE_TIME) }
    fun onNoteChange(v: String) = _state.update { it.copy(note = v) }

    fun save() {
        val s = _state.value
        val input = RecordInput(
            type = s.type,
            value1 = s.value1,
            value2 = s.value2,
            pulse = s.pulse,
            unit = s.unit,
            sugarContext = s.sugarContext,
            dateTime = LocalDateTime.of(s.date, s.time),
            note = s.note,
        )
        when (val result = RecordValidator.validate(input, LocalDateTime.now())) {
            is ValidationResult.Invalid -> _state.update { it.copy(errors = result.errors) }
            is ValidationResult.Valid -> viewModelScope.launch {
                val parsed = result.record
                val now = Instant.now()
                val base = original ?: HealthRecord(
                    personId = route.personId,
                    type = s.type,
                    value1 = parsed.value1,
                    unit = parsed.unit,
                    dateTime = now,
                    createdAt = now,
                    updatedAt = now,
                )
                repository.save(
                    base.copy(
                        value1 = parsed.value1,
                        value2 = parsed.value2,
                        pulse = parsed.pulse,
                        unit = parsed.unit,
                        context = parsed.sugarContext,
                        dateTime = parsed.dateTime.atZone(ZoneId.systemDefault()).toInstant(),
                        note = parsed.note,
                    ),
                )
                _state.update { it.copy(done = true) }
            }
        }
    }

    companion object {
        val Factory = appViewModelFactory { c, handle -> RecordEditViewModel(handle, c.recordRepository) }
    }
}
