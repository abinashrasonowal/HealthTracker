package com.healthtracker.ui.person

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.PhotoStorage
import com.healthtracker.data.local.Person
import com.healthtracker.data.repository.PersonRepository
import com.healthtracker.domain.Gender
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.navigation.PersonEditRoute
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

data class PersonEditState(
    val isNew: Boolean,
    val loading: Boolean,
    val name: String = "",
    val relationship: String = "",
    val dateOfBirth: LocalDate? = null,
    val gender: Gender? = null,
    val notes: String = "",
    val photoUri: String? = null,
    val nameError: String? = null,
    /** Non-null while the delete confirmation is showing. */
    val deleteCounts: DeleteCounts? = null,
    val result: PersonEditResult? = null,
)

data class DeleteCounts(val records: Int, val notes: Int, val medications: Int)

sealed interface PersonEditResult {
    data class Created(val personId: Long) : PersonEditResult
    data object Saved : PersonEditResult
    data object Deleted : PersonEditResult
}

class PersonEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PersonRepository,
    private val photos: PhotoStorage,
) : ViewModel() {

    private val personId = savedStateHandle.toRoute<PersonEditRoute>().personId
    private var original: Person? = null

    /** Photos copied during this edit session that haven't been committed by a save yet. */
    private val uncommittedPhotos = mutableSetOf<String>()

    private val _state = MutableStateFlow(PersonEditState(isNew = personId == 0L, loading = personId != 0L))
    val state: StateFlow<PersonEditState> = _state.asStateFlow()

    init {
        if (personId != 0L) {
            viewModelScope.launch {
                val person = repository.get(personId)
                original = person
                _state.update {
                    if (person == null) {
                        it.copy(loading = false, result = PersonEditResult.Deleted)
                    } else {
                        it.copy(
                            loading = false,
                            name = person.name,
                            relationship = person.relationship.orEmpty(),
                            dateOfBirth = person.dateOfBirth,
                            gender = person.gender,
                            notes = person.notes.orEmpty(),
                            photoUri = person.photoUri,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun onRelationshipChange(value: String) = _state.update { it.copy(relationship = value) }
    fun onDateOfBirthChange(value: LocalDate) = _state.update { it.copy(dateOfBirth = value) }
    fun onClearDateOfBirth() = _state.update { it.copy(dateOfBirth = null) }
    fun onGenderChange(value: Gender?) = _state.update { it.copy(gender = value) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val stored = photos.import(uri) ?: return@launch
            uncommittedPhotos += stored
            _state.update { it.copy(photoUri = stored) }
        }
    }

    fun onRemovePhoto() = _state.update { it.copy(photoUri = null) }

    fun save() {
        val s = _state.value
        val name = s.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(nameError = "Enter a name") }
            return
        }
        viewModelScope.launch {
            val now = Instant.now()
            val person = (original ?: Person(name = name, createdAt = now, updatedAt = now)).copy(
                name = name,
                relationship = s.relationship.trim().ifEmpty { null },
                dateOfBirth = s.dateOfBirth,
                gender = s.gender,
                notes = s.notes.trim().ifEmpty { null },
                photoUri = s.photoUri,
            )
            val id = repository.save(person)
            uncommittedPhotos.remove(s.photoUri)
            if (original?.photoUri != s.photoUri) photos.delete(original?.photoUri)
            _state.update {
                it.copy(result = if (original == null) PersonEditResult.Created(id) else PersonEditResult.Saved)
            }
        }
    }

    fun requestDelete() {
        viewModelScope.launch {
            val counts = DeleteCounts(repository.recordCount(personId), repository.noteCount(personId), repository.medicationCount(personId))
            _state.update { it.copy(deleteCounts = counts) }
        }
    }

    fun dismissDelete() = _state.update { it.copy(deleteCounts = null) }

    fun confirmDelete() {
        val person = original ?: return
        viewModelScope.launch {
            repository.delete(person)
            _state.update { it.copy(deleteCounts = null, result = PersonEditResult.Deleted) }
        }
    }

    override fun onCleared() {
        // Discard photos picked but never saved. Runs after viewModelScope is cancelled, so block briefly.
        val leftovers = uncommittedPhotos.toList()
        if (leftovers.isNotEmpty()) {
            runBlocking { withContext(NonCancellable) { leftovers.forEach { photos.delete(it) } } }
        }
    }

    companion object {
        val Factory = appViewModelFactory { c, handle -> PersonEditViewModel(handle, c.personRepository, c.photoStorage) }
    }
}
