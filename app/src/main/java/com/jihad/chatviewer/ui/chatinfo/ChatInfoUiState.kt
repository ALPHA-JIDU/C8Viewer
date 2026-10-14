package com.jihad.chatviewer.ui.chatinfo

import com.jihad.chatviewer.data.local.dao.MediaCounts
import com.jihad.chatviewer.data.local.entity.MessageEntity
import com.jihad.chatviewer.data.local.entity.ThreadEntity
import com.jihad.chatviewer.data.model.ChatStats

sealed interface ChatInfoUiState {
    data object Loading : ChatInfoUiState
    data class Ready(
        val thread: ThreadEntity,
        val participants: List<String>,
        val pinnedMessages: List<MessageEntity>,
        val mediaCounts: MediaCounts,
        val stats: ChatStats,
        val callHistory: List<MessageEntity>
    ) : ChatInfoUiState
}
