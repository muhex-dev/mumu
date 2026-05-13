package com.example.myapplication

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class WidgetAppGroup(
    val appLabel: String,
    val appIcon: Drawable?,
    val widgets: List<WidgetProviderItem>
)

data class WidgetProviderItem(
    val info: AppWidgetProviderInfo,
    val label: String
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerScreen(
    providers: List<AppWidgetProviderInfo>,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF050505) else Color(0xFFF8F9FA)
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
    val accentColor = Color(0xFF30C1F4)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)

    var searchQuery by remember { mutableStateOf("") }
    var expandedAppLabel by remember { mutableStateOf<String?>(null) }

    val groupedWidgets = remember(providers, searchQuery) {
        providers.map { info ->
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(info.provider.packageName, 0)
            val appLabel = pm.getApplicationLabel(appInfo).toString()
            val widgetLabel = info.loadLabel(pm)
            WidgetProviderItem(info, widgetLabel) to appLabel
        }
        .filter { it.first.label.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }
        .groupBy({ it.second }, { it.first })
        .map { (appLabel, widgets) ->
            val pm = context.packageManager
            val appIcon = try {
                pm.getApplicationIcon(widgets.first().info.provider.packageName)
            } catch (e: Exception) {
                null
            }
            WidgetAppGroup(appLabel, appIcon, widgets.sortedBy { it.label })
        }
        .sortedBy { it.appLabel }
    }

    // Auto-expand if only one result remains
    LaunchedEffect(groupedWidgets) {
        if (groupedWidgets.size == 1) {
            expandedAppLabel = groupedWidgets.first().appLabel
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // High-end background glows
        val infiniteTransition = rememberInfiniteTransition(label = "BackgroundGlow")
        val glowOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 60f,
            animationSpec = infiniteRepeatable(
                animation = tween(15000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowOffset"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(120.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(500.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 150.dp + glowOffset.dp, y = (-150).dp)
                    .background(accentColor.copy(alpha = if (isDark) 0.08f else 0.12f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(450.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-150).dp - glowOffset.dp, y = 200.dp)
                    .background(Color(0xFF7C6DFF).copy(alpha = if (isDark) 0.08f else 0.12f), CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Add Widget",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            letterSpacing = (-1).sp
                        )
                    )
                    Text(
                        "Select an app to browse widgets",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = subTextColor
                        )
                    )
                }
                
                IconButton(
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss() 
                    },
                    modifier = Modifier
                        .background(textColor.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, borderColor, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .holoBorder(
                        cornerRadius = 16.dp, 
                        durationMillis = 10000, 
                        baseAlpha = if (isDark) 0.2f else 0.15f,
                        color = textColor
                    )
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps or widgets...", color = subTextColor.copy(alpha = 0.3f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = subTextColor.copy(alpha = 0.4f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = textColor.copy(alpha = 0.03f),
                        unfocusedContainerColor = textColor.copy(alpha = 0.03f),
                        disabledContainerColor = textColor.copy(alpha = 0.03f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Accordion List with Exclusive Expansion
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                groupedWidgets.forEachIndexed { index, group ->
                    val isExpanded = expandedAppLabel == group.appLabel
                    
                    item(key = "header_${group.appLabel}") {
                        AppAccordionHeader(
                            modifier = Modifier.animateItem(),
                            group = group,
                            isExpanded = isExpanded,
                            isDark = isDark,
                            textColor = textColor,
                            accentColor = accentColor,
                            onToggle = {
                                expandedAppLabel = if (isExpanded) null else group.appLabel
                                if (!isExpanded) {
                                    scope.launch {
                                        delay(150) // Wait for expansion to start
                                        listState.animateScrollToItem(index * 2)
                                    }
                                }
                            }
                        )
                    }

                    item(key = "content_${group.appLabel}") {
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        textColor.copy(alpha = 0.02f),
                                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    )
                                    .padding(bottom = 12.dp)
                            ) {
                                group.widgets.forEach { widget ->
                                    WidgetPickerItemRow(
                                        widget = widget,
                                        isDark = isDark,
                                        textColor = textColor,
                                        accentColor = accentColor,
                                        onWidgetSelected = onWidgetSelected
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

@Composable
fun AppAccordionHeader(
    modifier: Modifier = Modifier,
    group: WidgetAppGroup,
    isExpanded: Boolean,
    isDark: Boolean,
    textColor: Color,
    accentColor: Color,
    onToggle: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "ArrowRotation"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isExpanded) {
            textColor.copy(alpha = if (isDark) 0.08f else 0.06f)
        } else {
            textColor.copy(alpha = if (isDark) 0.04f else 0.03f)
        },
        label = "BgColor"
    )

    val shape = if (isExpanded) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    } else {
        RoundedCornerShape(16.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(shape)
            .background(backgroundColor)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            group.appIcon?.let {
                Image(
                    bitmap = it.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.appLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) accentColor else textColor,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "${group.widgets.size} widget${if (group.widgets.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = textColor.copy(alpha = 0.4f)
                    )
                )
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isExpanded) accentColor else textColor.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
fun WidgetPickerItemRow(
    widget: WidgetProviderItem,
    isDark: Boolean,
    textColor: Color,
    accentColor: Color,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onWidgetSelected(widget.info)
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Widget Preview Placeholder
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                textColor.copy(alpha = if (isDark) 0.08f else 0.05f),
                                textColor.copy(alpha = if (isDark) 0.02f else 0.01f)
                            )
                        ),
                        RoundedCornerShape(10.dp)
                    )
                    .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = widget.label.take(1).uppercase(),
                    color = textColor.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = widget.label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "${widget.info.minWidth} x ${widget.info.minHeight}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = accentColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
