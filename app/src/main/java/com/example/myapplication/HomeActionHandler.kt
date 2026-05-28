package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity

class HomeActionHandler(
    private val context: Context,
    private val activity: FragmentActivity,
    private val overlayController: HomeOverlayController,
    private val onOpenDrawer: (type: String, focusSearch: Boolean) -> Unit,
    private val onOpenWallpaper: () -> Unit
) {
    private var isFlashlightOn = false

    fun handleHomePopupAction(action: HomePopupAction) {
        when (action) {
            HomePopupAction.Wallpaper -> {
                overlayController.dismissComposeOverlays()
                onOpenWallpaper()
            }
            HomePopupAction.Muhex -> overlayController.showMuhexSettings()
            HomePopupAction.Dock -> overlayController.showUnifiedSettings(tab = 5)
            HomePopupAction.PinnedApps -> overlayController.showUnifiedSettings(tab = 2)
            HomePopupAction.Gestures -> overlayController.showUnifiedSettings(tab = 4)
            HomePopupAction.Settings -> overlayController.showUnifiedSettings(tab = 0)
            HomePopupAction.Dismiss -> overlayController.dismissComposeOverlays()
        }
    }

    fun handleQuickMenuAction(action: QuickMenuAction) {
        overlayController.dismissComposeOverlays()
        when (action) {
            is QuickMenuAction.Flashlight -> toggleFlashlight()
            is QuickMenuAction.Settings -> activity.startActivity(Intent(Settings.ACTION_SETTINGS))
            is QuickMenuAction.Apps -> onOpenDrawer("quick_menu", false)
            is QuickMenuAction.Wifi -> activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            is QuickMenuAction.Bluetooth -> activity.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            is QuickMenuAction.Calculator -> launchCommonApp("calculator")
            is QuickMenuAction.Calendar -> launchCommonApp("calendar")
            is QuickMenuAction.Search -> onOpenDrawer("search", true)
            is QuickMenuAction.Files -> launchCommonApp("files")
            is QuickMenuAction.Browser -> launchCommonApp("browser")
            else -> {}
        }
    }

    fun toggleFlashlight() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            Toast.makeText(context, if (isFlashlightOn) "Flashlight ON" else "Flashlight OFF", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCommonApp(type: String) {
        val intent = when (type) {
            "calculator" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
            "calendar" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
            "files" -> Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
            "browser" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
            else -> null
        }
        intent?.let {
            try {
                activity.startActivity(it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
