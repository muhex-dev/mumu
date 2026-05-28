package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build

class HomeReceiverManager(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val prefListener: SharedPreferences.OnSharedPreferenceChangeListener,
    private val onBatteryChanged: (Int) -> Unit,
    private val onWidgetAdded: (widgetId: Int, explicitIndex: Int) -> Unit
) {

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level != -1 && scale != -1) {
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                onBatteryChanged(batteryPct)
            }
        }
    }

    private val widgetAddedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val widgetId = intent.getIntExtra(
                WidgetPickerActivity.EXTRA_WIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val explicitIndex = intent.getIntExtra("extra_widget_index", -1)
            onWidgetAdded(widgetId, explicitIndex)
        }
    }

    fun register() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val widgetFilter = IntentFilter(WidgetPickerActivity.ACTION_WIDGET_ADDED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(widgetAddedReceiver, widgetFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(widgetAddedReceiver, widgetFilter)
        }
    }

    fun unregister() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        try {
            context.unregisterReceiver(widgetAddedReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }
}
