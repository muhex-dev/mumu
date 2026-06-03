package com.muhex.mumu

import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import com.muhex.mumu.settings.AppSelectionContent
import com.muhex.mumu.settings.colorPalette
import kotlin.math.roundToInt

import java.util.Locale

/**
 * DockSettingsPopup: A central overlay for managing Dock-specific settings.
 */
@Composable
fun DockSettingsPopup(
    isVisible: Boolean,
    repository: AppRepository,
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

    // --- Dock State ---
    var dockedIds by remember { mutableStateOf(repository.getDockIds()) }
    var orientation by remember { mutableStateOf(prefs.getString("dock_orientation", "horizontal") ?: "horizontal") }
    var dockAlignment by remember { mutableStateOf(prefs.getString("dock_alignment", "left") ?: "left") }

    var dockBottomMargin by remember { mutableFloatStateOf(prefs.getFloat("dock_bottom_margin", 8f)) }
    var dockHorizontalMargin by remember { mutableFloatStateOf(prefs.getFloat("dock_horizontal_margin", 0f)) }
    var dockSpacing by remember { mutableFloatStateOf(prefs.getFloat("dock_spacing", 16f)) }
    var dockDisplayMode by remember { mutableStateOf(prefs.getString("dock_display_mode", "icon") ?: "icon") }
    var dockIconSize by remember { mutableFloatStateOf(prefs.getFloat("dock_icon_size", 48f)) }
    var dockTextSize by remember { mutableFloatStateOf(prefs.getFloat("dock_text_size", 10f)) }
    var dockBgColor by remember { mutableIntStateOf(prefs.getInt("dock_bg_color", AndroidColor.WHITE)) }
    var dockBgAlpha by remember { mutableFloatStateOf(prefs.getFloat("dock_bg_alpha", 0.15f)) }
    var dockTextColor by remember { mutableIntStateOf(prefs.getInt("dock_text_color", AndroidColor.WHITE)) }
    var showDockBar by remember { mutableStateOf(prefs.getBoolean("show_dock_bar", true)) }

    var isAppSelectionExpanded by rememberSaveable { mutableStateOf(false) }
    var isThemingExpanded by rememberSaveable { mutableStateOf(false) }

    val allApps = remember { mutableStateOf<List<AppModel>>(emptyList()) }
    LaunchedEffect(Unit) { allApps.value = repository.getAllApps() }

    // --- UI Theme Helpers ---
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color.Black else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val accentColor = contentColor
    val subTextColor = Color(0xFF888888)

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
                                text = "Dock Configuration",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = contentColor
                            )
                        }

                        // App Selection Section
                        item {
                            AppSelectionSection(
                                isExpanded = isAppSelectionExpanded,
                                allApps = allApps.value,
                                selectedIds = dockedIds,
                                repository = repository,
                                accentColor = accentColor,
                                contentColor = contentColor,
                                backgroundColor = backgroundColor,
                                subTextColor = subTextColor,
                                onToggleExpand = { isAppSelectionExpanded = !isAppSelectionExpanded },
                                onUpdate = { dockedIds = it }
                            )
                        }

                        // Layout Section
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PopupSectionHeader(Icons.Default.Settings, "Layout", accentColor)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(accentColor.copy(alpha = 0.05f))
                                        .clickable {
                                            showDockBar = !showDockBar
                                            prefs.edit { putBoolean("show_dock_bar", showDockBar) }
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Visibility, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Show Dock", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = showDockBar,
                                        onCheckedChange = {
                                            showDockBar = it
                                            prefs.edit { putBoolean("show_dock_bar", it) }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = backgroundColor,
                                            checkedTrackColor = accentColor,
                                            uncheckedThumbColor = accentColor.copy(alpha = 0.4f),
                                            uncheckedTrackColor = accentColor.copy(alpha = 0.1f),
                                            uncheckedBorderColor = Color.Transparent
                                        )
                                    )
                                }
                                
                                PopupSegmentedControl(
                                    label = "Orientation",
                                    currentSelection = orientation,
                                    options = mapOf("horizontal" to "Horizontal", "vertical" to "Vertical"),
                                    subTextColor = subTextColor,
                                    accentColor = accentColor,
                                    onOptionSelected = {
                                        orientation = it
                                        val newAlign = if (it == "vertical") "row" else "column"
                                        prefs.edit {
                                            putString("dock_orientation", it)
                                            putString("dock_content_alignment", newAlign)
                                        }
                                    }
                                )

                                if (orientation == "vertical") {
                                    PopupSegmentedControl(
                                        label = "Side Alignment",
                                        currentSelection = dockAlignment,
                                        options = mapOf("left" to "Left", "right" to "Right"),
                                        subTextColor = subTextColor,
                                        accentColor = accentColor,
                                        onOptionSelected = {
                                            dockAlignment = it
                                            prefs.edit { putString("dock_alignment", it) }
                                        }
                                    )
                                }

                                PopupSlider(
                                    label = "Bottom Margin",
                                    value = dockBottomMargin,
                                    valueRange = 0f..200f,
                                    valueSuffix = " dp",
                                    subTextColor = subTextColor,
                                    accentColor = accentColor,
                                    onValueChange = {
                                        dockBottomMargin = it
                                        prefs.edit { putFloat("dock_bottom_margin", it) }
                                    }
                                )

                                if (orientation == "vertical") {
                                    PopupSlider(
                                        label = "Side Margin",
                                        value = dockHorizontalMargin,
                                        valueRange = 0f..200f,
                                        valueSuffix = " dp",
                                        subTextColor = subTextColor,
                                        accentColor = accentColor,
                                        onValueChange = {
                                            dockHorizontalMargin = it
                                            prefs.edit { putFloat("dock_horizontal_margin", it) }
                                        }
                                    )
                                }

                                PopupSlider(
                                    label = "Item Spacing",
                                    value = dockSpacing,
                                    valueRange = 0f..100f,
                                    valueSuffix = " dp",
                                    subTextColor = subTextColor,
                                    accentColor = accentColor,
                                    onValueChange = {
                                        dockSpacing = it
                                        prefs.edit { putFloat("dock_spacing", it) }
                                    }
                                )
                            }
                        }

                        // Content Section
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PopupSectionHeader(Icons.Default.TextFields, "Content & Font", accentColor)

                                PopupSegmentedControl(
                                    label = "Display Mode",
                                    currentSelection = dockDisplayMode,
                                    options = mapOf("both" to "Both", "icon" to "Icon", "label" to "Label"),
                                    subTextColor = subTextColor,
                                    accentColor = accentColor,
                                    onOptionSelected = {
                                        dockDisplayMode = it
                                        prefs.edit { putString("dock_display_mode", it) }
                                    }
                                )

                                PopupSlider(
                                    label = "Icon Size",
                                    value = dockIconSize,
                                    valueRange = 24f..96f,
                                    valueSuffix = " dp",
                                    subTextColor = subTextColor,
                                    accentColor = accentColor,
                                    onValueChange = {
                                        dockIconSize = it
                                        prefs.edit { putFloat("dock_icon_size", it) }
                                    }
                                )

                                PopupSlider(
                                    label = "Label Size",
                                    value = dockTextSize,
                                    valueRange = 8f..20f,
                                    valueSuffix = " sp",
                                    subTextColor = subTextColor,
                                    accentColor = accentColor,
                                    onValueChange = {
                                        dockTextSize = it
                                        prefs.edit { putFloat("dock_text_size", it) }
                                    }
                                )

                                Button(
                                    onClick = { onOpenFontPicker("dock_font_family", "Dock Font") },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.FontDownload, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Change Dock Font", color = accentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Theming Section
                        item {
                            ThemingSection(
                                isExpanded = isThemingExpanded,
                                dockBgColor = dockBgColor,
                                dockBgAlpha = dockBgAlpha,
                                dockTextColor = dockTextColor,
                                accentColor = accentColor,
                                subTextColor = subTextColor,
                                onToggleExpand = { isThemingExpanded = !isThemingExpanded },
                                onBgColorChange = {
                                    dockBgColor = it.toArgb()
                                    prefs.edit { putInt("dock_bg_color", it.toArgb()) }
                                },
                                onBgAlphaChange = {
                                    dockBgAlpha = it
                                    prefs.edit { putFloat("dock_bg_alpha", it) }
                                },
                                onTextColorChange = {
                                    dockTextColor = it.toArgb()
                                    prefs.edit { putInt("dock_text_color", it.toArgb()) }
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

@Composable
private fun AppSelectionSection(
    isExpanded: Boolean,
    allApps: List<AppModel>,
    selectedIds: List<String>,
    repository: AppRepository,
    accentColor: Color,
    contentColor: Color,
    backgroundColor: Color,
    subTextColor: Color,
    onToggleExpand: () -> Unit,
    onUpdate: (List<String>) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PopupSectionHeader(Icons.Default.Apps, "Select Apps", accentColor)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Select up to 8 apps. 5 is recommended.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor
                )
                AppSelectionContent(
                    allApps = allApps,
                    selectedIds = selectedIds,
                    repository = repository,
                    contentColor = contentColor,
                    backgroundColor = backgroundColor,
                    onUpdate = onUpdate
                )
            }
        }
    }
}

