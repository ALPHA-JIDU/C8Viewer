package com.jihad.chatviewer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jihad.chatviewer.data.local.dao.MessageDao
import com.jihad.chatviewer.data.local.dao.ParticipantNicknameDao
import com.jihad.chatviewer.data.local.dao.RecentSearchDao
import com.jihad.chatviewer.data.local.dao.ThreadDao
import com.jihad.chatviewer.data.local.entity.MessageEntity
import com.jihad.chatviewer.data.local.entity.ParticipantNicknameEntity
import com.jihad.chatviewer.data.local.entity.RecentSearchEntity
import com.jihad.chatviewer.data.local.entity.ThreadEntity

@Database(
    entities = [
        ThreadEntity::class,
        MessageEntity::class,
        RecentSearchEntity::class,
        ParticipantNicknameEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun threadDao(): ThreadDao
    abstract fun messageDao(): MessageDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun participantNicknameDao(): ParticipantNicknameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chats.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
