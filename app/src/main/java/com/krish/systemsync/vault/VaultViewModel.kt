package com.krish.systemsync.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class VaultEvent {
    data class Success(val message: String) : VaultEvent()
    data class NeedsDeleteConsent(val result: RemoveOriginalResult.NeedsConsent) : VaultEvent()
    data class Error(val message: String) : VaultEvent()
}

class VaultViewModel(application: Application, isDummy: Boolean = false) : AndroidViewModel(application) {
    private val repository = VaultRepository(application, isDummy)

    private val _files = MutableStateFlow<List<VaultFile>>(emptyList())
    val files = _files.asStateFlow()

    private val _playerQueue = MutableStateFlow<List<VaultFile>>(emptyList())
    val playerQueue = _playerQueue.asStateFlow()

    fun setPlayerQueue(queue: List<VaultFile>) {
        _playerQueue.value = queue
    }

    private val _trash = MutableStateFlow<List<TrashedFile>>(emptyList())
    val trash = _trash.asStateFlow()

    private val _storageMode = MutableStateFlow(StorageMode.MAXIMUM_PRIVACY)
    val storageMode = _storageMode.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _events = MutableStateFlow<VaultEvent?>(null)
    val events = _events.asStateFlow()

    private val _resetTrigger = MutableStateFlow(0)
    val resetTrigger = _resetTrigger.asStateFlow()

    init {
        loadFiles()
        loadTrash()
    }

    fun requestReset() {
        _resetTrigger.value++
    }

    fun loadFiles() {
        _files.value = repository.getAllFiles()
    }

    fun loadTrash() {
        _trash.value = repository.getTrashFiles()
    }

    fun setStorageMode(mode: StorageMode) {
        _storageMode.value = mode
    }

    /** Imports the picked file into the vault under [mode], then tries to remove the public original. */
    fun importFile(uri: Uri, fileName: String, mode: StorageMode, keepOriginal: Boolean = false) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = repository.importFile(uri, fileName, mode)
                loadFiles()
                result.fold(
                    onSuccess = {
                        if (keepOriginal) {
                            _events.value = VaultEvent.Success("File hidden")
                            return@launch
                        }
                        when (val removal = repository.requestRemoveOriginal(uri)) {
                            is RemoveOriginalResult.Removed ->
                                _events.value = VaultEvent.Success("File hidden and removed from Gallery")
                            is RemoveOriginalResult.NeedsConsent ->
                                _events.value = VaultEvent.NeedsDeleteConsent(removal)
                            is RemoveOriginalResult.NotRemovable ->
                                _events.value = VaultEvent.Success("File hidden")
                        }
                    },
                    onFailure = { e ->
                        _events.value = VaultEvent.Error(e.message ?: "Import failed")
                    }
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }

    /** Called once the system delete-consent dialog (Android 11+) returns success. */
    fun onOriginalDeleteConfirmed() {
        _events.value = VaultEvent.Success("File hidden and removed from Gallery")
    }

    fun unhideFile(file: VaultFile) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.unhideFile(file.name, file.mode, file.type).fold(
                    onSuccess = {
                        repository.deleteFile(file.name, file.mode, permanent = true)
                        loadFiles()
                        _events.value = VaultEvent.Success("File unhidden and moved to SystemSync folder")
                    },
                    onFailure = { e ->
                        _events.value = VaultEvent.Error(e.message ?: "Unhide failed")
                    }
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportFile(file: VaultFile, destUri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                runCatching {
                    repository.exportFile(file.name, file.mode, destUri)
                    repository.deleteFile(file.name, file.mode, permanent = true)
                }.onSuccess {
                    loadFiles()
                    _events.value = VaultEvent.Success("File exported and unhidden")
                }.onFailure { e ->
                    _events.value = VaultEvent.Error(e.message ?: "Export failed")
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteFile(fileName: String, mode: StorageMode, permanent: Boolean = false) {
        viewModelScope.launch {
            repository.deleteFile(fileName, mode, permanent)
            loadFiles()
            loadTrash()
        }
    }

    fun restoreFromTrash(trashName: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(trashName)
            loadFiles()
            loadTrash()
        }
    }

    fun permanentlyDeleteFromTrash(trashName: String) {
        viewModelScope.launch {
            repository.permanentlyDeleteFromTrash(trashName)
            loadTrash()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            loadTrash()
        }
    }

    fun renameFile(oldName: String, newName: String, mode: StorageMode) {
        viewModelScope.launch {
            repository.renameFile(oldName, newName, mode)
            loadFiles()
        }
    }

    suspend fun preparePlaybackFile(file: VaultFile): File = repository.preparePlaybackFile(file)

    override fun onCleared() {
        super.onCleared()
        repository.clearPlaybackCache()
    }
}
