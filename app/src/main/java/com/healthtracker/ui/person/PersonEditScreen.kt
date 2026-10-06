package com.healthtracker.ui.person

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthtracker.domain.Gender
import com.healthtracker.ui.components.ConfirmDeleteDialog
import com.healthtracker.ui.components.DateField
import com.healthtracker.ui.components.PersonAvatar

private val relationshipSuggestions = listOf("Self", "Father", "Mother", "Spouse", "Son", "Daughter", "Brother", "Sister")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonEditScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: PersonEditViewModel = viewModel(factory = PersonEditViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.result) {
        when (val result = state.result) {
            is PersonEditResult.Created -> onCreated(result.personId)
            PersonEditResult.Saved -> onSaved()
            PersonEditResult.Deleted -> onDeleted()
            null -> Unit
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onPhotoPicked)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add Person" else "Edit Person") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = viewModel::requestDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete person")
                        }
                    }
                    // Always reachable, even when the keyboard hides the bottom button.
                    TextButton(onClick = viewModel::save, enabled = !state.loading) { Text("Save") }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(state.name, state.photoUri, size = 88.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    TextButton(onClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (state.photoUri == null) "Add Photo" else "Change Photo") }
                    if (state.photoUri != null) {
                        TextButton(onClick = viewModel::onRemovePhoto) { Text("Remove Photo") }
                    }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.relationship,
                    onValueChange = viewModel::onRelationshipChange,
                    label = { Text("Relationship (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    relationshipSuggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { viewModel.onRelationshipChange(suggestion) },
                            label = { Text(suggestion) },
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                DateField(
                    label = "Date of birth (optional)",
                    date = state.dateOfBirth,
                    onDateChange = viewModel::onDateOfBirthChange,
                    modifier = Modifier.weight(1f),
                )
                if (state.dateOfBirth != null) {
                    TextButton(onClick = viewModel::onClearDateOfBirth) { Text("Clear") }
                }
            }

            Column {
                Text("Gender (optional)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Gender.entries.forEach { gender ->
                        FilterChip(
                            selected = state.gender == gender,
                            onClick = { viewModel.onGenderChange(if (state.gender == gender) null else gender) },
                            label = { Text(gender.label) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Save Person") }

            Spacer(Modifier.height(16.dp))
        }
    }

    state.deleteCounts?.let { counts ->
        val name = state.name.ifBlank { "this person" }
        ConfirmDeleteDialog(
            title = "Delete $name?",
            message = "${deleteWarning(counts)} This can't be undone.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

private fun deleteWarning(counts: DeleteCounts): String {
    fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
    val parts = listOfNotNull(
        counts.records.takeIf { it > 0 }?.let { plural(it, "health record") },
        counts.notes.takeIf { it > 0 }?.let { plural(it, "note") },
        counts.medications.takeIf { it > 0 }?.let { plural(it, "medication") },
    )
    if (parts.isEmpty()) return "They have no records, notes or medications."
    val list = if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    return "Their $list will also be permanently deleted."
}
