package com.jihad.chatviewer.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jihad.chatviewer.data.local.AppDatabase
import com.jihad.chatviewer.data.local.entity.MessageEntity
import com.jihad.chatviewer.data.local.entity.ThreadEntity
import com.jihad.chatviewer.data.local.preferences.UserPreferencesDataStore
import com.jihad.chatviewer.data.model.MediaItem
import com.jihad.chatviewer.ui.media.ChatGalleryScreen
import com.jihad.chatviewer.ui.media.ImageViewerDialog
import com.jihad.chatviewer.ui.media.VideoPlayerDialog
import com.jihad.chatviewer.ui.media.VoicePlayerView
import com.jihad.chatviewer.ui.theme.ChatThemePresets
import com.jihad.chatviewer.util.MediaHelper
import com.jihad.chatviewer.util.SafUriHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    thread: ThreadEntity,
    messages: List<MessageEntity>,
    pinnedMessages: List<MessageEntity>,
    allMediaUris: List<Uri>,
    onTogglePin: (Long, Boolean) -> Unit,
    onToggleBookmark: (Long, Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val prefs = remember { UserPreferencesDataStore(context) }
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        initialFirstVisibleItemScrollOffset = 0
    )

    var showResumeBanner by remember { mutableStateOf(thread.lastScrollIndex > 5) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Multi-select state
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // Media viewer states
    var fullscreenImageIndex by remember { mutableStateOf<Int?>(null) }
    var activeVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showGallery by remember { mutableStateOf(false) }

    LaunchedEffect(thread.id) {
        prefs.saveLastOpenedThreadId(thread.id)
    }

    DisposableEffect(thread.id) {
        onDispose {
            scope.launch {
                db.threadDao().updateScrollPosition(
                    id = thread.id,
                    index = listState.firstVisibleItemIndex,
                    offset = listState.firstVisibleItemScrollOffset
                )
            }
        }
    }

    if (showGallery) {
        ChatGalleryScreen(
            chatTitle = thread.title,
            mediaList = allMediaUris,
            onBack = { showGallery = false }
        )
        return
    }

    val wallpaperUri = thread.customWallpaper
    val isSolidColor = wallpaperUri?.startsWith("#") == true
    val chatTheme = remember(thread.themeColor) { ChatThemePresets.findTheme(thread.themeColor) }

    Scaffold(
        topBar = {
            if (isSelectMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val selectedMsgs = messages.filter { it.id in selectedIds }
                            val combinedText = selectedMsgs.joinToString("\n\n") { "${it.sender}: ${it.content.orEmpty()}" }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Chat Messages", combinedText))
                            Toast.makeText(context, "Copied ${selectedMsgs.size} messages", Toast.LENGTH_SHORT).show()
                            isSelectMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = {
                            val selectedMsgs = messages.filter { it.id in selectedIds }
                            val combinedText = selectedMsgs.joinToString("\n\n") { "${it.sender}: ${it.content.orEmpty()}" }
                            MediaHelper.shareText(context, combinedText)
                            isSelectMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(thread.nickname ?: thread.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "%,d messages".format(thread.msgCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Jump to date")
                        }
                        IconButton(onClick = { showGallery = true }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Media Gallery")
                        }
                        IconButton(onClick = onOpenInfo) {
                            Icon(Icons.Default.Info, contentDescription = "Chat Info")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isSolidColor -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(AndroidColor.parseColor(wallpaperUri)))
                    )
                }
                !wallpaperUri.isNullOrBlank() -> {
                    AsyncImage(
                        model = Uri.parse(wallpaperUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                PinnedMessagesBar(
                    pinnedMessages = pinnedMessages,
                    onMessageClick = { msg ->
                        val idx = messages.indexOfFirst { it.id == msg.id }
                        if (idx != -1) {
                            scope.launch { listState.animateScrollToItem(idx) }
                        }
                    }
                )

                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(messages.size, key = { messages[it].id }) { index ->
                        val msg = messages[index]
                        MessageBubble(
                            message = msg,
                            isMine = msg.sender == thread.title,
                            themeContainer = chatTheme.container,
                            isSelectMode = isSelectMode,
                            isSelected = msg.id in selectedIds,
                            onSelectToggle = {
                                if (msg.id in selectedIds) selectedIds.remove(msg.id) else selectedIds.add(msg.id)
                            },
                            onStartSelectMode = {
                                isSelectMode = true
                                selectedIds.add(msg.id)
                            },
                            onTogglePin = { onTogglePin(msg.id, !msg.isPinned) },
                            onToggleBookmark = { onToggleBookmark(msg.id, !msg.isBookmarked) },
                            onImageClick = { uri ->
                                val matchIndex = allMediaUris.indexOf(uri)
                                fullscreenImageIndex = if (matchIndex != -1) matchIndex else 0
                            },
                            onVideoClick = { uri -> activeVideoUri = uri }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showResumeBanner,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Continue where you left off?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = {
                                showResumeBanner = false
                                scope.launch { listState.animateScrollToItem(thread.lastScrollIndex) }
                            }
                        ) {
                            Text("Jump")
                        }
                        IconButton(onClick = { showResumeBanner = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            JumpToDateDialog(
                onDateSelected = { selectedEpochMs ->
                    val closestIndex = messages.indexOfFirst { it.ts <= selectedEpochMs }
                    if (closestIndex != -1) {
                        scope.launch { listState.animateScrollToItem(closestIndex) }
                    }
                },
                onDismiss = { showDatePicker = false }
            )
        }

        fullscreenImageIndex?.let { idx ->
            ImageViewerDialog(
                mediaUris = allMediaUris,
                initialIndex = idx,
                onDismiss = { fullscreenImageIndex = null }
            )
        }

        activeVideoUri?.let { vUri ->
            VideoPlayerDialog(
                videoUri = vUri,
                onDismiss = { activeVideoUri = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageEntity,
    isMine: Boolean,
    themeContainer: Color,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onStartSelectMode: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleBookmark: () -> Unit,
    onImageClick: (Uri) -> Unit,
    onVideoClick: (Uri) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val parsedMedia = remember(message.media) {
        message.media?.split("\n")?.mapNotNull { line ->
            val p = line.split("\t", limit = 2)
            if (p.size == 2) MediaItem(p[0], p[1]) else null
        } ?: emptyList()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectMode) onSelectToggle() },
                onLongClick = { if (!isSelectMode) showMenu = true else onSelectToggle() }
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectMode) {
            Checkbox(checked = isSelected, onCheckedChange = { onSelectToggle() })
            Spacer(modifier = Modifier.width(4.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isMine) themeContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (message.isPinned) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pinned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (message.unsent) {
                        Text("Message unsent", fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyMedium)
                    }

                    for (item in parsedMedia) {
                        val uri = SafUriHelper.resolveMediaUri(context, item.uri)
                        if (uri != null) {
                            when (item.type) {
                                "photo", "gif", "sticker" -> {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .combinedClickable(onClick = { onImageClick(uri) })
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                "video" -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onVideoClick(uri) }).padding(8.dp)
                                    ) {
                                        Text("▶ Play Video", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                "audio" -> {
                                    VoicePlayerView(audioUri = uri)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    message.content?.let { text ->
                        Text(text = text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.content.orEmpty()))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        MediaHelper.shareText(context, "${message.sender}: ${message.content.orEmpty()}")
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (message.isPinned) "Unpin" else "Pin") },
                    onClick = {
                        onTogglePin()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (message.isBookmarked) "Remove Bookmark" else "Bookmark") },
                    onClick = {
                        onToggleBookmark()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Select") },
                    onClick = {
                        showMenu = false
                        onStartSelectMode()
                    }
                )
            }
        }
    }
}
