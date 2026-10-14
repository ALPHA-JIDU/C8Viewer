package com.jihad.chatviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.jihad.chatviewer.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

data class SearchResultItem(
    val id: Long,
    val threadId: Long,
    val threadTitle: String,
    val sender: String,
    val ts: Long,
    val content: String?
)

data class MediaCounts(
    val photos: Int,
    val videos: Int,
    val voiceNotes: Int,
    val files: Int,
    val links: Int
)

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY ts DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedMessages(threadId: Long, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY ts ASC")
    suspend fun getAllMessagesForThread(threadId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND is_pinned = 1 ORDER BY ts DESC")
    fun getPinnedMessages(threadId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE is_pinned = 1 ORDER BY ts DESC")
    fun getAllPinnedMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE is_bookmarked = 1 ORDER BY ts DESC")
    fun getAllBookmarkedMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND media IS NOT NULL AND media != '' ORDER BY ts DESC")
    fun getMediaMessages(threadId: Long): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET is_pinned = :isPinned WHERE id = :msgId")
    suspend fun updatePinned(msgId: Long, isPinned: Boolean)

    @Query("UPDATE messages SET is_bookmarked = :isBookmarked, bookmark_note = :note, bookmark_label = :label WHERE id = :msgId")
    suspend fun updateBookmarkDetails(msgId: Long, isBookmarked: Boolean, note: String?, label: String?)

    @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId AND (ts > :ts OR (ts = :ts AND id > :msgId))")
    suspend fun getPositionOfMessage(threadId: Long, msgId: Long, ts: Long): Int

    @Query(
        "SELECT " +
            "SUM(CASE WHEN media LIKE '%photo%' OR media LIKE '%sticker%' OR media LIKE '%gif%' THEN 1 ELSE 0 END) AS photos, " +
            "SUM(CASE WHEN media LIKE '%video%' THEN 1 ELSE 0 END) AS videos, " +
            "SUM(CASE WHEN media LIKE '%audio%' THEN 1 ELSE 0 END) AS voiceNotes, " +
            "SUM(CASE WHEN media LIKE '%file%' THEN 1 ELSE 0 END) AS files, " +
            "SUM(CASE WHEN link IS NOT NULL AND link != '' THEN 1 ELSE 0 END) AS links " +
            "FROM messages WHERE thread_id = :threadId"
    )
    suspend fun getMediaCounts(threadId: Long): MediaCounts?

    @Query(
        "SELECT * FROM messages WHERE thread_id = :threadId AND (" +
            "content LIKE '%called%' OR content LIKE '%video chat%' OR content LIKE '%missed call%') " +
            "ORDER BY ts DESC"
    )
    suspend fun getCallHistory(threadId: Long): List<MessageEntity>

    @RawQuery
    suspend fun searchMessagesRaw(query: SupportSQLiteQuery): List<SearchResultItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE thread_id = :threadId")
    suspend fun deleteMessagesForThread(threadId: Long)
}
