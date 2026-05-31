package com.muhex.mumu

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.*

// ── Design tokens ─────────────────────────────────────────────────────────────
private val AccentColor  = Color(0xFF7C6DFF)
private val DangerColor  = Color(0xFFFF5C5C)
private val HandleColor  = Color(0xCCFFFFFF)
private val OverlayColor = Color(0xCC000000)
private val TextPrimary  = Color.White
private val TextMuted    = Color(0x99FFFFFF)
private val SurfaceColor = Color(0x22FFFFFF)
private var isPagerAnimating = false

private enum class EditMode { NONE, EDITING }

@Composable
fun WidgetStackScreen(
    widgetHostManager: WidgetHostManager,
    onAddWidget: () -> Unit,
    onEnterPositioning: () -> Unit,
    recomposeKey: Int = 0,
    initialIndex: Int = 0
) {
    val context = LocalContext.current

    var slots by remember(recomposeKey) {
        mutableStateOf(widgetHostManager.pruneOrphanedSlots())
    }
    var currentIndex by remember(recomposeKey) { mutableIntStateOf(initialIndex) }
    var editMode     by remember { mutableStateOf(EditMode.NONE) }
    var showIndicators by remember { mutableStateOf(false) }

    // Track rendered container size for size reporting
    var containerWidthPx  by remember { mutableIntStateOf(0) }
    var containerHeightPx by remember { mutableIntStateOf(0) }

    val dimAlpha by animateFloatAsState(
        targetValue = if (editMode != EditMode.NONE) 0.6f else 0f,
        animationSpec = tween(300), label = "dim"
    )

    LaunchedEffect(slots.size) {
        if (currentIndex >= slots.size && slots.isNotEmpty())
            currentIndex = slots.size - 1
        if (slots.isEmpty()) editMode = EditMode.NONE
    }

    // Re-report size to current widget whenever container size changes
    LaunchedEffect(containerWidthPx, containerHeightPx) {
        val slot = slots.getOrNull(currentIndex) ?: return@LaunchedEffect
        if (containerWidthPx > 0 && containerHeightPx > 0) {
            widgetHostManager.updateWidgetSize(
                slot.widgetId, containerWidthPx, containerHeightPx
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (slots.isEmpty()) {
            EmptyWidgetState(onAddWidget = onAddWidget)
            return@Box
        }

        // ── Two-layer AndroidView pager ───────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                android.widget.FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Layer 0 — widget container with padding for breathing room
                    addView(android.widget.FrameLayout(ctx).apply {
                        id = android.R.id.content
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Subtle inner padding so widget doesn't touch edges
                        val padPx = (8 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(padPx, padPx, padPx, padPx)
                        clipToPadding = false
                    })
                    // Layer 1 — invisible touch interceptor
                    addView(android.view.View(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    })
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),           // breathing room so glow isn't clipped
            update = { root ->
                val widgetContainer = root.getChildAt(0) as android.widget.FrameLayout
                val touchOverlay    = root.getChildAt(1)

                // ── Size observer on the root container ──────────────────────
                // Fires whenever layout changes — during drag, orientation, etc.
                root.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            val w = root.width
                            val h = root.height
                            if (w > 0 && h > 0 &&
                                (w != containerWidthPx || h != containerHeightPx)
                            ) {
                                containerWidthPx  = w
                                containerHeightPx = h

                                // Report to current widget immediately
                                val slot = slots.getOrNull(currentIndex)
                                if (slot != null) {
                                    widgetHostManager.updateWidgetSize(slot.widgetId, w, h)
                                }

                                // Also update the widget's own view size hints
                                val hostView = widgetContainer.getChildAt(0)
                                if (hostView != null) {
                                    reportSizeToHostView(
                                        hostView, w, h,
                                        widgetContainer.paddingLeft,
                                        widgetContainer.paddingTop,
                                        context
                                    )
                                }
                            }
                        }
                    }
                )

                // ── Load widget view ─────────────────────────────────────────
                widgetContainer.removeAllViews()
                val slot = slots.getOrNull(currentIndex)
                if (slot != null) {
                    val info = widgetHostManager.getProviderInfo(slot.widgetId)
                    if (info == null) {
                        widgetContainer.addView(buildUnavailableView(context))
                    } else {
                        try {
                            val hostView = widgetHostManager.createHostView(slot.widgetId, info)
                            hostView.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Report min size from provider info as baseline
                            val densityVal = context.resources.displayMetrics.density
                            val minW = (info.minWidth  * densityVal).toInt()
                            val minH = (info.minHeight * densityVal).toInt()
                            hostView.minimumWidth  = minW
                            hostView.minimumHeight = minH

                            widgetContainer.addView(hostView)

                            // Report size immediately after add
                            widgetContainer.post {
                                val w = widgetContainer.width
                                val h = widgetContainer.height
                                if (w > 0 && h > 0) {
                                    widgetHostManager.updateWidgetSize(slot.widgetId, w, h)
                                    reportSizeToHostView(
                                        hostView, w, h,
                                        widgetContainer.paddingLeft,
                                        widgetContainer.paddingTop,
                                        context
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("WidgetStack", "createHostView: ${e.message}")
                            widgetContainer.addView(buildUnavailableView(context))
                        }
                    }
                }

                // ── Gesture detector ─────────────────────────────────────────
                var startX = 0f; var startY = 0f
                var isSwiping = false; var longHandled = false

                val gd = GestureDetector(context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDown(e: MotionEvent) = true

                        override fun onLongPress(e: MotionEvent) {
                            longHandled = true
                            editMode = if (editMode == EditMode.NONE)
                                EditMode.EDITING else EditMode.NONE
                            root.performHapticFeedback(
                                android.view.HapticFeedbackConstants.LONG_PRESS
                            )
                        }

                        override fun onFling(
                            e1: MotionEvent?, e2: MotionEvent,
                            velocityX: Float, velocityY: Float
                        ): Boolean {
                            if (editMode != EditMode.NONE) {
                                editMode = EditMode.NONE; return true
                            }
                            if (e1 == null || slots.size <= 1) return false
                            val dx = e1.x - e2.x
                            val dy = e1.y - e2.y
                            if (abs(dx) > abs(dy) * 3
                                && abs(dx) > 150
                                && abs(velocityX) > 200
                            ) {
                                val goNext = dx > 0
                                val newIdx = if (goNext)
                                    (currentIndex + 1) % slots.size
                                else if (currentIndex <= 0) slots.size - 1
                                else currentIndex - 1

                                if (!isPagerAnimating) {
                                    showIndicators = true
                                    animatePagerTransition(
                                        root, widgetContainer,
                                        currentIndex, newIdx, goNext,
                                        slots, widgetHostManager, context
                                    ) {
                                        currentIndex = newIdx
                                        showIndicators = false
                                        // Report size to newly shown widget
                                        widgetContainer.post {
                                            val newSlot = slots.getOrNull(newIdx)
                                            if (newSlot != null && containerWidthPx > 0) {
                                                widgetHostManager.updateWidgetSize(
                                                    newSlot.widgetId,
                                                    containerWidthPx,
                                                    containerHeightPx
                                                )
                                            }
                                        }
                                    }
                                }
                                return true
                            }
                            return false
                        }
                    }
                )

                touchOverlay.setOnTouchListener { _, event ->
                    gd.onTouchEvent(event)
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.x; startY = event.y
                            isSwiping = false; longHandled = false
                            if (editMode == EditMode.NONE)
                                widgetContainer.dispatchTouchEvent(event)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (abs(event.x - startX) > 20
                                || abs(event.y - startY) > 20) {
                                isSwiping = true
                                showIndicators = true
                                val c = MotionEvent.obtain(event).apply {
                                    action = MotionEvent.ACTION_CANCEL
                                }
                                widgetContainer.dispatchTouchEvent(c)
                                c.recycle()
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!isPagerAnimating) showIndicators = false
                            if (!isSwiping && !longHandled
                                && editMode == EditMode.NONE)
                                widgetContainer.dispatchTouchEvent(event)
                            isSwiping = false; longHandled = false
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            if (!isPagerAnimating) showIndicators = false
                            isSwiping = false
                            widgetContainer.dispatchTouchEvent(event)
                        }
                    }
                    true
                }
            }
        )

        // ── Dim overlay ───────────────────────────────────────────────────────
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(dimAlpha)
                    .background(OverlayColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        editMode = EditMode.NONE
                    }
            )
        }

        // ── Edit mode — action buttons ────────────────────────────────────────
        AnimatedVisibility(
            visible = editMode == EditMode.EDITING,
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.75f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            ),
            exit  = fadeOut(tween(160)) + scaleOut(targetScale = 0.75f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val currentSlot = slots.getOrNull(currentIndex)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Widget name label
                val info = currentSlot?.let {
                    widgetHostManager.getProviderInfo(it.widgetId)
                }
                val widgetLabel = info?.let {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(
                            it.provider.packageName, 0
                        )
                    ).toString()
                } ?: ""
                if (widgetLabel.isNotEmpty()) {
                    Text(
                        widgetLabel,
                        color = TextPrimary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Dot indicator showing which widget we're on
                if (slots.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        slots.forEachIndexed { i, _ ->
                            Box(
                                Modifier
                                    .size(if (i == currentIndex) 7.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == currentIndex) Color.White
                                        else Color.White.copy(0.3f)
                                    )
                            )
                        }
                    }
                }

                // Remove + Add row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditActionButton(
                        icon    = Icons.Default.Close,
                        label   = "Remove",
                        color   = DangerColor,
                        onClick = {
                            if (currentSlot != null) {
                                editMode = EditMode.NONE
                                slots = widgetHostManager.removeSlot(currentSlot.widgetId)
                            }
                        }
                    )
                    EditActionButton(
                        icon    = Icons.Default.Add,
                        label   = "Add",
                        color   = AccentColor,
                        onClick = {
                            editMode = EditMode.NONE
                            onAddWidget()
                        }
                    )
                }

                // Position & Size pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onEnterPositioning()
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenWith, null,
                            tint = TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "Position & Size",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }

        // ── Edit mode — hint ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = editMode == EditMode.EDITING,
            enter = fadeIn(tween(300)),
            exit  = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        ) {
            Text(
                "Tap background to dismiss",
                color = TextMuted, fontSize = 11.sp, letterSpacing = 0.3.sp
            )
        }

        // ── Page dots (normal mode only) ──────────────────────────────────────
        AnimatedVisibility(
            visible = slots.size > 1 && editMode == EditMode.NONE && showIndicators,
            enter = fadeIn(),
            exit = fadeOut(tween(800)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slots.forEachIndexed { index, _ ->
                    val isActive = index == currentIndex
                    val animatedWidth by animateDpAsState(
                        targetValue = if (isActive) 18.dp else 7.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "dotWidth"
                    )
                    Box(
                        Modifier
                            .size(width = animatedWidth, height = 7.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(
                                Color.White.copy(alpha = if (isActive) 1.0f else 0.3f)
                            )
                    )
                }
            }
        }
    }
}

