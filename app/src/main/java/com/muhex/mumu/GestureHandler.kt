package com.muhex.mumu

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class GestureHandler(
    private val context: Context,
    private val activity: FragmentActivity,
    private val repository: AppRepository,
    private val lifecycleOwner: LifecycleOwner
) {
    private var gestureDetector: GestureDetector? = null
    private val prefs = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    fun attachToView(
        root: View,
        prefs: SharedPreferences,
        onLongPress: () -> Unit,
        onOpenQuickMenu: () -> Unit,
        onOpenDrawer: (String, Boolean) -> Unit,
        onOpenHiddenApps: (String) -> Unit
    ) {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                handleGestureAction(
                    prefs.getString("gesture_double_tap", "lock_screen"),
                    "double_tap",
                    onOpenQuickMenu,
                    onOpenDrawer,
                    onOpenHiddenApps
                )
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                onLongPress()
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffY = e1.y - e2.y
                val diffX = e1.x - e2.x

                return if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > 100 && abs(velocityX) > 100) {
                        if (diffX > 0) handleGestureAction(prefs.getString("gesture_swipe_left", "none"), "swipe_left", onOpenQuickMenu, onOpenDrawer, onOpenHiddenApps)
                        else handleGestureAction(prefs.getString("gesture_swipe_right", "none"), "swipe_right", onOpenQuickMenu, onOpenDrawer, onOpenHiddenApps)
                        true
                    } else false
                } else {
                    if (abs(diffY) > 100 && abs(velocityY) > 100) {
                        if (diffY > 0) handleGestureAction(prefs.getString("gesture_swipe_up", "app_drawer"), "swipe_up", onOpenQuickMenu, onOpenDrawer, onOpenHiddenApps)
                        else handleGestureAction(prefs.getString("gesture_swipe_down", "notifications"), "swipe_down", onOpenQuickMenu, onOpenDrawer, onOpenHiddenApps)
                        true
                    } else false
                }
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        root.setOnTouchListener { v, event ->
            val handled = gestureDetector?.onTouchEvent(event) ?: false
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                v.performClick()
            }
            handled || true
        }
    }

    fun handleGestureAction(
        action: String?,
        gestureName: String,
        onOpenQuickMenu: () -> Unit,
        onOpenDrawer: (String, Boolean) -> Unit,
        onOpenHiddenApps: (String) -> Unit
    ) {
        val fullKey = when (gestureName) {
            "swipe_up" -> "gesture_swipe_up"
            "swipe_down" -> "gesture_swipe_down"
            "swipe_left" -> "gesture_swipe_left"
            "swipe_right" -> "gesture_swipe_right"
            "double_tap" -> "gesture_double_tap"
            else -> gestureName
        }

        when (action) {
            "none" -> {}
            "quick_menu" -> {
                onOpenQuickMenu()
            }
            "app_drawer" -> {
                onOpenDrawer(gestureName, false)
            }
            "hidden_apps" -> {
                onOpenHiddenApps(gestureName)
            }
            "notifications" -> {
                if (!ScreenLockService.expandNotifications()) {
                    Toast.makeText(context, "Enable Accessibility to expand notifications", Toast.LENGTH_SHORT).show()
                    openAccessibilitySettings()
                }
            }
            "quick_settings" -> {
                if (!ScreenLockService.expandQuickSettings()) {
                    Toast.makeText(context, "Enable Accessibility to expand quick settings", Toast.LENGTH_SHORT).show()
                    openAccessibilitySettings()
                }
            }
            "search" -> {
                onOpenDrawer(gestureName, true)
            }
            "assistant" -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(context, "Assistant not found", Toast.LENGTH_SHORT).show()
                }
            }
            "lock_screen" -> {
                if (!ScreenLockService.lock()) {
                    Toast.makeText(context, "Enable Accessibility to lock screen", Toast.LENGTH_SHORT).show()
                    openAccessibilitySettings()
                }
            }
            "settings" -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            "open_app" -> {
                val appId = prefs.getString("${fullKey}_app_id", "") ?: ""
                if (appId.isNotEmpty()) {
                    lifecycleOwner.lifecycleScope.launch {
                        val allApps = repository.getAllApps()
                        val app = allApps.find { repository.getAppUniqueId(it) == appId }
                        if (app != null) {
                            LaunchApp.launch(context, app)
                        } else {
                            Toast.makeText(context, "Assigned app not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "No app assigned for this gesture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
