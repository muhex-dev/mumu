package com.muhex.mumu.settings.tabs

import androidx.core.content.edit
import android.content.SharedPreferences
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhex.mumu.settings.*

@Composable
fun DrawerTab(prefs: SharedPreferences, contentColor: Color, onOpenFontPicker: (String, String) -> Unit) {
    var expandedItem by remember { mutableStateOf<String?>(null) }

    var drawerOpacity        by remember { mutableIntStateOf(prefs.getInt("drawer_opacity", 85)) }
    var drawerItemOpacity    by remember { mutableIntStateOf(prefs.getInt("drawer_item_opacity", 100)) }
    var drawerDisplayMode    by remember { mutableStateOf(prefs.getString("drawer_display_mode", "both") ?: "both") }
    var drawerIconSize       by remember { mutableFloatStateOf(prefs.getFloat("drawer_icon_size", 48f)) }
    var drawerLabelSize      by remember { mutableFloatStateOf(prefs.getFloat("drawer_label_size", 12f)) }
    var drawerOpenAnim       by remember { mutableStateOf(prefs.getString("drawer_open_anim", "fade") ?: "fade") }
    var drawerCloseAnim      by remember { mutableStateOf(prefs.getString("drawer_close_anim", "fade") ?: "fade") }
    var drawerColumns        by remember { mutableIntStateOf(prefs.getInt("drawer_columns", 4)) }
    var scrollerPadding      by remember { mutableFloatStateOf(prefs.getFloat("scroller_padding", 0.12f)) }
    var scrollerBending      by remember { mutableFloatStateOf(prefs.getFloat("scroller_bending", 300f)) }
    var scrollerSpread       by remember { mutableFloatStateOf(prefs.getFloat("scroller_spread", 7.5f)) }
    var scrollerTextSize     by remember { mutableFloatStateOf(prefs.getFloat("scroller_text_size", 28f)) }
    var scrollerScale        by remember { mutableFloatStateOf(prefs.getFloat("scroller_scale", 2.4f)) }
    var scrollerLineAlpha    by remember { mutableIntStateOf(prefs.getInt("scroller_line_alpha", 90)) }
    var scrollerBaseAlpha    by remember { mutableIntStateOf(prefs.getInt("scroller_base_alpha", 130)) }
    var scrollerAnimDuration by remember { mutableIntStateOf(prefs.getInt("scroller_anim_duration", 250)) }
    var scrollerTouchSlop    by remember { mutableFloatStateOf(prefs.getFloat("scroller_touch_slop", 80f)) }
    var scrollerHaptic       by remember { mutableStateOf(prefs.getBoolean("scroller_haptic", true)) }

    val animationOptions = mapOf("fade" to "Fade", "slide_up" to "Slide Up", "slide_down" to "Slide Down", "scale" to "Scale", "circle" to "Circle Reveal", "none" to "None")
    val displayModeOptions = mapOf("both" to "Icon & Label", "icon" to "Icon Only", "label" to "Label Only")

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SettingItem(Icons.Default.GridView, "Layout", "Columns and Opacity", contentColor, expandedItem == "layout", onClick = { expandedItem = if (expandedItem == "layout") null else "layout" }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Grid Columns", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4).forEach { col ->
                            val isSelected = drawerColumns == col
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color(0xFF4CAF50) else contentColor.copy(alpha = 0.05f)).clickable { 
                                    drawerColumns = col
                                    prefs.edit { putInt("drawer_columns", col) }
                                }.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$col", color = if (isSelected) Color.White else contentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    SliderSettingFloat("Background Opacity", drawerOpacity.toFloat(), 0f..100f, contentColor) { 
                        drawerOpacity = it.toInt()
                        prefs.edit { putInt("drawer_opacity", it.toInt()) } 
                    }
                    SliderSettingFloat("Item Transparency", drawerItemOpacity.toFloat(), 10f..100f, contentColor) { 
                        drawerItemOpacity = it.toInt()
                        prefs.edit { putInt("drawer_item_opacity", it.toInt()) } 
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.TextFields, "Icon & Label", "Sizes and Display", contentColor, expandedItem == "label", onClick = { expandedItem = if (expandedItem == "label") null else "label" }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownSetting("Display Mode", drawerDisplayMode, displayModeOptions, contentColor) { 
                        drawerDisplayMode = it
                        prefs.edit { putString("drawer_display_mode", it) } 
                    }
                    SliderSettingFloat("Icon Size", drawerIconSize, 24f..72f, contentColor) { 
                        drawerIconSize = it
                        prefs.edit { putFloat("drawer_icon_size", it) } 
                    }
                    SliderSettingFloat("Label Size", drawerLabelSize, 8f..24f, contentColor) { 
                        drawerLabelSize = it
                        prefs.edit { putFloat("drawer_label_size", it) } 
                    }
                    Button(onClick = { onOpenFontPicker("drawer_font_family", "Drawer Font") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                        Text("Select Drawer Font", color = Color(0xFF4CAF50))
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.AutoMode, "Animations", "Open & Close Effects", contentColor, expandedItem == "anim", onClick = { expandedItem = if (expandedItem == "anim") null else "anim" }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownSetting("Open Animation", drawerOpenAnim, animationOptions, contentColor) { 
                        drawerOpenAnim = it
                        prefs.edit { putString("drawer_open_anim", it) } 
                    }
                    DropdownSetting("Close Animation", drawerCloseAnim, animationOptions, contentColor) { 
                        drawerCloseAnim = it
                        prefs.edit { putString("drawer_close_anim", it) } 
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.SortByAlpha, "Scroller", "Muhex Style Scroller", contentColor, expandedItem == "scroller", onClick = { expandedItem = if (expandedItem == "scroller") null else "scroller" }) {
                Column {
                    SliderSettingFloat("Edge Offset", scrollerPadding, 0.05f..0.4f, contentColor) { 
                        scrollerPadding = it
                        prefs.edit { putFloat("scroller_padding", it) } 
                    }
                    SliderSettingFloat("Bending", scrollerBending, 0f..800f, contentColor) { 
                        scrollerBending = it
                        prefs.edit { putFloat("scroller_bending", it) } 
                    }
                    SliderSettingFloat("Curve Spread", scrollerSpread, 1f..25f, contentColor) { 
                        scrollerSpread = it
                        prefs.edit { putFloat("scroller_spread", it) } 
                    }
                    SliderSettingFloat("Font Size", scrollerTextSize, 8f..64f, contentColor) { 
                        scrollerTextSize = it
                        prefs.edit { putFloat("scroller_text_size", it) } 
                    }
                    SliderSettingFloat("Active Scale", scrollerScale, 1.0f..5f, contentColor) { 
                        scrollerScale = it
                        prefs.edit { putFloat("scroller_scale", it) } 
                    }
                    SliderSettingFloat("Line Opacity", scrollerLineAlpha.toFloat(), 0f..255f, contentColor) { 
                        scrollerLineAlpha = it.toInt()
                        prefs.edit { putInt("scroller_line_alpha", it.toInt()) } 
                    }
                    SliderSettingFloat("Idle Opacity", scrollerBaseAlpha.toFloat(), 0f..255f, contentColor) { 
                        scrollerBaseAlpha = it.toInt()
                        prefs.edit { putInt("scroller_base_alpha", it.toInt()) } 
                    }
                    SliderSettingFloat("Anim Speed", scrollerAnimDuration.toFloat(), 50f..1000f, contentColor) { 
                        scrollerAnimDuration = it.toInt()
                        prefs.edit { putInt("scroller_anim_duration", it.toInt()) } 
                    }
                    SliderSettingFloat("Sensitivity", scrollerTouchSlop, 20f..300f, contentColor) { 
                        scrollerTouchSlop = it
                        prefs.edit { putFloat("scroller_touch_slop", it) } 
                    }
                    SettingToggle("Haptic Feedback", scrollerHaptic, contentColor) { 
                        scrollerHaptic = it
                        prefs.edit { putBoolean("scroller_haptic", it) }
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.Palette, "Theming", "Drawer Colors", contentColor, expandedItem == "theme", onClick = { expandedItem = if (expandedItem == "theme") null else "theme" }) {
                Column {
                    // Border color settings removed
                }
            }
        }
    }
}
