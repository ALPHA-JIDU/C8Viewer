package com.jihad.chatviewer.util

import android.content.Context
import android.net.Uri
import com.jihad.chatviewer.data.local.AppDatabase
import com.jihad.chatviewer.data.local.entity.ParticipantNicknameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreManager {

    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val threads = db.threadDao().getAllVisibleThreads().first()
        val nicknames = db.participantNicknameDao().getAllNicknames().first()

        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val threadArray = JSONArray()
        for (t in threads) {
            val obj = JSONObject().apply {
                put("folder", t.folder)
                put("nickname", t.nickname)
                put("isPinned", t.isPinned)
                put("isBookmarked", t.isBookmarked)
                put("themeColor", t.themeColor)
                put("category", t.category)
                put("isHidden", t.isHidden)
                put("customWallpaper", t.customWallpaper)
            }
            threadArray.put(obj)
        }
        root.put("threads", threadArray)

        val nickArray = JSONArray()
        for (n in nicknames) {
            nickArray.put(JSONObject().apply {
                put("originalName", n.originalName)
                put("nickname", n.nickname)
            })
        }
        root.put("nicknames", nickArray)

        root.toString(2)
    }

    suspend fun restoreBackupJson(context: Context, jsonUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        try {
            val content = context.contentResolver.openInputStream(jsonUri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext false

            val root = JSONObject(content)
            val threadArray = root.optJSONArray("threads") ?: JSONArray()

            for (i in 0 until threadArray.length()) {
                val obj = threadArray.getJSONObject(i)
                val folder = obj.getString("folder")
                val existing = db.threadDao().getThreadByFolder(folder)
                if (existing != null) {
                    val updated = existing.copy(
                        nickname = if (obj.has("nickname")) obj.optString("nickname") else existing.nickname,
                        isPinned = obj.optBoolean("isPinned", existing.isPinned),
                        isBookmarked = obj.optBoolean("isBookmarked", existing.isBookmarked),
                        themeColor = obj.optString("themeColor", existing.themeColor),
                        category = obj.optString("category", existing.category),
                        isHidden = obj.optBoolean("isHidden", existing.isHidden),
                        customWallpaper = obj.optString("customWallpaper", existing.customWallpaper)
                    )
                    db.threadDao().updateThread(updated)
                }
            }

            val nickArray = root.optJSONArray("nicknames") ?: JSONArray()
            for (i in 0 until nickArray.length()) {
                val obj = nickArray.getJSONObject(i)
                db.participantNicknameDao().setNickname(
                    ParticipantNicknameEntity(
                        originalName = obj.getString("originalName"),
                        nickname = obj.getString("nickname")
                    )
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
