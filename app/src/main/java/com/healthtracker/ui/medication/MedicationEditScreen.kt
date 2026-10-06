package com.healthtracker.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthtracker.domain.MedicationField
import com.healthtracker.domain.frequencySuggestions
import com.healthtracker.ui.components.DateField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationEditScreen(
    onDone: () -> Unit,
    viewModel: MedicationEditViewModel = viewModel(factory = MedicationEditViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add Medication" else "Edit Medication") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !state.loading) { Text("Save") }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val nameFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { if (state.isNew) nameFocus.requestFocus() }

        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Medicine name") },
                placeholder = { Text("e.g. Amlodipine") },
                singleLine = true,
                isError = MedicationField.NAME in state.errors,
                supportingText = state.errors[MedicationField.NAME]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
            )
            OutlinedTextField(
                value = state.dosage,
                onValueChange = viewModel::onDosageChange,
                label = { Text("Dose (optional)") },
                placeholder = { Text("e.g. 5 mg") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.frequency,
                    onValueChange = viewModel::onFrequencyChange,
                    label = { Text("How often (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    frequencySuggestions.forEach { suggestion ->
                        SuggestionChip(onClick = { viewModel.onFrequencyChange(suggestion) }, label = { Text(suggestion) })
                    }
                }
            }
            OptionalDateRow("Start date (optional)", state.startDate, viewModel::onStartDateChange)
            OptionalDateRow(
                "Stop date (optional)",
                state.endDate,
                viewModel::onEndDateChange,
                error = state.errors[MedicationField.END_DATE],
                placeholder = "Still taking",
                allowFuture = true,
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Take after food") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Save") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionalDateRow(
    label: String,
    date: java.time.LocalDate?,
    onChange: (java.time.LocalDate?) -> Unit,
    error: String? = null,
    placeholder: String? = null,
    allowFuture: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DateField(
            label = label,
            date = date,
            onDateChange = onChange,
            placeholder = placeholder,
            error = error,
            allowFuture = allowFuture,
            modifier = Modifier.weight(1f),
        )
        if (date != null) TextButton(onClick = { onChange(null) }) { Text("Clear") }
    }
}
