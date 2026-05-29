package com.muhex.mumu.settings.tabs

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muhex.mumu.settings.SectionHeader
import com.muhex.mumu.settings.SettingToggle
import com.muhex.mumu.settings.UnifiedHomeItem

@Composable
fun LayoutsTab(prefs: SharedPreferences, contentColor: Color) {
    val layouts = listOf(
        UnifiedHomeItem("Clock View", "Top section with stylized clocks", Icons.Default.WatchLater, "clock_view_enabled"),
        UnifiedHomeItem("Pinned Stack", "Samsung-style app cards", Icons.Default.PushPin, "show_now_apps"),
        UnifiedHomeItem("Fixed Dock", "Quick access bottom bar", Icons.Default.ViewAgenda, "show_dock_bar"),
        UnifiedHomeItem("Gestures", "System-wide gesture control", Icons.Default.TouchApp, "gestures_enabled"),
        UnifiedHomeItem("Inspiration", "Motivational quotes on home screen", Icons.Default.FormatQuote, "quotes_enabled")
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("Features")
        }
        items(layouts) { item ->
            var isEnabled by remember { mutableStateOf(prefs.getBoolean(item.prefKey, true)) }
            SettingToggle(
                title = item.name,
                checked = isEnabled,
                contentColor = contentColor,
                icon = item.icon
            ) {
                isEnabled = it
                prefs.edit { putBoolean(item.prefKey, it) }
            }
        }
    }
}
