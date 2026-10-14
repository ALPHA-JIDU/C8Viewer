package com.jihad.chatviewer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "participant_nicknames",
    indices = [Index(value = ["original_name"], unique = true)]
)
data class ParticipantNicknameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "original_name")
    val originalName: String,
    val nickname: String
)
