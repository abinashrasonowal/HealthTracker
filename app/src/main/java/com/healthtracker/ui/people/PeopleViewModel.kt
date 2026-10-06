package com.healthtracker.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.local.PersonSummary
import com.healthtracker.data.repository.PersonRepository
import com.healthtracker.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PeopleViewModel(repository: PersonRepository) : ViewModel() {

    /** null while loading. */
    val people: StateFlow<List<PersonSummary>?> = repository.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = appViewModelFactory { c, _ -> PeopleViewModel(c.personRepository) }
    }
}
