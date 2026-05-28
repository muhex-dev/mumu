package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * WidgetPickerActivity: Handles the selection, binding, and configuration of App Widgets.
 * Modernized to use Activity Result API and structured flow.
 */
class WidgetPickerActivity : AppCompatActivity() {

    // region Properties & State
    private lateinit var widgetHostManager: WidgetHostManager
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(this) }

    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: ComponentName? = null

    // Compose State
    private var allProviders by mutableStateOf<List<AppWidgetProviderInfo>>(emptyList())
    private var addedProviders by mutableStateOf<Set<ComponentName>>(emptySet())
    private var successCount by mutableStateOf(0)
    // endregion

    // region Activity Result Launchers
    private val bindWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleBindResult(result.resultCode, result.data)
    }

    private val configureWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleConfigureResult(result.resultCode, result.data)
    }
    // endregion

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetHostManager = WidgetHostManager(this)

        restoreState(savedInstanceState)
        loadProviders()

        setContent {
            WidgetPickerScreen(
                providers = allProviders,
                addedProviders = addedProviders,
                successCount = successCount,
                onWidgetSelected = { selectWidget(it) },
                onDismiss = { cancelAndFinish() }
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING_ID, pendingWidgetId)
        outState.putParcelable(KEY_PENDING_PROVIDER, pendingProvider)
    }
    // endregion

    // region Initialization
    private fun restoreState(savedInstanceState: Bundle?) {
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        pendingProvider = savedInstanceState?.getParcelable(KEY_PENDING_PROVIDER)
    }

    private fun loadProviders() {
        allProviders = widgetHostManager.getAllProviders()
        refreshAddedProviders()
    }

    private fun refreshAddedProviders() {
        val slots = widgetHostManager.loadSlots()
        addedProviders = slots.mapNotNull {
            widgetHostManager.getProviderInfo(it.widgetId)?.provider
        }.toSet()
    }
    // endregion

    // region Widget Selection Flow
    private fun selectWidget(info: AppWidgetProviderInfo) {
        pendingWidgetId = widgetHostManager.allocateWidgetId()
        pendingProvider = info.provider

        Log.d(TAG, "Selecting widget: ${info.provider.flattenToShortString()} | ID: $pendingWidgetId")

        // Try to bind directly if we already have permission
        val success = appWidgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId, info.provider)
        if (success) {
            launchConfigureStep(pendingWidgetId, info)
        } else {
            launchBindStep(pendingWidgetId, info.provider)
        }
    }

    /**
     * Step 2: Request permission to bind the widget via system dialog.
     */
    private fun launchBindStep(widgetId: Int, provider: ComponentName) {
        try {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            }
            bindWidgetLauncher.launch(bindIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Bind launch failed: ${e.message}")
            // Fallback: check if it bound anyway
            val info = appWidgetManager.getAppWidgetInfo(widgetId)
            if (info != null) launchConfigureStep(widgetId, info)
            else handleError("Could not bind widget")
        }
    }

    /**
     * Step 3: Launch widget configuration activity if required.
     */
    private fun launchConfigureStep(widgetId: Int, info: AppWidgetProviderInfo) {
        val configComponent = info.configure
        if (configComponent == null) {
            notifySuccess(widgetId)
            return
        }

        try {
            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configComponent
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            configureWidgetLauncher.launch(configIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Configure launch failed: ${e.message}")
            notifySuccess(widgetId) // Proceed anyway, widget is bound
        }
    }
    // endregion

    // region Result Handlers
    private fun handleBindResult(resultCode: Int, data: Intent?) {
        val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        val info = appWidgetManager.getAppWidgetInfo(widgetId)

        if (resultCode == RESULT_OK && info != null) {
            launchConfigureStep(widgetId, info)
        } else {
            Log.e(TAG, "Bind failed. Result: $resultCode")
            cleanupPendingWidget(widgetId)
            handleError("Widget permission denied.")
        }
    }

    private fun handleConfigureResult(resultCode: Int, data: Intent?) {
        val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        if (resultCode == RESULT_OK) {
            notifySuccess(widgetId)
        } else {
            // User cancelled config, usually we should delete the widget ID
            cleanupPendingWidget(widgetId)
        }
    }
    // endregion

    // region Outcomes
    private fun notifySuccess(widgetId: Int) {
        Log.d(TAG, "Successfully added widget ID: $widgetId")
        if (pendingWidgetId == widgetId) {
            successCount++
            clearPendingState()
            refreshAddedProviders()
        }

        // Notify HomeFragment/System
        sendBroadcast(Intent(ACTION_WIDGET_ADDED).apply {
            setPackage(packageName)
            putExtra(EXTRA_WIDGET_ID, widgetId)
        })

        Toast.makeText(this, "Widget added", Toast.LENGTH_SHORT).show()
    }

    private fun handleError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun cleanupPendingWidget(widgetId: Int) {
        if (WidgetSlotModel.isValidWidgetId(widgetId)) {
            widgetHostManager.deleteWidgetId(widgetId)
        }
        if (pendingWidgetId == widgetId) clearPendingState()
    }

    private fun clearPendingState() {
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingProvider = null
    }

    private fun cancelAndFinish() {
        cleanupPendingWidget(pendingWidgetId)
        setResult(RESULT_CANCELED)
        finish()
    }
    // endregion

    companion object {
        private const val TAG = "WidgetPickerActivity"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
        const val ACTION_WIDGET_ADDED = "com.example.myapplication.WIDGET_ADDED"
        private const val KEY_PENDING_ID = "pending_widget_id"
        private const val KEY_PENDING_PROVIDER = "pending_provider"
    }
}
