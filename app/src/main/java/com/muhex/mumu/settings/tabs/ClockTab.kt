package com.muhex.mumu.settings.tabs

import androidx.core.content.edit
import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.muhex.mumu.R
import com.muhex.mumu.settings.SettingItem
import kotlin.math.roundToInt

@Composable
fun ClockPreview(index: Int, modifier: Modifier = Modifier) {
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
fun ClockTab(prefs: SharedPreferences, contentColor: Color, onOpenClockSettings: () -> Unit) {
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
        prefs.edit {
            putStringSet("added_clocks", newList.toSet())
            putString("added_clocks_ordered", newList.joinToString(","))
        }
    }

    val onMove: (Int, Int) -> Unit = { from, to ->
        val newList = addedClocksOrder.toMutableList()
        if (from in newList.indices && to in newList.indices) {
            val item = newList.removeAt(from)
            newList.add(to, item)
            addedClocksOrder = newList
            prefs.edit {
                putStringSet("added_clocks", newList.toSet())
                putString("added_clocks_ordered", newList.joinToString(","))
            }
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
                        prefs.edit { putInt("clock_index", it) }
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
                                    prefs.edit { putInt("clock_index", index) }
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
fun ReorderableClockGrid(
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
fun ClockGridItem(
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
