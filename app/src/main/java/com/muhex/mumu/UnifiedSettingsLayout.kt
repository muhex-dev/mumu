package com.muhex.mumu

import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import kotlin.math.roundToInt

internal val colorPalette = listOf(
    Color.White, Color.Black, Color.Gray, Color.Red, Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
    Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFFEB3B), Color(0xFFFFC107)
)

@Composable
internal fun HeaderHandle(
    contentColor: Color,
    sheetOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = onDragEnd,
                    onVerticalDrag = { _, dragAmount -> onDrag(dragAmount) }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(30.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.2f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Opacity, null, tint = contentColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            Slider(
                value = sheetOpacity,
                onValueChange = onOpacityChange,
                valueRange = 0.5f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color(0xFF4CAF50), activeTrackColor = Color(0xFF4CAF50))
            )
            Text("${(sheetOpacity * 100).toInt()}%", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun LayoutsTab(prefs: SharedPreferences, contentColor: Color) {
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
                        prefs.edit().putBoolean(item.prefKey, it).apply()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50))
                )
            }
        }
    }
}

@Composable
internal fun ClockPreview(index: Int, modifier: Modifier = Modifier) {
    val imageRes = when (index) {
        0 -> R.drawable.rotating
        1 -> R.drawable.classic
        2 -> R.drawable.stack
        3 -> R.drawable.info
        4 -> R.drawable.greeting
        5 -> R.drawable.numeric
        6 -> R.drawable.modern
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        if (imageRes != null) {
            Image(
                painter = rememberAsyncImagePainter(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp).align(Alignment.Center)
            )
        }
    }
}

