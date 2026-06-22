package com.muhex.mumu

import android.view.View

class HomeOverlayController(
    private val viewModel: HomeViewModel,
    private val getComposeView: () -> View
) {

    fun showHomePopup() {
        viewModel.isHomePopupVisible = true
        getComposeView().visibility = View.VISIBLE
    }

    fun showQuickMenu() {
        viewModel.isQuickMenuVisible = true
        getComposeView().visibility = View.VISIBLE
    }

    fun showMuhexSettings() {
        viewModel.isHomePopupVisible = false
        viewModel.isMuhexSettingsVisible = true
        getComposeView().visibility = View.VISIBLE
    }

    fun hideMuhexSettings() {
        viewModel.isMuhexSettingsVisible = false
        checkOverlayVisibility()
    }

    fun showClockSettings() {
        viewModel.isUnifiedSettingsVisible = false
        viewModel.isClockSettingsVisible = true
    }

    fun hideClockSettings() {
        viewModel.isClockSettingsVisible = false
        checkOverlayVisibility()
    }

    fun showFontPicker(key: String, title: String, source: String) {
        viewModel.fontPickerTargetKey = key
        viewModel.fontPickerTitle = title
        viewModel.fontPickerSource = source
        if (source == "unified") viewModel.isUnifiedSettingsVisible = false
        if (source == "clock") viewModel.isClockSettingsVisible = false
        if (source == "dock") viewModel.isDockSettingsVisible = false
        viewModel.isFontSettingsVisible = true
    }

    fun handleFontPickerDismiss() {
        viewModel.isFontSettingsVisible = false
        when (viewModel.fontPickerSource) {
            "clock" -> viewModel.isClockSettingsVisible = true
            "dock" -> viewModel.isDockSettingsVisible = true
            "unified" -> viewModel.isUnifiedSettingsVisible = true
            else -> {
                // Fallback to key-based logic if source is unknown
                when {
                    viewModel.fontPickerTargetKey.contains("clock") || viewModel.fontPickerTargetKey.contains("date") -> {
                        viewModel.isClockSettingsVisible = true
                    }
                    viewModel.fontPickerTargetKey.contains("dock") -> {
                        viewModel.isDockSettingsVisible = true
                    }
                    else -> checkOverlayVisibility()
                }
            }
        }
    }

    fun checkOverlayVisibility() {
        if (!viewModel.isHomePopupVisible && !viewModel.isUnifiedSettingsVisible &&
            !viewModel.isClockSettingsVisible && !viewModel.isFontSettingsVisible &&
            !viewModel.isMuhexSettingsVisible && !viewModel.isQuickMenuVisible &&
            !viewModel.isDockSettingsVisible) {
            getComposeView().visibility = View.GONE
        }
    }

    fun dismissComposeOverlays() {
        viewModel.dismissComposeOverlays()
        viewModel.loadPinnedApps()
        getComposeView().visibility = View.GONE
    }

    fun showUnifiedSettings(tab: Int) {
        viewModel.isHomePopupVisible = false
        viewModel.unifiedSettingsInitialTab = tab
        viewModel.isUnifiedSettingsVisible = true
        getComposeView().visibility = View.VISIBLE
    }

    fun showDockSettings() {
        viewModel.isHomePopupVisible = false
        viewModel.isDockSettingsVisible = true
        getComposeView().visibility = View.VISIBLE
    }
}
