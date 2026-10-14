package com.jihad.chatviewer.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.abs

@Composable
fun ChatAvatar(
    name: String,
    avatarUri: String?,
    size: Dp = 46.dp
) {
    if (!avatarUri.isNullOrBlank()) {
        AsyncImage(
            model = Uri.parse(avatarUri),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        val backgroundColor = remember(name) { generateAvatarColor(name) }
        val initials = remember(name) {
            val parts = name.trim().split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            } else {
                name.take(1).uppercase().ifEmpty { "?" }
            }
        }

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = if (size > 48.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

private fun generateAvatarColor(seed: String): Color {
    val colors = listOf(
        Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA),
        Color(0xFF5E35B1), Color(0xFF3949AB), Color(0xFF1E88E5),
        Color(0xFF00ACC1), Color(0xFF00897B), Color(0xFF43A047),
        Color(0xFF7CB342), Color(0xFFFB8C00), Color(0xFFF4511E)
    )
    val index = abs(seed.hashCode()) % colors.size
    return colors[index]
}
