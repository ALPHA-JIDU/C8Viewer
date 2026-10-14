package com.jihad.chatviewer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["thread_id", "ts"]),
        Index(value = ["thread_id", "is_pinned"]),
        Index(value = ["is_bookmarked"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "thread_id")
    val threadId: Long,
    val sender: String,
    val ts: Long,
    val content: String? = null,
    val media: String? = null,
    val link: String? = null,
    val reactions: String? = null,
    val unsent: Boolean = false,

    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "is_bookmarked", defaultValue = "0")
    val isBookmarked: Boolean = false,

    @ColumnInfo(name = "bookmark_note")
    val bookmarkNote: String? = null,

    @ColumnInfo(name = "bookmark_label")
    val bookmarkLabel: String? = null
)
