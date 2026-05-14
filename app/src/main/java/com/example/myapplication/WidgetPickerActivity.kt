package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class WidgetPickerActivity : AppCompatActivity() {

    private lateinit var widgetHostManager: WidgetHostManager
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: ComponentName? = null

    private var allProviders by mutableStateOf<List<AppWidgetProviderInfo>>(emptyList())
    private var isShowingPicker by mutableStateOf(true)
    private var lastAddedProvider by mutableStateOf<ComponentName?>(null)

    companion object {
        private const val TAG = "WidgetPickerActivity"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
        private const val KEY_PENDING_ID = "pending_widget_id"
        private const val KEY_PENDING_PROVIDER = "pending_provider"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetHostManager = WidgetHostManager(this)

        pendingWidgetId = savedInstanceState?.getInt(
            KEY_PENDING_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        pendingProvider = savedInstanceState?.getParcelable(KEY_PENDING_PROVIDER)

        allProviders = widgetHostManager.getAllProviders()

        setContent {
            if (isShowingPicker) {
                WidgetPickerScreen(
                    providers = allProviders,
                    lastAddedProvider = lastAddedProvider,
                    onWidgetSelected = { info ->
                        selectWidget(info)
                    },
                    onDismiss = {
                        cancelAndFinish()
                    }
                )
            }
        }
    }

    private fun selectWidget(info: AppWidgetProviderInfo) {
        // isShowingPicker = false // Removed to keep picker open
        pendingWidgetId = widgetHostManager.allocateWidgetId()
        pendingProvider = info.provider

        Log.d(TAG, "Selecting widget: ${info.provider.flattenToShortString()} | ID: $pendingWidgetId")

        val manager = AppWidgetManager.getInstance(this)
        // Check if we already have "Always allow" permission
        val success = manager.bindAppWidgetIdIfAllowed(pendingWidgetId, info.provider)
        
        if (success) {
            Log.d(TAG, "Permission already granted, skipping dialog.")
            launchConfigureStep(pendingWidgetId, info)
        } else {
            Log.d(TAG, "Permission needed, showing system bind dialog.")
            launchBindStep(pendingWidgetId, info.provider)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING_ID, pendingWidgetId)
        outState.putParcelable(KEY_PENDING_PROVIDER, pendingProvider)
    }

    // ── STEP 1: System widget picker ─────────────────────────────────────────

    private fun launchPickStep() {
        try {
            pendingWidgetId = widgetHostManager.allocateWidgetId()
            Log.d(TAG, "Allocated widgetId=$pendingWidgetId")

            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
                putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, ArrayList())
                putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, ArrayList())
            }
            startActivityForResult(intent, WidgetHostManager.REQUEST_PICK)

        } catch (e: Exception) {
            Log.e(TAG, "Pick failed: ${e.message}")
            Toast.makeText(this, "Could not open widget picker", Toast.LENGTH_SHORT).show()
            cancelAndFinish()
        }
    }

    // ── STEP 2: Ask user permission to bind via system dialog ────────────────
    // This is the correct approach for non-system launchers.
    // Android shows its own "Allow MAME to add this widget?" dialog.

    private fun launchBindStep(widgetId: Int, provider: ComponentName) {
        pendingProvider = provider
        try {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                // Passing an empty options bundle can help on some Android versions
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, Bundle())
            }
            startActivityForResult(bindIntent, WidgetHostManager.REQUEST_BIND)
        } catch (e: Exception) {
            Log.e(TAG, "Bind intent failed: ${e.message}")
            // Check if it bound anyway
            val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
            if (info != null) {
                launchConfigureStep(widgetId, info)
            } else {
                Toast.makeText(this, "Could not bind widget", Toast.LENGTH_SHORT).show()
                cancelAndFinish()
            }
        }
    }

    // ── STEP 3: Widget configuration ─────────────────────────────────────────

    private fun launchConfigureStep(widgetId: Int, info: AppWidgetProviderInfo) {
        val configComponent = info.configure
        if (configComponent == null) {
            returnSuccess(widgetId)
            return
        }
        try {
            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configComponent
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            startActivityForResult(configIntent, WidgetHostManager.REQUEST_CONFIG)
        } catch (e: Exception) {
            Log.e(TAG, "Config failed: ${e.message}")
            returnSuccess(widgetId)
        }
    }

    // ── Result handler ───────────────────────────────────────────────────────

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult request=$requestCode result=$resultCode")

        when (requestCode) {

            WidgetHostManager.REQUEST_PICK -> {
                if (resultCode == RESULT_OK && data != null) {
                    val widgetId = data.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                    )
                    if (!WidgetSlotModel.isValidWidgetId(widgetId)) {
                        cancelAndFinish(); return
                    }

                    pendingWidgetId = widgetId
                    val manager = AppWidgetManager.getInstance(this)
                    val info = manager.getAppWidgetInfo(widgetId)

                    if (info != null) {
                        // Already bound — skip to configure
                        launchConfigureStep(widgetId, info)
                    } else {
                        // Need to bind — get provider from intent extras
                        @Suppress("DEPRECATION")
                        val provider = data.getParcelableExtra<ComponentName>(
                            AppWidgetManager.EXTRA_APPWIDGET_PROVIDER
                        )
                        if (provider != null) {
                            launchBindStep(widgetId, provider)
                        } else {
                            // Try matching from installed providers
                            val matched = manager.installedProviders.firstOrNull { p ->
                                p.provider.packageName == data.getStringExtra("appWidgetPackageName")
                            }
                            if (matched != null) {
                                launchBindStep(widgetId, matched.provider)
                            } else {
                                Log.e(TAG, "No provider found")
                                cancelAndFinish()
                            }
                        }
                    }
                } else {
                    cancelAndFinish()
                }
            }

            WidgetHostManager.REQUEST_BIND -> {
                val widgetId = data?.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId
                ) ?: pendingWidgetId

                val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
                if (info != null) {
                    launchConfigureStep(widgetId, info)
                } else {
                    Log.e(TAG, "Bind failed — widget info still null")
                    Toast.makeText(
                        this,
                        "Widget permission denied or failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (WidgetSlotModel.isValidWidgetId(widgetId)) {
                        widgetHostManager.deleteWidgetId(widgetId)
                    }
                    pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                }
            }

            WidgetHostManager.REQUEST_CONFIG -> {
                val widgetId = data?.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId
                ) ?: pendingWidgetId
                returnSuccess(widgetId)
            }
        }
    }

    // ── Outcomes ─────────────────────────────────────────────────────────────

    private fun returnSuccess(widgetId: Int) {
        Log.d(TAG, "Success widgetId=$widgetId")
        
        // Reset pending state so it doesn't get deleted on close
        if (pendingWidgetId == widgetId) {
            lastAddedProvider = pendingProvider
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            pendingProvider = null
        }

        // Notify HomeFragment immediately
        val broadcastIntent = Intent("com.example.myapplication.WIDGET_ADDED").apply {
            setPackage(packageName)
            putExtra(EXTRA_WIDGET_ID, widgetId)
        }
        sendBroadcast(broadcastIntent)
        
        Toast.makeText(this, "Widget added to home screen", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAndFinish() {
        if (WidgetSlotModel.isValidWidgetId(pendingWidgetId)) {
            widgetHostManager.deleteWidgetId(pendingWidgetId)
        }
        setResult(RESULT_CANCELED)
        finish()
    }
}