@Composable
private fun ThemingSection(
    isExpanded: Boolean,
    dockBgColor: Int,
    dockBgAlpha: Float,
    dockTextColor: Int,
    accentColor: Color,
    subTextColor: Color,
    onToggleExpand: () -> Unit,
    onBgColorChange: (Color) -> Unit,
    onBgAlphaChange: (Float) -> Unit,
    onTextColorChange: (Color) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PopupSectionHeader(Icons.Default.Palette, "Theming", accentColor)
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = subTextColor
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Background Color", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                    Spacer(Modifier.height(8.dp))
                    ColorGrid(
                        selectedColor = dockBgColor,
                        onColorSelected = onBgColorChange
                    )
                }

                PopupSlider(
                    label = "Background Alpha",
                    value = dockBgAlpha,
                    valueRange = 0f..1f,
                    isDecimal = true,
                    subTextColor = subTextColor,
                    accentColor = accentColor,
                    onValueChange = onBgAlphaChange
                )

                Column {
                    Text("Text Color", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                    Spacer(Modifier.height(8.dp))
                    ColorGrid(
                        selectedColor = dockTextColor,
                        onColorSelected = onTextColorChange
                    )
                }
            }
        }
    }
}

// --- Micro UI Controller Components ---

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
                text = if (isDecimal) String.format(Locale.US, "%.2f", value) + valueSuffix else "${value.roundToInt()}$valueSuffix",
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

@Composable
private fun PopupSegmentedControl(
    label: String,
    currentSelection: String,
    options: Map<String, String>,
    subTextColor: Color,
    accentColor: Color,
    onOptionSelected: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = subTextColor)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.05f))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (key, value) ->
                val isSelected = currentSelection == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .clickable { onOptionSelected(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) {
                            if (isDark) Color.Black else Color.White
                        } else {
                            accentColor.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorGrid(selectedColor: Int, onColorSelected: (Color) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colorPalette.chunked(8).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(color) }
                            .then(
                                if (selectedColor == color.toArgb()) {
                                    Modifier.background(Color.Transparent, CircleShape)
                                        .padding(2.dp)
                                        .background(color, CircleShape)
                                        .clip(CircleShape)
                                        .background(if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f))
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == color.toArgb()) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
