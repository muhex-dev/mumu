package com.muhex.mumu.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.ColorFilter
import androidx.glance.GlanceTheme
import com.muhex.mumu.MainActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MumuGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MumuWidgetContent(context)
        }
    }

    @Composable
    private fun MumuWidgetContent(context: Context) {
        val currentTime = SimpleDateFormat("h mm a", Locale.getDefault()).format(Date()).lowercase()
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        val suffix = when (dayOfMonth) {
            1, 21, 31 -> "st"
            2, 22 -> "nd"
            3, 23 -> "rd"
            else -> "th"
        }
        val currentDate = "$monthName $dayOfMonth$suffix"
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).uppercase()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(12.dp,4.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Top Section: Time & Date with vertical bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier
                        .width(5.dp)
                        .height(28.dp)
                        .background(Color.White)
                ) {}
                Spacer(GlanceModifier.width(4.dp))
                Column {
                    Text(
                        text = currentTime,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = currentDate,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(GlanceModifier.height(14.dp))

            // Greeting Section
            Column {
                Text(
                    text = "HAVE",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily("adil_regula")
                    )
                )
                
                Box(
                    modifier = GlanceModifier
                        .background(Color.White)
                        .padding(start = 2.dp, end = 4.dp)
                ) {
                    Text(
                        text = "A NICE",
                        style = TextStyle(
                            color = ColorProvider(Color.Black),
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily("adil_regula")
                        )
                    )
                }

                Text(
                    text = currentDay,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily("adil_regula")
                    )
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            // Footer
            Column {
                Box(modifier = GlanceModifier.width(40.dp).height(4.dp).background(Color.White)) {}
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "Remember you asked for growth.\nDon't be surprised\nwhen life challenges you",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
    }

    companion object {
        fun forceUpdateAll(context: Context) {
            MainScope().launch {
                MumuGlanceWidget().updateAll(context)
            }
        }
    }
}

class MumuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MumuGlanceWidget()
}