// ── Report size directly to the AppWidgetHostView ─────────────────────────────
// This is the second layer of size reporting — directly on the View itself.
// Needed because some widgets (KWGT, clock widgets) read their own view size.

private fun reportSizeToHostView(
    hostView: android.view.View,
    containerW: Int,
    containerH: Int,
    paddingH: Int,
    paddingV: Int,
    context: android.content.Context
) {
    try {
        val netW = (containerW - paddingH * 2).coerceAtLeast(40)
        val netH = (containerH - paddingV * 2).coerceAtLeast(40)

        // Force measure and layout with exact dimensions
        hostView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(
                netW, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(
                netH, android.view.View.MeasureSpec.EXACTLY)
        )
        hostView.layout(0, 0, netW, netH)
        hostView.requestLayout()
    } catch (e: Exception) {
        Log.e("WidgetStack", "reportSizeToHostView: ${e.message}")
    }
}

// ── Unavailable widget placeholder ────────────────────────────────────────────

private fun buildUnavailableView(context: android.content.Context): android.view.View {
    return android.widget.TextView(context).apply {
        text = "Widget unavailable"
        setTextColor(android.graphics.Color.argb(150, 255, 255, 255))
        gravity = android.view.Gravity.CENTER
        textSize = 13f
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}

// ── 3D cube transition ────────────────────────────────────────────────────────

private fun animatePagerTransition(
    root: android.widget.FrameLayout,
    widgetContainer: android.widget.FrameLayout,
    oldIndex: Int,
    newIndex: Int,
    isNext: Boolean,
    slots: List<WidgetSlotModel>,
    widgetHostManager: WidgetHostManager,
    context: android.content.Context,
    onDone: () -> Unit
) {
    if (isPagerAnimating) return
    isPagerAnimating = true

    val oldView = widgetContainer.getChildAt(0) ?: run {
        isPagerAnimating = false; return }
    val slot = slots.getOrNull(newIndex) ?: run {
        isPagerAnimating = false; return }
    val info = widgetHostManager.getProviderInfo(slot.widgetId) ?: run {
        isPagerAnimating = false; return }

    val newView = try {
        widgetHostManager.createHostView(slot.widgetId, info).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    } catch (e: Exception) {
        Log.e("WidgetStack", "transition: ${e.message}")
        isPagerAnimating = false; return
    }

    widgetContainer.addView(newView)

    // Report size to incoming widget immediately
    widgetContainer.post {
        val w = widgetContainer.width; val h = widgetContainer.height
        if (w > 0 && h > 0) {
            widgetHostManager.updateWidgetSize(slot.widgetId, w, h)
            reportSizeToHostView(
                newView, w, h,
                widgetContainer.paddingLeft,
                widgetContainer.paddingTop,
                context
            )
        }
    }

    val w  = root.width.toFloat(); val h = root.height.toFloat()
    val cam = 8000f * context.resources.displayMetrics.density
    val dur = 600L
    val interp = FastOutSlowInInterpolator()

    oldView.cameraDistance = cam; newView.cameraDistance = cam
    oldView.pivotX = if (isNext) w else 0f; oldView.pivotY = h / 2f
    newView.pivotX = if (isNext) 0f else w; newView.pivotY = h / 2f
    newView.translationX = if (isNext) w else -w
    newView.rotationY    = if (isNext) 90f else -90f
    newView.alpha = 0f; newView.scaleX = 0.9f; newView.scaleY = 0.9f

    oldView.animate()
        .rotationY(if (isNext) -90f else 90f)
        .translationX(if (isNext) -w else w)
        .alpha(0f).scaleX(0.9f).scaleY(0.9f)
        .setDuration(dur).setInterpolator(interp)
        .withEndAction {
            widgetContainer.removeView(oldView)
            oldView.rotationY = 0f; oldView.translationX = 0f
            oldView.alpha = 1f; oldView.scaleX = 1f; oldView.scaleY = 1f
            isPagerAnimating = false
            onDone()
        }.start()

    newView.animate()
        .rotationY(0f).translationX(0f)
        .alpha(1f).scaleX(1f).scaleY(1f)
        .setDuration(dur).setInterpolator(interp).start()
}

// ── Edit action button ────────────────────────────────────────────────────────

@Composable
private fun EditActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.25f), color)
                    ),
                    CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Text(
            label, color = TextPrimary,
            fontSize = 12.sp, fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyWidgetState(onAddWidget: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentColor.copy(0.35f), Color.Transparent)
                    ), CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            FilledIconButton(
                onClick = onAddWidget,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AccentColor)
            ) {
                Icon(Icons.Default.Add, null,
                    tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "No widgets yet",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Long-press this area after adding\na widget to edit or reposition it",
            color = TextMuted, fontSize = 13.sp,
            textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AccentColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddWidget
                )
        ) {
            Text(
                "Add Widget", color = Color.White,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}
