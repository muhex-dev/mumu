package com.muhex.mumu.settings.tabs

import androidx.core.content.edit
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhex.mumu.AppModel
import com.muhex.mumu.AppRepository
import com.muhex.mumu.settings.*

@Composable
fun PinnedAppsTab(repository: AppRepository, prefs: SharedPreferences, contentColor: Color, backgroundColor: Color, onOpenFontPicker: (String, String) -> Unit) {
    var expandedItem by remember { mutableStateOf<String?>("apps") }
    var pinnedIds by remember { mutableStateOf(repository.getPinnedIds()) }
    val allApps = remember { mutableStateOf<List<AppModel>>(emptyList()) }
    val displayModeOptions = mapOf("both" to "Icon & Label", "icon" to "Icon Only", "label" to "Label Only")

    var nowAppsVisibleCount   by remember { mutableIntStateOf(prefs.getInt("now_apps_visible_count", 3)) }
    var nowAppsVerticalOffset by remember { mutableFloatStateOf(prefs.getFloat("now_apps_vertical_offset", 0f)) }
    var nowAppsHorizontalOffset by remember { mutableFloatStateOf(prefs.getFloat("now_apps_horizontal_offset", 0f)) }
    var nowAppsCardWidth      by remember { mutableFloatStateOf(prefs.getFloat("now_apps_card_width", 220f)) }
    var nowAppsCardHeight     by remember { mutableFloatStateOf(prefs.getFloat("now_apps_card_height", 64f)) }
    var nowAppsCornerRadius   by remember { mutableFloatStateOf(prefs.getFloat("now_apps_corner_radius", 28f)) }
    var nowAppsDisplayMode    by remember { mutableStateOf(prefs.getString("now_apps_display_mode", "both") ?: "both") }
    var nowAppsIconSize       by remember { mutableFloatStateOf(prefs.getFloat("now_apps_icon_size", 32f)) }
    var nowAppsTextSize       by remember { mutableFloatStateOf(prefs.getFloat("now_apps_text_size", 15f)) }
    var nowAppsBgColor        by remember { mutableIntStateOf(prefs.getInt("now_apps_bg_color", AndroidColor.WHITE)) }
    var nowAppsStrokeColor    by remember { mutableIntStateOf(prefs.getInt("now_apps_stroke_color", AndroidColor.WHITE)) }
    var nowAppsOpacity        by remember { mutableFloatStateOf(prefs.getFloat("now_apps_opacity", 1.0f)) }
    var nowAppsTextColor      by remember { mutableIntStateOf(prefs.getInt("now_apps_text_color", AndroidColor.BLACK)) }

    LaunchedEffect(Unit) { allApps.value = repository.getAllApps() }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SettingItem(
                icon = Icons.Default.PushPin,
                title = "Manage Apps",
                value = "${pinnedIds.size} Apps Pinned",
                contentColor = contentColor,
                isExpanded = expandedItem == "apps",
                onClick = { expandedItem = if (expandedItem == "apps") null else "apps" }
            ) {
                Column {
                    Text(
                        "Tap apps to pin them to your stack. The first app is always on top.",
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    AppSelectionContent(allApps.value, pinnedIds, repository, contentColor, backgroundColor, isPinnedApps = true) {
                        pinnedIds = it
                    }
                }
            }
        }

        item {
            SettingItem(
                icon = Icons.Default.Layers,
                title = "Stacking & Position",
                value = "$nowAppsVisibleCount Visible",
                contentColor = contentColor,
                isExpanded = expandedItem == "stack",
                onClick = { expandedItem = if (expandedItem == "stack") null else "stack" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Visible Cards", color = contentColor.copy(alpha = 0.6f), fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4, 5).forEach { count ->
                            val isSelected = nowAppsVisibleCount == count
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF4CAF50) else contentColor.copy(alpha = 0.05f))
                                    .clickable {
                                        nowAppsVisibleCount = count
                                        prefs.edit { putInt("now_apps_visible_count", count) }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$count", color = if (isSelected) Color.White else contentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SliderSettingFloat("Vertical Position", nowAppsVerticalOffset, 0f..800f, contentColor) {
                        nowAppsVerticalOffset = it
                        prefs.edit { putFloat("now_apps_vertical_offset", it) }
                    }
                    SliderSettingFloat("Horizontal Position", nowAppsHorizontalOffset, -200f..200f, contentColor) {
                        nowAppsHorizontalOffset = it
                        prefs.edit { putFloat("now_apps_horizontal_offset", it) }
                    }
                }
            }
        }

        item {
            SettingItem(
                icon = Icons.Default.AspectRatio,
                title = "Card Geometry",
                value = "${nowAppsCardWidth.toInt()} x ${nowAppsCardHeight.toInt()}",
                contentColor = contentColor,
                isExpanded = expandedItem == "geometry",
                onClick = { expandedItem = if (expandedItem == "geometry") null else "geometry" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SliderSettingFloat("Width", nowAppsCardWidth, 120f..420f, contentColor) {
                        nowAppsCardWidth = it
                        prefs.edit { putFloat("now_apps_card_width", it) }
                    }
                    SliderSettingFloat("Height", nowAppsCardHeight, 40f..180f, contentColor) {
                        nowAppsCardHeight = it
                        prefs.edit { putFloat("now_apps_card_height", it) }
                    }
                    SliderSettingFloat("Corner Roundness", nowAppsCornerRadius, 0f..64f, contentColor) {
                        nowAppsCornerRadius = it
                        prefs.edit { putFloat("now_apps_corner_radius", it) }
                    }
                }
            }
        }

        item {
            SettingItem(
                icon = Icons.Default.TextFields,
                title = "Display & Font",
                value = displayModeOptions[nowAppsDisplayMode] ?: "Both",
                contentColor = contentColor,
                isExpanded = expandedItem == "content",
                onClick = { expandedItem = if (expandedItem == "content") null else "content" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownSetting("Display Mode", nowAppsDisplayMode, displayModeOptions, contentColor) {
                        nowAppsDisplayMode = it
                        prefs.edit { putString("now_apps_display_mode", it) }
                    }
                    SliderSettingFloat("Icon Size", nowAppsIconSize, 16f..72f, contentColor) {
                        nowAppsIconSize = it
                        prefs.edit { putFloat("now_apps_icon_size", it) }
                    }
                    SliderSettingFloat("Text Size", nowAppsTextSize, 10f..28f, contentColor) {
                        nowAppsTextSize = it
                        prefs.edit { putFloat("now_apps_text_size", it) }
                    }
                    Button(
                        onClick = { onOpenFontPicker("now_apps_font_family", "Pinned Apps Font") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.FontDownload, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose Custom Font", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SettingItem(
                icon = Icons.Default.Palette,
                title = "Colors & Depth",
                value = "Custom Colors",
                contentColor = contentColor,
                isExpanded = expandedItem == "theme",
                onClick = { expandedItem = if (expandedItem == "theme") null else "theme" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorSection("Card Background", colorPalette, nowAppsBgColor) {
                        nowAppsBgColor = it.toArgb()
                        prefs.edit { putInt("now_apps_bg_color", it.toArgb()) }
                    }
                    ColorSection("Card Stroke Color", colorPalette, nowAppsStrokeColor) {
                        nowAppsStrokeColor = it.toArgb()
                        prefs.edit { putInt("now_apps_stroke_color", it.toArgb()) }
                    }
                    SliderSettingFloat("Overall Alpha", nowAppsOpacity, 0.1f..1f, contentColor) {
                        nowAppsOpacity = it
                        prefs.edit { putFloat("now_apps_opacity", it) }
                    }
                    ColorSection("Text Icon Color", colorPalette, nowAppsTextColor) {
                        nowAppsTextColor = it.toArgb()
                        prefs.edit { putInt("now_apps_text_color", it.toArgb()) }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    nowAppsCardWidth = 220f
                    nowAppsCardHeight = 64f
                    nowAppsVerticalOffset = 0f
                    nowAppsVisibleCount = 3
                    nowAppsCornerRadius = 28f
                    
                    prefs.edit {
                        putFloat("now_apps_card_width", 220f)
                        putFloat("now_apps_card_height", 64f)
                        putFloat("now_apps_vertical_offset", 0f)
                        putInt("now_apps_visible_count", 3)
                        putFloat("now_apps_corner_radius", 28f)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor.copy(alpha = 0.4f))
            ) {
                Text("Reset to Defaults", fontSize = 12.sp)
            }
        }
    }
}
