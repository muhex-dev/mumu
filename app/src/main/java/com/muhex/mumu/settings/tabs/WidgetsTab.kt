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
import com.muhex.mumu.settings.SectionHeader

@Composable
fun WidgetsTab(
    prefs: SharedPreferences,
    topMode: String,
    contentColor: Color,
    onModeChanged: (String) -> Unit,
    onAddWidget: (() -> Unit)?
) {
    val isWidgetMode = topMode == WidgetSlotModel.MODE_WIDGETS
    val accentColor = Color(0xFF4CAF50)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader("Display Mode")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeCard(
                    title = "Clock",
                    subtitle = "Minimalist focus",
                    icon = Icons.Default.WatchLater,
                    isSelected = !isWidgetMode,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChanged(WidgetSlotModel.MODE_CLOCK) }
                )

                ModeCard(
                    title = "Widgets",
                    subtitle = "System widgets",
                    icon = Icons.Default.Widgets,
                    isSelected = isWidgetMode,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChanged(WidgetSlotModel.MODE_WIDGETS) }
                )
            }
        }

        if (isWidgetMode && onAddWidget != null) {
            item {
                Button(
                    onClick = onAddWidget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    )
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Widget", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
