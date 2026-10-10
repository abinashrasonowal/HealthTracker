package com.healthtracker.ui.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthtracker.data.ExportData
import com.healthtracker.domain.ExportPreset
import com.healthtracker.domain.RecordType
import com.healthtracker.ui.components.DateField
import com.healthtracker.ui.components.FieldPair
import com.healthtracker.ui.components.PersonAvatar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = viewModel(factory = ExportViewModel.Factory),
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val people by viewModel.people.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)) { uri ->
        uri?.let { viewModel.write(ExportFormat.PDF, it) }
    }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType)) { uri ->
        uri?.let { viewModel.write(ExportFormat.CSV, it) }
    }
    fun export(format: ExportFormat) {
        val launcher = if (format == ExportFormat.PDF) createPdf else createCsv
        launcher.launch(viewModel.fileName(format))
    }

    // Clear each event only after its message is done: clearing first would change the key
    // and cancel this effect before the snackbar appears.
    LaunchedEffect(saved) {
        val file = saved ?: return@LaunchedEffect
        val result = snackbar.showSnackbar("${file.format.label} saved", actionLabel = "Share", withDismissAction = true)
        viewModel.savedShown()
        if (result == SnackbarResult.ActionPerformed) {
            val send = Intent(Intent.ACTION_SEND)
                .setType(file.format.mimeType)
                .putExtra(Intent.EXTRA_STREAM, file.uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Gives the share sheet the file's name to show instead of an internal id.
                .apply { clipData = ClipData.newUri(context.contentResolver, file.name, file.uri) }
            try {
                context.startActivity(Intent.createChooser(send, "Share health records"))
            } catch (e: ActivityNotFoundException) {
                snackbar.showSnackbar("No app on this phone can share files.")
            }
        }
    }
    LaunchedEffect(error) {
        val message = error ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.errorShown()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Export Records") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val all = people
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (all != null && all.isEmpty()) {
                Text("Add a person first; there's nothing to export yet.", style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            Label("Person")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                all.orEmpty().forEach { p ->
                    FilterChip(
                        selected = preview?.person?.id == p.id,
                        onClick = { viewModel.setPerson(p.id) },
                        label = { Text(p.name) },
                        leadingIcon = { PersonAvatar(p.name, p.photoUri, size = 24.dp) },
                    )
                }
            }

            Label("Dates")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportPreset.entries.forEach { p ->
                    FilterChip(selected = options.preset == p, onClick = { viewModel.setPreset(p) }, label = { Text(p.label) })
                }
            }
            if (options.preset == ExportPreset.CUSTOM) {
                FieldPair(
                    { DateField("From", options.customFrom, viewModel::setCustomFrom, it) },
                    { DateField("To", options.customTo, viewModel::setCustomTo, it) },
                )
            }

            Label("Include")
            Column {
                RecordType.entries.forEach { t ->
                    CheckRow(t.label, t in options.types) { viewModel.toggleType(t) }
                }
                CheckRow("Notes", options.includeNotes, viewModel::toggleNotes)
                CheckRow("Medications", options.includeMedications, viewModel::toggleMedications)
            }

            Spacer(Modifier.height(4.dp))
            Text(previewText(preview), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val canExport = preview?.isEmpty == false && !busy
            Button(onClick = { export(ExportFormat.PDF) }, enabled = canExport, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save PDF")
            }
            OutlinedButton(onClick = { export(ExportFormat.CSV) }, enabled = canExport, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Filled.TableChart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save spreadsheet (CSV)")
            }
            Text(
                "You choose where the file is saved. It contains medical information, so share it only with people you trust.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun previewText(data: ExportData?): String {
    if (data == null) return "Checking…"
    if (data.isEmpty) return "Nothing matches these choices."
    fun n(count: Int, word: String) = if (count == 1) "1 $word" else "$count ${word}s"
    val parts = listOfNotNull(
        data.records.size.takeIf { it > 0 }?.let { n(it, "reading") },
        data.notes.size.takeIf { it > 0 }?.let { n(it, "note") },
        data.medications.size.takeIf { it > 0 }?.let { n(it, "medication") },
    )
    val list = if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    return "Will include $list."
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    // The whole row is one checkbox control, so screen readers announce the label with it.
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(horizontal = 12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