@Composable
internal fun ClockTab(prefs: SharedPreferences, contentColor: Color, onOpenClockSettings: () -> Unit) {
    val clockStyles = listOf(
        "Rotating", "Classic", "Stack", "Info",
        "Greeting", "Numeric", "Modern"
    )

    var selectedIndex by remember { mutableIntStateOf(prefs.getInt("clock_index", 0)) }
    var addedClocksOrder by remember {
        val ordered = prefs.getString("added_clocks_ordered", null)
        val initialList = ordered?.split(",")?.filter { it.isNotEmpty() }
            ?: prefs.getStringSet("added_clocks", setOf("0"))?.toList()
            ?: listOf("0")
        mutableStateOf(initialList)
    }

    val allStyles = clockStyles.mapIndexed { index, name -> index to name }
    val addedList = addedClocksOrder.mapNotNull { id ->
        allStyles.find { it.first.toString() == id }
    }
    val availableList = allStyles.filter { (index, _) -> !addedClocksOrder.contains(index.toString()) }

    val onToggleAdded: (Int) -> Unit = { index ->
        val newList = addedClocksOrder.toMutableList()
        val idxStr = index.toString()
        if (newList.contains(idxStr)) {
            if (newList.size > 1) {
                newList.remove(idxStr)
            }
        } else {
            newList.add(idxStr)
        }
        addedClocksOrder = newList
        prefs.edit()
            .putStringSet("added_clocks", newList.toSet())
            .putString("added_clocks_ordered", newList.joinToString(","))
            .apply()
    }

    val onMove: (Int, Int) -> Unit = { from, to ->
        val newList = addedClocksOrder.toMutableList()
        if (from in newList.indices && to in newList.indices) {
            val item = newList.removeAt(from)
            newList.add(to, item)
            addedClocksOrder = newList
            prefs.edit()
                .putStringSet("added_clocks", newList.toSet())
                .putString("added_clocks_ordered", newList.joinToString(","))
                .apply()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Clock Style",
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (addedList.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DragHandle, null, tint = contentColor.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ADDED TO HOME (DRAG TO REORDER)", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            item {
                ReorderableClockGrid(
                    items = addedList,
                    selectedIndex = selectedIndex,
                    contentColor = contentColor,
                    onSelect = {
                        selectedIndex = it
                        prefs.edit().putInt("clock_index", it).apply()
                    },
                    onToggleAdded = onToggleAdded,
                    onMove = onMove
                )
            }
        }

        if (availableList.isNotEmpty()) {
            item {
                Text("AVAILABLE STYLES", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp))
            }

            items(availableList.chunked(2)) { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowItems.forEach { (index, name) ->
                        Box(modifier = Modifier.weight(1f)) {
                            ClockGridItem(
                                index = index,
                                name = name,
                                isSelected = selectedIndex == index,
                                isAdded = false,
                                contentColor = contentColor,
                                onSelect = {
                                    selectedIndex = index
                                    prefs.edit().putInt("clock_index", index).apply()
                                },
                                onToggleAdded = { onToggleAdded(index) }
                            )
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Selected: ${clockStyles.getOrNull(selectedIndex) ?: "Unknown"}",
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selectedIndex == 0 || clockStyles.getOrNull(selectedIndex) == "Rotating") {
                    SettingItem(
                        icon = Icons.Default.Tune,
                        title = "Customize Clock Style",
                        value = "Colors, Fonts & Scaling",
                        contentColor = contentColor,
                        isExpanded = false,
                        onClick = onOpenClockSettings
                    ) { }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
internal fun ReorderableClockGrid(
    items: List<Pair<Int, String>>,
    selectedIndex: Int,
    contentColor: Color,
    onSelect: (Int) -> Unit,
    onToggleAdded: (Int) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // We use a local list for immediate visual feedback during drag
    var listForDisplay by remember(items) { mutableStateOf(items) }

    val spacing = 16.dp
    val columns = 2

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gridWidth = maxWidth
        val itemSize = (gridWidth - spacing) / columns
        val itemHeight = itemSize / 1.35f

        val itemWidthPx = with(density) { itemSize.toPx() }
        val itemHeightPx = with(density) { itemHeight.toPx() }
        val spacingPx = with(density) { spacing.toPx() }

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            listForDisplay.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    rowItems.forEachIndexed { colIndex, (index, name) ->
                        val globalIndex = rowIndex * columns + colIndex
                        val isDragging = draggingIndex == globalIndex

                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.15f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        )

                        Box(
                            modifier = Modifier
                                .width(itemSize)
                                .zIndex(if (isDragging) 10f else 0f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    if (isDragging) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        shadowElevation = 20.dp.toPx()
                                    }
                                }
                                .pointerInput(globalIndex, items) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = globalIndex
                                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragEnd = {
                                            draggingIndex = null
                                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount

                                            val movedCols = (dragOffset.x / (itemWidthPx + spacingPx)).roundToInt()
                                            val movedRows = (dragOffset.y / (itemHeightPx + spacingPx)).roundToInt()

                                            val targetIndex = (globalIndex + movedRows * columns + movedCols)
                                                .coerceIn(0, listForDisplay.size - 1)

                                            if (targetIndex != draggingIndex && draggingIndex != null) {
                                                // Real-time swap in the display list
                                                val newList = listForDisplay.toMutableList()
                                                val item = newList.removeAt(draggingIndex!!)
                                                newList.add(targetIndex, item)
                                                listForDisplay = newList

                                                // Update dragging index to track the item's new position
                                                draggingIndex = targetIndex
                                                // Adjust offset so the item stays under the finger
                                                val colDiff = (targetIndex % columns) - (globalIndex % columns)
                                                val rowDiff = (targetIndex / columns) - (globalIndex / columns)
                                                dragOffset = androidx.compose.ui.geometry.Offset(
                                                    dragOffset.x - colDiff * (itemWidthPx + spacingPx),
                                                    dragOffset.y - rowDiff * (itemHeightPx + spacingPx)
                                                )

                                                // Commit the move to preferences
                                                onMove(globalIndex, targetIndex)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    )
                                }
                        ) {
                            ClockGridItem(
                                index = index,
                                name = name,
                                isSelected = selectedIndex == index,
                                isAdded = true,
                                contentColor = contentColor,
                                onSelect = { onSelect(index) },
                                onToggleAdded = { onToggleAdded(index) }
                            )
                        }
                    }
                    if (rowItems.size < columns) {
                        Spacer(modifier = Modifier.width(itemSize))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ClockGridItem(
    index: Int,
    name: String,
    isSelected: Boolean,
    isAdded: Boolean,
    contentColor: Color,
    onSelect: () -> Unit,
    onToggleAdded: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(24.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF4CAF50) else contentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ClockPreview(index = index)

                // Remove button for added clocks (top-right overlay)
                if (isAdded) {
                    IconButton(
                        onClick = onToggleAdded,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.12f) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name,
                    color = if (isSelected) Color(0xFF4CAF50) else contentColor,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isAdded) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add",
                        tint = contentColor.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onToggleAdded() }
                    )
                }
            }
        }
    }
}

