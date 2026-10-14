package com.jihad.chatviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jihad.chatviewer.data.local.entity.ThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {

    @Query("SELECT * FROM threads WHERE msg_count > 0 AND is_hidden = 0 ORDER BY is_pinned DESC, last_ts DESC")
    fun getAllVisibleThreads(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE is_hidden = 1 ORDER BY last_ts DESC")
    fun getAllHiddenThreads(): Flow<List<ThreadEntity>>

    @Query("SELECT DISTINCT category FROM threads WHERE category IS NOT NULL AND category != ''")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM threads WHERE id = :id LIMIT 1")
    suspend fun getThreadById(id: Long): ThreadEntity?

    @Query("SELECT * FROM threads WHERE folder = :folder LIMIT 1")
    suspend fun getThreadByFolder(folder: String): ThreadEntity?

    @Query("UPDATE threads SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String?)

    @Query("UPDATE threads SET custom_avatar_uri = :avatarUri WHERE id = :id")
    suspend fun updateCustomAvatar(id: Long, avatarUri: String?)

    @Query("UPDATE threads SET last_scroll_index = :index, last_scroll_offset = :offset WHERE id = :id")
    suspend fun updateScrollPosition(id: Long, index: Int, offset: Int)

    @Query("UPDATE threads SET theme_color = :themeColor, custom_wallpaper = :wallpaper WHERE id = :id")
    suspend fun updateThemeAndWallpaper(id: Long, themeColor: String?, wallpaper: String?)

    @Query("UPDATE threads SET nickname = :nickname WHERE id = :id")
    suspend fun updateNickname(id: Long, nickname: String?)

    @Query("UPDATE threads SET is_hidden = :hidden WHERE id = :id")
    suspend fun updateHidden(id: Long, hidden: Boolean)

    @Query("DELETE FROM threads WHERE id = :id")
    suspend fun deleteThread(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ThreadEntity): Long

    @Update
    suspend fun updateThread(thread: ThreadEntity)

    @Transaction
    suspend fun upsertPreservingUserData(
        title: String,
        folder: String,
        people: Int,
        msgCount: Int,
        lastTs: Long,
        preview: String,
        participants: String
    ): Long {
        val existing = getThreadByFolder(folder)
        return if (existing != null) {
            val updated = existing.copy(
                title = existing.nickname ?: title,
                folder = folder,
                people = people,
                msgCount = msgCount,
                lastTs = lastTs,
                preview = preview,
                participants = participants
            )
            updateThread(updated)
            existing.id
        } else {
            insertThread(
                ThreadEntity(
                    title = title,
                    folder = folder,
                    people = people,
                    msgCount = msgCount,
                    lastTs = lastTs,
                    preview = preview,
                    participants = participants
                )
            )
        }
    }
}
