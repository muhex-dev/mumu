package com.example.myapplication

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.*
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
class HomeFragment : Fragment() {

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
    private lateinit var homeReceiverManager: HomeReceiverManager
    private lateinit var homeOverlayController: HomeOverlayController
    private lateinit var homeActionHandler: HomeActionHandler

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

        homeReceiverManager = HomeReceiverManager(
            context = requireContext(),
            prefs = prefs,
            prefListener = prefListener,
            onBatteryChanged = { batteryPct ->
                UIHelper.updateBatteryUI(topPagesManager.getAllPages(), batteryPct)
            },
            onWidgetAdded = { widgetId, explicitIndex ->
                topSectionController.handleWidgetAddition(widgetId, explicitIndex)
            }
        )

        homeOverlayController = HomeOverlayController(
            viewModel = viewModel,
            getComposeView = { binding.homePopupCompose }
        )

        homeActionHandler = HomeActionHandler(
            context = requireContext(),
            activity = requireActivity(),
            overlayController = homeOverlayController,
            onOpenDrawer = { type, focus ->
                (requireActivity() as? MainActivity)?.openDrawer(type, focusSearch = focus)
            },
            onOpenWallpaper = {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                startActivity(Intent.createChooser(intent, "Select Wallpaper"))
            }
        )
    }

    private fun setupUI() {
        topPagesManager.setupTopPages(layoutInflater)
        topSectionController.apply {
            onAddWidgetRequested = { launchWidgetPicker() }
            applyMode()
        }
        gestureHandler.attachToView(
            root = binding.root,
            prefs = prefs,
            onLongPress = { homeOverlayController.showHomePopup() },
            onOpenQuickMenu = { homeOverlayController.showQuickMenu() },
            onOpenDrawer = { type, focus ->
                (requireActivity() as? MainActivity)?.openDrawer(type, focusSearch = focus)
            },
            onOpenHiddenApps = { gestureType ->
                AppAuthenticator(requireContext()).authenticate { success ->
                    if (success) {
                        (requireActivity() as? MainActivity)?.openDrawer(gestureType, showHiddenOnly = true)
                    }
                }
            }
        )
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
    /**
     * Consolidates all Compose-based UI components (Pinned Apps, Dock, and Overlays).
     * Callbacks are delegated to specialized handlers (homeActionHandler, homeOverlayController)
     * to keep the Fragment focused on wiring and lifecycle.
     */
    private fun setupComposeUIs() {
        // 1. Pinned Apps Section
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
                onEmptyLongClick = { homeOverlayController.showHomePopup() }
            )
        }

        // 2. Dock Bar Section
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

        // 3. Overlays (Home Popup, Quick Menu, Settings Sheets)
        binding.homePopupCompose.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(viewModel.isQuickMenuVisible) {
                    if (viewModel.isQuickMenuVisible) viewModel.updateRecentApps()
                }

                HomePopup(
                    isVisible = viewModel.isHomePopupVisible,
                    onAction = { homeActionHandler.handleHomePopupAction(it) }
                )

                UnifiedSettingsSheet(
                    repository = repository,
                    prefs = prefs,
                    isVisible = viewModel.isUnifiedSettingsVisible,
                    initialTab = viewModel.unifiedSettingsInitialTab,
                    onOpenFontPicker = { key, title -> homeOverlayController.showFontPicker(key, title, source = "unified") },
                    onOpenClockSettings = { homeOverlayController.showClockSettings() },
                    onAddWidget = { launchWidgetPicker() },
                    onDismiss = { homeOverlayController.dismissComposeOverlays() }
                )

                ClockSettingsSheet(
                    prefs = prefs,
                    isVisible = viewModel.isClockSettingsVisible,
                    onOpenFontPicker = { key, title -> homeOverlayController.showFontPicker(key, title, source = "clock") },
                    onDismiss = { homeOverlayController.hideClockSettings() }
                )

                QuickMenu(
                    isVisible = viewModel.isQuickMenuVisible,
                    apps = viewModel.allAppsList,
                    recentApps = viewModel.recentAppsList,
                    onAction = { homeActionHandler.handleQuickMenuAction(it) },
                    onAppClick = { app ->
                        LaunchApp.launch(requireContext(), app)
                        homeOverlayController.dismissComposeOverlays()
                    }
                )

                FontSettings(
                    prefs = prefs,
                    targetKey = viewModel.fontPickerTargetKey,
                    title = viewModel.fontPickerTitle,
                    isVisible = viewModel.isFontSettingsVisible,
                    onDismiss = { homeOverlayController.handleFontPickerDismiss() }
                )

                MuhexSettingsSheet(
                    prefs = prefs,
                    isVisible = viewModel.isMuhexSettingsVisible,
                    onDismiss = { homeOverlayController.hideMuhexSettings() }
                )

                if (viewModel.isPermissionCheckerVisible) {
                    PermissionCheckerPopup(onAllGranted = { viewModel.isPermissionCheckerVisible = false })
                }
            }
        }
    }
    // endregion

    // region Preferences
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (!isAdded || _binding == null) return@OnSharedPreferenceChangeListener

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
    private fun launchWidgetPicker() {
        widgetPickerLauncher.launch(Intent(requireContext(), WidgetPickerActivity::class.java))
    }

    private fun handleWidgetPickerResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val widgetId = data?.getIntExtra(
                WidgetPickerActivity.EXTRA_WIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

            topSectionController.handleWidgetAddition(widgetId)
        }
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
    private fun registerReceivers() {
        homeReceiverManager.register()
    }

    private fun unregisterReceivers() {
        homeReceiverManager.unregister()
    }
    // endregion
}
