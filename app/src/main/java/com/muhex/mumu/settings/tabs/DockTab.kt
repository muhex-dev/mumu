package com.muhex.mumu.settings.tabs

import androidx.core.content.edit
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhex.mumu.AppModel
import com.muhex.mumu.AppRepository
import com.muhex.mumu.settings.*

@Composable
fun DockTab(repository: AppRepository, prefs: SharedPreferences, contentColor: Color, backgroundColor: Color, onOpenFontPicker: (String, String) -> Unit) {
    var expandedItem by remember { mutableStateOf<String?>(null) }
    var dockedIds by remember { mutableStateOf(repository.getDockIds()) }
    var orientation by remember { mutableStateOf(prefs.getString("dock_orientation", "horizontal") ?: "horizontal") }
    var dockAlignment by remember { mutableStateOf(prefs.getString("dock_alignment", "left") ?: "left") }

    var dockBottomMargin    by remember { mutableFloatStateOf(prefs.getFloat("dock_bottom_margin", 8f)) }
    var dockHorizontalMargin by remember { mutableFloatStateOf(prefs.getFloat("dock_horizontal_margin", 0f)) }
    var dockSpacing         by remember { mutableFloatStateOf(prefs.getFloat("dock_spacing", 16f)) }
    var dockDisplayMode     by remember { mutableStateOf(prefs.getString("dock_display_mode", "icon") ?: "icon") }
    var dockIconSize        by remember { mutableFloatStateOf(prefs.getFloat("dock_icon_size", 48f)) }
    var dockTextSize        by remember { mutableFloatStateOf(prefs.getFloat("dock_text_size", 10f)) }
    var dockBgColor         by remember { mutableIntStateOf(prefs.getInt("dock_bg_color", AndroidColor.WHITE)) }
    var dockBgAlpha         by remember { mutableFloatStateOf(prefs.getFloat("dock_bg_alpha", 0.15f)) }
    var dockTextColor       by remember { mutableIntStateOf(prefs.getInt("dock_text_color", AndroidColor.WHITE)) }

    val allApps = remember { mutableStateOf<List<AppModel>>(emptyList()) }
    val displayModeOptions = mapOf("both" to "Icon & Label", "icon" to "Icon Only", "label" to "Label Only")
    val orientationOptions = mapOf("horizontal" to "Horizontal", "vertical" to "Vertical")
    val alignmentOptions = mapOf("left" to "Left Side", "right" to "Right Side")

    LaunchedEffect(Unit) { allApps.value = repository.getAllApps() }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SettingItem(Icons.Default.Apps, "Apps", "${dockedIds.size} apps selected", contentColor, expandedItem == "apps", onClick = { expandedItem = if (expandedItem == "apps") null else "apps" }) {
                Column {
                    Text(
                        "Select up to 8 apps. 5 apps is recommended for the best layout.",
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    AppSelectionContent(allApps.value, dockedIds, repository, contentColor, backgroundColor) { dockedIds = it }
                }
            }
        }
        item {
            SettingItem(Icons.Default.Settings, "Layout", orientationOptions[orientation] ?: "Horizontal", contentColor, expandedItem == "layout", onClick = { expandedItem = if (expandedItem == "layout") null else "layout" }) {
                Column {
                    DropdownSetting("Orientation", orientation, orientationOptions, contentColor) {
                        val newAlign = if (it == "vertical") "row" else "column"
                        orientation = it
                        prefs.edit {
                            putString("dock_orientation", it)
                            putString("dock_content_alignment", newAlign)
                        }
                    }
                    if (orientation == "vertical") {
                        DropdownSetting("Side Alignment", dockAlignment, alignmentOptions, contentColor) {
                            dockAlignment = it
                            prefs.edit { putString("dock_alignment", it) }
                        }
                    }
                    SliderSettingFloat("Bottom Margin", dockBottomMargin, 0f..200f, contentColor) {
                        dockBottomMargin = it
                        prefs.edit { putFloat("dock_bottom_margin", it) }
                    }
                    if (orientation == "vertical") {
                        SliderSettingFloat("Side Margin", dockHorizontalMargin, 0f..200f, contentColor) {
                            dockHorizontalMargin = it
                            prefs.edit { putFloat("dock_horizontal_margin", it) }
                        }
                    }
                    SliderSettingFloat("Item Spacing", dockSpacing, 0f..100f, contentColor) {
                        dockSpacing = it
                        prefs.edit { putFloat("dock_spacing", it) }
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.TextFields, "Content", "Text and Display", contentColor, expandedItem == "content", onClick = { expandedItem = if (expandedItem == "content") null else "content" }) {
                Column {
                    DropdownSetting("Display Mode", dockDisplayMode, displayModeOptions, contentColor) { 
                        dockDisplayMode = it
                        prefs.edit { putString("dock_display_mode", it) } 
                    }
                    SliderSettingFloat("Icon Size", dockIconSize, 24f..96f, contentColor) { 
                        dockIconSize = it
                        prefs.edit { putFloat("dock_icon_size", it) } 
                    }
                    SliderSettingFloat("Text Size", dockTextSize, 8f..20f, contentColor) { 
                        dockTextSize = it
                        prefs.edit { putFloat("dock_text_size", it) } 
                    }
                    Button(
                        onClick = { onOpenFontPicker("dock_font_family", "Dock Font") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FontDownload, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Dock Font", color = Color(0xFF4CAF50), fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.Palette, "Theming", "Colors and Alpha", contentColor, expandedItem == "theme", onClick = { expandedItem = if (expandedItem == "theme") null else "theme" }) {
                Column {
                    ColorSection("Background Color", colorPalette, dockBgColor) { 
                        dockBgColor = it.toArgb()
                        prefs.edit { putInt("dock_bg_color", it.toArgb()) } 
                    }
                    SliderSettingFloat("Background Alpha", dockBgAlpha, 0f..1f, contentColor) { 
                        dockBgAlpha = it
                        prefs.edit { putFloat("dock_bg_alpha", it) } 
                    }
                    ColorSection("Text Color", colorPalette, dockTextColor) { 
                        dockTextColor = it.toArgb()
                        prefs.edit { putInt("dock_text_color", it.toArgb()) }
                    }
                }
            }
        }
    }
}
