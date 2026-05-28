package com.muhex.mumu

import android.animation.*
import android.content.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.muhex.mumu.clock.ClockSettingsSheet
import com.muhex.mumu.databinding.FragmentDrawerBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * DrawerFragment: Displays the list of all installed applications.
 * Features: Search, Scroller, Hidden Apps, Settings Overlays, Circular Animations.
 */
class DrawerFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    // region Properties & State
    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { AppRepository.getInstance(requireContext()) }
    private val prefs by lazy { requireContext().getSharedPreferences("launcher_settings", Context.MODE_PRIVATE) }

    private lateinit var allAppsAdapter: AppGridAdapter
    private lateinit var gestureDetector: GestureDetector

    private var fullAppsList: List<AppModel> = emptyList()
    private var showHiddenOnly = false

    // UI State for Compose Overlays
    private var isSettingsVisible by mutableStateOf(false)
    private var isClockSettingsVisible by mutableStateOf(false)
    private var isFontSettingsVisible by mutableStateOf(false)
    private var fontPickerTargetKey by mutableStateOf("drawer_font_family")
    private var fontPickerTitle by mutableStateOf("Drawer Font")
    // endregion

    // region Lifecycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parseArguments()
        setupUI()
        setupObservers()
        handleEntryAnimation()
        applyCurrentSettings()
        refreshAppList()
    }

    override fun onResume() {
        super.onResume()
        applyCurrentSettings()
        refreshAppList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        _binding = null
    }
    // endregion

    // region Initialization
    private fun parseArguments() {
        showHiddenOnly = arguments?.getBoolean("show_hidden_only", false) ?: false
        if (showHiddenOnly) {
            binding.searchViewApps.queryHint = "Search Hidden Apps"
        }
    }

    private fun setupUI() {
        setupRecyclerView()
        setupMenuAction()
        setupMuhexScroller()
        setupSettingsCompose()
        setupGestures()
        setupSearch()
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.appsChangedFlow.collectLatest {
                    refreshAppList(forceRefresh = true)
                }
            }
        }
    }

    private fun handleEntryAnimation() {
        val openedByGesture = arguments?.getString("opened_by_gesture") ?: "swipe_up"
        val focusSearch = arguments?.getBoolean("focus_search", false) ?: false

        if (prefs.getString("drawer_open_anim", "fade") == "circle") {
            binding.drawerCard.visibility = View.INVISIBLE
            startCircularEnterAnimation()
        }

        if (openedByGesture == "search" || focusSearch) {
            binding.searchViewApps.isIconified = false
            binding.searchViewApps.requestFocus()
        }
    }
    // endregion

    // region Compose UI Setup
    private fun setupSettingsCompose() {
        binding.drawerSettingsCompose.setContent {
            UnifiedSettingsSheet(
                repository = repository,
                prefs = prefs,
                isVisible = isSettingsVisible,
                initialTab = 4,
                onOpenFontPicker = { key, title -> showFontPicker(key, title, from = "unified") },
                onOpenClockSettings = { showClockSettings() },
                onDismiss = { hideSettings() }
            )

            ClockSettingsSheet(
                prefs = prefs,
                isVisible = isClockSettingsVisible,
                onOpenFontPicker = { key, title -> showFontPicker(key, title, from = "clock") },
                onDismiss = { hideClockSettings() }
            )

            FontSettings(
                prefs = prefs,
                targetKey = fontPickerTargetKey,
                title = fontPickerTitle,
                isVisible = isFontSettingsVisible,
                onDismiss = { handleFontPickerDismiss() }
            )
        }
    }

    private fun showDrawerSettings() {
        isSettingsVisible = true
        binding.drawerSettingsCompose.visibility = View.VISIBLE
    }

    private fun hideSettings() {
        isSettingsVisible = false
        checkOverlayVisibility()
    }

    private fun showClockSettings() {
        isSettingsVisible = false
        isClockSettingsVisible = true
    }

    private fun hideClockSettings() {
        isClockSettingsVisible = false
        checkOverlayVisibility()
    }

    private fun showFontPicker(key: String, title: String, from: String) {
        fontPickerTargetKey = key
        fontPickerTitle = title
        if (from == "unified") isSettingsVisible = false
        else if (from == "clock") isClockSettingsVisible = false
        isFontSettingsVisible = true
    }

    private fun handleFontPickerDismiss() {
        isFontSettingsVisible = false
        if (fontPickerTargetKey.contains("clock") || fontPickerTargetKey.contains("date")) {
            isClockSettingsVisible = true
        } else {
            isSettingsVisible = true
        }
    }

    private fun checkOverlayVisibility() {
        if (!isSettingsVisible && !isClockSettingsVisible && !isFontSettingsVisible) {
            binding.drawerSettingsCompose.visibility = View.GONE
        }
    }
    // endregion

    // region Search & Filtering
    private fun setupSearch() {
        binding.searchViewApps.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterApps(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText)
                return true
            }
        })
    }

    private fun filterApps(query: String?) {
        val filtered = if (query.isNullOrBlank()) {
            fullAppsList
        } else {
            fullAppsList.filter { it.label.contains(query, ignoreCase = true) }
        }
        allAppsAdapter.submitList(filtered)
        binding.muhexScroller.visibility = if (query.isNullOrBlank()) View.VISIBLE else View.GONE
    }
    // endregion

    // region RecyclerView & Adapter
    private fun setupRecyclerView() {
        allAppsAdapter = AppGridAdapter(
            activity = requireActivity() as AppCompatActivity,
            onAppLongClick = { app, iconView ->
                iconView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                AppMenuHelper.showMenu(requireActivity() as AppCompatActivity, iconView, app) {
                    refreshAppList()
                }
            }
        )
        binding.appRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = allAppsAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun refreshAppList(forceRefresh: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            fullAppsList = if (showHiddenOnly) {
                repository.getHiddenApps()
            } else {
                repository.getDrawerApps(forceRefresh)
            }
            allAppsAdapter.submitList(fullAppsList) { updateScrollerAlphabet() }
        }
    }
    // endregion

    // region Muhex Scroller
    private fun setupMuhexScroller() {
        binding.muhexScroller.apply {
            onLetterSelected = { letter ->
                showBubble(letter.toString())
                scrollToLetter(letter.toString())
            }
            onActionUp = { hideBubble() }
        }
    }

    private fun updateScrollerAlphabet() {
        val alphabet = allAppsAdapter.currentList.map { it.label.uppercase().first() }.distinct().sorted()
        binding.muhexScroller.setAlphabet(alphabet)
    }

    private fun scrollToLetter(letter: String) {
        val pos = allAppsAdapter.currentList.indexOfFirst { it.label.uppercase().startsWith(letter) }
        if (pos != -1) {
            (binding.appRecyclerView.layoutManager as GridLayoutManager).scrollToPositionWithOffset(pos, 0)
        }
    }

    private fun showBubble(letter: String) {
        val bubble = binding.letterPreviewBubble
        if (bubble.visibility != View.VISIBLE) {
            bubble.alpha = 0f
            bubble.scaleX = 0.5f
            bubble.scaleY = 0.5f
            bubble.visibility = View.VISIBLE
            bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).setInterpolator(OvershootInterpolator()).start()
        }
        if (bubble.text != letter) {
            bubble.text = letter
            animateBubbleBounce(bubble)
        }
    }

    private fun hideBubble() {
        binding.letterPreviewBubble.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(200).withEndAction {
            binding.letterPreviewBubble.visibility = View.GONE
        }.start()
    }

    private fun animateBubbleBounce(view: View) {
        view.animate().scaleY(1.4f).scaleX(0.8f).setDuration(80).withEndAction {
            view.animate().scaleY(1f).scaleX(1f).setInterpolator(OvershootInterpolator(1.5f)).setDuration(250).start()
        }.start()
    }
    // endregion

    // region Gesture Handling
    private fun setupGestures() {
        val openedByGesture = arguments?.getString("opened_by_gesture") ?: "swipe_up"

        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffY = e1.y - e2.y
                val diffX = e1.x - e2.x

                val absDiffX = Math.abs(diffX)
                val absDiffY = Math.abs(diffY)
                val isHorizontal = absDiffX > absDiffY

                if (Math.abs(if (isHorizontal) velocityX else velocityY) < 100) return false

                val triggeredClose = when (openedByGesture) {
                    "swipe_up" -> !isHorizontal && diffY < -100
                    "swipe_down" -> !isHorizontal && diffY > 100
                    "swipe_left" -> isHorizontal && diffX < -100
                    "swipe_right" -> isHorizontal && diffX > 100
                    else -> false
                }

                if (triggeredClose) {
                    if (!isHorizontal) {
                        if (openedByGesture == "swipe_up" && binding.appRecyclerView.canScrollVertically(-1)) return false
                        if (openedByGesture == "swipe_down" && binding.appRecyclerView.canScrollVertically(1)) return false
                    }
                    (requireActivity() as? MainActivity)?.closeDrawer()
                    return true
                }
                return false
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        binding.drawerCard.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        binding.appRecyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
        })
    }
    // endregion

    // region Menu Actions
    private fun setupMenuAction() {
        binding.btnMenu.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showPopupMenu(view)
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.apply {
            add("Refresh")
            add(if (showHiddenOnly) "Show All Apps" else "Hidden Apps")
            add("Drawer Settings")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Refresh" -> {
                    ObjectAnimator.ofFloat(binding.btnMenu, "rotation", 0f, 360f).apply {
                        duration = 500
                        interpolator = LinearInterpolator()
                        start()
                    }
                    refreshAppList(forceRefresh = true)
                }
                "Hidden Apps" -> {
                    AppAuthenticator(requireContext()).authenticate { success ->
                        if (success) {
                            showHiddenOnly = true
                            binding.searchViewApps.queryHint = "Search Hidden Apps"
                            refreshAppList()
                        }
                    }
                }
                "Show All Apps" -> {
                    showHiddenOnly = false
                    binding.searchViewApps.queryHint = "Search Apps"
                    refreshAppList()
                }
                "Drawer Settings" -> showDrawerSettings()
            }
            true
        }
        popup.show()
    }
    // endregion

    // region Animations
    private fun startCircularEnterAnimation() {
        binding.root.post {
            if (_binding == null) return@post
            val view = binding.drawerCard
            view.visibility = View.VISIBLE
            val cx = view.width
            val cy = view.height
            val finalRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
            ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius).apply {
                duration = 500
                start()
            }
        }
    }

    fun startCircularExitAnimation(onComplete: () -> Unit) {
        val view = binding.drawerCard
        val cx = view.width
        val cy = view.height
        val initialRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()

        ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0f).apply {
            duration = 400
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    view.visibility = View.INVISIBLE
                    onComplete()
                }
            })
            start()
        }
    }
    // endregion

    // region Preferences & Appearance
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (!isAdded || _binding == null) return

        val drawerKeys = setOf(
            "drawer_columns", "drawer_opacity", "drawer_item_opacity",
            "drawer_display_mode", "drawer_icon_size", "drawer_label_size", "drawer_font_family"
        )
        val scrollerKeys = setOf(
            "scroller_padding", "scroller_bending", "scroller_spread",
            "scroller_text_size", "scroller_scale", "scroller_line_offset",
            "scroller_line_thickness", "scroller_line_alpha", "scroller_base_alpha",
            "scroller_anim_duration", "scroller_touch_slop", "scroller_haptic"
        )

        when {
            key in drawerKeys -> {
                applyCurrentSettings()
                allAppsAdapter.notifyDataSetChanged()
            }
            key in scrollerKeys -> updateScrollerSettings()
        }
    }

    private fun applyCurrentSettings() {
        configureGridLayout()
        updateDrawerAppearance()
        updateScrollerSettings()
        updateSearchFont()
    }

    private fun configureGridLayout() {
        val columns = prefs.getInt("drawer_columns", 4)
        val layoutManager = binding.appRecyclerView.layoutManager as? GridLayoutManager ?: return

        when (columns) {
            3 -> {
                layoutManager.spanCount = 4
                layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val cyclePos = position % 6
                        return if (cyclePos == 0 || cyclePos == 5) 2 else 1
                    }
                }
            }
            else -> {
                layoutManager.spanCount = columns.coerceAtLeast(1)
                layoutManager.spanSizeLookup = GridLayoutManager.DefaultSpanSizeLookup()
            }
        }
    }

    private fun updateDrawerAppearance() {
        val opacityProgress = prefs.getInt("drawer_opacity", 100)
        (binding.drawerCard.background?.mutate() as? GradientDrawable)?.let { background ->
            background.alpha = (opacityProgress * 2.55).toInt().coerceIn(0, 255)
        }
    }

    private fun updateScrollerSettings() {
        binding.muhexScroller.apply {
            edgePaddingRatio = prefs.getFloat("scroller_padding", 0.12f)
            maxBendingDistance = prefs.getFloat("scroller_bending", 300f)
            curveSpread = prefs.getFloat("scroller_spread", 7.5f)
            baseTextSize = prefs.getFloat("scroller_text_size", 28f)
            selectedTextScale = prefs.getFloat("scroller_scale", 2.4f)
            lineOffsetFromLetter = prefs.getFloat("scroller_line_offset", 40f)
            lineStrokeWidth = prefs.getFloat("scroller_line_thickness", 3f)
            lineMaxAlpha = prefs.getInt("scroller_line_alpha", 90)
            baseLetterAlpha = prefs.getInt("scroller_base_alpha", 130)
            animationDuration = prefs.getInt("scroller_anim_duration", 250).toLong()
            touchSlop = prefs.getFloat("scroller_touch_slop", 80f)
            hapticEnabled = prefs.getBoolean("scroller_haptic", true)
            updateColorsAndGradient()
            invalidate()
        }
    }

    private fun updateSearchFont() {
        val fontFamily = prefs.getString("drawer_font_family", "sans-serif-condensed") ?: "sans-serif-condensed"
        val typeface = FontManager.resolveTypeface(fontFamily)
        binding.searchViewApps.findViewById<android.widget.TextView>(androidx.appcompat.R.id.search_src_text)?.typeface = typeface
        binding.muhexScroller.setTypeface(typeface)
    }
    // endregion
}
