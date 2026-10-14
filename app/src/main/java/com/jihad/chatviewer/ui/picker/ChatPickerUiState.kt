package com.jihad.chatviewer.ui.picker

import com.jihad.chatviewer.data.model.DiscoveredChat

sealed interface ChatPickerUiState {
    data object Scanning : ChatPickerUiState
    data class Error(val message: String) : ChatPickerUiState
    data class Ready(
        val chats: List<DiscoveredChat>,
        val selectedCount: Int,
        val totalMessagesSelected: Int
    ) : ChatPickerUiState
    data class Importing(
        val progressPercent: Int,
        val currentStatus: String
    ) : ChatPickerUiState
}
