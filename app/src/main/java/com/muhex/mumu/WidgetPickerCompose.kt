package com.muhex.mumu

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class WidgetAppGroup(
    val appLabel: String,
    val appIcon: Drawable?,
    val widgets: List<WidgetProviderItem>
)

data class WidgetProviderItem(
    val info: AppWidgetProviderInfo,
    val label: String
)

data class WidgetPickerTheme(
    val background: Color,
    val text: Color,
    val subText: Color,
    val accent: Color,
    val border: Color,
    val holoAlpha: Float
) {
    companion object {
        fun getTheme(isDark: Boolean) = if (isDark) {
            WidgetPickerTheme(
                background = Color(0xFF050505),
                text = Color.White,
                subText = Color.White.copy(alpha = 0.5f),
                accent = Color(0xFF30C1F4),
                border = Color.White.copy(alpha = 0.1f),
                holoAlpha = 0.2f
            )
        } else {
            WidgetPickerTheme(
                background = Color(0xFFF8F9FA),
                text = Color.Black,
                subText = Color.Black.copy(alpha = 0.5f),
                accent = Color(0xFF30C1F4),
                border = Color.Black.copy(alpha = 0.08f),
                holoAlpha = 0.15f
            )
        }
    }
}

// ── MAIN COMPOSABLE ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerScreen(
    providers: List<AppWidgetProviderInfo>,
    addedProviders: Set<ComponentName> = emptySet(),
    successCount: Int = 0,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    val isDark = isSystemInDarkTheme()
    val theme = remember(isDark) { WidgetPickerTheme.getTheme(isDark) }

    var searchQuery by remember { mutableStateOf("") }
    var expandedAppLabel by remember { mutableStateOf<String?>(null) }
    
    var selectedWidgetForConfirmation by remember { mutableStateOf<WidgetProviderItem?>(null) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(successCount) {
        if (successCount > 0) {
            showSuccessAnimation = true
            delay(2000)
            showSuccessAnimation = false
        }
    }

    val groupedWidgets = remember(providers, searchQuery) {
        val pm = context.packageManager
        val distinctPackages = providers.map { it.provider.packageName }.distinct()
        val appMetadata = distinctPackages.associateWith { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString() to pm.getApplicationIcon(ai)
            } catch (e: Exception) {
                pkg to null
            }
        }

        providers.map { info ->
            val widgetLabel = info.loadLabel(pm)
            val appLabel = appMetadata[info.provider.packageName]?.first ?: info.provider.packageName
            WidgetProviderItem(info, widgetLabel) to appLabel
        }
        .filter { 
            it.first.label.contains(searchQuery, ignoreCase = true) || 
            it.second.contains(searchQuery, ignoreCase = true) 
        }
        .groupBy({ it.second }, { it.first })
        .map { (appLabel, widgets) ->
            val appIcon = appMetadata[widgets.first().info.provider.packageName]?.second
            WidgetAppGroup(appLabel, appIcon, widgets.sortedBy { it.label })
        }
        .sortedBy { it.appLabel }
    }

    LaunchedEffect(groupedWidgets) {
        if (groupedWidgets.size == 1) {
            expandedAppLabel = groupedWidgets.first().appLabel
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        WidgetBackgroundGlows(theme.accent)

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            WidgetPickerHeader(theme.text, theme.subText, theme.border, onDismiss)

            WidgetSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                theme = theme
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                groupedWidgets.forEachIndexed { index, group ->
                    val isExpanded = expandedAppLabel == group.appLabel
                    
                    item(key = "header_${group.appLabel}") {
                        AppAccordionHeader(
                            group = group,
                            isExpanded = isExpanded,
                            theme = theme,
                            onToggle = {
                                expandedAppLabel = if (isExpanded) null else group.appLabel
                                if (!isExpanded) {
                                    scope.launch {
                                        delay(200)
                                        listState.animateScrollToItem(index * 2)
                                    }
                                }
                            }
                        )
                    }

                    item(key = "content_${group.appLabel}") {
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        theme.text.copy(alpha = 0.02f),
                                        RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                                    )
                                    .padding(bottom = 8.dp)
                            ) {
                                group.widgets.forEach { widget ->
                                    WidgetPickerItemRow(
                                        widget = widget,
                                        theme = theme,
                                        isSuccess = addedProviders.contains(widget.info.provider),
                                        onWidgetSelected = { selectedWidgetForConfirmation = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Confirmation Dialog
        selectedWidgetForConfirmation?.let { widgetItem ->
            WidgetConfirmationDialog(
                widget = widgetItem,
                theme = theme,
                onConfirm = {
                    onWidgetSelected(widgetItem.info)
                    selectedWidgetForConfirmation = null
                },
                onDismiss = {
                    selectedWidgetForConfirmation = null
                }
            )
        }

        // Success Overlay
        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 1.2f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SuccessIndicator(theme)
        }
    }
}

// ── SUB-COMPONENTS ───────────────────────────────────────────────────────────

@Composable
private fun SuccessIndicator(theme: WidgetPickerTheme) {
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = theme.accent),
        modifier = Modifier.size(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

@Composable
private fun WidgetPickerHeader(
    textColor: Color,
    subTextColor: Color,
    borderColor: Color,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
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
                "Customize your home screen",
                style = MaterialTheme.typography.bodyMedium.copy(color = subTextColor)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    theme: WidgetPickerTheme
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .holoBorder(
                cornerRadius = 16.dp, 
                durationMillis = 8000, 
                baseAlpha = theme.holoAlpha,
                color = theme.text
            )
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search widgets...", color = theme.subText.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.subText.copy(alpha = 0.5f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = theme.text.copy(alpha = 0.03f),
                unfocusedContainerColor = theme.text.copy(alpha = 0.03f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = theme.text,
                unfocusedTextColor = theme.text
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}

@Composable
private fun AppAccordionHeader(
    group: WidgetAppGroup,
    isExpanded: Boolean,
    theme: WidgetPickerTheme,
    onToggle: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "Arrow")
    val bgColor by animateColorAsState(
        if (isExpanded) theme.text.copy(0.08f) else theme.text.copy(0.04f), label = "Bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(if (isExpanded) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) else RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            group.appIcon?.let {
                Image(
                    bitmap = it.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.appLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) theme.accent else theme.text
                    )
                )
                Text(
                    text = "${group.widgets.size} widget${if (group.widgets.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(color = theme.subText)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isExpanded) theme.accent else theme.subText,
                modifier = Modifier.size(24.dp).rotate(rotation)
            )
        }
    }
}

@Composable
private fun WidgetPickerItemRow(
    widget: WidgetProviderItem,
    theme: WidgetPickerTheme,
    isSuccess: Boolean = false,
    onWidgetSelected: (WidgetProviderItem) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val previewBitmap by produceState<ImageBitmap?>(initialValue = null, widget.info) {
        value = withContext(Dispatchers.IO) {
            try {
                (widget.info.loadPreviewImage(context, 0) ?: widget.info.loadIcon(context, 0))
                    ?.toBitmap()?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    val borderColor by animateColorAsState(
        if (isSuccess) theme.accent else theme.text.copy(alpha = 0.06f),
        label = "BorderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) theme.accent.copy(alpha = 0.05f) else theme.text.copy(alpha = 0.04f)
        ),
        border = BorderStroke(if (isSuccess) 2.dp else 1.dp, borderColor),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onWidgetSelected(widget)
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(theme.text.copy(0.05f), RoundedCornerShape(14.dp))
                    .border(1.dp, theme.text.copy(0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = previewBitmap, label = "Fade") { bitmap ->
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.padding(8.dp).fillMaxSize().clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(48.dp).background(theme.accent.copy(0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = widget.label.take(1).uppercase(),
                                color = theme.accent,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = widget.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = theme.text,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.2.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (isSuccess) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Added",
                            tint = theme.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = theme.accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${widget.info.minWidth} × ${widget.info.minHeight}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = theme.accent,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetConfirmationDialog(
    widget: WidgetProviderItem,
    theme: WidgetPickerTheme,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val previewBitmap by produceState<ImageBitmap?>(initialValue = null, widget.info) {
        value = withContext(Dispatchers.IO) {
            try {
                (widget.info.loadPreviewImage(context, 0) ?: widget.info.loadIcon(context, 0))
                    ?.toBitmap()?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = theme.background),
            border = BorderStroke(1.dp, theme.border)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add Widget?",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = theme.text
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = widget.label,
                    style = MaterialTheme.typography.bodyMedium.copy(color = theme.subText),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(theme.text.copy(0.05f), RoundedCornerShape(20.dp))
                        .border(1.dp, theme.text.copy(0.1f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.padding(12.dp).fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null, 
                            modifier = Modifier.size(48.dp),
                            tint = theme.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, theme.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.text)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

// ── UTILS ────────────────────────────────────────────────────────────────────

@Composable
private fun WidgetBackgroundGlows(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val glowOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 50f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Offset"
    )

    Box(modifier = Modifier.fillMaxSize().blur(120.dp)) {
        Box(
            modifier = Modifier.size(500.dp).align(Alignment.TopEnd)
                .offset(x = 150.dp + glowOffset.dp, y = (-150).dp)
                .background(accentColor.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier.size(450.dp).align(Alignment.BottomStart)
                .offset(x = (-150).dp - glowOffset.dp, y = 200.dp)
                .background(Color(0xFF7C6DFF).copy(alpha = 0.08f), CircleShape)
        )
    }
}
