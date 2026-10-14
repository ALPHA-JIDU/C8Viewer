package com.jihad.chatviewer.data.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.jihad.chatviewer.data.local.AppDatabase
import com.jihad.chatviewer.data.local.preferences.UserPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val db = AppDatabase.getInstance(appContext)
    private val prefs = UserPreferencesDataStore(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val selectedFolders = inputData.getStringArray(KEY_SELECTED_FOLDERS)?.toSet() ?: emptySet()
        val sourceUriStr = inputData.getString(KEY_SOURCE_URI) ?: return@withContext Result.failure()
        val isZip = inputData.getBoolean(KEY_IS_ZIP, false)
        val sourceUri = Uri.parse(sourceUriStr)

        createNotificationChannel()
        setForeground(createForegroundInfo(0, "Starting import...", 0, selectedFolders.size))

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ChatViewer:ImportWakeLock")
        wakeLock.acquire(60 * 60 * 1000L)

        try {
            var importedCount = 0
            val totalChats = selectedFolders.size
            val senderCounts = mutableMapOf<String, Int>()

            for ((index, folderName) in selectedFolders.withIndex()) {
                if (isStopped) {
                    return@withContext Result.failure()
                }

                val progressPercent = if (totalChats > 0) ((index.toFloat() / totalChats) * 100).toInt() else 0
                val statusText = "Importing $folderName (${index + 1}/$totalChats)"

                setForeground(createForegroundInfo(progressPercent, statusText, index + 1, totalChats))
                setProgress(
                    workDataOf(
                        PROGRESS_PERCENT to progressPercent,
                        PROGRESS_STATUS to statusText
                    )
                )

                db.threadDao().upsertPreservingUserData(
                    title = folderName,
                    folder = folderName,
                    people = 0,
                    msgCount = 0,
                    lastTs = 0L,
                    preview = "",
                    participants = ""
                )

                importedCount++
            }

            val detectedMe = senderCounts.maxByOrNull { it.value }?.key ?: ""
            if (detectedMe.isNotBlank()) {
                prefs.saveMyName(detectedMe)
            }
            prefs.setImported(true)

            Result.success(workDataOf(KEY_RESULT_COUNT to importedCount))
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    private fun createForegroundInfo(
        progress: Int,
        chatTitle: String,
        current: Int,
        total: Int
    ): ForegroundInfo {
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Importing Messages")
            .setContentText(chatTitle)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Archive Import",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during Facebook archive import"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "chat_import_channel"
        const val NOTIFICATION_ID = 2001

        const val KEY_SOURCE_URI = "source_uri"
        const val KEY_IS_ZIP = "is_zip"
        const val KEY_SELECTED_FOLDERS = "selected_folders"

        const val PROGRESS_PERCENT = "progress_percent"
        const val PROGRESS_STATUS = "progress_status"
        const val KEY_RESULT_COUNT = "result_count"
    }
}
