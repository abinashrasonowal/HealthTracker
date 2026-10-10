package com.healthtracker.ui.export

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.healthtracker.data.DataManager
import com.healthtracker.data.ExportData
import com.healthtracker.data.ExportSelection
import com.healthtracker.data.export.CsvExport
import com.healthtracker.data.export.PdfReport
import com.healthtracker.data.local.Person
import com.healthtracker.data.repository.PersonRepository
import com.healthtracker.domain.ExportPreset
import com.healthtracker.domain.RecordType
import com.healthtracker.domain.presetSpan
import com.healthtracker.ui.appViewModelFactory
import com.healthtracker.ui.navigation.ExportRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

enum class ExportFormat(val mimeType: String, val extension: String, val label: String) {
    PDF("application/pdf", "pdf", "PDF"),
    CSV("text/csv", "csv", "Spreadsheet"),
}

data class ExportOptions(
    val personId: Long,
    val preset: ExportPreset = ExportPreset.LAST_3_MONTHS,
    val customFrom: LocalDate = LocalDate.now().minusMonths(1),
    val customTo: LocalDate = LocalDate.now(),
    val types: Set<RecordType> = RecordType.entries.toSet(),
    val includeNotes: Boolean = true,
    val includeMedications: Boolean = true,
)

data class SavedFile(val uri: Uri, val format: ExportFormat, val name: String)

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModel(
    savedStateHandle: SavedStateHandle,
    personRepository: PersonRepository,
    private val dataManager: DataManager,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    private val _options = MutableStateFlow(ExportOptions(personId = savedStateHandle.toRoute<ExportRoute>().personId))
    val options: StateFlow<ExportOptions> = _options.asStateFlow()

    val people: StateFlow<List<Person>?> = personRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** What would be exported right now; null while loading or with no person chosen. */
    val preview: StateFlow<ExportData?> = combine(_options, people) { o, all ->
        // Fall back to the first person when none (or a deleted one) is selected.
        val id = all?.firstOrNull { it.id == o.personId }?.id ?: all?.firstOrNull()?.id
        id?.let { o.copy(personId = it) }
    }.mapLatest { o -> o?.let { dataManager.loadExport(it.toSelection()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _saved = MutableStateFlow<SavedFile?>(null)
    /** The file just written, so the screen can offer to share it. */
    val saved: StateFlow<SavedFile?> = _saved.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setPerson(id: Long) = _options.update { it.copy(personId = id) }
    fun setPreset(p: ExportPreset) = _options.update { it.copy(preset = p) }
    fun setCustomFrom(d: LocalDate) = _options.update { it.copy(customFrom = d) }
    fun setCustomTo(d: LocalDate) = _options.update { it.copy(customTo = d) }
    fun toggleType(t: RecordType) = _options.update { it.copy(types = if (t in it.types) it.types - t else it.types + t) }
    fun toggleNotes() = _options.update { it.copy(includeNotes = !it.includeNotes) }
    fun toggleMedications() = _options.update { it.copy(includeMedications = !it.includeMedications) }

    fun fileName(format: ExportFormat): String {
        val name = preview.value?.person?.name?.replace(Regex("[\\\\/:*?\"<>|]"), "")?.trim().orEmpty().ifEmpty { "Health" }
        return "$name - health records - ${LocalDate.now()}.${format.extension}"
    }

    fun write(format: ExportFormat, uri: Uri) {
        val data = preview.value ?: return
        val name = fileName(format)
        viewModelScope.launch {
            _busy.value = true
            try {
                withContext(Dispatchers.IO) {
                    val out = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Couldn't open the file")
                    out.use {
                        when (format) {
                            ExportFormat.PDF -> PdfReport().write(data, it)
                            ExportFormat.CSV -> it.write(
                                CsvExport.build(data.person.name, data.records, data.notes, data.medications).toByteArray(Charsets.UTF_8),
                            )
                        }
                    }
                }
                _saved.value = SavedFile(uri, format, name)
            } catch (e: IOException) {
                _error.value = "Couldn't save the file: ${e.message ?: "unknown error"}"
            } catch (e: SecurityException) {
                _error.value = "The app wasn't allowed to save there."
            } finally {
                _busy.value = false
            }
        }
    }

    fun savedShown() { _saved.value = null }
    fun errorShown() { _error.value = null }

    private fun ExportOptions.toSelection() = ExportSelection(
        personId = personId,
        span = presetSpan(preset, LocalDate.now(), customFrom, customTo),
        types = types,
        includeNotes = includeNotes,
        includeMedications = includeMedications,
    )

    companion object {
        val Factory = appViewModelFactory { c, handle ->
            ExportViewModel(handle, c.personRepository, c.dataManager, c.contentResolver)
        }
    }
}
