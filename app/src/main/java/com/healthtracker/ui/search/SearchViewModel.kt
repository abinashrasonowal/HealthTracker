package com.healthtracker.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.local.NoteWithPerson
import com.healthtracker.data.local.RecordWithPerson
import com.healthtracker.data.repository.HealthRecordRepository
import com.healthtracker.data.repository.NoteRepository
import com.healthtracker.domain.RecordSearch
import com.healthtracker.ui.appViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

sealed interface SearchHit {
    val dateTime: Instant
    val key: String

    data class RecordHit(val row: RecordWithPerson) : SearchHit {
        override val dateTime get() = row.record.dateTime
        override val key get() = "r-${row.record.id}"
    }

    data class NoteHit(val row: NoteWithPerson) : SearchHit {
        override val dateTime get() = row.note.dateTime
        override val key get() = "n-${row.note.id}"
    }
}

/** [results] is null when the query is blank (show the hint instead of "no results"). */
data class SearchResults(val query: String, val results: List<SearchHit>?)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val savedStateHandle: SavedStateHandle,
    recordRepository: HealthRecordRepository,
    noteRepository: NoteRepository,
) : ViewModel() {

    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    // Records are filtered in memory: a personal record book holds thousands of rows at most,
    // and matching on formatted dates and type names is awkward to express in SQL.
    val results: StateFlow<SearchResults> = combine(
        query.debounce(DEBOUNCE_MS),
        recordRepository.observeAllWithPerson(),
        noteRepository.observeAllWithPerson(),
    ) { q, records, notes ->
        val words = RecordSearch.queryWords(q)
        if (words.isEmpty()) {
            SearchResults(q, null)
        } else {
            val zone = ZoneId.systemDefault()
            val recordHits = records.filter { row ->
                val r = row.record
                RecordSearch.matches(
                    queryWords = words,
                    personName = row.personName,
                    type = r.type,
                    values = listOfNotNull(r.value1, r.value2, r.pulse?.toDouble()),
                    note = r.note,
                    date = r.dateTime.atZone(zone).toLocalDate(),
                )
            }.map(SearchHit::RecordHit)
            val noteHits = notes.filter { row ->
                val n = row.note
                RecordSearch.matchesNote(words, row.personName, n.title, n.content, n.dateTime.atZone(zone).toLocalDate())
            }.map(SearchHit::NoteHit)
            SearchResults(q, (recordHits + noteHits).sortedByDescending { it.dateTime })
        }
    }.flowOn(Dispatchers.Default) // matching thousands of records must not block the UI thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults("", null))

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    companion object {
        private const val KEY_QUERY = "query"
        private const val DEBOUNCE_MS = 200L

        val Factory = appViewModelFactory { c, handle -> SearchViewModel(handle, c.recordRepository, c.noteRepository) }
    }
}