@Composable
internal fun DrawerTab(prefs: SharedPreferences, contentColor: Color, onOpenFontPicker: (String, String) -> Unit) {
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
                                    prefs.edit().putInt("drawer_columns", col).apply() 
                                }.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$col", color = if (isSelected) Color.White else contentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    SliderSettingFloat("Background Opacity", drawerOpacity.toFloat(), 0f..100f, contentColor) { 
                        drawerOpacity = it.toInt()
                        prefs.edit().putInt("drawer_opacity", it.toInt()).apply() 
                    }
                    SliderSettingFloat("Item Transparency", drawerItemOpacity.toFloat(), 10f..100f, contentColor) { 
                        drawerItemOpacity = it.toInt()
                        prefs.edit().putInt("drawer_item_opacity", it.toInt()).apply() 
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.TextFields, "Icon & Label", "Sizes and Display", contentColor, expandedItem == "label", onClick = { expandedItem = if (expandedItem == "label") null else "label" }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownSetting("Display Mode", drawerDisplayMode, displayModeOptions, contentColor) { 
                        drawerDisplayMode = it
                        prefs.edit().putString("drawer_display_mode", it).apply() 
                    }
                    SliderSettingFloat("Icon Size", drawerIconSize, 24f..72f, contentColor) { 
                        drawerIconSize = it
                        prefs.edit().putFloat("drawer_icon_size", it).apply() 
                    }
                    SliderSettingFloat("Label Size", drawerLabelSize, 8f..24f, contentColor) { 
                        drawerLabelSize = it
                        prefs.edit().putFloat("drawer_label_size", it).apply() 
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
                        prefs.edit().putString("drawer_open_anim", it).apply() 
                    }
                    DropdownSetting("Close Animation", drawerCloseAnim, animationOptions, contentColor) { 
                        drawerCloseAnim = it
                        prefs.edit().putString("drawer_close_anim", it).apply() 
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.SortByAlpha, "Scroller", "Muhex Style Scroller", contentColor, expandedItem == "scroller", onClick = { expandedItem = if (expandedItem == "scroller") null else "scroller" }) {
                Column {
                    SliderSettingFloat("Edge Offset", scrollerPadding, 0.05f..0.4f, contentColor) { 
                        scrollerPadding = it
                        prefs.edit().putFloat("scroller_padding", it).apply() 
                    }
                    SliderSettingFloat("Bending", scrollerBending, 0f..800f, contentColor) { 
                        scrollerBending = it
                        prefs.edit().putFloat("scroller_bending", it).apply() 
                    }
                    SliderSettingFloat("Curve Spread", scrollerSpread, 1f..25f, contentColor) { 
                        scrollerSpread = it
                        prefs.edit().putFloat("scroller_spread", it).apply() 
                    }
                    SliderSettingFloat("Font Size", scrollerTextSize, 8f..64f, contentColor) { 
                        scrollerTextSize = it
                        prefs.edit().putFloat("scroller_text_size", it).apply() 
                    }
                    SliderSettingFloat("Active Scale", scrollerScale, 1.0f..5f, contentColor) { 
                        scrollerScale = it
                        prefs.edit().putFloat("scroller_scale", it).apply() 
                    }
                    SliderSettingFloat("Line Opacity", scrollerLineAlpha.toFloat(), 0f..255f, contentColor) { 
                        scrollerLineAlpha = it.toInt()
                        prefs.edit().putInt("scroller_line_alpha", it.toInt()).apply() 
                    }
                    SliderSettingFloat("Idle Opacity", scrollerBaseAlpha.toFloat(), 0f..255f, contentColor) { 
                        scrollerBaseAlpha = it.toInt()
                        prefs.edit().putInt("scroller_base_alpha", it.toInt()).apply() 
                    }
                    SliderSettingFloat("Anim Speed", scrollerAnimDuration.toFloat(), 50f..1000f, contentColor) { 
                        scrollerAnimDuration = it.toInt()
                        prefs.edit().putInt("scroller_anim_duration", it.toInt()).apply() 
                    }
                    SliderSettingFloat("Sensitivity", scrollerTouchSlop, 20f..300f, contentColor) { 
                        scrollerTouchSlop = it
                        prefs.edit().putFloat("scroller_touch_slop", it).apply() 
                    }
                    SettingToggle("Haptic Feedback", scrollerHaptic, contentColor) { 
                        scrollerHaptic = it
                        prefs.edit().putBoolean("scroller_haptic", it).apply() 
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

@Composable
internal fun DockTab(repository: AppRepository, prefs: SharedPreferences, contentColor: Color, backgroundColor: Color, onOpenFontPicker: (String, String) -> Unit) {
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
                        prefs.edit()
                            .putString("dock_orientation", it)
                            .putString("dock_content_alignment", newAlign)
                            .apply()
                    }
                    if (orientation == "vertical") {
                        DropdownSetting("Side Alignment", dockAlignment, alignmentOptions, contentColor) {
                            dockAlignment = it
                            prefs.edit().putString("dock_alignment", it).apply()
                        }
                    }
                    SliderSettingFloat("Bottom Margin", dockBottomMargin, 0f..200f, contentColor) {
                        dockBottomMargin = it
                        prefs.edit().putFloat("dock_bottom_margin", it).apply()
                    }
                    if (orientation == "vertical") {
                        SliderSettingFloat("Side Margin", dockHorizontalMargin, 0f..200f, contentColor) {
                            dockHorizontalMargin = it
                            prefs.edit().putFloat("dock_horizontal_margin", it).apply()
                        }
                    }
                    SliderSettingFloat("Item Spacing", dockSpacing, 0f..100f, contentColor) {
                        dockSpacing = it
                        prefs.edit().putFloat("dock_spacing", it).apply()
                    }
                }
            }
        }
        item {
            SettingItem(Icons.Default.TextFields, "Content", "Text and Display", contentColor, expandedItem == "content", onClick = { expandedItem = if (expandedItem == "content") null else "content" }) {
                Column {
                    DropdownSetting("Display Mode", dockDisplayMode, displayModeOptions, contentColor) { 
                        dockDisplayMode = it
                        prefs.edit().putString("dock_display_mode", it).apply() 
                    }
                    SliderSettingFloat("Icon Size", dockIconSize, 24f..96f, contentColor) { 
                        dockIconSize = it
                        prefs.edit().putFloat("dock_icon_size", it).apply() 
                    }
                    SliderSettingFloat("Text Size", dockTextSize, 8f..20f, contentColor) { 
                        dockTextSize = it
                        prefs.edit().putFloat("dock_text_size", it).apply() 
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
                        prefs.edit().putInt("dock_bg_color", it.toArgb()).apply() 
                    }
                    SliderSettingFloat("Background Alpha", dockBgAlpha, 0f..1f, contentColor) { 
                        dockBgAlpha = it
                        prefs.edit().putFloat("dock_bg_alpha", it).apply() 
                    }
                    ColorSection("Text Color", colorPalette, dockTextColor) { 
                        dockTextColor = it.toArgb()
                        prefs.edit().putInt("dock_text_color", it.toArgb()).apply() 
                    }
                }
            }
        }
    }
}

