package com.muhex.mumu

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load

/**
 * AppGridAdapter: A versatile adapter for displaying applications in a grid or list format.
 */
class AppGridAdapter(
    private val activity: AppCompatActivity,
    private val onAppLongClick: (AppModel, View) -> Unit,
    private val isFavoriteLayout: Boolean = false
) : ListAdapter<AppModel, AppGridAdapter.ViewHolder>(AppDiffCallback()) {

    companion object {
        const val VIEW_TYPE_FAVORITE = 0
        const val VIEW_TYPE_SMALL = 1
        const val VIEW_TYPE_LONG = 2
        const val VIEW_TYPE_SHORT = 3
    }

    private val prefs by lazy { activity.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE) }

    override fun getItemViewType(position: Int): Int {
        if (isFavoriteLayout) return VIEW_TYPE_FAVORITE
        val columns = prefs.getInt("drawer_columns", 4)
        return when (columns) {
            1, 2 -> VIEW_TYPE_LONG
            3 -> {
                val cyclePos = position % 6
                if (cyclePos == 0 || cyclePos == 5) VIEW_TYPE_LONG else VIEW_TYPE_SHORT
            }
            else -> VIEW_TYPE_SMALL 
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = when (viewType) {
            VIEW_TYPE_FAVORITE -> R.layout.item_pinned_app
            VIEW_TYPE_LONG -> R.layout.item_app_long
            VIEW_TYPE_SHORT -> R.layout.item_app_short
            else -> R.layout.item_app
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView: ImageView? = view.findViewById(R.id.app_icon)
        private val nameView: TextView? = view.findViewById(R.id.app_name)

        fun bind(app: AppModel) {
            val displayMode = prefs.getString("drawer_display_mode", "both") ?: "both"
            val iconSizeDp = prefs.getFloat("drawer_icon_size", 48f)
            val labelSizeSp = prefs.getFloat("drawer_label_size", 12f)
            val density = activity.resources.displayMetrics.density
            val iconSizePx = (iconSizeDp * density).toInt()
            
            if (nameView?.text != app.label) {
                nameView?.text = app.label
            }
            if (nameView?.textSize != labelSizeSp) {
                nameView?.textSize = labelSizeSp
            }
            
            // Load icon lazily using Coil
            iconView?.load(app) {
                crossfade(true)
            }

            // Apply Font
            val fontFamily = prefs.getString("drawer_font_family", "sans-serif-condensed") ?: "sans-serif-condensed"
            val targetTypeface = FontManager.resolveTypeface(fontFamily)
            if (nameView?.typeface != targetTypeface) {
                nameView?.typeface = targetTypeface
            }

            // Apply icon size
            iconView?.layoutParams?.let { lp ->
                if (lp.width != iconSizePx || lp.height != iconSizePx) {
                    lp.width = iconSizePx
                    lp.height = iconSizePx
                    iconView.layoutParams = lp
                }
            }

            // Respect the display mode preference
            val targetIconVisibility = when (displayMode) {
                "label" -> View.GONE
                else -> View.VISIBLE
            }
            if (iconView?.visibility != targetIconVisibility) {
                iconView?.visibility = targetIconVisibility
            }

            val targetNameVisibility = when (displayMode) {
                "icon" -> View.GONE
                else -> View.VISIBLE
            }
            if (nameView?.visibility != targetNameVisibility) {
                nameView?.visibility = targetNameVisibility
            }
            
            applyItemAppearance()

            itemView.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                LaunchApp.launch(activity, app)
            }

            itemView.setOnLongClickListener {
                onAppLongClick(app, it)
                true
            }
        }

        private fun applyItemAppearance() {
            if (isFavoriteLayout) return

            val itemOpacity = prefs.getInt("drawer_item_opacity", 100)

            val background = itemView.background?.mutate() as? GradientDrawable
            if (background != null) {
                val targetAlpha = (itemOpacity * 2.55).toInt().coerceIn(0, 255)
                if (background.alpha != targetAlpha) {
                    background.alpha = targetAlpha
                }
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppModel>() {
        override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean =
            oldItem.packageName == newItem.packageName && oldItem.userHandle == newItem.userHandle

        override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean =
            oldItem.label == newItem.label && oldItem.lastUpdated == newItem.lastUpdated
    }
}
