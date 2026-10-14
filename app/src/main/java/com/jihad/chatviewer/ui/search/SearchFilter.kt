package com.jihad.chatviewer.ui.search

enum class MediaTypeFilter { ALL, PHOTO, VIDEO, VOICE, LINK, FILE }
enum class TimeOfDayFilter { ANY, MORNING, AFTERNOON, EVENING, NIGHT }

data class SearchFilter(
    val threadId: Long? = null,
    val isExactPhrase: Boolean = false,
    val senderName: String? = null,
    val mediaType: MediaTypeFilter = MediaTypeFilter.ALL,
    val timeOfDay: TimeOfDayFilter = TimeOfDayFilter.ANY,
    val dateFromTs: Long? = null,
    val dateToTs: Long? = null
)
