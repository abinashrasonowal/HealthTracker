package com.healthtracker.ui.person

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.data.local.Note
import com.healthtracker.data.local.Person
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.TimelineRow
import com.healthtracker.domain.ageInYears
import com.healthtracker.domain.monthLabel
import com.healthtracker.domain.relativeDayLabel
import com.healthtracker.domain.timelineDayLabel
import com.healthtracker.ui.components.MedicationRow
import com.healthtracker.ui.components.NoteIcon
import com.healthtracker.ui.components.NoteRow
import com.healthtracker.ui.components.PersonAvatar
import com.healthtracker.ui.components.RecordRow
import com.healthtracker.ui.components.RecordTypeIcon
import com.healthtracker.ui.components.displayValue
import com.healthtracker.ui.components.localDateTime
import com.healthtracker.ui.trends.TrendsContent
import java.time.LocalDate

/** Leaves room for the floating "Add Record" button below the last item. */
private val FabClearance = 112.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonProfileScreen(
    onBack: () -> Unit,
    onEditPerson: (Long) -> Unit,
    onExport: (Long) -> Unit,
    onSwitchPerson: (Long) -> Unit,
    onAddRecord: (personId: Long, type: RecordType) -> Unit,
    onAddNote: (personId: Long) -> Unit,
    onRecordClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    onAddMedication: (personId: Long) -> Unit,
    onMedicationClick: (Long) -> Unit,
    viewModel: PersonProfileViewModel = viewModel(factory = PersonProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tab by viewModel.selectedTab.collectAsStateWithLifecycle()
    var showTypePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is ProfileState.Missing) onBack()
    }

    val loaded = state as? ProfileState.Loaded
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { loaded?.let { PersonSwitcher(it.person, it.otherPeople, onSwitchPerson) } },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (loaded != null) {
                            IconButton(onClick = { onExport(loaded.person.id) }) {
                                Icon(Icons.Filled.IosShare, contentDescription = "Export records")
                            }
                            IconButton(onClick = { onEditPerson(loaded.person.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit person")
                            }
                        }
                    },
                )
                PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 8.dp) {
                    ProfileTab.entries.forEach { t ->
                        Tab(
                            selected = tab == t,
                            onClick = { viewModel.selectTab(t) },
                            text = { Text(t.label, style = MaterialTheme.typography.titleSmall) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (loaded != null) {
                // The main action follows the tab, so each list's "add" is one tap away.
                val (label, onClick) = when (tab) {
                    ProfileTab.NOTES -> "Add Note" to { onAddNote(loaded.person.id) }
                    ProfileTab.MEDICATIONS -> "Add Medication" to { onAddMedication(loaded.person.id) }
                    else -> "Add Record" to { showTypePicker = true }
                }
                ExtendedFloatingActionButton(
                    onClick = onClick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(label) },
                    // The label inside isn't exposed to screen readers on its own; name the button explicitly.
                    modifier = Modifier.semantics { contentDescription = label },
                )
            }
        },
    ) { padding ->
        if (loaded == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = FabClearance)
        when (tab) {
            ProfileTab.OVERVIEW -> OverviewContent(
                loaded,
                onRecordClick,
                onMedicationClick,
                onSeeAllMedications = { viewModel.selectTab(ProfileTab.MEDICATIONS) },
                contentPadding,
            )
            ProfileTab.HISTORY -> {
                val filter by viewModel.historyFilter.collectAsStateWithLifecycle()
                val history by viewModel.history.collectAsStateWithLifecycle()
                HistoryContent(history, filter, viewModel::setHistoryFilter, onRecordClick, onNoteClick, contentPadding)
            }
            ProfileTab.TRENDS -> {
                val type by viewModel.trendType.collectAsStateWithLifecycle()
                val range by viewModel.trendRange.collectAsStateWithLifecycle()
                val trend by viewModel.trend.collectAsStateWithLifecycle()
                TrendsContent(
                    availableTypes = loaded.latest.map { it.type },
                    type = type,
                    range = range,
                    trend = trend,
                    onTypeChange = viewModel::setTrendType,
                    onRangeChange = viewModel::setTrendRange,
                    contentPadding = contentPadding,
                )
            }
            ProfileTab.NOTES -> {
                val notes by viewModel.noteList.collectAsStateWithLifecycle()
                NotesContent(notes, onNoteClick, contentPadding)
            }
            ProfileTab.MEDICATIONS -> MedicationsContent(loaded, onMedicationClick, contentPadding)
        }
    }

    if (showTypePicker && loaded != null) {
        RecordTypeSheet(
            onDismiss = { showTypePicker = false },
            onSelect = { type ->
                showTypePicker = false
                onAddRecord(loaded.person.id, type)
            },
            onSelectNote = {
                showTypePicker = false
                onAddNote(loaded.person.id)
            },
        )
    }
}

