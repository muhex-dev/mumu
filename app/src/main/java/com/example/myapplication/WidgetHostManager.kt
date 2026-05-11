package com.example.myapplication

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.Log

/**
 * WidgetHostManager — the engine that talks to Android's AppWidget system.
 *
 * Responsibilities:
 *  - Allocate and release widget IDs
 *  - Create live AppWidgetHostViews for rendering
 *  - Start/stop listening for widget updates (tied to lifecycle)
 *  - Save and load the slot list via WidgetSlotModel
 *  - Query the system for available widget providers
 */
class WidgetHostManager(private val context: Context) {

    companion object {
        const val HOST_ID        = 1024   // unique int identifying this host
        const val REQUEST_PICK   = 2001   // used in WidgetPickerActivity
        const val REQUEST_BIND   = 2002
        const val REQUEST_CONFIG = 2003
        private const val TAG    = "WidgetHostManager"
    }

    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    private val appWidgetHost = AppWidgetHost(context.applicationContext, HOST_ID)

    // ── Lifecycle ────────────────────────────────────────────────────────────
    // Call startListening() in onStart() and stopListening() in onStop()
    // of whatever Fragment or Activity owns this manager.
    // Without this, widgets won't update (clock won't tick, weather won't refresh).

    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "stopListening failed: ${e.message}")
        }
    }

    // ── Widget ID management ─────────────────────────────────────────────────
    // Android assigns each placed widget a unique integer ID.
    // You must allocate one before binding, and delete it when removing.

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun deleteWidgetId(widgetId: Int) {
        if (WidgetSlotModel.isValidWidgetId(widgetId)) {
            try {
                appWidgetHost.deleteAppWidgetId(widgetId)
            } catch (e: Exception) {
                Log.e(TAG, "deleteWidgetId failed for $widgetId: ${e.message}")
            }
        }
    }

    // ── View creation ────────────────────────────────────────────────────────
    // Creates the live, interactive View for a widget.
    // Must be called after the widget is bound (has a valid providerInfo).

    fun createHostView(
        widgetId: Int,
        providerInfo: AppWidgetProviderInfo
    ): AppWidgetHostView {
        return appWidgetHost.createView(
            context.applicationContext,
            widgetId,
            providerInfo
        ).also { view ->
            view.setAppWidget(widgetId, providerInfo)
        }
    }
// ── Size reporting ───────────────────────────────────────────────────────────
// Tells the widget exactly how big it is so it picks the right layout.
// Call this every time the container size changes.
// paddingDp — subtract padding from each side before reporting

    fun updateWidgetSize(
        widgetId: Int,
        widthPx: Int,
        heightPx: Int,
        paddingDp: Int = 8
    ) {
        if (!WidgetSlotModel.isValidWidgetId(widgetId)) return
        try {
            val density = context.resources.displayMetrics.density
            val paddingPx = (paddingDp * density).toInt()
            val netW = (widthPx  - paddingPx * 2).coerceAtLeast(40)
            val netH = (heightPx - paddingPx * 2).coerceAtLeast(40)

            // Convert px to dp for the API
            val netWDp = (netW / density).toInt()
            val netHDp = (netH / density).toInt()

            val options = android.os.Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,  netWDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,  netWDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, netHDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, netHDp)
            }
            appWidgetManager.updateAppWidgetOptions(widgetId, options)
        } catch (e: Exception) {
            Log.e(TAG, "updateWidgetSize failed for $widgetId: ${e.message}")
        }
    }

    // Reports size for all currently loaded slots at once
    fun updateAllWidgetSizes(widthPx: Int, heightPx: Int) {
        loadSlots().forEach { slot ->
            updateWidgetSize(slot.widgetId, widthPx, heightPx)
        }
    }
    // ── Provider info ────────────────────────────────────────────────────────
    // AppWidgetProviderInfo describes a widget — its name, preview,
    // min size, which component handles it.

    fun getProviderInfo(widgetId: Int): AppWidgetProviderInfo? {
        return if (WidgetSlotModel.isValidWidgetId(widgetId)) {
            appWidgetManager.getAppWidgetInfo(widgetId)
        } else null
    }

    fun getAllProviders(): List<AppWidgetProviderInfo> {
        return try {
            appWidgetManager.installedProviders
        } catch (e: Exception) {
            Log.e(TAG, "getAllProviders failed: ${e.message}")
            emptyList()
        }
    }

    // ── Slot management ──────────────────────────────────────────────────────
    // Thin wrappers around WidgetSlotModel persistence.

    fun loadSlots(): List<WidgetSlotModel> {
        return WidgetSlotModel.loadSlots(context)
    }

    fun saveSlots(slots: List<WidgetSlotModel>) {
        WidgetSlotModel.saveSlots(context, slots)
    }

    fun addSlot(widgetId: Int): List<WidgetSlotModel> {
        val current = loadSlots().toMutableList()
        val newSlot = WidgetSlotModel(
            widgetId = widgetId,
            heightDp = 180,
            position = current.size   // append to end
        )
        current.add(newSlot)
        saveSlots(current)
        return current
    }

    fun removeSlot(widgetId: Int): List<WidgetSlotModel> {
        // Delete the Android widget ID first to free system resources
        deleteWidgetId(widgetId)
        // Then remove from our saved list
        val updated = loadSlots()
            .filter { it.widgetId != widgetId }
            .mapIndexed { index, slot -> slot.copy(position = index) }
        saveSlots(updated)
        return updated
    }

    fun updateSlotHeight(widgetId: Int, newHeightDp: Int): List<WidgetSlotModel> {
        val updated = loadSlots().map { slot ->
            if (slot.widgetId == widgetId) slot.copy(heightDp = newHeightDp)
            else slot
        }
        saveSlots(updated)
        return updated
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────
    // Call this if the user resets everything or uninstalls widgets.

    fun deleteAllSlots() {
        loadSlots().forEach { deleteWidgetId(it.widgetId) }
        saveSlots(emptyList())
    }

    // ── Validation ───────────────────────────────────────────────────────────
    // Prune any saved slots whose widget is no longer installed.
    // Call once on startup.

    fun pruneOrphanedSlots(): List<WidgetSlotModel> {
        val valid = loadSlots().filter { slot ->
            val info = getProviderInfo(slot.widgetId)
            val isAlive = info != null
            if (!isAlive) {
                Log.d(TAG, "Pruning orphaned widget id=${slot.widgetId}")
                deleteWidgetId(slot.widgetId)
            }
            isAlive
        }.mapIndexed { index, slot -> slot.copy(position = index) }
        saveSlots(valid)
        return valid
    }
}