package com.healthtracker.ui.medication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.local.Medication
import com.healthtracker.data.repository.MedicationRepository
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.navigation.MedicationDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface MedicationDetailState {
    data object Loading : MedicationDetailState
    data object Gone : MedicationDetailState
    data class Loaded(val medication: Medication) : MedicationDetailState
}

class MedicationDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MedicationRepository,
) : ViewModel() {

    private val medicationId = savedStateHandle.toRoute<MedicationDetailRoute>().medicationId

    val state: StateFlow<MedicationDetailState> = repository.observe(medicationId)
        .map { if (it == null) MedicationDetailState.Gone else MedicationDetailState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicationDetailState.Loading)

    private val _confirmingDelete = MutableStateFlow(false)
    val confirmingDelete: StateFlow<Boolean> = _confirmingDelete.asStateFlow()

    private val current get() = (state.value as? MedicationDetailState.Loaded)?.medication

    fun stopTaking() {
        val med = current ?: return
        viewModelScope.launch { repository.save(med.copy(endDate = LocalDate.now())) }
    }

    fun requestDelete() { _confirmingDelete.value = true }
    fun dismissDelete() { _confirmingDelete.value = false }

    fun confirmDelete() {
        val med = current ?: return
        _confirmingDelete.value = false
        viewModelScope.launch { repository.delete(med) }
    }

    companion object {
        val Factory = appViewModelFactory { c, handle -> MedicationDetailViewModel(handle, c.medicationRepository) }
    }
}
