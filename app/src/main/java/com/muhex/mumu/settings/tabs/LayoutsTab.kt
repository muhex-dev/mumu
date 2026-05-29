package com.muhex.mumu.settings.tabs

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(layouts) { item ->
            var isEnabled by remember { mutableStateOf(prefs.getBoolean(item.prefKey, true)) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(contentColor.copy(alpha = 0.05f))
                    .border(1.dp, contentColor.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(item.name, color = contentColor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(item.description, color = contentColor.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        prefs.edit { putBoolean(item.prefKey, it) }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50))
                )
            }
        }
    }
}
