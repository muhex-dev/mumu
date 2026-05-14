package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.util.Log

/**
 * WidgetPinReceiver handles requests from other apps to "Pin" a widget to the home screen.
 * This is the modern way (Android 8.0+) to allow external apps to add widgets to your launcher.
 */
class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("WidgetPinReceiver", "onReceive: action=$action")

        // Handle the modern PinItemRequest (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pinItemRequest = intent.getParcelableExtra<LauncherApps.PinItemRequest>(
                LauncherApps.EXTRA_PIN_ITEM_REQUEST
            )
            if (pinItemRequest != null && pinItemRequest.isValid) {
                if (pinItemRequest.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET) {
                    handleWidgetPinRequest(context, pinItemRequest)
                    return
                }
            }
        }

        // Fallback for older style or specific internal broadcasts
        if (action == "android.appwidget.action.CONFIRM_PIN_APPWIDGET") {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, 
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (WidgetSlotModel.isValidWidgetId(widgetId)) {
                completeWidgetAddition(context, widgetId)
            }
        }
    }

    private fun handleWidgetPinRequest(context: Context, request: LauncherApps.PinItemRequest) {
        val info = request.getAppWidgetProviderInfo(context) ?: return
        val widgetHostManager = WidgetHostManager(context)
        
        // For external pin requests, the launcher must allocate a new ID and bind it
        val widgetId = widgetHostManager.allocateWidgetId()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        val success = appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, info.provider)
        if (success) {
            // Accepting the request tells the system the launcher has handled it
            val accepted = request.accept()
            if (accepted) {
                completeWidgetAddition(context, widgetId)
            } else {
                Log.e("WidgetPinReceiver", "Failed to accept pin request even after binding.")
                widgetHostManager.deleteWidgetId(widgetId)
            }
        } else {
            Log.e("WidgetPinReceiver", "Failed to bind widget for pin request. Launcher might not have BIND_APPWIDGET permission.")
            widgetHostManager.deleteWidgetId(widgetId)
        }
    }

    private fun completeWidgetAddition(context: Context, widgetId: Int) {
        // 1. Add to persistent storage
        val widgetHostManager = WidgetHostManager(context)
        val newIndex = widgetHostManager.addSlot(widgetId)
        
        // 2. Notify HomeFragment to update UI and scroll
        val broadcastIntent = Intent("com.example.myapplication.WIDGET_ADDED").apply {
            setPackage(context.packageName)
            putExtra(WidgetPickerActivity.EXTRA_WIDGET_ID, widgetId)
            putExtra("extra_widget_index", newIndex)
        }
        context.sendBroadcast(broadcastIntent)
        
        Log.d("WidgetPinReceiver", "Widget $widgetId successfully processed and added at index $newIndex")
    }
}
