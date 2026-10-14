package com.jihad.chatviewer.ui.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jihad.chatviewer.data.local.AppDatabase
import com.jihad.chatviewer.data.local.entity.ThreadEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val threads by db.threadDao().getAllVisibleThreads().collectAsState(initial = emptyList())

    var threadToDelete by remember { mutableStateOf<ThreadEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Usage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val totalMessages = threads.sumOf { it.msgCount }
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Messages Cached", style = MaterialTheme.typography.labelMedium)
                    Text("%,d".format(totalMessages), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Photos & videos stream directly from your archive without taking extra space.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Chats by Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(threads.sortedByDescending { it.msgCount }, key = { it.id }) { thread ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(thread.nickname ?: thread.title, fontWeight = FontWeight.SemiBold)
                            Text("%,d messages (~%d KB)".format(thread.msgCount, (thread.msgCount * 0.15).toInt()), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { threadToDelete = thread }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear chat", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    threadToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { threadToDelete = null },
            title = { Text("Clear '${target.title}'?") },
            text = { Text("This will remove this chat from the app's database. Your original export folder or ZIP files are NEVER modified or deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            db.messageDao().deleteMessagesForThread(target.id)
                            db.threadDao().deleteThread(target.id)
                            threadToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove from App") }
            },
            dismissButton = {
                TextButton(onClick = { threadToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
