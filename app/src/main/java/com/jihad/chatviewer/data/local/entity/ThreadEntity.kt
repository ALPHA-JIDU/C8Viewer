package com.jihad.chatviewer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "threads",
    indices = [
        Index(value = ["folder"], unique = true),
        Index(value = ["last_ts"]),
        Index(value = ["category"])
    ]
)
data class ThreadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val folder: String,
    val people: Int = 0,
    @ColumnInfo(name = "msg_count")
    val msgCount: Int = 0,
    @ColumnInfo(name = "last_ts")
    val lastTs: Long = 0,
    val preview: String = "",
    val participants: String = "",

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "is_bookmarked")
    val isBookmarked: Boolean = false,
    val nickname: String? = null,
    @ColumnInfo(name = "theme_color")
    val themeColor: String? = null,
    val category: String? = null,
    @ColumnInfo(name = "is_hidden", defaultValue = "0")
    val isHidden: Boolean = false,
    @ColumnInfo(name = "custom_wallpaper")
    val customWallpaper: String? = null,
    @ColumnInfo(name = "custom_avatar_uri")
    val customAvatarUri: String? = null,
    @ColumnInfo(name = "last_scroll_index", defaultValue = "0")
    val lastScrollIndex: Int = 0,
    @ColumnInfo(name = "last_scroll_offset", defaultValue = "0")
    val lastScrollOffset: Int = 0
)
