package com.example.myapplication

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.*
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.clock.ClockSettingsSheet
import com.example.myapplication.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

/**
 * HomeFragment: The primary screen of the launcher.
 * Responsibilities:
 * 1. UI Composition (Views & Compose)
 * 2. Widget Management (Delegated to WidgetHostManager)
 * 3. Top Section Management (Delegated to TopSectionController & TopPagesManager)
 * 4. Gesture Handling
 * 5. Launcher-level Actions (Flashlight, Settings, etc.)
 */
class HomeFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    // region Properties & State
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val repository by lazy { AppRepository.getInstance(requireContext()) }
    private val prefs by lazy { requireContext().getSharedPreferences("launcher_settings", Context.MODE_PRIVATE) }

    private lateinit var topPagesManager: TopPagesManager
    private lateinit var widgetHostManager: WidgetHostManager
    private lateinit var topSectionController: TopSectionController
    private lateinit var gestureHandler: GestureHandler
    private lateinit var gestureDetector: GestureDetector

    private var isFlashlightOn = false

    // Result Launchers
    private val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> handleWidgetPickerResult(result.resultCode, result.data) }
    // endregion

    // region Lifecycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initManagers()
        setupUI()
        setupObservers()
        registerReceivers()
    }

    override fun onStart() {
        super.onStart()
        if (::widgetHostManager.isInitialized) widgetHostManager.startListening()
    }

    override fun onStop() {
        super.onStop()
        if (::widgetHostManager.isInitialized) widgetHostManager.stopListening()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
        checkDefaultLauncherStatus()
        applyCurrentSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterReceivers()
        _binding = null
    }
    // endregion

    // region Initialization
    private fun initManagers() {
        topPagesManager = TopPagesManager(requireContext(), binding, prefs)
        widgetHostManager = WidgetHostManager(requireContext())
        topSectionController = TopSectionController(
            context = requireContext(),
            binding = binding,
            topPagesManager = topPagesManager,
            widgetHostManager = widgetHostManager,
            prefs = prefs
        )
        gestureHandler = GestureHandler(requireContext(), requireActivity(), repository, viewLifecycleOwner)
    }

    private fun setupUI() {
        topPagesManager.setupTopPages(layoutInflater)
        topSectionController.apply {
            onAddWidgetRequested = { launchWidgetPicker() }
            applyMode()
        }
        setupGestures(binding.root)
        setupComposeUIs()
        binding.btnSetDefaultLauncher.setOnClickListener { promptSetDefaultLauncher() }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.appsChangedFlow.collect { refreshData() }
        }
    }

    private fun applyCurrentSettings() {
        topPagesManager.applyClockSettings()
        topPagesManager.applyTopSectionVisibility()
        applyPinnedAppsVisibility()
        applyDockVisibility()
        refreshQuotesUI()
    }

    private fun refreshData() {
        viewModel.refreshData()
    }
    // endregion

    // region Compose UI Setup
    private fun setupComposeUIs() {
        setupPinnedAppsCompose()
        setupDockCompose()
        setupOverlaysCompose()
    }

    private fun setupPinnedAppsCompose() {
        binding.pinnedAppsCompose.setContent {
            PinnedApps(
                apps = viewModel.pinnedAppsList,
                isEditMode = false,
                onAppClick = { app -> LaunchApp.launch(requireActivity(), app) },
                onAppLongClick = { app, anchorView ->
                    AppMenuHelper.showMenu(requireActivity() as AppCompatActivity, anchorView, app) {
                        viewModel.loadPinnedApps()
                    }
                },
                onEmptyLongClick = { showHomePopup() }
            )
        }
    }

    private fun setupDockCompose() {
        binding.dockCompose.setContent {
            DockBar(
                repository = repository,
                apps = viewModel.dockAppsList,
                onAppClick = { app -> LaunchApp.launch(requireContext(), app) },
                onAppLongClick = { app, anchorView ->
                    AppMenuHelper.showMenu(requireActivity() as AppCompatActivity, anchorView, app) {
                        viewModel.loadDockApps()
                    }
                },
                onOrderChanged = { newList -> viewModel.dockAppsList = newList }
            )
        }
    }

    private fun setupOverlaysCompose() {
        binding.homePopupCompose.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(viewModel.isQuickMenuVisible) {
                    if (viewModel.isQuickMenuVisible) viewModel.updateRecentApps()
                }

                HomePopup(
                    isVisible = viewModel.isHomePopupVisible,
                    onAction = { handleHomePopupAction(it) }
                )

                UnifiedSettingsSheet(
                    repository = repository,
                    prefs = prefs,
                    isVisible = viewModel.isUnifiedSettingsVisible,
                    initialTab = viewModel.unifiedSettingsInitialTab,
                    onOpenFontPicker = { key, title -> showFontPicker(key, title, source = "unified") },
                    onOpenClockSettings = { showClockSettings() },
                    onAddWidget = { launchWidgetPicker() },
                    onDismiss = {
                        dismissComposeOverlays()
                        viewModel.loadPinnedApps()
                    }
                )

                ClockSettingsSheet(
                    prefs = prefs,
                    isVisible = viewModel.isClockSettingsVisible,
                    onOpenFontPicker = { key, title -> showFontPicker(key, title, source = "clock") },
                    onDismiss = { hideClockSettings() }
                )

                QuickMenu(
                    isVisible = viewModel.isQuickMenuVisible,
                    apps = viewModel.allAppsList,
                    recentApps = viewModel.recentAppsList,
                    onAction = { handleQuickMenuAction(it) },
                    onAppClick = { app ->
                        LaunchApp.launch(requireContext(), app)
                        dismissComposeOverlays()
                    }
                )

                FontSettings(
                    prefs = prefs,
                    targetKey = viewModel.fontPickerTargetKey,
                    title = viewModel.fontPickerTitle,
                    isVisible = viewModel.isFontSettingsVisible,
                    onDismiss = { handleFontPickerDismiss() }
                )

                MuhexSettingsSheet(
                    prefs = prefs,
                    isVisible = viewModel.isMuhexSettingsVisible,
                    onDismiss = {
                        viewModel.isMuhexSettingsVisible = false
                        checkOverlayVisibility()
                    }
                )

                if (viewModel.isPermissionCheckerVisible) {
                    PermissionCheckerPopup(onAllGranted = { viewModel.isPermissionCheckerVisible = false })
                }
            }
        }
    }
    // endregion

    // region UI Actions
    private fun showHomePopup() {
        viewModel.isHomePopupVisible = true
        binding.homePopupCompose.visibility = View.VISIBLE
    }

    private fun showClockSettings() {
        viewModel.isUnifiedSettingsVisible = false
        viewModel.isClockSettingsVisible = true
    }

    private fun hideClockSettings() {
        viewModel.isClockSettingsVisible = false
        checkOverlayVisibility()
    }

    private fun showFontPicker(key: String, title: String, source: String) {
        viewModel.fontPickerTargetKey = key
        viewModel.fontPickerTitle = title
        if (source == "unified") viewModel.isUnifiedSettingsVisible = false
        if (source == "clock") viewModel.isClockSettingsVisible = false
        viewModel.isFontSettingsVisible = true
    }

    private fun handleFontPickerDismiss() {
        viewModel.isFontSettingsVisible = false
        if (viewModel.fontPickerTargetKey.contains("clock") || viewModel.fontPickerTargetKey.contains("date")) {
            viewModel.isClockSettingsVisible = true
        } else {
            checkOverlayVisibility()
        }
    }

    private fun checkOverlayVisibility() {
        if (!viewModel.isHomePopupVisible && !viewModel.isUnifiedSettingsVisible &&
            !viewModel.isClockSettingsVisible && !viewModel.isFontSettingsVisible &&
            !viewModel.isMuhexSettingsVisible && !viewModel.isQuickMenuVisible) {
            binding.homePopupCompose.visibility = View.GONE
        }
    }

    private fun dismissComposeOverlays() {
        viewModel.dismissComposeOverlays()
        binding.homePopupCompose.visibility = View.GONE
    }
    // endregion

    // region Action Handlers
    private fun handleHomePopupAction(action: HomePopupAction) {
        when (action) {
            HomePopupAction.Wallpaper -> {
                dismissComposeOverlays()
                openWallpaperPicker()
            }
            HomePopupAction.Muhex -> {
                viewModel.isHomePopupVisible = false
                viewModel.isMuhexSettingsVisible = true
            }
            HomePopupAction.Dock -> showUnifiedSettings(tab = 5)
            HomePopupAction.PinnedApps -> showUnifiedSettings(tab = 2)
            HomePopupAction.Gestures -> showUnifiedSettings(tab = 4)
            HomePopupAction.Settings -> showUnifiedSettings(tab = 0)
            HomePopupAction.Dismiss -> dismissComposeOverlays()
        }
    }

    private fun showUnifiedSettings(tab: Int) {
        viewModel.isHomePopupVisible = false
        viewModel.unifiedSettingsInitialTab = tab
        viewModel.isUnifiedSettingsVisible = true
        binding.homePopupCompose.visibility = View.VISIBLE
    }

    private fun handleQuickMenuAction(action: QuickMenuAction) {
        dismissComposeOverlays()
        when (action) {
            is QuickMenuAction.Flashlight -> toggleFlashlight()
            is QuickMenuAction.Settings -> startActivity(Intent(Settings.ACTION_SETTINGS))
            is QuickMenuAction.Apps -> (requireActivity() as? MainActivity)?.openDrawer("quick_menu")
            is QuickMenuAction.Wifi -> startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            is QuickMenuAction.Bluetooth -> startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            is QuickMenuAction.Calculator -> launchCommonApp("calculator")
            is QuickMenuAction.Calendar -> launchCommonApp("calendar")
            is QuickMenuAction.Search -> (requireActivity() as? MainActivity)?.openDrawer("search", focusSearch = true)
            is QuickMenuAction.Files -> launchCommonApp("files")
            is QuickMenuAction.Browser -> launchCommonApp("browser")
            else -> {}
        }
    }
    // endregion

    // region Gesture Handling
    private fun setupGestures(root: View) {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                handleGestureAction(prefs.getString("gesture_double_tap", "lock_screen"), "double_tap")
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                showHomePopup()
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffY = e1.y - e2.y
                val diffX = e1.x - e2.x

                return if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                        if (diffX > 0) handleGestureAction(prefs.getString("gesture_swipe_left", "none"), "swipe_left")
                        else handleGestureAction(prefs.getString("gesture_swipe_right", "none"), "swipe_right")
                        true
                    } else false
                } else {
                    if (Math.abs(diffY) > 100 && Math.abs(velocityY) > 100) {
                        if (diffY > 0) handleGestureAction(prefs.getString("gesture_swipe_up", "app_drawer"), "swipe_up")
                        else handleGestureAction(prefs.getString("gesture_swipe_down", "notifications"), "swipe_down")
                        true
                    } else false
                }
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        root.setOnTouchListener { v, event ->
            val handled = gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                v.performClick()
            }
            handled || true
        }
    }

    private fun handleGestureAction(action: String?, gestureType: String) {
        gestureHandler.handleGestureAction(
            action,
            gestureType,
            onOpenQuickMenu = {
                viewModel.isQuickMenuVisible = true
                binding.homePopupCompose.visibility = View.VISIBLE
            },
            onOpenDrawer = { type, focus ->
                (requireActivity() as? MainActivity)?.openDrawer(type, focusSearch = focus)
            },
            onOpenHiddenApps = {
                AppAuthenticator(requireContext()).authenticate { success ->
                    if (success) {
                        (requireActivity() as? MainActivity)?.openDrawer(gestureType, showHiddenOnly = true)
                    }
                }
            }
        )
    }
    // endregion

    // region Preferences
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (!isAdded || _binding == null) return

        when (key) {
            "clock_show_main" -> topPagesManager.applyTopSectionVisibility()
            "clock_index", "added_clocks", "added_clocks_ordered" -> topPagesManager.updateTopViewPager()
            "show_now_apps" -> applyPinnedAppsVisibility()
            "show_dock_bar" -> applyDockVisibility()
            "quotes_enabled" -> refreshQuotesUI()
            "top_section_mode" -> topSectionController.applyMode()
        }

        if (key?.startsWith("clock_") == true || key?.startsWith("date_") == true) {
            topPagesManager.applyClockSettings()
        }
    }

    private fun applyPinnedAppsVisibility() {
        binding.pinnedAppsCompose.visibility = if (prefs.getBoolean("show_now_apps", true)) View.VISIBLE else View.GONE
    }

    private fun applyDockVisibility() {
        binding.dockCompose.visibility = if (prefs.getBoolean("show_dock_bar", true)) View.VISIBLE else View.GONE
    }
    // endregion

    // region Helpers
    private fun toggleFlashlight() {
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
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
                startActivity(it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchWidgetPicker() {
        widgetPickerLauncher.launch(Intent(requireContext(), WidgetPickerActivity::class.java))
    }

    private fun handleWidgetPickerResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val widgetId = data?.getIntExtra(
                WidgetPickerActivity.EXTRA_WIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

            if (WidgetSlotModel.isValidWidgetId(widgetId)) {
                val newIndex = widgetHostManager.addSlot(widgetId)
                if (!topSectionController.isWidgetMode()) {
                    topSectionController.switchToWidgetMode(animate = true)
                }
                topSectionController.refreshWidgetStack(targetIndex = newIndex)
            }
        }
    }

    private fun openWallpaperPicker() {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        startActivity(Intent.createChooser(intent, "Select Wallpaper"))
    }

    private fun checkDefaultLauncherStatus() {
        binding.btnSetDefaultLauncher.visibility = if (isDefaultLauncher(requireContext())) View.GONE else View.VISIBLE
    }

    private fun promptSetDefaultLauncher() {
        requestDefaultLauncher(requireActivity())
    }

    private fun refreshQuotesUI() {
        topPagesManager.getAllPages().forEach { UIHelper.updateInspirationalQuote(it) }
    }
    // endregion

    // region Receivers
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = (level * 100 / scale.toFloat()).toInt()
            UIHelper.updateBatteryUI(topPagesManager.getAllPages(), batteryPct)
        }
    }

    private val widgetAddedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val widgetId = intent.getIntExtra(
                WidgetPickerActivity.EXTRA_WIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val explicitIndex = intent.getIntExtra("extra_widget_index", -1)

            if (WidgetSlotModel.isValidWidgetId(widgetId)) {
                val newIndex = if (explicitIndex != -1) explicitIndex else widgetHostManager.addSlot(widgetId)
                if (!topSectionController.isWidgetMode()) {
                    topSectionController.switchToWidgetMode(animate = true)
                }
                topSectionController.refreshWidgetStack(targetIndex = newIndex)
            }
        }
    }

    private fun registerReceivers() {
        prefs.registerOnSharedPreferenceChangeListener(this)
        requireContext().registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val widgetFilter = IntentFilter("com.example.myapplication.WIDGET_ADDED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(widgetAddedReceiver, widgetFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            requireContext().registerReceiver(widgetAddedReceiver, widgetFilter)
        }
    }

    private fun unregisterReceivers() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        requireContext().unregisterReceiver(batteryReceiver)
        requireContext().unregisterReceiver(widgetAddedReceiver)
    }
    // endregion
}
