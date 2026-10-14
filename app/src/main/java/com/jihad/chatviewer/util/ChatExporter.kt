package com.jihad.chatviewer.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.jihad.chatviewer.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ChatExporter {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun exportToPlainText(messages: List<MessageEntity>, title: String, destFile: File) {
        destFile.printWriter().use { out ->
            out.println("Chat Archive: $title")
            out.println("Exported on: ${Instant.now()}")
            out.println("Total Messages: ${messages.size}")
            out.println("========================================\n")
            for (m in messages) {
                val date = DATE_FORMATTER.format(Instant.ofEpochMilli(m.ts).atZone(ZoneId.systemDefault()))
                out.println("[$date] ${m.sender}:")
                m.content?.let { out.println(it) }
                m.media?.let { out.println("[Attached Media]") }
                out.println()
            }
        }
    }

    suspend fun exportToPdf(messages: List<MessageEntity>, title: String, destFile: File) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        var pageNumber = 1

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
        }

        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }

        val senderPaint = Paint().apply {
            color = Color.rgb(30, 136, 229)
            textSize = 10f
            isFakeBoldText = true
        }

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 20f

        canvas.drawText("Chat Export: $title", margin, y, titlePaint)
        y += 24f

        for (m in messages) {
            val dateStr = DATE_FORMATTER.format(Instant.ofEpochMilli(m.ts).atZone(ZoneId.systemDefault()))
            val header = "${m.sender}  •  $dateStr"

            if (y > pageHeight - margin - 30f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin
            }

            canvas.drawText(header, margin, y, senderPaint)
            y += 14f

            val content = m.content ?: "[Attached Media]"
            canvas.drawText(content.take(85), margin, y, bodyPaint)
            y += 18f
        }

        document.finishPage(page)
        FileOutputStream(destFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }
}
