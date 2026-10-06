package com.healthtracker.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.Medication
import com.healthtracker.data.local.Note
import com.healthtracker.data.local.Person
import com.healthtracker.data.repository.HealthRecordRepository
import com.healthtracker.data.repository.MedicationRepository
import com.healthtracker.data.repository.NoteRepository
import com.healthtracker.data.repository.PersonRepository
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.TimelineRow
import com.healthtracker.domain.Trend
import com.healthtracker.domain.TrendInput
import com.healthtracker.domain.TrendRange
import com.healthtracker.domain.Trends
import com.healthtracker.domain.buildTimeline
import com.healthtracker.domain.isMedicationActive
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.navigation.PersonProfileRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ProfileTab(val label: String) {
    OVERVIEW("Overview"),
    HISTORY("History"),
    TRENDS("Trends"),
    NOTES("Notes"),
    MEDICATIONS("Medications"),
}

/** What the History tab shows: everything, one record type, or only notes. */
enum class HistoryFilter(val label: String, val recordType: RecordType?) {
    ALL("All", null),
    BLOOD_PRESSURE(RecordType.BLOOD_PRESSURE.label, RecordType.BLOOD_PRESSURE),
    BLOOD_SUGAR(RecordType.BLOOD_SUGAR.label, RecordType.BLOOD_SUGAR),
    WEIGHT(RecordType.WEIGHT.label, RecordType.WEIGHT),
    PULSE(RecordType.PULSE.label, RecordType.PULSE),
    SPO2(RecordType.SPO2.label, RecordType.SPO2),
    TEMPERATURE(RecordType.TEMPERATURE.label, RecordType.TEMPERATURE),
    NOTES("Notes", null),
}

/** A row in the History tab: either a measurement or a note. */
sealed interface HistoryEntry {
    val dateTime: Instant
    val key: String

    data class RecordEntry(val record: HealthRecord) : HistoryEntry {
        override val dateTime get() = record.dateTime
        override val key get() = "r-${record.id}"
    }

    data class NoteEntry(val note: Note) : HistoryEntry {
        override val dateTime get() = note.dateTime
        override val key get() = "n-${note.id}"
    }
}

sealed interface ProfileState {
    data object Loading : ProfileState
    data object Missing : ProfileState
    data class Loaded(
        val person: Person,
        /** Latest record of each type, in [RecordType] order. */
        val latest: List<HealthRecord>,
        val otherPeople: List<Person>,
        val currentMedications: List<Medication>,
        val pastMedications: List<Medication>,
    ) : ProfileState
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersonProfileViewModel(
    private val savedStateHandle: SavedStateHandle,
    personRepository: PersonRepository,
    recordRepository: HealthRecordRepository,
    noteRepository: NoteRepository,
    medicationRepository: MedicationRepository,
) : ViewModel() {

    val personId = savedStateHandle.toRoute<PersonProfileRoute>().personId

    val state: StateFlow<ProfileState> = combine(
        personRepository.observe(personId),
        recordRepository.observeLatestPerType(personId),
        personRepository.observeAll(),
        medicationRepository.observeForPerson(personId),
    ) { person, latest, all, medications ->
        if (person == null) {
            ProfileState.Missing
        } else {
            val today = LocalDate.now()
            val (current, past) = medications.partition { isMedicationActive(it.endDate, today) }
            ProfileState.Loaded(
                person = person,
                latest = latest.sortedBy { it.type.ordinal },
                otherPeople = all.filter { it.id != personId },
                currentMedications = current,
                pastMedications = past.sortedByDescending { it.endDate },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState.Loading)

    val selectedTab: StateFlow<ProfileTab> = savedStateHandle.getStateFlow(KEY_TAB, ProfileTab.OVERVIEW)

    val historyFilter: StateFlow<HistoryFilter> = savedStateHandle.getStateFlow(KEY_FILTER, HistoryFilter.ALL)

    private val notes = noteRepository.observeForPerson(personId)

    /** null while loading. */
    val history: StateFlow<List<TimelineRow<HistoryEntry>>?> = historyFilter
        .flatMapLatest { filter ->
            val records = when (filter) {
                HistoryFilter.NOTES -> flowOf(emptyList())
                HistoryFilter.ALL -> recordRepository.observeHistory(personId)
                else -> recordRepository.observeHistory(personId, listOfNotNull(filter.recordType))
            }
            val notesForFilter = if (filter == HistoryFilter.ALL || filter == HistoryFilter.NOTES) notes else flowOf(emptyList())
            combine(records, notesForFilter) { r, n ->
                (r.map(HistoryEntry::RecordEntry) + n.map(HistoryEntry::NoteEntry)).sortedByDescending { it.dateTime }
            }
        }
        .map { entries ->
            val zone = ZoneId.systemDefault()
            buildTimeline(entries) { it.dateTime.atZone(zone).toLocalDate() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null while loading. */
    val noteList: StateFlow<List<Note>?> = notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null until the person has measurements; then defaults to the first type they have. */
    val trendType: StateFlow<RecordType?> = combine(
        savedStateHandle.getStateFlow<RecordType?>(KEY_TREND_TYPE, null),
        state,
    ) { chosen, s ->
        val available = (s as? ProfileState.Loaded)?.latest?.map { it.type }.orEmpty()
        chosen?.takeIf { it in available } ?: available.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val trendRange: StateFlow<TrendRange> = savedStateHandle.getStateFlow(KEY_TREND_RANGE, TrendRange.MONTH)

    val trend: StateFlow<Trend?> = trendType
        .flatMapLatest { type ->
            if (type == null) {
                flowOf(null)
            } else {
                combine(recordRepository.observeHistory(personId, listOf(type)), trendRange) { records, range ->
                    Trends.build(
                        type,
                        records.map { TrendInput(it.dateTime, it.value1, it.value2, it.unit) },
                        range,
                        Instant.now(),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTrendType(type: RecordType) {
        savedStateHandle[KEY_TREND_TYPE] = type
    }

    fun setTrendRange(range: TrendRange) {
        savedStateHandle[KEY_TREND_RANGE] = range
    }

    fun selectTab(tab: ProfileTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        savedStateHandle[KEY_FILTER] = filter
    }

    companion object {
        private const val KEY_TAB = "tab"
        private const val KEY_FILTER = "historyFilter"
        private const val KEY_TREND_TYPE = "trendType"
        private const val KEY_TREND_RANGE = "trendRange"

        val Factory = appViewModelFactory { c, handle ->
            PersonProfileViewModel(handle, c.personRepository, c.recordRepository, c.noteRepository, c.medicationRepository)
        }
    }
}
