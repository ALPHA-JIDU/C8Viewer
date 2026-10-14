package com.jihad.chatviewer.ui.picker

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jihad.chatviewer.data.importer.ArchiveScanner
import com.jihad.chatviewer.data.model.DiscoveredChat
import com.jihad.chatviewer.data.worker.ImportWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val _uiState = MutableStateFlow<ChatPickerUiState>(ChatPickerUiState.Scanning)
    val uiState: StateFlow<ChatPickerUiState> = _uiState.asStateFlow()

    private var activeWorkId: UUID? = null
    private var sourceUri: Uri? = null
    private var isZipSource = false

    fun startScan(uri: Uri, isZip: Boolean) {
        sourceUri = uri
        isZipSource = isZip
        _uiState.value = ChatPickerUiState.Scanning

        viewModelScope.launch {
            try {
                val list = if (isZip) {
                    ArchiveScanner.scanZipFiles(getApplication(), listOf(uri))
                } else {
                    ArchiveScanner.scanFolder(getApplication(), uri)
                }
                val selectedCount = list.count { it.isSelected }
                val totalMsgs = list.filter { it.isSelected }.sumOf { it.estimatedMessages }
                _uiState.value = ChatPickerUiState.Ready(list, selectedCount, totalMsgs)
            } catch (e: Exception) {
                _uiState.value = ChatPickerUiState.Error(e.message ?: "Failed to scan archive")
            }
        }
    }

    fun toggleChat(folder: String) {
        val current = _uiState.value as? ChatPickerUiState.Ready ?: return
        val updated = current.chats.map {
            if (it.folder == folder) it.copy(isSelected = !it.isSelected) else it
        }
        recalcReady(updated)
    }

    fun selectAll(select: Boolean) {
        val current = _uiState.value as? ChatPickerUiState.Ready ?: return
        val updated = current.chats.map { it.copy(isSelected = select) }
        recalcReady(updated)
    }

    private fun recalcReady(chats: List<DiscoveredChat>) {
        val selectedCount = chats.count { it.isSelected }
        val totalMsgs = chats.filter { it.isSelected }.sumOf { it.estimatedMessages }
        _uiState.update { ChatPickerUiState.Ready(chats, selectedCount, totalMsgs) }
    }

    fun startImport() {
        val current = _uiState.value as? ChatPickerUiState.Ready ?: return
        val uri = sourceUri ?: return
        val selectedFolders = current.chats.filter { it.isSelected }.map { it.folder }.toTypedArray()

        if (selectedFolders.isEmpty()) return

        val request = OneTimeWorkRequestBuilder<ImportWorker>()
            .setInputData(
                workDataOf(
                    ImportWorker.KEY_SOURCE_URI to uri.toString(),
                    ImportWorker.KEY_IS_ZIP to isZipSource,
                    ImportWorker.KEY_SELECTED_FOLDERS to selectedFolders
                )
            )
            .build()

        activeWorkId = request.id
        workManager.enqueue(request)

        _uiState.value = ChatPickerUiState.Importing(0, "Queued...")

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { info ->
                if (info != null) {
                    when (info.state) {
                        WorkInfo.State.RUNNING -> {
                            val percent = info.progress.getInt(ImportWorker.PROGRESS_PERCENT, 0)
                            val status = info.progress.getString(ImportWorker.PROGRESS_STATUS) ?: "Importing..."
                            _uiState.value = ChatPickerUiState.Importing(percent, status)
                        }
                        WorkInfo.State.CANCELLED -> {
                            _uiState.value = ChatPickerUiState.Error("Import was cancelled")
                        }
                        WorkInfo.State.FAILED -> {
                            val err = info.outputData.getString("error") ?: "Import failed"
                            _uiState.value = ChatPickerUiState.Error(err)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    fun cancelImport() {
        activeWorkId?.let { workManager.cancelWorkById(it) }
    }
}
