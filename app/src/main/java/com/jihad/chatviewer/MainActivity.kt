package com.jihad.chatviewer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import com.jihad.chatviewer.data.local.preferences.UserPreferencesDataStore
import com.jihad.chatviewer.navigation.Screen
import com.jihad.chatviewer.ui.chatlist.ChatListScreen
import com.jihad.chatviewer.ui.lock.AppLockScreen
import com.jihad.chatviewer.ui.saved.SavedScreen
import com.jihad.chatviewer.ui.search.SearchScreen
import com.jihad.chatviewer.ui.settings.SettingsScreen
import com.jihad.chatviewer.ui.theme.ChatViewerTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val prefs = UserPreferencesDataStore(this)

        setContent {
            val isLockEnabled by prefs.isAppLockEnabled.collectAsState(initial = false)
            var isUnlocked by remember { mutableStateOf(false) }

            ChatViewerTheme {
                if (isLockEnabled && !isUnlocked) {
                    AppLockScreen(
                        activity = this,
                        prefs = prefs,
                        onUnlocked = { isUnlocked = true }
                    )
                } else {
                    MainNavigationScaffold()
                }
            }
        }
    }
}

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

@Composable
fun MainNavigationScaffold() {
    var currentTab by remember { mutableStateOf<Screen>(Screen.Chats) }

    val navItems = listOf(
        NavItem(Screen.Chats, "Chats", Icons.Default.Chat),
        NavItem(Screen.Saved, "Saved", Icons.Default.Bookmark),
        NavItem(Screen.Search, "Search", Icons.Default.Search),
        NavItem(Screen.Settings, "Settings", Icons.Default.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentTab.route == item.screen.route,
                        onClick = { currentTab = item.screen },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "TabTransition"
        ) { tab ->
            when (tab) {
                is Screen.Chats -> ChatListScreen(onOpenChat = { })
                is Screen.Saved -> SavedScreen(onNavigateToMessage = { _, _ -> })
                is Screen.Search -> SearchScreen(onNavigateToMessage = { _, _ -> })
                is Screen.Settings -> SettingsScreen(onReimport = { })
                else -> Unit
            }
        }
    }
}
