package com.jihad.chatviewer.data.importer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.util.JsonReader
import com.jihad.chatviewer.data.model.DiscoveredChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ArchiveScanner {

    private val MESSAGE_FILE_REGEX = Regex("""message_(\d+)\.json""")
    private val SECTIONS = setOf("inbox", "archived_threads", "filtered_threads", "message_requests", "e2ee_cutover")

    suspend fun scanFolder(context: Context, treeUri: Uri): List<DiscoveredChat> = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val children = getFolderChildren(context, treeUri, rootDocId)

        val messagesDocId = findMessagesFolder(context, treeUri, rootDocId, children)
            ?: throw IllegalArgumentException("Could not find a 'messages' directory in selected folder.")

        val sectionDirs = getFolderChildren(context, treeUri, messagesDocId)
            .filter { it.isDir && it.name in SECTIONS }

        val discovered = mutableListOf<DiscoveredChat>()

        for (section in sectionDirs) {
            val threads = getFolderChildren(context, treeUri, section.id).filter { it.isDir }
            for (t in threads) {
                val files = getFolderChildren(context, treeUri, t.id)
                    .filter { !it.isDir && MESSAGE_FILE_REGEX.matches(it.name) }
                if (files.isNotEmpty()) {
                    var title = t.name
                    var count = 0
                    val firstDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, files.first().id)
                    context.contentResolver.openInputStream(firstDocUri)?.use { stream ->
                        val info = quickInspectJson(stream)
                        if (!info.first.isNullOrBlank()) title = info.first!!
                        count += info.second
                    }
                    count += (files.size - 1) * 100
                    discovered.add(
                        DiscoveredChat(
                            folder = t.name,
                            title = title,
                            estimatedMessages = count,
                            section = section.name
                        )
                    )
                }
            }
        }
        discovered.sortedByDescending { it.estimatedMessages }
    }

    suspend fun scanZipFiles(context: Context, zipUris: List<Uri>): List<DiscoveredChat> = withContext(Dispatchers.IO) {
        val chatMap = mutableMapOf<String, Pair<String, Int>>()

        for (uri in zipUris) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream.buffered()).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val path = entry.name
                            val parts = path.split("/")
                            val messagesIdx = parts.indexOf("messages")
                            if (messagesIdx != -1 && parts.size > messagesIdx + 2) {
                                val section = parts[messagesIdx + 1]
                                val threadFolder = parts[messagesIdx + 2]
                                val fileName = parts.last()

                                if (section in SECTIONS && MESSAGE_FILE_REGEX.matches(fileName)) {
                                    val current = chatMap[threadFolder] ?: Pair(threadFolder, 0)
                                    chatMap[threadFolder] = current.copy(second = current.second + 100)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        }

        chatMap.map { (folder, data) ->
            DiscoveredChat(
                folder = folder,
                title = data.first,
                estimatedMessages = maxOf(1, data.second),
                section = "inbox"
            )
        }.sortedByDescending { it.estimatedMessages }
    }

    private data class DocChild(val id: String, val name: String, val isDir: Boolean)

    private fun getFolderChildren(context: Context, treeUri: Uri, parentDocId: String): List<DocChild> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val out = mutableListOf<DocChild>()
        context.contentResolver.query(
            uri,
            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                out.add(
                    DocChild(
                        id = c.getString(0),
                        name = c.getString(1) ?: "",
                        isDir = c.getString(2) == Document.MIME_TYPE_DIR
                    )
                )
            }
        }
        return out
    }

    private fun findMessagesFolder(
        context: Context,
        treeUri: Uri,
        rootDocId: String,
        children: List<DocChild>
    ): String? {
        val direct = children.firstOrNull { it.isDir && it.name == "messages" }
        if (direct != null) return direct.id

        val activity = children.firstOrNull { it.isDir && it.name == "your_facebook_activity" }
        if (activity != null) {
            val sub = getFolderChildren(context, treeUri, activity.id)
            return sub.firstOrNull { it.isDir && it.name == "messages" }?.id
        }
        return null
    }

    private fun quickInspectJson(stream: InputStream): Pair<String?, Int> {
        var title: String? = null
        var count = 0
        try {
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "title" -> title = fixEncoding(reader.nextString())
                        "messages" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.skipValue()
                                count++
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (_: Exception) {}
        return Pair(title, count)
    }

    private fun fixEncoding(s: String): String {
        for (ch in s) if (ch.code > 0xFF) return s
        return try {
            String(s.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        } catch (_: Exception) {
            s
        }
    }
}
