package com.healthtracker.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.DataManager
import com.healthtracker.data.SettingsRepository
import com.healthtracker.data.backup.BackupContents
import com.healthtracker.data.backup.BackupFormatException
import com.healthtracker.domain.AppSettings
import com.healthtracker.domain.MeasureUnit
import com.healthtracker.domain.ThemeMode
import com.healthtracker.ui.appViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

data class DataUiState(
    val busy: Boolean = false,
    /** A checked backup waiting for the user to confirm the restore. */
    val pendingRestore: BackupContents? = null,
    val confirmingDeleteAll: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val dataManager: DataManager,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _data = MutableStateFlow(DataUiState())
    val data: StateFlow<DataUiState> = _data.asStateFlow()

    fun setTheme(v: ThemeMode) = viewModelScope.launch { settingsRepository.setTheme(v) }
    fun setWeightUnit(v: MeasureUnit) = viewModelScope.launch { settingsRepository.setWeightUnit(v) }
    fun setTemperatureUnit(v: MeasureUnit) = viewModelScope.launch { settingsRepository.setTemperatureUnit(v) }
    fun setGlucoseUnit(v: MeasureUnit) = viewModelScope.launch { settingsRepository.setGlucoseUnit(v) }

    val backupFileName: String get() = "medical-records-backup-${LocalDate.now()}.json"

    fun backUp(uri: Uri) = runBusy {
        val out = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Couldn't open the file")
        out.use { dataManager.writeBackup(it) }
        "Backup saved. Keep it somewhere safe; it contains medical information."
    }

    fun pickedBackup(uri: Uri) = runBusy {
        val input = contentResolver.openInputStream(uri) ?: throw IOException("Couldn't open the file")
        val backup = input.use { dataManager.readBackup(it) }
        _data.update { it.copy(pendingRestore = backup) }
        null
    }

    fun confirmRestore() {
        val backup = _data.value.pendingRestore ?: return
        _data.update { it.copy(pendingRestore = null) }
        runBusy {
            dataManager.restore(backup)
            "Backup restored."
        }
    }

    fun cancelRestore() = _data.update { it.copy(pendingRestore = null) }

    fun requestDeleteAll() = _data.update { it.copy(confirmingDeleteAll = true) }
    fun cancelDeleteAll() = _data.update { it.copy(confirmingDeleteAll = false) }

    fun confirmDeleteAll() {
        _data.update { it.copy(confirmingDeleteAll = false) }
        runBusy {
            dataManager.deleteEverything()
            "All data deleted."
        }
    }

    fun messageShown() = _data.update { it.copy(message = null) }

    /** Runs [block] with a busy indicator; its result (or a readable error) becomes the message. */
    private fun runBusy(block: suspend () -> String?) {
        viewModelScope.launch {
            _data.update { it.copy(busy = true) }
            val message = try {
                block()
            } catch (e: BackupFormatException) {
                e.message
            } catch (e: IOException) {
                "Something went wrong with the file: ${e.message ?: "unknown error"}"
            } catch (e: SecurityException) {
                "The app wasn't allowed to use that file."
            }
            _data.update { it.copy(busy = false, message = message ?: it.message) }
        }
    }

    companion object {
        val Factory = appViewModelFactory { c, _ -> SettingsViewModel(c.settingsRepository, c.dataManager, c.contentResolver) }
    }
}