@Composable
internal fun PinnedAppsTab(repository: AppRepository, prefs: SharedPreferences, contentColor: Color, backgroundColor: Color, onOpenFontPicker: (String, String) -> Unit) {
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
                                        prefs.edit().putInt("now_apps_visible_count", count).apply()
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
                        prefs.edit().putFloat("now_apps_vertical_offset", it).apply()
                    }
                    SliderSettingFloat("Horizontal Position", nowAppsHorizontalOffset, -200f..200f, contentColor) {
                        nowAppsHorizontalOffset = it
                        prefs.edit().putFloat("now_apps_horizontal_offset", it).apply()
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
                        prefs.edit().putFloat("now_apps_card_width", it).apply()
                    }
                    SliderSettingFloat("Height", nowAppsCardHeight, 40f..180f, contentColor) {
                        nowAppsCardHeight = it
                        prefs.edit().putFloat("now_apps_card_height", it).apply()
                    }
                    SliderSettingFloat("Corner Roundness", nowAppsCornerRadius, 0f..64f, contentColor) {
                        nowAppsCornerRadius = it
                        prefs.edit().putFloat("now_apps_corner_radius", it).apply()
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
                        prefs.edit().putString("now_apps_display_mode", it).apply()
                    }
                    SliderSettingFloat("Icon Size", nowAppsIconSize, 16f..72f, contentColor) {
                        nowAppsIconSize = it
                        prefs.edit().putFloat("now_apps_icon_size", it).apply()
                    }
                    SliderSettingFloat("Text Size", nowAppsTextSize, 10f..28f, contentColor) {
                        nowAppsTextSize = it
                        prefs.edit().putFloat("now_apps_text_size", it).apply()
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
                        prefs.edit().putInt("now_apps_bg_color", it.toArgb()).apply()
                    }
                    ColorSection("Card Stroke Color", colorPalette, nowAppsStrokeColor) {
                        nowAppsStrokeColor = it.toArgb()
                        prefs.edit().putInt("now_apps_stroke_color", it.toArgb()).apply()
                    }
                    SliderSettingFloat("Overall Alpha", nowAppsOpacity, 0.1f..1f, contentColor) {
                        nowAppsOpacity = it
                        prefs.edit().putFloat("now_apps_opacity", it).apply()
                    }
                    ColorSection("Text & Icon Color", colorPalette, nowAppsTextColor) {
                        nowAppsTextColor = it.toArgb()
                        prefs.edit().putInt("now_apps_text_color", it.toArgb()).apply()
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
                    
                    prefs.edit()
                        .putFloat("now_apps_card_width", 220f)
                        .putFloat("now_apps_card_height", 64f)
                        .putFloat("now_apps_vertical_offset", 0f)
                        .putInt("now_apps_visible_count", 3)
                        .putFloat("now_apps_corner_radius", 28f)
                        .apply()
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


@Composable
internal fun GesturesTab(repository: AppRepository, prefs: SharedPreferences, contentColor: Color) {
    val gestureOptions = mapOf(
        "none" to "None",
        "app_drawer" to "App Drawer",
        "notifications" to "Notifications",
        "quick_settings" to "Quick Settings",
        "lock_screen" to "Lock Screen",
        "quick_menu" to "Quick Menu",
        "open_app" to "Open App"
    )
    var expandedItem by remember { mutableStateOf<String?>(null) }
    var selectingAppForKey by remember { mutableStateOf<String?>(null) }
    val allApps by produceState(initialValue = emptyList<AppModel>()) {
        value = repository.getAllApps()
    }

    val gestureKeys = remember {
        listOf("gesture_swipe_up", "gesture_swipe_down", "gesture_swipe_left", "gesture_swipe_right", "gesture_double_tap")
    }

    val gestureAssignments = remember {
        mutableStateMapOf<String, String>().also { map ->
            gestureKeys.forEach { key ->
                map[key] = prefs.getString(key, "none") ?: "none"
            }
        }
    }
    val gestureAppIds = remember {
        mutableStateMapOf<String, String>().also { map ->
            gestureKeys.forEach { key ->
                val appIdKey = "${key}_app_id"
                map[appIdKey] = prefs.getString(appIdKey, "") ?: ""
            }
        }
    }

    if (selectingAppForKey != null) {
        AlertDialog(
            onDismissRequest = { selectingAppForKey = null },
            title = { Text("Select App", color = contentColor) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        items(allApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val appId = repository.getAppUniqueId(app)
                                        val key = selectingAppForKey!!
                                        
                                        gestureAppIds["${key}_app_id"] = appId
                                        gestureAssignments[key] = "open_app"

                                        prefs.edit()
                                            .putString("${key}_app_id", appId)
                                            .putString(key, "open_app")
                                            .apply()
                                        selectingAppForKey = null
                                        expandedItem = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(app.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    app.label,
                                    modifier = Modifier.padding(start = 12.dp),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectingAppForKey = null }) {
                    Text("Cancel", color = Color(0xFF4CAF50))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = contentColor
        )
    }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            "Up" to "gesture_swipe_up",
            "Down" to "gesture_swipe_down",
            "Left" to "gesture_swipe_left",
            "Right" to "gesture_swipe_right",
            "Double Tap" to "gesture_double_tap"
        ).forEach { (label, key) ->
            item {
                val current = gestureAssignments[key] ?: "none"
                val assignedAppId = gestureAppIds["${key}_app_id"] ?: ""
                val assignedAppLabel = if (current == "open_app" && assignedAppId.isNotEmpty()) {
                    allApps.find { repository.getAppUniqueId(it) == assignedAppId }?.label ?: "Unknown App"
                } else null

                val displayValue = if (assignedAppLabel != null) "Open $assignedAppLabel" else gestureOptions[current] ?: "None"

                SettingItem(
                    icon = Icons.Default.TouchApp,
                    title = label,
                    value = displayValue,
                    contentColor = contentColor,
                    isExpanded = expandedItem == key,
                    onClick = { expandedItem = if (expandedItem == key) null else key }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gestureOptions.forEach { (optionKey, optionLabel) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (optionKey == "open_app") {
                                            selectingAppForKey = key
                                        } else {
                                            gestureAssignments[key] = optionKey
                                            prefs.edit().putString(key, optionKey).apply()
                                            expandedItem = null
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = current == optionKey, onClick = null)
                                Text(optionLabel, color = contentColor, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
internal fun QuotesTab(prefs: SharedPreferences, contentColor: Color) {
    var quotesEnabled by remember { mutableStateOf(prefs.getBoolean("quotes_enabled", true)) }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Quotes Settings",
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Inspirational quotes are displayed on your home screen to keep you motivated throughout the day.",
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        item {
            SettingToggle("Show Quotes", quotesEnabled, contentColor) {
                quotesEnabled = it
                prefs.edit().putBoolean("quotes_enabled", it).apply()
            }
        }
    }
}
@Composable
internal fun WidgetsTab(
    prefs: SharedPreferences,
    topMode: String,
    contentColor: Color,
    onModeChanged: (String) -> Unit,
    onAddWidget: (() -> Unit)?
) {
    val isWidgetMode = topMode == WidgetSlotModel.MODE_WIDGETS

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Section title ────────────────────────────────────────────
        item {
            Text(
                "Top Section Mode",
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose what appears in the top area of your home screen.",
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }

        // ── Mode selector cards ──────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Clock mode card
                ModeCard(
                    title = "Clock",
                    subtitle = "Your custom clock styles",
                    icon = Icons.Default.WatchLater,
                    isSelected = !isWidgetMode,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChanged(WidgetSlotModel.MODE_CLOCK) }
                )

                // Widget mode card
                ModeCard(
                    title = "Widgets",
                    subtitle = "Real Android widgets",
                    icon = Icons.Default.Widgets,
                    isSelected = isWidgetMode,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChanged(WidgetSlotModel.MODE_WIDGETS) }
                )
            }
        }

        // ── Add Widget button (only shown in widget mode) ────────────
        if (isWidgetMode && onAddWidget != null) {
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAddWidget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C6DFF)
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Add Widget",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Pick from all widgets installed on your phone.\nLong-press any widget on the home screen to remove or resize it.",
                    color = contentColor.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // ── Info card when in clock mode ─────────────────────────────
        if (!isWidgetMode) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Switch to Widget mode to add real Android widgets from any installed app.",
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// ── Mode selector card ────────────────────────────────────────────────────────

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF7C6DFF) else contentColor.copy(alpha = 0.15f)
    val bgColor     = if (isSelected) Color(0xFF7C6DFF).copy(alpha = 0.12f) else contentColor.copy(alpha = 0.04f)

    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF7C6DFF) else contentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp)
        )
        Text(
            title,
            color = if (isSelected) Color(0xFF7C6DFF) else contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            subtitle,
            color = contentColor.copy(alpha = 0.5f),
            fontSize = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}
@Composable
internal fun SettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    contentColor: Color,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val borderColor = contentColor.copy(alpha = 0.08f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(contentColor.copy(alpha = 0.04f))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, color = contentColor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.5.sp)
                Text(value, color = Color(0xFF4CAF50).copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null,
                tint = contentColor.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun SettingToggle(label: String, initialValue: Boolean, contentColor: Color, onCheckedChange: (Boolean) -> Unit) {
    var checked by remember(initialValue) { mutableStateOf(initialValue) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = contentColor, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50))
        )
    }
}

