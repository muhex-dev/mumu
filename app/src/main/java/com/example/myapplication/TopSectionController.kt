package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.myapplication.databinding.FragmentHomeBinding

/**
 * TopSectionController
 *
 * Controls the top section of the home screen.
 *
 * Architecture:
 *   The clock (topContainer) and widgets (widgetStackCompose) are now children
 *   of "topView", which itself is a child of "topSectionWrapper".
 *
 *     • topSectionWrapper.layoutParams.topMargin  → moves the whole section
 *     • topSectionGuideline guidePercent           → resizes the whole section
 */
class TopSectionController(
    private val context: Context,
    private val binding: FragmentHomeBinding,
    private val topPagesManager: TopPagesManager,
    private val widgetHostManager: WidgetHostManager,
    private val prefs: SharedPreferences
) {
    var onAddWidgetRequested: (() -> Unit)? = null
    private var recomposeKey = 0
    private var initialWidgetIndex = 0

    // Widget size tracking
    private var containerWidthPx  = 0
    private var containerHeightPx = 0

    // Positioning state
    private var isPositioning = false
    private val positioningState = PositioningState()

    // ── Startup ──────────────────────────────────────────────────────────────

    fun applyMode() {
        restoreGeometry()
        initPositioningState()
        renderPositionOverlay()

        val mode = WidgetSlotModel.loadTopMode(context)
        if (mode == WidgetSlotModel.MODE_WIDGETS) {
            showWidgetView()
            renderWidgetStack()
            binding.widgetStackCompose.post {
                measureAndReportSize()
                attachSizeObserver()
            }
        } else {
            showClockView()
            topPagesManager.updateTopViewPager()
            topPagesManager.applyClockSettings()
        }

        // Wire long-press on topView → positioning (works in both modes)
        binding.topView.setOnLongClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            enterPositioning()
            true
        }

        topPagesManager.onLongPress = {
            enterPositioning()
        }
    }

    // ── Mode switching ────────────────────────────────────────────────────────

    fun switchToClockMode(animate: Boolean = true) {
        WidgetSlotModel.saveTopMode(context, WidgetSlotModel.MODE_CLOCK)
        exitPositioning(save = false)
        if (animate) {
            binding.widgetStackCompose.animate().alpha(0f).setDuration(200)
                .withEndAction {
                    showClockView()
                    binding.topContainer.alpha = 0f
                    binding.topContainer.animate().alpha(1f).setDuration(300).start()
                }.start()
        } else {
            showClockView()
        }
        topPagesManager.updateTopViewPager()
        topPagesManager.applyClockSettings()
    }

    fun switchToWidgetMode(animate: Boolean = true) {
        WidgetSlotModel.saveTopMode(context, WidgetSlotModel.MODE_WIDGETS)
        exitPositioning(save = false)
        renderWidgetStack()
        if (animate) {
            binding.topContainer.animate().alpha(0f).setDuration(200)
                .withEndAction {
                    showWidgetView()
                    binding.widgetStackCompose.alpha = 0f
                    binding.widgetStackCompose.animate().alpha(1f).setDuration(300).start()
                }.start()
        } else {
            showWidgetView()
        }
    }

    // ── Visibility helpers ────────────────────────────────────────────────────

    private fun showClockView() {
        binding.topSectionWrapper.visibility  = View.VISIBLE
        binding.topContainer.visibility       = View.VISIBLE
        binding.topContainer.alpha            = 1f
        binding.widgetStackCompose.visibility = View.GONE
    }

    private fun showWidgetView() {
        binding.topSectionWrapper.visibility  = View.VISIBLE
        binding.widgetStackCompose.visibility = View.VISIBLE
        binding.widgetStackCompose.alpha      = 1f
        binding.topContainer.visibility       = View.GONE
    }

    // ── Render widget stack ───────────────────────────────────────────────────

    fun renderWidgetStack() {
        val key = recomposeKey
        val targetIdx = initialWidgetIndex
        binding.widgetStackCompose.setContent {
            WidgetStackScreen(
                widgetHostManager  = widgetHostManager,
                onAddWidget        = { onAddWidgetRequested?.invoke() },
                onEnterPositioning = { enterPositioning() },
                recomposeKey       = key,
                initialIndex       = targetIdx
            )
        }
        // Reset after one-time use
        initialWidgetIndex = 0
    }

    fun refreshWidgetStack(targetIndex: Int = -1) {
        if (targetIndex >= 0) {
            initialWidgetIndex = targetIndex
        }
        recomposeKey++
        renderWidgetStack()
    }

    // ── Positioning ───────────────────────────────────────────────────────────

    fun enterPositioning() {
        if (isPositioning) return
        isPositioning = true
        initPositioningState()
        binding.topSectionWrapper.visibility = View.VISIBLE
        binding.positionOverlayCompose.visibility = View.VISIBLE
        renderPositionOverlay()
    }

    fun exitPositioning(save: Boolean = true) {
        isPositioning = false
        positioningState.isVisible = false
        if (save) saveGeometry()
        binding.positionOverlayCompose.postDelayed({
            if (!isPositioning) binding.positionOverlayCompose.visibility = View.GONE
        }, 280)
    }

    private fun initPositioningState() {
        val screenH = binding.root.height.toFloat()
        if (screenH <= 0f) {
            binding.root.post { initPositioningState() }
            return
        }
        val topMargin = (binding.topSectionWrapper.layoutParams as ConstraintLayout.LayoutParams).topMargin.toFloat()
        val guidePercent = (binding.topSectionGuideline.layoutParams as ConstraintLayout.LayoutParams).guidePercent
        
        positioningState.topOffsetPx = topMargin
        positioningState.bottomOffsetPx = guidePercent * screenH
        positioningState.isVisible = isPositioning
    }

    private fun renderPositionOverlay() {
        binding.positionOverlayCompose.setContent {
            PositionOverlay(
                state        = positioningState,
                onTopDrag    = { dy -> onTopDrag(dy) },
                onBottomDrag = { dy -> onBottomDrag(dy) },
                onDone       = { 
                    exitPositioning()
                    // Re-render widget stack to refresh its internal state if needed
                    if (isWidgetMode()) {
                        recomposeKey++
                        renderWidgetStack()
                    }
                }
            )
        }
    }



    // ── Geometry — move (top margin on wrapper) ───────────────────────────────

    private fun onTopDrag(deltaYPx: Float) {
        val screenH = binding.root.height.toFloat()
        if (screenH <= 0f) return

        val params = binding.topSectionWrapper.layoutParams
                as ConstraintLayout.LayoutParams
        val guideParams = binding.topSectionGuideline.layoutParams
                as ConstraintLayout.LayoutParams

        // Current bottom limit in PX
        val bottomLimitPx = guideParams.guidePercent * screenH
        // Minimum section height: 30% of screen
        val minHeightPx = screenH * 0.3f

        val newMargin = (params.topMargin + deltaYPx.toInt())
            .coerceIn(0, (bottomLimitPx - minHeightPx).toInt())
            
        params.topMargin = newMargin
        binding.topSectionWrapper.layoutParams = params
        
        positioningState.topOffsetPx = newMargin.toFloat()
    }

    // ── Geometry — resize (guideline percent) ─────────────────────────────────

    private fun onBottomDrag(deltaYPx: Float) {
        val screenH = binding.root.height.toFloat()
        if (screenH <= 0f) return

        val params = binding.topSectionGuideline.layoutParams
                as ConstraintLayout.LayoutParams
        val topMarginPx = (binding.topSectionWrapper.layoutParams
                as ConstraintLayout.LayoutParams).topMargin
        
        // Minimum section height: 30% of screen
        val minHeightPx = screenH * 0.3f
        val minPercent = (topMarginPx + minHeightPx) / screenH

        val newPercent = (params.guidePercent + deltaYPx / screenH)
            .coerceIn(minPercent.coerceAtMost(0.8f), 0.9f)

        params.guidePercent = newPercent
        binding.topSectionGuideline.layoutParams = params

        // Report new size to widgets immediately
        binding.widgetStackCompose.post { measureAndReportSize() }
        
        positioningState.bottomOffsetPx = newPercent * screenH
    }

    // ── Persist & restore geometry ────────────────────────────────────────────

    private fun saveGeometry() {
        val screenH = binding.root.height.toFloat()
        if (screenH <= 0f) return

        val topMargin = (binding.topSectionWrapper.layoutParams
                as ConstraintLayout.LayoutParams).topMargin
        val topPercent = topMargin.toFloat() / screenH

        val guidePercent = (binding.topSectionGuideline.layoutParams
                as ConstraintLayout.LayoutParams).guidePercent

        WidgetSlotModel.saveGeometry(context, topPercent, guidePercent)
    }

    private fun restoreGeometry() {
        // Restore top margin (section vertical position)
        // We set this first so it's available for bottom drag limits if needed
        applyTopMargin()

        // Restore guideline (section height/bottom boundary)
        val guidePercent = WidgetSlotModel.loadBottomPercent(context)
        val guideParams  = binding.topSectionGuideline.layoutParams
                as ConstraintLayout.LayoutParams
        guideParams.guidePercent = guidePercent
        binding.topSectionGuideline.layoutParams = guideParams
    }

    private fun applyTopMargin() {
        val screenH = binding.root.height.toFloat()
        if (screenH <= 0f) {
            // If screen height not yet available, post it
            binding.root.post { applyTopMargin() }
            return
        }
        val topPercent  = WidgetSlotModel.loadTopPercent(context)
        val topMarginPx = (topPercent * screenH).toInt()
        val params = binding.topSectionWrapper.layoutParams
                as ConstraintLayout.LayoutParams
        params.topMargin = topMarginPx
        binding.topSectionWrapper.layoutParams = params
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    fun isWidgetMode() =
        WidgetSlotModel.loadTopMode(context) == WidgetSlotModel.MODE_WIDGETS

    fun isClockMode() = !isWidgetMode()

    fun handleWidgetAddition(widgetId: Int, explicitIndex: Int = -1) {
        if (WidgetSlotModel.isValidWidgetId(widgetId)) {
            val newIndex = if (explicitIndex != -1) {
                // If it was already in data but we are "adding" it again (re-ordering/broadcast), 
                // just ensure it exists and move to it.
                explicitIndex
            } else {
                widgetHostManager.addSlot(widgetId)
            }
            
            if (!isWidgetMode()) {
                switchToWidgetMode(animate = true)
            }
            refreshWidgetStack(targetIndex = newIndex)
        }
    }

    // ── Size reporting ────────────────────────────────────────────────────────

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
