package com.healthtracker.ui.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.local.Note
import com.healthtracker.data.repository.NoteRepository
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.navigation.NoteDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NoteDetailState {
    data object Loading : NoteDetailState
    data object Gone : NoteDetailState
    data class Loaded(val note: Note) : NoteDetailState
}

class NoteDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
) : ViewModel() {

    private val noteId = savedStateHandle.toRoute<NoteDetailRoute>().noteId

    val state: StateFlow<NoteDetailState> = repository.observe(noteId)
        .map { if (it == null) NoteDetailState.Gone else NoteDetailState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteDetailState.Loading)

    private val _confirmingDelete = MutableStateFlow(false)
    val confirmingDelete: StateFlow<Boolean> = _confirmingDelete.asStateFlow()

    fun requestDelete() { _confirmingDelete.value = true }
    fun dismissDelete() { _confirmingDelete.value = false }

    fun confirmDelete() {
        val note = (state.value as? NoteDetailState.Loaded)?.note ?: return
        _confirmingDelete.value = false
        viewModelScope.launch { repository.delete(note) }
    }

    companion object {
        val Factory = appViewModelFactory { c, handle -> NoteDetailViewModel(handle, c.noteRepository) }
    }
}
