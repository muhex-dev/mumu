package com.example.myapplication

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

// ── Design tokens ─────────────────────────────────────────────────────────────
private val AccentColor = Color(0xFF7C6DFF)
private val DangerColor = Color(0xFFFF5C5C)
private val TextPrimary = Color.White
private val TextMuted   = Color(0x99FFFFFF)
private val heightSteps = listOf(120, 160, 200, 240, 280, 320)
private var isPagerAnimating = false

@Composable
fun WidgetStackScreen(
    widgetHostManager: WidgetHostManager,
    onAddWidget: () -> Unit,
    recomposeKey: Int = 0
) {
    val context = LocalContext.current

    var slots by remember(recomposeKey) {
        mutableStateOf(widgetHostManager.pruneOrphanedSlots())
    }
    var currentIndex by remember(recomposeKey) { mutableIntStateOf(0) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(slots.size) {
        if (currentIndex >= slots.size && slots.isNotEmpty()) {
            currentIndex = slots.size - 1
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (slots.isEmpty()) {
            EmptyWidgetState(onAddWidget = onAddWidget)
        } else {

            // ── Two-layer AndroidView ────────────────────────────────────────
            // Layer 0 (widgetContainer) — holds the actual widget view
            // Layer 1 (touchOverlay)    — invisible, intercepts ALL touches
            //                             so swipes always work even when the
            //                             widget fills the container completely
            AndroidView(
                factory = { ctx ->
                    android.widget.FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Layer 0 — widget lives here
                        addView(android.widget.FrameLayout(ctx).apply {
                            id = android.R.id.content
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        })
                        // Layer 1 — touch interceptor, always on top
                        addView(android.view.View(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        })
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { root ->
                    val widgetContainer = root.getChildAt(0) as android.widget.FrameLayout
                    val touchOverlay    = root.getChildAt(1)

                    // ── Refresh widget view ──────────────────────────────────
                    widgetContainer.removeAllViews()
                    val slot = slots.getOrNull(currentIndex)
                    if (slot != null) {
                        val info = widgetHostManager.getProviderInfo(slot.widgetId)
                        if (info == null) {
                            widgetContainer.addView(
                                android.widget.TextView(context).apply {
                                    text = "Widget unavailable"
                                    setTextColor(android.graphics.Color.WHITE)
                                    gravity = android.view.Gravity.CENTER
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            )
                        } else {
                            try {
                                val hostView = widgetHostManager.createHostView(slot.widgetId, info)
                                hostView.layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                widgetContainer.addView(hostView)
                            } catch (e: Exception) {
                                Log.e("WidgetStack", "createHostView failed: ${e.message}")
                            }
                        }
                    }

                    // ── Touch interceptor setup ──────────────────────────────
                    // Rebuilds every time update() runs (slot/index change)
                    var startX   = 0f
                    var startY   = 0f
                    var isSwiping = false

                    val detector = GestureDetector(context,
                        object : GestureDetector.SimpleOnGestureListener() {

                            override fun onDown(e: MotionEvent): Boolean = true

                            override fun onFling(
                                e1: MotionEvent?,
                                e2: MotionEvent,
                                velocityX: Float,
                                velocityY: Float
                            ): Boolean {
                                if (e1 == null || slots.size <= 1) return false
                                val diffX = e1.x - e2.x
                                val diffY = e1.y - e2.y

                                val isHorizontalSwipe = Math.abs(diffX) > Math.abs(diffY) * 3
                                        && Math.abs(diffX) > 150
                                        && Math.abs(velocityX) > 200

                                if (isHorizontalSwipe) {
                                    val goNext   = diffX > 0
                                    val oldIndex = currentIndex
                                    val newIndex = if (goNext) {
                                        (currentIndex + 1) % slots.size
                                    } else {
                                        if (currentIndex <= 0) slots.size - 1
                                        else currentIndex - 1
                                    }
                                    if (!isPagerAnimating) {
                                        animatePagerTransition(
                                            root          = root,
                                            widgetContainer = widgetContainer,
                                            oldIndex      = oldIndex,
                                            newIndex      = newIndex,
                                            isNext        = goNext,
                                            slots         = slots,
                                            widgetHostManager = widgetHostManager,
                                            context       = context
                                        ) { currentIndex = newIndex }
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                    )

                    touchOverlay.setOnTouchListener { _, event ->
                        // Always feed the gesture detector
                        detector.onTouchEvent(event)

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                startX    = event.x
                                startY    = event.y
                                isSwiping = false
                                // Forward DOWN to widget so it can prepare press state
                                widgetContainer.dispatchTouchEvent(event)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = Math.abs(event.x - startX)
                                val dy = Math.abs(event.y - startY)
                                if (dx > 20 || dy > 20) {
                                    isSwiping = true
                                    // Cancel widget's press state on move
                                    val cancel = MotionEvent.obtain(event).apply {
                                        action = MotionEvent.ACTION_CANCEL
                                    }
                                    widgetContainer.dispatchTouchEvent(cancel)
                                    cancel.recycle()
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                if (!isSwiping) {
                                    // It was a tap — forward UP so widget reacts
                                    widgetContainer.dispatchTouchEvent(event)
                                }
                                isSwiping = false
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                isSwiping = false
                                widgetContainer.dispatchTouchEvent(event)
                            }
                        }

                        // Interceptor always consumes — widget never sees raw swipes
                        true
                    }
                }
            )

            // ── Page indicator dots ──────────────────────────────────────────
            if (slots.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slots.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentIndex) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentIndex) Color.White
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }

            // ── Edit overlay ─────────────────────────────────────────────────
            val currentSlot = slots.getOrNull(currentIndex)
            if (editingIndex == currentIndex && currentSlot != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000))
                ) {
                    // Red remove button — top right
                    IconButton(
                        onClick = {
                            editingIndex = -1
                            slots = widgetHostManager.removeSlot(currentSlot.widgetId)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(38.dp)
                            .background(DangerColor, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null,
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    // Height controls — right center
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                val cur = heightSteps.minByOrNull {
                                    kotlin.math.abs(it - currentSlot.heightDp) }!!
                                val next = (heightSteps.indexOf(cur) + 1)
                                    .coerceAtMost(heightSteps.lastIndex)
                                slots = widgetHostManager.updateSlotHeight(
                                    currentSlot.widgetId, heightSteps[next])
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, null,
                                tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }

                        Text("${currentSlot.heightDp}dp",
                            color = TextMuted, fontSize = 9.sp,
                            textAlign = TextAlign.Center)

                        IconButton(
                            onClick = {
                                val cur = heightSteps.minByOrNull {
                                    kotlin.math.abs(it - currentSlot.heightDp) }!!
                                val prev = (heightSteps.indexOf(cur) - 1)
                                    .coerceAtLeast(0)
                                slots = widgetHostManager.updateSlotHeight(
                                    currentSlot.widgetId, heightSteps[prev])
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, null,
                                tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Hint text
                    Text(
                        "Tap to dismiss  ·  swipe to switch widget",
                        color = TextMuted, fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    )

                    // Tap anywhere to dismiss
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 50.dp, top = 50.dp, bottom = 30.dp)
                            .noRippleClick { editingIndex = -1 }
                    )
                }
            }

            // ── Floating action buttons ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallIconButton(
                    icon = if (editingIndex == currentIndex)
                        Icons.Default.Close else Icons.Default.KeyboardArrowUp,
                    tint = if (editingIndex == currentIndex) DangerColor else TextPrimary,
                    bg   = Color(0x44000000),
                    onClick = {
                        editingIndex =
                            if (editingIndex == currentIndex) -1 else currentIndex
                    }
                )
                SmallIconButton(
                    icon    = Icons.Default.Add,
                    tint    = Color.White,
                    bg      = AccentColor,
                    onClick = onAddWidget
                )
            }
        }
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
        isPagerAnimating = false; return
    }
    val slot = slots.getOrNull(newIndex) ?: run {
        isPagerAnimating = false; return
    }
    val info = widgetHostManager.getProviderInfo(slot.widgetId) ?: run {
        isPagerAnimating = false; return
    }

    val newView = try {
        widgetHostManager.createHostView(slot.widgetId, info).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    } catch (e: Exception) {
        Log.e("WidgetStack", "transition createHostView failed: ${e.message}")
        isPagerAnimating = false
        return
    }

    widgetContainer.addView(newView)

    val width    = root.width.toFloat()
    val height   = root.height.toFloat()
    val density  = context.resources.displayMetrics.density
    val camDist  = 8000f * density
    val duration = 600L
    val interp   = FastOutSlowInInterpolator()

    oldView.cameraDistance = camDist
    newView.cameraDistance = camDist

    oldView.pivotX = if (isNext) width else 0f
    oldView.pivotY = height / 2f
    newView.pivotX = if (isNext) 0f else width
    newView.pivotY = height / 2f

    newView.translationX = if (isNext) width else -width
    newView.rotationY    = if (isNext) 90f else -90f
    newView.alpha        = 0f
    newView.scaleX       = 0.9f
    newView.scaleY       = 0.9f

    oldView.animate()
        .rotationY(if (isNext) -90f else 90f)
        .translationX(if (isNext) -width else width)
        .alpha(0f).scaleX(0.9f).scaleY(0.9f)
        .setDuration(duration)
        .setInterpolator(interp)
        .withEndAction {
            widgetContainer.removeView(oldView)
            oldView.rotationY    = 0f
            oldView.translationX = 0f
            oldView.alpha        = 1f
            oldView.scaleX       = 1f
            oldView.scaleY       = 1f
            isPagerAnimating     = false
            onDone()
        }
        .start()

    newView.animate()
        .rotationY(0f).translationX(0f)
        .alpha(1f).scaleX(1f).scaleY(1f)
        .setDuration(duration)
        .setInterpolator(interp)
        .start()
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
                .size(64.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentColor.copy(0.4f), Color.Transparent)
                    ), CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            FilledIconButton(
                onClick = onAddWidget,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AccentColor)
            ) {
                Icon(Icons.Default.Add, null,
                    tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("No widgets yet", color = TextPrimary,
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Tap + to pick from all\nwidgets installed on your phone",
            color = TextMuted, fontSize = 13.sp,
            textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}

// ── Small icon button ─────────────────────────────────────────────────────────

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(35.dp)
            .clip(CircleShape)
            .background(bg)
            .noRippleClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// ── No-ripple click ───────────────────────────────────────────────────────────

@Composable
private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier {
    val interactionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = interactionSource,
            onClick = onClick
        )
    )
}