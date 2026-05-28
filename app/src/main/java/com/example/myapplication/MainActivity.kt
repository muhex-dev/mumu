package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import com.example.myapplication.databinding.ActivityMainBinding

/**
 * MainActivity serves as the primary host for the launcher's UI.
 * It manages the transitions between the Home screen and the App Drawer.
 */
class MainActivity : AppCompatActivity() {

    // region Properties & State
    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("launcher_settings", MODE_PRIVATE) }
    
    var isDrawerOpen = false
        private set

    /**
     * Dedicated back press handler for the drawer.
     * It is enabled only when the drawer is open.
     */
    private val drawerBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            closeDrawer()
        }
    }
    // endregion

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppRepository.getInstance(this).registerReceivers()
        setupSystemUI()
        setupNavigation()
        setupGlobalOverlays()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // If the Home button is pressed while the drawer is open, we should return to Home
        if (isDrawerOpen) closeDrawer()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppRepository.getInstance(this).unregisterReceivers()
    }
    // endregion

    // region Initialization
    private fun setupSystemUI() {
        // Ensure transparent status and navigation bars for edge-to-edge look
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Hide status bar on Home by default for a minimal launcher aesthetic
        setStatusBarVisible(false)

        // Prevent content from being hidden behind system bars in the drawer
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerContainer) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        onBackPressedDispatcher.addCallback(this, drawerBackCallback)
    }

    private fun setupGlobalOverlays() {
        val composeView = ComposeView(this).apply {
            setContent {
                PermissionCheckerPopup(onAllGranted = {
                    // Optional: Refresh fragments or trigger data load after permissions
                })
            }
        }
        binding.root.addView(composeView)
    }
    // endregion

    // region Drawer Management
    /**
     * Opens the app drawer with the specified configuration.
     */
    fun openDrawer(
        openedByGesture: String = "swipe_up",
        showHiddenOnly: Boolean = false,
        focusSearch: Boolean = false
    ) {
        if (isDrawerOpen) return

        val animType = prefs.getString("drawer_open_anim", "fade")
        val (enter, exit) = getAnimationResources(animType)

        val fragment = DrawerFragment().apply {
            arguments = Bundle().apply {
                putString("opened_by_gesture", openedByGesture)
                putBoolean("show_hidden_only", showHiddenOnly)
                putBoolean("focus_search", focusSearch)
            }
        }

        supportFragmentManager.commit {
            setCustomAnimations(enter, exit, 0, 0)
            replace(R.id.drawer_container, fragment)
        }

        setDrawerState(isOpen = true)

        // Transition: Fade out Home screen
        animateHomeAlpha(target = 0f, duration = 400) {
            binding.fragmentContainer.visibility = View.GONE
        }
    }

    /**
     * Closes the drawer and returns focus to the Home screen.
     */
    fun closeDrawer() {
        if (!isDrawerOpen) return

        val animType = prefs.getString("drawer_close_anim", "fade")
        val fragment = supportFragmentManager.findFragmentById(R.id.drawer_container)

        // Support for custom circular reveal animation if the fragment implements it
        if (animType == "circle" && fragment is DrawerFragment) {
            fragment.startCircularExitAnimation {
                removeDrawerFragment(fragment)
                finalizeClosing()
            }
            return
        }

        val (enter, exit) = getAnimationResources(animType, isClosing = true)
        removeDrawerFragment(fragment, enter, exit)
        finalizeClosing()
    }

    private fun removeDrawerFragment(fragment: androidx.fragment.app.Fragment?, enter: Int = 0, exit: Int = 0) {
        if (fragment != null) {
            supportFragmentManager.commit {
                setCustomAnimations(enter, exit)
                remove(fragment)
            }
        }
    }

    private fun setDrawerState(isOpen: Boolean) {
        isDrawerOpen = isOpen
        drawerBackCallback.isEnabled = isOpen
        setStatusBarVisible(isOpen)
        binding.drawerContainer.visibility = if (isOpen) View.VISIBLE else View.GONE
    }

    private fun finalizeClosing() {
        isDrawerOpen = false
        drawerBackCallback.isEnabled = false
        setStatusBarVisible(false)

        binding.fragmentContainer.visibility = View.VISIBLE
        animateHomeAlpha(target = 1f, duration = 400)

        // Delay hiding the container until fragment exit animations complete
        binding.drawerContainer.postDelayed({
            if (!isDrawerOpen) binding.drawerContainer.visibility = View.GONE
        }, 450)
    }
    // endregion

    // region UI Helpers
    fun setStatusBarVisible(visible: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            if (visible) {
                show(WindowInsetsCompat.Type.statusBars())
            } else {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun animateHomeAlpha(target: Float, duration: Long, onEnd: (() -> Unit)? = null) {
        binding.fragmentContainer.animate()
            .alpha(target)
            .setDuration(duration)
            .withEndAction { onEnd?.invoke() }
            .start()
    }

    private fun getAnimationResources(type: String?, isClosing: Boolean = false): Pair<Int, Int> = when (type) {
        "premium"    -> if (isClosing) 0 to R.anim.premium_exit else R.anim.premium_enter to 0
        "slide_up"   -> if (isClosing) 0 to R.anim.slide_out_down else R.anim.slide_in_up to 0
        "slide_down" -> if (isClosing) 0 to R.anim.slide_in_up else R.anim.slide_out_down to 0
        "scale"      -> if (isClosing) 0 to R.anim.popup_exit else R.anim.popup_enter to 0
        "fade"       -> android.R.anim.fade_in to android.R.anim.fade_out
        else         -> 0 to 0
    }
    // endregion
}

