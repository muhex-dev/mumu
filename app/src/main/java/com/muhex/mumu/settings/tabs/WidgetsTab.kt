package com.muhex.mumu.settings.tabs

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhex.mumu.WidgetSlotModel
import com.muhex.mumu.settings.ModeCard

@Composable
fun WidgetsTab(
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
                    shape = RoundedCornerShape(16.dp),
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
                        .clip(RoundedCornerShape(14.dp))
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
