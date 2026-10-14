package com.jihad.chatviewer.ui.export

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jihad.chatviewer.data.local.AppDatabase
import com.jihad.chatviewer.util.ChatExporter
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExportChatDialog(
    threadId: Long,
    chatTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPdf by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Chat") },
        text = {
            Column {
                Text("Select export format:")
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = isPdf,
                        onClick = { isPdf = true },
                        label = { Text("PDF Document (.pdf)") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = !isPdf,
                        onClick = { isPdf = false },
                        label = { Text("Plain Text (.txt)") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    val db = AppDatabase.getInstance(context)
                    val messages = db.messageDao().getAllMessagesForThread(threadId)
                    val ext = if (isPdf) "pdf" else "txt"
                    val file = File(context.cacheDir, "${chatTitle.replace(" ", "_")}_export.$ext")

                    if (isPdf) {
                        ChatExporter.exportToPdf(messages, chatTitle, file)
                    } else {
                        ChatExporter.exportToPlainText(messages, chatTitle, file)
                    }

                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = if (isPdf) "application/pdf" else "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export Chat"))
                    onDismiss()
                }
            }) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
