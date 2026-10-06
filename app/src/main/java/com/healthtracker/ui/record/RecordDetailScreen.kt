package com.healthtracker.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthtracker.data.local.HealthRecord
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.formatNumber
import com.healthtracker.ui.components.ConfirmDeleteDialog
import com.healthtracker.ui.components.RecordTypeIcon
import com.healthtracker.ui.components.dateLabel
import com.healthtracker.ui.components.displayValue
import com.healthtracker.ui.components.timeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    onBack: () -> Unit,
    onEdit: (HealthRecord) -> Unit,
    viewModel: RecordDetailViewModel = viewModel(factory = RecordDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val confirmingDelete by viewModel.confirmingDelete.collectAsStateWithLifecycle()

    LaunchedEffect(state) { if (state is RecordDetailState.Gone) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((state as? RecordDetailState.Loaded)?.record?.type?.label.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val record = (state as? RecordDetailState.Loaded)?.record
        if (record == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RecordTypeIcon(record.type, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Text(record.displayValue, style = MaterialTheme.typography.displaySmall)
            }

            Column {
                detailRows(record).forEach { (label, value) ->
                    DetailRow(label, value)
                    HorizontalDivider()
                }
                DetailRow("Date", record.dateLabel)
                HorizontalDivider()
                DetailRow("Time", record.timeLabel)
            }

            record.note?.let {
                Column {
                    Text("Note", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onEdit(record) }, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = viewModel::requestDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (confirmingDelete) {
        ConfirmDeleteDialog(
            title = "Delete this record?",
            message = "This can't be undone.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

private fun detailRows(record: HealthRecord): List<Pair<String, String>> {
    val unit = record.unit.symbol
    return buildList {
        when (record.type) {
            RecordType.BLOOD_PRESSURE -> {
                add("Systolic" to "${formatNumber(record.value1)} $unit")
                record.value2?.let { add("Diastolic" to "${formatNumber(it)} $unit") }
            }
            RecordType.BLOOD_SUGAR -> record.context?.let { add("Measured" to it.label) }
            else -> Unit
        }
        record.pulse?.let { add("Pulse" to "$it bpm") }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
