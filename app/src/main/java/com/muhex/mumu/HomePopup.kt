package com.muhex.mumu

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Actions that can be performed from the Home Popup
 */
sealed class HomePopupAction {
    object Wallpaper : HomePopupAction()
    object Muhex : HomePopupAction()
    object AddWidget : HomePopupAction()
    object Settings : HomePopupAction()
    object Dock : HomePopupAction()
    object Dismiss : HomePopupAction()
}

/**
 * HomePopup: A premium Compose-based customization menu.
 * Now a vertical list with dragging support for consistency.
 */
@Composable
fun HomePopup(
    isVisible: Boolean,
    onAction: (HomePopupAction) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color.Black else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val iconBackground = contentColor.copy(alpha = 0.05f)

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    var verticalOffset by remember { mutableFloatStateOf(0f) }

    // Reset offset when visibility changes to hidden
    LaunchedEffect(isVisible) {
        if (!isVisible) verticalOffset = 0f
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }) + scaleIn(initialScale = 0.95f),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }) + scaleOut(targetScale = 0.95f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onAction(HomePopupAction.Dismiss) })
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 64.dp)
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(0.8f)
                    .offset { IntOffset(0, verticalOffset.roundToInt()) }
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(32.dp),
                color = backgroundColor,
                contentColor = contentColor,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header & Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    verticalOffset = (verticalOffset + dragAmount).coerceIn(-screenHeightPx * 0.4f, screenHeightPx * 0.2f)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = { verticalOffset = 0f })
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(contentColor.copy(alpha = 0.2f))
                        )
                    }

                    Text(
                        text = "Home Customization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Vertical List of Actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HomeListItem(
                            icon = Icons.Default.Image,
                            label = "Wallpaper",
                            description = "Change home screen background",
                            contentColor = contentColor,
                            iconBackground = iconBackground,
                            onClick = { onAction(HomePopupAction.Wallpaper) }
                        )
                        HomeListItem(
                            icon = Icons.Default.Palette,
                            label = "Muhex Customizer",
                            description = "Clock, theme and UI effects",
                            contentColor = contentColor,
                            iconBackground = iconBackground,
                            onClick = { onAction(HomePopupAction.Muhex) }
                        )
                        HomeListItem(
                            icon = Icons.Default.Widgets,
                            label = "Add Widget",
                            description = "System and custom widgets",
                            contentColor = contentColor,
                            iconBackground = iconBackground,
                            onClick = { onAction(HomePopupAction.AddWidget) }
                        )
                        HomeListItem(
                            icon = Icons.Default.Layers,
                            label = "Dock Settings",
                            description = "Bottom bar layout and items",
                            contentColor = contentColor,
                            iconBackground = iconBackground,
                            onClick = { onAction(HomePopupAction.Dock) }
                        )
                        HomeListItem(
                            icon = Icons.Default.Settings,
                            label = "Launcher Settings",
                            description = "Grid, icons and system options",
                            contentColor = contentColor,
                            iconBackground = iconBackground,
                            onClick = { onAction(HomePopupAction.Settings) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeListItem(
    icon: ImageVector,
    label: String,
    description: String,
    contentColor: Color,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
