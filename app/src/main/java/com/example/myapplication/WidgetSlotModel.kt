package com.example.myapplication

import android.appwidget.AppWidgetManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents one saved widget slot in the stack.
 * Each slot maps to one real Android widget the user has added.
 */
data class WidgetSlotModel(
    val widgetId: Int,         // Android's AppWidget ID — the key to everything
    val heightDp: Int = 180,   // user-chosen height in dp, default 180
    val position: Int = 0      // order in the stack, 0 = top
) {

    // ── JSON serialization (for SharedPreferences storage) ──────────

    fun toJson(): JSONObject = JSONObject().apply {
        put("widgetId",  widgetId)
        put("heightDp",  heightDp)
        put("position",  position)
    }

    companion object {

        fun fromJson(json: JSONObject): WidgetSlotModel? {
            return try {
                WidgetSlotModel(
                    widgetId = json.getInt("widgetId"),
                    heightDp = json.optInt("heightDp", 180),
                    position = json.optInt("position", 0)
                )
            } catch (e: Exception) {
                null  // silently drop corrupt entries
            }
        }

        // ── Save & load the full list ───────────────────────────────

        private const val PREFS_NAME = "widget_slots_prefs"
        private const val KEY_SLOTS  = "widget_slots_json"

        fun saveSlots(context: android.content.Context, slots: List<WidgetSlotModel>) {
            val array = JSONArray()
            slots.forEachIndexed { index, slot ->
                // Always write the current index as position before saving
                array.put(slot.copy(position = index).toJson())
            }
            context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SLOTS, array.toString())
                .apply()
        }

        fun loadSlots(context: android.content.Context): List<WidgetSlotModel> {
            val raw = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_SLOTS, null) ?: return emptyList()

            return try {
                val array = JSONArray(raw)
                (0 until array.length())
                    .mapNotNull { fromJson(array.getJSONObject(it)) }
                    .sortedBy { it.position }
            } catch (e: Exception) {
                emptyList()  // return empty on any parse failure
            }
        }

        // ── Top section mode (clock vs widget) ─────────────────────

        private const val KEY_TOP_MODE = "top_section_mode"
        const val MODE_CLOCK   = "clock"
        const val MODE_WIDGETS = "widgets"

        fun saveTopMode(context: android.content.Context, mode: String) {
            context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOP_MODE, mode)
                .apply()
        }

        fun loadTopMode(context: android.content.Context): String {
            return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_TOP_MODE, MODE_CLOCK) ?: MODE_CLOCK
        }

        // ── Helper — check if a widget ID is valid ──────────────────

        fun isValidWidgetId(widgetId: Int): Boolean {
            return widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        }
        // ── Container geometry persistence ─────────────────────────────────────
        private const val KEY_TOP_PERCENT    = "widget_top_percent"
        private const val KEY_BOTTOM_PERCENT = "widget_bottom_percent"

        fun saveGeometry(
            context: android.content.Context,
            topPercent: Float,
            bottomPercent: Float
        ) {
            context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putFloat(KEY_TOP_PERCENT,    topPercent)
                .putFloat(KEY_BOTTOM_PERCENT, bottomPercent)
                .apply()
        }

        fun loadTopPercent(context: android.content.Context): Float =
            context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getFloat(KEY_TOP_PERCENT, 0.0f)

        fun loadBottomPercent(context: android.content.Context): Float =
            context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getFloat(KEY_BOTTOM_PERCENT, 0.4f)
    }
}