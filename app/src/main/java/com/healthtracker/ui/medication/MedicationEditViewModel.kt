package com.healthtracker.ui.medication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.local.Medication
import com.healthtracker.data.repository.MedicationRepository
import com.healthtracker.domain.MedicationField
import com.healthtracker.domain.MedicationValidator
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.savedForm
import com.healthtracker.ui.navigation.MedicationEditRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate

data class MedicationEditState(
    val isNew: Boolean,
    val loading: Boolean,
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val startDate: LocalDate? = LocalDate.now(),
    val endDate: LocalDate? = null,
    val notes: String = "",
    val errors: Map<MedicationField, String> = emptyMap(),
    val done: Boolean = false,
) : Serializable

class MedicationEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MedicationRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<MedicationEditRoute>()
    private var original: Medication? = null

    private val _state: MutableStateFlow<MedicationEditState>
    val state: StateFlow<MedicationEditState>

    init {
        val (form, restored) = savedStateHandle.savedForm(
            KEY_FORM,
            MedicationEditState(isNew = route.medicationId == 0L, loading = route.medicationId != 0L),
            usable = { !it.loading && !it.done },
        )
        _state = form
        state = form.asStateFlow()

        if (route.medicationId != 0L) {
            viewModelScope.launch {
                val med = repository.get(route.medicationId)
                original = med
                if (med != null && restored) return@launch // keep what the user had typed
                _state.value = if (med == null) {
                    MedicationEditState(isNew = false, loading = false, done = true)
                } else {
                    MedicationEditState(
                        isNew = false,
                        loading = false,
                        name = med.name,
                        dosage = med.dosage.orEmpty(),
                        frequency = med.frequency.orEmpty(),
                        startDate = med.startDate,
                        endDate = med.endDate,
                        notes = med.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v, errors = it.errors - MedicationField.NAME) }
    fun onDosageChange(v: String) = _state.update { it.copy(dosage = v) }
    fun onFrequencyChange(v: String) = _state.update { it.copy(frequency = v) }
    fun onStartDateChange(v: LocalDate?) = _state.update { it.copy(startDate = v, errors = it.errors - MedicationField.END_DATE) }
    fun onEndDateChange(v: LocalDate?) = _state.update { it.copy(endDate = v, errors = it.errors - MedicationField.END_DATE) }
    fun onNotesChange(v: String) = _state.update { it.copy(notes = v) }

    fun save() {
        val s = _state.value
        val errors = MedicationValidator.validate(s.name, s.startDate, s.endDate)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            val now = Instant.now()
            val base = original ?: Medication(personId = route.personId, name = s.name.trim(), createdAt = now, updatedAt = now)
            repository.save(
                base.copy(
                    name = s.name.trim(),
                    dosage = s.dosage.trim().ifEmpty { null },
                    frequency = s.frequency.trim().ifEmpty { null },
                    startDate = s.startDate,
                    endDate = s.endDate,
                    notes = s.notes.trim().ifEmpty { null },
                ),
            )
            _state.update { it.copy(done = true) }
        }
    }

    companion object {
        private const val KEY_FORM = "form"

        val Factory = appViewModelFactory { c, handle -> MedicationEditViewModel(handle, c.medicationRepository) }
    }
}
