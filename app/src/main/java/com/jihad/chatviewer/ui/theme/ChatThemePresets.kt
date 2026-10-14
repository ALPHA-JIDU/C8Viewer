package com.jihad.chatviewer.ui.theme

import androidx.compose.ui.graphics.Color

object ChatThemePresets {

    data class ThemeColor(val key: String, val name: String, val primary: Color, val container: Color)

    val PRESET_COLORS = listOf(
        ThemeColor("default", "Default Blue", Color(0xFF1E88E5), Color(0xFFBBDEFB)),
        ThemeColor("emerald", "Emerald Green", Color(0xFF2E7D32), Color(0xFFC8E6C9)),
        ThemeColor("purple", "Royal Purple", Color(0xFF7B1FA2), Color(0xFFE1BEE7)),
        ThemeColor("crimson", "Crimson Red", Color(0xFFC2185B), Color(0xFFF8BBD0)),
        ThemeColor("amber", "Warm Amber", Color(0xFFF57C00), Color(0xFFFFE0B2)),
        ThemeColor("midnight", "Midnight Dark", Color(0xFF263238), Color(0xFFCFD8DC))
    )

    fun findTheme(key: String?): ThemeColor {
        return PRESET_COLORS.firstOrNull { it.key == key } ?: PRESET_COLORS.first()
    }

    val SOLID_WALLPAPERS = listOf(
        "#FFFFFF" to "Light",
        "#F3F4F6" to "Soft Gray",
        "#E8F5E9" to "Soft Green",
        "#E3F2FD" to "Soft Blue",
        "#FFF3E0" to "Soft Warm",
        "#121212" to "OLED Dark"
    )
}
