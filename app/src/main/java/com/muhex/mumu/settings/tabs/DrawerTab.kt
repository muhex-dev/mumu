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
    val accentColor = Color(0xFF4CAF50)

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

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 24.dp), 
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("Drawer Configuration")
        }

        item {
            SettingItem(Icons.Default.GridView, "Grid Layout", "${drawerColumns} Columns, ${drawerOpacity}% Opacity", contentColor, expandedItem == "layout", onClick = { expandedItem = if (expandedItem == "layout") null else "layout" }) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Grid Columns", color = contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 4, 5).forEach { col ->
                                val isSelected = drawerColumns == col
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (isSelected) accentColor else contentColor.copy(alpha = 0.05f)).clickable { 
                                        drawerColumns = col
                                        prefs.edit { putInt("drawer_columns", col) }
                                    }.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$col", color = if (isSelected) Color.White else contentColor, fontWeight = FontWeight.Bold)
                                }
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
            SettingItem(Icons.Default.TextFields, "Typography & Icons", "Sizes and Font Selection", contentColor, expandedItem == "label", onClick = { expandedItem = if (expandedItem == "label") null else "label" }) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    Button(
                        onClick = { onOpenFontPicker("drawer_font_family", "Drawer Font") }, 
                        modifier = Modifier.fillMaxWidth().height(48.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.1f)), 
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Select Custom Font", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.AutoMode, "Motion Effects", "System Animations", contentColor, expandedItem == "anim", onClick = { expandedItem = if (expandedItem == "anim") null else "anim" }) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DropdownSetting("Enter Transition", drawerOpenAnim, animationOptions, contentColor) { 
                        drawerOpenAnim = it
                        prefs.edit { putString("drawer_open_anim", it) } 
                    }
                    DropdownSetting("Exit Transition", drawerCloseAnim, animationOptions, contentColor) { 
                        drawerCloseAnim = it
                        prefs.edit { putString("drawer_close_anim", it) } 
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.SortByAlpha, "Advanced Scroller", "Experimental Curved Scroller", contentColor, expandedItem == "scroller", onClick = { expandedItem = if (expandedItem == "scroller") null else "scroller" }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SliderSettingFloat("Edge Offset", scrollerPadding, 0.05f..0.4f, contentColor) { 
                        scrollerPadding = it
                        prefs.edit { putFloat("scroller_padding", it) } 
                    }
                    SliderSettingFloat("Curve Intensity", scrollerBending, 0f..800f, contentColor) { 
                        scrollerBending = it
                        prefs.edit { putFloat("scroller_bending", it) } 
                    }
                    SliderSettingFloat("Spread", scrollerSpread, 1f..25f, contentColor) { 
                        scrollerSpread = it
                        prefs.edit { putFloat("scroller_spread", it) } 
                    }
                    SliderSettingFloat("Text Size", scrollerTextSize, 8f..64f, contentColor) { 
                        scrollerTextSize = it
                        prefs.edit { putFloat("scroller_text_size", it) } 
                    }
                    SliderSettingFloat("Active Magnification", scrollerScale, 1.0f..5f, contentColor) { 
                        scrollerScale = it
                        prefs.edit { putFloat("scroller_scale", it) } 
                    }
                    SliderSettingFloat("Animation Duration (ms)", scrollerAnimDuration.toFloat(), 50f..1000f, contentColor) { 
                        scrollerAnimDuration = it.toInt()
                        prefs.edit { putInt("scroller_anim_duration", it.toInt()) } 
                    }
                    SettingToggle(
                        title = "Haptic Response",
                        checked = scrollerHaptic,
                        contentColor = contentColor
                    ) { 
                        scrollerHaptic = it
                        prefs.edit { putBoolean("scroller_haptic", it) }
                    }
                }
            }
        }
    }
}
