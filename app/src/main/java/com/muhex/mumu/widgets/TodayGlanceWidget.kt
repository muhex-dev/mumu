package com.muhex.mumu.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.muhex.mumu.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            TodayWidgetContent()
        }
    }

    @Composable
    private fun TodayWidgetContent() {
        val size = LocalSize.current
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("MMMM dd", Locale.getDefault()).format(Date())
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(4.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Top Section: Box + Hello Welcome Back
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Hollow square icon
                Box(modifier = GlanceModifier.width(42.dp).height(42.dp)) {
                    // Top line
                    Box(modifier = GlanceModifier.fillMaxSize().padding(bottom = 33.dp).background(Color.White)) {}
                    // Bottom line
                    Box(modifier = GlanceModifier.fillMaxSize().padding(top = 33.dp).background(Color.White)) {}
                    // Start line
                    Box(modifier = GlanceModifier.fillMaxSize().padding(end = 33.dp).background(Color.White)) {}
                    // End line
                    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 33.dp).background(Color.White)) {}
                }

                Spacer(GlanceModifier.width(12.dp))

                Column {
                    Text(
                        text = "HELLO",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "WELCOME BACK",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(GlanceModifier.height(if (size.height > 100.dp) 10.dp else 5.dp))

            // Middle Section: TODAY
            Text(
                text = "TODAY",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 78.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(2.dp))

            // Quote Section
            Column {
                Text(
                    text = "They call it justice for the poor...and 'influence' for the rich.",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = "In this world, money doesn't erase sin...",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = "it just hires silence.",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp
                    )
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            // Bottom Section: Time - Date. Day
            Text(
                text = "$currentTime - $currentDate. $currentDay",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayGlanceWidget()
}
