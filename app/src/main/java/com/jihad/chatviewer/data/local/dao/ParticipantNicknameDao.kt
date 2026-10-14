package com.jihad.chatviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jihad.chatviewer.data.local.entity.ParticipantNicknameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantNicknameDao {

    @Query("SELECT * FROM participant_nicknames")
    fun getAllNicknames(): Flow<List<ParticipantNicknameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setNickname(entity: ParticipantNicknameEntity)

    @Query("DELETE FROM participant_nicknames WHERE original_name = :originalName")
    suspend fun removeNickname(originalName: String)
}
