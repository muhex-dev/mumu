package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WidgetPickerActivity : AppCompatActivity() {

    private lateinit var widgetHostManager: WidgetHostManager
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: ComponentName? = null

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

        if (pendingWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            launchPickStep()
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
                // Check widget info regardless of resultCode —
                // some devices return CANCELED even on success
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
                        "Widget permission denied. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    cancelAndFinish()
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
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_WIDGET_ID, widgetId)
        })
        finish()
    }

    private fun cancelAndFinish() {
        if (WidgetSlotModel.isValidWidgetId(pendingWidgetId)) {
            widgetHostManager.deleteWidgetId(pendingWidgetId)
        }
        setResult(RESULT_CANCELED)
        finish()
    }
}