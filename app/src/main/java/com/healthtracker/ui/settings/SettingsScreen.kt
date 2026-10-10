package com.healthtracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthtracker.BuildConfig
import com.healthtracker.data.backup.BackupContents
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.ThemeMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(data.message) {
        data.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::backUp)
    }
    // Some file providers report .json files as plain or binary, so accept any file and check its contents.
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::pickedBackup)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
                if (data.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s = settings ?: return@Scaffold
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)) {
            item { SectionTitle("Appearance") }
            item {
                ChoiceRow("Theme", ThemeMode.entries, s.theme, { it.label }, viewModel::setTheme)
            }

            item { SectionTitle("Units for new records and charts") }
            item { ChoiceRow("Weight", listOf(MeasureUnit.KG, MeasureUnit.LB), s.weightUnit, { it.symbol }, viewModel::setWeightUnit) }
            item {
                ChoiceRow("Temperature", listOf(MeasureUnit.CELSIUS, MeasureUnit.FAHRENHEIT), s.temperatureUnit, { it.symbol }, viewModel::setTemperatureUnit)
            }
            item { ChoiceRow("Blood sugar", listOf(MeasureUnit.MG_DL, MeasureUnit.MMOL_L), s.glucoseUnit, { it.symbol }, viewModel::setGlucoseUnit) }

            item { SectionTitle("Data") }
            item {
                ActionRow(Icons.Filled.IosShare, "Export records", "A PDF for the doctor, or a spreadsheet (CSV)", enabled = !data.busy, onClick = onExport)
            }
            item {
                ActionRow(Icons.Filled.Backup, "Back up all data", "Save one file with everyone's records, notes, medications and photos", enabled = !data.busy) {
                    createBackup.launch(viewModel.backupFileName)
                }
            }
            item {
                ActionRow(Icons.Filled.Restore, "Restore from backup", "Replace what's in the app with a backup file", enabled = !data.busy) {
                    openBackup.launch(arrayOf("*/*"))
                }
            }
            item {
                ActionRow(
                    Icons.Filled.DeleteForever,
                    "Delete all data",
                    "Permanently remove everyone and everything",
                    enabled = !data.busy,
                    destructive = true,
                    onClick = viewModel::requestDeleteAll,
                )
            }

            item { SectionTitle("About") }
            item {
                Text(
                    "Medical Records ${BuildConfig.VERSION_NAME}\n\n" +
                        "Everything is stored only on this phone. The app has no internet access and no account. " +
                        "Information leaves the phone only when you export or back it up, and goes only where you choose.\n\n" +
                        "This app keeps records. It doesn't give medical advice or diagnose anything.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    data.pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("Restore this backup?") },
            text = { Text(restoreSummary(backup)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) { Text("Replace and restore", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text("Cancel") } },
        )
    }
    if (data.confirmingDeleteAll) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteAll,
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "This permanently deletes every person, record, note, medication and photo in the app. " +
                        "It can't be undone. If you might want them later, make a backup first.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteAll) { Text("Delete everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDeleteAll) { Text("Cancel") } },
        )
    }
}

private fun restoreSummary(b: BackupContents): String {
    fun n(count: Int, word: String) = if (count == 1) "1 $word" else "$count ${word}s"
    val made = b.createdAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    val people = if (b.people.size == 1) "1 person" else "${b.people.size} people"
    return "This backup was made on $made and has $people, " +
        "${n(b.records.size, "record")}, ${n(b.notes.size, "note")} and ${n(b.medications.size, "medication")}.\n\n" +
        "Restoring replaces everything currently in the app, including anything added since the backup was made. " +
        "This can't be undone."
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(label: String, options: List<T>, selected: T, optionLabel: (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(optionLabel(option)) }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null, tint = color) },
        headlineContent = { Text(title, color = color, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle) },
    )
    HorizontalDivider(Modifier.padding(start = 56.dp))
}
