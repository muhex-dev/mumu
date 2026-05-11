package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import com.example.myapplication.databinding.FragmentHomeBinding

class TopSectionController(
    private val context: Context,
    private val binding: FragmentHomeBinding,
    private val topPagesManager: TopPagesManager,
    private val widgetHostManager: WidgetHostManager,
    private val prefs: SharedPreferences
) {
    var onAddWidgetRequested: (() -> Unit)? = null
    private var recomposeKey = 0

    // ── Apply the current mode from prefs ────────────────────────────────────

    fun applyMode() {
        val mode = WidgetSlotModel.loadTopMode(context)
        if (mode == WidgetSlotModel.MODE_WIDGETS) {
            binding.topContainer.visibility       = View.GONE
            binding.widgetStackCompose.visibility = View.VISIBLE
            renderWidgetStack()
        } else {
            binding.widgetStackCompose.visibility = View.GONE
            binding.topContainer.visibility       = View.VISIBLE
            topPagesManager.updateTopViewPager()
            topPagesManager.applyTopSectionVisibility()
        }
    }

    // ── Switch to Clock Mode ─────────────────────────────────────────────────

    fun switchToClockMode(animate: Boolean = true) {
        WidgetSlotModel.saveTopMode(context, WidgetSlotModel.MODE_CLOCK)

        // Force hide widget stack immediately
        binding.widgetStackCompose.visibility = View.GONE

        if (animate) {
            binding.topContainer.alpha = 0f
            binding.topContainer.visibility = View.VISIBLE
            binding.topContainer.animate()
                .alpha(1f)
                .setDuration(350)
                .start()
        } else {
            binding.topContainer.visibility = View.VISIBLE
        }

        topPagesManager.updateTopViewPager()
        topPagesManager.applyTopSectionVisibility()
    }

    // ── Switch to Widget Mode ────────────────────────────────────────────────

    fun switchToWidgetMode(animate: Boolean = true) {
        WidgetSlotModel.saveTopMode(context, WidgetSlotModel.MODE_WIDGETS)

        // Render widget stack first
        renderWidgetStack()

        // Force hide clock immediately
        binding.topContainer.visibility = View.GONE

        if (animate) {
            binding.widgetStackCompose.alpha = 0f
            binding.widgetStackCompose.visibility = View.VISIBLE
            binding.widgetStackCompose.animate()
                .alpha(1f)
                .setDuration(350)
                .start()
        } else {
            binding.widgetStackCompose.visibility = View.VISIBLE
        }
    }

    // ── Render widget stack ──────────────────────────────────────────────────

    fun renderWidgetStack() {
        val key = recomposeKey
        binding.widgetStackCompose.setContent {
            WidgetStackScreen(
                widgetHostManager = widgetHostManager,
                onAddWidget       = { onAddWidgetRequested?.invoke() },
                recomposeKey      = key
            )
        }
    }

    // ── Refresh after add/remove ─────────────────────────────────────────────

    fun refreshWidgetStack() {
        val mode = WidgetSlotModel.loadTopMode(context)
        if (mode == WidgetSlotModel.MODE_WIDGETS) {
            recomposeKey++
            renderWidgetStack()
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun isWidgetMode(): Boolean =
        WidgetSlotModel.loadTopMode(context) == WidgetSlotModel.MODE_WIDGETS

    fun isClockMode(): Boolean = !isWidgetMode()

    private fun crossfade(hide: View, show: View) {
        show.alpha = 0f
        show.visibility = View.VISIBLE
        show.animate()
            .alpha(1f)
            .setDuration(350)
            .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
            .start()

        hide.animate()
            .alpha(0f)
            .setDuration(350)
            .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
            .withEndAction {
                hide.visibility = View.GONE
                hide.alpha = 1f
            }
            .start()
    }

    private fun setGuidelinePercent(percent: Float) {
        val params = binding.topSectionGuideline.layoutParams
                as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.guidePercent = percent
        binding.topSectionGuideline.layoutParams = params
    }
}