@Composable
private fun PersonSwitcher(person: Person, others: List<Person>, onSwitch: (Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (others.isNotEmpty()) Modifier.clickable { expanded = true } else Modifier,
        ) {
            Text(person.name)
            if (others.isNotEmpty()) Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch person")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            others.forEach { other ->
                DropdownMenuItem(
                    text = { Text(other.name, style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { PersonAvatar(other.name, other.photoUri, size = 32.dp) },
                    onClick = {
                        expanded = false
                        onSwitch(other.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun OverviewContent(
    state: ProfileState.Loaded,
    onRecordClick: (Long) -> Unit,
    onMedicationClick: (Long) -> Unit,
    onSeeAllMedications: () -> Unit,
    contentPadding: PaddingValues,
) {
    val person = state.person
    val today = LocalDate.now()
    LazyColumn(contentPadding = contentPadding) {
        item {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(person.name, person.photoUri, size = 72.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(person.name, style = MaterialTheme.typography.headlineSmall)
                    val details = listOfNotNull(
                        person.dateOfBirth?.let { "${ageInYears(it, today)} years old" },
                        person.relationship,
                    ).joinToString(" · ")
                    if (details.isNotEmpty()) {
                        Text(details, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { SectionTitle("Latest Measurements") }
        if (state.latest.isEmpty()) {
            item { EmptyText("No measurements yet. Tap Add Record to save the first one.") }
        } else {
            items(state.latest, key = { it.id }) { record ->
                LatestCard(record, today, onClick = { onRecordClick(record.id) })
            }
        }

        if (state.currentMedications.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SectionTitle("Current Medications") }
                    TextButton(onClick = onSeeAllMedications, modifier = Modifier.padding(end = 8.dp, top = 8.dp)) {
                        Text("See all")
                    }
                }
            }
            items(state.currentMedications, key = { "med-${it.id}" }) { med ->
                MedicationRow(med, onClick = { onMedicationClick(med.id) })
            }
        }
    }
}

@Composable
private fun MedicationsContent(state: ProfileState.Loaded, onMedicationClick: (Long) -> Unit, contentPadding: PaddingValues) {
    LazyColumn(contentPadding = contentPadding) {
        if (state.currentMedications.isEmpty() && state.pastMedications.isEmpty()) {
            item { EmptyText("No medications yet. Tap Add Medication to keep track of what's being taken.") }
            return@LazyColumn
        }
        item { SectionTitle("Current") }
        if (state.currentMedications.isEmpty()) {
            item { EmptyText("Not taking anything right now.") }
        }
        items(state.currentMedications, key = { it.id }) { med ->
            MedicationRow(med, onClick = { onMedicationClick(med.id) })
            HorizontalDivider(Modifier.padding(start = 76.dp))
        }
        if (state.pastMedications.isNotEmpty()) {
            item { SectionTitle("Past") }
            items(state.pastMedications, key = { it.id }) { med ->
                MedicationRow(med, onClick = { onMedicationClick(med.id) })
                HorizontalDivider(Modifier.padding(start = 76.dp))
            }
        }
    }
}

@Composable
private fun HistoryContent(
    rows: List<TimelineRow<HistoryEntry>>?,
    filter: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit,
    onRecordClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val today = LocalDate.now()
    LazyColumn(contentPadding = contentPadding) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(HistoryFilter.entries) { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { onFilterChange(if (filter == f) HistoryFilter.ALL else f) },
                        label = { Text(f.label) },
                    )
                }
            }
        }
        when {
            rows == null -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }
            }
            rows.isEmpty() -> item {
                EmptyText(
                    when (filter) {
                        HistoryFilter.ALL -> "No records yet."
                        HistoryFilter.NOTES -> "No notes yet."
                        else -> "No ${filter.label.lowercase()} records yet."
                    },
                )
            }
            else -> rows.forEach { row ->
                when (row) {
                    is TimelineRow.Month -> stickyHeader(key = "m-${row.month}") {
                        Surface(Modifier.fillMaxWidth()) {
                            Text(
                                monthLabel(row.month),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                    is TimelineRow.Day -> item(key = "d-${row.date}") {
                        Text(
                            timelineDayLabel(row.date, today),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    is TimelineRow.Item -> item(key = row.value.key) {
                        when (val entry = row.value) {
                            is HistoryEntry.RecordEntry -> RecordRow(entry.record, onClick = { onRecordClick(entry.record.id) })
                            is HistoryEntry.NoteEntry -> NoteRow(entry.note, onClick = { onNoteClick(entry.note.id) })
                        }
                        HorizontalDivider(Modifier.padding(start = 76.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesContent(notes: List<Note>?, onNoteClick: (Long) -> Unit, contentPadding: PaddingValues) {
    val today = LocalDate.now()
    LazyColumn(contentPadding = contentPadding) {
        when {
            notes == null -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }
            }
            notes.isEmpty() -> item {
                EmptyText("No notes yet. Use notes for doctor visits, symptoms, or medication changes.")
            }
            else -> items(notes, key = { it.id }) { note ->
                NoteRow(
                    note,
                    onClick = { onNoteClick(note.id) },
                    trailing = relativeDayLabel(note.localDateTime().toLocalDate(), today),
                )
                HorizontalDivider(Modifier.padding(start = 76.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun LatestCard(record: HealthRecord, today: LocalDate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RecordTypeIcon(record.type)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(record.type.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(record.displayValue, style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                relativeDayLabel(record.localDateTime().toLocalDate(), today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordTypeSheet(onDismiss: () -> Unit, onSelect: (RecordType) -> Unit, onSelectNote: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                "Add Record",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            RecordType.entries.forEach { type ->
                ListItem(
                    modifier = Modifier.clickable { onSelect(type) }.height(72.dp),
                    leadingContent = { RecordTypeIcon(type) },
                    headlineContent = { Text(type.label, style = MaterialTheme.typography.titleMedium) },
                )
            }
            ListItem(
                modifier = Modifier.clickable(onClick = onSelectNote).height(72.dp),
                leadingContent = { NoteIcon() },
                headlineContent = { Text("Note", style = MaterialTheme.typography.titleMedium) },
            )
        }
    }
}
