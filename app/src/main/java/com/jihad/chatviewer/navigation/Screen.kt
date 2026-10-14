package com.jihad.chatviewer.navigation

sealed class Screen(val route: String) {
    data object Chats : Screen("chats")
    data object Saved : Screen("saved")
    data object Search : Screen("search")
    data object Settings : Screen("settings")

    data object Storage : Screen("storage")
    data object HiddenChats : Screen("hidden_chats")
    data object AppLock : Screen("app_lock")

    data object ImportSource : Screen("import_source")
    data object ChatPicker : Screen("chat_picker")
    data class Chat(val threadId: Long, val focusId: Long? = null) : Screen("chat/{threadId}?focusId={focusId}")
    data class ChatInfo(val threadId: Long) : Screen("chat/{threadId}/info")
}
