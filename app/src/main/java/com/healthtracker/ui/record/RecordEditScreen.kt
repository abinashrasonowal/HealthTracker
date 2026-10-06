package com.healthtracker.ui.record

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthtracker.domain.Field
import com.healthtracker.domain.FieldSpec
import com.healthtracker.domain.SugarContext
import com.healthtracker.domain.spec
import com.healthtracker.ui.components.DateField
import com.healthtracker.ui.components.TimeField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordEditScreen(
    onDone: () -> Unit,
    viewModel: RecordEditViewModel = viewModel(factory = RecordEditViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.loading) "" else if (state.isNew) state.type.label else "Edit ${state.type.label}") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
        val spec = state.type.spec
        val focusManager = LocalFocusManager.current
        val firstField = remember { FocusRequester() }
        LaunchedEffect(Unit) { if (state.isNew) firstField.requestFocus() }

        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val unitSuffix = state.unit.symbol
            NumberField(
                spec = spec.value1,
                value = state.value1,
                onValueChange = viewModel::onValue1Change,
                suffix = unitSuffix,
                error = state.errors[Field.VALUE1],
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                modifier = Modifier.focusRequester(firstField),
            )
            spec.value2?.let {
                NumberField(
                    spec = it,
                    value = state.value2,
                    onValueChange = viewModel::onValue2Change,
                    suffix = unitSuffix,
                    error = state.errors[Field.VALUE2],
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                )
            }
            spec.pulse?.let {
                NumberField(
                    spec = it,
                    value = state.pulse,
                    onValueChange = viewModel::onPulseChange,
                    suffix = "bpm",
                    error = state.errors[Field.PULSE],
                    onNext = { focusManager.clearFocus() },
                )
            }

            if (spec.units.size > 1) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    spec.units.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = state.unit == unit,
                            onClick = { viewModel.onUnitChange(unit) },
                            shape = SegmentedButtonDefaults.itemShape(index, spec.units.size),
                        ) { Text(unit.symbol) }
                    }
                }
            }

            if (spec.hasSugarContext) {
                Column {
                    Text("When was it measured?", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SugarContext.entries.forEach { context ->
                            FilterChip(
                                selected = state.sugarContext == context,
                                onClick = { viewModel.onContextChange(context) },
                                label = { Text(context.label) },
                            )
                        }
                    }
                    state.errors[Field.CONTEXT]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    label = "Date",
                    date = state.date,
                    onDateChange = viewModel::onDateChange,
                    error = state.errors[Field.DATE_TIME],
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = "Time",
                    time = state.time,
                    onTimeChange = viewModel::onTimeChange,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. Before breakfast") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Save")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumberField(
    spec: FieldSpec,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    error: String?,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(if (spec.required) spec.label else "${spec.label} (optional)") },
        suffix = { Text(suffix) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (spec.allowDecimals) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = modifier.fillMaxWidth(),
    )
}
