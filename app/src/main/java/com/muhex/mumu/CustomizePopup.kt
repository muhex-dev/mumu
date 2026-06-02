package com.muhex.mumu

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun CustomizePopup(
    isVisible: Boolean,
    prefs: SharedPreferences,
    onOpenFontPicker: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // Gestures and physics
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, label = "PopupOffset")

    // --- State Initialization from SharedPreferences ---
    var drawerOpacity by remember { mutableIntStateOf(prefs.getInt("drawer_opacity", 85)) }
    var drawerItemOpacity by remember { mutableIntStateOf(prefs.getInt("drawer_item_opacity", 100)) }
    var drawerDisplayMode by remember { mutableStateOf(prefs.getString("drawer_display_mode", "both") ?: "both") }
    var drawerIconSize by remember { mutableFloatStateOf(prefs.getFloat("drawer_icon_size", 48f)) }
    var drawerLabelSize by remember { mutableFloatStateOf(prefs.getFloat("drawer_label_size", 12f)) }
    var drawerOpenAnim by remember { mutableStateOf(prefs.getString("drawer_open_anim", "fade") ?: "fade") }
    var drawerCloseAnim by remember { mutableStateOf(prefs.getString("drawer_close_anim", "fade") ?: "fade") }
    var drawerColumns by remember { mutableIntStateOf(prefs.getInt("drawer_columns", 4)) }
    var isScrollerExpanded by remember { mutableStateOf(false) }

    // Scroller settings
    var scrollerPadding by remember { mutableFloatStateOf(prefs.getFloat("scroller_padding", 0.12f)) }
    var scrollerBending by remember { mutableFloatStateOf(prefs.getFloat("scroller_bending", 300f)) }
    var scrollerSpread by remember { mutableFloatStateOf(prefs.getFloat("scroller_spread", 7.5f)) }
    var scrollerTextSize by remember { mutableFloatStateOf(prefs.getFloat("scroller_text_size", 28f)) }
    var scrollerScale by remember { mutableFloatStateOf(prefs.getFloat("scroller_scale", 2.4f)) }
    var scrollerAnimDuration by remember { mutableIntStateOf(prefs.getInt("scroller_anim_duration", 250)) }
    var scrollerHaptic by remember { mutableStateOf(prefs.getBoolean("scroller_haptic", true)) }

    // Constant Mappings
    val animationOptions = mapOf("fade" to "Fade", "slide_up" to "Slide Up", "slide_down" to "Slide Down", "scale" to "Scale", "circle" to "Circle Reveal", "none" to "None")
    val displayModeOptions = mapOf("both" to "Icon & Label", "icon" to "Icon Only", "label" to "Label Only")

    // Theme detection
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF0F0F0F) else Color(0xFFF8F9FA)
    val contentColor = if (isDark) Color.White else Color.Black
    val accentColor = MaterialTheme.colorScheme.primary
    val subTextColor = contentColor.copy(alpha = 0.6f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                    .widthIn(max = 600.dp) // Responsive max width for tablets/large screens
                    .fillMaxWidth(0.9f) // Comfortable width on most phones
                    .heightIn(max = screenHeight * 0.8f) // Dynamic height limit
                    .wrapContentHeight() // Adapt to content size
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                color = backgroundColor,
                contentColor = contentColor,
                tonalElevation = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Interactive Drag Handle Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = { offsetY = 0f },
                                    onDragCancel = { offsetY = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetY += dragAmount
                                    }
                                )
                            }
                            .padding(top = 14.dp, bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(contentColor.copy(alpha = 0.2f))
                        )
                    }

                    // Title Header
                    Text(
                        text = "Customize Launcher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )

                    // Scrollable Config Area
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        // SECTION: Grid Layout
                        item {
                            PopupSectionHeader(Icons.Default.GridView, "Grid Layout", accentColor)
                            Spacer(Modifier.height(10.dp))

                            ColumnSelector(
                                currentColumns = drawerColumns,
                                options = listOf(1, 2, 3, 4, 5),
                                accentColor = accentColor,
                                contentColor = contentColor
                            ) { selectedCol ->
                                drawerColumns = selectedCol
                                prefs.edit { putInt("drawer_columns", selectedCol) }
                            }

                            Spacer(Modifier.height(12.dp))

                            PopupSlider(
                                label = "Background Opacity",
                                value = drawerOpacity.toFloat(),
                                valueRange = 0f..100f,
                                valueSuffix = "%",
                                subTextColor = subTextColor,
                                accentColor = accentColor,
                                onValueChange = {
                                    drawerOpacity = it.toInt()
                                    prefs.edit { putInt("drawer_opacity", it.toInt()) }
                                }
                            )

                            PopupSlider(
                                label = "Item Transparency",
                                value = drawerItemOpacity.toFloat(),
                                valueRange = 10f..100f,
                                valueSuffix = "%",
                                subTextColor = subTextColor,
                                accentColor = accentColor,
                                onValueChange = {
                                    drawerItemOpacity = it.toInt()
                                    prefs.edit { putInt("drawer_item_opacity", it.toInt()) }
                                }
                            )
                        }

                        // SECTION: Typography & Icons
                        item {
                            PopupSectionHeader(Icons.Default.TextFields, "Typography & Icons", accentColor)
                            Spacer(Modifier.height(10.dp))

                            PopupDropdown(
                                label = "Display Mode",
                                currentSelection = drawerDisplayMode,
                                options = displayModeOptions,
                                subTextColor = subTextColor,
                                onOptionSelected = {
                                    drawerDisplayMode = it
                                    prefs.edit { putString("drawer_display_mode", it) }
                                }
                            )

                            PopupSlider(
                                label = "Icon Size",
                                value = drawerIconSize,
                                valueRange = 24f..72f,
                                valueSuffix = " dp",
                                subTextColor = subTextColor,
                                accentColor = accentColor,
                                onValueChange = {
                                    drawerIconSize = it
                                    prefs.edit { putFloat("drawer_icon_size", it) }
                                }
                            )

                            PopupSlider(
                                label = "Label Size",
                                value = drawerLabelSize,
                                valueRange = 8f..24f,
                                valueSuffix = " sp",
                                subTextColor = subTextColor,
                                accentColor = accentColor,
                                onValueChange = {
                                    drawerLabelSize = it
                                    prefs.edit { putFloat("drawer_label_size", it) }
                                }
                            )

                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = { onOpenFontPicker("drawer_font_family", "Drawer Font") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FontDownload, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Select Custom Font", color = accentColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        // SECTION: Motion Effects
                        item {
                            PopupSectionHeader(Icons.Default.AutoMode, "Motion Effects", accentColor)
                            Spacer(Modifier.height(10.dp))

                            PopupDropdown(
                                label = "Enter Transition",
                                currentSelection = drawerOpenAnim,
                                options = animationOptions,
                                subTextColor = subTextColor,
                                onOptionSelected = {
                                    drawerOpenAnim = it
                                    prefs.edit { putString("drawer_open_anim", it) }
                                }
                            )

                            PopupDropdown(
                                label = "Exit Transition",
                                currentSelection = drawerCloseAnim,
                                options = animationOptions,
                                subTextColor = subTextColor,
                                onOptionSelected = {
                                    drawerCloseAnim = it
                                    prefs.edit { putString("drawer_close_anim", it) }
                                }
                            )
                        }

                        // SECTION: Advanced Scroller
                        item {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isScrollerExpanded = !isScrollerExpanded }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PopupSectionHeader(Icons.Default.SortByAlpha, "Advanced Curved Scroller", accentColor)
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        imageVector = if (isScrollerExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = contentColor.copy(alpha = 0.4f)
                                    )
                                }

                                AnimatedVisibility(visible = isScrollerExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        PopupSlider(
                                            label = "Edge Offset",
                                            value = scrollerPadding,
                                            valueRange = 0.05f..0.4f,
                                            isDecimal = true,
                                            subTextColor = subTextColor,
                                            accentColor = accentColor,
                                            onValueChange = {
                                                scrollerPadding = it
                                                prefs.edit { putFloat("scroller_padding", it) }
                                            }
                                        )

                                        PopupSlider(
                                            label = "Curve Intensity",
                                            value = scrollerBending,
                                            valueRange = 0f..800f,
                                            subTextColor = subTextColor,
                                            accentColor = accentColor,
                                            onValueChange = {
                                                scrollerBending = it
                                                prefs.edit { putFloat("scroller_bending", it) }
                                            }
                                        )

                                        PopupSlider(
                                            label = "Spread Scale",
                                            value = scrollerSpread,
                                            valueRange = 1f..25f,
                                            isDecimal = true,
                                            subTextColor = subTextColor,
                                            accentColor = accentColor,
                                            onValueChange = {
                                                scrollerSpread = it
                                                prefs.edit { putFloat("scroller_spread", it) }
                                            }
                                        )

                                        PopupSlider(
                                            label = "Scroller Text Size",
                                            value = scrollerTextSize,
                                            valueRange = 8f..64f,
                                            valueSuffix = " sp",
                                            subTextColor = subTextColor,
                                            accentColor = accentColor,
                                            onValueChange = {
                                                scrollerTextSize = it
                                                prefs.edit { putFloat("scroller_text_size", it) }
                                            }
                                        )

                                        PopupSlider(
                                            label = "Active Magnification",
                                            value = scrollerScale,
                                            valueRange = 1.0f..5.0f,
                                            isDecimal = true,
                                            valueSuffix = "x",
                                            subTextColor = subTextColor,
                                            accentColor = accentColor,
                                            onValueChange = {
                                                scrollerScale = it
                                                prefs.edit { putFloat("scroller_scale", it) }
                                            }
                                        )

                                        PopupSlider(
                                            label = "Animation Duration",
                                            value = scrollerAnimDuration.toFloat(),
                                            valueRange = 50f..1000f,
                                            valueSuffix = " ms",
                                            subTextColor = subTextColor,
                                            accentColor = accentColor,
                                            onValueChange = {
                                                scrollerAnimDuration = it.toInt()
                                                prefs.edit { putInt("scroller_anim_duration", it.toInt()) }
                                            }
                                        )

                                        PopupToggle(
                                            title = "Haptic Response Feedback",
                                            checked = scrollerHaptic,
                                            contentColor = contentColor,
                                            onCheckedChange = {
                                                scrollerHaptic = it
                                                prefs.edit { putBoolean("scroller_haptic", it) }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}

// --- Micro UI Controller Components Built for Dialog Layouts ---

@Composable
fun PopupSectionHeader(icon: ImageVector, title: String, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentColor)
    }
}

@Composable
fun ColumnSelector(currentColumns: Int, options: List<Int>, accentColor: Color, contentColor: Color, onSelected: (Int) -> Unit) {
    Column {
        Text("Grid Columns", style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.6f))
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { col ->
                val isSelected = currentColumns == col
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accentColor else contentColor.copy(alpha = 0.05f))
                        .clickable { onSelected(col) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$col",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun PopupSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueSuffix: String = "",
    isDecimal: Boolean = false,
    subTextColor: Color,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = subTextColor)
            Text(
                text = if (isDecimal) String.format("%.2f", value) + valueSuffix else "${value.roundToInt()}$valueSuffix",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupDropdown(
    label: String,
    currentSelection: String,
    options: Map<String, String>,
    subTextColor: Color,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = subTextColor)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = options[currentSelection] ?: currentSelection,
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (key, labelText) ->
                    DropdownMenuItem(
                        text = { Text(labelText, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onOptionSelected(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PopupToggle(title: String, checked: Boolean, contentColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = contentColor)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}