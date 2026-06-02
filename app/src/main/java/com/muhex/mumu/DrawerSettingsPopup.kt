package com.muhex.mumu

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/**
 * DrawerSettingsPopup: A central overlay for managing drawer-specific UI settings.
 * Handles grid layouts, typography, icons, motion transitions, and the advanced scroller.
 */
@Composable
fun DrawerSettingsPopup(
    isVisible: Boolean,
    prefs: SharedPreferences,
    onOpenFontPicker: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    val screenHeightPx = with(density) { screenHeight.toPx() }

    var verticalOffset by remember { mutableFloatStateOf(0f) }

    // --- Drawer State ---
    // (These are synced with SharedPreferences, so remember is fine as they're re-read on recomposition/re-init)
    var drawerOpacity by remember { mutableIntStateOf(prefs.getInt("drawer_opacity", 85)) }
    var drawerItemOpacity by remember { mutableIntStateOf(prefs.getInt("drawer_item_opacity", 100)) }
    var drawerDisplayMode by remember { mutableStateOf(prefs.getString("drawer_display_mode", "both") ?: "both") }
    var drawerIconSize by remember { mutableFloatStateOf(prefs.getFloat("drawer_icon_size", 48f)) }
    var drawerLabelSize by remember { mutableFloatStateOf(prefs.getFloat("drawer_label_size", 12f)) }
    var drawerOpenAnim by remember { mutableStateOf(prefs.getString("drawer_open_anim", "fade") ?: "fade") }
    var drawerCloseAnim by remember { mutableStateOf(prefs.getString("drawer_close_anim", "fade") ?: "fade") }
    var drawerColumns by remember { mutableIntStateOf(prefs.getInt("drawer_columns", 4)) }

    // --- Scroller State ---
    var isScrollerExpanded by rememberSaveable { mutableStateOf(false) }
    var scrollerPadding by remember { mutableFloatStateOf(prefs.getFloat("scroller_padding", 0.12f)) }
    var scrollerBending by remember { mutableFloatStateOf(prefs.getFloat("scroller_bending", 300f)) }
    var scrollerSpread by remember { mutableFloatStateOf(prefs.getFloat("scroller_spread", 7.5f)) }
    var scrollerTextSize by remember { mutableFloatStateOf(prefs.getFloat("scroller_text_size", 28f)) }
    var scrollerScale by remember { mutableFloatStateOf(prefs.getFloat("scroller_scale", 2.4f)) }
    var scrollerAnimDuration by remember { mutableIntStateOf(prefs.getInt("scroller_anim_duration", 250)) }
    var scrollerHaptic by remember { mutableStateOf(prefs.getBoolean("scroller_haptic", true)) }

    // --- UI Theme Helpers ---
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
                .background(Color.Black.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(0.8f)
                    .heightIn(max = screenHeight * 0.6f)
                    .wrapContentHeight()
                    .offset { IntOffset(0, verticalOffset.roundToInt()) }
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
                color = backgroundColor,
                contentColor = contentColor,
                tonalElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Drag Handle & Header
                    HeaderSection(
                        contentColor = contentColor,
                        onReset = { verticalOffset = 0f },
                        onDrag = { dragAmount ->
                            verticalOffset = (verticalOffset + dragAmount).coerceIn(-screenHeightPx * 0.4f, screenHeightPx * 0.4f)
                        }
                    )

                    // 2. Main Scrollable Content
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            Text(
                                text = "Customize Launcher",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = contentColor
                            )
                        }

                        // Grid Settings
                        item {
                            GridLayoutSection(
                                columns = drawerColumns,
                                opacity = drawerOpacity,
                                itemOpacity = drawerItemOpacity,
                                accentColor = accentColor,
                                contentColor = contentColor,
                                subTextColor = subTextColor,
                                onColumnsChange = {
                                    drawerColumns = it
                                    prefs.edit { putInt("drawer_columns", it) }
                                },
                                onOpacityChange = {
                                    drawerOpacity = it
                                    prefs.edit { putInt("drawer_opacity", it) }
                                },
                                onItemOpacityChange = {
                                    drawerItemOpacity = it
                                    prefs.edit { putInt("drawer_item_opacity", it) }
                                }
                            )
                        }

                        // Typography & Icons
                        item {
                            TypographySection(
                                displayMode = drawerDisplayMode,
                                iconSize = drawerIconSize,
                                labelSize = drawerLabelSize,
                                accentColor = accentColor,
                                contentColor = contentColor,
                                subTextColor = subTextColor,
                                onDisplayModeChange = {
                                    drawerDisplayMode = it
                                    prefs.edit { putString("drawer_display_mode", it) }
                                },
                                onIconSizeChange = {
                                    drawerIconSize = it
                                    prefs.edit { putFloat("drawer_icon_size", it) }
                                },
                                onLabelSizeChange = {
                                    drawerLabelSize = it
                                    prefs.edit { putFloat("drawer_label_size", it) }
                                },
                                onOpenFontPicker = { onOpenFontPicker("drawer_font_family", "Drawer Font") }
                            )
                        }

                        // Transitions
                        item {
                            TransitionsSection(
                                openAnim = drawerOpenAnim,
                                closeAnim = drawerCloseAnim,
                                accentColor = accentColor,
                                subTextColor = subTextColor,
                                onOpenAnimChange = {
                                    drawerOpenAnim = it
                                    prefs.edit { putString("drawer_open_anim", it) }
                                },
                                onCloseAnimChange = {
                                    drawerCloseAnim = it
                                    prefs.edit { putString("drawer_close_anim", it) }
                                }
                            )
                        }

                        // Advanced Scroller
                        item {
                            ScrollerSection(
                                isExpanded = isScrollerExpanded,
                                padding = scrollerPadding,
                                bending = scrollerBending,
                                spread = scrollerSpread,
                                textSize = scrollerTextSize,
                                scale = scrollerScale,
                                duration = scrollerAnimDuration,
                                haptic = scrollerHaptic,
                                accentColor = accentColor,
                                contentColor = contentColor,
                                subTextColor = subTextColor,
                                onToggleExpand = { isScrollerExpanded = !isScrollerExpanded },
                                onPaddingChange = {
                                    scrollerPadding = it
                                    prefs.edit { putFloat("scroller_padding", it) }
                                },
                                onBendingChange = {
                                    scrollerBending = it
                                    prefs.edit { putFloat("scroller_bending", it) }
                                },
                                onSpreadChange = {
                                    scrollerSpread = it
                                    prefs.edit { putFloat("scroller_spread", it) }
                                },
                                onTextSizeChange = {
                                    scrollerTextSize = it
                                    prefs.edit { putFloat("scroller_text_size", it) }
                                },
                                onScaleChange = {
                                    scrollerScale = it
                                    prefs.edit { putFloat("scroller_scale", it) }
                                },
                                onDurationChange = {
                                    scrollerAnimDuration = it
                                    prefs.edit { putInt("scroller_anim_duration", it) }
                                },
                                onHapticChange = {
                                    scrollerHaptic = it
                                    prefs.edit { putBoolean("scroller_haptic", it) }
                                }
                            )
                        }

                        // Bottom Spacer
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// --- Section Composables ---

@Composable
private fun HeaderSection(contentColor: Color, onReset: () -> Unit, onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount -> onDrag(dragAmount) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onReset() }
                )
            }
            .padding(top = 16.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun GridLayoutSection(
    columns: Int,
    opacity: Int,
    itemOpacity: Int,
    accentColor: Color,
    contentColor: Color,
    subTextColor: Color,
    onColumnsChange: (Int) -> Unit,
    onOpacityChange: (Int) -> Unit,
    onItemOpacityChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PopupSectionHeader(Icons.Default.GridView, "Grid Layout", accentColor)
        
        ColumnSelector(
            currentColumns = columns,
            options = listOf(1, 2, 3, 4, 5),
            accentColor = accentColor,
            contentColor = contentColor,
            onSelected = onColumnsChange
        )

        PopupSlider(
            label = "Background Opacity",
            value = opacity.toFloat(),
            valueRange = 0f..100f,
            valueSuffix = "%",
            subTextColor = subTextColor,
            accentColor = accentColor,
            onValueChange = { onOpacityChange(it.toInt()) }
        )

        PopupSlider(
            label = "Item Transparency",
            value = itemOpacity.toFloat(),
            valueRange = 10f..100f,
            valueSuffix = "%",
            subTextColor = subTextColor,
            accentColor = accentColor,
            onValueChange = { onItemOpacityChange(it.toInt()) }
        )
    }
}

