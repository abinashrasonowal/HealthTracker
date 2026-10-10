package com.healthtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.healthtracker.ui.export.ExportScreen
import com.healthtracker.ui.medication.MedicationDetailScreen
import com.healthtracker.ui.medication.MedicationEditScreen
import com.healthtracker.ui.note.NoteDetailScreen
import com.healthtracker.ui.note.NoteEditScreen
import com.healthtracker.ui.people.PeopleScreen
import com.healthtracker.ui.person.PersonEditScreen
import com.healthtracker.ui.person.PersonProfileScreen
import com.healthtracker.ui.record.RecordDetailScreen
import com.healthtracker.ui.record.RecordEditScreen
import com.healthtracker.ui.search.SearchScreen
import com.healthtracker.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = PeopleRoute) {
        composable<PeopleRoute> {
            PeopleScreen(
                onPersonClick = { nav.navigate(PersonProfileRoute(it)) },
                onAddPerson = { nav.navigate(PersonEditRoute()) },
                onSearch = { nav.navigate(SearchRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { nav.popBackStack() }, onExport = { nav.navigate(ExportRoute()) })
        }
        composable<ExportRoute> {
            ExportScreen(onBack = { nav.popBackStack() })
        }
        composable<SearchRoute> {
            SearchScreen(
                onBack = { nav.popBackStack() },
                onRecordClick = { nav.navigate(RecordDetailRoute(it)) },
                onNoteClick = { nav.navigate(NoteDetailRoute(it)) },
            )
        }
        composable<PersonEditRoute> {
            PersonEditScreen(
                onBack = { nav.popBackStack() },
                onCreated = { id ->
                    nav.navigate(PersonProfileRoute(id)) {
                        popUpTo<PersonEditRoute> { inclusive = true }
                    }
                },
                onSaved = { nav.popBackStack() },
                onDeleted = { nav.popBackStack(PeopleRoute, inclusive = false) },
            )
        }
        composable<PersonProfileRoute> {
            PersonProfileScreen(
                onBack = { nav.popBackStack() },
                onEditPerson = { nav.navigate(PersonEditRoute(it)) },
                onExport = { nav.navigate(ExportRoute(it)) },
                onSwitchPerson = { id ->
                    nav.navigate(PersonProfileRoute(id)) {
                        popUpTo<PersonProfileRoute> { inclusive = true }
                    }
                },
                onAddRecord = { personId, type -> nav.navigate(RecordEditRoute(personId, type = type.name)) },
                onAddNote = { personId -> nav.navigate(NoteEditRoute(personId)) },
                onAddMedication = { personId -> nav.navigate(MedicationEditRoute(personId)) },
                onMedicationClick = { nav.navigate(MedicationDetailRoute(it)) },
                onRecordClick = { nav.navigate(RecordDetailRoute(it)) },
                onNoteClick = { nav.navigate(NoteDetailRoute(it)) },
            )
        }
        composable<RecordEditRoute> {
            RecordEditScreen(onDone = { nav.popBackStack() })
        }
        composable<MedicationEditRoute> {
            MedicationEditScreen(onDone = { nav.popBackStack() })
        }
        composable<MedicationDetailRoute> {
            MedicationDetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { med -> nav.navigate(MedicationEditRoute(med.personId, medicationId = med.id)) },
            )
        }
        composable<NoteEditRoute> {
            NoteEditScreen(onDone = { nav.popBackStack() })
        }
        composable<NoteDetailRoute> {
            NoteDetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { note -> nav.navigate(NoteEditRoute(note.personId, noteId = note.id)) },
            )
        }
        composable<RecordDetailRoute> {
            RecordDetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { record -> nav.navigate(RecordEditRoute(record.personId, recordId = record.id)) },
            )
        }
    }
}
