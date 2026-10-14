package com.jihad.chatviewer.ui.chatlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jihad.chatviewer.data.local.AppDatabase
import com.jihad.chatviewer.data.local.entity.ThreadEntity
import com.jihad.chatviewer.ui.components.ChatAvatar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChatListScreen(onOpenChat: (Long) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val threads by db.threadDao().getAllVisibleThreads().collectAsState(initial = emptyList())
    val customCategories by db.threadDao().getAllCategories().collectAsState(initial = emptyList())

    val baseCategories = listOf("All", "Favourites", "Family", "Work")
    val allTabs = remember(customCategories) {
        (baseCategories + customCategories.filter { it !in baseCategories }).distinct()
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var movingThread by remember { mutableStateOf<ThreadEntity?>(null) }

    val activeCategory = allTabs.getOrElse(selectedTabIndex) { "All" }
    val filteredThreads = remember(threads, activeCategory) {
        when (activeCategory) {
            "All" -> threads
            "Favourites" -> threads.filter { it.isPinned }
            else -> threads.filter { it.category.equals(activeCategory, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chats", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { showNewCategoryDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            allTabs.forEachIndexed { index, tabTitle ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(tabTitle) }
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredThreads, key = { it.id }) { thread ->
                ThreadListItem(
                    thread = thread,
                    onClick = { onOpenChat(thread.id) },
                    onMoveCategory = { movingThread = thread }
                )
            }
        }
    }

    movingThread?.let { target ->
        val categoryOptions = listOf(null, "Family", "Work") + customCategories

        AlertDialog(
            onDismissRequest = { movingThread = null },
            title = { Text("Move '${target.title}' to Category") },
            text = {
                Column {
                    categoryOptions.forEach { cat ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    db.threadDao().updateCategory(target.id, cat)
                                    movingThread = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cat ?: "None (General)")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { movingThread = null }) { Text("Cancel") }
            }
        )
    }

    if (showNewCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Create New Category") },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    showNewCategoryDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThreadListItem(
    thread: ThreadEntity,
    onClick: () -> Unit,
    onMoveCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(name = thread.nickname ?: thread.title, avatarUri = thread.customAvatarUri)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (thread.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(thread.nickname ?: thread.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                thread.preview.ifEmpty { "(Media)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            val dateStr = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                .format(Instant.ofEpochMilli(thread.lastTs).atZone(ZoneId.systemDefault()))
            Text(dateStr, style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = onMoveCategory, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.DriveFileMove, contentDescription = "Move Category", modifier = Modifier.size(16.dp))
            }
        }
    }
}
