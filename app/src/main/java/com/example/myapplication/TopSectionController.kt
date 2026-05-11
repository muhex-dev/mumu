package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import com.example.myapplication.databinding.FragmentHomeBinding
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
class TopSectionController(
    private val context: Context,
    private val binding: FragmentHomeBinding,
    private val topPagesManager: TopPagesManager,
    private val widgetHostManager: WidgetHostManager,
    private val prefs: SharedPreferences
) {
    // Tracks current container dimensions for size reporting
private var containerWidthPx  = 0
    private var containerHeightPx = 0
    var onAddWidgetRequested: (() -> Unit)? = null
    private var recomposeKey = 0

    // ── Apply mode on startup ────────────────────────────────────────────────

    fun applyMode() {
        val mode = WidgetSlotModel.loadTopMode(context)
        restoreGeometry()
        if (mode == WidgetSlotModel.MODE_WIDGETS) {
            binding.topContainer.visibility       = View.GONE
            binding.widgetStackCompose.visibility = View.VISIBLE
            renderWidgetStack()
            // Report size once layout is complete
            binding.widgetStackCompose.post {
                measureAndReportSize()
                attachSizeObserver()
            }
        } else {
            binding.widgetStackCompose.visibility = View.GONE
            binding.topContainer.visibility       = View.VISIBLE
            topPagesManager.updateTopViewPager()
            topPagesManager.applyTopSectionVisibility()
        }
    }

    // ── Clock mode ───────────────────────────────────────────────────────────

    fun switchToClockMode(animate: Boolean = true) {
        WidgetSlotModel.saveTopMode(context, WidgetSlotModel.MODE_CLOCK)
        binding.widgetStackCompose.visibility = View.GONE
        if (animate) {
            binding.topContainer.alpha = 0f
            binding.topContainer.visibility = View.VISIBLE
            binding.topContainer.animate().alpha(1f).setDuration(350).start()
        } else {
            binding.topContainer.visibility = View.VISIBLE
        }
        topPagesManager.updateTopViewPager()
        topPagesManager.applyTopSectionVisibility()
    }

    // ── Widget mode ──────────────────────────────────────────────────────────

    fun switchToWidgetMode(animate: Boolean = true) {
        WidgetSlotModel.saveTopMode(context, WidgetSlotModel.MODE_WIDGETS)
        renderWidgetStack()
        binding.topContainer.visibility = View.GONE
        if (animate) {
            binding.widgetStackCompose.alpha = 0f
            binding.widgetStackCompose.visibility = View.VISIBLE
            binding.widgetStackCompose.animate().alpha(1f).setDuration(350).start()
        } else {
            binding.widgetStackCompose.visibility = View.VISIBLE
        }
    }

    // ── Render ───────────────────────────────────────────────────────────────

    fun renderWidgetStack() {
        val key = recomposeKey
        binding.widgetStackCompose.setContent {
            WidgetStackScreen(
                widgetHostManager  = widgetHostManager,
                onAddWidget        = { onAddWidgetRequested?.invoke() },
                onTopDrag          = { delta -> onTopHandleDrag(delta) },
                onBottomDrag       = { delta -> onBottomHandleDrag(delta) },
                onDragEnd          = { saveGeometry() },
                recomposeKey       = key
            )
        }
    }

    fun refreshWidgetStack() {
        if (WidgetSlotModel.loadTopMode(context) == WidgetSlotModel.MODE_WIDGETS) {
            recomposeKey++
            renderWidgetStack()
        }
    }

    // ── Geometry — top handle moves top edge of container ───────────────────

    private fun onTopHandleDrag(deltaYPx: Float) {
        val screenH = binding.root.height.toFloat()
        if (screenH == 0f) return

        // Top handle moves the TOP constraint of widgetStackCompose.
        // We do this by changing the guideline that the TOP is pinned to.
        // Since top is pinned to parent top (0%), we use a second guideline
        // stored as top margin offset.
        val topParams = binding.widgetStackCompose.layoutParams
                as ConstraintLayout.LayoutParams
        val currentTopMargin = topParams.topMargin
        val newTopMargin = (currentTopMargin + deltaYPx.toInt())
            .coerceIn(0, (screenH * 0.4f).toInt())
        topParams.topMargin = newTopMargin
        binding.widgetStackCompose.layoutParams = topParams

        // Also move the dim overlay and any overlapping views
        val topContainerParams = binding.topDimOverlay.layoutParams
                as ConstraintLayout.LayoutParams
        topContainerParams.topMargin = newTopMargin
        binding.topDimOverlay.layoutParams = topContainerParams
    }

    // ── Geometry — bottom handle resizes the bottom edge ────────────────────

    private fun onBottomHandleDrag(deltaYPx: Float) {
        val screenH = binding.root.height.toFloat()
        if (screenH == 0f) return
        val params = binding.topSectionGuideline.layoutParams
                as ConstraintLayout.LayoutParams
        val newPercent = (params.guidePercent + deltaYPx / screenH)
            .coerceIn(0.15f, 0.75f)
        params.guidePercent = newPercent
        binding.topSectionGuideline.layoutParams = params

        // Report new size to widget immediately during drag
        binding.widgetStackCompose.post { measureAndReportSize() }
    }


    // ── Persist & restore ────────────────────────────────────────────────────

    private fun saveGeometry() {
        val screenH = binding.root.height.toFloat()
        if (screenH == 0f) return

        val topMargin = (binding.widgetStackCompose.layoutParams
                as ConstraintLayout.LayoutParams).topMargin
        val topPercent = topMargin / screenH

        val bottomParams = binding.topSectionGuideline.layoutParams
                as ConstraintLayout.LayoutParams
        val bottomPercent = bottomParams.guidePercent

        WidgetSlotModel.saveGeometry(context, topPercent, bottomPercent)
    }

    private fun restoreGeometry() {
        val screenH = binding.root.height.toFloat()

        // Restore bottom (guideline)
        val bottomPercent = WidgetSlotModel.loadBottomPercent(context)
        val guideParams = binding.topSectionGuideline.layoutParams
                as ConstraintLayout.LayoutParams
        guideParams.guidePercent = bottomPercent
        binding.topSectionGuideline.layoutParams = guideParams

        // Restore top (margin) — needs screen height, post if not ready
        if (screenH == 0f) {
            binding.root.post { restoreTopMargin() }
        } else {
            restoreTopMargin()
        }
    }

    private fun restoreTopMargin() {
        val screenH = binding.root.height.toFloat()
        if (screenH == 0f) return
        val topPercent = WidgetSlotModel.loadTopPercent(context)
        val topMarginPx = (topPercent * screenH).toInt()

        listOf(binding.widgetStackCompose, binding.topDimOverlay).forEach { v ->
            val p = v.layoutParams as ConstraintLayout.LayoutParams
            p.topMargin = topMarginPx
            v.layoutParams = p
        }
    }

    // ── Query ────────────────────────────────────────────────────────────────

    fun isWidgetMode() =
        WidgetSlotModel.loadTopMode(context) == WidgetSlotModel.MODE_WIDGETS

    fun isClockMode() = !isWidgetMode()

    private fun setGuidelinePercent(percent: Float) {
        val params = binding.topSectionGuideline.layoutParams
                as ConstraintLayout.LayoutParams
        params.guidePercent = percent
        binding.topSectionGuideline.layoutParams = params
    }
    // ── Size reporting ────────────────────────────────────────────────────────────

    private fun measureAndReportSize() {
        val w = binding.widgetStackCompose.width
        val h = binding.widgetStackCompose.height
        if (w > 0 && h > 0) {
            containerWidthPx  = w
            containerHeightPx = h
            widgetHostManager.updateAllWidgetSizes(w, h)
        }
    }

    private fun attachSizeObserver() {
        // ViewTreeObserver fires every time the container is laid out
        // This catches guideline changes (drag resize) automatically
        binding.widgetStackCompose.viewTreeObserver.addOnGlobalLayoutListener {
            val w = binding.widgetStackCompose.width
            val h = binding.widgetStackCompose.height
            if (w != containerWidthPx || h != containerHeightPx) {
                containerWidthPx  = w
                containerHeightPx = h
                widgetHostManager.updateAllWidgetSizes(w, h)
            }
        }
    }
}