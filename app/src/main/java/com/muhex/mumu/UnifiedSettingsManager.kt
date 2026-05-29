package com.muhex.mumu

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.muhex.mumu.settings.*
import com.muhex.mumu.settings.tabs.*
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSettingsSheet(
    repository: AppRepository,
    prefs: SharedPreferences,
    isVisible: Boolean,
    initialTab: Int = 0,
    onOpenFontPicker: (key: String, title: String) -> Unit,
    onOpenClockSettings: () -> Unit = {},
    onAddWidget: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit = {}
)  {
    val isDark = isSystemInDarkTheme()
    var sheetOpacity by remember { mutableFloatStateOf(1.0f) }
    val backgroundColor = (if (isDark) Color(0xFF121212) else Color(0xFFF8F9FA)).copy(alpha = sheetOpacity)
    val contentColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val topLimitPx = with(density) { 48.dp.toPx() }
    
    val offset = remember { Animatable(screenHeightPx) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var isClockViewEnabled by remember { mutableStateOf(prefs.getBoolean("clock_view_enabled", true)) }
    var showNowApps by remember { mutableStateOf(prefs.getBoolean("show_now_apps", true)) }
    var showDockBar by remember { mutableStateOf(prefs.getBoolean("show_dock_bar", true)) }
    var gesturesEnabled by remember { mutableStateOf(prefs.getBoolean("gestures_enabled", true)) }
    var quotesEnabled by remember { mutableStateOf(prefs.getBoolean("quotes_enabled", true)) }
    val context = LocalContext.current
    var topMode by remember { mutableStateOf(WidgetSlotModel.loadTopMode(context)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "clock_view_enabled" -> isClockViewEnabled = p.getBoolean(key, true)
                "show_now_apps"      -> showNowApps = p.getBoolean(key, true)
                "show_dock_bar"      -> showDockBar = p.getBoolean(key, true)
                "gestures_enabled"   -> gesturesEnabled = p.getBoolean(key, true)
                "quotes_enabled"     -> quotesEnabled = p.getBoolean(key, true)
                "top_section_mode"   -> topMode = WidgetSlotModel.loadTopMode(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            offset.animateTo(
                screenHeightPx * 0.05f, 
                spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
            )
        } else {
            offset.animateTo(screenHeightPx, tween(400, easing = FastOutSlowInEasing))
            onDismissFinished()
        }
    }
    
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    if (offset.value >= screenHeightPx && !isVisible) return

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - (offset.value / screenHeightPx)).coerceIn(0f, 0.4f)))
                .clickable { onDismiss() }
        )

        Surface(
            modifier = Modifier
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .fillMaxWidth()
                .height(config.screenHeightDp.dp - with(density) { offset.value.toDp() }),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = backgroundColor,
            shadowElevation = 0.dp // Modern flat look with better background color
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HeaderHandle(
                    contentColor = contentColor,
                    sheetOpacity = sheetOpacity,
                    onOpacityChange = {
                        sheetOpacity = it
                        prefs.edit().putFloat("sheet_transparency", it).apply()
                    },
                    onDrag = { dragAmount ->
                        val newOffset = (offset.value + dragAmount).coerceIn(topLimitPx, screenHeightPx)
                        scope.launch { offset.snapTo(newOffset) }
                    },
                    onDragEnd = {
                        if (offset.value > screenHeightPx * 0.6f) {
                            scope.launch {
                                offset.animateTo(screenHeightPx, tween(300))
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                offset.animateTo(screenHeightPx * 0.05f, spring(dampingRatio = 0.8f))
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = contentColor,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.05f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = contentColor)
                    }
                }

                val tabs = mutableListOf<Pair<String, ImageVector>>()
                tabs.add("Layouts" to Icons.Default.Dashboard)
                tabs.add("Widgets" to Icons.Default.Widgets)
                if (isClockViewEnabled) tabs.add("Clock" to Icons.Default.WatchLater)
                if (showNowApps) tabs.add("Pinned" to Icons.Default.PushPin)
                tabs.add("Drawer" to Icons.Default.GridView)
                if (gesturesEnabled) tabs.add("Gestures" to Icons.Default.TouchApp)
                if (showDockBar) tabs.add("Dock" to Icons.Default.ViewAgenda)
                if (quotesEnabled) tabs.add("Quotes" to Icons.Default.FormatQuote)

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    edgePadding = 24.dp,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .height(3.dp)
                                    .padding(horizontal = 12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                        }
                    },
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        val selected = selectedTab == index
                        val color = if (selected) Color(0xFF4CAF50) else contentColor.copy(alpha = 0.4f)

                        Tab(
                            selected = selected,
                            onClick = { selectedTab = index },
                            modifier = Modifier.padding(bottom = 8.dp),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = color,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    val currentTabTitle = tabs.getOrNull(selectedTab)?.first
                    when (currentTabTitle) {
                        "Layouts"  -> LayoutsTab(prefs, contentColor)
                        "Widgets"  -> WidgetsTab(
                            prefs        = prefs,
                            topMode      = topMode,
                            contentColor = contentColor,
                            onModeChanged = { newMode ->
                                topMode = newMode
                                WidgetSlotModel.saveTopMode(context, newMode)
                                prefs.edit().putString("top_section_mode", newMode).apply()
                            },
                            onAddWidget = onAddWidget
                        )
                        "Clock"    -> ClockTab(prefs, contentColor, onOpenClockSettings)
                        "Pinned"   -> PinnedAppsTab(repository, prefs, contentColor, backgroundColor, onOpenFontPicker)
                        "Drawer"   -> DrawerTab(prefs, contentColor, onOpenFontPicker)
                        "Gestures" -> GesturesTab(repository, prefs, contentColor)
                        "Dock"     -> DockTab(repository, prefs, contentColor, backgroundColor, onOpenFontPicker)
                        "Quotes"   -> QuotesTab(prefs, contentColor)
                    }
                }
            }
        }
    }
}

// All UI components and tabs have been moved to UnifiedSettingsLayout.kt
