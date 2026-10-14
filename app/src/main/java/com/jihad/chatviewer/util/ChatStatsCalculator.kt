package com.jihad.chatviewer.util

import com.jihad.chatviewer.data.local.entity.MessageEntity
import com.jihad.chatviewer.data.model.ChatStats
import com.jihad.chatviewer.data.model.SenderStat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ChatStatsCalculator {

    private val STOP_WORDS = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her",
        "she", "or", "an", "will", "my", "one", "all", "would", "there",
        "their", "what", "so", "up", "out", "if", "about", "who", "get",
        "which", "go", "me", "is", "are", "was", "were", "am", "ok", "yes", "no"
    )

    private val EMOJI_REGEX = Regex(
        "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+|[\\u2600-\\u27BF]|[\uFE00-\uFE0F]"
    )

    fun calculate(messages: List<MessageEntity>): ChatStats {
        if (messages.isEmpty()) {
            return ChatStats(0, emptyList(), emptyList(), "N/A", 0, emptyList(), emptyList())
        }

        val total = messages.size
        val senderMap = mutableMapOf<String, Int>()
        val hourMap = IntArray(24)
        val wordFreq = mutableMapOf<String, Int>()
        val emojiFreq = mutableMapOf<String, Int>()
        val distinctDays = mutableSetOf<LocalDate>()

        for (m in messages) {
            senderMap[m.sender] = (senderMap[m.sender] ?: 0) + 1

            val zdt = Instant.ofEpochMilli(m.ts).atZone(ZoneId.systemDefault())
            hourMap[zdt.hour]++
            distinctDays.add(zdt.toLocalDate())

            m.content?.let { text ->
                text.lowercase()
                    .split(Regex("[^\\p{L}\\p{N}]+"))
                    .filter { it.length > 2 && it !in STOP_WORDS }
                    .forEach { word -> wordFreq[word] = (wordFreq[word] ?: 0) + 1 }

                EMOJI_REGEX.findAll(text).forEach { match ->
                    val emoji = match.value
                    emojiFreq[emoji] = (emojiFreq[emoji] ?: 0) + 1
                }
            }
        }

        val senderBreakdown = senderMap.entries
            .sortedByDescending { it.value }
            .map { (sender, count) ->
                SenderStat(sender, count, (count.toFloat() / total) * 100f)
            }

        val peakHour = hourMap.indices.maxByOrNull { hourMap[it] } ?: 0
        val peakLabel = "%02d:00 - %02d:00 (%d msgs)".format(peakHour, (peakHour + 1) % 24, hourMap[peakHour])

        val longestStreak = calculateStreak(distinctDays.sorted())

        return ChatStats(
            totalMessages = total,
            senderBreakdown = senderBreakdown,
            busiestHours = hourMap.mapIndexed { h, count -> Pair(h, count) },
            peakHourLabel = peakLabel,
            longestStreakDays = longestStreak,
            topWords = wordFreq.entries.sortedByDescending { it.value }.take(10).map { Pair(it.key, it.value) },
            topEmojis = emojiFreq.entries.sortedByDescending { it.value }.take(8).map { Pair(it.key, it.value) }
        )
    }

    private fun calculateStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        var maxStreak = 1
        var current = 1

        for (i in 1 until sortedDates.size) {
            val diff = ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i])
            if (diff == 1L) {
                current++
                if (current > maxStreak) maxStreak = current
            } else if (diff > 1L) {
                current = 1
            }
        }
        return maxStreak
    }
}
