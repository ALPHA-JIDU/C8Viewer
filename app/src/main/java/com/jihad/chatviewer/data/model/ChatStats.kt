package com.jihad.chatviewer.data.model

data class SenderStat(
    val name: String,
    val count: Int,
    val percentage: Float
)

data class ChatStats(
    val totalMessages: Int,
    val senderBreakdown: List<SenderStat>,
    val busiestHours: List<Pair<Int, Int>>,
    val peakHourLabel: String,
    val longestStreakDays: Int,
    val topWords: List<Pair<String, Int>>,
    val topEmojis: List<Pair<String, Int>>
)