@Composable
private fun TypographySection(
    displayMode: String,
    iconSize: Float,
    labelSize: Float,
    accentColor: Color,
    contentColor: Color,
    subTextColor: Color,
    onDisplayModeChange: (String) -> Unit,
    onIconSizeChange: (Float) -> Unit,
    onLabelSizeChange: (Float) -> Unit,
    onOpenFontPicker: () -> Unit
) {
    val displayModeOptions = mapOf(
        "both" to "Icon & Label",
        "icon" to "Icon Only",
        "label" to "Label Only"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PopupSectionHeader(Icons.Default.TextFields, "Typography & Icons", accentColor)

        PopupDropdown(
            label = "Display Mode",
            currentSelection = displayMode,
            options = displayModeOptions,
            subTextColor = subTextColor,
            onOptionSelected = onDisplayModeChange
        )

        PopupSlider(
            label = "Icon Size",
            value = iconSize,
            valueRange = 24f..72f,
            valueSuffix = " dp",
            subTextColor = subTextColor,
            accentColor = accentColor,
            onValueChange = onIconSizeChange
        )

        PopupSlider(
            label = "Label Size",
            value = labelSize,
            valueRange = 8f..24f,
            valueSuffix = " sp",
            subTextColor = subTextColor,
            accentColor = accentColor,
            onValueChange = onLabelSizeChange
        )

        Button(
            onClick = onOpenFontPicker,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.FontDownload, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Change Font Style", color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransitionsSection(
    openAnim: String,
    closeAnim: String,
    accentColor: Color,
    subTextColor: Color,
    onOpenAnimChange: (String) -> Unit,
    onCloseAnimChange: (String) -> Unit
) {
    val animationOptions = mapOf(
        "fade" to "Fade",
        "slide_up" to "Slide Up",
        "slide_down" to "Slide Down",
        "scale" to "Scale",
        "circle" to "Circle Reveal",
        "none" to "None"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PopupSectionHeader(Icons.Default.AutoMode, "Motion Effects", accentColor)

        PopupDropdown(
            label = "Open Transition",
            currentSelection = openAnim,
            options = animationOptions,
            subTextColor = subTextColor,
            onOptionSelected = onOpenAnimChange
        )

        PopupDropdown(
            label = "Close Transition",
            currentSelection = closeAnim,
            options = animationOptions,
            subTextColor = subTextColor,
            onOptionSelected = onCloseAnimChange
        )
    }
}

@Composable
private fun ScrollerSection(
    isExpanded: Boolean,
    padding: Float,
    bending: Float,
    spread: Float,
    textSize: Float,
    scale: Float,
    duration: Int,
    haptic: Boolean,
    accentColor: Color,
    contentColor: Color,
    subTextColor: Color,
    onToggleExpand: () -> Unit,
    onPaddingChange: (Float) -> Unit,
    onBendingChange: (Float) -> Unit,
    onSpreadChange: (Float) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onDurationChange: (Int) -> Unit,
    onHapticChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PopupSectionHeader(Icons.Default.SortByAlpha, "Advanced Scroller", accentColor)
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = subTextColor
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PopupSlider(
                    label = "Edge Offset",
                    value = padding,
                    valueRange = 0.05f..0.4f,
                    isDecimal = true,
                    subTextColor = subTextColor,
                    accentColor = accentColor,
                    onValueChange = onPaddingChange
                )

                PopupSlider(
                    label = "Curve Intensity",
                    value = bending,
                    valueRange = 0f..800f,
                    subTextColor = subTextColor,
                    accentColor = accentColor,
                    onValueChange = onBendingChange
                )

                PopupSlider(
                    label = "Spread Scale",
                    value = spread,
                    valueRange = 1f..25f,
                    isDecimal = true,
                    subTextColor = subTextColor,
                    accentColor = accentColor,
                    onValueChange = onSpreadChange
                )

                PopupSlider(
                    label = "Scroller Text Size",
                    value = textSize,
                    valueRange = 8f..64f,
                    valueSuffix = " sp",
                    subTextColor = subTextColor,
                    accentColor = accentColor,
                    onValueChange = onTextSizeChange
                )

                PopupSlider(
                    label = "Active Magnification",
                    value = scale,
                    valueRange = 1.0f..5.0f,
                    isDecimal = true,
                    valueSuffix = "x",
                    subTextColor = subTextColor,
                    accentColor = accentColor,
                    onValueChange = onScaleChange
                )

                PopupSlider(
                    label = "Animation Duration",
                    value = duration.toFloat(),
                    valueRange = 50f..1000f,
                    valueSuffix = " ms",
                    subTextColor = subTextColor,
                    accentColor = accentColor,
                    onValueChange = { onDurationChange(it.toInt()) }
                )

                PopupToggle(
                    title = "Haptic Feedback",
                    checked = haptic,
                    contentColor = contentColor,
                    onCheckedChange = onHapticChange
                )
            }
        }
    }
}

// --- Micro UI Controller Components ---

@Composable
private fun PopupSectionHeader(icon: ImageVector, title: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = accentColor
        )
    }
}

@Composable
private fun ColumnSelector(
    currentColumns: Int,
    options: List<Int>,
    accentColor: Color,
    contentColor: Color,
    onSelected: (Int) -> Unit
) {
    Column {
        Text("Grid Columns", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.5f))
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { col ->
                val isSelected = currentColumns == col
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) accentColor else contentColor.copy(alpha = 0.05f),
                    onClick = { onSelected(col) }
                ) {
                    Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "$col",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueSuffix: String = "",
    isDecimal: Boolean = false,
    subTextColor: Color,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                activeTrackColor = accentColor,
                inactiveTrackColor = accentColor.copy(alpha = 0.1f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopupDropdown(
    label: String,
    currentSelection: String,
    options: Map<String, String>,
    subTextColor: Color,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = subTextColor)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = options[currentSelection] ?: currentSelection,
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = subTextColor.copy(alpha = 0.2f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(16.dp)
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
private fun PopupToggle(title: String, checked: Boolean, contentColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
