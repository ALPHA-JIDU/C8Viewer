package com.jihad.chatviewer.ui.chatinfo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jihad.chatviewer.data.local.AppDatabase
import com.jihad.chatviewer.data.local.dao.MediaCounts
import com.jihad.chatviewer.util.ChatStatsCalculator
import com.jihad.chatviewer.util.MediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatInfoViewModel(
    private val db: AppDatabase,
    private val threadId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatInfoUiState>(ChatInfoUiState.Loading)
    val uiState: StateFlow<ChatInfoUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val thread = db.threadDao().getThreadById(threadId) ?: return@launch
            val pins = db.messageDao().getPinnedMessages(threadId).first()
            val mediaCounts = db.messageDao().getMediaCounts(threadId)
                ?: MediaCounts(0, 0, 0, 0, 0)
            val calls = db.messageDao().getCallHistory(threadId)
            val allMessages = withContext(Dispatchers.IO) {
                db.messageDao().getAllMessagesForThread(threadId)
            }
            val stats = withContext(Dispatchers.Default) {
                ChatStatsCalculator.calculate(allMessages)
            }

            val members = thread.participants.split("\n").filter { it.isNotBlank() }

            _uiState.value = ChatInfoUiState.Ready(
                thread = thread,
                participants = members,
                pinnedMessages = pins,
                mediaCounts = mediaCounts,
                stats = stats,
                callHistory = calls
            )
        }
    }

    fun setNickname(nickname: String) {
        viewModelScope.launch {
            db.threadDao().updateNickname(threadId, nickname.ifBlank { null })
            loadData()
        }
    }

    fun updateTheme(themeColor: String?, wallpaper: String?) {
        viewModelScope.launch {
            db.threadDao().updateThemeAndWallpaper(threadId, themeColor, wallpaper)
            loadData()
        }
    }

    fun exportChat(context: Context) {
        viewModelScope.launch {
            val allMessages = db.messageDao().getAllMessagesForThread(threadId)
            val exportText = allMessages.joinToString("\n") { m ->
                "[${java.time.Instant.ofEpochMilli(m.ts)}] ${m.sender}: ${m.content ?: "[Media]"}"
            }
            MediaHelper.shareText(context, exportText)
        }
    }

    fun hideChat(onDone: () -> Unit) {
        viewModelScope.launch {
            db.threadDao().updateHidden(threadId, true)
            onDone()
        }
    }

    fun removeChat(onDone: () -> Unit) {
        viewModelScope.launch {
            db.messageDao().deleteMessagesForThread(threadId)
            db.threadDao().deleteThread(threadId)
            onDone()
        }
    }
}
