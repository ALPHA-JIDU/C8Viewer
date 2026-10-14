package com.jihad.chatviewer.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.sqlite.db.SimpleSQLiteQuery
import com.jihad.chatviewer.data.local.AppDatabase
import com.jihad.chatviewer.data.local.dao.SearchResultItem
import com.jihad.chatviewer.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    initialThreadId: Long? = null,
    onNavigateToMessage: (threadId: Long, msgId: Long) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(SearchFilter(threadId = initialThreadId)) }
    var results by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val recentSearches by db.recentSearchDao().getRecentSearches().collectAsState(initial = emptyList())

    fun executeSearch() {
        if (query.isBlank()) return
        scope.launch {
            db.recentSearchDao().saveSearch(RecentSearchEntity(query = query.trim()))

            val queryBuilder = StringBuilder(
                "SELECT m.id, m.thread_id AS threadId, t.title AS threadTitle, m.sender, m.ts, m.content " +
                    "FROM messages m JOIN threads t ON t.id = m.thread_id WHERE "
            )
            val args = mutableListOf<Any>()

            if (filters.isExactPhrase) {
                queryBuilder.append("m.content LIKE ? ")
                args.add("%${query.trim()}%")
            } else {
                val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                words.forEachIndexed { idx, word ->
                    if (idx > 0) queryBuilder.append("AND ")
                    queryBuilder.append("m.content LIKE ? ")
                    args.add("%$word%")
                }
            }

            filters.threadId?.let {
                queryBuilder.append("AND m.thread_id = ? ")
                args.add(it)
            }

            filters.senderName?.takeIf { it.isNotBlank() }?.let {
                queryBuilder.append("AND m.sender LIKE ? ")
                args.add("%$it%")
            }

            when (filters.mediaType) {
                MediaTypeFilter.PHOTO -> queryBuilder.append("AND m.media LIKE '%photo%' ")
                MediaTypeFilter.VIDEO -> queryBuilder.append("AND m.media LIKE '%video%' ")
                MediaTypeFilter.VOICE -> queryBuilder.append("AND m.media LIKE '%audio%' ")
                MediaTypeFilter.LINK -> queryBuilder.append("AND m.link IS NOT NULL ")
                MediaTypeFilter.FILE -> queryBuilder.append("AND m.media LIKE '%file%' ")
                MediaTypeFilter.ALL -> Unit
            }

            queryBuilder.append("ORDER BY m.ts DESC LIMIT 200")

            val rawQuery = SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
            results = withContext(Dispatchers.IO) {
                db.messageDao().searchMessagesRaw(rawQuery)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = { Text("Search messages") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; results = emptyList() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (query.isBlank() && recentSearches.isNotEmpty()) {
            Text("Recent Searches", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recentSearches.forEach { item ->
                    InputChip(
                        selected = false,
                        onClick = {
                            query = item.query
                            executeSearch()
                        },
                        label = { Text(item.query) },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { scope.launch { db.recentSearchDao().deleteSearch(item.id) } },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    )
                }
            }
        }

        if (query.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${results.size} matches", style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = { executeSearch() }) { Text("Search") }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(results, key = { it.id }) { hit ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigateToMessage(hit.threadId, hit.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "${hit.threadTitle} • ${hit.sender}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatTimestamp(hit.ts),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = highlightQuery(hit.content.orEmpty(), query, MaterialTheme.colorScheme.primaryContainer),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Search Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                FilterChip(
                    selected = filters.isExactPhrase,
                    onClick = { filters = filters.copy(isExactPhrase = !filters.isExactPhrase) },
                    label = { Text("Exact phrase match") }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Media Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaTypeFilter.values().forEach { type ->
                        FilterChip(
                            selected = filters.mediaType == type,
                            onClick = { filters = filters.copy(mediaType = type) },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        filters = SearchFilter(threadId = initialThreadId)
                        showFilterSheet = false
                        executeSearch()
                    }) {
                        Text("Reset")
                    }
                    TextButton(onClick = {
                        showFilterSheet = false
                        executeSearch()
                    }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

private fun highlightQuery(text: String, query: String, highlightColor: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase().trim()
    val builder = AnnotatedString.Builder(text)

    var index = lowerText.indexOf(lowerQuery)
    while (index >= 0) {
        builder.addStyle(
            SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold),
            index,
            index + lowerQuery.length
        )
        index = lowerText.indexOf(lowerQuery, index + lowerQuery.length)
    }
    return builder.toAnnotatedString()
}

private fun formatTimestamp(ts: Long): String {
    return DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
        .format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
}