@Composable
internal fun SliderSettingFloat(label: String, initialValue: Float, range: ClosedFloatingPointRange<Float>, color: Color, onValueChange: (Float) -> Unit) {
    var value by remember(initialValue) { mutableFloatStateOf(initialValue) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color, fontSize = 13.sp)
            Text("%.1f".format(value), color = color.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Slider(
            value = value,
            onValueChange = {
                value = it
                onValueChange(it)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4CAF50),
                activeTrackColor = Color(0xFF4CAF50),
                inactiveTrackColor = color.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
internal fun ColorSection(label: String, palette: List<Color>, selectedArgb: Int, onColorSelected: (Color) -> Unit) {
    var currentSelected by remember(selectedArgb) { mutableIntStateOf(selectedArgb) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            palette.take(8).forEach { color ->
                val isSelected = color.toArgb() == currentSelected
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color, CircleShape)
                        .border(2.dp, if (isSelected) Color(0xFF4CAF50) else Color.Transparent, CircleShape)
                        .clickable {
                            currentSelected = color.toArgb()
                            onColorSelected(color)
                        }
                )
            }
        }
    }
}

@Composable
internal fun AppSelectionContent(allApps: List<AppModel>, selectedIds: List<String>, repository: AppRepository, contentColor: Color, backgroundColor: Color, isPinnedApps: Boolean = false, onUpdate: (List<String>) -> Unit) {
    Column {
        allApps.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { app ->
                    val id = repository.getAppUniqueId(app)
                    val isSelected = selectedIds.contains(id)
                    Column(modifier = Modifier.weight(1f).clickable { if (isPinnedApps) repository.togglePin(app) else repository.toggleDock(app); onUpdate(if (isPinnedApps) repository.getPinnedIds() else repository.getDockIds()) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Image(painter = rememberAsyncImagePainter(app.icon), contentDescription = null, modifier = Modifier.size(40.dp))
                            if (isSelected) Box(modifier = Modifier.size(14.dp).align(Alignment.TopEnd).background(Color(0xFF4CAF50), CircleShape).border(2.dp, backgroundColor, CircleShape))
                        }
                        Text(app.label, color = contentColor, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
                if (row.size < 4) {
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun DropdownSetting(
    label: String,
    initialValue: String,
    options: Map<String, String>,
    contentColor: Color,
    onValueChange: (String) -> Unit
) {
    var currentValue by remember(initialValue) { mutableStateOf(initialValue) }
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = contentColor.copy(alpha = 0.6f), fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .background(contentColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(options[currentValue] ?: currentValue, color = contentColor, fontWeight = FontWeight.Medium)
                Icon(Icons.Default.ArrowDropDown, null, tint = contentColor.copy(alpha = 0.5f))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                options.forEach { (key, display) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            currentValue = key
                            onValueChange(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


internal data class UnifiedHomeItem(val name: String, val description: String, val icon: ImageVector, val prefKey: String)
