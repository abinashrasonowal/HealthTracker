package com.healthtracker.ui.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.repository.HealthRecordRepository
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.navigation.RecordDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RecordDetailState {
    data object Loading : RecordDetailState
    data object Gone : RecordDetailState
    data class Loaded(val record: HealthRecord) : RecordDetailState
}

class RecordDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HealthRecordRepository,
) : ViewModel() {

    private val recordId = savedStateHandle.toRoute<RecordDetailRoute>().recordId

    val state: StateFlow<RecordDetailState> = repository.observe(recordId)
        .map { if (it == null) RecordDetailState.Gone else RecordDetailState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordDetailState.Loading)

    private val _confirmingDelete = MutableStateFlow(false)
    val confirmingDelete: StateFlow<Boolean> = _confirmingDelete.asStateFlow()

    fun requestDelete() { _confirmingDelete.value = true }
    fun dismissDelete() { _confirmingDelete.value = false }

    fun confirmDelete() {
        val record = (state.value as? RecordDetailState.Loaded)?.record ?: return
        _confirmingDelete.value = false
        viewModelScope.launch { repository.delete(record) }
    }

    companion object {
        val Factory = appViewModelFactory { c, handle -> RecordDetailViewModel(handle, c.recordRepository) }
    }
}
