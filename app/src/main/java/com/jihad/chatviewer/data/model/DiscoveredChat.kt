package com.jihad.chatviewer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DiscoveredChat(
    val folder: String,
    val title: String,
    val estimatedMessages: Int,
    val section: String,
    val isSelected: Boolean = true
) : Parcelable
