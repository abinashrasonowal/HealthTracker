package com.healthtracker.ui.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.local.Note
import com.healthtracker.data.repository.NoteRepository
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.savedForm
import com.healthtracker.ui.navigation.NoteEditRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class NoteEditState(
    val isNew: Boolean,
    val loading: Boolean,
    val title: String = "",
    val content: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES),
    val titleError: String? = null,
    val done: Boolean = false,
) : Serializable

class NoteEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<NoteEditRoute>()
    private var original: Note? = null

    private val _state: MutableStateFlow<NoteEditState>
    val state: StateFlow<NoteEditState>

    init {
        val (form, restored) = savedStateHandle.savedForm(
            KEY_FORM,
            NoteEditState(isNew = route.noteId == 0L, loading = route.noteId != 0L),
            usable = { !it.loading && !it.done },
        )
        _state = form
        state = form.asStateFlow()

        if (route.noteId != 0L) {
            viewModelScope.launch {
                val note = repository.get(route.noteId)
                original = note
                if (note != null && restored) return@launch // keep what the user had typed
                _state.value = if (note == null) {
                    NoteEditState(isNew = false, loading = false, done = true)
                } else {
                    val local = note.dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
                    NoteEditState(
                        isNew = false,
                        loading = false,
                        title = note.title,
                        content = note.content.orEmpty(),
                        date = local.toLocalDate(),
                        time = local.toLocalTime(),
                    )
                }
            }
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v, titleError = null) }
    fun onContentChange(v: String) = _state.update { it.copy(content = v) }
    fun onDateChange(v: LocalDate) = _state.update { it.copy(date = v) }
    fun onTimeChange(v: LocalTime) = _state.update { it.copy(time = v) }

    fun save() {
        val s = _state.value
        val title = s.title.trim()
        if (title.isEmpty()) {
            _state.update { it.copy(titleError = "Enter a title") }
            return
        }
        viewModelScope.launch {
            val now = Instant.now()
            val dateTime = LocalDateTime.of(s.date, s.time).atZone(ZoneId.systemDefault()).toInstant()
            val base = original ?: Note(personId = route.personId, title = title, dateTime = dateTime, createdAt = now, updatedAt = now)
            repository.save(base.copy(title = title, content = s.content.trim().ifEmpty { null }, dateTime = dateTime))
            _state.update { it.copy(done = true) }
        }
    }

    companion object {
        private const val KEY_FORM = "form"

        val Factory = appViewModelFactory { c, handle -> NoteEditViewModel(handle, c.noteRepository) }
    }
}
